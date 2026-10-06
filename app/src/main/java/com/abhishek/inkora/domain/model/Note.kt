package com.abhishek.inkora.domain.model

/**
 * V1 Note domain model.
 *
 * contentFormat rationale: V1 stores lightweight Markdown-ish plain text
 * ("md-v1"). Future V2/V3 (drawings, images, audio, PDF annotations) can
 * introduce new formats / side tables keyed by note id without rewriting
 * existing rows. backgroundStyle/backgroundColor/textColor are part of the
 * note so a page keeps its paper look everywhere (home preview + editor).
 */
data class Note(
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    /** Format tag for forward-compat rich content. V1 = "md-v1". */
    val contentFormat: String = "md-v1",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val isPinned: Boolean = false,
    val noteType: NoteType = NoteType.TEXT,
    val folderId: Long? = null,
    val backgroundStyle: String = PageStyle.BLANK.key,
    val backgroundColor: String? = null, // nullable ARGB hex like "#FFF8E7"; null = default for style
    val textColor: String? = null,
    val pageStyle: String = PageStyle.BLANK.key
) {
    val isEmpty: Boolean get() = title.isBlank() && content.isBlank()
}
