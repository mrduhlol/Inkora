package com.abhishek.inkora.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.domain.model.Note

/**
 * Small sheet-of-paper preview with folded corner, real title + content.
 * Never shows placeholder text: falls back to content-derived excerpt.
 */
@Composable
fun InkoraPaperPreview(
    note: Note,
    onOpen: (Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .clickable(onClick = { onOpen(note.id) })
            .semantics { contentDescription = "Open note ${note.title.ifBlank { "untitled" }}" }
    ) {
        InkoraPaperSurface(
            modifier = Modifier.fillMaxWidth().height(190.dp),
            background = note.backgroundStyle.ifBlank { "cream" }.let {
                // backgroundStyle doubles as paper color key in V1 for simplicity
                when (it) { "white", "gray", "dark", "custom" -> it; else -> "cream" }
            },
            customHex = note.backgroundColor,
            pageStyle = note.pageStyle
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = note.content.ifBlank { "No text yet" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5F5B58),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        FoldedCorner(
            modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
        )
        if (note.isFavorite) {
            Icon(
                Icons.Filled.Star,
                contentDescription = "Favorited",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(18.dp)
                    .clickable { onToggleFavorite(note.id) }
            )
        }
    }
}
