package com.mim.guruapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mim.guruapp.GuruAppUiState
import com.mim.guruapp.GuruDestination
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
import com.mim.guruapp.data.model.AttendanceHistoryEntry
import com.mim.guruapp.data.model.AttendanceApprovalRequest
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.GuruProfile
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
import com.mim.guruapp.data.model.TeacherOption
import com.mim.guruapp.data.model.TeachingReminderSettings
import com.mim.guruapp.data.model.UtsReportOverride
import com.mim.guruapp.data.model.WaliSantriProfile
import com.mim.guruapp.ui.components.clearFocusOnOutsideTap
import com.mim.guruapp.ui.screens.GuruHomeScreen
import com.mim.guruapp.ui.screens.LoginScreen
import com.mim.guruapp.ui.screens.WelcomeScreen
import com.mim.guruapp.ui.theme.AppBackground
import java.time.LocalDate

@Composable
fun GuruAppRoot(
  state: GuruAppUiState,
  onTeacherNameChange: (String) -> Unit,
  onPasswordChange: (String) -> Unit,
  onLoginClick: () -> Unit,
  onUseDemoAccount: () -> Unit,
  onToggleSidebar: () -> Unit,
  onCloseSidebar: () -> Unit,
  onToggleSidebarParent: (GuruSidebarParent) -> Unit,
  onSelectSidebarDestination: (GuruSidebarDestination) -> Unit,
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
  onSaveProfile: suspend (GuruProfile) -> ProfileSaveOutcome,
  onSaveSantri: suspend (WaliSantriProfile) -> SantriSaveOutcome,
  onSaveMonthlyReport: suspend (MonthlyReportItem) -> MonthlyReportSaveOutcome,
  onSaveMonthlyExtracurricularReports: suspend (List<MonthlyExtracurricularReport>) -> MonthlyExtracurricularSaveOutcome,
  onSaveUtsReportOverride: suspend (UtsReportOverride) -> UtsReportSaveOutcome,
  onLoadMutabaah: suspend (LocalDate) -> MutabaahSnapshot?,
  onSaveMutabaahStatus: suspend (String, String, Boolean) -> MutabaahSaveOutcome,
  onLoadLeaveRequests: suspend () -> LeaveRequestSnapshot?,
  onSubmitLeaveRequest: suspend (String, String, String) -> LeaveRequestSaveOutcome,
  onDeleteLeaveRequest: suspend (String) -> LeaveRequestSaveOutcome,
  onRefreshClick: () -> Unit,
  onLogoutClick: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxSize()
      .clearFocusOnOutsideTap()
      .background(AppBackground)
  ) {
    when (state.destination) {
      GuruDestination.Splash -> WelcomeScreen(
        title = "MIM Guru App",
        message = state.splashMessage
      )

      GuruDestination.Login -> LoginScreen(
        teacherName = state.loginTeacherName,
        password = state.loginPassword,
        errorMessage = state.loginError,
        isBusy = state.isBusy,
        onTeacherNameChange = onTeacherNameChange,
        onPasswordChange = onPasswordChange,
        onLoginClick = onLoginClick,
        onUseDemoAccount = onUseDemoAccount
      )

      GuruDestination.Home -> {
        val dashboard = state.dashboard
        if (dashboard == null) {
          Box(modifier = Modifier.fillMaxSize())
        } else {
          GuruHomeScreen(
            dashboard = dashboard,
            syncBanner = state.syncBanner,
            selectedDestination = state.selectedSidebarDestination,
            isCalendarScreenOpen = state.isCalendarScreenOpen,
            isNotificationPopupOpen = state.isNotificationPopupOpen,
            selectedCalendarDateIso = state.selectedCalendarDateIso,
            teachingReminderSettings = state.teachingReminderSettings,
            pendingInputAbsensiTarget = state.pendingInputAbsensiTarget,
            isSidebarOpen = state.isSidebarOpen,
            expandedSidebarParent = state.expandedSidebarParent,
            isClaimSectionVisible = state.isClaimSectionVisible,
            selectedClaimSubjectIds = state.selectedClaimSubjectIds,
            onToggleSidebar = onToggleSidebar,
            onCloseSidebar = onCloseSidebar,
            onToggleSidebarParent = onToggleSidebarParent,
            onSelectDestination = onSelectSidebarDestination,
            onOpenCalendarScreen = onOpenCalendarScreen,
            onCloseCalendarScreen = onCloseCalendarScreen,
            onOpenNotificationPopup = onOpenNotificationPopup,
            onCloseNotificationPopup = onCloseNotificationPopup,
            onMarkNotificationAsRead = onMarkNotificationAsRead,
            onMarkAllNotificationsAsRead = onMarkAllNotificationsAsRead,
            onUpdateTeachingReminderSettings = onUpdateTeachingReminderSettings,
            onOpenInputAbsensiTarget = onOpenInputAbsensiTarget,
            onConsumePendingInputAbsensiTarget = onConsumePendingInputAbsensiTarget,
            onLoadAttendanceApprovalRequest = onLoadAttendanceApprovalRequest,
            onReviewAttendanceApproval = onReviewAttendanceApproval,
            onSelectCalendarDate = onSelectCalendarDate,
            onToggleClaimSection = onToggleClaimSection,
            onToggleClaimSubject = onToggleClaimSubject,
            onClearClaimSelection = onClearClaimSelection,
            onClaimSelectedSubjects = onClaimSelectedSubjects,
            onLoadMapelAttendance = onLoadMapelAttendance,
            onSaveMapelAttendance = onSaveMapelAttendance,
            onSaveMapelAttendanceBatch = onSaveMapelAttendanceBatch,
            onDeleteMapelAttendance = onDeleteMapelAttendance,
            onSendMapelAttendanceDelegation = onSendMapelAttendanceDelegation,
            onLoadSubstituteTeacherContext = onLoadSubstituteTeacherContext,
            onLoadDelegatedAttendanceContext = onLoadDelegatedAttendanceContext,
            onLoadMapelScores = onLoadMapelScores,
            onSaveMapelScores = onSaveMapelScores,
            onSaveMapelScoresBatch = onSaveMapelScoresBatch,
            onLoadMapelPatronMateri = onLoadMapelPatronMateri,
            onSaveMapelPatronMateri = onSaveMapelPatronMateri,
            onSaveProfile = onSaveProfile,
            onSaveSantri = onSaveSantri,
            onSaveMonthlyReport = onSaveMonthlyReport,
            onSaveMonthlyExtracurricularReports = onSaveMonthlyExtracurricularReports,
            onSaveUtsReportOverride = onSaveUtsReportOverride,
            onLoadMutabaah = onLoadMutabaah,
            onSaveMutabaahStatus = onSaveMutabaahStatus,
            onLoadLeaveRequests = onLoadLeaveRequests,
            onSubmitLeaveRequest = onSubmitLeaveRequest,
            onDeleteLeaveRequest = onDeleteLeaveRequest,
            onRefreshClick = onRefreshClick,
            onLogoutClick = onLogoutClick
          )
        }
      }
    }
  }
}
