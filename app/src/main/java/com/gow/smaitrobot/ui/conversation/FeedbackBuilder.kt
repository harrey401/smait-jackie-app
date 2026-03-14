package com.gow.smaitrobot.ui.conversation

import com.gow.smaitrobot.data.model.FeedbackData

/**
 * Pure logic object for building [FeedbackData] from dialog state.
 * Extracted to a plain Kotlin object for JVM testability without Compose runtime.
 */
object FeedbackBuilder {

    /**
     * Returns true if [rating] is within the valid 1-5 range.
     */
    fun isValidRating(rating: Int): Boolean = rating in 1..5

    /**
     * Returns true if the auto-dismiss timeout has elapsed.
     *
     * @param startTimeMs   Unix epoch ms when the dialog was shown.
     * @param timeoutMs     Duration before auto-dismiss (default 10 seconds).
     */
    fun isAutoTimeoutDue(startTimeMs: Long, timeoutMs: Long = 10_000L): Boolean {
        return System.currentTimeMillis() - startTimeMs >= timeoutMs
    }

    /**
     * Builds a [FeedbackData] object from the dialog's current state.
     *
     * @param rating            1-5 star rating selected by the user.
     * @param sessionId         Session ID for the conversation being rated.
     * @param surveyResponses   Map of question IDs to answers (empty if survey skipped).
     */
    fun build(
        rating: Int,
        sessionId: String,
        surveyResponses: Map<String, String> = emptyMap()
    ): FeedbackData = FeedbackData(
        rating = rating,
        surveyResponses = surveyResponses,
        sessionId = sessionId,
        timestamp = System.currentTimeMillis()
    )
}
