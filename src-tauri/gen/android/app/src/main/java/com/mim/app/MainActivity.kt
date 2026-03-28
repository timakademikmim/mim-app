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
    setIntent(intent)
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
    val threadId = extractThreadId(intent)
    if (!threadId.isNullOrBlank()) {
      emitChatOpen(threadId)
      return
    }
    if (extractRoute(intent) == "chat") {
      emitChatPanelOpen()
    }
  }

  private fun extractThreadId(intent: Intent): String? {
    val direct = intent.getStringExtra("open_chat_thread_id")
      ?: intent.getStringExtra("thread_id")
    if (!direct.isNullOrBlank()) return direct

    val extras = intent.extras
    if (extras != null) {
      val candidates = arrayOf(
        "open_chat_thread_id",
        "thread_id",
        "gcm.notification.open_chat_thread_id",
        "gcm.notification.thread_id",
        "google.c.a.c_l"
      )
      for (key in candidates) {
        val value = extras.getString(key)
        if (!value.isNullOrBlank()) return value
      }
    }

    val data = intent.data
    if (data != null) {
      val fromUri = data.getQueryParameter("open_chat_thread_id")
        ?: data.getQueryParameter("thread_id")
      if (!fromUri.isNullOrBlank()) return fromUri
    }
    return null
  }

  private fun extractRoute(intent: Intent): String {
    val direct = intent.getStringExtra("route")
      ?: intent.getStringExtra("gcm.notification.route")
    if (!direct.isNullOrBlank()) return direct.trim().lowercase()

    val extras = intent.extras
    if (extras != null) {
      val candidates = arrayOf("route", "gcm.notification.route")
      for (key in candidates) {
        val value = extras.getString(key)
        if (!value.isNullOrBlank()) return value.trim().lowercase()
      }
    }

    val data = intent.data
    if (data != null) {
      val fromUri = data.getQueryParameter("route")
      if (!fromUri.isNullOrBlank()) return fromUri.trim().lowercase()
    }
    return ""
  }

  companion object {
    @Volatile
    private var webViewRef: WebView? = null
    @Volatile
    private var pendingFcmToken: String? = null
    @Volatile
    private var pendingThreadId: String? = null
    @Volatile
    private var pendingOpenChatPanel: Boolean = false
    @Volatile
    private var pendingThreadAttempts: Int = 0
    private const val MAX_PENDING_THREAD_ATTEMPTS = 10

    fun setWebView(webView: WebView) {
      webViewRef = webView
      pendingFcmToken?.let { token ->
        pendingFcmToken = null
        emitFcmToken(token)
      }
      pendingThreadId?.let { tid ->
        emitChatOpen(tid)
      }
      if (pendingOpenChatPanel) {
        emitChatPanelOpen()
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
      val webView = webViewRef
      if (webView == null) {
        pendingThreadId = threadId
        pendingThreadAttempts = 0
        return
      }
      val currentUrl = webView.url ?: ""
      if ((currentUrl.isBlank() || currentUrl == "about:blank") &&
          pendingThreadAttempts < MAX_PENDING_THREAD_ATTEMPTS) {
        pendingThreadId = threadId
        pendingThreadAttempts += 1
        webView.postDelayed({ emitChatOpen(threadId) }, 450L)
        return
      }
      pendingThreadId = null
      pendingThreadAttempts = 0
      val safeThread = JSONObject.quote(threadId)
      val script =
        "try { window.__mimPendingOpenThread = $safeThread; localStorage.setItem('chat_open_thread_id', $safeThread); } catch (e) {}" +
        "window.dispatchEvent(new CustomEvent('mim-open-chat-thread', { detail: { threadId: $safeThread } }));"
      emitScript(script)
    }

    fun emitChatPanelOpen() {
      val webView = webViewRef
      if (webView == null) {
        pendingOpenChatPanel = true
        return
      }
      val currentUrl = webView.url ?: ""
      if (currentUrl.isBlank() || currentUrl == "about:blank") {
        pendingOpenChatPanel = true
        webView.postDelayed({ emitChatPanelOpen() }, 450L)
        return
      }
      pendingOpenChatPanel = false
      val script = "window.dispatchEvent(new CustomEvent('mim-open-chat-panel'));"
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
