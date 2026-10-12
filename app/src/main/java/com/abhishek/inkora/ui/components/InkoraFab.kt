package com.abhishek.inkora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.ui.theme.FabIcon
import com.abhishek.inkora.ui.theme.FabInk

/**
 * v1.7 + button: one big black circle with a cream glyph. On dark canvases a
 * hairline ring keeps the black shape legible against near-black.
 */
@Composable
fun InkoraFab(onClick: () -> Unit, modifier: Modifier = Modifier, menuOpen: Boolean = false) {
    val onDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = FabInk,
        contentColor = FabIcon,
        border = if (onDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)) else null,
        shadowElevation = 8.dp,
        modifier = modifier.size(64.dp).semantics {
            contentDescription = if (menuOpen) "Close creation menu" else "Create new note"
        }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                if (menuOpen) Icons.Filled.Close else Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
