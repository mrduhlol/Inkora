package com.abhishek.inkora.ui.navigation

import kotlinx.serialization.Serializable

sealed interface InkoraRoute {
    @Serializable data object Home : InkoraRoute
    @Serializable data class Editor(val noteId: Long) : InkoraRoute
    @Serializable data class Handwriting(val noteId: Long) : InkoraRoute
    @Serializable data object Settings : InkoraRoute
    @Serializable data object Trash : InkoraRoute
    @Serializable data object Favorites : InkoraRoute
    @Serializable data object Folders : InkoraRoute
    @Serializable data object Archive : InkoraRoute
}
