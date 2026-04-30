package com.mim.guruapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.mim.guruapp.ui.GuruAppRoot
import com.mim.guruapp.ui.theme.MimGuruTheme
import com.mim.guruapp.update.AppUpdateClient
import com.mim.guruapp.update.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
  private val viewModel: GuruAppViewModel by viewModels()
  private val appUpdateClient = AppUpdateClient()
  private var pendingUpdateInfo by mutableStateOf<AppUpdateInfo?>(null)
  private var dismissedUpdateVersionCode by mutableStateOf<Int?>(null)
  private var isDownloadingUpdate by mutableStateOf(false)
  private var updateStatusMessage by mutableStateOf("")

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    viewModel.handleSystemNavigationIntent(intent)

    setContent {
      MimGuruTheme {
        GuruAppRoot(
          state = viewModel.uiState,
          onTeacherNameChange = viewModel::onTeacherNameChange,
          onPasswordChange = viewModel::onPasswordChange,
          onLoginClick = viewModel::login,
          onUseDemoAccount = viewModel::useDemoAccount,
          onToggleSidebar = viewModel::toggleSidebar,
          onCloseSidebar = viewModel::closeSidebar,
          onToggleSidebarParent = viewModel::toggleSidebarParent,
          onSelectSidebarDestination = viewModel::selectSidebarDestination,
          onOpenCalendarScreen = viewModel::openCalendarScreen,
          onCloseCalendarScreen = viewModel::closeCalendarScreen,
          onOpenNotificationPopup = viewModel::openNotificationPopup,
          onCloseNotificationPopup = viewModel::closeNotificationPopup,
          onMarkNotificationAsRead = viewModel::markNotificationAsRead,
          onMarkAllNotificationsAsRead = viewModel::markAllNotificationsAsRead,
          onUpdateTeachingReminderSettings = viewModel::updateTeachingReminderSettings,
          onOpenInputAbsensiTarget = viewModel::openInputAbsensiTarget,
          onConsumePendingInputAbsensiTarget = viewModel::consumePendingInputAbsensiTarget,
          onLoadAttendanceApprovalRequest = viewModel::loadAttendanceApprovalRequest,
          onReviewAttendanceApproval = viewModel::reviewAttendanceApproval,
          onSelectCalendarDate = viewModel::selectCalendarDate,
          onToggleClaimSection = viewModel::toggleClaimSection,
          onToggleClaimSubject = viewModel::toggleClaimSubject,
          onClearClaimSelection = viewModel::clearSelectedClaimSubjects,
          onClaimSelectedSubjects = viewModel::claimSelectedSubjects,
          onLoadMapelAttendance = viewModel::loadMapelAttendance,
          onSaveMapelAttendance = viewModel::saveMapelAttendanceChanges,
          onSaveMapelAttendanceBatch = viewModel::saveMapelAttendanceBatch,
          onDeleteMapelAttendance = viewModel::deleteMapelAttendanceRows,
          onSendMapelAttendanceDelegation = viewModel::sendMapelAttendanceDelegation,
          onLoadSubstituteTeacherContext = viewModel::loadSubstituteTeacherContext,
          onLoadDelegatedAttendanceContext = viewModel::loadDelegatedAttendanceContext,
          onLoadMapelScores = viewModel::loadMapelScores,
          onSaveMapelScores = viewModel::saveMapelScoreChanges,
          onSaveMapelScoresBatch = viewModel::saveMapelScoreChangesBatch,
          onLoadMapelPatronMateri = viewModel::loadMapelPatronMateri,
          onSaveMapelPatronMateri = viewModel::saveMapelPatronMateri,
          onSaveProfile = viewModel::saveProfile,
          onSaveSantri = viewModel::saveWaliSantriProfile,
          onSaveMonthlyReport = viewModel::saveMonthlyReport,
          onSaveMonthlyExtracurricularReports = viewModel::saveMonthlyExtracurricularReports,
          onSaveUtsReportOverride = viewModel::saveUtsReportOverride,
          onLoadMutabaah = viewModel::loadMutabaahSnapshot,
          onSaveMutabaahStatus = viewModel::saveMutabaahStatus,
          onLoadLeaveRequests = viewModel::loadLeaveRequestSnapshot,
          onSubmitLeaveRequest = viewModel::submitLeaveRequest,
          onDeleteLeaveRequest = viewModel::deleteLeaveRequest,
          onRefreshClick = { viewModel.refreshFromServer(force = true) },
          onLogoutClick = viewModel::logout
        )
        pendingUpdateInfo
          ?.takeIf { dismissedUpdateVersionCode != it.versionCode }
          ?.let { updateInfo ->
            AppUpdateDialog(
              updateInfo = updateInfo,
              isDownloading = isDownloadingUpdate,
              statusMessage = updateStatusMessage,
              onDismiss = {
                if (!updateInfo.mandatory && !isDownloadingUpdate) {
                  dismissedUpdateVersionCode = updateInfo.versionCode
                }
              },
              onDownload = { downloadAndInstallUpdate(updateInfo) }
            )
          }
      }
    }

    lifecycleScope.launch {
      checkForAppUpdate()
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    viewModel.handleSystemNavigationIntent(intent)
  }

  private suspend fun checkForAppUpdate() {
    val updateInfo = appUpdateClient.checkForUpdate(BuildConfig.APP_UPDATE_MANIFEST_URL)
    if (updateInfo != null) {
      updateStatusMessage = ""
      pendingUpdateInfo = updateInfo
    }
  }

  private fun downloadAndInstallUpdate(updateInfo: AppUpdateInfo) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
      updateStatusMessage = "Izinkan install dari sumber ini, lalu tekan Download lagi."
      val settingsIntent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:$packageName")
      )
      startActivity(settingsIntent)
      return
    }
    if (isDownloadingUpdate) return
    lifecycleScope.launch {
      isDownloadingUpdate = true
      updateStatusMessage = "Mengunduh MIM APP.apk..."
      val downloadResult = withContext(Dispatchers.IO) {
        downloadApk(updateInfo.apkUrl)
      }
      isDownloadingUpdate = false
      val apkFile = downloadResult.file
      if (apkFile == null) {
        updateStatusMessage = downloadResult.message.ifBlank {
          "Gagal mengunduh APK. Periksa koneksi atau link rilis."
        }
      } else {
        updateStatusMessage = "Download selesai. Membuka installer..."
        installApk(apkFile)
      }
    }
  }

  private fun downloadApk(apkUrl: String): ApkDownloadResult {
    return runCatching {
      val outputDir = File(cacheDir, "apk_updates").apply {
        if (!exists()) mkdirs()
      }
      val outputFile = File(outputDir, "MIM APP.apk")
      if (outputFile.exists()) outputFile.delete()
      val connection = openApkConnection(apkUrl)
      try {
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
          return@runCatching ApkDownloadResult(
            message = "Gagal mengunduh APK. Server membalas kode $responseCode."
          )
        }
        connection.inputStream.use { input ->
          outputFile.outputStream().use { output ->
            input.copyTo(output)
          }
        }
        outputFile
          .takeIf { it.exists() && it.length() > 0L }
          ?.let { ApkDownloadResult(file = it) }
          ?: ApkDownloadResult(message = "File APK kosong setelah diunduh.")
      } finally {
        connection.disconnect()
      }
    }.getOrElse { error ->
      ApkDownloadResult(
        message = error.message
          ?.takeIf { it.isNotBlank() }
          ?.let { "Gagal mengunduh APK: $it" }
          ?: "Gagal mengunduh APK. Periksa koneksi atau link rilis."
      )
    }
  }

  private fun openApkConnection(
    apkUrl: String,
    redirectCount: Int = 0
  ): HttpURLConnection {
    require(redirectCount <= 8) { "Terlalu banyak redirect saat mengunduh APK." }
    val connection = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      instanceFollowRedirects = false
      connectTimeout = 15000
      readTimeout = 45000
      setRequestProperty("User-Agent", "MIM-Guru-App/${BuildConfig.VERSION_NAME} Android")
      setRequestProperty("Accept", "application/vnd.android.package-archive,application/octet-stream,*/*")
      setRequestProperty("Connection", "close")
    }
    val responseCode = connection.responseCode
    if (responseCode in 300..399) {
      val location = connection.getHeaderField("Location")
        ?.takeIf { it.isNotBlank() }
        ?: return connection
      connection.disconnect()
      val nextUrl = URL(URL(apkUrl), location).toString()
      return openApkConnection(nextUrl, redirectCount + 1)
    }
    return connection
  }

  private fun installApk(apkFile: File) {
    val apkUri = FileProvider.getUriForFile(
      this,
      "$packageName.fileprovider",
      apkFile
    )
    val installIntent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(apkUri, "application/vnd.android.package-archive")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(installIntent)
  }
}

private data class ApkDownloadResult(
  val file: File? = null,
  val message: String = ""
)

@Composable
private fun AppUpdateDialog(
  updateInfo: AppUpdateInfo,
  isDownloading: Boolean,
  statusMessage: String,
  onDismiss: () -> Unit,
  onDownload: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Versi baru tersedia",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "MIM App versi ${updateInfo.versionName} sudah tersedia dan bisa langsung diunduh.",
          style = MaterialTheme.typography.bodyMedium
        )
        if (updateInfo.releaseNotes.isNotBlank()) {
          Text(
            text = updateInfo.releaseNotes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        if (isDownloading) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 2.dp))
            Text(
              text = statusMessage.ifBlank { "Mengunduh pembaruan..." },
              style = MaterialTheme.typography.bodySmall
            )
          }
        } else if (statusMessage.isNotBlank()) {
          Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    },
    confirmButton = {
      Button(
        enabled = !isDownloading,
        onClick = onDownload
      ) {
        Text(if (isDownloading) "Mengunduh..." else "Download")
      }
    },
    dismissButton = {
      if (!updateInfo.mandatory) {
        OutlinedButton(
          enabled = !isDownloading,
          onClick = onDismiss
        ) {
          Text("Nanti")
        }
      } else {
        TextButton(enabled = false, onClick = {}) {
          Text("Wajib update")
        }
      }
    }
  )
}
