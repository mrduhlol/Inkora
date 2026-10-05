package com.abhishek.inkora.data.repository

import com.abhishek.inkora.data.local.database.NoteDao
import com.abhishek.inkora.data.local.database.entities.NoteEntity
import com.abhishek.inkora.domain.model.Note
import com.abhishek.inkora.domain.repository.NoteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private fun NoteEntity.toDomain() = Note(
    id = id, title = title, content = content, contentFormat = contentFormat,
    createdAt = createdAt, updatedAt = updatedAt,
    isFavorite = isFavorite, isArchived = isArchived, isDeleted = isDeleted,
    isPinned = isPinned,
    folderId = folderId, backgroundStyle = backgroundStyle,
    backgroundColor = backgroundColor, textColor = textColor, pageStyle = pageStyle
)

private fun Note.toEntity(now: Long = System.currentTimeMillis()) = NoteEntity(
    id = id, title = title, content = content, contentFormat = contentFormat,
    createdAt = if (createdAt == 0L) now else createdAt,
    updatedAt = now,
    isFavorite = isFavorite, isArchived = isArchived, isDeleted = isDeleted,
    isPinned = isPinned,
    folderId = folderId, backgroundStyle = backgroundStyle,
    backgroundColor = backgroundColor, textColor = textColor, pageStyle = pageStyle
)

class NoteRepositoryImpl @Inject constructor(
    private val dao: NoteDao
) : NoteRepository {
    override fun observeActiveNotes(): Flow<List<Note>> = dao.observeActive().map { it.map(NoteEntity::toDomain) }
    override fun observeTrash(): Flow<List<Note>> = dao.observeTrash().map { it.map(NoteEntity::toDomain) }
    override fun observeArchived(): Flow<List<Note>> = dao.observeArchived().map { it.map(NoteEntity::toDomain) }
    override fun observeFavorites(): Flow<List<Note>> = dao.observeFavorites().map { it.map(NoteEntity::toDomain) }
    override fun observeNotesInFolder(folderId: Long?): Flow<List<Note>> =
        dao.observeInFolder(folderId).map { it.map(NoteEntity::toDomain) }
    override fun observeNotesWithTag(tagId: Long): Flow<List<Note>> =
        dao.observeNotesWithTag(tagId).map { it.map(NoteEntity::toDomain) }
    override fun observeNoteIdsWithAttachments(): Flow<List<Long>> = dao.observeNoteIdsWithAttachments()
    override fun searchNotes(query: String): Flow<List<Note>> = dao.search(query).map { it.map(NoteEntity::toDomain) }
    override suspend fun getById(id: Long): Note? = dao.getById(id)?.toDomain()
    override suspend fun createBlank(): Long = dao.upsert(NoteEntity())
    override suspend fun upsert(note: Note): Long = dao.upsert(note.toEntity())
    override suspend fun moveToTrash(id: Long) = dao.moveToTrash(id)
    override suspend fun restore(id: Long) = dao.restore(id)
    override suspend fun deleteForever(id: Long) = dao.deleteForever(id)
    override suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)
    override suspend fun setPinned(id: Long, pinned: Boolean) = dao.setPinned(id, pinned)
    override suspend fun setArchived(id: Long, archived: Boolean) = dao.setArchived(id, archived)
    override suspend fun moveToFolder(id: Long, folderId: Long?) = dao.moveToFolder(id, folderId)
    override suspend fun duplicate(id: Long): Long? {
        val src = dao.getById(id) ?: return null
        val now = System.currentTimeMillis()
        val base = if (src.title.isBlank()) "Untitled" else src.title
        return dao.upsert(
            src.copy(id = 0L, title = "$base (Copy)", isDeleted = false, createdAt = now, updatedAt = now)
        )
    }
}
