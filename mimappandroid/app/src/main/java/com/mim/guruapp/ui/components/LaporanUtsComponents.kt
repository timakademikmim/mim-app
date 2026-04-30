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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.UtsReportSaveOutcome
import com.mim.guruapp.data.model.GuruProfile
import com.mim.guruapp.data.model.UtsAttendanceSummary
import com.mim.guruapp.data.model.UtsReportOverride
import com.mim.guruapp.data.model.UtsReportPayload
import com.mim.guruapp.data.model.UtsReportSnapshot
import com.mim.guruapp.data.model.UtsReportSubject
import com.mim.guruapp.data.model.UtsSemesterInfo
import com.mim.guruapp.data.model.UtsSubjectOverride
import com.mim.guruapp.data.model.WaliSantriProfile
import com.mim.guruapp.data.model.WaliSantriSnapshot
import com.mim.guruapp.export.UtsReportExporter
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.round

private const val UTS_MAX_SCORE = 25.0
private const val UTS_REMEDIAL_THRESHOLD = 17.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanUtsScreen(
  waliSantriSnapshot: WaliSantriSnapshot,
  utsReportSnapshot: UtsReportSnapshot,
  profile: GuruProfile,
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onSaveOverride: suspend (UtsReportOverride) -> UtsReportSaveOutcome,
  onDetailModeChange: (Boolean) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val semesters = remember(utsReportSnapshot.semesters) {
    val availableSemesters = utsReportSnapshot.semesters.ifEmpty {
      listOf(UtsSemesterInfo(id = "", label = "Semester aktif"))
    }
    val activeAcademicYearId = availableSemesters.firstOrNull { it.isActive }?.tahunAjaranId.orEmpty()
    if (activeAcademicYearId.isBlank()) {
      availableSemesters
    } else {
      availableSemesters.filter { it.tahunAjaranId == activeAcademicYearId }.ifEmpty { availableSemesters }
    }
  }
  var selectedSemesterId by rememberSaveable {
    mutableStateOf(semesters.firstOrNull { it.isActive }?.id ?: semesters.firstOrNull()?.id.orEmpty())
  }
  var selectedSantriId by rememberSaveable { mutableStateOf<String?>(null) }
  var quickActionSantriId by rememberSaveable { mutableStateOf<String?>(null) }
  var isSaving by rememberSaveable { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  val snackbarScope = rememberCoroutineScope()

  LaunchedEffect(semesters) {
    if (selectedSemesterId !in semesters.map { it.id }) {
      selectedSemesterId = semesters.firstOrNull { it.isActive }?.id ?: semesters.firstOrNull()?.id.orEmpty()
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
  val selectedSemester = semesters.firstOrNull { it.id == selectedSemesterId } ?: semesters.firstOrNull() ?: UtsSemesterInfo()
  val payloads = remember(students, selectedSemesterId, utsReportSnapshot, profile.name) {
    students.associate { student ->
      student.id to buildUtsPayload(
        student = student,
        semester = selectedSemester,
        snapshot = utsReportSnapshot,
        waliName = profile.name
      )
    }
  }
  val selectedSantri = students.firstOrNull { it.id == selectedSantriId }
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
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    AnimatedContent(
      targetState = selectedSantri,
      transitionSpec = {
        val openingDetail = targetState != null
        fadeIn() + slideInHorizontally { width -> if (openingDetail) width / 5 else -width / 5 } togetherWith
          fadeOut() + slideOutHorizontally { width -> if (openingDetail) -width / 6 else width / 6 }
      },
      label = "laporan-pts-content",
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
          LaporanUtsListContent(
            isWaliKelas = waliSantriSnapshot.isWaliKelas,
            students = students,
            payloads = payloads,
            snapshot = utsReportSnapshot,
            profile = profile,
            selectedSemester = selectedSemester,
            semesters = semesters,
            selectedQuickActionSantriId = quickActionSantriId,
            onQuickActionSantriChange = { quickActionSantriId = it },
            onSemesterChange = { selectedSemesterId = it },
            showSkeleton = showSkeleton,
            isSaving = isSaving,
            onMenuClick = onMenuClick,
            showMessage = { message -> snackbarScope.launch { snackbarHostState.showSnackbar(message) } },
            onSaveOverride = { override ->
              isSaving = true
              val result = onSaveOverride(override)
              isSaving = false
              result
            },
            onSantriClick = { selectedSantriId = it.id }
          )
        }
      } else {
        val payload = payloads[activeSantri.id] ?: UtsReportPayload()
        LaporanUtsDetailContent(
          santri = activeSantri,
          payload = payload,
          existingOverride = utsReportSnapshot.overrides.find {
            it.studentId == activeSantri.id && it.semesterId == selectedSemester.id
          },
          guruId = utsReportSnapshot.guruId,
          isSaving = isSaving,
          onBackClick = { selectedSantriId = null },
          showMessage = { message -> snackbarScope.launch { snackbarHostState.showSnackbar(message) } },
          onSaveOverride = { override ->
            isSaving = true
            val result = onSaveOverride(override)
            isSaving = false
            snackbarScope.launch { snackbarHostState.showSnackbar(result.message) }
            result
          }
        )
      }
    }
  }
}

@Composable
private fun LaporanUtsListContent(
  isWaliKelas: Boolean,
  students: List<WaliSantriProfile>,
  payloads: Map<String, UtsReportPayload>,
  snapshot: UtsReportSnapshot,
  profile: GuruProfile,
  selectedSemester: UtsSemesterInfo,
  semesters: List<UtsSemesterInfo>,
  selectedQuickActionSantriId: String?,
  onQuickActionSantriChange: (String?) -> Unit,
  onSemesterChange: (String) -> Unit,
  showSkeleton: Boolean,
  isSaving: Boolean,
  onMenuClick: () -> Unit,
  showMessage: (String) -> Unit,
  onSaveOverride: suspend (UtsReportOverride) -> UtsReportSaveOutcome,
  onSantriClick: (WaliSantriProfile) -> Unit
) {
  var showImportDialog by rememberSaveable { mutableStateOf(false) }
  var isBulkResetting by rememberSaveable { mutableStateOf(false) }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val studentIds = remember(students) { students.map { it.id }.toSet() }
  val resettableOverrides = remember(snapshot.overrides, selectedSemester.id, studentIds) {
    snapshot.overrides.filter { override ->
      override.semesterId == selectedSemester.id && override.studentId in studentIds
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
    ) {
      LaporanUtsTopBar(
        title = "Laporan PTS",
        navigationIcon = Icons.Outlined.Menu,
        onNavigationClick = onMenuClick
      )

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        item {
          LaporanUtsControlCard(
            selectedSemester = selectedSemester,
            semesters = semesters,
            onSemesterChange = onSemesterChange,
            missingTable = snapshot.missingOverrideTable,
            onImportClick = { showImportDialog = true }
          )
        }

        if (resettableOverrides.isNotEmpty()) {
          item {
            LaporanUtsResetCard(
              count = resettableOverrides.size,
              isBusy = isSaving || isBulkResetting,
              title = "Reset Data PTS Semester Ini",
              body = "Kosongkan ${resettableOverrides.size} data edit/import agar laporan kembali memakai data asli sistem.",
              onClick = {
                if (!isSaving && !isBulkResetting) {
                  scope.launch {
                    isBulkResetting = true
                    var successCount = 0
                    for (override in resettableOverrides) {
                      val student = students.firstOrNull { it.id == override.studentId }
                      val targetGuruId = if (snapshot.guruId.isNotBlank()) snapshot.guruId else override.guruId
                      val targetClassId = student?.classId ?: override.classId
                      val result = onSaveOverride(
                        UtsReportOverride(
                          guruId = targetGuruId,
                          semesterId = selectedSemester.id,
                          classId = targetClassId,
                          studentId = override.studentId
                        )
                      )
                      if (result.success) successCount += 1
                    }
                    isBulkResetting = false
                    showMessage("Reset PTS selesai untuk $successCount santri.")
                  }
                }
              }
            )
          }
        }

        if (!isWaliKelas) {
          item {
            LaporanUtsInfoCard(
              icon = Icons.Outlined.WarningAmber,
              title = "Khusus wali kelas",
              body = "Laporan PTS menampilkan santri pada kelas wali. Akun ini belum terdeteksi sebagai wali kelas."
            )
          }
        }

        if (showSkeleton) {
          items(5) {
            SkeletonContentCard(modifier = Modifier.fillMaxWidth())
          }
        } else if (students.isEmpty()) {
          item {
            EmptyPlaceholderCard(
              message = "Belum ada santri. Data santri kelas wali belum tersedia. Tarik layar ke bawah untuk refresh data."
            )
          }
        } else {
          itemsIndexed(students, key = { _, student -> student.id }) { index, student ->
            val payload = payloads[student.id] ?: UtsReportPayload()
            LaporanUtsStudentCard(
              number = index + 1,
              student = student,
              payload = payload,
              hasOverride = snapshot.overrides.any {
                it.studentId == student.id && it.semesterId == selectedSemester.id
              },
              selectedForQuickAction = selectedQuickActionSantriId == student.id,
              onLongClick = {
                onQuickActionSantriChange(if (selectedQuickActionSantriId == student.id) null else student.id)
              },
              onClick = { onSantriClick(student) }
            )
          }
        }
      }
    }

    if (selectedQuickActionSantriId != null) {
      val payload = payloads[selectedQuickActionSantriId]
      if (payload != null) {
        LaporanUtsQuickActionBar(
          payload = payload,
          modifier = Modifier.align(Alignment.BottomCenter),
          onPrint = {
            scope.launch {
              runCatching {
                val pdf = UtsReportExporter.createPdfFile(context, payload)
                UtsReportExporter.printPdf(context, pdf, payload)
              }.onFailure { showMessage("Gagal menyiapkan PDF laporan PTS.") }
            }
          },
          onShare = {
            scope.launch {
              runCatching {
                val pdf = UtsReportExporter.createPdfFile(context, payload)
                val message = UtsReportExporter.buildWhatsappAttachmentMessage(payload)
                val phone = payload.findWhatsappTarget(students, profile)
                UtsReportExporter.sharePdfToWhatsApp(context, pdf, phone, message)
              }.onFailure { showMessage("Gagal menyiapkan lampiran WhatsApp.") }
            }
          }
        )
      }
    }

    SavingOverlay(
      visible = isSaving || isBulkResetting,
      title = if (isBulkResetting) "Mereset laporan PTS..." else "Menyimpan laporan PTS...",
      subtitle = "Data asli sistem tetap aman"
    )
  }

  if (showImportDialog) {
    LaporanUtsBulkImportDialog(
      students = students,
      payloads = payloads,
      snapshot = snapshot,
      selectedSemester = selectedSemester,
      profile = profile,
      onDismiss = { showImportDialog = false },
      showMessage = showMessage,
      onSaveOverride = onSaveOverride
    )
  }
}

@Composable
private fun LaporanUtsDetailContent(
  santri: WaliSantriProfile,
  payload: UtsReportPayload,
  existingOverride: UtsReportOverride?,
  guruId: String,
  isSaving: Boolean,
  onBackClick: () -> Unit,
  showMessage: (String) -> Unit,
  onSaveOverride: suspend (UtsReportOverride) -> UtsReportSaveOutcome
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var editMode by rememberSaveable(payload.studentId, payload.semesterId) { mutableStateOf(false) }
  var midCapaian by rememberSaveable(payload.studentId, payload.semesterId) { mutableStateOf(payload.midTahfizCapaian) }
  var midScore by rememberSaveable(payload.studentId, payload.semesterId) { mutableStateOf(payload.midTahfizScore) }
  var halaqahSakit by rememberSaveable(payload.studentId, payload.semesterId) { mutableStateOf(payload.halaqahSakitText) }
  var halaqahIzin by rememberSaveable(payload.studentId, payload.semesterId) { mutableStateOf(payload.halaqahIzinText) }
  val subjectScores = remember(payload.studentId, payload.semesterId) {
    mutableStateMapOf<String, String>().apply {
      payload.subjects.forEach { put(it.key, it.scoreText.takeIf { value -> value != "-" }.orEmpty()) }
    }
  }

  val draftPayload = remember(payload, midCapaian, midScore, halaqahSakit, halaqahIzin, subjectScores.toMap()) {
    payload.applyDraft(
      midCapaian = midCapaian,
      midScore = midScore,
      halaqahSakit = halaqahSakit,
      halaqahIzin = halaqahIzin,
      subjectScores = subjectScores
    )
  }
  val hasChanges = draftPayload != payload

  LaunchedEffect(payload) {
    if (!editMode) {
      midCapaian = payload.midTahfizCapaian
      midScore = payload.midTahfizScore
      halaqahSakit = payload.halaqahSakitText
      halaqahIzin = payload.halaqahIzinText
      subjectScores.clear()
      payload.subjects.forEach { subject ->
        subjectScores[subject.key] = subject.scoreText.takeIf { value -> value != "-" }.orEmpty()
      }
    }
  }

  BackHandler {
    if (editMode && hasChanges) {
      midCapaian = payload.midTahfizCapaian
      midScore = payload.midTahfizScore
      halaqahSakit = payload.halaqahSakitText
      halaqahIzin = payload.halaqahIzinText
      subjectScores.clear()
      payload.subjects.forEach { subject ->
        subjectScores[subject.key] = subject.scoreText.takeIf { value -> value != "-" }.orEmpty()
      }
      editMode = false
    } else {
      onBackClick()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
    ) {
      LaporanUtsTopBar(
        title = "Detail PTS",
        navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
        onNavigationClick = onBackClick,
        actionIcon = when {
          editMode && hasChanges -> Icons.Outlined.Check
          else -> Icons.Outlined.Edit
        },
        onActionClick = {
          if (editMode && hasChanges) {
            scope.launch {
              val result = onSaveOverride(draftPayload.toOverride(guruId))
              if (result.success) editMode = false
            }
          } else {
            editMode = !editMode
          }
        }
      )

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        item {
          LaporanUtsStudentHeaderCard(
            santri = santri,
            payload = draftPayload,
            hasOverride = existingOverride != null
          )
        }
        if (existingOverride != null) {
          item {
            LaporanUtsResetCard(
              count = 1,
              isBusy = isSaving,
              title = "Reset Data Santri Ini",
              body = "Kosongkan edit/import PTS ${santri.name} agar kembali memakai data asli sistem.",
              onClick = {
                if (!isSaving) {
                  scope.launch {
                    val result = onSaveOverride(draftPayload.toBlankOverride(guruId))
                    if (result.success) {
                      editMode = false
                      showMessage("Laporan PTS ${santri.name} dikembalikan ke data sistem.")
                    }
                  }
                }
              }
            )
          }
        }
        item {
          LaporanUtsSectionCard(
            title = "A. Mata Pelajaran",
            subtitle = "Nilai maksimal 25. Nilai di bawah 17 ditandai remedial."
          ) {
            draftPayload.subjects.forEach { subject ->
              LaporanUtsSubjectRow(
                subject = subject,
                editable = editMode,
                value = subjectScores[subject.key].orEmpty(),
                onValueChange = { subjectScores[subject.key] = sanitizeScoreInput(it) }
              )
            }
            LaporanUtsSummaryRow("Jumlah", draftPayload.totalScoreText)
            LaporanUtsSummaryRow("Rata-Rata", draftPayload.averageScoreText)
          }
        }
        item {
          LaporanUtsSectionCard(title = "B. Ujian Mid Semester Ketahfizan") {
            LaporanUtsEditableInfoRow("Capaian Hafalan", midCapaian, editMode, onValueChange = { midCapaian = it })
            LaporanUtsEditableInfoRow("Nilai", midScore, editMode, onValueChange = { midScore = sanitizeMidTahfizScoreInput(it) })
          }
        }
        item {
          LaporanUtsSectionCard(title = "C. Kehadiran") {
            LaporanUtsReadOnlyInfoRow("Kelas - Izin", draftPayload.kelasIzinText)
            LaporanUtsReadOnlyInfoRow("Kelas - Sakit", draftPayload.kelasSakitText)
            LaporanUtsEditableInfoRow("Halaqah - Izin", halaqahIzin, editMode, onValueChange = { halaqahIzin = sanitizeCountInput(it) })
            LaporanUtsEditableInfoRow("Halaqah - Sakit", halaqahSakit, editMode, onValueChange = { halaqahSakit = sanitizeCountInput(it) })
            if (draftPayload.attendanceRangeLabel.isNotBlank()) {
              Text(
                text = "Rentang: ${draftPayload.attendanceRangeLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = SubtleInk
              )
            }
          }
        }
      }
    }

    LaporanUtsQuickActionBar(
      payload = draftPayload,
      modifier = Modifier.align(Alignment.BottomCenter),
      onPrint = {
        scope.launch {
          runCatching {
            val pdf = UtsReportExporter.createPdfFile(context, draftPayload)
            UtsReportExporter.printPdf(context, pdf, draftPayload)
          }.onFailure { showMessage("Gagal menyiapkan PDF laporan PTS.") }
        }
      },
      onShare = {
        scope.launch {
          runCatching {
            val pdf = UtsReportExporter.createPdfFile(context, draftPayload)
            val message = UtsReportExporter.buildWhatsappAttachmentMessage(draftPayload)
            val phone = santri.guardianPhone.ifBlank { santri.fatherPhone.ifBlank { santri.motherPhone } }
            UtsReportExporter.sharePdfToWhatsApp(context, pdf, phone, message)
          }.onFailure { showMessage("Gagal menyiapkan lampiran WhatsApp.") }
        }
      }
    )

    SavingOverlay(
      visible = isSaving,
      title = "Menyimpan laporan PTS...",
      subtitle = "Data sistem tidak ikut diubah"
    )
  }
}

@Composable
private fun LaporanUtsTopBar(
  title: String,
  navigationIcon: ImageVector,
  onNavigationClick: () -> Unit,
  actionIcon: ImageVector? = null,
  onActionClick: () -> Unit = {}
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .shadow(10.dp, CircleShape, ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
        .clip(CircleShape)
        .background(CardBackground)
        .border(1.dp, CardBorder, CircleShape)
        .clickable(onClick = onNavigationClick),
      contentAlignment = Alignment.Center
    ) {
      Icon(navigationIcon, contentDescription = title, tint = PrimaryBlueDark)
    }
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .weight(1f)
        .padding(start = 14.dp)
    )
    if (actionIcon != null) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(PrimaryBlue.copy(alpha = 0.12f))
          .border(1.dp, PrimaryBlue.copy(alpha = 0.18f), CircleShape)
          .clickable(onClick = onActionClick),
        contentAlignment = Alignment.Center
      ) {
        Icon(actionIcon, contentDescription = "Aksi", tint = PrimaryBlue)
      }
    } else {
      Spacer(modifier = Modifier.width(42.dp))
    }
  }
}

@Composable
private fun LaporanUtsControlCard(
  selectedSemester: UtsSemesterInfo,
  semesters: List<UtsSemesterInfo>,
  onSemesterChange: (String) -> Unit,
  missingTable: Boolean,
  onImportClick: () -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(16.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x120F172A), spotColor = Color(0x120F172A))
      .clip(RoundedCornerShape(26.dp))
      .background(CardBackground.copy(alpha = 0.96f))
      .border(1.dp, CardBorder, RoundedCornerShape(26.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Periode PTS", style = MaterialTheme.typography.labelLarge, color = SubtleInk)
        Box {
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(SoftPanel)
              .clickable { expanded = true }
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = selectedSemester.label.ifBlank { "Pilih semester" },
              style = MaterialTheme.typography.bodyMedium,
              color = PrimaryBlueDark,
              fontWeight = FontWeight.SemiBold
            )
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Pilih semester", tint = SubtleInk)
          }
          DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            semesters.forEach { semester ->
              DropdownMenuItem(
                text = { Text(semester.label.ifBlank { "Semester" }) },
                onClick = {
                  expanded = false
                  onSemesterChange(semester.id)
                }
              )
            }
          }
        }
      }
      Button(onClick = onImportClick) {
        Icon(Icons.Outlined.UploadFile, contentDescription = "Input massal")
        Spacer(modifier = Modifier.width(8.dp))
        Text("Input Massal")
      }
    }
    Text(
      text = "Data dasar diambil dari nilai PTS sistem. Edit dan input massal disimpan sebagai override laporan saja.",
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
    if (missingTable) {
      Text(
        text = "Tabel override laporan_uts_input_massal belum tersedia, edit/input massal belum bisa disimpan.",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFB45309),
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
private fun LaporanUtsResetCard(
  count: Int,
  isBusy: Boolean,
  title: String,
  body: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(Color(0xFFFFF7ED))
      .border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(22.dp))
      .clickable(enabled = !isBusy, onClick = onClick)
      .padding(14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(Color.White.copy(alpha = 0.86f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Outlined.RestartAlt,
        contentDescription = title,
        tint = if (isBusy) SubtleInk.copy(alpha = 0.55f) else Color(0xFFEA580C)
      )
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = Color(0xFF9A3412),
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = body,
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
    if (count > 1) {
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.labelLarge,
        color = Color(0xFFEA580C),
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .background(Color.White.copy(alpha = 0.86f))
          .padding(horizontal = 10.dp, vertical = 6.dp)
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LaporanUtsStudentCard(
  number: Int,
  student: WaliSantriProfile,
  payload: UtsReportPayload,
  hasOverride: Boolean,
  selectedForQuickAction: Boolean,
  onLongClick: () -> Unit,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(22.dp))
      .background(if (selectedForQuickAction) PrimaryBlue.copy(alpha = 0.10f) else CardBackground)
      .border(1.dp, if (selectedForQuickAction) PrimaryBlue.copy(alpha = 0.24f) else CardBorder, RoundedCornerShape(22.dp))
      .combinedClickable(onClick = onClick, onLongClick = onLongClick)
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(PrimaryBlue.copy(alpha = 0.12f)),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = number.toString(),
        color = PrimaryBlue,
        fontWeight = FontWeight.ExtraBold
      )
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = student.name,
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        if (hasOverride) {
          Text(
            text = "Manual",
            style = MaterialTheme.typography.labelSmall,
            color = PrimaryBlue,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .background(PrimaryBlue.copy(alpha = 0.10f))
              .padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }
      Text(
        text = "NISN ${student.nisn.ifBlank { "-" }}",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .clip(RoundedCornerShape(18.dp))
        .background(utsScoreColorFor(payload.averageScoreText).copy(alpha = 0.10f))
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Text(
        text = payload.averageScoreText,
        style = MaterialTheme.typography.titleMedium,
        color = utsScoreColorFor(payload.averageScoreText),
        fontWeight = FontWeight.ExtraBold
      )
      Text("Rata-rata", style = MaterialTheme.typography.labelSmall, color = SubtleInk)
    }
  }
}

@Composable
private fun LaporanUtsStudentHeaderCard(
  santri: WaliSantriProfile,
  payload: UtsReportPayload,
  hasOverride: Boolean
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(24.dp))
      .background(CardBackground.copy(alpha = 0.96f))
      .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
      .padding(18.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(santri.name, style = MaterialTheme.typography.titleLarge, color = PrimaryBlueDark, fontWeight = FontWeight.ExtraBold)
    Text("NISN ${santri.nisn.ifBlank { "-" }} • ${santri.className.ifBlank { "-" }}", style = MaterialTheme.typography.bodyMedium, color = SubtleInk)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
      LaporanUtsMiniStat("Jumlah", payload.totalScoreText)
      LaporanUtsMiniStat("Rata-rata", payload.averageScoreText)
      if (hasOverride) {
        Text(
          text = "Manual",
          style = MaterialTheme.typography.labelMedium,
          color = PrimaryBlue,
          fontWeight = FontWeight.ExtraBold,
          modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(PrimaryBlue.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 7.dp)
        )
      }
    }
  }
}

@Composable
private fun LaporanUtsMiniStat(label: String, value: String) {
  val color = utsScoreColorFor(value, PrimaryBlueDark)
  Column(
    modifier = Modifier
      .clip(RoundedCornerShape(16.dp))
      .background(color.copy(alpha = 0.09f))
      .border(1.dp, color.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Text(value.ifBlank { "-" }, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.ExtraBold)
    Text(label, style = MaterialTheme.typography.labelSmall, color = SubtleInk)
  }
}

@Composable
private fun LaporanUtsSectionCard(
  title: String,
  subtitle: String = "",
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .background(CardBackground.copy(alpha = 0.96f))
      .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium, color = PrimaryBlueDark, fontWeight = FontWeight.ExtraBold)
      if (subtitle.isNotBlank()) {
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SubtleInk)
      }
    }
    content()
  }
}

@Composable
private fun LaporanUtsSubjectRow(
  subject: UtsReportSubject,
  editable: Boolean,
  value: String,
  onValueChange: (String) -> Unit
) {
  val displayValue = if (editable) value.ifBlank { "-" } else subject.scoreText.ifBlank { "-" }
  val valueColor = utsScoreColorFor(displayValue, PrimaryBlueDark)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color.White.copy(alpha = 0.76f))
      .border(1.dp, CardBorder.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
      .padding(horizontal = 12.dp, vertical = 11.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Text(
        text = subject.name,
        style = MaterialTheme.typography.bodyMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = "Batas remedial ${subject.kkmText.ifBlank { UTS_REMEDIAL_THRESHOLD.toInt().toString() }}",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
    if (editable) {
      OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.width(76.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleSmall.copy(
          textAlign = TextAlign.Center,
          fontWeight = FontWeight.ExtraBold,
          color = valueColor
        ),
        colors = laporanUtsTextFieldColors()
      )
    } else {
      Text(
        text = displayValue,
        style = MaterialTheme.typography.titleLarge,
        color = valueColor,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(56.dp)
      )
    }
  }
}

@Composable
private fun LaporanUtsSummaryRow(label: String, value: String) {
  val valueColor = utsScoreColorFor(value, PrimaryBlueDark)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(PrimaryBlue.copy(alpha = 0.07f))
      .border(1.dp, PrimaryBlue.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = PrimaryBlueDark, fontWeight = FontWeight.Bold)
    Text(value.ifBlank { "-" }, style = MaterialTheme.typography.titleSmall, color = valueColor, fontWeight = FontWeight.ExtraBold)
  }
}

@Composable
private fun LaporanUtsEditableInfoRow(
  label: String,
  value: String,
  editable: Boolean,
  onValueChange: (String) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color.White.copy(alpha = 0.76f))
      .border(1.dp, CardBorder.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(7.dp)
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = PrimaryBlueDark, fontWeight = FontWeight.ExtraBold)
    if (editable) {
      OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = false,
        minLines = 1,
        colors = laporanUtsTextFieldColors()
      )
    } else {
      Text(
        text = value.ifBlank { "-" },
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
private fun LaporanUtsReadOnlyInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color.White.copy(alpha = 0.76f))
      .border(1.dp, CardBorder.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
      .padding(horizontal = 12.dp, vertical = 11.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = PrimaryBlueDark, fontWeight = FontWeight.ExtraBold)
    Text(value.ifBlank { "0" }, style = MaterialTheme.typography.titleSmall, color = PrimaryBlueDark, fontWeight = FontWeight.ExtraBold)
  }
}

@Composable
private fun LaporanUtsQuickActionBar(
  payload: UtsReportPayload,
  modifier: Modifier = Modifier,
  onPrint: () -> Unit,
  onShare: () -> Unit
) {
  Row(
    modifier = modifier
      .navigationBarsPadding()
      .padding(horizontal = 18.dp, vertical = 10.dp)
      .shadow(18.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x180F172A), spotColor = Color(0x180F172A))
      .clip(RoundedCornerShape(32.dp))
      .background(Color.White.copy(alpha = 0.96f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(32.dp))
      .padding(horizontal = 10.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    LaporanUtsBottomAction(Icons.Outlined.Print, "Cetak", onPrint)
    LaporanUtsBottomAction(Icons.AutoMirrored.Outlined.Message, "Kirim", onShare)
  }
}

@Composable
private fun LaporanUtsBottomAction(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .height(52.dp)
      .clip(RoundedCornerShape(24.dp))
      .background(PrimaryBlue.copy(alpha = 0.12f))
      .clickable(onClick = onClick)
      .padding(horizontal = 18.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = label, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(start = 8.dp)
    )
  }
}

@Composable
private fun LaporanUtsInfoCard(icon: ImageVector, title: String, body: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(Color(0xFFFFFBEB))
      .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(22.dp))
      .padding(16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = title, tint = Color(0xFFB45309))
    Column {
      Text(title, style = MaterialTheme.typography.titleSmall, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
      Text(body, style = MaterialTheme.typography.bodySmall, color = Color(0xFF92400E))
    }
  }
}

@Composable
private fun LaporanUtsBulkImportDialog(
  students: List<WaliSantriProfile>,
  payloads: Map<String, UtsReportPayload>,
  snapshot: UtsReportSnapshot,
  selectedSemester: UtsSemesterInfo,
  profile: GuruProfile,
  onDismiss: () -> Unit,
  showMessage: (String) -> Unit,
  onSaveOverride: suspend (UtsReportOverride) -> UtsReportSaveOutcome
) {
  var link by rememberSaveable { mutableStateOf("") }
  var isLoading by rememberSaveable { mutableStateOf(false) }
  var preview by remember { mutableStateOf<List<UtsBulkPreviewRow>>(emptyList()) }
  val acceptedReviewIds = remember { mutableStateMapOf<String, Boolean>() }
  val scope = rememberCoroutineScope()

  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        enabled = preview.any { it.canSave(acceptedReviewIds[it.id] == true) } && !isLoading,
        onClick = {
          scope.launch {
            isLoading = true
            val rows = preview.filter { it.canSave(acceptedReviewIds[it.id] == true) }
            var successCount = 0
            rows.forEach { row ->
              val student = row.student ?: return@forEach
              val payload = payloads[student.id] ?: return@forEach
              val override = row.toOverride(
                guruId = snapshot.guruId,
                semesterId = selectedSemester.id,
                classId = student.classId,
                basePayload = payload
              )
              val result = onSaveOverride(override)
              if (result.success) successCount += 1
            }
            isLoading = false
            showMessage("Input massal PTS tersimpan untuk $successCount santri.")
            onDismiss()
          }
        }
      ) { Text("Simpan Preview") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Tutup") }
    },
    title = { Text("Input Massal Laporan PTS") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "Masukkan link Google Sheet/CSV. Sistem akan mencocokkan NISN/nama santri dan kolom mapel, lalu menyimpan hasilnya sebagai override laporan.",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
        OutlinedTextField(
          value = link,
          onValueChange = { link = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Link spreadsheet / CSV") },
          singleLine = false,
          minLines = 2
        )
        Button(
          enabled = link.isNotBlank() && !isLoading,
          onClick = {
            scope.launch {
              isLoading = true
              runCatching {
                val csv = fetchUtsBulkCsv(link, students.map { it.className }.distinct())
                buildUtsBulkPreview(
                  csv = csv,
                  students = students,
                  payloads = payloads
                )
              }.onSuccess { rows ->
                preview = rows
                acceptedReviewIds.clear()
                rows.filter { it.status == UtsBulkStatus.NeedsReview }.forEach { acceptedReviewIds[it.id] = false }
                showMessage("Preview input massal siap: ${rows.size} baris.")
              }.onFailure { error ->
                showMessage(error.message ?: "Gagal membaca spreadsheet.")
              }
              isLoading = false
            }
          }
        ) {
          if (isLoading) Text("Membaca...") else Text("Ambil Preview")
        }
        if (preview.isNotEmpty()) {
          LazyColumn(
            modifier = Modifier.height(320.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(preview, key = { it.id }) { row ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .background(SoftPanel)
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                if (row.status == UtsBulkStatus.NeedsReview) {
                  Checkbox(
                    checked = acceptedReviewIds[row.id] == true,
                    onCheckedChange = { acceptedReviewIds[row.id] = it }
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(row.student?.name ?: row.rawName.ifBlank { "-" }, fontWeight = FontWeight.Bold, color = PrimaryBlueDark)
                  Text(row.message, style = MaterialTheme.typography.bodySmall, color = SubtleInk)
                  if (row.changedFields.isNotEmpty()) {
                    Text(row.changedFields.joinToString(", "), style = MaterialTheme.typography.labelSmall, color = PrimaryBlue)
                  }
                }
              }
            }
          }
        }
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun laporanUtsTextFieldColors() = TextFieldDefaults.colors(
  focusedIndicatorColor = PrimaryBlue,
  unfocusedIndicatorColor = CardBorder,
  focusedTextColor = PrimaryBlueDark,
  unfocusedTextColor = PrimaryBlueDark,
  focusedContainerColor = Color.White.copy(alpha = 0.78f),
  unfocusedContainerColor = Color.White.copy(alpha = 0.78f)
)

private fun buildUtsPayload(
  student: WaliSantriProfile,
  semester: UtsSemesterInfo,
  snapshot: UtsReportSnapshot,
  waliName: String
): UtsReportPayload {
  val baseSubjects = snapshot.classSubjects
    .filter { it.classId == student.classId && it.semesterId == semester.id }
    .distinctBy { it.mapelId }
  val scoreByMapel = snapshot.scoreRows
    .filter { it.studentId == student.id && it.semesterId == semester.id }
    .associateBy { it.mapelId }
  val override = snapshot.overrides.find { it.studentId == student.id && it.semesterId == semester.id }
  val overrideByKey = override?.subjects.orEmpty().associateBy { it.key.ifBlank { normalizeUtsSubjectKey(it.name) } }
  val subjects = baseSubjects.map { subject ->
    val key = normalizeUtsSubjectKey(subject.name)
    val score = scoreByMapel[subject.mapelId]
    val overrideScore = overrideByKey[key]
    UtsReportSubject(
      key = key,
      name = subject.name.ifBlank { "-" },
      kkmText = "17",
      scoreText = overrideScore?.scoreText?.ifBlank { null } ?: score?.scoreText?.ifBlank { null } ?: "-",
      scoreValue = overrideScore?.scoreValue ?: overrideScore?.scoreText?.toDoubleOrNull() ?: score?.scoreValue
    )
  }.sortedWith(compareBy<UtsReportSubject> { utsSubjectOrderIndex(it.name) }.thenBy { it.name.lowercase(Locale.ROOT) })
  val summary = buildUtsScoreSummary(subjects)
  val attendance = snapshot.attendanceSummaries.find { it.studentId == student.id && it.semesterId == semester.id }
  return UtsReportPayload(
    studentId = student.id,
    studentName = student.name.ifBlank { "-" },
    studentNisn = student.nisn.ifBlank { "-" },
    classId = student.classId,
    className = student.className.ifBlank { "-" },
    waliKelasName = waliName.ifBlank { "-" },
    semesterId = semester.id,
    semesterLabel = semester.label.ifBlank { "Semester" },
    subjects = subjects,
    totalScoreText = summary.first,
    averageScoreText = summary.second,
    kelasIzinText = (attendance?.kelasIzinCount ?: 0).toString(),
    kelasSakitText = (attendance?.kelasSakitCount ?: 0).toString(),
    halaqahIzinText = override?.halaqahIzinText.orEmpty(),
    halaqahSakitText = override?.halaqahSakitText.orEmpty(),
    midTahfizCapaian = override?.midTahfizCapaian.orEmpty(),
    midTahfizScore = override?.midTahfizScore.orEmpty(),
    ptsDateIso = attendance?.ptsDateIso.orEmpty(),
    attendanceRangeLabel = attendance?.rangeLabel.orEmpty().ifBlank { "-" }
  )
}

private fun UtsReportPayload.applyDraft(
  midCapaian: String,
  midScore: String,
  halaqahSakit: String,
  halaqahIzin: String,
  subjectScores: Map<String, String>
): UtsReportPayload {
  val nextSubjects = subjects.map { subject ->
    val scoreText = subjectScores[subject.key].orEmpty().ifBlank { "-" }
    subject.copy(scoreText = scoreText, scoreValue = scoreText.toDoubleOrNull())
  }
  val summary = buildUtsScoreSummary(nextSubjects)
  return copy(
    subjects = nextSubjects,
    totalScoreText = summary.first,
    averageScoreText = summary.second,
    midTahfizCapaian = midCapaian.trim(),
    midTahfizScore = midScore.trim(),
    halaqahSakitText = halaqahSakit.trim(),
    halaqahIzinText = halaqahIzin.trim()
  )
}

private fun UtsReportPayload.toOverride(guruId: String): UtsReportOverride {
  return UtsReportOverride(
    guruId = guruId,
    semesterId = semesterId,
    classId = classId,
    studentId = studentId,
    midTahfizCapaian = midTahfizCapaian,
    midTahfizScore = midTahfizScore,
    halaqahSakitText = halaqahSakitText,
    halaqahIzinText = halaqahIzinText,
    subjects = subjects.map {
      UtsSubjectOverride(
        key = it.key,
        name = it.name,
        scoreText = it.scoreText.takeIf { value -> value != "-" }.orEmpty(),
        scoreValue = it.scoreValue
      )
    }
  )
}

private fun UtsReportPayload.toBlankOverride(guruId: String): UtsReportOverride {
  return UtsReportOverride(
    guruId = guruId,
    semesterId = semesterId,
    classId = classId,
    studentId = studentId
  )
}

private fun buildUtsScoreSummary(subjects: List<UtsReportSubject>): Pair<String, String> {
  val scores = subjects.mapNotNull { it.scoreValue ?: it.scoreText.toDoubleOrNull() }
  if (scores.isEmpty()) return "-" to "-"
  val total = round(scores.sum() * 100.0) / 100.0
  val average = round((total / scores.size) * 100.0) / 100.0
  return formatUtsNumber(total) to formatUtsNumber(average)
}

private fun sanitizeScoreInput(value: String): String {
  return sanitizeDecimalScoreInput(value, UTS_MAX_SCORE)
}

private fun sanitizeMidTahfizScoreInput(value: String): String {
  return sanitizeDecimalScoreInput(value, 100.0)
}

private fun sanitizeDecimalScoreInput(
  value: String,
  maxValue: Double
): String {
  val normalized = value.replace(",", ".").filter { it.isDigit() || it == '.' }
  val number = normalized.toDoubleOrNull()
  return when {
    normalized.isBlank() -> ""
    number == null -> normalized.take(5)
    number > maxValue -> formatUtsNumber(maxValue)
    else -> normalized.take(6)
  }
}

private fun sanitizeCountInput(value: String): String {
  return value.filter(Char::isDigit).take(3)
}

private fun formatUtsNumber(value: Double): String {
  return if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(Locale.US, value).trimEnd('0').trimEnd('.')
}

private fun utsScoreColorFor(
  value: String,
  fallback: Color = Color(0xFF0F172A)
): Color {
  val score = value.replace(",", ".")
    .let { Regex("""-?\d+(\.\d+)?""").find(it)?.value?.toDoubleOrNull() }
    ?: return fallback
  return when {
    score < UTS_REMEDIAL_THRESHOLD -> Color(0xFFDC2626)
    score >= 23.0 -> Color(0xFF2563EB)
    score >= 21.0 -> Color(0xFF16A34A)
    score >= 19.0 -> Color(0xFFD97706)
    else -> Color(0xFFEA580C)
  }
}

private fun utsSubjectOrderIndex(name: String): Int {
  val key = normalizeUtsSubjectKey(name)
  val index = UtsSubjectOrder.indexOfFirst { it == key || key.contains(it) || it.contains(key) }
  return if (index >= 0) index else UtsSubjectOrder.size + 10
}

private val UtsSubjectOrder = listOf(
  "bhs arab",
  "akhlak",
  "tafsir",
  "hadits",
  "akidah",
  "fikih",
  "sirah",
  "nahwu",
  "sharf",
  "matematika",
  "ipa",
  "ips",
  "pkn",
  "bhs inggris",
  "bhs indonesia"
)

private fun normalizeUtsSubjectKey(value: String): String {
  return value.lowercase(Locale.ROOT)
    .replace("bahasa", "bhs")
    .replace("aqidah", "akidah")
    .replace("hadis", "hadits")
    .replace("fiqih", "fikih")
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()
    .replace(Regex("\\s+"), " ")
}

private fun UtsReportPayload.findWhatsappTarget(
  students: List<WaliSantriProfile>,
  profile: GuruProfile
): String {
  val student = students.firstOrNull { it.id == studentId }
  return student?.guardianPhone?.ifBlank { student.fatherPhone.ifBlank { student.motherPhone } }.orEmpty()
    .ifBlank { profile.phoneNumber }
}

private data class UtsBulkPreviewRow(
  val id: String,
  val status: UtsBulkStatus,
  val student: WaliSantriProfile?,
  val rawName: String,
  val message: String,
  val changedFields: List<String>,
  val subjectScores: Map<String, String>,
  val midCapaian: String = "",
  val midScore: String = "",
  val halaqahSakit: String = "",
  val halaqahIzin: String = ""
) {
  fun canSave(reviewAccepted: Boolean): Boolean {
    return (status == UtsBulkStatus.Ready || (status == UtsBulkStatus.NeedsReview && reviewAccepted)) &&
      student != null &&
      (changedFields.isNotEmpty() || subjectScores.isNotEmpty())
  }

  fun toOverride(
    guruId: String,
    semesterId: String,
    classId: String,
    basePayload: UtsReportPayload
  ): UtsReportOverride {
    return basePayload.applyDraft(
      midCapaian = midCapaian.ifBlank { basePayload.midTahfizCapaian },
      midScore = midScore.ifBlank { basePayload.midTahfizScore },
      halaqahSakit = halaqahSakit.ifBlank { basePayload.halaqahSakitText },
      halaqahIzin = halaqahIzin.ifBlank { basePayload.halaqahIzinText },
      subjectScores = basePayload.subjects.associate { subject ->
        subject.key to (subjectScores[subject.key] ?: subject.scoreText.takeIf { it != "-" }.orEmpty())
      }
    ).toOverride(guruId).copy(semesterId = semesterId, classId = classId)
  }
}

private enum class UtsBulkStatus {
  Ready,
  NeedsReview,
  NotFound,
  Duplicate,
  NoChange
}

private suspend fun fetchUtsBulkCsv(
  rawUrl: String,
  classNames: List<String>
): String = withContext(Dispatchers.IO) {
  val spreadsheetId = Regex("""/spreadsheets/d/([a-zA-Z0-9-_]+)""").find(rawUrl)?.groupValues?.getOrNull(1)
  val gid = Regex("""[?&]gid=([0-9]+)""").find(rawUrl)?.groupValues?.getOrNull(1)
  val classTargets = classNames.map { it.trim() }.filter { it.isNotBlank() }.distinct()
  val isGoogleSheetUrl = spreadsheetId != null ||
    rawUrl.contains("docs.google.com/spreadsheets", ignoreCase = true)
  val candidates = buildList {
    if (!isGoogleSheetUrl && (rawUrl.contains("output=csv", ignoreCase = true) || rawUrl.contains("format=csv", ignoreCase = true))) {
      add(rawUrl)
    }
    if (spreadsheetId != null) {
      classTargets.flatMap(::utsClassSheetAliases).distinct().forEach { sheet ->
        add("https://docs.google.com/spreadsheets/d/$spreadsheetId/gviz/tq?tqx=out:csv&sheet=${encodeUtsSheetName(sheet)}")
        add("https://docs.google.com/spreadsheets/d/$spreadsheetId/gviz/tq?tqx=out:csv&sheet=${encodeUtsSheetName("'$sheet'")}")
        add("https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&sheet=${encodeUtsSheetName(sheet)}")
      }
      if (classTargets.isEmpty()) {
        if (rawUrl.contains("output=csv", ignoreCase = true) || rawUrl.contains("format=csv", ignoreCase = true)) {
          add(rawUrl)
        }
        if (gid != null) {
          add("https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&gid=$gid")
        }
        add("https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv")
      }
    }
  }.ifEmpty { listOf(rawUrl) }

  var lastError = ""
  for (candidate in candidates.distinct()) {
    runCatching {
      val connection = (URL(candidate).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 15_000
        readTimeout = 20_000
      }
      try {
        val code = connection.responseCode
        val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (code in 200..299 && body.isLikelyCsv()) return@withContext body
        lastError = "Tab spreadsheet belum bisa dibaca."
      } finally {
        connection.disconnect()
      }
    }.onFailure { lastError = it.message.orEmpty() }
  }
  error(lastError.ifBlank { "Spreadsheet tidak bisa dibaca." })
}

private fun buildUtsBulkPreview(
  csv: String,
  students: List<WaliSantriProfile>,
  payloads: Map<String, UtsReportPayload>
): List<UtsBulkPreviewRow> {
  val rows = parseCsvRows(csv).filter { row -> row.any { it.isNotBlank() } }
  if (rows.size < 2) error("CSV tidak memiliki data santri.")
  val headers = rows.first().map(::normalizeSpreadsheetKey)
  val nameIndex = headers.indexOfFirst { it in setOf("nama", "nama santri", "santri", "name") }
  val nisnIndex = headers.indexOfFirst { it in setOf("nisn", "nomor induk", "no induk", "id santri") }
  val classIndex = headers.indexOfFirst { it in setOf("kelas", "class") }
  if (nameIndex < 0 && nisnIndex < 0) error("Kolom nama atau NISN tidak ditemukan.")

  return rows.drop(1).mapIndexedNotNull { index, row ->
    val rawName = row.getOrNull(nameIndex).orEmpty()
    val rawNisn = row.getOrNull(nisnIndex).orEmpty()
    val rawClass = row.getOrNull(classIndex).orEmpty()
    val match = matchUtsBulkStudent(rawNisn, rawName, rawClass, students)
    val student = match.second
    val payload = student?.let { payloads[it.id] }
    val subjectScores = linkedMapOf<String, String>()
    val changedFields = mutableListOf<String>()
    if (payload != null) {
      headers.forEachIndexed { columnIndex, header ->
        val value = row.getOrNull(columnIndex).orEmpty().trim()
        if (value.isBlank()) return@forEachIndexed
        val subject = payload.subjects.firstOrNull { subject ->
          val key = normalizeUtsSubjectKey(header)
          key == subject.key || key.contains(subject.key) || subject.key.contains(key)
        }
        if (subject != null) {
          val clean = sanitizeScoreInput(value)
          if (clean.isNotBlank()) {
            subjectScores[subject.key] = clean
            changedFields += subject.name
          }
        }
      }
    }
    val midCapaian = valueByAlias(row, headers, "capaian hafalan", "capaian tahfiz", "capaian tahfizh")
    val midScore = valueByAlias(
      row,
      headers,
      "nilai tahfiz",
      "nilai tahfizh",
      "nilai ketahfizan",
      "mid tahfiz",
      "al-qur'an",
      "al-quran",
      "al quran",
      "alquran",
      "qur'an",
      "quran",
      "nilai al-qur'an",
      "nilai al-quran",
      "nilai al quran",
      "nilai alquran"
    )
    val sakit = valueByAlias(row, headers, "sakit halaqah", "halaqah sakit")
    val izin = valueByAlias(row, headers, "izin halaqah", "halaqah izin")
    if (midCapaian.isNotBlank()) changedFields += "Capaian Hafalan"
    if (midScore.isNotBlank()) changedFields += "Nilai Tahfiz"
    if (sakit.isNotBlank()) changedFields += "Sakit Halaqah"
    if (izin.isNotBlank()) changedFields += "Izin Halaqah"
    val status = when {
      match.first != UtsBulkStatus.Ready && match.first != UtsBulkStatus.NeedsReview -> match.first
      changedFields.isEmpty() -> UtsBulkStatus.NoChange
      else -> match.first
    }
    UtsBulkPreviewRow(
      id = "uts-bulk-$index-${student?.id.orEmpty()}",
      status = status,
      student = student,
      rawName = rawName,
      message = match.third,
      changedFields = changedFields.distinct(),
      subjectScores = subjectScores,
      midCapaian = midCapaian,
      midScore = sanitizeMidTahfizScoreInput(midScore),
      halaqahSakit = sanitizeCountInput(sakit),
      halaqahIzin = sanitizeCountInput(izin)
    )
  }
}

private fun matchUtsBulkStudent(
  nisn: String,
  name: String,
  className: String,
  students: List<WaliSantriProfile>
): Triple<UtsBulkStatus, WaliSantriProfile?, String> {
  val cleanNisn = nisn.filter(Char::isDigit)
  if (cleanNisn.isNotBlank()) {
    val matches = students.filter { it.nisn.filter(Char::isDigit) == cleanNisn }
    if (matches.size == 1) return Triple(UtsBulkStatus.Ready, matches.first(), "Cocok berdasarkan NISN.")
    if (matches.size > 1) return Triple(UtsBulkStatus.Duplicate, null, "NISN cocok ke lebih dari satu santri.")
  }
  val normalizedName = normalizeStudentNameForUtsBulk(name)
  if (normalizedName.isBlank()) return Triple(UtsBulkStatus.NotFound, null, "Nama/NISN kosong.")
  val classAliases = utsClassSheetAliases(className).map(::normalizeSpreadsheetKey).toSet()
  val exact = students.filter { student ->
    normalizeStudentNameForUtsBulk(student.name) == normalizedName &&
      (className.isBlank() || normalizeSpreadsheetKey(student.className) in classAliases)
  }
  if (exact.size == 1) return Triple(UtsBulkStatus.Ready, exact.first(), "Cocok berdasarkan nama.")
  if (exact.size > 1) return Triple(UtsBulkStatus.Duplicate, null, "Nama cocok ke lebih dari satu santri.")
  val suggestions = students
    .map { it to studentNameSimilarityScore(normalizedName, normalizeStudentNameForUtsBulk(it.name)) }
    .filter { it.second >= 0.68 }
    .sortedByDescending { it.second }
    .take(3)
  if (suggestions.isNotEmpty()) {
    val target = suggestions.first().first
    return Triple(UtsBulkStatus.NeedsReview, target, "Perlu cek: kemungkinan cocok dengan ${target.name}.")
  }
  return Triple(UtsBulkStatus.NotFound, null, "Santri $name tidak ditemukan.")
}

private fun valueByAlias(row: List<String>, headers: List<String>, vararg aliases: String): String {
  val aliasSet = aliases.map(::normalizeSpreadsheetKey).toSet()
  val index = headers.indexOfFirst { it in aliasSet }
  return row.getOrNull(index).orEmpty().trim()
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
      char == '"' && quoted && csv.getOrNull(index + 1) == '"' -> {
        cell.append('"')
        index += 1
      }
      char == '"' -> quoted = !quoted
      char == ',' && !quoted -> {
        row += cell.toString().trim()
        cell.clear()
      }
      (char == '\n' || char == '\r') && !quoted -> {
        if (char == '\r' && csv.getOrNull(index + 1) == '\n') index += 1
        row += cell.toString().trim()
        cell.clear()
        rows += row
        row = mutableListOf()
      }
      else -> cell.append(char)
    }
    index += 1
  }
  row += cell.toString().trim()
  if (row.any { it.isNotBlank() }) rows += row
  return rows
}

private fun normalizeSpreadsheetKey(value: String): String {
  return value.lowercase(Locale.ROOT)
    .replace("'", "")
    .replace(".", " ")
    .replace(Regex("[^a-z0-9%]+"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()
}

private fun normalizeStudentNameForUtsBulk(value: String): String {
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
    .joinToString(" ")
}

private fun studentNameSimilarityScore(a: String, b: String): Double {
  if (a.isBlank() || b.isBlank()) return 0.0
  if (a == b) return 1.0
  val aTokens = a.split(" ").filter { it.isNotBlank() }.toSet()
  val bTokens = b.split(" ").filter { it.isNotBlank() }.toSet()
  if (aTokens.isEmpty() || bTokens.isEmpty()) return 0.0
  val overlap = aTokens.intersect(bTokens).size.toDouble()
  val dice = (2.0 * overlap) / (aTokens.size + bTokens.size)
  val containsBonus = if (a.contains(b) || b.contains(a)) 0.18 else 0.0
  return (dice + containsBonus).coerceAtMost(1.0)
}

private fun utsClassSheetAliases(className: String): List<String> {
  val trimmed = className.trim()
  if (trimmed.isBlank()) return emptyList()
  val withoutPrefix = trimmed
    .replace(Regex("""^(kelas|kls|class)\s*""", RegexOption.IGNORE_CASE), "")
    .trim()
  val baseForms = listOf(trimmed, withoutPrefix)
    .filter { it.isNotBlank() }
    .flatMap(::utsClassNameShapeVariants)
  val gradeOnlyForms = baseForms.mapNotNull(::extractUtsClassGradeToken)
    .flatMap(::utsClassNameShapeVariants)
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

private fun utsClassNameShapeVariants(value: String): List<String> {
  val spaced = value.replace(".", " ").replace(Regex("\\s+"), " ").trim()
  val dotted = spaced.replace(" ", ".")
  val compact = spaced.replace(" ", "")
  val numeric = utsRomanClassToNumber(spaced)
  val roman = utsNumberClassToRoman(spaced)
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

private fun extractUtsClassGradeToken(value: String): String? {
  val spaced = value.replace(".", " ").replace(Regex("\\s+"), " ").trim()
  return Regex("""^(VII|VIII|IX|X|7|8|9|10)\b""", RegexOption.IGNORE_CASE)
    .find(spaced)
    ?.value
    ?.uppercase(Locale.ROOT)
}

private fun utsRomanClassToNumber(value: String): String {
  return value
    .replace(Regex("""\bVII\b""", RegexOption.IGNORE_CASE), "7")
    .replace(Regex("""\bVIII\b""", RegexOption.IGNORE_CASE), "8")
    .replace(Regex("""\bIX\b""", RegexOption.IGNORE_CASE), "9")
    .replace(Regex("""\bX\b""", RegexOption.IGNORE_CASE), "10")
}

private fun utsNumberClassToRoman(value: String): String {
  return value
    .replace(Regex("""\b7\b"""), "VII")
    .replace(Regex("""\b8\b"""), "VIII")
    .replace(Regex("""\b9\b"""), "IX")
    .replace(Regex("""\b10\b"""), "X")
}

private fun encodeUtsSheetName(sheet: String): String {
  return URLEncoder.encode(sheet, Charsets.UTF_8.name()).replace("+", "%20")
}

private fun String.isLikelyCsv(): Boolean {
  val trimmed = trimStart()
  if (trimmed.isBlank()) return false
  val lower = trimmed.take(300).lowercase(Locale.ROOT)
  if (lower.startsWith("<!doctype") || lower.startsWith("<html") || lower.startsWith("{")) return false
  if ("unable to parse" in lower || "invalid query" in lower || "not found" in lower) return false
  return contains(",") || contains("\n")
}
