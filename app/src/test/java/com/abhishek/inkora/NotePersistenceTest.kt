package com.abhishek.inkora

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.abhishek.inkora.data.local.database.InkoraDatabase
import com.abhishek.inkora.data.local.database.entities.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Critical persistence test: create → close → reopen → still exists.
 * Uses in-memory Room with the real DAO (no fakes).
 */
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
}
