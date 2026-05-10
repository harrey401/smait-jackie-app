package com.smait.jackie.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Elevator
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.Wc
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Resolves a string icon name (as authored in the cockpit Tours UI) to a
 * Material Compose [ImageVector]. Unknown names return [Icons.Filled.Place]
 * — that's intentional. The cockpit field is freeform so an operator typo
 * shouldn't crash the tour; a generic pin is good enough until the typo is
 * fixed.
 *
 * Match is case-insensitive and ignores whitespace so "Table Restaurant"
 * and "tableRestaurant" both work.
 */
fun iconForName(name: String?): ImageVector {
    val key = name?.replace(" ", "")?.lowercase() ?: return Icons.Filled.Place
    return when (key) {
        "login" -> Icons.Filled.Login
        "build" -> Icons.Filled.Build
        "tablerestaurant" -> Icons.Filled.TableRestaurant
        "apartment" -> Icons.Filled.Apartment
        "home" -> Icons.Filled.Home
        "meetingroom" -> Icons.Filled.MeetingRoom
        "coffee" -> Icons.Filled.Coffee
        "restaurant" -> Icons.Filled.Restaurant
        "print", "printer" -> Icons.Filled.Print
        "computer" -> Icons.Filled.Computer
        "wc", "restroom", "bathroom" -> Icons.Filled.Wc
        "elevator", "lift" -> Icons.Filled.Elevator
        "stairs" -> Icons.Filled.Stairs
        "place", "" -> Icons.Filled.Place
        else -> Icons.Filled.Place
    }
}
