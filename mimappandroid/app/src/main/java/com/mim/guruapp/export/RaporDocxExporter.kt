package com.mim.guruapp.export

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RaporExportData(
  val schoolName: String = "",
  val schoolAddress: String = "",
  val studentName: String = "",
  val studentNisn: String = "",
  val className: String = "",
  val semesterLabel: String = "",
  val academicYearLabel: String = "",
  val waliKelasName: String = "",
  val kepalaSekolahName: String = "",
  val spiritualPredicate: String = "",
  val spiritualDescription: String = "",
  val socialPredicate: String = "",
  val socialDescription: String = "",
  val affectiveRows: List<RaporExportAffectiveRow> = emptyList(),
  val musyrifName: String = "",
  val quran: RaporExportQuranData = RaporExportQuranData(),
  val subjects: List<RaporExportSubject> = emptyList(),
  val totalKnowledgeText: String = "",
  val totalKnowledgePredicate: String = "",
  val totalSkillText: String = "",
  val totalSkillPredicate: String = "",
  val extracurricularRows: List<RaporExportSimpleRow> = emptyList(),
  val achievementRows: List<RaporExportSimpleRow> = emptyList(),
  val attendanceRows: List<RaporExportAttendanceRow> = emptyList()
)

data class RaporExportAffectiveRow(
  val aspect: String,
  val percentText: String,
  val note: String
)

data class RaporExportQuranData(
  val ikhtibarScore: String = "",
  val targetSemester: String = "",
  val totalHafalan: String = "",
  val rows: List<RaporExportQuranRow> = emptyList(),
  val averageScore: String = "",
  val averagePredicate: String = "",
  val muhaffizName: String = ""
)

data class RaporExportQuranRow(
  val juzLabel: String,
  val tajwidScore: String,
  val fluencyScore: String,
  val scoreText: String,
  val predicate: String
)

data class RaporExportSubject(
  val name: String,
  val kkmText: String,
  val knowledgeScoreText: String,
  val knowledgePredicate: String,
  val knowledgeDescription: String,
  val skillScoreText: String,
  val skillPredicate: String,
  val skillDescription: String
)

data class RaporExportSimpleRow(
  val label: String,
  val value: String,
  val note: String
)

data class RaporExportAttendanceRow(
  val label: String,
  val izinText: String,
  val sakitText: String,
  val telatText: String,
  val alpaText: String
)

private data class RaporSubjectTemplateSlot(
  val startOccurrence: Int,
  val aliases: List<String>
)

object RaporDocxExporter {
  private const val DocxMime = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
  private const val TemplateAsset = "exam/Format Rapor.docx"
  private val RaporSubjectTemplateSlots = listOf(
    RaporSubjectTemplateSlot(147, listOf("bahasa arab", "arab")),
    RaporSubjectTemplateSlot(153, listOf("bahasa inggris", "inggris")),
    RaporSubjectTemplateSlot(159, listOf("bahasa indonesia", "indonesia")),
    RaporSubjectTemplateSlot(165, listOf("tafsir")),
    RaporSubjectTemplateSlot(171, listOf("hadits", "hadis")),
    RaporSubjectTemplateSlot(177, listOf("aqidah", "akidah")),
    RaporSubjectTemplateSlot(183, listOf("fiqih", "fikih")),
    RaporSubjectTemplateSlot(189, listOf("sirah nabawiyah", "sirah")),
    RaporSubjectTemplateSlot(195, listOf("nahwu")),
    RaporSubjectTemplateSlot(201, listOf("sharf", "sharaf", "saraf", "sorof")),
    RaporSubjectTemplateSlot(207, listOf("akhlak"))
  )

  suspend fun createDocxFile(
    context: Context,
    data: RaporExportData
  ): File = withContext(Dispatchers.IO) {
    val entries = readZipEntries(context.assets.open(TemplateAsset))
    val documentXml = entries["word/document.xml"]?.toString(Charsets.UTF_8)
      ?: throw IllegalStateException("Template rapor tidak memiliki word/document.xml")
    val values = buildMergeFieldValues(data)
    val occurrenceValues = buildMergeFieldOccurrenceValues(data)
    entries["word/document.xml"] = fillMergeFields(documentXml, values, occurrenceValues)
      .fillSubjectTableRows(data)
      .replaceStaticTemplateText(data)
      .replaceRoleSignaturePlaceholders(data)
      .toByteArray(Charsets.UTF_8)

    val outputDir = File(
      context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
      "MIM/rapor"
    ).apply { mkdirs() }
    val file = File(outputDir, "${buildFileName(data)}-${System.currentTimeMillis()}.docx")
    FileOutputStream(file).use { output ->
      writeZipEntries(entries, output)
    }
    file
  }

  fun openDocument(
    context: Context,
    documentFile: File,
    data: RaporExportData
  ) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", documentFile)
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, DocxMime)
      putExtra(Intent.EXTRA_TITLE, documentFile.nameWithoutExtension)
      clipData = ClipData.newUri(context.contentResolver, documentFile.name, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      context.startActivity(
        Intent.createChooser(viewIntent, "Buka rapor ${data.studentName}")
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      )
    } catch (_: ActivityNotFoundException) {
      shareDocument(context, documentFile, data)
    }
  }

  fun shareDocument(
    context: Context,
    documentFile: File,
    data: RaporExportData
  ) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", documentFile)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = DocxMime
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, "Rapor ${data.studentName}")
      putExtra(Intent.EXTRA_TITLE, documentFile.nameWithoutExtension)
      clipData = ClipData.newUri(context.contentResolver, documentFile.name, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(
      Intent.createChooser(shareIntent, "Kirim rapor")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    )
  }

  fun shareDocumentToWhatsApp(
    context: Context,
    documentFile: File,
    data: RaporExportData,
    phone: String
  ): Boolean {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", documentFile)
    val message = buildWhatsappAttachmentMessage(data)
    val normalizedPhone = normalizeWhatsappNumber(phone)

    fun buildIntent(packageName: String? = null): Intent {
      return Intent(Intent.ACTION_SEND).apply {
        type = DocxMime
        packageName?.let(::setPackage)
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, message)
        putExtra(Intent.EXTRA_SUBJECT, "Rapor ${data.studentName}")
        putExtra(Intent.EXTRA_TITLE, documentFile.nameWithoutExtension)
        if (normalizedPhone.isNotBlank()) {
          putExtra("jid", "$normalizedPhone@s.whatsapp.net")
        }
        clipData = ClipData.newUri(context.contentResolver, documentFile.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    }

    listOf("com.whatsapp", "com.whatsapp.w4b").forEach { packageName ->
      val opened = runCatching {
        context.startActivity(buildIntent(packageName))
        true
      }.getOrDefault(false)
      if (opened) return true
    }
    return runCatching {
      context.startActivity(
        Intent.createChooser(buildIntent(), "Kirim rapor")
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      )
      true
    }.getOrDefault(false)
  }

  fun buildWhatsappAttachmentMessage(data: RaporExportData): String {
    return listOf(
      "Assalamu'alaikum warahmatullahi wabarakatuh",
      "",
      "Bapak/Ibu hafizakumullahu ta'ala",
      "",
      "Berikut kami lampirkan rapor ananda ${data.studentName} kelas ${data.className} semester ${data.semesterLabel}.",
      "",
      "Mohon dibaca dengan seksama. Jika ada hal yang perlu dikonfirmasi, silakan menghubungi wali kelas.",
      "",
      "Syukron wajazakumullahu khairan.",
      "",
      "Dokumen rapor terlampir."
    ).joinToString("\n")
  }

  private fun buildMergeFieldValues(data: RaporExportData): Map<String, String> {
    val values = linkedMapOf<String, String>()
    values["Nama_Santri"] = data.studentName
    values["Kelas"] = data.className
    values["F2"] = data.studentName
    values["F3"] = data.className
    values["F4"] = data.studentNisn
    values["F5"] = data.waliKelasName
    values["F6"] = data.semesterLabel
    values["F7"] = data.waliKelasName
    values["Tahun_Pelajaran"] = data.academicYearLabel
    values["Tanggal_Rapor"] = buildReportDateLabel()

    values["Kegiatan_Ekstrakulikuler"] = data.extracurricularRows.joinToString("\n") { row ->
      listOf(row.label, row.value, row.note).filter { it.isNotBlank() }.joinToString(" - ")
    }
    values["Prestasi"] = data.achievementRows.joinToString("\n") { row ->
      listOf(row.label, row.note).filter { it.isNotBlank() }.joinToString(" - ")
    }
    values["Kehadiran_Kelas"] = data.attendanceRows.firstOrNull { it.label.contains("kelas", ignoreCase = true) }?.formatAttendance().orEmpty()
    values["Kehadiran_Halaqah"] = data.attendanceRows.firstOrNull { it.label.contains("halaqah", ignoreCase = true) }?.formatAttendance().orEmpty()
    values["Liqo_Muhasabah"] = data.attendanceRows.firstOrNull { it.label.contains("liqo", ignoreCase = true) }?.formatAttendance().orEmpty()
    return values.mapValues { it.value.ifBlank { "" } }
  }

  private fun buildMergeFieldOccurrenceValues(data: RaporExportData): Map<Int, String> {
    val values = linkedMapOf<Int, String>()
    val affectiveByAspect = data.affectiveRows.associateBy { normalizeKey(it.aspect) }
    values[1] = data.className
    values[2] = data.studentName
    values[3] = data.spiritualPredicate
    values[4] = data.spiritualDescription
    values[5] = data.socialPredicate
    values[6] = data.socialDescription
    values[7] = affectiveByAspect["ibadah"]?.percentText.orEmpty().roundedNumericText()
    values[8] = affectiveByAspect["ibadah"]?.note.orEmpty()
    values[9] = affectiveByAspect["kebersihan"]?.percentText.orEmpty().roundedNumericText()
    values[10] = affectiveByAspect["kebersihan"]?.note.orEmpty()
    values[11] = affectiveByAspect["kedisiplinan"]?.percentText.orEmpty().roundedNumericText()
    values[12] = affectiveByAspect["kedisiplinan"]?.note.orEmpty()
    values[13] = affectiveByAspect["adab dan akhlak"]?.percentText.orEmpty()
      .ifBlank { affectiveByAspect["akhlak"]?.percentText.orEmpty() }
      .ifBlank { affectiveByAspect["adab"]?.percentText.orEmpty() }
      .roundedNumericText()
    values[14] = affectiveByAspect["adab dan akhlak"]?.note.orEmpty()
      .ifBlank { affectiveByAspect["akhlak"]?.note.orEmpty() }
      .ifBlank { affectiveByAspect["adab"]?.note.orEmpty() }
    values[15] = data.className
    values[16] = data.studentName
    values[17] = data.quran.ikhtibarScore.roundedNumericText()
    values[18] = data.quran.targetSemester
    values[19] = data.quran.totalHafalan
    val quranByJuz = data.quran.rows.associateBy { normalizeKey(it.juzLabel) }
    (30 downTo 1).forEachIndexed { index, juz ->
      val row = quranByJuz["juz $juz"]
      val offset = 20 + (index * 4)
      values[offset] = row?.tajwidScore.orEmpty().roundedNumericText()
      values[offset + 1] = row?.fluencyScore.orEmpty().roundedNumericText()
      values[offset + 2] = row?.scoreText.orEmpty().roundedNumericText()
      values[offset + 3] = row?.predicate.orEmpty()
    }
    values[140] = data.quran.averageScore.roundedNumericText()
    values[141] = data.quran.averagePredicate

    values[142] = data.className
    values[143] = data.studentName
    values[144] = data.studentNisn

    values[145] = data.totalKnowledgeText.roundedNumericText()
    values[146] = data.totalKnowledgePredicate
    values[147] = data.totalSkillText.roundedNumericText()
    values[148] = data.totalSkillPredicate

    values[149] = data.className
    values[150] = data.studentName
    values[151] = data.studentNisn
    data.extracurricularRows.getOrNull(0)?.let { row ->
      values[152] = row.label
      values[153] = row.value
      values[154] = row.note
    }
    data.extracurricularRows.getOrNull(1)?.let { row ->
      values[155] = row.label
      values[156] = row.value
      values[157] = row.note
    }
    data.achievementRows.getOrNull(0)?.let { row ->
      values[158] = row.label
      values[159] = row.note
    }
    data.achievementRows.getOrNull(1)?.let { row ->
      values[160] = row.label
      values[161] = row.note
    }
    val kelas = data.attendanceRows.firstOrNull { it.label.contains("kelas", ignoreCase = true) }
    val halaqah = data.attendanceRows.firstOrNull { it.label.contains("halaqah", ignoreCase = true) }
    val liqo = data.attendanceRows.firstOrNull { it.label.contains("liqo", ignoreCase = true) }
    values[162] = kelas?.izinText.orEmpty()
    values[163] = kelas?.sakitText.orEmpty()
    values[164] = kelas?.telatText.orEmpty()
    values[165] = kelas?.alpaText.orEmpty()
    values[166] = halaqah?.izinText.orEmpty()
    values[167] = halaqah?.sakitText.orEmpty()
    values[168] = halaqah?.telatText.orEmpty()
    values[169] = halaqah?.alpaText.orEmpty()
    values[170] = liqo?.izinText.orEmpty()
    values[171] = liqo?.sakitText.orEmpty()
    values[172] = liqo?.telatText.orEmpty()
    values[173] = liqo?.alpaText.orEmpty()
    return values
  }

  private fun RaporExportData.findSubjectForSlot(slot: RaporSubjectTemplateSlot): RaporExportSubject? {
    return subjects.firstOrNull { subject ->
      val key = normalizeKey(subject.name)
      slot.aliases.any { alias -> key == alias || key.contains(alias) || alias.contains(key) }
    }
  }

  private fun String.fillSubjectTableRows(data: RaporExportData): String {
    val rowRegex = Regex("""<w:tr\b[^>]*>.*?</w:tr>""", RegexOption.DOT_MATCHES_ALL)
    return rowRegex.replace(this) { rowMatch ->
      val rowXml = rowMatch.value
      val rowText = extractPlainWordText(rowXml)
      val slot = RaporSubjectTemplateSlots.firstOrNull { templateSlot ->
        val key = normalizeKey(rowText)
        templateSlot.aliases.any { alias -> key.contains(alias) }
      } ?: return@replace rowXml
      val subject = data.findSubjectForSlot(slot)
      rowXml.replaceSubjectScoreCells(subject)
    }
  }

  private fun String.replaceSubjectScoreCells(subject: RaporExportSubject?): String {
    val cellMatches = Regex("""<w:tc\b[^>]*>.*?</w:tc>""", RegexOption.DOT_MATCHES_ALL)
      .findAll(this)
      .toList()
    if (cellMatches.size < 9) return this
    val replacements = mapOf(
      3 to subject?.knowledgeScoreText.orEmpty().roundedNumericText(),
      4 to subject?.knowledgePredicate.orEmpty(),
      5 to subject?.knowledgeDescription.orEmpty(),
      6 to subject?.skillScoreText.orEmpty().roundedNumericText(),
      7 to subject?.skillPredicate.orEmpty(),
      8 to subject?.skillDescription.orEmpty()
    )
    return buildString {
      var cursor = 0
      cellMatches.forEachIndexed { index, match ->
        append(this@replaceSubjectScoreCells.substring(cursor, match.range.first))
        val replacement = replacements[index]
        append(
          if (replacement == null) {
            match.value
          } else {
            match.value.replaceCellText(replacement)
          }
        )
        cursor = match.range.last + 1
      }
      append(this@replaceSubjectScoreCells.substring(cursor))
    }
  }

  private fun String.replaceCellText(value: String): String {
    val paragraph = Regex("""<w:p\b[^>]*>.*?</w:p>""", RegexOption.DOT_MATCHES_ALL).find(this)
      ?: return this
    val paragraphXml = paragraph.value
    val startTagEnd = paragraphXml.indexOf('>').takeIf { it >= 0 }?.plus(1) ?: return this
    val paragraphProperties = Regex("""<w:pPr\b[^>]*>.*?</w:pPr>""", RegexOption.DOT_MATCHES_ALL)
      .find(paragraphXml)
      ?.takeIf { it.range.first == startTagEnd }
    val prefixEnd = paragraphProperties?.range?.last?.plus(1) ?: startTagEnd
    val newParagraph = paragraphXml.substring(0, prefixEnd) +
      buildTextRuns(value, runPropertiesForValue(this, value)) +
      "</w:p>"
    return replaceRange(paragraph.range, newParagraph)
  }

  private fun extractPlainWordText(xml: String): String {
    return Regex("""<w:t\b[^>]*>(.*?)</w:t>""", RegexOption.DOT_MATCHES_ALL)
      .findAll(xml)
      .joinToString("") { unescapeXml(it.groupValues[1]) }
      .replace(Regex("\\s+"), " ")
      .trim()
  }

  private fun fillMergeFields(
    documentXml: String,
    values: Map<String, String>,
    occurrenceValues: Map<Int, String>
  ): String {
    val runBegin = """<w:r\b[^>]*>(?:(?!</w:r>).)*?<w:fldChar[^>]*w:fldCharType="begin"[^>]*/>(?:(?!</w:r>).)*?</w:r>"""
    val runInstr = """<w:r\b[^>]*>(?:(?!</w:r>).)*?<w:instrText[^>]*>\s*MERGEFIELD\s+"?([^\s"<]+)"?\s*</w:instrText>(?:(?!</w:r>).)*?</w:r>"""
    val runEnd = """<w:r\b[^>]*>(?:(?!</w:r>).)*?<w:fldChar[^>]*w:fldCharType="end"[^>]*/>(?:(?!</w:r>).)*?</w:r>"""
    val pattern = Regex(
      "($runBegin\\s*$runInstr)((?:(?!<w:fldChar[^>]*w:fldCharType=\"begin\").)*?)($runEnd)",
      setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    var occurrence = 0
    return pattern.replace(documentXml) { match ->
      occurrence += 1
      val fieldName = match.groupValues[2]
      val prefix = match.groupValues[1]
      val middle = match.groupValues[3]
      val end = match.groupValues[4]
      val value = if (occurrenceValues.containsKey(occurrence)) {
        occurrenceValues[occurrence].orEmpty()
      } else if (fieldName.isGeneratedFieldName()) {
        ""
      } else {
        values[fieldName].orEmpty().ifBlank { values[fieldName.trim('"')].orEmpty() }
      }
      prefix + replaceFieldResult(middle, value, match.value) + end
    }
  }

  private fun String.isGeneratedFieldName(): Boolean {
    return matches(Regex("F\\d+", RegexOption.IGNORE_CASE))
  }

  private fun buildTextRuns(
    value: String,
    runProperties: String = ""
  ): String {
    val safeLines = value.ifBlank { " " }.lines()
    return buildString {
      safeLines.forEachIndexed { index, line ->
        if (index > 0) append("<w:r><w:br/></w:r>")
        append("<w:r>")
        append(runProperties)
        append("""<w:t xml:space="preserve">""")
        append(escapeXml(line))
        append("</w:t></w:r>")
      }
    }
  }

  private fun replaceFieldResult(
    middleXml: String,
    value: String,
    fieldXml: String
  ): String {
    val separateRun = Regex(
      """<w:r\b[^>]*>(?:(?!</w:r>).)*?<w:fldChar[^>]*w:fldCharType="separate"[^>]*/>(?:(?!</w:r>).)*?</w:r>""",
      setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    ).find(middleXml)
    val separator = if (separateRun != null) {
      middleXml.substring(0, separateRun.range.last + 1)
    } else {
      buildFieldSeparateRun(fieldXml)
    }
    return separator + buildTextRuns(value, runPropertiesForValue(fieldXml, value))
  }

  private fun buildFieldSeparateRun(fieldXml: String): String {
    return "<w:r>${runPropertiesForValue(fieldXml, "")}<w:fldChar w:fldCharType=\"separate\"/></w:r>"
  }

  private fun runPropertiesForValue(
    fieldXml: String,
    value: String
  ): String {
    val base = Regex("<w:rPr>.*?</w:rPr>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
      .find(fieldXml)
      ?.value
      .orEmpty()
    val fontSize = compactFontSizeForValue(value)
    val withBase = base.ifBlank {
      """<w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/></w:rPr>"""
    }
    if (fontSize == null) return withBase
    val withoutSize = withBase
      .replace(Regex("""<w:sz\s+[^>]*/>""", RegexOption.IGNORE_CASE), "")
      .replace(Regex("""<w:szCs\s+[^>]*/>""", RegexOption.IGNORE_CASE), "")
    return withoutSize.replace(
      "</w:rPr>",
      """<w:sz w:val="$fontSize"/><w:szCs w:val="$fontSize"/></w:rPr>"""
    )
  }

  private fun compactFontSizeForValue(value: String): Int? {
    val length = value.trim().length
    return when {
      length >= 260 -> 12
      length >= 190 -> 14
      length >= 120 -> 16
      length >= 80 -> 18
      else -> null
    }
  }

  private fun String.roundedNumericText(): String {
    val raw = trim()
    if (raw.isBlank()) return raw
    val match = Regex("""^(-?\d+(?:[\.,]\d+)?)(\s*%)?$""").matchEntire(raw) ?: return raw
    val number = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return raw
    val rounded = kotlin.math.round(number).toInt().toString()
    return rounded + match.groupValues[2]
  }

  private fun String.replaceStaticTemplateText(data: RaporExportData): String {
    val replacements = listOf(
      "PQ Putra Markaz Imam Malik" to data.schoolName,
      "Jln. Tamangapa Raya No. 64" to data.schoolAddress,
      "I/Ganjil" to data.semesterLabel,
      "2025/2026" to data.academicYearLabel,
      "Makassar, 10 Desember 2025" to "Makassar, ${buildReportDateLabel()}"
    )
    return replacements.fold(this) { current, (oldValue, newValue) ->
      if (newValue.isBlank()) current else current.replace(escapeXml(oldValue), escapeXml(newValue))
    }
  }

  private fun String.replaceRoleSignaturePlaceholders(data: RaporExportData): String {
    return replaceSignaturePlaceholderAfter("Musyrif", data.musyrifName)
      .replaceSignaturePlaceholderAfter("Muhaffiz", data.quran.muhaffizName)
  }

  private fun String.replaceSignaturePlaceholderAfter(
    label: String,
    value: String
  ): String {
    if (value.isBlank()) return this
    val labelIndex = indexOf(escapeXml(label), ignoreCase = true)
    if (labelIndex < 0) return this
    val placeholder = Regex("""<w:t([^>]*)>\(\)</w:t>""", RegexOption.IGNORE_CASE)
      .find(this, labelIndex)
      ?: return this
    val replacement = "<w:t${placeholder.groupValues[1]}>${escapeXml(value)}</w:t>"
    return replaceRange(placeholder.range, replacement)
  }

  private fun buildReportDateLabel(): String {
    return LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID")))
  }

  private fun RaporExportAttendanceRow.formatAttendance(): String {
    return "Izin ${izinText.ifBlank { "0" }}, Sakit ${sakitText.ifBlank { "0" }}, Telat ${telatText.ifBlank { "0" }}, Alpa ${alpaText.ifBlank { "0" }}"
  }

  private fun buildFileName(data: RaporExportData): String {
    val name = data.studentName.ifBlank { "Santri" }.sanitizeFilePart()
    val className = data.className.ifBlank { "Kelas" }.sanitizeFilePart()
    val semester = data.semesterLabel.ifBlank { "Semester" }.sanitizeFilePart()
    return "Rapor-$name-$className-$semester"
  }

  private fun normalizeWhatsappNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    if (digits.isBlank()) return ""
    return when {
      digits.startsWith("62") -> digits
      digits.startsWith("0") -> "62${digits.drop(1)}"
      else -> digits
    }
  }

  private fun String.sanitizeFilePart(): String {
    return trim()
      .replace(Regex("[^A-Za-z0-9._ -]+"), "")
      .replace(Regex("\\s+"), " ")
      .ifBlank { "Rapor" }
      .take(72)
  }

  private fun escapeXml(value: String): String {
    return value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")
  }

  private fun unescapeXml(value: String): String {
    return value
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&quot;", "\"")
      .replace("&apos;", "'")
      .replace("&amp;", "&")
  }

  private fun normalizeKey(value: String): String {
    return value
      .trim()
      .lowercase()
      .replace(Regex("[^a-z0-9]+"), " ")
      .trim()
  }

  private fun readZipEntries(input: InputStream): LinkedHashMap<String, ByteArray> {
    val entries = linkedMapOf<String, ByteArray>()
    ZipInputStream(input).use { zip ->
      while (true) {
        val entry = zip.nextEntry ?: break
        val buffer = ByteArrayOutputStream()
        zip.copyTo(buffer)
        entries[entry.name] = buffer.toByteArray()
        zip.closeEntry()
      }
    }
    return entries
  }

  private fun writeZipEntries(
    entries: LinkedHashMap<String, ByteArray>,
    output: FileOutputStream
  ) {
    ZipOutputStream(output).use { zip ->
      entries.forEach { (name, bytes) ->
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
      }
    }
  }
}
