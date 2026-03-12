package com.mim.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

class MainActivity : TauriActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    requestNotificationPermissionIfNeeded()
    fetchFcmToken()
    handleIntent(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  override fun onWebViewCreate(webView: WebView) {
    super.onWebViewCreate(webView)
    setWebView(webView)
  }

  private fun requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val status = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
    if (status == PackageManager.PERMISSION_GRANTED) return
    ActivityCompat.requestPermissions(
      this,
      arrayOf(Manifest.permission.POST_NOTIFICATIONS),
      1001
    )
  }

  private fun fetchFcmToken() {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
      if (!task.isSuccessful) return@addOnCompleteListener
      val token = task.result ?: return@addOnCompleteListener
      emitFcmToken(token)
    }
  }

  private fun handleIntent(intent: Intent?) {
    if (intent == null) return
    val threadId = intent.getStringExtra("open_chat_thread_id") ?: return
    if (threadId.isBlank()) return
    emitChatOpen(threadId)
  }

  companion object {
    @Volatile
    private var webViewRef: WebView? = null
    @Volatile
    private var pendingFcmToken: String? = null
    @Volatile
    private var pendingThreadId: String? = null

    fun setWebView(webView: WebView) {
      webViewRef = webView
      pendingFcmToken?.let { token ->
        pendingFcmToken = null
        emitFcmToken(token)
      }
      pendingThreadId?.let { tid ->
        pendingThreadId = null
        emitChatOpen(tid)
      }
    }

    fun emitFcmToken(token: String) {
      if (webViewRef == null) {
        pendingFcmToken = token
        return
      }
      val safeToken = JSONObject.quote(token)
      val script =
        "window.__mimLastFcmToken=$safeToken; window.dispatchEvent(new CustomEvent('mim-fcm-token', { detail: { token: $safeToken } }));"
      emitScript(script)
    }

    fun emitChatOpen(threadId: String) {
      if (webViewRef == null) {
        pendingThreadId = threadId
        return
      }
      val safeThread = JSONObject.quote(threadId)
      val script =
        "window.dispatchEvent(new CustomEvent('mim-open-chat-thread', { detail: { threadId: $safeThread } }));"
      emitScript(script)
    }

    private fun emitScript(script: String) {
      val webView = webViewRef ?: return
      webView.post {
        webView.evaluateJavascript(script, null)
      }
    }
  }
}
