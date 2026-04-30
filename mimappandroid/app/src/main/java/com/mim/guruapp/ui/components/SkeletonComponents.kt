package com.mim.guruapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mim.guruapp.ui.theme.CardBorder

@Composable
fun rememberSkeletonBrush(): Brush {
  val transition = rememberInfiniteTransition(label = "skeleton-shimmer")
  val translate by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "skeleton-shimmer-translate"
  )
  val startX = (translate * 900f) - 320f
  return Brush.linearGradient(
    colors = listOf(
      Color(0xFFE5E7EB).copy(alpha = 0.78f),
      Color(0xFFF8FAFC).copy(alpha = 0.96f),
      Color(0xFFD7DBE2).copy(alpha = 0.82f)
    ),
    start = Offset(startX, 0f),
    end = Offset(startX + 320f, 120f)
  )
}

@Composable
fun SkeletonBlock(
  brush: Brush,
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(10.dp)
) {
  Box(
    modifier = modifier
      .clip(shape)
      .background(brush)
  )
}

@Composable
fun SkeletonContentCard(
  modifier: Modifier = Modifier,
  leadingSize: Dp = 40.dp,
  trailingSize: Dp = 38.dp,
  firstLineWidth: Float = 0.54f,
  secondLineWidth: Float = 0.88f,
  thirdLineWidth: Float? = null
) {
  val brush = rememberSkeletonBrush()
  Row(
    modifier = modifier
      .fillMaxWidth()
      .shadow(9.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(20.dp))
      .background(Color.White.copy(alpha = 0.82f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
      .padding(horizontal = 16.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    SkeletonBlock(
      brush = brush,
      modifier = Modifier.size(leadingSize),
      shape = RoundedCornerShape(14.dp)
    )
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      SkeletonBlock(
        brush = brush,
        modifier = Modifier
          .fillMaxWidth(firstLineWidth)
          .height(13.dp),
        shape = RoundedCornerShape(999.dp)
      )
      SkeletonBlock(
        brush = brush,
        modifier = Modifier
          .fillMaxWidth(secondLineWidth)
          .height(10.dp),
        shape = RoundedCornerShape(999.dp)
      )
      if (thirdLineWidth != null) {
        SkeletonBlock(
          brush = brush,
          modifier = Modifier
            .fillMaxWidth(thirdLineWidth)
            .height(10.dp),
          shape = RoundedCornerShape(999.dp)
        )
      }
    }
    SkeletonBlock(
      brush = brush,
      modifier = Modifier.size(trailingSize),
      shape = RoundedCornerShape(14.dp)
    )
  }
}
