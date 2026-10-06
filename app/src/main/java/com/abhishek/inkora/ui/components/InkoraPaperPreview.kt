package com.abhishek.inkora.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.domain.model.Note
import com.abhishek.inkora.domain.model.RichText
import com.abhishek.inkora.ui.theme.InkoraTokens
import com.abhishek.inkora.ui.theme.mutedOnPaperColor
import com.abhishek.inkora.ui.theme.onPaperColor
import com.abhishek.inkora.ui.theme.paperColorFor

/**
 * Small sheet-of-paper preview with folded corner, real title + clean content.
 * Formatted notes render as readable text — raw markers are never shown.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InkoraPaperPreview(
    note: Note,
    onOpen: (Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selecting: Boolean = false,
    onToggleSelect: ((Long) -> Unit)? = null,
    hideContent: Boolean = false,
    compact: Boolean = false,
    query: String = ""
) {
    Box(
        modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(InkoraTokens.PaperCardRadius))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .combinedClickable(
                onClick = {
                    if (selecting) onToggleSelect?.invoke(note.id) else onOpen(note.id)
                },
                onLongClick = { onToggleSelect?.invoke(note.id) }
            )
            .semantics { contentDescription = "Open note ${note.title.ifBlank { "untitled" }}" }
    ) {
        val bgKey = note.backgroundStyle.ifBlank { "cream" }.let {
            // backgroundStyle doubles as paper color key in V1 for simplicity
            when (it) { "white", "gray", "dark", "custom" -> it; else -> "cream" }
        }
        val paper = paperColorFor(bgKey, note.backgroundColor)
        val ink = onPaperColor(paper)
        val muted = mutedOnPaperColor(paper)
        // Clean excerpt: rich payloads render with glyphs, legacy markers stripped.
        // Title falls back to the first content line; empty notes get a subtle hint.
        val excerpt = remember(note.content, note.contentFormat) {
            RichText.previewText(note.content, note.contentFormat)
        }
        val displayTitle = remember(note.title, note.content, note.contentFormat) {
            RichText.displayTitle(note.title, note.content, note.contentFormat)
        }
        val titleText = rememberHighlighted(displayTitle.ifBlank { "Untitled" }, query)
        val haptics = LocalHapticFeedback.current
        val bodyText = rememberHighlighted(
            if (hideContent) "Locked note" else excerpt.ifBlank { "No text yet" },
            query
        )
        InkoraPaperSurface(
            modifier = Modifier.fillMaxWidth().height(if (compact) 132.dp else 190.dp),
            background = bgKey,
            customHex = note.backgroundColor,
            pageStyle = note.pageStyle
        ) {
            Column(Modifier.fillMaxWidth().padding(if (compact) 10.dp else 14.dp)) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    color = ink,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    // Privacy mode masks the body so sensitive surfaces
                    // (recents, shoulders) never leak note content.
                    text = bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                    maxLines = if (compact) 2 else 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        FoldedCorner(
            modifier = Modifier.align(Alignment.TopEnd).size(28.dp),
            paperColor = paper
        )
        // Note-type identity: text notes carry no badge (no clutter).
        val typeBadge = when (note.noteType) {
            com.abhishek.inkora.domain.model.NoteType.HANDWRITING ->
                Icons.Filled.Edit to "Handwriting note"
            com.abhishek.inkora.domain.model.NoteType.TODO ->
                Icons.Filled.CheckBox to "To-do list"
            else -> null
        }
        if (typeBadge != null) {
            Icon(
                typeBadge.first,
                contentDescription = typeBadge.second,
                tint = muted,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp).size(16.dp)
            )
        }
        if (note.isFavorite) {
            Icon(
                Icons.Filled.Star,
                contentDescription = "Favorited",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(18.dp)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleFavorite(note.id)
                    }
            )
        }
        if (note.isPinned) {
            Icon(
                Icons.Filled.PushPin,
                contentDescription = "Pinned",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp).size(16.dp)
            )
        }
    }
}
