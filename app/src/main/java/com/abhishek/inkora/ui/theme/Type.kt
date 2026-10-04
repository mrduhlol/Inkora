package com.abhishek.inkora.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Clean, readable type scale. Scalable via system font settings automatically.
val InkoraTypography = Typography(
    displaySmall = TextStyle(FontFamily.Default, FontWeight.SemiBold, 28.sp, 34.sp, letterSpacing = (-0.25).sp),
    headlineSmall = TextStyle(FontFamily.Default, FontWeight.SemiBold, 22.sp, 28.sp),
    titleLarge = TextStyle(FontFamily.Default, FontWeight.SemiBold, 20.sp, 26.sp),
    titleMedium = TextStyle(FontFamily.Default, FontWeight.Medium, 16.sp, 22.sp),
    bodyLarge = TextStyle(FontFamily.Default, FontWeight.Normal, 16.sp, 24.sp),
    bodyMedium = TextStyle(FontFamily.Default, FontWeight.Normal, 14.sp, 20.sp),
    labelLarge = TextStyle(FontFamily.Default, FontWeight.Medium, 14.sp, 20.sp),
    labelSmall = TextStyle(FontFamily.Default, FontWeight.Medium, 11.sp, 16.sp)
)
