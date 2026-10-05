package com.abhishek.inkora.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local attachment metadata. Bytes live in app-private storage
 * (`note_images/` for images, `note_files/` for generic files) — never raw
 * blobs in Room. Rows survive trash/restore; permanent note deletion removes
 * rows + files.
 */
@Entity(
    tableName = "attachments",
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
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val noteId: Long = 0L,
    val fileName: String = "",
    val mimeType: String = "image/jpeg",
    val width: Int = 0,
    val height: Int = 0,
    /** "image" or "file". Images render inline; files render as compact cards. */
    val kind: String = "image",
    val sizeBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
