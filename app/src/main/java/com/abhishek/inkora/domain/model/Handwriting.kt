package com.abhishek.inkora.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Vector handwriting model. Strokes are points in a large virtual canvas
 * space ([CANVAS_SIZE]px square), so ink stays crisp at any zoom and cheap
 * to store. Rendering maps canvas → screen through scale/offset owned by UI.
 */
const val CANVAS_SIZE = 4096f
const val MIN_SCALE = 0.1f
const val MAX_SCALE = 8f

/** Cap on strokes per note: bounds memory and keeps the canvas smooth. */
const val MAX_STROKES = 2000

@Serializable
data class HwPoint(val x: Float, val y: Float, val pressure: Float = 1f)

@Serializable
data class HwStroke(
    val points: List<HwPoint> = emptyList(),
    val colorArgb: Int = 0xFF1C1B1F.toInt(),
    val widthPx: Float = 8f,
    val erase: Boolean = false
)

@Serializable
data class HwDoc(val strokes: List<HwStroke> = emptyList())

enum class HwTool { PEN, ERASER, PAN }

/** Small curated ink palette plus white for dark paper. */
val INK_COLORS = listOf(
    0xFF1C1B1F.toInt(),
    0xFFFFFFFF.toInt(),
    0xFFD32F2F.toInt(),
    0xFF2F6FED.toInt(),
    0xFF2E7D32.toInt(),
    0xFFFFD600.toInt(),
    0xFF7B1FA2.toInt()
)

/** Pen thickness presets in canvas px. */
val PEN_WIDTHS = listOf(4f, 8f, 16f)

private val HwJson = Json { ignoreUnknownKeys = true }

/** Stable stroke encoding shared by storage, backup and tests. */
fun encodeHw(strokes: List<HwStroke>): String =
    HwJson.encodeToString(HwDoc.serializer(), HwDoc(strokes))

fun decodeHw(json: String): List<HwStroke> =
    runCatching { HwJson.decodeFromString(HwDoc.serializer(), json).strokes }
        .getOrDefault(emptyList())
