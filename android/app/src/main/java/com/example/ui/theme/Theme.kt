package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BunkIQColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = CanvasDark,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = TextPrimary,
    secondary = Emerald,
    onSecondary = CanvasDark,
    secondaryContainer = EmeraldMuted,
    onSecondaryContainer = EmeraldGlow,
    tertiary = Ruby,
    onTertiary = TextPrimary,
    tertiaryContainer = RubyMuted,
    onTertiaryContainer = RubyGlow,
    background = CanvasDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderSubtle,
    error = Ruby,
    onError = TextPrimary
)

@Composable
fun BunkIQTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BunkIQColorScheme,
        typography = Typography,
        content = content
    )
}

