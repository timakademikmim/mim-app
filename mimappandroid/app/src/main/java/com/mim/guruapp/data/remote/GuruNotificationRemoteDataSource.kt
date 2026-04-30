package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.AppNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class GuruNotificationRemoteDataSource {
  suspend fun fetchNotifications(
    teacherRowId: String,
    teacherKaryawanId: String,
    rangeDays: Int = 7
  ): List<AppNotification>? = withContext(Dispatchers.IO) {
    runCatching {
      val resolvedTeacherRowId = teacherRowId.ifBlank { resolveTeacherRowId(teacherKaryawanId) }
      if (resolvedTeacherRowId.isBlank()) return@runCatching emptyList()

      val today = LocalDate.now()
      val endDate = today.plusDays((rangeDays - 1).coerceAtLeast(0).toLong())
      val result = mutableListOf<AppNotification>()

      val holidayDateSet = mutableSetOf<String>()
      val calendarRows = fetchRows(
        table = "kalender_akademik",
        query = "select=id,judul,mulai,selesai,detail,warna,jenis_kegiatan&order=mulai.asc"
      )
      result += buildCalendarNotifications(
        rows = calendarRows,
        startDate = today,
        endDate = endDate,
        holidayDateSet = holidayDateSet
      )

      val activeSemesterId = resolveActiveSemesterId()
      val distribusiRows = fetchDistribusiRows(
        teacherRowId = resolvedTeacherRowId,
        teacherKaryawanId = teacherKaryawanId,
        activeSemesterId = activeSemesterId
      )
      val jadwalRows = fetchJadwalRows(distribusiRows.map { it.id })
      val kelasMap = fetchNameMap("kelas", "nama_kelas", distribusiRows.map { it.kelasId })
      val mapelMap = fetchNameMap("mapel", "nama", distribusiRows.map { it.mapelId })
      result += buildTeachingNotifications(
        distribusiRows = distribusiRows,
        jadwalRows = jadwalRows,
        kelasMap = kelasMap,
        mapelMap = mapelMap,
        holidayDates = holidayDateSet,
        startDate = today,
        endDate = endDate
      )

      result += buildAttendanceApprovalNotifications(
        teacherRowId = resolvedTeacherRowId,
        teacherKaryawanId = teacherKaryawanId,
        kelasMap = kelasMap,
        mapelMap = mapelMap
      )

      result += buildDelegationNotifications(
        teacherRowId = resolvedTeacherRowId,
        teacherKaryawanId = teacherKaryawanId
      )

      result
        .sortedByDescending { it.createdAtMillis }
        .distinctBy { it.id }
        .take(30)
    }.getOrNull()
  }

  private fun resolveTeacherRowId(teacherKaryawanId: String): String {
    if (teacherKaryawanId.isBlank()) return ""
    val encoded = URLEncoder.encode(teacherKaryawanId, Charsets.UTF_8.name())
    val rows = fetchRows("karyawan", "select=id&id_karyawan=eq.$encoded&limit=1")
    return rows.firstOrNull()?.opt("id")?.toString().orEmpty().trim()
  }

  private fun resolveActiveSemesterId(): String {
    val rows = fetchRows("semester", "select=id,aktif&aktif=eq.true&limit=1")
    return rows.firstOrNull()?.opt("id")?.toString().orEmpty().trim()
  }

  private fun fetchDistribusiRows(
    teacherRowId: String,
    teacherKaryawanId: String,
    activeSemesterId: String
  ): List<DistribusiRow> {
    val primaryQuery = buildString {
      append("select=id,kelas_id,mapel_id,guru_id,semester_id&guru_id=eq.")
      append(URLEncoder.encode(teacherRowId, Charsets.UTF_8.name()))
      if (activeSemesterId.isNotBlank()) {
        append("&semester_id=eq.")
        append(URLEncoder.encode(activeSemesterId, Charsets.UTF_8.name()))
      }
    }
    var rows = fetchRows("distribusi_mapel", primaryQuery)
    if (rows.isEmpty() && teacherKaryawanId.isNotBlank()) {
      val secondaryQuery = buildString {
        append("select=id,kelas_id,mapel_id,guru_id,semester_id&guru_id=eq.")
        append(URLEncoder.encode(teacherKaryawanId, Charsets.UTF_8.name()))
        if (activeSemesterId.isNotBlank()) {
          append("&semester_id=eq.")
          append(URLEncoder.encode(activeSemesterId, Charsets.UTF_8.name()))
        }
      }
      rows = fetchRows("distribusi_mapel", secondaryQuery)
    }
    if (rows.isEmpty() && activeSemesterId.isNotBlank()) {
      val fallbackQuery = buildString {
        append("select=id,kelas_id,mapel_id,guru_id,semester_id&guru_id=eq.")
        append(URLEncoder.encode(teacherRowId, Charsets.UTF_8.name()))
      }
      rows = fetchRows("distribusi_mapel", fallbackQuery)
    }
    if (rows.isEmpty() && teacherKaryawanId.isNotBlank()) {
      val fallbackSecondaryQuery = buildString {
        append("select=id,kelas_id,mapel_id,guru_id,semester_id&guru_id=eq.")
        append(URLEncoder.encode(teacherKaryawanId, Charsets.UTF_8.name()))
      }
      rows = fetchRows("distribusi_mapel", fallbackSecondaryQuery)
    }
    return rows
      .mapNotNull { row ->
        val id = row.opt("id")?.toString().orEmpty().trim()
        val kelasId = row.optString("kelas_id").trim()
        val mapelId = row.optString("mapel_id").trim()
        if (id.isBlank() || kelasId.isBlank() || mapelId.isBlank()) {
          null
        } else {
          DistribusiRow(id = id, kelasId = kelasId, mapelId = mapelId)
        }
      }
  }

  private fun fetchJadwalRows(distribusiIds: List<String>): List<JadwalRow> {
    if (distribusiIds.isEmpty()) return emptyList()
    val inClause = distribusiIds.distinct().joinToString(",") { "\"${it}\"" }
    val rowsWithJamPelajaran = fetchRows(
      "jadwal_pelajaran",
      "select=id,distribusi_id,hari,jam_mulai,jam_selesai,jam_pelajaran_id&distribusi_id=in.($inClause)"
    )
    val rows = rowsWithJamPelajaran.ifEmpty {
      fetchRows(
        "jadwal_pelajaran",
        "select=id,distribusi_id,hari,jam_mulai,jam_selesai&distribusi_id=in.($inClause)"
      )
    }
    val mappedRows = rows.mapNotNull { row ->
      val id = row.opt("id")?.toString().orEmpty().trim()
      val distribusiId = row.optString("distribusi_id").trim()
      val hari = row.optString("hari").trim()
      if (id.isBlank() || distribusiId.isBlank() || hari.isBlank()) {
        null
      } else {
        JadwalRow(
          id = id,
          distribusiId = distribusiId,
          hari = hari,
          jamMulai = row.optString("jam_mulai").trim(),
          jamSelesai = row.optString("jam_selesai").trim(),
          jamPelajaranId = row.optString("jam_pelajaran_id").trim()
        )
      }
    }
    if (mappedRows.none { it.jamPelajaranId.isBlank() }) return mappedRows

    val fallbackJamIdsByTime = fetchJamIdsByTime()
    if (fallbackJamIdsByTime.isEmpty()) return mappedRows
    return mappedRows.map { row ->
      if (row.jamPelajaranId.isNotBlank()) row else row.copy(
        jamPelajaranId = fallbackJamIdsByTime[jamTimeKey(row.jamMulai, row.jamSelesai)].orEmpty()
      )
    }
  }

  private fun fetchJamIdsByTime(): Map<String, String> {
    return fetchRows("jam_pelajaran", "select=id,jam_mulai,jam_selesai")
      .mapNotNull { row ->
        val id = row.opt("id")?.toString().orEmpty().trim()
        val key = jamTimeKey(row.optString("jam_mulai"), row.optString("jam_selesai"))
        if (id.isBlank() || key.isBlank()) null else key to id
      }
      .toMap()
  }

  private fun fetchNameMap(
    table: String,
    nameField: String,
    ids: List<String>
  ): Map<String, String> {
    if (ids.isEmpty()) return emptyMap()
    val inClause = ids.distinct().joinToString(",") { "\"${it}\"" }
    return fetchRows(table, "select=id,$nameField&id=in.($inClause)")
      .associate { row ->
        row.opt("id")?.toString().orEmpty().trim() to row.optString(nameField).trim()
      }
      .filterKeys { it.isNotBlank() }
  }

  private fun buildCalendarNotifications(
    rows: List<JSONObject>,
    startDate: LocalDate,
    endDate: LocalDate,
    holidayDateSet: MutableSet<String>
  ): List<AppNotification> {
    return rows.mapNotNull { row ->
      val start = parseDate(row.optString("mulai")) ?: return@mapNotNull null
      val end = parseDate(row.optString("selesai")) ?: start
      if (end.isBefore(startDate) || start.isAfter(endDate)) return@mapNotNull null
      val title = cleanDisplayText(row.opt("judul")).ifBlank { "Agenda Akademik" }
      val description = cleanDisplayText(row.opt("detail"))

      val kind = inferActivityKind(
        jenis = row.optString("jenis_kegiatan"),
        title = title,
        desc = description
      )
      if (kind == "academic_holiday" || kind == "all_holiday") {
        var cursor = start
        while (!cursor.isAfter(end)) {
          holidayDateSet += cursor.toString()
          cursor = cursor.plusDays(1)
        }
      }

      AppNotification(
        id = "agenda|${row.opt("id")?.toString().orEmpty().ifBlank { start.toString() }}",
        typeLabel = when (kind) {
          "all_holiday" -> "Libur Semua Kegiatan"
          "academic_holiday" -> "Libur Akademik"
          "tahfizh_holiday" -> "Libur Ketahfizan"
          else -> "Agenda Akademik"
        },
        title = title,
        metaLabel = formatDateRangeLabel(start, end),
        description = description,
        timeLabel = relativeDayLabel(start, startDate),
        createdAtMillis = startOfDayMillis(start),
        isRead = false,
        kind = kind
      )
    }
  }

  private fun buildTeachingNotifications(
    distribusiRows: List<DistribusiRow>,
    jadwalRows: List<JadwalRow>,
    kelasMap: Map<String, String>,
    mapelMap: Map<String, String>,
    holidayDates: Set<String>,
    startDate: LocalDate,
    endDate: LocalDate
  ): List<AppNotification> {
    if (distribusiRows.isEmpty() || jadwalRows.isEmpty()) return emptyList()
    val distribusiMap = distribusiRows.associateBy { it.id }
    val result = mutableListOf<AppNotification>()

    var cursor = startDate
    while (!cursor.isAfter(endDate)) {
      val expectedDay = normalizeHari(cursor.dayOfWeek)
      jadwalRows.forEach { jadwal ->
        if (holidayDates.contains(cursor.toString())) return@forEach
        if (normalizeHari(jadwal.hari) != expectedDay) return@forEach
        val distribusi = distribusiMap[jadwal.distribusiId] ?: return@forEach
        val kelasLabel = kelasMap[distribusi.kelasId].orEmpty().ifBlank { "-" }
        val mapelLabel = mapelMap[distribusi.mapelId].orEmpty().ifBlank { "-" }
        result += AppNotification(
          id = "ajar|${cursor}|${jadwal.id}|${distribusi.id}",
          typeLabel = "Jam Mengajar",
          title = "$mapelLabel - $kelasLabel",
          metaLabel = "${formatNotifDateLabel(cursor)} | ${toTimeLabel(jadwal.jamMulai)}-${toTimeLabel(jadwal.jamSelesai)}",
          description = "",
          timeLabel = relativeDayLabel(cursor, startDate),
          createdAtMillis = startOfDayMillis(cursor),
          isRead = false,
          kind = "teaching",
          actionType = "open_input_absensi",
          actionDateIso = cursor.toString(),
          actionDistribusiId = distribusi.id,
          actionLessonSlotId = jadwal.jamPelajaranId
        )
      }
      cursor = cursor.plusDays(1)
    }

    return result
  }

  private fun buildAttendanceApprovalNotifications(
    teacherRowId: String,
    teacherKaryawanId: String,
    kelasMap: Map<String, String>,
    mapelMap: Map<String, String>
  ): List<AppNotification> {
    val encodedTeacherId = URLEncoder.encode(teacherRowId, Charsets.UTF_8.name())
    var rows = fetchRows(
      table = "absensi_pengajuan_pengganti",
      query = "select=id,tanggal,kelas_id,mapel_id,jam_pelajaran_1_id,jam_pelajaran_2_id,created_at,status&guru_asal_id=eq.$encodedTeacherId&status=eq.pending&order=created_at.desc"
    )
    if (rows.isEmpty() && teacherKaryawanId.isNotBlank()) {
      val encodedTeacherKaryawanId = URLEncoder.encode(teacherKaryawanId, Charsets.UTF_8.name())
      rows = fetchRows(
        table = "absensi_pengajuan_pengganti",
        query = "select=id,tanggal,kelas_id,mapel_id,jam_pelajaran_1_id,jam_pelajaran_2_id,created_at,status&guru_asal_id=eq.$encodedTeacherKaryawanId&status=eq.pending&order=created_at.desc"
      )
    }
    if (rows.isEmpty()) return emptyList()

    val jamIds = rows.flatMap { row ->
      listOf(
        row.optString("jam_pelajaran_1_id").trim(),
        row.optString("jam_pelajaran_2_id").trim()
      )
    }.filter { it.isNotBlank() }.distinct()
    val jamMap = fetchJamMap(jamIds)
    val guruPenggantiIds = rows
      .map { row -> row.optString("guru_pengganti_id").trim() }
      .filter { it.isNotBlank() }
      .distinct()
    val guruMap = fetchNameMap("karyawan", "nama", guruPenggantiIds)

    return rows.mapNotNull { row ->
      val id = row.opt("id")?.toString().orEmpty().trim()
      if (id.isBlank()) return@mapNotNull null

      val tanggal = parseDate(row.optString("tanggal")) ?: LocalDate.now()
      val kelasLabel = kelasMap[row.optString("kelas_id").trim()].orEmpty().ifBlank { "-" }
      val mapelLabel = mapelMap[row.optString("mapel_id").trim()].orEmpty().ifBlank { "-" }
      val guruPenggantiLabel = guruMap[row.optString("guru_pengganti_id").trim()].orEmpty().ifBlank { "Guru Pengganti" }
      val jamLabels = listOf(
        row.optString("jam_pelajaran_1_id").trim(),
        row.optString("jam_pelajaran_2_id").trim()
      ).mapNotNull { jamMap[it] }

      AppNotification(
        id = "absensi-approval|$id",
        typeLabel = "Persetujuan Absensi",
        title = "$mapelLabel - $kelasLabel",
        metaLabel = buildString {
          append(formatNotifDateLabel(tanggal))
          if (jamLabels.isNotEmpty()) append(" | ${jamLabels.joinToString(", ")}")
        },
        description = "$guruPenggantiLabel mengirim absensi pengganti dan menunggu persetujuan.",
        timeLabel = relativeDayLabel(tanggal, LocalDate.now()),
        createdAtMillis = parseTimestampMillis(row.optString("created_at")).takeIf { it > 0L }
          ?: startOfDayMillis(tanggal),
        isRead = false,
        kind = "approval",
        actionType = "attendance_approval",
        actionId = id
      )
    }
  }

  private fun buildDelegationNotifications(
    teacherRowId: String,
    teacherKaryawanId: String
  ): List<AppNotification> {
    val teacherIds = listOf(teacherRowId, teacherKaryawanId)
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
    if (teacherIds.isEmpty()) return emptyList()

    val rows = teacherIds
      .flatMap { teacherId ->
        val encodedTeacherId = URLEncoder.encode(teacherId, Charsets.UTF_8.name())
        fetchRows(
          table = "absensi_pengganti_tugas",
          query = "select=id,tanggal,kelas_id,mapel_id,distribusi_id,jam_pelajaran_id,guru_asal_id,created_at,status,keterangan&guru_pengganti_id=eq.$encodedTeacherId&status=eq.pending&order=created_at.desc"
        )
      }
      .distinctBy { row -> row.opt("id")?.toString().orEmpty().trim() }
    if (rows.isEmpty()) return emptyList()

    val kelasMap = fetchNameMap(
      "kelas",
      "nama_kelas",
      rows.map { row -> row.optString("kelas_id").trim() }.filter { it.isNotBlank() }
    )
    val mapelMap = fetchNameMap(
      "mapel",
      "nama",
      rows.map { row -> row.optString("mapel_id").trim() }.filter { it.isNotBlank() }
    )
    val guruMap = fetchNameMap(
      "karyawan",
      "nama",
      rows.map { row -> row.optString("guru_asal_id").trim() }.filter { it.isNotBlank() }
    )
    val jamMap = fetchJamMap(
      rows.map { row -> row.optString("jam_pelajaran_id").trim() }.filter { it.isNotBlank() }
    )

    return rows.mapNotNull { row ->
      val id = row.opt("id")?.toString().orEmpty().trim()
      if (id.isBlank()) return@mapNotNull null

      val tanggal = parseDate(row.optString("tanggal")) ?: LocalDate.now()
      val kelasLabel = kelasMap[row.optString("kelas_id").trim()].orEmpty().ifBlank { "-" }
      val mapelLabel = mapelMap[row.optString("mapel_id").trim()].orEmpty().ifBlank { "Mapel" }
      val guruAsalLabel = guruMap[row.optString("guru_asal_id").trim()].orEmpty().ifBlank { "Guru Utama" }
      val jamLabel = jamMap[row.optString("jam_pelajaran_id").trim()].orEmpty()
      val note = cleanDisplayText(row.opt("keterangan"))

      AppNotification(
        id = "attendance-delegation|$id",
        typeLabel = "Amanat Pengganti",
        title = "$mapelLabel - $kelasLabel",
        metaLabel = buildString {
          append(formatNotifDateLabel(tanggal))
          if (jamLabel.isNotBlank()) append(" | $jamLabel")
        },
        description = buildString {
          append("$guruAsalLabel memberi amanat penggantian absensi.")
          if (note.isNotBlank()) append(" Catatan: $note")
        },
        timeLabel = relativeDayLabel(tanggal, LocalDate.now()),
        createdAtMillis = parseTimestampMillis(row.optString("created_at")).takeIf { it > 0L }
          ?: startOfDayMillis(tanggal),
        isRead = false,
        kind = "delegation",
        actionType = "open_input_absensi",
        actionId = id,
        actionDateIso = row.optString("tanggal").trim().take(10),
        actionDistribusiId = row.optString("distribusi_id").trim(),
        actionLessonSlotId = row.optString("jam_pelajaran_id").trim()
      )
    }
  }

  private fun fetchJamMap(jamIds: List<String>): Map<String, String> {
    if (jamIds.isEmpty()) return emptyMap()
    val inClause = jamIds.distinct().joinToString(",") { "\"${it}\"" }
    return fetchRows("jam_pelajaran", "select=id,nama,jam_mulai,jam_selesai&id=in.($inClause)")
      .associate { row ->
        val id = row.opt("id")?.toString().orEmpty().trim()
        val label = buildString {
          append(row.optString("nama").trim().ifBlank { "Jam" })
          append(" (${toTimeLabel(row.optString("jam_mulai"))}-${toTimeLabel(row.optString("jam_selesai"))})")
        }
        id to label
      }
      .filterKeys { it.isNotBlank() }
  }

  private fun fetchRows(
    table: String,
    query: String
  ): List<JSONObject> {
    val url = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 15_000
      readTimeout = 15_000
      setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
      setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
      setRequestProperty("Accept", "application/json")
    }

    return try {
      if (connection.responseCode !in 200..299) return emptyList()
      val payload = connection.inputStream.bufferedReader().use(BufferedReader::readText)
      val rows = JSONArray(payload.ifBlank { "[]" })
      buildList {
        for (index in 0 until rows.length()) {
          val item = rows.optJSONObject(index) ?: continue
          add(item)
        }
      }
    } finally {
      connection.disconnect()
    }
  }

  private fun cleanDisplayText(value: Any?): String {
    if (value == null || value == JSONObject.NULL) return ""
    val raw = value.toString().trim()
    if (raw.isBlank() || raw.equals("null", ignoreCase = true)) return ""
    if (!raw.startsWith("{") && !raw.startsWith("[")) return raw

    val jsonObject = runCatching { JSONObject(raw) }.getOrNull()
    if (jsonObject != null) return extractDisplayText(jsonObject)

    val jsonArray = runCatching { JSONArray(raw) }.getOrNull() ?: return ""
    return buildList {
      for (index in 0 until jsonArray.length()) {
        val child = jsonArray.opt(index)
        val text = when (child) {
          is JSONObject -> extractDisplayText(child)
          else -> cleanDisplayText(child)
        }
        if (text.isReadableNotificationText()) add(text)
      }
    }.distinct().joinToString(", ")
  }

  private fun extractDisplayText(json: JSONObject): String {
    val preferredKeys = listOf("detail", "keterangan", "deskripsi", "description", "body", "content", "text", "label", "title", "judul", "value")
    for (key in preferredKeys) {
      val text = cleanDisplayText(json.opt(key))
      if (text.isReadableNotificationText()) return text
    }
    val markerText = cleanDisplayText(json.opt("text")).lowercase()
    if (markerText.endsWith("_id") || markerText.endsWith("_ids")) return ""

    return buildList {
      val keys = json.keys()
      while (keys.hasNext()) {
        val text = cleanDisplayText(json.opt(keys.next()))
        if (text.isReadableNotificationText()) add(text)
      }
    }.distinct().joinToString(", ")
  }

  private fun String.isReadableNotificationText(): Boolean {
    val text = trim()
    if (text.isBlank() || text.equals("null", ignoreCase = true)) return false
    if (text.startsWith("{") || text.startsWith("[")) return false
    val uuidLike = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)
    if (uuidLike.matches(text)) return false
    val lower = text.lowercase()
    val technicalTokens = listOf("kelas_id", "kelas_ids", "mapel_id", "mapel_ids", "semester_id", "tahun_ajaran_id", "created_at", "updated_at")
    return technicalTokens.none { lower == it || lower.contains("\"$it\"") }
  }

  private fun parseDate(raw: String?): LocalDate? {
    val text = raw.orEmpty().trim().take(10)
    return runCatching { LocalDate.parse(text) }.getOrNull()
  }

  private fun parseTimestampMillis(raw: String?): Long {
    return runCatching { Instant.parse(raw.orEmpty().trim()).toEpochMilli() }.getOrDefault(0L)
  }

  private fun inferActivityKind(
    jenis: String?,
    title: String?,
    desc: String?
  ): String {
    val normalizedJenis = jenis.orEmpty().trim().lowercase()
    return when {
      normalizedJenis == "libur_semua_kegiatan" -> "all_holiday"
      normalizedJenis == "libur_akademik" -> "academic_holiday"
      normalizedJenis == "libur_ketahfizan" -> "tahfizh_holiday"
      else -> {
        val text = buildString {
          append(title.orEmpty())
          append(' ')
          append(desc.orEmpty())
        }.lowercase()
        when {
          "libur semua" in text -> "all_holiday"
          "libur akademik" in text -> "academic_holiday"
          "libur ketahfiz" in text -> "tahfizh_holiday"
          else -> "calendar"
        }
      }
    }
  }

  private fun formatDateRangeLabel(start: LocalDate, end: LocalDate): String {
    return if (start == end) {
      formatNotifDateLabel(start)
    } else {
      "${formatNotifDateLabel(start)} - ${formatNotifDateLabel(end)}"
    }
  }

  private fun formatNotifDateLabel(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", localeId()))
  }

  private fun relativeDayLabel(date: LocalDate, reference: LocalDate): String {
    return when {
      date == reference -> "Hari ini"
      date == reference.plusDays(1) -> "Besok"
      else -> date.format(DateTimeFormatter.ofPattern("dd MMM", localeId()))
    }
  }

  private fun normalizeHari(dayOfWeek: DayOfWeek): String {
    return when (dayOfWeek) {
      DayOfWeek.MONDAY -> "senin"
      DayOfWeek.TUESDAY -> "selasa"
      DayOfWeek.WEDNESDAY -> "rabu"
      DayOfWeek.THURSDAY -> "kamis"
      DayOfWeek.FRIDAY -> "jumat"
      DayOfWeek.SATURDAY -> "sabtu"
      DayOfWeek.SUNDAY -> "minggu"
    }
  }

  private fun normalizeHari(value: String): String {
    return value.trim().lowercase()
      .replace("'", "")
      .replace("’", "")
      .replace(" ", "")
      .replace("jum'at", "jumat")
  }

  private fun toTimeLabel(value: String?): String {
    val text = value.orEmpty().trim()
    return if (text.length >= 5) text.take(5) else text.ifBlank { "--:--" }
  }

  private fun jamTimeKey(start: String?, end: String?): String {
    val startLabel = toTimeLabel(start).takeIf { it != "--:--" }.orEmpty()
    val endLabel = toTimeLabel(end).takeIf { it != "--:--" }.orEmpty()
    return if (startLabel.isBlank() || endLabel.isBlank()) "" else "$startLabel|$endLabel"
  }

  private fun startOfDayMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
  }

  private fun localeId(): Locale = Locale.forLanguageTag("id-ID")

  private data class DistribusiRow(
    val id: String,
    val kelasId: String,
    val mapelId: String
  )

  private data class JadwalRow(
    val id: String,
    val distribusiId: String,
    val hari: String,
    val jamMulai: String,
    val jamSelesai: String,
    val jamPelajaranId: String
  )
}
