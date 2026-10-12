package com.abhishek.inkora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * v1.7 pill chip: solid ink when selected, hairline outline when idle.
 * Carries the reference's "All 23" pattern via the optional [count] and an
 * optional leading icon for control-style chips (sort).
 */
@Composable
fun InkoraChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    leadingIcon: ImageVector? = null
) {
    val container = if (selected) MaterialTheme.colorScheme.inverseSurface else Color.Transparent
    val content = if (selected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = BorderStroke(
            1.dp,
            if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        ),
        modifier = modifier.height(38.dp).semantics { role = Role.Button }
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge)
            if (count != null) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelLarge,
                    color = content.copy(alpha = 0.6f)
                )
            }
        }
    }
}
