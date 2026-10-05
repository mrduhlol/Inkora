package com.abhishek.inkora.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.ui.theme.foldShadowFor
import com.abhishek.inkora.ui.theme.foldUndersideFor

/**
 * Dog-eared top-right page corner, drawn with Compose (no static image).
 *
 * Geometry: a right triangle whose legs run along the page's top and right
 * edges. A slightly larger translucent triangle underneath peeks out along
 * the hypotenuse as a soft contact shadow; the fold face itself is the paper
 * darkened a touch, so it reads as the page's own underside on light AND
 * dark paper. A faint crease line finishes the edge.
 */
@Composable
fun FoldedCorner(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    paperColor: Color = Color.White,
    foldColor: Color? = null
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val f = minOf(w, h)
        val underside = foldColor ?: foldUndersideFor(paperColor)
        val shadow = foldShadowFor(paperColor)

        // Contact shadow: marginally larger triangle peeking past the fold.
        val pad = f * 0.10f
        drawPath(
            Path().apply {
                moveTo(w - f - pad, 0f)
                lineTo(w, 0f)
                lineTo(w, f + pad)
                close()
            },
            color = shadow
        )
        // Fold face: the page's own underside.
        val ax = w - f
        val by = f
        drawPath(
            Path().apply {
                moveTo(ax, 0f)
                lineTo(w, 0f)
                lineTo(w, by)
                close()
            },
            color = underside
        )
        // Crease along the hypotenuse suggests paper thickness.
        drawLine(
            color = paperColor,
            start = Offset(ax, 0f),
            end = Offset(w, by),
            strokeWidth = (f * 0.045f).coerceAtLeast(1f)
        )
    }
}
