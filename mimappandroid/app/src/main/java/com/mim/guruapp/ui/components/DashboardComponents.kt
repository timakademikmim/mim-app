package com.mim.guruapp.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.DashboardCategory
import com.mim.guruapp.data.model.DashboardTask
import com.mim.guruapp.ui.i18n.LocalAppLanguage
import com.mim.guruapp.ui.i18n.formatDateForLanguage
import com.mim.guruapp.ui.i18n.t
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SubtleInk
import java.time.LocalDate

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
  onMenuButtonPositioned: (Rect) -> Unit = {},
  onDatePositioned: (Rect) -> Unit = {},
  onNotificationButtonPositioned: (Rect) -> Unit = {},
  onSearchBarPositioned: (Rect) -> Unit = {},
  onCategoriesPositioned: (Rect) -> Unit = {},
  onAgendaPositioned: (Rect) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategoryId by rememberSaveable { mutableStateOf("semua") }
  val normalizedQuery = searchQuery.trim().lowercase()
  val language = LocalAppLanguage.current
  val filteredTasks = remember(calendarEvents, selectedCategoryId, normalizedQuery, currentDate, language) {
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
    buildAgendaTasksForDashboard(filteredEvents, language)
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
        onNotificationClick = onNotificationClick,
        onMenuButtonPositioned = onMenuButtonPositioned,
        onDatePositioned = onDatePositioned,
        onNotificationButtonPositioned = onNotificationButtonPositioned
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
          modifier = Modifier
            .padding(top = 12.dp)
            .onGloballyPositioned { onSearchBarPositioned(it.boundsInRoot()) }
        )

        DashboardSectionHeader(
          title = "Categories",
          modifier = Modifier.padding(top = 20.dp)
        )

        LazyRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .onGloballyPositioned { onCategoriesPositioned(it.boundsInRoot()) },
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
            .weight(1f)
            .onGloballyPositioned { onAgendaPositioned(it.boundsInRoot()) },
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
  onNotificationClick: () -> Unit,
  onMenuButtonPositioned: (Rect) -> Unit = {},
  onDatePositioned: (Rect) -> Unit = {},
  onNotificationButtonPositioned: (Rect) -> Unit = {}
) {
  val language = LocalAppLanguage.current

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
        contentDescription = t("Buka sidebar"),
        onClick = onMenuClick,
        modifier = Modifier.onGloballyPositioned { onMenuButtonPositioned(it.boundsInRoot()) }
      )

    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(14.dp))
        .onGloballyPositioned { onDatePositioned(it.boundsInRoot()) }
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
        text = formatDateForLanguage(currentDate, "EEEE, dd MMM yyyy", language),
        style = MaterialTheme.typography.bodyMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp)
      )
    }

    Box {
      GlassActionButton(
        icon = Icons.Outlined.NotificationsNone,
        contentDescription = t("Notifikasi"),
        onClick = onNotificationClick,
        modifier = Modifier.onGloballyPositioned { onNotificationButtonPositioned(it.boundsInRoot()) }
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
      .background(CardBackground.copy(alpha = 0.88f), RoundedCornerShape(18.dp))
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
            text = t(placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtleInk.copy(alpha = 0.72f)
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
  val localizedSubtitle = remember(category.subtitle) {
    category.subtitle.removeSuffix(" Agenda").takeIf { it != category.subtitle }
  }?.let { count -> "$count ${t("Agenda")}" } ?: t(category.subtitle)

  PremiumGlassCard(
    accentColor = parseHexColor(category.colorHex),
    title = t(category.title),
    subtitle = localizedSubtitle,
    modifier = Modifier
      .width(168.dp)
      .height(96.dp),
    selected = selected,
    compact = true,
    onClick = onClick
  )
}

@Composable
fun TaskCard(task: DashboardTask) {
  PremiumGlassCard(
    accentColor = parseHexColor(task.colorHex),
    title = task.title,
    subtitle = task.timeLabel,
    modifier = Modifier.fillMaxWidth(),
    onClick = {}
  )
}

@Composable
private fun EmptyAgendaState() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground.copy(alpha = 0.82f), RoundedCornerShape(20.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
      .padding(16.dp)
  ) {
    Text(
      text = t("Tidak ada agenda yang cocok dengan kategori atau pencarian ini."),
      style = MaterialTheme.typography.bodyMedium,
      color = SubtleInk
    )
  }
}

@Composable
private fun PremiumGlassCard(
  accentColor: Color,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  selected: Boolean = false,
  compact: Boolean = false,
  onClick: () -> Unit
) {
  val isDarkMode = CardBackground.luminance() < 0.5f
  if (!isDarkMode) {
    LightDashboardCard(
      accentColor = accentColor,
      title = title,
      subtitle = subtitle,
      modifier = modifier,
      selected = selected,
      compact = compact,
      onClick = onClick
    )
    return
  }
  val shape = RoundedCornerShape(36.dp)
  val minHeight = if (compact) 92.dp else 112.dp
  val contentPadding = if (compact) 14.dp else 18.dp
  Box(
    modifier = modifier
      .heightIn(min = minHeight)
      .shadow(
        elevation = 22.dp,
        shape = shape,
        ambientColor = accentColor.copy(alpha = 0.18f),
        spotColor = Color(0x66000000)
      )
      .clip(shape)
      .background(Color(0xFF101216).copy(alpha = 0.88f), shape)
      .border(
        1.dp,
        if (selected) accentColor.copy(alpha = 0.48f) else Color.White.copy(alpha = 0.13f),
        shape
      )
      .clickable(onClick = onClick)
  ) {
    AmbientGlowLayer(accentColor = accentColor)
    GlassSurface()
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(contentPadding),
      verticalArrangement = Arrangement.Bottom
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          color = Color.White.copy(alpha = 0.96f),
          fontWeight = FontWeight.ExtraBold,
          maxLines = 2
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.64f),
          fontWeight = FontWeight.Medium,
          maxLines = 2
        )
      }
    }
    if (selected) {
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(16.dp)
          .size(8.dp)
          .background(accentColor.copy(alpha = 0.95f), CircleShape)
      )
    }
  }
}

@Composable
private fun LightDashboardCard(
  accentColor: Color,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  selected: Boolean = false,
  compact: Boolean = false,
  onClick: () -> Unit
) {
  val shape = RoundedCornerShape(20.dp)
  val contentPadding = if (compact) 14.dp else 16.dp
  Box(
    modifier = modifier
      .shadow(10.dp, shape, ambientColor = Color(0x120F172A), spotColor = Color(0x120F172A))
      .clip(shape)
      .background(
        brush = Brush.verticalGradient(
          listOf(
            CardBackground.copy(alpha = 0.96f),
            accentColor.copy(alpha = 0.12f),
            CardBackground.copy(alpha = 0.92f)
          )
        ),
        shape = shape
      )
      .border(
        1.dp,
        if (selected) PrimaryBlue.copy(alpha = 0.36f) else CardBorder.copy(alpha = 0.72f),
        shape
      )
      .clickable(onClick = onClick)
      .padding(contentPadding)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 2
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 2
      )
    }
  }
}

@Composable
private fun BoxScope.AmbientGlowLayer(accentColor: Color) {
  Canvas(
    modifier = Modifier
      .matchParentSize()
      .graphicsLayer {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          renderEffect = RenderEffect
            .createBlurEffect(34f, 34f, Shader.TileMode.CLAMP)
            .asComposeRenderEffect()
        }
      }
  ) {
    drawRect(
      brush = Brush.radialGradient(
        colors = listOf(
          accentColor.copy(alpha = 0.58f),
          accentColor.copy(alpha = 0.26f),
          Color.Transparent
        ),
        center = Offset(size.width * 1.03f, size.height * 0.42f),
        radius = size.maxDimension * 0.78f
      )
    )
    drawRect(
      brush = Brush.radialGradient(
        colors = listOf(
          accentColor.copy(alpha = 0.36f),
          Color.Transparent
        ),
        center = Offset(size.width * 0.92f, size.height * 1.04f),
        radius = size.maxDimension * 0.50f
      )
    )
  }
}

@Composable
private fun BoxScope.GlassSurface() {
  Canvas(modifier = Modifier.matchParentSize()) {
    val corner = CornerRadius(36.dp.toPx(), 36.dp.toPx())
    drawRoundRect(
      brush = Brush.verticalGradient(
        listOf(
          Color.White.copy(alpha = 0.11f),
          Color.White.copy(alpha = 0.025f),
          Color.Black.copy(alpha = 0.18f)
        )
      ),
      cornerRadius = corner
    )
    drawRoundRect(
      brush = Brush.horizontalGradient(
        listOf(
          Color.White.copy(alpha = 0.055f),
          Color.Transparent,
          Color.Black.copy(alpha = 0.20f)
        )
      ),
      cornerRadius = corner
    )
    drawRoundRect(
      brush = Brush.verticalGradient(
        listOf(
          Color.White.copy(alpha = 0.18f),
          Color.Transparent
        ),
        startY = 0f,
        endY = size.height * 0.36f
      ),
      cornerRadius = corner
    )
  }
}

@Composable
private fun GlassActionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(42.dp)
      .background(CardBackground.copy(alpha = 0.86f), CircleShape)
      .border(1.dp, CardBorder, CircleShape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = t(contentDescription),
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
      text = t(title),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    if (!actionLabel.isNullOrBlank()) {
      Text(
        text = t(actionLabel),
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

private fun buildAgendaTasksForDashboard(
  events: List<CalendarEvent>,
  language: com.mim.guruapp.ui.i18n.AppLanguage
): List<DashboardTask> {
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
        timeLabel = formatDashboardEventRange(event, language),
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

private fun formatDashboardEventRange(
  event: CalendarEvent,
  language: com.mim.guruapp.ui.i18n.AppLanguage
): String {
  val start = parseDashboardEventStart(event)
  val end = parseDashboardEventEnd(event)
  val range = if (start == end) {
    formatDateForLanguage(start, "dd MMM yyyy", language)
  } else {
    "${formatDateForLanguage(start, "dd MMM yyyy", language)} - ${formatDateForLanguage(end, "dd MMM yyyy", language)}"
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
