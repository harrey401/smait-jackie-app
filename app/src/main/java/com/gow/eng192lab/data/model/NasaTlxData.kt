package com.gow.eng192lab.data.model

/**
 * NASA-TLX subscale responses on the standard 0-100 scale (5-point increments,
 * stored as raw integer 0-100). Anchors:
 *  - Mental, Physical, Temporal, Effort, Frustration: Low (0) -> High (100)
 *  - Performance: Perfect (0) -> Failure (100)  (NOTE: reversed anchor by convention)
 *
 * -1 means "not answered" — used when participant skips the survey or skips
 * an individual subscale. Analysis should drop rows containing -1.
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
