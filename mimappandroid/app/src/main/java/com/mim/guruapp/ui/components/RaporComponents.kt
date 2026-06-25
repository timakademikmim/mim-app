package com.mim.guruapp.ui.components

import android.content.Context
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.model.GuruProfile
import com.mim.guruapp.data.model.MapelAttendanceSnapshot
import com.mim.guruapp.data.model.MapelScoreSnapshot
import com.mim.guruapp.data.model.ScoreStudent
import com.mim.guruapp.data.model.SubjectOverview
import com.mim.guruapp.data.model.UtsClassSubject
import com.mim.guruapp.data.model.UtsReportSnapshot
import com.mim.guruapp.data.model.UtsScoreRow
import com.mim.guruapp.data.model.UtsSemesterInfo
import com.mim.guruapp.data.model.WaliSantriProfile
import com.mim.guruapp.data.model.WaliSantriSnapshot
import com.mim.guruapp.export.RaporDocxExporter
import com.mim.guruapp.export.RaporExportAffectiveRow
import com.mim.guruapp.export.RaporExportAttendanceRow
import com.mim.guruapp.export.RaporExportData
import com.mim.guruapp.export.RaporExportQuranData
import com.mim.guruapp.export.RaporExportQuranRow
import com.mim.guruapp.export.RaporExportSection
import com.mim.guruapp.export.RaporExportSimpleRow
import com.mim.guruapp.export.RaporExportSubject
import com.mim.guruapp.ui.i18n.t
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaporScreen(
  waliSantriSnapshot: WaliSantriSnapshot,
  utsReportSnapshot: UtsReportSnapshot,
  scoreSnapshots: List<MapelScoreSnapshot> = emptyList(),
  attendanceSnapshots: List<MapelAttendanceSnapshot> = emptyList(),
  profile: GuruProfile = GuruProfile(),
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadAttendance: suspend (String, SubjectOverview) -> MapelAttendanceSnapshot? = { _, _ -> null },
  onDetailModeChange: (Boolean) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val semesters = remember(utsReportSnapshot.semesters) {
    utsReportSnapshot.semesters.ifEmpty {
      listOf(UtsSemesterInfo(id = "", label = "Semester aktif"))
    }
  }
  var selectedSemesterId by rememberSaveable {
    mutableStateOf(semesters.firstOrNull { it.isActive }?.id ?: semesters.firstOrNull()?.id.orEmpty())
  }
  var selectedSantriId by rememberSaveable { mutableStateOf<String?>(null) }
  var query by rememberSaveable { mutableStateOf("") }
  var manualReports by remember(context) { mutableStateOf(loadRaporManualReports(context)) }
  var isImportDialogOpen by rememberSaveable { mutableStateOf(false) }
  var isDownloadDialogOpen by rememberSaveable { mutableStateOf(false) }
  var isResetDialogOpen by rememberSaveable { mutableStateOf(false) }
  var progressDialogState by remember { mutableStateOf<RaporProgressDialogState?>(null) }
  var requestedAttendanceLoadIds by remember(selectedSemesterId) { mutableStateOf<Set<String>>(emptySet()) }

  LaunchedEffect(semesters) {
    if (selectedSemesterId !in semesters.map { it.id }) {
      selectedSemesterId = semesters.firstOrNull { it.isActive }?.id ?: semesters.firstOrNull()?.id.orEmpty()
    }
  }

  val selectedSemester = semesters.firstOrNull { it.id == selectedSemesterId } ?: semesters.firstOrNull() ?: UtsSemesterInfo()
  val students = remember(waliSantriSnapshot.students) {
    waliSantriSnapshot.students.sortedWith(compareBy<WaliSantriProfile> { it.className }.thenBy { it.name })
  }
  val raporAttendanceSubjects = remember(utsReportSnapshot.classSubjects, selectedSemester, students) {
    val classIds = students.map { it.classId }.filter { it.isNotBlank() }.toSet()
    utsReportSnapshot.classSubjects
      .filter { subject ->
        subject.distribusiId.isNotBlank() &&
          subject.semesterId == selectedSemester.id &&
          (classIds.isEmpty() || subject.classId in classIds)
      }
      .distinctBy { it.distribusiId }
  }
  val classNameById = remember(waliSantriSnapshot.classes, students) {
    (waliSantriSnapshot.classes.associate { it.id to it.name } +
      students.mapNotNull { student ->
        student.classId.takeIf { it.isNotBlank() }?.let { it to student.className }
      }).filterValues { it.isNotBlank() }
  }
  LaunchedEffect(raporAttendanceSubjects, attendanceSnapshots, selectedSemester) {
    val loadedIds = attendanceSnapshots.map { it.distribusiId }.toSet()
    val missingSubjects = raporAttendanceSubjects
      .filter { it.distribusiId !in loadedIds && it.distribusiId !in requestedAttendanceLoadIds }
    if (missingSubjects.isEmpty()) return@LaunchedEffect
    requestedAttendanceLoadIds = requestedAttendanceLoadIds + missingSubjects.map { it.distribusiId }
    missingSubjects.forEach { subject ->
      onLoadAttendance(
        subject.distribusiId,
        subject.toSubjectOverview(
          className = classNameById[subject.classId].orEmpty(),
          semester = selectedSemester
        )
      )
    }
  }
  val raporDescriptionTemplateIds = remember(utsReportSnapshot.classSubjects, scoreSnapshots) {
    (utsReportSnapshot.classSubjects.map { it.distribusiId } + scoreSnapshots.map { it.distribusiId })
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
  }
  val raporDescriptionTemplates = loadRaporDescriptionTemplates(context, raporDescriptionTemplateIds)
  val reports = remember(students, selectedSemester, utsReportSnapshot, scoreSnapshots, attendanceSnapshots, manualReports, profile, raporDescriptionTemplates) {
    students.associate { student ->
      val manual = manualReports[buildRaporManualKey(student, selectedSemester)]
        ?: manualReports[student.id]
      student.id to buildRaporStudentReport(
        student = student,
        semester = selectedSemester,
        utsSnapshot = utsReportSnapshot,
        scoreSnapshots = scoreSnapshots,
        attendanceSnapshots = attendanceSnapshots,
        manual = manual,
        descriptionTemplates = raporDescriptionTemplates,
        fallbackWaliName = profile.name
      )
    }
  }
  val selectedSantri = students.firstOrNull { it.id == selectedSantriId }
  val showSkeleton = students.isEmpty() && (isRefreshing || waliSantriSnapshot.updatedAt <= 0L)

  LaunchedEffect(selectedSantriId) {
    onDetailModeChange(selectedSantriId != null)
  }
  DisposableEffect(Unit) {
    onDispose { onDetailModeChange(false) }
  }

  BackHandler(enabled = selectedSantri != null) {
    selectedSantriId = null
  }

  fun updateManualReports(nextReports: Map<String, RaporManualData>) {
    val cleanedReports = nextReports.filterValues { it.hasAnyManualData() }
    manualReports = cleanedReports
    saveRaporManualReports(context, cleanedReports)
  }

  fun resetManualReportsForCurrentStudents(section: RaporSection?): Int {
    val targetKeys = students.flatMap { student ->
      listOf(buildRaporManualKey(student, selectedSemester), student.id)
    }.toSet()
    var affectedCount = 0
    val nextReports = manualReports.toMutableMap()
    targetKeys.forEach { key ->
      val current = nextReports[key] ?: return@forEach
      val hasTargetData = section?.let { current.hasSectionManualData(it) } ?: current.hasAnyManualData()
      if (!hasTargetData) return@forEach
      affectedCount += 1
      val nextManual = section?.let { current.clearSection(it) }
      if (nextManual == null || !nextManual.hasAnyManualData()) {
        nextReports.remove(key)
      } else {
        nextReports[key] = nextManual
      }
    }
    if (affectedCount > 0) updateManualReports(nextReports)
    return affectedCount
  }

  fun launchExport(report: RaporStudentReport, share: Boolean) {
    scope.launch {
      progressDialogState = RaporProgressDialogState(
        title = if (share) "Menyiapkan Dokumen Kirim" else "Menyiapkan Dokumen Rapor",
        message = if (share) {
          "Sedang membuat file rapor dan membuka WhatsApp. Mohon tunggu sampai proses selesai."
        } else {
          "Sedang membuat file rapor. Mohon tunggu sampai dokumen terbuka."
        },
        isLoading = true
      )
      runCatching {
        val file = RaporDocxExporter.createDocxFile(context, report.toExportData())
        progressDialogState = null
        if (share) {
          val phone = selectedSantri?.guardianPhone
            ?.ifBlank { selectedSantri.fatherPhone }
            ?.ifBlank { selectedSantri.motherPhone }
            .orEmpty()
          RaporDocxExporter.shareDocumentToWhatsApp(
            context = context,
            documentFile = file,
            data = report.toExportData(),
            phone = phone
          )
        } else {
          RaporDocxExporter.openDocument(context, file, report.toExportData())
        }
      }.onFailure { error ->
        progressDialogState = RaporProgressDialogState(
          title = "Gagal Membuat Dokumen",
          message = error.message ?: "Gagal membuat dokumen rapor.",
          isLoading = false
        )
      }
    }
  }

  fun launchSectionDownload(section: RaporExportSection) {
    scope.launch {
      val exportData = students.mapNotNull { student -> reports[student.id]?.toExportData() }
      if (exportData.isEmpty()) {
        progressDialogState = RaporProgressDialogState(
          title = "Download Rapor",
          message = "Belum ada data santri untuk didownload.",
          isLoading = false
        )
        return@launch
      }
      progressDialogState = RaporProgressDialogState(
        title = "Download Rapor ${section.label}",
        message = "Sedang membuat file Word untuk ${exportData.size} santri. Mohon tunggu sampai dokumen terbuka.",
        isLoading = true
      )
      runCatching {
        val file = RaporDocxExporter.createSectionDocxFile(
          context = context,
          data = exportData,
          section = section
        )
        progressDialogState = null
        RaporDocxExporter.openDocument(
          context = context,
          documentFile = file,
          title = "Rapor ${section.label} ${selectedSemester.label}"
        )
      }.onFailure { error ->
        progressDialogState = RaporProgressDialogState(
          title = "Gagal Download Rapor",
          message = error.message ?: "Gagal membuat download rapor ${section.label}.",
          isLoading = false
        )
      }
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
          .navigationBarsPadding()
          .padding(horizontal = 16.dp)
          .padding(bottom = 96.dp)
      )
    },
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
  ) { innerPadding ->
    AnimatedContent(
      targetState = selectedSantri,
      transitionSpec = {
        val openingDetail = targetState != null
        fadeIn() + slideInHorizontally { width -> if (openingDetail) width / 5 else -width / 5 } togetherWith
          fadeOut() + slideOutHorizontally { width -> if (openingDetail) -width / 6 else width / 6 }
      },
      label = "rapor-content",
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) { activeSantri ->
      if (activeSantri == null) {
        PullToRefreshBox(
          isRefreshing = isRefreshing,
          onRefresh = onRefresh,
          modifier = Modifier.fillMaxSize()
        ) {
          RaporListContent(
            isWaliKelas = waliSantriSnapshot.isWaliKelas,
            students = students,
            reports = reports,
            semesters = semesters,
            selectedSemester = selectedSemester,
            selectedSemesterId = selectedSemesterId,
            onSemesterChange = { selectedSemesterId = it },
            query = query,
            onQueryChange = { query = it },
            showSkeleton = showSkeleton,
            onMenuClick = onMenuClick,
            onImportClick = { isImportDialogOpen = true },
            onDownloadClick = { isDownloadDialogOpen = true },
            onResetClick = { isResetDialogOpen = true },
            onSantriClick = { selectedSantriId = it.id }
          )
        }
      } else {
        val report = reports[activeSantri.id] ?: RaporStudentReport()
        val manualKey = buildRaporManualKey(activeSantri, selectedSemester)
        val manualData = manualReports[manualKey] ?: manualReports[activeSantri.id]
        RaporDetailContent(
          santri = activeSantri,
          report = report,
          manualData = manualData,
          onBackClick = { selectedSantriId = null },
          onPrintClick = { launchExport(report, share = false) },
          onSendClick = { launchExport(report, share = true) },
          onResetSectionClick = { section ->
            val nextManual = manualData?.clearSection(section)
            val nextReports = manualReports.toMutableMap().apply {
              remove(activeSantri.id)
              if (nextManual == null || !nextManual.hasAnyManualData()) {
                remove(manualKey)
              } else {
                put(manualKey, nextManual)
              }
            }
            updateManualReports(nextReports)
            scope.launch {
              snackbarHostState.showSnackbar("Data manual bagian ${section.label} direset.", duration = SnackbarDuration.Short)
            }
          }
        )
      }
    }
  }

  if (isImportDialogOpen) {
    RaporImportDialog(
      students = students,
      classNames = students.map { it.className }.filter { it.isNotBlank() }.distinct()
        .ifEmpty { waliSantriSnapshot.classes.map { it.name } },
      onDismiss = { isImportDialogOpen = false },
      onApply = { importSection, importedRows ->
        val nextReports = manualReports.toMutableMap().apply {
          importedRows.forEach { row ->
            val key = buildRaporManualKey(row.student, selectedSemester)
            val value = row.manual
            val current = this[key] ?: RaporManualData()
            put(key, current.mergeSection(importSection, value))
          }
        }
        updateManualReports(nextReports)
        isImportDialogOpen = false
        scope.launch {
          snackbarHostState.showSnackbar("${importedRows.size} data ${importSection.label} berhasil diimpor.", duration = SnackbarDuration.Short)
        }
      }
    )
  }

  if (isDownloadDialogOpen) {
    RaporDownloadDialog(
      onDismiss = { isDownloadDialogOpen = false },
      onDownload = { section ->
        isDownloadDialogOpen = false
        launchSectionDownload(section)
      }
    )
  }

  if (isResetDialogOpen) {
    RaporBulkResetDialog(
      semesterLabel = semesterDisplayLabel(selectedSemester),
      onDismiss = { isResetDialogOpen = false },
      onReset = { section ->
        val affectedCount = resetManualReportsForCurrentStudents(section)
        isResetDialogOpen = false
        scope.launch {
          val targetLabel = section?.label ?: "Semua bagian"
          snackbarHostState.showSnackbar(
            if (affectedCount > 0) {
              "Data manual $targetLabel untuk $affectedCount santri direset."
            } else {
              "Tidak ada data manual $targetLabel untuk direset."
            },
            duration = SnackbarDuration.Short
          )
        }
      }
    )
  }

  progressDialogState?.let { state ->
    RaporProgressDialog(
      state = state,
      onDismiss = {
        if (!state.isLoading) progressDialogState = null
      }
    )
  }
}

@Composable
private fun RaporListContent(
  isWaliKelas: Boolean,
  students: List<WaliSantriProfile>,
  reports: Map<String, RaporStudentReport>,
  semesters: List<UtsSemesterInfo>,
  selectedSemester: UtsSemesterInfo,
  selectedSemesterId: String,
  onSemesterChange: (String) -> Unit,
  query: String,
  onQueryChange: (String) -> Unit,
  showSkeleton: Boolean,
  onMenuClick: () -> Unit,
  onImportClick: () -> Unit,
  onDownloadClick: () -> Unit,
  onResetClick: () -> Unit,
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
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    RaporTopBar(
      title = "Rapor",
      leadingIcon = Icons.Outlined.Menu,
      leadingContentDescription = "Buka sidebar",
      onLeadingClick = onMenuClick,
      actions = {
        RaporListActionsMenu(
          onImportClick = onImportClick,
          onDownloadClick = onDownloadClick,
          onResetClick = onResetClick
        )
      }
    )

    RaporSemesterSelector(
      semesters = semesters,
      selectedSemesterId = selectedSemesterId,
      selectedSemester = selectedSemester,
      onSemesterChange = onSemesterChange
    )

    RaporSearchBar(
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
            RaporSkeletonCard(index = index)
          }
        }

        filteredStudents.isEmpty() -> {
          item {
            EmptyPlaceholderCard(
              message = when {
                !isWaliKelas -> "Menu rapor hanya tersedia untuk wali kelas. Tarik ke bawah untuk refresh data wali kelas."
                students.isEmpty() -> "Belum ada santri pada kelas wali ini."
                else -> "Tidak ada santri yang cocok dengan pencarian."
              }
            )
          }
        }

        else -> {
          items(filteredStudents, key = { it.id }) { student ->
            RaporStudentCard(
              santri = student,
              report = reports[student.id] ?: RaporStudentReport(),
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
private fun RaporDetailContent(
  santri: WaliSantriProfile,
  report: RaporStudentReport,
  manualData: RaporManualData?,
  onBackClick: () -> Unit,
  onPrintClick: () -> Unit,
  onSendClick: () -> Unit,
  onResetSectionClick: (RaporSection) -> Unit
) {
  var selectedSection by rememberSaveable { mutableStateOf(RaporSection.Info.name) }
  val activeSection = RaporSection.valueOf(selectedSection)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .navigationBarsPadding()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      RaporTopBar(
        title = "Detail Rapor",
        leadingIcon = Icons.AutoMirrored.Outlined.ArrowBack,
        leadingContentDescription = "Kembali ke daftar rapor",
        onLeadingClick = onBackClick,
        actions = {
          RaporTopButton(Icons.Outlined.Print, "Cetak rapor", onPrintClick)
          Spacer(modifier = Modifier.width(8.dp))
          RaporTopButton(Icons.AutoMirrored.Outlined.Send, "Kirim rapor", onSendClick)
        }
      )

      if (activeSection.canImport) {
        RaporSectionActionBar(
          section = activeSection,
          hasManualData = manualData.hasSectionManualData(activeSection),
          onResetClick = { onResetSectionClick(activeSection) }
        )
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 112.dp)
      ) {
        when (activeSection) {
          RaporSection.Info -> {
            item {
              RaporStudentHeader(
                santri = santri,
                semesterLabel = report.semesterLabel,
                academicYearLabel = report.academicYearLabel
              )
            }
            item { RaporIdentityPanel(report) }
          }

          RaporSection.Akhlak -> {
            item { RaporAttitudePanel(report) }
            item { RaporAffectivePanel(report) }
          }

          RaporSection.Alquran -> {
            item { RaporQuranPanel(report) }
          }

          RaporSection.Nilai -> {
            item { RaporAcademicSummaryPanel(report) }
            if (report.subjects.isEmpty()) {
              item { EmptyPlaceholderCard("Belum ada data nilai rapor untuk semester ini.") }
            } else {
              items(report.subjects, key = { it.subjectKey }) { subject ->
                RaporSubjectCard(subject)
              }
            }
          }

          RaporSection.Lainnya -> {
            item { RaporExtracurricularPanel(report) }
            item { RaporAchievementPanel(report) }
            item { RaporAttendancePanel(report) }
            item { RaporSignaturePanel(report) }
          }
        }
      }
    }

    RaporDetailBottomNav(
      selectedSection = activeSection,
      onSelectSection = { selectedSection = it.name },
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(horizontal = 18.dp, vertical = 10.dp)
    )
  }
}

@Composable
private fun RaporTopBar(
  title: String,
  leadingIcon: ImageVector,
  leadingContentDescription: String,
  onLeadingClick: () -> Unit,
  actions: @Composable RowScope.() -> Unit = { Spacer(modifier = Modifier.size(42.dp)) }
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    RaporTopButton(
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
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(0.dp),
      verticalAlignment = Alignment.CenterVertically,
      content = actions
    )
  }
}

@Composable
private fun RaporTopButton(
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
private fun RaporListActionsMenu(
  onImportClick: () -> Unit,
  onDownloadClick: () -> Unit,
  onResetClick: () -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    RaporTopButton(
      icon = Icons.Outlined.MoreVert,
      contentDescription = "Menu rapor",
      onClick = { expanded = true }
    )
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.background(CardBackground)
    ) {
      DropdownMenuItem(
        text = { Text("Import data") },
        leadingIcon = { Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = PrimaryBlue) },
        onClick = {
          expanded = false
          onImportClick()
        }
      )
      DropdownMenuItem(
        text = { Text("Download") },
        leadingIcon = { Icon(Icons.Outlined.Print, contentDescription = null, tint = PrimaryBlue) },
        onClick = {
          expanded = false
          onDownloadClick()
        }
      )
      DropdownMenuItem(
        text = { Text("Reset data") },
        leadingIcon = { Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = PrimaryBlue) },
        onClick = {
          expanded = false
          onResetClick()
        }
      )
    }
  }
}

@Composable
private fun RaporSemesterSelector(
  semesters: List<UtsSemesterInfo>,
  selectedSemesterId: String,
  selectedSemester: UtsSemesterInfo,
  onSemesterChange: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
      .clickable { expanded = true }
      .padding(horizontal = 14.dp, vertical = 12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Semester", style = MaterialTheme.typography.labelMedium, color = SubtleInk)
        Text(
          text = semesterDisplayLabel(selectedSemester),
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
      }
      Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = PrimaryBlueDark)
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.background(CardBackground)
    ) {
      semesters.forEach { semester ->
        DropdownMenuItem(
          text = { Text(semesterDisplayLabel(semester)) },
          onClick = {
            expanded = false
            onSemesterChange(semester.id)
          },
          leadingIcon = if (semester.id == selectedSemesterId) {
            { Icon(Icons.Outlined.Grade, contentDescription = null, tint = PrimaryBlue) }
          } else {
            null
          }
        )
      }
    }
  }
}

@Composable
private fun RaporDownloadDialog(
  onDismiss: () -> Unit,
  onDownload: (RaporExportSection) -> Unit
) {
  val sections = remember {
    listOf(
      RaporExportSection.Akhlak,
      RaporExportSection.Alquran,
      RaporExportSection.Nilai,
      RaporExportSection.Lainnya
    )
  }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Download Rapor") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sections.forEach { section ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(SoftPanel.copy(alpha = 0.62f))
              .clickable { onDownload(section) }
              .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = section.label,
              style = MaterialTheme.typography.titleSmall,
              color = PrimaryBlueDark,
              fontWeight = FontWeight.ExtraBold
            )
            Text(
              text = "Semua santri",
              style = MaterialTheme.typography.bodySmall,
              color = SubtleInk
            )
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Batal")
      }
    }
  )
}

@Composable
private fun RaporBulkResetDialog(
  semesterLabel: String,
  onDismiss: () -> Unit,
  onReset: (RaporSection?) -> Unit
) {
  val sections = remember {
    RaporSection.entries.filter { it.canImport }
  }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Reset Data Manual") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Pilih bagian yang akan dikembalikan ke data asli sistem untuk semua santri pada $semesterLabel.",
          style = MaterialTheme.typography.bodyMedium,
          color = SubtleInk
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PrimaryBlue.copy(alpha = 0.1f))
            .clickable { onReset(null) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Semua bagian",
            style = MaterialTheme.typography.titleSmall,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = "Semua santri",
            style = MaterialTheme.typography.bodySmall,
            color = SubtleInk
          )
        }
        sections.forEach { section ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(SoftPanel.copy(alpha = 0.62f))
              .clickable { onReset(section) }
              .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = section.label,
              style = MaterialTheme.typography.titleSmall,
              color = PrimaryBlueDark,
              fontWeight = FontWeight.ExtraBold
            )
            Text(
              text = "Semua santri",
              style = MaterialTheme.typography.bodySmall,
              color = SubtleInk
            )
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Batal")
      }
    }
  )
}

private data class RaporProgressDialogState(
  val title: String,
  val message: String,
  val isLoading: Boolean
)

@Composable
private fun RaporProgressDialog(
  state: RaporProgressDialogState,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = {
      if (!state.isLoading) onDismiss()
    },
    title = { Text(state.title) },
    text = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        if (state.isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(30.dp),
            color = PrimaryBlue,
            strokeWidth = 3.dp
          )
        }
        Text(
          text = state.message,
          style = MaterialTheme.typography.bodyMedium,
          color = PrimaryBlueDark
        )
      }
    },
    confirmButton = {
      if (!state.isLoading) {
        TextButton(onClick = onDismiss) {
          Text("Tutup")
        }
      }
    }
  )
}

@Composable
private fun RaporSectionActionBar(
  section: RaporSection,
  hasManualData: Boolean,
  onResetClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(PrimaryBlue.copy(alpha = 0.09f))
      .border(1.dp, PrimaryBlue.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
      .padding(10.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = "Data ${section.label}",
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = if (hasManualData) "Memakai data manual untuk cetak/kirim" else "Menggunakan data asli sistem",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
    TextButton(onClick = onResetClick, enabled = hasManualData) {
      Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = if (hasManualData) PrimaryBlue else SubtleInk)
      Spacer(modifier = Modifier.width(4.dp))
      Text("Reset")
    }
  }
}

@Composable
private fun RaporSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    singleLine = true,
    label = { Text(t("Cari santri")) },
    leadingIcon = {
      Icon(
        imageVector = Icons.Outlined.Search,
        contentDescription = t("Cari"),
        tint = SubtleInk
      )
    },
    modifier = modifier,
    shape = RoundedCornerShape(20.dp)
  )
}

@Composable
private fun RaporDetailBottomNav(
  selectedSection: RaporSection,
  onSelectSection: (RaporSection) -> Unit,
  modifier: Modifier = Modifier
) {
  var animatedSelectedSectionKey by rememberSaveable {
    mutableStateOf(selectedSection.name)
  }
  var pendingNavigationJob by remember { mutableStateOf<Job?>(null) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(selectedSection) {
    animatedSelectedSectionKey = selectedSection.name
  }

  Box(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center
  ) {
    Row(
      modifier = Modifier
        .width(356.dp)
        .shadow(18.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x180F172A), spotColor = Color(0x180F172A))
        .clip(RoundedCornerShape(32.dp))
        .background(CardBackground.copy(alpha = 0.96f))
        .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(32.dp))
        .padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      RaporSection.entries.forEach { section ->
        val selected = section.name == animatedSelectedSectionKey
        Column(
          modifier = Modifier
            .weight(1f)
            .height(58.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) PrimaryBlue.copy(alpha = 0.12f) else Color.Transparent)
            .clickable {
              if (animatedSelectedSectionKey != section.name) {
                animatedSelectedSectionKey = section.name
              }
              pendingNavigationJob?.cancel()
              pendingNavigationJob = scope.launch {
                delay(280)
                onSelectSection(section)
              }
            }
            .padding(horizontal = 4.dp, vertical = 7.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = section.icon,
            contentDescription = t(section.label),
            tint = if (selected) PrimaryBlue else SubtleInk.copy(alpha = 0.82f),
            modifier = Modifier.size(21.dp)
          )
          Text(
            text = t(section.label),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) PrimaryBlueDark else SubtleInk.copy(alpha = 0.82f),
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

@Composable
private fun RaporStudentCard(
  santri: WaliSantriProfile,
  report: RaporStudentReport,
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
    RaporAvatar(name = santri.name, size = 50)
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
        text = listOf(
          santri.className.ifBlank { "-" },
          if (santri.nisn.isBlank()) "NISN belum tersedia" else "NISN ${santri.nisn}"
        ).joinToString(" - "),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    RaporScoreBadge(report.averageKnowledgeText.ifBlank { "-" })
  }
}

@Composable
private fun RaporStudentHeader(
  santri: WaliSantriProfile,
  semesterLabel: String,
  academicYearLabel: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(28.dp))
      .background(PrimaryBlue.copy(alpha = 0.10f))
      .border(1.dp, PrimaryBlue.copy(alpha = 0.18f), RoundedCornerShape(28.dp))
      .padding(18.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    RaporAvatar(name = santri.name, size = 84)
    Text(
      text = santri.name.ifBlank { t("Nama Santri") },
      style = MaterialTheme.typography.titleLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = listOf(
        santri.className.ifBlank { "-" },
        semesterLabel.ifBlank { "-" },
        academicYearLabel.ifBlank { "-" }
      ).joinToString(" - "),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
  }
}

@Composable
private fun RaporIdentityPanel(report: RaporStudentReport) {
  RaporSectionCard("Informasi Rapor") {
    RaporInfoRow("Nama sekolah", report.schoolName.ifBlank { "-" })
    RaporInfoRow("Alamat", report.schoolAddress.ifBlank { "-" })
    RaporInfoRow("Nama santri", report.studentName.ifBlank { "-" })
    RaporInfoRow("Nomor induk", report.studentNisn.ifBlank { "-" })
    RaporInfoRow("Kelas", report.className.ifBlank { "-" })
    RaporInfoRow("Semester", report.semesterLabel.ifBlank { "-" })
    RaporInfoRow("Tahun pelajaran", report.academicYearLabel.ifBlank { "-" })
  }
}

@Composable
private fun RaporAttitudePanel(report: RaporStudentReport) {
  RaporSectionCard("Sikap") {
    RaporValueBlock(
      title = "Sikap spiritual",
      value = report.spiritualPredicate.ifBlank { "-" },
      description = report.spiritualDescription.ifBlank { "-" }
    )
    RaporValueBlock(
      title = "Sikap sosial",
      value = report.socialPredicate.ifBlank { "-" },
      description = report.socialDescription.ifBlank { "-" }
    )
  }
}

@Composable
private fun RaporAffectivePanel(report: RaporStudentReport) {
  RaporSectionCard("Afektif") {
    report.affectiveRows.forEach { row ->
      RaporThreeColumnRow(
        first = row.aspect,
        second = row.percentText.ifBlank { "-" },
        third = row.note.ifBlank { "-" }
      )
    }
    RaporInfoRow("Musyrif", report.musyrifName.ifBlank { "-" })
  }
}

@Composable
private fun RaporQuranPanel(report: RaporStudentReport) {
  RaporSectionCard("Capaian Alquran") {
    RaporInfoRow("Nilai ikhtibar semester", report.quranIkhtibarScore.ifBlank { "-" })
    RaporInfoRow("Target hafalan semester", report.quranTargetSemester.ifBlank { "-" })
    RaporInfoRow("Total hafalan keseluruhan", report.quranTotalHafalan.ifBlank { "-" })
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "Laporan mutabaah",
      style = MaterialTheme.typography.labelLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    report.quranRows.forEach { row ->
      RaporQuranRowCard(row)
    }
    RaporInfoRow("Rata-rata", report.quranAverageScore.ifBlank { "-" })
    RaporInfoRow("Predikat", report.quranAveragePredicate.ifBlank { "-" })
    RaporInfoRow("Muhaffiz", report.muhaffizName.ifBlank { "-" })
  }
}

@Composable
private fun RaporAcademicSummaryPanel(report: RaporStudentReport) {
  RaporSectionCard("Total Pengetahuan dan Keterampilan") {
    RaporInfoRow("Total pengetahuan", report.totalKnowledgeText.ifBlank { "-" })
    RaporInfoRow("Predikat pengetahuan", report.totalKnowledgePredicate.ifBlank { "-" })
    RaporInfoRow("Total keterampilan", report.totalSkillText.ifBlank { "-" })
    RaporInfoRow("Predikat keterampilan", report.totalSkillPredicate.ifBlank { "-" })
  }
}

@Composable
private fun RaporSubjectCard(subject: RaporSubjectReport) {
  RaporSectionCard(subject.subjectName.ifBlank { "-" }) {
    RaporInfoRow("KKM", subject.kkmText.ifBlank { "-" })
    RaporScoreSection(
      title = "Nilai Pengetahuan",
      scoreText = subject.knowledgeScoreText,
      predicate = subject.knowledgePredicate,
      description = subject.knowledgeDescription
    )
    RaporScoreSection(
      title = "Nilai Keterampilan",
      scoreText = subject.skillScoreText,
      predicate = subject.skillPredicate,
      description = subject.skillDescription
    )
  }
}

@Composable
private fun RaporExtracurricularPanel(report: RaporStudentReport) {
  RaporSectionCard("Ekstrakurikuler") {
    if (report.extracurricularRows.isEmpty()) {
      Text("-", style = MaterialTheme.typography.bodySmall, color = SubtleInk)
    } else {
      report.extracurricularRows.forEach { row ->
        RaporValueBlock(row.label, row.value.ifBlank { "-" }, row.note.ifBlank { "-" })
      }
    }
  }
}

@Composable
private fun RaporAchievementPanel(report: RaporStudentReport) {
  RaporSectionCard("Prestasi") {
    if (report.achievementRows.isEmpty()) {
      Text("-", style = MaterialTheme.typography.bodySmall, color = SubtleInk)
    } else {
      report.achievementRows.forEach { row ->
        RaporValueBlock(row.label, "", row.note.ifBlank { "-" })
      }
    }
  }
}

@Composable
private fun RaporAttendancePanel(report: RaporStudentReport) {
  RaporSectionCard("Kehadiran") {
    report.attendanceRows.forEach { row ->
      RaporAttendanceRowCard(row)
    }
  }
}

@Composable
private fun RaporSignaturePanel(report: RaporStudentReport) {
  RaporSectionCard("Tanda Tangan") {
    RaporInfoRow("Wali kelas", report.waliKelasName.ifBlank { "-" })
    RaporInfoRow("Kepala sekolah", report.kepalaSekolahName.ifBlank { "-" })
  }
}

@Composable
private fun RaporSectionCard(
  title: String,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(24.dp))
      .background(CardBackground.copy(alpha = 0.96f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.weight(1f)
      )
      Icon(Icons.Outlined.School, contentDescription = null, tint = PrimaryBlue)
    }
    content()
  }
}

@Composable
private fun RaporScoreSection(
  title: String,
  scoreText: String,
  predicate: String,
  description: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(SoftPanel.copy(alpha = 0.72f))
      .border(1.dp, CardBorder.copy(alpha = 0.74f), RoundedCornerShape(18.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = t(title),
          style = MaterialTheme.typography.labelLarge,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = "Predikat ${predicate.ifBlank { "-" }}",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
      RaporScoreBadge(scoreText.ifBlank { "-" })
    }
    Text(
      text = description.ifBlank { "-" },
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
  }
}

@Composable
private fun RaporValueBlock(
  title: String,
  value: String,
  description: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(SoftPanel.copy(alpha = 0.72f))
      .border(1.dp, CardBorder.copy(alpha = 0.74f), RoundedCornerShape(18.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(5.dp)
  ) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
      Text(title, style = MaterialTheme.typography.labelLarge, color = PrimaryBlueDark, fontWeight = FontWeight.ExtraBold)
      if (value.isNotBlank()) RaporScoreBadge(value)
    }
    Text(description.ifBlank { "-" }, style = MaterialTheme.typography.bodySmall, color = SubtleInk)
  }
}

@Composable
private fun RaporThreeColumnRow(
  first: String,
  second: String,
  third: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(SoftPanel.copy(alpha = 0.56f))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(first, style = MaterialTheme.typography.bodySmall, color = PrimaryBlueDark, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.34f))
    Text(second, style = MaterialTheme.typography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.22f))
    Text(third, style = MaterialTheme.typography.bodySmall, color = SubtleInk, modifier = Modifier.weight(0.44f))
  }
}

@Composable
private fun RaporQuranRowCard(row: RaporQuranRowReport) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(SoftPanel.copy(alpha = 0.56f))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(row.juzLabel, style = MaterialTheme.typography.labelLarge, color = PrimaryBlueDark, fontWeight = FontWeight.ExtraBold)
    RaporInfoRow("Tajwid", row.tajwidScore.ifBlank { "-" })
    RaporInfoRow("Kelancaran", row.fluencyScore.ifBlank { "-" })
    RaporInfoRow("Nilai", row.scoreText.ifBlank { "-" })
    RaporInfoRow("Predikat", row.predicate.ifBlank { "-" })
  }
}

@Composable
private fun RaporAttendanceRowCard(row: RaporAttendanceRowReport) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(SoftPanel.copy(alpha = 0.56f))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(row.label, style = MaterialTheme.typography.labelLarge, color = PrimaryBlueDark, fontWeight = FontWeight.ExtraBold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      RaporMiniStat("Izin", row.izinText)
      RaporMiniStat("Sakit", row.sakitText)
      RaporMiniStat("Telat", row.telatText)
      RaporMiniStat("Alpa", row.alpaText)
    }
  }
}

@Composable
private fun RowScope.RaporMiniStat(label: String, value: String) {
  Column(
    modifier = Modifier
      .weight(1f)
      .clip(RoundedCornerShape(12.dp))
      .background(CardBackground.copy(alpha = 0.72f))
      .padding(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Text(value.ifBlank { "0" }, style = MaterialTheme.typography.labelLarge, color = PrimaryBlue, fontWeight = FontWeight.ExtraBold)
    Text(label, style = MaterialTheme.typography.labelSmall, color = SubtleInk)
  }
}

@Composable
private fun RaporInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = t(label),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      modifier = Modifier.weight(0.42f)
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.weight(0.58f),
      maxLines = 3,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun RaporScoreBadge(value: String) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(PrimaryBlue.copy(alpha = 0.12f))
      .border(1.dp, PrimaryBlue.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
      .padding(horizontal = 12.dp, vertical = 7.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = value.ifBlank { "-" },
      style = MaterialTheme.typography.labelLarge,
      color = PrimaryBlue,
      fontWeight = FontWeight.ExtraBold
    )
  }
}

@Composable
private fun RaporAvatar(name: String, size: Int) {
  Box(
    modifier = Modifier
      .size(size.dp)
      .background(PrimaryBlue.copy(alpha = 0.14f), CircleShape)
      .border(1.dp, PrimaryBlue.copy(alpha = 0.28f), CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = name.initials(),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlue,
      fontWeight = FontWeight.ExtraBold
    )
  }
}

@Composable
private fun RaporSkeletonCard(index: Int) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .background(CardBackground.copy(alpha = 0.70f))
      .border(1.dp, CardBorder.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
      .padding(14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    RaporAvatar(name = "Santri $index", size = 50)
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(0.74f)
          .height(14.dp)
          .clip(RoundedCornerShape(999.dp))
          .background(SoftPanel)
      )
      Box(
        modifier = Modifier
          .fillMaxWidth(0.46f)
          .height(12.dp)
          .clip(RoundedCornerShape(999.dp))
          .background(SoftPanel.copy(alpha = 0.74f))
      )
    }
  }
}

@Composable
private fun RaporImportDialog(
  students: List<WaliSantriProfile>,
  classNames: List<String>,
  onDismiss: () -> Unit,
  onApply: (RaporSection, List<RaporImportPreviewRow>) -> Unit
) {
  val scope = rememberCoroutineScope()
  val importableSections = remember { RaporSection.entries.filter { it.canImport } }
  var selectedSectionName by rememberSaveable { mutableStateOf(RaporSection.Nilai.name) }
  val selectedSection = remember(selectedSectionName) {
    runCatching { RaporSection.valueOf(selectedSectionName) }.getOrDefault(RaporSection.Nilai)
  }
  var sectionDropdownExpanded by remember { mutableStateOf(false) }
  var url by rememberSaveable { mutableStateOf("") }
  var preview by remember { mutableStateOf<RaporImportPreview?>(null) }
  var error by rememberSaveable { mutableStateOf<String?>(null) }
  var isLoading by rememberSaveable { mutableStateOf(false) }
  var importProgress by rememberSaveable { mutableStateOf(0) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Import Data Rapor") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBackground.copy(alpha = 0.94f))
            .border(1.dp, CardBorder.copy(alpha = 0.82f), RoundedCornerShape(18.dp))
            .clickable { sectionDropdownExpanded = true }
            .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text("Jenis import", style = MaterialTheme.typography.labelMedium, color = SubtleInk)
              Text(
                selectedSection.label,
                style = MaterialTheme.typography.titleSmall,
                color = PrimaryBlueDark,
                fontWeight = FontWeight.ExtraBold
              )
            }
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = PrimaryBlueDark)
          }
          DropdownMenu(
            expanded = sectionDropdownExpanded,
            onDismissRequest = { sectionDropdownExpanded = false },
            modifier = Modifier.background(CardBackground)
          ) {
            importableSections.forEach { section ->
              DropdownMenuItem(
                text = { Text(section.label) },
                leadingIcon = { Icon(section.icon, contentDescription = null, tint = PrimaryBlue) },
                onClick = {
                  selectedSectionName = section.name
                  sectionDropdownExpanded = false
                  preview = null
                  error = null
                  importProgress = 0
                }
              )
            }
          }
        }
        OutlinedTextField(
          value = url,
          onValueChange = {
            url = it
            preview = null
            error = null
            importProgress = 0
          },
          label = { Text("Link spreadsheet") },
          singleLine = false,
          minLines = 2,
          modifier = Modifier.fillMaxWidth()
        )
        Button(
          onClick = {
            scope.launch {
              isLoading = true
              importProgress = 0
              error = null
              preview = null
              runCatching {
                fetchRaporImportPreview(
                  rawUrl = url,
                  classNames = classNames,
                  students = students,
                  section = selectedSection,
                  onProgress = { importProgress = it }
                )
              }.onSuccess {
                importProgress = 100
                preview = it
              }.onFailure {
                error = it.message ?: "Gagal membaca spreadsheet."
              }
              isLoading = false
            }
          },
          enabled = url.isNotBlank() && !isLoading,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(if (isLoading) "Membaca... ${importProgress.coerceIn(0, 100)}%" else "Ambil data")
        }
        error?.let {
          Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        preview?.let { currentPreview ->
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "${currentPreview.matchedRows.size} cocok, ${currentPreview.unmatchedCount} belum cocok",
              style = MaterialTheme.typography.labelLarge,
              color = PrimaryBlueDark,
              fontWeight = FontWeight.ExtraBold
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              items(currentPreview.matchedRows.take(10), key = { it.student.id }) { row ->
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SoftPanel.copy(alpha = 0.58f))
                    .padding(10.dp)
                ) {
                  Text(row.student.name, style = MaterialTheme.typography.labelLarge, color = PrimaryBlueDark, fontWeight = FontWeight.ExtraBold)
                  Text(
                    text = row.manual.previewSummary(selectedSection),
                    style = MaterialTheme.typography.bodySmall,
                    color = SubtleInk
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        enabled = preview?.matchedRows?.isNotEmpty() == true,
        onClick = {
          onApply(selectedSection, preview?.matchedRows.orEmpty())
        }
      ) {
        Text("Terapkan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Batal")
      }
    }
  )
}

private enum class RaporSection(
  val label: String,
  val icon: ImageVector,
  val canImport: Boolean
) {
  Info("Informasi", Icons.Outlined.School, false),
  Akhlak("Akhlak", Icons.Outlined.Grade, true),
  Alquran("Alquran", Icons.Outlined.School, true),
  Nilai("Nilai", Icons.Outlined.Grade, true),
  Lainnya("Lainnya", Icons.Outlined.Menu, true)
}

private data class RaporStudentReport(
  val studentId: String = "",
  val studentName: String = "",
  val studentNisn: String = "",
  val className: String = "",
  val semesterLabel: String = "",
  val academicYearLabel: String = "",
  val schoolName: String = DefaultSchoolName,
  val schoolAddress: String = DefaultSchoolAddress,
  val waliKelasName: String = "",
  val kepalaSekolahName: String = "",
  val spiritualPredicate: String = "",
  val spiritualDescription: String = "",
  val socialPredicate: String = "",
  val socialDescription: String = "",
  val affectiveRows: List<RaporAffectiveRowReport> = defaultAffectiveRows(),
  val musyrifName: String = "",
  val quranIkhtibarScore: String = "",
  val quranTargetSemester: String = "",
  val quranTotalHafalan: String = "",
  val quranRows: List<RaporQuranRowReport> = defaultQuranRows(),
  val quranAverageScore: String = "",
  val quranAveragePredicate: String = "",
  val muhaffizName: String = "",
  val subjects: List<RaporSubjectReport> = emptyList(),
  val totalKnowledgeText: String = "-",
  val totalKnowledgePredicate: String = "-",
  val totalSkillText: String = "-",
  val totalSkillPredicate: String = "-",
  val averageKnowledgeText: String = "-",
  val extracurricularRows: List<RaporSimpleRowReport> = emptyList(),
  val achievementRows: List<RaporSimpleRowReport> = emptyList(),
  val attendanceRows: List<RaporAttendanceRowReport> = defaultAttendanceRows()
)

private data class RaporSubjectReport(
  val subjectKey: String,
  val subjectName: String,
  val kkmText: String,
  val knowledgeScoreText: String,
  val knowledgePredicate: String,
  val knowledgeDescription: String,
  val skillScoreText: String,
  val skillPredicate: String,
  val skillDescription: String
)

private data class RaporAffectiveRowReport(
  val aspect: String,
  val percentText: String = "",
  val note: String = ""
)

private data class RaporQuranRowReport(
  val juzLabel: String,
  val tajwidScore: String = "",
  val fluencyScore: String = "",
  val scoreText: String = "",
  val predicate: String = ""
)

private data class RaporSimpleRowReport(
  val label: String,
  val value: String = "",
  val note: String = ""
)

private data class RaporAttendanceRowReport(
  val label: String,
  val izinText: String = "0",
  val sakitText: String = "0",
  val telatText: String = "0",
  val alpaText: String = "0"
)

private data class RaporManualData(
  val schoolName: String = "",
  val schoolAddress: String = "",
  val waliKelasName: String = "",
  val kepalaSekolahName: String = "",
  val spiritualPredicate: String = "",
  val spiritualDescription: String = "",
  val socialPredicate: String = "",
  val socialDescription: String = "",
  val affectiveRows: List<RaporAffectiveRowReport> = defaultAffectiveRows(),
  val musyrifName: String = "",
  val quranIkhtibarScore: String = "",
  val quranTargetSemester: String = "",
  val quranTotalHafalan: String = "",
  val quranRows: List<RaporQuranRowReport> = emptyList(),
  val quranAverageScore: String = "",
  val quranAveragePredicate: String = "",
  val muhaffizName: String = "",
  val subjectOverrides: Map<String, RaporManualSubjectData> = emptyMap(),
  val extracurricularRows: List<RaporSimpleRowReport> = emptyList(),
  val achievementRows: List<RaporSimpleRowReport> = emptyList(),
  val attendanceRows: List<RaporAttendanceRowReport> = defaultAttendanceRows()
)

private data class RaporManualSubjectData(
  val knowledgeScoreText: String = "",
  val knowledgePredicate: String = "",
  val knowledgeDescription: String = "",
  val skillScoreText: String = "",
  val skillPredicate: String = "",
  val skillDescription: String = ""
)

private data class RaporDescriptionTemplate(
  val knowledgeDescriptions: Map<String, String> = emptyRaporDescriptionMap(),
  val skillDescriptions: Map<String, String> = emptyRaporDescriptionMap()
) {
  fun knowledgeDescription(predicate: String): String {
    return knowledgeDescriptions[predicate.uppercase(Locale.ROOT)].orEmpty()
  }

  fun skillDescription(predicate: String): String {
    return skillDescriptions[predicate.uppercase(Locale.ROOT)].orEmpty()
  }

  fun hasAnyDescription(): Boolean {
    return knowledgeDescriptions.values.any { it.isNotBlank() } ||
      skillDescriptions.values.any { it.isNotBlank() }
  }
}

private data class RaporImportPreview(
  val matchedRows: List<RaporImportPreviewRow>,
  val unmatchedCount: Int
)

private data class RaporImportPreviewRow(
  val student: WaliSantriProfile,
  val manual: RaporManualData
)

private data class RaporHeaderSpec(
  val headers: List<String>,
  val dataStartIndex: Int
)

private const val RaporManualReportsPrefs = "rapor_manual_reports"
private const val RaporManualReportsKey = "manual_reports_json"

private fun loadRaporManualReports(context: Context): Map<String, RaporManualData> {
  val raw = context
    .getSharedPreferences(RaporManualReportsPrefs, Context.MODE_PRIVATE)
    .getString(RaporManualReportsKey, null)
    .orEmpty()
  if (raw.isBlank()) return emptyMap()
  return runCatching {
    val root = JSONObject(raw)
    val entries = root.optJSONObject("entries") ?: root
    val result = linkedMapOf<String, RaporManualData>()
    val keys = entries.keys()
    while (keys.hasNext()) {
      val key = keys.next()
      val data = entries.optJSONObject(key)?.toRaporManualData() ?: continue
      if (data.hasAnyManualData()) result[key] = data
    }
    result
  }.getOrDefault(emptyMap())
}

private fun saveRaporManualReports(
  context: Context,
  reports: Map<String, RaporManualData>
) {
  val entries = JSONObject()
  reports.filterValues { it.hasAnyManualData() }.forEach { (key, data) ->
    entries.put(key, data.toJson())
  }
  val root = JSONObject()
    .put("version", 1)
    .put("entries", entries)
  context
    .getSharedPreferences(RaporManualReportsPrefs, Context.MODE_PRIVATE)
    .edit()
    .putString(RaporManualReportsKey, root.toString())
    .apply()
}

private fun RaporManualData.toJson(): JSONObject {
  return JSONObject()
    .put("schoolName", schoolName)
    .put("schoolAddress", schoolAddress)
    .put("waliKelasName", waliKelasName)
    .put("kepalaSekolahName", kepalaSekolahName)
    .put("spiritualPredicate", spiritualPredicate)
    .put("spiritualDescription", spiritualDescription)
    .put("socialPredicate", socialPredicate)
    .put("socialDescription", socialDescription)
    .put("affectiveRows", affectiveRows.toAffectiveJson())
    .put("musyrifName", musyrifName)
    .put("quranIkhtibarScore", quranIkhtibarScore)
    .put("quranTargetSemester", quranTargetSemester)
    .put("quranTotalHafalan", quranTotalHafalan)
    .put("quranRows", quranRows.toQuranJson())
    .put("quranAverageScore", quranAverageScore)
    .put("quranAveragePredicate", quranAveragePredicate)
    .put("muhaffizName", muhaffizName)
    .put("subjectOverrides", subjectOverrides.toSubjectOverridesJson())
    .put("extracurricularRows", extracurricularRows.toSimpleRowsJson())
    .put("achievementRows", achievementRows.toSimpleRowsJson())
    .put("attendanceRows", attendanceRows.toAttendanceRowsJson())
}

private fun JSONObject.toRaporManualData(): RaporManualData {
  return RaporManualData(
    schoolName = optCleanString("schoolName"),
    schoolAddress = optCleanString("schoolAddress"),
    waliKelasName = optCleanString("waliKelasName"),
    kepalaSekolahName = optCleanString("kepalaSekolahName"),
    spiritualPredicate = optCleanString("spiritualPredicate"),
    spiritualDescription = optCleanString("spiritualDescription"),
    socialPredicate = optCleanString("socialPredicate"),
    socialDescription = optCleanString("socialDescription"),
    affectiveRows = optJSONArray("affectiveRows").toAffectiveRows().ifEmpty { defaultAffectiveRows() },
    musyrifName = optCleanString("musyrifName"),
    quranIkhtibarScore = optCleanString("quranIkhtibarScore"),
    quranTargetSemester = optCleanString("quranTargetSemester"),
    quranTotalHafalan = optCleanString("quranTotalHafalan"),
    quranRows = optJSONArray("quranRows").toQuranRows(),
    quranAverageScore = optCleanString("quranAverageScore"),
    quranAveragePredicate = optCleanString("quranAveragePredicate"),
    muhaffizName = optCleanString("muhaffizName"),
    subjectOverrides = optJSONObject("subjectOverrides").toSubjectOverrides(),
    extracurricularRows = optJSONArray("extracurricularRows").toSimpleRows(),
    achievementRows = optJSONArray("achievementRows").toSimpleRows(),
    attendanceRows = optJSONArray("attendanceRows").toAttendanceRows().ifEmpty { defaultAttendanceRows() }
  )
}

private fun List<RaporAffectiveRowReport>.toAffectiveJson(): JSONArray {
  val array = JSONArray()
  forEach { row ->
    array.put(
      JSONObject()
        .put("aspect", row.aspect)
        .put("percentText", row.percentText)
        .put("note", row.note)
    )
  }
  return array
}

private fun JSONArray?.toAffectiveRows(): List<RaporAffectiveRowReport> {
  return mapObjects { item ->
    RaporAffectiveRowReport(
      aspect = item.optCleanString("aspect"),
      percentText = item.optCleanString("percentText"),
      note = item.optCleanString("note")
    )
  }
}

private fun List<RaporQuranRowReport>.toQuranJson(): JSONArray {
  val array = JSONArray()
  forEach { row ->
    array.put(
      JSONObject()
        .put("juzLabel", row.juzLabel)
        .put("tajwidScore", row.tajwidScore)
        .put("fluencyScore", row.fluencyScore)
        .put("scoreText", row.scoreText)
        .put("predicate", row.predicate)
    )
  }
  return array
}

private fun JSONArray?.toQuranRows(): List<RaporQuranRowReport> {
  return mapObjects { item ->
    RaporQuranRowReport(
      juzLabel = item.optCleanString("juzLabel"),
      tajwidScore = item.optCleanString("tajwidScore"),
      fluencyScore = item.optCleanString("fluencyScore"),
      scoreText = item.optCleanString("scoreText"),
      predicate = item.optCleanString("predicate")
    )
  }
}

private fun Map<String, RaporManualSubjectData>.toSubjectOverridesJson(): JSONObject {
  val root = JSONObject()
  forEach { (key, subject) ->
    root.put(
      key,
      JSONObject()
        .put("knowledgeScoreText", subject.knowledgeScoreText)
        .put("knowledgePredicate", subject.knowledgePredicate)
        .put("knowledgeDescription", subject.knowledgeDescription)
        .put("skillScoreText", subject.skillScoreText)
        .put("skillPredicate", subject.skillPredicate)
        .put("skillDescription", subject.skillDescription)
    )
  }
  return root
}

private fun JSONObject?.toSubjectOverrides(): Map<String, RaporManualSubjectData> {
  val root = this ?: return emptyMap()
  val result = linkedMapOf<String, RaporManualSubjectData>()
  val keys = root.keys()
  while (keys.hasNext()) {
    val key = keys.next()
    val item = root.optJSONObject(key) ?: continue
    result[key] = RaporManualSubjectData(
      knowledgeScoreText = item.optCleanString("knowledgeScoreText"),
      knowledgePredicate = item.optCleanString("knowledgePredicate"),
      knowledgeDescription = item.optCleanString("knowledgeDescription"),
      skillScoreText = item.optCleanString("skillScoreText"),
      skillPredicate = item.optCleanString("skillPredicate"),
      skillDescription = item.optCleanString("skillDescription")
    )
  }
  return result
}

private fun List<RaporSimpleRowReport>.toSimpleRowsJson(): JSONArray {
  val array = JSONArray()
  forEach { row ->
    array.put(
      JSONObject()
        .put("label", row.label)
        .put("value", row.value)
        .put("note", row.note)
    )
  }
  return array
}

private fun JSONArray?.toSimpleRows(): List<RaporSimpleRowReport> {
  return mapObjects { item ->
    RaporSimpleRowReport(
      label = item.optCleanString("label"),
      value = item.optCleanString("value"),
      note = item.optCleanString("note")
    )
  }.filterNot(::simpleRowLooksLikeHeader)
}

private fun List<RaporAttendanceRowReport>.toAttendanceRowsJson(): JSONArray {
  val array = JSONArray()
  forEach { row ->
    array.put(
      JSONObject()
        .put("label", row.label)
        .put("izinText", row.izinText)
        .put("sakitText", row.sakitText)
        .put("telatText", row.telatText)
        .put("alpaText", row.alpaText)
    )
  }
  return array
}

private fun JSONArray?.toAttendanceRows(): List<RaporAttendanceRowReport> {
  return mapObjects { item ->
    RaporAttendanceRowReport(
      label = item.optCleanString("label"),
      izinText = item.optCleanString("izinText").ifBlank { "0" },
      sakitText = item.optCleanString("sakitText").ifBlank { "0" },
      telatText = item.optCleanString("telatText").ifBlank { "0" },
      alpaText = item.optCleanString("alpaText").ifBlank { "0" }
    )
  }
}

private inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
  if (this == null) return emptyList()
  val result = mutableListOf<T>()
  for (index in 0 until length()) {
    optJSONObject(index)?.let { result += transform(it) }
  }
  return result
}

private fun JSONObject.optCleanString(name: String): String {
  if (!has(name) || isNull(name)) return ""
  return optString(name).trim()
}

private fun RaporManualData.mergeSection(
  section: RaporSection,
  incoming: RaporManualData
): RaporManualData {
  return when (section) {
    RaporSection.Info -> this
    RaporSection.Akhlak -> copy(
      spiritualPredicate = incoming.spiritualPredicate,
      spiritualDescription = incoming.spiritualDescription,
      socialPredicate = incoming.socialPredicate,
      socialDescription = incoming.socialDescription,
      affectiveRows = incoming.affectiveRows,
      musyrifName = incoming.musyrifName,
      achievementRows = mergeSimpleRowsByLabel(achievementRows, incoming.achievementRows),
      attendanceRows = mergeManualAttendanceRows(attendanceRows, incoming.attendanceRows)
    )
    RaporSection.Alquran -> copy(
      quranIkhtibarScore = incoming.quranIkhtibarScore,
      quranTargetSemester = incoming.quranTargetSemester,
      quranTotalHafalan = incoming.quranTotalHafalan,
      quranRows = incoming.quranRows,
      quranAverageScore = incoming.quranAverageScore,
      quranAveragePredicate = incoming.quranAveragePredicate,
      muhaffizName = incoming.muhaffizName,
      attendanceRows = mergeManualAttendanceRows(attendanceRows, incoming.attendanceRows)
    )
    RaporSection.Nilai -> copy(
      subjectOverrides = incoming.subjectOverrides
    )
    RaporSection.Lainnya -> copy(
      kepalaSekolahName = incoming.kepalaSekolahName.ifBlank { kepalaSekolahName },
      extracurricularRows = mergeSimpleRowsByLabel(extracurricularRows, incoming.extracurricularRows),
      achievementRows = mergeSimpleRowsByLabel(achievementRows, incoming.achievementRows),
      attendanceRows = mergeManualAttendanceRows(attendanceRows, incoming.attendanceRows)
    )
  }
}

private fun mergeSimpleRowsByLabel(
  current: List<RaporSimpleRowReport>,
  incoming: List<RaporSimpleRowReport>
): List<RaporSimpleRowReport> {
  val cleanCurrent = current.filterNot(::simpleRowLooksLikeHeader)
  val filledIncoming = incoming.filter { row ->
    !simpleRowLooksLikeHeader(row) &&
      (row.label.isNotBlank() || row.value.isNotBlank() || row.note.isNotBlank())
  }
  if (filledIncoming.isEmpty()) return cleanCurrent
  val incomingLabels = filledIncoming.map { normalizeSpreadsheetKey(it.label) }.toSet()
  return (cleanCurrent.filterNot { normalizeSpreadsheetKey(it.label) in incomingLabels } + filledIncoming)
    .distinctBy { normalizeSpreadsheetKey("${it.label}|${it.value}|${it.note}") }
}

private fun mergeManualAttendanceRows(
  current: List<RaporAttendanceRowReport>,
  incoming: List<RaporAttendanceRowReport>
): List<RaporAttendanceRowReport> {
  val incomingByLabel = incoming
    .filter { it.hasManualAttendanceValue() }
    .associateBy { normalizeSpreadsheetKey(it.label) }
  if (incomingByLabel.isEmpty()) return current
  return defaultAttendanceRows().map { defaultRow ->
    incomingByLabel[normalizeSpreadsheetKey(defaultRow.label)]
      ?: current.firstOrNull { normalizeSpreadsheetKey(it.label) == normalizeSpreadsheetKey(defaultRow.label) }
      ?: defaultRow
  }
}

private fun RaporManualData.clearSection(section: RaporSection): RaporManualData {
  return when (section) {
    RaporSection.Info -> this
    RaporSection.Akhlak -> copy(
      spiritualPredicate = "",
      spiritualDescription = "",
      socialPredicate = "",
      socialDescription = "",
      affectiveRows = defaultAffectiveRows(),
      musyrifName = ""
    )
    RaporSection.Alquran -> copy(
      quranIkhtibarScore = "",
      quranTargetSemester = "",
      quranTotalHafalan = "",
      quranRows = emptyList(),
      quranAverageScore = "",
      quranAveragePredicate = "",
      muhaffizName = ""
    )
    RaporSection.Nilai -> copy(subjectOverrides = emptyMap())
    RaporSection.Lainnya -> copy(
      kepalaSekolahName = "",
      extracurricularRows = emptyList(),
      achievementRows = emptyList(),
      attendanceRows = defaultAttendanceRows()
    )
  }
}

private fun RaporManualData?.hasSectionManualData(section: RaporSection): Boolean {
  val data = this ?: return false
  return when (section) {
    RaporSection.Info -> false
    RaporSection.Akhlak -> listOf(
      data.spiritualPredicate,
      data.spiritualDescription,
      data.socialPredicate,
      data.socialDescription,
      data.musyrifName
    ).any { it.isNotBlank() } || data.affectiveRows.any { it.percentText.isNotBlank() || it.note.isNotBlank() }
    RaporSection.Alquran -> listOf(
      data.quranIkhtibarScore,
      data.quranTargetSemester,
      data.quranTotalHafalan,
      data.quranAverageScore,
      data.quranAveragePredicate,
      data.muhaffizName
    ).any { it.isNotBlank() } || data.quranRows.any { it.hasValue() }
    RaporSection.Nilai -> data.subjectOverrides.isNotEmpty()
    RaporSection.Lainnya -> data.kepalaSekolahName.isNotBlank() ||
      data.extracurricularRows.isNotEmpty() ||
      data.achievementRows.isNotEmpty() ||
      data.attendanceRows.any { it.izinText != "0" || it.sakitText != "0" || it.telatText != "0" || it.alpaText != "0" }
  }
}

private fun RaporManualData.hasAnyManualData(): Boolean {
  return RaporSection.entries.any { section -> this.hasSectionManualData(section) }
}

private fun RaporManualData.previewSummary(section: RaporSection): String {
  return when (section) {
    RaporSection.Info -> "Informasi memakai data sistem"
    RaporSection.Akhlak -> {
      val filled = affectiveRows.count { it.percentText.isNotBlank() || it.note.isNotBlank() }
      "Sikap ${listOf(spiritualPredicate, socialPredicate).count { it.isNotBlank() }} item, afektif $filled aspek"
    }
    RaporSection.Alquran -> {
      val filled = quranRows.count { it.hasValue() }
      "$filled juz terisi, muhaffiz ${muhaffizName.ifBlank { "-" }}"
    }
    RaporSection.Nilai -> "${subjectOverrides.size} mapel"
    RaporSection.Lainnya -> "${extracurricularRows.size} ekskul, ${achievementRows.size} prestasi"
  }
}

private fun buildRaporStudentReport(
  student: WaliSantriProfile,
  semester: UtsSemesterInfo,
  utsSnapshot: UtsReportSnapshot,
  scoreSnapshots: List<MapelScoreSnapshot>,
  attendanceSnapshots: List<MapelAttendanceSnapshot>,
  manual: RaporManualData?,
  descriptionTemplates: Map<String, RaporDescriptionTemplate>,
  fallbackWaliName: String
): RaporStudentReport {
  val manualSubjects = manual?.subjectOverrides.orEmpty()
  val classSubjects = utsSnapshot.classSubjects
    .filter { subject -> subject.classId == student.classId && subject.semesterId == semester.id }
  val kkmBySubject = classSubjects
    .associate { subject -> normalizeSpreadsheetKey(subject.name) to subject.kkmText.ifBlank { "17" } }
  val subjectGroups = classSubjects.groupBy { subject -> subject.raporSubjectDedupKey() }
  val classSubjectReports = subjectGroups.values
    .map { subjectsForMapel ->
      val subject = subjectsForMapel.preferredRaporSubject()
      val detailScore = scoreSnapshots.findRaporScoreStudentForSubjects(student, subjectsForMapel)
      val academicScore = utsSnapshot.scoreRows.findRaporAcademicScore(
        studentId = student.id,
        semesterId = semester.id,
        subjects = subjectsForMapel
      )
      val descriptionTemplate = subjectsForMapel.firstNotNullOfOrNull { item ->
        descriptionTemplates[item.distribusiId]?.takeIf { it.hasAnyDescription() }
      }
      subject.toRaporSubjectReport(
        detailScore = detailScore,
        score = academicScore,
        manualSubjects = manualSubjects,
        descriptionTemplate = descriptionTemplate
      )
    }
    .sortedWith(compareBy<RaporSubjectReport> { raporSubjectOrderIndex(it.subjectName) }.thenBy { it.subjectName.lowercase() }.thenBy { it.subjectKey })
  val classSubjectKeys = subjectGroups.keys
  val extraScoreSubjects = scoreSnapshots
    .filter { snapshot -> snapshot.matchesRaporStudentClass(student) && normalizeSpreadsheetKey(snapshot.subjectTitle) !in classSubjectKeys }
    .distinctBy { snapshot -> normalizeSpreadsheetKey(snapshot.subjectTitle).ifBlank { snapshot.distribusiId } }
    .sortedWith(compareBy<MapelScoreSnapshot> { raporSubjectOrderIndex(it.subjectTitle) }.thenBy { it.subjectTitle.lowercase() }.thenBy { it.distribusiId })
    .map { snapshot ->
      snapshot.toRaporSubjectReport(
        student = student,
        kkmBySubject = kkmBySubject,
        manualSubjects = manualSubjects,
        descriptionTemplate = descriptionTemplates[snapshot.distribusiId]
      )
    }
  val subjects = (classSubjectReports + extraScoreSubjects).dedupeRaporSubjectReports()
  val knowledgeValues = subjects.mapNotNull { it.knowledgeScoreText.toDoubleOrNull() }
  val skillValues = subjects.mapNotNull { it.skillScoreText.toDoubleOrNull() }
  val averageKnowledge = knowledgeValues.average().takeIf { !it.isNaN() }
  val averageSkill = skillValues.average().takeIf { !it.isNaN() }
  val automaticAttendanceRows = buildAutomaticRaporAttendanceRows(
    student = student,
    semester = semester,
    utsSnapshot = utsSnapshot,
    attendanceSnapshots = attendanceSnapshots
  )
  val quranReportRows = (manual?.quranRows?.takeIf { it.isNotEmpty() } ?: defaultQuranRows()).withAutomaticQuranScores()
  val quranReportAverage = quranReportRows.mapNotNull { parseRaporNumber(it.scoreText) }.average().takeIf { !it.isNaN() }
  val quranReportAverageScore = manual?.quranAverageScore.orEmpty()
    .ifBlank { quranReportAverage?.let(::formatRaporScore).orEmpty() }
  val quranReportAveragePredicate = manual?.quranAveragePredicate.orEmpty()
    .ifBlank { quranReportAverage?.let(::quranScorePredicate).orEmpty() }
  return RaporStudentReport(
    studentId = student.id,
    studentName = student.name,
    studentNisn = student.nisn,
    className = student.className,
    semesterLabel = semester.label,
    academicYearLabel = semester.academicYearDisplayLabel(),
    schoolName = manual?.schoolName?.ifBlank { null } ?: DefaultSchoolName,
    schoolAddress = manual?.schoolAddress?.ifBlank { null } ?: DefaultSchoolAddress,
    waliKelasName = manual?.waliKelasName?.ifBlank { null } ?: fallbackWaliName,
    kepalaSekolahName = manual?.kepalaSekolahName.orEmpty(),
    spiritualPredicate = manual?.spiritualPredicate.orEmpty(),
    spiritualDescription = manual?.spiritualDescription.orEmpty(),
    socialPredicate = manual?.socialPredicate.orEmpty(),
    socialDescription = manual?.socialDescription.orEmpty(),
    affectiveRows = manual?.affectiveRows ?: defaultAffectiveRows(),
    musyrifName = manual?.musyrifName.orEmpty(),
    quranIkhtibarScore = manual?.quranIkhtibarScore.orEmpty(),
    quranTargetSemester = manual?.quranTargetSemester.orEmpty(),
    quranTotalHafalan = manual?.quranTotalHafalan.orEmpty(),
    quranRows = quranReportRows,
    quranAverageScore = quranReportAverageScore,
    quranAveragePredicate = quranReportAveragePredicate,
    muhaffizName = manual?.muhaffizName.orEmpty(),
    subjects = subjects,
    totalKnowledgeText = knowledgeValues.sum().takeIf { knowledgeValues.isNotEmpty() }?.let(::formatRaporScore) ?: "-",
    totalKnowledgePredicate = averageKnowledge?.let(::scorePredicate).orEmpty().ifBlank { "-" },
    totalSkillText = skillValues.sum().takeIf { skillValues.isNotEmpty() }?.let(::formatRaporScore) ?: "-",
    totalSkillPredicate = averageSkill?.let(::scorePredicate).orEmpty().ifBlank { "-" },
    averageKnowledgeText = averageKnowledge?.let(::formatRaporScore) ?: "-",
    extracurricularRows = manual?.extracurricularRows.orEmpty().filterNot(::simpleRowLooksLikeHeader),
    achievementRows = manual?.achievementRows.orEmpty()
      .filterNot(::simpleRowLooksLikeHeader)
      .filterNot { row -> manual?.extracurricularRows.orEmpty().containsEquivalentSimpleRow(row) },
    attendanceRows = mergeAutomaticAndManualAttendanceRows(automaticAttendanceRows, manual?.attendanceRows)
  )
}

private fun UtsClassSubject.toSubjectOverview(
  className: String,
  semester: UtsSemesterInfo
): SubjectOverview {
  return SubjectOverview(
    id = distribusiId,
    title = name,
    className = className,
    semester = semester.label,
    semesterActive = semester.isActive,
    attendancePending = 0,
    scorePending = 0,
    materialCount = 0
  )
}

private fun buildAutomaticRaporAttendanceRows(
  student: WaliSantriProfile,
  semester: UtsSemesterInfo,
  utsSnapshot: UtsReportSnapshot,
  attendanceSnapshots: List<MapelAttendanceSnapshot>
): List<RaporAttendanceRowReport> {
  val relevantDistribusiIds = utsSnapshot.classSubjects
    .filter { subject ->
      subject.semesterId == semester.id &&
        subject.classId == student.classId &&
        subject.distribusiId.isNotBlank()
    }
    .map { it.distribusiId }
    .toSet()
  val relevantSnapshots = attendanceSnapshots.filter { snapshot ->
    snapshot.distribusiId in relevantDistribusiIds ||
      (relevantDistribusiIds.isEmpty() && snapshot.matchesRaporAttendanceClass(student))
  }
  val classAttendance = buildClassAttendanceRow(student, relevantSnapshots)
  return defaultAttendanceRows().map { row ->
    if (row.label.isKehadiranKelasLabel()) classAttendance else row
  }
}

private fun buildClassAttendanceRow(
  student: WaliSantriProfile,
  snapshots: List<MapelAttendanceSnapshot>
): RaporAttendanceRowReport {
  val normalizedStudentName = normalizeSpreadsheetKey(student.name)
  val statusesByDate = snapshots
    .flatMap { snapshot ->
      val attendanceStudent = snapshot.students.firstOrNull { it.id == student.id } ?: snapshot.students.firstOrNull {
        val attendanceName = normalizeSpreadsheetKey(it.name)
        normalizedStudentName.isNotBlank() &&
          (attendanceName == normalizedStudentName || attendanceName.contains(normalizedStudentName) || normalizedStudentName.contains(attendanceName))
      }
      attendanceStudent?.history.orEmpty()
    }
    .mapNotNull { entry ->
      val date = entry.dateIso.trim().take(10)
      val status = normalizeRaporAttendanceStatus(entry.status)
      if (date.isBlank() || status.isBlank()) null else date to status
    }
    .groupBy({ it.first }, { it.second })
    .mapValues { (_, statuses) -> resolveRaporAttendanceStatusForDate(statuses) }

  return RaporAttendanceRowReport(
    label = "Kehadiran kelas",
    izinText = statusesByDate.values.count { it == "Izin" }.toString(),
    sakitText = statusesByDate.values.count { it == "Sakit" }.toString(),
    telatText = statusesByDate.values.count { it == "Terlambat" }.toString(),
    alpaText = statusesByDate.values.count { it == "Alpa" }.toString()
  )
}

private fun mergeAutomaticAndManualAttendanceRows(
  automaticRows: List<RaporAttendanceRowReport>,
  manualRows: List<RaporAttendanceRowReport>?
): List<RaporAttendanceRowReport> {
  val manualByLabel = manualRows.orEmpty().associateBy { normalizeSpreadsheetKey(it.label) }
  return automaticRows.map { automatic ->
    manualByLabel[normalizeSpreadsheetKey(automatic.label)]
      ?.takeIf { it.hasManualAttendanceValue() }
      ?: automatic
  }
}

private fun RaporAttendanceRowReport.hasManualAttendanceValue(): Boolean {
  return listOf(izinText, sakitText, telatText, alpaText).any { it.trim().ifBlank { "0" } != "0" }
}

private fun MapelAttendanceSnapshot.matchesRaporAttendanceClass(student: WaliSantriProfile): Boolean {
  val snapshotClass = normalizeSpreadsheetKey(className)
  val studentClass = normalizeSpreadsheetKey(student.className)
  return studentClass.isNotBlank() && (
    snapshotClass == studentClass ||
      snapshotClass.contains(studentClass) ||
      studentClass.contains(snapshotClass)
    )
}

private fun String.isKehadiranKelasLabel(): Boolean {
  val normalized = normalizeSpreadsheetKey(this)
  return normalized == "kehadiran kelas" || normalized.contains("kelas")
}

private fun normalizeRaporAttendanceStatus(status: String): String {
  val normalized = normalizeSpreadsheetKey(status)
  return when {
    normalized == "hadir" || normalized == "h" -> "Hadir"
    normalized == "terlambat" || normalized == "telat" || normalized == "t" -> "Terlambat"
    normalized == "sakit" || normalized == "s" -> "Sakit"
    normalized == "izin" || normalized == "ijin" || normalized == "i" -> "Izin"
    normalized == "alpa" || normalized == "alpha" || normalized == "absen" || normalized == "a" -> "Alpa"
    else -> ""
  }
}

private fun resolveRaporAttendanceStatusForDate(statuses: List<String>): String {
  return when {
    statuses.any { it == "Hadir" || it == "Terlambat" } -> "Hadir"
    statuses.any { it == "Sakit" } -> "Sakit"
    statuses.any { it == "Izin" } -> "Izin"
    statuses.any { it == "Alpa" } -> "Alpa"
    else -> ""
  }
}

private fun MapelScoreSnapshot.toRaporSubjectReport(
  student: WaliSantriProfile,
  kkmBySubject: Map<String, String>,
  manualSubjects: Map<String, RaporManualSubjectData>,
  descriptionTemplate: RaporDescriptionTemplate?
): RaporSubjectReport {
  val manual = manualSubjects[normalizeSpreadsheetKey(subjectTitle)]
  val score = findRaporScoreStudent(student)
  val knowledgeScore = manual?.knowledgeScoreText.orEmpty()
    .ifBlank { score?.nilaiAkhir?.let(::formatRaporScore).orEmpty() }
    .ifBlank { "-" }
  val skillScore = manual?.skillScoreText.orEmpty()
    .ifBlank { score?.nilaiKeterampilan?.let(::formatRaporScore).orEmpty() }
    .ifBlank { "-" }
  val knowledgePredicate = manual?.knowledgePredicate.orEmpty().ifBlank { knowledgeScore.toDoubleOrNull()?.let(::scorePredicate).orEmpty() }
  val skillPredicate = manual?.skillPredicate.orEmpty().ifBlank { skillScore.toDoubleOrNull()?.let(::scorePredicate).orEmpty() }
  return RaporSubjectReport(
    subjectKey = distribusiId.ifBlank { subjectTitle },
    subjectName = subjectTitle,
    kkmText = kkmBySubject[normalizeSpreadsheetKey(subjectTitle)].orEmpty().ifBlank { "17" },
    knowledgeScoreText = knowledgeScore,
    knowledgePredicate = knowledgePredicate,
    knowledgeDescription = manual?.knowledgeDescription.orEmpty().ifBlank {
      descriptionTemplate?.knowledgeDescription(knowledgePredicate).orEmpty()
    },
    skillScoreText = skillScore,
    skillPredicate = skillPredicate,
    skillDescription = manual?.skillDescription.orEmpty().ifBlank {
      descriptionTemplate?.skillDescription(skillPredicate).orEmpty()
    }
  )
}

private fun MapelScoreSnapshot.findRaporScoreStudent(student: WaliSantriProfile): ScoreStudent? {
  val normalizedName = normalizeSpreadsheetKey(student.name)
  return students.firstOrNull { it.id == student.id }
    ?: students.firstOrNull { normalizeSpreadsheetKey(it.name) == normalizedName }
    ?: students.firstOrNull {
      val scoreName = normalizeSpreadsheetKey(it.name)
      normalizedName.isNotBlank() && (scoreName.contains(normalizedName) || normalizedName.contains(scoreName))
    }
}

private fun List<UtsClassSubject>.preferredRaporSubject(): UtsClassSubject {
  return firstOrNull { it.name.isNotBlank() && it.kkmText.isNotBlank() }
    ?: firstOrNull()
    ?: UtsClassSubject()
}

private fun UtsClassSubject.raporSubjectDedupKey(): String {
  return normalizeSpreadsheetKey(name).ifBlank { mapelId.ifBlank { distribusiId } }
}

private fun List<UtsScoreRow>.findRaporAcademicScore(
  studentId: String,
  semesterId: String,
  subjects: List<UtsClassSubject>
): UtsScoreRow? {
  val mapelIds = subjects.map { it.mapelId }.filter { it.isNotBlank() }.toSet()
  val candidates = filter { row ->
    row.studentId == studentId &&
      row.semesterId == semesterId &&
      row.mapelId in mapelIds
  }
  return candidates.firstOrNull { it.hasRaporScoreData() } ?: candidates.firstOrNull()
}

private fun UtsScoreRow.hasRaporScoreData(): Boolean {
  return knowledgeScoreText.isNotBlank() ||
    skillScoreText.isNotBlank() ||
    knowledgeScoreValue != null ||
    skillScoreValue != null
}

private fun List<MapelScoreSnapshot>.findRaporScoreStudentForSubjects(
  student: WaliSantriProfile,
  subjects: List<UtsClassSubject>
): ScoreStudent? {
  val exactDistributionIds = subjects.map { it.distribusiId }.filter { it.isNotBlank() }.toSet()
  val subjectNames = subjects.map { normalizeSpreadsheetKey(it.name) }.filter { it.isNotBlank() }.toSet()
  val candidates = filter { snapshot ->
    (snapshot.distribusiId in exactDistributionIds) ||
      (snapshot.matchesRaporStudentClass(student) && normalizeSpreadsheetKey(snapshot.subjectTitle) in subjectNames)
  }.distinctBy { snapshot -> snapshot.distribusiId.ifBlank { normalizeSpreadsheetKey(snapshot.subjectTitle) } }
  return candidates.mapNotNull { snapshot ->
    snapshot.findRaporScoreStudent(student)?.takeIf { it.hasRaporScoreData() }
  }.firstOrNull() ?: candidates.mapNotNull { snapshot ->
    snapshot.findRaporScoreStudent(student)
  }.firstOrNull()
}

private fun ScoreStudent.hasRaporScoreData(): Boolean {
  return nilaiAkhir != null || nilaiKeterampilan != null
}

private fun List<RaporSubjectReport>.dedupeRaporSubjectReports(): List<RaporSubjectReport> {
  val merged = linkedMapOf<String, RaporSubjectReport>()
  forEach { report ->
    val key = normalizeSpreadsheetKey(report.subjectName).ifBlank { report.subjectKey }
    val existing = merged[key]
    merged[key] = existing?.mergeDuplicateSubject(report) ?: report
  }
  return merged.values.sortedWith(
    compareBy<RaporSubjectReport> { raporSubjectOrderIndex(it.subjectName) }
      .thenBy { it.subjectName.lowercase() }
      .thenBy { it.subjectKey }
  )
}

private fun RaporSubjectReport.mergeDuplicateSubject(other: RaporSubjectReport): RaporSubjectReport {
  val primary = if (!hasRaporScoreData() && other.hasRaporScoreData()) other else this
  val fallback = if (primary === this) other else this
  return primary.copy(
    kkmText = primary.kkmText.valueOrFallback(fallback.kkmText),
    knowledgeScoreText = primary.knowledgeScoreText.valueOrFallback(fallback.knowledgeScoreText),
    knowledgePredicate = primary.knowledgePredicate.valueOrFallback(fallback.knowledgePredicate),
    knowledgeDescription = primary.knowledgeDescription.valueOrFallback(fallback.knowledgeDescription),
    skillScoreText = primary.skillScoreText.valueOrFallback(fallback.skillScoreText),
    skillPredicate = primary.skillPredicate.valueOrFallback(fallback.skillPredicate),
    skillDescription = primary.skillDescription.valueOrFallback(fallback.skillDescription)
  )
}

private fun RaporSubjectReport.hasRaporScoreData(): Boolean {
  return knowledgeScoreText.isUsefulRaporValue() || skillScoreText.isUsefulRaporValue()
}

private fun String.valueOrFallback(fallback: String): String {
  return if (isUsefulRaporValue()) this else fallback
}

private fun String.isUsefulRaporValue(): Boolean {
  return isNotBlank() && this != "-"
}

private fun MapelScoreSnapshot.matchesRaporSubject(subject: UtsClassSubject): Boolean {
  return if (distribusiId.isNotBlank() && subject.distribusiId.isNotBlank()) {
    distribusiId == subject.distribusiId
  } else {
    normalizeSpreadsheetKey(subjectTitle) == normalizeSpreadsheetKey(subject.name)
  }
}

private fun MapelScoreSnapshot.matchesRaporStudentClass(student: WaliSantriProfile): Boolean {
  val snapshotClass = normalizeClassName(className)
  val studentClass = normalizeClassName(student.className)
  if (snapshotClass.isBlank() || studentClass.isBlank()) return true
  return snapshotClass == studentClass ||
    snapshotClass.contains(studentClass) ||
    studentClass.contains(snapshotClass)
}

private fun UtsClassSubject.toRaporSubjectReport(
  detailScore: ScoreStudent?,
  score: UtsScoreRow?,
  manualSubjects: Map<String, RaporManualSubjectData>,
  descriptionTemplate: RaporDescriptionTemplate?
): RaporSubjectReport {
  val manual = manualSubjects[normalizeSpreadsheetKey(name)]
  val knowledgeScore = manual?.knowledgeScoreText.orEmpty()
    .ifBlank { detailScore?.nilaiAkhir?.let(::formatRaporScore).orEmpty() }
    .ifBlank { score?.knowledgeScoreText.orEmpty() }
    .ifBlank { "-" }
  val skillScore = manual?.skillScoreText.orEmpty()
    .ifBlank { detailScore?.nilaiKeterampilan?.let(::formatRaporScore).orEmpty() }
    .ifBlank { score?.skillScoreText.orEmpty() }
    .ifBlank { "-" }
  val knowledgePredicate = manual?.knowledgePredicate.orEmpty().ifBlank { knowledgeScore.toDoubleOrNull()?.let(::scorePredicate).orEmpty() }
  val skillPredicate = manual?.skillPredicate.orEmpty().ifBlank { skillScore.toDoubleOrNull()?.let(::scorePredicate).orEmpty() }
  return RaporSubjectReport(
    subjectKey = distribusiId.ifBlank { "$semesterId|$mapelId" },
    subjectName = name,
    kkmText = kkmText.ifBlank { "17" },
    knowledgeScoreText = knowledgeScore,
    knowledgePredicate = knowledgePredicate,
    knowledgeDescription = manual?.knowledgeDescription.orEmpty()
      .ifBlank { descriptionTemplate?.knowledgeDescription(knowledgePredicate).orEmpty() }
      .ifBlank { score?.knowledgeDescription.orEmpty() },
    skillScoreText = skillScore,
    skillPredicate = skillPredicate,
    skillDescription = manual?.skillDescription.orEmpty()
      .ifBlank { descriptionTemplate?.skillDescription(skillPredicate).orEmpty() }
      .ifBlank { score?.skillDescription.orEmpty() }
  )
}

private fun RaporStudentReport.toExportData(): RaporExportData {
  return RaporExportData(
    schoolName = schoolName,
    schoolAddress = schoolAddress,
    studentName = studentName,
    studentNisn = studentNisn,
    className = className,
    semesterLabel = semesterLabel,
    academicYearLabel = academicYearLabel,
    waliKelasName = waliKelasName,
    kepalaSekolahName = kepalaSekolahName,
    spiritualPredicate = spiritualPredicate,
    spiritualDescription = spiritualDescription,
    socialPredicate = socialPredicate,
    socialDescription = socialDescription,
    affectiveRows = affectiveRows.map { RaporExportAffectiveRow(it.aspect, it.percentText, it.note) },
    musyrifName = musyrifName,
    quran = RaporExportQuranData(
      ikhtibarScore = quranIkhtibarScore,
      targetSemester = quranTargetSemester,
      totalHafalan = quranTotalHafalan,
      rows = quranRows.map { RaporExportQuranRow(it.juzLabel, it.tajwidScore, it.fluencyScore, it.scoreText, it.predicate) },
      averageScore = quranAverageScore,
      averagePredicate = quranAveragePredicate,
      muhaffizName = muhaffizName
    ),
    subjects = subjects.map {
      RaporExportSubject(
        name = it.subjectName,
        kkmText = it.kkmText,
        knowledgeScoreText = it.knowledgeScoreText,
        knowledgePredicate = it.knowledgePredicate,
        knowledgeDescription = it.knowledgeDescription,
        skillScoreText = it.skillScoreText,
        skillPredicate = it.skillPredicate,
        skillDescription = it.skillDescription
      )
    },
    totalKnowledgeText = totalKnowledgeText,
    totalKnowledgePredicate = totalKnowledgePredicate,
    totalSkillText = totalSkillText,
    totalSkillPredicate = totalSkillPredicate,
    extracurricularRows = extracurricularRows.map { RaporExportSimpleRow(it.label, it.value, it.note) },
    achievementRows = achievementRows.map { RaporExportSimpleRow(it.label, it.value, it.note) },
    attendanceRows = attendanceRows.map { RaporExportAttendanceRow(it.label, it.izinText, it.sakitText, it.telatText, it.alpaText) }
  )
}

private suspend fun fetchRaporImportPreview(
  rawUrl: String,
  classNames: List<String>,
  students: List<WaliSantriProfile>,
  section: RaporSection,
  onProgress: (Int) -> Unit = {}
): RaporImportPreview = withContext(Dispatchers.IO) {
  val spreadsheetId = Regex("""/spreadsheets/d/([a-zA-Z0-9-_]+)""").find(rawUrl)?.groupValues?.getOrNull(1)
  val gid = Regex("""[?&]gid=([0-9]+)""").find(rawUrl)?.groupValues?.getOrNull(1)
  val candidateUrls = buildList {
    if (spreadsheetId != null) {
      gid?.let { add("https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&gid=$it") }
      classNames.flatMap { raporClassSheetAliases(it) }.distinct().forEach { sheet ->
        val encoded = URLEncoder.encode(sheet, "UTF-8")
        add("https://docs.google.com/spreadsheets/d/$spreadsheetId/gviz/tq?tqx=out:csv&sheet=$encoded")
      }
      raporSectionSheetAliases(section).forEach { sheet ->
        val encoded = URLEncoder.encode(sheet, "UTF-8")
        add("https://docs.google.com/spreadsheets/d/$spreadsheetId/gviz/tq?tqx=out:csv&sheet=$encoded")
      }
      add("https://docs.google.com/spreadsheets/d/$spreadsheetId/gviz/tq?tqx=out:csv")
      add("https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv")
    } else {
      add(rawUrl)
    }
  }.distinct()

  var lastError: Throwable? = null
  var firstReadablePreview: RaporImportPreview? = null
  var sawMatchingHeader = false
  reportRaporImportProgress(onProgress, 0)
  candidateUrls.forEachIndexed { index, url ->
    reportRaporImportProgress(onProgress, ((index * 100) / candidateUrls.size).coerceIn(1, 99))
    runCatching { downloadText(url) }
      .onSuccess { csv ->
        if (csv.looksLikeHtmlResponse()) {
          lastError = IllegalStateException("Spreadsheet belum bisa dibaca sebagai CSV. Pastikan link bisa diakses publik atau gunakan link sheet yang benar.")
          reportRaporImportProgress(onProgress, (((index + 1) * 100) / candidateUrls.size).coerceIn(1, 99))
          return@onSuccess
        }
        val rows = parseCsvRows(csv).filter { it.any(String::isNotBlank) }
        val headers = buildRaporHeaderSpec(rows, section).headers
        if (!headers.looksLikeRaporImportHeaders(section)) {
          reportRaporImportProgress(onProgress, (((index + 1) * 100) / candidateUrls.size).coerceIn(1, 99))
          return@onSuccess
        }
        sawMatchingHeader = true
        val preview = buildRaporImportPreview(csv, students, section)
        if (preview.matchedRows.isNotEmpty()) {
          reportRaporImportProgress(onProgress, 100)
          return@withContext preview
        }
        if (firstReadablePreview == null) firstReadablePreview = preview
        reportRaporImportProgress(onProgress, (((index + 1) * 100) / candidateUrls.size).coerceIn(1, 99))
      }
      .onFailure {
        lastError = it
        reportRaporImportProgress(onProgress, (((index + 1) * 100) / candidateUrls.size).coerceIn(1, 99))
      }
  }
  if (firstReadablePreview != null) {
    throw IllegalStateException(
      "Spreadsheet bagian ${section.label} terbaca, tapi belum ada baris yang cocok dengan daftar santri kelas ini. Pastikan tab sheet kelas yang benar dipakai."
    )
  }
  throw IllegalStateException(
    lastError?.message ?: if (sawMatchingHeader) {
      "Spreadsheet bagian ${section.label} terbaca, tapi datanya belum cocok dengan santri kelas ini."
    } else {
      "Spreadsheet bisa dibuka, tapi header bagian ${section.label} belum ditemukan. Pastikan memilih jenis import dan tab sheet yang sesuai."
    }
  )
}

private suspend fun reportRaporImportProgress(
  onProgress: (Int) -> Unit,
  progress: Int
) {
  withContext(Dispatchers.Main) {
    onProgress(progress.coerceIn(0, 100))
  }
}

private fun downloadText(url: String): String {
  val connection = (URL(url).openConnection() as HttpURLConnection).apply {
    requestMethod = "GET"
    connectTimeout = 5_000
    readTimeout = 10_000
  }
  return try {
    val code = connection.responseCode
    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
    val text = stream.bufferedReader().use { it.readText() }
    if (code !in 200..299) throw IllegalStateException(text.ifBlank { "HTTP $code" })
    text
  } finally {
    connection.disconnect()
  }
}

private fun String.looksLikeHtmlResponse(): Boolean {
  val trimmed = trimStart()
  return trimmed.startsWith("<!doctype", ignoreCase = true) ||
    trimmed.startsWith("<html", ignoreCase = true)
}

private fun buildRaporHeaderSpec(
  rows: List<List<String>>,
  section: RaporSection
): RaporHeaderSpec {
  val first = rows.firstOrNull().orEmpty()
  if (first.isEmpty()) return RaporHeaderSpec(emptyList(), 0)
  val second = rows.getOrNull(1).orEmpty()
  val useTwoRows = second.isRaporSubHeaderRow(section)
  val headers = if (useTwoRows) {
    combineRaporHeaderRows(first, second).map(::normalizeSpreadsheetKey)
  } else {
    first.map(::normalizeSpreadsheetKey)
  }
  return RaporHeaderSpec(headers, if (useTwoRows) 2 else 1)
}

private fun List<String>.isRaporSubHeaderRow(section: RaporSection): Boolean {
  val normalized = map(::normalizeSpreadsheetKey).filter { it.isNotBlank() }
  if (normalized.isEmpty()) return false
  val subHeaderMarkers = when (section) {
    RaporSection.Alquran -> listOf("tajwid", "kelancaran", "nilai", "predikat")
    RaporSection.Akhlak -> listOf("predikat", "deskripsi", "keterangan", "persentase", "nilai", "jenis prestasi", "bulan")
    RaporSection.Lainnya -> listOf("keterangan", "deskripsi", "nilai", "sakit", "izin", "telat", "alpa")
    else -> listOf("predikat", "deskripsi", "keterangan", "nilai")
  }
  val markerCount = normalized.count { header ->
    subHeaderMarkers.any { marker -> header == marker || header.contains(marker) }
  }
  val identityCount = normalized.count { header ->
    header.contains("nama") || header == "nis" || header == "nisn" || header.contains("kelas")
  }
  return markerCount >= 2 && identityCount < 2
}

private fun combineRaporHeaderRows(
  first: List<String>,
  second: List<String>
): List<String> {
  val combined = mutableListOf<String>()
  var groupHeader = ""
  repeat(maxOf(first.size, second.size)) { index ->
    val top = first.getOrNull(index).orEmpty().trim()
    val sub = second.getOrNull(index).orEmpty().trim()
    if (top.isNotBlank()) groupHeader = top
    val group = if (top.isNotBlank()) {
      top
    } else {
      groupHeader.takeIf { it.isRaporGroupedHeader() }.orEmpty()
    }
    val header = when {
      group.isNotBlank() && sub.isNotBlank() && normalizeSpreadsheetKey(group) != normalizeSpreadsheetKey(sub) -> "$group $sub"
      group.isNotBlank() -> group
      else -> sub
    }
    combined += header
  }
  return combined
}

private fun String.isRaporGroupedHeader(): Boolean {
  val normalized = normalizeSpreadsheetKey(this)
  return normalized.startsWith("juz") ||
    normalized in setOf("prestasi", "ekstrakurikuler", "ekskul", "sikap spiritual", "sikap sosial") ||
    normalized.contains("adab") ||
    normalized.contains("akhlak")
}

private fun List<String>.looksLikeRaporImportHeaders(section: RaporSection): Boolean {
  val joined = joinToString(" ")
  val hasIdentity = any { header ->
    header == "nama" ||
      header == "nama santri" ||
      header == "nama siswa" ||
      header == "nama lengkap" ||
      header == "nisn" ||
      header == "nis" ||
      header.contains("nomor induk") ||
      header.contains("nama peserta didik")
  }
  val hasSectionMarker = when (section) {
    RaporSection.Info -> true
    RaporSection.Akhlak -> listOf(
      "predikat spiritual",
      "sikap spiritual",
      "predikat sosial",
      "sikap sosial",
      "ibadah",
      "kebersihan",
      "kedisiplinan",
      "musyrif"
    ).any { marker -> joined.contains(marker) }
    RaporSection.Alquran -> listOf(
      "nilai ikhtibar",
      "ikhtibar semester",
      "target hafalan",
      "total hafalan",
      "tajwid",
      "kelancaran",
      "juz 1",
      "muhaffiz"
    ).any { marker -> joined.contains(marker) }
    RaporSection.Nilai -> listOf(
      "pengetahuan nilai",
      "nilai pengetahuan",
      "keterampilan nilai",
      "nilai keterampilan"
    ).any { marker -> joined.contains(marker) }
    RaporSection.Lainnya -> listOf(
      "ekstrakurikuler",
      "ekskul",
      "prestasi",
      "kelas izin",
      "halaqah izin",
      "liqo izin",
      "kepala sekolah"
    ).any { marker -> joined.contains(marker) }
  }
  return hasIdentity && hasSectionMarker
}

private fun buildRaporImportPreview(
  csv: String,
  students: List<WaliSantriProfile>,
  section: RaporSection
): RaporImportPreview {
  val rows = parseCsvRows(csv).filter { it.any(String::isNotBlank) }
  val headerSpec = buildRaporHeaderSpec(rows, section)
  if (headerSpec.headers.isEmpty()) return RaporImportPreview(emptyList(), 0)
  val normalizedHeaders = headerSpec.headers
  val dataRows = rows.drop(headerSpec.dataStartIndex)
  val matched = linkedMapOf<String, RaporImportPreviewRow>()
  var unmatched = 0
  dataRows.forEach { row ->
    val studentName = valueByAlias(
      row,
      normalizedHeaders,
      "nama santri",
      "nama siswa",
      "nama peserta didik",
      "nama lengkap",
      "santri",
      "siswa",
      "nama"
    ).ifBlank { fallbackStudentName(row, normalizedHeaders) }
    val nisn = valueByAlias(
      row,
      normalizedHeaders,
      "nisn",
      "nis",
      "nomor induk",
      "nomor induk siswa",
      "nomor induk santri",
      "no induk"
    ).ifBlank { fallbackStudentNisn(row, normalizedHeaders) }
    val className = valueByAlias(
      row,
      normalizedHeaders,
      "kelas",
      "kelas siswa",
      "kelas santri",
      "class"
    ).ifBlank { fallbackStudentClass(row, normalizedHeaders) }
    val student = matchRaporStudent(nisn, studentName, className, students)
    if (student == null) {
      unmatched++
    } else {
      val incomingManual = parseRaporManualData(row, normalizedHeaders, section)
      val existing = matched[student.id]
      matched[student.id] = if (existing == null) {
        RaporImportPreviewRow(
          student = student,
          manual = incomingManual
        )
      } else {
        existing.copy(manual = existing.manual.mergeSection(section, incomingManual))
      }
    }
  }
  return RaporImportPreview(matched.values.toList(), unmatched)
}

private fun parseRaporManualData(
  row: List<String>,
  headers: List<String>,
  section: RaporSection
): RaporManualData {
  val subjectOverrides = if (section == RaporSection.Nilai) parseSubjectOverrides(row, headers) else emptyMap()
  val affectiveRows = listOf(
    parseAffectiveRow(row, headers, "Ibadah", listOf("ibadah", "persentase ibadah", "nilai ibadah"), listOf("keterangan ibadah", "deskripsi ibadah")),
    parseAffectiveRow(row, headers, "Kebersihan", listOf("kebersihan", "persentase kebersihan", "nilai kebersihan"), listOf("keterangan kebersihan", "deskripsi kebersihan")),
    parseAffectiveRow(row, headers, "Kedisiplinan", listOf("kedisiplinan", "persentase kedisiplinan", "nilai kedisiplinan"), listOf("keterangan kedisiplinan", "deskripsi kedisiplinan")),
    parseAffectiveRow(
      row = row,
      headers = headers,
      label = "Adab dan akhlak",
      valueAliases = listOf("adab dan akhlak", "adab akhlak", "adab", "akhlak", "persentase adab dan akhlak", "nilai adab dan akhlak", "persentase akhlak", "nilai akhlak", "persentase adab", "nilai adab"),
      noteAliases = listOf("keterangan adab dan akhlak", "deskripsi adab dan akhlak", "keterangan akhlak", "deskripsi akhlak", "keterangan adab", "deskripsi adab")
    )
  )
  val quranRows = if (section == RaporSection.Alquran) (1..30).map { juz ->
    val label = "Juz $juz"
    val tajwid = quranJuzValue(row, headers, juz, QuranJuzValueType.Tajwid)
    val fluency = quranJuzValue(row, headers, juz, QuranJuzValueType.Fluency)
    val score = quranJuzValue(row, headers, juz, QuranJuzValueType.Score)
      .ifBlank { averageQuranScore(tajwid, fluency) }
    val predicate = quranJuzValue(row, headers, juz, QuranJuzValueType.Predicate)
      .ifBlank { quranScorePredicate(score) }
    RaporQuranRowReport(label, tajwid, fluency, score, predicate)
  } else emptyList()
  val quranAverage = quranRows.mapNotNull { parseRaporNumber(it.scoreText) }.average().takeIf { !it.isNaN() }
  val extracurriculars = if (section == RaporSection.Lainnya) {
    parseSimpleRows(row, headers, "ekstrakurikuler", "ekskul", "kegiatan ekstrakurikuler", "kegiatan ekstrakulikuler")
  } else {
    emptyList()
  }
  val achievements = if (section == RaporSection.Lainnya) {
    parseSimpleRows(row, headers, "prestasi", "jenis prestasi")
  } else if (section == RaporSection.Akhlak) {
    parseSimpleRows(row, headers, "prestasi", "jenis prestasi")
  } else {
    emptyList()
  }
  val attendanceRows = parseRaporImportAttendanceRows(row, headers, section)
  val spiritualPredicate = if (section == RaporSection.Akhlak) {
    valueByAlias(row, headers, "predikat spiritual", "sikap spiritual predikat", "spiritual predikat", "sikap spiritual", "spiritual")
  } else {
    ""
  }
  val socialPredicate = if (section == RaporSection.Akhlak) {
    valueByAlias(row, headers, "predikat sosial", "sikap sosial predikat", "sosial predikat", "sikap sosial", "sosial")
  } else {
    ""
  }
  return RaporManualData(
    schoolName = "",
    schoolAddress = "",
    waliKelasName = "",
    kepalaSekolahName = if (section == RaporSection.Lainnya) valueByAlias(row, headers, "kepala sekolah", "pimpinan") else "",
    spiritualPredicate = spiritualPredicate,
    spiritualDescription = if (section == RaporSection.Akhlak) {
      valueByAlias(row, headers, "deskripsi spiritual", "spiritual deskripsi", "keterangan spiritual", "spiritual keterangan", "sikap spiritual deskripsi", "deskripsi sikap spiritual", "sikap spiritual keterangan", "keterangan sikap spiritual")
        .ifBlank { valueNearAnchor(row, headers, listOf("predikat spiritual", "sikap spiritual predikat", "sikap spiritual", "spiritual"), listOf("deskripsi", "keterangan"), 1) }
    } else "",
    socialPredicate = socialPredicate,
    socialDescription = if (section == RaporSection.Akhlak) {
      valueByAlias(row, headers, "deskripsi sosial", "sosial deskripsi", "keterangan sosial", "sosial keterangan", "sikap sosial deskripsi", "deskripsi sikap sosial", "sikap sosial keterangan", "keterangan sikap sosial")
        .ifBlank { valueNearAnchor(row, headers, listOf("predikat sosial", "sikap sosial predikat", "sikap sosial", "sosial"), listOf("deskripsi", "keterangan"), 1) }
    } else "",
    affectiveRows = if (section == RaporSection.Akhlak) affectiveRows else defaultAffectiveRows(),
    musyrifName = if (section == RaporSection.Akhlak) valueByAlias(row, headers, "musyrif", "nama musyrif") else "",
    quranIkhtibarScore = if (section == RaporSection.Alquran) {
      valueByAlias(
        row,
        headers,
        "nilai ikhtibar semester",
        "ikhtibar semester",
        "nilai ikhtibar",
        "ikhtibar",
        "nilai ujian semester",
        "ujian semester",
        "nilai ujian",
        "ujian",
        "nilai semester"
      )
    } else "",
    quranTargetSemester = if (section == RaporSection.Alquran) {
      valueByAlias(
        row,
        headers,
        "capaian target hafalan semester",
        "target hafalan semester",
        "target hafalan",
        "target semester",
        "capaian target",
        "capaian hafalan semester",
        "capaian hafalan",
        "hafalan semester",
        "target"
      )
    } else "",
    quranTotalHafalan = if (section == RaporSection.Alquran) valueByAlias(row, headers, "total jumlah hafalan keseluruhan", "total hafalan", "jumlah hafalan keseluruhan") else "",
    quranRows = quranRows,
    quranAverageScore = if (section == RaporSection.Alquran) valueByAlias(row, headers, "rata rata alquran", "rata rata quran").ifBlank { quranAverage?.let(::formatRaporScore).orEmpty() } else "",
    quranAveragePredicate = if (section == RaporSection.Alquran) valueByAlias(row, headers, "predikat alquran", "predikat quran").ifBlank { quranAverage?.let(::quranScorePredicate).orEmpty() } else "",
    muhaffizName = if (section == RaporSection.Alquran) {
      valueByAlias(
        row,
        headers,
        "muhaffiz",
        "nama muhaffiz",
        "muhafiz",
        "nama muhafiz",
        "muhafidz",
        "nama muhafidz",
        "guru tahfidz",
        "nama guru tahfidz",
        "pengampu tahfidz",
        "ustadz tahfidz"
      )
    } else "",
    subjectOverrides = subjectOverrides,
    extracurricularRows = extracurriculars,
    achievementRows = achievements,
    attendanceRows = attendanceRows
  )
}

private fun parseAffectiveRow(
  row: List<String>,
  headers: List<String>,
  label: String,
  valueAliases: List<String>,
  noteAliases: List<String>
): RaporAffectiveRowReport {
  val percent = valueByAlias(row, headers, *valueAliases.toTypedArray())
  val note = valueByAlias(row, headers, *noteAliases.toTypedArray())
    .takeUnless { cellLooksLikeHeader(it, noteAliases) }
    .orEmpty()
    .ifBlank { affectiveNoteForPercent(percent) }
  return RaporAffectiveRowReport(label, percent, note)
}

private fun affectiveNoteForPercent(percentText: String): String {
  val value = Regex("""-?\d+(?:[\.,]\d+)?""")
    .find(percentText)
    ?.value
    ?.replace(",", ".")
    ?.toDoubleOrNull()
    ?: return ""
  return when {
    value >= 95.0 -> "Istimewa"
    value >= 85.0 -> "Amat baik"
    value >= 80.0 -> "Baik"
    value >= 70.0 -> "Cukup"
    else -> "Kurang"
  }
}

private enum class QuranJuzValueType(
  val offsetFromJuzAnchor: Int,
  val markers: List<String>
) {
  Tajwid(0, listOf("tajwid")),
  Fluency(1, listOf("kelancaran", "lancar")),
  Score(2, listOf("nilai", "skor")),
  Predicate(3, listOf("predikat"))
}

private fun quranJuzValue(
  row: List<String>,
  headers: List<String>,
  juz: Int,
  type: QuranJuzValueType
): String {
  val paddedJuz = juz.toString().padStart(2, '0')
  val aliases = type.markers.flatMap { marker ->
    listOf(
      "juz $juz $marker",
      "juz $paddedJuz $marker",
      "juz ke $juz $marker",
      "juz ke $paddedJuz $marker",
      "juz$juz $marker",
      "juz$paddedJuz $marker",
      "juz-$juz $marker",
      "juz-$paddedJuz $marker",
      "$marker juz $juz",
      "$marker juz $paddedJuz",
      "$marker juz ke $juz",
      "$marker juz ke $paddedJuz",
      "$marker $juz",
      "$marker $paddedJuz"
    )
  }
  val direct = valueByAlias(row, headers, *aliases.toTypedArray())
  if (direct.isNotBlank()) return direct

  val anchorIndex = headers.indexOfFirst { header -> header.matchesQuranJuz(juz) }
  if (anchorIndex < 0) return ""
  val rangeEnd = generateSequence(anchorIndex + 1) { it + 1 }
    .takeWhile { it <= headers.lastIndex }
    .firstOrNull { index -> headers[index].matchesAnyOtherQuranJuz(juz) }
    ?.minus(1)
    ?: (anchorIndex + 4).coerceAtMost(headers.lastIndex)
  val groupRange = anchorIndex..rangeEnd
  val typedIndex = groupRange.firstOrNull { index ->
    val header = headers[index]
    type.markers.any { marker -> header == marker || header.contains(marker) }
  }
  val offsetIndex = (anchorIndex + type.offsetFromJuzAnchor).takeIf { it in groupRange }
  val valueIndex = typedIndex ?: offsetIndex ?: return ""
  return cleanSpreadsheetCell(row.getOrNull(valueIndex).orEmpty())
}

private fun String.matchesQuranJuz(juz: Int): Boolean {
  val normalized = normalizeSpreadsheetKey(this)
  if (normalized.isBlank()) return false
  val compact = normalized.replace(" ", "")
  return Regex("""\bjuz\s*ke?\s*0?$juz\b""").containsMatchIn(normalized) ||
    Regex("""\bjuz\s*0?$juz\b""").containsMatchIn(normalized) ||
    Regex("""juz0?$juz(?!\d)""").containsMatchIn(compact)
}

private fun String.matchesAnyOtherQuranJuz(currentJuz: Int): Boolean {
  return (1..30).any { juz -> juz != currentJuz && matchesQuranJuz(juz) }
}

private fun averageQuranScore(tajwidText: String, fluencyText: String): String {
  val tajwid = parseRaporNumber(tajwidText) ?: return ""
  val fluency = parseRaporNumber(fluencyText) ?: return ""
  return formatRaporScore((tajwid + fluency) / 2.0)
}

private fun quranScorePredicate(scoreText: String): String {
  return parseRaporNumber(scoreText)?.let(::quranScorePredicate).orEmpty()
}

private fun quranScorePredicate(value: Double): String {
  return when {
    value >= 91.0 -> "A"
    value >= 81.0 -> "B"
    value >= 71.0 -> "C"
    else -> "D"
  }
}

private fun parseRaporNumber(value: String): Double? {
  if (value.isSpreadsheetErrorCell()) return null
  return Regex("""-?\d+(?:[\.,]\d+)?""")
    .find(value)
    ?.value
    ?.replace(",", ".")
    ?.toDoubleOrNull()
}

private fun parseRaporImportAttendanceRows(
  row: List<String>,
  headers: List<String>,
  section: RaporSection
): List<RaporAttendanceRowReport> {
  fun rowFor(label: String, aliases: List<String>): RaporAttendanceRowReport {
    val prefixAliases = aliases.flatMap { alias ->
      listOf(alias, alias.replace(" ", ""))
    }
    fun aliasesFor(type: String, vararg extras: String): Array<String> {
      return (
        prefixAliases.flatMap { prefix ->
          listOf("$prefix $type", "$type $prefix")
        } +
          extras +
          listOf(type)
        ).toTypedArray()
    }
    return RaporAttendanceRowReport(
      label = label,
      izinText = valueByAlias(row, headers, *aliasesFor("izin", "ijin")),
      sakitText = valueByAlias(row, headers, *aliasesFor("sakit")),
      telatText = valueByAlias(row, headers, *aliasesFor("telat", "terlambat")),
      alpaText = valueByAlias(row, headers, *aliasesFor("alpa", "alpha"))
    )
  }

  return when (section) {
    RaporSection.Akhlak -> defaultAttendanceRows().map { defaultRow ->
      if (normalizeSpreadsheetKey(defaultRow.label).contains("liqo")) {
        rowFor("Liqo muhasabah", listOf("liqo", "liqo muhasabah", "muhasabah"))
      } else {
        defaultRow
      }
    }
    RaporSection.Alquran -> defaultAttendanceRows().map { defaultRow ->
      if (normalizeSpreadsheetKey(defaultRow.label).contains("halaqah")) {
        rowFor("Halaqah tahfidz", listOf("halaqah", "halaqah tahfidz", "tahfidz"))
      } else {
        defaultRow
      }
    }
    RaporSection.Lainnya -> listOf(
      rowFor("Kehadiran kelas", listOf("kelas", "kehadiran kelas")),
      rowFor("Halaqah tahfidz", listOf("halaqah", "halaqah tahfidz", "tahfidz")),
      rowFor("Liqo muhasabah", listOf("liqo", "liqo muhasabah", "muhasabah"))
    )
    else -> defaultAttendanceRows()
  }
}

private fun parseSubjectOverrides(
  row: List<String>,
  headers: List<String>
): Map<String, RaporManualSubjectData> {
  val result = linkedMapOf<String, RaporManualSubjectData>()
  val knowledgeScoreSuffixes = listOf("pengetahuan nilai", "nilai pengetahuan")
  headers.forEachIndexed { index, header ->
    val suffix = knowledgeScoreSuffixes.firstOrNull { header.endsWith(" $it") } ?: return@forEachIndexed
    val subjectName = header.removeSuffix(" $suffix").trim()
    if (subjectName.isBlank()) return@forEachIndexed
    val knowledgeScore = row.getOrNull(index).orEmpty().trim()
    val knowledgePredicate = subjectColumnValue(row, headers, subjectName, "pengetahuan predikat", "predikat pengetahuan")
      .ifBlank { row.getOrNull(index + 1).orEmpty().trim() }
    val knowledgeDescription = subjectColumnValue(row, headers, subjectName, "pengetahuan deskripsi", "deskripsi pengetahuan")
      .ifBlank { row.getOrNull(index + 2).orEmpty().trim() }
    val skillScore = subjectColumnValue(row, headers, subjectName, "keterampilan nilai", "nilai keterampilan")
      .ifBlank { row.getOrNull(index + 3).orEmpty().trim() }
    val skillPredicate = subjectColumnValue(row, headers, subjectName, "keterampilan predikat", "predikat keterampilan")
      .ifBlank { row.getOrNull(index + 4).orEmpty().trim() }
    val skillDescription = subjectColumnValue(row, headers, subjectName, "keterampilan deskripsi", "deskripsi keterampilan")
      .ifBlank { row.getOrNull(index + 5).orEmpty().trim() }
    if (listOf(knowledgeScore, knowledgePredicate, knowledgeDescription, skillScore, skillPredicate, skillDescription).any { it.isNotBlank() }) {
      result[normalizeSpreadsheetKey(subjectName)] = RaporManualSubjectData(
        knowledgeScoreText = knowledgeScore,
        knowledgePredicate = knowledgePredicate,
        knowledgeDescription = knowledgeDescription,
        skillScoreText = skillScore,
        skillPredicate = skillPredicate,
        skillDescription = skillDescription
      )
    }
  }
  return result
}

private fun subjectColumnValue(
  row: List<String>,
  headers: List<String>,
  subjectName: String,
  vararg suffixes: String
): String {
  suffixes.forEach { suffix ->
    val key = "${normalizeSpreadsheetKey(subjectName)} ${normalizeSpreadsheetKey(suffix)}".trim()
    val index = headers.indexOf(key)
    if (index >= 0) return row.getOrNull(index).orEmpty().trim()
  }
  return ""
}

private fun parseSimpleRows(
  row: List<String>,
  headers: List<String>,
  vararg baseAliases: String
): List<RaporSimpleRowReport> {
  val rows = mutableListOf<RaporSimpleRowReport>()
  baseAliases.forEach { alias ->
    val normalizedAlias = normalizeSpreadsheetKey(alias)
    val activityAliases = if (normalizedAlias.contains("ekstrakurikuler") || normalizedAlias.contains("ekskul")) {
      listOf(
        "kegiatan ekstrakurikuler",
        "kegiatan ekstrakulikuler",
        "kegiatan ekskul"
      )
    } else {
      emptyList()
    }
    val labelAliases = (
      listOf(
      alias,
      "$alias 1",
      "jenis $alias",
      "kegiatan $alias",
      "nama $alias",
      "nama kegiatan $alias",
      "$alias $alias",
      "$alias jenis $alias"
    ) + activityAliases).distinct()
    val label = valueByAlias(row, headers, *labelAliases.toTypedArray())
      .takeUnless { cellLooksLikeHeader(it, labelAliases + baseAliases.toList()) }
      .orEmpty()
    if (label.isBlank()) return@forEach
    val value = valueByAlias(
      row,
      headers,
      "nilai $alias",
      "$alias nilai",
      "nilai $alias 1",
      "nilai kegiatan $alias",
      "kegiatan $alias nilai"
    ).ifBlank {
      valueNearAnchor(row, headers, labelAliases, listOf("nilai"), 1)
    }
    val note = valueByAlias(row, headers, "deskripsi $alias", "keterangan $alias", "$alias keterangan")
      .ifBlank { valueNearAnchor(row, headers, labelAliases, listOf("keterangan", "deskripsi", "catatan"), 1) }
      .ifBlank { valueNearSimpleRowLabel(row, headers, labelAliases) }
      .takeUnless { cellLooksLikeHeader(it, listOf("keterangan", "deskripsi", "keterangan $alias", "$alias keterangan", "deskripsi $alias")) }
      .orEmpty()
    if (listOf(label, value, note).any { it.isNotBlank() }) {
      rows += RaporSimpleRowReport(label.ifBlank { alias.replaceFirstChar { it.titlecase(Locale.ROOT) } }, value, note)
    }
  }
  return rows.distinctBy { normalizeSpreadsheetKey("${it.label}|${it.value}|${it.note}") }
}

private fun List<RaporSimpleRowReport>.containsEquivalentSimpleRow(row: RaporSimpleRowReport): Boolean {
  val target = row.normalizedSimpleRowKey()
  return target.isNotBlank() && any { it.normalizedSimpleRowKey() == target }
}

private fun RaporSimpleRowReport.normalizedSimpleRowKey(): String {
  return normalizeSpreadsheetKey("${label}|${value}|${note}")
}

private fun valueNearSimpleRowLabel(
  row: List<String>,
  headers: List<String>,
  labelAliases: List<String>
): String {
  val anchorIndex = indexByAlias(headers, *labelAliases.toTypedArray())
  if (anchorIndex < 0) return ""
  val valueIndex = ((anchorIndex + 1)..(anchorIndex + 4).coerceAtMost(headers.lastIndex)).firstOrNull { index ->
    val header = headers[index]
    header == "keterangan" ||
      header == "deskripsi" ||
      header.contains("keterangan") ||
      header.contains("deskripsi") ||
      header.contains("bulan")
  } ?: return ""
  return cleanSpreadsheetCell(row.getOrNull(valueIndex).orEmpty())
}

private fun cellLooksLikeHeader(value: String, aliases: List<String>): Boolean {
  val normalized = normalizeSpreadsheetKey(value)
  if (normalized.isBlank()) return false
  val headerKeys = aliases.map(::normalizeSpreadsheetKey).toSet() +
    setOf("keterangan", "deskripsi", "nilai", "prestasi", "jenis prestasi", "ekstrakurikuler", "ekskul")
  return normalized in headerKeys
}

private fun simpleRowLooksLikeHeader(row: RaporSimpleRowReport): Boolean {
  val labelAliases = listOf("prestasi", "jenis prestasi", "ekstrakurikuler", "ekskul", "kegiatan ekstrakurikuler", "kegiatan ekstrakulikuler")
  val noteAliases = listOf("keterangan", "deskripsi", "bulan")
  return cellLooksLikeHeader(row.label, labelAliases) ||
    (row.label.isBlank() && (cellLooksLikeHeader(row.note, noteAliases) || row.note.isMonthHeaderCell()))
}

private fun String.isMonthHeaderCell(): Boolean {
  return normalizeSpreadsheetKey(this).startsWith("bulan ")
}

private fun indexByAlias(headers: List<String>, vararg aliases: String): Int {
  aliases.forEach { alias ->
    val normalized = normalizeSpreadsheetKey(alias)
    val index = headers.indexOf(normalized)
    if (index >= 0) return index
  }
  return -1
}

private fun valueNearAnchor(
  row: List<String>,
  headers: List<String>,
  anchorAliases: List<String>,
  targetMarkers: List<String>,
  fallbackOffset: Int
): String {
  val normalizedAnchors = anchorAliases.map(::normalizeSpreadsheetKey).filter { it.isNotBlank() }
  val anchorIndex = headers.indexOfFirst { header ->
    header.isNotBlank() && normalizedAnchors.any { alias -> header == alias || header.contains(alias) || alias.contains(header) }
  }
  if (anchorIndex < 0) return ""
  val nearbyRange = ((anchorIndex + 1)..(anchorIndex + 5).coerceAtMost(headers.lastIndex))
  val normalizedMarkers = targetMarkers.map(::normalizeSpreadsheetKey).filter { it.isNotBlank() }
  val markerIndex = nearbyRange.firstOrNull { index ->
    val header = headers[index]
    normalizedMarkers.any { marker -> header == marker || header.contains(marker) } &&
      (
        normalizedMarkers.any { marker -> header == marker } ||
          normalizedAnchors.any { anchor -> header.contains(anchor) || anchor.contains(header) }
        )
  }
  val fallbackIndex = (anchorIndex + fallbackOffset).takeIf { index ->
    index <= headers.lastIndex && headers[index].isBlank()
  }
  val valueIndex = markerIndex ?: fallbackIndex ?: return ""
  return cleanSpreadsheetCell(row.getOrNull(valueIndex).orEmpty())
}

private fun matchRaporStudent(
  nisn: String,
  name: String,
  className: String,
  students: List<WaliSantriProfile>
): WaliSantriProfile? {
  val nisnDigits = nisn.filter(Char::isDigit)
  if (nisnDigits.isNotBlank()) {
    students.firstOrNull { it.nisn.filter(Char::isDigit) == nisnDigits }?.let { return it }
  }
  val normalizedName = normalizeSpreadsheetKey(name)
  if (normalizedName.isBlank()) return null
  val normalizedClass = normalizeClassName(className)
  return students.firstOrNull {
    normalizeSpreadsheetKey(it.name) == normalizedName &&
      (normalizedClass.isBlank() || normalizeClassName(it.className) == normalizedClass)
  } ?: students.firstOrNull {
    normalizeSpreadsheetKey(it.name).contains(normalizedName) || normalizedName.contains(normalizeSpreadsheetKey(it.name))
  }
}

private fun valueByAlias(row: List<String>, headers: List<String>, vararg aliases: String): String {
  val index = indexByAlias(headers, *aliases)
  if (index < 0) return ""
  return cleanSpreadsheetCell(row.getOrNull(index).orEmpty())
}

private fun cleanSpreadsheetCell(value: String): String {
  val trimmed = value.trim()
  return if (trimmed.isSpreadsheetErrorCell()) "" else trimmed
}

private fun String.isSpreadsheetErrorCell(): Boolean {
  val normalized = trim().uppercase(Locale.ROOT)
  return normalized.startsWith("#DIV/0") ||
    normalized.startsWith("#N/A") ||
    normalized.startsWith("#VALUE") ||
    normalized.startsWith("#REF") ||
    normalized.startsWith("#ERROR") ||
    normalized.startsWith("#NUM") ||
    normalized.startsWith("#NAME")
}

private fun fallbackStudentName(row: List<String>, headers: List<String>): String {
  val headerIndex = headers.indexOfFirst { header ->
    header.contains("nama") || header == "santri" || header == "siswa" || header.contains("peserta didik")
  }
  if (headerIndex >= 0) return row.getOrNull(headerIndex).orEmpty().trim()
  return row.drop(1).firstOrNull { cell ->
    val normalized = normalizeSpreadsheetKey(cell)
    normalized.any(Char::isLetter) &&
      !normalized.startsWith("kelas ") &&
      normalized !in setOf("l", "p", "lk", "pr")
  }.orEmpty().trim()
}

private fun fallbackStudentNisn(row: List<String>, headers: List<String>): String {
  val headerIndex = headers.indexOfFirst { header ->
    header == "nis" || header == "nisn" || header.contains("nomor induk")
  }
  if (headerIndex >= 0) return row.getOrNull(headerIndex).orEmpty().trim()
  return row.firstOrNull { cell -> cell.filter(Char::isDigit).length >= 5 }.orEmpty().trim()
}

private fun fallbackStudentClass(row: List<String>, headers: List<String>): String {
  val headerIndex = headers.indexOfFirst { it.contains("kelas") || it == "class" }
  if (headerIndex >= 0) return row.getOrNull(headerIndex).orEmpty().trim()
  return row.firstOrNull { cell -> normalizeSpreadsheetKey(cell).startsWith("kelas ") }.orEmpty().trim()
}

private fun parseCsvRows(csv: String): List<List<String>> {
  val rows = mutableListOf<List<String>>()
  val currentRow = mutableListOf<String>()
  val current = StringBuilder()
  var inQuotes = false
  var index = 0
  while (index < csv.length) {
    val char = csv[index]
    when {
      char == '"' -> {
        if (inQuotes && index + 1 < csv.length && csv[index + 1] == '"') {
          current.append('"')
          index++
        } else {
          inQuotes = !inQuotes
        }
      }
      char == ',' && !inQuotes -> {
        currentRow += current.toString()
        current.clear()
      }
      (char == '\n' || char == '\r') && !inQuotes -> {
        if (char == '\r' && index + 1 < csv.length && csv[index + 1] == '\n') index++
        currentRow += current.toString()
        rows += currentRow.toList()
        currentRow.clear()
        current.clear()
      }
      else -> current.append(char)
    }
    index++
  }
  currentRow += current.toString()
  if (currentRow.any { it.isNotBlank() }) rows += currentRow.toList()
  return rows
}

private fun raporClassSheetAliases(className: String): List<String> {
  val cleaned = className.trim()
  val noPrefix = cleaned
    .replace(Regex("(?i)^kelas\\s+"), "")
    .trim()
  val levelVariants = raporClassLevelVariants(noPrefix)
  return (listOf(cleaned, noPrefix, "Kelas $noPrefix", "kelas $noPrefix") + levelVariants.flatMap { variant ->
    listOf(
      variant,
      "Kelas $variant",
      "kelas $variant",
      variant.replace(" ", ""),
      "Kelas ${variant.replace(" ", "")}",
      "kelas ${variant.replace(" ", "")}"
    )
  })
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .distinct()
}

private fun raporClassLevelVariants(className: String): List<String> {
  val normalized = className.trim()
  val match = Regex("""(?i)^([0-9]+|[ivx]+)\s*([a-z])?$""").find(normalized) ?: return listOf(normalized)
  val level = match.groupValues.getOrNull(1).orEmpty()
  val suffix = match.groupValues.getOrNull(2).orEmpty().uppercase(Locale.ROOT)
  val numericLevel = level.toIntOrNull() ?: romanToInt(level)
  val romanLevel = numericLevel?.let(::intToRoman).orEmpty()
  val levelOptions = listOf(level.uppercase(Locale.ROOT), numericLevel?.toString().orEmpty(), romanLevel)
    .filter { it.isNotBlank() }
    .distinct()
  return levelOptions.flatMap { option ->
    if (suffix.isBlank()) {
      listOf(option)
    } else {
      listOf("$option $suffix", "$option$suffix")
    }
  }.distinct()
}

private fun romanToInt(value: String): Int? {
  return when (value.trim().uppercase(Locale.ROOT)) {
    "I" -> 1
    "II" -> 2
    "III" -> 3
    "IV" -> 4
    "V" -> 5
    "VI" -> 6
    "VII" -> 7
    "VIII" -> 8
    "IX" -> 9
    "X" -> 10
    "XI" -> 11
    "XII" -> 12
    else -> null
  }
}

private fun intToRoman(value: Int): String {
  return when (value) {
    1 -> "I"
    2 -> "II"
    3 -> "III"
    4 -> "IV"
    5 -> "V"
    6 -> "VI"
    7 -> "VII"
    8 -> "VIII"
    9 -> "IX"
    10 -> "X"
    11 -> "XI"
    12 -> "XII"
    else -> ""
  }
}

private fun raporSectionSheetAliases(section: RaporSection): List<String> {
  return when (section) {
    RaporSection.Info -> emptyList()
    RaporSection.Akhlak -> listOf("Akhlak", "Sikap", "Sikap dan Afektif", "Afektif")
    RaporSection.Alquran -> listOf("Alquran", "Al-Quran", "Quran", "Tahfidz", "Capaian Alquran", "Capaian Quran")
    RaporSection.Nilai -> listOf("Nilai", "Nilai Rapor", "Pengetahuan Keterampilan", "Pengetahuan dan Keterampilan")
    RaporSection.Lainnya -> listOf("Lainnya", "Ekskul Prestasi Kehadiran", "Ekstrakurikuler", "Ekskul", "Kegiatan Ekstrakurikuler", "Prestasi", "Kehadiran")
  }.distinct()
}

private fun semesterDisplayLabel(semester: UtsSemesterInfo): String {
  return listOf(
    semester.label.ifBlank { "Semester" },
    semester.academicYearDisplayLabel().ifBlank { null }
  ).filterNotNull().joinToString(" - ")
}

private fun UtsSemesterInfo.academicYearDisplayLabel(): String {
  if (tahunAjaranLabel.isNotBlank()) return tahunAjaranLabel
  val startYear = startDateIso.take(4).toIntOrNull()
  if (startYear != null) return "$startYear/${startYear + 1}"
  val today = LocalDate.now()
  val academicStart = if (today.monthValue >= 7) today.year else today.year - 1
  return "$academicStart/${academicStart + 1}"
}

private val RaporDescriptionPredicates = listOf("A", "B", "C", "D", "E")

private fun emptyRaporDescriptionMap(): Map<String, String> {
  return RaporDescriptionPredicates.associateWith { "" }
}

private fun loadRaporDescriptionTemplates(
  context: Context,
  distribusiIds: List<String>
): Map<String, RaporDescriptionTemplate> {
  if (distribusiIds.isEmpty()) return emptyMap()
  val preferences = context.getSharedPreferences("mapel_rapor_templates", Context.MODE_PRIVATE)
  return distribusiIds.mapNotNull { distribusiId ->
    val rawJson = preferences.getString(distribusiId, "{}").orEmpty()
    val template = decodeRaporDescriptionTemplate(rawJson)
    if (template.hasAnyDescription()) distribusiId to template else null
  }.toMap()
}

private fun decodeRaporDescriptionTemplate(rawJson: String): RaporDescriptionTemplate {
  return runCatching {
    val root = JSONObject(rawJson.ifBlank { "{}" })
    RaporDescriptionTemplate(
      knowledgeDescriptions = readRaporDescriptionMap(
        root.optJSONObject("knowledgeDescriptions") ?: root.optJSONObject("knowledge")
      ),
      skillDescriptions = readRaporDescriptionMap(
        root.optJSONObject("skillDescriptions") ?: root.optJSONObject("skill")
      )
    )
  }.getOrDefault(RaporDescriptionTemplate())
}

private fun readRaporDescriptionMap(row: JSONObject?): Map<String, String> {
  return RaporDescriptionPredicates.associateWith { predicate ->
    row?.optString(predicate).orEmpty().trim()
  }
}

private fun formatRaporScore(value: Double): String {
  val rounded = kotlin.math.round(value * 100.0) / 100.0
  return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "%.2f".format(Locale.US, rounded).trimEnd('0').trimEnd('.')
}

private fun scorePredicate(value: Double): String {
  return when {
    value >= 90.0 -> "A"
    value >= 80.0 -> "B"
    value >= 70.0 -> "C"
    value >= 60.0 -> "D"
    else -> "E"
  }
}

private fun normalizeSpreadsheetKey(value: String): String {
  return value
    .trim()
    .lowercase(Locale.ROOT)
    .replace("'", "")
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()
    .replace(Regex("\\s+"), " ")
}

private fun normalizeClassName(value: String): String {
  return normalizeSpreadsheetKey(value)
    .removePrefix("kelas ")
    .trim()
}

private fun buildRaporManualKey(student: WaliSantriProfile, semester: UtsSemesterInfo): String {
  return "${student.id}|${semester.id}"
}

private fun defaultAffectiveRows(): List<RaporAffectiveRowReport> {
  return listOf(
    RaporAffectiveRowReport("Ibadah"),
    RaporAffectiveRowReport("Kebersihan"),
    RaporAffectiveRowReport("Kedisiplinan"),
    RaporAffectiveRowReport("Adab dan akhlak")
  )
}

private fun defaultQuranRows(): List<RaporQuranRowReport> {
  return (1..30).map { juz -> RaporQuranRowReport("Juz $juz") }
}

private fun List<RaporQuranRowReport>.withAutomaticQuranScores(): List<RaporQuranRowReport> {
  return map { row ->
    val score = row.scoreText.ifBlank { averageQuranScore(row.tajwidScore, row.fluencyScore) }
    row.copy(
      scoreText = score,
      predicate = row.predicate.ifBlank { quranScorePredicate(score) }
    )
  }
}

private fun RaporQuranRowReport.hasValue(): Boolean {
  return tajwidScore.isNotBlank() ||
    fluencyScore.isNotBlank() ||
    scoreText.isNotBlank() ||
    predicate.isNotBlank()
}

private fun defaultAttendanceRows(): List<RaporAttendanceRowReport> {
  return listOf(
    RaporAttendanceRowReport("Kehadiran kelas"),
    RaporAttendanceRowReport("Halaqah tahfidz"),
    RaporAttendanceRowReport("Liqo muhasabah")
  )
}

private fun raporSubjectOrderIndex(name: String): Int {
  val normalized = normalizeSpreadsheetKey(name)
  val index = RaporSubjectOrder.indexOfFirst { order ->
    normalized == order || normalized.contains(order) || order.contains(normalized)
  }
  return if (index >= 0) index else RaporSubjectOrder.size
}

private fun String.initials(): String {
  val words = trim().split(Regex("\\s+")).filter { it.isNotBlank() }
  return when {
    words.isEmpty() -> "?"
    words.size == 1 -> words.first().take(2).uppercase()
    else -> "${words.first().first()}${words.last().first()}".uppercase()
  }
}

private const val DefaultSchoolName = "PQ Putra Markaz Imam Malik"
private const val DefaultSchoolAddress = "Jln. Tamangapa Raya No. 64"
private val RaporSubjectOrder = listOf(
  "bahasa arab",
  "tafsir",
  "hadis",
  "akidah",
  "fikih",
  "sirah",
  "akhlak",
  "hifzul mutun",
  "matematika",
  "ipa",
  "ips",
  "pkn",
  "bahasa inggris",
  "bahasa indonesia"
)
