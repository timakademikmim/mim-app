package com.mim.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import java.io.File

class DownloadNotificationReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      ACTION_STATUS -> handleStatus(context, intent)
      ACTION_OPEN -> handleOpen(context, intent)
    }
  }

  private fun handleStatus(context: Context, intent: Intent) {
    val stage = String(intent.getStringExtra(EXTRA_STAGE) ?: "").trim().lowercase()
    val id = intent.getIntExtra(EXTRA_ID, 3001)
    val title = String(intent.getStringExtra(EXTRA_TITLE) ?: "Unduhan").trim()
    val progress = intent.getIntExtra(EXTRA_PROGRESS, 0).coerceIn(0, 100)
    val path = String(intent.getStringExtra(EXTRA_PATH) ?: "").trim()
    val mime = String(intent.getStringExtra(EXTRA_MIME) ?: "").trim()
    val error = String(intent.getStringExtra(EXTRA_ERROR) ?: "").trim()

    ensureChannel(context)

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(if (title.isBlank()) "Unduhan" else title)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setOnlyAlertOnce(true)

    when (stage) {
      "progress" -> {
        builder
          .setContentText("Mengunduh... $progress%")
          .setOngoing(true)
          .setAutoCancel(false)
          .setProgress(100, progress, false)
      }
      "complete" -> {
        val openIntent = Intent(context, DownloadNotificationReceiver::class.java).apply {
          action = ACTION_OPEN
          putExtra(EXTRA_PATH, path)
          putExtra(EXTRA_MIME, mime)
          putExtra(EXTRA_ID, id)
        }
        val openPendingIntent = PendingIntent.getBroadcast(
          context,
          id,
          openIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder
          .setContentText("Selesai. Ketuk untuk membuka.")
          .setOngoing(false)
          .setAutoCancel(true)
          .setProgress(0, 0, false)
          .setContentIntent(openPendingIntent)
      }
      "error" -> {
        val errorText = if (error.isBlank()) "Unduhan gagal." else error
        builder
          .setContentText(errorText)
          .setOngoing(false)
          .setAutoCancel(true)
          .setProgress(0, 0, false)
      }
      else -> return
    }

    NotificationManagerCompat.from(context).notify(id, builder.build())
  }

  private fun handleOpen(context: Context, intent: Intent) {
    val path = String(intent.getStringExtra(EXTRA_PATH) ?: "").trim()
    if (path.isBlank()) return
    val mimeRaw = String(intent.getStringExtra(EXTRA_MIME) ?: "").trim()
    val mime = if (mimeRaw.isBlank()) guessMime(path) else mimeRaw
    val id = intent.getIntExtra(EXTRA_ID, 3001)

    try {
      val uri = if (path.startsWith("content://")) {
        Uri.parse(path)
      } else {
        val file = File(path)
        if (!file.exists()) {
          Log.w(TAG, "File tidak ditemukan untuk dibuka: $path")
          return
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
      }

      val openIntent = if (mime == APK_MIME) {
        Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
          data = uri
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      } else {
        Intent(Intent.ACTION_VIEW).apply {
          setDataAndType(uri, mime)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      }

      context.startActivity(openIntent)
      NotificationManagerCompat.from(context).cancel(id)
    } catch (error: Exception) {
      Log.e(TAG, "Gagal membuka file hasil unduhan.", error)
    }
  }

  private fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    val channel = NotificationChannel(
      CHANNEL_ID,
      "Unduhan MIM App",
      NotificationManager.IMPORTANCE_HIGH
    )
    manager.createNotificationChannel(channel)
  }

  private fun guessMime(path: String): String {
    val lower = path.lowercase()
    return when {
      lower.endsWith(".apk") -> APK_MIME
      lower.endsWith(".pdf") -> "application/pdf"
      lower.endsWith(".png") -> "image/png"
      lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
      else -> "*/*"
    }
  }

  companion object {
    private const val TAG = "MimDownloadNotif"
    private const val CHANNEL_ID = "download_status"
    private const val APK_MIME = "application/vnd.android.package-archive"
    const val ACTION_STATUS = "com.mim.app.DOWNLOAD_STATUS"
    const val ACTION_OPEN = "com.mim.app.DOWNLOAD_OPEN"
    const val EXTRA_STAGE = "stage"
    const val EXTRA_ID = "id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_PROGRESS = "progress"
    const val EXTRA_PATH = "path"
    const val EXTRA_MIME = "mime"
    const val EXTRA_ERROR = "error"
  }
}
