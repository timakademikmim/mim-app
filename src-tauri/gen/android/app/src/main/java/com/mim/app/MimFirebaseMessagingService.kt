package com.mim.app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MimFirebaseMessagingService : FirebaseMessagingService() {
  private fun firstNonBlank(vararg values: String?): String {
    values.forEach { value ->
      val clean = value?.trim().orEmpty()
      if (clean.isNotEmpty()) return clean
    }
    return ""
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.d("MimFCM", "Refreshed token: $token")
    MainActivity.emitFcmToken(token)
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    val data = remoteMessage.data
    val title = firstNonBlank(
      data["title"],
      data["gcm.notification.title"],
      remoteMessage.notification?.title,
      "Pesan baru"
    )
    val body = firstNonBlank(
      data["body"],
      data["gcm.notification.body"],
      remoteMessage.notification?.body,
      "Anda menerima pesan baru."
    )
    val threadId = firstNonBlank(
      data["open_chat_thread_id"],
      data["thread_id"],
      data["gcm.notification.open_chat_thread_id"],
      data["gcm.notification.thread_id"]
    )
    val notifyUserId = firstNonBlank(
      data["notify_user_id"],
      data["user_id"],
      data["receiver_id"],
      data["karyawan_id"],
      data["gcm.notification.notify_user_id"],
      data["gcm.notification.user_id"]
    )
    ChatNotificationHelper.show(this, title, body, threadId, notifyUserId)
  }
}
