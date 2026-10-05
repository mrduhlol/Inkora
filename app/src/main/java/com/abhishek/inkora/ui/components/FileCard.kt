package com.abhishek.inkora.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.data.repository.Attachment

/** Compact card for a generic file attachment: name, type, size, open, remove. */
@Composable
fun FileCard(
    attachment: Attachment,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth(), onClick = onOpen) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    attachment.fileName.ifBlank { "Attachment" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${attachment.mimeType} · ${formatSize(attachment.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove ${attachment.fileName}")
            }
        }
    }
}

fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024f * 1024f))
}

/** Small tag chip with an optional remove affordance. */
@Composable
fun TagChip(
    name: String,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.FilterChip(
        selected = false,
        onClick = { onRemove?.invoke() },
        enabled = onRemove != null,
        label = { Text("#$name") },
        trailingIcon = if (onRemove != null) {
            { Icon(Icons.Filled.Close, contentDescription = "Remove tag $name", modifier = Modifier.size(14.dp)) }
        } else null,
        modifier = modifier
    )
}
