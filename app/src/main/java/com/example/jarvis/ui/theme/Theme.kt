package com.example.jarvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDarkColorScheme = darkColorScheme(
    primary = Color(0xFF66CED5),
    onPrimary = Color(0xFF09242B),

    primaryContainer = Color(0xFF244149),
    onPrimaryContainer = Color(0xFFE2F7F8),

    secondary = Color(0xFF91BCC4),
    onSecondary = Color(0xFF10262D),

    secondaryContainer = Color(0xFF263D46),
    onSecondaryContainer = Color(0xFFE0EDF0),

    background = Color(0xFF10171D),
    onBackground = Color(0xFFE4ECEF),

    surface = Color(0xFF172229),
    onSurface = Color(0xFFE4ECEF),

    surfaceVariant = Color(0xFF2B3B44),
    onSurfaceVariant = Color(0xFFAFBEC4),

    surfaceContainerLowest = Color(0xFF10171D),
    surfaceContainerLow = Color(0xFF1B2931),
    surfaceContainer = Color(0xFF22313A),
    surfaceContainerHigh = Color(0xFF2B3B44),

    outline = Color(0xFF607680),
    outlineVariant = Color(0xFF354750),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF782D30),
    onErrorContainer = Color(0xFFFFDAD6)
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
