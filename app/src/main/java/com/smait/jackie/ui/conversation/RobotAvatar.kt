package com.smait.jackie.ui.conversation

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment
import app.rive.runtime.kotlin.core.Fit
import com.smait.jackie.R
import com.smait.jackie.data.model.RobotState
import com.smait.jackie.data.robot.RobotAction
import com.smait.jackie.jackieApp
import kotlinx.coroutines.delay

private const val TAG = "RobotAvatar"
private const val SM_NAME = "State Machine 1"

private const val INPUT_HIDE = "Hide"
private val SPEAK_INPUT_NAMES = listOf("Speaking", "Talk")
private val LISTEN_INPUT_NAMES = listOf("Hear", "Listen", "Listening")
private val THINK_INPUT_NAMES = listOf("Check", "Think", "Thinking")
private val WAVE_INPUT_NAMES = listOf("hands_up", "Wave", "Waving")
private const val INPUT_LOOK = "Look"
private val SUCCESS_TRIGGER_NAMES = listOf("success", "Success", "Correct")
private val FAIL_TRIGGER_NAMES = listOf("fail", "Fail", "Incorrect")

private data class InputCatalog(
    val booleans: Set<String>,
    val numbers: Set<String>,
    val triggers: Set<String>
) {
    fun hasBoolean(name: String) = name in booleans
    fun hasNumber(name: String) = name in numbers
    fun hasTrigger(name: String) = name in triggers
}

@Composable
fun RobotAvatar(
    robotState: RobotState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ttsPlayer = remember { context.jackieApp.ttsAudioPlayer }
    val actions = remember { context.jackieApp.robotActionController.actions }
    val amplitude by ttsPlayer.amplitude.collectAsStateWithLifecycle()

    var view by remember { mutableStateOf<RiveAnimationView?>(null) }
    var catalog by remember { mutableStateOf<InputCatalog?>(null) }
    var waveOn by remember { mutableStateOf(false) }
    var waveDurationMs by remember { mutableStateOf(0L) }
    var lookValue by remember { mutableStateOf(50f) }

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

    // State-driven booleans (Speaking / Listening / Thinking)
    LaunchedEffect(robotState, amplitude, catalog) {
        val v = view ?: return@LaunchedEffect
        val cat = catalog ?: return@LaunchedEffect

        val isSpeaking = robotState == RobotState.SPEAKING
        val isListening = robotState == RobotState.LISTENING
        val isThinking = robotState == RobotState.THINKING
        val talkOn = isSpeaking && amplitude > 0.06f

        SPEAK_INPUT_NAMES.firstOrNull(cat::hasBoolean)?.let {
            runCatching { v.setBooleanState(SM_NAME, it, talkOn) }
        }
        LISTEN_INPUT_NAMES.firstOrNull(cat::hasBoolean)?.let {
            runCatching { v.setBooleanState(SM_NAME, it, isListening) }
        }
        THINK_INPUT_NAMES.firstOrNull(cat::hasBoolean)?.let {
            runCatching { v.setBooleanState(SM_NAME, it, isThinking) }
        }
    }

    // Server-pushed RobotActions (celebrate / frown / wave / look)
    LaunchedEffect(catalog) {
        val cat = catalog ?: return@LaunchedEffect
        actions.collect { action ->
            val v = view ?: return@collect
            when (action) {
                RobotAction.Celebrate -> SUCCESS_TRIGGER_NAMES
                    .firstOrNull(cat::hasTrigger)
                    ?.let { runCatching { v.fireState(SM_NAME, it) } }
                RobotAction.Frown -> FAIL_TRIGGER_NAMES
                    .firstOrNull(cat::hasTrigger)
                    ?.let { runCatching { v.fireState(SM_NAME, it) } }
                is RobotAction.Wave -> {
                    waveDurationMs = action.durationMs
                    waveOn = true
                }
                is RobotAction.Look -> lookValue = action.value.coerceIn(0f, 100f)
            }
        }
    }

    // Drive transient inputs (wave hold + look value)
    LaunchedEffect(waveOn, catalog) {
        val v = view ?: return@LaunchedEffect
        val cat = catalog ?: return@LaunchedEffect
        WAVE_INPUT_NAMES.firstOrNull(cat::hasBoolean)?.let {
            runCatching { v.setBooleanState(SM_NAME, it, waveOn) }
        }
        if (waveOn && waveDurationMs > 0) {
            delay(waveDurationMs)
            waveOn = false
        }
    }
    LaunchedEffect(lookValue, catalog) {
        val v = view ?: return@LaunchedEffect
        val cat = catalog ?: return@LaunchedEffect
        if (cat.hasNumber(INPUT_LOOK)) {
            runCatching { v.setNumberState(SM_NAME, INPUT_LOOK, lookValue) }
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

@Suppress("unused")
internal fun robotStateToRawRes(@Suppress("UNUSED_PARAMETER") state: RobotState): Int = 0
