package com.example.timequest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    secondary = AccentGreen,
    background = SoftBackground,
    surface = CardWhite,
    surfaceVariant = Color(0xFFE7ECF3),
    onSurfaceVariant = Color(0xFF5B6472)
)

private val DarkColors = darkColorScheme(
    primary = PrimaryBlueDark,
    secondary = AccentGreen,
    background = DarkBackground,
    surface = DarkSurface
)

@Composable
fun TimeQuestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
