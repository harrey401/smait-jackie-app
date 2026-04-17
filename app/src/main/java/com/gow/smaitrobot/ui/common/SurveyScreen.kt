package com.gow.smaitrobot.ui.common

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.gow.smaitrobot.data.model.SurveyData
import com.gow.smaitrobot.ui.conversation.SurveyBuilder
import kotlinx.coroutines.delay
import java.util.UUID

// High-contrast palette — deep purple text on near-white background
private val DeepPurple = Color(0xFF2D1B69)
private val LightPurple = Color(0xFF7B52A8)
private val PageBg = Color(0xFFFAF9FC)

/**
 * Post-interaction survey overlay — HFES 2026 trim.
 *
 * Three questions, big numbered 1-5 rating with high-contrast text.
 * Auto-dismisses after [SurveyBuilder.SURVEY_TIMEOUT_MS].
 *
 * @param onSubmit   Called with [SurveyData] on user submit.
 * @param onDismiss  Called when the survey auto-dismisses (timer expired).
 */
@Composable
fun SurveyScreen(
    onSubmit: (SurveyData) -> Unit,
    onDismiss: (SurveyData) -> Unit
) {
    val surveyStartTime = remember { System.currentTimeMillis() }
    val sessionId = remember { UUID.randomUUID().toString() }

    var starRating by remember { mutableIntStateOf(0) }
    var understood by remember { mutableIntStateOf(0) }
    var natural by remember { mutableIntStateOf(0) }

    var remainingSeconds by remember { mutableLongStateOf(60L) }

    LaunchedEffect(Unit) {
        val endTime = surveyStartTime + SurveyBuilder.SURVEY_TIMEOUT_MS
        while (true) {
            val now = System.currentTimeMillis()
            val remaining = (endTime - now) / 1000L
            remainingSeconds = remaining.coerceAtLeast(0)
            if (remaining <= 0) {
                onDismiss(
                    SurveyBuilder.buildDismissed(
                        starRating = starRating,
                        understood = understood,
                        helpful = 0,
                        natural = natural,
                        attentive = 0,
                        comment = "",
                        startTimeMs = surveyStartTime,
                        sessionId = sessionId
                    )
                )
                break
            }
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        // Countdown — top-right
        Text(
            text = "${remainingSeconds}s",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = DeepPurple,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 32.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "How was it?",
                fontSize = 96.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepPurple,
                textAlign = TextAlign.Center,
                lineHeight = 100.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            BigRatingQuestion(
                question = "Overall experience with Jackie",
                selected = starRating,
                onSelect = { starRating = it }
            )

            Spacer(modifier = Modifier.height(48.dp))

            BigRatingQuestion(
                question = "Did Jackie understand you?",
                selected = understood,
                onSelect = { understood = it }
            )

            Spacer(modifier = Modifier.height(48.dp))

            BigRatingQuestion(
                question = "Did the conversation feel natural?",
                selected = natural,
                onSelect = { natural = it }
            )

            Spacer(modifier = Modifier.height(56.dp))

            Button(
                onClick = {
                    onSubmit(
                        SurveyBuilder.buildCompleted(
                            starRating = starRating,
                            understood = understood,
                            helpful = 0,
                            natural = natural,
                            attentive = 0,
                            comment = "",
                            startTimeMs = surveyStartTime,
                            sessionId = sessionId
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .height(96.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Submit", fontSize = 44.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.TextButton(
                onClick = {
                    onDismiss(
                        SurveyBuilder.buildDismissed(
                            starRating = starRating,
                            understood = understood,
                            helpful = 0,
                            natural = natural,
                            attentive = 0,
                            comment = "",
                            startTimeMs = surveyStartTime,
                            sessionId = sessionId
                        )
                    )
                }
            ) {
                Text("Skip", fontSize = 28.sp, color = DeepPurple, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * One question + a row of 5 huge numbered buttons.
 * Solid high-contrast colors so there's no ambiguity about what's selected.
 */
@Composable
private fun BigRatingQuestion(
    question: String,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = question,
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            color = DeepPurple,
            textAlign = TextAlign.Center,
            lineHeight = 58.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1-5 scale labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 800.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Bad",
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepPurple
            )
            Text(
                text = "Great",
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepPurple
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 800.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (value in 1..5) {
                BigNumberButton(
                    value = value,
                    isSelected = value == selected,
                    onClick = { onSelect(value) }
                )
            }
        }
    }
}

/**
 * 96dp numbered circle. Filled deep purple when selected, white with thick border otherwise.
 * Minimum 96dp tap target — easy on a kiosk display.
 */
@Composable
private fun BigNumberButton(
    value: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) DeepPurple else Color.White
    val textColor = if (isSelected) Color.White else DeepPurple
    val borderColor = if (isSelected) DeepPurple else LightPurple

    Box(
        modifier = Modifier
            .size(112.dp)
            .clip(CircleShape)
            .background(bgColor, CircleShape)
            .border(4.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            fontSize = 60.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
    }
}
