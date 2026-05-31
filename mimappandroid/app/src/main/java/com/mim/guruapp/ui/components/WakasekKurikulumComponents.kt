package com.mim.guruapp.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.WakasekReviewOutcome
import com.mim.guruapp.data.model.LeaveRequestItem
import com.mim.guruapp.data.model.MapelScoreSnapshot
import com.mim.guruapp.data.model.ScoreStudent
import com.mim.guruapp.data.model.SubjectOverview
import com.mim.guruapp.data.model.WakasekKurikulumSnapshot
import com.mim.guruapp.data.model.WakasekStudentMonitoringRow
import com.mim.guruapp.data.model.WakasekTeacherMonitoringRow
import com.mim.guruapp.export.WakasekScoreExcelExporter
import com.mim.guruapp.ui.i18n.t
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.HighlightCard
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk
import com.mim.guruapp.ui.theme.SuccessTint
import com.mim.guruapp.ui.theme.WarmAccent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class WakasekKurikulumPage(val title: String, val subtitle: String) {
  Teacher("Monitoring Guru", "Pantau kehadiran guru berdasarkan jadwal dan absensi"),
  Student("Monitoring Siswa", "Lihat santri sakit, izin, terlambat, dan alpa"),
  StudentScores("Nilai Siswa", "Pantau nilai santri per kelas dan mapel"),
  Permission("Perizinan", "Tinjau pengajuan izin guru")
}

private enum class WakasekPeriod(val label: String) {
  Day("Harian"),
  Week("Pekan"),
  Month("Bulan"),
  Semester("Semester")
}

private enum class StudentSortOption(val label: String) {
  Class("Per kelas"),
  Name("Nama"),
  Status("Status")
}

private enum class WakasekScoreSortMode(val label: String) {
  ByStudent("Per siswa"),
  ByType("Jenis nilai"),
  ByDate("Tanggal")
}

private enum class WakasekScoreExportAction(val title: String, val confirmLabel: String) {
  Open("Cetak Nilai Siswa", "Cetak"),
  Share("Kirim Nilai Siswa", "Kirim")
}

private data class TeacherMonitorSummary(
  val teacherId: String,
  val teacherName: String,
  val total: Int,
  val present: Int,
  val leave: Int,
  val substitute: Int,
  val absent: Int,
  val details: List<WakasekTeacherMonitoringRow>
)

private data class StudentStatusSummary(
  val label: String,
  val value: Int,
  val color: Color
)

private data class StudentMonitorSummary(
  val studentKey: String,
  val studentName: String,
  val className: String,
  val details: List<WakasekStudentMonitoringRow>
)

private data class WakasekScoreMetricDefinition(
  val key: String,
  val label: String,
  val supportsDetailRows: Boolean = true
)

private data class WakasekScoreMetric(
  val key: String,
  val label: String,
  val value: Double?,
  val details: List<com.mim.guruapp.data.model.ScoreDetailRow>
)

private data class WakasekScoreTypeOverview(
  val key: String,
  val label: String,
  val entries: List<Pair<ScoreStudent, Double?>>
)

private data class WakasekScoreDateEntry(
  val studentName: String,
  val value: Double?,
  val material: String
)

private data class WakasekScoreDateOverview(
  val key: String,
  val dateIso: String,
  val label: String,
  val entries: List<WakasekScoreDateEntry>
)

private data class WakasekDropdownOption(
  val key: String,
  val label: String,
  val supporting: String = ""
)

private data class WakasekSemesterOption(
  val key: String,
  val label: String,
  val shortLabel: String,
  val start: LocalDate,
  val end: LocalDate
)

private data class WakasekPeriodFilter(
  val period: WakasekPeriod,
  val selectedDate: LocalDate,
  val weekStart: LocalDate,
  val selectedMonth: YearMonth,
  val selectedSemester: WakasekSemesterOption
) {
  val label: String
    get() = when (period) {
      WakasekPeriod.Day -> selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
      WakasekPeriod.Week -> "Pekan ${weekOfMonthLabel(weekStart)}"
      WakasekPeriod.Month -> selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID")))
      WakasekPeriod.Semester -> selectedSemester.label
    }
}

private val WakasekScoreMetricDefinitions = listOf(
  WakasekScoreMetricDefinition("nilai_tugas", "Tugas"),
  WakasekScoreMetricDefinition("nilai_ulangan_harian", "Ulangan Harian"),
  WakasekScoreMetricDefinition("nilai_pts", "UTS"),
  WakasekScoreMetricDefinition("nilai_pas", "UAS"),
  WakasekScoreMetricDefinition("nilai_kehadiran", "Kehadiran", supportsDetailRows = false),
  WakasekScoreMetricDefinition("nilai_akhir", "Pengetahuan", supportsDetailRows = false),
  WakasekScoreMetricDefinition("nilai_keterampilan", "Keterampilan")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakasekKurikulumScreen(
  page: WakasekKurikulumPage,
  snapshot: WakasekKurikulumSnapshot,
  scoreSnapshots: List<MapelScoreSnapshot> = emptyList(),
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadScores: suspend (String, SubjectOverview) -> MapelScoreSnapshot? = { _, _ -> null },
  onReviewLeaveRequest: suspend (String, Boolean, String) -> WakasekReviewOutcome,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var selectedPeriod by rememberSaveable { mutableStateOf(WakasekPeriod.Day) }
  var selectedDateIso by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
  var selectedWeekStartIso by rememberSaveable { mutableStateOf(startOfWeek(LocalDate.now()).toString()) }
  var selectedMonthIso by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
  var selectedSemesterKey by rememberSaveable { mutableStateOf(semesterOptionFor(LocalDate.now()).key) }
  var selectedStudentSort by rememberSaveable { mutableStateOf(StudentSortOption.Class) }
  var reviewTarget by remember { mutableStateOf<Pair<LeaveRequestItem, Boolean>?>(null) }
  var reviewChoiceTarget by remember { mutableStateOf<LeaveRequestItem?>(null) }
  var processingReviewId by rememberSaveable { mutableStateOf("") }
  var expandedTeacherId by rememberSaveable { mutableStateOf("") }
  var expandedStudentKey by rememberSaveable { mutableStateOf("") }
  var selectedScoreClassName by rememberSaveable { mutableStateOf("") }
  var selectedScoreSubjectId by rememberSaveable { mutableStateOf("") }
  var selectedScoreSortKey by rememberSaveable { mutableStateOf(WakasekScoreSortMode.ByStudent.name) }
  var scoreSnapshot by remember { mutableStateOf<MapelScoreSnapshot?>(null) }
  var isLoadingScores by rememberSaveable { mutableStateOf(false) }
  var isExportingScores by rememberSaveable { mutableStateOf(false) }
  var scoreExportAction by remember { mutableStateOf<WakasekScoreExportAction?>(null) }
  var expandedScoreStudentId by rememberSaveable { mutableStateOf("") }
  var expandedScoreMetricKey by rememberSaveable { mutableStateOf("") }
  var expandedScoreDateKey by rememberSaveable { mutableStateOf("") }
  val selectedDate = parseIsoDate(selectedDateIso) ?: LocalDate.now()
  val selectedWeekStart = parseIsoDate(selectedWeekStartIso) ?: startOfWeek(LocalDate.now())
  val selectedMonth = runCatching { YearMonth.parse(selectedMonthIso) }.getOrElse { YearMonth.now() }
  val semesterOptions = remember(snapshot.teacherRows, snapshot.studentRows) {
    buildSemesterOptions(snapshot.teacherRows.map { it.periodKey } + snapshot.studentRows.map { it.dateIso })
  }
  val selectedSemester = semesterOptions.firstOrNull { it.key == selectedSemesterKey } ?: semesterOptionFor(LocalDate.now())
  val activeFilter = remember(selectedPeriod, selectedDate, selectedWeekStart, selectedMonth, selectedSemester) {
    WakasekPeriodFilter(
      period = selectedPeriod,
      selectedDate = selectedDate,
      weekStart = selectedWeekStart,
      selectedMonth = selectedMonth,
      selectedSemester = selectedSemester
    )
  }
  val teacherRows = remember(snapshot.teacherRows, activeFilter) {
    summarizeTeacherRows(snapshot.teacherRows, activeFilter)
  }
  val studentRows = remember(snapshot.studentRows, activeFilter) {
    filterStudentRows(snapshot.studentRows, activeFilter)
  }
  val studentSummaries = remember(studentRows, selectedStudentSort) {
    sortStudentSummaries(summarizeStudentRows(studentRows), selectedStudentSort)
  }
  val scoreClassOptions = remember(snapshot.scoreSubjects) {
    snapshot.scoreSubjects
      .map { it.className }
      .filter(String::isNotBlank)
      .distinct()
      .sortedWith(String.CASE_INSENSITIVE_ORDER)
  }
  val scoreSubjectsForClass = remember(snapshot.scoreSubjects, selectedScoreClassName) {
    snapshot.scoreSubjects
      .filter { selectedScoreClassName.isBlank() || it.className == selectedScoreClassName }
      .sortedWith(compareBy<SubjectOverview> { it.title.lowercase(Locale.ROOT) }.thenBy { it.className.lowercase(Locale.ROOT) })
  }
  val selectedScoreSubject = remember(scoreSubjectsForClass, selectedScoreSubjectId) {
    scoreSubjectsForClass.firstOrNull { it.id == selectedScoreSubjectId }
  }
  val selectedScoreSort = remember(selectedScoreSortKey) {
    WakasekScoreSortMode.entries.firstOrNull { it.name == selectedScoreSortKey } ?: WakasekScoreSortMode.ByStudent
  }

  LaunchedEffect(page, scoreClassOptions) {
    if (page == WakasekKurikulumPage.StudentScores && selectedScoreClassName !in scoreClassOptions) {
      selectedScoreClassName = scoreClassOptions.firstOrNull().orEmpty()
    }
  }
  LaunchedEffect(page, selectedScoreClassName, scoreSubjectsForClass) {
    if (page == WakasekKurikulumPage.StudentScores && selectedScoreSubjectId !in scoreSubjectsForClass.map { it.id }) {
      selectedScoreSubjectId = scoreSubjectsForClass.firstOrNull()?.id.orEmpty()
    }
  }
  LaunchedEffect(page, selectedScoreSubjectId, scoreSnapshots) {
    if (page != WakasekKurikulumPage.StudentScores || selectedScoreSubjectId.isBlank()) return@LaunchedEffect
    val subject = snapshot.scoreSubjects.firstOrNull { it.id == selectedScoreSubjectId } ?: return@LaunchedEffect
    val cachedSnapshot = scoreSnapshots.firstOrNull { it.distribusiId == selectedScoreSubjectId }
    if (cachedSnapshot != null) {
      scoreSnapshot = cachedSnapshot
      return@LaunchedEffect
    }
    isLoadingScores = true
    scoreSnapshot = onLoadScores(subject.id, subject)
    isLoadingScores = false
  }

  fun exportScores(action: WakasekScoreExportAction, subjects: List<SubjectOverview>) {
    scope.launch {
      isExportingScores = true
      runCatching {
        val worksheets = subjects.mapNotNull { subject ->
          val loadedSnapshot = scoreSnapshots.firstOrNull { it.distribusiId == subject.id }
            ?: scoreSnapshot?.takeIf { it.distribusiId == subject.id }
            ?: onLoadScores(subject.id, subject)
          if (loadedSnapshot != null && subject.id == selectedScoreSubjectId) {
            scoreSnapshot = loadedSnapshot
          }
          loadedSnapshot?.let { snapshot ->
            WakasekScoreExcelExporter.ScoreWorksheet(
              subject = subject,
              snapshot = snapshot
            )
          }
        }
        if (worksheets.isEmpty()) {
          snackbarHostState.showSnackbar("Data nilai belum bisa dimuat.")
        } else {
          val file = WakasekScoreExcelExporter.createWorkbook(
            context = context,
            worksheets = worksheets
          )
          if (action == WakasekScoreExportAction.Share) {
            WakasekScoreExcelExporter.shareWorkbook(context, file)
          } else {
            WakasekScoreExcelExporter.openWorkbook(context, file)
          }
          snackbarHostState.showSnackbar("File Excel nilai siswa siap.")
        }
      }.onFailure {
        snackbarHostState.showSnackbar("Gagal membuat file Excel nilai siswa.")
      }.also {
        isExportingScores = false
      }
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
        ) {
          WakasekTopBar(
            page = page,
            onMenuClick = onMenuClick,
            showScoreActions = page == WakasekKurikulumPage.StudentScores,
            scoreActionsEnabled = snapshot.scoreSubjects.isNotEmpty() && !isLoadingScores,
            scoreActionsProcessing = isExportingScores,
            onScoreActionSelected = { scoreExportAction = it }
          )
          if (page == WakasekKurikulumPage.Teacher || page == WakasekKurikulumPage.Student) {
            WakasekPeriodContextSelector(
              filter = activeFilter,
              semesterOptions = semesterOptions,
              selectedPeriod = selectedPeriod,
              onPeriodClick = { selectedPeriod = it },
              onJumpToToday = {
                val today = LocalDate.now()
                selectedDateIso = today.toString()
                selectedWeekStartIso = startOfWeek(today).toString()
                selectedMonthIso = YearMonth.from(today).toString()
                selectedSemesterKey = semesterOptionFor(today).key
              },
              onDateChange = { selectedDateIso = it.toString() },
              onWeekChange = { selectedWeekStartIso = it.toString() },
              onMonthChange = { selectedMonthIso = it.toString() },
              onSemesterChange = { selectedSemesterKey = it.key }
            )
          }

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 124.dp)
          ) {
            if (!snapshot.isWakasekKurikulum) {
              item {
                EmptyPlaceholderCard("Menu ini hanya muncul untuk guru yang tercatat sebagai Wakasek Akademik/Kurikulum di role karyawan atau struktur sekolah.")
              }
            } else {
              when (page) {
                WakasekKurikulumPage.Teacher -> {
                  item {
                    TeacherAttendanceChartCard(
                      rows = teacherRows,
                      label = activeFilter.label
                    )
                  }
                  if (teacherRows.isEmpty()) {
                    item { EmptyPlaceholderCard("Belum ada data kehadiran guru pada rentang ini.") }
                  } else {
                    items(teacherRows, key = { it.teacherId.ifBlank { it.teacherName } }) { row ->
                      val key = row.teacherId.ifBlank { row.teacherName }
                      TeacherMonitoringCard(
                        row = row,
                        expanded = expandedTeacherId == key,
                        onClick = {
                          expandedTeacherId = if (expandedTeacherId == key) "" else key
                        }
                      )
                    }
                  }
                }

                WakasekKurikulumPage.Student -> {
                  item {
                    StudentAttendanceChartCard(
                      rows = studentRows,
                      label = activeFilter.label
                    )
                  }
                  item {
                    StudentSortControl(
                      selected = selectedStudentSort,
                      onSelected = { selectedStudentSort = it }
                    )
                  }
                  if (studentSummaries.isEmpty()) {
                    item { EmptyPlaceholderCard("Tidak ada siswa sakit, izin, terlambat, atau alpa pada rentang ini.") }
                  } else {
                    items(studentSummaries, key = { it.studentKey }) { row ->
                      StudentMonitoringCard(
                        row = row,
                        expanded = expandedStudentKey == row.studentKey,
                        onClick = {
                          expandedStudentKey = if (expandedStudentKey == row.studentKey) "" else row.studentKey
                        }
                      )
                    }
                  }
                }

                WakasekKurikulumPage.StudentScores -> {
                  item {
                    WakasekScoreSelectorCard(
                      classOptions = scoreClassOptions,
                      selectedClassName = selectedScoreClassName,
                      onClassSelected = { className ->
                        selectedScoreClassName = className
                        selectedScoreSubjectId = snapshot.scoreSubjects
                          .filter { it.className == className }
                          .sortedBy { it.title.lowercase(Locale.ROOT) }
                          .firstOrNull()
                          ?.id
                          .orEmpty()
                      },
                      subjectOptions = scoreSubjectsForClass,
                      selectedSubjectId = selectedScoreSubjectId,
                      onSubjectSelected = { selectedScoreSubjectId = it },
                      selectedSubject = selectedScoreSubject,
                      isLoading = isLoadingScores || isExportingScores
                    )
                  }
                  if (snapshot.scoreSubjects.isEmpty()) {
                    item { EmptyPlaceholderCard("Belum ada distribusi kelas-mapel untuk ditampilkan.") }
                  } else {
                    item {
                      WakasekScoreSortControl(
                        selected = selectedScoreSort,
                        onSelected = { selectedScoreSortKey = it.name }
                      )
                    }
                    val activeScoreSnapshot = scoreSnapshot?.takeIf { it.distribusiId == selectedScoreSubjectId }
                    when {
                      isLoadingScores && activeScoreSnapshot == null -> {
                        item { WakasekScoreLoadingCard() }
                      }

                      activeScoreSnapshot == null -> {
                        item { EmptyPlaceholderCard("Pilih kelas dan mapel untuk melihat nilai siswa.") }
                      }

                      activeScoreSnapshot.students.isEmpty() -> {
                        item { EmptyPlaceholderCard("Belum ada nilai siswa untuk kelas dan mapel ini.") }
                      }

                      selectedScoreSort == WakasekScoreSortMode.ByStudent -> {
                        items(activeScoreSnapshot.students.sortedBy { it.name.lowercase(Locale.ROOT) }, key = { it.id }) { student ->
                          WakasekScoreStudentCard(
                            student = student,
                            expanded = expandedScoreStudentId == student.id,
                            onClick = {
                              expandedScoreStudentId = if (expandedScoreStudentId == student.id) "" else student.id
                            }
                          )
                        }
                      }

                      selectedScoreSort == WakasekScoreSortMode.ByType -> {
                        val overviews = buildWakasekScoreTypeOverviews(activeScoreSnapshot.students)
                        items(overviews, key = { it.key }) { item ->
                          WakasekScoreTypeCard(
                            item = item,
                            expanded = expandedScoreMetricKey == item.key,
                            onClick = {
                              expandedScoreMetricKey = if (expandedScoreMetricKey == item.key) "" else item.key
                            }
                          )
                        }
                      }

                      selectedScoreSort == WakasekScoreSortMode.ByDate -> {
                        val overviews = buildWakasekScoreDateOverviews(activeScoreSnapshot.students)
                        if (overviews.isEmpty()) {
                          item { EmptyPlaceholderCard("Belum ada riwayat nilai bertanggal untuk mapel ini.") }
                        } else {
                          items(overviews, key = { it.key }) { item ->
                            WakasekScoreDateCard(
                              item = item,
                              expanded = expandedScoreDateKey == item.key,
                              onClick = {
                                expandedScoreDateKey = if (expandedScoreDateKey == item.key) "" else item.key
                              }
                            )
                          }
                        }
                      }
                    }
                  }
                }

                WakasekKurikulumPage.Permission -> {
                  val pending = snapshot.leaveRequests.filter { isPendingLeaveStatus(it.status) }
                  val reviewed = snapshot.leaveRequests.filterNot { isPendingLeaveStatus(it.status) }
                  val leaveRequests = snapshot.leaveRequests.sortedWith(
                    compareByDescending<LeaveRequestItem> { isPendingLeaveStatus(it.status) }
                      .thenByDescending { it.createdAt.ifBlank { it.updatedAt } }
                  )
                  item {
                    WakasekSectionSummary(
                      title = "Perizinan Guru",
                      subtitle = "${pending.size} menunggu keputusan, ${reviewed.size} sudah ditinjau"
                    )
                  }
                  if (snapshot.leaveRequests.isEmpty()) {
                    item { EmptyPlaceholderCard("Belum ada pengajuan izin guru.") }
                  } else {
                    items(leaveRequests, key = { it.id }) { item ->
                      WakasekLeaveRequestCard(
                        item = item,
                        isProcessing = processingReviewId == item.id,
                        onClick = { reviewChoiceTarget = item },
                        onApprove = { reviewTarget = item to true },
                        onReject = { reviewTarget = item to false }
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  scoreExportAction?.let { action ->
    WakasekScoreExportDialog(
      action = action,
      subjects = snapshot.scoreSubjects,
      initialClassName = selectedScoreClassName,
      initialSubjectId = selectedScoreSubjectId,
      isProcessing = isExportingScores,
      onDismiss = {
        if (!isExportingScores) scoreExportAction = null
      },
      onConfirm = { selectedSubjects ->
        scoreExportAction = null
        selectedSubjects.firstOrNull()?.let { subject ->
          selectedScoreClassName = subject.className
          selectedScoreSubjectId = subject.id
        }
        exportScores(action, selectedSubjects)
      }
    )
  }

  reviewTarget?.let { (item, approved) ->
    WakasekReviewDialog(
      item = item,
      approved = approved,
      onDismiss = { reviewTarget = null },
      onSubmit = { note ->
        reviewTarget = null
        processingReviewId = item.id
        scope.launch {
          val result = onReviewLeaveRequest(item.id, approved, note)
          snackbarHostState.showSnackbar(result.message)
          processingReviewId = ""
        }
      }
    )
  }
  reviewChoiceTarget?.let { item ->
    WakasekReviewChoiceDialog(
      item = item,
      onDismiss = { reviewChoiceTarget = null },
      onSubmit = { approved, note ->
        reviewChoiceTarget = null
        processingReviewId = item.id
        scope.launch {
          val result = onReviewLeaveRequest(item.id, approved, note)
          snackbarHostState.showSnackbar(result.message)
          processingReviewId = ""
        }
      }
    )
  }
}

@Composable
private fun WakasekTopBar(
  page: WakasekKurikulumPage,
  onMenuClick: () -> Unit,
  showScoreActions: Boolean = false,
  scoreActionsEnabled: Boolean = true,
  scoreActionsProcessing: Boolean = false,
  onScoreActionSelected: (WakasekScoreExportAction) -> Unit = {}
) {
  var scoreMenuExpanded by remember { mutableStateOf(false) }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp, bottom = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .background(CardBackground.copy(alpha = 0.88f), CircleShape)
        .border(1.dp, CardBorder, CircleShape)
        .clickable(onClick = onMenuClick),
      contentAlignment = Alignment.Center
    ) {
      Icon(Icons.Outlined.Menu, contentDescription = t("Buka sidebar"), tint = PrimaryBlueDark)
    }
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = page.title,
        style = MaterialTheme.typography.titleLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
    }
    if (showScoreActions) {
      Box {
        Box(
          modifier = Modifier
            .size(42.dp)
            .background(CardBackground.copy(alpha = 0.88f), CircleShape)
            .border(1.dp, CardBorder, CircleShape)
            .clickable(enabled = scoreActionsEnabled && !scoreActionsProcessing) {
              scoreMenuExpanded = true
            },
          contentAlignment = Alignment.Center
        ) {
          if (scoreActionsProcessing) {
            CircularProgressIndicator(modifier = Modifier.size(19.dp), strokeWidth = 2.dp)
          } else {
            Icon(
              Icons.Outlined.MoreVert,
              contentDescription = t("Aksi nilai siswa"),
              tint = if (scoreActionsEnabled) PrimaryBlueDark else SubtleInk.copy(alpha = 0.5f)
            )
          }
        }
        DropdownMenu(expanded = scoreMenuExpanded, onDismissRequest = { scoreMenuExpanded = false }) {
          DropdownMenuItem(
            text = { Text(t("Cetak")) },
            leadingIcon = { Icon(Icons.Outlined.Print, contentDescription = null) },
            enabled = scoreActionsEnabled && !scoreActionsProcessing,
            onClick = {
              scoreMenuExpanded = false
              onScoreActionSelected(WakasekScoreExportAction.Open)
            }
          )
          DropdownMenuItem(
            text = { Text(t("Kirim")) },
            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
            enabled = scoreActionsEnabled && !scoreActionsProcessing,
            onClick = {
              scoreMenuExpanded = false
              onScoreActionSelected(WakasekScoreExportAction.Share)
            }
          )
        }
      }
    } else {
      Spacer(modifier = Modifier.size(42.dp))
    }
  }
}

private fun WakasekPeriod.description(): String {
  return when (this) {
    WakasekPeriod.Day -> "Tampilkan data hari ini"
    WakasekPeriod.Week -> "Tampilkan data pekan berjalan"
    WakasekPeriod.Month -> "Tampilkan data bulan berjalan"
    WakasekPeriod.Semester -> "Tampilkan data semester berjalan"
  }
}

@Composable
private fun WakasekPeriodContextSelector(
  filter: WakasekPeriodFilter,
  semesterOptions: List<WakasekSemesterOption>,
  selectedPeriod: WakasekPeriod,
  onPeriodClick: (WakasekPeriod) -> Unit,
  onJumpToToday: () -> Unit,
  onDateChange: (LocalDate) -> Unit,
  onWeekChange: (LocalDate) -> Unit,
  onMonthChange: (YearMonth) -> Unit,
  onSemesterChange: (WakasekSemesterOption) -> Unit
) {
  when (filter.period) {
    WakasekPeriod.Day -> WakasekDaySelector(
      selectedDate = filter.selectedDate,
      selectedPeriod = selectedPeriod,
      onPeriodClick = onPeriodClick,
      onJumpToToday = onJumpToToday,
      onDateChange = onDateChange
    )
    WakasekPeriod.Week -> WakasekWeekSelector(
      selectedWeekStart = filter.weekStart,
      selectedPeriod = selectedPeriod,
      onPeriodClick = onPeriodClick,
      onJumpToToday = onJumpToToday,
      onWeekChange = onWeekChange
    )
    WakasekPeriod.Month -> WakasekMonthSelector(
      selectedMonth = filter.selectedMonth,
      selectedPeriod = selectedPeriod,
      onPeriodClick = onPeriodClick,
      onJumpToToday = onJumpToToday,
      onMonthChange = onMonthChange
    )
    WakasekPeriod.Semester -> WakasekSemesterSelector(
      selectedSemester = filter.selectedSemester,
      options = semesterOptions,
      selectedPeriod = selectedPeriod,
      onPeriodClick = onPeriodClick,
      onJumpToToday = onJumpToToday,
      onSemesterChange = onSemesterChange
    )
  }
}

@Composable
private fun WakasekDaySelector(
  selectedDate: LocalDate,
  selectedPeriod: WakasekPeriod,
  onPeriodClick: (WakasekPeriod) -> Unit,
  onJumpToToday: () -> Unit,
  onDateChange: (LocalDate) -> Unit
) {
  val weekStart = startOfWeek(selectedDate)
  val dates = remember(weekStart) { List(7) { weekStart.plusDays(it.toLong()) } }
  val selectedIndex = dates.indexOf(selectedDate).coerceAtLeast(0)
  WakasekSelectorShell(
    eyebrow = "Tanggal",
    title = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.forLanguageTag("id-ID"))),
    selectedPeriod = selectedPeriod,
    onPeriodClick = onPeriodClick,
    onJumpToToday = onJumpToToday,
    onPrevious = { onDateChange(selectedDate.minusWeeks(1)) },
    onNext = { onDateChange(selectedDate.plusWeeks(1)) }
  ) {
    WakasekCenteredChoiceRow(items = dates, selectedIndex = selectedIndex, key = { it.toString() }) { date ->
      val selected = date == selectedDate
      WakasekChoiceChip(
        overline = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("id-ID")),
        main = date.dayOfMonth.toString(),
        selected = selected,
        onClick = { onDateChange(date) }
      )
    }
  }
}

@Composable
private fun WakasekWeekSelector(
  selectedWeekStart: LocalDate,
  selectedPeriod: WakasekPeriod,
  onPeriodClick: (WakasekPeriod) -> Unit,
  onJumpToToday: () -> Unit,
  onWeekChange: (LocalDate) -> Unit
) {
  val selectedMonth = YearMonth.from(selectedWeekStart.plusDays(3))
  val weeks = remember(selectedMonth) { weekStartsInMonth(selectedMonth) }
  val selectedIndex = weeks.indexOf(selectedWeekStart).coerceAtLeast(0)
  val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("id-ID"))
  WakasekSelectorShell(
    eyebrow = "Pekan",
    title = "${formatter.format(selectedWeekStart)} - ${formatter.format(selectedWeekStart.plusDays(6))}",
    selectedPeriod = selectedPeriod,
    onPeriodClick = onPeriodClick,
    onJumpToToday = onJumpToToday,
    onPrevious = { onWeekChange(selectedWeekStart.minusWeeks(1)) },
    onNext = { onWeekChange(selectedWeekStart.plusWeeks(1)) }
  ) {
    WakasekCenteredChoiceRow(items = weeks, selectedIndex = selectedIndex, key = { it.toString() }) { weekStart ->
      val selected = weekStart == selectedWeekStart
      val anchor = weekStart.plusDays(3)
      WakasekChoiceChip(
        overline = "Pekan",
        main = weekOfMonthNumber(anchor).toString(),
        supporting = anchor.format(DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("id-ID"))),
        selected = selected,
        onClick = { onWeekChange(weekStart) }
      )
    }
  }
}

@Composable
private fun WakasekMonthSelector(
  selectedMonth: YearMonth,
  selectedPeriod: WakasekPeriod,
  onPeriodClick: (WakasekPeriod) -> Unit,
  onJumpToToday: () -> Unit,
  onMonthChange: (YearMonth) -> Unit
) {
  val semester = semesterOptionFor(selectedMonth.atDay(1))
  val months = remember(semester.key) { monthsInSemester(semester) }
  val selectedIndex = months.indexOf(selectedMonth).coerceAtLeast(0)
  WakasekSelectorShell(
    eyebrow = "Bulan",
    title = semester.label,
    selectedPeriod = selectedPeriod,
    onPeriodClick = onPeriodClick,
    onJumpToToday = onJumpToToday,
    onPrevious = {
      val previousSemester = semesterOptionFor(semester.start.minusDays(1))
      onMonthChange(YearMonth.from(previousSemester.end))
    },
    onNext = {
      val nextSemester = semesterOptionFor(semester.end.plusDays(1))
      onMonthChange(YearMonth.from(nextSemester.start))
    }
  ) {
    WakasekCenteredChoiceRow(items = months, selectedIndex = selectedIndex, key = { it.toString() }) { month ->
      WakasekChoiceChip(
        overline = month.year.toString(),
        main = month.format(DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("id-ID"))),
        selected = month == selectedMonth,
        onClick = { onMonthChange(month) }
      )
    }
  }
}

@Composable
private fun WakasekSemesterSelector(
  selectedSemester: WakasekSemesterOption,
  options: List<WakasekSemesterOption>,
  selectedPeriod: WakasekPeriod,
  onPeriodClick: (WakasekPeriod) -> Unit,
  onJumpToToday: () -> Unit,
  onSemesterChange: (WakasekSemesterOption) -> Unit
) {
  val selectedIndex = options.indexOfFirst { it.key == selectedSemester.key }.coerceAtLeast(0)
  WakasekSelectorShell(
    eyebrow = "Semester",
    title = selectedSemester.label,
    selectedPeriod = selectedPeriod,
    onPeriodClick = onPeriodClick,
    onJumpToToday = onJumpToToday,
    onPrevious = {
      val index = options.indexOfFirst { it.key == selectedSemester.key }
      if (index > 0) onSemesterChange(options[index - 1])
    },
    onNext = {
      val index = options.indexOfFirst { it.key == selectedSemester.key }
      if (index >= 0 && index < options.lastIndex) onSemesterChange(options[index + 1])
    }
  ) {
    WakasekCenteredChoiceRow(items = options, selectedIndex = selectedIndex, key = { it.key }) { semester ->
      WakasekChoiceChip(
        overline = semester.label.substringAfter(" "),
        main = semester.shortLabel,
        selected = semester.key == selectedSemester.key,
        onClick = { onSemesterChange(semester) }
      )
    }
  }
}

@Composable
private fun WakasekSelectorShell(
  eyebrow: String,
  title: String,
  selectedPeriod: WakasekPeriod,
  onPeriodClick: (WakasekPeriod) -> Unit,
  onJumpToToday: () -> Unit,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  content: @Composable () -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp)
      .background(CardBackground.copy(alpha = 0.86f), RoundedCornerShape(20.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      WakasekSmallCircleButton(Icons.Outlined.ChevronLeft, "Sebelumnya", onPrevious)
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = SubtleInk, fontWeight = FontWeight.SemiBold)
        Text(
          title,
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Box(contentAlignment = Alignment.TopCenter) {
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .background(HighlightCard.copy(alpha = 0.18f))
              .border(1.dp, PrimaryBlue.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
              .clickable { expanded = true }
              .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = t(selectedPeriod.label),
              style = MaterialTheme.typography.labelSmall,
              color = PrimaryBlueDark,
              fontWeight = FontWeight.ExtraBold
            )
            Icon(
              imageVector = Icons.Outlined.ExpandMore,
              contentDescription = t("Pilih jenis sort"),
              tint = PrimaryBlueDark,
              modifier = Modifier.size(14.dp)
            )
          }
          DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WakasekPeriod.entries.forEach { period ->
              DropdownMenuItem(
                text = {
                  Column {
                    Text(
                      text = t(period.label),
                      style = MaterialTheme.typography.labelLarge,
                      color = PrimaryBlueDark,
                      fontWeight = if (period == selectedPeriod) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                    Text(
                      text = t(period.description()),
                      style = MaterialTheme.typography.labelSmall,
                      color = SubtleInk
                    )
                  }
                },
                onClick = {
                  expanded = false
                  onPeriodClick(period)
                }
              )
            }
          }
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(CardBackground.copy(alpha = 0.88f))
            .border(1.dp, CardBorder.copy(alpha = 0.86f), RoundedCornerShape(999.dp))
            .clickable(onClick = onJumpToToday)
            .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
          Text(
            text = t("Hari ini"),
            style = MaterialTheme.typography.labelSmall,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }
      WakasekSmallCircleButton(Icons.Outlined.ChevronRight, "Berikutnya", onNext)
    }
    content()
  }
}

@Composable
private fun <T> WakasekCenteredChoiceRow(
  items: List<T>,
  selectedIndex: Int,
  key: (T) -> String,
  itemContent: @Composable (T) -> Unit
) {
  val listState = rememberLazyListState()
  LaunchedEffect(items, selectedIndex) {
    if (items.isNotEmpty()) listState.scrollToItem(selectedIndex.coerceIn(items.indices))
  }
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val sidePadding = ((maxWidth - 74.dp) / 2).coerceAtLeast(0.dp)
    LazyRow(
      state = listState,
      contentPadding = PaddingValues(horizontal = sidePadding),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(items, key = key) { item ->
        itemContent(item)
      }
    }
  }
}

@Composable
private fun WakasekSmallCircleButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(38.dp)
      .clip(CircleShape)
      .background(CardBackground.copy(alpha = 0.9f))
      .border(1.dp, CardBorder, CircleShape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(icon, contentDescription = t(contentDescription), tint = PrimaryBlueDark, modifier = Modifier.size(20.dp))
  }
}

@Composable
private fun WakasekChoiceChip(
  overline: String,
  main: String,
  supporting: String = "",
  selected: Boolean,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .width(74.dp)
      .clip(RoundedCornerShape(22.dp))
      .background(if (selected) Color(0xFF60A5FA) else CardBackground.copy(alpha = 0.88f))
      .border(
        1.dp,
        when {
          selected -> Color(0xFF93C5FD)
          else -> CardBorder.copy(alpha = 0.86f)
        },
        RoundedCornerShape(22.dp)
      )
      .clickable(onClick = onClick)
      .padding(vertical = 14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      overline,
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) Color.White else SubtleInk,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      main,
      style = MaterialTheme.typography.titleMedium,
      color = if (selected) Color.White else PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(top = 4.dp)
    )
    if (supporting.isNotBlank()) {
      Text(
        supporting,
        style = MaterialTheme.typography.labelSmall,
        color = if (selected) Color.White.copy(alpha = 0.88f) else SubtleInk,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 2.dp)
      )
    }
  }
}

@Composable
private fun TeacherAttendanceChartCard(
  rows: List<TeacherMonitorSummary>,
  label: String
) {
  val total = rows.sumOf { it.total }
  val present = rows.sumOf { it.present }
  val absent = rows.sumOf { it.absent + it.leave }
  val presentPercent = if (total > 0) ((present * 100f) / total).toInt() else 0
  val absentPercent = if (total > 0) ((absent * 100f) / total).toInt() else 0
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(CardBackground.copy(alpha = 0.9f))
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = t("Kehadiran Guru"),
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = "${rows.size} ${t("guru pada")} ${label.lowercase()}",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          modifier = Modifier.padding(top = 3.dp)
        )
      }
      Text(
        text = "$total ${t("sesi")}",
        style = MaterialTheme.typography.labelMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .background(HighlightCard.copy(alpha = 0.16f))
          .padding(horizontal = 10.dp, vertical = 7.dp)
      )
    }
    AttendanceBarRow(
      label = "Hadir di kelas",
      value = present,
      percent = presentPercent,
      color = Color(0xFF22C55E)
    )
    AttendanceBarRow(
      label = "Ketidakhadiran",
      value = absent,
      percent = absentPercent,
      color = Color(0xFFFB7185)
    )
  }
}

@Composable
private fun AttendanceBarRow(
  label: String,
  value: Int,
  percent: Int,
  color: Color
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(7.dp)
          .background(color, CircleShape)
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
          .weight(1f)
          .padding(start = 7.dp)
      )
      Text(
        text = "$percent% ($value)",
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.ExtraBold
      )
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(10.dp)
        .clip(RoundedCornerShape(999.dp))
        .background(Color(0xFFE2E8F0).copy(alpha = 0.68f))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
          .height(10.dp)
          .clip(RoundedCornerShape(999.dp))
          .background(color)
      )
    }
  }
}

@Composable
private fun StudentAttendanceChartCard(
  rows: List<WakasekStudentMonitoringRow>,
  label: String
) {
  val summaries = remember(rows) { buildStudentStatusSummaries(rows) }
  val total = summaries.sumOf { it.value }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(CardBackground.copy(alpha = 0.9f))
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = t("Kehadiran Siswa"),
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = "${rows.size} ${t("catatan pada")} ${label.lowercase()}",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          modifier = Modifier.padding(top = 3.dp)
        )
      }
      Text(
        text = "$total ${t("data")}",
        style = MaterialTheme.typography.labelMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .background(WarmAccent.copy(alpha = 0.15f))
          .padding(horizontal = 10.dp, vertical = 7.dp)
      )
    }
    if (summaries.isEmpty()) {
      Text(
        text = t("Belum ada catatan siswa pada rentang ini."),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    } else {
      summaries.forEach { item ->
        val percent = if (total > 0) ((item.value * 100f) / total).toInt() else 0
        AttendanceBarRow(
          label = item.label,
          value = item.value,
          percent = percent,
          color = item.color
        )
      }
    }
  }
}

@Composable
private fun StudentSortControl(
  selected: StudentSortOption,
  onSelected: (StudentSortOption) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(CardBackground.copy(alpha = 0.78f))
      .border(1.dp, CardBorder.copy(alpha = 0.76f), RoundedCornerShape(18.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = t("Urutkan daftar siswa"),
        style = MaterialTheme.typography.labelMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = t("Bisa berdasarkan kelas, nama, atau status kehadiran."),
        style = MaterialTheme.typography.labelSmall,
        color = SubtleInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Box {
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .background(HighlightCard.copy(alpha = 0.16f))
          .clickable { expanded = true }
          .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
      ) {
        Text(
          text = selected.label,
          style = MaterialTheme.typography.labelMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Icon(
          imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
          contentDescription = t("Pilih sort siswa"),
          tint = PrimaryBlueDark,
          modifier = Modifier.size(16.dp)
        )
      }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        StudentSortOption.entries.forEach { option ->
          DropdownMenuItem(
            text = { Text(t(option.label)) },
            onClick = {
              onSelected(option)
              expanded = false
            }
          )
        }
      }
    }
  }
}

@Composable
private fun WakasekSectionSummary(title: String, subtitle: String) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(CardBackground.copy(alpha = 0.88f))
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .padding(16.dp)
  ) {
    Text(t(title), style = MaterialTheme.typography.titleMedium, color = PrimaryBlueDark, fontWeight = FontWeight.ExtraBold)
    Text(t(subtitle), style = MaterialTheme.typography.bodySmall, color = SubtleInk, modifier = Modifier.padding(top = 3.dp))
  }
}

@Composable
private fun WakasekScoreSelectorCard(
  classOptions: List<String>,
  selectedClassName: String,
  onClassSelected: (String) -> Unit,
  subjectOptions: List<SubjectOverview>,
  selectedSubjectId: String,
  onSubjectSelected: (String) -> Unit,
  selectedSubject: SubjectOverview?,
  isLoading: Boolean
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(CardBackground.copy(alpha = 0.92f))
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(PrimaryBlue.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Outlined.Grade, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(start = 10.dp)
      ) {
        Text(
          text = t("Nilai Siswa"),
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = t("Pilih kelas dan mapel untuk melihat rekap nilai."),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
      if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
      }
    }

    WakasekDropdownField(
      label = "Kelas",
      value = selectedClassName.ifBlank { "Pilih kelas" },
      options = classOptions.map { WakasekDropdownOption(it, it) },
      enabled = classOptions.isNotEmpty(),
      onSelected = onClassSelected
    )
    WakasekDropdownField(
      label = "Mapel",
      value = subjectOptions.firstOrNull { it.id == selectedSubjectId }?.title ?: "Pilih mapel",
      options = subjectOptions.map { subject ->
        WakasekDropdownOption(
          key = subject.id,
          label = subject.title,
          supporting = subject.semester
        )
      },
      enabled = subjectOptions.isNotEmpty(),
      onSelected = onSubjectSelected
    )

    selectedSubject?.let { subject ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(HighlightCard.copy(alpha = 0.12f))
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = subject.title,
            style = MaterialTheme.typography.labelLarge,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${subject.className} - Semester ${subject.semester}",
            style = MaterialTheme.typography.labelSmall,
            color = SubtleInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }

  }
}

@Composable
private fun WakasekDropdownField(
  label: String,
  value: String,
  options: List<WakasekDropdownOption>,
  enabled: Boolean,
  onSelected: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(CardBackground.copy(alpha = 0.86f))
        .border(1.dp, CardBorder.copy(alpha = 0.78f), RoundedCornerShape(16.dp))
        .clickable(enabled = enabled) { expanded = true }
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = t(label),
          style = MaterialTheme.typography.labelSmall,
          color = SubtleInk,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = t(value),
          style = MaterialTheme.typography.bodyMedium,
          color = if (enabled) PrimaryBlueDark else SubtleInk,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Icon(
        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
        contentDescription = null,
        tint = if (enabled) PrimaryBlueDark else SubtleInk.copy(alpha = 0.5f)
      )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { option ->
        DropdownMenuItem(
          text = {
            Column {
              Text(option.label, fontWeight = FontWeight.SemiBold)
              if (option.supporting.isNotBlank()) {
                Text(option.supporting, style = MaterialTheme.typography.labelSmall, color = SubtleInk)
              }
            }
          },
          onClick = {
            onSelected(option.key)
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
private fun WakasekScoreSortControl(
  selected: WakasekScoreSortMode,
  onSelected: (WakasekScoreSortMode) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(CardBackground.copy(alpha = 0.82f))
      .border(1.dp, CardBorder.copy(alpha = 0.76f), RoundedCornerShape(18.dp))
      .padding(8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    WakasekScoreSortMode.entries.forEach { option ->
      val isSelected = selected == option
      Text(
        text = t(option.label),
        style = MaterialTheme.typography.labelMedium,
        color = if (isSelected) Color.White else PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(999.dp))
          .background(if (isSelected) PrimaryBlue else Color.Transparent)
          .clickable { onSelected(option) }
          .padding(horizontal = 8.dp, vertical = 9.dp)
      )
    }
  }
}

@Composable
private fun WakasekScoreLoadingCard() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(CardBackground.copy(alpha = 0.88f))
      .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
    Text(t("Memuat nilai siswa..."), style = MaterialTheme.typography.bodyMedium, color = PrimaryBlueDark, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun WakasekScoreStudentCard(
  student: ScoreStudent,
  expanded: Boolean,
  onClick: () -> Unit
) {
  val metrics = remember(student) { buildWakasekScoreMetrics(student) }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(20.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.94f), RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .animateContentSize()
      .padding(horizontal = 14.dp, vertical = 12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = student.name,
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Icon(
        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
        contentDescription = null,
        tint = SubtleInk
      )
    }
    if (expanded) {
      Column(
        modifier = Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
      ) {
        metrics.forEach { metric ->
          WakasekScoreMetricDetailRow(metric)
        }
      }
    }
  }
}

@Composable
private fun WakasekScoreMetricPill(label: String, value: String) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(HighlightCard.copy(alpha = 0.14f))
      .border(1.dp, CardBorder.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
      .padding(horizontal = 10.dp, vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(t(label), style = MaterialTheme.typography.labelSmall, color = SubtleInk, fontWeight = FontWeight.SemiBold)
    Text(value, style = MaterialTheme.typography.labelMedium, color = PrimaryBlueDark, fontWeight = FontWeight.ExtraBold)
  }
}

@Composable
private fun WakasekScoreMetricDetailRow(metric: WakasekScoreMetric) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(SoftPanel.copy(alpha = 0.72f))
      .padding(10.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = t(metric.label),
        style = MaterialTheme.typography.labelMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.weight(1f)
      )
      Text(
        text = formatWakasekScoreNumber(metric.value),
        style = MaterialTheme.typography.labelMedium,
        color = PrimaryBlue,
        fontWeight = FontWeight.ExtraBold
      )
    }
    if (metric.details.isNotEmpty()) {
      Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        metric.details.sortedByDescending { it.dateIso }.forEach { detail ->
          Text(
            text = "${formatWakasekScoreDate(detail.dateIso)} - ${formatWakasekScoreNumber(detail.value)}${detail.material.takeIf { it.isNotBlank() }?.let { " - $it" }.orEmpty()}",
            style = MaterialTheme.typography.labelSmall,
            color = SubtleInk
          )
        }
      }
    }
  }
}

@Composable
private fun WakasekScoreTypeCard(
  item: WakasekScoreTypeOverview,
  expanded: Boolean,
  onClick: () -> Unit
) {
  val values = item.entries.mapNotNull { it.second }
  val average = values.takeIf { it.isNotEmpty() }?.average()
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .animateContentSize()
      .padding(14.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(item.label, style = MaterialTheme.typography.titleSmall, color = PrimaryBlueDark, fontWeight = FontWeight.ExtraBold)
        Text("${item.entries.size} siswa - Rata-rata ${formatWakasekScoreNumber(average)}", style = MaterialTheme.typography.labelSmall, color = SubtleInk)
      }
      Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = SubtleInk)
    }
    if (expanded) {
      Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item.entries.sortedBy { it.first.name.lowercase(Locale.ROOT) }.forEach { (student, value) ->
          WakasekScoreNameValueRow(student.name, formatWakasekScoreNumber(value))
        }
      }
    }
  }
}

@Composable
private fun WakasekScoreDateCard(
  item: WakasekScoreDateOverview,
  expanded: Boolean,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .animateContentSize()
      .padding(14.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(item.label, style = MaterialTheme.typography.titleSmall, color = PrimaryBlueDark, fontWeight = FontWeight.ExtraBold)
        Text("${item.entries.size} nilai dicatat", style = MaterialTheme.typography.labelSmall, color = SubtleInk)
      }
      Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = SubtleInk)
    }
    if (expanded) {
      Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item.entries.sortedBy { it.studentName.lowercase(Locale.ROOT) }.forEach { entry ->
          val value = buildString {
            append(formatWakasekScoreNumber(entry.value))
            if (entry.material.isNotBlank()) append(" - ${entry.material}")
          }
          WakasekScoreNameValueRow(entry.studentName, value)
        }
      }
    }
  }
}

@Composable
private fun WakasekScoreNameValueRow(name: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(SoftPanel.copy(alpha = 0.68f))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = name,
      style = MaterialTheme.typography.labelMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelMedium,
      color = PrimaryBlue,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(start = 8.dp)
    )
  }
}

@Composable
private fun WakasekScoreExportDialog(
  action: WakasekScoreExportAction,
  subjects: List<SubjectOverview>,
  initialClassName: String,
  initialSubjectId: String,
  isProcessing: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (List<SubjectOverview>) -> Unit
) {
  val classOptions = remember(subjects) {
    subjects.map { it.className }.filter(String::isNotBlank).distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
  }
  var selectedClassName by remember(action, subjects) {
    mutableStateOf(initialClassName.takeIf { it in classOptions }.orEmpty().ifBlank { classOptions.firstOrNull().orEmpty() })
  }
  val subjectOptions = remember(subjects, selectedClassName) {
    subjects
      .filter { selectedClassName.isBlank() || it.className == selectedClassName }
      .sortedWith(compareBy<SubjectOverview> { it.title.lowercase(Locale.ROOT) }.thenBy { it.semester })
  }
  var includeAllSubjects by remember(action, selectedClassName, subjects) { mutableStateOf(true) }
  var selectedSubjectIds by remember(action, selectedClassName, subjects) {
    mutableStateOf(
      initialSubjectId
        .takeIf { id -> subjectOptions.any { it.id == id } }
        ?.let { setOf(it) }
        ?: subjectOptions.firstOrNull()?.id?.let { setOf(it) }
        ?: emptySet()
    )
  }
  LaunchedEffect(selectedClassName, subjectOptions) {
    val allowedIds = subjectOptions.map { it.id }.toSet()
    if (selectedSubjectIds.none { it in allowedIds }) {
      selectedSubjectIds = subjectOptions.firstOrNull()?.id?.let { setOf(it) } ?: emptySet()
    } else {
      selectedSubjectIds = selectedSubjectIds.intersect(allowedIds)
    }
  }
  val selectedSubjects = if (includeAllSubjects) {
    subjectOptions
  } else {
    subjectOptions.filter { it.id in selectedSubjectIds }
  }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(t(action.title)) },
    text = {
      Column(
        modifier = Modifier
          .height(420.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = t("Pilih kelas, lalu cetak semua mapel atau centang mapel tertentu."),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
        WakasekDropdownField(
          label = "Kelas",
          value = selectedClassName.ifBlank { "Pilih kelas" },
          options = classOptions.map { WakasekDropdownOption(it, it) },
          enabled = classOptions.isNotEmpty() && !isProcessing,
          onSelected = { selectedClassName = it }
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HighlightCard.copy(alpha = 0.12f))
            .clickable(enabled = !isProcessing && subjectOptions.isNotEmpty()) {
              includeAllSubjects = !includeAllSubjects
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Checkbox(
            checked = includeAllSubjects,
            enabled = !isProcessing && subjectOptions.isNotEmpty(),
            onCheckedChange = { includeAllSubjects = it }
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = t("Semua mapel"),
              style = MaterialTheme.typography.labelLarge,
              color = PrimaryBlueDark,
              fontWeight = FontWeight.ExtraBold
            )
            Text(
              text = "${subjectOptions.size} ${t("mapel dalam kelas ini")}",
              style = MaterialTheme.typography.labelSmall,
              color = SubtleInk
            )
          }
        }
        if (!includeAllSubjects) {
          subjectOptions.forEach { subject ->
            val checked = subject.id in selectedSubjectIds
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardBackground.copy(alpha = 0.78f))
                .border(1.dp, CardBorder.copy(alpha = 0.62f), RoundedCornerShape(14.dp))
                .clickable(enabled = !isProcessing) {
                  selectedSubjectIds = if (checked) {
                    selectedSubjectIds - subject.id
                  } else {
                    selectedSubjectIds + subject.id
                  }
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = checked,
                enabled = !isProcessing,
                onCheckedChange = { nextChecked ->
                  selectedSubjectIds = if (nextChecked) {
                    selectedSubjectIds + subject.id
                  } else {
                    selectedSubjectIds - subject.id
                  }
                }
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = subject.title,
                  style = MaterialTheme.typography.labelMedium,
                  color = PrimaryBlueDark,
                  fontWeight = FontWeight.ExtraBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = subject.semester,
                  style = MaterialTheme.typography.labelSmall,
                  color = SubtleInk,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        enabled = selectedSubjects.isNotEmpty() && !isProcessing,
        onClick = { onConfirm(selectedSubjects) }
      ) {
        if (isProcessing) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
          Spacer(Modifier.width(7.dp))
        }
        Text(t(action.confirmLabel))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isProcessing) {
        Text(t("Batal"))
      }
    }
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeacherMonitoringCard(
  row: TeacherMonitorSummary,
  expanded: Boolean,
  onClick: () -> Unit
) {
  val success = SuccessTint
  val primaryDark = PrimaryBlueDark
  val stats = remember(row, success, primaryDark) {
    buildTeacherSummaryChips(
      row = row,
      success = success,
      primaryDark = primaryDark
    )
  }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(20.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.94f), RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .animateContentSize()
      .padding(horizontal = 14.dp, vertical = 12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = row.teacherName,
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Icon(
        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
        contentDescription = null,
        tint = SubtleInk
      )
    }
    if (stats.isNotEmpty()) {
      FlowRow(
        modifier = Modifier.padding(top = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
      ) {
        stats.forEach { stat ->
          MonitoringStatItem(stat)
        }
      }
    }
    if (expanded) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
      ) {
        row.details.forEach { detail ->
          TeacherSessionRow(detail)
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeacherSessionRow(row: WakasekTeacherMonitoringRow) {
  val palette = statusPalette(row.status)
  val subject = displayValueOrNull(row.subjectName)
  val className = displayValueOrNull(row.className)
  val time = displayValueOrNull(row.timeLabel)
  val note = displayValueOrNull(row.note)
  val title = listOfNotNull(subject, className).joinToString(" - ").ifBlank { "Jadwal guru" }
  val meta = listOfNotNull(row.status.ifBlank { null }, row.periodLabel.takeIf { it.isNotBlank() }, time, note)
    .joinToString(" | ")
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(SoftPanel)
      .border(1.dp, CardBorder.copy(alpha = 0.62f), RoundedCornerShape(14.dp))
      .padding(horizontal = 11.dp, vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .background(palette.first, CircleShape)
    )
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(start = 10.dp)
    ) {
      Text(
        text = t(title),
        style = MaterialTheme.typography.labelMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (meta.isNotBlank()) {
        Text(
          text = meta,
          style = MaterialTheme.typography.labelSmall,
          color = SubtleInk,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun MonitoringStatItem(stat: TeacherSummaryChip) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Box(
      modifier = Modifier
        .size(5.dp)
        .background(stat.accent, CircleShape)
    )
    Text(
      text = stat.label,
      style = MaterialTheme.typography.labelSmall,
      color = SubtleInk,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = stat.value.toString(),
      style = MaterialTheme.typography.labelSmall,
      color = stat.text,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 1
    )
  }
}

private fun displayValueOrNull(value: String): String? {
  val clean = value.trim()
  return clean.takeIf { it.isNotBlank() && it != "-" && !it.equals("null", ignoreCase = true) }
}

@Composable
private fun MonitoringChip(label: String, value: Int, background: Color, content: Color) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(background)
      .padding(horizontal = 9.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text("$label ", style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.78f), fontWeight = FontWeight.SemiBold)
    Text(value.toString(), style = MaterialTheme.typography.labelMedium, color = content, fontWeight = FontWeight.ExtraBold)
  }
}

private data class TeacherSummaryChip(
  val label: String,
  val value: Int,
  val accent: Color,
  val text: Color
)

private fun buildTeacherSummaryChips(
  row: TeacherMonitorSummary,
  success: Color,
  primaryDark: Color
): List<TeacherSummaryChip> {
  return buildList {
    if (row.total > 0) add(TeacherSummaryChip("Total", row.total, primaryDark.copy(alpha = 0.35f), primaryDark))
    if (row.present > 0) add(TeacherSummaryChip("Masuk", row.present, success, Color(0xFF166534)))
    if (row.leave > 0) add(TeacherSummaryChip("Izin", row.leave, Color(0xFFF59E0B), Color(0xFF92400E)))
    if (row.substitute > 0) add(TeacherSummaryChip("Diganti", row.substitute, Color(0xFF38BDF8), Color(0xFF0369A1)))
    if (row.absent > 0) add(TeacherSummaryChip("Tidak Masuk", row.absent, Color(0xFFFB7185), Color(0xFFBE123C)))
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudentMonitoringCard(
  row: StudentMonitorSummary,
  expanded: Boolean,
  onClick: () -> Unit
) {
  val stats = remember(row) { buildStudentSummaryChips(row.details) }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .animateContentSize()
      .padding(horizontal = 14.dp, vertical = 12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          row.studentName,
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = row.className.ifBlank { "Kelas belum terbaca" },
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Icon(
        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
        contentDescription = null,
        tint = SubtleInk
      )
    }
    if (stats.isNotEmpty()) {
      FlowRow(
        modifier = Modifier.padding(top = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
      ) {
        stats.forEach { stat ->
          MonitoringStatItem(stat)
        }
      }
    }
    if (expanded) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
      ) {
        row.details.forEach { detail ->
          StudentAbsenceDetailRow(detail)
        }
      }
    }
  }
}

@Composable
private fun StudentAbsenceDetailRow(row: WakasekStudentMonitoringRow) {
  val palette = statusPalette(row.status)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(SoftPanel)
      .border(1.dp, CardBorder.copy(alpha = 0.62f), RoundedCornerShape(14.dp))
      .padding(horizontal = 11.dp, vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .background(palette.first, CircleShape)
    )
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(start = 10.dp)
    ) {
      Text(
        text = row.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("id-ID")) else it.toString() },
        style = MaterialTheme.typography.labelMedium,
        color = palette.second,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = listOf(row.subjectName.ifBlank { "-" }, formatShortDate(row.dateIso)).filter(String::isNotBlank).joinToString(" | "),
        style = MaterialTheme.typography.labelSmall,
        color = SubtleInk,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun WakasekLeaveRequestCard(
  item: LeaveRequestItem,
  isProcessing: Boolean,
  onClick: () -> Unit,
  onApprove: () -> Unit,
  onReject: () -> Unit
) {
  val palette = statusPalette(item.status)
  val pending = isPendingLeaveStatus(item.status)
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .clickable(enabled = !isProcessing, onClick = onClick)
      .padding(16.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(item.teacherName.ifBlank { "Guru" }, style = MaterialTheme.typography.titleMedium, color = PrimaryBlueDark, fontWeight = FontWeight.ExtraBold)
        Text("${formatShortDate(item.startDateIso)} - ${formatShortDate(item.endDateIso)} • ${item.durationDays} hari", style = MaterialTheme.typography.bodySmall, color = SubtleInk)
      }
      Text(
        text = item.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("id-ID")) else it.toString() },
        style = MaterialTheme.typography.labelMedium,
        color = palette.second,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .background(palette.first.copy(alpha = 0.16f))
          .padding(horizontal = 10.dp, vertical = 7.dp)
      )
    }
    if (isProcessing) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(HighlightCard.copy(alpha = 0.14f))
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          strokeWidth = 2.dp,
          color = PrimaryBlue
        )
        Text(
          text = t("Memproses keputusan wakasek..."),
          style = MaterialTheme.typography.labelMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
    Text(
      text = item.purpose,
      style = MaterialTheme.typography.bodyMedium,
      color = PrimaryBlueDark,
      modifier = Modifier.padding(top = 10.dp)
    )
    if (item.reviewerNote.isNotBlank()) {
      Text(
        text = "${t("Catatan")}: ${item.reviewerNote}",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        modifier = Modifier.padding(top = 6.dp)
      )
    }
    if (!isProcessing) {
      Text(
        text = if (pending) {
          t("Ketuk card atau pilih keputusan di bawah.")
        } else {
          t("Ketuk card untuk meninjau ulang keputusan.")
        },
        style = MaterialTheme.typography.labelSmall,
        color = SubtleInk,
        modifier = Modifier.padding(top = 8.dp)
      )
    }
    if (pending && !isProcessing) {
      Row(
        modifier = Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(onClick = onApprove, enabled = !isProcessing, modifier = Modifier.weight(1f)) {
          Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(t("Setujui"), modifier = Modifier.padding(start = 6.dp))
        }
        TextButton(onClick = onReject, enabled = !isProcessing, modifier = Modifier.weight(1f)) {
          Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFBE123C))
          Text(t("Tolak"), color = Color(0xFFBE123C), modifier = Modifier.padding(start = 6.dp))
        }
      }
    }
  }
}

@Composable
private fun WakasekReviewDialog(
  item: LeaveRequestItem,
  approved: Boolean,
  onDismiss: () -> Unit,
  onSubmit: (String) -> Unit
) {
  var note by rememberSaveable(item.id, approved) { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (approved) "Setujui Izin" else "Tolak Izin") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${item.teacherName.ifBlank { "Guru" }} • ${formatShortDate(item.startDateIso)} - ${formatShortDate(item.endDateIso)}")
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text(t("Catatan wakasek")) },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2
        )
      }
    },
    confirmButton = {
      TextButton(onClick = { onSubmit(note) }) {
        Text(if (approved) "Setujui" else "Tolak")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(t("Batal")) }
    }
  )
}

@Composable
private fun WakasekReviewChoiceDialog(
  item: LeaveRequestItem,
  onDismiss: () -> Unit,
  onSubmit: (Boolean, String) -> Unit
) {
  var note by rememberSaveable(item.id) { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(t("Tentukan Status Izin")) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = item.teacherName.ifBlank { "Guru" },
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = "${formatShortDate(item.startDateIso)} - ${formatShortDate(item.endDateIso)} | ${item.durationDays} ${t("hari")}",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
        Text(
          text = item.purpose.ifBlank { t("Keperluan izin belum diisi.") },
          style = MaterialTheme.typography.bodyMedium,
          color = PrimaryBlueDark
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text(t("Catatan wakasek")) },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2
        )
      }
    },
    confirmButton = {
      Button(onClick = { onSubmit(true, note) }) {
        Text(t("Setujui"))
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = { onSubmit(false, note) }) {
          Text(t("Tolak"), color = Color(0xFFBE123C))
        }
        TextButton(onClick = onDismiss) {
          Text(t("Batal"))
        }
      }
    }
  )
}

private fun summarizeTeacherRows(
  rows: List<WakasekTeacherMonitoringRow>,
  filter: WakasekPeriodFilter
): List<TeacherMonitorSummary> {
  val filtered = rows.filter { isDateInFilter(it.periodKey, filter) }
  return filtered
    .groupBy { it.teacherId.ifBlank { it.teacherName } }
    .map { (teacherKey, group) ->
      val details = group.sortedWith(
        compareBy<WakasekTeacherMonitoringRow> { it.periodKey }
          .thenBy { it.timeLabel }
          .thenBy { it.className }
          .thenBy { it.subjectName }
      )
      TeacherMonitorSummary(
        teacherId = teacherKey,
        teacherName = group.firstOrNull()?.teacherName.orEmpty().ifBlank { "Guru" },
        total = group.sumOf { it.totalSessions },
        present = group.sumOf { it.presentCount },
        leave = group.sumOf { it.leaveCount },
        substitute = group.sumOf { it.substituteCount },
        absent = group.sumOf { it.absentCount },
        details = details
      )
    }
    .sortedBy { it.teacherName.lowercase() }
}

private fun filterStudentRows(
  rows: List<WakasekStudentMonitoringRow>,
  filter: WakasekPeriodFilter
): List<WakasekStudentMonitoringRow> {
  return rows.filter { isDateInFilter(it.dateIso, filter) }
}

private fun summarizeStudentRows(rows: List<WakasekStudentMonitoringRow>): List<StudentMonitorSummary> {
  return rows
    .filterNot { normalizedStudentStatus(it.status) == "hadir" }
    .groupBy { it.studentId.ifBlank { "${it.className}|${it.studentName}" } }
    .map { (studentKey, group) ->
      StudentMonitorSummary(
        studentKey = studentKey,
        studentName = group.firstOrNull()?.studentName.orEmpty().ifBlank { "Siswa" },
        className = group.firstOrNull()?.className.orEmpty(),
        details = group.sortedWith(
          compareByDescending<WakasekStudentMonitoringRow> { it.dateIso }
            .thenBy { statusSortRank(it.status) }
            .thenBy { it.subjectName.lowercase() }
        )
      )
    }
}

private fun sortStudentSummaries(
  rows: List<StudentMonitorSummary>,
  option: StudentSortOption
): List<StudentMonitorSummary> {
  return when (option) {
    StudentSortOption.Class -> rows.sortedWith(
      compareBy<StudentMonitorSummary> { it.className.lowercase() }
        .thenBy { it.studentName.lowercase() }
        .thenBy { it.details.minOfOrNull { detail -> statusSortRank(detail.status) } ?: 5 }
    )
    StudentSortOption.Name -> rows.sortedWith(
      compareBy<StudentMonitorSummary> { it.studentName.lowercase() }
        .thenBy { it.className.lowercase() }
    )
    StudentSortOption.Status -> rows.sortedWith(
      compareBy<StudentMonitorSummary> { it.details.minOfOrNull { detail -> statusSortRank(detail.status) } ?: 5 }
        .thenBy { it.className.lowercase() }
        .thenBy { it.studentName.lowercase() }
    )
  }
}

private fun buildWakasekScoreMetrics(student: ScoreStudent): List<WakasekScoreMetric> {
  return WakasekScoreMetricDefinitions.map { definition ->
    WakasekScoreMetric(
      key = definition.key,
      label = definition.label,
      value = wakasekScoreValue(student, definition.key),
      details = if (definition.supportsDetailRows) {
        student.detailRowsByMetric[definition.key].orEmpty()
      } else {
        emptyList()
      }
    )
  }
}

private fun buildWakasekScoreTypeOverviews(students: List<ScoreStudent>): List<WakasekScoreTypeOverview> {
  return WakasekScoreMetricDefinitions.map { definition ->
    WakasekScoreTypeOverview(
      key = definition.key,
      label = definition.label,
      entries = students.map { student -> student to wakasekScoreValue(student, definition.key) }
    )
  }
}

private fun buildWakasekScoreDateOverviews(students: List<ScoreStudent>): List<WakasekScoreDateOverview> {
  val grouped = linkedMapOf<String, MutableList<WakasekScoreDateEntry>>()
  val labels = linkedMapOf<String, String>()
  students.forEach { student ->
    WakasekScoreMetricDefinitions
      .filter { it.supportsDetailRows }
      .forEach { definition ->
        student.detailRowsByMetric[definition.key].orEmpty()
          .filter { it.dateIso.isNotBlank() }
          .forEach { detail ->
            val dateIso = detail.dateIso.take(10)
            val key = "${definition.key}|$dateIso"
            labels[key] = "${formatWakasekScoreDate(dateIso)} - ${definition.label}"
            grouped.getOrPut(key) { mutableListOf() } += WakasekScoreDateEntry(
              studentName = student.name,
              value = detail.value,
              material = detail.material
            )
          }
      }
  }
  return grouped.map { (key, entries) ->
    val dateIso = key.substringAfter("|")
    WakasekScoreDateOverview(
      key = key,
      dateIso = dateIso,
      label = labels[key].orEmpty().ifBlank { formatWakasekScoreDate(dateIso) },
      entries = entries
    )
  }.sortedWith(compareByDescending<WakasekScoreDateOverview> { it.dateIso }.thenBy { it.label })
}

private fun wakasekScoreValue(student: ScoreStudent, key: String): Double? {
  return when (key) {
    "nilai_tugas" -> student.nilaiTugas
    "nilai_ulangan_harian" -> student.nilaiUlanganHarian
    "nilai_pts" -> student.nilaiPts
    "nilai_pas" -> student.nilaiPas
    "nilai_kehadiran" -> student.nilaiKehadiran
    "nilai_akhir" -> student.nilaiAkhir
    "nilai_keterampilan" -> student.nilaiKeterampilan
    else -> null
  }
}

private fun formatWakasekScoreNumber(value: Double?): String {
  val number = value ?: return "-"
  val rounded = number.roundToInt()
  return if (kotlin.math.abs(number - rounded) < 0.0001) {
    rounded.toString()
  } else {
    "%.2f".format(Locale("id", "ID"), number)
  }
}

private fun formatWakasekScoreDate(dateIso: String): String {
  return parseIsoDate(dateIso)?.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
    ?: dateIso.ifBlank { "-" }
}

private fun buildStudentSummaryChips(rows: List<WakasekStudentMonitoringRow>): List<TeacherSummaryChip> {
  val groups = rows.groupingBy { normalizedStudentStatus(it.status) }.eachCount()
  return listOf(
    "terlambat" to "Terlambat",
    "sakit" to "Sakit",
    "izin" to "Izin",
    "alpa" to "Alpa"
  ).mapNotNull { (key, label) ->
    val value = groups[key] ?: 0
    if (value <= 0) null else {
      val palette = statusPalette(key)
      TeacherSummaryChip(label, value, palette.first, palette.second)
    }
  }
}

private fun buildStudentStatusSummaries(rows: List<WakasekStudentMonitoringRow>): List<StudentStatusSummary> {
  val groups = rows.groupingBy { normalizedStudentStatus(it.status) }.eachCount()
  return listOf(
    "hadir" to "Hadir",
    "terlambat" to "Terlambat",
    "sakit" to "Sakit",
    "izin" to "Izin",
    "alpa" to "Alpa"
  ).mapNotNull { (key, label) ->
    val value = groups[key] ?: 0
    if (value <= 0) null else StudentStatusSummary(
      label = label,
      value = value,
      color = statusPalette(key).first
    )
  }
}

private fun normalizedStudentStatus(status: String): String {
  val clean = status.lowercase()
  return when {
    clean.contains("terlambat") -> "terlambat"
    clean.contains("sakit") -> "sakit"
    clean.contains("izin") -> "izin"
    clean.contains("alpa") || clean.contains("alpha") -> "alpa"
    clean.contains("hadir") || clean.contains("masuk") -> "hadir"
    else -> clean.ifBlank { "-" }
  }
}

private fun statusSortRank(status: String): Int {
  return when (normalizedStudentStatus(status)) {
    "alpa" -> 0
    "sakit" -> 1
    "izin" -> 2
    "terlambat" -> 3
    "hadir" -> 4
    else -> 5
  }
}

private fun isPendingLeaveStatus(status: String): Boolean {
  val clean = status.trim().lowercase()
  return clean == "menunggu" || clean == "pending" || clean == "diajukan" || clean == "menunggu persetujuan"
}

private fun isDateInFilter(dateIso: String, filter: WakasekPeriodFilter): Boolean {
  val date = runCatching { LocalDate.parse(dateIso.take(10)) }.getOrNull() ?: return false
  return when (filter.period) {
    WakasekPeriod.Day -> date == filter.selectedDate
    WakasekPeriod.Week -> !date.isBefore(filter.weekStart) && !date.isAfter(filter.weekStart.plusDays(6))
    WakasekPeriod.Month -> date.year == filter.selectedMonth.year && date.month == filter.selectedMonth.month
    WakasekPeriod.Semester -> !date.isBefore(filter.selectedSemester.start) && !date.isAfter(filter.selectedSemester.end)
  }
}

private fun parseIsoDate(value: String): LocalDate? {
  return runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
}

private fun startOfWeek(date: LocalDate): LocalDate {
  return date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
}

private fun weekStartsInMonth(month: YearMonth): List<LocalDate> {
  val first = month.atDay(1)
  val last = month.atEndOfMonth()
  val result = mutableListOf<LocalDate>()
  var cursor = startOfWeek(first)
  while (!cursor.isAfter(last)) {
    result += cursor
    cursor = cursor.plusWeeks(1)
  }
  return result
}

private fun weekOfMonthLabel(weekStart: LocalDate): String {
  val anchor = weekStart.plusDays(3)
  val monthLabel = anchor.format(DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("id-ID")))
  return "${weekOfMonthNumber(anchor)} $monthLabel"
}

private fun weekOfMonthNumber(date: LocalDate): Int {
  return ((date.dayOfMonth - 1) / 7) + 1
}

private fun monthsInSemester(semester: WakasekSemesterOption): List<YearMonth> {
  val months = mutableListOf<YearMonth>()
  var cursor = YearMonth.from(semester.start)
  val end = YearMonth.from(semester.end)
  while (!cursor.isAfter(end)) {
    months += cursor
    cursor = cursor.plusMonths(1)
  }
  return months
}

private fun semesterOptionFor(date: LocalDate): WakasekSemesterOption {
  return if (date.monthValue >= 7) {
    val year = date.year
    WakasekSemesterOption(
      key = "$year-ganjil",
      label = "Ganjil $year/${year + 1}",
      shortLabel = "Ganjil",
      start = LocalDate.of(year, 7, 1),
      end = LocalDate.of(year, 12, 31)
    )
  } else {
    val schoolYearStart = date.year - 1
    WakasekSemesterOption(
      key = "$schoolYearStart-genap",
      label = "Genap $schoolYearStart/${schoolYearStart + 1}",
      shortLabel = "Genap",
      start = LocalDate.of(date.year, 1, 1),
      end = LocalDate.of(date.year, 6, 30)
    )
  }
}

private fun buildSemesterOptions(dateValues: List<String>): List<WakasekSemesterOption> {
  val parsedDates = dateValues.mapNotNull(::parseIsoDate) + LocalDate.now()
  return parsedDates
    .map(::semesterOptionFor)
    .distinctBy { it.key }
    .sortedBy { it.start }
}

private fun statusPalette(status: String): Pair<Color, Color> {
  val normalized = status.lowercase()
  return when {
    normalized.contains("terima") || normalized.contains("setuju") || normalized == "hadir" || normalized == "masuk" ->
      Color(0xFF22C55E) to Color(0xFF166534)
    normalized.contains("sakit") -> Color(0xFFA78BFA) to Color(0xFF6D28D9)
    normalized.contains("izin") || normalized == "menunggu" -> Color(0xFFF59E0B) to Color(0xFF92400E)
    normalized.contains("terlambat") -> Color(0xFF38BDF8) to Color(0xFF0369A1)
    normalized.contains("ganti") -> Color(0xFF38BDF8) to Color(0xFF0369A1)
    else -> Color(0xFFFB7185) to Color(0xFFBE123C)
  }
}

private fun formatShortDate(dateIso: String): String {
  val date = runCatching { LocalDate.parse(dateIso.take(10)) }.getOrNull() ?: return dateIso.ifBlank { "-" }
  return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
}
