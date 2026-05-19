package com.mim.guruapp.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mim.guruapp.data.storage.LessonNotificationStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LessonNotificationReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_TRIGGER_LESSON_NOTIFICATION) return
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val settings = LessonNotificationStore(context).readSettings()
        if (!settings.enabled) return@launch

        val notificationIdSeed = intent.getStringExtra(EXTRA_NOTIFICATION_SEED).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Jam pelajaran dimulai" }
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty().ifBlank { "Saatnya membuka jadwal atau input absensi." }
        val dateIso = intent.getStringExtra(EXTRA_DATE_ISO).orEmpty()
        val distribusiId = intent.getStringExtra(EXTRA_DISTRIBUSI_ID).orEmpty()
        val lessonSlotId = intent.getStringExtra(EXTRA_LESSON_SLOT_ID).orEmpty()

        LessonNotificationNotifier.showLessonNotification(
          context = context,
          notificationIdSeed = notificationIdSeed.ifBlank { "$dateIso|$distribusiId|$lessonSlotId" },
          title = title,
          body = body,
          dateIso = dateIso,
          distribusiId = distribusiId,
          lessonSlotId = lessonSlotId
        )
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    const val ACTION_TRIGGER_LESSON_NOTIFICATION = "com.mim.guruapp.action.TRIGGER_LESSON_NOTIFICATION"
    const val EXTRA_NOTIFICATION_SEED = "extra_lesson_notification_seed"
    const val EXTRA_TITLE = "extra_lesson_notification_title"
    const val EXTRA_BODY = "extra_lesson_notification_body"
    const val EXTRA_DATE_ISO = "extra_lesson_date_iso"
    const val EXTRA_DISTRIBUSI_ID = "extra_lesson_distribusi_id"
    const val EXTRA_LESSON_SLOT_ID = "extra_lesson_slot_id"
  }
}
