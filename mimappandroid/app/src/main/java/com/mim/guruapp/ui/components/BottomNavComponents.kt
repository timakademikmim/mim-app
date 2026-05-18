package com.mim.guruapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.GuruSidebarDestination
import com.mim.guruapp.defaultBottomNavShortcutDestinations
import com.mim.guruapp.ui.i18n.t
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SubtleInk
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class BottomNavEntry(
  val destination: GuruSidebarDestination,
  val label: String,
  val icon: ImageVector,
  val fullLabel: String = label
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomNavBar(
  items: List<BottomNavEntry>,
  selectedDestination: GuruSidebarDestination?,
  onSelect: (GuruSidebarDestination) -> Unit,
  onLongPress: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var animatedSelectedDestination by rememberSaveable {
    mutableStateOf(selectedDestination)
  }
  var pendingNavigationJob by androidx.compose.runtime.remember { mutableStateOf<Job?>(null) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(selectedDestination) {
    animatedSelectedDestination = selectedDestination
  }

  Row(
    modifier = modifier
      .width(356.dp)
      .shadow(18.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x180F172A), spotColor = Color(0x180F172A))
      .clip(RoundedCornerShape(32.dp))
      .background(CardBackground.copy(alpha = 0.96f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(32.dp))
      .combinedClickable(
        onClick = {},
        onLongClick = onLongPress
      )
      .padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    items.forEach { item ->
      val isSelected = animatedSelectedDestination == item.destination
      BottomNavItem(
        item = item,
        selected = isSelected,
        onLongPress = onLongPress,
        onClick = {
          if (animatedSelectedDestination != item.destination) {
            animatedSelectedDestination = item.destination
          }
          pendingNavigationJob?.cancel()
          pendingNavigationJob = scope.launch {
            delay(280)
            onSelect(item.destination)
          }
        },
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomNavItem(
  item: BottomNavEntry,
  selected: Boolean,
  onClick: () -> Unit,
  onLongPress: () -> Unit,
  modifier: Modifier = Modifier
) {
  val containerColor = if (selected) PrimaryBlue.copy(alpha = 0.12f) else Color.Transparent
  val contentColor = if (selected) PrimaryBlue else SubtleInk.copy(alpha = 0.82f)

  Column(
    modifier = modifier
      .height(58.dp)
      .clip(RoundedCornerShape(24.dp))
      .background(containerColor)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongPress
      )
      .padding(horizontal = 4.dp, vertical = 7.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(22.dp)
    ) {
      Icon(
        imageVector = item.icon,
        contentDescription = t(item.label),
        tint = contentColor,
        modifier = Modifier.size(21.dp)
      )
    }
    Text(
      text = t(item.label),
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) PrimaryBlueDark else contentColor,
      fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center
    )
  }
}

fun buildGuruBottomNavItems(): List<BottomNavEntry> {
  return buildGuruBottomNavItems(defaultBottomNavShortcutDestinations())
}

fun buildGuruBottomNavItems(destinations: List<GuruSidebarDestination>): List<BottomNavEntry> {
  return destinations
    .map { if (it == GuruSidebarDestination.InputNilai) GuruSidebarDestination.InputAbsensi else it }
    .distinct()
    .mapNotNull(::bottomNavEntryForDestination)
    .take(5)
}

fun buildAvailableBottomNavShortcutItems(
  isWaliKelas: Boolean,
  isWakasekKurikulum: Boolean
): List<BottomNavEntry> {
  val destinations = buildList {
    add(GuruSidebarDestination.Dashboard)
    add(GuruSidebarDestination.Tugas)
    add(GuruSidebarDestination.Jadwal)
    add(GuruSidebarDestination.Mapel)
    add(GuruSidebarDestination.Ujian)
    add(GuruSidebarDestination.InputAbsensi)
    add(GuruSidebarDestination.InputNilai)
    add(GuruSidebarDestination.Perizinan)
    if (isWaliKelas) {
      add(GuruSidebarDestination.Santri)
      add(GuruSidebarDestination.LaporanAbsensi)
      add(GuruSidebarDestination.LaporanUTS)
      add(GuruSidebarDestination.LaporanBulanan)
      add(GuruSidebarDestination.Rapor)
    }
    if (isWakasekKurikulum) {
      add(GuruSidebarDestination.WakasekMonitoringGuru)
      add(GuruSidebarDestination.WakasekMonitoringSiswa)
      add(GuruSidebarDestination.WakasekPerizinan)
    }
    add(GuruSidebarDestination.Profil)
  }
  return destinations.mapNotNull(::bottomNavEntryForDestination)
}

@Composable
fun BottomNavShortcutDialog(
  availableItems: List<BottomNavEntry>,
  selectedDestinations: List<GuruSidebarDestination>,
  onDismiss: () -> Unit,
  onSave: (List<GuruSidebarDestination>) -> Unit
) {
  var draft by rememberSaveable {
    mutableStateOf(selectedDestinations.normalizedShortcutDraft())
  }
  val maxItems = 5
  val distinctItems = availableItems.distinctBy { it.destination }
  val selectedItems = draft.mapNotNull { destination ->
    distinctItems.firstOrNull { it.destination == destination }
  }
  val unselectedItems = distinctItems.filterNot { item ->
    draft.contains(item.destination)
  }
  val orderedItems = selectedItems + unselectedItems
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(t("Atur Shortcut")) },
    text = {
      Column {
        Text(
          text = t("Dashboard selalu aktif. Pilih maksimal 5 item untuk ditampilkan di navigation bar."),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
        Column(
          modifier = Modifier
            .padding(top = 12.dp)
            .height(360.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          orderedItems.forEach { item ->
            val checked = draft.contains(item.destination)
            val locked = item.destination == GuruSidebarDestination.Dashboard
            val enabled = locked || checked || draft.size < maxItems
            val selectedIndex = draft.indexOf(item.destination)
            val canMoveUp = checked && !locked && selectedIndex > 1
            val canMoveDown = checked && !locked && selectedIndex in 1 until draft.lastIndex
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (checked) PrimaryBlue.copy(alpha = 0.12f) else Color.Transparent)
                .clickable(enabled = enabled && !locked) {
                  draft = if (checked) {
                    draft.filterNot { it == item.destination }.normalizedShortcutDraft()
                  } else {
                    (draft + item.destination).normalizedShortcutDraft().take(maxItems)
                  }
                }
                .padding(horizontal = 8.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = checked,
                enabled = enabled && !locked,
                onCheckedChange = {
                  draft = if (checked) {
                    draft.filterNot { it == item.destination }.normalizedShortcutDraft()
                  } else {
                    (draft + item.destination).normalizedShortcutDraft().take(maxItems)
                  }
                }
              )
              Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (checked) PrimaryBlue else SubtleInk,
                modifier = Modifier.size(21.dp)
              )
              Text(
                text = t(item.fullLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryBlueDark,
                fontWeight = if (checked) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                  .padding(start = 10.dp)
                  .weight(1f)
              )
              if (locked) {
                Text(
                  text = t("Tetap"),
                  style = MaterialTheme.typography.labelSmall,
                  color = SubtleInk
                )
              } else if (checked) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(2.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  IconButton(
                    onClick = {
                      draft = draft.moveShortcutItem(selectedIndex, selectedIndex - 1)
                    },
                    enabled = canMoveUp,
                    modifier = Modifier.size(32.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.KeyboardArrowUp,
                      contentDescription = t("Geser ke atas"),
                      tint = if (canMoveUp) PrimaryBlue else SubtleInk.copy(alpha = 0.42f),
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  IconButton(
                    onClick = {
                      draft = draft.moveShortcutItem(selectedIndex, selectedIndex + 1)
                    },
                    enabled = canMoveDown,
                    modifier = Modifier.size(32.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.KeyboardArrowDown,
                      contentDescription = t("Geser ke bawah"),
                      tint = if (canMoveDown) PrimaryBlue else SubtleInk.copy(alpha = 0.42f),
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { onSave(draft.normalizedShortcutDraft().take(maxItems)) }) {
        Text(t("Simpan"))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(t("Batal"))
      }
    }
  )
}

private fun List<GuruSidebarDestination>.normalizedShortcutDraft(): List<GuruSidebarDestination> {
  return (listOf(GuruSidebarDestination.Dashboard) + this)
    .filterNot { it == GuruSidebarDestination.Pesan || it == GuruSidebarDestination.Notifikasi }
    .map { if (it == GuruSidebarDestination.InputNilai) GuruSidebarDestination.InputAbsensi else it }
    .distinct()
}

private fun List<GuruSidebarDestination>.moveShortcutItem(
  fromIndex: Int,
  toIndex: Int
): List<GuruSidebarDestination> {
  if (fromIndex !in indices || toIndex !in indices) return this
  if (fromIndex == 0 || toIndex == 0 || fromIndex == toIndex) return this
  val mutable = toMutableList()
  val item = mutable.removeAt(fromIndex)
  mutable.add(toIndex, item)
  return mutable.normalizedShortcutDraft()
}

private fun bottomNavEntryForDestination(destination: GuruSidebarDestination): BottomNavEntry? {
  return when (destination) {
    GuruSidebarDestination.Dashboard -> BottomNavEntry(destination, "Dashboard", Icons.Outlined.DashboardCustomize)
    GuruSidebarDestination.Tugas -> BottomNavEntry(destination, "Mutabaah", Icons.Outlined.TaskAlt)
    GuruSidebarDestination.Perizinan -> BottomNavEntry(destination, "Izin", Icons.Outlined.TaskAlt, "Perizinan")
    GuruSidebarDestination.Jadwal -> BottomNavEntry(destination, "Jadwal", Icons.Outlined.Today)
    GuruSidebarDestination.Mapel -> BottomNavEntry(destination, "Mapel", Icons.AutoMirrored.Outlined.MenuBook)
    GuruSidebarDestination.Ujian -> BottomNavEntry(destination, "Ujian", Icons.Outlined.Quiz)
    GuruSidebarDestination.InputNilai -> BottomNavEntry(destination, "Nilai", Icons.Outlined.NoteAlt, "Input Nilai")
    GuruSidebarDestination.InputAbsensi -> BottomNavEntry(destination, "Input", Icons.Outlined.EditNote, "Input Absen")
    GuruSidebarDestination.LaporanAbsensi -> BottomNavEntry(destination, "Absensi", Icons.AutoMirrored.Outlined.FactCheck)
    GuruSidebarDestination.LaporanUTS -> BottomNavEntry(destination, "UTS", Icons.Outlined.Description, "Laporan UTS")
    GuruSidebarDestination.LaporanPekanan -> BottomNavEntry(destination, "Pekanan", Icons.Outlined.Report, "Laporan Pekanan")
    GuruSidebarDestination.LaporanBulanan -> BottomNavEntry(destination, "Bulanan", Icons.Outlined.Report, "Laporan Bulanan")
    GuruSidebarDestination.Rapor -> BottomNavEntry(destination, "Rapor", Icons.Outlined.Grade)
    GuruSidebarDestination.Santri -> BottomNavEntry(destination, "Santri", Icons.Outlined.Groups)
    GuruSidebarDestination.WakasekMonitoringGuru -> BottomNavEntry(destination, "Guru", Icons.Outlined.DashboardCustomize, "Monitoring Guru")
    GuruSidebarDestination.WakasekMonitoringSiswa -> BottomNavEntry(destination, "Siswa", Icons.Outlined.Groups, "Monitoring Siswa")
    GuruSidebarDestination.WakasekPerizinan -> BottomNavEntry(destination, "W-Izin", Icons.Outlined.AssignmentTurnedIn, "Perizinan Wakasek")
    GuruSidebarDestination.Profil -> BottomNavEntry(destination, "Profil", Icons.Outlined.PersonOutline)
    GuruSidebarDestination.Pesan,
    GuruSidebarDestination.Notifikasi -> null
  }
}
