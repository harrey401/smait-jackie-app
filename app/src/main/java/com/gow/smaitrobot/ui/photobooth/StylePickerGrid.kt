package com.gow.smaitrobot.ui.photobooth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
 * 2x3 style picker grid composable.
 *
 * Displays [STYLE_OPTIONS] as tappable cards in a 2-column LazyVerticalGrid.
 * Selected card shows a highlighted primary-color border.
 * "Take Photo" button is enabled only when [selectedStyle] is non-null.
 *
 * Touch targets are at least 44dp per UI/UX accessibility rules.
 */
@Composable
fun StylePickerGrid(
    selectedStyle: String?,
    onStyleSelected: (String) -> Unit,
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

            Text(
                text = "Choose a style",
                color = Color(0xFFB0B0C8),
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
