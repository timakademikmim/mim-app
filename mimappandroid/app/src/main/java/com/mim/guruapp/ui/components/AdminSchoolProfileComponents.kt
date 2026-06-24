package com.mim.guruapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.remote.AdminSchoolEmployeeOption
import com.mim.guruapp.data.remote.AdminSchoolProfile
import com.mim.guruapp.data.remote.AdminSchoolProfileLoadResult
import com.mim.guruapp.data.remote.AdminSchoolProfileSaveResult
import com.mim.guruapp.data.remote.AdminSchoolProfileSnapshot
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
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AdminSchoolProfileScreen(
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadSnapshot: suspend () -> AdminSchoolProfileLoadResult,
  onSaveProfile: suspend (AdminSchoolProfile) -> AdminSchoolProfileSaveResult,
  modifier: Modifier = Modifier
) {
  var snapshot by remember { mutableStateOf<AdminSchoolProfileSnapshot?>(null) }
  var schoolName by rememberSaveable { mutableStateOf("") }
  var schoolAddress by rememberSaveable { mutableStateOf("") }
  var principalName by rememberSaveable { mutableStateOf("") }
  var wakasekAkademikName by rememberSaveable { mutableStateOf("") }
  var wakasekKetahfizanName by rememberSaveable { mutableStateOf("") }
  var wakasekKesantrianName by rememberSaveable { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  fun applySnapshot(nextSnapshot: AdminSchoolProfileSnapshot) {
    snapshot = nextSnapshot
    val profile = nextSnapshot.profile
    schoolName = profile.schoolName
    schoolAddress = profile.schoolAddress
    principalName = profile.principalName
    wakasekAkademikName = profile.wakasekAkademikName
    wakasekKetahfizanName = profile.wakasekKetahfizanName
    wakasekKesantrianName = profile.wakasekKesantrianName
  }

  fun loadSnapshot(showGlobalRefresh: Boolean = false) {
    if (isLoading) return
    if (showGlobalRefresh) onRefresh()
    scope.launch {
      isLoading = true
      errorMessage = ""
      when (val result = onLoadSnapshot()) {
        is AdminSchoolProfileLoadResult.Success -> applySnapshot(result.snapshot)
        is AdminSchoolProfileLoadResult.Error -> errorMessage = result.message
      }
      isLoading = false
    }
  }

  LaunchedEffect(Unit) {
    loadSnapshot()
  }

  val originalProfile = snapshot?.profile ?: AdminSchoolProfile()
  val draft = originalProfile.copy(
    schoolName = schoolName,
    schoolAddress = schoolAddress,
    principalName = principalName,
    wakasekAkademikName = wakasekAkademikName,
    wakasekKetahfizanName = wakasekKetahfizanName,
    wakasekKesantrianName = wakasekKesantrianName
  )
  val isDirty = draft != originalProfile

  BackHandler(enabled = isDirty && !isSaving) {
    applySnapshot(snapshot ?: return@BackHandler)
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
      AdminSchoolProfileTopBar(
        title = "Profil Sekolah",
        isSaving = isSaving,
        saveEnabled = isDirty && !isSaving,
        onMenuClick = onMenuClick,
        onSaveClick = {
          if (isSaving || !isDirty) return@AdminSchoolProfileTopBar
          scope.launch {
            isSaving = true
            when (val result = onSaveProfile(draft)) {
              is AdminSchoolProfileSaveResult.Success -> {
                applySnapshot(result.snapshot)
                snackbarHostState.showSnackbar(result.message)
              }
              is AdminSchoolProfileSaveResult.Error -> snackbarHostState.showSnackbar(result.message)
            }
            isSaving = false
          }
        }
      )
      PullToRefreshBox(
        isRefreshing = isRefreshing || isLoading,
        onRefresh = { loadSnapshot(showGlobalRefresh = true) },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .navigationBarsPadding()
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          when {
            isLoading && snapshot == null -> {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(220.dp),
                contentAlignment = Alignment.Center
              ) {
                CircularProgressIndicator()
              }
            }
            errorMessage.isNotBlank() -> AdminSchoolNoticeCard(errorMessage)
          }

          AdminSchoolHeaderCard(
            schoolName = schoolName,
            schoolAddress = schoolAddress,
            principalName = principalName,
            statusMessage = snapshot?.statusMessage.orEmpty()
          )

          PlaceholderPanel(title = "Profil Sekolah") {
            AdminSchoolInputField(
              value = schoolName,
              onValueChange = { schoolName = it },
              label = "Nama Sekolah",
              placeholder = "Nama sekolah"
            )
            AdminSchoolInputField(
              value = schoolAddress,
              onValueChange = { schoolAddress = it },
              label = "Alamat Sekolah",
              placeholder = "Alamat sekolah",
              singleLine = false,
              modifier = Modifier.padding(top = 10.dp)
            )
          }

          PlaceholderPanel(title = "Struktur Pimpinan") {
            val employees = snapshot?.employeeOptions.orEmpty()
            AdminSchoolEmployeeDropdown(
              label = "Kepala Sekolah",
              value = principalName,
              placeholder = "-- Pilih Kepala Sekolah --",
              employees = employees,
              onValueChange = { principalName = it }
            )
            AdminSchoolEmployeeDropdown(
              label = "Wakasek Bidang Akademik",
              value = wakasekAkademikName,
              placeholder = "-- Pilih Wakasek Akademik --",
              employees = employees,
              onValueChange = { wakasekAkademikName = it },
              modifier = Modifier.padding(top = 10.dp)
            )
            AdminSchoolEmployeeDropdown(
              label = "Wakasek Bidang Ketahfizan",
              value = wakasekKetahfizanName,
              placeholder = "-- Pilih Wakasek Ketahfizan --",
              employees = employees,
              onValueChange = { wakasekKetahfizanName = it },
              modifier = Modifier.padding(top = 10.dp)
            )
            AdminSchoolEmployeeDropdown(
              label = "Wakasek Bidang Kesantrian",
              value = wakasekKesantrianName,
              placeholder = "-- Pilih Wakasek Kesantrian --",
              employees = employees,
              onValueChange = { wakasekKesantrianName = it },
              modifier = Modifier.padding(top = 10.dp)
            )
          }

          AdminSchoolAutoSection(
            title = "Wali Kelas",
            subtitle = "Semua kelas",
            items = snapshot?.waliKelasItems.orEmpty(),
            emptyText = "Belum ada data kelas."
          )
          AdminSchoolAutoSection(
            title = "Musyrif",
            subtitle = "Kamar",
            items = snapshot?.musyrifItems.orEmpty(),
            emptyText = "Belum ada data musyrif/kamar."
          )
          AdminSchoolAutoSection(
            title = "Muhaffiz",
            subtitle = "Halaqah",
            items = snapshot?.muhaffizItems.orEmpty(),
            emptyText = "Belum ada data muhaffiz/halaqah."
          )
          Spacer(modifier = Modifier.height(112.dp))
        }
      }
    }
  }
}

@Composable
private fun AdminSchoolProfileTopBar(
  title: String,
  isSaving: Boolean,
  saveEnabled: Boolean,
  onMenuClick: () -> Unit,
  onSaveClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    AdminSchoolTopButton(
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
    AdminSchoolTopButton(
      icon = Icons.Outlined.Check,
      contentDescription = if (isSaving) "Sedang menyimpan" else "Simpan profil sekolah",
      onClick = onSaveClick,
      enabled = saveEnabled
    )
  }
}

@Composable
private fun AdminSchoolTopButton(
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
      contentDescription = t(contentDescription),
      tint = if (enabled) PrimaryBlueDark else SubtleInk.copy(alpha = 0.45f)
    )
  }
}

@Composable
private fun AdminSchoolHeaderCard(
  schoolName: String,
  schoolAddress: String,
  principalName: String,
  statusMessage: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x160F172A), spotColor = Color(0x160F172A))
      .background(Brush.verticalGradient(listOf(CardBackground, CardGradientEnd)), RoundedCornerShape(24.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
      .padding(18.dp)
  ) {
    Text(
      text = schoolName.ifBlank { "Profil Sekolah" },
      style = MaterialTheme.typography.headlineSmall,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = schoolAddress.ifBlank { "Alamat sekolah belum diisi" },
      style = MaterialTheme.typography.bodyMedium,
      color = SubtleInk,
      modifier = Modifier.padding(top = 6.dp)
    )
    if (principalName.isNotBlank()) {
      StatusPill("Kepala: $principalName", PrimaryBlue)
    }
    if (statusMessage.isNotBlank()) {
      Text(
        text = t(statusMessage),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        modifier = Modifier.padding(top = 10.dp)
      )
    }
  }
}

@Composable
private fun AdminSchoolInputField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String,
  modifier: Modifier = Modifier,
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
    keyboardOptions = KeyboardOptions(
      capitalization = KeyboardCapitalization.Words,
      keyboardType = KeyboardType.Text
    )
  )
}

@Composable
private fun AdminSchoolEmployeeDropdown(
  label: String,
  value: String,
  placeholder: String,
  employees: List<AdminSchoolEmployeeOption>,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = modifier.fillMaxWidth()) {
    OutlinedTextField(
      value = value,
      onValueChange = {},
      modifier = Modifier
        .fillMaxWidth()
        .clickable { expanded = true },
      readOnly = true,
      label = { Text(t(label)) },
      placeholder = { Text(t(placeholder)) },
      shape = RoundedCornerShape(18.dp),
      trailingIcon = {
        Icon(
          imageVector = Icons.Outlined.ExpandMore,
          contentDescription = null,
          tint = PrimaryBlueDark
        )
      }
    )
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.background(CardBackground)
    ) {
      DropdownMenuItem(
        text = { Text(t("Kosongkan")) },
        onClick = {
          onValueChange("")
          expanded = false
        }
      )
      employees.forEach { employee ->
        DropdownMenuItem(
          text = {
            Column {
              Text(employee.name, fontWeight = FontWeight.SemiBold)
              if (employee.role.isNotBlank()) {
                Text(
                  text = employee.role.displayAdminRoles(),
                  style = MaterialTheme.typography.bodySmall,
                  color = SubtleInk
                )
              }
            }
          },
          onClick = {
            onValueChange(employee.name)
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
private fun AdminSchoolAutoSection(
  title: String,
  subtitle: String,
  items: List<String>,
  emptyText: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x120F172A), spotColor = Color(0x120F172A))
      .clip(RoundedCornerShape(18.dp))
      .background(Brush.linearGradient(listOf(CardGradientStart, CardGradientEnd)))
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(18.dp))
      .padding(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = t(title),
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = t(subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      }
      StatusPill("${items.size}", PrimaryBlue)
    }
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 12.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(SoftPanel)
        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      if (items.isEmpty()) {
        Text(
          text = t(emptyText),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk
        )
      } else {
        items.forEach { item ->
          Text(
            text = "- $item",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

@Composable
private fun AdminSchoolNoticeCard(message: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color(0xFFFB7185).copy(alpha = 0.12f))
      .border(1.dp, Color(0xFFFB7185).copy(alpha = 0.28f), RoundedCornerShape(16.dp))
      .padding(14.dp)
  ) {
    Text(
      text = t(message),
      style = MaterialTheme.typography.bodySmall,
      color = Color(0xFFBE123C),
      fontWeight = FontWeight.SemiBold
    )
  }
}

private fun String.displayAdminRoles(): String {
  val roles = split(',', '|', ';')
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .distinct()
  return roles.ifEmpty { listOf("-") }.joinToString(", ")
}
