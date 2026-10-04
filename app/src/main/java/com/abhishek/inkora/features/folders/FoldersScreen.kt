package com.abhishek.inkora.features.folders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhishek.inkora.domain.repository.FolderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FoldersViewModel @Inject constructor(private val folders: FolderRepository) : ViewModel() {
    val all = folders.observeFolders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun create(name: String) = viewModelScope.launch { if (name.isNotBlank()) folders.create(name) }
    fun rename(id: Long, name: String) = viewModelScope.launch { folders.rename(id, name) }
    fun delete(id: Long) = viewModelScope.launch { folders.delete(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(onBack: () -> Unit, vm: FoldersViewModel = hiltViewModel()) {
    val items by vm.all.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("Folders") },
                actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "New folder") } }
            )
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            items(items, key = { it.id }) { f ->
                ListItem(
                    headlineContent = { Text(f.name) },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { vm.delete(f.id) }) { Icon(Icons.Filled.Delete, "Delete folder") }
                        }
                    }
                )
            }
            if (items.isEmpty()) {
                item { Text("No folders yet. Keep it simple: create one for a project.", Modifier.padding(24.dp)) }
            }
        }
        if (showAdd) {
            AlertDialog(
                onDismissRequest = { showAdd = false },
                confirmButton = {
                    TextButton(onClick = { vm.create(name); name = ""; showAdd = false }) { Text("Create") }
                },
                dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } },
                title = { Text("New folder") },
                text = {
                    Column {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            )
        }
    }
}
