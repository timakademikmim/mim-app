package com.mim.guruapp.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuOpen
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mim.guruapp.GuruSidebarDestination
import com.mim.guruapp.GuruSidebarParent
import com.mim.guruapp.R
import com.mim.guruapp.ui.i18n.t
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk

data class SidebarLeafItem(
  val destination: GuruSidebarDestination,
  val label: String,
  val icon: ImageVector,
  val badge: Int? = null
)

data class SidebarParentItem(
  val parent: GuruSidebarParent,
  val label: String,
  val icon: ImageVector,
  val children: List<SidebarLeafItem>
)

data class SidebarProfileItem(
  val label: String,
  val icon: ImageVector,
  val badge: Int? = null,
  val destination: GuruSidebarDestination? = null,
  val isRoleSwitch: Boolean = false,
  val isLogout: Boolean = false
)

data class GuruSidebarContent(
  val topItems: List<SidebarLeafItem>,
  val parentItems: List<SidebarParentItem>,
  val bottomItems: List<SidebarLeafItem>,
  val profileItems: List<SidebarProfileItem>
)

@Composable
fun Sidebar(
  appName: String,
  appSubtitle: String,
  content: GuruSidebarContent,
  selectedDestination: GuruSidebarDestination,
  expandedParent: GuruSidebarParent?,
  onDismiss: () -> Unit,
  onToggleParent: (GuruSidebarParent) -> Unit,
  onSelectItem: (GuruSidebarDestination) -> Unit,
  onChangeRole: () -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxHeight()
      .width(304.dp)
      .statusBarsPadding()
      .navigationBarsPadding()
      .background(SoftPanel, RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
      .padding(horizontal = 16.dp, vertical = 18.dp)
  ) {
    SidebarHeader(
      appName = appName,
      appSubtitle = appSubtitle,
      onDismiss = onDismiss
    )

    Spacer(modifier = Modifier.height(18.dp))

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .verticalScroll(rememberScrollState())
      ) {
        SidebarSectionTitle("Main Menu")

        Column(
          modifier = Modifier.padding(top = 10.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          content.topItems.forEach { item ->
            SidebarLeafRow(
              item = item,
              selected = item.destination == selectedDestination,
              onClick = { onSelectItem(item.destination) }
            )
          }

          content.parentItems.forEach { item ->
            SidebarParentGroup(
              item = item,
              expanded = expandedParent == item.parent,
              selectedDestination = selectedDestination,
              onToggleParent = { onToggleParent(item.parent) },
              onSelectItem = onSelectItem
            )
          }

          content.bottomItems.forEach { item ->
            SidebarLeafRow(
              item = item,
              selected = item.destination == selectedDestination,
              onClick = { onSelectItem(item.destination) }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      HorizontalDivider(color = CardBorder, thickness = 1.dp)

      Spacer(modifier = Modifier.height(18.dp))

      SidebarSectionTitle("Profile")

      Column(
        modifier = Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        content.profileItems.forEach { item ->
          SidebarProfileRow(
            item = item,
            selected = item.destination == selectedDestination,
            onClick = {
              when {
                item.isLogout -> onLogout()
                item.isRoleSwitch -> onChangeRole()
                item.destination != null -> onSelectItem(item.destination)
              }
            }
          )
        }
      }
    }
  }
}

@Composable
private fun SidebarHeader(
  appName: String,
  appSubtitle: String,
  onDismiss: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .background(CardBackground, RoundedCornerShape(16.dp))
          .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          painter = painterResource(id = R.drawable.logo_mim_full),
          contentDescription = "Logo MIM",
          tint = Color.Unspecified,
          modifier = Modifier.size(32.dp)
        )
      }
      Column(modifier = Modifier.padding(start = 12.dp)) {
        Text(
          text = appName,
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = t(appSubtitle),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
    }

    Box(
      modifier = Modifier
        .size(38.dp)
        .background(CardBackground, CircleShape)
        .border(1.dp, CardBorder, CircleShape)
        .clickable(onClick = onDismiss),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Outlined.MenuOpen,
        contentDescription = "Tutup sidebar",
        tint = PrimaryBlueDark
      )
    }
  }
}

@Composable
private fun SidebarSectionTitle(title: String) {
  Text(
    text = t(title),
    style = MaterialTheme.typography.labelMedium,
    color = SubtleInk,
    fontWeight = FontWeight.Bold,
    modifier = Modifier.padding(horizontal = 6.dp)
  )
}

@Composable
private fun SidebarParentGroup(
  item: SidebarParentItem,
  expanded: Boolean,
  selectedDestination: GuruSidebarDestination,
  onToggleParent: () -> Unit,
  onSelectItem: (GuruSidebarDestination) -> Unit
) {
  Column(
    modifier = Modifier.animateContentSize()
  ) {
    SidebarParentRow(
      item = item,
      expanded = expanded,
      selected = expanded || item.children.any { it.destination == selectedDestination },
      onClick = onToggleParent
    )

    if (expanded) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 18.dp, top = 6.dp)
      ) {
        Box(
          modifier = Modifier
            .width(2.dp)
            .height((item.children.size * 52).dp)
            .background(CardBorder, RoundedCornerShape(999.dp))
        )
        Column(
          modifier = Modifier
            .weight(1f)
            .padding(start = 12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          item.children.forEach { child ->
            SidebarLeafRow(
              item = child,
              selected = child.destination == selectedDestination,
              compact = true,
              onClick = { onSelectItem(child.destination) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SidebarParentRow(
  item: SidebarParentItem,
  expanded: Boolean,
  selected: Boolean,
  onClick: () -> Unit
) {
  val background = if (selected) PrimaryBlue.copy(alpha = 0.14f) else Color.Transparent
  val borderColor = if (selected) PrimaryBlue.copy(alpha = 0.22f) else Color.Transparent
  val textColor = if (selected) PrimaryBlue else SubtleInk
  val iconColor = if (selected) PrimaryBlue else SubtleInk

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(background, RoundedCornerShape(16.dp))
      .border(1.dp, borderColor, RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 11.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .background(if (selected) CardBackground else SoftPanel, RoundedCornerShape(12.dp)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = item.icon,
        contentDescription = t(item.label),
        tint = iconColor,
        modifier = Modifier.size(18.dp)
      )
    }
    Text(
      text = t(item.label),
      style = MaterialTheme.typography.bodyMedium,
      color = textColor,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      modifier = Modifier
        .weight(1f)
        .padding(start = 12.dp)
    )
    Icon(
      imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
      contentDescription = if (expanded) t("Collapse") else t("Expand"),
      tint = iconColor,
      modifier = Modifier.size(18.dp)
    )
  }
}

@Composable
private fun SidebarLeafRow(
  item: SidebarLeafItem,
  selected: Boolean,
  onClick: () -> Unit,
  compact: Boolean = false
) {
  val background = if (selected) PrimaryBlue.copy(alpha = 0.14f) else Color.Transparent
  val borderColor = if (selected) PrimaryBlue.copy(alpha = 0.22f) else Color.Transparent
  val textColor = if (selected) PrimaryBlue else SubtleInk
  val iconColor = if (selected) PrimaryBlue else SubtleInk
  val verticalPadding = if (compact) 10.dp else 11.dp

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(background, RoundedCornerShape(16.dp))
      .border(1.dp, borderColor, RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = verticalPadding),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(if (compact) 30.dp else 34.dp)
        .background(if (selected) CardBackground else SoftPanel, RoundedCornerShape(12.dp)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = item.icon,
        contentDescription = t(item.label),
        tint = iconColor,
        modifier = Modifier.size(18.dp)
      )
    }
    Text(
      text = t(item.label),
      style = MaterialTheme.typography.bodyMedium,
      color = textColor,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      modifier = Modifier
        .weight(1f)
        .padding(start = 12.dp)
    )
    if (item.badge != null) {
      Badge(value = item.badge)
    }
  }
}

@Composable
private fun SidebarProfileRow(
  item: SidebarProfileItem,
  selected: Boolean,
  onClick: () -> Unit
) {
  val isDanger = item.isLogout
  val background = when {
    isDanger -> Color(0xFFFB7185).copy(alpha = 0.14f)
    selected -> PrimaryBlue.copy(alpha = 0.14f)
    else -> Color.Transparent
  }
  val borderColor = when {
    isDanger -> Color(0xFFFB7185).copy(alpha = 0.28f)
    selected -> PrimaryBlue.copy(alpha = 0.22f)
    else -> Color.Transparent
  }
  val textColor = when {
    isDanger -> Color(0xFFBE123C)
    selected -> PrimaryBlue
    else -> SubtleInk
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(background, RoundedCornerShape(16.dp))
      .border(1.dp, borderColor, RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 11.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .background(if (isDanger) Color(0xFFFB7185).copy(alpha = 0.16f) else SoftPanel, RoundedCornerShape(12.dp)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = item.icon,
        contentDescription = t(item.label),
        tint = textColor,
        modifier = Modifier.size(18.dp)
      )
    }
    Text(
      text = t(item.label),
      style = MaterialTheme.typography.bodyMedium,
      color = textColor,
      fontWeight = if (selected || isDanger) FontWeight.Bold else FontWeight.Medium,
      modifier = Modifier
        .weight(1f)
        .padding(start = 12.dp)
    )
    if (item.badge != null) {
      Badge(value = item.badge)
    }
  }
}

@Composable
fun Badge(value: Int) {
  Box(
    modifier = Modifier
      .background(PrimaryBlue.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
      .border(1.dp, PrimaryBlue.copy(alpha = 0.24f), RoundedCornerShape(999.dp))
      .padding(horizontal = 8.dp, vertical = 3.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = value.toString(),
      style = MaterialTheme.typography.labelSmall,
      color = PrimaryBlue,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun SidebarScrim(
  progress: Float,
  onDismiss: () -> Unit
) {
  if (progress <= 0.01f) return
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0x520F172A).copy(alpha = progress))
      .clickable(onClick = onDismiss)
  )
}

fun buildGuruSidebarContent(
  pendingSyncCount: Int,
  activeMapelCount: Int,
  isWaliKelas: Boolean,
  isWakasekKurikulum: Boolean,
  canChangeRole: Boolean = false
): GuruSidebarContent {
  return GuruSidebarContent(
    topItems = listOf(
      SidebarLeafItem(GuruSidebarDestination.Dashboard, "Dashboard", Icons.Outlined.Dashboard)
    ),
    parentItems = buildList {
      add(
        SidebarParentItem(
          parent = GuruSidebarParent.KinerjaGuru,
          label = "Kinerja Guru",
          icon = Icons.AutoMirrored.Outlined.Assignment,
          children = listOf(
            SidebarLeafItem(GuruSidebarDestination.Tugas, "Mutabaah", Icons.Outlined.TaskAlt)
          )
        )
      )
      add(
        SidebarParentItem(
          parent = GuruSidebarParent.Akademik,
          label = "Akademik",
          icon = Icons.Outlined.Book,
          children = listOf(
            SidebarLeafItem(GuruSidebarDestination.Jadwal, "Jadwal", Icons.Outlined.CalendarMonth),
            SidebarLeafItem(GuruSidebarDestination.Mapel, "Mapel", Icons.Outlined.Book, activeMapelCount.takeIf { it > 0 }),
            SidebarLeafItem(GuruSidebarDestination.Ujian, "Ujian", Icons.Outlined.Quiz)
          )
        )
      )
      add(
        SidebarParentItem(
          parent = GuruSidebarParent.AktivitasHarian,
          label = "Aktivitas Harian",
          icon = Icons.Outlined.NoteAlt,
          children = listOf(
            SidebarLeafItem(GuruSidebarDestination.InputAbsensi, "Input Absen", Icons.Outlined.AssignmentTurnedIn),
            SidebarLeafItem(GuruSidebarDestination.InputNilai, "Input Nilai", Icons.Outlined.NoteAlt),
            SidebarLeafItem(GuruSidebarDestination.Perizinan, "Perizinan", Icons.Outlined.TaskAlt)
          )
        )
      )
      if (isWaliKelas) {
        add(
          SidebarParentItem(
            parent = GuruSidebarParent.KelasSaya,
            label = "Kelas Saya",
            icon = Icons.Outlined.Groups,
            children = listOf(
              SidebarLeafItem(GuruSidebarDestination.Santri, "Santri", Icons.Outlined.Groups),
              SidebarLeafItem(GuruSidebarDestination.LaporanAbsensi, "Absensi", Icons.AutoMirrored.Outlined.FactCheck),
              SidebarLeafItem(GuruSidebarDestination.LaporanUTS, "Laporan UTS", Icons.Outlined.Description),
              SidebarLeafItem(GuruSidebarDestination.LaporanBulanan, "Laporan Bulanan", Icons.Outlined.Report, pendingSyncCount.takeIf { it > 0 }),
              SidebarLeafItem(GuruSidebarDestination.Rapor, "Rapor", Icons.Outlined.Grade)
            )
          )
        )
      }
      if (isWakasekKurikulum) {
        add(
          SidebarParentItem(
            parent = GuruSidebarParent.WakasekKurikulum,
            label = "Wakasek Akademik",
            icon = Icons.Outlined.AssignmentTurnedIn,
            children = listOf(
              SidebarLeafItem(GuruSidebarDestination.WakasekMonitoringGuru, "Monitoring Guru", Icons.Outlined.Dashboard),
              SidebarLeafItem(GuruSidebarDestination.WakasekMonitoringSiswa, "Monitoring Siswa", Icons.Outlined.Groups),
              SidebarLeafItem(GuruSidebarDestination.WakasekNilaiSiswa, "Nilai Siswa", Icons.Outlined.Grade),
              SidebarLeafItem(GuruSidebarDestination.WakasekPerizinan, "Perizinan", Icons.Outlined.TaskAlt)
            )
          )
        )
      }
    },
    bottomItems = emptyList(),
    profileItems = buildProfileItems(canChangeRole)
  )
}

fun buildAdminSidebarContent(
  canChangeRole: Boolean = false
): GuruSidebarContent {
  return GuruSidebarContent(
    topItems = listOf(
      SidebarLeafItem(GuruSidebarDestination.Dashboard, "Dashboard", Icons.Outlined.AdminPanelSettings)
    ),
    parentItems = listOf(
      SidebarParentItem(
        parent = GuruSidebarParent.AdminSekolah,
        label = "Sekolah",
        icon = Icons.Outlined.Book,
        children = listOf(
          SidebarLeafItem(GuruSidebarDestination.AdminProfilSekolah, "Profil Sekolah", Icons.Outlined.AccountCircle),
          SidebarLeafItem(GuruSidebarDestination.AdminKalenderAkademik, "Kalender Akademik", Icons.Outlined.CalendarMonth),
          SidebarLeafItem(GuruSidebarDestination.AdminKalenderTahunAjaran, "Tahun Ajaran", Icons.Outlined.CalendarMonth)
        )
      ),
      SidebarParentItem(
        parent = GuruSidebarParent.AdminAkademik,
        label = "Akademik",
        icon = Icons.Outlined.Book,
        children = listOf(
          SidebarLeafItem(GuruSidebarDestination.AdminKelas, "Kelas", Icons.Outlined.Dashboard),
          SidebarLeafItem(GuruSidebarDestination.AdminMapel, "Mapel", Icons.Outlined.Book),
          SidebarLeafItem(GuruSidebarDestination.AdminSantri, "Santri", Icons.Outlined.Groups),
          SidebarLeafItem(GuruSidebarDestination.AdminJadwalUjian, "Jadwal & Ujian", Icons.Outlined.Quiz),
          SidebarLeafItem(GuruSidebarDestination.AdminEkstrakurikuler, "Ekstrakurikuler", Icons.Outlined.Grade)
        )
      ),
      SidebarParentItem(
        parent = GuruSidebarParent.AdminTahfizAsrama,
        label = "Tahfiz & Asrama",
        icon = Icons.Outlined.Book,
        children = listOf(
          SidebarLeafItem(GuruSidebarDestination.AdminTahfiz, "Tahfiz", Icons.Outlined.Book),
          SidebarLeafItem(GuruSidebarDestination.AdminAsrama, "Asrama", Icons.Outlined.Groups)
        )
      ),
      SidebarParentItem(
        parent = GuruSidebarParent.AdminKaryawanSection,
        label = "Karyawan",
        icon = Icons.Outlined.Groups,
        children = listOf(
          SidebarLeafItem(GuruSidebarDestination.AdminKaryawan, "Data Karyawan", Icons.Outlined.Groups),
          SidebarLeafItem(GuruSidebarDestination.AdminPresensiIzin, "Presensi & Izin", Icons.AutoMirrored.Outlined.FactCheck),
          SidebarLeafItem(GuruSidebarDestination.AdminMutabaahKaryawan, "Mutabaah Karyawan", Icons.Outlined.TaskAlt)
        )
      )
    ),
    bottomItems = emptyList(),
    profileItems = buildProfileItems(canChangeRole)
  )
}

private fun buildProfileItems(canChangeRole: Boolean): List<SidebarProfileItem> {
  return buildList {
    add(SidebarProfileItem("Profil", Icons.Outlined.AccountCircle, destination = GuruSidebarDestination.Profil))
    if (canChangeRole) {
      add(SidebarProfileItem("Ganti Role", Icons.Outlined.SwapHoriz, isRoleSwitch = true))
    }
    add(SidebarProfileItem("Logout", Icons.AutoMirrored.Outlined.Logout, isLogout = true))
  }
}
