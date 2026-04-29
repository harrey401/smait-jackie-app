package com.smait.jackie.ui.photobooth

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.smait.jackie.data.websocket.WebSocketEvent
import com.smait.jackie.data.websocket.WebSocketRepository
import com.smait.jackie.ui.conversation.SelfieCapture
import com.smait.jackie.ui.theme.SmaitBlack
import com.smait.jackie.ui.theme.SmaitGreen
import com.smait.jackie.ui.theme.SmaitGreenDim
import com.smait.jackie.ui.theme.SmaitSurface
import com.smait.jackie.ui.theme.SmaitTextMuted
import com.smait.jackie.ui.theme.SmaitTextPrimary
import kotlinx.coroutines.flow.filterIsInstance
import org.json.JSONObject
import java.io.ByteArrayOutputStream

private const val TAG = "PhotoBooth"

data class StyleOption(val key: String, val name: String)

private val STYLES = listOf(
    StyleOption("ghibli", "Ghibli"),
    StyleOption("pixar", "Pixar 3D"),
    StyleOption("cyberpunk", "Cyberpunk"),
    StyleOption("claymation", "Claymation"),
)

private const val STYLE_NORMAL = "normal"

private sealed class BoothState {
    object Picking : BoothState()
    data class Capturing(val style: String) : BoothState()
    data class Processing(val style: String) : BoothState()
    data class Result(
        val styledBitmap: Bitmap,
        val qrBitmap: Bitmap? = null,
        val downloadUrl: String = ""
    ) : BoothState()
    data class Error(val message: String) : BoothState()
}

@Composable
fun PhotoBoothScreen(navController: NavHostController, wsRepo: WebSocketRepository) {
    var state: BoothState by remember { mutableStateOf<BoothState>(BoothState.Picking) }

    LaunchedEffect(Unit) {
        wsRepo.events.filterIsInstance<WebSocketEvent.JsonMessage>().collect { event ->
            try {
                val json = JSONObject(event.payload)
                when (json.optString("type")) {
                    "styled_result" -> {
                        val b64 = json.getString("styled_b64")
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { bmp ->
                            state = BoothState.Result(styledBitmap = bmp)
                        }
                    }
                    "qr_code" -> {
                        val current = state
                        if (current is BoothState.Result) {
                            val qrB64 = json.getString("qr_b64")
                            val qrBytes = Base64.decode(qrB64, Base64.DEFAULT)
                            val qrBmp = BitmapFactory.decodeByteArray(qrBytes, 0, qrBytes.size)
                            state = current.copy(
                                qrBitmap = qrBmp,
                                downloadUrl = json.optString("download_url", "")
                            )
                        }
                    }
                    "photo_booth_error" -> {
                        state = BoothState.Error(json.optString("error", "Unknown error"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse server message", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SmaitBlack)) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "booth_state"
        ) { current ->
            when (current) {
                is BoothState.Picking -> PickerScreen(
                    onStyleSelect = { style ->
                        val msg = JSONObject().apply {
                            put("type", "photo_booth_style")
                            put("style", style)
                            put("mode", "portrait")
                        }
                        wsRepo.send(msg.toString())
                        state = BoothState.Capturing(style)
                    },
                    onNormalCamera = {
                        val msg = JSONObject().apply {
                            put("type", "photo_booth_style")
                            put("style", STYLE_NORMAL)
                            put("mode", "portrait")
                        }
                        wsRepo.send(msg.toString())
                        state = BoothState.Capturing(STYLE_NORMAL)
                    },
                    onBack = { navController.popBackStack() }
                )
                is BoothState.Capturing -> SelfieCapture(
                    onDismiss = { state = BoothState.Picking },
                    onCapture = { bitmap ->
                        sendPhotoToServer(bitmap, wsRepo)
                        state = BoothState.Processing(current.style)
                    }
                )
                is BoothState.Processing -> ProcessingScreen(styleName = current.style)
                is BoothState.Result -> ResultScreen(
                    styledBitmap = current.styledBitmap,
                    qrBitmap = current.qrBitmap,
                    onRetake = { state = BoothState.Picking },
                    onBack = { navController.popBackStack() }
                )
                is BoothState.Error -> ErrorScreen(
                    message = current.message,
                    onRetry = { state = BoothState.Picking }
                )
            }
        }
    }
}

@Composable
private fun PickerScreen(
    onStyleSelect: (String) -> Unit,
    onNormalCamera: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SmaitTextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "Choose Your Style",
                color = SmaitTextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(0.85f)
                .align(Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        ) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                BoothStyleCard(STYLES[0], { onStyleSelect(STYLES[0].key) }, Modifier.weight(1f).fillMaxHeight())
                BoothStyleCard(STYLES[1], { onStyleSelect(STYLES[1].key) }, Modifier.weight(1f).fillMaxHeight())
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                BoothStyleCard(STYLES[2], { onStyleSelect(STYLES[2].key) }, Modifier.weight(1f).fillMaxHeight())
                BoothStyleCard(STYLES[3], { onStyleSelect(STYLES[3].key) }, Modifier.weight(1f).fillMaxHeight())
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onNormalCamera,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(
                Icons.Filled.PhotoCamera,
                contentDescription = null,
                tint = SmaitTextMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Normal Camera", fontSize = 18.sp, color = SmaitTextMuted)
        }
    }
}

@Composable
private fun BoothStyleCard(
    style: StyleOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val previewRes = context.resources.getIdentifier(
        "style_preview_${style.key}", "drawable", context.packageName
    )

    Card(
        onClick = onClick,
        modifier = modifier.padding(4.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, SmaitGreenDim.copy(alpha = 0.6f))
    ) {
        Box(modifier = Modifier.fillMaxSize().background(SmaitSurface)) {
            if (previewRes != 0) {
                Image(
                    painter = painterResource(id = previewRes),
                    contentDescription = style.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = style.name,
                    color = SmaitTextPrimary,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 70.sp
                )
            }
        }
    }
}

@Composable
private fun ProcessingScreen(styleName: String) {
    val displayName = STYLES.find { it.key == styleName }?.name ?: "your"
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            color = SmaitGreen,
            strokeWidth = 6.dp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Creating your $displayName photo...",
            color = SmaitTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("This takes about 10 seconds", color = SmaitTextMuted, fontSize = 16.sp)
    }
}

@Composable
private fun ResultScreen(
    styledBitmap: Bitmap,
    qrBitmap: Bitmap?,
    onRetake: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(2f)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, SmaitGreen.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
        ) {
            Image(
                bitmap = styledBitmap.asImageBitmap(),
                contentDescription = "Styled photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        Column(
            modifier = Modifier.weight(1f).padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Scan to Save", color = SmaitTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
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
                Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SmaitGreen)
                    Text(
                        "Generating QR...",
                        color = SmaitTextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 60.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRetake,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SmaitGreen,
                    contentColor = SmaitBlack
                )
            ) {
                Text("Try Another Style", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SmaitGreenDim)
            ) {
                Text("Done", color = SmaitTextPrimary, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Something went wrong", color = SmaitTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            message,
            color = SmaitTextMuted,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SmaitGreen,
                contentColor = SmaitBlack
            )
        ) {
            Text("Try Again", fontSize = 18.sp)
        }
    }
}

private fun sendPhotoToServer(bitmap: Bitmap, wsRepo: WebSocketRepository) {
    try {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        val jpegBytes = out.toByteArray()
        val frame = ByteArray(1 + jpegBytes.size)
        frame[0] = 0x08
        System.arraycopy(jpegBytes, 0, frame, 1, jpegBytes.size)
        wsRepo.send(frame)
        Log.i(TAG, "Photo sent to server (${jpegBytes.size} bytes)")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to send photo", e)
    }
}
