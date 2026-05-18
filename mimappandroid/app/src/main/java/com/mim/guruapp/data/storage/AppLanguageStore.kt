package com.mim.guruapp.data.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.first

private val Context.appLanguageDataStore by preferencesDataStore(name = "mim_guru_app_language")

class AppLanguageStore(private val context: Context) {
  private object Keys {
    val LanguageCode = stringPreferencesKey("language_code")
  }

  suspend fun readLanguageCode(defaultCode: String = "id"): String {
    val prefs = context.appLanguageDataStore.data.firstSafely()
    return prefs[Keys.LanguageCode].orEmpty().ifBlank { defaultCode }
  }

  suspend fun saveLanguageCode(languageCode: String) {
    context.appLanguageDataStore.edit { prefs ->
      prefs[Keys.LanguageCode] = languageCode.trim().lowercase().ifBlank { "id" }
    }
  }
}

private suspend fun kotlinx.coroutines.flow.Flow<Preferences>.firstSafely(): Preferences {
  return try {
    first()
  } catch (_: IOException) {
    emptyPreferences()
  }
}
