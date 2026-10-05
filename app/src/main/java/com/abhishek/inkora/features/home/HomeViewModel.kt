package com.abhishek.inkora.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.inkora.data.repository.SettingsRepository
import com.abhishek.inkora.domain.model.CardDensity
import com.abhishek.inkora.domain.model.Folder
import com.abhishek.inkora.domain.model.HomeViewMode
import com.abhishek.inkora.domain.model.Note
import com.abhishek.inkora.domain.model.SortOrder
import com.abhishek.inkora.domain.repository.FolderRepository
import com.abhishek.inkora.domain.repository.NoteRepository
import com.abhishek.inkora.domain.repository.TagRepository
import com.abhishek.inkora.features.templates.encoded
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

/** Home list filter. TAG scopes the list to [HomeUiState.tagFilterId]. */
enum class HomeFilter { ALL, FAVORITES, PINNED, ARCHIVED, WITH_IMAGES, TAG }

data class HomeUiState(
    val pinned: List<Note> = emptyList(),
    val notes: List<Note> = emptyList(), // unpinned, sorted
    val query: String = "",
    val viewMode: HomeViewMode = HomeViewMode.GRID,
    val sortOrder: SortOrder = SortOrder.UPDATED_DESC,
    val gridColumns: Int = 0,
    val density: CardDensity = CardDensity.COMFORTABLE,
    val hidePreviews: Boolean = false,
    val filter: HomeFilter = HomeFilter.ALL,
    val tagFilterId: Long? = null,
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
    private val tags: TagRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selection = MutableStateFlow(emptySet<Long>())
    private val filter = MutableStateFlow(HomeFilter.ALL)
    private val tagFilter = MutableStateFlow<Long?>(null)

    val allTags = tags.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<HomeUiState> = combine(
        query.debounce(250L),
        settingsRepo.settings,
        notes.observeActiveNotes(),
        notes.observeArchived(),
        notes.observeNoteIdsWithAttachments(),
        folders.observeFolders(),
        selection,
        filter,
        tagFilter
    ) { parts ->
        @Suppress("UNCHECKED_CAST")
        val q = parts[0] as String
        val settings = parts[1] as com.abhishek.inkora.data.repository.InkoraSettings
        val active = parts[2] as List<Note>
        val archived = parts[3] as List<Note>
        val withFiles = (parts[4] as List<Long>).toSet()
        val folderList = parts[5] as List<Folder>
        val sel = parts[6] as Set<Long>
        val f = parts[7] as HomeFilter
        val tagId = parts[8] as Long?
        Quint(q, settings, active, archived, withFiles, folderList, sel, f, tagId)
    }
        .flatMapLatest { (q, settings, active, archived, withFiles, folderList, sel, f, tagId) ->
            // ARCHIVED reads the archived stream; everything else reads active.
            // Search spans both so archived notes stay findable.
            if (q.isBlank() && tagId == null) {
                val base = if (f == HomeFilter.ARCHIVED) archived else active
                val (pinned, rest) = partition(applyFilter(base, f, withFiles), settings.sortOrder)
                kotlinx.coroutines.flow.flowOf(
                    HomeUiState(
                        pinned, rest, "", settings.viewMode, settings.sortOrder,
                        settings.gridColumns, settings.cardDensity, settings.hidePreviews,
                        f, null, false, sel, folderList
                    )
                )
            } else if (tagId != null && q.isBlank()) {
                notes.observeNotesWithTag(tagId).combine(settingsRepo.settings) { found, s ->
                    val (pinned, rest) = partition(applyFilter(found, f, withFiles), s.sortOrder)
                    HomeUiState(
                        pinned, rest, q, s.viewMode, s.sortOrder,
                        s.gridColumns, s.cardDensity, s.hidePreviews,
                        f, tagId, true, sel, folderList
                    )
                }
            } else {
                notes.searchNotes(q.trim()).combine(settingsRepo.settings) { found, s ->
                    val scoped = if (f == HomeFilter.ARCHIVED) found else found.filter { !it.isArchived }
                    val (pinned, rest) = partition(applyFilter(scoped, f, withFiles), s.sortOrder)
                    HomeUiState(
                        pinned, rest, q, s.viewMode, s.sortOrder,
                        s.gridColumns, s.cardDensity, s.hidePreviews,
                        f, tagId, true, sel, folderList
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private data class Quint(
        val q: String,
        val s: com.abhishek.inkora.data.repository.InkoraSettings,
        val active: List<Note>,
        val archived: List<Note>,
        val withFiles: Set<Long>,
        val folders: List<Folder>,
        val sel: Set<Long>,
        val filter: HomeFilter,
        val tagId: Long?
    )

    private fun applyFilter(all: List<Note>, f: HomeFilter, withFiles: Set<Long>): List<Note> =
        when (f) {
            HomeFilter.ALL, HomeFilter.ARCHIVED -> all
            HomeFilter.FAVORITES -> all.filter { it.isFavorite }
            HomeFilter.PINNED -> all.filter { it.isPinned }
            HomeFilter.WITH_IMAGES -> all.filter { it.id in withFiles }
            HomeFilter.TAG -> all // scoped by the tag stream already
        }

    private fun partition(all: List<Note>, order: SortOrder): Pair<List<Note>, List<Note>> {
        val (pinned, rest) = all.partition { it.isPinned }
        return sort(pinned, order) to sort(rest, order)
    }

    fun onQueryChange(v: String) { query.value = v }

    fun setFilter(f: HomeFilter) {
        filter.value = f
        if (f != HomeFilter.TAG) tagFilter.value = null
    }

    fun setTagFilter(tagId: Long?) {
        tagFilter.value = tagId
        filter.value = if (tagId == null) HomeFilter.ALL else HomeFilter.TAG
    }

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

    fun setDensity(d: CardDensity) {
        viewModelScope.launch { settingsRepo.setDensity(d) }
    }

    suspend fun createNote(): Long = notes.createBlank()

    suspend fun duplicate(id: Long): Long? = notes.duplicate(id)

    suspend fun createFromTemplate(t: com.abhishek.inkora.features.templates.NoteTemplate): Long =
        notes.upsert(
            Note(
                title = t.title,
                content = t.encoded(),
                contentFormat = com.abhishek.inkora.domain.model.RichText.FORMAT
            )
        )

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

    fun archiveSelected() {
        val ids = selection.value
        clearSelection()
        viewModelScope.launch { ids.forEach { notes.setArchived(it, true) } }
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
