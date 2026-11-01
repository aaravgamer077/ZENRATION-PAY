package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZenrationDarkColorScheme =
  darkColorScheme(
    primary = ZenElectricBlue,
    onPrimary = Color(0xFF001F26),
    primaryContainer = ZenDeepPurple,
    onPrimaryContainer = ZenTextPrimary,
    secondary = ZenRoyalPurple,
    onSecondary = Color.White,
    secondaryContainer = ZenSurfaceElevated,
    onSecondaryContainer = ZenPurpleGlow,
    tertiary = ZenSimAmber,
    onTertiary = Color(0xFF261900),
    background = ZenBlack,
    onBackground = ZenTextPrimary,
    surface = ZenSurfaceDark,
    onSurface = ZenTextPrimary,
    surfaceVariant = ZenSurfaceElevated,
    onSurfaceVariant = ZenTextSecondary,
    outline = ZenCardBorder,
    error = ZenErrorRed,
    onError = Color.White,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = ZenrationDarkColorScheme,
    typography = Typography,
    content = content,
  )
}
