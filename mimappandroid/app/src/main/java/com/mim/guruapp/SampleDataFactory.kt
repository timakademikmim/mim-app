package com.mim.guruapp

import com.mim.guruapp.data.model.DashboardPayload
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.DashboardCategory
import com.mim.guruapp.data.model.NoticeItem
import com.mim.guruapp.data.model.QuickStat
import com.mim.guruapp.data.model.AvailableSubjectOffer
import com.mim.guruapp.data.model.AppNotification
import com.mim.guruapp.data.model.AttendanceStudent
import com.mim.guruapp.data.model.AttendanceHistoryEntry
import com.mim.guruapp.data.model.DashboardTask
import com.mim.guruapp.data.model.GuruProfile
import com.mim.guruapp.data.model.LeaveRequestItem
import com.mim.guruapp.data.model.LeaveRequestSnapshot
import com.mim.guruapp.data.model.MapelAttendanceSnapshot
import com.mim.guruapp.data.model.MapelPatronMateriSnapshot
import com.mim.guruapp.data.model.MapelScoreSnapshot
import com.mim.guruapp.data.model.MutabaahSnapshot
import com.mim.guruapp.data.model.MutabaahSubmission
import com.mim.guruapp.data.model.MutabaahTask
import com.mim.guruapp.data.model.PatronMateriItem
import com.mim.guruapp.data.model.ScoreStudent
import com.mim.guruapp.data.model.SubjectOverview
import com.mim.guruapp.data.model.TeacherOption
import com.mim.guruapp.data.remote.GuruTeachingScheduleRemoteDataSource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object SampleDataFactory {
  fun createDashboard(
    teacherName: String,
    lastSuccessfulSyncAt: Long = System.currentTimeMillis(),
    lastServerRefreshAt: Long = lastSuccessfulSyncAt
  ): DashboardPayload {
    val today = LocalDate.now()
    val now = System.currentTimeMillis()
    val notifications = buildSampleNotifications(now)
    val teachingScheduleEvents = listOf(
      CalendarEvent(
        id = "event-teaching-1",
        startDateIso = today.toString(),
        endDateIso = today.toString(),
        title = "Nahwu - Kelas X-A",
        description = "Jadwal mengajar reguler guru pada ${today.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))}.",
        timeLabel = "07:15-08:00",
        colorHex = CATEGORY_TEACHING_COLOR,
        categoryKey = CATEGORY_TEACHING,
        distribusiId = "sample-distribusi-1"
      ),
      CalendarEvent(
        id = "event-teaching-2",
        startDateIso = today.toString(),
        endDateIso = today.toString(),
        title = "Sharf - Kelas X-B",
        description = "Jadwal mengajar reguler guru pada ${today.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))}.",
        timeLabel = "09:00-09:45",
        colorHex = CATEGORY_TEACHING_COLOR,
        categoryKey = CATEGORY_TEACHING,
        distribusiId = "sample-distribusi-2"
      )
    )
    val calendarEvents = listOf(
      CalendarEvent(
        id = "event-1",
        startDateIso = today.toString(),
        endDateIso = today.toString(),
        title = "Briefing guru mapel",
        description = "Sinkronkan agenda kelas, target materi, dan tindak lanjut absensi harian.",
        timeLabel = "07:15 AM",
        colorHex = "#DDEBFF",
        categoryKey = CATEGORY_GENERAL
      ),
      CalendarEvent(
        id = "event-2",
        startDateIso = today.toString(),
        endDateIso = today.toString(),
        title = "Input absensi kelas X-A",
        description = "Finalisasi absensi dan catatan perizinan sebelum jam istirahat.",
        timeLabel = "09:00 AM",
        colorHex = "#D9F4E7",
        categoryKey = CATEGORY_GENERAL
      ),
      CalendarEvent(
        id = "event-3",
        startDateIso = today.toString(),
        endDateIso = today.plusDays(1).toString(),
        title = "Review bank soal Nahwu",
        description = "Rapikan butir soal pilihan ganda dan pastikan metadata semester sesuai.",
        timeLabel = "01:30 PM",
        colorHex = "#FFE7C7",
        categoryKey = CATEGORY_GENERAL
      ),
      CalendarEvent(
        id = "event-4",
        startDateIso = today.plusDays(1).toString(),
        endDateIso = today.plusDays(1).toString(),
        title = "Patron materi Sharf",
        description = "Susun urutan pertemuan pekan ini dan cek referensi materi yang belum terbit.",
        timeLabel = "08:20 AM",
        colorHex = "#F5E2FF",
        categoryKey = CATEGORY_GENERAL
      ),
      CalendarEvent(
        id = "event-5",
        startDateIso = today.plusDays(2).toString(),
        endDateIso = today.plusDays(4).toString(),
        title = "Libur Akademik Tengah Semester",
        description = "Seluruh kegiatan akademik formal diliburkan selama jeda tengah semester.",
        timeLabel = "10:00 AM",
        colorHex = "#FFD8E2",
        categoryKey = CATEGORY_LIBUR_AKADEMIK
      ),
      CalendarEvent(
        id = "event-6",
        startDateIso = today.minusDays(1).toString(),
        endDateIso = today.minusDays(1).toString(),
        title = "Libur Ketahfizan",
        description = "Kegiatan ketahfizan diliburkan untuk agenda khusus pondok.",
        timeLabel = "02:15 PM",
        colorHex = "#D7F0FF",
        categoryKey = CATEGORY_LIBUR_KETAHFIZAN
      ),
      CalendarEvent(
        id = "event-7",
        startDateIso = today.plusDays(5).toString(),
        endDateIso = today.plusDays(5).toString(),
        title = "Libur Semua Kegiatan",
        description = "Seluruh aktivitas akademik dan ketahfizan diliburkan pada tanggal ini.",
        timeLabel = "11:30 AM",
        colorHex = "#FFE7C7",
        categoryKey = CATEGORY_LIBUR_SEMUA
      )
    )

    return DashboardPayload(
      teacherName = teacherName,
      teacherRole = "Guru Mapel",
      greeting = "Selamat datang kembali. Data lokal siap dipakai walau koneksi belum stabil.",
      profile = GuruProfile(
        name = teacherName,
        address = "Kompleks Markaz Imam Malik, Makassar",
        username = "khaerurrahmat",
        password = "rahasia123",
        phoneNumber = "81234567890"
      ),
      subjects = listOf(
        SubjectOverview(
          id = "mapel-1",
          title = "Nahwu",
          className = "Kelas X-A",
          semester = "Ganjil 2026/2027",
          semesterActive = true,
          attendancePending = 2,
          scorePending = 7,
          materialCount = 14
        ),
        SubjectOverview(
          id = "mapel-2",
          title = "Sharf",
          className = "Kelas X-B",
          semester = "Ganjil 2026/2027",
          semesterActive = true,
          attendancePending = 1,
          scorePending = 4,
          materialCount = 11
        ),
        SubjectOverview(
          id = "mapel-3",
          title = "Bahasa Arab",
          className = "Kelas XI-A",
          semester = "Ganjil 2026/2027",
          semesterActive = true,
          attendancePending = 0,
          scorePending = 3,
          materialCount = 9
        )
      ),
      availableSubjects = listOf(
        AvailableSubjectOffer(
          id = "offer-1",
          title = "Tahfiz",
          className = "Kelas IX-C",
          semester = "Ganjil 2026/2027",
          semesterActive = true
        ),
        AvailableSubjectOffer(
          id = "offer-2",
          title = "Fiqih",
          className = "Kelas VIII-B",
          semester = "Ganjil 2026/2027",
          semesterActive = true
        )
      ),
      pendingClaimOffers = emptyList(),
      attendanceSnapshots = listOf(
        MapelAttendanceSnapshot(
          distribusiId = "mapel-1",
          subjectTitle = "Nahwu",
          className = "Kelas X-A",
          rangeLabel = "01 Apr 2026 - 24 Apr 2026",
          students = listOf(
            AttendanceStudent(
              "santri-1", "Ahmad Fauzan", 80, 5, 5, 5, 5,
              history = listOf(
                AttendanceHistoryEntry("2026-04-24", "Hadir"),
                AttendanceHistoryEntry("2026-04-23", "Hadir"),
                AttendanceHistoryEntry("2026-04-22", "Terlambat"),
                AttendanceHistoryEntry("2026-04-21", "Izin")
              )
            ),
            AttendanceStudent(
              "santri-2", "Bilal Ramadhan", 72, 12, 4, 8, 4,
              history = listOf(
                AttendanceHistoryEntry("2026-04-24", "Terlambat"),
                AttendanceHistoryEntry("2026-04-23", "Hadir"),
                AttendanceHistoryEntry("2026-04-22", "Sakit"),
                AttendanceHistoryEntry("2026-04-21", "Hadir")
              )
            ),
            AttendanceStudent(
              "santri-3", "Chandra Maulana", 78, 6, 6, 5, 5,
              history = listOf(
                AttendanceHistoryEntry("2026-04-24", "Izin"),
                AttendanceHistoryEntry("2026-04-23", "Hadir"),
                AttendanceHistoryEntry("2026-04-22", "Hadir")
              )
            ),
            AttendanceStudent(
              "santri-4", "Dzaki Naufal", 68, 7, 15, 5, 5,
              history = listOf(
                AttendanceHistoryEntry("2026-04-24", "Sakit"),
                AttendanceHistoryEntry("2026-04-23", "Sakit"),
                AttendanceHistoryEntry("2026-04-22", "Hadir")
              )
            ),
            AttendanceStudent(
              "santri-5", "Eka Saputra", 60, 5, 5, 10, 20,
              history = listOf(
                AttendanceHistoryEntry("2026-04-24", "Alpa"),
                AttendanceHistoryEntry("2026-04-23", "Izin"),
                AttendanceHistoryEntry("2026-04-22", "Hadir")
              )
            )
          ),
          updatedAt = now
        )
      ),
      scoreSnapshots = listOf(
        MapelScoreSnapshot(
          distribusiId = "mapel-1",
          subjectTitle = "Nahwu",
          className = "Kelas X-A",
          students = listOf(
            ScoreStudent(
              id = "santri-1",
              name = "Ahmad Fauzan",
              rowId = "nilai-1",
              nilaiTugas = 4.5,
              nilaiUlanganHarian = 8.0,
              nilaiPts = 21.0,
              nilaiPas = 43.0,
              nilaiKehadiran = 18.0,
              nilaiAkhir = 94.5,
              nilaiKeterampilan = 88.0
            ),
            ScoreStudent(
              id = "santri-2",
              name = "Bilal Ramadhan",
              rowId = "nilai-2",
              nilaiTugas = 4.0,
              nilaiUlanganHarian = 7.5,
              nilaiPts = 18.0,
              nilaiPas = 39.0,
              nilaiKehadiran = 17.0,
              nilaiAkhir = 85.5,
              nilaiKeterampilan = 82.0
            ),
            ScoreStudent(
              id = "santri-3",
              name = "Chandra Maulana",
              rowId = "nilai-3",
              nilaiTugas = 4.8,
              nilaiUlanganHarian = 9.0,
              nilaiPts = 22.0,
              nilaiPas = 45.0,
              nilaiKehadiran = 19.0,
              nilaiAkhir = 99.8,
              nilaiKeterampilan = 90.0
            )
          ),
          updatedAt = now
        )
      ),
      patronMateriSnapshots = listOf(
        MapelPatronMateriSnapshot(
          distribusiId = "mapel-1",
          subjectTitle = "Nahwu",
          className = "Kelas X-A",
          items = listOf(
            PatronMateriItem("materi-1", "Jumlah ismiyah dan fi'liyah"),
            PatronMateriItem("materi-2", "Mubtada khabar"),
            PatronMateriItem("materi-3", "I'rab dasar")
          ),
          updatedAt = now
        )
      ),
      notices = listOf(
        NoticeItem(
          id = "notice-1",
          title = "Mode Offline Aktif",
          body = "Input guru akan disimpan dulu di perangkat internal, lalu dikirim saat sinkronisasi berjalan."
        ),
        NoticeItem(
          id = "notice-2",
          title = "Refresh Terjadwal",
          body = "Data utama dapat diperbarui saat aplikasi dibuka, ditarik manual, atau berdasarkan interval waktu."
        )
      ),
      quickStats = listOf(
        QuickStat("stat-1", "Mapel Aktif", "3"),
        QuickStat("stat-2", "Input Pending", "17"),
        QuickStat("stat-3", "Materi Tersimpan", "34")
      ),
      categories = buildAgendaCategories(calendarEvents),
      ongoingTasks = buildDashboardAgendaTasks(calendarEvents, today),
      calendarEvents = calendarEvents,
      teachingScheduleEvents = teachingScheduleEvents,
      notifications = notifications,
      unreadNotificationCount = notifications.count { !it.isRead },
      substituteTeacherOptions = listOf(
        TeacherOption("guru-demo-1", "Ustadz Ahmad", "G001"),
        TeacherOption("guru-demo-2", "Ustadz Bilal", "G002")
      ),
      sourceTeacherOptions = listOf(
        TeacherOption("guru-demo-1", "Ustadz Ahmad", "G001")
      ),
      mutabaahSnapshot = buildSampleMutabaahSnapshot(today, now),
      leaveRequestSnapshot = buildSampleLeaveRequestSnapshot(today, now),
      pendingSyncCount = 17,
      lastSuccessfulSyncAt = lastSuccessfulSyncAt,
      lastServerRefreshAt = lastServerRefreshAt
    )
  }

  fun refreshDashboard(previous: DashboardPayload, now: Long = System.currentTimeMillis()): DashboardPayload {
    val nextPending = (previous.pendingSyncCount - 3).coerceAtLeast(0)
    val nextStats = previous.quickStats.map { stat ->
      when (stat.id) {
        "stat-2" -> stat.copy(value = nextPending.toString())
        else -> stat
      }
    }
    val nextSubjects = previous.subjects.mapIndexed { index, subject ->
      if (index == 0) {
        subject.copy(
          attendancePending = (subject.attendancePending - 1).coerceAtLeast(0),
          scorePending = (subject.scorePending - 2).coerceAtLeast(0)
        )
      } else {
        subject
      }
    }
    val notifications = refreshNotifications(previous.notifications, now)
    return previous.copy(
      greeting = "Data terbaru berhasil diambil dari server. Aplikasi tetap memakai cache lokal sebagai sumber utama tampilan.",
      quickStats = nextStats,
      subjects = nextSubjects,
      ongoingTasks = buildDashboardAgendaTasks(previous.calendarEvents, LocalDate.now()),
      notifications = notifications,
      unreadNotificationCount = notifications.count { !it.isRead },
      pendingSyncCount = nextPending,
      lastSuccessfulSyncAt = now,
      lastServerRefreshAt = now
    )
  }

  private fun buildSampleNotifications(now: Long): List<AppNotification> {
    return listOf(
      AppNotification(
        id = "notif-1",
        typeLabel = "Input Nilai",
        title = "Nilai berhasil diinput",
        metaLabel = "Hari ini | 07:30-08:10",
        description = "Input nilai Nahwu untuk Kelas X-A sudah tersimpan di perangkat dan siap disinkronkan.",
        timeLabel = "1 jam lalu",
        createdAtMillis = now - 60 * 60 * 1000L,
        isRead = false,
        kind = "success"
      ),
      AppNotification(
        id = "notif-2",
        typeLabel = "Absensi",
        title = "Absen telah diperbarui",
        metaLabel = "Hari ini | 09:00-09:40",
        description = "Absensi Bahasa Arab Kelas XI-A selesai diperbarui oleh guru mapel.",
        timeLabel = "3 jam lalu",
        createdAtMillis = now - 3 * 60 * 60 * 1000L,
        isRead = false,
        kind = "attendance"
      ),
      AppNotification(
        id = "notif-3",
        typeLabel = "Agenda Akademik",
        title = "Agenda baru ditambahkan",
        metaLabel = "23 Apr 2026 - 25 Apr 2026",
        description = "Kalender akademik menambahkan agenda libur akademik untuk tengah semester.",
        timeLabel = "Kemarin",
        createdAtMillis = now - 26 * 60 * 60 * 1000L,
        isRead = true,
        kind = "calendar"
      ),
      AppNotification(
        id = "notif-4",
        typeLabel = "Sinkronisasi",
        title = "Sinkronisasi selesai",
        metaLabel = "2 hari lalu",
        description = "Data lokal berhasil disamakan dengan database terbaru tanpa konflik.",
        timeLabel = "2 hari lalu",
        createdAtMillis = now - 2 * 24 * 60 * 60 * 1000L,
        isRead = true,
        kind = "sync"
      ),
      AppNotification(
        id = "notif-5",
        typeLabel = "Patron Materi",
        title = "Patron materi diperbarui",
        metaLabel = "5 hari lalu",
        description = "Urutan patron materi Sharf pekan ini sudah diperbarui dan siap ditinjau.",
        timeLabel = "5 hari lalu",
        createdAtMillis = now - 5 * 24 * 60 * 60 * 1000L,
        isRead = false,
        kind = "material"
      )
    )
  }

  private fun buildSampleLeaveRequestSnapshot(today: LocalDate, now: Long): LeaveRequestSnapshot {
    return LeaveRequestSnapshot(
      guruId = "guru-demo-1",
      requests = listOf(
        LeaveRequestItem(
          id = "izin-1",
          startDateIso = today.plusDays(2).toString(),
          endDateIso = today.plusDays(3).toString(),
          durationDays = 2,
          purpose = "Menghadiri acara keluarga di luar kota.",
          status = "menunggu",
          createdAt = "${today}T08:15:00Z",
          updatedAt = "${today}T08:15:00Z"
        ),
        LeaveRequestItem(
          id = "izin-2",
          startDateIso = today.minusDays(4).toString(),
          endDateIso = today.minusDays(4).toString(),
          durationDays = 1,
          purpose = "Kontrol kesehatan dan pemeriksaan dokter.",
          status = "diterima",
          reviewerNote = "Disetujui. Pastikan materi pengganti sudah disiapkan.",
          reviewedAt = "${today.minusDays(5)}T10:00:00Z",
          createdAt = "${today.minusDays(6)}T07:40:00Z",
          updatedAt = "${today.minusDays(5)}T10:00:00Z"
        ),
        LeaveRequestItem(
          id = "izin-3",
          startDateIso = today.minusDays(10).toString(),
          endDateIso = today.minusDays(8).toString(),
          durationDays = 3,
          purpose = "Keperluan administrasi keluarga.",
          status = "ditolak",
          reviewerNote = "Mohon ajukan ulang dengan rentang yang tidak berbenturan ujian.",
          reviewedAt = "${today.minusDays(11)}T09:10:00Z",
          createdAt = "${today.minusDays(12)}T06:55:00Z",
          updatedAt = "${today.minusDays(11)}T09:10:00Z"
        )
      ),
      updatedAt = now
    )
  }

  private fun buildSampleMutabaahSnapshot(today: LocalDate, now: Long): MutabaahSnapshot {
    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val tasks = buildList {
      for (dayOffset in 0..5) {
        val date = weekStart.plusDays(dayOffset.toLong())
        add(
          MutabaahTask(
            id = "mutabaah-${date}-shalat",
            dateIso = date.toString(),
            title = "Shalat berjamaah",
            description = "Pastikan laporan mutabaah shalat hari ini sudah tertandai."
          )
        )
        add(
          MutabaahTask(
            id = "mutabaah-${date}-tilawah",
            dateIso = date.toString(),
            title = "Tilawah harian",
            description = "Catat keterlaksanaan tilawah atau murajaah harian."
          )
        )
        add(
          MutabaahTask(
            id = "mutabaah-${date}-adab",
            dateIso = date.toString(),
            title = "Adab dan kedisiplinan",
            description = "Tinjau adab kelas, kebersihan, dan kedisiplinan umum."
          )
        )
      }
    }
    val submissions = tasks
      .filter { it.dateIso < today.toString() || it.title == "Shalat berjamaah" }
      .map { task ->
        MutabaahSubmission(
          id = "submit-${task.id}",
          templateId = task.id,
          dateIso = task.dateIso,
          status = "selesai",
          updatedAt = now.toString()
        )
      }
    return MutabaahSnapshot(
      guruId = "guru-demo",
      period = today.toString().take(7),
      tasks = tasks,
      submissions = submissions,
      updatedAt = now
    )
  }

  private fun refreshNotifications(previous: List<AppNotification>, now: Long): List<AppNotification> {
    val updated = if (previous.isEmpty()) {
      buildSampleNotifications(now)
    } else {
      previous
    }
    val newNotification = AppNotification(
      id = "notif-sync-$now",
      typeLabel = "Sinkronisasi",
      title = "Pembaruan server diterima",
      metaLabel = "Baru saja",
      description = "Aplikasi menerima pembaruan data terbaru dari server dan menyimpannya ke cache lokal.",
      timeLabel = "Baru saja",
      createdAtMillis = now,
      isRead = false,
      kind = "sync"
    )
    return (listOf(newNotification) + updated)
      .sortedByDescending { it.createdAtMillis }
      .take(20)
  }

  private fun buildDashboardAgendaTasks(
    events: List<CalendarEvent>,
    referenceDate: LocalDate
  ): List<DashboardTask> {
    return events
      .sortedWith(
        compareBy<CalendarEvent> { eventPriority(it, referenceDate) }
          .thenBy { parseEventStart(it) }
          .thenBy { it.timeLabel }
      )
      .take(4)
      .map { event ->
        DashboardTask(
          id = "agenda-${event.id}",
          title = event.title,
          timeLabel = formatEventRangeLabel(event),
          colorHex = event.colorHex,
          categoryKey = event.categoryKey,
          description = event.description
        )
      }
  }

  private fun eventPriority(
    event: CalendarEvent,
    referenceDate: LocalDate
  ): Int {
    val start = parseEventStart(event)
    val end = parseEventEnd(event)
    return when {
      referenceDate in start..end -> 0
      start.isAfter(referenceDate) -> 1
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

  private fun parseEventStart(event: CalendarEvent): LocalDate {
    return runCatching { LocalDate.parse(event.startDateIso) }.getOrDefault(LocalDate.now())
  }

  private fun parseEventEnd(event: CalendarEvent): LocalDate {
    return runCatching { LocalDate.parse(event.endDateIso.ifBlank { event.startDateIso }) }
      .getOrDefault(parseEventStart(event))
  }

  private fun buildAgendaCategories(events: List<CalendarEvent>): List<DashboardCategory> {
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
        DashboardCategory(
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

  private const val CATEGORY_ALL = "semua"
  private const val CATEGORY_TEACHING = GuruTeachingScheduleRemoteDataSource.CATEGORY_TEACHING
  private const val CATEGORY_TEACHING_COLOR = GuruTeachingScheduleRemoteDataSource.CATEGORY_TEACHING_COLOR
  private const val CATEGORY_GENERAL = "kegiatan_umum"
  private const val CATEGORY_LIBUR_AKADEMIK = "libur_akademik"
  private const val CATEGORY_LIBUR_KETAHFIZAN = "libur_ketahfizan"
  private const val CATEGORY_LIBUR_SEMUA = "libur_semua_kegiatan"
}
