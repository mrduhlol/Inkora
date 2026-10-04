package com.abhishek.inkora.features.editor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhishek.inkora.domain.model.BlockKind
import com.abhishek.inkora.domain.model.PageStyle
import com.abhishek.inkora.domain.model.PaperBackground
import com.abhishek.inkora.domain.model.SpanKind
import com.abhishek.inkora.ui.components.FormattingToolbar
import com.abhishek.inkora.ui.components.InkoraPaperSurface
import com.abhishek.inkora.ui.components.InkoraTopBar
import com.abhishek.inkora.ui.components.PageStyleSelector
import com.abhishek.inkora.ui.theme.mutedOnPaperColor
import com.abhishek.inkora.ui.theme.onPaperColor
import com.abhishek.inkora.ui.theme.paperColorFor

/**
 * Notebook-page editor over [RichDoc]: selection-scoped spans, generated list
 * glyphs, checkbox tap-to-toggle, undo/redo and autosave.
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
    var menu by remember { mutableStateOf(false) }
    var styleSheet by remember { mutableStateOf(false) }
    val note = state.note

    if (state.notFound) {
        Scaffold { pad ->
            Text("Note not found or in Trash.", Modifier.padding(pad).padding(24.dp))
        }
        return
    }

    val paper = paperColorFor(note?.backgroundStyle ?: "cream", note?.backgroundColor)
    val ink = onPaperColor(paper)
    val muted = mutedOnPaperColor(paper)
    val doc = state.doc
    val annotated = remember(doc, muted) { doc.render(muted) }
    val field = remember(annotated, doc.selection) { TextFieldValue(annotated, doc.selection) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val prefixRanges = remember(doc) { doc.prefixRanges() }

    Scaffold(
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
                        textStyle = TextStyle(color = ink, fontSize = 20.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = ink,
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
                            cursorBrush = SolidColor(ink),
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
            // Toolbar rides above the keyboard via IME insets; zero extra space
            // when the keyboard is dismissed.
            FormattingToolbar(
                active = doc.activeKinds(),
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
                modifier = Modifier.navigationBarsPadding().imePadding()
            )
        }

        if (styleSheet) {
            ModalBottomSheet(onDismissRequest = { styleSheet = false }, sheetState = rememberModalBottomSheetState()) {
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
