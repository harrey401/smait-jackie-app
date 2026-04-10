package com.gow.eng192lab.data.model

/**
 * A point of interest (POI) shown in the wayfinding / guided tour flow.
 *
 * @param name      Internal machine-readable identifier matching the chassis POI map key.
 * @param humanName Human-friendly display name (e.g. "Main Auditorium").
 * @param category  Optional category tag (e.g. "restroom", "lab", "lecture", "exit").
 * @param floor     Optional floor label (e.g. "1F", "B1", "Ground").
 */
data class PoiItem(
    val name: String,
    val humanName: String,
    val category: String = "",
    val floor: String = ""
)
