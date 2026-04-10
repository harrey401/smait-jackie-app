package com.gow.eng192lab.data.model

/**
 * User feedback collected at the end of an interaction session.
 *
 * @param rating            Numeric rating, e.g. 1–5 stars.
 * @param surveyResponses   Map of survey question IDs to free-text answers.
 * @param sessionId         Unique ID of the conversation session being rated.
 * @param timestamp         Unix epoch milliseconds when feedback was submitted.
 */
data class FeedbackData(
    val rating: Int,
    val surveyResponses: Map<String, String> = emptyMap(),
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis()
)
