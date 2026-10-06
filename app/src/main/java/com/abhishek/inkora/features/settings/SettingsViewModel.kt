package com.abhishek.inkora.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.inkora.data.repository.DataRepository
import com.abhishek.inkora.data.repository.ImportResult
import com.abhishek.inkora.data.repository.InkoraSettings
import com.abhishek.inkora.data.repository.SettingsRepository
import com.abhishek.inkora.data.repository.StorageStats
import com.abhishek.inkora.domain.model.AccentColor
import com.abhishek.inkora.domain.model.AppTheme
import com.abhishek.inkora.domain.model.CardDensity
import com.abhishek.inkora.domain.model.DisplaySize
import com.abhishek.inkora.domain.model.HomeViewMode
import com.abhishek.inkora.domain.model.PageStyle
import com.abhishek.inkora.domain.model.PaperBackground
import com.abhishek.inkora.domain.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val data: DataRepository
) : ViewModel() {
    val settings: StateFlow<InkoraSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InkoraSettings())

    private val _storage = MutableStateFlow<StorageStats?>(null)
    val storage: StateFlow<StorageStats?> = _storage

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun setTheme(v: AppTheme) = viewModelScope.launch { repo.setTheme(v) }
    fun setAccent(v: AccentColor) = viewModelScope.launch { repo.setAccent(v) }
    fun setDynamic(v: Boolean) = viewModelScope.launch { repo.setDynamic(v) }
    fun setDefBg(v: PaperBackground) = viewModelScope.launch { repo.setDefaultBackground(v) }
    fun setDefStyle(v: PageStyle) = viewModelScope.launch { repo.setDefaultPageStyle(v) }
    fun setTextSize(v: Int) = viewModelScope.launch { repo.setTextSize(v) }
    fun setAutoSave(v: Boolean) = viewModelScope.launch { repo.setAutoSave(v) }
    fun setSort(v: SortOrder) = viewModelScope.launch { repo.setSort(v) }
    fun setGrid(v: Int) = viewModelScope.launch { repo.setGrid(v) }
    fun setViewMode(v: HomeViewMode) = viewModelScope.launch { repo.setViewMode(v) }
    fun setDensity(v: CardDensity) = viewModelScope.launch { repo.setDensity(v) }
    fun setDisplaySize(v: DisplaySize) = viewModelScope.launch { repo.setDisplaySize(v) }
    fun setAppLock(v: Boolean) = viewModelScope.launch { repo.setAppLock(v) }
    fun setHidePreviews(v: Boolean) = viewModelScope.launch { repo.setHidePreviews(v) }
    fun setSecureScreenshots(v: Boolean) = viewModelScope.launch { repo.setSecureScreenshots(v) }

    fun refreshStorage() = viewModelScope.launch {
        _storage.value = runCatching { data.storageStats() }.getOrNull()
    }

    suspend fun exportBytes(): ByteArray? =
        runCatching { data.exportAll() }.getOrNull()

    fun importBytes(bytes: ByteArray) = viewModelScope.launch {
        val r: ImportResult = runCatching { data.importBytes(bytes) }
            .getOrElse { ImportResult(0, 0, 0, 0, it.message) }
        _message.value = if (r.error != null) {
            "Import failed: ${r.error}"
        } else {
            "Imported ${r.notes} notes, ${r.folders} folders, ${r.images} images" +
                if (r.skipped > 0) " (${r.skipped} skipped)" else ""
        }
        refreshStorage()
    }

    fun clearTrash() = viewModelScope.launch {
        val n = runCatching { data.clearTrash() }.getOrDefault(0)
        _message.value = "Trash emptied ($n notes)"
        refreshStorage()
    }

    fun consumeMessage() { _message.value = null }
}
