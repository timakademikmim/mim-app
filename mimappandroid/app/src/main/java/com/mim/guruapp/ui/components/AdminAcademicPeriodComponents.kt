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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.remote.AdminAcademicPeriodLoadResult
import com.mim.guruapp.data.remote.AdminAcademicPeriodSaveResult
import com.mim.guruapp.data.remote.AdminAcademicPeriodSnapshot
import com.mim.guruapp.data.remote.AdminAcademicSemester
import com.mim.guruapp.data.remote.AdminAcademicYear
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
fun AdminAcademicPeriodScreen(
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadSnapshot: suspend () -> AdminAcademicPeriodLoadResult,
  onSaveAcademicYear: suspend (AdminAcademicYear) -> AdminAcademicPeriodSaveResult,
  onSaveSemester: suspend (AdminAcademicSemester) -> AdminAcademicPeriodSaveResult,
  onSetActiveAcademicYear: suspend (String) -> AdminAcademicPeriodSaveResult,
  onSetActiveSemester: suspend (String) -> AdminAcademicPeriodSaveResult,
  modifier: Modifier = Modifier
) {
  var snapshot by remember { mutableStateOf(AdminAcademicPeriodSnapshot(emptyList(), emptyList())) }
  var selectedYearId by rememberSaveable { mutableStateOf("") }
  var yearDialogDraft by remember { mutableStateOf<AdminAcademicYear?>(null) }
  var semesterDialogDraft by remember { mutableStateOf<AdminAcademicSemester?>(null) }
  var isLoading by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  fun applySnapshot(nextSnapshot: AdminAcademicPeriodSnapshot) {
    snapshot = nextSnapshot
    val yearIds = nextSnapshot.academicYears.map { it.rowId }.toSet()
    selectedYearId = when {
      selectedYearId in yearIds -> selectedYearId
      nextSnapshot.activeAcademicYear?.rowId?.isNotBlank() == true -> nextSnapshot.activeAcademicYear?.rowId.orEmpty()
      else -> nextSnapshot.academicYears.firstOrNull()?.rowId.orEmpty()
    }
  }

  fun loadSnapshot(showGlobalRefresh: Boolean = false) {
    scope.launch {
      isLoading = true
      errorMessage = ""
      if (showGlobalRefresh) onRefresh()
      when (val result = onLoadSnapshot()) {
        is AdminAcademicPeriodLoadResult.Success -> applySnapshot(result.snapshot)
        is AdminAcademicPeriodLoadResult.Error -> errorMessage = result.message
      }
      isLoading = false
    }
  }

  suspend fun handleSaveResult(result: AdminAcademicPeriodSaveResult, refreshDashboard: Boolean = false) {
    when (result) {
      is AdminAcademicPeriodSaveResult.Success -> {
        applySnapshot(result.snapshot)
        snackbarHostState.showSnackbar(result.message)
        if (refreshDashboard) onRefresh()
      }
      is AdminAcademicPeriodSaveResult.Error -> snackbarHostState.showSnackbar(result.message)
    }
  }

  LaunchedEffect(Unit) {
    loadSnapshot()
  }

  if (yearDialogDraft != null) {
    AdminAcademicYearDialog(
      draft = yearDialogDraft ?: AdminAcademicPeriodEmptyYear,
      isSaving = isSaving,
      onDismiss = { if (!isSaving) yearDialogDraft = null },
      onConfirm = { year ->
        scope.launch {
          isSaving = true
          when (val result = onSaveAcademicYear(year)) {
            is AdminAcademicPeriodSaveResult.Success -> {
              yearDialogDraft = null
              applySnapshot(result.snapshot)
              snackbarHostState.showSnackbar(result.message)
            }
            is AdminAcademicPeriodSaveResult.Error -> snackbarHostState.showSnackbar(result.message)
          }
          isSaving = false
        }
      }
    )
  }

  if (semesterDialogDraft != null) {
    AdminAcademicSemesterDialog(
      draft = semesterDialogDraft ?: snapshot.newSemesterDraft(selectedYearId),
      isSaving = isSaving,
      onDismiss = { if (!isSaving) semesterDialogDraft = null },
      onConfirm = { semester ->
        scope.launch {
          isSaving = true
          when (val result = onSaveSemester(semester)) {
            is AdminAcademicPeriodSaveResult.Success -> {
              semesterDialogDraft = null
              applySnapshot(result.snapshot)
              snackbarHostState.showSnackbar(result.message)
            }
            is AdminAcademicPeriodSaveResult.Error -> snackbarHostState.showSnackbar(result.message)
          }
          isSaving = false
        }
      }
    )
  }

  BackHandler(enabled = yearDialogDraft != null || semesterDialogDraft != null) {
    if (!isSaving) {
      yearDialogDraft = null
      semesterDialogDraft = null
    }
  }

  val selectedYear = snapshot.academicYears.firstOrNull { it.rowId == selectedYearId }
    ?: snapshot.activeAcademicYear
    ?: snapshot.academicYears.firstOrNull()
  val selectedSemesters = snapshot.semesters
    .filter { it.academicYearId == selectedYear?.rowId }
    .sortedWith(
      compareByDescending<AdminAcademicSemester> { it.active }
        .thenByDescending { it.startDateIso }
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
      AdminAcademicPeriodTopBar(
        title = "Tahun Ajaran",
        onMenuClick = onMenuClick,
        actionIcon = Icons.Outlined.Add,
        actionContentDescription = "Tambah tahun ajaran",
        onActionClick = { yearDialogDraft = AdminAcademicPeriodEmptyYear }
      )
      PullToRefreshBox(
        isRefreshing = isRefreshing || isLoading,
        onRefresh = { loadSnapshot(showGlobalRefresh = true) },
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
          if (isLoading && snapshot.academicYears.isEmpty()) {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(220.dp),
                contentAlignment = Alignment.Center
              ) {
                CircularProgressIndicator()
              }
            }
          }
          if (errorMessage.isNotBlank()) {
            item { AdminAcademicPeriodNoticeCard(errorMessage, Color(0xFFFB7185)) }
          }

          item {
            AdminAcademicPeriodStatusCard(
              activeYear = snapshot.activeAcademicYear,
              activeSemester = snapshot.activeSemester
            )
          }

          item {
            PlaceholderPanel(title = "Tahun Ajaran") {
              if (snapshot.academicYears.isEmpty()) {
                EmptyPlaceholderCard("Belum ada data tahun ajaran.")
              } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  snapshot.academicYears.forEach { year ->
                    AdminAcademicYearCard(
                      year = year,
                      selected = year.rowId == selectedYear?.rowId,
                      isSaving = isSaving,
                      onSelect = { selectedYearId = year.rowId },
                      onEdit = { yearDialogDraft = year },
                      onSetActive = {
                        if (year.active || isSaving) return@AdminAcademicYearCard
                        scope.launch {
                          isSaving = true
                          handleSaveResult(onSetActiveAcademicYear(year.rowId), refreshDashboard = true)
                          isSaving = false
                        }
                      }
                    )
                  }
                }
              }
            }
          }

          item {
            PlaceholderPanel(
              title = "Semester",
              subtitle = selectedYear?.name?.let { "Tahun ajaran: $it" } ?: "Pilih tahun ajaran untuk melihat semester."
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                AdminAcademicInfoPill("${selectedSemesters.size} semester", PrimaryBlue, Modifier.weight(1f))
                OutlinedButton(
                  onClick = { semesterDialogDraft = snapshot.newSemesterDraft(selectedYear?.rowId.orEmpty()) },
                  enabled = selectedYear?.rowId?.isNotBlank() == true && !isSaving
                ) {
                  Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.size(8.dp))
                  Text(t("Tambah Semester"))
                }
              }
              if (selectedYear == null) {
                EmptyPlaceholderCard("Belum ada tahun ajaran untuk menampung semester.")
              } else if (selectedSemesters.isEmpty()) {
                EmptyPlaceholderCard("Belum ada semester untuk tahun ajaran ini.")
              } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  selectedSemesters.forEach { semester ->
                    AdminAcademicSemesterCard(
                      semester = semester,
                      isSaving = isSaving,
                      onEdit = { semesterDialogDraft = semester },
                      onSetActive = {
                        if (semester.active || isSaving) return@AdminAcademicSemesterCard
                        scope.launch {
                          isSaving = true
                          handleSaveResult(onSetActiveSemester(semester.rowId), refreshDashboard = true)
                          isSaving = false
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
  }
}

@Composable
private fun AdminAcademicPeriodStatusCard(
  activeYear: AdminAcademicYear?,
  activeSemester: AdminAcademicSemester?
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x1A0F172A), spotColor = Color(0x1A0F172A))
      .background(Brush.linearGradient(listOf(CardGradientStart, CardGradientEnd)), RoundedCornerShape(18.dp))
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(18.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = t("Periode Aktif"),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    AdminAcademicSimpleRow("Tahun ajaran", activeYear?.name.orEmpty().ifBlank { "-" })
    AdminAcademicSimpleRow("Semester", activeSemester?.name.orEmpty().ifBlank { "-" })
  }
}

@Composable
private fun AdminAcademicYearCard(
  year: AdminAcademicYear,
  selected: Boolean,
  isSaving: Boolean,
  onSelect: () -> Unit,
  onEdit: () -> Unit,
  onSetActive: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(if (selected) PrimaryBlue.copy(alpha = 0.10f) else SoftPanel.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
      .border(1.dp, if (selected) PrimaryBlue.copy(alpha = 0.38f) else CardBorder, RoundedCornerShape(16.dp))
      .clickable(onClick = onSelect)
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = year.name.ifBlank { "Tahun ajaran" },
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        AdminAcademicDateText(year.startDateIso, year.endDateIso)
      }
      if (year.active) AdminAcademicInfoPill("Aktif", SuccessTint)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(
        onClick = onEdit,
        enabled = !isSaving,
        modifier = Modifier.weight(1f)
      ) {
        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.size(6.dp))
        Text(t("Edit"))
      }
      Button(
        onClick = onSetActive,
        enabled = !year.active && !isSaving,
        modifier = Modifier.weight(1f)
      ) {
        Text(t(if (year.active) "Aktif" else "Jadikan Aktif"))
      }
    }
  }
}

@Composable
private fun AdminAcademicSemesterCard(
  semester: AdminAcademicSemester,
  isSaving: Boolean,
  onEdit: () -> Unit,
  onSetActive: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(SoftPanel.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = semester.name.ifBlank { "Semester" },
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        AdminAcademicDateText(semester.startDateIso, semester.endDateIso)
      }
      if (semester.active) AdminAcademicInfoPill("Aktif", SuccessTint)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(
        onClick = onEdit,
        enabled = !isSaving,
        modifier = Modifier.weight(1f)
      ) {
        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.size(6.dp))
        Text(t("Edit"))
      }
      Button(
        onClick = onSetActive,
        enabled = !semester.active && !isSaving,
        modifier = Modifier.weight(1f)
      ) {
        Text(t(if (semester.active) "Aktif" else "Jadikan Aktif"))
      }
    }
  }
}

@Composable
private fun AdminAcademicYearDialog(
  draft: AdminAcademicYear,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (AdminAcademicYear) -> Unit
) {
  var name by rememberSaveable(draft.rowId, draft.name) { mutableStateOf(draft.name) }
  var startDate by rememberSaveable(draft.rowId, draft.startDateIso) { mutableStateOf(draft.startDateIso) }
  var endDate by rememberSaveable(draft.rowId, draft.endDateIso) { mutableStateOf(draft.endDateIso) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(t(if (draft.rowId.isBlank()) "Tambah Tahun Ajaran" else "Edit Tahun Ajaran")) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AdminAcademicDialogField("Nama tahun ajaran", name, { name = it })
        AdminAcademicDialogField("Tanggal mulai", startDate, { startDate = it }, "YYYY-MM-DD")
        AdminAcademicDialogField("Tanggal selesai", endDate, { endDate = it }, "YYYY-MM-DD")
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(draft.copy(name = name, startDateIso = startDate, endDateIso = endDate)) },
        enabled = !isSaving
      ) {
        Text(t(if (isSaving) "Menyimpan..." else "Simpan"))
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
private fun AdminAcademicSemesterDialog(
  draft: AdminAcademicSemester,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (AdminAcademicSemester) -> Unit
) {
  var name by rememberSaveable(draft.rowId, draft.name) { mutableStateOf(draft.name) }
  var startDate by rememberSaveable(draft.rowId, draft.startDateIso) { mutableStateOf(draft.startDateIso) }
  var endDate by rememberSaveable(draft.rowId, draft.endDateIso) { mutableStateOf(draft.endDateIso) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(t(if (draft.rowId.isBlank()) "Tambah Semester" else "Edit Semester")) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AdminAcademicDialogField("Nama semester", name, { name = it })
        AdminAcademicDialogField("Tanggal mulai", startDate, { startDate = it }, "YYYY-MM-DD")
        AdminAcademicDialogField("Tanggal selesai", endDate, { endDate = it }, "YYYY-MM-DD")
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(draft.copy(name = name, startDateIso = startDate, endDateIso = endDate)) },
        enabled = !isSaving
      ) {
        Text(t(if (isSaving) "Menyimpan..." else "Simpan"))
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
private fun AdminAcademicDialogField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String = ""
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    label = { Text(t(label)) },
    placeholder = if (placeholder.isBlank()) null else ({ Text(placeholder) }),
    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    shape = RoundedCornerShape(14.dp)
  )
}

@Composable
private fun AdminAcademicPeriodTopBar(
  title: String,
  onMenuClick: () -> Unit,
  onActionClick: () -> Unit,
  modifier: Modifier = Modifier,
  actionIcon: ImageVector,
  actionContentDescription: String
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    AdminAcademicTopButton(
      icon = Icons.Outlined.Menu,
      contentDescription = "Buka sidebar",
      onClick = onMenuClick
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
    AdminAcademicTopButton(
      icon = actionIcon,
      contentDescription = actionContentDescription,
      onClick = onActionClick
    )
  }
}

@Composable
private fun AdminAcademicTopButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(42.dp)
      .background(CardBackground.copy(alpha = 0.86f), CircleShape)
      .border(1.dp, CardBorder, CircleShape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = PrimaryBlueDark,
      modifier = Modifier.size(22.dp)
    )
  }
}

@Composable
private fun AdminAcademicSimpleRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(t(label), style = MaterialTheme.typography.bodySmall, color = SubtleInk)
    Text(
      t(value),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(start = 12.dp)
    )
  }
}

@Composable
private fun AdminAcademicDateText(startDate: String, endDate: String) {
  val text = listOf(startDate, endDate).filter { it.isNotBlank() }.joinToString(" - ")
  if (text.isNotBlank()) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun AdminAcademicInfoPill(
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
private fun AdminAcademicPeriodNoticeCard(message: String, tone: Color = WarmAccent) {
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

private val AdminAcademicPeriodEmptyYear = AdminAcademicYear(
  rowId = "",
  name = "",
  startDateIso = "",
  endDateIso = "",
  active = false
)

private fun AdminAcademicPeriodSnapshot.newSemesterDraft(yearId: String): AdminAcademicSemester {
  val year = academicYears.firstOrNull { it.rowId == yearId }
    ?: activeAcademicYear
    ?: academicYears.firstOrNull()
  return AdminAcademicSemester(
    rowId = "",
    academicYearId = year?.rowId.orEmpty(),
    academicYearName = year?.name.orEmpty(),
    name = "",
    startDateIso = "",
    endDateIso = "",
    active = false
  )
}
