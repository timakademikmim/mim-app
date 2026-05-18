package com.mim.guruapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.isSystemInDarkTheme

enum class AppThemeMode(val code: String) {
  System("system"),
  Light("light"),
  Dark("dark");

  companion object {
    fun fromCode(code: String): AppThemeMode {
      return when (code.trim().lowercase()) {
        "light" -> Light
        "dark" -> Dark
        else -> System
      }
    }
  }
}

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
  onPrimary = androidx.compose.ui.graphics.Color(0xFF0F172A),
  secondary = DarkHighlightCard,
  background = DarkAppBackground,
  surface = DarkCardBackground,
  surfaceVariant = DarkSoftPanel,
  outline = DarkCardBorder,
  onSurface = DarkPrimaryBlueDark,
  onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCBD5E1)
)

@Composable
fun MimGuruTheme(
  themeMode: AppThemeMode = AppThemeMode.System,
  content: @Composable () -> Unit
) {
  val useDarkTheme = when (themeMode) {
    AppThemeMode.System -> isSystemInDarkTheme()
    AppThemeMode.Light -> false
    AppThemeMode.Dark -> true
  }
  CompositionLocalProvider(LocalMimGuruColors provides if (useDarkTheme) DarkMimGuruColors else LightMimGuruColors) {
    MaterialTheme(
      colorScheme = if (useDarkTheme) DarkColors else LightColors,
      typography = Typography,
      content = content
    )
  }
}
