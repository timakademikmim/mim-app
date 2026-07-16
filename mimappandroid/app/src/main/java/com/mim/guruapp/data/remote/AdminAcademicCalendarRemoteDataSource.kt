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

data class AdminAcademicCalendarSnapshot(
  val events: List<AdminAcademicCalendarEvent>,
  val classes: List<AdminAcademicCalendarClassOption>
)

data class AdminAcademicCalendarEvent(
  val rowId: String,
  val title: String,
  val kind: String,
  val startDateIso: String,
  val endDateIso: String,
  val detail: String,
  val colorHex: String,
  val classIds: List<String>,
  val classNames: List<String>
)

data class AdminAcademicCalendarClassOption(
  val id: String,
  val name: String,
  val level: String
)

sealed interface AdminAcademicCalendarLoadResult {
  data class Success(val snapshot: AdminAcademicCalendarSnapshot) : AdminAcademicCalendarLoadResult
  data class Error(val message: String) : AdminAcademicCalendarLoadResult
}

sealed interface AdminAcademicCalendarSaveResult {
  data class Success(val snapshot: AdminAcademicCalendarSnapshot, val message: String) : AdminAcademicCalendarSaveResult
  data class Error(val message: String) : AdminAcademicCalendarSaveResult
}

class AdminAcademicCalendarRemoteDataSource {
  suspend fun fetchSnapshot(): AdminAcademicCalendarLoadResult = withContext(Dispatchers.IO) {
    try {
      AdminAcademicCalendarLoadResult.Success(loadSnapshot())
    } catch (_: SocketTimeoutException) {
      AdminAcademicCalendarLoadResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (error: Exception) {
      if (error.message.orEmpty().isAdminAcademicCalendarMissingTableMessage()) {
        AdminAcademicCalendarLoadResult.Error(getAdminAcademicCalendarMissingTableMessage())
      } else {
        AdminAcademicCalendarLoadResult.Error("Gagal memuat kalender akademik.")
      }
    }
  }

  suspend fun saveEvent(event: AdminAcademicCalendarEvent): AdminAcademicCalendarSaveResult = withContext(Dispatchers.IO) {
    val title = event.title.trim()
    val startDate = event.startDateIso.take(10)
    val endDate = event.endDateIso.take(10).ifBlank { startDate }
    if (title.isBlank()) return@withContext AdminAcademicCalendarSaveResult.Error("Judul kegiatan wajib diisi.")
    if (!startDate.isAdminAcademicCalendarDate()) return@withContext AdminAcademicCalendarSaveResult.Error("Tanggal mulai wajib diisi dengan format yyyy-MM-dd.")
    if (endDate.isNotBlank() && !endDate.isAdminAcademicCalendarDate()) {
      return@withContext AdminAcademicCalendarSaveResult.Error("Tanggal selesai wajib diisi dengan format yyyy-MM-dd.")
    }
    if (endDate.isNotBlank() && endDate < startDate) {
      return@withContext AdminAcademicCalendarSaveResult.Error("Tanggal selesai tidak boleh lebih kecil dari tanggal mulai.")
    }

    try {
      val fields = buildEventPayload(event.copy(title = title, startDateIso = startDate, endDateIso = endDate))
      if (event.rowId.isBlank()) {
        insertEvent(fields)
      } else {
        updateEvent(event.rowId, fields)
      }
      AdminAcademicCalendarSaveResult.Success(
        loadSnapshot(),
        if (event.rowId.isBlank()) "Kegiatan akademik berhasil ditambahkan." else "Kegiatan akademik berhasil disimpan."
      )
    } catch (_: SocketTimeoutException) {
      AdminAcademicCalendarSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (error: Exception) {
      val message = error.message.orEmpty()
      when {
        message.isAdminAcademicCalendarMissingTableMessage() ->
          AdminAcademicCalendarSaveResult.Error(getAdminAcademicCalendarMissingTableMessage())
        message.isAdminAcademicCalendarMissingColumnMessage("warna") ->
          AdminAcademicCalendarSaveResult.Error("Kolom warna belum ada di kalender_akademik.")
        message.isAdminAcademicCalendarMissingColumnMessage("jenis_kegiatan") ->
          AdminAcademicCalendarSaveResult.Error("Kolom jenis_kegiatan belum ada di kalender_akademik.")
        else -> AdminAcademicCalendarSaveResult.Error("Gagal menyimpan kegiatan akademik.")
      }
    }
  }

  suspend fun deleteEvent(rowId: String): AdminAcademicCalendarSaveResult = withContext(Dispatchers.IO) {
    val normalizedId = rowId.trim()
    if (normalizedId.isBlank()) return@withContext AdminAcademicCalendarSaveResult.Error("Kegiatan belum dipilih.")
    try {
      val connection = createConnection(
        "${BuildConfig.SUPABASE_URL}/rest/v1/kalender_akademik?id=eq.${encodeValue(normalizedId)}",
        method = "DELETE"
      )
      connection.useAdminAcademicCalendarJsonArrayResponse { }
      AdminAcademicCalendarSaveResult.Success(loadSnapshot(), "Kegiatan akademik berhasil dihapus.")
    } catch (_: SocketTimeoutException) {
      AdminAcademicCalendarSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (error: Exception) {
      if (error.message.orEmpty().isAdminAcademicCalendarMissingTableMessage()) {
        AdminAcademicCalendarSaveResult.Error(getAdminAcademicCalendarMissingTableMessage())
      } else {
        AdminAcademicCalendarSaveResult.Error("Gagal menghapus kegiatan akademik.")
      }
    }
  }

  private fun loadSnapshot(): AdminAcademicCalendarSnapshot {
    val classes = fetchRows("kelas", "select=*&order=nama_kelas.asc")
      .mapNotNull { it.toAdminAcademicCalendarClass() }
      .sortedWith(
        compareBy<AdminAcademicCalendarClassOption> { it.level.adminAcademicCalendarLevelNumber() ?: 999 }
          .thenBy { it.name.lowercase() }
      )
    val classNameById = classes.associate { it.id to it.name }
    val events = fetchCalendarRows().mapNotNull { it.toAdminAcademicCalendarEvent(classNameById) }
      .sortedWith(compareBy<AdminAcademicCalendarEvent> { it.startDateIso }.thenBy { it.title.lowercase() })
    return AdminAcademicCalendarSnapshot(events = events, classes = classes)
  }

  private fun fetchCalendarRows(): List<JSONObject> {
    val fullQuery = "select=id,judul,jenis_kegiatan,warna,mulai,selesai,detail,created_at&order=mulai.asc"
    return try {
      fetchRows("kalender_akademik", fullQuery)
    } catch (error: Exception) {
      val message = error.message.orEmpty()
      if (message.isAdminAcademicCalendarMissingTableMessage()) throw error
      val missingColor = message.isAdminAcademicCalendarMissingColumnMessage("warna")
      val missingKind = message.isAdminAcademicCalendarMissingColumnMessage("jenis_kegiatan")
      if (!missingColor && !missingKind) throw error
      val fallbackSelect = when {
        missingColor && missingKind -> "select=id,judul,mulai,selesai,detail,created_at&order=mulai.asc"
        missingColor -> "select=id,judul,jenis_kegiatan,mulai,selesai,detail,created_at&order=mulai.asc"
        else -> "select=id,judul,warna,mulai,selesai,detail,created_at&order=mulai.asc"
      }
      fetchRows("kalender_akademik", fallbackSelect)
    }
  }

  private fun buildEventPayload(event: AdminAcademicCalendarEvent): LinkedHashMap<String, Any?> {
    val kind = event.kind.normalizedAdminAcademicCalendarKind()
    val shouldStoreClassInfo = kind == "ujian" || kind == "libur_kelas"
    val detailPayload = if (shouldStoreClassInfo) {
      JSONObject().apply {
        put("text", event.detail.trim())
        put("kelas_ids", JSONArray(event.classIds.map { it.trim() }.filter { it.isNotBlank() }))
        put("kelas_nama", JSONArray(event.classNames.map { it.trim() }.filter { it.isNotBlank() }))
      }.toString()
    } else {
      event.detail.trim().ifBlank { null }
    }
    return linkedMapOf(
      "judul" to event.title.trim(),
      "jenis_kegiatan" to kind.ifBlank { null },
      "warna" to event.colorHex.normalizedAdminAcademicCalendarColor(),
      "mulai" to event.startDateIso.toAdminAcademicCalendarStoredIso(),
      "selesai" to event.endDateIso.take(10).ifBlank { event.startDateIso.take(10) }.toAdminAcademicCalendarStoredIso(),
      "detail" to detailPayload
    )
  }

  private fun insertEvent(fields: LinkedHashMap<String, Any?>) {
    val candidates = listOf(
      fields,
      LinkedHashMap(fields).apply { remove("warna") },
      LinkedHashMap(fields).apply { remove("jenis_kegiatan") },
      LinkedHashMap(fields).apply {
        remove("warna")
        remove("jenis_kegiatan")
      }
    )
    var lastError: Exception? = null
    candidates.forEach { candidate ->
      try {
        val connection = createConnection("${BuildConfig.SUPABASE_URL}/rest/v1/kalender_akademik?select=id", method = "POST").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        val payload = JSONObject().apply {
          candidate.forEach { (key, value) -> putPayloadValueForAdminAcademicCalendar(key, value) }
        }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        connection.useAdminAcademicCalendarJsonArrayResponse { rows ->
          if (rows.length() == 0) throw IllegalStateException("Data kalender tidak kembali setelah disimpan.")
        }
        return
      } catch (error: Exception) {
        lastError = error
        if (!error.message.orEmpty().isAdminAcademicCalendarMissingColumnMessage()) throw error
      }
    }
    throw lastError ?: IllegalStateException("Gagal menambahkan kegiatan.")
  }

  private fun updateEvent(rowId: String, fields: LinkedHashMap<String, Any?>) {
    fields.forEach { (field, value) ->
      try {
        val connection = createConnection(
          "${BuildConfig.SUPABASE_URL}/rest/v1/kalender_akademik?select=$field&id=eq.${encodeValue(rowId)}",
          method = "PATCH"
        ).apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        val payload = JSONObject().apply { putPayloadValueForAdminAcademicCalendar(field, value) }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        connection.useAdminAcademicCalendarJsonArrayResponse { }
      } catch (error: Exception) {
        if (!error.message.orEmpty().isAdminAcademicCalendarMissingColumnMessage(field)) throw error
      }
    }
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = createConnection(requestUrl)
    return connection.useAdminAcademicCalendarJsonArrayResponse { rows ->
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

private fun JSONObject.toAdminAcademicCalendarClass(): AdminAcademicCalendarClassOption? {
  val id = cleanStringForAdminAcademicCalendar("id")
  if (id.isBlank()) return null
  val name = firstCleanStringForAdminAcademicCalendar("nama_kelas", "kelas", "name")
  return AdminAcademicCalendarClassOption(
    id = id,
    name = name.ifBlank { "Kelas" },
    level = firstCleanStringForAdminAcademicCalendar("tingkat", "level").ifBlank { name.adminAcademicCalendarLevelNumber()?.toString().orEmpty() }
  )
}

private fun JSONObject.toAdminAcademicCalendarEvent(classNameById: Map<String, String>): AdminAcademicCalendarEvent? {
  val id = cleanStringForAdminAcademicCalendar("id")
  if (id.isBlank()) return null
  val detailPayload = parseAdminAcademicCalendarDetail(cleanStringForAdminAcademicCalendar("detail"))
  val classNames = detailPayload.classNames.ifEmpty {
    detailPayload.classIds.mapNotNull { classNameById[it]?.takeIf(String::isNotBlank) }
  }
  return AdminAcademicCalendarEvent(
    rowId = id,
    title = firstCleanStringForAdminAcademicCalendar("judul", "title").ifBlank { "Kegiatan Akademik" },
    kind = cleanStringForAdminAcademicCalendar("jenis_kegiatan").normalizedAdminAcademicCalendarKind(),
    startDateIso = cleanStringForAdminAcademicCalendar("mulai").take(10),
    endDateIso = cleanStringForAdminAcademicCalendar("selesai").take(10),
    detail = detailPayload.text,
    colorHex = cleanStringForAdminAcademicCalendar("warna").normalizedAdminAcademicCalendarColor(),
    classIds = detailPayload.classIds,
    classNames = classNames
  )
}

private data class AdminAcademicCalendarDetailPayload(
  val text: String,
  val classIds: List<String>,
  val classNames: List<String>
)

private fun parseAdminAcademicCalendarDetail(raw: String): AdminAcademicCalendarDetailPayload {
  val text = raw.trim()
  if (text.isBlank()) return AdminAcademicCalendarDetailPayload("", emptyList(), emptyList())
  val json = runCatching { JSONObject(text) }.getOrNull()
    ?: return AdminAcademicCalendarDetailPayload(text, emptyList(), emptyList())
  return AdminAcademicCalendarDetailPayload(
    text = json.firstCleanStringForAdminAcademicCalendar("text", "detail", "deskripsi"),
    classIds = json.optStringArrayForAdminAcademicCalendar("kelas_ids"),
    classNames = json.optStringArrayForAdminAcademicCalendar("kelas_nama")
  )
}

private inline fun <T> HttpURLConnection.useAdminAcademicCalendarJsonArrayResponse(
  block: (JSONArray) -> T
): T {
  return try {
    val status = responseCode
    val payload = readAdminAcademicCalendarPayload(status in 200..299)
    if (status !in 200..299) throw IllegalStateException(payload.ifBlank { "HTTP $status" })
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readAdminAcademicCalendarPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}

private fun JSONObject.cleanStringForAdminAcademicCalendar(key: String): String {
  val value = opt(key)
  if (value == null || value == JSONObject.NULL) return ""
  val text = value.toString().trim()
  return if (text.equals("null", ignoreCase = true)) "" else text
}

private fun JSONObject.firstCleanStringForAdminAcademicCalendar(vararg keys: String): String {
  return keys.firstNotNullOfOrNull { key ->
    cleanStringForAdminAcademicCalendar(key).takeIf { it.isNotBlank() }
  }.orEmpty()
}

private fun JSONObject.optStringArrayForAdminAcademicCalendar(key: String): List<String> {
  val array = optJSONArray(key) ?: return emptyList()
  return buildList {
    for (index in 0 until array.length()) {
      val text = array.opt(index)?.toString()?.trim().orEmpty()
      if (text.isNotBlank() && !text.equals("null", ignoreCase = true)) add(text)
    }
  }
}

private fun JSONObject.putPayloadValueForAdminAcademicCalendar(key: String, value: Any?) {
  put(
    key,
    when {
      value == null -> JSONObject.NULL
      value.toString().isBlank() -> JSONObject.NULL
      else -> value
    }
  )
}

private fun String.normalizedAdminAcademicCalendarColor(): String {
  val raw = trim()
  return if (Regex("^#[0-9a-fA-F]{6}$").matches(raw)) raw else "#2563eb"
}

private fun String.normalizedAdminAcademicCalendarKind(): String {
  return when (trim().lowercase()) {
    "libur_semua_kegiatan", "libur_akademik", "libur_ketahfizan", "ujian", "libur_kelas" -> trim().lowercase()
    else -> ""
  }
}

private fun String.isAdminAcademicCalendarDate(): Boolean {
  return Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(take(10))
}

private fun String.toAdminAcademicCalendarStoredIso(): String {
  val date = take(10)
  return if (date.isAdminAcademicCalendarDate()) "${date}T12:00:00Z" else this
}

private fun String.isAdminAcademicCalendarMissingTableMessage(): Boolean {
  val lower = lowercase()
  return lower.contains("kalender_akademik") && (
    lower.contains("does not exist") ||
      lower.contains("could not find the table") ||
      lower.contains("schema cache")
    )
}

private fun String.isAdminAcademicCalendarMissingColumnMessage(field: String = ""): Boolean {
  val lower = lowercase()
  if (!lower.contains("column") && !lower.contains("schema cache") && !lower.contains("pgrst")) return false
  return field.isBlank() || lower.contains(field.lowercase())
}

private fun getAdminAcademicCalendarMissingTableMessage(): String {
  return "Tabel kalender_akademik belum ada di Supabase. Buat tabel dengan kolom id, judul, jenis_kegiatan, mulai, selesai, detail, warna, dan created_at."
}

private fun String.adminAcademicCalendarLevelNumber(): Int? {
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
