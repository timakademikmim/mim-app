package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.SubjectOverview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class GuruTeachingScheduleRemoteDataSource {
  suspend fun fetchTeachingEvents(
    teacherRowId: String,
    teacherKaryawanId: String,
    startDate: LocalDate,
    endDate: LocalDate,
    holidayDates: Set<String> = emptySet()
  ): List<CalendarEvent>? = withContext(Dispatchers.IO) {
    runCatching {
      val distribusiRows = fetchDistribusiRows(teacherRowId, teacherKaryawanId)
      if (distribusiRows.isEmpty()) return@runCatching emptyList()

      val jadwalRows = fetchJadwalRows(distribusiRows.map { it.id })
      if (jadwalRows.isEmpty()) return@runCatching emptyList()

      val kelasMap = fetchNameMap("kelas", "nama_kelas", distribusiRows.map { it.kelasId })
      val mapelMap = fetchNameMap("mapel", "nama", distribusiRows.map { it.mapelId })
      val distribusiMap = distribusiRows.associateBy { it.id }
      val result = mutableListOf<CalendarEvent>()

      var cursor = startDate
      while (!cursor.isAfter(endDate)) {
        val expectedDay = normalizeHari(cursor.dayOfWeek)
        jadwalRows.forEach { jadwal ->
          if (holidayDates.contains(cursor.toString())) return@forEach
          if (normalizeHari(jadwal.hari) != expectedDay) return@forEach
          val distribusi = distribusiMap[jadwal.distribusiId] ?: return@forEach
          val kelasLabel = kelasMap[distribusi.kelasId].orEmpty().ifBlank { "-" }
          val mapelLabel = mapelMap[distribusi.mapelId].orEmpty().ifBlank { "-" }
          result += CalendarEvent(
            id = "teaching|${cursor}|${jadwal.id}|${distribusi.id}",
            startDateIso = cursor.toString(),
            endDateIso = cursor.toString(),
            title = "$mapelLabel - $kelasLabel",
            description = "Jadwal mengajar reguler guru pada ${formatNotifDateLabel(cursor)}.",
            timeLabel = "${toTimeLabel(jadwal.jamMulai)}-${toTimeLabel(jadwal.jamSelesai)}",
            colorHex = CATEGORY_TEACHING_COLOR,
            categoryKey = CATEGORY_TEACHING,
            lessonSlotId = jadwal.jamPelajaranId,
            distribusiId = distribusi.id
          )
        }
        cursor = cursor.plusDays(1)
      }

      result
        .sortedWith(compareBy<CalendarEvent> { it.startDateIso }.thenBy { it.timeLabel }.thenBy { it.title })
    }.getOrNull()
  }

  suspend fun fetchTeachingContextForDate(
    teacherRowId: String,
    teacherKaryawanId: String,
    date: LocalDate
  ): Pair<List<SubjectOverview>, List<CalendarEvent>>? = withContext(Dispatchers.IO) {
    runCatching {
      val distribusiRows = fetchDistribusiRows(teacherRowId, teacherKaryawanId)
      if (distribusiRows.isEmpty()) return@runCatching emptyList<SubjectOverview>() to emptyList()

      val expectedDay = normalizeHari(date.dayOfWeek)
      val jadwalRows = fetchJadwalRows(distribusiRows.map { it.id })
        .filter { jadwal -> normalizeHari(jadwal.hari) == expectedDay }
      if (jadwalRows.isEmpty()) return@runCatching emptyList<SubjectOverview>() to emptyList()

      val scheduledDistribusiIds = jadwalRows.map { it.distribusiId }.toSet()
      val scheduledDistribusiRows = distribusiRows.filter { scheduledDistribusiIds.contains(it.id) }
      val kelasMap = fetchNameMap("kelas", "nama_kelas", scheduledDistribusiRows.map { it.kelasId })
      val mapelMap = fetchNameMap("mapel", "nama", scheduledDistribusiRows.map { it.mapelId })
      val distribusiMap = scheduledDistribusiRows.associateBy { it.id }

      val subjects = scheduledDistribusiRows
        .map { distribusi ->
          SubjectOverview(
            id = distribusi.id,
            title = mapelMap[distribusi.mapelId].orEmpty().ifBlank { "Mapel" },
            className = kelasMap[distribusi.kelasId].orEmpty().ifBlank { "-" },
            semester = "Jadwal Pengganti",
            semesterActive = true,
            attendancePending = 0,
            scorePending = 0,
            materialCount = 0
          )
        }
        .distinctBy { it.id }
        .sortedWith(compareBy<SubjectOverview> { it.className.lowercase() }.thenBy { it.title.lowercase() })

      val events = jadwalRows.mapNotNull { jadwal ->
        val distribusi = distribusiMap[jadwal.distribusiId] ?: return@mapNotNull null
        val kelasLabel = kelasMap[distribusi.kelasId].orEmpty().ifBlank { "-" }
        val mapelLabel = mapelMap[distribusi.mapelId].orEmpty().ifBlank { "-" }
        CalendarEvent(
          id = "source-teaching|${date}|${jadwal.id}|${distribusi.id}",
          startDateIso = date.toString(),
          endDateIso = date.toString(),
          title = "$mapelLabel - $kelasLabel",
          description = "Jadwal guru yang digantikan pada ${formatNotifDateLabel(date)}.",
          timeLabel = "${toTimeLabel(jadwal.jamMulai)}-${toTimeLabel(jadwal.jamSelesai)}",
          colorHex = CATEGORY_TEACHING_COLOR,
          categoryKey = CATEGORY_TEACHING,
          lessonSlotId = jadwal.jamPelajaranId,
          distribusiId = distribusi.id
        )
      }.sortedWith(compareBy<CalendarEvent> { it.timeLabel }.thenBy { it.title })

      subjects to events
    }.getOrNull()
  }

  private fun fetchDistribusiRows(
    teacherRowId: String,
    teacherKaryawanId: String
  ): List<DistribusiRow> {
    val queries = buildList {
      if (teacherRowId.isNotBlank()) {
        add("select=id,kelas_id,mapel_id,guru_id,semester_id&guru_id=eq.${URLEncoder.encode(teacherRowId, Charsets.UTF_8.name())}")
      }
      if (teacherKaryawanId.isNotBlank()) {
        add("select=id,kelas_id,mapel_id,guru_id,semester_id&guru_id=eq.${URLEncoder.encode(teacherKaryawanId, Charsets.UTF_8.name())}")
      }
    }
    val rows = queries.firstNotNullOfOrNull { query ->
      val found = fetchRows("distribusi_mapel", query)
      found.takeIf { it.isNotEmpty() }
    } ?: emptyList()
    return rows.mapNotNull { row ->
      val id = row.opt("id")?.toString().orEmpty().trim()
      val kelasId = row.optString("kelas_id").trim()
      val mapelId = row.optString("mapel_id").trim()
      if (id.isBlank() || kelasId.isBlank() || mapelId.isBlank()) null
      else DistribusiRow(id = id, kelasId = kelasId, mapelId = mapelId)
    }
  }

  private fun fetchJadwalRows(distribusiIds: List<String>): List<JadwalRow> {
    if (distribusiIds.isEmpty()) return emptyList()
    val inClause = distribusiIds.distinct().joinToString(",") { "\"${it}\"" }
    val rowsWithJamPelajaran = fetchRows(
      "jadwal_pelajaran",
      "select=id,distribusi_id,hari,jam_mulai,jam_selesai,jam_pelajaran_id&distribusi_id=in.($inClause)"
    )
    val rows = rowsWithJamPelajaran.ifEmpty {
      fetchRows(
        "jadwal_pelajaran",
        "select=id,distribusi_id,hari,jam_mulai,jam_selesai&distribusi_id=in.($inClause)"
      )
    }
    val mappedRows = rows.mapNotNull { row ->
      val id = row.opt("id")?.toString().orEmpty().trim()
      val distribusiId = row.optString("distribusi_id").trim()
      val hari = row.optString("hari").trim()
      if (id.isBlank() || distribusiId.isBlank() || hari.isBlank()) null
      else JadwalRow(
        id = id,
        distribusiId = distribusiId,
        hari = hari,
        jamMulai = row.optString("jam_mulai").trim(),
        jamSelesai = row.optString("jam_selesai").trim(),
        jamPelajaranId = row.optString("jam_pelajaran_id").trim()
      )
    }
    if (mappedRows.none { it.jamPelajaranId.isBlank() }) return mappedRows

    val fallbackJamIdsByTime = fetchJamIdsByTime()
    if (fallbackJamIdsByTime.isEmpty()) return mappedRows
    return mappedRows.map { row ->
      if (row.jamPelajaranId.isNotBlank()) {
        row
      } else {
        row.copy(
          jamPelajaranId = fallbackJamIdsByTime[jamTimeKey(row.jamMulai, row.jamSelesai)].orEmpty()
        )
      }
    }
  }

  private fun fetchJamIdsByTime(): Map<String, String> {
    return fetchRows("jam_pelajaran", "select=id,jam_mulai,jam_selesai")
      .mapNotNull { row ->
        val id = row.opt("id")?.toString().orEmpty().trim()
        val key = jamTimeKey(row.optString("jam_mulai"), row.optString("jam_selesai"))
        if (id.isBlank() || key.isBlank()) null else key to id
      }
      .toMap()
  }

  private fun fetchNameMap(table: String, nameField: String, ids: List<String>): Map<String, String> {
    if (ids.isEmpty()) return emptyMap()
    val inClause = ids.distinct().joinToString(",") { "\"${it}\"" }
    return fetchRows(table, "select=id,$nameField&id=in.($inClause)")
      .associate { row ->
        row.opt("id")?.toString().orEmpty().trim() to row.optString(nameField).trim()
      }
      .filterKeys { it.isNotBlank() }
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val url = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 15_000
      readTimeout = 15_000
      setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
      setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
      setRequestProperty("Accept", "application/json")
    }
    return try {
      if (connection.responseCode !in 200..299) return emptyList()
      val payload = connection.inputStream.bufferedReader().use(BufferedReader::readText)
      val rows = JSONArray(payload.ifBlank { "[]" })
      buildList {
        for (index in 0 until rows.length()) {
          val item = rows.optJSONObject(index) ?: continue
          add(item)
        }
      }
    } finally {
      connection.disconnect()
    }
  }

  private fun formatNotifDateLabel(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
  }

  private fun normalizeHari(dayOfWeek: DayOfWeek): String {
    return when (dayOfWeek) {
      DayOfWeek.MONDAY -> "senin"
      DayOfWeek.TUESDAY -> "selasa"
      DayOfWeek.WEDNESDAY -> "rabu"
      DayOfWeek.THURSDAY -> "kamis"
      DayOfWeek.FRIDAY -> "jumat"
      DayOfWeek.SATURDAY -> "sabtu"
      DayOfWeek.SUNDAY -> "minggu"
    }
  }

  private fun normalizeHari(value: String): String {
    return value.trim().lowercase()
      .replace("'", "")
      .replace("’", "")
      .replace(" ", "")
      .replace("jum'at", "jumat")
  }

  private fun toTimeLabel(value: String?): String {
    val text = value.orEmpty().trim()
    return if (text.length >= 5) text.take(5) else text.ifBlank { "--:--" }
  }

  private fun jamTimeKey(start: String?, end: String?): String {
    val startLabel = toTimeLabel(start).takeIf { it != "--:--" }.orEmpty()
    val endLabel = toTimeLabel(end).takeIf { it != "--:--" }.orEmpty()
    return if (startLabel.isBlank() || endLabel.isBlank()) "" else "$startLabel|$endLabel"
  }

  private data class DistribusiRow(
    val id: String,
    val kelasId: String,
    val mapelId: String
  )

  private data class JadwalRow(
    val id: String,
    val distribusiId: String,
    val hari: String,
    val jamMulai: String,
    val jamSelesai: String,
    val jamPelajaranId: String
  )

  companion object {
    const val CATEGORY_TEACHING = "jadwal_mengajar"
    const val CATEGORY_TEACHING_COLOR = "#CFE7FF"
  }
}
