package com.mim.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class NotificationReplyReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val replyText = RemoteInput.getResultsFromIntent(intent)
      ?.getCharSequence(KEY_TEXT_REPLY)
      ?.toString()
      ?.trim()
      ?: ""
    val threadId = intent.getStringExtra(EXTRA_THREAD_ID)?.trim().orEmpty()
    val userId = intent.getStringExtra(EXTRA_USER_ID)?.trim().orEmpty()

    if (replyText.isBlank() || threadId.isBlank() || userId.isBlank()) return

    val pendingResult = goAsync()
    Thread {
      try {
        val sent = sendChatMessage(threadId, userId, replyText)
        if (!sent) {
          Log.w(TAG, "Direct reply gagal dikirim (thread=$threadId, user=$userId)")
        }
      } catch (error: Exception) {
        Log.e(TAG, "Error direct reply", error)
      } finally {
        pendingResult.finish()
      }
    }.start()
  }

  private fun sendChatMessage(
    threadId: String,
    userId: String,
    messageText: String
  ): Boolean {
    if (messageText.isBlank()) return false
    if (messageText.length > 4000) return false

    val insertPayload = JSONArray().put(
      JSONObject()
        .put("thread_id", threadId)
        .put("sender_id", userId)
        .put("message_text", messageText)
    )

    val insertOk = postJson(
      "$SUPABASE_URL/rest/v1/chat_messages",
      insertPayload.toString(),
      mapOf(
        "apikey" to SUPABASE_ANON_KEY,
        "Authorization" to "Bearer $SUPABASE_ANON_KEY",
        "Content-Type" to "application/json",
        "Prefer" to "return=minimal"
      )
    )
    if (!insertOk) return false

    val nowIso = nowIsoUtc()
    val updatePayload = JSONObject().put("last_message_at", nowIso).toString()
    postJson(
      "$SUPABASE_URL/rest/v1/chat_threads?id=eq.$threadId",
      updatePayload,
      mapOf(
        "apikey" to SUPABASE_ANON_KEY,
        "Authorization" to "Bearer $SUPABASE_ANON_KEY",
        "Content-Type" to "application/json"
      ),
      method = "PATCH"
    )

    val readPayload = JSONObject().put("last_read_at", nowIso).toString()
    postJson(
      "$SUPABASE_URL/rest/v1/chat_thread_members?thread_id=eq.$threadId&karyawan_id=eq.$userId",
      readPayload,
      mapOf(
        "apikey" to SUPABASE_ANON_KEY,
        "Authorization" to "Bearer $SUPABASE_ANON_KEY",
        "Content-Type" to "application/json"
      ),
      method = "PATCH"
    )

    return true
  }

  private fun postJson(
    targetUrl: String,
    body: String,
    headers: Map<String, String>,
    method: String = "POST"
  ): Boolean {
    var connection: HttpURLConnection? = null
    return try {
      val url = URL(targetUrl)
      connection = url.openConnection() as HttpURLConnection
      connection.requestMethod = method
      connection.connectTimeout = 7000
      connection.readTimeout = 7000
      connection.doOutput = true
      headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
      OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
        writer.write(body)
      }
      val code = connection.responseCode
      code in 200..299
    } catch (error: Exception) {
      Log.e(TAG, "HTTP request gagal: $targetUrl", error)
      false
    } finally {
      connection?.disconnect()
    }
  }

  private fun nowIsoUtc(): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    fmt.timeZone = TimeZone.getTimeZone("UTC")
    return fmt.format(Date())
  }

  companion object {
    private const val TAG = "MimNotifReply"
    const val ACTION_REPLY = "com.mim.app.ACTION_REPLY_FROM_NOTIFICATION"
    const val KEY_TEXT_REPLY = "key_text_reply"
    const val EXTRA_THREAD_ID = "open_chat_thread_id"
    const val EXTRA_USER_ID = "notify_user_id"
    private const val SUPABASE_URL = "https://optucpelkueqmlhwlbej.supabase.co"
    private const val SUPABASE_ANON_KEY =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9wdHVjcGVsa3VlcW1saHdsYmVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAxOTY4MTgsImV4cCI6MjA4NTc3MjgxOH0.Vqaey9pcnltu9uRbPk0J-AGWaGDZjQLw92pcRv67GNE"
  }
}
