package com.abhishek.inkora.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.abhishek.inkora.domain.model.AccentColor
import com.abhishek.inkora.domain.model.AppTheme

fun accentSeed(accent: AccentColor): Color = when (accent) {
    AccentColor.BLUE -> Color(0xFF2F6FED)
    AccentColor.PURPLE -> Color(0xFF6750A4)
    AccentColor.GREEN -> Color(0xFF2E7D32)
    AccentColor.ORANGE -> Color(0xFFE8710A)
    AccentColor.RED -> Color(0xFFD32F2F)
    AccentColor.PINK -> Color(0xFFC2185B)
    AccentColor.TEAL -> Color(0xFF00796B)
}

private fun lerp(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t
)

private val White = Color.White
private val Black = Color.Black

/**
 * Full seed-derived schemes so the accent drives FAB, switches, checkboxes,
 * selection, dialogs and highlights — not just `primary`. Pure (testable).
 */
fun inkoraLightScheme(seed: Color): ColorScheme {
    val onSeed = if (seed.luminance() > 0.55f) Color(0xFF1C1B1F) else White
    val container = lerp(seed, White, 0.86f)
    val onContainer = lerp(seed, Black, 0.55f)
    val secondary = lerp(seed, Black, 0.12f)
    return lightColorScheme(
        primary = seed,
        onPrimary = onSeed,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondary = secondary,
        onSecondary = White,
        secondaryContainer = lerp(seed, White, 0.78f),
        onSecondaryContainer = onContainer,
        tertiary = lerp(seed, Black, 0.28f),
        surface = Color(0xFFFAF7F0),
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = Color(0xFFE7E0D3),
        onSurfaceVariant = Color(0xFF4A4640),
        background = Color(0xFFFAF7F0),
        onBackground = Color(0xFF1C1B1F),
        outline = Color(0xFF79767A)
    )
}

/** [amoled] uses true black surfaces; regular dark uses near-black. */
fun inkoraDarkScheme(seed: Color, amoled: Boolean): ColorScheme {
    val surface = if (amoled) Black else Color(0xFF141318)
    val onSeed = if (seed.luminance() > 0.55f) Color(0xFF1C1B1F) else White
    val container = lerp(seed, Black, 0.72f)
    val onContainer = lerp(seed, White, 0.82f)
    return darkColorScheme(
        primary = lerp(seed, White, if (seed.luminance() > 0.4f) 0.1f else 0.25f),
        onPrimary = onSeed,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondary = lerp(seed, White, 0.2f),
        secondaryContainer = lerp(seed, Black, 0.6f),
        onSecondaryContainer = onContainer,
        tertiary = lerp(seed, White, 0.35f),
        surface = surface,
        onSurface = Color(0xFFE8E2D9),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0),
        background = surface,
        onBackground = Color(0xFFE8E2D9),
        outline = Color(0xFF938F99)
    )
}

@Composable
fun InkoraTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    accent: AccentColor = AccentColor.PURPLE,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK, AppTheme.AMOLED -> true
        AppTheme.SYSTEM -> systemDark
    }
    val seed = accentSeed(accent)

    // Dynamic color follows the OS palette when enabled; otherwise the Inkora
    // accent fully owns the scheme (page backgrounds stay independent — they
    // are per-note paper colors, never derived from this scheme).
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> inkoraDarkScheme(seed, amoled = appTheme == AppTheme.AMOLED)
        else -> inkoraLightScheme(seed)
    }

    MaterialTheme(colorScheme = scheme, typography = InkoraTypography, content = content)
}
