package com.abhishek.inkora.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One handwriting document per note. Strokes live here as compact JSON
 * (vector points, never a baked bitmap), so ink stays editable and
 * zoom-independent. Rendered thumbnails are cached files, not rows.
 */
@Entity(
    tableName = "handwriting_docs",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class HandwritingDocEntity(
    @PrimaryKey val noteId: Long = 0L,
    val strokesJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
)
