package com.martinosama.habittracker.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    primaryContainer = White,
    secondary = SecondaryGreen,
    secondaryContainer  = White,
    background = White,
    surface = White,
    surfaceVariant = White,
    tertiaryContainer = White,
    onPrimary = White,
    onPrimaryContainer = Black,
    onSecondary = Black,
    onSecondaryContainer= Black,
    onBackground = Black,
    onSurface = Black,
    onTertiaryContainer = Black,
)

@Composable
fun HabitTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}