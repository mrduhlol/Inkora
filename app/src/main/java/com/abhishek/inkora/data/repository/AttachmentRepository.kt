package com.abhishek.inkora.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.abhishek.inkora.data.local.database.AttachmentDao
import com.abhishek.inkora.data.local.database.entities.AttachmentEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class Attachment(
    val id: Long,
    val noteId: Long,
    val file: File?,
    val fileName: String = "",
    val mimeType: String,
    val kind: String = "image",
    val sizeBytes: Long = 0L,
    val width: Int,
    val height: Int,
    val missing: Boolean
)

/**
 * App-private attachment storage. Images live under `files/note_images/`,
 * generic files under `files/note_files/`; Room keeps metadata only. Missing
 * files degrade to a placeholder card — never a crash.
 */
@Singleton
class AttachmentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AttachmentDao
) {
    private val imageDir: File get() = File(context.filesDir, "note_images").apply { mkdirs() }
    private val fileDir: File get() = File(context.filesDir, "note_files").apply { mkdirs() }

    private fun dirFor(kind: String): File = if (kind == "file") fileDir else imageDir

    fun observe(noteId: Long): Flow<List<Attachment>> =
        dao.observeForNote(noteId).map { list -> list.map(::resolve) }

    suspend fun list(noteId: Long): List<Attachment> = dao.listForNote(noteId).map(::resolve)

    fun fileFor(entity: AttachmentEntity): File = File(dirFor(entity.kind), "${entity.id}_${entity.fileName}")

    private fun resolve(e: AttachmentEntity): Attachment {
        val f = fileFor(e)
        val size = if (f.exists()) f.length() else e.sizeBytes
        return Attachment(
            e.id, e.noteId, f.takeIf { it.exists() }, e.fileName, e.mimeType,
            e.kind, size, e.width, e.height, !f.exists()
        )
    }

    /** Copy a picked image into private storage. Returns attachment id or null. */
    suspend fun add(noteId: Long, uri: Uri): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val ext = when {
                mime.contains("png") -> "png"
                mime.contains("webp") -> "webp"
                else -> "jpg"
            }
            val rowId = dao.insert(
                AttachmentEntity(noteId = noteId, fileName = "img.$ext", mimeType = mime)
            )
            val dest = File(imageDir, "${rowId}_img.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            } ?: throw IllegalStateException("unreadable image")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(dest.absolutePath, bounds)
            dao.insert(
                AttachmentEntity(
                    id = rowId, noteId = noteId, fileName = "img.$ext", mimeType = mime,
                    width = bounds.outWidth, height = bounds.outHeight,
                    kind = "image", sizeBytes = dest.length()
                )
            )
            rowId
        }.getOrNull()
    }

    /**
     * Copy a picked generic file (PDF, TXT, document) into private storage.
     * Files over 25MB are refused to protect device storage. Returns id or null.
     */
    suspend fun addFile(noteId: Long, uri: Uri): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val name = queryName(uri).takeLast(80).ifBlank { "file-${System.currentTimeMillis()}" }
            val rowId = dao.insert(
                AttachmentEntity(noteId = noteId, fileName = name, mimeType = mime, kind = "file")
            )
            val dest = File(fileDir, "${rowId}_$name")
            var total = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(8192)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > MAX_FILE_BYTES) throw IllegalStateException("file too large")
                        out.write(buf, 0, n)
                    }
                }
            } ?: throw IllegalStateException("unreadable file")
            dao.insert(
                AttachmentEntity(
                    id = rowId, noteId = noteId, fileName = name, mimeType = mime,
                    kind = "file", sizeBytes = total
                )
            )
            rowId
        }.getOrNull()
    }

    private fun queryName(uri: Uri): String = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else ""
        }.orEmpty()
    }.getOrDefault("")

    /** Store raw bytes (import path). */
    suspend fun storeBytes(noteId: Long, fileName: String, mime: String, bytes: ByteArray): Long =
        withContext(Dispatchers.IO) {
            val rowId = dao.insert(AttachmentEntity(noteId = noteId, fileName = fileName, mimeType = mime))
            val dest = File(imageDir, "${rowId}_$fileName")
            dest.writeBytes(bytes)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            dao.insert(
                AttachmentEntity(
                    id = rowId, noteId = noteId, fileName = fileName, mimeType = mime,
                    width = bounds.outWidth, height = bounds.outHeight,
                    kind = "image", sizeBytes = bytes.size.toLong()
                )
            )
            rowId
        }

    /** Store raw generic-file bytes (import path). */
    suspend fun storeFileBytes(noteId: Long, fileName: String, mime: String, bytes: ByteArray): Long =
        withContext(Dispatchers.IO) {
            val rowId = dao.insert(
                AttachmentEntity(noteId = noteId, fileName = fileName, mimeType = mime, kind = "file")
            )
            File(fileDir, "${rowId}_$fileName").writeBytes(bytes)
            dao.insert(
                AttachmentEntity(
                    id = rowId, noteId = noteId, fileName = fileName, mimeType = mime,
                    kind = "file", sizeBytes = bytes.size.toLong()
                )
            )
            rowId
        }

    suspend fun readBytes(id: Long): ByteArray? = withContext(Dispatchers.IO) {
        val e = dao.getById(id) ?: return@withContext null
        runCatching { fileFor(e).readBytes() }.getOrNull()
    }

    /** Full fidelity copy of a note's attachments for undo. Null when too big. */
    data class AttachmentBackup(
        val fileName: String,
        val mimeType: String,
        val kind: String,
        val width: Int,
        val height: Int,
        val bytes: ByteArray
    )

    suspend fun snapshotForNote(noteId: Long, maxTotal: Long = 5L * 1024 * 1024): List<AttachmentBackup>? =
        withContext(Dispatchers.IO) {
            val rows = dao.listForNote(noteId)
            var total = 0L
            val out = mutableListOf<AttachmentBackup>()
            for (e in rows) {
                val bytes = runCatching { fileFor(e).readBytes() }.getOrNull() ?: continue
                total += bytes.size
                if (total > maxTotal) return@withContext null
                out.add(AttachmentBackup(e.fileName, e.mimeType, e.kind, e.width, e.height, bytes))
            }
            out
        }

    suspend fun restoreSnapshot(noteId: Long, backup: AttachmentBackup) {
        if (backup.kind == "file") {
            storeFileBytes(noteId, backup.fileName, backup.mimeType, backup.bytes)
        } else {
            storeBytes(noteId, backup.fileName, backup.mimeType, backup.bytes)
        }
    }

    suspend fun remove(id: Long) = withContext(Dispatchers.IO) {
        dao.getById(id)?.let { runCatching { fileFor(it).delete() } }
        dao.deleteById(id)
    }

    /** Permanent note deletion path: rows (FK cascade) + orphaned files. */
    suspend fun removeForNote(noteId: Long) = withContext(Dispatchers.IO) {
        dao.listForNote(noteId).forEach { runCatching { fileFor(it).delete() } }
        dao.deleteForNote(noteId)
    }

    suspend fun imagesBytes(): Long = withContext(Dispatchers.IO) {
        runCatching {
            (imageDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } +
                fileDir.walkTopDown().filter { it.isFile }.sumOf { it.length() })
        }.getOrDefault(0L)
    }

    companion object {
        const val MAX_FILE_BYTES = 25L * 1024 * 1024
    }
}
