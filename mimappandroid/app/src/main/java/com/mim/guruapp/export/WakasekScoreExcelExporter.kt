package com.mim.guruapp.export

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mim.guruapp.data.model.MapelScoreSnapshot
import com.mim.guruapp.data.model.SubjectOverview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt

object WakasekScoreExcelExporter {
  private const val XlsxMime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  private val IndonesianLocale = Locale("id", "ID")

  data class ScoreWorksheet(
    val subject: SubjectOverview,
    val snapshot: MapelScoreSnapshot,
    val reportTitle: String = "LAPORAN NILAI SISWA"
  )

  suspend fun createWorkbook(
    context: Context,
    worksheet: ScoreWorksheet
  ): File = createWorkbook(context, listOf(worksheet))

  suspend fun createWorkbook(
    context: Context,
    worksheets: List<ScoreWorksheet>
  ): File = withContext(Dispatchers.IO) {
    val safeWorksheets = worksheets.ifEmpty {
      listOf(
        ScoreWorksheet(
          subject = SubjectOverview(
            id = "",
            title = "Mapel",
            className = "Kelas",
            semester = "-",
            semesterActive = false,
            attendancePending = 0,
            scorePending = 0,
            materialCount = 0
          ),
          snapshot = MapelScoreSnapshot(
            distribusiId = "",
            subjectTitle = "Mapel",
            className = "Kelas",
            students = emptyList(),
            updatedAt = System.currentTimeMillis()
          )
        )
      )
    }
    val exportDir = File(context.getExternalFilesDir(null), "exports/nilai-siswa").apply { mkdirs() }
    val firstWorksheet = safeWorksheets.first()
    val safeSubject = listOf(
      if (safeWorksheets.size == 1) firstWorksheet.snapshot.subjectTitle.ifBlank { firstWorksheet.subject.title } else "semua_mapel",
      firstWorksheet.snapshot.className.ifBlank { firstWorksheet.subject.className }
    )
      .joinToString("_")
      .replace(Regex("[^A-Za-z0-9._-]+"), "_")
      .trim('_')
      .ifBlank { "nilai_siswa" }
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
    val file = File(exportDir, "nilai_${safeSubject}_$timestamp.xlsx")

    ZipOutputStream(file.outputStream()).use { zip ->
      zip.putText("[Content_Types].xml", contentTypesXml(safeWorksheets.size))
      zip.putText("_rels/.rels", rootRelsXml())
      zip.putText("docProps/app.xml", appXml())
      zip.putText("docProps/core.xml", coreXml())
      zip.putText("xl/workbook.xml", workbookXml(safeWorksheets))
      zip.putText("xl/_rels/workbook.xml.rels", workbookRelsXml(safeWorksheets.size))
      zip.putText("xl/styles.xml", stylesXml())
      safeWorksheets.forEachIndexed { index, item ->
        zip.putText("xl/worksheets/sheet${index + 1}.xml", worksheetXml(item))
      }
    }
    file
  }

  fun openWorkbook(context: Context, file: File) {
    val uri = file.toContentUri(context)
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, XlsxMime)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
      clipData = ClipData.newUri(context.contentResolver, file.name, uri)
    }
    try {
      context.startActivity(Intent.createChooser(intent, "Buka file nilai").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
      shareWorkbook(context, file)
    }
  }

  fun shareWorkbook(context: Context, file: File) {
    val uri = file.toContentUri(context)
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = XlsxMime
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, "Nilai Siswa")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
      clipData = ClipData.newUri(context.contentResolver, file.name, uri)
    }
    context.startActivity(Intent.createChooser(intent, "Kirim nilai siswa").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }

  private fun File.toContentUri(context: Context) =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", this)

  private fun worksheetXml(worksheet: ScoreWorksheet): String {
    val rows = buildRows(worksheet)
    val lastColumn = 11
    val titleMerge = "A1:${columnName(lastColumn)}1"
    val mapelMerge = "A2:${columnName(lastColumn)}2"
    val classMerge = "A3:${columnName(lastColumn)}3"
    val semesterMerge = "A4:${columnName(lastColumn)}4"
    val cols = """
      |<cols>
      |  <col min="1" max="1" width="6" customWidth="1"/>
      |  <col min="2" max="2" width="30" customWidth="1"/>
      |  <col min="3" max="9" width="14" customWidth="1"/>
      |  <col min="10" max="11" width="42" customWidth="1"/>
      |</cols>
    """.trimMargin()

    return xmlHeader() + """
      |<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
      |  <sheetViews><sheetView workbookViewId="0" showGridLines="1"/></sheetViews>
      |  <sheetFormatPr defaultRowHeight="18"/>
      |  $cols
      |  <sheetData>
      |    ${rows.joinToString("\n") { it.toXml() }}
      |  </sheetData>
      |  <mergeCells count="10">
      |    <mergeCell ref="$titleMerge"/>
      |    <mergeCell ref="$mapelMerge"/>
      |    <mergeCell ref="$classMerge"/>
      |    <mergeCell ref="$semesterMerge"/>
      |    <mergeCell ref="A6:A7"/>
      |    <mergeCell ref="B6:B7"/>
      |    <mergeCell ref="C6:H6"/>
      |    <mergeCell ref="I6:I7"/>
      |    <mergeCell ref="J6:J7"/>
      |    <mergeCell ref="K6:K7"/>
      |  </mergeCells>
      |  <pageMargins left="0.35" right="0.35" top="0.55" bottom="0.55" header="0.2" footer="0.2"/>
      |</worksheet>
    """.trimMargin()
  }

  private fun buildRows(worksheet: ScoreWorksheet): List<SheetRow> {
    val snapshot = worksheet.snapshot
    val subject = worksheet.subject
    val rows = mutableListOf<SheetRow>()
    rows += SheetRow(1, listOf(cell(1, 1, worksheet.reportTitle, 2)))
    rows += SheetRow(2, listOf(cell(1, 2, "Mapel: ${snapshot.subjectTitle.ifBlank { subject.title }.ifBlank { "-" }}", 6)))
    rows += SheetRow(3, listOf(cell(1, 3, "Kelas: ${snapshot.className.ifBlank { subject.className }.ifBlank { "-" }}", 6)))
    rows += SheetRow(4, listOf(cell(1, 4, "Semester: ${subject.semester.ifBlank { "-" }}", 6)))
    rows += SheetRow(
      6,
      listOf(
        cell(1, 6, "No", 1),
        cell(2, 6, "Nama Santri", 1),
        cell(3, 6, "Nilai Pengetahuan", 1),
        cell(9, 6, "Keterampilan", 1),
        cell(10, 6, "Deskripsi Pengetahuan", 1),
        cell(11, 6, "Deskripsi Keterampilan", 1)
      )
    )
    rows += SheetRow(
      7,
      listOf(
        cell(3, 7, "Tugas", 1),
        cell(4, 7, "Ulangan Harian", 1),
        cell(5, 7, "UTS", 1),
        cell(6, 7, "UAS", 1),
        cell(7, 7, "Kehadiran", 1),
        cell(8, 7, "Total", 1)
      )
    )

    val students = snapshot.students.sortedBy { it.name.lowercase(IndonesianLocale) }
    students.forEachIndexed { index, student ->
      val rowIndex = index + 8
      rows += SheetRow(
        rowIndex,
        listOf(
          cell(1, rowIndex, (index + 1).toString(), 3),
          cell(2, rowIndex, student.name, 0),
          cell(3, rowIndex, scoreText(student.nilaiTugas), 3),
          cell(4, rowIndex, scoreText(student.nilaiUlanganHarian), 3),
          cell(5, rowIndex, scoreText(student.nilaiPts), 3),
          cell(6, rowIndex, scoreText(student.nilaiPas), 3),
          cell(7, rowIndex, scoreText(student.nilaiKehadiran), 3),
          cell(8, rowIndex, scoreText(student.nilaiAkhir), 4),
          cell(9, rowIndex, scoreText(student.nilaiKeterampilan), 4),
          cell(10, rowIndex, "", 5),
          cell(11, rowIndex, "", 5)
        )
      )
    }
    if (students.isEmpty()) {
      rows += SheetRow(8, listOf(cell(1, 8, "Belum ada data nilai siswa.", 6)))
    }
    return rows
  }

  private fun scoreText(value: Double?): String =
    value?.let(::scoreText) ?: "-"

  private fun scoreText(value: Double): String {
    val rounded = value.roundToInt()
    return if (kotlin.math.abs(value - rounded) < 0.0001) rounded.toString() else "%.2f".format(IndonesianLocale, value)
  }

  private fun cell(column: Int, row: Int, value: String, style: Int) =
    SheetCell(column, row, value, style)

  private data class SheetRow(val index: Int, val cells: List<SheetCell>) {
    fun toXml(): String = """<row r="$index">${cells.joinToString("") { it.toXml() }}</row>"""
  }

  private data class SheetCell(
    val column: Int,
    val row: Int,
    val value: String,
    val style: Int
  ) {
    fun toXml(): String {
      val ref = "${columnName(column)}$row"
      return """<c r="$ref" t="inlineStr" s="$style"><is><t>${escapeXml(value)}</t></is></c>"""
    }
  }

  private fun columnName(index: Int): String {
    var value = index
    val result = StringBuilder()
    while (value > 0) {
      val remainder = (value - 1) % 26
      result.insert(0, ('A'.code + remainder).toChar())
      value = (value - 1) / 26
    }
    return result.toString()
  }

  private fun ZipOutputStream.putText(path: String, content: String) {
    putNextEntry(ZipEntry(path))
    write(content.toByteArray(Charsets.UTF_8))
    closeEntry()
  }

  private fun escapeXml(value: String): String =
    value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")

  private fun xmlHeader() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""

  private fun contentTypesXml(sheetCount: Int) = xmlHeader() + """
    |<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    |  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    |  <Default Extension="xml" ContentType="application/xml"/>
    |  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    |  ${(1..sheetCount).joinToString("\n  ") { """<Override PartName="/xl/worksheets/sheet$it.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" }}
    |  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
    |  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
    |  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
    |</Types>
  """.trimMargin()

  private fun rootRelsXml() = xmlHeader() + """
    |<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    |  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
    |  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
    |  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
    |</Relationships>
  """.trimMargin()

  private fun workbookXml(worksheets: List<ScoreWorksheet>): String {
    val sheetNames = uniqueSheetNames(worksheets)
    return xmlHeader() + """
    |<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    |  <sheets>
    |    ${worksheets.mapIndexed { index, _ ->
      """<sheet name="${escapeXml(sheetNames[index])}" sheetId="${index + 1}" r:id="rId${index + 1}"/>"""
    }.joinToString("\n    ")}
    |  </sheets>
    |</workbook>
  """.trimMargin()
  }

  private fun workbookRelsXml(sheetCount: Int) = xmlHeader() + """
    |<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    |  ${(1..sheetCount).joinToString("\n  ") { """<Relationship Id="rId$it" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$it.xml"/>""" }}
    |  <Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
    |</Relationships>
  """.trimMargin()

  private fun uniqueSheetNames(worksheets: List<ScoreWorksheet>): List<String> {
    val used = mutableMapOf<String, Int>()
    return worksheets.map { worksheet ->
      val base = sheetName(worksheet)
      val count = used.getOrDefault(base.lowercase(IndonesianLocale), 0) + 1
      used[base.lowercase(IndonesianLocale)] = count
      if (count == 1) {
        base
      } else {
        val suffix = " $count"
        base.take(31 - suffix.length).trim().ifBlank { "Sheet" } + suffix
      }
    }
  }

  private fun sheetName(worksheet: ScoreWorksheet): String {
    return listOf(worksheet.snapshot.subjectTitle.ifBlank { worksheet.subject.title }, worksheet.snapshot.className.ifBlank { worksheet.subject.className })
      .joinToString(" ")
      .replace(Regex("""[\[\]:*?/\\]"""), " ")
      .replace(Regex("\\s+"), " ")
      .trim()
      .take(31)
      .ifBlank { "Nilai Siswa" }
  }

  private fun appXml() = xmlHeader() + """
    |<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
    |  <Application>MIM APP</Application>
    |</Properties>
  """.trimMargin()

  private fun coreXml() = xmlHeader() + """
    |<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    |  <dc:title>Laporan Nilai Siswa</dc:title>
    |  <dc:creator>MIM APP</dc:creator>
    |</cp:coreProperties>
  """.trimMargin()

  private fun stylesXml() = xmlHeader() + """
    |<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    |  <fonts count="4">
    |    <font><sz val="11"/><color rgb="FF182135"/><name val="Calibri"/></font>
    |    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    |    <font><b/><sz val="16"/><color rgb="FF182135"/><name val="Calibri"/></font>
    |    <font><b/><sz val="11"/><color rgb="FF182135"/><name val="Calibri"/></font>
    |  </fonts>
    |  <fills count="6">
    |    <fill><patternFill patternType="none"/></fill>
    |    <fill><patternFill patternType="gray125"/></fill>
    |    <fill><patternFill patternType="solid"><fgColor rgb="FF256D7B"/><bgColor indexed="64"/></patternFill></fill>
    |    <fill><patternFill patternType="solid"><fgColor rgb="FFF8FAFC"/><bgColor indexed="64"/></patternFill></fill>
    |    <fill><patternFill patternType="solid"><fgColor rgb="FFE0ECFF"/><bgColor indexed="64"/></patternFill></fill>
    |    <fill><patternFill patternType="solid"><fgColor rgb="FFFFF7ED"/><bgColor indexed="64"/></patternFill></fill>
    |  </fills>
    |  <borders count="2">
    |    <border><left/><right/><top/><bottom/><diagonal/></border>
    |    <border>
    |      <left style="thin"><color rgb="FFD8DEE9"/></left>
    |      <right style="thin"><color rgb="FFD8DEE9"/></right>
    |      <top style="thin"><color rgb="FFD8DEE9"/></top>
    |      <bottom style="thin"><color rgb="FFD8DEE9"/></bottom>
    |      <diagonal/>
    |    </border>
    |  </borders>
    |  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
    |  <cellXfs count="7">
    |    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
    |    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFill="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    |    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    |    <xf numFmtId="0" fontId="0" fillId="3" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    |    <xf numFmtId="0" fontId="3" fillId="4" borderId="1" xfId="0" applyFill="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    |    <xf numFmtId="0" fontId="0" fillId="5" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf>
    |    <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment vertical="center"/></xf>
    |  </cellXfs>
    |  <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
    |</styleSheet>
  """.trimMargin()
}
