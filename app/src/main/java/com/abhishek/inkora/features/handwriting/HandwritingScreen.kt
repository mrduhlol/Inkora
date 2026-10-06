package com.abhishek.inkora.features.handwriting

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhishek.inkora.domain.model.CANVAS_SIZE
import com.abhishek.inkora.domain.model.HwPoint
import com.abhishek.inkora.domain.model.HwStroke
import com.abhishek.inkora.domain.model.HwTool
import com.abhishek.inkora.domain.model.INK_COLORS
import com.abhishek.inkora.domain.model.MAX_SCALE
import com.abhishek.inkora.domain.model.MIN_SCALE
import com.abhishek.inkora.domain.model.PEN_WIDTHS
import com.abhishek.inkora.ui.theme.mutedOnPaperColor
import com.abhishek.inkora.ui.theme.paperColorFor
import kotlin.math.abs

/**
 * Handwriting notebook: a large virtual canvas ([CANVAS_SIZE]px square) with
 * pinch-to-zoom, two-finger pan, pen/eraser/pan tools, stroke undo/redo and
 * debounced vector autosave. One finger draws; two fingers navigate; a stylus
 * always draws. Nothing writes to disk per pointer movement — only finished
 * strokes persist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandwritingScreen(
    onBack: () -> Unit,
    vm: HandwritingViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmClear by remember { mutableStateOf(false) }
    val note = state.note

    if (state.notFound) {
        Scaffold { pad ->
            Text("Note not found or in Trash.", Modifier.padding(pad).padding(24.dp))
        }
        return
    }

    LaunchedEffect(state.saveError) {
        state.saveError?.let { snackbar.showSnackbar(it); vm.dismissSaveError() }
    }
    LaunchedEffect(state.strokeLimitHit) {
        if (state.strokeLimitHit) {
            snackbar.showSnackbar("Stroke limit reached — undo or clear to continue")
            vm.dismissLimit()
        }
    }

    val paper = paperColorFor(note?.backgroundStyle ?: "cream", note?.backgroundColor)
    val muted = mutedOnPaperColor(paper)

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var live by remember { mutableStateOf<List<Offset>?>(null) }
    var liveErase by remember { mutableStateOf(false) }

    fun toCanvas(p: Offset): Offset = (p - offset) / scale

    fun clampOffset(s: Float, o: Offset): Offset {
        if (viewSize == IntSize.Zero) return o
        val extent = CANVAS_SIZE * s
        val margin = 200f
        val minX = viewSize.width - extent - margin
        val maxX = margin
        val minY = viewSize.height - extent - margin
        val maxY = margin
        return Offset(
            o.x.coerceIn(minOf(minX, maxX), maxOf(minX, maxX)),
            o.y.coerceIn(minOf(minY, maxY), maxOf(minY, maxY))
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { vm.flushNow(onBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            if (note?.title.isNullOrBlank()) "Handwriting" else note.title,
                            maxLines = 1
                        )
                        Text(
                            if (state.isSaving) "Saving…" else "Saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = vm::undo, enabled = state.canUndo) {
                        Icon(Icons.Filled.Undo, contentDescription = "Undo stroke")
                    }
                    IconButton(onClick = vm::redo, enabled = state.canRedo) {
                        Icon(Icons.Filled.Redo, contentDescription = "Redo stroke")
                    }
                    IconButton(onClick = { confirmClear = true }, enabled = state.strokes.isNotEmpty()) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear canvas")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { vm.setTool(HwTool.PEN) }) {
                            Icon(
                                Icons.Filled.Brush,
                                contentDescription = "Pen",
                                tint = if (state.tool == HwTool.PEN) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { vm.setTool(HwTool.ERASER) }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Eraser",
                                tint = if (state.tool == HwTool.ERASER) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { vm.setTool(HwTool.PAN) }) {
                            Icon(
                                Icons.Filled.PanTool,
                                contentDescription = "Pan",
                                tint = if (state.tool == HwTool.PAN) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        INK_COLORS.forEach { argb ->
                            val selected = state.tool == HwTool.PEN && state.colorArgb == argb
                            Box(
                                Modifier.size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        vm.setColor(argb)
                                        vm.setTool(HwTool.PEN)
                                    }
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PEN_WIDTHS.forEach { w ->
                            val selected = state.widthPx == w
                            Box(
                                Modifier.size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { vm.setWidth(w) },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier.size((w / 3f).dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurface)
                                )
                            }
                        }
                        IconButton(onClick = {
                            scale = (scale / 1.25f).coerceIn(MIN_SCALE, MAX_SCALE)
                            offset = clampOffset(scale, offset)
                        }) { Icon(Icons.Filled.Remove, contentDescription = "Zoom out") }
                        Text(
                            "${(scale * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clickable {
                                scale = 1f
                                offset = clampOffset(scale, Offset.Zero)
                            }
                        )
                        IconButton(onClick = {
                            scale = (scale * 1.25f).coerceIn(MIN_SCALE, MAX_SCALE)
                            offset = clampOffset(scale, offset)
                        }) { Icon(Icons.Filled.Add, contentDescription = "Zoom in") }
                    }
                }
            }
        }
    ) { pad ->
        Box(
            Modifier.fillMaxSize().padding(pad)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onSizeChanged { viewSize = it }
                .pointerInput(state.tool, state.colorArgb, state.widthPx) {
                    awaitEachGesture {
                        awaitFirstDown()
                        val points = mutableListOf<Offset>()
                        val pressures = mutableListOf<Float>()
                        var navigating = false
                        var drawErase = false
                        var done = false
                        while (!done) {
                            val event = awaitPointerEvent()
                            val count = event.changes.count { it.pressed }
                            if (count >= 2) {
                                // Two fingers: navigate, never draw.
                                navigating = true
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid()
                                val old = scale
                                val new = (old * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                                offset = clampOffset(
                                    new,
                                    centroid - (centroid - offset) * (new / old) + pan
                                )
                                scale = new
                            } else if (count == 1 && !navigating) {
                                val c = event.changes.first { it.pressed }
                                val stylus = c.type == androidx.compose.ui.input.pointer.PointerType.Stylus
                                val eraserPressed = c.type == androidx.compose.ui.input.pointer.PointerType.Eraser
                                val draw = stylus || eraserPressed ||
                                    state.tool == HwTool.PEN || state.tool == HwTool.ERASER
                                if (draw) {
                                    val cp = toCanvas(c.position)
                                    if (cp.x in -500f..CANVAS_SIZE + 500f && cp.y in -500f..CANVAS_SIZE + 500f) {
                                        points.add(cp)
                                        pressures.add(c.pressure.takeIf { it > 0f } ?: 1f)
                                        drawErase = eraserPressed || state.tool == HwTool.ERASER
                                        live = points.toList()
                                        liveErase = drawErase
                                    }
                                } else {
                                    // Pan tool: one finger moves the canvas.
                                    offset = clampOffset(scale, offset + c.previousPosition.let {
                                        c.position - it
                                    })
                                }
                            }
                            event.changes.forEach { if (!it.pressed) it.consume() }
                            if (event.changes.all { !it.pressed }) done = true
                        }
                        if (!navigating && points.isNotEmpty()) {
                            if (drawErase) {
                                vm.eraseAt { stroke -> strokeTouches(stroke, points, 40f) }
                            } else {
                                val avgP = if (pressures.isEmpty()) 1f else pressures.average().toFloat()
                                vm.addStroke(
                                    HwStroke(
                                        points = points.mapIndexed { i, o ->
                                            HwPoint(o.x, o.y, pressures.getOrElse(i) { 1f })
                                        },
                                        colorArgb = state.colorArgb,
                                        widthPx = state.widthPx * (0.6f + 0.4f * avgP.coerceIn(0f, 1f)),
                                        erase = false
                                    )
                                )
                            }
                        }
                        live = null
                    }
                }
        ) {
            val strokes = state.strokes
            Canvas(Modifier.fillMaxSize()) {
                withTransform({
                    translate(offset.x, offset.y)
                    scale(scale, scale)
                }) {
                    // Virtual sheet.
                    drawRect(color = paper, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(CANVAS_SIZE, CANVAS_SIZE))
                    drawRect(color = muted.copy(alpha = 0.5f), topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(CANVAS_SIZE, CANVAS_SIZE), style = Stroke(2f / scale))
                    strokes.forEach { s -> drawHwStroke(s) }
                    val cur = live
                    if (cur != null && cur.isNotEmpty()) {
                        val preview = HwStroke(
                            cur.map { HwPoint(it.x, it.y) },
                            state.colorArgb,
                            state.widthPx,
                            liveErase
                        )
                        if (liveErase) {
                            // Eraser trail: neutral preview, never paper-colored ink.
                            drawHwStroke(preview.copy(colorArgb = 0xFF9A958C.toInt()))
                        } else {
                            drawHwStroke(preview)
                        }
                    }
                }
            }
            if (strokes.isEmpty() && live == null) {
                Text(
                    "Draw with a finger or stylus — pinch to zoom",
                    color = muted,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        if (confirmClear) {
            AlertDialog(
                onDismissRequest = { confirmClear = false },
                confirmButton = {
                    TextButton(onClick = { vm.clearAll(); confirmClear = false }) { Text("Clear") }
                },
                dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Keep") } },
                title = { Text("Clear canvas?") },
                text = { Text("All strokes will be removed. You can undo right after.") }
            )
        }
    }
}

/** Stroke-eraser hit test in canvas px. */
private fun strokeTouches(stroke: HwStroke, path: List<Offset>, radius: Float): Boolean {
    if (path.isEmpty() || stroke.points.isEmpty()) return false
    // Coarse check: any eraser point near any stroke point.
    return path.any { e ->
        stroke.points.any { p ->
            abs(e.x - p.x) < radius + stroke.widthPx / 2f &&
                abs(e.y - p.y) < radius + stroke.widthPx / 2f
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHwStroke(s: HwStroke) {
    val paint = Color(s.colorArgb)
    if (s.points.size == 1) {
        val p = s.points[0]
        drawCircle(paint, s.widthPx / 2f, Offset(p.x, p.y))
    } else if (s.points.size > 1) {
        val path = Path().apply {
            moveTo(s.points[0].x, s.points[0].y)
            s.points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, paint, style = Stroke(s.widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
