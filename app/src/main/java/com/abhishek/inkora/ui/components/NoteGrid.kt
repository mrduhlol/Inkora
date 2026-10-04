package com.abhishek.inkora.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.domain.model.Note

/**
 * Responsive grid: 2 columns on phones, more on tablets/landscape.
 * gridOverride: 0 = adaptive, else fixed 1..4 (from Settings).
 */
@Composable
fun NoteGrid(
    notes: List<Note>,
    onOpen: (Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    modifier: Modifier = Modifier,
    gridOverride: Int = 0
) {
    val width = LocalConfiguration.current.screenWidthDp
    val adaptive = when {
        width >= 900 -> 4
        width >= 600 -> 3
        else -> 2
    }
    val columns = if (gridOverride in 1..4) gridOverride else adaptive
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(notes, key = { it.id }) { note ->
            InkoraPaperPreview(note = note, onOpen = onOpen, onToggleFavorite = onToggleFavorite)
        }
    }
}
