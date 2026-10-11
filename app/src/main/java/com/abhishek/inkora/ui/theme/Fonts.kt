package com.abhishek.inkora.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.abhishek.inkora.R

/**
 * Outfit — one open (SIL OFL, see licenses/Outfit-OFL.txt) geometric sans,
 * bundled with the app. It carries the v1.7 look: tight, warm, slightly
 * rounded. Headings, titles, labels and UI copy use it; note content stays
 * on the editor's platform serif for long-form reading comfort.
 */
val InkoraSans = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold)
)
