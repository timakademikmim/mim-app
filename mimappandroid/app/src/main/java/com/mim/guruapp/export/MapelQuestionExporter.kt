package com.mim.guruapp.export

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.mim.guruapp.data.model.SubjectOverview
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.LinkedHashMap
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class MapelQuestionExportData(
  val id: String,
  val title: String,
  val category: String,
  val form: String,
  val dateLabel: String,
  val instruction: String,
  val questionsText: String,
  val questionsPayloadJson: String,
  val languageCode: String = "ID",
  val academicYearLabel: String = ""
)

object MapelQuestionExporter {
  private const val DocxMime = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
  private const val TemplateArabic = "exam/ExamAR.docx"
  private const val TemplateIndonesian = "exam/ExamIN.docx"
  private const val PrintUtilsAsset = "exam/guru-exam-print-utils.js"
  private const val DocxUtilsAsset = "exam/guru-exam-docx-utils.js"
  private val ArabicRegex = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u0870-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")

  suspend fun createDocxFile(
    context: Context,
    subject: SubjectOverview,
    data: MapelQuestionExportData
  ): File = withContext(Dispatchers.IO) {
    val language = resolveLanguageCode(data)
    val templateAsset = if (language == "AR") TemplateArabic else TemplateIndonesian
    val entries = readZipEntries(context.assets.open(templateAsset))
    val documentXml = entries["word/document.xml"]?.toString(Charsets.UTF_8)
      ?: throw IllegalStateException("Template DOCX tidak memiliki word/document.xml")
    val headerMeta = buildHeaderMeta(subject, data, language)
    val soalJson = buildSoalJson(data)
    val transformedXml = runCatching {
      transformDocumentXml(
        context = context,
        templateXml = documentXml,
        soalJson = soalJson.toString(),
        headerMetaJson = headerMeta.toString()
      )
    }.getOrNull()
      ?.takeIf { xml -> documentContainsExpectedQuestions(xml, data) }
      ?: buildFallbackDocumentXml(subject, data, headerMeta, language)
    entries["word/document.xml"] = transformedXml.toByteArray(Charsets.UTF_8)

    val outputDir = File(
      context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
      "MIM/soal-docx"
    ).apply { mkdirs() }
    val exportToken = System.currentTimeMillis()
    val file = File(outputDir, "${buildFileName(subject, data)} - $exportToken.docx")
    FileOutputStream(file).use { output ->
      writeZipEntries(entries, output)
    }
    file
  }

  fun openDocument(
    context: Context,
    documentFile: File,
    data: MapelQuestionExportData
  ) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", documentFile)
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, DocxMime)
      putExtra(Intent.EXTRA_TITLE, documentFile.nameWithoutExtension)
      clipData = ClipData.newUri(context.contentResolver, documentFile.name, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = DocxMime
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, data.title.ifBlank { "Soal ${data.category}" })
      putExtra(Intent.EXTRA_TITLE, documentFile.nameWithoutExtension)
      clipData = ClipData.newUri(context.contentResolver, documentFile.name, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      context.startActivity(
        Intent.createChooser(viewIntent, "Buka dokumen soal")
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      )
    } catch (_: ActivityNotFoundException) {
      context.startActivity(
        Intent.createChooser(shareIntent, "Bagikan dokumen soal")
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      )
    }
  }

  private fun buildSoalJson(data: MapelQuestionExportData): JSONObject {
    val payload = runCatching { JSONObject(data.questionsPayloadJson) }.getOrNull()
    val questions = when {
      payload?.optJSONArray("questions") != null -> payload.optJSONArray("questions")
      runCatching { JSONArray(data.questionsPayloadJson) }.getOrNull() != null -> JSONArray(data.questionsPayloadJson)
      else -> JSONArray()
    }
    val normalizedQuestions = if ((questions?.length() ?: 0) > 0) {
      questions
    } else {
      buildFallbackQuestionsFromText(data.questionsText, data.instruction)
    }
    return JSONObject().apply {
      put("bentuk_soal", "campuran")
      put("instruksi", buildInstructionWithLang(resolveLanguageCode(data), data.instruction))
      put("questions_json", JSONObject().apply {
        put("questions", normalizedQuestions)
      }.toString())
    }
  }

  private fun buildFallbackQuestionsFromText(
    questionsText: String,
    instruction: String
  ): JSONArray {
    val rows = JSONArray()
    val sections = questionsText
      .split(Regex("\\n\\s*\\n"))
      .map { it.trim() }
      .filter { it.isNotBlank() }
    if (sections.isEmpty()) {
      rows.put(
        JSONObject().apply {
          put("no", 1)
          put("type", "esai")
          put("sectionKey", "fallback-esai")
          put("sectionInstruction", instruction.trim())
          put("text", "........................................................")
        }
      )
      return rows
    }
    sections.forEachIndexed { sectionIndex, block ->
      val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
      if (lines.isEmpty()) return@forEachIndexed
      val titleLine = lines.first()
      val bodyLines = lines.drop(1)
      val inferredType = when {
        titleLine.contains("pilihan ganda", ignoreCase = true) -> "pilihan-ganda"
        titleLine.contains("benar", ignoreCase = true) && titleLine.contains("salah", ignoreCase = true) -> "benar-salah"
        titleLine.contains("isian", ignoreCase = true) || titleLine.contains("isi titik", ignoreCase = true) -> "isi-titik"
        titleLine.contains("pasangkan", ignoreCase = true) -> "pasangkan-kata"
        titleLine.contains("cari kata", ignoreCase = true) -> "cari-kata"
        titleLine.contains("teka", ignoreCase = true) -> "teka-silang"
        else -> "esai"
      }
      val filteredBody = bodyLines.filterNot { it.startsWith("Instruksi:", ignoreCase = true) }
      val contentLines = if (filteredBody.isNotEmpty()) filteredBody else lines
      contentLines.forEachIndexed { lineIndex, line ->
        rows.put(
          JSONObject().apply {
            put("no", lineIndex + 1)
            put("type", inferredType)
            put("sectionKey", "fallback-${sectionIndex + 1}")
            put("sectionInstruction", instruction.trim())
            put("text", line)
          }
        )
      }
    }
    return rows
  }

  private fun documentContainsExpectedQuestions(xml: String, data: MapelQuestionExportData): Boolean {
    val expectedTexts = extractQuestionRows(data)
      .map { it.text.trim() }
      .filter { it.length >= 3 && !it.contains("...") }
      .take(12)
    if (expectedTexts.isEmpty()) return true
    return expectedTexts.all { expected ->
      val token = expected.take(48)
      xml.contains(escapeXml(token)) || xml.contains(token)
    }
  }

  private data class PrintableQuestionRow(
    val no: Int,
    val type: String,
    val sectionKey: String,
    val sectionInstruction: String,
    val text: String,
    val options: List<Pair<String, String>>,
    val clueLines: List<String> = emptyList()
  )

  private fun extractQuestionRows(data: MapelQuestionExportData): List<PrintableQuestionRow> {
    val payload = runCatching { JSONObject(data.questionsPayloadJson) }.getOrNull()
    val questions = when {
      payload?.optJSONArray("questions") != null -> payload.optJSONArray("questions")
      runCatching { JSONArray(data.questionsPayloadJson) }.getOrNull() != null -> JSONArray(data.questionsPayloadJson)
      else -> buildFallbackQuestionsFromText(data.questionsText, data.instruction)
    } ?: JSONArray()
    val normalized = if (questions.length() > 0) questions else buildFallbackQuestionsFromText(data.questionsText, data.instruction)
    return buildList {
      for (index in 0 until normalized.length()) {
        val item = normalized.optJSONObject(index) ?: continue
        val optionsObject = item.optJSONObject("options")
        val options = buildList {
          if (optionsObject != null) {
            val keys = buildList {
              val iterator = optionsObject.keys()
              while (iterator.hasNext()) add(iterator.next())
            }.sorted()
            keys.forEach { key ->
              val value = optionsObject.opt(key)
              val text = when (value) {
                is JSONObject -> value.optString("text")
                else -> value?.toString().orEmpty()
              }.trim()
              if (text.isNotBlank()) add(key.uppercase(Locale.ROOT) to text)
            }
          }
        }
        add(
          PrintableQuestionRow(
            no = item.optInt("no", index + 1).takeIf { it > 0 } ?: (index + 1),
            type = item.optString("type").ifBlank { "esai" },
            sectionKey = item.optString("sectionKey").ifBlank { "section-${index + 1}" },
            sectionInstruction = item.optString("sectionInstruction"),
            text = item.optString("text").ifBlank { "........................................................" },
            options = options,
            clueLines = buildList {
              fun appendCrosswordGroup(title: String, rows: org.json.JSONArray?) {
                val entries = rows ?: return
                val lines = buildList {
                  for (entryIndex in 0 until entries.length()) {
                    val entry = entries.optJSONObject(entryIndex) ?: continue
                    val clue = entry.optString("clue").trim()
                    if (clue.isBlank()) continue
                    val number = entry.optInt("number", entryIndex + 1)
                    val length = entry.optInt("length", 0)
                    add(
                      buildString {
                        append(number)
                        append(". ")
                        append(clue)
                        if (length > 0) {
                          append(" (")
                          append(length)
                          append(")")
                        }
                      }
                    )
                  }
                }
                if (lines.isEmpty()) return
                if (isNotEmpty()) add("")
                add(title)
                addAll(lines)
              }
              appendCrosswordGroup("Mendatar", item.optJSONArray("entriesAcross"))
              appendCrosswordGroup("Menurun", item.optJSONArray("entriesDown"))
            }
          )
        )
      }
    }
  }

  private fun buildFallbackDocumentXml(
    subject: SubjectOverview,
    data: MapelQuestionExportData,
    headerMeta: JSONObject,
    language: String
  ): String {
    val rows = extractQuestionRows(data)
    val sections = rows.groupBy { "${it.sectionKey}|${it.type}" }.values.toList()
    return buildString {
      append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
      append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
      append("<w:body>")
      appendParagraph(headerMeta.optString("examTitle").ifBlank { data.title.ifBlank { "SOAL" } }, bold = true, center = true, size = 24)
      appendParagraph(headerMeta.optString("schoolName"), bold = true, center = true, size = 22)
      appendParagraph(headerMeta.optString("schoolYear"), center = true, size = 20)
      appendParagraph("", size = 8)
      appendInfoTable(subject, data, headerMeta)
      val instruction = headerMeta.optString("generalInstruction").trim()
      if (instruction.isNotBlank()) {
        instruction.lines().filter { it.trim().isNotBlank() }.forEach { line ->
          appendParagraph(line.trim(), size = 22)
        }
        appendParagraph("", size = 8)
      }
      sections.forEachIndexed { sectionIndex, sectionRows ->
        val type = sectionRows.firstOrNull()?.type.orEmpty()
        appendParagraph("${sectionLabel(sectionIndex, type, language)}", bold = true, size = 22)
        val sectionInstruction = sectionRows.firstOrNull()?.sectionInstruction.orEmpty().trim()
        if (sectionInstruction.isNotBlank()) {
          sectionInstruction.lines().filter { it.trim().isNotBlank() }.forEach { line ->
            appendParagraph(
              line.trim(),
              italic = true,
              size = 20,
              spacingAfter = if (type == "isi-titik") 0 else 20
            )
          }
        }
        sectionRows.forEachIndexed { index, row ->
          val number = if (row.no > 0) row.no else index + 1
          val questionText = if (row.type == "cari-kata") {
            row.text
          } else {
            "$number. ${row.text}"
          }
          appendParagraph(
            questionText,
            size = 22,
            spacingAfter = 0
          )
          if (row.type == "teka-silang" && row.clueLines.isNotEmpty()) {
            row.clueLines.forEach { clueLine ->
              appendParagraph(
                clueLine,
                size = 20,
                spacingAfter = 0
              )
            }
          }
          val options = if (row.options.isNotEmpty()) {
            row.options
          } else if (row.type == "benar-salah") {
            listOf("A" to "Benar", "B" to "Salah")
          } else {
            emptyList()
          }
          options.forEach { (label, text) ->
            appendParagraph("   $label. $text", size = 21)
          }
        }
        appendParagraph("", size = 8)
      }
      append("""<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134" w:header="708" w:footer="708" w:gutter="0"/></w:sectPr>""")
      append("</w:body></w:document>")
    }
  }

  private fun StringBuilder.appendInfoTable(
    subject: SubjectOverview,
    data: MapelQuestionExportData,
    headerMeta: JSONObject
  ) {
    append("""<w:tbl><w:tblPr><w:tblW w:w="0" w:type="auto"/><w:tblBorders><w:top w:val="single" w:sz="4"/><w:left w:val="single" w:sz="4"/><w:bottom w:val="single" w:sz="4"/><w:right w:val="single" w:sz="4"/><w:insideH w:val="single" w:sz="4"/><w:insideV w:val="single" w:sz="4"/></w:tblBorders></w:tblPr>""")
    appendInfoRow("Pelajaran", headerMeta.optString("subject").ifBlank { subject.title }, "Hari/Tanggal", headerMeta.optString("examDate").ifBlank { "-" })
    appendInfoRow("Kelas", headerMeta.optString("className").ifBlank { subject.className }, "Waktu", headerMeta.optString("duration").ifBlank { "-" })
    appendInfoRow("Nama", "________________________________", "Kategori", data.category.ifBlank { "-" })
    append("</w:tbl>")
    appendParagraph("", size = 8)
  }

  private fun StringBuilder.appendInfoRow(labelA: String, valueA: String, labelB: String, valueB: String) {
    append("<w:tr>")
    listOf(labelA, valueA, labelB, valueB).forEach { value ->
      append("""<w:tc><w:tcPr><w:tcW w:w="2400" w:type="dxa"/></w:tcPr>""")
      appendParagraph(value, size = 20)
      append("</w:tc>")
    }
    append("</w:tr>")
  }

  private fun StringBuilder.appendParagraph(
    text: String,
    bold: Boolean = false,
    italic: Boolean = false,
    center: Boolean = false,
    size: Int = 22,
    spacingAfter: Int = 0
  ) {
    append("<w:p>")
    append("<w:pPr>")
    if (center) append("""<w:jc w:val="center"/>""")
    append("""<w:spacing w:before="0" w:after="$spacingAfter" w:line="240" w:lineRule="auto"/>""")
    append("</w:pPr>")
    append("<w:r><w:rPr>")
    if (bold) append("<w:b/>")
    if (italic) append("<w:i/>")
    append("""<w:sz w:val="$size"/><w:szCs w:val="$size"/>""")
    append("</w:rPr><w:t xml:space=\"preserve\">")
    append(escapeXml(text))
    append("</w:t></w:r></w:p>")
  }

  private fun sectionLabel(index: Int, type: String, language: String): String {
    val marker = if (language == "AR") {
      listOf("أ", "ب", "ج", "د", "هـ", "و").getOrElse(index) { "${index + 1}" }
    } else {
      ('A'.code + (index % 26)).toChar().toString()
    }
    val label = when (type) {
      "pilihan-ganda" -> "Pilihan Ganda"
      "benar-salah" -> "Pernyataan Benar atau Salah"
      "isi-titik" -> "Isian"
      "pasangkan-kata" -> "Pasangkan Kata"
      "cari-kata" -> "Cari Kata"
      "teka-silang" -> "Teka-Teki Silang"
      else -> "Esai"
    }
    return "$marker. $label"
  }

  private fun escapeXml(value: String): String {
    return value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")
  }

  private fun buildHeaderMeta(
    subject: SubjectOverview,
    data: MapelQuestionExportData,
    language: String
  ): JSONObject {
    val locale = Locale("id", "ID")
    val title = data.title.ifBlank { "SOAL" }.uppercase(locale)
    val yearLabel = data.academicYearLabel.trim().ifBlank { "-" }
    return JSONObject().apply {
      put("examTitle", title)
      put("schoolName", "PESANTREN QUR`AN PUTRA MARKAZ IMAM MALIK")
      put("schoolYear", "TAHUN AJARAN ${yearLabel.uppercase(locale)}")
      put("examName", data.title.ifBlank { "Soal" })
      put("examType", data.category.ifBlank { "Tugas" })
      put("subject", subject.title.ifBlank { "-" })
      put("className", subject.className.ifBlank { "-" })
      put("examDate", formatExamDateLabel(data.dateLabel))
      put("duration", "-")
      put("scoreLabel", "NILAI")
      put("studentLine", "________________________________")
      put("lang", language)
      put("generalInstruction", stripInstructionMarker(data.instruction))
    }
  }

  private fun buildInstructionWithLang(language: String, instruction: String): String {
    val cleanInstruction = stripInstructionMarker(instruction)
    return if (cleanInstruction.isBlank()) {
      "[[LANG:$language]]"
    } else {
      "[[LANG:$language]]\n$cleanInstruction"
    }
  }

  private fun stripInstructionMarker(value: String): String {
    return value.replace(Regex("^\\s*\\[\\[LANG:(AR|ID)\\]\\]\\s*\n?", RegexOption.IGNORE_CASE), "").trim()
  }

  private fun resolveLanguageCode(data: MapelQuestionExportData): String {
    val requested = data.languageCode.trim().uppercase(Locale.ROOT)
    if (requested == "AR") return "AR"
    val source = buildString {
      append(data.title)
      append('\n')
      append(data.instruction)
      append('\n')
      append(data.questionsText)
    }
    return if (ArabicRegex.containsMatchIn(source)) "AR" else "ID"
  }

  private fun formatExamDateLabel(value: String): String {
    val raw = value.trim()
    if (raw.isBlank()) return "-"
    return try {
      val localDate = LocalDate.parse(raw)
      localDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID")))
    } catch (_: DateTimeParseException) {
      raw
    }
  }

  private fun buildFileName(subject: SubjectOverview, data: MapelQuestionExportData): String {
    val title = data.title.ifBlank { "Soal ${subject.title}" }
    return "Soal ${sanitize(title)} - ${sanitize(subject.className.ifBlank { "-" })}"
  }

  private fun sanitize(value: String): String {
    return value
      .replace(Regex("[\\\\/:*?\"<>|]"), "-")
      .replace(Regex("\\s+"), " ")
      .trim()
      .ifBlank { "dokumen" }
  }

  private fun readZipEntries(input: InputStream): LinkedHashMap<String, ByteArray> {
    val entries = LinkedHashMap<String, ByteArray>()
    ZipInputStream(input.buffered()).use { zip ->
      generateSequence { zip.nextEntry }.forEach { entry ->
        val bytes = if (entry.isDirectory) {
          ByteArray(0)
        } else {
          zip.readBytes()
        }
        entries[entry.name] = bytes
        zip.closeEntry()
      }
    }
    return entries
  }

  private fun writeZipEntries(entries: LinkedHashMap<String, ByteArray>, output: FileOutputStream) {
    ZipOutputStream(output.buffered()).use { zip ->
      entries.forEach { (name, bytes) ->
        val entry = ZipEntry(name)
        zip.putNextEntry(entry)
        if (!name.endsWith("/")) {
          zip.write(bytes)
        }
        zip.closeEntry()
      }
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  private suspend fun transformDocumentXml(
    context: Context,
    templateXml: String,
    soalJson: String,
    headerMetaJson: String
  ): String = suspendCancellableCoroutine { continuation ->
    val handler = Handler(Looper.getMainLooper())
    handler.post {
      var finished = false
      lateinit var webView: WebView

      fun finishWith(result: Result<String>) {
        if (finished) return
        finished = true
        runCatching { webView.stopLoading() }
        runCatching { webView.destroy() }
        result.fold(
          onSuccess = { continuation.resume(it) },
          onFailure = { continuation.resumeWithException(it) }
        )
      }

      webView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.domStorageEnabled = true
        webViewClient = object : WebViewClient() {
          override fun onPageFinished(view: WebView?, url: String?) {
            if (finished) return
            val js = buildString {
              append("(function(){try{")
              append("var soal=")
              append(soalJson)
              append(";var headerMeta=")
              append(headerMetaJson)
              append(";var result=window.guruExamDocxUtils.transformExamDocumentXml(")
              append(JSONObject.quote(templateXml))
              append(",soal,headerMeta);")
              append("return JSON.stringify({ok:true,xml:result});")
              append("}catch(error){return JSON.stringify({ok:false,message:String(error&&error.message||error)});}})();")
            }
            evaluateJavascript(js) { raw ->
              if (finished) return@evaluateJavascript
              val payload = runCatching { JSONArray("[$raw]").getString(0) }
                .mapCatching { JSONObject(it) }
                .getOrElse {
                  finishWith(Result.failure(IllegalStateException("Gagal membaca hasil template DOCX.")))
                  return@evaluateJavascript
                }
              if (payload.optBoolean("ok")) {
                finishWith(Result.success(payload.optString("xml")))
              } else {
                finishWith(Result.failure(IllegalStateException(payload.optString("message").ifBlank { "Gagal menyusun template DOCX." })))
              }
            }
          }

          override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: android.webkit.WebResourceError?
          ) {
            if (request?.isForMainFrame == true) {
              finishWith(Result.failure(IllegalStateException(error?.description?.toString().orEmpty().ifBlank { "Gagal memuat mesin template DOCX." })))
            }
          }
        }
      }

      continuation.invokeOnCancellation {
        handler.post {
          if (!finished) {
            finished = true
            runCatching { webView.stopLoading() }
            runCatching { webView.destroy() }
          }
        }
      }

      val html = """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <script src="guru-exam-print-utils.js"></script>
          <script src="guru-exam-docx-utils.js"></script>
        </head>
        <body></body>
        </html>
      """.trimIndent()
      webView.loadDataWithBaseURL("file:///android_asset/exam/", html, "text/html", "utf-8", null)
    }
  }
}
