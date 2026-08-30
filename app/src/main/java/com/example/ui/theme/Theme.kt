package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SlateDarkPrimary,
    secondary = SlateDarkSecondary,
    tertiary = SlateDarkTertiary,
    background = SlateDarkBackground,
    surface = SlateDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = RawTextPrimaryDark,
    onSurface = RawTextPrimaryDark,
    surfaceVariant = SlateDarkCard,
    onSurfaceVariant = RawTextSecondaryDark,
    outline = RawBorderColorDark
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00C853),
    secondary = Color(0xFF0288D1),
    tertiary = Color(0xFFF57C00),
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
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
