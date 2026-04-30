package com.mim.guruapp.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mim.guruapp.data.storage.GuruCacheStore
import com.mim.guruapp.data.storage.TeachingReminderStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TeachingReminderRescheduleReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val reminderStore = TeachingReminderStore(context)
        val settings = reminderStore.readSettings()
        if (!settings.enabled) return@launch

        val dashboard = GuruCacheStore(context).readDashboard() ?: return@launch
        TeachingReminderScheduler(context, reminderStore).syncReminders(
          settings = settings,
          events = dashboard.teachingScheduleEvents
        )
      } finally {
        pendingResult.finish()
      }
    }
  }
}

