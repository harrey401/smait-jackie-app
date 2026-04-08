package com.gow.smaitrobot.ui.photobooth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BackgroundColor = Color(0xFF0D0D14)
private val CardBackgroundUnselected = Color(0xFF1A1A2E)
private val SelectedBorderColor = Color(0xFF7C4DFF)
private val TakePhotoBgColor = Color(0xFF7C4DFF)

/**
 * Style picker composable — mode tabs + 3-column style grid + Take Photo button.
 *
 * Two modes (added in v2.1):
 *   Portrait: face-centric restyling (PuLID). Best for single-person selfies —
 *             keeps your face perfectly recognizable, restyles only you.
 *   Scene:    whole-image restyling (SDXL img2img). Best for group photos with
 *             rich backgrounds — restyles everyone AND the environment together.
 *
 * The same 6 style keys work in both modes with different prompt templates
 * on the server side, so mode is a parallel axis, not a hierarchy.
 *
 * "Take Photo" button is enabled only when [selectedStyle] is non-null.
 * Touch targets are at least 44dp per UI/UX accessibility rules.
 */
@Composable
fun StylePickerGrid(
    selectedStyle: String?,
    mode: String,
    onStyleSelected: (String) -> Unit,
    onModeSelected: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with back button and title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(Color(0xFF12121F))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .semantics { contentDescription = "Back" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Text(
                    text = "Photo Booth",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Mode tabs — Portrait vs Scene
            ModeTabs(
                selectedMode = mode,
                onModeSelected = onModeSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Text(
                text = modeSubtitle(mode),
                color = Color(0xFFB0B0C8),
                fontSize = 13.sp,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // 3-column grid — 6 styles fit in 2 rows with no scroll required,
            // which avoids users thinking only the top row exists.
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(STYLE_OPTIONS, key = { it.key }) { style ->
                    StyleCard(
                        style = style,
                        isSelected = style.key == selectedStyle,
                        onSelected = { onStyleSelected(style.key) }
                    )
                }
            }

            // Take Photo button
            Button(
                onClick = onTakePhoto,
                enabled = selectedStyle != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TakePhotoBgColor,
                    disabledContainerColor = Color(0xFF3A3A5C)
                )
            ) {
                Text(
                    text = "Take Photo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedStyle != null) Color.White else Color(0xFF6B6B8A)
                )
            }
        }
    }
}

/**
 * A single style card showing the thumbnail and label.
 * Selected state is indicated by a highlighted border.
 */
@Composable
private fun StyleCard(
    style: StyleOption,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected)
            .semantics { contentDescription = "${style.label} style" },
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) {
            BorderStroke(3.dp, SelectedBorderColor)
        } else {
            BorderStroke(1.dp, Color(0xFF2A2A3E))
        },
        colors = CardDefaults.cardColors(containerColor = CardBackgroundUnselected),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Column {
            // Thumbnail image — 1:1 aspect ratio (ensures minimum touch target via card sizing)
            Image(
                painter = painterResource(id = style.thumbResId),
                contentDescription = "${style.label} thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )

            // Style label
            Text(
                text = style.label,
                color = if (isSelected) Color.White else Color(0xFFB0B0C8),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}

/**
 * Segmented control for switching between Portrait and Scene modes.
 *
 * Visual: pill-shaped container with two equal-width cells; the selected
 * cell fills with the SMAIT violet, unselected cells stay translucent.
 * Text and background colors animate on change (200ms tween) so the
 * tap feels smooth.
 *
 * Both modes accept the same 6 style keys; the server interprets them
 * differently (face-centric vs whole-image).
 */
@Composable
private fun ModeTabs(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1A1A2E))
            .border(BorderStroke(1.dp, Color(0xFF2A2A3E)), RoundedCornerShape(24.dp)),
    ) {
        ModeTabCell(
            label = "Portrait",
            selected = selectedMode == MODE_PORTRAIT,
            onClick = { onModeSelected(MODE_PORTRAIT) },
            modifier = Modifier.weight(1f),
        )
        ModeTabCell(
            label = "Scene",
            selected = selectedMode == MODE_SCENE,
            onClick = { onModeSelected(MODE_SCENE) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModeTabCell(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) Color(0xFF7C4DFF) else Color.Transparent,
        animationSpec = tween(200),
        label = "modeTabBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Color.White else Color(0xFFB0B0C8),
        animationSpec = tween(200),
        label = "modeTabFg",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$label mode" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.5.sp,
        )
    }
}

/** Short subtitle shown under the mode tabs — gives users a one-liner hint. */
private fun modeSubtitle(mode: String): String = when (mode) {
    MODE_SCENE -> "Transforms the whole photo — background, people, everything"
    else -> "Restyles your face while keeping you recognizable"
}
