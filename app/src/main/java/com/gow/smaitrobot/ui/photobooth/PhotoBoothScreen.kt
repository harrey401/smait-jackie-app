package com.gow.smaitrobot.ui.photobooth

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.foundation.border
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gow.smaitrobot.data.websocket.WebSocketRepository
import com.gow.smaitrobot.ui.conversation.SelfieCapture
import com.gow.smaitrobot.data.websocket.WebSocketEvent
import kotlinx.coroutines.flow.filterIsInstance
import org.json.JSONObject
import java.io.ByteArrayOutputStream

private const val TAG = "PhotoBooth"

// Theme colors
private val DarkBg = Color(0xFF121212)
private val CardBg = Color(0xFF1E1E2E)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentCyan = Color(0xFF06B6D4)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val SelectedBorder = Color(0xFF8B5CF6)

/**
 * Style metadata for the picker grid.
 * Keys must match STYLE_REGISTRY on the server.
 * Each style has a gradient color pair and icon for visual identity.
 */
data class StyleOption(
    val key: String,
    val name: String,
    val gradientStart: Color,
    val gradientEnd: Color
)

private val STYLES = listOf(
    StyleOption("ghibli", "Ghibli",
        Color(0xFF4ADE80), Color(0xFF059669)),         // green meadow
    StyleOption("pixar", "Pixar 3D",
        Color(0xFF67E8F9), Color(0xFF8B5CF6)),         // cyan-purple
    StyleOption("cyberpunk", "Cyberpunk",
        Color(0xFFE879F9), Color(0xFF6D28D9)),         // neon magenta-purple
    StyleOption("claymation", "Claymation",
        Color(0xFFFDA4AF), Color(0xFFF59E0B)),         // clay pink-amber
)

// Sentinel style key — server returns the raw camera JPEG with no styling.
private const val STYLE_NORMAL = "normal"

/**
 * Photo booth states.
 */
private sealed class BoothState {
    /** Style selection — two tabs: Themes and Custom */
    object Picking : BoothState()
    /** Camera capture with countdown */
    data class Capturing(val style: String, val mode: String, val customPrompt: String = "") : BoothState()
    /** Waiting for server to process */
    data class Processing(val style: String) : BoothState()
    /** Result ready with styled image + QR */
    data class Result(
        val styledBitmap: Bitmap,
        val qrBitmap: Bitmap? = null,
        val downloadUrl: String = ""
    ) : BoothState()
    /** Error from server */
    data class Error(val message: String) : BoothState()
}

/**
 * Full photo booth screen with style picker, camera, processing, and result display.
 *
 * Two modes:
 * - **Themes**: 10 preset styles in a grid
 * - **Custom**: User types a description of what they want
 */
@Composable
fun PhotoBoothScreen(navController: NavHostController, wsRepo: WebSocketRepository) {
    var state: BoothState by remember { mutableStateOf(BoothState.Picking) }
    var selectedStyle by remember { mutableStateOf("ghibli") }

    // Listen for server responses
    LaunchedEffect(Unit) {
        wsRepo.events.filterIsInstance<WebSocketEvent.JsonMessage>().collect { event ->
            try {
                val json = JSONObject(event.payload)
                when (json.optString("type")) {
                    "styled_result" -> {
                        val b64 = json.getString("styled_b64")
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bmp != null) {
                            state = BoothState.Result(styledBitmap = bmp)
                        }
                    }
                    "qr_code" -> {
                        val currentState = state
                        if (currentState is BoothState.Result) {
                            val qrB64 = json.getString("qr_b64")
                            val qrBytes = Base64.decode(qrB64, Base64.DEFAULT)
                            val qrBmp = BitmapFactory.decodeByteArray(qrBytes, 0, qrBytes.size)
                            state = currentState.copy(
                                qrBitmap = qrBmp,
                                downloadUrl = json.optString("download_url", "")
                            )
                        }
                    }
                    "photo_booth_error" -> {
                        val error = json.optString("error", "Unknown error")
                        state = BoothState.Error(error)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse server message", e)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(200))
            },
            label = "booth_state"
        ) { currentState ->
            when (currentState) {
                is BoothState.Picking -> {
                    PickerScreen(
                        selectedTab = 0,
                        onTabChange = {},
                        selectedStyle = selectedStyle,
                        onStyleSelect = { style ->
                            selectedStyle = style
                            val json = JSONObject().apply {
                                put("type", "photo_booth_style")
                                put("style", style)
                                put("mode", "portrait")
                            }
                            wsRepo.send(json.toString())
                            state = BoothState.Capturing(style, "portrait")
                        },
                        customPrompt = "",
                        onCustomPromptChange = {},
                        onNext = {},
                        onNormalCamera = {
                            // Skip styling: send "normal" and go straight to capture.
                            val json = JSONObject().apply {
                                put("type", "photo_booth_style")
                                put("style", STYLE_NORMAL)
                                put("mode", "portrait")
                            }
                            wsRepo.send(json.toString())
                            state = BoothState.Capturing(STYLE_NORMAL, "portrait", "")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                is BoothState.Capturing -> {
                    SelfieCapture(
                        onDismiss = { state = BoothState.Picking },
                        onCapture = { bitmap ->
                            sendPhotoToServer(bitmap, wsRepo)
                            state = BoothState.Processing(currentState.style)
                        }
                    )
                }
                is BoothState.Processing -> {
                    ProcessingScreen(styleName = currentState.style)
                }
                is BoothState.Result -> {
                    ResultScreen(
                        styledBitmap = currentState.styledBitmap,
                        qrBitmap = currentState.qrBitmap,
                        onRetake = { state = BoothState.Picking },
                        onBack = { navController.popBackStack() }
                    )
                }
                is BoothState.Error -> {
                    ErrorScreen(
                        message = currentState.message,
                        onRetry = { state = BoothState.Picking }
                    )
                }
            }
        }
    }
}

// ── Picker Screen ────────────────────────────────────────────────────────────
//
// Matches the Home screen 2x2 card layout: 4 style cards fill the center,
// with a small "Normal Camera" link below. Tapping a style goes directly to
// capture — no separate "Take Photo" step needed.

// Card colors matching HomeScreen diagonal pattern
private val StylePrimary = Color(0xFF2D1B69).copy(alpha = 0.85f)
private val StyleSecondary = Color(0xFF4A3278).copy(alpha = 0.85f)

@Composable
private fun PickerScreen(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    selectedStyle: String,
    onStyleSelect: (String) -> Unit,
    customPrompt: String,
    onCustomPromptChange: (String) -> Unit,
    onNext: () -> Unit,
    onNormalCamera: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "Choose Your Style",
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2x2 grid — same layout as HomeScreen cards
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(0.85f)
                .align(Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        ) {
            // Top row
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                STYLES.getOrNull(0)?.let { style ->
                    BoothStyleCard(
                        style = style,
                        cardIndex = 0,
                        onClick = {
                            onStyleSelect(style.key)
                            onNext()
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                STYLES.getOrNull(1)?.let { style ->
                    BoothStyleCard(
                        style = style,
                        cardIndex = 1,
                        onClick = {
                            onStyleSelect(style.key)
                            onNext()
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
            // Bottom row
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                STYLES.getOrNull(2)?.let { style ->
                    BoothStyleCard(
                        style = style,
                        cardIndex = 2,
                        onClick = {
                            onStyleSelect(style.key)
                            onNext()
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                STYLES.getOrNull(3)?.let { style ->
                    BoothStyleCard(
                        style = style,
                        cardIndex = 3,
                        onClick = {
                            onStyleSelect(style.key)
                            onNext()
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Normal Camera — small secondary link at the bottom
        androidx.compose.material3.TextButton(
            onClick = onNormalCamera,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(
                Icons.Filled.PhotoCamera,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Normal Camera", fontSize = 18.sp, color = TextSecondary)
        }
    }
}

// ── Style Card (Home-page style) ────────────────────────────────────────────

/**
 * Photo booth style card — styled preview image background with text overlay.
 * Tapping goes straight to camera capture.
 */
@Composable
private fun BoothStyleCard(
    style: StyleOption,
    cardIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val previewRes = context.resources.getIdentifier(
        "style_preview_${style.key}", "drawable", context.packageName
    )

    Card(
        onClick = onClick,
        modifier = modifier.padding(4.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background: style preview image
            if (previewRes != 0) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = previewRes),
                    contentDescription = style.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback: purple gradient
                val cardColor = if (cardIndex == 0 || cardIndex == 3) StylePrimary else StyleSecondary
                Box(modifier = Modifier.fillMaxSize().background(cardColor))
            }

            // Dark scrim so text is always readable over the image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            // Style name centered
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = style.name,
                    color = Color.White,
                    fontSize = 74.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 78.sp
                )
            }
        }
    }
}

// ── Custom Prompt ────────────────────────────────────────────────────────────

// ── Processing Screen ────────────────────────────────────────────────────────

@Composable
private fun ProcessingScreen(styleName: String) {
    val displayName = STYLES.find { it.key == styleName }?.name ?: "Custom"

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            color = AccentPurple,
            strokeWidth = 6.dp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Creating your $displayName photo...",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This takes about 10 seconds",
            color = TextSecondary,
            fontSize = 16.sp
        )
    }
}

// ── Result Screen ────────────────────────────────────────────────────────────

@Composable
private fun ResultScreen(
    styledBitmap: Bitmap,
    qrBitmap: Bitmap?,
    onRetake: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Styled photo (left, larger)
        Box(
            modifier = Modifier
                .weight(2f)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, AccentPurple.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
            Image(
                bitmap = styledBitmap.asImageBitmap(),
                contentDescription = "Styled photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Right panel: QR + buttons
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Scan to Save",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // QR code
            if (qrBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(8.dp)
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR code",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentPurple)
                    Text("Generating QR...", color = TextSecondary, fontSize = 14.sp,
                        modifier = Modifier.padding(top = 60.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Try another
            Button(
                onClick = onRetake,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Try Another Style", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Done", color = TextPrimary, fontSize = 16.sp)
            }
        }
    }
}

// ── Error Screen ─────────────────────────────────────────────────────────────

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Something went wrong", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(message, color = TextSecondary, fontSize = 16.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp))
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
        ) {
            Text("Try Again", fontSize = 18.sp)
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Sends photo as HIGH_RES_PHOTO (0x08) binary frame to server.
 */
private fun sendPhotoToServer(bitmap: Bitmap, wsRepo: WebSocketRepository) {
    try {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        val jpegBytes = out.toByteArray()

        // Frame: 0x08 (HIGH_RES_PHOTO) + JPEG bytes
        val frame = ByteArray(1 + jpegBytes.size)
        frame[0] = 0x08
        System.arraycopy(jpegBytes, 0, frame, 1, jpegBytes.size)
        wsRepo.send(frame)
        Log.i(TAG, "Photo sent to server (${jpegBytes.size} bytes)")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to send photo", e)
    }
}
