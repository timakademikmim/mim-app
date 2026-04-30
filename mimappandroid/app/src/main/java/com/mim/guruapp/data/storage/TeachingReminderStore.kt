package com.mim.guruapp.data.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mim.guruapp.data.model.TeachingReminderSettings
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.datastore.preferences.core.Preferences

private val Context.teachingReminderDataStore by preferencesDataStore(name = "mim_guru_teaching_reminders")

class TeachingReminderStore(private val context: Context) {
  private val json = Json { ignoreUnknownKeys = true }

  private object Keys {
    val Enabled = booleanPreferencesKey("enabled")
    val MinutesBefore = intPreferencesKey("minutes_before")
    val TargetMode = stringPreferencesKey("target_mode")
    val SelectedDistribusiIds = stringPreferencesKey("selected_distribusi_ids")
    val RepeatMode = stringPreferencesKey("repeat_mode")
    val RingtoneUri = stringPreferencesKey("ringtone_uri")
    val RingtoneLabel = stringPreferencesKey("ringtone_label")
    val ScheduledReminderIds = stringPreferencesKey("scheduled_reminder_ids")
    val UpdatedAt = longPreferencesKey("updated_at")
  }

  suspend fun readSettings(): TeachingReminderSettings {
    val prefs = context.teachingReminderDataStore.data.firstSafely()
    val scheduledIds = runCatching {
      json.decodeFromString<List<String>>(prefs[Keys.ScheduledReminderIds] ?: "[]")
    }.getOrDefault(emptyList())
    val selectedDistribusiIds = runCatching {
      json.decodeFromString<List<String>>(prefs[Keys.SelectedDistribusiIds] ?: "[]")
    }.getOrDefault(emptyList())
    return TeachingReminderSettings(
      enabled = prefs[Keys.Enabled] ?: false,
      minutesBefore = prefs[Keys.MinutesBefore] ?: 10,
      targetMode = prefs[Keys.TargetMode] ?: "all",
      selectedDistribusiIds = selectedDistribusiIds,
      repeatMode = prefs[Keys.RepeatMode] ?: "every_lesson",
      ringtoneUri = prefs[Keys.RingtoneUri] ?: "",
      ringtoneLabel = prefs[Keys.RingtoneLabel] ?: "Nada default sistem",
      scheduledReminderIds = scheduledIds,
      updatedAt = prefs[Keys.UpdatedAt] ?: 0L
    )
  }

  suspend fun saveSettings(settings: TeachingReminderSettings) {
    context.teachingReminderDataStore.edit { prefs ->
      prefs[Keys.Enabled] = settings.enabled
      prefs[Keys.MinutesBefore] = settings.minutesBefore
      prefs[Keys.TargetMode] = settings.targetMode
      prefs[Keys.SelectedDistribusiIds] = json.encodeToString(settings.selectedDistribusiIds.distinct())
      prefs[Keys.RepeatMode] = settings.repeatMode
      prefs[Keys.RingtoneUri] = settings.ringtoneUri
      prefs[Keys.RingtoneLabel] = settings.ringtoneLabel
      prefs[Keys.ScheduledReminderIds] = json.encodeToString(settings.scheduledReminderIds)
      prefs[Keys.UpdatedAt] = settings.updatedAt
    }
  }

  suspend fun updateScheduledReminderIds(ids: List<String>) {
    context.teachingReminderDataStore.edit { prefs ->
      prefs[Keys.ScheduledReminderIds] = json.encodeToString(ids.distinct())
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
