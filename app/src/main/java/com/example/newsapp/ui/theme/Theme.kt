package com.example.newsapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = BluePrimaryLight,
    onPrimaryContainer = BluePrimaryDark,
    secondary = BluePrimaryDark,
    onSecondary = Color.White,
    secondaryContainer = BluePrimaryLight,
    onSecondaryContainer = BluePrimaryDark,
    background = SurfaceBackground,
    onBackground = TextPrimary,
    surface = SurfaceContainer,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = TextSecondary,
    outline = Divider
)

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryLight,
    onPrimary = TextPrimary,
    primaryContainer = BluePrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = BluePrimaryLight,
    onSecondary = TextPrimary,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5F5),
    outline = Color(0xFF475569)
)

@Composable
fun NewsAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
