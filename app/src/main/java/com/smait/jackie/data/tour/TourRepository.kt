package com.smait.jackie.data.tour

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.smait.jackie.data.model.TourManifest
import com.smait.jackie.data.websocket.WebSocketEvent
import com.smait.jackie.data.websocket.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "TourRepository"
private const val CACHE_FILE = "tour_manifest.json"

/**
 * Receives the office-tour manifest from SMAIT and caches it locally.
 *
 * Wire flow:
 *   1. SMAIT sends `{"type":"tour_manifest","manifest":{...}}` on connect and
 *      whenever the operator saves new tour content from the cockpit.
 *   2. This repository deserializes it into [TourManifest] and exposes the
 *      latest value via [manifest] (StateFlow).
 *   3. The manifest is also written to `<filesDir>/tour_manifest.json` so the
 *      tablet survives an offline boot — [start] hydrates the StateFlow from
 *      that cache before the WebSocket comes up.
 *
 * Null in [manifest] means "no tour received yet AND no cache" — callers
 * should fall back to the built-in default (OfficeTour.stops).
 */
class TourRepository(
    private val context: Context,
    private val wsRepo: WebSocketRepository,
    private val gson: Gson = Gson(),
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _manifest = MutableStateFlow<TourManifest?>(null)
    val manifest: StateFlow<TourManifest?> = _manifest.asStateFlow()

    private var collectJob: Job? = null

    /** Hydrate from disk cache and start collecting WS messages. Idempotent. */
    fun start() {
        if (collectJob != null) return
        scope.launch { hydrateFromCache() }
        collectJob = scope.launch {
            wsRepo.events
                .filterIsInstance<WebSocketEvent.JsonMessage>()
                .collect { event ->
                    if (event.type == "tour_manifest") {
                        handleManifestMessage(event.payload)
                    }
                }
        }
    }

    fun shutdown() {
        collectJob?.cancel()
        collectJob = null
    }

    private fun handleManifestMessage(payload: String) {
        val parsed = parseManifest(payload) ?: return
        _manifest.value = parsed
        scope.launch { saveToCache(parsed) }
        Log.i(TAG, "Tour manifest updated: ${parsed.stops.size} stops")
    }

    private fun parseManifest(payload: String): TourManifest? {
        return try {
            val root = JsonParser.parseString(payload) as? JsonObject ?: return null
            val inner = root.getAsJsonObject("manifest") ?: return null
            gson.fromJson(inner, TourManifest::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse tour_manifest: ${e.message}")
            null
        }
    }

    private suspend fun hydrateFromCache() {
        withContext(Dispatchers.IO) {
            val file = cacheFile()
            if (!file.exists()) return@withContext
            try {
                val text = file.readText()
                val cached = gson.fromJson(text, TourManifest::class.java)
                if (cached != null && _manifest.value == null) {
                    _manifest.value = cached
                    Log.i(TAG, "Loaded cached tour (${cached.stops.size} stops)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read cached tour: ${e.message}")
            }
        }
    }

    private suspend fun saveToCache(manifest: TourManifest) {
        withContext(Dispatchers.IO) {
            try {
                cacheFile().writeText(gson.toJson(manifest))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cache tour: ${e.message}")
            }
        }
    }

    private fun cacheFile(): File = File(context.filesDir, CACHE_FILE)
}
