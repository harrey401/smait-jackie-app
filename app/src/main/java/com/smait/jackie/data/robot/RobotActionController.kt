package com.smait.jackie.data.robot

import android.util.Log
import com.smait.jackie.data.websocket.WebSocketEvent
import com.smait.jackie.data.websocket.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "RobotAction"

/**
 * One-shot action emitted to the on-screen Rive character.
 *
 * The server emits these as JSON over the existing WebSocket:
 * ```
 * {"type":"robot_action","action":"celebrate"}
 * {"type":"robot_action","action":"wave","duration_ms":2000}
 * {"type":"robot_action","action":"look","value":75}
 * ```
 * The LLM/server is the source of truth for when these fire — typically
 * the LLM emits action tags inline with TTS text and the server forwards
 * them. The client side is just a player.
 */
sealed class RobotAction {
    object Celebrate : RobotAction()      // bear's "success" trigger — for compliments / yes / found-it
    object Frown : RobotAction()          // bear's "fail" trigger    — for sorry / not-found / no
    data class Wave(val durationMs: Long = 2000L) : RobotAction()
    data class Look(val value: Float) : RobotAction()  // 0..100 head turn
}

/**
 * Listens to incoming WebSocket JSON messages with type "robot_action" and
 * re-emits them as [RobotAction]s on a [SharedFlow] that any composable
 * showing the bear can collect.
 *
 * Exposed via [com.smait.jackie.JackieApplication.robotActionController].
 */
class RobotActionController(
    private val wsRepo: WebSocketRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _actions = MutableSharedFlow<RobotAction>(
        replay = 0,
        extraBufferCapacity = 8
    )
    val actions: SharedFlow<RobotAction> = _actions.asSharedFlow()

    fun start() {
        scope.launch {
            wsRepo.events.filterIsInstance<WebSocketEvent.JsonMessage>().collect { event ->
                if (event.type != "robot_action") return@collect
                val action = parse(event.payload) ?: return@collect
                _actions.emit(action)
                Log.i(TAG, "emit $action")
            }
        }
    }

    /** Used by client-side UI to fire actions without going through the server. */
    suspend fun emit(action: RobotAction) {
        _actions.emit(action)
    }

    private fun parse(payload: String): RobotAction? {
        return try {
            val json = JSONObject(payload)
            when (val name = json.optString("action").lowercase()) {
                "celebrate", "success", "yes" -> RobotAction.Celebrate
                "frown", "fail", "sorry", "no" -> RobotAction.Frown
                "wave", "hands_up" -> RobotAction.Wave(
                    durationMs = json.optLong("duration_ms", 2000L)
                )
                "look" -> {
                    val v = json.optDouble("value", 50.0).toFloat().coerceIn(0f, 100f)
                    RobotAction.Look(v)
                }
                else -> {
                    Log.w(TAG, "unknown action: $name")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parse failed: ${e.message}")
            null
        }
    }
}
