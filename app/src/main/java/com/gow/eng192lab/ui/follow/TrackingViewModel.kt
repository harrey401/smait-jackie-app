package com.gow.eng192lab.ui.follow

import android.util.Log
import com.gow.eng192lab.data.websocket.WebSocketEvent
import com.gow.eng192lab.data.websocket.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "TrackingVM"

/** Poll interval for the `follow_status` request while Follow Me is running. */
private const val FOLLOW_POLL_INTERVAL_MS = 500L

/** How long without a FACE_UPDATED before we declare the face lost in the UI. */
private const val FACE_STALE_TIMEOUT_MS = 1_000L

/**
 * Which tracking skill is currently RUNNING on the server.
 * Mirrors the two start/stop buttons on the Tracking Tests page.
 */
enum class TrackingSkill { NONE, FACE_USER, FOLLOW }

/**
 * Snapshot of live tracking telemetry for the UI.
 *
 * Fields that the server does NOT yet push to the app are null. The UI shows
 * "—" or a "NO FACE" warning when a field is null or stale.
 *
 * ## What the server pushes today
 * - `follow_update` JSON (reply to a `follow_status` request) — contains
 *   `state`, `proximity_pct`, `face_visible`, `face_centered`, `face_to_face`.
 *
 * ## What the server does NOT push yet (TODOs — see FollowScreen header)
 * - `face_cx` (normalised 0..1 horizontal position)
 * - `face_w_norm` (face width as a fraction of frame width)
 * - Tracking-logic reload events (`face_user.py reloaded` / `reload failed`)
 * - Any telemetry at all while `face_user_start` is active — the
 *   `FaceUserController` currently emits no events that reach the connection
 *   manager, so the Face-the-User card can only show "RUNNING / IDLE".
 */
data class TrackingTelemetry(
    val followState: String? = null,
    val proximityPct: Int? = null,
    val faceVisible: Boolean? = null,
    val faceCentered: Boolean? = null,
    val faceToFace: Boolean? = null,
    // Live face position telemetry — server does NOT push these yet.
    val faceCx: Float? = null,
    val faceWNorm: Float? = null,
    val lastFaceUpdateMs: Long = 0L,
    // Last tracking-logic reload event — server does NOT push this yet.
    val lastReloadFile: String? = null,
    val lastReloadStatus: String? = null,
    val lastReloadAtMs: Long = 0L,
)

/**
 * Owns the Tracking Tests page state.
 *
 * Responsibilities:
 * - Sends start/stop messages for each skill over the WebSocket.
 * - Polls `follow_status` every 500ms while Follow Me is active.
 * - Parses incoming `follow_update` JSON frames into [TrackingTelemetry].
 * - Future: parses `face_user_update`, `tracking_reload` frames when the
 *   server gains those features (see TODOs at top of file).
 *
 * This is deliberately NOT an AndroidX `ViewModel` — the screen is entered
 * from a simple Compose navigation destination and the repo-owned scope is
 * enough. If we need lifecycle tie-in later, upgrade to `androidx.lifecycle.ViewModel`.
 */
class TrackingViewModel(
    private val wsRepo: WebSocketRepository,
    coroutineScope: CoroutineScope? = null,
) {
    private val scope: CoroutineScope = coroutineScope
        ?: CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _activeSkill = MutableStateFlow(TrackingSkill.NONE)
    val activeSkill: StateFlow<TrackingSkill> = _activeSkill.asStateFlow()

    private val _telemetry = MutableStateFlow(TrackingTelemetry())
    val telemetry: StateFlow<TrackingTelemetry> = _telemetry.asStateFlow()

    private var pollJob: Job? = null

    init {
        // Collect incoming WebSocket messages and surface the ones we care about.
        scope.launch {
            wsRepo.events.collect { event ->
                if (event is WebSocketEvent.JsonMessage) {
                    handleJsonMessage(event)
                }
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    fun startFaceUser() {
        if (_activeSkill.value == TrackingSkill.FOLLOW) stopFollow()
        wsRepo.send("""{"type":"face_user_start"}""")
        _activeSkill.value = TrackingSkill.FACE_USER
        _telemetry.value = TrackingTelemetry()
        // No poller yet — server does not expose face_user_status today.
    }

    fun stopFaceUser() {
        wsRepo.send("""{"type":"face_user_stop"}""")
        if (_activeSkill.value == TrackingSkill.FACE_USER) {
            _activeSkill.value = TrackingSkill.NONE
        }
    }

    fun startFollow() {
        if (_activeSkill.value == TrackingSkill.FACE_USER) stopFaceUser()
        wsRepo.send("""{"type":"follow_start"}""")
        _activeSkill.value = TrackingSkill.FOLLOW
        _telemetry.value = TrackingTelemetry()
        startFollowPoller()
    }

    fun stopFollow() {
        wsRepo.send("""{"type":"follow_stop"}""")
        pollJob?.cancel()
        pollJob = null
        if (_activeSkill.value == TrackingSkill.FOLLOW) {
            _activeSkill.value = TrackingSkill.NONE
        }
    }

    /**
     * Stop whichever skill (if any) is running. Call from DisposableEffect
     * when the screen is disposed so we never leave Jackie spinning.
     */
    fun stopAll() {
        when (_activeSkill.value) {
            TrackingSkill.FACE_USER -> stopFaceUser()
            TrackingSkill.FOLLOW -> stopFollow()
            TrackingSkill.NONE -> Unit
        }
    }

    /**
     * Called from the composable to drive UI staleness: if more than
     * [FACE_STALE_TIMEOUT_MS] has passed since the last telemetry update we
     * want the UI to flip to "NO FACE" without waiting for a new event.
     */
    fun isFaceStale(nowMs: Long): Boolean {
        val last = _telemetry.value.lastFaceUpdateMs
        if (last == 0L) return true
        return (nowMs - last) > FACE_STALE_TIMEOUT_MS
    }

    fun shutdown() {
        pollJob?.cancel()
        scope.cancel()
    }

    // ── WebSocket message handling ────────────────────────────────────────

    private fun handleJsonMessage(msg: WebSocketEvent.JsonMessage) {
        when (msg.type) {
            // Reply to our `follow_status` poll — see FollowController.get_status().
            "follow_update" -> parseFollowUpdate(msg.payload)

            // TODO(server): `face_user_update` — broadcast from FaceUserController
            // at ~10Hz while running, carrying `face_cx`, `face_w_norm`, `face_visible`.
            // When implemented, parse here.
            "face_user_update" -> parseFaceUserUpdate(msg.payload)

            // TODO(server): `tracking_reload` — emit from TrackingLogicLoader
            // when a reload happens (success OR SyntaxError failure), carrying
            // `file`, `ok`, `error`. When implemented, parse here.
            "tracking_reload" -> parseTrackingReload(msg.payload)

            else -> Unit
        }
    }

    private fun parseFollowUpdate(payload: String) {
        try {
            val json = JSONObject(payload)
            val now = System.currentTimeMillis()
            val cur = _telemetry.value

            // face_cx / face_w_norm are optional — server MAY start pushing
            // them in a future follow_update extension. We read them leniently.
            val faceCx = if (json.has("face_cx") && !json.isNull("face_cx")) {
                json.getDouble("face_cx").toFloat()
            } else cur.faceCx

            val faceWNorm = if (json.has("face_w_norm") && !json.isNull("face_w_norm")) {
                json.getDouble("face_w_norm").toFloat()
            } else cur.faceWNorm

            _telemetry.value = cur.copy(
                followState = json.optString("state", cur.followState ?: ""),
                proximityPct = if (json.has("proximity_pct")) json.optInt("proximity_pct", 0) else cur.proximityPct,
                faceVisible = if (json.has("face_visible")) json.optBoolean("face_visible", false) else cur.faceVisible,
                faceCentered = if (json.has("face_centered")) json.optBoolean("face_centered", false) else cur.faceCentered,
                faceToFace = if (json.has("face_to_face")) json.optBoolean("face_to_face", false) else cur.faceToFace,
                faceCx = faceCx,
                faceWNorm = faceWNorm,
                lastFaceUpdateMs = now,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse follow_update: ${e.message}")
        }
    }

    /**
     * Parses a `face_user_update` frame. **Not yet emitted by the server.**
     *
     * TODO(server): in `smait/navigation/face_user_controller.py`, once per
     * control-loop tick, build and broadcast via `ConnectionManager`:
     * ```python
     * {
     *   "type": "face_user_update",
     *   "state": "FACING" | "IDLE" | "NO_FACE",
     *   "face_cx": float | null,     # 0..1
     *   "face_w_norm": float | null, # 0..1 (face width / frame width)
     *   "face_visible": bool,
     * }
     * ```
     * Then wire a sender in `ConnectionManager` the same way `follow_status`
     * already routes `FollowController.get_status()` (manager.py:309).
     */
    private fun parseFaceUserUpdate(payload: String) {
        try {
            val json = JSONObject(payload)
            val now = System.currentTimeMillis()
            val cur = _telemetry.value

            val faceCx = if (json.has("face_cx") && !json.isNull("face_cx")) {
                json.getDouble("face_cx").toFloat()
            } else null
            val faceWNorm = if (json.has("face_w_norm") && !json.isNull("face_w_norm")) {
                json.getDouble("face_w_norm").toFloat()
            } else null

            _telemetry.value = cur.copy(
                faceVisible = if (json.has("face_visible")) json.optBoolean("face_visible", false) else cur.faceVisible,
                faceCx = faceCx,
                faceWNorm = faceWNorm,
                lastFaceUpdateMs = now,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse face_user_update: ${e.message}")
        }
    }

    /**
     * Parses a `tracking_reload` frame. **Not yet emitted by the server.**
     *
     * TODO(server): in `smait/navigation/tracking_loader.py`, whenever
     * `maybe_reload()` actually swaps or fails to swap the logic module,
     * emit an event that reaches `ConnectionManager` and gets sent as:
     * ```python
     * {
     *   "type": "tracking_reload",
     *   "file": "tracking/face_user.py",
     *   "ok": true,
     *   "ts": time.time(),
     * }
     * # or on failure:
     * {
     *   "type": "tracking_reload",
     *   "file": "tracking/follow_mode.py",
     *   "ok": false,
     *   "error": "SyntaxError: invalid syntax (line 42)",
     *   "ts": time.time(),
     * }
     * ```
     * This gives Jason an immediate on-robot signal when his GitHub edit
     * landed — success or failure — without tailing the server log.
     */
    private fun parseTrackingReload(payload: String) {
        try {
            val json = JSONObject(payload)
            val file = json.optString("file", "tracking/?.py")
            val ok = json.optBoolean("ok", true)
            val err = json.optString("error", "")
            val status = if (ok) "reloaded" else "RELOAD FAILED: $err"
            _telemetry.value = _telemetry.value.copy(
                lastReloadFile = file,
                lastReloadStatus = status,
                lastReloadAtMs = System.currentTimeMillis(),
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse tracking_reload: ${e.message}")
        }
    }

    // ── Follow-status poller ──────────────────────────────────────────────

    private fun startFollowPoller() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (_activeSkill.value == TrackingSkill.FOLLOW) {
                wsRepo.send("""{"type":"follow_status"}""")
                delay(FOLLOW_POLL_INTERVAL_MS)
            }
        }
    }
}
