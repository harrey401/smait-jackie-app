package com.smait.jackie.ui.navigation_map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smait.jackie.data.model.NavStatus
import com.smait.jackie.data.websocket.WebSocketEvent
import com.smait.jackie.data.websocket.WebSocketRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ViewModel for the Office Tour screen.
 *
 * Owns:
 *  - the live map bitmap (binary frame 0x06)
 *  - the latest [NavStatus] from "nav_status" JSON
 *  - the [TourState] state machine that orchestrates the guided tour
 *
 * Tour flow per stop:
 *   1. send `lab_tour_navigate`  → state = Navigating(i)
 *   2. nav_status arrived         → send `lab_tour_tts`, state = Speaking(i)
 *   3. tts_control end            → if driveBy: advance immediately;
 *                                   else: state = Dwelling(i), start timer
 *   4. timer fires                → advance to next stop, or Complete
 */
class NavigationMapViewModel(
    private val wsRepo: WebSocketRepository,
    private val bitmapDecoder: (ByteArray, Int, Int) -> Bitmap? = { bytes, offset, length ->
        BitmapFactory.decodeByteArray(bytes, offset, length)
    },
    private val dispatcher: CoroutineDispatcher? = null
) : ViewModel() {

    private val effectiveScope: CoroutineScope by lazy {
        if (dispatcher != null) CoroutineScope(dispatcher + SupervisorJob()) else viewModelScope
    }

    private val _mapBitmap = MutableStateFlow<Bitmap?>(null)
    val mapBitmap: StateFlow<Bitmap?> = _mapBitmap.asStateFlow()

    private val _navStatus = MutableStateFlow<NavStatus?>(null)
    val navStatus: StateFlow<NavStatus?> = _navStatus.asStateFlow()

    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    private val _tourState = MutableStateFlow<TourState>(TourState.Idle)
    val tourState: StateFlow<TourState> = _tourState.asStateFlow()

    private val stops = OfficeTour.stops
    private var dwellJob: Job? = null

    init {
        effectiveScope.launch {
            wsRepo.events.collect { event ->
                when (event) {
                    is WebSocketEvent.BinaryFrame -> handleBinaryFrame(event.bytes)
                    is WebSocketEvent.JsonMessage -> handleJsonMessage(event.type, event.payload)
                    else -> Unit
                }
            }
        }
    }

    // --- Tour controls ---

    fun startTour() {
        if (stops.isEmpty()) return
        cancelDwell()
        navigateToStop(0)
    }

    fun pauseTour() {
        val current = _tourState.value
        if (current is TourState.Paused || current is TourState.Idle || current is TourState.Complete) return
        cancelDwell()
        if (current is TourState.Speaking) {
            // Stop speech immediately so we don't keep talking after pause.
            sendTtsStop()
        }
        _tourState.value = TourState.Paused(saved = current)
    }

    fun resumeTour() {
        val current = _tourState.value
        if (current !is TourState.Paused) return
        when (val saved = current.saved) {
            is TourState.Navigating -> {
                // Re-issue the navigate in case the prior cancel arrived first.
                navigateToStop(saved.index)
            }
            is TourState.Speaking -> {
                // Restart this stop's narration.
                speakAtStop(saved.index)
            }
            is TourState.Dwelling -> {
                // Restart dwell from full duration.
                startDwell(saved.index)
            }
            else -> _tourState.value = saved
        }
    }

    /** Skip forward: cancel current step and jump to the next stop's navigation. */
    fun skipStop() {
        val current = _tourState.value
        val index = when (current) {
            is TourState.Navigating -> current.index
            is TourState.Speaking -> current.index
            is TourState.Dwelling -> current.index
            is TourState.NavFailed -> current.index
            is TourState.Paused -> when (val s = current.saved) {
                is TourState.Navigating -> s.index
                is TourState.Speaking -> s.index
                is TourState.Dwelling -> s.index
                is TourState.NavFailed -> s.index
                else -> return
            }
            else -> return
        }
        cancelDwell()
        if (current is TourState.Speaking ||
            (current is TourState.Paused && current.saved is TourState.Speaking)) {
            sendTtsStop()
        }
        advanceFrom(index)
    }

    /** Stop the tour entirely — no further nav/TTS will be sent. */
    fun endTour() {
        cancelDwell()
        val current = _tourState.value
        if (current is TourState.Speaking ||
            (current is TourState.Paused && current.saved is TourState.Speaking)) {
            sendTtsStop()
        }
        _tourState.value = TourState.Idle
    }

    // --- State transitions ---

    private fun navigateToStop(index: Int) {
        if (index !in stops.indices) {
            _tourState.value = TourState.Complete
            return
        }
        _tourState.value = TourState.Navigating(index)
        sendNavigate(stops[index].poi, index)
        // Speak the en-route remark immediately while the robot starts driving;
        // landmark gets mentioned without making it its own stop.
        stops[index].enRouteRemark?.let { remark -> sendTts(remark) }
    }

    private fun speakAtStop(index: Int) {
        if (index !in stops.indices) return
        _tourState.value = TourState.Speaking(index)
        sendTts(stops[index].narration)
    }

    private fun startDwell(index: Int) {
        cancelDwell()
        _tourState.value = TourState.Dwelling(index)
        dwellJob = effectiveScope.launch {
            delay(OfficeTour.DWELL_MS)
            advanceFrom(index)
        }
    }

    private fun advanceFrom(index: Int) {
        cancelDwell()
        val next = index + 1
        if (next >= stops.size) {
            _tourState.value = TourState.Complete
        } else {
            navigateToStop(next)
        }
    }

    private fun cancelDwell() {
        dwellJob?.cancel()
        dwellJob = null
    }

    // --- Inbound events ---

    private fun handleBinaryFrame(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        if (bytes[0] != 0x06.toByte()) return
        val bitmap = bitmapDecoder(bytes, 1, bytes.size - 1) ?: return
        _mapBitmap.value = bitmap
    }

    private fun handleJsonMessage(type: String, payload: String) {
        when (type) {
            "nav_status" -> {
                try {
                    val json = JSONObject(payload)
                    val destination = json.optString("destination", "")
                    val status = json.optString("status", "")
                    val progress = json.optDouble("progress", 0.0).toFloat()
                    _navStatus.value = NavStatus(destination, status, progress)
                    _isNavigating.value = status == "navigating"

                    if (status == "arrived") {
                        onArrived(destination)
                    } else if (status == "failed") {
                        onNavFailed()
                    }
                } catch (_: Exception) { /* ignore */ }
            }
            "tts_control" -> {
                try {
                    val cmd = JSONObject(payload).optString("command", "")
                    if (cmd == "end") onTtsEnd()
                } catch (_: Exception) { /* ignore */ }
            }
        }
    }

    private fun onArrived(destination: String) {
        val state = _tourState.value
        if (state is TourState.Navigating && stops.getOrNull(state.index)?.poi == destination) {
            speakAtStop(state.index)
        }
    }

    private fun onNavFailed() {
        val state = _tourState.value
        if (state is TourState.Navigating) {
            // Don't auto-skip — that hides real problems (e-stop engaged, no path,
            // robot stuck). Surface it; the operator chooses to retry or skip.
            cancelDwell()
            _tourState.value = TourState.NavFailed(state.index)
        }
    }

    /** Retry the failed stop (e.g. after releasing the e-stop). */
    fun retryFailedStop() {
        val state = _tourState.value
        if (state is TourState.NavFailed) navigateToStop(state.index)
    }

    private fun onTtsEnd() {
        val state = _tourState.value
        if (state !is TourState.Speaking) return
        startDwell(state.index)
    }

    // --- Outbound messages ---

    private fun sendNavigate(poi: String, stopIndex: Int) {
        val json = JSONObject().apply {
            put("type", "lab_tour_navigate")
            put("poi", poi)
            put("stop_index", stopIndex)
        }.toString()
        wsRepo.send(json)
    }

    private fun sendTts(text: String) {
        val json = JSONObject().apply {
            put("type", "lab_tour_tts")
            put("text", text)
        }.toString()
        wsRepo.send(json)
    }

    private fun sendTtsStop() {
        val json = JSONObject().apply {
            put("type", "tts_control")
            put("command", "stop")
        }.toString()
        wsRepo.send(json)
    }
}
