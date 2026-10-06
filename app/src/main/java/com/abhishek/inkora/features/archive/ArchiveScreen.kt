package com.abhishek.inkora.features.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhishek.inkora.domain.model.RichText
import com.abhishek.inkora.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ArchiveViewModel @Inject constructor(private val notes: NoteRepository) : ViewModel() {
    val items = notes.observeArchived().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun toggleFav(id: Long, fav: Boolean) = viewModelScope.launch { notes.setFavorite(id, !fav) }
    fun unarchive(id: Long) = viewModelScope.launch { notes.setArchived(id, false) }
}

/**
 * Archived notes stay fully intact and recoverable. Each row opens the note
 * or restores it to Home with Unarchive — never mixed up with Trash.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenHandwriting: (Long) -> Unit = onOpen,
    vm: ArchiveViewModel = hiltViewModel()
) {
    val items by vm.items.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("Archive") }
            )
        }
    ) { pad ->
        if (items.isEmpty()) {
            Text("Nothing archived. Archiving hides notes from Home without deleting them.", Modifier.padding(pad).padding(24.dp))
        } else {
            LazyColumn(Modifier.padding(pad)) {
                items(items, key = { it.id }) { n ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (n.noteType == com.abhishek.inkora.domain.model.NoteType.HANDWRITING) {
                                onOpenHandwriting(n.id)
                            } else {
                                onOpen(n.id)
                            }
                        }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                n.title.ifBlank { "Untitled" },
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                RichText.previewText(n.content, n.contentFormat).lines().firstOrNull().orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = { vm.unarchive(n.id) }) { Text("Unarchive") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
