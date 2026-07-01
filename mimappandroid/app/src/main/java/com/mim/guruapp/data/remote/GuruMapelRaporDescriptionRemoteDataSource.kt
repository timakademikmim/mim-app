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

sealed interface GuruMapelRaporDescriptionSaveResult {
  data object Success : GuruMapelRaporDescriptionSaveResult
  data class SuccessWithWarning(val message: String) : GuruMapelRaporDescriptionSaveResult
  data class Error(val message: String) : GuruMapelRaporDescriptionSaveResult
}

class GuruMapelRaporDescriptionRemoteDataSource {
  suspend fun fetchDescriptionJson(
    distribusiId: String,
    guruId: String = ""
  ): String? = withContext(Dispatchers.IO) {
    if (distribusiId.isBlank()) return@withContext null
    runCatching {
      val row = fetchRows(
        table = "rapor_deskripsi_mapel",
        query = buildString {
          append("select=*")
          append("&distribusi_id=eq.")
          append(encodeValue(distribusiId))
          append("&limit=1")
        }
      ).firstOrNull()
      val exactRow = row?.takeIf { it.hasAnyRaporDescription() }
      val fallbackRow = if (exactRow == null) {
        fetchFallbackDescriptionRow(distribusiId, guruId)
      } else {
        null
      }
      (exactRow ?: fallbackRow ?: row)?.toDescriptionJson()
    }.getOrNull()
  }

  suspend fun saveDescriptionJson(
    distribusiId: String,
    rawJson: String,
    guruId: String
  ): GuruMapelRaporDescriptionSaveResult = withContext(Dispatchers.IO) {
    if (distribusiId.isBlank()) {
      return@withContext GuruMapelRaporDescriptionSaveResult.Error("Data distribusi mapel belum lengkap.")
    }

    val distribusiRow = fetchRows(
      table = "distribusi_mapel",
      query = "select=id,mapel_id,semester_id&id=eq.${encodeValue(distribusiId)}&limit=1"
    ).firstOrNull()
      ?: return@withContext GuruMapelRaporDescriptionSaveResult.Error("Distribusi mapel tidak ditemukan.")

    val template = runCatching { JSONObject(rawJson.ifBlank { "{}" }) }.getOrDefault(JSONObject())
    val payload = JSONObject().apply {
      put("distribusi_id", distribusiId)
      put("guru_id", guruId.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
      put("mapel_id", distribusiRow.optString("mapel_id").trim().ifBlank { JSONObject.NULL })
      put("semester_id", distribusiRow.optString("semester_id").trim().ifBlank { JSONObject.NULL })
      put("deskripsi_a_pengetahuan", template.descriptionValue("knowledgeDescriptions", "A"))
      put("deskripsi_b_pengetahuan", template.descriptionValue("knowledgeDescriptions", "B"))
      put("deskripsi_c_pengetahuan", template.descriptionValue("knowledgeDescriptions", "C"))
      put("deskripsi_d_pengetahuan", template.descriptionValue("knowledgeDescriptions", "D"))
      put("deskripsi_e_pengetahuan", template.descriptionValue("knowledgeDescriptions", "E"))
      put("deskripsi_a_keterampilan", template.descriptionValue("skillDescriptions", "A"))
      put("deskripsi_b_keterampilan", template.descriptionValue("skillDescriptions", "B"))
      put("deskripsi_c_keterampilan", template.descriptionValue("skillDescriptions", "C"))
      put("deskripsi_d_keterampilan", template.descriptionValue("skillDescriptions", "D"))
      put("deskripsi_e_keterampilan", template.descriptionValue("skillDescriptions", "E"))
      put("updated_at", Instant.now().toString())
    }

    return@withContext try {
      postDescriptionPayload(payload)
      GuruMapelRaporDescriptionSaveResult.Success
    } catch (_: SocketTimeoutException) {
      GuruMapelRaporDescriptionSaveResult.Error("Koneksi ke server terlalu lama. Deskripsi rapor tetap tersimpan di perangkat ini.")
    } catch (error: Exception) {
      if (isMissingRaporDescriptionEColumn(error)) {
        val hasPredicateEValue = template.descriptionText("knowledgeDescriptions", "E").isNotBlank() ||
          template.descriptionText("skillDescriptions", "E").isNotBlank()
        val legacyPayload = JSONObject(payload.toString()).apply {
          remove("deskripsi_e_pengetahuan")
          remove("deskripsi_e_keterampilan")
        }
        return@withContext try {
          postDescriptionPayload(legacyPayload)
          if (hasPredicateEValue) {
            GuruMapelRaporDescriptionSaveResult.SuccessWithWarning("Deskripsi A-D berhasil disimpan ke database. Deskripsi E tersimpan di perangkat ini; jalankan SQL setup rapor agar E bisa sinkron antar perangkat.")
          } else {
            GuruMapelRaporDescriptionSaveResult.Success
          }
        } catch (_: SocketTimeoutException) {
          GuruMapelRaporDescriptionSaveResult.Error("Koneksi ke server terlalu lama. Deskripsi rapor tetap tersimpan di perangkat ini.")
        } catch (_: Exception) {
          GuruMapelRaporDescriptionSaveResult.Error("Gagal menyimpan deskripsi rapor ke server.")
        }
      }
      GuruMapelRaporDescriptionSaveResult.Error("Gagal menyimpan deskripsi rapor ke server.")
    }
  }

  private fun postDescriptionPayload(payload: JSONObject) {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/rapor_deskripsi_mapel?on_conflict=distribusi_id"
    val connection = createConnection(requestUrl, "POST").apply {
      doOutput = true
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
    }
    connection.outputStream.use { stream ->
      stream.write(JSONArray().put(payload).toString().toByteArray(Charsets.UTF_8))
      stream.flush()
    }
    connection.useRaporDescriptionResponse { }
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

  private fun fetchFallbackDescriptionRow(
    distribusiId: String,
    guruId: String
  ): JSONObject? {
    val distribusiRow = fetchRows(
      table = "distribusi_mapel",
      query = "select=id,mapel_id,semester_id&id=eq.${encodeValue(distribusiId)}&limit=1"
    ).firstOrNull() ?: return null

    val mapelId = distribusiRow.optString("mapel_id").trim()
    val semesterId = distribusiRow.optString("semester_id").trim()
    if (mapelId.isBlank() || semesterId.isBlank()) return null

    val rows = fetchRows(
      table = "rapor_deskripsi_mapel",
      query = buildString {
        append("select=*")
        append("&mapel_id=eq.")
        append(encodeValue(mapelId))
        append("&semester_id=eq.")
        append(encodeValue(semesterId))
        append("&order=updated_at.desc")
        append("&limit=20")
      }
    )
    val normalizedGuruId = guruId.trim()
    return rows.firstOrNull { row ->
      normalizedGuruId.isNotBlank() &&
        row.optString("guru_id").trim() == normalizedGuruId &&
        row.hasAnyRaporDescription()
    } ?: rows.firstOrNull { it.hasAnyRaporDescription() } ?: rows.firstOrNull()
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

private fun JSONObject.toDescriptionJson(): String {
  return JSONObject().apply {
    put("knowledgeDescriptions", JSONObject().apply {
      put("A", optString("deskripsi_a_pengetahuan"))
      put("B", optString("deskripsi_b_pengetahuan"))
      put("C", optString("deskripsi_c_pengetahuan"))
      put("D", optString("deskripsi_d_pengetahuan"))
      put("E", optString("deskripsi_e_pengetahuan"))
    })
    put("skillDescriptions", JSONObject().apply {
      put("A", optString("deskripsi_a_keterampilan"))
      put("B", optString("deskripsi_b_keterampilan"))
      put("C", optString("deskripsi_c_keterampilan"))
      put("D", optString("deskripsi_d_keterampilan"))
      put("E", optString("deskripsi_e_keterampilan"))
    })
  }.toString()
}

private fun JSONObject.hasAnyRaporDescription(): Boolean {
  return listOf(
    "deskripsi_a_pengetahuan",
    "deskripsi_b_pengetahuan",
    "deskripsi_c_pengetahuan",
    "deskripsi_d_pengetahuan",
    "deskripsi_e_pengetahuan",
    "deskripsi_a_keterampilan",
    "deskripsi_b_keterampilan",
    "deskripsi_c_keterampilan",
    "deskripsi_d_keterampilan",
    "deskripsi_e_keterampilan"
  ).any { key -> optString(key).trim().isNotBlank() }
}

private fun JSONObject.descriptionText(sectionKey: String, predicate: String): String {
  return optJSONObject(sectionKey)
    ?.optString(predicate)
    .orEmpty()
    .trim()
}

private fun JSONObject.descriptionValue(sectionKey: String, predicate: String): Any {
  val value = descriptionText(sectionKey, predicate)
  return value.takeIf { it.isNotBlank() } ?: JSONObject.NULL
}

private inline fun <T> HttpURLConnection.useRaporDescriptionResponse(
  block: (JSONArray) -> T
): T {
  return try {
    val code = responseCode
    val payload = readRaporDescriptionPayload(code in 200..299)
    if (code !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $code" })
    }
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readRaporDescriptionPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}

private fun isMissingRaporDescriptionEColumn(error: Throwable): Boolean {
  val text = error.message.orEmpty().lowercase()
  return "deskripsi_e_" in text &&
    ("column" in text || "pgrst204" in text || "42703" in text || "does not exist" in text)
}
