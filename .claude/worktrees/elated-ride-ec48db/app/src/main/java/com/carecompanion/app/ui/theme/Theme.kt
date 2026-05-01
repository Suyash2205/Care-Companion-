package com.carecompanion.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CareGreen,
    onPrimary = Color.White,
    primaryContainer = CareGreen.copy(alpha = 0.12f),
    secondary = CareGreenDark,
    onSecondary = Color.White,
    tertiary = CareGreen,
    background = Color(0xFFF5F6F4),
    surface = Color.White,
    onBackground = Color(0xFF1C1C1C),
    onSurface = Color(0xFF1C1C1C)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF0D1F12),
    secondary = CareGreen,
    onSecondary = Color.White,
    background = Color(0xFF121512),
    surface = Color(0xFF1C221E),
    onBackground = Color(0xFFE8EBE9),
    onSurface = Color(0xFFE8EBE9)
)

@Composable
fun CareCompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
