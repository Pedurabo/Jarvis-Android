package com.example.jarvis.ui

import androidx.compose.ui.text.AnnotatedString

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

// UI State Models
enum class AssistantState {
    IDLE, LISTENING, THINKING, SPEAKING
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// Theme Colors
private val DeepBlack = Color(0xFF0A0A0A)
private val CyberCyan = Color(0xFF00E5FF)
private val DarkGray = Color(0xFF1A1A1A)

@Composable
fun AssistantScreen(
    state: AssistantState,
    messages: List<ChatMessage>,
    onMicClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(24.dp)
    ) {
        // 1. Top Status/Brand
        Text(
            text = "JARVIS",
            style = MaterialTheme.typography.labelMedium,
            color = CyberCyan.copy(alpha = 0.6f),
            letterSpacing = 4.sp,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 2. Minimal Chat History (Last 3-5)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(bottom = 100.dp)
        ) {
            val displayMessages = messages.takeLast(4)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(displayMessages) { msg ->
                    ChatBubble(msg)
                }
            }
        }

        // 3. Central Voice Controller (The Orb)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            VoiceOrb(
                state = state,
                onClick = onMicClick
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(message) {
        alpha.animateTo(1f, animationSpec = tween(500))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = alpha.value),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (message.isUser) "You" else "Jarvis",
            style = MaterialTheme.typography.labelSmall,
            color = if (message.isUser) Color.Gray else CyberCyan,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Surface(
            color = if (message.isUser) DarkGray else CyberCyan.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            border = if (message.isUser) null else androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
        ) {
            Text(
                text = if (message.isUser) AnnotatedString(message.text) else JarvisMarkdownFormatter.format(message.text),
                color = Color.White,
                modifier = Modifier.padding(12.dp),
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun VoiceOrb(state: AssistantState, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    // Breathing Glow Animation
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
    )

    // Speaking Wave Animation
    val waveHeight by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "wave"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer Glow
        if (state != AssistantState.IDLE) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer(scaleX = glowScale, scaleY = glowScale)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = CyberCyan,
                        spotColor = CyberCyan
                    )
                    .background(CyberCyan.copy(alpha = 0.2f), CircleShape)
            )
        }

        // Main Button
        Surface(
            modifier = Modifier
                .size(72.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            shape = CircleShape,
            color = if (state == AssistantState.LISTENING) CyberCyan else DarkGray,
            tonalElevation = 8.dp,
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(2.dp, CyberCyan)
        ) {
            Box(contentAlignment = Alignment.Center) {
                when (state) {
                    AssistantState.IDLE -> Icon(Icons.Default.Mic, "Mic", tint = CyberCyan, modifier = Modifier.size(32.dp))
                    AssistantState.LISTENING -> Icon(Icons.Default.Stop, "Stop", tint = DeepBlack, modifier = Modifier.size(32.dp))
                    AssistantState.THINKING -> CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    AssistantState.SPEAKING -> SpeakingWaves(waveHeight)
                }
            }
        }
    }
}

@Composable
fun SpeakingWaves(intensity: Float) {
    Row(
        modifier = Modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            val heightFactor = if (index % 2 == 0) intensity else intensity * 0.7f
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightFactor)
                    .background(CyberCyan, RoundedCornerShape(2.dp))
            )
        }
    }
}
