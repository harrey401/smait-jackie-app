package com.gow.smaitrobot.data.model

/**
 * A single message in the chat conversation.
 *
 * @param id        Unique identifier for this message (UUID or sequential string).
 * @param text      The displayed message text.
 * @param isUser    True if sent by the human user; false if sent by the robot/AI.
 * @param timestamp Unix epoch milliseconds when the message was created.
 */
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
