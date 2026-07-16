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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.remote.AdminKelas
import com.mim.guruapp.data.remote.AdminKelasAcademicYear
import com.mim.guruapp.data.remote.AdminKelasAssignStudentsResult
import com.mim.guruapp.data.remote.AdminKelasLoadResult
import com.mim.guruapp.data.remote.AdminKelasSaveResult
import com.mim.guruapp.data.remote.AdminKelasSnapshot
import com.mim.guruapp.data.remote.AdminKelasStudentOption
import com.mim.guruapp.data.remote.AdminKelasTeacherOption
import com.mim.guruapp.ui.i18n.t
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
fun AdminKelasScreen(
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadKelas: suspend () -> AdminKelasLoadResult,
  onSaveKelas: suspend (AdminKelas) -> AdminKelasSaveResult,
  onAssignStudents: suspend (AdminKelas, List<String>) -> AdminKelasAssignStudentsResult,
  modifier: Modifier = Modifier
) {
  var snapshot by remember { mutableStateOf(AdminKelasSnapshot(emptyList(), emptyList(), emptyList(), emptyList())) }
  var selectedKelas by remember { mutableStateOf<AdminKelas?>(null) }
  var query by rememberSaveable { mutableStateOf("") }
  var selectedAcademicYearFilterId by rememberSaveable { mutableStateOf(AdminKelasYearFilterUnsetKey) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  fun loadClasses(showGlobalRefresh: Boolean = false) {
    scope.launch {
      isLoading = true
      if (showGlobalRefresh) onRefresh()
      when (val result = onLoadKelas()) {
        is AdminKelasLoadResult.Success -> {
          snapshot = result.snapshot
          val availableYearIds = result.snapshot.academicYears.map { it.id }.toSet()
          selectedAcademicYearFilterId = when {
            selectedAcademicYearFilterId == AdminKelasYearFilterUnsetKey ->
              result.snapshot.activeAcademicYearId.ifBlank { AdminKelasAllYearFilterKey }
            selectedAcademicYearFilterId == AdminKelasAllYearFilterKey ->
              AdminKelasAllYearFilterKey
            selectedAcademicYearFilterId in availableYearIds ->
              selectedAcademicYearFilterId
            else ->
              result.snapshot.activeAcademicYearId.ifBlank { AdminKelasAllYearFilterKey }
          }
          errorMessage = ""
          val opened = selectedKelas
          if (opened != null && opened.rowId.isNotBlank()) {
            selectedKelas = result.snapshot.classes.firstOrNull { it.rowId == opened.rowId } ?: opened
          }
        }
        is AdminKelasLoadResult.Error -> errorMessage = result.message
      }
      isLoading = false
    }
  }

  LaunchedEffect(Unit) {
    loadClasses()
  }

  if (selectedKelas != null) {
    BackHandler {
      selectedKelas = null
    }
    AdminKelasDetailContent(
      kelas = selectedKelas ?: snapshot.newClassDraft(),
      snapshot = snapshot,
      isRefreshing = isRefreshing || isLoading,
      onBackClick = { selectedKelas = null },
      onSaveKelas = onSaveKelas,
      onAssignStudents = onAssignStudents,
      onReload = { loadClasses() },
      onKelasSaved = { selectedKelas = it },
      snackbarHostState = snackbarHostState,
      modifier = modifier
    )
    return
  }

  val effectiveYearFilterId = selectedAcademicYearFilterId
    .takeUnless { it == AdminKelasYearFilterUnsetKey }
    ?: snapshot.activeAcademicYearId.ifBlank { AdminKelasAllYearFilterKey }
  val filteredClasses = snapshot.classes
    .filter { kelas ->
      effectiveYearFilterId == AdminKelasAllYearFilterKey || kelas.academicYearId == effectiveYearFilterId
    }
    .filter { kelas ->
      val needle = query.trim()
      needle.isBlank() ||
        kelas.name.contains(needle, ignoreCase = true) ||
        kelas.level.contains(needle, ignoreCase = true) ||
        kelas.academicYearName.contains(needle, ignoreCase = true) ||
        kelas.homeroomTeacherName.contains(needle, ignoreCase = true)
    }
    .sortedWith(
      compareBy<AdminKelas> { if (it.academicYearId == snapshot.activeAcademicYearId) 0 else 1 }
        .thenByDescending { it.academicYearName.firstYearForAdminKelasUi() ?: 0 }
        .thenBy { it.academicYearName.ifBlank { "zzzz" }.lowercase() }
        .thenBy { it.level.filter { char -> char.isDigit() }.toIntOrNull() ?: 999 }
        .thenBy { it.name.lowercase() }
    )

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      AdminKelasTopBar(
        title = "Kelas",
        isDetail = false,
        onMenuClick = onMenuClick,
        onBackClick = {},
        actionIcon = Icons.Outlined.Add,
        actionContentDescription = "Tambah kelas",
        onActionClick = { selectedKelas = snapshot.newClassDraft() }
      )
      AdminKelasSearchBar(
        value = query,
        onValueChange = { query = it }
      )
      AdminKelasYearFilterBar(
        selectedYearId = effectiveYearFilterId,
        years = snapshot.academicYears,
        activeAcademicYearId = snapshot.activeAcademicYearId,
        resultCount = filteredClasses.size,
        onYearSelected = { selectedAcademicYearFilterId = it }
      )
      PullToRefreshBox(
        isRefreshing = isRefreshing || isLoading,
        onRefresh = { loadClasses(showGlobalRefresh = true) },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .navigationBarsPadding()
      ) {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(bottom = 112.dp)
        ) {
          if (snapshot.activeAcademicYearName.isNotBlank()) {
            item {
              AdminKelasNoticeCard("Tahun ajaran aktif: ${snapshot.activeAcademicYearName}", tone = PrimaryBlue)
            }
          }
          if (errorMessage.isNotBlank()) {
            item {
              AdminKelasNoticeCard(errorMessage, tone = Color(0xFFFB7185))
            }
          }
          if (isLoading && snapshot.classes.isEmpty()) {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(180.dp),
                contentAlignment = Alignment.Center
              ) {
                CircularProgressIndicator()
              }
            }
          } else if (filteredClasses.isEmpty()) {
            item {
              EmptyPlaceholderCard(
                if (query.isBlank()) "Belum ada data kelas untuk tahun ajaran ini." else "Tidak ada kelas yang cocok dengan pencarian."
              )
            }
          } else {
            items(filteredClasses, key = { it.rowId }) { kelas ->
              AdminKelasCard(
                kelas = kelas,
                onClick = { selectedKelas = kelas }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AdminKelasDetailContent(
  kelas: AdminKelas,
  snapshot: AdminKelasSnapshot,
  isRefreshing: Boolean,
  onBackClick: () -> Unit,
  onSaveKelas: suspend (AdminKelas) -> AdminKelasSaveResult,
  onAssignStudents: suspend (AdminKelas, List<String>) -> AdminKelasAssignStudentsResult,
  onReload: () -> Unit,
  onKelasSaved: (AdminKelas) -> Unit,
  snackbarHostState: SnackbarHostState,
  modifier: Modifier = Modifier
) {
  var name by rememberSaveable(kelas.rowId, kelas.name) { mutableStateOf(kelas.name) }
  var level by rememberSaveable(kelas.rowId, kelas.level) { mutableStateOf(kelas.level) }
  var academicYearId by rememberSaveable(kelas.rowId, kelas.academicYearId) { mutableStateOf(kelas.academicYearId) }
  var homeroomTeacherId by rememberSaveable(kelas.rowId, kelas.homeroomTeacherId) { mutableStateOf(kelas.homeroomTeacherId) }
  var isSaving by remember { mutableStateOf(false) }
  var isAssignDialogOpen by rememberSaveable { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val selectedYear = snapshot.academicYears.firstOrNull { it.id == academicYearId }
  val selectedTeacher = snapshot.teachers.firstOrNull { it.id == homeroomTeacherId || it.employeeId == homeroomTeacherId }
  val draft = kelas.copy(
    name = name,
    level = level,
    academicYearId = academicYearId,
    academicYearName = selectedYear?.name ?: kelas.academicYearName,
    homeroomTeacherId = homeroomTeacherId,
    homeroomTeacherName = selectedTeacher?.name ?: kelas.homeroomTeacherName,
    studentCount = snapshot.students.count { it.classId == kelas.rowId && it.isActiveForAdminKelasUi() }
  )
  val currentStudents = snapshot.students
    .filter { it.classId == kelas.rowId && it.isActiveForAdminKelasUi() }
    .sortedBy { it.name.lowercase() }

  if (isAssignDialogOpen) {
    AdminKelasAssignStudentsDialog(
      kelas = draft,
      students = snapshot.students,
      isSaving = isSaving,
      onDismiss = { if (!isSaving) isAssignDialogOpen = false },
      onConfirm = { selectedIds ->
        scope.launch {
          isSaving = true
          when (val result = onAssignStudents(draft, selectedIds)) {
            is AdminKelasAssignStudentsResult.Success -> {
              snackbarHostState.showSnackbar(result.message)
              isAssignDialogOpen = false
              onReload()
            }
            is AdminKelasAssignStudentsResult.Error -> snackbarHostState.showSnackbar(result.message)
          }
          isSaving = false
        }
      }
    )
  }

  BackHandler(enabled = !isSaving) {
    onBackClick()
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      AdminKelasTopBar(
        title = if (kelas.rowId.isBlank()) "Tambah Kelas" else kelas.name,
        isDetail = true,
        onMenuClick = {},
        onBackClick = onBackClick,
        actionIcon = Icons.Outlined.Check,
        actionContentDescription = "Simpan kelas",
        actionEnabled = !isSaving,
        onActionClick = {
          scope.launch {
            isSaving = true
            when (val result = onSaveKelas(draft)) {
              is AdminKelasSaveResult.Success -> {
                val saved = result.kelas.copy(
                  academicYearName = selectedYear?.name ?: result.kelas.academicYearName,
                  homeroomTeacherName = selectedTeacher?.name ?: result.kelas.homeroomTeacherName
                )
                onKelasSaved(saved)
                snackbarHostState.showSnackbar(result.message)
                onReload()
              }
              is AdminKelasSaveResult.Error -> snackbarHostState.showSnackbar(result.message)
            }
            isSaving = false
          }
        }
      )
      PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onReload,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .navigationBarsPadding()
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 112.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          PlaceholderPanel(title = "Detail Kelas") {
            AdminKelasTextField(
              label = "Nama kelas",
              value = name,
              onValueChange = { name = it }
            )
            AdminKelasTextField(
              label = "Tingkat",
              value = level,
              onValueChange = { level = it }
            )
            AdminKelasAcademicYearDropdown(
              value = selectedYear?.name ?: "Pilih tahun ajaran",
              options = snapshot.academicYears,
              onOptionSelected = { year -> academicYearId = year.id }
            )
            AdminKelasTeacherDropdown(
              value = selectedTeacher?.name ?: "Belum dipilih",
              options = snapshot.teachers,
              onOptionSelected = { teacher -> homeroomTeacherId = teacher?.id.orEmpty() }
            )
          }

          PlaceholderPanel(title = "Santri di Kelas") {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              AdminKelasInfoPill("${currentStudents.size} santri", SuccessTint, Modifier.weight(1f))
              OutlinedButton(
                onClick = { isAssignDialogOpen = true },
                enabled = kelas.rowId.isNotBlank() && !isSaving
              ) {
                Icon(
                  imageVector = Icons.Outlined.GroupAdd,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(t("Tambah Santri"))
              }
            }
            if (kelas.rowId.isBlank()) {
              AdminKelasNoticeCard("Simpan kelas dulu sebelum menambahkan santri.", tone = WarmAccent)
            } else if (currentStudents.isEmpty()) {
              EmptyPlaceholderCard("Belum ada santri aktif di kelas ini.")
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                currentStudents.forEach { student ->
                  AdminKelasStudentRow(student)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminKelasAssignStudentsDialog(
  kelas: AdminKelas,
  students: List<AdminKelasStudentOption>,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (List<String>) -> Unit
) {
  var query by rememberSaveable { mutableStateOf("") }
  var filterMode by rememberSaveable { mutableStateOf(AdminKelasStudentFilter.All.name) }
  var selectedIds by remember { mutableStateOf<List<String>>(emptyList()) }
  val mode = runCatching { AdminKelasStudentFilter.valueOf(filterMode) }
    .getOrDefault(AdminKelasStudentFilter.All)
  val candidates = students
    .filter { it.rowId.isNotBlank() && it.classId != kelas.rowId && it.isActiveForAdminKelasUi() }
    .filter { student ->
      mode == AdminKelasStudentFilter.All ||
        kelas.level.isBlank() ||
        student.classLevel == kelas.level
    }
    .filter { student ->
      query.isBlank() ||
        student.name.contains(query, ignoreCase = true) ||
        student.nisn.contains(query, ignoreCase = true) ||
        student.className.contains(query, ignoreCase = true)
    }
    .sortedWith(
      compareBy<AdminKelasStudentOption> { it.className.ifBlank { "zzzz" }.lowercase() }
        .thenBy { it.name.lowercase() }
    )
  val selectedSet = selectedIds.toSet()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(t("Tambah Santri ke Kelas")) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = t("Pilih santri yang akan dimasukkan ke ${kelas.name}. Data santri tidak digandakan, hanya kelas aktifnya yang dipindahkan."),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
        AdminKelasSearchBar(
          value = query,
          onValueChange = { query = it },
          placeholder = "Cari santri"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          AdminKelasFilterChip(
            label = "Semua santri",
            selected = mode == AdminKelasStudentFilter.All,
            onClick = { filterMode = AdminKelasStudentFilter.All.name }
          )
          AdminKelasFilterChip(
            label = "Tingkatan sama",
            selected = mode == AdminKelasStudentFilter.SameLevel,
            onClick = { filterMode = AdminKelasStudentFilter.SameLevel.name }
          )
        }
        if (candidates.isEmpty()) {
          EmptyPlaceholderCard("Tidak ada santri yang bisa dipilih.")
        } else {
          LazyColumn(
            modifier = Modifier.heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(candidates, key = { it.rowId }) { student ->
              val selected = student.rowId in selectedSet
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(SoftPanel.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
                  .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                  .clickable(enabled = !isSaving) {
                    selectedIds = if (selected) {
                      selectedIds - student.rowId
                    } else {
                      selectedIds + student.rowId
                    }
                  }
                  .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = selected,
                  onCheckedChange = {
                    selectedIds = if (selected) {
                      selectedIds - student.rowId
                    } else {
                      selectedIds + student.rowId
                    }
                  },
                  enabled = !isSaving
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = student.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = listOf(student.className.ifBlank { "Belum ada kelas" }, student.nisn)
                      .filter { it.isNotBlank() }
                      .joinToString(" - "),
                    style = MaterialTheme.typography.bodySmall,
                    color = SubtleInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(selectedIds) },
        enabled = !isSaving && selectedIds.isNotEmpty()
      ) {
        Text(t(if (isSaving) "Menyimpan..." else "Masukkan"))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isSaving) {
        Text(t("Batal"))
      }
    }
  )
}

@Composable
private fun AdminKelasCard(
  kelas: AdminKelas,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x1A0F172A), spotColor = Color(0x1A0F172A))
      .background(
        brush = Brush.linearGradient(listOf(CardGradientStart, CardGradientEnd)),
        shape = RoundedCornerShape(18.dp)
      )
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = kelas.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = PrimaryBlueDark,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = listOf(kelas.academicYearName, kelas.homeroomTeacherName)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
            .ifBlank { "Detail kelas belum lengkap" },
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      AdminKelasInfoPill("${kelas.studentCount} santri", SuccessTint)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      if (kelas.level.isNotBlank()) {
        AdminKelasInfoPill("Tingkat ${kelas.level}", PrimaryBlue)
      }
      if (kelas.homeroomTeacherName.isBlank()) {
        AdminKelasInfoPill("Wali kelas kosong", WarmAccent)
      }
    }
  }
}

@Composable
private fun AdminKelasStudentRow(student: AdminKelasStudentOption) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(SoftPanel.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = student.name,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (student.nisn.isNotBlank()) {
        Text(
          text = student.nisn,
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun AdminKelasTopBar(
  title: String,
  isDetail: Boolean,
  onMenuClick: () -> Unit,
  onBackClick: () -> Unit,
  onActionClick: () -> Unit,
  modifier: Modifier = Modifier,
  actionIcon: ImageVector,
  actionContentDescription: String,
  actionEnabled: Boolean = true
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    AdminKelasTopButton(
      icon = if (isDetail) Icons.AutoMirrored.Outlined.ArrowBack else Icons.Outlined.Menu,
      contentDescription = if (isDetail) "Kembali" else "Buka sidebar",
      onClick = if (isDetail) onBackClick else onMenuClick
    )
    Text(
      text = t(title),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 12.dp)
    )
    AdminKelasTopButton(
      icon = actionIcon,
      contentDescription = actionContentDescription,
      onClick = onActionClick,
      enabled = actionEnabled
    )
  }
}

@Composable
private fun AdminKelasTopButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  enabled: Boolean = true
) {
  Box(
    modifier = Modifier
      .size(42.dp)
      .background(CardBackground.copy(alpha = 0.86f), CircleShape)
      .border(1.dp, CardBorder, CircleShape)
      .clickable(enabled = enabled, onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = if (enabled) PrimaryBlueDark else SubtleInk,
      modifier = Modifier.size(22.dp)
    )
  }
}

@Composable
private fun AdminKelasSearchBar(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String = "Cari kelas"
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    leadingIcon = {
      Icon(
        imageVector = Icons.Outlined.Search,
        contentDescription = null
      )
    },
    placeholder = { Text(t(placeholder)) },
    shape = RoundedCornerShape(18.dp)
  )
}

@Composable
private fun AdminKelasYearFilterBar(
  selectedYearId: String,
  years: List<AdminKelasAcademicYear>,
  activeAcademicYearId: String,
  resultCount: Int,
  onYearSelected: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  val sortedYears = years.sortedWith(
    compareBy<AdminKelasAcademicYear> { if (it.id == activeAcademicYearId) 0 else 1 }
      .thenByDescending { it.name.firstYearForAdminKelasUi() ?: 0 }
      .thenBy { it.name.lowercase() }
  )
  val selectedYear = years.firstOrNull { it.id == selectedYearId }
  val selectedLabel = when {
    selectedYearId == AdminKelasAllYearFilterKey -> "Semua tahun ajaran"
    selectedYear != null && selectedYear.active -> "${selectedYear.name} (aktif)"
    selectedYear != null -> selectedYear.name
    activeAcademicYearId.isNotBlank() -> years.firstOrNull { it.id == activeAcademicYearId }?.name
      ?.let { "$it (aktif)" }
      ?: "Pilih tahun ajaran"
    else -> "Pilih tahun ajaran"
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(modifier = Modifier.weight(1f)) {
      OutlinedButton(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = t(selectedLabel),
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Icon(
          imageVector = Icons.Outlined.ExpandMore,
          contentDescription = null
        )
      }
      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
      ) {
        if (years.isNotEmpty()) {
          DropdownMenuItem(
            text = { Text(t("Semua tahun ajaran")) },
            onClick = {
              onYearSelected(AdminKelasAllYearFilterKey)
              expanded = false
            }
          )
        }
        sortedYears.forEach { year ->
          val label = if (year.active) "${year.name} (aktif)" else year.name
          DropdownMenuItem(
            text = { Text(t(label)) },
            onClick = {
              onYearSelected(year.id)
              expanded = false
            }
          )
        }
      }
    }
    AdminKelasInfoPill("$resultCount kelas", PrimaryBlue)
  }
}

@Composable
private fun AdminKelasTextField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    label = { Text(t(label)) },
    shape = RoundedCornerShape(14.dp)
  )
}

@Composable
private fun AdminKelasAcademicYearDropdown(
  value: String,
  options: List<AdminKelasAcademicYear>,
  onOptionSelected: (AdminKelasAcademicYear) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = t("Tahun ajaran"),
      style = MaterialTheme.typography.labelMedium,
      color = SubtleInk,
      fontWeight = FontWeight.SemiBold
    )
    Box {
      OutlinedButton(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = t(value),
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Icon(
          imageVector = Icons.Outlined.ExpandMore,
          contentDescription = null
        )
      }
      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
      ) {
        options.forEach { option ->
          val label = if (option.active) "${option.name} (aktif)" else option.name
          DropdownMenuItem(
            text = { Text(t(label)) },
            onClick = {
              onOptionSelected(option)
              expanded = false
            }
          )
        }
      }
    }
  }
}

@Composable
private fun AdminKelasTeacherDropdown(
  value: String,
  options: List<AdminKelasTeacherOption>,
  onOptionSelected: (AdminKelasTeacherOption?) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = t("Wali kelas"),
      style = MaterialTheme.typography.labelMedium,
      color = SubtleInk,
      fontWeight = FontWeight.SemiBold
    )
    Box {
      OutlinedButton(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = t(value),
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Icon(
          imageVector = Icons.Outlined.ExpandMore,
          contentDescription = null
        )
      }
      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
      ) {
        DropdownMenuItem(
          text = { Text(t("Kosongkan wali kelas")) },
          onClick = {
            onOptionSelected(null)
            expanded = false
          }
        )
        options.forEach { option ->
          DropdownMenuItem(
            text = { Text(option.name) },
            onClick = {
              onOptionSelected(option)
              expanded = false
            }
          )
        }
      }
    }
  }
}

@Composable
private fun AdminKelasInfoPill(
  text: String,
  tone: Color,
  modifier: Modifier = Modifier
) {
  Text(
    text = t(text),
    style = MaterialTheme.typography.labelSmall,
    color = tone,
    fontWeight = FontWeight.Bold,
    modifier = modifier
      .background(tone.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
      .border(1.dp, tone.copy(alpha = 0.26f), RoundedCornerShape(999.dp))
      .padding(horizontal = 10.dp, vertical = 6.dp),
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    textAlign = TextAlign.Center
  )
}

@Composable
private fun AdminKelasNoticeCard(
  message: String,
  tone: Color
) {
  Text(
    text = t(message),
    style = MaterialTheme.typography.bodySmall,
    color = tone,
    modifier = Modifier
      .fillMaxWidth()
      .background(tone.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
      .border(1.dp, tone.copy(alpha = 0.24f), RoundedCornerShape(14.dp))
      .padding(12.dp)
  )
}

@Composable
private fun AdminKelasFilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  val tone = if (selected) PrimaryBlue else SubtleInk
  Text(
    text = t(label),
    style = MaterialTheme.typography.labelMedium,
    color = tone,
    fontWeight = FontWeight.Bold,
    modifier = Modifier
      .background(tone.copy(alpha = if (selected) 0.14f else 0.06f), RoundedCornerShape(999.dp))
      .border(1.dp, tone.copy(alpha = if (selected) 0.32f else 0.18f), RoundedCornerShape(999.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 8.dp)
  )
}

private enum class AdminKelasStudentFilter {
  All,
  SameLevel
}

private const val AdminKelasAllYearFilterKey = "__all_years__"
private const val AdminKelasYearFilterUnsetKey = "__unset_year__"

private fun AdminKelasSnapshot.newClassDraft(): AdminKelas {
  val activeYear = academicYears.firstOrNull { it.active }
  return AdminKelas(
    rowId = "",
    name = "",
    level = "",
    academicYearId = activeYear?.id ?: activeAcademicYearId,
    academicYearName = activeYear?.name ?: activeAcademicYearName,
    homeroomTeacherId = "",
    homeroomTeacherName = "",
    studentCount = 0
  )
}

private fun AdminKelasStudentOption.isActiveForAdminKelasUi(): Boolean {
  val normalizedStatus = status.trim().lowercase()
  return active && normalizedStatus !in setOf("tidak_aktif", "nonaktif", "inactive", "lulus")
}

private fun String.firstYearForAdminKelasUi(): Int? {
  return Regex("\\b(19|20)\\d{2}\\b").find(this)?.value?.toIntOrNull()
}
