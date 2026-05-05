package com.gow.eng192lab.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gow.eng192lab.data.model.NasaTlxData
import kotlinx.coroutines.delay
import java.util.UUID

// Palette
private val DeepPurple = Color(0xFF2D1B69)
private val LightPurple = Color(0xFF7B52A8)
private val PageBg = Color(0xFFFAF9FC)
private val CellEmpty = Color(0xFFE7E2F0)
private val CellBorder = Color(0xFFB7A8D9)

// 21 stops, 0..100 in 5-point increments (Hart & Staveland 1988 raw TLX).
private const val STEP = 5
private const val NUM_STOPS = 21
private const val UNRATED = -1
private const val TIMEOUT_MS = 60_000L

private data class TlxItem(
    val key: String,
    val label: String,
    val description: String,
    val lowAnchor: String,
    val highAnchor: String,
)

/**
 * HRI-contextualized prompts, anchors per Hart & Staveland (1988) raw TLX.
 * The label + endpoints are verbatim NASA-TLX; only the description is
 * scoped to "talking with Jackie" so the participant knows what task to rate.
 */
private val ITEMS = listOf(
    TlxItem(
        key = "mental",
        label = "Mental Demand",
        description = "How much thinking, deciding, or remembering did talking with Jackie require?",
        lowAnchor = "Very Low",
        highAnchor = "Very High",
    ),
    TlxItem(
        key = "physical",
        label = "Physical Demand",
        description = "How much physical activity (speaking, leaning in, repeating yourself) was required?",
        lowAnchor = "Very Low",
        highAnchor = "Very High",
    ),
    TlxItem(
        key = "temporal",
        label = "Temporal Demand",
        description = "How hurried or rushed did the back-and-forth with Jackie feel?",
        lowAnchor = "Very Low",
        highAnchor = "Very High",
    ),
    TlxItem(
        key = "performance",
        label = "Performance",
        description = "How successful do you think you were in getting Jackie to understand and help you?",
        // NOTE: anchor reversed by NASA-TLX convention.
        lowAnchor = "Perfect",
        highAnchor = "Failure",
    ),
    TlxItem(
        key = "effort",
        label = "Effort",
        description = "How hard did you have to work to communicate with Jackie?",
        lowAnchor = "Very Low",
        highAnchor = "Very High",
    ),
    TlxItem(
        key = "frustration",
        label = "Frustration",
        description = "How insecure, discouraged, irritated, or annoyed did you feel during the conversation?",
        lowAnchor = "Very Low",
        highAnchor = "Very High",
    ),
)

/**
 * Post-session NASA-TLX raw rating overlay (Hart & Staveland 1988).
 *
 * Six 21-point tap bars (0..100 in 5-pt increments). No default selection so
 * we don't bias responses toward 50; Submit stays disabled until all six are
 * rated. Auto-dismisses after [TIMEOUT_MS] and submits whatever's set; any
 * un-rated dimension is sent as -1 so analysis can flag and drop it.
 */
@Composable
fun NasaTlxScreen(
    onSubmit: (NasaTlxData) -> Unit,
    onDismiss: (NasaTlxData) -> Unit,
) {
    val startTime = remember { System.currentTimeMillis() }
    val sessionId = remember { UUID.randomUUID().toString() }
    val values = remember { mutableStateMapOf<String, Int>() }
    val ratedCount by remember { derivedStateOf { values.size } }
    val allRated by remember { derivedStateOf { values.size == ITEMS.size } }
    var remainingSeconds by remember { mutableLongStateOf(TIMEOUT_MS / 1000L) }

    LaunchedEffect(Unit) {
        val endTime = startTime + TIMEOUT_MS
        while (true) {
            val now = System.currentTimeMillis()
            val remaining = (endTime - now) / 1000L
            remainingSeconds = remaining.coerceAtLeast(0)
            if (remaining <= 0) {
                onDismiss(buildData(values, false, startTime, sessionId))
                break
            }
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg),
    ) {
        // Top bar: progress + countdown
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "$ratedCount of ${ITEMS.size} rated",
                fontSize = 22.sp,
                color = if (allRated) DeepPurple else LightPurple,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${remainingSeconds}s",
                fontSize = 22.sp,
                color = LightPurple,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp)
                .padding(top = 60.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Workload",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurple,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tap one box per row that best describes the conversation you just had.",
                    fontSize = 20.sp,
                    color = LightPurple,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(18.dp))

                ITEMS.forEach { item ->
                    TlxRow(
                        item = item,
                        value = values[item.key] ?: UNRATED,
                        onChange = { values[item.key] = it },
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // Action bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = { onSubmit(buildData(values, true, startTime, sessionId)) },
                    enabled = allRated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .height(76.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepPurple,
                        contentColor = Color.White,
                        disabledContainerColor = LightPurple.copy(alpha = 0.35f),
                        disabledContentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        if (allRated) "Submit" else "Submit ($ratedCount/${ITEMS.size})",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = { onDismiss(buildData(values, false, startTime, sessionId)) },
                ) {
                    Text(
                        "Skip remaining",
                        fontSize = 18.sp,
                        color = LightPurple,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TlxRow(
    item: TlxItem,
    value: Int,
    onChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().widthIn(max = 1100.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.label,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DeepPurple,
            )
            Text(
                text = if (value == UNRATED) "—" else "$value",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (value == UNRATED) LightPurple else DeepPurple,
            )
        }
        Text(
            text = item.description,
            fontSize = 17.sp,
            color = DeepPurple.copy(alpha = 0.75f),
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 21-cell tap bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (i in 0 until NUM_STOPS) {
                val cellValue = i * STEP
                val selected = cellValue == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) DeepPurple else CellEmpty)
                        .border(
                            width = if (selected) 0.dp else 1.dp,
                            color = CellBorder,
                            shape = RoundedCornerShape(6.dp),
                        )
                        .clickable { onChange(cellValue) },
                    contentAlignment = Alignment.Center,
                ) {
                    // Show numeric label only on the major ticks (every 25, i.e. i = 0,5,10,15,20).
                    if (i % 5 == 0) {
                        Text(
                            text = "$cellValue",
                            fontSize = 13.sp,
                            color = if (selected) Color.White else DeepPurple,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(item.lowAnchor, fontSize = 18.sp, color = DeepPurple, fontWeight = FontWeight.SemiBold)
            Text(item.highAnchor, fontSize = 18.sp, color = DeepPurple, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun buildData(
    values: Map<String, Int>,
    completed: Boolean,
    startTimeMs: Long,
    sessionId: String,
): NasaTlxData = NasaTlxData(
    mental = values["mental"] ?: UNRATED,
    physical = values["physical"] ?: UNRATED,
    temporal = values["temporal"] ?: UNRATED,
    performance = values["performance"] ?: UNRATED,
    effort = values["effort"] ?: UNRATED,
    frustration = values["frustration"] ?: UNRATED,
    completedInTime = completed,
    timeToCompleteMs = System.currentTimeMillis() - startTimeMs,
    sessionId = sessionId,
)
