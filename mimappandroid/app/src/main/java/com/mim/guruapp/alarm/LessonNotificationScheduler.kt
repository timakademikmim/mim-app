package com.mim.guruapp.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.mim.guruapp.alarm.LessonNotificationReceiver.Companion.ACTION_TRIGGER_LESSON_NOTIFICATION
import com.mim.guruapp.alarm.LessonNotificationReceiver.Companion.EXTRA_BODY
import com.mim.guruapp.alarm.LessonNotificationReceiver.Companion.EXTRA_DATE_ISO
import com.mim.guruapp.alarm.LessonNotificationReceiver.Companion.EXTRA_DISTRIBUSI_ID
import com.mim.guruapp.alarm.LessonNotificationReceiver.Companion.EXTRA_LESSON_SLOT_ID
import com.mim.guruapp.alarm.LessonNotificationReceiver.Companion.EXTRA_NOTIFICATION_SEED
import com.mim.guruapp.alarm.LessonNotificationReceiver.Companion.EXTRA_TITLE
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.LessonNotificationSettings
import com.mim.guruapp.data.remote.GuruTeachingScheduleRemoteDataSource
import com.mim.guruapp.data.storage.LessonNotificationStore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class LessonNotificationScheduler(
  private val context: Context,
  private val notificationStore: LessonNotificationStore = LessonNotificationStore(context)
) {
  private val alarmManager = context.getSystemService(AlarmManager::class.java)

  suspend fun syncNotifications(
    settings: LessonNotificationSettings,
    events: List<CalendarEvent>
  ) {
    cancelNotificationIds(settings.scheduledNotificationIds)
    if (!settings.enabled) {
      notificationStore.updateScheduledNotificationIds(emptyList())
      return
    }

    LessonNotificationNotifier.ensureChannel(context)

    val nowMillis = System.currentTimeMillis()
    val maxDate = LocalDate.now().plusDays(45)
    val candidates = events
      .filter { it.categoryKey == GuruTeachingScheduleRemoteDataSource.CATEGORY_TEACHING }
      .filter { it.distribusiId.isNotBlank() }
      .mapNotNull { event ->
        val triggerAtMillis = event.toLessonNotificationTriggerMillis(settings.minutesBefore) ?: return@mapNotNull null
        val eventDate = runCatching { LocalDate.parse(event.startDateIso) }.getOrNull() ?: return@mapNotNull null
        if (eventDate.isAfter(maxDate)) return@mapNotNull null
        if (triggerAtMillis <= nowMillis + 15_000L) return@mapNotNull null
        ScheduledLessonNotificationCandidate(event, triggerAtMillis)
      }
      .sortedWith(compareBy<ScheduledLessonNotificationCandidate> { it.triggerAtMillis }.thenBy { it.event.title })

    val scheduledIds = mutableListOf<String>()
    candidates.forEach { candidate ->
      val notificationId = buildNotificationId(candidate.event, settings.minutesBefore)
      scheduleNotification(notificationId, candidate.event, candidate.triggerAtMillis)
      scheduledIds += notificationId
    }

    notificationStore.updateScheduledNotificationIds(scheduledIds)
  }

  suspend fun cancelAll(settings: LessonNotificationSettings) {
    cancelNotificationIds(settings.scheduledNotificationIds)
    notificationStore.updateScheduledNotificationIds(emptyList())
  }

  private fun scheduleNotification(
    notificationId: String,
    event: CalendarEvent,
    triggerAtMillis: Long
  ) {
    val title = "Jam pelajaran dimulai"
    val body = buildString {
      append(event.title)
      if (event.timeLabel.isNotBlank()) {
        append(" ")
        append(event.timeLabel)
      }
      append(". Tekan untuk buka input absensi.")
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      notificationId.hashCode(),
      Intent(context, LessonNotificationReceiver::class.java).apply {
        action = ACTION_TRIGGER_LESSON_NOTIFICATION
        putExtra(EXTRA_NOTIFICATION_SEED, notificationId)
        putExtra(EXTRA_TITLE, title)
        putExtra(EXTRA_BODY, body)
        putExtra(EXTRA_DATE_ISO, event.startDateIso)
        putExtra(EXTRA_DISTRIBUSI_ID, event.distribusiId)
        putExtra(EXTRA_LESSON_SLOT_ID, event.lessonSlotId)
      },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (TeachingReminderNotifier.canScheduleExactAlarms(context)) {
      alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    } else {
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
  }

  private fun cancelNotificationIds(ids: List<String>) {
    ids.distinct().forEach { notificationId ->
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        notificationId.hashCode(),
        Intent(context, LessonNotificationReceiver::class.java).apply {
          action = ACTION_TRIGGER_LESSON_NOTIFICATION
          putExtra(EXTRA_NOTIFICATION_SEED, notificationId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      alarmManager.cancel(pendingIntent)
      pendingIntent.cancel()
    }
  }

  private fun buildNotificationId(
    event: CalendarEvent,
    minutesBefore: Int
  ): String {
    val slotKey = event.lessonSlotId.ifBlank { event.timeLabel.replace(" ", "") }
    return "lesson_notification|${event.startDateIso}|${event.distribusiId}|$slotKey|$minutesBefore"
  }

  private data class ScheduledLessonNotificationCandidate(
    val event: CalendarEvent,
    val triggerAtMillis: Long
  )
}

private fun CalendarEvent.toLessonNotificationTriggerMillis(minutesBefore: Int): Long? {
  val date = runCatching { LocalDate.parse(startDateIso) }.getOrNull() ?: return null
  val startTime = timeLabel.substringBefore("-").trim().takeIf { it.matches(Regex("\\d{2}:\\d{2}")) } ?: return null
  val localTime = runCatching { LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())) }.getOrNull()
    ?: return null
  return LocalDateTime.of(date, localTime)
    .minusMinutes(minutesBefore.coerceAtLeast(0).toLong())
    .atZone(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()
}
