package com.abhishek.inkora.features.editor

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhishek.inkora.data.repository.Attachment
import com.abhishek.inkora.domain.model.BlockKind
import com.abhishek.inkora.domain.model.PageStyle
import com.abhishek.inkora.domain.model.PaperBackground
import com.abhishek.inkora.domain.model.ParaAlign
import com.abhishek.inkora.domain.model.SpanKind
import com.abhishek.inkora.features.draw.DrawDialog
import com.abhishek.inkora.ui.components.FormattingToolbar
import com.abhishek.inkora.ui.components.InkoraPaperSurface
import com.abhishek.inkora.ui.components.InkoraTopBar
import com.abhishek.inkora.ui.components.LocalImageFull
import com.abhishek.inkora.ui.components.LocalImageThumb
import com.abhishek.inkora.ui.components.PageStyleSelector
import com.abhishek.inkora.ui.theme.mutedOnPaperColor
import com.abhishek.inkora.ui.theme.onPaperColor
import com.abhishek.inkora.ui.theme.paperColorFor

/**
 * Notebook-page editor over [RichDoc]: selection-scoped spans, headings,
 * quotes, dividers, alignment, indent, generated list glyphs, checkbox
 * tap-to-toggle, local image attachments, undo/redo and autosave.
 *
 * Layout when the keyboard is open: PAGE → TOOLBAR → KEYBOARD. The toolbar is
 * part of the content column with [imePadding] so it rides directly above the
 * IME instead of hiding behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    vm: EditorViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val folders by vm.allFolders.collectAsStateWithLifecycle()
    val attachments by vm.attachments.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    var styleSheet by remember { mutableStateOf(false) }
    var folderDialog by remember { mutableStateOf(false) }
    var infoDialog by remember { mutableStateOf(false) }
    var showDraw by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<Attachment?>(null) }
    val note = state.note
    val titleFocus = remember { FocusRequester() }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.addImage(uri)
    }

    if (state.notFound) {
        Scaffold { pad ->
            Text("Note not found or in Trash.", Modifier.padding(pad).padding(24.dp))
        }
        return
    }

    // New blank note: focus the title immediately so creation feels instant.
    val isFresh = note != null && state.title.isBlank() && state.doc.lines.all { it.text.isBlank() }
    LaunchedEffect(note?.id, isFresh) {
        if (isFresh) titleFocus.requestFocus()
    }
    LaunchedEffect(state.saveError) {
        state.saveError?.let { snackbar.showSnackbar(it); vm.dismissSaveError() }
    }

    val paper = paperColorFor(note?.backgroundStyle ?: "cream", note?.backgroundColor)
    val ink = onPaperColor(paper)
    val muted = mutedOnPaperColor(paper)
    val doc = state.doc
    val accent = MaterialTheme.colorScheme.primary
    val currentAlign = remember(doc, doc.selection) {
        val li = doc.lineIndexAtRendered(doc.selection.min)
        doc.lines.getOrNull(li)?.align ?: ParaAlign.LEFT
    }
    val annotated = remember(doc, muted, accent) { doc.render(muted, accent) }
    val field = remember(annotated, doc.selection) { TextFieldValue(annotated, doc.selection) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val prefixRanges = remember(doc) { doc.prefixRanges() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            InkoraTopBar(
                title = state.title,
                isFavorite = note?.isFavorite == true,
                menuExpanded = menu,
                onBack = { vm.flushNow(onBack) },
                onToggleFavorite = { vm.toggleFavorite() },
                onMore = { menu = true },
                onMenuDismiss = { menu = false },
                menuContent = {
                    DropdownMenuItem(
                        text = { Text("Page style") },
                        onClick = { menu = false; styleSheet = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to folder") },
                        onClick = { menu = false; folderDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Add image") },
                        leadingIcon = { Icon(Icons.Filled.AddPhotoAlternate, null) },
                        onClick = {
                            menu = false
                            picker.launch("image/*")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Draw") },
                        leadingIcon = { Icon(Icons.Filled.Brush, null) },
                        onClick = { menu = false; showDraw = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Note info") },
                        leadingIcon = { Icon(Icons.Filled.Info, null) },
                        onClick = { menu = false; infoDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Filled.Share, null) },
                        onClick = {
                            menu = false
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, vm.shareText())
                            }
                            context.startActivity(Intent.createChooser(send, "Share note"))
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Move to Trash") },
                        leadingIcon = { Icon(Icons.Filled.Delete, null) },
                        onClick = { menu = false; vm.trash(onBack) }
                    )
                }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            InkoraPaperSurface(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                background = note?.backgroundStyle ?: "cream",
                customHex = note?.backgroundColor,
                pageStyle = note?.pageStyle ?: "blank"
            ) {
                Column(Modifier.fillMaxSize().padding(18.dp)) {
                    TextField(
                        value = state.title,
                        onValueChange = vm::onTitleChange,
                        placeholder = { Text("Title", color = muted) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge.copy(color = ink),
                        modifier = Modifier.fillMaxWidth().focusRequester(titleFocus),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedPlaceholderColor = muted,
                            unfocusedPlaceholderColor = muted
                        )
                    )
                    Box(Modifier.fillMaxSize()) {
                        BasicTextField(
                            value = field,
                            onValueChange = { vm.onBodyInput(it.text, it.selection) },
                            onTextLayout = { layout = it },
                            modifier = Modifier.fillMaxSize().pointerInput(doc, layout) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                    val lr = layout
                                    if (lr != null) {
                                        val offset = lr.getOffsetForPosition(down.position)
                                        val li = doc.lineIndexAtRendered(offset)
                                        val range = prefixRanges.getOrNull(li)
                                        if (range != null && offset in range &&
                                            doc.lines.getOrNull(li)?.block == BlockKind.CHECK
                                        ) {
                                            down.consume()
                                            vm.toggleCheck(li)
                                        }
                                    }
                                }
                            },
                            textStyle = TextStyle(color = ink, fontSize = state.textSizeSp.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                Box {
                                    if (field.text.isEmpty()) {
                                        Text("Start writing…", color = muted, fontSize = state.textSizeSp.sp)
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }
            }
            // Attachments strip, then the toolbar riding above the keyboard via
            // IME insets (zero extra space when the keyboard is dismissed).
            if (attachments.isNotEmpty()) {
                AttachmentStrip(
                    attachments = attachments,
                    onOpen = { viewer = it },
                    onRemove = { vm.removeImage(it) },
                    onAdd = { picker.launch("image/*") }
                )
            }
            FormattingToolbar(
                active = doc.activeKinds(),
                blocks = doc.activeBlocks(),
                align = currentAlign,
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                onUndo = vm::undo,
                onRedo = vm::redo,
                onBold = { vm.toggleSpan(SpanKind.BOLD) },
                onItalic = { vm.toggleSpan(SpanKind.ITALIC) },
                onUnderline = { vm.toggleSpan(SpanKind.UNDERLINE) },
                onStrike = { vm.toggleSpan(SpanKind.STRIKE) },
                onBullet = { vm.toggleBlock(BlockKind.BULLET) },
                onNumbered = { vm.toggleBlock(BlockKind.NUMBERED) },
                onChecklist = { vm.toggleBlock(BlockKind.CHECK) },
                onHeading = vm::cycleHeading,
                onQuote = vm::toggleQuote,
                onDivider = vm::insertDivider,
                onAlign = vm::cycleAlign,
                onIndentMore = vm::indentMore,
                onIndentLess = vm::indentLess,
                modifier = Modifier.navigationBarsPadding().imePadding()
            )
        }

        if (folderDialog) {
            AlertDialog(
                onDismissRequest = { folderDialog = false },
                confirmButton = {
                    TextButton(onClick = { folderDialog = false }) { Text("Done") }
                },
                title = { Text("Move to folder") },
                text = {
                    Column {
                        val current = note?.folderId
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(
                                "No folder",
                                Modifier.weight(1f).align(Alignment.CenterVertically)
                            )
                            RadioButton(
                                selected = current == null,
                                onClick = { vm.moveToFolder(null) }
                            )
                        }
                        folders.forEach { f ->
                            ListItem(
                                headlineContent = { Text(f.name) },
                                trailingContent = {
                                    RadioButton(
                                        selected = current == f.id,
                                        onClick = { vm.moveToFolder(f.id) }
                                    )
                                },
                                modifier = Modifier.clickable { vm.moveToFolder(f.id) }
                            )
                        }
                        if (folders.isEmpty()) {
                            Text(
                                "No folders yet — create one from the Folders screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }

        val viewing = viewer
        if (viewing != null) {
            Dialog(
                onDismissRequest = { viewer = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
                    LocalImageFull(viewing.file, Modifier.fillMaxSize().align(Alignment.Center))
                    IconButton(
                        onClick = { viewer = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }

        if (infoDialog && note != null) {
            val stats = remember(state.doc, state.title) { vm.stats() }
            val folderName = folders.firstOrNull { it.id == note.folderId }?.name ?: "No folder"
            AlertDialog(
                onDismissRequest = { infoDialog = false },
                confirmButton = { TextButton(onClick = { infoDialog = false }) { Text("Close") } },
                title = { Text("Note info") },
                text = {
                    Column {
                        InfoRow("Created", formatDate(note.createdAt))
                        InfoRow("Modified", formatDate(note.updatedAt))
                        InfoRow("Words", stats.words.toString())
                        InfoRow("Characters", stats.chars.toString())
                        InfoRow("Folder", folderName)
                        InfoRow("Favorite", if (note.isFavorite) "Yes" else "No")
                        InfoRow("Pinned", if (note.isPinned) "Yes" else "No")
                        InfoRow("Images", attachments.size.toString())
                    }
                }
            )
        }

        if (showDraw) {
            DrawDialog(
                onDismiss = { showDraw = false },
                onSave = { png -> vm.addDrawing(png); showDraw = false }
            )
        }

        if (styleSheet) {            ModalBottomSheet(onDismissRequest = { styleSheet = false }, sheetState = rememberModalBottomSheetState()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Page style", fontSize = 18.sp)
                    PageStyleSelector(
                        selected = PageStyle.fromKey(note?.pageStyle),
                        onSelect = { vm.setPageStyle(it) }
                    )
                    Text("Paper", fontSize = 18.sp, modifier = Modifier.padding(top = 16.dp))
                    Row {
                        listOf(PaperBackground.WHITE, PaperBackground.CREAM, PaperBackground.GRAY, PaperBackground.DARK)
                            .forEach { bg ->
                                FilterChip(
                                    selected = (note?.backgroundStyle == bg.key.lowercase()),
                                    onClick = { vm.setPaperBackground(bg) },
                                    label = { Text(bg.key) },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDate(millis: Long): String =
    java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
        .format(java.util.Date(millis))

/** Horizontal strip of attached image cards with remove actions and an add tile. */
@Composable
private fun AttachmentStrip(
    attachments: List<Attachment>,
    onOpen: (Attachment) -> Unit,
    onRemove: (Long) -> Unit,
    onAdd: () -> Unit
) {
    var confirming: Long? by remember { mutableStateOf(null) }
    LazyRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(attachments, key = { it.id }) { a ->
            Box(Modifier.size(88.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = { onOpen(a) })) {
                LocalImageThumb(a.file, Modifier.fillMaxSize())
                IconButton(
                    onClick = { confirming = a.id },
                    modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove image", tint = Color.White)
                }
            }
        }
        item {
            Card(
                onClick = onAdd,
                modifier = Modifier.size(88.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Add image")
                }
            }
        }
    }
    val doomed = confirming
    if (doomed != null) {
        AlertDialog(
            onDismissRequest = { confirming = null },
            confirmButton = { TextButton(onClick = { onRemove(doomed); confirming = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirming = null }) { Text("Keep") } },
            title = { Text("Remove image?") },
            text = { Text("The image file is deleted from this device.") }
        )
    }
}
