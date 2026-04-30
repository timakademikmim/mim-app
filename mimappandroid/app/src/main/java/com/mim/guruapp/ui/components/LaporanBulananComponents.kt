package com.mim.guruapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.WarningAmber
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import com.mim.guruapp.MonthlyExtracurricularSaveOutcome
import com.mim.guruapp.MonthlyReportSaveOutcome
import com.mim.guruapp.data.model.MonthlyExtracurricularReport
import com.mim.guruapp.data.model.MonthlyReportItem
import com.mim.guruapp.data.model.MonthlyReportSnapshot
import com.mim.guruapp.data.model.MonthlyAttendanceSummary
import com.mim.guruapp.data.model.GuruProfile
import com.mim.guruapp.data.model.WaliSantriProfile
import com.mim.guruapp.data.model.WaliSantriSnapshot
import com.mim.guruapp.export.MonthlyReportExportData
import com.mim.guruapp.export.MonthlyReportExportExtracurricularRow
import com.mim.guruapp.export.MonthlyReportExportRow
import com.mim.guruapp.export.MonthlyReportExportSection
import com.mim.guruapp.export.MonthlyReportExporter
import com.mim.guruapp.export.MonthlyReportWaTarget
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanBulananScreen(
  waliSantriSnapshot: WaliSantriSnapshot,
  monthlyReportSnapshot: MonthlyReportSnapshot,
  profile: GuruProfile,
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onSaveReport: suspend (MonthlyReportItem) -> MonthlyReportSaveOutcome,
  onSaveExtracurricularReports: suspend (List<MonthlyExtracurricularReport>) -> MonthlyExtracurricularSaveOutcome,
  onDetailModeChange: (Boolean) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val periods = remember(monthlyReportSnapshot.availablePeriods) {
    monthlyReportSnapshot.availablePeriods.ifEmpty { buildAvailableMonthlyPeriods() }
  }
  var selectedPeriod by rememberSaveable { mutableStateOf(periods.firstOrNull().orEmpty()) }
  var selectedSantriId by rememberSaveable { mutableStateOf<String?>(null) }
  var quickActionSantriId by rememberSaveable { mutableStateOf<String?>(null) }
  var isSaving by rememberSaveable { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  val snackbarScope = rememberCoroutineScope()

  LaunchedEffect(periods) {
    if (selectedPeriod !in periods) {
      selectedPeriod = periods.firstOrNull().orEmpty()
    }
  }

  LaunchedEffect(selectedSantriId, quickActionSantriId) {
    onDetailModeChange(selectedSantriId != null || quickActionSantriId != null)
  }

  DisposableEffect(Unit) {
    onDispose { onDetailModeChange(false) }
  }

  val students = remember(waliSantriSnapshot.students) {
    waliSantriSnapshot.students.sortedWith(compareBy<WaliSantriProfile> { it.className }.thenBy { it.name })
  }
  LaunchedEffect(selectedPeriod, students) {
    if (quickActionSantriId != null && students.none { it.id == quickActionSantriId }) {
      quickActionSantriId = null
    }
  }
  val selectedSantri = students.firstOrNull { it.id == selectedSantriId }
  val reportsByStudent = remember(monthlyReportSnapshot.reports, selectedPeriod) {
    monthlyReportSnapshot.reports
      .filter { it.period == selectedPeriod }
      .associateBy { it.studentId }
  }
  val attendanceByStudent = remember(monthlyReportSnapshot.attendanceSummaries, selectedPeriod) {
    monthlyReportSnapshot.attendanceSummaries
      .filter { it.period == selectedPeriod }
      .associateBy { it.studentId }
  }
  val extracurricularByStudent = remember(monthlyReportSnapshot.extracurricularReports, selectedPeriod) {
    monthlyReportSnapshot.extracurricularReports
      .filter { it.period == selectedPeriod }
      .groupBy { it.studentId }
  }
  val showSkeleton = students.isEmpty() && (isRefreshing || waliSantriSnapshot.updatedAt <= 0L)

  BackHandler(enabled = selectedSantri != null) {
    selectedSantriId = null
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
  ) { innerPadding ->
    AnimatedContent(
      targetState = selectedSantri,
      transitionSpec = {
        val openingDetail = targetState != null
        fadeIn() + slideInHorizontally { width -> if (openingDetail) width / 5 else -width / 5 } togetherWith
          fadeOut() + slideOutHorizontally { width -> if (openingDetail) -width / 6 else width / 6 }
      },
      label = "laporan-bulanan-content",
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
          LaporanBulananListContent(
            isWaliKelas = waliSantriSnapshot.isWaliKelas,
            students = students,
            reportsByStudent = reportsByStudent,
            attendanceByStudent = attendanceByStudent,
            extracurricularByStudent = extracurricularByStudent,
            guruId = monthlyReportSnapshot.guruId,
            waliName = profile.name,
            waliPhone = profile.phoneNumber,
            periods = periods,
            selectedPeriod = selectedPeriod,
            onPeriodChange = { selectedPeriod = it },
            selectedQuickActionSantriId = quickActionSantriId,
            onQuickActionSantriChange = { quickActionSantriId = it },
            showSkeleton = showSkeleton,
            missingTable = monthlyReportSnapshot.missingTable,
            missingExtendedColumns = monthlyReportSnapshot.missingExtendedColumns,
            onMenuClick = onMenuClick,
            showMessage = { message ->
              snackbarScope.launch { snackbarHostState.showSnackbar(message) }
            },
            onSaveReport = onSaveReport,
            onSaveExtracurricularReports = onSaveExtracurricularReports,
            onSantriClick = { selectedSantriId = it.id }
          )
        }
      } else {
        LaporanBulananDetailContent(
          santri = activeSantri,
          report = reportsByStudent[activeSantri.id],
          attendanceSummary = attendanceByStudent[activeSantri.id],
          extracurricularReports = extracurricularByStudent[activeSantri.id].orEmpty(),
          guruId = monthlyReportSnapshot.guruId,
          waliName = profile.name,
          waliPhone = profile.phoneNumber,
          period = selectedPeriod,
          isSaving = isSaving,
          onBackClick = { selectedSantriId = null },
          showMessage = { message ->
            snackbarScope.launch { snackbarHostState.showSnackbar(message) }
          },
          onSaveReport = { draft ->
            isSaving = true
            val result = onSaveReport(draft)
            isSaving = false
            snackbarScope.launch {
              snackbarHostState.showSnackbar(result.message)
            }
            result
          }
        )
      }
    }
  }
}

@Composable
private fun LaporanBulananListContent(
  isWaliKelas: Boolean,
  students: List<WaliSantriProfile>,
  reportsByStudent: Map<String, MonthlyReportItem>,
  attendanceByStudent: Map<String, MonthlyAttendanceSummary>,
  extracurricularByStudent: Map<String, List<MonthlyExtracurricularReport>>,
  guruId: String,
  waliName: String,
  waliPhone: String,
  periods: List<String>,
  selectedPeriod: String,
  onPeriodChange: (String) -> Unit,
  selectedQuickActionSantriId: String?,
  onQuickActionSantriChange: (String?) -> Unit,
  showSkeleton: Boolean,
  missingTable: Boolean,
  missingExtendedColumns: Boolean,
  onMenuClick: () -> Unit,
  showMessage: (String) -> Unit,
  onSaveReport: suspend (MonthlyReportItem) -> MonthlyReportSaveOutcome,
  onSaveExtracurricularReports: suspend (List<MonthlyExtracurricularReport>) -> MonthlyExtracurricularSaveOutcome,
  onSantriClick: (WaliSantriProfile) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var templateText by rememberSaveable { mutableStateOf(defaultMonthlyWhatsappTemplate()) }
  var isTemplateEditing by rememberSaveable { mutableStateOf(false) }
  var isExporting by rememberSaveable { mutableStateOf(false) }
  var isWaDialogOpen by rememberSaveable { mutableStateOf(false) }
  var waTargetNumber by rememberSaveable { mutableStateOf("") }
  var isImportDialogOpen by rememberSaveable { mutableStateOf(false) }
  val studentsById = remember(students) { students.associateBy { it.id } }
  val systemResetReportDrafts = remember(reportsByStudent, studentsById, selectedPeriod, guruId) {
    reportsByStudent.values
      .filter { it.hasMonthlyOverrideValues() }
      .map { report ->
        report.toSystemResetReport(
          period = selectedPeriod,
          guruId = guruId,
          classId = studentsById[report.studentId]?.classId.orEmpty()
        )
      }
  }
  val systemResetExtracurricularDrafts = remember(extracurricularByStudent) {
    extracurricularByStudent.values
      .flatten()
      .filter { it.hasMonthlyOverride }
      .map { it.copy(attendanceLabel = "", note = "", hasMonthlyOverride = false) }
  }
  val quickActionSantri = students.firstOrNull { it.id == selectedQuickActionSantriId }
  val quickActionExportData = remember(
    quickActionSantri,
    selectedPeriod,
    reportsByStudent,
    attendanceByStudent,
    extracurricularByStudent,
    guruId,
    waliName,
    waliPhone
  ) {
    quickActionSantri?.let { santri ->
      val report = reportsByStudent[santri.id] ?: MonthlyReportItem(
        period = selectedPeriod,
        guruId = guruId,
        classId = santri.classId,
        studentId = santri.id
      )
      buildMonthlyReportExportData(
        santri = santri,
        period = selectedPeriod,
        report = report.withAutomaticGradeDescriptions(),
        attendanceSummary = attendanceByStudent[santri.id],
        extracurricularReports = extracurricularByStudent[santri.id].orEmpty(),
        waliName = waliName,
        waliPhone = waliPhone
      )
    }
  }
  Box(
    modifier = Modifier
      .fillMaxSize()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    LaporanBulananTopBar(
      title = "Laporan Bulanan",
      leadingIcon = Icons.Outlined.Menu,
      onLeadingClick = onMenuClick,
      leadingContentDescription = "Buka sidebar"
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(bottom = if (quickActionExportData == null) 82.dp else 96.dp)
    ) {
      item {
        MonthlyTemplateCard(
          template = templateText,
          isEditing = isTemplateEditing,
          onTemplateChange = { templateText = it },
          onToggleEdit = { isTemplateEditing = !isTemplateEditing },
          onReset = {
            templateText = defaultMonthlyWhatsappTemplate()
            isTemplateEditing = false
          }
        )
      }

      item {
        PeriodSelectorCard(
          periods = periods,
          selectedPeriod = selectedPeriod,
          onPeriodChange = onPeriodChange
        )
      }

      item {
        MonthlyBulkImportCard(
          enabled = isWaliKelas && !showSkeleton,
          onClick = {
            onQuickActionSantriChange(null)
            isImportDialogOpen = true
          }
        )
      }

      if (systemResetReportDrafts.isNotEmpty() || systemResetExtracurricularDrafts.isNotEmpty()) {
        item {
          MonthlyBulkResetCard(
            count = systemResetReportDrafts.size + systemResetExtracurricularDrafts.size,
            isBusy = isExporting,
            onClick = {
              scope.launch {
                isExporting = true
                var successCount = 0
                var failedMessage = ""
                systemResetReportDrafts.forEach { draft ->
                  val result = onSaveReport(draft)
                  if (result.success) {
                    successCount += 1
                  } else if (failedMessage.isBlank()) {
                    failedMessage = result.message
                  }
                }
                if (failedMessage.isBlank() && systemResetExtracurricularDrafts.isNotEmpty()) {
                  val result = onSaveExtracurricularReports(systemResetExtracurricularDrafts)
                  if (result.success) {
                    successCount += result.reports.size
                  } else {
                    failedMessage = result.message
                  }
                }
                isExporting = false
                if (failedMessage.isBlank()) {
                  showMessage("Reset periode berhasil untuk $successCount data laporan.")
                } else {
                  showMessage("Sebagian reset periode gagal: $failedMessage")
                }
              }
            }
          )
        }
      }

      if (missingTable) {
        item {
          InfoWarningCard("Tabel laporan_bulanan_wali belum tersedia atau belum bisa diakses.")
        }
      } else if (missingExtendedColumns) {
        item {
          InfoWarningCard("Kolom detail ketahfizan/kesantrian belum lengkap. Data dasar wali kelas tetap ditampilkan.")
        }
      }

      when {
        showSkeleton -> {
          items(7) { index ->
            LaporanBulananSkeletonCard(index = index)
          }
        }

        students.isEmpty() -> {
          item {
            EmptyLaporanCard(
              message = if (isWaliKelas) {
                "Belum ada santri aktif pada kelas wali ini."
              } else {
                "Menu laporan bulanan hanya tersedia untuk wali kelas. Tarik ke bawah untuk refresh data."
              }
            )
          }
        }

        else -> {
          items(students, key = { it.id }) { student ->
            LaporanBulananStudentCard(
              santri = student,
              report = reportsByStudent[student.id],
              selectedForQuickAction = student.id == selectedQuickActionSantriId,
              onClick = {
                onQuickActionSantriChange(null)
                onSantriClick(student)
              },
              onLongClick = {
                onQuickActionSantriChange(
                  if (student.id == selectedQuickActionSantriId) null else student.id
                )
              }
            )
          }
        }
      }
      }
    }
    if (isImportDialogOpen) {
      MonthlyBulkImportDialog(
        students = students,
        reportsByStudent = reportsByStudent,
        attendanceByStudent = attendanceByStudent,
        extracurricularByStudent = extracurricularByStudent,
        period = selectedPeriod,
        guruId = guruId,
        waliName = waliName,
        waliPhone = waliPhone,
        onDismiss = { if (!isExporting) isImportDialogOpen = false },
        onApplyRows = { rows ->
          isExporting = true
          var successCount = 0
          var failedMessage = ""
          rows.filter { it.reportChanged }.forEach { row ->
            val report = row.report
            val result = onSaveReport(report)
            if (result.success) {
              successCount += 1
            } else if (failedMessage.isBlank()) {
              failedMessage = result.message
            }
          }
          val extracurricularDrafts = rows.flatMap { it.extracurricularReports }
          if (failedMessage.isBlank() && extracurricularDrafts.isNotEmpty()) {
            val result = onSaveExtracurricularReports(extracurricularDrafts)
            if (result.success) {
              successCount += result.reports.size
            } else {
              failedMessage = result.message
            }
          }
          isExporting = false
          if (failedMessage.isBlank()) {
            isImportDialogOpen = false
            showMessage("Import massal berhasil untuk $successCount santri.")
          } else {
            showMessage("Sebagian import gagal: $failedMessage")
          }
        }
      )
    }
    quickActionExportData?.let { exportData ->
      MonthlyReportActionBar(
        isBusy = isExporting,
        onPrintClick = {
          scope.launch {
            isExporting = true
            runCatching {
              val pdfFile = MonthlyReportExporter.createPdfFile(context, exportData)
              MonthlyReportExporter.printPdf(context, pdfFile, exportData)
            }.onFailure { error ->
              showMessage("Gagal menyiapkan cetak: ${error.message ?: "Unknown error"}")
            }
            isExporting = false
          }
        },
        onSendClick = {
          waTargetNumber = exportData.waTargets.firstOrNull()?.phone.orEmpty()
          isWaDialogOpen = true
        },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(horizontal = 18.dp, vertical = 10.dp)
      )
      if (isWaDialogOpen) {
        WhatsAppTargetDialog(
          data = exportData,
          targetNumber = waTargetNumber,
          onTargetNumberChange = { waTargetNumber = it },
          onDismiss = { if (!isExporting) isWaDialogOpen = false },
          onSend = {
            val phone = waTargetNumber
            scope.launch {
              isExporting = true
              runCatching {
                val normalizedPhone = MonthlyReportExporter.normalizeWhatsappNumber(phone)
                if (normalizedPhone.isBlank()) {
                  error("Nomor WhatsApp tujuan belum valid.")
                }
                val pdfFile = MonthlyReportExporter.createPdfFile(context, exportData)
                val message = MonthlyReportExporter.buildWhatsappAttachmentMessage(exportData)
                if (!MonthlyReportExporter.sharePdfToWhatsApp(context, pdfFile, normalizedPhone, message)) {
                  error("Tidak bisa membuka WhatsApp dengan lampiran PDF.")
                }
                isWaDialogOpen = false
                showMessage("WhatsApp dibuka. Jika caption belum muncul, teks sudah disalin dan bisa ditempel.")
              }.onFailure { error ->
                showMessage("Gagal kirim WhatsApp: ${error.message ?: "Unknown error"}")
              }
              isExporting = false
            }
          }
        )
      }
    }
  }
}

@Composable
private fun LaporanBulananDetailContent(
  santri: WaliSantriProfile,
  report: MonthlyReportItem?,
  attendanceSummary: MonthlyAttendanceSummary?,
  extracurricularReports: List<MonthlyExtracurricularReport>,
  guruId: String,
  waliName: String,
  waliPhone: String,
  period: String,
  isSaving: Boolean,
  onBackClick: () -> Unit,
  showMessage: (String) -> Unit,
  onSaveReport: suspend (MonthlyReportItem) -> MonthlyReportSaveOutcome
) {
  val context = LocalContext.current
  val baseReport = remember(report, santri.id, santri.classId, period, guruId) {
    report ?: MonthlyReportItem(
      period = period,
      guruId = guruId,
      classId = santri.classId,
      studentId = santri.id
    )
  }
  var isEditing by rememberSaveable(santri.id, period) { mutableStateOf(false) }
  var draftReport by remember(baseReport) { mutableStateOf(baseReport) }
  var isExporting by rememberSaveable { mutableStateOf(false) }
  var isWaDialogOpen by rememberSaveable { mutableStateOf(false) }
  var waTargetNumber by rememberSaveable { mutableStateOf("") }
  val scope = rememberCoroutineScope()
  val isDirty = draftReport != baseReport
  val visibleReport = (if (isEditing) draftReport else baseReport).withAutomaticGradeDescriptions()
  val resetSourceReport = baseReport.toSystemResetReport(
    period = period,
    guruId = guruId,
    classId = santri.classId
  )
  val canResetToSource = baseReport.hasMonthlyOverrideValues()
  val exportData = remember(
    santri,
    period,
    waliName,
    waliPhone,
    visibleReport,
    attendanceSummary,
    extracurricularReports
  ) {
    buildMonthlyReportExportData(
      santri = santri,
      period = period,
      report = visibleReport,
      attendanceSummary = attendanceSummary,
      extracurricularReports = extracurricularReports,
      waliName = waliName,
      waliPhone = waliPhone
    )
  }

  LaunchedEffect(baseReport, isEditing) {
    if (!isEditing) {
      draftReport = baseReport
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    LaporanBulananTopBar(
      title = "Detail Laporan",
      leadingIcon = Icons.AutoMirrored.Outlined.ArrowBack,
      onLeadingClick = onBackClick,
      leadingContentDescription = "Kembali",
      trailingIcon = if (isSaving) null else if (isEditing) Icons.Outlined.CheckCircle else Icons.Outlined.Edit,
      trailingContentDescription = if (isEditing) "Simpan laporan" else "Edit laporan",
      onTrailingClick = {
        if (isSaving) return@LaporanBulananTopBar
        if (!isEditing) {
          isEditing = true
          return@LaporanBulananTopBar
        }
        if (!isDirty) {
          isEditing = false
          return@LaporanBulananTopBar
        }
        val normalizedDraft = draftReport.copy(
          period = period,
          guruId = draftReport.guruId.ifBlank { guruId },
          classId = draftReport.classId.ifBlank { santri.classId },
          studentId = santri.id,
          predikat = akhlakDescriptionFromValue(draftReport.nilaiAkhlak)
        )
        scope.launch {
          val result = onSaveReport(normalizedDraft)
          if (result.success) {
            isEditing = false
          }
        }
      }
    )

    if (isSaving) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(SoftPanel, RoundedCornerShape(18.dp))
          .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
          .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          strokeWidth = 2.dp,
          color = PrimaryBlue
        )
        Text(
          text = "Menyimpan perubahan laporan...",
          style = MaterialTheme.typography.bodySmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.Bold
        )
      }
    }

    if (isEditing || canResetToSource) {
      ReportResetCard(
        isEditing = isEditing,
        canReset = isDirty || canResetToSource,
        onResetClick = {
          if (isEditing) {
            draftReport = resetSourceReport
          } else {
            scope.launch {
              onSaveReport(resetSourceReport)
            }
          }
        }
      )
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(bottom = 96.dp)
    ) {
      item {
        DetailHeaderCard(
          santri = santri,
          period = period,
          report = report
        )
      }

      item {
        DetailSectionCard(
          title = "Wali Kelas",
          subtitle = buildMentorLabel("Wali kelas", waliName, waliPhone)
        ) {
          AspectCard(
            title = "Kehadiran Kelas",
            value = attendanceSummary?.attendancePercent.withPercentSuffix(),
            description = "Sakit ${attendanceSummary?.sakitCount ?: 0} hari, izin ${attendanceSummary?.izinCount ?: 0} hari",
            accentColor = Color(0xFF16A34A)
          )
          if (isEditing) {
            EditableAspectCard(
              title = "Akhlak",
              value = draftReport.nilaiAkhlak,
              onValueChange = { value ->
                sanitizeGradeInput(value)?.let { draftReport = draftReport.copy(nilaiAkhlak = it) }
              },
              description = akhlakDescriptionFromValue(draftReport.nilaiAkhlak).ifBlank { "-" },
              onDescriptionChange = {},
              valueLabel = "Nilai (A-E)",
              descriptionLabel = "Predikat otomatis",
              descriptionReadOnly = true,
              accentColor = Color(0xFF2563EB)
            )
            EditableLongTextCard(
              title = "Catatan Wali Kelas",
              value = draftReport.catatanWali,
              onValueChange = { draftReport = draftReport.copy(catatanWali = it) }
            )
          } else {
            AspectCard(
              title = "Akhlak",
              value = visibleReport.nilaiAkhlak.orDash(),
              description = visibleReport.predikat.orDash(),
              accentColor = Color(0xFF2563EB)
            )
            NoteCard(
              title = "Catatan Wali Kelas",
              note = visibleReport.catatanWali.orDash()
            )
          }
        }
      }

      item {
        DetailSectionCard(
          title = "Ketahfizan",
          subtitle = buildMentorLabel("Muhaffiz", visibleReport.muhaffiz, visibleReport.noHpMuhaffiz)
        ) {
          if (isEditing) {
            EditableMentorFields(
              nameLabel = "Nama Muhaffiz",
              phoneLabel = "Nomor HP Muhaffiz",
              name = draftReport.muhaffiz,
              phone = draftReport.noHpMuhaffiz,
              onNameChange = { draftReport = draftReport.copy(muhaffiz = it) },
              onPhoneChange = { draftReport = draftReport.copy(noHpMuhaffiz = it) }
            )
            AspectCard(
              title = "Kehadiran Halaqah",
              value = visibleReport.nilaiKehadiranHalaqah.withPercentSuffix(),
              description = "Otomatis dari sumber data. Sakit ${visibleReport.sakitHalaqah.orZero()} hari, izin ${visibleReport.izinHalaqah.orZero()} hari",
              accentColor = Color(0xFF16A34A)
            )
            EditableAspectCard(
              title = "Akhlak Halaqah",
              value = draftReport.nilaiAkhlakHalaqah,
              onValueChange = { value ->
                sanitizeGradeInput(value)?.let { draftReport = draftReport.copy(nilaiAkhlakHalaqah = it) }
              },
              description = akhlakDescriptionFromValue(draftReport.nilaiAkhlakHalaqah).ifBlank { "-" },
              onDescriptionChange = {},
              valueLabel = "Nilai (A-E)",
              descriptionLabel = "Keterangan otomatis",
              descriptionReadOnly = true,
              accentColor = Color(0xFF7C3AED)
            )
            EditableAspectCard(
              title = "Ujian Bulanan",
              value = draftReport.nilaiUjianBulanan,
              onValueChange = { draftReport = draftReport.copy(nilaiUjianBulanan = it) },
              description = draftReport.keteranganUjianBulanan,
              onDescriptionChange = { draftReport = draftReport.copy(keteranganUjianBulanan = it) },
              accentColor = Color(0xFF0891B2)
            )
            EditableAspectCard(
              title = "Target Hafalan",
              value = draftReport.nilaiTargetHafalan,
              onValueChange = { draftReport = draftReport.copy(nilaiTargetHafalan = it) },
              description = draftReport.keteranganTargetHafalan,
              onDescriptionChange = { draftReport = draftReport.copy(keteranganTargetHafalan = it) },
              accentColor = Color(0xFFEA580C)
            )
            EditableLongTextCard(
              title = "Capaian Hafalan Bulanan",
              value = draftReport.keteranganCapaianHafalanBulanan.ifBlank { draftReport.nilaiCapaianHafalanBulanan },
              onValueChange = { draftReport = draftReport.copy(keteranganCapaianHafalanBulanan = it) },
              label = "Capaian"
            )
            EditableLongTextCard(
              title = "Jumlah Hafalan",
              value = draftReport.keteranganJumlahHafalanBulanan.ifBlank { buildJumlahHafalanLabel(draftReport) },
              onValueChange = { draftReport = draftReport.copy(keteranganJumlahHafalanBulanan = it) },
              label = "Jumlah"
            )
            EditableLongTextCard(
              title = "Catatan Muhaffiz",
              value = draftReport.catatanMuhaffiz,
              onValueChange = { draftReport = draftReport.copy(catatanMuhaffiz = it) }
            )
          } else {
            AspectCard(
              title = "Kehadiran Halaqah",
              value = visibleReport.nilaiKehadiranHalaqah.withPercentSuffix(),
              description = "Sakit ${visibleReport.sakitHalaqah.orZero()} hari, izin ${visibleReport.izinHalaqah.orZero()} hari",
              accentColor = Color(0xFF16A34A)
            )
            AspectCard(
              title = "Akhlak Halaqah",
              value = visibleReport.nilaiAkhlakHalaqah.orDash(),
              description = visibleReport.keteranganAkhlakHalaqah.orDash(),
              accentColor = Color(0xFF7C3AED)
            )
            AspectCard(
              title = "Ujian Bulanan",
              value = visibleReport.nilaiUjianBulanan.orDash(),
              description = visibleReport.keteranganUjianBulanan.orDash(),
              accentColor = Color(0xFF0891B2)
            )
            AspectCard(
              title = "Target Hafalan",
              value = visibleReport.nilaiTargetHafalan.withPercentSuffix(),
              description = visibleReport.keteranganTargetHafalan.orDash(),
              accentColor = Color(0xFFEA580C)
            )
            AspectCard(
              title = "Capaian Hafalan Bulanan",
              value = "",
              description = buildCapaianHafalanBulananLabel(visibleReport),
              accentColor = Color(0xFF0D9488)
            )
            AspectCard(
              title = "Jumlah Hafalan",
              value = "",
              description = buildJumlahHafalanLabel(visibleReport),
              accentColor = Color(0xFF0D9488)
            )
            NoteCard(
              title = "Catatan Muhaffiz",
              note = visibleReport.catatanMuhaffiz.orDash()
            )
          }
        }
      }

      item {
        DetailSectionCard(
          title = "Kesantrian",
          subtitle = buildMentorLabel("Musyrif", visibleReport.musyrif, visibleReport.noHpMusyrif)
        ) {
          if (isEditing) {
            EditableMentorFields(
              nameLabel = "Nama Musyrif",
              phoneLabel = "Nomor HP Musyrif",
              name = draftReport.musyrif,
              phone = draftReport.noHpMusyrif,
              onNameChange = { draftReport = draftReport.copy(musyrif = it) },
              onPhoneChange = { draftReport = draftReport.copy(noHpMusyrif = it) }
            )
            AspectCard(
              title = "Kehadiran Liqa",
              value = visibleReport.nilaiKehadiranLiqaMuhasabah.withPercentSuffix(),
              description = "Otomatis dari sumber data. Sakit ${visibleReport.sakitLiqaMuhasabah.orZero()} hari, izin ${visibleReport.izinLiqaMuhasabah.orZero()} hari",
              accentColor = Color(0xFF16A34A)
            )
            EditableAspectCard(
              title = "Ibadah",
              value = draftReport.nilaiIbadah,
              onValueChange = { value ->
                sanitizeGradeInput(value)?.let { draftReport = draftReport.copy(nilaiIbadah = it) }
              },
              description = akhlakDescriptionFromValue(draftReport.nilaiIbadah).ifBlank { "-" },
              onDescriptionChange = {},
              valueLabel = "Nilai (A-E)",
              descriptionLabel = "Keterangan otomatis",
              descriptionReadOnly = true,
              accentColor = Color(0xFF2563EB)
            )
            EditableAspectCard(
              title = "Kedisiplinan",
              value = draftReport.nilaiKedisiplinan,
              onValueChange = { value ->
                sanitizeGradeInput(value)?.let { draftReport = draftReport.copy(nilaiKedisiplinan = it) }
              },
              description = akhlakDescriptionFromValue(draftReport.nilaiKedisiplinan).ifBlank { "-" },
              onDescriptionChange = {},
              valueLabel = "Nilai (A-E)",
              descriptionLabel = "Keterangan otomatis",
              descriptionReadOnly = true,
              accentColor = Color(0xFFEA580C)
            )
            EditableAspectCard(
              title = "Kebersihan",
              value = draftReport.nilaiKebersihan,
              onValueChange = { value ->
                sanitizeGradeInput(value)?.let { draftReport = draftReport.copy(nilaiKebersihan = it) }
              },
              description = akhlakDescriptionFromValue(draftReport.nilaiKebersihan).ifBlank { "-" },
              onDescriptionChange = {},
              valueLabel = "Nilai (A-E)",
              descriptionLabel = "Keterangan otomatis",
              descriptionReadOnly = true,
              accentColor = Color(0xFF0D9488)
            )
            EditableAspectCard(
              title = "Adab",
              value = draftReport.nilaiAdab,
              onValueChange = { value ->
                sanitizeGradeInput(value)?.let { draftReport = draftReport.copy(nilaiAdab = it) }
              },
              description = akhlakDescriptionFromValue(draftReport.nilaiAdab).ifBlank { "-" },
              onDescriptionChange = {},
              valueLabel = "Nilai (A-E)",
              descriptionLabel = "Keterangan otomatis",
              descriptionReadOnly = true,
              accentColor = Color(0xFF7C3AED)
            )
            EditableLongTextCard(
              title = "Prestasi",
              value = draftReport.prestasiKesantrian,
              onValueChange = { draftReport = draftReport.copy(prestasiKesantrian = it) }
            )
            EditableLongTextCard(
              title = "Pelanggaran",
              value = draftReport.pelanggaranKesantrian,
              onValueChange = { draftReport = draftReport.copy(pelanggaranKesantrian = it) }
            )
            EditableLongTextCard(
              title = "Catatan Musyrif",
              value = draftReport.catatanMusyrif,
              onValueChange = { draftReport = draftReport.copy(catatanMusyrif = it) }
            )
          } else {
            AspectCard(
              title = "Kehadiran Liqa",
              value = visibleReport.nilaiKehadiranLiqaMuhasabah.withPercentSuffix(),
              description = "Sakit ${visibleReport.sakitLiqaMuhasabah.orZero()} hari, izin ${visibleReport.izinLiqaMuhasabah.orZero()} hari",
              accentColor = Color(0xFF16A34A)
            )
            CompactAspectGrid(
              items = listOf(
                AspectSummary("Ibadah", visibleReport.nilaiIbadah.orDash(), visibleReport.keteranganIbadah.orDash(), Color(0xFF2563EB)),
                AspectSummary("Kedisiplinan", visibleReport.nilaiKedisiplinan.orDash(), visibleReport.keteranganKedisiplinan.orDash(), Color(0xFFEA580C)),
                AspectSummary("Kebersihan", visibleReport.nilaiKebersihan.orDash(), visibleReport.keteranganKebersihan.orDash(), Color(0xFF0D9488)),
                AspectSummary("Adab", visibleReport.nilaiAdab.orDash(), visibleReport.keteranganAdab.orDash(), Color(0xFF7C3AED))
              )
            )
            AspectCard(
              title = "Prestasi",
              value = "",
              description = visibleReport.prestasiKesantrian.orDash(),
              accentColor = Color(0xFF16A34A)
            )
            AspectCard(
              title = "Pelanggaran",
              value = "",
              description = visibleReport.pelanggaranKesantrian.orDash(),
              accentColor = Color(0xFFDC2626)
            )
            NoteCard(
              title = "Catatan Musyrif",
              note = visibleReport.catatanMusyrif.orDash()
            )
          }
        }
      }

      item {
        DetailSectionCard(
          title = "Ekstrakurikuler",
          subtitle = "Laporan kegiatan tambahan bulan ini"
        ) {
          if (extracurricularReports.isEmpty()) {
            NoteCard(
              title = "Belum ada laporan ekstrakurikuler",
              note = "Santri belum tercatat sebagai anggota ekskul atau laporan ekskul bulan ini belum tersedia."
            )
          } else {
            extracurricularReports.forEach { item ->
              ExtracurricularReportCard(item)
            }
          }
        }
      }
    }
    }

    MonthlyReportActionBar(
      isBusy = isExporting,
      onPrintClick = {
        scope.launch {
          isExporting = true
          runCatching {
            val pdfFile = MonthlyReportExporter.createPdfFile(context, exportData)
            MonthlyReportExporter.printPdf(context, pdfFile, exportData)
          }.onFailure { error ->
            showMessage("Gagal menyiapkan cetak: ${error.message ?: "Unknown error"}")
          }
          isExporting = false
        }
      },
      onSendClick = {
        waTargetNumber = exportData.waTargets.firstOrNull()?.phone.orEmpty()
        isWaDialogOpen = true
      },
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(horizontal = 18.dp, vertical = 10.dp)
    )

    if (isWaDialogOpen) {
      WhatsAppTargetDialog(
        data = exportData,
        targetNumber = waTargetNumber,
        onTargetNumberChange = { waTargetNumber = it },
        onDismiss = { if (!isExporting) isWaDialogOpen = false },
        onSend = {
          val phone = waTargetNumber
          scope.launch {
            isExporting = true
            runCatching {
              val normalizedPhone = MonthlyReportExporter.normalizeWhatsappNumber(phone)
              if (normalizedPhone.isBlank()) {
                error("Nomor WhatsApp tujuan belum valid.")
              }
              val pdfFile = MonthlyReportExporter.createPdfFile(context, exportData)
              val message = MonthlyReportExporter.buildWhatsappAttachmentMessage(exportData)
              if (!MonthlyReportExporter.sharePdfToWhatsApp(context, pdfFile, normalizedPhone, message)) {
                error("Tidak bisa membuka WhatsApp dengan lampiran PDF.")
              }
              isWaDialogOpen = false
              showMessage("WhatsApp dibuka. Jika caption belum muncul, teks sudah disalin dan bisa ditempel.")
            }.onFailure { error ->
              showMessage("Gagal kirim WhatsApp: ${error.message ?: "Unknown error"}")
            }
            isExporting = false
          }
        }
      )
    }
  }
}

@Composable
private fun MonthlyReportActionBar(
  isBusy: Boolean,
  onPrintClick: () -> Unit,
  onSendClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
  ) {
    Row(
      modifier = Modifier
        .width(244.dp)
        .shadow(18.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x180F172A), spotColor = Color(0x180F172A))
        .clip(RoundedCornerShape(32.dp))
        .background(Color.White.copy(alpha = 0.96f))
        .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(32.dp))
        .padding(horizontal = 10.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      ReportActionItem(
        label = "Cetak",
        icon = Icons.Outlined.Print,
        enabled = !isBusy,
        onClick = onPrintClick,
        modifier = Modifier.weight(1f)
      )
      ReportActionItem(
        label = "Kirim",
        icon = Icons.AutoMirrored.Outlined.Send,
        enabled = !isBusy,
        onClick = onSendClick,
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
private fun ReportActionItem(
  label: String,
  icon: ImageVector,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .height(52.dp)
      .clip(RoundedCornerShape(24.dp))
      .background(PrimaryBlue.copy(alpha = if (enabled) 0.12f else 0.05f))
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (enabled) PrimaryBlue else SubtleInk.copy(alpha = 0.5f),
      modifier = Modifier.size(20.dp)
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (enabled) PrimaryBlueDark else SubtleInk.copy(alpha = 0.5f),
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(start = 8.dp)
    )
  }
}

@Composable
private fun WhatsAppTargetDialog(
  data: MonthlyReportExportData,
  targetNumber: String,
  onTargetNumberChange: (String) -> Unit,
  onDismiss: () -> Unit,
  onSend: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Kirim WhatsApp",
        style = MaterialTheme.typography.titleMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Pilih nomor tujuan atau isi manual. PDF laporan akan dibuka sebagai lampiran WhatsApp, dan caption otomatis disalin sebagai cadangan.",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
        data.waTargets.forEach { target ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(Color.White.copy(alpha = 0.82f))
              .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
              .clickable { onTargetNumberChange(target.phone) }
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = target.label,
              style = MaterialTheme.typography.labelLarge,
              color = PrimaryBlueDark,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = target.phone,
              style = MaterialTheme.typography.bodySmall,
              color = SubtleInk,
              textAlign = TextAlign.End,
              modifier = Modifier.padding(start = 10.dp)
            )
          }
        }
        EditTextField(
          value = targetNumber,
          onValueChange = onTargetNumberChange,
          label = "Nomor WhatsApp",
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(onClick = onSend) {
        Text("Kirim")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Batal")
      }
    },
    containerColor = CardBackground
  )
}

@Composable
private fun ReportResetCard(
  isEditing: Boolean,
  canReset: Boolean,
  onResetClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFFEFF6FF), RoundedCornerShape(18.dp))
      .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(18.dp))
      .padding(12.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Text(
        text = if (isEditing) "Mode edit laporan" else "Laporan sudah diedit",
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = if (isEditing) {
          "Perubahan hanya untuk laporan. Reset mengosongkan edit/import agar kembali memakai data asli sistem."
        } else {
          "Gunakan reset untuk mengembalikan laporan ke data asli sistem pada periode ini."
        },
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(if (canReset) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.58f))
        .border(1.dp, CardBorder, CircleShape)
        .clickable(enabled = canReset, onClick = onResetClick),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Outlined.RestartAlt,
        contentDescription = "Reset ke data asli sistem",
        tint = if (canReset) PrimaryBlueDark else SubtleInk.copy(alpha = 0.55f),
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

@Composable
private fun LaporanBulananTopBar(
  title: String,
  leadingIcon: ImageVector,
  onLeadingClick: () -> Unit,
  leadingContentDescription: String,
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
    Box(
      modifier = Modifier
        .size(42.dp)
        .shadow(10.dp, CircleShape, clip = false)
        .clip(CircleShape)
        .background(CardBackground.copy(alpha = 0.94f))
        .border(1.dp, CardBorder, CircleShape)
        .clickable(onClick = onLeadingClick),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = leadingIcon,
        contentDescription = leadingContentDescription,
        tint = PrimaryBlueDark
      )
    }
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 6.dp),
      textAlign = TextAlign.Center
    )
    if (trailingIcon == null) {
      Spacer(modifier = Modifier.size(42.dp))
    } else {
      Box(
        modifier = Modifier
          .size(42.dp)
          .shadow(10.dp, CircleShape, clip = false)
          .clip(CircleShape)
          .background(CardBackground.copy(alpha = 0.94f))
          .border(1.dp, CardBorder, CircleShape)
          .clickable(onClick = onTrailingClick),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = trailingIcon,
          contentDescription = trailingContentDescription,
          tint = PrimaryBlueDark
        )
      }
    }
  }
}

@Composable
private fun EditableMentorFields(
  nameLabel: String,
  phoneLabel: String,
  name: String,
  phone: String,
  onNameChange: (String) -> Unit,
  onPhoneChange: (String) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
      .border(1.dp, CardBorder.copy(alpha = 0.74f), RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    EditTextField(
      value = name,
      onValueChange = onNameChange,
      label = nameLabel,
      modifier = Modifier.fillMaxWidth()
    )
    EditTextField(
      value = phone,
      onValueChange = onPhoneChange,
      label = phoneLabel,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
private fun EditableAspectCard(
  title: String,
  value: String,
  onValueChange: (String) -> Unit,
  description: String,
  onDescriptionChange: (String) -> Unit,
  accentColor: Color,
  valueLabel: String = "Nilai",
  descriptionLabel: String = "Keterangan",
  descriptionReadOnly: Boolean = false
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
      .border(1.dp, CardBorder.copy(alpha = 0.74f), RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(width = 4.dp, height = 20.dp)
          .background(accentColor, RoundedCornerShape(999.dp))
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      EditTextField(
        value = value,
        onValueChange = onValueChange,
        label = valueLabel,
        modifier = Modifier.weight(0.82f)
      )
      EditTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = descriptionLabel,
        readOnly = descriptionReadOnly,
        modifier = Modifier.weight(1.18f)
      )
    }
  }
}

@Composable
private fun EditableAttendanceAspectCard(
  title: String,
  value: String,
  sakit: String,
  izin: String,
  onValueChange: (String) -> Unit,
  onSakitChange: (String) -> Unit,
  onIzinChange: (String) -> Unit,
  accentColor: Color
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
      .border(1.dp, CardBorder.copy(alpha = 0.74f), RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(width = 4.dp, height = 20.dp)
          .background(accentColor, RoundedCornerShape(999.dp))
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      EditTextField(
        value = value,
        onValueChange = onValueChange,
        label = "Persen",
        modifier = Modifier.weight(1f)
      )
      EditTextField(
        value = sakit,
        onValueChange = onSakitChange,
        label = "Sakit",
        modifier = Modifier.weight(1f)
      )
      EditTextField(
        value = izin,
        onValueChange = onIzinChange,
        label = "Izin",
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
private fun EditableLongTextCard(
  title: String,
  value: String,
  onValueChange: (String) -> Unit,
  label: String = "Keterangan"
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
      .border(1.dp, CardBorder.copy(alpha = 0.74f), RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    EditTextField(
      value = value,
      onValueChange = onValueChange,
      label = label,
      minLines = 2,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
private fun EditTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  minLines: Int = 1,
  readOnly: Boolean = false
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    readOnly = readOnly,
    minLines = minLines,
    maxLines = if (minLines > 1) 5 else 1,
    label = { Text(label) },
    shape = RoundedCornerShape(14.dp),
    colors = TextFieldDefaults.colors(
      focusedContainerColor = Color.White.copy(alpha = 0.94f),
      unfocusedContainerColor = Color.White.copy(alpha = 0.82f),
      focusedIndicatorColor = PrimaryBlue,
      unfocusedIndicatorColor = CardBorder,
      focusedTextColor = PrimaryBlueDark,
      unfocusedTextColor = PrimaryBlueDark,
      cursorColor = PrimaryBlue
    ),
    textStyle = MaterialTheme.typography.bodySmall,
    modifier = modifier
  )
}

@Composable
private fun MonthlyTemplateCard(
  template: String,
  isEditing: Boolean,
  onTemplateChange: (String) -> Unit,
  onToggleEdit: () -> Unit,
  onReset: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground.copy(alpha = 0.94f), RoundedCornerShape(24.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .background(PrimaryBlue.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.Message,
          contentDescription = null,
          tint = PrimaryBlueDark
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Template Pesan",
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = "Gunakan placeholder <nama santri> dan <link>.",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
      IconActionButton(
        icon = if (isEditing) Icons.Outlined.CheckCircle else Icons.Outlined.Edit,
        contentDescription = if (isEditing) "Selesai edit template" else "Edit template",
        onClick = onToggleEdit
      )
      IconActionButton(
        icon = Icons.Outlined.RestartAlt,
        contentDescription = "Reset template",
        onClick = onReset
      )
    }

    OutlinedTextField(
      value = template,
      onValueChange = onTemplateChange,
      readOnly = !isEditing,
      minLines = if (isEditing) 6 else 4,
      maxLines = if (isEditing) 9 else 5,
      shape = RoundedCornerShape(18.dp),
      colors = TextFieldDefaults.colors(
        focusedContainerColor = Color.White.copy(alpha = 0.94f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.76f),
        disabledContainerColor = Color.White.copy(alpha = 0.76f),
        focusedIndicatorColor = PrimaryBlue,
        unfocusedIndicatorColor = CardBorder,
        focusedTextColor = PrimaryBlueDark,
        unfocusedTextColor = PrimaryBlueDark,
        cursorColor = PrimaryBlue
      ),
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
private fun PeriodSelectorCard(
  periods: List<String>,
  selectedPeriod: String,
  onPeriodChange: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(SoftPanel, RoundedCornerShape(22.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = "Periode",
      style = MaterialTheme.typography.labelLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.Bold
    )
    Box {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(Color.White.copy(alpha = 0.88f))
          .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
          .clickable { expanded = true }
          .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = selectedPeriod.toPeriodLabel(),
          style = MaterialTheme.typography.bodyMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.Bold
        )
        Icon(
          imageVector = Icons.Outlined.KeyboardArrowDown,
          contentDescription = null,
          tint = SubtleInk
        )
      }
      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
      ) {
        periods.forEach { period ->
          DropdownMenuItem(
            text = { Text(period.toPeriodLabel()) },
            onClick = {
              onPeriodChange(period)
              expanded = false
            }
          )
        }
      }
    }
    Text(
      text = "Bulan berjalan belum tersedia sampai bulan tersebut selesai.",
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
  }
}

@Composable
private fun MonthlyBulkImportCard(
  enabled: Boolean,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFFEFF6FF), RoundedCornerShape(22.dp))
      .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(22.dp))
      .clip(RoundedCornerShape(22.dp))
      .clickable(enabled = enabled, onClick = onClick)
      .padding(14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .background(Color.White.copy(alpha = 0.86f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Outlined.UploadFile,
        contentDescription = null,
        tint = if (enabled) PrimaryBlue else SubtleInk.copy(alpha = 0.55f)
      )
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(
        text = "Import Massal dari Spreadsheet",
        style = MaterialTheme.typography.titleSmall,
        color = if (enabled) PrimaryBlueDark else SubtleInk.copy(alpha = 0.62f),
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = "Masukkan link Google Sheet public, cek preview, lalu terapkan data yang cocok.",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
  }
}

@Composable
private fun MonthlyBulkResetCard(
  count: Int,
  isBusy: Boolean,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFFFFF7ED), RoundedCornerShape(22.dp))
      .border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(22.dp))
      .clip(RoundedCornerShape(22.dp))
      .clickable(enabled = !isBusy, onClick = onClick)
      .padding(14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .background(Color.White.copy(alpha = 0.86f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      if (isBusy) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFFEA580C))
      } else {
        Icon(
          imageVector = Icons.Outlined.RestartAlt,
          contentDescription = null,
          tint = Color(0xFFEA580C)
        )
      }
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(
        text = "Reset Data Periode Ini",
        style = MaterialTheme.typography.titleSmall,
        color = Color(0xFF9A3412),
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = "Kosongkan $count data edit/import agar laporan kembali memakai data asli sistem.",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
  }
}

@Composable
private fun MonthlyBulkImportDialog(
  students: List<WaliSantriProfile>,
  reportsByStudent: Map<String, MonthlyReportItem>,
  attendanceByStudent: Map<String, MonthlyAttendanceSummary>,
  extracurricularByStudent: Map<String, List<MonthlyExtracurricularReport>>,
  period: String,
  guruId: String,
  waliName: String,
  waliPhone: String,
  onDismiss: () -> Unit,
  onApplyRows: suspend (List<MonthlyBulkImportPreviewRow>) -> Unit
) {
  val scope = rememberCoroutineScope()
  var spreadsheetUrl by rememberSaveable { mutableStateOf("") }
  var isLoading by rememberSaveable { mutableStateOf(false) }
  var errorMessage by rememberSaveable { mutableStateOf("") }
  var preview by remember { mutableStateOf<MonthlyBulkImportPreview?>(null) }
  val applicableRows = preview?.rows.orEmpty().filter { it.canApply }

  AlertDialog(
    onDismissRequest = { if (!isLoading) onDismiss() },
    title = {
      Text(
        text = "Import Massal",
        style = MaterialTheme.typography.titleMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Gunakan Google Sheet public/shareable. Sistem akan mencoba membaca tab sesuai kelas wali, lalu mencocokkan santri dari NISN atau nama.",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
        EditTextField(
          value = spreadsheetUrl,
          onValueChange = {
            spreadsheetUrl = it
            preview = null
            errorMessage = ""
          },
          label = "Link Spreadsheet",
          modifier = Modifier.fillMaxWidth()
        )
        if (isLoading) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryBlue)
            Text("Memproses spreadsheet...", style = MaterialTheme.typography.bodySmall, color = SubtleInk)
          }
        }
        if (errorMessage.isNotBlank()) {
          InfoWarningCard(errorMessage)
        }
        preview?.let { data ->
          MonthlyBulkPreviewSummary(preview = data)
          LazyColumn(
            modifier = Modifier.heightIn(max = 290.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(data.rows.take(30), key = { it.rowNumber }) { row ->
              MonthlyBulkPreviewRowCard(
                row = row,
                onAccept = {
                  preview = preview?.copy(
                    rows = preview?.rows.orEmpty().map { item ->
                      if (item.rowNumber == row.rowNumber) {
                        item.copy(
                          status = MonthlyBulkImportStatus.Ready,
                          message = "Disetujui manual. ${item.message}"
                        )
                      } else {
                        item
                      }
                    }
                  )
                },
                onReject = {
                  preview = preview?.copy(
                    rows = preview?.rows.orEmpty().map { item ->
                      if (item.rowNumber == row.rowNumber) {
                        item.copy(
                          status = MonthlyBulkImportStatus.NotFound,
                          message = "Ditolak manual. Data baris ini tidak akan diterapkan.",
                          changedFields = emptyList()
                        )
                      } else {
                        item
                      }
                    }
                  )
                }
              )
            }
            if (data.rows.size > 30) {
              item {
                Text(
                  text = "Menampilkan 30 dari ${data.rows.size} baris.",
                  style = MaterialTheme.typography.bodySmall,
                  color = SubtleInk
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        enabled = !isLoading && applicableRows.isNotEmpty(),
        onClick = {
          scope.launch {
            isLoading = true
            onApplyRows(applicableRows)
            isLoading = false
          }
        }
      ) {
        Text("Terapkan ${applicableRows.size}")
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
          enabled = !isLoading,
          onClick = {
            scope.launch {
              isLoading = true
              errorMessage = ""
              preview = null
              runCatching {
                buildMonthlyBulkImportPreview(
                  spreadsheetUrl = spreadsheetUrl,
                  students = students,
                  reportsByStudent = reportsByStudent,
                  attendanceByStudent = attendanceByStudent,
                  extracurricularByStudent = extracurricularByStudent,
                  period = period,
                  guruId = guruId,
                  waliName = waliName,
                  waliPhone = waliPhone
                )
              }.onSuccess {
                preview = it
              }.onFailure { error ->
                errorMessage = error.message ?: "Gagal membaca spreadsheet."
              }
              isLoading = false
            }
          }
        ) {
          Text("Preview")
        }
        TextButton(enabled = !isLoading, onClick = onDismiss) {
          Text("Batal")
        }
      }
    },
    containerColor = CardBackground
  )
}

@Composable
private fun MonthlyBulkPreviewSummary(preview: MonthlyBulkImportPreview) {
  val matched = preview.rows.count { it.canApply }
  val needsCheck = preview.rows.count { it.status == MonthlyBulkImportStatus.NeedsReview }
  val failed = preview.rows.count { it.status == MonthlyBulkImportStatus.NotFound || it.status == MonthlyBulkImportStatus.Duplicate }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White.copy(alpha = 0.76f), RoundedCornerShape(16.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(
      text = "Preview ${preview.sheetLabel}",
      style = MaterialTheme.typography.labelLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "Cocok: $matched | Perlu cek: $needsCheck | Tidak siap: $failed",
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
    if (preview.unknownColumns.isNotEmpty()) {
      Text(
        text = "Kolom belum dikenali: ${preview.unknownColumns.take(5).joinToString(", ")}",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFB45309)
      )
    }
  }
}

@Composable
private fun MonthlyBulkPreviewRowCard(
  row: MonthlyBulkImportPreviewRow,
  onAccept: () -> Unit,
  onReject: () -> Unit
) {
  val color = when (row.status) {
    MonthlyBulkImportStatus.Ready -> Color(0xFF16A34A)
    MonthlyBulkImportStatus.NeedsReview -> Color(0xFFEA580C)
    MonthlyBulkImportStatus.Duplicate,
    MonthlyBulkImportStatus.NotFound -> Color(0xFFDC2626)
    MonthlyBulkImportStatus.NoChanges -> SubtleInk
  }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(16.dp))
      .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
      .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp)
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
      Text(
        text = row.displayName,
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f)
      )
      Text(
        text = row.status.label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold
      )
    }
    Text(
      text = row.message,
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      maxLines = 3,
      overflow = TextOverflow.Ellipsis
    )
    if (row.changedFields.isNotEmpty()) {
      Text(
        text = "Diisi: ${row.changedFields.take(4).joinToString(", ")}",
        style = MaterialTheme.typography.bodySmall,
        color = PrimaryBlueDark,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
    if (row.status == MonthlyBulkImportStatus.NeedsReview && row.changedFields.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(onClick = onReject) {
          Text("Tolak", color = Color(0xFFDC2626))
        }
        TextButton(onClick = onAccept) {
          Text("Setujui", color = PrimaryBlue, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LaporanBulananStudentCard(
  santri: WaliSantriProfile,
  report: MonthlyReportItem?,
  selectedForQuickAction: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit
) {
  val missingLabels = remember(report) { report.missingLabels() }
  val isComplete = missingLabels.isEmpty()
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(8.dp, RoundedCornerShape(22.dp), clip = false)
      .clip(RoundedCornerShape(22.dp))
      .background(if (selectedForQuickAction) Color(0xFFEFF6FF) else CardBackground.copy(alpha = 0.94f))
      .border(
        width = 1.dp,
        color = if (selectedForQuickAction) PrimaryBlue.copy(alpha = 0.58f) else CardBorder,
        shape = RoundedCornerShape(22.dp)
      )
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
      )
      .padding(14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .background(if (isComplete) Color(0xFFDCFCE7) else Color(0xFFFFF7ED), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = if (isComplete) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
        contentDescription = null,
        tint = if (isComplete) Color(0xFF16A34A) else Color(0xFFEA580C)
      )
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(
        text = santri.name,
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = if (santri.nisn.isBlank()) santri.className.ifBlank { "Kelas belum tersedia" } else "NISN ${santri.nisn}",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = if (isComplete) "Sudah lengkap" else "Belum lengkap: ${missingLabels.joinToString(", ")}",
        style = MaterialTheme.typography.bodySmall,
        color = if (isComplete) Color(0xFF15803D) else Color(0xFFB45309),
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun DetailHeaderCard(
  santri: WaliSantriProfile,
  period: String,
  report: MonthlyReportItem?
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground.copy(alpha = 0.94f), RoundedCornerShape(24.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Box(
      modifier = Modifier
        .size(68.dp)
        .background(PrimaryBlue.copy(alpha = 0.12f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Outlined.Person,
        contentDescription = null,
        tint = PrimaryBlueDark,
        modifier = Modifier.size(34.dp)
      )
    }
    Text(
      text = santri.name,
      style = MaterialTheme.typography.titleLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      textAlign = TextAlign.Center
    )
    Text(
      text = "${santri.className.ifBlank { "Kelas" }} - ${period.toPeriodLabel()}",
      style = MaterialTheme.typography.bodyMedium,
      color = SubtleInk,
      textAlign = TextAlign.Center
    )
    if (report == null) {
      InfoWarningCard("Belum ada data laporan untuk santri ini pada periode terpilih.")
    }
  }
}

@Composable
private fun DetailSectionCard(
  title: String,
  subtitle: String = "",
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground.copy(alpha = 0.94f), RoundedCornerShape(22.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      if (subtitle.isNotBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
    }
    content()
  }
}

@Composable
private fun AspectCard(
  title: String,
  value: String,
  description: String,
  accentColor: Color,
  meta: String = ""
) {
  val valueLabel = value.takeIf { it.isNotBlank() && it != "-" }
  val valueColor = valueLabel?.let { scoreColorFor(it, accentColor) } ?: accentColor
  val descriptionText = description.ifBlank { "-" }
  val metaText = meta.takeIf { it.isNotBlank() && it != "-" }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White.copy(alpha = 0.74f), RoundedCornerShape(16.dp))
      .border(1.dp, CardBorder.copy(alpha = 0.74f), RoundedCornerShape(16.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = if (metaText == null) descriptionText else "$metaText - $descriptionText",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
      )
    }
    if (valueLabel != null) {
      Text(
        text = valueLabel,
        style = MaterialTheme.typography.titleMedium,
        color = valueColor,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(74.dp)
      )
    }
  }
}

@Composable
private fun CompactAspectGrid(items: List<AspectSummary>) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    items.chunked(2).forEach { rowItems ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        rowItems.forEach { item ->
          CompactAspectCard(
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
private fun CompactAspectCard(
  item: AspectSummary,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(Color.White.copy(alpha = 0.74f))
      .border(1.dp, CardBorder.copy(alpha = 0.74f), RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(7.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = item.title,
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.weight(1f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = item.value,
        style = MaterialTheme.typography.titleSmall,
        color = scoreColorFor(item.value, item.color),
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .padding(start = 8.dp)
          .width(38.dp)
      )
    }
    Text(
      text = item.description.ifBlank { "-" },
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      maxLines = 3,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun NoteCard(
  title: String,
  note: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color(0xFFF8FAFC))
      .border(1.dp, CardBorder.copy(alpha = 0.74f), RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = note.ifBlank { "-" },
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
  }
}

@Composable
private fun ExtracurricularReportCard(item: MonthlyExtracurricularReport) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color.White.copy(alpha = 0.74f))
      .border(1.dp, CardBorder.copy(alpha = 0.74f), RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = item.activityName.ifBlank { "-" },
        style = MaterialTheme.typography.bodyMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = "PJ: ${item.pjName.ifBlank { "-" }}",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = "HP: ${item.pjPhone.ifBlank { "-" }}",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
    AspectCard(
      title = "Kehadiran Ekskul",
      value = item.attendanceLabel.ifBlank { "-" },
      description = "Rekap kehadiran kegiatan bulan ini",
      accentColor = Color(0xFF0D9488)
    )
    NoteCard(
      title = "Catatan PJ",
      note = item.note.ifBlank { "-" }
    )
  }
}

@Composable
private fun IconActionButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(38.dp)
      .clip(CircleShape)
      .background(Color.White.copy(alpha = 0.8f))
      .border(1.dp, CardBorder, CircleShape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = PrimaryBlueDark,
      modifier = Modifier.size(20.dp)
    )
  }
}

@Composable
private fun InfoWarningCard(message: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFFFFFBEB), RoundedCornerShape(18.dp))
      .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(18.dp))
      .padding(12.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Outlined.WarningAmber,
      contentDescription = null,
      tint = Color(0xFFB45309)
    )
    Text(
      text = message,
      style = MaterialTheme.typography.bodySmall,
      color = PrimaryBlueDark,
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun EmptyLaporanCard(message: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground.copy(alpha = 0.9f), RoundedCornerShape(22.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .padding(20.dp),
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
private fun LaporanBulananSkeletonCard(index: Int) {
  val alpha = 0.55f + (index % 3) * 0.08f
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground.copy(alpha = 0.9f), RoundedCornerShape(22.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .padding(14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .background(Color(0xFFE2E8F0).copy(alpha = alpha), CircleShape)
    )
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Box(
        modifier = Modifier
          .fillMaxWidth(0.62f)
          .height(13.dp)
          .background(Color(0xFFE2E8F0).copy(alpha = alpha), RoundedCornerShape(999.dp))
      )
      Box(
        modifier = Modifier
          .fillMaxWidth(0.42f)
          .height(10.dp)
          .background(Color(0xFFE2E8F0).copy(alpha = alpha), RoundedCornerShape(999.dp))
      )
      Box(
        modifier = Modifier
          .fillMaxWidth(0.74f)
          .height(10.dp)
          .background(Color(0xFFE2E8F0).copy(alpha = alpha), RoundedCornerShape(999.dp))
      )
    }
  }
}

private enum class MonthlyBulkImportStatus(val label: String) {
  Ready("Cocok"),
  NeedsReview("Perlu cek"),
  NotFound("Tidak ditemukan"),
  Duplicate("Duplikat"),
  NoChanges("Tanpa perubahan")
}

private data class MonthlyBulkImportPreview(
  val sheetLabel: String,
  val rows: List<MonthlyBulkImportPreviewRow>,
  val unknownColumns: List<String>
)

private data class MonthlyBulkImportPreviewRow(
  val rowNumber: Int,
  val displayName: String,
  val status: MonthlyBulkImportStatus,
  val message: String,
  val changedFields: List<String>,
  val report: MonthlyReportItem,
  val reportChanged: Boolean = changedFields.isNotEmpty(),
  val extracurricularReports: List<MonthlyExtracurricularReport> = emptyList()
) {
  val canApply: Boolean = status == MonthlyBulkImportStatus.Ready && (reportChanged || extracurricularReports.isNotEmpty())
}

private data class MonthlyBulkField(
  val label: String,
  val aliases: List<String>,
  val apply: (MonthlyReportItem, String) -> MonthlyReportItem
)

private data class MonthlyBulkExtracurricularField(
  val label: String,
  val aliases: List<String>,
  val apply: (MonthlyExtracurricularReport, String) -> MonthlyExtracurricularReport
)

private enum class MonthlyBulkColumnContext {
  None,
  Tahfiz,
  Kesantrian
}

private val monthlyBulkFields = listOf(
  MonthlyBulkField("Akhlak kelas", listOf("akhlak", "akhlak kelas", "akhlak di kelas", "nilai akhlak", "nilai akhlak kelas")) { item, value -> item.copy(nilaiAkhlak = value) },
  MonthlyBulkField("Catatan wali", listOf("catatan", "catatan wali", "catatan wali kelas", "catatan kelas")) { item, value -> item.copy(catatanWali = value) },
  MonthlyBulkField("Muhaffiz", listOf("muhaffiz", "muhafidz", "muhaffizh", "muhafizh", "nama muhaffiz", "nama muhafidz", "nama muhaffizh", "nama muhafizh", "guru tahfiz", "guru tahfidz", "guru tahfizh", "nama guru tahfiz", "nama guru tahfidz", "nama guru tahfizh")) { item, value -> item.copy(muhaffiz = value) },
  MonthlyBulkField("No HP Muhaffiz", listOf("hp muhaffiz", "no hp muhaffiz", "nomor hp muhaffiz", "kontak muhaffiz", "telepon muhaffiz", "nomor telepon muhaffiz", "wa muhaffiz", "whatsapp muhaffiz", "hp muhafidz", "no hp muhafidz", "nomor hp muhafidz")) { item, value -> item.copy(noHpMuhaffiz = value) },
  MonthlyBulkField("Kehadiran halaqah", listOf("kehadiran halaqah", "kehadiran di halaqah", "nilai kehadiran halaqah", "kehadiran tahfiz", "kehadiran tahfidz")) { item, value -> item.copy(nilaiKehadiranHalaqah = normalizePercentSpreadsheetValue(value)) },
  MonthlyBulkField("Sakit halaqah", listOf("sakit halaqah", "sakit di halaqah")) { item, value -> item.copy(sakitHalaqah = value) },
  MonthlyBulkField("Izin halaqah", listOf("izin halaqah", "izin di halaqah")) { item, value -> item.copy(izinHalaqah = value) },
  MonthlyBulkField("Akhlak halaqah", listOf("akhlak halaqah", "akhlak di halaqah", "akhlak di halaqoh", "akhlak halaqoh", "nilai akhlak halaqah", "nilai akhlak halaqoh")) { item, value -> item.copy(nilaiAkhlakHalaqah = value) },
  MonthlyBulkField("Ujian bulanan", listOf("ujian bulanan", "nilai ujian bulanan", "ujian tahfiz", "ujian tahfidz", "nilai tahfiz", "nilai tahfidz")) { item, value -> item.copy(nilaiUjianBulanan = value) },
  MonthlyBulkField("Keterangan ujian", listOf("keterangan ujian", "ket ujian", "keterangan ujian bulanan")) { item, value -> item.copy(keteranganUjianBulanan = value) },
  MonthlyBulkField("Target hafalan", listOf("target hafalan", "target hafalan %", "target hafalan (%)", "target hapalan", "target hapalan %", "target hapalan (%)", "nilai target hafalan", "nilai target hapalan", "persen target hafalan", "persen target hapalan", "persentase target hafalan", "persentase target hapalan")) { item, value -> item.copy(nilaiTargetHafalan = normalizePercentSpreadsheetValue(value)) },
  MonthlyBulkField("Keterangan target", listOf("keterangan target", "ket target", "keterangan target hafalan")) { item, value -> item.copy(keteranganTargetHafalan = value) },
  MonthlyBulkField("Capaian hafalan bulanan", listOf("capaian", "capaian hafalan", "capaian hafalan bulanan")) { item, value -> item.copy(keteranganCapaianHafalanBulanan = value) },
  MonthlyBulkField("Jumlah hafalan", listOf("jumlah hafalan", "total hafalan", "jumlah hafalan bulanan", "total hafalan bulanan")) { item, value -> item.copy(keteranganJumlahHafalanBulanan = value) },
  MonthlyBulkField("Jumlah halaman", listOf("jumlah halaman", "hafalan halaman", "halaman hafalan")) { item, value -> item.copy(nilaiJumlahHafalanHalaman = value) },
  MonthlyBulkField("Jumlah juz", listOf("jumlah juz", "hafalan juz", "juz hafalan")) { item, value -> item.copy(nilaiJumlahHafalanJuz = value) },
  MonthlyBulkField("Catatan muhaffiz", listOf("catatan muhaffiz", "catatan tahfiz", "catatan tahfidz")) { item, value -> item.copy(catatanMuhaffiz = value) },
  MonthlyBulkField("Musyrif", listOf("musyrif", "nama musyrif", "pembina asrama", "nama pembina asrama", "wali asrama", "nama wali asrama")) { item, value -> item.copy(musyrif = value) },
  MonthlyBulkField("No HP Musyrif", listOf("hp musyrif", "no hp musyrif", "nomor hp musyrif", "kontak musyrif", "telepon musyrif", "nomor telepon musyrif", "wa musyrif", "whatsapp musyrif")) { item, value -> item.copy(noHpMusyrif = value) },
  MonthlyBulkField("Kehadiran liqa", listOf("kehadiran liqa", "kehadiran liqa muhasabah", "kehadiran liqa' muhasabah", "nilai kehadiran liqa")) { item, value -> item.copy(nilaiKehadiranLiqaMuhasabah = normalizePercentSpreadsheetValue(value)) },
  MonthlyBulkField("Sakit liqa", listOf("sakit liqa", "sakit liqa muhasabah")) { item, value -> item.copy(sakitLiqaMuhasabah = value) },
  MonthlyBulkField("Izin liqa", listOf("izin liqa", "izin liqa muhasabah")) { item, value -> item.copy(izinLiqaMuhasabah = value) },
  MonthlyBulkField("Ibadah", listOf("ibadah", "nilai ibadah")) { item, value -> item.copy(nilaiIbadah = value) },
  MonthlyBulkField("Kedisiplinan", listOf("kedisiplinan", "disiplin", "nilai kedisiplinan")) { item, value -> item.copy(nilaiKedisiplinan = value) },
  MonthlyBulkField("Kebersihan", listOf("kebersihan", "nilai kebersihan")) { item, value -> item.copy(nilaiKebersihan = value) },
  MonthlyBulkField("Adab", listOf("adab", "nilai adab")) { item, value -> item.copy(nilaiAdab = value) },
  MonthlyBulkField("Prestasi", listOf("prestasi", "prestasi kesantrian")) { item, value -> item.copy(prestasiKesantrian = value) },
  MonthlyBulkField("Pelanggaran", listOf("pelanggaran", "pelanggaran kesantrian")) { item, value -> item.copy(pelanggaranKesantrian = value) },
  MonthlyBulkField("Catatan musyrif", listOf("catatan musyrif", "catatan kesantrian", "catatan pembina", "catatan wali asrama")) { item, value -> item.copy(catatanMusyrif = value) }
)

private val monthlyBulkExtracurricularFields = listOf(
  MonthlyBulkExtracurricularField("Kehadiran ekskul", listOf("kehadiran", "kehadiran ekskul", "kehadiran ekstrakurikuler", "persen ekskul", "persentase ekskul", "nilai ekskul")) { item, value ->
    item.copy(attendanceLabel = value)
  },
  MonthlyBulkExtracurricularField("Catatan PJ ekskul", listOf("catatan pj", "catatan pj ekskul", "catatan ekskul", "catatan ekstrakurikuler", "laporan ekskul")) { item, value ->
    item.copy(note = value)
  }
)

private fun resolveMonthlyBulkFieldColumns(headers: List<String>): List<Pair<Int, MonthlyBulkField>> {
  var context = MonthlyBulkColumnContext.None
  return headers.mapIndexedNotNull { index, header ->
    val normalized = normalizeSpreadsheetKey(header)
    val directField = findMonthlyBulkDirectField(header)
    val inferredField = directField ?: inferMonthlyBulkGenericField(
      normalizedHeader = normalized,
      context = inferMonthlyBulkColumnContext(headers, index, context)
    )
    val nextContext = when {
      inferredField != null -> contextForMonthlyBulkField(inferredField)
      else -> contextFromMonthlyBulkText(normalized)
    }
    if (nextContext != MonthlyBulkColumnContext.None) context = nextContext
    inferredField?.let { index to it }
  }
}

private fun buildMonthlyBulkHeaders(
  table: List<List<String>>,
  headerIndex: Int
): List<String> {
  val rawHeaders = table.getOrNull(headerIndex).orEmpty()
  val contextRow = table.getOrNull(headerIndex - 1)
    .orEmpty()
    .fillForwardMonthlyBulkContexts()
  return rawHeaders.mapIndexed { index, header ->
    val normalized = normalizeSpreadsheetKey(header)
    val context = contextRow.getOrNull(index).orEmpty()
    val contextType = contextFromMonthlyBulkText(normalizeSpreadsheetKey(context))
    if (contextType != MonthlyBulkColumnContext.None && normalized.isMonthlyBulkGenericSubHeader()) {
      "$context $header"
    } else {
      header
    }
  }
}

private fun List<String>.fillForwardMonthlyBulkContexts(): List<String> {
  var last = ""
  return map { cell ->
    val trimmed = cell.trim()
    if (trimmed.isNotBlank()) last = trimmed
    last
  }
}

private fun findMonthlyBulkDirectField(header: String): MonthlyBulkField? {
  val normalizedHeader = normalizeSpreadsheetKey(header)
  return monthlyBulkFields.firstOrNull { field ->
    field.aliases.any { alias -> normalizeSpreadsheetKey(alias) == normalizedHeader }
  }
}

private fun inferMonthlyBulkGenericField(
  normalizedHeader: String,
  context: MonthlyBulkColumnContext
): MonthlyBulkField? {
  val words = normalizedHeader.split(" ").filter { it.isNotBlank() }.toSet()
  if ("pj" in words) return null
  val genericPhone = words.any { it in setOf("hp", "kontak", "telepon", "telp", "wa", "whatsapp") }
  val genericSakit = "sakit" in words
  val genericIzin = "izin" in words
  val label = when {
    genericPhone && context == MonthlyBulkColumnContext.Tahfiz -> "No HP Muhaffiz"
    genericPhone && context == MonthlyBulkColumnContext.Kesantrian -> "No HP Musyrif"
    genericSakit && context == MonthlyBulkColumnContext.Tahfiz -> "Sakit halaqah"
    genericIzin && context == MonthlyBulkColumnContext.Tahfiz -> "Izin halaqah"
    genericSakit && context == MonthlyBulkColumnContext.Kesantrian -> "Sakit liqa"
    genericIzin && context == MonthlyBulkColumnContext.Kesantrian -> "Izin liqa"
    else -> ""
  }
  return monthlyBulkFields.firstOrNull { it.label == label }
}

private fun String.isMonthlyBulkGenericSubHeader(): Boolean {
  val words = split(" ").filter { it.isNotBlank() }.toSet()
  return words.any { it in setOf("hp", "kontak", "telepon", "telp", "wa", "whatsapp", "sakit", "izin") }
}

private fun inferMonthlyBulkColumnContext(
  headers: List<String>,
  index: Int,
  current: MonthlyBulkColumnContext
): MonthlyBulkColumnContext {
  val self = contextFromMonthlyBulkText(normalizeSpreadsheetKey(headers.getOrNull(index).orEmpty()))
  if (self != MonthlyBulkColumnContext.None) return self
  val nearby = ((index - 3)..(index + 2))
    .filter { it in headers.indices && it != index }
    .map { normalizeSpreadsheetKey(headers[it]) }
  val tahfizHint = nearby.any { contextFromMonthlyBulkText(it) == MonthlyBulkColumnContext.Tahfiz }
  val kesantrianHint = nearby.any { contextFromMonthlyBulkText(it) == MonthlyBulkColumnContext.Kesantrian }
  return when {
    tahfizHint && !kesantrianHint -> MonthlyBulkColumnContext.Tahfiz
    kesantrianHint && !tahfizHint -> MonthlyBulkColumnContext.Kesantrian
    else -> current
  }
}

private fun contextForMonthlyBulkField(field: MonthlyBulkField): MonthlyBulkColumnContext {
  return when (field.label) {
    "Muhaffiz",
    "No HP Muhaffiz",
    "Kehadiran halaqah",
    "Sakit halaqah",
    "Izin halaqah",
    "Akhlak halaqah",
    "Ujian bulanan",
    "Keterangan ujian",
    "Target hafalan",
    "Keterangan target",
    "Capaian hafalan bulanan",
    "Jumlah hafalan",
    "Jumlah halaman",
    "Jumlah juz",
    "Catatan muhaffiz" -> MonthlyBulkColumnContext.Tahfiz
    "Musyrif",
    "No HP Musyrif",
    "Kehadiran liqa",
    "Sakit liqa",
    "Izin liqa",
    "Ibadah",
    "Kedisiplinan",
    "Kebersihan",
    "Adab",
    "Prestasi",
    "Pelanggaran",
    "Catatan musyrif" -> MonthlyBulkColumnContext.Kesantrian
    else -> MonthlyBulkColumnContext.None
  }
}

private fun contextFromMonthlyBulkText(text: String): MonthlyBulkColumnContext {
  return when {
    text.contains("muhaffiz") ||
      text.contains("muhafidz") ||
      text.contains("tahfiz") ||
      text.contains("halaqah") ||
      text.contains("hafalan") -> MonthlyBulkColumnContext.Tahfiz
    text.contains("musyrif") ||
      text.contains("kesantrian") ||
      text.contains("asrama") ||
      text.contains("liqa") ||
      text.contains("muhasabah") -> MonthlyBulkColumnContext.Kesantrian
    else -> MonthlyBulkColumnContext.None
  }
}

private fun normalizePercentSpreadsheetValue(value: String): String {
  return value.trim().removeSuffix("%").trim()
}

private suspend fun buildMonthlyBulkImportPreview(
  spreadsheetUrl: String,
  students: List<WaliSantriProfile>,
  reportsByStudent: Map<String, MonthlyReportItem>,
  attendanceByStudent: Map<String, MonthlyAttendanceSummary>,
  extracurricularByStudent: Map<String, List<MonthlyExtracurricularReport>>,
  period: String,
  guruId: String,
  waliName: String,
  waliPhone: String
): MonthlyBulkImportPreview {
  if (spreadsheetUrl.isBlank()) error("Link spreadsheet belum diisi.")
  if (students.isEmpty()) error("Data santri wali kelas belum tersedia.")
  val fetchResult = fetchMonthlyBulkCsv(spreadsheetUrl, students.map { it.className }.distinct())
  val table = parseCsvRows(fetchResult.csv).filter { row -> row.any { it.isNotBlank() } }
  if (table.size < 2) error("Spreadsheet belum memiliki data yang bisa dibaca.")
  val headerIndex = table.indexOfFirst { row -> row.count { it.isNotBlank() } >= 2 }
  if (headerIndex < 0 || headerIndex >= table.lastIndex) error("Header spreadsheet tidak ditemukan.")
  val headers = buildMonthlyBulkHeaders(table, headerIndex)
  val nameColumn = headers.indexOfFirst { normalizeSpreadsheetKey(it) in setOf("nama", "nama santri", "nama siswa", "santri", "siswa") }
  val nisnColumn = headers.indexOfFirst { normalizeSpreadsheetKey(it) in setOf("nisn", "nis", "no induk", "nomor induk") }
  val classColumn = headers.indexOfFirst { normalizeSpreadsheetKey(it) in setOf("kelas", "class") }
  val extracurricularNameColumn = headers.indexOfFirst {
    normalizeSpreadsheetKey(it) in setOf("ekskul", "ekstrakurikuler", "kegiatan ekskul", "kegiatan ekstrakurikuler")
  }
  if (nameColumn < 0 && nisnColumn < 0) error("Kolom nama atau NISN belum ditemukan.")

  val fieldColumns = resolveMonthlyBulkFieldColumns(headers)
  val extracurricularFieldColumns = headers.mapIndexedNotNull { index, header ->
    monthlyBulkExtracurricularFields.firstOrNull { field ->
      field.aliases.any { alias -> normalizeSpreadsheetKey(alias) == normalizeSpreadsheetKey(header) }
    }?.let { index to it }
  }
  if (fieldColumns.isEmpty() && extracurricularFieldColumns.isEmpty()) error("Belum ada kolom laporan yang dikenali.")
  val knownIndexes = buildSet {
    if (nameColumn >= 0) add(nameColumn)
    if (nisnColumn >= 0) add(nisnColumn)
    if (classColumn >= 0) add(classColumn)
    if (extracurricularNameColumn >= 0) add(extracurricularNameColumn)
    fieldColumns.forEach { add(it.first) }
    extracurricularFieldColumns.forEach { add(it.first) }
  }
  val unknownColumns = headers.mapIndexedNotNull { index, header ->
    header.trim().takeIf { it.isNotBlank() && index !in knownIndexes }
  }

  val rows = table.drop(headerIndex + 1).mapIndexedNotNull { offset, cells ->
    val rowNumber = headerIndex + offset + 2
    val sourceName = cells.getOrNull(nameColumn).orEmpty().trim()
    val sourceNisn = cells.getOrNull(nisnColumn).orEmpty().trim()
    if (sourceName.isBlank() && sourceNisn.isBlank()) return@mapIndexedNotNull null
    val sourceClass = cells.getOrNull(classColumn).orEmpty().trim()
    val match = matchMonthlyBulkStudent(sourceNisn, sourceName, sourceClass, students)
    val student = match.student
    when {
      student == null -> {
        MonthlyBulkImportPreviewRow(
          rowNumber = rowNumber,
          displayName = sourceName.ifBlank { sourceNisn.ifBlank { "Baris $rowNumber" } },
          status = match.status,
          message = match.message,
          changedFields = emptyList(),
          report = MonthlyReportItem()
        )
      }
      else -> {
        val base = reportsByStudent[student.id] ?: MonthlyReportItem(
          period = period,
          guruId = guruId,
          classId = student.classId,
          studentId = student.id
        )
        var next = base.copy(
          period = period,
          guruId = base.guruId.ifBlank { guruId },
          classId = base.classId.ifBlank { student.classId },
          studentId = student.id
        )
        val changed = mutableListOf<String>()
        fieldColumns.forEach { (index, field) ->
          val value = cells.getOrNull(index).orEmpty().trim()
          if (value.isNotBlank()) {
            val before = next
            next = field.apply(next, value)
            if (before != next) changed += field.label
          }
        }
        next = next.withAutomaticGradeDescriptions()
        val extracurricularChanged = mutableListOf<String>()
        val sourceActivity = cells.getOrNull(extracurricularNameColumn).orEmpty().trim()
        val extracurricularTargets = if (extracurricularFieldColumns.isEmpty()) {
          emptyList()
        } else {
          findMonthlyBulkExtracurricularTargets(
            sourceActivity = sourceActivity,
            reports = extracurricularByStudent[student.id].orEmpty()
          )
        }
        val extracurricularDrafts = extracurricularTargets.mapNotNull { existingExtracurricular ->
          var draft = existingExtracurricular.copy(period = period, studentId = student.id)
          val changedForItem = mutableListOf<String>()
          extracurricularFieldColumns.forEach { (index, field) ->
            val value = cells.getOrNull(index).orEmpty().trim()
            if (value.isNotBlank()) {
              val before = draft
              draft = field.apply(draft, value)
              if (before != draft) changedForItem += field.label
            }
          }
          if (changedForItem.isEmpty()) {
            null
          } else {
            extracurricularChanged += changedForItem.map { "${existingExtracurricular.activityName}: $it" }
            draft
          }
        }
        val allChanged = (changed.distinct() + extracurricularChanged.distinct())
        val status = when {
          allChanged.isEmpty() -> MonthlyBulkImportStatus.NoChanges
          match.status == MonthlyBulkImportStatus.NeedsReview -> MonthlyBulkImportStatus.NeedsReview
          else -> MonthlyBulkImportStatus.Ready
        }
        val exportData = buildMonthlyReportExportData(
          santri = student,
          period = period,
          report = next,
          attendanceSummary = attendanceByStudent[student.id],
          extracurricularReports = extracurricularByStudent[student.id].orEmpty(),
          waliName = waliName,
          waliPhone = waliPhone
        )
        MonthlyBulkImportPreviewRow(
          rowNumber = rowNumber,
          displayName = student.name,
          status = status,
          message = when (status) {
            MonthlyBulkImportStatus.Ready -> "Cocok dengan ${student.name}. PDF preview siap untuk ${exportData.periodLabel}."
            MonthlyBulkImportStatus.NeedsReview -> "${match.message} Tekan Setujui jika benar."
            MonthlyBulkImportStatus.NoChanges ->
              if (extracurricularFieldColumns.isNotEmpty() && extracurricularTargets.isEmpty()) {
                "Cocok dengan ${student.name}, tetapi kegiatan ekskul belum ditemukan untuk baris ini."
              } else {
                "Cocok dengan ${student.name}, tetapi tidak ada nilai baru pada kolom yang dikenali."
              }
            else -> "Cocok dengan ${student.name}, tetapi tidak ada nilai baru pada kolom yang dikenali."
          },
          changedFields = allChanged,
          report = next,
          reportChanged = changed.isNotEmpty(),
          extracurricularReports = extracurricularDrafts
        )
      }
    }
  }
  if (rows.isEmpty()) error("Tidak ada baris santri yang bisa diproses.")
  return MonthlyBulkImportPreview(fetchResult.sheetLabel, rows, unknownColumns)
}

private fun findMonthlyBulkExtracurricularTargets(
  sourceActivity: String,
  reports: List<MonthlyExtracurricularReport>
): List<MonthlyExtracurricularReport> {
  if (reports.isEmpty()) return emptyList()
  val normalizedActivity = normalizeSpreadsheetKey(sourceActivity)
  if (normalizedActivity.isBlank()) {
    return if (reports.size == 1) reports else emptyList()
  }
  val exact = reports.filter { normalizeSpreadsheetKey(it.activityName) == normalizedActivity }
  if (exact.isNotEmpty()) return exact
  return reports
    .map { it to similarityScore(normalizedActivity, normalizeSpreadsheetKey(it.activityName)) }
    .filter { it.second >= 0.78 }
    .maxByOrNull { it.second }
    ?.let { listOf(it.first) }
    .orEmpty()
}

private data class MonthlyBulkFetchResult(val sheetLabel: String, val csv: String)

private suspend fun fetchMonthlyBulkCsv(
  rawUrl: String,
  classNames: List<String>
): MonthlyBulkFetchResult = withContext(Dispatchers.IO) {
  val spreadsheetId = Regex("""/spreadsheets/d/([a-zA-Z0-9-_]+)""").find(rawUrl)?.groupValues?.getOrNull(1)
  val gid = Regex("""[?&]gid=([0-9]+)""").find(rawUrl)?.groupValues?.getOrNull(1)
  val classTargets = classNames.map { it.trim() }.filter { it.isNotBlank() }.distinct()
  val isGoogleSheetUrl = spreadsheetId != null ||
    rawUrl.contains("docs.google.com/spreadsheets", ignoreCase = true)
  val candidates = buildList {
    if (!isGoogleSheetUrl && (rawUrl.contains("output=csv", ignoreCase = true) || rawUrl.contains("format=csv", ignoreCase = true))) {
      add("Link CSV" to rawUrl)
    }
    if (spreadsheetId != null) {
      classTargets.flatMap(::classSheetAliases).distinct().forEach { sheet ->
        add(sheet to "https://docs.google.com/spreadsheets/d/$spreadsheetId/gviz/tq?tqx=out:csv&sheet=${encodeGoogleSheetName(sheet)}")
        add("$sheet (quoted)" to "https://docs.google.com/spreadsheets/d/$spreadsheetId/gviz/tq?tqx=out:csv&sheet=${encodeGoogleSheetName("'$sheet'")}")
        add("$sheet (export)" to "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&sheet=${encodeGoogleSheetName(sheet)}")
      }
      if (classTargets.isEmpty()) {
        if (rawUrl.contains("output=csv", ignoreCase = true) || rawUrl.contains("format=csv", ignoreCase = true)) {
          add("Link CSV" to rawUrl)
        }
        if (gid != null) {
          add("Tab aktif" to "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&gid=$gid")
        }
        add("Sheet pertama" to "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv")
      }
    }
  }
  if (candidates.isEmpty()) error("Link Google Sheet tidak valid atau belum public.")
  var lastError = ""
  candidates.forEach { (label, url) ->
    runCatching {
      val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 12_000
        readTimeout = 18_000
        requestMethod = "GET"
      }
      try {
        val code = connection.responseCode
        val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (code in 200..299 && body.isLikelyCsv()) return@withContext MonthlyBulkFetchResult(label, body)
        lastError = "Tab $label belum bisa dibaca."
      } finally {
        connection.disconnect()
      }
    }.onFailure { error ->
      lastError = error.message.orEmpty()
    }
  }
  error(lastError.ifBlank { "Spreadsheet tidak bisa dibaca. Pastikan link public/shareable." })
}

private fun encodeGoogleSheetName(sheet: String): String {
  return URLEncoder.encode(sheet, Charsets.UTF_8.name()).replace("+", "%20")
}

private fun String.isLikelyCsv(): Boolean {
  val trimmed = trimStart()
  return trimmed.isNotBlank() &&
    !trimmed.startsWith("<") &&
    !trimmed.startsWith("{\"version\"", ignoreCase = true) &&
    !trimmed.contains("Unable to parse", ignoreCase = true) &&
    !trimmed.contains("Invalid query", ignoreCase = true) &&
    !trimmed.contains("not found", ignoreCase = true)
}

private data class MonthlyBulkStudentMatch(
  val status: MonthlyBulkImportStatus,
  val student: WaliSantriProfile?,
  val message: String
)

private fun matchMonthlyBulkStudent(
  nisn: String,
  name: String,
  className: String,
  students: List<WaliSantriProfile>
): MonthlyBulkStudentMatch {
  val cleanNisn = nisn.filter(Char::isDigit)
  if (cleanNisn.isNotBlank()) {
    val matches = students.filter { it.nisn.filter(Char::isDigit) == cleanNisn }
    if (matches.size == 1) return MonthlyBulkStudentMatch(MonthlyBulkImportStatus.Ready, matches.first(), "Cocok berdasarkan NISN.")
    if (matches.size > 1) return MonthlyBulkStudentMatch(MonthlyBulkImportStatus.Duplicate, null, "NISN $nisn cocok ke lebih dari satu santri.")
  }
  val normalizedName = normalizeStudentNameForBulkMatch(name)
  if (normalizedName.isBlank()) {
    return MonthlyBulkStudentMatch(MonthlyBulkImportStatus.NotFound, null, "Nama/NISN kosong.")
  }
  val classAliases = classSheetAliases(className).map(::normalizeSpreadsheetKey).toSet()
  val exactMatches = students.filter { student ->
    normalizeStudentNameForBulkMatch(student.name) == normalizedName &&
      (classAliases.isEmpty() || normalizeSpreadsheetKey(student.className) in classAliases || className.isBlank())
  }
  if (exactMatches.size == 1) return MonthlyBulkStudentMatch(MonthlyBulkImportStatus.Ready, exactMatches.first(), "Cocok berdasarkan nama.")
  if (exactMatches.size > 1) return MonthlyBulkStudentMatch(MonthlyBulkImportStatus.Duplicate, null, "Nama $name cocok ke lebih dari satu santri.")
  val suggestions = students
    .map { it to studentNameSimilarityScore(normalizedName, normalizeStudentNameForBulkMatch(it.name)) }
    .filter { it.second >= 0.68 }
    .sortedByDescending { it.second }
    .take(3)
  return if (suggestions.isNotEmpty()) {
    val topSuggestion = suggestions.first().first
    MonthlyBulkStudentMatch(
      status = MonthlyBulkImportStatus.NeedsReview,
      student = topSuggestion,
      message = "Kemungkinan cocok: ${topSuggestion.name}. Alternatif: ${suggestions.joinToString { it.first.name }}."
    )
  } else {
    MonthlyBulkStudentMatch(MonthlyBulkImportStatus.NotFound, null, "Santri $name tidak ditemukan.")
  }
}

private fun parseCsvRows(csv: String): List<List<String>> {
  val rows = mutableListOf<MutableList<String>>()
  var row = mutableListOf<String>()
  val cell = StringBuilder()
  var quoted = false
  var index = 0
  while (index < csv.length) {
    val char = csv[index]
    when {
      char == '"' && quoted && index + 1 < csv.length && csv[index + 1] == '"' -> {
        cell.append('"')
        index += 1
      }
      char == '"' -> quoted = !quoted
      char == ',' && !quoted -> {
        row += cell.toString()
        cell.clear()
      }
      (char == '\n' || char == '\r') && !quoted -> {
        if (char == '\r' && index + 1 < csv.length && csv[index + 1] == '\n') index += 1
        row += cell.toString()
        rows += row
        row = mutableListOf()
        cell.clear()
      }
      else -> cell.append(char)
    }
    index += 1
  }
  row += cell.toString()
  if (row.any { it.isNotBlank() }) rows += row
  return rows
}

private fun normalizeSpreadsheetKey(value: String): String {
  val cleaned = value
    .trim()
    .lowercase(Locale.ROOT)
    .replace("_", " ")
    .replace("-", " ")
    .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
    .replace(Regex("""\b(kelas|kls|class|mapel|mata pelajaran|nilai|nomor|no)\b"""), " ")
    .replace(Regex("\\s+"), " ")
    .trim()
  return cleaned
    .replace("akidah", "aqidah")
    .replace("hapalan", "hafalan")
    .replace("halaqoh", "halaqah")
    .replace("muhaffizh", "muhaffiz")
    .replace("muhafizh", "muhafidz")
    .replace("tahfidz", "tahfiz")
    .replace("tahfizh", "tahfiz")
}

private fun normalizeStudentNameForBulkMatch(value: String): String {
  return normalizeSpreadsheetKey(value)
    .split(" ")
    .filter { it.isNotBlank() }
    .map { token ->
      when (token) {
        "m", "muh", "muhd", "mhd", "moh", "mohd", "muhamad", "muhammad", "mohammad" -> "muhammad"
        "abd", "abdul" -> "abdul"
        else -> token
      }
    }
    .filterNot { it in setOf("bin", "binti", "al", "el") }
    .joinToString(" ")
}

private fun classSheetAliases(className: String): List<String> {
  val trimmed = className.trim()
  if (trimmed.isBlank()) return emptyList()
  val withoutPrefix = trimmed
    .replace(Regex("""^(kelas|kls|class)\s*""", RegexOption.IGNORE_CASE), "")
    .trim()
  val baseForms = listOf(trimmed, withoutPrefix)
    .filter { it.isNotBlank() }
    .flatMap(::classNameShapeVariants)
  val gradeOnlyForms = baseForms.mapNotNull(::extractClassGradeToken)
    .flatMap(::classNameShapeVariants)
  return (baseForms + gradeOnlyForms)
    .flatMap { form ->
      val hasPrefix = form.startsWith("kelas ", ignoreCase = true) ||
        form.startsWith("kls ", ignoreCase = true) ||
        form.startsWith("class ", ignoreCase = true)
      val preferred = if (hasPrefix) {
        listOf(form)
      } else {
        listOf(
          "Kelas $form",
          "kelas $form",
          "Kls $form",
          "kls $form"
        )
      }
      preferred + listOf(
        "Kelas $form",
        "kelas $form",
        "Kls $form",
        "kls $form",
        form,
        form.replace(" ", ""),
        form.replace(".", "")
      )
    }
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .distinct()
}

private fun classNameShapeVariants(value: String): List<String> {
  val spaced = value.replace(".", " ").replace(Regex("\\s+"), " ").trim()
  val dotted = spaced.replace(" ", ".")
  val compact = spaced.replace(" ", "")
  val numeric = romanClassToNumber(spaced)
  val roman = numberClassToRoman(spaced)
  return listOf(
    value,
    spaced,
    dotted,
    compact,
    numeric,
    numeric.replace(" ", "."),
    numeric.replace(" ", ""),
    roman,
    roman.replace(" ", "."),
    roman.replace(" ", "")
  ).filter { it.isNotBlank() }.distinct()
}

private fun extractClassGradeToken(value: String): String? {
  val spaced = value.replace(".", " ").replace(Regex("\\s+"), " ").trim()
  return Regex("""^(VII|VIII|IX|X|7|8|9|10)\b""", RegexOption.IGNORE_CASE)
    .find(spaced)
    ?.value
    ?.uppercase(Locale.ROOT)
}

private fun romanClassToNumber(value: String): String {
  return value
    .replace(Regex("""\bVII\b""", RegexOption.IGNORE_CASE), "7")
    .replace(Regex("""\bVIII\b""", RegexOption.IGNORE_CASE), "8")
    .replace(Regex("""\bIX\b""", RegexOption.IGNORE_CASE), "9")
    .replace(Regex("""\bX\b""", RegexOption.IGNORE_CASE), "10")
}

private fun numberClassToRoman(value: String): String {
  return value
    .replace(Regex("""\b7\b"""), "VII")
    .replace(Regex("""\b8\b"""), "VIII")
    .replace(Regex("""\b9\b"""), "IX")
    .replace(Regex("""\b10\b"""), "X")
}

private fun similarityScore(a: String, b: String): Double {
  if (a.isBlank() || b.isBlank()) return 0.0
  if (a == b) return 1.0
  val distance = levenshteinDistance(a, b)
  return 1.0 - (distance.toDouble() / maxOf(a.length, b.length).toDouble())
}

private fun studentNameSimilarityScore(source: String, target: String): Double {
  val sourceTokens = source.split(" ").filter { it.isNotBlank() }.toSet()
  val targetTokens = target.split(" ").filter { it.isNotBlank() }.toSet()
  if (sourceTokens.isEmpty() || targetTokens.isEmpty()) return 0.0
  val intersection = sourceTokens.intersect(targetTokens).size.toDouble()
  val coverage = intersection / minOf(sourceTokens.size, targetTokens.size).toDouble()
  val dice = (2.0 * intersection) / (sourceTokens.size + targetTokens.size).toDouble()
  val orderBonus = if (
    sourceTokens.firstOrNull() == targetTokens.firstOrNull() ||
    sourceTokens.drop(1).intersect(targetTokens.drop(1).toSet()).isNotEmpty()
  ) 0.06 else 0.0
  return maxOf(similarityScore(source, target), coverage * 0.72 + dice * 0.28 + orderBonus).coerceAtMost(1.0)
}

private fun levenshteinDistance(a: String, b: String): Int {
  val costs = IntArray(b.length + 1) { it }
  for (i in 1..a.length) {
    var previous = i - 1
    costs[0] = i
    for (j in 1..b.length) {
      val current = costs[j]
      costs[j] = minOf(
        costs[j] + 1,
        costs[j - 1] + 1,
        previous + if (a[i - 1] == b[j - 1]) 0 else 1
      )
      previous = current
    }
  }
  return costs[b.length]
}

private fun MonthlyReportItem.toSystemResetReport(
  period: String,
  guruId: String,
  classId: String
): MonthlyReportItem {
  return MonthlyReportItem(
    id = id,
    period = this.period.ifBlank { period },
    guruId = this.guruId.ifBlank { guruId },
    classId = this.classId.ifBlank { classId },
    studentId = studentId
  )
}

private fun MonthlyReportItem.hasMonthlyOverrideValues(): Boolean {
  return listOf(
    nilaiAkhlak,
    predikat,
    catatanWali,
    muhaffiz,
    noHpMuhaffiz,
    nilaiKehadiranHalaqah,
    sakitHalaqah,
    izinHalaqah,
    nilaiAkhlakHalaqah,
    keteranganAkhlakHalaqah,
    nilaiUjianBulanan,
    keteranganUjianBulanan,
    nilaiTargetHafalan,
    keteranganTargetHafalan,
    nilaiCapaianHafalanBulanan,
    keteranganCapaianHafalanBulanan,
    keteranganJumlahHafalanBulanan,
    nilaiJumlahHafalanHalaman,
    nilaiJumlahHafalanJuz,
    catatanMuhaffiz,
    musyrif,
    noHpMusyrif,
    nilaiKehadiranLiqaMuhasabah,
    sakitLiqaMuhasabah,
    izinLiqaMuhasabah,
    nilaiIbadah,
    keteranganIbadah,
    nilaiKedisiplinan,
    keteranganKedisiplinan,
    nilaiKebersihan,
    keteranganKebersihan,
    nilaiAdab,
    keteranganAdab,
    prestasiKesantrian,
    pelanggaranKesantrian,
    catatanMusyrif
  ).any { it.isNotBlank() }
}

private fun MonthlyReportItem?.missingLabels(): List<String> {
  if (this == null) return listOf("Wali Kelas", "Muhaffiz", "Musyrif")
  val missing = mutableListOf<String>()
  if (nilaiAkhlak.isBlank() || catatanWali.isBlank()) missing += "Wali Kelas"
  if (!hasTahfizValue()) missing += "Muhaffiz"
  if (!hasKesantrianValue()) missing += "Musyrif"
  return missing
}

private fun MonthlyReportItem.hasTahfizValue(): Boolean {
  return listOf(
    muhaffiz,
    nilaiKehadiranHalaqah,
    nilaiAkhlakHalaqah,
    nilaiUjianBulanan,
    nilaiTargetHafalan,
    nilaiCapaianHafalanBulanan,
    nilaiJumlahHafalanHalaman,
    nilaiJumlahHafalanJuz,
    catatanMuhaffiz
  ).any { it.isNotBlank() }
}

private fun MonthlyReportItem.hasKesantrianValue(): Boolean {
  return listOf(
    musyrif,
    nilaiKehadiranLiqaMuhasabah,
    nilaiIbadah,
    nilaiKedisiplinan,
    nilaiKebersihan,
    nilaiAdab,
    prestasiKesantrian,
    pelanggaranKesantrian,
    catatanMusyrif
  ).any { it.isNotBlank() }
}

private data class AspectSummary(
  val title: String,
  val value: String,
  val description: String,
  val color: Color
)

private fun buildMentorLabel(
  role: String,
  name: String?,
  phone: String?
): String {
  val mentorName = name.orDash()
  val mentorPhone = phone.orDash()
  if (mentorName == "-" && mentorPhone == "-") return "$role belum tersedia"
  return "$role: $mentorName | HP: $mentorPhone"
}

private fun scoreColorFor(
  value: String,
  fallback: Color
): Color {
  val firstToken = value.trim().uppercase().substringBefore(" ").substringBefore("(")
  val grade = firstToken.takeIf { it in listOf("A", "B", "C", "D", "E") }
  if (grade != null) {
    return gradeColor(grade, fallback)
  }

  val normalized = value.replace(",", ".")
  val number = Regex("""-?\d+(\.\d+)?""").find(normalized)?.value?.toDoubleOrNull()
    ?: return fallback
  val comparable = if (number in 1.0..5.0 && !value.contains("%")) {
    number * 20.0
  } else {
    number
  }
  return when {
    comparable >= 90.0 -> Color(0xFF2563EB)
    comparable >= 80.0 -> Color(0xFF16A34A)
    comparable >= 70.0 -> Color(0xFFD97706)
    comparable >= 60.0 -> Color(0xFFEA580C)
    comparable >= 0.0 -> Color(0xFFDC2626)
    else -> fallback
  }
}

private fun MonthlyReportItem.withAutomaticGradeDescriptions(): MonthlyReportItem {
  return copy(
    predikat = akhlakDescriptionFromValue(nilaiAkhlak),
    keteranganAkhlakHalaqah = akhlakDescriptionFromValue(nilaiAkhlakHalaqah),
    keteranganIbadah = akhlakDescriptionFromValue(nilaiIbadah),
    keteranganKedisiplinan = akhlakDescriptionFromValue(nilaiKedisiplinan),
    keteranganKebersihan = akhlakDescriptionFromValue(nilaiKebersihan),
    keteranganAdab = akhlakDescriptionFromValue(nilaiAdab)
  )
}

private fun buildMonthlyReportExportData(
  santri: WaliSantriProfile,
  period: String,
  report: MonthlyReportItem,
  attendanceSummary: MonthlyAttendanceSummary?,
  extracurricularReports: List<MonthlyExtracurricularReport>,
  waliName: String,
  waliPhone: String
): MonthlyReportExportData {
  val waTargets = buildList {
    if (santri.fatherPhone.isNotBlank()) add(MonthlyReportWaTarget("Ayah", santri.fatherPhone))
    if (santri.motherPhone.isNotBlank()) add(MonthlyReportWaTarget("Ibu", santri.motherPhone))
    if (santri.guardianPhone.isNotBlank()) add(MonthlyReportWaTarget("Wali", santri.guardianPhone))
    if (santri.studentPhone.isNotBlank()) add(MonthlyReportWaTarget("Santri", santri.studentPhone))
  }.distinctBy { it.phone.filter(Char::isDigit) }

  val ekskulExportRows = if (extracurricularReports.isEmpty()) {
    listOf(MonthlyReportExportExtracurricularRow(activityName = "-", pjName = "-", pjPhone = "-", attendance = "-", note = "-"))
  } else {
    extracurricularReports.map { item ->
      MonthlyReportExportExtracurricularRow(
        activityName = item.activityName.ifBlank { "-" },
        pjName = item.pjName.ifBlank { "-" },
        pjPhone = item.pjPhone.ifBlank { "-" },
        attendance = item.attendanceLabel.ifBlank { "-" },
        note = item.note.ifBlank { "-" }
      )
    }
  }

  return MonthlyReportExportData(
    studentId = santri.id,
    studentName = santri.name.ifBlank { "-" },
    className = santri.className.ifBlank { "-" },
    periodLabel = period.toPeriodLabel(),
    waliName = waliName.ifBlank { "-" },
    waliPhone = waliPhone.ifBlank { "-" },
    waTargets = waTargets,
    extracurricularRows = ekskulExportRows,
    sections = listOf(
      MonthlyReportExportSection(
        title = "A. Laporan Akademik",
        subtitle = buildMentorLabel("Wali kelas", waliName, waliPhone),
        rows = listOf(
          MonthlyReportExportRow(
            title = "Kehadiran di kelas",
            value = attendanceSummary?.attendancePercent.withPercentSuffix(),
            description = "Sakit ${attendanceSummary?.sakitCount ?: 0} kali\nIzin ${attendanceSummary?.izinCount ?: 0} kali"
          ),
          MonthlyReportExportRow("Akhlak di kelas", report.nilaiAkhlak.orDash(), report.predikat.orDash()),
          MonthlyReportExportRow("Catatan wali kelas", description = report.catatanWali.orDash())
        )
      ),
      MonthlyReportExportSection(
        title = "B. Laporan Ketahfizan",
        subtitle = buildMentorLabel("Muhaffiz", report.muhaffiz, report.noHpMuhaffiz),
        rows = listOf(
          MonthlyReportExportRow("Kehadiran di halaqah", report.nilaiKehadiranHalaqah.withPercentSuffix(), "Sakit ${report.sakitHalaqah.orZero()} kali\nIzin ${report.izinHalaqah.orZero()} kali"),
          MonthlyReportExportRow("Akhlak di halaqah", report.nilaiAkhlakHalaqah.orDash(), report.keteranganAkhlakHalaqah.orDash()),
          MonthlyReportExportRow("Ujian bulanan", report.nilaiUjianBulanan.orDash(), report.keteranganUjianBulanan.orDash()),
          MonthlyReportExportRow("Target hafalan", report.nilaiTargetHafalan.withPercentSuffix(), report.keteranganTargetHafalan.orDash()),
          MonthlyReportExportRow("Capaian hafalan bulanan", value = buildCapaianHafalanBulananLabel(report), description = "-"),
          MonthlyReportExportRow("Jumlah hafalan", value = buildJumlahHafalanLabel(report), description = "-"),
          MonthlyReportExportRow("Catatan muhaffiz", description = report.catatanMuhaffiz.orDash())
        )
      ),
      MonthlyReportExportSection(
        title = "C. Laporan Kesantrian",
        subtitle = buildMentorLabel("Musyrif", report.musyrif, report.noHpMusyrif),
        rows = listOf(
          MonthlyReportExportRow("Kehadiran di Liqa' Muhasabah", report.nilaiKehadiranLiqaMuhasabah.withPercentSuffix(), "Sakit ${report.sakitLiqaMuhasabah.orZero()} kali\nIzin ${report.izinLiqaMuhasabah.orZero()} kali"),
          MonthlyReportExportRow("Ibadah", report.nilaiIbadah.orDash(), report.keteranganIbadah.orDash()),
          MonthlyReportExportRow("Kedisiplinan", report.nilaiKedisiplinan.orDash(), report.keteranganKedisiplinan.orDash()),
          MonthlyReportExportRow("Kebersihan", report.nilaiKebersihan.orDash(), report.keteranganKebersihan.orDash()),
          MonthlyReportExportRow("Adab", report.nilaiAdab.orDash(), report.keteranganAdab.orDash()),
          MonthlyReportExportRow("Prestasi", value = report.prestasiKesantrian.orDash(), description = "-"),
          MonthlyReportExportRow("Pelanggaran", value = report.pelanggaranKesantrian.orDash(), description = "-"),
          MonthlyReportExportRow("Catatan musyrif", description = report.catatanMusyrif.orDash())
        )
      ),
      MonthlyReportExportSection(
        title = "D. Laporan Ekstrakulikuler"
      )
    )
  )
}

private fun sanitizeGradeInput(value: String): String? {
  val normalized = value.trim().uppercase(Locale.ROOT)
  if (normalized.isBlank()) return ""
  val grade = normalized.lastOrNull { it in 'A'..'E' } ?: return null
  return grade.toString()
}

private fun buildCapaianHafalanBulananLabel(report: MonthlyReportItem): String {
  return report.keteranganCapaianHafalanBulanan
    .trim()
    .ifBlank { report.nilaiCapaianHafalanBulanan.withPageSuffix() }
}

private fun akhlakDescriptionFromValue(value: String): String {
  val raw = value.trim().uppercase()
  val grade = when {
    raw in listOf("A", "B", "C", "D", "E") -> raw
    raw.toDoubleOrNull() != null -> {
      val number = raw.toDouble()
      when {
        number in 1.0..5.0 -> when (number.toInt().coerceIn(1, 5)) {
          5 -> "A"
          4 -> "B"
          3 -> "C"
          2 -> "D"
          else -> "E"
        }
        number >= 90.0 -> "A"
        number >= 80.0 -> "B"
        number >= 70.0 -> "C"
        number >= 60.0 -> "D"
        else -> "E"
      }
    }
    else -> ""
  }
  return when (grade) {
    "A" -> "Istimewa"
    "B" -> "Baik Sekali"
    "C" -> "Baik"
    "D" -> "Kurang"
    "E" -> "Sangat Kurang"
    else -> ""
  }
}

private fun gradeColor(
  grade: String,
  fallback: Color
): Color {
  return when (grade) {
    "A" -> Color(0xFF2563EB)
    "B" -> Color(0xFF16A34A)
    "C" -> Color(0xFFD97706)
    "D" -> Color(0xFFEA580C)
    "E" -> Color(0xFFDC2626)
    else -> fallback
  }
}

private fun buildJumlahHafalanLabel(report: MonthlyReportItem?): String {
  val keterangan = report?.keteranganJumlahHafalanBulanan.orDash()
  if (keterangan != "-") return keterangan
  val halaman = report?.nilaiJumlahHafalanHalaman.orDash()
  val juz = report?.nilaiJumlahHafalanJuz.orDash()
  if (halaman == "-" && juz == "-") return "-"
  return "$halaman halaman / $juz juz"
}

private fun String?.orDash(): String {
  return this?.trim()?.takeIf { it.isNotBlank() } ?: "-"
}

private fun String?.orZero(): String {
  return this?.trim()?.takeIf { it.isNotBlank() } ?: "0"
}

private fun String?.withPercentSuffix(): String {
  val value = this?.trim().orEmpty()
  if (value.isBlank()) return "-"
  return if (value.endsWith("%")) value else "$value%"
}

private fun String?.withPageSuffix(): String {
  val value = this?.trim().orEmpty()
  if (value.isBlank()) return "-"
  return if (value.contains("halaman", ignoreCase = true)) value else "$value halaman"
}

private fun String.toPeriodLabel(): String {
  val month = runCatching { YearMonth.parse(this) }.getOrNull() ?: return ifBlank { "Periode" }
  val monthName = month.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("id-ID"))
  return "$monthName ${month.year}"
}

private fun buildAvailableMonthlyPeriods(): List<String> {
  val start = YearMonth.from(LocalDate.now()).minusMonths(1)
  return (0 until 12).map { offset -> start.minusMonths(offset.toLong()).toString() }
}

private fun defaultMonthlyWhatsappTemplate(): String {
  return listOf(
    "Assalamu'alaikum warahmatullahi wabarakatuh",
    "",
    "Bapak/Ibu hafizakumullahu ta'ala",
    "",
    "Alhamdulillah kembali menyampaikan Laporan Evaluasi Perkembangan Santri bulan ini ananda <nama santri>.",
    "",
    "Mohon dibaca dengan seksama dan jika ada hal yang kurang jelas maka Ibu/Bapak bisa menanyakan secara langsung dengan menghubungi nomor penanggung jawab yang tertera.",
    "",
    "Laporan ini bisa menjadi catatan muhasabah untuk Ibu/Bapak atas perkembangan ananda selama sebulan di pondok.",
    "",
    "Semoga Allah SWT mengistiqamahkan ananda dalam kebaikan dan menjadikannya pribadi yang lebih baik ke depannya.",
    "",
    "Syukron wajazakumullahu khairan",
    "",
    "Link laporan:",
    "<link>"
  ).joinToString("\n")
}
