package com.mim.guruapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mim.guruapp.R

private val MimFontFamily = FontFamily(
  Font(R.font.poppins_regular, FontWeight.Normal),
  Font(R.font.poppins_semibold, FontWeight.SemiBold),
  Font(R.font.poppins_bold, FontWeight.Bold),
  Font(R.font.poppins_bold, FontWeight.ExtraBold)
)

val Typography = Typography(
  headlineMedium = TextStyle(
    fontFamily = MimFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 30.sp,
    lineHeight = 34.sp
  ),
  headlineSmall = TextStyle(
    fontFamily = MimFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 24.sp,
    lineHeight = 29.sp
  ),
  titleMedium = TextStyle(
    fontFamily = MimFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 22.sp,
    lineHeight = 26.sp
  ),
  titleSmall = TextStyle(
    fontFamily = MimFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 20.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = MimFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 22.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = MimFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp
  ),
  bodySmall = TextStyle(
    fontFamily = MimFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp
  ),
  labelLarge = TextStyle(
    fontFamily = MimFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp
  ),
  labelMedium = TextStyle(
    fontFamily = MimFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 16.sp
  ),
  labelSmall = TextStyle(
    fontFamily = MimFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
    lineHeight = 15.sp
  )
)
