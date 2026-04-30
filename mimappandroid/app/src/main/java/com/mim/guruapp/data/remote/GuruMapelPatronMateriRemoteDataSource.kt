package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.MapelPatronMateriSnapshot
import com.mim.guruapp.data.model.PatronMateriItem
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

sealed interface GuruMapelPatronMateriSaveResult {
  data object Success : GuruMapelPatronMateriSaveResult
  data class Error(val message: String) : GuruMapelPatronMateriSaveResult
}

class GuruMapelPatronMateriRemoteDataSource {
  suspend fun fetchPatronMateriSnapshot(
    distribusiId: String,
    fallbackTitle: String,
    fallbackClassName: String
  ): MapelPatronMateriSnapshot? = withContext(Dispatchers.IO) {
    runCatching {
      val row = fetchRows(
        table = "mapel_patron_materi",
        query = "select=materi_json&distribusi_id=eq.${encodeValue(distribusiId)}&limit=1"
      ).firstOrNull()
      val normalized = normalizeMateriItems(row?.optString("materi_json").orEmpty())

      MapelPatronMateriSnapshot(
        distribusiId = distribusiId,
        subjectTitle = fallbackTitle,
        className = fallbackClassName,
        items = normalized.items,
        updatedAt = System.currentTimeMillis(),
        containsRecoveredText = normalized.containsRecoveredText
      )
    }.getOrNull()
  }

  suspend fun savePatronMateri(
    distribusiId: String,
    items: List<PatronMateriItem>,
    guruId: String
  ): GuruMapelPatronMateriSaveResult = withContext(Dispatchers.IO) {
    if (distribusiId.isBlank()) {
      return@withContext GuruMapelPatronMateriSaveResult.Error("Data distribusi belum lengkap.")
    }

    val distribusiRow = fetchRows(
      table = "distribusi_mapel",
      query = "select=id,mapel_id,semester_id&id=eq.${encodeValue(distribusiId)}&limit=1"
    ).firstOrNull()
      ?: return@withContext GuruMapelPatronMateriSaveResult.Error("Distribusi mapel tidak ditemukan.")

    val materiList = items
      .map { it.text.trim() }
      .filter { it.isNotBlank() }
      .distinct()

    val payload = JSONObject().apply {
      put("distribusi_id", distribusiId)
      put("guru_id", guruId)
      put("mapel_id", distribusiRow.optString("mapel_id").trim())
      put("semester_id", distribusiRow.optString("semester_id").trim().ifBlank { JSONObject.NULL })
      put("materi_json", JSONArray(materiList).toString())
      put("updated_at", java.time.Instant.now().toString())
    }

    return@withContext try {
      val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/mapel_patron_materi?on_conflict=distribusi_id"
      val connection = createConnection(requestUrl, "POST").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
      }
      connection.outputStream.use { stream ->
        stream.write(JSONArray().put(payload).toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      connection.usePatronJsonArrayResponse { }
      GuruMapelPatronMateriSaveResult.Success
    } catch (_: SocketTimeoutException) {
      GuruMapelPatronMateriSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      GuruMapelPatronMateriSaveResult.Error("Gagal menyimpan patron materi.")
    }
  }

  private fun normalizeMateriItems(rawJson: String): NormalizedPatronMateri {
    if (rawJson.isBlank()) return NormalizedPatronMateri(emptyList(), false)
    return runCatching {
      val array = JSONArray(rawJson)
      var containsRecoveredText = false
      buildList {
        for (index in 0 until array.length()) {
          val rawText = array.optString(index).trim()
          if (rawText.isBlank()) continue
          val repaired = repairArabicWriteBytesText(rawText)
          containsRecoveredText = containsRecoveredText || repaired.wasRecovered
          add(PatronMateriItem(id = "materi-${index + 1}-${repaired.text.hashCode()}", text = repaired.text))
        }
      } to containsRecoveredText
    }.map { (items, containsRecoveredText) ->
      NormalizedPatronMateri(items, containsRecoveredText)
    }.getOrDefault(NormalizedPatronMateri(emptyList(), false))
  }

  private fun repairArabicWriteBytesText(text: String): RepairedText {
    if (text.any { it in '\u0600'..'\u06FF' }) return RepairedText(text, false)
    if (text.length < 3) return RepairedText(text, false)

    val nonWhitespace = text.count { !it.isWhitespace() }
    if (nonWhitespace == 0) return RepairedText(text, false)

    val signatureChars = text.count { it in ArabicWriteBytesSignatureChars }
    val latinLowercase = text.count { it in 'a'..'z' }
    val convertible = text.count { it.code in 0x21..0x7E }
    val likelyCorruptArabic = latinLowercase == 0 &&
      convertible >= 3 &&
      signatureChars >= 2 &&
      convertible.toFloat() / nonWhitespace >= 0.65f

    if (!likelyCorruptArabic) return RepairedText(text, false)

    val repaired = buildString {
      text.forEach { char ->
        when {
          char.isWhitespace() -> append(char)
          char.code in 0x21..0x7E -> append((0x0600 + char.code).toChar())
          else -> append(char)
        }
      }
    }
    return RepairedText(repaired, repaired != text)
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

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
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

private data class NormalizedPatronMateri(
  val items: List<PatronMateriItem>,
  val containsRecoveredText: Boolean
)

private data class RepairedText(
  val text: String,
  val wasRecovered: Boolean
)

private val ArabicWriteBytesSignatureChars = setOf(
  '\'', '(', ')', '*', '+', ',', '-', '.', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':',
  'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R'
)

private inline fun <T> HttpURLConnection.usePatronJsonArrayResponse(
  block: (JSONArray) -> T
): T {
  return try {
    val code = responseCode
    val payload = readPatronPayload(code in 200..299)
    if (code !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $code" })
    }
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readPatronPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}
