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

data class GuruTeachingSessionRecord(
  val id: String = "",
  val dateIso: String,
  val distribusiId: String,
  val lessonSlotId: String = "",
  val material: String = "",
  val teachingContent: String = "",
  val taskTitle: String = "",
  val taskDescription: String = "",
  val assessmentSummaryJson: String = "[]",
  val status: String = "draft",
  val updatedAt: String = ""
)

sealed interface GuruTeachingSessionSaveResult {
  data class Success(val record: GuruTeachingSessionRecord) : GuruTeachingSessionSaveResult
  data class Error(val message: String) : GuruTeachingSessionSaveResult
}

class GuruTeachingSessionRemoteDataSource {
  suspend fun fetchSession(
    distribusiId: String,
    lessonSlotId: String,
    dateIso: String
  ): GuruTeachingSessionRecord? = withContext(Dispatchers.IO) {
    if (distribusiId.isBlank() || dateIso.isBlank()) return@withContext null
    runCatching {
      fetchRows(
        table = "sesi_mengajar",
        query = buildString {
          append("select=*")
          append("&distribusi_id=eq.")
          append(encodeValue(distribusiId))
          append("&lesson_slot_id=eq.")
          append(encodeValue(lessonSlotId.trim()))
          append("&tanggal=eq.")
          append(encodeValue(dateIso.take(10)))
          append("&limit=1")
        }
      ).firstOrNull()?.toTeachingSessionRecord()
    }.getOrNull()
  }

  suspend fun saveSession(
    record: GuruTeachingSessionRecord,
    guruId: String
  ): GuruTeachingSessionSaveResult = withContext(Dispatchers.IO) {
    if (record.distribusiId.isBlank() || record.dateIso.isBlank()) {
      return@withContext GuruTeachingSessionSaveResult.Error("Data sesi mengajar belum lengkap.")
    }

    val distribusiRow = fetchRows(
      table = "distribusi_mapel",
      query = "select=id,kelas_id,mapel_id,semester_id&id=eq.${encodeValue(record.distribusiId)}&limit=1"
    ).firstOrNull()
      ?: return@withContext GuruTeachingSessionSaveResult.Error("Distribusi mapel tidak ditemukan.")

    val payload = JSONObject().apply {
      put("tanggal", record.dateIso.take(10))
      put("distribusi_id", record.distribusiId)
      put("lesson_slot_id", record.lessonSlotId.trim())
      put("kelas_id", distribusiRow.optCleanString("kelas_id").ifBlank { JSONObject.NULL })
      put("mapel_id", distribusiRow.optCleanString("mapel_id").ifBlank { JSONObject.NULL })
      put("semester_id", distribusiRow.optCleanString("semester_id").ifBlank { JSONObject.NULL })
      put("guru_id", guruId.trim().takeIf { it.isUuidText() } ?: JSONObject.NULL)
      put("materi", record.material.trim())
      put("isi_materi", record.teachingContent.trim())
      put("tugas_judul", record.taskTitle.trim())
      put("tugas_deskripsi", record.taskDescription.trim())
      put("assessment_summary_json", normalizeAssessmentSummaryJson(record.assessmentSummaryJson))
      put("status", if (record.status == "saved") "saved" else "draft")
    }

    return@withContext try {
      val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/sesi_mengajar?on_conflict=distribusi_id,lesson_slot_id,tanggal"
      val connection = createConnection(requestUrl, "POST").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
      }
      connection.outputStream.use { stream ->
        stream.write(JSONArray().put(payload).toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      val saved = connection.useTeachingSessionJsonArrayResponse { rows ->
        rows.optJSONObject(0)?.toTeachingSessionRecord()
      } ?: record
      GuruTeachingSessionSaveResult.Success(saved)
    } catch (_: SocketTimeoutException) {
      GuruTeachingSessionSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      GuruTeachingSessionSaveResult.Error("Gagal menyimpan sesi mengajar.")
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
          rows.optJSONObject(index)?.let(::add)
        }
      }
    } finally {
      connection.disconnect()
    }
  }

  private fun createConnection(requestUrl: String, method: String): HttpURLConnection {
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

private fun JSONObject.toTeachingSessionRecord(): GuruTeachingSessionRecord {
  return GuruTeachingSessionRecord(
    id = optCleanString("id"),
    dateIso = optCleanString("tanggal").take(10),
    distribusiId = optCleanString("distribusi_id"),
    lessonSlotId = optCleanString("lesson_slot_id"),
    material = optCleanString("materi"),
    teachingContent = optCleanString("isi_materi"),
    taskTitle = optCleanString("tugas_judul"),
    taskDescription = optCleanString("tugas_deskripsi"),
    assessmentSummaryJson = normalizeAssessmentSummaryJson(optCleanString("assessment_summary_json")),
    status = optCleanString("status").ifBlank { "draft" },
    updatedAt = optCleanString("updated_at")
  )
}

private fun normalizeAssessmentSummaryJson(value: String): String {
  val trimmed = value.trim()
  if (trimmed.isBlank()) return "[]"
  return runCatching {
    JSONArray(trimmed).toString()
  }.getOrDefault("[]")
}

private fun JSONObject.optCleanString(name: String): String {
  if (isNull(name)) return ""
  return opt(name)?.toString().orEmpty().trim()
}

private fun String.isUuidText(): Boolean {
  return matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
}

private inline fun <T> HttpURLConnection.useTeachingSessionJsonArrayResponse(
  block: (JSONArray) -> T
): T {
  return try {
    val code = responseCode
    val payload = readTeachingSessionPayload(code in 200..299)
    if (code !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $code" })
    }
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readTeachingSessionPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}
