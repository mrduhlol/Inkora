package com.abhishek.inkora.data.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Single-file Inkora backup. Attachments embed downsampled JPEG bytes (base64). */
@Serializable
data class ExportFolder(val id: Long, val name: String)

@Serializable
data class ExportNote(
    val title: String = "",
    val content: String = "",
    val contentFormat: String = "rich-v1",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val folderId: Long? = null,
    val backgroundStyle: String = "blank",
    val backgroundColor: String? = null,
    val textColor: String? = null,
    val pageStyle: String = "blank"
)

@Serializable
data class ExportAttachment(
    val noteIndex: Int,
    val fileName: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    /** Base64 JPEG bytes, capped at write time. Empty = metadata only. */
    val dataBase64: String = ""
)

@Serializable
data class ExportPayload(
    val app: String = "inkora",
    val format: Int = 1,
    val exportedAt: Long = 0L,
    val folders: List<ExportFolder> = emptyList(),
    val notes: List<ExportNote> = emptyList(),
    val attachments: List<ExportAttachment> = emptyList()
)

private val ExportJson = Json { ignoreUnknownKeys = true; prettyPrint = true }

object InkoraExport {
    const val MAX_NOTES = 5000
    const val MAX_ATTACHMENTS = 500
    const val MAX_TOTAL_BYTES = 25 * 1024 * 1024

    fun encode(p: ExportPayload): String =
        ExportJson.encodeToString(ExportPayload.serializer(), p)

    /** Parse with sanity caps. Returns failure instead of throwing on garbage. */
    fun decode(raw: String): Result<ExportPayload> = runCatching {
        if (raw.length > MAX_TOTAL_BYTES + 1024 * 1024) throw IllegalArgumentException("Export file too large")
        val p = ExportJson.decodeFromString(ExportPayload.serializer(), raw)
        require(p.app == "inkora") { "Not an Inkora file" }
        require(p.format == 1) { "Unsupported export version ${p.format}" }
        require(p.notes.size <= MAX_NOTES) { "Too many notes (${p.notes.size})" }
        require(p.attachments.size <= MAX_ATTACHMENTS) { "Too many attachments" }
        p
    }
}
