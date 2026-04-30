package com.mim.guruapp.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mim.guruapp.R
import com.mim.guruapp.ui.theme.PrimaryBlueDark

@Composable
fun LogoBackground() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(Color(0xFF0D1F18), Color(0xFF143328), Color(0xFF09130F))
        )
      )
  ) {
    Image(
      painter = painterResource(id = R.drawable.logo_mim_full),
      contentDescription = null,
      contentScale = ContentScale.Fit,
      modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)
        .graphicsLayer {
          alpha = 0.16f
        }
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          brush = Brush.radialGradient(
            colors = listOf(Color(0x40A3E635), Color.Transparent),
            radius = 880f
          )
        )
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0x66040B08))
    )
  }
}

@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  val shape = RoundedCornerShape(28.dp)
  Box(
    modifier = modifier
      .widthIn(max = 460.dp)
      .fillMaxWidth()
      .shadow(
        elevation = 30.dp,
        shape = shape,
        ambientColor = Color(0x33000000),
        spotColor = Color(0x33000000)
      )
      .clip(shape)
  ) {
    Image(
      painter = painterResource(id = R.drawable.logo_mim_full),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .matchParentSize()
        .graphicsLayer {
          alpha = 0.22f
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            renderEffect = RenderEffect
              .createBlurEffect(44f, 44f, Shader.TileMode.CLAMP)
              .asComposeRenderEffect()
          }
        }
    )

    Box(
      modifier = Modifier
        .matchParentSize()
        .background(Color.White.copy(alpha = 0.18f))
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(Color(0x3AFFFFFF), Color(0x18FFFFFF))
          )
        )
        .background(
          brush = Brush.radialGradient(
            colors = listOf(Color(0x26BBF7D0), Color.Transparent),
            radius = 360f
          )
        )
        .border(1.dp, Color.White.copy(alpha = 0.34f), shape)
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 28.dp),
      content = content
    )
  }
}

@Composable
fun LoginHeader(
  subtitle: String
) {
  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center
  ) {
    Image(
      painter = painterResource(id = R.drawable.logo_mim_full),
      contentDescription = "Logo MIM",
      modifier = Modifier.size(82.dp)
    )
  }
  Text(
    text = "Login",
    style = MaterialTheme.typography.headlineMedium,
    color = Color.White,
    fontWeight = FontWeight.ExtraBold,
    textAlign = TextAlign.Center,
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 16.dp)
  )
  Text(
    text = subtitle,
    style = MaterialTheme.typography.bodyMedium,
    color = Color(0xFFE5E7EB),
    textAlign = TextAlign.Center,
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp)
  )
}

@Composable
fun LoginTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String,
  leadingIcon: ImageVector,
  modifier: Modifier = Modifier,
  isPassword: Boolean = false,
  passwordVisible: Boolean = false,
  onPasswordVisibilityToggle: (() -> Unit)? = null
) {
  val shape = RoundedCornerShape(18.dp)
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = Color.White.copy(alpha = 0.92f),
      modifier = Modifier.padding(bottom = 8.dp)
    )
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      visualTransformation = when {
        isPassword && !passwordVisible -> PasswordVisualTransformation()
        else -> VisualTransformation.None
      },
      textStyle = MaterialTheme.typography.bodyLarge.merge(
        TextStyle(color = Color.White)
      ),
      modifier = Modifier.fillMaxWidth(),
      decorationBox = { innerTextField ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.16f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.20f), shape)
            .padding(horizontal = 16.dp, vertical = 15.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.82f),
            modifier = Modifier.size(20.dp)
          )
          Box(modifier = Modifier.weight(1f)) {
            if (value.isBlank()) {
              Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.54f)
              )
            }
            innerTextField()
          }
          if (isPassword && onPasswordVisibilityToggle != null) {
            Icon(
              imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
              contentDescription = if (passwordVisible) "Sembunyikan password" else "Lihat password",
              tint = Color.White.copy(alpha = 0.82f),
              modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onPasswordVisibilityToggle)
            )
          }
        }
      }
    )
  }
}

@Composable
fun RememberMeRow(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  onUseDemoAccount: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically
    ) {
      Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = CheckboxDefaults.colors(
          checkedColor = Color(0xFF22C55E),
          uncheckedColor = Color.White.copy(alpha = 0.72f),
          checkmarkColor = PrimaryBlueDark
        )
      )
      Text(
        text = "Remember me",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.88f)
      )
    }

    Text(
      text = "Isi contoh ID",
      style = MaterialTheme.typography.bodySmall,
      color = Color(0xFFD1FAE5),
      modifier = Modifier.clickable(onClick = onUseDemoAccount)
    )
  }
}

@Composable
fun GradientLoginButton(
  text: String,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
  val isPressed = interactionSource.collectIsPressedAsState()
  val scale = animateFloatAsState(
    targetValue = if (isPressed.value) 0.985f else 1f,
    label = "login-button-scale"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(56.dp)
      .scale(scale.value)
      .clip(RoundedCornerShape(18.dp))
      .background(
        brush = Brush.horizontalGradient(
          colors = listOf(Color(0xFF16A34A), Color(0xFF22C55E), Color(0xFF34D399))
        )
      )
      .clickable(
        enabled = enabled,
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.titleSmall,
      color = Color.White,
      fontWeight = FontWeight.Bold
    )
  }
}

val UsernameIcon = Icons.Outlined.Person
val PasswordIcon = Icons.Outlined.Lock
