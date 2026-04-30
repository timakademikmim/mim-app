package com.mim.guruapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DashboardPayload(
  val teacherName: String,
  val teacherRole: String,
  val greeting: String,
  val profile: GuruProfile = GuruProfile(),
  val subjects: List<SubjectOverview>,
  val availableSubjects: List<AvailableSubjectOffer>,
  val pendingClaimOffers: List<AvailableSubjectOffer> = emptyList(),
  val pendingAttendanceBatches: List<PendingAttendanceBatch> = emptyList(),
  val pendingScoreBatches: List<PendingScoreBatch> = emptyList(),
  val attendanceSnapshots: List<MapelAttendanceSnapshot> = emptyList(),
  val scoreSnapshots: List<MapelScoreSnapshot> = emptyList(),
  val patronMateriSnapshots: List<MapelPatronMateriSnapshot> = emptyList(),
  val notices: List<NoticeItem>,
  val quickStats: List<QuickStat>,
  val categories: List<DashboardCategory>,
  val ongoingTasks: List<DashboardTask>,
  val calendarEvents: List<CalendarEvent>,
  val teachingScheduleEvents: List<CalendarEvent> = emptyList(),
  val notifications: List<AppNotification> = emptyList(),
  val unreadNotificationCount: Int = 0,
  val substituteTeacherOptions: List<TeacherOption> = emptyList(),
  val sourceTeacherOptions: List<TeacherOption> = emptyList(),
  val waliSantriSnapshot: WaliSantriSnapshot = WaliSantriSnapshot(),
  val mutabaahSnapshot: MutabaahSnapshot = MutabaahSnapshot(),
  val leaveRequestSnapshot: LeaveRequestSnapshot = LeaveRequestSnapshot(),
  val monthlyReportSnapshot: MonthlyReportSnapshot = MonthlyReportSnapshot(),
  val utsReportSnapshot: UtsReportSnapshot = UtsReportSnapshot(),
  val pendingSyncCount: Int = 0,
  val lastSuccessfulSyncAt: Long = 0L,
  val lastServerRefreshAt: Long = 0L
)

@Serializable
data class GuruProfile(
  val name: String = "",
  val address: String = "",
  val username: String = "",
  val password: String = "",
  val phoneNumber: String = "",
  val avatarUri: String = ""
)

@Serializable
data class TeacherOption(
  val id: String,
  val name: String,
  val employeeId: String = ""
)

@Serializable
data class MutabaahSnapshot(
  val guruId: String = "",
  val period: String = "",
  val tasks: List<MutabaahTask> = emptyList(),
  val submissions: List<MutabaahSubmission> = emptyList(),
  val academicHolidayDates: List<String> = emptyList(),
  val updatedAt: Long = 0L
)

@Serializable
data class MutabaahTask(
  val id: String,
  val dateIso: String,
  val title: String,
  val description: String = "",
  val active: Boolean = true
)

@Serializable
data class MutabaahSubmission(
  val id: String = "",
  val templateId: String,
  val dateIso: String,
  val status: String = "belum",
  val note: String = "",
  val submittedAt: String = "",
  val updatedAt: String = "",
  val syncState: String = "synced"
)

@Serializable
data class LeaveRequestSnapshot(
  val guruId: String = "",
  val requests: List<LeaveRequestItem> = emptyList(),
  val updatedAt: Long = 0L
)

@Serializable
data class LeaveRequestItem(
  val id: String,
  val startDateIso: String,
  val endDateIso: String,
  val durationDays: Int,
  val purpose: String,
  val status: String = "menunggu",
  val reviewerNote: String = "",
  val reviewedAt: String = "",
  val createdAt: String = "",
  val updatedAt: String = ""
)

@Serializable
data class WaliSantriSnapshot(
  val isWaliKelas: Boolean = false,
  val classes: List<WaliClassInfo> = emptyList(),
  val students: List<WaliSantriProfile> = emptyList(),
  val updatedAt: Long = 0L
)

@Serializable
data class WaliClassInfo(
  val id: String,
  val name: String
)

@Serializable
data class WaliSantriProfile(
  val id: String,
  val name: String,
  val classId: String = "",
  val className: String = "",
  val nisn: String = "",
  val gender: String = "",
  val fatherName: String = "",
  val fatherPhone: String = "",
  val motherName: String = "",
  val motherPhone: String = "",
  val guardianName: String = "",
  val guardianPhone: String = "",
  val studentPhone: String = "",
  val address: String = "",
  val note: String = ""
)

@Serializable
data class MonthlyReportSnapshot(
  val guruId: String = "",
  val availablePeriods: List<String> = emptyList(),
  val reports: List<MonthlyReportItem> = emptyList(),
  val extracurricularReports: List<MonthlyExtracurricularReport> = emptyList(),
  val attendanceSummaries: List<MonthlyAttendanceSummary> = emptyList(),
  val updatedAt: Long = 0L,
  val missingTable: Boolean = false,
  val missingExtendedColumns: Boolean = false
)

@Serializable
data class MonthlyExtracurricularReport(
  val id: String = "",
  val period: String = "",
  val studentId: String = "",
  val activityId: String = "",
  val activityName: String = "",
  val pjName: String = "",
  val pjPhone: String = "",
  val attendanceLabel: String = "",
  val note: String = "",
  val hasMonthlyOverride: Boolean = false
)

@Serializable
data class MonthlyAttendanceSummary(
  val period: String = "",
  val studentId: String = "",
  val attendancePercent: String = "",
  val attendancePredicate: String = "",
  val sakitCount: Int = 0,
  val izinCount: Int = 0,
  val alpaCount: Int = 0,
  val totalDays: Int = 0
)

@Serializable
data class MonthlyReportItem(
  val id: String = "",
  val period: String = "",
  val guruId: String = "",
  val classId: String = "",
  val studentId: String = "",
  val nilaiAkhlak: String = "",
  val predikat: String = "",
  val catatanWali: String = "",
  val muhaffiz: String = "",
  val noHpMuhaffiz: String = "",
  val nilaiKehadiranHalaqah: String = "",
  val sakitHalaqah: String = "",
  val izinHalaqah: String = "",
  val nilaiAkhlakHalaqah: String = "",
  val keteranganAkhlakHalaqah: String = "",
  val nilaiUjianBulanan: String = "",
  val keteranganUjianBulanan: String = "",
  val nilaiTargetHafalan: String = "",
  val keteranganTargetHafalan: String = "",
  val nilaiCapaianHafalanBulanan: String = "",
  val keteranganCapaianHafalanBulanan: String = "",
  val keteranganJumlahHafalanBulanan: String = "",
  val nilaiJumlahHafalanHalaman: String = "",
  val nilaiJumlahHafalanJuz: String = "",
  val catatanMuhaffiz: String = "",
  val musyrif: String = "",
  val noHpMusyrif: String = "",
  val nilaiKehadiranLiqaMuhasabah: String = "",
  val sakitLiqaMuhasabah: String = "",
  val izinLiqaMuhasabah: String = "",
  val nilaiIbadah: String = "",
  val keteranganIbadah: String = "",
  val nilaiKedisiplinan: String = "",
  val keteranganKedisiplinan: String = "",
  val nilaiKebersihan: String = "",
  val keteranganKebersihan: String = "",
  val nilaiAdab: String = "",
  val keteranganAdab: String = "",
  val prestasiKesantrian: String = "",
  val pelanggaranKesantrian: String = "",
  val catatanMusyrif: String = ""
)

@Serializable
data class UtsReportSnapshot(
  val guruId: String = "",
  val semesters: List<UtsSemesterInfo> = emptyList(),
  val classSubjects: List<UtsClassSubject> = emptyList(),
  val scoreRows: List<UtsScoreRow> = emptyList(),
  val attendanceSummaries: List<UtsAttendanceSummary> = emptyList(),
  val overrides: List<UtsReportOverride> = emptyList(),
  val updatedAt: Long = 0L,
  val missingOverrideTable: Boolean = false
)

@Serializable
data class UtsSemesterInfo(
  val id: String = "",
  val label: String = "",
  val tahunAjaranId: String = "",
  val startDateIso: String = "",
  val endDateIso: String = "",
  val isActive: Boolean = false
)

@Serializable
data class UtsClassSubject(
  val classId: String = "",
  val semesterId: String = "",
  val mapelId: String = "",
  val name: String = ""
)

@Serializable
data class UtsScoreRow(
  val studentId: String = "",
  val semesterId: String = "",
  val mapelId: String = "",
  val scoreText: String = "",
  val scoreValue: Double? = null
)

@Serializable
data class UtsAttendanceSummary(
  val studentId: String = "",
  val semesterId: String = "",
  val kelasIzinCount: Int = 0,
  val kelasSakitCount: Int = 0,
  val ptsDateIso: String = "",
  val rangeLabel: String = ""
)

@Serializable
data class UtsReportOverride(
  val id: String = "",
  val guruId: String = "",
  val semesterId: String = "",
  val classId: String = "",
  val studentId: String = "",
  val midTahfizCapaian: String = "",
  val midTahfizScore: String = "",
  val halaqahSakitText: String = "",
  val halaqahIzinText: String = "",
  val subjects: List<UtsSubjectOverride> = emptyList(),
  val updatedAt: String = ""
)

@Serializable
data class UtsSubjectOverride(
  val key: String = "",
  val name: String = "",
  val scoreText: String = "",
  val scoreValue: Double? = null
)

@Serializable
data class UtsReportSubject(
  val key: String = "",
  val name: String = "",
  val kkmText: String = "17",
  val scoreText: String = "-",
  val scoreValue: Double? = null
)

@Serializable
data class UtsReportPayload(
  val studentId: String = "",
  val studentName: String = "",
  val studentNisn: String = "",
  val classId: String = "",
  val className: String = "",
  val waliKelasName: String = "",
  val semesterId: String = "",
  val semesterLabel: String = "",
  val subjects: List<UtsReportSubject> = emptyList(),
  val totalScoreText: String = "-",
  val averageScoreText: String = "-",
  val kelasIzinText: String = "0",
  val kelasSakitText: String = "0",
  val halaqahIzinText: String = "",
  val halaqahSakitText: String = "",
  val midTahfizCapaian: String = "",
  val midTahfizScore: String = "",
  val ptsDateIso: String = "",
  val attendanceRangeLabel: String = "-"
)

@Serializable
data class SubjectOverview(
  val id: String,
  val title: String,
  val className: String,
  val semester: String,
  val semesterActive: Boolean,
  val attendancePending: Int,
  val scorePending: Int,
  val materialCount: Int
)

@Serializable
data class AvailableSubjectOffer(
  val id: String,
  val title: String,
  val className: String,
  val semester: String,
  val semesterActive: Boolean
)

@Serializable
data class AttendanceStudent(
  val id: String,
  val name: String,
  val hadirPercent: Int,
  val terlambatPercent: Int,
  val sakitPercent: Int,
  val izinPercent: Int,
  val alpaPercent: Int,
  val history: List<AttendanceHistoryEntry> = emptyList()
)

@Serializable
data class AttendanceHistoryEntry(
  val dateIso: String,
  val status: String,
  val rowIds: List<String> = emptyList(),
  val lessonSlotId: String = "",
  val patronMateri: String = ""
)

@Serializable
data class AttendanceApprovalStudent(
  val studentId: String,
  val studentName: String,
  val status: String
)

@Serializable
data class AttendanceApprovalRequest(
  val id: String,
  val distribusiId: String = "",
  val dateIso: String,
  val className: String,
  val subjectTitle: String,
  val sourceTeacherId: String = "",
  val substituteTeacherId: String = "",
  val substituteTeacherName: String = "",
  val lessonLabels: List<String> = emptyList(),
  val material: String = "",
  val note: String = "",
  val students: List<AttendanceApprovalStudent> = emptyList(),
  val createdAtMillis: Long = 0L
)

@Serializable
data class MapelAttendanceSnapshot(
  val distribusiId: String,
  val subjectTitle: String,
  val className: String,
  val rangeLabel: String,
  val students: List<AttendanceStudent>,
  val updatedAt: Long,
  val supportsPatronMateri: Boolean = false
)

@Serializable
data class PendingAttendanceBatch(
  val id: String = "",
  val distribusiId: String = "",
  val subjectTitle: String = "",
  val className: String = "",
  val changesByStudent: Map<String, List<AttendanceHistoryEntry>> = emptyMap(),
  val sourceTeacherId: String = "",
  val substituteNote: String = "",
  val createdAt: Long = 0L
)

@Serializable
data class ScoreStudent(
  val id: String,
  val name: String,
  val rowId: String = "",
  val nilaiTugas: Double? = null,
  val nilaiUlanganHarian: Double? = null,
  val nilaiPts: Double? = null,
  val nilaiPas: Double? = null,
  val nilaiKehadiran: Double? = null,
  val nilaiAkhir: Double? = null,
  val nilaiKeterampilan: Double? = null,
  val detailRowsByMetric: Map<String, List<ScoreDetailRow>> = emptyMap(),
  val pendingDetailMetricKey: String = "",
  val pendingDeletedDetailIds: List<String> = emptyList()
)

@Serializable
data class ScoreDetailRow(
  val id: String = "",
  val dateIso: String = "",
  val value: Double? = null,
  val material: String = ""
)

@Serializable
data class MapelScoreSnapshot(
  val distribusiId: String,
  val subjectTitle: String,
  val className: String,
  val students: List<ScoreStudent>,
  val updatedAt: Long,
  val supportsMaterialHistory: Boolean = false
)

@Serializable
data class PendingScoreBatch(
  val id: String = "",
  val distribusiId: String = "",
  val subjectTitle: String = "",
  val className: String = "",
  val metricKey: String = "",
  val students: List<ScoreStudent> = emptyList(),
  val createdAt: Long = 0L
)

@Serializable
data class PatronMateriItem(
  val id: String,
  val text: String
)

@Serializable
data class MapelPatronMateriSnapshot(
  val distribusiId: String,
  val subjectTitle: String,
  val className: String,
  val items: List<PatronMateriItem>,
  val updatedAt: Long,
  val containsRecoveredText: Boolean = false
)

@Serializable
data class NoticeItem(
  val id: String,
  val title: String,
  val body: String
)

@Serializable
data class QuickStat(
  val id: String,
  val label: String,
  val value: String
)

@Serializable
data class DashboardCategory(
  val id: String,
  val title: String,
  val subtitle: String,
  val colorHex: String
)

@Serializable
data class DashboardTask(
  val id: String,
  val title: String,
  val timeLabel: String,
  val colorHex: String,
  val categoryKey: String = "semua",
  val description: String = ""
)

@Serializable
data class CalendarEvent(
  val id: String,
  val startDateIso: String = "",
  val endDateIso: String = "",
  val title: String,
  val description: String,
  val timeLabel: String,
  val colorHex: String,
  val categoryKey: String = "kegiatan_umum",
  val lessonSlotId: String = "",
  val distribusiId: String = ""
)

@Serializable
data class AppNotification(
  val id: String,
  val typeLabel: String = "",
  val title: String,
  val metaLabel: String = "",
  val description: String,
  val timeLabel: String,
  val createdAtMillis: Long,
  val isRead: Boolean = false,
  val kind: String = "info",
  val actionType: String = "",
  val actionId: String = "",
  val actionDateIso: String = "",
  val actionDistribusiId: String = "",
  val actionLessonSlotId: String = ""
)

@Serializable
data class InputAbsensiNavigationTarget(
  val requestId: String,
  val dateIso: String,
  val distribusiId: String = "",
  val lessonSlotId: String = ""
)

@Serializable
data class TeachingReminderSettings(
  val enabled: Boolean = false,
  val minutesBefore: Int = 10,
  val targetMode: String = "all",
  val selectedDistribusiIds: List<String> = emptyList(),
  val repeatMode: String = "every_lesson",
  val ringtoneUri: String = "",
  val ringtoneLabel: String = "Nada default sistem",
  val scheduledReminderIds: List<String> = emptyList(),
  val updatedAt: Long = 0L
)

data class SessionSnapshot(
  val isLoggedIn: Boolean = false,
  val teacherRowId: String = "",
  val teacherId: String = "",
  val teacherName: String = "",
  val activeRole: String = "",
  val roles: List<String> = emptyList(),
  val lastLoginAt: Long = 0L
)

data class SyncBannerState(
  val message: String = "",
  val isSyncing: Boolean = false
)
