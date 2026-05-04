package com.gow.eng192lab.ui.conversation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gow.eng192lab.data.model.RobotState

/**
 * Compact real-time status strip — placed in the top-right of [ConversationScreen]
 * during a session so the experimenter can verify what the system is doing without
 * leaving the room or watching the cockpit:
 *
 *  ● connection state (pulsing green = connected)
 *  ● robot state (IDLE / LISTENING / THINKING / SPEAKING)
 *  ● turn count for the current session
 *
 * Designed to be glanceable, not intrusive. Hidden by default participant view
 * could opt-out, but for lab testing it's always on.
 */
@Composable
fun LiveStatusPill(
    connected: Boolean,
    robotState: RobotState,
    turnCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ConnectionDot(connected = connected)
        Column {
            Text(
                text = robotState.label(),
                color = robotState.tint(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "turns $turnCount",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ConnectionDot(connected: Boolean) {
    if (connected) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val alpha by transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse-alpha",
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935)),
        )
    }
}

private fun RobotState.label(): String = when (this) {
    RobotState.IDLE -> "READY"
    RobotState.LISTENING -> "LISTENING"
    RobotState.THINKING -> "THINKING"
    RobotState.SPEAKING -> "SPEAKING"
}

private fun RobotState.tint(): Color = when (this) {
    RobotState.IDLE -> Color(0xFFB0BEC5)
    RobotState.LISTENING -> Color(0xFF80D8FF)
    RobotState.THINKING -> Color(0xFFFFD54F)
    RobotState.SPEAKING -> Color(0xFFAED581)
}
