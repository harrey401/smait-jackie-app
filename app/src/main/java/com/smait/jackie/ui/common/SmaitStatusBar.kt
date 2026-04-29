package com.smait.jackie.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smait.jackie.data.model.RobotState
import com.smait.jackie.jackieApp
import com.smait.jackie.ui.theme.SmaitGreen
import com.smait.jackie.ui.theme.SmaitTextMuted
import com.smait.jackie.ui.theme.SmaitTextPrimary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Disconnected = Color(0xFFFF6B6B)

@Composable
fun SmaitStatusBar(
    robotState: RobotState? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wsRepo = context.jackieApp.webSocketRepository
    val connected by wsRepo.isConnected.collectAsStateWithLifecycle()

    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(15_000L)
        }
    }
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Pill {
            PulseDot(color = if (connected) SmaitGreen else Disconnected)
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (connected) "Connected" else "Offline",
                color = SmaitTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (robotState != null && robotState != RobotState.IDLE) {
            Pill {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SmaitGreen)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = robotStateLabel(robotState),
                    color = SmaitTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Pill {
            Text(
                text = timeFmt.format(now),
                color = SmaitTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = dateFmt.format(now),
                color = SmaitTextMuted,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun Pill(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF0E0E0E))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

@Composable
private fun PulseDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(color)
    )
}

private fun robotStateLabel(state: RobotState): String = when (state) {
    RobotState.LISTENING -> "Listening"
    RobotState.THINKING -> "Thinking"
    RobotState.SPEAKING -> "Speaking"
    RobotState.IDLE -> "Idle"
}
