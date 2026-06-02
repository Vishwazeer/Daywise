package com.example.daywise.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Green10,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = Tan60,
    onSecondary = Tan10,
    secondaryContainer = Tan20,
    onSecondaryContainer = Tan80,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral20,
    onSurface = Neutral90,
    surfaceVariant = Neutral30,
    onSurfaceVariant = Neutral80,
    error = ErrorRedDark,
    onError = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = Green30,
    onPrimary = Color.White,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = Tan40,
    onSecondary = Color.White,
    secondaryContainer = Tan90,
    onSecondaryContainer = Tan10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Color.White,
    onSurface = Neutral10,
    surfaceVariant = Neutral95,
    onSurfaceVariant = Neutral20,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun DaywiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
