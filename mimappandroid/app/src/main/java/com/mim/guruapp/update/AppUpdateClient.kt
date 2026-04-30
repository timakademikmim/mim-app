package com.mim.guruapp.update

import com.mim.guruapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
  val versionCode: Int,
  val versionName: String,
  val apkUrl: String,
  val releaseNotes: String,
  val mandatory: Boolean
)

class AppUpdateClient {
  suspend fun checkForUpdate(manifestUrl: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
    val normalizedUrl = manifestUrl.trim()
    if (normalizedUrl.isBlank()) return@withContext null

    runCatching {
      val connection = (URL(normalizedUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8000
        readTimeout = 8000
        setRequestProperty("Accept", "application/json")
      }
      try {
        if (connection.responseCode !in 200..299) return@withContext null
        val payload = connection.inputStream.bufferedReader().use(BufferedReader::readText)
        val json = JSONObject(payload)
        val info = AppUpdateInfo(
          versionCode = json.optInt("versionCode", 0),
          versionName = json.optString("versionName").trim(),
          apkUrl = json.optString("apkUrl").trim(),
          releaseNotes = json.optString("releaseNotes").trim(),
          mandatory = json.optBoolean("mandatory", false)
        )
        info.takeIf { it.versionCode > BuildConfig.VERSION_CODE && it.apkUrl.isNotBlank() }
      } finally {
        connection.disconnect()
      }
    }.getOrNull()
  }
}
