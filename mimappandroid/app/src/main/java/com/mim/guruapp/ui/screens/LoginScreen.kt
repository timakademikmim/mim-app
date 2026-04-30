package com.mim.guruapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mim.guruapp.ui.components.GlassCard
import com.mim.guruapp.ui.components.GradientLoginButton
import com.mim.guruapp.ui.components.LoginHeader
import com.mim.guruapp.ui.components.LoginTextField
import com.mim.guruapp.ui.components.LogoBackground
import com.mim.guruapp.ui.components.PasswordIcon
import com.mim.guruapp.ui.components.RememberMeRow
import com.mim.guruapp.ui.components.UsernameIcon
import com.mim.guruapp.ui.theme.MimGuruTheme

@Composable
fun LoginScreen(
  teacherName: String,
  password: String,
  errorMessage: String,
  isBusy: Boolean,
  onTeacherNameChange: (String) -> Unit,
  onPasswordChange: (String) -> Unit,
  onLoginClick: () -> Unit,
  onUseDemoAccount: () -> Unit
) {
  var rememberMe by rememberSaveable { mutableStateOf(true) }
  var passwordVisible by rememberSaveable { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize()) {
    LogoBackground()

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 32.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      GlassCard(
        modifier = Modifier.widthIn(max = 460.dp)
      ) {
        LoginHeader(
          subtitle = "Silahkan login untuk melanjutkan"
        )

        LoginTextField(
          value = teacherName,
          onValueChange = onTeacherNameChange,
          label = "ID Karyawan",
          placeholder = "Masukkan ID Karyawan",
          leadingIcon = UsernameIcon,
          modifier = Modifier.padding(top = 24.dp)
        )

        LoginTextField(
          value = password,
          onValueChange = onPasswordChange,
          label = "Password",
          placeholder = "Masukkan password",
          leadingIcon = PasswordIcon,
          isPassword = true,
          passwordVisible = passwordVisible,
          onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
          modifier = Modifier.padding(top = 16.dp)
        )

        RememberMeRow(
          checked = rememberMe,
          onCheckedChange = { rememberMe = it },
          onUseDemoAccount = onUseDemoAccount
        )

        GradientLoginButton(
          text = if (isBusy) "Memeriksa akun..." else "Login",
          enabled = !isBusy,
          onClick = onLoginClick,
          modifier = Modifier.padding(top = 18.dp)
        )

        if (errorMessage.isNotBlank()) {
          androidx.compose.material3.Text(
            text = errorMessage,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 12.dp)
          )
        }
      }
    }
  }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun LoginScreenPreview() {
  var teacherName by remember { mutableStateOf("khaerurrahmat") }
  var password by remember { mutableStateOf("") }

  MimGuruTheme {
    LoginScreen(
      teacherName = teacherName,
      password = password,
      errorMessage = "",
      isBusy = false,
      onTeacherNameChange = { teacherName = it },
      onPasswordChange = { password = it },
      onLoginClick = {},
      onUseDemoAccount = {
        teacherName = "khaerurrahmat"
        password = ""
      }
    )
  }
}
