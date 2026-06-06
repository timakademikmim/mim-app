package com.mim.guruapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.mim.guruapp.data.model.UtsClassSubject
import com.mim.guruapp.data.model.UtsReportSnapshot
import com.mim.guruapp.data.model.UtsScoreRow
import com.mim.guruapp.data.model.UtsSemesterInfo
import com.mim.guruapp.data.model.WaliSantriProfile
import com.mim.guruapp.data.model.WaliSantriSnapshot
import com.mim.guruapp.ui.i18n.t
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaporScreen(
  waliSantriSnapshot: WaliSantriSnapshot,
  utsReportSnapshot: UtsReportSnapshot,
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier
) {
  val semesters = remember(utsReportSnapshot.semesters) {
    utsReportSnapshot.semesters.ifEmpty {
      listOf(UtsSemesterInfo(id = "", label = "Semester aktif"))
    }
  }
  var selectedSemesterId by rememberSaveable {
    mutableStateOf(semesters.firstOrNull { it.isActive }?.id ?: semesters.firstOrNull()?.id.orEmpty())
  }
  var selectedSantriId by rememberSaveable { mutableStateOf<String?>(null) }
  var query by rememberSaveable { mutableStateOf("") }

  LaunchedEffect(semesters) {
    if (selectedSemesterId !in semesters.map { it.id }) {
      selectedSemesterId = semesters.firstOrNull { it.isActive }?.id ?: semesters.firstOrNull()?.id.orEmpty()
    }
  }

  val selectedSemester = semesters.firstOrNull { it.id == selectedSemesterId } ?: semesters.firstOrNull() ?: UtsSemesterInfo()
  val students = remember(waliSantriSnapshot.students) {
    waliSantriSnapshot.students.sortedWith(compareBy<WaliSantriProfile> { it.className }.thenBy { it.name })
  }
  val reports = remember(students, selectedSemester, utsReportSnapshot) {
    students.associate { student ->
      student.id to buildRaporStudentReport(
        student = student,
        semester = selectedSemester,
        snapshot = utsReportSnapshot
      )
    }
  }
  val selectedSantri = students.firstOrNull { it.id == selectedSantriId }
  val showSkeleton = students.isEmpty() && (isRefreshing || waliSantriSnapshot.updatedAt <= 0L)

  BackHandler(enabled = selectedSantri != null) {
    selectedSantriId = null
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
  ) { innerPadding ->
    AnimatedContent(
      targetState = selectedSantri,
      transitionSpec = {
        val openingDetail = targetState != null
        fadeIn() + slideInHorizontally { width -> if (openingDetail) width / 5 else -width / 5 } togetherWith
          fadeOut() + slideOutHorizontally { width -> if (openingDetail) -width / 6 else width / 6 }
      },
      label = "rapor-content",
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) { activeSantri ->
      if (activeSantri == null) {
        PullToRefreshBox(
          isRefreshing = isRefreshing,
          onRefresh = onRefresh,
          modifier = Modifier.fillMaxSize()
        ) {
          RaporListContent(
            isWaliKelas = waliSantriSnapshot.isWaliKelas,
            students = students,
            reports = reports,
            semesters = semesters,
            selectedSemester = selectedSemester,
            selectedSemesterId = selectedSemesterId,
            onSemesterChange = { selectedSemesterId = it },
            query = query,
            onQueryChange = { query = it },
            showSkeleton = showSkeleton,
            onMenuClick = onMenuClick,
            onSantriClick = { selectedSantriId = it.id }
          )
        }
      } else {
        RaporDetailContent(
          santri = activeSantri,
          report = reports[activeSantri.id] ?: RaporStudentReport(),
          onBackClick = { selectedSantriId = null }
        )
      }
    }
  }
}

@Composable
private fun RaporListContent(
  isWaliKelas: Boolean,
  students: List<WaliSantriProfile>,
  reports: Map<String, RaporStudentReport>,
  semesters: List<UtsSemesterInfo>,
  selectedSemester: UtsSemesterInfo,
  selectedSemesterId: String,
  onSemesterChange: (String) -> Unit,
  query: String,
  onQueryChange: (String) -> Unit,
  showSkeleton: Boolean,
  onMenuClick: () -> Unit,
  onSantriClick: (WaliSantriProfile) -> Unit
) {
  val filteredStudents = remember(students, query) {
    val needle = query.trim().lowercase()
    if (needle.isBlank()) {
      students
    } else {
      students.filter { student ->
        student.name.lowercase().contains(needle) ||
          student.nisn.lowercase().contains(needle) ||
          student.className.lowercase().contains(needle)
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .navigationBarsPadding()
      .padding(horizontal = 18.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    RaporTopBar(
      title = "Rapor",
      leadingIcon = Icons.Outlined.Menu,
      leadingContentDescription = "Buka sidebar",
      onLeadingClick = onMenuClick
    )

    RaporSemesterSelector(
      semesters = semesters,
      selectedSemesterId = selectedSemesterId,
      selectedSemester = selectedSemester,
      onSemesterChange = onSemesterChange
    )

    RaporSearchBar(
      query = query,
      onQueryChange = onQueryChange,
      modifier = Modifier.fillMaxWidth()
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(bottom = 124.dp)
    ) {
      when {
        showSkeleton -> {
          items(7) { index ->
            RaporSkeletonCard(index = index)
          }
        }

        filteredStudents.isEmpty() -> {
          item {
            EmptyPlaceholderCard(
              message = when {
                !isWaliKelas -> "Menu rapor hanya tersedia untuk wali kelas. Tarik ke bawah untuk refresh data wali kelas."
                students.isEmpty() -> "Belum ada santri pada kelas wali ini."
                else -> "Tidak ada santri yang cocok dengan pencarian."
              }
            )
          }
        }

        else -> {
          items(filteredStudents, key = { it.id }) { student ->
            RaporStudentCard(
              santri = student,
              report = reports[student.id] ?: RaporStudentReport(),
              onClick = { onSantriClick(student) }
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(10.dp))
      }
    }
  }
}

@Composable
private fun RaporDetailContent(
  santri: WaliSantriProfile,
  report: RaporStudentReport,
  onBackClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .navigationBarsPadding()
      .padding(horizontal = 18.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    RaporTopBar(
      title = "Detail Rapor",
      leadingIcon = Icons.AutoMirrored.Outlined.ArrowBack,
      leadingContentDescription = "Kembali ke daftar rapor",
      onLeadingClick = onBackClick
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(bottom = 28.dp)
    ) {
      item {
        RaporStudentHeader(
          santri = santri,
          semesterLabel = report.semesterLabel,
          academicYearLabel = report.academicYearLabel
        )
      }

      item {
        RaporIdentityPanel(report)
      }

      if (report.subjects.isEmpty()) {
        item {
          EmptyPlaceholderCard("Belum ada data nilai rapor untuk semester ini.")
        }
      } else {
        items(report.subjects, key = { it.subjectKey }) { subject ->
          RaporSubjectCard(subject)
        }
      }
    }
  }
}

@Composable
private fun RaporTopBar(
  title: String,
  leadingIcon: ImageVector,
  leadingContentDescription: String,
  onLeadingClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    RaporTopButton(
      icon = leadingIcon,
      contentDescription = leadingContentDescription,
      onClick = onLeadingClick
    )
    Text(
      text = t(title),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 12.dp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.size(42.dp))
  }
}

@Composable
private fun RaporTopButton(
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
      contentDescription = t(contentDescription),
      tint = PrimaryBlueDark
    )
  }
}

@Composable
private fun RaporSemesterSelector(
  semesters: List<UtsSemesterInfo>,
  selectedSemesterId: String,
  selectedSemester: UtsSemesterInfo,
  onSemesterChange: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
      .clickable { expanded = true }
      .padding(horizontal = 14.dp, vertical = 12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = t("Semester"),
          style = MaterialTheme.typography.labelMedium,
          color = SubtleInk
        )
        Text(
          text = semesterDisplayLabel(selectedSemester),
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
      }
      Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = PrimaryBlueDark)
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.background(CardBackground)
    ) {
      semesters.forEach { semester ->
        DropdownMenuItem(
          text = { Text(semesterDisplayLabel(semester)) },
          onClick = {
            expanded = false
            onSemesterChange(semester.id)
          },
          leadingIcon = if (semester.id == selectedSemesterId) {
            { Icon(Icons.Outlined.Grade, contentDescription = null, tint = PrimaryBlue) }
          } else {
            null
          }
        )
      }
    }
  }
}

@Composable
private fun RaporSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    singleLine = true,
    label = { Text(t("Cari santri")) },
    leadingIcon = {
      Icon(
        imageVector = Icons.Outlined.Search,
        contentDescription = t("Cari"),
        tint = SubtleInk
      )
    },
    modifier = modifier,
    shape = RoundedCornerShape(20.dp)
  )
}

@Composable
private fun RaporStudentCard(
  santri: WaliSantriProfile,
  report: RaporStudentReport,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(24.dp))
      .background(CardBackground.copy(alpha = 0.96f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
      .clickable(onClick = onClick)
      .padding(14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    RaporAvatar(name = santri.name, size = 50)
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Text(
        text = santri.name,
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = listOf(
          santri.className.ifBlank { "-" },
          if (santri.nisn.isBlank()) "NISN belum tersedia" else "NISN ${santri.nisn}"
        ).joinToString(" - "),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    RaporScoreBadge(report.averageKnowledgeText.ifBlank { "-" })
  }
}

@Composable
private fun RaporStudentHeader(
  santri: WaliSantriProfile,
  semesterLabel: String,
  academicYearLabel: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(28.dp))
      .background(PrimaryBlue.copy(alpha = 0.10f))
      .border(1.dp, PrimaryBlue.copy(alpha = 0.18f), RoundedCornerShape(28.dp))
      .padding(18.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    RaporAvatar(name = santri.name, size = 84)
    Text(
      text = santri.name.ifBlank { t("Nama Santri") },
      style = MaterialTheme.typography.titleLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = listOf(
        santri.className.ifBlank { "-" },
        semesterLabel.ifBlank { "-" },
        academicYearLabel.ifBlank { "-" }
      ).joinToString(" - "),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
  }
}

@Composable
private fun RaporIdentityPanel(report: RaporStudentReport) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x120F172A), spotColor = Color(0x120F172A))
      .clip(RoundedCornerShape(24.dp))
      .background(CardBackground.copy(alpha = 0.96f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    RaporInfoRow("Nama", report.studentName.ifBlank { "-" })
    RaporInfoRow("NISN", report.studentNisn.ifBlank { "-" })
    RaporInfoRow("Semester", report.semesterLabel.ifBlank { "-" })
    RaporInfoRow("Tahun ajaran", report.academicYearLabel.ifBlank { "-" })
  }
}

@Composable
private fun RaporSubjectCard(subject: RaporSubjectReport) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(24.dp))
      .background(CardBackground.copy(alpha = 0.96f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = subject.subjectName.ifBlank { "-" },
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = "KKM ${subject.kkmText.ifBlank { "-" }}",
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
      Icon(Icons.Outlined.School, contentDescription = null, tint = PrimaryBlue)
    }

    RaporScoreSection(
      title = "Nilai Pengetahuan",
      scoreText = subject.knowledgeScoreText,
      description = subject.knowledgeDescription
    )
    RaporScoreSection(
      title = "Nilai Keterampilan",
      scoreText = subject.skillScoreText,
      description = subject.skillDescription
    )
  }
}

@Composable
private fun RaporScoreSection(
  title: String,
  scoreText: String,
  description: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(SoftPanel.copy(alpha = 0.72f))
      .border(1.dp, CardBorder.copy(alpha = 0.74f), RoundedCornerShape(18.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = t(title),
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      RaporScoreBadge(scoreText.ifBlank { "-" })
    }
    Text(
      text = description.ifBlank { "-" },
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
  }
}

@Composable
private fun RaporInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = t(label),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk,
      modifier = Modifier.weight(0.42f)
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.weight(0.58f),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun RaporScoreBadge(value: String) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(PrimaryBlue.copy(alpha = 0.12f))
      .border(1.dp, PrimaryBlue.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
      .padding(horizontal = 12.dp, vertical = 7.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = value.ifBlank { "-" },
      style = MaterialTheme.typography.labelLarge,
      color = PrimaryBlue,
      fontWeight = FontWeight.ExtraBold
    )
  }
}

@Composable
private fun RaporAvatar(name: String, size: Int) {
  Box(
    modifier = Modifier
      .size(size.dp)
      .background(PrimaryBlue.copy(alpha = 0.14f), CircleShape)
      .border(1.dp, PrimaryBlue.copy(alpha = 0.28f), CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = name.initials(),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlue,
      fontWeight = FontWeight.ExtraBold
    )
  }
}

@Composable
private fun RaporSkeletonCard(index: Int) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .background(CardBackground.copy(alpha = 0.70f))
      .border(1.dp, CardBorder.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
      .padding(14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    RaporAvatar(name = "Santri $index", size = 50)
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(0.74f)
          .height(14.dp)
          .clip(RoundedCornerShape(999.dp))
          .background(SoftPanel)
      )
      Box(
        modifier = Modifier
          .fillMaxWidth(0.46f)
          .height(12.dp)
          .clip(RoundedCornerShape(999.dp))
          .background(SoftPanel.copy(alpha = 0.74f))
      )
    }
  }
}

private data class RaporStudentReport(
  val studentId: String = "",
  val studentName: String = "",
  val studentNisn: String = "",
  val className: String = "",
  val semesterLabel: String = "",
  val academicYearLabel: String = "",
  val subjects: List<RaporSubjectReport> = emptyList(),
  val averageKnowledgeText: String = "-"
)

private data class RaporSubjectReport(
  val subjectKey: String,
  val subjectName: String,
  val kkmText: String,
  val knowledgeScoreText: String,
  val knowledgeDescription: String,
  val skillScoreText: String,
  val skillDescription: String
)

private fun buildRaporStudentReport(
  student: WaliSantriProfile,
  semester: UtsSemesterInfo,
  snapshot: UtsReportSnapshot
): RaporStudentReport {
  val subjects = snapshot.classSubjects
    .filter { subject -> subject.classId == student.classId && subject.semesterId == semester.id }
    .sortedWith(compareBy<UtsClassSubject> { it.name.lowercase() }.thenBy { it.mapelId })
    .map { subject ->
      val score = snapshot.scoreRows.firstOrNull { row ->
        row.studentId == student.id &&
          row.semesterId == semester.id &&
          row.mapelId == subject.mapelId
      }
      subject.toRaporSubjectReport(score)
    }
  val average = subjects
    .mapNotNull { it.knowledgeScoreText.toDoubleOrNull() }
    .average()
    .takeIf { !it.isNaN() }
    ?.let(::formatRaporScore)
    ?: "-"
  return RaporStudentReport(
    studentId = student.id,
    studentName = student.name,
    studentNisn = student.nisn,
    className = student.className,
    semesterLabel = semester.label,
    academicYearLabel = semester.academicYearDisplayLabel(),
    subjects = subjects,
    averageKnowledgeText = average
  )
}

private fun UtsClassSubject.toRaporSubjectReport(score: UtsScoreRow?): RaporSubjectReport {
  return RaporSubjectReport(
    subjectKey = "$semesterId|$mapelId",
    subjectName = name,
    kkmText = kkmText.ifBlank { "17" },
    knowledgeScoreText = score?.knowledgeScoreText.orEmpty().ifBlank { "-" },
    knowledgeDescription = score?.knowledgeDescription.orEmpty(),
    skillScoreText = score?.skillScoreText.orEmpty().ifBlank { "-" },
    skillDescription = score?.skillDescription.orEmpty()
  )
}

private fun semesterDisplayLabel(semester: UtsSemesterInfo): String {
  return listOf(
    semester.label.ifBlank { "Semester" },
    semester.academicYearDisplayLabel().ifBlank { null }
  ).filterNotNull().joinToString(" - ")
}

private fun UtsSemesterInfo.academicYearDisplayLabel(): String {
  if (tahunAjaranLabel.isNotBlank()) return tahunAjaranLabel
  val startYear = startDateIso.take(4).toIntOrNull()
  if (startYear != null) return "$startYear/${startYear + 1}"
  val today = LocalDate.now()
  val academicStart = if (today.monthValue >= 7) today.year else today.year - 1
  return "$academicStart/${academicStart + 1}"
}

private fun formatRaporScore(value: Double): String {
  val rounded = kotlin.math.round(value * 100.0) / 100.0
  return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "%.2f".format(rounded).trimEnd('0').trimEnd('.')
}

private fun String.initials(): String {
  val words = trim().split(Regex("\\s+")).filter { it.isNotBlank() }
  return when {
    words.isEmpty() -> "?"
    words.size == 1 -> words.first().take(2).uppercase()
    else -> "${words.first().first()}${words.last().first()}".uppercase()
  }
}
