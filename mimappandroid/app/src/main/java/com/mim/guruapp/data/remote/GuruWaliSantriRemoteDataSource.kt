package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.WaliClassInfo
import com.mim.guruapp.data.model.WaliSantriProfile
import com.mim.guruapp.data.model.WaliSantriSnapshot
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

sealed interface GuruWaliSantriSaveResult {
  data class Success(
    val student: WaliSantriProfile,
    val message: String
  ) : GuruWaliSantriSaveResult
  data class Error(val message: String) : GuruWaliSantriSaveResult
}

class GuruWaliSantriRemoteDataSource {
  suspend fun fetchWaliSantriSnapshot(
    teacherRowId: String,
    teacherKaryawanId: String
  ): WaliSantriSnapshot? = withContext(Dispatchers.IO) {
    val teacherIds = listOf(teacherRowId, teacherKaryawanId)
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
    if (teacherIds.isEmpty()) return@withContext null

    runCatching {
      val activeYearId = resolveActiveTahunAjaranId()
      val kelasRows = teacherIds.firstNotNullOfOrNull { teacherId ->
        val rows = fetchRows(
          table = "kelas",
          query = buildString {
            append("select=id,nama_kelas,wali_kelas_id,tahun_ajaran_id")
            append("&wali_kelas_id=eq.")
            append(encodeValue(teacherId))
            if (activeYearId.isNotBlank()) {
              append("&tahun_ajaran_id=eq.")
              append(encodeValue(activeYearId))
            }
            append("&order=nama_kelas.asc")
          }
        )
        rows.takeIf { it.isNotEmpty() }
      }.orEmpty()

      if (kelasRows.isEmpty()) {
        return@runCatching WaliSantriSnapshot(
          isWaliKelas = false,
          classes = emptyList(),
          students = emptyList(),
          updatedAt = System.currentTimeMillis()
        )
      }

      val classes = kelasRows.mapNotNull { row ->
        val id = row.cleanString("id")
        if (id.isBlank()) return@mapNotNull null
        WaliClassInfo(
          id = id,
          name = row.cleanString("nama_kelas").ifBlank { "Kelas" }
        )
      }
      val classNameById = classes.associate { it.id to it.name }
      val students = fetchWaliStudents(classNameById)

      WaliSantriSnapshot(
        isWaliKelas = true,
        classes = classes,
        students = students,
        updatedAt = System.currentTimeMillis()
      )
    }.getOrNull()
  }

  suspend fun updateWaliSantriProfile(
    draft: WaliSantriProfile
  ): GuruWaliSantriSaveResult = withContext(Dispatchers.IO) {
    val normalizedId = draft.id.trim()
    val normalizedName = draft.name.trim()
    if (normalizedId.isBlank()) {
      return@withContext GuruWaliSantriSaveResult.Error("ID santri tidak ditemukan.")
    }
    if (normalizedName.isBlank()) {
      return@withContext GuruWaliSantriSaveResult.Error("Nama santri wajib diisi.")
    }

    val updateFields = linkedMapOf(
      "nama" to normalizedName,
      "nisn" to draft.nisn.trim(),
      "jenis_kelamin" to draft.gender.trim(),
      "ayah" to draft.fatherName.trim(),
      "ibu" to draft.motherName.trim(),
      "wali" to draft.guardianName.trim(),
      "no_hp_ayah" to draft.fatherPhone.trim(),
      "no_hp_ibu" to draft.motherPhone.trim(),
      "no_hp_wali" to draft.guardianPhone.trim(),
      "no_hp" to draft.studentPhone.trim(),
      "alamat" to draft.address.trim(),
      "catatan" to draft.note.trim()
    )
    val savedFields = linkedMapOf<String, String>()
    val unsupportedFields = mutableSetOf<String>()

    updateFields.forEach { (key, value) ->
      try {
        val requestUrl = buildString {
          append(BuildConfig.SUPABASE_URL)
          append("/rest/v1/santri?id=eq.")
          append(encodeValue(normalizedId))
          append("&select=")
          append(key)
        }
        val connection = createConnection(requestUrl, method = "PATCH").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        val payload = JSONObject().apply {
          put(key, value.ifBlank { JSONObject.NULL })
        }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        val savedValue = connection.useJsonArrayResponse { rows ->
          rows.optJSONObject(0)?.cleanString(key)
        }
        savedFields[key] = savedValue ?: value
      } catch (_: SocketTimeoutException) {
        return@withContext GuruWaliSantriSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
      } catch (error: Exception) {
        val message = error.message.orEmpty()
        val isMissingColumn = message.contains("column", ignoreCase = true) ||
          message.contains("PGRST", ignoreCase = true)
        if (!isMissingColumn) {
          return@withContext GuruWaliSantriSaveResult.Error("Gagal menyimpan data santri ke server.")
        }
        unsupportedFields += key
      }
    }

    if (savedFields.isEmpty()) {
      return@withContext GuruWaliSantriSaveResult.Error("Gagal menyimpan data santri. Kolom update santri belum tersedia atau RLS menolak perubahan.")
    }

    val savedStudent = draft.copy(
      name = savedFields["nama"] ?: draft.name.trim(),
      nisn = savedFields["nisn"] ?: draft.nisn.trim(),
      gender = savedFields["jenis_kelamin"] ?: draft.gender.trim(),
      fatherName = savedFields["ayah"] ?: draft.fatherName.trim(),
      motherName = savedFields["ibu"] ?: draft.motherName.trim(),
      guardianName = savedFields["wali"] ?: draft.guardianName.trim(),
      fatherPhone = savedFields["no_hp_ayah"] ?: draft.fatherPhone.trim(),
      motherPhone = savedFields["no_hp_ibu"] ?: draft.motherPhone.trim(),
      guardianPhone = savedFields["no_hp_wali"] ?: draft.guardianPhone.trim(),
      studentPhone = savedFields["no_hp"] ?: draft.studentPhone.trim(),
      address = savedFields["alamat"] ?: draft.address.trim(),
      note = savedFields["catatan"] ?: draft.note.trim()
    )
    val message = if (unsupportedFields.isEmpty()) {
      "Data santri berhasil disimpan."
    } else {
      "Data santri berhasil disimpan. Beberapa kolom belum tersedia: ${unsupportedFields.joinToString(", ")}."
    }
    GuruWaliSantriSaveResult.Success(savedStudent, message)
  }

  private fun fetchWaliStudents(classNameById: Map<String, String>): List<WaliSantriProfile> {
    if (classNameById.isEmpty()) return emptyList()
    val classIds = classNameById.keys.toList()
    val selectAttempts = listOf(
      "id,nama,kelas_id,aktif,nisn,jenis_kelamin,ayah,ibu,wali,no_hp_ayah,no_hp_ibu,no_hp_wali,no_hp,alamat,catatan",
      "id,nama,kelas_id,aktif,nisn,jenis_kelamin,ayah,ibu,no_hp_ayah,no_hp_ibu,no_hp",
      "id,nama,kelas_id,aktif,nisn,ayah,ibu,no_hp_ayah,no_hp_ibu,no_hp",
      "id,nama,kelas_id,aktif,nisn,ayah,ibu,no_hp_ayah,no_hp_ibu",
      "id,nama,kelas_id,aktif,nisn,ayah,ibu,no_hp",
      "id,nama,kelas_id,aktif,nisn"
    )

    for (select in selectAttempts) {
      try {
        val rows = fetchRows(
          table = "santri",
          query = buildString {
            append("select=")
            append(select)
            append("&kelas_id=in.(")
            append(classIds.joinToString(",") { "\"${encodeValue(it)}\"" })
            append(")&aktif=eq.true&order=nama.asc")
          }
        )
        return rows
          .mapNotNull { row ->
            val id = row.cleanString("id")
            if (id.isBlank()) return@mapNotNull null
            val classId = row.cleanString("kelas_id")
            WaliSantriProfile(
              id = id,
              name = row.cleanString("nama").ifBlank { "Santri" },
              classId = classId,
              className = classNameById[classId].orEmpty().ifBlank { "Kelas" },
              nisn = row.cleanString("nisn"),
              gender = row.cleanString("jenis_kelamin"),
              fatherName = row.cleanString("ayah"),
              fatherPhone = row.cleanString("no_hp_ayah"),
              motherName = row.cleanString("ibu"),
              motherPhone = row.cleanString("no_hp_ibu"),
              guardianName = row.cleanString("wali"),
              guardianPhone = row.cleanString("no_hp_wali"),
              studentPhone = row.cleanString("no_hp"),
              address = row.cleanString("alamat"),
              note = row.cleanString("catatan")
            )
          }
          .sortedWith(compareBy<WaliSantriProfile> { it.className }.thenBy { it.name })
      } catch (error: Exception) {
        val message = error.message.orEmpty()
        val isMissingColumn = message.contains("column", ignoreCase = true) ||
          message.contains("PGRST", ignoreCase = true)
        if (!isMissingColumn && error !is SocketTimeoutException) return emptyList()
      }
    }

    return emptyList()
  }

  private fun resolveActiveTahunAjaranId(): String {
    return runCatching {
      fetchRows(
        table = "tahun_ajaran",
        query = "select=id&aktif=eq.true&limit=1"
      ).firstOrNull()?.cleanString("id").orEmpty()
    }.getOrDefault("")
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = createConnection(requestUrl)
    return connection.useJsonArrayResponse { rows ->
      List(rows.length()) { index -> rows.optJSONObject(index) }.filterNotNull()
    }
  }

  private fun createConnection(requestUrl: String, method: String = "GET"): HttpURLConnection {
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

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
  }
}

private inline fun <T> HttpURLConnection.useJsonArrayResponse(block: (JSONArray) -> T): T {
  return try {
    val code = responseCode
    val payload = readPayload(code in 200..299)
    if (code !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $code" })
    }
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}

private fun JSONObject.cleanString(key: String): String {
  val value = opt(key)
  if (value == null || value == JSONObject.NULL) return ""
  return value.toString().trim().takeUnless { it == "null" }.orEmpty()
}
