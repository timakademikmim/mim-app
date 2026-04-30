package com.mim.guruapp.data.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mim.guruapp.data.model.SessionSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.sessionDataStore by preferencesDataStore(name = "mim_guru_session")

class SessionStore(private val context: Context) {
  private val json = Json { ignoreUnknownKeys = true }

  private object Keys {
    val IsLoggedIn = booleanPreferencesKey("is_logged_in")
    val TeacherRowId = stringPreferencesKey("teacher_row_id")
    val TeacherId = stringPreferencesKey("teacher_id")
    val TeacherName = stringPreferencesKey("teacher_name")
    val ActiveRole = stringPreferencesKey("active_role")
    val Roles = stringPreferencesKey("roles")
    val LastLoginAt = longPreferencesKey("last_login_at")
  }

  suspend fun readSession(): SessionSnapshot {
    val prefs = context.sessionDataStore.data.firstSafely()
    val roles = runCatching {
      json.decodeFromString<List<String>>(prefs[Keys.Roles] ?: "[]")
    }.getOrDefault(emptyList())
    return SessionSnapshot(
      isLoggedIn = prefs[Keys.IsLoggedIn] ?: false,
      teacherRowId = prefs[Keys.TeacherRowId] ?: "",
      teacherId = prefs[Keys.TeacherId] ?: "",
      teacherName = prefs[Keys.TeacherName] ?: "",
      activeRole = prefs[Keys.ActiveRole] ?: "",
      roles = roles,
      lastLoginAt = prefs[Keys.LastLoginAt] ?: 0L
    )
  }

  suspend fun saveLogin(
    teacherRowId: String,
    teacherId: String,
    teacherName: String,
    activeRole: String,
    roles: List<String>
  ) {
    context.sessionDataStore.edit { prefs ->
      prefs[Keys.IsLoggedIn] = true
      prefs[Keys.TeacherRowId] = teacherRowId
      prefs[Keys.TeacherId] = teacherId
      prefs[Keys.TeacherName] = teacherName
      prefs[Keys.ActiveRole] = activeRole
      prefs[Keys.Roles] = json.encodeToString(roles)
      prefs[Keys.LastLoginAt] = System.currentTimeMillis()
    }
  }

  suspend fun updateTeacherName(teacherName: String) {
    context.sessionDataStore.edit { prefs ->
      prefs[Keys.TeacherName] = teacherName
    }
  }

  suspend fun clear() {
    context.sessionDataStore.edit { prefs ->
      prefs.clear()
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
