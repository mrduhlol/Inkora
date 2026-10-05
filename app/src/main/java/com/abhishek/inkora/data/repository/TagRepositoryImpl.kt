package com.abhishek.inkora.data.repository

import com.abhishek.inkora.data.local.database.TagDao
import com.abhishek.inkora.data.local.database.entities.TagEntity
import com.abhishek.inkora.domain.model.Tag
import com.abhishek.inkora.domain.repository.TagRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepositoryImpl @Inject constructor(
    private val dao: TagDao
) : TagRepository {
    override fun observeTags(): Flow<List<Tag>> =
        dao.observeTags().map { list -> list.map { Tag(it.id, it.name) } }

    override fun observeTagsForNote(noteId: Long): Flow<List<Tag>> =
        dao.observeTagsForNote(noteId).map { list -> list.map { Tag(it.id, it.name) } }

    override suspend fun attach(noteId: Long, rawName: String): Long {
        val normalized = rawName.trim().lowercase().take(40)
        require(normalized.isNotBlank()) { "Empty tag" }
        return dao.attachTag(noteId, normalized)
    }

    override suspend fun detach(noteId: Long, tagId: Long) = dao.unlink(noteId, tagId)

    override suspend fun deleteTag(tagId: Long) = dao.deleteTag(tagId)
}
