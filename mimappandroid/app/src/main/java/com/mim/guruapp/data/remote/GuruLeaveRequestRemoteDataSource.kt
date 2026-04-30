package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.LeaveRequestItem
import com.mim.guruapp.data.model.LeaveRequestSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed interface GuruLeaveRequestSaveResult {
  data class Success(val snapshot: LeaveRequestSnapshot) : GuruLeaveRequestSaveResult
  data class Error(val message: String) : GuruLeaveRequestSaveResult
}

class GuruLeaveRequestRemoteDataSource {
  suspend fun fetchLeaveRequestSnapshot(
    teacherRowId: String,
    teacherKaryawanId: String
  ): LeaveRequestSnapshot? = withContext(Dispatchers.IO) {
    runCatching {
      val guruId = teacherRowId.trim().ifBlank { resolveTeacherRowId(teacherKaryawanId) }
      if (guruId.isBlank()) return@runCatching null

      val rows = fetchRows(
        table = "izin_karyawan",
        query = buildString {
          append("select=id,guru_id,tanggal_mulai,tanggal_selesai,durasi_hari,keperluan,status,catatan_wakasek,reviewed_at,created_at,updated_at")
          append("&guru_id=eq.")
          append(encodeValue(guruId))
          append("&order=created_at.desc")
        }
      )

      LeaveRequestSnapshot(
        guruId = guruId,
        requests = rows.mapNotNull(::parseLeaveRequestItem),
        updatedAt = System.currentTimeMillis()
      )
    }.getOrNull()
  }

  suspend fun submitLeaveRequest(
    teacherRowId: String,
    teacherKaryawanId: String,
    startDateIso: String,
    endDateIso: String,
    purpose: String
  ): GuruLeaveRequestSaveResult = withContext(Dispatchers.IO) {
    val guruId = teacherRowId.trim().ifBlank { resolveTeacherRowId(teacherKaryawanId) }
    if (guruId.isBlank()) {
      return@withContext GuruLeaveRequestSaveResult.Error("ID guru belum tersedia.")
    }

    val startDate = parseDate(startDateIso)
      ?: return@withContext GuruLeaveRequestSaveResult.Error("Tanggal mulai tidak valid.")
    val endDate = parseDate(endDateIso)
      ?: return@withContext GuruLeaveRequestSaveResult.Error("Tanggal selesai tidak valid.")
    if (endDate.isBefore(startDate)) {
      return@withContext GuruLeaveRequestSaveResult.Error("Tanggal selesai tidak boleh sebelum tanggal mulai.")
    }

    val normalizedPurpose = purpose.trim()
    if (normalizedPurpose.isBlank()) {
      return@withContext GuruLeaveRequestSaveResult.Error("Keperluan izin wajib diisi.")
    }

    val durationDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
    if (durationDays !in 1..90) {
      return@withContext GuruLeaveRequestSaveResult.Error("Durasi izin harus antara 1 sampai 90 hari.")
    }

    runCatching {
      val requestUrl = buildString {
        append(BuildConfig.SUPABASE_URL)
        append("/rest/v1/izin_karyawan?select=id,guru_id,tanggal_mulai,tanggal_selesai,durasi_hari,keperluan,status,catatan_wakasek,reviewed_at,created_at,updated_at")
      }
      val connection = createConnection(requestUrl, method = "POST").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Prefer", "return=representation")
      }
      val payload = JSONArray().apply {
        put(
          JSONObject().apply {
            put("guru_id", guruId)
            put("tanggal_mulai", startDate.toString())
            put("tanggal_selesai", endDate.toString())
            put("durasi_hari", durationDays)
            put("keperluan", normalizedPurpose)
            put("status", "menunggu")
          }
        )
      }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      connection.useJsonArrayResponse { }

      val snapshot = fetchLeaveRequestSnapshot(
        teacherRowId = guruId,
        teacherKaryawanId = teacherKaryawanId
      ) ?: LeaveRequestSnapshot(
        guruId = guruId,
        requests = emptyList(),
        updatedAt = System.currentTimeMillis()
      )
      GuruLeaveRequestSaveResult.Success(snapshot)
    }.getOrElse { error ->
      GuruLeaveRequestSaveResult.Error(resolveErrorMessage(error, fallback = "Gagal mengirim pengajuan izin."))
    }
  }

  suspend fun deleteLeaveRequest(
    teacherRowId: String,
    teacherKaryawanId: String,
    requestId: String
  ): GuruLeaveRequestSaveResult = withContext(Dispatchers.IO) {
    val guruId = teacherRowId.trim().ifBlank { resolveTeacherRowId(teacherKaryawanId) }
    if (guruId.isBlank()) {
      return@withContext GuruLeaveRequestSaveResult.Error("ID guru belum tersedia.")
    }

    val normalizedRequestId = requestId.trim()
    if (normalizedRequestId.isBlank()) {
      return@withContext GuruLeaveRequestSaveResult.Error("Data izin yang akan dihapus belum valid.")
    }

    runCatching {
      val requestUrl = buildString {
        append(BuildConfig.SUPABASE_URL)
        append("/rest/v1/izin_karyawan?")
        append("id=eq.")
        append(encodeValue(normalizedRequestId))
        append("&guru_id=eq.")
        append(encodeValue(guruId))
        append("&select=id")
      }
      val connection = createConnection(requestUrl, method = "DELETE").apply {
        setRequestProperty("Prefer", "return=representation")
      }
      connection.useJsonArrayResponse { }

      val snapshot = fetchLeaveRequestSnapshot(
        teacherRowId = guruId,
        teacherKaryawanId = teacherKaryawanId
      ) ?: LeaveRequestSnapshot(
        guruId = guruId,
        requests = emptyList(),
        updatedAt = System.currentTimeMillis()
      )
      GuruLeaveRequestSaveResult.Success(snapshot)
    }.getOrElse { error ->
      GuruLeaveRequestSaveResult.Error(resolveErrorMessage(error, fallback = "Gagal menghapus pengajuan izin."))
    }
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = createConnection(requestUrl)
    return connection.useJsonArrayResponse {
      List(it.length()) { index -> it.optJSONObject(index) }.filterNotNull()
    }
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

  private fun resolveTeacherRowId(teacherKaryawanId: String): String {
    if (teacherKaryawanId.isBlank()) return ""
    return runCatching {
      fetchRows(
        table = "karyawan",
        query = "select=id&id_karyawan=eq.${encodeValue(teacherKaryawanId)}&limit=1"
      ).firstOrNull()?.cleanString("id").orEmpty()
    }.getOrDefault("")
  }

  private fun parseLeaveRequestItem(item: JSONObject?): LeaveRequestItem? {
    item ?: return null
    val id = item.cleanString("id")
    if (id.isBlank()) return null
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
      updatedAt = item.cleanString("updated_at")
    )
  }

  private fun normalizeStatus(value: String): String {
    val normalized = value.trim().lowercase()
    return when {
      normalized.contains("terima") || normalized.contains("setuju") || normalized.contains("approve") ->
        "diterima"

      normalized.contains("tolak") || normalized.contains("reject") ->
        "ditolak"

      else -> "menunggu"
    }
  }

  private fun parseDate(value: String): LocalDate? {
    return runCatching { LocalDate.parse(value.trim().take(10)) }.getOrNull()
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
}

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
  val value = opt(key)
  if (value == null || value == JSONObject.NULL) return ""
  return value.toString().trim().takeUnless { it.equals("null", ignoreCase = true) }.orEmpty()
}
