package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.LeaveRequestItem
import com.mim.guruapp.data.model.SubjectOverview
import com.mim.guruapp.data.model.WakasekKurikulumSnapshot
import com.mim.guruapp.data.model.WakasekStudentMonitoringRow
import com.mim.guruapp.data.model.WakasekTeacherMonitoringRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

sealed interface GuruWakasekKurikulumReviewResult {
  data class Success(val changedItem: LeaveRequestItem, val message: String) : GuruWakasekKurikulumReviewResult
  data class Error(val message: String) : GuruWakasekKurikulumReviewResult
}

class GuruWakasekKurikulumRemoteDataSource {
  suspend fun fetchSnapshot(
    teacherRowId: String,
    teacherKaryawanId: String,
    teacherName: String,
    roles: List<String> = emptyList(),
    referenceDate: LocalDate = LocalDate.now()
  ): WakasekKurikulumSnapshot? = withContext(Dispatchers.IO) {
    runCatching {
      val isAllowed = resolveWakasekAccess(
        teacherRowId = teacherRowId,
        teacherKaryawanId = teacherKaryawanId,
        teacherName = teacherName,
        roles = roles
      )
      if (!isAllowed) {
        return@runCatching WakasekKurikulumSnapshot(
          isWakasekKurikulum = false,
          updatedAt = System.currentTimeMillis()
        )
      }

      val startDate = referenceDate.minusMonths(6).withDayOfMonth(1)
      val endDate = referenceDate.plusDays(7)
      val attendanceRows = fetchAttendanceRows(startDate, endDate)
      val leaveRows = fetchRows(
        table = "izin_karyawan",
        query = buildString {
          append("select=id,guru_id,tanggal_mulai,tanggal_selesai,durasi_hari,keperluan,status,catatan_wakasek,reviewed_by,reviewed_at,created_at,updated_at")
          append("&order=created_at.desc")
        }
      )
      val distribusiRows = fetchRows(
        table = "distribusi_mapel",
        query = "select=id,kelas_id,mapel_id,guru_id,semester_id"
      )
      val jadwalRows = fetchJadwalRows()
      val jamRows = fetchJamRows()
      val teacherIds = (attendanceRows.flatMap { row ->
        listOf(row.cleanString("guru_id"), row.cleanString("guru_pengganti_id"))
      } + leaveRows.map { it.cleanString("guru_id") } + distribusiRows.map { it.cleanString("guru_id") })
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
      val studentIds = attendanceRows.map { it.cleanString("santri_id") }.filter(String::isNotBlank).distinct()
      val classIds = (attendanceRows.map { it.cleanString("kelas_id") } + distribusiRows.map { it.cleanString("kelas_id") })
        .filter(String::isNotBlank)
        .distinct()
      val subjectIds = (attendanceRows.map { it.cleanString("mapel_id") } + distribusiRows.map { it.cleanString("mapel_id") })
        .filter(String::isNotBlank)
        .distinct()
      val semesterIds = distribusiRows.map { it.cleanString("semester_id") }
        .filter(String::isNotBlank)
        .distinct()

      val teacherMap = fetchNameMap("karyawan", "nama", teacherIds)
      val studentMap = fetchNameMap("santri", "nama", studentIds)
      val classMap = fetchNameMap("kelas", "nama_kelas", classIds)
      val subjectMap = fetchNameMap("mapel", "nama", subjectIds)
      val semesterMap = fetchSemesterMap(semesterIds)
      val activeSemesterId = resolveActiveSemesterId()
      val calendarRows = fetchCalendarRows()

      WakasekKurikulumSnapshot(
        isWakasekKurikulum = true,
        teacherRows = buildTeacherRows(
          attendanceRows = attendanceRows,
          leaveRows = leaveRows,
          distribusiRows = distribusiRows,
          jadwalRows = jadwalRows,
          jamRows = jamRows,
          calendarRows = calendarRows,
          teacherMap = teacherMap,
          classMap = classMap,
          subjectMap = subjectMap,
          startDate = startDate,
          endDate = referenceDate
        ),
        studentRows = buildStudentRows(attendanceRows, studentMap, classMap, subjectMap),
        scoreSubjects = buildWakasekScoreSubjects(
          distribusiRows = distribusiRows,
          classMap = classMap,
          subjectMap = subjectMap,
          semesterMap = semesterMap,
          activeSemesterId = activeSemesterId
        ),
        leaveRequests = leaveRows.mapNotNull { parseLeaveRequestItem(it, teacherMap) },
        updatedAt = System.currentTimeMillis()
      )
    }.getOrNull()
  }

  suspend fun reviewLeaveRequest(
    teacherRowId: String,
    teacherKaryawanId: String,
    teacherName: String,
    roles: List<String> = emptyList(),
    requestId: String,
    approved: Boolean,
    note: String
  ): GuruWakasekKurikulumReviewResult = withContext(Dispatchers.IO) {
    val reviewerId = teacherRowId.trim().ifBlank { teacherKaryawanId.trim() }
    val normalizedRequestId = requestId.trim()
    if (reviewerId.isBlank()) {
      return@withContext GuruWakasekKurikulumReviewResult.Error("ID wakasek belum tersedia.")
    }
    if (normalizedRequestId.isBlank()) {
      return@withContext GuruWakasekKurikulumReviewResult.Error("Data izin yang dipilih belum valid.")
    }

    runCatching {
      val requestUrl = buildString {
        append(BuildConfig.SUPABASE_URL)
        append("/rest/v1/izin_karyawan?id=eq.")
        append(encodeValue(normalizedRequestId))
        append("&select=id,guru_id,tanggal_mulai,tanggal_selesai,durasi_hari,keperluan,status,catatan_wakasek,reviewed_by,reviewed_at,created_at,updated_at")
      }
      val connection = createConnection(requestUrl, method = "PATCH").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Prefer", "return=representation")
      }
      val payload = JSONObject().apply {
        put("status", if (approved) "diterima" else "ditolak")
        put("catatan_wakasek", note.trim().ifBlank { JSONObject.NULL })
        put("reviewed_by", reviewerId)
        put("reviewed_at", LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME))
      }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      val changedItem = connection.useJsonArrayResponse { response ->
        parseLeaveRequestItem(response.optJSONObject(0) ?: JSONObject(), emptyMap())
      } ?: return@withContext GuruWakasekKurikulumReviewResult.Error("Data izin berhasil diproses, tetapi respons server tidak lengkap.")
      GuruWakasekKurikulumReviewResult.Success(
        changedItem = changedItem,
        message = if (approved) "Izin guru disetujui." else "Izin guru ditolak."
      )
    }.getOrElse { error ->
      GuruWakasekKurikulumReviewResult.Error(resolveErrorMessage(error, "Gagal memperbarui status izin."))
    }
  }

  private fun resolveWakasekAccess(
    teacherRowId: String,
    teacherKaryawanId: String,
    teacherName: String,
    roles: List<String>
  ): Boolean {
    if (roles.any(::isWakasekKurikulumRole)) return true

    val karyawanRoleRows = buildList {
      if (teacherRowId.isNotBlank()) add("id=eq.${encodeValue(teacherRowId)}")
      if (teacherKaryawanId.isNotBlank()) add("id_karyawan=eq.${encodeValue(teacherKaryawanId)}")
    }
      .firstNotNullOfOrNull { filter ->
        runCatching {
          fetchRows(
            table = "karyawan",
            query = "select=role&$filter&limit=1"
          )
        }.getOrDefault(emptyList()).takeIf { it.isNotEmpty() }
      }
      .orEmpty()
    if (karyawanRoleRows.any { row -> isWakasekKurikulumRole(row.cleanString("role")) }) return true

    val selectAttempts = listOf(
      "wakasek_bidang_akademik,wakasek_akademik,updated_at,created_at",
      "wakasek_bidang_akademik,updated_at,created_at",
      "wakasek_akademik,updated_at,created_at"
    )
    val row = selectAttempts.firstNotNullOfOrNull { select ->
      val rows = runCatching {
        fetchRows(
          table = "struktur_sekolah",
          query = "select=$select&order=updated_at.desc&order=created_at.desc&limit=1"
        )
      }.getOrDefault(emptyList())
      rows.firstOrNull()
    } ?: return false

    val raw = row.cleanString("wakasek_bidang_akademik").ifBlank { row.cleanString("wakasek_akademik") }
    val normalizedName = normalizeName(raw)
    val normalizedTeacherName = normalizeName(teacherName)
    val token = raw.trim().lowercase()
    return (normalizedName.isNotBlank() && normalizedName == normalizedTeacherName) ||
      (teacherRowId.isNotBlank() && token == teacherRowId.trim().lowercase()) ||
      (teacherKaryawanId.isNotBlank() && token == teacherKaryawanId.trim().lowercase())
  }

  private fun buildTeacherRows(
    attendanceRows: List<JSONObject>,
    leaveRows: List<JSONObject>,
    distribusiRows: List<JSONObject>,
    jadwalRows: List<JSONObject>,
    jamRows: List<JSONObject>,
    calendarRows: List<JSONObject>,
    teacherMap: Map<String, String>,
    classMap: Map<String, String>,
    subjectMap: Map<String, String>,
    startDate: LocalDate,
    endDate: LocalDate
  ): List<WakasekTeacherMonitoringRow> {
    val grouped = linkedMapOf<String, TeacherAggregate>()
    val calendarHolidayDates = buildCalendarAcademicHolidayDates(calendarRows, startDate, endDate)
    val calendarClassIndex = buildCalendarClassIndex(calendarRows, classMap, startDate, endDate)
    val jamById = jamRows
      .mapNotNull { row ->
        val id = row.cleanString("id")
        if (id.isBlank()) null else id to JamMonitorRow(
          start = row.cleanString("jam_mulai"),
          end = row.cleanString("jam_selesai")
        )
      }
      .toMap()
    val distribusiById = distribusiRows
      .mapNotNull { row ->
        val id = row.cleanString("id")
        if (id.isBlank()) return@mapNotNull null
        id to DistribusiMonitorRow(
          id = id,
          teacherId = row.cleanString("guru_id"),
          classId = row.cleanString("kelas_id"),
          subjectId = row.cleanString("mapel_id")
        )
      }
      .toMap()
    val presentExactKeys = mutableSetOf<String>()
    val presentGenericMarkers = linkedMapOf<String, MutableSet<String>>()
    val substitutedKeys = mutableSetOf<String>()
    val substitutedGenericMarkers = linkedMapOf<String, MutableSet<String>>()
    attendanceRows.forEach { row ->
      val dateIso = row.cleanString("tanggal").take(10)
      val distribusi = distribusiById[row.cleanString("distribusi_id")]
      val teacherId = row.cleanString("guru_id").ifBlank { distribusi?.teacherId.orEmpty() }
      val classId = row.cleanString("kelas_id").ifBlank { distribusi?.classId.orEmpty() }
      val subjectId = row.cleanString("mapel_id").ifBlank { distribusi?.subjectId.orEmpty() }
      val distribusiId = row.cleanString("distribusi_id").ifBlank {
        distribusiRows.firstOrNull { candidate ->
          candidate.cleanString("guru_id") == teacherId &&
            candidate.cleanString("kelas_id") == classId &&
            candidate.cleanString("mapel_id") == subjectId
        }?.cleanString("id").orEmpty()
      }
      val jamId = row.cleanString("jam_pelajaran_id")
      val jam = jamById[jamId]
      val jamKey = buildJamMatchKey(jam?.start.orEmpty(), jam?.end.orEmpty(), jamId)
      if (dateIso.isBlank() || teacherId.isBlank() || classId.isBlank() || subjectId.isBlank()) return@forEach
      if (distribusiId.isNotBlank() || classId.isNotBlank()) {
        val exactKey = teacherExactKey(dateIso, teacherId, classId, subjectId, jamKey)
        val genericKey = teacherGenericKey(dateIso, teacherId, classId, subjectId)
        presentExactKeys += exactKey
        presentGenericMarkers.getOrPut(genericKey) { linkedSetOf() } += jamKey.ifBlank { NoJamMarker }
        if (row.cleanString("guru_pengganti_id").isNotBlank()) {
          substitutedKeys += exactKey
          substitutedGenericMarkers.getOrPut(genericKey) { linkedSetOf() } += jamKey.ifBlank { NoJamMarker }
        }
      }
    }
    val presentGenericRemaining = presentGenericMarkers.mapValues { it.value.size }.toMutableMap()
    val substitutedGenericRemaining = substitutedGenericMarkers.mapValues { it.value.size }.toMutableMap()
    val expectedExactKeys = mutableSetOf<String>()
    val expectedGenericKeys = mutableSetOf<String>()

    val leaveKeys = mutableSetOf<String>()
    leaveRows
      .filter { normalizeStatus(it.cleanString("status")) == "diterima" }
      .forEach { row ->
        val teacherId = row.cleanString("guru_id")
        val start = parseDate(row.cleanString("tanggal_mulai")) ?: return@forEach
        val end = parseDate(row.cleanString("tanggal_selesai")) ?: start
        var cursor = start
        while (!cursor.isAfter(end)) {
          leaveKeys += "${cursor}|$teacherId"
          cursor = cursor.plusDays(1)
        }
      }

    val jadwalByDistribusi = jadwalRows.groupBy { it.cleanString("distribusi_id") }
    var cursor = startDate
    while (!cursor.isAfter(endDate)) {
      if (calendarHolidayDates.contains(cursor.toString())) {
        cursor = cursor.plusDays(1)
        continue
      }
      val dayKey = normalizeDay(cursor.dayOfWeek)
      distribusiById.values.forEach { distribusi ->
        if (distribusi.teacherId.isBlank()) return@forEach
        jadwalByDistribusi[distribusi.id].orEmpty()
          .filter { normalizeDay(it.cleanString("hari")) == dayKey }
          .forEach { jadwal ->
            val dateIso = cursor.toString()
            val jamId = jadwal.cleanString("jam_pelajaran_id")
            val jam = jamById[jamId]
            val jamStart = jadwal.cleanString("jam_mulai").ifBlank { jam?.start.orEmpty() }
            val jamEnd = jadwal.cleanString("jam_selesai").ifBlank { jam?.end.orEmpty() }
            val jamKey = buildJamMatchKey(jamStart, jamEnd, jamId)
            val key = teacherExactKey(dateIso, distribusi.teacherId, distribusi.classId, distribusi.subjectId, jamKey)
            val genericKey = teacherGenericKey(dateIso, distribusi.teacherId, distribusi.classId, distribusi.subjectId)
            expectedExactKeys += key
            expectedGenericKeys += genericKey
            val calendarMatch = getCalendarClassMatch(calendarClassIndex, dateIso, distribusi.classId)
            val aggregate = grouped.getOrPut(key) {
              TeacherAggregate(
                dateIso = dateIso,
                teacherId = distribusi.teacherId,
                classId = distribusi.classId,
                subjectId = distribusi.subjectId,
                timeLabel = buildTimeLabel(jamStart, jamEnd)
              )
            }
            val presentGenericOnlyNoJam = presentGenericMarkers[genericKey]?.let { markers ->
              markers.isNotEmpty() && markers.all { it == NoJamMarker }
            } == true
            val exactPresent = presentExactKeys.contains(key)
            if (exactPresent && !presentGenericOnlyNoJam) {
              val remaining = presentGenericRemaining[genericKey] ?: 0
              if (remaining > 0) presentGenericRemaining[genericKey] = remaining - 1
            }
            val presentFallback = if (presentGenericOnlyNoJam) {
              true
            } else {
              val remaining = presentGenericRemaining[genericKey] ?: 0
              if (remaining > 0 && !exactPresent) {
                presentGenericRemaining[genericKey] = remaining - 1
                true
              } else {
                false
              }
            }
            val substitutedGenericOnlyNoJam = substitutedGenericMarkers[genericKey]?.let { markers ->
              markers.isNotEmpty() && markers.all { it == NoJamMarker }
            } == true
            val exactSubstituted = substitutedKeys.contains(key)
            if (exactSubstituted && !substitutedGenericOnlyNoJam) {
              val remaining = substitutedGenericRemaining[genericKey] ?: 0
              if (remaining > 0) substitutedGenericRemaining[genericKey] = remaining - 1
            }
            val substitutedFallback = if (substitutedGenericOnlyNoJam) {
              true
            } else {
              val remaining = substitutedGenericRemaining[genericKey] ?: 0
              if (remaining > 0 && !exactSubstituted) {
                substitutedGenericRemaining[genericKey] = remaining - 1
                true
              } else {
                false
              }
            }
            aggregate.leave = leaveKeys.contains("$dateIso|${distribusi.teacherId}")
            aggregate.present = exactPresent || presentFallback
            if (calendarMatch != null && !aggregate.present) aggregate.present = true
            if (calendarMatch != null) aggregate.note = calendarMatch.note
            aggregate.substituted = exactSubstituted || substitutedFallback
            aggregate.expected = true
          }
      }
      cursor = cursor.plusDays(1)
    }

    attendanceRows.forEach { row ->
      val dateIso = row.cleanString("tanggal").take(10)
      val teacherId = row.cleanString("guru_id")
      if (dateIso.isBlank() || teacherId.isBlank()) return@forEach
      val rawDistribusiId = row.cleanString("distribusi_id")
      val distribusi = distribusiById[rawDistribusiId]
      val classId = row.cleanString("kelas_id").ifBlank { distribusi?.classId.orEmpty() }
      val subjectId = row.cleanString("mapel_id").ifBlank { distribusi?.subjectId.orEmpty() }
      if (classId.isBlank() || subjectId.isBlank()) return@forEach
      val jamId = row.cleanString("jam_pelajaran_id")
      val jam = jamById[jamId]
      val jamKey = buildJamMatchKey(jam?.start.orEmpty(), jam?.end.orEmpty(), jamId)
      val key = teacherExactKey(dateIso, teacherId, classId, subjectId, jamKey)
      val genericKey = teacherGenericKey(dateIso, teacherId, classId, subjectId)
      if (expectedExactKeys.contains(key) || (jamKey.isBlank() && expectedGenericKeys.contains(genericKey))) {
        return@forEach
      }
      val aggregate = grouped.getOrPut(key) {
        TeacherAggregate(
          dateIso = dateIso,
          teacherId = teacherId,
          classId = classId,
          subjectId = subjectId,
          timeLabel = buildTimeLabel(jam?.start.orEmpty(), jam?.end.orEmpty())
        )
      }
      aggregate.present = true
      if (row.cleanString("guru_pengganti_id").isNotBlank()) aggregate.substituted = true

      val substituteId = row.cleanString("guru_pengganti_id")
      if (substituteId.isNotBlank()) {
        val substituteKey = "$dateIso|$substituteId"
        grouped.getOrPut(substituteKey) {
          TeacherAggregate(
            dateIso = dateIso,
            teacherId = substituteId,
            classId = classId,
            subjectId = subjectId,
            timeLabel = buildTimeLabel(jam?.start.orEmpty(), jam?.end.orEmpty())
          )
        }.present = true
      }
    }
    leaveRows
      .filter { normalizeStatus(it.cleanString("status")) == "diterima" }
      .forEach { row ->
        val teacherId = row.cleanString("guru_id")
        val start = parseDate(row.cleanString("tanggal_mulai")) ?: return@forEach
        val end = parseDate(row.cleanString("tanggal_selesai")) ?: start
        var cursor = start
        while (!cursor.isAfter(end)) {
          val key = "${cursor}|$teacherId"
          val aggregate = grouped.getOrPut(key) { TeacherAggregate(dateIso = cursor.toString(), teacherId = teacherId) }
          aggregate.leave = true
          cursor = cursor.plusDays(1)
        }
      }

    return grouped.values
      .map { aggregate ->
        WakasekTeacherMonitoringRow(
          teacherId = aggregate.teacherId,
          teacherName = teacherMap[aggregate.teacherId].orEmpty().ifBlank { aggregate.teacherId },
          periodKey = aggregate.dateIso,
          periodLabel = formatDateLabel(aggregate.dateIso),
          className = classMap[aggregate.classId].orEmpty().ifBlank { "-" },
          subjectName = subjectMap[aggregate.subjectId].orEmpty().ifBlank { "-" },
          timeLabel = aggregate.timeLabel.ifBlank { "-" },
          status = aggregate.statusLabel(),
          note = aggregate.note,
          totalSessions = 1,
          presentCount = if (aggregate.present && !aggregate.leave) 1 else 0,
          substituteCount = if (aggregate.substituted) 1 else 0,
          leaveCount = if (aggregate.leave) 1 else 0,
          absentCount = if (aggregate.expected && !aggregate.present && !aggregate.leave) 1 else 0
        )
      }
      .sortedWith(compareByDescending<WakasekTeacherMonitoringRow> { it.periodKey }.thenBy { it.teacherName })
  }

  private fun buildStudentRows(
    attendanceRows: List<JSONObject>,
    studentMap: Map<String, String>,
    classMap: Map<String, String>,
    subjectMap: Map<String, String>
  ): List<WakasekStudentMonitoringRow> {
    return attendanceRows
      .mapNotNull { row ->
        val status = normalizeStudentStatus(row.cleanString("status"))
        val dateIso = row.cleanString("tanggal").take(10)
        val studentId = row.cleanString("santri_id")
        if (dateIso.isBlank() || studentId.isBlank()) return@mapNotNull null
        WakasekStudentMonitoringRow(
          studentId = studentId,
          studentName = studentMap[studentId].orEmpty().ifBlank { studentId },
          className = classMap[row.cleanString("kelas_id")].orEmpty(),
          dateIso = dateIso,
          status = status,
          subjectName = subjectMap[row.cleanString("mapel_id")].orEmpty(),
          periodKey = dateIso,
          periodLabel = formatDateLabel(dateIso)
        )
      }
      .distinctBy { "${it.dateIso}|${it.studentId}|${it.status}|${it.subjectName}" }
      .sortedWith(compareByDescending<WakasekStudentMonitoringRow> { it.dateIso }.thenBy { it.studentName })
  }

  private fun buildWakasekScoreSubjects(
    distribusiRows: List<JSONObject>,
    classMap: Map<String, String>,
    subjectMap: Map<String, String>,
    semesterMap: Map<String, JSONObject>,
    activeSemesterId: String
  ): List<SubjectOverview> {
    val rows = if (activeSemesterId.isNotBlank() && distribusiRows.any { it.cleanString("semester_id") == activeSemesterId }) {
      distribusiRows.filter { it.cleanString("semester_id") == activeSemesterId }
    } else {
      distribusiRows
    }
    return rows
      .mapNotNull { row ->
        val id = row.cleanString("id")
        val classId = row.cleanString("kelas_id")
        val subjectId = row.cleanString("mapel_id")
        if (id.isBlank() || classId.isBlank() || subjectId.isBlank()) return@mapNotNull null
        val semesterId = row.cleanString("semester_id")
        val semester = semesterMap[semesterId]
        SubjectOverview(
          id = id,
          title = subjectMap[subjectId].orEmpty().ifBlank { "Mapel" },
          className = classMap[classId].orEmpty().ifBlank { "Kelas" },
          semester = semesterLabel(semester),
          semesterActive = semesterId.isNotBlank() && (semesterId == activeSemesterId || isActiveSemester(semester)),
          attendancePending = 0,
          scorePending = 0,
          materialCount = 0
        )
      }
      .distinctBy { it.id }
      .sortedWith(compareBy<SubjectOverview> { it.className.lowercase(Locale.ROOT) }.thenBy { it.title.lowercase(Locale.ROOT) })
  }

  private fun parseLeaveRequestItem(item: JSONObject, teacherMap: Map<String, String>): LeaveRequestItem? {
    val id = item.cleanString("id")
    if (id.isBlank()) return null
    val teacherId = item.cleanString("guru_id")
    return LeaveRequestItem(
      id = id,
      startDateIso = item.cleanString("tanggal_mulai").take(10),
      endDateIso = item.cleanString("tanggal_selesai").take(10),
      durationDays = item.optInt("durasi_hari").takeIf { it > 0 } ?: 1,
      purpose = item.cleanString("keperluan"),
      status = normalizeStatus(item.cleanString("status")),
      reviewerNote = item.cleanString("catatan_wakasek"),
      reviewedAt = item.cleanString("reviewed_at"),
      createdAt = item.cleanString("created_at"),
      updatedAt = item.cleanString("updated_at"),
      teacherId = teacherId,
      teacherName = teacherMap[teacherId].orEmpty().ifBlank { teacherId }
    )
  }

  private fun fetchAttendanceRows(startDate: LocalDate, endDate: LocalDate): List<JSONObject> {
    val dateFilter = buildString {
      append("&tanggal=gte.")
      append(encodeValue(startDate.toString()))
      append("&tanggal=lte.")
      append(encodeValue(endDate.toString()))
      append("&order=tanggal.desc")
    }
    return runCatching {
      fetchRows(
        table = "absensi_santri",
        query = "select=id,tanggal,guru_id,guru_pengganti_id,santri_id,kelas_id,mapel_id,jam_pelajaran_id,distribusi_id,status$dateFilter"
      )
    }.getOrElse { error ->
      if (!shouldRetryWithFallbackSelect(error)) throw error
      fetchRows(
        table = "absensi_santri",
        query = "select=id,tanggal,guru_id,santri_id,kelas_id,mapel_id,status$dateFilter"
      )
    }
  }

  private fun fetchJadwalRows(): List<JSONObject> {
    return runCatching {
      fetchRows(
        table = "jadwal_pelajaran",
        query = "select=id,distribusi_id,hari,jam_mulai,jam_selesai,jam_pelajaran_id"
      )
    }.getOrElse { error ->
      if (!shouldRetryWithFallbackSelect(error)) throw error
      fetchRows(
        table = "jadwal_pelajaran",
        query = "select=id,distribusi_id,hari,jam_mulai,jam_selesai"
      )
    }
  }

  private fun fetchJamRows(): List<JSONObject> {
    return fetchRows(
      table = "jam_pelajaran",
      query = "select=id,jam_mulai,jam_selesai"
    )
  }

  private fun fetchCalendarRows(): List<JSONObject> {
    return runCatching {
      fetchRows(
        table = "kalender_akademik",
        query = "select=id,judul,detail,jenis_kegiatan,mulai,selesai"
      )
    }.getOrElse { error ->
      if (!shouldRetryWithFallbackSelect(error)) throw error
      fetchRows(
        table = "kalender_akademik",
        query = "select=id,judul,detail,mulai,selesai"
      )
    }
  }

  private fun buildCalendarAcademicHolidayDates(
    rows: List<JSONObject>,
    startDate: LocalDate,
    endDate: LocalDate
  ): Set<String> {
    return rows
      .filter { row ->
        val type = inferCalendarType(row)
        type == CalendarType.LiburSemua || type == CalendarType.LiburAkademik
      }
      .flatMap { row -> getDateRange(row.cleanString("mulai"), row.cleanString("selesai").ifBlank { row.cleanString("mulai") }) }
      .filter { date -> !date.isBefore(startDate) && !date.isAfter(endDate) }
      .map(LocalDate::toString)
      .toSet()
  }

  private fun buildCalendarClassIndex(
    rows: List<JSONObject>,
    classMap: Map<String, String>,
    startDate: LocalDate,
    endDate: LocalDate
  ): Map<String, CalendarClassEntry> {
    val classNameToId = classMap
      .mapNotNull { (id, name) ->
        val normalized = normalizeLookupLabel(name)
        if (id.isBlank() || normalized.isBlank()) null else normalized to id
      }
      .toMap()
    val index = linkedMapOf<String, CalendarClassEntry>()
    rows.forEach { row ->
      val type = inferCalendarType(row)
      if (type != CalendarType.Ujian && type != CalendarType.LiburKelas) return@forEach
      val detail = parseCalendarDetail(row.cleanString("detail"))
      val classIds = detail.classIds.toMutableSet()
      detail.classNames.forEach { className ->
        classNameToId[normalizeLookupLabel(className)]?.let(classIds::add)
      }
      val allClasses = classIds.isEmpty()
      val baseLabel = if (type == CalendarType.Ujian) "Waktu pelaksanaan ujian" else "Libur kelas"
      val extra = detail.text.ifBlank { row.cleanString("judul") }
      val note = if (extra.isNotBlank()) "$baseLabel - $extra" else baseLabel
      getDateRange(row.cleanString("mulai"), row.cleanString("selesai").ifBlank { row.cleanString("mulai") })
        .filter { date -> !date.isBefore(startDate) && !date.isAfter(endDate) }
        .forEach { date ->
          val entry = index.getOrPut(date.toString()) { CalendarClassEntry() }
          if (type == CalendarType.Ujian) {
            if (allClasses) entry.examAll = true
            entry.examClasses += classIds
            if (entry.examNote.isBlank()) entry.examNote = note
          } else {
            if (allClasses) entry.holidayAll = true
            entry.holidayClasses += classIds
            if (entry.holidayNote.isBlank()) entry.holidayNote = note
          }
        }
    }
    return index
  }

  private fun getCalendarClassMatch(
    index: Map<String, CalendarClassEntry>,
    dateIso: String,
    classId: String
  ): CalendarClassMatch? {
    val entry = index[dateIso] ?: return null
    val cleanClassId = classId.trim()
    return when {
      entry.examAll || entry.examClasses.contains(cleanClassId) ->
        CalendarClassMatch(type = CalendarType.Ujian, note = entry.examNote.ifBlank { "Waktu pelaksanaan ujian" })
      entry.holidayAll || entry.holidayClasses.contains(cleanClassId) ->
        CalendarClassMatch(type = CalendarType.LiburKelas, note = entry.holidayNote.ifBlank { "Libur kelas" })
      else -> null
    }
  }

  private fun fetchNameMap(table: String, nameField: String, ids: List<String>): Map<String, String> {
    if (ids.isEmpty()) return emptyMap()
    return ids.distinct()
      .chunked(100)
      .flatMap { chunk ->
        val inClause = chunk.joinToString(",") { "\"${it}\"" }
        fetchRows(table, "select=id,$nameField&id=in.($inClause)")
      }
      .associate { row ->
        row.cleanString("id") to row.cleanString(nameField)
      }
      .filterKeys(String::isNotBlank)
  }

  private fun fetchSemesterMap(ids: List<String>): Map<String, JSONObject> {
    if (ids.isEmpty()) return emptyMap()
    return ids.distinct()
      .chunked(100)
      .flatMap { chunk ->
        val inClause = chunk.joinToString(",") { "\"${it}\"" }
        fetchRows("semester", "select=id,nama,aktif,tahun_ajaran_id&id=in.($inClause)")
      }
      .associateBy { row -> row.cleanString("id") }
      .filterKeys(String::isNotBlank)
  }

  private fun resolveActiveSemesterId(): String {
    return runCatching {
      fetchRows(
        table = "semester",
        query = "select=id,nama,aktif,tahun_ajaran_id&aktif=eq.true&order=id.desc&limit=1"
      ).firstOrNull()?.cleanString("id").orEmpty()
    }.getOrDefault("")
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val pageSize = 1_000
    val rows = mutableListOf<JSONObject>()
    var offset = 0
    while (true) {
      val pageRows = createConnection(requestUrl).apply {
        setRequestProperty("Range-Unit", "items")
        setRequestProperty("Range", "$offset-${offset + pageSize - 1}")
      }.useJsonArrayResponse { page ->
        List(page.length()) { index -> page.optJSONObject(index) }.filterNotNull()
      }
      rows += pageRows
      if (pageRows.size < pageSize) break
      offset += pageSize
    }
    return rows
  }

  private fun createConnection(requestUrl: String, method: String = "GET"): HttpURLConnection {
    return (URL(requestUrl).openConnection() as HttpURLConnection).apply {
      requestMethod = method
      connectTimeout = 15_000
      readTimeout = 15_000
      setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
      setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
      setRequestProperty("Accept", "application/json")
      setRequestProperty("Accept-Charset", "UTF-8")
    }
  }

  private fun parseDate(value: String): LocalDate? {
    return runCatching { LocalDate.parse(value.trim().take(10)) }.getOrNull()
  }

  private fun normalizeName(value: String): String {
    return value.trim().replace(Regex("\\s+"), " ").lowercase()
  }

  private fun normalizeLookupLabel(value: String): String {
    return value.trim().lowercase()
      .replace(Regex("[^a-z0-9]+"), " ")
      .replace(Regex("\\s+"), " ")
      .trim()
  }

  private fun normalizeStatus(value: String): String {
    val normalized = value.trim().lowercase()
    return when {
      normalized.contains("terima") || normalized.contains("setuju") || normalized.contains("approve") -> "diterima"
      normalized.contains("tolak") || normalized.contains("reject") -> "ditolak"
      else -> "menunggu"
    }
  }

  private fun isWakasekKurikulumRole(value: String): Boolean {
    val clean = value.trim().lowercase()
      .replace("_", " ")
      .replace("-", " ")
      .replace(Regex("\\s+"), " ")
    val compact = clean.replace(" ", "")
    return compact == "wakasekakademik" ||
      compact == "wakasekbidangakademik" ||
      compact == "wakasekkurikulum" ||
      compact == "wakasekbidangkurikulum" ||
      (clean.contains("wakasek") && (clean.contains("akademik") || clean.contains("kurikulum")))
  }

  private fun normalizeStudentStatus(value: String): String {
    val normalized = value.trim().lowercase()
    return when {
      normalized.contains("terlambat") -> "terlambat"
      normalized.contains("sakit") -> "sakit"
      normalized.contains("izin") -> "izin"
      normalized.contains("alpa") || normalized.contains("alpha") -> "alpa"
      else -> "hadir"
    }
  }

  private fun inferCalendarType(row: JSONObject): CalendarType {
    val direct = normalizeCalendarType(row.cleanString("jenis_kegiatan"))
    if (direct != CalendarType.None) return direct
    val text = "${row.cleanString("judul")} ${row.cleanString("detail")}".lowercase()
    return when {
      text.contains("libur semua") -> CalendarType.LiburSemua
      text.contains("libur akademik") -> CalendarType.LiburAkademik
      text.contains("libur kelas") -> CalendarType.LiburKelas
      text.contains("ujian") -> CalendarType.Ujian
      else -> CalendarType.None
    }
  }

  private fun normalizeCalendarType(value: String): CalendarType {
    return when (value.trim().lowercase()) {
      "libur_semua_kegiatan" -> CalendarType.LiburSemua
      "libur_akademik" -> CalendarType.LiburAkademik
      "libur_kelas" -> CalendarType.LiburKelas
      "ujian" -> CalendarType.Ujian
      else -> CalendarType.None
    }
  }

  private fun parseCalendarDetail(value: String): CalendarDetailPayload {
    val raw = value.trim()
    if (raw.isBlank()) return CalendarDetailPayload()
    return runCatching {
      val json = JSONObject(raw)
      CalendarDetailPayload(
        text = json.optString("text").ifBlank { json.optString("detail") }.trim(),
        classIds = json.optJSONArray("kelas_ids")?.toStringList().orEmpty(),
        classNames = json.optJSONArray("kelas_nama")?.toStringList().orEmpty()
      )
    }.getOrDefault(CalendarDetailPayload(text = raw))
  }

  private fun getDateRange(startValue: String, endValue: String): List<LocalDate> {
    val start = parseDate(startValue) ?: return emptyList()
    val end = parseDate(endValue).takeIf { it != null && !it.isBefore(start) } ?: start
    val dates = mutableListOf<LocalDate>()
    var cursor = start
    while (!cursor.isAfter(end)) {
      dates += cursor
      cursor = cursor.plusDays(1)
    }
    return dates
  }

  private fun normalizeDay(dayOfWeek: DayOfWeek): String {
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

  private fun normalizeDay(value: String): String {
    return value.trim().lowercase()
      .replace("'", "")
      .replace(" ", "")
      .replace("jum'at", "jumat")
  }

  private fun formatDateLabel(dateIso: String): String {
    return parseDate(dateIso)?.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
      ?: dateIso
  }

  private fun semesterLabel(semester: JSONObject?): String {
    return semester?.cleanString("nama").orEmpty().ifBlank { "-" }
  }

  private fun isActiveSemester(semester: JSONObject?): Boolean {
    if (semester == null) return false
    return semester.opt("aktif") == true || semester.cleanString("aktif").equals("true", ignoreCase = true)
  }

  private fun buildTimeLabel(start: String, end: String): String {
    val normalizedStart = start.trim().take(5)
    val normalizedEnd = end.trim().take(5)
    return when {
      normalizedStart.isNotBlank() && normalizedEnd.isNotBlank() -> "$normalizedStart-$normalizedEnd"
      normalizedStart.isNotBlank() -> normalizedStart
      normalizedEnd.isNotBlank() -> normalizedEnd
      else -> ""
    }
  }

  private fun buildJamKey(start: String, end: String): String {
    val normalizedStart = start.trim().take(5)
    val normalizedEnd = end.trim().take(5)
    return if (normalizedStart.isBlank() && normalizedEnd.isBlank()) "" else "$normalizedStart|$normalizedEnd"
  }

  private fun buildJamMatchKey(start: String, end: String, jamId: String): String {
    val rangeKey = buildJamKey(start, end)
    if (rangeKey.isNotBlank()) return rangeKey
    val cleanJamId = jamId.trim()
    return if (cleanJamId.isNotBlank()) "JP:$cleanJamId" else ""
  }

  private fun teacherExactKey(dateIso: String, teacherId: String, classId: String, subjectId: String, jamKey: String): String {
    return "${dateIso}|${teacherId}|${classId}|${subjectId}|${jamKey}"
  }

  private fun teacherGenericKey(dateIso: String, teacherId: String, classId: String, subjectId: String): String {
    return "${dateIso}|${teacherId}|${classId}|${subjectId}"
  }

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value.trim(), Charsets.UTF_8.name())
  }

  private fun resolveErrorMessage(error: Throwable, fallback: String): String {
    val raw = error.message.orEmpty().trim()
    if (raw.isBlank()) return fallback
    return runCatching {
      val json = JSONObject(raw)
      json.optString("message").ifBlank { raw }
    }.getOrDefault(raw)
  }

  private fun shouldRetryWithFallbackSelect(error: Throwable): Boolean {
    val message = error.message.orEmpty().lowercase()
    return message.contains("column") ||
      message.contains("schema cache") ||
      message.contains("could not find") ||
      message.contains("42703") ||
      message.contains("pgrst204")
  }
}

private data class TeacherAggregate(
  val dateIso: String,
  val teacherId: String,
  val classId: String = "",
  val subjectId: String = "",
  val timeLabel: String = "",
  var note: String = "",
  var present: Boolean = false,
  var substituted: Boolean = false,
  var leave: Boolean = false,
  var expected: Boolean = false
) {
  fun statusLabel(): String {
    return when {
      leave -> "Izin"
      substituted -> "Diganti"
      present -> "Masuk"
      expected -> "Tidak Masuk"
      else -> "Masuk"
    }
  }
}

private data class DistribusiMonitorRow(
  val id: String,
  val teacherId: String,
  val classId: String,
  val subjectId: String
)

private data class JamMonitorRow(
  val start: String,
  val end: String
)

private const val NoJamMarker = "__NO_JAM__"

private enum class CalendarType {
  None,
  LiburSemua,
  LiburAkademik,
  LiburKelas,
  Ujian
}

private data class CalendarDetailPayload(
  val text: String = "",
  val classIds: List<String> = emptyList(),
  val classNames: List<String> = emptyList()
)

private data class CalendarClassEntry(
  var examAll: Boolean = false,
  val examClasses: MutableSet<String> = mutableSetOf(),
  var examNote: String = "",
  var holidayAll: Boolean = false,
  val holidayClasses: MutableSet<String> = mutableSetOf(),
  var holidayNote: String = ""
)

private data class CalendarClassMatch(
  val type: CalendarType,
  val note: String
)

private inline fun <T> HttpURLConnection.useJsonArrayResponse(block: (JSONArray) -> T): T {
  return try {
    val code = responseCode
    val payload = inputStreamOrError(code in 200..299).bufferedReader().use(BufferedReader::readText)
    if (code !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $code" })
    }
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.inputStreamOrError(success: Boolean) =
  if (success) inputStream else errorStream ?: inputStream

private fun JSONObject.cleanString(key: String): String {
  if (!has(key) || isNull(key)) return ""
  return opt(key)?.toString().orEmpty().trim().takeUnless { it.equals("null", ignoreCase = true) } ?: ""
}

private fun JSONArray.toStringList(): List<String> {
  return List(length()) { index -> opt(index)?.toString().orEmpty().trim() }
    .filter(String::isNotBlank)
}
