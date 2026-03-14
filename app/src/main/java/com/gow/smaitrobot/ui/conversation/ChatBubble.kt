package com.gow.smaitrobot.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gow.smaitrobot.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure logic object for ChatBubble alignment decisions.
 * Extracted to a plain Kotlin object for JVM testability without Compose runtime.
 */
object ChatBubbleAlignment {
    /**
     * Returns true if the message should be right-aligned (user message),
     * false if left-aligned (robot message).
     */
    fun isEndAligned(message: ChatMessage): Boolean = message.isUser
}

/**
 * Displays a single chat message as a styled bubble.
 *
 * - User messages: right-aligned, secondary color background
 * - Robot messages: left-aligned, surfaceVariant background
 *
 * @param message The chat message to display.
 */
@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val arrangement = if (isUser) Arrangement.End else Arrangement.Start
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = arrangement
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Text(
                text = message.text,
                color = textColor,
                fontSize = 18.sp
            )
            Text(
                text = formatTimestamp(message.timestamp),
                color = textColor.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun formatTimestamp(epochMs: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(epochMs))
}
