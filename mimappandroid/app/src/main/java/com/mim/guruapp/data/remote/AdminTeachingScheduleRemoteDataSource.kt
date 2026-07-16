package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
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

data class AdminTeachingScheduleSnapshot(
  val schedules: List<AdminTeachingScheduleRow>,
  val lessonSlots: List<AdminLessonSlot>,
  val distributions: List<AdminTeachingScheduleDistribution>,
  val academicYears: List<AdminTeachingAcademicYear>,
  val semesters: List<AdminTeachingSemesterOption>,
  val classes: List<AdminTeachingClassOption>,
  val activeAcademicYearId: String = "",
  val activeAcademicYearName: String = "",
  val activeSemesterId: String = "",
  val activeSemesterName: String = ""
)

data class AdminTeachingScheduleRow(
  val rowId: String,
  val distributionId: String,
  val day: String,
  val lessonSlotId: String,
  val startTime: String,
  val endTime: String,
  val classId: String,
  val className: String,
  val classLevel: String,
  val subjectId: String,
  val subjectName: String,
  val subjectCategory: String,
  val teacherId: String,
  val teacherName: String,
  val semesterId: String,
  val semesterName: String,
  val academicYearId: String,
  val academicYearName: String
)

data class AdminLessonSlot(
  val rowId: String,
  val name: String,
  val startTime: String,
  val endTime: String,
  val order: String,
  val academicYearId: String,
  val academicYearName: String
)

data class AdminTeachingScheduleDistribution(
  val rowId: String,
  val classId: String,
  val className: String,
  val classLevel: String,
  val subjectId: String,
  val subjectName: String,
  val subjectCategory: String,
  val teacherId: String,
  val teacherName: String,
  val semesterId: String,
  val semesterName: String,
  val academicYearId: String,
  val academicYearName: String
)

data class AdminTeachingAcademicYear(
  val id: String,
  val name: String,
  val active: Boolean
)

data class AdminTeachingSemesterOption(
  val id: String,
  val name: String,
  val academicYearId: String,
  val academicYearName: String,
  val active: Boolean
)

data class AdminTeachingClassOption(
  val id: String,
  val name: String,
  val level: String,
  val academicYearId: String,
  val academicYearName: String
)

sealed interface AdminTeachingScheduleLoadResult {
  data class Success(val snapshot: AdminTeachingScheduleSnapshot) : AdminTeachingScheduleLoadResult
  data class Error(val message: String) : AdminTeachingScheduleLoadResult
}

sealed interface AdminTeachingScheduleSaveResult {
  data class Success(val snapshot: AdminTeachingScheduleSnapshot, val message: String) : AdminTeachingScheduleSaveResult
  data class Error(val message: String) : AdminTeachingScheduleSaveResult
}

class AdminTeachingScheduleRemoteDataSource {
  suspend fun fetchSnapshot(): AdminTeachingScheduleLoadResult = withContext(Dispatchers.IO) {
    try {
      AdminTeachingScheduleLoadResult.Success(loadSnapshot())
    } catch (_: SocketTimeoutException) {
      AdminTeachingScheduleLoadResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminTeachingScheduleLoadResult.Error("Gagal memuat jadwal pelajaran.")
    }
  }

  suspend fun saveSchedule(row: AdminTeachingScheduleRow): AdminTeachingScheduleSaveResult = withContext(Dispatchers.IO) {
    val distributionId = row.distributionId.trim()
    val day = row.day.normalizedAdminTeachingDay()
    val slotId = row.lessonSlotId.trim()
    if (distributionId.isBlank()) return@withContext AdminTeachingScheduleSaveResult.Error("Distribusi mapel wajib dipilih.")
    if (day.isBlank()) return@withContext AdminTeachingScheduleSaveResult.Error("Hari wajib dipilih.")
    if (slotId.isBlank()) return@withContext AdminTeachingScheduleSaveResult.Error("Jam pelajaran wajib dipilih.")

    try {
      val slot = fetchLessonSlots(emptyMap()).firstOrNull { it.rowId == slotId }
        ?: return@withContext AdminTeachingScheduleSaveResult.Error("Jam pelajaran tidak ditemukan. Muat ulang lalu pilih lagi.")
      val fields = linkedMapOf<String, Any?>(
        "distribusi_id" to distributionId,
        "hari" to day,
        "jam_mulai" to slot.startTime.toAdminTeachingTimeLabel(),
        "jam_selesai" to slot.endTime.toAdminTeachingTimeLabel(),
        "jam_pelajaran_id" to slot.rowId
      )
      writeScheduleRow(row.rowId.trim(), fields)
      AdminTeachingScheduleSaveResult.Success(
        loadSnapshot(),
        if (row.rowId.isBlank()) "Jadwal pelajaran berhasil ditambahkan." else "Jadwal pelajaran berhasil disimpan."
      )
    } catch (_: SocketTimeoutException) {
      AdminTeachingScheduleSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminTeachingScheduleSaveResult.Error("Gagal menyimpan jadwal pelajaran.")
    }
  }

  suspend fun deleteSchedule(rowId: String): AdminTeachingScheduleSaveResult = withContext(Dispatchers.IO) {
    val cleanId = rowId.trim()
    if (cleanId.isBlank()) return@withContext AdminTeachingScheduleSaveResult.Error("Jadwal belum dipilih.")
    try {
      deleteRowById("jadwal_pelajaran", cleanId)
      AdminTeachingScheduleSaveResult.Success(loadSnapshot(), "Jadwal pelajaran berhasil dihapus.")
    } catch (_: SocketTimeoutException) {
      AdminTeachingScheduleSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminTeachingScheduleSaveResult.Error("Gagal menghapus jadwal pelajaran.")
    }
  }

  suspend fun saveLessonSlot(slot: AdminLessonSlot): AdminTeachingScheduleSaveResult = withContext(Dispatchers.IO) {
    val name = slot.name.trim()
    val startTime = slot.startTime.toAdminTeachingTimeLabel()
    val endTime = slot.endTime.toAdminTeachingTimeLabel()
    if (name.isBlank()) return@withContext AdminTeachingScheduleSaveResult.Error("Nama jam wajib diisi.")
    if (startTime.isBlank() || endTime.isBlank()) return@withContext AdminTeachingScheduleSaveResult.Error("Jam mulai dan selesai wajib diisi.")
    if (endTime <= startTime) return@withContext AdminTeachingScheduleSaveResult.Error("Jam selesai harus lebih besar dari jam mulai.")

    try {
      val fields = linkedMapOf<String, Any?>(
        "nama" to name,
        "jam_mulai" to startTime,
        "jam_selesai" to endTime,
        "urutan" to slot.order.trim().toIntOrNull(),
        "tahun_ajaran_id" to slot.academicYearId.trim().ifBlank { null }
      )
      writeLessonSlot(slot.rowId.trim(), fields)
      AdminTeachingScheduleSaveResult.Success(
        loadSnapshot(),
        if (slot.rowId.isBlank()) "Jam pelajaran berhasil ditambahkan." else "Jam pelajaran berhasil disimpan."
      )
    } catch (_: SocketTimeoutException) {
      AdminTeachingScheduleSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminTeachingScheduleSaveResult.Error("Gagal menyimpan jam pelajaran.")
    }
  }

  suspend fun deleteLessonSlot(rowId: String): AdminTeachingScheduleSaveResult = withContext(Dispatchers.IO) {
    val cleanId = rowId.trim()
    if (cleanId.isBlank()) return@withContext AdminTeachingScheduleSaveResult.Error("Jam pelajaran belum dipilih.")
    try {
      deleteRowById("jam_pelajaran", cleanId)
      AdminTeachingScheduleSaveResult.Success(loadSnapshot(), "Jam pelajaran berhasil dihapus.")
    } catch (_: SocketTimeoutException) {
      AdminTeachingScheduleSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminTeachingScheduleSaveResult.Error("Gagal menghapus jam pelajaran. Pastikan slot tidak sedang dipakai jadwal.")
    }
  }

  private fun loadSnapshot(): AdminTeachingScheduleSnapshot {
    val years = fetchRows("tahun_ajaran", "select=*&order=id.desc")
      .mapNotNull { it.toAdminTeachingAcademicYear() }
      .sortedWith(
        compareByDescending<AdminTeachingAcademicYear> { it.active }
          .thenByDescending { it.name.firstYearForAdminTeaching() ?: 0 }
          .thenBy { it.name.lowercase() }
      )
    val activeYear = years.firstOrNull { it.active }
    val yearNameById = years.associate { it.id to it.name }

    val semesters = fetchRows("semester", "select=*&order=id.desc")
      .mapNotNull { it.toAdminTeachingSemester(yearNameById) }
      .sortedWith(
        compareByDescending<AdminTeachingSemesterOption> { it.active }
          .thenByDescending { yearNameById[it.academicYearId].orEmpty().firstYearForAdminTeaching() ?: 0 }
          .thenBy { it.name.lowercase() }
      )
    val activeSemester = semesters.firstOrNull { it.active && (activeYear == null || it.academicYearId == activeYear.id) }
      ?: semesters.firstOrNull { it.active }
    val semesterById = semesters.associateBy { it.id }

    val classes = fetchRows("kelas", "select=*&order=nama_kelas.asc")
      .mapNotNull { it.toAdminTeachingClass(yearNameById) }
      .sortedWith(
        compareBy<AdminTeachingClassOption> { it.level.adminTeachingLevelNumber() ?: 999 }
          .thenBy { it.name.lowercase() }
      )
    val classById = classes.associateBy { it.id }

    val teachers = fetchRows("karyawan", "select=*&order=nama.asc")
      .mapNotNull { it.toAdminTeachingTeacher() }
      .associateBy { it.id }
    val subjects = fetchRows("mapel", "select=*&order=nama.asc")
      .mapNotNull { it.toAdminTeachingSubject(yearNameById) }
      .associateBy { it.id }

    val distributions = fetchRows("distribusi_mapel", "select=*&order=id.desc")
      .mapNotNull { row ->
        row.toAdminTeachingDistribution(
          classById = classById,
          subjectById = subjects,
          teacherById = teachers,
          semesterById = semesterById,
          yearNameById = yearNameById
        )
      }
      .sortedWith(
        compareBy<AdminTeachingScheduleDistribution> { it.academicYearName.firstYearForAdminTeaching()?.unaryMinus() ?: 0 }
          .thenBy { it.classLevel.adminTeachingLevelNumber() ?: 999 }
          .thenBy { it.className.lowercase() }
          .thenBy { it.subjectName.lowercase() }
      )
    val distributionById = distributions.associateBy { it.rowId }

    val lessonSlots = fetchLessonSlots(yearNameById)
    val slotById = lessonSlots.associateBy { it.rowId }
    val slotByTime = lessonSlots
      .mapNotNull { slot ->
        val key = adminTeachingTimeKey(slot.startTime, slot.endTime)
        if (key.isBlank()) null else key to slot.rowId
      }
      .toMap()
    val schedules = fetchScheduleRows()
      .mapNotNull { row ->
        row.toAdminTeachingScheduleRow(
          distributionById = distributionById,
          slotById = slotById,
          slotByTime = slotByTime
        )
      }
      .sortedWith(
        compareBy<AdminTeachingScheduleRow> { it.academicYearName.firstYearForAdminTeaching()?.unaryMinus() ?: 0 }
          .thenBy { it.classLevel.adminTeachingLevelNumber() ?: 999 }
          .thenBy { it.className.lowercase() }
          .thenBy { it.day.adminTeachingDayOrder() }
          .thenBy { it.startTime }
          .thenBy { it.subjectName.lowercase() }
      )

    return AdminTeachingScheduleSnapshot(
      schedules = schedules,
      lessonSlots = lessonSlots,
      distributions = distributions,
      academicYears = years,
      semesters = semesters,
      classes = classes,
      activeAcademicYearId = activeYear?.id.orEmpty(),
      activeAcademicYearName = activeYear?.name.orEmpty(),
      activeSemesterId = activeSemester?.id.orEmpty(),
      activeSemesterName = activeSemester?.name.orEmpty()
    )
  }

  private fun fetchScheduleRows(): List<JSONObject> {
    return try {
      fetchRows("jadwal_pelajaran", "select=id,distribusi_id,hari,jam_mulai,jam_selesai,jam_pelajaran_id&order=hari.asc,jam_mulai.asc")
    } catch (error: Exception) {
      if (!error.message.orEmpty().isAdminTeachingMissingColumnMessage("jam_pelajaran_id")) throw error
      fetchRows("jadwal_pelajaran", "select=id,distribusi_id,hari,jam_mulai,jam_selesai&order=hari.asc,jam_mulai.asc")
    }
  }

  private fun fetchLessonSlots(yearNameById: Map<String, String>): List<AdminLessonSlot> {
    val rows = try {
      fetchRows("jam_pelajaran", "select=*&order=urutan.asc,jam_mulai.asc")
    } catch (error: Exception) {
      if (!error.message.orEmpty().isAdminTeachingMissingColumnMessage("urutan")) throw error
      fetchRows("jam_pelajaran", "select=*&order=jam_mulai.asc")
    }
    return rows
      .mapNotNull { it.toAdminLessonSlot(yearNameById) }
      .sortedWith(
        compareBy<AdminLessonSlot> { it.academicYearName.firstYearForAdminTeaching()?.unaryMinus() ?: 0 }
          .thenBy { it.order.toIntOrNull() ?: 999 }
          .thenBy { it.startTime }
      )
  }

  private fun writeScheduleRow(rowId: String, fields: Map<String, Any?>) {
    val candidates = listOf(
      fields,
      fields.minus("jam_pelajaran_id")
    ).distinctBy { it.keys.joinToString("|") }
    writeRowWithCandidates("jadwal_pelajaran", rowId, candidates)
  }

  private fun writeLessonSlot(rowId: String, fields: Map<String, Any?>) {
    val candidates = listOf(
      fields,
      fields.minus("urutan"),
      fields.minus("tahun_ajaran_id"),
      fields.minus("urutan").minus("tahun_ajaran_id")
    ).distinctBy { it.keys.joinToString("|") }
    writeRowWithCandidates("jam_pelajaran", rowId, candidates)
  }

  private fun writeRowWithCandidates(
    table: String,
    rowId: String,
    candidates: List<Map<String, Any?>>
  ) {
    var lastError: Exception? = null
    candidates.forEach { fields ->
      try {
        val baseUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?select=id"
        val requestUrl = if (rowId.isBlank()) baseUrl else "$baseUrl&id=eq.${encodeValue(rowId)}"
        val connection = createConnection(requestUrl, method = if (rowId.isBlank()) "POST" else "PATCH").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        val payload = JSONObject().apply {
          fields.forEach { (key, value) -> putPayloadValueForAdminTeaching(key, value) }
        }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        connection.useAdminTeachingJsonArrayResponse { rows ->
          if (rowId.isBlank() && rows.length() == 0) throw IllegalStateException("Data tidak kembali setelah disimpan.")
        }
        return
      } catch (error: Exception) {
        lastError = error
        if (!error.message.orEmpty().isAdminTeachingMissingColumnMessage()) throw error
      }
    }
    throw lastError ?: IllegalStateException("Gagal menyimpan data.")
  }

  private fun deleteRowById(table: String, rowId: String) {
    val connection = createConnection(
      "${BuildConfig.SUPABASE_URL}/rest/v1/$table?id=eq.${encodeValue(rowId)}",
      method = "DELETE"
    ).apply {
      setRequestProperty("Prefer", "return=minimal")
    }
    try {
      val status = connection.responseCode
      val payload = connection.readAdminTeachingPayload(status in 200..299)
      if (status !in 200..299) throw IllegalStateException(payload.ifBlank { "HTTP $status" })
    } finally {
      connection.disconnect()
    }
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = createConnection(requestUrl)
    return connection.useAdminTeachingJsonArrayResponse { rows ->
      List(rows.length()) { index -> rows.optJSONObject(index) }.filterNotNull()
    }
  }

  private fun createConnection(requestUrl: String, method: String = "GET"): HttpURLConnection {
    return (URL(requestUrl).openConnection() as HttpURLConnection).apply {
      requestMethod = method
      connectTimeout = 15_000
      readTimeout = 15_000
      applySupabaseRequestHeaders()
      setRequestProperty("Accept", "application/json")
      setRequestProperty("Accept-Charset", "UTF-8")
    }
  }

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
  }
}

private data class AdminTeachingTeacherOption(
  val id: String,
  val name: String
)

private data class AdminTeachingSubjectOption(
  val id: String,
  val name: String,
  val category: String,
  val academicYearId: String,
  val academicYearName: String
)

private fun JSONObject.toAdminTeachingAcademicYear(): AdminTeachingAcademicYear? {
  val id = cleanStringForAdminTeaching("id")
  if (id.isBlank()) return null
  return AdminTeachingAcademicYear(
    id = id,
    name = firstCleanStringForAdminTeaching("nama", "tahun_ajaran", "tahun").ifBlank { "Tahun Ajaran" },
    active = booleanFlexibleForAdminTeaching("aktif") ||
      booleanFlexibleForAdminTeaching("is_active") ||
      booleanFlexibleForAdminTeaching("active")
  )
}

private fun JSONObject.toAdminTeachingSemester(yearNameById: Map<String, String>): AdminTeachingSemesterOption? {
  val id = cleanStringForAdminTeaching("id")
  if (id.isBlank()) return null
  val yearId = firstCleanStringForAdminTeaching("tahun_ajaran_id", "tahunAjaranId")
  return AdminTeachingSemesterOption(
    id = id,
    name = firstCleanStringForAdminTeaching("nama", "nama_semester", "semester", "label").ifBlank { "Semester" },
    academicYearId = yearId,
    academicYearName = yearNameById[yearId].orEmpty(),
    active = booleanFlexibleForAdminTeaching("aktif") ||
      booleanFlexibleForAdminTeaching("is_active") ||
      booleanFlexibleForAdminTeaching("active")
  )
}

private fun JSONObject.toAdminTeachingClass(yearNameById: Map<String, String>): AdminTeachingClassOption? {
  val id = cleanStringForAdminTeaching("id")
  if (id.isBlank()) return null
  val name = firstCleanStringForAdminTeaching("nama_kelas", "kelas", "name").ifBlank { "Kelas" }
  val yearId = firstCleanStringForAdminTeaching("tahun_ajaran_id", "tahunAjaranId")
  return AdminTeachingClassOption(
    id = id,
    name = name,
    level = firstCleanStringForAdminTeaching("tingkat", "level").ifBlank { name.deriveAdminTeachingLevelFromName() },
    academicYearId = yearId,
    academicYearName = yearNameById[yearId].orEmpty()
  )
}

private fun JSONObject.toAdminTeachingTeacher(): AdminTeachingTeacherOption? {
  val id = cleanStringForAdminTeaching("id")
  val name = firstCleanStringForAdminTeaching("nama", "name")
  if (id.isBlank() || name.isBlank()) return null
  return AdminTeachingTeacherOption(id = id, name = name)
}

private fun JSONObject.toAdminTeachingSubject(yearNameById: Map<String, String>): AdminTeachingSubjectOption? {
  val id = cleanStringForAdminTeaching("id")
  if (id.isBlank()) return null
  val yearId = firstCleanStringForAdminTeaching("tahun_ajaran_id", "tahunAjaranId")
  return AdminTeachingSubjectOption(
    id = id,
    name = firstCleanStringForAdminTeaching("nama", "nama_mapel", "mapel").ifBlank { "Mapel" },
    category = firstCleanStringForAdminTeaching("kategori", "category"),
    academicYearId = yearId,
    academicYearName = yearNameById[yearId].orEmpty()
  )
}

private fun JSONObject.toAdminTeachingDistribution(
  classById: Map<String, AdminTeachingClassOption>,
  subjectById: Map<String, AdminTeachingSubjectOption>,
  teacherById: Map<String, AdminTeachingTeacherOption>,
  semesterById: Map<String, AdminTeachingSemesterOption>,
  yearNameById: Map<String, String>
): AdminTeachingScheduleDistribution? {
  val id = cleanStringForAdminTeaching("id")
  if (id.isBlank()) return null
  val classId = cleanStringForAdminTeaching("kelas_id")
  val subjectId = cleanStringForAdminTeaching("mapel_id")
  val teacherId = cleanStringForAdminTeaching("guru_id")
  val semesterId = cleanStringForAdminTeaching("semester_id")
  val kelas = classById[classId]
  val subject = subjectById[subjectId]
  val semester = semesterById[semesterId]
  val academicYearId = semester?.academicYearId.orEmpty()
    .ifBlank { kelas?.academicYearId.orEmpty() }
    .ifBlank { subject?.academicYearId.orEmpty() }
  return AdminTeachingScheduleDistribution(
    rowId = id,
    classId = classId,
    className = kelas?.name.orEmpty().ifBlank { "-" },
    classLevel = kelas?.level.orEmpty(),
    subjectId = subjectId,
    subjectName = subject?.name.orEmpty().ifBlank { "-" },
    subjectCategory = subject?.category.orEmpty(),
    teacherId = teacherId,
    teacherName = teacherById[teacherId]?.name.orEmpty(),
    semesterId = semesterId,
    semesterName = semester?.name.orEmpty().ifBlank { "-" },
    academicYearId = academicYearId,
    academicYearName = yearNameById[academicYearId].orEmpty().ifBlank { semester?.academicYearName.orEmpty() }
  )
}

private fun JSONObject.toAdminLessonSlot(yearNameById: Map<String, String>): AdminLessonSlot? {
  val id = cleanStringForAdminTeaching("id")
  if (id.isBlank()) return null
  val yearId = firstCleanStringForAdminTeaching("tahun_ajaran_id", "tahunAjaranId")
  val startTime = firstCleanStringForAdminTeaching("jam_mulai", "mulai", "start_time").toAdminTeachingTimeLabel()
  val endTime = firstCleanStringForAdminTeaching("jam_selesai", "selesai", "end_time").toAdminTeachingTimeLabel()
  return AdminLessonSlot(
    rowId = id,
    name = firstCleanStringForAdminTeaching("nama", "label", "name").ifBlank { "Jam Pelajaran" },
    startTime = startTime,
    endTime = endTime,
    order = firstCleanStringForAdminTeaching("urutan", "order", "position"),
    academicYearId = yearId,
    academicYearName = yearNameById[yearId].orEmpty()
  )
}

private fun JSONObject.toAdminTeachingScheduleRow(
  distributionById: Map<String, AdminTeachingScheduleDistribution>,
  slotById: Map<String, AdminLessonSlot>,
  slotByTime: Map<String, String>
): AdminTeachingScheduleRow? {
  val id = cleanStringForAdminTeaching("id")
  if (id.isBlank()) return null
  val distributionId = cleanStringForAdminTeaching("distribusi_id")
  val distribution = distributionById[distributionId] ?: return null
  val startTime = cleanStringForAdminTeaching("jam_mulai").toAdminTeachingTimeLabel()
  val endTime = cleanStringForAdminTeaching("jam_selesai").toAdminTeachingTimeLabel()
  val directSlotId = cleanStringForAdminTeaching("jam_pelajaran_id")
  val slotId = directSlotId.ifBlank { slotByTime[adminTeachingTimeKey(startTime, endTime)].orEmpty() }
  val slot = slotById[slotId]
  return AdminTeachingScheduleRow(
    rowId = id,
    distributionId = distributionId,
    day = cleanStringForAdminTeaching("hari").normalizedAdminTeachingDay(),
    lessonSlotId = slotId,
    startTime = startTime.ifBlank { slot?.startTime.orEmpty() },
    endTime = endTime.ifBlank { slot?.endTime.orEmpty() },
    classId = distribution.classId,
    className = distribution.className,
    classLevel = distribution.classLevel,
    subjectId = distribution.subjectId,
    subjectName = distribution.subjectName,
    subjectCategory = distribution.subjectCategory,
    teacherId = distribution.teacherId,
    teacherName = distribution.teacherName,
    semesterId = distribution.semesterId,
    semesterName = distribution.semesterName,
    academicYearId = distribution.academicYearId,
    academicYearName = distribution.academicYearName
  )
}

private inline fun <T> HttpURLConnection.useAdminTeachingJsonArrayResponse(
  block: (JSONArray) -> T
): T {
  return try {
    val status = responseCode
    val payload = readAdminTeachingPayload(status in 200..299)
    if (status !in 200..299) throw IllegalStateException(payload.ifBlank { "HTTP $status" })
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readAdminTeachingPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}

private fun JSONObject.cleanStringForAdminTeaching(key: String): String {
  val value = opt(key)
  if (value == null || value == JSONObject.NULL) return ""
  val text = value.toString().trim()
  return if (text.equals("null", ignoreCase = true)) "" else text
}

private fun JSONObject.firstCleanStringForAdminTeaching(vararg keys: String): String {
  return keys.firstNotNullOfOrNull { key ->
    cleanStringForAdminTeaching(key).takeIf { it.isNotBlank() }
  }.orEmpty()
}

private fun JSONObject.booleanFlexibleForAdminTeaching(key: String): Boolean {
  if (!has(key) || isNull(key)) return false
  val value = opt(key)
  if (value == true || value == 1) return true
  if (value == false || value == 0) return false
  return when (value?.toString().orEmpty().trim().lowercase()) {
    "true", "t", "1", "yes", "aktif", "active" -> true
    else -> false
  }
}

private fun JSONObject.putPayloadValueForAdminTeaching(key: String, value: Any?) {
  put(
    key,
    when {
      value is Boolean -> value
      value == null -> JSONObject.NULL
      value.toString().isBlank() -> JSONObject.NULL
      else -> value
    }
  )
}

private fun String.normalizedAdminTeachingDay(): String {
  return when (trim().lowercase()) {
    "senin", "monday", "mon", "1" -> "senin"
    "selasa", "tuesday", "tue", "2" -> "selasa"
    "rabu", "wednesday", "wed", "3" -> "rabu"
    "kamis", "thursday", "thu", "4" -> "kamis"
    "jumat", "jum'at", "friday", "fri", "5" -> "jumat"
    "sabtu", "saturday", "sat", "6" -> "sabtu"
    "minggu", "ahad", "sunday", "sun", "7" -> "minggu"
    else -> ""
  }
}

private fun String.adminTeachingDayOrder(): Int {
  return when (normalizedAdminTeachingDay()) {
    "senin" -> 1
    "selasa" -> 2
    "rabu" -> 3
    "kamis" -> 4
    "jumat" -> 5
    "sabtu" -> 6
    "minggu" -> 7
    else -> 99
  }
}

private fun String.toAdminTeachingTimeLabel(): String {
  val clean = trim()
  if (clean.isBlank()) return ""
  val match = Regex("\\d{1,2}:\\d{2}").find(clean)?.value.orEmpty()
  if (match.isBlank()) return clean.take(5)
  val parts = match.split(":")
  return "${parts.getOrNull(0).orEmpty().padStart(2, '0')}:${parts.getOrNull(1).orEmpty().padStart(2, '0')}"
}

private fun adminTeachingTimeKey(startTime: String, endTime: String): String {
  val start = startTime.toAdminTeachingTimeLabel()
  val end = endTime.toAdminTeachingTimeLabel()
  return if (start.isBlank() || end.isBlank()) "" else "$start|$end"
}

private fun String.adminTeachingLevelNumber(): Int? {
  val lowered = trim().lowercase()
  lowered.toIntOrNull()?.let { return it }
  return when {
    lowered.contains("viii") -> 8
    lowered.contains("vii") -> 7
    lowered.contains("ix") -> 9
    lowered.contains("xii") -> 12
    lowered.contains("xi") -> 11
    lowered.contains("x") -> 10
    else -> Regex("\\d+").find(lowered)?.value?.toIntOrNull()
  }
}

private fun String.deriveAdminTeachingLevelFromName(): String {
  return adminTeachingLevelNumber()?.toString().orEmpty()
}

private fun String.firstYearForAdminTeaching(): Int? {
  return Regex("\\b(19|20)\\d{2}\\b").find(this)?.value?.toIntOrNull()
}

private fun String.isAdminTeachingMissingColumnMessage(field: String = ""): Boolean {
  val lower = lowercase()
  if (!lower.contains("column") && !lower.contains("schema cache") && !lower.contains("pgrst")) return false
  return field.isBlank() || lower.contains(field.lowercase())
}
