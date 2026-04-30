package com.mim.guruapp.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mim.guruapp.data.storage.TeachingReminderStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TeachingReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_TRIGGER_TEACHING_REMINDER) return
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val reminderStore = TeachingReminderStore(context)
        val settings = reminderStore.readSettings()
        if (!settings.enabled) return@launch

        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID).orEmpty()
        val title = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE).orEmpty().ifBlank { "Pengingat Jam Pelajaran" }
        val body = intent.getStringExtra(EXTRA_NOTIFICATION_BODY).orEmpty().ifBlank { "Sudah waktunya membuka input absensi." }
        val dateIso = intent.getStringExtra(EXTRA_DATE_ISO).orEmpty()
        val distribusiId = intent.getStringExtra(EXTRA_DISTRIBUSI_ID).orEmpty()
        val lessonSlotId = intent.getStringExtra(EXTRA_LESSON_SLOT_ID).orEmpty()

        TeachingReminderNotifier.startOverlayService(
          context = context,
          reminderId = reminderId,
          title = title,
          body = body,
          dateIso = dateIso,
          distribusiId = distribusiId,
          lessonSlotId = lessonSlotId,
          settings = settings
        )
        TeachingReminderNotifier.showReminderAlert(
          context = context,
          reminderId = reminderId,
          title = title,
          body = body,
          dateIso = dateIso,
          distribusiId = distribusiId,
          lessonSlotId = lessonSlotId,
          settings = settings
        )
        TeachingReminderNotifier.showReminderNotification(
          context = context,
          reminderId = reminderId,
          title = title,
          body = body,
          dateIso = dateIso,
          distribusiId = distribusiId,
          lessonSlotId = lessonSlotId,
          settings = settings
        )
        if (settings.repeatMode == "once") {
          reminderStore.saveSettings(
            settings.copy(
              enabled = false,
              scheduledReminderIds = emptyList(),
              updatedAt = System.currentTimeMillis()
            )
          )
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    const val ACTION_TRIGGER_TEACHING_REMINDER = "com.mim.guruapp.action.TRIGGER_TEACHING_REMINDER"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_NOTIFICATION_TITLE = "extra_notification_title"
    const val EXTRA_NOTIFICATION_BODY = "extra_notification_body"
    const val EXTRA_DATE_ISO = "extra_date_iso"
    const val EXTRA_DISTRIBUSI_ID = "extra_distribusi_id"
    const val EXTRA_LESSON_SLOT_ID = "extra_lesson_slot_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
  }
}
