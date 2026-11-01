package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = DarkBackground,
    primaryContainer = ElectricCyanDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = NeonPurple,
    onSecondary = TextPrimaryDark,
    secondaryContainer = NeonPurpleDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = AccentPink,
    onTertiary = TextPrimaryDark,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkSurfaceBorder,
    error = AccentPink,
    onError = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricCyanDark,
    onPrimary = LightSurface,
    primaryContainer = ElectricCyanLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = NeonPurpleDark,
    onSecondary = LightSurface,
    secondaryContainer = NeonPurpleLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = AccentPink,
    onTertiary = LightSurface,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightSurfaceBorder,
    error = AccentPink,
    onError = LightSurface
)

@Composable
fun AiCreatorStudioTheme(
    darkTheme: Boolean = true, // Default to premium dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
