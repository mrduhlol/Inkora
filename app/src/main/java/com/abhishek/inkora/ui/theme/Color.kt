package com.abhishek.inkora.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Centralized design tokens. No hard-coded colors outside this file + Theme.kt.

// Canvas — the desk pages sit on. Vivid paper reads best on a warm cream desk
// in light mode and a near-black one in dark; v1.7 leans into that contrast.
val CanvasCream = Color(0xFFF2E8CF)
val CanvasInk = Color(0xFF0C0B0B)

// Paper surfaces (light)
val PaperWhite = Color(0xFFFFFFFF)
val PaperCream = Color(0xFFFBF4E4)
val PaperGray = Color(0xFFF1F0EC)
val PaperDark = Color(0xFF1E1D1B)
val PaperInk = Color(0xFF17130E)
val PaperMuted = Color(0xFF6B6355)
val PaperLightInk = Color(0xFFF5F1E6)
val PaperLightMuted = Color(0xFFB9B2A6)

// Vivid papers — the colored sheets of the v1.7 look.
val PaperOrange = Color(0xFFF26B3A)
val PaperYellow = Color(0xFFF7C948)
val PaperGreen = Color(0xFF84B25E)
val PaperBlue = Color(0xFFA9C3EC)

// Floating controls — always the same black circle with cream glyphs,
// outlined on dark canvases so the shape stays readable on black.
val FabInk = Color(0xFF14110E)
val FabIcon = Color(0xFFF7F3EA)

// Folded corner + rulings
val FoldShadow = Color(0xFFD8D2C4)
val RuleLine = Color(0xFFE3DCCB)
val GridLine = Color(0xFFE7E0D0)
val DotColor = Color(0xFFCFC6B4)

fun paperColorFor(background: String, customHex: String?): Color {
    customHex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()?.let { c -> return c } }
    return when (background) {
        "white" -> PaperWhite
        "gray" -> PaperGray
        "dark" -> PaperDark
        "orange" -> PaperOrange
        "yellow" -> PaperYellow
        "green" -> PaperGreen
        "blue" -> PaperBlue
        else -> PaperCream
    }
}

/** WCAG-style contrast ratio between two colors (1..21). */
private fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/**
 * Readable ink color for a given paper: picks whichever ink actually
 * contrasts more. A pure luminance threshold would put white text on the
 * vivid orange; real contrast prefers the dark ink there.
 */
fun onPaperColor(paper: Color): Color =
    if (contrastRatio(paper, PaperInk) >= contrastRatio(paper, PaperLightInk)) PaperInk
    else PaperLightInk

/** Muted/placeholder tone with the same contrast rule. */
fun mutedOnPaperColor(paper: Color): Color =
    if (contrastRatio(paper, PaperMuted) >= contrastRatio(paper, PaperLightMuted)) PaperMuted
    else PaperLightMuted

/**
 * Guard a requested text color against the paper: obviously unreadable
 * combinations (dark-on-dark, light-on-light) fall back to the readable ink
 * color instead of surprising the user. Contrast threshold ~3:1.
 */
fun guardedTextColor(requestedArgb: Int, paper: Color): Color {
    val requested = Color(requestedArgb)
    val lp = paper.luminance()
    val lr = requested.luminance()
    val ratio = (maxOf(lp, lr) + 0.05f) / (minOf(lp, lr) + 0.05f)
    return if (ratio >= 3f) requested else onPaperColor(paper)
}
