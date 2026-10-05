package com.abhishek.inkora.domain.repository

import com.abhishek.inkora.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun observeTags(): Flow<List<Tag>>
    fun observeTagsForNote(noteId: Long): Flow<List<Tag>>
    suspend fun attach(noteId: Long, rawName: String): Long
    suspend fun detach(noteId: Long, tagId: Long)
    suspend fun deleteTag(tagId: Long)
}
