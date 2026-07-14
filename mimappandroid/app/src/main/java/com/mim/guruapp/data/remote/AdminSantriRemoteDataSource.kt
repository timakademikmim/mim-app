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
import java.time.LocalDate

data class AdminSantriSnapshot(
  val students: List<AdminSantri>,
  val classes: List<AdminSantriClassOption> = emptyList(),
  val activeAcademicYearId: String = "",
  val activeAcademicYearName: String = "",
  val statusMessage: String = ""
)

data class AdminSantriClassOption(
  val id: String,
  val name: String,
  val academicYearId: String = "",
  val academicYearName: String = "",
  val level: String = ""
)

data class AdminSantri(
  val rowId: String,
  val identityKey: String,
  val historyRowIds: List<String>,
  val name: String,
  val nisn: String,
  val gender: String,
  val classId: String,
  val className: String,
  val classLevel: String,
  val academicYearId: String,
  val academicYearName: String,
  val status: String,
  val active: Boolean,
  val studentPhone: String,
  val fatherName: String,
  val fatherPhone: String,
  val motherName: String,
  val motherPhone: String,
  val guardianName: String,
  val guardianPhone: String,
  val address: String,
  val note: String,
  val room: String,
  val halaqah: String,
  val createdAt: String,
  val updatedAt: String
)

sealed interface AdminSantriLoadResult {
  data class Success(val snapshot: AdminSantriSnapshot) : AdminSantriLoadResult
  data class Error(val message: String) : AdminSantriLoadResult
}

sealed interface AdminSantriSaveResult {
  data class Success(
    val student: AdminSantri,
    val message: String
  ) : AdminSantriSaveResult
  data class Error(val message: String) : AdminSantriSaveResult
}

class AdminSantriRemoteDataSource {
  suspend fun fetchSantri(): AdminSantriLoadResult = withContext(Dispatchers.IO) {
    try {
      val yearRows = fetchRows(
        table = "tahun_ajaran",
        query = "select=*&order=id.desc"
      )
      val activeYear = yearRows.firstOrNull { it.optBooleanFlexible("aktif") }
      val activeYearId = activeYear?.optCleanString("id").orEmpty()
      val yearNameById = yearRows.associate { row ->
        row.optCleanString("id") to row.academicYearName()
      }

      val classRows = fetchRows(
        table = "kelas",
        query = "select=*&order=nama_kelas.asc"
      )
      val classInfos = classRows.mapNotNull { it.toClassInfo() }
      val classById = classInfos.associateBy { it.id }

      val rawRows = fetchRows(
        table = "santri",
        query = "select=*&order=nama.asc"
      )
      val rows = rawRows.mapNotNull { row ->
        row.toRawSantriRow(
          classById = classById,
          yearNameById = yearNameById
        )
      }
      val students = rows
        .groupBy { it.identityKey }
        .mapNotNull { (_, history) ->
          history.pickCurrent(activeYearId)?.toAdminSantri(history)
        }
        .sortedWith(
          compareBy<AdminSantri> { it.className.sortableText() }
            .thenBy { it.name.sortableText() }
        )

      AdminSantriLoadResult.Success(
        AdminSantriSnapshot(
          students = students,
          classes = classInfos
            .map { it.toClassOption(yearNameById) }
            .sortedWith(compareBy<AdminSantriClassOption> { it.name.sortableText() }),
          activeAcademicYearId = activeYearId,
          activeAcademicYearName = activeYear?.academicYearName().orEmpty(),
          statusMessage = if (activeYearId.isBlank()) {
            "Tahun ajaran aktif belum ditemukan. Kelas ditampilkan dari data terbaru santri."
          } else {
            ""
          }
        )
      )
    } catch (_: SocketTimeoutException) {
      AdminSantriLoadResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminSantriLoadResult.Error("Gagal memuat data santri.")
    }
  }

  suspend fun updateSantri(student: AdminSantri): AdminSantriSaveResult = withContext(Dispatchers.IO) {
    val rowId = student.rowId.trim()
    val name = student.name.trim()
    if (rowId.isBlank()) {
      return@withContext AdminSantriSaveResult.Error("ID santri tidak ditemukan.")
    }
    if (name.isBlank()) {
      return@withContext AdminSantriSaveResult.Error("Nama santri wajib diisi.")
    }

    val normalizedStatus = student.status.trim().ifBlank { if (student.active) "aktif" else "tidak_aktif" }
    val fields = linkedMapOf<String, Any?>(
      "nama" to name,
      "nisn" to student.nisn.trim(),
      "jenis_kelamin" to student.gender.trim(),
      "kelas_id" to student.classId.trim(),
      "status" to normalizedStatus,
      "aktif" to normalizedStatus.statusMeansActive(),
      "no_hp" to student.studentPhone.trim(),
      "ayah" to student.fatherName.trim(),
      "no_hp_ayah" to student.fatherPhone.trim(),
      "ibu" to student.motherName.trim(),
      "no_hp_ibu" to student.motherPhone.trim(),
      "wali" to student.guardianName.trim(),
      "no_hp_wali" to student.guardianPhone.trim(),
      "alamat" to student.address.trim(),
      "catatan" to student.note.trim(),
      "kamar" to student.room.trim(),
      "halaqah" to student.halaqah.trim()
    )
    val savedFields = linkedMapOf<String, Any?>()
    val unsupportedFields = mutableSetOf<String>()

    fields.forEach { (field, value) ->
      try {
        val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/santri" +
          "?select=$field&id=eq.${encodeValue(rowId)}"
        val connection = createConnection(requestUrl, method = "PATCH").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        val payload = JSONObject().apply {
          put(field, when {
            value is Boolean -> value
            value?.toString().orEmpty().isBlank() -> JSONObject.NULL
            else -> value
          })
        }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        val savedValue = connection.useAdminSantriJsonArrayResponse { rows ->
          rows.optJSONObject(0)?.opt(field)
        }
        savedFields[field] = savedValue ?: value
      } catch (_: SocketTimeoutException) {
        return@withContext AdminSantriSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
      } catch (error: Exception) {
        val message = error.message.orEmpty()
        val isMissingColumn = message.contains("column", ignoreCase = true) ||
          message.contains("PGRST", ignoreCase = true)
        if (!isMissingColumn) {
          return@withContext AdminSantriSaveResult.Error("Gagal menyimpan data santri.")
        }
        unsupportedFields += field
      }
    }

    if (savedFields.isEmpty()) {
      return@withContext AdminSantriSaveResult.Error("Gagal menyimpan data santri. Kolom update belum tersedia atau RLS menolak perubahan.")
    }

    val saved = student.copy(
      name = savedFields.cleanString("nama", student.name),
      nisn = savedFields.cleanString("nisn", student.nisn),
      gender = savedFields.cleanString("jenis_kelamin", student.gender),
      classId = savedFields.cleanString("kelas_id", student.classId),
      status = savedFields.cleanString("status", normalizedStatus),
      active = savedFields.cleanBoolean("aktif", normalizedStatus.statusMeansActive()),
      studentPhone = savedFields.cleanString("no_hp", student.studentPhone),
      fatherName = savedFields.cleanString("ayah", student.fatherName),
      fatherPhone = savedFields.cleanString("no_hp_ayah", student.fatherPhone),
      motherName = savedFields.cleanString("ibu", student.motherName),
      motherPhone = savedFields.cleanString("no_hp_ibu", student.motherPhone),
      guardianName = savedFields.cleanString("wali", student.guardianName),
      guardianPhone = savedFields.cleanString("no_hp_wali", student.guardianPhone),
      address = savedFields.cleanString("alamat", student.address),
      note = savedFields.cleanString("catatan", student.note),
      room = savedFields.cleanString("kamar", student.room),
      halaqah = savedFields.cleanString("halaqah", student.halaqah)
    )
    val message = if (unsupportedFields.isEmpty()) {
      "Data santri berhasil disimpan."
    } else {
      "Data santri berhasil disimpan. Beberapa kolom belum tersedia: ${unsupportedFields.joinToString(", ")}."
    }
    AdminSantriSaveResult.Success(saved, message)
  }

  suspend fun promoteSantri(student: AdminSantri): AdminSantriSaveResult = withContext(Dispatchers.IO) {
    val rowId = student.rowId.trim()
    val currentClassId = student.classId.trim()
    if (rowId.isBlank()) {
      return@withContext AdminSantriSaveResult.Error("ID santri tidak ditemukan.")
    }
    if (currentClassId.isBlank()) {
      return@withContext AdminSantriSaveResult.Error("Kelas santri saat ini belum ditemukan.")
    }

    try {
      val yearRows = fetchRows("tahun_ajaran", "select=*&order=id.desc")
      val yearNameById = yearRows.associate { row ->
        row.optCleanString("id") to row.academicYearName()
      }
      val currentClass = fetchRows(
        table = "kelas",
        query = "select=*&id=eq.${encodeValue(currentClassId)}&limit=1"
      ).firstOrNull()?.toClassInfo()
        ?: return@withContext AdminSantriSaveResult.Error("Kelas saat ini tidak ditemukan untuk proses naik kelas.")

      val currentLevel = currentClass.level.toLevelNumber()
        ?: return@withContext AdminSantriSaveResult.Error("Tingkat kelas saat ini belum valid.")
      val nextLevel = currentLevel + 1
      if (nextLevel > 12) {
        return@withContext AdminSantriSaveResult.Error("Santri sudah berada di tingkat akhir, tidak bisa naik kelas.")
      }

      val currentYearName = yearNameById[currentClass.academicYearId].orEmpty()
      val currentStartYear = currentYearName.academicYearStart()
        ?: return@withContext AdminSantriSaveResult.Error("Tahun ajaran kelas saat ini belum bisa ditentukan.")
      val nextYear = yearRows.firstOrNull { row ->
        row.academicYearName().academicYearStart() == currentStartYear + 1
      } ?: return@withContext AdminSantriSaveResult.Error("Tahun ajaran berikutnya belum tersedia.")
      val nextYearId = nextYear.optCleanString("id")

      val targetClasses = fetchRows(
        table = "kelas",
        query = buildString {
          append("select=*&tahun_ajaran_id=eq.")
          append(encodeValue(nextYearId))
          append("&tingkat=eq.")
          append(encodeValue(nextLevel.toString()))
          append("&order=nama_kelas.asc")
        }
      ).mapNotNull { it.toClassInfo() }
      if (targetClasses.isEmpty()) {
        return@withContext AdminSantriSaveResult.Error("Belum ada kelas tingkat $nextLevel pada tahun ajaran berikutnya.")
      }

      val suffix = currentClass.name.classSuffix()
      val targetClass = targetClasses.firstOrNull { it.name.classSuffix() == suffix } ?: targetClasses.first()
      val nisn = student.nisn.trim()
      if (nisn.isNotBlank()) {
        val existingNextYear = fetchRows(
          table = "santri",
          query = buildString {
            append("select=id,nisn,kelas_id,kelas!inner(tahun_ajaran_id)")
            append("&nisn=eq.")
            append(encodeValue(nisn))
            append("&kelas.tahun_ajaran_id=eq.")
            append(encodeValue(nextYearId))
            append("&id=neq.")
            append(encodeValue(rowId))
            append("&limit=1")
          }
        )
        if (existingNextYear.isNotEmpty()) {
          return@withContext AdminSantriSaveResult.Error("Data santri untuk tahun ajaran berikutnya sudah ada. Gabungkan atau hapus duplikat lama dulu sebelum naik kelas.")
        }
      }

      val todayIso = LocalDate.now().toString()
      val oldHistorySaved = upsertClassHistoryRow(
        linkedMapOf(
          "santri_id" to rowId,
          "kelas_id" to currentClass.id,
          "tahun_ajaran_id" to currentClass.academicYearId,
          "status" to "naik_kelas",
          "tanggal_selesai" to todayIso
        )
      )

      val savedFields = patchSantriFields(
        rowId,
        linkedMapOf(
          "kelas_id" to targetClass.id,
          "status" to "aktif",
          "aktif" to true
        )
      )
      if (!savedFields.containsKey("kelas_id")) {
        return@withContext AdminSantriSaveResult.Error("Gagal memindahkan santri ke kelas baru. Data santri tidak digandakan.")
      }

      val newHistorySaved = upsertClassHistoryRow(
        linkedMapOf(
          "santri_id" to rowId,
          "kelas_id" to targetClass.id,
          "tahun_ajaran_id" to targetClass.academicYearId,
          "status" to "aktif",
          "tanggal_mulai" to todayIso,
          "tanggal_selesai" to null
        )
      )

      val promotedStudent = student.copy(
        rowId = rowId,
        historyRowIds = student.historyRowIds.ifEmpty { listOf(rowId) },
        classId = targetClass.id,
        className = targetClass.name,
        classLevel = targetClass.level,
        academicYearId = targetClass.academicYearId,
        academicYearName = yearNameById[targetClass.academicYearId].orEmpty(),
        status = savedFields.cleanString("status", "aktif"),
        active = savedFields.cleanBoolean("aktif", true)
      )

      val message = if (oldHistorySaved && newHistorySaved) {
        "Santri berhasil dinaikkan ke ${targetClass.name} tanpa membuat data ganda."
      } else {
        "Santri berhasil dinaikkan ke ${targetClass.name} tanpa membuat data ganda. Riwayat kelas belum tercatat karena tabel riwayat belum tersedia atau belum menerima data."
      }
      AdminSantriSaveResult.Success(promotedStudent, message)
    } catch (_: SocketTimeoutException) {
      AdminSantriSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (error: Exception) {
      val message = error.message.orEmpty()
      if (message.isUniqueViolationMessage()) {
        AdminSantriSaveResult.Error("Gagal naik kelas karena NISN masih unik global di database.")
      } else {
        AdminSantriSaveResult.Error("Gagal memproses naik kelas.")
      }
    }
  }

  suspend fun graduateSantri(student: AdminSantri): AdminSantriSaveResult = withContext(Dispatchers.IO) {
    val rowId = student.rowId.trim()
    if (rowId.isBlank()) {
      return@withContext AdminSantriSaveResult.Error("ID santri tidak ditemukan.")
    }

    try {
      val tahunLulus = student.academicYearName.firstYearText()
      val fields = linkedMapOf<String, Any?>(
        "aktif" to false,
        "status" to "lulus"
      )
      if (tahunLulus.isNotBlank()) fields["tahun_lulus"] = tahunLulus
      val savedFields = patchSantriFields(rowId, fields)
      if (!savedFields.containsKey("aktif") || !savedFields.containsKey("status")) {
        return@withContext AdminSantriSaveResult.Error("Gagal meluluskan santri karena kolom status belum tersimpan.")
      }

      AdminSantriSaveResult.Success(
        student.copy(
          status = "lulus",
          active = false
        ),
        "Santri berhasil diluluskan."
      )
    } catch (_: SocketTimeoutException) {
      AdminSantriSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      AdminSantriSaveResult.Error("Gagal memproses kelulusan santri.")
    }
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = createConnection(requestUrl)
    return connection.useAdminSantriJsonArrayResponse { rows ->
      List(rows.length()) { index -> rows.optJSONObject(index) }.filterNotNull()
    }
  }

  private fun patchSantriFields(rowId: String, fields: Map<String, Any?>): Map<String, Any?> {
    val savedFields = linkedMapOf<String, Any?>()
    fields.forEach { (field, value) ->
      try {
        val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/santri" +
          "?select=$field&id=eq.${encodeValue(rowId)}"
        val connection = createConnection(requestUrl, method = "PATCH").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        val payload = JSONObject().apply { putPayloadValue(field, value) }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        val savedValue = connection.useAdminSantriJsonArrayResponse { rows ->
          rows.optJSONObject(0)?.opt(field)
        }
        savedFields[field] = savedValue ?: value
      } catch (error: Exception) {
        if (!error.message.orEmpty().isMissingColumnMessage(field)) throw error
      }
    }
    return savedFields
  }

  private fun upsertClassHistoryRow(fields: Map<String, Any?>): Boolean {
    return runCatching {
      val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/riwayat_kelas_santri" +
        "?on_conflict=santri_id,kelas_id,tahun_ajaran_id&select=id"
      val connection = createConnection(requestUrl, method = "POST").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
      }
      val payload = JSONObject().apply {
        fields.forEach { (field, value) -> putPayloadValue(field, value) }
      }
      connection.outputStream.use { stream ->
        stream.write(payload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      connection.useAdminSantriJsonArrayResponse { rows -> rows.length() > 0 }
    }.getOrDefault(false)
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

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
  }
}

private data class AdminClassInfo(
  val id: String,
  val name: String,
  val academicYearId: String,
  val level: String
)

private data class RawSantriRow(
  val rowId: String,
  val identityKey: String,
  val name: String,
  val nisn: String,
  val gender: String,
  val classId: String,
  val className: String,
  val classLevel: String,
  val academicYearId: String,
  val academicYearName: String,
  val status: String,
  val active: Boolean,
  val studentPhone: String,
  val fatherName: String,
  val fatherPhone: String,
  val motherName: String,
  val motherPhone: String,
  val guardianName: String,
  val guardianPhone: String,
  val address: String,
  val note: String,
  val room: String,
  val halaqah: String,
  val createdAt: String,
  val updatedAt: String
) {
  fun toAdminSantri(history: List<RawSantriRow>): AdminSantri {
    return AdminSantri(
      rowId = rowId,
      identityKey = identityKey,
      historyRowIds = history.map { it.rowId }.filter { it.isNotBlank() }.distinct(),
      name = name,
      nisn = nisn,
      gender = gender,
      classId = classId,
      className = className,
      classLevel = classLevel,
      academicYearId = academicYearId,
      academicYearName = academicYearName,
      status = status,
      active = active,
      studentPhone = studentPhone,
      fatherName = fatherName,
      fatherPhone = fatherPhone,
      motherName = motherName,
      motherPhone = motherPhone,
      guardianName = guardianName,
      guardianPhone = guardianPhone,
      address = address,
      note = note,
      room = room,
      halaqah = halaqah,
      createdAt = createdAt,
      updatedAt = updatedAt
    )
  }
}

private fun JSONObject.toClassInfo(): AdminClassInfo? {
  val id = optCleanString("id")
  if (id.isBlank()) return null
  return AdminClassInfo(
    id = id,
    name = optCleanString("nama_kelas").ifBlank { optCleanString("kelas") },
    academicYearId = optCleanString("tahun_ajaran_id"),
    level = optCleanString("tingkat")
  )
}

private fun AdminClassInfo.toClassOption(yearNameById: Map<String, String>): AdminSantriClassOption {
  return AdminSantriClassOption(
    id = id,
    name = name,
    academicYearId = academicYearId,
    academicYearName = yearNameById[academicYearId].orEmpty(),
    level = level
  )
}

private fun JSONObject.toRawSantriRow(
  classById: Map<String, AdminClassInfo>,
  yearNameById: Map<String, String>
): RawSantriRow? {
  val rowId = optCleanString("id")
  val name = optCleanString("nama").ifBlank { optCleanString("nama_santri") }
  if (rowId.isBlank() && name.isBlank()) return null

  val nisn = optCleanString("nisn")
  val classId = optCleanString("kelas_id")
  val classInfo = classById[classId]
  val academicYearId = classInfo?.academicYearId.orEmpty().ifBlank { optCleanString("tahun_ajaran_id") }
  val explicitStatus = optCleanString("status")
  val activeFromStatus = when (explicitStatus.lowercase()) {
    "aktif", "active" -> true
    "tidak_aktif", "nonaktif", "inactive", "naik_kelas", "lulus" -> false
    else -> null
  }
  val active = optBooleanFlexibleOrNull("aktif") ?: activeFromStatus ?: true
  val status = explicitStatus.ifBlank {
    if (active) "aktif" else "tidak_aktif"
  }
  val identityKey = buildSantriIdentityKey(
    nisn = nisn,
    idSantri = optCleanString("id_santri"),
    noInduk = optCleanString("no_induk").ifBlank { optCleanString("nomor_induk") },
    rowId = rowId
  )

  return RawSantriRow(
    rowId = rowId,
    identityKey = identityKey,
    name = name.ifBlank { "Santri" },
    nisn = nisn,
    gender = optCleanString("jenis_kelamin"),
    classId = classId,
    className = classInfo?.name.orEmpty(),
    classLevel = classInfo?.level.orEmpty(),
    academicYearId = academicYearId,
    academicYearName = yearNameById[academicYearId].orEmpty(),
    status = status,
    active = active,
    studentPhone = optCleanString("no_hp"),
    fatherName = optCleanString("ayah"),
    fatherPhone = optCleanString("no_hp_ayah"),
    motherName = optCleanString("ibu"),
    motherPhone = optCleanString("no_hp_ibu"),
    guardianName = optCleanString("wali"),
    guardianPhone = optCleanString("no_hp_wali"),
    address = optCleanString("alamat"),
    note = optCleanString("catatan"),
    room = optCleanString("kamar").ifBlank { optCleanString("asrama") },
    halaqah = optCleanString("halaqah"),
    createdAt = optCleanString("created_at"),
    updatedAt = optCleanString("updated_at")
  )
}

private fun List<RawSantriRow>.pickCurrent(activeYearId: String): RawSantriRow? {
  val candidates = if (activeYearId.isBlank()) {
    this
  } else {
    filter { it.academicYearId == activeYearId }.ifEmpty { this }
  }
  return candidates.maxWithOrNull(
    compareBy<RawSantriRow> { it.active }
      .thenBy { it.updatedAt.ifBlank { it.createdAt } }
      .thenBy { it.name }
  )
}

private inline fun <T> HttpURLConnection.useAdminSantriJsonArrayResponse(
  block: (JSONArray) -> T
): T {
  return try {
    val responseCode = responseCode
    val payload = readAdminSantriPayload(responseCode in 200..299)
    if (responseCode !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $responseCode" })
    }
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readAdminSantriPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}

private fun JSONObject.academicYearName(): String {
  return optCleanString("nama")
    .ifBlank { optCleanString("tahun_ajaran") }
    .ifBlank { optCleanString("tahun") }
}

private fun JSONObject.optBooleanFlexible(key: String): Boolean {
  return optBooleanFlexibleOrNull(key) ?: false
}

private fun JSONObject.optBooleanFlexibleOrNull(key: String): Boolean? {
  if (!has(key) || isNull(key)) return null
  val value = opt(key)
  if (value == true || value == 1) return true
  if (value == false || value == 0) return false
  val text = value?.toString().orEmpty().trim().lowercase()
  if (text.isBlank() || text == "null") return null
  return when (text) {
    "true", "t", "1", "yes", "aktif", "active" -> true
    "false", "f", "0", "no", "tidak_aktif", "nonaktif", "inactive" -> false
    else -> null
  }
}

private fun JSONObject.optCleanString(key: String): String {
  val value = opt(key)
  if (value == null || value == JSONObject.NULL) return ""
  val text = value.toString().trim()
  return if (text.equals("null", ignoreCase = true)) "" else text
}

private fun buildSantriIdentityKey(
  nisn: String,
  idSantri: String,
  noInduk: String,
  rowId: String
): String {
  return when {
    nisn.isNotBlank() -> "nisn:${nisn.lowercase()}"
    idSantri.isNotBlank() -> "id_santri:${idSantri.lowercase()}"
    noInduk.isNotBlank() -> "no_induk:${noInduk.lowercase()}"
    else -> "row:${rowId.lowercase()}"
  }
}

private fun String.sortableText(): String {
  return if (isBlank()) "zzzz" else trim().lowercase()
}

private fun String.statusMeansActive(): Boolean {
  return when (trim().lowercase()) {
    "tidak_aktif", "nonaktif", "inactive", "naik_kelas", "lulus" -> false
    else -> true
  }
}

private fun JSONObject.putPayloadValue(key: String, value: Any?) {
  put(
    key,
    when {
      value is Boolean -> value
      value == null -> JSONObject.NULL
      value.toString().isBlank() -> JSONObject.NULL
      else -> value
    }
  )
}

private fun String.toLevelNumber(): Int? {
  val trimmed = trim()
  if (trimmed.isBlank()) return null
  trimmed.toIntOrNull()?.let { return it }
  val digits = trimmed.filter { it.isDigit() }
  return digits.toIntOrNull()
}

private fun String.firstYearText(): String {
  val match = Regex("\\b(19|20)\\d{2}\\b").find(this)
  return match?.value.orEmpty()
}

private fun String.academicYearStart(): Int? {
  val text = trim()
  if (text.isBlank()) return null
  val range = Regex("\\b(19|20)\\d{2}\\s*[/\\-]\\s*(19|20)\\d{2}\\b").find(text)
  if (range != null) return range.value.firstYearText().toIntOrNull()
  return firstYearText().toIntOrNull()
}

private fun String.classSuffix(): String {
  val match = Regex("([A-Za-z]+)\\s*$").find(trim())
  return match?.value.orEmpty().uppercase()
}

private fun String.isMissingColumnMessage(field: String = ""): Boolean {
  val lower = lowercase()
  if (!lower.contains("column") && !lower.contains("pgrst")) return false
  return field.isBlank() || lower.contains(field.lowercase())
}

private fun String.isUniqueViolationMessage(): Boolean {
  val lower = lowercase()
  return lower.contains("23505") ||
    lower.contains("duplicate key") ||
    lower.contains("unique constraint")
}

private fun Map<String, Any?>.cleanString(key: String, fallback: String): String {
  if (!containsKey(key)) return fallback
  val value = this[key]
  if (value == null || value == JSONObject.NULL) return ""
  val text = value.toString().trim()
  return if (text.equals("null", ignoreCase = true)) "" else text
}

private fun Map<String, Any?>.cleanBoolean(key: String, fallback: Boolean): Boolean {
  val value = this[key] ?: return fallback
  if (value == true || value == 1) return true
  if (value == false || value == 0) return false
  return when (value.toString().trim().lowercase()) {
    "true", "t", "1", "yes", "aktif", "active" -> true
    "false", "f", "0", "no", "tidak_aktif", "nonaktif", "inactive" -> false
    else -> fallback
  }
}
