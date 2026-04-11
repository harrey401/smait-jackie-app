package com.gow.eng192lab.ui.labtour

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val LabDark = Color(0xFF0D1642)
private val LabPrimary = Color(0xFF1A237E)
private val LabAccent = Color(0xFF42A5F5)

@Composable
fun LabTourScreen(
    viewModel: LabTourViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tourState by viewModel.tourState.collectAsStateWithLifecycle()
    val tourData by viewModel.tourData.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentStopIndex.collectAsStateWithLifecycle()

    // Load tour data from assets on first composition
    LaunchedEffect(Unit) {
        if (tourData == null) {
            val data = loadTourFromAssets(context)
            if (data != null) viewModel.loadTour(data)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LabDark, LabPrimary, LabDark)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.resetTour()
                    navController.popBackStack()
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.Explore, contentDescription = null, tint = LabAccent, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Lab Tour",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )

                // Progress indicator
                tourData?.let { data ->
                    if (tourState !is TourState.NotStarted && tourState !is TourState.Finished) {
                        Spacer(modifier = Modifier.width(24.dp))
                        LinearProgressIndicator(
                            progress = { (currentIndex + 1).toFloat() / data.stops.size },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = LabAccent,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${currentIndex + 1}/${data.stops.size}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 24.sp
                        )
                    }
                }
            }

            // Main content — animated state transitions
            AnimatedContent(
                targetState = tourState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "tour_state"
            ) { state ->
                when (state) {
                    is TourState.NotStarted -> StartScreen(
                        tourData = tourData,
                        onStart = { viewModel.startTour() }
                    )
                    is TourState.Introduction -> IntroScreen(
                        text = tourData?.introduction ?: "",
                        onProceed = { viewModel.proceedFromIntro() }
                    )
                    is TourState.Navigating -> NavigatingScreen(stopName = state.stopName)
                    is TourState.Narrating -> NarratingScreen(
                        stopName = state.stopName,
                        narration = state.narration,
                        onDone = { viewModel.narrationComplete() }
                    )
                    is TourState.WaitingForNext -> WaitingScreen(
                        stopName = state.stopName,
                        isLast = tourData?.let { state.stopIndex >= it.stops.size - 1 } ?: false,
                        onNext = { viewModel.nextStop() }
                    )
                    is TourState.Conclusion -> ConclusionScreen(
                        text = tourData?.conclusion ?: "",
                        onFinish = { viewModel.finishTour() }
                    )
                    is TourState.Finished -> FinishedScreen(
                        onRestart = { viewModel.resetTour() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun StartScreen(tourData: TourData?, onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Explore,
            contentDescription = null,
            tint = LabAccent,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = tourData?.tourName ?: "Lab Tour",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "${tourData?.stops?.size ?: 0} stops",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = LabAccent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(72.dp).width(320.dp)
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Start Tour", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IntroScreen(text: String, onProceed: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 32.sp,
            textAlign = TextAlign.Center,
            lineHeight = 42.sp
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onProceed,
            colors = ButtonDefaults.buttonColors(containerColor = LabAccent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(64.dp).width(280.dp)
        ) {
            Text("Let's Go!", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.NavigateNext, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun NavigatingScreen(stopName: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.LocationOn,
            contentDescription = null,
            tint = LabAccent,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Heading to",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 28.sp
        )
        Text(
            text = stopName,
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Follow me!",
            color = LabAccent,
            fontSize = 36.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NarratingScreen(stopName: String, narration: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stopName,
            color = LabAccent,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = narration,
            color = Color.White,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
            lineHeight = 38.sp,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = LabAccent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(56.dp).width(240.dp)
        ) {
            Text("Continue", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WaitingScreen(stopName: String, isLast: Boolean, onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF66BB6A),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stopName,
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = LabAccent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(64.dp).width(300.dp)
        ) {
            Text(
                if (isLast) "Finish Tour" else "Next Stop",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.NavigateNext, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun ConclusionScreen(text: String, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 30.sp,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(64.dp).width(280.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Done", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FinishedScreen(onRestart: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF66BB6A),
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Tour Complete!",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = LabAccent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text("Restart", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text("Back Home", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

private fun loadTourFromAssets(context: Context): TourData? {
    return try {
        val json = context.assets.open("lab_tour.json").bufferedReader().readText()
        val obj = org.json.JSONObject(json)
        val stops = mutableListOf<TourStop>()
        val stopsArray = obj.getJSONArray("stops")
        for (i in 0 until stopsArray.length()) {
            val s = stopsArray.getJSONObject(i)
            stops.add(TourStop(
                name = s.getString("name"),
                poi = s.getString("poi"),
                narration = s.getString("narration"),
                waitForTap = s.optBoolean("waitForTap", true)
            ))
        }
        TourData(
            tourName = obj.getString("tourName"),
            introduction = obj.getString("introduction"),
            conclusion = obj.getString("conclusion"),
            stops = stops
        )
    } catch (e: Exception) {
        Log.e("LabTour", "Failed to load tour data", e)
        null
    }
}
