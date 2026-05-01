package com.example.timequest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppThemeStyle {
    CLASSIC,
    FOREST,
    SUNSET,
    COSMOS,
    OCEAN,
    SAKURA,
    GRAPHITE
}

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
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    themeStyle: AppThemeStyle = AppThemeStyle.CLASSIC,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val colorScheme = themedColorScheme(
        base = if (darkTheme) DarkColors else LightColors,
        style = themeStyle,
        darkTheme = darkTheme
    )
    val animatedPrimary by animateColorAsState(colorScheme.primary, label = "theme primary")
    val animatedSecondary by animateColorAsState(colorScheme.secondary, label = "theme secondary")
    val animatedBackground by animateColorAsState(colorScheme.background, label = "theme background")
    val animatedSurface by animateColorAsState(colorScheme.surface, label = "theme surface")
    val animatedPrimaryContainer by animateColorAsState(colorScheme.primaryContainer, label = "theme primary container")
    val animatedSecondaryContainer by animateColorAsState(colorScheme.secondaryContainer, label = "theme secondary container")
    val animatedColorScheme = colorScheme.copy(
        primary = animatedPrimary,
        secondary = animatedSecondary,
        background = animatedBackground,
        surface = animatedSurface,
        primaryContainer = animatedPrimaryContainer,
        secondaryContainer = animatedSecondaryContainer
    )

    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        window.statusBarColor = animatedColorScheme.background.toArgb()
        window.navigationBarColor = animatedColorScheme.surfaceContainer.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

private fun themedColorScheme(
    base: androidx.compose.material3.ColorScheme,
    style: AppThemeStyle,
    darkTheme: Boolean
): androidx.compose.material3.ColorScheme {
    return when (style) {
        AppThemeStyle.CLASSIC -> base
        AppThemeStyle.FOREST -> base.copy(
            primary = if (darkTheme) Color(0xFF8EDDB5) else Color(0xFF207A4C),
            secondary = if (darkTheme) Color(0xFFB7D99A) else Color(0xFF5C7F2B),
            primaryContainer = if (darkTheme) Color(0xFF1F3A2D) else Color(0xFFDDF4E9),
            secondaryContainer = if (darkTheme) Color(0xFF2B3721) else Color(0xFFE9F4D8)
        )
        AppThemeStyle.SUNSET -> base.copy(
            primary = if (darkTheme) Color(0xFFFFB08A) else Color(0xFFC65332),
            secondary = if (darkTheme) Color(0xFFFFD166) else Color(0xFF8A5A00),
            primaryContainer = if (darkTheme) Color(0xFF4A2B22) else Color(0xFFFFE1D3),
            secondaryContainer = if (darkTheme) Color(0xFF3D3016) else Color(0xFFFFEDC2)
        )
        AppThemeStyle.COSMOS -> base.copy(
            primary = if (darkTheme) Color(0xFFB9A7FF) else Color(0xFF5A4ACB),
            secondary = if (darkTheme) Color(0xFF8BDDF0) else Color(0xFF087B91),
            primaryContainer = if (darkTheme) Color(0xFF302A55) else Color(0xFFE6E0FF),
            secondaryContainer = if (darkTheme) Color(0xFF183B44) else Color(0xFFD5F5FB)
        )
        AppThemeStyle.OCEAN -> base.copy(
            primary = if (darkTheme) Color(0xFF7CC7FF) else Color(0xFF086CA8),
            secondary = if (darkTheme) Color(0xFF74E4D1) else Color(0xFF00796B),
            primaryContainer = if (darkTheme) Color(0xFF12364F) else Color(0xFFD7ECFF),
            secondaryContainer = if (darkTheme) Color(0xFF163E39) else Color(0xFFD0F4EC)
        )
        AppThemeStyle.SAKURA -> base.copy(
            primary = if (darkTheme) Color(0xFFFFA8C7) else Color(0xFFB83268),
            secondary = if (darkTheme) Color(0xFFFFD6A5) else Color(0xFF9A5A00),
            primaryContainer = if (darkTheme) Color(0xFF4A2334) else Color(0xFFFFD9E6),
            secondaryContainer = if (darkTheme) Color(0xFF3F301E) else Color(0xFFFFEBCF)
        )
        AppThemeStyle.GRAPHITE -> base.copy(
            primary = if (darkTheme) Color(0xFFE1E7EF) else Color(0xFF3F4A56),
            secondary = if (darkTheme) Color(0xFFB5C2D0) else Color(0xFF66717F),
            primaryContainer = if (darkTheme) Color(0xFF303844) else Color(0xFFE4E8EE),
            secondaryContainer = if (darkTheme) Color(0xFF252C34) else Color(0xFFD9DEE5)
        )
    }
}
