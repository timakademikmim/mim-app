package com.mim.guruapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.remote.AdminLessonSlot
import com.mim.guruapp.data.remote.AdminTeachingAcademicYear
import com.mim.guruapp.data.remote.AdminTeachingClassOption
import com.mim.guruapp.data.remote.AdminTeachingScheduleDistribution
import com.mim.guruapp.data.remote.AdminTeachingScheduleLoadResult
import com.mim.guruapp.data.remote.AdminTeachingScheduleRow
import com.mim.guruapp.data.remote.AdminTeachingScheduleSaveResult
import com.mim.guruapp.data.remote.AdminTeachingScheduleSnapshot
import com.mim.guruapp.data.remote.AdminTeachingSemesterOption
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.CardGradientEnd
import com.mim.guruapp.ui.theme.CardGradientStart
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk
import com.mim.guruapp.ui.theme.SuccessTint
import com.mim.guruapp.ui.theme.WarmAccent
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AdminTeachingScheduleScreen(
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadSnapshot: suspend () -> AdminTeachingScheduleLoadResult,
  onSaveSchedule: suspend (AdminTeachingScheduleRow) -> AdminTeachingScheduleSaveResult,
  onDeleteSchedule: suspend (String) -> AdminTeachingScheduleSaveResult,
  onSaveLessonSlot: suspend (AdminLessonSlot) -> AdminTeachingScheduleSaveResult,
  onDeleteLessonSlot: suspend (String) -> AdminTeachingScheduleSaveResult,
  modifier: Modifier = Modifier
) {
  var snapshot by remember { mutableStateOf(AdminTeachingScheduleSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())) }
  var selectedTabName by rememberSaveable { mutableStateOf(AdminTeachingScheduleTab.Schedule.name) }
  var selectedYearId by rememberSaveable { mutableStateOf(AdminTeachingYearFilterUnsetKey) }
  var selectedSemesterId by rememberSaveable { mutableStateOf(AdminTeachingSemesterFilterUnsetKey) }
  var selectedClassId by rememberSaveable { mutableStateOf(AdminTeachingAllClassFilterKey) }
  var selectedDayKey by rememberSaveable { mutableStateOf(AdminTeachingAllDayFilterKey) }
  var query by rememberSaveable { mutableStateOf("") }
  var scheduleDraft by remember { mutableStateOf<AdminTeachingScheduleRow?>(null) }
  var slotDraft by remember { mutableStateOf<AdminLessonSlot?>(null) }
  var deleteScheduleTarget by remember { mutableStateOf<AdminTeachingScheduleRow?>(null) }
  var deleteSlotTarget by remember { mutableStateOf<AdminLessonSlot?>(null) }
  var isLoading by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  fun applySnapshot(nextSnapshot: AdminTeachingScheduleSnapshot) {
    snapshot = nextSnapshot
    val yearIds = nextSnapshot.academicYears.map { it.id }.toSet()
    selectedYearId = when {
      selectedYearId == AdminTeachingYearFilterUnsetKey ->
        nextSnapshot.activeAcademicYearId.ifBlank { nextSnapshot.academicYears.firstOrNull()?.id.orEmpty() }
      selectedYearId == AdminTeachingAllYearFilterKey -> AdminTeachingAllYearFilterKey
      selectedYearId in yearIds -> selectedYearId
      else -> nextSnapshot.activeAcademicYearId.ifBlank { nextSnapshot.academicYears.firstOrNull()?.id.orEmpty() }
    }
    val effectiveYear = selectedYearId.takeUnless { it == AdminTeachingAllYearFilterKey || it == AdminTeachingYearFilterUnsetKey }.orEmpty()
    val semesterOptions = nextSnapshot.semesters.filter { effectiveYear.isBlank() || it.academicYearId == effectiveYear }
    val semesterIds = semesterOptions.map { it.id }.toSet()
    selectedSemesterId = when {
      selectedSemesterId == AdminTeachingSemesterFilterUnsetKey ->
        semesterOptions.firstOrNull { it.active }?.id ?: semesterOptions.firstOrNull()?.id.orEmpty()
      selectedSemesterId == AdminTeachingAllSemesterFilterKey -> AdminTeachingAllSemesterFilterKey
      selectedSemesterId in semesterIds -> selectedSemesterId
      else -> semesterOptions.firstOrNull { it.active }?.id ?: semesterOptions.firstOrNull()?.id.orEmpty()
    }
    val classIds = nextSnapshot.classes.map { it.id }.toSet()
    if (selectedClassId != AdminTeachingAllClassFilterKey && selectedClassId !in classIds) {
      selectedClassId = AdminTeachingAllClassFilterKey
    }
  }

  fun loadSchedule(showGlobalRefresh: Boolean = false) {
    scope.launch {
      isLoading = true
      errorMessage = ""
      if (showGlobalRefresh) onRefresh()
      when (val result = onLoadSnapshot()) {
        is AdminTeachingScheduleLoadResult.Success -> applySnapshot(result.snapshot)
        is AdminTeachingScheduleLoadResult.Error -> errorMessage = result.message
      }
      isLoading = false
    }
  }

  LaunchedEffect(Unit) {
    loadSchedule()
  }

  if (scheduleDraft != null) {
    AdminTeachingScheduleDialog(
      draft = scheduleDraft ?: snapshot.newAdminTeachingScheduleDraft(selectedYearId, selectedSemesterId, selectedClassId, selectedDayKey),
      snapshot = snapshot,
      selectedYearId = selectedYearId,
      selectedSemesterId = selectedSemesterId,
      selectedClassId = selectedClassId,
      selectedDayKey = selectedDayKey,
      isSaving = isSaving,
      onDismiss = { if (!isSaving) scheduleDraft = null },
      onConfirm = { row ->
        scope.launch {
          isSaving = true
          when (val result = onSaveSchedule(row)) {
            is AdminTeachingScheduleSaveResult.Success -> {
              scheduleDraft = null
              applySnapshot(result.snapshot)
              snackbarHostState.showSnackbar(result.message)
            }
            is AdminTeachingScheduleSaveResult.Error -> snackbarHostState.showSnackbar(result.message)
          }
          isSaving = false
        }
      }
    )
  }

  if (slotDraft != null) {
    AdminLessonSlotDialog(
      draft = slotDraft ?: snapshot.newAdminLessonSlotDraft(selectedYearId),
      years = snapshot.academicYears,
      isSaving = isSaving,
      onDismiss = { if (!isSaving) slotDraft = null },
      onConfirm = { slot ->
        scope.launch {
          isSaving = true
          when (val result = onSaveLessonSlot(slot)) {
            is AdminTeachingScheduleSaveResult.Success -> {
              slotDraft = null
              applySnapshot(result.snapshot)
              snackbarHostState.showSnackbar(result.message)
            }
            is AdminTeachingScheduleSaveResult.Error -> snackbarHostState.showSnackbar(result.message)
          }
          isSaving = false
        }
      }
    )
  }

  if (deleteScheduleTarget != null) {
    val target = deleteScheduleTarget
    AlertDialog(
      onDismissRequest = { if (!isSaving) deleteScheduleTarget = null },
      title = { Text("Hapus Jadwal") },
      text = { Text("Yakin ingin menghapus jadwal ${target?.subjectName.orEmpty().ifBlank { "ini" }}?") },
      confirmButton = {
        Button(
          onClick = {
            val id = target?.rowId.orEmpty()
            scope.launch {
              isSaving = true
              when (val result = onDeleteSchedule(id)) {
                is AdminTeachingScheduleSaveResult.Success -> {
                  deleteScheduleTarget = null
                  applySnapshot(result.snapshot)
                  snackbarHostState.showSnackbar(result.message)
                }
                is AdminTeachingScheduleSaveResult.Error -> snackbarHostState.showSnackbar(result.message)
              }
              isSaving = false
            }
          },
          enabled = !isSaving
        ) {
          if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
          else Text("Hapus")
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteScheduleTarget = null }, enabled = !isSaving) { Text("Batal") }
      }
    )
  }

  if (deleteSlotTarget != null) {
    val target = deleteSlotTarget
    AlertDialog(
      onDismissRequest = { if (!isSaving) deleteSlotTarget = null },
      title = { Text("Hapus Jam") },
      text = { Text("Yakin ingin menghapus ${target?.name.orEmpty().ifBlank { "jam pelajaran ini" }}?") },
      confirmButton = {
        Button(
          onClick = {
            val id = target?.rowId.orEmpty()
            scope.launch {
              isSaving = true
              when (val result = onDeleteLessonSlot(id)) {
                is AdminTeachingScheduleSaveResult.Success -> {
                  deleteSlotTarget = null
                  applySnapshot(result.snapshot)
                  snackbarHostState.showSnackbar(result.message)
                }
                is AdminTeachingScheduleSaveResult.Error -> snackbarHostState.showSnackbar(result.message)
              }
              isSaving = false
            }
          },
          enabled = !isSaving
        ) {
          if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
          else Text("Hapus")
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteSlotTarget = null }, enabled = !isSaving) { Text("Batal") }
      }
    )
  }

  BackHandler(enabled = scheduleDraft != null || slotDraft != null || deleteScheduleTarget != null || deleteSlotTarget != null) {
    if (!isSaving) {
      scheduleDraft = null
      slotDraft = null
      deleteScheduleTarget = null
      deleteSlotTarget = null
    }
  }

  val selectedTab = runCatching { AdminTeachingScheduleTab.valueOf(selectedTabName) }.getOrDefault(AdminTeachingScheduleTab.Schedule)
  val effectiveYearId = selectedYearId
    .takeUnless { it == AdminTeachingYearFilterUnsetKey }
    ?: snapshot.activeAcademicYearId.ifBlank { snapshot.academicYears.firstOrNull()?.id.orEmpty() }
  val semesterOptionsForFilter = snapshot.semesters.filter {
    effectiveYearId == AdminTeachingAllYearFilterKey || it.academicYearId == effectiveYearId
  }
  val yearOptions = listOf(AdminTeachingAcademicYear(AdminTeachingAllYearFilterKey, "Semua tahun ajaran", false)) + snapshot.academicYears
  val semesterFilterOptions = listOf(AdminTeachingSemesterOption(AdminTeachingAllSemesterFilterKey, "Semua semester", "", "", false)) + semesterOptionsForFilter
  val classOptionsForFilter = snapshot.classes.filter {
    effectiveYearId == AdminTeachingAllYearFilterKey || it.academicYearId == effectiveYearId
  }
  val classFilterOptions = listOf(AdminTeachingClassOption(AdminTeachingAllClassFilterKey, "Semua kelas", "", "", "")) + classOptionsForFilter

  val filteredSchedules = snapshot.schedules
    .filter { row ->
      val yearMatches = effectiveYearId == AdminTeachingAllYearFilterKey || row.academicYearId == effectiveYearId
      val semesterMatches = selectedSemesterId == AdminTeachingSemesterFilterUnsetKey ||
        selectedSemesterId == AdminTeachingAllSemesterFilterKey ||
        row.semesterId == selectedSemesterId
      val classMatches = selectedClassId == AdminTeachingAllClassFilterKey || row.classId == selectedClassId
      val dayMatches = selectedDayKey == AdminTeachingAllDayFilterKey || row.day == selectedDayKey
      val needle = query.trim()
      val queryMatches = needle.isBlank() ||
        row.className.contains(needle, ignoreCase = true) ||
        row.subjectName.contains(needle, ignoreCase = true) ||
        row.teacherName.contains(needle, ignoreCase = true) ||
        row.semesterName.contains(needle, ignoreCase = true)
      yearMatches && semesterMatches && classMatches && dayMatches && queryMatches
    }
    .sortedWith(
      compareBy<AdminTeachingScheduleRow> { it.classLevel.adminTeachingLevelNumberForUi() ?: 999 }
        .thenBy { it.className.lowercase() }
        .thenBy { it.day.adminTeachingDayOrderForUi() }
        .thenBy { it.startTime }
        .thenBy { it.subjectName.lowercase() }
    )

  val filteredSlots = snapshot.lessonSlots
    .filter { slot ->
      val yearMatches = effectiveYearId == AdminTeachingAllYearFilterKey ||
        slot.academicYearId.isBlank() ||
        slot.academicYearId == effectiveYearId
      val needle = query.trim()
      val queryMatches = needle.isBlank() ||
        slot.name.contains(needle, ignoreCase = true) ||
        slot.startTime.contains(needle, ignoreCase = true) ||
        slot.endTime.contains(needle, ignoreCase = true)
      yearMatches && queryMatches
    }
    .sortedWith(compareBy<AdminLessonSlot> { it.order.toIntOrNull() ?: 999 }.thenBy { it.startTime })

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = AppBackground,
    contentWindowInsets = WindowInsets(0),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(AppBackground)
          .padding(horizontal = 16.dp)
      ) {
        AdminTeachingTopBar(
          onMenuClick = onMenuClick,
          title = "Jadwal Pelajaran",
          actionContentDescription = if (selectedTab == AdminTeachingScheduleTab.Schedule) "Tambah jadwal" else "Tambah jam pelajaran",
          onActionClick = {
            if (selectedTab == AdminTeachingScheduleTab.Schedule) {
              scheduleDraft = snapshot.newAdminTeachingScheduleDraft(selectedYearId, selectedSemesterId, selectedClassId, selectedDayKey)
            } else {
              slotDraft = snapshot.newAdminLessonSlotDraft(selectedYearId)
            }
          }
        )
        AdminTeachingSearchBar(query, onValueChange = { query = it })
      }
    }
  ) { innerPadding ->
    PullToRefreshBox(
      isRefreshing = isRefreshing || isLoading,
      onRefresh = { loadSchedule(showGlobalRefresh = true) },
      modifier = Modifier
        .padding(innerPadding)
        .fillMaxSize()
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          AdminTeachingSummaryCard(
            selectedYearLabel = yearOptions.firstOrNull { it.id == effectiveYearId }?.name ?: "-",
            selectedSemesterLabel = semesterFilterOptions.firstOrNull { it.id == selectedSemesterId }?.name ?: "-",
            scheduleCount = filteredSchedules.size,
            slotCount = filteredSlots.size
          )
        }
        item {
          AdminTeachingTabBar(
            selectedTab = selectedTab,
            onSelected = { selectedTabName = it.name }
          )
        }
        item {
          AdminTeachingFilterPanel(
            years = yearOptions,
            selectedYearId = effectiveYearId,
            onYearSelected = {
              selectedYearId = it
              selectedSemesterId = AdminTeachingSemesterFilterUnsetKey
              selectedClassId = AdminTeachingAllClassFilterKey
            },
            semesters = semesterFilterOptions,
            selectedSemesterId = selectedSemesterId,
            onSemesterSelected = { selectedSemesterId = it },
            classes = classFilterOptions,
            selectedClassId = selectedClassId,
            onClassSelected = { selectedClassId = it },
            selectedDayKey = selectedDayKey,
            onDaySelected = { selectedDayKey = it },
            showScheduleFilters = selectedTab == AdminTeachingScheduleTab.Schedule
          )
        }
        if (errorMessage.isNotBlank()) {
          item { AdminTeachingNoticeCard(errorMessage, Color(0xFFFB7185)) }
        }
        if (selectedTab == AdminTeachingScheduleTab.Schedule) {
          if (filteredSchedules.isEmpty()) {
            item { AdminTeachingNoticeCard("Belum ada jadwal pelajaran untuk filter ini.", WarmAccent) }
          } else {
            items(filteredSchedules, key = { it.rowId }) { row ->
              AdminTeachingScheduleCard(
                row = row,
                onEdit = { scheduleDraft = row },
                onDelete = { deleteScheduleTarget = row }
              )
            }
          }
        } else {
          if (filteredSlots.isEmpty()) {
            item { AdminTeachingNoticeCard("Belum ada jam pelajaran untuk tahun ajaran ini.", WarmAccent) }
          } else {
            items(filteredSlots, key = { it.rowId }) { slot ->
              AdminLessonSlotCard(
                slot = slot,
                onEdit = { slotDraft = slot },
                onDelete = { deleteSlotTarget = slot }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminTeachingTopBar(
  onMenuClick: () -> Unit,
  title: String,
  actionContentDescription: String,
  onActionClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 8.dp, bottom = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    AdminTeachingCircleButton(Icons.Outlined.Menu, "Buka menu", onMenuClick)
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Kelola jadwal kelas dan slot jam",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    AdminTeachingCircleButton(
      icon = Icons.Outlined.Add,
      contentDescription = actionContentDescription,
      onClick = onActionClick,
      background = PrimaryBlue,
      contentColor = Color.White
    )
  }
}

@Composable
private fun AdminTeachingCircleButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  background: Color = CardBackground,
  contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
  Box(
    modifier = Modifier
      .size(46.dp)
      .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
      .background(background, CircleShape)
      .border(BorderStroke(1.dp, CardBorder), CircleShape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(icon, contentDescription = contentDescription, tint = contentColor)
  }
}

@Composable
private fun AdminTeachingSearchBar(
  value: String,
  onValueChange: (String) -> Unit
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier.fillMaxWidth(),
    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
    placeholder = { Text("Cari kelas, mapel, guru, jam") },
    singleLine = true,
    shape = RoundedCornerShape(16.dp)
  )
}

@Composable
private fun AdminTeachingSummaryCard(
  selectedYearLabel: String,
  selectedSemesterLabel: String,
  scheduleCount: Int,
  slotCount: Int
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(22.dp), ambientColor = PrimaryBlue.copy(alpha = 0.10f), spotColor = PrimaryBlue.copy(alpha = 0.10f))
      .background(Brush.horizontalGradient(listOf(CardGradientStart, CardGradientEnd)), RoundedCornerShape(22.dp))
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(22.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .background(PrimaryBlue.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = PrimaryBlue)
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = selectedYearLabel.ifBlank { "-" },
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "Semester: ${selectedSemesterLabel.ifBlank { "-" }}",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      AdminTeachingInfoPill("$scheduleCount jadwal", SuccessTint)
      AdminTeachingInfoPill("$slotCount jam", WarmAccent.copy(alpha = 0.22f))
    }
  }
}

@Composable
private fun AdminTeachingTabBar(
  selectedTab: AdminTeachingScheduleTab,
  onSelected: (AdminTeachingScheduleTab) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(SoftPanel, RoundedCornerShape(18.dp))
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    AdminTeachingScheduleTab.values().forEach { tab ->
      val selected = selectedTab == tab
      Box(
        modifier = Modifier
          .weight(1f)
          .background(if (selected) PrimaryBlue else Color.Transparent, RoundedCornerShape(14.dp))
          .clickable { onSelected(tab) }
          .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = tab.label,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
private fun AdminTeachingFilterPanel(
  years: List<AdminTeachingAcademicYear>,
  selectedYearId: String,
  onYearSelected: (String) -> Unit,
  semesters: List<AdminTeachingSemesterOption>,
  selectedSemesterId: String,
  onSemesterSelected: (String) -> Unit,
  classes: List<AdminTeachingClassOption>,
  selectedClassId: String,
  onClassSelected: (String) -> Unit,
  selectedDayKey: String,
  onDaySelected: (String) -> Unit,
  showScheduleFilters: Boolean
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground, RoundedCornerShape(18.dp))
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(18.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    AdminTeachingOptionDropdown(
      label = "Tahun ajaran",
      selectedLabel = years.firstOrNull { it.id == selectedYearId }?.name ?: "Pilih tahun ajaran",
      options = years.map { it.id to (it.name + if (it.active) " (aktif)" else "") },
      onSelected = onYearSelected
    )
    if (showScheduleFilters) {
      AdminTeachingOptionDropdown(
        label = "Semester",
        selectedLabel = semesters.firstOrNull { it.id == selectedSemesterId }?.name ?: "Pilih semester",
        options = semesters.map { it.id to (it.name + if (it.active) " (aktif)" else "") },
        onSelected = onSemesterSelected
      )
      AdminTeachingOptionDropdown(
        label = "Kelas",
        selectedLabel = classes.firstOrNull { it.id == selectedClassId }?.name ?: "Semua kelas",
        options = classes.map { it.id to it.name },
        onSelected = onClassSelected
      )
      AdminTeachingOptionDropdown(
        label = "Hari",
        selectedLabel = AdminTeachingDayOptions.firstOrNull { it.first == selectedDayKey }?.second ?: "Semua hari",
        options = AdminTeachingDayOptions,
        onSelected = onDaySelected
      )
    }
  }
}

@Composable
private fun AdminTeachingScheduleCard(
  row: AdminTeachingScheduleRow,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground, RoundedCornerShape(18.dp))
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(18.dp))
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = row.subjectName.withAdminTeachingCategory(row.subjectCategory),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${row.className} - ${row.teacherName.ifBlank { "Belum diampu" }}",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      IconButton(onClick = onEdit) {
        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = PrimaryBlue)
      }
      IconButton(onClick = onDelete) {
        Icon(Icons.Outlined.Delete, contentDescription = "Hapus", tint = Color(0xFFDC2626))
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      AdminTeachingInfoPill(row.day.adminTeachingDayLabel(), SuccessTint)
      AdminTeachingInfoPill("${row.startTime}-${row.endTime}", SoftPanel)
      AdminTeachingInfoPill(row.semesterName, WarmAccent.copy(alpha = 0.22f))
    }
  }
}

@Composable
private fun AdminLessonSlotCard(
  slot: AdminLessonSlot,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground, RoundedCornerShape(18.dp))
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(18.dp))
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = slot.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${slot.startTime}-${slot.endTime}",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
      IconButton(onClick = onEdit) {
        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = PrimaryBlue)
      }
      IconButton(onClick = onDelete) {
        Icon(Icons.Outlined.Delete, contentDescription = "Hapus", tint = Color(0xFFDC2626))
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      if (slot.order.isNotBlank()) AdminTeachingInfoPill("Urutan ${slot.order}", SuccessTint)
      AdminTeachingInfoPill(slot.academicYearName.ifBlank { "Global" }, SoftPanel)
    }
  }
}

@Composable
private fun AdminTeachingScheduleDialog(
  draft: AdminTeachingScheduleRow,
  snapshot: AdminTeachingScheduleSnapshot,
  selectedYearId: String,
  selectedSemesterId: String,
  selectedClassId: String,
  selectedDayKey: String,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (AdminTeachingScheduleRow) -> Unit
) {
  var yearId by remember(draft.rowId) {
    mutableStateOf(draft.academicYearId.ifBlank { selectedYearId.takeUnless { it == AdminTeachingAllYearFilterKey || it == AdminTeachingYearFilterUnsetKey }.orEmpty().ifBlank { snapshot.activeAcademicYearId } })
  }
  var semesterId by remember(draft.rowId) {
    mutableStateOf(draft.semesterId.ifBlank { selectedSemesterId.takeUnless { it == AdminTeachingAllSemesterFilterKey || it == AdminTeachingSemesterFilterUnsetKey }.orEmpty().ifBlank { snapshot.activeSemesterId } })
  }
  var classId by remember(draft.rowId) {
    mutableStateOf(draft.classId.ifBlank { selectedClassId.takeUnless { it == AdminTeachingAllClassFilterKey }.orEmpty() })
  }
  var day by remember(draft.rowId) {
    mutableStateOf(draft.day.ifBlank { selectedDayKey.takeUnless { it == AdminTeachingAllDayFilterKey }.orEmpty() })
  }
  var distributionId by remember(draft.rowId) { mutableStateOf(draft.distributionId) }
  var slotId by remember(draft.rowId) { mutableStateOf(draft.lessonSlotId) }
  val scrollState = rememberScrollState()
  val yearOptions = snapshot.academicYears
  val semesterOptions = snapshot.semesters.filter { yearId.isBlank() || it.academicYearId == yearId }
  val classOptions = snapshot.classes.filter { yearId.isBlank() || it.academicYearId == yearId }
  if (semesterId.isBlank()) semesterId = semesterOptions.firstOrNull { it.active }?.id ?: semesterOptions.firstOrNull()?.id.orEmpty()
  if (classId.isBlank()) classId = classOptions.firstOrNull()?.id.orEmpty()
  val distributionOptions = snapshot.distributions.filter { item ->
    (yearId.isBlank() || item.academicYearId == yearId) &&
      (semesterId.isBlank() || item.semesterId == semesterId) &&
      (classId.isBlank() || item.classId == classId)
  }
  if (distributionId.isBlank()) distributionId = distributionOptions.firstOrNull()?.rowId.orEmpty()
  val selectedDistribution = snapshot.distributions.firstOrNull { it.rowId == distributionId }
  val slotOptions = snapshot.lessonSlots.filter { slot ->
    yearId.isBlank() || slot.academicYearId.isBlank() || slot.academicYearId == yearId
  }
  if (slotId.isBlank()) slotId = slotOptions.firstOrNull()?.rowId.orEmpty()
  val selectedSlot = snapshot.lessonSlots.firstOrNull { it.rowId == slotId }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (draft.rowId.isBlank()) "Tambah Jadwal" else "Edit Jadwal") },
    text = {
      Column(
        modifier = Modifier
          .heightIn(max = 560.dp)
          .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        AdminTeachingOptionDropdown(
          label = "Tahun ajaran",
          selectedLabel = yearOptions.firstOrNull { it.id == yearId }?.name ?: "Pilih tahun ajaran",
          options = yearOptions.map { it.id to (it.name + if (it.active) " (aktif)" else "") },
          onSelected = {
            yearId = it
            semesterId = snapshot.semesters.firstOrNull { semester -> semester.academicYearId == it && semester.active }?.id
              ?: snapshot.semesters.firstOrNull { semester -> semester.academicYearId == it }?.id.orEmpty()
            classId = snapshot.classes.firstOrNull { kelas -> kelas.academicYearId == it }?.id.orEmpty()
            distributionId = ""
            slotId = ""
          }
        )
        AdminTeachingOptionDropdown(
          label = "Semester",
          selectedLabel = semesterOptions.firstOrNull { it.id == semesterId }?.name ?: "Pilih semester",
          options = semesterOptions.map { it.id to (it.name + if (it.active) " (aktif)" else "") },
          onSelected = {
            semesterId = it
            distributionId = ""
          }
        )
        AdminTeachingOptionDropdown(
          label = "Hari",
          selectedLabel = AdminTeachingDayOptions.firstOrNull { it.first == day }?.second ?: "Pilih hari",
          options = AdminTeachingDayOptions.filterNot { it.first == AdminTeachingAllDayFilterKey },
          onSelected = { day = it }
        )
        AdminTeachingOptionDropdown(
          label = "Kelas",
          selectedLabel = classOptions.firstOrNull { it.id == classId }?.name ?: "Pilih kelas",
          options = classOptions.map { it.id to it.name },
          onSelected = {
            classId = it
            distributionId = ""
          }
        )
        AdminTeachingOptionDropdown(
          label = "Mapel dan guru",
          selectedLabel = selectedDistribution?.dialogLabel().orEmpty().ifBlank { "Pilih mapel" },
          options = distributionOptions.map { it.rowId to it.dialogLabel() },
          onSelected = { distributionId = it }
        )
        AdminTeachingOptionDropdown(
          label = "Jam pelajaran",
          selectedLabel = selectedSlot?.dialogLabel().orEmpty().ifBlank { "Pilih jam" },
          options = slotOptions.map { it.rowId to it.dialogLabel() },
          onSelected = { slotId = it }
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val distribution = snapshot.distributions.firstOrNull { it.rowId == distributionId }
          val slot = snapshot.lessonSlots.firstOrNull { it.rowId == slotId }
          onConfirm(
            draft.copy(
              distributionId = distributionId,
              day = day,
              lessonSlotId = slotId,
              startTime = slot?.startTime.orEmpty(),
              endTime = slot?.endTime.orEmpty(),
              classId = distribution?.classId.orEmpty(),
              className = distribution?.className.orEmpty(),
              classLevel = distribution?.classLevel.orEmpty(),
              subjectId = distribution?.subjectId.orEmpty(),
              subjectName = distribution?.subjectName.orEmpty(),
              subjectCategory = distribution?.subjectCategory.orEmpty(),
              teacherId = distribution?.teacherId.orEmpty(),
              teacherName = distribution?.teacherName.orEmpty(),
              semesterId = distribution?.semesterId.orEmpty(),
              semesterName = distribution?.semesterName.orEmpty(),
              academicYearId = distribution?.academicYearId.orEmpty(),
              academicYearName = distribution?.academicYearName.orEmpty()
            )
          )
        },
        enabled = !isSaving
      ) {
        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
        else Text("Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Batal") }
    }
  )
}

@Composable
private fun AdminLessonSlotDialog(
  draft: AdminLessonSlot,
  years: List<AdminTeachingAcademicYear>,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (AdminLessonSlot) -> Unit
) {
  var yearId by remember(draft.rowId) { mutableStateOf(draft.academicYearId.ifBlank { years.firstOrNull { it.active }?.id ?: years.firstOrNull()?.id.orEmpty() }) }
  var name by remember(draft.rowId) { mutableStateOf(draft.name) }
  var startTime by remember(draft.rowId) { mutableStateOf(draft.startTime) }
  var endTime by remember(draft.rowId) { mutableStateOf(draft.endTime) }
  var order by remember(draft.rowId) { mutableStateOf(draft.order) }
  val scrollState = rememberScrollState()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (draft.rowId.isBlank()) "Tambah Jam Pelajaran" else "Edit Jam Pelajaran") },
    text = {
      Column(
        modifier = Modifier
          .heightIn(max = 520.dp)
          .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        AdminTeachingOptionDropdown(
          label = "Tahun ajaran",
          selectedLabel = years.firstOrNull { it.id == yearId }?.name ?: "Pilih tahun ajaran",
          options = years.map { it.id to (it.name + if (it.active) " (aktif)" else "") },
          onSelected = { yearId = it }
        )
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Nama jam") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )
        OutlinedTextField(
          value = startTime,
          onValueChange = { startTime = it.take(5) },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Jam mulai (HH:mm)") },
          singleLine = true
        )
        OutlinedTextField(
          value = endTime,
          onValueChange = { endTime = it.take(5) },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Jam selesai (HH:mm)") },
          singleLine = true
        )
        OutlinedTextField(
          value = order,
          onValueChange = { order = it.filter(Char::isDigit).take(3) },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Urutan") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val year = years.firstOrNull { it.id == yearId }
          onConfirm(
            draft.copy(
              name = name.trim(),
              startTime = startTime.trim(),
              endTime = endTime.trim(),
              order = order.trim(),
              academicYearId = yearId,
              academicYearName = year?.name.orEmpty()
            )
          )
        },
        enabled = !isSaving
      ) {
        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
        else Text("Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Batal") }
    }
  )
}

@Composable
private fun AdminTeachingOptionDropdown(
  label: String,
  selectedLabel: String,
  options: List<Pair<String, String>>,
  onSelected: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = SubtleInk,
      fontWeight = FontWeight.SemiBold
    )
    Box {
      OutlinedButton(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
      ) {
        Text(
          text = selectedLabel.ifBlank { "-" },
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Icon(Icons.Outlined.ExpandMore, contentDescription = null)
      }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { (id, text) ->
          DropdownMenuItem(
            text = { Text(text.ifBlank { "-" }) },
            onClick = {
              expanded = false
              onSelected(id)
            }
          )
        }
      }
    }
  }
}

@Composable
private fun AdminTeachingInfoPill(
  text: String,
  background: Color
) {
  Box(
    modifier = Modifier
      .background(background, RoundedCornerShape(999.dp))
      .padding(horizontal = 10.dp, vertical = 5.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text.ifBlank { "-" },
      style = MaterialTheme.typography.labelSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun AdminTeachingNoticeCard(
  message: String,
  accent: Color
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(accent.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
      .border(BorderStroke(1.dp, accent.copy(alpha = 0.24f)), RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Icon(Icons.Outlined.Check, contentDescription = null, tint = accent)
    Text(
      text = message,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f)
    )
  }
}

private enum class AdminTeachingScheduleTab(val label: String) {
  Schedule("Jadwal"),
  Slots("Jam")
}

private const val AdminTeachingAllYearFilterKey = "__all_years__"
private const val AdminTeachingYearFilterUnsetKey = "__unset_year__"
private const val AdminTeachingAllSemesterFilterKey = "__all_semesters__"
private const val AdminTeachingSemesterFilterUnsetKey = "__unset_semester__"
private const val AdminTeachingAllClassFilterKey = "__all_classes__"
private const val AdminTeachingAllDayFilterKey = "__all_days__"

private val AdminTeachingDayOptions = listOf(
  AdminTeachingAllDayFilterKey to "Semua hari",
  "senin" to "Senin",
  "selasa" to "Selasa",
  "rabu" to "Rabu",
  "kamis" to "Kamis",
  "jumat" to "Jumat",
  "sabtu" to "Sabtu",
  "minggu" to "Minggu"
)

private fun AdminTeachingScheduleSnapshot.newAdminTeachingScheduleDraft(
  selectedYearId: String,
  selectedSemesterId: String,
  selectedClassId: String,
  selectedDayKey: String
): AdminTeachingScheduleRow {
  val yearId = selectedYearId.takeUnless { it == AdminTeachingAllYearFilterKey || it == AdminTeachingYearFilterUnsetKey }.orEmpty()
    .ifBlank { activeAcademicYearId }
  val semesterId = selectedSemesterId.takeUnless { it == AdminTeachingAllSemesterFilterKey || it == AdminTeachingSemesterFilterUnsetKey }.orEmpty()
    .ifBlank { activeSemesterId }
  val classId = selectedClassId.takeUnless { it == AdminTeachingAllClassFilterKey }.orEmpty()
  val distribution = distributions.firstOrNull {
    (yearId.isBlank() || it.academicYearId == yearId) &&
      (semesterId.isBlank() || it.semesterId == semesterId) &&
      (classId.isBlank() || it.classId == classId)
  }
  val slot = lessonSlots.firstOrNull { yearId.isBlank() || it.academicYearId.isBlank() || it.academicYearId == yearId }
  return AdminTeachingScheduleRow(
    rowId = "",
    distributionId = distribution?.rowId.orEmpty(),
    day = selectedDayKey.takeUnless { it == AdminTeachingAllDayFilterKey }.orEmpty(),
    lessonSlotId = slot?.rowId.orEmpty(),
    startTime = slot?.startTime.orEmpty(),
    endTime = slot?.endTime.orEmpty(),
    classId = distribution?.classId.orEmpty(),
    className = distribution?.className.orEmpty(),
    classLevel = distribution?.classLevel.orEmpty(),
    subjectId = distribution?.subjectId.orEmpty(),
    subjectName = distribution?.subjectName.orEmpty(),
    subjectCategory = distribution?.subjectCategory.orEmpty(),
    teacherId = distribution?.teacherId.orEmpty(),
    teacherName = distribution?.teacherName.orEmpty(),
    semesterId = distribution?.semesterId.orEmpty(),
    semesterName = distribution?.semesterName.orEmpty(),
    academicYearId = distribution?.academicYearId.orEmpty(),
    academicYearName = distribution?.academicYearName.orEmpty()
  )
}

private fun AdminTeachingScheduleSnapshot.newAdminLessonSlotDraft(selectedYearId: String): AdminLessonSlot {
  val yearId = selectedYearId.takeUnless { it == AdminTeachingAllYearFilterKey || it == AdminTeachingYearFilterUnsetKey }.orEmpty()
    .ifBlank { activeAcademicYearId }
  val order = ((lessonSlots.filter { it.academicYearId == yearId }.mapNotNull { it.order.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
  return AdminLessonSlot(
    rowId = "",
    name = "Jam $order",
    startTime = "",
    endTime = "",
    order = order,
    academicYearId = yearId,
    academicYearName = academicYears.firstOrNull { it.id == yearId }?.name.orEmpty()
  )
}

private fun AdminTeachingScheduleDistribution.dialogLabel(): String {
  val teacher = teacherName.ifBlank { "Belum diampu" }
  return "${subjectName.withAdminTeachingCategory(subjectCategory)} - $teacher"
}

private fun AdminLessonSlot.dialogLabel(): String {
  return "$name (${startTime.ifBlank { "-" }}-${endTime.ifBlank { "-" }})"
}

private fun String.withAdminTeachingCategory(category: String): String {
  val clean = trim().ifBlank { "-" }
  val cleanCategory = category.trim()
  return if (cleanCategory.isBlank()) clean else "$clean ($cleanCategory)"
}

private fun String.adminTeachingDayLabel(): String {
  return AdminTeachingDayOptions.firstOrNull { it.first == normalizedAdminTeachingDayForUi() }?.second ?: "-"
}

private fun String.normalizedAdminTeachingDayForUi(): String {
  return when (trim().lowercase()) {
    "senin", "monday", "mon", "1" -> "senin"
    "selasa", "tuesday", "tue", "2" -> "selasa"
    "rabu", "wednesday", "wed", "3" -> "rabu"
    "kamis", "thursday", "thu", "4" -> "kamis"
    "jumat", "jum'at", "friday", "fri", "5" -> "jumat"
    "sabtu", "saturday", "sat", "6" -> "sabtu"
    "minggu", "ahad", "sunday", "sun", "7" -> "minggu"
    else -> ""
  }
}

private fun String.adminTeachingDayOrderForUi(): Int {
  return when (normalizedAdminTeachingDayForUi()) {
    "senin" -> 1
    "selasa" -> 2
    "rabu" -> 3
    "kamis" -> 4
    "jumat" -> 5
    "sabtu" -> 6
    "minggu" -> 7
    else -> 99
  }
}

private fun String.adminTeachingLevelNumberForUi(): Int? {
  val lowered = trim().lowercase()
  lowered.toIntOrNull()?.let { return it }
  return when {
    lowered.contains("viii") -> 8
    lowered.contains("vii") -> 7
    lowered.contains("ix") -> 9
    lowered.contains("xii") -> 12
    lowered.contains("xi") -> 11
    lowered.contains("x") -> 10
    else -> Regex("\\d+").find(lowered)?.value?.toIntOrNull()
  }
}
