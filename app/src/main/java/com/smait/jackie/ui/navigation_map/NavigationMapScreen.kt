package com.smait.jackie.ui.navigation_map

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.smait.jackie.ui.common.SmaitStatusBar
import com.smait.jackie.ui.common.SubScreenTopBar
import com.smait.jackie.ui.theme.SmaitBlack
import com.smait.jackie.ui.theme.SmaitGreen
import com.smait.jackie.ui.theme.SmaitTextPrimary

private val Accent = SmaitGreen
private val OnDark = SmaitTextPrimary
private val PanelBg = Color(0xFF0E0E0E)
private val PanelBgSoft = Color(0xFF1A1A1A)
private val DimText = OnDark.copy(alpha = 0.55f)

@Composable
fun NavigationMapScreen(
    viewModel: NavigationMapViewModel,
    navController: NavHostController,
) {
    val tourState by viewModel.tourState.collectAsState()
    val stops by viewModel.stops.collectAsState()
    val currentIndex = activeIndex(tourState)
    val currentStop = currentIndex?.let { stops.getOrNull(it) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SmaitBlack),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SmaitStatusBar()
            SubScreenTopBar(
                title = "Office Tour",
                onBack = { navController.popBackStack() },
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                HeroCard(state = tourState, stop = currentStop)
                RouteRail(stops = stops, currentIndex = currentIndex)
                Spacer(Modifier.weight(1f))
                ControlBar(
                    state = tourState,
                    onStart = viewModel::startTour,
                    onPause = viewModel::pauseTour,
                    onResume = viewModel::resumeTour,
                    onSkip = viewModel::skipStop,
                    onEnd = viewModel::endTour,
                    onRetry = viewModel::retryFailedStop,
                )
            }
        }
    }
}

private fun activeIndex(state: TourState): Int? = when (state) {
    is TourState.Navigating -> state.index
    is TourState.Speaking -> state.index
    is TourState.Dwelling -> state.index
    is TourState.NavFailed -> state.index
    is TourState.Paused -> when (val s = state.saved) {
        is TourState.Navigating -> s.index
        is TourState.Speaking -> s.index
        is TourState.Dwelling -> s.index
        is TourState.NavFailed -> s.index
        else -> null
    }
    else -> null
}

@Composable
private fun HeroCard(state: TourState, stop: TourStop?) {
    val phase = when (state) {
        is TourState.Idle -> "READY"
        is TourState.Navigating -> "ON THE MOVE"
        is TourState.Speaking -> "ARRIVED"
        is TourState.Dwelling -> "ARRIVED"
        is TourState.Paused -> "PAUSED"
        is TourState.NavFailed -> "BLOCKED"
        is TourState.Complete -> "TOUR COMPLETE"
    }
    val phaseColor = when (state) {
        is TourState.NavFailed -> Color(0xFFFF6B6B)
        is TourState.Paused -> Color(0xFFFFC857)
        else -> Accent
    }
    val title = when (state) {
        is TourState.Idle -> "Welcome to SMAIT"
        is TourState.Complete -> "Thanks for visiting"
        else -> stop?.title ?: "—"
    }
    val body = when (state) {
        is TourState.Idle ->
            "Tap Start Tour. Jackie will drive you through the office and tell you about each spot along the way."
        is TourState.Speaking -> stop?.narration ?: ""
        is TourState.Navigating -> "Hold tight — heading to ${stop?.title ?: "the next stop"}…"
        is TourState.Dwelling -> "Continuing to the next stop in a moment."
        is TourState.Paused -> "Tour paused. Tap Resume when you're ready to continue."
        is TourState.NavFailed -> "Couldn't reach ${stop?.title ?: "the stop"}. Check the e-stop or path, then Retry — or Skip to move on."
        is TourState.Complete -> "Tap Start Tour to run it again."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, phaseColor.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            phaseColor.copy(alpha = 0.12f),
                            PanelBg,
                        ),
                    ),
                ),
        ) {
            Row(
                modifier = Modifier.padding(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (stop != null && state !is TourState.Idle && state !is TourState.Complete) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(phaseColor.copy(alpha = 0.18f))
                            .border(2.dp, phaseColor, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = stop.icon,
                            contentDescription = null,
                            tint = phaseColor,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                    Spacer(Modifier.width(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = phase,
                        color = phaseColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = title,
                        color = OnDark,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = body,
                        color = OnDark.copy(alpha = 0.82f),
                        fontSize = 22.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteRail(stops: List<TourStop>, currentIndex: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        stops.forEachIndexed { index, stop ->
            StopTile(
                index = index,
                stop = stop,
                isActive = index == currentIndex,
                isDone = currentIndex != null && index < currentIndex,
                modifier = Modifier.weight(1f),
            )
            if (index < stops.lastIndex) {
                Connector(isDone = currentIndex != null && index < currentIndex)
            }
        }
    }
}

@Composable
private fun StopTile(
    index: Int,
    stop: TourStop,
    isActive: Boolean,
    isDone: Boolean,
    modifier: Modifier = Modifier,
) {
    // Pulse the active tile for a soft "now playing" cue.
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = if (isActive) 0.55f else 0f,
        targetValue = if (isActive) 1.0f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse-alpha",
    )
    val iconBg = when {
        isActive -> Accent
        isDone -> Accent.copy(alpha = 0.55f)
        else -> PanelBgSoft
    }
    val iconFg = when {
        isActive -> SmaitBlack
        isDone -> SmaitBlack
        else -> OnDark.copy(alpha = 0.7f)
    }
    val ring = when {
        isActive -> Accent.copy(alpha = pulse)
        isDone -> Accent.copy(alpha = 0.7f)
        else -> OnDark.copy(alpha = 0.18f)
    }
    val titleColor = when {
        isActive -> OnDark
        isDone -> OnDark.copy(alpha = 0.85f)
        else -> DimText
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(iconBg)
                .border(width = 3.dp, color = ring, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = iconFg,
                    modifier = Modifier.size(40.dp),
                )
            } else {
                Icon(
                    imageVector = stop.icon,
                    contentDescription = stop.title,
                    tint = iconFg,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stop.title,
            color = titleColor,
            fontSize = 16.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Stop ${index + 1}",
            color = DimText,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun Connector(isDone: Boolean) {
    val targetAlpha by animateFloatAsState(
        targetValue = if (isDone) 0.7f else 0.2f,
        animationSpec = tween(400),
        label = "connector",
    )
    Row(
        modifier = Modifier.widthIn(min = 24.dp, max = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .height(3.dp)
                .weight(1f)
                .background(
                    color = Accent.copy(alpha = targetAlpha),
                    shape = RoundedCornerShape(2.dp),
                ),
        )
        Icon(
            imageVector = Icons.Filled.ArrowForward,
            contentDescription = null,
            tint = Accent.copy(alpha = targetAlpha),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ControlBar(
    state: TourState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onEnd: () -> Unit,
    onRetry: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state) {
            is TourState.Idle, is TourState.Complete -> {
                PrimaryButton("Start Tour", onStart, Modifier.weight(1f))
            }
            is TourState.Paused -> {
                PrimaryButton("Resume", onResume, Modifier.weight(1f))
                SecondaryButton("Skip", onSkip, Modifier.weight(1f))
                SecondaryButton("End", onEnd, Modifier.weight(1f))
            }
            is TourState.NavFailed -> {
                PrimaryButton("Retry", onRetry, Modifier.weight(1f))
                SecondaryButton("Skip", onSkip, Modifier.weight(1f))
                SecondaryButton("End", onEnd, Modifier.weight(1f))
            }
            else -> {
                SecondaryButton("Pause", onPause, Modifier.weight(1f))
                SecondaryButton("Skip", onSkip, Modifier.weight(1f))
                SecondaryButton("End", onEnd, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = SmaitBlack,
        ),
    ) {
        Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PanelBgSoft,
            contentColor = OnDark,
        ),
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.5f)),
    ) {
        Text(text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}
