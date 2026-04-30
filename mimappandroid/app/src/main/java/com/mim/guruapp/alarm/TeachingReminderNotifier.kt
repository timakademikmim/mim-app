package com.mim.guruapp.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mim.guruapp.MainActivity
import com.mim.guruapp.R
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_DATE_ISO
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_DISTRIBUSI_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_LESSON_SLOT_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_NOTIFICATION_BODY
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_NOTIFICATION_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_NOTIFICATION_TITLE
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_REMINDER_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_RINGTONE_URI
import com.mim.guruapp.data.model.TeachingReminderSettings
import kotlin.math.absoluteValue

object TeachingReminderNotifier {
  const val ACTION_OPEN_FROM_REMINDER = "com.mim.guruapp.action.OPEN_FROM_REMINDER"
  const val ACTION_DISMISS_REMINDER = "com.mim.guruapp.action.DISMISS_REMINDER"
  const val EXTRA_OPEN_INPUT_ABSENSI = "open_input_absensi"
  const val EXTRA_SOURCE = "source"

  fun ensureChannel(
    context: Context,
    settings: TeachingReminderSettings
  ): String {
    val channelId = buildChannelId(settings)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(NotificationManager::class.java)
      if (manager.getNotificationChannel(channelId) == null) {
        val soundUri = settings.ringtoneUri.toAlarmUri()
        val channel = NotificationChannel(
          channelId,
          "Pengingat Jadwal Mengajar",
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          description = "Pengingat jam pelajaran guru"
          setShowBadge(true)
          enableVibration(true)
          lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
          setSound(
            soundUri,
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ALARM)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
        }
        manager.createNotificationChannel(channel)
      }
    }
    return channelId
  }

  fun showReminderNotification(
    context: Context,
    reminderId: String,
    title: String,
    body: String,
    dateIso: String,
    distribusiId: String,
    lessonSlotId: String,
    settings: TeachingReminderSettings
  ) {
    val channelId = ensureChannel(context, settings)
    val notificationId = reminderId.toNotificationId()
    val openIntent = buildOpenMainIntent(
      context = context,
      reminderId = reminderId,
      title = title,
      body = body,
      dateIso = dateIso,
      distribusiId = distribusiId,
      lessonSlotId = lessonSlotId
    )
    val fullScreenIntent = PendingIntent.getActivity(
      context,
      notificationId,
      buildReminderAlertIntent(
        context = context,
        reminderId = reminderId,
        title = title,
        body = body,
        dateIso = dateIso,
        distribusiId = distribusiId,
        lessonSlotId = lessonSlotId,
        settings = settings
      ),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val dismissIntent = PendingIntent.getBroadcast(
      context,
      notificationId + 1,
      Intent(context, TeachingReminderActionReceiver::class.java).apply {
        action = ACTION_DISMISS_REMINDER
        putExtra(EXTRA_NOTIFICATION_ID, notificationId)
      },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setContentIntent(openIntent)
      .setAutoCancel(true)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setFullScreenIntent(fullScreenIntent, true)
      .addAction(0, "Matikan", dismissIntent)
      .addAction(0, "Buka", openIntent)

    NotificationManagerCompat.from(context).notify(notificationId, builder.build())
  }

  fun showReminderAlert(
    context: Context,
    reminderId: String,
    title: String,
    body: String,
    dateIso: String,
    distribusiId: String,
    lessonSlotId: String,
    settings: TeachingReminderSettings
  ) {
    runCatching {
      context.startActivity(
        buildReminderAlertIntent(
          context = context,
          reminderId = reminderId,
          title = title,
          body = body,
          dateIso = dateIso,
          distribusiId = distribusiId,
          lessonSlotId = lessonSlotId,
          settings = settings
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
      )
    }
  }

  fun cancelNotification(
    context: Context,
    notificationId: Int
  ) {
    NotificationManagerCompat.from(context).cancel(notificationId)
  }

  fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    return alarmManager.canScheduleExactAlarms()
  }

  fun canUseFullScreenIntent(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
    val manager = context.getSystemService(NotificationManager::class.java)
    return manager.canUseFullScreenIntent()
  }

  fun canDrawOverlays(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    return Settings.canDrawOverlays(context)
  }

  fun buildOverlayPermissionIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
        data = Uri.parse("package:${context.packageName}")
      }
    } else {
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
      }
    }
  }

  fun startOverlayService(
    context: Context,
    reminderId: String,
    title: String,
    body: String,
    dateIso: String,
    distribusiId: String,
    lessonSlotId: String,
    settings: TeachingReminderSettings
  ) {
    if (!canDrawOverlays(context)) return
    runCatching {
      ContextCompat.startForegroundService(
        context,
        TeachingReminderOverlayService.buildStartIntent(
          context = context,
          reminderId = reminderId,
          title = title,
          body = body,
          dateIso = dateIso,
          distribusiId = distribusiId,
          lessonSlotId = lessonSlotId,
          ringtoneUri = settings.ringtoneUri
        )
      )
    }
  }

  fun buildFullScreenIntentSettingsIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
        data = Uri.parse("package:${context.packageName}")
      }
    } else {
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
      }
    }
  }

  fun buildOpenMainIntent(
    context: Context,
    reminderId: String,
    title: String,
    body: String,
    dateIso: String,
    distribusiId: String,
    lessonSlotId: String
  ): PendingIntent {
    val notificationId = reminderId.toNotificationId()
    return PendingIntent.getActivity(
      context,
      notificationId + 2,
      Intent(context, MainActivity::class.java).apply {
        action = ACTION_OPEN_FROM_REMINDER
        putExtra(EXTRA_OPEN_INPUT_ABSENSI, true)
        putExtra(EXTRA_SOURCE, "teaching_reminder")
        putExtra(EXTRA_REMINDER_ID, reminderId)
        putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        putExtra(EXTRA_NOTIFICATION_TITLE, title)
        putExtra(EXTRA_NOTIFICATION_BODY, body)
        putExtra(EXTRA_DATE_ISO, dateIso)
        putExtra(EXTRA_DISTRIBUSI_ID, distribusiId)
        putExtra(EXTRA_LESSON_SLOT_ID, lessonSlotId)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  fun buildChannelId(settings: TeachingReminderSettings): String {
    val suffix = settings.ringtoneUri.ifBlank { "default" }.hashCode().absoluteValue
    return "teaching_reminder_$suffix"
  }

  private fun buildReminderAlertIntent(
    context: Context,
    reminderId: String,
    title: String,
    body: String,
    dateIso: String,
    distribusiId: String,
    lessonSlotId: String,
    settings: TeachingReminderSettings
  ): Intent {
    val notificationId = reminderId.toNotificationId()
    return Intent(context, ReminderAlertActivity::class.java).apply {
      putExtra(EXTRA_REMINDER_ID, reminderId)
      putExtra(EXTRA_NOTIFICATION_ID, notificationId)
      putExtra(EXTRA_NOTIFICATION_TITLE, title)
      putExtra(EXTRA_NOTIFICATION_BODY, body)
      putExtra(EXTRA_DATE_ISO, dateIso)
      putExtra(EXTRA_DISTRIBUSI_ID, distribusiId)
      putExtra(EXTRA_LESSON_SLOT_ID, lessonSlotId)
      putExtra(EXTRA_RINGTONE_URI, settings.ringtoneUri)
    }
  }

  fun String.toNotificationId(): Int = hashCode().absoluteValue

  private fun String.toAlarmUri(): Uri {
    return runCatching { Uri.parse(this) }.getOrNull()
      ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
  }
}
