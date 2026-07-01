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
import java.time.Instant
import java.time.format.DateTimeParseException

sealed interface GuruMapelQuestionSaveResult {
  data object Success : GuruMapelQuestionSaveResult
  data class Error(val message: String) : GuruMapelQuestionSaveResult
}

class GuruMapelQuestionRemoteDataSource {
  suspend fun fetchQuestionJson(
    distribusiId: String
  ): String? = withContext(Dispatchers.IO) {
    if (distribusiId.isBlank()) return@withContext null
    runCatching {
      val rows = fetchRows(
        table = "mapel_soal_guru",
        query = buildString {
          append("select=id,judul,kategori,tanggal,bentuk_soal,jumlah_nomor,instruksi,questions_json,status,updated_at")
          append("&distribusi_id=eq.")
          append(encodeValue(distribusiId))
          append("&order=updated_at.desc")
        }
      )
      JSONArray().apply {
        rows.forEach { row ->
          put(row.toDraftJson())
        }
      }.toString()
    }.getOrNull()
  }

  suspend fun saveQuestionJson(
    distribusiId: String,
    rawJson: String,
    guruId: String,
    guruName: String
  ): GuruMapelQuestionSaveResult = withContext(Dispatchers.IO) {
    if (distribusiId.isBlank()) {
      return@withContext GuruMapelQuestionSaveResult.Error("Data distribusi mapel belum lengkap.")
    }

    val distribusiRow = fetchRows(
      table = "distribusi_mapel",
      query = "select=id,kelas_id,mapel_id,semester_id&id=eq.${encodeValue(distribusiId)}&limit=1"
    ).firstOrNull()
      ?: return@withContext GuruMapelQuestionSaveResult.Error("Distribusi mapel tidak ditemukan.")

    val drafts = parseDraftArray(rawJson)
    val payloads = drafts.map { draft ->
      draft to draft.toServerPayload(
        distribusiId = distribusiId,
        distribusiRow = distribusiRow,
        guruId = guruId,
        guruName = guruName
      )
    }

    return@withContext try {
      val existingRows = fetchRows(
        table = "mapel_soal_guru",
        query = buildString {
          append("select=id,questions_json")
          append("&distribusi_id=eq.")
          append(encodeValue(distribusiId))
        }
      )
      val existingByDraftId = existingRows
        .mapNotNull { row ->
          val draftId = row.remoteDraftId()
          val rowId = row.optString("id").trim()
          if (draftId.isBlank() || rowId.isBlank()) null else draftId to rowId
        }
        .toMap()

      payloads.forEach { (draft, payload) ->
        val draftId = draft.optString("id").trim()
        val existingRowId = existingByDraftId[draftId].orEmpty()
        if (existingRowId.isNotBlank()) {
          val updateUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/mapel_soal_guru?id=eq.${encodeValue(existingRowId)}"
          val connection = createConnection(updateUrl, "PATCH").apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=minimal")
          }
          connection.outputStream.use { stream ->
            stream.write(payload.toString().toByteArray(Charsets.UTF_8))
            stream.flush()
          }
          connection.useQuestionResponse { }
        } else {
          val insertUrl = "${BuildConfig.SUPABASE_URL}/rest/v1/mapel_soal_guru"
          val connection = createConnection(insertUrl, "POST").apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=representation")
          }
          connection.outputStream.use { stream ->
            stream.write(JSONArray().put(payload).toString().toByteArray(Charsets.UTF_8))
            stream.flush()
          }
          connection.useQuestionResponse { }
        }
      }

      GuruMapelQuestionSaveResult.Success
    } catch (_: SocketTimeoutException) {
      GuruMapelQuestionSaveResult.Error("Koneksi ke server terlalu lama. Soal tetap menjadi draft lokal.")
    } catch (_: Exception) {
      GuruMapelQuestionSaveResult.Error("Gagal menyimpan soal ke server. Soal tetap menjadi draft lokal.")
    }
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

  private fun createConnection(requestUrl: String, method: String): HttpURLConnection {
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

private fun parseDraftArray(rawJson: String): List<JSONObject> {
  return runCatching {
    val array = JSONArray(rawJson.ifBlank { "[]" })
    buildList {
      for (index in 0 until array.length()) {
        val row = array.optJSONObject(index) ?: continue
        add(row)
      }
    }
  }.getOrDefault(emptyList())
}

internal fun parseQuestionDraftArray(rawJson: String): List<JSONObject> = parseDraftArray(rawJson)

private fun parseQuestionPayload(value: Any?): JSONObject {
  return when (value) {
    is JSONObject -> JSONObject(value.toString())
    is JSONArray -> JSONObject().put("questions", JSONArray(value.toString()))
    else -> {
      val raw = value?.toString().orEmpty().trim()
      if (raw.isBlank() || raw == "null") return JSONObject()
      runCatching { JSONObject(raw) }
        .recoverCatching { JSONObject().put("questions", JSONArray(raw)) }
        .getOrDefault(JSONObject())
    }
  }
}

internal fun parseQuestionPayloadObject(value: Any?): JSONObject = parseQuestionPayload(value)

private fun JSONObject.toAndroidDraftJson(row: JSONObject): JSONObject {
  val explicitDraft = optJSONObject("draft")
  val base = when {
    explicitDraft != null -> JSONObject(explicitDraft.toString())
    looksLikeAndroidDraft() -> JSONObject(toString())
    else -> JSONObject()
  }

  if (base.optJSONArray("sections").isNullOrEmpty()) {
    val convertedSections = buildAndroidSectionsFromLegacyPayload(this)
    if (convertedSections.length() > 0) {
      base.put("sections", convertedSections)
    }
  }

  if (base.optString("questionsText").isBlank()) {
    val questionText = buildQuestionsTextFromLegacyPayload(this)
    if (questionText.isNotBlank()) base.put("questionsText", questionText)
  }

  if (base.optString("languageCode").isBlank()) {
    base.put("languageCode", extractLanguageFromInstruction(row.optString("instruksi")))
  }
  return base
}

internal fun JSONObject.toQuestionDraftJson(row: JSONObject): JSONObject = toAndroidDraftJson(row)

private fun JSONObject.looksLikeAndroidDraft(): Boolean {
  if (has("title") || has("category") || has("dateIso") || has("languageCode") || has("questionsText")) {
    return true
  }
  val sections = optJSONArray("sections") ?: return false
  for (index in 0 until sections.length()) {
    val section = sections.optJSONObject(index) ?: continue
    if (
      section.has("typeKey") ||
      section.has("typeLabel") ||
      section.has("choiceQuestions") ||
      section.has("matchingPairs") ||
      section.has("wordSearchQuestions") ||
      section.has("crosswordQuestions")
    ) {
      return true
    }
  }
  return false
}

private fun buildAndroidSectionsFromLegacyPayload(payload: JSONObject): JSONArray {
  val questions = payload.optJSONArray("questions") ?: JSONArray()
  val legacySections = payload.optJSONArray("sections")
  val sectionSpecs = when {
    legacySections != null && legacySections.length() > 0 -> legacySections
    questions.length() > 0 -> buildLegacySectionsFromQuestions(questions)
    else -> JSONArray()
  }

  return JSONArray().apply {
    for (index in 0 until sectionSpecs.length()) {
      val section = sectionSpecs.optJSONObject(index) ?: continue
      val typeKey = normalizeLegacyQuestionType(section.optString("type").ifBlank { section.optString("typeKey") })
      val start = section.optInt("start", 1).coerceAtLeast(1)
      val end = section.optInt("end", start).coerceAtLeast(start)
      val matchingQuestions = filterLegacyQuestionsForSection(questions, typeKey, start, end, index)
      put(
        JSONObject().apply {
          put("id", "model-legacy-${index + 1}")
          put("typeKey", typeKey)
          put("typeLabel", legacyTypeLabel(typeKey))
          put("count", legacySectionCount(typeKey, section, matchingQuestions))
          put("instruction", section.optString("instruction").ifBlank { section.optString("instruksi") })
          put("content", "")
          put("scoreText", section.optString("score"))
          put("choiceQuestions", buildAndroidChoiceQuestions(typeKey, matchingQuestions))
          put("matchingPairs", buildAndroidMatchingPairs(matchingQuestions))
          put("wordSearchQuestions", buildAndroidWordSearchQuestions(matchingQuestions))
          put("crosswordQuestions", buildAndroidCrosswordQuestions(matchingQuestions))
        }
      )
    }
  }
}

private fun buildLegacySectionsFromQuestions(questions: JSONArray): JSONArray {
  val orderedTypes = linkedSetOf<String>()
  for (index in 0 until questions.length()) {
    val question = questions.optJSONObject(index) ?: continue
    orderedTypes.add(normalizeLegacyQuestionType(question.optString("type")))
  }
  if (orderedTypes.isEmpty()) orderedTypes.add("pilihan-ganda")
  return JSONArray().apply {
    orderedTypes.forEach { typeKey ->
      val numbers = buildList {
        for (index in 0 until questions.length()) {
          val question = questions.optJSONObject(index) ?: continue
          if (normalizeLegacyQuestionType(question.optString("type")) == typeKey) {
            add(question.optInt("no", index + 1))
          }
        }
      }
      put(
        JSONObject().apply {
          put("type", typeKey)
          put("start", numbers.minOrNull() ?: 1)
          put("end", numbers.maxOrNull() ?: (numbers.minOrNull() ?: 1))
          put("count", numbers.size.coerceAtLeast(1))
        }
      )
    }
  }
}

private fun filterLegacyQuestionsForSection(
  questions: JSONArray,
  typeKey: String,
  start: Int,
  end: Int,
  sectionIndex: Int
): JSONArray {
  return JSONArray().apply {
    for (index in 0 until questions.length()) {
      val question = questions.optJSONObject(index) ?: continue
      val questionType = normalizeLegacyQuestionType(question.optString("type"))
      val questionNo = question.optInt("no", index + 1)
      val sectionKey = question.optString("sectionKey").trim()
      val matchesSectionKey = sectionKey.isNotBlank() && (
        sectionKey == start.toString() ||
          sectionKey == (sectionIndex + 1).toString()
        )
      val matchesRange = questionNo in start..end
      if (questionType == typeKey && (matchesSectionKey || matchesRange)) {
        put(question)
      }
    }
  }
}

private fun buildAndroidChoiceQuestions(typeKey: String, questions: JSONArray): JSONArray {
  if (typeKey !in setOf("pilihan-ganda", "esai", "isi-titik", "benar-salah")) return JSONArray()
  return JSONArray().apply {
    for (index in 0 until questions.length()) {
      val question = questions.optJSONObject(index) ?: continue
      val prompt = question.optString("prompt").ifBlank { question.optString("text") }.trim()
      if (prompt.isBlank()) continue
      put(
        JSONObject().apply {
          put("id", question.optString("id").ifBlank { "q-${index + 1}" })
          put("prompt", prompt)
          put("options", if (typeKey == "pilihan-ganda") legacyOptionsArray(question.opt("options")) else JSONArray())
        }
      )
    }
  }
}

private fun buildAndroidMatchingPairs(questions: JSONArray): JSONArray {
  val question = questions.optJSONObject(0) ?: return JSONArray()
  val columnA = question.optJSONArray("columnA") ?: JSONArray()
  val columnB = question.optJSONArray("columnB") ?: JSONArray()
  val maxSize = maxOf(columnA.length(), columnB.length())
  return JSONArray().apply {
    for (index in 0 until maxSize) {
      val left = columnA.optString(index).trim()
      val right = columnB.optString(index).trim()
      if (left.isBlank() && right.isBlank()) continue
      put(
        JSONObject().apply {
          put("id", "pair-${index + 1}")
          put("leftText", left)
          put("rightText", right)
        }
      )
    }
  }
}

private fun buildAndroidWordSearchQuestions(questions: JSONArray): JSONArray {
  return JSONArray().apply {
    for (index in 0 until questions.length()) {
      val question = questions.optJSONObject(index) ?: continue
      val words = question.optJSONArray("words")?.toTextList().orEmpty()
      if (question.optString("text").isBlank() && words.isEmpty()) continue
      put(
        JSONObject().apply {
          put("id", question.optString("id").ifBlank { "ws-${index + 1}" })
          put("prompt", question.optString("text"))
          put("rows", question.optInt("rows", 10))
          put("cols", question.optInt("cols", 10))
          put("wordsText", words.joinToString(", "))
        }
      )
    }
  }
}

private fun buildAndroidCrosswordQuestions(questions: JSONArray): JSONArray {
  val question = questions.optJSONObject(0) ?: return JSONArray()
  val entriesAcross = question.optJSONArray("entriesAcross") ?: JSONArray()
  val entriesDown = question.optJSONArray("entriesDown") ?: JSONArray()
  if (entriesAcross.length() == 0 && entriesDown.length() == 0) return JSONArray()
  return JSONArray().put(
    JSONObject().apply {
      put("id", question.optString("id").ifBlank { "cw-1" })
      put("rows", question.optInt("rows", 10))
      put("cols", question.optInt("cols", 10))
      put("entriesAcross", normalizeCrosswordEntries(entriesAcross, "across"))
      put("entriesDown", normalizeCrosswordEntries(entriesDown, "down"))
    }
  )
}

private fun normalizeCrosswordEntries(entries: JSONArray, direction: String): JSONArray {
  return JSONArray().apply {
    for (index in 0 until entries.length()) {
      val entry = entries.optJSONObject(index) ?: continue
      put(
        JSONObject().apply {
          put("id", entry.optString("id").ifBlank { "$direction-${index + 1}" })
          put("row", entry.optInt("row", 0) + 1)
          put("col", entry.optInt("col", 0) + 1)
          put("length", entry.optInt("length", 2))
          put("clue", entry.optString("clue"))
          put("answer", entry.optString("answer"))
        }
      )
    }
  }
}

private fun legacySectionCount(typeKey: String, section: JSONObject, questions: JSONArray): Int {
  return when (typeKey) {
    "pasangkan-kata" -> if (questions.length() > 0) 1 else 0
    "cari-kata",
    "teka-silang" -> questions.length()
    else -> questions.length().takeIf { it > 0 }
      ?: section.optInt("count", 0).takeIf { it > 0 }
      ?: section.optInt("blankCount", 0).takeIf { it > 0 }
      ?: 0
  }
}

private fun legacyOptionsArray(value: Any?): JSONArray {
  return when (value) {
    is JSONArray -> JSONArray().apply {
      for (index in 0 until value.length()) {
        val text = legacyOptionText(value.opt(index))
        if (text.isNotBlank()) put(text)
      }
    }
    is JSONObject -> JSONArray().apply {
      val keys = buildList {
        val iterator = value.keys()
        while (iterator.hasNext()) {
          val key = iterator.next().trim()
          if (key.isNotBlank() && !key.startsWith("__")) add(key)
        }
      }.sortedWith(
        compareBy<String> { legacyOptionKeyOrder(it) }
          .thenBy { it.lowercase() }
      )
      keys.forEach { key ->
        val text = legacyOptionText(value.opt(key))
        if (text.isNotBlank()) put(text)
      }
    }
    else -> JSONArray()
  }
}

private fun legacyOptionText(value: Any?): String {
  if (value == null || value == JSONObject.NULL) return ""
  return when (value) {
    is JSONObject -> value.optString("text")
      .ifBlank { value.optString("label") }
      .ifBlank { value.optString("value") }
    else -> value.toString()
  }.trim()
}

private fun legacyOptionKeyOrder(key: String): Int {
  val clean = key.trim().lowercase()
  if (clean.isBlank()) return Int.MAX_VALUE
  clean.toIntOrNull()?.let { return it }
  if (!clean.all { it in 'a'..'z' }) return Int.MAX_VALUE
  return clean.fold(0) { total, char -> (total * 26) + (char.code - 'a'.code + 1) }
}

private fun JSONArray.toTextList(): List<String> {
  return buildList {
    for (index in 0 until length()) {
      val text = optString(index).trim()
      if (text.isNotBlank()) add(text)
    }
  }
}

private fun buildQuestionsTextFromLegacyPayload(payload: JSONObject): String {
  val questions = payload.optJSONArray("questions") ?: return ""
  return buildString {
    for (index in 0 until questions.length()) {
      val question = questions.optJSONObject(index) ?: continue
      val text = question.optString("text").ifBlank { question.optString("prompt") }.trim()
      if (text.isBlank()) continue
      if (isNotBlank()) appendLine()
      append("${question.optInt("no", index + 1)}. ")
      append(text)
    }
  }
}

private fun normalizeLegacyQuestionType(value: String): String {
  val clean = value.trim().lowercase().replace("_", "-").replace(" ", "-")
  return when {
    clean.contains("pilihan") || clean.contains("multiple") -> "pilihan-ganda"
    clean.contains("esai") || clean.contains("essay") -> "esai"
    clean.contains("isi") || clean.contains("isian") || clean.contains("fill") -> "isi-titik"
    clean.contains("benar") || clean.contains("salah") || clean.contains("true") || clean.contains("false") -> "benar-salah"
    clean.contains("pasang") || clean.contains("matching") -> "pasangkan-kata"
    clean.contains("cari") || clean.contains("word") -> "cari-kata"
    clean.contains("teka") || clean.contains("crossword") -> "teka-silang"
    else -> "pilihan-ganda"
  }
}

private fun legacyTypeLabel(typeKey: String): String {
  return when (typeKey) {
    "pilihan-ganda" -> "Pilihan Ganda"
    "esai" -> "Esai"
    "isi-titik" -> "Isian"
    "benar-salah" -> "Benar / Salah"
    "pasangkan-kata" -> "Pasangkan Kata"
    "cari-kata" -> "Cari Kata"
    "teka-silang" -> "Teka-Teki Silang"
    else -> "Pilihan Ganda"
  }
}

private fun extractLanguageFromInstruction(value: String): String {
  return if (value.contains("[[LANG:AR]]", ignoreCase = true)) "AR" else "ID"
}

private fun JSONArray?.isNullOrEmpty(): Boolean = this == null || length() == 0

private fun JSONObject.toDraftJson(): JSONObject {
  val storedPayload = parseQuestionPayload(opt("questions_json"))
  val storedDraft = storedPayload.toAndroidDraftJson(this)
  return JSONObject(storedDraft.toString()).apply {
    putIfBlank("id", optString("id").ifBlank { this@toDraftJson.optString("id") })
    putIfBlank("title", this@toDraftJson.optString("judul"))
    putIfBlank("category", this@toDraftJson.optString("kategori").ifBlank { "Ujian" })
    putIfBlank("form", this@toDraftJson.optString("bentuk_soal"))
    putIfBlank("dateIso", this@toDraftJson.optString("tanggal").take(10))
    putIfBlank("academicYearLabel", "")
    putIfBlank("instruction", this@toDraftJson.optString("instruksi"))
    putIfBlank("statusLabel", this@toDraftJson.optString("status").ifBlank { "Draft" })
    if (!has("updatedAt") || optLong("updatedAt", 0L) <= 0L) {
      put("updatedAt", parseInstantMillis(this@toDraftJson.optString("updated_at")))
    }
  }
}

private fun JSONObject.remoteDraftId(): String {
  val storedPayload = parseQuestionPayload(opt("questions_json"))
  val storedDraftId = storedPayload.toAndroidDraftJson(this).optString("id").trim()
  return storedDraftId.ifBlank { optString("id").trim() }
}

private fun JSONObject.toServerPayload(
  distribusiId: String,
  distribusiRow: JSONObject,
  guruId: String,
  guruName: String
): JSONObject {
  val title = optString("title").trim().ifBlank { "Soal" }
  val category = optString("category").trim().ifBlank { "Ujian" }
  val form = optString("form").trim()
  val instruction = optString("instruction").trim()
  val dateIso = optString("dateIso").trim().take(10)
  val questionCount = draftQuestionCount()
  val questionsJson = toServerQuestionsJsonString()
  return JSONObject().apply {
    put("distribusi_id", distribusiId)
    put("kelas_id", distribusiRow.optString("kelas_id").trim())
    put("mapel_id", distribusiRow.optString("mapel_id").trim())
    put("semester_id", distribusiRow.optString("semester_id").trim().takeIf { it.isNotBlank() } ?: JSONObject.NULL)
    put("tahun_ajaran_id", JSONObject.NULL)
    put("created_by_guru_id", guruId)
    put("created_by_guru_nama", guruName.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
    put("updated_by_guru_id", guruId.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
    put("updated_by_guru_nama", guruName.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
    put("judul", title)
    put("kategori", category)
    put("tanggal", dateIso.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) } ?: JSONObject.NULL)
    put("keterangan", JSONObject.NULL)
    put("bentuk_soal", form.ifBlank { JSONObject.NULL })
    put("jumlah_nomor", questionCount.takeIf { it > 0 } ?: JSONObject.NULL)
    put("instruksi", instruction.ifBlank { JSONObject.NULL })
    put("questions_json", questionsJson)
    put("status", optString("statusLabel").trim().lowercase().ifBlank { "draft" })
  }
}

private fun JSONObject.toServerQuestionsJsonString(): String {
  val draft = JSONObject(toString())
  return JSONObject().apply {
    put("draft", draft)
    put("questions", buildLegacyQuestionsFromAndroidDraft(draft))
    put("sections", buildLegacySectionsFromAndroidDraft(draft))
    put("schema", "mim-native-mapel-question-v1")
  }.toString()
}

internal fun JSONObject.toServerQuestionPayloadJsonString(): String = toServerQuestionsJsonString()

private fun JSONObject.draftQuestionCount(): Int {
  val sections = optJSONArray("sections")
  if (sections != null && sections.length() > 0) {
    var total = 0
    for (index in 0 until sections.length()) {
      val section = sections.optJSONObject(index) ?: continue
      val typeKey = section.optString("typeKey").ifBlank { section.optString("type") }
      total += when (normalizeLegacyQuestionType(typeKey)) {
        "pasangkan-kata" -> if ((section.optJSONArray("matchingPairs")?.length() ?: 0) > 0) 1 else 0
        "cari-kata" -> section.optJSONArray("wordSearchQuestions")?.length() ?: 0
        "teka-silang" -> section.optJSONArray("crosswordQuestions")?.length() ?: 0
        else -> countFilledChoiceQuestions(section.optJSONArray("choiceQuestions"))
      }.takeIf { it > 0 } ?: section.optInt("count", 0)
    }
    if (total > 0) return total
  }
  val questionsTextCount = Regex("""(?m)^\s*\d+[\).]\s+""")
    .findAll(optString("questionsText"))
    .count()
  return questionsTextCount.takeIf { it > 0 } ?: optString("questionsText")
    .lines()
    .count { it.trim().isNotBlank() }
}

internal fun JSONObject.serverQuestionCount(): Int = draftQuestionCount()

private fun countFilledChoiceQuestions(questions: JSONArray?): Int {
  if (questions == null) return 0
  var count = 0
  for (index in 0 until questions.length()) {
    val question = questions.optJSONObject(index) ?: continue
    val prompt = question.optString("prompt").trim()
    val options = question.optJSONArray("options")
    val hasOption = (0 until (options?.length() ?: 0)).any { optionIndex ->
      options?.optString(optionIndex)?.trim()?.isNotBlank() == true
    }
    if (prompt.isNotBlank() || hasOption) count += 1
  }
  return count
}

private fun buildLegacySectionsFromAndroidDraft(draft: JSONObject): JSONArray {
  val sections = draft.optJSONArray("sections") ?: return JSONArray()
  var nextStart = 1
  return JSONArray().apply {
    for (index in 0 until sections.length()) {
      val section = sections.optJSONObject(index) ?: continue
      val typeKey = normalizeLegacyQuestionType(section.optString("typeKey"))
      val count = when (typeKey) {
        "pasangkan-kata" -> if ((section.optJSONArray("matchingPairs")?.length() ?: 0) > 0) 1 else 0
        "cari-kata" -> section.optJSONArray("wordSearchQuestions")?.length() ?: 0
        "teka-silang" -> section.optJSONArray("crosswordQuestions")?.length() ?: 0
        else -> countFilledChoiceQuestions(section.optJSONArray("choiceQuestions"))
      }.takeIf { it > 0 } ?: section.optInt("count", 0).coerceAtLeast(1)
      val start = nextStart
      val end = (start + count - 1).coerceAtLeast(start)
      nextStart = end + 1
      put(
        JSONObject().apply {
          put("type", typeKey)
          put("start", start)
          put("end", end)
          put("count", count)
          put("instruction", section.optString("instruction"))
          put("score", section.optString("scoreText").ifBlank { JSONObject.NULL })
        }
      )
    }
  }
}

private fun buildLegacyQuestionsFromAndroidDraft(draft: JSONObject): JSONArray {
  val sections = draft.optJSONArray("sections") ?: return JSONArray()
  var questionNo = 1
  return JSONArray().apply {
    for (sectionIndex in 0 until sections.length()) {
      val section = sections.optJSONObject(sectionIndex) ?: continue
      val typeKey = normalizeLegacyQuestionType(section.optString("typeKey"))
      val sectionKey = (sectionIndex + 1).toString()
      when (typeKey) {
        "pasangkan-kata" -> {
          val pairs = section.optJSONArray("matchingPairs") ?: JSONArray()
          if (pairs.length() > 0) {
            put(
              JSONObject().apply {
                put("no", questionNo++)
                put("type", typeKey)
                put("text", "")
                put("columnA", JSONArray().apply {
                  for (index in 0 until pairs.length()) {
                    val pair = pairs.optJSONObject(index) ?: continue
                    val text = pair.optString("leftText").trim()
                    if (text.isNotBlank()) put(text)
                  }
                })
                put("columnB", JSONArray().apply {
                  for (index in 0 until pairs.length()) {
                    val pair = pairs.optJSONObject(index) ?: continue
                    val text = pair.optString("rightText").trim()
                    if (text.isNotBlank()) put(text)
                  }
                })
                put("sectionKey", sectionKey)
                put("sectionInstruction", section.optString("instruction"))
              }
            )
          }
        }
        "cari-kata" -> {
          val questions = section.optJSONArray("wordSearchQuestions") ?: JSONArray()
          for (index in 0 until questions.length()) {
            val question = questions.optJSONObject(index) ?: continue
            put(
              JSONObject().apply {
                put("no", questionNo++)
                put("type", typeKey)
                put("text", question.optString("prompt"))
                put("rows", question.optInt("rows", 10))
                put("cols", question.optInt("cols", 10))
                put("words", JSONArray().apply {
                  question.optString("wordsText")
                    .split(",", ";", "\n")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { put(it) }
                })
                put("sectionKey", sectionKey)
                put("sectionInstruction", section.optString("instruction"))
              }
            )
          }
        }
        "teka-silang" -> {
          val questions = section.optJSONArray("crosswordQuestions") ?: JSONArray()
          for (index in 0 until questions.length()) {
            val question = questions.optJSONObject(index) ?: continue
            put(
              JSONObject().apply {
                put("no", questionNo++)
                put("type", typeKey)
                put("text", "")
                put("rows", question.optInt("rows", 10))
                put("cols", question.optInt("cols", 10))
                put("entriesAcross", question.optJSONArray("entriesAcross") ?: JSONArray())
                put("entriesDown", question.optJSONArray("entriesDown") ?: JSONArray())
                put("sectionKey", sectionKey)
                put("sectionInstruction", section.optString("instruction"))
              }
            )
          }
        }
        else -> {
          val questions = section.optJSONArray("choiceQuestions") ?: JSONArray()
          for (index in 0 until questions.length()) {
            val question = questions.optJSONObject(index) ?: continue
            val prompt = question.optString("prompt").trim()
            if (prompt.isBlank()) continue
            put(
              JSONObject().apply {
                put("no", questionNo++)
                put("type", typeKey)
                put("text", prompt)
                put("options", if (typeKey == "pilihan-ganda") legacyOptionsObject(question.optJSONArray("options")) else JSONObject())
                put("answer", "")
                put("sectionKey", sectionKey)
                put("sectionInstruction", section.optString("instruction"))
              }
            )
          }
        }
      }
    }
  }
}

private fun legacyOptionsObject(options: JSONArray?): JSONObject {
  return JSONObject().apply {
    if (options == null) return@apply
    var keyIndex = 0
    for (index in 0 until options.length()) {
      val text = options.optString(index).trim()
      if (text.isBlank()) continue
      val key = ('A'.code + keyIndex).toChar().toString()
      put(key, text)
      keyIndex += 1
    }
  }
}

private fun JSONObject.putIfBlank(key: String, value: String) {
  if (!has(key) || optString(key).isBlank()) put(key, value)
}

private fun parseInstantMillis(value: String): Long {
  return try {
    Instant.parse(value).toEpochMilli()
  } catch (_: DateTimeParseException) {
    System.currentTimeMillis()
  }
}

private inline fun <T> HttpURLConnection.useQuestionResponse(
  block: (String) -> T
): T {
  return try {
    val code = responseCode
    val payload = readQuestionPayload(code in 200..299)
    if (code !in 200..299) {
      throw IllegalStateException(payload.ifBlank { "HTTP $code" })
    }
    block(payload)
  } finally {
    disconnect()
  }
}

private fun HttpURLConnection.readQuestionPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}
