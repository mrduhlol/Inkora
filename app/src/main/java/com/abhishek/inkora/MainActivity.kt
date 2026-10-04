package com.abhishek.inkora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.abhishek.inkora.features.archive.ArchiveScreen
import com.abhishek.inkora.features.editor.EditorScreen
import com.abhishek.inkora.features.favorites.FavoritesScreen
import com.abhishek.inkora.features.folders.FoldersScreen
import com.abhishek.inkora.features.home.HomeScreen
import com.abhishek.inkora.features.settings.SettingsScreen
import com.abhishek.inkora.features.settings.SettingsViewModel
import com.abhishek.inkora.features.trash.TrashScreen
import com.abhishek.inkora.ui.navigation.InkoraRoute
import com.abhishek.inkora.ui.theme.InkoraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val s by settingsVm.settings.collectAsStateWithLifecycle(
                initialValue = com.abhishek.inkora.data.repository.InkoraSettings()
            )
            InkoraTheme(appTheme = s.theme, accent = s.accent, dynamicColor = s.dynamicColor) {
                val nav = rememberNavController()
                NavHost(nav, startDestination = InkoraRoute.Home) {
                    composable<InkoraRoute.Home> {
                        HomeScreen(
                            onOpenNote = { nav.navigate(InkoraRoute.Editor(it)) },
                            onOpenSettings = { nav.navigate(InkoraRoute.Settings) },
                            onOpenTrash = { nav.navigate(InkoraRoute.Trash) },
                            onOpenFavorites = { nav.navigate(InkoraRoute.Favorites) },
                            onOpenFolders = { nav.navigate(InkoraRoute.Folders) }
                        )
                    }
                    composable<InkoraRoute.Editor> {
                        EditorScreen(onBack = { nav.popBackStack() })
                    }
                    composable<InkoraRoute.Settings> {
                        SettingsScreen(onBack = { nav.popBackStack() })
                    }
                    composable<InkoraRoute.Trash> {
                        TrashScreen(onBack = { nav.popBackStack() })
                    }
                    composable<InkoraRoute.Favorites> {
                        FavoritesScreen(onBack = { nav.popBackStack() }, onOpen = { nav.navigate(InkoraRoute.Editor(it)) })
                    }
                    composable<InkoraRoute.Folders> {
                        FoldersScreen(onBack = { nav.popBackStack() })
                    }
                    composable<InkoraRoute.Archive> {
                        ArchiveScreen(onBack = { nav.popBackStack() }, onOpen = { nav.navigate(InkoraRoute.Editor(it)) })
                    }
                }
            }
        }
    }
}
