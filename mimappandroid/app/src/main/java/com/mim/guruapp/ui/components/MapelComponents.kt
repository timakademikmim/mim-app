package com.mim.guruapp.ui.components

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import androidx.core.content.FileProvider
import com.mim.guruapp.AttendanceSaveOutcome
import com.mim.guruapp.PatronMateriSaveOutcome
import com.mim.guruapp.QuestionSaveOutcome
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
import com.mim.guruapp.data.remote.GuruAiGenerateRequest
import com.mim.guruapp.data.remote.GuruAiGenerateResult
import com.mim.guruapp.data.remote.GuruAiMaterialItem
import com.mim.guruapp.data.remote.GuruAiMaterialResult
import com.mim.guruapp.data.remote.GuruAiQuestionResult
import com.mim.guruapp.data.remote.GuruAiTokenWallet
import com.mim.guruapp.export.MapelAttendanceExcelExporter
import com.mim.guruapp.export.MapelQuestionExportData
import com.mim.guruapp.export.MapelQuestionExporter
import com.mim.guruapp.ui.i18n.LocalAppLanguage
import com.mim.guruapp.ui.i18n.formatDateForLanguage
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.io.File
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
  onLoadQuestions: suspend (String, SubjectOverview) -> String?,
  onSaveQuestions: suspend (String, SubjectOverview, String) -> QuestionSaveOutcome,
  onLoadAiTokenBalance: suspend () -> GuruAiTokenWallet?,
  onGenerateAiContent: suspend (GuruAiGenerateRequest) -> GuruAiGenerateResult,
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
        onLoadQuestions = onLoadQuestions,
        onSaveQuestions = onSaveQuestions,
        onLoadAiTokenBalance = onLoadAiTokenBalance,
        onGenerateAiContent = onGenerateAiContent,
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 124.dp),
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
      .background(CardBackground.copy(alpha = 0.92f))
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
        text = t("Mapel Tidak Tersedia"),
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
        .background(SoftPanel.copy(alpha = 0.8f))
        .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(999.dp))
        .clickable(onClick = onDismiss)
        .padding(horizontal = 14.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = t("Tutup"),
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
        .background(CardBackground.copy(alpha = 0.90f))
        .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
        .clickable(onClick = onToggleClaimSection)
        .padding(horizontal = 18.dp, vertical = 11.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = if (isClaimSectionVisible) t("Tutup") else t("Tambah Mapel"),
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
      .background(
        Brush.verticalGradient(
          listOf(
            CardBackground.copy(alpha = 0.96f),
            visual.background.copy(alpha = 0.16f),
            SoftPanel.copy(alpha = 0.94f)
          )
        )
      )
      .border(1.dp, CardBorder.copy(alpha = 0.72f), RoundedCornerShape(20.dp))
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
        .background(SoftPanel.copy(alpha = 0.92f))
        .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = t("Lihat detail"),
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
  onLoadQuestions: suspend (String, SubjectOverview) -> String?,
  onSaveQuestions: suspend (String, SubjectOverview, String) -> QuestionSaveOutcome,
  onLoadAiTokenBalance: suspend () -> GuruAiTokenWallet?,
  onGenerateAiContent: suspend (GuruAiGenerateRequest) -> GuruAiGenerateResult,
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
  var patronLearningMaterials by remember(subject.id) {
    mutableStateOf<Map<String, PatronLearningMaterial>>(emptyMap())
  }
  var activePatronLearningItemId by rememberSaveable(subject.id) { mutableStateOf<String?>(null) }
  var isPatronLearningEditMode by rememberSaveable(subject.id) { mutableStateOf(false) }
  var patronLearningUndoStack by remember(subject.id) { mutableStateOf<List<PatronLearningMaterial>>(emptyList()) }
  var questionItems by remember(subject.id) { mutableStateOf<List<MapelQuestionDraft>>(emptyList()) }
  var isCreatingQuestion by remember(subject.id) { mutableStateOf(false) }
  var isAddingQuestionModel by remember(subject.id) { mutableStateOf(false) }
  var editingQuestionDetail by remember(subject.id) { mutableStateOf<MapelQuestionDraft?>(null) }
  var attendanceExportShouldShare by remember(subject.id) { mutableStateOf<Boolean?>(null) }
  var attendanceExportMonthKeys by remember(subject.id) { mutableStateOf(listOf(YearMonth.now().toString())) }
  var isExportingAttendance by remember(subject.id) { mutableStateOf(false) }
  var activeQuestionId by rememberSaveable(subject.id) { mutableStateOf<String?>(null) }
  var editorQuestionDraft by remember(subject.id) { mutableStateOf<MapelQuestionDraft?>(null) }
  var questionUndoStack by remember(subject.id) { mutableStateOf<List<MapelQuestionDraft>>(emptyList()) }
  var isPrintingQuestion by remember(subject.id) { mutableStateOf(false) }
  var isSavingQuestionToServer by rememberSaveable(subject.id) { mutableStateOf(false) }
  var aiDialogFeature by rememberSaveable(subject.id) { mutableStateOf<String?>(null) }
  var aiPrompt by rememberSaveable(subject.id) { mutableStateOf("") }
  var aiCountText by rememberSaveable(subject.id) { mutableStateOf("5") }
  var aiQuestionTypeKey by rememberSaveable(subject.id) { mutableStateOf(SoalTypeOptions.first().key) }
  var aiLanguageCode by rememberSaveable(subject.id) { mutableStateOf("ID") }
  var aiWallet by remember(subject.id) { mutableStateOf<GuruAiTokenWallet?>(null) }
  var isLoadingAiWallet by rememberSaveable(subject.id) { mutableStateOf(false) }
  var isAiGenerating by rememberSaveable(subject.id) { mutableStateOf(false) }
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
  val activeQuestion = editorQuestionDraft
  val isQuestionEditorOpen = selectedSection == MapelDetailSection.Soal && activeQuestion != null
  val activePatronLearningItem = activePatronLearningItemId?.let { currentId ->
    patronMateriItems.firstOrNull { it.id == currentId }
  }
  val attendanceExportBaseMonth = remember(attendanceSnapshot) {
    attendanceSnapshot?.latestAttendanceMonth() ?: YearMonth.now()
  }

  fun persistQuestionItems(
    items: List<MapelQuestionDraft>,
    synchronous: Boolean = false
  ): String {
    return saveMapelQuestions(context, subject.id, items, synchronous = synchronous)
  }

  fun updateQuestionDraft(updated: MapelQuestionDraft) {
    val latestUpdated = updated.copy(
      statusLabel = QuestionStatusDraft,
      updatedAt = System.currentTimeMillis()
    )
    if (activeQuestionId == latestUpdated.id) {
      editorQuestionDraft = latestUpdated
    }
    val nextItems = questionItems.map { current ->
      if (current.id == latestUpdated.id) latestUpdated else current
    }.sortedByDescending { it.updatedAt }
    questionItems = nextItems
    persistQuestionItems(nextItems)
  }

  fun flushLatestQuestionItems(): List<MapelQuestionDraft> {
    val latestItems = if (activeQuestionId != null && editorQuestionDraft != null) {
      questionItems.map { current ->
        if (current.id == activeQuestionId) editorQuestionDraft!! else current
      }.sortedByDescending { it.updatedAt }
    } else {
      questionItems
    }
    questionItems = latestItems
    persistQuestionItems(latestItems, synchronous = true)
    return loadMapelQuestions(context, subject.id)
  }

  fun addQuestionModel(question: MapelQuestionDraft, typeKey: String) {
    val selectedType = SoalTypeOptions.firstOrNull { it.key == typeKey } ?: SoalTypeOptions.first()
    val newSection = buildQuestionSectionDraft(selectedType)
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
          val latestQuestion = if (activeQuestionId == question.id) {
            editorQuestionDraft ?: question
          } else {
            val latestQuestions = flushLatestQuestionItems()
            latestQuestions.firstOrNull { it.id == question.id } ?: question
          }
          val exportData = latestQuestion.toExportData()
          val docx = MapelQuestionExporter.createDocxFile(context, subject, exportData)
          MapelQuestionExporter.openDocument(context, docx, exportData)
        }.onFailure {
          Toast.makeText(
            context,
            it.message ?: "Gagal menyiapkan dokumen Word.",
            Toast.LENGTH_LONG
          ).show()
        }
        isPrintingQuestion = false
      }
    }
  }

  fun shareQuestion(question: MapelQuestionDraft) {
    if (!isPrintingQuestion) {
      detailScope.launch {
        isPrintingQuestion = true
        runCatching {
          val latestQuestion = if (activeQuestionId == question.id) {
            editorQuestionDraft ?: question
          } else {
            val latestQuestions = flushLatestQuestionItems()
            latestQuestions.firstOrNull { it.id == question.id } ?: question
          }
          val exportData = latestQuestion.toExportData()
          val docx = MapelQuestionExporter.createDocxFile(context, subject, exportData)
          MapelQuestionExporter.shareDocument(context, docx, exportData)
        }.onFailure {
          Toast.makeText(
            context,
            it.message ?: "Gagal menyiapkan dokumen Word.",
            Toast.LENGTH_LONG
          ).show()
        }
        isPrintingQuestion = false
      }
    }
  }

  fun saveActiveQuestionToServer() {
    val question = editorQuestionDraft ?: return
    if (isSavingQuestionToServer) return
    detailScope.launch {
      isSavingQuestionToServer = true
      val savedQuestion = question.copy(
        statusLabel = QuestionStatusSaved,
        updatedAt = System.currentTimeMillis()
      )
      val hasQuestionInList = questionItems.any { it.id == savedQuestion.id }
      val savingItems = if (hasQuestionInList) {
        questionItems.map { current ->
          if (current.id == savedQuestion.id) savedQuestion else current
        }
      } else {
        questionItems + savedQuestion
      }.sortedByDescending { it.updatedAt }
      questionItems = savingItems
      editorQuestionDraft = savedQuestion
      val savedLocalJson = persistQuestionItems(savingItems, synchronous = true)
      val serverQuestionJson = filterMapelQuestionJsonById(savedLocalJson, savedQuestion.id)
      val outcome = runCatching {
        if (serverQuestionJson == "[]") {
          QuestionSaveOutcome(false, "Data soal belum siap disimpan. Soal tetap draft lokal.")
        } else {
          onSaveQuestions(subject.id, subject, serverQuestionJson)
        }
      }.getOrElse { error ->
        QuestionSaveOutcome(
          success = false,
          message = error.message ?: "Gagal menyimpan soal ke database. Soal tetap draft lokal."
        )
      }
      if (outcome.success) {
        Toast.makeText(
          context,
          outcome.message.ifBlank { "Soal berhasil disimpan ke database." },
          Toast.LENGTH_SHORT
        ).show()
      } else {
        val draftQuestion = savedQuestion.copy(
          statusLabel = QuestionStatusDraft,
          updatedAt = System.currentTimeMillis()
        )
        val draftItems = savingItems.map { current ->
          if (current.id == draftQuestion.id) draftQuestion else current
        }.sortedByDescending { it.updatedAt }
        questionItems = draftItems
        editorQuestionDraft = draftQuestion
        persistQuestionItems(draftItems, synchronous = true)
        Toast.makeText(
          context,
          outcome.message.ifBlank { "Gagal menyimpan soal ke database. Soal tetap draft lokal." },
          Toast.LENGTH_LONG
        ).show()
      }
      isSavingQuestionToServer = false
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

  fun refreshAiWallet() {
    detailScope.launch {
      isLoadingAiWallet = true
      aiWallet = onLoadAiTokenBalance()
      isLoadingAiWallet = false
    }
  }

  fun openAiDialog(feature: String) {
    aiDialogFeature = feature
    aiCountText = if (feature == "soal") "5" else "3"
    aiQuestionTypeKey = SoalTypeOptions.first().key
    aiLanguageCode = if (feature == "soal") "ID" else "ID"
    aiPrompt = when (feature) {
      "soal" -> "Buat soal ${subject.title} untuk ${subject.className} ${subject.semester}."
      else -> "Buat materi ${subject.title} untuk ${subject.className} ${subject.semester}."
    }
    refreshAiWallet()
  }

  suspend fun applyAiMaterialResult(
    material: GuruAiMaterialResult,
    wallet: GuruAiTokenWallet,
    chargedTokens: Int
  ) {
    val generated = buildAiPatronMaterialPayload(
      generatedMaterial = material,
      currentItems = patronMateriItems,
      currentMaterials = patronLearningMaterials
    )
    if (generated.items.isEmpty()) {
      Toast.makeText(context, "AI belum menghasilkan materi yang bisa dipakai.", Toast.LENGTH_LONG).show()
      return
    }
    patronMateriItems = generated.items
    patronLearningMaterials = generated.materials
    savePatronLearningMaterials(context, subject.id, generated.materials)
    val outcome = savePatronMateriChanges(generated.items)
    aiWallet = wallet
    val tokenInfo = if (chargedTokens > 0) " Token terpakai: $chargedTokens." else ""
    val message = if (outcome.success) {
      outcome.message.ifBlank { "Materi AI berhasil dibuat." } + tokenInfo
    } else {
      outcome.message.ifBlank { "Materi AI tersimpan lokal, tapi belum tersimpan ke server." } + tokenInfo
    }
    Toast.makeText(
      context,
      message,
      if (outcome.success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
    ).show()
  }

  fun applyAiQuestionResult(
    question: GuruAiQuestionResult,
    wallet: GuruAiTokenWallet,
    chargedTokens: Int
  ) {
    val draft = buildAiQuestionDraft(
      subject = subject,
      result = question,
      fallbackTypeKey = aiQuestionTypeKey,
      fallbackLanguageCode = aiLanguageCode,
      academicYearLabel = defaultAcademicYearLabel()
    )
    val nextItems = (listOf(draft) + questionItems).sortedByDescending { it.updatedAt }
    questionItems = nextItems
    persistQuestionItems(nextItems, synchronous = true)
    activeQuestionId = draft.id
    editorQuestionDraft = draft
    aiWallet = wallet
    val tokenInfo = if (chargedTokens > 0) " Token terpakai: $chargedTokens." else ""
    Toast.makeText(context, "Soal AI berhasil dibuat sebagai draft.$tokenInfo", Toast.LENGTH_SHORT).show()
  }

  fun generateAiContentFromDialog() {
    val feature = aiDialogFeature ?: return
    if (isAiGenerating) return
    val safeCount = aiCountText.toIntOrNull()
      ?.coerceIn(1, if (feature == "soal") 50 else 20)
      ?: if (feature == "soal") 5 else 3
    detailScope.launch {
      isAiGenerating = true
      val result = onGenerateAiContent(
        GuruAiGenerateRequest(
          feature = feature,
          distribusiId = subject.id,
          subjectTitle = subject.title,
          className = subject.className,
          semester = subject.semester,
          languageCode = aiLanguageCode,
          prompt = aiPrompt,
          count = safeCount,
          questionType = if (feature == "soal") aiQuestionTypeKey else "",
          existingItems = if (feature == "soal") {
            questionItems.map { it.title }
          } else {
            patronMateriItems.map { it.text }
          },
          academicYear = defaultAcademicYearLabel()
        )
      )
      when (result) {
        is GuruAiGenerateResult.MaterialSuccess -> {
          applyAiMaterialResult(result.material, result.wallet, result.chargedTokens)
          aiDialogFeature = null
        }
        is GuruAiGenerateResult.QuestionSuccess -> {
          applyAiQuestionResult(result.question, result.wallet, result.chargedTokens)
          aiDialogFeature = null
        }
        is GuruAiGenerateResult.Error -> {
          aiWallet = result.wallet ?: aiWallet
          Toast.makeText(
            context,
            result.message.ifBlank { "Gagal membuat konten AI." },
            Toast.LENGTH_LONG
          ).show()
        }
      }
      isAiGenerating = false
    }
  }

  LaunchedEffect(subject.id) {
    isLoadingAttendance = true
    isLoadingScores = true
    isLoadingPatronMateri = true
    val localQuestions = loadMapelQuestions(context, subject.id)
    questionItems = localQuestions
    val remoteQuestions = onLoadQuestions(subject.id, subject)
      ?.let { decodeMapelQuestions(it) }
    val loadedQuestions = when {
      remoteQuestions == null -> localQuestions
      else -> {
        val mergedQuestions = mergeLocalAndRemoteMapelQuestions(localQuestions, remoteQuestions)
        persistQuestionItems(mergedQuestions)
        mergedQuestions
      }
    }
    questionItems = loadedQuestions
    patronLearningMaterials = loadPatronLearningMaterials(context, subject.id)
    attendanceSnapshot = onLoadAttendance(subject.id, subject)
    scoreSnapshot = onLoadScores(subject.id, subject)
    patronMateriSnapshot = onLoadPatronMateri(subject.id, subject)
    patronMateriItems = patronMateriSnapshot?.items.orEmpty()
    editorQuestionDraft = activeQuestionId?.let { currentId ->
      loadedQuestions.firstOrNull { it.id == currentId }
    }
    isLoadingAttendance = false
    isLoadingScores = false
    isLoadingPatronMateri = false
  }

  LaunchedEffect(selectedSection, activeQuestionId, questionItems) {
    if (selectedSection != MapelDetailSection.Soal) {
      activeQuestionId = null
      editorQuestionDraft = null
    } else if (activeQuestionId != null) {
      val matching = questionItems.firstOrNull { it.id == activeQuestionId }
      if (matching == null) {
        activeQuestionId = null
        editorQuestionDraft = null
      } else if (editorQuestionDraft == null || editorQuestionDraft?.id != activeQuestionId) {
        editorQuestionDraft = matching
      }
    } else {
      editorQuestionDraft = null
    }
    if (selectedSection != MapelDetailSection.Soal || (activeQuestionId != null && editorQuestionDraft == null)) {
      activeQuestionId = null
    }
    if (selectedSection != MapelDetailSection.PatronMateri) {
      activePatronLearningItemId = null
    }
  }

  LaunchedEffect(activeQuestionId) {
    questionUndoStack = emptyList()
  }

  LaunchedEffect(activePatronLearningItemId) {
    patronLearningUndoStack = emptyList()
  }

  LaunchedEffect(attendanceExportBaseMonth, subject.id) {
    if (attendanceExportShouldShare == null) {
      attendanceExportMonthKeys = listOf(attendanceExportBaseMonth.toString())
    }
  }

  BackHandler(enabled = selectedSection == MapelDetailSection.Soal && activeQuestionId != null) {
    activeQuestionId = null
    editorQuestionDraft = null
  }

  BackHandler(enabled = activePatronLearningItem != null) {
    isPatronLearningEditMode = false
    activePatronLearningItemId = null
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    topBar = {
      MapelDetailTopBar(
        title = if (activePatronLearningItem != null) "Materi Pembelajaran" else selectedSection.label,
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
            editorQuestionDraft = null
          } else if (activePatronLearningItem != null) {
            isPatronLearningEditMode = false
            activePatronLearningItemId = null
          } else {
            onBackClick()
          }
        },
        actions = if (isQuestionEditorOpen) {
          {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              AnimatedVisibility(visible = questionUndoStack.isNotEmpty()) {
                MapelTopButton(
                  icon = Icons.AutoMirrored.Outlined.Undo,
                  contentDescription = t("Batalkan hapus"),
                  onClick = onUndoClick@{
                    val restored = questionUndoStack.lastOrNull() ?: return@onUndoClick
                    questionUndoStack = questionUndoStack.dropLast(1)
                    updateQuestionDraft(restored)
                  }
                )
              }
              MapelTopButton(
                icon = Icons.Outlined.Save,
                contentDescription = t(if (isSavingQuestionToServer) "Menyimpan soal" else "Simpan soal"),
                enabled = !isSavingQuestionToServer,
                onClick = { saveActiveQuestionToServer() }
              )
            }
          }
        } else if (activePatronLearningItem != null) {
          {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              AnimatedVisibility(visible = isPatronLearningEditMode && patronLearningUndoStack.isNotEmpty()) {
                MapelTopButton(
                  icon = Icons.AutoMirrored.Outlined.Undo,
                  contentDescription = t("Batalkan perubahan"),
                  onClick = {
                  val restored = patronLearningUndoStack.lastOrNull()
                  val itemId = activePatronLearningItem.id
                  if (restored != null) {
                    patronLearningUndoStack = patronLearningUndoStack.dropLast(1)
                      val nextMaterials = patronLearningMaterials.toMutableMap().apply {
                        put(itemId, restored)
                      }
                      patronLearningMaterials = nextMaterials
                      savePatronLearningMaterials(context, subject.id, nextMaterials)
                  }
                }
              )
              }
              MapelTopButton(
                icon = if (isPatronLearningEditMode) Icons.Outlined.Check else Icons.Outlined.EditNote,
                contentDescription = t(if (isPatronLearningEditMode) "Selesai edit" else "Edit materi"),
                onClick = { isPatronLearningEditMode = !isPatronLearningEditMode }
              )
            }
          }
        } else if (selectedSection == MapelDetailSection.Absensi && !isQuestionEditorOpen) {
          {
            var actionMenuExpanded by rememberSaveable { mutableStateOf(false) }
            Box {
              MapelTopButton(
                icon = Icons.Outlined.MoreVert,
                contentDescription = t("Menu absensi"),
                onClick = { actionMenuExpanded = true }
              )
              DropdownMenu(
                expanded = actionMenuExpanded,
                onDismissRequest = { actionMenuExpanded = false },
                modifier = Modifier.background(CardBackground)
              ) {
                DropdownMenuItem(
                  text = { Text(t("Cetak Absensi")) },
                  enabled = attendanceSnapshot != null && !isLoadingAttendance && !isExportingAttendance,
                  onClick = {
                    actionMenuExpanded = false
                    attendanceExportShouldShare = false
                  }
                )
                DropdownMenuItem(
                  text = { Text(t("Kirim Absensi")) },
                  enabled = attendanceSnapshot != null && !isLoadingAttendance && !isExportingAttendance,
                  onClick = {
                    actionMenuExpanded = false
                    attendanceExportShouldShare = true
                  }
                )
              }
            }
          }
        } else {
          null
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
            .padding(bottom = if (isQuestionEditorOpen) 18.dp else 0.dp)
        ) {
          if (activePatronLearningItem != null) {
            PatronLearningDocumentScreen(
              subject = subject,
              item = activePatronLearningItem,
              material = patronLearningMaterials[activePatronLearningItem.id] ?: PatronLearningMaterial(),
              isEditMode = isPatronLearningEditMode,
              onMaterialChange = { updated ->
                val previous = patronLearningMaterials[activePatronLearningItem.id] ?: PatronLearningMaterial()
                if (previous != updated) {
                  patronLearningUndoStack = (patronLearningUndoStack + previous).takeLast(30)
                }
                val nextMaterials = patronLearningMaterials.toMutableMap().apply {
                  put(activePatronLearningItem.id, updated)
                }
                patronLearningMaterials = nextMaterials
                savePatronLearningMaterials(context, subject.id, nextMaterials)
              },
              modifier = Modifier.weight(1f)
            )
          } else {
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
                onGenerateAiMateri = { openAiDialog("materi") },
                modifier = Modifier.padding(bottom = 14.dp)
              )
            }

            MapelDetailSection.Soal -> {
              if (activeQuestion == null) {
                SoalToolbar(
                  totalSoal = questionItems.size,
                  onAddSoal = { isCreatingQuestion = true },
                  onGenerateAiSoal = { openAiDialog("soal") },
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
            contentPadding = PaddingValues(bottom = if (isQuestionEditorOpen) 24.dp else 124.dp)
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
                      material = patronLearningMaterials[item.id],
                      onTextChange = { text ->
                        patronMateriItems = patronMateriItems.map { current ->
                          if (current.id == item.id) current.copy(text = text) else current
                        }
                      },
                      onOpenLearningMaterial = {
                        activePatronLearningItemId = item.id
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
                          patronLearningMaterials[item.id]?.pdfFileName?.takeIf { it.isNotBlank() }?.let { fileName ->
                            deletePatronPdf(context, subject.id, item.id, fileName)
                          }
                          patronLearningMaterials = patronLearningMaterials - item.id
                          savePatronLearningMaterials(context, subject.id, patronLearningMaterials)
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
                    onEdit = {
                      activeQuestionId = question.id
                      editorQuestionDraft = question
                    },
                    onEditDetail = {
                      editingQuestionDetail = question
                    },
                    onPrint = {
                      if (!isPrintingQuestion) {
                        detailScope.launch {
                          isPrintingQuestion = true
                          runCatching {
                            val latestQuestions = flushLatestQuestionItems()
                            val latestQuestion = latestQuestions.firstOrNull { it.id == question.id } ?: question
                            val exportData = latestQuestion.toExportData()
                            val docx = MapelQuestionExporter.createDocxFile(context, subject, exportData)
                            MapelQuestionExporter.openDocument(context, docx, exportData)
                          }.onFailure {
                            Toast.makeText(
                              context,
                              it.message ?: "Gagal menyiapkan dokumen Word.",
                              Toast.LENGTH_LONG
                            ).show()
                          }
                          isPrintingQuestion = false
                        }
                      }
                    },
                    onShare = {
                      shareQuestion(question)
                    },
                    onDelete = {
                      val nextItems = questionItems.filterNot { it.id == question.id }
                      questionItems = nextItems
                      persistQuestionItems(nextItems)
                    }
                  )
                }
              }
            }
            }
          }
        }

        if (!isQuestionEditorOpen && activePatronLearningItem == null) {
          MapelDetailBottomNav(
            selectedSection = selectedSection,
            onSelectSection = onSectionChange,
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .navigationBarsPadding()
              .padding(horizontal = 18.dp, vertical = 10.dp)
          )
        } else if (isQuestionEditorOpen) {
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
          SoalDetailDialog(
            subject = subject,
            onDismiss = { isCreatingQuestion = false },
            onSave = { created ->
              val nextItems = (questionItems + created)
                .sortedByDescending { it.updatedAt }
              questionItems = nextItems
              persistQuestionItems(nextItems)
              isCreatingQuestion = false
              activeQuestionId = created.id
              editorQuestionDraft = created
            }
          )
        }

        editingQuestionDetail?.let { question ->
          SoalDetailDialog(
            subject = subject,
            question = question,
            onDismiss = { editingQuestionDetail = null },
            onSave = { updated ->
              updateQuestionDraft(updated)
              editingQuestionDetail = null
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

        aiDialogFeature?.let { feature ->
          AiGenerateContentDialog(
            feature = feature,
            subject = subject,
            wallet = aiWallet,
            isLoadingWallet = isLoadingAiWallet,
            prompt = aiPrompt,
            onPromptChange = { aiPrompt = it },
            countText = aiCountText,
            onCountTextChange = { aiCountText = it.filter(Char::isDigit).take(2) },
            questionTypeKey = aiQuestionTypeKey,
            onQuestionTypeChange = { aiQuestionTypeKey = it },
            languageCode = aiLanguageCode,
            onLanguageChange = { aiLanguageCode = it },
            isGenerating = isAiGenerating,
            onRefreshWallet = ::refreshAiWallet,
            onDismiss = {
              if (!isAiGenerating) aiDialogFeature = null
            },
            onGenerate = ::generateAiContentFromDialog
          )
        }

        attendanceExportShouldShare?.let { shouldShare ->
          AttendanceExportPeriodDialog(
            shouldShare = shouldShare,
            currentMonth = attendanceExportBaseMonth,
            selectedMonthKeys = attendanceExportMonthKeys,
            isExporting = isExportingAttendance,
            onSelectedMonthsChange = { attendanceExportMonthKeys = it },
            onDismiss = {
              if (!isExportingAttendance) attendanceExportShouldShare = null
            },
            onConfirm = {
              val snapshot = attendanceSnapshot
              val months = attendanceExportMonthKeys
                .mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }
                .distinct()
                .sorted()
              when {
                snapshot == null -> {
                  Toast.makeText(context, "Data absensi belum tersedia.", Toast.LENGTH_LONG).show()
                }
                months.isEmpty() -> {
                  Toast.makeText(context, "Pilih minimal satu bulan.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                  detailScope.launch {
                    isExportingAttendance = true
                    runCatching {
                      val worksheets = months.map { month ->
                        MapelAttendanceExcelExporter.AttendanceWorksheet(
                          period = month,
                          snapshot = snapshot
                        )
                      }
                      MapelAttendanceExcelExporter.createWorkbook(context, worksheets)
                    }.onSuccess { file ->
                      attendanceExportShouldShare = null
                      if (shouldShare) {
                        MapelAttendanceExcelExporter.shareWorkbook(context, file)
                      } else {
                        MapelAttendanceExcelExporter.openWorkbook(context, file)
                      }
                    }.onFailure { error ->
                      Toast.makeText(
                        context,
                        error.message ?: "Gagal membuat file absensi.",
                        Toast.LENGTH_LONG
                      ).show()
                    }
                    isExportingAttendance = false
                  }
                }
              }
            }
          )
        }
      }
    }
  }
}

@Composable
private fun AiGenerateContentDialog(
  feature: String,
  subject: SubjectOverview,
  wallet: GuruAiTokenWallet?,
  isLoadingWallet: Boolean,
  prompt: String,
  onPromptChange: (String) -> Unit,
  countText: String,
  onCountTextChange: (String) -> Unit,
  questionTypeKey: String,
  onQuestionTypeChange: (String) -> Unit,
  languageCode: String,
  onLanguageChange: (String) -> Unit,
  isGenerating: Boolean,
  onRefreshWallet: () -> Unit,
  onDismiss: () -> Unit,
  onGenerate: () -> Unit
) {
  val isQuestion = feature == "soal"
  val title = if (isQuestion) "Buat Soal dengan AI" else "Buat Materi dengan AI"
  val selectedLanguageCode = normalizeSoalPrintLanguageCode(languageCode).ifBlank { "ID" }
  val selectedLanguage = SoalPrintLanguageOptions.firstOrNull { it.code == selectedLanguageCode }
    ?: SoalPrintLanguageOptions.first()
  val walletLabel = when {
    isLoadingWallet -> "Memuat saldo token..."
    wallet != null -> "Saldo: ${wallet.balanceTokens} token"
    else -> "Saldo token belum tersedia"
  }
  val questionTypeOptions = remember {
    SoalTypeOptions.filter { option ->
      option.key in setOf("pilihan-ganda", "esai", "isi-titik", "benar-salah")
    }
  }
  val selectedType = questionTypeOptions.firstOrNull { it.key == questionTypeKey }
    ?: questionTypeOptions.first()

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
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "${subject.title} - ${subject.className} - ${subject.semester}",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = walletLabel,
            style = MaterialTheme.typography.labelLarge,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.SemiBold
          )
          TextButton(
            enabled = !isLoadingWallet && !isGenerating,
            onClick = onRefreshWallet
          ) {
            Text(t("Refresh"))
          }
        }
        OutlinedTextField(
          value = prompt,
          onValueChange = onPromptChange,
          label = { Text(t("Instruksi AI")) },
          minLines = 4,
          modifier = Modifier.fillMaxWidth()
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          SoalTextField(
            value = countText,
            onValueChange = onCountTextChange,
            label = if (isQuestion) "Jumlah soal" else "Jumlah materi",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(0.9f)
          )
          SoalDropdownField(
            label = "Bahasa",
            selectedLabel = selectedLanguage.label,
            options = SoalPrintLanguageOptions.map { it.label },
            onSelect = { label ->
              SoalPrintLanguageOptions.firstOrNull { it.label == label }?.let { option ->
                onLanguageChange(option.code)
              }
            },
            modifier = Modifier.weight(1.2f)
          )
        }
        if (isQuestion) {
          SoalDropdownField(
            label = "Model soal",
            selectedLabel = selectedType.label,
            options = questionTypeOptions.map { it.label },
            onSelect = { label ->
              questionTypeOptions.firstOrNull { it.label == label }?.let { option ->
                onQuestionTypeChange(option.key)
              }
            }
          )
        }
      }
    },
    confirmButton = {
      Button(
        enabled = !isGenerating && prompt.trim().isNotBlank(),
        onClick = onGenerate
      ) {
        if (isGenerating) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp
          )
        } else {
          Text(t("Buat"))
        }
      }
    },
    dismissButton = {
      TextButton(
        enabled = !isGenerating,
        onClick = onDismiss
      ) {
        Text(t("Batal"))
      }
    }
  )
}

@Composable
private fun AttendanceExportPeriodDialog(
  shouldShare: Boolean,
  currentMonth: YearMonth,
  selectedMonthKeys: List<String>,
  isExporting: Boolean,
  onSelectedMonthsChange: (List<String>) -> Unit,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  val monthOptions = remember(currentMonth) { currentMonth.mapelExportAcademicYearMonths() }
  val selectedSet = selectedMonthKeys.toSet()
  val semesterMonths = remember(currentMonth) { currentMonth.mapelExportSemesterMonths() }
  val title = if (shouldShare) "Kirim Absensi" else "Cetak Absensi"

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
          AttendancePeriodShortcut(
            label = "Bulan Ini",
            selected = selectedSet == setOf(currentMonth.toString()),
            onClick = { onSelectedMonthsChange(listOf(currentMonth.toString())) }
          )
          AttendancePeriodShortcut(
            label = "Semester",
            selected = selectedSet == semesterMonths.map { it.toString() }.toSet(),
            onClick = { onSelectedMonthsChange(semesterMonths.map { it.toString() }) }
          )
          AttendancePeriodShortcut(
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
                  text = month.mapelExportMonthTitle(),
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
private fun AttendancePeriodShortcut(
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
private fun SoalToolbar(
  totalSoal: Int,
  onAddSoal: () -> Unit,
  onGenerateAiSoal: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "$totalSoal ${t("dokumen soal")}",
      style = MaterialTheme.typography.labelLarge,
      color = SubtleInk,
      fontWeight = FontWeight.SemiBold
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SoalPillButton(
        label = "AI",
        icon = Icons.Outlined.Science,
        onClick = onGenerateAiSoal
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
      .background(if (enabled) CardBackground.copy(alpha = 0.92f) else SoftPanel.copy(alpha = 0.58f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(7.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = t(label),
      tint = if (enabled) PrimaryBlue else SubtleInk.copy(alpha = 0.58f),
      modifier = Modifier.size(18.dp)
    )
    Text(
      text = t(label),
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
  onEditDetail: () -> Unit,
  onPrint: () -> Unit,
  onShare: () -> Unit,
  onDelete: () -> Unit
) {
  var menuExpanded by remember { mutableStateOf(false) }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(22.dp))
      .background(CardBackground.copy(alpha = 0.94f))
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
          text = listOf(question.category, question.form, question.dateIso, question.academicYearLabel)
            .filter { it.isNotBlank() }
            .joinToString(" | "),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Box {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(SoftPanel.copy(alpha = 0.92f))
            .clickable { menuExpanded = true },
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Outlined.MoreVert, contentDescription = t("Aksi soal"), tint = PrimaryBlueDark)
        }
        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false }
        ) {
          DropdownMenuItem(
            text = { Text(t("Edit Detail")) },
            leadingIcon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onEditDetail()
            }
          )
          DropdownMenuItem(
            text = { Text(t("Cetak")) },
            leadingIcon = { Icon(Icons.Outlined.Print, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onPrint()
            }
          )
          DropdownMenuItem(
            text = { Text(t("Kirim")) },
            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onShare()
            }
          )
          DropdownMenuItem(
            text = { Text(t("Hapus")) },
            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFDC2626)) },
            onClick = {
              menuExpanded = false
              onDelete()
            }
          )
        }
      }
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      SoalMiniChip("${question.questionCount()} nomor")
      SoalMiniChip("${question.sections.size} model")
      SoalMiniChip(question.statusLabel)
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
private fun SoalDetailDialog(
  subject: SubjectOverview,
  onDismiss: () -> Unit,
  onSave: (MapelQuestionDraft) -> Unit,
  question: MapelQuestionDraft? = null
) {
  var title by remember(question?.id) { mutableStateOf(question?.title.orEmpty()) }
  var selectedCategory by remember(question?.id) {
    mutableStateOf(
      SoalCategoryOptions.firstOrNull { it == question?.category } ?: SoalCategoryOptions.first()
    )
  }
  var dateIso by remember(question?.id) { mutableStateOf(question?.dateIso ?: LocalDate.now().toString()) }
  var academicYearLabel by remember(question?.id) {
    mutableStateOf(question?.academicYearLabel?.ifBlank { defaultAcademicYearLabel() } ?: defaultAcademicYearLabel())
  }
  val canSave = title.trim().isNotBlank()

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(26.dp))
        .background(CardBackground.copy(alpha = 0.98f))
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
            text = t(if (question == null) "Buat Soal" else "Edit Detail Soal"),
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = t("Atur judul, jenis, tanggal, dan tahun ajaran untuk header cetak."),
            style = MaterialTheme.typography.bodySmall,
            color = SubtleInk
          )
        }
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(SoftPanel)
            .clickable(onClick = onDismiss),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Outlined.Close, contentDescription = t("Tutup"), tint = PrimaryBlueDark)
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
      SoalTextField(
        value = academicYearLabel,
        onValueChange = { academicYearLabel = it },
        label = "Tahun ajaran"
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(999.dp))
            .background(SoftPanel)
            .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(999.dp))
            .clickable(onClick = onDismiss)
            .padding(vertical = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(t("Batal"), color = SubtleInk, fontWeight = FontWeight.SemiBold)
        }
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(999.dp))
            .background(if (canSave) PrimaryBlue else PrimaryBlue.copy(alpha = 0.36f))
            .clickable(enabled = canSave) {
              val baseQuestion = question ?: MapelQuestionDraft(
                id = "soal-${System.currentTimeMillis()}",
                title = "Soal ${subject.title}",
                category = selectedCategory,
                form = "",
                dateIso = LocalDate.now().toString(),
                languageCode = "ID"
              )
              onSave(
                baseQuestion.copy(
                  title = title.trim().ifBlank { "Soal ${subject.title}" },
                  category = selectedCategory,
                  dateIso = dateIso.trim().ifBlank { LocalDate.now().toString() },
                  academicYearLabel = academicYearLabel.trim().ifBlank { defaultAcademicYearLabel() },
                  updatedAt = System.currentTimeMillis()
                )
              )
            }
            .padding(vertical = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(t("Simpan"), color = Color.White, fontWeight = FontWeight.Bold)
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
    SoalPrintLanguageCard(
      languageCode = question.resolvedPrintLanguageCode(),
      onLanguageChange = { selectedCode ->
        onQuestionChange(question.copy(languageCode = selectedCode))
      }
    )
    SoalGeneralInstructionCard(
      instruction = question.instruction,
      onInstructionChange = { nextInstruction ->
        onQuestionChange(question.copy(instruction = nextInstruction))
      }
    )
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
  var actionMenuExpanded by rememberSaveable(section.id) { mutableStateOf(false) }
  var showChangeModelDialog by rememberSaveable(section.id) { mutableStateOf(false) }
  val activeQuestionCount = section.effectiveQuestionCount()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = palette.accent.copy(alpha = 0.10f), spotColor = palette.accent.copy(alpha = 0.18f))
      .clip(RoundedCornerShape(24.dp))
      .background(palette.background)
      .border(1.dp, palette.border, RoundedCornerShape(24.dp))
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
      Box {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(CardBackground.copy(alpha = 0.78f))
            .border(1.dp, palette.border.copy(alpha = 0.72f), CircleShape)
            .clickable { actionMenuExpanded = true },
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Outlined.MoreVert, contentDescription = t("Aksi model soal"), tint = palette.text)
        }
        DropdownMenu(
          expanded = actionMenuExpanded,
          onDismissRequest = { actionMenuExpanded = false },
          modifier = Modifier.background(CardBackground)
        ) {
          DropdownMenuItem(
            text = { Text(t("Ubah model soal")) },
            leadingIcon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
            onClick = {
              actionMenuExpanded = false
              showChangeModelDialog = true
            }
          )
          DropdownMenuItem(
            text = { Text(t("Hapus")) },
            leadingIcon = {
              Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFDC2626))
            },
            onClick = {
              actionMenuExpanded = false
              onDelete()
            }
          )
        }
      }
    }

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
    } else if (section.typeKey.usesMatchingPairEditor()) {
      SoalMatchingPairsEditor(
        section = section,
        palette = palette,
        onChange = { nextPairs ->
          onChange(
            section.copy(
              count = if (nextPairs.filledMatchingPairs().isNotEmpty()) 1 else 0,
              matchingPairs = nextPairs
            )
          )
        }
      )
    } else if (section.typeKey.usesWordSearchEditor()) {
      SoalWordSearchEditor(
        section = section,
        palette = palette,
        onChange = { nextQuestions ->
          val primaryQuestion = nextQuestions.firstOrNull() ?: MapelWordSearchQuestion(id = "ws-1")
          onChange(
            section.copy(
              count = if (primaryQuestion.hasContent()) 1 else 0,
              wordSearchQuestions = listOf(primaryQuestion)
            )
          )
        }
      )
    } else if (section.typeKey.usesCrosswordEditor()) {
      SoalCrosswordEditor(
        section = section,
        palette = palette,
        onChange = { nextQuestions ->
          val primaryQuestion = nextQuestions.firstOrNull() ?: MapelCrosswordQuestion(id = "cw-1")
          onChange(
            section.copy(
              count = if (primaryQuestion.hasContent()) 1 else 0,
              crosswordQuestions = listOf(primaryQuestion)
            )
          )
        }
      )
    } else {
      SoalTextField(
        value = section.content,
        onValueChange = { onChange(section.copy(content = it)) },
        label = "Isi soal / catatan editor",
        minLines = 5
      )
    }

    if (showChangeModelDialog) {
      SoalChangeModelDialog(
        selectedTypeKey = section.typeKey,
        onDismiss = { showChangeModelDialog = false },
        onSelect = { option ->
          showChangeModelDialog = false
          onChange(section.withQuestionType(option))
        }
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
        contentDescription = t("Aksi soal"),
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
            text = t("Tambah model soal"),
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
private fun SoalChangeModelDialog(
  selectedTypeKey: String,
  onDismiss: () -> Unit,
  onSelect: (SoalTypeOption) -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(26.dp))
        .background(CardBackground.copy(alpha = 0.98f))
        .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(26.dp))
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = t("Ubah Model Soal"),
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(SoftPanel)
            .clickable(onClick = onDismiss),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Outlined.Close, contentDescription = t("Tutup"), tint = PrimaryBlueDark)
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SoalTypeOptions.forEach { option ->
          val selected = option.key == selectedTypeKey
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(if (selected) PrimaryBlue.copy(alpha = 0.12f) else SoftPanel.copy(alpha = 0.62f))
              .border(
                1.dp,
                if (selected) PrimaryBlue.copy(alpha = 0.34f) else CardBorder.copy(alpha = 0.76f),
                RoundedCornerShape(16.dp)
              )
              .clickable { onSelect(option) }
              .padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = t(option.label),
              style = MaterialTheme.typography.labelLarge,
              color = PrimaryBlueDark,
              fontWeight = FontWeight.SemiBold
            )
            if (selected) {
              Icon(Icons.Outlined.Check, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
            }
          }
        }
      }
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
        .background(CardBackground.copy(alpha = 0.98f))
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
            text = t("Tambah Model Soal"),
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = t("Pilih model, lalu isi soal satu per satu."),
            style = MaterialTheme.typography.bodySmall,
            color = SubtleInk
          )
        }
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(SoftPanel)
            .clickable(onClick = onDismiss),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Outlined.Close, contentDescription = t("Tutup"), tint = PrimaryBlueDark)
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
      text = t("Tambah Model Soal"),
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
      Text(t("Tambah Model"), color = Color.White, fontWeight = FontWeight.Bold)
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

private fun buildQuestionSectionDraft(selectedType: SoalTypeOption): MapelQuestionSection {
  return MapelQuestionSection(
    id = "model-${System.currentTimeMillis()}",
    typeKey = selectedType.key,
    typeLabel = selectedType.label,
    count = if (selectedType.key.usesMatchingPairEditor() || selectedType.key.usesWordSearchEditor() || selectedType.key.usesCrosswordEditor()) 1 else 0,
    choiceQuestions = if (selectedType.key.usesQuestionPromptEditor()) {
      emptyList<MapelChoiceQuestion>().withTrailingBlankQuestion()
    } else {
      emptyList()
    },
    matchingPairs = if (selectedType.key.usesMatchingPairEditor()) {
      emptyList<MapelMatchingPair>().withTrailingBlankMatchingPair()
    } else {
      emptyList()
    },
    wordSearchQuestions = if (selectedType.key.usesWordSearchEditor()) {
      listOf(MapelWordSearchQuestion(id = "ws-1"))
    } else {
      emptyList()
    },
    crosswordQuestions = if (selectedType.key.usesCrosswordEditor()) {
      listOf(MapelCrosswordQuestion(id = "cw-1"))
    } else {
      emptyList()
    }
  )
}

private fun MapelQuestionSection.withQuestionType(option: SoalTypeOption): MapelQuestionSection {
  val nextChoiceQuestions = if (option.key.usesQuestionPromptEditor()) {
    choiceQuestions.withTrailingBlankQuestion()
  } else {
    emptyList()
  }
  return copy(
    typeKey = option.key,
    typeLabel = option.label,
    count = when {
      option.key.usesMatchingPairEditor() -> 1
      option.key.usesWordSearchEditor() -> 0
      option.key.usesCrosswordEditor() -> 0
      option.key.usesQuestionPromptEditor() -> nextChoiceQuestions.filledQuestionCount()
      else -> count
    },
    choiceQuestions = nextChoiceQuestions,
    matchingPairs = if (option.key.usesMatchingPairEditor()) {
      matchingPairs.withTrailingBlankMatchingPair()
    } else {
      emptyList()
    },
    wordSearchQuestions = if (option.key.usesWordSearchEditor()) {
      listOf(wordSearchQuestions.firstOrNull() ?: MapelWordSearchQuestion(id = "ws-1"))
    } else {
      emptyList()
    },
    crosswordQuestions = if (option.key.usesCrosswordEditor()) {
      listOf(crosswordQuestions.firstOrNull() ?: MapelCrosswordQuestion(id = "cw-1"))
    } else {
      emptyList()
    }
  )
}

private data class SoalPrintLanguageOption(
  val code: String,
  val label: String
)

private val SoalPrintLanguageOptions = listOf(
  SoalPrintLanguageOption(
    code = "ID",
    label = "Bahasa Indonesia"
  ),
  SoalPrintLanguageOption(
    code = "AR",
    label = "Bahasa Arab"
  )
)

@Composable
private fun SoalPrintLanguageCard(
  languageCode: String,
  onLanguageChange: (String) -> Unit
) {
  val selectedCode = normalizeSoalPrintLanguageCode(languageCode).ifBlank { "ID" }
  val selectedOption = SoalPrintLanguageOptions.firstOrNull { it.code == selectedCode }
    ?: SoalPrintLanguageOptions.first()
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x120F172A), spotColor = Color(0x120F172A))
      .clip(RoundedCornerShape(18.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
      .padding(10.dp)
  ) {
    SoalDropdownField(
      label = "Bahasa cetak",
      selectedLabel = selectedOption.label,
      options = SoalPrintLanguageOptions.map { it.label },
      onSelect = { label ->
        SoalPrintLanguageOptions.firstOrNull { it.label == label }?.let { option ->
          onLanguageChange(option.code)
        }
      }
    )
  }
}

@Composable
private fun SoalGeneralInstructionCard(
  instruction: String,
  onInstructionChange: (String) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x160F172A), spotColor = Color(0x160F172A))
      .clip(RoundedCornerShape(24.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
      .padding(15.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(PrimaryBlue.copy(alpha = 0.12f))
          .border(1.dp, PrimaryBlue.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Outlined.EditNote, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
      }
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = t("Instruksi Umum"),
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
      }
    }
    SoalTextField(
      value = instruction,
      onValueChange = onInstructionChange,
      label = "Instruksi umum untuk peserta",
      minLines = 3
    )
  }
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
      value = t(selectedLabel),
      onValueChange = {},
      readOnly = true,
      label = { Text(t(label)) },
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
              text = t(option),
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
      .background(CardBackground.copy(alpha = 0.78f))
      .border(1.dp, palette.border.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
      .combinedClickable(
        onClick = { if (showDeleteAction) showDeleteAction = false },
        onLongClick = { if (canDelete) showDeleteAction = true }
      )
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.Top
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
      SoalTextField(
        value = question.prompt,
        onValueChange = { onChange(question.copy(prompt = it)) },
        label = promptLabel,
        modifier = Modifier.weight(1f),
        minLines = 1
      )
      AnimatedVisibility(visible = showDeleteAction) {
        SoalIconAction(Icons.Outlined.Delete, "Hapus soal", onDelete, tint = Color(0xFFDC2626))
      }
    }
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
              label = { Text(t("Pilihan jawaban")) },
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
      .background(if (selected) PrimaryBlue else CardBackground.copy(alpha = 0.86f))
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
      text = t(label),
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
    label = { Text(t(label)) },
    modifier = modifier.fillMaxWidth(),
    minLines = minLines,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
  )
}

@Composable
private fun PatronMateriToolbar(
  totalMateri: Int,
  onAddMateri: () -> Unit,
  onGenerateAiMateri: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "$totalMateri ${t("materi")}",
      style = MaterialTheme.typography.labelLarge,
      color = SubtleInk,
      fontWeight = FontWeight.SemiBold
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      SoalPillButton(
        label = "AI",
        icon = Icons.Outlined.Science,
        onClick = onGenerateAiMateri
      )
      SoalPillButton(
        label = "Tambah Materi",
        icon = Icons.Outlined.Add,
        onClick = onAddMateri
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PatronMateriCard(
  item: PatronMateriItem,
  material: PatronLearningMaterial?,
  onTextChange: (String) -> Unit,
  onOpenLearningMaterial: () -> Unit,
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
        .background(CardBackground.copy(alpha = 0.88f))
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
            } else if (!isEditing && item.text.isNotBlank()) {
              onOpenLearningMaterial()
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
          placeholder = { Text(t("Tulis materi...")) },
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
          PatronLearningSummary(material = material)
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
            contentDescription = t("Simpan materi"),
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
            contentDescription = t("Hapus materi"),
            tint = Color(0xFFDC2626)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PatronLearningSummary(material: PatronLearningMaterial?) {
  val blocks = material?.normalizedBlocks().orEmpty()
  val hasText = blocks.any { it.type == PatronLearningBlockType.Text.key || it.type == PatronLearningBlockType.List.key }
  val hasImage = blocks.any { it.type == PatronLearningBlockType.Image.key }
  val hasPdf = blocks.any { it.type == PatronLearningBlockType.Pdf.key }
  if (!hasText && !hasImage && !hasPdf) {
    Text(
      text = t("Ketuk untuk menyiapkan bahan ajar."),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    return
  }
  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(7.dp),
    verticalArrangement = Arrangement.spacedBy(5.dp)
  ) {
    if (hasText) {
      PatronLearningChip(label = "Ada panduan", tone = SuccessTint)
    }
    if (hasImage) {
      PatronLearningChip(label = "Ada gambar", tone = WarmAccent)
    }
    if (hasPdf) {
      PatronLearningChip(label = "PDF bahan ajar", tone = HighlightCard)
    }
  }
}

@Composable
private fun PatronLearningChip(
  label: String,
  tone: Color
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(tone.copy(alpha = 0.10f))
      .border(1.dp, tone.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
      .padding(horizontal = 9.dp, vertical = 5.dp)
  ) {
    Text(
      text = t(label),
      style = MaterialTheme.typography.labelSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.SemiBold
    )
  }
}

@Composable
private fun PatronLearningDocumentScreen(
  subject: SubjectOverview,
  item: PatronMateriItem,
  material: PatronLearningMaterial,
  isEditMode: Boolean,
  onMaterialChange: (PatronLearningMaterial) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var feedbackMessage by remember(item.id, material) { mutableStateOf<String?>(null) }
  var activeTextTarget by remember(item.id) { mutableStateOf<PatronTextEditTarget?>(null) }
  val blocks = remember(material) { material.normalizedBlocks() }

  fun updateMaterial(nextBlocks: List<PatronLearningBlock>) {
    onMaterialChange(
      material.copy(
        note = "",
        pdfFileName = "",
        pdfOriginalName = "",
        blocks = nextBlocks,
        updatedAt = System.currentTimeMillis()
      )
    )
  }

  fun appendBlock(type: PatronLearningBlockType) {
    updateMaterial(
      blocks + PatronLearningBlock(
        id = "block-${System.currentTimeMillis()}",
        type = type.key
      )
    )
  }

  fun applyActiveTextFormat(format: PatronTextFormat) {
    val target = activeTextTarget ?: return
    updateMaterial(
      blocks.map { block ->
        if (block.id == target.blockId && block.isPatronTextBlock()) {
          block.withAppliedTextFormat(target.selection, format)
        } else {
          block
        }
      }
    )
  }

  val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) {
      runCatching {
        copyPatronPdfToInternalStorage(context, subject.id, item.id, uri)
      }.onSuccess { copied ->
        updateMaterial(
          blocks + PatronLearningBlock(
            id = "block-${System.currentTimeMillis()}",
            type = PatronLearningBlockType.Pdf.key,
            fileName = copied.fileName,
            originalName = copied.originalName
          )
        )
        feedbackMessage = "PDF berhasil ditambahkan."
      }.onFailure { error ->
        feedbackMessage = error.message ?: "Gagal menambahkan PDF."
      }
    }
  }

  val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) {
      runCatching {
        copyPatronImageToInternalStorage(context, subject.id, item.id, uri)
      }.onSuccess { copied ->
        updateMaterial(
          blocks + PatronLearningBlock(
            id = "block-${System.currentTimeMillis()}",
            type = PatronLearningBlockType.Image.key,
            fileName = copied.fileName,
            originalName = copied.originalName
          )
        )
        feedbackMessage = "Gambar berhasil ditambahkan."
      }.onFailure { error ->
        feedbackMessage = error.message ?: "Gagal menambahkan gambar."
      }
    }
  }

  Box(modifier = modifier.fillMaxWidth()) {
    LazyColumn(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(top = 12.dp, bottom = if (isEditMode) 106.dp else 30.dp)
    ) {
      item {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = item.text.ifBlank { "Materi pembelajaran" },
            style = MaterialTheme.typography.headlineSmall,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
          )
          Text(
            text = "${subject.title} | ${subject.className}",
            style = MaterialTheme.typography.bodyMedium,
            color = SubtleInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      if (blocks.isEmpty()) {
        item {
          Text(
            text = if (isEditMode) t("Tekan tombol + untuk mulai menyusun materi.") else t("Belum ada isi materi."),
            style = MaterialTheme.typography.bodyLarge,
            color = SubtleInk,
            modifier = Modifier.padding(top = 22.dp)
          )
        }
      } else {
        items(
          items = blocks,
          key = { it.id }
        ) { block ->
          PatronLearningBlockView(
            subjectId = subject.id,
            itemId = item.id,
            block = block,
            isEditMode = isEditMode,
            onBlockChange = { updated ->
              updateMaterial(blocks.map { if (it.id == updated.id) updated else it })
            },
            onTextSelectionChange = { selection ->
              activeTextTarget = PatronTextEditTarget(block.id, selection)
            },
            onDelete = {
              if (activeTextTarget?.blockId == block.id) {
                activeTextTarget = null
              }
              updateMaterial(blocks.filterNot { it.id == block.id })
            },
            onOpenPdf = { file ->
              if (file.exists()) {
                openPatronPdf(context, file)
              } else {
                feedbackMessage = "File PDF tidak ditemukan di perangkat ini."
              }
            }
          )
        }
      }

      feedbackMessage?.let { message ->
        item {
          AttendanceInfoBox(
            message = message,
            tone = if ("Gagal" in message || "tidak ditemukan" in message) WarmAccent else SuccessTint
          )
        }
      }
    }

    if (isEditMode) {
      PatronLearningEditToolBar(
        canFormatText = activeTextTarget?.blockId?.let { activeId ->
          blocks.any { it.id == activeId && it.isPatronTextBlock() }
        } == true,
        onAddText = { appendBlock(PatronLearningBlockType.Text) },
        onAddImage = { imagePicker.launch(arrayOf("image/*")) },
        onAddPdf = { pdfPicker.launch(arrayOf("application/pdf")) },
        onFormat = ::applyActiveTextFormat,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .imePadding()
          .padding(horizontal = 10.dp, vertical = 10.dp)
      )
    }

  }
}

private data class PatronTextEditTarget(
  val blockId: String,
  val selection: TextRange
)

@Composable
private fun PatronLearningBlockView(
  subjectId: String,
  itemId: String,
  block: PatronLearningBlock,
  isEditMode: Boolean,
  onBlockChange: (PatronLearningBlock) -> Unit,
  onTextSelectionChange: (TextRange) -> Unit,
  onDelete: () -> Unit,
  onOpenPdf: (File) -> Unit
) {
  var showDeleteConfirmation by rememberSaveable(block.id) { mutableStateOf(false) }
  var textFieldValue by remember(block.id) {
    mutableStateOf(TextFieldValue(block.text, selection = TextRange(block.text.length)))
  }
  LaunchedEffect(block.id, block.text) {
    if (block.text != textFieldValue.text) {
      val cursor = textFieldValue.selection.min.coerceIn(0, block.text.length)
      textFieldValue = TextFieldValue(block.text, selection = TextRange(cursor))
      onTextSelectionChange(TextRange(cursor))
    }
  }
  Column(
    modifier = (if (isEditMode) {
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(CardBackground.copy(alpha = 0.88f))
        .border(1.dp, CardBorder.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
        .padding(12.dp)
    } else {
      Modifier.fillMaxWidth()
    }).animateContentSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    if (isEditMode) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFDC2626).copy(alpha = 0.10f))
            .border(1.dp, Color(0xFFDC2626).copy(alpha = 0.24f), RoundedCornerShape(12.dp))
            .clickable { showDeleteConfirmation = true },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = t("Hapus blok"),
            tint = Color(0xFFDC2626),
            modifier = Modifier.size(19.dp)
          )
        }
      }
    }

    when (block.type) {
      PatronLearningBlockType.List.key -> {
        if (isEditMode) {
          OutlinedTextField(
            value = textFieldValue,
            onValueChange = {
              val change = resolvePatronEditorTextChange(block, textFieldValue, it)
              textFieldValue = change.value
              onTextSelectionChange(change.value.selection)
              onBlockChange(block.copy(text = change.value.text, spans = change.spans))
            },
            visualTransformation = PatronRichTextVisualTransformation(block.spans),
            placeholder = { Text(t("Satu poin per baris")) },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
          )
        } else {
          val rows = block.text.lines().map { it.trim() }.filter { it.isNotBlank() }
          if (rows.isEmpty()) {
            Text(t("Daftar kosong"), color = SubtleInk, style = MaterialTheme.typography.bodyMedium)
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
              rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Top) {
                  Text("•", color = PrimaryBlueDark, fontWeight = FontWeight.Bold)
                  Text(
                    text = buildPatronRichText(row, block.spansForDisplayRow(row)),
                    color = PrimaryBlueDark,
                    style = MaterialTheme.typography.bodyLarge
                  )
                }
              }
            }
          }
        }
      }

      PatronLearningBlockType.Image.key -> {
        val file = patronLearningBlockFile(LocalContext.current, subjectId, itemId, block)
        val bitmap = remember(file.absolutePath, file.lastModified()) {
          runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull()
        }
        if (bitmap != null) {
          Image(
            bitmap = bitmap,
            contentDescription = block.originalName.ifBlank { "Gambar bahan ajar" },
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 160.dp, max = 280.dp)
              .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
          )
        } else {
          PatronLearningFileRow(
            icon = Icons.Outlined.Description,
            title = block.originalName.ifBlank { "Gambar tidak ditemukan" },
            subtitle = "File gambar tidak ditemukan di perangkat ini.",
            onClick = {}
          )
        }
      }

      PatronLearningBlockType.Pdf.key -> {
        val file = patronLearningBlockFile(LocalContext.current, subjectId, itemId, block)
        PatronLearningFileRow(
          icon = Icons.Outlined.Description,
          title = block.originalName.ifBlank { "PDF bahan ajar" },
          subtitle = "Ketuk untuk membuka PDF.",
          onClick = { onOpenPdf(file) }
        )
      }

      else -> {
        if (isEditMode) {
          OutlinedTextField(
            value = textFieldValue,
            onValueChange = {
              val change = resolvePatronEditorTextChange(block, textFieldValue, it)
              textFieldValue = change.value
              onTextSelectionChange(change.value.selection)
              onBlockChange(block.copy(text = change.value.text, spans = change.spans))
            },
            visualTransformation = PatronRichTextVisualTransformation(block.spans),
            placeholder = { Text(t("Tulis materi...")) },
            minLines = 5,
            modifier = Modifier.fillMaxWidth()
          )
        } else {
          Text(
            text = if (block.text.isBlank()) AnnotatedString(t("Teks kosong")) else buildPatronRichText(block.text, block.spans),
            style = MaterialTheme.typography.bodyLarge,
            color = if (block.text.isBlank()) SubtleInk else PrimaryBlueDark,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
          )
        }
      }
    }

  }

  if (showDeleteConfirmation) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmation = false },
      containerColor = CardBackground,
      title = {
        Text(
          text = t("Hapus bagian materi?"),
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
      },
      text = {
        Text(
          text = t("Bagian ini akan dihapus dari materi pembelajaran. Tindakan ini bisa dibatalkan dengan tombol undo selama masih di mode edit."),
          color = SubtleInk,
          style = MaterialTheme.typography.bodyMedium
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirmation = false
            onDelete()
          }
        ) {
          Text(t("Hapus"))
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirmation = false }) {
          Text(t("Batal"))
        }
      }
    )
  }
}

@Composable
private fun PatronTextFormatButton(
  label: String,
  fontWeight: FontWeight = FontWeight.Bold,
  fontStyle: FontStyle = FontStyle.Normal,
  textDecoration: TextDecoration? = null,
  enabled: Boolean = true,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(34.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(if (enabled) SoftPanel.copy(alpha = 0.78f) else SoftPanel.copy(alpha = 0.36f))
      .border(1.dp, CardBorder.copy(alpha = if (enabled) 0.82f else 0.36f), RoundedCornerShape(12.dp))
      .clickable(enabled = enabled, onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (enabled) PrimaryBlueDark else SubtleInk.copy(alpha = 0.55f),
      fontWeight = fontWeight,
      fontStyle = fontStyle,
      textDecoration = textDecoration
    )
  }
}

@Composable
private fun PatronLearningEditToolBar(
  canFormatText: Boolean,
  onAddText: () -> Unit,
  onAddImage: () -> Unit,
  onAddPdf: () -> Unit,
  onFormat: (PatronTextFormat) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .shadow(16.dp, RoundedCornerShape(26.dp), ambientColor = PrimaryBlue.copy(alpha = 0.12f), spotColor = PrimaryBlue.copy(alpha = 0.12f))
      .clip(RoundedCornerShape(26.dp))
      .background(CardBackground.copy(alpha = 0.96f))
      .border(1.dp, CardBorder.copy(alpha = 0.86f), RoundedCornerShape(26.dp))
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    PatronLearningToolIcon(
      icon = Icons.Outlined.TextFields,
      contentDescription = t("Tambah teks"),
      onClick = onAddText
    )
    PatronLearningToolIcon(
      icon = Icons.Outlined.Image,
      contentDescription = t("Tambah gambar"),
      onClick = onAddImage
    )
    PatronLearningToolIcon(
      icon = Icons.Outlined.Description,
      contentDescription = t("Tambah PDF"),
      onClick = onAddPdf
    )
    Box(
      modifier = Modifier
        .height(28.dp)
        .width(1.dp)
        .background(CardBorder.copy(alpha = 0.72f))
    )
    PatronTextFormatButton(
      label = "B",
      fontWeight = FontWeight.ExtraBold,
      enabled = canFormatText,
      onClick = { onFormat(PatronTextFormat.Bold) }
    )
    PatronTextFormatButton(
      label = "I",
      fontStyle = FontStyle.Italic,
      enabled = canFormatText,
      onClick = { onFormat(PatronTextFormat.Italic) }
    )
    PatronTextFormatButton(
      label = "U",
      textDecoration = TextDecoration.Underline,
      enabled = canFormatText,
      onClick = { onFormat(PatronTextFormat.Underline) }
    )
    PatronTextFormatButton(
      label = "\u2022",
      enabled = canFormatText,
      onClick = { onFormat(PatronTextFormat.List) }
    )
  }
}

@Composable
private fun PatronLearningToolIcon(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(38.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(HighlightCard.copy(alpha = 0.12f))
      .border(1.dp, HighlightCard.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = HighlightCard,
      modifier = Modifier.size(20.dp)
    )
  }
}

private enum class PatronTextFormat {
  Bold,
  Italic,
  Underline,
  List
}

private fun PatronLearningBlock.withAppliedTextFormat(
  selection: TextRange,
  format: PatronTextFormat
): PatronLearningBlock {
  val rawStart = selection.min.coerceIn(0, text.length)
  val rawEnd = selection.max.coerceIn(0, text.length)
  val (start, end) = if (format == PatronTextFormat.List) {
    selectedPatronLineRange(text, rawStart, rawEnd)
  } else {
    rawStart to rawEnd
  }
  if (start == end && format != PatronTextFormat.List) return this

  val normalized = spans.normalizedPatronTextSpans(text.length)
  val shouldRemove = normalized.isPatronFormatFullyCovered(text, start, end, format)
  val nextSpans = if (shouldRemove) {
    normalized.removePatronFormat(text, start, end, format)
  } else {
    normalized.addPatronFormat(text, start, end, format)
  }
  return copy(spans = nextSpans.normalizedPatronTextSpans(text.length))
}

private fun PatronLearningBlock.isPatronTextBlock(): Boolean =
  type == PatronLearningBlockType.Text.key || type == PatronLearningBlockType.List.key

private fun selectedPatronLineRange(
  text: String,
  selectionStart: Int,
  selectionEnd: Int
): Pair<Int, Int> {
  if (text.isEmpty()) return 0 to 0
  val safeStart = selectionStart.coerceIn(0, text.length)
  val safeEnd = selectionEnd.coerceIn(0, text.length)
  val start = if (safeStart <= 0) {
    0
  } else {
    text.lastIndexOf('\n', safeStart - 1).let { if (it < 0) 0 else it + 1 }
  }
  val endAnchor = if (safeEnd > safeStart) safeEnd - 1 else safeEnd
  val end = if (endAnchor >= text.length) {
    text.length
  } else {
    text.indexOf('\n', endAnchor.coerceAtLeast(0)).let { if (it < 0) text.length else it }
  }
  return start to end
}

private fun PatronLearningBlock.spansForDisplayRow(row: String): List<PatronTextSpan> {
  val rowStart = text.indexOf(row)
  if (rowStart < 0) return emptyList()
  val rowEnd = rowStart + row.length
  return spans.mapNotNull { span ->
    val start = maxOf(span.start, rowStart)
    val end = minOf(span.end, rowEnd)
    if (end <= start) {
      null
    } else {
      span.copy(start = start - rowStart, end = end - rowStart)
    }
  }.normalizedPatronTextSpans(row.length)
}

private fun adjustPatronTextSpans(
  oldText: String,
  newText: String,
  spans: List<PatronTextSpan>
): List<PatronTextSpan> {
  if (oldText == newText || spans.isEmpty()) return spans.normalizedPatronTextSpans(newText.length)

  var prefix = 0
  val maxPrefix = minOf(oldText.length, newText.length)
  while (prefix < maxPrefix && oldText[prefix] == newText[prefix]) prefix += 1

  var suffix = 0
  val oldRemaining = oldText.length - prefix
  val newRemaining = newText.length - prefix
  while (
    suffix < oldRemaining &&
    suffix < newRemaining &&
    oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
  ) {
    suffix += 1
  }

  val oldChangeEnd = oldText.length - suffix
  val newChangeEnd = newText.length - suffix
  val delta = newText.length - oldText.length
  return spans.mapNotNull { span ->
    val shifted = when {
      span.list && span.start == span.end && span.start == prefix && newChangeEnd > prefix ->
        span.copy(end = newChangeEnd)
      span.end <= prefix -> span
      span.start >= oldChangeEnd -> span.copy(start = span.start + delta, end = span.end + delta)
      else -> {
        val nextStart = if (span.start <= prefix) span.start else newChangeEnd
        val nextEnd = if (span.end >= oldChangeEnd) span.end + delta else prefix
        span.copy(start = nextStart, end = nextEnd)
      }
    }
    shifted.takeIf { it.end > it.start || (it.list && it.end == it.start) }
  }.normalizedPatronTextSpans(newText.length)
}

private fun List<PatronTextSpan>.isPatronFormatFullyCovered(
  text: String,
  start: Int,
  end: Int,
  format: PatronTextFormat
): Boolean {
  if (format == PatronTextFormat.List) {
    val lineRanges = patronSelectedLineRanges(text, start, end)
    if (lineRanges.isEmpty()) return false
    return lineRanges.all { (lineStart, lineEnd) ->
      this.any { span ->
        span.list && if (lineStart == lineEnd) {
          span.start == lineStart && span.end == lineEnd
        } else {
          span.start <= lineStart && span.end >= lineEnd
        }
      }
    }
  }
  if (end <= start) return false
  val covered = BooleanArray(end - start)
  filter { it.hasPatronFormat(format) }.forEach { span ->
    val from = maxOf(span.start, start)
    val until = minOf(span.end, end)
    for (index in from until until) {
      covered[index - start] = true
    }
  }
  return covered.all { it }
}

private fun List<PatronTextSpan>.addPatronFormat(
  text: String,
  start: Int,
  end: Int,
  format: PatronTextFormat
): List<PatronTextSpan> {
  if (format == PatronTextFormat.List) {
    val lines = patronSelectedLineRanges(text, start, end)
    val withoutCurrentLineList = lines.fold(this) { current, (lineStart, lineEnd) ->
      current.removePatronFormatRange(lineStart, lineEnd, format)
    }
    return withoutCurrentLineList + lines.map { (lineStart, lineEnd) ->
      PatronTextSpan(start = lineStart, end = lineEnd, list = true)
    }
  }
  if (end <= start) return this
  return this + PatronTextSpan(
    start = start,
    end = end,
    bold = format == PatronTextFormat.Bold,
    italic = format == PatronTextFormat.Italic,
    underline = format == PatronTextFormat.Underline
  )
}

private fun List<PatronTextSpan>.removePatronFormat(
  text: String,
  start: Int,
  end: Int,
  format: PatronTextFormat
): List<PatronTextSpan> {
  if (format == PatronTextFormat.List) {
    return patronSelectedLineRanges(text, start, end).fold(this) { current, (lineStart, lineEnd) ->
      current.removePatronFormatRange(lineStart, lineEnd, format)
    }
  }
  return removePatronFormatRange(start, end, format)
}

private fun List<PatronTextSpan>.removePatronFormatRange(
  start: Int,
  end: Int,
  format: PatronTextFormat
): List<PatronTextSpan> {
  return flatMap { span ->
    if (!span.hasPatronFormat(format)) return@flatMap listOf(span)
    if (format == PatronTextFormat.List && start == end) {
      return@flatMap if (span.start == start && span.end == end) emptyList() else listOf(span)
    }
    if (end <= start || span.end <= start || span.start >= end) return@flatMap listOf(span)

    buildList {
      if (span.start < start) {
        add(span.copy(end = start))
      }
      val middleStart = maxOf(span.start, start)
      val middleEnd = minOf(span.end, end)
      val withoutFormat = span.withPatronFormat(format, enabled = false)
      if (middleEnd > middleStart && withoutFormat.hasAnyPatronFormat()) {
        add(withoutFormat.copy(start = middleStart, end = middleEnd))
      }
      if (span.end > end) {
        add(span.copy(start = end))
      }
    }
  }
}

private fun patronSelectedLineRanges(
  text: String,
  start: Int,
  end: Int
): List<Pair<Int, Int>> {
  val expanded = selectedPatronLineRange(text, start, end)
  val lineRanges = mutableListOf<Pair<Int, Int>>()
  var cursor = expanded.first
  while (cursor <= expanded.second) {
    val nextNewline = text.indexOf('\n', cursor).let { if (it < 0) text.length else it }
    val lineEnd = minOf(nextNewline, expanded.second)
    lineRanges += cursor to lineEnd
    if (nextNewline >= expanded.second || nextNewline >= text.length) break
    cursor = nextNewline + 1
  }
  return lineRanges.ifEmpty { listOf(expanded) }
}

private fun PatronTextSpan.hasPatronFormat(format: PatronTextFormat): Boolean =
  when (format) {
    PatronTextFormat.Bold -> bold
    PatronTextFormat.Italic -> italic
    PatronTextFormat.Underline -> underline
    PatronTextFormat.List -> list
  }

private fun PatronTextSpan.withPatronFormat(
  format: PatronTextFormat,
  enabled: Boolean
): PatronTextSpan =
  when (format) {
    PatronTextFormat.Bold -> copy(bold = enabled)
    PatronTextFormat.Italic -> copy(italic = enabled)
    PatronTextFormat.Underline -> copy(underline = enabled)
    PatronTextFormat.List -> copy(list = enabled)
  }

private fun PatronTextSpan.hasAnyPatronFormat(): Boolean =
  bold || italic || underline || list

private fun List<PatronTextSpan>.normalizedPatronTextSpans(textLength: Int): List<PatronTextSpan> =
  mapNotNull { span ->
    val start = span.start.coerceIn(0, textLength)
    val end = span.end.coerceIn(0, textLength)
    when {
      end < start -> null
      !span.hasAnyPatronFormat() -> null
      end == start && !span.list -> null
      end == start -> span.copy(
        start = start,
        end = end,
        bold = false,
        italic = false,
        underline = false,
        list = true
      )
      else -> span.copy(start = start, end = end)
    }
  }.distinct()

private fun buildPatronRichText(
  text: String,
  spans: List<PatronTextSpan>
): AnnotatedString {
  val render = renderPatronRichText(text, spans)
  return render.text
}

private data class PatronRichTextRender(
  val text: AnnotatedString,
  val originalToTransformed: IntArray,
  val transformedToOriginal: IntArray
)

private fun renderPatronRichText(
  text: String,
  spans: List<PatronTextSpan>
): PatronRichTextRender {
  val normalizedSpans = spans.normalizedPatronTextSpans(text.length)
  val listSpans = normalizedSpans.filter { it.list }
  val builder = AnnotatedString.Builder()
  val originalToTransformed = IntArray(text.length + 1)
  val transformedToOriginal = mutableListOf<Int>()

  fun appendMapped(value: String, originalOffset: Int) {
    value.forEach { char ->
      transformedToOriginal += originalOffset.coerceIn(0, text.length)
      builder.append(char)
    }
  }

  var lineStart = 0
  while (lineStart <= text.length) {
    val newlineIndex = text.indexOf('\n', lineStart).let { if (it < 0) text.length else it }
    val lineEnd = newlineIndex
    val lineText = text.substring(lineStart, lineEnd)
    val hasListMarker = listSpans.any { span ->
      if (lineStart == lineEnd) {
        span.start == lineStart && span.end == lineEnd
      } else {
        lineText.isNotBlank() && span.end > lineStart && span.start < lineEnd
      }
    }
    if (hasListMarker) {
      appendMapped("\u2022 ", lineStart)
    }
    originalToTransformed[lineStart] = builder.length
    for (index in lineStart until lineEnd) {
      originalToTransformed[index] = builder.length
      appendMapped(text[index].toString(), index)
    }
    if (newlineIndex < text.length) {
      originalToTransformed[newlineIndex] = builder.length
      appendMapped("\n", newlineIndex)
      lineStart = newlineIndex + 1
    } else {
      break
    }
  }
  originalToTransformed[text.length] = builder.length
  transformedToOriginal += text.length

  normalizedSpans.filterNot { it.list }.forEach { span ->
    val start = originalToTransformed[span.start.coerceIn(0, text.length)]
    val end = originalToTransformed[span.end.coerceIn(0, text.length)]
    if (end <= start) return@forEach
    builder.addStyle(
      SpanStyle(
        fontWeight = if (span.bold) FontWeight.Bold else null,
        fontStyle = if (span.italic) FontStyle.Italic else null,
        textDecoration = if (span.underline) TextDecoration.Underline else null
      ),
      start,
      end
    )
  }
  return PatronRichTextRender(
    text = builder.toAnnotatedString(),
    originalToTransformed = originalToTransformed,
    transformedToOriginal = transformedToOriginal.toIntArray()
  )
}

private class PatronRichTextVisualTransformation(
  private val spans: List<PatronTextSpan>
) : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    val render = renderPatronRichText(text.text, spans)
    return TransformedText(
      text = render.text,
      offsetMapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int =
          render.originalToTransformed[offset.coerceIn(render.originalToTransformed.indices)]

        override fun transformedToOriginal(offset: Int): Int =
          render.transformedToOriginal[offset.coerceIn(render.transformedToOriginal.indices)]
      }
    )
  }
}

private data class PatronEditorTextChange(
  val value: TextFieldValue,
  val spans: List<PatronTextSpan>
)

private fun resolvePatronEditorTextChange(
  block: PatronLearningBlock,
  previous: TextFieldValue,
  next: TextFieldValue
): PatronEditorTextChange {
  val listBackspace = resolvePatronListBackspace(block, previous, next)
  if (listBackspace != null) return listBackspace
  return PatronEditorTextChange(
    value = next,
    spans = adjustPatronTextSpans(block.text, next.text, block.spans)
  )
}

private fun resolvePatronListBackspace(
  block: PatronLearningBlock,
  previous: TextFieldValue,
  next: TextFieldValue
): PatronEditorTextChange? {
  if (previous.text.length != next.text.length + 1) return null
  val prefix = previous.text.commonPrefixWith(next.text).length
  if (prefix !in previous.text.indices || previous.text[prefix] != '\n') return null

  val lineStart = prefix + 1
  val lineEnd = previous.text.indexOf('\n', lineStart).let { if (it < 0) previous.text.length else it }
  val hadList = block.spans.normalizedPatronTextSpans(previous.text.length).any { span ->
    span.list && if (lineStart == lineEnd) {
      span.start == lineStart && span.end == lineEnd
    } else {
      span.start <= lineStart && span.end >= lineEnd
    }
  }
  if (!hadList) return null

  val nextSpans = block.spans
    .normalizedPatronTextSpans(previous.text.length)
    .removePatronFormatRange(lineStart, lineEnd, PatronTextFormat.List)
  return PatronEditorTextChange(
    value = previous.copy(selection = TextRange(lineStart)),
    spans = nextSpans
  )
}

@Composable
private fun PatronLearningFileRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(SoftPanel.copy(alpha = 0.62f))
      .border(1.dp, CardBorder.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .padding(13.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, tint = HighlightCard)
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = t(subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
  }
}

@Composable
private fun PatronLearningAddFab(
  onAddText: () -> Unit,
  onAddImage: () -> Unit,
  onAddPdf: () -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  Box(modifier = modifier) {
    Box(
      modifier = Modifier
        .size(58.dp)
        .shadow(14.dp, CircleShape, ambientColor = HighlightCard.copy(alpha = 0.18f), spotColor = HighlightCard.copy(alpha = 0.18f))
        .clip(CircleShape)
        .background(HighlightCard)
        .clickable { expanded = true },
      contentAlignment = Alignment.Center
    ) {
      Icon(Icons.Outlined.Add, contentDescription = t("Tambah blok"), tint = Color.White)
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.background(CardBackground)
    ) {
      DropdownMenuItem(
        text = { Text(t("Teks")) },
        onClick = {
          expanded = false
          onAddText()
        }
      )
      DropdownMenuItem(
        text = { Text(t("Gambar")) },
        onClick = {
          expanded = false
          onAddImage()
        }
      )
      DropdownMenuItem(
        text = { Text(t("PDF")) },
        onClick = {
          expanded = false
          onAddPdf()
        }
      )
    }
  }
}

@Composable
private fun PatronLearningMaterialScreen(
  subject: SubjectOverview,
  item: PatronMateriItem,
  material: PatronLearningMaterial,
  onMaterialChange: (PatronLearningMaterial) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var noteDraft by remember(item.id, material.note) { mutableStateOf(material.note) }
  var feedbackMessage by remember(item.id, material) { mutableStateOf<String?>(null) }
  val hasNoteChange = noteDraft != material.note
  val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) {
      runCatching {
        copyPatronPdfToInternalStorage(
          context = context,
          subjectId = subject.id,
          itemId = item.id,
          uri = uri
        )
      }.onSuccess { copied ->
        onMaterialChange(
          material.copy(
            pdfFileName = copied.fileName,
            pdfOriginalName = copied.originalName,
            updatedAt = System.currentTimeMillis()
          )
        )
        feedbackMessage = "PDF bahan ajar berhasil dilampirkan."
      }.onFailure { error ->
        feedbackMessage = error.message ?: "Gagal melampirkan PDF."
      }
    }
  }

  LazyColumn(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(18.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 30.dp)
  ) {
    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = item.text.ifBlank { "Materi pembelajaran" },
          style = MaterialTheme.typography.headlineSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = "${subject.title} • ${subject.className}",
          style = MaterialTheme.typography.bodyMedium,
          color = SubtleInk
        )
      }
    }

    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
              text = t("Panduan Mengajar"),
              style = MaterialTheme.typography.titleMedium,
              color = PrimaryBlueDark,
              fontWeight = FontWeight.ExtraBold
            )
            Text(
              text = t("Tulis catatan, alur, atau poin penting untuk mengajar materi ini."),
              style = MaterialTheme.typography.bodySmall,
              color = SubtleInk
            )
          }
        }

        OutlinedTextField(
          value = noteDraft,
          onValueChange = { noteDraft = it },
          placeholder = { Text(t("Contoh: awali dengan murojaah, jelaskan contoh, lalu beri latihan.")) },
          minLines = 8,
          maxLines = 14,
          modifier = Modifier.fillMaxWidth()
        )

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (hasNoteChange) SuccessTint.copy(alpha = 0.15f) else SoftPanel.copy(alpha = 0.70f))
            .border(
              1.dp,
              if (hasNoteChange) SuccessTint.copy(alpha = 0.34f) else CardBorder.copy(alpha = 0.88f),
              RoundedCornerShape(18.dp)
            )
            .clickable(enabled = hasNoteChange) {
              onMaterialChange(material.copy(note = noteDraft, updatedAt = System.currentTimeMillis()))
              feedbackMessage = "Panduan mengajar disimpan di perangkat."
            }
            .padding(vertical = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Check,
              contentDescription = null,
              tint = if (hasNoteChange) SuccessTint else SubtleInk,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = t(if (hasNoteChange) "Simpan Materi" else "Materi tersimpan"),
              style = MaterialTheme.typography.labelLarge,
              color = if (hasNoteChange) SuccessTint else SubtleInk,
              fontWeight = FontWeight.ExtraBold
            )
          }
        }
      }
    }

    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = t("PDF Bahan Ajar"),
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = t("PDF disimpan di perangkat ini agar bisa dipakai offline dan tidak membebani database."),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )

        if (material.pdfFileName.isBlank()) {
          PatronLearningActionRow(
            icon = Icons.Outlined.UploadFile,
            title = "Lampirkan PDF",
            subtitle = "Pilih file bahan ajar dari perangkat.",
            onClick = { pdfPicker.launch(arrayOf("application/pdf")) }
          )
        } else {
          PatronLearningActionRow(
            icon = Icons.Outlined.Description,
            title = material.pdfOriginalName.ifBlank { "Bahan ajar PDF" },
            subtitle = "Ketuk untuk membuka file PDF.",
            onClick = {
              val file = patronPdfFile(context, subject.id, item.id, material.pdfFileName)
              if (file.exists()) {
                openPatronPdf(context, file)
              } else {
                feedbackMessage = "File PDF tidak ditemukan di perangkat ini."
              }
            }
          )
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PatronLearningSmallButton(
              label = "Ganti PDF",
              tone = HighlightCard,
              onClick = { pdfPicker.launch(arrayOf("application/pdf")) }
            )
            PatronLearningSmallButton(
              label = "Hapus PDF",
              tone = Color(0xFFDC2626),
              onClick = {
                deletePatronPdf(context, subject.id, item.id, material.pdfFileName)
                onMaterialChange(material.copy(pdfFileName = "", pdfOriginalName = "", updatedAt = System.currentTimeMillis()))
                feedbackMessage = "Lampiran PDF dihapus."
              }
            )
          }
        }

        feedbackMessage?.let { message ->
          AttendanceInfoBox(
            message = message,
            tone = if ("Gagal" in message || "tidak ditemukan" in message) WarmAccent else SuccessTint
          )
        }
      }
    }
  }
}

@Composable
private fun PatronLearningActionRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(SoftPanel.copy(alpha = 0.72f))
      .border(1.dp, CardBorder.copy(alpha = 0.90f), RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .padding(14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(RoundedCornerShape(15.dp))
        .background(HighlightCard.copy(alpha = 0.13f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = null, tint = HighlightCard)
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(
        text = t(title),
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = t(subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
  }
}

@Composable
private fun PatronLearningSmallButton(
  label: String,
  tone: Color,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(tone.copy(alpha = 0.12f))
      .border(1.dp, tone.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 13.dp, vertical = 9.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = t(label),
      style = MaterialTheme.typography.labelMedium,
      color = tone,
      fontWeight = FontWeight.Bold
    )
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
      .background(CardBackground.copy(alpha = 0.88f))
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
          text = t(mode.label),
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
      .background(CardBackground.copy(alpha = 0.88f))
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
          text = t(mode.label),
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
      .background(CardBackground.copy(alpha = 0.96f), RoundedCornerShape(22.dp))
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
      .background(CardBackground.copy(alpha = 0.94f))
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
            text = t("Belum ada data kehadiran untuk mapel ini."),
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
      .background(SoftPanel)
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
          .background(CardBackground.copy(alpha = 0.86f))
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
                    if (isSelected) optionPalette.background else CardBackground.copy(alpha = 0.9f)
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
        contentDescription = t("Hapus absensi"),
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
        contentDescription = t(contentDescription),
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
        .background(CardBackground.copy(alpha = 0.88f))
        .border(1.dp, CardBorder.copy(alpha = 0.94f), RoundedCornerShape(999.dp))
        .clickable(enabled = !isSaving) { onCancel() }
        .padding(vertical = 9.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = t("Batal"),
        style = MaterialTheme.typography.labelMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold
      )
    }
    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(999.dp))
        .background(if (hasChange) SuccessTint.copy(alpha = 0.18f) else SoftPanel.copy(alpha = 0.66f))
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
            text = if (hasChange) t("Simpan") else t("Belum berubah"),
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
        .background(CardBackground.copy(alpha = 0.94f))
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
                contentDescription = t("Hapus absensi tanggal ini"),
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
        .background(CardBackground.copy(alpha = 0.86f))
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
                .background(if (isSelected) optionPalette.background else CardBackground.copy(alpha = 0.9f))
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
      .background(CardBackground.copy(alpha = 0.94f))
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
        text = "${metrics.count { it.value != null }} ${t("jenis nilai tersedia")}",
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
      .background(CardBackground.copy(alpha = 0.94f))
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
        text = "${t("Rata-rata")} ${overview.averageLabel} • ${overview.entries.size} ${t("santri")}",
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
        .background(CardBackground.copy(alpha = 0.94f))
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
            text = "${t(overview.metric.label)} - ${t("rata-rata")} ${overview.averageLabel} - ${overview.entries.size} ${t("nilai")}",
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
                contentDescription = t("Hapus nilai tanggal ini"),
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
      .background(SoftPanel)
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
      .background(SoftPanel)
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
      .background(SoftPanel)
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
        .background(CardBackground.copy(alpha = 0.98f))
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
            text = "${t("Rincian")} ${t(metric.label)}",
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
            text = "${t("Rata-rata")}: ${average?.let(::formatScoreNumber) ?: "-"}",
            style = MaterialTheme.typography.labelLarge,
            color = PrimaryBlue,
            fontWeight = FontWeight.Bold
          )
        }
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(SoftPanel)
            .border(1.dp, CardBorder.copy(alpha = 0.86f), CircleShape)
            .clickable(enabled = !isSaving, onClick = onDismiss),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = t("Tutup"),
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
            .background(CardBackground.copy(alpha = 0.9f))
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
            text = t("Tambah Baris"),
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
              text = t("Simpan"),
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
      .background(SoftPanel)
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
        label = { Text(t("Tanggal")) },
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
        label = { Text(if (maxValue != null) "${t("Nilai")} /$maxValue" else t("Nilai")) },
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
          contentDescription = t("Hapus rincian"),
          tint = Color(0xFFB91C1C),
          modifier = Modifier.size(18.dp)
        )
      }
    }
    OutlinedTextField(
      value = row.material,
      onValueChange = { onRowChange(row.copy(material = it)) },
      singleLine = true,
      label = { Text(t("Materi / keterangan")) },
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
        .background(CardBackground.copy(alpha = 0.88f))
        .border(1.dp, CardBorder.copy(alpha = 0.94f), RoundedCornerShape(999.dp))
        .clickable(enabled = !isSaving, onClick = onCancel)
        .padding(vertical = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = t("Batal"),
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
            text = t("Simpan perubahan"),
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
        .background(CardBackground.copy(alpha = 0.96f))
        .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(32.dp))
        .padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      MapelDetailSection.entries.forEach { section ->
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
        .background(CardBackground.copy(alpha = 0.88f))
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

private const val QuestionStatusDraft = "Draft"
private const val QuestionStatusSaved = "Tersimpan"

private data class MapelQuestionDraft(
  val id: String,
  val title: String = "",
  val category: String = "Ujian",
  val form: String = "Pilihan Ganda",
  val dateIso: String = LocalDate.now().toString(),
  val academicYearLabel: String = defaultAcademicYearLabel(),
  val instruction: String = "",
  val questionsText: String = "",
  val sections: List<MapelQuestionSection> = emptyList(),
  val languageCode: String = "ID",
  val statusLabel: String = QuestionStatusDraft,
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
    val payloadJson = buildDocxQuestionsPayloadJson()
    return MapelQuestionExportData(
      id = id,
      title = title,
      category = category,
      form = form,
      dateLabel = dateIso,
      academicYearLabel = academicYearLabel.ifBlank { defaultAcademicYearLabel() },
      instruction = instruction.trim(),
      questionsText = exportQuestionText(),
      questionsPayloadJson = payloadJson,
      languageCode = resolvedPrintLanguageCode()
    )
  }

  fun exportInstructionText(): String {
    if (instruction.isNotBlank()) return instruction
    return sections
      .filter { it.instruction.isNotBlank() }
      .joinToString("\n") { "${it.typeLabel}: ${it.instruction}" }
  }

  fun exportQuestionText(): String {
    if (sections.isEmpty()) return questionsText
    return sections.mapIndexed { index, section ->
      buildString {
        val sectionCount = section.effectiveQuestionCount()
        appendLine("MODEL ${modelOrdinalLabel(index)}: ${section.typeLabel} (${sectionCount} nomor)")
        if (section.instruction.isNotBlank()) appendLine("Instruksi: ${section.instruction}")
        appendLine()
        if (section.typeKey == "teka-silang") {
          val question = section.crosswordQuestions.firstOrNull() ?: MapelCrosswordQuestion(id = "cw-1")
          val model = buildCrosswordModel(question)
          appendLine("Mendatar")
          if (model.entriesAcross.isEmpty()) {
            appendLine("-")
          } else {
            model.entriesAcross.forEach { entry ->
              appendLine("${entry.number}. ${entry.clue.ifBlank { "........................................................" }} (${entry.length})")
            }
          }
          appendLine()
          appendLine("Menurun")
          if (model.entriesDown.isEmpty()) {
            appendLine("-")
          } else {
            model.entriesDown.forEach { entry ->
              appendLine("${entry.number}. ${entry.clue.ifBlank { "........................................................" }} (${entry.length})")
            }
          }
        } else if (section.typeKey == "cari-kata") {
          val questions = section.wordSearchQuestions.filledWordSearchQuestions()
          if (questions.isEmpty()) {
            appendLine("Kata: ........................................................")
          } else {
            questions.forEachIndexed { questionIndex, question ->
              appendLine("Kata: ${question.words().joinToString(", ").ifBlank { "........................................................" }}")
              if (questionIndex < questions.lastIndex) appendLine()
            }
          }
        } else if (section.typeKey == "pasangkan-kata") {
          val pairs = section.matchingPairs.filledMatchingPairs()
          if (pairs.isEmpty()) {
            appendLine("Qoimah A")
            appendLine("1. ........................................................")
            appendLine()
            appendLine("Qoimah B")
            appendLine("A. ........................................................")
          } else {
            appendLine("Qoimah A")
            pairs.forEachIndexed { pairIndex, pair ->
              appendLine("${pairIndex + 1}. ${pair.leftText.ifBlank { "........................................................" }}")
            }
            appendLine()
            appendLine("Qoimah B")
            pairs.forEachIndexed { pairIndex, pair ->
              appendLine("${('A'.code + pairIndex).toChar()}. ${pair.rightText.ifBlank { "........................................................" }}")
            }
          }
        } else if (section.typeKey == "pilihan-ganda") {
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

private fun MapelQuestionDraft.buildDocxQuestionsPayloadJson(): String {
  val questionRows = JSONArray()
  if (sections.isNotEmpty()) {
    sections.forEachIndexed { sectionIndex, section ->
      val sectionKey = section.id.ifBlank { "section-${sectionIndex + 1}" }
      val instruction = section.instruction.trim()
      if (section.typeKey.usesCrosswordEditor()) {
        val question = section.crosswordQuestions.firstOrNull() ?: MapelCrosswordQuestion(id = "cw-1")
        val model = buildCrosswordModel(question)
        questionRows.put(
          JSONObject().apply {
            put("no", 1)
            put("type", section.typeKey)
            put("sectionKey", sectionKey)
            put("sectionInstruction", instruction)
            put("text", "")
            put("rows", model.rows)
            put("cols", model.cols)
            put("mask", JSONArray().apply { model.maskRows.forEach { put(it) } })
            put(
              "entriesAcross",
              JSONArray().apply {
                model.entriesAcross.forEach { entry ->
                  put(
                    JSONObject().apply {
                      put("direction", "across")
                      put("row", entry.row)
                      put("col", entry.col)
                      put("length", entry.length)
                      put("clue", entry.clue)
                      put("answer", entry.answer)
                      put("number", entry.number)
                    }
                  )
                }
              }
            )
            put(
              "entriesDown",
              JSONArray().apply {
                model.entriesDown.forEach { entry ->
                  put(
                    JSONObject().apply {
                      put("direction", "down")
                      put("row", entry.row)
                      put("col", entry.col)
                      put("length", entry.length)
                      put("clue", entry.clue)
                      put("answer", entry.answer)
                      put("number", entry.number)
                    }
                  )
                }
              }
            )
          }
        )
      } else if (section.typeKey.usesWordSearchEditor()) {
        section.wordSearchQuestions
          .filledWordSearchQuestions()
          .forEachIndexed { questionIndex, question ->
            val puzzle = buildWordSearchPuzzle(question)
            questionRows.put(
              JSONObject().apply {
                put("no", questionIndex + 1)
                put("type", section.typeKey)
                put("sectionKey", sectionKey)
                put("sectionInstruction", instruction)
                put("text", "")
                put("rows", puzzle.rows)
                put("cols", puzzle.cols)
                put("words", JSONArray().apply { puzzle.words.forEach { put(it) } })
                put(
                  "grid",
                  JSONArray().apply {
                    puzzle.grid.forEach { row ->
                      put(JSONArray().apply { row.forEach { put(it.toString()) } })
                    }
                  }
                )
                put(
                  "placements",
                  JSONArray().apply {
                    puzzle.placements.forEach { placement ->
                      put(
                        JSONObject().apply {
                          put("word", placement.word)
                          put("row", placement.row)
                          put("col", placement.col)
                          put("direction", placement.direction)
                        }
                      )
                    }
                  }
                )
              }
            )
          }
      } else if (section.typeKey.usesMatchingPairEditor()) {
        val pairs = section.matchingPairs.filledMatchingPairs()
        questionRows.put(
          JSONObject().apply {
            put("no", 1)
            put("type", section.typeKey)
            put("sectionKey", sectionKey)
            put("sectionInstruction", instruction)
            put("text", "")
            put(
              "columnA",
              JSONArray().apply {
                pairs.forEach { pair ->
                  if (pair.leftText.isNotBlank()) put(pair.leftText)
                }
              }
            )
            put(
              "columnB",
              JSONArray().apply {
                pairs.forEach { pair ->
                  if (pair.rightText.isNotBlank()) put(pair.rightText)
                }
              }
            )
          }
        )
      } else if (section.typeKey.usesQuestionPromptEditor()) {
        section.choiceQuestions
          .filledQuestions()
          .forEachIndexed { questionIndex, question ->
            val row = JSONObject().apply {
              put("no", questionIndex + 1)
              put("type", section.typeKey)
              put("sectionKey", sectionKey)
              put("sectionInstruction", instruction)
              put("text", question.prompt.ifBlank { "........................................................" })
            }
            if (section.typeKey == "pilihan-ganda") {
              val options = JSONObject()
              question.options
                .normalizedChoiceOptions()
                .filter { it.isNotBlank() }
                .forEachIndexed { optionIndex, optionText ->
                  val optionKey = ('a'.code + optionIndex).toChar().toString()
                  options.put(optionKey, JSONObject().apply { put("text", optionText) })
                }
              row.put("options", options)
            }
            questionRows.put(row)
          }
      } else {
        val contentLines = section.content
          .lines()
          .map { it.trim() }
          .filter { it.isNotBlank() }
        if (contentLines.isEmpty()) {
          questionRows.put(
            JSONObject().apply {
              put("no", 1)
              put("type", section.typeKey)
              put("sectionKey", sectionKey)
              put("sectionInstruction", instruction)
              put("text", "........................................................")
            }
          )
        } else {
          contentLines.forEachIndexed { lineIndex, line ->
            questionRows.put(
              JSONObject().apply {
                put("no", lineIndex + 1)
                put("type", section.typeKey)
                put("sectionKey", sectionKey)
                put("sectionInstruction", instruction)
                put("text", line)
              }
            )
          }
        }
      }
    }
  }
  if (questionRows.length() == 0) {
    exportQuestionText()
      .lines()
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .forEachIndexed { index, line ->
        questionRows.put(
          JSONObject().apply {
            put("no", index + 1)
            put("type", "esai")
            put("sectionKey", "fallback-esai")
            put("sectionInstruction", instruction.trim())
            put("text", line)
          }
        )
      }
  }
  return JSONObject().apply {
    put("questions", questionRows)
  }.toString()
}

private fun MapelQuestionDraft.detectQuestionLanguage(): String {
  val combinedText = buildString {
    append(title)
    append('\n')
    append(category)
    append('\n')
    append(exportInstructionText())
    append('\n')
    append(exportQuestionText())
  }
  return if (Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u0870-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]").containsMatchIn(combinedText)) {
    "AR"
  } else {
    "ID"
  }
}

private fun normalizeSoalPrintLanguageCode(value: String): String {
  val normalized = value.trim().uppercase(Locale.ROOT)
  return when {
    normalized == "AR" || normalized == "ARAB" || normalized == "ARABIC" || normalized.contains("ARAB") -> "AR"
    normalized == "ID" || normalized == "INDONESIA" || normalized == "INDONESIAN" || normalized.contains("INDONES") -> "ID"
    else -> ""
  }
}

private fun normalizeQuestionStatusLabel(value: String): String {
  val normalized = value.trim().lowercase(Locale.ROOT)
  return when (normalized) {
    "tersimpan", "saved", "sinkron", "synced" -> QuestionStatusSaved
    "draft", "draf", "" -> QuestionStatusDraft
    else -> value.trim().ifBlank { QuestionStatusDraft }
  }
}

private fun MapelQuestionDraft.resolvedPrintLanguageCode(): String {
  return normalizeSoalPrintLanguageCode(languageCode).ifBlank { detectQuestionLanguage() }
}

@Composable
private fun SoalMatchingPairsEditor(
  section: MapelQuestionSection,
  palette: SoalModelPalette,
  onChange: (List<MapelMatchingPair>) -> Unit
) {
  val visiblePairs = section.matchingPairs.withTrailingBlankMatchingPair()
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = t("Pasangan A - B"),
        style = MaterialTheme.typography.labelLarge,
        color = palette.text,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = t("Baris kosong baru muncul otomatis"),
        style = MaterialTheme.typography.labelSmall,
        color = palette.accent
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = t("Qoimah A"),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.labelMedium,
        color = palette.text,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = t("Qoimah B"),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.labelMedium,
        color = palette.text,
        fontWeight = FontWeight.Bold
      )
    }
    visiblePairs.forEachIndexed { pairIndex, pair ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
      ) {
        Box(
          modifier = Modifier
            .padding(top = 10.dp)
            .size(28.dp)
            .clip(CircleShape)
            .background(palette.accent.copy(alpha = 0.10f))
            .border(1.dp, palette.accent.copy(alpha = 0.20f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "${pairIndex + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = palette.text,
            fontWeight = FontWeight.ExtraBold
          )
        }
        OutlinedTextField(
          value = pair.leftText,
          onValueChange = { value ->
            onChange(
              visiblePairs.toMutableList()
                .also { it[pairIndex] = pair.copy(leftText = value) }
                .withTrailingBlankMatchingPair()
            )
          },
          label = { Text(t("Pilihan A")) },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(16.dp)
        )
        OutlinedTextField(
          value = pair.rightText,
          onValueChange = { value ->
            onChange(
              visiblePairs.toMutableList()
                .also { it[pairIndex] = pair.copy(rightText = value) }
                .withTrailingBlankMatchingPair()
            )
          },
          label = { Text(t("Pilihan B")) },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(16.dp)
        )
      }
    }
  }
}

@Composable
private fun SoalWordSearchEditor(
  section: MapelQuestionSection,
  palette: SoalModelPalette,
  onChange: (List<MapelWordSearchQuestion>) -> Unit
) {
  val question = section.wordSearchQuestions.firstOrNull() ?: MapelWordSearchQuestion(id = "ws-1")
  var rowsInput by rememberSaveable(question.id) { mutableStateOf(question.rows.toString()) }
  var colsInput by rememberSaveable(question.id) { mutableStateOf(question.cols.toString()) }
  val puzzle = remember(question.prompt, question.rows, question.cols, question.wordsText) {
    buildWordSearchPuzzle(question)
  }
  LaunchedEffect(question.rows) {
    val desired = question.rows.toString()
    if (rowsInput != desired) rowsInput = desired
  }
  LaunchedEffect(question.cols) {
    val desired = question.cols.toString()
    if (colsInput != desired) colsInput = desired
  }
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(CardBackground.copy(alpha = 0.78f))
        .border(1.dp, palette.border.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
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
          Icon(
            imageVector = Icons.Outlined.Quiz,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
          Text(
            text = t("Puzzle Cari Kata"),
            style = MaterialTheme.typography.labelLarge,
            color = palette.text,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = if (question.hasContent()) "${question.rows} x ${question.cols}" else "Isi daftar kata",
            style = MaterialTheme.typography.labelSmall,
            color = palette.text.copy(alpha = 0.68f),
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = rowsInput,
          onValueChange = { value ->
            val filtered = value.filter { it.isDigit() }.take(2)
            rowsInput = filtered
            val parsed = filtered.toIntOrNull()
            if (parsed != null && parsed in 5..20) {
              onChange(listOf(question.copy(rows = parsed)))
            }
          },
          label = { Text(t("Baris")) },
          modifier = Modifier.weight(1f),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(16.dp)
        )
        OutlinedTextField(
          value = colsInput,
          onValueChange = { value ->
            val filtered = value.filter { it.isDigit() }.take(2)
            colsInput = filtered
            val parsed = filtered.toIntOrNull()
            if (parsed != null && parsed in 5..20) {
              onChange(listOf(question.copy(cols = parsed)))
            }
          },
          label = { Text(t("Kolom")) },
          modifier = Modifier.weight(1f),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(16.dp)
        )
      }

      SoalTextField(
        value = question.wordsText,
        onValueChange = { value ->
          onChange(listOf(question.copy(wordsText = value)))
        },
        label = "Daftar kata, satu kata per baris",
        minLines = 5
      )

      if (puzzle.words.isEmpty()) {
        Text(
          text = t("Isi daftar kata untuk melihat preview puzzle."),
          style = MaterialTheme.typography.bodySmall,
          color = palette.text.copy(alpha = 0.72f)
        )
      } else {
        WordSearchPreviewCard(puzzle = puzzle, palette = palette)
      }
    }
  }
}

@Composable
private fun SoalCrosswordEditor(
  section: MapelQuestionSection,
  palette: SoalModelPalette,
  onChange: (List<MapelCrosswordQuestion>) -> Unit
) {
  val question = section.crosswordQuestions.firstOrNull() ?: MapelCrosswordQuestion(id = "cw-1")
  val model = remember(question.rows, question.cols, question.entriesAcross, question.entriesDown) {
    buildCrosswordModel(question)
  }
  var rowsInput by rememberSaveable(question.id) { mutableStateOf(question.rows.toString()) }
  var colsInput by rememberSaveable(question.id) { mutableStateOf(question.cols.toString()) }
  var anchorRow by rememberSaveable(question.id) { mutableStateOf(-1) }
  var anchorCol by rememberSaveable(question.id) { mutableStateOf(-1) }
  var pendingRow by rememberSaveable(question.id) { mutableStateOf(-1) }
  var pendingCol by rememberSaveable(question.id) { mutableStateOf(-1) }

  fun resetDraftSelection() {
    anchorRow = -1
    anchorCol = -1
    pendingRow = -1
    pendingCol = -1
  }

  fun buildDraftEntry(): MapelCrosswordEntry? {
    if (anchorRow < 0 || anchorCol < 0 || pendingRow < 0 || pendingCol < 0) return null
    return when {
      anchorRow == pendingRow -> {
        val startCol = minOf(anchorCol, pendingCol)
        val endCol = maxOf(anchorCol, pendingCol)
        MapelCrosswordEntry(
          id = "across-${System.currentTimeMillis()}",
          direction = "across",
          row = anchorRow + 1,
          col = startCol + 1,
          length = (endCol - startCol) + 1
        )
      }

      anchorCol == pendingCol -> {
        val startRow = minOf(anchorRow, pendingRow)
        val endRow = maxOf(anchorRow, pendingRow)
        MapelCrosswordEntry(
          id = "down-${System.currentTimeMillis()}",
          direction = "down",
          row = startRow + 1,
          col = anchorCol + 1,
          length = (endRow - startRow) + 1
        )
      }

      else -> null
    }?.takeIf { it.length >= 2 }
  }

  LaunchedEffect(question.rows) {
    val desired = question.rows.toString()
    if (rowsInput != desired) rowsInput = desired
  }
  LaunchedEffect(question.cols) {
    val desired = question.cols.toString()
    if (colsInput != desired) colsInput = desired
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(CardBackground.copy(alpha = 0.78f))
      .border(1.dp, palette.border.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
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
        Icon(Icons.Outlined.Quiz, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
      }
      Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
          text = t("Puzzle Teka-Teki Silang"),
          style = MaterialTheme.typography.labelLarge,
          color = palette.text,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = if (question.hasContent()) "${question.rows} x ${question.cols}" else "Isi slot mendatar dan menurun",
          style = MaterialTheme.typography.labelSmall,
          color = palette.text.copy(alpha = 0.68f),
          fontWeight = FontWeight.SemiBold
        )
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedTextField(
        value = rowsInput,
        onValueChange = { value ->
          val filtered = value.filter { it.isDigit() }.take(2)
          rowsInput = filtered
          val parsed = filtered.toIntOrNull()
          if (parsed != null && parsed in 5..20) {
            onChange(listOf(question.copy(rows = parsed)))
          }
        },
        label = { Text(t("Baris")) },
        modifier = Modifier.weight(1f),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(16.dp)
      )
      OutlinedTextField(
        value = colsInput,
        onValueChange = { value ->
          val filtered = value.filter { it.isDigit() }.take(2)
          colsInput = filtered
          val parsed = filtered.toIntOrNull()
          if (parsed != null && parsed in 5..20) {
            onChange(listOf(question.copy(cols = parsed)))
          }
        },
        label = { Text(t("Kolom")) },
        modifier = Modifier.weight(1f),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(16.dp)
      )
    }

    Text(
      text = if (pendingRow >= 0 && pendingCol >= 0) {
        "Draft slot siap dikonfirmasi"
      } else if (anchorRow >= 0 && anchorCol >= 0) {
        "Titik awal: baris ${anchorRow + 1}, kolom ${anchorCol + 1}"
      } else {
        "Pilih titik awal lalu titik akhir. Arah slot otomatis mengikuti garis yang Anda pilih."
      },
      style = MaterialTheme.typography.labelSmall,
      color = palette.text.copy(alpha = 0.72f)
    )

    InteractiveCrosswordGridCard(
      model = model,
      palette = palette,
      anchorRow = anchorRow,
      anchorCol = anchorCol,
      pendingRow = pendingRow,
      pendingCol = pendingCol,
      onClearAll = {
        onChange(listOf(question.copy(entriesAcross = emptyList(), entriesDown = emptyList())))
        resetDraftSelection()
      },
      onConfirmDraft = {
        val nextEntry = buildDraftEntry() ?: return@InteractiveCrosswordGridCard
        val nextAcross = if (nextEntry.direction == "across") {
          upsertCrosswordEntry(question.entriesAcross, nextEntry)
        } else {
          question.entriesAcross
        }
        val nextDown = if (nextEntry.direction == "down") {
          upsertCrosswordEntry(question.entriesDown, nextEntry)
        } else {
          question.entriesDown
        }
        onChange(listOf(question.copy(entriesAcross = nextAcross, entriesDown = nextDown)))
        resetDraftSelection()
      },
      onCellClick = { rowIndex, colIndex ->
        if (anchorRow < 0 || anchorCol < 0) {
          anchorRow = rowIndex
          anchorCol = colIndex
          pendingRow = -1
          pendingCol = -1
          return@InteractiveCrosswordGridCard
        }
        if (anchorRow == rowIndex && anchorCol == colIndex) {
          resetDraftSelection()
          return@InteractiveCrosswordGridCard
        }
        val validEndpoint = (anchorRow == rowIndex && kotlin.math.abs(anchorCol - colIndex) >= 1) ||
          (anchorCol == colIndex && kotlin.math.abs(anchorRow - rowIndex) >= 1)
        if (validEndpoint) {
          pendingRow = rowIndex
          pendingCol = colIndex
        } else {
          anchorRow = rowIndex
          anchorCol = colIndex
          pendingRow = -1
          pendingCol = -1
        }
      }
    )

    CrosswordEntryGroup(
      title = "Mendatar",
      entries = model.entriesAcross,
      palette = palette,
      onEntryChange = { updated ->
        onChange(
          listOf(
            question.copy(
              entriesAcross = question.entriesAcross.map { current ->
                if (current.id == updated.id) {
                  current.copy(clue = updated.clue, answer = updated.answer)
                } else {
                  current
                }
              }
            )
          )
        )
      },
      onDeleteEntry = { entryId ->
        onChange(
          listOf(
            question.copy(
              entriesAcross = question.entriesAcross.filterNot { it.id == entryId }
            )
          )
        )
      }
    )

    CrosswordEntryGroup(
      title = "Menurun",
      entries = model.entriesDown,
      palette = palette,
      onEntryChange = { updated ->
        onChange(
          listOf(
            question.copy(
              entriesDown = question.entriesDown.map { current ->
                if (current.id == updated.id) {
                  current.copy(clue = updated.clue, answer = updated.answer)
                } else {
                  current
                }
              }
            )
          )
        )
      },
      onDeleteEntry = { entryId ->
        onChange(
          listOf(
            question.copy(
              entriesDown = question.entriesDown.filterNot { it.id == entryId }
            )
          )
        )
      }
    )

  }
}

@Composable
private fun InteractiveCrosswordGridCard(
  model: CrosswordModel,
  palette: SoalModelPalette,
  anchorRow: Int,
  anchorCol: Int,
  pendingRow: Int,
  pendingCol: Int,
  onClearAll: () -> Unit,
  onConfirmDraft: () -> Unit,
  onCellClick: (Int, Int) -> Unit
) {
  val numberMap = remember(model) { model.numberMap }
  val usedCells = remember(model) {
    buildSet {
      (model.entriesAcross + model.entriesDown).forEach { entry ->
        repeat(entry.length) { offset ->
          val row = entry.row + if (entry.direction == "down") offset else 0
          val col = entry.col + if (entry.direction == "across") offset else 0
          add("$row:$col")
        }
      }
    }
  }
  val letterMap = remember(model) {
    buildMap {
      (model.entriesAcross + model.entriesDown).forEach { entry ->
        val letters = entry.answer
          .filterNot { it.isWhitespace() }
          .take(entry.length)
        letters.forEachIndexed { index, char ->
          val row = entry.row + if (entry.direction == "down") index else 0
          val col = entry.col + if (entry.direction == "across") index else 0
          putIfAbsent("$row:$col", char.toString())
        }
      }
    }
  }
  val draftCells = remember(model, anchorRow, anchorCol, pendingRow, pendingCol) {
    buildSet {
      if (anchorRow < 0 || anchorCol < 0 || pendingRow < 0 || pendingCol < 0) return@buildSet
      if (anchorRow == pendingRow) {
        val start = minOf(anchorCol, pendingCol)
        val end = maxOf(anchorCol, pendingCol)
        for (col in start..end) add("$anchorRow:$col")
      } else if (anchorCol == pendingCol) {
        val start = minOf(anchorRow, pendingRow)
        val end = maxOf(anchorRow, pendingRow)
        for (row in start..end) add("$row:$anchorCol")
      }
    }
  }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(palette.accent.copy(alpha = 0.08f))
      .border(1.dp, palette.border.copy(alpha = 0.84f), RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = t("Grid Teka-Teki Silang"),
          style = MaterialTheme.typography.labelLarge,
          color = palette.text,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = t("Pilih titik awal lalu titik akhir. Slot baru disimpan setelah dikonfirmasi."),
          style = MaterialTheme.typography.bodySmall,
          color = palette.text.copy(alpha = 0.72f)
        )
      }
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .background(CardBackground.copy(alpha = 0.9f))
          .border(1.dp, Color(0xFFF5C2E7), RoundedCornerShape(999.dp))
          .clickable(onClick = onClearAll)
          .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFF9D174D), modifier = Modifier.size(16.dp))
          Text(
            text = t("Hapus semua slot"),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF9D174D),
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        (0 until model.rows).forEach { rowIndex ->
          Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            (0 until model.cols).forEach { colIndex ->
              val key = "$rowIndex:$colIndex"
              val isAnchor = anchorRow == rowIndex && anchorCol == colIndex
              val isPending = pendingRow == rowIndex && pendingCol == colIndex
              val isUsed = key in usedCells
              val isDraft = key in draftCells && !isAnchor && !isPending
              val isSelectableEnd = anchorRow >= 0 &&
                anchorCol >= 0 &&
                pendingRow < 0 &&
                pendingCol < 0 &&
                (
                  (rowIndex == anchorRow && kotlin.math.abs(colIndex - anchorCol) >= 1) ||
                    (colIndex == anchorCol && kotlin.math.abs(rowIndex - anchorRow) >= 1)
                  )
              val number = numberMap[key]
              val letter = letterMap[key]
              Box(
                modifier = Modifier
                  .size(30.dp)
                  .clip(RoundedCornerShape(5.dp))
                  .background(
                    when {
                      isAnchor -> Color(0xFFFDE68A)
                      isPending -> Color(0xFF86EFAC)
                      isDraft -> Color(0xFFDCFCE7)
                      isSelectableEnd -> Color(0xFFECFCCB)
                      isUsed -> palette.accent.copy(alpha = 0.14f)
                      else -> CardBackground.copy(alpha = 0.95f)
                    }
                  )
                  .border(
                    1.dp,
                    when {
                      isAnchor -> Color(0xFFD97706)
                      isPending -> Color(0xFF16A34A)
                      isDraft -> Color(0xFF22C55E)
                      isSelectableEnd -> Color(0xFF84CC16)
                      isUsed -> palette.accent.copy(alpha = 0.65f)
                      else -> palette.border.copy(alpha = 0.7f)
                    },
                    RoundedCornerShape(5.dp)
                  )
                  .clickable { onCellClick(rowIndex, colIndex) }
              ) {
                if (number != null) {
                  Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.text,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 3.dp, top = 1.dp)
                  )
                }
                if (!letter.isNullOrBlank()) {
                  Text(
                    text = letter,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.text,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.Center)
                  )
                }
              }
            }
          }
        }
      }
    }
    AnimatedVisibility(visible = pendingRow >= 0 && pendingCol >= 0) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .background(Color(0xFF16A34A))
          .clickable(onClick = onConfirmDraft)
          .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
          Text(
            text = t("Konfirmasi slot"),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
    if (anchorRow >= 0 && anchorCol >= 0 && pendingRow < 0 && pendingCol < 0) {
      Text(
        text = t("Pilih titik akhir pada baris atau kolom yang sama untuk melanjutkan draft."),
        style = MaterialTheme.typography.bodySmall,
        color = palette.text.copy(alpha = 0.72f)
      )
    }
  }
}

@Composable
private fun CrosswordEntryGroup(
  title: String,
  entries: List<CrosswordEntryModel>,
  palette: SoalModelPalette,
  onEntryChange: (CrosswordEntryModel) -> Unit,
  onDeleteEntry: (String) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = t(title),
      style = MaterialTheme.typography.labelLarge,
      color = palette.text,
      fontWeight = FontWeight.ExtraBold
    )
    if (entries.isEmpty()) {
      Text(
        text = t("Belum ada slot. Buat dari grid di atas."),
        style = MaterialTheme.typography.bodySmall,
        color = palette.text.copy(alpha = 0.72f)
      )
    }
    entries.forEach { entry ->
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(palette.accent.copy(alpha = 0.06f))
          .border(1.dp, palette.border.copy(alpha = 0.74f), RoundedCornerShape(16.dp))
          .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(30.dp)
              .clip(CircleShape)
              .background(palette.accent.copy(alpha = 0.12f))
              .border(1.dp, palette.accent.copy(alpha = 0.20f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text("${entry.number}", style = MaterialTheme.typography.labelMedium, color = palette.text, fontWeight = FontWeight.Bold)
          }
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "${t("Baris")} ${entry.row + 1}, ${t("Kolom")} ${entry.col + 1}",
              style = MaterialTheme.typography.labelMedium,
              color = palette.text,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${entry.length} ${t("kotak")}",
              style = MaterialTheme.typography.bodySmall,
              color = palette.text.copy(alpha = 0.72f)
            )
          }
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .background(CardBackground.copy(alpha = 0.92f))
              .border(1.dp, Color(0xFFF5C2E7), RoundedCornerShape(999.dp))
              .clickable(onClick = { onDeleteEntry(entry.id) })
              .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Outlined.Close, contentDescription = t("Hapus slot"), tint = Color(0xFFBE123C), modifier = Modifier.size(16.dp))
          }
        }
        SoalTextField(
          value = entry.clue,
          onValueChange = { value ->
            onEntryChange(entry.copy(clue = value))
          },
          label = "Petunjuk",
          modifier = Modifier.fillMaxWidth(),
          minLines = 2
        )
        SoalTextField(
          value = entry.answer,
          onValueChange = { value ->
            val compact = value.filterNot { it.isWhitespace() }.take(entry.length)
            onEntryChange(entry.copy(answer = compact))
          },
          label = "Jawaban",
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@Composable
private fun CrosswordPreviewCard(
  model: CrosswordModel,
  palette: SoalModelPalette
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(palette.accent.copy(alpha = 0.08f))
      .border(1.dp, palette.border.copy(alpha = 0.84f), RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = t("Preview puzzle"),
      style = MaterialTheme.typography.labelLarge,
      color = palette.text,
      fontWeight = FontWeight.ExtraBold
    )
    model.maskRows.forEachIndexed { rowIndex, row ->
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        row.forEachIndexed { colIndex, cell ->
          val isOpen = cell == '.'
          val number = model.numberMap["$rowIndex:$colIndex"]
          Box(
            modifier = Modifier
              .size(24.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(if (isOpen) CardBackground.copy(alpha = 0.94f) else palette.text.copy(alpha = 0.9f))
              .border(1.dp, if (isOpen) palette.border.copy(alpha = 0.7f) else palette.text.copy(alpha = 0.9f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
          ) {
            if (isOpen && number != null) {
              Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = palette.text,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WordSearchPreviewCard(
  puzzle: WordSearchPuzzle,
  palette: SoalModelPalette
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(palette.accent.copy(alpha = 0.08f))
      .border(1.dp, palette.border.copy(alpha = 0.84f), RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = t("Preview puzzle"),
      style = MaterialTheme.typography.labelLarge,
      color = palette.text,
      fontWeight = FontWeight.ExtraBold
    )
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        puzzle.grid.forEach { row ->
          Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            row.forEach { cell ->
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(RoundedCornerShape(5.dp))
                  .background(CardBackground.copy(alpha = 0.92f))
                  .border(1.dp, palette.border.copy(alpha = 0.7f), RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = cell.toString(),
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.text,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    }
    Text(
      text = puzzle.words.joinToString(", "),
      style = MaterialTheme.typography.bodySmall,
      color = palette.text.copy(alpha = 0.8f)
    )
    if (puzzle.unplacedWords.isNotEmpty()) {
      Text(
        text = "${t("Kata belum muat")}: ${puzzle.unplacedWords.joinToString(", ")}",
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFFB91C1C),
        fontWeight = FontWeight.SemiBold
      )
    }
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
  val choiceQuestions: List<MapelChoiceQuestion> = emptyList(),
  val matchingPairs: List<MapelMatchingPair> = emptyList(),
  val wordSearchQuestions: List<MapelWordSearchQuestion> = emptyList(),
  val crosswordQuestions: List<MapelCrosswordQuestion> = emptyList()
)

private data class SoalModelPalette(
  val background: Color,
  val border: Color,
  val accent: Color,
  val text: Color
)

@Composable
private fun MapelQuestionSection.modelPalette(): SoalModelPalette {
  val lightPalette = when (typeKey) {
    "pilihan-ganda" -> SoalModelPalette(Color(0xFFEFF6FF), Color(0xFFBFDBFE), Color(0xFF2563EB), Color(0xFF1E3A8A))
    "benar-salah" -> SoalModelPalette(Color(0xFFECFDF5), Color(0xFFA7F3D0), Color(0xFF059669), Color(0xFF065F46))
    "cari-kata" -> SoalModelPalette(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFD97706), Color(0xFF92400E))
    "teka-silang" -> SoalModelPalette(Color(0xFFF5F3FF), Color(0xFFDDD6FE), Color(0xFF7C3AED), Color(0xFF4C1D95))
    "esai" -> SoalModelPalette(Color(0xFFFFF7ED), Color(0xFFFED7AA), Color(0xFFEA580C), Color(0xFF9A3412))
    "pasangkan-kata" -> SoalModelPalette(Color(0xFFFDF2F8), Color(0xFFFBCFE8), Color(0xFFDB2777), Color(0xFF9D174D))
    "isi-titik" -> SoalModelPalette(Color(0xFFF0FDFA), Color(0xFF99F6E4), Color(0xFF0D9488), Color(0xFF115E59))
    else -> SoalModelPalette(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFF2563EB), Color(0xFF1E3A8A))
  }
  val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
  if (!isDarkTheme) return lightPalette

  val darkAccent = lerp(lightPalette.accent, Color.White, 0.18f)
  return SoalModelPalette(
    background = lerp(CardBackground, darkAccent, 0.10f),
    border = lerp(CardBorder, darkAccent, 0.35f),
    accent = darkAccent,
    text = PrimaryBlueDark
  )
}

private data class MapelChoiceQuestion(
  val id: String,
  val prompt: String = "",
  val options: List<String> = listOf("", "")
)

private data class MapelMatchingPair(
  val id: String,
  val leftText: String = "",
  val rightText: String = ""
)

private data class MapelWordSearchQuestion(
  val id: String,
  val prompt: String = "",
  val rows: Int = 10,
  val cols: Int = 10,
  val wordsText: String = ""
)

private data class MapelCrosswordEntry(
  val id: String,
  val direction: String,
  val row: Int = 1,
  val col: Int = 1,
  val length: Int = 2,
  val clue: String = "",
  val answer: String = ""
)

private data class MapelCrosswordQuestion(
  val id: String,
  val rows: Int = 10,
  val cols: Int = 10,
  val entriesAcross: List<MapelCrosswordEntry> = emptyList(),
  val entriesDown: List<MapelCrosswordEntry> = emptyList()
)

private data class WordSearchPlacement(
  val word: String,
  val row: Int,
  val col: Int,
  val direction: String
)

private data class WordSearchPuzzle(
  val rows: Int,
  val cols: Int,
  val words: List<String>,
  val grid: List<List<Char>>,
  val placements: List<WordSearchPlacement>,
  val unplacedWords: List<String>
)

private data class CrosswordEntryModel(
  val id: String,
  val direction: String,
  val row: Int,
  val col: Int,
  val length: Int,
  val clue: String,
  val answer: String,
  val number: Int
)

private data class CrosswordModel(
  val rows: Int,
  val cols: Int,
  val maskRows: List<String>,
  val numberMap: Map<String, Int>,
  val entriesAcross: List<CrosswordEntryModel>,
  val entriesDown: List<CrosswordEntryModel>
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

private fun defaultAcademicYearLabel(date: LocalDate = LocalDate.now()): String {
  val startYear = if (date.monthValue >= 7) date.year else date.year - 1
  return "$startYear/${startYear + 1}"
}

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
  return this in SoalTypeOptions.map { it.key } &&
    this != "pasangkan-kata" &&
    this != "cari-kata" &&
    this != "teka-silang"
}

private fun String.usesMatchingPairEditor(): Boolean {
  return this == "pasangkan-kata"
}

private fun String.usesWordSearchEditor(): Boolean {
  return this == "cari-kata"
}

private fun String.usesCrosswordEditor(): Boolean {
  return this == "teka-silang"
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

private fun List<MapelMatchingPair>.withTrailingBlankMatchingPair(): List<MapelMatchingPair> {
  val cleanPairs = mapIndexed { index, current ->
    current.copy(id = current.id.ifBlank { "pair-${index + 1}" })
  }.toMutableList()
  while (cleanPairs.isNotEmpty() && !cleanPairs.last().hasContent()) {
    cleanPairs.removeAt(cleanPairs.lastIndex)
  }
  cleanPairs.add(MapelMatchingPair(id = "pair-${cleanPairs.size + 1}"))
  return cleanPairs
}

private fun List<MapelMatchingPair>.filledMatchingPairs(): List<MapelMatchingPair> {
  return mapIndexed { index, current ->
    current.copy(id = current.id.ifBlank { "pair-${index + 1}" })
  }.filter { it.hasContent() }
}

private fun MapelMatchingPair.hasContent(): Boolean {
  return leftText.isNotBlank() || rightText.isNotBlank()
}

private fun List<MapelWordSearchQuestion>.filledWordSearchQuestions(): List<MapelWordSearchQuestion> {
  return take(1).mapIndexed { index, current ->
    current.copy(
      id = current.id.ifBlank { "ws-${index + 1}" },
      rows = current.rows.coerceIn(5, 20),
      cols = current.cols.coerceIn(5, 20)
    )
  }.filter { it.hasContent() }
}

private fun MapelWordSearchQuestion.hasContent(): Boolean {
  return words().isNotEmpty()
}

private fun MapelWordSearchQuestion.words(languageCode: String = "ID"): List<String> {
  val isArabic = languageCode.uppercase() == "AR"
  return wordsText
    .lines()
    .map { line ->
      val trimmed = line.trim()
      if (isArabic) trimmed.replace(Regex("\\s+"), "")
      else trimmed.uppercase().replace(Regex("[^A-Z0-9]"), "")
    }
    .filter { it.isNotBlank() }
    .distinct()
}

private fun List<MapelCrosswordEntry>.withTrailingBlankCrosswordEntry(direction: String): List<MapelCrosswordEntry> {
  val safeDirection = if (direction == "down") "down" else "across"
  val cleanEntries = mapIndexed { index, current ->
    current.copy(
      id = current.id.ifBlank { "$safeDirection-${index + 1}" },
      direction = safeDirection
    )
  }.toMutableList()
  while (cleanEntries.isNotEmpty() && !cleanEntries.last().hasContent()) {
    cleanEntries.removeAt(cleanEntries.lastIndex)
  }
  cleanEntries.add(MapelCrosswordEntry(id = "$safeDirection-${cleanEntries.size + 1}", direction = safeDirection))
  return cleanEntries
}

private fun List<MapelCrosswordEntry>.filledCrosswordEntries(direction: String): List<MapelCrosswordEntry> {
  val safeDirection = if (direction == "down") "down" else "across"
  return mapIndexed { index, current ->
    current.copy(
      id = current.id.ifBlank { "$safeDirection-${index + 1}" },
      direction = safeDirection,
      row = current.row.coerceIn(1, 20),
      col = current.col.coerceIn(1, 20),
      length = current.length.coerceIn(2, 20)
    )
  }.filter { it.hasContent() }
}

private fun upsertCrosswordEntry(
  currentEntries: List<MapelCrosswordEntry>,
  newEntry: MapelCrosswordEntry
): List<MapelCrosswordEntry> {
  val safeDirection = if (newEntry.direction == "down") "down" else "across"
  val normalizedNew = newEntry.copy(direction = safeDirection)
  val previous = currentEntries.firstOrNull { entry ->
    entry.direction == safeDirection &&
      entry.row == normalizedNew.row &&
      entry.col == normalizedNew.col
  }
  val filtered = currentEntries.filterNot { entry ->
    val sameDirection = entry.direction == safeDirection
    val sameStart = entry.row == normalizedNew.row && entry.col == normalizedNew.col
    sameDirection && sameStart
  }
  val merged = normalizedNew.copy(
    id = previous?.id?.ifBlank { normalizedNew.id } ?: normalizedNew.id,
    clue = if (normalizedNew.clue.isBlank()) previous?.clue.orEmpty() else normalizedNew.clue,
    answer = if (normalizedNew.answer.isBlank()) previous?.answer.orEmpty() else normalizedNew.answer
  )
  return (filtered + merged)
    .sortedWith(compareBy<MapelCrosswordEntry> { it.row }.thenBy { it.col }.thenBy { it.length })
}

private fun MapelCrosswordEntry.isPlaceholderCrosswordEntry(): Boolean {
  return clue.isBlank() &&
    answer.isBlank() &&
    row == 1 &&
    col == 1 &&
    length == 2 &&
    Regex("^(across|down)-\\d+$").matches(id)
}

private fun MapelCrosswordEntry.hasContent(): Boolean {
  return !isPlaceholderCrosswordEntry()
}

private fun MapelCrosswordQuestion.hasContent(): Boolean {
  return entriesAcross.filledCrosswordEntries("across").isNotEmpty() ||
    entriesDown.filledCrosswordEntries("down").isNotEmpty()
}

private fun MapelQuestionSection.effectiveQuestionCount(): Int {
  return when {
    typeKey.usesCrosswordEditor() -> if (crosswordQuestions.firstOrNull()?.hasContent() == true) 1 else 0
    typeKey.usesWordSearchEditor() -> wordSearchQuestions.filledWordSearchQuestions().size
    typeKey.usesMatchingPairEditor() -> if (matchingPairs.filledMatchingPairs().isNotEmpty()) 1 else 0
    typeKey.usesQuestionPromptEditor() -> choiceQuestions.filledQuestionCount()
    content.isNotBlank() -> count.coerceAtLeast(1)
    else -> count.coerceAtLeast(0)
  }
}

private fun buildWordSearchPuzzle(
  question: MapelWordSearchQuestion,
  languageCode: String = "ID"
): WordSearchPuzzle {
  val rows = question.rows.coerceIn(5, 20)
  val cols = question.cols.coerceIn(5, 20)
  val words = question.words(languageCode).sortedByDescending { it.length }
  val random = seededWordSearchRandom("${languageCode.uppercase()}|${rows}x$cols|${question.prompt}|${question.wordsText}")
  val alphabet = if (languageCode.uppercase() == "AR") {
    listOf('ا', 'ب', 'ت', 'ث', 'ج', 'ح', 'خ', 'د', 'ر', 'س', 'ص', 'ع', 'ف', 'ق', 'ك', 'ل', 'م', 'ن', 'ه', 'و', 'ي')
  } else {
    ('A'..'Z').toList()
  }
  val grid = MutableList(rows) { MutableList(cols) { '\u0000' } }
  val directions = listOf(
    Triple(1, 0, "H"),
    Triple(0, 1, "V"),
    Triple(1, 1, "D1"),
    Triple(-1, 1, "D2")
  )
  val placements = mutableListOf<WordSearchPlacement>()
  val unplacedWords = mutableListOf<String>()

  words.forEach { word ->
    val letters = word.toList()
    val candidates = mutableListOf<Triple<Int, Int, Triple<Int, Int, String>>>()
    directions.forEach { direction ->
      for (row in 0 until rows) {
        for (col in 0 until cols) {
          val endRow = row + (direction.second * (letters.size - 1))
          val endCol = col + (direction.first * (letters.size - 1))
          if (endRow !in 0 until rows || endCol !in 0 until cols) continue
          var fits = true
          letters.forEachIndexed { index, char ->
            val rr = row + (direction.second * index)
            val cc = col + (direction.first * index)
            val current = grid[rr][cc]
            if (current != '\u0000' && current != char) {
              fits = false
              return@forEachIndexed
            }
          }
          if (fits) candidates.add(Triple(row, col, direction))
        }
      }
    }
    if (candidates.isEmpty()) {
      unplacedWords.add(word)
    } else {
      val choice = candidates[random.nextInt(candidates.size)]
      letters.forEachIndexed { index, char ->
        val rr = choice.first + (choice.third.second * index)
        val cc = choice.second + (choice.third.first * index)
        grid[rr][cc] = char
      }
      placements.add(WordSearchPlacement(word, choice.first, choice.second, choice.third.third))
    }
  }

  for (row in 0 until rows) {
    for (col in 0 until cols) {
      if (grid[row][col] == '\u0000') {
        grid[row][col] = alphabet[random.nextInt(alphabet.size)]
      }
    }
  }

  return WordSearchPuzzle(
    rows = rows,
    cols = cols,
    words = words,
    grid = grid.map { it.toList() },
    placements = placements,
    unplacedWords = unplacedWords
  )
}

private fun seededWordSearchRandom(seed: String): java.util.Random {
  var hash = 1125899906842597L
  seed.forEach { char ->
    hash = 31L * hash + char.code.toLong()
  }
  return java.util.Random(hash)
}

private fun buildCrosswordModel(question: MapelCrosswordQuestion): CrosswordModel {
  val rows = question.rows.coerceIn(5, 20)
  val cols = question.cols.coerceIn(5, 20)
  val entriesAcross = question.entriesAcross.filledCrosswordEntries("across")
  val entriesDown = question.entriesDown.filledCrosswordEntries("down")
  val startCells = linkedMapOf<String, Pair<Int, Int>>()
  val maskGrid = MutableList(rows) { MutableList(cols) { '#' } }

  fun apply(entries: List<MapelCrosswordEntry>) {
    entries.forEach { entry ->
      val row = (entry.row - 1).coerceIn(0, rows - 1)
      val col = (entry.col - 1).coerceIn(0, cols - 1)
      val maxLength = if (entry.direction == "down") rows - row else cols - col
      val length = entry.length.coerceIn(2, maxLength.coerceAtLeast(2))
      startCells.putIfAbsent("$row:$col", row to col)
      repeat(length) { offset ->
        val rr = row + if (entry.direction == "down") offset else 0
        val cc = col + if (entry.direction == "across") offset else 0
        if (rr in 0 until rows && cc in 0 until cols) {
          maskGrid[rr][cc] = '.'
        }
      }
    }
  }

  apply(entriesAcross)
  apply(entriesDown)

  val numberMap = linkedMapOf<String, Int>()
  startCells.values
    .sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
    .forEachIndexed { index, pair ->
      numberMap["${pair.first}:${pair.second}"] = index + 1
    }

  fun toModel(entries: List<MapelCrosswordEntry>): List<CrosswordEntryModel> {
    return entries.map { entry ->
      val row = (entry.row - 1).coerceIn(0, rows - 1)
      val col = (entry.col - 1).coerceIn(0, cols - 1)
      CrosswordEntryModel(
        id = entry.id,
        direction = entry.direction,
        row = row,
        col = col,
        length = entry.length.coerceAtLeast(2),
        clue = entry.clue,
        answer = entry.answer,
        number = numberMap["$row:$col"] ?: 0
      )
    }
  }

  return CrosswordModel(
    rows = rows,
    cols = cols,
    maskRows = maskGrid.map { it.joinToString("") },
    numberMap = numberMap,
    entriesAcross = toModel(entriesAcross),
    entriesDown = toModel(entriesDown)
  )
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
  val combinedQuestionRows = JSONArray()
  sorted.forEachIndexed { draftIndex, draft ->
    val draftPayload = runCatching { JSONObject(draft.buildDocxQuestionsPayloadJson()) }.getOrNull()
    val rows = draftPayload?.optJSONArray("questions") ?: JSONArray()
    for (index in 0 until rows.length()) {
      val row = rows.optJSONObject(index) ?: continue
      val originalKey = row.optString("sectionKey").ifBlank { "section-${index + 1}" }
      row.put("sectionKey", "draft-${draftIndex + 1}-$originalKey")
      if (index == 0) {
        val intro = buildString {
          append(draft.title.ifBlank { "Soal" })
          if (draft.category.isNotBlank()) append(" (${draft.category})")
          if (draft.exportInstructionText().isNotBlank()) {
            append('\n')
            append(draft.exportInstructionText())
          }
        }
        row.put("sectionInstruction", intro)
      }
      combinedQuestionRows.put(row)
    }
  }
  val languageCode = if (sorted.any { it.resolvedPrintLanguageCode() == "AR" }) "AR" else "ID"
  return MapelQuestionExportData(
    id = "combined-${subject.id}-${System.currentTimeMillis()}",
    title = "Kumpulan Soal ${subject.title}",
    category = "Kumpulan Soal",
    form = sorted.map { it.form }.filter { it.isNotBlank() }.distinct().joinToString(", "),
    dateLabel = LocalDate.now().toString(),
    academicYearLabel = sorted.firstNotNullOfOrNull { it.academicYearLabel.takeIf(String::isNotBlank) }
      ?: defaultAcademicYearLabel(),
    instruction = "Dokumen ini berisi ${sorted.size} paket soal yang dibuat guru.",
    questionsPayloadJson = JSONObject().apply {
      put("questions", combinedQuestionRows)
    }.toString(),
    languageCode = languageCode,
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

private fun mergeLocalAndRemoteMapelQuestions(
  localQuestions: List<MapelQuestionDraft>,
  remoteQuestions: List<MapelQuestionDraft>
): List<MapelQuestionDraft> {
  if (localQuestions.isEmpty()) return remoteQuestions
  if (remoteQuestions.isEmpty()) return localQuestions

  val localById = localQuestions.associateBy { it.id }
  val remoteById = remoteQuestions.associateBy { it.id }
  return (localById.keys + remoteById.keys)
    .mapNotNull { questionId ->
      val localQuestion = localById[questionId]
      val remoteQuestion = remoteById[questionId]
      when {
        localQuestion == null -> remoteQuestion
        remoteQuestion == null -> localQuestion
        shouldPreferLocalQuestion(localQuestion, remoteQuestion) -> localQuestion
        else -> remoteQuestion
      }
    }
    .sortedByDescending { it.updatedAt }
}

private fun shouldPreferLocalQuestion(
  localQuestion: MapelQuestionDraft,
  remoteQuestion: MapelQuestionDraft
): Boolean {
  val localStatus = normalizeQuestionStatusLabel(localQuestion.statusLabel)
  val remoteStatus = normalizeQuestionStatusLabel(remoteQuestion.statusLabel)
  return when {
    localQuestion.hasQuestionEditorContent() && !remoteQuestion.hasQuestionEditorContent() -> true
    localStatus == QuestionStatusDraft && localQuestion.updatedAt >= remoteQuestion.updatedAt -> true
    localQuestion.updatedAt > remoteQuestion.updatedAt -> true
    localStatus == QuestionStatusSaved && remoteStatus != QuestionStatusSaved -> true
    else -> false
  }
}

private fun MapelQuestionDraft.hasQuestionEditorContent(): Boolean {
  if (questionsText.isNotBlank()) return true
  return sections.any { section ->
    section.content.isNotBlank() ||
      section.choiceQuestions.any { it.hasQuestionContent() } ||
      section.matchingPairs.any { it.hasContent() } ||
      section.wordSearchQuestions.any { it.hasContent() } ||
      section.crosswordQuestions.any { it.hasContent() }
  }
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

private fun loadMatchingPairs(array: JSONArray?): List<MapelMatchingPair> {
  if (array == null) return emptyList()
  return buildList {
    for (index in 0 until array.length()) {
      val row = array.optJSONObject(index)
      if (row != null) {
        add(
          MapelMatchingPair(
            id = row.optString("id").ifBlank { "pair-${index + 1}" },
            leftText = row.optString("leftText"),
            rightText = row.optString("rightText")
          )
        )
      } else {
        val text = array.optString(index)
        if (text.isNotBlank()) add(MapelMatchingPair(id = "pair-${index + 1}", leftText = text))
      }
    }
  }
}

private fun loadWordSearchQuestions(array: JSONArray?): List<MapelWordSearchQuestion> {
  if (array == null) return emptyList()
  return buildList {
    for (index in 0 until array.length()) {
      val row = array.optJSONObject(index) ?: continue
      add(
        MapelWordSearchQuestion(
          id = row.optString("id").ifBlank { "ws-${index + 1}" },
          prompt = row.optString("prompt"),
          rows = row.optInt("rows", 10).coerceIn(5, 20),
          cols = row.optInt("cols", 10).coerceIn(5, 20),
          wordsText = row.optString("wordsText")
        )
      )
    }
  }
}

private fun loadCrosswordEntries(array: JSONArray?, direction: String): List<MapelCrosswordEntry> {
  if (array == null) return emptyList()
  val safeDirection = if (direction == "down") "down" else "across"
  return buildList {
    for (index in 0 until array.length()) {
      val row = array.optJSONObject(index) ?: continue
      add(
        MapelCrosswordEntry(
          id = row.optString("id").ifBlank { "$safeDirection-${index + 1}" },
          direction = safeDirection,
          row = row.optInt("row", 1),
          col = row.optInt("col", 1),
          length = row.optInt("length", 2),
          clue = row.optString("clue"),
          answer = row.optString("answer")
        )
      )
    }
  }
}

private fun loadCrosswordQuestions(sectionRow: JSONObject): List<MapelCrosswordQuestion> {
  val explicitArray = sectionRow.optJSONArray("crosswordQuestions")
  if (explicitArray != null) {
    return buildList {
      for (index in 0 until explicitArray.length()) {
        val row = explicitArray.optJSONObject(index) ?: continue
        add(
          MapelCrosswordQuestion(
            id = row.optString("id").ifBlank { "cw-${index + 1}" },
            rows = row.optInt("rows", 10).coerceIn(5, 20),
            cols = row.optInt("cols", 10).coerceIn(5, 20),
            entriesAcross = loadCrosswordEntries(row.optJSONArray("entriesAcross"), "across"),
            entriesDown = loadCrosswordEntries(row.optJSONArray("entriesDown"), "down")
          )
        )
      }
    }
  }
  val legacyAcross = loadCrosswordEntries(sectionRow.optJSONArray("entriesAcross"), "across")
  val legacyDown = loadCrosswordEntries(sectionRow.optJSONArray("entriesDown"), "down")
  return if (legacyAcross.isNotEmpty() || legacyDown.isNotEmpty()) {
    listOf(
      MapelCrosswordQuestion(
        id = "cw-1",
        rows = sectionRow.optInt("rows", 10).coerceIn(5, 20),
        cols = sectionRow.optInt("cols", 10).coerceIn(5, 20),
        entriesAcross = legacyAcross,
        entriesDown = legacyDown
      )
    )
  } else {
    emptyList()
  }
}

private fun loadMapelQuestions(context: Context, subjectId: String): List<MapelQuestionDraft> {
  val rawJson = context.getSharedPreferences("mapel_questions", Context.MODE_PRIVATE)
    .getString(subjectId, "[]")
    .orEmpty()
  return decodeMapelQuestions(rawJson)
}

private fun decodeMapelQuestions(rawJson: String): List<MapelQuestionDraft> {
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
                  choiceQuestions = loadChoiceQuestions(sectionRow.optJSONArray("choiceQuestions")),
                  matchingPairs = loadMatchingPairs(sectionRow.optJSONArray("matchingPairs")),
                  wordSearchQuestions = loadWordSearchQuestions(sectionRow.optJSONArray("wordSearchQuestions")),
                  crosswordQuestions = loadCrosswordQuestions(sectionRow)
                )
              )
            }
          }
        }
        val storedLanguageCode = row.optString("languageCode")
          .ifBlank { row.optString("printLanguageCode") }
          .ifBlank { row.optString("language_code") }
        val draft = MapelQuestionDraft(
          id = row.optString("id").ifBlank { "soal-${System.currentTimeMillis()}-$index" },
          title = row.optString("title"),
          category = row.optString("category").ifBlank { "Ujian" },
          form = row.optString("form").ifBlank { sections.map { it.typeLabel }.distinct().joinToString(", ") },
          dateIso = row.optString("dateIso").ifBlank { LocalDate.now().toString() },
          academicYearLabel = row.optString("academicYearLabel")
            .ifBlank { row.optString("academic_year_label") }
            .ifBlank { defaultAcademicYearLabel() },
          instruction = row.optString("instruction"),
          questionsText = row.optString("questionsText"),
          sections = sections,
          languageCode = normalizeSoalPrintLanguageCode(storedLanguageCode).ifBlank { "ID" },
          statusLabel = normalizeQuestionStatusLabel(row.optString("statusLabel")),
          updatedAt = row.optLong("updatedAt", System.currentTimeMillis())
        )
        add(
          if (storedLanguageCode.isBlank()) {
            draft.copy(languageCode = draft.detectQuestionLanguage())
          } else {
            draft
          }
        )
      }
    }.sortedByDescending { it.updatedAt }
  }.getOrDefault(emptyList())
}

private fun saveMapelQuestions(
  context: Context,
  subjectId: String,
  questions: List<MapelQuestionDraft>,
  synchronous: Boolean = false
): String {
  val array = JSONArray()
  questions.forEach { item ->
    array.put(
      JSONObject().apply {
        put("id", item.id)
        put("title", item.title)
        put("category", item.category)
        put("form", item.form)
        put("dateIso", item.dateIso)
        put("academicYearLabel", item.academicYearLabel.ifBlank { defaultAcademicYearLabel() })
        put("instruction", item.instruction)
        put("questionsText", item.questionsText)
        put("languageCode", item.resolvedPrintLanguageCode())
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
                  put(
                    "matchingPairs",
                    JSONArray().apply {
                      section.matchingPairs.forEach { pair ->
                        put(
                          JSONObject().apply {
                            put("id", pair.id)
                            put("leftText", pair.leftText)
                            put("rightText", pair.rightText)
                          }
                        )
                      }
                    }
                  )
                  put(
                    "wordSearchQuestions",
                    JSONArray().apply {
                      section.wordSearchQuestions.forEach { question ->
                        put(
                          JSONObject().apply {
                            put("id", question.id)
                            put("prompt", question.prompt)
                            put("rows", question.rows)
                            put("cols", question.cols)
                            put("wordsText", question.wordsText)
                          }
                        )
                      }
                    }
                  )
                  put(
                    "crosswordQuestions",
                    JSONArray().apply {
                      section.crosswordQuestions.forEach { question ->
                        put(
                          JSONObject().apply {
                            put("id", question.id)
                            put("rows", question.rows)
                            put("cols", question.cols)
                            put(
                              "entriesAcross",
                              JSONArray().apply {
                                question.entriesAcross.forEach { entry ->
                                  put(
                                    JSONObject().apply {
                                      put("id", entry.id)
                                      put("row", entry.row)
                                      put("col", entry.col)
                                      put("length", entry.length)
                                      put("clue", entry.clue)
                                      put("answer", entry.answer)
                                    }
                                  )
                                }
                              }
                            )
                            put(
                              "entriesDown",
                              JSONArray().apply {
                                question.entriesDown.forEach { entry ->
                                  put(
                                    JSONObject().apply {
                                      put("id", entry.id)
                                      put("row", entry.row)
                                      put("col", entry.col)
                                      put("length", entry.length)
                                      put("clue", entry.clue)
                                      put("answer", entry.answer)
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
                }
              )
            }
          }
        )
        put("statusLabel", normalizeQuestionStatusLabel(item.statusLabel))
        put("updatedAt", item.updatedAt)
      }
    )
  }
  val encoded = array.toString()
  val editor = context.getSharedPreferences("mapel_questions", Context.MODE_PRIVATE)
    .edit()
    .putString(subjectId, encoded)
  if (synchronous) {
    editor.commit()
  } else {
    editor.apply()
  }
  return encoded
}

private fun filterMapelQuestionJsonById(rawJson: String, questionId: String): String {
  return runCatching {
    val sourceArray = JSONArray(rawJson.ifBlank { "[]" })
    JSONArray().apply {
      for (index in 0 until sourceArray.length()) {
        val row = sourceArray.optJSONObject(index) ?: continue
        if (row.optString("id") == questionId) {
          put(row)
        }
      }
    }.toString()
  }.getOrDefault("[]")
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

private data class PatronLearningMaterial(
  val note: String = "",
  val pdfFileName: String = "",
  val pdfOriginalName: String = "",
  val blocks: List<PatronLearningBlock> = emptyList(),
  val updatedAt: Long = 0L
)

private data class PatronLearningBlock(
  val id: String,
  val type: String,
  val text: String = "",
  val fileName: String = "",
  val originalName: String = "",
  val spans: List<PatronTextSpan> = emptyList()
)

private data class PatronTextSpan(
  val start: Int,
  val end: Int,
  val bold: Boolean = false,
  val italic: Boolean = false,
  val underline: Boolean = false,
  val list: Boolean = false
)

private enum class PatronLearningBlockType(val key: String) {
  Text("text"),
  List("list"),
  Image("image"),
  Pdf("pdf")
}

private data class AiPatronMaterialPayload(
  val items: List<PatronMateriItem>,
  val materials: Map<String, PatronLearningMaterial>
)

private fun buildAiPatronMaterialPayload(
  generatedMaterial: GuruAiMaterialResult,
  currentItems: List<PatronMateriItem>,
  currentMaterials: Map<String, PatronLearningMaterial>
): AiPatronMaterialPayload {
  val generatedItems = generatedMaterial.items.ifEmpty {
    val fallbackTitle = generatedMaterial.title.ifBlank { "Materi AI" }
    val fallbackBody = generatedMaterial.summary.ifBlank { fallbackTitle }
    listOf(GuruAiMaterialItem(title = fallbackTitle, body = fallbackBody, bulletPoints = emptyList()))
  }
  val previousMaterialByTitle = currentItems.associate { item ->
    item.text.aiContentKey() to currentMaterials[item.id]
  }
  val generatedByTitle = linkedMapOf<String, GuruAiMaterialItem>()
  generatedItems.forEachIndexed { index, item ->
    val title = item.title.trim().ifBlank { "Materi AI ${index + 1}" }
    generatedByTitle.putIfAbsent(title.aiContentKey(), item.copy(title = title))
  }

  val orderedTitles = buildList {
    generatedByTitle.values.forEach { add(it.title.trim()) }
    currentItems.map { it.text.trim() }
      .filter { it.isNotBlank() }
      .forEach { add(it) }
  }.distinctBy { it.aiContentKey() }

  val normalizedItems = orderedTitles.mapIndexed { index, title ->
    PatronMateriItem(
      id = normalizedPatronMateriId(index, title),
      text = title
    )
  }
  val materials = linkedMapOf<String, PatronLearningMaterial>()
  normalizedItems.forEachIndexed { index, item ->
    val generated = generatedByTitle[item.text.aiContentKey()]
    val previous = previousMaterialByTitle[item.text.aiContentKey()]
    val material = when {
      generated != null -> generated.toPatronLearningMaterial(index)
      previous != null -> previous
      else -> null
    }
    if (material != null) materials[item.id] = material
  }
  return AiPatronMaterialPayload(items = normalizedItems, materials = materials)
}

private fun GuruAiMaterialItem.toPatronLearningMaterial(index: Int): PatronLearningMaterial {
  val bodyText = buildString {
    if (body.isNotBlank()) appendLine(body.trim())
    if (bulletPoints.isNotEmpty()) {
      if (isNotBlank()) appendLine()
      bulletPoints.forEach { point ->
        val cleanPoint = point.trim()
        if (cleanPoint.isNotBlank()) appendLine("- $cleanPoint")
      }
    }
  }.trim().ifBlank { title.trim() }
  return PatronLearningMaterial(
    blocks = listOf(
      PatronLearningBlock(
        id = "ai-block-${System.currentTimeMillis()}-$index",
        type = PatronLearningBlockType.Text.key,
        text = bodyText
      )
    ),
    updatedAt = System.currentTimeMillis()
  )
}

private fun buildAiQuestionDraft(
  subject: SubjectOverview,
  result: GuruAiQuestionResult,
  fallbackTypeKey: String,
  fallbackLanguageCode: String,
  academicYearLabel: String
): MapelQuestionDraft {
  val now = System.currentTimeMillis()
  val typeKey = normalizeAiQuestionTypeKey(result.questionType, fallbackTypeKey)
  val selectedType = SoalTypeOptions.firstOrNull { it.key == typeKey } ?: SoalTypeOptions.first()
  val generatedQuestions = result.questions.take(50)
  val choiceQuestions = generatedQuestions.mapIndexed { index, question ->
    MapelChoiceQuestion(
      id = "q-ai-$now-${index + 1}",
      prompt = question.prompt,
      options = if (typeKey == "pilihan-ganda") {
        question.options.normalizedChoiceOptions()
      } else {
        emptyList()
      }
    )
  }.withTrailingBlankQuestion()
  val section = buildQuestionSectionDraft(selectedType).copy(
    id = "model-ai-$now",
    count = generatedQuestions.size,
    instruction = result.instruction,
    choiceQuestions = if (selectedType.key.usesQuestionPromptEditor()) choiceQuestions else emptyList()
  )
  return MapelQuestionDraft(
    id = "soal-ai-$now",
    title = result.title.ifBlank { "Soal ${subject.title}" },
    category = "Ujian",
    form = selectedType.label,
    academicYearLabel = academicYearLabel.ifBlank { defaultAcademicYearLabel() },
    instruction = result.instruction,
    sections = listOf(section),
    languageCode = normalizeSoalPrintLanguageCode(result.languageCode)
      .ifBlank { normalizeSoalPrintLanguageCode(fallbackLanguageCode) }
      .ifBlank { "ID" },
    statusLabel = QuestionStatusDraft,
    updatedAt = now
  )
}

private fun normalizeAiQuestionTypeKey(value: String, fallbackTypeKey: String): String {
  val clean = value.trim().lowercase(Locale.getDefault())
    .replace("_", "-")
    .replace(" ", "-")
  return when {
    clean.contains("pilihan") || clean.contains("multiple") -> "pilihan-ganda"
    clean.contains("esai") || clean.contains("essay") -> "esai"
    clean.contains("isi") || clean.contains("isian") || clean.contains("fill") -> "isi-titik"
    clean.contains("benar") || clean.contains("salah") || clean.contains("true") || clean.contains("false") -> "benar-salah"
    fallbackTypeKey in setOf("pilihan-ganda", "esai", "isi-titik", "benar-salah") -> fallbackTypeKey
    else -> "pilihan-ganda"
  }
}

private fun normalizedPatronMateriId(index: Int, title: String): String {
  return "materi-${index + 1}-${title.trim().hashCode()}"
}

private fun String.aiContentKey(): String {
  return trim().lowercase(Locale.getDefault())
}

private data class CopiedPatronPdf(
  val fileName: String,
  val originalName: String
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

private fun MapelAttendanceSnapshot.latestAttendanceMonth(): YearMonth? =
  students
    .asSequence()
    .flatMap { it.history.asSequence() }
    .mapNotNull { entry -> runCatching { LocalDate.parse(entry.dateIso.take(10)) }.getOrNull() }
    .maxOrNull()
    ?.let { YearMonth.from(it) }

private fun YearMonth.mapelExportSemesterMonths(): List<YearMonth> {
  val firstMonth = if (monthValue in 7..12) 7 else 1
  return (0 until 6).map { plusMonths((firstMonth - monthValue + it).toLong()) }
}

private fun YearMonth.mapelExportAcademicYearMonths(): List<YearMonth> {
  val first = if (monthValue >= 7) YearMonth.of(year, 7) else YearMonth.of(year - 1, 7)
  return (0 until 12).map { first.plusMonths(it.toLong()) }
}

private fun YearMonth.mapelExportMonthTitle(): String =
  DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID")).format(atDay(1))

private fun PatronLearningMaterial.normalizedBlocks(): List<PatronLearningBlock> {
  if (blocks.isNotEmpty()) return blocks.map { it.asUnifiedTextBlock() }
  val migrated = mutableListOf<PatronLearningBlock>()
  if (note.isNotBlank()) {
    migrated += PatronLearningBlock(
      id = "legacy-note",
      type = PatronLearningBlockType.Text.key,
      text = note
    )
  }
  if (pdfFileName.isNotBlank()) {
    migrated += PatronLearningBlock(
      id = "legacy-pdf",
      type = PatronLearningBlockType.Pdf.key,
      fileName = pdfFileName,
      originalName = pdfOriginalName
    )
  }
  return migrated
}

private fun PatronLearningBlock.asUnifiedTextBlock(): PatronLearningBlock {
  if (type != PatronLearningBlockType.List.key) return this
  val hasListSpan = spans.any { it.list }
  return copy(
    type = PatronLearningBlockType.Text.key,
    spans = if (hasListSpan || text.isBlank()) {
      spans
    } else {
      spans + PatronTextSpan(start = 0, end = text.length, list = true)
    }
  )
}

private fun loadPatronLearningBlocks(array: JSONArray?): List<PatronLearningBlock> {
  if (array == null) return emptyList()
  return buildList {
    for (index in 0 until array.length()) {
      val row = array.optJSONObject(index) ?: continue
      val type = row.optString("type").ifBlank { PatronLearningBlockType.Text.key }
      add(
        PatronLearningBlock(
          id = row.optString("id").ifBlank { "block-$index" },
          type = type,
          text = row.optString("text"),
          fileName = row.optString("fileName"),
          originalName = row.optString("originalName"),
          spans = loadPatronTextSpans(row.optJSONArray("spans"), row.optString("text").length)
        )
      )
    }
  }
}

private fun loadPatronTextSpans(
  array: JSONArray?,
  textLength: Int
): List<PatronTextSpan> {
  if (array == null) return emptyList()
  return buildList {
    for (index in 0 until array.length()) {
      val row = array.optJSONObject(index) ?: continue
      add(
        PatronTextSpan(
          start = row.optInt("start", 0),
          end = row.optInt("end", 0),
          bold = row.optBoolean("bold", false),
          italic = row.optBoolean("italic", false),
          underline = row.optBoolean("underline", false),
          list = row.optBoolean("list", false)
        )
      )
    }
  }.normalizedPatronTextSpans(textLength)
}

private fun loadPatronLearningMaterials(
  context: Context,
  subjectId: String
): Map<String, PatronLearningMaterial> {
  val rawJson = context.getSharedPreferences("patron_learning_materials", Context.MODE_PRIVATE)
    .getString(subjectId, "{}")
    .orEmpty()
  return runCatching {
    val root = JSONObject(rawJson.ifBlank { "{}" })
    buildMap {
      val keys = root.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val row = root.optJSONObject(key) ?: continue
        put(
          key,
          PatronLearningMaterial(
            note = row.optString("note"),
            pdfFileName = row.optString("pdfFileName"),
            pdfOriginalName = row.optString("pdfOriginalName"),
            blocks = loadPatronLearningBlocks(row.optJSONArray("blocks")),
            updatedAt = row.optLong("updatedAt", 0L)
          )
        )
      }
    }
  }.getOrDefault(emptyMap())
}

private fun savePatronLearningMaterials(
  context: Context,
  subjectId: String,
  materials: Map<String, PatronLearningMaterial>
) {
  val root = JSONObject()
  materials
    .filterValues { it.note.isNotBlank() || it.pdfFileName.isNotBlank() || it.normalizedBlocks().isNotEmpty() }
    .forEach { (itemId, material) ->
      root.put(
        itemId,
        JSONObject().apply {
          put("note", material.note)
          put("pdfFileName", material.pdfFileName)
          put("pdfOriginalName", material.pdfOriginalName)
          put(
            "blocks",
            JSONArray().apply {
              material.normalizedBlocks().forEach { block ->
                put(
                  JSONObject().apply {
                    put("id", block.id)
                    put("type", block.type)
                    put("text", block.text)
                    put("fileName", block.fileName)
                    put("originalName", block.originalName)
                    put(
                      "spans",
                      JSONArray().apply {
                        block.spans.normalizedPatronTextSpans(block.text.length).forEach { span ->
                          put(
                            JSONObject().apply {
                              put("start", span.start)
                              put("end", span.end)
                              put("bold", span.bold)
                              put("italic", span.italic)
                              put("underline", span.underline)
                              put("list", span.list)
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
          put("updatedAt", material.updatedAt)
        }
      )
    }
  context.getSharedPreferences("patron_learning_materials", Context.MODE_PRIVATE)
    .edit()
    .putString(subjectId, root.toString())
    .apply()
}

private fun copyPatronPdfToInternalStorage(
  context: Context,
  subjectId: String,
  itemId: String,
  uri: Uri
): CopiedPatronPdf {
  val originalName = context.resolveDisplayName(uri).ifBlank { "bahan_ajar.pdf" }
  val safeName = originalName
    .replace(Regex("[^A-Za-z0-9._-]+"), "_")
    .trim('_')
    .ifBlank { "bahan_ajar.pdf" }
    .let { if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf" }
  val fileName = "${System.currentTimeMillis()}_$safeName"
  val target = patronPdfFile(context, subjectId, itemId, fileName)
  target.parentFile?.mkdirs()
  context.contentResolver.openInputStream(uri)?.use { input ->
    target.outputStream().use { output -> input.copyTo(output) }
  } ?: error("File PDF tidak bisa dibuka.")
  return CopiedPatronPdf(fileName = fileName, originalName = originalName)
}

private fun copyPatronImageToInternalStorage(
  context: Context,
  subjectId: String,
  itemId: String,
  uri: Uri
): CopiedPatronPdf {
  val originalName = context.resolveDisplayName(uri).ifBlank { "gambar_bahan_ajar" }
  val safeName = originalName
    .replace(Regex("[^A-Za-z0-9._-]+"), "_")
    .trim('_')
    .ifBlank { "gambar_bahan_ajar" }
  val fileName = "${System.currentTimeMillis()}_$safeName"
  val target = patronLearningFile(context, subjectId, itemId, "images", fileName)
  target.parentFile?.mkdirs()
  context.contentResolver.openInputStream(uri)?.use { input ->
    target.outputStream().use { output -> input.copyTo(output) }
  } ?: error("File gambar tidak bisa dibuka.")
  return CopiedPatronPdf(fileName = fileName, originalName = originalName)
}

private fun Context.resolveDisplayName(uri: Uri): String {
  return runCatching {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) {
        cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)).orEmpty()
      } else {
        ""
      }
    }.orEmpty()
  }.getOrDefault("").ifBlank {
    uri.lastPathSegment?.substringAfterLast('/').orEmpty()
  }
}

private fun patronPdfFile(
  context: Context,
  subjectId: String,
  itemId: String,
  fileName: String
): File {
  val safeSubject = subjectId.replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "subject" }
  val safeItem = itemId.replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "materi" }
  return File(File(File(context.filesDir, "patron_learning_pdfs"), safeSubject), safeItem).resolve(fileName)
}

private fun patronLearningFile(
  context: Context,
  subjectId: String,
  itemId: String,
  folder: String,
  fileName: String
): File {
  val safeSubject = subjectId.replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "subject" }
  val safeItem = itemId.replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "materi" }
  val safeFolder = folder.replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "files" }
  return File(File(File(File(context.filesDir, "patron_learning_files"), safeSubject), safeItem), safeFolder).resolve(fileName)
}

private fun patronLearningBlockFile(
  context: Context,
  subjectId: String,
  itemId: String,
  block: PatronLearningBlock
): File =
  if (block.type == PatronLearningBlockType.Pdf.key) {
    patronPdfFile(context, subjectId, itemId, block.fileName)
  } else {
    patronLearningFile(context, subjectId, itemId, "images", block.fileName)
  }

private fun deletePatronLearningBlockFile(
  context: Context,
  subjectId: String,
  itemId: String,
  block: PatronLearningBlock
) {
  runCatching { patronLearningBlockFile(context, subjectId, itemId, block).delete() }
}

private fun openPatronPdf(context: Context, file: File) {
  val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  val intent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, "application/pdf")
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    clipData = ClipData.newUri(context.contentResolver, file.name, uri)
  }
  try {
    context.startActivity(Intent.createChooser(intent, "Buka PDF bahan ajar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  } catch (_: ActivityNotFoundException) {
    Toast.makeText(context, "Tidak ada aplikasi pembuka PDF di perangkat ini.", Toast.LENGTH_LONG).show()
  } catch (error: Exception) {
    Toast.makeText(context, error.message ?: "PDF belum bisa dibuka di perangkat ini.", Toast.LENGTH_LONG).show()
  }
}

private fun deletePatronPdf(
  context: Context,
  subjectId: String,
  itemId: String,
  fileName: String
) {
  runCatching { patronPdfFile(context, subjectId, itemId, fileName).delete() }
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

@Composable
private fun formatAttendanceDate(dateIso: String): String {
  return runCatching {
    formatDateForLanguage(LocalDate.parse(dateIso), "dd MMM yyyy", LocalAppLanguage.current)
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
      contentDescription = t("Buka sidebar"),
      onClick = onMenuClick
    )
    Text(
      text = t(title),
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
  onUndoClick: () -> Unit = {},
  actions: (@Composable () -> Unit)? = null
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
      contentDescription = t("Kembali ke daftar mapel"),
      onClick = onBackClick
    )
    Text(
      text = t(title),
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
    if (actions != null) {
      actions()
    } else if (showUndo) {
      MapelTopButton(
        icon = Icons.AutoMirrored.Outlined.Undo,
        contentDescription = t("Batalkan hapus"),
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
  enabled: Boolean = true,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(42.dp)
      .background(
        if (enabled) CardBackground.copy(alpha = 0.86f) else SoftPanel.copy(alpha = 0.72f),
        androidx.compose.foundation.shape.CircleShape
      )
      .border(1.dp, CardBorder, androidx.compose.foundation.shape.CircleShape)
      .clickable(enabled = enabled, onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = t(contentDescription),
      tint = if (enabled) PrimaryBlueDark else SubtleInk.copy(alpha = 0.55f)
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
      text = t(label),
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
