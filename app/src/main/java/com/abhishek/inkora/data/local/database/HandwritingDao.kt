package com.abhishek.inkora.data.local.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.abhishek.inkora.data.local.database.entities.HandwritingDocEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HandwritingDao {
    @Query("SELECT * FROM handwriting_docs WHERE noteId = :noteId LIMIT 1")
    suspend fun get(noteId: Long): HandwritingDocEntity?

    @Query("SELECT * FROM handwriting_docs WHERE noteId = :noteId LIMIT 1")
    fun observe(noteId: Long): Flow<HandwritingDocEntity?>

    @Upsert
    suspend fun upsert(doc: HandwritingDocEntity)

    @Query("DELETE FROM handwriting_docs WHERE noteId = :noteId")
    suspend fun delete(noteId: Long)
}
