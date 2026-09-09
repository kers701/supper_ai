package com.aichat.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF10A37F),
    onPrimary = Color.White,
    secondary = Color(0xFF565869),
    background = Color(0xFF212121),
    surface = Color(0xFF2F2F2F),
    onBackground = Color(0xFFECECEC),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF3E3E3E),
    error = Color(0xFFEF4444)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF10A37F),
    onPrimary = Color.White,
    secondary = Color(0xFF6E6E80),
    background = Color(0xFFF7F7F8),
    surface = Color.White,
    onBackground = Color(0xFF0D0D0D),
    onSurface = Color(0xFF0D0D0D),
    surfaceVariant = Color(0xFFECECF1),
    error = Color(0xFFDC2626)
)

@Composable
fun AiChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
