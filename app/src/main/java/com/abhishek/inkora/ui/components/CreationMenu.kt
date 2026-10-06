package com.abhishek.inkora.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.ui.theme.InkoraTokens

/**
 * Vertical creation menu rising above the + button: Handwriting, Text,
 * To-do list. Compact by design — no full-screen dialog, no extra screens.
 */
@Composable
fun CreationMenu(
    expanded: Boolean,
    onPickHandwriting: () -> Unit,
    onPickText: () -> Unit,
    onPickTodo: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(animationSpec = tween(InkoraTokens.DurationFast)) +
            expandVertically(
                animationSpec = tween(InkoraTokens.DurationFast),
                expandFrom = Alignment.Bottom
            ),
        exit = fadeOut(animationSpec = tween(InkoraTokens.DurationFast)) +
            shrinkVertically(
                animationSpec = tween(InkoraTokens.DurationFast),
                shrinkTowards = Alignment.Bottom
            ),
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            CreationOption(
                icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                label = "Handwriting",
                description = "Create handwriting note",
                onClick = onPickHandwriting
            )
            CreationOption(
                icon = { Icon(Icons.Filled.TextFields, contentDescription = null) },
                label = "Text",
                description = "Create text note",
                onClick = onPickText
            )
            CreationOption(
                icon = { Icon(Icons.Filled.CheckBox, contentDescription = null) },
                label = "To-do list",
                description = "Create to-do list",
                onClick = onPickTodo
            )
        }
    }
}

@Composable
private fun CreationOption(
    icon: @Composable () -> Unit,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(InkoraTokens.ControlRadius),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        modifier = Modifier
            .heightIn(min = InkoraTokens.MinTouch)
            .semantics { contentDescription = description; role = Role.Button }
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                icon()
            }
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
