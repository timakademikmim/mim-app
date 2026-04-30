package com.mim.guruapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.LeaveRequestSaveOutcome
import com.mim.guruapp.data.model.LeaveRequestItem
import com.mim.guruapp.data.model.LeaveRequestSnapshot
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.HighlightCard
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SubtleInk
import com.mim.guruapp.ui.theme.SuccessTint
import com.mim.guruapp.ui.theme.WarmAccent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.launch

private enum class LeaveDateTarget {
  Start,
  End
}

private data class LeaveStatusPalette(
  val accent: Color,
  val background: Color,
  val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerizinanScreen(
  snapshot: LeaveRequestSnapshot,
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadSnapshot: suspend () -> LeaveRequestSnapshot?,
  onSubmitRequest: suspend (String, String, String) -> LeaveRequestSaveOutcome,
  onDeleteRequest: suspend (String) -> LeaveRequestSaveOutcome,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  var startDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
  var endDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
  var purpose by rememberSaveable { mutableStateOf("") }
  var isSubmitting by rememberSaveable { mutableStateOf(false) }
  var deletingRequestId by rememberSaveable { mutableStateOf("") }
  var datePickerTarget by rememberSaveable { mutableStateOf<LeaveDateTarget?>(null) }
  var confirmDeleteTarget by remember { mutableStateOf<LeaveRequestItem?>(null) }
  var isInitialLoading by rememberSaveable { mutableStateOf(false) }
  var hasRequestedInitialLoad by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    if (!hasRequestedInitialLoad) {
      hasRequestedInitialLoad = true
      isInitialLoading = snapshot.requests.isEmpty()
      onLoadSnapshot()
      isInitialLoading = false
    }
  }

  val durationDays = remember(startDate, endDate) {
    if (endDate.isBefore(startDate)) 0 else ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
  }
  val canSubmit = !isSubmitting &&
    purpose.trim().isNotBlank() &&
    durationDays in 1..90

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      PullToRefreshBox(
        isRefreshing = isRefreshing || isInitialLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
        ) {
          PerizinanTopBar(
            title = "Perizinan",
            onMenuClick = onMenuClick
          )

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 124.dp)
          ) {
            item {
              PerizinanFormCard(
                startDate = startDate,
                endDate = endDate,
                purpose = purpose,
                durationDays = durationDays,
                isSubmitting = isSubmitting,
                canSubmit = canSubmit,
                onStartDateClick = { datePickerTarget = LeaveDateTarget.Start },
                onEndDateClick = { datePickerTarget = LeaveDateTarget.End },
                onPurposeChange = { purpose = it },
                onSubmitClick = {
                  scope.launch {
                    isSubmitting = true
                    val result = onSubmitRequest(
                      startDate.toString(),
                      endDate.toString(),
                      purpose
                    )
                    isSubmitting = false
                    snackbarHostState.showSnackbar(result.message)
                    if (result.success) {
                      startDate = LocalDate.now()
                      endDate = LocalDate.now()
                      purpose = ""
                    }
                  }
                }
              )
            }

            item {
              PerizinanSectionHeader(
                title = "Daftar Pengajuan",
                subtitle = if (snapshot.requests.isEmpty()) {
                  "Belum ada pengajuan izin."
                } else {
                  "${snapshot.requests.size} pengajuan tersimpan."
                }
              )
            }

            when {
              isInitialLoading && snapshot.requests.isEmpty() -> {
                items(3) {
                  SkeletonContentCard(
                    leadingSize = 44.dp,
                    trailingSize = 40.dp,
                    firstLineWidth = 0.56f,
                    secondLineWidth = 0.8f,
                    thirdLineWidth = 0.44f
                  )
                }
              }

              snapshot.requests.isEmpty() -> {
                item {
                  PerizinanEmptyCard("Belum ada pengajuan izin. Buat pengajuan baru dari card di atas.")
                }
              }

              else -> {
                items(
                  items = snapshot.requests,
                  key = { it.id }
                ) { request ->
                  LeaveRequestCard(
                    request = request,
                    isDeleting = deletingRequestId == request.id,
                    onDeleteClick = { confirmDeleteTarget = request }
                  )
                }
              }
            }
          }
        }
      }

      SavingOverlay(visible = isSubmitting)
    }
  }

  val activeDate = when (datePickerTarget) {
    LeaveDateTarget.Start -> startDate
    LeaveDateTarget.End -> endDate
    null -> null
  }
  if (activeDate != null) {
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
      initialSelectedDateMillis = activeDate.toPickerMillis()
    )
    DatePickerDialog(
      onDismissRequest = { datePickerTarget = null },
      confirmButton = {
        TextButton(
          onClick = {
            val selectedDate = datePickerState.selectedDateMillis?.toLocalDateFromPicker() ?: activeDate
            when (datePickerTarget) {
              LeaveDateTarget.Start -> {
                startDate = selectedDate
                if (endDate.isBefore(selectedDate)) {
                  endDate = selectedDate
                }
              }

              LeaveDateTarget.End -> {
                endDate = if (selectedDate.isBefore(startDate)) startDate else selectedDate
              }

              null -> Unit
            }
            datePickerTarget = null
          }
        ) {
          Text("Pilih")
        }
      },
      dismissButton = {
        TextButton(onClick = { datePickerTarget = null }) {
          Text("Batal")
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }

  confirmDeleteTarget?.let { request ->
    val deleteLabel = if (request.status == "menunggu") "Batalkan" else "Hapus"
    AlertDialog(
      onDismissRequest = { confirmDeleteTarget = null },
      title = { Text("$deleteLabel pengajuan") },
      text = {
        Text(
          if (request.status == "menunggu") {
            "Pengajuan izin ini akan dibatalkan dari daftar."
          } else {
            "Pengajuan izin ini akan dihapus dari daftar perizinan."
          }
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            scope.launch {
              deletingRequestId = request.id
              confirmDeleteTarget = null
              val result = onDeleteRequest(request.id)
              deletingRequestId = ""
              snackbarHostState.showSnackbar(result.message)
            }
          }
        ) {
          Text(deleteLabel)
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDeleteTarget = null }) {
          Text("Tutup")
        }
      }
    )
  }
}

@Composable
private fun PerizinanTopBar(
  title: String,
  onMenuClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    PerizinanCircleButton(
      icon = Icons.Outlined.Menu,
      contentDescription = "Buka sidebar",
      onClick = onMenuClick
    )
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      textAlign = TextAlign.Center,
      modifier = Modifier.weight(1f)
    )
    Spacer(modifier = Modifier.size(42.dp))
  }
}

@Composable
private fun PerizinanFormCard(
  startDate: LocalDate,
  endDate: LocalDate,
  purpose: String,
  durationDays: Int,
  isSubmitting: Boolean,
  canSubmit: Boolean,
  onStartDateClick: () -> Unit,
  onEndDateClick: () -> Unit,
  onPurposeChange: (String) -> Unit,
  onSubmitClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x120F172A), spotColor = Color(0x120F172A))
      .clip(RoundedCornerShape(26.dp))
      .background(Color.White.copy(alpha = 0.95f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(26.dp))
      .padding(18.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(PrimaryBlue.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.Send,
          contentDescription = null,
          tint = PrimaryBlue,
          modifier = Modifier.size(20.dp)
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = "Ajukan Perizinan",
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = "Tentukan rentang tanggal izin dan isi keperluannya. Status persetujuan akan muncul di daftar bawah.",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      PerizinanPickerField(
        label = "Tanggal mulai",
        value = startDate.formatFriendlyDate(),
        onClick = onStartDateClick,
        modifier = Modifier.weight(1f)
      )
      PerizinanPickerField(
        label = "Tanggal selesai",
        value = endDate.formatFriendlyDate(),
        onClick = onEndDateClick,
        modifier = Modifier.weight(1f)
      )
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(HighlightCard.copy(alpha = 0.1f))
        .border(1.dp, HighlightCard.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
        .padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Outlined.CalendarMonth,
        contentDescription = null,
        tint = HighlightCard,
        modifier = Modifier.size(18.dp)
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = "Durasi izin",
          style = MaterialTheme.typography.labelMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = if (durationDays > 0) "$durationDays hari" else "Tanggal selesai harus sesudah tanggal mulai",
          style = MaterialTheme.typography.bodySmall,
          color = if (durationDays > 0) SubtleInk else WarmAccent,
          fontWeight = FontWeight.SemiBold
        )
      }
    }

    OutlinedTextField(
      value = purpose,
      onValueChange = onPurposeChange,
      label = { Text("Keperluan izin") },
      placeholder = { Text("Contoh: menghadiri acara keluarga, kontrol kesehatan, dan seterusnya") },
      modifier = Modifier.fillMaxWidth(),
      minLines = 4,
      maxLines = 5,
      shape = RoundedCornerShape(18.dp)
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End
    ) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(18.dp))
          .background(if (canSubmit) HighlightCard else CardBorder.copy(alpha = 0.65f))
          .clickable(enabled = canSubmit, onClick = onSubmitClick)
          .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 2.2.dp,
            modifier = Modifier.size(18.dp)
          )
        } else {
          Text(
            text = "Ajukan Izin",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }
    }
  }
}

@Composable
private fun PerizinanSectionHeader(
  title: String,
  subtitle: String
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(3.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
  }
}

@Composable
private fun LeaveRequestCard(
  request: LeaveRequestItem,
  isDeleting: Boolean,
  onDeleteClick: () -> Unit
) {
  val palette = leaveStatusPalette(request.status)
  val actionLabel = if (request.status == "menunggu") "Batalkan" else "Hapus"

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(24.dp))
      .background(palette.background)
      .border(1.dp, palette.accent.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(Color.White.copy(alpha = 0.78f))
          .border(1.dp, Color.White.copy(alpha = 0.56f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = palette.icon,
          contentDescription = null,
          tint = palette.accent,
          modifier = Modifier.size(20.dp)
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(5.dp)
      ) {
        Text(
          text = request.purpose.ifBlank { "Keperluan izin" },
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${request.startDateIso.formatFriendlyDateIso()} - ${request.endDateIso.formatFriendlyDateIso()}",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          fontWeight = FontWeight.SemiBold
        )
      }
      PerizinanStatusChip(
        status = request.status,
        accent = palette.accent
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      PerizinanMetaPill(
        label = "Durasi ${request.durationDays} hari",
        accent = palette.accent
      )
      PerizinanMetaPill(
        label = "Diajukan ${request.createdAt.formatFriendlyTimestamp()}",
        accent = PrimaryBlue
      )
    }

    if (request.reviewerNote.isNotBlank()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(Color.White.copy(alpha = 0.62f))
          .border(1.dp, Color.White.copy(alpha = 0.58f), RoundedCornerShape(18.dp))
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = "Catatan Wakasek",
          style = MaterialTheme.typography.labelLarge,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = request.reviewerNote,
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End
    ) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(16.dp))
          .background(Color.White.copy(alpha = 0.72f))
          .border(1.dp, palette.accent.copy(alpha = 0.24f), RoundedCornerShape(16.dp))
          .clickable(enabled = !isDeleting, onClick = onDeleteClick)
          .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (isDeleting) {
            CircularProgressIndicator(
              color = palette.accent,
              strokeWidth = 2.dp,
              modifier = Modifier.size(16.dp)
            )
          } else {
            Icon(
              imageVector = Icons.Outlined.DeleteOutline,
              contentDescription = null,
              tint = palette.accent,
              modifier = Modifier.size(16.dp)
            )
          }
          Text(
            text = if (isDeleting) "Memproses..." else actionLabel,
            style = MaterialTheme.typography.labelLarge,
            color = palette.accent,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }
    }
  }
}

@Composable
private fun PerizinanEmptyCard(message: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(Color.White.copy(alpha = 0.86f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(22.dp))
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
private fun PerizinanStatusChip(
  status: String,
  accent: Color
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(Color.White.copy(alpha = 0.8f))
      .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Text(
      text = statusLabel(status),
      style = MaterialTheme.typography.labelMedium,
      color = accent,
      fontWeight = FontWeight.ExtraBold
    )
  }
}

@Composable
private fun PerizinanMetaPill(
  label: String,
  accent: Color
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(Color.White.copy(alpha = 0.68f))
      .border(1.dp, accent.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.SemiBold
    )
  }
}

@Composable
private fun PerizinanPickerField(
  label: String,
  value: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color.White.copy(alpha = 0.74f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = SubtleInk,
      fontWeight = FontWeight.SemiBold
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        imageVector = Icons.Outlined.CalendarMonth,
        contentDescription = null,
        tint = HighlightCard,
        modifier = Modifier.size(18.dp)
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
private fun PerizinanCircleButton(
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

@Composable
private fun leaveStatusPalette(status: String): LeaveStatusPalette {
  return when (status) {
    "diterima" -> LeaveStatusPalette(
      accent = HighlightCard,
      background = SuccessTint.copy(alpha = 0.76f),
      icon = Icons.Outlined.CheckCircle
    )

    "ditolak" -> LeaveStatusPalette(
      accent = WarmAccent,
      background = Color(0xFFFFF1F2),
      icon = Icons.Outlined.DeleteOutline
    )

    else -> LeaveStatusPalette(
      accent = PrimaryBlue,
      background = Color(0xFFF7FAFF),
      icon = Icons.Outlined.HourglassTop
    )
  }
}

private fun statusLabel(status: String): String {
  return when (status) {
    "diterima" -> "Diterima"
    "ditolak" -> "Ditolak"
    else -> "Menunggu"
  }
}

private fun LocalDate.formatFriendlyDate(): String {
  return format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
}

private fun String.formatFriendlyDateIso(): String {
  val date = runCatching { LocalDate.parse(take(10)) }.getOrNull() ?: return this
  return date.formatFriendlyDate()
}

private fun String.formatFriendlyTimestamp(): String {
  if (isBlank()) return "-"
  return runCatching {
    val instant = Instant.parse(this)
    DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
      .withZone(ZoneId.systemDefault())
      .format(instant)
  }.getOrElse {
    formatFriendlyDateIso()
  }
}

private fun LocalDate.toPickerMillis(): Long {
  return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun Long.toLocalDateFromPicker(): LocalDate {
  return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}
