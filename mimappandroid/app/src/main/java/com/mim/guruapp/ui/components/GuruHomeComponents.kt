package com.mim.guruapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.model.AvailableSubjectOffer
import com.mim.guruapp.data.model.SubjectOverview
import com.mim.guruapp.ui.i18n.t
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.CardGradientEnd
import com.mim.guruapp.ui.theme.CardGradientStart
import com.mim.guruapp.ui.theme.HighlightCard
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.SubtleInk
import com.mim.guruapp.ui.theme.SuccessTint
import com.mim.guruapp.ui.theme.WarmAccent

@Composable
fun HomeHeroCard(
  teacherName: String,
  teacherRole: String,
  message: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x180F172A), spotColor = Color(0x180F172A))
      .background(
        brush = Brush.verticalGradient(listOf(CardBackground, CardGradientEnd)),
        shape = RoundedCornerShape(24.dp)
      )
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(24.dp))
      .padding(20.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(SuccessTint)
      )
      Text(
        text = t(teacherRole),
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlue
      )
    }
    Text(
      text = teacherName,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier.padding(top = 8.dp)
    )
    Text(
      text = t(message),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 10.dp)
    )
  }
}

@Composable
fun MapelToolbarCard(
  isClaimSectionVisible: Boolean,
  hasAvailableSubjects: Boolean,
  onToggleClaimSection: () -> Unit
) {
  PlaceholderPanel(
    title = "Mapel",
    subtitle = t("Halaman ini mengikuti struktur daftar mapel di panel guru versi web.")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = t("Guru hanya bisa mengambil mapel yang belum memiliki pengajar. Penggantian pengajar tetap melalui admin."),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f)
      )
      OutlinedButton(
        onClick = onToggleClaimSection,
        modifier = Modifier.padding(start = 12.dp)
      ) {
        Text(
          text = if (isClaimSectionVisible) t("Tutup") else t("Tambah Mapel")
        )
      }
    }
    if (!hasAvailableSubjects) {
      Text(
        text = t("Tidak ada mapel kosong untuk tahun ajaran aktif saat ini."),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        modifier = Modifier.padding(top = 12.dp)
      )
    }
  }
}

@Composable
fun AvailableMapelPanel(
  subjects: List<AvailableSubjectOffer>,
  selectedIds: Set<String>,
  onToggleSubject: (String) -> Unit,
  onClearSelection: () -> Unit,
  onClaimSelectedSubjects: () -> Unit,
  searchQuery: String = "",
  onSearchQueryChange: ((String) -> Unit)? = null
) {
  val normalizedQuery = searchQuery.trim().lowercase()
  val filteredSubjects = if (normalizedQuery.isBlank()) {
    subjects
  } else {
    subjects.filter { subject ->
      buildString {
        append(subject.title)
        append(' ')
        append(subject.className)
        append(' ')
        append(subject.semester)
      }.lowercase().contains(normalizedQuery)
    }
  }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(CardBackground)
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(18.dp))
      .padding(16.dp)
  ) {
    Text(
      text = t("Mapel tersedia untuk diambil"),
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold
    )
    if (onSearchQueryChange != null) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        label = { Text(t("Cari Mapel")) },
        placeholder = { Text(t("Cari nama mapel, kelas, atau semester")) }
      )
    }
    Column(
      modifier = Modifier.padding(top = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (filteredSubjects.isEmpty()) {
        EmptyPlaceholderCard("Tidak ada mapel tersedia yang cocok dengan pencarian.")
      } else {
        filteredSubjects.forEach { subject ->
          AvailableMapelCard(
            subject = subject,
            checked = selectedIds.contains(subject.id),
            onToggle = { onToggleSubject(subject.id) }
          )
        }
      }
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 14.dp),
      horizontalArrangement = Arrangement.End
    ) {
      OutlinedButton(onClick = onClearSelection) {
        Text(t("Batal Pilih"))
      }
      androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
      OutlinedButton(
        onClick = onClaimSelectedSubjects,
        enabled = selectedIds.isNotEmpty()
      ) {
        Text(t("Tambahkan Terpilih"))
      }
    }
  }
}

@Composable
fun AvailableMapelCard(
  subject: AvailableSubjectOffer,
  checked: Boolean,
  onToggle: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Brush.linearGradient(listOf(CardGradientStart, CardGradientEnd)))
      .border(
        BorderStroke(1.dp, if (checked) HighlightCard else CardBorder),
        RoundedCornerShape(16.dp)
      )
      .padding(14.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically
    ) {
      Checkbox(
        checked = checked,
        onCheckedChange = { onToggle() }
      )
      Text(
        text = t("Pilih mapel ini"),
        style = MaterialTheme.typography.labelMedium,
        color = SubtleInk
      )
    }
    Text(
      text = subject.title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "${t("Kelas")}: ${subject.className}",
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      modifier = Modifier.padding(top = 6.dp)
    )
    Text(
      text = "${t("Semester")}: ${subject.semester}${if (subject.semesterActive) " (${t("Aktif")})" else ""}",
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      modifier = Modifier.padding(top = 2.dp)
    )
  }
}

@Composable
fun WebMapelCard(subject: SubjectOverview) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x160F172A), spotColor = Color(0x160F172A))
      .background(
        Brush.linearGradient(listOf(CardGradientStart, CardGradientEnd)),
        RoundedCornerShape(16.dp)
      )
      .border(BorderStroke(1.dp, Color(0xFFD3DEEE)), RoundedCornerShape(16.dp))
      .padding(14.dp)
  ) {
    Text(
      text = subject.title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "${t("Kelas")}: ${subject.className}",
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      modifier = Modifier.padding(top = 8.dp)
    )
    Text(
      text = "${t("Semester")}: ${subject.semester}${if (subject.semesterActive) " (${t("Aktif")})" else ""}",
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      modifier = Modifier.padding(top = 2.dp)
    )
    Row(
      modifier = Modifier.padding(top = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      StatusPill("${t("Absen")} ${subject.attendancePending}", HighlightCard)
      StatusPill("${t("Nilai")} ${subject.scorePending}", WarmAccent)
      StatusPill("${t("Materi")} ${subject.materialCount}", SuccessTint)
    }
  }
}

@Composable
fun EmptyPlaceholderCard(message: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(CardGradientEnd)
      .border(
        BorderStroke(1.dp, CardBorder),
        RoundedCornerShape(14.dp)
      )
      .padding(14.dp)
  ) {
    Text(
      text = t(message),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
  }
}

@Composable
fun ProfileInfoRow(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .widthIn(min = 120.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(CardBackground)
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(14.dp))
      .padding(12.dp)
  ) {
    Text(
      text = t(label),
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(top = 6.dp)
    )
  }
}
