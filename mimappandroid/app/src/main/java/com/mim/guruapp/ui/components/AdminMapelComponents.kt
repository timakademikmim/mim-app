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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.remote.AdminMapelAcademicYear
import com.mim.guruapp.data.remote.AdminMapelDistribution
import com.mim.guruapp.data.remote.AdminMapelLoadResult
import com.mim.guruapp.data.remote.AdminMapelSaveResult
import com.mim.guruapp.data.remote.AdminMapelSemesterOption
import com.mim.guruapp.data.remote.AdminMapelSnapshot
import com.mim.guruapp.data.remote.AdminMapelSubject
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
fun AdminMapelScreen(
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadMapel: suspend () -> AdminMapelLoadResult,
  onSaveSubject: suspend (AdminMapelSubject) -> AdminMapelSaveResult,
  onSaveDistribution: suspend (AdminMapelDistribution) -> AdminMapelSaveResult,
  modifier: Modifier = Modifier
) {
  var snapshot by remember { mutableStateOf(AdminMapelSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())) }
  var selectedTab by rememberSaveable { mutableStateOf(AdminMapelTab.Subjects.name) }
  var selectedYearId by rememberSaveable { mutableStateOf(AdminMapelYearFilterUnsetKey) }
  var selectedSemesterId by rememberSaveable { mutableStateOf(AdminMapelSemesterFilterUnsetKey) }
  var query by rememberSaveable { mutableStateOf("") }
  var subjectDraft by remember { mutableStateOf<AdminMapelSubject?>(null) }
  var distributionDraft by remember { mutableStateOf<AdminMapelDistribution?>(null) }
  var isLoading by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  fun applySnapshot(nextSnapshot: AdminMapelSnapshot) {
    snapshot = nextSnapshot
    val yearIds = nextSnapshot.academicYears.map { it.id }.toSet()
    selectedYearId = when {
      selectedYearId == AdminMapelYearFilterUnsetKey ->
        nextSnapshot.activeAcademicYearId.ifBlank { nextSnapshot.academicYears.firstOrNull()?.id.orEmpty() }
      selectedYearId == AdminMapelAllYearFilterKey -> AdminMapelAllYearFilterKey
      selectedYearId in yearIds -> selectedYearId
      else -> nextSnapshot.activeAcademicYearId.ifBlank { nextSnapshot.academicYears.firstOrNull()?.id.orEmpty() }
    }
    val effectiveYear = selectedYearId.takeUnless { it == AdminMapelAllYearFilterKey || it == AdminMapelYearFilterUnsetKey }
    val semesterOptions = nextSnapshot.semesters.filter { effectiveYear.isNullOrBlank() || it.academicYearId == effectiveYear }
    val semesterIds = semesterOptions.map { it.id }.toSet()
    selectedSemesterId = when {
      selectedSemesterId == AdminMapelSemesterFilterUnsetKey ->
        semesterOptions.firstOrNull { it.active }?.id ?: semesterOptions.firstOrNull()?.id.orEmpty()
      selectedSemesterId == AdminMapelAllSemesterFilterKey -> AdminMapelAllSemesterFilterKey
      selectedSemesterId in semesterIds -> selectedSemesterId
      else -> semesterOptions.firstOrNull { it.active }?.id ?: semesterOptions.firstOrNull()?.id.orEmpty()
    }
  }

  fun loadMapel(showGlobalRefresh: Boolean = false) {
    scope.launch {
      isLoading = true
      errorMessage = ""
      if (showGlobalRefresh) onRefresh()
      when (val result = onLoadMapel()) {
        is AdminMapelLoadResult.Success -> applySnapshot(result.snapshot)
        is AdminMapelLoadResult.Error -> errorMessage = result.message
      }
      isLoading = false
    }
  }

  LaunchedEffect(Unit) {
    loadMapel()
  }

  if (subjectDraft != null) {
    AdminMapelSubjectDialog(
      draft = subjectDraft ?: snapshot.newSubjectDraft(selectedYearId),
      years = snapshot.academicYears,
      isSaving = isSaving,
      onDismiss = { if (!isSaving) subjectDraft = null },
      onConfirm = { subject ->
        scope.launch {
          isSaving = true
          when (val result = onSaveSubject(subject)) {
            is AdminMapelSaveResult.Success -> {
              subjectDraft = null
              applySnapshot(result.snapshot)
              snackbarHostState.showSnackbar(result.message)
            }
            is AdminMapelSaveResult.Error -> snackbarHostState.showSnackbar(result.message)
          }
          isSaving = false
        }
      }
    )
  }

  if (distributionDraft != null) {
    AdminMapelDistributionDialog(
      draft = distributionDraft ?: snapshot.newDistributionDraft(selectedYearId, selectedSemesterId),
      snapshot = snapshot,
      selectedYearId = selectedYearId.takeUnless { it == AdminMapelAllYearFilterKey || it == AdminMapelYearFilterUnsetKey }.orEmpty(),
      isSaving = isSaving,
      onDismiss = { if (!isSaving) distributionDraft = null },
      onConfirm = { distribution ->
        scope.launch {
          isSaving = true
          when (val result = onSaveDistribution(distribution)) {
            is AdminMapelSaveResult.Success -> {
              distributionDraft = null
              applySnapshot(result.snapshot)
              snackbarHostState.showSnackbar(result.message)
            }
            is AdminMapelSaveResult.Error -> snackbarHostState.showSnackbar(result.message)
          }
          isSaving = false
        }
      }
    )
  }

  BackHandler(enabled = subjectDraft != null || distributionDraft != null) {
    if (!isSaving) {
      subjectDraft = null
      distributionDraft = null
    }
  }

  val tab = runCatching { AdminMapelTab.valueOf(selectedTab) }.getOrDefault(AdminMapelTab.Subjects)
  val effectiveYearId = selectedYearId
    .takeUnless { it == AdminMapelYearFilterUnsetKey }
    ?: snapshot.activeAcademicYearId.ifBlank { snapshot.academicYears.firstOrNull()?.id.orEmpty() }
  val yearsForFilter = listOf(AdminMapelAcademicYear(AdminMapelAllYearFilterKey, "Semua tahun ajaran", false)) + snapshot.academicYears
  val semesterOptionsForFilter = snapshot.semesters
    .filter { effectiveYearId == AdminMapelAllYearFilterKey || it.academicYearId == effectiveYearId }
  val semesterFilterOptions = listOf(AdminMapelSemesterOption(AdminMapelAllSemesterFilterKey, "Semua semester", "", "", false)) + semesterOptionsForFilter

  val filteredSubjects = snapshot.subjects
    .filter { subject ->
      effectiveYearId == AdminMapelAllYearFilterKey ||
        subject.academicYearId == effectiveYearId ||
        subject.academicYearId.isBlank()
    }
    .filter { subject ->
      val needle = query.trim()
      needle.isBlank() ||
        subject.name.contains(needle, ignoreCase = true) ||
        subject.category.contains(needle, ignoreCase = true) ||
        subject.levels.displayAdminMapelLevels().contains(needle, ignoreCase = true)
    }
    .sortedWith(compareBy<AdminMapelSubject> { it.name.lowercase() }.thenBy { it.category.lowercase() })

  val filteredDistributions = snapshot.distributions
    .filter { distribution ->
      effectiveYearId == AdminMapelAllYearFilterKey || distribution.academicYearId == effectiveYearId
    }
    .filter { distribution ->
      selectedSemesterId == AdminMapelSemesterFilterUnsetKey ||
        selectedSemesterId == AdminMapelAllSemesterFilterKey ||
        selectedSemesterId.isBlank() ||
        distribution.semesterId == selectedSemesterId
    }
    .filter { distribution ->
      val needle = query.trim()
      needle.isBlank() ||
        distribution.subjectName.contains(needle, ignoreCase = true) ||
        distribution.className.contains(needle, ignoreCase = true) ||
        distribution.teacherName.contains(needle, ignoreCase = true) ||
        distribution.semesterName.contains(needle, ignoreCase = true)
    }
    .sortedWith(
      compareBy<AdminMapelDistribution> { it.classLevel.adminMapelLevelNumberForUi() ?: 999 }
        .thenBy { it.className.lowercase() }
        .thenBy { it.subjectName.lowercase() }
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
      AdminMapelTopBar(
        title = "Mapel",
        onMenuClick = onMenuClick,
        actionIcon = Icons.Outlined.Add,
        actionContentDescription = if (tab == AdminMapelTab.Subjects) "Tambah mapel" else "Tambah distribusi",
        onActionClick = {
          if (tab == AdminMapelTab.Subjects) {
            subjectDraft = snapshot.newSubjectDraft(effectiveYearId)
          } else {
            distributionDraft = snapshot.newDistributionDraft(effectiveYearId, selectedSemesterId)
          }
        }
      )

      AdminMapelSearchBar(
        value = query,
        onValueChange = { query = it }
      )

      PullToRefreshBox(
        isRefreshing = isRefreshing || isLoading,
        onRefresh = { loadMapel(showGlobalRefresh = true) },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .navigationBarsPadding()
      ) {
        AdminMapelListContent(
          isLoading = isLoading,
          hasInitialData = snapshot.subjects.isNotEmpty() || snapshot.distributions.isNotEmpty(),
          errorMessage = errorMessage,
          tab = tab,
          yearsForFilter = yearsForFilter,
          selectedYearId = effectiveYearId,
          selectedYearLabel = yearsForFilter.firstOrNull { it.id == effectiveYearId }?.name
            ?: snapshot.activeAcademicYearName.ifBlank { "-" },
          semesterFilterOptions = semesterFilterOptions,
          selectedSemesterId = selectedSemesterId,
          selectedSemesterLabel = semesterFilterOptions.firstOrNull { it.id == selectedSemesterId }?.name
            ?: snapshot.activeSemesterName.ifBlank { "-" },
          subjects = filteredSubjects,
          distributions = filteredDistributions,
          onTabSelected = { selectedTab = it.name },
          onYearSelected = {
            selectedYearId = it
            selectedSemesterId = AdminMapelSemesterFilterUnsetKey
          },
          onSemesterSelected = { selectedSemesterId = it },
          onSubjectClick = { subjectDraft = it },
          onDistributionClick = { distributionDraft = it }
        )
      }
    }
  }
}

@Composable
private fun AdminMapelListContent(
  isLoading: Boolean,
  hasInitialData: Boolean,
  errorMessage: String,
  tab: AdminMapelTab,
  yearsForFilter: List<AdminMapelAcademicYear>,
  selectedYearId: String,
  selectedYearLabel: String,
  semesterFilterOptions: List<AdminMapelSemesterOption>,
  selectedSemesterId: String,
  selectedSemesterLabel: String,
  subjects: List<AdminMapelSubject>,
  distributions: List<AdminMapelDistribution>,
  onTabSelected: (AdminMapelTab) -> Unit,
  onYearSelected: (String) -> Unit,
  onSemesterSelected: (String) -> Unit,
  onSubjectClick: (AdminMapelSubject) -> Unit,
  onDistributionClick: (AdminMapelDistribution) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(bottom = 112.dp)
  ) {
    if (isLoading && !hasInitialData) {
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
      item { AdminMapelNoticeCard(errorMessage, Color(0xFFFB7185)) }
    }

    item {
      AdminMapelSummaryCard(
        selectedYearLabel = selectedYearLabel,
        selectedSemesterLabel = selectedSemesterLabel,
        subjectCount = subjects.size,
        distributionCount = distributions.size
      )
    }

    item {
      AdminMapelTabBar(
        selectedTab = tab,
        onSelected = onTabSelected
      )
    }

    item {
      AdminMapelFilterPanel(
        years = yearsForFilter,
        selectedYearId = selectedYearId,
        onYearSelected = onYearSelected,
        semesterOptions = semesterFilterOptions,
        selectedSemesterId = selectedSemesterId,
        onSemesterSelected = onSemesterSelected,
        showSemester = tab == AdminMapelTab.Distribution
      )
    }

    if (tab == AdminMapelTab.Subjects) {
      if (!isLoading && subjects.isEmpty()) {
        item {
          EmptyPlaceholderCard(
            message = "Belum ada mapel. Tambahkan mapel untuk tahun ajaran yang dipilih."
          )
        }
      }
      items(subjects, key = { it.rowId }) { subject ->
        AdminMapelSubjectCard(
          subject = subject,
          onClick = { onSubjectClick(subject) }
        )
      }
    } else {
      if (!isLoading && distributions.isEmpty()) {
        item {
          EmptyPlaceholderCard(
            message = "Belum ada distribusi mapel. Pasangkan mapel ke kelas, semester, dan guru pengampu."
          )
        }
      }
      items(distributions, key = { it.rowId }) { distribution ->
        AdminMapelDistributionCard(
          distribution = distribution,
          onClick = { onDistributionClick(distribution) }
        )
      }
    }
  }
}

@Composable
private fun AdminMapelTopBar(
  title: String,
  onMenuClick: () -> Unit,
  actionIcon: ImageVector,
  actionContentDescription: String,
  onActionClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    AdminMapelCircleButton(
      icon = Icons.Outlined.Menu,
      contentDescription = "Buka menu",
      onClick = onMenuClick
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Kelola data mapel dan distribusinya",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    AdminMapelCircleButton(
      icon = actionIcon,
      contentDescription = actionContentDescription,
      onClick = onActionClick,
      background = PrimaryBlue,
      contentColor = Color.White
    )
  }
}

@Composable
private fun AdminMapelCircleButton(
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
private fun AdminMapelSearchBar(
  value: String,
  onValueChange: (String) -> Unit
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier.fillMaxWidth(),
    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
    placeholder = { Text("Cari mapel, kelas, guru, semester") },
    singleLine = true,
    shape = RoundedCornerShape(16.dp)
  )
}

@Composable
private fun AdminMapelSummaryCard(
  selectedYearLabel: String,
  selectedSemesterLabel: String,
  subjectCount: Int,
  distributionCount: Int
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(22.dp), ambientColor = PrimaryBlue.copy(alpha = 0.10f), spotColor = PrimaryBlue.copy(alpha = 0.10f))
      .background(
        Brush.horizontalGradient(listOf(CardGradientStart, CardGradientEnd)),
        RoundedCornerShape(22.dp)
      )
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(22.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .background(PrimaryBlue.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Outlined.Book, contentDescription = null, tint = PrimaryBlue)
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = selectedYearLabel.ifBlank { "-" },
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Semester: ${selectedSemesterLabel.ifBlank { "-" }}",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      AdminMapelInfoPill("$subjectCount mapel", SuccessTint)
      AdminMapelInfoPill("$distributionCount distribusi", WarmAccent.copy(alpha = 0.22f))
    }
  }
}

@Composable
private fun AdminMapelTabBar(
  selectedTab: AdminMapelTab,
  onSelected: (AdminMapelTab) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(SoftPanel, RoundedCornerShape(18.dp))
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    AdminMapelTab.values().forEach { tab ->
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
private fun AdminMapelFilterPanel(
  years: List<AdminMapelAcademicYear>,
  selectedYearId: String,
  onYearSelected: (String) -> Unit,
  semesterOptions: List<AdminMapelSemesterOption>,
  selectedSemesterId: String,
  onSemesterSelected: (String) -> Unit,
  showSemester: Boolean
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground, RoundedCornerShape(18.dp))
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(18.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    AdminMapelOptionDropdown(
      label = "Tahun ajaran",
      selectedLabel = years.firstOrNull { it.id == selectedYearId }?.name ?: "Pilih tahun ajaran",
      options = years.map { it.id to (it.name + if (it.active) " (aktif)" else "") },
      onSelected = onYearSelected
    )
    if (showSemester) {
      AdminMapelOptionDropdown(
        label = "Semester",
        selectedLabel = semesterOptions.firstOrNull { it.id == selectedSemesterId }?.name ?: "Pilih semester",
        options = semesterOptions.map { it.id to (it.name + if (it.active) " (aktif)" else "") },
        onSelected = onSemesterSelected
      )
    }
  }
}

@Composable
private fun AdminMapelSubjectCard(
  subject: AdminMapelSubject,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground, RoundedCornerShape(18.dp))
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = subject.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = listOf(
            subject.category.ifBlank { "Tanpa kategori" },
            subject.levels.displayAdminMapelLevels(),
            subject.academicYearName.ifBlank { "Global" }
          ).joinToString(" • "),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
      Icon(Icons.Outlined.Edit, contentDescription = null, tint = PrimaryBlue)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      AdminMapelInfoPill("${subject.distributionCount} kelas", SuccessTint)
      if (subject.kkm.isNotBlank()) AdminMapelInfoPill("KKM ${subject.kkm}", SoftPanel)
    }
  }
}

@Composable
private fun AdminMapelDistributionCard(
  distribution: AdminMapelDistribution,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBackground, RoundedCornerShape(18.dp))
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = distribution.subjectName.withAdminMapelCategory(distribution.subjectCategory),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${distribution.className} • ${distribution.semesterName}",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Icon(Icons.Outlined.Edit, contentDescription = null, tint = PrimaryBlue)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      AdminMapelInfoPill(distribution.level.displayAdminMapelLevels(), SuccessTint)
      AdminMapelInfoPill(distribution.teacherName.ifBlank { "Belum diampu" }, SoftPanel)
    }
  }
}

@Composable
private fun AdminMapelSubjectDialog(
  draft: AdminMapelSubject,
  years: List<AdminMapelAcademicYear>,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (AdminMapelSubject) -> Unit
) {
  var name by remember(draft.rowId) { mutableStateOf(draft.name) }
  var category by remember(draft.rowId) { mutableStateOf(draft.category) }
  var kkm by remember(draft.rowId) { mutableStateOf(draft.kkm) }
  var yearId by remember(draft.rowId) { mutableStateOf(draft.academicYearId.ifBlank { years.firstOrNull { it.active }?.id ?: years.firstOrNull()?.id.orEmpty() }) }
  var smp by remember(draft.rowId) { mutableStateOf(draft.levels.hasAdminMapelLevel("smp")) }
  var sma by remember(draft.rowId) { mutableStateOf(draft.levels.hasAdminMapelLevel("sma")) }
  val scrollState = rememberScrollState()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (draft.rowId.isBlank()) "Tambah Mapel" else "Edit Mapel") },
    text = {
      Column(
        modifier = Modifier
          .heightIn(max = 520.dp)
          .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        AdminMapelOptionDropdown(
          label = "Tahun ajaran",
          selectedLabel = years.firstOrNull { it.id == yearId }?.name ?: "Pilih tahun ajaran",
          options = years.map { it.id to (it.name + if (it.active) " (aktif)" else "") },
          onSelected = { yearId = it }
        )
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Nama mapel") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )
        OutlinedTextField(
          value = category,
          onValueChange = { category = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Kategori") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )
        OutlinedTextField(
          value = kkm,
          onValueChange = { kkm = it.filter(Char::isDigit).take(3) },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("KKM") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(SoftPanel, RoundedCornerShape(14.dp))
            .padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "Tingkatan",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          AdminMapelLevelCheckbox("SMP", smp) { smp = it }
          AdminMapelLevelCheckbox("SMA", sma) { sma = it }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val levels = buildList {
            if (smp) add("smp")
            if (sma) add("sma")
          }.joinToString(",")
          onConfirm(
            draft.copy(
              name = name.trim(),
              category = category.trim(),
              kkm = kkm.trim(),
              academicYearId = yearId,
              levels = levels
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
private fun AdminMapelDistributionDialog(
  draft: AdminMapelDistribution,
  snapshot: AdminMapelSnapshot,
  selectedYearId: String,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (AdminMapelDistribution) -> Unit
) {
  val yearId = draft.academicYearId.ifBlank { selectedYearId.ifBlank { snapshot.activeAcademicYearId } }
  var classId by remember(draft.rowId) { mutableStateOf(draft.classId.ifBlank { snapshot.classes.firstOrNull { it.academicYearId == yearId }?.id.orEmpty() }) }
  var subjectId by remember(draft.rowId) { mutableStateOf(draft.subjectId.ifBlank { snapshot.subjects.firstOrNull { it.academicYearId == yearId || it.academicYearId.isBlank() }?.rowId.orEmpty() }) }
  var teacherId by remember(draft.rowId) { mutableStateOf(draft.teacherId) }
  var semesterId by remember(draft.rowId) { mutableStateOf(draft.semesterId.ifBlank { snapshot.semesters.firstOrNull { it.academicYearId == yearId && it.active }?.id ?: snapshot.semesters.firstOrNull { it.academicYearId == yearId }?.id.orEmpty() }) }
  val classOptions = snapshot.classes.filter { yearId.isBlank() || it.academicYearId == yearId }
  val subjectOptions = snapshot.subjects.filter { yearId.isBlank() || it.academicYearId == yearId || it.academicYearId.isBlank() }
  val semesterOptions = snapshot.semesters.filter { yearId.isBlank() || it.academicYearId == yearId }
  val selectedClass = snapshot.classes.firstOrNull { it.id == classId }
  val selectedSubject = snapshot.subjects.firstOrNull { it.rowId == subjectId }
  val selectedTeacher = snapshot.teachers.firstOrNull { it.id == teacherId }
  val selectedSemester = snapshot.semesters.firstOrNull { it.id == semesterId }
  val scrollState = rememberScrollState()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (draft.rowId.isBlank()) "Tambah Distribusi" else "Edit Distribusi") },
    text = {
      Column(
        modifier = Modifier
          .heightIn(max = 520.dp)
          .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        AdminMapelOptionDropdown(
          label = "Kelas",
          selectedLabel = selectedClass?.name ?: "Pilih kelas",
          options = classOptions.map { it.id to "${it.name} (${it.level.displayAdminClassLevel()})" },
          onSelected = { classId = it }
        )
        AdminMapelOptionDropdown(
          label = "Mapel",
          selectedLabel = selectedSubject?.name?.withAdminMapelCategory(selectedSubject.category) ?: "Pilih mapel",
          options = subjectOptions.map { it.rowId to it.name.withAdminMapelCategory(it.category) },
          onSelected = { subjectId = it }
        )
        AdminMapelOptionDropdown(
          label = "Guru pengampu",
          selectedLabel = selectedTeacher?.name ?: "Belum diampu",
          options = listOf("" to "Belum diampu") + snapshot.teachers.map { it.id to it.name },
          onSelected = { teacherId = it }
        )
        AdminMapelOptionDropdown(
          label = "Semester",
          selectedLabel = selectedSemester?.name ?: "Pilih semester",
          options = semesterOptions.map { it.id to (it.name + if (it.active) " (aktif)" else "") },
          onSelected = { semesterId = it }
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            draft.copy(
              classId = classId,
              className = selectedClass?.name.orEmpty(),
              classLevel = selectedClass?.level.orEmpty(),
              subjectId = subjectId,
              subjectName = selectedSubject?.name.orEmpty(),
              subjectCategory = selectedSubject?.category.orEmpty(),
              teacherId = teacherId,
              teacherName = selectedTeacher?.name.orEmpty(),
              semesterId = semesterId,
              semesterName = selectedSemester?.name.orEmpty(),
              academicYearId = yearId,
              level = selectedClass?.level.orEmpty().adminMapelStageForUi()
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
private fun AdminMapelOptionDropdown(
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
      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
      ) {
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
private fun AdminMapelLevelCheckbox(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onCheckedChange(!checked) },
    verticalAlignment = Alignment.CenterVertically
  ) {
    Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
  }
}

@Composable
private fun AdminMapelInfoPill(
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
private fun AdminMapelNoticeCard(
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

private enum class AdminMapelTab(val label: String) {
  Subjects("Data Mapel"),
  Distribution("Distribusi")
}

private const val AdminMapelAllYearFilterKey = "__all_years__"
private const val AdminMapelYearFilterUnsetKey = "__unset_year__"
private const val AdminMapelAllSemesterFilterKey = "__all_semesters__"
private const val AdminMapelSemesterFilterUnsetKey = "__unset_semester__"

private fun AdminMapelSnapshot.newSubjectDraft(selectedYearId: String): AdminMapelSubject {
  val yearId = selectedYearId
    .takeUnless { it == AdminMapelAllYearFilterKey || it == AdminMapelYearFilterUnsetKey }
    ?: activeAcademicYearId.ifBlank { academicYears.firstOrNull()?.id.orEmpty() }
  return AdminMapelSubject(
    rowId = "",
    name = "",
    category = "",
    levels = "",
    academicYearId = yearId,
    academicYearName = academicYears.firstOrNull { it.id == yearId }?.name.orEmpty(),
    kkm = "",
    distributionCount = 0
  )
}

private fun AdminMapelSnapshot.newDistributionDraft(
  selectedYearId: String,
  selectedSemesterId: String
): AdminMapelDistribution {
  val yearId = selectedYearId
    .takeUnless { it == AdminMapelAllYearFilterKey || it == AdminMapelYearFilterUnsetKey }
    ?: activeAcademicYearId.ifBlank { academicYears.firstOrNull()?.id.orEmpty() }
  val kelas = classes.firstOrNull { it.academicYearId == yearId } ?: classes.firstOrNull()
  val subject = subjects.firstOrNull { it.academicYearId == yearId || it.academicYearId.isBlank() } ?: subjects.firstOrNull()
  val semester = semesters.firstOrNull {
    it.id == selectedSemesterId && (yearId.isBlank() || it.academicYearId == yearId)
  } ?: semesters.firstOrNull { it.academicYearId == yearId && it.active }
    ?: semesters.firstOrNull { it.academicYearId == yearId }
    ?: semesters.firstOrNull()
  return AdminMapelDistribution(
    rowId = "",
    classId = kelas?.id.orEmpty(),
    className = kelas?.name.orEmpty(),
    classLevel = kelas?.level.orEmpty(),
    subjectId = subject?.rowId.orEmpty(),
    subjectName = subject?.name.orEmpty(),
    subjectCategory = subject?.category.orEmpty(),
    teacherId = "",
    teacherName = "",
    semesterId = semester?.id.orEmpty(),
    semesterName = semester?.name.orEmpty(),
    academicYearId = yearId,
    academicYearName = academicYears.firstOrNull { it.id == yearId }?.name.orEmpty(),
    level = kelas?.level.orEmpty().adminMapelStageForUi()
  )
}

private fun String.hasAdminMapelLevel(level: String): Boolean {
  return split(',', '|', ';')
    .map { it.trim().lowercase() }
    .contains(level)
}

private fun String.displayAdminMapelLevels(): String {
  val values = split(',', '|', ';')
    .map { it.trim().lowercase() }
    .filter { it.isNotBlank() }
    .distinct()
  if (values.isEmpty()) return "-"
  return values.joinToString(", ") {
    when (it) {
      "smp" -> "SMP"
      "sma" -> "SMA"
      else -> it.uppercase()
    }
  }
}

private fun String.withAdminMapelCategory(category: String): String {
  return if (category.isBlank()) this else "$this ($category)"
}

private fun String.displayAdminClassLevel(): String {
  return when (adminMapelLevelNumberForUi()) {
    7 -> "VII"
    8 -> "VIII"
    9 -> "IX"
    10 -> "X"
    11 -> "XI"
    12 -> "XII"
    else -> ifBlank { "-" }
  }
}

private fun String.adminMapelStageForUi(): String {
  return when (adminMapelLevelNumberForUi()) {
    7, 8, 9 -> "smp"
    10, 11, 12 -> "sma"
    else -> when (trim().lowercase()) {
      "smp", "mts" -> "smp"
      "sma", "ma" -> "sma"
      else -> ""
    }
  }
}

private fun String.adminMapelLevelNumberForUi(): Int? {
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
