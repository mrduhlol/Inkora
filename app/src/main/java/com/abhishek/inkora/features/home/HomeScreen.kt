package com.abhishek.inkora.features.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhishek.inkora.ui.components.EmptyNotesState
import com.abhishek.inkora.ui.components.InkoraFab
import com.abhishek.inkora.ui.components.NoteGrid
import kotlinx.coroutines.launch

/**
 * Home: INKORA wordmark, search, settings, paper grid, bottom-LEFT + FAB.
 * FAB is bottom-left per spec (unusual on purpose) — start-aligned.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenNote: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenFolders: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            Box(Modifier.fillMaxWidth().padding(start = 32.dp, end = 16.dp), contentAlignment = Alignment.BottomStart) {
                InkoraFab(onClick = {
                    scope.launch {
                        val id = vm.createNote()
                        onOpenNote(id)
                    }
                })
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            // Brand row
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("INKORA", style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenFavorites) { Icon(Icons.Filled.Favorite, "Favorites") }
                IconButton(onClick = onOpenFolders) { Icon(Icons.Filled.Folder, "Folders") }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
            }
            SearchBar(
                query = state.query,
                onQueryChange = vm::onQueryChange,
                onSearch = {},
                active = false,
                onActiveChange = {},
                placeholder = { Text("Search notes…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {}
            if (state.notes.isEmpty() && !state.isSearching) {
                EmptyNotesState()
            } else if (state.notes.isEmpty()) {
                NoResultsState(query = state.query)
            } else {
                NoteGrid(
                    notes = state.notes,
                    onOpen = onOpenNote,
                    onToggleFavorite = { id ->
                        val n = state.notes.firstOrNull { it.id == id }
                        if (n != null) vm.toggleFavorite(id, n.isFavorite)
                    },
                    gridOverride = state.gridColumns,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/** Intentional empty state for searches with no matches. */
@Composable
private fun NoResultsState(query: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Icon(Icons.Filled.SearchOff, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))
        Text("No matching notes", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Nothing matches “$query”.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
