package com.mim.guruapp.update

import com.mim.guruapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.OffsetDateTime

data class AppUpdateInfo(
  val versionCode: Int,
  val versionName: String,
  val apkUrl: String,
  val releaseNotes: String,
  val features: List<String> = emptyList(),
  val fixes: List<String> = emptyList(),
  val mandatory: Boolean
)

data class AppServiceNotice(
  val id: String,
  val title: String,
  val message: String,
  val acknowledgementLabel: String,
  val startsAt: Instant? = null,
  val endsAt: Instant? = null,
  val blockAt: Instant? = null
) {
  fun shouldDisplayAt(now: Instant = Instant.now()): Boolean {
    if (startsAt?.isAfter(now) == true) return false
    if (isBlockingAt(now)) return true
    return endsAt?.isAfter(now) != false
  }

  fun isBlockingAt(now: Instant = Instant.now()): Boolean =
    blockAt?.let { !now.isBefore(it) } == true
}

data class AppRemoteConfig(
  val updateInfo: AppUpdateInfo?,
  val serviceNotice: AppServiceNotice?
)

class AppUpdateClient {
  suspend fun checkForRemoteConfig(manifestUrl: String): AppRemoteConfig? = withContext(Dispatchers.IO) {
    val normalizedUrl = manifestUrl.trim()
    if (normalizedUrl.isBlank()) return@withContext null

    runCatching {
      val cacheSeparator = if (normalizedUrl.contains("?")) "&" else "?"
      val requestUrl = "$normalizedUrl${cacheSeparator}ts=${System.currentTimeMillis() / 60_000L}"
      val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8000
        readTimeout = 8000
        useCaches = false
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Cache-Control", "no-cache")
      }
      try {
        if (connection.responseCode !in 200..299) return@withContext null
        val payload = connection.inputStream.bufferedReader().use(BufferedReader::readText)
        val json = JSONObject(payload)
        val updateInfo = AppUpdateInfo(
          versionCode = json.optInt("versionCode", 0),
          versionName = json.optString("versionName").trim(),
          apkUrl = json.optString("apkUrl").trim(),
          releaseNotes = json.optString("releaseNotes").trim(),
          features = json.optStringArray("features"),
          fixes = json.optStringArray("fixes"),
          mandatory = json.optBoolean("mandatory", false)
        )
        AppRemoteConfig(
          updateInfo = updateInfo.takeIf {
            it.versionCode > BuildConfig.VERSION_CODE && it.apkUrl.isNotBlank()
          },
          serviceNotice = json.optServiceNotice()
        )
      } finally {
        connection.disconnect()
      }
    }.getOrNull()
  }

  suspend fun checkForUpdate(manifestUrl: String): AppUpdateInfo? =
    checkForRemoteConfig(manifestUrl)?.updateInfo
}

private fun JSONObject.optStringArray(key: String): List<String> {
  val array = optJSONArray(key) ?: return emptyList()
  return buildList {
    for (index in 0 until array.length()) {
      val value = array.optString(index).trim()
      if (value.isNotBlank()) add(value)
    }
  }
}

private fun JSONObject.optServiceNotice(): AppServiceNotice? {
  val notice = optJSONObject("serviceNotice") ?: return null
  if (!notice.optBoolean("enabled", false)) return null

  val id = notice.optString("id").trim()
  val title = notice.optString("title").trim()
  val message = notice.optString("message").trim()
  if (id.isBlank() || title.isBlank() || message.isBlank()) return null

  return AppServiceNotice(
    id = id,
    title = title,
    message = message,
    acknowledgementLabel = notice.optString("acknowledgementLabel")
      .trim()
      .ifBlank { "Saya mengerti" },
    startsAt = notice.optInstant("startsAt"),
    endsAt = notice.optInstant("endsAt"),
    blockAt = notice.optInstant("blockAt")
  )
}

private fun JSONObject.optInstant(key: String): Instant? {
  val value = optString(key).trim()
  if (value.isBlank()) return null
  return runCatching { OffsetDateTime.parse(value).toInstant() }
    .recoverCatching { Instant.parse(value) }
    .getOrNull()
}
