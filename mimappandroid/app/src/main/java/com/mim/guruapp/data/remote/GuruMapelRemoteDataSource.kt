package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.AvailableSubjectOffer
import com.mim.guruapp.data.model.SubjectOverview
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

data class GuruMapelPayload(
  val subjects: List<SubjectOverview>,
  val availableSubjects: List<AvailableSubjectOffer>
)

sealed interface GuruMapelClaimResult {
  data class Success(val updatedIds: Set<String>) : GuruMapelClaimResult
  data class Error(val message: String) : GuruMapelClaimResult
}

class GuruMapelRemoteDataSource {
  suspend fun fetchMapelPayload(
    teacherRowId: String,
    teacherKaryawanId: String
  ): GuruMapelPayload? = withContext(Dispatchers.IO) {
    runCatching {
      val activeTahunAjaranId = resolveActiveTahunAjaranId()
      val activeSemester = resolveActiveSemester(activeTahunAjaranId)

      val distribusiRows = fetchGuruDistribusiRows(teacherRowId, teacherKaryawanId)
      val semesterIds = distribusiRows.mapNotNull { it.optString("semester_id").trim().takeIf(String::isNotBlank) }.distinct()
      val semesterMap = fetchSemesterMap(semesterIds)

      val yearDistribusiRows = if (activeTahunAjaranId.isNotBlank()) {
        distribusiRows.filter { row ->
          val semesterId = row.optString("semester_id").trim()
          semesterMap[semesterId]?.optString("tahun_ajaran_id").orEmpty().trim() == activeTahunAjaranId
        }
      } else {
        distribusiRows
      }
      val activeDistribusiRows = if (activeSemester != null) {
        yearDistribusiRows.filter { row ->
          row.optString("semester_id").trim() == activeSemester.opt("id")?.toString().orEmpty().trim()
        }
      } else {
        yearDistribusiRows
      }

      val availableRows = fetchAvailableDistribusiRows()
      val availableSemesterIds = availableRows.mapNotNull { it.optString("semester_id").trim().takeIf(String::isNotBlank) }.distinct()
      val availableSemesterMap = fetchSemesterMap(availableSemesterIds)
      val availableYearRows = if (activeTahunAjaranId.isNotBlank()) {
        availableRows.filter { row ->
          val semesterId = row.optString("semester_id").trim()
          availableSemesterMap[semesterId]?.optString("tahun_ajaran_id").orEmpty().trim() == activeTahunAjaranId
        }
      } else {
        availableRows
      }

      val kelasIds = (yearDistribusiRows + availableYearRows)
        .mapNotNull { it.optString("kelas_id").trim().takeIf(String::isNotBlank) }
        .distinct()
      val mapelIds = (yearDistribusiRows + availableYearRows)
        .mapNotNull { it.optString("mapel_id").trim().takeIf(String::isNotBlank) }
        .distinct()

      val kelasMap = fetchSimpleMap("kelas", "nama_kelas", kelasIds)
      val mapelMap = fetchSimpleMap("mapel", "nama", mapelIds)

      GuruMapelPayload(
        subjects = activeDistribusiRows.mapNotNull { row ->
          val distribusiId = row.opt("id")?.toString().orEmpty().trim()
          val kelasId = row.optString("kelas_id").trim()
          val mapelId = row.optString("mapel_id").trim()
          if (distribusiId.isBlank() || kelasId.isBlank() || mapelId.isBlank()) return@mapNotNull null
          val semester = semesterMap[row.optString("semester_id").trim()]
          SubjectOverview(
            id = distribusiId,
            title = mapelMap[mapelId].orEmpty().ifBlank { "Mapel" },
            className = kelasMap[kelasId].orEmpty().ifBlank { "-" },
            semester = semesterLabel(semester),
            semesterActive = semester?.opt("aktif") == true || semester?.optString("aktif").orEmpty().trim().lowercase() == "true",
            attendancePending = 0,
            scorePending = 0,
            materialCount = 0
          )
        }.sortedWith(
          compareBy<SubjectOverview> { it.className.lowercase() }
            .thenBy { it.title.lowercase() }
        ),
        availableSubjects = availableYearRows.mapNotNull { row ->
          val distribusiId = row.opt("id")?.toString().orEmpty().trim()
          val kelasId = row.optString("kelas_id").trim()
          val mapelId = row.optString("mapel_id").trim()
          if (distribusiId.isBlank() || kelasId.isBlank() || mapelId.isBlank()) return@mapNotNull null
          val semester = availableSemesterMap[row.optString("semester_id").trim()]
          AvailableSubjectOffer(
            id = distribusiId,
            title = mapelMap[mapelId].orEmpty().ifBlank { "Mapel" },
            className = kelasMap[kelasId].orEmpty().ifBlank { "-" },
            semester = semesterLabel(semester),
            semesterActive = semester?.opt("aktif") == true || semester?.optString("aktif").orEmpty().trim().lowercase() == "true"
          )
        }.sortedWith(
          compareBy<AvailableSubjectOffer> { it.className.lowercase() }
            .thenBy { it.title.lowercase() }
        )
      )
    }.getOrNull()
  }

  suspend fun claimSubjects(
    teacherRowId: String,
    teacherKaryawanId: String,
    distribusiIds: List<String>
  ): GuruMapelClaimResult = withContext(Dispatchers.IO) {
    val normalizedIds = distribusiIds.map { it.trim() }.filter(String::isNotBlank).distinct()
    if (normalizedIds.isEmpty()) {
      return@withContext GuruMapelClaimResult.Error("Tidak ada mapel yang dipilih.")
    }
    val targetGuruId = teacherRowId.trim().ifBlank { teacherKaryawanId.trim() }
    if (targetGuruId.isBlank()) {
      return@withContext GuruMapelClaimResult.Error("ID guru tidak ditemukan.")
    }

    try {
      val inClause = normalizedIds.joinToString(",") { "\"${it}\"" }
      val requestUrl = buildString {
        append(BuildConfig.SUPABASE_URL)
        append("/rest/v1/distribusi_mapel?")
        append("id=in.(")
        append(inClause)
        append(")&guru_id=is.null&select=id")
      }
      val connection = createConnection(requestUrl, "PATCH").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Prefer", "return=representation")
      }
      val payload = JSONObject().apply {
        put("guru_id", targetGuruId)
      }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      val updatedIds = connection.useJsonArrayResponse { rows ->
        buildSet {
          for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            val id = row.opt("id")?.toString().orEmpty().trim()
            if (id.isNotBlank()) add(id)
          }
        }
      }
      GuruMapelClaimResult.Success(updatedIds)
    } catch (_: SocketTimeoutException) {
      GuruMapelClaimResult.Error("Koneksi ke server terlalu lama. Mapel tetap tersimpan lokal dan akan dicoba lagi.")
    } catch (_: Exception) {
      GuruMapelClaimResult.Error("Gagal mengirim mapel ke server. Data lokal tetap aman dan akan dicoba lagi.")
    }
  }

  private fun resolveActiveTahunAjaranId(): String {
    val rows = fetchRows("tahun_ajaran", "select=id,nama,aktif&aktif=eq.true&order=id.desc&limit=1")
    return rows.firstOrNull()?.opt("id")?.toString().orEmpty().trim()
  }

  private fun resolveActiveSemester(activeTahunAjaranId: String): JSONObject? {
    val query = buildString {
      append("select=id,nama,aktif,tahun_ajaran_id&aktif=eq.true&order=id.desc&limit=1")
      if (activeTahunAjaranId.isNotBlank()) {
        append("&tahun_ajaran_id=eq.")
        append(encodeValue(activeTahunAjaranId))
      }
    }
    return fetchRows("semester", query).firstOrNull()
  }

  private fun fetchGuruDistribusiRows(
    teacherRowId: String,
    teacherKaryawanId: String
  ): List<JSONObject> {
    val queries = buildList {
      if (teacherRowId.isNotBlank()) {
        add("select=id,kelas_id,mapel_id,guru_id,semester_id&guru_id=eq.${encodeValue(teacherRowId)}")
      }
      if (teacherKaryawanId.isNotBlank()) {
        add("select=id,kelas_id,mapel_id,guru_id,semester_id&guru_id=eq.${encodeValue(teacherKaryawanId)}")
      }
    }
    return queries.firstNotNullOfOrNull { query ->
      val rows = fetchRows("distribusi_mapel", query)
      rows.takeIf { it.isNotEmpty() }
    } ?: emptyList()
  }

  private fun fetchAvailableDistribusiRows(): List<JSONObject> {
    return fetchRows(
      "distribusi_mapel",
      "select=id,kelas_id,mapel_id,guru_id,semester_id&guru_id=is.null&order=id.desc"
    )
  }

  private fun fetchSemesterMap(ids: List<String>): Map<String, JSONObject> {
    if (ids.isEmpty()) return emptyMap()
    val inClause = ids.distinct().joinToString(",") { "\"${it}\"" }
    return fetchRows("semester", "select=id,nama,aktif,tahun_ajaran_id&id=in.($inClause)")
      .associateBy { it.opt("id")?.toString().orEmpty().trim() }
      .filterKeys { it.isNotBlank() }
  }

  private fun fetchSimpleMap(
    table: String,
    field: String,
    ids: List<String>
  ): Map<String, String> {
    if (ids.isEmpty()) return emptyMap()
    val inClause = ids.distinct().joinToString(",") { "\"${it}\"" }
    return fetchRows(table, "select=id,$field&id=in.($inClause)")
      .associate { row ->
        row.opt("id")?.toString().orEmpty().trim() to row.optString(field).trim()
      }
      .filterKeys { it.isNotBlank() }
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val url = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = createConnection(url, "GET")
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

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
  }

  private fun semesterLabel(semester: JSONObject?): String {
    return semester?.optString("nama").orEmpty().trim().ifBlank { "-" }
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

private inline fun <T> HttpURLConnection.useJsonArrayResponse(
  block: (JSONArray) -> T
): T {
  return try {
    val code = responseCode
    val payload = readMapelPayload(code in 200..299)
    if (code !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $code" })
    }
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readMapelPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}
