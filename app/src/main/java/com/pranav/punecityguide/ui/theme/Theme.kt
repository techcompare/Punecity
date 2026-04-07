package com.pranav.punecityguide.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CostPilotCyan,
    onPrimary = CostPilotNavy,
    primaryContainer = Color(0xFF003D4D),
    onPrimaryContainer = Color(0xFFB8F0FF),
    secondary = CostPilotGold,
    onSecondary = Color(0xFF3D2E00),
    secondaryContainer = Color(0xFF594400),
    onSecondaryContainer = Color(0xFFFFE08A),
    tertiary = CostPilotSuccess,
    onTertiary = Color(0xFF003919),
    background = CostPilotNavy,
    onBackground = CostPilotSilver,
    surface = CostPilotNavyLight,
    onSurface = CostPilotSilver,
    surfaceVariant = Color(0xFF1A2744),
    onSurfaceVariant = Color(0xFFB0BEC5),
    error = CostPilotDanger,
    outline = Color(0xFF3A4F6A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0088AA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F5FF),
    onPrimaryContainer = Color(0xFF003544),
    secondary = Color(0xFF996D00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE08A),
    onSecondaryContainer = Color(0xFF3D2E00),
    tertiary = Color(0xFF007A3D),
    onTertiary = Color.White,
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF0A1628),
    surface = Color.White,
    onSurface = Color(0xFF0A1628),
    surfaceVariant = Color(0xFFECF0F6),
    onSurfaceVariant = Color(0xFF4A5568),
    error = Color(0xFFD32F2F),
    outline = Color(0xFFB0BEC5)
)

@Composable
fun PuneCityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}