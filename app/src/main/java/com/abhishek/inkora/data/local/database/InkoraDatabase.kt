package com.abhishek.inkora.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.abhishek.inkora.data.local.database.entities.AttachmentEntity
import com.abhishek.inkora.data.local.database.entities.FolderEntity
import com.abhishek.inkora.data.local.database.entities.NoteEntity

/**
 * V2: adds `notes.isPinned` and the `attachments` side table.
 * [MIGRATION_1_2] is additive only — existing notes, folders and formatting
 * survive untouched.
 */
@Database(
    entities = [NoteEntity::class, FolderEntity::class, AttachmentEntity::class],
    version = 2,
    exportSchema = false
)
abstract class InkoraDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        const val NAME = "inkora.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS attachments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    noteId INTEGER NOT NULL,
                    fileName TEXT NOT NULL,
                    mimeType TEXT NOT NULL,
                    width INTEGER NOT NULL,
                    height INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_noteId ON attachments(noteId)")
            }
        }
    }
}
