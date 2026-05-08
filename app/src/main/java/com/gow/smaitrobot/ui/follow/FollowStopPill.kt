package com.gow.smaitrobot.ui.follow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gow.smaitrobot.data.websocket.WebSocketEvent
import com.gow.smaitrobot.data.websocket.WebSocketRepository
import org.json.JSONObject

/**
 * Floating "Stop following" pill that appears over the active screen whenever
 * the SERVER-side FollowController is in the ACTIVE state. Listens to
 * `follow_update` JSON messages on the WebSocket and animates in/out.
 *
 * Tapping the pill sends `{"type":"follow_stop"}` to the server, which
 * routes through ConnectionManager → FollowController.stop_following().
 *
 * Drop this composable on top of the NavHost (Box overlay) so it persists
 * across screen transitions while follow mode is engaged.
 */
@Composable
fun FollowStopPill(
    wsRepo: WebSocketRepository,
    modifier: Modifier = Modifier,
) {
    var isFollowing by remember { mutableStateOf(false) }

    LaunchedEffect(wsRepo) {
        wsRepo.events.collect { event ->
            when (event) {
                is WebSocketEvent.JsonMessage -> {
                    if (event.type == "follow_update") {
                        try {
                            val state = JSONObject(event.payload).optString("state", "IDLE")
                            isFollowing = (state == "ACTIVE")
                        } catch (_: Exception) {
                            // ignore malformed payload
                        }
                    }
                }
                is WebSocketEvent.Disconnected -> {
                    // Server gone → assume follow is no longer active
                    isFollowing = false
                }
                else -> Unit
            }
        }
    }

    AnimatedVisibility(
        visible = isFollowing,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 12.dp),
    ) {
        Button(
            onClick = {
                wsRepo.send(JSONObject().apply { put("type", "follow_stop") }.toString())
                // Optimistic — the server's next follow_update will confirm
                isFollowing = false
            },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 24.dp,
                vertical = 12.dp,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = null,
                tint = Color.White,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Stop following",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}
