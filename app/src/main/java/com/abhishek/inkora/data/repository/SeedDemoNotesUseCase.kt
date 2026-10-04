package com.abhishek.inkora.data.repository

import com.abhishek.inkora.domain.model.Note
import com.abhishek.inkora.domain.model.PageStyle
import com.abhishek.inkora.domain.repository.NoteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Debug-only sample dataset. Never called from release builds —
 * MainActivity does not invoke it; call manually from a debug menu if needed.
 */
class SeedDemoNotesUseCase @Inject constructor(private val repo: NoteRepository) {
    suspend operator fun invoke() {
        if (repo.observeActiveNotes().first().isNotEmpty()) return
        val now = System.currentTimeMillis()
        listOf(
            Note(title = "Physics Notes", content = "Semiconductor is a material with conductivity between conductor and insulator. An intrinsic semiconductor is pure…", pageStyle = PageStyle.RULED.key, createdAt = now, updatedAt = now),
            Note(title = "Project Ideas", content = "Inkora-web companion, offline-first sync, handwriting in V2.", pageStyle = PageStyle.BLANK.key, createdAt = now, updatedAt = now),
            Note(title = "To Do", content = "- [ ] Polish paper preview\n- [ ] Verify autosave\n- [ ] Ship V1", pageStyle = PageStyle.DOTTED.key, createdAt = now, updatedAt = now)
        ).forEach { repo.upsert(it) }
    }
}
