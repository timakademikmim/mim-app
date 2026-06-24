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

data class AdminSchoolProfile(
  val id: String = "",
  val schoolName: String = "",
  val schoolAddress: String = "",
  val principalName: String = "",
  val wakasekAkademikName: String = "",
  val wakasekKetahfizanName: String = "",
  val wakasekKesantrianName: String = ""
)

data class AdminSchoolEmployeeOption(
  val id: String,
  val name: String,
  val role: String,
  val active: Boolean
)

data class AdminSchoolProfileSnapshot(
  val profile: AdminSchoolProfile,
  val employeeOptions: List<AdminSchoolEmployeeOption>,
  val waliKelasItems: List<String>,
  val musyrifItems: List<String>,
  val muhaffizItems: List<String>,
  val statusMessage: String
)

sealed interface AdminSchoolProfileLoadResult {
  data class Success(val snapshot: AdminSchoolProfileSnapshot) : AdminSchoolProfileLoadResult
  data class Error(val message: String) : AdminSchoolProfileLoadResult
}

sealed interface AdminSchoolProfileSaveResult {
  data class Success(val snapshot: AdminSchoolProfileSnapshot, val message: String) : AdminSchoolProfileSaveResult
  data class Error(val message: String) : AdminSchoolProfileSaveResult
}

class AdminSchoolProfileRemoteDataSource {
  suspend fun fetchSnapshot(): AdminSchoolProfileLoadResult = withContext(Dispatchers.IO) {
    try {
      val employees = fetchEmployees()
      val profile = fetchProfileRow() ?: AdminSchoolProfile()
      AdminSchoolProfileLoadResult.Success(
        buildSnapshot(
          profile = profile,
          employees = employees,
          statusMessage = if (profile.id.isBlank()) {
            "Belum ada data. Isi form lalu tekan Simpan."
          } else {
            ""
          }
        )
      )
    } catch (_: SocketTimeoutException) {
      AdminSchoolProfileLoadResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (error: Exception) {
      AdminSchoolProfileLoadResult.Error(mapSchoolProfileError(error))
    }
  }

  suspend fun saveProfile(profile: AdminSchoolProfile): AdminSchoolProfileSaveResult = withContext(Dispatchers.IO) {
    try {
      val savedProfile = upsertProfile(profile)
      syncWakasekRoles(savedProfile)
      val employees = fetchEmployees()
      AdminSchoolProfileSaveResult.Success(
        snapshot = buildSnapshot(
          profile = savedProfile,
          employees = employees,
          statusMessage = ""
        ),
        message = "Profil sekolah berhasil disimpan."
      )
    } catch (_: SocketTimeoutException) {
      AdminSchoolProfileSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (error: Exception) {
      AdminSchoolProfileSaveResult.Error(mapSchoolProfileError(error))
    }
  }

  private fun buildSnapshot(
    profile: AdminSchoolProfile,
    employees: List<AdminSchoolEmployeeOption>,
    statusMessage: String
  ): AdminSchoolProfileSnapshot {
    val classes = fetchClassRows()
    val santriRows = fetchSantriRows()
    val employeeById = employees.associateBy { it.id }
    val waliItems = classes.map { item ->
      val waliName = employeeById[item.waliKelasId]?.name?.ifBlank { "-" } ?: "-"
      "${item.className.ifBlank { "-" }}: $waliName"
    }

    val musyrifNames = employees
      .filter { it.active && it.hasRole("musyrif") }
      .map { it.name }
      .filter { it.isNotBlank() }
    val muhaffizNames = employees
      .filter { it.active && it.hasRole("muhaffiz") }
      .map { it.name }
      .filter { it.isNotBlank() }
    val kamarList = santriRows
      .map { it.kamar }
      .filter { it.isNotBlank() }
      .distinct()
      .sortedWith(String.CASE_INSENSITIVE_ORDER)
    val halaqahList = santriRows
      .map { it.halaqah }
      .filter { it.isNotBlank() }
      .distinct()
      .sortedWith(String.CASE_INSENSITIVE_ORDER)

    return AdminSchoolProfileSnapshot(
      profile = profile,
      employeeOptions = employees
        .filter { it.active && it.name.isNotBlank() }
        .sortedBy { it.name.lowercase() },
      waliKelasItems = waliItems,
      musyrifItems = listOf(
        "Daftar Musyrif: ${musyrifNames.joinToString(", ").ifBlank { "-" }}",
        "Kamar Aktif: ${kamarList.joinToString(", ").ifBlank { "-" }}"
      ),
      muhaffizItems = listOf(
        "Daftar Muhaffiz: ${muhaffizNames.joinToString(", ").ifBlank { "-" }}",
        "Halaqah Aktif: ${halaqahList.joinToString(", ").ifBlank { "-" }}"
      ),
      statusMessage = statusMessage
    )
  }

  private fun fetchProfileRow(): AdminSchoolProfile? {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/struktur_sekolah" +
      "?select=*&order=updated_at.desc&order=created_at.desc&limit=1"
    val rows = createConnection(requestUrl, "GET").useSchoolProfileJsonArrayResponse { it }
    return parseProfile(rows.optJSONObject(0))
  }

  private fun upsertProfile(profile: AdminSchoolProfile): AdminSchoolProfile {
    val payload = JSONObject().apply {
      putNullableString("nama_sekolah", profile.schoolName)
      putNullableString("alamat_sekolah", profile.schoolAddress)
      putNullableString("kepala_sekolah", profile.principalName)
      putNullableString("wakasek_bidang_akademik", profile.wakasekAkademikName)
      putNullableString("wakasek_bidang_ketahfizan", profile.wakasekKetahfizanName)
      putNullableString("wakasek_bidang_kesantrian", profile.wakasekKesantrianName)
      put("updated_at", java.time.Instant.now().toString())
    }
    val requestUrl = if (profile.id.isBlank()) {
      "${BuildConfig.SUPABASE_URL}/rest/v1/struktur_sekolah?select=*"
    } else {
      "${BuildConfig.SUPABASE_URL}/rest/v1/struktur_sekolah?select=*&id=eq.${encodeValue(profile.id)}"
    }
    val connection = createConnection(requestUrl, if (profile.id.isBlank()) "POST" else "PATCH").apply {
      doOutput = true
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Prefer", "return=representation")
    }
    val body = if (profile.id.isBlank()) JSONArray().put(payload).toString() else payload.toString()
    connection.outputStream.use { stream ->
      stream.write(body.toByteArray(Charsets.UTF_8))
      stream.flush()
    }
    val saved = connection.useSchoolProfileJsonArrayResponse { rows ->
      parseProfile(rows.optJSONObject(0))
    }
    return saved ?: throw IllegalStateException("Data profil sekolah tidak ditemukan setelah disimpan.")
  }

  private fun fetchEmployees(): List<AdminSchoolEmployeeOption> {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/karyawan" +
      "?select=id,nama,role,aktif&order=nama.asc"
    val rows = createConnection(requestUrl, "GET").useSchoolProfileJsonArrayResponse { it }
    return (0 until rows.length())
      .mapNotNull { index -> parseEmployee(rows.optJSONObject(index)) }
  }

  private fun fetchClassRows(): List<SchoolClassRow> {
    return runCatching {
      val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/kelas" +
        "?select=id,nama_kelas,wali_kelas_id&order=nama_kelas.asc"
      val rows = createConnection(requestUrl, "GET").useSchoolProfileJsonArrayResponse { it }
      (0 until rows.length()).map { index ->
        val item = rows.optJSONObject(index)
        SchoolClassRow(
          className = item.optCleanString("nama_kelas"),
          waliKelasId = item.optCleanString("wali_kelas_id")
        )
      }
    }.getOrDefault(emptyList())
  }

  private fun fetchSantriRows(): List<SchoolSantriRow> {
    val selectAttempts = listOf(
      "id,kamar,halaqah,aktif",
      "id,kamar,halaqah",
      "id,halaqah,aktif",
      "id,halaqah",
      "id,aktif",
      "id"
    )
    for (select in selectAttempts) {
      val result = runCatching {
        val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/santri?select=$select"
        val rows = createConnection(requestUrl, "GET").useSchoolProfileJsonArrayResponse { it }
        (0 until rows.length())
          .map { index -> rows.optJSONObject(index) }
          .filter { item -> !select.contains("aktif") || item.optBooleanFlexible("aktif", default = true) }
          .map { item ->
            SchoolSantriRow(
              kamar = item.optCleanString("kamar"),
              halaqah = item.optCleanString("halaqah")
            )
          }
      }
      if (result.isSuccess) return result.getOrDefault(emptyList())
    }
    return emptyList()
  }

  private fun syncWakasekRoles(profile: AdminSchoolProfile) {
    val config = listOf(
      profile.wakasekAkademikName.trim() to "wakasek akademik",
      profile.wakasekKetahfizanName.trim() to "wakasek ketahfizan",
      profile.wakasekKesantrianName.trim() to "wakasek kesantrian"
    )
    val roleTokens = config.map { it.second }
    val selectedNames = config.map { it.first }.filter { it.isNotBlank() }.toSet()
    val employees = fetchEmployees().filter { it.active && it.name.isNotBlank() }
    employees.forEach { employee ->
      val currentRoles = parseRoleList(employee.role)
      val nextRoles = currentRoles
        .filterNot { roleTokens.contains(it) }
        .toMutableList()
      config.forEach { (name, role) ->
        if (name.isNotBlank() && name == employee.name) nextRoles.add(role)
      }
      val normalizedCurrent = normalizeRolesForStore(currentRoles)
      val normalizedNext = normalizeRolesForStore(nextRoles)
      val shouldUpdate = normalizedCurrent != normalizedNext ||
        roleTokens.any { currentRoles.contains(it) } ||
        selectedNames.contains(employee.name)
      if (shouldUpdate) {
        updateEmployeeRole(employee.id, normalizedNext)
      }
    }
  }

  private fun updateEmployeeRole(employeeId: String, role: String) {
    if (employeeId.isBlank()) return
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/karyawan?id=eq.${encodeValue(employeeId)}"
    val connection = createConnection(requestUrl, "PATCH").apply {
      doOutput = true
      setRequestProperty("Content-Type", "application/json")
    }
    val payload = JSONObject().put("role", role)
    connection.outputStream.use { stream ->
      stream.write(payload.toString().toByteArray(Charsets.UTF_8))
      stream.flush()
    }
    connection.useSchoolProfileStringResponse { }
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

  private fun parseProfile(item: JSONObject?): AdminSchoolProfile? {
    item ?: return null
    return AdminSchoolProfile(
      id = item.optCleanString("id"),
      schoolName = item.optCleanString("nama_sekolah"),
      schoolAddress = item.optCleanString("alamat_sekolah"),
      principalName = item.optCleanString("kepala_sekolah"),
      wakasekAkademikName = item.optCleanString("wakasek_bidang_akademik").ifBlank { item.optCleanString("wakasek_akademik") },
      wakasekKetahfizanName = item.optCleanString("wakasek_bidang_ketahfizan").ifBlank { item.optCleanString("ketahfizan") },
      wakasekKesantrianName = item.optCleanString("wakasek_bidang_kesantrian").ifBlank { item.optCleanString("kesantrian") }
    )
  }

  private fun parseEmployee(item: JSONObject?): AdminSchoolEmployeeOption? {
    item ?: return null
    return AdminSchoolEmployeeOption(
      id = item.optCleanString("id"),
      name = item.optCleanString("nama"),
      role = item.optCleanString("role"),
      active = item.optBooleanFlexible("aktif", default = true)
    )
  }

  private fun mapSchoolProfileError(error: Exception): String {
    val message = error.message.orEmpty()
    return when {
      message.contains("struktur_sekolah", ignoreCase = true) &&
        (message.contains("schema cache", ignoreCase = true) ||
          message.contains("does not exist", ignoreCase = true) ||
          message.contains("42P01", ignoreCase = true)) ->
        "Tabel struktur_sekolah belum ada di Supabase."
      message.contains("wakasek_bidang_", ignoreCase = true) ->
        "Kolom wakasek bidang belum lengkap di tabel struktur_sekolah."
      message.isNotBlank() -> "Gagal memuat profil sekolah: $message"
      else -> "Gagal memuat profil sekolah."
    }
  }

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
  }

  private data class SchoolClassRow(
    val className: String,
    val waliKelasId: String
  )

  private data class SchoolSantriRow(
    val kamar: String,
    val halaqah: String
  )
}

private inline fun <T> HttpURLConnection.useSchoolProfileJsonArrayResponse(
  block: (JSONArray) -> T
): T {
  return useSchoolProfileStringResponse { payload ->
    block(JSONArray(payload.ifBlank { "[]" }))
  }
}

private inline fun <T> HttpURLConnection.useSchoolProfileStringResponse(
  block: (String) -> T
): T {
  return try {
    val responseCode = responseCode
    val payload = readSchoolProfilePayload(responseCode in 200..299)
    if (responseCode !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $responseCode" })
    }
    block(payload)
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readSchoolProfilePayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}

private fun JSONObject.putNullableString(key: String, value: String) {
  val clean = value.trim()
  put(key, if (clean.isBlank()) JSONObject.NULL else clean)
}

private fun JSONObject.optCleanString(key: String): String {
  val value = opt(key)
  if (value == null || value == JSONObject.NULL) return ""
  val text = value.toString().trim()
  return if (text.equals("null", ignoreCase = true)) "" else text
}

private fun JSONObject.optBooleanFlexible(key: String, default: Boolean = false): Boolean {
  val value = opt(key)
  if (value == null || value == JSONObject.NULL) return default
  if (value == true || value == 1) return true
  if (value == false || value == 0) return false
  val text = value.toString().trim().lowercase()
  return when (text) {
    "true", "t", "1", "yes", "aktif" -> true
    "false", "f", "0", "no", "nonaktif" -> false
    else -> default
  }
}

private fun parseRoleList(rawRole: String): List<String> {
  return rawRole
    .split(',', '|', ';')
    .map { it.trim().lowercase() }
    .filter { it.isNotBlank() }
}

private fun normalizeRolesForStore(roles: List<String>): String {
  return roles
    .map { it.trim().lowercase() }
    .filter { it.isNotBlank() }
    .distinct()
    .joinToString(",")
}

private fun AdminSchoolEmployeeOption.hasRole(roleName: String): Boolean {
  return parseRoleList(role).contains(roleName.trim().lowercase())
}
