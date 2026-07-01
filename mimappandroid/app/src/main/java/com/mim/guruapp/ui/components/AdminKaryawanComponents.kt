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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.data.remote.AdminEmployee
import com.mim.guruapp.data.remote.AdminEmployeeListResult
import com.mim.guruapp.data.remote.AdminEmployeeSaveResult
import com.mim.guruapp.ui.i18n.t
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.CardGradientEnd
import com.mim.guruapp.ui.theme.CardGradientStart
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SubtleInk
import com.mim.guruapp.ui.theme.SuccessTint
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AdminKaryawanScreen(
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  onLoadEmployees: suspend () -> AdminEmployeeListResult,
  onSaveEmployee: suspend (AdminEmployee, String) -> AdminEmployeeSaveResult,
  modifier: Modifier = Modifier
) {
  var employees by remember { mutableStateOf<List<AdminEmployee>>(emptyList()) }
  var selectedEmployee by remember { mutableStateOf<AdminEmployee?>(null) }
  var query by rememberSaveable { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  fun loadEmployees(showGlobalRefresh: Boolean = false) {
    if (isLoading) return
    if (showGlobalRefresh) onRefresh()
    scope.launch {
      isLoading = true
      errorMessage = ""
      when (val result = onLoadEmployees()) {
        is AdminEmployeeListResult.Success -> {
          employees = result.employees
          selectedEmployee = selectedEmployee?.let { current ->
            result.employees.firstOrNull { it.rowId == current.rowId } ?: current
          }
        }
        is AdminEmployeeListResult.Error -> errorMessage = result.message
      }
      isLoading = false
    }
  }

  LaunchedEffect(Unit) {
    loadEmployees()
  }

  selectedEmployee?.let { employee ->
    AdminKaryawanDetailContent(
      employee = employee,
      isSaving = isSaving,
      onBackClick = { selectedEmployee = null },
      onSave = { draft, newPassword ->
        if (isSaving) return@AdminKaryawanDetailContent
        scope.launch {
          isSaving = true
          when (val result = onSaveEmployee(draft, newPassword)) {
            is AdminEmployeeSaveResult.Success -> {
              val saved = result.employee
              employees = employees.map { if (it.rowId == saved.rowId) saved else it }
              selectedEmployee = saved
              snackbarHostState.showSnackbar("Data karyawan berhasil disimpan.")
            }
            is AdminEmployeeSaveResult.Error -> {
              snackbarHostState.showSnackbar(result.message)
            }
          }
          isSaving = false
        }
      },
      snackbarHostState = snackbarHostState,
      modifier = modifier
    )
    return
  }

  val filteredEmployees = remember(employees, query) {
    val normalizedQuery = query.trim().lowercase()
    employees
      .filter { employee ->
        if (normalizedQuery.isBlank()) {
          true
        } else {
          buildString {
            append(employee.employeeId)
            append(' ')
            append(employee.name)
            append(' ')
            append(employee.role)
            append(' ')
            append(employee.phoneNumber)
          }.lowercase().contains(normalizedQuery)
        }
      }
      .sortedWith(compareByDescending<AdminEmployee> { it.active }.thenBy { it.name.lowercase() })
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
      AdminKaryawanTopBar(
        title = "Karyawan",
        subtitle = "Kelola akses login dan biodata karyawan",
        isDetail = false,
        isSaving = false,
        onMenuClick = onMenuClick,
        onBackClick = {},
        onSaveClick = {}
      )
      AdminKaryawanSearchBar(
        value = query,
        onValueChange = { query = it }
      )
      PullToRefreshBox(
        isRefreshing = isRefreshing || isLoading,
        onRefresh = { loadEmployees(showGlobalRefresh = true) },
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
          if (errorMessage.isNotBlank()) {
            item {
              AdminKaryawanNoticeCard(errorMessage)
            }
          }
          if (isLoading && employees.isEmpty()) {
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
          } else if (filteredEmployees.isEmpty()) {
            item {
              EmptyPlaceholderCard(
                if (query.isBlank()) "Belum ada data karyawan." else "Tidak ada karyawan yang cocok dengan pencarian."
              )
            }
          } else {
            items(filteredEmployees, key = { it.rowId.ifBlank { it.employeeId } }) { employee ->
              AdminKaryawanCard(
                employee = employee,
                onClick = { selectedEmployee = employee }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminKaryawanDetailContent(
  employee: AdminEmployee,
  isSaving: Boolean,
  onBackClick: () -> Unit,
  onSave: (AdminEmployee, String) -> Unit,
  snackbarHostState: SnackbarHostState,
  modifier: Modifier = Modifier
) {
  var employeeId by rememberSaveable(employee.rowId, employee.employeeId) { mutableStateOf(employee.employeeId) }
  var name by rememberSaveable(employee.rowId, employee.name) { mutableStateOf(employee.name) }
  var newPassword by rememberSaveable(employee.rowId) { mutableStateOf("") }
  var isChangingPassword by rememberSaveable(employee.rowId) { mutableStateOf(false) }
  var role by rememberSaveable(employee.rowId, employee.role) { mutableStateOf(employee.role) }
  var phoneNumber by rememberSaveable(employee.rowId, employee.phoneNumber) { mutableStateOf(employee.phoneNumber) }
  var address by rememberSaveable(employee.rowId, employee.address) { mutableStateOf(employee.address) }
  var active by rememberSaveable(employee.rowId, employee.active) { mutableStateOf(employee.active) }
  var showPassword by rememberSaveable { mutableStateOf(false) }
  val draft = employee.copy(
    employeeId = employeeId,
    name = name,
    role = role,
    phoneNumber = phoneNumber,
    address = address,
    active = active
  )
  val passwordToSave = if (isChangingPassword) newPassword else ""
  val isDirty = draft != employee || passwordToSave.isNotBlank()

  BackHandler(enabled = true) {
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
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      AdminKaryawanTopBar(
        title = "Detail Karyawan",
        subtitle = name.ifBlank { employee.employeeId.ifBlank { "Edit data karyawan" } },
        isDetail = true,
        isSaving = isSaving,
        onMenuClick = {},
        onBackClick = onBackClick,
        onSaveClick = { onSave(draft, passwordToSave) },
        saveEnabled = isDirty
      )
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .navigationBarsPadding()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        AdminKaryawanProfileHeader(
          name = name,
          employeeId = employeeId,
          role = role,
          active = active
        )

        PlaceholderPanel(title = "Informasi Karyawan") {
          AdminKaryawanInputField(
            value = employeeId,
            onValueChange = { employeeId = it },
            label = "ID Karyawan",
            placeholder = "Masukkan ID karyawan"
          )
          AdminKaryawanInputField(
            value = name,
            onValueChange = { name = it },
            label = "Nama",
            placeholder = "Masukkan nama karyawan",
            modifier = Modifier.padding(top = 10.dp)
          )
          AdminKaryawanInputField(
            value = if (employee.authLinked) "Terhubung ke Supabase Auth" else "Belum dimigrasikan",
            onValueChange = {},
            label = "Status Login",
            placeholder = "Belum terhubung",
            readOnly = true,
            modifier = Modifier.padding(top = 10.dp)
          )
          if (employee.passwordResetRequestedAt.isNotBlank()) {
            AdminKaryawanInputField(
              value = "Pengguna meminta reset password",
              onValueChange = {},
              label = "Permintaan Reset",
              placeholder = "",
              readOnly = true,
              modifier = Modifier.padding(top = 10.dp)
            )
          }
          OutlinedButton(
            onClick = {
              isChangingPassword = !isChangingPassword
              if (!isChangingPassword) newPassword = ""
            },
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 10.dp)
          ) {
            Text(t(if (isChangingPassword) "Batal ganti password" else "Ganti password"))
          }
          if (isChangingPassword) {
            AdminKaryawanInputField(
              value = newPassword,
              onValueChange = { newPassword = it },
              label = "Password baru",
              placeholder = "Minimal 12 karakter, huruf besar/kecil, angka, simbol",
              isPassword = true,
              passwordVisible = showPassword,
              onPasswordVisibilityToggle = { showPassword = !showPassword },
              modifier = Modifier.padding(top = 10.dp)
            )
          }
          AdminKaryawanInputField(
            value = role,
            onValueChange = { role = it },
            label = "Role",
            placeholder = "Contoh: admin,guru",
            modifier = Modifier.padding(top = 10.dp)
          )
          AdminKaryawanInputField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = "Nomor HP",
            placeholder = "Masukkan nomor HP",
            keyboardType = KeyboardType.Phone,
            modifier = Modifier.padding(top = 10.dp)
          )
          AdminKaryawanInputField(
            value = address,
            onValueChange = { address = it },
            label = "Alamat",
            placeholder = "Masukkan alamat",
            singleLine = false,
            modifier = Modifier.padding(top = 10.dp)
          )
        }

        AdminKaryawanStatusCard(
          active = active,
          onActiveChange = { active = it }
        )
        Spacer(modifier = Modifier.height(112.dp))
      }
    }
  }
}

@Composable
private fun AdminKaryawanTopBar(
  title: String,
  subtitle: String,
  isDetail: Boolean,
  isSaving: Boolean,
  onMenuClick: () -> Unit,
  onBackClick: () -> Unit,
  onSaveClick: () -> Unit,
  modifier: Modifier = Modifier,
  saveEnabled: Boolean = false
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    AdminKaryawanTopButton(
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
    if (isDetail) {
      AdminKaryawanTopButton(
        icon = Icons.Outlined.Check,
        contentDescription = if (isSaving) "Sedang menyimpan" else "Simpan perubahan",
        onClick = onSaveClick,
        enabled = saveEnabled && !isSaving
      )
    } else {
      Spacer(modifier = Modifier.size(42.dp))
    }
  }
}

@Composable
private fun AdminKaryawanTopButton(
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
private fun AdminKaryawanSearchBar(
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
    label = { Text(t("Cari karyawan")) },
    placeholder = { Text(t("Nama, ID, role, atau nomor HP")) }
  )
}

@Composable
private fun AdminKaryawanCard(
  employee: AdminEmployee,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x120F172A), spotColor = Color(0x120F172A))
      .clip(RoundedCornerShape(18.dp))
      .background(Brush.linearGradient(listOf(CardGradientStart, CardGradientEnd)))
      .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(18.dp))
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
        text = employee.initials(),
        style = MaterialTheme.typography.titleSmall,
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
          text = employee.name.ifBlank { "-" },
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        StatusPill(if (employee.active) "Aktif" else "Nonaktif", if (employee.active) SuccessTint else Color(0xFFFB7185))
      }
      Text(
        text = employee.employeeId.ifBlank { "-" },
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        modifier = Modifier.padding(top = 3.dp)
      )
      Text(
        text = employee.role.displayRoles(),
        style = MaterialTheme.typography.bodySmall,
        color = PrimaryBlue,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 6.dp)
      )
    }
  }
}

@Composable
private fun AdminKaryawanProfileHeader(
  name: String,
  employeeId: String,
  role: String,
  active: Boolean
) {
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
          text = AdminEmployee("", employeeId, name, "", role, "", "", active).initials(),
          style = MaterialTheme.typography.titleLarge,
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
          text = name.ifBlank { "Nama karyawan" },
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = employeeId.ifBlank { "ID Karyawan" },
          style = MaterialTheme.typography.bodyMedium,
          color = SubtleInk,
          modifier = Modifier.padding(top = 3.dp)
        )
      }
    }
    Row(
      modifier = Modifier.padding(top = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      StatusPill(role.displayRoles(), PrimaryBlue)
      StatusPill(if (active) "Aktif" else "Nonaktif", if (active) SuccessTint else Color(0xFFFB7185))
    }
  }
}

@Composable
private fun AdminKaryawanInputField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String,
  modifier: Modifier = Modifier,
  isPassword: Boolean = false,
  passwordVisible: Boolean = false,
  onPasswordVisibilityToggle: (() -> Unit)? = null,
  keyboardType: KeyboardType = KeyboardType.Text,
  singleLine: Boolean = true,
  readOnly: Boolean = false
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier.fillMaxWidth(),
    readOnly = readOnly,
    label = { Text(t(label)) },
    placeholder = { Text(t(placeholder)) },
    shape = RoundedCornerShape(18.dp),
    singleLine = singleLine,
    minLines = if (singleLine) 1 else 3,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
    trailingIcon = if (isPassword && onPasswordVisibilityToggle != null) {
      {
        IconButton(onClick = onPasswordVisibilityToggle) {
          Icon(
            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = t(if (passwordVisible) "Sembunyikan password" else "Tampilkan password")
          )
        }
      }
    } else {
      null
    }
  )
}

@Composable
private fun AdminKaryawanStatusCard(
  active: Boolean,
  onActiveChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(CardBackground)
      .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(end = 12.dp)
    ) {
      Text(
        text = t("Status"),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold
      )
      StatusPill(if (active) "Aktif" else "Nonaktif", if (active) SuccessTint else Color(0xFFFB7185))
    }
    Switch(
      checked = active,
      onCheckedChange = onActiveChange
    )
  }
}

@Composable
private fun AdminKaryawanNoticeCard(message: String) {
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

private fun AdminEmployee.initials(): String {
  val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
  return when {
    words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
    words.size == 1 -> words[0].take(2).uppercase()
    employeeId.isNotBlank() -> employeeId.take(2).uppercase()
    else -> "KA"
  }
}

private fun String.displayRoles(): String {
  val roles = split(',', '|', ';')
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .distinct()
  return roles.ifEmpty { listOf("-") }.joinToString(", ")
}
