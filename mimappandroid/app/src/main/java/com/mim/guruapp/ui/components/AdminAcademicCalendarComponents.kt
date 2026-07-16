package com.mim.guruapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.remote.AdminAcademicCalendarClassOption
import com.mim.guruapp.data.remote.AdminAcademicCalendarEvent
import com.mim.guruapp.data.remote.AdminAcademicCalendarLoadResult
import com.mim.guruapp.data.remote.AdminAcademicCalendarSaveResult
import com.mim.guruapp.data.remote.AdminAcademicCalendarSnapshot
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.CardGradientEnd
import com.mim.guruapp.ui.theme.CardGradientStart
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk
import com.mim.guruapp.ui.theme.SuccessTint
import com.mim.guruapp.ui.theme.WarmAccent
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AdminAcademicCalendarScreen(
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadSnapshot: suspend () -> AdminAcademicCalendarLoadResult,
  onSaveEvent: suspend (AdminAcademicCalendarEvent) -> AdminAcademicCalendarSaveResult,
  onDeleteEvent: suspend (String) -> AdminAcademicCalendarSaveResult,
  modifier: Modifier = Modifier
) {
  var snapshot by remember { mutableStateOf(AdminAcademicCalendarSnapshot(emptyList(), emptyList())) }
  var selectedTab by rememberSaveable { mutableStateOf(AdminAcademicCalendarTab.List.name) }
  var selectedMonthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
  var selectedDateKey by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
  var query by rememberSaveable { mutableStateOf("") }
  var eventDraft by remember { mutableStateOf<AdminAcademicCalendarEvent?>(null) }
  var deleteTarget by remember { mutableStateOf<AdminAcademicCalendarEvent?>(null) }
  var isLoading by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  fun applySnapshot(nextSnapshot: AdminAcademicCalendarSnapshot) {
    snapshot = nextSnapshot
    val selectedStillVisible = nextSnapshot.events.any { event ->
      event.adminAcademicCalendarDateKeys().contains(selectedDateKey)
    }
    if (!selectedStillVisible) {
      selectedDateKey = nextSnapshot.events.firstOrNull {
        it.startDateIso.startsWith(selectedMonthKey)
      }?.startDateIso?.take(10).orEmpty()
    }
  }

  fun loadCalendar(showGlobalRefresh: Boolean = false) {
    scope.launch {
      isLoading = true
      errorMessage = ""
      if (showGlobalRefresh) onRefresh()
      when (val result = onLoadSnapshot()) {
        is AdminAcademicCalendarLoadResult.Success -> applySnapshot(result.snapshot)
        is AdminAcademicCalendarLoadResult.Error -> errorMessage = result.message
      }
      isLoading = false
    }
  }

  LaunchedEffect(Unit) {
    loadCalendar()
  }

  if (eventDraft != null) {
    AdminAcademicCalendarEventDialog(
      draft = eventDraft ?: snapshot.newCalendarEventDraft(selectedMonthKey),
      classes = snapshot.classes,
      isSaving = isSaving,
      onDismiss = { if (!isSaving) eventDraft = null },
      onConfirm = { event ->
        scope.launch {
          isSaving = true
          when (val result = onSaveEvent(event)) {
            is AdminAcademicCalendarSaveResult.Success -> {
              eventDraft = null
              applySnapshot(result.snapshot)
              snackbarHostState.showSnackbar(result.message)
            }
            is AdminAcademicCalendarSaveResult.Error -> snackbarHostState.showSnackbar(result.message)
          }
          isSaving = false
        }
      }
    )
  }

  if (deleteTarget != null) {
    val target = deleteTarget
    AlertDialog(
      onDismissRequest = { if (!isSaving) deleteTarget = null },
      title = { Text("Hapus Kegiatan") },
      text = { Text("Yakin ingin menghapus ${target?.title.orEmpty().ifBlank { "kegiatan ini" }}?") },
      confirmButton = {
        Button(
          onClick = {
            val id = target?.rowId.orEmpty()
            scope.launch {
              isSaving = true
              when (val result = onDeleteEvent(id)) {
                is AdminAcademicCalendarSaveResult.Success -> {
                  deleteTarget = null
                  applySnapshot(result.snapshot)
                  snackbarHostState.showSnackbar(result.message)
                }
                is AdminAcademicCalendarSaveResult.Error -> snackbarHostState.showSnackbar(result.message)
              }
              isSaving = false
            }
          },
          enabled = !isSaving
        ) { Text("Hapus") }
      },
      dismissButton = {
        TextButton(onClick = { deleteTarget = null }, enabled = !isSaving) { Text("Batal") }
      }
    )
  }

  BackHandler(enabled = eventDraft != null || deleteTarget != null) {
    if (!isSaving) {
      eventDraft = null
      deleteTarget = null
    }
  }

  val tab = runCatching { AdminAcademicCalendarTab.valueOf(selectedTab) }
    .getOrDefault(AdminAcademicCalendarTab.List)
  val selectedMonth = selectedMonthKey.toAdminAcademicYearMonth()
  val eventsInMonth = snapshot.events.filter { event ->
    event.adminAcademicCalendarDateKeys().any { it.startsWith(selectedMonth.toString()) }
  }
  val filteredEvents = snapshot.events
    .filter { event ->
      val needle = query.trim()
      needle.isBlank() ||
        event.title.contains(needle, ignoreCase = true) ||
        event.detail.contains(needle, ignoreCase = true) ||
        event.kind.adminAcademicCalendarKindLabel().contains(needle, ignoreCase = true) ||
        event.classNames.any { it.contains(needle, ignoreCase = true) }
    }
  val selectedDateEvents = eventsInMonth.filter { it.adminAcademicCalendarDateKeys().contains(selectedDateKey) }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      AdminAcademicCalendarTopBar(
        title = "Kalender Akademik",
        onMenuClick = onMenuClick,
        actionIcon = Icons.Outlined.Add,
        actionContentDescription = "Tambah kegiatan akademik",
        onActionClick = { eventDraft = snapshot.newCalendarEventDraft(selectedMonthKey) }
      )
      AdminAcademicCalendarSearchBar(query, onValueChange = { query = it })

      PullToRefreshBox(
        isRefreshing = isRefreshing || isLoading,
        onRefresh = { loadCalendar(showGlobalRefresh = true) },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .navigationBarsPadding()
      ) {
        AdminAcademicCalendarContent(
          isLoading = isLoading,
          errorMessage = errorMessage,
          tab = tab,
          eventCount = filteredEvents.size,
          monthEventCount = eventsInMonth.size,
          selectedMonth = selectedMonth,
          selectedDateKey = selectedDateKey,
          events = filteredEvents,
          eventsInMonth = eventsInMonth,
          selectedDateEvents = selectedDateEvents,
          onTabSelected = { selectedTab = it.name },
          onMonthChange = {
            selectedMonthKey = it.toString()
            selectedDateKey = ""
          },
          onDateSelected = { selectedDateKey = it },
          onEdit = { eventDraft = it },
          onDelete = { deleteTarget = it }
        )
      }
    }
  }
}

@Composable
private fun AdminAcademicCalendarContent(
  isLoading: Boolean,
  errorMessage: String,
  tab: AdminAcademicCalendarTab,
  eventCount: Int,
  monthEventCount: Int,
  selectedMonth: YearMonth,
  selectedDateKey: String,
  events: List<AdminAcademicCalendarEvent>,
  eventsInMonth: List<AdminAcademicCalendarEvent>,
  selectedDateEvents: List<AdminAcademicCalendarEvent>,
  onTabSelected: (AdminAcademicCalendarTab) -> Unit,
  onMonthChange: (YearMonth) -> Unit,
  onDateSelected: (String) -> Unit,
  onEdit: (AdminAcademicCalendarEvent) -> Unit,
  onDelete: (AdminAcademicCalendarEvent) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(bottom = 112.dp)
  ) {
    if (isLoading && events.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator()
        }
      }
    }
    if (errorMessage.isNotBlank()) {
      item { AdminAcademicCalendarNoticeCard(errorMessage, Color(0xFFFB7185)) }
    }
    item {
      AdminAcademicCalendarSummaryCard(
        eventCount = eventCount,
        monthEventCount = monthEventCount,
        selectedMonth = selectedMonth
      )
    }
    item {
      AdminAcademicCalendarTabBar(tab, onSelected = onTabSelected)
    }
    if (tab == AdminAcademicCalendarTab.List) {
      if (!isLoading && events.isEmpty()) {
        item { EmptyPlaceholderCard("Belum ada kegiatan akademik.") }
      }
      items(events, key = { it.rowId }) { event ->
        AdminAcademicCalendarEventCard(event, onEdit = { onEdit(event) }, onDelete = { onDelete(event) })
      }
    } else {
      item {
        AdminAcademicCalendarMonthCard(
          selectedMonth = selectedMonth,
          selectedDateKey = selectedDateKey,
          events = eventsInMonth,
          onMonthChange = onMonthChange,
          onDateSelected = onDateSelected
        )
      }
      if (selectedDateKey.isBlank()) {
        item { EmptyPlaceholderCard("Pilih tanggal untuk melihat detail kegiatan.") }
      } else if (selectedDateEvents.isEmpty()) {
        item { EmptyPlaceholderCard("Tidak ada kegiatan pada tanggal ${selectedDateKey.adminAcademicCalendarDisplayDate()}.") }
      } else {
        items(selectedDateEvents, key = { "date-${it.rowId}" }) { event ->
          AdminAcademicCalendarEventCard(event, onEdit = { onEdit(event) }, onDelete = { onDelete(event) })
        }
      }
    }
  }
}

@Composable
private fun AdminAcademicCalendarTopBar(
  title: String,
  onMenuClick: () -> Unit,
  actionIcon: ImageVector,
  actionContentDescription: String,
  onActionClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    AdminAcademicCalendarCircleButton(Icons.Outlined.Menu, "Buka menu", onMenuClick)
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Kelola agenda, libur, ujian, dan kalender bulanan",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    AdminAcademicCalendarCircleButton(
      icon = actionIcon,
      contentDescription = actionContentDescription,
      onClick = onActionClick,
      background = PrimaryBlue,
      contentColor = Color.White
    )
  }
}

@Composable
private fun AdminAcademicCalendarCircleButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  background: Color = CardBackground,
  contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
  Box(
    modifier = Modifier
      .size(46.dp)
      .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
      .background(background, CircleShape)
      .border(BorderStroke(1.dp, CardBorder), CircleShape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(icon, contentDescription = contentDescription, tint = contentColor)
  }
}

@Composable
private fun AdminAcademicCalendarSearchBar(
  value: String,
  onValueChange: (String) -> Unit
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier.fillMaxWidth(),
    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
    placeholder = { Text("Cari kegiatan, jenis, detail, kelas") },
    singleLine = true,
    shape = RoundedCornerShape(16.dp)
  )
}

@Composable
private fun AdminAcademicCalendarSummaryCard(
  eventCount: Int,
  monthEventCount: Int,
  selectedMonth: YearMonth
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(22.dp), ambientColor = PrimaryBlue.copy(alpha = 0.10f), spotColor = PrimaryBlue.copy(alpha = 0.10f))
      .background(Brush.horizontalGradient(listOf(CardGradientStart, CardGradientEnd)), RoundedCornerShape(22.dp))
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(22.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .background(PrimaryBlue.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = PrimaryBlue)
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Kalender ${selectedMonth.adminAcademicCalendarMonthLabel()}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Agenda akademik tersinkron ke dashboard guru",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      AdminAcademicCalendarInfoPill("$eventCount kegiatan", SuccessTint)
      AdminAcademicCalendarInfoPill("$monthEventCount bulan ini", WarmAccent.copy(alpha = 0.22f))
    }
  }
}

@Composable
private fun AdminAcademicCalendarTabBar(
  selectedTab: AdminAcademicCalendarTab,
  onSelected: (AdminAcademicCalendarTab) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(SoftPanel, RoundedCornerShape(18.dp))
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    AdminAcademicCalendarTab.values().forEach { tab ->
      val selected = selectedTab == tab
      Box(
        modifier = Modifier
          .weight(1f)
          .background(if (selected) PrimaryBlue else Color.Transparent, RoundedCornerShape(14.dp))
          .clickable { onSelected(tab) }
          .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = tab.label,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
private fun AdminAcademicCalendarMonthCard(
  selectedMonth: YearMonth,
  selectedDateKey: String,
  events: List<AdminAcademicCalendarEvent>,
  onMonthChange: (YearMonth) -> Unit,
  onDateSelected: (String) -> Unit
) {
  val eventMap = remember(events) {
    buildMap<String, List<AdminAcademicCalendarEvent>> {
      val mutable = linkedMapOf<String, MutableList<AdminAcademicCalendarEvent>>()
      events.forEach { event ->
        event.adminAcademicCalendarDateKeys().forEach { key ->
          mutable.getOrPut(key) { mutableListOf() }.add(event)
        }
      }
      mutable.forEach { (key, value) -> put(key, value) }
    }
  }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground, RoundedCornerShape(18.dp))
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(18.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(onClick = { onMonthChange(selectedMonth.minusMonths(1)) }) { Text("Sebelumnya") }
      Text(
        text = selectedMonth.adminAcademicCalendarMonthLabel(),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      OutlinedButton(onClick = { onMonthChange(selectedMonth.plusMonths(1)) }) { Text("Berikutnya") }
    }
    AdminAcademicCalendarWeekHeader()
    selectedMonth.adminAcademicCalendarGridDates().chunked(7).forEach { week ->
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        week.forEach { date ->
          val key = date?.toString().orEmpty()
          val dayEvents = eventMap[key].orEmpty()
          AdminAcademicCalendarDayCell(
            day = date?.dayOfMonth?.toString().orEmpty(),
            isEmpty = date == null,
            isToday = date == LocalDate.now(),
            isSelected = key.isNotBlank() && key == selectedDateKey,
            events = dayEvents,
            onClick = { if (key.isNotBlank()) onDateSelected(key) },
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

@Composable
private fun AdminAcademicCalendarWeekHeader() {
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
    listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab").forEach { day ->
      Text(
        text = day,
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = SubtleInk
      )
    }
  }
}

@Composable
private fun AdminAcademicCalendarDayCell(
  day: String,
  isEmpty: Boolean,
  isToday: Boolean,
  isSelected: Boolean,
  events: List<AdminAcademicCalendarEvent>,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val firstColor = events.firstOrNull()?.colorHex?.adminAcademicCalendarColor() ?: PrimaryBlue
  val background = when {
    isEmpty -> SoftPanel.copy(alpha = 0.45f)
    events.isNotEmpty() -> firstColor.copy(alpha = 0.12f)
    else -> Color.Transparent
  }
  Column(
    modifier = modifier
      .height(58.dp)
      .background(background, RoundedCornerShape(12.dp))
      .border(
        BorderStroke(
          width = if (isSelected || isToday) 2.dp else 1.dp,
          color = when {
            isSelected -> PrimaryBlue
            isToday -> WarmAccent
            else -> CardBorder
          }
        ),
        RoundedCornerShape(12.dp)
      )
      .clickable(enabled = !isEmpty, onClick = onClick)
      .padding(6.dp),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = day,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = if (isEmpty) Color.Transparent else MaterialTheme.colorScheme.onSurface
    )
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
      events.take(4).forEach { event ->
        Box(
          modifier = Modifier
            .size(6.dp)
            .background(event.colorHex.adminAcademicCalendarColor(), CircleShape)
        )
      }
    }
  }
}

@Composable
private fun AdminAcademicCalendarEventCard(
  event: AdminAcademicCalendarEvent,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground, RoundedCornerShape(18.dp))
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(18.dp))
      .padding(14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .height(92.dp)
        .size(width = 5.dp, height = 92.dp)
        .background(event.colorHex.adminAcademicCalendarColor(), RoundedCornerShape(999.dp))
    )
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = event.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = event.adminAcademicCalendarDateLabel(),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminAcademicCalendarInfoPill(event.kind.adminAcademicCalendarKindLabel(), SuccessTint)
        if (event.classNames.isNotEmpty()) {
          AdminAcademicCalendarInfoPill("Kelas: ${event.classNames.joinToString(", ")}", SoftPanel)
        }
      }
      if (event.detail.isNotBlank()) {
        Text(
          text = event.detail,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
    Column {
      IconButton(onClick = onEdit) {
        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = PrimaryBlue)
      }
      IconButton(onClick = onDelete) {
        Icon(Icons.Outlined.Delete, contentDescription = "Hapus", tint = Color(0xFFDC2626))
      }
    }
  }
}

@Composable
private fun AdminAcademicCalendarEventDialog(
  draft: AdminAcademicCalendarEvent,
  classes: List<AdminAcademicCalendarClassOption>,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (AdminAcademicCalendarEvent) -> Unit
) {
  var title by remember(draft.rowId) { mutableStateOf(draft.title) }
  var kind by remember(draft.rowId) { mutableStateOf(draft.kind) }
  var colorHex by remember(draft.rowId) { mutableStateOf(draft.colorHex.ifBlank { "#2563eb" }) }
  var startDate by remember(draft.rowId) { mutableStateOf(draft.startDateIso.take(10)) }
  var endDate by remember(draft.rowId) { mutableStateOf(draft.endDateIso.take(10)) }
  var detail by remember(draft.rowId) { mutableStateOf(draft.detail) }
  var selectedClassIds by remember(draft.rowId) { mutableStateOf(draft.classIds.toSet()) }
  val showClasses = kind == "ujian" || kind == "libur_kelas"
  val scrollState = rememberScrollState()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (draft.rowId.isBlank()) "Tambah Kegiatan Akademik" else "Edit Kegiatan Akademik") },
    text = {
      Column(
        modifier = Modifier
          .heightIn(max = 560.dp)
          .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Judul kegiatan") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        AdminAcademicCalendarOptionDropdown(
          label = "Jenis kegiatan",
          selectedLabel = kind.adminAcademicCalendarKindLabel(),
          options = AdminAcademicCalendarKindOptions,
          onSelected = { kind = it }
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Warna penanda", style = MaterialTheme.typography.labelMedium, color = SubtleInk, fontWeight = FontWeight.SemiBold)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminAcademicCalendarColorOptions.forEach { color ->
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .background(color.adminAcademicCalendarColor(), CircleShape)
                  .border(BorderStroke(if (colorHex == color) 3.dp else 1.dp, if (colorHex == color) MaterialTheme.colorScheme.onSurface else CardBorder), CircleShape)
                  .clickable { colorHex = color }
              )
            }
          }
        }
        OutlinedTextField(
          value = startDate,
          onValueChange = { startDate = it.take(10) },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Mulai (yyyy-MM-dd)") },
          singleLine = true
        )
        OutlinedTextField(
          value = endDate,
          onValueChange = { endDate = it.take(10) },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Selesai (opsional)") },
          singleLine = true
        )
        if (showClasses) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(SoftPanel, RoundedCornerShape(14.dp))
              .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            Text("Target kelas", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("Kosongkan jika berlaku untuk semua kelas.", style = MaterialTheme.typography.bodySmall, color = SubtleInk)
            classes.forEach { kelas ->
              val checked = selectedClassIds.contains(kelas.id)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    selectedClassIds = if (checked) selectedClassIds - kelas.id else selectedClassIds + kelas.id
                  },
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = checked,
                  onCheckedChange = { selected ->
                    selectedClassIds = if (selected) selectedClassIds + kelas.id else selectedClassIds - kelas.id
                  }
                )
                Text(kelas.name, style = MaterialTheme.typography.bodyMedium)
              }
            }
          }
        }
        OutlinedTextField(
          value = detail,
          onValueChange = { detail = it },
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
          label = { Text("Detail kegiatan") }
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val classNames = classes.filter { selectedClassIds.contains(it.id) }.map { it.name }
          onConfirm(
            draft.copy(
              title = title.trim(),
              kind = kind,
              colorHex = colorHex,
              startDateIso = startDate.trim(),
              endDateIso = endDate.trim(),
              detail = detail.trim(),
              classIds = if (showClasses) selectedClassIds.toList() else emptyList(),
              classNames = if (showClasses) classNames else emptyList()
            )
          )
        },
        enabled = !isSaving
      ) {
        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
        else Text("Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Batal") }
    }
  )
}

@Composable
private fun AdminAcademicCalendarOptionDropdown(
  label: String,
  selectedLabel: String,
  options: List<Pair<String, String>>,
  onSelected: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = SubtleInk, fontWeight = FontWeight.SemiBold)
    Box {
      OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Text(selectedLabel, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.Outlined.ExpandMore, contentDescription = null)
      }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { (id, text) ->
          DropdownMenuItem(
            text = { Text(text) },
            onClick = {
              expanded = false
              onSelected(id)
            }
          )
        }
      }
    }
  }
}

@Composable
private fun AdminAcademicCalendarInfoPill(text: String, background: Color) {
  Box(
    modifier = Modifier
      .background(background, RoundedCornerShape(999.dp))
      .padding(horizontal = 10.dp, vertical = 5.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text.ifBlank { "-" },
      style = MaterialTheme.typography.labelSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun AdminAcademicCalendarNoticeCard(message: String, accent: Color) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(accent.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
      .border(BorderStroke(1.dp, accent.copy(alpha = 0.24f)), RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Icon(Icons.Outlined.Check, contentDescription = null, tint = accent)
    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
  }
}

private enum class AdminAcademicCalendarTab(val label: String) {
  List("Daftar"),
  Calendar("Kalender")
}

private val AdminAcademicCalendarKindOptions = listOf(
  "" to "Kegiatan Umum",
  "ujian" to "Ujian",
  "libur_kelas" to "Libur Kelas",
  "libur_semua_kegiatan" to "Libur Semua Kegiatan",
  "libur_akademik" to "Libur Akademik",
  "libur_ketahfizan" to "Libur Ketahfizan"
)

private val AdminAcademicCalendarColorOptions = listOf(
  "#2563eb",
  "#16a34a",
  "#f59e0b",
  "#dc2626",
  "#7c3aed",
  "#0891b2"
)

private fun AdminAcademicCalendarSnapshot.newCalendarEventDraft(monthKey: String): AdminAcademicCalendarEvent {
  val startDate = runCatching { YearMonth.parse(monthKey) }.getOrDefault(YearMonth.now()).atDay(1).toString()
  return AdminAcademicCalendarEvent(
    rowId = "",
    title = "",
    kind = "",
    startDateIso = startDate,
    endDateIso = "",
    detail = "",
    colorHex = "#2563eb",
    classIds = emptyList(),
    classNames = emptyList()
  )
}

private fun AdminAcademicCalendarEvent.adminAcademicCalendarDateKeys(): List<String> {
  val start = runCatching { LocalDate.parse(startDateIso.take(10)) }.getOrNull() ?: return emptyList()
  val end = runCatching { LocalDate.parse(endDateIso.take(10).ifBlank { startDateIso.take(10) }) }.getOrNull() ?: start
  if (end.isBefore(start)) return listOf(start.toString())
  return buildList {
    var cursor = start
    while (!cursor.isAfter(end)) {
      add(cursor.toString())
      cursor = cursor.plusDays(1)
    }
  }
}

private fun AdminAcademicCalendarEvent.adminAcademicCalendarDateLabel(): String {
  val start = startDateIso.take(10).adminAcademicCalendarDisplayDate()
  val end = endDateIso.take(10).adminAcademicCalendarDisplayDate()
  return if (end.isBlank() || end == start) start else "$start - $end"
}

private fun YearMonth.adminAcademicCalendarGridDates(): List<LocalDate?> {
  val first = atDay(1)
  val leading = first.dayOfWeek.value % 7
  val days = (1..lengthOfMonth()).map { atDay(it) }
  val raw = List(leading) { null } + days
  val trailing = (7 - raw.size % 7).takeIf { it < 7 } ?: 0
  return raw + List(trailing) { null }
}

private fun YearMonth.adminAcademicCalendarMonthLabel(): String {
  val month = month.getDisplayName(TextStyle.FULL, Locale("id", "ID"))
  return "${month.replaceFirstChar { it.uppercase() }} $year"
}

private fun String.toAdminAcademicYearMonth(): YearMonth {
  return runCatching { YearMonth.parse(this) }.getOrDefault(YearMonth.now())
}

private fun String.adminAcademicCalendarDisplayDate(): String {
  if (isBlank()) return ""
  val date = runCatching { LocalDate.parse(take(10)) }.getOrNull() ?: return this
  return date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
}

private fun String.adminAcademicCalendarKindLabel(): String {
  return when (trim().lowercase()) {
    "libur_semua_kegiatan" -> "Libur Semua Kegiatan"
    "libur_akademik" -> "Libur Akademik"
    "libur_ketahfizan" -> "Libur Ketahfizan"
    "ujian" -> "Ujian"
    "libur_kelas" -> "Libur Kelas"
    else -> "Kegiatan Umum"
  }
}

private fun String.adminAcademicCalendarColor(): Color {
  val raw = trim().removePrefix("#")
  val value = raw.toLongOrNull(16) ?: 0x2563eb
  val r = ((value shr 16) and 0xFF).toInt()
  val g = ((value shr 8) and 0xFF).toInt()
  val b = (value and 0xFF).toInt()
  return Color(r, g, b)
}
