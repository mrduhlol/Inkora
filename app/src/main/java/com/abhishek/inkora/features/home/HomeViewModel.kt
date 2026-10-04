package com.abhishek.inkora.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.inkora.data.repository.SettingsRepository
import com.abhishek.inkora.domain.model.Folder
import com.abhishek.inkora.domain.model.HomeViewMode
import com.abhishek.inkora.domain.model.Note
import com.abhishek.inkora.domain.model.SortOrder
import com.abhishek.inkora.domain.repository.FolderRepository
import com.abhishek.inkora.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val pinned: List<Note> = emptyList(),
    val notes: List<Note> = emptyList(), // unpinned, sorted
    val query: String = "",
    val viewMode: HomeViewMode = HomeViewMode.GRID,
    val sortOrder: SortOrder = SortOrder.UPDATED_DESC,
    val gridColumns: Int = 0,
    val isSearching: Boolean = false,
    val selection: Set<Long> = emptySet(),
    val folders: List<Folder> = emptyList()
) {
    val selecting: Boolean get() = selection.isNotEmpty()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val notes: NoteRepository,
    private val folders: FolderRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selection = MutableStateFlow(emptySet<Long>())

    val state: StateFlow<HomeUiState> = combine(
        query.debounce(250L),
        settingsRepo.settings,
        notes.observeActiveNotes(),
        folders.observeFolders(),
        selection
    ) { q, settings, all, folderList, sel -> Quint(q, settings, all, folderList, sel) }
        .flatMapLatest { (q, settings, all, folderList, sel) ->
            if (q.isBlank()) {
                val (pinned, rest) = partition(all, settings.sortOrder)
                kotlinx.coroutines.flow.flowOf(
                    HomeUiState(pinned, rest, "", settings.viewMode, settings.sortOrder, settings.gridColumns, false, sel, folderList)
                )
            } else {
                notes.searchNotes(q.trim()).combine(settingsRepo.settings) { found, s ->
                    val (pinned, rest) = partition(found, s.sortOrder)
                    HomeUiState(pinned, rest, q, s.viewMode, s.sortOrder, s.gridColumns, true, sel, folderList)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private data class Quint(
        val q: String,
        val s: com.abhishek.inkora.data.repository.InkoraSettings,
        val all: List<Note>,
        val folders: List<Folder>,
        val sel: Set<Long>
    )

    private fun partition(all: List<Note>, order: SortOrder): Pair<List<Note>, List<Note>> {
        val (pinned, rest) = all.partition { it.isPinned }
        return sort(pinned, order) to sort(rest, order)
    }

    fun onQueryChange(v: String) { query.value = v }

    fun toggleFavorite(id: Long, fav: Boolean) {
        viewModelScope.launch { notes.setFavorite(id, !fav) }
    }

    fun togglePin(id: Long, pinned: Boolean) {
        viewModelScope.launch { notes.setPinned(id, !pinned) }
    }

    fun setSort(order: SortOrder) {
        viewModelScope.launch { settingsRepo.setSort(order) }
    }

    fun setViewMode(mode: HomeViewMode) {
        viewModelScope.launch { settingsRepo.setViewMode(mode) }
    }

    suspend fun createNote(): Long = notes.createBlank()

    suspend fun duplicate(id: Long): Long? = notes.duplicate(id)

    // ---------- multi-select ----------

    fun toggleSelect(id: Long) {
        selection.value = if (selection.value.contains(id)) selection.value - id else selection.value + id
    }

    fun clearSelection() { selection.value = emptySet() }

    fun trashSelected() {
        val ids = selection.value
        clearSelection()
        viewModelScope.launch { ids.forEach { notes.moveToTrash(it) } }
    }

    fun favoriteSelected(fav: Boolean) {
        val ids = selection.value
        viewModelScope.launch { ids.forEach { notes.setFavorite(it, fav) } }
        clearSelection()
    }

    fun pinSelected(pin: Boolean) {
        val ids = selection.value
        viewModelScope.launch { ids.forEach { notes.setPinned(it, pin) } }
        clearSelection()
    }

    fun moveSelected(folderId: Long?) {
        val ids = selection.value
        viewModelScope.launch { ids.forEach { notes.moveToFolder(it, folderId) } }
        clearSelection()
    }

    private fun sort(list: List<Note>, order: SortOrder): List<Note> = when (order) {
        SortOrder.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
        SortOrder.TITLE_DESC -> list.sortedByDescending { it.title.lowercase() }
        SortOrder.CREATED_DESC -> list.sortedByDescending { it.createdAt }
        SortOrder.UPDATED_DESC -> list.sortedByDescending { it.updatedAt }
    }
}
