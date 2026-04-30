package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.MapelScoreSnapshot
import com.mim.guruapp.data.model.ScoreDetailRow
import com.mim.guruapp.data.model.ScoreStudent
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

sealed interface GuruMapelScoreSaveResult {
  data object Success : GuruMapelScoreSaveResult
  data class Error(val message: String) : GuruMapelScoreSaveResult
}

data class ScoreDraftPayload(
  val rowId: String = "",
  val nilaiTugas: Double? = null,
  val nilaiUlanganHarian: Double? = null,
  val nilaiPts: Double? = null,
  val nilaiPas: Double? = null,
  val nilaiKehadiran: Double? = null,
  val nilaiKeterampilan: Double? = null
)

class GuruMapelScoreRemoteDataSource {
  suspend fun fetchScoreSnapshot(
    distribusiId: String,
    fallbackTitle: String,
    fallbackClassName: String
  ): MapelScoreSnapshot? = withContext(Dispatchers.IO) {
    runCatching {
      val distribusiRow = fetchRows(
        table = "distribusi_mapel",
        query = "select=id,kelas_id,mapel_id,semester_id&id=eq.${encodeValue(distribusiId)}&limit=1"
      ).firstOrNull() ?: return@withContext null

      val kelasId = distribusiRow.optString("kelas_id").trim()
      val mapelId = distribusiRow.optString("mapel_id").trim()
      val semesterId = distribusiRow.optString("semester_id").trim()
      if (kelasId.isBlank() || mapelId.isBlank()) return@withContext null

      val santriRows = fetchRows(
        table = "santri",
        query = buildString {
          append("select=id,nama,kelas_id,aktif")
          append("&kelas_id=eq.")
          append(encodeValue(kelasId))
          append("&aktif=eq.true&order=nama.asc")
        }
      )

      val nilaiRows = fetchRows(
        table = "nilai_akademik",
        query = buildString {
          append("select=id,santri_id,mapel_id,semester_id,nilai_tugas,nilai_ulangan_harian,nilai_pts,nilai_pas,nilai_kehadiran,nilai_akhir,nilai_keterampilan")
          append("&mapel_id=eq.")
          append(encodeValue(mapelId))
          if (semesterId.isNotBlank()) {
            append("&semester_id=eq.")
            append(encodeValue(semesterId))
          }
        }
      )

      val nilaiBySantriId = nilaiRows.associateBy { it.optString("santri_id").trim() }
      val inputRows = mergeScoreInputRows(
        fetchScoreInputRows(
          buildString {
            append("&kelas_id=eq.")
            append(encodeValue(kelasId))
            append("&mapel_id=eq.")
            append(encodeValue(mapelId))
            if (semesterId.isNotBlank()) {
              append("&semester_id=eq.")
              append(encodeValue(semesterId))
            }
            append("&order=tanggal.desc")
          }
        ),
        fetchScoreInputRows(
          buildString {
            append("&distribusi_id=eq.")
            append(encodeValue(distribusiId))
            append("&order=tanggal.desc")
          }
        )
      )
      val detailRowsBySantriMetric = inputRows
        .mapNotNull { row ->
          val santriId = row.optString("santri_id").trim()
          val metricKey = jenisToMetricKey(row.optString("jenis").trim())
          if (santriId.isBlank() || metricKey.isBlank()) {
            null
          } else {
            santriId to (metricKey to ScoreDetailRow(
              id = row.optString("id").trim(),
              dateIso = row.optString("tanggal").take(10),
              value = row.optDoubleOrNull("nilai"),
              material = row.optScoreMaterial("materi")
            ))
          }
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, metricRows) ->
          metricRows
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, rows) -> rows.sortedByDescending { it.dateIso } }
        }

      val students = santriRows.map { santri ->
        val santriId = santri.optString("id").trim()
        val nilai = nilaiBySantriId[santriId]
        ScoreStudent(
          id = santriId,
          name = santri.optString("nama").trim().ifBlank { "-" },
          rowId = nilai?.optString("id")?.trim().orEmpty(),
          nilaiTugas = nilai?.optDoubleOrNull("nilai_tugas"),
          nilaiUlanganHarian = nilai?.optDoubleOrNull("nilai_ulangan_harian"),
          nilaiPts = nilai?.optDoubleOrNull("nilai_pts"),
          nilaiPas = nilai?.optDoubleOrNull("nilai_pas"),
          nilaiKehadiran = nilai?.optDoubleOrNull("nilai_kehadiran"),
          nilaiAkhir = nilai?.optDoubleOrNull("nilai_akhir"),
          nilaiKeterampilan = nilai?.optDoubleOrNull("nilai_keterampilan"),
          detailRowsByMetric = detailRowsBySantriMetric[santriId].orEmpty()
        )
      }

      MapelScoreSnapshot(
        distribusiId = distribusiId,
        subjectTitle = fallbackTitle,
        className = fallbackClassName,
        students = students,
        updatedAt = System.currentTimeMillis(),
        supportsMaterialHistory = true
      )
    }.getOrNull()
  }

  suspend fun saveScoreDraft(
    distribusiId: String,
    santriId: String,
    draft: ScoreDraftPayload
  ): GuruMapelScoreSaveResult = withContext(Dispatchers.IO) {
    if (distribusiId.isBlank() || santriId.isBlank()) {
      return@withContext GuruMapelScoreSaveResult.Error("Data nilai belum lengkap.")
    }

    val distribusiRow = fetchRows(
      table = "distribusi_mapel",
      query = "select=id,mapel_id,semester_id&id=eq.${encodeValue(distribusiId)}&limit=1"
    ).firstOrNull()
      ?: return@withContext GuruMapelScoreSaveResult.Error("Distribusi mapel tidak ditemukan.")

    val payload = JSONObject().apply {
      put("santri_id", santriId)
      put("mapel_id", distribusiRow.optString("mapel_id").trim())
      put("semester_id", distribusiRow.optString("semester_id").trim().ifBlank { JSONObject.NULL })
      put("nilai_tugas", draft.nilaiTugas ?: JSONObject.NULL)
      put("nilai_ulangan_harian", draft.nilaiUlanganHarian ?: 0.0)
      put("nilai_pts", draft.nilaiPts ?: 0.0)
      put("nilai_pas", draft.nilaiPas ?: 0.0)
      put("nilai_kehadiran", draft.nilaiKehadiran ?: JSONObject.NULL)
      put("nilai_keterampilan", draft.nilaiKeterampilan ?: 0.0)
      put(
        "nilai_akhir",
        calculateNilaiAkhir(
          nilaiTugas = draft.nilaiTugas,
          nilaiUlanganHarian = draft.nilaiUlanganHarian,
          nilaiPts = draft.nilaiPts,
          nilaiPas = draft.nilaiPas,
          nilaiKehadiran = draft.nilaiKehadiran
        )
      )
    }

    return@withContext try {
      if (draft.rowId.isNotBlank()) {
        val updateUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/nilai_akademik?id=eq.${encodeValue(draft.rowId)}"
        val connection = createConnection(updateUrl, "PATCH").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        connection.outputStream.use { stream ->
          stream.write(payload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        connection.useScoreJsonArrayResponse { }
      } else {
        val insertUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/nilai_akademik"
        val connection = createConnection(insertUrl, "POST").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
          setRequestProperty("Prefer", "return=representation")
        }
        connection.outputStream.use { stream ->
          stream.write(JSONArray().put(payload).toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        connection.useScoreJsonArrayResponse { }
      }
      GuruMapelScoreSaveResult.Success
    } catch (_: SocketTimeoutException) {
      GuruMapelScoreSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      GuruMapelScoreSaveResult.Error("Gagal menyimpan perubahan nilai.")
    }
  }

  suspend fun saveScoreDetailRows(
    distribusiId: String,
    santriId: String,
    guruId: String,
    metricKey: String,
    rows: List<ScoreDetailRow>,
    deletedIds: List<String>
  ): GuruMapelScoreSaveResult = withContext(Dispatchers.IO) {
    val jenis = metricKeyToJenis(metricKey)
    if (distribusiId.isBlank() || santriId.isBlank() || jenis.isBlank()) {
      return@withContext GuruMapelScoreSaveResult.Error("Detail nilai belum lengkap.")
    }

    val distribusiRow = fetchRows(
      table = "distribusi_mapel",
      query = "select=id,kelas_id,mapel_id,semester_id&id=eq.${encodeValue(distribusiId)}&limit=1"
    ).firstOrNull()
      ?: return@withContext GuruMapelScoreSaveResult.Error("Distribusi mapel tidak ditemukan.")

    return@withContext try {
      deletedIds
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .forEach { rowId ->
          val deleteUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/nilai_input_akademik?id=eq.${encodeValue(rowId)}"
          createConnection(deleteUrl, "DELETE").useScoreJsonArrayResponse { }
        }

      val validRows = rows.mapNotNull { row ->
        val tanggal = row.dateIso.trim()
        val nilai = row.value
        if (tanggal.isBlank() || nilai == null) {
          null
        } else {
          row.copy(
            dateIso = tanggal.take(10),
            value = round2(nilai),
            material = normalizeScoreMaterial(row.material)
          )
        }
      }

      validRows
        .filter { it.id.isNotBlank() }
        .forEach { row ->
          val payload = JSONObject().apply {
            put("tanggal", row.dateIso)
            put("nilai", row.value ?: JSONObject.NULL)
            put("materi", normalizeScoreMaterial(row.material))
          }
          val updateUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/nilai_input_akademik?id=eq.${encodeValue(row.id)}"
          val connection = createConnection(updateUrl, "PATCH").apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Prefer", "return=representation")
          }
          connection.outputStream.use { stream ->
            stream.write(payload.toString().toByteArray(Charsets.UTF_8))
            stream.flush()
          }
          connection.useScoreJsonArrayResponse { }
        }

      val insertPayload = JSONArray()
      validRows
        .filter { it.id.isBlank() }
        .forEach { row ->
          insertPayload.put(JSONObject().apply {
            put("tanggal", row.dateIso)
            put("kelas_id", distribusiRow.optString("kelas_id").trim())
            put("mapel_id", distribusiRow.optString("mapel_id").trim())
            put("guru_id", guruId.trim())
            put("semester_id", distribusiRow.optString("semester_id").trim().ifBlank { JSONObject.NULL })
            put("distribusi_id", distribusiId)
            put("santri_id", santriId)
            put("jenis", jenis)
            put("nilai", row.value ?: JSONObject.NULL)
            put("materi", normalizeScoreMaterial(row.material))
          })
        }

      if (insertPayload.length() > 0) {
        val insertUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/nilai_input_akademik"
        val connection = createConnection(insertUrl, "POST").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json; charset=utf-8")
          setRequestProperty("Prefer", "return=representation")
        }
        connection.outputStream.use { stream ->
          stream.write(insertPayload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        connection.useScoreJsonArrayResponse { }
      }

      recalculateScoreSummariesFromInput(distribusiRow, listOf(santriId))
      GuruMapelScoreSaveResult.Success
    } catch (_: SocketTimeoutException) {
      GuruMapelScoreSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      GuruMapelScoreSaveResult.Error("Gagal menyimpan detail nilai.")
    }
  }

  suspend fun saveScoreDetailRowsBatch(
    distribusiId: String,
    guruId: String,
    metricKey: String,
    rowsBySantriId: Map<String, List<ScoreDetailRow>>,
    deletedIdsBySantriId: Map<String, List<String>>
  ): GuruMapelScoreSaveResult = withContext(Dispatchers.IO) {
    val jenis = metricKeyToJenis(metricKey)
    val santriIds = (rowsBySantriId.keys + deletedIdsBySantriId.keys)
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
    if (distribusiId.isBlank() || jenis.isBlank() || santriIds.isEmpty()) {
      return@withContext GuruMapelScoreSaveResult.Error("Detail nilai belum lengkap.")
    }

    val distribusiRow = fetchRows(
      table = "distribusi_mapel",
      query = "select=id,kelas_id,mapel_id,semester_id&id=eq.${encodeValue(distribusiId)}&limit=1"
    ).firstOrNull()
      ?: return@withContext GuruMapelScoreSaveResult.Error("Distribusi mapel tidak ditemukan.")

    return@withContext try {
      val deletedIds = deletedIdsBySantriId.values
        .flatten()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
      if (deletedIds.isNotEmpty()) {
        val deleteUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/nilai_input_akademik?id=in.(${deletedIds.joinToString(",") { encodeValue(it) }})"
        createConnection(deleteUrl, "DELETE").useScoreJsonArrayResponse { }
      }

      val validRowsBySantriId = rowsBySantriId
        .mapKeys { it.key.trim() }
        .mapValues { (_, rows) ->
          rows.mapNotNull { row ->
            val tanggal = row.dateIso.trim()
            val nilai = row.value
            if (tanggal.isBlank() || nilai == null) {
              null
            } else {
              row.copy(
                dateIso = tanggal.take(10),
                value = round2(nilai),
                material = normalizeScoreMaterial(row.material)
              )
            }
          }
        }
        .filterKeys { it.isNotBlank() }

      val detailUpdatePayload = JSONArray()
      val detailInsertPayload = JSONArray()
      validRowsBySantriId.forEach { (santriId, rows) ->
        rows.forEach { row ->
          val payload = JSONObject().apply {
            put("tanggal", row.dateIso)
            put("kelas_id", distribusiRow.optString("kelas_id").trim())
            put("mapel_id", distribusiRow.optString("mapel_id").trim())
            put("guru_id", guruId.trim())
            put("semester_id", distribusiRow.optString("semester_id").trim().ifBlank { JSONObject.NULL })
            put("distribusi_id", distribusiId)
            put("santri_id", santriId)
            put("jenis", jenis)
            put("nilai", row.value ?: JSONObject.NULL)
            put("materi", normalizeScoreMaterial(row.material))
          }
          if (row.id.isNotBlank()) {
            detailUpdatePayload.put(payload.apply { put("id", row.id) })
          } else {
            detailInsertPayload.put(payload)
          }
        }
      }

      if (detailUpdatePayload.length() > 0) {
        val upsertUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/nilai_input_akademik?on_conflict=id"
        val connection = createConnection(upsertUrl, "POST").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json; charset=utf-8")
          setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
        }
        connection.outputStream.use { stream ->
          stream.write(detailUpdatePayload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        connection.useScoreJsonArrayResponse { }
      }

      if (detailInsertPayload.length() > 0) {
        val insertUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/nilai_input_akademik"
        val connection = createConnection(insertUrl, "POST").apply {
          doOutput = true
          setRequestProperty("Content-Type", "application/json; charset=utf-8")
          setRequestProperty("Prefer", "return=representation")
        }
        connection.outputStream.use { stream ->
          stream.write(detailInsertPayload.toString().toByteArray(Charsets.UTF_8))
          stream.flush()
        }
        connection.useScoreJsonArrayResponse { }
      }

      recalculateScoreSummariesFromInput(distribusiRow, santriIds)
      GuruMapelScoreSaveResult.Success
    } catch (_: SocketTimeoutException) {
      GuruMapelScoreSaveResult.Error("Koneksi ke server terlalu lama. Coba lagi.")
    } catch (_: Exception) {
      GuruMapelScoreSaveResult.Error("Gagal menyimpan detail nilai.")
    }
  }

  private fun recalculateScoreSummariesFromInput(
    distribusiRow: JSONObject,
    santriIds: List<String>
  ) {
    val normalizedSantriIds = santriIds
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
    if (normalizedSantriIds.isEmpty()) return

    val kelasId = distribusiRow.optString("kelas_id").trim()
    val mapelId = distribusiRow.optString("mapel_id").trim()
    val semesterId = distribusiRow.optString("semester_id").trim()
    val santriIdFilter = normalizedSantriIds.joinToString(",") { encodeValue(it) }

    val inputRows = fetchRows(
      table = "nilai_input_akademik",
      query = buildString {
        append("select=santri_id,jenis,nilai")
        append("&santri_id=in.(")
        append(santriIdFilter)
        append(")")
        append("&kelas_id=eq.")
        append(encodeValue(kelasId))
        append("&mapel_id=eq.")
        append(encodeValue(mapelId))
        if (semesterId.isNotBlank()) {
          append("&semester_id=eq.")
          append(encodeValue(semesterId))
        }
      }
    )

    val grouped = inputRows
      .mapNotNull { row ->
        val santriId = row.optString("santri_id").trim()
        val key = jenisToMetricKey(row.optString("jenis").trim())
        val nilai = row.optDoubleOrNull("nilai")
        if (santriId.isBlank() || key.isBlank() || nilai == null) null else santriId to (key to nilai)
      }
      .groupBy({ it.first }, { it.second })

    val existingBySantriId = fetchRows(
      table = "nilai_akademik",
      query = buildString {
        append("select=id,santri_id,nilai_kehadiran")
        append("&santri_id=in.(")
        append(santriIdFilter)
        append(")")
        append("&mapel_id=eq.")
        append(encodeValue(mapelId))
        if (semesterId.isNotBlank()) {
          append("&semester_id=eq.")
          append(encodeValue(semesterId))
        }
      }
    ).associateBy { it.optString("santri_id").trim() }

    val summaryUpdatePayload = JSONArray()
    val summaryInsertPayload = JSONArray()
    normalizedSantriIds.forEach { santriId ->
      val groupedByMetric = grouped[santriId].orEmpty().groupBy({ it.first }, { it.second })
      fun average(metricKey: String): Double? {
        val values = groupedByMetric[metricKey].orEmpty()
        if (values.isEmpty()) return null
        return round2(values.sum() / values.size.toDouble())
      }

      val existing = existingBySantriId[santriId]
      val existingId = existing?.optString("id")?.trim().orEmpty()
      val nilaiTugas = average("nilai_tugas")
      val nilaiUlanganHarian = average("nilai_ulangan_harian") ?: 0.0
      val nilaiPts = average("nilai_pts") ?: 0.0
      val nilaiPas = average("nilai_pas") ?: 0.0
      val nilaiKeterampilan = average("nilai_keterampilan") ?: 0.0
      val nilaiKehadiran = existing?.optDoubleOrNull("nilai_kehadiran")

      val payload = JSONObject().apply {
        put("santri_id", santriId)
        put("mapel_id", mapelId)
        put("semester_id", semesterId.ifBlank { JSONObject.NULL })
        put("nilai_tugas", nilaiTugas ?: JSONObject.NULL)
        put("nilai_ulangan_harian", nilaiUlanganHarian)
        put("nilai_pts", nilaiPts)
        put("nilai_pas", nilaiPas)
        put("nilai_kehadiran", nilaiKehadiran ?: JSONObject.NULL)
        put("nilai_akhir", calculateNilaiAkhir(nilaiTugas, nilaiUlanganHarian, nilaiPts, nilaiPas, nilaiKehadiran))
        put("nilai_keterampilan", nilaiKeterampilan)
      }
      if (existingId.isNotBlank()) {
        summaryUpdatePayload.put(payload.apply { put("id", existingId) })
      } else {
        summaryInsertPayload.put(payload)
      }
    }

    if (summaryUpdatePayload.length() > 0) {
      val upsertUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/nilai_akademik?on_conflict=id"
      val connection = createConnection(upsertUrl, "POST").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
      }
      connection.outputStream.use { stream ->
        stream.write(summaryUpdatePayload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      connection.useScoreJsonArrayResponse { }
    }

    if (summaryInsertPayload.length() > 0) {
      val insertUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/nilai_akademik"
      val connection = createConnection(insertUrl, "POST").apply {
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Prefer", "return=representation")
      }
      connection.outputStream.use { stream ->
        stream.write(summaryInsertPayload.toString().toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      connection.useScoreJsonArrayResponse { }
    }
  }

  private fun calculateNilaiAkhir(
    nilaiTugas: Double?,
    nilaiUlanganHarian: Double?,
    nilaiPts: Double?,
    nilaiPas: Double?,
    nilaiKehadiran: Double?
  ): Double {
    return listOf(
      nilaiTugas ?: 0.0,
      nilaiUlanganHarian ?: 0.0,
      nilaiPts ?: 0.0,
      nilaiPas ?: 0.0,
      nilaiKehadiran ?: 0.0
    ).sum()
  }

  private fun jenisToMetricKey(jenis: String): String {
    return when (jenis.trim().lowercase()) {
      "tugas" -> "nilai_tugas"
      "ulangan harian" -> "nilai_ulangan_harian"
      "uts", "pts" -> "nilai_pts"
      "uas", "pas" -> "nilai_pas"
      "keterampilan" -> "nilai_keterampilan"
      else -> ""
    }
  }

  private fun metricKeyToJenis(metricKey: String): String {
    return when (metricKey.trim()) {
      "nilai_tugas" -> "Tugas"
      "nilai_ulangan_harian" -> "Ulangan Harian"
      "nilai_pts" -> "UTS"
      "nilai_pas" -> "UAS"
      "nilai_keterampilan" -> "Keterampilan"
      else -> ""
    }
  }

  private fun round2(value: Double): Double {
    return kotlin.math.round(value * 100.0) / 100.0
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val requestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/$table?$query"
    val connection = createConnection(requestUrl, "GET")
    return try {
      if (connection.responseCode !in 200..299) return emptyList()
      val payload = connection.inputStream.bufferedReader().use(BufferedReader::readText)
      val rows = JSONArray(payload.ifBlank { "[]" })
      buildList {
        for (index in 0 until rows.length()) {
          val row = rows.optJSONObject(index) ?: continue
          add(row)
        }
      }
    } finally {
      connection.disconnect()
    }
  }

  private fun fetchScoreInputRows(queryTail: String): List<JSONObject> {
    val rowsWithMateri = fetchRows(
      table = "nilai_input_akademik",
      query = "select=id,santri_id,tanggal,nilai,jenis,materi$queryTail"
    )
    if (rowsWithMateri.isNotEmpty()) return rowsWithMateri
    return fetchRows(
      table = "nilai_input_akademik",
      query = "select=id,santri_id,tanggal,nilai,jenis$queryTail"
    )
  }

  private fun mergeScoreInputRows(vararg groups: List<JSONObject>): List<JSONObject> {
    val seen = mutableSetOf<String>()
    return groups
      .flatMap { it }
      .filter { row ->
        val key = row.optString("id").trim().ifBlank {
          listOf(
            row.optString("santri_id").trim(),
            row.optString("tanggal").take(10),
            row.optString("jenis").trim(),
            row.optString("nilai").trim()
          ).joinToString("|")
        }
        seen.add(key)
      }
  }

  private fun encodeValue(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
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
}

private fun JSONObject.optDoubleOrNull(key: String): Double? {
  if (!has(key) || isNull(key)) return null
  val value = opt(key) ?: return null
  return when (value) {
    is Number -> value.toDouble()
    else -> value.toString().toDoubleOrNull()
  }
}

private fun JSONObject.optScoreMaterial(key: String): String {
  if (!has(key) || isNull(key)) return ""
  return normalizeScoreMaterial(optString(key))
}

private fun normalizeScoreMaterial(value: String): String {
  val normalized = value.trim()
  return if (normalized.equals("null", ignoreCase = true)) "" else normalized
}

private inline fun <T> HttpURLConnection.useScoreJsonArrayResponse(
  block: (JSONArray) -> T
): T {
  return try {
    val code = responseCode
    val payload = readScorePayload(code in 200..299)
    if (code !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $code" })
    }
    block(JSONArray(payload.ifBlank { "[]" }))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readScorePayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}
