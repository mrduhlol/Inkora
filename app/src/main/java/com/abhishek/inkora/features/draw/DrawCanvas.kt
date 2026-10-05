package com.abhishek.inkora.features.draw

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

/** One finger stroke. Color is ARGB int so the model stays UI-toolkit light. */
data class DrawStroke(
    val points: List<Offset> = emptyList(),
    val colorArgb: Int = 0xFF1C1B1F.toInt(),
    val widthPx: Float = 8f,
    val erase: Boolean = false
)

/** Immutable stroke history with undo/redo (cap keeps memory bounded). */
data class DrawingDoc(
    val strokes: List<DrawStroke> = emptyList(),
    val past: List<List<DrawStroke>> = emptyList(),
    val future: List<List<DrawStroke>> = emptyList()
) {
    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    private fun push(): DrawingDoc {
        val kept = (past + listOf(strokes)).takeLast(50)
        return copy(past = kept, future = emptyList())
    }

    fun addStroke(s: DrawStroke): DrawingDoc = push().copy(strokes = strokes + s)
    fun undo(): DrawingDoc {
        if (past.isEmpty()) return this
        return copy(strokes = past.last(), past = past.dropLast(1), future = listOf(strokes) + future)
    }
    fun redo(): DrawingDoc {
        if (future.isEmpty()) return this
        return copy(strokes = future.first(), past = past + listOf(strokes), future = future.drop(1))
    }
    fun clear(): DrawingDoc = if (strokes.isEmpty()) this else push().copy(strokes = emptyList())
}

/** Bake strokes onto a white bitmap at [size] pixels. Eraser paints paper-white. */
fun renderToBitmap(strokes: List<DrawStroke>, size: IntSize): Bitmap {
    val bmp = Bitmap.createBitmap(size.width.coerceAtLeast(1), size.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    canvas.drawColor(android.graphics.Color.WHITE)
    strokes.forEach { s ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeWidth = s.widthPx
            color = if (s.erase) android.graphics.Color.WHITE else s.colorArgb
        }
        if (s.points.size == 1) {
            val p = s.points[0]
            val dot = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                color = if (s.erase) android.graphics.Color.WHITE else s.colorArgb
            }
            canvas.drawCircle(p.x, p.y, s.widthPx / 2f, dot)
        } else if (s.points.size > 1) {
            val path = android.graphics.Path().apply {
                moveTo(s.points[0].x, s.points[0].y)
                s.points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            canvas.drawPath(path, paint)
        }
    }
    return bmp
}

private val DrawColors = listOf(
    0xFF1C1B1F.toInt() to "Black",
    0xFFD32F2F.toInt() to "Red",
    0xFF2F6FED.toInt() to "Blue",
    0xFF2E7D32.toInt() to "Green",
    0xFFE8710A.toInt() to "Orange"
)

/**
 * Full-screen finger-drawing dialog. Strokes save as a PNG attachment on the
 * note — same local lifecycle as picked images (trash-safe, exportable).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawDialog(
    onDismiss: () -> Unit,
    onSave: (ByteArray) -> Unit
) {
    var doc by remember { mutableStateOf(DrawingDoc()) }
    var inProgress by remember { mutableStateOf<List<Offset>?>(null) }
    var colorArgb by remember { mutableStateOf(DrawColors[0].first) }
    var erase by remember { mutableStateOf(false) }
    var widthDp by remember { mutableStateOf(6) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val widthPx = with(density) { widthDp.dp.toPx() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close drawing") } },
                title = { Text("Draw") },
                actions = {
                    IconButton(onClick = { doc = doc.undo() }, enabled = doc.canUndo) {
                        Icon(Icons.Filled.Undo, "Undo")
                    }
                    IconButton(onClick = { doc = doc.redo() }, enabled = doc.canRedo) {
                        Icon(Icons.Filled.Redo, "Redo")
                    }
                    IconButton(onClick = { doc = doc.clear() }, enabled = doc.strokes.isNotEmpty()) {
                        Icon(Icons.Filled.DeleteSweep, "Clear all")
                    }
                    IconButton(
                        onClick = {
                            if (canvasSize.width > 0 && canvasSize.height > 0 && doc.strokes.isNotEmpty()) {
                                val bmp = renderToBitmap(doc.strokes, canvasSize)
                                val out = ByteArrayOutputStream()
                                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                                bmp.recycle()
                                onSave(out.toByteArray())
                            }
                        },
                        enabled = doc.strokes.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.Check, "Save drawing")
                    }
                }
            )
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    DrawColors.forEach { (argb, name) ->
                        val selected = !erase && colorArgb == argb
                        Box(
                            Modifier.size(40.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    colorArgb = argb
                                    erase = false
                                }
                        )
                    }
                    Box(
                        Modifier.size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (erase) 3.dp else 1.dp,
                                color = if (erase) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { erase = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Edit, "Eraser", modifier = Modifier.size(20.dp))
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Text("Size", style = MaterialTheme.typography.labelLarge)
                    listOf(3, 6, 12).forEach { w ->
                        val selected = widthDp == w
                        Box(
                            Modifier.size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { widthDp = w },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier.size(w.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                }
            }
        }
    ) { pad ->
        Box(
            Modifier.fillMaxSize().padding(pad).padding(16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val pts = mutableListOf(down.position)
                        inProgress = pts.toList()
                        var done = false
                        while (!done) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { c ->
                                if (c.pressed) {
                                    pts.add(c.position)
                                    c.consume()
                                    inProgress = pts.toList()
                                }
                            }
                            if (event.changes.all { !it.pressed }) done = true
                        }
                        doc = doc.addStroke(DrawStroke(pts.toList(), colorArgb, widthPx, erase))
                        inProgress = null
                    }
                }
        ) {
            val live = inProgress
            Canvas(Modifier.fillMaxSize()) {
                (doc.strokes + listOfNotNull(live?.let { DrawStroke(it, colorArgb, widthPx, erase) })).forEach { s ->
                    val paint = Color(if (s.erase) 0xFFFFFFFF.toInt() else s.colorArgb)
                    if (s.points.size == 1) {
                        drawCircle(paint, s.widthPx / 2f, s.points[0])
                    } else if (s.points.size > 1) {
                        val path = Path().apply {
                            moveTo(s.points[0].x, s.points[0].y)
                            s.points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, paint, style = Stroke(s.widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
            if (doc.strokes.isEmpty() && live == null) {
                Text(
                    "Draw with your finger",
                    color = Color(0xFF9A958C),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
