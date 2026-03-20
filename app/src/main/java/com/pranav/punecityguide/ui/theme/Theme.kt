package com.pranav.punecityguide.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8), // Indigo 400
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFFACC15), // Yellow 400 (Vibrant Contrast)
    onSecondary = Color(0xFF422006),
    secondaryContainer = Color(0xFF713F12),
    onSecondaryContainer = Color(0xFFFEF9C3),
    tertiary = Color(0xFF34D399), // Emerald 400
    onTertiary = Color(0xFF064E3B),
    background = Color(0xFF020617), // Slate 950
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0F172A), // Slate 900
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B), // Slate 800
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = Color(0xFFF87171),
    outline = Color(0xFF475569)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5), // Indigo 600
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFFEA580C), // Orange 600
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEDD5),
    onSecondaryContainer = Color(0xFF7C2D12),
    tertiary = Color(0xFF059669), // Emerald 600
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC), // Slate 50
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9), // Slate 100
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFEF4444),
    outline = Color(0xFF94A3B8)
)

@Composable
fun PuneCityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic color to ensure our custom premium palette shines
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography, // Ensure standard typography is available or use default
        content = content
    )
}