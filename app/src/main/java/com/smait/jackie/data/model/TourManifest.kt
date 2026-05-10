package com.smait.jackie.data.model

import com.google.gson.annotations.SerializedName

/**
 * Office tour manifest received from SMAIT over WebSocket as a `tour_manifest`
 * JSON message. Authored in the cockpit web UI; written to disk by the cockpit
 * server; loaded by SMAIT and pushed here so the tablet never needs a rebuild
 * for tour edits.
 *
 * Field names mirror the on-disk schema (snake_case via [SerializedName]) so
 * the same Gson instance can deserialize the WS payload directly.
 */
data class TourManifest(
    @SerializedName("name")
    val name: String = "",

    @SerializedName("dwell_ms")
    val dwellMs: Long = 4000L,

    @SerializedName("stops")
    val stops: List<RemoteTourStop> = emptyList(),
)

data class RemoteTourStop(
    @SerializedName("poi")
    val poi: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("narration")
    val narration: String,

    @SerializedName("icon")
    val icon: String = "",

    @SerializedName("en_route_remark")
    val enRouteRemark: String? = null,
)
