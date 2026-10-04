package com.abhishek.inkora.features.trash

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhishek.inkora.data.repository.AttachmentRepository
import com.abhishek.inkora.data.repository.DataRepository
import com.abhishek.inkora.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val notes: NoteRepository,
    private val attachments: AttachmentRepository,
    private val data: DataRepository
) : ViewModel() {
    val trash = notes.observeTrash().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _cleared = MutableStateFlow<Int?>(null)
    val cleared: StateFlow<Int?> = _cleared
    fun restore(id: Long) = viewModelScope.launch { notes.restore(id) }
    fun deleteForever(id: Long) = viewModelScope.launch {
        attachments.removeForNote(id)
        notes.deleteForever(id)
    }
    fun emptyTrash() = viewModelScope.launch { _cleared.value = data.clearTrash() }
    fun consumeCleared() { _cleared.value = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(onBack: () -> Unit, vm: TrashViewModel = hiltViewModel()) {
    val items by vm.trash.collectAsStateWithLifecycle()
    var pendingDelete: Long? by remember { mutableStateOf(null) }
    var confirmEmpty by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("Trash") },
                actions = {
                    if (items.isNotEmpty()) {
                        TextButton(onClick = { confirmEmpty = true }) { Text("Empty trash") }
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            items(items, key = { it.id }) { n ->
                ListItem(
                    headlineContent = { Text(n.title.ifBlank { "Untitled" }) },
                    supportingContent = {
                        Text(
                            com.abhishek.inkora.domain.model.RichText
                                .previewText(n.content, n.contentFormat).take(80)
                        )
                    },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { vm.restore(n.id) }) { Text("Restore") }
                            TextButton(onClick = { pendingDelete = n.id }) { Text("Delete") }
                        }
                    }
                )
            }
            if (items.isEmpty()) {
                item { Text("Trash is empty.", Modifier.padding(24.dp)) }
            }
        }
        val doomed = pendingDelete
        if (doomed != null) {
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                confirmButton = {
                    TextButton(onClick = { vm.deleteForever(doomed); pendingDelete = null }) {
                        Text("Delete forever")
                    }
                },
                dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Keep") } },
                title = { Text("Delete forever?") },
                text = { Text("This cannot be undone. Restore keeps everything instead.") }
            )
        }
        if (confirmEmpty) {
            AlertDialog(
                onDismissRequest = { confirmEmpty = false },
                confirmButton = {
                    TextButton(onClick = { vm.emptyTrash(); confirmEmpty = false }) {
                        Text("Empty trash")
                    }
                },
                dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text("Keep") } },
                title = { Text("Empty trash?") },
                text = { Text("All trashed notes and their images will be permanently deleted.") }
            )
        }
    }
}
