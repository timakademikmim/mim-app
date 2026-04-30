package com.mim.guruapp

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mim.guruapp.alarm.TeachingReminderNotifier
import com.mim.guruapp.alarm.TeachingReminderReceiver
import com.mim.guruapp.alarm.TeachingReminderScheduler
import com.mim.guruapp.data.remote.GuruAuthRemoteDataSource
import com.mim.guruapp.data.remote.GuruMapelAttendanceRemoteDataSource
import com.mim.guruapp.data.remote.GuruMapelAttendanceReviewResult
import com.mim.guruapp.data.remote.GuruMapelAttendanceSaveResult
import com.mim.guruapp.data.remote.GuruLeaveRequestRemoteDataSource
import com.mim.guruapp.data.remote.GuruLeaveRequestSaveResult
import com.mim.guruapp.data.remote.GuruMapelPatronMateriRemoteDataSource
import com.mim.guruapp.data.remote.GuruMapelPatronMateriSaveResult
import com.mim.guruapp.data.remote.GuruMapelScoreRemoteDataSource
import com.mim.guruapp.data.remote.GuruMapelScoreSaveResult
import com.mim.guruapp.data.remote.GuruMutabaahRemoteDataSource
import com.mim.guruapp.data.remote.GuruMutabaahSaveResult
import com.mim.guruapp.data.remote.GuruMonthlyReportRemoteDataSource
import com.mim.guruapp.data.remote.GuruMonthlyExtracurricularSaveResult
import com.mim.guruapp.data.remote.GuruMonthlyReportSaveResult
import com.mim.guruapp.data.remote.GuruUtsReportRemoteDataSource
import com.mim.guruapp.data.remote.GuruUtsReportSaveResult
import com.mim.guruapp.data.remote.ScoreDraftPayload
import com.mim.guruapp.data.remote.GuruMapelClaimResult
import com.mim.guruapp.data.remote.GuruMapelPayload
import com.mim.guruapp.data.remote.GuruMapelRemoteDataSource
import com.mim.guruapp.data.remote.GuruAuthResult
import com.mim.guruapp.data.remote.GuruProfileRemoteDataSource
import com.mim.guruapp.data.remote.GuruProfileSyncResult
import com.mim.guruapp.data.remote.GuruRemoteProfile
import com.mim.guruapp.data.remote.KalenderAkademikRemoteDataSource
import com.mim.guruapp.data.remote.GuruNotificationRemoteDataSource
import com.mim.guruapp.data.remote.GuruTeacherOptionsRemoteDataSource
import com.mim.guruapp.data.remote.GuruTeachingScheduleRemoteDataSource
import com.mim.guruapp.data.remote.GuruWaliSantriSaveResult
import com.mim.guruapp.data.remote.GuruWaliSantriRemoteDataSource
import com.mim.guruapp.data.model.AvailableSubjectOffer
import com.mim.guruapp.data.model.AppNotification
import com.mim.guruapp.data.model.AttendanceApprovalRequest
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.DashboardPayload
import com.mim.guruapp.data.model.GuruProfile
import com.mim.guruapp.data.model.AttendanceHistoryEntry
import com.mim.guruapp.data.model.AttendanceStudent
import com.mim.guruapp.data.model.InputAbsensiNavigationTarget
import com.mim.guruapp.data.model.LeaveRequestSnapshot
import com.mim.guruapp.data.model.MapelAttendanceSnapshot
import com.mim.guruapp.data.model.MapelPatronMateriSnapshot
import com.mim.guruapp.data.model.MapelScoreSnapshot
import com.mim.guruapp.data.model.MonthlyExtracurricularReport
import com.mim.guruapp.data.model.MonthlyReportItem
import com.mim.guruapp.data.model.MonthlyReportSnapshot
import com.mim.guruapp.data.model.MutabaahSnapshot
import com.mim.guruapp.data.model.MutabaahSubmission
import com.mim.guruapp.data.model.PatronMateriItem
import com.mim.guruapp.data.model.PendingAttendanceBatch
import com.mim.guruapp.data.model.PendingScoreBatch
import com.mim.guruapp.data.model.ScoreStudent
import com.mim.guruapp.data.model.SessionSnapshot
import com.mim.guruapp.data.model.SubjectOverview
import com.mim.guruapp.data.model.SyncBannerState
import com.mim.guruapp.data.model.TeacherOption
import com.mim.guruapp.data.model.TeachingReminderSettings
import com.mim.guruapp.data.model.UtsReportOverride
import com.mim.guruapp.data.model.UtsReportSnapshot
import com.mim.guruapp.data.model.WaliSantriProfile
import com.mim.guruapp.data.model.WaliSantriSnapshot
import com.mim.guruapp.data.storage.GuruCacheStore
import com.mim.guruapp.data.storage.SessionStore
import com.mim.guruapp.data.storage.TeachingReminderStore
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class GuruDestination {
  Splash,
  Login,
  Home
}

enum class GuruSidebarDestination(val title: String) {
  Dashboard("Dashboard"),
  Tugas("Tugas"),
  Perizinan("Perizinan"),
  Jadwal("Jadwal"),
  Mapel("Mapel"),
  Ujian("Ujian"),
  InputNilai("Input Nilai"),
  InputAbsensi("Input Absensi"),
  LaporanAbsensi("Laporan Absensi"),
  LaporanUTS("Laporan UTS"),
  LaporanPekanan("Laporan Pekanan"),
  LaporanBulanan("Laporan Bulanan"),
  Rapor("Rapor"),
  Santri("Santri"),
  Pesan("Pesan"),
  Notifikasi("Notifikasi"),
  Profil("Profil")
}

enum class GuruSidebarParent {
  KinerjaGuru,
  Akademik,
  AktivitasHarian,
  KelasSaya
}

data class GuruAppUiState(
  val destination: GuruDestination = GuruDestination.Splash,
  val splashMessage: String = "Menyiapkan aplikasi...",
  val loginTeacherName: String = "",
  val loginPassword: String = "",
  val loginError: String = "",
  val isBusy: Boolean = false,
  val dashboard: DashboardPayload? = null,
  val session: SessionSnapshot = SessionSnapshot(),
  val syncBanner: SyncBannerState = SyncBannerState(),
  val selectedSidebarDestination: GuruSidebarDestination = GuruSidebarDestination.Mapel,
  val isCalendarScreenOpen: Boolean = false,
  val isNotificationPopupOpen: Boolean = false,
  val selectedCalendarDateIso: String = LocalDate.now().toString(),
  val teachingReminderSettings: TeachingReminderSettings = TeachingReminderSettings(),
  val pendingInputAbsensiTarget: InputAbsensiNavigationTarget? = null,
  val isSidebarOpen: Boolean = false,
  val expandedSidebarParent: GuruSidebarParent? = null,
  val isClaimSectionVisible: Boolean = false,
  val selectedClaimSubjectIds: Set<String> = emptySet()
)

data class ProfileSaveOutcome(
  val success: Boolean,
  val message: String
)

data class SantriSaveOutcome(
  val success: Boolean,
  val message: String
)

data class MutabaahSaveOutcome(
  val success: Boolean,
  val message: String
)

data class LeaveRequestSaveOutcome(
  val success: Boolean,
  val message: String
)

data class AttendanceSaveOutcome(
  val success: Boolean,
  val message: String
)

data class ScoreSaveOutcome(
  val success: Boolean,
  val message: String
)

data class PatronMateriSaveOutcome(
  val success: Boolean,
  val message: String
)

data class MonthlyReportSaveOutcome(
  val success: Boolean,
  val message: String,
  val report: MonthlyReportItem? = null
)

data class MonthlyExtracurricularSaveOutcome(
  val success: Boolean,
  val message: String,
  val reports: List<MonthlyExtracurricularReport> = emptyList()
)

data class UtsReportSaveOutcome(
  val success: Boolean,
  val message: String,
  val override: UtsReportOverride? = null,
  val deletedStudentId: String = "",
  val semesterId: String = ""
)

class GuruAppViewModel(application: Application) : AndroidViewModel(application) {
  private val sessionStore = SessionStore(application.applicationContext)
  private val cacheStore = GuruCacheStore(application.applicationContext)
  private val teachingReminderStore = TeachingReminderStore(application.applicationContext)
  private val teachingReminderScheduler = TeachingReminderScheduler(application.applicationContext, teachingReminderStore)
  private val authRemoteDataSource = GuruAuthRemoteDataSource()
  private val mapelRemoteDataSource = GuruMapelRemoteDataSource()
  private val mapelAttendanceRemoteDataSource = GuruMapelAttendanceRemoteDataSource()
  private val mapelScoreRemoteDataSource = GuruMapelScoreRemoteDataSource()
  private val mapelPatronMateriRemoteDataSource = GuruMapelPatronMateriRemoteDataSource()
  private val mutabaahRemoteDataSource = GuruMutabaahRemoteDataSource()
  private val leaveRequestRemoteDataSource = GuruLeaveRequestRemoteDataSource()
  private val monthlyReportRemoteDataSource = GuruMonthlyReportRemoteDataSource()
  private val utsReportRemoteDataSource = GuruUtsReportRemoteDataSource()
  private val profileRemoteDataSource = GuruProfileRemoteDataSource()
  private val kalenderAkademikRemoteDataSource = KalenderAkademikRemoteDataSource()
  private val guruNotificationRemoteDataSource = GuruNotificationRemoteDataSource()
  private val teachingScheduleRemoteDataSource = GuruTeachingScheduleRemoteDataSource()
  private val teacherOptionsRemoteDataSource = GuruTeacherOptionsRemoteDataSource()
  private val waliSantriRemoteDataSource = GuruWaliSantriRemoteDataSource()
  private val substituteTeacherContextCache = mutableMapOf<String, Pair<List<SubjectOverview>, List<CalendarEvent>>>()

  var uiState by mutableStateOf(GuruAppUiState())
    private set

  init {
    bootstrap()
  }

  fun onTeacherNameChange(value: String) {
    uiState = uiState.copy(loginTeacherName = value, loginError = "")
  }

  fun onPasswordChange(value: String) {
    uiState = uiState.copy(loginPassword = value, loginError = "")
  }

  fun login() {
    if (uiState.isBusy) return
    val teacherId = uiState.loginTeacherName.trim()
    val password = uiState.loginPassword.trim()
    if (teacherId.isEmpty() || password.isEmpty()) {
      uiState = uiState.copy(loginError = "ID Karyawan dan password wajib diisi.")
      return
    }

    viewModelScope.launch {
      uiState = uiState.copy(isBusy = true, loginError = "", splashMessage = "Masuk ke aplikasi...")
      when (val result = authRemoteDataSource.loginAsGuru(teacherId = teacherId, password = password)) {
        is GuruAuthResult.Error -> {
          uiState = uiState.copy(
            isBusy = false,
            loginError = result.message
          )
        }

        is GuruAuthResult.Success -> {
          val initialSession = SessionSnapshot(
            isLoggedIn = true,
            teacherRowId = result.user.teacherRowId,
            teacherId = result.user.teacherId,
            teacherName = result.user.teacherName,
            activeRole = result.user.activeRole,
            roles = result.user.roles
          )
          val remoteProfile = profileRemoteDataSource.fetchProfile(
            teacherRowId = result.user.teacherRowId,
            teacherKaryawanId = result.user.teacherId
          )
          val resolvedTeacherName = remoteProfile?.name?.ifBlank { result.user.teacherName } ?: result.user.teacherName
          sessionStore.saveLogin(
            teacherRowId = result.user.teacherRowId,
            teacherId = result.user.teacherId,
            teacherName = resolvedTeacherName,
            activeRole = result.user.activeRole,
            roles = result.user.roles
          )
          val dashboard = cacheStore.readDashboard() ?: SampleDataFactory.createDashboard(resolvedTeacherName)
          val nextDashboard = dashboard
            .copy(teacherName = resolvedTeacherName)
            .withResolvedProfile(session = initialSession.copy(teacherName = resolvedTeacherName), remoteProfile = remoteProfile)
          cacheStore.writeDashboard(nextDashboard)
          val session = sessionStore.readSession()
          val reminderSettings = teachingReminderStore.readSettings()
          uiState = uiState.copy(
            destination = GuruDestination.Home,
            isBusy = false,
            dashboard = nextDashboard,
            session = session,
            loginTeacherName = result.user.teacherId,
            loginPassword = "",
            syncBanner = SyncBannerState("Login berhasil. Data lokal siap dan sinkron awal sedang dijalankan...", true),
            selectedSidebarDestination = GuruSidebarDestination.Dashboard,
            isCalendarScreenOpen = false,
            selectedCalendarDateIso = LocalDate.now().toString(),
            teachingReminderSettings = reminderSettings,
            isSidebarOpen = false,
            expandedSidebarParent = null,
            isClaimSectionVisible = false,
            selectedClaimSubjectIds = emptySet()
          )
          refreshFromServer(force = true)
        }
      }
    }
  }

  fun useDemoAccount() {
    onTeacherNameChange("khaerurrahmat")
    onPasswordChange("")
  }

  fun toggleSidebar() {
    uiState = uiState.copy(isSidebarOpen = !uiState.isSidebarOpen)
  }

  fun openSidebar() {
    if (uiState.isSidebarOpen) return
    uiState = uiState.copy(isSidebarOpen = true)
  }

  fun closeSidebar() {
    uiState = uiState.copy(isSidebarOpen = false)
  }

  fun openCalendarScreen() {
    uiState = uiState.copy(
      isCalendarScreenOpen = true,
      isNotificationPopupOpen = false,
      isSidebarOpen = false,
      selectedSidebarDestination = GuruSidebarDestination.Dashboard
    )
  }

  fun closeCalendarScreen() {
    uiState = uiState.copy(isCalendarScreenOpen = false)
  }

  fun openNotificationPopup() {
    uiState = uiState.copy(
      isNotificationPopupOpen = true,
      isSidebarOpen = false,
      selectedSidebarDestination = GuruSidebarDestination.Dashboard
    )
    refreshNotificationsOnly()
  }

  fun closeNotificationPopup() {
    uiState = uiState.copy(isNotificationPopupOpen = false)
  }

  fun updateTeachingReminderSettings(settings: TeachingReminderSettings) {
    val normalized = settings.copy(
      minutesBefore = settings.minutesBefore.coerceAtLeast(0),
      ringtoneLabel = settings.ringtoneLabel.ifBlank { "Nada default sistem" },
      updatedAt = System.currentTimeMillis(),
      scheduledReminderIds = uiState.teachingReminderSettings.scheduledReminderIds
    )
    uiState = uiState.copy(teachingReminderSettings = normalized)
    viewModelScope.launch {
      teachingReminderStore.saveSettings(normalized)
      val dashboard = uiState.dashboard
      if (dashboard != null) {
        teachingReminderScheduler.syncReminders(
          settings = normalized,
          events = dashboard.teachingScheduleEvents
        )
      } else if (!normalized.enabled) {
        teachingReminderScheduler.cancelAll(normalized)
      }
      val refreshedSettings = teachingReminderStore.readSettings()
      uiState = uiState.copy(
        teachingReminderSettings = refreshedSettings,
        syncBanner = SyncBannerState(
          if (refreshedSettings.enabled) {
            "Pengingat jam pelajaran aktif ${refreshedSettings.minutesBefore} menit sebelum mulai."
          } else {
            "Pengingat jam pelajaran dinonaktifkan."
          },
          false
        )
      )
    }
  }

  fun consumePendingInputAbsensiTarget() {
    if (uiState.pendingInputAbsensiTarget == null) return
    uiState = uiState.copy(pendingInputAbsensiTarget = null)
  }

  fun openInputAbsensiTarget(target: InputAbsensiNavigationTarget) {
    uiState = uiState.copy(
      destination = GuruDestination.Home,
      selectedSidebarDestination = GuruSidebarDestination.InputAbsensi,
      isCalendarScreenOpen = false,
      isNotificationPopupOpen = false,
      isSidebarOpen = false,
      selectedCalendarDateIso = target.dateIso,
      pendingInputAbsensiTarget = target
    )
  }

  fun handleSystemNavigationIntent(intent: Intent?) {
    if (intent == null) return
    if (!intent.getBooleanExtra(TeachingReminderNotifier.EXTRA_OPEN_INPUT_ABSENSI, false)) return
    val reminderId = intent.getStringExtra(TeachingReminderReceiver.EXTRA_REMINDER_ID).orEmpty()
    val dateIso = intent.getStringExtra(TeachingReminderReceiver.EXTRA_DATE_ISO).orEmpty()
    if (reminderId.isBlank() || dateIso.isBlank()) return
    val notificationId = intent.getIntExtra(TeachingReminderReceiver.EXTRA_NOTIFICATION_ID, 0)
    if (notificationId != 0) {
      TeachingReminderNotifier.cancelNotification(getApplication(), notificationId)
    }
    openInputAbsensiTarget(
      InputAbsensiNavigationTarget(
        requestId = "$reminderId|${System.currentTimeMillis()}",
        dateIso = dateIso,
        distribusiId = intent.getStringExtra(TeachingReminderReceiver.EXTRA_DISTRIBUSI_ID).orEmpty(),
        lessonSlotId = intent.getStringExtra(TeachingReminderReceiver.EXTRA_LESSON_SLOT_ID).orEmpty()
      )
    )
  }

  fun selectCalendarDate(dateIso: String) {
    uiState = uiState.copy(selectedCalendarDateIso = dateIso)
  }

  fun toggleSidebarParent(parent: GuruSidebarParent) {
    uiState = uiState.copy(
      expandedSidebarParent = if (uiState.expandedSidebarParent == parent) null else parent
    )
  }

  fun selectSidebarDestination(destination: GuruSidebarDestination) {
    uiState = uiState.copy(
      selectedSidebarDestination = destination,
      isCalendarScreenOpen = false,
      isNotificationPopupOpen = false,
      isSidebarOpen = false,
      expandedSidebarParent = when (destination) {
        GuruSidebarDestination.Tugas -> GuruSidebarParent.KinerjaGuru

        GuruSidebarDestination.Jadwal,
        GuruSidebarDestination.Mapel,
        GuruSidebarDestination.Ujian -> GuruSidebarParent.Akademik

        GuruSidebarDestination.InputNilai,
        GuruSidebarDestination.InputAbsensi,
        GuruSidebarDestination.Perizinan -> GuruSidebarParent.AktivitasHarian

        GuruSidebarDestination.Santri,
        GuruSidebarDestination.LaporanAbsensi,
        GuruSidebarDestination.LaporanUTS,
        GuruSidebarDestination.LaporanPekanan,
        GuruSidebarDestination.LaporanBulanan,
        GuruSidebarDestination.Rapor -> GuruSidebarParent.KelasSaya

        else -> null
      },
      isClaimSectionVisible = if (destination == GuruSidebarDestination.Mapel) uiState.isClaimSectionVisible else false,
      selectedClaimSubjectIds = if (destination == GuruSidebarDestination.Mapel) uiState.selectedClaimSubjectIds else emptySet()
    )
  }

  fun toggleClaimSection() {
    uiState = uiState.copy(isClaimSectionVisible = !uiState.isClaimSectionVisible)
  }

  fun toggleClaimSubject(subjectId: String) {
    val current = uiState.selectedClaimSubjectIds
    val next = if (current.contains(subjectId)) current - subjectId else current + subjectId
    uiState = uiState.copy(selectedClaimSubjectIds = next)
  }

  fun clearSelectedClaimSubjects() {
    uiState = uiState.copy(
      isClaimSectionVisible = false,
      selectedClaimSubjectIds = emptySet()
    )
  }

  fun claimSelectedSubjects() {
    val dashboard = uiState.dashboard ?: return
    val selectedIds = uiState.selectedClaimSubjectIds
    if (selectedIds.isEmpty()) return

    val selectedOffers = dashboard.availableSubjects.filter { selectedIds.contains(it.id) }
    if (selectedOffers.isEmpty()) return
    val nextPayload = dashboard.withQueuedMapelClaims(selectedOffers)

    viewModelScope.launch {
      cacheStore.writeDashboard(nextPayload)
      uiState = uiState.copy(
        dashboard = nextPayload,
        isClaimSectionVisible = false,
        selectedClaimSubjectIds = emptySet(),
        syncBanner = SyncBannerState("Mapel disimpan ke perangkat. Menyinkronkan perubahan ke server...", true)
      )
      syncPendingMapelClaims(showFeedback = true)
    }
  }

  fun logout() {
    viewModelScope.launch {
      sessionStore.clear()
      uiState = GuruAppUiState(
        destination = GuruDestination.Login,
        splashMessage = "Sesi diakhiri.",
        loginTeacherName = "",
        loginPassword = "",
        loginError = ""
      )
    }
  }

  suspend fun saveProfile(profile: GuruProfile): ProfileSaveOutcome {
    val dashboard = uiState.dashboard ?: return ProfileSaveOutcome(false, "Data profil belum siap.")
    val session = uiState.session
    val normalizedName = profile.name.trim().ifBlank { dashboard.teacherName }
    val nextProfile = profile.copy(
      name = normalizedName,
      username = session.teacherId.ifBlank { profile.username.trim() },
      address = profile.address.trim(),
      password = profile.password.trim(),
      phoneNumber = profile.phoneNumber.trim()
    )
    uiState = uiState.copy(syncBanner = SyncBannerState("Menyimpan profil ke server...", true))
    return when (
      val result = profileRemoteDataSource.updateProfile(
        teacherRowId = session.teacherRowId,
        teacherKaryawanId = session.teacherId,
        profile = nextProfile
      )
    ) {
      is GuruProfileSyncResult.Error -> {
        uiState = uiState.copy(syncBanner = SyncBannerState(result.message, false))
        ProfileSaveOutcome(false, result.message)
      }

      is GuruProfileSyncResult.Success -> {
        val nextDashboard = dashboard
          .copy(teacherName = result.profile.name.ifBlank { normalizedName })
          .withResolvedProfile(session = session, remoteProfile = result.profile)
        cacheStore.writeDashboard(nextDashboard)
        sessionStore.updateTeacherName(nextDashboard.teacherName)
        val nextSession = sessionStore.readSession()
        uiState = uiState.copy(
          dashboard = nextDashboard,
          session = nextSession,
          syncBanner = SyncBannerState("Profil berhasil disinkronkan dengan server.", false)
        )
        ProfileSaveOutcome(true, "Profil berhasil disimpan.")
      }
    }
  }

  suspend fun saveWaliSantriProfile(profile: WaliSantriProfile): SantriSaveOutcome {
    val dashboard = uiState.dashboard ?: return SantriSaveOutcome(false, "Data santri belum siap.")
    val draft = profile.copy(
      name = profile.name.trim(),
      nisn = profile.nisn.trim(),
      gender = profile.gender.trim(),
      studentPhone = profile.studentPhone.trim(),
      fatherName = profile.fatherName.trim(),
      fatherPhone = profile.fatherPhone.trim(),
      motherName = profile.motherName.trim(),
      motherPhone = profile.motherPhone.trim(),
      guardianName = profile.guardianName.trim(),
      guardianPhone = profile.guardianPhone.trim(),
      address = profile.address.trim(),
      note = profile.note.trim()
    )
    uiState = uiState.copy(syncBanner = SyncBannerState("Menyimpan data santri ke server...", true))

    return when (val result = waliSantriRemoteDataSource.updateWaliSantriProfile(draft)) {
      is GuruWaliSantriSaveResult.Error -> {
        uiState = uiState.copy(syncBanner = SyncBannerState(result.message, false))
        SantriSaveOutcome(false, result.message)
      }

      is GuruWaliSantriSaveResult.Success -> {
        val nextSnapshot = dashboard.waliSantriSnapshot.copy(
          students = dashboard.waliSantriSnapshot.students.map { student ->
            if (student.id == result.student.id) result.student else student
          },
          updatedAt = System.currentTimeMillis()
        )
        val nextDashboard = dashboard.copy(waliSantriSnapshot = nextSnapshot)
        cacheStore.writeDashboard(nextDashboard)
        uiState = uiState.copy(
          dashboard = nextDashboard,
          syncBanner = SyncBannerState(result.message, false)
        )
        SantriSaveOutcome(true, result.message)
      }
    }
  }

  suspend fun saveMonthlyReport(report: MonthlyReportItem): MonthlyReportSaveOutcome {
    val dashboard = uiState.dashboard ?: return MonthlyReportSaveOutcome(false, "Data laporan belum siap.")
    val snapshot = dashboard.monthlyReportSnapshot
    val session = uiState.session
    val nilaiAkhlakHalaqah = report.nilaiAkhlakHalaqah.trim()
    val nilaiIbadah = report.nilaiIbadah.trim()
    val nilaiKedisiplinan = report.nilaiKedisiplinan.trim()
    val nilaiKebersihan = report.nilaiKebersihan.trim()
    val nilaiAdab = report.nilaiAdab.trim()
    val draft = report.copy(
      period = report.period.trim(),
      guruId = report.guruId.trim().ifBlank {
        snapshot.guruId.ifBlank { session.teacherRowId.ifBlank { session.teacherId } }
      },
      classId = report.classId.trim(),
      studentId = report.studentId.trim(),
      nilaiAkhlak = report.nilaiAkhlak.trim(),
      predikat = monthlyReportPredicate(report.nilaiAkhlak),
      catatanWali = report.catatanWali.trim(),
      muhaffiz = report.muhaffiz.trim(),
      noHpMuhaffiz = report.noHpMuhaffiz.trim(),
      nilaiKehadiranHalaqah = report.nilaiKehadiranHalaqah.trim(),
      sakitHalaqah = report.sakitHalaqah.trim(),
      izinHalaqah = report.izinHalaqah.trim(),
      nilaiAkhlakHalaqah = nilaiAkhlakHalaqah,
      keteranganAkhlakHalaqah = monthlyReportPredicate(nilaiAkhlakHalaqah),
      nilaiUjianBulanan = report.nilaiUjianBulanan.trim(),
      keteranganUjianBulanan = report.keteranganUjianBulanan.trim(),
      nilaiTargetHafalan = report.nilaiTargetHafalan.trim(),
      keteranganTargetHafalan = report.keteranganTargetHafalan.trim(),
      nilaiCapaianHafalanBulanan = report.nilaiCapaianHafalanBulanan.trim(),
      keteranganCapaianHafalanBulanan = report.keteranganCapaianHafalanBulanan.trim(),
      keteranganJumlahHafalanBulanan = report.keteranganJumlahHafalanBulanan.trim(),
      nilaiJumlahHafalanHalaman = report.nilaiJumlahHafalanHalaman.trim(),
      nilaiJumlahHafalanJuz = report.nilaiJumlahHafalanJuz.trim(),
      catatanMuhaffiz = report.catatanMuhaffiz.trim(),
      musyrif = report.musyrif.trim(),
      noHpMusyrif = report.noHpMusyrif.trim(),
      nilaiKehadiranLiqaMuhasabah = report.nilaiKehadiranLiqaMuhasabah.trim(),
      sakitLiqaMuhasabah = report.sakitLiqaMuhasabah.trim(),
      izinLiqaMuhasabah = report.izinLiqaMuhasabah.trim(),
      nilaiIbadah = nilaiIbadah,
      keteranganIbadah = monthlyReportPredicate(nilaiIbadah),
      nilaiKedisiplinan = nilaiKedisiplinan,
      keteranganKedisiplinan = monthlyReportPredicate(nilaiKedisiplinan),
      nilaiKebersihan = nilaiKebersihan,
      keteranganKebersihan = monthlyReportPredicate(nilaiKebersihan),
      nilaiAdab = nilaiAdab,
      keteranganAdab = monthlyReportPredicate(nilaiAdab),
      prestasiKesantrian = report.prestasiKesantrian.trim(),
      pelanggaranKesantrian = report.pelanggaranKesantrian.trim(),
      catatanMusyrif = report.catatanMusyrif.trim()
    )
    if (draft.period.isBlank() || draft.studentId.isBlank()) {
      return MonthlyReportSaveOutcome(false, "Periode dan santri laporan belum lengkap.")
    }

    uiState = uiState.copy(syncBanner = SyncBannerState("Menyimpan laporan bulanan ke server...", true))
    return when (val result = monthlyReportRemoteDataSource.saveMonthlyReport(draft)) {
      is GuruMonthlyReportSaveResult.Error -> {
        uiState = uiState.copy(syncBanner = SyncBannerState(result.message, false))
        MonthlyReportSaveOutcome(false, result.message)
      }

      is GuruMonthlyReportSaveResult.Success -> {
        val savedReport = result.report
        val updatedReports = (snapshot.reports.filterNot { existing ->
          if (savedReport.id.isNotBlank()) {
            existing.id == savedReport.id
          } else {
            existing.period == savedReport.period && existing.studentId == savedReport.studentId
          }
        } + savedReport).sortedWith(
          compareByDescending<MonthlyReportItem> { it.period }
            .thenBy { it.studentId }
        )
        val nextSnapshot = snapshot.copy(
          reports = updatedReports,
          updatedAt = System.currentTimeMillis(),
          missingTable = false
        )
        val nextDashboard = dashboard.copy(monthlyReportSnapshot = nextSnapshot)
        cacheStore.writeDashboard(nextDashboard)
        uiState = uiState.copy(
          dashboard = nextDashboard,
          syncBanner = SyncBannerState(result.message, false)
        )
        MonthlyReportSaveOutcome(true, result.message, savedReport)
      }
    }
  }

  suspend fun saveMonthlyExtracurricularReports(
    reports: List<MonthlyExtracurricularReport>
  ): MonthlyExtracurricularSaveOutcome {
    val dashboard = uiState.dashboard ?: return MonthlyExtracurricularSaveOutcome(false, "Data laporan belum siap.")
    val snapshot = dashboard.monthlyReportSnapshot
    val session = uiState.session
    val updatedBy = session.teacherRowId.ifBlank { session.teacherId }
    uiState = uiState.copy(syncBanner = SyncBannerState("Menyimpan laporan ekskul bulanan...", true))
    return when (val result = monthlyReportRemoteDataSource.saveMonthlyExtracurricularReports(reports, updatedBy)) {
      is GuruMonthlyExtracurricularSaveResult.Error -> {
        uiState = uiState.copy(syncBanner = SyncBannerState(result.message, false))
        MonthlyExtracurricularSaveOutcome(false, result.message)
      }

      is GuruMonthlyExtracurricularSaveResult.Success -> {
        val savedReports = result.reports
        val updatedReports = (snapshot.extracurricularReports.filterNot { existing ->
          savedReports.any { saved ->
            existing.period == saved.period &&
              existing.studentId == saved.studentId &&
              existing.activityId == saved.activityId
          }
        } + savedReports).sortedWith(
          compareBy<MonthlyExtracurricularReport> { it.period }
            .thenBy { it.activityName.lowercase(Locale.ROOT) }
            .thenBy { it.studentId }
        )
        val nextSnapshot = snapshot.copy(
          extracurricularReports = updatedReports,
          updatedAt = System.currentTimeMillis()
        )
        val nextDashboard = dashboard.copy(monthlyReportSnapshot = nextSnapshot)
        cacheStore.writeDashboard(nextDashboard)
        uiState = uiState.copy(
          dashboard = nextDashboard,
          syncBanner = SyncBannerState(result.message, false)
        )
        MonthlyExtracurricularSaveOutcome(true, result.message, savedReports)
      }
    }
  }

  suspend fun saveUtsReportOverride(override: UtsReportOverride): UtsReportSaveOutcome {
    val dashboard = uiState.dashboard ?: return UtsReportSaveOutcome(false, "Data laporan PTS belum siap.")
    val snapshot = dashboard.utsReportSnapshot
    val session = uiState.session
    val draft = override.copy(
      guruId = override.guruId.trim().ifBlank {
        snapshot.guruId.ifBlank { session.teacherRowId.ifBlank { session.teacherId } }
      },
      semesterId = override.semesterId.trim(),
      classId = override.classId.trim(),
      studentId = override.studentId.trim()
    )
    if (draft.semesterId.isBlank() || draft.studentId.isBlank()) {
      return UtsReportSaveOutcome(false, "Semester dan santri laporan PTS belum lengkap.")
    }

    uiState = uiState.copy(syncBanner = SyncBannerState("Menyimpan laporan PTS ke server...", true))
    return when (val result = utsReportRemoteDataSource.saveOverride(draft)) {
      is GuruUtsReportSaveResult.Error -> {
        uiState = uiState.copy(syncBanner = SyncBannerState(result.message, false))
        UtsReportSaveOutcome(false, result.message)
      }

      is GuruUtsReportSaveResult.Success -> {
        val nextOverrides = if (result.override == null) {
          snapshot.overrides.filterNot {
            it.studentId == result.deletedStudentId && it.semesterId == result.semesterId
          }
        } else {
          (snapshot.overrides.filterNot { existing ->
            existing.studentId == result.override.studentId &&
              existing.semesterId == result.override.semesterId
          } + result.override).sortedWith(
            compareBy<UtsReportOverride> { it.semesterId }.thenBy { it.studentId }
          )
        }
        val nextSnapshot = snapshot.copy(
          overrides = nextOverrides,
          updatedAt = System.currentTimeMillis(),
          missingOverrideTable = false
        )
        val nextDashboard = dashboard.copy(utsReportSnapshot = nextSnapshot)
        cacheStore.writeDashboard(nextDashboard)
        uiState = uiState.copy(
          dashboard = nextDashboard,
          syncBanner = SyncBannerState(result.message, false)
        )
        UtsReportSaveOutcome(
          success = true,
          message = result.message,
          override = result.override,
          deletedStudentId = result.deletedStudentId,
          semesterId = result.semesterId
        )
      }
    }
  }

  private fun monthlyReportPredicate(value: String): String {
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

  suspend fun loadMutabaahSnapshot(referenceDate: LocalDate): MutabaahSnapshot? {
    val dashboard = uiState.dashboard ?: return null
    val requestedPeriod = referenceDate.toString().take(7)
    val cachedSnapshot = dashboard.mutabaahSnapshot.takeIf {
      it.period == requestedPeriod && it.updatedAt > 0L
    }
    if (cachedSnapshot != null) return cachedSnapshot

    val remoteSnapshot = mutabaahRemoteDataSource.fetchMutabaahSnapshot(
      teacherRowId = uiState.session.teacherRowId,
      teacherKaryawanId = uiState.session.teacherId,
      referenceDate = referenceDate
    ) ?: return cachedSnapshot

    val nextDashboard = dashboard.withMutabaahSnapshot(remoteSnapshot)
    cacheStore.writeDashboard(nextDashboard)
    uiState = uiState.copy(dashboard = nextDashboard)
    return remoteSnapshot
  }

  suspend fun saveMutabaahStatus(
    templateId: String,
    dateIso: String,
    isDone: Boolean
  ): MutabaahSaveOutcome {
    val dashboard = uiState.dashboard ?: return MutabaahSaveOutcome(false, "Data mutabaah belum siap.")
    val snapshot = dashboard.mutabaahSnapshot
    val guruId = snapshot.guruId.ifBlank { uiState.session.teacherRowId.ifBlank { uiState.session.teacherId } }
    if (guruId.isBlank()) return MutabaahSaveOutcome(false, "ID guru belum tersedia.")

    val localSubmission = MutabaahSubmission(
      templateId = templateId,
      dateIso = dateIso,
      status = if (isDone) "selesai" else "belum",
      updatedAt = System.currentTimeMillis().toString(),
      syncState = "syncing"
    )
    updateMutabaahSubmission(localSubmission)

    return when (
      val result = mutabaahRemoteDataSource.saveMutabaahStatus(
        guruId = guruId,
        templateId = templateId,
        dateIso = dateIso,
        isDone = isDone
      )
    ) {
      is GuruMutabaahSaveResult.Success -> {
        updateMutabaahSubmission(result.submission)
        MutabaahSaveOutcome(true, "Mutabaah tersimpan otomatis.")
      }

      is GuruMutabaahSaveResult.Error -> {
        updateMutabaahSubmission(localSubmission.copy(syncState = "pending"))
        MutabaahSaveOutcome(false, "Disimpan lokal. Sinkronisasi mutabaah menunggu koneksi.")
      }
    }
  }

  private suspend fun updateMutabaahSubmission(submission: MutabaahSubmission) {
    val dashboard = uiState.dashboard ?: return
    val snapshot = dashboard.mutabaahSnapshot
    val nextSubmissions = snapshot.submissions
      .filterNot { it.templateId == submission.templateId && it.dateIso == submission.dateIso } + submission
    val nextDashboard = dashboard.withMutabaahSnapshot(
      snapshot.copy(
        submissions = nextSubmissions,
        updatedAt = System.currentTimeMillis()
      )
    )
    cacheStore.writeDashboard(nextDashboard)
    uiState = uiState.copy(dashboard = nextDashboard)
  }

  suspend fun loadLeaveRequestSnapshot(): LeaveRequestSnapshot? {
    val dashboard = uiState.dashboard ?: return null
    val session = uiState.session
    val cachedSnapshot = dashboard.leaveRequestSnapshot.takeIf {
      it.updatedAt > 0L && (
        it.guruId.isBlank() ||
          it.guruId == session.teacherRowId ||
          it.guruId == session.teacherId
        )
    }
    if (cachedSnapshot != null && cachedSnapshot.requests.isNotEmpty()) return cachedSnapshot

    val remoteSnapshot = leaveRequestRemoteDataSource.fetchLeaveRequestSnapshot(
      teacherRowId = session.teacherRowId,
      teacherKaryawanId = session.teacherId
    ) ?: return cachedSnapshot

    val nextDashboard = dashboard.withLeaveRequestSnapshot(remoteSnapshot)
    cacheStore.writeDashboard(nextDashboard)
    uiState = uiState.copy(dashboard = nextDashboard)
    return remoteSnapshot
  }

  suspend fun submitLeaveRequest(
    startDateIso: String,
    endDateIso: String,
    purpose: String
  ): LeaveRequestSaveOutcome {
    val dashboard = uiState.dashboard ?: return LeaveRequestSaveOutcome(false, "Data perizinan belum siap.")
    uiState = uiState.copy(syncBanner = SyncBannerState("Mengirim pengajuan izin ke server...", true))

    return when (
      val result = leaveRequestRemoteDataSource.submitLeaveRequest(
        teacherRowId = uiState.session.teacherRowId,
        teacherKaryawanId = uiState.session.teacherId,
        startDateIso = startDateIso,
        endDateIso = endDateIso,
        purpose = purpose
      )
    ) {
      is GuruLeaveRequestSaveResult.Error -> {
        uiState = uiState.copy(syncBanner = SyncBannerState(result.message, false))
        LeaveRequestSaveOutcome(false, result.message)
      }

      is GuruLeaveRequestSaveResult.Success -> {
        val nextDashboard = dashboard.withLeaveRequestSnapshot(result.snapshot)
        cacheStore.writeDashboard(nextDashboard)
        uiState = uiState.copy(
          dashboard = nextDashboard,
          syncBanner = SyncBannerState("Pengajuan izin berhasil dikirim.", false)
        )
        LeaveRequestSaveOutcome(true, "Pengajuan izin berhasil dikirim.")
      }
    }
  }

  suspend fun deleteLeaveRequest(requestId: String): LeaveRequestSaveOutcome {
    val dashboard = uiState.dashboard ?: return LeaveRequestSaveOutcome(false, "Data perizinan belum siap.")
    uiState = uiState.copy(syncBanner = SyncBannerState("Memperbarui daftar perizinan...", true))

    return when (
      val result = leaveRequestRemoteDataSource.deleteLeaveRequest(
        teacherRowId = uiState.session.teacherRowId,
        teacherKaryawanId = uiState.session.teacherId,
        requestId = requestId
      )
    ) {
      is GuruLeaveRequestSaveResult.Error -> {
        uiState = uiState.copy(syncBanner = SyncBannerState(result.message, false))
        LeaveRequestSaveOutcome(false, result.message)
      }

      is GuruLeaveRequestSaveResult.Success -> {
        val nextDashboard = dashboard.withLeaveRequestSnapshot(result.snapshot)
        cacheStore.writeDashboard(nextDashboard)
        uiState = uiState.copy(
          dashboard = nextDashboard,
          syncBanner = SyncBannerState("Pengajuan izin berhasil diperbarui.", false)
        )
        LeaveRequestSaveOutcome(true, "Pengajuan izin berhasil diperbarui.")
      }
    }
  }

  suspend fun loadMapelAttendance(
    distribusiId: String,
    subject: SubjectOverview
  ): MapelAttendanceSnapshot? {
    val dashboard = uiState.dashboard ?: return null
    val cachedSnapshot = dashboard.attendanceSnapshots.firstOrNull { it.distribusiId == distribusiId }
    if (cachedSnapshot != null && isAttendanceSnapshotComplete(cachedSnapshot)) return cachedSnapshot

    val remoteSnapshot = mapelAttendanceRemoteDataSource.fetchAttendanceSnapshot(
      distribusiId = distribusiId,
      fallbackTitle = subject.title,
      fallbackClassName = subject.className
    ) ?: return cachedSnapshot

    val nextDashboard = dashboard.withAttendanceSnapshot(remoteSnapshot)
    cacheStore.writeDashboard(nextDashboard)
    uiState = uiState.copy(dashboard = nextDashboard)
    return remoteSnapshot
  }

  suspend fun saveMapelAttendanceChanges(
    distribusiId: String,
    subject: SubjectOverview,
    studentId: String,
    changes: List<AttendanceHistoryEntry>
  ): AttendanceSaveOutcome {
    if (changes.isEmpty()) return AttendanceSaveOutcome(false, "Tidak ada perubahan absensi.")

    for (change in changes) {
      val result = runCatching {
        mapelAttendanceRemoteDataSource.saveAttendanceStatus(
          distribusiId = distribusiId,
          santriId = studentId,
          dateIso = change.dateIso,
          status = change.status,
          rowIds = change.rowIds,
          lessonSlotId = change.lessonSlotId,
          patronMateri = change.patronMateri,
          teacherRowId = uiState.session.teacherRowId,
          teacherKaryawanId = uiState.session.teacherId
        )
      }.getOrElse {
        GuruMapelAttendanceSaveResult.Error("Koneksi belum tersedia.")
      }
      when (result) {
        is GuruMapelAttendanceSaveResult.Error -> {
          return queuePendingMapelAttendance(
            distribusiId = distribusiId,
            subject = subject,
            changesByStudent = mapOf(studentId to changes),
            sourceTeacherId = "",
            substituteNote = "",
            fallbackMessage = result.message
          )
        }

        is GuruMapelAttendanceSaveResult.SubmittedForReview -> {
          return AttendanceSaveOutcome(true, result.message)
        }

        GuruMapelAttendanceSaveResult.Success -> Unit
      }
    }

    val refreshedSnapshot = mapelAttendanceRemoteDataSource.fetchAttendanceSnapshot(
      distribusiId = distribusiId,
      fallbackTitle = subject.title,
      fallbackClassName = subject.className
    )
    if (refreshedSnapshot != null) {
      val dashboard = uiState.dashboard ?: return AttendanceSaveOutcome(true, "Absensi berhasil diperbarui.")
      val nextDashboard = dashboard.withAttendanceSnapshot(refreshedSnapshot)
      cacheStore.writeDashboard(nextDashboard)
      uiState = uiState.copy(
        dashboard = nextDashboard,
        syncBanner = SyncBannerState("Absensi berhasil diperbarui.", false)
      )
    }

    return AttendanceSaveOutcome(true, "Absensi berhasil diperbarui.")
  }

  suspend fun saveMapelAttendanceBatch(
    distribusiId: String,
    subject: SubjectOverview,
    changesByStudent: Map<String, List<AttendanceHistoryEntry>>,
    sourceTeacherId: String = "",
    substituteNote: String = ""
  ): AttendanceSaveOutcome {
    val normalizedChanges = changesByStudent
      .mapKeys { it.key.trim() }
      .filterKeys { it.isNotBlank() }
      .mapValues { (_, changes) ->
        changes.filter { it.dateIso.isNotBlank() && it.status.isNotBlank() }
      }
      .filterValues { it.isNotEmpty() }

    if (normalizedChanges.isEmpty()) {
      return AttendanceSaveOutcome(false, "Tidak ada perubahan absensi.")
    }

    val result = runCatching {
      mapelAttendanceRemoteDataSource.saveAttendanceStatuses(
        distribusiId = distribusiId,
        changesByStudent = normalizedChanges,
        teacherRowId = uiState.session.teacherRowId,
        teacherKaryawanId = uiState.session.teacherId,
        sourceTeacherId = sourceTeacherId,
        substituteNote = substituteNote
      )
    }.getOrElse {
      GuruMapelAttendanceSaveResult.Error("Koneksi belum tersedia.")
    }
    when (result) {
      is GuruMapelAttendanceSaveResult.Error -> {
        return queuePendingMapelAttendance(
          distribusiId = distribusiId,
          subject = subject,
          changesByStudent = normalizedChanges,
          sourceTeacherId = sourceTeacherId,
          substituteNote = substituteNote,
          fallbackMessage = result.message
        )
      }

      is GuruMapelAttendanceSaveResult.SubmittedForReview -> {
        uiState = uiState.copy(
          syncBanner = SyncBannerState(result.message, false)
        )
        refreshNotificationsOnly()
        return AttendanceSaveOutcome(true, result.message)
      }

      GuruMapelAttendanceSaveResult.Success -> Unit
    }

    val refreshedSnapshot = mapelAttendanceRemoteDataSource.fetchAttendanceSnapshot(
      distribusiId = distribusiId,
      fallbackTitle = subject.title,
      fallbackClassName = subject.className
    )
    if (refreshedSnapshot != null) {
      val dashboard = uiState.dashboard ?: return AttendanceSaveOutcome(true, "Absensi berhasil diperbarui.")
      val nextDashboard = dashboard.withAttendanceSnapshot(refreshedSnapshot)
      cacheStore.writeDashboard(nextDashboard)
      uiState = uiState.copy(
        dashboard = nextDashboard,
        syncBanner = SyncBannerState("Absensi berhasil diperbarui.", false)
      )
    }

    return AttendanceSaveOutcome(true, "Absensi berhasil diperbarui.")
  }

  suspend fun deleteMapelAttendanceRows(
    distribusiId: String,
    subject: SubjectOverview,
    rowIds: List<String>
  ): AttendanceSaveOutcome {
    val normalizedIds = rowIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    if (normalizedIds.isEmpty()) {
      return AttendanceSaveOutcome(false, "Data absensi belum memiliki ID untuk dihapus.")
    }

    when (val result = mapelAttendanceRemoteDataSource.deleteAttendanceRows(normalizedIds)) {
      is GuruMapelAttendanceSaveResult.Error -> {
        return AttendanceSaveOutcome(false, result.message)
      }

      is GuruMapelAttendanceSaveResult.SubmittedForReview -> {
        return AttendanceSaveOutcome(false, result.message)
      }

      GuruMapelAttendanceSaveResult.Success -> Unit
    }

    val refreshedSnapshot = mapelAttendanceRemoteDataSource.fetchAttendanceSnapshot(
      distribusiId = distribusiId,
      fallbackTitle = subject.title,
      fallbackClassName = subject.className
    )
    if (refreshedSnapshot != null) {
      val dashboard = uiState.dashboard ?: return AttendanceSaveOutcome(true, "Absensi berhasil dihapus.")
      val nextDashboard = dashboard.withAttendanceSnapshot(refreshedSnapshot)
      cacheStore.writeDashboard(nextDashboard)
      uiState = uiState.copy(
        dashboard = nextDashboard,
        syncBanner = SyncBannerState("Absensi berhasil dihapus.", false)
      )
    }

    return AttendanceSaveOutcome(true, "Absensi berhasil dihapus.")
  }

  suspend fun sendMapelAttendanceDelegation(
    distribusiId: String,
    subject: SubjectOverview,
    dateIso: String,
    lessonSlotIds: List<String>,
    substituteTeacherId: String,
    note: String
  ): AttendanceSaveOutcome {
    return when (
      val result = mapelAttendanceRemoteDataSource.sendSubstituteDelegation(
        distribusiId = distribusiId,
        dateIso = dateIso,
        lessonSlotIds = lessonSlotIds,
        substituteTeacherId = substituteTeacherId,
        note = note,
        teacherRowId = uiState.session.teacherRowId
      )
    ) {
      is GuruMapelAttendanceSaveResult.Error -> {
        AttendanceSaveOutcome(false, result.message)
      }

      is GuruMapelAttendanceSaveResult.SubmittedForReview -> {
        AttendanceSaveOutcome(false, result.message)
      }

      GuruMapelAttendanceSaveResult.Success -> {
        uiState = uiState.copy(
          syncBanner = SyncBannerState(
            "Amanat absensi ${subject.title} berhasil dikirim.",
            false
          )
        )
        AttendanceSaveOutcome(true, "Amanat absensi berhasil dikirim ke guru pengganti.")
      }
    }
  }

  suspend fun loadSubstituteTeacherContext(
    teacher: TeacherOption,
    dateIso: String
  ): Pair<List<SubjectOverview>, List<CalendarEvent>> {
    val normalizedDate = dateIso.take(10)
    val cacheKey = "${teacher.id}|${teacher.employeeId}|$normalizedDate"
    substituteTeacherContextCache[cacheKey]?.let { return it }

    val date = runCatching { LocalDate.parse(normalizedDate) }.getOrDefault(LocalDate.now())
    val context = teachingScheduleRemoteDataSource.fetchTeachingContextForDate(
      teacherRowId = teacher.id,
      teacherKaryawanId = teacher.employeeId,
      date = date
    ) ?: (emptyList<SubjectOverview>() to emptyList())

    substituteTeacherContextCache[cacheKey] = context
    return context
  }

  suspend fun loadDelegatedAttendanceContext(
    dateIso: String
  ): Pair<List<SubjectOverview>, List<CalendarEvent>> {
    return mapelAttendanceRemoteDataSource.fetchDelegatedAttendanceContext(
      teacherRowId = uiState.session.teacherRowId,
      teacherKaryawanId = uiState.session.teacherId,
      dateIso = dateIso
    ) ?: (emptyList<SubjectOverview>() to emptyList())
  }

  suspend fun loadAttendanceApprovalRequest(
    submissionId: String
  ): AttendanceApprovalRequest? {
    return mapelAttendanceRemoteDataSource.fetchAttendanceApprovalRequest(
      submissionId = submissionId,
      teacherRowId = uiState.session.teacherRowId,
      teacherKaryawanId = uiState.session.teacherId
    )
  }

  suspend fun reviewAttendanceApproval(
    submissionId: String,
    approve: Boolean,
    reviewerNote: String
  ): AttendanceSaveOutcome {
    val detail = mapelAttendanceRemoteDataSource.fetchAttendanceApprovalRequest(
      submissionId = submissionId,
      teacherRowId = uiState.session.teacherRowId,
      teacherKaryawanId = uiState.session.teacherId
    ) ?: return AttendanceSaveOutcome(false, "Pengajuan absensi tidak ditemukan.")

    return when (
      val result = mapelAttendanceRemoteDataSource.reviewAttendanceSubmission(
        submissionId = submissionId,
        teacherRowId = uiState.session.teacherRowId,
        teacherKaryawanId = uiState.session.teacherId,
        approve = approve,
        reviewerNote = reviewerNote
      )
    ) {
      is GuruMapelAttendanceReviewResult.Error -> {
        AttendanceSaveOutcome(false, result.message)
      }

      is GuruMapelAttendanceReviewResult.Success -> {
        val dashboard = uiState.dashboard
        var nextDashboard = dashboard
        if (dashboard != null && approve && detail.distribusiId.isNotBlank()) {
          val refreshedSnapshot = mapelAttendanceRemoteDataSource.fetchAttendanceSnapshot(
            distribusiId = detail.distribusiId,
            fallbackTitle = detail.subjectTitle,
            fallbackClassName = detail.className
          )
          if (refreshedSnapshot != null) {
            nextDashboard = dashboard.withAttendanceSnapshot(refreshedSnapshot)
          }
        }
        if (nextDashboard != null) {
          val notifications = guruNotificationRemoteDataSource.fetchNotifications(
            teacherRowId = uiState.session.teacherRowId,
            teacherKaryawanId = uiState.session.teacherId
          )
          if (notifications != null) {
            nextDashboard = nextDashboard.withNotifications(
              mergeNotificationReadState(nextDashboard.notifications, notifications)
            )
          }
          cacheStore.writeDashboard(nextDashboard)
          uiState = uiState.copy(
            dashboard = nextDashboard,
            syncBanner = SyncBannerState(result.message, false)
          )
        } else {
          uiState = uiState.copy(syncBanner = SyncBannerState(result.message, false))
        }
        AttendanceSaveOutcome(true, result.message)
      }
    }
  }

  suspend fun loadMapelScores(
    distribusiId: String,
    subject: SubjectOverview
  ): MapelScoreSnapshot? {
    val dashboard = uiState.dashboard ?: return null
    val cachedSnapshot = dashboard.scoreSnapshots.firstOrNull { it.distribusiId == distribusiId }
    if (cachedSnapshot != null && !isScoreSnapshotMissingDetailRows(cachedSnapshot)) {
      return cachedSnapshot
    }

    val remoteSnapshot = mapelScoreRemoteDataSource.fetchScoreSnapshot(
      distribusiId = distribusiId,
      fallbackTitle = subject.title,
      fallbackClassName = subject.className
    ) ?: return cachedSnapshot

    val nextDashboard = dashboard.withScoreSnapshot(remoteSnapshot)
    cacheStore.writeDashboard(nextDashboard)
    uiState = uiState.copy(dashboard = nextDashboard)
    return remoteSnapshot
  }

  suspend fun saveMapelScoreChanges(
    distribusiId: String,
    subject: SubjectOverview,
    student: ScoreStudent
  ): ScoreSaveOutcome {
    val result = runCatching {
      if (student.pendingDetailMetricKey.isNotBlank()) {
        mapelScoreRemoteDataSource.saveScoreDetailRows(
          distribusiId = distribusiId,
          santriId = student.id,
          guruId = uiState.session.teacherRowId.ifBlank { uiState.session.teacherId },
          metricKey = student.pendingDetailMetricKey,
          rows = student.detailRowsByMetric[student.pendingDetailMetricKey].orEmpty(),
          deletedIds = student.pendingDeletedDetailIds
        )
      } else {
        mapelScoreRemoteDataSource.saveScoreDraft(
          distribusiId = distribusiId,
          santriId = student.id,
          draft = ScoreDraftPayload(
            rowId = student.rowId,
            nilaiTugas = student.nilaiTugas,
            nilaiUlanganHarian = student.nilaiUlanganHarian,
            nilaiPts = student.nilaiPts,
            nilaiPas = student.nilaiPas,
            nilaiKehadiran = student.nilaiKehadiran,
            nilaiKeterampilan = student.nilaiKeterampilan
          )
        )
      }
    }.getOrElse {
      GuruMapelScoreSaveResult.Error("Koneksi belum tersedia.")
    }

    if (result is GuruMapelScoreSaveResult.Error) {
      return queuePendingMapelScore(
        distribusiId = distribusiId,
        subject = subject,
        students = listOf(student),
        metricKey = student.pendingDetailMetricKey,
        fallbackMessage = result.message
      )
    }

    val refreshedSnapshot = mapelScoreRemoteDataSource.fetchScoreSnapshot(
      distribusiId = distribusiId,
      fallbackTitle = subject.title,
      fallbackClassName = subject.className
    )
    if (refreshedSnapshot != null) {
      val dashboard = uiState.dashboard ?: return ScoreSaveOutcome(true, "Nilai berhasil diperbarui.")
      val nextDashboard = dashboard.withScoreSnapshot(refreshedSnapshot)
      cacheStore.writeDashboard(nextDashboard)
      uiState = uiState.copy(
        dashboard = nextDashboard,
        syncBanner = SyncBannerState("Nilai berhasil diperbarui.", false)
      )
    }

    return ScoreSaveOutcome(true, "Nilai berhasil diperbarui.")
  }

  suspend fun saveMapelScoreChangesBatch(
    distribusiId: String,
    subject: SubjectOverview,
    students: List<ScoreStudent>
  ): ScoreSaveOutcome {
    val normalizedStudents = students.filter { it.id.isNotBlank() }
    if (normalizedStudents.isEmpty()) {
      return ScoreSaveOutcome(false, "Tidak ada nilai yang perlu disimpan.")
    }

    val metricKeys = normalizedStudents
      .map { it.pendingDetailMetricKey }
      .filter { it.isNotBlank() }
      .distinct()
    if (metricKeys.size != 1 || normalizedStudents.any { it.pendingDetailMetricKey.isBlank() }) {
      return ScoreSaveOutcome(false, "Simpan batch hanya tersedia untuk input rincian nilai.")
    }

    val metricKey = metricKeys.first()
    val result = runCatching {
      mapelScoreRemoteDataSource.saveScoreDetailRowsBatch(
        distribusiId = distribusiId,
        guruId = uiState.session.teacherRowId.ifBlank { uiState.session.teacherId },
        metricKey = metricKey,
        rowsBySantriId = normalizedStudents.associate { student ->
          student.id to student.detailRowsByMetric[metricKey].orEmpty()
        },
        deletedIdsBySantriId = normalizedStudents.associate { student ->
          student.id to student.pendingDeletedDetailIds
        }
      )
    }.getOrElse {
      GuruMapelScoreSaveResult.Error("Koneksi belum tersedia.")
    }

    if (result is GuruMapelScoreSaveResult.Error) {
      return queuePendingMapelScore(
        distribusiId = distribusiId,
        subject = subject,
        students = normalizedStudents,
        metricKey = metricKey,
        fallbackMessage = result.message
      )
    }

    val refreshedSnapshot = mapelScoreRemoteDataSource.fetchScoreSnapshot(
      distribusiId = distribusiId,
      fallbackTitle = subject.title,
      fallbackClassName = subject.className
    )
    if (refreshedSnapshot != null) {
      val dashboard = uiState.dashboard ?: return ScoreSaveOutcome(true, "Nilai berhasil diperbarui.")
      val nextDashboard = dashboard.withScoreSnapshot(refreshedSnapshot)
      cacheStore.writeDashboard(nextDashboard)
      uiState = uiState.copy(
        dashboard = nextDashboard,
        syncBanner = SyncBannerState("Nilai berhasil diperbarui.", false)
      )
    }

    return ScoreSaveOutcome(true, "Nilai berhasil diperbarui.")
  }

  suspend fun loadMapelPatronMateri(
    distribusiId: String,
    subject: SubjectOverview
  ): MapelPatronMateriSnapshot? {
    val dashboard = uiState.dashboard ?: return null
    val cachedSnapshot = dashboard.patronMateriSnapshots.firstOrNull { it.distribusiId == distribusiId }

    var remoteSnapshot = mapelPatronMateriRemoteDataSource.fetchPatronMateriSnapshot(
      distribusiId = distribusiId,
      fallbackTitle = subject.title,
      fallbackClassName = subject.className
    ) ?: return cachedSnapshot

    if (remoteSnapshot.containsRecoveredText) {
      val repairResult = mapelPatronMateriRemoteDataSource.savePatronMateri(
        distribusiId = distribusiId,
        items = remoteSnapshot.items,
        guruId = uiState.session.teacherRowId.ifBlank { uiState.session.teacherId }
      )
      if (repairResult is GuruMapelPatronMateriSaveResult.Success) {
        remoteSnapshot = mapelPatronMateriRemoteDataSource.fetchPatronMateriSnapshot(
          distribusiId = distribusiId,
          fallbackTitle = subject.title,
          fallbackClassName = subject.className
        ) ?: remoteSnapshot.copy(containsRecoveredText = false)
      }
    }

    val nextDashboard = dashboard.withPatronMateriSnapshot(remoteSnapshot)
    cacheStore.writeDashboard(nextDashboard)
    uiState = uiState.copy(dashboard = nextDashboard)
    return remoteSnapshot
  }

  suspend fun saveMapelPatronMateri(
    distribusiId: String,
    subject: SubjectOverview,
    items: List<PatronMateriItem>
  ): PatronMateriSaveOutcome {
    val normalizedItems = items
      .mapIndexedNotNull { index, item ->
        val text = item.text.trim()
        if (text.isBlank()) null else PatronMateriItem("materi-${index + 1}-${text.hashCode()}", text)
      }
      .distinctBy { it.text.lowercase() }

    val dashboard = uiState.dashboard
    if (dashboard != null) {
      val localSnapshot = MapelPatronMateriSnapshot(
        distribusiId = distribusiId,
        subjectTitle = subject.title,
        className = subject.className,
        items = normalizedItems,
        updatedAt = System.currentTimeMillis()
      )
      val nextDashboard = dashboard.withPatronMateriSnapshot(localSnapshot)
      cacheStore.writeDashboard(nextDashboard)
      uiState = uiState.copy(dashboard = nextDashboard)
    }

    val result = mapelPatronMateriRemoteDataSource.savePatronMateri(
      distribusiId = distribusiId,
      items = normalizedItems,
      guruId = uiState.session.teacherRowId.ifBlank { uiState.session.teacherId }
    )

    if (result is GuruMapelPatronMateriSaveResult.Error) {
      return PatronMateriSaveOutcome(false, result.message)
    }

    val refreshedSnapshot = mapelPatronMateriRemoteDataSource.fetchPatronMateriSnapshot(
      distribusiId = distribusiId,
      fallbackTitle = subject.title,
      fallbackClassName = subject.className
    )
    if (refreshedSnapshot != null) {
      val currentDashboard = uiState.dashboard ?: return PatronMateriSaveOutcome(true, "Patron materi berhasil diperbarui.")
      val nextDashboard = currentDashboard.withPatronMateriSnapshot(refreshedSnapshot)
      cacheStore.writeDashboard(nextDashboard)
      uiState = uiState.copy(
        dashboard = nextDashboard,
        syncBanner = SyncBannerState("Patron materi berhasil diperbarui.", false)
      )
    }

    return PatronMateriSaveOutcome(true, "Patron materi berhasil diperbarui.")
  }

  fun refreshFromServer(force: Boolean = false) {
    val currentDashboard = uiState.dashboard ?: return
    if (uiState.isBusy) return
    if (force) substituteTeacherContextCache.clear()
    val shouldRefresh = force || isRefreshDue(currentDashboard.lastServerRefreshAt)
    if (!shouldRefresh) {
      uiState = uiState.copy(
        syncBanner = SyncBannerState("Data masih segar. Belum perlu refresh server.", false)
      )
      return
    }

    viewModelScope.launch {
      uiState = uiState.copy(syncBanner = SyncBannerState("Mengambil pembaruan dari server...", true))
      delay(300)
      val mapelSyncOutcome = syncPendingMapelClaims(showFeedback = false)
      val offlineSyncOutcome = syncPendingOfflineChanges(showFeedback = false)
      val baseDashboard = uiState.dashboard ?: currentDashboard
      val teacherRowId = uiState.session.teacherRowId
      val teacherKaryawanId = uiState.session.teacherId
      val remoteProfileDeferred = async {
        profileRemoteDataSource.fetchProfile(
          teacherRowId = teacherRowId,
          teacherKaryawanId = teacherKaryawanId
        )
      }
      val calendarEventsDeferred = async {
        kalenderAkademikRemoteDataSource.fetchCalendarEvents(
          teacherRowId = teacherRowId,
          teacherKaryawanId = teacherKaryawanId
        )
      }
      val mapelPayloadDeferred = async {
        mapelRemoteDataSource.fetchMapelPayload(
          teacherRowId = teacherRowId,
          teacherKaryawanId = teacherKaryawanId
        )
      }
      val teachingScheduleEventsDeferred = async {
        teachingScheduleRemoteDataSource.fetchTeachingEvents(
          teacherRowId = teacherRowId,
          teacherKaryawanId = teacherKaryawanId,
          startDate = LocalDate.now().minusDays(14),
          endDate = LocalDate.now().plusDays(120)
        )
      }
      val notificationsDeferred = async {
        guruNotificationRemoteDataSource.fetchNotifications(
          teacherRowId = teacherRowId,
          teacherKaryawanId = teacherKaryawanId
        )
      }
      val substituteTeacherOptionsDeferred = async {
        teacherOptionsRemoteDataSource.fetchSubstituteTeacherOptions()
      }
      val sourceTeacherOptionsDeferred = async {
        teacherOptionsRemoteDataSource.fetchSourceTeacherOptions()
      }
      val waliSantriSnapshotDeferred = async {
        waliSantriRemoteDataSource.fetchWaliSantriSnapshot(
          teacherRowId = teacherRowId,
          teacherKaryawanId = teacherKaryawanId
        )
      }
      val mutabaahSnapshotDeferred = async {
        mutabaahRemoteDataSource.fetchMutabaahSnapshot(
          teacherRowId = teacherRowId,
          teacherKaryawanId = teacherKaryawanId,
          referenceDate = LocalDate.now()
        )
      }
      val leaveRequestSnapshotDeferred = async {
        leaveRequestRemoteDataSource.fetchLeaveRequestSnapshot(
          teacherRowId = teacherRowId,
          teacherKaryawanId = teacherKaryawanId
        )
      }
      val remoteProfile = remoteProfileDeferred.await()
      val calendarEvents = calendarEventsDeferred.await()
      val mapelPayload = mapelPayloadDeferred.await()
      val teachingScheduleEvents = teachingScheduleEventsDeferred.await()
      val notifications = notificationsDeferred.await()
      val substituteTeacherOptions = substituteTeacherOptionsDeferred.await()
      val sourceTeacherOptions = sourceTeacherOptionsDeferred.await()
      val waliSantriSnapshot = waliSantriSnapshotDeferred.await()
      val monthlyReportSnapshot = if (waliSantriSnapshot != null) {
        monthlyReportRemoteDataSource.fetchMonthlyReportSnapshot(
          teacherRowId = teacherRowId,
          teacherKaryawanId = teacherKaryawanId,
          waliSantriSnapshot = waliSantriSnapshot
        )
      } else {
        null
      }
      val utsReportSnapshot = if (waliSantriSnapshot != null) {
        utsReportRemoteDataSource.fetchUtsReportSnapshot(
          teacherRowId = teacherRowId,
          teacherKaryawanId = teacherKaryawanId,
          waliSantriSnapshot = waliSantriSnapshot
        )
      } else {
        null
      }
      val mutabaahSnapshot = mutabaahSnapshotDeferred.await()
      val leaveRequestSnapshot = leaveRequestSnapshotDeferred.await()
      val didReachServer = remoteProfile != null ||
        mapelPayload != null ||
        calendarEvents != null ||
        teachingScheduleEvents != null ||
        notifications != null ||
        substituteTeacherOptions != null ||
        sourceTeacherOptions != null ||
        waliSantriSnapshot != null ||
        monthlyReportSnapshot != null ||
        utsReportSnapshot != null ||
        mutabaahSnapshot != null ||
        leaveRequestSnapshot != null ||
        mapelSyncOutcome is PendingMapelSyncOutcome.Applied ||
        offlineSyncOutcome.syncedCount > 0
      val refreshedBase = if (didReachServer) {
        SampleDataFactory.refreshDashboard(baseDashboard)
      } else {
        baseDashboard
      }
      var refreshed = refreshedBase
      if (remoteProfile != null) {
        refreshed = refreshed.withResolvedProfile(uiState.session, remoteProfile)
        sessionStore.updateTeacherName(refreshed.teacherName)
      }
      if (mapelPayload != null) {
        refreshed = refreshed.withMapelPayload(mapelPayload)
      }
      if (calendarEvents != null) {
        refreshed = refreshed.withCalendarAgenda(calendarEvents)
      }
      if (teachingScheduleEvents != null) {
        refreshed = refreshed.withTeachingSchedule(teachingScheduleEvents)
      }
      if (notifications != null) {
        refreshed = refreshed.withNotifications(mergeNotificationReadState(currentDashboard.notifications, notifications))
      }
      if (substituteTeacherOptions != null || sourceTeacherOptions != null) {
        refreshed = refreshed.withTeacherOptions(
          substituteTeacherOptions = substituteTeacherOptions ?: refreshed.substituteTeacherOptions,
          sourceTeacherOptions = sourceTeacherOptions ?: refreshed.sourceTeacherOptions
        )
      }
      if (waliSantriSnapshot != null) {
        refreshed = refreshed.withWaliSantriSnapshot(waliSantriSnapshot)
      }
      if (monthlyReportSnapshot != null) {
        refreshed = refreshed.withMonthlyReportSnapshot(monthlyReportSnapshot)
      }
      if (utsReportSnapshot != null) {
        refreshed = refreshed.withUtsReportSnapshot(utsReportSnapshot)
      }
      if (mutabaahSnapshot != null) {
        refreshed = refreshed.withMutabaahSnapshot(mutabaahSnapshot)
      }
      if (leaveRequestSnapshot != null) {
        refreshed = refreshed.withLeaveRequestSnapshot(leaveRequestSnapshot)
      }
      refreshed = refreshed.recalculatePendingSyncCount()
      cacheStore.writeDashboard(refreshed)
      teachingReminderScheduler.syncReminders(
        settings = uiState.teachingReminderSettings,
        events = refreshed.teachingScheduleEvents
      )
      val refreshedReminderSettings = teachingReminderStore.readSettings()
      val nextSession = sessionStore.readSession()
      uiState = uiState.copy(
        dashboard = refreshed,
        session = nextSession,
        teachingReminderSettings = refreshedReminderSettings,
        syncBanner = SyncBannerState(
          buildRefreshSyncMessage(didReachServer, mapelSyncOutcome, offlineSyncOutcome),
          false
        )
      )
    }
  }

  fun markNotificationAsRead(notificationId: String) {
    val dashboard = uiState.dashboard ?: return
    val notifications = dashboard.notifications
    val target = notifications.firstOrNull { it.id == notificationId } ?: return
    if (target.isRead) return
    val updated = notifications.map { notification ->
      if (notification.id == notificationId) notification.copy(isRead = true) else notification
    }
    persistNotifications(updated)
  }

  fun markAllNotificationsAsRead() {
    val dashboard = uiState.dashboard ?: return
    if (dashboard.notifications.none { !it.isRead }) return
    val updated = dashboard.notifications.map { it.copy(isRead = true) }
    persistNotifications(updated)
  }

  private fun bootstrap() {
    viewModelScope.launch {
      uiState = uiState.copy(destination = GuruDestination.Splash, splashMessage = "Memeriksa sesi login...")
      delay(900)

      val session = sessionStore.readSession()
      val reminderSettings = teachingReminderStore.readSettings()
      if (!session.isLoggedIn) {
        uiState = uiState.copy(
          destination = GuruDestination.Login,
          splashMessage = "Silakan masuk.",
          session = session,
          loginTeacherName = "",
          teachingReminderSettings = reminderSettings
        )
        return@launch
      }

      uiState = uiState.copy(splashMessage = "Membuka data lokal dari perangkat...")
      delay(600)
      val localDashboard = cacheStore.readDashboard() ?: SampleDataFactory.createDashboard(session.teacherName)
      val nextDashboard = localDashboard
        .copy(teacherName = session.teacherName)
        .withResolvedProfile(session)
        .recalculatePendingSyncCount()
      cacheStore.writeDashboard(nextDashboard)

      uiState = uiState.copy(
        destination = GuruDestination.Home,
        session = session,
        dashboard = nextDashboard,
        syncBanner = SyncBannerState("Data lokal siap dipakai. Memeriksa pembaruan...", false),
        selectedSidebarDestination = GuruSidebarDestination.Dashboard,
        isCalendarScreenOpen = false,
        selectedCalendarDateIso = LocalDate.now().toString(),
        teachingReminderSettings = reminderSettings,
        isSidebarOpen = false,
        expandedSidebarParent = null,
        isClaimSectionVisible = false,
        selectedClaimSubjectIds = emptySet()
      )

      teachingReminderScheduler.syncReminders(
        settings = reminderSettings,
        events = nextDashboard.teachingScheduleEvents
      )

      refreshFromServer(force = true)
    }
  }

  private fun isRefreshDue(lastRefreshAt: Long): Boolean {
    if (lastRefreshAt <= 0L) return true
    val fifteenMinutes = 15 * 60 * 1000L
    return (System.currentTimeMillis() - lastRefreshAt) >= fifteenMinutes
  }

  private fun AvailableSubjectOffer.toClaimedSubject(): SubjectOverview {
    return SubjectOverview(
      id = id,
      title = title,
      className = className,
      semester = semester,
      semesterActive = semesterActive,
      attendancePending = 0,
      scorePending = 0,
      materialCount = 0
    )
  }

  private fun DashboardPayload.withCalendarAgenda(events: List<CalendarEvent>): DashboardPayload {
    return copy(
      calendarEvents = events,
      ongoingTasks = buildDashboardAgendaTasks(events),
      categories = buildAgendaCategories(events)
    )
  }

  private fun DashboardPayload.withNotifications(notifications: List<AppNotification>): DashboardPayload {
    return copy(
      notifications = notifications,
      unreadNotificationCount = notifications.count { !it.isRead }
    )
  }

  private fun DashboardPayload.withTeacherOptions(
    substituteTeacherOptions: List<TeacherOption>,
    sourceTeacherOptions: List<TeacherOption>
  ): DashboardPayload {
    return copy(
      substituteTeacherOptions = substituteTeacherOptions.distinctBy { it.id },
      sourceTeacherOptions = sourceTeacherOptions.distinctBy { it.id }
    )
  }

  private fun DashboardPayload.withWaliSantriSnapshot(snapshot: WaliSantriSnapshot): DashboardPayload {
    return copy(waliSantriSnapshot = snapshot)
  }

  private fun DashboardPayload.withMutabaahSnapshot(snapshot: MutabaahSnapshot): DashboardPayload {
    return copy(mutabaahSnapshot = snapshot)
  }

  private fun DashboardPayload.withLeaveRequestSnapshot(snapshot: LeaveRequestSnapshot): DashboardPayload {
    return copy(leaveRequestSnapshot = snapshot)
  }

  private fun DashboardPayload.withMonthlyReportSnapshot(snapshot: MonthlyReportSnapshot): DashboardPayload {
    return copy(monthlyReportSnapshot = snapshot)
  }

  private fun DashboardPayload.withUtsReportSnapshot(snapshot: UtsReportSnapshot): DashboardPayload {
    return copy(utsReportSnapshot = snapshot)
  }

  private fun DashboardPayload.withResolvedProfile(
    session: SessionSnapshot,
    remoteProfile: GuruRemoteProfile? = null
  ): DashboardPayload {
    val baseProfile = remoteProfile?.toGuruProfile(fallbackTeacherId = session.teacherId) ?: profile
    val resolvedName = baseProfile.name.ifBlank { teacherName.ifBlank { session.teacherName } }
    val resolvedUsername = baseProfile.username.ifBlank { session.teacherId }
    return copy(
      teacherName = resolvedName,
      profile = baseProfile.copy(
        name = resolvedName,
        username = resolvedUsername
      )
    )
  }

  private fun DashboardPayload.withTeachingSchedule(events: List<CalendarEvent>): DashboardPayload {
    return copy(
      teachingScheduleEvents = events
    )
  }

  private fun DashboardPayload.withMapelPayload(payload: GuruMapelPayload): DashboardPayload {
    val pendingOffers = pendingClaimOffers.distinctBy { it.id }
    val pendingIds = pendingOffers.map { it.id }.toSet()
    val mergedSubjects = (payload.subjects + pendingOffers.map { it.toClaimedSubject() })
      .distinctBy { it.id }
      .sortedWith(
        compareByDescending<SubjectOverview> { it.semesterActive }
          .thenBy { it.title.lowercase() }
          .thenBy { it.className.lowercase() }
      )
    return copy(
      subjects = mergedSubjects,
      availableSubjects = payload.availableSubjects.filterNot { pendingIds.contains(it.id) },
      quickStats = quickStats.map { stat ->
        if (stat.id == "stat-1") stat.copy(value = mergedSubjects.size.toString()) else stat
      }
    )
  }

  private fun DashboardPayload.withAttendanceSnapshot(snapshot: MapelAttendanceSnapshot): DashboardPayload {
    return copy(
      attendanceSnapshots = attendanceSnapshots
        .filterNot { it.distribusiId == snapshot.distribusiId } + snapshot
    )
  }

  private fun DashboardPayload.withScoreSnapshot(snapshot: MapelScoreSnapshot): DashboardPayload {
    return copy(
      scoreSnapshots = scoreSnapshots
        .filterNot { it.distribusiId == snapshot.distribusiId } + snapshot
    )
  }

  private suspend fun queuePendingMapelAttendance(
    distribusiId: String,
    subject: SubjectOverview,
    changesByStudent: Map<String, List<AttendanceHistoryEntry>>,
    sourceTeacherId: String,
    substituteNote: String,
    fallbackMessage: String
  ): AttendanceSaveOutcome {
    val dashboard = uiState.dashboard
      ?: return AttendanceSaveOutcome(false, fallbackMessage.ifBlank { "Data lokal belum siap." })
    val normalizedChanges = changesByStudent
      .mapKeys { it.key.trim() }
      .filterKeys { it.isNotBlank() }
      .mapValues { (_, changes) ->
        changes.mapNotNull { change ->
          val dateIso = change.dateIso.trim().take(10)
          val status = normalizeAttendanceStatusLabel(change.status)
          if (dateIso.isBlank() || status.isBlank()) {
            null
          } else {
            change.copy(
              dateIso = dateIso,
              status = status,
              lessonSlotId = change.lessonSlotId.trim(),
              patronMateri = change.patronMateri.trim()
            )
          }
        }
      }
      .filterValues { it.isNotEmpty() }
    if (normalizedChanges.isEmpty()) {
      return AttendanceSaveOutcome(false, "Tidak ada perubahan absensi.")
    }

    val batch = PendingAttendanceBatch(
      id = "attendance-${System.currentTimeMillis()}-${distribusiId.hashCode()}",
      distribusiId = distribusiId,
      subjectTitle = subject.title,
      className = subject.className,
      changesByStudent = normalizedChanges,
      sourceTeacherId = sourceTeacherId.trim(),
      substituteNote = substituteNote.trim(),
      createdAt = System.currentTimeMillis()
    )
    val nextDashboard = dashboard
      .withLocalAttendanceChanges(distribusiId, subject, normalizedChanges)
      .copy(pendingAttendanceBatches = dashboard.pendingAttendanceBatches + batch)
      .recalculatePendingSyncCount()
    cacheStore.writeDashboard(nextDashboard)
    val message = if (sourceTeacherId.isBlank()) {
      "Absensi disimpan lokal. Akan disinkronkan otomatis saat online."
    } else {
      "Pengajuan absensi pengganti disimpan lokal. Akan dikirim saat online."
    }
    uiState = uiState.copy(
      dashboard = nextDashboard,
      syncBanner = SyncBannerState(message, false)
    )
    return AttendanceSaveOutcome(true, message)
  }

  private suspend fun queuePendingMapelScore(
    distribusiId: String,
    subject: SubjectOverview,
    students: List<ScoreStudent>,
    metricKey: String,
    fallbackMessage: String
  ): ScoreSaveOutcome {
    val dashboard = uiState.dashboard
      ?: return ScoreSaveOutcome(false, fallbackMessage.ifBlank { "Data lokal belum siap." })
    val normalizedStudents = students.filter { it.id.isNotBlank() }
    if (normalizedStudents.isEmpty()) {
      return ScoreSaveOutcome(false, "Tidak ada nilai yang perlu disimpan.")
    }

    val batch = PendingScoreBatch(
      id = "score-${System.currentTimeMillis()}-${distribusiId.hashCode()}",
      distribusiId = distribusiId,
      subjectTitle = subject.title,
      className = subject.className,
      metricKey = metricKey.trim(),
      students = normalizedStudents,
      createdAt = System.currentTimeMillis()
    )
    val nextDashboard = dashboard
      .withLocalScoreStudents(distribusiId, subject, normalizedStudents)
      .copy(pendingScoreBatches = dashboard.pendingScoreBatches + batch)
      .recalculatePendingSyncCount()
    cacheStore.writeDashboard(nextDashboard)
    val message = "Nilai disimpan lokal. Akan disinkronkan otomatis saat online."
    uiState = uiState.copy(
      dashboard = nextDashboard,
      syncBanner = SyncBannerState(message, false)
    )
    return ScoreSaveOutcome(true, message)
  }

  private fun DashboardPayload.withLocalAttendanceChanges(
    distribusiId: String,
    subject: SubjectOverview,
    changesByStudent: Map<String, List<AttendanceHistoryEntry>>
  ): DashboardPayload {
    val existingSnapshot = attendanceSnapshots.firstOrNull { it.distribusiId == distribusiId }
    val existingStudents = existingSnapshot?.students.orEmpty()
    val changedIds = changesByStudent.keys
    val updatedById = existingStudents.associate { student ->
      val changes = changesByStudent[student.id].orEmpty()
      student.id to if (changes.isEmpty()) student else student.withLocalAttendanceHistory(changes)
    }.toMutableMap()

    changedIds
      .filterNot { updatedById.containsKey(it) }
      .forEach { studentId ->
        updatedById[studentId] = AttendanceStudent(
          id = studentId,
          name = "Santri",
          hadirPercent = 0,
          terlambatPercent = 0,
          sakitPercent = 0,
          izinPercent = 0,
          alpaPercent = 0
        ).withLocalAttendanceHistory(changesByStudent[studentId].orEmpty())
      }

    val orderedStudents = buildList {
      existingStudents.forEach { student ->
        updatedById[student.id]?.let(::add)
      }
      updatedById.values
        .filterNot { student -> existingStudents.any { it.id == student.id } }
        .sortedBy { it.name.lowercase() }
        .forEach(::add)
    }
    val snapshot = MapelAttendanceSnapshot(
      distribusiId = distribusiId,
      subjectTitle = existingSnapshot?.subjectTitle.orEmpty().ifBlank { subject.title },
      className = existingSnapshot?.className.orEmpty().ifBlank { subject.className },
      rangeLabel = buildLocalAttendanceRangeLabel(
        students = orderedStudents,
        fallback = existingSnapshot?.rangeLabel.orEmpty()
      ),
      students = orderedStudents,
      updatedAt = System.currentTimeMillis(),
      supportsPatronMateri = true
    )
    return withAttendanceSnapshot(snapshot)
  }

  private fun DashboardPayload.withLocalScoreStudents(
    distribusiId: String,
    subject: SubjectOverview,
    students: List<ScoreStudent>
  ): DashboardPayload {
    val existingSnapshot = scoreSnapshots.firstOrNull { it.distribusiId == distribusiId }
    val existingStudents = existingSnapshot?.students.orEmpty()
    val changedById = students.associateBy { it.id }
    val cleanChangedById = changedById.mapValues { (_, student) ->
      student.copy(
        pendingDetailMetricKey = "",
        pendingDeletedDetailIds = emptyList()
      )
    }
    val orderedStudents = buildList {
      existingStudents.forEach { student ->
        add(cleanChangedById[student.id] ?: student)
      }
      cleanChangedById.values
        .filterNot { student -> existingStudents.any { it.id == student.id } }
        .sortedBy { it.name.lowercase() }
        .forEach(::add)
    }
    val snapshot = MapelScoreSnapshot(
      distribusiId = distribusiId,
      subjectTitle = existingSnapshot?.subjectTitle.orEmpty().ifBlank { subject.title },
      className = existingSnapshot?.className.orEmpty().ifBlank { subject.className },
      students = orderedStudents,
      updatedAt = System.currentTimeMillis(),
      supportsMaterialHistory = true
    )
    return withScoreSnapshot(snapshot)
  }

  private fun AttendanceStudent.withLocalAttendanceHistory(
    changes: List<AttendanceHistoryEntry>
  ): AttendanceStudent {
    val normalizedChanges = changes.mapNotNull { change ->
      val dateIso = change.dateIso.trim().take(10)
      val status = normalizeAttendanceStatusLabel(change.status)
      if (dateIso.isBlank() || status.isBlank()) {
        null
      } else {
        change.copy(
          dateIso = dateIso,
          status = status,
          lessonSlotId = change.lessonSlotId.trim(),
          patronMateri = change.patronMateri.trim()
        )
      }
    }
    if (normalizedChanges.isEmpty()) return this
    val changedKeys = normalizedChanges.map { attendanceHistoryKey(it) }.toSet()
    val nextHistory = (history.filterNot { attendanceHistoryKey(it) in changedKeys } + normalizedChanges)
      .sortedWith(
        compareByDescending<AttendanceHistoryEntry> { it.dateIso }
          .thenBy { it.lessonSlotId }
      )
    return withAttendancePercentages(nextHistory)
  }

  private fun AttendanceStudent.withAttendancePercentages(
    nextHistory: List<AttendanceHistoryEntry>
  ): AttendanceStudent {
    val total = nextHistory.size
    fun percent(status: String): Int {
      if (total <= 0) return 0
      val count = nextHistory.count { it.status.equals(status, ignoreCase = true) }
      return ((count.toDouble() / total.toDouble()) * 100.0).roundToInt()
    }
    return copy(
      hadirPercent = percent("Hadir"),
      terlambatPercent = percent("Terlambat"),
      sakitPercent = percent("Sakit"),
      izinPercent = percent("Izin"),
      alpaPercent = percent("Alpa"),
      history = nextHistory
    )
  }

  private fun attendanceHistoryKey(entry: AttendanceHistoryEntry): String {
    return "${entry.dateIso.trim().take(10)}|${entry.lessonSlotId.trim()}"
  }

  private fun normalizeAttendanceStatusLabel(status: String): String {
    return when (status.trim().lowercase()) {
      "hadir", "masuk" -> "Hadir"
      "terlambat", "telat" -> "Terlambat"
      "sakit" -> "Sakit"
      "izin", "ijin" -> "Izin"
      "alpa", "alpha", "absen" -> "Alpa"
      else -> status.trim()
    }
  }

  private fun buildLocalAttendanceRangeLabel(
    students: List<AttendanceStudent>,
    fallback: String
  ): String {
    val dates = students
      .flatMap { it.history }
      .mapNotNull { entry ->
        runCatching { LocalDate.parse(entry.dateIso.take(10)) }.getOrNull()
      }
      .distinct()
      .sorted()
    if (dates.isEmpty()) return fallback.ifBlank { "Data lokal" }
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
    val start = dates.first().format(formatter)
    val end = dates.last().format(formatter)
    return if (start == end) start else "$start - $end"
  }

  private fun DashboardPayload.recalculatePendingSyncCount(): DashboardPayload {
    val nextCount = pendingClaimOffers.size + pendingAttendanceBatches.size + pendingScoreBatches.size
    return copy(
      pendingSyncCount = nextCount,
      quickStats = quickStats.map { stat ->
        if (stat.id == "stat-2") stat.copy(value = nextCount.toString()) else stat
      }
    )
  }

  private fun isScoreSnapshotMissingDetailRows(snapshot: MapelScoreSnapshot): Boolean {
    if (!snapshot.supportsMaterialHistory) return true
    return snapshot.students.any { student ->
      listOf(
        "nilai_tugas" to student.nilaiTugas,
        "nilai_ulangan_harian" to student.nilaiUlanganHarian,
        "nilai_pts" to student.nilaiPts,
        "nilai_pas" to student.nilaiPas,
        "nilai_keterampilan" to student.nilaiKeterampilan
      ).any { (metricKey, value) ->
        value != null && student.detailRowsByMetric[metricKey].isNullOrEmpty()
      }
    }
  }

  private fun DashboardPayload.withPatronMateriSnapshot(snapshot: MapelPatronMateriSnapshot): DashboardPayload {
    return copy(
      patronMateriSnapshots = patronMateriSnapshots
        .filterNot { it.distribusiId == snapshot.distribusiId } + snapshot,
      subjects = subjects.map { subject ->
        if (subject.id == snapshot.distribusiId) subject.copy(materialCount = snapshot.items.size) else subject
      }
    )
  }

  private fun isAttendanceSnapshotComplete(snapshot: MapelAttendanceSnapshot): Boolean {
    if (!snapshot.supportsPatronMateri) return false
    return snapshot.students.none { student ->
      student.history.isEmpty() && listOf(
        student.hadirPercent,
        student.terlambatPercent,
        student.sakitPercent,
        student.izinPercent,
        student.alpaPercent
      ).any { it > 0 }
    }
  }

  private fun DashboardPayload.withQueuedMapelClaims(selectedOffers: List<AvailableSubjectOffer>): DashboardPayload {
    val normalizedOffers = selectedOffers.distinctBy { it.id }
    val nextSubjects = (subjects + normalizedOffers.map { it.toClaimedSubject() })
      .distinctBy { it.id }
      .sortedWith(
        compareByDescending<SubjectOverview> { it.semesterActive }
          .thenBy { it.title.lowercase() }
          .thenBy { it.className.lowercase() }
      )
    val nextPendingOffers = (pendingClaimOffers + normalizedOffers).distinctBy { it.id }
    return copy(
      subjects = nextSubjects,
      availableSubjects = availableSubjects.filterNot { offer -> normalizedOffers.any { it.id == offer.id } },
      pendingClaimOffers = nextPendingOffers,
      quickStats = quickStats.map { stat ->
        if (stat.id == "stat-1") stat.copy(value = nextSubjects.size.toString()) else stat
      }
    ).recalculatePendingSyncCount()
  }

  private fun buildDashboardAgendaTasks(events: List<CalendarEvent>): List<com.mim.guruapp.data.model.DashboardTask> {
    val today = LocalDate.now()
    return events
      .sortedWith(
        compareBy<CalendarEvent> { eventPriority(it, today) }
          .thenBy { parseEventStart(it) }
          .thenBy { it.title }
      )
      .take(4)
      .map { event ->
        com.mim.guruapp.data.model.DashboardTask(
          id = "agenda-${event.id}",
          title = event.title,
          timeLabel = formatEventRangeLabel(event),
          colorHex = event.colorHex,
          categoryKey = event.categoryKey,
          description = event.description
        )
      }
  }

  private fun eventPriority(event: CalendarEvent, today: LocalDate): Int {
    val start = parseEventStart(event)
    val end = parseEventEnd(event)
    return when {
      !today.isBefore(start) && !today.isAfter(end) -> 0
      start.isAfter(today) -> 1
      else -> 2
    }
  }

  private fun formatEventRangeLabel(event: CalendarEvent): String {
    val start = parseEventStart(event)
    val end = parseEventEnd(event)
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
    val range = if (start == end) {
      formatter.format(start)
    } else {
      "${formatter.format(start)} - ${formatter.format(end)}"
    }
    return if (event.timeLabel.isBlank()) range else "$range | ${event.timeLabel}"
  }

  private fun buildAgendaCategories(events: List<CalendarEvent>): List<com.mim.guruapp.data.model.DashboardCategory> {
    val counts = events.groupingBy { it.categoryKey }.eachCount()
    val orderedKeys = listOf(
      CATEGORY_ALL,
      CATEGORY_GENERAL,
      CATEGORY_LIBUR_AKADEMIK,
      CATEGORY_LIBUR_KETAHFIZAN,
      CATEGORY_LIBUR_SEMUA
    )
    return orderedKeys
      .map { key ->
        com.mim.guruapp.data.model.DashboardCategory(
          id = key,
          title = categoryLabel(key),
          subtitle = if (key == CATEGORY_ALL) "${events.size} Agenda" else "${counts[key] ?: 0} Agenda",
          colorHex = categoryColor(key)
        )
      }
  }

  private fun categoryLabel(key: String): String {
    return when (key) {
      CATEGORY_ALL -> "Semua Kategori"
      CATEGORY_GENERAL -> "Kegiatan Umum"
      CATEGORY_LIBUR_AKADEMIK -> "Libur Akademik"
      CATEGORY_LIBUR_KETAHFIZAN -> "Libur Ketahfizan"
      CATEGORY_LIBUR_SEMUA -> "Libur Semua"
      else -> "Agenda"
    }
  }

  private fun categoryColor(key: String): String {
    return when (key) {
      CATEGORY_GENERAL -> "#DDEBFF"
      CATEGORY_LIBUR_AKADEMIK -> "#FFD8E2"
      CATEGORY_LIBUR_KETAHFIZAN -> "#D7F0FF"
      CATEGORY_LIBUR_SEMUA -> "#FFE7C7"
      else -> "#D9F4E7"
    }
  }

  private fun parseEventStart(event: CalendarEvent): LocalDate {
    return runCatching { LocalDate.parse(event.startDateIso) }.getOrDefault(LocalDate.now())
  }

  private fun parseEventEnd(event: CalendarEvent): LocalDate {
    return runCatching { LocalDate.parse(event.endDateIso.ifBlank { event.startDateIso }) }
      .getOrDefault(parseEventStart(event))
  }

  private fun persistNotifications(notifications: List<AppNotification>) {
    val dashboard = uiState.dashboard ?: return
    val updatedDashboard = dashboard.copy(
      notifications = notifications,
      unreadNotificationCount = notifications.count { !it.isRead }
    )
    uiState = uiState.copy(dashboard = updatedDashboard)
    viewModelScope.launch {
      cacheStore.writeDashboard(updatedDashboard)
    }
  }

  private suspend fun syncPendingMapelClaims(showFeedback: Boolean): PendingMapelSyncOutcome {
    val dashboard = uiState.dashboard ?: return PendingMapelSyncOutcome.NotNeeded
    val pendingOffers = dashboard.pendingClaimOffers.distinctBy { it.id }
    if (pendingOffers.isEmpty()) return PendingMapelSyncOutcome.NotNeeded

    return when (
      val result = mapelRemoteDataSource.claimSubjects(
        teacherRowId = uiState.session.teacherRowId,
        teacherKaryawanId = uiState.session.teacherId,
        distribusiIds = pendingOffers.map { it.id }
      )
    ) {
      is GuruMapelClaimResult.Error -> {
        if (showFeedback) {
          uiState = uiState.copy(syncBanner = SyncBannerState(result.message, false))
        }
        PendingMapelSyncOutcome.Deferred(result.message)
      }

      is GuruMapelClaimResult.Success -> {
        val attemptedIds = pendingOffers.map { it.id }.toSet()
        var nextDashboard = (uiState.dashboard ?: dashboard).copy(
          pendingClaimOffers = (uiState.dashboard ?: dashboard).pendingClaimOffers.filterNot { attemptedIds.contains(it.id) }
        ).recalculatePendingSyncCount()
        val payload = mapelRemoteDataSource.fetchMapelPayload(
          teacherRowId = uiState.session.teacherRowId,
          teacherKaryawanId = uiState.session.teacherId
        )
        if (payload != null) {
          nextDashboard = nextDashboard.withMapelPayload(payload)
        }
        cacheStore.writeDashboard(nextDashboard)
        uiState = uiState.copy(dashboard = nextDashboard)
        val skippedCount = (pendingOffers.size - result.updatedIds.size).coerceAtLeast(0)
        if (showFeedback) {
          uiState = uiState.copy(
            syncBanner = SyncBannerState(
              when {
                result.updatedIds.isEmpty() ->
                  "Tidak ada mapel yang ditambahkan. Mapel kemungkinan sudah diambil guru lain."
                skippedCount > 0 ->
                  "Berhasil menambahkan ${result.updatedIds.size} mapel. $skippedCount mapel gagal karena sudah diambil guru lain."
                else ->
                  "Mapel berhasil ditambahkan dan sinkron dengan server."
              },
              false
            )
          )
        }
        PendingMapelSyncOutcome.Applied(
          updatedCount = result.updatedIds.size,
          skippedCount = skippedCount
        )
      }
    }
  }

  private suspend fun syncPendingOfflineChanges(showFeedback: Boolean): PendingOfflineSyncOutcome {
    val dashboard = uiState.dashboard ?: return PendingOfflineSyncOutcome()
    val pendingAttendance = dashboard.pendingAttendanceBatches.distinctBy { it.id.ifBlank { it.hashCode().toString() } }
    val pendingScores = dashboard.pendingScoreBatches.distinctBy { it.id.ifBlank { it.hashCode().toString() } }
    val attemptedCount = pendingAttendance.size + pendingScores.size
    if (attemptedCount == 0) return PendingOfflineSyncOutcome()

    var nextDashboard = dashboard
    val remainingAttendance = mutableListOf<PendingAttendanceBatch>()
    val remainingScores = mutableListOf<PendingScoreBatch>()
    var syncedCount = 0

    pendingAttendance.forEach { batch ->
      val result = runCatching {
        mapelAttendanceRemoteDataSource.saveAttendanceStatuses(
          distribusiId = batch.distribusiId,
          changesByStudent = batch.changesByStudent,
          teacherRowId = uiState.session.teacherRowId,
          teacherKaryawanId = uiState.session.teacherId,
          sourceTeacherId = batch.sourceTeacherId,
          substituteNote = batch.substituteNote
        )
      }.getOrElse {
        GuruMapelAttendanceSaveResult.Error("Koneksi belum tersedia.")
      }
      when (result) {
        is GuruMapelAttendanceSaveResult.Error -> {
          remainingAttendance += batch
        }

        is GuruMapelAttendanceSaveResult.SubmittedForReview -> {
          syncedCount += 1
        }

        GuruMapelAttendanceSaveResult.Success -> {
          syncedCount += 1
          val refreshedSnapshot = mapelAttendanceRemoteDataSource.fetchAttendanceSnapshot(
            distribusiId = batch.distribusiId,
            fallbackTitle = batch.subjectTitle,
            fallbackClassName = batch.className
          )
          if (refreshedSnapshot != null) {
            nextDashboard = nextDashboard.withAttendanceSnapshot(refreshedSnapshot)
          }
        }
      }
    }

    pendingScores.forEach { batch ->
      when (runCatching { syncPendingScoreBatch(batch) }.getOrElse { GuruMapelScoreSaveResult.Error("Koneksi belum tersedia.") }) {
        is GuruMapelScoreSaveResult.Error -> {
          remainingScores += batch
        }

        GuruMapelScoreSaveResult.Success -> {
          syncedCount += 1
          val refreshedSnapshot = mapelScoreRemoteDataSource.fetchScoreSnapshot(
            distribusiId = batch.distribusiId,
            fallbackTitle = batch.subjectTitle,
            fallbackClassName = batch.className
          )
          if (refreshedSnapshot != null) {
            nextDashboard = nextDashboard.withScoreSnapshot(refreshedSnapshot)
          }
        }
      }
    }

    val remainingCount = remainingAttendance.size + remainingScores.size
    if (syncedCount > 0 || remainingCount != attemptedCount) {
      nextDashboard = nextDashboard.copy(
        pendingAttendanceBatches = remainingAttendance,
        pendingScoreBatches = remainingScores
      ).recalculatePendingSyncCount()
      cacheStore.writeDashboard(nextDashboard)
      uiState = uiState.copy(dashboard = nextDashboard)
    }
    if (showFeedback && syncedCount > 0) {
      uiState = uiState.copy(
        syncBanner = SyncBannerState(
          if (remainingCount > 0) {
            "$syncedCount input lokal berhasil dikirim. $remainingCount input masih menunggu koneksi."
          } else {
            "$syncedCount input lokal berhasil dikirim ke server."
          },
          false
        )
      )
    }
    return PendingOfflineSyncOutcome(
      attemptedCount = attemptedCount,
      syncedCount = syncedCount,
      remainingCount = remainingCount
    )
  }

  private suspend fun syncPendingScoreBatch(batch: PendingScoreBatch): GuruMapelScoreSaveResult {
    val metricKey = batch.metricKey.trim()
    if (metricKey.isBlank()) {
      batch.students.forEach { student ->
        val result = runCatching {
          mapelScoreRemoteDataSource.saveScoreDraft(
            distribusiId = batch.distribusiId,
            santriId = student.id,
            draft = ScoreDraftPayload(
              rowId = student.rowId,
              nilaiTugas = student.nilaiTugas,
              nilaiUlanganHarian = student.nilaiUlanganHarian,
              nilaiPts = student.nilaiPts,
              nilaiPas = student.nilaiPas,
              nilaiKehadiran = student.nilaiKehadiran,
              nilaiKeterampilan = student.nilaiKeterampilan
            )
          )
        }.getOrElse {
          GuruMapelScoreSaveResult.Error("Koneksi belum tersedia.")
        }
        if (result is GuruMapelScoreSaveResult.Error) return result
      }
      return GuruMapelScoreSaveResult.Success
    }

    return runCatching {
      mapelScoreRemoteDataSource.saveScoreDetailRowsBatch(
        distribusiId = batch.distribusiId,
        guruId = uiState.session.teacherRowId.ifBlank { uiState.session.teacherId },
        metricKey = metricKey,
        rowsBySantriId = batch.students.associate { student ->
          student.id to student.detailRowsByMetric[metricKey].orEmpty()
        },
        deletedIdsBySantriId = batch.students.associate { student ->
          student.id to student.pendingDeletedDetailIds
        }
      )
    }.getOrElse {
      GuruMapelScoreSaveResult.Error("Koneksi belum tersedia.")
    }
  }

  private fun refreshNotificationsOnly() {
    val dashboard = uiState.dashboard ?: return
    viewModelScope.launch {
      val calendarEvents = kalenderAkademikRemoteDataSource.fetchCalendarEvents(
        teacherRowId = uiState.session.teacherRowId,
        teacherKaryawanId = uiState.session.teacherId
      )
      val notifications = guruNotificationRemoteDataSource.fetchNotifications(
        teacherRowId = uiState.session.teacherRowId,
        teacherKaryawanId = uiState.session.teacherId
      )
      if (notifications == null && calendarEvents == null) return@launch
      if ((notifications?.isEmpty() != false) && (calendarEvents?.isEmpty() != false) && !isLikelySampleNotifications(dashboard.notifications)) return@launch
      var updatedDashboard = dashboard
      if (calendarEvents != null) {
        updatedDashboard = updatedDashboard.withCalendarAgenda(calendarEvents)
      }
      if (notifications != null) {
        val merged = mergeNotificationReadState(dashboard.notifications, notifications)
        updatedDashboard = updatedDashboard.copy(
          notifications = merged,
          unreadNotificationCount = merged.count { !it.isRead }
        )
      }
      cacheStore.writeDashboard(updatedDashboard)
      uiState = uiState.copy(dashboard = updatedDashboard)
    }
  }

  private fun mergeNotificationReadState(
    existing: List<AppNotification>,
    incoming: List<AppNotification>
  ): List<AppNotification> {
    val readMap = existing.associate { it.id to it.isRead }
    return incoming.map { notification ->
      notification.copy(isRead = readMap[notification.id] ?: notification.isRead)
    }
  }

  private fun isLikelySampleNotifications(notifications: List<AppNotification>): Boolean {
    return notifications.isNotEmpty() && notifications.all { it.id.startsWith("notif-") }
  }

  private fun GuruRemoteProfile.toGuruProfile(fallbackTeacherId: String): GuruProfile {
    return GuruProfile(
      name = name,
      address = address,
      username = teacherId.ifBlank { fallbackTeacherId },
      password = password,
      phoneNumber = phoneNumber,
      avatarUri = avatarUrl
    )
  }

  private companion object {
    const val CATEGORY_ALL = "semua"
    const val CATEGORY_GENERAL = "kegiatan_umum"
    const val CATEGORY_LIBUR_AKADEMIK = "libur_akademik"
    const val CATEGORY_LIBUR_KETAHFIZAN = "libur_ketahfizan"
    const val CATEGORY_LIBUR_SEMUA = "libur_semua_kegiatan"
  }
}

private fun buildRefreshSyncMessage(
  didReachServer: Boolean,
  mapelSyncOutcome: PendingMapelSyncOutcome,
  offlineSyncOutcome: PendingOfflineSyncOutcome
): String {
  val baseMessage = when (mapelSyncOutcome) {
    is PendingMapelSyncOutcome.Applied -> when {
      mapelSyncOutcome.updatedCount > 0 && mapelSyncOutcome.skippedCount > 0 ->
        "Sinkronisasi selesai. ${mapelSyncOutcome.updatedCount} mapel baru masuk, ${mapelSyncOutcome.skippedCount} mapel gagal karena sudah diambil guru lain."
      mapelSyncOutcome.updatedCount > 0 ->
        "Sinkronisasi selesai. Mapel baru sudah dikirim ke server dan cache lokal diperbarui."
      else ->
        "Sinkronisasi selesai. Beberapa mapel pending tidak bisa ditambahkan karena sudah diambil guru lain."
    }

    is PendingMapelSyncOutcome.Deferred -> if (didReachServer) {
      "Sinkronisasi selesai. Sebagian mapel baru masih menunggu koneksi untuk dikirim ke server."
    } else {
      "Server belum terjangkau. Menampilkan data lokal terakhir, termasuk mapel yang masih menunggu sinkronisasi."
    }

    PendingMapelSyncOutcome.NotNeeded -> if (didReachServer) {
      "Sinkronisasi selesai. Tampilan memakai cache lokal terbaru."
    } else {
      "Server belum terjangkau. Menampilkan data lokal terakhir."
    }
  }
  return when {
    offlineSyncOutcome.syncedCount > 0 && offlineSyncOutcome.remainingCount > 0 ->
      "$baseMessage ${offlineSyncOutcome.syncedCount} input lokal terkirim, ${offlineSyncOutcome.remainingCount} masih antre."
    offlineSyncOutcome.syncedCount > 0 ->
      "$baseMessage ${offlineSyncOutcome.syncedCount} input lokal terkirim."
    offlineSyncOutcome.remainingCount > 0 && !didReachServer ->
      "Server belum terjangkau. Menampilkan data lokal terakhir. ${offlineSyncOutcome.remainingCount} input masih antre sinkronisasi."
    offlineSyncOutcome.remainingCount > 0 ->
      "$baseMessage ${offlineSyncOutcome.remainingCount} input masih antre sinkronisasi."
    else -> baseMessage
  }
}

private sealed interface PendingMapelSyncOutcome {
  data object NotNeeded : PendingMapelSyncOutcome
  data class Deferred(val message: String) : PendingMapelSyncOutcome
  data class Applied(val updatedCount: Int, val skippedCount: Int) : PendingMapelSyncOutcome
}

private data class PendingOfflineSyncOutcome(
  val attemptedCount: Int = 0,
  val syncedCount: Int = 0,
  val remainingCount: Int = 0
)
