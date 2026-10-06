package com.abhishek.inkora.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import com.abhishek.inkora.data.local.database.HandwritingDao
import com.abhishek.inkora.data.local.database.entities.HandwritingDocEntity
import com.abhishek.inkora.domain.model.CANVAS_SIZE
import com.abhishek.inkora.domain.model.HwStroke
import com.abhishek.inkora.domain.model.decodeHw
import com.abhishek.inkora.domain.model.encodeHw
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Handwriting persistence: vector strokes as one JSON row per note plus a
 * cached PNG thumbnail for Home previews. Thumbnails regenerate on save;
 * Home never renders stroke vectors itself.
 */
@Singleton
class HandwritingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: HandwritingDao
) {
    private val thumbDir: File get() = File(context.filesDir, "thumbs").apply { mkdirs() }

    fun thumbFile(noteId: Long): File = File(thumbDir, "hw_$noteId.png")

    fun observeStrokes(noteId: Long): Flow<List<HwStroke>> =
        dao.observe(noteId).map { it?.let(::decode) ?: emptyList() }

    suspend fun load(noteId: Long): List<HwStroke> =
        dao.get(noteId)?.let(::decode) ?: emptyList()

    /** Save strokes (already debounced by callers — never per pointer event). */
    suspend fun save(noteId: Long, strokes: List<HwStroke>) = withContext(Dispatchers.IO) {
        dao.upsert(
            HandwritingDocEntity(
                noteId = noteId,
                strokesJson = encodeHw(strokes),
                updatedAt = System.currentTimeMillis()
            )
        )
        renderThumbnail(noteId, strokes)
    }

    suspend fun delete(noteId: Long) = withContext(Dispatchers.IO) {
        dao.delete(noteId)
        runCatching { thumbFile(noteId).delete() }
    }

    fun encode(strokes: List<HwStroke>): String = encodeHw(strokes)

    fun decodeStrokes(json: String): List<HwStroke> = decodeHw(json)

    private fun decode(entity: HandwritingDocEntity): List<HwStroke> = decodeHw(entity.strokesJson)

    /** Fit the whole canvas into a 512px box on white: cheap, zoom-independent. */
    private fun renderThumbnail(noteId: Long, strokes: List<HwStroke>) {
        runCatching {
            val size = 512
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            canvas.drawColor(android.graphics.Color.WHITE)
            if (strokes.isNotEmpty()) {
                val k = size / CANVAS_SIZE
                strokes.forEach { s ->
                    val paint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        strokeJoin = Paint.Join.ROUND
                        strokeCap = Paint.Cap.ROUND
                        strokeWidth = (s.widthPx * k).coerceAtLeast(1f)
                        color = s.colorArgb
                    }
                    if (s.points.size == 1) {
                        val p = s.points[0]
                        canvas.drawCircle(p.x * k, p.y * k, (s.widthPx * k) / 2f, paint.apply {
                            style = Paint.Style.FILL
                        })
                    } else if (s.points.size > 1) {
                        val path = Path().apply {
                            moveTo(s.points[0].x * k, s.points[0].y * k)
                            s.points.drop(1).forEach { lineTo(it.x * k, it.y * k) }
                        }
                        canvas.drawPath(path, paint)
                    }
                }
            }
            thumbFile(noteId).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
        }
    }
}
