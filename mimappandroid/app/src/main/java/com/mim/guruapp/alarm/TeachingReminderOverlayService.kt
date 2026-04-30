package com.mim.guruapp.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.mim.guruapp.MainActivity
import com.mim.guruapp.alarm.TeachingReminderNotifier.EXTRA_OPEN_INPUT_ABSENSI
import com.mim.guruapp.alarm.TeachingReminderNotifier.EXTRA_SOURCE
import com.mim.guruapp.alarm.TeachingReminderNotifier.toNotificationId
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_DATE_ISO
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_DISTRIBUSI_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_LESSON_SLOT_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_NOTIFICATION_BODY
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_NOTIFICATION_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_NOTIFICATION_TITLE
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_REMINDER_ID
import com.mim.guruapp.alarm.TeachingReminderReceiver.Companion.EXTRA_RINGTONE_URI

class TeachingReminderOverlayService : Service() {
  private var overlayView: View? = null
  private var ringtone: Ringtone? = null
  private val windowManager by lazy { getSystemService(WindowManager::class.java) }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP_OVERLAY -> {
        stopAlarm()
        return START_NOT_STICKY
      }

      ACTION_OPEN_FROM_OVERLAY -> {
        openInputAbsensi(intent)
        return START_NOT_STICKY
      }
    }

    val reminderId = intent?.getStringExtra(EXTRA_REMINDER_ID).orEmpty()
    val notificationId = reminderId.toNotificationId()
    val title = intent?.getStringExtra(EXTRA_NOTIFICATION_TITLE).orEmpty().ifBlank { "Pengingat Jam Pelajaran" }
    val body = intent?.getStringExtra(EXTRA_NOTIFICATION_BODY).orEmpty().ifBlank { "Sudah waktunya membuka input absensi." }

    startForeground(
      notificationId,
      buildForegroundNotification(
        notificationId = notificationId,
        title = title,
        body = body,
        sourceIntent = intent
      )
    )

    if (TeachingReminderNotifier.canDrawOverlays(this)) {
      showOverlay(
        title = title,
        body = body,
        sourceIntent = intent
      )
    }
    startAlarmTone(intent?.getStringExtra(EXTRA_RINGTONE_URI).orEmpty())
    return START_NOT_STICKY
  }

  override fun onDestroy() {
    stopAlarmTone()
    removeOverlay()
    super.onDestroy()
  }

  private fun showOverlay(
    title: String,
    body: String,
    sourceIntent: Intent?
  ) {
    removeOverlay()
    val root = FrameLayout(this).apply {
      setBackgroundColor(0xD90F172A.toInt())
      isClickable = true
      isFocusable = true
    }

    val card = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER_HORIZONTAL
      background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 28.dp().toFloat()
        setColor(Color.WHITE)
      }
      elevation = 18.dp().toFloat()
      setPadding(24.dp(), 24.dp(), 24.dp(), 24.dp())
    }

    val icon = TextView(this).apply {
      text = "\u23f0"
      textSize = 38f
      gravity = Gravity.CENTER
    }
    val titleView = TextView(this).apply {
      text = title
      textSize = 24f
      setTypeface(typeface, Typeface.BOLD)
      setTextColor(0xFF0F172A.toInt())
      gravity = Gravity.CENTER
    }
    val bodyView = TextView(this).apply {
      text = body
      textSize = 16f
      setTextColor(0xFF475569.toInt())
      gravity = Gravity.CENTER
      setLineSpacing(2.dp().toFloat(), 1f)
    }
    val buttonRow = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER
    }
    val dismissButton = overlayButton("Matikan", 0xFFDC2626.toInt()) {
      stopAlarm()
    }
    val openButton = overlayButton("Buka", 0xFF16A34A.toInt()) {
      openInputAbsensi(sourceIntent)
    }

    buttonRow.addView(
      dismissButton,
      LinearLayout.LayoutParams(0, 52.dp(), 1f).apply { marginEnd = 6.dp() }
    )
    buttonRow.addView(
      openButton,
      LinearLayout.LayoutParams(0, 52.dp(), 1f).apply { marginStart = 6.dp() }
    )

    card.addView(icon, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    card.addView(titleView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 14.dp() })
    card.addView(bodyView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 10.dp() })
    card.addView(buttonRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 20.dp() })

    root.addView(
      card,
      FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
        marginStart = 24.dp()
        marginEnd = 24.dp()
      }
    )

    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      @Suppress("DEPRECATION")
      WindowManager.LayoutParams.TYPE_PHONE
    }
    val params = WindowManager.LayoutParams(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.MATCH_PARENT,
      type,
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
      android.graphics.PixelFormat.TRANSLUCENT
    ).apply {
      gravity = Gravity.CENTER
    }

    runCatching {
      windowManager.addView(root, params)
      overlayView = root
    }
  }

  private fun overlayButton(
    label: String,
    color: Int,
    onClick: () -> Unit
  ): TextView {
    return TextView(this).apply {
      text = label
      textSize = 15f
      setTypeface(typeface, Typeface.BOLD)
      setTextColor(color)
      gravity = Gravity.CENTER
      background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 999.dp().toFloat()
        setColor(color.withAlpha(0.12f))
        setStroke(1.dp(), color.withAlpha(0.24f))
      }
      setOnClickListener { onClick() }
    }
  }

  private fun buildForegroundNotification(
    notificationId: Int,
    title: String,
    body: String,
    sourceIntent: Intent?
  ): android.app.Notification {
    ensureServiceChannel()
    val stopIntent = PendingIntent.getService(
      this,
      notificationId + 10,
      Intent(this, TeachingReminderOverlayService::class.java).apply {
        action = ACTION_STOP_OVERLAY
      },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val openIntent = PendingIntent.getService(
      this,
      notificationId + 11,
      Intent(this, TeachingReminderOverlayService::class.java).apply {
        action = ACTION_OPEN_FROM_OVERLAY
        copyReminderExtrasFrom(sourceIntent)
      },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setContentIntent(openIntent)
      .addAction(0, "Matikan", stopIntent)
      .addAction(0, "Buka", openIntent)
      .build()
  }

  private fun ensureServiceChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = getSystemService(NotificationManager::class.java)
    if (manager.getNotificationChannel(SERVICE_CHANNEL_ID) != null) return
    manager.createNotificationChannel(
      NotificationChannel(
        SERVICE_CHANNEL_ID,
        "Layar Alarm Jadwal",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Menjaga layar alarm jadwal tetap aktif"
        setSound(null, null)
        enableVibration(false)
      }
    )
  }

  private fun startAlarmTone(ringtoneUri: String) {
    stopAlarmTone()
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

  private fun removeOverlay() {
    val view = overlayView ?: return
    runCatching { windowManager.removeView(view) }
    overlayView = null
  }

  private fun stopAlarm() {
    stopAlarmTone()
    removeOverlay()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun openInputAbsensi(sourceIntent: Intent?) {
    stopAlarm()
    startActivity(
      Intent(this, MainActivity::class.java).apply {
        action = TeachingReminderNotifier.ACTION_OPEN_FROM_REMINDER
        putExtra(EXTRA_OPEN_INPUT_ABSENSI, true)
        putExtra(EXTRA_SOURCE, "teaching_reminder")
        copyReminderExtrasFrom(sourceIntent)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      }
    )
  }

  private fun Intent.copyReminderExtrasFrom(source: Intent?) {
    putExtra(EXTRA_REMINDER_ID, source?.getStringExtra(EXTRA_REMINDER_ID).orEmpty())
    putExtra(EXTRA_NOTIFICATION_ID, source?.getIntExtra(EXTRA_NOTIFICATION_ID, 0) ?: 0)
    putExtra(EXTRA_NOTIFICATION_TITLE, source?.getStringExtra(EXTRA_NOTIFICATION_TITLE).orEmpty())
    putExtra(EXTRA_NOTIFICATION_BODY, source?.getStringExtra(EXTRA_NOTIFICATION_BODY).orEmpty())
    putExtra(EXTRA_DATE_ISO, source?.getStringExtra(EXTRA_DATE_ISO).orEmpty())
    putExtra(EXTRA_DISTRIBUSI_ID, source?.getStringExtra(EXTRA_DISTRIBUSI_ID).orEmpty())
    putExtra(EXTRA_LESSON_SLOT_ID, source?.getStringExtra(EXTRA_LESSON_SLOT_ID).orEmpty())
    putExtra(EXTRA_RINGTONE_URI, source?.getStringExtra(EXTRA_RINGTONE_URI).orEmpty())
  }

  private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

  private fun Int.withAlpha(alpha: Float): Int {
    return Color.argb((alpha * 255).toInt(), Color.red(this), Color.green(this), Color.blue(this))
  }

  companion object {
    private const val SERVICE_CHANNEL_ID = "teaching_reminder_overlay"
    private const val ACTION_START_OVERLAY = "com.mim.guruapp.action.START_REMINDER_OVERLAY"
    private const val ACTION_STOP_OVERLAY = "com.mim.guruapp.action.STOP_REMINDER_OVERLAY"
    private const val ACTION_OPEN_FROM_OVERLAY = "com.mim.guruapp.action.OPEN_FROM_REMINDER_OVERLAY"

    fun buildStartIntent(
      context: Context,
      reminderId: String,
      title: String,
      body: String,
      dateIso: String,
      distribusiId: String,
      lessonSlotId: String,
      ringtoneUri: String
    ): Intent {
      val notificationId = reminderId.toNotificationId()
      return Intent(context, TeachingReminderOverlayService::class.java).apply {
        action = ACTION_START_OVERLAY
        putExtra(EXTRA_REMINDER_ID, reminderId)
        putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        putExtra(EXTRA_NOTIFICATION_TITLE, title)
        putExtra(EXTRA_NOTIFICATION_BODY, body)
        putExtra(EXTRA_DATE_ISO, dateIso)
        putExtra(EXTRA_DISTRIBUSI_ID, distribusiId)
        putExtra(EXTRA_LESSON_SLOT_ID, lessonSlotId)
        putExtra(EXTRA_RINGTONE_URI, ringtoneUri)
      }
    }
  }
}
