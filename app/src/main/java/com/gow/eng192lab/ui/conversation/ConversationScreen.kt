package com.gow.eng192lab.ui.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gow.eng192lab.data.model.RobotState
import com.gow.eng192lab.data.model.UiEvent
import com.gow.eng192lab.ui.common.SubScreenTopBar
import com.gow.eng192lab.ui.common.SurveyScreen
import com.gow.eng192lab.ui.common.WieBackground

// Debug preview: cycle states on long-press, show survey on double-tap.
private val DEBUG_STATES = RobotState.entries.toTypedArray()

/**
 * Primary interaction screen for conversing with Jackie.
 *
 * Layout:
 * - SubScreenTopBar with back arrow
 * - Left (35%): [LabDashboard] — lab identity, mascot tile, rotating suggestions
 * - Right (65%): Chat transcript with a small floating [StatusOrb] in the bottom-right
 *
 * When the session ends (robot state returns to IDLE after conversing),
 * a full-screen [SurveyScreen] overlay replaces the conversation view.
 */
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    navController: NavHostController
) {
    val messages by viewModel.transcript.collectAsState()
    val realRobotState by viewModel.robotState.collectAsState()
    val showSurvey by viewModel.showSurvey.collectAsState()

    // Debug state preview — overrides real state when cycling
    var debugStateIndex by remember { mutableIntStateOf(-1) }
    var debugShowSurvey by remember { mutableStateOf(false) }
    val robotState = if (debugStateIndex >= 0) DEBUG_STATES[debugStateIndex] else realRobotState

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.onScreenEntered()
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onScreenExited()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    navController.navigate(event.screen) {
                        popUpTo(event.screen) { inclusive = true }
                    }
                }
            }
        }
    }

    if (showSurvey || debugShowSurvey) {
        SurveyScreen(
            onSubmit = { survey ->
                debugShowSurvey = false
                viewModel.submitSurvey(survey)
            },
            onDismiss = { survey ->
                debugShowSurvey = false
                viewModel.dismissSurvey(survey)
            }
        )
        return
    }

    WieBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "Chat with Jackie",
                onBack = {
                    viewModel.onBackPressed()
                }
            )

            Box(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left: Lab dashboard (35%)
                    LabDashboard(
                        robotState = robotState,
                        onMascotLongPress = {
                            debugStateIndex = (debugStateIndex + 1) % DEBUG_STATES.size
                        },
                        onMascotDoubleTap = {
                            debugShowSurvey = true
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.35f)
                    )

                    // Right: Transcript (65%) with floating StatusOrb in the bottom-right
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.65f)
                            .padding(end = 12.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
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
                                            color = Color(0xFF1B0A6E).copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }

                        StatusOrb(
                            robotState = robotState,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
