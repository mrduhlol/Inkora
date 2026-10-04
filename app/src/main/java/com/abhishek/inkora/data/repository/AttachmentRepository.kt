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
    val mimeType: String,
    val width: Int,
    val height: Int,
    val missing: Boolean
)

/**
 * App-private image storage. Bytes live under `files/note_images/`, Room keeps
 * metadata only. Missing files degrade to a placeholder card — never a crash.
 */
@Singleton
class AttachmentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AttachmentDao
) {
    private val dir: File get() = File(context.filesDir, "note_images").apply { mkdirs() }

    fun observe(noteId: Long): Flow<List<Attachment>> =
        dao.observeForNote(noteId).map { list -> list.map(::resolve) }

    suspend fun list(noteId: Long): List<Attachment> = dao.listForNote(noteId).map(::resolve)

    fun fileFor(entity: AttachmentEntity): File = File(dir, "${entity.id}_${entity.fileName}")

    private fun resolve(e: AttachmentEntity): Attachment {
        val f = fileFor(e)
        return Attachment(e.id, e.noteId, f.takeIf { it.exists() }, e.mimeType, e.width, e.height, !f.exists())
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
            val dest = File(dir, "${rowId}_img.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            } ?: throw IllegalStateException("unreadable image")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(dest.absolutePath, bounds)
            dao.insert(
                AttachmentEntity(
                    id = rowId, noteId = noteId, fileName = "img.$ext", mimeType = mime,
                    width = bounds.outWidth, height = bounds.outHeight
                )
            )
            rowId
        }.getOrNull()
    }

    /** Store raw bytes (import path). */
    suspend fun storeBytes(noteId: Long, fileName: String, mime: String, bytes: ByteArray): Long =
        withContext(Dispatchers.IO) {
            val rowId = dao.insert(AttachmentEntity(noteId = noteId, fileName = fileName, mimeType = mime))
            val dest = File(dir, "${rowId}_$fileName")
            dest.writeBytes(bytes)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            dao.insert(
                AttachmentEntity(
                    id = rowId, noteId = noteId, fileName = fileName, mimeType = mime,
                    width = bounds.outWidth, height = bounds.outHeight
                )
            )
            rowId
        }

    suspend fun readBytes(id: Long): ByteArray? = withContext(Dispatchers.IO) {
        val e = dao.getById(id) ?: return@withContext null
        runCatching { fileFor(e).readBytes() }.getOrNull()
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
        runCatching { dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } }.getOrDefault(0L)
    }
}
