package com.mim.guruapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mim.guruapp.ScoreSaveOutcome
import com.mim.guruapp.data.model.MapelPatronMateriSnapshot
import com.mim.guruapp.data.model.MapelScoreSnapshot
import com.mim.guruapp.data.model.ScoreDetailRow
import com.mim.guruapp.data.model.ScoreStudent
import com.mim.guruapp.data.model.SubjectOverview
import com.mim.guruapp.data.model.SyncBannerState
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.HighlightCard
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SubtleInk
import com.mim.guruapp.ui.theme.SuccessTint
import com.mim.guruapp.ui.theme.WarmAccent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputNilaiScreen(
  subjects: List<SubjectOverview>,
  scoreSnapshots: List<MapelScoreSnapshot>,
  patronMateriSnapshots: List<MapelPatronMateriSnapshot>,
  syncBanner: SyncBannerState,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadScores: suspend (String, SubjectOverview) -> MapelScoreSnapshot?,
  onLoadPatronMateri: suspend (String, SubjectOverview) -> MapelPatronMateriSnapshot?,
  onSaveScoresBatch: suspend (String, SubjectOverview, List<ScoreStudent>) -> ScoreSaveOutcome,
  activeInputPage: Int = 0,
  showTopBar: Boolean = true,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  var dateIso by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
  var selectedClassName by rememberSaveable { mutableStateOf("") }
  var selectedSubjectId by rememberSaveable { mutableStateOf("") }
  var selectedMetricKey by rememberSaveable { mutableStateOf(InputNilaiMetricOptions.first().key) }
  var materiText by rememberSaveable { mutableStateOf("") }
  var isMateriEditedByUser by rememberSaveable { mutableStateOf(false) }
  var scoreSnapshot by remember(selectedSubjectId, scoreSnapshots) {
    mutableStateOf(scoreSnapshots.firstOrNull { it.distribusiId == selectedSubjectId })
  }
  var patronMateriSnapshot by remember(selectedSubjectId, patronMateriSnapshots) {
    mutableStateOf(patronMateriSnapshots.firstOrNull { it.distribusiId == selectedSubjectId })
  }
  var isLoadingStudents by remember(selectedSubjectId) { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var feedbackMessage by remember { mutableStateOf<String?>(null) }
  var draftValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

  val classOptions = remember(subjects) {
    subjects.map { it.className }.filter { it.isNotBlank() }.distinct().sorted()
  }
  val subjectsForClass = remember(subjects, selectedClassName) {
    subjects
      .filter { selectedClassName.isBlank() || it.className == selectedClassName }
      .distinctBy { it.id }
      .sortedWith(compareBy<SubjectOverview> { it.title.lowercase() }.thenBy { it.semester.lowercase() })
  }
  val selectedSubject = subjectsForClass.firstOrNull { it.id == selectedSubjectId } ?: subjectsForClass.firstOrNull()
  val selectedMetric = InputNilaiMetricOptions.firstOrNull { it.key == selectedMetricKey }
    ?: InputNilaiMetricOptions.first()
  val students = scoreSnapshot?.students.orEmpty()
  val patronOptions = remember(patronMateriSnapshot, patronMateriSnapshots, selectedSubjectId) {
    (patronMateriSnapshot ?: patronMateriSnapshots.firstOrNull { it.distribusiId == selectedSubjectId })
      ?.items
      .orEmpty()
      .map { it.text.trim() }
      .filter { it.isNotBlank() }
      .distinct()
  }
  val materiSuggestion = remember(students, dateIso, selectedMetricKey) {
    buildInputNilaiMateriSuggestion(students, dateIso, selectedMetricKey)
  }

  LaunchedEffect(subjectsForClass, selectedClassName, selectedSubjectId) {
    val nextClassName = selectedClassName.takeIf { it in classOptions }
      ?: classOptions.firstOrNull().orEmpty()
    val nextSubjects = subjects
      .filter { nextClassName.isBlank() || it.className == nextClassName }
      .distinctBy { it.id }
      .sortedWith(compareBy<SubjectOverview> { it.title.lowercase() }.thenBy { it.semester.lowercase() })
    val nextSubjectId = selectedSubjectId.takeIf { id -> nextSubjects.any { it.id == id } }
      ?: nextSubjects.firstOrNull()?.id.orEmpty()

    if (selectedClassName != nextClassName) selectedClassName = nextClassName
    if (selectedSubjectId != nextSubjectId) selectedSubjectId = nextSubjectId
  }

  LaunchedEffect(selectedSubjectId, subjects) {
    val subject = subjects.firstOrNull { it.id == selectedSubjectId }
    if (subject == null) {
      scoreSnapshot = null
      isLoadingStudents = false
      return@LaunchedEffect
    }
    isLoadingStudents = true
    scoreSnapshot = scoreSnapshots.firstOrNull { it.distribusiId == selectedSubjectId }
    scoreSnapshot = onLoadScores(subject.id, subject) ?: scoreSnapshot
    isLoadingStudents = false
  }

  LaunchedEffect(selectedSubjectId, subjects) {
    val subject = subjects.firstOrNull { it.id == selectedSubjectId }
    if (subject == null) {
      patronMateriSnapshot = null
      return@LaunchedEffect
    }
    patronMateriSnapshot = patronMateriSnapshots.firstOrNull { it.distribusiId == selectedSubjectId }
    patronMateriSnapshot = onLoadPatronMateri(subject.id, subject) ?: patronMateriSnapshot
  }

  LaunchedEffect(scoreSnapshot?.updatedAt, dateIso, selectedMetricKey) {
    draftValues = students.associate { student ->
      val existing = student.detailRowsByMetric[selectedMetricKey]
        .orEmpty()
        .firstOrNull { it.dateIso == dateIso }
      student.id to existing?.value?.let(::formatInputNilaiNumber).orEmpty()
    }
  }

  LaunchedEffect(materiSuggestion?.text, selectedSubjectId, selectedMetricKey, dateIso) {
    if (!isMateriEditedByUser && materiText.isBlank()) {
      materiText = materiSuggestion?.text.orEmpty()
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    topBar = {
      if (showTopBar) {
        InputNilaiTopBar(
          title = "Input Nilai",
          activeInputPage = activeInputPage,
          onMenuClick = onMenuClick
        )
      }
    }
  ) { innerPadding ->
    PullToRefreshBox(
      isRefreshing = syncBanner.isSyncing,
      onRefresh = onRefresh,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .navigationBarsPadding()
        .background(AppBackground)
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 22.dp)
      ) {
        item {
          InputNilaiFormCard(
            dateIso = dateIso,
            onDateSelect = {
              dateIso = it
              materiText = ""
              isMateriEditedByUser = false
              feedbackMessage = null
            },
            classValue = selectedClassName.ifBlank { "Pilih kelas" },
            classOptions = classOptions,
            onClassSelect = {
              selectedClassName = it
              materiText = ""
              isMateriEditedByUser = false
              feedbackMessage = null
            },
            subjectValue = selectedSubject?.title ?: "Pilih mapel",
            subjectOptions = subjectsForClass,
            onSubjectSelect = {
              selectedSubjectId = it.id
              selectedClassName = it.className
              materiText = ""
              isMateriEditedByUser = false
              feedbackMessage = null
            },
            metricValue = selectedMetric.label,
            metricOptions = InputNilaiMetricOptions,
            onMetricSelect = {
              selectedMetricKey = it.key
              materiText = ""
              isMateriEditedByUser = false
              feedbackMessage = null
            }
          )
        }

        item {
          InputNilaiMateriCard(
            materiText = materiText,
            onMateriChange = {
              materiText = normalizeInputNilaiMaterial(it)
              isMateriEditedByUser = true
              feedbackMessage = null
            },
            materiSuggestion = materiSuggestion,
            onSuggestionClick = {
              materiText = it
              isMateriEditedByUser = true
              feedbackMessage = null
            },
            patronOptions = patronOptions,
            onPatronSelect = {
              materiText = it
              isMateriEditedByUser = true
              feedbackMessage = null
            }
          )
        }

        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Nilai Siswa",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryBlueDark,
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = "${students.size} santri - ${selectedMetric.label}",
                style = MaterialTheme.typography.bodySmall,
                color = SubtleInk
              )
            }
            InputNilaiSaveButton(
              isSaving = isSaving,
              enabled = students.isNotEmpty() && selectedSubject != null,
              onClick = {
                val subject = selectedSubject
                if (subject == null) {
                  feedbackMessage = "Pilih mapel terlebih dahulu."
                  return@InputNilaiSaveButton
                }
                scope.launch {
                  isSaving = true
                  feedbackMessage = null
                  val changedStudents = students.mapNotNull { student ->
                    val rawValue = draftValues[student.id].orEmpty()
                    val parsedValue = if (rawValue.isBlank()) {
                      0.0
                    } else {
                      parseInputNilaiValue(rawValue, selectedMetric.maxValue)
                    }
                    if (parsedValue == null) return@mapNotNull null
                    buildInputNilaiUpdatedStudent(
                      student = student,
                      metric = selectedMetric,
                      dateIso = dateIso,
                      value = parsedValue,
                      material = materiText
                    )
                  }
                  if (changedStudents.isEmpty()) {
                    feedbackMessage = "Nilai yang diisi belum valid."
                    isSaving = false
                    return@launch
                  }

                  val lastOutcome = try {
                    onSaveScoresBatch(subject.id, subject, changedStudents)
                  } catch (_: Exception) {
                    ScoreSaveOutcome(false, "Gagal menyimpan nilai.")
                  }
                  if (lastOutcome.success) {
                    scoreSnapshot = onLoadScores(subject.id, subject) ?: scoreSnapshot
                  }
                  feedbackMessage = lastOutcome.message
                  isSaving = false
                }
              }
            )
          }
        }

        if (feedbackMessage != null) {
          item {
            InputNilaiInfoCard(
              message = feedbackMessage.orEmpty(),
              tone = if (feedbackMessage?.contains("berhasil", ignoreCase = true) == true) SuccessTint else WarmAccent
            )
          }
        }

        when {
          subjects.isEmpty() -> {
            item {
              EmptyPlaceholderCard("Belum ada mapel aktif. Tambahkan mapel dahulu sebelum mengisi nilai.")
            }
          }

          isLoadingStudents -> {
            items(5) { index -> InputNilaiStudentSkeleton(index + 1) }
          }

          students.isEmpty() -> {
            item {
              EmptyPlaceholderCard("Belum ada data santri untuk kelas/mapel ini. Tarik refresh saat koneksi aktif.")
            }
          }

          else -> {
            items(
              items = students,
              key = { it.id }
            ) { student ->
              InputNilaiStudentCard(
                student = student,
                value = draftValues[student.id].orEmpty(),
                onValueChange = { value ->
                  sanitizeInputNilaiValueText(value, selectedMetric.maxValue)?.let { safeValue ->
                    draftValues = draftValues.toMutableMap().apply {
                      put(student.id, safeValue)
                    }
                    feedbackMessage = null
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

@Composable
private fun InputNilaiTopBar(
  title: String,
  activeInputPage: Int,
  onMenuClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp, start = 18.dp, end = 18.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    InputNilaiGlassActionButton(
      icon = Icons.Outlined.Menu,
      contentDescription = "Buka sidebar",
      onClick = onMenuClick
    )

    Box(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        InputNilaiPageIndicator(activeInputPage = activeInputPage)
      }
    }

    Box(modifier = Modifier.size(42.dp))
  }
}

@Composable
private fun InputNilaiPageIndicator(activeInputPage: Int) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(5.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    repeat(2) { index ->
      val active = index == activeInputPage
      Box(
        modifier = Modifier
          .size(if (active) 7.dp else 5.dp)
          .clip(CircleShape)
          .background(if (active) HighlightCard else CardBorder.copy(alpha = 0.9f))
      )
    }
  }
}

@Composable
private fun InputNilaiGlassActionButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(42.dp)
      .background(Color.White.copy(alpha = 0.86f), CircleShape)
      .border(1.dp, CardBorder, CircleShape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = PrimaryBlueDark
    )
  }
}

@Composable
private fun InputNilaiFormCard(
  dateIso: String,
  onDateSelect: (String) -> Unit,
  classValue: String,
  classOptions: List<String>,
  onClassSelect: (String) -> Unit,
  subjectValue: String,
  subjectOptions: List<SubjectOverview>,
  onSubjectSelect: (SubjectOverview) -> Unit,
  metricValue: String,
  metricOptions: List<InputNilaiMetric>,
  onMetricSelect: (InputNilaiMetric) -> Unit
) {
  InputNilaiSectionCard(
    icon = Icons.Outlined.CalendarMonth,
    title = "Jadwal dan Kelas",
    infoText = "Pilih tanggal, kelas, mapel, dan jenis nilai yang akan diinput. Nilai akan disimpan sebagai rincian nilai tanggal tersebut."
  ) {
    InputNilaiDateField(
      value = dateIso,
      onSelectDate = onDateSelect
    )
    InputNilaiDropdownField(
      label = "Kelas",
      value = classValue,
      options = classOptions,
      optionLabel = { it },
      onSelect = onClassSelect
    )
    InputNilaiDropdownField(
      label = "Mapel",
      value = subjectValue,
      options = subjectOptions,
      optionLabel = { "${it.title} - ${it.semester}" },
      onSelect = onSubjectSelect
    )
    InputNilaiDropdownField(
      label = "Jenis Nilai",
      value = metricValue,
      options = metricOptions,
      optionLabel = { it.labelWithLimit },
      onSelect = onMetricSelect
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputNilaiDateField(
  value: String,
  onSelectDate: (String) -> Unit
) {
  var showPicker by remember { mutableStateOf(false) }

  InputNilaiPickerSurface(
    label = "Tanggal",
    value = formatInputNilaiDate(value),
    onClick = { showPicker = true }
  )

  if (showPicker) {
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
      initialSelectedDateMillis = dateIsoToInputNilaiPickerMillis(value)
    )
    DatePickerDialog(
      onDismissRequest = { showPicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              onSelectDate(inputNilaiPickerMillisToDateIso(millis))
            }
            showPicker = false
          }
        ) {
          Text("Pilih")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPicker = false }) {
          Text("Batal")
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }
}

@Composable
private fun InputNilaiMateriCard(
  materiText: String,
  onMateriChange: (String) -> Unit,
  materiSuggestion: InputNilaiMateriSuggestion?,
  onSuggestionClick: (String) -> Unit,
  patronOptions: List<String>,
  onPatronSelect: (String) -> Unit
) {
  InputNilaiSectionCard(
    icon = Icons.Outlined.AutoStories,
    title = "Materi",
    infoText = "Isi materi manual, pilih dari patron materi, atau tekan saran materi terakhir untuk mengisi otomatis."
  ) {
    OutlinedTextField(
      value = materiText,
      onValueChange = onMateriChange,
      singleLine = true,
      label = { Text("Materi") },
      placeholder = { Text("Contoh: Murojaah bab fi'il") },
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp)
    )
    if (materiSuggestion != null) {
      InputNilaiSuggestionChip(
        suggestion = materiSuggestion,
        onClick = { onSuggestionClick(materiSuggestion.text) }
      )
    } else {
      Text(
        text = "Belum ada riwayat materi nilai sebelumnya untuk mapel ini.",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
    InputNilaiDropdownField(
      label = "Pilih dari patron materi",
      value = materiText.takeIf { it in patronOptions } ?: "Pilih materi",
      options = patronOptions,
      optionLabel = { it },
      onSelect = onPatronSelect
    )
  }
}

@Composable
private fun InputNilaiSuggestionChip(
  suggestion: InputNilaiMateriSuggestion,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(HighlightCard.copy(alpha = 0.11f))
      .border(1.dp, HighlightCard.copy(alpha = 0.24f), RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(RoundedCornerShape(13.dp))
        .background(Color.White.copy(alpha = 0.72f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Outlined.AutoStories,
        contentDescription = null,
        tint = HighlightCard,
        modifier = Modifier.size(18.dp)
      )
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = suggestion.title,
        style = MaterialTheme.typography.labelMedium,
        color = HighlightCard,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = suggestion.label,
        style = MaterialTheme.typography.bodySmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
    Text(
      text = "Pakai",
      style = MaterialTheme.typography.labelMedium,
      color = HighlightCard,
      fontWeight = FontWeight.ExtraBold
    )
  }
}

@Composable
private fun InputNilaiSectionCard(
  icon: ImageVector,
  title: String,
  infoText: String,
  content: @Composable () -> Unit
) {
  var showInfo by remember { mutableStateOf(false) }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(24.dp))
      .background(Color.White.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(PrimaryBlue.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = PrimaryBlue,
          modifier = Modifier.size(21.dp)
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )
      Box {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFFF8FAFC))
            .border(1.dp, CardBorder.copy(alpha = 0.9f), CircleShape)
            .clickable { showInfo = true },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "Info $title",
            tint = SubtleInk,
            modifier = Modifier.size(18.dp)
          )
        }
        if (showInfo) {
          Popup(
            alignment = Alignment.TopEnd,
            onDismissRequest = { showInfo = false },
            properties = PopupProperties(focusable = true)
          ) {
            Column(
              modifier = Modifier
                .width(250.dp)
                .padding(top = 42.dp)
                .shadow(16.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x1A0F172A), spotColor = Color(0x1A0F172A))
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.98f))
                .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(18.dp))
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "Catatan",
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryBlueDark,
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = infoText,
                style = MaterialTheme.typography.bodySmall,
                color = SubtleInk
              )
            }
          }
        }
      }
    }
    content()
  }
}

@Composable
private fun <T> InputNilaiDropdownField(
  label: String,
  value: String,
  options: List<T>,
  optionLabel: (T) -> String,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = modifier.fillMaxWidth()) {
    InputNilaiPickerSurface(
      label = label,
      value = value,
      onClick = { expanded = true }
    )
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      if (options.isEmpty()) {
        DropdownMenuItem(
          text = { Text("Belum ada pilihan") },
          onClick = { expanded = false }
        )
      } else {
        options.forEach { option ->
          DropdownMenuItem(
            text = { Text(optionLabel(option)) },
            onClick = {
              onSelect(option)
              expanded = false
            }
          )
        }
      }
    }
  }
}

@Composable
private fun InputNilaiPickerSurface(
  label: String,
  value: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color.White.copy(alpha = 0.74f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = SubtleInk,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun InputNilaiStudentCard(
  student: ScoreStudent,
  value: String,
  onValueChange: (String) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(9.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(20.dp))
      .background(Color.White.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
      .padding(horizontal = 14.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(38.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(HighlightCard.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Outlined.EditNote,
        contentDescription = null,
        tint = HighlightCard,
        modifier = Modifier.size(19.dp)
      )
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = student.name,
        style = MaterialTheme.typography.bodyLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      modifier = Modifier.width(92.dp),
      textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      placeholder = { Text("0") },
      shape = RoundedCornerShape(16.dp)
    )
  }
}

@Composable
private fun InputNilaiSaveButton(
  isSaving: Boolean,
  enabled: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(999.dp))
      .background(if (enabled) SuccessTint.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.7f))
      .border(
        1.dp,
        if (enabled) SuccessTint.copy(alpha = 0.38f) else CardBorder.copy(alpha = 0.86f),
        RoundedCornerShape(999.dp)
      )
      .clickable(enabled = enabled && !isSaving, onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center
  ) {
    if (isSaving) {
      CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        color = SuccessTint,
        strokeWidth = 2.dp
      )
    } else {
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Outlined.Check,
          contentDescription = null,
          tint = if (enabled) SuccessTint else SubtleInk,
          modifier = Modifier.size(18.dp)
        )
        Text(
          text = "Simpan",
          style = MaterialTheme.typography.labelLarge,
          color = if (enabled) SuccessTint else SubtleInk,
          fontWeight = FontWeight.ExtraBold
        )
      }
    }
  }
}

@Composable
private fun InputNilaiInfoCard(
  message: String,
  tone: Color
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(tone.copy(alpha = 0.12f))
      .border(1.dp, tone.copy(alpha = 0.26f), RoundedCornerShape(18.dp))
      .padding(horizontal = 14.dp, vertical = 12.dp)
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodySmall,
      color = PrimaryBlueDark
    )
  }
}

@Composable
private fun InputNilaiStudentSkeleton(index: Int) {
  SkeletonContentCard(
    firstLineWidth = if (index % 2 == 0) 0.5f else 0.64f,
    secondLineWidth = if (index % 3 == 0) 0.36f else 0.5f,
    trailingSize = 92.dp
  )
}

private data class InputNilaiMetric(
  val key: String,
  val label: String,
  val maxValue: Int?
) {
  val labelWithLimit: String
    get() = maxValue?.let { "$label (maks $it)" } ?: label
}

private data class InputNilaiMateriSuggestion(
  val title: String,
  val label: String,
  val text: String
)

private val InputNilaiMetricOptions = listOf(
  InputNilaiMetric("nilai_tugas", "Tugas", maxValue = 5),
  InputNilaiMetric("nilai_ulangan_harian", "Ulangan Harian", maxValue = 10),
  InputNilaiMetric("nilai_pts", "UTS", maxValue = 25),
  InputNilaiMetric("nilai_pas", "UAS", maxValue = 50),
  InputNilaiMetric("nilai_keterampilan", "Keterampilan", maxValue = 100)
)

private fun buildInputNilaiUpdatedStudent(
  student: ScoreStudent,
  metric: InputNilaiMetric,
  dateIso: String,
  value: Double,
  material: String
): ScoreStudent {
  val existingRows = student.detailRowsByMetric[metric.key].orEmpty()
  val existingForDate = existingRows.firstOrNull { it.dateIso == dateIso }
  val nextRow = ScoreDetailRow(
    id = existingForDate?.id.orEmpty(),
    dateIso = dateIso,
    value = value,
    material = normalizeInputNilaiMaterial(material)
  )
  val nextRows = (existingRows.filterNot { row ->
    if (existingForDate?.id?.isNotBlank() == true) row.id == existingForDate.id else row.dateIso == dateIso
  } + nextRow).sortedByDescending { it.dateIso }
  val nextDetailRows = student.detailRowsByMetric.toMutableMap().apply {
    put(metric.key, nextRows)
  }
  val average = calculateInputNilaiAverage(nextRows.mapNotNull { it.value })
  return applyInputNilaiMetricValue(student, metric.key, average).copy(
    detailRowsByMetric = nextDetailRows,
    pendingDetailMetricKey = metric.key,
    pendingDeletedDetailIds = emptyList()
  )
}

private fun applyInputNilaiMetricValue(
  student: ScoreStudent,
  metricKey: String,
  value: Double?
): ScoreStudent {
  val updated = when (metricKey) {
    "nilai_tugas" -> student.copy(nilaiTugas = value)
    "nilai_ulangan_harian" -> student.copy(nilaiUlanganHarian = value ?: 0.0)
    "nilai_pts" -> student.copy(nilaiPts = value ?: 0.0)
    "nilai_pas" -> student.copy(nilaiPas = value ?: 0.0)
    "nilai_keterampilan" -> student.copy(nilaiKeterampilan = value ?: 0.0)
    else -> student
  }
  return updated.copy(
    nilaiAkhir = listOf(
      updated.nilaiTugas ?: 0.0,
      updated.nilaiUlanganHarian ?: 0.0,
      updated.nilaiPts ?: 0.0,
      updated.nilaiPas ?: 0.0,
      updated.nilaiKehadiran ?: 0.0
    ).sum()
  )
}

private fun buildInputNilaiMateriSuggestion(
  students: List<ScoreStudent>,
  dateIso: String,
  metricKey: String
): InputNilaiMateriSuggestion? {
  val rows = students
    .flatMap { it.detailRowsByMetric[metricKey].orEmpty() }
    .filter { normalizeInputNilaiMaterial(it.material).isNotBlank() }

  val currentDay = rows.firstOrNull { it.dateIso == dateIso }
  if (currentDay != null) {
    val materi = normalizeInputNilaiMaterial(currentDay.material)
    return InputNilaiMateriSuggestion(
      title = "Materi tersimpan di tanggal ini",
      label = materi,
      text = materi
    )
  }

  val latest = rows.maxByOrNull { it.dateIso } ?: return null
  val formattedDate = runCatching { formatInputNilaiDate(latest.dateIso) }.getOrDefault(latest.dateIso)
  val materi = normalizeInputNilaiMaterial(latest.material)
  return InputNilaiMateriSuggestion(
    title = "Materi terakhir dinilai",
    label = if (formattedDate.isBlank()) materi else "$materi ($formattedDate)",
    text = materi
  )
}

private fun normalizeInputNilaiMaterial(value: String): String {
  val normalized = value.trim()
  return if (normalized.equals("null", ignoreCase = true)) "" else normalized
}

private fun parseInputNilaiValue(value: String, maxValue: Int?): Double? {
  val parsed = value.trim().replace(',', '.').toDoubleOrNull() ?: return null
  if (parsed < 0) return null
  if (maxValue != null && parsed > maxValue) return null
  return kotlin.math.round(parsed * 100.0) / 100.0
}

private fun sanitizeInputNilaiValueText(value: String, maxValue: Int?): String? {
  val normalized = value.replace(',', '.')
  if (normalized.isBlank()) return ""
  if (normalized.count { it == '.' } > 1) return null
  if (normalized.any { !it.isDigit() && it != '.' }) return null
  if (normalized == ".") return normalized

  val parsed = normalized.toDoubleOrNull() ?: return null
  if (parsed < 0) return null
  if (maxValue != null && parsed > maxValue.toDouble()) return maxValue.toString()
  return normalized
}

private fun calculateInputNilaiAverage(values: List<Double>): Double? {
  if (values.isEmpty()) return null
  return kotlin.math.round((values.sum() / values.size.toDouble()) * 100.0) / 100.0
}

private fun formatInputNilaiNumber(value: Double): String {
  val rounded = kotlin.math.round(value * 100.0) / 100.0
  return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

private fun formatInputNilaiDate(value: String): String {
  val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return value
  val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
  return formatter.format(date)
}

private fun dateIsoToInputNilaiPickerMillis(value: String): Long {
  val date = runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())
  return date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}

private fun inputNilaiPickerMillisToDateIso(millis: Long): String {
  return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
}
