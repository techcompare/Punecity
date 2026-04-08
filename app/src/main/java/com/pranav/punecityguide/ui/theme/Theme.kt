package com.pranav.punecityguide.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PuneBuzzColorScheme = darkColorScheme(
    primary = BuzzPrimary,
    secondary = BuzzSecondary,
    tertiary = BuzzAccent,
    background = BuzzBackgroundStart,
    surface = BuzzCard,
    onPrimary = BuzzTextPrimary,
    onSecondary = BuzzTextPrimary,
    onBackground = BuzzTextPrimary,
    onSurface = BuzzTextPrimary,
)

@Composable
fun PuneBuzzTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PuneBuzzColorScheme,
        typography = AppTypography,
        content = content,
    )
}
