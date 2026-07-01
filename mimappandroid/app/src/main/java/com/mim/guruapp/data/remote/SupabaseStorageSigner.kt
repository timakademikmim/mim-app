package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object SupabaseStorageSigner {
  fun createSignedUrl(
    bucket: String,
    storagePath: String,
    expiresInSeconds: Int = 604_800
  ): String {
    check(SupabaseRequestAuth.isAuthenticated()) { "Sesi login berakhir. Silakan masuk kembali." }
    val encodedPath = storagePath
      .split('/')
      .filter { it.isNotBlank() }
      .joinToString("/") { segment ->
        URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
      }
    val requestUrl = "${BuildConfig.SUPABASE_URL}/storage/v1/object/sign/$bucket/$encodedPath"
    val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      connectTimeout = 15_000
      readTimeout = 20_000
      doOutput = true
      applySupabaseRequestHeaders()
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Accept", "application/json")
    }
    connection.outputStream.use { stream ->
      stream.write(JSONObject().put("expiresIn", expiresInSeconds.coerceAtLeast(60)).toString().toByteArray())
    }
    return try {
      val status = connection.responseCode
      val stream = if (status in 200..299) connection.inputStream else connection.errorStream
      val payload = stream?.let { BufferedReader(InputStreamReader(it)).use(BufferedReader::readText) }.orEmpty()
      if (status !in 200..299) throw IllegalStateException(payload.ifBlank { "HTTP $status" })
      val json = JSONObject(payload.ifBlank { "{}" })
      val signedUrl = json.optString("signedURL").ifBlank { json.optString("signedUrl") }
      when {
        signedUrl.startsWith("http") -> signedUrl
        signedUrl.startsWith("/") -> BuildConfig.SUPABASE_URL + signedUrl
        else -> throw IllegalStateException("Link aman file tidak tersedia.")
      }
    } finally {
      connection.disconnect()
    }
  }
}
