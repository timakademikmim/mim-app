package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder

data class AdminEmployee(
  val rowId: String,
  val employeeId: String,
  val name: String,
  val password: String,
  val role: String,
  val phoneNumber: String,
  val address: String,
  val active: Boolean
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
      val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/karyawan" +
        "?select=id,id_karyawan,nama,password,role,no_hp,alamat,aktif&order=aktif.desc&order=nama.asc"
      val rows = createConnection(requestUrl, "GET").useAdminEmployeeJsonArrayResponse { it }
      AdminEmployeeListResult.Success(
        (0 until rows.length())
          .mapNotNull { index -> parseEmployee(rows.optJSONObject(index)) }
      )
    } catch (_: SocketTimeoutException) {
      AdminEmployeeListResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
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
      val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/karyawan" +
        "?select=id,id_karyawan,nama,password,role,no_hp,alamat,aktif&id=eq.${encodeValue(employee.rowId)}"
      val connection = createConnection(requestUrl, "PATCH").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Prefer", "return=representation")
      }
      val payload = JSONObject().apply {
        put("id_karyawan", normalizedId)
        put("nama", normalizedName)
        put("role", normalizedRole)
        val phone = employee.phoneNumber.trim()
        val address = employee.address.trim()
        put("no_hp", if (phone.isBlank()) JSONObject.NULL else phone)
        put("alamat", if (address.isBlank()) JSONObject.NULL else address)
        put("aktif", employee.active)
        val password = newPassword.trim()
        if (password.isNotBlank()) {
          put("password", password)
        }
      }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      val saved = connection.useAdminEmployeeJsonArrayResponse { rows ->
        parseEmployee(rows.optJSONObject(0))
      }
      if (saved != null) {
        AdminEmployeeSaveResult.Success(saved)
      } else {
        AdminEmployeeSaveResult.Error("Data karyawan tidak ditemukan setelah disimpan.")
      }
    } catch (_: SocketTimeoutException) {
      AdminEmployeeSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminEmployeeSaveResult.Error("Gagal menyimpan data karyawan.")
    }
  }

  private fun createConnection(requestUrl: String, method: String): HttpURLConnection {
    return (URL(requestUrl).openConnection() as HttpURLConnection).apply {
      requestMethod = method
      connectTimeout = 15_000
      readTimeout = 15_000
      setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
      setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
      setRequestProperty("Accept", "application/json")
      setRequestProperty("Accept-Charset", "UTF-8")
    }
  }

  private fun parseEmployee(item: JSONObject?): AdminEmployee? {
    item ?: return null
    return AdminEmployee(
      rowId = item.optCleanString("id"),
      employeeId = item.optCleanString("id_karyawan"),
      name = item.optCleanString("nama"),
      password = item.optCleanString("password"),
      role = item.optCleanString("role"),
      phoneNumber = item.optCleanString("no_hp"),
      address = item.optCleanString("alamat"),
      active = item.optBooleanFlexible("aktif")
    )
  }

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
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

private inline fun <T> HttpURLConnection.useAdminEmployeeJsonArrayResponse(
  block: (JSONArray) -> T
): T {
  return try {
    val responseCode = responseCode
    val payload = readAdminEmployeePayload(responseCode in 200..299)
    if (responseCode !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $responseCode" })
    }
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readAdminEmployeePayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
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
