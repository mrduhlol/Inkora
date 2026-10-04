package com.abhishek.inkora.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.domain.model.AppTheme

@Composable
fun ThemeSelector(
    selected: AppTheme,
    onSelect: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppTheme.entries.forEach { t ->
            FilterChip(
                selected = t == selected,
                onClick = { onSelect(t) },
                label = { Text(t.key.replaceFirstChar(Char::titlecase)) }
            )
        }
    }
}
