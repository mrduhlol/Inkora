package com.abhishek.inkora.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
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
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppTheme.entries.forEach { t ->
            FilterChip(
                selected = t == selected,
                onClick = { onSelect(t) },
                label = { Text(themeLabel(t)) }
            )
        }
    }
}

/** v1.7 canvas names — plain words for what each theme actually shows. */
private fun themeLabel(t: AppTheme): String = when (t) {
    AppTheme.SYSTEM -> "System"
    AppTheme.LIGHT -> "Cream"
    AppTheme.DARK -> "Charcoal"
    AppTheme.AMOLED -> "Black"
}
