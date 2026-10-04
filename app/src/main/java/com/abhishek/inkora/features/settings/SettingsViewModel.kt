package com.abhishek.inkora.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.inkora.data.repository.InkoraSettings
import com.abhishek.inkora.data.repository.SettingsRepository
import com.abhishek.inkora.domain.model.AccentColor
import com.abhishek.inkora.domain.model.AppTheme
import com.abhishek.inkora.domain.model.PageStyle
import com.abhishek.inkora.domain.model.PaperBackground
import com.abhishek.inkora.domain.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {
    val settings: StateFlow<InkoraSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InkoraSettings())

    fun setTheme(v: AppTheme) = viewModelScope.launch { repo.setTheme(v) }
    fun setAccent(v: AccentColor) = viewModelScope.launch { repo.setAccent(v) }
    fun setDynamic(v: Boolean) = viewModelScope.launch { repo.setDynamic(v) }
    fun setDefBg(v: PaperBackground) = viewModelScope.launch { repo.setDefaultBackground(v) }
    fun setDefStyle(v: PageStyle) = viewModelScope.launch { repo.setDefaultPageStyle(v) }
    fun setTextSize(v: Int) = viewModelScope.launch { repo.setTextSize(v) }
    fun setAutoSave(v: Boolean) = viewModelScope.launch { repo.setAutoSave(v) }
    fun setSort(v: SortOrder) = viewModelScope.launch { repo.setSort(v) }
    fun setGrid(v: Int) = viewModelScope.launch { repo.setGrid(v) }
}
