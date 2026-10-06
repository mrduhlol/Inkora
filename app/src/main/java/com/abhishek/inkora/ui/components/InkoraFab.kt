package com.abhishek.inkora.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun InkoraFab(onClick: () -> Unit, modifier: Modifier = Modifier, menuOpen: Boolean = false) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = if (menuOpen) "Close creation menu" else "Create new note"
        }
    ) {
        Icon(
            if (menuOpen) Icons.Filled.Close else Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}
