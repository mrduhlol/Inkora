package com.abhishek.inkora.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Central Inkora design tokens. Layout code must use these instead of
 * scattered magic numbers so spacing, radii, depth and motion stay coherent.
 */
object InkoraTokens {
    // Corner radii
    val PaperCardRadius = 14.dp
    val PaperSwatchRadius = 6.dp
    val SheetRadius = 8.dp
    val ControlRadius = 10.dp

    // Spacing scale
    val SpaceXxs = 2.dp
    val SpaceXs = 4.dp
    val SpaceSm = 8.dp
    val SpaceMd = 12.dp
    val SpaceLg = 16.dp
    val SpaceXl = 20.dp
    val SpaceXxl = 24.dp
    val ScreenGutter = 16.dp

    // Paper depth: a sheet resting on a surface, not a floating card.
    val PaperShadow = 2.dp
    val PaperTonal = 1.dp

    // Grid rhythm
    val GridGap = 14.dp
    val GridPadding = 16.dp

    // Motion (subtle, never bouncy)
    const val DurationFast = 150
    const val DurationMedium = 250

    // Touch targets
    val MinTouch = 48.dp
}

/** Darken [c] toward black by [t] (0..1). */
fun darken(c: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return c.copy(red = c.red * (1 - k), green = c.green * (1 - k), blue = c.blue * (1 - k))
}

/** Lighten [c] toward white by [t] (0..1). */
fun lighten(c: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return c.copy(
        red = c.red + (1 - c.red) * k,
        green = c.green + (1 - c.green) * k,
        blue = c.blue + (1 - c.blue) * k
    )
}

/**
 * Underside color of a folded page corner: always a touch darker than the
 * paper itself so the fold reads on light AND dark pages.
 */
fun foldUndersideFor(paper: Color): Color =
    if (paper.luminance() > 0.5f) darken(paper, 0.14f) else lighten(paper, 0.12f)

/** Soft contact shadow cast by the fold onto the page. */
fun foldShadowFor(paper: Color): Color =
    if (paper.luminance() > 0.5f) Color.Black.copy(alpha = 0.10f)
    else Color.Black.copy(alpha = 0.28f)
