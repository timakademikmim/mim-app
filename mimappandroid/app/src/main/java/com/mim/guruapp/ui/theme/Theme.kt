package com.mim.guruapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun MimGuruTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = LightColors,
    typography = Typography,
    content = content
  )
}
