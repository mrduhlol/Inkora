package com.abhishek.inkora.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.abhishek.inkora.domain.model.AccentColor
import com.abhishek.inkora.domain.model.AppTheme

private fun accentSeed(accent: AccentColor): Color = when (accent) {
    AccentColor.BLUE -> Color(0xFF2F6FED)
    AccentColor.PURPLE -> Color(0xFF6750A4)
    AccentColor.GREEN -> Color(0xFF2E7D32)
    AccentColor.ORANGE -> Color(0xFFE8710A)
    AccentColor.RED -> Color(0xFFD32F2F)
    AccentColor.PINK -> Color(0xFFC2185B)
    AccentColor.TEAL -> Color(0xFF00796B)
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

    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> darkColorScheme(
            primary = seed,
            surface = if (appTheme == AppTheme.AMOLED) Color.Black else Color(0xFF141318),
            background = if (appTheme == AppTheme.AMOLED) Color.Black else Color(0xFF141318)
        )
        else -> lightColorScheme(
            primary = seed,
            surface = Color(0xFFFAF7F0),
            background = Color(0xFFFAF7F0)
        )
    }

    MaterialTheme(colorScheme = scheme, typography = InkoraTypography, content = content)
}
