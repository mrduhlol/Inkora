package com.abhishek.inkora.features.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.launch
import com.abhishek.inkora.data.local.database.TagDao
import com.abhishek.inkora.data.local.database.entities.NoteTagCrossRef
import com.abhishek.inkora.data.repository.AttachmentRepository
import com.abhishek.inkora.data.repository.AttachmentRepository.AttachmentBackup
import com.abhishek.inkora.data.repository.DataRepository
import com.abhishek.inkora.domain.model.Note
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
    private val tags: TagDao,
    private val data: DataRepository
) : ViewModel() {
    val trash = notes.observeTrash().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _cleared = MutableStateFlow<Int?>(null)
    val cleared: StateFlow<Int?> = _cleared
    fun restore(id: Long) = viewModelScope.launch { notes.restore(id) }

    private data class DeletedNote(val note: Note, val files: List<AttachmentBackup>, val tagIds: List<Long>)
    private var lastDeleted: DeletedNote? = null

    /**
     * Permanent delete with a real undo window: the note row, tag links and
     * attachment bytes are cached first. [onDone] reports whether undo is
     * available (huge attachments skip the cache to protect memory).
     */
    fun deleteForever(id: Long, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val note = notes.getById(id)
        val files = attachments.snapshotForNote(id)
        if (note != null && files != null) {
            val tagIds = runCatching { tags.listTagsForNote(id).map { it.id } }.getOrDefault(emptyList())
            attachments.removeForNote(id)
            notes.deleteForever(id)
            lastDeleted = DeletedNote(note, files, tagIds)
            onDone(true)
        } else {
            if (note != null) {
                attachments.removeForNote(id)
                notes.deleteForever(id)
            }
            onDone(false)
        }
    }

    fun undoDelete() {
        val deleted = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch {
            val id = notes.upsert(deleted.note.copy(isDeleted = false))
            deleted.files.forEach { attachments.restoreSnapshot(id, it) }
            deleted.tagIds.forEach { runCatching { tags.link(NoteTagCrossRef(id, it)) } }
        }
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
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
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
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(n.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium)
                    Text(
                        com.abhishek.inkora.domain.model.RichText
                            .previewText(n.content, n.contentFormat).take(80),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Actions get a full-width row of their own, so Restore and
                    // Delete can never overlap — even on narrow screens.
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { vm.restore(n.id) }) { Text("Restore") }
                        TextButton(onClick = { pendingDelete = n.id }) { Text("Delete") }
                    }
                }
                HorizontalDivider()
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
                    TextButton(onClick = {
                        pendingDelete = null
                        vm.deleteForever(doomed) { undoable ->
                            scope.launch {
                                if (undoable && snackbar.showSnackbar("Note deleted", actionLabel = "UNDO") ==
                                    SnackbarResult.ActionPerformed
                                ) {
                                    vm.undoDelete()
                                }
                            }
                        }
                    }) {
                        Text("Delete forever")
                    }
                },
                dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Keep") } },
                title = { Text("Delete forever?") },
                text = { Text("You can undo right after deleting. Restore keeps everything instead.") }
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
