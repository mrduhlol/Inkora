package com.abhishek.inkora.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * V1 formatting toolbar. Every button performs a real Markdown edit on the
 * body text (bold/italic/underline/strike/bullets/numbered/checklist).
 * No fake buttons: all wired in EditorScreen.
 */
@Composable
fun FormattingToolbar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onStrike: () -> Unit,
    onBullet: () -> Unit,
    onNumbered: () -> Unit,
    onChecklist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(onClick = onBold) { Icon(Icons.Filled.FormatBold, "Bold") }
        IconButton(onClick = onItalic) { Icon(Icons.Filled.FormatItalic, "Italic") }
        IconButton(onClick = onUnderline) { Icon(Icons.Filled.FormatUnderlined, "Underline") }
        IconButton(onClick = onStrike) { Icon(Icons.Filled.FormatStrikethrough, "Strikethrough") }
        IconButton(onClick = onBullet) { Icon(Icons.Filled.FormatListBulleted, "Bullet list") }
        IconButton(onClick = onNumbered) { Icon(Icons.Filled.FormatListNumbered, "Numbered list") }
        IconButton(onClick = onChecklist) { Icon(Icons.Filled.CheckBox, "Checklist") }
    }
}

/** Markdown helpers: wrap selection or current line. Kept pure for testability. */
object MarkdownFormat {
    fun wrap(text: String, marker: String): String = "$marker$text$marker"
    fun prefixLines(text: String, prefix: (Int) -> String): String =
        text.lines().mapIndexed { i, l -> prefix(i) + l }.joinToString("\n")
}
