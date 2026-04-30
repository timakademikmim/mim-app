package com.mim.guruapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SubtleInk

@Composable
fun SavingOverlay(
  visible: Boolean,
  title: String = "Menyimpan perubahan...",
  subtitle: String = "Mohon tunggu sebentar",
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(durationMillis = 160)),
    exit = fadeOut(tween(durationMillis = 130)),
    modifier = modifier.fillMaxSize()
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0x660F172A))
        .pointerInput(Unit) {
          awaitPointerEventScope {
            while (true) {
              val event = awaitPointerEvent()
              event.changes.forEach { it.consume() }
            }
          }
        },
      contentAlignment = Alignment.Center
    ) {
      AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(durationMillis = 180)) + scaleIn(tween(durationMillis = 180), initialScale = 0.9f),
        exit = fadeOut(tween(durationMillis = 120)) + scaleOut(tween(durationMillis = 120), targetScale = 0.94f)
      ) {
        Column(
          modifier = Modifier
            .shadow(18.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x240F172A), spotColor = Color(0x240F172A))
            .clip(RoundedCornerShape(26.dp))
            .background(CardBackground.copy(alpha = 0.96f))
            .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(26.dp))
            .padding(horizontal = 24.dp, vertical = 22.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          CircularProgressIndicator(
            color = PrimaryBlue,
            strokeWidth = 3.dp,
            modifier = Modifier.size(34.dp)
          )
          Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = SubtleInk,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}
