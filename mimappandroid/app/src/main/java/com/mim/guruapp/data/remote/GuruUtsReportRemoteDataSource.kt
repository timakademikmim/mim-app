package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.UtsAttendanceSummary
import com.mim.guruapp.data.model.UtsClassSubject
import com.mim.guruapp.data.model.UtsReportOverride
import com.mim.guruapp.data.model.UtsReportSnapshot
import com.mim.guruapp.data.model.UtsScoreRow
import com.mim.guruapp.data.model.UtsSemesterInfo
import com.mim.guruapp.data.model.UtsSubjectOverride
import com.mim.guruapp.data.model.WaliSantriSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface GuruUtsReportSaveResult {
  data class Success(
    val override: UtsReportOverride?,
    val deletedStudentId: String = "",
    val semesterId: String = "",
    val message: String
  ) : GuruUtsReportSaveResult

  data class Error(val message: String) : GuruUtsReportSaveResult
}

class GuruUtsReportRemoteDataSource {
  suspend fun fetchUtsReportSnapshot(
    teacherRowId: String,
    teacherKaryawanId: String,
    waliSantriSnapshot: WaliSantriSnapshot
  ): UtsReportSnapshot? = withContext(Dispatchers.IO) {
    val guruId = teacherRowId.trim().ifBlank { resolveTeacherRowId(teacherKaryawanId) }
    val students = waliSantriSnapshot.students
    if (guruId.isBlank() || students.isEmpty()) {
      return@withContext UtsReportSnapshot(guruId = guruId, updatedAt = System.currentTimeMillis())
    }

    runCatching {
      val activeTahunAjaranId = resolveActiveTahunAjaranId()
      val semesters = fetchSemesters(activeTahunAjaranId)
      val semesterIds = semesters.map { it.id }.filter { it.isNotBlank() }.distinct()
      val classIds = waliSantriSnapshot.classes.map { it.id.trim() }.filter { it.isNotBlank() }.distinct()
      val studentIds = students.map { it.id.trim() }.filter { it.isNotBlank() }.distinct()
      if (semesterIds.isEmpty() || classIds.isEmpty() || studentIds.isEmpty()) {
        return@runCatching UtsReportSnapshot(
          guruId = guruId,
          semesters = semesters,
          updatedAt = System.currentTimeMillis()
        )
      }

      val scoreRows = fetchScoreRows(studentIds, semesterIds)
      val classSubjects = fetchClassSubjects(classIds, semesterIds)
      val overrideResult = fetchOverrides(guruId, semesterIds, studentIds)
      val scheduleRows = fetchExamScheduleRows(classIds, semesterIds)
      val attendanceRows = fetchAttendanceRows(studentIds, classIds, semesterIds)
      val holidayDates = fetchAcademicHolidayDates()
      val attendanceSummaries = buildAttendanceSummaries(
        students = students.map { StudentRef(it.id, it.classId) },
        semesters = semesters,
        scheduleRows = scheduleRows,
        attendanceRows = attendanceRows,
        holidayDates = holidayDates
      )

      UtsReportSnapshot(
        guruId = guruId,
        semesters = semesters,
        classSubjects = classSubjects,
        scoreRows = scoreRows,
        attendanceSummaries = attendanceSummaries,
        overrides = overrideResult.overrides,
        updatedAt = System.currentTimeMillis(),
        missingOverrideTable = overrideResult.missingTable
      )
    }.getOrNull()
  }

  suspend fun saveOverride(
    override: UtsReportOverride
  ): GuruUtsReportSaveResult = withContext(Dispatchers.IO) {
    val normalized = override.copy(
      guruId = override.guruId.trim(),
      semesterId = override.semesterId.trim(),
      classId = override.classId.trim(),
      studentId = override.studentId.trim(),
      midTahfizCapaian = override.midTahfizCapaian.trim(),
      midTahfizScore = override.midTahfizScore.trim(),
      halaqahSakitText = override.halaqahSakitText.trim(),
      halaqahIzinText = override.halaqahIzinText.trim(),
      subjects = override.subjects.mapNotNull { subject ->
        val name = subject.name.trim()
        val key = subject.key.trim().ifBlank { normalizeSubjectKey(name) }
        val scoreText = subject.scoreText.trim()
        if (key.isBlank() && name.isBlank() && scoreText.isBlank()) {
          null
        } else {
          subject.copy(
            key = key,
            name = name,
            scoreText = scoreText,
            scoreValue = scoreText.toDoubleOrNull()
          )
        }
      }
    )
    if (normalized.guruId.isBlank() || normalized.semesterId.isBlank() || normalized.studentId.isBlank()) {
      return@withContext GuruUtsReportSaveResult.Error("Data laporan PTS belum lengkap.")
    }

    runCatching {
      if (!normalized.hasContent()) {
        deleteOverride(normalized.guruId, normalized.semesterId, normalized.studentId)
        return@runCatching GuruUtsReportSaveResult.Success(
          override = null,
          deletedStudentId = normalized.studentId,
          semesterId = normalized.semesterId,
          message = "Laporan PTS dikembalikan ke data sistem."
        )
      }

      val requestUrl = buildString {
        append(BuildConfig.SUPABASE_URL)
        append("/rest/v1/")
        append(UTS_REPORT_OVERRIDE_TABLE)
        append("?on_conflict=guru_id,semester_id,santri_id&select=id,guru_id,semester_id,kelas_id,santri_id,override_json,updated_at")
      }
      val payload = JSONArray().put(JSONObject().apply {
        put("guru_id", normalized.guruId)
        put("semester_id", normalized.semesterId)
        putNullableString("kelas_id", normalized.classId)
        put("santri_id", normalized.studentId)
        put("override_json", normalized.toOverrideJson())
      })
      val connection = createConnection(requestUrl, method = "POST").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
      }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      val saved = connection.useJsonArrayResponse { rows ->
        parseOverride(rows.optJSONObject(0) ?: JSONObject())
      } ?: normalized
      GuruUtsReportSaveResult.Success(saved, message = "Laporan PTS berhasil disimpan.")
    }.getOrElse { error ->
      val message = when {
        error is SocketTimeoutException -> "Koneksi ke server terlalu lama. Coba lagi."
        isMissingTableError(error) -> "Tabel laporan_uts_input_massal belum tersedia atau belum bisa diakses."
        else -> "Gagal menyimpan laporan PTS."
      }
      GuruUtsReportSaveResult.Error(message)
    }
  }

  private fun fetchSemesters(activeTahunAjaranId: String): List<UtsSemesterInfo> {
    val rows = fetchSemesterRows(activeTahunAjaranId)
    return rows.mapNotNull { row ->
      val id = row.cleanString("id")
      if (id.isBlank()) return@mapNotNull null
      val rawLabel = pickFirst(row, "nama", "nama_semester", "semester", "label")
      val label = formatSemesterLabel(rawLabel, row)
      val tahunAjaranId = pickFirst(row, "tahun_ajaran_id", "tahunAjaranId")
      val rowActive = row.optBooleanFlexible("aktif") || row.optBooleanFlexible("is_active") || row.optBooleanFlexible("active")
      UtsSemesterInfo(
        id = id,
        label = label,
        tahunAjaranId = tahunAjaranId,
        startDateIso = pickFirst(row, "tanggal_mulai", "mulai", "start_date").take(10),
        endDateIso = pickFirst(row, "tanggal_selesai", "selesai", "end_date").take(10),
        isActive = rowActive && (activeTahunAjaranId.isBlank() || tahunAjaranId == activeTahunAjaranId)
      )
    }.sortedWith(compareByDescending<UtsSemesterInfo> { it.isActive }.thenByDescending { it.startDateIso })
  }

  private fun fetchSemesterRows(activeTahunAjaranId: String): List<JSONObject> {
    val scopedRows = if (activeTahunAjaranId.isBlank()) {
      emptyList()
    } else {
      runCatching {
        fetchRows(
          "semester",
          "select=*&order=tanggal_mulai.desc&tahun_ajaran_id=eq.${encodeValue(activeTahunAjaranId)}"
        )
      }.getOrDefault(emptyList())
    }
    if (scopedRows.isNotEmpty()) return scopedRows
    return runCatching {
      fetchRows("semester", "select=*&order=tanggal_mulai.desc")
    }.getOrElse {
      fetchRows("semester", "select=*")
    }
  }

  private fun resolveActiveTahunAjaranId(): String {
    val rows = runCatching {
      fetchRows("tahun_ajaran", "select=*&aktif=eq.true")
    }.getOrElse {
      fetchRows("tahun_ajaran", "select=id,nama,aktif&aktif=eq.true")
    }
    if (rows.isEmpty()) return ""
    val today = LocalDate.now()
    return rows
      .maxWithOrNull(
        compareBy<JSONObject> { row -> activeAcademicYearPriority(row, today) }
          .thenBy { row -> academicYearStartYear(row) }
          .thenBy { row -> row.cleanString("id") }
      )
      ?.cleanString("id")
      .orEmpty()
  }

  private fun fetchScoreRows(
    studentIds: List<String>,
    semesterIds: List<String>
  ): List<UtsScoreRow> {
    return fetchRows(
      table = "nilai_akademik",
      query = buildString {
        append("select=santri_id,mapel_id,semester_id,nilai_pts")
        appendInFilter("santri_id", studentIds)
        appendInFilter("semester_id", semesterIds)
      }
    ).mapNotNull { row ->
      val studentId = row.cleanString("santri_id")
      val semesterId = row.cleanString("semester_id")
      val mapelId = row.cleanString("mapel_id")
      if (studentId.isBlank() || semesterId.isBlank() || mapelId.isBlank()) return@mapNotNull null
      val value = row.optDoubleOrNull("nilai_pts")
      UtsScoreRow(
        studentId = studentId,
        semesterId = semesterId,
        mapelId = mapelId,
        scoreText = value?.let(::formatScore).orEmpty(),
        scoreValue = value
      )
    }
  }

  private fun fetchClassSubjects(
    classIds: List<String>,
    semesterIds: List<String>
  ): List<UtsClassSubject> {
    val distribusiRows = fetchRows(
      table = "distribusi_mapel",
      query = buildString {
        append("select=kelas_id,mapel_id,semester_id")
        appendInFilter("kelas_id", classIds)
        appendInFilter("semester_id", semesterIds)
      }
    )
    val mapelIds = distribusiRows.map { it.cleanString("mapel_id") }.filter { it.isNotBlank() }.distinct()
    val mapelById = fetchMapelRows(mapelIds).associateBy { it.cleanString("id") }
    return distribusiRows.mapNotNull { row ->
      val classId = row.cleanString("kelas_id")
      val semesterId = row.cleanString("semester_id")
      val mapelId = row.cleanString("mapel_id")
      if (classId.isBlank() || semesterId.isBlank() || mapelId.isBlank()) return@mapNotNull null
      UtsClassSubject(
        classId = classId,
        semesterId = semesterId,
        mapelId = mapelId,
        name = mapelById[mapelId]?.subjectName().orEmpty().ifBlank { "-" }
      )
    }.distinctBy { "${it.classId}|${it.semesterId}|${it.mapelId}" }
  }

  private fun fetchMapelRows(mapelIds: List<String>): List<JSONObject> {
    if (mapelIds.isEmpty()) return emptyList()
    return fetchRows(
      table = "mapel",
      query = buildString {
        append("select=*")
        appendInFilter("id", mapelIds)
      }
    )
  }

  private fun fetchOverrides(
    guruId: String,
    semesterIds: List<String>,
    studentIds: List<String>
  ): OverrideFetchResult {
    return runCatching {
      val rows = fetchRows(
        table = UTS_REPORT_OVERRIDE_TABLE,
        query = buildString {
          append("select=id,guru_id,semester_id,kelas_id,santri_id,override_json,updated_at")
          append("&guru_id=eq.")
          append(encodeValue(guruId))
          appendInFilter("semester_id", semesterIds)
          appendInFilter("santri_id", studentIds)
        }
      )
      OverrideFetchResult(rows.mapNotNull(::parseOverride))
    }.getOrElse { error ->
      if (isMissingTableError(error)) OverrideFetchResult(emptyList(), missingTable = true) else throw error
    }
  }

  private fun fetchExamScheduleRows(
    classIds: List<String>,
    semesterIds: List<String>
  ): List<JSONObject> {
    return runCatching {
      fetchRows(
        table = "jadwal_ujian",
        query = buildString {
          append("select=*")
          appendInFilter("kelas_id", classIds)
          appendInFilter("semester_id", semesterIds)
          append("&order=tanggal.asc")
        }
      )
    }.getOrDefault(emptyList())
  }

  private fun fetchAttendanceRows(
    studentIds: List<String>,
    classIds: List<String>,
    semesterIds: List<String>
  ): List<JSONObject> {
    return runCatching {
      fetchRows(
        table = "absensi_santri",
        query = buildString {
          append("select=santri_id,kelas_id,semester_id,tanggal,status")
          appendInFilter("santri_id", studentIds)
          appendInFilter("kelas_id", classIds)
          appendInFilter("semester_id", semesterIds)
        }
      )
    }.getOrElse { error ->
      if (!isMissingColumnError(error)) return@getOrElse emptyList()
      fetchRows(
        table = "absensi_santri",
        query = buildString {
          append("select=santri_id,kelas_id,tanggal,status")
          appendInFilter("santri_id", studentIds)
          appendInFilter("kelas_id", classIds)
        }
      )
    }
  }

  private fun fetchAcademicHolidayDates(): Set<String> {
    return runCatching {
      val rows = fetchRows(
        table = "kalender_akademik",
        query = "select=mulai,selesai,judul,detail,jenis_kegiatan"
      )
      rows.flatMap { row ->
        val text = "${row.cleanString("jenis_kegiatan")} ${row.cleanString("judul")} ${row.cleanString("detail")}".lowercase()
        val isHoliday = "libur akademik" in text ||
          "libur semua" in text ||
          row.cleanString("jenis_kegiatan").equals("libur_akademik", ignoreCase = true) ||
          row.cleanString("jenis_kegiatan").equals("libur_semua_kegiatan", ignoreCase = true)
        if (!isHoliday) {
          emptyList()
        } else {
          expandDateRange(row.cleanString("mulai").take(10), row.cleanString("selesai").take(10))
        }
      }.toSet()
    }.getOrDefault(emptySet())
  }

  private fun buildAttendanceSummaries(
    students: List<StudentRef>,
    semesters: List<UtsSemesterInfo>,
    scheduleRows: List<JSONObject>,
    attendanceRows: List<JSONObject>,
    holidayDates: Set<String>
  ): List<UtsAttendanceSummary> {
    if (students.isEmpty() || semesters.isEmpty()) return emptyList()
    val rowsByStudent = attendanceRows.groupBy { it.cleanString("santri_id") }
    return students.flatMap { student ->
      semesters.map { semester ->
        val ptsDate = findPtsDate(scheduleRows, student.classId, semester.id)
        val start = parseIsoDate(semester.startDateIso)
        val end = parseIsoDate(ptsDate)?.minusDays(1) ?: LocalDate.now().minusDays(1)
        val rangeRows = rowsByStudent[student.id].orEmpty().filter { row ->
          val date = parseIsoDate(row.cleanString("tanggal").take(10)) ?: return@filter false
          val semesterMatches = row.cleanString("semester_id").isBlank() || row.cleanString("semester_id") == semester.id
          val classMatches = row.cleanString("kelas_id").isBlank() || row.cleanString("kelas_id") == student.classId
          semesterMatches && classMatches && (start == null || !date.isBefore(start)) && !date.isAfter(end) && date.toString() !in holidayDates
        }
        val dailyStatuses = rangeRows
          .groupBy { it.cleanString("tanggal").take(10) }
          .mapValues { (_, rows) -> chooseDailyAttendanceStatus(rows.map { it.cleanString("status") }) }
        UtsAttendanceSummary(
          studentId = student.id,
          semesterId = semester.id,
          kelasIzinCount = dailyStatuses.values.count { it == "izin" },
          kelasSakitCount = dailyStatuses.values.count { it == "sakit" },
          ptsDateIso = ptsDate,
          rangeLabel = if (start != null) {
            "${formatDate(start)} s.d. ${formatDate(end)}"
          } else {
            "-"
          }
        )
      }
    }
  }

  private fun findPtsDate(
    scheduleRows: List<JSONObject>,
    classId: String,
    semesterId: String
  ): String {
    return scheduleRows
      .filter { row ->
        row.cleanString("kelas_id") == classId &&
          row.cleanString("semester_id") == semesterId &&
          row.cleanString("tanggal").take(10).isNotBlank()
      }
      .firstOrNull { row ->
        val text = listOf("jenis", "judul", "nama", "nama_ujian", "keterangan")
          .joinToString(" ") { row.cleanString(it) }
          .lowercase()
        listOf("uts", "usts", "pts", "tengah").any { it in text }
      }
      ?.cleanString("tanggal")
      ?.take(10)
      .orEmpty()
  }

  private fun deleteOverride(guruId: String, semesterId: String, studentId: String) {
    val url = buildString {
      append(BuildConfig.SUPABASE_URL)
      append("/rest/v1/")
      append(UTS_REPORT_OVERRIDE_TABLE)
      append("?guru_id=eq.")
      append(encodeValue(guruId))
      append("&semester_id=eq.")
      append(encodeValue(semesterId))
      append("&santri_id=eq.")
      append(encodeValue(studentId))
    }
    createConnection(url, "DELETE").useJsonArrayResponse { }
  }

  private fun fetchRows(
    table: String,
    query: String
  ): List<JSONObject> {
    val separator = if (query.startsWith("?")) "" else "?"
    val url = "${BuildConfig.SUPABASE_URL}/rest/v1/$table$separator$query"
    val connection = createConnection(url, method = "GET")
    return connection.useJsonArrayResponse { rows ->
      List(rows.length()) { index -> rows.optJSONObject(index) }.filterNotNull()
    }
  }

  private fun createConnection(
    url: String,
    method: String
  ): HttpURLConnection {
    return (URL(url).openConnection() as HttpURLConnection).apply {
      requestMethod = method
      connectTimeout = 15_000
      readTimeout = 20_000
      setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
      setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
      setRequestProperty("Accept", "application/json")
    }
  }

  private fun <T> HttpURLConnection.useJsonArrayResponse(block: (JSONArray) -> T): T {
    try {
      val code = responseCode
      val payload = readPayload(code in 200..299)
      if (code !in 200..299) {
        throw IllegalStateException(payload.ifBlank { "HTTP $code" })
      }
      val safePayload = payload.trim().ifBlank { "[]" }
      return block(JSONArray(safePayload))
    } finally {
      disconnect()
    }
  }

  private fun HttpURLConnection.readPayload(useInputStream: Boolean): String {
    val stream = if (useInputStream) inputStream else errorStream
    if (stream == null) return ""
    return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
      reader.lineSequence().joinToString(separator = "")
    }
  }

  private fun resolveTeacherRowId(teacherKaryawanId: String): String {
    val employeeId = teacherKaryawanId.trim()
    if (employeeId.isBlank()) return ""
    return runCatching {
      fetchRows(
        table = "karyawan",
        query = "select=id&id_karyawan=eq.${encodeValue(employeeId)}&limit=1"
      ).firstOrNull()?.cleanString("id").orEmpty()
    }.getOrDefault("")
  }

  private fun parseOverride(row: JSONObject): UtsReportOverride? {
    val studentId = row.cleanString("santri_id")
    val semesterId = row.cleanString("semester_id")
    if (studentId.isBlank() || semesterId.isBlank()) return null
    val overrideJson = row.optJsonObjectFlexible("override_json")
    val subjectsJson = overrideJson.optJSONArray("subjects")
      ?: overrideJson.optJSONArray("subject_overrides")
      ?: JSONArray()
    val subjects = buildList {
      for (index in 0 until subjectsJson.length()) {
        val item = subjectsJson.optJSONObject(index) ?: continue
        val name = item.cleanString("name")
        val key = item.cleanString("key").ifBlank { normalizeSubjectKey(name) }
        val scoreText = pickFirst(item, "scoreText", "nilai", "score", "value")
        add(
          UtsSubjectOverride(
            key = key,
            name = name,
            scoreText = scoreText,
            scoreValue = scoreText.toDoubleOrNull()
          )
        )
      }
    }
    return UtsReportOverride(
      id = row.cleanString("id"),
      guruId = row.cleanString("guru_id"),
      semesterId = semesterId,
      classId = row.cleanString("kelas_id"),
      studentId = studentId,
      midTahfizCapaian = pickFirst(overrideJson, "midTahfizCapaian", "capaian_hafalan", "capaianHafalan"),
      midTahfizScore = pickFirst(overrideJson, "midTahfizScore", "nilai_tahfiz", "nilaiTahfiz"),
      halaqahSakitText = pickFirst(overrideJson, "halaqahSakitText", "sakit_halaqah", "sakitHalaqah"),
      halaqahIzinText = pickFirst(overrideJson, "halaqahIzinText", "izin_halaqah", "izinHalaqah"),
      subjects = subjects,
      updatedAt = row.cleanString("updated_at")
    )
  }

  private data class OverrideFetchResult(
    val overrides: List<UtsReportOverride>,
    val missingTable: Boolean = false
  )

  private data class StudentRef(
    val id: String,
    val classId: String
  )

  companion object {
    private const val UTS_REPORT_OVERRIDE_TABLE = "laporan_uts_input_massal"
  }
}

private fun UtsReportOverride.hasContent(): Boolean {
  return midTahfizCapaian.isNotBlank() ||
    midTahfizScore.isNotBlank() ||
    halaqahSakitText.isNotBlank() ||
    halaqahIzinText.isNotBlank() ||
    subjects.any { it.scoreText.isNotBlank() }
}

private fun UtsReportOverride.toOverrideJson(): JSONObject {
  return JSONObject().apply {
    putNullableString("midTahfizCapaian", midTahfizCapaian)
    putNullableString("midTahfizScore", midTahfizScore)
    putNullableString("halaqahSakitText", halaqahSakitText)
    putNullableString("halaqahIzinText", halaqahIzinText)
    put("subjects", JSONArray().apply {
      subjects.filter { it.scoreText.isNotBlank() }.forEach { subject ->
        put(JSONObject().apply {
          putNullableString("key", subject.key.ifBlank { normalizeSubjectKey(subject.name) })
          putNullableString("name", subject.name)
          putNullableString("scoreText", subject.scoreText)
          putNullableNumber("scoreValue", subject.scoreValue ?: subject.scoreText.toDoubleOrNull())
        })
      }
    })
  }
}

private fun StringBuilder.appendInFilter(
  column: String,
  values: List<String>
) {
  val normalized = values.map { it.trim() }.filter { it.isNotBlank() }.distinct()
  if (normalized.isEmpty()) return
  append("&")
  append(column)
  append("=in.(")
  append(normalized.joinToString(",") { "%22${encodeValue(it)}%22" })
  append(")")
}

private fun JSONObject.optJsonObjectFlexible(key: String): JSONObject {
  val raw = opt(key)
  return when (raw) {
    is JSONObject -> raw
    is String -> runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    else -> JSONObject()
  }
}

private fun JSONObject.subjectName(): String {
  return pickFirst(this, "nama", "nama_mapel", "nama_pelajaran", "mapel", "judul")
    .replace(Regex("\\s+"), " ")
    .trim()
}

private fun JSONObject.cleanString(key: String): String {
  val value = opt(key)
  if (value == null || value == JSONObject.NULL) return ""
  val raw = value.toString().trim()
  if (raw.equals("null", ignoreCase = true)) return ""
  return raw
}

private fun JSONObject.optDoubleOrNull(key: String): Double? {
  val raw = cleanString(key).replace(",", ".")
  return raw.toDoubleOrNull()
}

private fun JSONObject.optBooleanFlexible(key: String): Boolean {
  val raw = cleanString(key).lowercase()
  return raw == "true" || raw == "1" || raw == "ya" || raw == "aktif"
}

private fun JSONObject.putNullableString(key: String, value: String?) {
  val normalized = value.orEmpty().trim()
  if (normalized.isBlank()) put(key, JSONObject.NULL) else put(key, normalized)
}

private fun JSONObject.putNullableNumber(key: String, value: Number?) {
  if (value == null) put(key, JSONObject.NULL) else put(key, value)
}

private fun pickFirst(
  row: JSONObject,
  vararg keys: String
): String {
  return keys.firstNotNullOfOrNull { key -> row.cleanString(key).takeIf { it.isNotBlank() } }.orEmpty()
}

private fun encodeValue(value: String): String {
  return URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
}

private fun isMissingTableError(error: Throwable): Boolean {
  val text = error.message.orEmpty().lowercase()
  return "does not exist" in text || "could not find the table" in text || "42p01" in text
}

private fun isMissingColumnError(error: Throwable): Boolean {
  val text = error.message.orEmpty().lowercase()
  return "column" in text && ("does not exist" in text || "pgrst204" in text || "42703" in text)
}

private fun normalizeSubjectKey(value: String): String {
  return value
    .lowercase(Locale.ROOT)
    .replace("bahasa", "bhs")
    .replace("fiqih", "fikih")
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()
    .replace(Regex("\\s+"), " ")
}

private fun formatScore(value: Double): String {
  val rounded = kotlin.math.round(value * 100.0) / 100.0
  return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "%.2f".format(Locale.US, rounded).trimEnd('0').trimEnd('.')
}

private fun formatSemesterLabel(
  rawLabel: String,
  row: JSONObject
): String {
  val label = rawLabel.trim()
  if (label.isNotBlank()) {
    return if (label.contains("semester", ignoreCase = true)) label else "Semester $label"
  }
  val ordinal = row.cleanString("urutan").ifBlank { row.cleanString("nomor") }
  return ordinal.takeIf { it.isNotBlank() }?.let { "Semester $it" } ?: "Semester"
}

private fun activeAcademicYearPriority(
  row: JSONObject,
  today: LocalDate
): Int {
  val start = parseIsoDate(pickFirst(row, "tanggal_mulai", "mulai", "start_date", "start").take(10))
  val end = parseIsoDate(pickFirst(row, "tanggal_selesai", "selesai", "end_date", "end").take(10))
  if (start != null && end != null && !today.isBefore(start) && !today.isAfter(end)) return 4

  val currentAcademicStart = if (today.monthValue >= 7) today.year else today.year - 1
  val currentAcademicEnd = currentAcademicStart + 1
  val years = academicYearNumbers(row)
  return when {
    currentAcademicStart in years && currentAcademicEnd in years -> 3
    today.year in years -> 2
    start != null && !today.isBefore(start) -> 1
    else -> 0
  }
}

private fun academicYearStartYear(row: JSONObject): Int {
  return parseIsoDate(pickFirst(row, "tanggal_mulai", "mulai", "start_date", "start").take(10))?.year
    ?: academicYearNumbers(row).minOrNull()
    ?: 0
}

private fun academicYearNumbers(row: JSONObject): List<Int> {
  val text = listOf("nama", "nama_tahun_ajaran", "tahun_ajaran", "label", "tahun")
    .joinToString(" ") { row.cleanString(it) }
  return Regex("""20\d{2}""")
    .findAll(text)
    .mapNotNull { it.value.toIntOrNull() }
    .toList()
}

private fun parseIsoDate(value: String): LocalDate? {
  return runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
}

private fun formatDate(value: LocalDate): String {
  return value.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
}

private fun chooseDailyAttendanceStatus(statuses: List<String>): String {
  val normalized = statuses.joinToString(" ").lowercase()
  return when {
    "sakit" in normalized -> "sakit"
    "izin" in normalized -> "izin"
    else -> ""
  }
}

private fun expandDateRange(
  startIso: String,
  endIso: String
): List<String> {
  val start = parseIsoDate(startIso) ?: return emptyList()
  val end = parseIsoDate(endIso).takeIf { it != null && !it.isBefore(start) } ?: start
  val dates = mutableListOf<String>()
  var current = start
  while (!current.isAfter(end)) {
    dates += current.toString()
    current = current.plusDays(1)
  }
  return dates
}
