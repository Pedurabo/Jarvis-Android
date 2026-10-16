package com.example.jarvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisBlue,
    onPrimary = Color.Black,
    secondary = UserBubble,
    onSecondary = Color.White,
    secondaryContainer = AssistantBubble,
    onSecondaryContainer = Color.White,
    background = DeepBlack,
    surface = DarkGrey,
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFCF6679)
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = Typography,
        content = content
    )
}
