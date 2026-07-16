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
import java.time.LocalDate

data class AdminKelasSnapshot(
  val classes: List<AdminKelas>,
  val academicYears: List<AdminKelasAcademicYear>,
  val teachers: List<AdminKelasTeacherOption>,
  val students: List<AdminKelasStudentOption>,
  val activeAcademicYearId: String = "",
  val activeAcademicYearName: String = ""
)

data class AdminKelas(
  val rowId: String,
  val name: String,
  val level: String,
  val academicYearId: String,
  val academicYearName: String,
  val homeroomTeacherId: String,
  val homeroomTeacherName: String,
  val studentCount: Int
)

data class AdminKelasAcademicYear(
  val id: String,
  val name: String,
  val active: Boolean
)

data class AdminKelasTeacherOption(
  val id: String,
  val employeeId: String,
  val name: String,
  val role: String,
  val active: Boolean
)

data class AdminKelasStudentOption(
  val rowId: String,
  val name: String,
  val nisn: String,
  val classId: String,
  val className: String,
  val classLevel: String,
  val active: Boolean,
  val status: String
)

sealed interface AdminKelasLoadResult {
  data class Success(val snapshot: AdminKelasSnapshot) : AdminKelasLoadResult
  data class Error(val message: String) : AdminKelasLoadResult
}

sealed interface AdminKelasSaveResult {
  data class Success(
    val kelas: AdminKelas,
    val message: String
  ) : AdminKelasSaveResult
  data class Error(val message: String) : AdminKelasSaveResult
}

sealed interface AdminKelasAssignStudentsResult {
  data class Success(
    val updatedCount: Int,
    val message: String
  ) : AdminKelasAssignStudentsResult
  data class Error(val message: String) : AdminKelasAssignStudentsResult
}

class AdminKelasRemoteDataSource {
  suspend fun fetchKelas(): AdminKelasLoadResult = withContext(Dispatchers.IO) {
    try {
      val yearRows = fetchRows("tahun_ajaran", "select=*&order=id.desc")
      val academicYears = yearRows.mapNotNull { it.toAcademicYear() }
      val activeYear = academicYears.firstOrNull { it.active }
      val yearNameById = academicYears.associate { it.id to it.name }

      val teachers = fetchRows(
        table = "karyawan",
        query = "select=*&order=nama.asc"
      ).mapNotNull { it.toTeacherOption() }
      val teacherNameById = teachers.associate { it.id to it.name }

      val classRows = fetchRows("kelas", "select=*&order=nama_kelas.asc")
      val rawClasses = classRows.mapNotNull { row ->
        row.toAdminKelas(
          yearNameById = yearNameById,
          teacherNameById = teacherNameById,
          studentCount = 0
        )
      }
      val classById = rawClasses.associateBy { it.rowId }

      val students = fetchRows("santri", "select=*&order=nama.asc")
        .mapNotNull { it.toStudentOption(classById) }
        .sortedWith(
          compareBy<AdminKelasStudentOption> { it.className.sortableTextForAdminKelas() }
            .thenBy { it.name.sortableTextForAdminKelas() }
        )
      val activeStudentCountByClass = students
        .filter { it.isActiveForClassList() }
        .groupingBy { it.classId }
        .eachCount()
      val classes = rawClasses
        .map { kelas -> kelas.copy(studentCount = activeStudentCountByClass[kelas.rowId] ?: 0) }
        .sortedWith(
          compareBy<AdminKelas> { it.level.levelNumberForAdminKelas() ?: 999 }
            .thenBy { it.name.sortableTextForAdminKelas() }
        )

      AdminKelasLoadResult.Success(
        AdminKelasSnapshot(
          classes = classes,
          academicYears = academicYears,
          teachers = teachers.sortedWith(compareBy { it.name.sortableTextForAdminKelas() }),
          students = students,
          activeAcademicYearId = activeYear?.id.orEmpty(),
          activeAcademicYearName = activeYear?.name.orEmpty()
        )
      )
    } catch (_: SocketTimeoutException) {
      AdminKelasLoadResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminKelasLoadResult.Error("Gagal memuat data kelas.")
    }
  }

  suspend fun saveKelas(kelas: AdminKelas): AdminKelasSaveResult = withContext(Dispatchers.IO) {
    val name = kelas.name.trim()
    val level = kelas.level.trim()
    val academicYearId = kelas.academicYearId.trim()
    val teacherId = kelas.homeroomTeacherId.trim()
    if (name.isBlank()) return@withContext AdminKelasSaveResult.Error("Nama kelas wajib diisi.")
    if (level.isBlank()) return@withContext AdminKelasSaveResult.Error("Tingkat kelas wajib diisi.")
    if (academicYearId.isBlank()) return@withContext AdminKelasSaveResult.Error("Tahun ajaran wajib dipilih.")

    try {
      val saved = if (kelas.rowId.isBlank()) {
        insertKelas(
          linkedMapOf(
            "nama_kelas" to name,
            "tingkat" to level,
            "tahun_ajaran_id" to academicYearId,
            "wali_kelas_id" to teacherId.ifBlank { null }
          )
        )
      } else {
        val savedFields = patchKelasFields(
          rowId = kelas.rowId,
          fields = linkedMapOf(
            "nama_kelas" to name,
            "tingkat" to level,
            "tahun_ajaran_id" to academicYearId
          )
        ).toMutableMap()
        savedFields.putAll(
          patchKelasHomeroomField(
            rowId = kelas.rowId,
            teacherId = teacherId.ifBlank { null }
          )
        )
        kelas.copy(
          name = savedFields.cleanTextForAdminKelas("nama_kelas", name),
          level = savedFields.cleanTextForAdminKelas("tingkat", level),
          academicYearId = savedFields.cleanTextForAdminKelas("tahun_ajaran_id", academicYearId),
          homeroomTeacherId = savedFields.cleanTextForAdminKelas("wali_kelas_id", teacherId)
            .ifBlank { savedFields.cleanTextForAdminKelas("wali_id", teacherId) }
        )
      }

      AdminKelasSaveResult.Success(
        saved,
        if (kelas.rowId.isBlank()) "Kelas berhasil ditambahkan." else "Kelas berhasil disimpan."
      )
    } catch (_: SocketTimeoutException) {
      AdminKelasSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminKelasSaveResult.Error("Gagal menyimpan kelas.")
    }
  }

  suspend fun assignStudentsToKelas(
    kelas: AdminKelas,
    studentIds: List<String>
  ): AdminKelasAssignStudentsResult = withContext(Dispatchers.IO) {
    val classId = kelas.rowId.trim()
    val normalizedStudentIds = studentIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    if (classId.isBlank()) {
      return@withContext AdminKelasAssignStudentsResult.Error("Kelas harus disimpan dulu sebelum menambahkan santri.")
    }
    if (normalizedStudentIds.isEmpty()) {
      return@withContext AdminKelasAssignStudentsResult.Error("Pilih minimal satu santri.")
    }

    try {
      var updatedCount = 0
      normalizedStudentIds.forEach { studentId ->
        val savedFields = patchSantriFields(
          rowId = studentId,
          fields = linkedMapOf(
            "kelas_id" to classId,
            "status" to "aktif",
            "aktif" to true
          )
        )
        if (savedFields.containsKey("kelas_id")) {
          updatedCount += 1
          upsertClassHistoryRow(
            linkedMapOf(
              "santri_id" to studentId,
              "kelas_id" to classId,
              "tahun_ajaran_id" to kelas.academicYearId.ifBlank { null },
              "status" to "aktif",
              "tanggal_mulai" to LocalDate.now().toString(),
              "tanggal_selesai" to null
            )
          )
        }
      }
      if (updatedCount == 0) {
        return@withContext AdminKelasAssignStudentsResult.Error("Santri belum bisa ditambahkan ke kelas.")
      }
      AdminKelasAssignStudentsResult.Success(
        updatedCount = updatedCount,
        message = "$updatedCount santri berhasil dimasukkan ke ${kelas.name}."
      )
    } catch (_: SocketTimeoutException) {
      AdminKelasAssignStudentsResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminKelasAssignStudentsResult.Error("Gagal menambahkan santri ke kelas.")
    }
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = createConnection(requestUrl)
    return connection.useAdminKelasJsonArrayResponse { rows ->
      List(rows.length()) { index -> rows.optJSONObject(index) }.filterNotNull()
    }
  }

  private fun insertKelas(fields: Map<String, Any?>): AdminKelas {
    val candidates = listOf(
      fields,
      fields.replaceKeyForAdminKelas("wali_kelas_id", "wali_id"),
      fields - "wali_kelas_id"
    ).distinctBy { it.keys.joinToString("|") }

    var lastError: Exception? = null
    candidates.forEach { candidate ->
      try {
        val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/kelas?select=*"
        val connection = createConnection(requestUrl, method = "POST").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        val payload = JSONObject().apply {
          candidate.forEach { (field, value) -> putPayloadValueForAdminKelas(field, value) }
        }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        return connection.useAdminKelasJsonArrayResponse { rows ->
          rows.optJSONObject(0)?.toAdminKelas(
            yearNameById = emptyMap(),
            teacherNameById = emptyMap(),
            studentCount = 0
          ) ?: throw IllegalStateException("Data kelas tidak ditemukan setelah disimpan.")
        }
      } catch (error: Exception) {
        lastError = error
        if (!error.message.orEmpty().isAdminKelasMissingColumnMessage()) throw error
      }
    }
    throw lastError ?: IllegalStateException("Gagal menambahkan kelas.")
  }

  private fun patchKelasFields(rowId: String, fields: Map<String, Any?>): Map<String, Any?> {
    val savedFields = linkedMapOf<String, Any?>()
    fields.forEach { (field, value) ->
      try {
        val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/kelas" +
          "?select=$field&id=eq.${encodeValue(rowId)}"
        val connection = createConnection(requestUrl, method = "PATCH").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        val payload = JSONObject().apply { putPayloadValueForAdminKelas(field, value) }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        val savedValue = connection.useAdminKelasJsonArrayResponse { rows ->
          rows.optJSONObject(0)?.opt(field)
        }
        savedFields[field] = savedValue ?: value
      } catch (error: Exception) {
        if (!error.message.orEmpty().isAdminKelasMissingColumnMessage(field)) throw error
      }
    }
    return savedFields
  }

  private fun patchKelasHomeroomField(rowId: String, teacherId: String?): Map<String, Any?> {
    val primary = patchKelasFields(rowId, mapOf("wali_kelas_id" to teacherId))
    if (primary.containsKey("wali_kelas_id")) return primary
    return patchKelasFields(rowId, mapOf("wali_id" to teacherId))
  }

  private fun patchSantriFields(rowId: String, fields: Map<String, Any?>): Map<String, Any?> {
    val savedFields = linkedMapOf<String, Any?>()
    fields.forEach { (field, value) ->
      try {
        val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/santri" +
          "?select=$field&id=eq.${encodeValue(rowId)}"
        val connection = createConnection(requestUrl, method = "PATCH").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        val payload = JSONObject().apply { putPayloadValueForAdminKelas(field, value) }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        val savedValue = connection.useAdminKelasJsonArrayResponse { rows ->
          rows.optJSONObject(0)?.opt(field)
        }
        savedFields[field] = savedValue ?: value
      } catch (error: Exception) {
        if (!error.message.orEmpty().isAdminKelasMissingColumnMessage(field)) throw error
      }
    }
    return savedFields
  }

  private fun upsertClassHistoryRow(fields: Map<String, Any?>): Boolean {
    return runCatching {
      val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/riwayat_kelas_santri" +
        "?on_conflict=santri_id,kelas_id,tahun_ajaran_id&select=id"
      val connection = createConnection(requestUrl, method = "POST").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
      }
      val payload = JSONObject().apply {
        fields.forEach { (field, value) -> putPayloadValueForAdminKelas(field, value) }
      }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      connection.useAdminKelasJsonArrayResponse { rows -> rows.length() > 0 }
    }.getOrDefault(false)
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

private fun JSONObject.toAcademicYear(): AdminKelasAcademicYear? {
  val id = optCleanStringForAdminKelas("id")
  if (id.isBlank()) return null
  return AdminKelasAcademicYear(
    id = id,
    name = optCleanStringForAdminKelas("nama")
      .ifBlank { optCleanStringForAdminKelas("tahun_ajaran") }
      .ifBlank { optCleanStringForAdminKelas("tahun") },
    active = optBooleanFlexibleForAdminKelas("aktif")
  )
}

private fun JSONObject.toTeacherOption(): AdminKelasTeacherOption? {
  val id = optCleanStringForAdminKelas("id")
  val name = optCleanStringForAdminKelas("nama")
  if (id.isBlank() || name.isBlank()) return null
  return AdminKelasTeacherOption(
    id = id,
    employeeId = optCleanStringForAdminKelas("id_karyawan"),
    name = name,
    role = optCleanStringForAdminKelas("role"),
    active = optBooleanFlexibleForAdminKelas("aktif", default = true)
  )
}

private fun JSONObject.toAdminKelas(
  yearNameById: Map<String, String>,
  teacherNameById: Map<String, String>,
  studentCount: Int
): AdminKelas? {
  val id = optCleanStringForAdminKelas("id")
  if (id.isBlank()) return null
  val name = optCleanStringForAdminKelas("nama_kelas").ifBlank { optCleanStringForAdminKelas("kelas") }
  val academicYearId = optCleanStringForAdminKelas("tahun_ajaran_id")
  val homeroomId = optCleanStringForAdminKelas("wali_kelas_id")
    .ifBlank { optCleanStringForAdminKelas("wali_id") }
    .ifBlank { optCleanStringForAdminKelas("guru_id") }
  return AdminKelas(
    rowId = id,
    name = name.ifBlank { "Kelas" },
    level = optCleanStringForAdminKelas("tingkat").ifBlank { name.deriveLevelForAdminKelas() },
    academicYearId = academicYearId,
    academicYearName = yearNameById[academicYearId].orEmpty(),
    homeroomTeacherId = homeroomId,
    homeroomTeacherName = teacherNameById[homeroomId].orEmpty()
      .ifBlank { optCleanStringForAdminKelas("wali_kelas") },
    studentCount = studentCount
  )
}

private fun JSONObject.toStudentOption(classById: Map<String, AdminKelas>): AdminKelasStudentOption? {
  val rowId = optCleanStringForAdminKelas("id")
  val name = optCleanStringForAdminKelas("nama").ifBlank { optCleanStringForAdminKelas("nama_santri") }
  if (rowId.isBlank() || name.isBlank()) return null
  val classId = optCleanStringForAdminKelas("kelas_id")
  val kelas = classById[classId]
  val explicitStatus = optCleanStringForAdminKelas("status")
  val active = optBooleanFlexibleOrNullForAdminKelas("aktif") ?: !explicitStatus.isInactiveStudentStatusForAdminKelas()
  return AdminKelasStudentOption(
    rowId = rowId,
    name = name,
    nisn = optCleanStringForAdminKelas("nisn"),
    classId = classId,
    className = kelas?.name.orEmpty(),
    classLevel = kelas?.level.orEmpty(),
    active = active,
    status = explicitStatus.ifBlank { if (active) "aktif" else "tidak_aktif" }
  )
}

private inline fun <T> HttpURLConnection.useAdminKelasJsonArrayResponse(
  block: (JSONArray) -> T
): T {
  return try {
    val status = responseCode
    val payload = readAdminKelasPayload(status in 200..299)
    if (status !in 200..299) throw IllegalStateException(payload.ifBlank { "HTTP $status" })
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readAdminKelasPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}

private fun JSONObject.optCleanStringForAdminKelas(key: String): String {
  val value = opt(key)
  if (value == null || value == JSONObject.NULL) return ""
  val text = value.toString().trim()
  return if (text.equals("null", ignoreCase = true)) "" else text
}

private fun JSONObject.optBooleanFlexibleForAdminKelas(key: String, default: Boolean = false): Boolean {
  return optBooleanFlexibleOrNullForAdminKelas(key) ?: default
}

private fun JSONObject.optBooleanFlexibleOrNullForAdminKelas(key: String): Boolean? {
  if (!has(key) || isNull(key)) return null
  val value = opt(key)
  if (value == true || value == 1) return true
  if (value == false || value == 0) return false
  return when (value?.toString().orEmpty().trim().lowercase()) {
    "true", "t", "1", "yes", "aktif", "active" -> true
    "false", "f", "0", "no", "tidak_aktif", "nonaktif", "inactive", "lulus" -> false
    else -> null
  }
}

private fun JSONObject.putPayloadValueForAdminKelas(key: String, value: Any?) {
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

private fun Map<String, Any?>.replaceKeyForAdminKelas(oldKey: String, newKey: String): Map<String, Any?> {
  if (!containsKey(oldKey)) return this
  return LinkedHashMap<String, Any?>().apply {
    this@replaceKeyForAdminKelas.forEach { (key, value) ->
      put(if (key == oldKey) newKey else key, value)
    }
  }
}

private fun Map<String, Any?>.cleanTextForAdminKelas(key: String, fallback: String): String {
  if (!containsKey(key)) return fallback
  val value = this[key]
  if (value == null || value == JSONObject.NULL) return ""
  val text = value.toString().trim()
  return if (text.equals("null", ignoreCase = true)) "" else text
}

private fun String.isAdminKelasMissingColumnMessage(field: String = ""): Boolean {
  val lower = lowercase()
  if (!lower.contains("column") && !lower.contains("pgrst")) return false
  return field.isBlank() || lower.contains(field.lowercase())
}

private fun String.sortableTextForAdminKelas(): String {
  return if (isBlank()) "zzzz" else trim().lowercase()
}

private fun String.levelNumberForAdminKelas(): Int? {
  val trimmed = trim()
  if (trimmed.isBlank()) return null
  trimmed.toIntOrNull()?.let { return it }
  return trimmed.filter { it.isDigit() }.toIntOrNull()
}

private fun String.deriveLevelForAdminKelas(): String {
  val text = trim()
  val roman = Regex("\\b(XII|XI|X|IX|VIII|VII|VI|V|IV|III|II|I)\\b", RegexOption.IGNORE_CASE)
    .find(text)
    ?.value
    ?.uppercase()
  if (!roman.isNullOrBlank()) {
    return when (roman) {
      "I" -> "1"
      "II" -> "2"
      "III" -> "3"
      "IV" -> "4"
      "V" -> "5"
      "VI" -> "6"
      "VII" -> "7"
      "VIII" -> "8"
      "IX" -> "9"
      "X" -> "10"
      "XI" -> "11"
      "XII" -> "12"
      else -> ""
    }
  }
  return Regex("\\d+").find(text)?.value.orEmpty()
}

private fun String.isInactiveStudentStatusForAdminKelas(): Boolean {
  return when (trim().lowercase()) {
    "tidak_aktif", "nonaktif", "inactive", "lulus" -> true
    else -> false
  }
}

private fun AdminKelasStudentOption.isActiveForClassList(): Boolean {
  return active && !status.isInactiveStudentStatusForAdminKelas()
}
