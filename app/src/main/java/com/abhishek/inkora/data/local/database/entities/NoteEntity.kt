package com.abhishek.inkora.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity mirroring [com.abhishek.inkora.domain.model.Note].
 * Future attachments (images/audio/PDF/drawings) go in side tables keyed by
 * noteId so this table never needs a rewrite.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val contentFormat: String = "md-v1",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val isPinned: Boolean = false,
    val folderId: Long? = null,
    val backgroundStyle: String = "blank",
    val backgroundColor: String? = null,
    val textColor: String? = null,
    val pageStyle: String = "blank"
)
