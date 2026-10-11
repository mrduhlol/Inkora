package com.abhishek.inkora.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// v1.7 type scale: Outfit, tight and bold — the premium-minimal look.
// Chrome (headings, titles, labels, dialogs) is all InkoraSans. Note content
// is NOT styled from here; the editor sets its own serif (EditorScreen) so
// long-form reading comfort from v1.6 is preserved.
val InkoraTypography = Typography(
    displaySmall = TextStyle(fontFamily = InkoraSans, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 39.sp, letterSpacing = (-1.1).sp),
    headlineSmall = TextStyle(fontFamily = InkoraSans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = InkoraSans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = InkoraSans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = InkoraSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = InkoraSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = InkoraSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = InkoraSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = InkoraSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = InkoraSans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
)
