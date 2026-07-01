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

sealed interface GuruPasswordChangeResult {
  data object Success : GuruPasswordChangeResult
  data class Error(val message: String) : GuruPasswordChangeResult
}

class GuruProfileRemoteDataSource {
  suspend fun fetchProfile(
    teacherRowId: String,
    teacherKaryawanId: String
  ): GuruRemoteProfile? = withContext(Dispatchers.IO) {
    if (!SupabaseRequestAuth.isAuthenticated()) return@withContext null
    val protectedProfile = runCatching {
      val response = postSelfProfile(JSONObject().put("action", "get"))
      parseProfile(response.optJSONObject("profile"))
    }.getOrNull()
    if (protectedProfile != null) return@withContext protectedProfile

    val selectVariants = listOf(
      "id,id_karyawan,nama,no_hp,alamat,foto_url",
      "id,id_karyawan,nama,no_hp,alamat"
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

    if (!SupabaseRequestAuth.isAuthenticated()) {
      return@withContext GuruProfileSyncResult.Error("Sesi login berakhir. Silakan masuk kembali.")
    }
    return@withContext try {
        val response = postSelfProfile(
          JSONObject().apply {
            put("action", "update")
            put("name", normalizedName)
            putNullableProfileText("phone", profile.phoneNumber)
            putNullableProfileText("address", profile.address)
            putNullableProfileText("avatar_url", profile.avatarUri)
          }
        )
        val saved = parseProfile(response.optJSONObject("profile"))
          ?: return@withContext GuruProfileSyncResult.Error("Profil tidak ditemukan setelah disimpan.")
        GuruProfileSyncResult.Success(saved)
      } catch (_: SocketTimeoutException) {
        GuruProfileSyncResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
      } catch (error: Exception) {
        val message = runCatching {
          JSONObject(error.message.orEmpty()).optString("error").trim()
        }.getOrDefault("")
        GuruProfileSyncResult.Error(message.ifBlank { "Gagal menyimpan profil ke server." })
      }
  }

  suspend fun changePassword(
    currentPassword: String,
    newPassword: String
  ): GuruPasswordChangeResult = withContext(Dispatchers.IO) {
    if (!SupabaseRequestAuth.isAuthenticated()) {
      return@withContext GuruPasswordChangeResult.Error("Silakan masuk kembali sebelum mengganti password.")
    }
    return@withContext try {
      postSelfProfile(
        JSONObject().apply {
          put("action", "change_password")
          put("current_password", currentPassword)
          put("new_password", newPassword)
        }
      )
      GuruPasswordChangeResult.Success
    } catch (_: SocketTimeoutException) {
      GuruPasswordChangeResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (error: Exception) {
      GuruPasswordChangeResult.Error(extractProfileError(error, "Gagal mengganti password."))
    }
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

  private fun postSelfProfile(payload: JSONObject): JSONObject {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/functions/v1/manage-self-profile"
    val connection = createConnection(requestUrl, "POST").apply {
      doOutput = true
      setRequestProperty("Content-Type", "application/json")
    }
    connection.outputStream.use { stream ->
      stream.write(payload.toString().toByteArray(Charsets.UTF_8))
      stream.flush()
    }
    return connection.useProfileJsonObjectResponse { it }
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
      teacherId = item.optProfileString("id_karyawan"),
      name = item.optProfileString("nama"),
      phoneNumber = item.optProfileString("no_hp"),
      address = item.optProfileString("alamat"),
      password = "",
      avatarUrl = item.optProfileString("foto_url")
    )
  }
}

private fun JSONObject.optProfileString(key: String): String {
  val value = opt(key)
  return if (value == null || value == JSONObject.NULL) "" else value.toString().trim()
}

private inline fun <T> HttpURLConnection.useProfileJsonObjectResponse(
  block: (JSONObject) -> T
): T {
  return try {
    val responseCode = responseCode
    val payload = readProfilePayload(responseCode in 200..299)
    if (responseCode !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $responseCode" })
    }
    block(JSONObject(payload.ifBlank { "{}" }))
  } finally {
    disconnect()
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

private fun JSONObject.putNullableProfileText(key: String, value: String) {
  val normalized = value.trim()
  put(key, if (normalized.isBlank()) JSONObject.NULL else normalized)
}

private fun extractProfileError(error: Exception, fallback: String): String {
  return runCatching {
    JSONObject(error.message.orEmpty()).optString("error").trim()
  }.getOrDefault("").ifBlank { fallback }
}
