package com.abhishek.inkora.features.templates

import com.abhishek.inkora.domain.model.BlockKind
import com.abhishek.inkora.domain.model.RichBlock
import com.abhishek.inkora.domain.model.RichContent
import com.abhishek.inkora.domain.model.RichText

/**
 * Built-in note templates. A template only pre-fills title + structured
 * content — the note itself is an ordinary note afterwards.
 */
data class NoteTemplate(
    val id: String,
    val name: String,
    val description: String,
    val title: String,
    val content: RichContent
)

private fun doc(
    lines: List<String>,
    blocks: List<Pair<Int, BlockKind>> = emptyList(),
    checked: Set<Int> = emptySet()
): RichContent = RichContent(
    text = lines.joinToString("\n"),
    blocks = blocks.map { (line, kind) -> RichBlock(line, kind, checked = line in checked) }
)

private fun check(rows: IntRange): List<Pair<Int, BlockKind>> =
    rows.map { it to BlockKind.CHECK }

private fun bullets(rows: IntRange): List<Pair<Int, BlockKind>> =
    rows.map { it to BlockKind.BULLET }

val NoteTemplates: List<NoteTemplate> = listOf(
    NoteTemplate(
        id = "meeting",
        name = "Meeting Notes",
        description = "Attendees, agenda and action items",
        title = "Meeting Notes",
        content = doc(
            listOf("Meeting Notes", "Attendees:", "", "Agenda:", "", "Action items:", "", ""),
            blocks = listOf(0 to BlockKind.HEADING1) + check(6..7)
        )
    ),
    NoteTemplate(
        id = "study",
        name = "Study Notes",
        description = "Topic summary with key points",
        title = "Study Notes",
        content = doc(
            listOf("Study Notes", "Summary:", "", "Key points:", "", "", "Questions:", ""),
            blocks = listOf(0 to BlockKind.HEADING1, 3 to BlockKind.HEADING2, 6 to BlockKind.HEADING2) +
                bullets(4..5)
        )
    ),
    NoteTemplate(
        id = "journal",
        name = "Daily Journal",
        description = "Gratitude and reflection prompts",
        title = "Daily Journal",
        content = doc(
            listOf("Daily Journal", "Today I am grateful for:", "", "Highlights:", "", "Tomorrow:", ""),
            blocks = listOf(0 to BlockKind.HEADING1) + bullets(2..2) + bullets(4..4) + bullets(6..6)
        )
    ),
    NoteTemplate(
        id = "project",
        name = "Project Notes",
        description = "Goals, milestones and open questions",
        title = "Project Notes",
        content = doc(
            listOf("Project Notes", "Goal:", "", "Milestones:", "", "", "Open questions:", ""),
            blocks = listOf(0 to BlockKind.HEADING1, 3 to BlockKind.HEADING2, 6 to BlockKind.HEADING2) +
                check(4..5)
        )
    ),
    NoteTemplate(
        id = "lecture",
        name = "Lecture Notes",
        description = "Structured lecture capture",
        title = "Lecture Notes",
        content = doc(
            listOf("Lecture Notes", "Course:", "Date:", "", "Main ideas:", "", "", "To review:", ""),
            blocks = listOf(0 to BlockKind.HEADING1, 4 to BlockKind.HEADING2, 7 to BlockKind.HEADING2) +
                bullets(5..6)
        )
    ),
    NoteTemplate(
        id = "checklist",
        name = "Checklist",
        description = "A clean actionable list",
        title = "Checklist",
        content = doc(
            listOf("Checklist", "", "", ""),
            blocks = check(1..3)
        )
    )
)

/** Encode a template body for storage (already a valid note payload). */
fun NoteTemplate.encoded(): String = RichText.encode(content)
