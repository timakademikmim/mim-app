package com.mim.guruapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
  primary = LightPrimaryBlue,
  onPrimary = androidx.compose.ui.graphics.Color.White,
  secondary = LightHighlightCard,
  background = LightAppBackground,
  surface = LightCardBackground,
  surfaceVariant = LightSoftPanel,
  outline = LightCardBorder,
  onSurface = LightPrimaryBlueDark,
  onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF475569)
)

private val DarkColors = darkColorScheme(
  primary = DarkPrimaryBlue,
  onPrimary = Color(0xFF07111F),
  secondary = DarkHighlightCard,
  background = DarkAppBackground,
  surface = DarkCardBackground,
  surfaceVariant = DarkSoftPanel,
  outline = DarkCardBorder,
  onSurface = DarkPrimaryBlueDark,
  onSurfaceVariant = DarkSubtleInk
)

@Composable
fun MimGuruTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
    typography = Typography,
    content = content
  )
}
