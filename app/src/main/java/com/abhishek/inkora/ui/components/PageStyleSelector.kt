package com.abhishek.inkora.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.domain.model.PageStyle

/**
 * Page-style picker with true mini paper previews (blank / ruled / grid /
 * dotted) instead of text-only chips. Selection applies immediately.
 */
@Composable
fun PageStyleSelector(
    selected: PageStyle,
    onSelect: (PageStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PageStyle.entries.forEach { style ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MiniPaperPreview(
                    style = style,
                    selected = style == selected,
                    onClick = { onSelect(style) }
                )
                Text(
                    style.key.replaceFirstChar(Char::titlecase),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (style == selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniPaperPreview(style: PageStyle, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = Modifier
            .width(56.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = border,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Page style ${style.key}" },
        color = Color(0xFFFAF3E3)
    ) {
        Canvas(Modifier.padding(0.dp)) {
            val w = size.width
            val h = size.height
            when (style) {
                PageStyle.RULED -> {
                    var y = h / 5f
                    while (y < h) {
                        drawLine(Color(0xFFE3DCCB), Offset(0f, y), Offset(w, y), 1.5f)
                        y += h / 5f
                    }
                }
                PageStyle.GRID -> {
                    var x = 0f
                    while (x < w) {
                        drawLine(Color(0xFFE7E0D0), Offset(x, 0f), Offset(x, h), 1.2f)
                        x += w / 4f
                    }
                    var y = 0f
                    while (y < h) {
                        drawLine(Color(0xFFE7E0D0), Offset(0f, y), Offset(w, y), 1.2f)
                        y += h / 5f
                    }
                }
                PageStyle.DOTTED -> {
                    var y = h / 5f
                    while (y < h) {
                        var x = w / 4f
                        while (x < w) {
                            drawCircle(Color(0xFFCFC6B4), 1.6f, Offset(x, y))
                            x += w / 4f
                        }
                        y += h / 5f
                    }
                }
                PageStyle.BLANK -> Unit
            }
        }
    }
}
