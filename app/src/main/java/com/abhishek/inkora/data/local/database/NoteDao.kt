package com.abhishek.inkora.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abhishek.inkora.data.local.database.entities.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun observeTrash(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 1 ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND ((:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId) ORDER BY updatedAt DESC")
    fun observeInFolder(folderId: Long?): Flow<List<NoteEntity>>

    // Offline search via Room; LIKE is enough for V1 scale. FTS can be added later.
    // Matches title, body and folder name (case-insensitive via LIKE).
    @Query(
        """SELECT * FROM notes WHERE isDeleted = 0 AND (
        title LIKE '%' || :query || '%' ESCAPE '\' OR content LIKE '%' || :query || '%' ESCAPE '\'
        OR folderId IN (SELECT id FROM folders WHERE name LIKE '%' || :query || '%' ESCAPE '\')
        ) ORDER BY updatedAt DESC"""
    )
    fun search(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NoteEntity): Long

    @Query("UPDATE notes SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun moveToTrash(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 0, updatedAt = :now WHERE id = :id")
    suspend fun restore(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteForever(id: Long)

    @Query("UPDATE notes SET isFavorite = :fav, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isPinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isArchived = :arch, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, arch: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET folderId = :folderId, updatedAt = :now WHERE id = :id")
    suspend fun moveToFolder(id: Long, folderId: Long?, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolder(folderId: Long)
}
