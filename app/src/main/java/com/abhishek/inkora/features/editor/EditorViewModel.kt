package com.abhishek.inkora.features.editor

import androidx.compose.ui.text.TextRange
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.inkora.data.repository.AttachmentRepository
import com.abhishek.inkora.data.repository.SettingsRepository
import com.abhishek.inkora.domain.model.BlockKind
import com.abhishek.inkora.domain.model.Note
import com.abhishek.inkora.domain.model.PageStyle
import com.abhishek.inkora.domain.model.PaperBackground
import com.abhishek.inkora.domain.model.RichText
import com.abhishek.inkora.domain.model.SpanKind
import com.abhishek.inkora.domain.repository.FolderRepository
import com.abhishek.inkora.domain.repository.NoteRepository
import com.abhishek.inkora.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditorUiState(
    val note: Note? = null,
    val title: String = "",
    val doc: RichDoc = RichDoc(),
    val notFound: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val textSizeSp: Int = 16,
    val savedTick: Long = 0L,
    val isSaving: Boolean = false,
    val saveError: String? = null
)

data class NoteStats(
    val words: Int,
    val chars: Int,
    val lines: Int
)

private const val UNDO_CAP = 60

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val notes: NoteRepository,
    private val settings: SettingsRepository,
    private val folders: FolderRepository,
    private val tags: TagRepository,
    private val attachmentsRepo: AttachmentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: 0L
    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state
    val allFolders = folders.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val noteTags = tags.observeTagsForNote(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val attachments = attachmentsRepo.observe(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
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

    fun cycleHeading() = structureOp { it.cycleHeading() }
    fun toggleQuote() = structureOp { it.toggleQuote() }
    fun toggleCode() = structureOp { it.toggleBlock(BlockKind.CODE) }
    fun insertDivider() = structureOp { it.insertDivider() }
    fun cycleAlign() = structureOp { it.cycleAlign() }
    fun indentMore() = structureOp { it.indentMore() }
    fun indentLess() = structureOp { it.indentLess() }

    // ---------- links (each is one undo step) ----------

    /** Attach a URL to the current selection. Returns false when unusable. */
    fun setLink(rawUrl: String): Boolean {
        val url = RichText.normalizeUrl(rawUrl) ?: return false
        pushUndo()
        _state.value = _state.value.copy(doc = _state.value.doc.setLink(url))
        refreshUndo()
        scheduleSave()
        return true
    }

    fun removeLink() = structureOp { it.removeLink() }

    fun linkAtSelection(): com.abhishek.inkora.domain.model.RichLink? =
        _state.value.doc.linkAtSelection()

    // ---------- tables (each is one undo step) ----------

    fun insertTable(rows: Int, cols: Int) = structureOp { it.insertTable(rows, cols) }

    fun tableGroup(): IntRange? = _state.value.doc.tableGroupAtCursor()

    fun tableCells(group: IntRange): List<List<String>> = _state.value.doc.tableCells(group)

    fun setTableCells(group: IntRange, grid: List<List<String>>) =
        structureOp { it.setTableCells(group, grid) }

    private fun structureOp(op: (RichDoc) -> RichDoc) {
        pushUndo()
        _state.value = _state.value.copy(doc = op(_state.value.doc))
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

    /** Assign to a folder (null = no folder). Never duplicates; persists via Room. */
    fun moveToFolder(folderId: Long?) {
        val n = _state.value.note ?: return
        viewModelScope.launch {
            notes.moveToFolder(n.id, folderId)
            _state.value = _state.value.copy(note = n.copy(folderId = folderId))
        }
    }

    // ---------- tags (metadata, never body syntax) ----------

    fun addTag(rawName: String) {
        viewModelScope.launch {
            runCatching { tags.attach(noteId, rawName) }.onFailure {
                _state.value = _state.value.copy(saveError = "Couldn't add that tag")
            }
        }
    }

    fun removeTag(tagId: Long) {
        viewModelScope.launch { tags.detach(noteId, tagId) }
    }

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
        _state.value = _state.value.copy(isSaving = true)
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
        // Data safety: a failed write keeps in-memory content and surfaces an
        // error instead of silently discarding; the next edit retries.
        val ok = runCatching { notes.upsert(updated) }.isSuccess
        _state.value = if (ok) {
            s.copy(note = updated, savedTick = System.currentTimeMillis(), isSaving = false, saveError = null)
        } else {
            s.copy(isSaving = false, saveError = "Could not save — will retry on your next edit")
        }
    }

    fun dismissSaveError() {
        _state.value = _state.value.copy(saveError = null)
    }

    // ---------- attachments (app-private files, Room metadata) ----------

    fun addImage(uri: Uri) {
        viewModelScope.launch {
            val id = attachmentsRepo.add(noteId, uri)
            if (id == null) {
                _state.value = _state.value.copy(saveError = "Couldn't add that image — try another file")
            } else {
                scheduleSave() // bump updatedAt so the note resurfaces
            }
        }
    }

    fun addDrawing(png: ByteArray) {
        viewModelScope.launch {
            runCatching {
                attachmentsRepo.storeBytes(noteId, "drawing-${System.currentTimeMillis()}.png", "image/png", png)
            }.onFailure {
                _state.value = _state.value.copy(saveError = "Couldn't save drawing")
            }.onSuccess {
                scheduleSave()
            }
        }
    }

    fun removeImage(id: Long) {
        viewModelScope.launch { attachmentsRepo.remove(id) }
    }

    fun addFile(uri: Uri) {
        viewModelScope.launch {
            val id = attachmentsRepo.addFile(noteId, uri)
            if (id == null) {
                _state.value = _state.value.copy(saveError = "Couldn't attach that file (25MB max)")
            } else {
                scheduleSave()
            }
        }
    }

    fun toggleArchiveState() {
        val n = _state.value.note ?: return
        viewModelScope.launch {
            notes.setArchived(n.id, !n.isArchived)
            _state.value = _state.value.copy(note = n.copy(isArchived = !n.isArchived))
        }
    }

    /** Lightweight stats, computed on demand for Note Info (never per keystroke). */
    fun stats(): NoteStats {
        val text = _state.value.doc.toRich().text
        val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
        return NoteStats(words, text.length, text.lines().size)
    }

    /** Readable plain-text share payload: title + clean body. */
    fun shareText(): String {
        val s = _state.value
        val body = RichText.plain(s.doc.toRich())
        return if (s.title.isBlank()) body else "${s.title}\n\n$body"
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
