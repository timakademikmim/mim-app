package com.mim.guruapp.data.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.bottomNavShortcutDataStore by preferencesDataStore(name = "mim_guru_bottom_nav_shortcuts")

class BottomNavShortcutStore(private val context: Context) {
  private val json = Json { ignoreUnknownKeys = true }

  private object Keys {
    val Destinations = stringPreferencesKey("destinations")
  }

  suspend fun readDestinations(defaults: List<String>): List<String> {
    val prefs = context.bottomNavShortcutDataStore.data.firstSafely()
    val stored = prefs[Keys.Destinations].orEmpty()
    return runCatching {
      json.decodeFromString<List<String>>(stored)
    }.getOrDefault(emptyList()).ifEmpty { defaults }
  }

  suspend fun saveDestinations(destinations: List<String>) {
    context.bottomNavShortcutDataStore.edit { prefs ->
      prefs[Keys.Destinations] = json.encodeToString(destinations.distinct())
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
