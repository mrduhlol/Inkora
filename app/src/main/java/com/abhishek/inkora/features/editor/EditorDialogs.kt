package com.abhishek.inkora.features.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.domain.model.RichText
import com.abhishek.inkora.domain.model.Tag

/**
 * Insert/edit hyperlink. Shows the selected text (read-only context), a URL
 * field, and Open/Edit/Remove actions. The visible text is never modified —
 * only the underlying link is attached, changed or detached.
 */
@Composable
fun LinkDialog(
    selectedText: String,
    existingUrl: String?,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
    onRemove: () -> Unit,
    onInvalid: () -> Unit
) {
    var url by remember(existingUrl) { mutableStateOf(existingUrl ?: "") }
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (RichText.normalizeUrl(url) == null) onInvalid() else onApply(url)
            }) { Text(if (existingUrl == null) "Add link" else "Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(if (existingUrl == null) "Add link" else "Edit link") },
        text = {
            Column {
                Text(
                    "“${selectedText.take(80)}”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    placeholder = { Text("example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                if (existingUrl != null) {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            runCatching {
                                uriHandler.openUri(RichText.normalizeUrl(existingUrl) ?: existingUrl)
                            }
                        }) { Text("Open") }
                        OutlinedButton(onClick = onRemove) { Text("Remove link") }
                    }
                }
            }
        }
    )
}

/**
 * Structured table editor: editable cell grid with add/remove row/column.
 * Writes back through [onCommit]; the dialog never touches the document itself.
 */
@Composable
fun TableDialog(
    initial: List<List<String>>,
    onDismiss: () -> Unit,
    onCommit: (List<List<String>>) -> Unit
) {
    val grid = remember {
        mutableStateListOf<MutableList<String>>().apply {
            val base = initial.ifEmpty { listOf(listOf("", ""), listOf("", "")) }
            base.forEach { add(it.toMutableList()) }
        }
    }
    fun normalize() {
        val cols = grid.maxOf { it.size }.coerceAtLeast(1)
        grid.forEach { while (it.size < cols) it.add("") }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                normalize()
                onCommit(grid.map { it.toList() })
            }) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Edit table") },
        text = {
            Column(Modifier.widthIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                grid.forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                        row.forEachIndexed { c, cell ->
                            var v by remember(grid, r, c) { mutableStateOf(cell) }
                            OutlinedTextField(
                                value = v,
                                onValueChange = { v = it; grid[r][c] = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    OutlinedButton(onClick = {
                        normalize()
                        grid.add(MutableList(grid.firstOrNull()?.size ?: 1) { "" })
                    }) { Text("+ Row") }
                    OutlinedButton(onClick = {
                        if (grid.size > 1) grid.removeAt(grid.lastIndex)
                    }) { Text("− Row") }
                    OutlinedButton(onClick = {
                        normalize()
                        grid.forEach { it.add("") }
                    }) { Text("+ Col") }
/** Small rows×cols picker shown when inserting a brand-new table. */
@Composable
fun TableSizeDialog(
    onDismiss: () -> Unit,
    onCreate: (rows: Int, cols: Int) -> Unit
) {
    var rows by remember { mutableIntStateOf(2) }
    var cols by remember { mutableIntStateOf(2) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onCreate(rows, cols) }) { Text("Insert table") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("New table") },
        text = {
            Column {
                Text("Rows: $rows", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { n ->
                        FilterChip(selected = rows == n, onClick = { rows = n }, label = { Text("$n") })
                    }
                }
                Text("Columns: $cols", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..4).forEach { n ->
                        FilterChip(selected = cols == n, onClick = { cols = n }, label = { Text("$n") })
                    }
                }
            }
        }
    )
}

/**
 * Tag manager for one note. Tags are metadata chips — typing here never
 * touches the note body.
 */
@Composable
fun TagsDialog(
    tags: List<Tag>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (Long) -> Unit
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Tags") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("New tag") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (input.isNotBlank()) {
                            onAdd(input)
                            input = ""
                        }
                    }) { Icon(Icons.Filled.Add, "Add tag") }
                }
                if (tags.isEmpty()) {
                    Text(
                        "No tags yet. Tags help you find notes from Home.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                } else {
                    tags.forEach { t ->
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${t.name}", Modifier.weight(1f))
                            IconButton(onClick = { onRemove(t.id) }) {
                                Icon(Icons.Filled.Close, "Remove tag ${t.name}")
                            }
                        }
                    }
                }
            }
        }
    )
}
