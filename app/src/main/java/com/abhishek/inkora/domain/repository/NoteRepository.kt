package com.abhishek.inkora.domain.repository

import com.abhishek.inkora.domain.model.Note
import com.abhishek.inkora.domain.model.NoteType
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeActiveNotes(): Flow<List<Note>>
    fun observeTrash(): Flow<List<Note>>
    fun observeArchived(): Flow<List<Note>>
    fun observeFavorites(): Flow<List<Note>>
    fun observeNotesInFolder(folderId: Long?): Flow<List<Note>>
    fun observeNotesWithTag(tagId: Long): Flow<List<Note>>
    fun observeNoteIdsWithAttachments(): Flow<List<Long>>
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun getById(id: Long): Note?
    suspend fun createBlank(): Long
    /** Create a note of an explicit type (handwriting/todo prefill handled by callers). */
    suspend fun createNote(type: NoteType): Long
    suspend fun upsert(note: Note): Long
    suspend fun moveToTrash(id: Long)
    suspend fun restore(id: Long)
    suspend fun deleteForever(id: Long)
    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun setPinned(id: Long, pinned: Boolean)
    suspend fun setArchived(id: Long, archived: Boolean)
    suspend fun moveToFolder(id: Long, folderId: Long?)
    /** Independent copy with "(Copy)" title. Never shares the row; never trashed. */
    suspend fun duplicate(id: Long): Long?
}
