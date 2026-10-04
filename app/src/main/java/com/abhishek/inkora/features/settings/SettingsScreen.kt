package com.abhishek.inkora.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
            Text("Appearance", style = MaterialTheme.typography.titleLarge)
            Text("Theme", modifier = Modifier.padding(top = 12.dp))
            ThemeSelector(selected = s.theme, onSelect = vm::setTheme)
            Text("Accent", modifier = Modifier.padding(top = 12.dp))
            AccentColorSelector(selected = s.accent, onSelect = vm::setAccent)
            androidx.compose.foundation.layout.Row(Modifier.padding(top = 12.dp)) {
                Text("Dynamic color", Modifier.weight(1f))
                Switch(checked = s.dynamicColor, onCheckedChange = vm::setDynamic)
            }
            Text("Editor", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 20.dp))
            Text("Default page style", modifier = Modifier.padding(top = 8.dp))
            PageStyleSelector(selected = s.defaultPageStyle, onSelect = vm::setDefStyle)
            androidx.compose.foundation.layout.Row(Modifier.padding(top = 12.dp)) {
                Text("Auto-save", Modifier.weight(1f))
                Switch(checked = s.autoSave, onCheckedChange = vm::setAutoSave)
            }
            Text("Notes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 20.dp))
            Text("Sort: ${s.sortOrder.key}", modifier = Modifier.padding(top = 8.dp))
            androidx.compose.foundation.layout.Row {
                SortOrder.entries.forEach {
                    androidx.compose.material3.FilterChip(
                        selected = it == s.sortOrder,
                        onClick = { vm.setSort(it) },
                        label = { Text(it.key) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Text("About", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 20.dp))
            Text("Inkora 1.0.0 — offline-first notebook. No account, no cloud, no tracking.", modifier = Modifier.padding(top = 8.dp))
        }
    }
}
