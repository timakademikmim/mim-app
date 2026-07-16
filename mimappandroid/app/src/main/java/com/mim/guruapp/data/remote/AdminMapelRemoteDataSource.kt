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

data class AdminMapelSnapshot(
  val subjects: List<AdminMapelSubject>,
  val distributions: List<AdminMapelDistribution>,
  val academicYears: List<AdminMapelAcademicYear>,
  val semesters: List<AdminMapelSemesterOption>,
  val classes: List<AdminMapelClassOption>,
  val teachers: List<AdminMapelTeacherOption>,
  val activeAcademicYearId: String = "",
  val activeAcademicYearName: String = "",
  val activeSemesterId: String = "",
  val activeSemesterName: String = ""
)

data class AdminMapelSubject(
  val rowId: String,
  val name: String,
  val category: String,
  val levels: String,
  val academicYearId: String,
  val academicYearName: String,
  val kkm: String,
  val distributionCount: Int
)

data class AdminMapelDistribution(
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
  val academicYearName: String,
  val level: String
)

data class AdminMapelAcademicYear(
  val id: String,
  val name: String,
  val active: Boolean
)

data class AdminMapelSemesterOption(
  val id: String,
  val name: String,
  val academicYearId: String,
  val academicYearName: String,
  val active: Boolean
)

data class AdminMapelClassOption(
  val id: String,
  val name: String,
  val level: String,
  val academicYearId: String,
  val academicYearName: String
)

data class AdminMapelTeacherOption(
  val id: String,
  val employeeId: String,
  val name: String,
  val role: String,
  val active: Boolean
)

sealed interface AdminMapelLoadResult {
  data class Success(val snapshot: AdminMapelSnapshot) : AdminMapelLoadResult
  data class Error(val message: String) : AdminMapelLoadResult
}

sealed interface AdminMapelSaveResult {
  data class Success(val snapshot: AdminMapelSnapshot, val message: String) : AdminMapelSaveResult
  data class Error(val message: String) : AdminMapelSaveResult
}

class AdminMapelRemoteDataSource {
  suspend fun fetchSnapshot(): AdminMapelLoadResult = withContext(Dispatchers.IO) {
    try {
      AdminMapelLoadResult.Success(loadSnapshot())
    } catch (_: SocketTimeoutException) {
      AdminMapelLoadResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminMapelLoadResult.Error("Gagal memuat data mapel.")
    }
  }

  suspend fun saveSubject(subject: AdminMapelSubject): AdminMapelSaveResult = withContext(Dispatchers.IO) {
    val name = subject.name.trim()
    val category = subject.category.trim()
    val levels = subject.levels.normalizedAdminMapelLevels()
    val academicYearId = subject.academicYearId.trim()
    val kkm = subject.kkm.trim()
    if (name.isBlank()) return@withContext AdminMapelSaveResult.Error("Nama mapel wajib diisi.")
    if (levels.isBlank()) return@withContext AdminMapelSaveResult.Error("Pilih minimal satu tingkatan mapel.")
    if (academicYearId.isBlank()) return@withContext AdminMapelSaveResult.Error("Tahun ajaran wajib dipilih.")

    try {
      val savedId = if (subject.rowId.isBlank()) {
        insertSubject(name, category, levels, academicYearId, kkm)
      } else {
        updateSubject(subject.rowId, name, category, levels, academicYearId, kkm)
        subject.rowId
      }
      autoCreateDistributionsForSubject(savedId, levels, academicYearId)
      AdminMapelSaveResult.Success(
        loadSnapshot(),
        if (subject.rowId.isBlank()) "Mapel berhasil ditambahkan." else "Mapel berhasil disimpan."
      )
    } catch (_: SocketTimeoutException) {
      AdminMapelSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminMapelSaveResult.Error("Gagal menyimpan mapel.")
    }
  }

  suspend fun saveDistribution(distribution: AdminMapelDistribution): AdminMapelSaveResult = withContext(Dispatchers.IO) {
    val classId = distribution.classId.trim()
    val subjectId = distribution.subjectId.trim()
    val semesterId = distribution.semesterId.trim()
    val teacherId = distribution.teacherId.trim()
    val level = distribution.level.normalizedAdminMapelLevel()
      .ifBlank { distribution.classLevel.toAdminMapelStage() }
    if (classId.isBlank()) return@withContext AdminMapelSaveResult.Error("Kelas wajib dipilih.")
    if (subjectId.isBlank()) return@withContext AdminMapelSaveResult.Error("Mapel wajib dipilih.")
    if (semesterId.isBlank()) return@withContext AdminMapelSaveResult.Error("Semester wajib dipilih.")

    try {
      if (distribution.rowId.isBlank()) {
        insertDistribution(
          linkedMapOf(
            "kelas_id" to classId,
            "mapel_id" to subjectId,
            "guru_id" to teacherId.ifBlank { null },
            "semester_id" to semesterId,
            "tingkatan" to level.ifBlank { null }
          )
        )
      } else {
        patchRowFields(
          table = "distribusi_mapel",
          rowId = distribution.rowId,
          fields = linkedMapOf(
            "kelas_id" to classId,
            "mapel_id" to subjectId,
            "guru_id" to teacherId.ifBlank { null },
            "semester_id" to semesterId,
            "tingkatan" to level.ifBlank { null }
          ),
          ignoreMissingColumns = true
        )
      }
      AdminMapelSaveResult.Success(
        loadSnapshot(),
        if (distribution.rowId.isBlank()) "Distribusi mapel berhasil ditambahkan." else "Distribusi mapel berhasil disimpan."
      )
    } catch (_: SocketTimeoutException) {
      AdminMapelSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminMapelSaveResult.Error("Gagal menyimpan distribusi mapel.")
    }
  }

  private fun loadSnapshot(): AdminMapelSnapshot {
    val academicYears = fetchRows("tahun_ajaran", "select=*&order=id.desc")
      .mapNotNull { it.toAdminMapelAcademicYear() }
      .sortedWith(
        compareByDescending<AdminMapelAcademicYear> { it.active }
          .thenByDescending { it.name.firstYearForAdminMapel() ?: 0 }
          .thenBy { it.name.lowercase() }
      )
    val activeYear = academicYears.firstOrNull { it.active }
    val yearNameById = academicYears.associate { it.id to it.name }

    val semesters = fetchRows("semester", "select=*&order=id.desc")
      .mapNotNull { it.toAdminMapelSemester(yearNameById) }
      .sortedWith(
        compareByDescending<AdminMapelSemesterOption> { it.active }
          .thenByDescending { yearNameById[it.academicYearId].orEmpty().firstYearForAdminMapel() ?: 0 }
          .thenBy { it.name.lowercase() }
      )
    val activeSemester = semesters.firstOrNull { it.active && (activeYear == null || it.academicYearId == activeYear.id) }
      ?: semesters.firstOrNull { it.active }

    val classes = fetchRows("kelas", "select=*&order=nama_kelas.asc")
      .mapNotNull { it.toAdminMapelClass(yearNameById) }
      .sortedWith(
        compareBy<AdminMapelClassOption> { it.level.toAdminMapelLevelNumber() ?: 999 }
          .thenBy { it.name.lowercase() }
      )
    val classById = classes.associateBy { it.id }

    val teachers = fetchRows("karyawan", "select=*&order=nama.asc")
      .mapNotNull { it.toAdminMapelTeacher() }
      .sortedWith(compareBy { it.name.lowercase() })
    val teacherById = teachers.associateBy { it.id }

    val rawSubjects = fetchRows("mapel", "select=*&order=nama.asc")
      .mapNotNull { it.toAdminMapelSubject(yearNameById, 0) }
      .sortedWith(compareBy<AdminMapelSubject> { it.name.lowercase() }.thenBy { it.category.lowercase() })
    val subjectById = rawSubjects.associateBy { it.rowId }

    val distributions = fetchRows("distribusi_mapel", "select=*&order=id.desc")
      .mapNotNull { row ->
        row.toAdminMapelDistribution(
          classById = classById,
          subjectById = subjectById,
          teacherById = teacherById,
          semesterById = semesters.associateBy { it.id },
          yearNameById = yearNameById
        )
      }
      .sortedWith(
        compareBy<AdminMapelDistribution> { it.academicYearName.firstYearForAdminMapel()?.unaryMinus() ?: 0 }
          .thenBy { it.classLevel.toAdminMapelLevelNumber() ?: 999 }
          .thenBy { it.className.lowercase() }
          .thenBy { it.subjectName.lowercase() }
      )

    val distributionCountBySubject = distributions.groupingBy { it.subjectId }.eachCount()
    val subjects = rawSubjects.map { subject ->
      subject.copy(distributionCount = distributionCountBySubject[subject.rowId] ?: 0)
    }

    return AdminMapelSnapshot(
      subjects = subjects,
      distributions = distributions,
      academicYears = academicYears,
      semesters = semesters,
      classes = classes,
      teachers = teachers,
      activeAcademicYearId = activeYear?.id.orEmpty(),
      activeAcademicYearName = activeYear?.name.orEmpty(),
      activeSemesterId = activeSemester?.id.orEmpty(),
      activeSemesterName = activeSemester?.name.orEmpty()
    )
  }

  private fun insertSubject(
    name: String,
    category: String,
    levels: String,
    academicYearId: String,
    kkm: String
  ): String {
    val firstLevel = levels.firstAdminMapelLevel()
    val base = linkedMapOf<String, Any?>(
      "nama" to name,
      "tingkatan" to levels,
      "tingkatan_multi" to levels,
      "kategori" to category.ifBlank { null },
      "tahun_ajaran_id" to academicYearId,
      "kkm" to kkm.toIntOrNull()
    )
    val candidates = listOf(
      base,
      base.minusAdminMapelKeys("kkm"),
      base + ("tingkatan" to firstLevel),
      (base + ("tingkatan" to firstLevel)).minusAdminMapelKeys("kkm"),
      base.minusAdminMapelKeys("tingkatan_multi", "kkm"),
      (base.minusAdminMapelKeys("tingkatan_multi", "kkm") + ("tingkatan" to firstLevel)),
      base.minusAdminMapelKeys("tahun_ajaran_id", "kkm"),
      (base.minusAdminMapelKeys("tahun_ajaran_id", "tingkatan_multi", "kkm") + ("tingkatan" to firstLevel))
    ).distinctBy { it.keys.joinToString("|") + it["tingkatan"].toString() }

    var lastError: Exception? = null
    candidates.forEach { fields ->
      try {
        val connection = createConnection("${BuildConfig.SUPABASE_URL}/rest/v1/mapel?select=id", method = "POST").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        val payload = JSONObject().apply {
          fields.forEach { (key, value) -> putPayloadValueForAdminMapel(key, value) }
        }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        return connection.useAdminMapelJsonArrayResponse { rows ->
          rows.optJSONObject(0)?.cleanStringForAdminMapel("id")
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Data mapel tidak kembali setelah disimpan.")
        }
      } catch (error: Exception) {
        lastError = error
        val message = error.message.orEmpty()
        if (!message.isAdminMapelMissingColumnMessage() && !message.isAdminMapelCheckConstraintMessage()) throw error
      }
    }
    throw lastError ?: IllegalStateException("Gagal menambahkan mapel.")
  }

  private fun updateSubject(
    rowId: String,
    name: String,
    category: String,
    levels: String,
    academicYearId: String,
    kkm: String
  ) {
    patchFirstSupportedField("mapel", rowId, listOf("nama" to name, "nama_mapel" to name, "mapel" to name))
    patchRowFields("mapel", rowId, mapOf("kategori" to category.ifBlank { null }), ignoreMissingColumns = true)
    patchSubjectLevels(rowId, levels)
    patchRowFields("mapel", rowId, mapOf("tahun_ajaran_id" to academicYearId), ignoreMissingColumns = true)
    patchRowFields("mapel", rowId, mapOf("kkm" to kkm.toIntOrNull()), ignoreMissingColumns = true)
  }

  private fun patchSubjectLevels(rowId: String, levels: String) {
    patchRowFields("mapel", rowId, mapOf("tingkatan_multi" to levels), ignoreMissingColumns = true)
    try {
      patchRowFields("mapel", rowId, mapOf("tingkatan" to levels), ignoreMissingColumns = true)
    } catch (error: Exception) {
      if (!error.message.orEmpty().isAdminMapelCheckConstraintMessage()) throw error
      patchRowFields("mapel", rowId, mapOf("tingkatan" to levels.firstAdminMapelLevel()), ignoreMissingColumns = true)
    }
  }

  private fun autoCreateDistributionsForSubject(
    subjectId: String,
    levels: String,
    academicYearId: String
  ) {
    runCatching {
      val targetLevels = levels.split(',', '|', ';')
        .map { it.normalizedAdminMapelLevel() }
        .filter { it.isNotBlank() }
        .distinct()
      if (subjectId.isBlank() || academicYearId.isBlank() || targetLevels.isEmpty()) return
      val semester = fetchRows(
        "semester",
        "select=*&tahun_ajaran_id=eq.${encodeValue(academicYearId)}&aktif=eq.true&order=id.desc&limit=1"
      ).firstOrNull() ?: fetchRows(
        "semester",
        "select=*&tahun_ajaran_id=eq.${encodeValue(academicYearId)}&order=id.desc&limit=1"
      ).firstOrNull() ?: return
      val semesterId = semester.cleanStringForAdminMapel("id")
      if (semesterId.isBlank()) return

      val classes = fetchRows(
        "kelas",
        "select=*&tahun_ajaran_id=eq.${encodeValue(academicYearId)}&order=nama_kelas.asc"
      ).mapNotNull { it.toAdminMapelClass(emptyMap()) }
        .filter { targetLevels.contains(it.level.toAdminMapelStage()) }
      if (classes.isEmpty()) return
      val existing = fetchRows(
        "distribusi_mapel",
        "select=id,kelas_id&mapel_id=eq.${encodeValue(subjectId)}&semester_id=eq.${encodeValue(semesterId)}"
      ).map { it.cleanStringForAdminMapel("kelas_id") }.toSet()
      classes
        .filterNot { existing.contains(it.id) }
        .forEach { kelas ->
          insertDistribution(
            linkedMapOf(
              "kelas_id" to kelas.id,
              "mapel_id" to subjectId,
              "guru_id" to null,
              "semester_id" to semesterId,
              "tingkatan" to kelas.level.toAdminMapelStage().ifBlank { null }
            )
          )
        }
    }
  }

  private fun insertDistribution(fields: Map<String, Any?>) {
    val connection = createConnection("${BuildConfig.SUPABASE_URL}/rest/v1/distribusi_mapel?select=id", method = "POST").apply {
      doOutput = true
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Prefer", "return=representation")
    }
    val payload = JSONObject().apply {
      fields.forEach { (key, value) -> putPayloadValueForAdminMapel(key, value) }
    }
    connection.outputStream.use { stream ->
      stream.write(payload.toString().toByteArray(Charsets.UTF_8))
      stream.flush()
    }
    connection.useAdminMapelJsonArrayResponse { rows ->
      if (rows.length() == 0) throw IllegalStateException("Distribusi tidak kembali setelah disimpan.")
    }
  }

  private fun patchFirstSupportedField(
    table: String,
    rowId: String,
    candidates: List<Pair<String, Any?>>
  ): Boolean {
    candidates.forEach { (field, value) ->
      val saved = patchRowFields(
        table = table,
        rowId = rowId,
        fields = mapOf(field to value),
        ignoreMissingColumns = true
      )
      if (saved.containsKey(field)) return true
    }
    return false
  }

  private fun patchRowFields(
    table: String,
    rowId: String,
    fields: Map<String, Any?>,
    ignoreMissingColumns: Boolean = false
  ): Map<String, Any?> {
    val savedFields = linkedMapOf<String, Any?>()
    fields.forEach { (field, value) ->
      try {
        val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table" +
          "?select=$field&id=eq.${encodeValue(rowId)}"
        val connection = createConnection(requestUrl, method = "PATCH").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        val payload = JSONObject().apply { putPayloadValueForAdminMapel(field, value) }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        val savedValue = connection.useAdminMapelJsonArrayResponse { rows ->
          rows.optJSONObject(0)?.opt(field)
        }
        savedFields[field] = savedValue ?: value
      } catch (error: Exception) {
        if (!ignoreMissingColumns || !error.message.orEmpty().isAdminMapelMissingColumnMessage(field)) throw error
      }
    }
    return savedFields
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = createConnection(requestUrl)
    return connection.useAdminMapelJsonArrayResponse { rows ->
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

private fun JSONObject.toAdminMapelAcademicYear(): AdminMapelAcademicYear? {
  val id = cleanStringForAdminMapel("id")
  if (id.isBlank()) return null
  return AdminMapelAcademicYear(
    id = id,
    name = firstCleanStringForAdminMapel("nama", "tahun_ajaran", "tahun").ifBlank { "Tahun Ajaran" },
    active = booleanFlexibleForAdminMapel("aktif") ||
      booleanFlexibleForAdminMapel("is_active") ||
      booleanFlexibleForAdminMapel("active")
  )
}

private fun JSONObject.toAdminMapelSemester(yearNameById: Map<String, String>): AdminMapelSemesterOption? {
  val id = cleanStringForAdminMapel("id")
  if (id.isBlank()) return null
  val yearId = firstCleanStringForAdminMapel("tahun_ajaran_id", "tahunAjaranId")
  return AdminMapelSemesterOption(
    id = id,
    name = firstCleanStringForAdminMapel("nama", "nama_semester", "semester", "label").ifBlank { "Semester" },
    academicYearId = yearId,
    academicYearName = yearNameById[yearId].orEmpty(),
    active = booleanFlexibleForAdminMapel("aktif") ||
      booleanFlexibleForAdminMapel("is_active") ||
      booleanFlexibleForAdminMapel("active")
  )
}

private fun JSONObject.toAdminMapelClass(yearNameById: Map<String, String>): AdminMapelClassOption? {
  val id = cleanStringForAdminMapel("id")
  if (id.isBlank()) return null
  val name = firstCleanStringForAdminMapel("nama_kelas", "kelas", "name").ifBlank { "Kelas" }
  val yearId = firstCleanStringForAdminMapel("tahun_ajaran_id", "tahunAjaranId")
  return AdminMapelClassOption(
    id = id,
    name = name,
    level = firstCleanStringForAdminMapel("tingkat", "level").ifBlank { name.deriveAdminMapelLevelFromName() },
    academicYearId = yearId,
    academicYearName = yearNameById[yearId].orEmpty()
  )
}

private fun JSONObject.toAdminMapelTeacher(): AdminMapelTeacherOption? {
  val id = cleanStringForAdminMapel("id")
  val name = firstCleanStringForAdminMapel("nama", "name")
  if (id.isBlank() || name.isBlank()) return null
  return AdminMapelTeacherOption(
    id = id,
    employeeId = firstCleanStringForAdminMapel("id_karyawan", "employee_id"),
    name = name,
    role = cleanStringForAdminMapel("role"),
    active = booleanFlexibleOrNullForAdminMapel("aktif") ?: true
  )
}

private fun JSONObject.toAdminMapelSubject(
  yearNameById: Map<String, String>,
  distributionCount: Int
): AdminMapelSubject? {
  val id = cleanStringForAdminMapel("id")
  if (id.isBlank()) return null
  val yearId = firstCleanStringForAdminMapel("tahun_ajaran_id", "tahunAjaranId")
  return AdminMapelSubject(
    rowId = id,
    name = firstCleanStringForAdminMapel("nama", "nama_mapel", "mapel").ifBlank { "Mapel" },
    category = firstCleanStringForAdminMapel("kategori", "category"),
    levels = firstCleanStringForAdminMapel("tingkatan_multi", "tingkatan", "jenjang").normalizedAdminMapelLevels(),
    academicYearId = yearId,
    academicYearName = yearNameById[yearId].orEmpty(),
    kkm = firstCleanStringForAdminMapel("kkm", "kkm_nilai"),
    distributionCount = distributionCount
  )
}

private fun JSONObject.toAdminMapelDistribution(
  classById: Map<String, AdminMapelClassOption>,
  subjectById: Map<String, AdminMapelSubject>,
  teacherById: Map<String, AdminMapelTeacherOption>,
  semesterById: Map<String, AdminMapelSemesterOption>,
  yearNameById: Map<String, String>
): AdminMapelDistribution? {
  val id = cleanStringForAdminMapel("id")
  if (id.isBlank()) return null
  val classId = cleanStringForAdminMapel("kelas_id")
  val subjectId = cleanStringForAdminMapel("mapel_id")
  val teacherId = cleanStringForAdminMapel("guru_id")
  val semesterId = cleanStringForAdminMapel("semester_id")
  val kelas = classById[classId]
  val subject = subjectById[subjectId]
  val semester = semesterById[semesterId]
  val academicYearId = semester?.academicYearId.orEmpty()
    .ifBlank { kelas?.academicYearId.orEmpty() }
    .ifBlank { subject?.academicYearId.orEmpty() }
  val classLevel = kelas?.level.orEmpty()
  val level = firstCleanStringForAdminMapel("tingkatan", "jenjang")
    .normalizedAdminMapelLevel()
    .ifBlank { classLevel.toAdminMapelStage() }
    .ifBlank { subject?.levels.orEmpty().split(',').firstOrNull().orEmpty() }
  return AdminMapelDistribution(
    rowId = id,
    classId = classId,
    className = kelas?.name.orEmpty().ifBlank { "-" },
    classLevel = classLevel,
    subjectId = subjectId,
    subjectName = subject?.name.orEmpty().ifBlank { "-" },
    subjectCategory = subject?.category.orEmpty(),
    teacherId = teacherId,
    teacherName = teacherById[teacherId]?.name.orEmpty(),
    semesterId = semesterId,
    semesterName = semester?.name.orEmpty().ifBlank { "-" },
    academicYearId = academicYearId,
    academicYearName = yearNameById[academicYearId].orEmpty().ifBlank { semester?.academicYearName.orEmpty() },
    level = level
  )
}

private inline fun <T> HttpURLConnection.useAdminMapelJsonArrayResponse(
  block: (JSONArray) -> T
): T {
  return try {
    val status = responseCode
    val payload = readAdminMapelPayload(status in 200..299)
    if (status !in 200..299) throw IllegalStateException(payload.ifBlank { "HTTP $status" })
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readAdminMapelPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}

private fun JSONObject.cleanStringForAdminMapel(key: String): String {
  val value = opt(key)
  if (value == null || value == JSONObject.NULL) return ""
  val text = value.toString().trim()
  return if (text.equals("null", ignoreCase = true)) "" else text
}

private fun JSONObject.firstCleanStringForAdminMapel(vararg keys: String): String {
  return keys.firstNotNullOfOrNull { key ->
    cleanStringForAdminMapel(key).takeIf { it.isNotBlank() }
  }.orEmpty()
}

private fun JSONObject.booleanFlexibleForAdminMapel(key: String): Boolean {
  return booleanFlexibleOrNullForAdminMapel(key) ?: false
}

private fun JSONObject.booleanFlexibleOrNullForAdminMapel(key: String): Boolean? {
  if (!has(key) || isNull(key)) return null
  val value = opt(key)
  if (value == true || value == 1) return true
  if (value == false || value == 0) return false
  return when (value?.toString().orEmpty().trim().lowercase()) {
    "true", "t", "1", "yes", "aktif", "active" -> true
    "false", "f", "0", "no", "tidak_aktif", "nonaktif", "inactive" -> false
    else -> null
  }
}

private fun JSONObject.putPayloadValueForAdminMapel(key: String, value: Any?) {
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

private fun String.normalizedAdminMapelLevel(): String {
  val value = trim().lowercase()
  return when (value) {
    "smp", "mts", "junior", "jhs", "7", "8", "9", "vii", "viii", "ix" -> "smp"
    "sma", "ma", "senior", "shs", "10", "11", "12", "x", "xi", "xii" -> "sma"
    else -> ""
  }
}

private fun String.normalizedAdminMapelLevels(): String {
  val values = trim()
    .removePrefix("[")
    .removeSuffix("]")
    .replace("\"", "")
    .split(',', '|', ';')
    .map { it.normalizedAdminMapelLevel() }
    .filter { it.isNotBlank() }
    .distinct()
  return values.joinToString(",")
}

private fun String.firstAdminMapelLevel(): String {
  return normalizedAdminMapelLevels().split(',').firstOrNull { it.isNotBlank() }.orEmpty()
}

private fun String.toAdminMapelStage(): String {
  return normalizedAdminMapelLevel().ifBlank {
    val number = toAdminMapelLevelNumber()
    when (number) {
      7, 8, 9 -> "smp"
      10, 11, 12 -> "sma"
      else -> ""
    }
  }
}

private fun String.toAdminMapelLevelNumber(): Int? {
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

private fun String.deriveAdminMapelLevelFromName(): String {
  return toAdminMapelLevelNumber()?.toString().orEmpty()
}

private fun String.firstYearForAdminMapel(): Int? {
  return Regex("\\b(19|20)\\d{2}\\b").find(this)?.value?.toIntOrNull()
}

private fun String.isAdminMapelMissingColumnMessage(field: String = ""): Boolean {
  val lower = lowercase()
  if (!lower.contains("column") && !lower.contains("pgrst")) return false
  return field.isBlank() || lower.contains(field.lowercase())
}

private fun String.isAdminMapelCheckConstraintMessage(): Boolean {
  val lower = lowercase()
  return lower.contains("check constraint") || lower.contains("violates check") || lower.contains("23514")
}

private fun Map<String, Any?>.minusAdminMapelKeys(vararg keys: String): Map<String, Any?> {
  return filterKeys { key -> key !in keys }
}
