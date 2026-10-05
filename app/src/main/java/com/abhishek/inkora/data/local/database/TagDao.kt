package com.abhishek.inkora.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.abhishek.inkora.data.local.database.entities.NoteTagCrossRef
import com.abhishek.inkora.data.local.database.entities.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun listTags(): List<TagEntity>

    @Query("SELECT t.* FROM tags t INNER JOIN note_tags nt ON nt.tagId = t.id WHERE nt.noteId = :noteId ORDER BY t.name ASC")
    fun observeTagsForNote(noteId: Long): Flow<List<TagEntity>>

    @Query("SELECT t.* FROM tags t INNER JOIN note_tags nt ON nt.tagId = t.id WHERE nt.noteId = :noteId ORDER BY t.name ASC")
    suspend fun listTagsForNote(noteId: Long): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(entity: TagEntity): Long

    @Query("SELECT id FROM tags WHERE name = :normalized LIMIT 1")
    suspend fun findIdByName(normalized: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId AND tagId = :tagId")
    suspend fun unlink(noteId: Long, tagId: Long)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: Long)

    /** Attach an existing-or-new tag to a note. Returns the tag id. */
    @Transaction
    suspend fun attachTag(noteId: Long, normalized: String): Long {
        var tagId = findIdByName(normalized)
        if (tagId == null) {
            val inserted = insertTag(TagEntity(name = normalized))
            tagId = if (inserted != -1L) inserted else findIdByName(normalized)
        }
        val id = tagId ?: return -1L
        link(NoteTagCrossRef(noteId, id))
        return id
    }
}
