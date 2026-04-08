package com.gow.smaitrobot.ui.photobooth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.gow.smaitrobot.data.websocket.WebSocketRepository
import com.gow.smaitrobot.ui.conversation.VideoStreamManager

/**
 * Top-level Photo Booth screen composable.
 *
 * Delegates rendering to the appropriate sub-composable based on [PhotoBoothUiState]:
 * - [StylePickerGrid]    — style selection (StylePicker state)
 * - [CountdownOverlay]   — 3-2-1 countdown (Countdown state)
 * - [ProcessingScreen]   — server-side SD inference in progress (Processing state)
 * - [ResultScreen]       — styled result with crossfade + QR + retake (Result state)
 *
 * Photo capture is done by snapshotting the next JPEG emitted by the shared
 * [VideoStreamManager] rather than opening a second Camera2 session — Jackie's
 * USB camera only accepts one owner, so opening it here collided with the
 * live conversation video stream and silently failed.
 *
 * Uses `remember { PhotoBoothViewModel(wsRepo) }` to avoid Android ViewModel lifecycle
 * dependency, matching the ConversationViewModel pattern in AppNavigation.kt.
 */
@Composable
fun PhotoBoothScreen(
    navController: NavHostController,
    wsRepo: WebSocketRepository,
    videoStreamManager: VideoStreamManager,
) {
    val viewModel = remember { PhotoBoothViewModel(wsRepo) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Notify server when screen enters / exits
    LaunchedEffect(Unit) { viewModel.onScreenEntered() }
    DisposableEffect(Unit) {
        onDispose { viewModel.onScreenExited() }
    }

    // Snapshot the next live-stream frame when the countdown finishes and the
    // ViewModel transitions to Processing. The rawBitmap == null guard
    // ensures we only capture once per session (it's cleared by onRetake).
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is PhotoBoothUiState.Processing && viewModel.rawBitmap == null) {
            videoStreamManager.snapshotNextFrame { jpegBytes ->
                viewModel.onPhotoJpegCaptured(jpegBytes)
            }
        }
    }

    when (val state = uiState) {
        is PhotoBoothUiState.StylePicker -> StylePickerGrid(
            selectedStyle = state.selectedStyle,
            mode = state.mode,
            onStyleSelected = viewModel::onStyleSelected,
            onModeSelected = viewModel::onModeSelected,
            onTakePhoto = viewModel::onTakePhoto,
            onBack = { navController.popBackStack() }
        )
        is PhotoBoothUiState.Countdown -> CountdownOverlay(
            secondsLeft = state.secondsLeft
        )
        is PhotoBoothUiState.Processing -> ProcessingScreen(
            styleName = state.styleName
        )
        is PhotoBoothUiState.Result -> ResultScreen(
            state = state,
            onRetake = viewModel::onRetake,
            onExit = {
                viewModel.onScreenExited()
                navController.popBackStack()
            }
        )
    }
}
