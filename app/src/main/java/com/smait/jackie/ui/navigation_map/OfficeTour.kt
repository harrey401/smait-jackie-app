package com.smait.jackie.ui.navigation_map

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * One stop on the office tour.
 *
 * @property poi  Exact POI name from the chassis scenegraph (case-sensitive).
 * @property title  Display label.
 * @property narration  Text Jackie speaks at the stop after arriving.
 * @property enRouteRemark  Optional line Jackie speaks the moment we begin
 *                          driving to this stop. Used to "mention while passing"
 *                          a landmark that doesn't deserve its own stop.
 */
data class TourStop(
    val poi: String,
    val title: String,
    val narration: String,
    val icon: ImageVector,
    val enRouteRemark: String? = null,
)

object OfficeTour {
    /** Dwell after narration on a normal stop, in milliseconds. */
    const val DWELL_MS = 4000L

    val stops: List<TourStop> = listOf(
        TourStop(
            poi = "entrance",
            title = "Entrance",
            icon = Icons.Filled.Login,
            narration = "Hi, I'm Jackie. Welcome to SMAIT! Let me show you around the office.",
        ),
        TourStop(
            poi = "workshop",
            title = "Workshop",
            icon = Icons.Filled.Build,
            // Spoken as we start driving — robot moves, we talk in parallel.
            enRouteRemark = "On the way, the restrooms are right here on your left if you need them.",
            narration = "This is our workshop, where the team builds, tests, and tunes robots like me.",
        ),
        TourStop(
            poi = "table",
            title = "Workshop Table",
            icon = Icons.Filled.TableRestaurant,
            narration = "This is our workshop table. It's still being set up — soon we'll use it for demos and team meetings.",
        ),
        TourStop(
            poi = "office",
            title = "Office",
            icon = Icons.Filled.Apartment,
            narration = "And this is the main office, where the SMAIT team works day to day.",
        ),
        TourStop(
            poi = "home",
            title = "Home",
            icon = Icons.Filled.Home,
            narration = "That wraps up the tour. Thanks for visiting SMAIT — feel free to ask me any questions!",
        ),
    )
}

/** UI-visible tour state. */
sealed class TourState {
    object Idle : TourState()
    data class Navigating(val index: Int) : TourState()
    data class Speaking(val index: Int) : TourState()
    data class Dwelling(val index: Int) : TourState()
    data class Paused(val saved: TourState) : TourState()
    /** Navigation failed (e-stop, no path, stuck). User can retry or skip. */
    data class NavFailed(val index: Int) : TourState()
    object Complete : TourState()
}
