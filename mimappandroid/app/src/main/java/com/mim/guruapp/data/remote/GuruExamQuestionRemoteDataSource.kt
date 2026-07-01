package com.mim.guruapp.data.remote

import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.SubjectOverview
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
import java.text.Normalizer
import java.time.Instant
import java.time.format.DateTimeParseException

data class GuruExamQuestionSnapshot(
  val items: List<GuruExamQuestionItem>,
  val statusMessage: String = ""
)

data class GuruExamQuestionItem(
  val rowKey: String,
  val jadwalId: String,
  val kelasTarget: String,
  val jenis: String,
  val nama: String,
  val className: String,
  val subjectName: String,
  val dateIso: String,
  val timeLabel: String,
  val location: String,
  val note: String,
  val subject: SubjectOverview,
  val questionDraftJson: String,
  val serverStatus: String,
  val updatedAtMillis: Long
)

sealed interface GuruExamQuestionLoadResult {
  data class Success(val snapshot: GuruExamQuestionSnapshot) : GuruExamQuestionLoadResult
  data class Error(val message: String) : GuruExamQuestionLoadResult
}

sealed interface GuruExamQuestionSaveResult {
  data object Success : GuruExamQuestionSaveResult
  data class Error(val message: String) : GuruExamQuestionSaveResult
}

class GuruExamQuestionRemoteDataSource {
  suspend fun fetchExamQuestionSnapshot(
    subjects: List<SubjectOverview>,
    guruId: String
  ): GuruExamQuestionLoadResult = withContext(Dispatchers.IO) {
    val normalizedSubjects = subjects.filter { it.id.isNotBlank() }
    if (normalizedSubjects.isEmpty()) {
      return@withContext GuruExamQuestionLoadResult.Success(
        GuruExamQuestionSnapshot(emptyList(), "Belum ada mapel aktif untuk guru ini.")
      )
    }

    try {
      val distributionRows = fetchDistributionRows(normalizedSubjects.map { it.id })
      val refs = buildDistributionRefs(normalizedSubjects, distributionRows)
      val scheduleRows = fetchRows(
        table = ExamScheduleTable,
        query = "select=*&order=tanggal.asc"
      )
      val examItems = buildExamItems(scheduleRows, refs)
      val jadwalIds = examItems.map { it.jadwalId }.filter(String::isNotBlank).distinct()
      val questionRows = if (guruId.isBlank() || jadwalIds.isEmpty()) {
        emptyList()
      } else {
        runCatching {
          fetchRows(
            table = ExamQuestionTable,
            query = buildString {
              append("select=*")
              append("&guru_id=eq.")
              append(encodeValue(guruId))
              appendInFilter("jadwal_id", jadwalIds)
              append("&order=updated_at.desc")
            }
          )
        }.getOrDefault(emptyList())
      }
      val questionsByKey = questionRows.toQuestionMap()
      GuruExamQuestionLoadResult.Success(
        GuruExamQuestionSnapshot(
          items = examItems.map { item ->
            val question = questionsByKey[item.rowKey] ?: questionsByKey["${item.jadwalId}|*"]
            item.withQuestionRow(question)
          },
          statusMessage = if (examItems.isEmpty()) {
            "Belum ada folder ujian dari admin untuk kelas dan mapel yang diajar."
          } else {
            ""
          }
        )
      )
    } catch (_: SocketTimeoutException) {
      GuruExamQuestionLoadResult.Error("Koneksi ke server terlalu lama saat memuat ujian.")
    } catch (error: Exception) {
      GuruExamQuestionLoadResult.Error(
        buildLoadErrorMessage(error.message.orEmpty())
      )
    }
  }

  suspend fun saveExamQuestionJson(
    item: GuruExamQuestionItem,
    rawJson: String,
    guruId: String,
    guruName: String
  ): GuruExamQuestionSaveResult = withContext(Dispatchers.IO) {
    if (item.jadwalId.isBlank()) {
      return@withContext GuruExamQuestionSaveResult.Error("Jadwal ujian belum lengkap.")
    }
    if (guruId.isBlank()) {
      return@withContext GuruExamQuestionSaveResult.Error("Data guru belum lengkap.")
    }
    val draft = parseQuestionDraftArray(rawJson).firstOrNull()
      ?: return@withContext GuruExamQuestionSaveResult.Error("Data soal belum siap disimpan.")

    val payload = buildSavePayload(item, draft, guruId, guruName, includeKelasTarget = true)
    return@withContext try {
      val existingRow = fetchExistingQuestionRow(item.jadwalId, guruId, item.kelasTarget)
      savePayload(existingRow?.cleanString("id").orEmpty(), payload)
      GuruExamQuestionSaveResult.Success
    } catch (error: Exception) {
      val message = error.message.orEmpty()
      if (isMissingColumnError(message, "kelas_target")) {
        runCatching {
          val fallbackPayload = buildSavePayload(item, draft, guruId, guruName, includeKelasTarget = false)
          val existingRow = fetchExistingQuestionRow(item.jadwalId, guruId, "")
          savePayload(existingRow?.cleanString("id").orEmpty(), fallbackPayload)
          GuruExamQuestionSaveResult.Success
        }.getOrElse {
          GuruExamQuestionSaveResult.Error("Gagal menyimpan soal ujian ke server.")
        }
      } else {
        GuruExamQuestionSaveResult.Error("Gagal menyimpan soal ujian ke server.")
      }
    }
  }

  private fun fetchDistributionRows(distribusiIds: List<String>): List<JSONObject> {
    if (distribusiIds.isEmpty()) return emptyList()
    return fetchRows(
      table = "distribusi_mapel",
      query = buildString {
        append("select=id,kelas_id,mapel_id,semester_id")
        appendInFilter("id", distribusiIds)
      }
    )
  }

  private fun buildDistributionRefs(
    subjects: List<SubjectOverview>,
    distributionRows: List<JSONObject>
  ): List<ExamDistributionRef> {
    val distributionById = distributionRows.associateBy { it.cleanString("id") }
    return subjects.map { subject ->
      val row = distributionById[subject.id]
      ExamDistributionRef(
        distribusiId = subject.id,
        classId = row?.cleanString("kelas_id").orEmpty(),
        mapelId = row?.cleanString("mapel_id").orEmpty(),
        semesterId = row?.cleanString("semester_id").orEmpty(),
        className = subject.className,
        subjectName = subject.title,
        classLevel = resolveClassLevel(subject.className),
        subjectBaseName = subject.title.subjectBaseLabel(),
        subject = subject
      )
    }
  }

  private fun buildExamItems(
    scheduleRows: List<JSONObject>,
    refs: List<ExamDistributionRef>
  ): List<GuruExamQuestionItem> {
    return scheduleRows.flatMap { row ->
      refs
        .filter { ref -> row.matchesRef(ref) }
        .map { ref -> row.toExamItem(ref) }
    }
      .distinctBy { it.rowKey }
      .sortedWith(
        compareBy<GuruExamQuestionItem> { it.dateIso.ifBlank { "9999-12-31" } }
          .thenBy { it.className.lowercase() }
          .thenBy { it.subjectName.lowercase() }
      )
  }

  private fun JSONObject.matchesRef(ref: ExamDistributionRef): Boolean {
    val scheduleClassId = cleanString("kelas_id")
    val scheduleMapelId = cleanString("mapel_id")
    val scheduleSemesterId = cleanString("semester_id")
    if (scheduleClassId.isNotBlank() || scheduleMapelId.isNotBlank()) {
      val classMatches = scheduleClassId.isBlank() || scheduleClassId == ref.classId
      val mapelMatches = scheduleMapelId.isBlank() || scheduleMapelId == ref.mapelId
      val semesterMatches = scheduleSemesterId.isBlank() || ref.semesterId.isBlank() || scheduleSemesterId == ref.semesterId
      return classMatches && mapelMatches && semesterMatches
    }

    val meta = parseExamMeta()
    val classText = cleanString("kelas")
    val mapelText = meta.cleanString("mapel_nama").ifBlank { cleanString("mapel") }
    val classList = rowClassList(meta)
    val directClassMatches = classText.isBlank() ||
      classText.normalizedKey() == ref.className.normalizedKey() ||
      classList.any { it.normalizedKey() == ref.className.normalizedKey() }
    val directMapelMatches = mapelText.isBlank() ||
      mapelText.normalizedKey() == ref.subjectName.normalizedKey() ||
      mapelText.subjectBaseLabel().normalizedKey() == ref.subjectBaseName.normalizedKey()
    if (directClassMatches && directMapelMatches) return true

    val perangkatan = meta.cleanString("perangkatan").ifBlank { classText }
    val perangkatanMatches = perangkatan.normalizedKey() == ref.classLevel.normalizedKey()
    val mapelBaseMatches = mapelText.subjectBaseLabel().normalizedKey() == ref.subjectBaseName.normalizedKey()
    val classListAllowsRef = classList.isEmpty() || classList.any { it.normalizedKey() == ref.className.normalizedKey() }
    return perangkatanMatches && mapelBaseMatches && classListAllowsRef
  }

  private fun JSONObject.toExamItem(ref: ExamDistributionRef): GuruExamQuestionItem {
    val jadwalId = cleanString("id")
    val meta = parseExamMeta()
    val kelasTarget = ref.className.ifBlank { cleanString("kelas") }.ifBlank { "-" }
    val className = ref.className.ifBlank { cleanString("kelas") }
    val subjectName = meta.cleanString("mapel_nama").ifBlank { cleanString("mapel") }.subjectBaseLabel().ifBlank { ref.subjectName }
    return GuruExamQuestionItem(
      rowKey = buildRowKey(jadwalId, kelasTarget),
      jadwalId = jadwalId,
      kelasTarget = kelasTarget,
      jenis = cleanString("jenis").ifBlank { "Ujian" },
      nama = cleanString("nama").ifBlank { "Ujian" },
      className = className,
      subjectName = subjectName,
      dateIso = cleanString("tanggal").take(10),
      timeLabel = buildTimeLabel(cleanString("jam_mulai"), cleanString("jam_selesai")),
      location = cleanString("lokasi"),
      note = cleanString("keterangan"),
      subject = ref.subject.copy(title = subjectName.ifBlank { ref.subject.title }, className = className.ifBlank { ref.subject.className }),
      questionDraftJson = "",
      serverStatus = "",
      updatedAtMillis = 0L
    )
  }

  private fun List<JSONObject>.toQuestionMap(): Map<String, JSONObject> {
    val map = linkedMapOf<String, JSONObject>()
    forEach { row ->
      val jadwalId = row.cleanString("jadwal_id")
      if (jadwalId.isBlank()) return@forEach
      val kelasTarget = row.cleanString("kelas_target")
      val key = buildRowKey(jadwalId, kelasTarget.ifBlank { "*" })
      map.putIfAbsent(key, row)
      if (kelasTarget.isBlank()) map.putIfAbsent("$jadwalId|*", row)
    }
    return map
  }

  private fun GuruExamQuestionItem.withQuestionRow(row: JSONObject?): GuruExamQuestionItem {
    if (row == null) return copy()
    val payload = parseQuestionPayloadObject(row.opt("questions_json"))
    val draft = payload.toQuestionDraftJson(row).apply {
      putIfBlank("id", row.cleanString("id"))
      putIfBlank("title", nama)
      putIfBlank("category", jenis)
      putIfBlank("form", row.cleanString("bentuk_soal"))
      putIfBlank("dateIso", dateIso)
      putIfBlank("academicYearLabel", "")
      putIfBlank("instruction", stripLanguageMarker(row.cleanString("instruksi")))
      put("statusLabel", normalizeServerStatus(row.cleanString("status")))
      if (!has("updatedAt") || optLong("updatedAt", 0L) <= 0L) {
        put("updatedAt", parseInstantMillis(row.cleanString("updated_at")))
      }
    }
    return copy(
      questionDraftJson = JSONArray().put(draft).toString(),
      serverStatus = normalizeServerStatus(row.cleanString("status")),
      updatedAtMillis = parseInstantMillis(row.cleanString("updated_at"))
    )
  }

  private fun buildSavePayload(
    item: GuruExamQuestionItem,
    draft: JSONObject,
    guruId: String,
    guruName: String,
    includeKelasTarget: Boolean
  ): JSONObject {
    val languageCode = draft.cleanString("languageCode").uppercase().let { if (it == "AR") "AR" else "ID" }
    val instruction = draft.cleanString("instruction")
    return JSONObject().apply {
      put("jadwal_id", item.jadwalId)
      if (includeKelasTarget) putNullableString("kelas_target", item.kelasTarget)
      put("guru_id", guruId)
      putNullableString("guru_nama", guruName)
      put("bentuk_soal", draft.cleanString("form").ifBlank { "campuran" })
      put("jumlah_nomor", draft.serverQuestionCount().takeIf { it > 0 } ?: JSONObject.NULL)
      putNullableString("instruksi", buildInstructionWithLanguage(languageCode, instruction))
      put("questions_json", draft.toServerQuestionPayloadJsonString())
      put("status", "submitted")
      put("updated_at", Instant.now().toString())
    }
  }

  private fun fetchExistingQuestionRow(
    jadwalId: String,
    guruId: String,
    kelasTarget: String
  ): JSONObject? {
    val rows = fetchRows(
      table = ExamQuestionTable,
      query = buildString {
        append("select=*")
        append("&jadwal_id=eq.")
        append(encodeValue(jadwalId))
        append("&guru_id=eq.")
        append(encodeValue(guruId))
        append("&order=updated_at.desc")
      }
    )
    val target = kelasTarget.trim()
    return if (target.isBlank()) {
      rows.firstOrNull()
    } else {
      rows.firstOrNull { it.cleanString("kelas_target") == target } ?: rows.firstOrNull { it.cleanString("kelas_target").isBlank() }
    }
  }

  private fun savePayload(rowId: String, payload: JSONObject) {
    val method = if (rowId.isBlank()) "POST" else "PATCH"
    val url = if (rowId.isBlank()) {
      "${BuildConfig.SUPABASE_URL}/rest/v1/$ExamQuestionTable"
    } else {
      "${BuildConfig.SUPABASE_URL}/rest/v1/$ExamQuestionTable?id=eq.${encodeValue(rowId)}"
    }
    val body = if (rowId.isBlank()) JSONArray().put(payload).toString() else payload.toString()
    val connection = createConnection(url, method).apply {
      doOutput = true
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Prefer", "return=representation")
    }
    connection.outputStream.use { stream ->
      stream.write(body.toByteArray(Charsets.UTF_8))
      stream.flush()
    }
    connection.useJsonArrayResponse { }
  }

  private fun fetchRows(table: String, query: String): List<JSONObject> {
    val separator = if (query.startsWith("?")) "" else "?"
    val url = "${BuildConfig.SUPABASE_URL}/rest/v1/$table$separator$query"
    val connection = createConnection(url, "GET")
    return connection.useJsonArrayResponse { rows ->
      List(rows.length()) { index -> rows.optJSONObject(index) }.filterNotNull()
    }
  }

  private fun createConnection(requestUrl: String, method: String): HttpURLConnection {
    return (URL(requestUrl).openConnection() as HttpURLConnection).apply {
      requestMethod = method
      connectTimeout = 15_000
      readTimeout = 20_000
      applySupabaseRequestHeaders()
      setRequestProperty("Accept", "application/json")
      setRequestProperty("Accept-Charset", "UTF-8")
    }
  }

  private data class ExamDistributionRef(
    val distribusiId: String,
    val classId: String,
    val mapelId: String,
    val semesterId: String,
    val className: String,
    val subjectName: String,
    val classLevel: String,
    val subjectBaseName: String,
    val subject: SubjectOverview
  )

  companion object {
    private const val ExamScheduleTable = "jadwal_ujian"
    private const val ExamQuestionTable = "soal_ujian"
  }
}

private fun buildRowKey(jadwalId: String, kelasTarget: String): String {
  return "${jadwalId.trim()}|${kelasTarget.trim().ifBlank { "-" }}"
}

private fun buildTimeLabel(start: String, end: String): String {
  val cleanStart = start.take(5)
  val cleanEnd = end.take(5)
  return when {
    cleanStart.isNotBlank() && cleanEnd.isNotBlank() -> "$cleanStart - $cleanEnd"
    cleanStart.isNotBlank() -> cleanStart
    cleanEnd.isNotBlank() -> cleanEnd
    else -> ""
  }
}

private inline fun <T> HttpURLConnection.useJsonArrayResponse(block: (JSONArray) -> T): T {
  try {
    val code = responseCode
    val payload = readPayload(code in 200..299)
    if (code !in 200..299) throw IllegalStateException(payload.ifBlank { "HTTP $code" })
    val safePayload = payload.trim().ifBlank { "[]" }
    return block(JSONArray(safePayload))
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}

private fun StringBuilder.appendInFilter(column: String, values: List<String>) {
  val normalized = values.map { it.trim() }.filter(String::isNotBlank).distinct()
  if (normalized.isEmpty()) return
  append("&")
  append(column)
  append("=in.(")
  append(normalized.joinToString(",") { "%22${encodeValue(it)}%22" })
  append(")")
}

private fun encodeValue(value: String): String {
  return URLEncoder.encode(value, Charsets.UTF_8.name())
}

private fun JSONObject.cleanString(key: String): String {
  val value = opt(key)
  if (value == null || value == JSONObject.NULL) return ""
  val raw = value.toString().trim()
  if (raw.equals("null", ignoreCase = true)) return ""
  return raw
}

private fun JSONObject.putIfBlank(key: String, value: String) {
  if (!has(key) || optString(key).isBlank()) put(key, value)
}

private fun JSONObject.putNullableString(key: String, value: String?) {
  val normalized = value.orEmpty().trim()
  if (normalized.isBlank()) put(key, JSONObject.NULL) else put(key, normalized)
}

private fun String.normalizedKey(): String {
  val noAccent = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
  return noAccent.lowercase()
    .replace(Regex("[^a-z0-9]+"), "")
    .trim()
}

private fun String.subjectBaseLabel(): String {
  return trim()
    .replace(Regex("""\s*\([^)]*\)\s*"""), " ")
    .replace(Regex("""(\s+(SMP|SMA|MA|Umum))+$""", RegexOption.IGNORE_CASE), "")
    .replace(Regex("\\s{2,}"), " ")
    .trim()
}

private fun resolveClassLevel(className: String): String {
  val text = className.trim().lowercase()
  if (text.isBlank()) return ""
  return when {
    "sma" in text || " ma" in text || text.endsWith("ma") ||
      Regex("""\b(x|xi|xii|10|11|12)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "SMA"
    "smp" in text || Regex("""\b(7|8|9)\b""").containsMatchIn(text) -> "SMP"
    else -> text
  }
}

private fun JSONObject.parseExamMeta(): JSONObject {
  val raw = cleanString("keterangan")
  if (raw.isBlank()) return JSONObject()
  return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
}

private fun JSONObject.rowClassList(meta: JSONObject = parseExamMeta()): List<String> {
  val classRows = meta.optJSONArray("class_rows")
  val classNames = mutableListOf<String>()
  if (classRows != null) {
    for (index in 0 until classRows.length()) {
      val item = classRows.optJSONObject(index) ?: continue
      listOf("kelas_nama", "kelas", "nama_kelas").firstNotNullOfOrNull { key ->
        item.cleanString(key).takeIf(String::isNotBlank)
      }?.let(classNames::add)
    }
  }

  listOf("kelas_list", "classes").forEach { key ->
    val array = meta.optJSONArray(key) ?: return@forEach
    for (index in 0 until array.length()) {
      val value = array.optString(index).trim()
      if (value.isNotBlank()) classNames.add(value)
    }
  }

  val kelasRows = meta.optJSONArray("kelas_rows")
  if (kelasRows != null) {
    for (index in 0 until kelasRows.length()) {
      val item = kelasRows.optJSONObject(index) ?: continue
      listOf("kelas_nama", "kelas", "nama_kelas").firstNotNullOfOrNull { key ->
        item.cleanString(key).takeIf(String::isNotBlank)
      }?.let(classNames::add)
    }
  }

  if (classNames.isEmpty()) {
    splitClassTokens(cleanString("kelas")).forEach(classNames::add)
  }
  return classNames.map(String::trim).filter(String::isNotBlank).distinct()
}

private fun splitClassTokens(value: String): List<String> {
  val raw = value.trim()
  if (raw.isBlank()) return emptyList()
  return raw
    .replace(Regex("""\s+(dan|&)\s+""", RegexOption.IGNORE_CASE), ",")
    .split(Regex("""[;,/|]+"""))
    .map(String::trim)
    .filter(String::isNotBlank)
    .distinct()
}

private fun normalizeServerStatus(value: String): String {
  return when (value.trim().lowercase()) {
    "submitted", "published", "saved", "tersimpan", "sinkron", "synced" -> "Tersimpan"
    else -> "Draft"
  }
}

private fun stripLanguageMarker(value: String): String {
  return value.replace(Regex("""^\[\[LANG:(AR|ID)]]\s*""", RegexOption.IGNORE_CASE), "").trim()
}

private fun buildInstructionWithLanguage(languageCode: String, instruction: String): String {
  val lang = if (languageCode.uppercase() == "AR") "AR" else "ID"
  return "[[LANG:$lang]]\n${instruction.trim()}".trim()
}

private fun parseInstantMillis(value: String): Long {
  if (value.isBlank()) return 0L
  return try {
    Instant.parse(value).toEpochMilli()
  } catch (_: DateTimeParseException) {
    0L
  }
}

private fun buildLoadErrorMessage(message: String): String {
  val lower = message.lowercase()
  return when {
    "jadwal_ujian" in lower -> "Tabel jadwal ujian belum tersedia di server."
    "soal_ujian" in lower -> "Tabel soal ujian belum tersedia di server."
    else -> "Gagal memuat folder ujian dari server."
  }
}

private fun isMissingColumnError(message: String, column: String): Boolean {
  val lower = message.lowercase()
  return "column" in lower && column.lowercase() in lower
}
