package com.mim.guruapp.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.DashboardCategory
import com.mim.guruapp.data.model.DashboardTask
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.CardGradientEnd
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SubtleInk
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenScaffold(
  currentDate: LocalDate,
  notificationCount: Int,
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onDateClick: () -> Unit,
  onNotificationClick: () -> Unit,
  onRefresh: () -> Unit,
  categories: List<DashboardCategory>,
  tasks: List<DashboardTask>,
  calendarEvents: List<CalendarEvent>,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategoryId by rememberSaveable { mutableStateOf("semua") }
  val normalizedQuery = searchQuery.trim().lowercase()
  val filteredTasks = remember(calendarEvents, selectedCategoryId, normalizedQuery, currentDate) {
    val filteredEvents = calendarEvents.filter { event ->
      val matchesCategory = when {
        selectedCategoryId == "semua" -> true
        else -> event.categoryKey == selectedCategoryId
      }
      val haystack = buildString {
        append(event.title)
        append(' ')
        append(event.timeLabel)
        append(' ')
        append(event.description)
      }.lowercase()
      val matchesSearch = normalizedQuery.isBlank() || haystack.contains(normalizedQuery)
      matchesCategory && matchesSearch
    }
    buildAgendaTasksForDashboard(filteredEvents)
  }

  Scaffold(
    modifier = modifier,
    containerColor = Color.Transparent,
    topBar = {
      TopBarCustom(
        currentDate = currentDate,
        notificationCount = notificationCount,
        onMenuClick = onMenuClick,
        onDateClick = onDateClick,
        onNotificationClick = onNotificationClick
      )
    }
  ) { innerPadding ->
    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = onRefresh,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .navigationBarsPadding()
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 2.dp)
      ) {
        SearchBar(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = "Cari agenda",
          modifier = Modifier.padding(top = 12.dp)
        )

        DashboardSectionHeader(
          title = "Categories",
          modifier = Modifier.padding(top = 20.dp)
        )

        LazyRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(end = 12.dp)
        ) {
          items(categories, key = { it.id }) { category ->
            CategoryCard(
              category = category,
              selected = category.id == selectedCategoryId,
              onClick = { selectedCategoryId = category.id }
            )
          }
        }

        DashboardSectionHeader(
          title = "Dashboard Agenda",
          modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)
        )

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentPadding = PaddingValues(bottom = 124.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          if (filteredTasks.isEmpty()) {
            item {
              EmptyAgendaState()
            }
          } else {
            items(filteredTasks, key = { it.id }) { task ->
              TaskCard(task = task)
            }
          }
        }
      }
    }
  }
}

private fun eventContainsDateForDashboard(
  event: CalendarEvent,
  date: LocalDate
): Boolean {
  val start = parseDashboardEventStart(event)
  val end = parseDashboardEventEnd(event)
  return !date.isBefore(start) && !date.isAfter(end)
}

@Composable
fun TopBarCustom(
  currentDate: LocalDate,
  notificationCount: Int,
  onMenuClick: () -> Unit,
  onDateClick: () -> Unit,
  onNotificationClick: () -> Unit
) {
  val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    GlassActionButton(
      icon = Icons.Outlined.Menu,
      contentDescription = "Open sidebar",
      onClick = onMenuClick
    )

    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onDateClick)
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = Icons.Outlined.CalendarMonth,
        contentDescription = null,
        tint = PrimaryBlueDark,
        modifier = Modifier.size(18.dp)
      )
      Text(
        text = formatter.format(currentDate),
        style = MaterialTheme.typography.bodyMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp)
      )
    }

    Box {
      GlassActionButton(
        icon = Icons.Outlined.NotificationsNone,
        contentDescription = "Notifications",
        onClick = onNotificationClick
      )
      if (notificationCount > 0) {
        Badge(
          modifier = Modifier.align(Alignment.TopEnd)
        ) {
          Text(notificationCount.toString())
        }
      }
    }
  }
}

@Composable
fun SearchBar(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(Color.White.copy(alpha = 0.88f), RoundedCornerShape(18.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Outlined.Search,
      contentDescription = null,
      tint = SubtleInk,
      modifier = Modifier.size(20.dp)
    )
    androidx.compose.foundation.text.BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      textStyle = MaterialTheme.typography.bodyMedium.copy(color = PrimaryBlueDark),
      modifier = Modifier
        .weight(1f)
        .padding(start = 12.dp),
      decorationBox = { innerTextField ->
        if (value.isBlank()) {
          Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8)
          )
        }
        innerTextField()
      }
    )
  }
}

@Composable
fun CategoryCard(
  category: DashboardCategory,
  selected: Boolean,
  onClick: () -> Unit
) {
  GlassColorCard(
    tint = parseHexColor(category.colorHex),
    modifier = Modifier
      .width(168.dp)
      .clickable(onClick = onClick),
    selected = selected
  ) {
    Text(
      text = category.title,
      style = MaterialTheme.typography.titleSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = category.subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      modifier = Modifier.padding(top = 8.dp)
    )
  }
}

@Composable
fun TaskCard(task: DashboardTask) {
  GlassColorCard(
    tint = parseHexColor(task.colorHex),
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = task.title,
      style = MaterialTheme.typography.titleSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = task.timeLabel,
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      modifier = Modifier.padding(top = 10.dp)
    )
  }
}

@Composable
private fun GlassColorCard(
  tint: Color,
  modifier: Modifier = Modifier,
  selected: Boolean = false,
  content: @Composable ColumnScope.() -> Unit
) {
  Box(
    modifier = modifier
      .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x120F172A), spotColor = Color(0x120F172A))
      .border(
        1.dp,
        if (selected) PrimaryBlueDark.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.40f),
        RoundedCornerShape(20.dp)
      )
  ) {
    GlassBackground(tint = tint)
    Column(
      modifier = Modifier.padding(16.dp),
      content = content
    )
  }
}

@Composable
private fun EmptyAgendaState() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(20.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
      .padding(16.dp)
  ) {
    Text(
      text = "Tidak ada agenda yang cocok dengan kategori atau pencarian ini.",
      style = MaterialTheme.typography.bodyMedium,
      color = SubtleInk
    )
  }
}

@Composable
private fun BoxScope.GlassBackground(tint: Color) {
  Box(
    modifier = Modifier
      .matchParentSize()
      .graphicsLayer {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          renderEffect = RenderEffect
            .createBlurEffect(18f, 18f, Shader.TileMode.CLAMP)
            .asComposeRenderEffect()
        }
      }
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(tint.copy(alpha = 0.88f), tint.copy(alpha = 0.72f), CardGradientEnd.copy(alpha = 0.86f))
        ),
        shape = RoundedCornerShape(20.dp)
      )
  )
}

@Composable
private fun GlassActionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(42.dp)
      .background(Color.White.copy(alpha = 0.86f), CircleShape)
      .border(1.dp, CardBorder, CircleShape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = PrimaryBlueDark
    )
  }
}

@Composable
private fun DashboardSectionHeader(
  title: String,
  actionLabel: String? = null,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    if (!actionLabel.isNullOrBlank()) {
      Text(
        text = actionLabel,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

private fun parseHexColor(hex: String): Color {
  return try {
    Color(android.graphics.Color.parseColor(hex))
  } catch (_: Exception) {
    Color(0xFFDDEBFF)
  }
}

private fun buildAgendaTasksForDashboard(events: List<CalendarEvent>): List<DashboardTask> {
  val today = LocalDate.now()
  return events
    .sortedWith(
      compareBy<CalendarEvent> { eventPriority(it, today) }
        .thenBy { parseDashboardEventStart(it) }
        .thenBy { it.title }
    )
    .map { event ->
      DashboardTask(
        id = "agenda-${event.id}",
        title = event.title,
        timeLabel = formatDashboardEventRange(event),
        colorHex = event.colorHex,
        categoryKey = event.categoryKey,
        description = event.description
      )
    }
}

private fun eventPriority(event: CalendarEvent, today: LocalDate): Int {
  val start = parseDashboardEventStart(event)
  val end = parseDashboardEventEnd(event)
  return when {
    !today.isBefore(start) && !today.isAfter(end) -> 0
    start.isAfter(today) -> 1
    else -> 2
  }
}

private fun formatDashboardEventRange(event: CalendarEvent): String {
  val start = parseDashboardEventStart(event)
  val end = parseDashboardEventEnd(event)
  val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
  val range = if (start == end) {
    formatter.format(start)
  } else {
    "${formatter.format(start)} - ${formatter.format(end)}"
  }
  return if (event.timeLabel.isBlank()) range else "$range | ${event.timeLabel}"
}

private fun parseDashboardEventStart(event: CalendarEvent): LocalDate {
  return runCatching { LocalDate.parse(event.startDateIso) }.getOrDefault(LocalDate.now())
}

private fun parseDashboardEventEnd(event: CalendarEvent): LocalDate {
  return runCatching { LocalDate.parse(event.endDateIso.ifBlank { event.startDateIso }) }
    .getOrDefault(parseDashboardEventStart(event))
}
