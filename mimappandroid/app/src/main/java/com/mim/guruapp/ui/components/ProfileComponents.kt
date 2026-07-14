package com.mim.guruapp.ui.components

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mim.guruapp.R
import com.mim.guruapp.data.model.GuruProfile
import com.mim.guruapp.export.UserGuidePdfBlock
import com.mim.guruapp.export.UserGuidePdfExporter
import com.mim.guruapp.export.UserGuidePdfSection
import com.mim.guruapp.ProfileSaveOutcome
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SoftPanel
import com.mim.guruapp.ui.theme.SubtleInk
import com.mim.guruapp.ui.i18n.t
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private enum class ProfileSection {
  Menu,
  Information,
  PasswordSecurity,
  GoogleAccount,
  Settings,
  Guide
}

private data class ProfileLanguageOption(
  val code: String,
  val title: String,
  val subtitle: String
)

private data class ProfileThemeOption(
  val code: String,
  val title: String,
  val subtitle: String
)

private data class UserGuideSection(
  val title: String,
  val summary: String,
  val steps: List<String>,
  val note: String = "",
  val detailBlocks: List<UserGuideDetailBlock> = emptyList(),
  val interactiveGuideKey: String = ""
)

private enum class UserGuideBlockType {
  Text,
  Step,
  Tip,
  Warning
}

private data class UserGuideDetailBlock(
  val type: UserGuideBlockType,
  val title: String,
  val body: String,
  val imageResName: String = "",
  val imageCaption: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
  profile: GuruProfile,
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
  languageCode: String,
  onApplyLanguage: (String) -> Unit,
  themeModeCode: String,
  onApplyThemeMode: (String) -> Unit,
  onSaveClick: suspend (GuruProfile) -> ProfileSaveOutcome,
  onChangePassword: suspend (String, String) -> ProfileSaveOutcome,
  onLinkGoogleAccount: () -> Unit,
  onStartInteractiveGuide: (String) -> Unit = {},
  openGuideRequest: Int = 0,
  openSettingsRequest: Int = 0,
  modifier: Modifier = Modifier
) {
  var name by rememberSaveable(profile) { mutableStateOf(profile.name) }
  var address by rememberSaveable(profile) { mutableStateOf(profile.address) }
  var username by rememberSaveable(profile) { mutableStateOf(profile.username) }
  var phoneNumber by rememberSaveable(profile) { mutableStateOf(profile.phoneNumber) }
  var isSaving by rememberSaveable { mutableStateOf(false) }
  var activeSectionName by rememberSaveable { mutableStateOf(ProfileSection.Menu.name) }
  var activeGuideTitle by rememberSaveable { mutableStateOf<String?>(null) }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val activeSection = runCatching { ProfileSection.valueOf(activeSectionName) }
    .getOrDefault(ProfileSection.Menu)
  val guideSections = remember { buildUserGuideSections() }
  val activeGuide = remember(activeGuideTitle, guideSections) {
    guideSections.firstOrNull { it.title == activeGuideTitle }
  }
  val resetDraftAndReturnToMenu = {
    name = profile.name
    address = profile.address
    username = profile.username
    phoneNumber = profile.phoneNumber
    activeGuideTitle = null
    activeSectionName = ProfileSection.Menu.name
  }

  val draft = GuruProfile(
    name = name,
    address = address,
    username = username,
    password = "",
    phoneNumber = phoneNumber,
    avatarUri = profile.avatarUri,
    googleLinked = profile.googleLinked,
    googleEmail = profile.googleEmail
  )
  val isDirty = draft != profile.copy(password = "")
  val isInformationDetail = activeSection == ProfileSection.Information
  val avatarPickerMessage = t("Pemilih foto profil akan ditambahkan.")

  LaunchedEffect(openGuideRequest) {
    if (openGuideRequest > 0) {
      activeSectionName = ProfileSection.Guide.name
      activeGuideTitle = null
    }
  }

  LaunchedEffect(openSettingsRequest) {
    if (openSettingsRequest > 0) {
      activeSectionName = ProfileSection.Settings.name
      activeGuideTitle = null
    }
  }

  BackHandler(enabled = activeSection != ProfileSection.Menu) {
    if (isInformationDetail && isDirty) {
      resetDraftAndReturnToMenu()
    } else if (activeSection == ProfileSection.Guide && activeGuideTitle != null) {
      activeGuideTitle = null
    } else {
      activeSectionName = ProfileSection.Menu.name
      activeGuideTitle = null
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(AppBackground),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState)
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
          .fillMaxSize()
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
          ProfileTopBar(
            title = when (activeSection) {
              ProfileSection.Menu -> "Profil"
              ProfileSection.Information -> "Informasi Umum"
              ProfileSection.PasswordSecurity -> "Atur Password"
              ProfileSection.GoogleAccount -> "Akun Google"
              ProfileSection.Settings -> "Pengaturan"
              ProfileSection.Guide -> activeGuide?.title ?: "Panduan Pengguna"
            }.let { t(it) },
            isDetail = activeSection != ProfileSection.Menu,
            isDirty = isInformationDetail && isDirty,
            isSaving = isSaving,
            onMenuClick = onMenuClick,
            onBackClick = {
              if (activeSection == ProfileSection.Guide && activeGuideTitle != null) {
                activeGuideTitle = null
              } else {
                activeSectionName = ProfileSection.Menu.name
                activeGuideTitle = null
              }
            },
            onCancelClick = resetDraftAndReturnToMenu,
            onSaveClick = {
              if (!isSaving) {
                scope.launch {
                  isSaving = true
                  val result = onSaveClick(draft)
                  isSaving = false
                  snackbarHostState.showSnackbar(result.message)
                }
              }
            }
          )

          when (activeSection) {
            ProfileSection.Menu -> {
              ProfileLandingHeader(
                profile = profile,
                onEditAvatarClick = {
                  scope.launch {
                    snackbarHostState.showSnackbar(avatarPickerMessage)
                  }
                }
              )
              ProfileSectionCard(
                title = t("Informasi Umum"),
                description = t("Edit nama, alamat, username, dan nomor HP."),
                icon = Icons.Outlined.Person,
                onClick = { activeSectionName = ProfileSection.Information.name }
              )
              ProfileSectionCard(
                title = t("Atur Password"),
                description = t("Verifikasi password saat ini lalu buat password baru."),
                icon = Icons.Outlined.Lock,
                onClick = { activeSectionName = ProfileSection.PasswordSecurity.name }
              )
              ProfileSectionCard(
                title = t("Akun Google"),
                description = if (profile.googleLinked) {
                  t("Google sudah tertaut. Login bisa memakai tombol Masuk dengan Google.")
                } else {
                  t("Tautkan akun Google untuk login lebih cepat.")
                },
                icon = Icons.Outlined.Sync,
                onClick = { activeSectionName = ProfileSection.GoogleAccount.name }
              )
              ProfileSectionCard(
                title = t("Pengaturan"),
                description = t("Atur sinkronisasi, keamanan, dan preferensi aplikasi."),
                icon = Icons.Outlined.Settings,
                onClick = { activeSectionName = ProfileSection.Settings.name }
              )
              ProfileSectionCard(
                title = t("Panduan Pengguna"),
                description = t("Baca alur penggunaan tombol, fitur, input, laporan, dan notifikasi."),
                icon = Icons.Outlined.Info,
                onClick = { activeSectionName = ProfileSection.Guide.name }
              )
            }

            ProfileSection.Information -> {
              ProfileAvatar(
                name = if (name.isBlank()) profile.name else name,
                avatarUrl = draft.avatarUri,
                onEditClick = {
                  scope.launch {
                    snackbarHostState.showSnackbar(avatarPickerMessage)
                  }
                }
              )

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
                  text = t("Informasi Profil"),
                  style = MaterialTheme.typography.titleMedium,
                  color = PrimaryBlueDark,
                  fontWeight = FontWeight.ExtraBold
                )
                Text(
                  text = t("Perubahan profil akan disimpan ke server web lalu diperbarui ke cache lokal."),
                  style = MaterialTheme.typography.bodySmall,
                  color = SubtleInk
                )

                ProfileInputField(
                  label = t("Name"),
                  value = name,
                  onValueChange = { name = it }
                )
                ProfileInputField(
                  label = t("Address"),
                  value = address,
                  onValueChange = { address = it }
                )
                ProfileInputField(
                  label = t("Username"),
                  value = username,
                  onValueChange = { username = it },
                  enabled = false
                )
                PhoneNumberField(
                  label = t("Phone number"),
                  value = phoneNumber,
                  onValueChange = { phoneNumber = it.filter(Char::isDigit) }
                )
              }
            }

            ProfileSection.Settings -> {
              ProfileSettingsPanel(
                selectedLanguageCode = languageCode,
                onSelectLanguage = onApplyLanguage,
                selectedThemeModeCode = themeModeCode,
                onSelectThemeMode = onApplyThemeMode,
                onRefresh = onRefresh
              )
            }

            ProfileSection.Guide -> {
              if (activeGuide != null) {
                UserGuideDetailPanel(section = activeGuide)
              } else {
                UserGuidePanel(
                  guideSections = guideSections,
                  onOpenDetail = { activeGuideTitle = it.title },
                  onStartInteractiveGuide = onStartInteractiveGuide
                )
              }
            }

            ProfileSection.PasswordSecurity -> {
              PasswordSecurityPanel(
                onChangePassword = onChangePassword,
                snackbarHostState = snackbarHostState
              )
            }

            ProfileSection.GoogleAccount -> {
              GoogleAccountPanel(
                profile = profile,
                onLinkGoogleAccount = onLinkGoogleAccount,
                onRefresh = onRefresh
              )
            }
          }

          Spacer(modifier = Modifier.size(124.dp))
        }
      }

      SavingOverlay(visible = isSaving)
    }
  }
}

@Composable
private fun PasswordSecurityPanel(
  onChangePassword: suspend (String, String) -> ProfileSaveOutcome,
  snackbarHostState: SnackbarHostState
) {
  var currentPassword by rememberSaveable { mutableStateOf("") }
  var newPassword by rememberSaveable { mutableStateOf("") }
  var confirmation by rememberSaveable { mutableStateOf("") }
  var showCurrentPassword by rememberSaveable { mutableStateOf(false) }
  var showNewPassword by rememberSaveable { mutableStateOf(false) }
  var showConfirmation by rememberSaveable { mutableStateOf(false) }
  var isChanging by rememberSaveable { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val currentPasswordRequiredMessage = t("Password saat ini wajib diisi.")
  val passwordLengthMessage = t("Password baru minimal 12 karakter.")
  val passwordStrengthMessage = t("Password harus memuat huruf besar, huruf kecil, angka, dan simbol.")
  val passwordMustDifferMessage = t("Password baru harus berbeda dari password saat ini.")
  val confirmationMismatchMessage = t("Konfirmasi password baru tidak cocok.")

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
      text = t("Keamanan Akun"),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = t("Password lama tidak dapat ditampilkan karena tidak disimpan sebagai teks. Masukkan password saat ini untuk verifikasi."),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
    PasswordField(
      label = t("Password saat ini"),
      value = currentPassword,
      onValueChange = { currentPassword = it },
      visible = showCurrentPassword,
      onToggleVisibility = { showCurrentPassword = !showCurrentPassword }
    )
    PasswordField(
      label = t("Password baru"),
      value = newPassword,
      onValueChange = { newPassword = it },
      visible = showNewPassword,
      onToggleVisibility = { showNewPassword = !showNewPassword }
    )
    PasswordField(
      label = t("Konfirmasi password baru"),
      value = confirmation,
      onValueChange = { confirmation = it },
      visible = showConfirmation,
      onToggleVisibility = { showConfirmation = !showConfirmation }
    )
    Button(
      onClick = {
        scope.launch {
          val validationMessage = when {
            currentPassword.isBlank() -> currentPasswordRequiredMessage
            newPassword.length < 12 -> passwordLengthMessage
            newPassword.none(Char::isLowerCase) || newPassword.none(Char::isUpperCase) ||
              newPassword.none(Char::isDigit) || newPassword.none { !it.isLetterOrDigit() } -> passwordStrengthMessage
            currentPassword == newPassword -> passwordMustDifferMessage
            confirmation != newPassword -> confirmationMismatchMessage
            else -> ""
          }
          if (validationMessage.isNotBlank()) {
            snackbarHostState.showSnackbar(validationMessage)
            return@launch
          }
          isChanging = true
          val result = onChangePassword(currentPassword, newPassword)
          isChanging = false
          if (result.success) {
            currentPassword = ""
            newPassword = ""
            confirmation = ""
            showCurrentPassword = false
            showNewPassword = false
            showConfirmation = false
          }
          snackbarHostState.showSnackbar(result.message)
        }
      },
      enabled = !isChanging,
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp)
    ) {
      Text(if (isChanging) t("Menyimpan...") else t("Simpan Password"))
    }
  }
}

@Composable
private fun GoogleAccountPanel(
  profile: GuruProfile,
  onLinkGoogleAccount: () -> Unit,
  onRefresh: () -> Unit
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
      text = t("Akun Google"),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = if (profile.googleLinked) {
        profile.googleEmail.ifBlank { t("Google sudah tertaut ke akun ini.") }
      } else {
        t("Google belum tertaut. Tautkan akun Google dari sesi login saat ini agar berikutnya bisa masuk dengan tombol Google.")
      },
      style = MaterialTheme.typography.bodyMedium,
      color = if (profile.googleLinked) PrimaryBlueDark else SubtleInk,
      fontWeight = if (profile.googleLinked) FontWeight.SemiBold else FontWeight.Normal
    )
    if (!profile.googleLinked) {
      Button(
        onClick = onLinkGoogleAccount,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
      ) {
        Text(t("Tautkan Google"))
      }
    } else {
      Button(
        onClick = onRefresh,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
      ) {
        Text(t("Perbarui Status"))
      }
    }
  }
}

@Composable
private fun ProfileLandingHeader(
  profile: GuruProfile,
  onEditAvatarClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
      .clip(RoundedCornerShape(28.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(28.dp))
      .padding(horizontal = 18.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    ProfileAvatar(
      name = profile.name,
      avatarUrl = profile.avatarUri,
      onEditClick = onEditAvatarClick
    )
    Text(
      text = profile.name.ifBlank { "Guru MIM" },
      style = MaterialTheme.typography.titleLarge,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      textAlign = TextAlign.Center
    )
    Text(
      text = profile.username.ifBlank { profile.phoneNumber.ifBlank { t("Profil guru") } },
      style = MaterialTheme.typography.bodyMedium,
      color = SubtleInk,
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun ProfileSectionCard(
  title: String,
  description: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(24.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
      .clickable(onClick = onClick)
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Box(
      modifier = Modifier
        .size(46.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(PrimaryBlue.copy(alpha = 0.1f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = PrimaryBlueDark
      )
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        text = t(title),
        style = MaterialTheme.typography.titleMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = t(description),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
    Icon(
      imageVector = Icons.Outlined.ChevronRight,
      contentDescription = null,
      tint = SubtleInk
    )
  }
}

@Composable
private fun UserGuidePanel(
  guideSections: List<UserGuideSection>,
  onOpenDetail: (UserGuideSection) -> Unit,
  onStartInteractiveGuide: (String) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isExportingPdf by rememberSaveable { mutableStateOf(false) }
  var expandedGuideTitle by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
        .clip(RoundedCornerShape(28.dp))
        .background(CardBackground.copy(alpha = 0.94f))
        .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(28.dp))
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = t("Panduan Pengguna"),
        style = MaterialTheme.typography.titleLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = t("Panduan ini menjelaskan alur utama aplikasi. Bagian gambar akan kita tambahkan bertahap dari screenshot aplikasi agar guru mudah mengenali setiap tombol dan halaman."),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
      Button(
        enabled = !isExportingPdf,
        onClick = {
          scope.launch {
            isExportingPdf = true
            val result = runCatching {
              UserGuidePdfExporter.createPdfFile(
                context = context,
                sections = guideSections.toPdfSections()
              )
            }
            result.onSuccess { pdf ->
              UserGuidePdfExporter.openPdf(context, pdf)
            }.onFailure { error ->
              Toast
                .makeText(context, error.message ?: "Gagal membuat PDF panduan.", Toast.LENGTH_LONG)
                .show()
            }
            isExportingPdf = false
          }
        }
      ) {
        Text(t(if (isExportingPdf) "Menyiapkan PDF..." else "Unduh PDF"))
      }
    }

    guideSections.forEachIndexed { index, section ->
      UserGuideSectionCard(
        number = index + 1,
        section = section,
        expanded = expandedGuideTitle == section.title,
        onToggleExpanded = {
          expandedGuideTitle = if (expandedGuideTitle == section.title) null else section.title
        },
        onOpenDetail = { onOpenDetail(section) },
        onStartInteractiveGuide = section.interactiveGuideKey
          .takeIf { it.isNotBlank() }
          ?.let { key -> { onStartInteractiveGuide(key) } }
      )
    }
  }
}

@Composable
private fun UserGuideSectionCard(
  number: Int,
  section: UserGuideSection,
  expanded: Boolean,
  onToggleExpanded: () -> Unit,
  onOpenDetail: () -> Unit,
  onStartInteractiveGuide: (() -> Unit)? = null
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
      .clip(RoundedCornerShape(24.dp))
      .background(CardBackground.copy(alpha = 0.94f))
      .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
      .clickable { onToggleExpanded() }
      .animateContentSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(PrimaryBlue.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = number.toString(),
          style = MaterialTheme.typography.labelLarge,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = t(section.title),
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = t(section.summary),
          style = MaterialTheme.typography.bodySmall,
          color = SubtleInk,
          maxLines = if (expanded) Int.MAX_VALUE else 2,
          overflow = TextOverflow.Ellipsis
        )
      }
      Icon(
        imageVector = Icons.Outlined.KeyboardArrowDown,
        contentDescription = null,
        tint = SubtleInk,
        modifier = Modifier
          .size(24.dp)
          .graphicsLayer {
            rotationZ = if (expanded) 180f else 0f
          }
      )
    }

    if (expanded) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(SoftPanel.copy(alpha = 0.66f))
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        section.steps.forEachIndexed { index, step ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "${index + 1}.",
              style = MaterialTheme.typography.bodyMedium,
              color = PrimaryBlueDark,
              fontWeight = FontWeight.ExtraBold
            )
            Text(
              text = t(step),
              style = MaterialTheme.typography.bodyMedium,
              color = PrimaryBlueDark,
              modifier = Modifier.weight(1f)
            )
          }
        }
        if (section.note.isNotBlank()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(PrimaryBlue.copy(alpha = 0.09f))
              .border(1.dp, PrimaryBlue.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
              .padding(12.dp)
          ) {
            Text(
              text = t(section.note),
              style = MaterialTheme.typography.bodySmall,
              color = SubtleInk
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          if (onStartInteractiveGuide != null) {
            Button(
              onClick = onStartInteractiveGuide,
              modifier = Modifier.padding(end = 8.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = t("Play"),
                modifier = Modifier.padding(start = 6.dp)
              )
            }
          }
          Button(onClick = onOpenDetail) {
            Text(t("Lihat Detail"))
          }
        }
      }
    }
  }
}

@Composable
private fun UserGuideDetailPanel(
  section: UserGuideSection
) {
  val blocks = remember(section) {
    section.detailBlocks.ifEmpty { section.toDefaultDetailBlocks() }
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
        .clip(RoundedCornerShape(28.dp))
        .background(CardBackground.copy(alpha = 0.94f))
        .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(28.dp))
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text(
        text = t(section.title),
        style = MaterialTheme.typography.titleLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = t(section.summary),
        style = MaterialTheme.typography.bodyMedium,
        color = SubtleInk
      )
      Text(
        text = t("Detail panduan ini disiapkan sebagai tutorial lengkap. Gambar referensi akan kita tambahkan bertahap dari screenshot aplikasi."),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk
      )
    }

    blocks.forEachIndexed { index, block ->
      UserGuideDetailBlockCard(
        number = index + 1,
        block = block
      )
    }
  }
}

@Composable
private fun UserGuideDetailBlockCard(
  number: Int,
  block: UserGuideDetailBlock
) {
  val context = LocalContext.current
  val imageResId = remember(block.imageResName) {
    block.imageResName
      .takeIf { it.isNotBlank() }
      ?.let { context.resources.getIdentifier(it, "drawable", context.packageName) }
      ?: 0
  }
  val showImageSlot = block.type == UserGuideBlockType.Step || block.imageResName.isNotBlank()
  val accent = when (block.type) {
    UserGuideBlockType.Warning -> Color(0xFFB45309)
    UserGuideBlockType.Tip -> PrimaryBlue
    UserGuideBlockType.Step -> PrimaryBlueDark
    UserGuideBlockType.Text -> PrimaryBlueDark
  }
  val label = when (block.type) {
    UserGuideBlockType.Warning -> "Catatan Penting"
    UserGuideBlockType.Tip -> "Tips"
    UserGuideBlockType.Step -> "Langkah"
    UserGuideBlockType.Text -> "Penjelasan"
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x0F0F172A), spotColor = Color(0x0F0F172A))
      .clip(RoundedCornerShape(22.dp))
      .background(CardBackground.copy(alpha = 0.95f))
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(22.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = number.toString(),
          style = MaterialTheme.typography.labelLarge,
          color = accent,
          fontWeight = FontWeight.ExtraBold
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = t(label),
          style = MaterialTheme.typography.labelSmall,
          color = accent,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = t(block.title),
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryBlueDark,
          fontWeight = FontWeight.ExtraBold
        )
      }
    }

    if (showImageSlot) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(154.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(SoftPanel.copy(alpha = 0.76f))
          .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
          .padding(12.dp),
        contentAlignment = Alignment.Center
      ) {
        if (imageResId != 0) {
          Image(
            painter = painterResource(id = imageResId),
            contentDescription = t(block.imageCaption.ifBlank { block.title }),
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(14.dp))
          )
        } else {
          Text(
            text = t(block.imageCaption.ifBlank { "Screenshot untuk langkah ini akan ditambahkan di sini." }),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtleInk,
            textAlign = TextAlign.Center
          )
        }
      }
    }

    Text(
      text = t(block.body),
      style = MaterialTheme.typography.bodyMedium,
      color = PrimaryBlueDark
    )
  }
}

private fun UserGuideSection.toDefaultDetailBlocks(): List<UserGuideDetailBlock> {
  val specificBlocks = buildSpecificGuideDetailBlocks(title)
  val blocks = mutableListOf(
    UserGuideDetailBlock(
      type = UserGuideBlockType.Text,
      title = "Apa fungsi bagian ini?",
      body = summary
    )
  )
  if (specificBlocks.isNotEmpty()) {
    blocks += specificBlocks
  } else {
    steps.forEachIndexed { index, step ->
      blocks += UserGuideDetailBlock(
        type = UserGuideBlockType.Step,
        title = "Langkah ${index + 1}",
        body = step
      )
    }
  }
  if (note.isNotBlank()) {
    blocks += UserGuideDetailBlock(
      type = UserGuideBlockType.Warning,
      title = "Hal yang perlu diperhatikan",
      body = note
    )
  }
  blocks += UserGuideDetailBlock(
    type = UserGuideBlockType.Tip,
    title = "Tips penggunaan",
    body = "Baca bagian ini sambil membuka halaman terkait agar posisi tombol dan alurnya lebih mudah dipahami."
  )
  return blocks
}

private fun List<UserGuideSection>.toPdfSections(): List<UserGuidePdfSection> =
  map { section ->
    val blocks = section.detailBlocks.ifEmpty { section.toDefaultDetailBlocks() }
    UserGuidePdfSection(
      title = section.title,
      summary = section.summary,
      blocks = blocks.map { it.toPdfBlock() }
    )
  }

private fun UserGuideDetailBlock.toPdfBlock(): UserGuidePdfBlock {
  val label = when (type) {
    UserGuideBlockType.Warning -> "Catatan Penting"
    UserGuideBlockType.Tip -> "Tips"
    UserGuideBlockType.Step -> "Langkah"
    UserGuideBlockType.Text -> "Penjelasan"
  }
  return UserGuidePdfBlock(
    label = label,
    title = title,
    body = body,
    hasImageSlot = type == UserGuideBlockType.Step || imageResName.isNotBlank(),
    imageResName = imageResName,
    imageCaption = imageCaption
  )
}

private fun guideText(title: String, body: String) =
  UserGuideDetailBlock(UserGuideBlockType.Text, title, body)

private fun guideStep(
  title: String,
  body: String,
  imageResName: String = "",
  imageCaption: String = ""
) = UserGuideDetailBlock(UserGuideBlockType.Step, title, body, imageResName, imageCaption)

private fun guideTip(title: String, body: String) =
  UserGuideDetailBlock(UserGuideBlockType.Tip, title, body)

private fun guideWarning(title: String, body: String) =
  UserGuideDetailBlock(UserGuideBlockType.Warning, title, body)

private fun buildSpecificGuideDetailBlocks(title: String): List<UserGuideDetailBlock> = when (title) {
  "Mulai Menggunakan Aplikasi" -> listOf(
    guideStep(
      "Halaman login",
      "Halaman login berisi logo madrasah, judul aplikasi, kolom ID Karyawan, kolom Password, pilihan Remember me, dan tombol Masuk. ID Karyawan dipakai sebagai identitas akun guru. Password dipakai untuk memverifikasi akses. Tombol mata pada kolom password dipakai untuk melihat atau menyembunyikan password agar guru bisa memeriksa ketikan."
    ),
    guideStep(
      "Remember me dan sesi login",
      "Jika Remember me aktif, aplikasi menyimpan sesi login di perangkat. Setelah login berhasil, guru tidak perlu login ulang saat aplikasi dibuka lagi, kecuali logout manual, data aplikasi dihapus, atau sesi dinyatakan tidak valid."
    ),
    guideStep(
      "Welcome screen dan progress data",
      "Setelah login atau saat aplikasi dibuka, welcome screen menampilkan proses persiapan data. Bar progress menunjukkan tahapan sinkronisasi. Pada instalasi pertama, proses bisa lebih lama karena aplikasi mengunduh banyak data awal. Pada pembukaan berikutnya, aplikasi memakai cache lokal lalu hanya menyinkronkan perubahan."
    ),
    guideWarning(
      "Jika server atau jaringan bermasalah",
      "Jika muncul pesan tidak dapat terhubung ke server login, periksa jaringan perangkat dan status server. Jika guru sudah pernah login, data cache tetap dapat dipakai untuk beberapa fitur yang sudah tersedia lokal, kemudian disinkronkan kembali saat jaringan normal."
    )
  )

  "Navigasi Utama" -> listOf(
    guideStep(
      "Tombol sidebar di kiri atas",
      "Tombol bergambar garis menu di kiri atas membuka sidebar. Sidebar adalah daftar lengkap fitur aplikasi. Jika sedang berada di detail profil atau detail panduan, tombol kiri berubah menjadi tombol kembali agar guru bisa kembali satu level."
    ),
    guideStep(
      "Isi sidebar",
      "Sidebar dikelompokkan menjadi Dashboard, Kinerja Guru, Akademik, Aktivitas Harian, Kelas Saya, Wakasek Akademik, dan Profil. Beberapa grup hanya muncul jika guru punya hak akses, misalnya Kelas Saya untuk wali kelas dan Wakasek Akademik untuk akun wakasek akademik."
    ),
    guideStep(
      "Bottom navigation",
      "Bottom navigation berada di bawah layar sebagai shortcut. Label selalu terlihat agar guru tidak menebak fungsi ikon. Dashboard menjadi item tetap, sedangkan item lain dapat disesuaikan dari daftar fitur yang tersedia."
    ),
    guideStep(
      "Popup atur shortcut",
      "Tekan lama area bottom navigation atau salah satu tombolnya untuk membuka popup Atur Shortcut. Popup ini menampilkan daftar tab yang bisa dipilih. Item yang dicentang muncul di bagian atas. Gunakan tombol geser ke atas dan geser ke bawah untuk mengubah urutan. Urutan paling atas menjadi posisi paling kiri di bottom navigation."
    ),
    guideWarning(
      "Batas shortcut",
      "Jumlah shortcut dibatasi agar bottom navigation tetap rapi dan tidak terlalu padat. Jika pilihan sudah penuh, hapus salah satu item sebelum menambahkan item baru."
    )
  )

  "Dashboard dan Agenda" -> listOf(
    guideStep(
      "Tombol menu dan notifikasi",
      "Di bagian atas dashboard ada tombol sidebar di kiri, tanggal di tengah, dan tombol notifikasi di kanan. Tombol notifikasi memiliki badge jumlah notifikasi belum dibaca. Jika ditekan, muncul popup notifikasi."
    ),
    guideStep(
      "Popup notifikasi",
      "Popup notifikasi berisi judul Notifikasi, tombol Tandai semua dibaca, filter Hari ini, 3 Hari, dan 7 Hari, lalu daftar notifikasi. Item notifikasi bisa berisi jam mengajar, agenda, amanat penggantian, tugas pengganti, atau informasi sistem. Menekan item tertentu dapat membuka halaman terkait, misalnya input absen untuk amanat penggantian."
    ),
    guideStep(
      "Kategori agenda",
      "Bagian kategori menampilkan Semua Kategori, Kegiatan Umum, Libur Akademik, Libur Ketahfizan, dan kategori lain dari kalender akademik. Memilih kategori akan memfilter daftar Dashboard Agenda. Semua Kategori tidak memasukkan jadwal mengajar agar agenda tidak terlalu ramai."
    ),
    guideStep(
      "Search agenda",
      "Kolom pencarian dipakai untuk mencari agenda berdasarkan judul, keterangan, atau kategori. Ketik sebagian kata saja, daftar agenda akan menyesuaikan."
    ),
    guideStep(
      "Tanggal di dashboard",
      "Tanggal di header dapat ditekan untuk membuka halaman kalender. Halaman kalender menampilkan timeline kegiatan sesuai tanggal yang dipilih."
    )
  )

  "Jadwal Mengajar" -> listOf(
    guideStep(
      "Pemilihan hari",
      "Halaman Jadwal menampilkan pilihan hari dalam satu pekan. Geser horizontal untuk melihat hari lain. Hari ini diberi penanda khusus dan bisa dikembalikan dengan tombol Hari ini."
    ),
    guideStep(
      "Timeline jadwal",
      "Timeline menampilkan mapel, kelas, jam pelajaran, dan status jadwal. Kartu jadwal disusun berdasarkan waktu. Jika tidak ada jadwal, halaman menampilkan keterangan kosong."
    ),
    guideStep(
      "Ikon pengingat",
      "Ikon pengingat di area judul timeline membuka popup pengaturan pengingat jam pelajaran. Popup ini berisi pilihan pengingat untuk semua mapel atau mapel tertentu, pilihan frekuensi, dan daftar pengingat yang sudah dibuat."
    ),
    guideStep(
      "Alarm dan notifikasi jadwal",
      "Notifikasi jadwal pelajaran dikirim melalui notifikasi HP. Alarm khusus dapat menampilkan halaman alarm di dalam aplikasi saat aplikasi aktif. Izin notifikasi diminta saat pertama kali aplikasi digunakan."
    ),
    guideWarning(
      "Perbedaan notifikasi dan alarm",
      "Notifikasi jadwal adalah pemberitahuan HP untuk jam pelajaran. Alarm adalah pengingat yang lebih menonjol dan memiliki pengaturan tersendiri. Keduanya dipisahkan agar guru bisa mengatur sesuai kebutuhan."
    )
  )

  "Input Absensi" -> listOf(
    guideStep(
      "Header dan tab input",
      "Halaman input memiliki header dengan tombol sidebar dan judul. Di bawahnya ada indikator tab untuk Input Absen dan Input Nilai. Geser horizontal untuk berpindah antara input absen dan input nilai tanpa menggeser header."
    ),
    guideStep(
      "Card jadwal dan kelas",
      "Card pertama berisi tanggal, kelas, mapel, dan jam pelajaran. Tanggal dipilih dari picker, bukan diketik. Setelah tanggal dipilih, aplikasi mencari jadwal guru pada tanggal tersebut lalu mengisi kelas, mapel, dan jam secara otomatis. Jika ada lebih dari satu jadwal, guru memilih dari dropdown."
    ),
    guideStep(
      "Popup info pada card",
      "Setiap card penting memiliki tombol info kecil bertanda i. Jika ditekan, muncul popup kecil yang menjelaskan cara memakai card tersebut. Contohnya card jadwal menjelaskan bahwa guru harus memilih tanggal dulu agar kelas dan mapel tersedia."
    ),
    guideStep(
      "Card materi",
      "Card materi berisi input materi yang diajarkan. Guru bisa mengetik manual atau memilih materi dari dropdown patron materi. Jika ada riwayat materi terakhir, kartu pengingat materi terakhir dapat ditekan untuk mengisi input materi otomatis."
    ),
    guideStep(
      "Card guru pengganti",
      "Card guru pengganti memiliki pilihan isi sebagai guru pengganti dan amanatkan guru pengganti. Jika guru mengisi sebagai pengganti tanpa amanat dari guru utama, data dikirim untuk review guru utama. Jika guru menerima amanat dari guru utama, hasil absen langsung masuk sistem."
    ),
    guideStep(
      "Daftar santri",
      "Setiap baris santri berisi nama dan dropdown status. Status yang tersedia mengikuti versi web, seperti Hadir, Terlambat, Izin, Sakit, dan Alpa. Setelah semua status sesuai, tekan Simpan untuk menyimpan absensi."
    ),
    guideWarning(
      "Saat pilihan kelas atau mapel kosong",
      "Jika kelas atau mapel tidak bisa dipilih, biasanya tanggal belum memiliki jadwal untuk guru tersebut atau data jadwal belum tersinkron. Coba tarik untuk refresh atau pilih mode guru pengganti jika memang sedang menggantikan guru lain."
    )
  )

  "Input Nilai" -> listOf(
    guideStep(
      "Card jadwal nilai",
      "Input nilai memakai tanggal, kelas, mapel, dan jenis nilai. Berbeda dengan absensi, input nilai tidak memakai jam pelajaran, guru pengganti, atau materi terakhir."
    ),
    guideStep(
      "Jenis nilai",
      "Jenis nilai dipilih dari dropdown, misalnya tugas, ulangan harian, UTS, UAS, atau jenis lain yang tersedia. Jenis nilai menentukan batas maksimal nilai dan cara nilai ditampilkan di detail mapel."
    ),
    guideStep(
      "Kolom nilai santri",
      "Setiap santri memiliki kolom nilai. Jika nilai tidak diisi, saat disimpan akan dihitung 0. Jika nilai melebihi batas maksimal, input dicegah agar data tetap sesuai ketentuan."
    ),
    guideStep(
      "Simpan nilai",
      "Tombol Simpan menyimpan nilai semua santri sekaligus. Setelah tersimpan, data muncul di detail mapel bagian Nilai sebagai rata-rata atau akumulasi sesuai jenis nilai."
    ),
    guideWarning(
      "Nilai akumulasi",
      "Nilai yang terlihat di depan detail mapel bisa berupa rata-rata dari banyak data. Untuk mengubah rincian, buka detail jenis nilai agar guru mengedit data sumbernya, bukan hanya angka rata-rata."
    )
  )

  "Mapel dan Detail Mapel" -> listOf(
    guideStep(
      "Daftar mapel",
      "Halaman Mapel menampilkan daftar mata pelajaran yang diampu guru. Tombol Tambah Mapel dipakai untuk mengambil mapel yang tersedia. Jika tidak ada mapel yang bisa ditambahkan, aplikasi menampilkan pesan bahwa tidak ada mapel tersedia."
    ),
    guideStep(
      "Detail mapel",
      "Menekan card mapel membuka detail mapel. Bottom navigation di detail berubah menjadi tab khusus seperti Absensi, Nilai, Patron Materi, dan Soal. Tombol back HP dari detail mapel kembali ke daftar Mapel."
    ),
    guideStep(
      "Absensi di detail mapel",
      "Tab Absensi bisa disortir per nama atau per tanggal. Card per nama menampilkan persentase status yang ada saja. Menekan card membuka riwayat tanggal dan status. Tekan lama item riwayat untuk mengubah status per item."
    ),
    guideStep(
      "Nilai di detail mapel",
      "Tab Nilai bisa disortir per nama, jenis nilai, atau tanggal. Menekan jenis nilai membuka popup rincian nilai sehingga nilai akumulasi dapat diedit dari data sumbernya."
    ),
    guideStep(
      "Patron materi",
      "Patron Materi berisi daftar materi rencana ajar. Tombol tambah menaruh kolom baru di atas agar mudah diisi. Tekan lama card untuk edit. Geser kiri secukupnya untuk memunculkan tombol hapus."
    )
  )

  "Soal dan Cetak Soal" -> listOf(
    guideStep(
      "Daftar soal",
      "Di tab Soal, guru bisa membuat file soal baru dengan tombol buat soal. Popup buat soal hanya berisi judul, kategori seperti Tugas atau UTS, dan tanggal. Setelah disimpan, card soal muncul di daftar."
    ),
    guideStep(
      "Editor soal",
      "Menekan card soal membuka editor. Bottom navigation disembunyikan agar ruang menulis lebih luas. Tombol back di topbar kembali ke daftar soal."
    ),
    guideStep(
      "Tombol plus",
      "Tombol plus bulat di kanan bawah membuka pilihan Tambah Model Soal dan Cetak. Tambah Model Soal menampilkan dropdown model seperti Pilihan Ganda, Esai, Isian, Benar/Salah, Pasangkan Kata, Cari Kata, dan Teka-teki Silang."
    ),
    guideStep(
      "Model pilihan ganda dan esai",
      "Pilihan ganda menyediakan nomor pertanyaan dan pilihan jawaban. Pilihan baru muncul otomatis saat pilihan terakhir diisi. Esai dan isian mengikuti gaya pertanyaan bernomor tanpa skor dan bobot."
    ),
    guideStep(
      "Model puzzle",
      "Cari Kata dan Teka-teki Silang memakai grid. Jika kolom banyak, grid dapat digeser horizontal. Pada Teka-teki Silang, petunjuk dicetak di bawah tabel."
    ),
    guideStep(
      "Cetak soal",
      "Cetak soal mengekspor file Word sesuai template web. Hasil cetak memakai data yang ada di editor saat itu, bukan hanya data lama. Jika file pernah dicetak, file berikutnya diperbarui agar perubahan terbaru ikut masuk."
    )
  )

  "Mutabaah" -> listOf(
    guideStep(
      "Timeline mutabaah",
      "Mutabaah menampilkan tugas harian dalam timeline. Setiap card memiliki titik indikator di samping timeline dan checkbox/tombol centang untuk menandai tugas selesai."
    ),
    guideStep(
      "Hari tanpa jadwal mengajar",
      "Jika guru tidak memiliki jadwal mengajar pada hari itu, hanya tugas Hadir tepat waktu dan Pulang tepat waktu yang ditampilkan. Saat Hadir dicentang, tugas latar belakang selain Pulang ikut ditandai. Saat Pulang dicentang, tugas latar belakang selain Hadir ikut ditandai."
    ),
    guideStep(
      "Hari izin",
      "Jika izin guru sudah disetujui, hari itu ditandai Izin. Mutabaah tidak dihitung sebagai tugas gagal. Di hasil Excel, Izin ditulis dengan background abu-abu."
    ),
    guideStep(
      "Menu titik tiga",
      "Titik tiga di topbar membuka pilihan Cetak Mutabaah dan Kirim Mutabaah. Keduanya membuka popup pilih periode terlebih dahulu."
    ),
    guideStep(
      "Popup pilih periode mutabaah",
      "Popup periode berisi pilihan Bulan Ini, Semester, Tahun, daftar bulan yang bisa dicentang, tombol Batal, dan tombol Cetak atau Kirim. Jika beberapa bulan dipilih, file Excel berisi beberapa tab, satu tab untuk setiap bulan."
    )
  )

  "Perizinan" -> listOf(
    guideStep(
      "Card input izin",
      "Card input izin berisi tanggal mulai, tanggal selesai, durasi, dan keperluan izin. Tanggal dipilih dari picker agar format tanggal konsisten."
    ),
    guideStep(
      "Kirim pengajuan",
      "Setelah data lengkap, tombol kirim membuat pengajuan izin. Status awal biasanya Menunggu. Jika proses sedang berjalan, card menampilkan animasi loading agar guru tahu data sedang diproses."
    ),
    guideStep(
      "Daftar pengajuan",
      "Daftar pengajuan menampilkan status Menunggu, Diterima, atau Ditolak. Jika masih bisa dibatalkan, tersedia aksi hapus atau batal pada card pengajuan."
    ),
    guideStep(
      "Perizinan wakasek",
      "Untuk wakasek akademik, card izin dapat dibuka untuk melihat detail. Tombol Setujui dan Tolak memproses izin. Setelah ditekan, popup menutup dan proses loading muncul pada card yang sedang diproses."
    )
  )

  "Laporan Bulanan dan PTS" -> listOf(
    guideStep(
      "Daftar santri",
      "Halaman laporan menampilkan periode dan daftar santri. Tekan card santri untuk masuk detail. Tekan lama card santri untuk memunculkan bottom bar Cetak dan Kirim tanpa masuk detail."
    ),
    guideStep(
      "Detail laporan",
      "Detail laporan memisahkan aspek penilaian dalam card agar mudah dibaca. Nilai berada di kanan jika berbentuk angka, sedangkan teks panjang tampil di area keterangan."
    ),
    guideStep(
      "Edit laporan",
      "Tombol edit membuat data laporan bisa diubah. Perubahan hanya untuk kebutuhan laporan dan tidak mengubah data asli sistem seperti absensi atau nilai sumber. Setelah disimpan, mode edit ditutup."
    ),
    guideStep(
      "Reset data",
      "Tombol reset mengembalikan data laporan bulan atau periode tersebut ke data sistem. Reset tetap tersedia meskipun data sudah pernah disimpan setelah edit atau import massal."
    ),
    guideStep(
      "Input massal",
      "Input massal memakai link spreadsheet. Aplikasi mencocokkan tab kelas dan nama santri dengan toleransi alias. Preview menampilkan data cocok, meragukan, atau tidak cocok. Data meragukan perlu dikonfirmasi sebelum diterapkan."
    ),
    guideStep(
      "Cetak dan kirim WhatsApp",
      "Cetak menghasilkan PDF sesuai template. Kirim WhatsApp membuka pilihan nomor orang tua atau input manual. Pilih dari kontak HP tersedia untuk mengambil nomor dari kontak, lalu file dikirim sebagai lampiran."
    )
  )

  "Santri dan Wali Kelas" -> listOf(
    guideStep(
      "Akses wali kelas",
      "Menu Santri, Laporan Absensi, Laporan Bulanan, dan Laporan UTS hanya muncul jika guru terdaftar sebagai wali kelas pada data kelas atau struktur sekolah."
    ),
    guideStep(
      "Daftar santri",
      "Daftar santri menampilkan santri di kelas wali. Keterangan di bawah nama menampilkan NISN. Card tidak memiliki tombol hapus karena pengelolaan santri utama tetap dari sistem admin."
    ),
    guideStep(
      "Detail santri",
      "Detail santri berisi nama, NISN, jenis kelamin, kontak santri, alamat, ayah, kontak ayah, ibu, kontak ibu, wali, kontak wali, dan catatan. Jenis kelamin dipilih dari dropdown agar tidak asal mengetik."
    ),
    guideStep(
      "Simpan detail santri",
      "Saat ada perubahan, tombol simpan muncul. Ketika disimpan, layar menampilkan loading agar guru tahu proses berjalan. Data dikirim ke server dan cache lokal diperbarui."
    ),
    guideStep(
      "Laporan absensi kelas",
      "Laporan absensi menghitung kehadiran santri per hari dari semua mapel. Detail menampilkan tanggal, mapel jika tersedia, jam pelajaran jika tersedia, dan status kehadiran."
    )
  )

  "Wakasek Akademik" -> listOf(
    guideStep(
      "Akses menu wakasek",
      "Menu Wakasek Akademik muncul untuk akun guru yang rolenya tercatat sebagai wakasek akademik. Menu ini memiliki sub menu Monitoring Guru, Monitoring Siswa, dan Perizinan."
    ),
    guideStep(
      "Monitoring Guru",
      "Monitoring Guru memiliki pilihan periode harian, pekanan, bulanan, dan semester. Pilihannya berbentuk pill seperti jadwal. Card diagram menunjukkan persentase hadir dan tidak hadir. Menekan guru membuka detail kelas, mapel, waktu, dan status."
    ),
    guideStep(
      "Monitoring Siswa",
      "Monitoring Siswa menampilkan grafik status siswa. Siswa yang hadir tidak dimunculkan di list agar daftar tidak terlalu panjang. Nama yang berulang disatukan, lalu detailnya muncul saat card ditekan."
    ),
    guideStep(
      "Sort monitoring siswa",
      "Tombol sort dapat mengurutkan atau memfilter list berdasarkan kelas, nama, atau status kehadiran. Ini membantu wakasek menemukan santri bermasalah lebih cepat."
    ),
    guideStep(
      "Perizinan wakasek",
      "Perizinan wakasek menampilkan pengajuan izin guru. Wakasek membuka detail, memberi catatan jika perlu, lalu memilih Setujui atau Tolak. Proses berjalan pada card terkait tanpa menyegarkan seluruh halaman."
    )
  )

  "Bahasa, Tema, dan Sinkronisasi" -> listOf(
    guideStep(
      "Bahasa aplikasi",
      "Card Bahasa menyediakan dropdown bahasa Indonesia, Inggris, dan Arab. Setelah memilih bahasa, tekan Terapkan agar label UI berubah. Beberapa istilah data seperti nama santri atau nama mapel tidak diterjemahkan karena berasal dari database."
    ),
    guideStep(
      "Tema aplikasi",
      "Card Tema menyediakan Ikuti Sistem, Terang, dan Gelap. Ikuti Sistem mengikuti pengaturan HP. Tema gelap mengubah background, card, sidebar, navbar, dan tombol agar tetap terbaca."
    ),
    guideStep(
      "Sinkronisasi data",
      "Tombol Sinkronisasi Data menarik data terbaru dari server dan memperbarui cache lokal. Tarik ke bawah pada halaman utama juga menjalankan refresh sesuai halaman tersebut."
    ),
    guideStep(
      "Pembaruan aplikasi",
      "Jika tersedia versi baru, aplikasi menampilkan popup pembaruan saat dibuka. Popup ini berisi informasi versi dan tombol untuk mengunduh APK terbaru."
    ),
    guideWarning(
      "Cache dan data lokal",
      "Cache membantu aplikasi terasa cepat. Jika user menghapus cache dari pengaturan HP, pembukaan berikutnya akan lebih lama karena aplikasi perlu mengunduh ulang data awal."
    )
  )

  else -> emptyList()
}

private fun buildUserGuideSections(): List<UserGuideSection> = listOf(
  UserGuideSection(
    title = "Mulai Menggunakan Aplikasi",
    summary = "Gunakan akun guru untuk masuk sekali, lalu aplikasi menyimpan sesi dan cache agar pembukaan berikutnya lebih cepat.",
    steps = listOf(
      "Masukkan ID karyawan dan password pada halaman login.",
      "Tunggu welcome screen menyiapkan data awal sampai masuk ke dashboard.",
      "Gunakan tarik ke bawah pada halaman utama untuk menyinkronkan data terbaru dari server.",
      "Jika jaringan tidak stabil, gunakan data yang sudah tersimpan lebih dulu lalu sinkronkan saat jaringan kembali."
    ),
    note = "Login hanya tersedia untuk akun dengan akses guru. Beberapa menu seperti Kelas Saya dan Wakasek Akademik mengikuti amanah jabatan di data sekolah.",
    interactiveGuideKey = "start_app"
  ),
  UserGuideSection(
    title = "Navigasi Utama",
    summary = "Aplikasi memakai sidebar dan bottom navigation. Sidebar berisi semua fitur, sedangkan bottom navigation bisa menjadi shortcut harian.",
    steps = listOf(
      "Tekan tombol menu di kiri atas untuk membuka sidebar.",
      "Pilih grup menu seperti Akademik, Aktivitas Harian, Kelas Saya, atau Wakasek Akademik.",
      "Tekan lama bottom navigation untuk mengatur shortcut yang sering dipakai.",
      "Tombol back dari halaman utama akan kembali ke dashboard, kecuali dashboard yang bisa menutup aplikasi."
    ),
    interactiveGuideKey = "main_navigation"
  ),
  UserGuideSection(
    title = "Dashboard dan Agenda",
    summary = "Dashboard menampilkan kategori kalender akademik, agenda terdekat, notifikasi, dan akses cepat ke tanggal.",
    steps = listOf(
      "Pilih kategori untuk memfilter agenda yang tampil.",
      "Gunakan kolom pencarian untuk mencari agenda tertentu.",
      "Tekan tanggal di header untuk membuka kalender dan timeline kegiatan.",
      "Tekan ikon notifikasi untuk melihat jam mengajar, amanat penggantian, agenda, dan status penting lainnya."
    ),
    interactiveGuideKey = "dashboard_agenda"
  ),
  UserGuideSection(
    title = "Jadwal Mengajar",
    summary = "Halaman jadwal menampilkan jam mengajar dalam bentuk timeline mingguan.",
    steps = listOf(
      "Geser daftar hari untuk memilih tanggal jadwal.",
      "Tekan tombol hari ini untuk kembali ke tanggal sekarang.",
      "Gunakan ikon pengingat untuk mengatur alarm atau notifikasi jam pelajaran.",
      "Notifikasi jadwal dapat aktif walaupun aplikasi sedang tidak dibuka."
    ),
    interactiveGuideKey = "teaching_schedule"
  ),
  UserGuideSection(
    title = "Input Absensi",
    summary = "Input absensi dipakai untuk mengisi kehadiran santri berdasarkan jadwal guru atau tugas pengganti.",
    steps = listOf(
      "Pilih tanggal terlebih dahulu agar kelas, mapel, dan jam pelajaran terisi sesuai jadwal.",
      "Isi materi manual atau pilih dari patron materi.",
      "Jika menjadi guru pengganti, pilih guru yang digantikan lalu pilih jadwal yang sesuai.",
      "Ubah status santri melalui dropdown di samping nama, lalu simpan."
    ),
    note = "Amanat penggantian dari guru utama bisa langsung masuk ke input absen melalui notifikasi.",
    interactiveGuideKey = "attendance_input"
  ),
  UserGuideSection(
    title = "Input Nilai",
    summary = "Input nilai digunakan untuk mengisi nilai per jenis penilaian tanpa jam pelajaran dan tanpa guru pengganti.",
    steps = listOf(
      "Pilih tanggal, kelas, mapel, dan jenis nilai.",
      "Masukkan nilai santri sesuai batas maksimal yang ditentukan.",
      "Nilai kosong akan dihitung sebagai 0 saat disimpan.",
      "Gunakan halaman detail nilai untuk melihat rata-rata dan rincian nilai per tanggal."
    ),
    interactiveGuideKey = "score_input"
  ),
  UserGuideSection(
    title = "Mapel dan Detail Mapel",
    summary = "Mapel menampilkan daftar mata pelajaran yang diampu, lalu detailnya berisi absensi, nilai, patron materi, dan soal.",
    steps = listOf(
      "Tekan card mapel untuk membuka detail mapel.",
      "Tab Absensi menampilkan rekap per nama atau per tanggal.",
      "Tab Nilai menampilkan akumulasi per nama atau per tanggal beserta rincian nilai.",
      "Tab Patron Materi dipakai untuk menyiapkan daftar materi yang bisa dipilih saat input absensi.",
      "Tab Soal dipakai untuk membuat draft soal dan mengekspor hasilnya."
    ),
    interactiveGuideKey = "subject_detail"
  ),
  UserGuideSection(
    title = "Soal dan Cetak Soal",
    summary = "Editor soal bebas dipakai untuk membuat tugas, UTS, UAS, atau latihan tanpa bergantung pada jadwal ujian admin.",
    steps = listOf(
      "Buat file soal dari daftar soal dengan mengisi judul, kategori, dan tanggal.",
      "Masuk ke editor soal lalu tambah model soal dari tombol plus.",
      "Isi instruksi umum, instruksi per model, pertanyaan, pilihan jawaban, pasangan kata, cari kata, atau teka-teki silang.",
      "Gunakan tombol cetak untuk mengekspor soal sesuai template Word."
    ),
    interactiveGuideKey = "question_export"
  ),
  UserGuideSection(
    title = "Mutabaah",
    summary = "Mutabaah berisi aktivitas harian guru dan bisa dicetak atau dikirim dalam format Excel.",
    steps = listOf(
      "Centang tugas yang sudah dilakukan pada tanggal terpilih.",
      "Jika guru tidak memiliki jadwal mengajar, hanya tugas hadir dan pulang tepat waktu yang ditampilkan.",
      "Hari izin yang disetujui akan ditandai sebagai izin dan tidak dihitung sebagai kegagalan tugas.",
      "Gunakan titik tiga di topbar untuk cetak atau kirim mutabaah per bulan, semester, atau tahun pelajaran."
    ),
    interactiveGuideKey = "mutabaah"
  ),
  UserGuideSection(
    title = "Perizinan",
    summary = "Perizinan dipakai untuk mengajukan izin guru dan memantau status pengajuan.",
    steps = listOf(
      "Isi rentang tanggal dan keperluan izin.",
      "Kirim pengajuan lalu pantau status menunggu, diterima, atau ditolak.",
      "Pengajuan yang belum selesai dapat dibatalkan atau dihapus.",
      "Wakasek Akademik dapat menyetujui atau menolak izin dari menu Perizinan Wakasek."
    ),
    interactiveGuideKey = "permission"
  ),
  UserGuideSection(
    title = "Laporan Bulanan dan PTS",
    summary = "Laporan dapat dilihat, diedit sebagai data laporan, direset ke data sistem, dicetak, dan dikirim melalui WhatsApp.",
    steps = listOf(
      "Pilih periode laporan yang tersedia.",
      "Tekan nama santri untuk melihat detail laporan.",
      "Gunakan edit untuk mengubah data khusus laporan tanpa mengubah data asli sistem.",
      "Gunakan reset jika ingin kembali ke data sistem.",
      "Cetak atau kirim laporan dari detail, atau tekan lama nama santri dari daftar."
    ),
    interactiveGuideKey = "reports"
  ),
  UserGuideSection(
    title = "Santri dan Wali Kelas",
    summary = "Menu santri dan laporan kelas hanya muncul untuk guru yang menjadi wali kelas.",
    steps = listOf(
      "Buka menu Santri untuk melihat santri di kelas yang diampu sebagai wali kelas.",
      "Tekan nama santri untuk mengubah detail santri, kontak, orang tua, wali, alamat, dan catatan.",
      "Gunakan laporan absensi untuk melihat rekap kehadiran kelas dari semua mapel.",
      "Menu Kelas Saya tidak ditampilkan untuk guru yang bukan wali kelas."
    ),
    interactiveGuideKey = "student_teacher"
  ),
  UserGuideSection(
    title = "Wakasek Akademik",
    summary = "Menu Wakasek Akademik hanya muncul untuk akun yang tercatat sebagai wakasek akademik di data karyawan.",
    steps = listOf(
      "Monitoring Guru menampilkan kehadiran guru per hari, pekan, bulan, atau semester.",
      "Monitoring Siswa menampilkan santri yang sakit, izin, terlambat, atau alpa dengan filter kelas, nama, dan status.",
      "Perizinan Wakasek dipakai untuk menyetujui atau menolak pengajuan izin guru.",
      "Data monitoring mengikuti data admin dan kalender akademik."
    ),
    interactiveGuideKey = "wakasek"
  ),
  UserGuideSection(
    title = "Bahasa, Tema, dan Sinkronisasi",
    summary = "Pengaturan berisi pilihan bahasa, tema terang atau gelap, serta sinkronisasi data manual.",
    steps = listOf(
      "Pilih bahasa Indonesia, Inggris, atau Arab lalu tekan Terapkan.",
      "Pilih tema Ikuti Sistem, Terang, atau Gelap sesuai kebutuhan.",
      "Gunakan Sinkronisasi Data untuk menarik ulang data terbaru dari server.",
      "Jika ada pembaruan aplikasi, popup update akan muncul saat aplikasi dibuka."
    ),
    interactiveGuideKey = "settings_sync"
  )
)

@Composable
private fun ProfileSettingsPanel(
  selectedLanguageCode: String,
  onSelectLanguage: (String) -> Unit,
  selectedThemeModeCode: String,
  onSelectThemeMode: (String) -> Unit,
  onRefresh: () -> Unit
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
      text = t("Pengaturan Aplikasi"),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = t("Bagian ini kita siapkan sebagai tempat pengaturan lanjutan agar profil tidak terlalu padat."),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
    ProfileSectionCard(
      title = t("Sinkronisasi Data"),
      description = t("Tarik ulang data terbaru dari server dan perbarui cache lokal."),
      icon = Icons.Outlined.Sync,
      onClick = onRefresh
    )
    ProfileLanguageCard(
      selectedLanguageCode = selectedLanguageCode,
      onSelectLanguage = onSelectLanguage
    )
    ProfileThemeCard(
      selectedThemeModeCode = selectedThemeModeCode,
      onSelectThemeMode = onSelectThemeMode
    )
  }
}

@Composable
private fun ProfileLanguageCard(
  selectedLanguageCode: String,
  onSelectLanguage: (String) -> Unit
) {
  val languageOptions = listOf(
    ProfileLanguageOption("id", t("Bahasa Indonesia"), t("Default aplikasi")),
    ProfileLanguageOption("en", "English", t("Use English labels")),
    ProfileLanguageOption(
      "ar",
      "\u0627\u0644\u0639\u0631\u0628\u064A\u0629",
      "\u0627\u0633\u062A\u062E\u062F\u0627\u0645 \u0627\u0644\u0644\u063A\u0629 \u0627\u0644\u0639\u0631\u0628\u064A\u0629"
    )
  )
  var draftLanguageCode by rememberSaveable(selectedLanguageCode) {
    mutableStateOf(selectedLanguageCode)
  }
  var expanded by remember { mutableStateOf(false) }
  val selectedOption = languageOptions.firstOrNull { it.code == selectedLanguageCode } ?: languageOptions.first()
  val draftOption = languageOptions.firstOrNull { it.code == draftLanguageCode } ?: selectedOption

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .background(SoftPanel)
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = t("Bahasa"),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = t("Pilih bahasa tampilan aplikasi. Tekan Terapkan agar pilihan dipakai."),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )

    Box {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(CardBackground.copy(alpha = 0.82f))
          .border(1.dp, CardBorder.copy(alpha = 0.86f), RoundedCornerShape(18.dp))
          .clickable { expanded = true }
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Text(
            text = draftOption.title,
            style = MaterialTheme.typography.bodyMedium,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = draftOption.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = SubtleInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Icon(
          imageVector = Icons.Outlined.KeyboardArrowDown,
          contentDescription = t("Pilih bahasa"),
          tint = PrimaryBlueDark
        )
      }

      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
      ) {
        languageOptions.forEach { option ->
          DropdownMenuItem(
            text = {
              Column {
                Text(
                  text = option.title,
                  fontWeight = if (draftLanguageCode == option.code) FontWeight.ExtraBold else FontWeight.SemiBold,
                  color = PrimaryBlueDark
                )
                Text(
                  text = option.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = SubtleInk
                )
              }
            },
            onClick = {
              draftLanguageCode = option.code
              expanded = false
            }
          )
        }
      }
    }

    Button(
      onClick = { onSelectLanguage(draftLanguageCode) },
      enabled = draftLanguageCode != selectedLanguageCode,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = if (draftLanguageCode == selectedLanguageCode) {
          "${t("Bahasa aktif")}: ${selectedOption.title}"
        } else {
          t("Terapkan")
        },
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun ProfileThemeCard(
  selectedThemeModeCode: String,
  onSelectThemeMode: (String) -> Unit
) {
  val themeOptions = listOf(
    ProfileThemeOption("system", t("Ikuti Sistem"), t("Mengikuti pengaturan tema perangkat.")),
    ProfileThemeOption("light", t("Terang"), t("Gunakan tampilan terang.")),
    ProfileThemeOption("dark", t("Gelap"), t("Gunakan tampilan gelap."))
  )
  var draftThemeModeCode by rememberSaveable(selectedThemeModeCode) {
    mutableStateOf(selectedThemeModeCode.ifBlank { "system" })
  }
  var expanded by remember { mutableStateOf(false) }
  val selectedOption = themeOptions.firstOrNull { it.code == selectedThemeModeCode } ?: themeOptions.first()
  val draftOption = themeOptions.firstOrNull { it.code == draftThemeModeCode } ?: selectedOption

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .background(SoftPanel)
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = t("Tema"),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = t("Pilih tampilan aplikasi. Ikuti Sistem akan menyesuaikan tema HP."),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )

    Box {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(CardBackground.copy(alpha = 0.82f))
          .border(1.dp, CardBorder.copy(alpha = 0.86f), RoundedCornerShape(18.dp))
          .clickable { expanded = true }
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Text(
            text = draftOption.title,
            style = MaterialTheme.typography.bodyMedium,
            color = PrimaryBlueDark,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = draftOption.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = SubtleInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Icon(
          imageVector = Icons.Outlined.KeyboardArrowDown,
          contentDescription = t("Pilih tema"),
          tint = PrimaryBlueDark
        )
      }

      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
      ) {
        themeOptions.forEach { option ->
          DropdownMenuItem(
            text = {
              Column {
                Text(
                  text = option.title,
                  fontWeight = if (draftThemeModeCode == option.code) FontWeight.ExtraBold else FontWeight.SemiBold,
                  color = PrimaryBlueDark
                )
                Text(
                  text = option.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = SubtleInk
                )
              }
            },
            onClick = {
              draftThemeModeCode = option.code
              expanded = false
            }
          )
        }
      }
    }

    Button(
      onClick = { onSelectThemeMode(draftThemeModeCode) },
      enabled = draftThemeModeCode != selectedThemeModeCode,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = if (draftThemeModeCode == selectedThemeModeCode) {
          "${t("Tema aktif")}: ${selectedOption.title}"
        } else {
          t("Terapkan")
        },
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun ProfileLanguageCardOld(
  selectedLanguageCode: String,
  onSelectLanguage: (String) -> Unit
) {
  val options = listOf(
    Triple("id", "Bahasa Indonesia", "Default aplikasi"),
    Triple("en", "English", "Use English labels"),
    Triple("ar", "العربية", "استخدام اللغة العربية")
  )
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .background(SoftPanel)
      .border(1.dp, CardBorder.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = t("Bahasa"),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = t("Pilih bahasa tampilan aplikasi. Terjemahan menyeluruh akan disambungkan bertahap."),
      style = MaterialTheme.typography.bodySmall,
      color = SubtleInk
    )
    options.forEach { (code, title, subtitle) ->
      ProfileLanguageRow(
        title = title,
        subtitle = subtitle,
        selected = selectedLanguageCode == code,
        onClick = { onSelectLanguage(code) }
      )
    }
  }
}

@Composable
private fun ProfileLanguageRow(
  title: String,
  subtitle: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(if (selected) PrimaryBlue.copy(alpha = 0.10f) else CardBackground.copy(alpha = 0.72f))
      .border(
        width = 1.dp,
        color = if (selected) PrimaryBlue.copy(alpha = 0.28f) else CardBorder.copy(alpha = 0.82f),
        shape = RoundedCornerShape(18.dp)
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(18.dp)
        .clip(CircleShape)
        .border(
          width = 2.dp,
          color = if (selected) PrimaryBlue else SubtleInk.copy(alpha = 0.4f),
          shape = CircleShape
        ),
      contentAlignment = Alignment.Center
    ) {
      if (selected) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(PrimaryBlue)
        )
      }
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = t(title),
        style = MaterialTheme.typography.bodyMedium,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = t(subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = SubtleInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun ProfileTopBar(
  title: String,
  isDetail: Boolean,
  isDirty: Boolean,
  isSaving: Boolean,
  onMenuClick: () -> Unit,
  onBackClick: () -> Unit,
  onCancelClick: () -> Unit,
  onSaveClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    ProfileTopButton(
      icon = when {
        isDirty -> Icons.Outlined.Close
        isDetail -> Icons.AutoMirrored.Outlined.ArrowBack
        else -> Icons.Outlined.Menu
      },
      contentDescription = when {
        isDirty -> t("Batalkan perubahan")
        isDetail -> t("Kembali ke profil")
        else -> t("Buka sidebar")
      },
      onClick = when {
        isDirty -> onCancelClick
        isDetail -> onBackClick
        else -> onMenuClick
      }
    )
    Text(
      text = t(title),
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      textAlign = TextAlign.Center,
      modifier = Modifier.weight(1f)
    )
    if (isDirty) {
      ProfileTopButton(
        icon = Icons.Outlined.Check,
        contentDescription = if (isSaving) t("Sedang menyimpan") else t("Simpan perubahan"),
        onClick = onSaveClick
      )
    } else {
      Spacer(modifier = Modifier.size(42.dp))
    }
  }
}

@Composable
private fun ProfileTopButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
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
fun ProfileAvatar(
  name: String,
  avatarUrl: String,
  onEditClick: () -> Unit
) {
  val remoteAvatar by rememberRemoteAvatar(avatarUrl)
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier.size(104.dp),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .size(96.dp)
          .shadow(12.dp, CircleShape, ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
          .background(CardBackground.copy(alpha = 0.96f), CircleShape)
          .border(1.dp, CardBorder, CircleShape)
          .clip(CircleShape)
          .clickable(onClick = onEditClick),
        contentAlignment = Alignment.Center
      ) {
        if (remoteAvatar != null) {
          Image(
            bitmap = remoteAvatar!!,
            contentDescription = name.ifBlank { "Avatar profil" },
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        } else {
          Icon(
            painter = painterResource(id = R.drawable.logo_mim_full),
            contentDescription = name.ifBlank { "Avatar profil" },
            tint = Color.Unspecified,
            modifier = Modifier.size(58.dp)
          )
        }
      }
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .size(30.dp)
          .background(PrimaryBlue, CircleShape)
          .border(2.dp, CardBackground, CircleShape)
          .clickable(onClick = onEditClick),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.Edit,
          contentDescription = "Edit avatar",
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}

@Composable
fun ProfileInputField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  enabled: Boolean = true
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
      label = { Text(t(label)) },
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    enabled = enabled,
    shape = RoundedCornerShape(16.dp),
    colors = profileTextFieldColors()
  )
}

@Composable
fun PasswordField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  visible: Boolean,
  onToggleVisibility: () -> Unit
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(t(label)) },
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    shape = RoundedCornerShape(16.dp),
    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
    trailingIcon = {
      Icon(
        imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
        contentDescription = if (visible) t("Sembunyikan password") else t("Tampilkan password"),
        tint = SubtleInk,
        modifier = Modifier.clickable(onClick = onToggleVisibility)
      )
    },
    colors = profileTextFieldColors()
  )
}

@Composable
fun PhoneNumberField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(t(label)) },
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    leadingIcon = {
      Text(
        text = "+62",
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlueDark,
        fontWeight = FontWeight.Bold
      )
    },
    keyboardOptions = KeyboardOptions.Default,
    shape = RoundedCornerShape(16.dp),
    colors = profileTextFieldColors()
  )
}

@Composable
private fun profileTextFieldColors() = TextFieldDefaults.colors(
  focusedContainerColor = SoftPanel,
  unfocusedContainerColor = SoftPanel,
  disabledContainerColor = SoftPanel,
  focusedIndicatorColor = PrimaryBlue.copy(alpha = 0.48f),
  unfocusedIndicatorColor = CardBorder,
  focusedLabelColor = PrimaryBlueDark,
  unfocusedLabelColor = SubtleInk,
  focusedTextColor = PrimaryBlueDark,
  unfocusedTextColor = PrimaryBlueDark,
  disabledTextColor = SubtleInk,
  cursorColor = PrimaryBlueDark
)

@Composable
private fun rememberRemoteAvatar(url: String): androidx.compose.runtime.State<ImageBitmap?> {
  return produceState<ImageBitmap?>(initialValue = null, key1 = url) {
    value = loadRemoteAvatar(url)
  }
}

private suspend fun loadRemoteAvatar(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
  if (url.isBlank()) return@withContext null
  runCatching {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
      connectTimeout = 12_000
      readTimeout = 12_000
      doInput = true
    }
    connection.inputStream.use { stream ->
      BitmapFactory.decodeStream(stream)?.asImageBitmap()
    }
  }.getOrNull()
}
