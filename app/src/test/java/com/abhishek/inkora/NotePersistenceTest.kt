package com.abhishek.inkora

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.abhishek.inkora.data.local.database.InkoraDatabase
import com.abhishek.inkora.data.local.database.entities.FolderEntity
import com.abhishek.inkora.data.local.database.entities.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Critical persistence test: create → close → reopen → still exists.
 * Uses in-memory Room with the real DAO (no fakes), Robolectric for JVM context.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotePersistenceTest {
    private lateinit var db: InkoraDatabase

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            InkoraDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After fun close() { db.close() }

    @Test fun create_edit_save_retrieve() = runTest {
        val id = db.noteDao().upsert(NoteEntity(title = "Physics Notes", content = "Semiconductor…"))
        val got = db.noteDao().getById(id)
        assertNotNull(got)
        assertEquals("Physics Notes", got!!.title)
    }

    @Test fun delete_restore_and_favorite_search() = runTest {
        val id = db.noteDao().upsert(NoteEntity(title = "To Do", content = "ship V1"))
        db.noteDao().moveToTrash(id)
        assertTrue(db.noteDao().observeTrash().first().any { it.id == id })
        db.noteDao().restore(id)
        db.noteDao().setFavorite(id, true)
        val found = db.noteDao().search("ship").first()
        assertTrue(found.any { it.id == id && it.isFavorite })
        db.noteDao().setArchived(id, true)
        assertTrue(db.noteDao().observeArchived().first().any { it.id == id })
    }

    @Test fun folder_assign_move_persists_and_delete_keeps_note() = runTest {
        val folderId = db.folderDao().upsert(FolderEntity(name = "Physics"))
        val id = db.noteDao().upsert(NoteEntity(title = "Notes", content = "x"))
        db.noteDao().moveToFolder(id, folderId)
        assertTrue(db.noteDao().observeInFolder(folderId).first().any { it.id == id })
        // No duplication: still exactly one row.
        assertEquals(1, db.noteDao().observeInFolder(folderId).first().size)
        // Folder deletion unfiles but preserves the note.
        db.noteDao().clearFolder(folderId)
        db.folderDao().delete(folderId)
        val kept = db.noteDao().getById(id)
        assertNotNull(kept)
        assertEquals(null, kept!!.folderId)
    }

    @Test fun pin_and_duplicate() = runTest {
        val repo = com.abhishek.inkora.data.repository.NoteRepositoryImpl(db.noteDao())
        val id = db.noteDao().upsert(NoteEntity(title = "Original", content = "{}", isFavorite = true))
        db.noteDao().setPinned(id, true)
        assertEquals(true, db.noteDao().getById(id)!!.isPinned)
        val copyId = repo.duplicate(id)
        assertNotNull(copyId)
        val copy = db.noteDao().getById(copyId!!)!!
        assertEquals("Original (Copy)", copy.title)
        assertEquals("{}", copy.content)
        assertTrue(copy.isFavorite)
        assertEquals(false, copy.isDeleted)
        assertTrue(copy.id != id)
    }

    @Test fun search_matches_folder_name() = runTest {
        val folderId = db.folderDao().upsert(FolderEntity(name = "Recipes"))
        val id = db.noteDao().upsert(NoteEntity(title = "Dinner", content = "pasta"))
        db.noteDao().moveToFolder(id, folderId)
        assertTrue(db.noteDao().search("recip").first().any { it.id == id })
    }

    @Test fun migration_columns_exist_with_defaults() = runTest {
        // v2 schema: legacy-style insert still works, pin defaults to false.
        val id = db.noteDao().upsert(NoteEntity(title = "Legacy", content = "old"))
        assertEquals(false, db.noteDao().getById(id)!!.isPinned)
        assertTrue(db.attachmentDao().listForNote(id).isEmpty())
    }

    @Test fun tags_attach_search_detach_delete() = runTest {
        val repo = com.abhishek.inkora.data.repository.TagRepositoryImpl(db.tagDao())
        val id = db.noteDao().upsert(NoteEntity(title = "Thermo", content = "entropy"))
        repo.attach(id, "Physics")
        repo.attach(id, "physics") // same normalized tag, no duplicate
        assertEquals(1, db.tagDao().listTags().size)
        assertEquals("physics", db.tagDao().listTagsForNote(id).single().name)
        assertTrue(db.noteDao().search("phys").first().any { it.id == id })
        assertTrue(db.noteDao().observeNotesWithTag(db.tagDao().listTags().single().id).first().any { it.id == id })
        repo.detach(id, db.tagDao().listTags().single().id)
        assertTrue(db.tagDao().listTagsForNote(id).isEmpty())
        // Deleting a tag never deletes notes.
        val tagId = repo.attach(id, "temp")
        repo.deleteTag(tagId)
        assertNotNull(db.noteDao().getById(id))
    }

    @Test fun noteType_defaultsTextAndPersists() = runTest {
        val id = db.noteDao().upsert(NoteEntity(title = "Plain"))
        assertEquals("text", db.noteDao().getById(id)!!.noteType)
        val hw = db.noteDao().upsert(NoteEntity(title = "Sketch", noteType = "handwriting"))
        assertEquals("handwriting", db.noteDao().getById(hw)!!.noteType)
        val todo = db.noteDao().upsert(NoteEntity(title = "Tasks", noteType = "todo"))
        assertEquals("todo", db.noteDao().getById(todo)!!.noteType)
    }
}
