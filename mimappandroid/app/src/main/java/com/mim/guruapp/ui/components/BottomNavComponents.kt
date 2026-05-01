package com.mim.guruapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.GuruSidebarDestination
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SubtleInk
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class BottomNavEntry(
  val destination: GuruSidebarDestination,
  val label: String,
  val icon: ImageVector
)

@Composable
fun BottomNavBar(
  items: List<BottomNavEntry>,
  selectedDestination: GuruSidebarDestination?,
  onSelect: (GuruSidebarDestination) -> Unit,
  modifier: Modifier = Modifier
) {
  var animatedSelectedDestination by rememberSaveable {
    mutableStateOf(selectedDestination)
  }
  var pendingNavigationJob by androidx.compose.runtime.remember { mutableStateOf<Job?>(null) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(selectedDestination) {
    animatedSelectedDestination = selectedDestination
  }

  Row(
    modifier = modifier
      .width(356.dp)
      .shadow(18.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x180F172A), spotColor = Color(0x180F172A))
      .clip(RoundedCornerShape(32.dp))
      .background(Color.White.copy(alpha = 0.96f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(32.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    items.forEach { item ->
      val isSelected = animatedSelectedDestination == item.destination
      BottomNavItem(
        item = item,
        selected = isSelected,
        onClick = {
          if (animatedSelectedDestination != item.destination) {
            animatedSelectedDestination = item.destination
          }
          pendingNavigationJob?.cancel()
          pendingNavigationJob = scope.launch {
            delay(280)
            onSelect(item.destination)
          }
        },
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
fun BottomNavItem(
  item: BottomNavEntry,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val containerColor = if (selected) PrimaryBlue.copy(alpha = 0.12f) else Color.Transparent
  val contentColor = if (selected) PrimaryBlue else SubtleInk.copy(alpha = 0.82f)

  Column(
    modifier = modifier
      .height(58.dp)
      .clip(RoundedCornerShape(24.dp))
      .background(containerColor)
      .clickable(onClick = onClick)
      .padding(horizontal = 4.dp, vertical = 7.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(22.dp)
    ) {
      Icon(
        imageVector = item.icon,
        contentDescription = item.label,
        tint = contentColor,
        modifier = Modifier.size(21.dp)
      )
    }
    Text(
      text = item.label,
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) PrimaryBlueDark else contentColor,
      fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center
    )
  }
}

fun buildGuruBottomNavItems(): List<BottomNavEntry> {
  return listOf(
    BottomNavEntry(
      destination = GuruSidebarDestination.Dashboard,
      label = "Dashboard",
      icon = Icons.Outlined.DashboardCustomize
    ),
    BottomNavEntry(
      destination = GuruSidebarDestination.Mapel,
      label = "Mapel",
      icon = Icons.AutoMirrored.Outlined.MenuBook
    ),
    BottomNavEntry(
      destination = GuruSidebarDestination.Jadwal,
      label = "Jadwal",
      icon = Icons.Outlined.Today
    ),
    BottomNavEntry(
      destination = GuruSidebarDestination.InputAbsensi,
      label = "Input",
      icon = Icons.Outlined.EditNote
    ),
    BottomNavEntry(
      destination = GuruSidebarDestination.Profil,
      label = "Profil",
      icon = Icons.Outlined.PersonOutline
    )
  )
}
