package com.mim.guruapp.export

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mim.guruapp.data.model.AttendanceHistoryEntry
import com.mim.guruapp.data.model.AttendanceStudent
import com.mim.guruapp.data.model.MapelAttendanceSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt

object MapelAttendanceExcelExporter {
  private const val XlsxMime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  private val IndonesianLocale = Locale("id", "ID")
  private val MonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", IndonesianLocale)
  private val DayFormatter = DateTimeFormatter.ofPattern("d MMM", IndonesianLocale)
  private val SheetFormatter = DateTimeFormatter.ofPattern("MMM yyyy", IndonesianLocale)

  data class AttendanceWorksheet(
    val period: YearMonth,
    val snapshot: MapelAttendanceSnapshot,
    val reportTitle: String = "LAPORAN ABSENSI MAPEL",
    val subjectLabel: String = "Mapel"
  )

  suspend fun createWorkbook(
    context: Context,
    worksheets: List<AttendanceWorksheet>
  ): File = withContext(Dispatchers.IO) {
    val safeWorksheets = worksheets.sortedBy { it.period }.ifEmpty {
      val now = YearMonth.now()
      listOf(
        AttendanceWorksheet(
          period = now,
          snapshot = MapelAttendanceSnapshot(
            distribusiId = "",
            subjectTitle = "Mapel",
            className = "Kelas",
            rangeLabel = "",
            students = emptyList(),
            updatedAt = System.currentTimeMillis()
          )
        )
      )
    }
    val exportDir = File(context.getExternalFilesDir(null), "exports/absensi-mapel").apply { mkdirs() }
    val firstSnapshot = safeWorksheets.first().snapshot
    val safePeriod = if (safeWorksheets.size == 1) {
      safeWorksheets.first().period.toString()
    } else {
      "${safeWorksheets.first().period}_${safeWorksheets.last().period}"
    }
    val safeSubject = listOf(firstSnapshot.subjectTitle, firstSnapshot.className)
      .joinToString("_")
      .replace(Regex("[^A-Za-z0-9._-]+"), "_")
      .trim('_')
      .ifBlank { "mapel" }
    val file = File(exportDir, "absensi_${safeSubject}_$safePeriod.xlsx")

    ZipOutputStream(file.outputStream()).use { zip ->
      zip.putText("[Content_Types].xml", contentTypesXml(safeWorksheets.size))
      zip.putText("_rels/.rels", rootRelsXml())
      zip.putText("docProps/app.xml", appXml())
      zip.putText("docProps/core.xml", coreXml())
      zip.putText("xl/workbook.xml", workbookXml(safeWorksheets))
      zip.putText("xl/_rels/workbook.xml.rels", workbookRelsXml(safeWorksheets.size))
      zip.putText("xl/styles.xml", stylesXml())
      safeWorksheets.forEachIndexed { index, worksheet ->
        zip.putText(
          "xl/worksheets/sheet${index + 1}.xml",
          worksheetXml(worksheet)
        )
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
      context.startActivity(Intent.createChooser(intent, "Buka file absensi").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
      shareWorkbook(context, file)
    }
  }

  fun shareWorkbook(context: Context, file: File) {
    val uri = file.toContentUri(context)
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = XlsxMime
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, "Absensi Mapel")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
      clipData = ClipData.newUri(context.contentResolver, file.name, uri)
    }
    context.startActivity(Intent.createChooser(intent, "Kirim absensi").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }

  private fun File.toContentUri(context: Context) =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", this)

  private fun worksheetXml(worksheet: AttendanceWorksheet): String {
    val period = worksheet.period
    val snapshot = worksheet.snapshot
    val dates = snapshot.students
      .flatMap { student -> student.history.mapNotNull { entry -> parseDate(entry.dateIso) } }
      .filter { YearMonth.from(it) == period }
      .distinct()
      .sorted()
    val rows = buildRows(worksheet, dates)
    val lastColumn = 2 + dates.size + 3
    val titleMerge = "A1:${columnName(lastColumn)}1"
    val mapelMerge = "A2:${columnName(lastColumn)}2"
    val classMerge = "A3:${columnName(lastColumn)}3"
    val cols = buildString {
      append("<cols>")
      append("""<col min="1" max="1" width="6" customWidth="1"/>""")
      append("""<col min="2" max="2" width="30" customWidth="1"/>""")
      if (dates.isNotEmpty()) {
        append("""<col min="3" max="${dates.size + 2}" width="13" customWidth="1"/>""")
      }
      append("""<col min="${dates.size + 3}" max="$lastColumn" width="12" customWidth="1"/>""")
      append("</cols>")
    }

    return xmlHeader() + """
      |<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
      |  <sheetViews><sheetView workbookViewId="0" showGridLines="1"/></sheetViews>
      |  <sheetFormatPr defaultRowHeight="18"/>
      |  $cols
      |  <sheetData>
      |    ${rows.joinToString("\n") { it.toXml() }}
      |  </sheetData>
      |  <mergeCells count="3">
      |    <mergeCell ref="$titleMerge"/>
      |    <mergeCell ref="$mapelMerge"/>
      |    <mergeCell ref="$classMerge"/>
      |  </mergeCells>
      |  <pageMargins left="0.35" right="0.35" top="0.55" bottom="0.55" header="0.2" footer="0.2"/>
      |</worksheet>
    """.trimMargin()
  }

  private fun buildRows(
    worksheet: AttendanceWorksheet,
    dates: List<LocalDate>
  ): List<SheetRow> {
    val period = worksheet.period
    val snapshot = worksheet.snapshot
    val rows = mutableListOf<SheetRow>()
    val lastColumn = 2 + dates.size + 3
    rows += SheetRow(1, listOf(cell(1, 1, worksheet.reportTitle, 2)))
    rows += SheetRow(2, listOf(cell(1, 2, "${worksheet.subjectLabel}: ${snapshot.subjectTitle.ifBlank { "-" }}", 8)))
    rows += SheetRow(3, listOf(cell(1, 3, "Kelas: ${snapshot.className.ifBlank { "-" }}", 8)))
    rows += SheetRow(4, listOf(cell(1, 4, "Periode: ${MonthFormatter.format(period.atDay(1))}", 8)))

    val headerCells = mutableListOf(
      cell(1, 6, "No", 1),
      cell(2, 6, "Nama Santri", 1)
    )
    dates.forEachIndexed { index, date ->
      headerCells += cell(index + 3, 6, DayFormatter.format(date), 1)
    }
    headerCells += cell(dates.size + 3, 6, "% Hadir", 1)
    headerCells += cell(dates.size + 4, 6, "% Izin", 1)
    headerCells += cell(dates.size + 5, 6, "% Sakit", 1)
    rows += SheetRow(6, headerCells)

    snapshot.students
      .sortedBy { it.name.lowercase(IndonesianLocale) }
      .forEachIndexed { index, student ->
        val rowIndex = index + 7
        val historyByDate = student.history
          .filter { entry -> parseDate(entry.dateIso)?.let { YearMonth.from(it) == period } == true }
          .groupBy { parseDate(it.dateIso)?.toString().orEmpty() }
          .mapValues { (_, entries) -> entries.resolveDailyStatus() }
        val rowCells = mutableListOf(
          cell(1, rowIndex, (index + 1).toString(), 3),
          cell(2, rowIndex, student.name, 0)
        )
        val statuses = dates.map { date -> historyByDate[date.toString()] ?: "-" }
        statuses.forEachIndexed { dateIndex, status ->
          rowCells += cell(dateIndex + 3, rowIndex, status, status.toStyle())
        }
        val counted = statuses.filter { it != "-" }
        val hadirCount = counted.count { it == "Hadir" || it == "Terlambat" }
        val izinCount = counted.count { it == "Izin" }
        val sakitCount = counted.count { it == "Sakit" }
        rowCells += cell(dates.size + 3, rowIndex, counted.percentText(hadirCount), 7)
        rowCells += cell(dates.size + 4, rowIndex, counted.percentText(izinCount), 7)
        rowCells += cell(dates.size + 5, rowIndex, counted.percentText(sakitCount), 7)
        rows += SheetRow(rowIndex, rowCells)
      }

    if (snapshot.students.isEmpty()) {
      rows += SheetRow(7, listOf(cell(1, 7, "Belum ada data santri.", 8)))
    } else if (dates.isEmpty()) {
      rows += SheetRow(7 + snapshot.students.size, listOf(cell(1, 7 + snapshot.students.size, "Belum ada tanggal absensi pada periode ini.", 8)))
    }
    return rows
  }

  private fun List<AttendanceHistoryEntry>.resolveDailyStatus(): String {
    val statuses = map { it.status.normalizeStatus() }.filter { it.isNotBlank() }
    return when {
      statuses.any { it == "Hadir" || it == "Terlambat" } -> "Hadir"
      statuses.any { it == "Sakit" } -> "Sakit"
      statuses.any { it == "Izin" } -> "Izin"
      statuses.any { it == "Alpa" } -> "Alpa"
      else -> "-"
    }
  }

  private fun String.normalizeStatus(): String =
    when (trim().lowercase(IndonesianLocale)) {
      "hadir", "masuk" -> "Hadir"
      "terlambat", "telat" -> "Terlambat"
      "sakit" -> "Sakit"
      "izin", "ijin" -> "Izin"
      "alpa", "alpha", "tidak hadir" -> "Alpa"
      else -> trim()
    }

  private fun List<String>.percentText(count: Int): String =
    if (isEmpty()) "-" else "${((count.toDouble() / size.toDouble()) * 100.0).roundToInt()}%"

  private fun String.toStyle(): Int =
    when (normalizeStatus()) {
      "Hadir" -> 4
      "Terlambat" -> 5
      "Sakit" -> 6
      "Izin" -> 7
      "Alpa" -> 9
      else -> 3
    }

  private fun parseDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim().take(10)) }.getOrNull()

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

  private fun workbookXml(worksheets: List<AttendanceWorksheet>) = xmlHeader() + """
    |<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    |  <sheets>
    |    ${worksheets.mapIndexed { index, worksheet ->
      """<sheet name="${escapeXml(worksheet.period.toSheetName())}" sheetId="${index + 1}" r:id="rId${index + 1}"/>"""
    }.joinToString("\n    ")}
    |  </sheets>
    |</workbook>
  """.trimMargin()

  private fun workbookRelsXml(sheetCount: Int) = xmlHeader() + """
    |<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    |  ${(1..sheetCount).joinToString("\n  ") { """<Relationship Id="rId$it" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$it.xml"/>""" }}
    |  <Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
    |</Relationships>
  """.trimMargin()

  private fun YearMonth.toSheetName(): String =
    SheetFormatter.format(atDay(1))
      .replace(Regex("""[\[\]:*?/\\]"""), " ")
      .replace(Regex("\\s+"), " ")
      .trim()
      .take(31)
      .ifBlank { toString() }

  private fun appXml() = xmlHeader() + """
    |<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
    |  <Application>MIM APP</Application>
    |</Properties>
  """.trimMargin()

  private fun coreXml() = xmlHeader() + """
    |<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    |  <dc:title>Laporan Absensi Mapel</dc:title>
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
    |  <fills count="10">
    |    <fill><patternFill patternType="none"/></fill>
    |    <fill><patternFill patternType="gray125"/></fill>
    |    <fill><patternFill patternType="solid"><fgColor rgb="FF256D7B"/><bgColor indexed="64"/></patternFill></fill>
    |    <fill><patternFill patternType="solid"><fgColor rgb="FFF8FAFC"/><bgColor indexed="64"/></patternFill></fill>
    |    <fill><patternFill patternType="solid"><fgColor rgb="FFDDF5E8"/><bgColor indexed="64"/></patternFill></fill>
    |    <fill><patternFill patternType="solid"><fgColor rgb="FFFFF1D6"/><bgColor indexed="64"/></patternFill></fill>
    |    <fill><patternFill patternType="solid"><fgColor rgb="FFF1E7FF"/><bgColor indexed="64"/></patternFill></fill>
    |    <fill><patternFill patternType="solid"><fgColor rgb="FFE0ECFF"/><bgColor indexed="64"/></patternFill></fill>
    |    <fill><patternFill patternType="solid"><fgColor rgb="FFEAF3F5"/><bgColor indexed="64"/></patternFill></fill>
    |    <fill><patternFill patternType="solid"><fgColor rgb="FFFFE1E1"/><bgColor indexed="64"/></patternFill></fill>
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
    |  <cellXfs count="10">
    |    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
    |    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFill="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    |    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    |    <xf numFmtId="0" fontId="0" fillId="3" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    |    <xf numFmtId="0" fontId="3" fillId="4" borderId="1" xfId="0" applyFill="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    |    <xf numFmtId="0" fontId="3" fillId="5" borderId="1" xfId="0" applyFill="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    |    <xf numFmtId="0" fontId="3" fillId="6" borderId="1" xfId="0" applyFill="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    |    <xf numFmtId="0" fontId="3" fillId="7" borderId="1" xfId="0" applyFill="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    |    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment vertical="center"/></xf>
    |    <xf numFmtId="0" fontId="3" fillId="9" borderId="1" xfId="0" applyFill="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    |  </cellXfs>
    |  <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
    |</styleSheet>
  """.trimMargin()
}
