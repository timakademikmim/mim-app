package com.mim.guruapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mim.guruapp.APP_ROLE_ADMIN
import com.mim.guruapp.appRoleLabel
import com.mim.guruapp.ui.components.GlassCard
import com.mim.guruapp.ui.components.LogoBackground
import com.mim.guruapp.ui.i18n.t

@Composable
fun RolePickerScreen(
  teacherName: String,
  activeRole: String,
  roles: List<String>,
  onSelectRole: (String) -> Unit,
  onLogoutClick: () -> Unit
) {
  Box(modifier = Modifier.fillMaxSize()) {
    LogoBackground()

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 32.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      GlassCard(
        modifier = Modifier.widthIn(max = 460.dp)
      ) {
        Text(
          text = t("Pilih Role"),
          style = MaterialTheme.typography.headlineMedium,
          color = Color.White,
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
        Text(
          text = t("Akun ini memiliki lebih dari satu role. Masuk sebagai:"),
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFE5E7EB),
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
        )
        if (teacherName.isNotBlank()) {
          Text(
            text = teacherName,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.92f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 12.dp)
          )
        }

        Column(
          modifier = Modifier.padding(top = 22.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          roles.forEach { role ->
            RoleChoiceCard(
              role = role,
              selected = role == activeRole,
              onClick = { onSelectRole(role) }
            )
          }
        }
        Text(
          text = t("Logout"),
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFD1FAE5),
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .clickable(onClick = onLogoutClick)
        )
      }
    }
  }
}

@Composable
private fun RoleChoiceCard(
  role: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  val roleLabel = appRoleLabel(role)
  val icon = roleIcon(role)
  val tint = if (selected) Color(0xFF86EFAC) else Color.White.copy(alpha = 0.88f)
  val borderColor = if (selected) Color(0xFF86EFAC).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.22f)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(Color.White.copy(alpha = if (selected) 0.22f else 0.14f))
      .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(CircleShape)
        .background(Color.White.copy(alpha = 0.16f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(22.dp)
      )
    }
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(start = 12.dp)
    ) {
      Text(
        text = t(roleLabel),
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = t(roleSubtitle(role)),
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFE5E7EB),
        modifier = Modifier.padding(top = 3.dp)
      )
    }
    Icon(
      imageVector = Icons.Outlined.ChevronRight,
      contentDescription = null,
      tint = tint,
      modifier = Modifier.size(22.dp)
    )
  }
}

private fun roleIcon(role: String): ImageVector {
  return if (role == APP_ROLE_ADMIN) Icons.Outlined.AdminPanelSettings else Icons.Outlined.School
}

private fun roleSubtitle(role: String): String {
  return if (role == APP_ROLE_ADMIN) {
    "Dashboard dan profil admin Android."
  } else {
    "Dashboard, kelas, mapel, dan fitur guru."
  }
}
