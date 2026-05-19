package com.mim.guruapp.data.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mim.guruapp.data.model.LessonNotificationSettings
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.lessonNotificationDataStore by preferencesDataStore(name = "mim_guru_lesson_notifications")

class LessonNotificationStore(private val context: Context) {
  private val json = Json { ignoreUnknownKeys = true }

  private object Keys {
    val Enabled = booleanPreferencesKey("enabled")
    val MinutesBefore = intPreferencesKey("minutes_before")
    val ScheduledNotificationIds = stringPreferencesKey("scheduled_notification_ids")
    val UpdatedAt = longPreferencesKey("updated_at")
  }

  suspend fun readSettings(): LessonNotificationSettings {
    val prefs = context.lessonNotificationDataStore.data.firstSafely()
    val scheduledIds = runCatching {
      json.decodeFromString<List<String>>(prefs[Keys.ScheduledNotificationIds] ?: "[]")
    }.getOrDefault(emptyList())
    return LessonNotificationSettings(
      enabled = prefs[Keys.Enabled] ?: true,
      minutesBefore = prefs[Keys.MinutesBefore] ?: 0,
      scheduledNotificationIds = scheduledIds,
      updatedAt = prefs[Keys.UpdatedAt] ?: 0L
    )
  }

  suspend fun saveSettings(settings: LessonNotificationSettings) {
    context.lessonNotificationDataStore.edit { prefs ->
      prefs[Keys.Enabled] = settings.enabled
      prefs[Keys.MinutesBefore] = settings.minutesBefore.coerceAtLeast(0)
      prefs[Keys.ScheduledNotificationIds] = json.encodeToString(settings.scheduledNotificationIds.distinct())
      prefs[Keys.UpdatedAt] = settings.updatedAt
    }
  }

  suspend fun updateScheduledNotificationIds(ids: List<String>) {
    context.lessonNotificationDataStore.edit { prefs ->
      prefs[Keys.ScheduledNotificationIds] = json.encodeToString(ids.distinct())
      prefs[Keys.UpdatedAt] = System.currentTimeMillis()
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
