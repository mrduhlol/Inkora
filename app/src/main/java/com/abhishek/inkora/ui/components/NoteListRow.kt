package com.abhishek.inkora.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.domain.model.Note
import com.abhishek.inkora.domain.model.RichText
import com.abhishek.inkora.ui.theme.paperColorFor

/** Subtle relative timestamp: "Updated 2 hours ago". */
fun relativeTime(millis: Long, now: Long = System.currentTimeMillis()): String {
    val s = ((now - millis) / 1000).coerceAtLeast(0)
    return when {
        s < 60 -> "Updated just now"
        s < 3600 -> "Updated ${s / 60} min ago"
        s < 86400 -> "Updated ${s / 3600} h ago"
        s < 86400 * 7 -> "Updated ${s / 86400} d ago"
        else -> "Updated " + java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
            .format(java.util.Date(millis))
    }
}

/**
 * Compact horizontal note row that keeps the paper identity: a mini paper
 * swatch with folded corner, title, clean excerpt and subtle metadata.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteListRow(
    note: Note,
    selected: Boolean,
    selecting: Boolean,
    onOpen: (Long) -> Unit,
    onToggleSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    hideContent: Boolean = false,
    compact: Boolean = false
) {
    val bgKey = note.backgroundStyle.ifBlank { "cream" }.let {
        when (it) { "white", "gray", "dark", "custom" -> it; else -> "cream" }
    }
    val paper = paperColorFor(bgKey, note.backgroundColor)
    val excerpt = remember(note.content, note.contentFormat) {
        RichText.previewText(note.content, note.contentFormat)
    }
    val title = remember(note.title, note.content, note.contentFormat) {
        RichText.displayTitle(note.title, note.content, note.contentFormat).ifBlank { "Untitled" }
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .combinedClickable(
                onClick = { if (selecting) onToggleSelect(note.id) else onOpen(note.id) },
                onLongClick = { onToggleSelect(note.id) }
            )
            .semantics { contentDescription = "Open note $title" }
            .padding(if (compact) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selecting) {
            Checkbox(checked = selected, onCheckedChange = { onToggleSelect(note.id) })
            Spacer(Modifier.width(4.dp))
        }
        // Mini paper swatch with folded corner.
        Box(
            Modifier.size(width = if (compact) 32.dp else 40.dp, height = if (compact) 42.dp else 52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(paper)
        ) {
            FoldedCorner(modifier = Modifier.align(Alignment.TopEnd).size(12.dp), paperColor = paper)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (note.isPinned) {
                    Icon(Icons.Filled.PushPin, contentDescription = "Pinned", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                }
                if (note.isFavorite) {
                    Icon(Icons.Filled.Star, contentDescription = "Favorited", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (!hideContent && excerpt.isNotBlank()) {
                Text(
                    excerpt.lines().firstOrNull().orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                relativeTime(note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
