package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.TeacherOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

class GuruTeacherOptionsRemoteDataSource {
  suspend fun fetchSubstituteTeacherOptions(): List<TeacherOption>? = withContext(Dispatchers.IO) {
    runCatching {
      fetchTeacherRows(
        query = "select=id,nama,id_karyawan,aktif&aktif=eq.true&order=nama.asc"
      )
    }.getOrNull()
  }

  suspend fun fetchSourceTeacherOptions(): List<TeacherOption>? = withContext(Dispatchers.IO) {
    runCatching {
      val distribusiRows = fetchRows(
        table = "distribusi_mapel",
        query = "select=guru_id"
      )
      val teacherIds = distribusiRows
        .map { it.optString("guru_id").trim() }
        .filter { it.isNotBlank() }
        .distinct()
      if (teacherIds.isEmpty()) return@runCatching emptyList()

      val inClause = teacherIds.joinToString(",") { "\"${it}\"" }
      val filtered = fetchTeacherRows(
        query = "select=id,nama,id_karyawan,aktif&id=in.($inClause)&aktif=eq.true&order=nama.asc"
      )
      if (filtered.isNotEmpty()) {
        filtered
      } else {
        fetchSubstituteTeacherOptions()
          .orEmpty()
          .filter { option -> teacherIds.contains(option.id) }
      }
    }.getOrNull()
  }

  private fun fetchTeacherRows(query: String): List<TeacherOption> {
    return fetchRows("karyawan", query)
      .mapNotNull { row ->
        val id = row.opt("id")?.toString().orEmpty().trim()
        if (id.isBlank()) return@mapNotNull null
        TeacherOption(
          id = id,
          name = row.optString("nama").trim().ifBlank { "-" },
          employeeId = row.optString("id_karyawan").trim()
        )
      }
      .distinctBy { it.id }
      .sortedBy { it.name.lowercase() }
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
      setRequestProperty("Accept-Charset", "UTF-8")
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
}
