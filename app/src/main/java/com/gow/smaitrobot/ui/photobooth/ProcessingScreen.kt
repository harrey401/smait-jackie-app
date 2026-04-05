package com.gow.smaitrobot.ui.photobooth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Processing screen shown while SD style transfer runs on the server.
 *
 * Displays a centered spinner and the name of the selected style.
 * Dark background matches the overall Photo Booth theme.
 *
 * @param styleName Human-readable style name from [StyleOption.label].
 */
@Composable
fun ProcessingScreen(styleName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(72.dp),
                color = Color(0xFF7C4DFF),
                strokeWidth = 5.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Applying $styleName…",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This usually takes 5–8 seconds",
                color = Color(0xFF7070A0),
                fontSize = 13.sp
            )
        }
    }
}
