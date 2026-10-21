package com.abhishek.inkora.features.editor

import android.content.Intent
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhishek.inkora.data.repository.Attachment
import com.abhishek.inkora.domain.model.BlockKind
import com.abhishek.inkora.domain.model.ImagePlacement
import com.abhishek.inkora.domain.model.PageStyle
import com.abhishek.inkora.domain.model.PaperBackground
import com.abhishek.inkora.domain.model.ParaAlign
import com.abhishek.inkora.domain.model.SpanKind
import com.abhishek.inkora.features.draw.DrawDialog
import com.abhishek.inkora.ui.components.FileCard
import com.abhishek.inkora.ui.components.InkoraChip
import com.abhishek.inkora.ui.components.FormattingToolbar
import com.abhishek.inkora.ui.components.InkoraPaperSurface
import com.abhishek.inkora.ui.components.InkoraTopBar
import com.abhishek.inkora.ui.components.LocalImageFull
import com.abhishek.inkora.ui.components.LocalImageThumb
import com.abhishek.inkora.ui.components.PageStyleSelector
import com.abhishek.inkora.ui.components.TagChip
import com.abhishek.inkora.ui.theme.mutedOnPaperColor
import com.abhishek.inkora.ui.theme.onPaperColor
import com.abhishek.inkora.ui.theme.paperColorFor
import java.io.File
import kotlinx.coroutines.launch

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
    val noteTags by vm.noteTags.collectAsStateWithLifecycle()
    val attachments by vm.attachments.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    var styleSheet by remember { mutableStateOf(false) }
    var folderDialog by remember { mutableStateOf(false) }
    var infoDialog by remember { mutableStateOf(false) }
    var showDraw by remember { mutableStateOf(false) }
    var linkDialog by remember { mutableStateOf(false) }
    var tableDialog by remember { mutableStateOf(false) }
    var tableSizeDialog by remember { mutableStateOf(false) }
    var tagDialog by remember { mutableStateOf(false) }
    var colorDialog by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<Attachment?>(null) }
    val note = state.note
    val titleFocus = remember { FocusRequester() }
    val bodyFocus = remember { FocusRequester() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.addImage(uri)
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.addFile(uri)
    }

    if (state.notFound) {
        Scaffold { pad ->
            Text("Note not found or in Trash.", Modifier.padding(pad).padding(24.dp))
        }
        return
    }

    // New blank note: focus the title immediately so creation feels instant.
    // Fresh to-do notes focus the first task instead — typing starts working.
    val isFresh = note != null && state.title.isBlank() && state.doc.lines.all { it.text.isBlank() }
    val isFreshTodo = isFresh && note?.noteType == com.abhishek.inkora.domain.model.NoteType.TODO
    LaunchedEffect(note?.id, isFresh, isFreshTodo) {
        if (isFreshTodo) bodyFocus.requestFocus()
        else if (isFresh) titleFocus.requestFocus()
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
    val viewConfig = LocalViewConfiguration.current
    val taps = remember { TapTracker() }

    // Inline images: where their placeholder sits in the rendered text and
    // which attachment holds the file.
    val imageAnchors = remember(doc) { doc.imageAnchors() }
    val attachmentsById = remember(attachments) { attachments.associateBy { it.id } }
    val selectedImageLine = doc.imageLineAtCaret()
    val selectedImage = doc.imageAtCaret()

    /**
     * Owns multi-tap text selection explicitly: double-tap selects the word,
     * triple-tap the paragraph — clamped to line content so generated list
     * glyphs are never selected. The repeated tap is consumed in the Initial
     * pass, so the text field never moves the caret over the selection made
     * here. Single taps are never consumed, so cursor placement, copy/cut/
     * paste and the native toolbar stay intact.
     * Saving is never attached to any tap; autosave owns persistence.
     */
    fun selectByTap(offset: Int, tapCount: Int, lr: TextLayoutResult): Boolean {
        val li = doc.lineIndexAtRendered(offset)
        val range = when {
            tapCount >= 3 -> doc.contentBounds(li)?.let { TextRange(it.first, it.second) }
            else -> {
                val text = doc.rendered()
                val boundary = lr.getWordBoundary(offset.coerceIn(0, text.length))
                doc.wordRangeAt(offset, boundary.start, boundary.end)
            }
        } ?: return false
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        vm.setSelection(range)
        return true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            InkoraTopBar(
                title = state.title,
                isFavorite = note?.isFavorite == true,
                menuExpanded = menu,
                // Unobtrusive save state: present only while a write is pending.
                subtitle = if (state.isSaving) "Saving…" else null,
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
                        text = { Text("Attach file") },
                        leadingIcon = { Icon(Icons.Filled.AttachFile, null) },
                        onClick = { menu = false; filePicker.launch("*/*") }
                    )
                    DropdownMenuItem(
                        text = { Text("Tags") },
                        leadingIcon = { Icon(Icons.Filled.Label, null) },
                        onClick = { menu = false; tagDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text(if (note?.isArchived == true) "Unarchive" else "Archive") },
                        leadingIcon = { Icon(Icons.Filled.Inventory2, null) },
                        onClick = { menu = false; vm.toggleArchiveState() }
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
        // Comfortable reading width on large screens; phones use full width.
        Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 720.dp)) {
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
                            modifier = Modifier.fillMaxSize().focusRequester(bodyFocus).pointerInput(doc, layout) {
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
                                            // Checkbox glyph: own the tap immediately so the
                                            // text field never moves the cursor there.
                                            down.consume()
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            vm.toggleCheck(li)
                                        } else {
                                            val now = SystemClock.uptimeMillis()
                                            // Distance is measured on screen
                                            // (pixels), not in characters.
                                            val dx = down.position.x - taps.x
                                            val dy = down.position.y - taps.y
                                            val slop = viewConfig.touchSlop
                                            val repeated = taps.count > 0 &&
                                                now - taps.time < viewConfig.doubleTapTimeoutMillis &&
                                                (dx * dx + dy * dy) <= slop * slop
                                            taps.count = if (repeated) taps.count + 1 else 1
                                            taps.time = now
                                            taps.x = down.position.x
                                            taps.y = down.position.y
                                            if (taps.count >= 2) {
                                                // Second (or third) tap: consume the down in
                                                // the Initial pass so the text field keeps
                                                // the word/paragraph selection made here.
                                                down.consume()
                                                selectByTap(offset, taps.count, lr)
                                                if (taps.count >= 3) taps.count = 0
                                            }
                                        }
                                    }
                                }
                            },
                            textStyle = TextStyle(
                                color = ink,
                                fontSize = state.textSizeSp.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                Box(Modifier.fillMaxSize()) {
                                    // Pictures that float behind the text are painted
                                    // first, so the words stay readable on top.
                                    InlineImageLayer(
                                        anchors = imageAnchors,
                                        layout = layout,
                                        attachments = attachmentsById,
                                        selectedLine = selectedImageLine,
                                        layer = ImageLayer.BEHIND_PAINT,
                                        onSelect = vm::selectImage
                                    )
                                    if (field.text.isEmpty()) {
                                        Text("Start writing…", color = muted, fontSize = state.textSizeSp.sp)
                                    }
                                    inner()
                                    // A floating picture sits under the text, so it needs
                                    // an invisible target on top to stay selectable.
                                    InlineImageLayer(
                                        anchors = imageAnchors,
                                        layout = layout,
                                        attachments = attachmentsById,
                                        selectedLine = selectedImageLine,
                                        layer = ImageLayer.BEHIND_HIT,
                                        onSelect = vm::selectImage
                                    )
                                    // Pictures in the flow or floating in front are
                                    // painted over the text.
                                    InlineImageLayer(
                                        anchors = imageAnchors,
                                        layout = layout,
                                        attachments = attachmentsById,
                                        selectedLine = selectedImageLine,
                                        layer = ImageLayer.FRONT_PAINT,
                                        onSelect = vm::selectImage
                                    )
                                }
                            }
                        )
                    }
                }
            }
            // Attachments strip, then the toolbar riding above the keyboard via
            // IME insets (zero extra space when the keyboard is dismissed).
            // Pictures that live inline in the text are not listed here.
            val inlineImageIds = remember(doc) { doc.imageAnchors().map { it.image.attachmentId }.toSet() }
            if (attachments.isNotEmpty()) {
                AttachmentStrip(
                    attachments = attachments.filter { it.kind != "file" && it.id !in inlineImageIds },
                    onOpen = { viewer = it },
                    onRemove = { vm.removeImage(it) },
                    onAdd = { picker.launch("image/*") }
                )
            }
            val files = remember(attachments) { attachments.filter { it.kind == "file" } }
            files.forEach { f ->
                var fileConfirm by remember(f.id) { mutableStateOf(false) }
                FileCard(
                    attachment = f,
                    onOpen = {
                        val opened = openAttachment(context, f)
                        if (!opened) scope.launch { snackbar.showSnackbar("No app can open this file") }
                    },
                    onRemove = { fileConfirm = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                if (fileConfirm) {
                    AlertDialog(
                        onDismissRequest = { fileConfirm = false },
                        confirmButton = {
                            TextButton(onClick = { vm.removeImage(f.id); fileConfirm = false }) { Text("Remove") }
                        },
                        dismissButton = { TextButton(onClick = { fileConfirm = false }) { Text("Keep") } },
                        title = { Text("Remove attachment?") },
                        text = { Text("${f.fileName} will be deleted from this device.") }
                    )
                }
            }
            // Placement controls for the picture at the caret.
            if (selectedImage != null) {
                ImageActionBar(
                    placement = selectedImage.placement,
                    onInline = { vm.setImagePlacement(ImagePlacement.INLINE) },
                    onBehind = { vm.setImagePlacement(ImagePlacement.BEHIND) },
                    onFront = { vm.setImagePlacement(ImagePlacement.IN_FRONT) },
                    onRemove = { vm.removeImageAtCaret() }
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
                onCode = { vm.toggleBlock(BlockKind.CODE) },
                onLink = { linkDialog = true },
                onTable = {
                    if (vm.tableGroup() != null) tableDialog = true else tableSizeDialog = true
                },
                onImage = { picker.launch("image/*") },
                onDraw = { showDraw = true },
                onDivider = vm::insertDivider,
                onAlign = vm::cycleAlign,
                onIndentMore = vm::indentMore,
                onIndentLess = vm::indentLess,
                fontSize = doc.activeSize(),
                baseFontSize = state.textSizeSp,
                onShrink = { vm.adjustTextSize(-1) },
                onGrow = { vm.adjustTextSize(1) },
                fontColor = doc.activeColor(),
                onColor = { colorDialog = true },
                modifier = Modifier.navigationBarsPadding().imePadding()
            )
        }
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
            GalleryViewer(
                attachments = attachments.filter { it.kind != "file" && !it.missing },
                startId = viewing.id,
                onDismiss = { viewer = null }
            )
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
                        InfoRow("Archived", if (note.isArchived) "Yes" else "No")
                        InfoRow("Images", attachments.count { it.kind != "file" }.toString())
                        InfoRow("Files", attachments.count { it.kind == "file" }.toString())
                        if (noteTags.isNotEmpty()) {
                            Text(
                                "Tags",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            Row(
                                Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                noteTags.forEach { t -> TagChip(t.name) }
                            }
                        }
                    }
                }
            )
        }

        if (showDraw) {
            Dialog(
                onDismissRequest = { showDraw = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                DrawDialog(
                    onDismiss = { showDraw = false },
                    onSave = { png -> vm.addDrawing(png); showDraw = false }
                )
            }
        }

        if (linkDialog) {
            val sel = doc.selection
            val selectedText = remember(doc, sel) {
                val r = doc.rendered()
                r.substring(sel.min.coerceIn(0, r.length), sel.max.coerceIn(0, r.length))
            }
            val existing = remember(doc, sel) { vm.linkAtSelection()?.url }
            if (sel.min >= sel.max && existing == null) {
                LaunchedEffect(Unit) {
                    snackbar.showSnackbar("Select text first, then add a link")
                    linkDialog = false
                }
            } else {
                LinkDialog(
                    selectedText = selectedText.ifBlank { "(link at cursor)" },
                    existingUrl = existing,
                    onDismiss = { linkDialog = false },
                    onApply = { url ->
                        vm.setLink(url)
                        linkDialog = false
                    },
                    onRemove = { vm.removeLink(); linkDialog = false },
                    onInvalid = {
                        scope.launch {
                            snackbar.showSnackbar("That doesn't look like a valid URL")
                        }
                    }
                )
            }
        }

        if (tableSizeDialog) {
            TableSizeDialog(
                onDismiss = { tableSizeDialog = false },
                onCreate = { rows, cols -> tableSizeDialog = false; vm.insertTable(rows, cols) }
            )
        }

        if (tableDialog) {
            val group = remember(doc, doc.selection) { vm.tableGroup() }
            if (group == null) {
                LaunchedEffect(Unit) { tableDialog = false }
            } else {
                TableDialog(
                    initial = vm.tableCells(group),
                    onDismiss = { tableDialog = false },
                    onCommit = { grid -> vm.setTableCells(group, grid); tableDialog = false }
                )
            }
        }

        if (colorDialog) {
            TextColorDialog(
                paper = paper,
                ink = ink,
                accent = accent,
                current = doc.activeColor(),
                onDismiss = { colorDialog = false },
                onPick = { argb ->
                    vm.applyColor(argb?.let {
                        com.abhishek.inkora.ui.theme.guardedTextColor(it, paper).toArgb()
                    })
                }
            )
        }

        if (tagDialog && note != null) {
            TagsDialog(
                tags = noteTags,
                onDismiss = { tagDialog = false },
                onAdd = { vm.addTag(it) },
                onRemove = { vm.removeTag(it) }
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
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        listOf(
                            PaperBackground.WHITE, PaperBackground.CREAM, PaperBackground.GRAY,
                            PaperBackground.ORANGE, PaperBackground.YELLOW, PaperBackground.GREEN,
                            PaperBackground.BLUE, PaperBackground.DARK
                        ).forEach { bg ->
                            InkoraChip(
                                label = bg.key.replaceFirstChar(Char::titlecase),
                                selected = (note?.backgroundStyle == bg.key.lowercase()),
                                onClick = { vm.setPaperBackground(bg) },
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

/** Multi-tap bookkeeping for word/paragraph selection (kept out of composition). */
private class TapTracker {
    var count = 0
    var time = 0L
    /** Screen position of the last tap, in pixels. */
    var x = 0f
    var y = 0f
}

/**
 * Placement controls for the picture at the caret: in the text flow, behind
 * the text or in front of it, plus removal.
 */
@Composable
private fun ImageActionBar(
    placement: ImagePlacement,
    onInline: () -> Unit,
    onBehind: () -> Unit,
    onFront: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InkoraChip("Inline", selected = placement == ImagePlacement.INLINE, onClick = onInline)
        InkoraChip("Behind text", selected = placement == ImagePlacement.BEHIND, onClick = onBehind)
        InkoraChip("In front", selected = placement == ImagePlacement.IN_FRONT, onClick = onFront)
        InkoraChip("Remove image", selected = false, onClick = onRemove)
    }
}

/**
 * Which pass of the editor an inline image is painted in.
 *
 * BEHIND_PAINT draws the picture under the text, FRONT_PAINT draws it over
 * the text, BEHIND_HIT is an invisible, tappable copy of a floating picture
 * so it can still be selected although the text sits on top of it.
 */
private enum class ImageLayer { BEHIND_PAINT, FRONT_PAINT, BEHIND_HIT }

/**
 * Paints the document's inline images over the text field. Positions come
 * from the text layout, so a picture always sits where its placeholder
 * character sits.
 */
@Composable
private fun InlineImageLayer(
    anchors: List<ImageAnchor>,
    layout: TextLayoutResult?,
    attachments: Map<Long, Attachment>,
    selectedLine: Int?,
    layer: ImageLayer,
    onSelect: (Int) -> Unit
) {
    if (layout == null || anchors.isEmpty()) return
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        anchors.forEach { anchor ->
            val behind = anchor.image.placement == ImagePlacement.BEHIND
            val wanted = when (layer) {
                ImageLayer.BEHIND_PAINT, ImageLayer.BEHIND_HIT -> behind
                ImageLayer.FRONT_PAINT -> !behind
            }
            if (!wanted) return@forEach
            val attachment = attachments[anchor.image.attachmentId] ?: return@forEach
            val rect = imageRect(layout, anchor) ?: return@forEach
            InlineImageBox(
                file = attachment.file,
                width = with(density) { rect.width.toDp() },
                height = with(density) { rect.height.toDp() },
                x = with(density) { rect.left.toDp() },
                y = with(density) { rect.top.toDp() },
                placement = anchor.image.placement,
                showImage = layer != ImageLayer.BEHIND_HIT,
                selected = anchor.line == selectedLine,
                onSelect = { onSelect(anchor.line) }
            )
        }
    }
}

/**
 * Screen box of an inline image, derived from the text layout: a picture in
 * the flow fills the line its placeholder reserved, a floating one hovers
 * over the paragraph it is anchored to.
 */
private fun imageRect(layout: TextLayoutResult, anchor: ImageAnchor): Rect? {
    val text = layout.layoutInput.text
    val offset = anchor.offset.coerceIn(0, text.length)
    val visualLine = layout.getLineForOffset(offset)
    if (visualLine < 0 || visualLine >= layout.lineCount) return null
    val top = layout.getLineTop(visualLine)
    val bottom = layout.getLineBottom(visualLine)
    val left = layout.getLineLeft(visualLine)
    val column = layout.size.width.toFloat()
    return when (anchor.image.placement) {
        ImagePlacement.INLINE -> Rect(left, top, column, bottom)
        else -> {
            val lineHeight = (bottom - top).coerceAtLeast(1f)
            Rect(left, top, left + column * 0.62f, bottom + lineHeight * 1.4f)
        }
    }
}

/** One inline picture, placed at the box the text layout reserved for it. */
@Composable
private fun InlineImageBox(
    file: File?,
    width: Dp,
    height: Dp,
    x: Dp,
    y: Dp,
    placement: ImagePlacement,
    showImage: Boolean,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        Modifier
            .offset(x = x, y = y)
            .size(width, height)
            .then(if (showImage && placement == ImagePlacement.BEHIND) Modifier.alpha(0.35f) else Modifier)
            .clickable { onSelect() }
    ) {
        if (showImage) {
            LocalImageFull(file, Modifier.fillMaxSize())
        }
        if (selected) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
            )
        }
    }
}

private fun formatDate(millis: Long): String =
    java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
        .format(java.util.Date(millis))

/** Open a generic attachment with an external viewer via FileProvider. */
private fun openAttachment(context: android.content.Context, a: Attachment): Boolean {
    val file = a.file ?: return false
    return runCatching {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.files", file
        )
        val view = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, a.mimeType)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(view, "Open ${a.fileName}"))
        true
    }.getOrDefault(false)
}

/** Swipeable full-screen gallery over a note's images. Simple, no zoom engine. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryViewer(
    attachments: List<Attachment>,
    startId: Long,
    onDismiss: () -> Unit
) {
    if (attachments.isEmpty()) return
    val start = attachments.indexOfFirst { it.id == startId }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = start) { attachments.size }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                val a = attachments[page]
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        LocalImageFull(a.file, Modifier.fillMaxSize())
                    }
                    Text(
                        "${page + 1} of ${attachments.size} · ${a.fileName}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close gallery", tint = Color.White)
            }
        }
    }
}

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
