package com.abhishek.inkora.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.abhishek.inkora.data.local.database.entities.AttachmentEntity
import com.abhishek.inkora.data.local.database.entities.FolderEntity
import com.abhishek.inkora.data.local.database.entities.NoteEntity
import com.abhishek.inkora.data.local.database.entities.NoteTagCrossRef
import com.abhishek.inkora.data.local.database.entities.TagEntity

/**
 * V3: tags + note_tags tables, generic file columns on attachments.
 * [MIGRATION_2_3] is additive only — V1.3 notes, folders, pins, formatting
 * and images survive untouched.
 */
@Database(
    entities = [NoteEntity::class, FolderEntity::class, AttachmentEntity::class, TagEntity::class, NoteTagCrossRef::class],
    version = 3,
    exportSchema = false
)
abstract class InkoraDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun tagDao(): TagDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                    )"""
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS note_tags (
                    noteId INTEGER NOT NULL,
                    tagId INTEGER NOT NULL,
                    PRIMARY KEY(noteId, tagId),
                    FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_tags_tagId ON note_tags(tagId)")
                db.execSQL("ALTER TABLE attachments ADD COLUMN kind TEXT NOT NULL DEFAULT 'image'")
                db.execSQL("ALTER TABLE attachments ADD COLUMN sizeBytes INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
