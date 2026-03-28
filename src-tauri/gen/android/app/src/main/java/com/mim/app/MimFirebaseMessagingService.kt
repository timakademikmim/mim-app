package com.mim.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
    val title = remoteMessage.notification?.title ?: "Pesan baru"
    val body = remoteMessage.notification?.body
      ?: remoteMessage.data["body"]
      ?: "Anda menerima pesan baru."
    val threadId = remoteMessage.data["open_chat_thread_id"]
      ?: remoteMessage.data["thread_id"]
      ?: ""
    showNotification(title, body, threadId)
  }

  private fun showNotification(title: String, body: String, threadId: String) {
    val channelId = "chat_messages"
    val manager = getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        channelId,
        "Pesan Chat",
        NotificationManager.IMPORTANCE_HIGH
      )
      manager?.createNotificationChannel(channel)
    }

    val intent = Intent(this, MainActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      if (threadId.isNotBlank()) {
        putExtra("open_chat_thread_id", threadId)
      }
    }

    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(this, channelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .build()

    NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
  }
}
