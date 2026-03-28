package com.mim.app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MimFirebaseMessagingService : FirebaseMessagingService() {
  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.d("MimFCM", "Refreshed token: $token")
    MainActivity.emitFcmToken(token)
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    val title = remoteMessage.data["title"]
      ?: remoteMessage.notification?.title
      ?: "Pesan baru"
    val body = remoteMessage.data["body"]
      ?: remoteMessage.notification?.body
      ?: "Anda menerima pesan baru."
    val threadId = remoteMessage.data["open_chat_thread_id"]
      ?: remoteMessage.data["thread_id"]
      ?: ""
    val notifyUserId = remoteMessage.data["notify_user_id"]
      ?: remoteMessage.data["user_id"]
      ?: remoteMessage.data["receiver_id"]
      ?: remoteMessage.data["karyawan_id"]
      ?: ""
    ChatNotificationHelper.show(this, title, body, threadId, notifyUserId)
  }
}
