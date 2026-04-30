package com.mim.guruapp.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mim.guruapp.R
import com.mim.guruapp.ui.theme.LoginBackground
import com.mim.guruapp.ui.theme.MimGuruTheme
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SubtleInk

@Composable
fun WelcomeScreen(
  title: String,
  message: String
) {
  val transition = rememberInfiniteTransition(label = "welcome-loading")
  val pulse = transition.animateFloat(
    initialValue = 0.88f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(900),
      repeatMode = RepeatMode.Reverse
    ),
    label = "welcome-pulse"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        brush = Brush.radialGradient(
          colors = listOf(Color(0x1F2B8CFF), LoginBackground),
          radius = 720f
        )
      )
  ) {
    Image(
      painter = painterResource(id = R.drawable.logo_mim_full),
      contentDescription = null,
      contentScale = ContentScale.Fit,
      modifier = Modifier
        .align(Alignment.Center)
        .padding(24.dp)
        .alpha(0.06f)
    )

    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Image(
        painter = painterResource(id = R.drawable.logo_mim_android),
        contentDescription = "Logo MIM",
        modifier = Modifier
          .size(116.dp)
          .alpha(pulse.value)
      )
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = PrimaryBlueDark,
        modifier = Modifier.padding(top = 20.dp)
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = SubtleInk,
        modifier = Modifier.padding(top = 10.dp, start = 32.dp, end = 32.dp)
      )
    }
  }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun WelcomeScreenPreview() {
  MimGuruTheme {
    WelcomeScreen(
      title = "Portal Akademik",
      message = "Memeriksa sesi login dan menyiapkan data lokal..."
    )
  }
}
