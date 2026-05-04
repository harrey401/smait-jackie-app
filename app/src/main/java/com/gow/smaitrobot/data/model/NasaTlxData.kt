package com.gow.smaitrobot.data.model

/**
 * NASA-TLX subscale responses on the standard 0-100 scale (5-point increments,
 * stored as raw integer 0-100). Anchors:
 *  - Mental, Physical, Temporal, Effort, Frustration: Low (0) -> High (100)
 *  - Performance: Perfect (0) -> Failure (100)  (NOTE: reversed anchor by convention)
 *
 * 0 means "not answered" only when [completedInTime] is false. When
 * [completedInTime] is true all six values are user-set in 0..100 range.
 */
data class NasaTlxData(
    val mental: Int,
    val physical: Int,
    val temporal: Int,
    val performance: Int,
    val effort: Int,
    val frustration: Int,
    val completedInTime: Boolean,
    val timeToCompleteMs: Long,
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis()
)
