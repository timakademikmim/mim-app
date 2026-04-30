package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.AttendanceApprovalRequest
import com.mim.guruapp.data.model.AttendanceApprovalStudent
import com.mim.guruapp.data.model.AttendanceHistoryEntry
import com.mim.guruapp.data.model.AttendanceStudent
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.MapelAttendanceSnapshot
import com.mim.guruapp.data.model.SubjectOverview
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
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface GuruMapelAttendanceSaveResult {
  data object Success : GuruMapelAttendanceSaveResult
  data class SubmittedForReview(val message: String) : GuruMapelAttendanceSaveResult
  data class Error(val message: String) : GuruMapelAttendanceSaveResult
}

sealed interface GuruMapelAttendanceReviewResult {
  data class Success(val message: String) : GuruMapelAttendanceReviewResult
  data class Error(val message: String) : GuruMapelAttendanceReviewResult
}

class GuruMapelAttendanceRemoteDataSource {
  suspend fun fetchAttendanceSnapshot(
    distribusiId: String,
    fallbackTitle: String,
    fallbackClassName: String
  ): MapelAttendanceSnapshot? = withContext(Dispatchers.IO) {
    runCatching {
      val distribusiRow = fetchRows(
        table = "distribusi_mapel",
        query = "select=id,kelas_id,mapel_id,semester_id&id=eq.${encodeValue(distribusiId)}&limit=1"
      ).firstOrNull() ?: return@withContext null

      val kelasId = distribusiRow.optString("kelas_id").trim()
      val mapelId = distribusiRow.optString("mapel_id").trim()
      val semesterId = distribusiRow.optString("semester_id").trim()
      if (kelasId.isBlank() || mapelId.isBlank()) return@withContext null

      val santriRows = fetchRows(
        table = "santri",
        query = buildString {
          append("select=id,nama,kelas_id,aktif")
          append("&kelas_id=eq.")
          append(encodeValue(kelasId))
          append("&aktif=eq.true&order=nama.asc")
        }
      )

      val absensiByDistribusiRows = fetchAttendanceRowsWithOptionalMaterial(
        queryTail = buildString {
          append("&distribusi_id=eq.")
          append(encodeValue(distribusiId))
          append("&order=tanggal.desc")
        }
      )

      val absensiByMapelRows = fetchAttendanceRowsWithOptionalMaterial(
        queryTail = buildString {
          append("&kelas_id=eq.")
          append(encodeValue(kelasId))
          append("&mapel_id=eq.")
          append(encodeValue(mapelId))
          if (semesterId.isNotBlank()) {
            append("&semester_id=eq.")
            append(encodeValue(semesterId))
          } else {
            append("&semester_id=is.null")
          }
          append("&order=tanggal.desc")
        }
      )

      val mergedAbsensiRows = buildList {
        val seenKeys = mutableSetOf<String>()
        (absensiByDistribusiRows + absensiByMapelRows).forEach { row ->
          val dedupeKey = row.opt("id")?.toString().orEmpty().trim().ifBlank {
            listOf(
              row.optString("santri_id").trim(),
              row.optString("tanggal").trim(),
              row.optString("mapel_id").trim(),
              row.optString("kelas_id").trim(),
              row.optString("semester_id").trim(),
              row.optString("status").trim()
            ).joinToString("|")
          }
          if (dedupeKey.isBlank() || seenKeys.contains(dedupeKey)) return@forEach
          seenKeys += dedupeKey
          add(row)
        }
      }

      val allDates = mergedAbsensiRows
        .map { it.optString("tanggal").take(10) }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
      val rangeLabel = buildAttendanceRangeLabel(allDates)

      val students = santriRows.map { santri ->
        val santriId = santri.opt("id")?.toString().orEmpty().trim()
        val studentRows = mergedAbsensiRows.filter { row ->
          row.optString("santri_id").trim() == santriId
        }
        val history = buildHistoryEntries(studentRows)
        val counts = countStatuses(history.map { it.status.lowercase() })
        val total = history.size
        AttendanceStudent(
          id = santriId,
          name = santri.optString("nama").trim().ifBlank { "-" },
          hadirPercent = percentage(counts["hadir"] ?: 0, total),
          terlambatPercent = percentage(counts["terlambat"] ?: 0, total),
          sakitPercent = percentage(counts["sakit"] ?: 0, total),
          izinPercent = percentage(counts["izin"] ?: 0, total),
          alpaPercent = percentage(counts["alpa"] ?: 0, total),
          history = history
        )
      }

      MapelAttendanceSnapshot(
        distribusiId = distribusiId,
        subjectTitle = fallbackTitle,
        className = fallbackClassName,
        rangeLabel = rangeLabel,
        students = students,
        updatedAt = System.currentTimeMillis(),
        supportsPatronMateri = true
      )
    }.getOrNull()
  }

  suspend fun fetchDelegatedAttendanceContext(
    teacherRowId: String,
    teacherKaryawanId: String,
    dateIso: String
  ): Pair<List<SubjectOverview>, List<CalendarEvent>>? = withContext(Dispatchers.IO) {
    runCatching {
      val normalizedDate = dateIso.trim().take(10)
      if (normalizedDate.isBlank()) return@withContext null

      val teacherIds = listOf(teacherRowId, teacherKaryawanId)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
      if (teacherIds.isEmpty()) return@withContext null

      val taskRows = teacherIds
        .flatMap { teacherId ->
          fetchRows(
            table = "absensi_pengganti_tugas",
            query = buildString {
              append("select=id,tanggal,kelas_id,mapel_id,distribusi_id,semester_id,jam_pelajaran_id,guru_asal_id,guru_pengganti_id,keterangan,status,created_at,filled_at")
              append("&guru_pengganti_id=eq.")
              append(encodeValue(teacherId))
              append("&tanggal=eq.")
              append(encodeValue(normalizedDate))
              append("&status=eq.pending")
              append("&order=created_at.asc")
            }
          )
        }
        .distinctBy { it.opt("id")?.toString().orEmpty().trim() }

      if (taskRows.isEmpty()) return@runCatching emptyList<SubjectOverview>() to emptyList()

      val kelasIds = taskRows.mapNotNull { it.optString("kelas_id").trim().takeIf(String::isNotBlank) }.distinct()
      val mapelIds = taskRows.mapNotNull { it.optString("mapel_id").trim().takeIf(String::isNotBlank) }.distinct()
      val semesterIds = taskRows.mapNotNull { it.cleanString("semester_id").takeIf(String::isNotBlank) }.distinct()
      val jamIds = taskRows.mapNotNull { it.cleanString("jam_pelajaran_id").takeIf(String::isNotBlank) }.distinct()

      val kelasMap = fetchSimpleNameMap("kelas", "nama_kelas", kelasIds)
      val mapelMap = fetchSimpleNameMap("mapel", "nama", mapelIds)
      val semesterMap = fetchSemesterLabelMap(semesterIds)
      val jamMap = fetchJamLabelMap(jamIds)

      val subjects = taskRows
        .mapNotNull { row ->
          val distribusiId = row.cleanString("distribusi_id")
          val kelasId = row.cleanString("kelas_id")
          val mapelId = row.cleanString("mapel_id")
          if (distribusiId.isBlank() || kelasId.isBlank() || mapelId.isBlank()) return@mapNotNull null

          SubjectOverview(
            id = distribusiId,
            title = mapelMap[mapelId].orEmpty().ifBlank { "Mapel" },
            className = kelasMap[kelasId].orEmpty().ifBlank { "-" },
            semester = semesterMap[row.cleanString("semester_id")].orEmpty().ifBlank { "Tugas Ganti" },
            semesterActive = true,
            attendancePending = 0,
            scorePending = 0,
            materialCount = 0
          )
        }
        .distinctBy { it.id }
        .sortedWith(compareBy<SubjectOverview> { it.className.lowercase() }.thenBy { it.title.lowercase() })

      val subjectById = subjects.associateBy { it.id }
      val events = taskRows.mapNotNull { row ->
        val distribusiId = row.cleanString("distribusi_id")
        val subject = subjectById[distribusiId] ?: return@mapNotNull null
        val rowId = row.cleanString("id").ifBlank { distribusiId }
        val jamId = row.cleanString("jam_pelajaran_id")

        CalendarEvent(
          id = "delegated|$normalizedDate|$rowId",
          startDateIso = normalizedDate,
          endDateIso = normalizedDate,
          title = "${subject.title} - ${subject.className}",
          description = row.cleanString("keterangan").ifBlank { "Tugas ganti absensi." },
          timeLabel = jamMap[jamId].orEmpty().ifBlank { "Tugas Ganti" },
          colorHex = GuruTeachingScheduleRemoteDataSource.CATEGORY_TEACHING_COLOR,
          categoryKey = GuruTeachingScheduleRemoteDataSource.CATEGORY_TEACHING,
          lessonSlotId = jamId,
          distribusiId = distribusiId
        )
      }

      subjects to events
    }.getOrNull()
  }

  suspend fun fetchAttendanceApprovalRequest(
    submissionId: String,
    teacherRowId: String,
    teacherKaryawanId: String
  ): AttendanceApprovalRequest? = withContext(Dispatchers.IO) {
    runCatching {
      val normalizedId = submissionId.cleanDbText()
      if (normalizedId.isBlank()) return@withContext null

      val row = fetchRows(
        table = "absensi_pengajuan_pengganti",
        query = buildString {
          append("select=id,tanggal,kelas_id,mapel_id,distribusi_id,semester_id,guru_asal_id,guru_pengganti_id,jam_pelajaran_1_id,jam_pelajaran_2_id,patron_materi,keterangan,payload_absensi,status,created_at")
          append("&id=eq.")
          append(encodeValue(normalizedId))
          append("&limit=1")
        }
      ).firstOrNull() ?: return@withContext null

      val normalizedRow = normalizeSubmissionRow(row)
      val allowedTeacherIds = listOf(teacherRowId, teacherKaryawanId)
        .map { it.cleanDbText() }
        .filter { it.isNotBlank() }
      if (allowedTeacherIds.isNotEmpty() && normalizedRow.guruAsalId !in allowedTeacherIds) {
        return@withContext null
      }

      val kelasLabel = fetchSimpleNameMap("kelas", "nama_kelas", listOf(normalizedRow.kelasId))[normalizedRow.kelasId]
        .orEmpty()
        .ifBlank { "-" }
      val mapelLabel = fetchSimpleNameMap("mapel", "nama", listOf(normalizedRow.mapelId))[normalizedRow.mapelId]
        .orEmpty()
        .ifBlank { "Mapel" }
      val guruPenggantiLabel = fetchSimpleNameMap("karyawan", "nama", listOf(normalizedRow.guruPenggantiId))[normalizedRow.guruPenggantiId]
        .orEmpty()
        .ifBlank { "Guru Pengganti" }
      val jamMap = fetchJamLabelMap(
        listOf(normalizedRow.jamPelajaran1Id, normalizedRow.jamPelajaran2Id)
          .map { it.cleanDbText() }
          .filter { it.isNotBlank() }
      )

      AttendanceApprovalRequest(
        id = normalizedRow.id,
        distribusiId = normalizedRow.distribusiId,
        dateIso = normalizedRow.dateIso,
        className = kelasLabel,
        subjectTitle = mapelLabel,
        sourceTeacherId = normalizedRow.guruAsalId,
        substituteTeacherId = normalizedRow.guruPenggantiId,
        substituteTeacherName = guruPenggantiLabel,
        lessonLabels = listOf(normalizedRow.jamPelajaran1Id, normalizedRow.jamPelajaran2Id)
          .mapNotNull { jamMap[it] }
          .distinct(),
        material = normalizedRow.patronMateri,
        note = normalizedRow.note,
        students = normalizedRow.payloadAbsensi
          .sortedBy { it.studentName.lowercase() },
        createdAtMillis = parseTimestampMillis(normalizedRow.createdAt)
      )
    }.getOrNull()
  }

  suspend fun reviewAttendanceSubmission(
    submissionId: String,
    teacherRowId: String,
    teacherKaryawanId: String,
    approve: Boolean,
    reviewerNote: String
  ): GuruMapelAttendanceReviewResult = withContext(Dispatchers.IO) {
    val normalizedId = submissionId.cleanDbText()
    val reviewerId = teacherRowId.cleanDbText().ifBlank { teacherKaryawanId.cleanDbText() }
    if (normalizedId.isBlank()) {
      return@withContext GuruMapelAttendanceReviewResult.Error("Pengajuan absensi tidak ditemukan.")
    }

    val row = fetchRows(
      table = "absensi_pengajuan_pengganti",
      query = buildString {
        append("select=id,tanggal,kelas_id,mapel_id,distribusi_id,semester_id,guru_asal_id,guru_pengganti_id,jam_pelajaran_1_id,jam_pelajaran_2_id,patron_materi,keterangan,payload_absensi,status,created_at")
        append("&id=eq.")
        append(encodeValue(normalizedId))
        append("&limit=1")
      }
    ).firstOrNull()
      ?: return@withContext GuruMapelAttendanceReviewResult.Error("Pengajuan absensi tidak ditemukan.")

    val submission = normalizeSubmissionRow(row)
    val allowedTeacherIds = listOf(teacherRowId, teacherKaryawanId)
      .map { it.cleanDbText() }
      .filter { it.isNotBlank() }
    if (allowedTeacherIds.isNotEmpty() && submission.guruAsalId !in allowedTeacherIds) {
      return@withContext GuruMapelAttendanceReviewResult.Error("Anda tidak memiliki akses untuk meninjau pengajuan ini.")
    }
    if (submission.status != "pending") {
      return@withContext GuruMapelAttendanceReviewResult.Error("Pengajuan ini sudah ditinjau sebelumnya.")
    }

    return@withContext try {
      if (approve) {
        val jamIds = listOf(submission.jamPelajaran1Id, submission.jamPelajaran2Id)
          .map { it.cleanDbText() }
          .filter { it.isNotBlank() }
          .distinct()
        val jamIdsToSave = jamIds.ifEmpty { listOf("") }
        val payloads = JSONArray()
        submission.payloadAbsensi.forEach { item ->
          jamIdsToSave.forEach { jamId ->
            payloads.put(
              JSONObject().apply {
                put("tanggal", submission.dateIso)
                put("kelas_id", submission.kelasId)
                put("mapel_id", submission.mapelId)
                put("guru_id", submission.guruAsalId)
                put("jam_pelajaran_id", jamId.ifBlank { JSONObject.NULL })
                put("semester_id", submission.semesterId.ifBlank { JSONObject.NULL })
                put("distribusi_id", submission.distribusiId.ifBlank { JSONObject.NULL })
                put("santri_id", item.studentId)
                put("status", normalizeStatusLabel(item.status))
                put("guru_pengganti_id", submission.guruPenggantiId.ifBlank { JSONObject.NULL })
                put("keterangan_pengganti", submission.note.ifBlank { JSONObject.NULL })
                if (submission.patronMateri.isNotBlank()) {
                  put("patron_materi", submission.patronMateri)
                }
              }
            )
          }
        }

        try {
          upsertAttendanceRows(
            rows = payloads,
            onConflict = "tanggal,kelas_id,mapel_id,jam_pelajaran_id,santri_id"
          )
        } catch (error: Exception) {
          val message = error.message.orEmpty()
          if (message.contains("no unique or exclusion constraint", ignoreCase = true) && jamIds.size <= 1) {
            upsertAttendanceRows(
              rows = payloads,
              onConflict = "tanggal,kelas_id,mapel_id,santri_id"
            )
          } else {
            throw error
          }
        }
      }

      val updateUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/absensi_pengajuan_pengganti?id=eq.${encodeValue(normalizedId)}"
      val connection = createConnection(updateUrl, "PATCH").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Prefer", "return=minimal")
      }
      val payload = JSONObject().apply {
        put("status", if (approve) "approved" else "rejected")
        put("reviewed_by", reviewerId.ifBlank { JSONObject.NULL })
        put("reviewed_at", Instant.now().toString())
        put("review_note", reviewerNote.cleanDbText().ifBlank { JSONObject.NULL })
      }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      connection.useEmptyResponse()

      GuruMapelAttendanceReviewResult.Success(
        if (approve) {
          "Absensi pengganti disetujui dan langsung masuk ke sistem."
        } else {
          "Pengajuan absensi pengganti berhasil ditolak."
        }
      )
    } catch (_: SocketTimeoutException) {
      GuruMapelAttendanceReviewResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (error: Exception) {
      GuruMapelAttendanceReviewResult.Error(
        error.message
          ?.takeIf { it.isNotBlank() }
          ?.let { "Gagal meninjau absensi pengganti: $it" }
          ?: "Gagal meninjau absensi pengganti."
      )
    }
  }

  suspend fun saveAttendanceStatus(
    distribusiId: String,
    santriId: String,
    dateIso: String,
    status: String,
    rowIds: List<String>,
    lessonSlotId: String,
    patronMateri: String,
    teacherRowId: String,
    teacherKaryawanId: String
  ): GuruMapelAttendanceSaveResult = withContext(Dispatchers.IO) {
    val normalizedStatus = normalizeStatusLabel(status)
    val normalizedMateri = patronMateri.cleanDbText()
    if (distribusiId.isBlank() || santriId.isBlank() || dateIso.isBlank() || normalizedStatus.isBlank()) {
      return@withContext GuruMapelAttendanceSaveResult.Error("Data absensi belum lengkap.")
    }

    val distribusiRow = fetchRows(
      table = "distribusi_mapel",
      query = "select=id,kelas_id,mapel_id,semester_id&id=eq.${encodeValue(distribusiId)}&limit=1"
    ).firstOrNull()
      ?: return@withContext GuruMapelAttendanceSaveResult.Error("Distribusi mapel tidak ditemukan.")

    return@withContext try {
      if (rowIds.isNotEmpty()) {
        val inClause = rowIds.map { "\"${it}\"" }.joinToString(",")
        val updateUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/absensi_santri?id=in.($inClause)"
        val connection = createConnection(updateUrl, "PATCH").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        val payload = JSONObject().apply {
          put("status", normalizedStatus)
          if (normalizedMateri.isNotBlank()) {
            put("patron_materi", normalizedMateri)
          }
        }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        connection.useJsonArrayResponse { }
      } else {
        val insertUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/absensi_santri"
        val connection = createConnection(insertUrl, "POST").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        val payload = JSONArray().put(
          JSONObject().apply {
            put("tanggal", dateIso)
            put("kelas_id", distribusiRow.optString("kelas_id").trim())
            put("mapel_id", distribusiRow.optString("mapel_id").trim())
            put("guru_id", teacherRowId.trim().ifBlank { teacherKaryawanId.trim() })
            put("jam_pelajaran_id", lessonSlotId.cleanDbText().ifBlank { JSONObject.NULL })
            put("semester_id", distribusiRow.optString("semester_id").trim().ifBlank { JSONObject.NULL })
            put("distribusi_id", distribusiId)
            put("santri_id", santriId)
            put("status", normalizedStatus)
            if (normalizedMateri.isNotBlank()) {
              put("patron_materi", normalizedMateri)
            }
          }
        )
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        connection.useJsonArrayResponse { }
      }
      GuruMapelAttendanceSaveResult.Success
    } catch (_: SocketTimeoutException) {
      GuruMapelAttendanceSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      GuruMapelAttendanceSaveResult.Error("Gagal menyimpan perubahan absensi.")
    }
  }

  suspend fun saveAttendanceStatuses(
    distribusiId: String,
    changesByStudent: Map<String, List<AttendanceHistoryEntry>>,
    teacherRowId: String,
    teacherKaryawanId: String,
    sourceTeacherId: String = "",
    substituteNote: String = ""
  ): GuruMapelAttendanceSaveResult = withContext(Dispatchers.IO) {
    val normalizedChanges = changesByStudent
      .mapKeys { it.key.trim() }
      .filterKeys { it.isNotBlank() }
      .mapValues { (_, changes) ->
        changes.filter { change ->
          change.dateIso.isNotBlank() && normalizeStatusLabel(change.status).isNotBlank()
        }
      }
      .filterValues { it.isNotEmpty() }

    if (distribusiId.isBlank() || normalizedChanges.isEmpty()) {
      return@withContext GuruMapelAttendanceSaveResult.Error("Data absensi belum lengkap.")
    }

    val distribusiRow = fetchRows(
      table = "distribusi_mapel",
      query = "select=id,kelas_id,mapel_id,semester_id&id=eq.${encodeValue(distribusiId)}&limit=1"
    ).firstOrNull()
      ?: return@withContext GuruMapelAttendanceSaveResult.Error("Distribusi mapel tidak ditemukan.")

    return@withContext try {
      val updateIdsByStatusAndMaterial = linkedMapOf<Pair<String, String>, MutableSet<String>>()
      val insertRows = JSONArray()
      val changeDates = normalizedChanges.values
        .flatten()
        .map { it.dateIso.take(10) }
        .filter { it.isNotBlank() }
        .distinct()
      val manualSourceTeacherId = sourceTeacherId.cleanDbText()
      val delegationRows = fetchPendingDelegationRows(
        distribusiId = distribusiId,
        dates = changeDates,
        teacherRowId = teacherRowId,
        teacherKaryawanId = teacherKaryawanId
      )
      if (manualSourceTeacherId.isNotBlank() && delegationRows.isEmpty()) {
        return@withContext submitAttendanceSubstituteProposal(
          distribusiId = distribusiId,
          distribusiRow = distribusiRow,
          changesByStudent = normalizedChanges,
          teacherRowId = teacherRowId,
          teacherKaryawanId = teacherKaryawanId,
          sourceTeacherId = manualSourceTeacherId,
          substituteNote = substituteNote
        )
      }
      normalizedChanges.forEach { (santriId, changes) ->
        changes.forEach { change ->
          val normalizedStatus = normalizeStatusLabel(change.status)
          val normalizedMateri = change.patronMateri.cleanDbText()
          val rowIds = change.rowIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
          if (rowIds.isNotEmpty()) {
            val ids = updateIdsByStatusAndMaterial.getOrPut(normalizedStatus to normalizedMateri) { linkedSetOf() }
            ids += rowIds
          } else {
            val normalizedDate = change.dateIso.take(10)
            val lessonSlotId = change.lessonSlotId.cleanDbText()
            val delegation = resolveDelegationRow(
              rows = delegationRows,
              dateIso = normalizedDate,
              lessonSlotId = lessonSlotId
            )
            val currentTeacherId = teacherRowId.trim().ifBlank { teacherKaryawanId.trim() }
            val sourceTeacher = manualSourceTeacherId
            val isManualSubstitute = sourceTeacher.isNotBlank()
            val ownerTeacherId = delegation?.guruAsalId?.ifBlank { sourceTeacher.ifBlank { currentTeacherId } }
              ?: sourceTeacher.ifBlank { currentTeacherId }
            val substituteTeacherValue = if (delegation != null || isManualSubstitute) currentTeacherId else ""
            val substituteNoteValue = delegation?.note?.ifBlank { substituteNote.cleanDbText() }
              ?: substituteNote.cleanDbText()
            insertRows.put(
              JSONObject().apply {
                put("tanggal", normalizedDate)
                put("kelas_id", distribusiRow.optString("kelas_id").trim())
                put("mapel_id", distribusiRow.optString("mapel_id").trim())
                put("guru_id", ownerTeacherId)
                put("jam_pelajaran_id", lessonSlotId.ifBlank { JSONObject.NULL })
                put(
                  "semester_id",
                  (delegation?.semesterId ?: distribusiRow.optString("semester_id").trim()).ifBlank { JSONObject.NULL }
                )
                put("distribusi_id", distribusiId)
                put("santri_id", santriId)
                put("status", normalizedStatus)
                put("guru_pengganti_id", substituteTeacherValue.ifBlank { JSONObject.NULL })
                put("keterangan_pengganti", substituteNoteValue.ifBlank { JSONObject.NULL })
                if (normalizedMateri.isNotBlank()) {
                  put("patron_materi", normalizedMateri)
                }
              }
            )
          }
        }
      }

      updateIdsByStatusAndMaterial.forEach { (statusAndMaterial, rowIds) ->
        if (rowIds.isEmpty()) return@forEach
        val (status, material) = statusAndMaterial
        val inClause = rowIds.joinToString(",") { "\"${it}\"" }
        val updateUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/absensi_santri?id=in.($inClause)"
        val connection = createConnection(updateUrl, "PATCH").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=minimal")
        }
        val payload = JSONObject().apply {
          put("status", status)
          if (material.isNotBlank()) {
            put("patron_materi", material)
          }
        }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        connection.useEmptyResponse()
      }

      if (insertRows.length() > 0) {
        try {
          upsertAttendanceRows(
            rows = insertRows,
            onConflict = "tanggal,kelas_id,mapel_id,jam_pelajaran_id,santri_id"
          )
        } catch (error: Exception) {
          val message = error.message.orEmpty()
          if (message.contains("no unique or exclusion constraint", ignoreCase = true)) {
            upsertAttendanceRows(
              rows = insertRows,
              onConflict = "tanggal,kelas_id,mapel_id,santri_id"
            )
          } else {
            throw error
          }
        }
      }

      val completedDelegationIds = delegationRows.map { it.id }.filter { it.isNotBlank() }.distinct()
      if (completedDelegationIds.isNotEmpty()) {
        markDelegationsDone(completedDelegationIds)
      }

      GuruMapelAttendanceSaveResult.Success
    } catch (_: SocketTimeoutException) {
      GuruMapelAttendanceSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (error: Exception) {
      GuruMapelAttendanceSaveResult.Error(
        error.message
          ?.takeIf { it.isNotBlank() }
          ?.let { "Gagal menyimpan perubahan absensi: $it" }
          ?: "Gagal menyimpan perubahan absensi."
      )
    }
  }

  suspend fun deleteAttendanceRows(
    rowIds: List<String>
  ): GuruMapelAttendanceSaveResult = withContext(Dispatchers.IO) {
    val normalizedIds = rowIds
      .map { it.cleanDbText() }
      .filter { it.isNotBlank() }
      .distinct()

    if (normalizedIds.isEmpty()) {
      return@withContext GuruMapelAttendanceSaveResult.Error("Data absensi belum memiliki ID untuk dihapus.")
    }

    return@withContext try {
      val inClause = normalizedIds.joinToString(",") { "\"${it}\"" }
      val deleteUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/absensi_santri?id=in.($inClause)"
      val connection = createConnection(deleteUrl, "DELETE").apply {
        setRequestProperty("Prefer", "return=minimal")
      }
      connection.useEmptyResponse()
      GuruMapelAttendanceSaveResult.Success
    } catch (_: SocketTimeoutException) {
      GuruMapelAttendanceSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (error: Exception) {
      GuruMapelAttendanceSaveResult.Error(
        error.message
          ?.takeIf { it.isNotBlank() }
          ?.let { "Gagal menghapus absensi: $it" }
          ?: "Gagal menghapus absensi."
      )
    }
  }

  suspend fun sendSubstituteDelegation(
    distribusiId: String,
    dateIso: String,
    lessonSlotIds: List<String>,
    substituteTeacherId: String,
    note: String,
    teacherRowId: String
  ): GuruMapelAttendanceSaveResult = withContext(Dispatchers.IO) {
    val normalizedDate = dateIso.trim().take(10)
    val normalizedSlots = lessonSlotIds
      .map { it.cleanDbText() }
      .distinct()
      .ifEmpty { listOf("") }
    val guruAsalId = teacherRowId.trim()
    val penggantiId = substituteTeacherId.trim()

    if (distribusiId.isBlank() || normalizedDate.isBlank() || penggantiId.isBlank()) {
      return@withContext GuruMapelAttendanceSaveResult.Error("Data amanat guru pengganti belum lengkap.")
    }
    if (guruAsalId.isBlank()) {
      return@withContext GuruMapelAttendanceSaveResult.Error("Data guru login belum lengkap.")
    }
    if (guruAsalId == penggantiId) {
      return@withContext GuruMapelAttendanceSaveResult.Error("Guru pengganti tidak boleh sama dengan guru yang sedang login.")
    }

    val distribusiRow = fetchRows(
      table = "distribusi_mapel",
      query = "select=id,kelas_id,mapel_id,semester_id&id=eq.${encodeValue(distribusiId)}&limit=1"
    ).firstOrNull()
      ?: return@withContext GuruMapelAttendanceSaveResult.Error("Distribusi mapel tidak ditemukan.")

    return@withContext try {
      val payload = JSONArray()
      normalizedSlots.forEach { jamId ->
        payload.put(
          JSONObject().apply {
            put("tanggal", normalizedDate)
            put("kelas_id", distribusiRow.optString("kelas_id").trim())
            put("mapel_id", distribusiRow.optString("mapel_id").trim())
            put("distribusi_id", distribusiId)
            put("semester_id", distribusiRow.optString("semester_id").trim().ifBlank { JSONObject.NULL })
            put("jam_pelajaran_id", jamId.ifBlank { JSONObject.NULL })
            put("guru_asal_id", guruAsalId)
            put("guru_pengganti_id", penggantiId)
            put("keterangan", note.trim().ifBlank { JSONObject.NULL })
            put("status", "pending")
            put("filled_at", JSONObject.NULL)
          }
        )
      }

      val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/absensi_pengganti_tugas" +
        "?on_conflict=tanggal,kelas_id,mapel_id,jam_pelajaran_id,guru_asal_id"
      val connection = createConnection(requestUrl, "POST").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
      }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      connection.useEmptyResponse()
      GuruMapelAttendanceSaveResult.Success
    } catch (_: SocketTimeoutException) {
      GuruMapelAttendanceSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (error: Exception) {
      val message = error.message.orEmpty()
      GuruMapelAttendanceSaveResult.Error(
        when {
          message.contains("Could not find the table", ignoreCase = true) ->
            "Tabel amanat guru pengganti belum tersedia di Supabase."
          message.contains("no unique or exclusion constraint", ignoreCase = true) ->
            "Unique constraint amanat guru pengganti belum sesuai."
          else -> "Gagal mengirim amanat guru pengganti."
        }
      )
    }
  }

  private fun submitAttendanceSubstituteProposal(
    distribusiId: String,
    distribusiRow: JSONObject,
    changesByStudent: Map<String, List<AttendanceHistoryEntry>>,
    teacherRowId: String,
    teacherKaryawanId: String,
    sourceTeacherId: String,
    substituteNote: String
  ): GuruMapelAttendanceSaveResult {
    val currentTeacherId = teacherRowId.cleanDbText().ifBlank { teacherKaryawanId.cleanDbText() }
    if (currentTeacherId.isBlank()) {
      return GuruMapelAttendanceSaveResult.Error("Data guru pengganti belum lengkap.")
    }
    if (sourceTeacherId == currentTeacherId) {
      return GuruMapelAttendanceSaveResult.Error("Guru pengganti tidak boleh sama dengan guru utama.")
    }

    val allChanges = changesByStudent.values.flatten()
    val dates = allChanges
      .map { it.dateIso.take(10) }
      .filter { it.isNotBlank() }
      .distinct()
    if (dates.size != 1) {
      return GuruMapelAttendanceSaveResult.Error("Pengajuan absensi pengganti hanya bisa dikirim untuk satu tanggal sekali simpan.")
    }

    val lessonSlotIds = allChanges
      .map { it.lessonSlotId.cleanDbText() }
      .filter { it.isNotBlank() }
      .distinct()
    val statusByStudent = linkedMapOf<String, String>()
    changesByStudent.forEach { (studentId, changes) ->
      val normalizedStudentId = studentId.cleanDbText()
      if (normalizedStudentId.isBlank()) return@forEach
      val status = changes
        .firstOrNull { it.status.isNotBlank() }
        ?.status
        .orEmpty()
      statusByStudent[normalizedStudentId] = normalizeStatusLabel(status.ifBlank { "Hadir" })
    }
    if (statusByStudent.isEmpty()) {
      return GuruMapelAttendanceSaveResult.Error("Belum ada status absensi yang bisa diajukan.")
    }

    val santriMap = fetchSimpleNameMap("santri", "nama", statusByStudent.keys.toList())
    val payloadAbsensi = JSONArray()
    statusByStudent.entries
      .sortedBy { santriMap[it.key].orEmpty().lowercase() }
      .forEach { (studentId, status) ->
        payloadAbsensi.put(
          JSONObject().apply {
            put("santri_id", studentId)
            put("nama", santriMap[studentId].orEmpty().ifBlank { "-" })
            put("status", status)
          }
        )
      }

    val patronMateri = allChanges
      .map { it.patronMateri.cleanDbText() }
      .firstOrNull { it.isNotBlank() }
      .orEmpty()

    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/absensi_pengajuan_pengganti"
    val connection = createConnection(requestUrl, "POST").apply {
      doOutput = true
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Prefer", "return=representation")
    }
    val payload = JSONArray().put(
      JSONObject().apply {
        put("tanggal", dates.first())
        put("kelas_id", distribusiRow.optString("kelas_id").trim())
        put("mapel_id", distribusiRow.optString("mapel_id").trim())
        put("distribusi_id", distribusiId)
        put("semester_id", distribusiRow.optString("semester_id").trim().ifBlank { JSONObject.NULL })
        put("guru_asal_id", sourceTeacherId)
        put("guru_pengganti_id", currentTeacherId)
        put("jam_pelajaran_1_id", lessonSlotIds.getOrNull(0).orEmpty().ifBlank { JSONObject.NULL })
        put("jam_pelajaran_2_id", lessonSlotIds.getOrNull(1).orEmpty().ifBlank { JSONObject.NULL })
        put("patron_materi", patronMateri.ifBlank { JSONObject.NULL })
        put("keterangan", substituteNote.cleanDbText().ifBlank { JSONObject.NULL })
        put("payload_absensi", payloadAbsensi)
        put("status", "pending")
      }
    )
    connection.outputStream.use { stream ->
      stream.write(payload.toString().toByteArray(Charsets.UTF_8))
      stream.flush()
    }
    connection.useJsonArrayResponse { }

    return GuruMapelAttendanceSaveResult.SubmittedForReview(
      "Pengajuan absensi guru pengganti berhasil dikirim dan menunggu persetujuan guru utama."
    )
  }

  private fun fetchPendingDelegationRows(
    distribusiId: String,
    dates: List<String>,
    teacherRowId: String,
    teacherKaryawanId: String
  ): List<AttendanceDelegationRow> {
    val normalizedDistribusiId = distribusiId.trim()
    val normalizedDates = dates.map { it.trim().take(10) }.filter { it.isNotBlank() }.distinct()
    val teacherIds = listOf(teacherRowId, teacherKaryawanId)
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
    if (normalizedDistribusiId.isBlank() || normalizedDates.isEmpty() || teacherIds.isEmpty()) return emptyList()

    return teacherIds
      .flatMap { teacherId ->
        normalizedDates.flatMap { dateIso ->
          fetchRows(
            table = "absensi_pengganti_tugas",
            query = buildString {
              append("select=id,tanggal,distribusi_id,semester_id,jam_pelajaran_id,guru_asal_id,guru_pengganti_id,keterangan,status")
              append("&guru_pengganti_id=eq.")
              append(encodeValue(teacherId))
              append("&distribusi_id=eq.")
              append(encodeValue(normalizedDistribusiId))
              append("&tanggal=eq.")
              append(encodeValue(dateIso))
              append("&status=eq.pending")
            }
          )
        }
      }
      .mapNotNull { row ->
          val id = row.cleanString("id")
        if (id.isBlank()) return@mapNotNull null
        AttendanceDelegationRow(
          id = id,
          dateIso = row.cleanString("tanggal").take(10),
          lessonSlotId = row.cleanString("jam_pelajaran_id"),
          guruAsalId = row.cleanString("guru_asal_id"),
          semesterId = row.cleanString("semester_id"),
          note = row.cleanString("keterangan")
        )
      }
      .distinctBy { it.id }
  }

  private fun resolveDelegationRow(
    rows: List<AttendanceDelegationRow>,
    dateIso: String,
    lessonSlotId: String
  ): AttendanceDelegationRow? {
    val dateRows = rows.filter { it.dateIso == dateIso }
    if (dateRows.isEmpty()) return null
    return dateRows.firstOrNull { it.lessonSlotId == lessonSlotId } ?:
      dateRows.firstOrNull { it.lessonSlotId.isBlank() } ?:
      dateRows.takeIf { lessonSlotId.isBlank() && it.size == 1 }?.firstOrNull()
  }

  private fun upsertAttendanceRows(
    rows: JSONArray,
    onConflict: String
  ) {
    val insertUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/absensi_santri?on_conflict=$onConflict"
    val connection = createConnection(insertUrl, "POST").apply {
      doOutput = true
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
    }
    connection.outputStream.use { stream ->
      stream.write(rows.toString().toByteArray(Charsets.UTF_8))
      stream.flush()
    }
    connection.useEmptyResponse()
  }

  private fun markDelegationsDone(ids: List<String>) {
    val normalizedIds = ids.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    if (normalizedIds.isEmpty()) return
    runCatching {
      val inClause = normalizedIds.joinToString(",") { "\"${it}\"" }
      val updateUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/absensi_pengganti_tugas?id=in.($inClause)"
      val connection = createConnection(updateUrl, "PATCH").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Prefer", "return=minimal")
      }
      val payload = JSONObject().apply {
        put("status", "done")
        put("filled_at", Instant.now().toString())
      }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      connection.useEmptyResponse()
    }
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = createConnection(requestUrl, "GET")
    return try {
      if (connection.responseCode !in 200..299) return emptyList()
      val payload = connection.inputStream.bufferedReader().use(BufferedReader::readText)
      val rows = JSONArray(payload.ifBlank { "[]" })
      buildList {
        for (index in 0 until rows.length()) {
          val row = rows.optJSONObject(index) ?: continue
          add(row)
        }
      }
    } finally {
      connection.disconnect()
    }
  }

  private fun fetchAttendanceRowsWithOptionalMaterial(queryTail: String): List<JSONObject> {
    val selectWithMaterial = "select=id,tanggal,santri_id,status,kelas_id,mapel_id,semester_id,distribusi_id,patron_materi"
    val rowsWithMaterial = fetchRows(
      table = "absensi_santri",
      query = selectWithMaterial + queryTail
    )
    if (rowsWithMaterial.isNotEmpty()) return rowsWithMaterial

    // Fallback keeps older databases usable if patron_materi has not been added yet.
    return fetchRows(
      table = "absensi_santri",
      query = "select=id,tanggal,santri_id,status,kelas_id,mapel_id,semester_id,distribusi_id$queryTail"
    )
  }

  private fun normalizeSubmissionRow(row: JSONObject): AttendanceSubmissionRow {
    return AttendanceSubmissionRow(
      id = row.cleanString("id"),
      dateIso = row.cleanString("tanggal").take(10),
      kelasId = row.cleanString("kelas_id"),
      mapelId = row.cleanString("mapel_id"),
      distribusiId = row.cleanString("distribusi_id"),
      semesterId = row.cleanString("semester_id"),
      guruAsalId = row.cleanString("guru_asal_id"),
      guruPenggantiId = row.cleanString("guru_pengganti_id"),
      jamPelajaran1Id = row.cleanString("jam_pelajaran_1_id"),
      jamPelajaran2Id = row.cleanString("jam_pelajaran_2_id"),
      patronMateri = row.cleanString("patron_materi"),
      note = row.cleanString("keterangan"),
      status = row.cleanString("status").lowercase().ifBlank { "pending" },
      createdAt = row.cleanString("created_at"),
      payloadAbsensi = parseSubmissionPayload(row.opt("payload_absensi"))
    )
  }

  private fun parseSubmissionPayload(raw: Any?): List<AttendanceApprovalStudent> {
    val payloadArray = when (raw) {
      is JSONArray -> raw
      is String -> runCatching { JSONArray(raw) }.getOrNull()
      else -> null
    } ?: JSONArray()

    return buildList {
      for (index in 0 until payloadArray.length()) {
        val item = payloadArray.optJSONObject(index) ?: continue
        val studentId = item.cleanString("santri_id")
        if (studentId.isBlank()) continue
        add(
          AttendanceApprovalStudent(
            studentId = studentId,
            studentName = item.cleanString("nama").ifBlank {
              item.cleanString("santri_nama").ifBlank { "-" }
            },
            status = normalizeStatusLabel(item.cleanString("status").ifBlank { "Hadir" })
          )
        )
      }
    }
  }

  private fun String.cleanDbText(): String {
    val text = trim()
    return if (text.equals("null", ignoreCase = true)) "" else text
  }

  private fun JSONObject.cleanString(name: String): String {
    return opt(name)?.toString().orEmpty().cleanDbText()
  }

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
  }

  private fun fetchSimpleNameMap(
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

  private fun fetchSemesterLabelMap(ids: List<String>): Map<String, String> {
    if (ids.isEmpty()) return emptyMap()
    val inClause = ids.distinct().joinToString(",") { "\"${it}\"" }
    return fetchRows("semester", "select=id,nama,aktif&id=in.($inClause)")
      .associate { row ->
        val id = row.opt("id")?.toString().orEmpty().trim()
        val name = row.optString("nama").trim().ifBlank { "Semester" }
        id to name
      }
      .filterKeys { it.isNotBlank() }
  }

  private fun fetchJamLabelMap(ids: List<String>): Map<String, String> {
    if (ids.isEmpty()) return emptyMap()
    val inClause = ids.distinct().joinToString(",") { "\"${it}\"" }
    return fetchRows("jam_pelajaran", "select=id,nama,jam_mulai,jam_selesai&id=in.($inClause)")
      .associate { row ->
        val id = row.opt("id")?.toString().orEmpty().trim()
        val name = row.optString("nama").trim().ifBlank { "Jam" }
        val start = toTimeLabel(row.optString("jam_mulai"))
        val end = toTimeLabel(row.optString("jam_selesai"))
        val range = listOf(start, end).filter { it.isNotBlank() }.joinToString("-")
        id to if (range.isBlank()) name else "$name ($range)"
      }
      .filterKeys { it.isNotBlank() }
  }

  private fun buildHistoryEntries(rows: List<JSONObject>): List<AttendanceHistoryEntry> {
    val grouped = linkedMapOf<String, MutableList<String>>()
    val groupedIds = linkedMapOf<String, MutableList<String>>()
    val groupedMateri = linkedMapOf<String, MutableList<String>>()
    rows.forEach { row ->
      val tanggal = row.optString("tanggal").take(10)
      if (tanggal.isBlank()) return@forEach
      val statuses = grouped.getOrPut(tanggal) { mutableListOf() }
      val status = row.optString("status").trim().lowercase()
      if (status.isNotBlank()) statuses += status
      val ids = groupedIds.getOrPut(tanggal) { mutableListOf() }
      val id = row.opt("id")?.toString().orEmpty().trim()
      if (id.isNotBlank()) ids += id
      val materi = row.cleanString("patron_materi")
      if (materi.isNotBlank()) {
        val materiItems = groupedMateri.getOrPut(tanggal) { mutableListOf() }
        materiItems += materi
      }
    }
    return grouped.entries
      .map { (tanggal, statuses) ->
        AttendanceHistoryEntry(
          dateIso = tanggal,
          status = normalizeStatusLabel(
            when {
              statuses.any { it == "terlambat" } -> "terlambat"
              statuses.any { it == "hadir" } -> "hadir"
              statuses.any { it == "sakit" } -> "sakit"
              statuses.any { it == "izin" } -> "izin"
              statuses.any { it == "alpa" } -> "alpa"
              else -> ""
            }
          ),
          rowIds = groupedIds[tanggal].orEmpty().distinct(),
          patronMateri = groupedMateri[tanggal].orEmpty().firstOrNull().orEmpty()
        )
      }
      .sortedByDescending { it.dateIso }
  }

  private fun countStatuses(statuses: List<String>): Map<String, Int> {
    return statuses.groupingBy { it }.eachCount()
  }

  private fun percentage(count: Int, total: Int): Int {
    if (total <= 0) return 0
    return ((count.toDouble() / total.toDouble()) * 100.0).toInt()
  }

  private fun buildAttendanceRangeLabel(dates: List<String>): String {
    if (dates.isEmpty()) return "Belum ada tanggal absensi"
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
    val start = runCatching { LocalDate.parse(dates.first()) }.getOrNull()
    val end = runCatching { LocalDate.parse(dates.last()) }.getOrNull()
    if (start == null || end == null) return dates.last()
    return if (start == end) {
      formatter.format(start)
    } else {
      "${formatter.format(start)} - ${formatter.format(end)}"
    }
  }

  private fun parseTimestampMillis(value: String): Long {
    return runCatching { Instant.parse(value.cleanDbText()).toEpochMilli() }.getOrDefault(0L)
  }

  private fun normalizeStatusLabel(status: String): String {
    return when (status.trim().lowercase()) {
      "hadir" -> "Hadir"
      "terlambat" -> "Terlambat"
      "sakit" -> "Sakit"
      "izin" -> "Izin"
      "alpa" -> "Alpa"
      else -> status.trim()
    }
  }

  private fun toTimeLabel(value: String): String {
    val cleaned = value.trim()
    if (cleaned.isBlank()) return ""
    return cleaned.take(5)
  }

  private fun createConnection(requestUrl: String, method: String): HttpURLConnection {
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
}

private inline fun <T> HttpURLConnection.useJsonArrayResponse(
  block: (JSONArray) -> T
): T {
  return try {
    val code = responseCode
    val payload = readAttendancePayload(code in 200..299)
    if (code !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $code" })
    }
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.useEmptyResponse() {
  try {
    val code = responseCode
    if (code !in 200..299) {
      val payload = readAttendancePayload(false)
      throw IllegalStateException(payload.ifBlank { "HTTP $code" })
    }
    readAttendancePayload(true)
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readAttendancePayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}

private data class AttendanceDelegationRow(
  val id: String,
  val dateIso: String,
  val lessonSlotId: String,
  val guruAsalId: String,
  val semesterId: String,
  val note: String
)

private data class AttendanceSubmissionRow(
  val id: String,
  val dateIso: String,
  val kelasId: String,
  val mapelId: String,
  val distribusiId: String,
  val semesterId: String,
  val guruAsalId: String,
  val guruPenggantiId: String,
  val jamPelajaran1Id: String,
  val jamPelajaran2Id: String,
  val patronMateri: String,
  val note: String,
  val status: String,
  val createdAt: String,
  val payloadAbsensi: List<AttendanceApprovalStudent>
)
