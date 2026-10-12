package com.abhishek.inkora.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Editor top bar. The overflow menu is rendered INSIDE the ⋮ action Box so the
 * [DropdownMenu] anchors to the button on every screen size, font scale and
 * orientation — never floating at an unrelated screen position.
 *
 * v1.7: transparent container (the page canvas shows through) and soft
 * circular controls instead of bare icons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InkoraTopBar(
    title: String,
    isFavorite: Boolean,
    menuExpanded: Boolean = false,
    subtitle: String? = null,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMore: () -> Unit,
    onMenuDismiss: () -> Unit = {},
    menuContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        navigationIcon = {
            Box(Modifier.padding(start = 8.dp)) {
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack
                )
            }
        },
        title = {
            Column {
                Text(
                    title.ifBlank { "Untitled" },
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            CircleIconButton(
                icon = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = "Favorite",
                onClick = onToggleFavorite
            )
            Spacer(Modifier.width(6.dp))
            Box(Modifier.padding(end = 8.dp)) {
                CircleIconButton(
                    icon = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    onClick = onMore
                )
                DropdownMenu(expanded = menuExpanded, onDismissRequest = onMenuDismiss) {
                    menuContent()
                }
            }
        }
    )
}
