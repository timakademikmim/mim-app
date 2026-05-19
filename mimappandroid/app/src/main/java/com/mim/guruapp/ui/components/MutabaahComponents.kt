package com.mim.guruapp.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.MutabaahSaveOutcome
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.LeaveRequestItem
import com.mim.guruapp.data.model.MutabaahSnapshot
import com.mim.guruapp.data.model.MutabaahSubmission
import com.mim.guruapp.data.model.MutabaahTask
import com.mim.guruapp.export.MutabaahExcelExporter
import com.mim.guruapp.ui.i18n.t
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.CardGradientEnd
import com.mim.guruapp.ui.theme.HighlightCard
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk
import com.mim.guruapp.ui.theme.SuccessTint
import com.mim.guruapp.ui.theme.WarmAccent
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutabaahScreen(
  selectedDate: LocalDate,
  teacherName: String,
  snapshot: MutabaahSnapshot,
  teachingScheduleEvents: List<CalendarEvent>,
  leaveRequests: List<LeaveRequestItem>,
  isRefreshing: Boolean,
  onSelectDate: (LocalDate) -> Unit,
  onJumpToToday: () -> Unit,
  onRefresh: () -> Unit,
  onMenuClick: () -> Unit,
  onLoadSnapshot: suspend (LocalDate) -> MutabaahSnapshot?,
  onToggleTask: suspend (String, String, Boolean) -> MutabaahSaveOutcome,
  onToggleTasks: suspend (List<String>, String, String) -> MutabaahSaveOutcome,
  modifier: Modifier = Modifier
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  var isLoadingPeriod by rememberSaveable { mutableStateOf(false) }
  var savingTaskId by rememberSaveable { mutableStateOf<String?>(null) }
  var isExporting by rememberSaveable { mutableStateOf(false) }
  var exportShouldShare by rememberSaveable { mutableStateOf<Boolean?>(null) }
  var exportMonthKeys by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
  val selectedPeriod = selectedDate.toString().take(7)
  val selectedMonth = remember(selectedPeriod) { YearMonth.parse(selectedPeriod) }

  BackHandler(enabled = false) {}

  LaunchedEffect(selectedPeriod, snapshot.period) {
    if (snapshot.period != selectedPeriod && !isLoadingPeriod) {
      isLoadingPeriod = true
      onLoadSnapshot(selectedDate)
      isLoadingPeriod = false
    }
  }

  val holidayDates = remember(snapshot.academicHolidayDates) { snapshot.academicHolidayDates.toSet() }
  val dayTasks = remember(snapshot.tasks, selectedDate) {
    snapshot.tasks
      .filter { it.active && it.dateIso == selectedDate.toString() }
      .sortedWith(compareBy<MutabaahTask> { it.title.lowercase() }.thenBy { it.id })
  }
  val submissions = remember(snapshot.submissions) {
    snapshot.submissions.associateBy { "${it.templateId}|${it.dateIso}" }
  }
  val selectedDateIso = selectedDate.toString()
  val hasTeachingSchedule = remember(teachingScheduleEvents, selectedDate) {
    teachingScheduleEvents.any { event -> event.coversDate(selectedDate) }
  }
  val approvedLeave = remember(leaveRequests, selectedDate) {
    leaveRequests.firstOrNull { it.isApprovedOn(selectedDate) }
  }
  val isApprovedLeaveDay = approvedLeave != null
  val isOfficeOnlyDay = !isApprovedLeaveDay && !hasTeachingSchedule && dayTasks.isNotEmpty()
  val visibleDayTasks = remember(dayTasks, isOfficeOnlyDay) {
    if (isOfficeOnlyDay) dayTasks.officeOnlyVisibleTasks() else dayTasks
  }
  val visibleTaskIds = remember(visibleDayTasks) { visibleDayTasks.map { it.id }.toSet() }
  val hiddenDayTaskIds = remember(dayTasks, visibleTaskIds) {
    dayTasks.mapNotNull { task -> task.id.takeIf { it !in visibleTaskIds } }
  }
  val allDayTasksIzin = dayTasks.isNotEmpty() && dayTasks.all { task ->
    submissions["${task.id}|$selectedDateIso"]?.status == "izin"
  }
  val doneCount = when {
    isApprovedLeaveDay -> 0
    else -> visibleDayTasks.count { task ->
      submissions["${task.id}|$selectedDateIso"]?.status == "selesai"
    }
  }
  val summaryTotal = if (isApprovedLeaveDay && dayTasks.isNotEmpty()) dayTasks.size else visibleDayTasks.size
  val isHoliday = holidayDates.contains(selectedDate.toString())
  val isSunday = selectedDate.dayOfWeek == DayOfWeek.SUNDAY
  val timelineEvents = remember(snapshot.tasks) { snapshot.tasks.toCalendarEvents() }
  var autoSyncedLeaveDates by remember { mutableStateOf<Set<String>>(emptySet()) }

  LaunchedEffect(isApprovedLeaveDay, selectedDateIso, dayTasks, allDayTasksIzin) {
    if (isApprovedLeaveDay && dayTasks.isNotEmpty() && !allDayTasksIzin && selectedDateIso !in autoSyncedLeaveDates) {
      autoSyncedLeaveDates = autoSyncedLeaveDates + selectedDateIso
      val result = onToggleTasks(dayTasks.map { it.id }, selectedDateIso, "izin")
      if (!result.success) snackbarHostState.showSnackbar(result.message)
    }
  }

  androidx.compose.material3.Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
  ) { innerPadding ->
    PullToRefreshBox(
      isRefreshing = isRefreshing || isLoadingPeriod,
      onRefresh = onRefresh,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        MutabaahHeader(
          selectedDate = selectedDate,
          onJumpToToday = onJumpToToday,
          onMenuClick = onMenuClick,
          isExporting = isExporting,
          onExportClick = { shouldShare ->
            exportMonthKeys = listOf(selectedMonth.toString())
            exportShouldShare = shouldShare
          }
        )

        MutabaahWeekSwitcher(
          selectedDate = selectedDate,
          onPreviousWeek = { onSelectDate(selectedDate.minusWeeks(1)) },
          onNextWeek = { onSelectDate(selectedDate.plusWeeks(1)) }
        )

        WeekCalendar(
          selectedDate = selectedDate,
          events = timelineEvents,
          accentColor = HighlightCard,
          onSelectDate = onSelectDate
        )

        MutabaahSummaryCard(
          total = summaryTotal,
          done = doneCount,
          isSunday = isSunday,
          isHoliday = isHoliday,
          isApprovedLeave = isApprovedLeaveDay,
          isOfficeOnlyDay = isOfficeOnlyDay
        )

        Text(
          text = t("Timeline Mutabaah"),
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(bottom = 124.dp)
        ) {
          when {
            isSunday -> item {
              MutabaahEmptyCard("Hari Ahad libur. Mutabaah tidak diinput pada hari Ahad.")
            }

            isHoliday -> item {
              MutabaahEmptyCard("Tanggal ini libur akademik. Mutabaah tidak dihitung.")
            }

            isApprovedLeaveDay -> item {
              MutabaahEmptyCard(
                "Perizinan disetujui untuk tanggal ini. Hari ini tidak dihitung sebagai progress mutabaah dan akan ditandai izin di laporan admin."
              )
            }

            isLoadingPeriod -> items(4) {
              SkeletonContentCard(
                leadingSize = 44.dp,
                trailingSize = 42.dp,
                firstLineWidth = 0.62f,
                secondLineWidth = 0.82f
              )
            }

            visibleDayTasks.isEmpty() -> item {
              MutabaahEmptyCard("Belum ada tugas mutabaah untuk tanggal ini.")
            }

            else -> visibleDayTasks.forEachIndexed { index, task ->
              item(key = task.id) {
                val submission = submissions["${task.id}|$selectedDateIso"]
                val checked = submission?.status == "selesai"
                MutabaahTimelineRow(
                  task = task,
                  checked = checked,
                  submission = submission,
                  isSaving = savingTaskId == task.id || savingTaskId == selectedDateIso || submission?.syncState == "syncing",
                  isLast = index == visibleDayTasks.lastIndex,
                  onCheckedChange = { nextChecked ->
                    scope.launch {
                      savingTaskId = if (isOfficeOnlyDay && hiddenDayTaskIds.isNotEmpty()) selectedDateIso else task.id
                      val result = if (isOfficeOnlyDay) {
                        val visibleResult = onToggleTask(task.id, selectedDateIso, nextChecked)
                        if (!visibleResult.success) {
                          visibleResult
                        } else if (hiddenDayTaskIds.isEmpty()) {
                          visibleResult
                        } else {
                          val anotherVisibleDone = visibleDayTasks.any { visibleTask ->
                            visibleTask.id != task.id &&
                              submissions["${visibleTask.id}|$selectedDateIso"]?.status == "selesai"
                          }
                          val hiddenStatus = if (nextChecked || anotherVisibleDone) "selesai" else "belum"
                          val hiddenResult = onToggleTasks(hiddenDayTaskIds, selectedDateIso, hiddenStatus)
                          if (hiddenResult.success) {
                            visibleResult
                          } else {
                            hiddenResult
                          }
                        }
                      } else {
                        onToggleTask(task.id, selectedDateIso, nextChecked)
                      }
                      savingTaskId = null
                      if (!result.success) {
                        snackbarHostState.showSnackbar(result.message)
                      }
                    }
                  }
                )
              }
            }
          }
        }
      }
    }
  }

  exportShouldShare?.let { shouldShare ->
    MutabaahExportPeriodDialog(
      shouldShare = shouldShare,
      currentMonth = selectedMonth,
      selectedMonthKeys = exportMonthKeys,
      isExporting = isExporting,
      onSelectedMonthsChange = { exportMonthKeys = it },
      onDismiss = {
        if (!isExporting) exportShouldShare = null
      },
      onConfirm = {
        val months = exportMonthKeys
          .mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }
          .distinct()
          .sorted()
        if (months.isEmpty()) {
          scope.launch { snackbarHostState.showSnackbar("Pilih minimal satu bulan.") }
          return@MutabaahExportPeriodDialog
        }
        scope.launch {
          isExporting = true
          val result = runCatching {
            val worksheets = months.map { month ->
              val monthSnapshot = if (snapshot.period == month.toString()) {
                snapshot
              } else {
                onLoadSnapshot(month.atDay(1)) ?: MutabaahSnapshot(period = month.toString())
              }
              MutabaahExcelExporter.MutabaahWorksheet(
                period = month,
                snapshot = monthSnapshot,
                leaveRequests = leaveRequests
              )
            }
            MutabaahExcelExporter.createWorkbook(
              context = context,
              teacherName = teacherName,
              worksheets = worksheets
            )
          }
          result.onSuccess { file ->
            exportShouldShare = null
            if (shouldShare) {
              MutabaahExcelExporter.shareWorkbook(context, file)
            } else {
              MutabaahExcelExporter.openWorkbook(context, file)
            }
          }.onFailure { error ->
            snackbarHostState.showSnackbar(error.message ?: "Gagal membuat file mutabaah.")
          }
          isExporting = false
        }
      }
    )
  }
}

@Composable
private fun MutabaahExportPeriodDialog(
  shouldShare: Boolean,
  currentMonth: YearMonth,
  selectedMonthKeys: List<String>,
  isExporting: Boolean,
  onSelectedMonthsChange: (List<String>) -> Unit,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  val monthOptions = remember(currentMonth) { currentMonth.academicYearMonths() }
  val selectedSet = selectedMonthKeys.toSet()
  val semesterMonths = remember(currentMonth) { currentMonth.semesterMonths() }
  val title = if (shouldShare) "Kirim Mutabaah" else "Cetak Mutabaah"

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = CardBackground,
    title = {
      Text(
        text = t(title),
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
          text = t("Pilih bulan yang ingin dimasukkan ke file Excel. Jika memilih lebih dari satu bulan, setiap bulan akan dibuat sebagai tab tersendiri."),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          MutabaahPeriodShortcut(
            label = "Bulan Ini",
            selected = selectedSet == setOf(currentMonth.toString()),
            onClick = { onSelectedMonthsChange(listOf(currentMonth.toString())) }
          )
          MutabaahPeriodShortcut(
            label = "Semester",
            selected = selectedSet == semesterMonths.map { it.toString() }.toSet(),
            onClick = { onSelectedMonthsChange(semesterMonths.map { it.toString() }) }
          )
          MutabaahPeriodShortcut(
            label = "Tahun",
            selected = selectedSet == monthOptions.map { it.toString() }.toSet(),
            onClick = { onSelectedMonthsChange(monthOptions.map { it.toString() }) }
          )
        }
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(monthOptions.size) { index ->
            val month = monthOptions[index]
            val monthKey = month.toString()
            val checked = monthKey in selectedSet
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (checked) HighlightCard.copy(alpha = 0.14f) else SoftPanel.copy(alpha = 0.72f))
                .border(
                  width = 1.dp,
                  color = if (checked) HighlightCard.copy(alpha = 0.35f) else CardBorder,
                  shape = RoundedCornerShape(16.dp)
                )
                .clickable(enabled = !isExporting) {
                  val next = if (checked) {
                    selectedMonthKeys.filterNot { it == monthKey }
                  } else {
                    (selectedMonthKeys + monthKey).distinct().sorted()
                  }
                  onSelectedMonthsChange(next)
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = checked,
                enabled = !isExporting,
                onCheckedChange = {
                  val next = if (checked) {
                    selectedMonthKeys.filterNot { key -> key == monthKey }
                  } else {
                    (selectedMonthKeys + monthKey).distinct().sorted()
                  }
                  onSelectedMonthsChange(next)
                }
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = month.formatMonthTitle(),
                  style = MaterialTheme.typography.titleSmall,
                  color = PrimaryBlueDark,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = t("Akan dibuat sebagai satu tab di file Excel."),
                  style = MaterialTheme.typography.bodySmall,
                  color = SubtleInk
                )
              }
            }
          }
        }
        if (isExporting) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = HighlightCard
            )
            Text(
              text = t("Menyiapkan data dan file Excel..."),
              style = MaterialTheme.typography.bodySmall,
              color = SubtleInk
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        enabled = !isExporting && selectedMonthKeys.isNotEmpty(),
        onClick = onConfirm
      ) {
        Text(t(if (shouldShare) "Kirim" else "Cetak"))
      }
    },
    dismissButton = {
      TextButton(
        enabled = !isExporting,
        onClick = onDismiss
      ) {
        Text(t("Batal"))
      }
    }
  )
}

@Composable
private fun MutabaahPeriodShortcut(
  label: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(if (selected) HighlightCard else SoftPanel.copy(alpha = 0.78f))
      .border(
        width = 1.dp,
        color = if (selected) HighlightCard else CardBorder,
        shape = RoundedCornerShape(999.dp)
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 11.dp, vertical = 8.dp)
  ) {
    Text(
      text = t(label),
      style = MaterialTheme.typography.labelMedium,
      color = if (selected) Color.White else PrimaryBlueDark,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun MutabaahHeader(
  selectedDate: LocalDate,
  onJumpToToday: () -> Unit,
  onMenuClick: () -> Unit,
  isExporting: Boolean,
  onExportClick: (Boolean) -> Unit
) {
  var actionMenuExpanded by rememberSaveable { mutableStateOf(false) }
  val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
  val title = if (selectedDate == LocalDate.now()) {
    "Mutabaah Hari Ini"
  } else {
    selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.Top
  ) {
    MutabaahCircleButton(
      icon = Icons.Outlined.Menu,
      contentDescription = t("Buka sidebar"),
      onClick = onMenuClick
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = dateFormatter.format(selectedDate),
        style = MaterialTheme.typography.bodyMedium,
        color = SubtleInk,
        textAlign = TextAlign.Center
      )
      Text(
        text = t(title),
        style = MaterialTheme.typography.headlineSmall,
        color = HighlightCard,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp)
      )
    }

    Box {
      MutabaahCircleButton(
        icon = Icons.Outlined.MoreVert,
        contentDescription = t("Menu mutabaah"),
        onClick = { actionMenuExpanded = true }
      )
      DropdownMenu(
        expanded = actionMenuExpanded,
        onDismissRequest = { actionMenuExpanded = false },
        modifier = Modifier.background(CardBackground)
      ) {
        DropdownMenuItem(
          text = { Text(t("Cetak Mutabaah")) },
          enabled = !isExporting,
          onClick = {
            actionMenuExpanded = false
            onExportClick(false)
          }
        )
        DropdownMenuItem(
          text = { Text(t("Kirim Mutabaah")) },
          enabled = !isExporting,
          onClick = {
            actionMenuExpanded = false
            onExportClick(true)
          }
        )
      }
    }
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 12.dp),
    horizontalArrangement = Arrangement.End
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(14.dp))
        .background(CardBackground.copy(alpha = 0.88f))
        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
        .clickable(onClick = onJumpToToday)
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      Text(
        text = t("Hari ini"),
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
private fun MutabaahWeekSwitcher(
  selectedDate: LocalDate,
  onPreviousWeek: () -> Unit,
  onNextWeek: () -> Unit
) {
  val startOfWeek = selectedDate.minusDays((selectedDate.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
  val endOfWeek = startOfWeek.plusDays(6)
  val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale("id", "ID"))

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground.copy(alpha = 0.86f), RoundedCornerShape(20.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    MutabaahCircleButton(
      icon = Icons.Outlined.ChevronLeft,
      contentDescription = t("Minggu sebelumnya"),
      onClick = onPreviousWeek
    )
    Text(
      text = "${formatter.format(startOfWeek)} - ${formatter.format(endOfWeek)}",
      style = MaterialTheme.typography.titleSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    MutabaahCircleButton(
      icon = Icons.Outlined.ChevronRight,
      contentDescription = t("Minggu berikutnya"),
      onClick = onNextWeek
    )
  }
}

@Composable
private fun MutabaahSummaryCard(
  total: Int,
  done: Int,
  isSunday: Boolean,
  isHoliday: Boolean,
  isApprovedLeave: Boolean,
  isOfficeOnlyDay: Boolean
) {
  val label = when {
    isSunday -> "Ahad libur"
    isHoliday -> "Libur akademik"
    isApprovedLeave -> "Izin disetujui - tidak dihitung progress"
    isOfficeOnlyDay -> "$done dari $total opsi kantor selesai"
    total == 0 -> "Belum ada tugas"
    else -> "$done dari $total selesai"
  }
  val percent = if (total <= 0) 0 else ((done.toFloat() / total.toFloat()) * 100).toInt()
  val scoreText = if (isApprovedLeave) t("Izin") else "$percent%"
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x120F172A), spotColor = Color(0x120F172A))
      .clip(RoundedCornerShape(22.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(22.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(
          text = t("Progress Mutabaah"),
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = t(label),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
      Text(
        text = scoreText,
        style = MaterialTheme.typography.titleLarge,
        color = if (isApprovedLeave) WarmAccent else HighlightCard,
        fontWeight = FontWeight.ExtraBold
      )
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(9.dp)
        .clip(RoundedCornerShape(999.dp))
        .background(SoftPanel)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
          .height(9.dp)
          .background(HighlightCard, RoundedCornerShape(999.dp))
      )
    }
  }
}

@Composable
private fun MutabaahTimelineRow(
  task: MutabaahTask,
  checked: Boolean,
  submission: MutabaahSubmission?,
  isSaving: Boolean,
  isLast: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Column(
      modifier = Modifier.width(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(30.dp)
          .shadow(8.dp, CircleShape, ambientColor = Color(0x180F172A), spotColor = Color(0x180F172A))
          .clip(CircleShape)
          .background(if (checked) HighlightCard else CardBackground)
          .border(
            2.dp,
            if (checked) HighlightCard.copy(alpha = 0.28f) else WarmAccent.copy(alpha = 0.62f),
            CircleShape
          )
          .clickable(enabled = !isSaving) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
      ) {
        when {
          isSaving -> CircularProgressIndicator(
            color = if (checked) Color.White else HighlightCard,
            strokeWidth = 2.dp,
            modifier = Modifier.size(14.dp)
          )

          checked -> Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = t("Selesai"),
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )

          else -> Box(
            modifier = Modifier
              .size(8.dp)
              .background(WarmAccent, CircleShape)
          )
        }
      }
      if (!isLast) {
        Spacer(
          modifier = Modifier
            .padding(top = 4.dp)
            .width(2.dp)
            .height(96.dp)
            .background((if (checked) HighlightCard else WarmAccent).copy(alpha = 0.24f), RoundedCornerShape(999.dp))
        )
      }
    }

    Box(
      modifier = Modifier
        .weight(1f)
        .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
        .clip(RoundedCornerShape(24.dp))
        .border(
          1.dp,
          if (checked) HighlightCard.copy(alpha = 0.18f) else WarmAccent.copy(alpha = 0.26f),
          RoundedCornerShape(24.dp)
        )
        .animateContentSize()
    ) {
      MutabaahGlassBackground(
        tint = if (checked) SuccessTint else Color(0xFFFFF1D8),
        accent = if (checked) HighlightCard else WarmAccent
      )
      Box(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 1.dp)
          .width(5.dp)
          .height(74.dp)
          .clip(RoundedCornerShape(999.dp))
          .background((if (checked) HighlightCard else WarmAccent).copy(alpha = 0.64f))
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 18.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.Top
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(CardBackground.copy(alpha = 0.62f))
              .border(1.dp, CardBorder.copy(alpha = 0.58f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.TaskAlt,
              contentDescription = null,
              tint = if (checked) HighlightCard else WarmAccent,
              modifier = Modifier.size(20.dp)
            )
          }
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Text(
              text = task.title,
              style = MaterialTheme.typography.titleSmall,
              color = PrimaryBlueDark,
              fontWeight = FontWeight.ExtraBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (task.description.isNotBlank()) {
              Text(
                text = task.description,
                style = MaterialTheme.typography.bodySmall,
                color = SubtleInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .background(CardBackground.copy(alpha = 0.68f))
              .border(1.dp, CardBorder.copy(alpha = 0.58f), RoundedCornerShape(999.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = when {
                isSaving -> "Menyinkronkan..."
                submission?.syncState == "pending" -> "Belum sync"
                checked -> "Tersimpan otomatis"
                else -> "Ketuk titik untuk selesai"
              },
              style = MaterialTheme.typography.labelMedium,
              color = when {
                isSaving -> WarmAccent
                submission?.syncState == "pending" -> WarmAccent
                checked -> HighlightCard
                else -> SubtleInk
              },
              fontWeight = FontWeight.SemiBold
            )
          }
          Text(
            text = if (checked) "Selesai" else "Belum",
            style = MaterialTheme.typography.labelMedium,
            color = if (checked) HighlightCard else WarmAccent,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}

@Composable
private fun MutabaahEmptyCard(message: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(CardBackground.copy(alpha = 0.84f))
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .padding(18.dp)
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = SubtleInk
    )
  }
}

@Composable
private fun MutabaahCircleButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
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

private fun List<MutabaahTask>.toCalendarEvents(): List<CalendarEvent> {
  return filter { it.active }.map { task ->
    CalendarEvent(
      id = "mutabaah-${task.id}",
      startDateIso = task.dateIso,
      endDateIso = task.dateIso,
      title = task.title,
      description = task.description,
      timeLabel = "",
      colorHex = "#CFF7D3",
      categoryKey = "mutabaah"
    )
  }
}

private fun List<MutabaahTask>.officeOnlyVisibleTasks(): List<MutabaahTask> {
  val hadirTask = firstOrNull { it.title.isArrivalTaskTitle() }
  val pulangTask = firstOrNull { it.title.isDepartureTaskTitle() && it.id != hadirTask?.id }
  val matched = listOfNotNull(
    hadirTask?.copy(
      title = "Hadir tepat waktu",
      description = hadirTask.description.ifBlank { "Tandai jika guru hadir berkantor tepat waktu." }
    ),
    pulangTask?.copy(
      title = "Pulang tepat waktu",
      description = pulangTask.description.ifBlank { "Tandai jika guru pulang sesuai ketentuan waktu." }
    )
  )
  if (matched.isNotEmpty()) return matched
  return take(2).mapIndexed { index, task ->
    task.copy(
      title = if (index == 0) "Hadir tepat waktu" else "Pulang tepat waktu",
      description = if (index == 0) {
        "Tandai jika guru hadir berkantor tepat waktu."
      } else {
        "Tandai jika guru pulang sesuai ketentuan waktu."
      }
    )
  }
}

private fun String.isArrivalTaskTitle(): Boolean {
  val value = lowercase(Locale.getDefault())
  return ("hadir" in value || "masuk" in value || "datang" in value) &&
    ("pulang" !in value)
}

private fun String.isDepartureTaskTitle(): Boolean {
  val value = lowercase(Locale.getDefault())
  return "pulang" in value || "keluar" in value
}

private fun CalendarEvent.coversDate(date: LocalDate): Boolean {
  val start = parseMutabaahDate(startDateIso) ?: date
  val end = parseMutabaahDate(endDateIso.ifBlank { startDateIso }) ?: start
  return !date.isBefore(start) && !date.isAfter(end)
}

private fun LeaveRequestItem.isApprovedOn(date: LocalDate): Boolean {
  if (status.trim().lowercase(Locale.getDefault()) != "diterima") return false
  val start = parseMutabaahDate(startDateIso) ?: return false
  val end = parseMutabaahDate(endDateIso.ifBlank { startDateIso }) ?: start
  return !date.isBefore(start) && !date.isAfter(end)
}

private fun parseMutabaahDate(value: String): LocalDate? {
  return runCatching { LocalDate.parse(value.trim().take(10)) }.getOrNull()
}

private fun YearMonth.academicYearMonths(): List<YearMonth> {
  val startYear = if (monthValue >= 7) year else year - 1
  val start = YearMonth.of(startYear, 7)
  return (0 until 12).map { start.plusMonths(it.toLong()) }
}

private fun YearMonth.semesterMonths(): List<YearMonth> {
  val start = if (monthValue >= 7) YearMonth.of(year, 7) else YearMonth.of(year, 1)
  return (0 until 6).map { start.plusMonths(it.toLong()) }
}

private fun YearMonth.formatMonthTitle(): String {
  val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID"))
  return formatter.format(atDay(1))
}

@Composable
private fun BoxScope.MutabaahGlassBackground(
  tint: Color,
  accent: Color
) {
  Box(
    modifier = Modifier
      .matchParentSize()
      .graphicsLayer {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          renderEffect = RenderEffect
            .createBlurEffect(14f, 14f, Shader.TileMode.CLAMP)
            .asComposeRenderEffect()
        }
      }
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(
            tint.copy(alpha = 0.88f),
            tint.copy(alpha = 0.74f),
            CardGradientEnd.copy(alpha = 0.9f)
          )
        ),
        shape = RoundedCornerShape(24.dp)
      )
      .border(1.dp, accent.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
  )
}
