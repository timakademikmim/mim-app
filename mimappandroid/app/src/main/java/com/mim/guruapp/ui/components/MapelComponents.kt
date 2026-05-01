package com.mim.guruapp.ui.components

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import com.mim.guruapp.AttendanceSaveOutcome
import com.mim.guruapp.PatronMateriSaveOutcome
import com.mim.guruapp.ScoreSaveOutcome
import com.mim.guruapp.data.model.AttendanceHistoryEntry
import com.mim.guruapp.data.model.AttendanceStudent
import com.mim.guruapp.data.model.MapelAttendanceSnapshot
import com.mim.guruapp.data.model.MapelPatronMateriSnapshot
import com.mim.guruapp.data.model.MapelScoreSnapshot
import com.mim.guruapp.data.model.AvailableSubjectOffer
import com.mim.guruapp.data.model.PatronMateriItem
import com.mim.guruapp.data.model.ScoreDetailRow
import com.mim.guruapp.data.model.ScoreStudent
import com.mim.guruapp.data.model.SubjectOverview
import com.mim.guruapp.data.model.SyncBannerState
import com.mim.guruapp.export.MapelQuestionExportData
import com.mim.guruapp.export.MapelQuestionExporter
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.HighlightCard
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SubtleInk
import com.mim.guruapp.ui.theme.SuccessTint
import com.mim.guruapp.ui.theme.WarmAccent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private data class MapelCardVisual(
  val background: Color,
  val iconTint: Color,
  val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapelScreen(
  subjects: List<SubjectOverview>,
  syncBanner: SyncBannerState,
  isClaimSectionVisible: Boolean,
  availableSubjects: List<AvailableSubjectOffer>,
  selectedClaimSubjectIds: Set<String>,
  onToggleClaimSection: () -> Unit,
  onToggleClaimSubject: (String) -> Unit,
  onClearClaimSelection: () -> Unit,
  onClaimSelectedSubjects: () -> Unit,
  onLoadAttendance: suspend (String, SubjectOverview) -> MapelAttendanceSnapshot?,
  onSaveAttendance: suspend (String, SubjectOverview, String, List<AttendanceHistoryEntry>) -> AttendanceSaveOutcome,
  onDeleteAttendance: suspend (String, SubjectOverview, List<String>) -> AttendanceSaveOutcome,
  onLoadScores: suspend (String, SubjectOverview) -> MapelScoreSnapshot?,
  onSaveScores: suspend (String, SubjectOverview, ScoreStudent) -> ScoreSaveOutcome,
  onSaveScoresBatch: suspend (String, SubjectOverview, List<ScoreStudent>) -> ScoreSaveOutcome,
  onLoadPatronMateri: suspend (String, SubjectOverview) -> MapelPatronMateriSnapshot?,
  onSavePatronMateri: suspend (String, SubjectOverview, List<PatronMateriItem>) -> PatronMateriSaveOutcome,
  isRefreshing: Boolean,
  onRefresh: () -> Unit,
  onDetailModeChange: (Boolean) -> Unit = {},
  onMenuClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
  var selectedDetailSectionKey by rememberSaveable { mutableStateOf(MapelDetailSection.Absensi.name) }
  var showUnavailableMapelMessage by rememberSaveable { mutableStateOf(false) }
  val selectedDetailSection = remember(selectedDetailSectionKey) {
    runCatching { MapelDetailSection.valueOf(selectedDetailSectionKey) }
      .getOrDefault(MapelDetailSection.Absensi)
  }

  LaunchedEffect(selectedSubjectId) {
    onDetailModeChange(selectedSubjectId != null)
  }

  BackHandler(enabled = selectedSubjectId != null) {
    selectedSubjectId = null
    selectedDetailSectionKey = MapelDetailSection.Absensi.name
  }

  AnimatedContent(
    targetState = selectedSubjectId,
    transitionSpec = {
      val openingDetail = targetState != null
      fadeIn(animationSpec = tween(180)) +
        slideInHorizontally(animationSpec = tween(260)) { width -> if (openingDetail) width / 8 else -width / 8 } togetherWith
        fadeOut(animationSpec = tween(140)) +
        slideOutHorizontally(animationSpec = tween(220)) { width -> if (openingDetail) -width / 10 else width / 10 }
    },
    modifier = modifier.fillMaxSize(),
    label = "mapel-list-detail-transition"
  ) { targetSubjectId ->
    val targetSubject = subjects.firstOrNull { it.id == targetSubjectId }
    if (targetSubject != null) {
      MapelDetailScreen(
        subject = targetSubject,
        onLoadAttendance = onLoadAttendance,
        onSaveAttendance = onSaveAttendance,
        onDeleteAttendance = onDeleteAttendance,
        onLoadScores = onLoadScores,
        onSaveScores = onSaveScores,
        onSaveScoresBatch = onSaveScoresBatch,
        onLoadPatronMateri = onLoadPatronMateri,
        onSavePatronMateri = onSavePatronMateri,
        selectedSection = selectedDetailSection,
        onSectionChange = { selectedDetailSectionKey = it.name },
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        onBackClick = {
          selectedSubjectId = null
          selectedDetailSectionKey = MapelDetailSection.Absensi.name
        },
        modifier = Modifier.fillMaxSize()
      )
    } else {
      Scaffold(
        modifier = Modifier
          .fillMaxSize()
          .background(AppBackground),
        containerColor = Color.Transparent,
        topBar = {
          MapelTopBar(
            title = "Mapel",
            onMenuClick = onMenuClick
          )
        }
      ) { innerPadding ->
        PullToRefreshBox(
          isRefreshing = isRefreshing,
          onRefresh = onRefresh,
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
          LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            item(span = { GridItemSpan(2) }) {
              MapelActionBar(
                isClaimSectionVisible = isClaimSectionVisible,
                hasAvailableSubjects = availableSubjects.isNotEmpty(),
                onToggleClaimSection = {
                  if (availableSubjects.isNotEmpty()) {
                    showUnavailableMapelMessage = false
                    onToggleClaimSection()
                  } else {
                    showUnavailableMapelMessage = true
                  }
                }
              )
            }

            if (showUnavailableMapelMessage && availableSubjects.isEmpty()) {
              item(span = { GridItemSpan(2) }) {
                MapelUnavailableInfoBox(
                  message = "Tidak ada mapel yang tersedia untuk ditambahkan.",
                  onDismiss = { showUnavailableMapelMessage = false }
                )
              }
            }

            if (isClaimSectionVisible && availableSubjects.isNotEmpty()) {
              item(span = { GridItemSpan(2) }) {
                AvailableMapelPanel(
                  subjects = availableSubjects,
                  selectedIds = selectedClaimSubjectIds,
                  onToggleSubject = onToggleClaimSubject,
                  onClearSelection = onClearClaimSelection,
                  onClaimSelectedSubjects = onClaimSelectedSubjects
                )
              }
            }

            if (subjects.isEmpty()) {
              item(span = { GridItemSpan(2) }) {
                EmptyPlaceholderCard("Belum ada data mapel untuk guru ini.")
              }
            } else {
              items(subjects, key = { it.id }) { subject ->
                SubjectGridCard(
                  subject = subject,
                  onClick = { selectedSubjectId = subject.id }
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MapelUnavailableInfoBox(
  message: String,
  onDismiss: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(Color.White.copy(alpha = 0.92f))
      .border(1.dp, WarmAccent.copy(alpha = 0.26f), RoundedCornerShape(22.dp))
      .padding(horizontal = 16.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(38.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(WarmAccent.copy(alpha = 0.16f))
        .border(1.dp, WarmAccent.copy(alpha = 0.24f), RoundedCornerShape(14.dp)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Outlined.MenuBook,
        contentDescription = null,
        tint = WarmAccent
      )
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        text = "Mapel Tidak Tersedia",
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = SubtleInk
      )
    }
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(999.dp))
        .background(Color.White.copy(alpha = 0.8f))
        .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(999.dp))
        .clickable(onClick = onDismiss)
        .padding(horizontal = 14.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "Tutup",
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
private fun MapelActionBar(
  isClaimSectionVisible: Boolean,
  hasAvailableSubjects: Boolean,
  onToggleClaimSection: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.End
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(999.dp))
        .background(Color.White.copy(alpha = 0.90f))
        .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
        .clickable(onClick = onToggleClaimSection)
        .padding(horizontal = 18.dp, vertical = 11.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = if (isClaimSectionVisible) "Tutup" else "Tambah Mapel",
        style = MaterialTheme.typography.labelLarge,
        color = if (hasAvailableSubjects) PrimaryBlueDark else SubtleInk,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
private fun SubjectGridCard(
  subject: SubjectOverview,
  onClick: () -> Unit
) {
  val visual = remember(subject.id, subject.title) { resolveMapelVisual(subject) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x120F172A), spotColor = Color(0x120F172A))
      .clip(RoundedCornerShape(20.dp))
      .background(visual.background.copy(alpha = 0.92f))
      .border(1.dp, Color.White.copy(alpha = 0.54f), RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(visual.iconTint.copy(alpha = 0.18f))
        .border(1.dp, visual.iconTint.copy(alpha = 0.18f), RoundedCornerShape(14.dp)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = visual.icon,
        contentDescription = subject.title,
        tint = visual.iconTint
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = subject.title,
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = buildMapelDescription(subject),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
      )
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(999.dp))
        .background(Color.White.copy(alpha = 0.84f))
        .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "Lihat detail",
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapelDetailScreen(
  subject: SubjectOverview,
  onLoadAttendance: suspend (String, SubjectOverview) -> MapelAttendanceSnapshot?,
  onSaveAttendance: suspend (String, SubjectOverview, String, List<AttendanceHistoryEntry>) -> AttendanceSaveOutcome,
  onDeleteAttendance: suspend (String, SubjectOverview, List<String>) -> AttendanceSaveOutcome,
  onLoadScores: suspend (String, SubjectOverview) -> MapelScoreSnapshot?,
  onSaveScores: suspend (String, SubjectOverview, ScoreStudent) -> ScoreSaveOutcome,
  onSaveScoresBatch: suspend (String, SubjectOverview, List<ScoreStudent>) -> ScoreSaveOutcome,
  onLoadPatronMateri: suspend (String, SubjectOverview) -> MapelPatronMateriSnapshot?,
  onSavePatronMateri: suspend (String, SubjectOverview, List<PatronMateriItem>) -> PatronMateriSaveOutcome,
  selectedSection: MapelDetailSection,
  onSectionChange: (MapelDetailSection) -> Unit,
  isRefreshing: Boolean,
  onRefresh: () -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var attendanceSnapshot by remember(subject.id) { mutableStateOf<MapelAttendanceSnapshot?>(null) }
  var scoreSnapshot by remember(subject.id) { mutableStateOf<MapelScoreSnapshot?>(null) }
  var patronMateriSnapshot by remember(subject.id) { mutableStateOf<MapelPatronMateriSnapshot?>(null) }
  var patronMateriItems by remember(subject.id) { mutableStateOf<List<PatronMateriItem>>(emptyList()) }
  var questionItems by remember(subject.id) { mutableStateOf<List<MapelQuestionDraft>>(emptyList()) }
  var isCreatingQuestion by remember(subject.id) { mutableStateOf(false) }
  var isAddingQuestionModel by remember(subject.id) { mutableStateOf(false) }
  var activeQuestionId by rememberSaveable(subject.id) { mutableStateOf<String?>(null) }
  var questionUndoStack by remember(subject.id) { mutableStateOf<List<MapelQuestionDraft>>(emptyList()) }
  var isPrintingQuestion by remember(subject.id) { mutableStateOf(false) }
  var isLoadingAttendance by remember(subject.id) { mutableStateOf(true) }
  var isLoadingScores by remember(subject.id) { mutableStateOf(true) }
  var isLoadingPatronMateri by remember(subject.id) { mutableStateOf(true) }
  var attendanceSortModeKey by rememberSaveable(subject.id) {
    mutableStateOf(AttendanceSortMode.ByStudent.name)
  }
  var scoreSortModeKey by rememberSaveable(subject.id) {
    mutableStateOf(ScoreSortMode.ByStudent.name)
  }
  val attendanceSortMode = remember(attendanceSortModeKey) {
    runCatching { AttendanceSortMode.valueOf(attendanceSortModeKey) }
      .getOrDefault(AttendanceSortMode.ByStudent)
  }
  val scoreSortMode = remember(scoreSortModeKey) {
    runCatching { ScoreSortMode.valueOf(scoreSortModeKey) }
      .getOrDefault(ScoreSortMode.ByStudent)
  }
  val detailScope = rememberCoroutineScope()
  val context = LocalContext.current
  val activeQuestion = remember(questionItems, activeQuestionId) {
    questionItems.firstOrNull { it.id == activeQuestionId }
  }
  val isQuestionEditorOpen = selectedSection == MapelDetailSection.Soal && activeQuestion != null

  fun updateQuestionDraft(updated: MapelQuestionDraft) {
    val nextItems = questionItems.map { current ->
      if (current.id == updated.id) updated.copy(updatedAt = System.currentTimeMillis()) else current
    }.sortedByDescending { it.updatedAt }
    questionItems = nextItems
    saveMapelQuestions(context, subject.id, nextItems)
  }

  fun addQuestionModel(question: MapelQuestionDraft, typeKey: String) {
    val selectedType = SoalTypeOptions.firstOrNull { it.key == typeKey } ?: SoalTypeOptions.first()
    val newSection = MapelQuestionSection(
      id = "model-${System.currentTimeMillis()}",
      typeKey = selectedType.key,
      typeLabel = selectedType.label,
      count = 0,
      choiceQuestions = emptyList<MapelChoiceQuestion>().withTrailingBlankQuestion()
    )
    val nextSections = question.sections + newSection
    updateQuestionDraft(
      question.copy(
        form = nextSections.map { it.typeLabel }.distinct().joinToString(", "),
        sections = nextSections
      )
    )
  }

  fun printQuestion(question: MapelQuestionDraft) {
    if (!isPrintingQuestion) {
      detailScope.launch {
        isPrintingQuestion = true
        runCatching {
          val exportData = question.toExportData()
          val pdf = MapelQuestionExporter.createPdfFile(context, subject, exportData)
          MapelQuestionExporter.printPdf(context, pdf, exportData)
        }
        isPrintingQuestion = false
      }
    }
  }

  suspend fun saveStudentAttendanceChanges(
    studentId: String,
    changes: List<AttendanceHistoryEntry>
  ): AttendanceSaveOutcome {
    val outcome = onSaveAttendance(subject.id, subject, studentId, changes)
    if (outcome.success) {
      attendanceSnapshot = onLoadAttendance(subject.id, subject) ?: attendanceSnapshot
    }
    return outcome
  }

  suspend fun deleteAttendanceRows(
    rowIds: List<String>
  ): AttendanceSaveOutcome {
    val outcome = onDeleteAttendance(subject.id, subject, rowIds)
    if (outcome.success) {
      attendanceSnapshot = onLoadAttendance(subject.id, subject) ?: attendanceSnapshot
    }
    return outcome
  }

  suspend fun saveStudentScoreChanges(
    student: ScoreStudent
  ): ScoreSaveOutcome {
    val outcome = onSaveScores(subject.id, subject, student)
    if (outcome.success) {
      scoreSnapshot = onLoadScores(subject.id, subject) ?: scoreSnapshot
    }
    return outcome
  }

  suspend fun saveStudentScoreChangesBatch(
    students: List<ScoreStudent>
  ): ScoreSaveOutcome {
    val outcome = onSaveScoresBatch(subject.id, subject, students)
    if (outcome.success) {
      scoreSnapshot = onLoadScores(subject.id, subject) ?: scoreSnapshot
    }
    return outcome
  }

  suspend fun savePatronMateriChanges(
    items: List<PatronMateriItem>
  ): PatronMateriSaveOutcome {
    val outcome = onSavePatronMateri(subject.id, subject, items)
    if (outcome.success) {
      patronMateriSnapshot = onLoadPatronMateri(subject.id, subject) ?: patronMateriSnapshot
      patronMateriItems = patronMateriSnapshot?.items.orEmpty()
    }
    return outcome
  }

  LaunchedEffect(subject.id) {
    isLoadingAttendance = true
    isLoadingScores = true
    isLoadingPatronMateri = true
    questionItems = loadMapelQuestions(context, subject.id)
    attendanceSnapshot = onLoadAttendance(subject.id, subject)
    scoreSnapshot = onLoadScores(subject.id, subject)
    patronMateriSnapshot = onLoadPatronMateri(subject.id, subject)
    patronMateriItems = patronMateriSnapshot?.items.orEmpty()
    isLoadingAttendance = false
    isLoadingScores = false
    isLoadingPatronMateri = false
  }

  LaunchedEffect(selectedSection, activeQuestionId, activeQuestion) {
    if (selectedSection != MapelDetailSection.Soal || (activeQuestionId != null && activeQuestion == null)) {
      activeQuestionId = null
    }
  }

  LaunchedEffect(activeQuestionId) {
    questionUndoStack = emptyList()
  }

  BackHandler(enabled = selectedSection == MapelDetailSection.Soal && activeQuestionId != null) {
    activeQuestionId = null
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    topBar = {
      MapelDetailTopBar(
        title = selectedSection.label,
        showUndo = isQuestionEditorOpen && questionUndoStack.isNotEmpty(),
        onUndoClick = {
          val restored = questionUndoStack.lastOrNull()
          if (restored != null) {
            questionUndoStack = questionUndoStack.dropLast(1)
            updateQuestionDraft(restored)
          }
        },
        onBackClick = {
          if (isQuestionEditorOpen) {
            activeQuestionId = null
          } else {
            onBackClick()
          }
        }
      )
    }
  ) { innerPadding ->
    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = onRefresh,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = if (isQuestionEditorOpen) 18.dp else 96.dp)
        ) {
          AbsensiMapelHeaderCard(
            subject = subject,
            modifier = Modifier.padding(top = 12.dp, bottom = 14.dp)
          )

          when (selectedSection) {
            MapelDetailSection.Absensi -> {
              AttendanceSortToggle(
                selectedMode = attendanceSortMode,
                onSelectMode = { attendanceSortModeKey = it.name },
                modifier = Modifier.padding(bottom = 14.dp)
              )
            }

            MapelDetailSection.Nilai -> {
              ScoreSortToggle(
                selectedMode = scoreSortMode,
                onSelectMode = { scoreSortModeKey = it.name },
                modifier = Modifier.padding(bottom = 14.dp)
              )
            }

            MapelDetailSection.PatronMateri -> {
              PatronMateriToolbar(
                totalMateri = patronMateriItems.count { it.text.isNotBlank() },
                onAddMateri = {
                  patronMateriItems = listOf(PatronMateriItem(
                    id = "draft-${System.currentTimeMillis()}",
                    text = ""
                  )) + patronMateriItems
                },
                modifier = Modifier.padding(bottom = 14.dp)
              )
            }

            MapelDetailSection.Soal -> {
              if (activeQuestion == null) {
                SoalToolbar(
                  totalSoal = questionItems.size,
                  isPrinting = isPrintingQuestion,
                  onAddSoal = { isCreatingQuestion = true },
                  onPrintAll = {
                    if (questionItems.isNotEmpty() && !isPrintingQuestion) {
                      detailScope.launch {
                        isPrintingQuestion = true
                        runCatching {
                          val combined = questionItems.toCombinedExportData(subject)
                          val pdf = MapelQuestionExporter.createPdfFile(context, subject, combined)
                          MapelQuestionExporter.printPdf(context, pdf, combined)
                        }
                        isPrintingQuestion = false
                      }
                    }
                  },
                  modifier = Modifier.padding(bottom = 14.dp)
                )
              }
            }
          }

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
          ) {
            if (selectedSection == MapelDetailSection.Absensi) {
              when {
                isLoadingAttendance -> {
                  items(4) { index ->
                    AttendanceSkeletonCard(index = index + 1)
                  }
                }

                attendanceSnapshot == null -> {
                  item {
                    EmptyPlaceholderCard("Data absensi belum tersedia. Coba buka kembali saat koneksi aktif.")
                  }
                }

                attendanceSnapshot?.students?.isEmpty() != false -> {
                  item {
                    EmptyPlaceholderCard("Belum ada santri aktif untuk kelas ini.")
                  }
                }

                else -> {
                  if (attendanceSortMode == AttendanceSortMode.ByStudent) {
                    items(
                      items = attendanceSnapshot?.students.orEmpty(),
                      key = { it.id }
                    ) { student ->
                      AttendanceCard(
                        student = student,
                        onSaveAttendance = ::saveStudentAttendanceChanges,
                        onDeleteAttendance = ::deleteAttendanceRows
                      )
                    }
                  } else {
                    val dateOverviews = buildAttendanceDateOverviews(attendanceSnapshot?.students.orEmpty())
                    if (dateOverviews.isEmpty()) {
                      item {
                        EmptyPlaceholderCard("Belum ada tanggal absensi yang bisa ditampilkan untuk mapel ini.")
                      }
                    } else {
                      items(
                        items = dateOverviews,
                        key = { it.dateIso }
                      ) { overview ->
                        AttendanceDateCard(
                          overview = overview,
                          onDeleteAttendance = ::deleteAttendanceRows,
                          onSaveAttendance = saveByDate@{ changesByStudent ->
                            var lastOutcome = AttendanceSaveOutcome(true, "Absensi berhasil diperbarui.")
                            for ((studentId, changes) in changesByStudent) {
                              val outcome = saveStudentAttendanceChanges(studentId, changes)
                              if (!outcome.success) return@saveByDate outcome
                              lastOutcome = outcome
                            }
                            lastOutcome
                          }
                        )
                      }
                    }
                  }
                }
              }
            } else if (selectedSection == MapelDetailSection.Nilai) {
              when {
                isLoadingScores -> {
                  items(4) { index ->
                    ScoreSkeletonCard(index = index + 1)
                  }
                }

                scoreSnapshot == null -> {
                  item {
                    EmptyPlaceholderCard("Data nilai belum tersedia. Coba buka kembali saat koneksi aktif.")
                  }
                }

                scoreSnapshot?.students?.isEmpty() != false -> {
                  item {
                    EmptyPlaceholderCard("Belum ada santri aktif untuk kelas ini.")
                  }
                }

                else -> {
                  when (scoreSortMode) {
                    ScoreSortMode.ByStudent -> {
                      items(
                        items = scoreSnapshot?.students.orEmpty(),
                        key = { it.id }
                      ) { student ->
                        ScoreCard(
                          student = student,
                          onSaveScores = ::saveStudentScoreChanges
                        )
                      }
                    }

                    ScoreSortMode.ByType -> {
                      val typeOverviews = buildScoreTypeOverviews(scoreSnapshot?.students.orEmpty())
                      items(
                        items = typeOverviews,
                        key = { it.key }
                      ) { overview ->
                        ScoreTypeCard(
                          overview = overview,
                          onSaveScores = saveByType@{ students ->
                            var lastOutcome = ScoreSaveOutcome(true, "Nilai berhasil diperbarui.")
                            for (student in students) {
                              val outcome = saveStudentScoreChanges(student)
                              if (!outcome.success) return@saveByType outcome
                              lastOutcome = outcome
                            }
                            lastOutcome
                          }
                        )
                      }
                    }

                    ScoreSortMode.ByDate -> {
                      val dateOverviews = buildScoreDateOverviews(scoreSnapshot?.students.orEmpty())
                      if (dateOverviews.isEmpty()) {
                        item {
                          EmptyPlaceholderCard("Belum ada tanggal nilai yang bisa ditampilkan untuk mapel ini.")
                        }
                      } else {
                        items(
                          items = dateOverviews,
                          key = { it.key }
                        ) { overview ->
                          ScoreDateCard(
                            overview = overview,
                            onSaveScores = ::saveStudentScoreChangesBatch
                          )
                        }
                      }
                    }
                  }
                }
              }
            } else if (selectedSection == MapelDetailSection.PatronMateri) {
              when {
                isLoadingPatronMateri -> {
                  items(3) { index ->
                    PatronMateriSkeletonCard(index = index + 1)
                  }
                }

                patronMateriSnapshot == null -> {
                  item {
                    EmptyPlaceholderCard("Data patron materi belum tersedia. Coba buka kembali saat koneksi aktif.")
                  }
                }

                patronMateriItems.isEmpty() -> {
                  item {
                    EmptyPlaceholderCard("Belum ada patron materi untuk mapel ini. Tekan Tambah Materi untuk mulai mengisi.")
                  }
                }

                else -> {
                  items(
                    items = patronMateriItems,
                    key = { it.id }
                  ) { item ->
                    PatronMateriCard(
                      item = item,
                      onTextChange = { text ->
                        patronMateriItems = patronMateriItems.map { current ->
                          if (current.id == item.id) current.copy(text = text) else current
                        }
                      },
                      onSave = {
                        detailScope.launch {
                          val cleanItems = preparePatronMateriItemsForSave(patronMateriItems)
                          val outcome = savePatronMateriChanges(cleanItems)
                          if (outcome.success) {
                            patronMateriItems = patronMateriSnapshot?.items.orEmpty()
                          }
                        }
                      },
                      onDelete = {
                        detailScope.launch {
                          val nextItems = patronMateriItems.filterNot { it.id == item.id }
                          patronMateriItems = nextItems
                          savePatronMateriChanges(nextItems)
                        }
                      }
                    )
                  }
                }
              }
            } else {
              val openedQuestion = activeQuestion
              if (openedQuestion != null) {
                item {
                  SoalWorkspaceScreen(
                    question = openedQuestion,
                    onQuestionChange = ::updateQuestionDraft,
                    onBeforeDelete = { snapshot ->
                      questionUndoStack = (questionUndoStack + snapshot).takeLast(20)
                    }
                  )
                }
              } else if (questionItems.isEmpty()) {
                item {
                  EmptyPlaceholderCard("Belum ada soal. Tekan Buat Soal untuk menyusun soal bebas tanpa jadwal ujian.")
                }
              } else {
                items(
                  items = questionItems,
                  key = { it.id }
                ) { question ->
                  SoalCard(
                    question = question,
                    onEdit = { activeQuestionId = question.id },
                    onPrint = {
                      if (!isPrintingQuestion) {
                        detailScope.launch {
                          isPrintingQuestion = true
                          runCatching {
                            val exportData = question.toExportData()
                            val pdf = MapelQuestionExporter.createPdfFile(context, subject, exportData)
                            MapelQuestionExporter.printPdf(context, pdf, exportData)
                          }
                          isPrintingQuestion = false
                        }
                      }
                    },
                    onDelete = {
                      val nextItems = questionItems.filterNot { it.id == question.id }
                      questionItems = nextItems
                      saveMapelQuestions(context, subject.id, nextItems)
                    }
                  )
                }
              }
            }
          }
        }

        if (!isQuestionEditorOpen) {
          MapelDetailBottomNav(
            selectedSection = selectedSection,
            onSelectSection = onSectionChange,
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .navigationBarsPadding()
              .padding(horizontal = 18.dp, vertical = 10.dp)
          )
        } else {
          SoalEditorFabMenu(
            isPrinting = isPrintingQuestion,
            onAddModel = { isAddingQuestionModel = true },
            onPrint = { activeQuestion?.let(::printQuestion) },
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .navigationBarsPadding()
              .padding(end = 20.dp, bottom = 20.dp)
          )
        }

        if (isCreatingQuestion) {
          SoalCreateDialog(
            subject = subject,
            onDismiss = { isCreatingQuestion = false },
            onSave = { created ->
              val nextItems = (questionItems + created)
                .sortedByDescending { it.updatedAt }
              questionItems = nextItems
              saveMapelQuestions(context, subject.id, nextItems)
              isCreatingQuestion = false
              activeQuestionId = created.id
            }
          )
        }

        val questionForAddModel = activeQuestion
        if (isAddingQuestionModel && questionForAddModel != null) {
          SoalAddModelDialog(
            onDismiss = { isAddingQuestionModel = false },
            onAdd = { typeKey ->
              addQuestionModel(questionForAddModel, typeKey)
              isAddingQuestionModel = false
            }
          )
        }
      }
    }
  }
}

@Composable
private fun SoalToolbar(
  totalSoal: Int,
  isPrinting: Boolean,
  onAddSoal: () -> Unit,
  onPrintAll: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "$totalSoal dokumen soal",
      style = MaterialTheme.typography.labelLarge,
      color = SubtleInk,
      fontWeight = FontWeight.SemiBold
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SoalPillButton(
        label = if (isPrinting) "Menyiapkan..." else "Cetak Semua",
        icon = Icons.Outlined.Print,
        enabled = totalSoal > 0 && !isPrinting,
        onClick = onPrintAll
      )
      SoalPillButton(
        label = "Buat Soal",
        icon = Icons.Outlined.Add,
        onClick = onAddSoal
      )
    }
  }
}

@Composable
private fun SoalPillButton(
  label: String,
  icon: ImageVector,
  enabled: Boolean = true,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(if (enabled) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.58f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(7.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (enabled) PrimaryBlue else SubtleInk.copy(alpha = 0.58f),
      modifier = Modifier.size(18.dp)
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (enabled) PrimaryBlueDark else SubtleInk.copy(alpha = 0.68f),
      fontWeight = FontWeight.SemiBold
    )
  }
}

@Composable
private fun SoalCard(
  question: MapelQuestionDraft,
  onEdit: () -> Unit,
  onPrint: () -> Unit,
  onDelete: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(22.dp))
      .background(Color.White.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.86f), RoundedCornerShape(22.dp))
      .clickable(onClick = onEdit)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(PrimaryBlue.copy(alpha = 0.12f))
          .border(1.dp, PrimaryBlue.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Outlined.Quiz, contentDescription = null, tint = PrimaryBlue)
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = question.title.ifBlank { "Soal tanpa judul" },
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = listOf(question.category, question.form, question.dateIso).filter { it.isNotBlank() }.joinToString(" | "),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    Text(
      text = question.previewText(),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      SoalMiniChip("${question.questionCount()} nomor")
      SoalMiniChip("${question.sections.size} model")
      SoalMiniChip(question.statusLabel)
      Spacer(modifier = Modifier.weight(1f))
      SoalIconAction(Icons.Outlined.Print, "Cetak", onPrint)
      SoalIconAction(Icons.Outlined.Delete, "Hapus", onDelete, tint = Color(0xFFDC2626))
    }
  }
}

@Composable
private fun SoalMiniChip(label: String) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(PrimaryBlue.copy(alpha = 0.08f))
      .border(1.dp, PrimaryBlue.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
      .padding(horizontal = 10.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.SemiBold
    )
  }
}

@Composable
private fun SoalIconAction(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  tint: Color = PrimaryBlue
) {
  Box(
    modifier = Modifier
      .size(38.dp)
      .clip(CircleShape)
      .background(tint.copy(alpha = 0.10f))
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(19.dp))
  }
}

@Composable
private fun SoalCreateDialog(
  subject: SubjectOverview,
  onDismiss: () -> Unit,
  onSave: (MapelQuestionDraft) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(SoalCategoryOptions.first()) }
  var dateIso by remember { mutableStateOf(LocalDate.now().toString()) }
  val canSave = title.trim().isNotBlank()

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(26.dp))
        .background(Color.White.copy(alpha = 0.98f))
        .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(26.dp))
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = "Buat Soal",
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = "Isi data awal dulu, detailnya diedit setelah file dibuat.",
            style = MaterialTheme.typography.bodySmall,
            color = SubtleInk
          )
        }
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFFF8FAFC))
            .clickable(onClick = onDismiss),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = PrimaryBlueDark)
        }
      }

      SoalTextField(title, { title = it }, "Judul soal")
      SoalDropdownField(
        label = "Jenis soal",
        selectedLabel = selectedCategory,
        options = SoalCategoryOptions,
        onSelect = { selectedCategory = it }
      )
      SoalTextField(
        value = dateIso,
        onValueChange = { dateIso = it },
        label = "Tanggal",
        keyboardType = KeyboardType.Number
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(999.dp))
            .clickable(onClick = onDismiss)
            .padding(vertical = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Text("Batal", color = SubtleInk, fontWeight = FontWeight.SemiBold)
        }
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(999.dp))
            .background(if (canSave) PrimaryBlue else PrimaryBlue.copy(alpha = 0.36f))
            .clickable(enabled = canSave) {
              onSave(
                MapelQuestionDraft(
                  id = "soal-${System.currentTimeMillis()}",
                  title = title.trim().ifBlank { "Soal ${subject.title}" },
                  category = selectedCategory,
                  form = "",
                  dateIso = dateIso.trim().ifBlank { LocalDate.now().toString() },
                  updatedAt = System.currentTimeMillis()
                )
              )
            }
            .padding(vertical = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun SoalWorkspaceScreen(
  question: MapelQuestionDraft,
  onQuestionChange: (MapelQuestionDraft) -> Unit,
  onBeforeDelete: (MapelQuestionDraft) -> Unit
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    if (question.sections.isEmpty()) {
      EmptyPlaceholderCard("Belum ada model soal. Tekan tombol + di kanan bawah untuk menambahkan model.")
    } else {
      question.sections.forEachIndexed { index, section ->
        SoalSectionEditorCard(
          section = section,
          index = index,
          onChange = { updated ->
            val nextSections = question.sections.map { current ->
              if (current.id == updated.id) updated else current
            }
            onQuestionChange(
              question.copy(
                form = nextSections.map { it.typeLabel }.distinct().joinToString(", "),
                sections = nextSections
              )
            )
          },
          onDelete = {
            onBeforeDelete(question)
            val nextSections = question.sections.filterNot { it.id == section.id }
            onQuestionChange(
              question.copy(
                form = nextSections.map { it.typeLabel }.distinct().joinToString(", "),
                sections = nextSections
              )
            )
          },
          onDeleteQuestion = { questionIndex ->
            onBeforeDelete(question)
            val nextSections = question.sections.map { currentSection ->
              if (currentSection.id == section.id) {
                val nextQuestions = currentSection.choiceQuestions
                  .withTrailingBlankQuestion()
                  .filterIndexed { index, _ -> index != questionIndex }
                  .withTrailingBlankQuestion()
                currentSection.copy(
                  count = nextQuestions.filledQuestionCount(),
                  choiceQuestions = nextQuestions
                )
              } else {
                currentSection
              }
            }
            onQuestionChange(
              question.copy(
                form = nextSections.map { it.typeLabel }.distinct().joinToString(", "),
                sections = nextSections
              )
            )
          }
        )
      }
    }

    Spacer(modifier = Modifier.height(78.dp))
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SoalSectionEditorCard(
  section: MapelQuestionSection,
  index: Int,
  onChange: (MapelQuestionSection) -> Unit,
  onDelete: () -> Unit,
  onDeleteQuestion: (Int) -> Unit
) {
  val palette = section.modelPalette()
  var showDeleteAction by rememberSaveable(section.id) { mutableStateOf(false) }
  val activeQuestionCount = section.effectiveQuestionCount()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = palette.accent.copy(alpha = 0.10f), spotColor = palette.accent.copy(alpha = 0.18f))
      .clip(RoundedCornerShape(24.dp))
      .background(palette.background)
      .border(1.dp, palette.border, RoundedCornerShape(24.dp))
      .combinedClickable(
        onClick = { if (showDeleteAction) showDeleteAction = false },
        onLongClick = { showDeleteAction = true }
      )
      .padding(15.dp),
    verticalArrangement = Arrangement.spacedBy(13.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.accent.copy(alpha = 0.16f))
            .border(1.dp, palette.accent.copy(alpha = 0.28f), RoundedCornerShape(16.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = modelOrdinalLabel(index),
            style = MaterialTheme.typography.titleMedium,
            color = palette.accent,
            fontWeight = FontWeight.ExtraBold
          )
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = section.typeLabel,
            style = MaterialTheme.typography.titleMedium,
            color = palette.text,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = if (activeQuestionCount > 0) "$activeQuestionCount nomor" else "Belum ada soal",
            style = MaterialTheme.typography.labelLarge,
            color = palette.text.copy(alpha = 0.72f),
            fontWeight = FontWeight.SemiBold
          )
        }
      }
      AnimatedVisibility(visible = showDeleteAction) {
        SoalIconAction(Icons.Outlined.Delete, "Hapus model", onDelete, tint = Color(0xFFDC2626))
      }
    }

    SoalTypeDropdown(
      selectedTypeKey = section.typeKey,
      onSelect = { option ->
        val normalizedChoiceQuestions = if (option.key.usesQuestionPromptEditor()) {
          section.choiceQuestions.withTrailingBlankQuestion()
        } else {
          section.choiceQuestions
        }
        onChange(
          section.copy(
            typeKey = option.key,
            typeLabel = option.label,
            choiceQuestions = normalizedChoiceQuestions
          )
        )
      }
    )

    SoalTextField(
      value = section.instruction,
      onValueChange = { onChange(section.copy(instruction = it)) },
      label = "Instruksi model",
      minLines = 2
    )
    if (section.typeKey.usesQuestionPromptEditor()) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        section.choiceQuestions.withTrailingBlankQuestion().forEachIndexed { questionIndex, choiceQuestion ->
          SoalChoiceQuestionEditor(
            question = choiceQuestion,
            questionIndex = questionIndex,
            modelLabel = section.typeLabel,
            typeKey = section.typeKey,
            palette = palette,
            showOptions = section.typeKey == "pilihan-ganda",
            canDelete = choiceQuestion.hasQuestionContent(),
            onDelete = { onDeleteQuestion(questionIndex) },
            onChange = { updated ->
              val nextQuestions = section.choiceQuestions
                .withTrailingBlankQuestion()
                .mapIndexed { index, current -> if (index == questionIndex) updated else current }
                .withTrailingBlankQuestion()
              onChange(section.copy(count = nextQuestions.filledQuestionCount(), choiceQuestions = nextQuestions))
            }
          )
        }
      }
    } else {
      SoalTextField(
        value = section.content,
        onValueChange = { onChange(section.copy(content = it)) },
        label = "Isi soal / catatan editor",
        minLines = 5
      )
    }
  }
}

@Composable
private fun SoalEditorFabMenu(
  isPrinting: Boolean,
  onAddModel: () -> Unit,
  onPrint: () -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = modifier) {
    Box(
      modifier = Modifier
        .size(58.dp)
        .shadow(18.dp, CircleShape, ambientColor = PrimaryBlue.copy(alpha = 0.22f), spotColor = PrimaryBlue.copy(alpha = 0.28f))
        .clip(CircleShape)
        .background(PrimaryBlue)
        .clickable { expanded = true },
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Outlined.Add,
        contentDescription = "Aksi soal",
        tint = Color.White,
        modifier = Modifier.size(28.dp)
      )
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      DropdownMenuItem(
        text = {
          Text(
            text = "Tambah model soal",
            color = PrimaryBlueDark,
            fontWeight = FontWeight.SemiBold
          )
        },
        leadingIcon = {
          Icon(Icons.Outlined.Add, contentDescription = null, tint = PrimaryBlue)
        },
        onClick = {
          expanded = false
          onAddModel()
        }
      )
      DropdownMenuItem(
        text = {
          Text(
            text = if (isPrinting) "Menyiapkan cetak..." else "Cetak",
            color = PrimaryBlueDark,
            fontWeight = FontWeight.SemiBold
          )
        },
        leadingIcon = {
          Icon(Icons.Outlined.Print, contentDescription = null, tint = PrimaryBlue)
        },
        enabled = !isPrinting,
        onClick = {
          expanded = false
          onPrint()
        }
      )
    }
  }
}

@Composable
private fun SoalAddModelDialog(
  onDismiss: () -> Unit,
  onAdd: (String) -> Unit
) {
  var selectedTypeKey by rememberSaveable { mutableStateOf(SoalTypeOptions.first().key) }

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(26.dp))
        .background(Color.White.copy(alpha = 0.98f))
        .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(26.dp))
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = "Tambah Model Soal",
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = "Pilih model, lalu isi soal satu per satu.",
            style = MaterialTheme.typography.bodySmall,
            color = SubtleInk
          )
        }
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFFF8FAFC))
            .clickable(onClick = onDismiss),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = PrimaryBlueDark)
        }
      }

      SoalAddModelCard(
        selectedTypeKey = selectedTypeKey,
        onTypeChange = { selectedTypeKey = it },
        onAdd = { onAdd(selectedTypeKey) }
      )
    }
  }
}

@Composable
private fun SoalAddModelCard(
  selectedTypeKey: String,
  onTypeChange: (String) -> Unit,
  onAdd: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(HighlightCard.copy(alpha = 0.72f))
      .border(1.dp, PrimaryBlue.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = "Tambah Model Soal",
      style = MaterialTheme.typography.titleSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    SoalTypeDropdown(
      selectedTypeKey = selectedTypeKey,
      onSelect = { onTypeChange(it.key) }
    )
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(999.dp))
        .background(PrimaryBlue)
        .clickable(onClick = onAdd)
        .padding(horizontal = 16.dp, vertical = 14.dp),
      contentAlignment = Alignment.Center
    ) {
      Text("Tambah Model", color = Color.White, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun SoalTypeDropdown(
  selectedTypeKey: String,
  onSelect: (SoalTypeOption) -> Unit,
  modifier: Modifier = Modifier
) {
  val selectedType = SoalTypeOptions.firstOrNull { it.key == selectedTypeKey } ?: SoalTypeOptions.first()
  SoalDropdownField(
    label = "Model soal",
    selectedLabel = selectedType.label,
    options = SoalTypeOptions.map { it.label },
    onSelect = { label ->
      SoalTypeOptions.firstOrNull { it.label == label }?.let(onSelect)
    },
    modifier = modifier
  )
}

@Composable
private fun SoalDropdownField(
  label: String,
  selectedLabel: String,
  options: List<String>,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = modifier.fillMaxWidth()) {
    OutlinedTextField(
      value = selectedLabel,
      onValueChange = {},
      readOnly = true,
      label = { Text(label) },
      trailingIcon = {
        Text(
          text = "v",
          style = MaterialTheme.typography.labelLarge,
          color = SubtleInk,
          fontWeight = FontWeight.Bold
        )
      },
      modifier = Modifier
        .fillMaxWidth(),
      shape = RoundedCornerShape(16.dp)
    )
    Box(
      modifier = Modifier
        .matchParentSize()
        .clip(RoundedCornerShape(16.dp))
        .clickable { expanded = true }
    )
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.fillMaxWidth(0.86f)
    ) {
      options.forEach { option ->
        DropdownMenuItem(
          text = {
            Text(
              text = option,
              style = MaterialTheme.typography.bodyMedium,
              color = PrimaryBlueDark,
              fontWeight = if (option == selectedLabel) FontWeight.Bold else FontWeight.Normal
            )
          },
          onClick = {
            onSelect(option)
            expanded = false
          }
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SoalChoiceQuestionEditor(
  question: MapelChoiceQuestion,
  questionIndex: Int,
  modelLabel: String,
  typeKey: String,
  palette: SoalModelPalette,
  showOptions: Boolean,
  canDelete: Boolean,
  onDelete: () -> Unit,
  onChange: (MapelChoiceQuestion) -> Unit
) {
  var showDeleteAction by rememberSaveable(question.id, questionIndex) { mutableStateOf(false) }
  val hasPrompt = question.prompt.isNotBlank()
  val hasFilledOptions = question.options.any { it.isNotBlank() }
  val shouldShowOptions = showOptions && (hasPrompt || hasFilledOptions)
  val questionTitle = when (typeKey) {
    "benar-salah" -> "Pernyataan"
    "isi-titik" -> "Isian"
    else -> "Pertanyaan"
  }
  val promptLabel = when (typeKey) {
    "esai" -> "Teks pertanyaan esai"
    "isi-titik" -> "Teks soal isian"
    "benar-salah" -> "Teks pernyataan"
    else -> "Teks pertanyaan"
  }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(Color.White.copy(alpha = 0.78f))
      .border(1.dp, palette.border.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
      .combinedClickable(
        onClick = { if (showDeleteAction) showDeleteAction = false },
        onLongClick = { if (canDelete) showDeleteAction = true }
      )
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(palette.accent)
          .shadow(8.dp, CircleShape, ambientColor = palette.accent.copy(alpha = 0.16f), spotColor = palette.accent.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "${questionIndex + 1}",
          style = MaterialTheme.typography.labelLarge,
          color = Color.White,
          fontWeight = FontWeight.ExtraBold
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
          text = questionTitle,
          style = MaterialTheme.typography.labelLarge,
          color = palette.text,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = modelLabel,
          style = MaterialTheme.typography.labelSmall,
          color = palette.text.copy(alpha = 0.68f),
          fontWeight = FontWeight.SemiBold
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      AnimatedVisibility(visible = showDeleteAction) {
        SoalIconAction(Icons.Outlined.Delete, "Hapus soal", onDelete, tint = Color(0xFFDC2626))
      }
    }
    SoalTextField(
      value = question.prompt,
      onValueChange = { onChange(question.copy(prompt = it)) },
      label = promptLabel,
      minLines = 2
    )
    AnimatedVisibility(visible = shouldShowOptions) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        question.options.normalizedChoiceOptions().forEachIndexed { optionIndex, optionText ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(palette.accent.copy(alpha = 0.10f))
                .border(1.dp, palette.accent.copy(alpha = 0.18f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "${('A'.code + optionIndex).toChar()}",
                style = MaterialTheme.typography.labelLarge,
                color = palette.text,
                fontWeight = FontWeight.ExtraBold
              )
            }
            OutlinedTextField(
              value = optionText,
              onValueChange = { value ->
                val nextOptions = question.options
                  .normalizedChoiceOptions()
                  .toMutableList()
                  .also { options ->
                    options[optionIndex] = value
                  }
                  .normalizedChoiceOptions()
                onChange(question.copy(options = nextOptions))
              },
              label = { Text("Pilihan jawaban") },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(16.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SoalSelectableChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(if (selected) PrimaryBlue else Color.White.copy(alpha = 0.86f))
      .border(
        1.dp,
        if (selected) PrimaryBlue else CardBorder.copy(alpha = 0.9f),
        RoundedCornerShape(999.dp)
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (selected) Color.White else PrimaryBlueDark,
      fontWeight = FontWeight.SemiBold
    )
  }
}

@Composable
private fun SoalTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  minLines: Int = 1,
  keyboardType: KeyboardType = KeyboardType.Text
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    modifier = modifier.fillMaxWidth(),
    minLines = minLines,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
  )
}

@Composable
private fun PatronMateriToolbar(
  totalMateri: Int,
  onAddMateri: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "$totalMateri materi",
      style = MaterialTheme.typography.labelLarge,
      color = SubtleInk,
      fontWeight = FontWeight.SemiBold
    )
    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(999.dp))
        .background(Color.White.copy(alpha = 0.90f))
        .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
        .clickable(onClick = onAddMateri)
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Outlined.Add,
        contentDescription = null,
        tint = PrimaryBlue,
        modifier = Modifier.size(18.dp)
      )
      Text(
        text = "Tambah Materi",
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PatronMateriCard(
  item: PatronMateriItem,
  onTextChange: (String) -> Unit,
  onSave: () -> Unit,
  onDelete: () -> Unit
) {
  val isDraft = item.id.startsWith("draft-")
  var isEditing by rememberSaveable(item.id) { mutableStateOf(isDraft) }
  var showDeleteAction by rememberSaveable(item.id) { mutableStateOf(false) }
  var offsetX by remember(item.id) { mutableStateOf(0f) }
  val swipeTriggerPx = with(LocalDensity.current) { 96.dp.toPx() }
  val swipeRevealThresholdPx = swipeTriggerPx * 0.72f
  val animatedOffsetX by animateFloatAsState(
    targetValue = offsetX,
    animationSpec = tween(180),
    label = "patron-materi-swipe-offset"
  )

  Box(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
        .shadow(9.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
        .clip(RoundedCornerShape(20.dp))
        .background(Color.White.copy(alpha = 0.88f))
        .border(1.dp, CardBorder.copy(alpha = 0.90f), RoundedCornerShape(20.dp))
        .pointerInput(item.id) {
          detectHorizontalDragGestures(
            onDragEnd = {
              showDeleteAction = offsetX <= -swipeRevealThresholdPx
              offsetX = 0f
            },
            onDragCancel = {
              offsetX = 0f
            },
            onHorizontalDrag = { change, dragAmount ->
              change.consume()
              offsetX = (offsetX + dragAmount).coerceIn(-swipeTriggerPx, 0f)
            }
          )
        }
        .combinedClickable(
          onClick = {
            if (showDeleteAction || offsetX < 0f) {
              showDeleteAction = false
              offsetX = 0f
            }
          },
          onLongClick = {
            offsetX = 0f
            showDeleteAction = false
            isEditing = true
          }
        )
        .animateContentSize()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(HighlightCard.copy(alpha = 0.16f))
          .border(1.dp, HighlightCard.copy(alpha = 0.22f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.AutoStories,
          contentDescription = null,
          tint = HighlightCard,
          modifier = Modifier.size(19.dp)
        )
      }

      if (isEditing) {
        OutlinedTextField(
          value = item.text,
          onValueChange = onTextChange,
          placeholder = { Text("Tulis materi...") },
          singleLine = true,
          modifier = Modifier.weight(1f)
        )
      } else {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = item.text,
            style = MaterialTheme.typography.titleSmall,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      AnimatedVisibility(visible = isEditing) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SuccessTint.copy(alpha = 0.14f))
            .border(1.dp, SuccessTint.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .clickable {
              isEditing = false
              offsetX = 0f
              showDeleteAction = false
              onSave()
            },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = "Simpan materi",
            tint = SuccessTint
          )
        }
      }

      AnimatedVisibility(visible = showDeleteAction && !isEditing) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFDC2626).copy(alpha = 0.12f))
            .border(1.dp, Color(0xFFDC2626).copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .clickable(onClick = onDelete),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Hapus materi",
            tint = Color(0xFFDC2626)
          )
        }
      }
    }
  }
}

private fun preparePatronMateriItemsForSave(items: List<PatronMateriItem>): List<PatronMateriItem> {
  val cleanItems = items.filter { it.text.trim().isNotBlank() }
  val draftItems = cleanItems.filter { it.id.startsWith("draft-") }
  val savedItems = cleanItems.filterNot { it.id.startsWith("draft-") }
  return savedItems + draftItems
}

@Composable
private fun PatronMateriSkeletonCard(index: Int) {
  SkeletonContentCard(
    leadingSize = 40.dp,
    firstLineWidth = if (index % 2 == 0) 0.68f else 0.48f,
    secondLineWidth = if (index % 3 == 0) 0.84f else 0.62f,
    trailingSize = 40.dp
  )
}

@Composable
private fun AttendanceSortToggle(
  selectedMode: AttendanceSortMode,
  onSelectMode: (AttendanceSortMode) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(999.dp))
      .background(Color.White.copy(alpha = 0.88f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    AttendanceSortMode.entries.forEach { mode ->
      val selected = mode == selectedMode
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(999.dp))
          .background(if (selected) HighlightCard.copy(alpha = 0.18f) else Color.Transparent)
          .border(
            1.dp,
            if (selected) HighlightCard.copy(alpha = 0.28f) else Color.Transparent,
            RoundedCornerShape(999.dp)
          )
          .clickable { onSelectMode(mode) }
          .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = mode.label,
          style = MaterialTheme.typography.labelLarge,
          color = if (selected) PrimaryBlueDark else SubtleInk,
          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
      }
    }
  }
}

@Composable
private fun ScoreSortToggle(
  selectedMode: ScoreSortMode,
  onSelectMode: (ScoreSortMode) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(999.dp))
      .background(Color.White.copy(alpha = 0.88f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    ScoreSortMode.entries.forEach { mode ->
      val selected = mode == selectedMode
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(999.dp))
          .background(if (selected) HighlightCard.copy(alpha = 0.18f) else Color.Transparent)
          .border(
            1.dp,
            if (selected) HighlightCard.copy(alpha = 0.28f) else Color.Transparent,
            RoundedCornerShape(999.dp)
          )
          .clickable { onSelectMode(mode) }
          .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = mode.label,
          style = MaterialTheme.typography.labelLarge,
          color = if (selected) PrimaryBlueDark else SubtleInk,
          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
      }
    }
  }
}

@Composable
private fun AbsensiMapelHeaderCard(
  subject: SubjectOverview,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x120F172A), spotColor = Color(0x120F172A))
      .background(Color.White.copy(alpha = 0.96f), RoundedCornerShape(22.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .padding(18.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = subject.title,
      style = MaterialTheme.typography.headlineSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = subject.className,
      style = MaterialTheme.typography.titleSmall,
      color = PrimaryBlue,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = subject.semester,
      style = MaterialTheme.typography.bodyMedium,
      color = SubtleInk
    )
  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun AttendanceCard(
  student: AttendanceStudent,
  onSaveAttendance: suspend (String, List<AttendanceHistoryEntry>) -> AttendanceSaveOutcome,
  onDeleteAttendance: suspend (List<String>) -> AttendanceSaveOutcome
) {
  val stats = remember(student) {
    listOf(
      AttendanceChartStat("Hadir", student.hadirPercent, resolveAttendancePalette("Hadir")),
      AttendanceChartStat("Terlambat", student.terlambatPercent, resolveAttendancePalette("Terlambat")),
      AttendanceChartStat("Sakit", student.sakitPercent, resolveAttendancePalette("Sakit")),
      AttendanceChartStat("Izin", student.izinPercent, resolveAttendancePalette("Izin")),
      AttendanceChartStat("Alpa", student.alpaPercent, resolveAttendancePalette("Alpa"))
    )
  }
  val visibleStats = remember(stats) { stats.filter { it.percent > 0 } }
  val coroutineScope = rememberCoroutineScope()
  var isExpanded by rememberSaveable(student.id) { mutableStateOf(false) }
  var editingDateIso by rememberSaveable(student.id) { mutableStateOf<String?>(null) }
  var savingDateIso by rememberSaveable(student.id) { mutableStateOf<String?>(null) }
  var deletingDateIso by rememberSaveable(student.id) { mutableStateOf<String?>(null) }
  var feedbackMessage by remember(student.history) { mutableStateOf<String?>(null) }
  var draftStatuses by remember(student.history) {
    mutableStateOf(student.history.associate { it.dateIso to it.status })
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(20.dp))
      .background(Color.White.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.94f), RoundedCornerShape(20.dp))
      .combinedClickable(
        onClick = {
          isExpanded = !isExpanded
          if (!isExpanded) {
            editingDateIso = null
            savingDateIso = null
            deletingDateIso = null
            feedbackMessage = null
            draftStatuses = student.history.associate { it.dateIso to it.status }
          }
        },
        onLongClick = {
          isExpanded = true
          feedbackMessage = null
        }
      )
      .animateContentSize()
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = student.name,
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (visibleStats.isEmpty()) {
          Text(
            text = "Belum ada data kehadiran untuk mapel ini.",
            style = MaterialTheme.typography.bodySmall,
            color = SubtleInk
          )
        } else {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            visibleStats.forEach { stat ->
              AttendanceStatItem(stat = stat)
            }
          }
        }
      }
      AttendanceChart(stats = visibleStats)
    }

    AnimatedVisibility(visible = isExpanded) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AttendanceCardHint(
          isEditing = editingDateIso != null,
          readOnlyMessage = "Buka riwayat, lalu tekan lama item tanggal yang ingin diubah."
        )
        if (student.history.isEmpty()) {
          EmptyPlaceholderCard(
            if (visibleStats.isNotEmpty()) {
              "Riwayat tanggal belum ikut tersinkron. Buka ulang halaman ini saat koneksi aktif atau tarik refresh."
            } else {
              "Belum ada riwayat kehadiran untuk santri ini."
            }
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            student.history.forEachIndexed { index, entry ->
              val currentStatus = draftStatuses[entry.dateIso] ?: entry.status
              val isItemEditing = editingDateIso == entry.dateIso
              AttendanceTimelineItem(
                entry = entry,
                isLast = index == student.history.lastIndex,
                currentStatus = currentStatus,
                isEditing = isItemEditing,
                isSaving = savingDateIso == entry.dateIso,
                isDeleting = deletingDateIso == entry.dateIso,
                hasChange = currentStatus != entry.status,
                onLongClick = {
                  if (editingDateIso != entry.dateIso) {
                    draftStatuses = student.history.associate { it.dateIso to it.status }
                  }
                  editingDateIso = entry.dateIso
                  feedbackMessage = null
                },
                onStatusSelect = { selectedStatus ->
                  draftStatuses = draftStatuses.toMutableMap().apply {
                    put(entry.dateIso, selectedStatus)
                  }
                },
                onCancelEdit = {
                  draftStatuses = draftStatuses.toMutableMap().apply {
                    put(entry.dateIso, entry.status)
                  }
                  editingDateIso = null
                  feedbackMessage = null
                },
                onSaveEdit = {
                  if (currentStatus == entry.status) {
                    editingDateIso = null
                    feedbackMessage = null
                  } else {
                    coroutineScope.launch {
                      savingDateIso = entry.dateIso
                      feedbackMessage = null
                      val outcome = onSaveAttendance(
                        student.id,
                        listOf(entry.copy(status = currentStatus))
                      )
                      feedbackMessage = outcome.message
                      if (outcome.success) {
                        editingDateIso = null
                      }
                      savingDateIso = null
                    }
                  }
                },
                onDelete = {
                  coroutineScope.launch {
                    deletingDateIso = entry.dateIso
                    feedbackMessage = null
                    val outcome = onDeleteAttendance(entry.rowIds)
                    feedbackMessage = outcome.message
                    if (outcome.success) {
                      editingDateIso = null
                      draftStatuses = draftStatuses - entry.dateIso
                    }
                    deletingDateIso = null
                  }
                }
              )
            }
          }
        }

        if (feedbackMessage != null) {
          AttendanceInfoBox(
            message = feedbackMessage ?: "",
            tone = if (editingDateIso != null) WarmAccent else SuccessTint
          )
        }
      }
    }
  }
}

@Composable
private fun AttendanceCardHint(
  isEditing: Boolean,
  readOnlyMessage: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color(0xFFF8FAFC))
      .border(1.dp, CardBorder.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = if (isEditing) {
        "Ubah status item aktif, lalu simpan di item tersebut."
      } else {
        readOnlyMessage
      },
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun AttendanceTimelineItem(
  entry: AttendanceHistoryEntry,
  isLast: Boolean,
  currentStatus: String,
  isEditing: Boolean,
  isSaving: Boolean,
  isDeleting: Boolean,
  hasChange: Boolean,
  onLongClick: () -> Unit,
  onStatusSelect: (String) -> Unit,
  onCancelEdit: () -> Unit,
  onSaveEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val palette = remember(currentStatus) { resolveAttendancePalette(currentStatus) }
  var showDeleteAction by remember(entry.dateIso, entry.rowIds) { mutableStateOf(false) }
  var offsetX by remember(entry.dateIso) { mutableStateOf(0f) }
  val swipeTriggerPx = with(LocalDensity.current) { 96.dp.toPx() }
  val swipeRevealThresholdPx = swipeTriggerPx * 0.72f
  val animatedOffsetX by animateFloatAsState(
    targetValue = offsetX,
    animationSpec = tween(180),
    label = "attendance-timeline-swipe-offset"
  )

  Box(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
        .pointerInput(entry.dateIso, isEditing, isSaving, isDeleting) {
          if (!isEditing && !isSaving && !isDeleting) {
            detectHorizontalDragGestures(
              onDragEnd = {
                showDeleteAction = offsetX <= -swipeRevealThresholdPx
                offsetX = 0f
              },
              onDragCancel = {
                offsetX = 0f
              },
              onHorizontalDrag = { change, dragAmount ->
                change.consume()
                offsetX = (offsetX + dragAmount).coerceIn(-swipeTriggerPx, 0f)
              }
            )
          }
        },
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Top
    ) {
      Column(
        modifier = Modifier.padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(12.dp)
            .background(palette.accent, CircleShape)
        )
        if (!isLast) {
          Box(
            modifier = Modifier
              .padding(top = 4.dp)
              .width(2.dp)
              .height(54.dp)
              .background(CardBorder.copy(alpha = 0.8f), RoundedCornerShape(999.dp))
          )
        }
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(18.dp))
          .background(Color.White.copy(alpha = 0.86f))
          .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(18.dp))
          .combinedClickable(
            onClick = {
              if (showDeleteAction || offsetX < 0f) {
                showDeleteAction = false
                offsetX = 0f
              }
            },
            onLongClick = {
              showDeleteAction = false
              offsetX = 0f
              onLongClick()
            }
          )
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = formatAttendanceDate(entry.dateIso),
            style = MaterialTheme.typography.labelLarge,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.Bold
          )
          AttendanceStatusBadge(
            status = currentStatus,
            palette = palette
          )
        }

        if (isEditing) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            AttendanceStatusOptions.forEach { statusOption ->
              val optionPalette = resolveAttendancePalette(statusOption)
              val isSelected = currentStatus == statusOption
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(999.dp))
                  .background(
                    if (isSelected) optionPalette.background else Color.White.copy(alpha = 0.9f)
                  )
                  .border(
                    1.dp,
                    if (isSelected) optionPalette.border else CardBorder.copy(alpha = 0.84f),
                    RoundedCornerShape(999.dp)
                  )
                  .clickable(enabled = !isSaving) { onStatusSelect(statusOption) }
                  .padding(horizontal = 12.dp, vertical = 8.dp)
              ) {
                Text(
                  text = statusOption,
                  style = MaterialTheme.typography.labelMedium,
                  color = if (isSelected) optionPalette.text else SubtleInk,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
              }
            }
          }
          AttendanceInlineEditActions(
            hasChange = hasChange,
            isSaving = isSaving,
            onCancel = onCancelEdit,
            onSave = onSaveEdit
          )
        }
      }
    }

    AnimatedVisibility(
      visible = showDeleteAction && !isEditing,
      modifier = Modifier.align(Alignment.CenterEnd)
    ) {
      AttendanceDeleteActionButton(
        isBusy = isDeleting,
        contentDescription = "Hapus absensi",
        onClick = onDelete
      )
    }
  }
}

@Composable
private fun AttendanceDeleteActionButton(
  isBusy: Boolean,
  contentDescription: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(40.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(Color(0xFFDC2626).copy(alpha = 0.12f))
      .border(1.dp, Color(0xFFDC2626).copy(alpha = 0.28f), RoundedCornerShape(14.dp))
      .clickable(enabled = !isBusy, onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    if (isBusy) {
      CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        color = Color(0xFFDC2626),
        strokeWidth = 2.dp
      )
    } else {
      Icon(
        imageVector = Icons.Outlined.Close,
        contentDescription = contentDescription,
        tint = Color(0xFFDC2626)
      )
    }
  }
}

@Composable
private fun AttendanceInlineEditActions(
  hasChange: Boolean,
  isSaving: Boolean,
  onCancel: () -> Unit,
  onSave: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(999.dp))
        .background(Color.White.copy(alpha = 0.88f))
        .border(1.dp, CardBorder.copy(alpha = 0.94f), RoundedCornerShape(999.dp))
        .clickable(enabled = !isSaving) { onCancel() }
        .padding(vertical = 9.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "Batal",
        style = MaterialTheme.typography.labelMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold
      )
    }
    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(999.dp))
        .background(if (hasChange) SuccessTint.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.66f))
        .border(
          1.dp,
          if (hasChange) SuccessTint.copy(alpha = 0.42f) else CardBorder.copy(alpha = 0.8f),
          RoundedCornerShape(999.dp)
        )
        .clickable(enabled = hasChange && !isSaving) { onSave() }
        .padding(vertical = 9.dp),
      contentAlignment = Alignment.Center
    ) {
      if (isSaving) {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          color = SuccessTint,
          strokeWidth = 2.dp
        )
      } else {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = if (hasChange) SuccessTint else SubtleInk
          )
          Text(
            text = if (hasChange) "Simpan" else "Belum berubah",
            style = MaterialTheme.typography.labelMedium,
            color = if (hasChange) SuccessTint else SubtleInk,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}

@Composable
private fun AttendanceStatusBadge(
  status: String,
  palette: AttendancePalette
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(palette.background)
      .border(1.dp, palette.border, RoundedCornerShape(999.dp))
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Text(
      text = status,
      style = MaterialTheme.typography.labelMedium,
      color = palette.text,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
private fun AttendanceInfoBox(
  message: String,
  tone: Color
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(tone.copy(alpha = 0.12f))
      .border(1.dp, tone.copy(alpha = 0.26f), RoundedCornerShape(16.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodySmall,
      color = PrimaryBlueDark
    )
  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun AttendanceDateCard(
  overview: AttendanceDateOverview,
  onDeleteAttendance: suspend (List<String>) -> AttendanceSaveOutcome,
  onSaveAttendance: suspend (Map<String, List<AttendanceHistoryEntry>>) -> AttendanceSaveOutcome
) {
  val coroutineScope = rememberCoroutineScope()
  val allRowIds = remember(overview.entries) {
    overview.entries.flatMap { it.history.rowIds }.distinct()
  }
  var isExpanded by rememberSaveable(overview.dateIso) { mutableStateOf(false) }
  var editingStudentId by rememberSaveable(overview.dateIso) { mutableStateOf<String?>(null) }
  var savingStudentId by rememberSaveable(overview.dateIso) { mutableStateOf<String?>(null) }
  var isDeletingDate by rememberSaveable(overview.dateIso) { mutableStateOf(false) }
  var showDeleteAction by rememberSaveable(overview.dateIso) { mutableStateOf(false) }
  var offsetX by remember(overview.dateIso) { mutableStateOf(0f) }
  var feedbackMessage by remember(overview.dateIso, overview.entries) { mutableStateOf<String?>(null) }
  var draftStatuses by remember(overview.dateIso, overview.entries) {
    mutableStateOf(overview.entries.associate { it.studentId to it.status })
  }
  val swipeTriggerPx = with(LocalDensity.current) { 96.dp.toPx() }
  val swipeRevealThresholdPx = swipeTriggerPx * 0.72f
  val animatedOffsetX by animateFloatAsState(
    targetValue = offsetX,
    animationSpec = tween(180),
    label = "attendance-date-card-swipe-offset"
  )

  Box(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
        .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
        .clip(RoundedCornerShape(20.dp))
        .background(Color.White.copy(alpha = 0.94f))
        .border(1.dp, CardBorder.copy(alpha = 0.94f), RoundedCornerShape(20.dp))
        .pointerInput(overview.dateIso, editingStudentId, savingStudentId, isDeletingDate) {
          if (editingStudentId == null && savingStudentId == null && !isDeletingDate) {
            detectHorizontalDragGestures(
              onDragEnd = {
                showDeleteAction = offsetX <= -swipeRevealThresholdPx
                offsetX = 0f
              },
              onDragCancel = {
                offsetX = 0f
              },
              onHorizontalDrag = { change, dragAmount ->
                change.consume()
                offsetX = (offsetX + dragAmount).coerceIn(-swipeTriggerPx, 0f)
              }
            )
          }
        }
        .combinedClickable(
          onClick = {
            if (showDeleteAction || offsetX < 0f) {
              showDeleteAction = false
              offsetX = 0f
            } else {
              isExpanded = !isExpanded
              if (!isExpanded) {
                editingStudentId = null
                savingStudentId = null
                feedbackMessage = null
                draftStatuses = overview.entries.associate { it.studentId to it.status }
              }
            }
          },
          onLongClick = {
            showDeleteAction = false
            offsetX = 0f
            isExpanded = true
            feedbackMessage = null
          }
        )
        .animateContentSize()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = formatAttendanceDate(overview.dateIso),
            style = MaterialTheme.typography.titleSmall,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            overview.stats.forEach { stat ->
              AttendanceStatItem(stat = stat)
            }
          }
        }
        Box(
          modifier = Modifier.size(54.dp),
          contentAlignment = Alignment.Center
        ) {
          AnimatedContent(
            targetState = showDeleteAction,
            label = "attendance-date-delete-slot"
          ) { isDeleteActionVisible ->
            if (isDeleteActionVisible) {
              AttendanceDeleteActionButton(
                isBusy = isDeletingDate,
                contentDescription = "Hapus absensi tanggal ini",
                onClick = {
                  coroutineScope.launch {
                    isDeletingDate = true
                    feedbackMessage = null
                    val outcome = onDeleteAttendance(allRowIds)
                    feedbackMessage = outcome.message
                    if (outcome.success) {
                      isExpanded = false
                      showDeleteAction = false
                      editingStudentId = null
                      savingStudentId = null
                    } else {
                      isExpanded = true
                    }
                    isDeletingDate = false
                  }
                }
              )
            } else {
              AttendanceChart(stats = overview.stats)
            }
          }
        }
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          AttendanceCardHint(
            isEditing = editingStudentId != null,
            readOnlyMessage = "Buka daftar santri, lalu tekan lama nama santri yang ingin diubah."
          )
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            overview.entries.forEachIndexed { index, entry ->
              val currentStatus = draftStatuses[entry.studentId] ?: entry.status
              val isItemEditing = editingStudentId == entry.studentId
              AttendanceDateStudentItem(
                entry = entry,
                isLast = index == overview.entries.lastIndex,
                currentStatus = currentStatus,
                isEditing = isItemEditing,
                isSaving = savingStudentId == entry.studentId,
                hasChange = currentStatus != entry.status,
                onLongClick = {
                  if (editingStudentId != entry.studentId) {
                    draftStatuses = overview.entries.associate { it.studentId to it.status }
                  }
                  editingStudentId = entry.studentId
                  feedbackMessage = null
                },
                onStatusSelect = { selectedStatus ->
                  draftStatuses = draftStatuses.toMutableMap().apply {
                    put(entry.studentId, selectedStatus)
                  }
                },
                onCancelEdit = {
                  draftStatuses = draftStatuses.toMutableMap().apply {
                    put(entry.studentId, entry.status)
                  }
                  editingStudentId = null
                  feedbackMessage = null
                },
                onSaveEdit = {
                  if (currentStatus == entry.status) {
                    editingStudentId = null
                    feedbackMessage = null
                  } else {
                    coroutineScope.launch {
                      savingStudentId = entry.studentId
                      feedbackMessage = null
                      val outcome = onSaveAttendance(
                        mapOf(entry.studentId to listOf(entry.history.copy(status = currentStatus)))
                      )
                      feedbackMessage = outcome.message
                      if (outcome.success) {
                        editingStudentId = null
                      }
                      savingStudentId = null
                    }
                  }
                }
              )
            }
          }

          if (feedbackMessage != null) {
            AttendanceInfoBox(
              message = feedbackMessage ?: "",
              tone = if (editingStudentId != null || isDeletingDate) WarmAccent else SuccessTint
            )
          }
        }
      }
    }

  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun AttendanceDateStudentItem(
  entry: AttendanceDateStudentEntry,
  isLast: Boolean,
  currentStatus: String,
  isEditing: Boolean,
  isSaving: Boolean,
  hasChange: Boolean,
  onLongClick: () -> Unit,
  onStatusSelect: (String) -> Unit,
  onCancelEdit: () -> Unit,
  onSaveEdit: () -> Unit
) {
  val palette = remember(currentStatus) { resolveAttendancePalette(currentStatus) }
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.Top
  ) {
    Column(
      modifier = Modifier.padding(top = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(12.dp)
          .background(palette.accent, CircleShape)
      )
      if (!isLast) {
        Box(
          modifier = Modifier
            .padding(top = 4.dp)
            .width(2.dp)
            .height(54.dp)
            .background(CardBorder.copy(alpha = 0.8f), RoundedCornerShape(999.dp))
        )
      }
    }
    Column(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(18.dp))
        .background(Color.White.copy(alpha = 0.86f))
        .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(18.dp))
        .combinedClickable(
          onClick = {},
          onLongClick = onLongClick
        )
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = entry.studentName,
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.size(10.dp))
        AttendanceStatusBadge(
          status = currentStatus,
          palette = palette
        )
      }

      if (isEditing) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          AttendanceStatusOptions.forEach { statusOption ->
            val optionPalette = resolveAttendancePalette(statusOption)
            val isSelected = currentStatus == statusOption
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (isSelected) optionPalette.background else Color.White.copy(alpha = 0.9f))
                .border(
                  1.dp,
                  if (isSelected) optionPalette.border else CardBorder.copy(alpha = 0.84f),
                  RoundedCornerShape(999.dp)
                )
                .clickable(enabled = !isSaving) { onStatusSelect(statusOption) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Text(
                text = statusOption,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) optionPalette.text else SubtleInk,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              )
            }
          }
        }
        AttendanceInlineEditActions(
          hasChange = hasChange,
          isSaving = isSaving,
          onCancel = onCancelEdit,
          onSave = onSaveEdit
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScoreCard(
  student: ScoreStudent,
  onSaveScores: suspend (ScoreStudent) -> ScoreSaveOutcome
) {
  val metrics = remember(student) { buildScoreMetrics(student) }
  val coroutineScope = rememberCoroutineScope()
  var isExpanded by rememberSaveable(student.id) { mutableStateOf(false) }
  var isSaving by rememberSaveable(student.id) { mutableStateOf(false) }
  var feedbackMessage by remember(student) { mutableStateOf<String?>(null) }
  var selectedMetricKey by rememberSaveable(student.id) { mutableStateOf<String?>(null) }
  val selectedMetric = metrics.firstOrNull { it.key == selectedMetricKey }

  if (selectedMetric != null) {
    ScoreDetailDialog(
      studentName = student.name,
      metric = selectedMetric,
      isSaving = isSaving,
      onDismiss = {
        if (!isSaving) selectedMetricKey = null
      },
      onSave = { rows, deletedIds ->
        coroutineScope.launch {
          isSaving = true
          feedbackMessage = null
          val updatedStudent = updateScoreStudentWithDetailRows(
            student = student,
            metric = selectedMetric,
            rows = rows,
            deletedIds = deletedIds
          )
          val outcome = onSaveScores(updatedStudent)
          feedbackMessage = outcome.message
          isSaving = false
          if (outcome.success) {
            selectedMetricKey = null
          }
        }
      }
    )
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(20.dp))
      .background(Color.White.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.94f), RoundedCornerShape(20.dp))
      .combinedClickable(
        onClick = {
          isExpanded = !isExpanded
          if (!isExpanded) {
            isSaving = false
            feedbackMessage = null
          }
        }
      )
      .animateContentSize()
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = student.name,
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = "${metrics.count { it.value != null }} jenis nilai tersedia",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }

    AnimatedVisibility(visible = isExpanded) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AttendanceCardHint(
          isEditing = false,
          readOnlyMessage = "Tekan jenis nilai untuk melihat rincian tanggal dan mengeditnya."
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          metrics.forEach { metric ->
            ScoreMetricRow(
              metric = metric,
              isEditing = false,
              valueText = "",
              onValueChange = {},
              onClick = {
                if (metric.editable) {
                  selectedMetricKey = metric.key
                } else {
                  feedbackMessage = "Nilai ${metric.label} dihitung otomatis dan tidak diedit dari rincian nilai."
                }
              }
            )
          }
        }

        if (feedbackMessage != null) {
          AttendanceInfoBox(
            message = feedbackMessage ?: "",
            tone = if (isSaving) WarmAccent else SuccessTint
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScoreTypeCard(
  overview: ScoreTypeOverview,
  onSaveScores: suspend (List<ScoreStudent>) -> ScoreSaveOutcome
) {
  val coroutineScope = rememberCoroutineScope()
  var isExpanded by rememberSaveable(overview.key) { mutableStateOf(false) }
  var isSaving by rememberSaveable(overview.key) { mutableStateOf(false) }
  var feedbackMessage by remember(overview.key, overview.entries) { mutableStateOf<String?>(null) }
  var selectedStudentId by rememberSaveable(overview.key) { mutableStateOf<String?>(null) }
  val selectedEntry = overview.entries.firstOrNull { it.student.id == selectedStudentId }
  val selectedMetric = selectedEntry?.let { entry ->
    buildScoreMetrics(entry.student).firstOrNull { it.key == overview.metric.key }
  }

  if (selectedEntry != null && selectedMetric != null) {
    ScoreDetailDialog(
      studentName = selectedEntry.student.name,
      metric = selectedMetric,
      isSaving = isSaving,
      onDismiss = {
        if (!isSaving) selectedStudentId = null
      },
      onSave = { rows, deletedIds ->
        coroutineScope.launch {
          isSaving = true
          feedbackMessage = null
          val updatedStudent = updateScoreStudentWithDetailRows(
            student = selectedEntry.student,
            metric = selectedMetric,
            rows = rows,
            deletedIds = deletedIds
          )
          val outcome = onSaveScores(listOf(updatedStudent))
          feedbackMessage = outcome.message
          isSaving = false
          if (outcome.success) {
            selectedStudentId = null
          }
        }
      }
    )
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(20.dp))
      .background(Color.White.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.94f), RoundedCornerShape(20.dp))
      .combinedClickable(
        onClick = {
          isExpanded = !isExpanded
          if (!isExpanded) {
            isSaving = false
            feedbackMessage = null
          }
        }
      )
      .animateContentSize()
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = overview.label,
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = "Rata-rata ${overview.averageLabel} • ${overview.entries.size} santri",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }

    AnimatedVisibility(visible = isExpanded) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AttendanceCardHint(
          isEditing = false,
          readOnlyMessage = "Tekan nilai santri untuk melihat rincian tanggal dan mengeditnya."
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          overview.entries.forEach { entry ->
            ScoreTypeStudentRow(
              entry = entry,
              isEditing = false,
              valueText = "",
              onValueChange = {},
              onClick = {
                if (overview.metric.editable) {
                  selectedStudentId = entry.student.id
                } else {
                  feedbackMessage = "Nilai ${overview.metric.label} dihitung otomatis dan tidak diedit dari rincian nilai."
                }
              }
            )
          }
        }
        if (feedbackMessage != null) {
          AttendanceInfoBox(
            message = feedbackMessage ?: "",
            tone = if (isSaving) WarmAccent else SuccessTint
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ScoreDateCard(
  overview: ScoreDateOverview,
  onSaveScores: suspend (List<ScoreStudent>) -> ScoreSaveOutcome
) {
  val coroutineScope = rememberCoroutineScope()
  val studentCount = remember(overview.entries) { overview.entries.map { it.student.id }.distinct().size }
  var isExpanded by rememberSaveable(overview.key) { mutableStateOf(false) }
  var isDeletingDate by rememberSaveable(overview.key) { mutableStateOf(false) }
  var showDeleteAction by rememberSaveable(overview.key) { mutableStateOf(false) }
  var offsetX by remember(overview.key) { mutableStateOf(0f) }
  var feedbackMessage by remember(overview.key, overview.entries) { mutableStateOf<String?>(null) }
  val swipeTriggerPx = with(LocalDensity.current) { 96.dp.toPx() }
  val swipeRevealThresholdPx = swipeTriggerPx * 0.72f
  val animatedOffsetX by animateFloatAsState(
    targetValue = offsetX,
    animationSpec = tween(180),
    label = "score-date-card-swipe-offset"
  )

  Box(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
        .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
        .clip(RoundedCornerShape(20.dp))
        .background(Color.White.copy(alpha = 0.94f))
        .border(1.dp, CardBorder.copy(alpha = 0.94f), RoundedCornerShape(20.dp))
        .pointerInput(overview.key, isDeletingDate) {
          if (!isDeletingDate) {
            detectHorizontalDragGestures(
              onDragEnd = {
                showDeleteAction = offsetX <= -swipeRevealThresholdPx
                offsetX = 0f
              },
              onDragCancel = {
                offsetX = 0f
              },
              onHorizontalDrag = { change, dragAmount ->
                change.consume()
                offsetX = (offsetX + dragAmount).coerceIn(-swipeTriggerPx, 0f)
              }
            )
          }
        }
        .combinedClickable(
          onClick = {
            if (showDeleteAction || offsetX < 0f) {
              showDeleteAction = false
              offsetX = 0f
            } else {
              isExpanded = !isExpanded
              if (!isExpanded) {
                feedbackMessage = null
              }
            }
          }
        )
        .animateContentSize()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = formatAttendanceDate(overview.dateIso),
            style = MaterialTheme.typography.titleSmall,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${overview.metric.label} - rata-rata ${overview.averageLabel} - ${overview.entries.size} nilai",
            style = MaterialTheme.typography.bodySmall,
            color = SubtleInk
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            ScoreDateMiniChip(overview.metric.label)
            ScoreDateMiniChip("$studentCount santri")
            ScoreDateMiniChip("${overview.entries.count { it.row.id.isNotBlank() }} tersimpan")
          }
        }
        Box(
          modifier = Modifier.size(54.dp),
          contentAlignment = Alignment.Center
        ) {
          AnimatedContent(
            targetState = showDeleteAction,
            label = "score-date-delete-slot"
          ) { isDeleteActionVisible ->
            if (isDeleteActionVisible) {
              AttendanceDeleteActionButton(
                isBusy = isDeletingDate,
                contentDescription = "Hapus nilai tanggal ini",
                onClick = {
                  coroutineScope.launch {
                    isDeletingDate = true
                    feedbackMessage = null
                    val updates = buildScoreDateDeleteStudents(overview)
                    if (updates.isEmpty()) {
                      feedbackMessage = "Belum ada nilai tersimpan yang bisa dihapus pada tanggal ini."
                      isExpanded = true
                    } else {
                      val outcome = onSaveScores(updates)
                      feedbackMessage = outcome.message
                      if (outcome.success) {
                        isExpanded = false
                        showDeleteAction = false
                      } else {
                        isExpanded = true
                      }
                    }
                    isDeletingDate = false
                  }
                }
              )
            } else {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(RoundedCornerShape(15.dp))
                  .background(HighlightCard.copy(alpha = 0.13f))
                  .border(1.dp, HighlightCard.copy(alpha = 0.22f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Outlined.Calculate,
                  contentDescription = null,
                  tint = HighlightCard,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          AttendanceCardHint(
            isEditing = false,
            readOnlyMessage = "Swipe kartu ke kiri untuk menghapus nilai ${overview.metric.label} pada tanggal ini."
          )
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            overview.entries.forEach { entry ->
              ScoreDateEntryRow(entry = entry)
            }
          }
          if (feedbackMessage != null) {
            AttendanceInfoBox(
              message = feedbackMessage ?: "",
              tone = if (isDeletingDate) WarmAccent else SuccessTint
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ScoreDateMiniChip(label: String) {
  Text(
    text = label,
    style = MaterialTheme.typography.labelSmall,
    color = HighlightCard,
    fontWeight = FontWeight.SemiBold,
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(HighlightCard.copy(alpha = 0.1f))
      .border(1.dp, HighlightCard.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp)
  )
}

@Composable
private fun ScoreDateEntryRow(entry: ScoreDateEntry) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color(0xFFF8FAFC))
      .border(1.dp, CardBorder.copy(alpha = 0.86f), RoundedCornerShape(16.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = entry.student.name,
        style = MaterialTheme.typography.bodyMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = listOf(entry.metric.label, normalizeScoreMaterialText(entry.row.material))
          .filter { it.isNotBlank() }
          .joinToString(" - "),
        style = MaterialTheme.typography.labelSmall,
        color = SubtleInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Text(
      text = entry.row.value?.let(::formatScoreNumber) ?: "-",
      style = MaterialTheme.typography.labelLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
private fun ScoreMetricRow(
  metric: ScoreMetric,
  isEditing: Boolean,
  valueText: String,
  onValueChange: (String) -> Unit,
  onClick: () -> Unit = {}
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color(0xFFF8FAFC))
      .border(1.dp, CardBorder.copy(alpha = 0.86f), RoundedCornerShape(16.dp))
      .clickable(enabled = metric.editable, onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = metric.label,
        style = MaterialTheme.typography.bodyMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.Medium
      )
      Text(
        text = if (metric.editable) {
          if (metric.detailCount > 0) "${metric.detailCount} rincian nilai" else "Ketuk untuk tambah rincian"
        } else {
          "Dihitung otomatis"
        },
        style = MaterialTheme.typography.labelSmall,
        color = SubtleInk
      )
    }
    if (isEditing && metric.editable) {
      OutlinedTextField(
        value = valueText,
        onValueChange = onValueChange,
        singleLine = true,
        modifier = Modifier.width(104.dp),
        shape = RoundedCornerShape(14.dp)
      )
    } else {
      Text(
        text = metric.value?.let(::formatScoreNumber) ?: "-",
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun ScoreTypeStudentRow(
  entry: ScoreTypeStudentEntry,
  isEditing: Boolean,
  valueText: String,
  onValueChange: (String) -> Unit,
  onClick: () -> Unit = {}
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color(0xFFF8FAFC))
      .border(1.dp, CardBorder.copy(alpha = 0.86f), RoundedCornerShape(16.dp))
      .clickable(enabled = entry.metric.editable, onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = entry.student.name,
      style = MaterialTheme.typography.bodyMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.weight(1f)
    )
    if (isEditing && entry.metric.editable) {
      OutlinedTextField(
        value = valueText,
        onValueChange = onValueChange,
        singleLine = true,
        modifier = Modifier.width(104.dp),
        shape = RoundedCornerShape(14.dp)
      )
    } else {
      Text(
        text = entry.value?.let(::formatScoreNumber) ?: "-",
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun ScoreDetailDialog(
  studentName: String,
  metric: ScoreMetric,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onSave: (List<ScoreDetailRow>, List<String>) -> Unit
) {
  var rows by remember(metric.key, studentName) {
    mutableStateOf(metric.detailRows.map { it.toDraftRow() })
  }
  var deletedIds by remember(metric.key, studentName) { mutableStateOf<List<String>>(emptyList()) }
  var errorMessage by remember(metric.key, studentName) { mutableStateOf<String?>(null) }
  val average = remember(rows) {
    calculateScoreDetailAverage(rows.mapNotNull { it.valueText.replace(',', '.').toDoubleOrNull() })
  }

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 620.dp)
        .shadow(18.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x220F172A), spotColor = Color(0x220F172A))
        .clip(RoundedCornerShape(26.dp))
        .background(Color.White.copy(alpha = 0.98f))
        .border(1.dp, CardBorder.copy(alpha = 0.94f), RoundedCornerShape(26.dp))
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          Text(
            text = "Rincian ${metric.label}",
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = studentName,
            style = MaterialTheme.typography.bodySmall,
            color = SubtleInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "Rata-rata: ${average?.let(::formatScoreNumber) ?: "-"}",
            style = MaterialTheme.typography.labelLarge,
            color = PrimaryBlue,
            fontWeight = FontWeight.Bold
          )
        }
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFFF8FAFC))
            .border(1.dp, CardBorder.copy(alpha = 0.86f), CircleShape)
            .clickable(enabled = !isSaving, onClick = onDismiss),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Tutup",
            tint = PrimaryBlueDark,
            modifier = Modifier.size(19.dp)
          )
        }
      }

      if (errorMessage != null) {
        AttendanceInfoBox(
          message = errorMessage ?: "",
          tone = WarmAccent
        )
      }

      if (rows.isEmpty()) {
        EmptyPlaceholderCard("Belum ada rincian nilai. Tekan Tambah Baris untuk mulai mengisi.")
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 330.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          itemsIndexed(
            items = rows,
            key = { index, item -> item.id.ifBlank { "draft-$index" } }
          ) { index, row ->
            ScoreDetailRowEditor(
              index = index,
              row = row,
              maxValue = metric.maxValue,
              onRowChange = { next ->
                rows = rows.toMutableList().also { it[index] = next }
                errorMessage = null
              },
              onDelete = {
                if (row.id.isNotBlank()) {
                  deletedIds = deletedIds + row.id
                }
                rows = rows.toMutableList().also { it.removeAt(index) }
                errorMessage = null
              }
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .border(1.dp, CardBorder.copy(alpha = 0.94f), RoundedCornerShape(999.dp))
            .clickable(enabled = !isSaving) {
              rows = rows + ScoreDetailDraftRow(
                dateIso = LocalDate.now().toString(),
                valueText = "",
                material = ""
              )
              errorMessage = null
            }
            .padding(vertical = 11.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Tambah Baris",
            style = MaterialTheme.typography.labelLarge,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.SemiBold
          )
        }
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(999.dp))
            .background(SuccessTint.copy(alpha = 0.18f))
            .border(1.dp, SuccessTint.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
            .clickable(enabled = !isSaving) {
              val normalizedRows = mutableListOf<ScoreDetailRow>()
              for (draft in rows) {
                val hasContent = draft.dateIso.isNotBlank() || draft.valueText.isNotBlank() || draft.material.isNotBlank()
                if (!hasContent) continue
                val parsedValue = parseScoreDraftValue(draft.valueText, metric.maxValue)
                if (draft.dateIso.isBlank() || parsedValue == null) {
                  errorMessage = "Tanggal dan nilai wajib valid. Batas ${metric.label}: ${metric.maxValue?.toString() ?: "bebas"}."
                  return@clickable
                }
                normalizedRows.add(
                  ScoreDetailRow(
                    id = draft.id,
                    dateIso = draft.dateIso.trim().take(10),
                    value = parsedValue,
                    material = normalizeScoreMaterialText(draft.material)
                  )
                )
              }
              onSave(normalizedRows, deletedIds)
            }
            .padding(vertical = 11.dp),
          contentAlignment = Alignment.Center
        ) {
          if (isSaving) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = SuccessTint,
              strokeWidth = 2.dp
            )
          } else {
            Text(
              text = "Simpan",
              style = MaterialTheme.typography.labelLarge,
              color = SuccessTint,
              fontWeight = FontWeight.ExtraBold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ScoreDetailRowEditor(
  index: Int,
  row: ScoreDetailDraftRow,
  maxValue: Int?,
  onRowChange: (ScoreDetailDraftRow) -> Unit,
  onDelete: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(Color(0xFFF8FAFC))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "${index + 1}",
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      OutlinedTextField(
        value = row.dateIso,
        onValueChange = { onRowChange(row.copy(dateIso = it)) },
        singleLine = true,
        label = { Text("Tanggal") },
        modifier = Modifier.weight(1.25f),
        shape = RoundedCornerShape(14.dp)
      )
      OutlinedTextField(
        value = row.valueText,
        onValueChange = { value ->
          sanitizeScoreDraftValueText(value, maxValue)?.let { safeValue ->
            onRowChange(row.copy(valueText = safeValue))
          }
        },
        singleLine = true,
        label = { Text(if (maxValue != null) "Nilai /$maxValue" else "Nilai") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.weight(0.9f),
        shape = RoundedCornerShape(14.dp)
      )
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(Color(0xFFFEF2F2))
          .border(1.dp, Color(0xFFFCA5A5), CircleShape)
          .clickable(onClick = onDelete),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.Close,
          contentDescription = "Hapus rincian",
          tint = Color(0xFFB91C1C),
          modifier = Modifier.size(18.dp)
        )
      }
    }
    OutlinedTextField(
      value = row.material,
      onValueChange = { onRowChange(row.copy(material = it)) },
      singleLine = true,
      label = { Text("Materi / keterangan") },
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp)
    )
  }
}

@Composable
private fun ScoreSaveRow(
  isSaving: Boolean,
  onCancel: () -> Unit,
  onSave: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(999.dp))
        .background(Color.White.copy(alpha = 0.88f))
        .border(1.dp, CardBorder.copy(alpha = 0.94f), RoundedCornerShape(999.dp))
        .clickable(enabled = !isSaving, onClick = onCancel)
        .padding(vertical = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "Batal",
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold
      )
    }
    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(999.dp))
        .background(SuccessTint.copy(alpha = 0.18f))
        .border(1.dp, SuccessTint.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
        .clickable(enabled = !isSaving, onClick = onSave)
        .padding(vertical = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      if (isSaving) {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          color = SuccessTint,
          strokeWidth = 2.dp
        )
      } else {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = SuccessTint
          )
          Text(
            text = "Simpan perubahan",
            style = MaterialTheme.typography.labelLarge,
            color = SuccessTint,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}

@Composable
private fun MapelDetailBottomNav(
  selectedSection: MapelDetailSection,
  onSelectSection: (MapelDetailSection) -> Unit,
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
        .background(Color.White.copy(alpha = 0.96f))
        .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(32.dp))
        .padding(horizontal = 10.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      MapelDetailSection.entries.forEach { section ->
        val selected = section.name == animatedSelectedSectionKey
        Row(
          modifier = Modifier
            .weight(if (selected) 1.7f else 0.825f)
            .height(52.dp)
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
            .padding(horizontal = 12.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = section.icon,
            contentDescription = section.label,
            tint = if (selected) PrimaryBlue else SubtleInk.copy(alpha = 0.82f),
            modifier = Modifier.size(20.dp)
          )
          AnimatedVisibility(
            visible = selected,
            enter = fadeIn(animationSpec = tween(180)) + expandHorizontally(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(120)) + shrinkHorizontally(animationSpec = tween(180))
          ) {
            Text(
              text = section.label,
              style = MaterialTheme.typography.labelLarge,
              color = if (selected) PrimaryBlueDark else SubtleInk,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(start = 8.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AttendanceSkeletonCard(index: Int) {
  SkeletonContentCard(
    firstLineWidth = if (index % 2 == 0) 0.52f else 0.66f,
    secondLineWidth = if (index % 3 == 0) 0.76f else 0.58f,
    thirdLineWidth = 0.34f,
    trailingSize = 52.dp
  )
}

@Composable
private fun ScoreSkeletonCard(index: Int) {
  SkeletonContentCard(
    firstLineWidth = if (index % 2 == 0) 0.48f else 0.62f,
    secondLineWidth = if (index % 3 == 0) 0.72f else 0.54f,
    thirdLineWidth = 0.42f,
    trailingSize = 50.dp
  )
}

@Composable
private fun AttendanceStatItem(stat: AttendanceChartStat) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(5.dp)
        .background(stat.palette.accent, CircleShape)
    )
    Text(
      text = stat.label,
      style = MaterialTheme.typography.labelSmall,
      color = SubtleInk
    )
    Text(
      text = "${stat.percent}%",
      style = MaterialTheme.typography.labelSmall,
      color = stat.palette.text,
      fontWeight = FontWeight.ExtraBold
    )
  }
}

@Composable
private fun AttendanceChart(stats: List<AttendanceChartStat>) {
  val chartSize = when (stats.size) {
    0 -> 34.dp
    1 -> 28.dp
    2 -> 36.dp
    3 -> 44.dp
    4 -> 52.dp
    else -> 60.dp
  }
  if (stats.isEmpty()) {
    Box(
      modifier = Modifier
        .size(chartSize)
        .clip(CircleShape)
        .background(Color.White.copy(alpha = 0.88f))
        .border(1.dp, CardBorder.copy(alpha = 0.92f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Outlined.Refresh,
        contentDescription = null,
        tint = SubtleInk
      )
    }
    return
  }
  Canvas(modifier = Modifier.size(chartSize)) {
    val ringThickness = 4.dp.toPx()
    val ringSpacing = 1.8.dp.toPx()
    val ringStep = ringThickness + ringSpacing
    val baseInset = ringThickness / 2f
    stats.forEachIndexed { index, stat ->
      val inset = baseInset + (index * ringStep)
      val arcSize = size.minDimension - (inset * 2)
      if (arcSize <= 0f) return@forEachIndexed
      val topLeft = Offset(inset, inset)
      drawArc(
        color = stat.palette.background,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = Size(arcSize, arcSize),
        style = Stroke(width = ringThickness, cap = StrokeCap.Round)
      )
      drawArc(
        color = stat.palette.accent,
        startAngle = -90f,
        sweepAngle = (360f * stat.percent.toFloat() / 100f).coerceIn(0f, 360f),
        useCenter = false,
        topLeft = topLeft,
        size = Size(arcSize, arcSize),
        style = Stroke(width = ringThickness, cap = StrokeCap.Round)
      )
    }
  }
}

private data class AttendancePalette(
  val background: Color,
  val border: Color,
  val accent: Color,
  val text: Color
)

private data class AttendanceChartStat(
  val label: String,
  val percent: Int,
  val palette: AttendancePalette
)

private enum class AttendanceSortMode(val label: String) {
  ByStudent("Per Nama"),
  ByDate("Per Tanggal")
}

private enum class ScoreSortMode(val label: String) {
  ByStudent("Per Nama"),
  ByType("Per Jenis"),
  ByDate("Per Tanggal")
}

private enum class MapelDetailSection(
  val label: String,
  val icon: ImageVector
) {
  Absensi("Absensi", Icons.Outlined.TaskAlt),
  Nilai("Nilai", Icons.Outlined.EditNote),
  PatronMateri("Patron Materi", Icons.Outlined.AutoStories),
  Soal("Soal", Icons.Outlined.Quiz)
}

private data class MapelQuestionDraft(
  val id: String,
  val title: String = "",
  val category: String = "Ujian",
  val form: String = "Pilihan Ganda",
  val dateIso: String = LocalDate.now().toString(),
  val instruction: String = "",
  val questionsText: String = "",
  val sections: List<MapelQuestionSection> = emptyList(),
  val statusLabel: String = "Draft",
  val updatedAt: Long = System.currentTimeMillis()
) {
  fun questionCount(): Int {
    if (sections.isNotEmpty()) return sections.sumOf { it.effectiveQuestionCount() }
    val numbered = Regex("""(?m)^\s*\d+[\).]\s+""").findAll(questionsText).count()
    return numbered.takeIf { it > 0 } ?: questionsText.lines().count { it.trim().isNotBlank() }
  }

  fun previewText(): String {
    return when {
      sections.isNotEmpty() -> sections.joinToString(", ") {
        val count = it.effectiveQuestionCount()
        if (count > 0) "${it.typeLabel} $count nomor" else "${it.typeLabel} belum diisi"
      }
      instruction.isNotBlank() -> instruction
      questionsText.isNotBlank() -> questionsText.lines().firstOrNull { it.isNotBlank() }.orEmpty()
      else -> "Ketuk card untuk membuka editor soal."
    }
  }

  fun toExportData(): MapelQuestionExportData {
    return MapelQuestionExportData(
      id = id,
      title = title,
      category = category,
      form = form,
      dateLabel = dateIso,
      instruction = exportInstructionText(),
      questionsText = exportQuestionText()
    )
  }

  private fun exportInstructionText(): String {
    if (instruction.isNotBlank()) return instruction
    return sections
      .filter { it.instruction.isNotBlank() }
      .joinToString("\n") { "${it.typeLabel}: ${it.instruction}" }
  }

  private fun exportQuestionText(): String {
    if (sections.isEmpty()) return questionsText
    return sections.mapIndexed { index, section ->
      buildString {
        val sectionCount = section.effectiveQuestionCount()
        appendLine("MODEL ${modelOrdinalLabel(index)}: ${section.typeLabel} (${sectionCount} nomor)")
        if (section.instruction.isNotBlank()) appendLine("Instruksi: ${section.instruction}")
        appendLine()
        if (section.typeKey == "pilihan-ganda") {
          val questions = section.choiceQuestions.filledQuestions()
          if (questions.isEmpty() && section.content.isNotBlank()) {
            appendLine(section.content)
          } else {
            val printableQuestions = questions.ifEmpty { listOf(MapelChoiceQuestion(id = "placeholder")) }
            printableQuestions.forEachIndexed { questionIndex, question ->
              appendLine("${questionIndex + 1}. ${question.prompt.ifBlank { "........................................................" }}")
              val filledOptions = question.options.normalizedChoiceOptions().filter { it.isNotBlank() }
              if (filledOptions.isEmpty()) {
                appendLine("   A. ........................................................")
                appendLine("   B. ........................................................")
              } else {
                filledOptions.forEachIndexed { optionIndex, option ->
                  appendLine("   ${('A'.code + optionIndex).toChar()}. $option")
                }
              }
              if (questionIndex < printableQuestions.lastIndex) appendLine()
            }
          }
        } else if (section.typeKey.usesQuestionPromptEditor()) {
          val questions = section.choiceQuestions.filledQuestions()
          if (questions.isNotEmpty() || section.content.isBlank()) {
            val printableQuestions = questions.ifEmpty { listOf(MapelChoiceQuestion(id = "placeholder")) }
            printableQuestions.forEachIndexed { questionIndex, question ->
              appendLine("${questionIndex + 1}. ${question.prompt.ifBlank { "........................................................" }}")
              if (questionIndex < printableQuestions.lastIndex) appendLine()
            }
          } else {
            appendLine(section.content)
          }
        } else if (section.content.isNotBlank()) {
          appendLine(section.content)
        } else {
          repeat(section.count.coerceAtLeast(1)) { number ->
            appendLine("${number + 1}. ........................................................")
          }
        }
      }.trim()
    }.joinToString("\n\n")
  }
}

private data class MapelQuestionSection(
  val id: String,
  val typeKey: String,
  val typeLabel: String,
  val count: Int,
  val instruction: String = "",
  val content: String = "",
  val scoreText: String = "",
  val choiceQuestions: List<MapelChoiceQuestion> = emptyList()
)

private data class SoalModelPalette(
  val background: Color,
  val border: Color,
  val accent: Color,
  val text: Color
)

private fun MapelQuestionSection.modelPalette(): SoalModelPalette {
  return when (typeKey) {
    "pilihan-ganda" -> SoalModelPalette(Color(0xFFEFF6FF), Color(0xFFBFDBFE), Color(0xFF2563EB), Color(0xFF1E3A8A))
    "benar-salah" -> SoalModelPalette(Color(0xFFECFDF5), Color(0xFFA7F3D0), Color(0xFF059669), Color(0xFF065F46))
    "cari-kata" -> SoalModelPalette(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFD97706), Color(0xFF92400E))
    "teka-silang" -> SoalModelPalette(Color(0xFFF5F3FF), Color(0xFFDDD6FE), Color(0xFF7C3AED), Color(0xFF4C1D95))
    "esai" -> SoalModelPalette(Color(0xFFFFF7ED), Color(0xFFFED7AA), Color(0xFFEA580C), Color(0xFF9A3412))
    "pasangkan-kata" -> SoalModelPalette(Color(0xFFFDF2F8), Color(0xFFFBCFE8), Color(0xFFDB2777), Color(0xFF9D174D))
    "isi-titik" -> SoalModelPalette(Color(0xFFF0FDFA), Color(0xFF99F6E4), Color(0xFF0D9488), Color(0xFF115E59))
    else -> SoalModelPalette(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFF2563EB), Color(0xFF1E3A8A))
  }
}

private data class MapelChoiceQuestion(
  val id: String,
  val prompt: String = "",
  val options: List<String> = listOf("", "")
)

private data class SoalTypeOption(
  val key: String,
  val label: String
)

private val SoalCategoryOptions = listOf(
  "Tugas",
  "Kuis",
  "Ulangan Harian",
  "UTS",
  "UAS",
  "Remedial",
  "Latihan"
)

private val SoalTypeOptions = listOf(
  SoalTypeOption("pilihan-ganda", "Pilihan Ganda"),
  SoalTypeOption("esai", "Esai"),
  SoalTypeOption("isi-titik", "Isian"),
  SoalTypeOption("benar-salah", "Benar / Salah"),
  SoalTypeOption("pasangkan-kata", "Pasangkan Kata"),
  SoalTypeOption("cari-kata", "Cari Kata"),
  SoalTypeOption("teka-silang", "Teka-Teki Silang")
)

private fun String.usesQuestionPromptEditor(): Boolean {
  return this in SoalTypeOptions.map { it.key }
}

private fun modelOrdinalLabel(index: Int): String {
  var current = index.coerceAtLeast(0)
  val label = StringBuilder()
  do {
    label.insert(0, ('A'.code + (current % 26)).toChar())
    current = (current / 26) - 1
  } while (current >= 0)
  return label.toString()
}

private fun List<MapelChoiceQuestion>.normalizedForCount(count: Int): List<MapelChoiceQuestion> {
  val safeCount = count.coerceIn(1, 200)
  return (0 until safeCount).map { index ->
    val current = getOrNull(index)
    current?.copy(options = current.options.normalizedChoiceOptions())
      ?: MapelChoiceQuestion(id = "q-${index + 1}", options = listOf("", ""))
  }
}

private fun List<MapelChoiceQuestion>.withChoiceQuestionCapacity(count: Int): List<MapelChoiceQuestion> {
  val safeCount = count.coerceIn(1, 200)
  val questions = mapIndexed { index, current ->
    current.copy(
      id = current.id.ifBlank { "q-${index + 1}" },
      options = current.options.normalizedChoiceOptions()
    )
  }.toMutableList()
  while (questions.size < safeCount) {
    questions.add(MapelChoiceQuestion(id = "q-${questions.size + 1}", options = listOf("", "")))
  }
  return questions
}

private fun List<MapelChoiceQuestion>.visibleForCount(count: Int): List<MapelChoiceQuestion> {
  return withChoiceQuestionCapacity(count).take(count.coerceIn(1, 200))
}

private fun List<MapelChoiceQuestion>.withTrailingBlankQuestion(): List<MapelChoiceQuestion> {
  val cleanQuestions = mapIndexed { index, current ->
    current.copy(
      id = current.id.ifBlank { "q-${index + 1}" },
      options = current.options.normalizedChoiceOptions()
    )
  }.toMutableList()
  while (cleanQuestions.isNotEmpty() && !cleanQuestions.last().hasQuestionContent()) {
    cleanQuestions.removeAt(cleanQuestions.lastIndex)
  }
  cleanQuestions.add(MapelChoiceQuestion(id = "q-${cleanQuestions.size + 1}", options = listOf("", "")))
  return cleanQuestions
}

private fun List<MapelChoiceQuestion>.filledQuestions(): List<MapelChoiceQuestion> {
  return map { it.copy(options = it.options.normalizedChoiceOptions()) }
    .filter { it.hasQuestionContent() }
}

private fun List<MapelChoiceQuestion>.filledQuestionCount(): Int {
  return filledQuestions().size
}

private fun MapelChoiceQuestion.hasQuestionContent(): Boolean {
  return prompt.isNotBlank() || options.any { it.isNotBlank() }
}

private fun MapelQuestionSection.effectiveQuestionCount(): Int {
  return when {
    typeKey.usesQuestionPromptEditor() -> choiceQuestions.filledQuestionCount()
    content.isNotBlank() -> count.coerceAtLeast(1)
    else -> count.coerceAtLeast(0)
  }
}

private fun List<String>.normalizedChoiceOptions(): List<String> {
  val withoutExtraTrailingBlank = dropLastWhile { it.isBlank() }.toMutableList()
  withoutExtraTrailingBlank.add("")
  while (withoutExtraTrailingBlank.size < 2) {
    withoutExtraTrailingBlank.add("")
  }
  return withoutExtraTrailingBlank
}

private fun List<MapelQuestionDraft>.toCombinedExportData(subject: SubjectOverview): MapelQuestionExportData {
  val sorted = sortedBy { it.updatedAt }
  return MapelQuestionExportData(
    id = "combined-${subject.id}-${System.currentTimeMillis()}",
    title = "Kumpulan Soal ${subject.title}",
    category = "Kumpulan Soal",
    form = sorted.map { it.form }.filter { it.isNotBlank() }.distinct().joinToString(", "),
    dateLabel = LocalDate.now().toString(),
    instruction = "Dokumen ini berisi ${sorted.size} paket soal yang dibuat guru.",
    questionsText = sorted.joinToString("\n\n") { draft ->
      buildString {
        appendLine(draft.title.ifBlank { "Soal" })
        appendLine("Kategori: ${draft.category.ifBlank { "-" }}")
        appendLine("Bentuk: ${draft.form.ifBlank { "-" }}")
        val instructionText = draft.toExportData().instruction
        if (instructionText.isNotBlank()) {
          appendLine("Instruksi: $instructionText")
        }
        appendLine()
        append(draft.toExportData().questionsText.ifBlank { "Belum ada isi soal." })
      }
    }
  )
}

private fun loadChoiceQuestions(array: JSONArray?): List<MapelChoiceQuestion> {
  if (array == null) return emptyList()
  return buildList {
    for (index in 0 until array.length()) {
      val row = array.optJSONObject(index) ?: continue
      val optionsArray = row.optJSONArray("options")
      val options = buildList {
        if (optionsArray != null) {
          for (optionIndex in 0 until optionsArray.length()) {
            add(optionsArray.optString(optionIndex))
          }
        }
      }.normalizedChoiceOptions()
      add(
        MapelChoiceQuestion(
          id = row.optString("id").ifBlank { "q-${index + 1}" },
          prompt = row.optString("prompt"),
          options = options
        )
      )
    }
  }
}

private fun loadMapelQuestions(context: Context, subjectId: String): List<MapelQuestionDraft> {
  val rawJson = context.getSharedPreferences("mapel_questions", Context.MODE_PRIVATE)
    .getString(subjectId, "[]")
    .orEmpty()
  return runCatching {
    val array = JSONArray(rawJson.ifBlank { "[]" })
    buildList {
      for (index in 0 until array.length()) {
        val row = array.optJSONObject(index) ?: continue
        val sectionArray = row.optJSONArray("sections")
        val sections = buildList {
          if (sectionArray != null) {
            for (sectionIndex in 0 until sectionArray.length()) {
              val sectionRow = sectionArray.optJSONObject(sectionIndex) ?: continue
              val rawKey = sectionRow.optString("typeKey")
              val rawLabel = sectionRow.optString("typeLabel")
              val option = SoalTypeOptions.firstOrNull { option ->
                option.key == rawKey || option.label.equals(rawLabel, ignoreCase = true)
              }
              add(
                MapelQuestionSection(
                  id = sectionRow.optString("id").ifBlank { "model-${System.currentTimeMillis()}-$sectionIndex" },
                  typeKey = option?.key ?: rawKey.ifBlank { SoalTypeOptions.first().key },
                  typeLabel = option?.label ?: rawLabel.ifBlank { SoalTypeOptions.first().label },
                  count = sectionRow.optInt("count", 1).coerceIn(1, 200),
                  instruction = sectionRow.optString("instruction"),
                  content = sectionRow.optString("content"),
                  scoreText = sectionRow.optString("scoreText"),
                  choiceQuestions = loadChoiceQuestions(sectionRow.optJSONArray("choiceQuestions"))
                )
              )
            }
          }
        }
        add(
          MapelQuestionDraft(
            id = row.optString("id").ifBlank { "soal-${System.currentTimeMillis()}-$index" },
            title = row.optString("title"),
            category = row.optString("category").ifBlank { "Ujian" },
            form = row.optString("form").ifBlank { sections.map { it.typeLabel }.distinct().joinToString(", ") },
            dateIso = row.optString("dateIso").ifBlank { LocalDate.now().toString() },
            instruction = row.optString("instruction"),
            questionsText = row.optString("questionsText"),
            sections = sections,
            statusLabel = row.optString("statusLabel").ifBlank { "Draft" },
            updatedAt = row.optLong("updatedAt", System.currentTimeMillis())
          )
        )
      }
    }.sortedByDescending { it.updatedAt }
  }.getOrDefault(emptyList())
}

private fun saveMapelQuestions(
  context: Context,
  subjectId: String,
  questions: List<MapelQuestionDraft>
) {
  val array = JSONArray()
  questions.forEach { item ->
    array.put(
      JSONObject().apply {
        put("id", item.id)
        put("title", item.title)
        put("category", item.category)
        put("form", item.form)
        put("dateIso", item.dateIso)
        put("instruction", item.instruction)
        put("questionsText", item.questionsText)
        put(
          "sections",
          JSONArray().apply {
            item.sections.forEach { section ->
              put(
                JSONObject().apply {
                  put("id", section.id)
                  put("typeKey", section.typeKey)
                  put("typeLabel", section.typeLabel)
                  put("count", section.count)
                  put("instruction", section.instruction)
                  put("content", section.content)
                  put("scoreText", section.scoreText)
                  put(
                    "choiceQuestions",
                    JSONArray().apply {
                      section.choiceQuestions.forEach { question ->
                        put(
                          JSONObject().apply {
                            put("id", question.id)
                            put("prompt", question.prompt)
                            put(
                              "options",
                              JSONArray().apply {
                                question.options.forEach { option -> put(option) }
                              }
                            )
                          }
                        )
                      }
                    }
                  )
                }
              )
            }
          }
        )
        put("statusLabel", item.statusLabel)
        put("updatedAt", item.updatedAt)
      }
    )
  }
  context.getSharedPreferences("mapel_questions", Context.MODE_PRIVATE)
    .edit()
    .putString(subjectId, array.toString())
    .apply()
}

private data class AttendanceDateOverview(
  val dateIso: String,
  val entries: List<AttendanceDateStudentEntry>,
  val stats: List<AttendanceChartStat>
)

private data class AttendanceDateStudentEntry(
  val studentId: String,
  val studentName: String,
  val status: String,
  val history: AttendanceHistoryEntry
)

private data class ScoreMetric(
  val key: String,
  val label: String,
  val value: Double?,
  val maxValue: Int? = null,
  val editable: Boolean = true,
  val detailCount: Int = 0,
  val detailRows: List<ScoreDetailRow> = emptyList()
)

private data class ScoreTypeOverview(
  val key: String,
  val label: String,
  val metric: ScoreMetricDefinition,
  val averageLabel: String,
  val entries: List<ScoreTypeStudentEntry>
)

private data class ScoreTypeStudentEntry(
  val student: ScoreStudent,
  val metric: ScoreMetricDefinition,
  val value: Double?
)

private data class ScoreDateOverview(
  val key: String,
  val dateIso: String,
  val metric: ScoreMetricDefinition,
  val averageLabel: String,
  val entries: List<ScoreDateEntry>
)

private data class ScoreDateEntry(
  val student: ScoreStudent,
  val metric: ScoreMetricDefinition,
  val row: ScoreDetailRow
)

private data class ScoreMetricDefinition(
  val key: String,
  val label: String,
  val maxValue: Int? = null,
  val editable: Boolean = true
)

private data class ScoreDetailDraftRow(
  val id: String = "",
  val dateIso: String = "",
  val valueText: String = "",
  val material: String = ""
)

private fun resolveAttendancePalette(status: String): AttendancePalette {
  return when (status.trim()) {
    "Hadir" -> AttendancePalette(
      background = Color(0xFFECFDF5),
      border = Color(0xFFA7F3D0),
      accent = Color(0xFF047857),
      text = Color(0xFF047857)
    )

    "Terlambat" -> AttendancePalette(
      background = Color(0xFFFFF7ED),
      border = Color(0xFFFDBA74),
      accent = Color(0xFFC2410C),
      text = Color(0xFFC2410C)
    )

    "Sakit" -> AttendancePalette(
      background = Color(0xFFF5E8FF),
      border = Color(0xFFD8B4FE),
      accent = Color(0xFF9333EA),
      text = Color(0xFF9333EA)
    )

    "Izin" -> AttendancePalette(
      background = Color(0xFFEFF6FF),
      border = Color(0xFF93C5FD),
      accent = Color(0xFF1D4ED8),
      text = Color(0xFF1D4ED8)
    )

    "Alpa" -> AttendancePalette(
      background = Color(0xFFFEF2F2),
      border = Color(0xFFFCA5A5),
      accent = Color(0xFFB91C1C),
      text = Color(0xFFB91C1C)
    )

    else -> AttendancePalette(
      background = Color(0xFFF8FAFC),
      border = Color(0xFFCBD5E1),
      accent = Color(0xFF64748B),
      text = Color(0xFF334155)
    )
  }
}

private fun buildAttendanceDateOverviews(students: List<AttendanceStudent>): List<AttendanceDateOverview> {
  val grouped = linkedMapOf<String, MutableList<AttendanceDateStudentEntry>>()
  students.forEach { student ->
    student.history.forEach { entry ->
      val dateIso = entry.dateIso.trim()
      if (dateIso.isBlank()) return@forEach
      grouped.getOrPut(dateIso) { mutableListOf() }.add(
        AttendanceDateStudentEntry(
          studentId = student.id,
          studentName = student.name,
          status = entry.status,
          history = entry
        )
      )
    }
  }

  return grouped.entries
    .sortedByDescending { it.key }
    .map { (dateIso, entries) ->
      val sortedEntries = entries.sortedBy { it.studentName.lowercase() }
      val counts = sortedEntries.groupingBy { it.status }.eachCount()
      val total = sortedEntries.size.coerceAtLeast(1)
      val stats = AttendanceStatusOptions.mapNotNull { status ->
        val count = counts[status] ?: 0
        if (count <= 0) null else {
          AttendanceChartStat(
            label = status,
            percent = ((count.toDouble() / total.toDouble()) * 100.0).toInt(),
            palette = resolveAttendancePalette(status)
          )
        }
      }
      AttendanceDateOverview(
        dateIso = dateIso,
        entries = sortedEntries,
        stats = stats
      )
    }
}

private val ScoreMetricDefinitions = listOf(
  ScoreMetricDefinition("nilai_tugas", "Tugas", maxValue = 5),
  ScoreMetricDefinition("nilai_ulangan_harian", "Ulangan Harian", maxValue = 10),
  ScoreMetricDefinition("nilai_pts", "UTS", maxValue = 25),
  ScoreMetricDefinition("nilai_pas", "UAS", maxValue = 50),
  ScoreMetricDefinition("nilai_kehadiran", "Kehadiran", editable = false),
  ScoreMetricDefinition("nilai_akhir", "Nilai Akhir", editable = false),
  ScoreMetricDefinition("nilai_keterampilan", "Keterampilan", maxValue = 100)
)

private fun buildScoreMetrics(student: ScoreStudent): List<ScoreMetric> {
  return ScoreMetricDefinitions.map { metric ->
    val detailRows = student.detailRowsByMetric[metric.key].orEmpty()
    ScoreMetric(
      key = metric.key,
      label = metric.label,
      value = metricValueFromStudent(student, metric.key),
      maxValue = metric.maxValue,
      editable = metric.editable,
      detailCount = detailRows.size,
      detailRows = detailRows
    )
  }
}

private fun buildScoreTypeOverviews(students: List<ScoreStudent>): List<ScoreTypeOverview> {
  return ScoreMetricDefinitions.map { metric ->
    val entries = students.map { student ->
      ScoreTypeStudentEntry(
        student = student,
        metric = metric,
        value = metricValueFromStudent(student, metric.key)
      )
    }
    val numericValues = entries.mapNotNull { it.value }
    ScoreTypeOverview(
      key = metric.key,
      label = metric.label,
      metric = metric,
      averageLabel = if (numericValues.isEmpty()) "-" else formatScoreNumber(numericValues.average()),
      entries = entries.sortedBy { it.student.name.lowercase() }
    )
  }
}

private fun buildScoreDateOverviews(students: List<ScoreStudent>): List<ScoreDateOverview> {
  val grouped = linkedMapOf<String, MutableList<ScoreDateEntry>>()
  students.forEach { student ->
    ScoreMetricDefinitions
      .filter { it.editable }
      .forEach { metric ->
        student.detailRowsByMetric[metric.key].orEmpty().forEach { row ->
          val dateIso = row.dateIso.trim().take(10)
          if (dateIso.isBlank()) return@forEach
          if (row.id.isBlank() && row.value == null) return@forEach
          val groupKey = "$dateIso|${metric.key}"
          grouped.getOrPut(groupKey) { mutableListOf() }.add(
            ScoreDateEntry(
              student = student,
              metric = metric,
              row = row.copy(dateIso = dateIso)
            )
          )
        }
      }
  }

  return grouped.entries
    .sortedWith(
      compareByDescending<Map.Entry<String, MutableList<ScoreDateEntry>>> {
        it.value.firstOrNull()?.row?.dateIso.orEmpty()
      }.thenBy { entry ->
        val metricKey = entry.value.firstOrNull()?.metric?.key.orEmpty()
        ScoreMetricDefinitions.indexOfFirst { it.key == metricKey }.let { if (it < 0) Int.MAX_VALUE else it }
      }
    )
    .mapNotNull { (key, entries) ->
      val firstEntry = entries.firstOrNull() ?: return@mapNotNull null
      val dateIso = firstEntry.row.dateIso
      val metric = firstEntry.metric
      val sortedEntries = entries.sortedWith(
        compareBy<ScoreDateEntry> { it.student.name.lowercase() }
      )
      val values = sortedEntries.mapNotNull { it.row.value }
      ScoreDateOverview(
        key = key,
        dateIso = dateIso,
        metric = metric,
        averageLabel = if (values.isEmpty()) "-" else formatScoreNumber(values.average()),
        entries = sortedEntries
      )
    }
}

private fun metricValueFromStudent(student: ScoreStudent, key: String): Double? {
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

private fun buildScoreDateDeleteStudents(overview: ScoreDateOverview): List<ScoreStudent> {
  return overview.entries
    .groupBy { it.student.id }
    .values
    .flatMap { studentEntries ->
      val student = studentEntries.first().student
      val metrics = buildScoreMetrics(student).associateBy { it.key }
      studentEntries
        .groupBy { it.metric.key }
        .mapNotNull { (metricKey, entries) ->
          val metric = metrics[metricKey] ?: return@mapNotNull null
          val deletedIds = entries.map { it.row.id }.filter { it.isNotBlank() }.distinct()
          if (deletedIds.isEmpty()) return@mapNotNull null
          val remainingRows = metric.detailRows.filterNot { row ->
            row.id in deletedIds || row.dateIso.take(10) == overview.dateIso && row.id.isBlank()
          }
          updateScoreStudentWithDetailRows(
            student = student,
            metric = metric,
            rows = remainingRows,
            deletedIds = deletedIds
          )
        }
    }
}

private fun buildUpdatedScoreStudent(
  student: ScoreStudent,
  draftValues: Map<String, String>
): ScoreStudent? {
  val parsed = ScoreMetricDefinitions.associate { metric ->
    val value = parseScoreDraftValue(draftValues[metric.key].orEmpty(), metric.maxValue)
      ?: return null
    metric.key to value
  }
  return student.copy(
    nilaiTugas = parsed["nilai_tugas"],
    nilaiUlanganHarian = parsed["nilai_ulangan_harian"],
    nilaiPts = parsed["nilai_pts"],
    nilaiPas = parsed["nilai_pas"],
    nilaiKehadiran = parsed["nilai_kehadiran"],
    nilaiKeterampilan = parsed["nilai_keterampilan"],
    nilaiAkhir = listOf(
      parsed["nilai_tugas"] ?: 0.0,
      parsed["nilai_ulangan_harian"] ?: 0.0,
      parsed["nilai_pts"] ?: 0.0,
      parsed["nilai_pas"] ?: 0.0,
      parsed["nilai_kehadiran"] ?: 0.0
    ).sum()
  )
}

private fun updateStudentScoreByMetric(
  student: ScoreStudent,
  metric: ScoreMetricDefinition,
  valueText: String
): ScoreStudent? {
  val value = parseScoreDraftValue(valueText, metric.maxValue) ?: return null
  return when (metric.key) {
    "nilai_tugas" -> student.copy(nilaiTugas = value)
    "nilai_ulangan_harian" -> student.copy(nilaiUlanganHarian = value)
    "nilai_pts" -> student.copy(nilaiPts = value)
    "nilai_pas" -> student.copy(nilaiPas = value)
    "nilai_kehadiran" -> student.copy(nilaiKehadiran = value)
    "nilai_keterampilan" -> student.copy(nilaiKeterampilan = value)
    else -> student
  }.let { updated ->
    updated.copy(
      nilaiAkhir = listOf(
        updated.nilaiTugas ?: 0.0,
        updated.nilaiUlanganHarian ?: 0.0,
        updated.nilaiPts ?: 0.0,
        updated.nilaiPas ?: 0.0,
        updated.nilaiKehadiran ?: 0.0
      ).sum()
    )
  }
}

private fun updateScoreStudentWithDetailRows(
  student: ScoreStudent,
  metric: ScoreMetric,
  rows: List<ScoreDetailRow>,
  deletedIds: List<String>
): ScoreStudent {
  val average = calculateScoreDetailAverage(rows.mapNotNull { it.value })
  val summaryValue = average ?: defaultScoreValueForMetric(metric.key)
  val detailRowsByMetric = student.detailRowsByMetric.toMutableMap().apply {
    put(metric.key, rows.sortedByDescending { it.dateIso })
  }
  return applyScoreMetricValue(student, metric.key, summaryValue).copy(
    detailRowsByMetric = detailRowsByMetric,
    pendingDetailMetricKey = metric.key,
    pendingDeletedDetailIds = deletedIds
  )
}

private fun applyScoreMetricValue(
  student: ScoreStudent,
  metricKey: String,
  value: Double?
): ScoreStudent {
  val updated = when (metricKey) {
    "nilai_tugas" -> student.copy(nilaiTugas = value)
    "nilai_ulangan_harian" -> student.copy(nilaiUlanganHarian = value)
    "nilai_pts" -> student.copy(nilaiPts = value)
    "nilai_pas" -> student.copy(nilaiPas = value)
    "nilai_keterampilan" -> student.copy(nilaiKeterampilan = value)
    else -> student
  }
  return updated.copy(
    nilaiAkhir = listOf(
      updated.nilaiTugas ?: 0.0,
      updated.nilaiUlanganHarian ?: 0.0,
      updated.nilaiPts ?: 0.0,
      updated.nilaiPas ?: 0.0,
      updated.nilaiKehadiran ?: 0.0
    ).sum()
  )
}

private fun defaultScoreValueForMetric(metricKey: String): Double? {
  return when (metricKey) {
    "nilai_tugas" -> null
    "nilai_ulangan_harian",
    "nilai_pts",
    "nilai_pas",
    "nilai_keterampilan" -> 0.0
    else -> null
  }
}

private fun ScoreDetailRow.toDraftRow(): ScoreDetailDraftRow {
  return ScoreDetailDraftRow(
    id = id,
    dateIso = dateIso,
    valueText = value?.let(::formatScoreNumber).orEmpty(),
    material = normalizeScoreMaterialText(material)
  )
}

private fun calculateScoreDetailAverage(values: List<Double>): Double? {
  if (values.isEmpty()) return null
  return kotlin.math.round((values.sum() / values.size.toDouble()) * 100.0) / 100.0
}

private fun parseScoreDraftValue(value: String, maxValue: Int?): Double? {
  val trimmed = value.trim()
  if (trimmed.isBlank()) return null
  val number = trimmed.replace(',', '.').toDoubleOrNull() ?: return null
  if (number < 0) return null
  if (maxValue != null && number > maxValue.toDouble()) return null
  return number
}

private fun sanitizeScoreDraftValueText(value: String, maxValue: Int?): String? {
  val normalized = value.replace(',', '.')
  if (normalized.isBlank()) return ""
  if (normalized.count { it == '.' } > 1) return null
  if (normalized.any { !it.isDigit() && it != '.' }) return null
  if (normalized == ".") return normalized

  val parsed = normalized.toDoubleOrNull() ?: return null
  if (parsed < 0) return null
  if (maxValue != null && parsed > maxValue.toDouble()) return maxValue.toString()
  return normalized
}

private fun normalizeScoreMaterialText(value: String): String {
  val normalized = value.trim()
  return if (normalized.equals("null", ignoreCase = true)) "" else normalized
}

private fun formatScoreNumber(value: Double): String {
  val rounded = kotlin.math.round(value * 100.0) / 100.0
  return if (rounded % 1.0 == 0.0) {
    rounded.toInt().toString()
  } else {
    rounded.toString()
  }
}

private val AttendanceStatusOptions = listOf("Hadir", "Terlambat", "Sakit", "Izin", "Alpa")

private val AttendanceDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID"))

private fun formatAttendanceDate(dateIso: String): String {
  return runCatching {
    LocalDate.parse(dateIso).format(AttendanceDateFormatter)
  }.getOrDefault(dateIso)
}

@Composable
private fun MapelTopBar(
  title: String,
  onMenuClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp, start = 18.dp, end = 18.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    MapelTopButton(
      icon = Icons.Outlined.Menu,
      contentDescription = "Buka sidebar",
      onClick = onMenuClick
    )
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 12.dp),
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.size(42.dp))
  }
}

@Composable
private fun MapelDetailTopBar(
  title: String,
  onBackClick: () -> Unit,
  showUndo: Boolean = false,
  onUndoClick: () -> Unit = {}
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp, start = 18.dp, end = 18.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    MapelTopButton(
      icon = Icons.AutoMirrored.Outlined.ArrowBack,
      contentDescription = "Kembali ke daftar mapel",
      onClick = onBackClick
    )
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 12.dp),
      textAlign = TextAlign.Center
    )
    if (showUndo) {
      MapelTopButton(
        icon = Icons.AutoMirrored.Outlined.Undo,
        contentDescription = "Batalkan hapus",
        onClick = onUndoClick
      )
    } else {
      Spacer(modifier = Modifier.size(42.dp))
    }
  }
}

@Composable
private fun MapelTopButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(42.dp)
      .background(Color.White.copy(alpha = 0.86f), androidx.compose.foundation.shape.CircleShape)
      .border(1.dp, CardBorder, androidx.compose.foundation.shape.CircleShape)
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

@Composable
private fun MapelStatChip(
  label: String,
  value: String,
  tone: Color,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(tone.copy(alpha = 0.12f))
      .border(1.dp, tone.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
      .padding(horizontal = 12.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    horizontalAlignment = Alignment.CenterHorizontally
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
      fontWeight = FontWeight.ExtraBold
    )
  }
}

private fun resolveMapelVisual(subject: SubjectOverview): MapelCardVisual {
  val palette = listOf(
    Color(0xFFE8F1FF) to Color(0xFF2563EB),
    Color(0xFFEAFBF3) to Color(0xFF059669),
    Color(0xFFFFF3E7) to Color(0xFFD97706),
    Color(0xFFF6EEFF) to Color(0xFF7C3AED),
    Color(0xFFFFEDF3) to Color(0xFFE11D48)
  )
  val tone = palette[(subject.title.hashCode().absoluteValue) % palette.size]
  val icon = when {
    "nahwu" in subject.title.lowercase() || "sharf" in subject.title.lowercase() -> Icons.Outlined.Translate
    "bahasa" in subject.title.lowercase() -> Icons.Outlined.MenuBook
    "fisika" in subject.title.lowercase() || "kimia" in subject.title.lowercase() -> Icons.Outlined.Science
    "mat" in subject.title.lowercase() -> Icons.Outlined.Calculate
    else -> Icons.Outlined.AutoStories
  }
  return MapelCardVisual(
    background = tone.first,
    iconTint = tone.second,
    icon = icon
  )
}

private fun buildMapelDescription(subject: SubjectOverview): String {
  return buildString {
    append(subject.className)
    append(" • ")
    append(subject.semester)
    append(" • ")
    append("${subject.materialCount} materi")
  }
}

private val Int.absoluteValue: Int
  get() = if (this < 0) -this else this
