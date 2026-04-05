package com.gow.smaitrobot.ui.photobooth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.gow.smaitrobot.data.websocket.WebSocketRepository

/**
 * Top-level Photo Booth screen composable.
 *
 * Delegates rendering to the appropriate sub-composable based on [PhotoBoothUiState].
 * State transitions: StylePicker -> Countdown -> Processing -> Result (Plan 03).
 *
 * Uses `remember { PhotoBoothViewModel(wsRepo) }` to avoid Android ViewModel lifecycle
 * dependency, matching the ConversationViewModel pattern in AppNavigation.kt.
 */
@Composable
fun PhotoBoothScreen(navController: NavHostController, wsRepo: WebSocketRepository) {
    val viewModel = remember { PhotoBoothViewModel(wsRepo) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Notify server when screen enters / exits
    LaunchedEffect(Unit) { viewModel.onScreenEntered() }
    DisposableEffect(Unit) { onDispose { viewModel.onScreenExited() } }

    when (val state = uiState) {
        is PhotoBoothUiState.StylePicker -> StylePickerGrid(
            selectedStyle = state.selectedStyle,
            onStyleSelected = viewModel::onStyleSelected,
            onTakePhoto = viewModel::onTakePhoto,
            onBack = { navController.popBackStack() }
        )
        is PhotoBoothUiState.Countdown -> CountdownOverlay(
            secondsLeft = state.secondsLeft
        )
        is PhotoBoothUiState.Processing -> ProcessingScreen(
            styleName = state.styleName
        )
        is PhotoBoothUiState.Result -> {
            // Plan 03 adds ResultScreen here
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Result — Plan 03")
            }
        }
    }
}
