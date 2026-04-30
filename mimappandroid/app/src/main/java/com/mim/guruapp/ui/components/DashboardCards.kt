package com.mim.guruapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.mim.guruapp.data.model.NoticeItem
import com.mim.guruapp.data.model.QuickStat
import com.mim.guruapp.data.model.SubjectOverview
import com.mim.guruapp.data.model.SyncBannerState
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.CardGradientEnd
import com.mim.guruapp.ui.theme.CardGradientStart
import com.mim.guruapp.ui.theme.HighlightCard
import com.mim.guruapp.ui.theme.SuccessTint
import com.mim.guruapp.ui.theme.SubtleInk
import com.mim.guruapp.ui.theme.WarmAccent

@Composable
fun PlaceholderPanel(
  title: String,
  subtitle: String? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x1A0F172A), spotColor = Color(0x1A0F172A))
      .background(CardBackground, RoundedCornerShape(18.dp))
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(18.dp))
      .padding(16.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    if (!subtitle.isNullOrBlank()) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
      )
    } else {
      Box(modifier = Modifier.size(10.dp))
    }
    content()
  }
}

@Composable
fun SyncBannerCard(state: SyncBannerState) {
  if (state.message.isBlank()) return
  val tint = if (state.isSyncing) HighlightCard else SuccessTint
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(tint.copy(alpha = 0.10f))
      .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
      .padding(16.dp)
  ) {
    Text(
      text = if (state.isSyncing) "Sinkronisasi Berjalan" else "Data Lokal Aktif",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = state.message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 6.dp)
    )
  }
}

@Composable
fun QuickStatsRow(stats: List<QuickStat>) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    stats.forEach { stat ->
      Column(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(14.dp))
          .background(Color.White)
          .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
          .padding(horizontal = 12.dp, vertical = 10.dp)
      ) {
        Text(
          text = stat.label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = stat.value,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
fun AgendaCard(notice: NoticeItem, accent: Color) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(Color.White, Color(0xFFF8FAFC))
        )
      )
      .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
      .padding(14.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(accent)
      )
      Text(
        text = notice.title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp)
      )
    }
    Text(
      text = notice.body,
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      modifier = Modifier.padding(top = 8.dp)
    )
  }
}

@Composable
fun SubjectCard(subject: SubjectOverview) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
      .background(
        Brush.linearGradient(
          colors = listOf(CardGradientStart, CardGradientEnd)
        ),
        RoundedCornerShape(16.dp)
      )
      .border(1.dp, Color(0xFFD3DEEE), RoundedCornerShape(16.dp))
      .padding(14.dp)
  ) {
    Text(
      text = subject.title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = "Kelas: ${subject.className}",
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      modifier = Modifier.padding(top = 6.dp)
    )
    Text(
      text = "Semester: ${subject.semester}",
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      modifier = Modifier.padding(top = 2.dp)
    )

    Row(
      modifier = Modifier.padding(top = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      StatusPill(
        text = "Absen ${subject.attendancePending}",
        tone = HighlightCard
      )
      StatusPill(
        text = "Nilai ${subject.scorePending}",
        tone = WarmAccent
      )
      StatusPill(
        text = "Materi ${subject.materialCount}",
        tone = SuccessTint
      )
    }
  }
}

@Composable
fun NoticeCard(notice: NoticeItem) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color.White)
      .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
      .padding(14.dp)
  ) {
    Text(
      text = notice.title,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = notice.body,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 6.dp)
    )
  }
}

@Composable
fun StatusPill(text: String, tone: Color) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelMedium,
    color = tone,
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(tone.copy(alpha = 0.12f))
      .padding(horizontal = 10.dp, vertical = 7.dp)
  )
}
