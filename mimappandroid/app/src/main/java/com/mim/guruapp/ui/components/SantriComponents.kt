package com.mim.guruapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.SantriSaveOutcome
import com.mim.guruapp.data.model.MapelScoreSnapshot
import com.mim.guruapp.data.model.MonthlyReportItem
import com.mim.guruapp.data.model.MonthlyReportSnapshot
import com.mim.guruapp.data.model.ScoreDetailRow
import com.mim.guruapp.data.model.ScoreStudent
import com.mim.guruapp.data.model.UtsClassSubject
import com.mim.guruapp.data.model.UtsReportSnapshot
import com.mim.guruapp.data.model.UtsScoreRow
import com.mim.guruapp.data.model.UtsSemesterInfo
import com.mim.guruapp.data.model.WaliSantriProfile
import com.mim.guruapp.data.model.WaliSantriSnapshot
import com.mim.guruapp.ui.i18n.t
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk
import kotlinx.coroutines.launch

private data class SantriGenderOption(
  val code: String,
  val label: String
)

private val SantriGenderOptions = listOf(
  SantriGenderOption("L", "Laki-laki"),
  SantriGenderOption("P", "Perempuan")
)

private enum class SantriDetailMode {
  Menu,
  Biodata,
  AcademicHistory
}

private enum class SantriAcademicClassMode {
  Menu,
  RaporList,
  RaporDetail,
  MonthlyList,
  MonthlyDetail,
  ScoreMetricList,
  ScoreMetricDetail
}

private data class SantriScoreMetricOption(
  val key: String,
  val label: String
)

private data class SantriMonthlyAspectSummary(
  val title: String,
  val value: String,
  val description: String,
  val accentColor: Color
)

private data class SantriAcademicClassHistory(
  val key: String,
  val classId: String,
  val className: String,
  val academicYearLabel: String,
  val semesters: List<SantriAcademicSemesterHistory>,
  val monthlyReports: List<MonthlyReportItem>
) {
  val subjectScores: List<SantriAcademicSubjectScore>
    get() = semesters.flatMap { it.subjectScores }
}

private data class SantriAcademicSemesterHistory(
  val semester: UtsSemesterInfo,
  val subjectScores: List<SantriAcademicSubjectScore>
)

private data class SantriAcademicSubjectScore(
  val subjectKey: String,
  val semesterLabel: String,
  val subjectName: String,
  val kkmText: String,
  val taskText: String,
  val dailyTestText: String,
  val ptsText: String,
  val finalExamText: String,
  val attendanceText: String,
  val totalText: String,
  val skillText: String,
  val knowledgeDescription: String,
  val skillDescription: String,
  val detailRowsByMetric: Map<String, List<ScoreDetailRow>>
)

private val SantriScoreMetricOptions = listOf(
  SantriScoreMetricOption("nilai_tugas", "Tugas"),
  SantriScoreMetricOption("nilai_ulangan_harian", "Ulangan Harian"),
  SantriScoreMetricOption("nilai_pts", "UTS"),
  SantriScoreMetricOption("nilai_pas", "UAS"),
  SantriScoreMetricOption("nilai_kehadiran", "Kehadiran"),
  SantriScoreMetricOption("nilai_akhir", "Nilai Akhir"),
  SantriScoreMetricOption("nilai_keterampilan", "Keterampilan")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SantriScreen(
  waliSantriSnapshot: WaliSantriSnapshot,
  monthlyReportSnapshot: MonthlyReportSnapshot,
  utsReportSnapshot: UtsReportSnapshot,
  scoreSnapshots: List<MapelScoreSnapshot>,
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onSaveSantri: suspend (WaliSantriProfile) -> SantriSaveOutcome,
  modifier: Modifier = Modifier
) {
  val editedProfiles = remember { mutableStateMapOf<String, WaliSantriProfile>() }
  val students = waliSantriSnapshot.students
    .map { student -> editedProfiles[student.id] ?: student }
    .sortedWith(compareBy<WaliSantriProfile> { it.className }.thenBy { it.name })
  var selectedSantriId by rememberSaveable { mutableStateOf<String?>(null) }
  var detailModeName by rememberSaveable { mutableStateOf(SantriDetailMode.Menu.name) }
  var selectedAcademicClassKey by rememberSaveable { mutableStateOf<String?>(null) }
  var query by rememberSaveable { mutableStateOf("") }
  var isSaving by rememberSaveable { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val selectedSantri = students.firstOrNull { it.id == selectedSantriId }
  val detailMode = runCatching { SantriDetailMode.valueOf(detailModeName) }.getOrDefault(SantriDetailMode.Menu)
  val showListSkeleton = students.isEmpty() && (isRefreshing || waliSantriSnapshot.updatedAt <= 0L)

  BackHandler(enabled = selectedSantri != null) {
    when {
      selectedAcademicClassKey != null -> selectedAcademicClassKey = null
      detailMode != SantriDetailMode.Menu -> detailModeName = SantriDetailMode.Menu.name
      else -> selectedSantriId = null
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      AnimatedContent(
        targetState = selectedSantri,
        transitionSpec = {
          val openingDetail = targetState != null
          fadeIn() + slideInHorizontally { width -> if (openingDetail) width / 5 else -width / 5 } togetherWith
            fadeOut() + slideOutHorizontally { width -> if (openingDetail) -width / 6 else width / 6 }
        },
        label = "santri-content",
        modifier = Modifier.fillMaxSize()
      ) { activeSantri ->
        if (activeSantri == null) {
          PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
          ) {
            SantriListContent(
              isWaliKelas = waliSantriSnapshot.isWaliKelas,
              students = students,
              showSkeleton = showListSkeleton,
              query = query,
              onQueryChange = { query = it },
              onMenuClick = onMenuClick,
              onSantriClick = {
                selectedSantriId = it.id
                detailModeName = SantriDetailMode.Menu.name
                selectedAcademicClassKey = null
              }
            )
          }
        } else {
          SantriDetailContent(
            santri = activeSantri,
            waliSantriSnapshot = waliSantriSnapshot,
            monthlyReportSnapshot = monthlyReportSnapshot,
            utsReportSnapshot = utsReportSnapshot,
            scoreSnapshots = scoreSnapshots,
            detailMode = detailMode,
            selectedAcademicClassKey = selectedAcademicClassKey,
            isSaving = isSaving,
            onBackClick = {
              when {
                selectedAcademicClassKey != null -> selectedAcademicClassKey = null
                detailMode != SantriDetailMode.Menu -> detailModeName = SantriDetailMode.Menu.name
                else -> selectedSantriId = null
              }
            },
            onModeChange = {
              detailModeName = it.name
              selectedAcademicClassKey = null
            },
            onAcademicClassChange = { selectedAcademicClassKey = it },
            onSaveClick = { draft ->
              scope.launch {
                isSaving = true
                val result = onSaveSantri(draft)
                isSaving = false
                if (result.success) {
                  editedProfiles.remove(activeSantri.id)
                }
                snackbarHostState.showSnackbar(result.message)
              }
            }
          )
        }
      }

      SavingOverlay(visible = isSaving)
    }
  }
}

@Composable
private fun SantriListContent(
  isWaliKelas: Boolean,
  students: List<WaliSantriProfile>,
  showSkeleton: Boolean,
  query: String,
  onQueryChange: (String) -> Unit,
  onMenuClick: () -> Unit,
  onSantriClick: (WaliSantriProfile) -> Unit
) {
  val filteredStudents = remember(students, query) {
    val needle = query.trim().lowercase()
    if (needle.isBlank()) {
      students
    } else {
      students.filter { student ->
        student.name.lowercase().contains(needle) ||
          student.nisn.lowercase().contains(needle) ||
          student.className.lowercase().contains(needle)
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .navigationBarsPadding()
      .padding(horizontal = 18.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    SantriTopBar(
      title = "Santri",
      leadingIcon = Icons.Outlined.Menu,
      leadingContentDescription = "Buka sidebar",
      onLeadingClick = onMenuClick
    )

    SantriSearchBar(
      query = query,
      onQueryChange = onQueryChange,
      modifier = Modifier.fillMaxWidth()
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(bottom = 124.dp)
    ) {
      when {
        showSkeleton -> {
          items(7) { index ->
            SantriSkeletonCard(index = index)
          }
        }

        filteredStudents.isEmpty() -> {
        item {
          EmptyPlaceholderCard(
            message = when {
              !isWaliKelas -> "Menu santri hanya tersedia untuk wali kelas. Tarik ke bawah untuk refresh data wali kelas."
              students.isEmpty() -> "Belum ada santri pada kelas wali ini."
              else -> "Tidak ada santri yang cocok dengan pencarian."
            }
          )
        }
      }

        else -> {
        items(filteredStudents, key = { it.id }) { student ->
          SantriCard(
            santri = student,
            onClick = { onSantriClick(student) }
          )
        }
      }
      }

      item {
        Spacer(modifier = Modifier.height(10.dp))
      }
    }
  }
}

@Composable
private fun SantriDetailContent(
  santri: WaliSantriProfile,
  waliSantriSnapshot: WaliSantriSnapshot,
  monthlyReportSnapshot: MonthlyReportSnapshot,
  utsReportSnapshot: UtsReportSnapshot,
  scoreSnapshots: List<MapelScoreSnapshot>,
  detailMode: SantriDetailMode,
  selectedAcademicClassKey: String?,
  isSaving: Boolean,
  onBackClick: () -> Unit,
  onModeChange: (SantriDetailMode) -> Unit,
  onAcademicClassChange: (String?) -> Unit,
  onSaveClick: (WaliSantriProfile) -> Unit
) {
  when (detailMode) {
    SantriDetailMode.Menu -> SantriDetailMenuContent(
      santri = santri,
      onBackClick = onBackClick,
      onModeChange = onModeChange
    )

    SantriDetailMode.Biodata -> SantriBiodataContent(
      santri = santri,
      isSaving = isSaving,
      onBackClick = onBackClick,
      onSaveClick = onSaveClick
    )

    SantriDetailMode.AcademicHistory -> SantriAcademicHistoryContent(
      santri = santri,
      waliSantriSnapshot = waliSantriSnapshot,
      monthlyReportSnapshot = monthlyReportSnapshot,
      utsReportSnapshot = utsReportSnapshot,
      scoreSnapshots = scoreSnapshots,
      selectedClassKey = selectedAcademicClassKey,
      onBackClick = onBackClick,
      onClassClick = onAcademicClassChange
    )
  }
}

@Composable
private fun SantriDetailMenuContent(
  santri: WaliSantriProfile,
  onBackClick: () -> Unit,
  onModeChange: (SantriDetailMode) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .navigationBarsPadding()
      .padding(horizontal = 18.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    SantriTopBar(
      title = "Detail Santri",
      leadingIcon = Icons.AutoMirrored.Outlined.ArrowBack,
      leadingContentDescription = "Kembali ke daftar santri",
      onLeadingClick = onBackClick
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(bottom = 124.dp)
    ) {
      item {
        SantriProfileHeader(
          name = santri.name,
          classLabel = santri.className.ifBlank { "Kelas belum tersedia" },
          nisn = santri.nisn
        )
      }
      item {
        SantriActionCard(
          title = "Biodata",
          subtitle = "Data santri, kontak orang tua, alamat, dan catatan.",
          icon = Icons.Outlined.Person,
          onClick = { onModeChange(SantriDetailMode.Biodata) }
        )
      }
      item {
        SantriActionCard(
          title = "Riwayat Akademik",
          subtitle = "Kelas, rapor, laporan bulanan, dan nilai santri.",
          icon = Icons.Outlined.School,
          onClick = { onModeChange(SantriDetailMode.AcademicHistory) }
        )
      }
      item {
        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun SantriAcademicHistoryContent(
  santri: WaliSantriProfile,
  waliSantriSnapshot: WaliSantriSnapshot,
  monthlyReportSnapshot: MonthlyReportSnapshot,
  utsReportSnapshot: UtsReportSnapshot,
  scoreSnapshots: List<MapelScoreSnapshot>,
  selectedClassKey: String?,
  onBackClick: () -> Unit,
  onClassClick: (String?) -> Unit
) {
  var classModeName by rememberSaveable(selectedClassKey) { mutableStateOf(SantriAcademicClassMode.Menu.name) }
  var selectedSemesterId by rememberSaveable(selectedClassKey) { mutableStateOf<String?>(null) }
  var selectedMonthlyPeriod by rememberSaveable(selectedClassKey) { mutableStateOf<String?>(null) }
  var selectedMetricKey by rememberSaveable(selectedClassKey) { mutableStateOf<String?>(null) }
  val histories = remember(
    santri.id,
    santri.classId,
    santri.className,
    waliSantriSnapshot.classes,
    monthlyReportSnapshot.reports,
    utsReportSnapshot.semesters,
    utsReportSnapshot.classSubjects,
    utsReportSnapshot.scoreRows,
    scoreSnapshots
  ) {
    buildSantriAcademicHistories(
      santri = santri,
      waliSantriSnapshot = waliSantriSnapshot,
      monthlyReportSnapshot = monthlyReportSnapshot,
      utsReportSnapshot = utsReportSnapshot,
      scoreSnapshots = scoreSnapshots
    )
  }
  val selectedHistory = histories.firstOrNull { it.key == selectedClassKey }
  val classMode = runCatching {
    SantriAcademicClassMode.valueOf(classModeName)
  }.getOrDefault(SantriAcademicClassMode.Menu)
  val selectedSemester = selectedHistory?.semesters?.firstOrNull { it.semester.id == selectedSemesterId }
  val selectedMonthlyReport = selectedHistory?.monthlyReports?.firstOrNull { it.period == selectedMonthlyPeriod }
  val selectedMetric = SantriScoreMetricOptions.firstOrNull { it.key == selectedMetricKey }

  fun openClassMenu() {
    classModeName = SantriAcademicClassMode.Menu.name
    selectedSemesterId = null
    selectedMonthlyPeriod = null
    selectedMetricKey = null
  }

  fun navigateBackInsideClass() {
    when (classMode) {
      SantriAcademicClassMode.RaporDetail -> {
        classModeName = SantriAcademicClassMode.RaporList.name
        selectedSemesterId = null
      }
      SantriAcademicClassMode.MonthlyDetail -> {
        classModeName = SantriAcademicClassMode.MonthlyList.name
        selectedMonthlyPeriod = null
      }
      SantriAcademicClassMode.ScoreMetricDetail -> {
        classModeName = SantriAcademicClassMode.ScoreMetricList.name
        selectedMetricKey = null
      }
      SantriAcademicClassMode.RaporList,
      SantriAcademicClassMode.MonthlyList,
      SantriAcademicClassMode.ScoreMetricList -> openClassMenu()
      SantriAcademicClassMode.Menu -> onClassClick(null)
    }
  }

  BackHandler(enabled = selectedHistory != null && classMode != SantriAcademicClassMode.Menu) {
    navigateBackInsideClass()
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .navigationBarsPadding()
      .padding(horizontal = 18.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    SantriTopBar(
      title = when {
        selectedHistory == null -> "Riwayat Akademik"
        classMode == SantriAcademicClassMode.RaporList -> "Rapor"
        classMode == SantriAcademicClassMode.RaporDetail -> "Detail Rapor"
        classMode == SantriAcademicClassMode.MonthlyList -> "Laporan Bulanan"
        classMode == SantriAcademicClassMode.MonthlyDetail -> "Detail Laporan"
        classMode == SantriAcademicClassMode.ScoreMetricList -> "Nilai"
        classMode == SantriAcademicClassMode.ScoreMetricDetail -> selectedMetric?.label ?: "Detail Nilai"
        else -> selectedHistory.className.toClassDisplayLabel()
      },
      leadingIcon = Icons.AutoMirrored.Outlined.ArrowBack,
      leadingContentDescription = "Kembali",
      onLeadingClick = {
        when {
          selectedHistory == null -> onBackClick()
          classMode == SantriAcademicClassMode.Menu -> onClassClick(null)
          else -> navigateBackInsideClass()
        }
      }
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(bottom = 124.dp)
    ) {
      if (selectedHistory == null) {
        item {
          SantriProfileHeader(
            name = santri.name,
            classLabel = santri.className.ifBlank { "Kelas belum tersedia" },
            nisn = santri.nisn
          )
        }

        if (histories.isEmpty()) {
          item {
            EmptyPlaceholderCard("Riwayat akademik santri ini belum tersedia.")
          }
        } else {
          items(histories, key = { it.key }) { history ->
            SantriAcademicClassCard(
              history = history,
              onClick = { onClassClick(history.key) }
            )
          }
        }
      } else {
        when (classMode) {
          SantriAcademicClassMode.Menu -> {
            item {
              SantriAcademicClassMenu(
                history = selectedHistory,
                onOpenRapor = { classModeName = SantriAcademicClassMode.RaporList.name },
                onOpenMonthly = { classModeName = SantriAcademicClassMode.MonthlyList.name },
                onOpenScores = { classModeName = SantriAcademicClassMode.ScoreMetricList.name }
              )
            }
          }

          SantriAcademicClassMode.RaporList -> {
            if (selectedHistory.semesters.isEmpty()) {
              item { EmptyPlaceholderCard("Data rapor kelas ini belum tersedia.") }
            } else {
              items(selectedHistory.semesters, key = { it.semester.id }) { semesterHistory ->
                SantriActionCard(
                  title = semesterHistory.semester.label.ifBlank { "Semester" },
                  subtitle = semesterHistory.semester.academicYearDisplayLabel(),
                  icon = Icons.Outlined.Grade,
                  onClick = {
                    selectedSemesterId = semesterHistory.semester.id
                    classModeName = SantriAcademicClassMode.RaporDetail.name
                  }
                )
              }
            }
          }

          SantriAcademicClassMode.RaporDetail -> {
            item {
              if (selectedSemester == null) {
                EmptyPlaceholderCard("Detail rapor belum tersedia.")
              } else {
                SantriAcademicRaporDetail(
                  santri = santri,
                  history = selectedHistory,
                  semesterHistory = selectedSemester
                )
              }
            }
          }

          SantriAcademicClassMode.MonthlyList -> {
            if (selectedHistory.monthlyReports.isEmpty()) {
              item { EmptyPlaceholderCard("Laporan bulanan kelas ini belum tersedia.") }
            } else {
              items(selectedHistory.monthlyReports, key = { it.period }) { report ->
                SantriActionCard(
                  title = report.period.toMonthlyPeriodLabel(),
                  subtitle = report.predikat.ifBlank { "Predikat belum tersedia" },
                  icon = Icons.AutoMirrored.Outlined.StickyNote2,
                  onClick = {
                    selectedMonthlyPeriod = report.period
                    classModeName = SantriAcademicClassMode.MonthlyDetail.name
                  }
                )
              }
            }
          }

          SantriAcademicClassMode.MonthlyDetail -> {
            item {
              if (selectedMonthlyReport == null) {
                EmptyPlaceholderCard("Detail laporan bulanan belum tersedia.")
              } else {
                SantriAcademicMonthlyDetail(
                  santri = santri,
                  history = selectedHistory,
                  report = selectedMonthlyReport
                )
              }
            }
          }

          SantriAcademicClassMode.ScoreMetricList -> {
            SantriScoreMetricOptions.forEach { metric ->
              item(key = metric.key) {
                SantriActionCard(
                  title = metric.label,
                  subtitle = "",
                  icon = Icons.Outlined.Grade,
                  onClick = {
                    selectedMetricKey = metric.key
                    classModeName = SantriAcademicClassMode.ScoreMetricDetail.name
                  }
                )
              }
            }
          }

          SantriAcademicClassMode.ScoreMetricDetail -> {
            item {
              if (selectedMetric == null) {
                EmptyPlaceholderCard("Detail nilai belum tersedia.")
              } else {
                SantriAcademicScoreMetricDetail(
                  history = selectedHistory,
                  metric = selectedMetric
                )
              }
            }
          }
        }
      }
      item {
        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun SantriAcademicClassMenu(
  history: SantriAcademicClassHistory,
  onOpenRapor: () -> Unit,
  onOpenMonthly: () -> Unit,
  onOpenScores: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    SantriFormPanel(
      title = history.className.toClassDisplayLabel(),
      subtitle = history.academicYearLabel.ifBlank { "Tahun ajaran belum tersedia" },
      content = { SantriMutedText("Pilih data akademik yang ingin dilihat.") }
    )
    SantriActionCard(
      title = "Rapor",
      subtitle = "Lihat daftar rapor semester di kelas ini.",
      icon = Icons.Outlined.Grade,
      onClick = onOpenRapor
    )
    SantriActionCard(
      title = "Laporan Bulanan",
      subtitle = "Lihat laporan bulanan selama di kelas ini.",
      icon = Icons.AutoMirrored.Outlined.StickyNote2,
      onClick = onOpenMonthly
    )
    SantriActionCard(
      title = "Nilai",
      subtitle = "Lihat riwayat nilai tugas, ujian, kehadiran, dan keterampilan.",
      icon = Icons.Outlined.School,
      onClick = onOpenScores
    )
  }
}

@Composable
private fun SantriAcademicRaporDetail(
  santri: WaliSantriProfile,
  history: SantriAcademicClassHistory,
  semesterHistory: SantriAcademicSemesterHistory
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SantriProfileHeader(
      name = santri.name,
      classLabel = history.className.toClassDisplayLabel(),
      nisn = santri.nisn
    )
    SantriFormPanel(
      title = "Identitas Rapor",
      subtitle = "Data rapor santri pada semester ini.",
      content = {
        SantriSimpleRow("Nama", santri.name.ifBlank { "-" })
        SantriSimpleRow("NISN", santri.nisn.ifBlank { "-" })
        SantriSimpleRow("Semester", semesterHistory.semester.label.ifBlank { "-" })
        SantriSimpleRow("Tahun ajaran", semesterHistory.semester.academicYearDisplayLabel().ifBlank { history.academicYearLabel.orDash() })
      }
    )
    if (semesterHistory.subjectScores.isEmpty()) {
      EmptyPlaceholderCard("Belum ada data nilai rapor untuk semester ini.")
    } else {
      semesterHistory.subjectScores.forEach { score ->
        SantriRaporSubjectCard(score)
      }
    }
  }
}

@Composable
private fun SantriRaporSubjectCard(score: SantriAcademicSubjectScore) {
  SantriFormPanel(
    title = score.subjectName,
    subtitle = "KKM ${score.kkmText.orDash()}",
    content = {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SantriHistoryStat("Pengetahuan", score.totalText.orDash(), Modifier.weight(1f))
        SantriHistoryStat("Keterampilan", score.skillText.orDash(), Modifier.weight(1f))
      }
      SantriSimpleRow("Deskripsi pengetahuan", score.knowledgeDescription.orDash())
      SantriSimpleRow("Deskripsi keterampilan", score.skillDescription.orDash())
    }
  )
}

@Composable
private fun SantriAcademicMonthlyDetail(
  santri: WaliSantriProfile,
  history: SantriAcademicClassHistory,
  report: MonthlyReportItem
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SantriMonthlyDetailHeaderCard(
      name = santri.name,
      classLabel = history.className.toClassDisplayLabel(),
      periodLabel = report.period.toMonthlyPeriodLabel(),
      academicYearLabel = history.academicYearLabel,
      nisn = santri.nisn
    )

    SantriMonthlyDetailSection(
      title = "Wali Kelas",
      subtitle = "Laporan perkembangan kelas"
    ) {
      SantriMonthlyAspectCard(
        title = "Akhlak",
        value = report.nilaiAkhlak.orDash(),
        description = report.predikat.orDash(),
        accentColor = Color(0xFF2563EB)
      )
      SantriMonthlyNoteCard(
        title = "Catatan Wali Kelas",
        note = report.catatanWali.orDash()
      )
    }

    SantriMonthlyDetailSection(
      title = "Ketahfizan",
      subtitle = buildSantriMonthlyMentorLabel("Muhaffiz", report.muhaffiz, report.noHpMuhaffiz)
    ) {
      SantriMonthlyAspectCard(
        title = "Kehadiran Halaqah",
        value = report.nilaiKehadiranHalaqah.withPercentSuffix(),
        description = "Sakit ${report.sakitHalaqah.orZero()} hari, izin ${report.izinHalaqah.orZero()} hari",
        accentColor = Color(0xFF16A34A)
      )
      SantriMonthlyAspectCard(
        title = "Akhlak Halaqah",
        value = report.nilaiAkhlakHalaqah.orDash(),
        description = report.keteranganAkhlakHalaqah.orDash(),
        accentColor = Color(0xFF7C3AED)
      )
      SantriMonthlyAspectCard(
        title = "Ujian Bulanan",
        value = report.nilaiUjianBulanan.orDash(),
        description = report.keteranganUjianBulanan.orDash(),
        accentColor = Color(0xFF0891B2)
      )
      SantriMonthlyAspectCard(
        title = "Target Hafalan",
        value = report.nilaiTargetHafalan.withPercentSuffix(),
        description = report.keteranganTargetHafalan.orDash(),
        accentColor = Color(0xFFEA580C)
      )
      SantriMonthlyAspectCard(
        title = "Capaian Hafalan Bulanan",
        value = "",
        description = report.keteranganCapaianHafalanBulanan.ifBlank { report.nilaiCapaianHafalanBulanan }.orDash(),
        accentColor = Color(0xFF0D9488)
      )
      SantriMonthlyAspectCard(
        title = "Jumlah Hafalan",
        value = "",
        description = buildSantriMonthlyJumlahHafalanLabel(report),
        accentColor = Color(0xFF0D9488)
      )
      SantriMonthlyNoteCard(
        title = "Catatan Muhaffiz",
        note = report.catatanMuhaffiz.orDash()
      )
    }

    SantriMonthlyDetailSection(
      title = "Kesantrian",
      subtitle = buildSantriMonthlyMentorLabel("Musyrif", report.musyrif, report.noHpMusyrif)
    ) {
      SantriMonthlyAspectCard(
        title = "Kehadiran Liqa",
        value = report.nilaiKehadiranLiqaMuhasabah.withPercentSuffix(),
        description = "Sakit ${report.sakitLiqaMuhasabah.orZero()} hari, izin ${report.izinLiqaMuhasabah.orZero()} hari",
        accentColor = Color(0xFF16A34A)
      )
      SantriMonthlyCompactAspectGrid(
        items = listOf(
          SantriMonthlyAspectSummary("Ibadah", report.nilaiIbadah.orDash(), report.keteranganIbadah.orDash(), Color(0xFF2563EB)),
          SantriMonthlyAspectSummary("Kedisiplinan", report.nilaiKedisiplinan.orDash(), report.keteranganKedisiplinan.orDash(), Color(0xFFEA580C)),
          SantriMonthlyAspectSummary("Kebersihan", report.nilaiKebersihan.orDash(), report.keteranganKebersihan.orDash(), Color(0xFF0D9488)),
          SantriMonthlyAspectSummary("Adab", report.nilaiAdab.orDash(), report.keteranganAdab.orDash(), Color(0xFF7C3AED))
        )
      )
      SantriMonthlyAspectCard(
        title = "Prestasi",
        value = "",
        description = report.prestasiKesantrian.orDash(),
        accentColor = Color(0xFF16A34A)
      )
      SantriMonthlyAspectCard(
        title = "Pelanggaran",
        value = "",
        description = report.pelanggaranKesantrian.orDash(),
        accentColor = Color(0xFFDC2626)
      )
      SantriMonthlyNoteCard(
        title = "Catatan Musyrif",
        note = report.catatanMusyrif.orDash()
      )
    }
  }
}

@Composable
private fun SantriMonthlyDetailHeaderCard(
  name: String,
  classLabel: String,
  periodLabel: String,
  academicYearLabel: String,
  nisn: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
      .clip(RoundedCornerShape(28.dp))
      .background(PrimaryBlue.copy(alpha = 0.10f))
      .border(1.dp, PrimaryBlue.copy(alpha = 0.18f), RoundedCornerShape(28.dp))
      .padding(18.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    SantriAvatar(name = name, size = 84)
    Text(
      text = name.ifBlank { t("Nama Santri") },
      style = MaterialTheme.typography.titleLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      textAlign = TextAlign.Center
    )
    Text(
      text = listOf(
        classLabel.ifBlank { "-" },
        periodLabel.ifBlank { "-" },
        academicYearLabel.ifBlank { null },
        if (nisn.isBlank()) null else "NISN $nisn"
      ).filterNotNull().joinToString(" - "),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      textAlign = TextAlign.Center
    )
  }
}

@Composable
private fun SantriMonthlyDetailSection(
  title: String,
  subtitle: String,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x120F172A), spotColor = Color(0x120F172A))
      .clip(RoundedCornerShape(24.dp))
      .background(CardBackground.copy(alpha = 0.96f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(
        text = t(title),
        style = MaterialTheme.typography.titleMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      if (subtitle.isNotBlank()) {
        Text(
          text = t(subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
    }
    content()
  }
}

@Composable
private fun SantriMonthlyAspectCard(
  title: String,
  value: String,
  description: String,
  accentColor: Color
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(SoftPanel.copy(alpha = 0.74f))
      .border(1.dp, CardBorder.copy(alpha = 0.72f), RoundedCornerShape(20.dp))
      .padding(14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(CircleShape)
        .background(accentColor.copy(alpha = 0.15f)),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = value.takeIf { it.isNotBlank() && it != "-" } ?: "-",
        style = MaterialTheme.typography.labelLarge,
        color = accentColor,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Text(
        text = t(title),
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = description.orDash(),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
  }
}

@Composable
private fun SantriMonthlyCompactAspectGrid(items: List<SantriMonthlyAspectSummary>) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    items.chunked(2).forEach { rowItems ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        rowItems.forEach { item ->
          SantriMonthlyCompactAspectCard(
            item = item,
            modifier = Modifier.weight(1f)
          )
        }
        if (rowItems.size == 1) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun SantriMonthlyCompactAspectCard(
  item: SantriMonthlyAspectSummary,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(SoftPanel.copy(alpha = 0.74f))
      .border(1.dp, CardBorder.copy(alpha = 0.72f), RoundedCornerShape(18.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(item.accentColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = item.value.takeIf { it.isNotBlank() && it != "-" } ?: "-",
          style = MaterialTheme.typography.labelMedium,
          color = item.accentColor,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1
        )
      }
      Text(
        text = t(item.title),
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Text(
      text = item.description.orDash(),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      maxLines = 3,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun SantriMonthlyNoteCard(
  title: String,
  note: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(SoftPanel.copy(alpha = 0.74f))
      .border(1.dp, CardBorder.copy(alpha = 0.72f), RoundedCornerShape(20.dp))
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(
      text = t(title),
      style = MaterialTheme.typography.titleSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = note.orDash(),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
  }
}

@Composable
private fun SantriAcademicScoreMetricDetail(
  history: SantriAcademicClassHistory,
  metric: SantriScoreMetricOption
) {
  var expandedSubjectKey by rememberSaveable(history.key, metric.key) { mutableStateOf<String?>(null) }
  val subjectScores = history.semesters.flatMap { semester ->
    semester.subjectScores.map { score -> semester.semester to score }
  }
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SantriFormPanel(
      title = metric.label,
      subtitle = "${history.className.toClassDisplayLabel()} - ${history.academicYearLabel.ifBlank { "Tahun ajaran belum tersedia" }}",
      content = { SantriMutedText("Riwayat nilai per mapel dan tanggal input.") }
    )
    if (subjectScores.isEmpty()) {
      EmptyPlaceholderCard("Data nilai kelas ini belum tersedia.")
    } else {
      subjectScores.forEach { (semester, score) ->
        val key = "${semester.id}|${score.subjectKey}|${metric.key}"
        SantriScoreMetricSubjectCard(
          semester = semester,
          score = score,
          metric = metric,
          expanded = expandedSubjectKey == key,
          onClick = {
            expandedSubjectKey = if (expandedSubjectKey == key) null else key
          }
        )
      }
    }
  }
}

@Composable
private fun SantriScoreMetricSubjectCard(
  semester: UtsSemesterInfo,
  score: SantriAcademicSubjectScore,
  metric: SantriScoreMetricOption,
  expanded: Boolean,
  onClick: () -> Unit
) {
  val detailRows = score.detailRowsByMetric[metric.key].orEmpty()
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(CardBackground.copy(alpha = 0.96f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(22.dp))
      .clickable(onClick = onClick)
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp)
      ) {
        Text(
          text = score.subjectName,
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = listOf(
            semester.label.ifBlank { "Semester" },
            "${detailRows.size} riwayat"
          ).joinToString(" - "),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      SantriScorePill(metric.label, score.metricValueText(metric.key), Modifier.width(92.dp))
      Icon(
        imageVector = Icons.Outlined.KeyboardArrowDown,
        contentDescription = t(if (expanded) "Tutup riwayat nilai" else "Buka riwayat nilai"),
        tint = PrimaryBlueDark,
        modifier = Modifier.size(22.dp)
      )
    }

    if (expanded) {
      if (detailRows.isEmpty()) {
        SantriMutedText("Riwayat input belum tersedia. Ringkasan nilai tetap ditampilkan dari data nilai akhir.")
      } else {
        detailRows.sortedByDescending { it.dateIso }.forEach { row ->
          SantriHistoryBlock(
            title = row.dateIso.ifBlank { "Tanggal belum tersedia" },
            subtitle = row.material.ifBlank { "Materi belum tersedia" }
          ) {
            SantriSimpleRow("Nilai", row.value?.let(::formatSantriScoreNumber).orDash())
          }
        }
      }
    }
  }
}

@Composable
private fun SantriBiodataContent(
  santri: WaliSantriProfile,
  isSaving: Boolean,
  onBackClick: () -> Unit,
  onSaveClick: (WaliSantriProfile) -> Unit
) {
  var name by rememberSaveable(santri.id, santri.name) { mutableStateOf(santri.name) }
  var nisn by rememberSaveable(santri.id, santri.nisn) { mutableStateOf(santri.nisn) }
  var gender by rememberSaveable(santri.id, santri.gender) { mutableStateOf(santri.gender) }
  var studentPhone by rememberSaveable(santri.id, santri.studentPhone) { mutableStateOf(santri.studentPhone) }
  var fatherName by rememberSaveable(santri.id, santri.fatherName) { mutableStateOf(santri.fatherName) }
  var fatherPhone by rememberSaveable(santri.id, santri.fatherPhone) { mutableStateOf(santri.fatherPhone) }
  var motherName by rememberSaveable(santri.id, santri.motherName) { mutableStateOf(santri.motherName) }
  var motherPhone by rememberSaveable(santri.id, santri.motherPhone) { mutableStateOf(santri.motherPhone) }
  var guardianName by rememberSaveable(santri.id, santri.guardianName) { mutableStateOf(santri.guardianName) }
  var guardianPhone by rememberSaveable(santri.id, santri.guardianPhone) { mutableStateOf(santri.guardianPhone) }
  var address by rememberSaveable(santri.id, santri.address) { mutableStateOf(santri.address) }
  var note by rememberSaveable(santri.id, santri.note) { mutableStateOf(santri.note) }

  val comparableSantri = santri.copy(gender = normalizeGenderCode(santri.gender))
  val draft = santri.copy(
    name = name,
    nisn = nisn,
    gender = normalizeGenderCode(gender),
    studentPhone = studentPhone,
    fatherName = fatherName,
    fatherPhone = fatherPhone,
    motherName = motherName,
    motherPhone = motherPhone,
    guardianName = guardianName,
    guardianPhone = guardianPhone,
    address = address,
    note = note
  )
  val isDirty = draft != comparableSantri

  Column(
    modifier = Modifier
      .fillMaxSize()
      .navigationBarsPadding()
      .padding(horizontal = 18.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    SantriTopBar(
      title = "Biodata Santri",
      leadingIcon = if (isDirty) Icons.Outlined.Close else Icons.AutoMirrored.Outlined.ArrowBack,
      leadingContentDescription = if (isDirty) "Batalkan perubahan" else "Kembali ke daftar santri",
      onLeadingClick = {
        if (isDirty) {
          name = santri.name
          nisn = santri.nisn
          gender = santri.gender
          studentPhone = santri.studentPhone
          fatherName = santri.fatherName
          fatherPhone = santri.fatherPhone
          motherName = santri.motherName
          motherPhone = santri.motherPhone
          guardianName = santri.guardianName
          guardianPhone = santri.guardianPhone
          address = santri.address
          note = santri.note
        } else {
          onBackClick()
        }
      },
      trailingIcon = if (isDirty) Icons.Outlined.Check else null,
      trailingContentDescription = if (isSaving) "Sedang menyimpan" else "Simpan detail santri",
      onTrailingClick = {
        if (!isSaving) onSaveClick(draft)
      }
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(bottom = 124.dp)
    ) {
      item {
        SantriProfileHeader(
          name = name,
          classLabel = santri.className.ifBlank { "Kelas belum tersedia" },
          nisn = nisn
        )
      }

      item {
        SantriFormPanel(
          title = "Biodata Santri",
          subtitle = "Data dasar santri wali kelas.",
          content = {
            SantriInputField(
              label = "Nama Santri",
              value = name,
              onValueChange = { name = it },
              leadingIcon = Icons.Outlined.Person
            )
            SantriInputField(
              label = "NISN",
              value = nisn,
              onValueChange = { nisn = it },
              leadingIcon = Icons.Outlined.School
            )
            SantriGenderDropdownField(
              label = "Jenis Kelamin",
              value = gender,
              onValueChange = { gender = it }
            )
            SantriInputField(
              label = "Kontak Santri",
              value = studentPhone,
              onValueChange = { studentPhone = it.filter(Char::isDigit) },
              leadingIcon = Icons.Outlined.Phone,
              keyboardType = KeyboardType.Phone
            )
          }
        )
      }

      item {
        SantriFormPanel(
          title = "Data Orang Tua & Wali",
          subtitle = "Nomor kontak dipisahkan agar laporan bisa dikirim ke penerima yang tepat.",
          content = {
            ContactPersonFields(
              title = "Ayah",
              name = fatherName,
              phone = fatherPhone,
              onNameChange = { fatherName = it },
              onPhoneChange = { fatherPhone = it.filter(Char::isDigit) }
            )
            ContactPersonFields(
              title = "Ibu",
              name = motherName,
              phone = motherPhone,
              onNameChange = { motherName = it },
              onPhoneChange = { motherPhone = it.filter(Char::isDigit) }
            )
            ContactPersonFields(
              title = "Wali",
              name = guardianName,
              phone = guardianPhone,
              onNameChange = { guardianName = it },
              onPhoneChange = { guardianPhone = it.filter(Char::isDigit) }
            )
          }
        )
      }

      item {
        SantriFormPanel(
          title = "Alamat & Catatan",
          subtitle = "Catatan tambahan untuk wali kelas.",
          content = {
            SantriInputField(
              label = "Alamat",
              value = address,
              onValueChange = { address = it },
              leadingIcon = Icons.Outlined.Home,
              singleLine = false,
              minLines = 2
            )
            SantriInputField(
              label = "Catatan",
              value = note,
              onValueChange = { note = it },
              leadingIcon = Icons.AutoMirrored.Outlined.StickyNote2,
              singleLine = false,
              minLines = 3
            )
          }
        )
      }

      item {
        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun SantriTopBar(
  title: String,
  leadingIcon: ImageVector,
  leadingContentDescription: String,
  onLeadingClick: () -> Unit,
  trailingIcon: ImageVector? = null,
  trailingContentDescription: String = "",
  onTrailingClick: () -> Unit = {}
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    SantriTopButton(
      icon = leadingIcon,
      contentDescription = leadingContentDescription,
      onClick = onLeadingClick
    )
    Text(
      text = t(title),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 12.dp),
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    if (trailingIcon != null) {
      SantriTopButton(
        icon = trailingIcon,
        contentDescription = trailingContentDescription,
        onClick = onTrailingClick
      )
    } else {
      Spacer(modifier = Modifier.size(42.dp))
    }
  }
}

@Composable
private fun SantriTopButton(
  icon: ImageVector,
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

@Composable
private fun SantriSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    placeholder = { Text(t("Cari nama atau NISN")) },
    singleLine = true,
    leadingIcon = {
      Icon(
        imageVector = Icons.Outlined.Search,
        contentDescription = t("Cari"),
        tint = SubtleInk
      )
    },
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    colors = santriTextFieldColors()
  )
}

@Composable
private fun SantriCard(
  santri: WaliSantriProfile,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(24.dp))
      .background(CardBackground.copy(alpha = 0.96f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
      .clickable(onClick = onClick)
      .padding(14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    SantriAvatar(name = santri.name, size = 50)
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Text(
        text = santri.name,
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = if (santri.nisn.isBlank()) t("NISN belum tersedia") else "NISN ${santri.nisn}",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun SantriActionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(24.dp))
      .background(CardBackground.copy(alpha = 0.96f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
      .clickable(onClick = onClick)
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(46.dp)
        .clip(CircleShape)
        .background(PrimaryBlue.copy(alpha = 0.12f))
        .border(1.dp, PrimaryBlue.copy(alpha = 0.18f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = t(title),
        tint = PrimaryBlue,
        modifier = Modifier.size(22.dp)
      )
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        text = t(title),
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      if (subtitle.isNotBlank()) {
        Text(
          text = t(subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun SantriAcademicClassCard(
  history: SantriAcademicClassHistory,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(24.dp))
      .background(CardBackground.copy(alpha = 0.96f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
      .clickable(onClick = onClick)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(PrimaryBlue.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.School,
          contentDescription = t("Kelas"),
          tint = PrimaryBlue,
          modifier = Modifier.size(22.dp)
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = history.className.toClassDisplayLabel(),
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = history.academicYearLabel.ifBlank { t("Tahun ajaran belum tersedia") },
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
    }
  }
}

@Composable
private fun SantriHistoryStat(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(SoftPanel.copy(alpha = 0.78f))
      .border(1.dp, CardBorder.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
      .padding(horizontal = 10.dp, vertical = 9.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 1
    )
    Text(
      text = t(label),
      style = MaterialTheme.typography.labelSmall,
      color = SubtleInk,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun SantriHistoryBlock(
  title: String,
  subtitle: String,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(SoftPanel.copy(alpha = 0.72f))
      .border(1.dp, CardBorder.copy(alpha = 0.72f), RoundedCornerShape(18.dp))
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = t(title),
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      if (subtitle.isNotBlank()) {
        Text(
          text = t(subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
    }
    content()
  }
}

@Composable
private fun SantriSimpleRow(
  title: String,
  value: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = t(title),
      style = MaterialTheme.typography.bodyMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.weight(1f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = SubtleInk,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.End
    )
  }
}

@Composable
private fun SantriMutedText(text: String) {
  Text(
    text = t(text),
    style = MaterialTheme.typography.bodySmall,
    color = SubtleInk
  )
}

@Composable
private fun SantriScorePill(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(CardBackground.copy(alpha = 0.78f))
      .border(1.dp, CardBorder.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
      .padding(horizontal = 8.dp, vertical = 7.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Text(
      text = t(label),
      style = MaterialTheme.typography.labelSmall,
      color = SubtleInk,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = value.orDash(),
      style = MaterialTheme.typography.bodySmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 1
    )
  }
}

@Composable
private fun SantriProfileHeader(
  name: String,
  classLabel: String,
  nisn: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Box(contentAlignment = Alignment.BottomEnd) {
      SantriAvatar(name = name, size = 94)
      Box(
        modifier = Modifier
          .size(30.dp)
          .background(PrimaryBlue, CircleShape)
          .border(2.dp, CardBackground, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.Edit,
          contentDescription = t("Edit foto santri"),
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = name.ifBlank { t("Nama Santri") },
        style = MaterialTheme.typography.titleLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center
      )
      Text(
        text = listOf(classLabel, nisn.ifBlank { null }).filterNotNull().joinToString(" - "),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun SantriFormPanel(
  title: String,
  subtitle: String,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
      .clip(RoundedCornerShape(28.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(28.dp))
      .padding(18.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Text(
      text = t(title),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = t(subtitle),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
    content()
  }
}

@Composable
private fun ContactPersonFields(
  title: String,
  name: String,
  phone: String,
  onNameChange: (String) -> Unit,
  onPhoneChange: (String) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(SoftPanel.copy(alpha = 0.72f))
      .border(1.dp, CardBorder.copy(alpha = 0.78f), RoundedCornerShape(20.dp))
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = t(title),
      style = MaterialTheme.typography.titleSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    SantriInputField(
      label = "Nama $title",
      value = name,
      onValueChange = onNameChange,
      leadingIcon = Icons.Outlined.Groups
    )
    SantriInputField(
      label = "Kontak $title",
      value = phone,
      onValueChange = onPhoneChange,
      leadingIcon = Icons.Outlined.Phone,
      keyboardType = KeyboardType.Phone
    )
  }
}

@Composable
private fun SantriGenderDropdownField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedLabel = genderDisplayLabel(value)

  Box(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(SoftPanel)
        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        .clickable { expanded = true }
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier.width(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.Person,
          contentDescription = t(label),
          tint = SubtleInk,
          modifier = Modifier.size(20.dp)
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = t(label),
          style = MaterialTheme.typography.labelMedium,
          color = SubtleInk,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = t(selectedLabel),
          style = MaterialTheme.typography.bodyMedium,
          color = if (normalizeGenderCode(value).isBlank()) SubtleInk else PrimaryBlueDark,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Icon(
        imageVector = Icons.Outlined.KeyboardArrowDown,
        contentDescription = t("Buka pilihan jenis kelamin"),
        tint = SubtleInk,
        modifier = Modifier.size(20.dp)
      )
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      SantriGenderOptions.forEach { option ->
        DropdownMenuItem(
          text = { Text(t(option.label)) },
          onClick = {
            onValueChange(option.code)
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
private fun SantriAvatar(
  name: String,
  size: Int
) {
  val initials = remember(name) {
    name
      .split(" ")
      .filter { it.isNotBlank() }
      .take(2)
      .joinToString("") { it.first().uppercase() }
      .ifBlank { "S" }
  }
  Box(
    modifier = Modifier
      .size(size.dp)
      .shadow(10.dp, CircleShape, ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
      .clip(CircleShape)
      .background(PrimaryBlue.copy(alpha = 0.12f))
      .border(1.dp, PrimaryBlue.copy(alpha = 0.24f), CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = initials,
      style = if (size > 60) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
      color = PrimaryBlue,
      fontWeight = FontWeight.ExtraBold
    )
  }
}

private fun buildSantriAcademicHistories(
  santri: WaliSantriProfile,
  waliSantriSnapshot: WaliSantriSnapshot,
  monthlyReportSnapshot: MonthlyReportSnapshot,
  utsReportSnapshot: UtsReportSnapshot,
  scoreSnapshots: List<MapelScoreSnapshot>
): List<SantriAcademicClassHistory> {
  val classNamesById = waliSantriSnapshot.classes.associate { it.id to it.name }
  val semestersById = utsReportSnapshot.semesters.associateBy { it.id }
  val studentReports = monthlyReportSnapshot.reports
    .filter { it.studentId == santri.id }
    .sortedByDescending { it.period }
  val classYearKeys = linkedSetOf<Pair<String, String>>()

  utsReportSnapshot.classSubjects
    .filter { it.classId == santri.classId }
    .forEach { subject ->
      val yearLabel = semestersById[subject.semesterId]?.academicYearDisplayLabel().orEmpty()
      if (subject.classId.isNotBlank() || yearLabel.isNotBlank()) {
        classYearKeys += subject.classId to yearLabel
      }
    }

  studentReports.forEach { report ->
    val classId = report.classId.ifBlank { santri.classId }
    classYearKeys += classId to report.period.academicYearLabelFromPeriod()
  }

  if (classYearKeys.isEmpty()) {
    val activeYear = utsReportSnapshot.semesters
      .firstOrNull { it.isActive }
      ?.academicYearDisplayLabel()
      ?: utsReportSnapshot.semesters.firstOrNull()?.academicYearDisplayLabel()
      ?: ""
    classYearKeys += santri.classId to activeYear
  }

  return classYearKeys.map { (classId, yearLabel) ->
    val className = classNamesById[classId].orEmpty()
      .ifBlank { if (classId == santri.classId) santri.className else "" }
      .ifBlank { "Kelas" }
    val classSemesters = utsReportSnapshot.semesters
      .filter { semester ->
        val semesterYear = semester.academicYearDisplayLabel()
        val subjectExists = utsReportSnapshot.classSubjects.any { subject ->
          subject.classId == classId && subject.semesterId == semester.id
        }
        subjectExists && (yearLabel.isBlank() || semesterYear == yearLabel)
      }
      .sortedWith(compareBy<UtsSemesterInfo> { it.startDateIso }.thenBy { it.label })
      .map { semester ->
        SantriAcademicSemesterHistory(
          semester = semester,
          subjectScores = buildSantriSubjectScores(
            santri = santri,
            semester = semester,
            classSubjects = utsReportSnapshot.classSubjects,
            scoreRows = utsReportSnapshot.scoreRows,
            classId = classId,
            scoreSnapshots = scoreSnapshots
          )
        )
      }
    val classReports = studentReports.filter { report ->
      val reportClassId = report.classId.ifBlank { santri.classId }
      reportClassId == classId &&
        (yearLabel.isBlank() || report.period.academicYearLabelFromPeriod() == yearLabel)
    }
    SantriAcademicClassHistory(
      key = "${classId.ifBlank { "kelas" }}|${yearLabel.ifBlank { "tahun" }}",
      classId = classId,
      className = className,
      academicYearLabel = yearLabel,
      semesters = classSemesters,
      monthlyReports = classReports
    )
  }.sortedWith(
    compareByDescending<SantriAcademicClassHistory> { it.academicYearLabel.take(4).toIntOrNull() ?: 0 }
      .thenBy { it.className.lowercase() }
  )
}

private fun buildSantriSubjectScores(
  santri: WaliSantriProfile,
  semester: UtsSemesterInfo,
  classSubjects: List<UtsClassSubject>,
  scoreRows: List<UtsScoreRow>,
  classId: String,
  scoreSnapshots: List<MapelScoreSnapshot>
): List<SantriAcademicSubjectScore> {
  return classSubjects
    .filter { it.classId == classId && it.semesterId == semester.id }
    .sortedWith(compareBy<UtsClassSubject> { it.name.lowercase() }.thenBy { it.mapelId })
    .map { subject ->
      val score = scoreRows.firstOrNull { row ->
        row.studentId == santri.id &&
          row.semesterId == semester.id &&
          row.mapelId == subject.mapelId
      }
      val detailScore = scoreSnapshots.findScoreStudentForSubject(santri, subject)
      SantriAcademicSubjectScore(
        subjectKey = subject.distribusiId.ifBlank { "${subject.semesterId}|${subject.mapelId}" },
        semesterLabel = semester.label,
        subjectName = subject.name.ifBlank { "-" },
        kkmText = subject.kkmText,
        taskText = detailScore?.nilaiTugas?.let(::formatSantriScoreNumber) ?: score?.taskScoreText.orEmpty(),
        dailyTestText = detailScore?.nilaiUlanganHarian?.let(::formatSantriScoreNumber) ?: score?.dailyTestScoreText.orEmpty(),
        ptsText = detailScore?.nilaiPts?.let(::formatSantriScoreNumber) ?: score?.scoreText.orEmpty(),
        finalExamText = detailScore?.nilaiPas?.let(::formatSantriScoreNumber) ?: score?.finalExamScoreText.orEmpty(),
        attendanceText = detailScore?.nilaiKehadiran?.let(::formatSantriScoreNumber) ?: score?.attendanceScoreText.orEmpty(),
        totalText = detailScore?.nilaiAkhir?.let(::formatSantriScoreNumber) ?: score?.knowledgeScoreText.orEmpty(),
        skillText = detailScore?.nilaiKeterampilan?.let(::formatSantriScoreNumber) ?: score?.skillScoreText.orEmpty(),
        knowledgeDescription = score?.knowledgeDescription.orEmpty(),
        skillDescription = score?.skillDescription.orEmpty(),
        detailRowsByMetric = detailScore?.detailRowsByMetric?.takeIf { it.isNotEmpty() } ?: score?.detailRowsByMetric.orEmpty()
      )
    }
}

private fun UtsSemesterInfo.academicYearDisplayLabel(): String {
  if (tahunAjaranLabel.isNotBlank()) return tahunAjaranLabel
  val startYear = startDateIso.take(4).toIntOrNull()
  if (startYear != null) return "$startYear/${startYear + 1}"
  return label.extractAcademicYearLabel()
}

private fun String.extractAcademicYearLabel(): String {
  val match = Regex("(20\\d{2})\\s*/\\s*(20\\d{2})").find(this)
  if (match != null) return "${match.groupValues[1]}/${match.groupValues[2]}"
  return ""
}

private fun String.academicYearLabelFromPeriod(): String {
  val year = take(4).toIntOrNull() ?: return ""
  val month = drop(5).take(2).toIntOrNull() ?: return "$year/${year + 1}"
  val startYear = if (month >= 7) year else year - 1
  return "$startYear/${startYear + 1}"
}

private fun String.toMonthlyPeriodLabel(): String {
  val year = take(4)
  val month = drop(5).take(2).toIntOrNull()
  val monthLabel = when (month) {
    1 -> "Januari"
    2 -> "Februari"
    3 -> "Maret"
    4 -> "April"
    5 -> "Mei"
    6 -> "Juni"
    7 -> "Juli"
    8 -> "Agustus"
    9 -> "September"
    10 -> "Oktober"
    11 -> "November"
    12 -> "Desember"
    else -> ""
  }
  return listOf(monthLabel, year).filter { it.isNotBlank() }.joinToString(" ").ifBlank { this }
}

private fun buildSantriMonthlyMentorLabel(
  role: String,
  name: String,
  phone: String
): String {
  val identity = listOf(name, phone).filter { it.isNotBlank() }.joinToString(" - ")
  return if (identity.isBlank()) "$role belum tersedia" else "$role: $identity"
}

private fun buildSantriMonthlyJumlahHafalanLabel(report: MonthlyReportItem): String {
  if (report.keteranganJumlahHafalanBulanan.isNotBlank()) return report.keteranganJumlahHafalanBulanan
  val parts = buildList {
    if (report.nilaiJumlahHafalanHalaman.isNotBlank()) add("${report.nilaiJumlahHafalanHalaman} halaman")
    if (report.nilaiJumlahHafalanJuz.isNotBlank()) add("${report.nilaiJumlahHafalanJuz} juz")
  }
  return parts.joinToString(", ").ifBlank { "-" }
}

private fun SantriAcademicClassHistory.scoreMetricSummary(metricKey: String): String {
  val totalSubjects = subjectScores.size
  if (totalSubjects == 0) return "Belum ada data nilai."
  val detailCount = subjectScores.sumOf { it.detailRowsByMetric[metricKey].orEmpty().size }
  return if (detailCount > 0) {
    "$detailCount riwayat nilai dari $totalSubjects mapel"
  } else {
    "$totalSubjects mapel dengan ringkasan nilai"
  }
}

private fun List<MapelScoreSnapshot>.findScoreStudentForSubject(
  santri: WaliSantriProfile,
  subject: UtsClassSubject
): ScoreStudent? {
  val snapshot = firstOrNull { it.distribusiId == subject.distribusiId && subject.distribusiId.isNotBlank() }
    ?: firstOrNull { scoreSnapshot ->
      scoreSnapshot.className.normalizedSantriKey() == santri.className.normalizedSantriKey() &&
        scoreSnapshot.subjectTitle.normalizedSantriKey() == subject.name.normalizedSantriKey()
    }
  return snapshot?.students?.firstOrNull { it.id == santri.id }
}

private fun SantriAcademicSubjectScore.metricValueText(metricKey: String): String {
  return when (metricKey) {
    "nilai_tugas" -> taskText
    "nilai_ulangan_harian" -> dailyTestText
    "nilai_pts" -> ptsText
    "nilai_pas" -> finalExamText
    "nilai_kehadiran" -> attendanceText
    "nilai_akhir" -> totalText
    "nilai_keterampilan" -> skillText
    else -> ""
  }
}

private fun String.normalizedSantriKey(): String {
  return trim().lowercase().replace(Regex("\\s+"), " ")
}

private fun String.toClassDisplayLabel(): String {
  val value = trim()
  if (value.isBlank()) return "Kelas"
  return if (value.startsWith("kelas", ignoreCase = true)) value else "Kelas $value"
}

private fun formatSantriScoreNumber(value: Double): String {
  val rounded = kotlin.math.round(value * 100.0) / 100.0
  return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "%.2f".format(rounded).trimEnd('0').trimEnd('.')
}

private fun String?.withPercentSuffix(): String {
  val value = orDash()
  if (value == "-") return value
  return if (value.endsWith("%")) value else "$value%"
}

private fun String?.orZero(): String {
  return this?.trim().orEmpty().ifBlank { "0" }
}

private fun String?.orDash(): String {
  return this?.trim().orEmpty().ifBlank { "-" }
}

private fun normalizeGenderCode(value: String): String {
  return when (value.trim().lowercase()) {
    "l", "lk", "laki", "laki-laki", "laki laki", "male" -> "L"
    "p", "pr", "perempuan", "female" -> "P"
    else -> value.trim().uppercase().take(1)
  }
}

private fun genderDisplayLabel(value: String): String {
  return when (normalizeGenderCode(value)) {
    "L" -> "Laki-laki"
    "P" -> "Perempuan"
    else -> "Pilih jenis kelamin"
  }
}

@Composable
private fun SantriInputField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  leadingIcon: ImageVector,
  keyboardType: KeyboardType = KeyboardType.Text,
  singleLine: Boolean = true,
  minLines: Int = 1
) {
  if (!singleLine) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(SoftPanel)
          .border(1.dp, CardBorder.copy(alpha = 0.72f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = t(label),
          tint = SubtleInk,
          modifier = Modifier.size(20.dp)
        )
      }
      OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(t(label)) },
        modifier = Modifier.weight(1f),
        singleLine = false,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = santriTextFieldColors()
      )
    }
    return
  }

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(t(label)) },
    modifier = Modifier.fillMaxWidth(),
    singleLine = singleLine,
    minLines = minLines,
    leadingIcon = {
      Box(
        modifier = Modifier.width(44.dp),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = t(label),
          tint = SubtleInk,
          modifier = Modifier.size(20.dp)
        )
      }
    },
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    shape = RoundedCornerShape(16.dp),
    colors = santriTextFieldColors()
  )
}

@Composable
private fun santriTextFieldColors() = TextFieldDefaults.colors(
  focusedContainerColor = SoftPanel,
  unfocusedContainerColor = SoftPanel,
  disabledContainerColor = SoftPanel,
  focusedIndicatorColor = PrimaryBlue.copy(alpha = 0.48f),
  unfocusedIndicatorColor = CardBorder,
  focusedLabelColor = PrimaryBlueDark,
  unfocusedLabelColor = SubtleInk,
  cursorColor = PrimaryBlueDark
)

@Composable
private fun SantriSkeletonCard(index: Int) {
  SkeletonContentCard(
    leadingSize = 50.dp,
    trailingSize = 58.dp,
    firstLineWidth = if (index % 2 == 0) 0.58f else 0.72f,
    secondLineWidth = if (index % 3 == 0) 0.34f else 0.46f
  )
}
