package com.gow.eng192lab.ui.labtour

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gow.eng192lab.TtsAudioPlayer
import com.gow.eng192lab.data.websocket.WebSocketEvent
import com.gow.eng192lab.data.websocket.WebSocketRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "LabTour"
private const val AUTO_ADVANCE_DELAY_MS = 15_000L

data class TourStop(
    val name: String,
    val poi: String,
    val narration: String,
    val waitForTap: Boolean
)

data class TourData(
    val tourName: String,
    val introduction: String,
    val conclusion: String,
    val stops: List<TourStop>
)

sealed class TourState {
    object NotStarted : TourState()
    object Introduction : TourState()
    data class Navigating(val stopIndex: Int, val stopName: String) : TourState()
    data class Narrating(val stopIndex: Int, val stopName: String, val narration: String) : TourState()
    data class WaitingForNext(val stopIndex: Int, val stopName: String) : TourState()
    object Conclusion : TourState()
    object Finished : TourState()
}

class LabTourViewModel(
    private val wsRepo: WebSocketRepository,
    private val ttsPlayer: TtsAudioPlayer
) : ViewModel() {

    private val _tourState = MutableStateFlow<TourState>(TourState.NotStarted)
    val tourState: StateFlow<TourState> = _tourState.asStateFlow()

    private val _tourData = MutableStateFlow<TourData?>(null)
    val tourData: StateFlow<TourData?> = _tourData.asStateFlow()

    private val _currentStopIndex = MutableStateFlow(0)
    val currentStopIndex: StateFlow<Int> = _currentStopIndex.asStateFlow()

    private var autoAdvanceJob: Job? = null

    init {
        // Tour narration must not start until Jackie physically arrives at
        // the POI. We listen for `nav_status` from the server and gate the
        // Navigating → Narrating transition on `status == "arrived"`.
        // We also listen for `tts_control` so we can auto-advance the tour
        // once each narration finishes playing, with a 15s visitor buffer.
        viewModelScope.launch {
            wsRepo.events.collect { event ->
                if (event is WebSocketEvent.JsonMessage) {
                    when (event.type) {
                        "nav_status" -> handleNavStatus(event.payload)
                        "tts_control" -> handleTtsControl(event.payload)
                    }
                }
            }
        }
    }

    fun loadTour(tourData: TourData) {
        _tourData.value = tourData
        _tourState.value = TourState.NotStarted
        _currentStopIndex.value = 0
    }

    fun startTour() {
        val data = _tourData.value ?: return
        _tourState.value = TourState.Introduction
        sendTtsRequest(data.introduction)
        Log.i(TAG, "Tour started: ${data.tourName}")
    }

    fun proceedFromIntro() {
        navigateToStop(0)
    }

    fun nextStop() {
        cancelAutoAdvance()
        val data = _tourData.value ?: return
        val nextIndex = _currentStopIndex.value + 1
        if (nextIndex < data.stops.size) {
            navigateToStop(nextIndex)
        } else {
            _tourState.value = TourState.Conclusion
            sendTtsRequest(data.conclusion)
        }
    }

    fun finishTour() {
        cancelAutoAdvance()
        val data = _tourData.value
        val firstStop = data?.stops?.firstOrNull()
        if (firstStop != null) {
            val navCmd = JSONObject().apply {
                put("type", "lab_tour_navigate")
                put("poi", firstStop.poi)
                put("stop_index", 0)
            }
            wsRepo.send(navCmd.toString())
            Log.i(TAG, "Tour finished, returning to ${firstStop.name}")
        }
        _tourState.value = TourState.Finished
        _currentStopIndex.value = 0
    }

    fun resetTour() {
        cancelAutoAdvance()
        _tourState.value = TourState.NotStarted
        _currentStopIndex.value = 0
    }

    private fun navigateToStop(index: Int) {
        val data = _tourData.value ?: return
        val stop = data.stops[index]
        _currentStopIndex.value = index
        _tourState.value = TourState.Navigating(index, stop.name)

        val navCmd = JSONObject().apply {
            put("type", "lab_tour_navigate")
            put("poi", stop.poi)
            put("stop_index", index)
        }
        wsRepo.send(navCmd.toString())
        Log.i(TAG, "Navigating to stop $index: ${stop.name} (POI: ${stop.poi})")
        // Wait for server `nav_status` arrived — handled in handleNavStatus().
    }

    fun arriveAtStop(index: Int) {
        val data = _tourData.value ?: return
        val stop = data.stops[index]
        _tourState.value = TourState.Narrating(index, stop.name, stop.narration)
        sendTtsRequest(stop.narration)
    }

    fun narrationComplete() {
        cancelAutoAdvance()
        val data = _tourData.value ?: return
        val index = _currentStopIndex.value
        val stop = data.stops[index]
        if (stop.waitForTap) {
            _tourState.value = TourState.WaitingForNext(index, stop.name)
            scheduleAutoAdvance { nextStop() }
        } else {
            nextStop()
        }
    }

    private fun handleTtsControl(payload: String) {
        val action = try {
            JSONObject(payload).optString("action", "")
        } catch (_: Exception) {
            ""
        }
        if (action != "end") return
        when (_tourState.value) {
            is TourState.Introduction -> proceedFromIntro()
            is TourState.Narrating -> narrationComplete()
            is TourState.Conclusion -> scheduleAutoAdvance { finishTour() }
            else -> Unit
        }
    }

    private fun scheduleAutoAdvance(action: () -> Unit) {
        cancelAutoAdvance()
        autoAdvanceJob = viewModelScope.launch {
            delay(AUTO_ADVANCE_DELAY_MS)
            action()
        }
    }

    private fun cancelAutoAdvance() {
        autoAdvanceJob?.cancel()
        autoAdvanceJob = null
    }

    private fun handleNavStatus(payload: String) {
        val current = _tourState.value as? TourState.Navigating ?: return
        val status = try {
            JSONObject(payload).optString("status", "")
        } catch (_: Exception) {
            ""
        }
        when (status) {
            "arrived" -> {
                Log.i(TAG, "Arrived at stop ${current.stopIndex}: ${current.stopName}")
                arriveAtStop(current.stopIndex)
            }
            "failed" -> {
                // Don't leave the user stuck on "Navigating..." forever —
                // fall through to narration at the current location.
                Log.w(TAG, "Navigation failed for stop ${current.stopIndex}; playing narration anyway")
                arriveAtStop(current.stopIndex)
            }
            // "navigating" and any other intermediate status: keep waiting.
        }
    }

    private fun sendTtsRequest(text: String) {
        val msg = JSONObject().apply {
            put("type", "lab_tour_tts")
            put("text", text)
        }
        wsRepo.send(msg.toString())
    }
}
