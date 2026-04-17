package com.gow.eng192lab.ui.conversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gow.eng192lab.data.model.RobotState
import kotlinx.coroutines.delay

private val DeepBlue = Color(0xFF0956A4)
private val DarkNavy = Color(0xFF1A3D6D)
private val Gold = Color(0xFFE8A317)
private val PanelBg = Color(0xFFEEF3FB)

private const val SUGGESTION_ROTATE_MS = 5_000L

private val LAB_SUGGESTIONS = listOf(
    "Where is the Franka robot?",
    "What is the SCARA used for?",
    "Start a lab tour",
    "Tell me about the BioRob Lab",
    "Show me the 3D printers",
    "What projects happen here?",
    "Who is the lab director?",
)

/**
 * Left-panel dashboard for the lab Conversation screen.
 *
 * Replaces the old 240dp Lottie orb with a richer panel:
 *  - Lab identity card
 *  - Jackie mascot tile (state-reactive, debug gestures)
 *  - Rotating "Try asking..." suggestions
 *
 * Lab-focused content — no event schedule or sponsor rotation.
 */
@Composable
fun LabDashboard(
    robotState: RobotState,
    onMascotLongPress: () -> Unit = {},
    onMascotDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LabIdentityCard()

        Box(modifier = Modifier.weight(1.2f)) {
            MascotTile(
                robotState = robotState,
                onLongPress = onMascotLongPress,
                onDoubleTap = onMascotDoubleTap
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            SuggestionsCard()
        }
    }
}

@Composable
private fun LabIdentityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ENG192",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DeepBlue,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "BioRob Lab",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DarkNavy,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MascotTile(
    robotState: RobotState,
    onLongPress: () -> Unit = {},
    onDoubleTap: () -> Unit = {}
) {
    val (stateColor, stateLabel) = when (robotState) {
        RobotState.IDLE -> Gold to "Ready"
        RobotState.LISTENING -> Color(0xFF8BC53F) to "Listening..."
        RobotState.THINKING -> Color(0xFFFBBF24) to "Thinking..."
        RobotState.SPEAKING -> Color(0xFF38BDF8) to "Speaking..."
    }
    Card(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = { },
                onLongClick = onLongPress,
                onDoubleClick = onDoubleTap
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(stateColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = "Jackie",
                        tint = stateColor,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Jackie",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stateLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = stateColor
                )
            }
        }
    }
}

@Composable
private fun SuggestionsCard() {
    var idx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(SUGGESTION_ROTATE_MS)
            idx = (idx + 1) % LAB_SUGGESTIONS.size
        }
    }
    val suggestion = LAB_SUGGESTIONS[idx]

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "TRY ASKING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DeepBlue,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = suggestion,
                transitionSpec = {
                    fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                },
                label = "suggestion_rotate"
            ) { current ->
                Text(
                    text = "\u201C$current\u201D",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkNavy,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

/**
 * Compact status orb — small pill indicator in the bottom-right of the chat area.
 * Single colored dot + state label.
 */
@Composable
fun StatusOrb(robotState: RobotState, modifier: Modifier = Modifier) {
    val (color, label) = when (robotState) {
        RobotState.IDLE -> Color(0xFF9CA3AF) to "Idle"
        RobotState.LISTENING -> Color(0xFF8BC53F) to "Listening"
        RobotState.THINKING -> Color(0xFFFBBF24) to "Thinking"
        RobotState.SPEAKING -> Color(0xFF38BDF8) to "Speaking"
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(PanelBg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkNavy
        )
    }
}
