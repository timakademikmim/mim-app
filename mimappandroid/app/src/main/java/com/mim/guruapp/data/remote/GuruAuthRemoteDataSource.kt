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

data class GuruMfaPendingSession(
  val tenant: TenantLoginOption?,
  val teacherId: String,
  val authUserId: String,
  val accessToken: String,
  val refreshToken: String,
  val expiresAt: Long,
  val factorId: String
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

  data class MfaRequired(val pending: GuruMfaPendingSession) : GuruAuthResult

  data class Error(val message: String) : GuruAuthResult
}

sealed interface GuruAuthRefreshResult {
  data class Success(val session: GuruSupabaseAuthSession) : GuruAuthRefreshResult
  data class Error(val message: String) : GuruAuthRefreshResult
}

sealed interface GuruPasswordResetRequestResult {
  data class Success(val message: String) : GuruPasswordResetRequestResult
  data class Error(val message: String) : GuruPasswordResetRequestResult
}

sealed interface GuruGoogleOAuthUrlResult {
  data class Success(val url: String) : GuruGoogleOAuthUrlResult
  data class Error(val message: String) : GuruGoogleOAuthUrlResult
}

class GuruAuthRemoteDataSource {
  fun buildGoogleLoginUrl(codeChallenge: String): String {
    val redirectUrl = buildGoogleRedirectUrl()
    return buildString {
      append(BuildConfig.SUPABASE_URL.trimEnd('/'))
      append("/auth/v1/authorize?provider=google")
      append("&redirect_to=")
      append(encodeValue(redirectUrl))
      append("&code_challenge=")
      append(encodeValue(codeChallenge))
      append("&code_challenge_method=s256")
    }
  }

  suspend fun buildGoogleLinkUrl(
    accessToken: String
  ): GuruGoogleOAuthUrlResult = withContext(Dispatchers.IO) {
    if (accessToken.isBlank()) {
      return@withContext GuruGoogleOAuthUrlResult.Error("Sesi login tidak tersedia. Silakan masuk kembali.")
    }
    try {
      val redirectUrl = buildGoogleRedirectUrl()
      val requestUrl = buildString {
        append(BuildConfig.SUPABASE_URL.trimEnd('/'))
        append("/auth/v1/user/identities/authorize?provider=google")
        append("&redirect_to=")
        append(encodeValue(redirectUrl))
        append("&skip_http_redirect=true")
      }
      val connection = createConnection(requestUrl, "GET", accessToken)
      connection.useJsonObjectResponse { json ->
        val url = json.optCleanString("url")
        if (url.isBlank()) {
          GuruGoogleOAuthUrlResult.Error("URL tautkan Google belum tersedia.")
        } else {
          GuruGoogleOAuthUrlResult.Success(url)
        }
      }
    } catch (_: SocketTimeoutException) {
      GuruGoogleOAuthUrlResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (error: HttpResponseException) {
      GuruGoogleOAuthUrlResult.Error(
        parseAuthErrorMessage(
          error,
          "Tautkan Google belum dapat dimulai. Pastikan Google provider sudah aktif dan redirect URL Android sudah diizinkan."
        )
      )
    } catch (_: Exception) {
      GuruGoogleOAuthUrlResult.Error("Tautkan Google belum dapat dibuka.")
    }
  }

  suspend fun requestPasswordReset(
    tenantId: String,
    teacherId: String
  ): GuruPasswordResetRequestResult = withContext(Dispatchers.IO) {
    if (tenantId.isBlank() || teacherId.isBlank()) {
      return@withContext GuruPasswordResetRequestResult.Error("Pilih unit dan isi ID Karyawan terlebih dahulu.")
    }
    try {
      val requestUrl = "${BuildConfig.SUPABASE_URL}/functions/v1/request-password-reset"
      val connection = createConnection(requestUrl, "POST", BuildConfig.SUPABASE_ANON_KEY).apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
      }
      val payload = JSONObject().apply {
        put("tenant_id", tenantId)
        put("login_id", teacherId.trim())
      }
      connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
      connection.useJsonObjectResponse { json ->
        GuruPasswordResetRequestResult.Success(
          json.optCleanString("message").ifBlank { "Permintaan reset telah dikirim kepada admin unit." }
        )
      }
    } catch (_: Exception) {
      GuruPasswordResetRequestResult.Error("Permintaan reset belum dapat dikirim. Coba lagi.")
    }
  }

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
    loginWithSupabaseAuth(tenant, teacherId, password)
  }

  suspend fun loginWithOAuthSession(
    accessToken: String,
    refreshToken: String,
    expiresAt: Long
  ): GuruAuthResult = withContext(Dispatchers.IO) {
    if (accessToken.isBlank()) {
      return@withContext GuruAuthResult.Error("Sesi Google tidak ditemukan.")
    }
    try {
      val user = fetchAuthUser(accessToken)
        ?: return@withContext GuruAuthResult.Error("Sesi Google tidak valid.")
      val authUserId = user.optCleanString("id")
      if (authUserId.isBlank()) return@withContext GuruAuthResult.Error("Identitas Google tidak valid.")
      val factorId = findVerifiedTotpFactorId(user)
      if (factorId.isNotBlank()) {
        return@withContext GuruAuthResult.MfaRequired(
          GuruMfaPendingSession(
            tenant = null,
            teacherId = "",
            authUserId = authUserId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
            factorId = factorId
          )
        )
      }
      buildAuthenticatedResultFromAuthUser(authUserId, accessToken, refreshToken, expiresAt)
    } catch (_: SocketTimeoutException) {
      GuruAuthResult.Error("Login Google terlalu lama. Periksa koneksi internet.")
    } catch (_: HttpResponseException) {
      GuruAuthResult.Error("Login Google gagal atau akun belum ditautkan.")
    } catch (_: Exception) {
      GuruAuthResult.Error("Login Google belum dapat diselesaikan.")
    }
  }

  suspend fun loginWithOAuthCode(
    authCode: String,
    codeVerifier: String
  ): GuruAuthResult = withContext(Dispatchers.IO) {
    if (authCode.isBlank() || codeVerifier.isBlank()) {
      return@withContext GuruAuthResult.Error("Sesi login Google tidak lengkap. Coba login ulang.")
    }
    try {
      val requestUrl = "${BuildConfig.SUPABASE_URL}/auth/v1/token?grant_type=pkce"
      val connection = createConnection(requestUrl, "POST", BuildConfig.SUPABASE_ANON_KEY).apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
      }
      val payload = JSONObject().apply {
        put("auth_code", authCode)
        put("code_verifier", codeVerifier)
      }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
      }
      connection.useJsonObjectResponse { authJson ->
        val accessToken = authJson.optCleanString("access_token")
        val refreshToken = authJson.optCleanString("refresh_token")
        val user = authJson.optJSONObject("user")
        val authUserId = user?.optCleanString("id").orEmpty()
        val expiresInSeconds = authJson.optLong("expires_in", 3600L).coerceAtLeast(60L)
        val expiresAt = System.currentTimeMillis() + expiresInSeconds * 1000L
        if (accessToken.isBlank() || authUserId.isBlank()) {
          return@useJsonObjectResponse GuruAuthResult.Error("Sesi Google tidak valid.")
        }
        val factorId = findVerifiedTotpFactorId(user)
        if (factorId.isNotBlank()) {
          return@useJsonObjectResponse GuruAuthResult.MfaRequired(
            GuruMfaPendingSession(
              tenant = null,
              teacherId = "",
              authUserId = authUserId,
              accessToken = accessToken,
              refreshToken = refreshToken,
              expiresAt = expiresAt,
              factorId = factorId
            )
          )
        }
        buildAuthenticatedResultFromAuthUser(authUserId, accessToken, refreshToken, expiresAt)
      }
    } catch (_: SocketTimeoutException) {
      GuruAuthResult.Error("Login Google terlalu lama. Periksa koneksi internet.")
    } catch (_: HttpResponseException) {
      GuruAuthResult.Error("Login Google gagal atau akun belum ditautkan.")
    } catch (_: Exception) {
      GuruAuthResult.Error("Login Google belum dapat diselesaikan.")
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

  suspend fun verifyMfa(pending: GuruMfaPendingSession, code: String): GuruAuthResult = withContext(Dispatchers.IO) {
    if (!code.matches(Regex("^\\d{6}$"))) {
      return@withContext GuruAuthResult.Error("Masukkan 6 digit kode authenticator.")
    }
    try {
      val challengeUrl = "${BuildConfig.SUPABASE_URL}/auth/v1/factors/${encodeValue(pending.factorId)}/challenge"
      val challengeConnection = createConnection(challengeUrl, "POST", pending.accessToken).apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
      }
      challengeConnection.outputStream.use { it.write("{}".toByteArray()) }
      val challengeId = challengeConnection.useJsonObjectResponse { it.optCleanString("id") }
      if (challengeId.isBlank()) return@withContext GuruAuthResult.Error("Tantangan MFA tidak dapat dibuat.")

      val verifyUrl = "${BuildConfig.SUPABASE_URL}/auth/v1/factors/${encodeValue(pending.factorId)}/verify"
      val verifyConnection = createConnection(verifyUrl, "POST", pending.accessToken).apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
      }
      val verifyPayload = JSONObject().apply {
        put("challenge_id", challengeId)
        put("code", code)
      }
      verifyConnection.outputStream.use { it.write(verifyPayload.toString().toByteArray(Charsets.UTF_8)) }
      verifyConnection.useJsonObjectResponse { verified ->
        val accessToken = verified.optCleanString("access_token")
        val refreshToken = verified.optCleanString("refresh_token").ifBlank { pending.refreshToken }
        val expiresInSeconds = verified.optLong("expires_in", 3600L).coerceAtLeast(60L)
        if (accessToken.isBlank()) return@useJsonObjectResponse GuruAuthResult.Error("Verifikasi MFA gagal.")
        val expiresAt = System.currentTimeMillis() + expiresInSeconds * 1000L
        val tenant = pending.tenant
        if (tenant != null) {
          buildAuthenticatedResult(
            tenant = tenant,
            teacherId = pending.teacherId,
            authUserId = pending.authUserId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt
          )
        } else {
          buildAuthenticatedResultFromAuthUser(
            authUserId = pending.authUserId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt
          )
        }
      }
    } catch (_: HttpResponseException) {
      GuruAuthResult.Error("Kode authenticator salah atau sudah kedaluwarsa.")
    } catch (_: SocketTimeoutException) {
      GuruAuthResult.Error("Verifikasi MFA terlalu lama. Periksa koneksi internet.")
    } catch (_: Exception) {
      GuruAuthResult.Error("MFA belum dapat diverifikasi.")
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
        val factorId = findVerifiedTotpFactorId(authJson.optJSONObject("user"))
        val expiresAt = System.currentTimeMillis() + expiresInSeconds * 1000L
        if (factorId.isNotBlank()) {
          return@useJsonObjectResponse GuruAuthResult.MfaRequired(
            GuruMfaPendingSession(
              tenant = tenant,
              teacherId = teacherId,
              authUserId = authUserId,
              accessToken = accessToken,
              refreshToken = refreshToken,
              expiresAt = expiresAt,
              factorId = factorId
            )
          )
        }
        buildAuthenticatedResult(tenant, teacherId, authUserId, accessToken, refreshToken, expiresAt)
      }
    } catch (_: HttpResponseException) {
      GuruAuthResult.Error("ID Karyawan atau password salah.")
    } catch (_: SocketTimeoutException) {
      GuruAuthResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      GuruAuthResult.Error("Tidak dapat terhubung ke server login.")
    }
  }

  private fun buildAuthenticatedResultFromAuthUser(
    authUserId: String,
    accessToken: String,
    refreshToken: String,
    expiresAt: Long
  ): GuruAuthResult {
    val employee = fetchActiveEmployeeForAuthUser(accessToken, authUserId)
      ?: return GuruAuthResult.Error("Akun Google ini belum ditautkan ke akun karyawan aktif.")
    val tenant = fetchTenantOption(accessToken, employee.optCleanString("tenant_id"))
      ?: return GuruAuthResult.Error("Unit sekolah untuk akun ini tidak aktif atau tidak ditemukan.")
    return buildAuthenticatedResult(
      tenant = tenant,
      teacherId = employee.optCleanString("id_karyawan"),
      authUserId = authUserId,
      accessToken = accessToken,
      refreshToken = refreshToken,
      expiresAt = expiresAt,
      employeeOverride = employee
    )
  }

  private fun buildAuthenticatedResult(
    tenant: TenantLoginOption,
    teacherId: String,
    authUserId: String,
    accessToken: String,
    refreshToken: String,
    expiresAt: Long,
    employeeOverride: JSONObject? = null
  ): GuruAuthResult {
    val employee = employeeOverride ?: fetchAuthenticatedEmployee(accessToken, authUserId, tenant)
      ?: return GuruAuthResult.Error("Akun Auth belum terhubung ke data karyawan unit ini.")
    val roles = parseRoleList(employee.optCleanString("role"))
    if (!employee.optBooleanFlexible("aktif")) {
      return GuruAuthResult.Error("Akun nonaktif. Silakan hubungi admin.")
    }
    if (availableAppRoles(roles).isEmpty()) {
      return GuruAuthResult.Error("Akun ini tidak memiliki akses ke aplikasi Android.")
    }
    return GuruAuthResult.Success(
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
      authSession = GuruSupabaseAuthSession(authUserId, accessToken, refreshToken, expiresAt)
    )
  }

  private fun fetchAuthUser(accessToken: String): JSONObject? {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/auth/v1/user"
    val connection = createConnection(requestUrl, "GET", accessToken)
    return connection.useJsonObjectResponse { it }
  }

  private fun findVerifiedTotpFactorId(user: JSONObject?): String {
    val factors = user?.optJSONArray("factors") ?: return ""
    for (index in 0 until factors.length()) {
      val factor = factors.optJSONObject(index) ?: continue
      val status = factor.optCleanString("status").lowercase()
      val type = factor.optCleanString("factor_type").ifBlank { factor.optCleanString("type") }.lowercase()
      if (status == "verified" && type == "totp") return factor.optCleanString("id")
    }
    return ""
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

  private fun fetchActiveEmployeeForAuthUser(accessToken: String, authUserId: String): JSONObject? {
    val requestUrl = buildString {
      append(BuildConfig.SUPABASE_URL)
      append("/rest/v1/karyawan")
      append("?select=id,id_karyawan,nama,role,aktif,tenant_id,organization_id,must_change_password")
      append("&auth_user_id=eq.")
      append(encodeValue(authUserId))
      append("&limit=2")
    }
    val connection = createConnection(requestUrl, "GET", accessToken)
    return connection.useJsonArrayResponse { rows ->
      val activeRows = (0 until rows.length())
        .mapNotNull { rows.optJSONObject(it) }
        .filter { it.optBooleanFlexible("aktif") }
      if (activeRows.size > 1) {
        throw HttpResponseException(409, "Akun Google terhubung ke lebih dari satu profil aktif.")
      }
      activeRows.firstOrNull()
    }
  }

  private fun fetchTenantOption(accessToken: String, tenantId: String): TenantLoginOption? {
    if (tenantId.isBlank()) return null
    val requestUrl = buildString {
      append(BuildConfig.SUPABASE_URL)
      append("/rest/v1/tenants")
      append("?select=id,code,name,official_name,logo_url,active")
      append("&id=eq.")
      append(encodeValue(tenantId))
      append("&limit=1")
    }
    val connection = createConnection(requestUrl, "GET", accessToken)
    return connection.useJsonArrayResponse { rows ->
      val row = rows.optJSONObject(0) ?: return@useJsonArrayResponse null
      if (!row.optBooleanFlexible("active")) return@useJsonArrayResponse null
      TenantLoginOption(
        id = row.optCleanString("id"),
        code = row.optCleanString("code"),
        name = row.optCleanString("name"),
        officialName = row.optCleanString("official_name"),
        logoUrl = row.optCleanString("logo_url")
      )
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

  private fun buildGoogleRedirectUrl(): String {
    return "com.mim.guruapp://auth/callback"
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

private fun parseAuthErrorMessage(error: HttpResponseException, fallback: String): String {
  val raw = error.message.orEmpty().trim()
  if (raw.isBlank()) return fallback
  return runCatching {
    val json = JSONObject(raw)
    json.optCleanString("msg")
      .ifBlank { json.optCleanString("message") }
      .ifBlank { json.optCleanString("error_description") }
      .ifBlank { json.optCleanString("error") }
      .ifBlank { fallback }
  }.getOrElse { raw.ifBlank { fallback } }
}
