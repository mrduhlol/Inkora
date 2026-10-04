package com.abhishek.inkora.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.abhishek.inkora.domain.model.AccentColor
import com.abhishek.inkora.domain.model.AppTheme
import com.abhishek.inkora.domain.model.PageStyle
import com.abhishek.inkora.domain.model.PaperBackground
import com.abhishek.inkora.domain.model.SortOrder
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class InkoraSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val accent: AccentColor = AccentColor.PURPLE,
    val dynamicColor: Boolean = true,
    val defaultBackground: PaperBackground = PaperBackground.CREAM,
    val defaultPageStyle: PageStyle = PageStyle.BLANK,
    val defaultTextSizeSp: Int = 16,
    val autoSave: Boolean = true,
    val sortOrder: SortOrder = SortOrder.UPDATED_DESC,
    val gridColumns: Int = 0 // 0 = adaptive
)

class SettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>
) {
    private object K {
        val THEME = stringPreferencesKey("theme")
        val ACCENT = stringPreferencesKey("accent")
        val DYNAMIC = stringPreferencesKey("dynamic")
        val DEF_BG = stringPreferencesKey("def_bg")
        val DEF_STYLE = stringPreferencesKey("def_style")
        val TEXT_SIZE = intPreferencesKey("text_size")
        val AUTOSAVE = stringPreferencesKey("autosave")
        val SORT = stringPreferencesKey("sort")
        val GRID = intPreferencesKey("grid")
    }

    val settings: Flow<InkoraSettings> = store.data.map { p ->
        InkoraSettings(
            theme = runCatching { AppTheme.valueOf(p[K.THEME] ?: "SYSTEM") }.getOrDefault(AppTheme.SYSTEM),
            accent = runCatching { AccentColor.valueOf(p[K.ACCENT] ?: "PURPLE") }.getOrDefault(AccentColor.PURPLE),
            dynamicColor = (p[K.DYNAMIC] ?: "1") == "1",
            defaultBackground = runCatching { PaperBackground.valueOf(p[K.DEF_BG] ?: "CREAM") }.getOrDefault(PaperBackground.CREAM),
            defaultPageStyle = runCatching { PageStyle.valueOf(p[K.DEF_STYLE] ?: "BLANK") }.getOrDefault(PageStyle.BLANK),
            defaultTextSizeSp = p[K.TEXT_SIZE] ?: 16,
            autoSave = (p[K.AUTOSAVE] ?: "1") == "1",
            sortOrder = runCatching { SortOrder.valueOf(p[K.SORT] ?: "UPDATED_DESC") }.getOrDefault(SortOrder.UPDATED_DESC),
            gridColumns = p[K.GRID] ?: 0
        )
    }

    suspend fun setTheme(v: AppTheme) = store.edit { it[K.THEME] = v.name }
    suspend fun setAccent(v: AccentColor) = store.edit { it[K.ACCENT] = v.name }
    suspend fun setDynamic(v: Boolean) = store.edit { it[K.DYNAMIC] = if (v) "1" else "0" }
    suspend fun setDefaultBackground(v: PaperBackground) = store.edit { it[K.DEF_BG] = v.name }
    suspend fun setDefaultPageStyle(v: PageStyle) = store.edit { it[K.DEF_STYLE] = v.name }
    suspend fun setTextSize(v: Int) = store.edit { it[K.TEXT_SIZE] = v.coerceIn(12, 24) }
    suspend fun setAutoSave(v: Boolean) = store.edit { it[K.AUTOSAVE] = if (v) "1" else "0" }
    suspend fun setSort(v: SortOrder) = store.edit { it[K.SORT] = v.name }
    suspend fun setGrid(v: Int) = store.edit { it[K.GRID] = v.coerceIn(0, 4) }
}
