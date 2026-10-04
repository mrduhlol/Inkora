package com.abhishek.inkora.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.ui.theme.FoldShadow

/**
 * Folded top-right corner, drawn with Compose (no static image).
 * Draws a small right-triangle fold + soft shadow edge.
 */
@Composable
fun FoldedCorner(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    paperColor: Color = Color.White,
    foldColor: Color = FoldShadow
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // Shadow under fold
        drawPath(
            Path().apply {
                moveTo(w - w * 0.0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h)
                close()
            },
            color = foldColor.copy(alpha = 0.55f)
        )
        // Fold triangle (paper underside)
        drawPath(
            Path().apply {
                moveTo(w - h, 0f)
                lineTo(w, h)
                lineTo(w - h, h)
                close()
            },
            color = foldColor
        )
        // Tiny highlight to suggest paper thickness
        drawLine(
            color = paperColor,
            start = Offset(w - h, 0f),
            end = Offset(w, h),
            strokeWidth = 1.5f
        )
        // Clip illusion: cover top-right with background handled by parent
        drawRect(color = Color.Transparent, topLeft = Offset.Zero, size = Size(w, h))
    }
}
