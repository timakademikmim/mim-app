package com.mim.guruapp.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.TeachingReminderSettings
import com.mim.guruapp.alarm.TeachingReminderNotifier
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.CardGradientEnd
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SubtleInk
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class CalendarViewMode {
  Week,
  Month
}

private val WeekCalendarDayChipWidth = 74.dp
private const val ReminderTargetAll = "all"
private const val ReminderTargetSpecific = "specific"
private const val ReminderRepeatOnce = "once"
private const val ReminderRepeatEveryLesson = "every_lesson"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
  selectedDate: LocalDate,
  events: List<CalendarEvent>,
  isRefreshing: Boolean,
  onSelectDate: (LocalDate) -> Unit,
  onJumpToToday: () -> Unit,
  onRefresh: () -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var viewMode by rememberSaveable { mutableStateOf(CalendarViewMode.Week) }
  var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }

  BackHandler(onBack = onBackClick)

  LaunchedEffect(selectedDate) {
    currentMonth = YearMonth.from(selectedDate)
  }

  val selectedEvents = remember(events, selectedDate) {
    events
      .filter { eventContainsDate(it, selectedDate) }
      .sortedWith(
        compareBy<CalendarEvent> { parseEventStart(it) }
          .thenBy { it.timeLabel }
          .thenBy { it.title }
      )
  }
  val accentColor = remember(selectedEvents) {
    selectedEvents.firstOrNull()?.let { parseCalendarColor(it.colorHex) } ?: Color(0xFF3B82F6)
  }

  PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    modifier = modifier.fillMaxSize()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      CalendarHeader(
        selectedDate = selectedDate,
        accentColor = accentColor,
        onJumpToToday = onJumpToToday,
        onBackClick = onBackClick
      )

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(bottom = 110.dp)
      ) {
        item {
          CalendarViewToggle(
            viewMode = viewMode,
            accentColor = accentColor,
            onViewModeChange = { viewMode = it }
          )
        }

        item {
          AnimatedContent(
            targetState = viewMode,
            transitionSpec = {
              fadeIn(animationSpec = tween(220)) + slideInVertically(animationSpec = tween(220)) { it / 8 } togetherWith
                fadeOut(animationSpec = tween(180)) + slideOutVertically(animationSpec = tween(180)) { -it / 8 }
            },
            label = "calendar-view-switch"
          ) { mode ->
            when (mode) {
              CalendarViewMode.Week -> {
                WeekCalendar(
                  selectedDate = selectedDate,
                  events = events,
                  accentColor = accentColor,
                  onSelectDate = onSelectDate
                )
              }

              CalendarViewMode.Month -> {
                MonthCalendar(
                  currentMonth = currentMonth,
                  selectedDate = selectedDate,
                  events = events,
                  accentColor = accentColor,
                  onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                  onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                  onSelectDate = onSelectDate
                )
              }
            }
          }
        }

        item {
          Text(
            text = "Timeline",
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.ExtraBold
          )
        }

        item {
          EventTimeline(
            events = selectedEvents,
            accentColor = accentColor
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachingScheduleScreen(
  selectedDate: LocalDate,
  events: List<CalendarEvent>,
  reminderSettings: TeachingReminderSettings,
  isRefreshing: Boolean,
  onSelectDate: (LocalDate) -> Unit,
  onJumpToToday: () -> Unit,
  onRefresh: () -> Unit,
  onMenuClick: () -> Unit,
  onReminderSettingsChange: (TeachingReminderSettings) -> Unit,
  modifier: Modifier = Modifier
) {
  val selectedEvents = remember(events, selectedDate) {
    events
      .filter { eventContainsDate(it, selectedDate) }
      .sortedWith(
        compareBy<CalendarEvent> { parseEventStart(it) }
          .thenBy { it.timeLabel }
          .thenBy { it.title }
      )
  }
  val accentColor = remember {
    Color(0xFF60A5FA)
  }
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var hasNotificationPermission by remember { mutableStateOf(context.hasNotificationPermission()) }
  var hasExactAlarmPermission by remember { mutableStateOf(TeachingReminderNotifier.canScheduleExactAlarms(context)) }
  var hasFullScreenIntentPermission by remember { mutableStateOf(TeachingReminderNotifier.canUseFullScreenIntent(context)) }
  var hasOverlayPermission by remember { mutableStateOf(TeachingReminderNotifier.canDrawOverlays(context)) }
  val ringtonePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    val pickedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, android.net.Uri::class.java)
    } else {
      @Suppress("DEPRECATION")
      result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    }
    val ringtoneLabel = pickedUri?.let { uri ->
      runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }.getOrNull()
    }.orEmpty().ifBlank { "Nada default sistem" }
    onReminderSettingsChange(
      reminderSettings.copy(
        ringtoneUri = pickedUri?.toString().orEmpty(),
        ringtoneLabel = ringtoneLabel
      )
    )
  }
  val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    hasNotificationPermission = granted
  }
  val exactAlarmPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    hasExactAlarmPermission = TeachingReminderNotifier.canScheduleExactAlarms(context)
  }
  val fullScreenIntentPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    hasFullScreenIntentPermission = TeachingReminderNotifier.canUseFullScreenIntent(context)
  }
  val overlayPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    hasOverlayPermission = TeachingReminderNotifier.canDrawOverlays(context)
  }
  var showReminderSettingsDialog by rememberSaveable { mutableStateOf(false) }
  val reminderSubjectOptions = remember(events) {
    buildReminderSubjectOptions(events)
  }
  val applyReminderSettings: (TeachingReminderSettings) -> Unit = { next ->
    onReminderSettingsChange(next)
    if (next.enabled) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmPermission) {
        exactAlarmPermissionLauncher.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !hasFullScreenIntentPermission) {
        fullScreenIntentPermissionLauncher.launch(TeachingReminderNotifier.buildFullScreenIntentSettingsIntent(context))
      }
      if (!hasOverlayPermission) {
        overlayPermissionLauncher.launch(TeachingReminderNotifier.buildOverlayPermissionIntent(context))
      }
    }
  }

  DisposableEffect(lifecycleOwner, context) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        hasNotificationPermission = context.hasNotificationPermission()
        hasExactAlarmPermission = TeachingReminderNotifier.canScheduleExactAlarms(context)
        hasFullScreenIntentPermission = TeachingReminderNotifier.canUseFullScreenIntent(context)
        hasOverlayPermission = TeachingReminderNotifier.canDrawOverlays(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  if (showReminderSettingsDialog) {
    TeachingReminderSettingsDialog(
      settings = reminderSettings,
      subjectOptions = reminderSubjectOptions,
      hasNotificationPermission = hasNotificationPermission,
      hasExactAlarmPermission = hasExactAlarmPermission,
      hasFullScreenIntentPermission = hasFullScreenIntentPermission,
      hasOverlayPermission = hasOverlayPermission,
      onDismiss = { showReminderSettingsDialog = false },
      onSettingsChange = applyReminderSettings,
      onPickRingtone = {
        ringtonePickerLauncher.launch(
          Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            val existingUri = reminderSettings.ringtoneUri.takeIf { it.isNotBlank() }?.let(android.net.Uri::parse)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
          }
        )
      },
      onRequestNotifications = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
      },
      onRequestExactAlarmPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          exactAlarmPermissionLauncher.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
      },
      onRequestFullScreenIntentPermission = {
        fullScreenIntentPermissionLauncher.launch(TeachingReminderNotifier.buildFullScreenIntentSettingsIntent(context))
      },
      onRequestOverlayPermission = {
        overlayPermissionLauncher.launch(TeachingReminderNotifier.buildOverlayPermissionIntent(context))
      }
    )
  }

  PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    modifier = modifier.fillMaxSize()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      TeachingScheduleHeader(
        selectedDate = selectedDate,
        onJumpToToday = onJumpToToday,
        onMenuClick = onMenuClick
      )

      WeekSwitcherRow(
        selectedDate = selectedDate,
        onPreviousWeek = { onSelectDate(selectedDate.minusWeeks(1)) },
        onNextWeek = { onSelectDate(selectedDate.plusWeeks(1)) }
      )

      WeekCalendar(
        selectedDate = selectedDate,
        events = events,
        accentColor = accentColor,
        onSelectDate = onSelectDate
      )

      TimelineTitleWithAlarm(
        reminderEnabled = reminderSettings.enabled,
        onAlarmClick = { showReminderSettingsDialog = true }
      )

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp)
      ) {
        item {
          EventTimeline(
            events = selectedEvents,
            accentColor = accentColor,
            emptyMessage = "Belum ada jadwal mengajar pada tanggal ini."
          )
        }
      }
    }
  }
}

@Composable
fun CalendarHeader(
  selectedDate: LocalDate,
  accentColor: Color,
  onJumpToToday: () -> Unit,
  onBackClick: () -> Unit
) {
  val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
  val title = if (selectedDate == LocalDate.now()) {
    "Today"
  } else {
    selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.Top
  ) {
    GlassCircleButton(
      icon = Icons.AutoMirrored.Outlined.ArrowBack,
      contentDescription = "Kembali ke dashboard",
      onClick = onBackClick
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .padding(end = 44.dp),
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
        style = MaterialTheme.typography.headlineMedium,
        color = accentColor,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
  }

  TodayShortcutRow(
    modifier = Modifier.padding(top = 12.dp),
    onClick = onJumpToToday
  )
}

@Composable
private fun TeachingScheduleHeader(
  selectedDate: LocalDate,
  onJumpToToday: () -> Unit,
  onMenuClick: () -> Unit
) {
  val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
  val title = if (selectedDate == LocalDate.now()) {
    "Jadwal Hari Ini"
  } else {
    selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.Top
  ) {
    GlassCircleButton(
      icon = Icons.Outlined.Menu,
      contentDescription = "Buka sidebar",
      onClick = onMenuClick
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .padding(end = 44.dp),
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
        style = MaterialTheme.typography.headlineMedium,
        color = Color(0xFF60A5FA),
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
  }

  TodayShortcutRow(
    modifier = Modifier.padding(top = 12.dp),
    onClick = onJumpToToday
  )
}

@Composable
private fun TimelineTitleWithAlarm(
  reminderEnabled: Boolean,
  onAlarmClick: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "Timeline",
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Box(contentAlignment = Alignment.TopEnd) {
      GlassCircleButton(
        icon = Icons.Outlined.Alarm,
        contentDescription = "Atur pengingat jadwal",
        onClick = onAlarmClick
      )
      if (reminderEnabled) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .background(Color(0xFF22C55E), CircleShape)
            .border(1.dp, Color.White, CircleShape)
        )
      }
    }
  }
}

@Composable
private fun TeachingReminderSettingsDialog(
  settings: TeachingReminderSettings,
  subjectOptions: List<ReminderSubjectOption>,
  hasNotificationPermission: Boolean,
  hasExactAlarmPermission: Boolean,
  hasFullScreenIntentPermission: Boolean,
  hasOverlayPermission: Boolean,
  onDismiss: () -> Unit,
  onSettingsChange: (TeachingReminderSettings) -> Unit,
  onPickRingtone: () -> Unit,
  onRequestNotifications: () -> Unit,
  onRequestExactAlarmPermission: () -> Unit,
  onRequestFullScreenIntentPermission: () -> Unit,
  onRequestOverlayPermission: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 660.dp),
      shape = RoundedCornerShape(28.dp),
      color = Color.White,
      tonalElevation = 6.dp,
      shadowElevation = 14.dp
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .background(Color(0xFFE0F2FE), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
          ) {
            androidx.compose.material3.Icon(
              imageVector = Icons.Outlined.Alarm,
              contentDescription = null,
              tint = Color(0xFF0369A1)
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Pengingat Jam Pelajaran",
              style = MaterialTheme.typography.titleMedium,
              color = PrimaryBlueDark,
              fontWeight = FontWeight.ExtraBold
            )
            Text(
              text = if (settings.enabled) "Aktif" else "Nonaktif",
              style = MaterialTheme.typography.bodySmall,
              color = SubtleInk
            )
          }
          Switch(
            checked = settings.enabled,
            onCheckedChange = { enabled ->
              onSettingsChange(settings.copy(enabled = enabled))
            }
          )
        }

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(bottom = 4.dp)
        ) {
          if (settings.enabled) {
            item {
              ReminderLeadTimeSelector(
                selectedMinutes = settings.minutesBefore,
                onSelect = { minutes ->
                  onSettingsChange(settings.copy(minutesBefore = minutes))
                }
              )
            }
            item {
              ReminderTargetSelector(
                settings = settings,
                onSettingsChange = onSettingsChange
              )
            }
            item {
              ReminderSubjectPicker(
                settings = settings,
                subjectOptions = subjectOptions,
                onSettingsChange = onSettingsChange
              )
            }
            item {
              ReminderRepeatSelector(
                selectedMode = settings.repeatMode,
                onSelect = { mode ->
                  onSettingsChange(settings.copy(repeatMode = mode))
                }
              )
            }
            item {
              ReminderOptionRow(
                icon = Icons.Outlined.MusicNote,
                title = "Nada alarm",
                value = settings.ringtoneLabel.ifBlank { "Nada default sistem" },
                onClick = onPickRingtone
              )
            }
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              item {
                ReminderPermissionBanner(
                  title = "Izin notifikasi belum aktif",
                  actionLabel = "Izinkan",
                  onClick = onRequestNotifications
                )
              }
            }
            if (!hasExactAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              item {
                ReminderPermissionBanner(
                  title = "Alarm presisi belum diizinkan",
                  actionLabel = "Buka Pengaturan",
                  onClick = onRequestExactAlarmPermission
                )
              }
            }
            if (!hasFullScreenIntentPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
              item {
                ReminderPermissionBanner(
                  title = "Tampilan alarm penuh belum diizinkan",
                  actionLabel = "Izinkan",
                  onClick = onRequestFullScreenIntentPermission
                )
              }
            }
            if (!hasOverlayPermission) {
              item {
                ReminderPermissionBanner(
                  title = "Agar alarm muncul di atas aplikasi lain, izinkan tampilan overlay",
                  actionLabel = "Izinkan",
                  onClick = onRequestOverlayPermission
                )
              }
            }
          } else {
            item {
              Text(
                text = "Aktifkan pengingat untuk mengatur target mapel, frekuensi, dan nada alarm.",
                style = MaterialTheme.typography.bodyMedium,
                color = SubtleInk
              )
            }
          }
        }

        Text(
          text = "Tutup",
          style = MaterialTheme.typography.labelLarge,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold,
          modifier = Modifier
            .align(Alignment.End)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 10.dp)
        )
      }
    }
  }
}

@Composable
private fun ReminderTargetSelector(
  settings: TeachingReminderSettings,
  onSettingsChange: (TeachingReminderSettings) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "Target pengingat",
      style = MaterialTheme.typography.labelLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.Bold
    )
    ReminderRadioRow(
      title = "Semua mapel",
      subtitle = "Pengingat aktif untuk semua jadwal mengajar.",
      selected = settings.targetMode != ReminderTargetSpecific,
      onClick = {
        onSettingsChange(
          settings.copy(
            targetMode = ReminderTargetAll,
            selectedDistribusiIds = emptyList()
          )
        )
      }
    )
    ReminderRadioRow(
      title = "Mapel tertentu",
      subtitle = "Pilih jadwal yang ingin dibuatkan pengingat.",
      selected = settings.targetMode == ReminderTargetSpecific,
      onClick = {
        onSettingsChange(settings.copy(targetMode = ReminderTargetSpecific))
      }
    )
  }
}

@Composable
private fun ReminderSubjectPicker(
  settings: TeachingReminderSettings,
  subjectOptions: List<ReminderSubjectOption>,
  onSettingsChange: (TeachingReminderSettings) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "Daftar pengingat",
      style = MaterialTheme.typography.labelLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.Bold
    )

    when {
      settings.targetMode != ReminderTargetSpecific -> {
        ReminderSummaryCard(
          title = "Semua mapel",
          subtitle = "${subjectOptions.size} jadwal mapel tersimpan"
        )
      }

      subjectOptions.isEmpty() -> {
        ReminderSummaryCard(
          title = "Belum ada jadwal",
          subtitle = "Tarik untuk refresh setelah jadwal mengajar tersedia."
        )
      }

      else -> {
        subjectOptions.forEach { option ->
          val selected = settings.selectedDistribusiIds.contains(option.distribusiId)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFFF8FAFC), RoundedCornerShape(18.dp))
              .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
              .clickable {
                val nextIds = if (selected) {
                  settings.selectedDistribusiIds - option.distribusiId
                } else {
                  settings.selectedDistribusiIds + option.distribusiId
                }
                onSettingsChange(settings.copy(selectedDistribusiIds = nextIds.distinct()))
              }
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = selected,
              onCheckedChange = { checked ->
                val nextIds = if (checked) {
                  settings.selectedDistribusiIds + option.distribusiId
                } else {
                  settings.selectedDistribusiIds - option.distribusiId
                }
                onSettingsChange(settings.copy(selectedDistribusiIds = nextIds.distinct()))
              }
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = option.title,
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryBlueDark,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = option.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SubtleInk
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ReminderRepeatSelector(
  selectedMode: String,
  onSelect: (String) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "Frekuensi ulang",
      style = MaterialTheme.typography.labelLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.Bold
    )
    ReminderRadioRow(
      title = "Sekali",
      subtitle = "Hanya jadwal terdekat yang cocok.",
      selected = selectedMode == ReminderRepeatOnce,
      onClick = { onSelect(ReminderRepeatOnce) }
    )
    ReminderRadioRow(
      title = "Setiap jam mapel",
      subtitle = "Diulang di setiap jadwal yang cocok.",
      selected = selectedMode != ReminderRepeatOnce,
      onClick = { onSelect(ReminderRepeatEveryLesson) }
    )
  }
}

@Composable
private fun ReminderRadioRow(
  title: String,
  subtitle: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(if (selected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC), RoundedCornerShape(18.dp))
      .border(1.dp, if (selected) Color(0xFFBFDBFE) else CardBorder.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    RadioButton(
      selected = selected,
      onClick = onClick
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
  }
}

@Composable
private fun ReminderSummaryCard(
  title: String,
  subtitle: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFFF8FAFC), RoundedCornerShape(18.dp))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
  }
}

private data class ReminderSubjectOption(
  val distribusiId: String,
  val title: String,
  val subtitle: String
)

private fun buildReminderSubjectOptions(events: List<CalendarEvent>): List<ReminderSubjectOption> {
  return events
    .filter { it.distribusiId.isNotBlank() }
    .groupBy { it.distribusiId }
    .map { (distribusiId, rows) ->
      val first = rows.first()
      val times = rows.map { it.timeLabel }.filter { it.isNotBlank() }.distinct().take(2).joinToString(", ")
      ReminderSubjectOption(
        distribusiId = distribusiId,
        title = first.title,
        subtitle = buildString {
          append("${rows.size} jadwal")
          if (times.isNotBlank()) {
            append(" - ")
            append(times)
          }
        }
      )
    }
    .sortedBy { it.title }
}

@Composable
private fun TeachingReminderSettingsCard(
  settings: TeachingReminderSettings,
  hasNotificationPermission: Boolean,
  hasExactAlarmPermission: Boolean,
  onSettingsChange: (TeachingReminderSettings) -> Unit,
  onPickRingtone: () -> Unit,
  onRequestNotifications: () -> Unit,
  onRequestExactAlarmPermission: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White.copy(alpha = 0.86f), RoundedCornerShape(22.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .background(Color(0xFFE0F2FE), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
      ) {
        androidx.compose.material3.Icon(
          imageVector = Icons.Outlined.NotificationsActive,
          contentDescription = null,
          tint = Color(0xFF0369A1)
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Pengingat Jam Pelajaran",
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = "Alarm lokal akan berbunyi sebelum jam pelajaran dimulai.",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
      Switch(
        checked = settings.enabled,
        onCheckedChange = { enabled ->
          onSettingsChange(settings.copy(enabled = enabled))
        }
      )
    }

    if (settings.enabled) {
      ReminderLeadTimeSelector(
        selectedMinutes = settings.minutesBefore,
        onSelect = { minutes ->
          onSettingsChange(settings.copy(minutesBefore = minutes))
        }
      )

      ReminderOptionRow(
        icon = Icons.Outlined.MusicNote,
        title = "Nada dering",
        value = settings.ringtoneLabel.ifBlank { "Nada default sistem" },
        onClick = onPickRingtone
      )

      if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ReminderPermissionBanner(
          title = "Izin notifikasi belum aktif",
          actionLabel = "Izinkan",
          onClick = onRequestNotifications
        )
      }

      if (!hasExactAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ReminderPermissionBanner(
          title = "Alarm presisi belum diizinkan",
          actionLabel = "Buka Pengaturan",
          onClick = onRequestExactAlarmPermission
        )
      }
    }
  }
}

@Composable
private fun ReminderLeadTimeSelector(
  selectedMinutes: Int,
  onSelect: (Int) -> Unit
) {
  val options = listOf(0, 5, 10, 15, 30)
  var expanded by remember { mutableStateOf(false) }
  Box {
    ReminderOptionRow(
      icon = Icons.Outlined.Schedule,
      title = "Waktu pengingat",
      value = if (selectedMinutes == 0) "Tepat saat mulai" else "$selectedMinutes menit sebelum",
      onClick = { expanded = true }
    )
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      options.forEach { minutes ->
        DropdownMenuItem(
          text = { Text(if (minutes == 0) "Tepat saat mulai" else "$minutes menit sebelum") },
          onClick = {
            onSelect(minutes)
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
private fun ReminderOptionRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  value: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFFF8FAFC), RoundedCornerShape(18.dp))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .background(Color.White, RoundedCornerShape(14.dp))
        .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(14.dp)),
      contentAlignment = Alignment.Center
    ) {
      androidx.compose.material3.Icon(
        imageVector = icon,
        contentDescription = null,
        tint = PrimaryBlueDark
      )
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
  }
}

@Composable
private fun ReminderPermissionBanner(
  title: String,
  actionLabel: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFFFFFBEB), RoundedCornerShape(18.dp))
      .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(18.dp))
      .padding(horizontal = 14.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.bodySmall,
      color = PrimaryBlueDark,
      modifier = Modifier.weight(1f)
    )
    Text(
      text = actionLabel,
      style = MaterialTheme.typography.labelLarge,
      color = Color(0xFFB45309),
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier.clickable(onClick = onClick)
    )
  }
}

@Composable
private fun TodayShortcutRow(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.End
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(14.dp))
        .background(Color.White.copy(alpha = 0.88f))
        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
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
private fun WeekSwitcherRow(
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
      .background(Color.White.copy(alpha = 0.86f), RoundedCornerShape(20.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    GlassCircleButton(
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
    GlassCircleButton(
      icon = Icons.Outlined.ChevronRight,
      contentDescription = "Minggu berikutnya",
      onClick = onNextWeek
    )
  }
}

@Composable
private fun CalendarViewToggle(
  viewMode: CalendarViewMode,
  accentColor: Color,
  onViewModeChange: (CalendarViewMode) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White.copy(alpha = 0.86f), RoundedCornerShape(18.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
      .padding(6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    CalendarToggleChip(
      label = "Week",
      isSelected = viewMode == CalendarViewMode.Week,
      accentColor = accentColor,
      onClick = { onViewModeChange(CalendarViewMode.Week) },
      modifier = Modifier.weight(1f)
    )
    CalendarToggleChip(
      label = "Month",
      isSelected = viewMode == CalendarViewMode.Month,
      accentColor = accentColor,
      onClick = { onViewModeChange(CalendarViewMode.Month) },
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun CalendarToggleChip(
  label: String,
  isSelected: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(if (isSelected) accentColor.copy(alpha = 0.22f) else Color.Transparent)
      .clickable(onClick = onClick)
      .padding(vertical = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = if (isSelected) PrimaryBlueDark else SubtleInk,
      fontWeight = FontWeight.SemiBold
    )
  }
}

@Composable
fun WeekCalendar(
  selectedDate: LocalDate,
  events: List<CalendarEvent>,
  accentColor: Color,
  onSelectDate: (LocalDate) -> Unit
) {
  val startOfWeek = selectedDate.minusDays((selectedDate.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
  val today = LocalDate.now()
  val weekDates = remember(startOfWeek) {
    List(7) { index -> startOfWeek.plusDays(index.toLong()) }
  }
  val selectedIndex = weekDates.indexOf(selectedDate).coerceAtLeast(0)
  val listState = rememberLazyListState()

  LaunchedEffect(startOfWeek) {
    listState.scrollToItem(selectedIndex)
  }

  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val sidePadding = ((maxWidth - WeekCalendarDayChipWidth) / 2).coerceAtLeast(0.dp)

    LazyRow(
      state = listState,
      contentPadding = PaddingValues(horizontal = sidePadding),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(weekDates) { date ->
        val eventColors = remember(events, date) { colorsForDate(events, date) }
        DayChip(
          date = date,
          isSelected = date == selectedDate,
          isToday = date == today,
          eventColors = eventColors,
          accentColor = accentColor,
          onClick = { onSelectDate(date) }
        )
      }
    }
  }
}

@Composable
fun MonthCalendar(
  currentMonth: YearMonth,
  selectedDate: LocalDate,
  events: List<CalendarEvent>,
  accentColor: Color,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onSelectDate: (LocalDate) -> Unit
) {
  val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy").format(currentMonth.atDay(1))
  val firstDay = currentMonth.atDay(1)
  val leadingSlots = firstDay.dayOfWeek.value - 1
  val monthDays = (1..currentMonth.lengthOfMonth()).map { currentMonth.atDay(it) }
  val cells = List(leadingSlots) { null } + monthDays
  val rows = cells.chunked(7)
  val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .pointerInput(currentMonth, swipeThresholdPx) {
        var totalDrag = 0f
        detectHorizontalDragGestures(
          onDragStart = { totalDrag = 0f },
          onHorizontalDrag = { change, dragAmount ->
            totalDrag += dragAmount
            change.consume()
          },
          onDragCancel = { totalDrag = 0f },
          onDragEnd = {
            when {
              totalDrag > swipeThresholdPx -> onPreviousMonth()
              totalDrag < -swipeThresholdPx -> onNextMonth()
            }
            totalDrag = 0f
          }
        )
      }
      .background(Color.White.copy(alpha = 0.80f), RoundedCornerShape(22.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .padding(16.dp)
      .animateContentSize(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      GlassCircleButton(
        icon = Icons.Outlined.ChevronLeft,
        contentDescription = "Bulan sebelumnya",
        onClick = onPreviousMonth
      )
      Text(
        text = monthTitle,
        style = MaterialTheme.typography.titleMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      GlassCircleButton(
        icon = Icons.Outlined.ChevronRight,
        contentDescription = "Bulan berikutnya",
        onClick = onNextMonth
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { dayName ->
        Text(
          text = dayName,
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          fontWeight = FontWeight.SemiBold,
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f)
        )
      }
    }

    rows.forEach { rowDates ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        repeat(7) { index ->
          val date = rowDates.getOrNull(index)
          MonthDayCell(
            date = date,
            isSelected = date == selectedDate,
            isCurrentMonth = date != null,
            isToday = date == LocalDate.now(),
            eventColors = date?.let { colorsForDate(events, it) }.orEmpty(),
            accentColor = accentColor,
            onClick = {
              if (date != null) {
                onSelectDate(date)
              }
            },
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

@Composable
fun EventTimeline(
  events: List<CalendarEvent>,
  accentColor: Color,
  emptyMessage: String = "Belum ada agenda pada tanggal ini.",
  modifier: Modifier = Modifier
) {
  if (events.isEmpty()) {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(22.dp))
        .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
        .padding(18.dp)
    ) {
      Text(
        text = emptyMessage,
        style = MaterialTheme.typography.bodyMedium,
        color = SubtleInk
      )
    }
    return
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    events.forEachIndexed { index, event ->
      TimelineRow(
        event = event,
        isLast = index == events.lastIndex,
        accentColor = accentColor
      )
    }
  }
}

@Composable
private fun TimelineRow(
  event: CalendarEvent,
  isLast: Boolean,
  accentColor: Color
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Column(
      modifier = Modifier.width(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(14.dp)
          .background(accentColor, CircleShape)
      )
      if (!isLast) {
        Spacer(
          modifier = Modifier
            .padding(top = 4.dp)
            .width(2.dp)
            .height(112.dp)
            .background(accentColor.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
        )
      }
    }

    EventCard(
      event = event,
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
fun EventCard(
  event: CalendarEvent,
  modifier: Modifier = Modifier
) {
  val tint = parseCalendarColor(event.colorHex)
  val title = remember(event.title) { sanitizeCalendarText(event.title).ifBlank { "Agenda Akademik" } }
  val description = remember(event.description) { sanitizeCalendarText(event.description) }
  AnimatedVisibility(
    visible = true,
    enter = fadeIn(animationSpec = tween(260)) + slideInVertically(animationSpec = tween(260)) { it / 8 },
    modifier = modifier
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(10.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
        .border(1.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(22.dp))
    ) {
      CalendarGlassBackground(tint = tint)
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = buildCalendarEventMeta(event),
          style = MaterialTheme.typography.labelLarge,
          color = PrimaryBlueDark.copy(alpha = 0.78f),
          fontWeight = FontWeight.Bold
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        if (description.isNotBlank()) {
          Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = SubtleInk
          )
        }
      }
    }
  }
}

@Composable
private fun DayChip(
  date: LocalDate,
  isSelected: Boolean,
  isToday: Boolean,
  eventColors: List<Color>,
  accentColor: Color,
  onClick: () -> Unit
) {
  val backgroundAlpha by animateFloatAsState(
    targetValue = if (isSelected) 1f else 0f,
    animationSpec = tween(220),
    label = "day-chip-alpha"
  )

  Column(
    modifier = Modifier
      .width(74.dp)
      .clip(RoundedCornerShape(22.dp))
      .background(dayCellBrush(isSelected, accentColor, eventColors), RoundedCornerShape(22.dp))
      .border(
        1.dp,
        when {
          isSelected -> accentColor.copy(alpha = 0.34f + (0.20f * backgroundAlpha))
          isToday -> PrimaryBlueDark.copy(alpha = 0.42f)
          eventColors.isNotEmpty() -> eventColors.first().copy(alpha = 0.34f)
          else -> CardBorder
        },
        RoundedCornerShape(22.dp)
      )
      .clickable(onClick = onClick)
      .padding(vertical = 14.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
      style = MaterialTheme.typography.labelMedium,
      color = if (isSelected) Color.White else SubtleInk,
      fontWeight = FontWeight.SemiBold
    )
    Text(
      text = date.dayOfMonth.toString(),
      style = MaterialTheme.typography.titleMedium,
      color = when {
        isSelected -> Color.White
        eventColors.isNotEmpty() -> eventColors.first()
        else -> PrimaryBlueDark
      },
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier.padding(top = 6.dp)
    )
    Row(
      modifier = Modifier.padding(top = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      eventColors.take(3).forEach { color ->
        Box(
          modifier = Modifier
            .size(6.dp)
            .background(if (isSelected) Color.White else color, CircleShape)
        )
      }
      if (isToday) {
        Text(
          text = "Hari ini",
          style = MaterialTheme.typography.labelSmall,
          color = if (isSelected) Color.White else PrimaryBlueDark,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
private fun MonthDayCell(
  date: LocalDate?,
  isSelected: Boolean,
  isCurrentMonth: Boolean,
  isToday: Boolean,
  eventColors: List<Color>,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .height(42.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(monthDayCellBrush(isSelected, isCurrentMonth, accentColor, eventColors), RoundedCornerShape(14.dp))
      .border(
        1.dp,
        when {
          isSelected -> accentColor.copy(alpha = 0.20f)
          isToday -> PrimaryBlueDark.copy(alpha = 0.38f)
          eventColors.isNotEmpty() && isCurrentMonth -> eventColors.first().copy(alpha = 0.28f)
          else -> Color.Transparent
        },
        RoundedCornerShape(14.dp)
      )
      .clickable(enabled = date != null, onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = date?.dayOfMonth?.toString().orEmpty(),
      style = MaterialTheme.typography.bodyMedium,
      color = when {
        isSelected -> Color.White
        eventColors.isNotEmpty() && isCurrentMonth -> eventColors.first()
        isCurrentMonth -> PrimaryBlueDark
        else -> Color.Transparent
      },
      fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
    )
    if (date != null && (eventColors.isNotEmpty() || isToday)) {
      Row(
        modifier = Modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        eventColors.take(3).forEach { color ->
          Box(
            modifier = Modifier
              .size(5.dp)
              .background(if (isSelected) Color.White else color, CircleShape)
          )
        }
        if (isToday) {
          Box(
            modifier = Modifier
              .width(12.dp)
              .height(3.dp)
              .background(if (isSelected) Color.White else PrimaryBlueDark, RoundedCornerShape(999.dp))
          )
        }
      }
    }
  }
}

@Composable
private fun BoxScope.CalendarGlassBackground(tint: Color) {
  Box(
    modifier = Modifier
      .matchParentSize()
      .graphicsLayer {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          renderEffect = RenderEffect
            .createBlurEffect(18f, 18f, Shader.TileMode.CLAMP)
            .asComposeRenderEffect()
        }
      }
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(
            tint.copy(alpha = 0.88f),
            tint.copy(alpha = 0.72f),
            CardGradientEnd.copy(alpha = 0.84f)
          )
        ),
        shape = RoundedCornerShape(22.dp)
      )
  )
}

@Composable
private fun GlassCircleButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(42.dp)
      .background(Color.White.copy(alpha = 0.86f), CircleShape)
      .border(1.dp, CardBorder, CircleShape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    androidx.compose.material3.Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = PrimaryBlueDark
    )
  }
}

private fun parseCalendarColor(hex: String): Color {
  return try {
    Color(android.graphics.Color.parseColor(hex))
  } catch (_: Exception) {
    Color(0xFFDDEBFF)
  }
}

private fun colorsForDate(
  events: List<CalendarEvent>,
  date: LocalDate
): List<Color> {
  return events
    .filter { eventContainsDate(it, date) }
    .map { parseCalendarColor(it.colorHex) }
    .distinct()
}

private fun dayCellBrush(
  isSelected: Boolean,
  accentColor: Color,
  eventColors: List<Color>
): Brush {
  return when {
    isSelected -> Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.94f), accentColor.copy(alpha = 0.86f)))
    eventColors.size > 1 -> Brush.horizontalGradient(eventColors.map { it.copy(alpha = 0.18f) } + listOf(Color.White.copy(alpha = 0.78f)))
    eventColors.size == 1 -> Brush.verticalGradient(listOf(eventColors.first().copy(alpha = 0.20f), Color.White.copy(alpha = 0.78f)))
    else -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.78f), Color.White.copy(alpha = 0.78f)))
  }
}

private fun monthDayCellBrush(
  isSelected: Boolean,
  isCurrentMonth: Boolean,
  accentColor: Color,
  eventColors: List<Color>
): Brush {
  return when {
    isSelected -> Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.94f), accentColor.copy(alpha = 0.86f)))
    !isCurrentMonth -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    eventColors.size > 1 -> Brush.horizontalGradient(eventColors.map { it.copy(alpha = 0.14f) } + listOf(Color(0xFFF8FAFC)))
    eventColors.size == 1 -> Brush.verticalGradient(listOf(eventColors.first().copy(alpha = 0.16f), Color(0xFFF8FAFC)))
    else -> Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF8FAFC)))
  }
}

private fun eventContainsDate(
  event: CalendarEvent,
  selectedDate: LocalDate
): Boolean {
  val start = parseEventStart(event)
  val end = parseEventEnd(event)
  return !selectedDate.isBefore(start) && !selectedDate.isAfter(end)
}

private fun buildCalendarEventMeta(event: CalendarEvent): String {
  val start = parseEventStart(event)
  val end = parseEventEnd(event)
  val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID"))
  val range = if (start == end) {
    formatter.format(start)
  } else {
    "${formatter.format(start)} - ${formatter.format(end)}"
  }
  return if (event.timeLabel.isBlank()) range else "$range | ${event.timeLabel}"
}

private fun parseEventStart(event: CalendarEvent): LocalDate {
  return runCatching { LocalDate.parse(event.startDateIso) }.getOrDefault(LocalDate.now())
}

private fun parseEventEnd(event: CalendarEvent): LocalDate {
  return runCatching { LocalDate.parse(event.endDateIso.ifBlank { event.startDateIso }) }
    .getOrDefault(parseEventStart(event))
}

private fun sanitizeCalendarText(value: String): String {
  val raw = value.trim()
  if (raw.isBlank() || raw.equals("null", ignoreCase = true)) return ""
  if (raw.looksLikeTechnicalJsonText()) return ""
  if (!raw.startsWith("{") && !raw.startsWith("[")) return raw

  val parsedObject = runCatching { JSONObject(raw) }.getOrNull()
  if (parsedObject != null) return extractCalendarText(parsedObject)

  val parsedArray = runCatching { JSONArray(raw) }.getOrNull() ?: return ""
  return buildList {
    for (index in 0 until parsedArray.length()) {
      val child = parsedArray.opt(index)
      val text = when (child) {
        is JSONObject -> extractCalendarText(child)
        else -> sanitizeCalendarText(child?.toString().orEmpty())
      }
      if (text.isReadableCalendarText()) add(text)
    }
  }.distinct().joinToString(", ")
}

private fun extractCalendarText(json: JSONObject): String {
  val preferredKeys = listOf("detail", "keterangan", "deskripsi", "description", "body", "content", "text", "label", "title", "judul", "value")
  for (key in preferredKeys) {
    val text = sanitizeCalendarText(json.opt(key)?.toString().orEmpty())
    if (text.isReadableCalendarText()) return text
  }
  val markerText = sanitizeCalendarText(json.opt("text")?.toString().orEmpty()).lowercase()
  if (markerText.endsWith("_id") || markerText.endsWith("_ids")) return ""

  return buildList {
    val keys = json.keys()
    while (keys.hasNext()) {
      val text = sanitizeCalendarText(json.opt(keys.next())?.toString().orEmpty())
      if (text.isReadableCalendarText()) add(text)
    }
  }.distinct().joinToString(", ")
}

private fun String.isReadableCalendarText(): Boolean {
  val text = trim()
  if (text.isBlank() || text.equals("null", ignoreCase = true)) return false
  if (text.looksLikeTechnicalJsonText()) return false
  if (text.startsWith("{") || text.startsWith("[")) return false
  val lower = text.lowercase()
  val uuidLike = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)
  if (uuidLike.matches(text)) return false
  val technicalTokens = listOf("kelas_id", "kelas_ids", "mapel_id", "mapel_ids", "semester_id", "tahun_ajaran_id", "created_at", "updated_at")
  return technicalTokens.none { lower == it || lower.contains("\"$it\"") }
}

private fun String.looksLikeTechnicalJsonText(): Boolean {
  val lower = trim().lowercase()
  if (!lower.contains("text") || !lower.contains("value")) return false
  val technicalTokens = listOf("kelas_id", "kelas_ids", "mapel_id", "mapel_ids", "semester_id", "tahun_ajaran_id")
  return technicalTokens.any { token ->
    lower.contains("\"text\":\"$token\"") ||
      lower.contains("\"text\": \"$token\"") ||
      lower.contains("\\\"text\\\":\\\"$token\\\"") ||
      lower.contains("\\\"text\\\": \\\"$token\\\"")
  }
}

private fun Context.hasNotificationPermission(): Boolean {
  return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
}
