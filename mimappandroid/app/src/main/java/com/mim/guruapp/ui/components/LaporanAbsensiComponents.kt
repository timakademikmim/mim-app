package com.mim.guruapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.model.AttendanceHistoryEntry
import com.mim.guruapp.data.model.AttendanceStudent
import com.mim.guruapp.data.model.MapelAttendanceSnapshot
import com.mim.guruapp.data.model.MonthlyAttendanceSummary
import com.mim.guruapp.data.model.MonthlyReportSnapshot
import com.mim.guruapp.data.model.WaliAttendanceDetailSnapshot
import com.mim.guruapp.data.model.WaliSantriProfile
import com.mim.guruapp.data.model.WaliSantriSnapshot
import com.mim.guruapp.export.MapelAttendanceExcelExporter
import com.mim.guruapp.ui.i18n.t
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.HighlightCard
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanAbsensiScreen(
  waliSantriSnapshot: WaliSantriSnapshot,
  monthlyReportSnapshot: MonthlyReportSnapshot,
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadAttendanceSummaries: suspend (String) -> List<MonthlyAttendanceSummary>,
  onLoadAttendanceDetail: suspend (String, WaliSantriProfile) -> WaliAttendanceDetailSnapshot?,
  modifier: Modifier = Modifier
) {
  val periods = remember(monthlyReportSnapshot.availablePeriods) {
    monthlyReportSnapshot.availablePeriods.ifEmpty { buildAttendancePeriods() }
  }
  var selectedPeriod by rememberSaveable { mutableStateOf(periods.firstOrNull().orEmpty()) }
  var selectedStudentId by rememberSaveable { mutableStateOf<String?>(null) }
  var isPeriodMenuOpen by remember { mutableStateOf(false) }
  var isActionMenuOpen by remember { mutableStateOf(false) }
  var exportShouldShare by rememberSaveable { mutableStateOf<Boolean?>(null) }
  var exportPeriodKeys by rememberSaveable { mutableStateOf(listOf<String>()) }
  var isExportingAttendance by rememberSaveable { mutableStateOf(false) }
  var loadingStudentId by rememberSaveable { mutableStateOf<String?>(null) }
  var isAttendanceLoading by rememberSaveable { mutableStateOf(false) }
  val detailCache = remember { mutableStateMapOf<String, WaliAttendanceDetailSnapshot>() }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  var liveAttendanceSummaries by remember(monthlyReportSnapshot.updatedAt) {
    mutableStateOf(monthlyReportSnapshot.attendanceSummaries)
  }

  LaunchedEffect(periods) {
    if (selectedPeriod !in periods) {
      selectedPeriod = periods.firstOrNull().orEmpty()
      selectedStudentId = null
    }
    if (exportPeriodKeys.isEmpty()) {
      exportPeriodKeys = listOfNotNull(periods.firstOrNull()).ifEmpty { listOf(selectedPeriod).filter { it.isNotBlank() } }
    }
  }

  val students = remember(waliSantriSnapshot.students) {
    waliSantriSnapshot.students.sortedWith(compareBy<WaliSantriProfile> { it.className }.thenBy { it.name })
  }
  LaunchedEffect(selectedPeriod, monthlyReportSnapshot.updatedAt, waliSantriSnapshot.updatedAt) {
    if (selectedPeriod.isBlank()) return@LaunchedEffect
    isAttendanceLoading = true
    val loaded = onLoadAttendanceSummaries(selectedPeriod)
    liveAttendanceSummaries = if (loaded.isNotEmpty()) {
      monthlyReportSnapshot.attendanceSummaries
        .filterNot { it.period == selectedPeriod } + loaded
    } else {
      monthlyReportSnapshot.attendanceSummaries
    }
    isAttendanceLoading = false
  }
  val attendanceByStudent = remember(liveAttendanceSummaries, selectedPeriod) {
    liveAttendanceSummaries
      .filter { it.period == selectedPeriod }
      .associateBy { it.studentId }
  }
  val selectedStudent = students.firstOrNull { it.id == selectedStudentId }
  val selectedDetail = selectedStudent?.let { detailCache["$selectedPeriod|${it.id}"] }

  fun exportAttendanceReport(shouldShare: Boolean, periodKeys: List<String>) {
    val cleanPeriods = periodKeys.filter { it.isNotBlank() }.distinct()
    if (cleanPeriods.isEmpty()) {
      scope.launch { snackbarHostState.showSnackbar("Pilih minimal satu periode.") }
      return
    }
    scope.launch {
      isExportingAttendance = true
      runCatching {
        val loadedSummaries = mutableListOf<MonthlyAttendanceSummary>()
        val worksheets = cleanPeriods.mapNotNull { periodKey ->
          val period = runCatching { YearMonth.parse(periodKey) }.getOrNull() ?: return@mapNotNull null
          val remoteSummaries = onLoadAttendanceSummaries(periodKey)
          loadedSummaries += remoteSummaries
          val summariesForPeriod = (liveAttendanceSummaries + remoteSummaries)
            .filter { it.period == periodKey }
            .associateBy { it.studentId }
          val detailByStudent = students.associate { student ->
            val cacheKey = "$periodKey|${student.id}"
            val detail = detailCache[cacheKey] ?: onLoadAttendanceDetail(periodKey, student)?.also { loaded ->
              detailCache[cacheKey] = loaded
            }
            student.id to detail
          }
          MapelAttendanceExcelExporter.AttendanceWorksheet(
            period = period,
            snapshot = buildLaporanAbsensiExportSnapshot(
              periodKey = periodKey,
              students = students,
              summaries = summariesForPeriod,
              detailByStudent = detailByStudent,
              classNames = waliSantriSnapshot.classes.map { it.name }
            ),
            reportTitle = "LAPORAN ABSENSI KELAS",
            subjectLabel = "Cakupan"
          )
        }
        if (worksheets.isEmpty()) error("Periode laporan tidak valid.")
        if (loadedSummaries.isNotEmpty()) {
          liveAttendanceSummaries = liveAttendanceSummaries
            .filterNot { summary -> loadedSummaries.any { it.period == summary.period && it.studentId == summary.studentId } } +
            loadedSummaries
        }
        val file = MapelAttendanceExcelExporter.createWorkbook(context, worksheets)
        if (shouldShare) {
          MapelAttendanceExcelExporter.shareWorkbook(context, file)
        } else {
          MapelAttendanceExcelExporter.openWorkbook(context, file)
        }
      }.onSuccess {
        exportShouldShare = null
      }.onFailure { error ->
        snackbarHostState.showSnackbar(error.message ?: "Gagal membuat laporan absensi.")
      }
      isExportingAttendance = false
    }
  }

  BackHandler(enabled = selectedStudent != null) {
    selectedStudentId = null
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
  ) { innerPadding ->
    PullToRefreshBox(
      isRefreshing = isRefreshing,
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        LaporanAbsensiTopBar(
          isDetail = selectedStudent != null,
          title = if (selectedStudent == null) "Laporan Absensi" else "Detail Absensi",
          subtitle = if (selectedStudent == null) selectedPeriod.toPeriodLabel() else selectedStudent.name,
          onPrimaryClick = {
            if (selectedStudent == null) onMenuClick() else selectedStudentId = null
          },
          actions = if (selectedStudent == null && waliSantriSnapshot.isWaliKelas) {
            {
              Box {
                LaporanAbsensiCircleButton(
                  icon = Icons.Outlined.MoreVert,
                  contentDescription = t("Menu laporan absensi"),
                  onClick = { isActionMenuOpen = true }
                )
                DropdownMenu(
                  expanded = isActionMenuOpen,
                  onDismissRequest = { isActionMenuOpen = false },
                  modifier = Modifier.background(CardBackground)
                ) {
                  DropdownMenuItem(
                    text = { Text(t("Cetak Absensi")) },
                    enabled = !isExportingAttendance && students.isNotEmpty(),
                    onClick = {
                      isActionMenuOpen = false
                      exportPeriodKeys = listOf(selectedPeriod).filter { it.isNotBlank() }
                      exportShouldShare = false
                    }
                  )
                  DropdownMenuItem(
                    text = { Text(t("Kirim Absensi")) },
                    enabled = !isExportingAttendance && students.isNotEmpty(),
                    onClick = {
                      isActionMenuOpen = false
                      exportPeriodKeys = listOf(selectedPeriod).filter { it.isNotBlank() }
                      exportShouldShare = true
                    }
                  )
                }
              }
            }
          } else {
            null
          }
        )

        if (!waliSantriSnapshot.isWaliKelas) {
          LaporanAbsensiEmptyCard("Menu ini hanya tersedia untuk wali kelas.")
          return@PullToRefreshBox
        }

        if (selectedStudent == null) {
          LaporanAbsensiPeriodPicker(
            selectedPeriod = selectedPeriod,
            periods = periods,
            expanded = isPeriodMenuOpen,
            onExpandedChange = { isPeriodMenuOpen = it }
          ) { selected ->
            selectedPeriod = selected
            selectedStudentId = null
          }

          LaporanAbsensiInfoCard(
            classNames = waliSantriSnapshot.classes.map { it.name },
            studentCount = students.size
          )

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 124.dp)
          ) {
            if (isAttendanceLoading) {
              items(5) {
                SkeletonContentCard(
                  leadingSize = 42.dp,
                  trailingSize = 36.dp,
                  firstLineWidth = 0.52f,
                  secondLineWidth = 0.84f
                )
              }
            } else if (students.isEmpty()) {
              item {
                LaporanAbsensiEmptyCard("Belum ada santri aktif pada kelas yang Anda naungi.")
              }
            } else {
              items(students) { student ->
                val summary = attendanceByStudent[student.id]
                LaporanAbsensiStudentCard(
                  student = student,
                  summary = summary,
                  isLoading = loadingStudentId == student.id
                ) {
                  scope.launch {
                    selectedStudentId = student.id
                    val cacheKey = "$selectedPeriod|${student.id}"
                    if (detailCache[cacheKey] == null) {
                      loadingStudentId = student.id
                      val detail = onLoadAttendanceDetail(selectedPeriod, student)
                      loadingStudentId = null
                      if (detail != null) {
                        detailCache[cacheKey] = detail
                      } else {
                        selectedStudentId = null
                        snackbarHostState.showSnackbar("Detail absensi belum bisa dimuat.")
                      }
                    }
                  }
                }
              }
            }
          }
        } else {
          val cacheKey = "$selectedPeriod|${selectedStudent.id}"
          LaunchedEffect(cacheKey) {
            if (detailCache[cacheKey] == null) {
              loadingStudentId = selectedStudent.id
              val detail = onLoadAttendanceDetail(selectedPeriod, selectedStudent)
              loadingStudentId = null
              if (detail != null) {
                detailCache[cacheKey] = detail
              } else {
                snackbarHostState.showSnackbar("Detail absensi belum bisa dimuat.")
              }
            }
          }

          LaporanAbsensiDetailHeader(
            student = selectedStudent,
            period = selectedPeriod,
            summary = attendanceByStudent[selectedStudent.id]
          )

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 124.dp)
          ) {
            when {
              loadingStudentId == selectedStudent.id && selectedDetail == null -> {
                items(4) {
                  SkeletonContentCard(
                    leadingSize = 42.dp,
                    trailingSize = 36.dp,
                    firstLineWidth = 0.55f,
                    secondLineWidth = 0.8f
                  )
                }
              }

              selectedDetail?.rows.isNullOrEmpty() -> {
                item {
                  LaporanAbsensiEmptyCard("Belum ada data absensi pada periode ini.")
                }
              }

              else -> {
                items(selectedDetail?.rows.orEmpty(), key = { it.id }) { row ->
                  LaporanAbsensiDetailRowCard(row.dateIso, row.subjectName, row.lessonLabel, row.status)
                }
              }
            }
          }
        }
      }
        exportShouldShare?.let { shouldShare ->
          LaporanAbsensiExportPeriodDialog(
            shouldShare = shouldShare,
            currentMonth = runCatching { YearMonth.parse(selectedPeriod) }.getOrNull() ?: YearMonth.now(),
            selectedPeriodKeys = exportPeriodKeys,
            isExporting = isExportingAttendance,
          onSelectedPeriodKeysChange = { exportPeriodKeys = it },
          onDismiss = {
            if (!isExportingAttendance) exportShouldShare = null
          },
          onConfirm = {
            exportAttendanceReport(shouldShare, exportPeriodKeys)
          }
        )
      }
    }
  }
}

@Composable
private fun LaporanAbsensiTopBar(
  isDetail: Boolean,
  title: String,
  subtitle: String,
  onPrimaryClick: () -> Unit,
  actions: (@Composable RowScope.() -> Unit)? = null
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.Top
  ) {
    LaporanAbsensiCircleButton(
      icon = if (isDetail) Icons.AutoMirrored.Outlined.ArrowBack else Icons.Outlined.Menu,
      contentDescription = if (isDetail) t("Kembali ke daftar absensi") else t("Buka sidebar"),
      onClick = onPrimaryClick
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = t(subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = SubtleInk,
        textAlign = TextAlign.Center
      )
      Text(
        text = t(title),
        style = MaterialTheme.typography.headlineSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center
      )
    }
    if (actions != null) {
      Row(content = actions)
    } else {
      Box(modifier = Modifier.size(42.dp))
    }
  }
}

@Composable
private fun LaporanAbsensiExportPeriodDialog(
  shouldShare: Boolean,
  currentMonth: YearMonth,
  selectedPeriodKeys: List<String>,
  isExporting: Boolean,
  onSelectedPeriodKeysChange: (List<String>) -> Unit,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  val monthOptions = remember(currentMonth) { currentMonth.laporanAbsensiAcademicYearMonths() }
  val semesterMonths = remember(currentMonth) { currentMonth.laporanAbsensiSemesterMonths() }
  val selectedSet = selectedPeriodKeys.toSet()
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = CardBackground,
    title = {
      Text(
        text = t(if (shouldShare) "Kirim Absensi" else "Cetak Absensi"),
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
          LaporanAbsensiPeriodShortcut(
            label = "Bulan Ini",
            selected = selectedSet == setOf(currentMonth.toString()),
            enabled = !isExporting,
            onClick = { onSelectedPeriodKeysChange(listOf(currentMonth.toString())) }
          )
          LaporanAbsensiPeriodShortcut(
            label = "Semester",
            selected = selectedSet == semesterMonths.map { it.toString() }.toSet(),
            enabled = !isExporting,
            onClick = { onSelectedPeriodKeysChange(semesterMonths.map { it.toString() }) }
          )
          LaporanAbsensiPeriodShortcut(
            label = "Tahun",
            selected = selectedSet == monthOptions.map { it.toString() }.toSet(),
            enabled = !isExporting,
            onClick = { onSelectedPeriodKeysChange(monthOptions.map { it.toString() }) }
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
            val period = month.toString()
            val checked = period in selectedSet
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
                  onSelectedPeriodKeysChange(
                    if (checked) selectedPeriodKeys.filterNot { it == period } else (selectedPeriodKeys + period).distinct().sorted()
                  )
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = checked,
                enabled = !isExporting,
                onCheckedChange = { isChecked ->
                  onSelectedPeriodKeysChange(
                    if (isChecked) (selectedPeriodKeys + period).distinct().sorted() else selectedPeriodKeys.filterNot { it == period }
                  )
                }
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = month.laporanAbsensiMonthTitle(),
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryBlue)
            Text(
              text = t("Menyiapkan data dan file Excel..."),
              color = SubtleInk,
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        enabled = !isExporting && selectedPeriodKeys.isNotEmpty(),
        onClick = onConfirm
      ) {
        Text(t(if (shouldShare) "Kirim" else "Cetak"))
      }
    },
    dismissButton = {
      TextButton(enabled = !isExporting, onClick = onDismiss) {
        Text(t("Batal"))
      }
    }
  )
}

@Composable
private fun LaporanAbsensiPeriodShortcut(
  label: String,
  selected: Boolean,
  enabled: Boolean,
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
      .clickable(enabled = enabled, onClick = onClick)
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
private fun LaporanAbsensiPeriodPicker(
  selectedPeriod: String,
  periods: List<String>,
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  onSelectPeriod: (String) -> Unit
) {
  Box {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(CardBackground)
        .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
        .clickable { onExpandedChange(true) }
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(Color(0xFFE0ECFF)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.CalendarMonth,
          contentDescription = null,
          tint = PrimaryBlueDark
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = t("Periode"),
          style = MaterialTheme.typography.labelMedium,
          color = SubtleInk
        )
        Text(
          text = selectedPeriod.toPeriodLabel(),
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.Bold
        )
      }
      Icon(
        imageVector = Icons.Outlined.KeyboardArrowDown,
        contentDescription = null,
        tint = PrimaryBlueDark
      )
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { onExpandedChange(false) }
    ) {
      periods.forEach { period ->
        DropdownMenuItem(
          text = { Text(period.toPeriodLabel()) },
          onClick = {
            onExpandedChange(false)
            onSelectPeriod(period)
          }
        )
      }
    }
  }
}

@Composable
private fun LaporanAbsensiInfoCard(
  classNames: List<String>,
  studentCount: Int
) {
  val classLabel = classNames
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .distinct()
    .joinToString(", ")
    .ifBlank { "-" }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(SoftPanel)
      .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
      .padding(16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    LaporanAbsensiMiniStat("Kelas", classLabel, Color(0xFFDDEBFF))
    LaporanAbsensiMiniStat("Santri", studentCount.toString(), Color(0xFFE6F7E8))
  }
}

@Composable
private fun RowScope.LaporanAbsensiMiniStat(
  label: String,
  value: String,
  tone: Color
) {
  Column(
    modifier = Modifier
      .weight(1f)
      .clip(RoundedCornerShape(18.dp))
      .background(tone)
      .padding(horizontal = 14.dp, vertical = 12.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = SubtleInk
    )
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun LaporanAbsensiStudentCard(
  student: WaliSantriProfile,
  summary: MonthlyAttendanceSummary?,
  isLoading: Boolean,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(CardBackground)
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .clickable(onClick = onClick)
      .padding(16.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(Color(0xFFE0ECFF)),
      contentAlignment = Alignment.Center
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          strokeWidth = 2.dp,
          color = PrimaryBlue
        )
      } else {
        Icon(
          imageVector = Icons.Outlined.Person,
          contentDescription = null,
          tint = PrimaryBlueDark
        )
      }
    }

    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = student.name.ifBlank { "-" },
        style = MaterialTheme.typography.titleMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = "NISN ${student.nisn.ifBlank { "-" }}",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LaporanAbsensiStatusPill("Hadir", "${summary?.hadirCount ?: 0}", Color(0xFFE6F7E8), Color(0xFF166534))
        LaporanAbsensiStatusPill("Sakit", "${summary?.sakitCount ?: 0}", Color(0xFFF2E8FF), Color(0xFF7C3AED))
        LaporanAbsensiStatusPill("Izin", "${summary?.izinCount ?: 0}", Color(0xFFE0ECFF), Color(0xFF1D4ED8))
        LaporanAbsensiStatusPill("Alpa", "${summary?.alpaCount ?: 0}", Color(0xFFFFE4E6), Color(0xFFBE123C))
      }
      Text(
        text = "${t("Kehadiran")} ${summary?.attendancePercent.withPercentSuffix()}",
        style = MaterialTheme.typography.labelMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
private fun LaporanAbsensiStatusPill(
  label: String,
  value: String,
  background: Color,
  content: Color
) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(background)
      .padding(horizontal = 10.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = content
    )
    Text(
      text = value.ifBlank { "-" },
      style = MaterialTheme.typography.labelMedium,
      color = content,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
private fun LaporanAbsensiDetailHeader(
  student: WaliSantriProfile,
  period: String,
  summary: MonthlyAttendanceSummary?
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(CardBackground)
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .padding(18.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = student.name.ifBlank { "-" },
      style = MaterialTheme.typography.headlineSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = "${student.className.ifBlank { "-" }} • ${period.toPeriodLabel()}",
      style = MaterialTheme.typography.bodyMedium,
      color = SubtleInk
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      LaporanAbsensiStatusPill("Hadir", "${summary?.hadirCount ?: 0}", Color(0xFFE6F7E8), Color(0xFF166534))
      LaporanAbsensiStatusPill("Sakit", "${summary?.sakitCount ?: 0}", Color(0xFFF2E8FF), Color(0xFF7C3AED))
      LaporanAbsensiStatusPill("Izin", "${summary?.izinCount ?: 0}", Color(0xFFE0ECFF), Color(0xFF1D4ED8))
      LaporanAbsensiStatusPill("Alpa", "${summary?.alpaCount ?: 0}", Color(0xFFFFE4E6), Color(0xFFBE123C))
    }
  }
}

@Composable
private fun LaporanAbsensiDetailRowCard(
  dateIso: String,
  subjectName: String,
  lessonLabel: String,
  status: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(CardBackground)
      .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
      .padding(16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0xFFF3F4F6)),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = dateIso.takeLast(2),
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = dateIso,
        style = MaterialTheme.typography.labelMedium,
        color = SubtleInk
      )
      Text(
        text = subjectName.ifBlank { "-" },
        style = MaterialTheme.typography.bodyMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (lessonLabel.isNotBlank()) {
        Text(
          text = lessonLabel,
          style = MaterialTheme.typography.labelSmall,
          color = SubtleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    Text(
      text = status.ifBlank { "-" },
      style = MaterialTheme.typography.labelLarge,
      color = attendanceStatusColor(status),
      fontWeight = FontWeight.ExtraBold,
      textAlign = TextAlign.End
    )
  }
}

@Composable
private fun LaporanAbsensiEmptyCard(message: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(CardBackground)
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .padding(horizontal = 18.dp, vertical = 22.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = SubtleInk,
      textAlign = TextAlign.Center
    )
  }
}

@Composable
private fun LaporanAbsensiCircleButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(42.dp)
      .clip(CircleShape)
      .background(CardBackground.copy(alpha = 0.86f))
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

private fun buildAttendancePeriods(): List<String> {
  val start = YearMonth.now().minusMonths(1)
  return (0 until 12).map { offset -> start.minusMonths(offset.toLong()).toString() }
}

private fun YearMonth.laporanAbsensiSemesterMonths(): List<YearMonth> {
  val firstMonth = if (monthValue in 7..12) 7 else 1
  return (0 until 6).map { plusMonths((firstMonth - monthValue + it).toLong()) }
}

private fun YearMonth.laporanAbsensiAcademicYearMonths(): List<YearMonth> {
  val first = if (monthValue >= 7) YearMonth.of(year, 7) else YearMonth.of(year - 1, 7)
  return (0 until 12).map { first.plusMonths(it.toLong()) }
}

private fun YearMonth.laporanAbsensiMonthTitle(): String {
  val monthName = month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("id-ID"))
  return "$monthName $year"
}

private fun buildLaporanAbsensiExportSnapshot(
  periodKey: String,
  students: List<WaliSantriProfile>,
  summaries: Map<String, MonthlyAttendanceSummary>,
  detailByStudent: Map<String, WaliAttendanceDetailSnapshot?>,
  classNames: List<String>
): MapelAttendanceSnapshot {
  val classLabel = classNames
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .distinct()
    .joinToString(", ")
    .ifBlank { students.firstOrNull()?.className.orEmpty().ifBlank { "Kelas" } }
  return MapelAttendanceSnapshot(
    distribusiId = "wali-$periodKey",
    subjectTitle = "Semua Mapel",
    className = classLabel,
    rangeLabel = periodKey.toPeriodLabel(),
    students = students.sortedBy { it.name.lowercase(Locale.getDefault()) }.map { student ->
      val summary = summaries[student.id]
      val history = detailByStudent[student.id]?.rows.orEmpty().map { row ->
        AttendanceHistoryEntry(
          dateIso = row.dateIso,
          status = row.status,
          rowIds = listOf(row.id).filter { it.isNotBlank() },
          lessonSlotId = row.lessonSortKey,
          patronMateri = listOf(row.subjectName, row.lessonLabel)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
        )
      }
      AttendanceStudent(
        id = student.id,
        name = student.name,
        hadirPercent = summary?.attendancePercent.toPercentInt(),
        terlambatPercent = 0,
        sakitPercent = summary?.sakitCount.toPercentInt(summary?.totalDays ?: 0),
        izinPercent = summary?.izinCount.toPercentInt(summary?.totalDays ?: 0),
        alpaPercent = summary?.alpaCount.toPercentInt(summary?.totalDays ?: 0),
        history = history
      )
    },
    updatedAt = System.currentTimeMillis()
  )
}

private fun String?.toPercentInt(): Int {
  val value = this?.trim().orEmpty().removeSuffix("%").replace(',', '.')
  return value.toDoubleOrNull()?.toInt() ?: 0
}

private fun Int?.toPercentInt(total: Int): Int {
  val count = this ?: 0
  if (total <= 0) return 0
  return ((count.toDouble() / total.toDouble()) * 100.0).toInt()
}

private fun String.toPeriodLabel(): String {
  val month = runCatching { YearMonth.parse(this) }.getOrNull() ?: return ifBlank { "Periode" }
  val monthName = month.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("id-ID"))
  return "$monthName ${month.year}"
}

private fun String?.withPercentSuffix(): String {
  val value = this?.trim().orEmpty()
  if (value.isBlank()) return "-"
  return if (value.endsWith("%")) value else "$value%"
}

private fun attendanceStatusColor(status: String): Color {
  return when (status.trim().lowercase(Locale.getDefault())) {
    "hadir", "terlambat" -> Color(0xFF166534)
    "sakit" -> Color(0xFF7C3AED)
    "izin" -> Color(0xFF1D4ED8)
    "alpa" -> Color(0xFFBE123C)
    else -> Color(0xFF0F172A)
  }
}
