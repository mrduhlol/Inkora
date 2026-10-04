package com.abhishek.inkora.features.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.inkora.data.repository.SettingsRepository
import com.abhishek.inkora.domain.model.Note
import com.abhishek.inkora.domain.model.PageStyle
import com.abhishek.inkora.domain.model.PaperBackground
import com.abhishek.inkora.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class EditorUiState(
    val note: Note? = null,
    val title: String = "",
    val body: String = "",
    val notFound: Boolean = false,
    val savedTick: Long = 0L
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val notes: NoteRepository,
    private val settings: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: 0L
    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val existing = notes.getById(noteId)
            if (existing == null || existing.isDeleted) {
                _state.value = EditorUiState(notFound = true)
            } else {
                // Apply defaults only to brand-new blank notes.
                val prefs = settings.settings.first()
                val withDefaults = if (existing.title.isBlank() && existing.content.isBlank()) {
                    existing.copy(
                        backgroundStyle = existing.backgroundStyle.ifBlank { prefs.defaultBackground.name.lowercase() },
                        pageStyle = existing.pageStyle.ifBlank { prefs.defaultPageStyle.name.lowercase() }
                    )
                } else existing
                _state.value = EditorUiState(note = withDefaults, title = withDefaults.title, body = withDefaults.content)
            }
        }
    }

    fun onTitleChange(v: String) {
        _state.value = _state.value.copy(title = v)
        scheduleSave()
    }

    fun onBodyChange(v: String) {
        _state.value = _state.value.copy(body = v)
        scheduleSave()
    }

    fun applyBodyTransform(t: (String) -> String) {
        _state.value = _state.value.copy(body = t(_state.value.body))
        scheduleSave()
    }

    fun toggleFavorite() {
        val n = _state.value.note ?: return
        viewModelScope.launch { notes.setFavorite(n.id, !n.isFavorite) }
        _state.value = _state.value.copy(note = n.copy(isFavorite = !n.isFavorite))
    }

    fun setPageStyle(s: PageStyle) = persistStyle { it.copy(pageStyle = s.key) }
    fun setPaperBackground(b: PaperBackground, customHex: String? = null) =
        persistStyle { it.copy(backgroundStyle = b.key.lowercase(), backgroundColor = customHex ?: it.backgroundColor) }

    fun trash(onDone: () -> Unit) {
        val n = _state.value.note ?: return
        viewModelScope.launch {
            notes.upsert(n.copy(title = _state.value.title, content = _state.value.body))
            notes.moveToTrash(n.id)
            onDone()
        }
    }

    private fun persistStyle(map: (Note) -> Note) {
        val n = _state.value.note ?: return
        val updated = map(n)
        _state.value = _state.value.copy(note = updated)
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400L) // debounce: avoid DB write per keystroke, still lossless
            val s = _state.value
            val n = s.note ?: return@launch
            val updated = n.copy(title = s.title, content = s.body)
            notes.upsert(updated)
            _state.value = s.copy(note = updated, savedTick = System.currentTimeMillis())
        }
    }

    /** Flush pending autosave, e.g. on back navigation. */
    fun flushNow(onDone: () -> Unit = {}) {
        saveJob?.cancel()
        viewModelScope.launch {
            val s = _state.value
            s.note?.let { notes.upsert(it.copy(title = s.title, content = s.body)) }
            onDone()
        }
    }
}
