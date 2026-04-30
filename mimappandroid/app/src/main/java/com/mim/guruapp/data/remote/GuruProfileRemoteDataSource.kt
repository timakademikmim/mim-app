package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.GuruProfile
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

data class GuruRemoteProfile(
  val teacherRowId: String,
  val teacherId: String,
  val name: String,
  val phoneNumber: String,
  val address: String,
  val password: String,
  val avatarUrl: String
)

sealed interface GuruProfileSyncResult {
  data class Success(val profile: GuruRemoteProfile) : GuruProfileSyncResult
  data class Error(val message: String) : GuruProfileSyncResult
}

class GuruProfileRemoteDataSource {
  suspend fun fetchProfile(
    teacherRowId: String,
    teacherKaryawanId: String
  ): GuruRemoteProfile? = withContext(Dispatchers.IO) {
    val selectVariants = listOf(
      "id,id_karyawan,nama,no_hp,alamat,password,foto_url",
      "id,id_karyawan,nama,no_hp,alamat,password"
    )
    for (select in selectVariants) {
      try {
        val requestUrl = buildSelectUrl(
          select = select,
          teacherRowId = teacherRowId,
          teacherKaryawanId = teacherKaryawanId
        ) ?: return@withContext null
        val connection = createConnection(requestUrl, "GET")
        val result = connection.useProfileJsonArrayResponse { rows ->
          if (rows.length() == 0) return@useProfileJsonArrayResponse null
          parseProfile(rows.optJSONObject(0))
        }
        if (result != null) return@withContext result
      } catch (_: SocketTimeoutException) {
        return@withContext null
      } catch (error: Exception) {
        val message = error.message.orEmpty()
        if (!(message.contains("foto_url", ignoreCase = true) && message.contains("column", ignoreCase = true))) {
          return@withContext null
        }
      }
    }
    null
  }

  suspend fun updateProfile(
    teacherRowId: String,
    teacherKaryawanId: String,
    profile: GuruProfile
  ): GuruProfileSyncResult = withContext(Dispatchers.IO) {
    val normalizedName = profile.name.trim()
    if (normalizedName.isBlank()) {
      return@withContext GuruProfileSyncResult.Error("Nama wajib diisi.")
    }

    val syncAttempts = listOf(true, false)
    for (includePhoto in syncAttempts) {
      try {
        val requestUrl = buildSelectUrl(
          select = if (includePhoto) {
            "id,id_karyawan,nama,no_hp,alamat,password,foto_url"
          } else {
            "id,id_karyawan,nama,no_hp,alamat,password"
          },
          teacherRowId = teacherRowId,
          teacherKaryawanId = teacherKaryawanId
        ) ?: return@withContext GuruProfileSyncResult.Error("ID guru tidak ditemukan.")

        val connection = createConnection(requestUrl, "PATCH").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }

        val payload = JSONObject().apply {
          val normalizedPhone = profile.phoneNumber.trim()
          val normalizedAddress = profile.address.trim()
          val normalizedAvatarUrl = profile.avatarUri.trim()
          put("nama", normalizedName)
          put("no_hp", if (normalizedPhone.isBlank()) JSONObject.NULL else normalizedPhone)
          put("alamat", if (normalizedAddress.isBlank()) JSONObject.NULL else normalizedAddress)
          if (profile.password.trim().isNotBlank()) {
            put("password", profile.password.trim())
          }
          if (includePhoto) {
            put("foto_url", if (normalizedAvatarUrl.isBlank()) JSONObject.NULL else normalizedAvatarUrl)
          }
        }

        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }

        val response = connection.useProfileJsonArrayResponse { rows ->
          parseProfile(rows.optJSONObject(0))
        }

        if (response != null) {
          return@withContext GuruProfileSyncResult.Success(response)
        }

        val fallbackProfile = fetchProfile(teacherRowId = teacherRowId, teacherKaryawanId = teacherKaryawanId)
        if (fallbackProfile != null) {
          return@withContext GuruProfileSyncResult.Success(fallbackProfile)
        }
      } catch (_: SocketTimeoutException) {
        return@withContext GuruProfileSyncResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
      } catch (error: Exception) {
        val message = error.message.orEmpty()
        if (includePhoto && message.contains("foto_url", ignoreCase = true) && message.contains("column", ignoreCase = true)) {
          continue
        }
        return@withContext GuruProfileSyncResult.Error("Gagal menyimpan profil ke server.")
      }
    }

    GuruProfileSyncResult.Error("Gagal menyimpan profil ke server.")
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

  private fun buildSelectUrl(
    select: String,
    teacherRowId: String,
    teacherKaryawanId: String
  ): String? {
    val base = "${BuildConfig.SUPABASE_URL}/rest/v1/karyawan"
    val filter = when {
      teacherRowId.isNotBlank() -> "id=eq.${encodeValue(teacherRowId)}"
      teacherKaryawanId.isNotBlank() -> "id_karyawan=eq.${encodeValue(teacherKaryawanId)}"
      else -> return null
    }
    return "$base?select=$select&$filter&limit=1"
  }

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
  }

  private fun parseProfile(item: JSONObject?): GuruRemoteProfile? {
    item ?: return null
    return GuruRemoteProfile(
      teacherRowId = item.opt("id")?.toString().orEmpty().trim(),
      teacherId = item.optString("id_karyawan").trim(),
      name = item.optString("nama").trim(),
      phoneNumber = item.optString("no_hp").trim(),
      address = item.optString("alamat").trim(),
      password = item.optString("password").trim(),
      avatarUrl = item.optString("foto_url").trim()
    )
  }
}

private inline fun <T> HttpURLConnection.useProfileJsonArrayResponse(
  block: (JSONArray) -> T
): T {
  return try {
    val responseCode = responseCode
    val payload = readProfilePayload(responseCode in 200..299)
    if (responseCode !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $responseCode" })
    }
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readProfilePayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}
