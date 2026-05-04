package com.gow.eng192lab.ui.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gow.eng192lab.data.model.UiEvent
import com.gow.eng192lab.ui.common.SubScreenTopBar
import com.gow.eng192lab.ui.common.NasaTlxScreen
import com.gow.eng192lab.ui.common.WieBackground

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
    val connected by viewModel.connected.collectAsState()

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
      Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "Ask Me Anything",
                onBack = { viewModel.onBackPressed() }
            )

            Box(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.5f),
                        contentAlignment = Alignment.Center
                    ) {
                        RobotAvatar(
                            robotState = robotState,
                            modifier = Modifier.size(620.dp)
                        )
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

        // Real-time experimenter overlay — connection, robot state, turn count.
        LiveStatusPill(
            connected = connected,
            robotState = robotState,
            turnCount = messages.size,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 16.dp),
        )
      }
    }
}
