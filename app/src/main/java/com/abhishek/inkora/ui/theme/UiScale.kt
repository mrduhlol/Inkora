package com.abhishek.inkora.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp

/**
 * App-wide UI scale from Settings → Display size. Chrome only: spacing, icon
 * and control dimensions. Note content (editor text, preview bodies) never
 * reads this — body size is its own formatting. Composes with system font
 * scaling instead of fighting it (sp still scales independently).
 */
val LocalUiScale = compositionLocalOf { 1f }

/** Scale a chrome dimension by the current UI scale factor. */
@Composable
fun scaledDp(base: Dp): Dp = base * LocalUiScale.current
