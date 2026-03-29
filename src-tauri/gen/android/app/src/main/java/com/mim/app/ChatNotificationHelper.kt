package com.mim.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput

object ChatNotificationHelper {
  private const val CHANNEL_ID = "chat_messages"

  fun show(context: Context, title: String, body: String, threadId: String, notifyUserId: String) {
    ensureChannel(context)

    val safeTitle = if (title.isBlank()) "Pesan baru" else title
    val safeBody = if (body.isBlank()) "Anda menerima pesan baru." else body

    val openIntent = Intent(context, MainActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      if (threadId.isNotBlank()) {
        putExtra("open_chat_thread_id", threadId)
      }
      if (notifyUserId.isNotBlank()) {
        putExtra("notify_user_id", notifyUserId)
      }
    }
    val openPendingIntent = PendingIntent.getActivity(
      context,
      0,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val sender = Person.Builder()
      .setName(safeTitle)
      .build()

    val messagingStyle = NotificationCompat.MessagingStyle(sender)
      .setConversationTitle("Pesan")
      .addMessage(safeBody, System.currentTimeMillis(), sender)

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(safeTitle)
      .setContentText(safeBody)
      .setStyle(messagingStyle)
      .setAutoCancel(true)
      .setContentIntent(openPendingIntent)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

    if (threadId.isNotBlank()) {
      val replyPendingIntent = PendingIntent.getBroadcast(
        context,
        (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
        Intent(context, NotificationReplyReceiver::class.java).apply {
          action = NotificationReplyReceiver.ACTION_REPLY
          putExtra(NotificationReplyReceiver.EXTRA_THREAD_ID, threadId)
          if (notifyUserId.isNotBlank()) {
            putExtra(NotificationReplyReceiver.EXTRA_USER_ID, notifyUserId)
          }
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
      )
      val remoteInput = RemoteInput.Builder(NotificationReplyReceiver.KEY_TEXT_REPLY)
        .setLabel("Balas pesan...")
        .build()
      val replyAction = NotificationCompat.Action.Builder(
        android.R.drawable.ic_menu_send,
        "Balas",
        replyPendingIntent
      ).addRemoteInput(remoteInput)
        .setAllowGeneratedReplies(true)
        .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
        .setShowsUserInterface(false)
        .build()
      builder.addAction(replyAction)
    }

    NotificationManagerCompat.from(context)
      .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), builder.build())
  }

  private fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    val channel = NotificationChannel(
      CHANNEL_ID,
      "Pesan Chat",
      NotificationManager.IMPORTANCE_HIGH
    )
    manager.createNotificationChannel(channel)
  }
}
