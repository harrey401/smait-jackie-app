package com.gow.eng192lab.ui.follow

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gow.eng192lab.data.websocket.WebSocketRepository
import com.gow.eng192lab.ui.common.SubScreenTopBar
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LabDark = Color(0xFF0D1642)
private val LabPrimary = Color(0xFF1A237E)
private val LabAccent = Color(0xFF42A5F5)
private val CardBlue = Color(0xFF1565C0)
private val CardBlueDim = Color(0xFF0D47A1)
private val WarnRed = Color(0xFFE53935)
private val OkGreen = Color(0xFF43A047)
private val TrackBg = Color(0x33FFFFFF)
private val CenterMarker = Color(0xFFFFEB3B)

/**
 * Tracking-test screen — two skills, live telemetry cards, shared reload status row.
 *
 * Both behaviours run on the SMAIT server. The Python logic for each lives
 * in the `jackie-tracking` GitHub repo (`tracking/face_user.py` and
 * `tracking/follow_mode.py`) and is hot-reloaded on every push, so Jason
 * can iterate from the lab without restarting the server.
 *
 * ## What we show today
 * - FACE THE USER card: START/STOP only. No live telemetry — the server's
 *   `FaceUserController` does not emit any app-facing events yet.
 * - FOLLOW ME card: we poll `follow_status` every 500ms while active and
 *   display the returned `follow_update` fields (`state`, `proximity_pct`,
 *   `face_visible`, `face_centered`, `face_to_face`).
 *
 * ## What is TODO (server-side work required)
 * See `TrackingViewModel.parseFaceUserUpdate` and `parseTrackingReload`
 * for the exact JSON schema the server needs to start broadcasting:
 *
 * 1. **`face_user_update`** — broadcast from FaceUserController control loop
 *    with `face_cx`, `face_w_norm`, `face_visible`. Unlocks the face-position
 *    bar + face-size readout on the Face the User card.
 *
 * 2. **`follow_update` extension** — add `face_cx` and `face_w_norm` to
 *    `FollowController.get_status()` so the Follow Me card can show the same
 *    position bar during follow mode.
 *
 * 3. **`tracking_reload`** — emit from TrackingLogicLoader on every
 *    `maybe_reload()` swap (success OR SyntaxError) so the bottom status row
 *    can show "face_user.py reloaded at 12:34:56" in real time.
 */
@Composable
fun FollowScreen(
    wsRepo: WebSocketRepository,
    navController: NavHostController,
) {
    val vm = remember { TrackingViewModel(wsRepo) }

    DisposableEffect(Unit) {
        onDispose {
            vm.stopAll()
            vm.shutdown()
        }
    }

    val activeSkill by vm.activeSkill.collectAsState()
    val telemetry by vm.telemetry.collectAsState()

    // Drive a 4Hz clock so the "NO FACE" indicator flips without needing
    // a new WS event. Cheap — the composable re-renders only on value change.
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(250L)
        }
    }
    val faceStale = vm.isFaceStale(nowMs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LabDark, LabPrimary))),
    ) {
        SubScreenTopBar(title = "Tracking Tests", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Two skills, edited live from GitHub",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.7f),
            )

            // ── Face the User ─────────────────────────────────────────
            TestCard(
                title = "Face the User",
                description = "Jackie rotates to keep you centered. Rotation only — no driving. " +
                    "Edit logic in tracking/face_user.py.",
                icon = Icons.Default.Face,
                isActive = activeSkill == TrackingSkill.FACE_USER,
                onStart = { vm.startFaceUser() },
                onStop = { vm.stopFaceUser() },
            ) {
                if (activeSkill == TrackingSkill.FACE_USER) {
                    // NOTE: server does not push face_user_update yet, so
                    // faceCx / faceWNorm will be null. We still render the bar
                    // so Jason gets visual layout feedback, and flip to
                    // "NO FACE" via the staleness clock.
                    LiveTelemetryBody(
                        faceCx = telemetry.faceCx,
                        faceWNorm = telemetry.faceWNorm,
                        faceStale = faceStale,
                        stateLabel = null,
                        distanceLabel = null,
                    )
                } else {
                    IdleBody()
                }
            }

            // ── Follow Me ─────────────────────────────────────────────
            TestCard(
                title = "Follow Me",
                description = "Jackie drives toward you and faces you. " +
                    "Edit logic in tracking/follow_mode.py.",
                icon = Icons.Default.DirectionsWalk,
                isActive = activeSkill == TrackingSkill.FOLLOW,
                onStart = { vm.startFollow() },
                onStop = { vm.stopFollow() },
            ) {
                if (activeSkill == TrackingSkill.FOLLOW) {
                    LiveTelemetryBody(
                        faceCx = telemetry.faceCx,
                        faceWNorm = telemetry.faceWNorm,
                        faceStale = faceStale,
                        stateLabel = telemetry.followState,
                        distanceLabel = telemetry.proximityPct?.let { "Proximity: ${it}%" },
                    )
                } else {
                    IdleBody()
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Only one skill can run at a time. Starting one stops the other.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
            )

            Spacer(modifier = Modifier.height(2.dp))

            ReloadStatusRow(
                file = telemetry.lastReloadFile,
                status = telemetry.lastReloadStatus,
                atMs = telemetry.lastReloadAtMs,
            )
        }
    }
}

// ── Card ────────────────────────────────────────────────────────────────────

@Composable
private fun TestCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    body: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBlue),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (isActive) LabAccent else Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(modifier = Modifier.size(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = if (isActive) "RUNNING" else "Idle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) LabAccent else Color.White.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))

                Button(
                    onClick = { if (isActive) onStop() else onStart() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) WarnRed else OkGreen,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(width = 140.dp, height = 58.dp),
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isActive) "Stop" else "Start",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = if (isActive) "STOP" else "START",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBlueDim, shape = RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                body()
            }
        }
    }
}

// ── Card body variants ──────────────────────────────────────────────────────

@Composable
private fun IdleBody() {
    Text(
        text = "Idle",
        color = Color.White.copy(alpha = 0.55f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun LiveTelemetryBody(
    faceCx: Float?,
    faceWNorm: Float?,
    faceStale: Boolean,
    stateLabel: String?,
    distanceLabel: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Optional top row: follow state + distance/proximity
        if (stateLabel != null || distanceLabel != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stateLabel?.takeIf { it.isNotBlank() } ?: "—",
                    color = LabAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = distanceLabel ?: "",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Face position bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Face position",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
                Text(
                    text = when {
                        faceStale -> "NO FACE"
                        faceCx == null -> "—"
                        else -> "cx=${"%.2f".format(faceCx)}"
                    },
                    color = if (faceStale) WarnRed else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            FacePositionBar(
                cx = if (faceStale) null else faceCx,
            )
        }

        // Face size readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Face size",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
            Text(
                text = when {
                    faceStale || faceWNorm == null -> "—"
                    else -> "w_norm=${"%.3f".format(faceWNorm)}"
                },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

/**
 * Horizontal 0..1 bar with a fixed yellow marker at 0.5 (target) and a
 * moving white marker at [cx]. Null cx → empty bar (no marker).
 */
@Composable
private fun FacePositionBar(cx: Float?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .background(TrackBg, shape = RoundedCornerShape(11.dp)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width

            // Center target line
            drawLine(
                color = CenterMarker,
                start = androidx.compose.ui.geometry.Offset(w * 0.5f, 2f),
                end = androidx.compose.ui.geometry.Offset(w * 0.5f, h - 2f),
                strokeWidth = 3f,
            )

            // Current face marker
            if (cx != null) {
                val clamped = cx.coerceIn(0f, 1f)
                val x = w * clamped
                drawCircle(
                    color = Color.White,
                    radius = h * 0.38f,
                    center = androidx.compose.ui.geometry.Offset(x, h * 0.5f),
                )
                drawCircle(
                    color = LabAccent,
                    radius = h * 0.38f,
                    center = androidx.compose.ui.geometry.Offset(x, h * 0.5f),
                    style = Stroke(width = 2.5f),
                )
            }
        }
    }
}

// ── Bottom reload status row ────────────────────────────────────────────────

@Composable
private fun ReloadStatusRow(
    file: String?,
    status: String?,
    atMs: Long,
) {
    val tsFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    val text = when {
        file != null && status != null && atMs > 0L ->
            "${file.substringAfterLast('/')} $status at ${tsFmt.format(Date(atMs))}"
        else ->
            // TODO(server): see TrackingViewModel.parseTrackingReload for the
            // JSON schema needed to populate this row.
            "Waiting for tracking_reload events… (server-side TODO)"
    }

    val isError = status?.startsWith("RELOAD FAILED", ignoreCase = true) == true

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isError) WarnRed.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            color = if (isError) WarnRed else Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}
