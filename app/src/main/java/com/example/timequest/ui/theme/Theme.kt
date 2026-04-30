package com.example.timequest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    secondary = AccentGreen,
    background = SoftBackground,
    surface = CardWhite,
    surfaceVariant = Color(0xFFE7ECF3),
    primaryContainer = Color(0xFFDDE8FF),
    secondaryContainer = Color(0xFFDDF4E9),
    error = Color(0xFFB94A4A),
    errorContainer = Color(0xFFFFDADA),
    onSurfaceVariant = Color(0xFF5B6472)
)

private val DarkColors = darkColorScheme(
    primary = PrimaryBlueDark,
    secondary = AccentGreen,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceSoft,
    surfaceContainer = DarkSurfaceHigh,
    surfaceContainerHigh = DarkSurfaceSoft,
    primaryContainer = Color(0xFF24345A),
    secondaryContainer = Color(0xFF183B32),
    error = DangerRed,
    errorContainer = Color(0xFF4A2529),
    onBackground = Color(0xFFE9EDF5),
    onSurface = Color(0xFFE9EDF5),
    onSurfaceVariant = Color(0xFFC2C8D4),
    onPrimaryContainer = Color(0xFFDCE6FF),
    onSecondaryContainer = Color(0xFFD9F6E7),
    onErrorContainer = Color(0xFFFFD8D8)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun TimeQuestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
