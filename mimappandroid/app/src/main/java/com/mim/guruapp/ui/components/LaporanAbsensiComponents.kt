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
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.model.MonthlyAttendanceSummary
import com.mim.guruapp.data.model.MonthlyReportSnapshot
import com.mim.guruapp.data.model.WaliAttendanceDetailSnapshot
import com.mim.guruapp.data.model.WaliSantriProfile
import com.mim.guruapp.data.model.WaliSantriSnapshot
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
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
  var loadingStudentId by rememberSaveable { mutableStateOf<String?>(null) }
  var isAttendanceLoading by rememberSaveable { mutableStateOf(false) }
  val detailCache = remember { mutableStateMapOf<String, WaliAttendanceDetailSnapshot>() }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var liveAttendanceSummaries by remember(monthlyReportSnapshot.updatedAt) {
    mutableStateOf(monthlyReportSnapshot.attendanceSummaries)
  }

  LaunchedEffect(periods) {
    if (selectedPeriod !in periods) {
      selectedPeriod = periods.firstOrNull().orEmpty()
      selectedStudentId = null
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
                  LaporanAbsensiDetailRowCard(row.dateIso, row.subjectName, row.status)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LaporanAbsensiTopBar(
  isDetail: Boolean,
  title: String,
  subtitle: String,
  onPrimaryClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.Top
  ) {
    LaporanAbsensiCircleButton(
      icon = if (isDetail) Icons.AutoMirrored.Outlined.ArrowBack else Icons.Outlined.Menu,
      contentDescription = if (isDetail) "Kembali ke daftar absensi" else "Buka sidebar",
      onClick = onPrimaryClick
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .padding(end = 44.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = SubtleInk,
        textAlign = TextAlign.Center
      )
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center
      )
    }
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
          text = "Periode",
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
        text = "Kehadiran ${summary?.attendancePercent.withPercentSuffix()}",
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
        style = MaterialTheme.typography.titleMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.Bold
      )
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
      .background(Color.White.copy(alpha = 0.86f))
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

private fun buildAttendancePeriods(): List<String> {
  val start = YearMonth.now().minusMonths(1)
  return (0 until 12).map { offset -> start.minusMonths(offset.toLong()).toString() }
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
