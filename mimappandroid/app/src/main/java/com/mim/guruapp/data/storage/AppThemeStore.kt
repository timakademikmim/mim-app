package com.mim.guruapp.data.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.first

private val Context.appThemeDataStore by preferencesDataStore(name = "mim_guru_app_theme")

class AppThemeStore(private val context: Context) {
  private object Keys {
    val ThemeModeCode = stringPreferencesKey("theme_mode_code")
  }

  suspend fun readThemeModeCode(defaultCode: String = "system"): String {
    val prefs = context.appThemeDataStore.data.firstSafely()
    return prefs[Keys.ThemeModeCode].orEmpty().ifBlank { defaultCode }
  }

  suspend fun saveThemeModeCode(themeModeCode: String) {
    val normalized = themeModeCode.trim().lowercase().let { code ->
      if (code in listOf("system", "light", "dark")) code else "system"
    }
    context.appThemeDataStore.edit { prefs ->
      prefs[Keys.ThemeModeCode] = normalized
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
