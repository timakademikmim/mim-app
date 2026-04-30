package com.mim.guruapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PersonAddAlt
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mim.guruapp.AttendanceSaveOutcome
import com.mim.guruapp.data.model.AttendanceHistoryEntry
import com.mim.guruapp.data.model.AttendanceStudent
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.InputAbsensiNavigationTarget
import com.mim.guruapp.data.model.MapelAttendanceSnapshot
import com.mim.guruapp.data.model.MapelPatronMateriSnapshot
import com.mim.guruapp.data.model.SubjectOverview
import com.mim.guruapp.data.model.SyncBannerState
import com.mim.guruapp.data.model.TeacherOption
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
fun InputAbsensiScreen(
  subjects: List<SubjectOverview>,
  attendanceSnapshots: List<MapelAttendanceSnapshot>,
  patronMateriSnapshots: List<MapelPatronMateriSnapshot>,
  teachingScheduleEvents: List<CalendarEvent>,
  substituteTeacherOptions: List<TeacherOption>,
  sourceTeacherOptions: List<TeacherOption>,
  syncBanner: SyncBannerState,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadAttendance: suspend (String, SubjectOverview) -> MapelAttendanceSnapshot?,
  onLoadPatronMateri: suspend (String, SubjectOverview) -> MapelPatronMateriSnapshot?,
  onSaveAttendanceBatch: suspend (String, SubjectOverview, Map<String, List<AttendanceHistoryEntry>>, String, String) -> AttendanceSaveOutcome,
  onSaveAttendance: suspend (String, SubjectOverview, String, List<AttendanceHistoryEntry>) -> AttendanceSaveOutcome,
  onSendSubstituteDelegation: suspend (String, SubjectOverview, String, List<String>, String, String) -> AttendanceSaveOutcome,
  onLoadSubstituteTeacherContext: suspend (TeacherOption, String) -> Pair<List<SubjectOverview>, List<CalendarEvent>>,
  onLoadDelegatedAttendanceContext: suspend (String) -> Pair<List<SubjectOverview>, List<CalendarEvent>>,
  launchTarget: InputAbsensiNavigationTarget? = null,
  onLaunchTargetConsumed: () -> Unit = {},
  activeInputPage: Int = 1,
  showTopBar: Boolean = true,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  var dateIso by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
  var selectedClassName by rememberSaveable { mutableStateOf("") }
  var selectedSubjectId by rememberSaveable { mutableStateOf("") }
  var jamPelajaran1 by rememberSaveable { mutableStateOf("") }
  var jamPelajaran2 by rememberSaveable { mutableStateOf("") }
  var materiText by rememberSaveable { mutableStateOf("") }
  var useSubstituteTeacher by rememberSaveable { mutableStateOf(false) }
  var fillAsSubstituteTeacher by rememberSaveable { mutableStateOf(false) }
  var substituteTeacherId by rememberSaveable { mutableStateOf("") }
  var sourceTeacherId by rememberSaveable { mutableStateOf("") }
  var substituteNote by rememberSaveable { mutableStateOf("") }
  var attendanceSnapshot by remember(selectedSubjectId, attendanceSnapshots) {
    mutableStateOf(attendanceSnapshots.firstOrNull { it.distribusiId == selectedSubjectId })
  }
  var patronMateriSnapshot by remember(selectedSubjectId, patronMateriSnapshots) {
    mutableStateOf(patronMateriSnapshots.firstOrNull { it.distribusiId == selectedSubjectId })
  }
  var isLoadingStudents by remember(selectedSubjectId) { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var isSendingDelegation by remember { mutableStateOf(false) }
  var isLoadingSourceTeacherContext by remember { mutableStateOf(false) }
  var isLoadingDelegatedContext by remember { mutableStateOf(false) }
  var sourceTeacherSubjects by remember { mutableStateOf<List<SubjectOverview>>(emptyList()) }
  var sourceTeacherScheduleEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
  var delegatedSubjects by remember { mutableStateOf<List<SubjectOverview>>(emptyList()) }
  var delegatedScheduleEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
  var feedbackMessage by remember { mutableStateOf<String?>(null) }
  var draftStatuses by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
  var lastAppliedLaunchTargetId by remember { mutableStateOf("") }
  val selectedSourceTeacher = remember(sourceTeacherOptions, sourceTeacherId) {
    sourceTeacherOptions.firstOrNull { it.id == sourceTeacherId }
  }
  val activeSubjects = remember(fillAsSubstituteTeacher, sourceTeacherSubjects, subjects, delegatedSubjects) {
    if (fillAsSubstituteTeacher) {
      sourceTeacherSubjects
    } else {
      (subjects + delegatedSubjects).distinctBy { it.id }
    }
  }
  val activeTeachingScheduleEvents = remember(fillAsSubstituteTeacher, sourceTeacherScheduleEvents, teachingScheduleEvents, delegatedScheduleEvents) {
    if (fillAsSubstituteTeacher) {
      sourceTeacherScheduleEvents
    } else {
      (teachingScheduleEvents + delegatedScheduleEvents).distinctBy { it.id }
    }
  }

  val scheduleOptionsForDate = remember(activeSubjects, activeTeachingScheduleEvents, dateIso) {
    buildInputAbsensiScheduleOptions(
      subjects = activeSubjects,
      teachingScheduleEvents = activeTeachingScheduleEvents,
      dateIso = dateIso
    )
  }
  val classOptions = remember(scheduleOptionsForDate) {
    scheduleOptionsForDate
      .map { it.subject.className }
      .filter { it.isNotBlank() }
      .distinct()
      .sorted()
  }
  val subjectsForClass = remember(scheduleOptionsForDate, selectedClassName) {
    scheduleOptionsForDate
      .filter { option -> selectedClassName.isBlank() || option.subject.className == selectedClassName }
      .map { it.subject }
      .distinctBy { it.id }
      .sortedWith(compareBy<SubjectOverview> { it.title.lowercase() }.thenBy { it.semester.lowercase() })
  }
  val selectedSubject = subjectsForClass.firstOrNull { it.id == selectedSubjectId } ?: subjectsForClass.firstOrNull()
  val jamOptions = remember(scheduleOptionsForDate, selectedSubject) {
    scheduleOptionsForDate
      .filter { it.subject.id == selectedSubject?.id }
      .map { it.timeLabel }
      .filter { it.isNotBlank() }
      .distinct()
  }
  val selectedLessonSlotIds = remember(scheduleOptionsForDate, selectedSubject, jamPelajaran1, jamPelajaran2) {
    listOf(jamPelajaran1, jamPelajaran2)
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
      .mapNotNull { label ->
        scheduleOptionsForDate.firstOrNull { option ->
          option.subject.id == selectedSubject?.id && option.timeLabel == label
        }?.lessonSlotId?.normalizedNullableId()?.takeIf { it.isNotBlank() }
      }
      .distinct()
  }
  val selectedDelegationLessonSlotIds = remember(selectedLessonSlotIds, jamPelajaran1, jamPelajaran2) {
    selectedLessonSlotIds.ifEmpty {
      val hasSelectedJamLabel = listOf(jamPelajaran1, jamPelajaran2)
        .map { it.trim() }
        .any { it.isNotBlank() }
      if (hasSelectedJamLabel) listOf("") else emptyList()
    }
  }
  val patronOptions = remember(patronMateriSnapshot, patronMateriSnapshots, selectedSubjectId) {
    (patronMateriSnapshot ?: patronMateriSnapshots.firstOrNull { it.distribusiId == selectedSubjectId })
      ?.items
      .orEmpty()
      .map { it.text.trim() }
      .filter { it.isNotBlank() }
      .distinct()
  }
  val students = attendanceSnapshot?.students.orEmpty()
  val materiSuggestion = remember(students, dateIso, selectedSubjectId) {
    buildInputAbsensiMateriSuggestion(students = students, dateIso = dateIso)
  }

  LaunchedEffect(scheduleOptionsForDate, selectedClassName, selectedSubjectId) {
    val nextClassName = selectedClassName.takeIf { it in classOptions }
      ?: classOptions.firstOrNull().orEmpty()
    val nextSubjectsForClass = scheduleOptionsForDate
      .filter { option -> nextClassName.isBlank() || option.subject.className == nextClassName }
      .map { it.subject }
      .distinctBy { it.id }
      .sortedWith(compareBy<SubjectOverview> { it.title.lowercase() }.thenBy { it.semester.lowercase() })
    val nextSubjectId = selectedSubjectId.takeIf { id -> nextSubjectsForClass.any { it.id == id } }
      ?: nextSubjectsForClass.firstOrNull()?.id.orEmpty()
    val nextJamOptions = scheduleOptionsForDate
      .filter { it.subject.id == nextSubjectId }
      .map { it.timeLabel }
      .filter { it.isNotBlank() }
      .distinct()
    val nextJam1 = jamPelajaran1.takeIf { it in nextJamOptions } ?: nextJamOptions.firstOrNull().orEmpty()
    val nextJam2 = jamPelajaran2.takeIf { it in nextJamOptions && it != nextJam1 }
      ?: nextJamOptions.drop(1).firstOrNull().orEmpty()

    if (selectedClassName != nextClassName) selectedClassName = nextClassName
    if (selectedSubjectId != nextSubjectId) selectedSubjectId = nextSubjectId
    if (jamPelajaran1 != nextJam1) jamPelajaran1 = nextJam1
    if (jamPelajaran2 != nextJam2) jamPelajaran2 = nextJam2
  }

  LaunchedEffect(selectedSubjectId, activeSubjects) {
    val subject = activeSubjects.firstOrNull { it.id == selectedSubjectId }
    if (subject == null) {
      attendanceSnapshot = null
      isLoadingStudents = false
      return@LaunchedEffect
    }
    isLoadingStudents = true
    attendanceSnapshot = attendanceSnapshots.firstOrNull { it.distribusiId == selectedSubjectId }
    attendanceSnapshot = onLoadAttendance(subject.id, subject) ?: attendanceSnapshot
    isLoadingStudents = false
  }

  LaunchedEffect(selectedSubjectId, activeSubjects) {
    val subject = activeSubjects.firstOrNull { it.id == selectedSubjectId }
    if (subject == null) {
      patronMateriSnapshot = null
      return@LaunchedEffect
    }
    patronMateriSnapshot = patronMateriSnapshots.firstOrNull { it.distribusiId == selectedSubjectId }
    patronMateriSnapshot = onLoadPatronMateri(subject.id, subject) ?: patronMateriSnapshot
  }

  LaunchedEffect(attendanceSnapshot?.updatedAt, dateIso, selectedSubjectId) {
    draftStatuses = students.associate { student ->
      student.id to (student.history.firstOrNull { it.dateIso == dateIso }?.status ?: "Hadir")
    }
  }

  LaunchedEffect(substituteTeacherOptions, sourceTeacherOptions) {
    if (substituteTeacherId.isNotBlank() && substituteTeacherOptions.none { it.id == substituteTeacherId }) {
      substituteTeacherId = ""
    }
    if (sourceTeacherId.isNotBlank() && sourceTeacherOptions.none { it.id == sourceTeacherId }) {
      sourceTeacherId = ""
    }
  }

  LaunchedEffect(fillAsSubstituteTeacher, sourceTeacherId, dateIso) {
    if (!fillAsSubstituteTeacher || selectedSourceTeacher == null) {
      sourceTeacherSubjects = emptyList()
      sourceTeacherScheduleEvents = emptyList()
      isLoadingSourceTeacherContext = false
      return@LaunchedEffect
    }

    isLoadingSourceTeacherContext = true
    val (loadedSubjects, loadedEvents) = try {
      onLoadSubstituteTeacherContext(selectedSourceTeacher, dateIso)
    } catch (_: Exception) {
      emptyList<SubjectOverview>() to emptyList()
    }
    sourceTeacherSubjects = loadedSubjects
    sourceTeacherScheduleEvents = loadedEvents
    isLoadingSourceTeacherContext = false
  }

  LaunchedEffect(dateIso, syncBanner.isSyncing) {
    if (syncBanner.isSyncing) return@LaunchedEffect
    isLoadingDelegatedContext = true
    val (loadedSubjects, loadedEvents) = try {
      onLoadDelegatedAttendanceContext(dateIso)
    } catch (_: Exception) {
      emptyList<SubjectOverview>() to emptyList()
    }
    delegatedSubjects = loadedSubjects
    delegatedScheduleEvents = loadedEvents
    isLoadingDelegatedContext = false
  }

  LaunchedEffect(
    launchTarget?.requestId,
    dateIso,
    scheduleOptionsForDate,
    isLoadingDelegatedContext,
    isLoadingSourceTeacherContext
  ) {
    val target = launchTarget ?: return@LaunchedEffect
    if (lastAppliedLaunchTargetId == target.requestId) return@LaunchedEffect

    if (dateIso != target.dateIso) {
      dateIso = target.dateIso
      feedbackMessage = null
      return@LaunchedEffect
    }
    if (isLoadingDelegatedContext || isLoadingSourceTeacherContext) return@LaunchedEffect

    val matchingOptions = scheduleOptionsForDate
      .filter { option ->
        target.distribusiId.isBlank() || option.subject.id == target.distribusiId
      }
    if (matchingOptions.isEmpty()) return@LaunchedEffect

    val preferredOption = matchingOptions.firstOrNull { option ->
      target.lessonSlotId.isBlank() || option.lessonSlotId.normalizedNullableId() == target.lessonSlotId
    } ?: matchingOptions.first()
    val preferredSubject = preferredOption.subject
    val preferredSubjectOptions = scheduleOptionsForDate
      .filter { it.subject.id == preferredSubject.id }
      .distinctBy { "${it.timeLabel}|${it.lessonSlotId}" }
    val preferredJam1 = preferredOption.timeLabel.ifBlank { preferredSubjectOptions.firstOrNull()?.timeLabel.orEmpty() }
    val preferredJam2 = preferredSubjectOptions
      .firstOrNull { option ->
        option.timeLabel != preferredJam1 &&
          option.lessonSlotId.normalizedNullableId() != target.lessonSlotId
      }
      ?.timeLabel
      .orEmpty()

    selectedClassName = preferredSubject.className
    selectedSubjectId = preferredSubject.id
    jamPelajaran1 = preferredJam1
    jamPelajaran2 = preferredJam2
    feedbackMessage = null
    lastAppliedLaunchTargetId = target.requestId
    onLaunchTargetConsumed()
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    topBar = {
      if (showTopBar) {
        InputAbsensiTopBar(
          title = "Input Absensi",
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
          InputAbsensiFormCard(
            dateIso = dateIso,
            onDateSelect = {
              dateIso = it
              feedbackMessage = null
            },
            classValue = selectedClassName.ifBlank { "Pilih kelas" },
            classOptions = classOptions,
            onClassSelect = {
              selectedClassName = it
              feedbackMessage = null
            },
            subjectValue = selectedSubject?.title ?: "Pilih mapel",
            subjectOptions = subjectsForClass,
            onSubjectSelect = {
              selectedSubjectId = it.id
              selectedClassName = it.className
              val nextJamOptions = scheduleOptionsForDate
                .filter { option -> option.subject.id == it.id }
                .map { option -> option.timeLabel }
                .filter { option -> option.isNotBlank() }
                .distinct()
              jamPelajaran1 = nextJamOptions.firstOrNull().orEmpty()
              jamPelajaran2 = nextJamOptions.drop(1).firstOrNull().orEmpty()
              feedbackMessage = null
            },
            jam1 = jamPelajaran1.ifBlank { "Tidak dipilih" },
            jam2 = jamPelajaran2.ifBlank { "Tidak dipilih" },
            jamOptions = jamOptions,
            onJam1Select = { jamPelajaran1 = it },
            onJam2Select = { jamPelajaran2 = it }
          )
        }

        item {
          InputAbsensiMateriCard(
            materiText = materiText,
            onMateriChange = {
              materiText = it
              feedbackMessage = null
            },
            materiSuggestion = materiSuggestion,
            onSuggestionClick = {
              materiText = it
              feedbackMessage = null
            },
            patronOptions = patronOptions,
            onPatronSelect = {
              materiText = it
              feedbackMessage = null
            }
          )
        }

        item {
          InputAbsensiGuruPenggantiCard(
            useSubstituteTeacher = useSubstituteTeacher,
            onUseSubstituteTeacherChange = {
              useSubstituteTeacher = it
              if (it) {
                fillAsSubstituteTeacher = false
                sourceTeacherId = ""
              } else {
                substituteTeacherId = ""
              }
              feedbackMessage = null
            },
            fillAsSubstituteTeacher = fillAsSubstituteTeacher,
            onFillAsSubstituteTeacherChange = {
              fillAsSubstituteTeacher = it
              if (it) {
                useSubstituteTeacher = false
                substituteTeacherId = ""
              } else {
                sourceTeacherId = ""
              }
              feedbackMessage = null
            },
            substituteTeacherOptions = substituteTeacherOptions,
            selectedSubstituteTeacherId = substituteTeacherId,
            onSubstituteTeacherSelect = { teacher ->
              substituteTeacherId = teacher.id
              feedbackMessage = null
            },
            sourceTeacherOptions = sourceTeacherOptions,
            selectedSourceTeacherId = sourceTeacherId,
            onSourceTeacherSelect = { teacher ->
              sourceTeacherId = teacher.id
              feedbackMessage = null
            },
            note = substituteNote,
            onNoteChange = {
              substituteNote = it
              feedbackMessage = null
            },
            isSendingDelegation = isSendingDelegation,
            canSendDelegation = selectedSubject != null &&
              substituteTeacherId.isNotBlank() &&
              selectedDelegationLessonSlotIds.isNotEmpty(),
            onSendDelegation = {
              val subject = selectedSubject
              if (subject == null) {
                feedbackMessage = "Pilih mapel terlebih dahulu."
              } else {
                scope.launch {
                  isSendingDelegation = true
                  feedbackMessage = null
                  val outcome = try {
                    onSendSubstituteDelegation(
                      subject.id,
                      subject,
                      dateIso,
                      selectedDelegationLessonSlotIds,
                      substituteTeacherId,
                      substituteNote
                    )
                  } catch (_: Exception) {
                    AttendanceSaveOutcome(false, "Gagal mengirim amanat guru pengganti.")
                  }
                  feedbackMessage = outcome.message
                  isSendingDelegation = false
                  if (outcome.success) {
                    useSubstituteTeacher = false
                    substituteTeacherId = ""
                    substituteNote = ""
                  }
                }
              }
            }
          )
        }

        if (!useSubstituteTeacher) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Kehadiran Siswa",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryBlueDark,
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = "${students.size} santri siap diisi",
                style = MaterialTheme.typography.bodySmall,
                color = SubtleInk
              )
            }
            InputAbsensiSaveButton(
              isSaving = isSaving,
              enabled = students.isNotEmpty() && selectedSubject != null,
              onClick = {
                val subject = selectedSubject
                if (subject != null) {
                  scope.launch {
                    isSaving = true
                    feedbackMessage = null
                    val changesByStudent = students.associate { student ->
                      student.id to buildInputAbsensiEntries(
                        student = student,
                        dateIso = dateIso,
                        status = draftStatuses[student.id] ?: "Hadir",
                        lessonSlotIds = selectedLessonSlotIds,
                        materiText = materiText
                      )
                    }
                    val outcome = try {
                      onSaveAttendanceBatch(
                        subject.id,
                        subject,
                        changesByStudent,
                        if (fillAsSubstituteTeacher) sourceTeacherId else "",
                        if (fillAsSubstituteTeacher) substituteNote else ""
                      )
                    } catch (_: Exception) {
                      AttendanceSaveOutcome(false, "Gagal menyimpan absensi.")
                    }
                    if (outcome.success) {
                      attendanceSnapshot = onLoadAttendance(subject.id, subject) ?: attendanceSnapshot
                    }
                    feedbackMessage = outcome.message
                    isSaving = false
                  }
                }
              }
            )
          }
        }
        }

        if (feedbackMessage != null) {
          item {
            InputAbsensiInfoCard(
              message = feedbackMessage.orEmpty(),
              tone = if (feedbackMessage?.contains("berhasil", ignoreCase = true) == true) SuccessTint else WarmAccent
            )
          }
        }

        when {
          fillAsSubstituteTeacher && selectedSourceTeacher == null -> {
            item {
              EmptyPlaceholderCard("Pilih guru yang digantikan terlebih dahulu agar kelas, mapel, dan jam pelajarannya muncul.")
            }
          }

          fillAsSubstituteTeacher && isLoadingSourceTeacherContext -> {
            items(3) { index ->
              InputAbsensiStudentSkeleton(index + 1)
            }
          }

          !fillAsSubstituteTeacher && isLoadingDelegatedContext && scheduleOptionsForDate.isEmpty() -> {
            items(2) { index ->
              InputAbsensiStudentSkeleton(index + 1)
            }
          }

          activeSubjects.isEmpty() -> {
            item {
              EmptyPlaceholderCard("Belum ada mapel aktif. Tambahkan mapel dahulu sebelum mengisi absensi.")
            }
          }

          isLoadingStudents -> {
            items(5) { index ->
              InputAbsensiStudentSkeleton(index + 1)
            }
          }

          useSubstituteTeacher -> {
            item {
              EmptyPlaceholderCard("Mode amanat aktif. Guru pengganti yang dipilih nanti akan mengisi absensi dari halamannya sendiri.")
            }
          }

          scheduleOptionsForDate.isEmpty() -> {
            item {
              EmptyPlaceholderCard("Tidak ada jadwal mengajar pada tanggal ini. Pilih tanggal lain atau tarik refresh jika jadwal belum masuk.")
            }
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
              InputAbsensiStudentCompactCard(
                student = student,
                status = draftStatuses[student.id] ?: "Hadir",
                onStatusSelect = { status ->
                  draftStatuses = draftStatuses.toMutableMap().apply {
                    put(student.id, status)
                  }
                  feedbackMessage = null
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
private fun InputAbsensiTopBar(
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
    InputAbsensiGlassActionButton(
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
        InputAbsensiPageIndicator(activeInputPage = activeInputPage)
      }
    }

    Box(modifier = Modifier.size(42.dp))
  }
}

@Composable
private fun InputAbsensiPageIndicator(activeInputPage: Int) {
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
private fun InputAbsensiGlassActionButton(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputAbsensiFormCard(
  dateIso: String,
  onDateSelect: (String) -> Unit,
  classValue: String,
  classOptions: List<String>,
  onClassSelect: (String) -> Unit,
  subjectValue: String,
  subjectOptions: List<SubjectOverview>,
  onSubjectSelect: (SubjectOverview) -> Unit,
  jam1: String,
  jam2: String,
  jamOptions: List<String>,
  onJam1Select: (String) -> Unit,
  onJam2Select: (String) -> Unit
) {
  InputAbsensiSectionCard(
    icon = Icons.Outlined.CalendarMonth,
    title = "Jadwal dan Kelas",
    infoText = "Pilih tanggal terlebih dahulu. Kelas, mapel, dan jam akan mengikuti jadwal mengajar atau tugas pengganti yang tersedia."
  ) {
    InputAbsensiDateField(
      value = dateIso,
      onSelectDate = onDateSelect
    )
    InputAbsensiDropdownField(
      label = "Kelas",
      value = classValue,
      options = classOptions,
      optionLabel = { it },
      onSelect = onClassSelect
    )
    InputAbsensiDropdownField(
      label = "Mapel",
      value = subjectValue,
      options = subjectOptions,
      optionLabel = { "${it.title} - ${it.semester}" },
      onSelect = onSubjectSelect
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      InputAbsensiDropdownField(
        label = "Jam Pelajaran 1",
        value = jam1,
        options = listOf("") + jamOptions,
        optionLabel = { it.ifBlank { "Tidak dipilih" } },
        onSelect = onJam1Select,
        modifier = Modifier.weight(1f)
      )
      InputAbsensiDropdownField(
        label = "Jam Pelajaran 2",
        value = jam2,
        options = listOf("") + jamOptions,
        optionLabel = { it.ifBlank { "Tidak dipilih" } },
        onSelect = onJam2Select,
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputAbsensiDateField(
  value: String,
  onSelectDate: (String) -> Unit
) {
  var showPicker by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxWidth()) {
    InputAbsensiPickerSurface(
      label = "Tanggal",
      value = formatInputAbsensiDate(value),
      onClick = { showPicker = true }
    )
  }

  if (showPicker) {
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
      initialSelectedDateMillis = dateIsoToPickerMillis(value)
    )
    DatePickerDialog(
      onDismissRequest = { showPicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              onSelectDate(pickerMillisToDateIso(millis))
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
private fun InputAbsensiMateriCard(
  materiText: String,
  onMateriChange: (String) -> Unit,
  materiSuggestion: InputAbsensiMateriSuggestion?,
  onSuggestionClick: (String) -> Unit,
  patronOptions: List<String>,
  onPatronSelect: (String) -> Unit
) {
  InputAbsensiSectionCard(
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
      InputAbsensiMateriSuggestionChip(
        suggestion = materiSuggestion,
        onClick = { onSuggestionClick(materiSuggestion.text) }
      )
    } else {
      Text(
        text = "Belum ada riwayat materi sebelumnya untuk mapel ini.",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
    InputAbsensiDropdownField(
      label = "Pilih dari patron materi",
      value = materiText.takeIf { it in patronOptions } ?: "Pilih materi",
      options = patronOptions,
      optionLabel = { it },
      onSelect = onPatronSelect
    )
  }
}

@Composable
private fun InputAbsensiMateriSuggestionChip(
  suggestion: InputAbsensiMateriSuggestion,
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
private fun InputAbsensiGuruPenggantiCard(
  useSubstituteTeacher: Boolean,
  onUseSubstituteTeacherChange: (Boolean) -> Unit,
  fillAsSubstituteTeacher: Boolean,
  onFillAsSubstituteTeacherChange: (Boolean) -> Unit,
  substituteTeacherOptions: List<TeacherOption>,
  selectedSubstituteTeacherId: String,
  onSubstituteTeacherSelect: (TeacherOption) -> Unit,
  sourceTeacherOptions: List<TeacherOption>,
  selectedSourceTeacherId: String,
  onSourceTeacherSelect: (TeacherOption) -> Unit,
  note: String,
  onNoteChange: (String) -> Unit,
  isSendingDelegation: Boolean,
  canSendDelegation: Boolean,
  onSendDelegation: () -> Unit
) {
  val selectedSubstituteTeacher = substituteTeacherOptions.firstOrNull { it.id == selectedSubstituteTeacherId }
  val selectedSourceTeacher = sourceTeacherOptions.firstOrNull { it.id == selectedSourceTeacherId }
  val isNoteVisible = useSubstituteTeacher || fillAsSubstituteTeacher

  InputAbsensiSectionCard(
    icon = Icons.Outlined.PersonAddAlt,
    title = "Guru Pengganti",
    infoText = "Isi sebagai guru pengganti akan dikirim ke guru utama untuk direview terlebih dahulu. Amanat penggantian dari guru utama akan langsung bisa diisi dan masuk ke sistem."
  ) {
    if (!useSubstituteTeacher) {
      InputAbsensiSubstituteToggleRow(
        title = "Isi sebagai guru pengganti",
        subtitle = "Pilih guru asli yang digantikan. Hasilnya akan menunggu persetujuan guru utama.",
        checked = fillAsSubstituteTeacher,
        onCheckedChange = onFillAsSubstituteTeacherChange
      )
    }

    AnimatedVisibility(visible = fillAsSubstituteTeacher && !useSubstituteTeacher) {
      InputAbsensiDropdownField(
        label = "Guru yang Digantikan",
        value = selectedSourceTeacher?.name ?: "Pilih guru asli",
        options = sourceTeacherOptions,
        optionLabel = { it.name },
        onSelect = onSourceTeacherSelect
      )
    }

    if (!fillAsSubstituteTeacher) {
      InputAbsensiSubstituteToggleRow(
        title = "Gunakan guru pengganti",
        subtitle = "Amanatkan pengisian absensi ke guru lain. Saat mereka mengisi, datanya langsung masuk ke sistem.",
        checked = useSubstituteTeacher,
        onCheckedChange = onUseSubstituteTeacherChange
      )
    }

    AnimatedVisibility(visible = useSubstituteTeacher) {
      InputAbsensiDropdownField(
        label = "Pengganti (Karyawan)",
        value = selectedSubstituteTeacher?.name ?: "Pilih pengganti",
        options = substituteTeacherOptions,
        optionLabel = { it.name },
        onSelect = onSubstituteTeacherSelect
      )
    }

    AnimatedVisibility(visible = isNoteVisible) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = note,
          onValueChange = onNoteChange,
          singleLine = true,
          label = { Text("Keterangan pengganti") },
          placeholder = { Text("Contoh: Guru utama izin") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp)
        )
      }
    }

    AnimatedVisibility(visible = useSubstituteTeacher) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InputAbsensiSaveButton(
          isSaving = isSendingDelegation,
          enabled = canSendDelegation,
          label = "Kirim Amanat",
          modifier = Modifier.fillMaxWidth(),
          onClick = onSendDelegation
        )
        if (!canSendDelegation) {
          Text(
            text = "Pilih guru pengganti dan minimal satu jam pelajaran terlebih dahulu.",
            style = MaterialTheme.typography.bodySmall,
            color = SubtleInk
          )
        }
      }
    }
  }
}

@Composable
private fun InputAbsensiSubstituteToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange
    )
  }
}

@Composable
private fun InputAbsensiSectionCard(
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
private fun <T> InputAbsensiDropdownField(
  label: String,
  value: String,
  options: List<T>,
  optionLabel: (T) -> String,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = modifier.fillMaxWidth()) {
    InputAbsensiPickerSurface(
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
private fun InputAbsensiPickerSurface(
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
private fun InputAbsensiStudentCard(
  student: AttendanceStudent,
  status: String,
  onStatusSelect: (String) -> Unit
) {
  val palette = remember(status) { inputAttendancePalette(status) }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(9.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(20.dp))
      .background(Color.White.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
      .animateContentSize()
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(12.dp)
          .background(palette.accent, CircleShape)
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = student.name,
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "Status hari ini: $status",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
      InputAbsensiStatusBadge(status = status, palette = palette)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      InputAttendanceStatusOptions.forEach { option ->
        val optionPalette = inputAttendancePalette(option)
        val selected = status == option
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) optionPalette.background else Color(0xFFF8FAFC))
            .border(
              1.dp,
              if (selected) optionPalette.border else CardBorder.copy(alpha = 0.84f),
              RoundedCornerShape(999.dp)
            )
            .clickable { onStatusSelect(option) }
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = option.take(1),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) optionPalette.text else SubtleInk,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }
    }
  }
}

@Composable
private fun InputAbsensiStatusBadge(
  status: String,
  palette: InputAttendancePalette
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(palette.background)
      .border(1.dp, palette.border, RoundedCornerShape(999.dp))
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Text(
      text = status,
      style = MaterialTheme.typography.labelMedium,
      color = palette.text,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
private fun InputAbsensiStudentCompactCard(
  student: AttendanceStudent,
  status: String,
  onStatusSelect: (String) -> Unit
) {
  val palette = remember(status) { inputAttendancePalette(status) }
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
        .size(11.dp)
        .background(palette.accent, CircleShape)
    )
    Text(
      text = student.name,
      style = MaterialTheme.typography.bodyLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )
    InputAbsensiStatusDropdown(
      status = status,
      onStatusSelect = onStatusSelect
    )
  }
}

@Composable
private fun InputAbsensiStatusDropdown(
  status: String,
  onStatusSelect: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  val palette = remember(status) { inputAttendancePalette(status) }
  Box(modifier = Modifier.width(124.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(999.dp))
        .background(palette.background)
        .border(1.dp, palette.border, RoundedCornerShape(999.dp))
        .clickable { expanded = true }
        .padding(horizontal = 10.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = status,
        style = MaterialTheme.typography.labelMedium,
        color = palette.text,
        fontWeight = FontWeight.Bold,
        maxLines = 1
      )
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      InputAttendanceStatusOptions.forEach { option ->
        DropdownMenuItem(
          text = { Text(option) },
          onClick = {
            onStatusSelect(option)
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
private fun InputAbsensiSaveButton(
  isSaving: Boolean,
  enabled: Boolean,
  label: String = "Simpan",
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
          text = label,
          style = MaterialTheme.typography.labelLarge,
          color = if (enabled) SuccessTint else SubtleInk,
          fontWeight = FontWeight.ExtraBold
        )
      }
    }
  }
}

@Composable
private fun InputAbsensiInfoCard(
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
private fun InputAbsensiStudentSkeleton(index: Int) {
  SkeletonContentCard(
    firstLineWidth = if (index % 2 == 0) 0.48f else 0.62f,
    secondLineWidth = if (index % 3 == 0) 0.42f else 0.56f,
    trailingSize = 78.dp
  )
}

private data class InputAttendancePalette(
  val background: Color,
  val border: Color,
  val accent: Color,
  val text: Color
)

private data class InputAbsensiScheduleOption(
  val subject: SubjectOverview,
  val timeLabel: String,
  val lessonSlotId: String
)

private data class InputAbsensiMateriSuggestion(
  val title: String,
  val label: String,
  val text: String
)

private val InputAttendanceStatusOptions = listOf("Hadir", "Terlambat", "Sakit", "Izin", "Alpa")

private fun buildInputAbsensiEntries(
  student: AttendanceStudent,
  dateIso: String,
  status: String,
  lessonSlotIds: List<String>,
  materiText: String
): List<AttendanceHistoryEntry> {
  val existing = student.history.firstOrNull { it.dateIso == dateIso }
  val normalizedLessonSlotIds = lessonSlotIds
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .distinct()
  val normalizedMateri = materiText.trim()

  return normalizedLessonSlotIds
    .map { lessonSlotId ->
      AttendanceHistoryEntry(
        dateIso = dateIso,
        status = status,
        rowIds = existing?.rowIds.orEmpty(),
        lessonSlotId = lessonSlotId,
        patronMateri = normalizedMateri
      )
    }
    .ifEmpty {
      listOf(
        AttendanceHistoryEntry(
          dateIso = dateIso,
          status = status,
          rowIds = existing?.rowIds.orEmpty(),
          patronMateri = normalizedMateri
        )
      )
    }
}

private fun buildInputAbsensiMateriSuggestion(
  students: List<AttendanceStudent>,
  dateIso: String
): InputAbsensiMateriSuggestion? {
  val histories = students
    .flatMap { it.history }
    .filter { it.patronMateri.trim().isNotBlank() }

  val currentDay = histories.firstOrNull { it.dateIso == dateIso }
  if (currentDay != null) {
    return InputAbsensiMateriSuggestion(
      title = "Materi tersimpan di tanggal ini",
      label = currentDay.patronMateri.trim(),
      text = currentDay.patronMateri.trim()
    )
  }

  val latest = histories.maxByOrNull { it.dateIso } ?: return null
  val formattedDate = runCatching { formatInputAbsensiDate(latest.dateIso) }
    .getOrDefault(latest.dateIso)
  val materi = latest.patronMateri.trim()
  return InputAbsensiMateriSuggestion(
    title = "Materi terakhir diajarkan",
    label = if (formattedDate.isBlank()) materi else "$materi ($formattedDate)",
    text = materi
  )
}

private fun buildInputAbsensiScheduleOptions(
  subjects: List<SubjectOverview>,
  teachingScheduleEvents: List<CalendarEvent>,
  dateIso: String
): List<InputAbsensiScheduleOption> {
  if (subjects.isEmpty()) return emptyList()
  return teachingScheduleEvents
    .filter { event -> event.startDateIso == dateIso }
    .flatMap { event ->
      subjects
        .filter { subject -> eventMatchesInputAbsensiSubject(event, subject) }
        .map { subject ->
          InputAbsensiScheduleOption(
            subject = subject,
            timeLabel = event.timeLabel.trim(),
            lessonSlotId = event.lessonSlotId.normalizedNullableId()
          )
        }
    }
    .filter { it.timeLabel.isNotBlank() }
    .distinctBy { "${it.subject.id}|${it.timeLabel}|${it.lessonSlotId}" }
    .sortedWith(compareBy<InputAbsensiScheduleOption> { it.subject.className }.thenBy { it.timeLabel }.thenBy { it.subject.title })
}

private fun eventMatchesInputAbsensiSubject(
  event: CalendarEvent,
  subject: SubjectOverview
): Boolean {
  val parts = event.title.split(" - ", limit = 2)
  val subjectTitle = subject.title.normalizedInputAbsensiText()
  val className = subject.className.normalizedInputAbsensiText()
  if (subjectTitle.isBlank() || className.isBlank()) return false

  if (parts.size == 2) {
    return parts[0].normalizedInputAbsensiText() == subjectTitle &&
      parts[1].normalizedInputAbsensiText() == className
  }

  return event.title.normalizedInputAbsensiText() == "$subjectTitle - $className"
}

private fun String.normalizedInputAbsensiText(): String {
  return trim().lowercase(Locale.getDefault())
}

private fun String.normalizedNullableId(): String {
  val text = trim()
  return if (text.equals("null", ignoreCase = true)) "" else text
}

private fun formatInputAbsensiDate(dateIso: String): String {
  val date = runCatching { LocalDate.parse(dateIso) }.getOrDefault(LocalDate.now())
  return date.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.forLanguageTag("id-ID")))
}

private fun dateIsoToPickerMillis(dateIso: String): Long {
  val date = runCatching { LocalDate.parse(dateIso) }.getOrDefault(LocalDate.now())
  return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun pickerMillisToDateIso(millis: Long): String {
  return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
}

private fun inputAttendancePalette(status: String): InputAttendancePalette {
  return when (status.trim()) {
    "Hadir" -> InputAttendancePalette(
      background = Color(0xFFECFDF5),
      border = Color(0xFFA7F3D0),
      accent = Color(0xFF047857),
      text = Color(0xFF047857)
    )

    "Terlambat" -> InputAttendancePalette(
      background = Color(0xFFFFF7ED),
      border = Color(0xFFFDBA74),
      accent = Color(0xFFC2410C),
      text = Color(0xFFC2410C)
    )

    "Sakit" -> InputAttendancePalette(
      background = Color(0xFFF5E8FF),
      border = Color(0xFFD8B4FE),
      accent = Color(0xFF9333EA),
      text = Color(0xFF9333EA)
    )

    "Izin" -> InputAttendancePalette(
      background = Color(0xFFEFF6FF),
      border = Color(0xFF93C5FD),
      accent = Color(0xFF1D4ED8),
      text = Color(0xFF1D4ED8)
    )

    "Alpa" -> InputAttendancePalette(
      background = Color(0xFFFEF2F2),
      border = Color(0xFFFCA5A5),
      accent = Color(0xFFB91C1C),
      text = Color(0xFFB91C1C)
    )

    else -> InputAttendancePalette(
      background = Color(0xFFF8FAFC),
      border = Color(0xFFCBD5E1),
      accent = Color(0xFF64748B),
      text = Color(0xFF334155)
    )
  }
}
