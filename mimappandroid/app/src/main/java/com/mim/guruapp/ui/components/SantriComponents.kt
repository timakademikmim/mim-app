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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.SantriSaveOutcome
import com.mim.guruapp.data.model.WaliSantriProfile
import com.mim.guruapp.data.model.WaliSantriSnapshot
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk
import kotlinx.coroutines.launch

private data class SantriGenderOption(
  val code: String,
  val label: String
)

private val SantriGenderOptions = listOf(
  SantriGenderOption("L", "Laki-laki"),
  SantriGenderOption("P", "Perempuan")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SantriScreen(
  waliSantriSnapshot: WaliSantriSnapshot,
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onSaveSantri: suspend (WaliSantriProfile) -> SantriSaveOutcome,
  modifier: Modifier = Modifier
) {
  val editedProfiles = remember { mutableStateMapOf<String, WaliSantriProfile>() }
  val students = waliSantriSnapshot.students
    .map { student -> editedProfiles[student.id] ?: student }
    .sortedWith(compareBy<WaliSantriProfile> { it.className }.thenBy { it.name })
  var selectedSantriId by rememberSaveable { mutableStateOf<String?>(null) }
  var query by rememberSaveable { mutableStateOf("") }
  var isSaving by rememberSaveable { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val selectedSantri = students.firstOrNull { it.id == selectedSantriId }
  val showListSkeleton = students.isEmpty() && (isRefreshing || waliSantriSnapshot.updatedAt <= 0L)

  BackHandler(enabled = selectedSantri != null) {
    selectedSantriId = null
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      AnimatedContent(
        targetState = selectedSantri,
        transitionSpec = {
          val openingDetail = targetState != null
          fadeIn() + slideInHorizontally { width -> if (openingDetail) width / 5 else -width / 5 } togetherWith
            fadeOut() + slideOutHorizontally { width -> if (openingDetail) -width / 6 else width / 6 }
        },
        label = "santri-content",
        modifier = Modifier.fillMaxSize()
      ) { activeSantri ->
        if (activeSantri == null) {
          PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
          ) {
            SantriListContent(
              isWaliKelas = waliSantriSnapshot.isWaliKelas,
              students = students,
              showSkeleton = showListSkeleton,
              query = query,
              onQueryChange = { query = it },
              onMenuClick = onMenuClick,
              onSantriClick = { selectedSantriId = it.id }
            )
          }
        } else {
          SantriDetailContent(
            santri = activeSantri,
            isSaving = isSaving,
            onBackClick = { selectedSantriId = null },
            onSaveClick = { draft ->
              scope.launch {
                isSaving = true
                val result = onSaveSantri(draft)
                isSaving = false
                if (result.success) {
                  editedProfiles.remove(activeSantri.id)
                }
                snackbarHostState.showSnackbar(result.message)
              }
            }
          )
        }
      }

      SavingOverlay(visible = isSaving)
    }
  }
}

@Composable
private fun SantriListContent(
  isWaliKelas: Boolean,
  students: List<WaliSantriProfile>,
  showSkeleton: Boolean,
  query: String,
  onQueryChange: (String) -> Unit,
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
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    SantriTopBar(
      title = "Santri",
      leadingIcon = Icons.Outlined.Menu,
      leadingContentDescription = "Buka sidebar",
      onLeadingClick = onMenuClick
    )

    SantriSearchBar(
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
            SantriSkeletonCard(index = index)
          }
        }

        filteredStudents.isEmpty() -> {
        item {
          EmptyPlaceholderCard(
            message = when {
              !isWaliKelas -> "Menu santri hanya tersedia untuk wali kelas. Tarik ke bawah untuk refresh data wali kelas."
              students.isEmpty() -> "Belum ada santri pada kelas wali ini."
              else -> "Tidak ada santri yang cocok dengan pencarian."
            }
          )
        }
      }

        else -> {
        items(filteredStudents, key = { it.id }) { student ->
          SantriCard(
            santri = student,
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
private fun SantriDetailContent(
  santri: WaliSantriProfile,
  isSaving: Boolean,
  onBackClick: () -> Unit,
  onSaveClick: (WaliSantriProfile) -> Unit
) {
  var name by rememberSaveable(santri.id, santri.name) { mutableStateOf(santri.name) }
  var nisn by rememberSaveable(santri.id, santri.nisn) { mutableStateOf(santri.nisn) }
  var gender by rememberSaveable(santri.id, santri.gender) { mutableStateOf(santri.gender) }
  var studentPhone by rememberSaveable(santri.id, santri.studentPhone) { mutableStateOf(santri.studentPhone) }
  var fatherName by rememberSaveable(santri.id, santri.fatherName) { mutableStateOf(santri.fatherName) }
  var fatherPhone by rememberSaveable(santri.id, santri.fatherPhone) { mutableStateOf(santri.fatherPhone) }
  var motherName by rememberSaveable(santri.id, santri.motherName) { mutableStateOf(santri.motherName) }
  var motherPhone by rememberSaveable(santri.id, santri.motherPhone) { mutableStateOf(santri.motherPhone) }
  var guardianName by rememberSaveable(santri.id, santri.guardianName) { mutableStateOf(santri.guardianName) }
  var guardianPhone by rememberSaveable(santri.id, santri.guardianPhone) { mutableStateOf(santri.guardianPhone) }
  var address by rememberSaveable(santri.id, santri.address) { mutableStateOf(santri.address) }
  var note by rememberSaveable(santri.id, santri.note) { mutableStateOf(santri.note) }

  val comparableSantri = santri.copy(gender = normalizeGenderCode(santri.gender))
  val draft = santri.copy(
    name = name,
    nisn = nisn,
    gender = normalizeGenderCode(gender),
    studentPhone = studentPhone,
    fatherName = fatherName,
    fatherPhone = fatherPhone,
    motherName = motherName,
    motherPhone = motherPhone,
    guardianName = guardianName,
    guardianPhone = guardianPhone,
    address = address,
    note = note
  )
  val isDirty = draft != comparableSantri

  Column(
    modifier = Modifier
      .fillMaxSize()
      .navigationBarsPadding()
      .padding(horizontal = 18.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    SantriTopBar(
      title = "Detail Santri",
      leadingIcon = if (isDirty) Icons.Outlined.Close else Icons.AutoMirrored.Outlined.ArrowBack,
      leadingContentDescription = if (isDirty) "Batalkan perubahan" else "Kembali ke daftar santri",
      onLeadingClick = {
        if (isDirty) {
          name = santri.name
          nisn = santri.nisn
          gender = santri.gender
          studentPhone = santri.studentPhone
          fatherName = santri.fatherName
          fatherPhone = santri.fatherPhone
          motherName = santri.motherName
          motherPhone = santri.motherPhone
          guardianName = santri.guardianName
          guardianPhone = santri.guardianPhone
          address = santri.address
          note = santri.note
        } else {
          onBackClick()
        }
      },
      trailingIcon = if (isDirty) Icons.Outlined.Check else null,
      trailingContentDescription = if (isSaving) "Sedang menyimpan" else "Simpan detail santri",
      onTrailingClick = {
        if (!isSaving) onSaveClick(draft)
      }
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(bottom = 124.dp)
    ) {
      item {
        SantriProfileHeader(
          name = name,
          classLabel = santri.className.ifBlank { "Kelas belum tersedia" },
          nisn = nisn
        )
      }

      item {
        SantriFormPanel(
          title = "Biodata Santri",
          subtitle = "Data dasar santri wali kelas.",
          content = {
            SantriInputField(
              label = "Nama Santri",
              value = name,
              onValueChange = { name = it },
              leadingIcon = Icons.Outlined.Person
            )
            SantriInputField(
              label = "NISN",
              value = nisn,
              onValueChange = { nisn = it },
              leadingIcon = Icons.Outlined.School
            )
            SantriGenderDropdownField(
              label = "Jenis Kelamin",
              value = gender,
              onValueChange = { gender = it }
            )
            SantriInputField(
              label = "Kontak Santri",
              value = studentPhone,
              onValueChange = { studentPhone = it.filter(Char::isDigit) },
              leadingIcon = Icons.Outlined.Phone,
              keyboardType = KeyboardType.Phone
            )
          }
        )
      }

      item {
        SantriFormPanel(
          title = "Data Orang Tua & Wali",
          subtitle = "Nomor kontak dipisahkan agar laporan bisa dikirim ke penerima yang tepat.",
          content = {
            ContactPersonFields(
              title = "Ayah",
              name = fatherName,
              phone = fatherPhone,
              onNameChange = { fatherName = it },
              onPhoneChange = { fatherPhone = it.filter(Char::isDigit) }
            )
            ContactPersonFields(
              title = "Ibu",
              name = motherName,
              phone = motherPhone,
              onNameChange = { motherName = it },
              onPhoneChange = { motherPhone = it.filter(Char::isDigit) }
            )
            ContactPersonFields(
              title = "Wali",
              name = guardianName,
              phone = guardianPhone,
              onNameChange = { guardianName = it },
              onPhoneChange = { guardianPhone = it.filter(Char::isDigit) }
            )
          }
        )
      }

      item {
        SantriFormPanel(
          title = "Alamat & Catatan",
          subtitle = "Catatan tambahan untuk wali kelas.",
          content = {
            SantriInputField(
              label = "Alamat",
              value = address,
              onValueChange = { address = it },
              leadingIcon = Icons.Outlined.Home,
              singleLine = false,
              minLines = 2
            )
            SantriInputField(
              label = "Catatan",
              value = note,
              onValueChange = { note = it },
              leadingIcon = Icons.AutoMirrored.Outlined.StickyNote2,
              singleLine = false,
              minLines = 3
            )
          }
        )
      }

      item {
        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun SantriTopBar(
  title: String,
  leadingIcon: ImageVector,
  leadingContentDescription: String,
  onLeadingClick: () -> Unit,
  trailingIcon: ImageVector? = null,
  trailingContentDescription: String = "",
  onTrailingClick: () -> Unit = {}
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    SantriTopButton(
      icon = leadingIcon,
      contentDescription = leadingContentDescription,
      onClick = onLeadingClick
    )
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 12.dp),
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    if (trailingIcon != null) {
      SantriTopButton(
        icon = trailingIcon,
        contentDescription = trailingContentDescription,
        onClick = onTrailingClick
      )
    } else {
      Spacer(modifier = Modifier.size(42.dp))
    }
  }
}

@Composable
private fun SantriTopButton(
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
      tint = PrimaryBlueDark
    )
  }
}

@Composable
private fun SantriSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    placeholder = { Text("Cari nama atau NISN") },
    singleLine = true,
    leadingIcon = {
      Icon(
        imageVector = Icons.Outlined.Search,
        contentDescription = "Cari",
        tint = SubtleInk
      )
    },
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    colors = santriTextFieldColors()
  )
}

@Composable
private fun SantriCard(
  santri: WaliSantriProfile,
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
    SantriAvatar(name = santri.name, size = 50)
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
        text = if (santri.nisn.isBlank()) "NISN belum tersedia" else "NISN ${santri.nisn}",
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun SantriProfileHeader(
  name: String,
  classLabel: String,
  nisn: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Box(contentAlignment = Alignment.BottomEnd) {
      SantriAvatar(name = name, size = 94)
      Box(
        modifier = Modifier
          .size(30.dp)
          .background(PrimaryBlue, CircleShape)
          .border(2.dp, CardBackground, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.Edit,
          contentDescription = "Edit foto santri",
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = name.ifBlank { "Nama Santri" },
        style = MaterialTheme.typography.titleLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center
      )
      Text(
        text = listOf(classLabel, nisn.ifBlank { null }).filterNotNull().joinToString(" - "),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun SantriFormPanel(
  title: String,
  subtitle: String,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
      .clip(RoundedCornerShape(28.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(28.dp))
      .padding(18.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
    content()
  }
}

@Composable
private fun ContactPersonFields(
  title: String,
  name: String,
  phone: String,
  onNameChange: (String) -> Unit,
  onPhoneChange: (String) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(SoftPanel.copy(alpha = 0.72f))
      .border(1.dp, CardBorder.copy(alpha = 0.78f), RoundedCornerShape(20.dp))
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    SantriInputField(
      label = "Nama $title",
      value = name,
      onValueChange = onNameChange,
      leadingIcon = Icons.Outlined.Groups
    )
    SantriInputField(
      label = "Kontak $title",
      value = phone,
      onValueChange = onPhoneChange,
      leadingIcon = Icons.Outlined.Phone,
      keyboardType = KeyboardType.Phone
    )
  }
}

@Composable
private fun SantriGenderDropdownField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedLabel = genderDisplayLabel(value)

  Box(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(SoftPanel)
        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        .clickable { expanded = true }
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier.width(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.Person,
          contentDescription = label,
          tint = SubtleInk,
          modifier = Modifier.size(20.dp)
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = SubtleInk,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = selectedLabel,
          style = MaterialTheme.typography.bodyMedium,
          color = if (normalizeGenderCode(value).isBlank()) SubtleInk else PrimaryBlueDark,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Icon(
        imageVector = Icons.Outlined.KeyboardArrowDown,
        contentDescription = "Buka pilihan jenis kelamin",
        tint = SubtleInk,
        modifier = Modifier.size(20.dp)
      )
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      SantriGenderOptions.forEach { option ->
        DropdownMenuItem(
          text = { Text(option.label) },
          onClick = {
            onValueChange(option.code)
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
private fun SantriAvatar(
  name: String,
  size: Int
) {
  val initials = remember(name) {
    name
      .split(" ")
      .filter { it.isNotBlank() }
      .take(2)
      .joinToString("") { it.first().uppercase() }
      .ifBlank { "S" }
  }
  Box(
    modifier = Modifier
      .size(size.dp)
      .shadow(10.dp, CircleShape, ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
      .clip(CircleShape)
      .background(PrimaryBlue.copy(alpha = 0.12f))
      .border(1.dp, PrimaryBlue.copy(alpha = 0.24f), CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = initials,
      style = if (size > 60) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
      color = PrimaryBlue,
      fontWeight = FontWeight.ExtraBold
    )
  }
}

private fun normalizeGenderCode(value: String): String {
  return when (value.trim().lowercase()) {
    "l", "lk", "laki", "laki-laki", "laki laki", "male" -> "L"
    "p", "pr", "perempuan", "female" -> "P"
    else -> value.trim().uppercase().take(1)
  }
}

private fun genderDisplayLabel(value: String): String {
  return when (normalizeGenderCode(value)) {
    "L" -> "Laki-laki"
    "P" -> "Perempuan"
    else -> "Pilih jenis kelamin"
  }
}

@Composable
private fun SantriInputField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  leadingIcon: ImageVector,
  keyboardType: KeyboardType = KeyboardType.Text,
  singleLine: Boolean = true,
  minLines: Int = 1
) {
  if (!singleLine) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(SoftPanel)
          .border(1.dp, CardBorder.copy(alpha = 0.72f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = label,
          tint = SubtleInk,
          modifier = Modifier.size(20.dp)
        )
      }
      OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.weight(1f),
        singleLine = false,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = santriTextFieldColors()
      )
    }
    return
  }

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    modifier = Modifier.fillMaxWidth(),
    singleLine = singleLine,
    minLines = minLines,
    leadingIcon = {
      Box(
        modifier = Modifier.width(44.dp),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = label,
          tint = SubtleInk,
          modifier = Modifier.size(20.dp)
        )
      }
    },
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    shape = RoundedCornerShape(16.dp),
    colors = santriTextFieldColors()
  )
}

@Composable
private fun santriTextFieldColors() = TextFieldDefaults.colors(
  focusedContainerColor = SoftPanel,
  unfocusedContainerColor = SoftPanel,
  disabledContainerColor = SoftPanel,
  focusedIndicatorColor = PrimaryBlue.copy(alpha = 0.48f),
  unfocusedIndicatorColor = CardBorder,
  focusedLabelColor = PrimaryBlueDark,
  unfocusedLabelColor = SubtleInk,
  cursorColor = PrimaryBlueDark
)

@Composable
private fun SantriSkeletonCard(index: Int) {
  SkeletonContentCard(
    leadingSize = 50.dp,
    trailingSize = 58.dp,
    firstLineWidth = if (index % 2 == 0) 0.58f else 0.72f,
    secondLineWidth = if (index % 3 == 0) 0.34f else 0.46f
  )
}
