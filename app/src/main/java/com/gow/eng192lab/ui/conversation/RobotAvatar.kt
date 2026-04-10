package com.gow.eng192lab.ui.conversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.gow.eng192lab.R
import com.gow.eng192lab.data.model.RobotState

/**
 * Animated robot avatar driven by [RobotState].
 *
 * Uses Lottie animations with infinite looping and crossfade transitions between states.
 * Four state-specific animations are preloaded for instant playback on state change:
 * - [RobotState.IDLE]      → robot_idle.json (gentle pulse)
 * - [RobotState.LISTENING] → robot_listening.json (ear wiggle)
 * - [RobotState.THINKING]  → robot_thinking.json (spinning dots)
 * - [RobotState.SPEAKING]  → robot_speaking.json (mouth movement)
 *
 * @param robotState  Current robot behavior state from [ConversationViewModel.robotState].
 * @param modifier    Layout modifier (defaults to 200dp x 200dp if not overridden).
 */
@Composable
fun RobotAvatar(
    robotState: RobotState,
    modifier: Modifier = Modifier
) {
    val rawRes = robotStateToRawRes(robotState)

    AnimatedContent(
        targetState = rawRes,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        modifier = modifier.size(200.dp),
        label = "RobotAvatarTransition"
    ) { res ->
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(res))
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )
        LottieAnimation(
            composition = composition,
            progress = { progress }
        )
    }
}

/**
 * Maps a [RobotState] to its corresponding Lottie raw resource ID.
 * Internal for unit testability.
 */
internal fun robotStateToRawRes(state: RobotState): Int = when (state) {
    RobotState.IDLE      -> R.raw.robot_idle
    RobotState.LISTENING -> R.raw.robot_listening
    RobotState.THINKING  -> R.raw.robot_thinking
    RobotState.SPEAKING  -> R.raw.robot_speaking
}
