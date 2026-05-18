package com.mim.guruapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
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

internal val DarkPrimaryBlue = Color(0xFF93C5FD)
internal val DarkPrimaryBlueDark = Color(0xFFE2E8F0)
internal val DarkHighlightCard = Color(0xFF38BDF8)
internal val DarkSuccessTint = Color(0xFF34D399)
internal val DarkWarmAccent = Color(0xFFFBBF24)
internal val DarkAppBackground = Color(0xFF07111F)
internal val DarkLoginBackground = Color(0xFF08111F)
internal val DarkCardBackground = Color(0xFF111827)
internal val DarkCardBorder = Color(0xFF334155)
internal val DarkCardGradientStart = Color(0xFF111827)
internal val DarkCardGradientEnd = Color(0xFF0F172A)
internal val DarkSoftPanel = Color(0xFF172033)
internal val DarkSubtleInk = Color(0xFFCBD5E1)
internal val DarkLoginButtonYellow = Color(0xFFE2E85D)
internal val DarkLoginFieldFill = Color(0xFF172A46)

internal data class MimGuruColors(
  val appBackground: Color,
  val loginBackground: Color,
  val primaryBlue: Color,
  val primaryBlueDark: Color,
  val highlightCard: Color,
  val successTint: Color,
  val cardBackground: Color,
  val cardBorder: Color,
  val warmAccent: Color,
  val cardGradientStart: Color,
  val cardGradientEnd: Color,
  val softPanel: Color,
  val subtleInk: Color,
  val loginButtonYellow: Color,
  val loginFieldFill: Color
)

internal val LightMimGuruColors = MimGuruColors(
  appBackground = LightAppBackground,
  loginBackground = LightLoginBackground,
  primaryBlue = LightPrimaryBlue,
  primaryBlueDark = LightPrimaryBlueDark,
  highlightCard = LightHighlightCard,
  successTint = LightSuccessTint,
  cardBackground = LightCardBackground,
  cardBorder = LightCardBorder,
  warmAccent = LightWarmAccent,
  cardGradientStart = LightCardGradientStart,
  cardGradientEnd = LightCardGradientEnd,
  softPanel = LightSoftPanel,
  subtleInk = LightSubtleInk,
  loginButtonYellow = LightLoginButtonYellow,
  loginFieldFill = LightLoginFieldFill
)

internal val DarkMimGuruColors = MimGuruColors(
  appBackground = DarkAppBackground,
  loginBackground = DarkLoginBackground,
  primaryBlue = DarkPrimaryBlue,
  primaryBlueDark = DarkPrimaryBlueDark,
  highlightCard = DarkHighlightCard,
  successTint = DarkSuccessTint,
  cardBackground = DarkCardBackground,
  cardBorder = DarkCardBorder,
  warmAccent = DarkWarmAccent,
  cardGradientStart = DarkCardGradientStart,
  cardGradientEnd = DarkCardGradientEnd,
  softPanel = DarkSoftPanel,
  subtleInk = DarkSubtleInk,
  loginButtonYellow = DarkLoginButtonYellow,
  loginFieldFill = DarkLoginFieldFill
)

internal val LocalMimGuruColors = staticCompositionLocalOf { LightMimGuruColors }

val AppBackground: Color
  @Composable
  get() = LocalMimGuruColors.current.appBackground

val LoginBackground: Color
  @Composable
  get() = LocalMimGuruColors.current.loginBackground

val PrimaryBlue: Color
  @Composable
  get() = LocalMimGuruColors.current.primaryBlue

val PrimaryBlueDark: Color
  @Composable
  get() = LocalMimGuruColors.current.primaryBlueDark

val HighlightCard: Color
  @Composable
  get() = LocalMimGuruColors.current.highlightCard

val SuccessTint: Color
  @Composable
  get() = LocalMimGuruColors.current.successTint

val CardBackground: Color
  @Composable
  get() = LocalMimGuruColors.current.cardBackground

val CardBorder: Color
  @Composable
  get() = LocalMimGuruColors.current.cardBorder

val WarmAccent: Color
  @Composable
  get() = LocalMimGuruColors.current.warmAccent

val CardGradientStart: Color
  @Composable
  get() = LocalMimGuruColors.current.cardGradientStart

val CardGradientEnd: Color
  @Composable
  get() = LocalMimGuruColors.current.cardGradientEnd

val SoftPanel: Color
  @Composable
  get() = LocalMimGuruColors.current.softPanel

val SubtleInk: Color
  @Composable
  get() = LocalMimGuruColors.current.subtleInk

val LoginButtonYellow: Color
  @Composable
  get() = LocalMimGuruColors.current.loginButtonYellow

val LoginFieldFill: Color
  @Composable
  get() = LocalMimGuruColors.current.loginFieldFill
