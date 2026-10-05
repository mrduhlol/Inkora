package com.abhishek.inkora.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhishek.inkora.domain.model.SortOrder
import com.abhishek.inkora.ui.components.AccentColorSelector
import com.abhishek.inkora.ui.components.PageStyleSelector
import com.abhishek.inkora.ui.components.ThemeSelector
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val bytes = vm.exportBytes()
            if (bytes != null) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        ?: throw IllegalStateException("unwritable")
                }
            }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes != null) vm.importBytes(bytes)
        }
    }

    LaunchedEffect(Unit) { vm.refreshStorage() }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
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
                        label = { Text(sortLabel(it)) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Label("Card density", top = 16.dp)
            Row(Modifier.padding(top = 4.dp)) {
                com.abhishek.inkora.domain.model.CardDensity.entries.forEach {
                    androidx.compose.material3.FilterChip(
                        selected = it == s.cardDensity,
                        onClick = { vm.setDensity(it) },
                        label = { Text(it.key) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 20.dp))
            Section("Privacy")
            SettingRow("App lock", "Require your device screen lock to open Inkora") {
                Switch(checked = s.appLock, onCheckedChange = vm::setAppLock)
            }
            SettingRow("Hide previews", "Show “Locked note” instead of content in lists") {
                Switch(checked = s.hidePreviews, onCheckedChange = vm::setHidePreviews)
            }
            SettingRow("Secure screenshots", "Block screenshots and screen recording (FLAG_SECURE)") {
                Switch(checked = s.secureScreenshots, onCheckedChange = vm::setSecureScreenshots)
            }
            HorizontalDivider(Modifier.padding(vertical = 20.dp))
            Section("Storage & Data")
            Text(
                storage?.let {
                    "Notes: ${it.noteCount} · Trash: ${it.trashCount}\n" +
                        "Database: ${formatBytes(it.dbBytes)} · Images: ${formatBytes(it.imageBytes)}"
                } ?: "Calculating…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
            Row {
                Button(onClick = { exporter.launch("inkora-backup.json") }, modifier = Modifier.padding(end = 8.dp)) {
                    Text("Export notes")
                }
                OutlinedButton(onClick = { importer.launch(arrayOf("application/json")) }) {
                    Text("Import")
                }
            }
            SettingRow("Empty trash", "Permanently delete all trashed notes") {
                TextButton(onClick = { confirmClear = true }) { Text("Empty") }
            }
            HorizontalDivider(Modifier.padding(vertical = 20.dp))
            Section("About")
            Text(
                "Inkora ${com.abhishek.inkora.BuildConfig.VERSION_NAME} — offline-first notebook. No account, no cloud, no tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Source: github.com/mrduhlol/Inkora",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (confirmClear) {
            AlertDialog(
                onDismissRequest = { confirmClear = false },
                confirmButton = {
                    TextButton(onClick = { vm.clearTrash(); confirmClear = false }) { Text("Empty trash") }
                },
                dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Keep") } },
                title = { Text("Empty trash?") },
                text = { Text("All trashed notes and their images will be permanently deleted.") }
            )
        }
    }
}

private fun formatBytes(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "${b / 1024} KB"
    else -> "${b / (1024 * 1024)} MB"
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
}

private fun sortLabel(o: SortOrder): String = when (o) {
    SortOrder.UPDATED_DESC -> "Recently updated"
    SortOrder.CREATED_DESC -> "Recently created"
    SortOrder.TITLE_ASC -> "Title A–Z"
    SortOrder.TITLE_DESC -> "Title Z–A"
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
