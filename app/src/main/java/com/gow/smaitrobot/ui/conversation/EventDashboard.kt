package com.gow.smaitrobot.ui.conversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gow.smaitrobot.data.model.RobotState
import com.gow.smaitrobot.data.model.ScheduleItem
import com.gow.smaitrobot.data.model.SponsorConfig
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val DeepPurple = Color(0xFF2D1B69)
private val LightPurple = Color(0xFF7B52A8)
private val Accent = Color(0xFF8BC53F)
private val PanelBg = Color(0xFFF0EAFA)  // light purple tint
private val SubtleText = Color(0xFF4A3278)

private const val SPONSOR_ROTATE_MS = 4500L
private const val CLOCK_TICK_MS = 1000L

/**
 * Live event dashboard panel — replaces the giant idle orb on the conversation screen.
 *
 * Shows (top → bottom):
 *  - Live clock
 *  - "Now happening" — current schedule item (resolved by comparing `Date` to schedule times)
 *  - "Up next" — next upcoming item with countdown
 *  - Small Lottie mascot in a card (Jackie tile)
 *  - Rotating sponsor spotlight at the bottom
 *
 * Schedule times are parsed loosely from common formats ("9:00 AM", "10:15 AM").
 * If parsing fails the entry is skipped — never crashes the panel.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EventDashboard(
    schedule: List<ScheduleItem>,
    sponsors: List<SponsorConfig>,
    robotState: RobotState,
    onMascotLongPress: () -> Unit = {},
    onMascotDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(CLOCK_TICK_MS)
        }
    }

    val parsed = remember(schedule, nowMs / 60_000L) {
        schedule.mapNotNull { item ->
            parseTimeOnToday(item.time)?.let { it to item }
        }.sortedBy { it.first }
    }

    val nowHappening: Pair<Long, ScheduleItem>? = parsed.lastOrNull { it.first <= nowMs }
    val upNext: Pair<Long, ScheduleItem>? = parsed.firstOrNull { it.first > nowMs }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ClockTile(nowMs = nowMs)
        NowHappeningTile(nowHappening?.second)
        UpNextTile(upNext?.second, nowMs = nowMs, scheduledMs = upNext?.first)
        Box(modifier = Modifier.weight(1f)) {
            MascotTile(
                robotState = robotState,
                onLongPress = onMascotLongPress,
                onDoubleTap = onMascotDoubleTap
            )
        }
        SponsorSpotlight(sponsors = sponsors)
    }
}

@Composable
private fun ClockTile(nowMs: Long) {
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.US) }
    val dateFmt = remember { SimpleDateFormat("EEEE, MMM d", Locale.US) }
    val date = Date(nowMs)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeFmt.format(date),
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepPurple
            )
            Text(
                text = dateFmt.format(date),
                fontSize = 16.sp,
                color = SubtleText
            )
        }
    }
}

@Composable
private fun NowHappeningTile(item: ScheduleItem?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurple),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "NOW",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Accent,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item?.title ?: "Conference in session",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 26.sp,
                maxLines = 2
            )
            if (item != null && item.location.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.location,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun UpNextTile(item: ScheduleItem?, nowMs: Long, scheduledMs: Long?) {
    if (item == null || scheduledMs == null) return
    val countdown = formatCountdown(scheduledMs - nowMs)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "UP NEXT • ${item.time}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightPurple,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepPurple,
                    lineHeight = 22.sp,
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = countdown,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Accent
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MascotTile(
    robotState: RobotState,
    onLongPress: () -> Unit = {},
    onDoubleTap: () -> Unit = {}
) {
    val (stateColor, stateLabel) = when (robotState) {
        RobotState.IDLE -> Accent to "Ready"
        RobotState.LISTENING -> Color(0xFF8BC53F) to "Listening..."
        RobotState.THINKING -> Color(0xFFFBBF24) to "Thinking..."
        RobotState.SPEAKING -> LightPurple to "Speaking..."
    }
    Card(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = { },
                onLongClick = onLongPress,
                onDoubleClick = onDoubleTap
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurple),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Robot icon in a colored circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(stateColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = "Jackie",
                        tint = stateColor,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Jackie",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stateLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = stateColor
                )
            }
        }
    }
}

@Composable
private fun SponsorSpotlight(sponsors: List<SponsorConfig>) {
    if (sponsors.isEmpty()) return
    var idx by remember { mutableIntStateOf(0) }
    LaunchedEffect(sponsors) {
        if (sponsors.size <= 1) return@LaunchedEffect
        while (true) {
            delay(SPONSOR_ROTATE_MS)
            idx = (idx + 1) % sponsors.size
        }
    }
    val sponsor = sponsors[idx.coerceAtMost(sponsors.size - 1)]
    val context = LocalContext.current
    val resName = sponsor.logoAsset.substringBeforeLast(".")
    val resId = if (resName.isNotEmpty()) {
        context.resources.getIdentifier(resName, "drawable", context.packageName)
    } else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SPONSOR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = LightPurple,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(6.dp))
            AnimatedContent(
                targetState = sponsor,
                transitionSpec = {
                    fadeIn(tween(500)) togetherWith fadeOut(tween(500))
                },
                label = "sponsor_rotate"
            ) { current ->
                if (resId != 0) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = current.name,
                        modifier = Modifier
                            .height(56.dp)
                            .widthIn(max = 200.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = current.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurple,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Compact status orb — tiny indicator in the bottom-right of the chat area.
 * Single small colored dot + state label.
 */
@Composable
fun StatusOrb(robotState: RobotState, modifier: Modifier = Modifier) {
    val (color, label) = when (robotState) {
        RobotState.IDLE -> Color(0xFF9CA3AF) to "Idle"
        RobotState.LISTENING -> Color(0xFF8BC53F) to "Listening"
        RobotState.THINKING -> Color(0xFFFBBF24) to "Thinking"
        RobotState.SPEAKING -> Color(0xFF7B52A8) to "Speaking"
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(PanelBg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepPurple
        )
    }
}

// ── Time helpers ────────────────────────────────────────────────────────────

/**
 * Parses a schedule time string ("9:00 AM", "10:15 AM", "3:00 PM") into today's epoch ms.
 * Returns null if the format isn't recognised — caller skips the entry.
 */
private fun parseTimeOnToday(timeStr: String): Long? {
    val trimmed = timeStr.trim()
    if (trimmed.isEmpty()) return null
    return try {
        val fmt = SimpleDateFormat("h:mm a", Locale.US)
        val parsed = fmt.parse(trimmed) ?: return null
        val parsedCal = Calendar.getInstance().apply { time = parsed }
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    } catch (_: Exception) {
        null
    }
}

/**
 * Formats a duration in ms as "in 12m" / "in 1h 5m".
 */
private fun formatCountdown(deltaMs: Long): String {
    val totalMin = abs(deltaMs / 60_000L)
    if (totalMin < 1L) return "soon"
    val h = totalMin / 60L
    val m = totalMin % 60L
    return when {
        h <= 0L -> "in ${m}m"
        m == 0L -> "in ${h}h"
        else -> "in ${h}h ${m}m"
    }
}
