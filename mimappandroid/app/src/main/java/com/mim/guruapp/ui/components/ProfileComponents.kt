package com.mim.guruapp.ui.components

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
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
  Settings
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
  modifier: Modifier = Modifier
) {
  var name by rememberSaveable(profile) { mutableStateOf(profile.name) }
  var address by rememberSaveable(profile) { mutableStateOf(profile.address) }
  var username by rememberSaveable(profile) { mutableStateOf(profile.username) }
  var password by rememberSaveable(profile) { mutableStateOf(profile.password) }
  var phoneNumber by rememberSaveable(profile) { mutableStateOf(profile.phoneNumber) }
  var showPassword by rememberSaveable { mutableStateOf(false) }
  var isSaving by rememberSaveable { mutableStateOf(false) }
  var activeSectionName by rememberSaveable { mutableStateOf(ProfileSection.Menu.name) }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val activeSection = runCatching { ProfileSection.valueOf(activeSectionName) }
    .getOrDefault(ProfileSection.Menu)
  val resetDraftAndReturnToMenu = {
    name = profile.name
    address = profile.address
    username = profile.username
    password = profile.password
    phoneNumber = profile.phoneNumber
    showPassword = false
    activeSectionName = ProfileSection.Menu.name
  }

  val draft = GuruProfile(
    name = name,
    address = address,
    username = username,
    password = password,
    phoneNumber = phoneNumber,
    avatarUri = profile.avatarUri
  )
  val isDirty = draft != profile
  val isInformationDetail = activeSection == ProfileSection.Information
  val avatarPickerMessage = t("Pemilih foto profil akan ditambahkan.")
  BackHandler(enabled = activeSection != ProfileSection.Menu) {
    if (isInformationDetail && isDirty) {
      resetDraftAndReturnToMenu()
    } else {
      activeSectionName = ProfileSection.Menu.name
      showPassword = false
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
              ProfileSection.Settings -> "Pengaturan"
            }.let { t(it) },
            isDetail = activeSection != ProfileSection.Menu,
            isDirty = isInformationDetail && isDirty,
            isSaving = isSaving,
            onMenuClick = onMenuClick,
            onBackClick = {
              activeSectionName = ProfileSection.Menu.name
              showPassword = false
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
                description = t("Edit nama, alamat, username, password, dan nomor HP."),
                icon = Icons.Outlined.Person,
                onClick = { activeSectionName = ProfileSection.Information.name }
              )
              ProfileSectionCard(
                title = t("Pengaturan"),
                description = t("Atur sinkronisasi, keamanan, dan preferensi aplikasi."),
                icon = Icons.Outlined.Settings,
                onClick = { activeSectionName = ProfileSection.Settings.name }
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
                PasswordField(
                  label = t("Password"),
                  value = password,
                  onValueChange = { password = it },
                  visible = showPassword,
                  onToggleVisibility = { showPassword = !showPassword }
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
          }

          Spacer(modifier = Modifier.size(124.dp))
        }
      }

      SavingOverlay(visible = isSaving)
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
