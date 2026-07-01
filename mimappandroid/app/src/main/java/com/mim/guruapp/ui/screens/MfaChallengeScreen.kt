package com.mim.guruapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mim.guruapp.ProfileSaveOutcome
import com.mim.guruapp.ui.components.GlassCard
import com.mim.guruapp.ui.components.GradientLoginButton
import com.mim.guruapp.ui.components.LoginHeader
import com.mim.guruapp.ui.components.LoginTextField
import com.mim.guruapp.ui.components.LogoBackground
import com.mim.guruapp.ui.components.PasswordIcon
import kotlinx.coroutines.launch

@Composable
fun MfaChallengeScreen(
  errorMessage: String,
  isBusy: Boolean,
  onVerify: suspend (String) -> ProfileSaveOutcome,
  onCancel: () -> Unit
) {
  var code by rememberSaveable { mutableStateOf("") }
  var localError by rememberSaveable { mutableStateOf("") }
  val scope = rememberCoroutineScope()

  Box(modifier = Modifier.fillMaxSize()) {
    LogoBackground()
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 32.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      GlassCard(modifier = Modifier.widthIn(max = 460.dp)) {
        LoginHeader(subtitle = "Verifikasi keamanan dua langkah")
        Text(
          text = "Masukkan 6 digit kode dari aplikasi authenticator.",
          style = MaterialTheme.typography.bodyMedium,
          color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f),
          modifier = Modifier.padding(top = 18.dp)
        )
        LoginTextField(
          value = code,
          onValueChange = { value ->
            code = value.filter(Char::isDigit).take(6)
            localError = ""
          },
          label = "Kode Authenticator",
          placeholder = "000000",
          leadingIcon = PasswordIcon,
          modifier = Modifier.padding(top = 16.dp)
        )
        GradientLoginButton(
          text = if (isBusy) "Memverifikasi..." else "Verifikasi",
          enabled = !isBusy && code.length == 6,
          onClick = {
            scope.launch {
              val result = onVerify(code)
              if (!result.success) localError = result.message
            }
          },
          modifier = Modifier.padding(top = 18.dp)
        )
        androidx.compose.material3.TextButton(
          onClick = onCancel,
          enabled = !isBusy,
          modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
          Text("Kembali ke Login")
        }
        val message = localError.ifBlank { errorMessage }
        if (message.isNotBlank()) {
          Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
      }
    }
  }
}
