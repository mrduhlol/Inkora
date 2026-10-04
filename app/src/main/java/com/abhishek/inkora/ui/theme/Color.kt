package com.abhishek.inkora.ui.theme

import androidx.compose.ui.graphics.Color

// Centralized design tokens. No hard-coded colors outside this file + Theme.kt.

// Paper surfaces (light)
val PaperWhite = Color(0xFFFFFFFF)
val PaperCream = Color(0xFFFAF3E3)
val PaperGray = Color(0xFFF1F0EC)
val PaperDark = Color(0xFF1E1D1B)
val PaperInk = Color(0xFF1C1B1F)
val PaperMuted = Color(0xFF5F5B58)

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
        else -> PaperCream
    }
}
