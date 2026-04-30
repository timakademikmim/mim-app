package com.mim.guruapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.model.AppNotification
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SubtleInk
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max

enum class NotificationFilter(val label: String, val days: Int) {
  Today("Hari ini", 0),
  ThreeDays("3 Hari", 3),
  SevenDays("7 Hari", 7)
}

@Composable
fun NotificationPopup(
  notifications: List<AppNotification>,
  onDismiss: () -> Unit,
  onMarkAllAsRead: () -> Unit,
  onNotificationClick: (AppNotification) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedFilter by rememberSaveable { mutableStateOf(NotificationFilter.Today) }
  val filteredNotifications = filterNotifications(notifications, selectedFilter)

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0x660F172A))
      .clickable(onClick = onDismiss),
    contentAlignment = Alignment.TopEnd
  ) {
    Column(
      modifier = Modifier
        .statusBarsPadding()
        .padding(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 16.dp)
        .widthIn(max = 420.dp)
        .fillMaxWidth()
        .heightIn(max = 560.dp)
        .shadow(18.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x220F172A), spotColor = Color(0x220F172A))
        .background(Color.White.copy(alpha = 0.96f), RoundedCornerShape(24.dp))
        .border(1.dp, CardBorder.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
        .padding(18.dp)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {}
        ),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      NotificationHeader(
        hasUnread = notifications.any { !it.isRead },
        onMarkAllAsRead = onMarkAllAsRead
      )

      NotificationTabs(
        selectedFilter = selectedFilter,
        onFilterSelected = { selectedFilter = it }
      )

      if (filteredNotifications.isEmpty()) {
        EmptyNotificationState()
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredNotifications, key = { it.id }) { notification ->
            NotificationItem(
              notification = notification,
              onClick = { onNotificationClick(notification) }
            )
          }
        }
      }
    }
  }
}

@Composable
fun NotificationHeader(
  hasUnread: Boolean,
  onMarkAllAsRead: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "Notifikasi",
      style = MaterialTheme.typography.titleLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = "Tandai semua dibaca",
      style = MaterialTheme.typography.bodySmall,
      color = if (hasUnread) MaterialTheme.colorScheme.primary else SubtleInk.copy(alpha = 0.6f),
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.clickable(enabled = hasUnread, onClick = onMarkAllAsRead)
    )
  }
}

@Composable
fun NotificationTabs(
  selectedFilter: NotificationFilter,
  onFilterSelected: (NotificationFilter) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFFF4F7FB), RoundedCornerShape(18.dp))
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    NotificationFilter.entries.forEach { filter ->
      val isSelected = filter == selectedFilter
      Box(
        modifier = Modifier
          .weight(1f)
          .background(
            if (isSelected) Color(0xFFDCE8FF) else Color.Transparent,
            RoundedCornerShape(14.dp)
          )
          .clickable { onFilterSelected(filter) }
          .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = filter.label,
          style = MaterialTheme.typography.bodyMedium,
          color = if (isSelected) PrimaryBlueDark else SubtleInk,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
      }
    }
  }
}

@Composable
fun NotificationItem(
  notification: AppNotification,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        if (notification.isRead) Color(0xFFF8FAFC) else Color(0xFFEFF6FF),
        RoundedCornerShape(18.dp)
      )
      .border(
        1.dp,
        if (notification.isRead) CardBorder.copy(alpha = 0.65f) else Color(0xFFBFDBFE),
        RoundedCornerShape(18.dp)
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.Top
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .background(notificationIconTint(notification.kind).copy(alpha = 0.16f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = notificationIcon(notification.kind),
        contentDescription = null,
        tint = notificationIconTint(notification.kind),
        modifier = Modifier.size(20.dp)
      )
    }

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      if (notification.typeLabel.isNotBlank()) {
        Text(
          text = notification.typeLabel,
          style = MaterialTheme.typography.labelMedium,
          color = notificationIconTint(notification.kind),
          fontWeight = FontWeight.SemiBold
        )
      }
      Text(
        text = notification.title,
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.Bold
      )
      if (notification.metaLabel.isNotBlank()) {
        Text(
          text = notification.metaLabel,
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk.copy(alpha = 0.92f)
        )
      }
      if (notification.description.isNotBlank()) {
        Text(
          text = notification.description,
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
    }

    Column(
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = notification.timeLabel,
        style = MaterialTheme.typography.labelMedium,
        color = SubtleInk
      )
      if (!notification.isRead) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .background(Color(0xFF2563EB), CircleShape)
        )
      }
    }
  }
}

@Composable
private fun EmptyNotificationState() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFFF8FAFC), RoundedCornerShape(18.dp))
      .border(1.dp, CardBorder.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
      .padding(18.dp)
  ) {
    Text(
      text = "Belum ada notifikasi pada rentang waktu ini.",
      style = MaterialTheme.typography.bodyMedium,
      color = SubtleInk
    )
  }
}

private fun filterNotifications(
  notifications: List<AppNotification>,
  filter: NotificationFilter
): List<AppNotification> {
  val zoneId = ZoneId.systemDefault()
  val today = LocalDate.now(zoneId)
  return notifications
    .sortedByDescending { it.createdAtMillis }
    .filter { notification ->
      val date = Instant.ofEpochMilli(notification.createdAtMillis).atZone(zoneId).toLocalDate()
      when (filter) {
        NotificationFilter.Today -> date == today
        NotificationFilter.ThreeDays -> {
          val days = max(0, today.toEpochDay().toInt() - date.toEpochDay().toInt())
          days <= 2
        }
        NotificationFilter.SevenDays -> {
          val days = max(0, today.toEpochDay().toInt() - date.toEpochDay().toInt())
          days <= 6
        }
      }
    }
}

private fun notificationIcon(kind: String): ImageVector {
  return when (kind) {
    "success" -> Icons.Outlined.CheckCircle
    "attendance" -> Icons.Outlined.AssignmentTurnedIn
    "calendar" -> Icons.Outlined.CalendarMonth
    "academic_holiday" -> Icons.Outlined.CalendarMonth
    "all_holiday" -> Icons.Outlined.CalendarMonth
    "tahfizh_holiday" -> Icons.Outlined.CalendarMonth
    "teaching" -> Icons.AutoMirrored.Outlined.MenuBook
    "approval" -> Icons.Outlined.AssignmentTurnedIn
    "delegation" -> Icons.Outlined.AssignmentTurnedIn
    "material" -> Icons.AutoMirrored.Outlined.MenuBook
    "sync" -> Icons.Outlined.Sync
    else -> Icons.Outlined.NotificationsNone
  }
}

@Composable
private fun notificationIconTint(kind: String): Color {
  return when (kind) {
    "success" -> Color(0xFF16A34A)
    "attendance" -> Color(0xFF2563EB)
    "calendar" -> Color(0xFFD97706)
    "academic_holiday" -> Color(0xFFDC2626)
    "all_holiday" -> Color(0xFFB45309)
    "tahfizh_holiday" -> Color(0xFF0891B2)
    "teaching" -> Color(0xFF2563EB)
    "approval" -> Color(0xFF7C3AED)
    "delegation" -> Color(0xFF0F766E)
    "material" -> Color(0xFF7C3AED)
    "sync" -> Color(0xFF0891B2)
    else -> PrimaryBlueDark
  }
}
