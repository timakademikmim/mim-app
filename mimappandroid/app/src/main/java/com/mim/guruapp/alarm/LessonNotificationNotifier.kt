package com.mim.guruapp.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mim.guruapp.MainActivity
import com.mim.guruapp.alarm.TeachingReminderNotifier.EXTRA_OPEN_INPUT_ABSENSI
import com.mim.guruapp.alarm.TeachingReminderNotifier.EXTRA_SOURCE
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_DATE_ISO
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_DISTRIBUSI_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_LESSON_SLOT_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_NOTIFICATION_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_REMINDER_ID
import kotlin.math.absoluteValue

object LessonNotificationNotifier {
  const val CHANNEL_ID = "lesson_schedule_notifications"
  const val ACTION_OPEN_FROM_LESSON_NOTIFICATION = "com.mim.guruapp.action.OPEN_FROM_LESSON_NOTIFICATION"

  fun ensureChannel(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(NotificationManager::class.java)
      if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          "Jadwal Pelajaran",
          NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
          description = "Notifikasi saat jam pelajaran guru dimulai"
          setShowBadge(true)
          enableVibration(true)
          lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
      }
    }
    return CHANNEL_ID
  }

  fun showLessonNotification(
    context: Context,
    notificationIdSeed: String,
    title: String,
    body: String,
    dateIso: String,
    distribusiId: String,
    lessonSlotId: String
  ) {
    if (!hasNotificationPermission(context)) return
    val notificationId = notificationIdSeed.toNotificationId()
    val openIntent = PendingIntent.getActivity(
      context,
      notificationId,
      Intent(context, MainActivity::class.java).apply {
        action = ACTION_OPEN_FROM_LESSON_NOTIFICATION
        putExtra(EXTRA_OPEN_INPUT_ABSENSI, true)
        putExtra(EXTRA_SOURCE, "lesson_notification")
        putExtra(EXTRA_REMINDER_ID, notificationIdSeed)
        putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        putExtra(EXTRA_DATE_ISO, dateIso)
        putExtra(EXTRA_DISTRIBUSI_ID, distribusiId)
        putExtra(EXTRA_LESSON_SLOT_ID, lessonSlotId)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, ensureChannel(context))
      .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setContentIntent(openIntent)
      .setAutoCancel(true)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .build()

    NotificationManagerCompat.from(context).notify(notificationId, notification)
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
  }

  fun String.toNotificationId(): Int = hashCode().absoluteValue
}
