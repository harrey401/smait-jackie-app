package com.smait.jackie.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.smait.jackie.R
import com.smait.jackie.data.model.RobotState
import com.smait.jackie.navigation.Screen
import com.smait.jackie.ui.common.SmaitStatusBar
import com.smait.jackie.ui.conversation.RobotAvatar
import com.smait.jackie.ui.theme.SmaitBlack
import com.smait.jackie.ui.theme.SmaitGreen
import com.smait.jackie.ui.theme.SmaitGreenDim
import com.smait.jackie.ui.theme.SmaitSurface
import com.smait.jackie.ui.theme.SmaitTextMuted
import kotlinx.coroutines.delay

private val GREETINGS = listOf(
    "How can I help today?",
    "Ask me anything.",
    "Looking for someone?",
    "Want a tour?",
    "Say hi — I'm listening."
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    var greetingIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_500L)
            greetingIndex = (greetingIndex + 1) % GREETINGS.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SmaitBlack)
    ) {
        AmbientGreenGlow()

        Column(modifier = Modifier.fillMaxSize()) {
            SmaitStatusBar()

            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Image(
                    painter = painterResource(id = R.drawable.smait_logo),
                    contentDescription = "SMAIT",
                    modifier = Modifier
                        .height(72.dp)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                navController.navigate(Screen.Settings) { launchSingleTop = true }
                            }
                        ),
                    contentScale = ContentScale.Fit
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.50f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    BreathingAvatar()
                }

                Spacer(Modifier.size(16.dp))

                Column(
                    modifier = Modifier
                        .weight(0.50f)
                        .fillMaxHeight()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        ActionCard(
                            label = "Ask Me Anything",
                            description = "Voice conversation",
                            icon = Icons.AutoMirrored.Filled.Chat,
                            onClick = { navController.navigate(Screen.Chat) { launchSingleTop = true } },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        ActionCard(
                            label = "Office Tour",
                            description = "Walk me through SMAIT",
                            icon = Icons.Filled.Map,
                            onClick = { navController.navigate(Screen.Map) { launchSingleTop = true } },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        ActionCard(
                            label = "Photo",
                            description = "Snap a styled portrait",
                            icon = Icons.Filled.CameraAlt,
                            onClick = { navController.navigate(Screen.Photo) { launchSingleTop = true } },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        ActionCard(
                            label = "Website",
                            description = "Visit smait.ai",
                            icon = Icons.Filled.Public,
                            onClick = { navController.navigate(Screen.Website) { launchSingleTop = true } },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "smAIT  ·  Office Robot Platform",
                    color = SmaitTextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 4.sp
                )
            }
        }
    }
}

@Composable
private fun BreathingAvatar() {
    RobotAvatar(
        robotState = RobotState.IDLE,
        modifier = Modifier.size(620.dp)
    )
}

@Composable
private fun AmbientGreenGlow() {
    val transition = rememberInfiniteTransition(label = "bgGlow")
    val alpha by transition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(SmaitGreen.copy(alpha = alpha), Color.Transparent),
                    radius = 900f
                )
            )
    )
}

@Composable
private fun ActionCard(
    label: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SmaitSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, SmaitGreenDim.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SmaitGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = SmaitBlack,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Column {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 40.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        color = SmaitTextMuted,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
