package com.mim.guruapp.alarm

import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.view.WindowManager
import com.mim.guruapp.MainActivity
import com.mim.guruapp.alarm.TeachingReminderNotifier.EXTRA_OPEN_INPUT_ABSENSI
import com.mim.guruapp.alarm.TeachingReminderNotifier.EXTRA_SOURCE
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_DATE_ISO
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_DISTRIBUSI_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_LESSON_SLOT_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_NOTIFICATION_BODY
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_NOTIFICATION_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_NOTIFICATION_TITLE
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_REMINDER_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_RINGTONE_URI
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.MimGuruTheme
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SuccessTint
import com.mim.guruapp.ui.theme.SubtleInk

class ReminderAlertActivity : ComponentActivity() {
  private var ringtone: Ringtone? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
    }
    val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
    val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID).orEmpty()
    val title = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE).orEmpty().ifBlank { "Pengingat Jam Pelajaran" }
    val body = intent.getStringExtra(EXTRA_NOTIFICATION_BODY).orEmpty()
    val dateIso = intent.getStringExtra(EXTRA_DATE_ISO).orEmpty()
    val distribusiId = intent.getStringExtra(EXTRA_DISTRIBUSI_ID).orEmpty()
    val lessonSlotId = intent.getStringExtra(EXTRA_LESSON_SLOT_ID).orEmpty()
    val ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI).orEmpty()

    stopService(Intent(this, TeachingReminderOverlayService::class.java))
    startAlarmTone(ringtoneUri)

    setContent {
      MimGuruTheme {
        ReminderAlertScreen(
          title = title,
          body = body,
          onDismiss = {
            if (notificationId != 0) {
              TeachingReminderNotifier.cancelNotification(this, notificationId)
            }
            finish()
          },
          onOpen = {
            if (notificationId != 0) {
              TeachingReminderNotifier.cancelNotification(this, notificationId)
            }
            startActivity(
              Intent(this, MainActivity::class.java).apply {
                action = TeachingReminderNotifier.ACTION_OPEN_FROM_REMINDER
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
              }
            )
            finish()
          }
        )
      }
    }
  }

  override fun onDestroy() {
    stopAlarmTone()
    super.onDestroy()
  }

  private fun startAlarmTone(ringtoneUri: String) {
    val uri = ringtoneUri.takeIf { it.isNotBlank() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
      ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    ringtone = runCatching { RingtoneManager.getRingtone(this, uri) }.getOrNull()?.apply {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        audioAttributes = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_ALARM)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        isLooping = true
      }
      play()
    }
  }

  private fun stopAlarmTone() {
    runCatching {
      ringtone?.stop()
      ringtone = null
    }
  }
}

@Composable
private fun ReminderAlertScreen(
  title: String,
  body: String,
  onDismiss: () -> Unit,
  onOpen: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xCC0F172A)),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .padding(horizontal = 24.dp)
        .fillMaxWidth()
        .background(Color.White, RoundedCornerShape(28.dp))
        .border(1.dp, CardBorder, RoundedCornerShape(28.dp))
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .background(SuccessTint.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
          .padding(18.dp)
      ) {
        Icon(
          imageVector = Icons.Outlined.Alarm,
          contentDescription = null,
          tint = SuccessTint
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center
      )
      Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        color = SubtleInk,
        textAlign = TextAlign.Center
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        ReminderActionButton(
          title = "Matikan",
          tint = Color(0xFFDC2626),
          modifier = Modifier.weight(1f),
          onClick = onDismiss
        )
        ReminderActionButton(
          title = "Buka",
          tint = SuccessTint,
          modifier = Modifier.weight(1f),
          onClick = onOpen
        )
      }
    }
  }
}

@Composable
private fun ReminderActionButton(
  title: String,
  tint: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .background(tint.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
      .border(1.dp, tint.copy(alpha = 0.24f), RoundedCornerShape(999.dp))
      .padding(vertical = 13.dp)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      color = tint,
      fontWeight = FontWeight.ExtraBold
    )
  }
}
