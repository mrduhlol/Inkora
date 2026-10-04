package com.abhishek.inkora

import com.abhishek.inkora.data.export.ExportAttachment
import com.abhishek.inkora.data.export.ExportFolder
import com.abhishek.inkora.data.export.ExportNote
import com.abhishek.inkora.data.export.ExportPayload
import com.abhishek.inkora.data.export.InkoraExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportTest {

    private fun sample() = ExportPayload(
        exportedAt = 123L,
        folders = listOf(ExportFolder(7L, "Work")),
        notes = listOf(
            ExportNote(title = "A", content = "{}", contentFormat = "rich-v1", folderId = 7L, isPinned = true),
            ExportNote(title = "B", content = "plain")
        ),
        attachments = listOf(ExportAttachment(0, "img.jpg", "image/jpeg", 10, 10, ""))
    )

    @Test fun roundTrip_preservesEverything() {
        val back = InkoraExport.decode(InkoraExport.encode(sample())).getOrThrow()
        assertEquals(2, back.notes.size)
        assertEquals(7L, back.folders.single().id)
        assertEquals(true, back.notes[0].isPinned)
        assertEquals(7L, back.notes[0].folderId)
        assertEquals(0, back.attachments.single().noteIndex)
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
