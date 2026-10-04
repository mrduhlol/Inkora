package com.abhishek.inkora.domain.repository

import com.abhishek.inkora.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeActiveNotes(): Flow<List<Note>>
    fun observeTrash(): Flow<List<Note>>
    fun observeArchived(): Flow<List<Note>>
    fun observeFavorites(): Flow<List<Note>>
    fun observeNotesInFolder(folderId: Long?): Flow<List<Note>>
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun getById(id: Long): Note?
    suspend fun createBlank(): Note
    suspend fun upsert(note: Note): Long
    suspend fun moveToTrash(id: Long)
    suspend fun restore(id: Long)
    suspend fun deleteForever(id: Long)
    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun setArchived(id: Long, archived: Boolean)
    suspend fun moveToFolder(id: Long, folderId: Long?)
}
