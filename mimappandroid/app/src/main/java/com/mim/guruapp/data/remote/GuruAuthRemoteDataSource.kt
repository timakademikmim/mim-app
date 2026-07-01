package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.availableAppRoles
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
import java.security.MessageDigest

data class TenantLoginOption(
  val id: String,
  val code: String,
  val name: String,
  val officialName: String,
  val logoUrl: String
)

data class GuruSupabaseAuthSession(
  val authUserId: String,
  val accessToken: String,
  val refreshToken: String,
  val expiresAt: Long
)

data class GuruAuthUser(
  val teacherRowId: String,
  val teacherId: String,
  val teacherName: String,
  val roles: List<String>,
  val activeRole: String = "guru",
  val tenantId: String = "",
  val tenantName: String = "",
  val organizationId: String = "",
  val mustChangePassword: Boolean = false
)

sealed interface TenantDirectoryResult {
  data class Success(val tenants: List<TenantLoginOption>) : TenantDirectoryResult
  data class Error(val message: String) : TenantDirectoryResult
}

sealed interface GuruAuthResult {
  data class Success(
    val user: GuruAuthUser,
    val authSession: GuruSupabaseAuthSession? = null
  ) : GuruAuthResult

  data class Error(val message: String) : GuruAuthResult
}

sealed interface GuruAuthRefreshResult {
  data class Success(val session: GuruSupabaseAuthSession) : GuruAuthRefreshResult
  data class Error(val message: String) : GuruAuthRefreshResult
}

class GuruAuthRemoteDataSource {
  suspend fun fetchLoginTenants(): TenantDirectoryResult = withContext(Dispatchers.IO) {
    try {
      val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/rpc/list_login_tenants"
      val connection = createConnection(requestUrl, "POST", BuildConfig.SUPABASE_ANON_KEY).apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
      }
      connection.outputStream.use { stream ->
        stream.write("{}".toByteArray(Charsets.UTF_8))
      }
      connection.useJsonArrayResponse { rows ->
        val tenants = (0 until rows.length())
          .mapNotNull { index -> rows.optJSONObject(index) }
          .mapNotNull { row ->
            val id = row.optCleanString("id")
            val code = row.optCleanString("code")
            val name = row.optCleanString("name")
            if (id.isBlank() || code.isBlank() || name.isBlank()) return@mapNotNull null
            TenantLoginOption(
              id = id,
              code = code,
              name = name,
              officialName = row.optCleanString("official_name"),
              logoUrl = row.optCleanString("logo_url")
            )
          }
        if (tenants.isEmpty()) {
          TenantDirectoryResult.Error("Belum ada unit aktif yang dapat dipilih.")
        } else {
          TenantDirectoryResult.Success(tenants)
        }
      }
    } catch (_: SocketTimeoutException) {
      TenantDirectoryResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      TenantDirectoryResult.Error("Tidak dapat memuat daftar unit.")
    }
  }

  suspend fun login(
    tenant: TenantLoginOption,
    teacherId: String,
    password: String
  ): GuruAuthResult = withContext(Dispatchers.IO) {
    when (val authResult = loginWithSupabaseAuth(tenant, teacherId, password)) {
      is GuruAuthResult.Success -> authResult
      is GuruAuthResult.Error -> loginLegacyAccount(tenant, teacherId, password, authResult.message)
    }
  }

  suspend fun refreshSession(refreshToken: String): GuruAuthRefreshResult = withContext(Dispatchers.IO) {
    if (refreshToken.isBlank()) {
      return@withContext GuruAuthRefreshResult.Error("Sesi login tidak memiliki refresh token.")
    }
    try {
      val requestUrl = "${BuildConfig.SUPABASE_URL}/auth/v1/token?grant_type=refresh_token"
      val connection = createConnection(requestUrl, "POST", BuildConfig.SUPABASE_ANON_KEY).apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
      }
      val payload = JSONObject().apply { put("refresh_token", refreshToken) }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
      }
      connection.useJsonObjectResponse { authJson ->
        val accessToken = authJson.optCleanString("access_token")
        val nextRefreshToken = authJson.optCleanString("refresh_token").ifBlank { refreshToken }
        val authUserId = authJson.optJSONObject("user")?.optCleanString("id").orEmpty()
        val expiresInSeconds = authJson.optLong("expires_in", 3600L).coerceAtLeast(60L)
        if (accessToken.isBlank() || authUserId.isBlank()) {
          return@useJsonObjectResponse GuruAuthRefreshResult.Error("Sesi login tidak dapat diperbarui.")
        }
        GuruAuthRefreshResult.Success(
          GuruSupabaseAuthSession(
            authUserId = authUserId,
            accessToken = accessToken,
            refreshToken = nextRefreshToken,
            expiresAt = System.currentTimeMillis() + expiresInSeconds * 1000L
          )
        )
      }
    } catch (_: SocketTimeoutException) {
      GuruAuthRefreshResult.Error("Pembaruan sesi terlalu lama. Periksa koneksi internet.")
    } catch (_: HttpResponseException) {
      GuruAuthRefreshResult.Error("Sesi login telah berakhir. Silakan login kembali.")
    } catch (_: Exception) {
      GuruAuthRefreshResult.Error("Sesi login belum dapat diperbarui.")
    }
  }

  private fun loginWithSupabaseAuth(
    tenant: TenantLoginOption,
    teacherId: String,
    password: String
  ): GuruAuthResult {
    return try {
      val authEmail = buildInternalAuthEmail(tenant.id, teacherId)
      val requestUrl = "${BuildConfig.SUPABASE_URL}/auth/v1/token?grant_type=password"
      val connection = createConnection(requestUrl, "POST", BuildConfig.SUPABASE_ANON_KEY).apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
      }
      val payload = JSONObject().apply {
        put("email", authEmail)
        put("password", password)
      }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
      }
      connection.useJsonObjectResponse { authJson ->
        val accessToken = authJson.optCleanString("access_token")
        val refreshToken = authJson.optCleanString("refresh_token")
        val expiresInSeconds = authJson.optLong("expires_in", 3600L).coerceAtLeast(60L)
        val authUserId = authJson.optJSONObject("user")?.optCleanString("id").orEmpty()
        if (accessToken.isBlank() || authUserId.isBlank()) {
          return@useJsonObjectResponse GuruAuthResult.Error("ID Karyawan atau password salah.")
        }

        val employee = fetchAuthenticatedEmployee(
          accessToken = accessToken,
          authUserId = authUserId,
          tenant = tenant
        ) ?: return@useJsonObjectResponse GuruAuthResult.Error(
          "Akun Auth belum terhubung ke data karyawan unit ini."
        )

        val roles = parseRoleList(employee.optCleanString("role"))
        if (!employee.optBooleanFlexible("aktif")) {
          return@useJsonObjectResponse GuruAuthResult.Error("Akun nonaktif. Silakan hubungi admin.")
        }
        if (availableAppRoles(roles).isEmpty()) {
          return@useJsonObjectResponse GuruAuthResult.Error(
            "Akun ini tidak memiliki akses ke aplikasi Android."
          )
        }

        GuruAuthResult.Success(
          user = GuruAuthUser(
            teacherRowId = employee.optCleanString("id"),
            teacherId = employee.optCleanString("id_karyawan"),
            teacherName = employee.optCleanString("nama").ifBlank { teacherId },
            roles = roles,
            tenantId = tenant.id,
            tenantName = tenant.name,
            organizationId = employee.optCleanString("organization_id"),
            mustChangePassword = employee.optBooleanFlexible("must_change_password")
          ),
          authSession = GuruSupabaseAuthSession(
            authUserId = authUserId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = System.currentTimeMillis() + expiresInSeconds * 1000L
          )
        )
      }
    } catch (_: HttpResponseException) {
      GuruAuthResult.Error("ID Karyawan atau password salah.")
    } catch (_: SocketTimeoutException) {
      GuruAuthResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      GuruAuthResult.Error("Tidak dapat terhubung ke server login.")
    }
  }

  private fun fetchAuthenticatedEmployee(
    accessToken: String,
    authUserId: String,
    tenant: TenantLoginOption
  ): JSONObject? {
    val requestUrl = buildString {
      append(BuildConfig.SUPABASE_URL)
      append("/rest/v1/karyawan")
      append("?select=id,id_karyawan,nama,role,aktif,tenant_id,organization_id,must_change_password")
      append("&auth_user_id=eq.")
      append(encodeValue(authUserId))
      append("&tenant_id=eq.")
      append(encodeValue(tenant.id))
      append("&limit=1")
    }
    val connection = createConnection(requestUrl, "GET", accessToken)
    return connection.useJsonArrayResponse { rows -> rows.optJSONObject(0) }
  }

  private fun loginLegacyAccount(
    tenant: TenantLoginOption,
    teacherId: String,
    password: String,
    authErrorMessage: String
  ): GuruAuthResult {
    return try {
      val requestUrl = buildString {
        append(BuildConfig.SUPABASE_URL)
        append("/rest/v1/karyawan")
        append("?select=id,id_karyawan,nama,role,aktif,tenant_id,organization_id,auth_user_id")
        append("&tenant_id=eq.")
        append(encodeValue(tenant.id))
        append("&id_karyawan=eq.")
        append(encodeValue(teacherId))
        append("&password=eq.")
        append(encodeValue(password))
        append("&auth_user_id=is.null&limit=1")
      }
      val connection = createConnection(requestUrl, "GET", BuildConfig.SUPABASE_ANON_KEY)
      connection.useJsonArrayResponse { rows ->
        if (rows.length() == 0) {
          return@useJsonArrayResponse GuruAuthResult.Error(authErrorMessage)
        }
        val item = rows.optJSONObject(0)
          ?: return@useJsonArrayResponse GuruAuthResult.Error("Data login tidak valid.")
        if (!item.optBooleanFlexible("aktif")) {
          return@useJsonArrayResponse GuruAuthResult.Error("Akun nonaktif. Silakan hubungi admin.")
        }
        val roles = parseRoleList(item.optCleanString("role"))
        if (availableAppRoles(roles).isEmpty()) {
          return@useJsonArrayResponse GuruAuthResult.Error(
            "Akun ini tidak memiliki akses ke aplikasi Android."
          )
        }
        GuruAuthResult.Success(
          user = GuruAuthUser(
            teacherRowId = item.optCleanString("id"),
            teacherId = item.optCleanString("id_karyawan"),
            teacherName = item.optCleanString("nama").ifBlank { teacherId },
            roles = roles,
            tenantId = tenant.id,
            tenantName = tenant.name,
            organizationId = item.optCleanString("organization_id")
          )
        )
      }
    } catch (_: Exception) {
      GuruAuthResult.Error(authErrorMessage)
    }
  }

  private fun createConnection(
    requestUrl: String,
    method: String,
    bearerToken: String
  ): HttpURLConnection {
    return (URL(requestUrl).openConnection() as HttpURLConnection).apply {
      requestMethod = method
      connectTimeout = 15_000
      readTimeout = 15_000
      applySupabaseRequestHeaders(bearerToken)
      setRequestProperty("Accept", "application/json")
      setRequestProperty("Accept-Charset", "UTF-8")
    }
  }

  private fun buildInternalAuthEmail(tenantId: String, teacherId: String): String {
    val normalizedLoginId = teacherId.trim().lowercase().replace(Regex("\\s+"), "")
    val source = "$tenantId:$normalizedLoginId"
    val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
    val hex = digest.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    return "$hex@accounts.mim.invalid"
  }

  private fun parseRoleList(rawRole: String?): List<String> {
    return rawRole.orEmpty()
      .split(',', '|', ';')
      .map { it.trim().lowercase() }
      .filter { it.isNotBlank() }
      .distinct()
  }

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
  }
}

private class HttpResponseException(val statusCode: Int, message: String) : Exception(message)

private inline fun <T> HttpURLConnection.useJsonArrayResponse(block: (JSONArray) -> T): T {
  return try {
    val status = responseCode
    val payload = readPayload(status in 200..299)
    if (status !in 200..299) throw HttpResponseException(status, payload)
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private inline fun <T> HttpURLConnection.useJsonObjectResponse(block: (JSONObject) -> T): T {
  return try {
    val status = responseCode
    val payload = readPayload(status in 200..299)
    if (status !in 200..299) throw HttpResponseException(status, payload)
    block(JSONObject(payload.ifBlank { "{}" }))
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

private fun JSONObject.optCleanString(key: String): String {
  val value = opt(key)
  if (value == null || value == JSONObject.NULL) return ""
  val text = value.toString().trim()
  return if (text.equals("null", ignoreCase = true)) "" else text
}

private fun JSONObject.optBooleanFlexible(key: String): Boolean {
  val value = opt(key)
  if (value == true || value == 1) return true
  val text = value?.toString().orEmpty().trim().lowercase()
  return text == "true" || text == "t" || text == "1" || text == "yes" || text == "aktif"
}
