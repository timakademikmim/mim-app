package com.mim.guruapp.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mim.guruapp.alarm.TeachingReminderNotifier.ACTION_DISMISS_REMINDER
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_NOTIFICATION_ID

class TeachingReminderActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      ACTION_DISMISS_REMINDER -> {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        if (notificationId != 0) {
          TeachingReminderNotifier.cancelNotification(context, notificationId)
        }
        context.stopService(Intent(context, TeachingReminderOverlayService::class.java))
      }
    }
  }
}
