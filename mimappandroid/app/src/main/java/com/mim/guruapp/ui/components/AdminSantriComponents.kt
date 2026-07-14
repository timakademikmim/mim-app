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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.remote.AdminSantri
import com.mim.guruapp.data.remote.AdminSantriClassOption
import com.mim.guruapp.data.remote.AdminSantriLoadResult
import com.mim.guruapp.data.remote.AdminSantriSaveResult
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
fun AdminSantriScreen(
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadSantri: suspend () -> AdminSantriLoadResult,
  onSaveSantri: suspend (AdminSantri) -> AdminSantriSaveResult,
  onPromoteSantri: suspend (AdminSantri) -> AdminSantriSaveResult,
  onGraduateSantri: suspend (AdminSantri) -> AdminSantriSaveResult,
  modifier: Modifier = Modifier
) {
  var students by remember { mutableStateOf<List<AdminSantri>>(emptyList()) }
  var classOptions by remember { mutableStateOf<List<AdminSantriClassOption>>(emptyList()) }
  var selectedStudent by remember { mutableStateOf<AdminSantri?>(null) }
  var query by rememberSaveable { mutableStateOf("") }
  var filterClassId by rememberSaveable { mutableStateOf(AdminSantriAllFilterKey) }
  var filterStatus by rememberSaveable { mutableStateOf(AdminSantriAllFilterKey) }
  var sortMode by rememberSaveable { mutableStateOf(AdminSantriSortMode.Name.name) }
  var isFilterSortDialogOpen by rememberSaveable { mutableStateOf(false) }
  var selectedStudentKeys by remember { mutableStateOf<List<String>>(emptyList()) }
  var pendingBulkAction by remember { mutableStateOf<AdminSantriAcademicAction?>(null) }
  var isBulkActionRunning by remember { mutableStateOf(false) }
  var activeYearName by remember { mutableStateOf("") }
  var noticeMessage by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  fun loadStudents(showGlobalRefresh: Boolean = false) {
    if (isLoading) return
    if (showGlobalRefresh) onRefresh()
    scope.launch {
      isLoading = true
      errorMessage = ""
      when (val result = onLoadSantri()) {
        is AdminSantriLoadResult.Success -> {
          students = result.snapshot.students
          classOptions = result.snapshot.classes
          activeYearName = result.snapshot.activeAcademicYearName
          noticeMessage = result.snapshot.statusMessage
          selectedStudentKeys = selectedStudentKeys.filter { selectedKey ->
            result.snapshot.students.any { it.selectionKey() == selectedKey && !it.isAcademicActionLocked() }
          }
          selectedStudent = selectedStudent?.let { current ->
            result.snapshot.students.firstOrNull { it.identityKey == current.identityKey } ?: current
          }
        }
        is AdminSantriLoadResult.Error -> errorMessage = result.message
      }
      isLoading = false
    }
  }

  LaunchedEffect(Unit) {
    loadStudents()
  }

  selectedStudent?.let { student ->
    AdminSantriDetailContent(
      student = student,
      classOptions = classOptions,
      onBackClick = { selectedStudent = null },
      onSaveStudent = onSaveSantri,
      onPromoteStudent = onPromoteSantri,
      onGraduateStudent = onGraduateSantri,
      onStudentSaved = { saved ->
        students = students.map { current ->
          if (current.identityKey == saved.identityKey) saved else current
        }
        selectedStudent = saved
      },
      snackbarHostState = snackbarHostState,
      modifier = modifier
    )
    return
  }

  val effectiveClassOptions = remember(classOptions, students) {
    classOptions.ifEmpty {
      students
        .filter { it.classId.isNotBlank() || it.className.isNotBlank() }
        .distinctBy { it.classId.ifBlank { it.className } }
        .map { student ->
          AdminSantriClassOption(
            id = student.classId,
            name = student.className.ifBlank { "Kelas" },
            academicYearId = student.academicYearId,
            academicYearName = student.academicYearName,
            level = student.classLevel
          )
        }
    }
  }
  val activeFilterCount = listOf(filterClassId, filterStatus).count { it != AdminSantriAllFilterKey }
  val filteredStudents = remember(students, query, filterClassId, filterStatus, sortMode) {
    val normalizedQuery = query.trim().lowercase()
    val sort = runCatching { AdminSantriSortMode.valueOf(sortMode) }.getOrDefault(AdminSantriSortMode.Name)
    students
      .filter { student ->
        val matchesQuery = if (normalizedQuery.isBlank()) {
          true
        } else {
          buildString {
            append(student.name)
            append(' ')
            append(student.nisn)
            append(' ')
            append(student.className)
            append(' ')
            append(student.status)
          }.lowercase().contains(normalizedQuery)
        }
        val matchesClass = filterClassId == AdminSantriAllFilterKey || student.classId == filterClassId
        val matchesStatus = filterStatus == AdminSantriAllFilterKey || student.status.normalizedStatusKey() == filterStatus
        matchesQuery && matchesClass && matchesStatus
      }
      .sortedWith(
        when (sort) {
          AdminSantriSortMode.Name -> compareBy<AdminSantri> { it.name.sortableText() }
            .thenBy { it.className.sortableText() }
          AdminSantriSortMode.Class -> compareBy<AdminSantri> { it.className.sortableText() }
            .thenBy { it.name.sortableText() }
          AdminSantriSortMode.Status -> compareBy<AdminSantri> { it.status.normalizedStatusKey() }
            .thenBy { it.name.sortableText() }
        }
      )
  }
  val selectedKeySet = selectedStudentKeys.toSet()
  val selectedStudents = remember(students, selectedStudentKeys) {
    val keys = selectedStudentKeys.toSet()
    students.filter { it.selectionKey() in keys && !it.isAcademicActionLocked() }
  }

  fun toggleStudentSelection(student: AdminSantri) {
    if (student.isAcademicActionLocked()) return
    val key = student.selectionKey()
    selectedStudentKeys = if (key in selectedStudentKeys) {
      selectedStudentKeys.filterNot { it == key }
    } else {
      (selectedStudentKeys + key).distinct()
    }
  }

  if (isFilterSortDialogOpen) {
    AdminSantriFilterSortDialog(
      classOptions = effectiveClassOptions,
      selectedClassId = filterClassId,
      selectedStatus = filterStatus,
      selectedSortMode = runCatching { AdminSantriSortMode.valueOf(sortMode) }.getOrDefault(AdminSantriSortMode.Name),
      onClassSelected = { filterClassId = it },
      onStatusSelected = { filterStatus = it },
      onSortSelected = { sortMode = it.name },
      onReset = {
        filterClassId = AdminSantriAllFilterKey
        filterStatus = AdminSantriAllFilterKey
        sortMode = AdminSantriSortMode.Name.name
      },
      onDismiss = { isFilterSortDialogOpen = false }
    )
  }
  pendingBulkAction?.let { action ->
    AdminSantriBulkActionDialog(
      action = action,
      students = selectedStudents,
      isSaving = isBulkActionRunning,
      onConfirm = {
        scope.launch {
          val actionStudents = selectedStudents
          if (actionStudents.isEmpty()) {
            pendingBulkAction = null
            selectedStudentKeys = emptyList()
            snackbarHostState.showSnackbar("Tidak ada santri yang bisa diproses.")
            return@launch
          }

          isBulkActionRunning = true
          var successCount = 0
          val failedKeys = mutableListOf<String>()
          val failures = mutableListOf<String>()
          actionStudents.forEach { student ->
            val result = when (action) {
              AdminSantriAcademicAction.Promote -> onPromoteSantri(student)
              AdminSantriAcademicAction.Graduate -> onGraduateSantri(student)
            }
            when (result) {
              is AdminSantriSaveResult.Success -> {
                successCount += 1
                students = students.map { current ->
                  if (current.identityKey == result.student.identityKey) result.student else current
                }
              }
              is AdminSantriSaveResult.Error -> {
                failedKeys += student.selectionKey()
                failures += "${student.name.ifBlank { "Santri" }}: ${result.message}"
              }
            }
          }
          selectedStudentKeys = failedKeys
          isBulkActionRunning = false
          pendingBulkAction = null

          val actionLabel = if (action == AdminSantriAcademicAction.Promote) "naik kelas" else "lulus"
          val failureText = failures.take(2).joinToString("; ")
          val message = buildString {
            append("$successCount santri berhasil diproses $actionLabel.")
            if (failures.isNotEmpty()) {
              append(" ${failures.size} gagal")
              if (failureText.isNotBlank()) append(": $failureText")
            }
          }
          snackbarHostState.showSnackbar(message)
        }
      },
      onDismiss = {
        if (!isBulkActionRunning) pendingBulkAction = null
      }
    )
  }

  BackHandler(enabled = selectedStudentKeys.isNotEmpty() && !isBulkActionRunning) {
    selectedStudentKeys = emptyList()
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
      AdminSantriTopBar(
        title = if (selectedStudentKeys.isEmpty()) "Santri" else "${selectedStudentKeys.size} dipilih",
        isDetail = false,
        actionIcon = if (selectedStudentKeys.isEmpty()) null else Icons.Outlined.Close,
        actionContentDescription = "Batal pilih",
        actionEnabled = !isBulkActionRunning,
        onMenuClick = onMenuClick,
        onBackClick = {},
        onActionClick = { selectedStudentKeys = emptyList() }
      )
      AdminSantriSearchBar(
        value = query,
        onValueChange = { query = it }
      )
      AdminSantriFilterSortButton(
        activeFilterCount = activeFilterCount,
        sortMode = runCatching { AdminSantriSortMode.valueOf(sortMode) }.getOrDefault(AdminSantriSortMode.Name),
        resultCount = filteredStudents.size,
        onClick = { isFilterSortDialogOpen = true }
      )
      if (selectedStudentKeys.isNotEmpty()) {
        val selectableVisibleStudents = filteredStudents.filterNot { it.isAcademicActionLocked() }
        AdminSantriBulkActionBar(
          selectedCount = selectedStudentKeys.size,
          visibleCount = selectableVisibleStudents.size,
          allVisibleSelected = selectableVisibleStudents.isNotEmpty() &&
            selectableVisibleStudents.all { it.selectionKey() in selectedKeySet },
          isSaving = isBulkActionRunning,
          onSelectVisible = {
            val visibleKeys = selectableVisibleStudents.map { it.selectionKey() }
            selectedStudentKeys = (selectedStudentKeys + visibleKeys).distinct()
          },
          onClearSelection = { selectedStudentKeys = emptyList() },
          onActionSelected = { action -> pendingBulkAction = action }
        )
      }
      PullToRefreshBox(
        isRefreshing = isRefreshing || isLoading,
        onRefresh = { loadStudents(showGlobalRefresh = true) },
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
          if (noticeMessage.isNotBlank()) {
            item {
              AdminSantriNoticeCard(noticeMessage)
            }
          }
          if (activeYearName.isNotBlank()) {
            item {
              AdminSantriNoticeCard("Tahun ajaran aktif: $activeYearName", tone = PrimaryBlue)
            }
          }
          if (errorMessage.isNotBlank()) {
            item {
              AdminSantriNoticeCard(errorMessage, tone = Color(0xFFFB7185))
            }
          }
          if (isLoading && students.isEmpty()) {
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
          } else if (filteredStudents.isEmpty()) {
            item {
              EmptyPlaceholderCard(
                if (query.isBlank()) "Belum ada data santri." else "Tidak ada santri yang cocok dengan pencarian."
              )
            }
          } else {
            items(filteredStudents, key = { it.identityKey.ifBlank { it.rowId } }) { student ->
              AdminSantriCard(
                student = student,
                selected = student.selectionKey() in selectedKeySet,
                onClick = {
                  if (selectedStudentKeys.isEmpty()) {
                    selectedStudent = student
                  } else {
                    toggleStudentSelection(student)
                  }
                },
                onSelectionToggle = { toggleStudentSelection(student) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminSantriDetailContent(
  student: AdminSantri,
  classOptions: List<AdminSantriClassOption>,
  onBackClick: () -> Unit,
  onSaveStudent: suspend (AdminSantri) -> AdminSantriSaveResult,
  onPromoteStudent: suspend (AdminSantri) -> AdminSantriSaveResult,
  onGraduateStudent: suspend (AdminSantri) -> AdminSantriSaveResult,
  onStudentSaved: (AdminSantri) -> Unit,
  snackbarHostState: SnackbarHostState,
  modifier: Modifier = Modifier
) {
  var isEditing by rememberSaveable(student.rowId) { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var pendingAction by remember { mutableStateOf<AdminSantriAcademicAction?>(null) }
  var name by rememberSaveable(student.rowId, student.name) { mutableStateOf(student.name) }
  var nisn by rememberSaveable(student.rowId, student.nisn) { mutableStateOf(student.nisn) }
  var gender by rememberSaveable(student.rowId, student.gender) { mutableStateOf(student.gender) }
  var classId by rememberSaveable(student.rowId, student.classId) { mutableStateOf(student.classId) }
  var status by rememberSaveable(student.rowId, student.status, student.active) {
    mutableStateOf(student.status.normalizedStatusKey().ifBlank { if (student.active) "aktif" else "tidak_aktif" })
  }
  var studentPhone by rememberSaveable(student.rowId, student.studentPhone) { mutableStateOf(student.studentPhone) }
  var fatherName by rememberSaveable(student.rowId, student.fatherName) { mutableStateOf(student.fatherName) }
  var fatherPhone by rememberSaveable(student.rowId, student.fatherPhone) { mutableStateOf(student.fatherPhone) }
  var motherName by rememberSaveable(student.rowId, student.motherName) { mutableStateOf(student.motherName) }
  var motherPhone by rememberSaveable(student.rowId, student.motherPhone) { mutableStateOf(student.motherPhone) }
  var guardianName by rememberSaveable(student.rowId, student.guardianName) { mutableStateOf(student.guardianName) }
  var guardianPhone by rememberSaveable(student.rowId, student.guardianPhone) { mutableStateOf(student.guardianPhone) }
  var address by rememberSaveable(student.rowId, student.address) { mutableStateOf(student.address) }
  var note by rememberSaveable(student.rowId, student.note) { mutableStateOf(student.note) }
  var room by rememberSaveable(student.rowId, student.room) { mutableStateOf(student.room) }
  var halaqah by rememberSaveable(student.rowId, student.halaqah) { mutableStateOf(student.halaqah) }
  val scope = rememberCoroutineScope()
  val selectedClass = classOptions.firstOrNull { it.id == classId }
  val lockedAcademicStatus = student.status.normalizedStatusKey() in AdminSantriLockedStatusKeys
  val draft = student.copy(
    name = name,
    nisn = nisn,
    gender = gender,
    classId = classId,
    className = selectedClass?.name ?: student.className,
    classLevel = selectedClass?.level ?: student.classLevel,
    academicYearId = selectedClass?.academicYearId ?: student.academicYearId,
    academicYearName = selectedClass?.academicYearName ?: student.academicYearName,
    status = status,
    active = status.statusMeansActive(),
    studentPhone = studentPhone,
    fatherName = fatherName,
    fatherPhone = fatherPhone,
    motherName = motherName,
    motherPhone = motherPhone,
    guardianName = guardianName,
    guardianPhone = guardianPhone,
    address = address,
    note = note,
    room = room,
    halaqah = halaqah
  )
  val isDirty = draft != student

  pendingAction?.let { action ->
    AdminSantriAcademicActionDialog(
      action = action,
      student = student,
      isSaving = isSaving,
      onConfirm = {
        scope.launch {
          isSaving = true
          val result = when (action) {
            AdminSantriAcademicAction.Promote -> onPromoteStudent(student)
            AdminSantriAcademicAction.Graduate -> onGraduateStudent(student)
          }
          when (result) {
            is AdminSantriSaveResult.Success -> {
              onStudentSaved(result.student)
              isEditing = false
              pendingAction = null
              snackbarHostState.showSnackbar(result.message)
            }
            is AdminSantriSaveResult.Error -> snackbarHostState.showSnackbar(result.message)
          }
          isSaving = false
        }
      },
      onDismiss = {
        if (!isSaving) pendingAction = null
      }
    )
  }

  BackHandler(enabled = true) {
    if (isEditing && !isSaving) {
      isEditing = false
    } else if (!isSaving) {
      onBackClick()
    }
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
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      AdminSantriTopBar(
        title = "Detail Santri",
        isDetail = true,
        actionIcon = if (isEditing) Icons.Outlined.Check else Icons.Outlined.Edit,
        actionContentDescription = if (isEditing) "Simpan perubahan" else "Edit santri",
        actionEnabled = !isSaving && (!isEditing || isDirty),
        onMenuClick = {},
        onBackClick = {
          if (isEditing && !isSaving) {
            isEditing = false
          } else if (!isSaving) {
            onBackClick()
          }
        },
        onActionClick = {
          if (!isEditing) {
            isEditing = true
          } else if (!isSaving && isDirty) {
            scope.launch {
              isSaving = true
              when (val result = onSaveStudent(draft)) {
                is AdminSantriSaveResult.Success -> {
                  onStudentSaved(result.student)
                  isEditing = false
                  snackbarHostState.showSnackbar(result.message)
                }
                is AdminSantriSaveResult.Error -> snackbarHostState.showSnackbar(result.message)
              }
              isSaving = false
            }
          }
        }
      )
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .navigationBarsPadding()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        AdminSantriProfileHeader(draft)

        PlaceholderPanel(title = "Status dan Kelas") {
          if (isEditing && !lockedAcademicStatus) {
            AdminSantriDropdownField(
              label = "Status",
              value = status.displayStatusLabel(),
              options = AdminSantriEditableStatusOptions.map { it to it.displayStatusLabel() },
              onSelect = { selected -> status = selected }
            )
            AdminSantriDropdownField(
              label = "Kelas sekarang",
              value = selectedClass?.displayName().orEmpty().ifBlank { draft.className.ifBlank { "-" } },
              options = classOptions.map { it.id to it.displayName() },
              onSelect = { selected -> classId = selected },
              modifier = Modifier.padding(top = 10.dp)
            )
          } else {
            AdminSantriInfoRow("Status", draft.displayStatus())
            AdminSantriInfoRow("Kelas sekarang", draft.className.ifBlank { "-" })
          }
          AdminSantriInfoRow("Tahun ajaran", draft.academicYearName.ifBlank { "-" })
          AdminSantriInfoRow("Aktif", if (draft.active) "Ya" else "Tidak")
          if (!isEditing && !lockedAcademicStatus) {
            AdminSantriAcademicActionButtons(
              enabled = !isSaving,
              onPromoteClick = { pendingAction = AdminSantriAcademicAction.Promote },
              onGraduateClick = { pendingAction = AdminSantriAcademicAction.Graduate },
              modifier = Modifier.padding(top = 12.dp)
            )
          }
        }

        PlaceholderPanel(title = "Biodata") {
          if (isEditing) {
            AdminSantriInputField(
              value = name,
              onValueChange = { name = it },
              label = "Nama",
              placeholder = "Masukkan nama santri"
            )
            AdminSantriInputField(
              value = nisn,
              onValueChange = { nisn = it },
              label = "NISN",
              placeholder = "Masukkan NISN",
              modifier = Modifier.padding(top = 10.dp)
            )
            AdminSantriInputField(
              value = gender,
              onValueChange = { gender = it },
              label = "Jenis kelamin",
              placeholder = "Contoh: Laki-laki",
              modifier = Modifier.padding(top = 10.dp)
            )
            AdminSantriInputField(
              value = studentPhone,
              onValueChange = { studentPhone = it },
              label = "Nomor HP",
              placeholder = "Masukkan nomor HP",
              keyboardType = KeyboardType.Phone,
              modifier = Modifier.padding(top = 10.dp)
            )
          } else {
            AdminSantriInfoRow("Nama", draft.name)
            AdminSantriInfoRow("NISN", draft.nisn.ifBlank { "-" })
            AdminSantriInfoRow("Jenis kelamin", draft.gender.ifBlank { "-" })
            AdminSantriInfoRow("Nomor HP", draft.studentPhone.ifBlank { "-" })
          }
        }

        PlaceholderPanel(title = "Orang Tua dan Wali") {
          if (isEditing) {
            AdminSantriInputField(
              value = fatherName,
              onValueChange = { fatherName = it },
              label = "Ayah",
              placeholder = "Masukkan nama ayah"
            )
            AdminSantriInputField(
              value = fatherPhone,
              onValueChange = { fatherPhone = it },
              label = "Nomor HP Ayah",
              placeholder = "Masukkan nomor HP ayah",
              keyboardType = KeyboardType.Phone,
              modifier = Modifier.padding(top = 10.dp)
            )
            AdminSantriInputField(
              value = motherName,
              onValueChange = { motherName = it },
              label = "Ibu",
              placeholder = "Masukkan nama ibu",
              modifier = Modifier.padding(top = 10.dp)
            )
            AdminSantriInputField(
              value = motherPhone,
              onValueChange = { motherPhone = it },
              label = "Nomor HP Ibu",
              placeholder = "Masukkan nomor HP ibu",
              keyboardType = KeyboardType.Phone,
              modifier = Modifier.padding(top = 10.dp)
            )
            AdminSantriInputField(
              value = guardianName,
              onValueChange = { guardianName = it },
              label = "Wali",
              placeholder = "Masukkan nama wali",
              modifier = Modifier.padding(top = 10.dp)
            )
            AdminSantriInputField(
              value = guardianPhone,
              onValueChange = { guardianPhone = it },
              label = "Nomor HP Wali",
              placeholder = "Masukkan nomor HP wali",
              keyboardType = KeyboardType.Phone,
              modifier = Modifier.padding(top = 10.dp)
            )
          } else {
            AdminSantriInfoRow("Ayah", draft.fatherName.ifBlank { "-" })
            AdminSantriInfoRow("Nomor HP Ayah", draft.fatherPhone.ifBlank { "-" })
            AdminSantriInfoRow("Ibu", draft.motherName.ifBlank { "-" })
            AdminSantriInfoRow("Nomor HP Ibu", draft.motherPhone.ifBlank { "-" })
            AdminSantriInfoRow("Wali", draft.guardianName.ifBlank { "-" })
            AdminSantriInfoRow("Nomor HP Wali", draft.guardianPhone.ifBlank { "-" })
          }
        }

        PlaceholderPanel(title = "Alamat dan Catatan") {
          if (isEditing) {
            AdminSantriInputField(
              value = address,
              onValueChange = { address = it },
              label = "Alamat",
              placeholder = "Masukkan alamat",
              singleLine = false
            )
            AdminSantriInputField(
              value = room,
              onValueChange = { room = it },
              label = "Kamar",
              placeholder = "Masukkan kamar",
              modifier = Modifier.padding(top = 10.dp)
            )
            AdminSantriInputField(
              value = halaqah,
              onValueChange = { halaqah = it },
              label = "Halaqah",
              placeholder = "Masukkan halaqah",
              modifier = Modifier.padding(top = 10.dp)
            )
            AdminSantriInputField(
              value = note,
              onValueChange = { note = it },
              label = "Catatan",
              placeholder = "Masukkan catatan",
              singleLine = false,
              modifier = Modifier.padding(top = 10.dp)
            )
          } else {
            AdminSantriInfoRow("Alamat", draft.address.ifBlank { "-" })
            AdminSantriInfoRow("Kamar", draft.room.ifBlank { "-" })
            AdminSantriInfoRow("Halaqah", draft.halaqah.ifBlank { "-" })
            AdminSantriInfoRow("Catatan", draft.note.ifBlank { "-" })
          }
        }

        Spacer(modifier = Modifier.height(112.dp))
      }
    }
  }
}

@Composable
private fun AdminSantriAcademicActionButtons(
  enabled: Boolean,
  onPromoteClick: () -> Unit,
  onGraduateClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    OutlinedButton(
      onClick = onPromoteClick,
      enabled = enabled,
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(16.dp)
    ) {
      Text(
        text = t("Naik Kelas"),
        fontWeight = FontWeight.ExtraBold,
        color = PrimaryBlueDark
      )
    }
    Button(
      onClick = onGraduateClick,
      enabled = enabled,
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(16.dp)
    ) {
      Text(
        text = t("Luluskan"),
        fontWeight = FontWeight.ExtraBold
      )
    }
  }
}

@Composable
private fun AdminSantriAcademicActionDialog(
  action: AdminSantriAcademicAction,
  student: AdminSantri,
  isSaving: Boolean,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  val isPromote = action == AdminSantriAcademicAction.Promote
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = t(if (isPromote) "Naikkan kelas santri?" else "Luluskan santri?"),
        fontWeight = FontWeight.ExtraBold
      )
    },
    text = {
      Text(
        text = t(
          if (isPromote) {
            "Aplikasi akan memindahkan ${student.name.ifBlank { "santri" }} ke kelas tahun ajaran berikutnya tanpa membuat data ganda. Nilai, absensi, dan laporan sebelumnya tetap tersimpan sebagai riwayat."
          } else {
            "Aplikasi akan menandai ${student.name.ifBlank { "santri" }} sebagai lulus dan tidak aktif. Data nilai, absensi, dan laporan sebelumnya tetap disimpan."
          }
        ),
        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        enabled = !isSaving
      ) {
        Text(t(if (isSaving) "Memproses..." else "Lanjut"))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isSaving
      ) {
        Text(t("Batal"))
      }
    }
  )
}

@Composable
private fun AdminSantriBulkActionDialog(
  action: AdminSantriAcademicAction,
  students: List<AdminSantri>,
  isSaving: Boolean,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  val isPromote = action == AdminSantriAcademicAction.Promote
  val actionTitle = if (isPromote) "Naikkan kelas santri terpilih?" else "Luluskan santri terpilih?"
  val previewNames = students.take(3).joinToString(", ") { it.name.ifBlank { "Santri" } }
  val extraCount = students.size - 3
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = t(actionTitle),
        fontWeight = FontWeight.ExtraBold
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = t(
            if (isPromote) {
              "Aplikasi akan memproses ${students.size} santri satu per satu ke kelas tahun ajaran berikutnya tanpa membuat data ganda."
            } else {
              "Aplikasi akan menandai ${students.size} santri sebagai lulus dan tidak aktif."
            }
          ),
          color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        )
        if (previewNames.isNotBlank()) {
          Text(
            text = t(
              buildString {
                append("Terpilih: ")
                append(previewNames)
                if (extraCount > 0) append(", dan $extraCount lainnya")
              }
            ),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = SubtleInk
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        enabled = !isSaving && students.isNotEmpty()
      ) {
        Text(t(if (isSaving) "Memproses..." else "Lanjut"))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isSaving
      ) {
        Text(t("Batal"))
      }
    }
  )
}

@Composable
private fun AdminSantriTopBar(
  title: String,
  isDetail: Boolean,
  onMenuClick: () -> Unit,
  onBackClick: () -> Unit,
  onActionClick: () -> Unit = {},
  modifier: Modifier = Modifier,
  actionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  actionContentDescription: String = "",
  actionEnabled: Boolean = true
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    AdminSantriTopButton(
      icon = if (isDetail) Icons.AutoMirrored.Outlined.ArrowBack else Icons.Outlined.Menu,
      contentDescription = if (isDetail) "Kembali" else "Buka sidebar",
      onClick = if (isDetail) onBackClick else onMenuClick
    )
    Text(
      text = t(title),
      style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 12.dp)
    )
    if (actionIcon != null) {
      AdminSantriTopButton(
        icon = actionIcon,
        contentDescription = actionContentDescription,
        onClick = onActionClick,
        enabled = actionEnabled
      )
    } else {
      Spacer(modifier = Modifier.size(42.dp))
    }
  }
}

@Composable
private fun AdminSantriTopButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
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
      contentDescription = t(contentDescription),
      tint = if (enabled) PrimaryBlueDark else SubtleInk.copy(alpha = 0.45f)
    )
  }
}

@Composable
private fun AdminSantriSearchBar(
  value: String,
  onValueChange: (String) -> Unit
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    shape = RoundedCornerShape(18.dp),
    leadingIcon = {
      Icon(
        imageVector = Icons.Outlined.Search,
        contentDescription = null
      )
    },
    label = { Text(t("Cari santri")) },
    placeholder = { Text(t("Nama, NISN, kelas, atau status")) }
  )
}

@Composable
private fun AdminSantriBulkActionBar(
  selectedCount: Int,
  visibleCount: Int,
  allVisibleSelected: Boolean,
  isSaving: Boolean,
  onSelectVisible: () -> Unit,
  onClearSelection: () -> Unit,
  onActionSelected: (AdminSantriAcademicAction) -> Unit
) {
  var menuExpanded by remember { mutableStateOf(false) }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x120F172A), spotColor = Color(0x120F172A))
      .background(CardBackground.copy(alpha = 0.94f), RoundedCornerShape(18.dp))
      .border(1.dp, PrimaryBlue.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = t("$selectedCount santri dipilih"),
        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = t(if (allVisibleSelected) "Semua hasil tampil sudah dipilih" else "$visibleCount santri bisa dipilih di hasil tampil"),
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    TextButton(
      onClick = onSelectVisible,
      enabled = !isSaving && !allVisibleSelected && visibleCount > 0
    ) {
      Text(t("Pilih semua"))
    }
    TextButton(
      onClick = onClearSelection,
      enabled = !isSaving
    ) {
      Text(t("Batal"))
    }
    Box {
      Button(
        onClick = { menuExpanded = true },
        enabled = !isSaving && selectedCount > 0,
        shape = RoundedCornerShape(14.dp)
      ) {
        Text(t(if (isSaving) "Memproses..." else "Tindakan"))
      }
      DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false }
      ) {
        DropdownMenuItem(
          text = { Text(t("Naik Kelas")) },
          onClick = {
            menuExpanded = false
            onActionSelected(AdminSantriAcademicAction.Promote)
          }
        )
        DropdownMenuItem(
          text = { Text(t("Luluskan")) },
          onClick = {
            menuExpanded = false
            onActionSelected(AdminSantriAcademicAction.Graduate)
          }
        )
      }
    }
  }
}

@Composable
private fun AdminSantriFilterSortButton(
  activeFilterCount: Int,
  sortMode: AdminSantriSortMode,
  resultCount: Int,
  onClick: () -> Unit
) {
  OutlinedButton(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp)
  ) {
    Text(
      text = t("Sortir & Filter"),
      fontWeight = FontWeight.ExtraBold,
      color = PrimaryBlueDark
    )
    Spacer(modifier = Modifier.weight(1f))
    Text(
      text = t("${sortMode.title} | $activeFilterCount filter | $resultCount santri"),
      style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun AdminSantriFilterSortDialog(
  classOptions: List<AdminSantriClassOption>,
  selectedClassId: String,
  selectedStatus: String,
  selectedSortMode: AdminSantriSortMode,
  onClassSelected: (String) -> Unit,
  onStatusSelected: (String) -> Unit,
  onSortSelected: (AdminSantriSortMode) -> Unit,
  onReset: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = t("Sortir & Filter"),
        fontWeight = FontWeight.ExtraBold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 440.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        AdminSantriDialogSectionTitle("Filter Kelas")
        AdminSantriChoiceRow(
          label = "Semua kelas",
          selected = selectedClassId == AdminSantriAllFilterKey,
          onClick = { onClassSelected(AdminSantriAllFilterKey) }
        )
        classOptions.forEach { option ->
          AdminSantriChoiceRow(
            label = option.displayName(),
            selected = selectedClassId == option.id,
            onClick = { onClassSelected(option.id) }
          )
        }

        AdminSantriDialogSectionTitle("Filter Status")
        AdminSantriChoiceRow(
          label = "Semua status",
          selected = selectedStatus == AdminSantriAllFilterKey,
          onClick = { onStatusSelected(AdminSantriAllFilterKey) }
        )
        AdminSantriFilterStatusOptions.forEach { status ->
          AdminSantriChoiceRow(
            label = status.displayStatusLabel(),
            selected = selectedStatus == status,
            onClick = { onStatusSelected(status) }
          )
        }

        AdminSantriDialogSectionTitle("Sortir")
        AdminSantriSortMode.values().forEach { mode ->
          AdminSantriChoiceRow(
            label = mode.title,
            selected = selectedSortMode == mode,
            onClick = { onSortSelected(mode) }
          )
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text(t("Terapkan"))
      }
    },
    dismissButton = {
      TextButton(onClick = onReset) {
        Text(t("Reset"))
      }
    }
  )
}

@Composable
private fun AdminSantriDialogSectionTitle(text: String) {
  Text(
    text = t(text),
    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
    color = PrimaryBlueDark,
    fontWeight = FontWeight.ExtraBold,
    modifier = Modifier.padding(top = 8.dp)
  )
}

@Composable
private fun AdminSantriChoiceRow(
  label: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(if (selected) PrimaryBlue.copy(alpha = 0.12f) else SoftPanel.copy(alpha = 0.58f))
      .border(
        1.dp,
        if (selected) PrimaryBlue.copy(alpha = 0.32f) else CardBorder.copy(alpha = 0.75f),
        RoundedCornerShape(14.dp)
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = t(label),
      style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
      color = if (selected) PrimaryBlueDark else androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
      fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
      modifier = Modifier.weight(1f)
    )
    if (selected) {
      Text(
        text = t("Dipilih"),
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        color = PrimaryBlue,
        fontWeight = FontWeight.ExtraBold
      )
    }
  }
}

@Composable
private fun AdminSantriCard(
  student: AdminSantri,
  selected: Boolean,
  onClick: () -> Unit,
  onSelectionToggle: () -> Unit
) {
  val selectable = !student.isAcademicActionLocked()
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x120F172A), spotColor = Color(0x120F172A))
      .clip(RoundedCornerShape(18.dp))
      .background(Brush.linearGradient(listOf(CardGradientStart, CardGradientEnd)))
      .border(
        BorderStroke(if (selected) 2.dp else 1.dp, if (selected) PrimaryBlue else CardBorder),
        RoundedCornerShape(18.dp)
      )
      .clickable(onClick = onClick)
      .padding(14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(PrimaryBlue.copy(alpha = 0.12f))
        .border(1.dp, PrimaryBlue.copy(alpha = 0.18f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = student.initials(),
        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
    }
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(start = 12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = student.name.ifBlank { "-" },
          style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
          color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        StatusPill(student.displayStatus(), if (student.active) SuccessTint else Color(0xFFFB7185))
      }
      Text(
        text = student.className.ifBlank { "Kelas belum diatur" },
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        color = PrimaryBlue,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 5.dp)
      )
      Text(
        text = student.nisn.ifBlank { "NISN belum diisi" },
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        modifier = Modifier.padding(top = 3.dp)
      )
    }
    Checkbox(
      checked = selected,
      onCheckedChange = {
        onSelectionToggle()
      },
      enabled = selectable,
      modifier = Modifier.padding(start = 8.dp)
    )
  }
}

@Composable
private fun AdminSantriProfileHeader(student: AdminSantri) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x160F172A), spotColor = Color(0x160F172A))
      .background(Brush.verticalGradient(listOf(CardBackground, CardGradientEnd)), RoundedCornerShape(24.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
      .padding(18.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(62.dp)
          .clip(CircleShape)
          .background(PrimaryBlue.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = student.initials(),
          style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(start = 14.dp)
      ) {
        Text(
          text = student.name.ifBlank { "Nama santri" },
          style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
          color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = student.className.ifBlank { "Kelas belum diatur" },
          style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
          color = SubtleInk,
          modifier = Modifier.padding(top = 3.dp)
        )
      }
    }
    Row(
      modifier = Modifier.padding(top = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      StatusPill(student.displayStatus(), if (student.active) SuccessTint else Color(0xFFFB7185))
      if (student.nisn.isNotBlank()) {
        StatusPill("NISN ${student.nisn}", PrimaryBlue)
      }
    }
  }
}

@Composable
private fun AdminSantriInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 7.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.Top
  ) {
    Text(
      text = t(label),
      style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      modifier = Modifier.weight(0.42f)
    )
    Text(
      text = value.ifBlank { "-" },
      style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
      color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.weight(0.58f)
    )
  }
}

@Composable
private fun AdminSantriInputField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String,
  modifier: Modifier = Modifier,
  keyboardType: KeyboardType = KeyboardType.Text,
  singleLine: Boolean = true
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier.fillMaxWidth(),
    label = { Text(t(label)) },
    placeholder = { Text(t(placeholder)) },
    shape = RoundedCornerShape(18.dp),
    singleLine = singleLine,
    minLines = if (singleLine) 1 else 3,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
  )
}

@Composable
private fun AdminSantriDropdownField(
  label: String,
  value: String,
  options: List<Pair<String, String>>,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = t(label),
      style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(bottom = 6.dp)
    )
    Box(modifier = Modifier.fillMaxWidth()) {
      OutlinedButton(
        onClick = { expanded = true },
        enabled = options.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
      ) {
        Text(
          text = value.ifBlank { "-" },
          color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
      }
      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.fillMaxWidth()
      ) {
        options.forEach { (key, labelText) ->
          DropdownMenuItem(
            text = { Text(t(labelText)) },
            onClick = {
              onSelect(key)
              expanded = false
            }
          )
        }
      }
    }
  }
}

@Composable
private fun AdminSantriNoticeCard(
  message: String,
  tone: Color = SuccessTint
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(tone.copy(alpha = 0.12f))
      .border(1.dp, tone.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
      .padding(14.dp)
  ) {
    Text(
      text = t(message),
      style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
      color = tone,
      fontWeight = FontWeight.SemiBold
    )
  }
}

private const val AdminSantriAllFilterKey = "__all__"

private val AdminSantriEditableStatusOptions = listOf(
  "aktif",
  "tidak_aktif"
)

private val AdminSantriFilterStatusOptions = listOf(
  "aktif",
  "tidak_aktif",
  "naik_kelas",
  "lulus"
)

private val AdminSantriLockedStatusKeys = setOf("naik_kelas", "lulus")

private enum class AdminSantriAcademicAction {
  Promote,
  Graduate
}

private enum class AdminSantriSortMode(val title: String) {
  Name("Nama"),
  Class("Kelas"),
  Status("Status")
}

private fun AdminSantri.initials(): String {
  val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
  return when {
    words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
    words.size == 1 -> words[0].take(2).uppercase()
    nisn.isNotBlank() -> nisn.take(2).uppercase()
    else -> "SN"
  }
}

private fun AdminSantri.displayStatus(): String {
  return status.displayStatusLabel().ifBlank { if (active) "Aktif" else "Nonaktif" }
}

private fun AdminSantri.selectionKey(): String {
  return identityKey.ifBlank { rowId }
}

private fun AdminSantri.isAcademicActionLocked(): Boolean {
  return status.normalizedStatusKey() in AdminSantriLockedStatusKeys
}

private fun String.displayStatusLabel(): String {
  return normalizedStatusKey()
    .replace('_', ' ')
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
}

private fun String.normalizedStatusKey(): String {
  val normalized = trim().lowercase().replace('-', '_').replace(' ', '_')
  return when (normalized) {
    "nonaktif", "tidakaktif", "tidak_aktif", "inactive" -> "tidak_aktif"
    "naik", "naik_kelas" -> "naik_kelas"
    "graduate", "graduated" -> "lulus"
    "aktif", "active" -> "aktif"
    else -> normalized
  }
}

private fun String.statusMeansActive(): Boolean {
  return when (normalizedStatusKey()) {
    "tidak_aktif", "naik_kelas", "lulus" -> false
    else -> true
  }
}

private fun String.sortableText(): String {
  return if (isBlank()) "zzzz" else trim().lowercase()
}

private fun AdminSantriClassOption.displayName(): String {
  return buildString {
    append(name.ifBlank { "Kelas" })
    if (academicYearName.isNotBlank()) {
      append(" - ")
      append(academicYearName)
    }
  }
}
