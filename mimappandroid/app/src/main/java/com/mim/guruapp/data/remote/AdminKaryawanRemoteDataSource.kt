package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

data class AdminEmployee(
  val rowId: String,
  val employeeId: String,
  val name: String,
  val password: String,
  val role: String,
  val phoneNumber: String,
  val address: String,
  val active: Boolean,
  val authLinked: Boolean = false,
  val mustChangePassword: Boolean = false
)

sealed interface AdminEmployeeListResult {
  data class Success(val employees: List<AdminEmployee>) : AdminEmployeeListResult
  data class Error(val message: String) : AdminEmployeeListResult
}

sealed interface AdminEmployeeSaveResult {
  data class Success(val employee: AdminEmployee) : AdminEmployeeSaveResult
  data class Error(val message: String) : AdminEmployeeSaveResult
}

class AdminKaryawanRemoteDataSource {
  suspend fun fetchEmployees(): AdminEmployeeListResult = withContext(Dispatchers.IO) {
    try {
      val response = postAction(JSONObject().put("action", "list"))
      val rows = response.optJSONArray("employees")
      AdminEmployeeListResult.Success(
        if (rows == null) {
          emptyList()
        } else {
          (0 until rows.length()).mapNotNull { index -> parseEmployee(rows.optJSONObject(index)) }
        }
      )
    } catch (_: SocketTimeoutException) {
      AdminEmployeeListResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (error: TenantAdminHttpException) {
      AdminEmployeeListResult.Error(error.userMessage("Gagal memuat data karyawan."))
    } catch (_: Exception) {
      AdminEmployeeListResult.Error("Gagal memuat data karyawan.")
    }
  }

  suspend fun updateEmployee(
    employee: AdminEmployee,
    newPassword: String
  ): AdminEmployeeSaveResult = withContext(Dispatchers.IO) {
    val normalizedId = employee.employeeId.trim()
    val normalizedName = employee.name.trim()
    val normalizedRole = normalizeRoleForStore(employee.role)
    if (employee.rowId.isBlank()) {
      return@withContext AdminEmployeeSaveResult.Error("ID baris karyawan tidak ditemukan.")
    }
    if (normalizedId.isBlank() || normalizedName.isBlank() || normalizedRole.isBlank()) {
      return@withContext AdminEmployeeSaveResult.Error("ID Karyawan, nama, dan role wajib diisi.")
    }

    try {
      val updateResponse = postAction(
        JSONObject().apply {
          put("action", "update")
          put("employee_id", employee.rowId)
          put("login_id", normalizedId)
          put("name", normalizedName)
          put("roles", normalizedRole)
          putNullableText("phone", employee.phoneNumber)
          putNullableText("address", employee.address)
        }
      )
      var saved = parseEmployee(updateResponse.optJSONObject("employee"))
        ?: throw IllegalStateException("Data karyawan tidak ditemukan setelah disimpan.")

      if (saved.active != employee.active) {
        val activeResponse = postAction(
          JSONObject().apply {
            put("action", "set_active")
            put("employee_id", employee.rowId)
            put("active", employee.active)
          }
        )
        saved = parseEmployee(activeResponse.optJSONObject("employee")) ?: saved.copy(active = employee.active)
      }

      if (newPassword.isNotBlank()) {
        val passwordPayload = JSONObject().apply {
          put("action", "reset_password")
          put("employee_id", employee.rowId)
          put("password", newPassword)
        }
        try {
          postAction(passwordPayload)
          saved = saved.copy(authLinked = true, mustChangePassword = true)
        } catch (error: TenantAdminHttpException) {
          if (error.statusCode != HttpURLConnection.HTTP_CONFLICT) throw error
          passwordPayload.put("action", "migrate_auth")
          val migrationResponse = postAction(passwordPayload)
          saved = parseEmployee(migrationResponse.optJSONObject("employee"))
            ?: saved.copy(authLinked = true, mustChangePassword = true)
        }
      }

      AdminEmployeeSaveResult.Success(saved.copy(password = ""))
    } catch (_: SocketTimeoutException) {
      AdminEmployeeSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (error: TenantAdminHttpException) {
      AdminEmployeeSaveResult.Error(error.userMessage("Gagal menyimpan data karyawan."))
    } catch (error: Exception) {
      AdminEmployeeSaveResult.Error(error.message?.takeIf { it.isNotBlank() } ?: "Gagal menyimpan data karyawan.")
    }
  }

  private fun postAction(payload: JSONObject): JSONObject {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/functions/v1/manage-tenant-user"
    val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      connectTimeout = 20_000
      readTimeout = 20_000
      doOutput = true
      applySupabaseRequestHeaders()
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Accept", "application/json")
      setRequestProperty("Accept-Charset", "UTF-8")
    }
    connection.outputStream.use { stream ->
      stream.write(payload.toString().toByteArray(Charsets.UTF_8))
      stream.flush()
    }
    return connection.useTenantAdminJsonResponse()
  }

  private fun parseEmployee(item: JSONObject?): AdminEmployee? {
    item ?: return null
    return AdminEmployee(
      rowId = item.optCleanString("id"),
      employeeId = item.optCleanString("id_karyawan"),
      name = item.optCleanString("nama"),
      password = "",
      role = item.optCleanString("role"),
      phoneNumber = item.optCleanString("no_hp"),
      address = item.optCleanString("alamat"),
      active = item.optBooleanFlexible("aktif"),
      authLinked = item.optCleanString("auth_user_id").isNotBlank(),
      mustChangePassword = item.optBooleanFlexible("must_change_password")
    )
  }

  private fun normalizeRoleForStore(rawRole: String): String {
    return rawRole
      .split(',', '|', ';')
      .map { it.trim().lowercase() }
      .filter { it.isNotBlank() }
      .distinct()
      .joinToString(",")
  }
}

private class TenantAdminHttpException(
  val statusCode: Int,
  private val payload: String
) : Exception(payload) {
  fun userMessage(fallback: String): String {
    val message = runCatching { JSONObject(payload).optCleanString("error") }.getOrDefault("")
    return message.ifBlank { fallback }
  }
}

private fun HttpURLConnection.useTenantAdminJsonResponse(): JSONObject {
  return try {
    val status = responseCode
    val payload = readTenantAdminPayload(status in 200..299)
    if (status !in 200..299) throw TenantAdminHttpException(status, payload)
    JSONObject(payload.ifBlank { "{}" })
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readTenantAdminPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}

private fun JSONObject.putNullableText(key: String, value: String) {
  val normalized = value.trim()
  put(key, if (normalized.isBlank()) JSONObject.NULL else normalized)
}

private fun JSONObject.optBooleanFlexible(key: String): Boolean {
  val value = opt(key)
  if (value == true || value == 1) return true
  val text = value?.toString().orEmpty().trim().lowercase()
  return text == "true" || text == "t" || text == "1" || text == "yes" || text == "aktif"
}

private fun JSONObject.optCleanString(key: String): String {
  val value = opt(key)
  if (value == null || value == JSONObject.NULL) return ""
  val text = value.toString().trim()
  return if (text.equals("null", ignoreCase = true)) "" else text
}
