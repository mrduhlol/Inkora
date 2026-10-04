package com.abhishek.inkora.features.editor

import androidx.compose.ui.text.TextRange
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.inkora.data.repository.SettingsRepository
import com.abhishek.inkora.domain.model.BlockKind
import com.abhishek.inkora.domain.model.Note
import com.abhishek.inkora.domain.model.PageStyle
import com.abhishek.inkora.domain.model.PaperBackground
import com.abhishek.inkora.domain.model.RichText
import com.abhishek.inkora.domain.model.SpanKind
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
    val doc: RichDoc = RichDoc(),
    val notFound: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val textSizeSp: Int = 16,
    val savedTick: Long = 0L
)

private const val UNDO_CAP = 60

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
    private val undoStack = ArrayDeque<RichDoc>()
    private val redoStack = ArrayDeque<RichDoc>()

    init {
        viewModelScope.launch {
            val existing = notes.getById(noteId)
            if (existing == null || existing.isDeleted) {
                _state.value = EditorUiState(notFound = true)
            } else {
                val prefs = settings.settings.first()
                var note = existing
                // Migrate legacy content once: markers become spans, text preserved.
                val migrated = RichText.migrate(existing.content, existing.contentFormat)
                val needsMigration = existing.contentFormat != RichText.FORMAT
                if (needsMigration) {
                    note = note.copy(content = RichText.encode(migrated), contentFormat = RichText.FORMAT)
                    notes.upsert(note)
                }
                // Apply defaults only to brand-new blank notes.
                if (note.title.isBlank() && migrated.text.isBlank()) {
                    note = note.copy(
                        backgroundStyle = note.backgroundStyle.ifBlank { prefs.defaultBackground.name.lowercase() },
                        pageStyle = note.pageStyle.ifBlank { prefs.defaultPageStyle.name.lowercase() }
                    )
                }
                _state.value = EditorUiState(
                    note = note,
                    title = note.title,
                    doc = RichDoc.fromRich(migrated),
                    textSizeSp = prefs.defaultTextSizeSp
                )
            }
        }
    }

    // ---------- text input ----------

    fun onTitleChange(v: String) {
        _state.value = _state.value.copy(title = v)
        scheduleSave()
    }

    /** Raw rendered text + selection from BasicTextField. */
    fun onBodyInput(text: String, selection: TextRange) {
        val cur = _state.value.doc
        // Typing is not undoable per keystroke: push history only on structural
        // changes (formatting, blocks, Enter). Plain typing coalesces into the
        // last pushed state, keeping undo meaningful without memory churn.
        _state.value = _state.value.copy(doc = cur.onInput(text, selection))
        refreshUndo()
        scheduleSave()
    }

    // ---------- formatting (each is one undo step) ----------

    private fun pushUndo() {
        undoStack.addLast(_state.value.doc)
        if (undoStack.size > UNDO_CAP) undoStack.removeFirst()
        redoStack.clear()
        refreshUndo()
    }

    fun toggleSpan(kind: SpanKind) {
        pushUndo()
        _state.value = _state.value.copy(doc = _state.value.doc.toggleSpan(kind))
        refreshUndo()
        scheduleSave()
    }

    fun toggleBlock(kind: BlockKind) {
        pushUndo()
        _state.value = _state.value.copy(doc = _state.value.doc.toggleBlock(kind))
        refreshUndo()
        scheduleSave()
    }

    fun toggleCheck(line: Int) {
        pushUndo()
        _state.value = _state.value.copy(doc = _state.value.doc.toggleCheck(line))
        refreshUndo()
        scheduleSave()
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_state.value.doc)
        _state.value = _state.value.copy(doc = prev)
        refreshUndo()
        scheduleSave()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_state.value.doc)
        _state.value = _state.value.copy(doc = next)
        refreshUndo()
        scheduleSave()
    }

    private fun refreshUndo() {
        _state.value = _state.value.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty())
    }

    // ---------- note properties ----------

    fun toggleFavorite() {
        val n = _state.value.note ?: return
        viewModelScope.launch { notes.setFavorite(n.id, !n.isFavorite) }
        _state.value = _state.value.copy(note = n.copy(isFavorite = !n.isFavorite))
    }

    fun setPageStyle(s: PageStyle) = persistStyle { it.copy(pageStyle = s.key) }
    fun setPaperBackground(b: PaperBackground, customHex: String? = null) =
        persistStyle { it.copy(backgroundStyle = b.key.lowercase(), backgroundColor = customHex ?: it.backgroundColor) }

    fun trash(onDone: () -> Unit) {
        val s = _state.value
        val n = s.note ?: return
        viewModelScope.launch {
            notes.upsert(n.copy(title = s.title, content = RichText.encode(s.doc.toRich()), contentFormat = RichText.FORMAT))
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

    // ---------- autosave (debounced, lossless, includes formatting) ----------

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400L)
            saveNow()
        }
    }

    private suspend fun saveNow() {
        val s = _state.value
        val n = s.note ?: return
        val updated = n.copy(
            title = s.title,
            content = RichText.encode(s.doc.toRich()),
            contentFormat = RichText.FORMAT
        )
        notes.upsert(updated)
        _state.value = s.copy(note = updated, savedTick = System.currentTimeMillis())
    }

    /** Flush pending autosave, e.g. on back navigation. */
    fun flushNow(onDone: () -> Unit = {}) {
        saveJob?.cancel()
        viewModelScope.launch {
            saveNow()
            onDone()
        }
    }
}
