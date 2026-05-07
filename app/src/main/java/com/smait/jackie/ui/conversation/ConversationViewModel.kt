package com.smait.jackie.ui.conversation

import android.util.Log
import com.smait.jackie.CaeAudioManager
import com.smait.jackie.TtsAudioPlayer
import com.smait.jackie.data.model.ChatMessage
import com.smait.jackie.data.model.RobotState
import com.smait.jackie.data.model.UiEvent
import com.smait.jackie.data.websocket.WebSocketEvent
import com.smait.jackie.data.websocket.WebSocketRepository
import com.smait.jackie.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

private const val TAG = "ConversationVM"
private const val SILENCE_TIMEOUT_MS = 30_000L

class ConversationViewModel(
    private val wsRepo: WebSocketRepository,
    private val ttsPlayer: TtsAudioPlayer,
    private val caeAudioManager: CaeAudioManager,
    private val videoStreamManager: VideoStreamManager,
    coroutineScope: CoroutineScope? = null
) {
    private val scope: CoroutineScope = coroutineScope
        ?: CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _transcript = MutableStateFlow<List<ChatMessage>>(emptyList())
    val transcript: StateFlow<List<ChatMessage>> = _transcript.asStateFlow()

    private val _robotState = MutableStateFlow(RobotState.IDLE)
    val robotState: StateFlow<RobotState> = _robotState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents: Flow<UiEvent> = _uiEvents.receiveAsFlow()

    private var sessionActive = false
    private var isNavigating = false
    private var silenceJob: Job? = null
    private var wasConversing = false

    init {
        caeAudioManager.setWriterCallback { bytes -> wsRepo.send(bytes) }
        caeAudioManager.setTextWriterCallback { json -> wsRepo.send(json) }

        resetSilenceTimer()

        scope.launch {
            wsRepo.events.collect { event ->
                resetSilenceTimer()
                when (event) {
                    is WebSocketEvent.JsonMessage -> handleJsonMessage(event)
                    is WebSocketEvent.BinaryFrame -> handleBinaryFrame(event.bytes)
                    is WebSocketEvent.Connected -> Log.d(TAG, "WS connected")
                    is WebSocketEvent.Disconnected -> Log.d(TAG, "WS disconnected: ${event.reason}")
                }
            }
        }
    }

    fun onScreenEntered() {
        clearTranscript()
        _robotState.value = RobotState.IDLE
        sessionActive = true
        wasConversing = false
        sendSessionCommand("start")
        resetSilenceTimer()
    }

    fun onBackPressed() {
        endSession()
        scope.launch {
            _uiEvents.send(UiEvent.NavigateTo(Screen.Home))
        }
    }

    fun onScreenExited() {
        if (sessionActive) {
            sendSessionCommand("end")
            sessionActive = false
        }
        clearTranscript()
        silenceJob?.cancel()
    }

    private fun endSession() {
        if (sessionActive) {
            sendSessionCommand("end")
            sessionActive = false
        }
        clearTranscript()
        silenceJob?.cancel()
    }

    fun clearTranscript() {
        _transcript.value = emptyList()
    }

    fun onCleared() {
        sendSessionCommand("end")
        caeAudioManager.stop()
        videoStreamManager.stop()
        silenceJob?.cancel()
        scope.cancel()
    }

    private fun handleJsonMessage(event: WebSocketEvent.JsonMessage) {
        val payload = event.payload
        when (event.type) {
            "transcript" -> {
                val text = parseTextField(payload, "text") ?: return
                val speaker = parseTextField(payload, "speaker") ?: "user"
                val isUser = speaker != "robot"
                appendMessage(ChatMessage(id = UUID.randomUUID().toString(), text = text, isUser = isUser))
            }
            "response" -> {
                val text = parseTextField(payload, "text") ?: return
                appendMessage(ChatMessage(id = UUID.randomUUID().toString(), text = text, isUser = false))
            }
            "state" -> {
                val sessionState = parseTextField(payload, "state") ?: "engaged"
                val robotStatus = parseTextField(payload, "robot_status") ?: "listening"
                val newState = if (sessionState == "idle") RobotState.IDLE else mapRobotState(robotStatus)
                val prevState = _robotState.value
                _robotState.value = newState

                if (newState != RobotState.IDLE) {
                    wasConversing = true
                } else if (wasConversing && prevState != RobotState.IDLE) {
                    // Session ended (goodbye or server timeout): return to Home
                    wasConversing = false
                    endSession()
                    scope.launch {
                        _uiEvents.send(UiEvent.NavigateTo(Screen.Home))
                    }
                }
            }
            "nav_status" -> {
                val status = parseTextField(payload, "status") ?: return
                when (status) {
                    "navigating" -> {
                        isNavigating = true
                        silenceJob?.cancel()
                    }
                    "arrived", "failed" -> {
                        isNavigating = false
                        resetSilenceTimer()
                    }
                }
            }
            "tts_control" -> {
                val cmd = parseTextField(payload, "command")
                if (cmd == "stop") ttsPlayer.stop()
            }
            else -> Log.v(TAG, "Ignoring JSON message type: ${event.type}")
        }
    }

    private fun handleBinaryFrame(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        when (bytes[0]) {
            0x05.toByte() -> ttsPlayer.handleBinaryFrame(bytes)
            else -> Log.v(TAG, "Ignoring inbound binary frame type: 0x${bytes[0].toUByte().toString(16)}")
        }
    }

    private fun appendMessage(message: ChatMessage) {
        _transcript.value = _transcript.value + message
    }

    private fun mapRobotState(value: String): RobotState = when (value.lowercase()) {
        "listening" -> RobotState.LISTENING
        "thinking" -> RobotState.THINKING
        "speaking" -> RobotState.SPEAKING
        "idle" -> RobotState.IDLE
        else -> {
            Log.w(TAG, "Unknown robot state value: $value")
            RobotState.IDLE
        }
    }

    private fun parseTextField(payload: String, field: String): String? {
        return try {
            JSONObject(payload).optString(field).takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.w(TAG, "JSON parse error for field '$field': ${e.message}")
            null
        }
    }

    private fun sendSessionCommand(action: String) {
        val json = JSONObject().apply {
            put("type", "session_command")
            put("action", action)
        }.toString()
        wsRepo.send(json)
        Log.d(TAG, "Sent session_command: $action")
    }

    private fun resetSilenceTimer() {
        silenceJob?.cancel()
        if (isNavigating) return
        silenceJob = scope.launch {
            delay(SILENCE_TIMEOUT_MS)
            Log.d(TAG, "Silence timeout — returning to Home")
            endSession()
            _uiEvents.send(UiEvent.NavigateTo(Screen.Home))
        }
    }
}
