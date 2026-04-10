package com.gow.eng192lab.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations for the ENG192 Lab app.
 *
 * Navigation is driven by the Home screen card grid (no bottom nav bar).
 */
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
        override val label: String = "Ask Jackie"
        override val iconName: String = "Chat"
    }

    @Serializable
    object PhotoBooth : Screen() {
        override val label: String = "Photo Booth"
        override val iconName: String = "CameraAlt"
    }

    @Serializable
    object Settings : Screen() {
        override val label: String = "Settings"
        override val iconName: String = "Settings"
    }

    @Serializable
    object Follow : Screen() {
        override val label: String = "Follow Me"
        override val iconName: String = "DirectionsWalk"
    }

    @Serializable
    object LabTour : Screen() {
        override val label: String = "Lab Tour"
        override val iconName: String = "Tour"
    }

    @Serializable
    object Map : Screen() {
        override val label: String = "Map"
        override val iconName: String = "Map"
    }
}
