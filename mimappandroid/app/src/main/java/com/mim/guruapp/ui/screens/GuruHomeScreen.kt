package com.mim.guruapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mim.guruapp.GuruSidebarParent
import com.mim.guruapp.GuruSidebarDestination
import com.mim.guruapp.AttendanceSaveOutcome
import com.mim.guruapp.LeaveRequestSaveOutcome
import com.mim.guruapp.PatronMateriSaveOutcome
import com.mim.guruapp.ScoreSaveOutcome
import com.mim.guruapp.ProfileSaveOutcome
import com.mim.guruapp.SantriSaveOutcome
import com.mim.guruapp.MutabaahSaveOutcome
import com.mim.guruapp.MonthlyExtracurricularSaveOutcome
import com.mim.guruapp.MonthlyReportSaveOutcome
import com.mim.guruapp.UtsReportSaveOutcome
import com.mim.guruapp.SampleDataFactory
import com.mim.guruapp.data.model.AttendanceHistoryEntry
import com.mim.guruapp.data.model.AttendanceApprovalRequest
import com.mim.guruapp.data.model.AppNotification
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.DashboardPayload
import com.mim.guruapp.data.model.InputAbsensiNavigationTarget
import com.mim.guruapp.data.model.LeaveRequestSnapshot
import com.mim.guruapp.data.model.MapelAttendanceSnapshot
import com.mim.guruapp.data.model.MapelPatronMateriSnapshot
import com.mim.guruapp.data.model.MapelScoreSnapshot
import com.mim.guruapp.data.model.MutabaahSnapshot
import com.mim.guruapp.data.model.MonthlyExtracurricularReport
import com.mim.guruapp.data.model.MonthlyReportItem
import com.mim.guruapp.data.model.PatronMateriItem
import com.mim.guruapp.data.model.ScoreStudent
import com.mim.guruapp.data.model.SubjectOverview
import com.mim.guruapp.data.model.SyncBannerState
import com.mim.guruapp.data.model.TeacherOption
import com.mim.guruapp.data.model.TeachingReminderSettings
import com.mim.guruapp.data.model.UtsReportOverride
import com.mim.guruapp.data.model.WaliSantriProfile
import com.mim.guruapp.ui.components.AgendaCard
import com.mim.guruapp.ui.components.AttendanceApprovalPopup
import com.mim.guruapp.ui.components.AvailableMapelPanel
import com.mim.guruapp.ui.components.BottomNavBar
import com.mim.guruapp.ui.components.CalendarScreen
import com.mim.guruapp.ui.components.DashboardScreenScaffold
import com.mim.guruapp.ui.components.EditProfileScreen
import com.mim.guruapp.ui.components.EmptyPlaceholderCard
import com.mim.guruapp.ui.components.HomeHeroCard
import com.mim.guruapp.ui.components.InputAbsensiScreen
import com.mim.guruapp.ui.components.InputNilaiScreen
import com.mim.guruapp.ui.components.LaporanBulananScreen
import com.mim.guruapp.ui.components.LaporanUtsScreen
import com.mim.guruapp.ui.components.MapelToolbarCard
import com.mim.guruapp.ui.components.MapelScreen
import com.mim.guruapp.ui.components.MutabaahScreen
import com.mim.guruapp.ui.components.NotificationPopup
import com.mim.guruapp.ui.components.NoticeCard
import com.mim.guruapp.ui.components.PerizinanScreen
import com.mim.guruapp.ui.components.PlaceholderPanel
import com.mim.guruapp.ui.components.ProfileInfoRow
import com.mim.guruapp.ui.components.QuickStatsRow
import com.mim.guruapp.ui.components.SantriScreen
import com.mim.guruapp.ui.components.Sidebar
import com.mim.guruapp.ui.components.SidebarScrim
import com.mim.guruapp.ui.components.SyncBannerCard
import com.mim.guruapp.ui.components.TeachingScheduleScreen
import com.mim.guruapp.ui.components.WebMapelCard
import com.mim.guruapp.ui.components.buildGuruBottomNavItems
import com.mim.guruapp.ui.components.buildGuruSidebarContent
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.HighlightCard
import com.mim.guruapp.ui.theme.MimGuruTheme
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SuccessTint
import com.mim.guruapp.ui.theme.WarmAccent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val SidebarWidth = 304.dp

private data class GuruHomeContentTarget(
  val destination: GuruSidebarDestination,
  val isCalendarOpen: Boolean
)

private fun GuruHomeContentTarget.transitionOrder(): Int {
  if (destination == GuruSidebarDestination.Dashboard && isCalendarOpen) return 1
  return when (destination) {
    GuruSidebarDestination.Dashboard -> 0
    GuruSidebarDestination.Tugas -> 2
    GuruSidebarDestination.Mapel -> 2
    GuruSidebarDestination.Jadwal -> 3
    GuruSidebarDestination.InputNilai,
    GuruSidebarDestination.InputAbsensi -> 4
    GuruSidebarDestination.Perizinan,
    GuruSidebarDestination.Profil,
    GuruSidebarDestination.Pesan,
    GuruSidebarDestination.Notifikasi -> 5
    else -> 6
  }
}

@Composable
fun GuruHomeScreen(
  dashboard: DashboardPayload,
  syncBanner: SyncBannerState,
  selectedDestination: GuruSidebarDestination,
  isCalendarScreenOpen: Boolean,
  isNotificationPopupOpen: Boolean,
  selectedCalendarDateIso: String,
  teachingReminderSettings: TeachingReminderSettings,
  pendingInputAbsensiTarget: InputAbsensiNavigationTarget?,
  isSidebarOpen: Boolean,
  expandedSidebarParent: GuruSidebarParent?,
  isClaimSectionVisible: Boolean,
  selectedClaimSubjectIds: Set<String>,
  onToggleSidebar: () -> Unit,
  onCloseSidebar: () -> Unit,
  onToggleSidebarParent: (GuruSidebarParent) -> Unit,
  onSelectDestination: (GuruSidebarDestination) -> Unit,
  onOpenCalendarScreen: () -> Unit,
  onCloseCalendarScreen: () -> Unit,
  onOpenNotificationPopup: () -> Unit,
  onCloseNotificationPopup: () -> Unit,
  onMarkNotificationAsRead: (String) -> Unit,
  onMarkAllNotificationsAsRead: () -> Unit,
  onUpdateTeachingReminderSettings: (TeachingReminderSettings) -> Unit,
  onOpenInputAbsensiTarget: (InputAbsensiNavigationTarget) -> Unit,
  onConsumePendingInputAbsensiTarget: () -> Unit,
  onLoadAttendanceApprovalRequest: suspend (String) -> AttendanceApprovalRequest?,
  onReviewAttendanceApproval: suspend (String, Boolean, String) -> AttendanceSaveOutcome,
  onSelectCalendarDate: (String) -> Unit,
  onToggleClaimSection: () -> Unit,
  onToggleClaimSubject: (String) -> Unit,
  onClearClaimSelection: () -> Unit,
  onClaimSelectedSubjects: () -> Unit,
  onLoadMapelAttendance: suspend (String, SubjectOverview) -> MapelAttendanceSnapshot?,
  onSaveMapelAttendance: suspend (String, SubjectOverview, String, List<AttendanceHistoryEntry>) -> AttendanceSaveOutcome,
  onSaveMapelAttendanceBatch: suspend (String, SubjectOverview, Map<String, List<AttendanceHistoryEntry>>, String, String) -> AttendanceSaveOutcome,
  onDeleteMapelAttendance: suspend (String, SubjectOverview, List<String>) -> AttendanceSaveOutcome,
  onSendMapelAttendanceDelegation: suspend (String, SubjectOverview, String, List<String>, String, String) -> AttendanceSaveOutcome,
  onLoadSubstituteTeacherContext: suspend (TeacherOption, String) -> Pair<List<SubjectOverview>, List<CalendarEvent>>,
  onLoadDelegatedAttendanceContext: suspend (String) -> Pair<List<SubjectOverview>, List<CalendarEvent>>,
  onLoadMapelScores: suspend (String, SubjectOverview) -> MapelScoreSnapshot?,
  onSaveMapelScores: suspend (String, SubjectOverview, ScoreStudent) -> ScoreSaveOutcome,
  onSaveMapelScoresBatch: suspend (String, SubjectOverview, List<ScoreStudent>) -> ScoreSaveOutcome,
  onLoadMapelPatronMateri: suspend (String, SubjectOverview) -> MapelPatronMateriSnapshot?,
  onSaveMapelPatronMateri: suspend (String, SubjectOverview, List<PatronMateriItem>) -> PatronMateriSaveOutcome,
  onSaveProfile: suspend (com.mim.guruapp.data.model.GuruProfile) -> ProfileSaveOutcome,
  onSaveSantri: suspend (WaliSantriProfile) -> SantriSaveOutcome,
  onSaveMonthlyReport: suspend (MonthlyReportItem) -> MonthlyReportSaveOutcome,
  onSaveMonthlyExtracurricularReports: suspend (List<MonthlyExtracurricularReport>) -> MonthlyExtracurricularSaveOutcome,
  onSaveUtsReportOverride: suspend (UtsReportOverride) -> UtsReportSaveOutcome,
  onLoadMutabaah: suspend (LocalDate) -> MutabaahSnapshot?,
  onSaveMutabaahStatus: suspend (String, String, Boolean) -> MutabaahSaveOutcome,
  onSaveMutabaahStatuses: suspend (List<String>, String, String) -> MutabaahSaveOutcome,
  onLoadLeaveRequests: suspend () -> LeaveRequestSnapshot?,
  onSubmitLeaveRequest: suspend (String, String, String) -> LeaveRequestSaveOutcome,
  onDeleteLeaveRequest: suspend (String) -> LeaveRequestSaveOutcome,
  onRefreshClick: () -> Unit,
  onLogoutClick: () -> Unit
) {
  val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    .withZone(ZoneId.systemDefault())
  val scope = rememberCoroutineScope()
  val selectedCalendarDate = runCatching { LocalDate.parse(selectedCalendarDateIso) }
    .getOrDefault(LocalDate.now())
  val bottomNavItems = buildGuruBottomNavItems()
  val sidebarWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { SidebarWidth.toPx() }
  val sidebarOffsetPx = remember { Animatable(-sidebarWidthPx) }
  val sidebarGestureScope = rememberCoroutineScope()
  val selectedBottomNavDestination = when (selectedDestination) {
    GuruSidebarDestination.Dashboard -> GuruSidebarDestination.Dashboard
    GuruSidebarDestination.Mapel -> GuruSidebarDestination.Mapel
    GuruSidebarDestination.Jadwal -> GuruSidebarDestination.Jadwal
    GuruSidebarDestination.InputNilai,
    GuruSidebarDestination.InputAbsensi -> GuruSidebarDestination.InputAbsensi
    GuruSidebarDestination.Profil,
    GuruSidebarDestination.Pesan,
    GuruSidebarDestination.Notifikasi -> GuruSidebarDestination.Profil
    else -> null
  }
  val contentDestination = when (selectedDestination) {
    GuruSidebarDestination.InputNilai,
    GuruSidebarDestination.InputAbsensi -> GuruSidebarDestination.InputAbsensi
    else -> selectedDestination
  }
  LaunchedEffect(sidebarWidthPx) {
    if (sidebarOffsetPx.value == 0f || sidebarOffsetPx.value == -sidebarWidthPx) return@LaunchedEffect
    sidebarOffsetPx.snapTo(if (isSidebarOpen) 0f else -sidebarWidthPx)
  }
  LaunchedEffect(isSidebarOpen, sidebarWidthPx) {
    val target = if (isSidebarOpen) 0f else -sidebarWidthPx
    sidebarOffsetPx.animateTo(
      targetValue = target,
      animationSpec = tween(durationMillis = 260)
    )
  }
  val sidebarProgress = if (sidebarWidthPx == 0f) 0f else 1f - ((-sidebarOffsetPx.value) / sidebarWidthPx).coerceIn(0f, 1f)
  var isMapelDetailMode by androidx.compose.runtime.remember { mutableStateOf(false) }
  var isLaporanBulananDetailMode by androidx.compose.runtime.remember { mutableStateOf(false) }
  var isLaporanUtsDetailMode by androidx.compose.runtime.remember { mutableStateOf(false) }
  val isFullScreenSidebarSwipeEnabled = when (selectedDestination) {
    GuruSidebarDestination.Dashboard -> !isCalendarScreenOpen
    GuruSidebarDestination.Tugas,
    GuruSidebarDestination.Jadwal,
    GuruSidebarDestination.Perizinan,
    GuruSidebarDestination.LaporanBulanan,
    GuruSidebarDestination.LaporanUTS,
    GuruSidebarDestination.Profil,
    GuruSidebarDestination.Santri -> true
    GuruSidebarDestination.Mapel -> !isMapelDetailMode
    else -> false
  }
  var isSidebarDragging by remember { mutableStateOf(false) }
  var sidebarDragStartOffset by remember { mutableStateOf(-sidebarWidthPx) }
  var sidebarDragAmount by remember { mutableStateOf(0f) }
  var selectedApprovalRequest by remember { mutableStateOf<AttendanceApprovalRequest?>(null) }
  var isLoadingApprovalRequest by remember { mutableStateOf(false) }
  var isReviewingApprovalRequest by remember { mutableStateOf(false) }
  var loadingApprovalRequestId by remember { mutableStateOf("") }
  val shouldHandleHomeBack = isSidebarOpen ||
    isLoadingApprovalRequest ||
    selectedApprovalRequest != null ||
    isNotificationPopupOpen ||
    selectedDestination != GuruSidebarDestination.Dashboard

  BackHandler(enabled = shouldHandleHomeBack) {
    when {
      isSidebarOpen -> onCloseSidebar()
      isLoadingApprovalRequest -> {
        isLoadingApprovalRequest = false
        selectedApprovalRequest = null
        loadingApprovalRequestId = ""
      }
      selectedApprovalRequest != null -> {
        selectedApprovalRequest = null
        loadingApprovalRequestId = ""
      }
      isNotificationPopupOpen -> onCloseNotificationPopup()
      selectedDestination != GuruSidebarDestination.Dashboard ->
        onSelectDestination(GuruSidebarDestination.Dashboard)
    }
  }

  val handleNotificationClick: (AppNotification) -> Unit = { notification ->
    onMarkNotificationAsRead(notification.id)
    when (notification.actionType) {
      "attendance_approval" -> {
        val submissionId = notification.actionId.trim()
        if (submissionId.isNotBlank()) {
          loadingApprovalRequestId = submissionId
          isLoadingApprovalRequest = true
          onCloseNotificationPopup()
          scope.launch {
            val request = onLoadAttendanceApprovalRequest(submissionId)
            if (loadingApprovalRequestId == submissionId) {
              selectedApprovalRequest = request
            }
            isLoadingApprovalRequest = false
          }
        }
      }
      "open_input_absensi" -> {
        val target = InputAbsensiNavigationTarget(
          requestId = "${notification.id}|${System.currentTimeMillis()}",
          dateIso = notification.actionDateIso.ifBlank { LocalDate.now().toString() },
          distribusiId = notification.actionDistribusiId,
          lessonSlotId = notification.actionLessonSlotId
        )
        onCloseNotificationPopup()
        onOpenInputAbsensiTarget(target)
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(isFullScreenSidebarSwipeEnabled, isSidebarOpen, sidebarWidthPx) {
        if (sidebarWidthPx <= 0f || (!isFullScreenSidebarSwipeEnabled && !isSidebarOpen)) {
          return@pointerInput
        }
        detectHorizontalDragGestures(
          onDragStart = {
            isSidebarDragging = true
            sidebarDragStartOffset = sidebarOffsetPx.value
            sidebarDragAmount = 0f
          },
          onDragCancel = {
            val target = if (isSidebarOpen) 0f else -sidebarWidthPx
            isSidebarDragging = false
            sidebarDragAmount = 0f
            sidebarGestureScope.launch {
              sidebarOffsetPx.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
              )
            }
          },
          onDragEnd = {
            val currentOffset = sidebarOffsetPx.value
            val shouldOpen = when {
              sidebarDragAmount > sidebarWidthPx * 0.24f -> true
              sidebarDragAmount < -sidebarWidthPx * 0.24f -> false
              else -> currentOffset > -sidebarWidthPx * 0.56f
            }
            val target = if (shouldOpen) 0f else -sidebarWidthPx
            isSidebarDragging = false
            sidebarDragAmount = 0f
            sidebarGestureScope.launch {
              sidebarOffsetPx.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
              )
              if (shouldOpen && !isSidebarOpen) {
                onToggleSidebar()
              } else if (!shouldOpen && isSidebarOpen) {
                onCloseSidebar()
              }
            }
          },
          onHorizontalDrag = { change, dragAmount ->
            sidebarDragAmount += dragAmount
            val nextOffset = (sidebarDragStartOffset + sidebarDragAmount).coerceIn(-sidebarWidthPx, 0f)
            change.consume()
            sidebarGestureScope.launch {
              sidebarOffsetPx.snapTo(nextOffset)
            }
          }
        )
      }
      .background(AppBackground)
  ) {
    AnimatedContent(
      targetState = GuruHomeContentTarget(
        destination = contentDestination,
        isCalendarOpen = selectedDestination == GuruSidebarDestination.Dashboard && isCalendarScreenOpen
      ),
      transitionSpec = {
        val forward = targetState.transitionOrder() >= initialState.transitionOrder()
        fadeIn(animationSpec = tween(180)) +
          slideInHorizontally(animationSpec = tween(260)) { width -> if (forward) width / 10 else -width / 10 } togetherWith
          fadeOut(animationSpec = tween(150)) +
          slideOutHorizontally(animationSpec = tween(220)) { width -> if (forward) -width / 12 else width / 12 }
      },
      modifier = Modifier.fillMaxSize(),
      label = "guru-home-content-transition"
    ) { target ->
      val targetDestination = target.destination
      if (targetDestination == GuruSidebarDestination.Dashboard) {
      if (target.isCalendarOpen) {
        CalendarScreen(
          selectedDate = selectedCalendarDate,
          events = dashboard.calendarEvents,
          isRefreshing = syncBanner.isSyncing,
          onSelectDate = { onSelectCalendarDate(it.toString()) },
          onJumpToToday = { onSelectCalendarDate(LocalDate.now().toString()) },
          onRefresh = onRefreshClick,
          onBackClick = onCloseCalendarScreen,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        DashboardScreenScaffold(
          currentDate = selectedCalendarDate,
          notificationCount = dashboard.unreadNotificationCount,
          isRefreshing = syncBanner.isSyncing,
          onMenuClick = onToggleSidebar,
          onDateClick = onOpenCalendarScreen,
          onNotificationClick = onOpenNotificationPopup,
          onRefresh = onRefreshClick,
          categories = dashboard.categories,
          tasks = dashboard.ongoingTasks,
          calendarEvents = dashboard.calendarEvents,
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
        )
      }
    } else if (targetDestination == GuruSidebarDestination.Jadwal) {
      TeachingScheduleScreen(
        selectedDate = selectedCalendarDate,
        events = dashboard.teachingScheduleEvents,
        reminderSettings = teachingReminderSettings,
        isRefreshing = syncBanner.isSyncing,
        onSelectDate = { onSelectCalendarDate(it.toString()) },
        onJumpToToday = { onSelectCalendarDate(LocalDate.now().toString()) },
        onRefresh = onRefreshClick,
        onMenuClick = onToggleSidebar,
        onReminderSettingsChange = onUpdateTeachingReminderSettings,
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.Tugas) {
      MutabaahScreen(
        selectedDate = selectedCalendarDate,
        snapshot = dashboard.mutabaahSnapshot,
        teachingScheduleEvents = dashboard.teachingScheduleEvents,
        leaveRequests = dashboard.leaveRequestSnapshot.requests,
        isRefreshing = syncBanner.isSyncing,
        onSelectDate = { onSelectCalendarDate(it.toString()) },
        onJumpToToday = { onSelectCalendarDate(LocalDate.now().toString()) },
        onRefresh = onRefreshClick,
        onMenuClick = onToggleSidebar,
        onLoadSnapshot = onLoadMutabaah,
        onToggleTask = onSaveMutabaahStatus,
        onToggleTasks = onSaveMutabaahStatuses,
        modifier = Modifier.fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.Perizinan) {
      PerizinanScreen(
        snapshot = dashboard.leaveRequestSnapshot,
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onLoadSnapshot = onLoadLeaveRequests,
        onSubmitRequest = onSubmitLeaveRequest,
        onDeleteRequest = onDeleteLeaveRequest,
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.Profil) {
      EditProfileScreen(
        profile = dashboard.profile,
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onSaveClick = onSaveProfile,
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.Santri) {
      SantriScreen(
        waliSantriSnapshot = dashboard.waliSantriSnapshot,
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onSaveSantri = onSaveSantri,
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.LaporanBulanan) {
      LaporanBulananScreen(
        waliSantriSnapshot = dashboard.waliSantriSnapshot,
        monthlyReportSnapshot = dashboard.monthlyReportSnapshot,
        profile = dashboard.profile,
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onSaveReport = onSaveMonthlyReport,
        onSaveExtracurricularReports = onSaveMonthlyExtracurricularReports,
        onDetailModeChange = { isLaporanBulananDetailMode = it },
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.LaporanUTS) {
      LaporanUtsScreen(
        waliSantriSnapshot = dashboard.waliSantriSnapshot,
        utsReportSnapshot = dashboard.utsReportSnapshot,
        profile = dashboard.profile,
        isRefreshing = syncBanner.isSyncing,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onSaveOverride = onSaveUtsReportOverride,
        onDetailModeChange = { isLaporanUtsDetailMode = it },
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.InputAbsensi) {
      InputSwipePager(
        selectedDestination = when (selectedDestination) {
          GuruSidebarDestination.InputNilai -> GuruSidebarDestination.InputNilai
          else -> GuruSidebarDestination.InputAbsensi
        },
        onSelectDestination = onSelectDestination,
        subjects = dashboard.subjects,
        scoreSnapshots = dashboard.scoreSnapshots,
        attendanceSnapshots = dashboard.attendanceSnapshots,
        patronMateriSnapshots = dashboard.patronMateriSnapshots,
        teachingScheduleEvents = dashboard.teachingScheduleEvents,
        substituteTeacherOptions = dashboard.substituteTeacherOptions,
        sourceTeacherOptions = dashboard.sourceTeacherOptions,
        syncBanner = syncBanner,
        onMenuClick = onToggleSidebar,
        onRefresh = onRefreshClick,
        onLoadScores = onLoadMapelScores,
        onSaveScoresBatch = onSaveMapelScoresBatch,
        onLoadAttendance = onLoadMapelAttendance,
        onLoadPatronMateri = onLoadMapelPatronMateri,
        onSaveAttendanceBatch = onSaveMapelAttendanceBatch,
        onSaveAttendance = onSaveMapelAttendance,
        onSendSubstituteDelegation = onSendMapelAttendanceDelegation,
        onLoadSubstituteTeacherContext = onLoadSubstituteTeacherContext,
        onLoadDelegatedAttendanceContext = onLoadDelegatedAttendanceContext,
        inputAbsensiLaunchTarget = pendingInputAbsensiTarget,
        onInputAbsensiLaunchTargetConsumed = onConsumePendingInputAbsensiTarget,
        modifier = Modifier
          .fillMaxSize()
      )
    } else if (targetDestination == GuruSidebarDestination.Mapel) {
      MapelScreen(
        subjects = dashboard.subjects,
        syncBanner = syncBanner,
        isClaimSectionVisible = isClaimSectionVisible,
        availableSubjects = dashboard.availableSubjects,
        selectedClaimSubjectIds = selectedClaimSubjectIds,
        onToggleClaimSection = onToggleClaimSection,
        onToggleClaimSubject = onToggleClaimSubject,
        onClearClaimSelection = onClearClaimSelection,
        onClaimSelectedSubjects = onClaimSelectedSubjects,
        onLoadAttendance = onLoadMapelAttendance,
        onSaveAttendance = onSaveMapelAttendance,
        onDeleteAttendance = onDeleteMapelAttendance,
        onLoadScores = onLoadMapelScores,
        onSaveScores = onSaveMapelScores,
        onSaveScoresBatch = onSaveMapelScoresBatch,
        onLoadPatronMateri = onLoadMapelPatronMateri,
        onSavePatronMateri = onSaveMapelPatronMateri,
        isRefreshing = syncBanner.isSyncing,
        onRefresh = onRefreshClick,
        onDetailModeChange = { isMapelDetailMode = it },
        onMenuClick = onToggleSidebar,
        modifier = Modifier
          .fillMaxSize()
      )
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 124.dp)
      ) {
        item {
          GuruHomeTopBar(
            title = targetDestination.title,
            onMenuClick = onToggleSidebar,
            modifier = Modifier.padding(top = 18.dp)
          )
        }

        when (targetDestination) {

        GuruSidebarDestination.Pesan -> {
          item {
            HomeHeroCard(
              teacherName = dashboard.teacherName,
              teacherRole = dashboard.teacherRole,
              message = "Inbox pesan guru akan ditempatkan di section profile sesuai struktur drawer."
            )
          }
          item {
            PlaceholderPanel(
              title = "Pesan",
              subtitle = "Badge dan item navigasinya sudah aktif di sidebar. Konten native pesan bisa kita bangun pada langkah berikutnya."
            ) {
              EmptyPlaceholderCard("Belum ada screen native untuk pesan.")
            }
          }
        }

        else -> {
          item {
            HomeHeroCard(
              teacherName = dashboard.teacherName,
              teacherRole = dashboard.teacherRole,
              message = "Menu ${targetDestination.title} sudah ditempatkan di sidebar sesuai struktur web dan siap dihubungkan ke halaman native berikutnya."
            )
          }
          item {
            PlaceholderPanel(
              title = targetDestination.title,
              subtitle = "Struktur navigasi sudah sama dengan panel guru web. Konten halaman ini bisa kita isi pada tahap berikutnya."
            ) {
              EmptyPlaceholderCard("Halaman ${targetDestination.title} sedang disiapkan sebagai screen native Android.")
            }
          }
        }
        }

        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = onRefreshClick,
              modifier = Modifier.weight(1f)
            ) {
              Text("Refresh Data")
            }
            OutlinedButton(
              onClick = onLogoutClick,
              modifier = Modifier.weight(1f)
            ) {
              Text("Logout")
            }
          }
        }

        item {
          Text(
            text = "Terakhir sync: ${formatter.format(Instant.ofEpochMilli(dashboard.lastSuccessfulSyncAt))}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 22.dp),
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
    }

    AnimatedVisibility(
      visible = !(selectedDestination == GuruSidebarDestination.Mapel && isMapelDetailMode) &&
        !(selectedDestination == GuruSidebarDestination.LaporanBulanan && isLaporanBulananDetailMode) &&
        !(selectedDestination == GuruSidebarDestination.LaporanUTS && isLaporanUtsDetailMode),
      enter = fadeIn(animationSpec = tween(180)) + slideInVertically(animationSpec = tween(220)) { it / 2 },
      exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(animationSpec = tween(200)) { it / 2 },
      modifier = Modifier.align(Alignment.BottomCenter)
    ) {
      BottomNavBar(
        items = bottomNavItems,
        selectedDestination = selectedBottomNavDestination,
        onSelect = onSelectDestination,
        modifier = Modifier
          .navigationBarsPadding()
          .padding(horizontal = 18.dp, vertical = 10.dp)
      )
    }

    SidebarScrim(
      progress = sidebarProgress,
      onDismiss = onCloseSidebar
    )

    Sidebar(
      appName = "MIM App",
      content = buildGuruSidebarContent(
        pendingSyncCount = dashboard.pendingSyncCount,
        activeMapelCount = dashboard.subjects.size
      ),
      selectedDestination = selectedDestination,
      expandedParent = expandedSidebarParent,
      onDismiss = onCloseSidebar,
      onToggleParent = onToggleSidebarParent,
      onSelectItem = onSelectDestination,
      onLogout = onLogoutClick,
      modifier = Modifier
        .align(Alignment.CenterStart)
        .offset { IntOffset(sidebarOffsetPx.value.roundToInt(), 0) }
    )

    if (isNotificationPopupOpen) {
      NotificationPopup(
        notifications = dashboard.notifications,
        onDismiss = onCloseNotificationPopup,
        onMarkAllAsRead = onMarkAllNotificationsAsRead,
        onNotificationClick = handleNotificationClick,
        modifier = Modifier.align(Alignment.BottomCenter)
      )
    }

    if (isLoadingApprovalRequest || selectedApprovalRequest != null) {
      AttendanceApprovalPopup(
        request = selectedApprovalRequest,
        isLoading = isLoadingApprovalRequest,
        isSaving = isReviewingApprovalRequest,
        onDismiss = {
          if (!isReviewingApprovalRequest) {
            selectedApprovalRequest = null
            isLoadingApprovalRequest = false
            loadingApprovalRequestId = ""
          }
        },
        onApprove = { reviewerNote ->
          val approvalId = selectedApprovalRequest?.id.orEmpty()
          if (approvalId.isBlank() || isReviewingApprovalRequest) return@AttendanceApprovalPopup
          scope.launch {
            isReviewingApprovalRequest = true
            val result = onReviewAttendanceApproval(approvalId, true, reviewerNote)
            isReviewingApprovalRequest = false
            if (result.success) {
              selectedApprovalRequest = null
              loadingApprovalRequestId = ""
            }
          }
        },
        onReject = { reviewerNote ->
          val approvalId = selectedApprovalRequest?.id.orEmpty()
          if (approvalId.isBlank() || isReviewingApprovalRequest) return@AttendanceApprovalPopup
          scope.launch {
            isReviewingApprovalRequest = true
            val result = onReviewAttendanceApproval(approvalId, false, reviewerNote)
            isReviewingApprovalRequest = false
            if (result.success) {
              selectedApprovalRequest = null
              loadingApprovalRequestId = ""
            }
          }
        },
        modifier = Modifier.align(Alignment.TopCenter)
      )
    }
  }
}

@Composable
private fun InputSwipePager(
  selectedDestination: GuruSidebarDestination,
  onSelectDestination: (GuruSidebarDestination) -> Unit,
  subjects: List<SubjectOverview>,
  scoreSnapshots: List<MapelScoreSnapshot>,
  attendanceSnapshots: List<MapelAttendanceSnapshot>,
  patronMateriSnapshots: List<MapelPatronMateriSnapshot>,
  teachingScheduleEvents: List<CalendarEvent>,
  substituteTeacherOptions: List<TeacherOption>,
  sourceTeacherOptions: List<TeacherOption>,
  syncBanner: SyncBannerState,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadScores: suspend (String, SubjectOverview) -> MapelScoreSnapshot?,
  onSaveScoresBatch: suspend (String, SubjectOverview, List<ScoreStudent>) -> ScoreSaveOutcome,
  onLoadAttendance: suspend (String, SubjectOverview) -> MapelAttendanceSnapshot?,
  onLoadPatronMateri: suspend (String, SubjectOverview) -> MapelPatronMateriSnapshot?,
  onSaveAttendanceBatch: suspend (String, SubjectOverview, Map<String, List<AttendanceHistoryEntry>>, String, String) -> AttendanceSaveOutcome,
  onSaveAttendance: suspend (String, SubjectOverview, String, List<AttendanceHistoryEntry>) -> AttendanceSaveOutcome,
  onSendSubstituteDelegation: suspend (String, SubjectOverview, String, List<String>, String, String) -> AttendanceSaveOutcome,
  onLoadSubstituteTeacherContext: suspend (TeacherOption, String) -> Pair<List<SubjectOverview>, List<CalendarEvent>>,
  onLoadDelegatedAttendanceContext: suspend (String) -> Pair<List<SubjectOverview>, List<CalendarEvent>>,
  inputAbsensiLaunchTarget: InputAbsensiNavigationTarget? = null,
  onInputAbsensiLaunchTargetConsumed: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .clipToBounds()
  ) {
    val widthPx = constraints.maxWidth.toFloat()
    val selectedIndex = inputPagerIndex(selectedDestination)
    val swipeThresholdPx = widthPx * 0.22f
    val pagerOffsetPx = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetPx by remember { mutableStateOf(0f) }
    val baseOffsetPx = -selectedIndex * widthPx
    val visualOffsetPx = if (isDragging) baseOffsetPx + dragOffsetPx else pagerOffsetPx.value

    LaunchedEffect(widthPx, selectedIndex) {
      if (widthPx > 0f && !isDragging) {
        pagerOffsetPx.animateTo(
          targetValue = -selectedIndex * widthPx,
          animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
        )
      }
    }

    Column(modifier = Modifier.fillMaxSize()) {
      InputPagerHeader(
        title = if (selectedIndex == 0) "Input Absensi" else "Input Nilai",
        activeInputPage = selectedIndex,
        onMenuClick = onMenuClick
      )

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .clipToBounds()
          .pointerInput(widthPx, selectedIndex) {
            if (widthPx <= 0f) return@pointerInput
            detectHorizontalDragGestures(
              onDragStart = {
                isDragging = true
                dragOffsetPx = pagerOffsetPx.value - (-selectedIndex * widthPx)
              },
              onDragCancel = {
                val currentOffset = (-selectedIndex * widthPx) + dragOffsetPx
                isDragging = false
                dragOffsetPx = 0f
                scope.launch {
                  pagerOffsetPx.snapTo(currentOffset)
                  pagerOffsetPx.animateTo(
                    targetValue = -selectedIndex * widthPx,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                  )
                }
              },
              onDragEnd = {
                val nextIndex = when {
                  selectedIndex == 1 && dragOffsetPx > swipeThresholdPx -> 0
                  selectedIndex == 0 && dragOffsetPx < -swipeThresholdPx -> 1
                  else -> selectedIndex
                }
                val currentVisualOffset = (-selectedIndex * widthPx) + dragOffsetPx
                val targetOffset = -nextIndex * widthPx
                isDragging = false
                dragOffsetPx = 0f
                scope.launch {
                  pagerOffsetPx.snapTo(currentVisualOffset)
                  pagerOffsetPx.animateTo(
                    targetValue = targetOffset,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                  )
                  if (nextIndex != selectedIndex) {
                    onSelectDestination(inputPagerDestination(nextIndex))
                  }
                }
              },
              onHorizontalDrag = { change, dragAmount ->
                change.consume()
                val nextOffset = dragOffsetPx + dragAmount
                dragOffsetPx = if (selectedIndex == 0) {
                  nextOffset.coerceIn(-widthPx, 0f)
                } else {
                  nextOffset.coerceIn(0f, widthPx)
                }
              }
            )
          }
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(visualOffsetPx.roundToInt(), 0) }
        ) {
          Box(
            modifier = Modifier.fillMaxSize()
          ) {
            InputAbsensiScreen(
              subjects = subjects,
              attendanceSnapshots = attendanceSnapshots,
              patronMateriSnapshots = patronMateriSnapshots,
              teachingScheduleEvents = teachingScheduleEvents,
              substituteTeacherOptions = substituteTeacherOptions,
              sourceTeacherOptions = sourceTeacherOptions,
              syncBanner = syncBanner,
              onMenuClick = onMenuClick,
              onRefresh = onRefresh,
              onLoadAttendance = onLoadAttendance,
              onLoadPatronMateri = onLoadPatronMateri,
              onSaveAttendanceBatch = onSaveAttendanceBatch,
              onSaveAttendance = onSaveAttendance,
              onSendSubstituteDelegation = onSendSubstituteDelegation,
              onLoadSubstituteTeacherContext = onLoadSubstituteTeacherContext,
              onLoadDelegatedAttendanceContext = onLoadDelegatedAttendanceContext,
              launchTarget = inputAbsensiLaunchTarget,
              onLaunchTargetConsumed = onInputAbsensiLaunchTargetConsumed,
              activeInputPage = 0,
              showTopBar = false,
              modifier = Modifier.fillMaxSize()
            )
          }

          Box(
            modifier = Modifier
              .fillMaxSize()
              .offset { IntOffset(widthPx.roundToInt(), 0) }
          ) {
            InputNilaiScreen(
              subjects = subjects,
              scoreSnapshots = scoreSnapshots,
              patronMateriSnapshots = patronMateriSnapshots,
              syncBanner = syncBanner,
              onMenuClick = onMenuClick,
              onRefresh = onRefresh,
              onLoadScores = onLoadScores,
              onLoadPatronMateri = onLoadPatronMateri,
              onSaveScoresBatch = onSaveScoresBatch,
              activeInputPage = 1,
              showTopBar = false,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }
  }
}

private fun inputPagerIndex(destination: GuruSidebarDestination): Int {
  return if (destination == GuruSidebarDestination.InputNilai) 1 else 0
}

private fun inputPagerDestination(index: Int): GuruSidebarDestination {
  return if (index == 0) GuruSidebarDestination.InputAbsensi else GuruSidebarDestination.InputNilai
}

@Composable
private fun InputPagerHeader(
  title: String,
  activeInputPage: Int,
  onMenuClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp, start = 18.dp, end = 18.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .background(Color.White.copy(alpha = 0.86f), CircleShape)
        .border(1.dp, CardBorder, CircleShape)
        .clickable(onClick = onMenuClick),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Outlined.Menu,
        contentDescription = "Buka sidebar",
        tint = PrimaryBlueDark
      )
    }

    Box(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          repeat(2) { index ->
            val active = index == activeInputPage
            Box(
              modifier = Modifier
                .size(if (active) 7.dp else 5.dp)
                .background(if (active) HighlightCard else CardBorder.copy(alpha = 0.9f), CircleShape)
            )
          }
        }
      }
    }

    Box(modifier = Modifier.size(42.dp))
  }
}

@Composable
private fun GuruHomeTopBar(
  title: String,
  onMenuClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x160F172A), spotColor = Color(0x160F172A))
      .background(Color.White, RoundedCornerShape(20.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
        .clickable(onClick = onMenuClick)
        .padding(10.dp)
    ) {
      Icon(
        imageVector = Icons.Outlined.Menu,
        contentDescription = "Buka sidebar",
        tint = PrimaryBlueDark
      )
    }
    Column(modifier = Modifier.padding(start = 14.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = "Drawer ini mengikuti struktur menu guru versi web",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GuruHomeScreenPreview() {
  MimGuruTheme {
    GuruHomeScreen(
      dashboard = SampleDataFactory.createDashboard("Ustadz Fulan"),
      syncBanner = SyncBannerState("Data lokal siap. Sinkronisasi akan berjalan otomatis di background.", false),
      selectedDestination = GuruSidebarDestination.Mapel,
      isCalendarScreenOpen = false,
      isNotificationPopupOpen = false,
      selectedCalendarDateIso = LocalDate.now().toString(),
      teachingReminderSettings = TeachingReminderSettings(),
      pendingInputAbsensiTarget = null,
      isSidebarOpen = true,
      expandedSidebarParent = GuruSidebarParent.AktivitasHarian,
      isClaimSectionVisible = true,
      selectedClaimSubjectIds = setOf("offer-1"),
      onToggleSidebar = {},
      onCloseSidebar = {},
      onToggleSidebarParent = {},
      onSelectDestination = {},
      onOpenCalendarScreen = {},
      onCloseCalendarScreen = {},
      onOpenNotificationPopup = {},
      onCloseNotificationPopup = {},
      onMarkNotificationAsRead = {},
      onMarkAllNotificationsAsRead = {},
      onUpdateTeachingReminderSettings = {},
      onOpenInputAbsensiTarget = {},
      onConsumePendingInputAbsensiTarget = {},
      onLoadAttendanceApprovalRequest = { null },
      onReviewAttendanceApproval = { _, _, _ -> AttendanceSaveOutcome(true, "OK") },
      onSelectCalendarDate = {},
      onToggleClaimSection = {},
      onToggleClaimSubject = {},
      onClearClaimSelection = {},
      onClaimSelectedSubjects = {},
      onLoadMapelAttendance = { _, _ -> null },
      onSaveMapelAttendance = { _, _, _, _ -> AttendanceSaveOutcome(true, "OK") },
      onSaveMapelAttendanceBatch = { _, _, _, _, _ -> AttendanceSaveOutcome(true, "OK") },
      onDeleteMapelAttendance = { _, _, _ -> AttendanceSaveOutcome(true, "OK") },
      onSendMapelAttendanceDelegation = { _, _, _, _, _, _ -> AttendanceSaveOutcome(true, "OK") },
      onLoadSubstituteTeacherContext = { _, _ -> emptyList<SubjectOverview>() to emptyList() },
      onLoadDelegatedAttendanceContext = { emptyList<SubjectOverview>() to emptyList() },
      onLoadMapelScores = { _, _ -> null },
      onSaveMapelScores = { _, _, _ -> ScoreSaveOutcome(true, "OK") },
      onSaveMapelScoresBatch = { _, _, _ -> ScoreSaveOutcome(true, "OK") },
      onLoadMapelPatronMateri = { _, _ -> null },
      onSaveMapelPatronMateri = { _, _, _ -> PatronMateriSaveOutcome(true, "OK") },
      onSaveProfile = { ProfileSaveOutcome(true, "OK") },
      onSaveSantri = { SantriSaveOutcome(true, "OK") },
      onSaveMonthlyReport = { MonthlyReportSaveOutcome(true, "OK") },
      onSaveMonthlyExtracurricularReports = { MonthlyExtracurricularSaveOutcome(true, "OK") },
      onSaveUtsReportOverride = { UtsReportSaveOutcome(true, "OK") },
      onLoadMutabaah = { null },
      onSaveMutabaahStatus = { _, _, _ -> MutabaahSaveOutcome(true, "OK") },
      onSaveMutabaahStatuses = { _, _, _ -> MutabaahSaveOutcome(true, "OK") },
      onLoadLeaveRequests = { null },
      onSubmitLeaveRequest = { _, _, _ -> LeaveRequestSaveOutcome(true, "OK") },
      onDeleteLeaveRequest = { _ -> LeaveRequestSaveOutcome(true, "OK") },
      onRefreshClick = {},
      onLogoutClick = {}
    )
  }
}
