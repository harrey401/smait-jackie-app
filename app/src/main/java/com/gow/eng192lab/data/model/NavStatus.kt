package com.gow.eng192lab.data.model

/**
 * Navigation status emitted by the SMAIT server during a guided tour.
 *
 * @param destination   Human-readable destination name (e.g. "Room 234").
 * @param status        One of: "navigating", "arrived", "failed", "cancelled".
 * @param progress      Estimated completion fraction in [0.0, 1.0]. Defaults to 0.
 */
data class NavStatus(
    val destination: String,
    val status: String,
    val progress: Float = 0f
)
