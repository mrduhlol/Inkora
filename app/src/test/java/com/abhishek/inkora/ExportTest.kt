package com.abhishek.inkora

import com.abhishek.inkora.data.export.ExportAttachment
import com.abhishek.inkora.data.export.ExportFolder
import com.abhishek.inkora.data.export.ExportHandwriting
import com.abhishek.inkora.data.export.ExportNote
import com.abhishek.inkora.data.export.ExportPayload
import com.abhishek.inkora.data.export.ExportTag
import com.abhishek.inkora.data.export.InkoraExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportTest {

    private fun sample() = ExportPayload(
        exportedAt = 123L,
        folders = listOf(ExportFolder(7L, "Work")),
        tags = listOf(ExportTag(3L, "college")),
        notes = listOf(
            ExportNote(title = "A", content = "{}", contentFormat = "rich-v1", folderId = 7L, isPinned = true, tagIds = listOf(3L)),
            ExportNote(title = "B", content = "plain")
        ),
        attachments = listOf(
            ExportAttachment(0, "img.jpg", "image/jpeg", 10, 10, ""),
            ExportAttachment(1, "doc.pdf", "application/pdf", 0, 0, "", kind = "file", sizeBytes = 42L)
        ),
        handwriting = listOf(ExportHandwriting(0, """{"strokes":[]}"""))
    )

    @Test fun roundTrip_preservesEverything() {
        val back = InkoraExport.decode(InkoraExport.encode(sample())).getOrThrow()
        assertEquals(2, back.notes.size)
        assertEquals(7L, back.folders.single().id)
        assertEquals(true, back.notes[0].isPinned)
        assertEquals(7L, back.notes[0].folderId)
        assertEquals(0, back.attachments.first().noteIndex)
        assertEquals(listOf(3L), back.notes[0].tagIds)
        assertEquals("college", back.tags.single().name)
        assertEquals("file", back.attachments.last().kind)
        assertEquals("text", back.notes[0].noteType)
        assertEquals(0, back.handwriting.single().noteIndex)
    }

    @Test fun v1Backup_stillImports() {
        val v1 = """{"app":"inkora","format":1,"exportedAt":1,"folders":[],"notes":[{"title":"Old"}],"attachments":[]}"""
        val back = InkoraExport.decode(v1).getOrThrow()
        assertEquals("Old", back.notes.single().title)
        assertTrue(back.tags.isEmpty())
        assertEquals("text", back.notes.single().noteType)
        assertTrue(back.handwriting.isEmpty())
    }

    @Test fun v2Backup_stillImports() {
        val v2 = """{"app":"inkora","format":2,"exportedAt":1,"folders":[],"tags":[],"notes":[{"title":"Mid","noteType":"todo"}],"attachments":[]}"""
        val back = InkoraExport.decode(v2).getOrThrow()
        assertEquals("todo", back.notes.single().noteType)
    }

    @Test fun garbage_isFailureNotCrash() {
        assertTrue(InkoraExport.decode("{nope").isFailure)
        assertTrue(InkoraExport.decode("{\"app\":\"other\",\"format\":1}").isFailure)
        assertTrue(InkoraExport.decode("{\"app\":\"inkora\",\"format\":99}").isFailure)
    }

    @Test fun oversized_isRejected() {
        val big = sample().copy(notes = List(6000) { ExportNote(title = "x") })
        assertTrue(InkoraExport.decode(InkoraExport.encode(big)).isFailure)
    }
}
