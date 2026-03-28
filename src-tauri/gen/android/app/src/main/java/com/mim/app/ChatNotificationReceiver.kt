package com.mim.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ChatNotificationReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_SHOW) return

    val title = String(intent.getStringExtra(EXTRA_TITLE) ?: "").trim()
    val body = String(intent.getStringExtra(EXTRA_BODY) ?: "").trim()
    val threadId = String(intent.getStringExtra(EXTRA_THREAD_ID) ?: "").trim()
    val notifyUserId = String(intent.getStringExtra(EXTRA_USER_ID) ?: "").trim()

    ChatNotificationHelper.show(
      context,
      if (title.isBlank()) "Pesan baru" else title,
      if (body.isBlank()) "Anda menerima pesan baru." else body,
      threadId,
      notifyUserId
    )
  }

  companion object {
    const val ACTION_SHOW = "com.mim.app.SHOW_CHAT_NOTIFICATION"
    const val EXTRA_TITLE = "title"
    const val EXTRA_BODY = "body"
    const val EXTRA_THREAD_ID = "open_chat_thread_id"
    const val EXTRA_USER_ID = "notify_user_id"
  }
}
