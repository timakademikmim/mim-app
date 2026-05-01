package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.MutabaahSnapshot
import com.mim.guruapp.data.model.MutabaahSubmission
import com.mim.guruapp.data.model.MutabaahTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

sealed interface GuruMutabaahSaveResult {
  data class Success(val submission: MutabaahSubmission) : GuruMutabaahSaveResult
  data class Error(val message: String) : GuruMutabaahSaveResult
}

sealed interface GuruMutabaahBatchSaveResult {
  data class Success(val submissions: List<MutabaahSubmission>) : GuruMutabaahBatchSaveResult
  data class Error(val message: String) : GuruMutabaahBatchSaveResult
}

class GuruMutabaahRemoteDataSource {
  suspend fun fetchMutabaahSnapshot(
    teacherRowId: String,
    teacherKaryawanId: String,
    referenceDate: LocalDate
  ): MutabaahSnapshot? = withContext(Dispatchers.IO) {
    runCatching {
      val guruId = teacherRowId.trim().ifBlank { resolveTeacherRowId(teacherKaryawanId) }
      if (guruId.isBlank()) return@runCatching null

      val period = YearMonth.from(referenceDate)
      val start = period.atDay(1)
      val end = period.atEndOfMonth()
      val activeYearId = resolveActiveTahunAjaranId()
      val holidays = fetchAcademicHolidayDates(start, end)

      val templateQuery = buildString {
        append("select=id,tahun_ajaran_id,tanggal,judul,deskripsi,aktif,created_at")
        append("&tanggal=gte.")
        append(start)
        append("&tanggal=lte.")
        append(end)
        if (activeYearId.isNotBlank()) {
          append("&tahun_ajaran_id=eq.")
          append(encodeValue(activeYearId))
        }
        append("&order=tanggal.asc&order=created_at.asc")
      }
      val templateRows = fetchRows("tugas_harian_template", templateQuery)
      val submissionRows = fetchRows(
        table = "tugas_harian_submit",
        query = buildString {
          append("select=id,template_id,guru_id,tanggal,status,catatan,submitted_at,updated_at")
          append("&guru_id=eq.")
          append(encodeValue(guruId))
          append("&tanggal=gte.")
          append(start)
          append("&tanggal=lte.")
          append(end)
        }
      )

      val tasks = templateRows
        .mapNotNull { row ->
          val dateIso = row.cleanString("tanggal").take(10)
          val id = row.cleanString("id")
          if (id.isBlank() || dateIso.isBlank() || isSunday(dateIso) || holidays.contains(dateIso)) return@mapNotNull null
          MutabaahTask(
            id = id,
            dateIso = dateIso,
            title = row.cleanString("judul").ifBlank { "Tugas Mutabaah" },
            description = row.cleanString("deskripsi"),
            active = row.opt("aktif") != false && row.optString("aktif").lowercase() != "false"
          )
        }
      val submissions = submissionRows
        .mapNotNull { row ->
          val dateIso = row.cleanString("tanggal").take(10)
          val templateId = row.cleanString("template_id")
          if (dateIso.isBlank() || templateId.isBlank() || holidays.contains(dateIso)) return@mapNotNull null
          MutabaahSubmission(
            id = row.cleanString("id"),
            templateId = templateId,
            dateIso = dateIso,
            status = normalizeStatus(row.cleanString("status")),
            note = row.cleanString("catatan"),
            submittedAt = row.cleanString("submitted_at"),
            updatedAt = row.cleanString("updated_at"),
            syncState = "synced"
          )
        }

      MutabaahSnapshot(
        guruId = guruId,
        period = period.toString(),
        tasks = tasks,
        submissions = submissions,
        academicHolidayDates = holidays.sorted(),
        updatedAt = System.currentTimeMillis()
      )
    }.getOrNull()
  }

  suspend fun saveMutabaahStatus(
    guruId: String,
    templateId: String,
    dateIso: String,
    isDone: Boolean,
    note: String = ""
  ): GuruMutabaahSaveResult = withContext(Dispatchers.IO) {
    val normalizedGuruId = guruId.trim()
    val normalizedTemplateId = templateId.trim()
    val normalizedDate = dateIso.trim().take(10)
    if (normalizedGuruId.isBlank() || normalizedTemplateId.isBlank() || normalizedDate.isBlank()) {
      return@withContext GuruMutabaahSaveResult.Error("Data mutabaah belum lengkap.")
    }

    runCatching {
      val nowIso = Instant.now().toString()
      val payload = JSONArray().apply {
        put(
          JSONObject().apply {
            put("template_id", normalizedTemplateId)
            put("guru_id", normalizedGuruId)
            put("tanggal", normalizedDate)
            put("status", if (isDone) "selesai" else "belum")
            put("catatan", note.trim().ifBlank { JSONObject.NULL })
            put("submitted_at", nowIso)
            put("updated_at", nowIso)
          }
        )
      }
      val requestUrl = buildString {
        append(BuildConfig.SUPABASE_URL)
        append("/rest/v1/tugas_harian_submit?on_conflict=template_id,guru_id,tanggal&select=id,template_id,guru_id,tanggal,status,catatan,submitted_at,updated_at")
      }
      val connection = createConnection(requestUrl, method = "POST").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
      }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      connection.useJsonArrayResponse { rows ->
        val row = rows.optJSONObject(0)
        GuruMutabaahSaveResult.Success(
          MutabaahSubmission(
            id = row?.cleanString("id").orEmpty(),
            templateId = row?.cleanString("template_id").orEmpty().ifBlank { normalizedTemplateId },
            dateIso = row?.cleanString("tanggal").orEmpty().take(10).ifBlank { normalizedDate },
            status = normalizeStatus(row?.cleanString("status").orEmpty().ifBlank { if (isDone) "selesai" else "belum" }),
            note = row?.cleanString("catatan").orEmpty(),
            submittedAt = row?.cleanString("submitted_at").orEmpty(),
            updatedAt = row?.cleanString("updated_at").orEmpty(),
            syncState = "synced"
          )
        )
      }
    }.getOrElse { error ->
      GuruMutabaahSaveResult.Error(error.message.orEmpty().ifBlank { "Gagal menyinkronkan mutabaah." })
    }
  }

  suspend fun saveMutabaahStatuses(
    guruId: String,
    templateIds: List<String>,
    dateIso: String,
    status: String,
    note: String = ""
  ): GuruMutabaahBatchSaveResult = withContext(Dispatchers.IO) {
    val normalizedGuruId = guruId.trim()
    val normalizedTemplateIds = templateIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    val normalizedDate = dateIso.trim().take(10)
    val normalizedStatus = normalizeStatus(status)
    if (normalizedGuruId.isBlank() || normalizedTemplateIds.isEmpty() || normalizedDate.isBlank()) {
      return@withContext GuruMutabaahBatchSaveResult.Error("Data mutabaah belum lengkap.")
    }

    runCatching {
      val nowIso = Instant.now().toString()
      val payload = JSONArray().apply {
        normalizedTemplateIds.forEach { templateId ->
          put(
            JSONObject().apply {
              put("template_id", templateId)
              put("guru_id", normalizedGuruId)
              put("tanggal", normalizedDate)
              put("status", normalizedStatus)
              put("catatan", note.trim().ifBlank { JSONObject.NULL })
              put("submitted_at", nowIso)
              put("updated_at", nowIso)
            }
          )
        }
      }
      val requestUrl = buildString {
        append(BuildConfig.SUPABASE_URL)
        append("/rest/v1/tugas_harian_submit?on_conflict=template_id,guru_id,tanggal&select=id,template_id,guru_id,tanggal,status,catatan,submitted_at,updated_at")
      }
      val connection = createConnection(requestUrl, method = "POST").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
      }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      connection.useJsonArrayResponse { rows ->
        val savedRows = List(rows.length()) { index -> rows.optJSONObject(index) }.filterNotNull()
        GuruMutabaahBatchSaveResult.Success(
          normalizedTemplateIds.map { templateId ->
            val row = savedRows.firstOrNull { it.cleanString("template_id") == templateId }
            row.toMutabaahSubmission(
              fallbackTemplateId = templateId,
              fallbackDateIso = normalizedDate,
              fallbackStatus = normalizedStatus
            )
          }
        )
      }
    }.getOrElse { error ->
      GuruMutabaahBatchSaveResult.Error(error.message.orEmpty().ifBlank { "Gagal menyinkronkan mutabaah." })
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

  private fun resolveActiveTahunAjaranId(): String {
    return runCatching {
      fetchRows("tahun_ajaran", "select=id&aktif=eq.true&limit=1")
        .firstOrNull()
        ?.cleanString("id")
        .orEmpty()
    }.getOrDefault("")
  }

  private fun fetchAcademicHolidayDates(start: LocalDate, end: LocalDate): Set<String> {
    val rows = fetchRows(
      table = "kalender_akademik",
      query = "select=mulai,selesai,jenis_kegiatan,judul,detail&mulai=lte.$end&selesai=gte.$start"
    )
    return buildSet {
      rows.forEach { row ->
        val categoryText = buildString {
          append(row.cleanString("jenis_kegiatan"))
          append(' ')
          append(row.cleanString("judul"))
          append(' ')
          append(row.cleanString("detail"))
        }.lowercase()
        val isHoliday = "libur_akademik" in categoryText ||
          "libur_semua" in categoryText ||
          "libur akademik" in categoryText ||
          "libur semua" in categoryText
        if (!isHoliday) return@forEach
        val itemStart = parseDate(row.cleanString("mulai")) ?: return@forEach
        val itemEnd = parseDate(row.cleanString("selesai")) ?: itemStart
        var cursor = maxOf(itemStart, start)
        val last = minOf(itemEnd, end)
        while (!cursor.isAfter(last)) {
          add(cursor.toString())
          cursor = cursor.plusDays(1)
        }
      }
    }
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = createConnection(requestUrl)
    return connection.useJsonArrayResponse { rows ->
      List(rows.length()) { index -> rows.optJSONObject(index) }.filterNotNull()
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

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value.trim(), Charsets.UTF_8.name())
  }

  private fun normalizeStatus(value: String): String {
    return when (value.trim().lowercase()) {
      "selesai", "done", "hadir" -> "selesai"
      "izin", "ijin", "leave" -> "izin"
      else -> "belum"
    }
  }

  private fun isSunday(dateIso: String): Boolean {
    return parseDate(dateIso)?.dayOfWeek == DayOfWeek.SUNDAY
  }

  private fun parseDate(value: String): LocalDate? {
    return runCatching { LocalDate.parse(value.trim().take(10)) }.getOrNull()
  }
}

private fun JSONObject?.toMutabaahSubmission(
  fallbackTemplateId: String,
  fallbackDateIso: String,
  fallbackStatus: String
): MutabaahSubmission {
  return MutabaahSubmission(
    id = this?.cleanString("id").orEmpty(),
    templateId = this?.cleanString("template_id").orEmpty().ifBlank { fallbackTemplateId },
    dateIso = this?.cleanString("tanggal").orEmpty().take(10).ifBlank { fallbackDateIso },
    status = when (this?.cleanString("status").orEmpty().trim().lowercase().ifBlank { fallbackStatus }) {
      "selesai", "done", "hadir" -> "selesai"
      "izin", "ijin", "leave" -> "izin"
      else -> "belum"
    },
    note = this?.cleanString("catatan").orEmpty(),
    submittedAt = this?.cleanString("submitted_at").orEmpty(),
    updatedAt = this?.cleanString("updated_at").orEmpty(),
    syncState = "synced"
  )
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
