package com.gow.smaitrobot.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Top logo bar showing SJSU and BioRob lab branding.
 *
 * Displays two placeholder boxes with text labels representing the institution logos.
 * Real PNG assets can be dropped in later by replacing the placeholder composables
 * with [androidx.compose.foundation.Image] using [painterResource].
 *
 * Height: 60dp as specified in design requirements.
 * Logos are horizontally centered and evenly spaced.
 */
@Composable
fun TopLogoBar(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SJSU logo placeholder
        LogoPlaceholder(
            label = "SJSU",
            modifier = Modifier.padding(end = 24.dp)
        )

        // BioRob lab logo placeholder
        LogoPlaceholder(
            label = "BioRob"
        )
    }
}

/**
 * Placeholder for a logo image.
 *
 * Renders as a colored box with a text label. Replace with a real logo image when
 * the PNG assets are available:
 * ```kotlin
 * Image(
 *     painter = painterResource(id = R.drawable.sjsu_logo),
 *     contentDescription = "SJSU Logo",
 *     modifier = Modifier.height(44.dp)
 * )
 * ```
 */
@Composable
private fun LogoPlaceholder(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 80.dp, height = 44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
