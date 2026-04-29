package com.smait.jackie.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {

    abstract val label: String
    abstract val iconName: String

    @Serializable
    object Home : Screen() {
        override val label: String = "Home"
        override val iconName: String = "Home"
    }

    @Serializable
    object Chat : Screen() {
        override val label: String = "Talk"
        override val iconName: String = "Chat"
    }

    @Serializable
    object Map : Screen() {
        override val label: String = "Go"
        override val iconName: String = "Map"
    }

    @Serializable
    object Photo : Screen() {
        override val label: String = "Photo"
        override val iconName: String = "CameraAlt"
    }

    @Serializable
    object Website : Screen() {
        override val label: String = "Website"
        override val iconName: String = "Public"
    }

    @Serializable
    object Settings : Screen() {
        override val label: String = "Settings"
        override val iconName: String = "Settings"
    }
}
