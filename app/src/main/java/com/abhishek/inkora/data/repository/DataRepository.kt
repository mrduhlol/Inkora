package com.abhishek.inkora.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import com.abhishek.inkora.data.export.ExportAttachment
import com.abhishek.inkora.data.export.ExportFolder
import com.abhishek.inkora.data.export.ExportHandwriting
import com.abhishek.inkora.data.export.ExportNote
import com.abhishek.inkora.data.export.ExportPayload
import com.abhishek.inkora.data.export.ExportTag
import com.abhishek.inkora.data.export.InkoraExport
import com.abhishek.inkora.data.local.database.AttachmentDao
import com.abhishek.inkora.data.local.database.FolderDao
import com.abhishek.inkora.data.local.database.HandwritingDao
import com.abhishek.inkora.data.local.database.InkoraDatabase
import com.abhishek.inkora.data.local.database.NoteDao
import com.abhishek.inkora.data.local.database.TagDao
import com.abhishek.inkora.data.local.database.entities.FolderEntity
import com.abhishek.inkora.data.local.database.entities.HandwritingDocEntity
import com.abhishek.inkora.data.local.database.entities.NoteEntity
import com.abhishek.inkora.data.local.database.entities.TagEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class StorageStats(
    val dbBytes: Long,
    val imageBytes: Long,
    val noteCount: Int,
    val trashCount: Int
)

data class ImportResult(
    val notes: Int,
    val folders: Int,
    val images: Int,
    val skipped: Int,
    val error: String? = null
)

/**
 * Local backup engine: whole-library export to one JSON file via SAF, and
 * guarded import that always creates NEW rows (never overwrites).
 */
@Singleton
class DataRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val attachmentDao: AttachmentDao,
    private val tagDao: TagDao,
    private val handwritingDao: HandwritingDao,
    private val attachments: AttachmentRepository
) {
    suspend fun exportAll(): ByteArray = withContext(Dispatchers.IO) {
        val notes = noteDao.observeActive().first() + noteDao.observeArchived().first()
        val folders = folderDao.observe().first()
        val tags = tagDao.listTags()
        val outNotes = notes.map { n ->
            ExportNote(
                n.title, n.content, n.contentFormat, n.createdAt, n.updatedAt,
                n.isFavorite, n.isArchived, n.isPinned, n.folderId,
                n.backgroundStyle, n.backgroundColor, n.textColor, n.pageStyle,
                tagDao.listTagsForNote(n.id).map { it.id },
                n.noteType
            )
        }
        val outHandwriting = mutableListOf<ExportHandwriting>()
        notes.forEachIndexed { index, n ->
            // Vector strokes only — thumbnails regenerate locally on import.
            handwritingDao.get(n.id)?.let { outHandwriting.add(ExportHandwriting(index, it.strokesJson)) }
        }
        val outAttachments = mutableListOf<ExportAttachment>()
        notes.forEachIndexed { index, n ->
            attachmentDao.listForNote(n.id).forEach { a ->
                val bytes = attachments.readBytes(a.id)
                // Images are downsampled for portability; generic files embed
                // as-is up to the backup size cap.
                val capped = if (a.kind == "file") bytes else bytes?.let { capImage(it) }
                outAttachments.add(
                    ExportAttachment(
                        noteIndex = index,
                        fileName = a.fileName,
                        mimeType = a.mimeType,
                        width = a.width,
                        height = a.height,
                        dataBase64 = capped?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: "",
                        kind = a.kind,
                        sizeBytes = a.sizeBytes
                    )
                )
            }
        }
        val payload = ExportPayload(
            exportedAt = System.currentTimeMillis(),
            folders = folders.map { ExportFolder(it.id, it.name) },
            tags = tags.map { ExportTag(it.id, it.name) },
            notes = outNotes,
            attachments = outAttachments,
            handwriting = outHandwriting
        )
        InkoraExport.encode(payload).toByteArray(Charsets.UTF_8)
    }

    /** Shrink images over 2MB to bounded JPEG so exports stay portable. */
    private fun capImage(bytes: ByteArray): ByteArray {
        if (bytes.size <= 2 * 1024 * 1024) return bytes
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return bytes
            val out = ByteArrayOutputStream()
            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
            bmp.recycle()
            out.toByteArray()
        }.getOrDefault(bytes)
    }

    suspend fun importBytes(bytes: ByteArray): ImportResult = withContext(Dispatchers.IO) {
        val payload = InkoraExport.decode(bytes.toString(Charsets.UTF_8)).getOrElse {
            return@withContext ImportResult(0, 0, 0, 0, it.message ?: "Invalid file")
        }
        var notes = 0
        var folders = 0
        var tags = 0
        var images = 0
        var skipped = 0
        val folderRemap = mutableMapOf<Long, Long>()
        payload.folders.forEach { f ->
            runCatching {
                val newId = folderDao.upsert(FolderEntity(name = f.name.take(120)))
                folderRemap[f.id] = newId
                folders++
            }.getOrElse { skipped++ }
        }
        val tagRemap = mutableMapOf<Long, Long>()
        payload.tags.forEach { t ->
            runCatching {
                val clean = t.name.trim().lowercase().take(40)
                require(clean.isNotBlank())
                val existing = tagDao.findIdByName(clean)
                val newId = existing ?: tagDao.insertTag(TagEntity(name = clean)).takeIf { it != -1L }
                    ?: tagDao.findIdByName(clean)!!
                tagRemap[t.id] = newId
                tags++
            }.getOrElse { skipped++ }
        }
        val newIds = mutableListOf<Long>()
        payload.notes.forEach { n ->
            runCatching {
                val id = noteDao.upsert(
                    NoteEntity(
                        title = n.title.take(500),
                        content = n.content,
                        contentFormat = n.contentFormat.ifBlank { "rich-v1" },
                        createdAt = n.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        isFavorite = n.isFavorite,
                        isArchived = n.isArchived,
                        isDeleted = false, // imports always land as live new notes
                        isPinned = n.isPinned,
                        noteType = n.noteType.ifBlank { "text" },
                        folderId = n.folderId?.let { folderRemap[it] },
                        backgroundStyle = n.backgroundStyle,
                        backgroundColor = n.backgroundColor,
                        textColor = n.textColor,
                        pageStyle = n.pageStyle
                    )
                )
                n.tagIds.mapNotNull { tagRemap[it] }.forEach { tagId ->
                    runCatching {
                        tagDao.link(
                            com.abhishek.inkora.data.local.database.entities.NoteTagCrossRef(id, tagId)
                        )
                    }
                }
                newIds.add(id)
                notes++
            }.getOrElse { skipped++ }
        }
        payload.attachments.forEach { a ->
            val target = newIds.getOrNull(a.noteIndex) ?: run { skipped++; return@forEach }
            if (a.dataBase64.isBlank()) { skipped++; return@forEach }
            runCatching {
                val raw = Base64.decode(a.dataBase64, Base64.DEFAULT)
                if (a.kind == "file") {
                    attachments.storeFileBytes(target, a.fileName.takeLast(60), a.mimeType, raw)
                } else {
                    attachments.storeBytes(target, a.fileName.takeLast(60), a.mimeType, raw)
                }
                images++
            }.getOrElse { skipped++ }
        }
        payload.handwriting.forEach { h ->
            val target = newIds.getOrNull(h.noteIndex) ?: run { skipped++; return@forEach }
            runCatching {
                handwritingDao.upsert(HandwritingDocEntity(target, h.strokesJson))
            }.getOrElse { skipped++ }
        }
        ImportResult(notes, folders, images, skipped)
    }

    suspend fun storageStats(): StorageStats = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(InkoraDatabase.NAME)
        val dbBytes = runCatching { if (dbFile.exists()) dbFile.length() else 0L }.getOrDefault(0L)
        val active = runCatching { noteDao.observeActive().first().size }.getOrDefault(0)
        val trash = runCatching { noteDao.observeTrash().first().size }.getOrDefault(0)
        StorageStats(dbBytes, attachments.imagesBytes(), active, trash)
    }

    /** Permanently delete everything in trash (notes + their image files). */
    suspend fun clearTrash(): Int = withContext(Dispatchers.IO) {
        val trashed = noteDao.observeTrash().first()
        trashed.forEach { n ->
            attachments.removeForNote(n.id)
            noteDao.deleteForever(n.id)
        }
        trashed.size
    }
}
