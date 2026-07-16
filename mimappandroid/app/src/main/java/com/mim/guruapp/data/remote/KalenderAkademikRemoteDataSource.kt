package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class KalenderAkademikRemoteDataSource {
  suspend fun fetchCalendarEvents(
    teacherRowId: String,
    teacherKaryawanId: String
  ): List<CalendarEvent>? = withContext(Dispatchers.IO) {
    runCatching {
      val activeTahunAjaranId = resolveActiveTahunAjaranId()
      val rows = fetchCalendarRows(activeTahunAjaranId)
        val calendarItems = buildList {
          rows.forEachIndexed { index, item ->
            val startDateIso = normalizeDateIso(item.optString("mulai"))
            if (startDateIso.isBlank()) return@forEachIndexed
            val endDateIso = normalizeDateIso(item.optString("selesai")).ifBlank { startDateIso }
            val title = item.displayString("judul").ifBlank { "Agenda Akademik" }
            val description = item.displayString("detail")
            val jenisKegiatan = item.displayString("jenis_kegiatan")
            add(
              CalendarEvent(
                id = item.cleanScalar("id").ifBlank { "kalender-$index" },
                startDateIso = startDateIso,
                endDateIso = endDateIso,
                title = title,
                description = description,
                timeLabel = "",
                colorHex = normalizeColor(item.optString("warna")),
                categoryKey = inferCategoryKey(
                  jenisKegiatan = jenisKegiatan,
                  title = title,
                  description = description
                )
              )
            )
          }
        }
        calendarItems.sortedWith(compareBy<CalendarEvent> { it.startDateIso }.thenBy { it.timeLabel }.thenBy { it.title })
    }.getOrNull()
  }

  private fun resolveActiveTahunAjaranId(): String {
    return runCatching {
      fetchRows("tahun_ajaran", "select=id,aktif&aktif=eq.true&order=id.desc&limit=1")
        .firstOrNull()
        ?.cleanScalar("id")
        .orEmpty()
    }.getOrDefault("")
  }

  private fun fetchCalendarRows(activeTahunAjaranId: String): List<JSONObject> {
    val selectWithYear = "select=id,judul,mulai,selesai,detail,warna,jenis_kegiatan,tahun_ajaran_id"
    val selectWithoutYear = "select=id,judul,mulai,selesai,detail,warna,jenis_kegiatan"
    val activeYearFilter = if (activeTahunAjaranId.isNotBlank()) {
      "&tahun_ajaran_id=eq.${encodeValue(activeTahunAjaranId)}"
    } else {
      ""
    }
    return try {
      fetchRows(
        table = "kalender_akademik",
        query = "$selectWithYear$activeYearFilter&order=mulai.asc"
      )
    } catch (error: Exception) {
      if (!error.message.orEmpty().isMissingCalendarYearColumnMessage()) throw error
      fetchRows(
        table = "kalender_akademik",
        query = "$selectWithoutYear&order=mulai.asc"
      )
    }
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val url = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 15_000
      readTimeout = 15_000
      applySupabaseRequestHeaders()
      setRequestProperty("Accept", "application/json")
    }
    return try {
      val success = connection.responseCode in 200..299
      val stream = if (success) connection.inputStream else connection.errorStream
      val payload = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
      if (!success) throw IllegalStateException(payload.ifBlank { "HTTP ${connection.responseCode}" })
      val rows = JSONArray(payload.ifBlank { "[]" })
      List(rows.length()) { index -> rows.optJSONObject(index) }.filterNotNull()
    } finally {
      connection.disconnect()
    }
  }

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
  }

  private fun JSONObject.displayString(key: String): String {
    return normalizeDisplayString(opt(key))
  }

  private fun JSONObject.cleanScalar(key: String): String {
    val value = opt(key)
    if (value == null || value == JSONObject.NULL) return ""
    return value.toString().trim().takeUnless { it.equals("null", ignoreCase = true) }.orEmpty()
  }

  private fun normalizeDisplayString(value: Any?): String {
    if (value == null || value == JSONObject.NULL) return ""
    val raw = value.toString().trim()
    if (raw.isBlank() || raw.equals("null", ignoreCase = true)) return ""
    if (raw.looksLikeTechnicalJsonText()) return ""
    if (!raw.startsWith("{") && !raw.startsWith("[")) return raw

    val parsedObject = runCatching { JSONObject(raw) }.getOrNull()
    if (parsedObject != null) return extractTextFromJsonObject(parsedObject)

    val parsedArray = runCatching { JSONArray(raw) }.getOrNull() ?: return ""
    return buildList {
      for (index in 0 until parsedArray.length()) {
        val child = parsedArray.opt(index)
        val text = when (child) {
          is JSONObject -> extractTextFromJsonObject(child)
          else -> normalizeDisplayString(child)
        }
        if (text.isHumanReadableCalendarText()) add(text)
      }
    }.distinct().joinToString(", ")
  }

  private fun extractTextFromJsonObject(json: JSONObject): String {
    val preferredKeys = listOf("detail", "keterangan", "deskripsi", "description", "body", "content", "text", "label", "title", "judul", "value")
    for (key in preferredKeys) {
      val text = normalizeDisplayString(json.opt(key))
      if (text.isHumanReadableCalendarText()) return text
    }
    val markerText = normalizeDisplayString(json.opt("text")).lowercase()
    if (markerText.endsWith("_id") || markerText.endsWith("_ids")) return ""

    return buildList {
      val keys = json.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val text = normalizeDisplayString(json.opt(key))
        if (text.isHumanReadableCalendarText()) add(text)
      }
    }.distinct().joinToString(", ")
  }

  private fun String.isHumanReadableCalendarText(): Boolean {
    val text = trim()
    if (text.isBlank() || text.equals("null", ignoreCase = true)) return false
    if (text.looksLikeTechnicalJsonText()) return false
    if (text.startsWith("{") || text.startsWith("[")) return false
    val lower = text.lowercase()
    val uuidLike = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)
    if (uuidLike.matches(text)) return false
    val technicalTokens = listOf(
      "kelas_id",
      "kelas_ids",
      "mapel_id",
      "mapel_ids",
      "semester_id",
      "tahun_ajaran_id",
      "created_at",
      "updated_at"
    )
    return technicalTokens.none { lower == it || lower.contains("\"$it\"") }
  }

  private fun String.looksLikeTechnicalJsonText(): Boolean {
    val lower = trim().lowercase()
    if (!lower.contains("text") || !lower.contains("value")) return false
    val technicalTokens = listOf("kelas_id", "kelas_ids", "mapel_id", "mapel_ids", "semester_id", "tahun_ajaran_id")
    return technicalTokens.any { token ->
      lower.contains("\"text\":\"$token\"") ||
        lower.contains("\"text\": \"$token\"") ||
        lower.contains("\\\"text\\\":\\\"$token\\\"") ||
        lower.contains("\\\"text\\\": \\\"$token\\\"")
    }
  }

  private fun String.isMissingCalendarYearColumnMessage(): Boolean {
    val lower = lowercase()
    return lower.contains("tahun_ajaran_id") &&
      (lower.contains("column") || lower.contains("pgrst"))
  }

  private fun normalizeDateIso(value: String?): String {
    val text = value.orEmpty().trim()
    if (text.isBlank()) return ""
    return text.take(10)
  }

  private fun normalizeColor(value: String?): String {
    val raw = value.orEmpty().trim()
    return if (Regex("^#[0-9a-fA-F]{6}$").matches(raw)) raw else "#2563eb"
  }

  private fun inferCategoryKey(
    jenisKegiatan: String?,
    title: String?,
    description: String?
  ): String {
    val normalizedJenis = jenisKegiatan.orEmpty().trim().lowercase()
    if (normalizedJenis == "libur_semua_kegiatan") return "libur_semua_kegiatan"
    if (normalizedJenis == "libur_akademik") return "libur_akademik"
    if (normalizedJenis == "libur_ketahfizan") return "libur_ketahfizan"
    val text = buildString {
      append(title.orEmpty())
      append(' ')
      append(description.orEmpty())
    }.trim().lowercase()
    return when {
      "libur semua" in text -> "libur_semua_kegiatan"
      "libur akademik" in text -> "libur_akademik"
      "libur ketahfiz" in text -> "libur_ketahfizan"
      else -> "kegiatan_umum"
    }
  }
}
