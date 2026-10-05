package com.abhishek.inkora.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Lightweight tag metadata. Tags live outside the note body — the user never
 * types `#tag` syntax. Case-insensitive uniqueness is enforced by storing the
 * normalized (lowercase, trimmed) name.
 */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** Many-to-many note↔tag link. Cascades on both sides: deleting a note or a tag cleans up. */
@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class NoteTagCrossRef(
    val noteId: Long = 0L,
    val tagId: Long = 0L
)
