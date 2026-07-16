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

data class AdminAcademicPeriodSnapshot(
  val academicYears: List<AdminAcademicYear>,
  val semesters: List<AdminAcademicSemester>
) {
  val activeAcademicYear: AdminAcademicYear?
    get() = academicYears.firstOrNull { it.active }

  val activeSemester: AdminAcademicSemester?
    get() = semesters.firstOrNull { it.active && (activeAcademicYear?.rowId.isNullOrBlank() || it.academicYearId == activeAcademicYear?.rowId) }
      ?: semesters.firstOrNull { it.active }
}

data class AdminAcademicYear(
  val rowId: String,
  val name: String,
  val startDateIso: String,
  val endDateIso: String,
  val active: Boolean
)

data class AdminAcademicSemester(
  val rowId: String,
  val academicYearId: String,
  val academicYearName: String,
  val name: String,
  val startDateIso: String,
  val endDateIso: String,
  val active: Boolean
)

sealed interface AdminAcademicPeriodLoadResult {
  data class Success(val snapshot: AdminAcademicPeriodSnapshot) : AdminAcademicPeriodLoadResult
  data class Error(val message: String) : AdminAcademicPeriodLoadResult
}

sealed interface AdminAcademicPeriodSaveResult {
  data class Success(
    val snapshot: AdminAcademicPeriodSnapshot,
    val message: String
  ) : AdminAcademicPeriodSaveResult
  data class Error(val message: String) : AdminAcademicPeriodSaveResult
}

class AdminAcademicPeriodRemoteDataSource {
  suspend fun fetchSnapshot(): AdminAcademicPeriodLoadResult = withContext(Dispatchers.IO) {
    try {
      AdminAcademicPeriodLoadResult.Success(loadSnapshot())
    } catch (_: SocketTimeoutException) {
      AdminAcademicPeriodLoadResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminAcademicPeriodLoadResult.Error("Gagal memuat tahun ajaran dan semester.")
    }
  }

  suspend fun saveAcademicYear(year: AdminAcademicYear): AdminAcademicPeriodSaveResult = withContext(Dispatchers.IO) {
    val name = year.name.trim()
    if (name.isBlank()) {
      return@withContext AdminAcademicPeriodSaveResult.Error("Nama tahun ajaran wajib diisi.")
    }
    try {
      if (year.rowId.isBlank()) {
        insertRow(
          table = "tahun_ajaran",
          fieldCandidates = listOf(
            linkedMapOf(
              "nama" to name,
              "tanggal_mulai" to year.startDateIso.trim().ifBlank { null },
              "tanggal_selesai" to year.endDateIso.trim().ifBlank { null },
              "aktif" to year.active
            ),
            linkedMapOf(
              "tahun_ajaran" to name,
              "tanggal_mulai" to year.startDateIso.trim().ifBlank { null },
              "tanggal_selesai" to year.endDateIso.trim().ifBlank { null },
              "aktif" to year.active
            ),
            linkedMapOf("nama" to name, "aktif" to year.active),
            linkedMapOf("tahun_ajaran" to name, "aktif" to year.active)
          )
        )
      } else {
        val nameSaved = patchFirstSupportedField(
          table = "tahun_ajaran",
          rowId = year.rowId,
          candidates = listOf("nama" to name, "tahun_ajaran" to name, "tahun" to name)
        )
        if (!nameSaved) {
          return@withContext AdminAcademicPeriodSaveResult.Error("Kolom nama tahun ajaran belum bisa disimpan.")
        }
        patchRowFields(
          table = "tahun_ajaran",
          rowId = year.rowId,
          fields = linkedMapOf(
            "tanggal_mulai" to year.startDateIso.trim().ifBlank { null },
            "tanggal_selesai" to year.endDateIso.trim().ifBlank { null }
          ),
          ignoreMissingColumns = true
        )
      }
      AdminAcademicPeriodSaveResult.Success(loadSnapshot(), "Tahun ajaran berhasil disimpan.")
    } catch (_: SocketTimeoutException) {
      AdminAcademicPeriodSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminAcademicPeriodSaveResult.Error("Gagal menyimpan tahun ajaran.")
    }
  }

  suspend fun saveSemester(semester: AdminAcademicSemester): AdminAcademicPeriodSaveResult = withContext(Dispatchers.IO) {
    val name = semester.name.trim()
    val yearId = semester.academicYearId.trim()
    if (name.isBlank()) {
      return@withContext AdminAcademicPeriodSaveResult.Error("Nama semester wajib diisi.")
    }
    if (yearId.isBlank()) {
      return@withContext AdminAcademicPeriodSaveResult.Error("Tahun ajaran semester wajib dipilih.")
    }
    try {
      if (semester.rowId.isBlank()) {
        insertRow(
          table = "semester",
          fieldCandidates = listOf(
            linkedMapOf(
              "nama" to name,
              "tahun_ajaran_id" to yearId,
              "tanggal_mulai" to semester.startDateIso.trim().ifBlank { null },
              "tanggal_selesai" to semester.endDateIso.trim().ifBlank { null },
              "aktif" to semester.active
            ),
            linkedMapOf("nama" to name, "tahun_ajaran_id" to yearId, "aktif" to semester.active),
            linkedMapOf("semester" to name, "tahun_ajaran_id" to yearId, "aktif" to semester.active)
          )
        )
      } else {
        val nameSaved = patchFirstSupportedField(
          table = "semester",
          rowId = semester.rowId,
          candidates = listOf("nama" to name, "nama_semester" to name, "semester" to name, "label" to name)
        )
        if (!nameSaved) {
          return@withContext AdminAcademicPeriodSaveResult.Error("Kolom nama semester belum bisa disimpan.")
        }
        patchRowFields(
          table = "semester",
          rowId = semester.rowId,
          fields = linkedMapOf(
            "tahun_ajaran_id" to yearId,
            "tanggal_mulai" to semester.startDateIso.trim().ifBlank { null },
            "tanggal_selesai" to semester.endDateIso.trim().ifBlank { null }
          ),
          ignoreMissingColumns = true
        )
      }
      AdminAcademicPeriodSaveResult.Success(loadSnapshot(), "Semester berhasil disimpan.")
    } catch (_: SocketTimeoutException) {
      AdminAcademicPeriodSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminAcademicPeriodSaveResult.Error("Gagal menyimpan semester.")
    }
  }

  suspend fun setActiveAcademicYear(rowId: String): AdminAcademicPeriodSaveResult = withContext(Dispatchers.IO) {
    val normalizedId = rowId.trim()
    if (normalizedId.isBlank()) {
      return@withContext AdminAcademicPeriodSaveResult.Error("Tahun ajaran belum dipilih.")
    }
    try {
      fetchRows("tahun_ajaran", "select=id&aktif=eq.true").forEach { row ->
        val activeId = row.cleanStringForAdminAcademicPeriod("id")
        if (activeId.isNotBlank() && activeId != normalizedId) {
          patchRowFields("tahun_ajaran", activeId, mapOf("aktif" to false))
        }
      }
      patchRowFields("tahun_ajaran", normalizedId, mapOf("aktif" to true))
      AdminAcademicPeriodSaveResult.Success(loadSnapshot(), "Tahun ajaran aktif berhasil diganti.")
    } catch (_: SocketTimeoutException) {
      AdminAcademicPeriodSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminAcademicPeriodSaveResult.Error("Gagal mengganti tahun ajaran aktif.")
    }
  }

  suspend fun setActiveSemester(rowId: String): AdminAcademicPeriodSaveResult = withContext(Dispatchers.IO) {
    val normalizedId = rowId.trim()
    if (normalizedId.isBlank()) {
      return@withContext AdminAcademicPeriodSaveResult.Error("Semester belum dipilih.")
    }
    try {
      fetchRows("semester", "select=id&aktif=eq.true").forEach { row ->
        val activeId = row.cleanStringForAdminAcademicPeriod("id")
        if (activeId.isNotBlank() && activeId != normalizedId) {
          patchRowFields("semester", activeId, mapOf("aktif" to false))
        }
      }
      patchRowFields("semester", normalizedId, mapOf("aktif" to true))
      AdminAcademicPeriodSaveResult.Success(loadSnapshot(), "Semester aktif berhasil diganti.")
    } catch (_: SocketTimeoutException) {
      AdminAcademicPeriodSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminAcademicPeriodSaveResult.Error("Gagal mengganti semester aktif.")
    }
  }

  private fun loadSnapshot(): AdminAcademicPeriodSnapshot {
    val yearRows = fetchRows("tahun_ajaran", "select=*&order=id.desc")
    val years = yearRows.mapNotNull { it.toAdminAcademicYear() }
      .sortedWith(
        compareByDescending<AdminAcademicYear> { it.active }
          .thenByDescending { it.name.firstYearForAdminAcademicPeriod() ?: 0 }
          .thenBy { it.name.lowercase() }
      )
    val yearNameById = years.associate { it.rowId to it.name }
    val semesterRows = runCatching {
      fetchRows("semester", "select=*&order=tanggal_mulai.desc")
    }.getOrElse {
      fetchRows("semester", "select=*")
    }
    val semesters = semesterRows.mapNotNull { it.toAdminAcademicSemester(yearNameById) }
      .sortedWith(
        compareByDescending<AdminAcademicSemester> { it.active }
          .thenByDescending { yearNameById[it.academicYearId].orEmpty().firstYearForAdminAcademicPeriod() ?: 0 }
          .thenByDescending { it.startDateIso }
          .thenBy { it.name.lowercase() }
      )
    return AdminAcademicPeriodSnapshot(years, semesters)
  }

  private fun insertRow(table: String, fieldCandidates: List<Map<String, Any?>>) {
    var lastError: Exception? = null
    fieldCandidates.forEach { fields ->
      try {
        val connection = createConnection("${BuildConfig.SUPABASE_URL}/rest/v1/$table?select=id", method = "POST").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        val payload = JSONObject().apply {
          fields.forEach { (key, value) -> putPayloadValueForAdminAcademicPeriod(key, value) }
        }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        connection.useAdminAcademicPeriodJsonArrayResponse { rows ->
          if (rows.length() == 0) throw IllegalStateException("Data tidak kembali setelah disimpan.")
        }
        return
      } catch (error: Exception) {
        lastError = error
        if (!error.message.orEmpty().isMissingColumnForAdminAcademicPeriod()) throw error
      }
    }
    throw lastError ?: IllegalStateException("Gagal menyimpan data.")
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
        val payload = JSONObject().apply { putPayloadValueForAdminAcademicPeriod(field, value) }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        val savedValue = connection.useAdminAcademicPeriodJsonArrayResponse { rows ->
          rows.optJSONObject(0)?.opt(field)
        }
        savedFields[field] = savedValue ?: value
      } catch (error: Exception) {
        if (!ignoreMissingColumns || !error.message.orEmpty().isMissingColumnForAdminAcademicPeriod(field)) {
          throw error
        }
      }
    }
    return savedFields
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = createConnection(requestUrl)
    return connection.useAdminAcademicPeriodJsonArrayResponse { rows ->
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

private fun JSONObject.toAdminAcademicYear(): AdminAcademicYear? {
  val id = cleanStringForAdminAcademicPeriod("id")
  if (id.isBlank()) return null
  return AdminAcademicYear(
    rowId = id,
    name = cleanStringForAdminAcademicPeriod("nama")
      .ifBlank { cleanStringForAdminAcademicPeriod("tahun_ajaran") }
      .ifBlank { cleanStringForAdminAcademicPeriod("tahun") },
    startDateIso = firstCleanStringForAdminAcademicPeriod("tanggal_mulai", "mulai", "start_date").take(10),
    endDateIso = firstCleanStringForAdminAcademicPeriod("tanggal_selesai", "selesai", "end_date").take(10),
    active = booleanFlexibleForAdminAcademicPeriod("aktif") ||
      booleanFlexibleForAdminAcademicPeriod("is_active") ||
      booleanFlexibleForAdminAcademicPeriod("active")
  )
}

private fun JSONObject.toAdminAcademicSemester(yearNameById: Map<String, String>): AdminAcademicSemester? {
  val id = cleanStringForAdminAcademicPeriod("id")
  if (id.isBlank()) return null
  val academicYearId = firstCleanStringForAdminAcademicPeriod("tahun_ajaran_id", "tahunAjaranId")
  return AdminAcademicSemester(
    rowId = id,
    academicYearId = academicYearId,
    academicYearName = yearNameById[academicYearId].orEmpty(),
    name = firstCleanStringForAdminAcademicPeriod("nama", "nama_semester", "semester", "label"),
    startDateIso = firstCleanStringForAdminAcademicPeriod("tanggal_mulai", "mulai", "start_date").take(10),
    endDateIso = firstCleanStringForAdminAcademicPeriod("tanggal_selesai", "selesai", "end_date").take(10),
    active = booleanFlexibleForAdminAcademicPeriod("aktif") ||
      booleanFlexibleForAdminAcademicPeriod("is_active") ||
      booleanFlexibleForAdminAcademicPeriod("active")
  )
}

private inline fun <T> HttpURLConnection.useAdminAcademicPeriodJsonArrayResponse(
  block: (JSONArray) -> T
): T {
  return try {
    val status = responseCode
    val payload = readAdminAcademicPeriodPayload(status in 200..299)
    if (status !in 200..299) throw IllegalStateException(payload.ifBlank { "HTTP $status" })
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readAdminAcademicPeriodPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}

private fun JSONObject.cleanStringForAdminAcademicPeriod(key: String): String {
  val value = opt(key)
  if (value == null || value == JSONObject.NULL) return ""
  val text = value.toString().trim()
  return if (text.equals("null", ignoreCase = true)) "" else text
}

private fun JSONObject.firstCleanStringForAdminAcademicPeriod(vararg keys: String): String {
  return keys.firstNotNullOfOrNull { key ->
    cleanStringForAdminAcademicPeriod(key).takeIf { it.isNotBlank() }
  }.orEmpty()
}

private fun JSONObject.booleanFlexibleForAdminAcademicPeriod(key: String): Boolean {
  if (!has(key) || isNull(key)) return false
  val value = opt(key)
  if (value == true || value == 1) return true
  if (value == false || value == 0) return false
  return value?.toString().orEmpty().trim().lowercase() in setOf("true", "t", "1", "yes", "aktif", "active")
}

private fun JSONObject.putPayloadValueForAdminAcademicPeriod(key: String, value: Any?) {
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

private fun String.isMissingColumnForAdminAcademicPeriod(field: String = ""): Boolean {
  val lower = lowercase()
  if (!lower.contains("column") && !lower.contains("pgrst")) return false
  return field.isBlank() || lower.contains(field.lowercase())
}

private fun String.firstYearForAdminAcademicPeriod(): Int? {
  return Regex("\\b(19|20)\\d{2}\\b").find(this)?.value?.toIntOrNull()
}
