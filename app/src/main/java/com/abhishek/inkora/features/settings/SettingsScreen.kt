package com.abhishek.inkora.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhishek.inkora.domain.model.SortOrder
import com.abhishek.inkora.ui.components.AccentColorSelector
import com.abhishek.inkora.ui.components.PageStyleSelector
import com.abhishek.inkora.ui.components.ThemeSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("Settings") }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Section("Appearance")
            Label("Theme")
            ThemeSelector(selected = s.theme, onSelect = vm::setTheme)
            Label("Accent", top = 16.dp)
            AccentColorSelector(selected = s.accent, onSelect = vm::setAccent)
            SettingRow("Dynamic color", "Follow the system palette when available") {
                Switch(checked = s.dynamicColor, onCheckedChange = vm::setDynamic)
            }
            HorizontalDivider(Modifier.padding(vertical = 20.dp))
            Section("Editor")
            Label("Default page style")
            PageStyleSelector(selected = s.defaultPageStyle, onSelect = vm::setDefStyle)
            SettingRow("Auto-save", "Notes save themselves as you write") {
                Switch(checked = s.autoSave, onCheckedChange = vm::setAutoSave)
            }
            HorizontalDivider(Modifier.padding(vertical = 20.dp))
            Section("Notes")
            Label("Sort order")
            Row(Modifier.padding(top = 4.dp)) {
                SortOrder.entries.forEach {
                    androidx.compose.material3.FilterChip(
                        selected = it == s.sortOrder,
                        onClick = { vm.setSort(it) },
                        label = { Text(it.key) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 20.dp))
            Section("About")
            Text(
                "Inkora 1.2 — offline-first notebook. No account, no cloud, no tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Label(text: String, top: androidx.compose.ui.unit.Dp = 12.dp) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = top, bottom = 8.dp)
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        control()
    }
}
