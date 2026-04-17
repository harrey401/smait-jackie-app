package com.gow.eng192lab.ui.photobooth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.gow.eng192lab.data.websocket.WebSocketRepository
import com.gow.eng192lab.ui.conversation.VideoStreamManager

/**
 * Top-level Photo Booth screen composable.
 *
 * Delegates rendering to the appropriate sub-composable based on [PhotoBoothUiState]:
 * - [StylePickerGrid]    — 2x2 style card grid (StylePicker state)
 * - [CountdownOverlay]   — 3-2-1 countdown (Countdown state)
 * - [ProcessingScreen]   — server-side inference in progress (Processing state)
 * - [ResultScreen]       — styled result with crossfade + QR + retake (Result state)
 *
 * Photo capture reuses the shared [VideoStreamManager] via `snapshotNextFrame` rather
 * than opening a second Camera2 session — Jackie's USB camera only accepts one owner.
 */
@Composable
fun PhotoBoothScreen(
    navController: NavHostController,
    wsRepo: WebSocketRepository,
    videoStreamManager: VideoStreamManager,
) {
    val viewModel = remember { PhotoBoothViewModel(wsRepo) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            onStyleTapped = { key ->
                viewModel.onStyleSelected(key)
                viewModel.onTakePhoto()
            },
            onNormalCamera = {
                viewModel.onStyleSelected(STYLE_NORMAL)
                viewModel.onTakePhoto()
            },
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
