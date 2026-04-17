package com.gow.eng192lab.ui.photobooth

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkBg = Color(0xFF121212)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val StylePrimary = Color(0xFF2D1B69).copy(alpha = 0.85f)
private val StyleSecondary = Color(0xFF4A3278).copy(alpha = 0.85f)

/**
 * Photo booth style picker — 2x2 grid of large cards matching the home screen layout.
 *
 * Tapping a card selects the style and immediately starts the countdown — no
 * separate "Take Photo" button. A "Normal Camera" link at the bottom bypasses
 * styling and captures a raw photo.
 *
 * The ViewModel's state-select + start-capture are called sequentially by
 * [onStyleTapped] (see PhotoBoothScreen).
 */
@Composable
fun StylePickerGrid(
    selectedStyle: String?,
    onStyleTapped: (String) -> Unit,
    onNormalCamera: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
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
                STYLE_OPTIONS.getOrNull(0)?.let { style ->
                    BoothStyleCard(
                        style = style,
                        cardIndex = 0,
                        onClick = { onStyleTapped(style.key) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                STYLE_OPTIONS.getOrNull(1)?.let { style ->
                    BoothStyleCard(
                        style = style,
                        cardIndex = 1,
                        onClick = { onStyleTapped(style.key) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                STYLE_OPTIONS.getOrNull(2)?.let { style ->
                    BoothStyleCard(
                        style = style,
                        cardIndex = 2,
                        onClick = { onStyleTapped(style.key) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                STYLE_OPTIONS.getOrNull(3)?.let { style ->
                    BoothStyleCard(
                        style = style,
                        cardIndex = 3,
                        onClick = { onStyleTapped(style.key) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
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
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Normal Camera", fontSize = 18.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun BoothStyleCard(
    style: StyleOption,
    cardIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val previewRes = if (style.previewResId != 0) {
        style.previewResId
    } else {
        context.resources.getIdentifier(
            "style_preview_${style.key}", "drawable", context.packageName
        )
    }

    Card(
        onClick = onClick,
        modifier = modifier.padding(4.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (previewRes != 0) {
                Image(
                    painter = painterResource(id = previewRes),
                    contentDescription = style.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                val cardColor = if (cardIndex == 0 || cardIndex == 3) StylePrimary else StyleSecondary
                Box(modifier = Modifier.fillMaxSize().background(cardColor))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = style.label,
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
