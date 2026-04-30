package com.mim.guruapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val LightPrimaryBlue = Color(0xFF1D4ED8)
internal val LightPrimaryBlueDark = Color(0xFF0F172A)
internal val LightHighlightCard = Color(0xFF0EA5E9)
internal val LightSuccessTint = Color(0xFF059669)
internal val LightWarmAccent = Color(0xFFF59E0B)
internal val LightAppBackground = Color(0xFFF4F7FB)
internal val LightLoginBackground = Color(0xFFFFFFFF)
internal val LightCardBackground = Color(0xFFFFFFFF)
internal val LightCardBorder = Color(0xFFD9E1EE)
internal val LightCardGradientStart = Color(0xFFFEFEFF)
internal val LightCardGradientEnd = Color(0xFFF8FAFC)
internal val LightSoftPanel = Color(0xFFF8FAFC)
internal val LightSubtleInk = Color(0xFF475569)
internal val LightLoginButtonYellow = Color(0xFFD4D456)
internal val LightLoginFieldFill = Color(0xFFDBE7F8)

internal val DarkPrimaryBlue = Color(0xFF7AA2FF)
internal val DarkPrimaryBlueDark = Color(0xFFF8FAFC)
internal val DarkHighlightCard = Color(0xFF38BDF8)
internal val DarkSuccessTint = Color(0xFF34D399)
internal val DarkWarmAccent = Color(0xFFFBBF24)
internal val DarkAppBackground = Color(0xFF07111F)
internal val DarkLoginBackground = Color(0xFF0C1728)
internal val DarkCardBackground = Color(0xFF0F1C2E)
internal val DarkCardBorder = Color(0xFF253752)
internal val DarkCardGradientStart = Color(0xFF162236)
internal val DarkCardGradientEnd = Color(0xFF0E1A2A)
internal val DarkSoftPanel = Color(0xFF132238)
internal val DarkSubtleInk = Color(0xFF9AA9C0)
internal val DarkLoginButtonYellow = Color(0xFFE4DD63)
internal val DarkLoginFieldFill = Color(0xFF1B2A42)

val AppBackground: Color
  @Composable
  get() = if (isSystemInDarkTheme()) DarkAppBackground else LightAppBackground

val LoginBackground: Color
  @Composable
  get() = if (isSystemInDarkTheme()) DarkLoginBackground else LightLoginBackground

val PrimaryBlue: Color
  @Composable
  get() = if (isSystemInDarkTheme()) DarkPrimaryBlue else LightPrimaryBlue

val PrimaryBlueDark: Color
  @Composable
  get() = if (isSystemInDarkTheme()) DarkPrimaryBlueDark else LightPrimaryBlueDark

val HighlightCard: Color
  @Composable
  get() = if (isSystemInDarkTheme()) DarkHighlightCard else LightHighlightCard

val SuccessTint: Color
  @Composable
  get() = if (isSystemInDarkTheme()) DarkSuccessTint else LightSuccessTint

val CardBackground: Color
  @Composable
  get() = if (isSystemInDarkTheme()) DarkCardBackground else LightCardBackground

val CardBorder: Color
  @Composable
  get() = if (isSystemInDarkTheme()) DarkCardBorder else LightCardBorder

val WarmAccent: Color
  @Composable
  get() = if (isSystemInDarkTheme()) DarkWarmAccent else LightWarmAccent

val CardGradientStart: Color
  @Composable
  get() = if (isSystemInDarkTheme()) DarkCardGradientStart else LightCardGradientStart

val CardGradientEnd: Color
  @Composable
  get() = if (isSystemInDarkTheme()) DarkCardGradientEnd else LightCardGradientEnd

val SoftPanel: Color
  @Composable
  get() = if (isSystemInDarkTheme()) DarkSoftPanel else LightSoftPanel

val SubtleInk: Color
  @Composable
  get() = if (isSystemInDarkTheme()) DarkSubtleInk else LightSubtleInk

val LoginButtonYellow: Color
  @Composable
  get() = if (isSystemInDarkTheme()) DarkLoginButtonYellow else LightLoginButtonYellow

val LoginFieldFill: Color
  @Composable
  get() = if (isSystemInDarkTheme()) DarkLoginFieldFill else LightLoginFieldFill
