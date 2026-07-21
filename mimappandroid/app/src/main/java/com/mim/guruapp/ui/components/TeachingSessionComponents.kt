package com.mim.guruapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.AttendanceSaveOutcome
import com.mim.guruapp.PatronMateriSaveOutcome
import com.mim.guruapp.ScoreSaveOutcome
import com.mim.guruapp.TeachingSessionSaveOutcome
import com.mim.guruapp.data.model.AttendanceHistoryEntry
import com.mim.guruapp.data.model.AttendanceStudent
import com.mim.guruapp.data.model.CalendarEvent
import com.mim.guruapp.data.model.MapelAttendanceSnapshot
import com.mim.guruapp.data.model.MapelPatronMateriSnapshot
import com.mim.guruapp.data.model.MapelScoreSnapshot
import com.mim.guruapp.data.model.PatronMateriItem
import com.mim.guruapp.data.model.ScoreDetailRow
import com.mim.guruapp.data.model.ScoreStudent
import com.mim.guruapp.data.model.SubjectOverview
import com.mim.guruapp.data.remote.GuruTeachingSessionRecord
import com.mim.guruapp.ui.i18n.formatDateForLanguage
import com.mim.guruapp.ui.i18n.t
import com.mim.guruapp.ui.i18n.ti
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk
import com.mim.guruapp.ui.theme.SuccessTint
import com.mim.guruapp.ui.theme.WarmAccent
import java.time.LocalDate
import kotlin.math.round
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class TeachingSessionSummary(
  val material: String = "",
  val isDraft: Boolean = false,
  val isSaved: Boolean = false,
  val hasAssessment: Boolean = false,
  val hasTask: Boolean = false
) {
  val hasVisibleContent: Boolean
    get() = material.isNotBlank() || isDraft || isSaved || hasAssessment || hasTask
}

private data class TeachingSessionStep(
  val key: String,
  val label: String
)

private data class TeachingSessionMetric(
  val key: String,
  val label: String,
  val maxValue: Double
)

private data class TeachingSessionAssessmentDraft(
  val id: String,
  val metricKey: String,
  val valuesByStudentId: Map<String, String>
)

private val TeachingSessionSteps = listOf(
  TeachingSessionStep("materi", "Materi"),
  TeachingSessionStep("absensi", "Absensi"),
  TeachingSessionStep("isi", "Isi Materi"),
  TeachingSessionStep("nilai", "Penilaian"),
  TeachingSessionStep("tugas", "Tugas")
)

private val TeachingSessionStatusOptions = listOf("Hadir", "Terlambat", "Sakit", "Izin", "Alpa")

private val TeachingSessionMetricOptions = listOf(
  TeachingSessionMetric("nilai_tugas", "Tugas", 5.0),
  TeachingSessionMetric("nilai_ulangan_harian", "Ulangan Harian", 10.0),
  TeachingSessionMetric("nilai_pts", "UTS", 25.0),
  TeachingSessionMetric("nilai_pas", "UAS", 50.0),
  TeachingSessionMetric("nilai_keterampilan", "Keterampilan", 100.0)
)

@Composable
fun TeachingSessionScreen(
  event: CalendarEvent,
  selectedDate: LocalDate,
  subject: SubjectOverview,
  attendanceSnapshot: MapelAttendanceSnapshot?,
  scoreSnapshot: MapelScoreSnapshot?,
  patronMateriSnapshot: MapelPatronMateriSnapshot?,
  onBack: () -> Unit,
  onLoadAttendance: suspend (String, SubjectOverview) -> MapelAttendanceSnapshot?,
  onSaveAttendanceBatch: suspend (String, SubjectOverview, Map<String, List<AttendanceHistoryEntry>>, String, String) -> AttendanceSaveOutcome,
  onLoadScores: suspend (String, SubjectOverview) -> MapelScoreSnapshot?,
  onSaveScoresBatch: suspend (String, SubjectOverview, List<ScoreStudent>) -> ScoreSaveOutcome,
  onLoadPatronMateri: suspend (String, SubjectOverview) -> MapelPatronMateriSnapshot?,
  onSavePatronMateri: suspend (String, SubjectOverview, List<PatronMateriItem>) -> PatronMateriSaveOutcome,
  onLoadTeachingSession: suspend (String, String, String) -> GuruTeachingSessionRecord?,
  onSaveTeachingSession: suspend (GuruTeachingSessionRecord) -> TeachingSessionSaveOutcome,
  onSessionSummaryChange: (TeachingSessionSummary) -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onBack)

  val scope = rememberCoroutineScope()
  val dateIso = selectedDate.toString()
  var currentStepIndex by remember { mutableStateOf(0) }
  var currentAttendance by remember(subject.id, attendanceSnapshot) { mutableStateOf(attendanceSnapshot) }
  var currentScores by remember(subject.id, scoreSnapshot) { mutableStateOf(scoreSnapshot) }
  var currentPatron by remember(subject.id, patronMateriSnapshot) { mutableStateOf(patronMateriSnapshot) }
  var currentSessionRecord by remember(subject.id, dateIso, event.lessonSlotId) { mutableStateOf<GuruTeachingSessionRecord?>(null) }
  var attendanceStatuses by remember(subject.id, dateIso, event.lessonSlotId) { mutableStateOf<Map<String, String>>(emptyMap()) }
  var materialText by remember(subject.id, dateIso, event.lessonSlotId) { mutableStateOf("") }
  var teachingContent by remember(subject.id, dateIso, event.lessonSlotId) { mutableStateOf("") }
  var selectedNewMetricKey by remember { mutableStateOf(TeachingSessionMetricOptions.first().key) }
  var assessments by remember(subject.id, dateIso, event.lessonSlotId) {
    mutableStateOf<List<TeachingSessionAssessmentDraft>>(emptyList())
  }
  var taskTitle by remember(subject.id, dateIso, event.lessonSlotId) { mutableStateOf("") }
  var taskDescription by remember(subject.id, dateIso, event.lessonSlotId) { mutableStateOf("") }
  var feedbackMessage by remember { mutableStateOf<String?>(null) }
  var isLoading by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var hasInitializedAttendance by remember(subject.id, dateIso, event.lessonSlotId) { mutableStateOf(false) }
  var hasInitializedMaterial by remember(subject.id, dateIso, event.lessonSlotId) { mutableStateOf(false) }

  val attendanceStudents = currentAttendance?.students.orEmpty()
  val scoreStudents = currentScores?.students.orEmpty()
  val materialSuggestions = currentPatron?.items.orEmpty()
  val currentStep = TeachingSessionSteps.getOrElse(currentStepIndex) { TeachingSessionSteps.first() }
  val usedMetricKeys = assessments.map { it.metricKey }.toSet()
  val availableMetricOptions = TeachingSessionMetricOptions.filter { it.key !in usedMetricKeys }

  LaunchedEffect(subject.id, dateIso, event.lessonSlotId) {
    isLoading = true
    val loadedSession = onLoadTeachingSession(subject.id, event.lessonSlotId, dateIso)
    currentSessionRecord = loadedSession
    if (loadedSession != null) {
      materialText = loadedSession.material
      teachingContent = loadedSession.teachingContent
      taskTitle = loadedSession.taskTitle
      taskDescription = loadedSession.taskDescription
      onSessionSummaryChange(loadedSession.toTeachingSessionSummary())
      hasInitializedMaterial = true
    }
    currentAttendance = onLoadAttendance(subject.id, subject) ?: currentAttendance
    currentPatron = onLoadPatronMateri(subject.id, subject) ?: currentPatron
    currentScores = onLoadScores(subject.id, subject) ?: currentScores
    isLoading = false
  }

  LaunchedEffect(currentAttendance?.updatedAt, currentAttendance?.students, hasInitializedAttendance) {
    if (!hasInitializedAttendance && attendanceStudents.isNotEmpty()) {
      attendanceStatuses = attendanceStudents.associate { student ->
        student.id to (student.sessionEntry(dateIso, event.lessonSlotId)?.status ?: "Hadir")
      }
      hasInitializedAttendance = true
    }
  }

  LaunchedEffect(currentAttendance?.updatedAt, hasInitializedMaterial) {
    if (!hasInitializedMaterial) {
      materialText = currentAttendance
        ?.students
        .orEmpty()
        .asSequence()
        .mapNotNull { it.sessionEntry(dateIso, event.lessonSlotId)?.patronMateri?.trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
      hasInitializedMaterial = true
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    TeachingSessionTopBar(
      title = subject.title.ifBlank { cleanTeachingSessionText(event.title).ifBlank { "Sesi Mengajar" } },
      subtitle = listOf(subject.className, event.timeLabel)
        .map { cleanTeachingSessionText(it) }
        .filter { it.isNotBlank() }
        .joinToString(" - "),
      onBack = onBack
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(top = 8.dp, bottom = 132.dp)
    ) {
      item {
        TeachingSessionHeaderCard(
          event = event,
          selectedDate = selectedDate,
          subject = subject
        )
      }

      item {
        TeachingSessionStepRow(
          selectedIndex = currentStepIndex,
          onSelect = { currentStepIndex = it }
        )
      }

      if (isLoading) {
        item { TeachingSessionInfoCard("Memuat data sesi mengajar...", tone = PrimaryBlue) }
      }

      feedbackMessage?.let { message ->
        item {
          TeachingSessionInfoCard(
            message = message,
            tone = if (message.contains("berhasil", ignoreCase = true)) SuccessTint else WarmAccent
          )
        }
      }

      when (currentStep.key) {
        "materi" -> {
          item {
            TeachingSessionMaterialCard(
              materialText = materialText,
              onMaterialChange = {
                materialText = it
                feedbackMessage = null
              },
              suggestions = materialSuggestions,
              onSuggestionClick = {
                materialText = it.text
                feedbackMessage = null
              }
            )
          }
        }

        "absensi" -> {
          if (attendanceStudents.isEmpty()) {
            item {
              TeachingSessionInfoCard("Belum ada data santri untuk absensi sesi ini.", tone = WarmAccent)
            }
          } else {
            items(
              items = attendanceStudents,
              key = { it.id }
            ) { student ->
              TeachingSessionAttendanceCard(
                student = student,
                selectedStatus = attendanceStatuses[student.id] ?: "Hadir",
                onStatusSelect = { status ->
                  attendanceStatuses = attendanceStatuses.toMutableMap().apply {
                    put(student.id, status)
                  }
                  feedbackMessage = null
                }
              )
            }
          }
        }

        "isi" -> {
          item {
            TeachingSessionContentCard(
              value = teachingContent,
              onValueChange = {
                teachingContent = it
                feedbackMessage = null
              }
            )
          }
        }

        "nilai" -> {
          item {
            TeachingSessionAssessmentToolbar(
              selectedMetricKey = selectedNewMetricKey,
              availableMetrics = availableMetricOptions,
              onMetricSelect = { selectedNewMetricKey = it },
              onAddAssessment = {
                val metric = TeachingSessionMetricOptions.firstOrNull { it.key == selectedNewMetricKey }
                  ?: availableMetricOptions.firstOrNull()
                if (metric != null && assessments.none { it.metricKey == metric.key }) {
                  val initialValues = scoreStudents.associate { student ->
                    val existing = student.detailRowsByMetric[metric.key]
                      .orEmpty()
                      .firstOrNull { it.dateIso == dateIso }
                    student.id to existing?.value?.let(::formatTeachingSessionNumber).orEmpty()
                  }
                  assessments = assessments + TeachingSessionAssessmentDraft(
                    id = "assessment-${metric.key}",
                    metricKey = metric.key,
                    valuesByStudentId = initialValues
                  )
                  selectedNewMetricKey = TeachingSessionMetricOptions
                    .firstOrNull { it.key !in (usedMetricKeys + metric.key) }
                    ?.key
                    ?: metric.key
                  feedbackMessage = null
                }
              }
            )
          }

          if (scoreStudents.isEmpty()) {
            item {
              TeachingSessionInfoCard("Belum ada data santri untuk penilaian sesi ini.", tone = WarmAccent)
            }
          } else if (assessments.isEmpty()) {
            item {
              TeachingSessionInfoCard("Penilaian opsional. Tambahkan jenis nilai jika sesi ini memakai penilaian.", tone = PrimaryBlue)
            }
          } else {
            items(
              items = assessments,
              key = { it.id }
            ) { assessment ->
              val metric = TeachingSessionMetricOptions.first { it.key == assessment.metricKey }
              TeachingSessionAssessmentCard(
                metric = metric,
                students = scoreStudents,
                draft = assessment,
                onValueChange = { studentId, value ->
                  val safeValue = sanitizeTeachingSessionScoreText(value, metric.maxValue)
                  if (safeValue != null) {
                    assessments = assessments.map { item ->
                      if (item.id == assessment.id) {
                        item.copy(valuesByStudentId = item.valuesByStudentId.toMutableMap().apply {
                          put(studentId, safeValue)
                        })
                      } else {
                        item
                      }
                    }
                    feedbackMessage = null
                  }
                },
                onRemove = {
                  assessments = assessments.filterNot { it.id == assessment.id }
                  feedbackMessage = null
                }
              )
            }
          }
        }

        "tugas" -> {
          item {
            TeachingSessionTaskCard(
              title = taskTitle,
              description = taskDescription,
              onTitleChange = {
                taskTitle = it
                feedbackMessage = null
              },
              onDescriptionChange = {
                taskDescription = it
                feedbackMessage = null
              }
            )
          }
        }
      }

      item {
        TeachingSessionActionRow(
          currentStepIndex = currentStepIndex,
          isSaving = isSaving,
          onPrevious = { currentStepIndex = (currentStepIndex - 1).coerceAtLeast(0) },
          onNext = { currentStepIndex = (currentStepIndex + 1).coerceAtMost(TeachingSessionSteps.lastIndex) },
          onSaveDraft = {
            scope.launch {
              isSaving = true
              feedbackMessage = null
              val draftRecord = buildTeachingSessionRecord(
                currentRecord = currentSessionRecord,
                subject = subject,
                event = event,
                dateIso = dateIso,
                material = materialText,
                teachingContent = teachingContent,
                taskTitle = taskTitle,
                taskDescription = taskDescription,
                assessments = assessments,
                status = "draft"
              )
              val outcome = onSaveTeachingSession(draftRecord)
              if (outcome.success) {
                currentSessionRecord = outcome.record ?: draftRecord
                onSessionSummaryChange((outcome.record ?: draftRecord).toTeachingSessionSummary())
              }
              feedbackMessage = outcome.message
              isSaving = false
            }
          },
          onSave = {
            scope.launch {
              isSaving = true
              feedbackMessage = null
              val material = materialText.trim()
              val patronOutcome = saveTeachingSessionPatronIfNeeded(
                material = material,
                subject = subject,
                currentPatron = currentPatron,
                onSavePatronMateri = onSavePatronMateri
              )
              if (patronOutcome != null && !patronOutcome.success) {
                feedbackMessage = patronOutcome.message
                isSaving = false
                return@launch
              }

              if (attendanceStudents.isNotEmpty()) {
                val changesByStudent = attendanceStudents.associate { student ->
                  val existing = student.sessionEntry(dateIso, event.lessonSlotId)
                  student.id to listOf(
                    AttendanceHistoryEntry(
                      dateIso = dateIso,
                      status = attendanceStatuses[student.id] ?: "Hadir",
                      rowIds = existing?.rowIds.orEmpty(),
                      lessonSlotId = event.lessonSlotId,
                      patronMateri = material
                    )
                  )
                }
                val attendanceOutcome = onSaveAttendanceBatch(subject.id, subject, changesByStudent, "", "")
                if (!attendanceOutcome.success) {
                  feedbackMessage = attendanceOutcome.message
                  isSaving = false
                  return@launch
                }
                currentAttendance = onLoadAttendance(subject.id, subject) ?: currentAttendance
              }

              val scoreOutcome = saveTeachingSessionAssessments(
                assessments = assessments,
                scoreSnapshot = currentScores,
                subject = subject,
                dateIso = dateIso,
                material = material,
                onSaveScoresBatch = onSaveScoresBatch
              )
              if (scoreOutcome != null && !scoreOutcome.success) {
                feedbackMessage = scoreOutcome.message
                isSaving = false
                return@launch
              }
              if (assessments.isNotEmpty()) {
                currentScores = onLoadScores(subject.id, subject) ?: currentScores
              }
              currentPatron = onLoadPatronMateri(subject.id, subject) ?: currentPatron

              val sessionRecord = buildTeachingSessionRecord(
                currentRecord = currentSessionRecord,
                subject = subject,
                event = event,
                dateIso = dateIso,
                material = material,
                teachingContent = teachingContent,
                taskTitle = taskTitle,
                taskDescription = taskDescription,
                assessments = assessments,
                status = "saved"
              )
              val sessionOutcome = onSaveTeachingSession(sessionRecord)
              if (!sessionOutcome.success) {
                feedbackMessage = sessionOutcome.message
                isSaving = false
                return@launch
              }
              currentSessionRecord = sessionOutcome.record ?: sessionRecord
              onSessionSummaryChange((sessionOutcome.record ?: sessionRecord).toTeachingSessionSummary())
              feedbackMessage = sessionOutcome.message
              isSaving = false
            }
          }
        )
      }
    }
  }
}

@Composable
internal fun TeachingSessionCardBadges(
  summary: TeachingSessionSummary,
  modifier: Modifier = Modifier
) {
  LazyRow(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(top = 4.dp)
  ) {
    if (summary.isSaved) {
      item { TeachingSessionBadge("Tersimpan", SuccessTint) }
    } else if (summary.isDraft) {
      item { TeachingSessionBadge("Draft", WarmAccent) }
    }
    if (summary.material.isNotBlank()) {
      item { TeachingSessionBadge(summary.material, PrimaryBlue) }
    }
    if (summary.hasAssessment) {
      item { TeachingSessionBadge("Ada penilaian", PrimaryBlueDark) }
    }
    if (summary.hasTask) {
      item { TeachingSessionBadge("Ada tugas", WarmAccent) }
    }
  }
}

@Composable
private fun TeachingSessionBadge(
  label: String,
  color: Color
) {
  Surface(
    color = color.copy(alpha = 0.14f),
    contentColor = color,
    shape = RoundedCornerShape(999.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.28f))
  ) {
    Text(
      text = ti(label),
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
    )
  }
}

@Composable
private fun TeachingSessionTopBar(
  title: String,
  subtitle: String,
  onBack: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Surface(
      modifier = Modifier
        .size(44.dp)
        .clickable(onClick = onBack),
      color = CardBackground.copy(alpha = 0.92f),
      shape = CircleShape,
      border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
          contentDescription = t("Kembali"),
          tint = PrimaryBlueDark
        )
      }
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = ti(title),
        style = MaterialTheme.typography.titleMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (subtitle.isNotBlank()) {
        Text(
          text = ti(subtitle),
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
private fun TeachingSessionHeaderCard(
  event: CalendarEvent,
  selectedDate: LocalDate,
  subject: SubjectOverview
) {
  TeachingSessionPanel {
    Text(
      text = "Classroom",
      style = MaterialTheme.typography.labelLarge,
      color = PrimaryBlue,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = ti(subject.title.ifBlank { cleanTeachingSessionText(event.title) }),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = listOf(
        subject.className,
        formatDateForLanguage(selectedDate, "EEEE, dd MMM yyyy", com.mim.guruapp.ui.i18n.LocalAppLanguage.current),
        event.timeLabel
      ).map { cleanTeachingSessionText(it) }.filter { it.isNotBlank() }.joinToString(" - "),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
  }
}

@Composable
private fun TeachingSessionStepRow(
  selectedIndex: Int,
  onSelect: (Int) -> Unit
) {
  LazyRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(horizontal = 2.dp)
  ) {
    items(TeachingSessionSteps.size) { index ->
      val step = TeachingSessionSteps[index]
      val selected = index == selectedIndex
      Surface(
        modifier = Modifier.clickable { onSelect(index) },
        color = if (selected) PrimaryBlue else CardBackground,
        contentColor = if (selected) Color.White else PrimaryBlueDark,
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          if (selected) PrimaryBlue else CardBorder
        )
      ) {
        Text(
          text = step.label,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
      }
    }
  }
}

@Composable
private fun TeachingSessionMaterialCard(
  materialText: String,
  onMaterialChange: (String) -> Unit,
  suggestions: List<PatronMateriItem>,
  onSuggestionClick: (PatronMateriItem) -> Unit
) {
  TeachingSessionPanel {
    TeachingSessionSectionTitle("Materi Pertemuan", "Pilih dari patron materi atau tulis materi baru.")
    OutlinedTextField(
      value = materialText,
      onValueChange = onMaterialChange,
      label = { Text("Materi") },
      modifier = Modifier.fillMaxWidth(),
      minLines = 2
    )
    if (suggestions.isNotEmpty()) {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(suggestions, key = { it.id }) { item ->
          TeachingSessionChoiceChip(
            label = item.text,
            selected = materialText.trim().equals(item.text.trim(), ignoreCase = true),
            onClick = { onSuggestionClick(item) }
          )
        }
      }
    }
  }
}

@Composable
private fun TeachingSessionAttendanceCard(
  student: AttendanceStudent,
  selectedStatus: String,
  onStatusSelect: (String) -> Unit
) {
  TeachingSessionPanel {
    Text(
      text = ti(student.name),
      style = MaterialTheme.typography.titleSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.Bold
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(TeachingSessionStatusOptions) { status ->
        TeachingSessionChoiceChip(
          label = status,
          selected = selectedStatus == status,
          onClick = { onStatusSelect(status) }
        )
      }
    }
  }
}

@Composable
private fun TeachingSessionContentCard(
  value: String,
  onValueChange: (String) -> Unit
) {
  TeachingSessionPanel {
    TeachingSessionSectionTitle("Isi Materi Ajar", "Catatan ini membantu guru menelusuri isi pembelajaran pada sesi tersebut.")
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      label = { Text("Isi materi ajar") },
      modifier = Modifier.fillMaxWidth(),
      minLines = 5
    )
  }
}

@Composable
private fun TeachingSessionAssessmentToolbar(
  selectedMetricKey: String,
  availableMetrics: List<TeachingSessionMetric>,
  onMetricSelect: (String) -> Unit,
  onAddAssessment: () -> Unit
) {
  TeachingSessionPanel {
    TeachingSessionSectionTitle("Penilaian Opsional", "Tambahkan jenis nilai yang dipakai pada sesi ini.")
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      TeachingSessionMetricDropdown(
        selectedMetricKey = selectedMetricKey,
        options = availableMetrics.ifEmpty { TeachingSessionMetricOptions },
        onSelect = onMetricSelect,
        modifier = Modifier.weight(1f)
      )
      Button(
        onClick = onAddAssessment,
        enabled = availableMetrics.isNotEmpty()
      ) {
        Text("Tambah")
      }
    }
  }
}

@Composable
private fun TeachingSessionAssessmentCard(
  metric: TeachingSessionMetric,
  students: List<ScoreStudent>,
  draft: TeachingSessionAssessmentDraft,
  onValueChange: (String, String) -> Unit,
  onRemove: () -> Unit
) {
  TeachingSessionPanel {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = metric.label,
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = "Maksimal ${formatTeachingSessionNumber(metric.maxValue)}",
          style = MaterialTheme.typography.labelSmall,
          color = SubtleInk
        )
      }
      TextButton(onClick = onRemove) {
        Text("Hapus")
      }
    }
    students.forEach { student ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = ti(student.name),
          style = MaterialTheme.typography.bodyMedium,
          color = PrimaryBlueDark,
          modifier = Modifier.weight(1f),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        OutlinedTextField(
          value = draft.valuesByStudentId[student.id].orEmpty(),
          onValueChange = { onValueChange(student.id, it) },
          modifier = Modifier.width(96.dp),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
      }
    }
  }
}

@Composable
private fun TeachingSessionTaskCard(
  title: String,
  description: String,
  onTitleChange: (String) -> Unit,
  onDescriptionChange: (String) -> Unit
) {
  TeachingSessionPanel {
    TeachingSessionSectionTitle("Tugas atau Proyek", "Opsional. Isi jika ada tindak lanjut setelah sesi.")
    OutlinedTextField(
      value = title,
      onValueChange = onTitleChange,
      label = { Text("Judul tugas/proyek") },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true
    )
    OutlinedTextField(
      value = description,
      onValueChange = onDescriptionChange,
      label = { Text("Keterangan") },
      modifier = Modifier.fillMaxWidth(),
      minLines = 4
    )
  }
}

@Composable
private fun TeachingSessionActionRow(
  currentStepIndex: Int,
  isSaving: Boolean,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onSaveDraft: () -> Unit,
  onSave: () -> Unit
) {
  TeachingSessionPanel {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedButton(
        onClick = onPrevious,
        enabled = currentStepIndex > 0 && !isSaving,
        modifier = Modifier.weight(1f)
      ) {
        Text("Kembali")
      }
      OutlinedButton(
        onClick = onNext,
        enabled = currentStepIndex < TeachingSessionSteps.lastIndex && !isSaving,
        modifier = Modifier.weight(1f)
      ) {
        Text("Next")
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedButton(
        onClick = onSaveDraft,
        enabled = !isSaving,
        modifier = Modifier.weight(1f)
      ) {
        Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Draft")
      }
      Button(
        onClick = onSave,
        enabled = !isSaving,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
      ) {
        if (isSaving) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = Color.White
          )
        } else {
          Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text("Simpan")
      }
    }
  }
}

@Composable
private fun TeachingSessionMetricDropdown(
  selectedMetricKey: String,
  options: List<TeachingSessionMetric>,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  val selected = options.firstOrNull { it.key == selectedMetricKey }
    ?: TeachingSessionMetricOptions.firstOrNull { it.key == selectedMetricKey }
    ?: options.firstOrNull()

  Box(modifier = modifier) {
    OutlinedButton(
      onClick = { expanded = true },
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = selected?.label.orEmpty().ifBlank { "Pilih jenis" },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      options.forEach { option ->
        DropdownMenuItem(
          text = { Text(option.label) },
          onClick = {
            onSelect(option.key)
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
private fun TeachingSessionChoiceChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    modifier = Modifier.clickable(onClick = onClick),
    color = if (selected) PrimaryBlue else SoftPanel.copy(alpha = 0.78f),
    contentColor = if (selected) Color.White else PrimaryBlueDark,
    shape = RoundedCornerShape(999.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (selected) PrimaryBlue else CardBorder
    )
  ) {
    Text(
      text = ti(label),
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun TeachingSessionPanel(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(CardBackground.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
      .border(1.dp, CardBorder.copy(alpha = 0.84f), RoundedCornerShape(20.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    content = content
  )
}

@Composable
private fun TeachingSessionSectionTitle(
  title: String,
  subtitle: String
) {
  Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
  }
}

@Composable
private fun TeachingSessionInfoCard(
  message: String,
  tone: Color
) {
  Surface(
    color = tone.copy(alpha = 0.12f),
    contentColor = tone,
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, tone.copy(alpha = 0.22f))
  ) {
    Text(
      text = ti(message),
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(14.dp)
    )
  }
}

internal fun teachingSessionKey(event: CalendarEvent, date: LocalDate): String {
  val subjectKey = event.distribusiId.ifBlank { event.id }
  val slotKey = event.lessonSlotId.ifBlank { event.timeLabel.ifBlank { event.id } }
  return "${date}|${subjectKey}|${slotKey}"
}

internal fun buildTeachingSessionSummary(
  event: CalendarEvent,
  dateIso: String,
  attendanceSnapshots: List<MapelAttendanceSnapshot>,
  scoreSnapshots: List<MapelScoreSnapshot>,
  localSummary: TeachingSessionSummary?
): TeachingSessionSummary {
  val attendanceSnapshot = attendanceSnapshots.firstOrNull { it.distribusiId == event.distribusiId }
  val sessionEntries = attendanceSnapshot
    ?.students
    .orEmpty()
    .mapNotNull { it.sessionEntry(dateIso, event.lessonSlotId) }
  val serverMaterial = sessionEntries
    .asSequence()
    .map { it.patronMateri.trim() }
    .firstOrNull { it.isNotBlank() }
    .orEmpty()
  val scoreSnapshot = scoreSnapshots.firstOrNull { it.distribusiId == event.distribusiId }
  val hasServerAssessment = scoreSnapshot
    ?.students
    .orEmpty()
    .any { student ->
      student.detailRowsByMetric.values.flatten().any { row ->
        row.dateIso == dateIso && (
          serverMaterial.isBlank() ||
            row.material.trim().equals(serverMaterial, ignoreCase = true) ||
            row.material.isBlank()
          )
      }
    }

  val saved = sessionEntries.isNotEmpty() || localSummary?.isSaved == true
  return TeachingSessionSummary(
    material = serverMaterial.ifBlank { localSummary?.material.orEmpty() },
    isSaved = saved,
    isDraft = !saved && localSummary?.isDraft == true,
    hasAssessment = hasServerAssessment || localSummary?.hasAssessment == true,
    hasTask = localSummary?.hasTask == true
  )
}

internal fun CalendarEvent.toTeachingSessionSubjectFallback(): SubjectOverview {
  return SubjectOverview(
    id = distribusiId.ifBlank { id },
    title = cleanTeachingSessionText(title).ifBlank { "Jadwal Mengajar" },
    className = "",
    semester = "",
    semesterActive = true,
    attendancePending = 0,
    scorePending = 0,
    materialCount = 0
  )
}

private fun buildTeachingSessionRecord(
  currentRecord: GuruTeachingSessionRecord?,
  subject: SubjectOverview,
  event: CalendarEvent,
  dateIso: String,
  material: String,
  teachingContent: String,
  taskTitle: String,
  taskDescription: String,
  assessments: List<TeachingSessionAssessmentDraft>,
  status: String
): GuruTeachingSessionRecord {
  return GuruTeachingSessionRecord(
    id = currentRecord?.id.orEmpty(),
    dateIso = dateIso,
    distribusiId = subject.id,
    lessonSlotId = event.lessonSlotId,
    material = material.trim(),
    teachingContent = teachingContent.trim(),
    taskTitle = taskTitle.trim(),
    taskDescription = taskDescription.trim(),
    assessmentSummaryJson = buildTeachingSessionAssessmentSummaryJson(assessments),
    status = if (status == "saved") "saved" else "draft",
    updatedAt = currentRecord?.updatedAt.orEmpty()
  )
}

private fun buildTeachingSessionAssessmentSummaryJson(
  assessments: List<TeachingSessionAssessmentDraft>
): String {
  val array = JSONArray()
  assessments.forEach { assessment ->
    val metric = TeachingSessionMetricOptions.firstOrNull { it.key == assessment.metricKey } ?: return@forEach
    val filledCount = assessment.valuesByStudentId.values.count { it.isNotBlank() }
    if (filledCount <= 0) return@forEach
    array.put(JSONObject().apply {
      put("metric_key", metric.key)
      put("label", metric.label)
      put("filled_count", filledCount)
    })
  }
  return array.toString()
}

private fun GuruTeachingSessionRecord.toTeachingSessionSummary(): TeachingSessionSummary {
  val hasAssessment = runCatching { JSONArray(assessmentSummaryJson).length() > 0 }.getOrDefault(false)
  return TeachingSessionSummary(
    material = material,
    isDraft = status != "saved",
    isSaved = status == "saved",
    hasAssessment = hasAssessment,
    hasTask = taskTitle.isNotBlank() || taskDescription.isNotBlank()
  )
}

private fun AttendanceStudent.sessionEntry(
  dateIso: String,
  lessonSlotId: String
): AttendanceHistoryEntry? {
  val normalizedLessonSlotId = lessonSlotId.trim()
  return history.firstOrNull { entry ->
    entry.dateIso == dateIso &&
      normalizedLessonSlotId.isNotBlank() &&
      entry.lessonSlotId.trim() == normalizedLessonSlotId
  } ?: history.firstOrNull { entry ->
    entry.dateIso == dateIso &&
      (entry.lessonSlotId.isBlank() || normalizedLessonSlotId.isBlank())
  } ?: history.firstOrNull { it.dateIso == dateIso }
}

private suspend fun saveTeachingSessionPatronIfNeeded(
  material: String,
  subject: SubjectOverview,
  currentPatron: MapelPatronMateriSnapshot?,
  onSavePatronMateri: suspend (String, SubjectOverview, List<PatronMateriItem>) -> PatronMateriSaveOutcome
): PatronMateriSaveOutcome? {
  if (material.isBlank()) return null
  val existingItems = currentPatron?.items.orEmpty()
  if (existingItems.any { it.text.trim().equals(material, ignoreCase = true) }) return null
  return onSavePatronMateri(
    subject.id,
    subject,
    existingItems + PatronMateriItem(
      id = "materi-${existingItems.size + 1}-${material.hashCode()}",
      text = material
    )
  )
}

private suspend fun saveTeachingSessionAssessments(
  assessments: List<TeachingSessionAssessmentDraft>,
  scoreSnapshot: MapelScoreSnapshot?,
  subject: SubjectOverview,
  dateIso: String,
  material: String,
  onSaveScoresBatch: suspend (String, SubjectOverview, List<ScoreStudent>) -> ScoreSaveOutcome
): ScoreSaveOutcome? {
  val students = scoreSnapshot?.students.orEmpty()
  if (students.isEmpty() || assessments.isEmpty()) return null

  assessments.forEach { assessment ->
    val metric = TeachingSessionMetricOptions.firstOrNull { it.key == assessment.metricKey } ?: return@forEach
    val changedStudents = students.mapNotNull { student ->
      val parsedValue = assessment.valuesByStudentId[student.id]
        ?.toDoubleOrNull()
        ?.takeIf { it >= 0.0 && it <= metric.maxValue }
      if (parsedValue == null) {
        null
      } else {
        buildTeachingSessionUpdatedScoreStudent(
          student = student,
          metric = metric,
          dateIso = dateIso,
          value = parsedValue,
          material = material
        )
      }
    }
    if (changedStudents.isNotEmpty()) {
      val outcome = onSaveScoresBatch(subject.id, subject, changedStudents)
      if (!outcome.success) return outcome
    }
  }
  return null
}

private fun buildTeachingSessionUpdatedScoreStudent(
  student: ScoreStudent,
  metric: TeachingSessionMetric,
  dateIso: String,
  value: Double,
  material: String
): ScoreStudent {
  val existingRows = student.detailRowsByMetric[metric.key].orEmpty()
  val normalizedMaterial = material.trim()
  val existingForDate = existingRows.firstOrNull { row ->
    row.dateIso == dateIso && (
      normalizedMaterial.isBlank() ||
        row.material.trim().equals(normalizedMaterial, ignoreCase = true)
      )
  } ?: existingRows.firstOrNull { it.dateIso == dateIso }
  val nextRow = ScoreDetailRow(
    id = existingForDate?.id.orEmpty(),
    dateIso = dateIso,
    value = value,
    material = normalizedMaterial
  )
  val nextRows = (existingRows.filterNot { row ->
    if (existingForDate?.id?.isNotBlank() == true) row.id == existingForDate.id else row.dateIso == dateIso
  } + nextRow).sortedByDescending { it.dateIso }
  val nextDetailRows = student.detailRowsByMetric.toMutableMap().apply {
    put(metric.key, nextRows)
  }
  val average = calculateTeachingSessionAverage(nextRows.mapNotNull { it.value })
  return applyTeachingSessionMetricValue(student, metric.key, average).copy(
    detailRowsByMetric = nextDetailRows,
    pendingDetailMetricKey = metric.key,
    pendingDeletedDetailIds = emptyList()
  )
}

private fun applyTeachingSessionMetricValue(
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

private fun calculateTeachingSessionAverage(values: List<Double>): Double? {
  if (values.isEmpty()) return null
  return round(values.sum() / values.size.toDouble() * 100.0) / 100.0
}

private fun sanitizeTeachingSessionScoreText(
  raw: String,
  maxValue: Double
): String? {
  val normalized = raw.replace(',', '.').trim()
  if (normalized.isBlank()) return ""
  val dotCount = normalized.count { it == '.' }
  if (dotCount > 1 || normalized.any { !it.isDigit() && it != '.' }) return null
  val parsed = normalized.toDoubleOrNull() ?: return null
  if (parsed < 0.0 || parsed > maxValue) return null
  return normalized
}

private fun formatTeachingSessionNumber(value: Double): String {
  val rounded = round(value * 100.0) / 100.0
  return if (rounded % 1.0 == 0.0) {
    rounded.toInt().toString()
  } else {
    rounded.toString().trimEnd('0').trimEnd('.')
  }
}

private fun cleanTeachingSessionText(value: String): String {
  return value
    .replace(Regex("\\s+"), " ")
    .trim()
}
