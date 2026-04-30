package com.mim.guruapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.model.AttendanceApprovalRequest
import com.mim.guruapp.data.model.AttendanceApprovalStudent
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SubtleInk
import com.mim.guruapp.ui.theme.SuccessTint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AttendanceApprovalPopup(
  request: AttendanceApprovalRequest?,
  isLoading: Boolean,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onApprove: (String) -> Unit,
  onReject: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var reviewerNote by remember(request?.id) { mutableStateOf("") }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0x660F172A))
      .clickable(onClick = onDismiss),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .statusBarsPadding()
        .padding(horizontal = 18.dp, vertical = 24.dp)
        .widthIn(max = 460.dp)
        .fillMaxWidth()
        .shadow(18.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x220F172A), spotColor = Color(0x220F172A))
        .background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(26.dp))
        .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(26.dp))
        .padding(18.dp)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {}
        ),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "Review Absensi Pengganti",
            style = MaterialTheme.typography.titleLarge,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = "Persetujuan guru utama diperlukan sebelum absensi masuk ke sistem.",
            style = MaterialTheme.typography.bodySmall,
            color = SubtleInk
          )
        }
        Box(
          modifier = Modifier
            .size(34.dp)
            .background(Color(0xFFF8FAFC), CircleShape)
            .border(1.dp, CardBorder, CircleShape)
            .clickable(onClick = onDismiss),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Tutup review",
            tint = SubtleInk,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      if (isLoading || request == null) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(
            color = SuccessTint,
            strokeWidth = 2.4.dp
          )
        }
      } else {
        AttendanceApprovalSummary(request = request)

        OutlinedTextField(
          value = reviewerNote,
          onValueChange = { reviewerNote = it },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          label = { Text("Catatan review (opsional)") },
          minLines = 2,
          maxLines = 4
        )

        Text(
          text = "Daftar santri",
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.Bold
        )

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(request.students, key = { it.studentId }) { student ->
            AttendanceApprovalStudentCard(student = student)
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          AttendanceApprovalActionButton(
            title = "Tolak",
            tint = Color(0xFFDC2626),
            icon = Icons.Outlined.Close,
            enabled = !isSaving,
            modifier = Modifier.weight(1f),
            onClick = { onReject(reviewerNote) }
          )
          AttendanceApprovalActionButton(
            title = "Setujui",
            tint = SuccessTint,
            icon = Icons.Outlined.Check,
            enabled = !isSaving,
            modifier = Modifier.weight(1f),
            onClick = { onApprove(reviewerNote) }
          )
        }

        if (isSaving) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = SuccessTint,
              strokeWidth = 2.4.dp
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AttendanceApprovalSummary(
  request: AttendanceApprovalRequest
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFFF8FAFC), RoundedCornerShape(20.dp))
      .border(1.dp, CardBorder.copy(alpha = 0.82f), RoundedCornerShape(20.dp))
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = "${request.subjectTitle} - ${request.className}",
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )
    AttendanceApprovalMetaRow("Tanggal", formatApprovalDate(request.dateIso))
    AttendanceApprovalMetaRow("Guru pengganti", request.substituteTeacherName)
    if (request.lessonLabels.isNotEmpty()) {
      AttendanceApprovalMetaRow("Jam", request.lessonLabels.joinToString(", "))
    }
    if (request.material.isNotBlank()) {
      AttendanceApprovalMetaRow("Materi", request.material)
    }
    if (request.note.isNotBlank()) {
      AttendanceApprovalMetaRow("Catatan", request.note)
    }
  }
}

@Composable
private fun AttendanceApprovalMetaRow(
  label: String,
  value: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = SubtleInk,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.widthIn(min = 82.dp)
    )
    Text(
      text = value.ifBlank { "-" },
      style = MaterialTheme.typography.bodyMedium,
      color = PrimaryBlueDark,
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun AttendanceApprovalStudentCard(
  student: AttendanceApprovalStudent
) {
  val palette = approvalStatusPalette(student.status)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White, RoundedCornerShape(18.dp))
      .border(1.dp, CardBorder.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
      .padding(horizontal = 14.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(10.dp)
        .background(palette, CircleShape)
    )
    Text(
      text = student.studentName,
      style = MaterialTheme.typography.bodyLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )
    Box(
      modifier = Modifier
        .background(palette.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
        .border(1.dp, palette.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
        .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
      Text(
        text = student.status,
        style = MaterialTheme.typography.labelMedium,
        color = palette,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun AttendanceApprovalActionButton(
  title: String,
  tint: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  enabled: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Row(
    modifier = modifier
      .background(tint.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
      .border(1.dp, tint.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = tint,
      modifier = Modifier.size(18.dp)
    )
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      color = tint,
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier.padding(start = 8.dp)
    )
  }
}

private fun formatApprovalDate(dateIso: String): String {
  val date = runCatching { LocalDate.parse(dateIso) }.getOrDefault(LocalDate.now())
  return date.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.forLanguageTag("id-ID")))
}

@Composable
private fun approvalStatusPalette(status: String): Color {
  return when (status.trim().lowercase()) {
    "hadir" -> Color(0xFF047857)
    "terlambat" -> Color(0xFFC2410C)
    "sakit" -> Color(0xFF9333EA)
    "izin" -> Color(0xFF1D4ED8)
    "alpa" -> Color(0xFFB91C1C)
    else -> PrimaryBlueDark
  }
}
