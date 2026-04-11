package com.gow.eng192lab.ui.follow

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.foundation.shape.RoundedCornerShape as RC
import androidx.compose.ui.graphics.Brush
import com.gow.eng192lab.data.websocket.WebSocketRepository
import com.gow.eng192lab.ui.common.SubScreenTopBar

private val LabDark = Color(0xFF0D1642)
private val LabPrimary = Color(0xFF1A237E)
private val LabAccent = Color(0xFF42A5F5)
private val CardBlue = Color(0xFF1565C0)

/**
 * Tracking-test screen — two skills, two start/stop buttons.
 *
 * Both behaviours run on the SMAIT server. The Python logic for each lives
 * in the `jackie-tracking` GitHub repo (`tracking/face_user.py` and
 * `tracking/follow_mode.py`) and is hot-reloaded on every push, so the
 * collaborator can iterate from the lab without restarting the server.
 *
 * This screen does NOT do any on-device perception — it just sends
 * start/stop messages over the WebSocket and shows the current state.
 */
@Composable
fun FollowScreen(
    wsRepo: WebSocketRepository,
    navController: NavHostController
) {
    var faceUserActive by remember { mutableStateOf(false) }
    var followActive by remember { mutableStateOf(false) }

    // Make sure we don't leave Jackie spinning if the user backs out.
    DisposableEffect(Unit) {
        onDispose {
            if (faceUserActive) wsRepo.send("""{"type":"face_user_stop"}""")
            if (followActive)  wsRepo.send("""{"type":"follow_stop"}""")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(LabDark, LabPrimary))
            )
    ) {
        SubScreenTopBar(title = "Tracking Tests", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Two skills, edited live from GitHub",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f)
            )

            // ── Face the User ─────────────────────────────────────────
            TestCard(
                title = "Face the User",
                description = "Jackie rotates to keep you centered. Rotation only — no driving. " +
                              "Edit logic in tracking/face_user.py.",
                icon = Icons.Default.Face,
                isActive = faceUserActive,
                onStart = {
                    if (followActive) {
                        wsRepo.send("""{"type":"follow_stop"}""")
                        followActive = false
                    }
                    wsRepo.send("""{"type":"face_user_start"}""")
                    faceUserActive = true
                },
                onStop = {
                    wsRepo.send("""{"type":"face_user_stop"}""")
                    faceUserActive = false
                }
            )

            // ── Follow Me ─────────────────────────────────────────────
            TestCard(
                title = "Follow Me",
                description = "Jackie drives toward you and faces you. " +
                              "Edit logic in tracking/follow_mode.py.",
                icon = Icons.Default.DirectionsWalk,
                isActive = followActive,
                onStart = {
                    if (faceUserActive) {
                        wsRepo.send("""{"type":"face_user_stop"}""")
                        faceUserActive = false
                    }
                    wsRepo.send("""{"type":"follow_start"}""")
                    followActive = true
                },
                onStop = {
                    wsRepo.send("""{"type":"follow_stop"}""")
                    followActive = false
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Only one skill can run at a time. Starting one stops the other.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun TestCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardBlue
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (isActive) LabAccent else Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isActive) "RUNNING" else "Idle",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) LabAccent else Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            Button(
                onClick = { if (isActive) onStop() else onStart() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) Color(0xFFE53935) else Color(0xFF43A047)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(width = 160.dp, height = 64.dp)
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isActive) "Stop" else "Start",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (isActive) "STOP" else "START",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1
                )
            }
        }
    }
}
