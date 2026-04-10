package com.gow.eng192lab.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.gow.eng192lab.R
import com.gow.eng192lab.data.model.CardConfig
import com.gow.eng192lab.navigation.Screen

// Lab color palette — deep blue tech theme
private val LabDark = Color(0xFF0D1642)
private val LabPrimary = Color(0xFF1A237E)
private val LabAccent = Color(0xFF42A5F5)
private val CardBlue1 = Color(0xFF1565C0)
private val CardBlue2 = Color(0xFF1A237E)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val eventName by viewModel.eventName.collectAsStateWithLifecycle()
    val tagline by viewModel.tagline.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LabDark, LabPrimary, LabDark)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: logos + title, long-press for Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            navController.navigate(Screen.Settings) {
                                launchSingleTop = true
                            }
                        }
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.biorob_logo),
                    contentDescription = "BioRob Lab",
                    modifier = Modifier.height(140.dp),
                    contentScale = ContentScale.Fit
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = eventName,
                        color = Color.White,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = tagline,
                        color = LabAccent,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.sjsu_logo),
                    contentDescription = "SJSU Mechanical Engineering",
                    modifier = Modifier.height(200.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // 2x2 card grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(0.85f)
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    cards.getOrNull(0)?.let { card ->
                        LabCard(card, 0, { handleCardClick(card, viewModel, navController) },
                            Modifier.weight(1f).fillMaxHeight())
                    }
                    cards.getOrNull(1)?.let { card ->
                        LabCard(card, 1, { handleCardClick(card, viewModel, navController) },
                            Modifier.weight(1f).fillMaxHeight())
                    }
                }
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    cards.getOrNull(2)?.let { card ->
                        LabCard(card, 2, { handleCardClick(card, viewModel, navController) },
                            Modifier.weight(1f).fillMaxHeight())
                    }
                    cards.getOrNull(3)?.let { card ->
                        LabCard(card, 3, { handleCardClick(card, viewModel, navController) },
                            Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }

            // Footer
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "BioRob Lab  ·  SJSU Mechanical Engineering  ·  ME 192",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 20.sp
                )
            }
        }
    }
}

private fun handleCardClick(
    card: CardConfig,
    viewModel: HomeViewModel,
    navController: NavHostController
) {
    when (val action = viewModel.parseCardAction(card.action)) {
        is CardAction.NavigateToTab -> {
            navController.navigate(action.screen) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        is CardAction.ShowInlineContent -> {}
        is CardAction.OpenUrl -> {}
    }
}

@Composable
private fun LabCard(
    card: CardConfig,
    cardIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = if (cardIndex == 0 || cardIndex == 3) {
        CardBlue1.copy(alpha = 0.85f)
    } else {
        CardBlue2.copy(alpha = 0.85f)
    }

    Card(
        onClick = onClick,
        modifier = modifier.padding(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = cardIcon(card.icon),
                contentDescription = card.label,
                tint = LabAccent,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = card.label,
                color = Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 68.sp
            )
            if (card.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = card.description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 36.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 40.sp
                )
            }
        }
    }
}

private fun cardIcon(iconName: String): ImageVector = when (iconName) {
    "chat" -> Icons.AutoMirrored.Filled.Chat
    "map" -> Icons.Filled.Map
    "camera" -> Icons.Filled.PhotoCamera
    "follow" -> Icons.Filled.DirectionsWalk
    "tour" -> Icons.Filled.Explore
    "info" -> Icons.Filled.Info
    "settings" -> Icons.Filled.Settings
    else -> Icons.Filled.Info
}
