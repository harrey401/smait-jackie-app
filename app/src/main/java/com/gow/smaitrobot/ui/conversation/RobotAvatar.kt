package com.gow.smaitrobot.ui.conversation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment
import app.rive.runtime.kotlin.core.Fit
import com.gow.smaitrobot.R
import com.gow.smaitrobot.data.model.RobotState

/**
 * Animated bear avatar driven by [RobotState] via the Rive state machine in
 * `res/raw/jackie_character.riv` (shared with the smAIT app).
 *
 * Inputs on "State Machine 1" — probed at runtime so renames in Rive don't
 * silently break the avatar:
 * - Booleans: Speaking / Listening / Thinking — driven by [RobotState]
 * - Boolean:  Hide — held false so the bear is always visible
 * - Number:   Look — neutral value (50f) until a server-side action wires it
 *
 * No local TTS amplitude is available in the Jackie app (TTS plays on the
 * robot, not the tablet), so Speaking is held true for the duration of the
 * SPEAKING state instead of being modulated per-frame.
 */
private const val TAG = "RobotAvatar"
private const val SM_NAME = "State Machine 1"

private const val INPUT_HIDE = "Hide"
private const val INPUT_LOOK = "Look"
private val SPEAK_INPUT_NAMES = listOf("Speaking", "Talk")
private val LISTEN_INPUT_NAMES = listOf("Hear", "Listen", "Listening")
private val THINK_INPUT_NAMES = listOf("Check", "Think", "Thinking")

private data class InputCatalog(
    val booleans: Set<String>,
    val numbers: Set<String>,
    val triggers: Set<String>
) {
    fun hasBoolean(name: String) = name in booleans
    fun hasNumber(name: String) = name in numbers
}

@Composable
fun RobotAvatar(
    robotState: RobotState,
    modifier: Modifier = Modifier
) {
    var view by remember { mutableStateOf<RiveAnimationView?>(null) }
    var catalog by remember { mutableStateOf<InputCatalog?>(null) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                RiveAnimationView(ctx).apply {
                    setRiveResource(
                        R.raw.jackie_character,
                        autoplay = true,
                        fit = Fit.CONTAIN,
                        alignment = RiveAlignment.CENTER
                    )
                    runCatching { play(SM_NAME, isStateMachine = true) }
                        .onFailure { Log.w(TAG, "play SM failed: ${it.message}") }

                    val cat = enumerateInputs(this)
                    catalog = cat
                    Log.i(TAG, "Rive ready — inputs: $cat")

                    if (cat.hasBoolean(INPUT_HIDE)) {
                        runCatching { setBooleanState(SM_NAME, INPUT_HIDE, false) }
                    }
                    if (cat.hasNumber(INPUT_LOOK)) {
                        runCatching { setNumberState(SM_NAME, INPUT_LOOK, 50f) }
                    }
                    cat.booleans.forEach { name ->
                        if (name != INPUT_HIDE) {
                            runCatching { setBooleanState(SM_NAME, name, false) }
                        }
                    }
                    view = this
                }
            }
        )
    }

    LaunchedEffect(robotState, catalog) {
        val v = view ?: return@LaunchedEffect
        val cat = catalog ?: return@LaunchedEffect

        val isSpeaking = robotState == RobotState.SPEAKING
        val isListening = robotState == RobotState.LISTENING
        val isThinking = robotState == RobotState.THINKING

        SPEAK_INPUT_NAMES.firstOrNull(cat::hasBoolean)?.let {
            runCatching { v.setBooleanState(SM_NAME, it, isSpeaking) }
        }
        LISTEN_INPUT_NAMES.firstOrNull(cat::hasBoolean)?.let {
            runCatching { v.setBooleanState(SM_NAME, it, isListening) }
        }
        THINK_INPUT_NAMES.firstOrNull(cat::hasBoolean)?.let {
            runCatching { v.setBooleanState(SM_NAME, it, isThinking) }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            view?.stop()
            view = null
        }
    }
}

private fun enumerateInputs(view: RiveAnimationView): InputCatalog {
    val booleans = mutableSetOf<String>()
    val numbers = mutableSetOf<String>()
    val triggers = mutableSetOf<String>()
    runCatching {
        val artboard = view.controller.activeArtboard ?: return@runCatching
        val machineNames: List<String> = artboard.stateMachineNames
        val smName = machineNames.firstOrNull() ?: return@runCatching
        val machine = artboard.stateMachine(smName)
        for (i in 0 until machine.inputCount) {
            val input = machine.input(i)
            val cls = input::class.java.simpleName
            when {
                cls.contains("Boolean", ignoreCase = true) -> booleans += input.name
                cls.contains("Number", ignoreCase = true) -> numbers += input.name
                cls.contains("Trigger", ignoreCase = true) -> triggers += input.name
            }
        }
    }
    return InputCatalog(booleans, numbers, triggers)
}

/**
 * Backward-compatible helper kept for any unit test that mapped each
 * [RobotState] to a non-zero raw resource handle. With the Rive bear there is
 * a single source asset, so all states return the same id.
 */
internal fun robotStateToRawRes(@Suppress("UNUSED_PARAMETER") state: RobotState): Int =
    R.raw.jackie_character
