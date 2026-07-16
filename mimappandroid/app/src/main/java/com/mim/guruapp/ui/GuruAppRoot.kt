package com.mim.guruapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.mim.guruapp.GuruAppUiState
import com.mim.guruapp.GuruDestination
import com.mim.guruapp.GuruSidebarParent
import com.mim.guruapp.GuruSidebarDestination
import com.mim.guruapp.AttendanceSaveOutcome
import com.mim.guruapp.LeaveRequestSaveOutcome
import com.mim.guruapp.PatronMateriSaveOutcome
import com.mim.guruapp.QuestionSaveOutcome
import com.mim.guruapp.ScoreSaveOutcome
import com.mim.guruapp.ProfileSaveOutcome
import com.mim.guruapp.SantriSaveOutcome
import com.mim.guruapp.MutabaahSaveOutcome
import com.mim.guruapp.MonthlyExtracurricularSaveOutcome
import com.mim.guruapp.MonthlyReportSaveOutcome
import com.mim.guruapp.UtsReportSaveOutcome
import com.mim.guruapp.WakasekReviewOutcome
import com.mim.guruapp.availableAppRoles
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
import com.mim.guruapp.data.model.MonthlyAttendanceSummary
import com.mim.guruapp.data.model.MonthlyReportItem
import com.mim.guruapp.data.model.PatronMateriItem
import com.mim.guruapp.data.model.ScoreStudent
import com.mim.guruapp.data.model.SubjectOverview
import com.mim.guruapp.data.model.TeacherOption
import com.mim.guruapp.data.model.TeachingReminderSettings
import com.mim.guruapp.data.model.UtsReportOverride
import com.mim.guruapp.data.model.WaliAttendanceDetailSnapshot
import com.mim.guruapp.data.model.WaliSantriProfile
import com.mim.guruapp.data.remote.GuruAiGenerateRequest
import com.mim.guruapp.data.remote.GuruAiGenerateResult
import com.mim.guruapp.data.remote.GuruAiTokenWallet
import com.mim.guruapp.data.remote.GuruExamQuestionItem
import com.mim.guruapp.data.remote.GuruExamQuestionSnapshot
import com.mim.guruapp.data.remote.AdminAcademicCalendarEvent
import com.mim.guruapp.data.remote.AdminAcademicCalendarLoadResult
import com.mim.guruapp.data.remote.AdminAcademicCalendarSaveResult
import com.mim.guruapp.data.remote.AdminAcademicPeriodLoadResult
import com.mim.guruapp.data.remote.AdminAcademicPeriodSaveResult
import com.mim.guruapp.data.remote.AdminAcademicSemester
import com.mim.guruapp.data.remote.AdminAcademicYear
import com.mim.guruapp.data.remote.AdminEmployee
import com.mim.guruapp.data.remote.AdminEmployeeListResult
import com.mim.guruapp.data.remote.AdminEmployeeSaveResult
import com.mim.guruapp.data.remote.AdminKelas
import com.mim.guruapp.data.remote.AdminKelasAssignStudentsResult
import com.mim.guruapp.data.remote.AdminKelasLoadResult
import com.mim.guruapp.data.remote.AdminKelasSaveResult
import com.mim.guruapp.data.remote.AdminLessonSlot
import com.mim.guruapp.data.remote.AdminMapelDistribution
import com.mim.guruapp.data.remote.AdminMapelLoadResult
import com.mim.guruapp.data.remote.AdminMapelSaveResult
import com.mim.guruapp.data.remote.AdminMapelSubject
import com.mim.guruapp.data.remote.AdminSchoolProfile
import com.mim.guruapp.data.remote.AdminSchoolProfileLoadResult
import com.mim.guruapp.data.remote.AdminSchoolProfileSaveResult
import com.mim.guruapp.data.remote.AdminSantri
import com.mim.guruapp.data.remote.AdminSantriLoadResult
import com.mim.guruapp.data.remote.AdminSantriSaveResult
import com.mim.guruapp.data.remote.AdminTeachingScheduleLoadResult
import com.mim.guruapp.data.remote.AdminTeachingScheduleRow
import com.mim.guruapp.data.remote.AdminTeachingScheduleSaveResult
import com.mim.guruapp.ui.components.clearFocusOnOutsideTap
import com.mim.guruapp.ui.i18n.AppLanguage
import com.mim.guruapp.ui.i18n.LocalAppLanguage
import com.mim.guruapp.ui.i18n.t
import com.mim.guruapp.ui.screens.GuruHomeScreen
import com.mim.guruapp.ui.screens.LoginScreen
import com.mim.guruapp.ui.screens.MfaChallengeScreen
import com.mim.guruapp.ui.screens.RolePickerScreen
import com.mim.guruapp.ui.screens.WelcomeScreen
import com.mim.guruapp.ui.theme.AppBackground
import java.time.LocalDate

@Composable
fun GuruAppRoot(
  state: GuruAppUiState,
  onLoginTenantSelected: (String) -> Unit,
  onTeacherNameChange: (String) -> Unit,
  onPasswordChange: (String) -> Unit,
  onLoginClick: () -> Unit,
  onGoogleLoginClick: () -> Unit,
  onForgotPassword: suspend () -> ProfileSaveOutcome,
  onVerifyMfa: suspend (String) -> ProfileSaveOutcome,
  onCancelMfa: () -> Unit,
  onUseDemoAccount: () -> Unit,
  onSelectActiveRole: (String) -> Unit,
  onOpenRolePicker: () -> Unit,
  onToggleSidebar: () -> Unit,
  onCloseSidebar: () -> Unit,
  onToggleSidebarParent: (GuruSidebarParent) -> Unit,
  onSelectSidebarDestination: (GuruSidebarDestination) -> Unit,
  onUpdateBottomNavShortcuts: (List<GuruSidebarDestination>) -> Unit,
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
  onLoadMapelQuestions: suspend (String, SubjectOverview) -> String?,
  onSaveMapelQuestions: suspend (String, SubjectOverview, String) -> QuestionSaveOutcome,
  onLoadMapelRaporDescriptions: suspend (String, SubjectOverview) -> String?,
  onSaveMapelRaporDescriptions: suspend (String, SubjectOverview, String) -> QuestionSaveOutcome,
  onLoadExamQuestions: suspend () -> GuruExamQuestionSnapshot,
  onSaveExamQuestions: suspend (GuruExamQuestionItem, String) -> QuestionSaveOutcome,
  onLoadAiTokenBalance: suspend () -> GuruAiTokenWallet?,
  onGenerateAiContent: suspend (GuruAiGenerateRequest) -> GuruAiGenerateResult,
  onLoadAdminAcademicCalendar: suspend () -> AdminAcademicCalendarLoadResult,
  onSaveAdminAcademicCalendarEvent: suspend (AdminAcademicCalendarEvent) -> AdminAcademicCalendarSaveResult,
  onDeleteAdminAcademicCalendarEvent: suspend (String) -> AdminAcademicCalendarSaveResult,
  onLoadAdminAcademicPeriods: suspend () -> AdminAcademicPeriodLoadResult,
  onSaveAdminAcademicYear: suspend (AdminAcademicYear) -> AdminAcademicPeriodSaveResult,
  onSaveAdminSemester: suspend (AdminAcademicSemester) -> AdminAcademicPeriodSaveResult,
  onSetActiveAdminAcademicYear: suspend (String) -> AdminAcademicPeriodSaveResult,
  onSetActiveAdminSemester: suspend (String) -> AdminAcademicPeriodSaveResult,
  onLoadAdminEmployees: suspend () -> AdminEmployeeListResult,
  onSaveAdminEmployee: suspend (AdminEmployee, String) -> AdminEmployeeSaveResult,
  onLoadAdminSchoolProfile: suspend () -> AdminSchoolProfileLoadResult,
  onSaveAdminSchoolProfile: suspend (AdminSchoolProfile) -> AdminSchoolProfileSaveResult,
  onLoadAdminKelas: suspend () -> AdminKelasLoadResult,
  onSaveAdminKelas: suspend (AdminKelas) -> AdminKelasSaveResult,
  onAssignAdminKelasStudents: suspend (AdminKelas, List<String>) -> AdminKelasAssignStudentsResult,
  onLoadAdminMapel: suspend () -> AdminMapelLoadResult,
  onSaveAdminMapelSubject: suspend (AdminMapelSubject) -> AdminMapelSaveResult,
  onSaveAdminMapelDistribution: suspend (AdminMapelDistribution) -> AdminMapelSaveResult,
  onLoadAdminTeachingSchedule: suspend () -> AdminTeachingScheduleLoadResult,
  onSaveAdminTeachingSchedule: suspend (AdminTeachingScheduleRow) -> AdminTeachingScheduleSaveResult,
  onDeleteAdminTeachingSchedule: suspend (String) -> AdminTeachingScheduleSaveResult,
  onSaveAdminLessonSlot: suspend (AdminLessonSlot) -> AdminTeachingScheduleSaveResult,
  onDeleteAdminLessonSlot: suspend (String) -> AdminTeachingScheduleSaveResult,
  onLoadAdminSantri: suspend () -> AdminSantriLoadResult,
  onSaveAdminSantri: suspend (AdminSantri) -> AdminSantriSaveResult,
  onPromoteAdminSantri: suspend (AdminSantri) -> AdminSantriSaveResult,
  onGraduateAdminSantri: suspend (AdminSantri) -> AdminSantriSaveResult,
  onSaveProfile: suspend (GuruProfile) -> ProfileSaveOutcome,
  onChangePassword: suspend (String, String) -> ProfileSaveOutcome,
  onLinkGoogleAccount: suspend () -> ProfileSaveOutcome,
  onSaveSantri: suspend (WaliSantriProfile) -> SantriSaveOutcome,
  onSaveMonthlyReport: suspend (MonthlyReportItem) -> MonthlyReportSaveOutcome,
  onSaveMonthlyExtracurricularReports: suspend (List<MonthlyExtracurricularReport>) -> MonthlyExtracurricularSaveOutcome,
  onLoadMonthlyAttendanceSummaries: suspend (String) -> List<MonthlyAttendanceSummary>,
  onLoadMonthlyAttendanceDetail: suspend (String, WaliSantriProfile) -> WaliAttendanceDetailSnapshot?,
  onSaveUtsReportOverride: suspend (UtsReportOverride) -> UtsReportSaveOutcome,
  onLoadMutabaah: suspend (LocalDate) -> MutabaahSnapshot?,
  onSaveMutabaahStatus: suspend (String, String, Boolean) -> MutabaahSaveOutcome,
  onSaveMutabaahStatuses: suspend (List<String>, String, String) -> MutabaahSaveOutcome,
  onLoadLeaveRequests: suspend () -> LeaveRequestSnapshot?,
  onSubmitLeaveRequest: suspend (String, String, String) -> LeaveRequestSaveOutcome,
  onDeleteLeaveRequest: suspend (String) -> LeaveRequestSaveOutcome,
  onReviewWakasekLeaveRequest: suspend (String, Boolean, String) -> WakasekReviewOutcome,
  onApplyLanguage: (String) -> Unit,
  onApplyThemeMode: (String) -> Unit,
  onRefreshClick: () -> Unit,
  onLogoutClick: () -> Unit
) {
  CompositionLocalProvider(LocalAppLanguage provides AppLanguage.fromCode(state.languageCode)) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .clearFocusOnOutsideTap()
        .background(AppBackground)
    ) {
      when (state.destination) {
        GuruDestination.Splash -> WelcomeScreen(
          title = t("MIM Guru App"),
          message = t(state.splashMessage),
          progress = state.splashProgress
        )

        GuruDestination.Login -> LoginScreen(
          tenants = state.loginTenants,
          selectedTenantId = state.selectedLoginTenantId,
          teacherName = state.loginTeacherName,
          password = state.loginPassword,
          errorMessage = t(state.loginError),
          isBusy = state.isBusy,
          onTenantSelected = onLoginTenantSelected,
          onTeacherNameChange = onTeacherNameChange,
          onPasswordChange = onPasswordChange,
          onLoginClick = onLoginClick,
          onGoogleLoginClick = onGoogleLoginClick,
          onForgotPassword = onForgotPassword,
          onUseDemoAccount = onUseDemoAccount
        )

        GuruDestination.Mfa -> MfaChallengeScreen(
          errorMessage = t(state.loginError),
          isBusy = state.isBusy,
          onVerify = onVerifyMfa,
          onCancel = onCancelMfa
        )

        GuruDestination.RolePicker -> RolePickerScreen(
          teacherName = state.session.teacherName.ifBlank { state.dashboard?.teacherName.orEmpty() },
          activeRole = state.session.activeRole,
          roles = availableAppRoles(state.session.roles),
          onSelectRole = onSelectActiveRole,
          onLogoutClick = onLogoutClick
        )

        GuruDestination.Home -> {
          val dashboard = state.dashboard
          if (dashboard == null) {
            Box(modifier = Modifier.fillMaxSize())
          } else {
            GuruHomeScreen(
              dashboard = dashboard,
              activeRole = state.session.activeRole,
              availableRoles = availableAppRoles(state.session.roles),
              syncBanner = state.syncBanner,
              selectedDestination = state.selectedSidebarDestination,
              isCalendarScreenOpen = state.isCalendarScreenOpen,
              isNotificationPopupOpen = state.isNotificationPopupOpen,
              selectedCalendarDateIso = state.selectedCalendarDateIso,
              teachingReminderSettings = state.teachingReminderSettings,
              pendingInputAbsensiTarget = state.pendingInputAbsensiTarget,
              isSidebarOpen = state.isSidebarOpen,
              expandedSidebarParent = state.expandedSidebarParent,
              bottomNavShortcutDestinations = state.bottomNavShortcutDestinations,
              languageCode = state.languageCode,
              themeModeCode = state.themeModeCode,
              isClaimSectionVisible = state.isClaimSectionVisible,
              selectedClaimSubjectIds = state.selectedClaimSubjectIds,
              onToggleSidebar = onToggleSidebar,
              onCloseSidebar = onCloseSidebar,
              onToggleSidebarParent = onToggleSidebarParent,
              onSelectDestination = onSelectSidebarDestination,
              onOpenRolePicker = onOpenRolePicker,
              onUpdateBottomNavShortcuts = onUpdateBottomNavShortcuts,
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
              onLoadMapelQuestions = onLoadMapelQuestions,
              onSaveMapelQuestions = onSaveMapelQuestions,
              onLoadMapelRaporDescriptions = onLoadMapelRaporDescriptions,
              onSaveMapelRaporDescriptions = onSaveMapelRaporDescriptions,
              onLoadExamQuestions = onLoadExamQuestions,
              onSaveExamQuestions = onSaveExamQuestions,
              onLoadAiTokenBalance = onLoadAiTokenBalance,
              onGenerateAiContent = onGenerateAiContent,
              onLoadAdminAcademicCalendar = onLoadAdminAcademicCalendar,
              onSaveAdminAcademicCalendarEvent = onSaveAdminAcademicCalendarEvent,
              onDeleteAdminAcademicCalendarEvent = onDeleteAdminAcademicCalendarEvent,
              onLoadAdminAcademicPeriods = onLoadAdminAcademicPeriods,
              onSaveAdminAcademicYear = onSaveAdminAcademicYear,
              onSaveAdminSemester = onSaveAdminSemester,
              onSetActiveAdminAcademicYear = onSetActiveAdminAcademicYear,
              onSetActiveAdminSemester = onSetActiveAdminSemester,
              onLoadAdminEmployees = onLoadAdminEmployees,
              onSaveAdminEmployee = onSaveAdminEmployee,
              onLoadAdminSchoolProfile = onLoadAdminSchoolProfile,
              onSaveAdminSchoolProfile = onSaveAdminSchoolProfile,
              onLoadAdminKelas = onLoadAdminKelas,
              onSaveAdminKelas = onSaveAdminKelas,
              onAssignAdminKelasStudents = onAssignAdminKelasStudents,
              onLoadAdminMapel = onLoadAdminMapel,
              onSaveAdminMapelSubject = onSaveAdminMapelSubject,
              onSaveAdminMapelDistribution = onSaveAdminMapelDistribution,
              onLoadAdminTeachingSchedule = onLoadAdminTeachingSchedule,
              onSaveAdminTeachingSchedule = onSaveAdminTeachingSchedule,
              onDeleteAdminTeachingSchedule = onDeleteAdminTeachingSchedule,
              onSaveAdminLessonSlot = onSaveAdminLessonSlot,
              onDeleteAdminLessonSlot = onDeleteAdminLessonSlot,
              onLoadAdminSantri = onLoadAdminSantri,
              onSaveAdminSantri = onSaveAdminSantri,
              onPromoteAdminSantri = onPromoteAdminSantri,
              onGraduateAdminSantri = onGraduateAdminSantri,
              onSaveProfile = onSaveProfile,
              onChangePassword = onChangePassword,
              onLinkGoogleAccount = onLinkGoogleAccount,
              onSaveSantri = onSaveSantri,
              onSaveMonthlyReport = onSaveMonthlyReport,
              onSaveMonthlyExtracurricularReports = onSaveMonthlyExtracurricularReports,
              onLoadMonthlyAttendanceSummaries = onLoadMonthlyAttendanceSummaries,
              onLoadMonthlyAttendanceDetail = onLoadMonthlyAttendanceDetail,
              onSaveUtsReportOverride = onSaveUtsReportOverride,
              onLoadMutabaah = onLoadMutabaah,
              onSaveMutabaahStatus = onSaveMutabaahStatus,
              onSaveMutabaahStatuses = onSaveMutabaahStatuses,
              onLoadLeaveRequests = onLoadLeaveRequests,
              onSubmitLeaveRequest = onSubmitLeaveRequest,
              onDeleteLeaveRequest = onDeleteLeaveRequest,
              onReviewWakasekLeaveRequest = onReviewWakasekLeaveRequest,
              onApplyLanguage = onApplyLanguage,
              onApplyThemeMode = onApplyThemeMode,
              onRefreshClick = onRefreshClick,
              onLogoutClick = onLogoutClick
            )
          }
        }
      }
    }
  }
}
