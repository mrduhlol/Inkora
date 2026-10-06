package com.abhishek.inkora.features.handwriting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.inkora.data.repository.HandwritingRepository
import com.abhishek.inkora.data.repository.SettingsRepository
import com.abhishek.inkora.domain.model.HwStroke
import com.abhishek.inkora.domain.model.HwTool
import com.abhishek.inkora.domain.model.INK_COLORS
import com.abhishek.inkora.domain.model.MAX_STROKES
import com.abhishek.inkora.domain.model.Note
import com.abhishek.inkora.domain.model.PEN_WIDTHS
import com.abhishek.inkora.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HandwritingUiState(
    val note: Note? = null,
    val strokes: List<HwStroke> = emptyList(),
    val tool: HwTool = HwTool.PEN,
    val colorArgb: Int = INK_COLORS[0],
    val widthPx: Float = PEN_WIDTHS[1],
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val notFound: Boolean = false,
    val savedTick: Long = 0L,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val strokeLimitHit: Boolean = false
)

private const val HW_UNDO_CAP = 50

@HiltViewModel
class HandwritingViewModel @Inject constructor(
    private val notes: NoteRepository,
    private val handwriting: HandwritingRepository,
    private val settings: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: 0L
    private val _state = MutableStateFlow(HandwritingUiState())
    val state: StateFlow<HandwritingUiState> = _state
    private var saveJob: Job? = null
    private val undoStack = ArrayDeque<List<HwStroke>>()
    private val redoStack = ArrayDeque<List<HwStroke>>()

    init {
        viewModelScope.launch {
            val existing = notes.getById(noteId)
            if (existing == null || existing.isDeleted) {
                _state.value = HandwritingUiState(notFound = true)
            } else {
                val prefs = settings.settings.first()
                var note = existing
                if (note.title.isBlank()) {
                    // Brand-new hand notes get paper defaults like text notes.
                    note = note.copy(
                        backgroundStyle = note.backgroundStyle.ifBlank { prefs.defaultBackground.name.lowercase() },
                        pageStyle = note.pageStyle.ifBlank { prefs.defaultPageStyle.name.lowercase() }
                    )
                }
                val strokes = handwriting.load(noteId)
                _state.value = HandwritingUiState(note = note, strokes = strokes)
            }
        }
    }

    fun setTool(tool: HwTool) {
        _state.value = _state.value.copy(tool = tool)
    }

    fun setColor(argb: Int) {
        _state.value = _state.value.copy(colorArgb = argb)
    }

    fun setWidth(px: Float) {
        _state.value = _state.value.copy(widthPx = px)
    }

    /** Commit a finished stroke (pointer up). One undo step; autosaves debounced. */
    fun addStroke(stroke: HwStroke) {
        val cur = _state.value.strokes
        if (cur.size >= MAX_STROKES) {
            _state.value = _state.value.copy(strokeLimitHit = true)
            return
        }
        undoStack.addLast(cur)
        if (undoStack.size > HW_UNDO_CAP) undoStack.removeFirst()
        redoStack.clear()
        _state.value = _state.value.copy(
            strokes = cur + stroke,
            canUndo = true,
            canRedo = false
        )
        scheduleSave()
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_state.value.strokes)
        _state.value = _state.value.copy(strokes = prev, canUndo = undoStack.isNotEmpty(), canRedo = true)
        scheduleSave()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_state.value.strokes)
        _state.value = _state.value.copy(strokes = next, canUndo = true, canRedo = redoStack.isNotEmpty())
        scheduleSave()
    }

    fun clearAll() {
        if (_state.value.strokes.isEmpty()) return
        undoStack.addLast(_state.value.strokes)
        redoStack.clear()
        _state.value = _state.value.copy(strokes = emptyList(), canUndo = true, canRedo = false)
        scheduleSave()
    }

    fun eraseAt(predicate: (HwStroke) -> Boolean) {
        val kept = _state.value.strokes.filterNot(predicate)
        if (kept.size == _state.value.strokes.size) return
        undoStack.addLast(_state.value.strokes)
        if (undoStack.size > HW_UNDO_CAP) undoStack.removeFirst()
        redoStack.clear()
        _state.value = _state.value.copy(strokes = kept, canUndo = true, canRedo = false)
        scheduleSave()
    }

    fun dismissLimit() {
        _state.value = _state.value.copy(strokeLimitHit = false)
    }

    private fun scheduleSave() {
        _state.value = _state.value.copy(isSaving = true)
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(800L)
            val s = _state.value
            val ok = runCatching { handwriting.save(noteId, s.strokes) }.isSuccess
            _state.value = if (ok) {
                s.copy(isSaving = false, savedTick = System.currentTimeMillis(), saveError = null)
            } else {
                s.copy(isSaving = false, saveError = "Could not save ink — will retry")
            }
        }
    }

    fun dismissSaveError() {
        _state.value = _state.value.copy(saveError = null)
    }

    /** Flush pending autosave, e.g. on back navigation. */
    fun flushNow(onDone: () -> Unit = {}) {
        saveJob?.cancel()
        viewModelScope.launch {
            val s = _state.value
            s.note?.let { runCatching { handwriting.save(noteId, s.strokes) } }
            onDone()
        }
    }
}
