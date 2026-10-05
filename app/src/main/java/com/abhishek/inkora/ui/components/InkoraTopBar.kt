package com.abhishek.inkora.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Editor top bar. The overflow menu is rendered INSIDE the ⋮ action Box so the
 * [DropdownMenu] anchors to the button on every screen size, font scale and
 * orientation — never floating at an unrelated screen position.
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
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        },
        title = {
            androidx.compose.foundation.layout.Column {
                Text(title.ifBlank { "Untitled" }, maxLines = 1)
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
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Favorite"
                )
            }
            Box {
                IconButton(onClick = onMore) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = onMenuDismiss) {
                    menuContent()
                }
            }
        }
    )
}
