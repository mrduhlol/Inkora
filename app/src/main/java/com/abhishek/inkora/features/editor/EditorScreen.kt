package com.abhishek.inkora.features.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhishek.inkora.domain.model.PageStyle
import com.abhishek.inkora.domain.model.PaperBackground
import com.abhishek.inkora.ui.components.FormattingToolbar
import com.abhishek.inkora.ui.components.InkoraPaperSurface
import com.abhishek.inkora.ui.components.InkoraTopBar
import com.abhishek.inkora.ui.components.MarkdownFormat
import com.abhishek.inkora.ui.components.PageStyleSelector

/**
 * Notebook-page editor: top bar (back/title/fav/more), large paper surface,
 * contextual bottom toolbar. Autosaves; no manual Save button.
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

    Scaffold(
        topBar = {
            InkoraTopBar(
                title = state.title,
                isFavorite = note?.isFavorite == true,
                onBack = { vm.flushNow(onBack) },
                onToggleFavorite = { vm.toggleFavorite() },
                onMore = { menu = true }
            )
        },
        bottomBar = {
            FormattingToolbar(
                onBold = { vm.applyBodyTransform { MarkdownFormat.wrap(it, "**") } },
                onItalic = { vm.applyBodyTransform { MarkdownFormat.wrap(it, "_") } },
                onUnderline = { vm.applyBodyTransform { MarkdownFormat.wrap(it, "<u>") } },
                onStrike = { vm.applyBodyTransform { MarkdownFormat.wrap(it, "~~") } },
                onBullet = { vm.applyBodyTransform { MarkdownFormat.prefixLines(it) { "- " } } },
                onNumbered = { vm.applyBodyTransform { MarkdownFormat.prefixLines(it) { i -> "${i + 1}. " } } },
                onChecklist = { vm.applyBodyTransform { MarkdownFormat.prefixLines(it) { "- [ ] " } } }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            InkoraPaperSurface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                background = note?.backgroundStyle ?: "cream",
                customHex = note?.backgroundColor,
                pageStyle = note?.pageStyle ?: "blank"
            ) {
                Column(Modifier.fillMaxSize().padding(18.dp)) {
                    TextField(
                        value = state.title,
                        onValueChange = vm::onTitleChange,
                        placeholder = { Text("Title") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    TextField(
                        value = state.body,
                        onValueChange = vm::onBodyChange,
                        placeholder = { Text("Start writing…") },
                        modifier = Modifier.fillMaxSize(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Page style") }, onClick = { menu = false; styleSheet = true })
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Move to Trash") },
                leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Delete, null) },
                onClick = { menu = false; vm.trash(onBack) }
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
                    PageStyleSelector( // reuse chips row for paper choices via style mapping
                        selected = PageStyle.fromKey(note?.pageStyle),
                        onSelect = {}
                    )
                    // Paper background shortcuts
                    androidx.compose.foundation.layout.Row {
                        listOf(PaperBackground.WHITE, PaperBackground.CREAM, PaperBackground.GRAY, PaperBackground.DARK)
                            .forEach { bg ->
                                androidx.compose.material3.FilterChip(
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
