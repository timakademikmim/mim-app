package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.atan2

enum class GuruLocationAttendanceAction {
  DATANG,
  PULANG
}

data class GuruLocationAttendanceConfig(
  val id: String,
  val name: String,
  val address: String,
  val latitude: Double,
  val longitude: Double,
  val radiusMeters: Int,
  val maxAccuracyMeters: Int
)

data class GuruLocationAttendanceRecord(
  val id: String,
  val teacherRowId: String,
  val dateIso: String,
  val datangAt: String,
  val datangDistanceMeters: Double?,
  val datangAccuracyMeters: Double?,
  val pulangAt: String,
  val pulangDistanceMeters: Double?,
  val pulangAccuracyMeters: Double?,
  val status: String,
  val updatedAt: String
)

data class GuruLocationAttendanceSnapshot(
  val teacherRowId: String = "",
  val config: GuruLocationAttendanceConfig? = null,
  val todayRecord: GuruLocationAttendanceRecord? = null,
  val history: List<GuruLocationAttendanceRecord> = emptyList(),
  val errorMessage: String = "",
  val updatedAt: Long = 0L
)

data class GuruLocationAttendanceSubmission(
  val action: GuruLocationAttendanceAction,
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Double,
  val mockDetected: Boolean
)

sealed interface GuruLocationAttendanceSaveResult {
  data class Success(
    val snapshot: GuruLocationAttendanceSnapshot,
    val message: String
  ) : GuruLocationAttendanceSaveResult

  data class Error(val message: String) : GuruLocationAttendanceSaveResult
}

class GuruLocationAttendanceRemoteDataSource {
  suspend fun fetchSnapshot(
    tenantId: String,
    teacherRowId: String,
    teacherKaryawanId: String
  ): GuruLocationAttendanceSnapshot = withContext(Dispatchers.IO) {
    runCatching {
      val normalizedTenantId = tenantId.trim()
      if (normalizedTenantId.isBlank()) {
        return@runCatching GuruLocationAttendanceSnapshot(
          errorMessage = "Unit/sekolah aktif belum terbaca. Silakan login ulang.",
          updatedAt = System.currentTimeMillis()
        )
      }

      val guruId = teacherRowId.trim().ifBlank {
        resolveTeacherRowId(
          tenantId = normalizedTenantId,
          teacherKaryawanId = teacherKaryawanId
        )
      }
      if (guruId.isBlank()) {
        return@runCatching GuruLocationAttendanceSnapshot(
          errorMessage = "ID guru belum tersedia.",
          updatedAt = System.currentTimeMillis()
        )
      }

      val config = fetchActiveConfig(normalizedTenantId)
      if (config == null) {
        return@runCatching GuruLocationAttendanceSnapshot(
          teacherRowId = guruId,
          errorMessage = "Pengaturan lokasi absensi guru belum dibuat.",
          updatedAt = System.currentTimeMillis()
        )
      }

      val todayIso = LocalDate.now().toString()
      val todayRecord = fetchAttendanceRows(
        tenantId = normalizedTenantId,
        guruId = guruId,
        querySuffix = "&tanggal=eq.${encodeValue(todayIso)}&limit=1"
      ).firstOrNull()
      val history = fetchAttendanceRows(
        tenantId = normalizedTenantId,
        guruId = guruId,
        querySuffix = "&order=tanggal.desc&limit=14"
      )

      GuruLocationAttendanceSnapshot(
        teacherRowId = guruId,
        config = config,
        todayRecord = todayRecord,
        history = history,
        updatedAt = System.currentTimeMillis()
      )
    }.getOrElse { error ->
      GuruLocationAttendanceSnapshot(
        errorMessage = resolveErrorMessage(error, "Gagal memuat absensi guru."),
        updatedAt = System.currentTimeMillis()
      )
    }
  }

  suspend fun submitAttendance(
    tenantId: String,
    teacherRowId: String,
    teacherKaryawanId: String,
    submission: GuruLocationAttendanceSubmission
  ): GuruLocationAttendanceSaveResult = withContext(Dispatchers.IO) {
    val normalizedTenantId = tenantId.trim()
    if (normalizedTenantId.isBlank()) {
      return@withContext GuruLocationAttendanceSaveResult.Error("Unit/sekolah aktif belum terbaca. Silakan login ulang.")
    }

    val guruId = teacherRowId.trim().ifBlank {
      resolveTeacherRowId(
        tenantId = normalizedTenantId,
        teacherKaryawanId = teacherKaryawanId
      )
    }
    if (guruId.isBlank()) {
      return@withContext GuruLocationAttendanceSaveResult.Error("ID guru belum tersedia.")
    }

    val config = fetchActiveConfig(normalizedTenantId)
      ?: return@withContext GuruLocationAttendanceSaveResult.Error("Pengaturan lokasi absensi guru belum dibuat.")

    if (submission.mockDetected) {
      return@withContext GuruLocationAttendanceSaveResult.Error("Lokasi perangkat terdeteksi sebagai lokasi tiruan.")
    }
    if (!submission.accuracyMeters.isFinite() || submission.accuracyMeters <= 0.0) {
      return@withContext GuruLocationAttendanceSaveResult.Error("Lokasi belum terbaca dengan baik. Coba cek ulang.")
    }
    if (submission.accuracyMeters > config.maxAccuracyMeters) {
      return@withContext GuruLocationAttendanceSaveResult.Error("Lokasi belum terbaca dengan baik. Coba cek ulang.")
    }

    val distanceMeters = distanceMeters(
      startLatitude = config.latitude,
      startLongitude = config.longitude,
      endLatitude = submission.latitude,
      endLongitude = submission.longitude
    )
    if (distanceMeters > config.radiusMeters) {
      return@withContext GuruLocationAttendanceSaveResult.Error("Anda belum berada di area absensi.")
    }

    runCatching {
      val todayIso = LocalDate.now().toString()
      val existingRecord = fetchAttendanceRows(
        tenantId = normalizedTenantId,
        guruId = guruId,
        querySuffix = "&tanggal=eq.${encodeValue(todayIso)}&limit=1"
      ).firstOrNull()

      when (submission.action) {
        GuruLocationAttendanceAction.DATANG -> {
          if (existingRecord?.datangAt?.isNotBlank() == true) {
            return@runCatching GuruLocationAttendanceSaveResult.Error("Absen datang hari ini sudah tercatat.")
          }
        }

        GuruLocationAttendanceAction.PULANG -> {
          if (existingRecord?.datangAt.isNullOrBlank()) {
            return@runCatching GuruLocationAttendanceSaveResult.Error("Absen datang dulu sebelum absen pulang.")
          }
          if (existingRecord?.pulangAt?.isNotBlank() == true) {
            return@runCatching GuruLocationAttendanceSaveResult.Error("Absen pulang hari ini sudah tercatat.")
          }
        }
      }

      val payload = buildAttendancePayload(
        tenantId = normalizedTenantId,
        guruId = guruId,
        dateIso = todayIso,
        submission = submission,
        distanceMeters = distanceMeters,
        existingRecord = existingRecord
      )
      if (existingRecord == null) {
        insertAttendance(payload)
      } else {
        updateAttendance(existingRecord.id, normalizedTenantId, guruId, payload)
      }

      val snapshot = fetchSnapshot(
        tenantId = normalizedTenantId,
        teacherRowId = guruId,
        teacherKaryawanId = teacherKaryawanId
      )
      GuruLocationAttendanceSaveResult.Success(
        snapshot = snapshot,
        message = if (submission.action == GuruLocationAttendanceAction.DATANG) {
          "Absen datang berhasil disimpan."
        } else {
          "Absen pulang berhasil disimpan."
        }
      )
    }.getOrElse { error ->
      GuruLocationAttendanceSaveResult.Error(resolveErrorMessage(error, "Gagal menyimpan absensi guru."))
    }
  }

  private fun buildAttendancePayload(
    tenantId: String,
    guruId: String,
    dateIso: String,
    submission: GuruLocationAttendanceSubmission,
    distanceMeters: Double,
    existingRecord: GuruLocationAttendanceRecord?
  ): JSONObject {
    val nowIso = Instant.now().toString()
    return JSONObject().apply {
      put("tenant_id", tenantId)
      put("guru_id", guruId)
      put("tanggal", dateIso)
      put("status", "tidak_lengkap")
      when (submission.action) {
        GuruLocationAttendanceAction.DATANG -> {
          put("datang_at", nowIso)
          put("datang_latitude", submission.latitude)
          put("datang_longitude", submission.longitude)
          put("datang_accuracy_meter", submission.accuracyMeters)
          put("datang_distance_meter", distanceMeters)
          put("datang_valid", true)
          put("datang_mock_detected", submission.mockDetected)
        }

        GuruLocationAttendanceAction.PULANG -> {
          put("pulang_at", nowIso)
          put("pulang_latitude", submission.latitude)
          put("pulang_longitude", submission.longitude)
          put("pulang_accuracy_meter", submission.accuracyMeters)
          put("pulang_distance_meter", distanceMeters)
          put("pulang_valid", true)
          put("pulang_mock_detected", submission.mockDetected)
          if (existingRecord?.datangAt?.isNotBlank() == true) {
            put("status", "hadir")
          }
        }
      }
    }
  }

  private fun fetchActiveConfig(tenantId: String): GuruLocationAttendanceConfig? {
    val rows = fetchRows(
      table = "pengaturan_absensi_lokasi",
      query = buildString {
        append("select=id,nama_lokasi,alamat_lokasi,latitude,longitude,radius_meter,max_accuracy_meter")
        append("&tenant_id=eq.")
        append(encodeValue(tenantId))
        append("&active=eq.true")
        append("&order=updated_at.desc")
        append("&limit=1")
      }
    )
    return rows.firstOrNull()?.let(::parseConfig)
  }

  private fun fetchAttendanceRows(
    tenantId: String,
    guruId: String,
    querySuffix: String
  ): List<GuruLocationAttendanceRecord> {
    return fetchRows(
      table = "absensi_karyawan_lokasi",
      query = buildString {
        append("select=id,guru_id,tanggal,datang_at,datang_distance_meter,datang_accuracy_meter,pulang_at,pulang_distance_meter,pulang_accuracy_meter,status,updated_at")
        append("&tenant_id=eq.")
        append(encodeValue(tenantId))
        append("&guru_id=eq.")
        append(encodeValue(guruId))
        append(querySuffix)
      }
    ).mapNotNull(::parseRecord)
  }

  private fun insertAttendance(payload: JSONObject) {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/absensi_karyawan_lokasi?select=id"
    val connection = createConnection(requestUrl, method = "POST").apply {
      doOutput = true
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Prefer", "return=representation")
    }
    connection.outputStream.use { stream ->
      stream.write(JSONArray().put(payload).toString().toByteArray(Charsets.UTF_8))
      stream.flush()
    }
    connection.useJsonArrayResponse { }
  }

  private fun updateAttendance(
    recordId: String,
    tenantId: String,
    guruId: String,
    payload: JSONObject
  ) {
    val requestUrl = buildString {
      append(BuildConfig.SUPABASE_URL)
      append("/rest/v1/absensi_karyawan_lokasi?id=eq.")
      append(encodeValue(recordId))
      append("&tenant_id=eq.")
      append(encodeValue(tenantId))
      append("&guru_id=eq.")
      append(encodeValue(guruId))
      append("&select=id")
    }
    val connection = createConnection(requestUrl, method = "PATCH").apply {
      doOutput = true
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Prefer", "return=representation")
    }
    connection.outputStream.use { stream ->
      stream.write(payload.toString().toByteArray(Charsets.UTF_8))
      stream.flush()
    }
    connection.useJsonArrayResponse { }
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = createConnection(requestUrl)
    return connection.useJsonArrayResponse {
      List(it.length()) { index -> it.optJSONObject(index) }.filterNotNull()
    }
  }

  private fun createConnection(requestUrl: String, method: String = "GET"): HttpURLConnection {
    return (URL(requestUrl).openConnection() as HttpURLConnection).apply {
      requestMethod = method
      connectTimeout = 15_000
      readTimeout = 15_000
      applySupabaseRequestHeaders()
      setRequestProperty("Accept", "application/json")
      setRequestProperty("Accept-Charset", "UTF-8")
    }
  }

  private fun resolveTeacherRowId(
    tenantId: String,
    teacherKaryawanId: String
  ): String {
    if (teacherKaryawanId.isBlank()) return ""
    return runCatching {
      fetchRows(
        table = "karyawan",
        query = buildString {
          append("select=id")
          append("&tenant_id=eq.")
          append(encodeValue(tenantId))
          append("&id_karyawan=eq.")
          append(encodeValue(teacherKaryawanId))
          append("&limit=1")
        }
      ).firstOrNull()?.cleanString("id").orEmpty()
    }.getOrDefault("")
  }

  private fun parseConfig(row: JSONObject): GuruLocationAttendanceConfig? {
    val id = row.cleanString("id")
    val latitude = row.optNullableDouble("latitude") ?: return null
    val longitude = row.optNullableDouble("longitude") ?: return null
    if (id.isBlank()) return null
    return GuruLocationAttendanceConfig(
      id = id,
      name = row.cleanString("nama_lokasi").ifBlank { "Sekolah" },
      address = row.cleanString("alamat_lokasi"),
      latitude = latitude,
      longitude = longitude,
      radiusMeters = row.optInt("radius_meter").takeIf { it > 0 } ?: 100,
      maxAccuracyMeters = row.optInt("max_accuracy_meter").takeIf { it > 0 } ?: 80
    )
  }

  private fun parseRecord(row: JSONObject): GuruLocationAttendanceRecord? {
    val id = row.cleanString("id")
    if (id.isBlank()) return null
    return GuruLocationAttendanceRecord(
      id = id,
      teacherRowId = row.cleanString("guru_id"),
      dateIso = row.cleanString("tanggal").take(10),
      datangAt = row.cleanString("datang_at"),
      datangDistanceMeters = row.optNullableDouble("datang_distance_meter"),
      datangAccuracyMeters = row.optNullableDouble("datang_accuracy_meter"),
      pulangAt = row.cleanString("pulang_at"),
      pulangDistanceMeters = row.optNullableDouble("pulang_distance_meter"),
      pulangAccuracyMeters = row.optNullableDouble("pulang_accuracy_meter"),
      status = row.cleanString("status").ifBlank { "tidak_lengkap" },
      updatedAt = row.cleanString("updated_at")
    )
  }

  private fun distanceMeters(
    startLatitude: Double,
    startLongitude: Double,
    endLatitude: Double,
    endLongitude: Double
  ): Double {
    val earthRadiusMeters = 6_371_000.0
    val startLatRad = Math.toRadians(startLatitude)
    val endLatRad = Math.toRadians(endLatitude)
    val deltaLat = Math.toRadians(endLatitude - startLatitude)
    val deltaLon = Math.toRadians(endLongitude - startLongitude)
    val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
      cos(startLatRad) * cos(endLatRad) *
      sin(deltaLon / 2) * sin(deltaLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusMeters * c
  }

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value.trim(), Charsets.UTF_8.name())
  }

  private fun resolveErrorMessage(error: Throwable, fallback: String): String {
    val raw = error.message.orEmpty().trim()
    if (raw.isBlank()) return fallback
    return runCatching {
      val json = JSONObject(raw)
      json.optString("message").ifBlank { raw }
    }.getOrDefault(raw)
  }
}

private fun JSONObject.cleanString(key: String): String {
  if (!has(key) || isNull(key)) return ""
  return optString(key).trim().takeUnless { it.equals("null", ignoreCase = true) }.orEmpty()
}

private fun JSONObject.optNullableDouble(key: String): Double? {
  if (!has(key) || isNull(key)) return null
  val value = optDouble(key, Double.NaN)
  return value.takeIf { it.isFinite() }
}

private inline fun <T> HttpURLConnection.useJsonArrayResponse(block: (JSONArray) -> T): T {
  return try {
    val code = responseCode
    val payload = inputStreamOrError(code in 200..299).bufferedReader().use(BufferedReader::readText)
    if (code !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $code" })
    }
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.inputStreamOrError(success: Boolean) =
  if (success) inputStream else errorStream ?: inputStream
