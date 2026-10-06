package com.abhishek.inkora.features.favorites

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhishek.inkora.domain.repository.NoteRepository
import com.abhishek.inkora.ui.components.NoteGrid
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FavoritesViewModel @Inject constructor(private val notes: NoteRepository) : ViewModel() {
    val favs = notes.observeFavorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun toggle(id: Long, fav: Boolean) = viewModelScope.launch { notes.setFavorite(id, !fav) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenHandwriting: (Long) -> Unit = onOpen,
    vm: FavoritesViewModel = hiltViewModel()
) {
    val items by vm.favs.collectAsStateWithLifecycle()
    val openById: (Long) -> Unit = { id ->
        val note = items.firstOrNull { it.id == id }
        if (note?.noteType == com.abhishek.inkora.domain.model.NoteType.HANDWRITING) {
            onOpenHandwriting(id)
        } else {
            onOpen(id)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text("Favorites") }
            )
        }
    ) { pad ->
        if (items.isEmpty()) {
            Text("No favorites yet. Star a note to pin it here.", Modifier.padding(pad).padding(24.dp))
        } else {
            NoteGrid(
                notes = items,
                onOpen = openById,
                onToggleFavorite = { id -> items.firstOrNull { it.id == id }?.let { vm.toggle(it.id, it.isFavorite) } },
                modifier = Modifier.padding(pad)
            )
        }
    }
}
