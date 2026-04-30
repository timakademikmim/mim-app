package com.mim.guruapp.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.ACTION_TRIGGER_TEACHING_REMINDER
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_DATE_ISO
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_DISTRIBUSI_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_LESSON_SLOT_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_NOTIFICATION_BODY
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_NOTIFICATION_TITLE
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_REMINDER_ID
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.TeachingReminderSettings
import com.mim.guruapp.data.remote.GuruTeachingScheduleRemoteDataSource
import com.mim.guruapp.data.storage.TeachingReminderStore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class TeachingReminderScheduler(
  private val context: Context,
  private val reminderStore: TeachingReminderStore = TeachingReminderStore(context)
) {
  private val alarmManager = context.getSystemService(AlarmManager::class.java)

  suspend fun syncReminders(
    settings: TeachingReminderSettings,
    events: List<CalendarEvent>
  ) {
    cancelReminderIds(settings.scheduledReminderIds)
    if (!settings.enabled) {
      reminderStore.updateScheduledReminderIds(emptyList())
      return
    }

    val nowMillis = System.currentTimeMillis()
    val maxDate = LocalDate.now().plusDays(45)
    val candidates = events
      .filter { it.categoryKey == GuruTeachingScheduleRemoteDataSource.CATEGORY_TEACHING }
      .filter { it.distribusiId.isNotBlank() }
      .filter { event ->
        settings.targetMode != "specific" || settings.selectedDistribusiIds.contains(event.distribusiId)
      }
      .mapNotNull { event ->
        val reminderAtMillis = event.toReminderTriggerMillis(settings.minutesBefore) ?: return@mapNotNull null
        val eventDate = runCatching { LocalDate.parse(event.startDateIso) }.getOrNull() ?: return@mapNotNull null
        if (eventDate.isAfter(maxDate)) return@mapNotNull null
        if (reminderAtMillis <= nowMillis + 15_000L) return@mapNotNull null
        ScheduledReminderCandidate(event, reminderAtMillis)
      }
      .sortedWith(compareBy<ScheduledReminderCandidate> { it.triggerAtMillis }.thenBy { it.event.title })

    val selectedCandidates = if (settings.repeatMode == "once") {
      candidates.take(1)
    } else {
      candidates
    }

    val scheduledIds = mutableListOf<String>()
    selectedCandidates.forEach { candidate ->
      val reminderId = buildReminderId(candidate.event, settings.minutesBefore)
      scheduleReminder(reminderId, candidate.event, candidate.triggerAtMillis)
      scheduledIds += reminderId
    }

    reminderStore.updateScheduledReminderIds(scheduledIds)
  }

  suspend fun cancelAll(settings: TeachingReminderSettings) {
    cancelReminderIds(settings.scheduledReminderIds)
    reminderStore.updateScheduledReminderIds(emptyList())
  }

  private fun scheduleReminder(
    reminderId: String,
    event: CalendarEvent,
    triggerAtMillis: Long
  ) {
    val notificationTitle = "Pengingat Jam Pelajaran"
    val notificationBody = buildString {
      append(event.title)
      append(" dimulai ")
      append(event.timeLabel)
      append(". Tekan untuk buka input absensi.")
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      reminderId.hashCode(),
      Intent(context, TeachingReminderReceiver::class.java).apply {
        action = ACTION_TRIGGER_TEACHING_REMINDER
        putExtra(EXTRA_REMINDER_ID, reminderId)
        putExtra(EXTRA_NOTIFICATION_TITLE, notificationTitle)
        putExtra(EXTRA_NOTIFICATION_BODY, notificationBody)
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

  private fun cancelReminderIds(ids: List<String>) {
    ids.distinct().forEach { reminderId ->
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        reminderId.hashCode(),
        Intent(context, TeachingReminderReceiver::class.java).apply {
          action = ACTION_TRIGGER_TEACHING_REMINDER
          putExtra(EXTRA_REMINDER_ID, reminderId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      alarmManager.cancel(pendingIntent)
      pendingIntent.cancel()
    }
  }

  private fun buildReminderId(
    event: CalendarEvent,
    minutesBefore: Int
  ): String {
    val slotKey = event.lessonSlotId.ifBlank { event.timeLabel.replace(" ", "") }
    return "teaching|${event.startDateIso}|${event.distribusiId}|$slotKey|$minutesBefore"
  }

  private data class ScheduledReminderCandidate(
    val event: CalendarEvent,
    val triggerAtMillis: Long
  )
}

private fun CalendarEvent.toReminderTriggerMillis(minutesBefore: Int): Long? {
  val date = runCatching { LocalDate.parse(startDateIso) }.getOrNull() ?: return null
  val startTime = timeLabel.substringBefore("-").trim().takeIf { it.matches(Regex("\\d{2}:\\d{2}")) } ?: return null
  val localTime = runCatching { LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())) }.getOrNull()
    ?: return null
  return LocalDateTime.of(date, localTime)
    .minusMinutes(minutesBefore.toLong())
    .atZone(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()
}
