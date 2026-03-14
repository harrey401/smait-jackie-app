package com.gow.smaitrobot.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gow.smaitrobot.data.model.FeedbackData
import com.gow.smaitrobot.ui.conversation.FeedbackBuilder
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Post-session feedback dialog.
 *
 * Step 1: Star rating (1-5) with optional "answer 2-3 quick questions" path.
 * Step 2 (optional): Short survey — naturalness (1-5), helpfulness (yes/no), suggestions (text).
 *
 * Auto-dismisses after ~10 seconds if no interaction (via [LaunchedEffect] delay).
 *
 * @param onSubmit   Called with [FeedbackData] when the user taps Submit.
 * @param onDismiss  Called when the dialog is dismissed (skip, timeout, or back).
 */
@Composable
fun FeedbackDialog(
    onSubmit: (FeedbackData) -> Unit,
    onDismiss: () -> Unit
) {
    val dialogStartTime = remember { System.currentTimeMillis() }
    var selectedRating by remember { mutableIntStateOf(0) }
    var showSurvey by remember { mutableStateOf(false) }
    var naturalnessRating by remember { mutableIntStateOf(0) }
    var helpfulAnswer by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf("") }
    val sessionId = remember { UUID.randomUUID().toString() }

    // Auto-dismiss after 10 seconds if no interaction
    LaunchedEffect(Unit) {
        delay(10_000L)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!showSurvey) {
                    // Step 1: Star Rating
                    Text(
                        text = "How was your experience?",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 5-star rating row
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (star in 1..5) {
                            IconButton(
                                onClick = { selectedRating = star },
                                modifier = Modifier.size(60.dp)
                            ) {
                                Icon(
                                    imageVector = if (star <= selectedRating) Icons.Filled.Star
                                                  else Icons.Outlined.StarOutline,
                                    contentDescription = "$star star",
                                    tint = if (star <= selectedRating) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    },
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Optional: take the survey
                    OutlinedButton(
                        onClick = { showSurvey = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Answer 2-3 quick questions", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Submit with just the rating
                    Button(
                        onClick = {
                            if (FeedbackBuilder.isValidRating(selectedRating)) {
                                onSubmit(FeedbackBuilder.build(
                                    rating = selectedRating,
                                    sessionId = sessionId
                                ))
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Submit", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Skip", fontSize = 16.sp)
                    }

                } else {
                    // Step 2: Survey questions
                    Text(
                        text = "Quick questions",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Q1: Naturalness (1-5 scale)
                    Text(
                        "How natural did the conversation feel?",
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (n in 1..5) {
                            IconButton(
                                onClick = { naturalnessRating = n },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    imageVector = if (n <= naturalnessRating) Icons.Filled.Star
                                                  else Icons.Outlined.StarOutline,
                                    contentDescription = "$n",
                                    tint = if (n <= naturalnessRating) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    },
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Q2: Helpfulness (Yes/No)
                    Text(
                        "Was Jackie helpful in answering your questions?",
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { helpfulAnswer = "yes" },
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Text(
                                "Yes",
                                fontSize = 16.sp,
                                color = if (helpfulAnswer == "yes") MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        OutlinedButton(
                            onClick = { helpfulAnswer = "no" },
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Text(
                                "No",
                                fontSize = 16.sp,
                                color = if (helpfulAnswer == "no") MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Q3: Suggestions (optional free text)
                    Text(
                        "Any suggestions for improvement? (optional)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = suggestions,
                        onValueChange = { suggestions = it },
                        placeholder = { Text("Your feedback here...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val surveyResponses = buildMap {
                                if (naturalnessRating > 0) put("naturalness", naturalnessRating.toString())
                                if (helpfulAnswer.isNotEmpty()) put("helpful", helpfulAnswer)
                                if (suggestions.isNotEmpty()) put("suggestions", suggestions)
                            }
                            onSubmit(FeedbackBuilder.build(
                                rating = selectedRating.takeIf { it > 0 } ?: 3,
                                sessionId = sessionId,
                                surveyResponses = surveyResponses
                            ))
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Submit Feedback", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Skip", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
