package com.abhishek.inkora

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.abhishek.inkora.data.local.database.InkoraDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * V2 → V3 migration test with representative V1.3 data.
 *
 * Builds a real v2 database file with raw SQL (notes, folder, image row),
 * opens it with the v3 schema + [InkoraDatabase.MIGRATION_2_3], and verifies
 * nothing disappears: content, formatting, folders, favorites, pins, trash,
 * archive and page styles all survive, with tags/files tables ready.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    private fun v2File(context: Context, name: String): java.io.File {
        val file = java.io.File(context.filesDir, name)
        if (file.exists()) file.delete()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(file.absolutePath)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            title TEXT NOT NULL, content TEXT NOT NULL, contentFormat TEXT NOT NULL,
                            createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL,
                            isFavorite INTEGER NOT NULL, isArchived INTEGER NOT NULL,
                            isDeleted INTEGER NOT NULL, isPinned INTEGER NOT NULL,
                            folderId INTEGER, backgroundStyle TEXT NOT NULL,
                            backgroundColor TEXT, textColor TEXT, pageStyle TEXT NOT NULL)"""
                        )
                        db.execSQL(
                            """CREATE TABLE folders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL, createdAt INTEGER NOT NULL)"""
                        )
                        db.execSQL(
                            """CREATE TABLE attachments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            noteId INTEGER NOT NULL, fileName TEXT NOT NULL, mimeType TEXT NOT NULL,
                            width INTEGER NOT NULL, height INTEGER NOT NULL, createdAt INTEGER NOT NULL,
                            FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE)"""
                        )
                        db.execSQL("CREATE INDEX index_attachments_noteId ON attachments(noteId)")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
                })
                .build()
        )
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO folders (name, createdAt) VALUES ('College', 1000)")
        val rich = """{"text":"Physics **bold**","spans":[{"start":8,"end":12,"kind":"BOLD"}],"blocks":[],"aligns":[]}"""
        db.execSQL(
            "INSERT INTO notes (title, content, contentFormat, createdAt, updatedAt, isFavorite, " +
                "isArchived, isDeleted, isPinned, folderId, backgroundStyle, backgroundColor, " +
                "textColor, pageStyle) VALUES ('Study', '$rich', 'rich-v1', 1000, 2000, 1, 0, 0, 1, 1, " +
                "'cream', NULL, NULL, 'ruled')"
        )
        db.execSQL(
            "INSERT INTO notes (title, content, contentFormat, createdAt, updatedAt, isFavorite, " +
                "isArchived, isDeleted, isPinned, folderId, backgroundStyle, backgroundColor, " +
                "textColor, pageStyle) VALUES ('Old', 'gone', 'md-v1', 1000, 1000, 0, 0, 1, 0, NULL, " +
                "'white', NULL, NULL, 'blank')"
        )
        db.execSQL(
            "INSERT INTO notes (title, content, contentFormat, createdAt, updatedAt, isFavorite, " +
                "isArchived, isDeleted, isPinned, folderId, backgroundStyle, backgroundColor, " +
                "textColor, pageStyle) VALUES ('Hidden', 'x', 'md-v1', 1000, 1000, 0, 1, 0, 0, NULL, " +
                "'gray', NULL, NULL, 'grid')"
        )
        db.execSQL(
            "INSERT INTO attachments (noteId, fileName, mimeType, width, height, createdAt) " +
                "VALUES (1, 'img.jpg', 'image/jpeg', 10, 10, 1000)"
        )
        db.close()
        return file
    }

    @Test fun migrate2to3_preservesEverything() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()
        val name = "mig-v2-v3.db"
        v2File(context, name)
        val db = Room.databaseBuilder(context, InkoraDatabase::class.java, name)
            .addMigrations(InkoraDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
            val study = db.noteDao().getById(1)!!
            assertEquals("Study", study.title)
            assertTrue(study.content.contains("Physics"))
            assertEquals("rich-v1", study.contentFormat)
            assertEquals(true, study.isFavorite)
            assertEquals(true, study.isPinned)
            assertEquals("ruled", study.pageStyle)
            assertEquals(1L, study.folderId)
            // Trash + archive rows intact.
            assertEquals(1, db.noteDao().observeTrash().first().size)
            assertEquals(1, db.noteDao().observeArchived().first().size)
            // Legacy attachment row gained v3 defaults, not garbage.
            val atts = db.attachmentDao().listForNote(1)
            assertEquals(1, atts.size)
            assertEquals("image", atts.single().kind)
            assertEquals(0L, atts.single().sizeBytes)
            // New tables exist and start empty.
            assertTrue(db.tagDao().listTags().isEmpty())
            assertTrue(db.tagDao().listTagsForNote(1).isEmpty())
            // Tags work post-migration.
            db.tagDao().attachTag(1, "college")
            assertEquals("college", db.tagDao().listTagsForNote(1).single().name)
            assertTrue(db.noteDao().search("college").first().any { it.id == 1L })
        } finally {
            db.close()
        }
    }
}
