package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.SessionSnapshot
import java.net.HttpURLConnection

internal object SupabaseRequestAuth {
  @Volatile
  private var accessToken: String = ""

  @Volatile
  private var tenantId: String = ""

  @Volatile
  private var authUserId: String = ""

  fun update(session: SessionSnapshot) {
    accessToken = session.authAccessToken.takeIf { session.usesSupabaseAuth }.orEmpty()
    tenantId = session.tenantId
    authUserId = session.authUserId
  }

  fun clear() {
    accessToken = ""
    tenantId = ""
    authUserId = ""
  }

  fun bearerToken(): String = accessToken.ifBlank { BuildConfig.SUPABASE_ANON_KEY }

  fun isAuthenticated(): Boolean {
    return accessToken.isNotBlank() && tenantId.isNotBlank() && authUserId.isNotBlank()
  }

  fun tenantStoragePath(relativePath: String): String {
    val cleanPath = relativePath
      .replace('\\', '/')
      .split('/')
      .map(String::trim)
      .filter { it.isNotBlank() && it != "." && it != ".." }
      .joinToString("/")
    require(cleanPath.isNotBlank()) { "Path file Storage tidak valid." }
    return if (isAuthenticated()) "${tenantId.trim()}/$cleanPath" else cleanPath
  }
}

internal fun HttpURLConnection.applySupabaseRequestHeaders(
  bearerToken: String = SupabaseRequestAuth.bearerToken()
) {
  setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
  setRequestProperty("Authorization", "Bearer $bearerToken")
}
