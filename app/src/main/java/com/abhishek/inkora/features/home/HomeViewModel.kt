package com.abhishek.inkora.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.inkora.data.repository.SettingsRepository
import com.abhishek.inkora.domain.model.Note
import com.abhishek.inkora.domain.model.SortOrder
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
    val notes: List<Note> = emptyList(),
    val query: String = "",
    val gridColumns: Int = 0,
    val isSearching: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val notes: NoteRepository,
    settingsRepo: SettingsRepository
) : ViewModel() {
    private val query = MutableStateFlow("")

    val state: StateFlow<HomeUiState> = combine(
        query.debounce(250L),
        settingsRepo.settings,
        notes.observeActiveNotes()
    ) { q, settings, all -> Triple(q, settings, all) }
        .flatMapLatest { (q, settings, all) ->
            if (q.isBlank()) {
                kotlinx.coroutines.flow.flowOf(
                    HomeUiState(sort(all, settings.sortOrder), "", settings.gridColumns, false)
                )
            } else {
                notes.searchNotes(q.trim()).combine(settingsRepo.settings) { found, s ->
                    HomeUiState(sort(found, s.sortOrder), q, s.gridColumns, true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(v: String) { query.value = v }

    fun toggleFavorite(id: Long, fav: Boolean) {
        viewModelScope.launch { notes.setFavorite(id, !fav) }
    }

    suspend fun createNote(): Long = notes.createBlank()

    private fun sort(list: List<Note>, order: SortOrder): List<Note> = when (order) {
        SortOrder.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
        SortOrder.CREATED_DESC -> list.sortedByDescending { it.createdAt }
        SortOrder.UPDATED_DESC -> list.sortedByDescending { it.updatedAt }
    }
}
