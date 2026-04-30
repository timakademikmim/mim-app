package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder

data class GuruAuthUser(
  val teacherRowId: String,
  val teacherId: String,
  val teacherName: String,
  val roles: List<String>,
  val activeRole: String = "guru"
)

sealed interface GuruAuthResult {
  data class Success(val user: GuruAuthUser) : GuruAuthResult
  data class Error(val message: String) : GuruAuthResult
}

class GuruAuthRemoteDataSource {
  suspend fun loginAsGuru(
    teacherId: String,
    password: String
  ): GuruAuthResult = withContext(Dispatchers.IO) {
    try {
      val requestUrl = buildUrl(
        teacherId = teacherId,
        password = password
      )
      val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 15_000
        readTimeout = 15_000
        setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
        setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
        setRequestProperty("Accept", "application/json")
      }

      connection.useJsonArrayResponse { rows ->
        if (rows.length() == 0) {
          return@withContext GuruAuthResult.Error("ID Karyawan atau password salah.")
        }

        val item = rows.optJSONObject(0)
          ?: return@withContext GuruAuthResult.Error("Data login tidak valid.")
        val activeValue = item.opt("aktif")
        if (!isActiveValue(activeValue)) {
          return@withContext GuruAuthResult.Error("Akun nonaktif. Silakan hubungi admin.")
        }

        val roles = parseRoleList(item.optString("role"))
        if (!roles.contains("guru")) {
          return@withContext GuruAuthResult.Error("Akun ini tidak memiliki akses ke aplikasi guru.")
        }

        val normalizedId = item.optString("id_karyawan").trim()
        val normalizedName = item.optString("nama").trim().ifBlank { normalizedId }
        GuruAuthResult.Success(
          GuruAuthUser(
            teacherRowId = item.opt("id")?.toString().orEmpty().trim(),
            teacherId = normalizedId,
            teacherName = normalizedName,
            roles = roles
          )
        )
      }
    } catch (_: SocketTimeoutException) {
      GuruAuthResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      GuruAuthResult.Error("Tidak dapat terhubung ke server login.")
    }
  }

  private fun buildUrl(teacherId: String, password: String): String {
    val base = "${BuildConfig.SUPABASE_URL}/rest/v1/karyawan"
    val encodedTeacherId = URLEncoder.encode(teacherId, Charsets.UTF_8.name())
    val encodedPassword = URLEncoder.encode(password, Charsets.UTF_8.name())
    return "$base?select=id,id_karyawan,nama,role,aktif&id_karyawan=eq.$encodedTeacherId&password=eq.$encodedPassword&limit=1"
  }

  private fun parseRoleList(rawRole: String?): List<String> {
    val text = rawRole.orEmpty().trim()
    if (text.isBlank()) return emptyList()
    return text
      .split(',', '|', ';')
      .map { it.trim().lowercase() }
      .filter { it.isNotBlank() }
      .distinct()
  }

  private fun isActiveValue(value: Any?): Boolean {
    if (value == true || value == 1) return true
    val text = value?.toString().orEmpty().trim().lowercase()
    return text == "true" || text == "t" || text == "1" || text == "yes"
  }
}

private inline fun <T> HttpURLConnection.useJsonArrayResponse(
  block: (JSONArray) -> T
): T {
  return try {
    val responseCode = responseCode
    val payload = readPayload(responseCode in 200..299)
    if (responseCode !in 200..299) {
      block(JSONArray())
    } else {
      block(JSONArray(payload.ifBlank { "[]" }))
    }
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}
