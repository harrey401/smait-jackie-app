package com.gow.smaitrobot.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.gow.smaitrobot.data.model.CardConfig
import com.gow.smaitrobot.ui.common.SponsorBar
import com.gow.smaitrobot.ui.common.TopLogoBar

/**
 * Home screen composable — the first screen users see on the robot.
 *
 * Layout (top to bottom):
 * 1. [TopLogoBar] — SJSU + BioRob logos, 60dp height
 * 2. Event name (28sp bold, primary color) + tagline (18sp)
 * 3. Card grid — [LazyVerticalGrid] with [GridCells.Fixed(3)] for a 2×3 layout
 * 4. [SponsorBar] — sponsor logos at the bottom
 *
 * Each card displays an icon and label. Tapping a card:
 * - For "navigate:*" actions: navigates to the corresponding bottom nav tab
 * - For "inline:*" actions: shows a dialog with placeholder content
 *
 * Accessibility requirements met:
 * - Minimum 18sp text, 24sp card labels
 * - 80dp minimum card height (large touch targets)
 * - 8dp minimum padding between cards
 * - Ripple feedback via Material 3 [ElevatedCard] default clickable
 *
 * @param viewModel     [HomeViewModel] providing card/sponsor/event data.
 * @param navController [NavHostController] for programmatic tab navigation.
 * @param modifier      Optional modifier for outer layout.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val eventName by viewModel.eventName.collectAsStateWithLifecycle()
    val tagline by viewModel.tagline.collectAsStateWithLifecycle()
    val sponsors by viewModel.sponsors.collectAsStateWithLifecycle()

    // State for inline content dialog
    var inlineContentKey by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // ── 1. Top logo bar ───────────────────────────────────────────────────
        TopLogoBar()

        // ── 2. Event name + tagline ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = eventName,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = tagline,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // ── 3. Card grid ──────────────────────────────────────────────────────
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cards) { card ->
                HomeCard(
                    card = card,
                    onClick = {
                        when (val action = viewModel.parseCardAction(card.action)) {
                            is CardAction.NavigateToTab -> {
                                navController.navigate(action.screen) {
                                    popUpTo(
                                        navController.graph.startDestinationId
                                    ) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            is CardAction.ShowInlineContent -> {
                                inlineContentKey = action.contentKey
                            }
                        }
                    }
                )
            }
        }

        // ── 4. Sponsor bar ────────────────────────────────────────────────────
        SponsorBar(sponsors = sponsors)
    }

    // ── Inline content dialog ─────────────────────────────────────────────────
    inlineContentKey?.let { key ->
        InlineContentDialog(
            contentKey = key,
            onDismiss = { inlineContentKey = null }
        )
    }
}

/**
 * Individual home screen card composable.
 *
 * Material 3 [ElevatedCard] with icon (top) and label text (bottom).
 * Minimum height 80dp for accessibility touch targets.
 *
 * @param card     [CardConfig] providing label, icon name, and action string.
 * @param onClick  Callback invoked when the card is tapped.
 */
@Composable
private fun HomeCard(
    card: CardConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = cardIcon(card.icon),
                contentDescription = card.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = card.label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

/**
 * Maps a [CardConfig.icon] name string to a Material Icons [ImageVector].
 *
 * Supported icon names: "chat", "map", "star", "schedule", "location", "info".
 * Unrecognised names fall back to [Icons.Filled.Info].
 */
private fun cardIcon(iconName: String): ImageVector = when (iconName) {
    "chat" -> Icons.Filled.Chat
    "map" -> Icons.Filled.Map
    "star" -> Icons.Filled.Star
    "schedule" -> Icons.Filled.Schedule
    "location" -> Icons.Filled.LocationOn
    "info" -> Icons.Filled.Info
    else -> Icons.Filled.Info
}

/**
 * Placeholder dialog for "inline:*" card actions.
 *
 * Shows the content key as a title and a placeholder message.
 * Replace with rich content (e.g. a bottom sheet with schedule data) in a future plan.
 *
 * @param contentKey  The inline content identifier (e.g. "keynote", "sessions").
 * @param onDismiss   Callback to dismiss the dialog.
 */
@Composable
private fun InlineContentDialog(
    contentKey: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = contentKey.replaceFirstChar { it.uppercase() },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Detailed $contentKey information will be displayed here.",
                fontSize = 18.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontSize = 18.sp)
            }
        }
    )
}
