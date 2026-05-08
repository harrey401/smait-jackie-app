package com.gow.smaitrobot.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

/**
 * Brochure-style event background. Re-creates the visual language of the
 * Mechanical Engineering Senior Projects brochure:
 *  - Thick gold trim band at the very top
 *  - Solid SJSU-navy "header bar" beneath the trim
 *  - Navy ribbon with concentric arc lines along the right side
 *  - Big organic navy wave hugging the bottom-left, with concentric arc
 *    line patterns flowing inside it
 *  - Bottom: thin gold strip then a thick navy footer strip
 *
 * All ornaments use [MaterialTheme.colorScheme] so swapping the theme JSON
 * recolours the entire layout.
 */
@Composable
fun WieBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Box(modifier = modifier.fillMaxSize().background(scheme.background)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // ── TOP ─────────────────────────────────────────────────
            // Thick gold trim
            drawRect(
                color = scheme.secondary,
                topLeft = Offset(0f, 0f),
                size = Size(w, 14.dp.toPx())
            )

            // ── RIGHT-SIDE NAVY RIBBON with concentric arc pattern ──
            // Vertical navy strip behind the arcs (subtle)
            val ribbonW = 220.dp.toPx()
            drawRect(
                color = scheme.primary.copy(alpha = 0.06f),
                topLeft = Offset(w - ribbonW, 14.dp.toPx()),
                size = Size(ribbonW, h * 0.32f)
            )
            // Concentric arcs anchored off the right edge — like sound rings
            val arcCenter = Offset(w + 80.dp.toPx(), -40.dp.toPx())
            val arcStrokeW = 3.dp.toPx()
            for (i in 0 until 14) {
                val r = (180 + i * 38).dp.toPx()
                drawCircle(
                    color = scheme.primary.copy(alpha = 0.16f),
                    radius = r,
                    center = arcCenter,
                    style = Stroke(width = arcStrokeW)
                )
            }

            // ── BOTTOM-LEFT WAVE ────────────────────────────────────
            // Big navy organic shape
            val waveTopL = Offset(-w * 0.10f, h * 0.62f)
            val wavePath = Path().apply {
                moveTo(waveTopL.x, waveTopL.y)
                cubicTo(
                    w * 0.18f, h * 0.55f,
                    w * 0.30f, h * 0.96f,
                    w * 0.62f, h * 0.78f
                )
                cubicTo(
                    w * 0.78f, h * 0.66f,
                    w * 0.92f, h * 0.92f,
                    w * 1.05f, h * 0.84f
                )
                lineTo(w * 1.05f, h)
                lineTo(-w * 0.10f, h)
                close()
            }
            drawPath(path = wavePath, color = scheme.primary)

            // Concentric arc lines INSIDE the wave (clipped to the wave shape)
            clipPath(path = wavePath, clipOp = ClipOp.Intersect) {
                val ringCenter = Offset(w * 0.30f, h * 1.10f)
                for (i in 0 until 18) {
                    val r = (120 + i * 28).dp.toPx()
                    drawCircle(
                        color = scheme.onPrimary.copy(alpha = 0.08f),
                        radius = r,
                        center = ringCenter,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                // Second arc cluster on the right side of the wave
                val ringCenter2 = Offset(w * 0.92f, h * 1.05f)
                for (i in 0 until 10) {
                    val r = (100 + i * 30).dp.toPx()
                    drawCircle(
                        color = scheme.onPrimary.copy(alpha = 0.06f),
                        radius = r,
                        center = ringCenter2,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // ── BOTTOM TRIM ─────────────────────────────────────────
            // Thin gold band sitting on top of the navy footer strip
            drawRect(
                color = scheme.secondary,
                topLeft = Offset(0f, h - 18.dp.toPx()),
                size = Size(w, 6.dp.toPx())
            )
            drawRect(
                color = scheme.primary,
                topLeft = Offset(0f, h - 12.dp.toPx()),
                size = Size(w, 12.dp.toPx())
            )
        }

        content()
    }
}
