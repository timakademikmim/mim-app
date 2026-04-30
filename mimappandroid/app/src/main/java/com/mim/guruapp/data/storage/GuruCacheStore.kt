package com.mim.guruapp.data.storage

import android.content.Context
import com.mim.guruapp.data.model.DashboardPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class GuruCacheStore(private val context: Context) {
  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
  }

  private val cacheFile: File
    get() = File(context.filesDir, "guru_dashboard_cache.json")

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
}
