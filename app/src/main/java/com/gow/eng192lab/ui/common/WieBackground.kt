package com.gow.eng192lab.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Full-screen BioRob blue gradient background.
 *
 * Wraps [content] on top of a dark navy → BioRob blue → dark navy gradient.
 * Retains the WieBackground name for callsite stability across screens.
 */
@Composable
fun WieBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0A1F3F),
                        Color(0xFF0F3270),
                        Color(0xFF0A1F3F)
                    )
                )
            )
    ) {
        content()
    }
}
