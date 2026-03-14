package com.gow.smaitrobot.ui.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gow.smaitrobot.data.model.SponsorConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Horizontal sponsor logo bar.
 *
 * - If the sponsor count is 4 or fewer, logos are displayed in a static centered row.
 * - If there are more than 4 sponsors, the row auto-scrolls horizontally at a gentle pace
 *   (1 pixel per 50ms) and loops back to start on overflow.
 *
 * Displayed on Home and Event Info screens only (not Chat/Map/Facilities).
 *
 * Logo images are shown as placeholder text boxes until real PNG assets are available.
 *
 * @param sponsors  List of [SponsorConfig] entries to display.
 * @param modifier  Optional modifier for outer layout control.
 */
@Composable
fun SponsorBar(
    sponsors: List<SponsorConfig>,
    modifier: Modifier = Modifier
) {
    if (sponsors.isEmpty()) return

    val scrollState = rememberScrollState()
    val shouldScroll = sponsors.size > 4

    if (shouldScroll) {
        AutoScrollRow(scrollState = scrollState, sponsors = sponsors, modifier = modifier)
    } else {
        StaticRow(sponsors = sponsors, modifier = modifier)
    }
}

/**
 * Static (non-scrolling) sponsor row for 4 or fewer sponsors.
 */
@Composable
private fun StaticRow(
    sponsors: List<SponsorConfig>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        sponsors.forEach { sponsor ->
            SponsorLogoPlaceholder(
                name = sponsor.name,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

/**
 * Auto-scrolling sponsor row for more than 4 sponsors.
 *
 * Scrolls 1 pixel every 50ms and resets to 0 when the end is reached,
 * creating a continuous marquee effect.
 */
@Composable
private fun AutoScrollRow(
    scrollState: ScrollState,
    sponsors: List<SponsorConfig>,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(sponsors) {
        while (isActive) {
            delay(50)
            val maxScroll = scrollState.maxValue
            if (maxScroll <= 0) {
                delay(200)
                continue
            }
            val next = scrollState.value + 1
            if (next >= maxScroll) {
                // Reset to beginning for seamless loop
                scrollState.scrollTo(0)
            } else {
                scrollState.scrollTo(next)
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(scrollState, enabled = false) // user drag disabled; auto-scroll only
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        sponsors.forEach { sponsor ->
            SponsorLogoPlaceholder(
                name = sponsor.name,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

/**
 * Placeholder box for a sponsor logo.
 *
 * Replace with a real logo [Image] once PNG assets are added to the assets folder.
 * Sized to 80×32dp to keep the sponsor bar at 48dp height with 8dp vertical padding.
 */
@Composable
private fun SponsorLogoPlaceholder(
    name: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 80.dp, height = 32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
