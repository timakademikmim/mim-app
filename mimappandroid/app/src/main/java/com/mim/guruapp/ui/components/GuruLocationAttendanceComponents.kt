package com.mim.guruapp.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mim.guruapp.LocationAttendanceSaveOutcome
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.remote.GuruLocationAttendanceAction
import com.mim.guruapp.data.remote.GuruLocationAttendanceConfig
import com.mim.guruapp.data.remote.GuruLocationAttendanceRecord
import com.mim.guruapp.data.remote.GuruLocationAttendanceSnapshot
import com.mim.guruapp.data.remote.GuruLocationAttendanceSubmission
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.CardGradientEnd
import com.mim.guruapp.ui.theme.HighlightCard
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk
import com.mim.guruapp.ui.theme.SuccessTint
import com.mim.guruapp.ui.theme.WarmAccent
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

private data class CapturedAttendanceLocation(
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Double,
  val distanceMeters: Double,
  val mockDetected: Boolean,
  val capturedAtIso: String,
  val insideRadius: Boolean,
  val accurateEnough: Boolean
)

private sealed interface LocationCaptureResult {
  data class Success(val location: CapturedAttendanceLocation) : LocationCaptureResult
  data class Error(val message: String) : LocationCaptureResult
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuruLocationAttendanceScreen(
  teacherName: String,
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadSnapshot: suspend () -> GuruLocationAttendanceSnapshot,
  onSubmitAttendance: suspend (GuruLocationAttendanceSubmission) -> LocationAttendanceSaveOutcome,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var snapshot by remember { mutableStateOf<GuruLocationAttendanceSnapshot?>(null) }
  var capturedLocation by remember { mutableStateOf<CapturedAttendanceLocation?>(null) }
  var isInitialLoading by rememberSaveable { mutableStateOf(true) }
  var isCapturingLocation by rememberSaveable { mutableStateOf(false) }
  var isSubmitting by rememberSaveable { mutableStateOf(false) }
  var message by rememberSaveable { mutableStateOf("") }
  var selectedDateIso by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
  val today = LocalDate.now()
  val selectedDate = runCatching { LocalDate.parse(selectedDateIso) }.getOrDefault(today)
  val selectedRecord = remember(snapshot?.todayRecord, snapshot?.history, selectedDate) {
    val historyRecord = snapshot?.history.orEmpty().firstOrNull { it.dateIso.take(10) == selectedDate.toString() }
    if (selectedDate == today) snapshot?.todayRecord ?: historyRecord else historyRecord
  }
  val calendarEvents = remember(snapshot?.todayRecord, snapshot?.history, selectedDate) {
    buildLocationAttendanceCalendarEvents(
      records = buildList {
        snapshot?.history.orEmpty().forEach { add(it) }
        snapshot?.todayRecord?.let { add(it) }
      },
      selectedDate = selectedDate
    )
  }
  val currentCapturedLocation = capturedLocation
  val locationReady = currentCapturedLocation != null &&
    currentCapturedLocation.insideRadius &&
    currentCapturedLocation.accurateEnough &&
    !currentCapturedLocation.mockDetected

  fun refreshSnapshot() {
    scope.launch {
      isInitialLoading = snapshot == null
      val loaded = onLoadSnapshot()
      snapshot = loaded
      message = loaded.errorMessage
      isInitialLoading = false
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    if (permissions.values.any { it }) {
      scope.launch {
        isCapturingLocation = true
        val result = captureCurrentLocation(context, snapshot?.config)
        when (result) {
          is LocationCaptureResult.Error -> message = result.message
          is LocationCaptureResult.Success -> {
            capturedLocation = result.location
            message = buildLocationMessage(result.location)
          }
        }
        isCapturingLocation = false
      }
    } else {
      message = "Izin lokasi dibutuhkan untuk absensi guru."
    }
  }

  fun requestLocationCapture() {
    val config = snapshot?.config
    if (config == null) {
      message = snapshot?.errorMessage.takeUnless { it.isNullOrBlank() }
        ?: "Pengaturan lokasi absensi guru belum dibuat."
      return
    }
    if (!hasLocationPermission(context)) {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      )
      return
    }
    scope.launch {
      isCapturingLocation = true
      val result = captureCurrentLocation(context, config)
      when (result) {
        is LocationCaptureResult.Error -> message = result.message
        is LocationCaptureResult.Success -> {
          capturedLocation = result.location
          message = buildLocationMessage(result.location)
        }
      }
      isCapturingLocation = false
    }
  }

  fun submit(action: GuruLocationAttendanceAction) {
    val location = capturedLocation
    val config = snapshot?.config
    if (config == null) {
      message = "Pengaturan lokasi absensi guru belum dibuat."
      return
    }
    if (location == null) {
      message = "Lokasi belum terbaca. Tunggu sebentar atau tekan Cek ulang."
      return
    }
    if (location.mockDetected) {
      message = "Lokasi perangkat terdeteksi tiruan."
      return
    }
    if (!location.accurateEnough) {
      message = "Lokasi belum terbaca dengan baik. Coba cek ulang."
      return
    }
    if (!location.insideRadius) {
      message = "Anda belum berada di area absensi."
      return
    }
    scope.launch {
      isSubmitting = true
      val result = onSubmitAttendance(
        GuruLocationAttendanceSubmission(
          action = action,
          latitude = location.latitude,
          longitude = location.longitude,
          accuracyMeters = location.accuracyMeters,
          mockDetected = location.mockDetected
        )
      )
      message = result.message
      result.snapshot?.let { snapshot = it }
      if (result.success) {
        capturedLocation = null
      }
      isSubmitting = false
    }
  }

  LaunchedEffect(Unit) {
    val loaded = onLoadSnapshot()
    snapshot = loaded
    message = loaded.errorMessage
    isInitialLoading = false
    if (loaded.errorMessage.isBlank() && loaded.config != null) {
      requestLocationCapture()
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
  ) { innerPadding ->
    PullToRefreshBox(
      isRefreshing = isRefreshing || isInitialLoading,
      onRefresh = {
        onRefresh()
        refreshSnapshot()
      },
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        LocationAttendanceHeader(
          selectedDate = selectedDate,
          onJumpToToday = {
            selectedDateIso = today.toString()
          },
          onMenuClick = onMenuClick
        )

        LocationAttendanceWeekSwitcher(
          selectedDate = selectedDate,
          onPreviousWeek = { selectedDateIso = selectedDate.minusWeeks(1).toString() },
          onNextWeek = {
            val nextDate = selectedDate.plusWeeks(1)
            selectedDateIso = if (nextDate.isAfter(today)) today.toString() else nextDate.toString()
          }
        )

        WeekCalendar(
          selectedDate = selectedDate,
          events = calendarEvents,
          accentColor = locationDateAccentColor(selectedRecord, selectedDate),
          onSelectDate = { date ->
            if (!date.isAfter(today)) selectedDateIso = date.toString()
          }
        )

        LocationAttendanceSummaryCard(
          config = snapshot?.config,
          capturedLocation = capturedLocation,
          message = message,
          isCapturing = isCapturingLocation,
          onCaptureClick = ::requestLocationCapture
        )

        Text(
          text = "Timeline Absensi",
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(bottom = 124.dp)
        ) {
          item {
            LocationAttendanceTimelineRow(
              title = "Kedatangan",
              description = "Catat jam kedatangan kantor.",
              time = selectedRecord?.datangAt.orEmpty(),
              checked = selectedRecord?.datangAt?.isNotBlank() == true,
              isSaving = isSubmitting,
              isLast = false,
              enabled = selectedDate == today &&
                locationReady &&
                selectedRecord?.datangAt.isNullOrBlank(),
              onClick = { submit(GuruLocationAttendanceAction.DATANG) }
            )
          }
          item {
            LocationAttendanceTimelineRow(
              title = "Kepulangan",
              description = "Catat jam kepulangan kantor.",
              time = selectedRecord?.pulangAt.orEmpty(),
              checked = selectedRecord?.pulangAt?.isNotBlank() == true,
              isSaving = isSubmitting,
              isLast = true,
              enabled = selectedDate == today &&
                locationReady &&
                selectedRecord?.datangAt?.isNotBlank() == true &&
                selectedRecord?.pulangAt.isNullOrBlank(),
              onClick = { submit(GuruLocationAttendanceAction.PULANG) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LocationAttendanceTopBar(
  title: String,
  onMenuClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    LocationAttendanceGlassActionButton(
      icon = Icons.Outlined.Menu,
      contentDescription = "Menu",
      onClick = onMenuClick
    )
    Box(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = PrimaryBlueDark,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Box(modifier = Modifier.size(42.dp))
  }
}

@Composable
private fun LocationAttendanceGlassActionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(42.dp)
      .shadow(6.dp, RoundedCornerShape(14.dp), clip = false)
      .clip(RoundedCornerShape(14.dp))
      .background(CardBackground.copy(alpha = 0.82f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(14.dp))
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(icon, contentDescription = contentDescription, tint = PrimaryBlueDark)
  }
}

@Composable
private fun LocationAttendanceHeader(
  selectedDate: LocalDate,
  onJumpToToday: () -> Unit,
  onMenuClick: () -> Unit
) {
  val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
  val title = if (selectedDate == LocalDate.now()) {
    "Absensi Hari Ini"
  } else {
    selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("id", "ID"))
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.Top
  ) {
    LocationAttendanceCircleButton(
      icon = Icons.Outlined.Menu,
      contentDescription = "Buka sidebar",
      onClick = onMenuClick
    )
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = dateFormatter.format(selectedDate),
        style = MaterialTheme.typography.bodyMedium,
        color = SubtleInk,
        textAlign = TextAlign.Center
      )
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = HighlightCard,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
    Box(modifier = Modifier.size(42.dp))
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 12.dp),
    horizontalArrangement = Arrangement.End
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(14.dp))
        .background(CardBackground.copy(alpha = 0.88f))
        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
        .clickable(onClick = onJumpToToday)
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      Text(
        text = "Hari ini",
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
private fun LocationAttendanceWeekSwitcher(
  selectedDate: LocalDate,
  onPreviousWeek: () -> Unit,
  onNextWeek: () -> Unit
) {
  val startOfWeek = selectedDate.minusDays((selectedDate.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
  val endOfWeek = startOfWeek.plusDays(6)
  val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale("id", "ID"))
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground.copy(alpha = 0.86f), RoundedCornerShape(20.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    LocationAttendanceCircleButton(
      icon = Icons.Outlined.ChevronLeft,
      contentDescription = "Minggu sebelumnya",
      onClick = onPreviousWeek
    )
    Text(
      text = "${formatter.format(startOfWeek)} - ${formatter.format(endOfWeek)}",
      style = MaterialTheme.typography.titleSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    LocationAttendanceCircleButton(
      icon = Icons.Outlined.ChevronRight,
      contentDescription = "Minggu berikutnya",
      onClick = onNextWeek
    )
  }
}

@Composable
private fun LocationAttendanceCircleButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(42.dp)
      .shadow(6.dp, CircleShape, clip = false)
      .clip(CircleShape)
      .background(CardBackground.copy(alpha = 0.86f))
      .border(1.dp, CardBorder, CircleShape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(icon, contentDescription = contentDescription, tint = PrimaryBlueDark, modifier = Modifier.size(21.dp))
  }
}

@Composable
private fun LocationAttendanceSummaryCard(
  config: GuruLocationAttendanceConfig?,
  capturedLocation: CapturedAttendanceLocation?,
  message: String,
  isCapturing: Boolean,
  onCaptureClick: () -> Unit
) {
  val locationReady = capturedLocation != null &&
    capturedLocation.insideRadius &&
    capturedLocation.accurateEnough &&
    !capturedLocation.mockDetected
  val statusText = when {
    config == null -> "Belum diatur"
    capturedLocation == null && isCapturing -> "Membaca lokasi"
    capturedLocation == null -> "Menunggu lokasi"
    capturedLocation.mockDetected -> "Lokasi tiruan"
    locationReady -> "Dalam area"
    !capturedLocation.insideRadius -> "Belum dalam area"
    else -> "GPS belum stabil"
  }
  val accent = when {
    locationReady -> HighlightCard
    capturedLocation?.mockDetected == true -> WarmAccent
    else -> WarmAccent
  }
  val percent = if (locationReady) 100 else 0
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x120F172A), spotColor = Color(0x120F172A))
      .clip(RoundedCornerShape(22.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(22.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Lokasi Absensi",
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = config?.name?.ifBlank { "Dicek otomatis saat halaman dibuka" }
            ?: "Pengaturan lokasi belum tersedia",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
      Text(
        text = statusText,
        style = MaterialTheme.typography.titleMedium,
        color = accent,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(9.dp)
        .clip(RoundedCornerShape(999.dp))
        .background(SoftPanel)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
          .height(9.dp)
          .background(accent, RoundedCornerShape(999.dp))
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = message.ifBlank { if (isCapturing) "Sedang membaca lokasi perangkat..." else "Lokasi akan dibaca otomatis." },
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )
      OutlinedButton(
        onClick = onCaptureClick,
        enabled = !isCapturing,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
      ) {
        if (isCapturing) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
          Text("Cek ulang")
        }
      }
    }
  }
}

@Composable
private fun LocationAttendanceTimelineRow(
  title: String,
  description: String,
  time: String,
  checked: Boolean,
  isSaving: Boolean,
  isLast: Boolean,
  enabled: Boolean,
  onClick: () -> Unit
) {
  val accent = if (checked) HighlightCard else WarmAccent
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Column(
      modifier = Modifier.width(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(30.dp)
          .shadow(8.dp, CircleShape, ambientColor = Color(0x180F172A), spotColor = Color(0x180F172A))
          .clip(CircleShape)
          .background(if (checked) HighlightCard else CardBackground)
          .border(
            2.dp,
            if (checked) HighlightCard.copy(alpha = 0.28f) else WarmAccent.copy(alpha = 0.62f),
            CircleShape
          )
          .clickable(enabled = enabled && !isSaving) { onClick() },
        contentAlignment = Alignment.Center
      ) {
        when {
          isSaving -> CircularProgressIndicator(
            color = if (checked) Color.White else HighlightCard,
            strokeWidth = 2.dp,
            modifier = Modifier.size(14.dp)
          )

          checked -> Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = "Tercatat",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )

          else -> Box(
            modifier = Modifier
              .size(8.dp)
              .background(WarmAccent, CircleShape)
          )
        }
      }
      if (!isLast) {
        Spacer(
          modifier = Modifier
            .padding(top = 4.dp)
            .width(2.dp)
            .height(96.dp)
            .background(accent.copy(alpha = 0.24f), RoundedCornerShape(999.dp))
        )
      }
    }

    Box(
      modifier = Modifier
        .weight(1f)
        .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
        .clip(RoundedCornerShape(24.dp))
        .border(1.dp, accent.copy(alpha = if (checked) 0.18f else 0.26f), RoundedCornerShape(24.dp))
        .clickable(enabled = enabled && !isSaving) { onClick() }
    ) {
      LocationAttendanceGlassBackground(
        tint = if (checked) SuccessTint else Color(0xFFFFF1D8),
        accent = accent
      )
      Box(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 1.dp)
          .width(5.dp)
          .height(74.dp)
          .clip(RoundedCornerShape(999.dp))
          .background(accent.copy(alpha = 0.64f))
      )
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 18.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.Top
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(CardBackground.copy(alpha = 0.62f))
              .border(1.dp, CardBorder.copy(alpha = 0.58f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.TaskAlt,
              contentDescription = null,
              tint = accent,
              modifier = Modifier.size(20.dp)
            )
          }
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleSmall,
              color = PrimaryBlueDark,
              fontWeight = FontWeight.ExtraBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = description,
              style = MaterialTheme.typography.bodySmall,
              color = SubtleInk,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .background(CardBackground.copy(alpha = 0.68f))
              .border(1.dp, CardBorder.copy(alpha = 0.58f), RoundedCornerShape(999.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = when {
                isSaving -> "Menyimpan..."
                checked -> "Tercatat ${formatTimeOnly(time)}"
                enabled -> "Ketuk untuk mencatat"
                else -> "Belum tercatat"
              },
              style = MaterialTheme.typography.labelMedium,
              color = when {
                isSaving -> WarmAccent
                checked -> HighlightCard
                else -> SubtleInk
              },
              fontWeight = FontWeight.SemiBold
            )
          }
          Text(
            text = if (checked) formatTimeOnly(time) else "Belum",
            style = MaterialTheme.typography.labelMedium,
            color = if (checked) HighlightCard else WarmAccent,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}

@Composable
private fun BoxScope.LocationAttendanceGlassBackground(
  tint: Color,
  accent: Color
) {
  Box(
    modifier = Modifier
      .matchParentSize()
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(
            tint.copy(alpha = 0.82f),
            CardGradientEnd.copy(alpha = 0.9f)
          )
        )
      )
  )
  Box(
    modifier = Modifier
      .align(Alignment.TopEnd)
      .size(96.dp)
      .clip(CircleShape)
      .background(accent.copy(alpha = 0.10f))
  )
}

@Composable
private fun LocationAttendanceTodayCard(
  teacherName: String,
  config: GuruLocationAttendanceConfig?,
  todayRecord: GuruLocationAttendanceRecord?,
  capturedLocation: CapturedAttendanceLocation?,
  message: String,
  isCapturing: Boolean,
  isSubmitting: Boolean,
  onCaptureClick: () -> Unit,
  onDatangClick: () -> Unit,
  onPulangClick: () -> Unit
) {
  val locationReady = capturedLocation != null &&
    capturedLocation.insideRadius &&
    capturedLocation.accurateEnough &&
    !capturedLocation.mockDetected
  val statusLabel = when {
    config == null -> "Menunggu pengaturan"
    capturedLocation == null && isCapturing -> "Membaca lokasi"
    capturedLocation == null -> "Menunggu lokasi"
    capturedLocation.mockDetected -> "Lokasi tiruan"
    locationReady -> "Dalam area"
    !capturedLocation.insideRadius -> "Belum dalam area"
    else -> "Menunggu GPS stabil"
  }
  val statusColor = when {
    locationReady -> SuccessTint
    capturedLocation?.mockDetected == true -> WarmAccent
    else -> WarmAccent
  }
  AttendanceCard {
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconBubble(background = HighlightCard.copy(alpha = 0.14f), tint = HighlightCard) {
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = PrimaryBlueDark,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = teacherName.ifBlank { "Guru" },
          style = MaterialTheme.typography.bodyMedium,
          color = SubtleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        text = statusLabel,
        style = MaterialTheme.typography.labelMedium,
        color = statusColor,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .background(statusColor.copy(alpha = 0.12f))
          .padding(horizontal = 10.dp, vertical = 6.dp)
      )
    }

    Spacer(Modifier.height(14.dp))

    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      AttendanceTimePill(
        label = "Datang",
        value = todayRecord?.datangAt?.let(::formatTimeOnly).orEmpty().ifBlank { "-" },
        modifier = Modifier.weight(1f)
      )
      AttendanceTimePill(
        label = "Pulang",
        value = todayRecord?.pulangAt?.let(::formatTimeOnly).orEmpty().ifBlank { "-" },
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(Modifier.height(12.dp))

    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Button(
        onClick = onDatangClick,
        enabled = locationReady && !isSubmitting && todayRecord?.datangAt.isNullOrBlank(),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f)
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
          Text("Datang")
        }
      }
      OutlinedButton(
        onClick = onPulangClick,
        enabled = locationReady &&
          !isSubmitting &&
          todayRecord?.datangAt?.isNotBlank() == true &&
          todayRecord.pulangAt.isBlank(),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f)
      ) {
        Text("Pulang")
      }
    }

    Spacer(Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = config?.name?.ifBlank { "Lokasi absensi" } ?: "Lokasi absensi belum diatur",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )
      OutlinedButton(
        onClick = onCaptureClick,
        enabled = !isCapturing,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
      ) {
        if (isCapturing) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
          Text("Cek ulang")
        }
      }
    }

    if (message.isNotBlank()) {
      Spacer(Modifier.height(10.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = PrimaryBlueDark,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(SoftPanel)
          .border(1.dp, CardBorder.copy(alpha = 0.68f), RoundedCornerShape(12.dp))
          .padding(horizontal = 12.dp, vertical = 10.dp)
      )
    }
  }
}

@Composable
private fun TeacherAttendanceStatusCard(
  teacherName: String,
  config: GuruLocationAttendanceConfig?,
  todayRecord: GuruLocationAttendanceRecord?
) {
  AttendanceCard {
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconBubble(background = HighlightCard.copy(alpha = 0.14f), tint = HighlightCard) {
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = PrimaryBlueDark
        )
        Text(
          text = teacherName.ifBlank { "Guru" },
          style = MaterialTheme.typography.bodyMedium,
          color = SubtleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
    Spacer(Modifier.height(14.dp))
    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      AttendanceTimePill(
        label = "Datang",
        value = todayRecord?.datangAt?.let(::formatTimeOnly).orEmpty().ifBlank { "-" },
        modifier = Modifier.weight(1f)
      )
      AttendanceTimePill(
        label = "Pulang",
        value = todayRecord?.pulangAt?.let(::formatTimeOnly).orEmpty().ifBlank { "-" },
        modifier = Modifier.weight(1f)
      )
    }
    Spacer(Modifier.height(12.dp))
    Text(
      text = if (config == null) {
        "Lokasi absensi belum diatur admin."
      } else {
        config.name
      },
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
    if (config?.address?.isNotBlank() == true) {
      Spacer(Modifier.height(6.dp))
      Text(
        text = config.address,
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
  }
}

@Composable
private fun AttendanceMessageCard(message: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(SoftPanel, RoundedCornerShape(14.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
      .padding(14.dp)
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = PrimaryBlueDark
    )
  }
}

@Composable
private fun LocationCaptureCard(
  config: GuruLocationAttendanceConfig?,
  capturedLocation: CapturedAttendanceLocation?,
  isCapturing: Boolean,
  onCaptureClick: () -> Unit
) {
  AttendanceCard {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      IconBubble(background = WarmAccent.copy(alpha = 0.16f), tint = WarmAccent) {
        Icon(Icons.Outlined.Place, contentDescription = null)
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Lokasi Saat Ini",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = PrimaryBlueDark
      )
      Text(
          text = if (config == null) "Menunggu pengaturan lokasi" else "Dicek otomatis saat halaman dibuka",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      OutlinedButton(
        onClick = onCaptureClick,
        enabled = !isCapturing,
        shape = RoundedCornerShape(12.dp)
      ) {
        if (isCapturing) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
          Text("Cek ulang")
        }
      }
    }

    Spacer(Modifier.height(14.dp))

    if (capturedLocation == null) {
      Text(
        text = if (isCapturing) {
          "Sedang membaca lokasi perangkat..."
        } else {
          "Lokasi akan dibaca otomatis saat halaman dibuka."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = SubtleInk
      )
    } else {
      val statusColor = when {
        capturedLocation.mockDetected -> WarmAccent
        capturedLocation.insideRadius && capturedLocation.accurateEnough -> SuccessTint
        else -> WarmAccent
      }
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AttendanceDetailRow(
          "Status area",
          when {
            capturedLocation.mockDetected -> "Lokasi tiruan"
            capturedLocation.insideRadius && capturedLocation.accurateEnough -> "Dalam area"
            !capturedLocation.insideRadius -> "Belum dalam area"
            else -> "Menunggu GPS stabil"
          },
          statusColor
        )
      }
    }
  }
}

@Composable
private fun LocationAttendanceActionCard(
  todayRecord: GuruLocationAttendanceRecord?,
  capturedLocation: CapturedAttendanceLocation?,
  isSubmitting: Boolean,
  onDatangClick: () -> Unit,
  onPulangClick: () -> Unit
) {
  val locationReady = capturedLocation != null &&
    capturedLocation.insideRadius &&
    capturedLocation.accurateEnough &&
    !capturedLocation.mockDetected
  AttendanceCard {
    Text(
      text = "Tindakan",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = PrimaryBlueDark
    )
    Spacer(Modifier.height(12.dp))
    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Button(
        onClick = onDatangClick,
        enabled = locationReady && !isSubmitting && todayRecord?.datangAt.isNullOrBlank(),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f)
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
          Text("Datang")
        }
      }
      OutlinedButton(
        onClick = onPulangClick,
        enabled = locationReady &&
          !isSubmitting &&
          todayRecord?.datangAt?.isNotBlank() == true &&
          todayRecord.pulangAt.isBlank(),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f)
      ) {
        Text("Pulang")
      }
    }
  }
}

@Composable
private fun LocationAttendanceHistoryCard(record: GuruLocationAttendanceRecord) {
  AttendanceCard {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      IconBubble(background = SuccessTint.copy(alpha = 0.14f), tint = SuccessTint) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = formatDate(record.dateIso),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = PrimaryBlueDark
        )
        Text(
          text = "Datang ${formatTimeOnly(record.datangAt)} - Pulang ${formatTimeOnly(record.pulangAt)}",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        text = if (record.status == "hadir") "Lengkap" else "Belum lengkap",
        style = MaterialTheme.typography.labelMedium,
        color = if (record.status == "hadir") SuccessTint else WarmAccent,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun EmptyHistoryCard() {
  AttendanceCard {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      IconBubble(background = SoftPanel, tint = SubtleInk) {
        Icon(Icons.Outlined.HourglassTop, contentDescription = null)
      }
      Text(
        text = "Belum ada riwayat absensi guru.",
        style = MaterialTheme.typography.bodyMedium,
        color = SubtleInk
      )
    }
  }
}

@Composable
private fun AttendanceCard(content: @Composable ColumnScope.() -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(4.dp, RoundedCornerShape(18.dp), clip = false)
      .background(CardBackground, RoundedCornerShape(18.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
      .padding(16.dp),
    content = content
  )
}

@Composable
private fun AttendanceTimePill(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .background(SoftPanel, RoundedCornerShape(14.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = SubtleInk
    )
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = PrimaryBlueDark
    )
  }
}

@Composable
private fun AttendanceDetailRow(
  label: String,
  value: String,
  valueColor: Color
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = valueColor,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
private fun IconBubble(
  background: Color,
  tint: Color,
  content: @Composable () -> Unit
) {
  Box(
    modifier = Modifier
      .size(42.dp)
      .clip(CircleShape)
      .background(background),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier.size(22.dp),
      contentAlignment = Alignment.Center
    ) {
      androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides tint,
        content = content
      )
    }
  }
}

private fun buildLocationMessage(location: CapturedAttendanceLocation): String {
  return when {
    location.mockDetected -> "Lokasi perangkat terdeteksi tiruan."
    !location.insideRadius -> "Anda belum berada di area absensi."
    !location.accurateEnough -> "Lokasi belum terbaca dengan baik. Coba cek ulang."
    else -> "Lokasi valid. Silakan tekan Datang atau Pulang."
  }
}

private fun buildLocationAttendanceCalendarEvents(
  records: List<GuruLocationAttendanceRecord>,
  selectedDate: LocalDate
): List<CalendarEvent> {
  val today = LocalDate.now()
  val startOfWeek = selectedDate.minusDays((selectedDate.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
  val recordByDate = records
    .filter { it.dateIso.isNotBlank() }
    .associateBy { it.dateIso.take(10) }
  return List(7) { startOfWeek.plusDays(it.toLong()) }
    .mapNotNull { date ->
      if (date.dayOfWeek == DayOfWeek.SUNDAY || date.isAfter(today)) return@mapNotNull null
      val record = recordByDate[date.toString()]
      val color = locationDateColor(record, date) ?: return@mapNotNull null
      CalendarEvent(
        id = "guru-location-${date}",
        startDateIso = date.toString(),
        endDateIso = date.toString(),
        title = locationDateTitle(record, date),
        description = "",
        timeLabel = "",
        colorHex = color
      )
    }
}

@Composable
private fun locationDateAccentColor(
  record: GuruLocationAttendanceRecord?,
  date: LocalDate
): Color {
  return when (locationDateColor(record, date)) {
    LocationColorBlue -> HighlightCard
    LocationColorYellow -> WarmAccent
    LocationColorRed -> Color(0xFFEF4444)
    else -> HighlightCard
  }
}

private fun locationDateColor(record: GuruLocationAttendanceRecord?, date: LocalDate): String? {
  val today = LocalDate.now()
  return when {
    record != null && record.datangAt.isBlank() -> LocationColorRed
    record != null && record.pulangAt.isBlank() -> LocationColorYellow
    record != null && isLateArrival(record.datangAt) -> LocationColorYellow
    record != null -> LocationColorBlue
    date.isBefore(today) -> LocationColorRed
    date == today && !LocalTime.now().isBefore(LocationAttendanceAbsentCutoff) -> LocationColorRed
    else -> null
  }
}

private fun locationDateTitle(record: GuruLocationAttendanceRecord?, date: LocalDate): String {
  return when (locationDateColor(record, date)) {
    LocationColorBlue -> "Hadir"
    LocationColorYellow -> if (record?.pulangAt.isNullOrBlank()) "Belum lengkap" else "Terlambat"
    LocationColorRed -> "Tidak hadir"
    else -> ""
  }
}

private fun isLateArrival(value: String): Boolean {
  val time = parseRecordLocalTime(value) ?: return false
  return time.isAfter(LocationAttendanceWorkStart)
}

private fun parseRecordLocalTime(value: String): LocalTime? {
  if (value.isBlank()) return null
  return runCatching {
    Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalTime()
  }.getOrElse {
    runCatching { java.time.LocalDateTime.parse(value.take(19).replace(' ', 'T')).toLocalTime() }.getOrNull()
  }
}

private fun hasLocationPermission(context: Context): Boolean {
  return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
private suspend fun captureCurrentLocation(
  context: Context,
  config: GuruLocationAttendanceConfig?
): LocationCaptureResult {
  if (config == null) {
    return LocationCaptureResult.Error("Pengaturan lokasi absensi guru belum dibuat.")
  }
  if (!hasLocationPermission(context)) {
    return LocationCaptureResult.Error("Izin lokasi belum diberikan.")
  }
  val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    ?: return LocationCaptureResult.Error("Layanan lokasi tidak tersedia.")
  val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    .filter { provider -> runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false) }
  if (providers.isEmpty()) {
    return LocationCaptureResult.Error("GPS/lokasi perangkat belum aktif.")
  }

  val requestedLocation = withTimeoutOrNull(15_000) {
    suspendCancellableCoroutine<Location?> { continuation ->
      val resumed = AtomicBoolean(false)
      val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
          if (resumed.compareAndSet(false, true)) {
            locationManager.removeUpdates(this)
            continuation.resume(location)
          }
        }

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) = Unit

        @Deprecated("Deprecated in Android framework")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
      }
      continuation.invokeOnCancellation {
        locationManager.removeUpdates(listener)
      }
      providers.forEach { provider ->
        runCatching {
          locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        }
      }
    }
  } ?: bestLastKnownLocation(locationManager, providers)

  val location = requestedLocation
    ?: return LocationCaptureResult.Error("Lokasi belum terbaca. Coba lagi di area terbuka.")
  val distanceMeters = distanceBetween(
    startLatitude = config.latitude,
    startLongitude = config.longitude,
    endLatitude = location.latitude,
    endLongitude = location.longitude
  )
  val accuracyMeters = if (location.hasAccuracy()) location.accuracy.toDouble() else Double.POSITIVE_INFINITY
  return LocationCaptureResult.Success(
    CapturedAttendanceLocation(
      latitude = location.latitude,
      longitude = location.longitude,
      accuracyMeters = accuracyMeters,
      distanceMeters = distanceMeters,
      mockDetected = isMockLocation(location),
      capturedAtIso = Instant.now().toString(),
      insideRadius = distanceMeters <= config.radiusMeters,
      accurateEnough = accuracyMeters <= config.maxAccuracyMeters
    )
  )
}

@SuppressLint("MissingPermission")
private fun bestLastKnownLocation(
  locationManager: LocationManager,
  providers: List<String>
): Location? {
  return providers
    .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
    .maxByOrNull { it.time }
}

private fun isMockLocation(location: Location): Boolean {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    location.isMock
  } else {
    @Suppress("DEPRECATION")
    location.isFromMockProvider
  }
}

private fun distanceBetween(
  startLatitude: Double,
  startLongitude: Double,
  endLatitude: Double,
  endLongitude: Double
): Double {
  val result = FloatArray(1)
  Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, result)
  return result.firstOrNull()?.toDouble() ?: Double.POSITIVE_INFINITY
}

private fun formatDate(value: String): String {
  return runCatching {
    LocalDate.parse(value.take(10)).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID")))
  }.getOrDefault(value.ifBlank { "-" })
}

private fun formatTimeOnly(value: String): String {
  if (value.isBlank()) return "-"
  return runCatching {
    Instant.parse(value)
      .atZone(ZoneId.systemDefault())
      .format(DateTimeFormatter.ofPattern("HH:mm", Locale("id", "ID")))
  }.getOrDefault("-")
}

private val LocationAttendanceWorkStart: LocalTime = LocalTime.of(8, 0)
private val LocationAttendanceAbsentCutoff: LocalTime = LocalTime.of(15, 0)
private const val LocationColorBlue = "#38BDF8"
private const val LocationColorYellow = "#F59E0B"
private const val LocationColorRed = "#EF4444"
