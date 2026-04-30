package com.mim.guruapp.ui.components

import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.unit.dp
import com.mim.guruapp.R
import com.mim.guruapp.data.model.GuruProfile
import com.mim.guruapp.ProfileSaveOutcome
import com.mim.guruapp.ui.theme.AppBackground
import com.mim.guruapp.ui.theme.CardBorder
import com.mim.guruapp.ui.theme.PrimaryBlue
import com.mim.guruapp.ui.theme.PrimaryBlueDark
import com.mim.guruapp.ui.theme.SubtleInk
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
  profile: GuruProfile,
  isRefreshing: Boolean,
  onMenuClick: () -> Unit,
  onRefresh: () -> Unit,
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
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  val draft = GuruProfile(
    name = name,
    address = address,
    username = username,
    password = password,
    phoneNumber = phoneNumber,
    avatarUri = profile.avatarUri
  )
  val isDirty = draft != profile

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
            isDirty = isDirty,
            isSaving = isSaving,
            onMenuClick = onMenuClick,
            onCancelClick = {
              name = profile.name
              address = profile.address
              username = profile.username
              password = profile.password
              phoneNumber = profile.phoneNumber
              showPassword = false
            },
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

          ProfileAvatar(
            name = if (name.isBlank()) profile.name else name,
            avatarUrl = draft.avatarUri,
            onEditClick = {
              scope.launch {
                snackbarHostState.showSnackbar("Pemilih foto profil akan ditambahkan.")
              }
            }
          )

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
              .clip(RoundedCornerShape(28.dp))
              .background(Color.White.copy(alpha = 0.94f))
              .border(1.dp, CardBorder.copy(alpha = 0.92f), RoundedCornerShape(28.dp))
              .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Text(
              text = "Informasi Profil",
              style = MaterialTheme.typography.titleMedium,
              color = PrimaryBlueDark,
              fontWeight = FontWeight.ExtraBold
            )
            Text(
              text = "Perubahan profil akan disimpan ke server web lalu diperbarui ke cache lokal.",
              style = MaterialTheme.typography.bodySmall,
              color = SubtleInk
            )

            ProfileInputField(
              label = "Name",
              value = name,
              onValueChange = { name = it }
            )
            ProfileInputField(
              label = "Address",
              value = address,
              onValueChange = { address = it }
            )
            ProfileInputField(
              label = "Username",
              value = username,
              onValueChange = { username = it },
              enabled = false
            )
            PasswordField(
              label = "Password",
              value = password,
              onValueChange = { password = it },
              visible = showPassword,
              onToggleVisibility = { showPassword = !showPassword }
            )
            PhoneNumberField(
              label = "Phone number",
              value = phoneNumber,
              onValueChange = { phoneNumber = it.filter(Char::isDigit) }
            )
          }

          Spacer(modifier = Modifier.size(8.dp))
        }
      }

      SavingOverlay(visible = isSaving)
    }
  }
}

@Composable
private fun ProfileTopBar(
  isDirty: Boolean,
  isSaving: Boolean,
  onMenuClick: () -> Unit,
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
      icon = if (isDirty) Icons.Outlined.Close else Icons.Outlined.Menu,
      contentDescription = if (isDirty) "Batalkan perubahan" else "Buka sidebar",
      onClick = if (isDirty) onCancelClick else onMenuClick
    )
    Text(
      text = "Edit Profile",
      style = MaterialTheme.typography.titleMedium,
      color = PrimaryBlueDark,
      fontWeight = FontWeight.ExtraBold,
      textAlign = TextAlign.Center,
      modifier = Modifier.weight(1f)
    )
    if (isDirty) {
      ProfileTopButton(
        icon = Icons.Outlined.Check,
        contentDescription = if (isSaving) "Sedang menyimpan" else "Simpan perubahan",
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
          .background(Color.White.copy(alpha = 0.96f), CircleShape)
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
          .border(2.dp, Color.White, CircleShape)
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
    label = { Text(label) },
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
    label = { Text(label) },
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    shape = RoundedCornerShape(16.dp),
    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
    trailingIcon = {
      Icon(
        imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
        contentDescription = if (visible) "Sembunyikan password" else "Tampilkan password",
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
    label = { Text(label) },
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
  focusedContainerColor = Color(0xFFF8FAFC),
  unfocusedContainerColor = Color(0xFFF8FAFC),
  disabledContainerColor = Color(0xFFF8FAFC),
  focusedIndicatorColor = PrimaryBlue.copy(alpha = 0.48f),
  unfocusedIndicatorColor = CardBorder,
  focusedLabelColor = PrimaryBlueDark,
  unfocusedLabelColor = SubtleInk,
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
