package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SiriColorScheme = darkColorScheme(
  primary = SiriCyan,
  onPrimary = Color.Black,
  primaryContainer = SiriSurfaceVariant,
  onPrimaryContainer = SiriCyan,
  secondary = SiriMagenta,
  onSecondary = Color.White,
  secondaryContainer = SiriSurfaceElevated,
  onSecondaryContainer = SiriMagenta,
  tertiary = SiriViolet,
  onTertiary = Color.White,
  background = SiriBackground,
  onBackground = SiriTextPrimary,
  surface = SiriSurface,
  onSurface = SiriTextPrimary,
  surfaceVariant = SiriSurfaceVariant,
  onSurfaceVariant = SiriTextSecondary,
  outline = SiriBorder,
  outlineVariant = SiriBorderGlow
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = SiriColorScheme,
    typography = Typography,
    content = content
  )
}

