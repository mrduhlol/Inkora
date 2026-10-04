package com.abhishek.inkora.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.domain.model.PageStyle

@Composable
fun PageStyleSelector(
    selected: PageStyle,
    onSelect: (PageStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PageStyle.entries.forEach { s ->
            FilterChip(
                selected = s == selected,
                onClick = { onSelect(s) },
                label = { Text(s.key.replaceFirstChar(Char::titlecase)) }
            )
        }
    }
}
