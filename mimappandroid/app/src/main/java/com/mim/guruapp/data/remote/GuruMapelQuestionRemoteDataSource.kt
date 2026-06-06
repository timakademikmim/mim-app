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
import java.time.Instant
import java.time.format.DateTimeParseException

sealed interface GuruMapelQuestionSaveResult {
  data object Success : GuruMapelQuestionSaveResult
  data class Error(val message: String) : GuruMapelQuestionSaveResult
}

class GuruMapelQuestionRemoteDataSource {
  suspend fun fetchQuestionJson(
    distribusiId: String
  ): String? = withContext(Dispatchers.IO) {
    if (distribusiId.isBlank()) return@withContext null
    runCatching {
      val rows = fetchRows(
        table = "mapel_soal_guru",
        query = buildString {
          append("select=id,judul,kategori,tanggal,bentuk_soal,instruksi,questions_json,status,updated_at")
          append("&distribusi_id=eq.")
          append(encodeValue(distribusiId))
          append("&order=updated_at.desc")
        }
      )
      JSONArray().apply {
        rows.forEach { row ->
          put(row.toDraftJson())
        }
      }.toString()
    }.getOrNull()
  }

  suspend fun saveQuestionJson(
    distribusiId: String,
    rawJson: String,
    guruId: String,
    guruName: String
  ): GuruMapelQuestionSaveResult = withContext(Dispatchers.IO) {
    if (distribusiId.isBlank()) {
      return@withContext GuruMapelQuestionSaveResult.Error("Data distribusi mapel belum lengkap.")
    }

    val distribusiRow = fetchRows(
      table = "distribusi_mapel",
      query = "select=id,kelas_id,mapel_id,semester_id&id=eq.${encodeValue(distribusiId)}&limit=1"
    ).firstOrNull()
      ?: return@withContext GuruMapelQuestionSaveResult.Error("Distribusi mapel tidak ditemukan.")

    val drafts = parseDraftArray(rawJson)
    val payloads = drafts.map { draft ->
      draft to draft.toServerPayload(
        distribusiId = distribusiId,
        distribusiRow = distribusiRow,
        guruId = guruId,
        guruName = guruName
      )
    }

    return@withContext try {
      val existingRows = fetchRows(
        table = "mapel_soal_guru",
        query = buildString {
          append("select=id,questions_json")
          append("&distribusi_id=eq.")
          append(encodeValue(distribusiId))
        }
      )
      val existingByDraftId = existingRows
        .mapNotNull { row ->
          val draftId = row.remoteDraftId()
          val rowId = row.optString("id").trim()
          if (draftId.isBlank() || rowId.isBlank()) null else draftId to rowId
        }
        .toMap()

      payloads.forEach { (draft, payload) ->
        val draftId = draft.optString("id").trim()
        val existingRowId = existingByDraftId[draftId].orEmpty()
        if (existingRowId.isNotBlank()) {
          val updateUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/mapel_soal_guru?id=eq.${encodeValue(existingRowId)}"
          val connection = createConnection(updateUrl, "PATCH").apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=minimal")
          }
          connection.outputStream.use { stream ->
            stream.write(payload.toString().toByteArray(Charsets.UTF_8))
            stream.flush()
          }
          connection.useQuestionResponse { }
        } else {
          val insertUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/mapel_soal_guru"
          val connection = createConnection(insertUrl, "POST").apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=representation")
          }
          connection.outputStream.use { stream ->
            stream.write(JSONArray().put(payload).toString().toByteArray(Charsets.UTF_8))
            stream.flush()
          }
          connection.useQuestionResponse { }
        }
      }

      GuruMapelQuestionSaveResult.Success
    } catch (_: SocketTimeoutException) {
      GuruMapelQuestionSaveResult.Error("Koneksi ke server terlalu lama. Soal tetap menjadi draft lokal.")
    } catch (_: Exception) {
      GuruMapelQuestionSaveResult.Error("Gagal menyimpan soal ke server. Soal tetap menjadi draft lokal.")
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

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
  }
}

private fun parseDraftArray(rawJson: String): List<JSONObject> {
  return runCatching {
    val array = JSONArray(rawJson.ifBlank { "[]" })
    buildList {
      for (index in 0 until array.length()) {
        val row = array.optJSONObject(index) ?: continue
        add(row)
      }
    }
  }.getOrDefault(emptyList())
}

private fun JSONObject.toDraftJson(): JSONObject {
  val storedDraft = runCatching { JSONObject(optString("questions_json")) }.getOrNull() ?: JSONObject()
  return JSONObject(storedDraft.toString()).apply {
    putIfBlank("id", optString("id").ifBlank { this@toDraftJson.optString("id") })
    putIfBlank("title", this@toDraftJson.optString("judul"))
    putIfBlank("category", this@toDraftJson.optString("kategori").ifBlank { "Ujian" })
    putIfBlank("form", this@toDraftJson.optString("bentuk_soal"))
    putIfBlank("dateIso", this@toDraftJson.optString("tanggal").take(10))
    putIfBlank("academicYearLabel", "")
    putIfBlank("instruction", this@toDraftJson.optString("instruksi"))
    putIfBlank("statusLabel", this@toDraftJson.optString("status").ifBlank { "Draft" })
    if (!has("updatedAt") || optLong("updatedAt", 0L) <= 0L) {
      put("updatedAt", parseInstantMillis(this@toDraftJson.optString("updated_at")))
    }
  }
}

private fun JSONObject.remoteDraftId(): String {
  val storedDraftId = runCatching { JSONObject(optString("questions_json")) }
    .getOrNull()
    ?.optString("id")
    .orEmpty()
    .trim()
  return storedDraftId.ifBlank { optString("id").trim() }
}

private fun JSONObject.toServerPayload(
  distribusiId: String,
  distribusiRow: JSONObject,
  guruId: String,
  guruName: String
): JSONObject {
  val title = optString("title").trim().ifBlank { "Soal" }
  val category = optString("category").trim().ifBlank { "Ujian" }
  val form = optString("form").trim()
  val instruction = optString("instruction").trim()
  val dateIso = optString("dateIso").trim().take(10)
  return JSONObject().apply {
    put("distribusi_id", distribusiId)
    put("kelas_id", distribusiRow.optString("kelas_id").trim())
    put("mapel_id", distribusiRow.optString("mapel_id").trim())
    put("semester_id", distribusiRow.optString("semester_id").trim().takeIf { it.isNotBlank() } ?: JSONObject.NULL)
    put("tahun_ajaran_id", JSONObject.NULL)
    put("created_by_guru_id", guruId)
    put("created_by_guru_nama", guruName.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
    put("updated_by_guru_id", guruId.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
    put("updated_by_guru_nama", guruName.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
    put("judul", title)
    put("kategori", category)
    put("tanggal", dateIso.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) } ?: JSONObject.NULL)
    put("keterangan", JSONObject.NULL)
    put("bentuk_soal", form.ifBlank { JSONObject.NULL })
    put("jumlah_nomor", optInt("questionCount", 0).takeIf { it > 0 } ?: JSONObject.NULL)
    put("instruksi", instruction.ifBlank { JSONObject.NULL })
    put("questions_json", toString())
    put("status", optString("statusLabel").trim().lowercase().ifBlank { "draft" })
  }
}

private fun JSONObject.putIfBlank(key: String, value: String) {
  if (!has(key) || optString(key).isBlank()) put(key, value)
}

private fun parseInstantMillis(value: String): Long {
  return try {
    Instant.parse(value).toEpochMilli()
  } catch (_: DateTimeParseException) {
    System.currentTimeMillis()
  }
}

private inline fun <T> HttpURLConnection.useQuestionResponse(
  block: (String) -> T
): T {
  return try {
    val code = responseCode
    val payload = readQuestionPayload(code in 200..299)
    if (code !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $code" })
    }
    block(payload)
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readQuestionPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}
