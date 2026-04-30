package com.mim.guruapp.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.MutabaahSaveOutcome
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.MutabaahSnapshot
import com.mim.guruapp.data.model.MutabaahSubmission
import com.mim.guruapp.data.model.MutabaahTask
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
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutabaahScreen(
  selectedDate: LocalDate,
  snapshot: MutabaahSnapshot,
  isRefreshing: Boolean,
  onSelectDate: (LocalDate) -> Unit,
  onJumpToToday: () -> Unit,
  onRefresh: () -> Unit,
  onMenuClick: () -> Unit,
  onLoadSnapshot: suspend (LocalDate) -> MutabaahSnapshot?,
  onToggleTask: suspend (String, String, Boolean) -> MutabaahSaveOutcome,
  modifier: Modifier = Modifier
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var isLoadingPeriod by rememberSaveable { mutableStateOf(false) }
  var savingTaskId by rememberSaveable { mutableStateOf<String?>(null) }
  val selectedPeriod = selectedDate.toString().take(7)

  BackHandler(enabled = false) {}

  LaunchedEffect(selectedPeriod, snapshot.period) {
    if (snapshot.period != selectedPeriod && !isLoadingPeriod) {
      isLoadingPeriod = true
      onLoadSnapshot(selectedDate)
      isLoadingPeriod = false
    }
  }

  val holidayDates = remember(snapshot.academicHolidayDates) { snapshot.academicHolidayDates.toSet() }
  val dayTasks = remember(snapshot.tasks, selectedDate) {
    snapshot.tasks
      .filter { it.active && it.dateIso == selectedDate.toString() }
      .sortedWith(compareBy<MutabaahTask> { it.title.lowercase() }.thenBy { it.id })
  }
  val submissions = remember(snapshot.submissions) {
    snapshot.submissions.associateBy { "${it.templateId}|${it.dateIso}" }
  }
  val doneCount = dayTasks.count { task ->
    submissions["${task.id}|${selectedDate}"]?.status == "selesai"
  }
  val isHoliday = holidayDates.contains(selectedDate.toString())
  val isSunday = selectedDate.dayOfWeek == DayOfWeek.SUNDAY
  val timelineEvents = remember(snapshot.tasks) { snapshot.tasks.toCalendarEvents() }

  androidx.compose.material3.Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
  ) { innerPadding ->
    PullToRefreshBox(
      isRefreshing = isRefreshing || isLoadingPeriod,
      onRefresh = onRefresh,
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
        MutabaahHeader(
          selectedDate = selectedDate,
          onJumpToToday = onJumpToToday,
          onMenuClick = onMenuClick
        )

        MutabaahWeekSwitcher(
          selectedDate = selectedDate,
          onPreviousWeek = { onSelectDate(selectedDate.minusWeeks(1)) },
          onNextWeek = { onSelectDate(selectedDate.plusWeeks(1)) }
        )

        WeekCalendar(
          selectedDate = selectedDate,
          events = timelineEvents,
          accentColor = HighlightCard,
          onSelectDate = onSelectDate
        )

        MutabaahSummaryCard(
          total = dayTasks.size,
          done = doneCount,
          isSunday = isSunday,
          isHoliday = isHoliday
        )

        Text(
          text = "Timeline Mutabaah",
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
          when {
            isSunday -> item {
              MutabaahEmptyCard("Hari Ahad libur. Mutabaah tidak diinput pada hari Ahad.")
            }

            isHoliday -> item {
              MutabaahEmptyCard("Tanggal ini libur akademik. Mutabaah tidak dihitung.")
            }

            isLoadingPeriod -> items(4) {
              SkeletonContentCard(
                leadingSize = 44.dp,
                trailingSize = 42.dp,
                firstLineWidth = 0.62f,
                secondLineWidth = 0.82f
              )
            }

            dayTasks.isEmpty() -> item {
              MutabaahEmptyCard("Belum ada tugas mutabaah untuk tanggal ini.")
            }

            else -> dayTasks.forEachIndexed { index, task ->
              item(key = task.id) {
                val submission = submissions["${task.id}|${selectedDate}"]
                val checked = submission?.status == "selesai"
                MutabaahTimelineRow(
                  task = task,
                  checked = checked,
                  submission = submission,
                  isSaving = savingTaskId == task.id || submission?.syncState == "syncing",
                  isLast = index == dayTasks.lastIndex,
                  onCheckedChange = { nextChecked ->
                    scope.launch {
                      savingTaskId = task.id
                      val result = onToggleTask(task.id, selectedDate.toString(), nextChecked)
                      savingTaskId = null
                      if (!result.success) {
                        snackbarHostState.showSnackbar(result.message)
                      }
                    }
                  }
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MutabaahHeader(
  selectedDate: LocalDate,
  onJumpToToday: () -> Unit,
  onMenuClick: () -> Unit
) {
  val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
  val title = if (selectedDate == LocalDate.now()) {
    "Mutabaah Hari Ini"
  } else {
    selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.Top
  ) {
    MutabaahCircleButton(
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
        style = MaterialTheme.typography.headlineSmall,
        color = HighlightCard,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
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
        .background(Color.White.copy(alpha = 0.88f))
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
private fun MutabaahWeekSwitcher(
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
    MutabaahCircleButton(
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
    MutabaahCircleButton(
      icon = Icons.Outlined.ChevronRight,
      contentDescription = "Minggu berikutnya",
      onClick = onNextWeek
    )
  }
}

@Composable
private fun MutabaahSummaryCard(
  total: Int,
  done: Int,
  isSunday: Boolean,
  isHoliday: Boolean
) {
  val label = when {
    isSunday -> "Ahad libur"
    isHoliday -> "Libur akademik"
    total == 0 -> "Belum ada tugas"
    else -> "$done dari $total selesai"
  }
  val percent = if (total <= 0) 0 else ((done.toFloat() / total.toFloat()) * 100).toInt()
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
      Column {
        Text(
          text = "Progress Mutabaah",
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = label,
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
      Text(
        text = "$percent%",
        style = MaterialTheme.typography.titleLarge,
        color = HighlightCard,
        fontWeight = FontWeight.ExtraBold
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
          .background(HighlightCard, RoundedCornerShape(999.dp))
      )
    }
  }
}

@Composable
private fun MutabaahTimelineRow(
  task: MutabaahTask,
  checked: Boolean,
  submission: MutabaahSubmission?,
  isSaving: Boolean,
  isLast: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
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
          .background(if (checked) HighlightCard else Color.White)
          .border(
            2.dp,
            if (checked) HighlightCard.copy(alpha = 0.28f) else WarmAccent.copy(alpha = 0.62f),
            CircleShape
          )
          .clickable(enabled = !isSaving) { onCheckedChange(!checked) },
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
            contentDescription = "Selesai",
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
            .background((if (checked) HighlightCard else WarmAccent).copy(alpha = 0.24f), RoundedCornerShape(999.dp))
        )
      }
    }

    Box(
      modifier = Modifier
        .weight(1f)
        .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
        .clip(RoundedCornerShape(24.dp))
        .border(
          1.dp,
          if (checked) HighlightCard.copy(alpha = 0.18f) else WarmAccent.copy(alpha = 0.26f),
          RoundedCornerShape(24.dp)
        )
        .animateContentSize()
    ) {
      MutabaahGlassBackground(
        tint = if (checked) SuccessTint else Color(0xFFFFF1D8),
        accent = if (checked) HighlightCard else WarmAccent
      )
      Box(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 1.dp)
          .width(5.dp)
          .height(74.dp)
          .clip(RoundedCornerShape(999.dp))
          .background((if (checked) HighlightCard else WarmAccent).copy(alpha = 0.64f))
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
              .background(Color.White.copy(alpha = 0.62f))
              .border(1.dp, Color.White.copy(alpha = 0.58f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.TaskAlt,
              contentDescription = null,
              tint = if (checked) HighlightCard else WarmAccent,
              modifier = Modifier.size(20.dp)
            )
          }
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Text(
              text = task.title,
              style = MaterialTheme.typography.titleSmall,
              color = PrimaryBlueDark,
              fontWeight = FontWeight.ExtraBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (task.description.isNotBlank()) {
              Text(
                text = task.description,
                style = MaterialTheme.typography.bodySmall,
                color = SubtleInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
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
              .background(Color.White.copy(alpha = 0.68f))
              .border(1.dp, Color.White.copy(alpha = 0.58f), RoundedCornerShape(999.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = when {
                isSaving -> "Menyinkronkan..."
                submission?.syncState == "pending" -> "Belum sync"
                checked -> "Tersimpan otomatis"
                else -> "Ketuk titik untuk selesai"
              },
              style = MaterialTheme.typography.labelMedium,
              color = when {
                isSaving -> WarmAccent
                submission?.syncState == "pending" -> WarmAccent
                checked -> HighlightCard
                else -> SubtleInk
              },
              fontWeight = FontWeight.SemiBold
            )
          }
          Text(
            text = if (checked) "Selesai" else "Belum",
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
private fun MutabaahEmptyCard(message: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(Color.White.copy(alpha = 0.84f))
      .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
      .padding(18.dp)
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = SubtleInk
    )
  }
}

@Composable
private fun MutabaahCircleButton(
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
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = PrimaryBlueDark
    )
  }
}

private fun List<MutabaahTask>.toCalendarEvents(): List<CalendarEvent> {
  return filter { it.active }.map { task ->
    CalendarEvent(
      id = "mutabaah-${task.id}",
      startDateIso = task.dateIso,
      endDateIso = task.dateIso,
      title = task.title,
      description = task.description,
      timeLabel = "",
      colorHex = "#CFF7D3",
      categoryKey = "mutabaah"
    )
  }
}

@Composable
private fun BoxScope.MutabaahGlassBackground(
  tint: Color,
  accent: Color
) {
  Box(
    modifier = Modifier
      .matchParentSize()
      .graphicsLayer {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          renderEffect = RenderEffect
            .createBlurEffect(14f, 14f, Shader.TileMode.CLAMP)
            .asComposeRenderEffect()
        }
      }
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(
            tint.copy(alpha = 0.88f),
            tint.copy(alpha = 0.74f),
            CardGradientEnd.copy(alpha = 0.9f)
          )
        ),
        shape = RoundedCornerShape(24.dp)
      )
      .border(1.dp, accent.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
  )
}
