package com.mim.guruapp.data.storage

import android.content.Context
import com.mim.guruapp.data.model.DashboardPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

class GuruCacheStore(private val context: Context) {
  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
  }

  private val scopePreferences = context.getSharedPreferences(
    "mim_guru_cache_scope",
    Context.MODE_PRIVATE
  )

  private val cacheFile: File
    get() {
      val scope = scopePreferences.getString(ACTIVE_SCOPE_KEY, "").orEmpty()
      return if (scope.isBlank()) {
        File(context.filesDir, LEGACY_CACHE_FILE)
      } else {
        File(context.filesDir, "guru_dashboard_cache_$scope.json")
      }
    }

  fun setScope(tenantId: String, teacherId: String) {
    val source = "${tenantId.trim()}:${teacherId.trim().lowercase()}"
    if (tenantId.isBlank() || teacherId.isBlank()) {
      clearScope()
      return
    }
    val digest = MessageDigest.getInstance("SHA-256")
      .digest(source.toByteArray(Charsets.UTF_8))
      .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
      .take(24)
    scopePreferences.edit().putString(ACTIVE_SCOPE_KEY, digest).apply()
  }

  fun clearScope() {
    scopePreferences.edit().remove(ACTIVE_SCOPE_KEY).apply()
  }

  suspend fun readDashboard(): DashboardPayload? = withContext(Dispatchers.IO) {
    if (!cacheFile.exists()) return@withContext null
    runCatching {
      json.decodeFromString<DashboardPayload>(cacheFile.readText())
    }.getOrNull()
  }

  suspend fun writeDashboard(payload: DashboardPayload) = withContext(Dispatchers.IO) {
    val parentDir = cacheFile.parentFile
    if (parentDir != null && !parentDir.exists()) {
      parentDir.mkdirs()
    }
    cacheFile.writeText(json.encodeToString(payload))
  }

  private companion object {
    const val ACTIVE_SCOPE_KEY = "active_scope"
    const val LEGACY_CACHE_FILE = "guru_dashboard_cache.json"
  }
}
