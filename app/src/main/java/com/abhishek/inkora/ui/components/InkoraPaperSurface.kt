package com.abhishek.inkora.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.domain.model.PageStyle
import com.abhishek.inkora.ui.theme.DotColor
import com.abhishek.inkora.ui.theme.GridLine
import com.abhishek.inkora.ui.theme.RuleLine
import com.abhishek.inkora.ui.theme.paperColorFor

/**
 * Paper surface that paints blank / ruled / grid / dotted backgrounds.
 * Used by both home preview and full editor page so they share one visual language.
 */
@Composable
fun InkoraPaperSurface(
    modifier: Modifier = Modifier,
    background: String,
    customHex: String?,
    pageStyle: String,
    textColor: Color = Color(0xFF1C1B1F),
    content: @Composable () -> Unit
) {
    val paper = paperColorFor(background, customHex)
    Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        color = paper,
        tonalElevation = 1.dp,
        shadowElevation = 3.dp
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                when (PageStyle.fromKey(pageStyle)) {
                    PageStyle.RULED -> {
                        val step = 28.dp.toPx()
                        var y = step
                        while (y < size.height) {
                            drawLine(RuleLine, Offset(0f, y), Offset(size.width, y), 1.2f)
                            y += step
                        }
                    }
                    PageStyle.GRID -> {
                        val step = 24.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(GridLine, Offset(x, 0f), Offset(x, size.height), 1f)
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(GridLine, Offset(0f, y), Offset(size.width, y), 1f)
                            y += step
                        }
                    }
                    PageStyle.DOTTED -> {
                        val stepX = 22.dp.toPx(); val stepY = 22.dp.toPx()
                        var y = stepY
                        while (y < size.height) {
                            var x = stepX
                            while (x < size.width) {
                                drawCircle(DotColor, 2.2f, Offset(x, y))
                                x += stepX
                            }
                            y += stepY
                        }
                    }
                    PageStyle.BLANK -> Unit
                }
            }
            content()
        }
    }
}
