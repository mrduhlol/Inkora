package com.abhishek.inkora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.domain.model.AccentColor

private fun AccentColor.toColor(): Color = when (this) {
    AccentColor.BLUE -> Color(0xFF2F6FED)
    AccentColor.PURPLE -> Color(0xFF6750A4)
    AccentColor.GREEN -> Color(0xFF2E7D32)
    AccentColor.ORANGE -> Color(0xFFE8710A)
    AccentColor.RED -> Color(0xFFD32F2F)
    AccentColor.PINK -> Color(0xFFC2185B)
    AccentColor.TEAL -> Color(0xFF00796B)
}

@Composable
fun AccentColorSelector(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AccentColor.entries.forEach { a ->
            Box(
                Modifier.size(36.dp)
                    .clip(CircleShape)
                    .background(a.toColor())
                    .clickable { onSelect(a) }
                    .border(
                        width = if (a == selected) 3.dp else 1.dp,
                        color = if (a == selected) Color.Black else Color.Gray,
                        shape = CircleShape
                    )
            )
        }
    }
}
