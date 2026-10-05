package com.abhishek.inkora.features.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhishek.inkora.domain.model.CardDensity
import com.abhishek.inkora.domain.model.HomeViewMode
import com.abhishek.inkora.domain.model.Note
import com.abhishek.inkora.domain.model.SortOrder
import com.abhishek.inkora.ui.components.EmptyNotesState
import com.abhishek.inkora.ui.components.InkoraFab
import com.abhishek.inkora.ui.components.InkoraPaperPreview
import com.abhishek.inkora.ui.components.NoteListRow
import kotlinx.coroutines.launch

/**
 * Home: INKORA wordmark, search, compact sort + view controls, Pinned / All
 * Notes sections, bottom-LEFT + FAB. Long-press enters multi-select with
 * pin, favorite, folder and trash batch actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenNote: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenArchive: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val tags by vm.allTags.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var overflow by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var folderDialog by remember { mutableStateOf(false) }
    var templateSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = state.selecting) { vm.clearSelection() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (!state.selecting) {
                Box(Modifier.fillMaxWidth().padding(start = 32.dp, end = 16.dp), contentAlignment = Alignment.BottomStart) {
                    InkoraFab(onClick = {
                        scope.launch {
                            val id = vm.createNote()
                            onOpenNote(id)
                        }
                    })
                }
            }
        }
    ) { pad ->
        // Comfortable reading width on tablets/landscape; phones use full width.
        Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 900.dp)) {
            if (state.selecting) {
                SelectionBar(
                    count = state.selection.size,
                    onClose = vm::clearSelection,
                    onPin = { vm.pinSelected(true); scope.launch { snackbar.showSnackbar("Pinned") } },
                    onFavorite = { vm.favoriteSelected(true); scope.launch { snackbar.showSnackbar("Added to favorites") } },
                    onArchive = {
                        val n = state.selection.size
                        vm.archiveSelected()
                        scope.launch { snackbar.showSnackbar("Archived $n note(s)") }
                    },
                    onFolder = { folderDialog = true },
                    onTrash = {
                        val n = state.selection.size
                        vm.trashSelected()
                        scope.launch { snackbar.showSnackbar("Moved $n note(s) to trash") }
                    }
                )
            } else {
                BrandRow(
                    onOpenFavorites = onOpenFavorites,
                    onOpenFolders = onOpenFolders,
                    onOpenSettings = onOpenSettings,
                    onOverflow = { overflow = true },
                    overflowExpanded = overflow,
                    onOverflowDismiss = { overflow = false },
                    onNewFromTemplate = { templateSheet = true },
                    onOpenTrash = onOpenTrash,
                    onOpenArchive = onOpenArchive
                )
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
            ControlsRow(
                viewMode = state.viewMode,
                sort = sortName(state.sortOrder),
                onToggleView = {
                    vm.setViewMode(
                        if (state.viewMode == HomeViewMode.GRID) HomeViewMode.LIST else HomeViewMode.GRID
                    )
                },
                sortExpanded = sortMenu,
                onSortClick = { sortMenu = true },
                onSortDismiss = { sortMenu = false },
                onSort = { vm.setSort(it); sortMenu = false }
            )
            FilterRow(
                filter = state.filter,
                tagFilterId = state.tagFilterId,
                tags = tags,
                onFilter = { vm.setFilter(it); if (it != HomeFilter.TAG) Unit },
                onTag = { vm.setTagFilter(it) },
                onClearTag = { vm.setTagFilter(null) }
            )
            val empty = state.pinned.isEmpty() && state.notes.isEmpty()
            when {
                empty && !state.isSearching -> EmptyNotesState()
                empty -> NoResultsState(query = state.query)
                state.viewMode == HomeViewMode.LIST -> NoteList(
                    pinned = state.pinned,
                    notes = state.notes,
                    selection = state.selection,
                    selecting = state.selecting,
                    hideContent = state.hidePreviews,
                    compact = state.density == CardDensity.COMPACT,
                    onOpen = onOpenNote,
                    onToggleSelect = vm::toggleSelect
                )
                else -> NoteSectionsGrid(
                    pinned = state.pinned,
                    notes = state.notes,
                    selection = state.selection,
                    selecting = state.selecting,
                    gridColumns = state.gridColumns,
                    hideContent = state.hidePreviews,
                    compact = state.density == CardDensity.COMPACT,
                    onOpen = onOpenNote,
                    onToggleFavorite = { id ->
                        (state.pinned + state.notes).firstOrNull { it.id == id }?.let {
                            vm.toggleFavorite(id, it.isFavorite)
                        }
                    },
                    onToggleSelect = vm::toggleSelect
                )
            }
        }
        }

        if (templateSheet) {
            TemplateSheet(
                onDismiss = { templateSheet = false },
                onPick = { t ->
                    templateSheet = false
                    scope.launch {
                        val id = vm.createFromTemplate(t)
                        onOpenNote(id)
                    }
                }
            )
        }

        if (folderDialog) {            val count = state.selection.size
            AlertDialog(
                onDismissRequest = { folderDialog = false },
                confirmButton = { TextButton(onClick = { folderDialog = false }) { Text("Done") } },
                title = { Text("Move $count to folder") },
                text = {
                    Column {
                        MoveTargetRow("No folder", false) { vm.moveSelected(null); folderDialog = false }
                        state.folders.forEach { f ->
                            MoveTargetRow(f.name, false) { vm.moveSelected(f.id); folderDialog = false }
                        }
                        if (state.folders.isEmpty()) {
                            Text(
                                "No folders yet — create one from the Folders screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun BrandRow(
    onOpenFavorites: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenSettings: () -> Unit,
    onOverflow: () -> Unit,
    overflowExpanded: Boolean,
    onOverflowDismiss: () -> Unit,
    onNewFromTemplate: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenArchive: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("INKORA", style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
        IconButton(onClick = onOpenFavorites) { Icon(Icons.Filled.Favorite, "Favorites") }
        IconButton(onClick = onOpenFolders) { Icon(Icons.Filled.Folder, "Folders") }
        IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
        Box {
            IconButton(onClick = onOverflow) { Icon(Icons.Filled.MoreVert, "More options") }
            DropdownMenu(expanded = overflowExpanded, onDismissRequest = onOverflowDismiss) {
                DropdownMenuItem(text = { Text("New from template") }, onClick = { onOverflowDismiss(); onNewFromTemplate() })
                DropdownMenuItem(text = { Text("Archive") }, onClick = { onOverflowDismiss(); onOpenArchive() })
                DropdownMenuItem(text = { Text("Trash") }, onClick = { onOverflowDismiss(); onOpenTrash() })
            }
        }
    }
}

@Composable
private fun ControlsRow(
    viewMode: HomeViewMode,
    sort: String,
    onToggleView: () -> Unit,
    sortExpanded: Boolean,
    onSortClick: () -> Unit,
    onSortDismiss: () -> Unit,
    onSort: (SortOrder) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            TextButton(onClick = onSortClick) {
                Icon(Icons.Filled.Sort, null, Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text(sort)
            }
            DropdownMenu(expanded = sortExpanded, onDismissRequest = onSortDismiss) {
                SortOrder.entries.forEach {
                    DropdownMenuItem(text = { Text(sortName(it)) }, onClick = { onSort(it) })
                }
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleView) {
            Icon(
                if (viewMode == HomeViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                contentDescription = if (viewMode == HomeViewMode.GRID) "List view" else "Grid view"
            )
        }
    }
}

private fun sortName(o: SortOrder): String = when (o) {
    SortOrder.UPDATED_DESC -> "Recently updated"
    SortOrder.CREATED_DESC -> "Recently created"
    SortOrder.TITLE_ASC -> "Title A–Z"
    SortOrder.TITLE_DESC -> "Title Z–A"
}

/** Single filter chip row: All, Favorites, Pinned, Archived, Images, then tags. */
@Composable
private fun FilterRow(
    filter: HomeFilter,
    tagFilterId: Long?,
    tags: List<com.abhishek.inkora.domain.model.Tag>,
    onFilter: (HomeFilter) -> Unit,
    onTag: (Long) -> Unit,
    onClearTag: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(selected = filter == HomeFilter.ALL && tagFilterId == null, onClick = { onClearTag(); onFilter(HomeFilter.ALL) }, label = { Text("All") })
        FilterChip(selected = filter == HomeFilter.FAVORITES, onClick = { onFilter(HomeFilter.FAVORITES) }, label = { Text("Favorites") })
        FilterChip(selected = filter == HomeFilter.PINNED, onClick = { onFilter(HomeFilter.PINNED) }, label = { Text("Pinned") })
        FilterChip(selected = filter == HomeFilter.ARCHIVED, onClick = { onFilter(HomeFilter.ARCHIVED) }, label = { Text("Archived") })
        FilterChip(selected = filter == HomeFilter.WITH_IMAGES, onClick = { onFilter(HomeFilter.WITH_IMAGES) }, label = { Text("Images") })
        tags.forEach { t ->
            FilterChip(
                selected = tagFilterId == t.id,
                onClick = { if (tagFilterId == t.id) onClearTag() else onTag(t.id) },
                label = { Text("#${t.name}") }
            )
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onPin: () -> Unit,
    onFavorite: () -> Unit,
    onArchive: () -> Unit,
    onFolder: () -> Unit,
    onTrash: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Cancel selection") }
        Text("$count selected", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onPin) { Icon(Icons.Filled.PushPin, "Pin") }
        IconButton(onClick = onFavorite) { Icon(Icons.Filled.Favorite, "Favorite") }
        IconButton(onClick = onArchive) { Icon(Icons.Filled.Inventory2, "Archive") }
        IconButton(onClick = onFolder) { Icon(Icons.Filled.Folder, "Move to folder") }
        IconButton(onClick = onTrash) { Icon(Icons.Filled.Delete, "Move to trash") }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
    )
}

@Composable
private fun NoteSectionsGrid(
    pinned: List<Note>,
    notes: List<Note>,
    selection: Set<Long>,
    selecting: Boolean,
    gridColumns: Int,
    hideContent: Boolean,
    compact: Boolean,
    onOpen: (Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onToggleSelect: (Long) -> Unit
) {
    val width = LocalConfiguration.current.screenWidthDp
    val adaptive = when {
        width >= 900 -> 4
        width >= 600 -> 3
        else -> 2
    }
    val columns = if (gridColumns in 1..4) gridColumns else adaptive
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (pinned.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader("Pinned") }
            items(pinned, key = { it.id }) { n ->
                InkoraPaperPreview(
                    note = n, onOpen = onOpen, onToggleFavorite = onToggleFavorite,
                    selected = selection.contains(n.id), selecting = selecting, onToggleSelect = onToggleSelect,
                    hideContent = hideContent, compact = compact
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader("All Notes") }
        }
        items(notes, key = { it.id }) { n ->
            InkoraPaperPreview(
                note = n, onOpen = onOpen, onToggleFavorite = onToggleFavorite,
                selected = selection.contains(n.id), selecting = selecting, onToggleSelect = onToggleSelect,
                hideContent = hideContent, compact = compact
            )
        }
    }
}

@Composable
private fun NoteList(
    pinned: List<Note>,
    notes: List<Note>,
    selection: Set<Long>,
    selecting: Boolean,
    hideContent: Boolean,
    compact: Boolean,
    onOpen: (Long) -> Unit,
    onToggleSelect: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (pinned.isNotEmpty()) {
            item { SectionHeader("Pinned") }
            items(pinned, key = { it.id }) { n ->
                NoteListRow(n, selection.contains(n.id), selecting, onOpen, onToggleSelect,
                    hideContent = hideContent, compact = compact)
            }
            item { SectionHeader("All Notes") }
        }
        items(notes, key = { it.id }) { n ->
            NoteListRow(n, selection.contains(n.id), selecting, onOpen, onToggleSelect,
                hideContent = hideContent, compact = compact)
        }
    }
}

@Composable
private fun MoveTargetRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, Modifier.weight(1f))
        RadioButton(selected = selected, onClick = onClick)
    }
}

/** Template picker sheet. The + FAB still creates a blank note instantly. */
@Composable
private fun TemplateSheet(onDismiss: () -> Unit, onPick: (com.abhishek.inkora.features.templates.NoteTemplate) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("New from template") },
        text = {
            Column {
                com.abhishek.inkora.features.templates.NoteTemplates.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().clickable(onClick = { onPick(t) }).padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, style = MaterialTheme.typography.bodyLarge)
                            Text(t.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    )
}

/** Intentional empty state for searches with no matches. */
@Composable
private fun NoResultsState(query: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Icon(
            Icons.Filled.Search, null, Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline
        )
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
