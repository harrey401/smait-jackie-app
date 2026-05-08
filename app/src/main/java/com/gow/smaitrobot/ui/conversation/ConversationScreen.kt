package com.gow.smaitrobot.ui.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gow.smaitrobot.data.model.UiEvent
import com.gow.smaitrobot.ui.common.SubScreenTopBar
import com.gow.smaitrobot.ui.common.NasaTlxScreen
import com.gow.smaitrobot.ui.common.WieBackground

/**
 * "Ask Me Anything" — primary interaction screen.
 *
 * Layout mirrors the smAIT app:
 * - Left 50%: large [RobotAvatar] (bear, ~620dp) centered.
 * - Right 50%: scrolling chat transcript.
 *
 * When the session ends (state returns to IDLE after conversing), a full-screen
 * [SurveyScreen] overlay replaces the conversation view.
 */
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    navController: NavHostController
) {
    val messages by viewModel.transcript.collectAsState()
    val robotState by viewModel.robotState.collectAsState()
    val showSurvey by viewModel.showSurvey.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.onScreenEntered() }
    DisposableEffect(Unit) { onDispose { viewModel.onScreenExited() } }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.NavigateTo -> navController.navigate(event.screen) {
                    popUpTo(event.screen) { inclusive = true }
                }
            }
        }
    }

    if (showSurvey) {
        NasaTlxScreen(
            onSubmit = { tlx -> viewModel.submitNasaTlx(tlx) },
            onDismiss = { tlx -> viewModel.dismissNasaTlx(tlx) }
        )
        return
    }

    WieBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "Ask Me Anything",
                onBack = { viewModel.onBackPressed() }
            )

            Box(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.5f)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        EventInfoPanel()
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            RobotAvatar(
                                robotState = robotState,
                                modifier = Modifier.size(440.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.5f)
                            .padding(end = 12.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(messages, key = { it.id }) { message ->
                                ChatBubble(message = message)
                            }
                            if (messages.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Say something to start the conversation",
                                            fontSize = 32.sp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ScheduleSlot(val startMin: Int, val title: String, val location: String)

// Attendee-facing schedule. The full all-day setup/DAC schedule lives in the
// server-side LLM context for question answering; this panel shows only what
// guests at the Alumni & Scholarships Event actually attend.
private val ALUMNI_SCHEDULE = listOf(
    ScheduleSlot(16 * 60, "Doors open - Senior Project Showcase begins", "Student Union Ballroom C"),
    ScheduleSlot(16 * 60, "Industry & Alumni Mixer begins", "Student Union Ballrooms"),
    ScheduleSlot(17 * 60 + 30, "Dinner Service begins", "Food Stations - Student Union Ballroom B"),
    ScheduleSlot(18 * 60, "Scholarship Awards Ceremony", "Mainstage - Student Union Ballroom A"),
    ScheduleSlot(19 * 60 + 30, "Announcement of Most Engaging Senior Project Winner", "Mainstage - Student Union Ballroom A"),
    ScheduleSlot(19 * 60 + 45, "Raffle Drawing", "Mainstage - Student Union Ballroom A"),
    ScheduleSlot(20 * 60, "Closing Remarks", "Mainstage - Student Union Ballroom A"),
    ScheduleSlot(20 * 60, "Event ends", "")
)

private fun fmt(min: Int): String {
    val t = LocalTime.of(min / 60, min % 60)
    return t.format(DateTimeFormatter.ofPattern("h:mm a"))
}

@Composable
private fun EventInfoPanel() {
    val scheme = MaterialTheme.colorScheme
    var nowTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowTime = LocalTime.now()
            delay(30_000L)
        }
    }
    val nowMin = nowTime.hour * 60 + nowTime.minute
    val current = ALUMNI_SCHEDULE.lastOrNull { it.startMin <= nowMin }
    val upNext = ALUMNI_SCHEDULE.firstOrNull { it.startMin > nowMin }
    val upcoming = ALUMNI_SCHEDULE.filter { it.startMin > nowMin }.take(3)
    val timeStr = nowTime.format(DateTimeFormatter.ofPattern("h:mm a"))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.primary, RoundedCornerShape(24.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ─── Header ───────────────────────────────────────────────
        Text(
            text = "Mechanical Engineering",
            color = scheme.onPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 30.sp
        )
        Text(
            text = "Alumni & Scholarships Event",
            color = scheme.secondary,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 40.sp
        )
        Text(
            text = "Thursday, May 8, 2026   •   $timeStr",
            color = scheme.onPrimary.copy(alpha = 0.9f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Gold separator
        Box(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 16.dp)
                .fillMaxWidth()
                .height(3.dp)
                .background(scheme.secondary, RoundedCornerShape(2.dp))
        )

        // ─── Happening now ───────────────────────────────────────
        if (current != null) {
            Text(
                text = "HAPPENING NOW",
                color = scheme.secondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = current.title,
                color = scheme.onPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp
            )
            Text(
                text = current.location,
                color = scheme.onPrimary.copy(alpha = 0.88f),
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
        } else {
            Text(
                text = "EVENT BEGINS AT 4:00 PM",
                color = scheme.secondary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Up next mini-timeline ───────────────────────────────
        if (upcoming.isNotEmpty()) {
            Text(
                text = "UP NEXT",
                color = scheme.secondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            upcoming.forEach { slot ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = fmt(slot.startMin),
                        color = scheme.secondary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(96.dp)
                    )
                    Text(
                        text = slot.title,
                        color = scheme.onPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Vote-to-win line ─────────────────────────────────────
        Text(
            text = "Vote: collect 9 stickers from 9 different teams, then submit at the voting table to be entered into tonight's raffle.",
            color = scheme.onPrimary.copy(alpha = 0.88f),
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium
        )

    }
}

