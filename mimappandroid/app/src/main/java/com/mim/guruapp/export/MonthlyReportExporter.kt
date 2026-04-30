package com.mim.guruapp.export

import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.mim.guruapp.BuildConfig
import androidx.core.content.FileProvider
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MonthlyReportWaTarget(
  val label: String,
  val phone: String
)

data class MonthlyReportExportRow(
  val title: String,
  val value: String = "",
  val description: String = ""
)

data class MonthlyReportExportSection(
  val title: String,
  val subtitle: String = "",
  val rows: List<MonthlyReportExportRow> = emptyList()
)

data class MonthlyReportExportExtracurricularRow(
  val activityName: String = "",
  val pjName: String = "",
  val pjPhone: String = "",
  val attendance: String = "",
  val note: String = ""
)

data class MonthlyReportExportData(
  val studentId: String,
  val studentName: String,
  val className: String,
  val periodLabel: String,
  val waliName: String,
  val waliPhone: String,
  val waTargets: List<MonthlyReportWaTarget>,
  val extracurricularRows: List<MonthlyReportExportExtracurricularRow> = emptyList(),
  val sections: List<MonthlyReportExportSection>
)

object MonthlyReportExporter {
  private const val BucketName = "laporan-bulanan"
  private const val PageWidth = 612
  private const val PageHeight = 936
  private const val Margin = 36f
  private val F4MediaSize = PrintAttributes.MediaSize("MIM_F4", "F4", 8500, 13000)

  suspend fun createPdfFile(
    context: Context,
    data: MonthlyReportExportData
  ): File = withContext(Dispatchers.IO) {
    val outputDir = File(context.cacheDir, "laporan-bulanan").apply { mkdirs() }
    val file = File(outputDir, "${buildFileName(data)}.pdf")
    PdfDocument().use { document ->
      drawReport(document, data)
      FileOutputStream(file).use { output -> document.writeTo(output) }
    }
    file
  }

  fun printPdf(
    context: Context,
    pdfFile: File,
    data: MonthlyReportExportData
  ) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = "Laporan Bulanan - ${data.studentName}"
    printManager.print(
      jobName,
      PdfFilePrintAdapter(pdfFile, jobName),
      PrintAttributes.Builder()
        .setMediaSize(F4MediaSize.asPortrait())
        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
        .build()
    )
  }

  suspend fun uploadPdfAndGetPublicUrl(
    pdfFile: File,
    data: MonthlyReportExportData
  ): String = withContext(Dispatchers.IO) {
    val storagePath = buildStoragePath(data)
    val requestUrl = "${BuildConfig.SUPABASE_URL}/storage/v1/object/$BucketName/$storagePath"
    val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      connectTimeout = 20_000
      readTimeout = 30_000
      doOutput = true
      setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
      setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
      setRequestProperty("Content-Type", "application/pdf")
      setRequestProperty("x-upsert", "true")
    }
    pdfFile.inputStream().use { input ->
      connection.outputStream.use { output -> input.copyTo(output) }
    }
    try {
      val code = connection.responseCode
      val payload = connection.readPayload(code in 200..299)
      if (code !in 200..299) {
        throw IllegalStateException(payload.ifBlank { "HTTP $code" })
      }
    } finally {
      connection.disconnect()
    }
    "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$BucketName/$storagePath"
  }

  fun openWhatsApp(
    context: Context,
    phone: String,
    message: String
  ): Boolean {
    val normalizedPhone = normalizeWhatsappNumber(phone)
    if (normalizedPhone.isBlank()) return false
    val encoded = URLEncoder.encode(message, Charsets.UTF_8.name())
    val urls = listOf(
      "whatsapp://send?phone=$normalizedPhone&text=$encoded",
      "https://wa.me/$normalizedPhone?text=$encoded",
      "https://api.whatsapp.com/send?phone=$normalizedPhone&text=$encoded"
    )
    for (url in urls) {
      val opened = runCatching {
        context.startActivity(
          Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
      }.getOrDefault(false)
      if (opened) return true
    }
    return false
  }

  fun sharePdfToWhatsApp(
    context: Context,
    pdfFile: File,
    phone: String,
    message: String
  ): Boolean {
    copyTextToClipboard(context, "Caption laporan bulanan", message)
    val uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      pdfFile
    )
    val normalizedPhone = normalizeWhatsappNumber(phone)

    fun buildIntent(packageName: String? = null): Intent {
      return Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        packageName?.let(::setPackage)
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, message)
        putExtra(Intent.EXTRA_HTML_TEXT, message.replace("\n", "<br>"))
        putExtra(Intent.EXTRA_SUBJECT, pdfFile.nameWithoutExtension)
        putExtra(Intent.EXTRA_TITLE, pdfFile.nameWithoutExtension)
        if (normalizedPhone.isNotBlank()) {
          putExtra("jid", "$normalizedPhone@s.whatsapp.net")
        }
        clipData = ClipData.newUri(context.contentResolver, pdfFile.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
      val chooser = Intent.createChooser(buildIntent(), "Kirim laporan")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      context.startActivity(chooser)
      true
    }.getOrDefault(false)
  }

  private fun copyTextToClipboard(
    context: Context,
    label: String,
    text: String
  ) {
    runCatching {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }
  }

  fun buildWhatsappMessage(
    data: MonthlyReportExportData,
    publicUrl: String
  ): String {
    return defaultMonthlyWhatsappTemplate()
      .replace("<nama santri>", data.studentName, ignoreCase = true)
      .replace("<link>", publicUrl, ignoreCase = true)
  }

  fun buildWhatsappAttachmentMessage(data: MonthlyReportExportData): String {
    return listOf(
      "Assalamu'alaikum warahmatullahi wabarakatuh",
      "",
      "Bapak/Ibu hafizakumullahu ta'ala",
      "",
      "Alhamdulillah kembali menyampaikan Laporan Evaluasi Perkembangan Santri bulan ini ananda ${data.studentName}.",
      "",
      "Mohon dibaca dengan seksama. Jika ada hal yang kurang jelas, Ibu/Bapak dapat menghubungi nomor penanggung jawab yang tertera.",
      "",
      "Laporan ini bisa menjadi catatan muhasabah atas perkembangan ananda selama sebulan di pondok.",
      "",
      "Semoga Allah SWT mengistiqamahkan ananda dalam kebaikan dan menjadikannya pribadi yang lebih baik ke depannya.",
      "",
      "Syukron wajazakumullahu khairan",
      "",
      "PDF laporan terlampir."
    ).joinToString("\n")
  }

  fun normalizeWhatsappNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    if (digits.isBlank()) return ""
    return when {
      digits.startsWith("62") -> digits
      digits.startsWith("0") -> "62${digits.drop(1)}"
      else -> digits
    }
  }

  private fun drawReport(
    document: PdfDocument,
    data: MonthlyReportExportData
  ) {
    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var canvas: Canvas? = null
    val mm = 2.8346457f
    val margin = 25f * mm
    val indent = 8f * mm
    val pageBottom = PageHeight - margin
    val pageContentHeight = PageHeight - (margin * 2f)
    val usableWidth = PageWidth - (margin * 2f)
    val tableX = margin + indent
    val tableColumns = floatArrayOf(
      usableWidth * 0.40f,
      usableWidth * 0.18f,
      usableWidth * 0.34f
    )
    val tableWidth = tableColumns.sum()
    val ekskulColumns = floatArrayOf(
      usableWidth * 0.30f,
      usableWidth * 0.70f
    )
    val ekskulTableWidth = ekskulColumns.sum()
    val black = Color.rgb(17, 24, 39)
    val tableHeaderFill = Color.rgb(237, 211, 127)
    val cellPadding = 2.5f * mm
    val ekskulCellPadding = 2.4f * mm
    val gridStrokeWidth = 0.2f * mm
    var y = margin

    fun dash(value: String): String = value.trim().ifBlank { "-" }

    fun textPaint(
      size: Float,
      style: Int = Typeface.NORMAL,
      align: Paint.Align = Paint.Align.LEFT
    ): Paint {
      return Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = black
        textSize = size
        textAlign = align
        typeface = Typeface.create(Typeface.SERIF, style)
      }
    }

    val titlePaint = textPaint(12f, Typeface.BOLD, Paint.Align.CENTER)
    val sectionPaint = textPaint(12f, Typeface.BOLD)
    val bodyPaint = textPaint(12f)
    val bodyBoldPaint = textPaint(12f, Typeface.BOLD)
    val noteSmallPaint = textPaint(10f)
    val ekskulPaint = textPaint(11f)
    val ekskulBoldPaint = textPaint(11f, Typeface.BOLD)
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = Color.WHITE
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      color = black
      strokeWidth = gridStrokeWidth
    }

    fun startPage() {
      pageNumber += 1
      page = document.startPage(
        PdfDocument.PageInfo.Builder(PageWidth, PageHeight, pageNumber).create()
      )
      canvas = page?.canvas
      y = margin
    }

    fun finishPage() {
      page?.let { document.finishPage(it) }
      page = null
      canvas = null
    }

    fun ensureSpace(required: Float) {
      if (page == null) startPage()
      if (y + required > pageBottom) {
        finishPage()
        startPage()
      }
    }

    fun moveToNextPageIfNeeded(required: Float) {
      if (page == null) startPage()
      if (required <= pageContentHeight && y + required > pageBottom) {
        finishPage()
        startPage()
      }
    }

    fun lineHeight(paint: Paint): Float {
      val metrics = paint.fontMetrics
      return (metrics.descent - metrics.ascent) * 1.15f
    }

    fun wrapText(
      text: String,
      paint: Paint,
      width: Float
    ): List<String> {
      val result = mutableListOf<String>()
      dash(text).split('\n').forEach { paragraph ->
        val words = paragraph.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) {
          result += ""
        } else {
          var line = ""
          words.forEach { word ->
            val candidate = if (line.isBlank()) word else "$line $word"
            if (paint.measureText(candidate) <= width || line.isBlank()) {
              line = candidate
            } else {
              result += line
              line = word
            }
          }
          if (line.isNotBlank()) result += line
        }
      }
      return result.ifEmpty { listOf("-") }
    }

    fun cellHeight(
      text: String,
      paint: Paint,
      width: Float,
      padding: Float
    ): Float {
      val lines = wrapText(text, paint, width - (padding * 2f))
      return (lines.size * lineHeight(paint)) + (padding * 2f)
    }

    fun drawCell(
      x: Float,
      top: Float,
      width: Float,
      height: Float,
      text: String,
      paint: Paint,
      padding: Float = cellPadding,
      align: Paint.Align = Paint.Align.LEFT,
      fillColor: Int = Color.WHITE
    ) {
      fillPaint.color = fillColor
      canvas?.drawRect(x, top, x + width, top + height, fillPaint)
      canvas?.drawRect(x, top, x + width, top + height, borderPaint)

      val drawPaint = Paint(paint).apply { textAlign = align }
      val lines = wrapText(text, drawPaint, width - (padding * 2f))
      val metrics = drawPaint.fontMetrics
      val textLineHeight = lineHeight(drawPaint)
      val textHeight = lines.size * textLineHeight
      var baseline = top + ((height - textHeight) / 2f) - metrics.ascent
      val textX = when (align) {
        Paint.Align.CENTER -> x + (width / 2f)
        Paint.Align.RIGHT -> x + width - padding
        else -> x + padding
      }
      lines.forEach { line ->
        canvas?.drawText(line, textX, baseline, drawPaint)
        baseline += textLineHeight
      }
    }

    fun drawTextLine(text: String, paint: Paint, x: Float, baseline: Float) {
      canvas?.drawText(dash(text), x, baseline, paint)
    }

    fun drawCenteredText(text: String, baseline: Float) {
      canvas?.drawText(dash(text), PageWidth / 2f, baseline, titlePaint)
    }

    fun threeColumnHeaderHeight(): Float {
      val headerPaint = bodyBoldPaint
      return listOf(
        cellHeight("Aspek Penilaian", headerPaint, tableColumns[0], cellPadding),
        cellHeight("Nilai", headerPaint, tableColumns[1], cellPadding),
        cellHeight("Keterangan", headerPaint, tableColumns[2], cellPadding)
      ).maxOrNull() ?: (9f * mm)
    }

    fun drawThreeColumnHeader() {
      val headerPaint = bodyBoldPaint
      val height = threeColumnHeaderHeight()
      if (y + height > pageBottom) {
        finishPage()
        startPage()
      }
      drawCell(tableX, y, tableColumns[0], height, "Aspek Penilaian", headerPaint, align = Paint.Align.CENTER, fillColor = tableHeaderFill)
      drawCell(tableX + tableColumns[0], y, tableColumns[1], height, "Nilai", headerPaint, align = Paint.Align.CENTER, fillColor = tableHeaderFill)
      drawCell(tableX + tableColumns[0] + tableColumns[1], y, tableColumns[2], height, "Keterangan", headerPaint, align = Paint.Align.CENTER, fillColor = tableHeaderFill)
      y += height
    }

    fun isNoteRow(row: MonthlyReportExportRow): Boolean {
      return row.title.trim().startsWith("Catatan", ignoreCase = true)
    }

    fun rowValue(row: MonthlyReportExportRow): String {
      val value = dash(row.value)
      val description = dash(row.description)
      return when {
        value != "-" -> value
        description != "-" && !isNoteRow(row) -> description
        else -> "-"
      }
    }

    fun rowDescription(row: MonthlyReportExportRow): String {
      val value = dash(row.value)
      val description = dash(row.description)
      return when {
        isNoteRow(row) -> description
        value == "-" && description != "-" -> "-"
        else -> description
      }
    }

    fun threeColumnBodyRowHeight(row: MonthlyReportExportRow): Float {
      if (isNoteRow(row)) {
        val notePaint = if (row.title.contains("muhaffiz", ignoreCase = true)) noteSmallPaint else bodyPaint
        val notePadding = if (row.title.contains("muhaffiz", ignoreCase = true)) 2f * mm else cellPadding
        return cellHeight("${dash(row.title)}:\n${dash(row.description)}", notePaint, tableWidth, notePadding)
      }
      val value = rowValue(row)
      val description = rowDescription(row)
      return listOf(
        cellHeight(dash(row.title), bodyPaint, tableColumns[0], cellPadding),
        cellHeight(value, bodyPaint, tableColumns[1], cellPadding),
        cellHeight(description, bodyPaint, tableColumns[2], cellPadding)
      ).maxOrNull() ?: (9f * mm)
    }

    fun drawThreeColumnBodyRow(row: MonthlyReportExportRow) {
      if (isNoteRow(row)) {
        val notePaint = if (row.title.contains("muhaffiz", ignoreCase = true)) noteSmallPaint else bodyPaint
        val notePadding = if (row.title.contains("muhaffiz", ignoreCase = true)) 2f * mm else cellPadding
        val noteText = "${dash(row.title)}:\n${dash(row.description)}"
        val height = cellHeight(noteText, notePaint, tableWidth, notePadding)
        if (y + height > pageBottom) {
          finishPage()
          startPage()
          drawThreeColumnHeader()
        }
        drawCell(tableX, y, tableWidth, height, noteText, notePaint, padding = notePadding)
        y += height
        return
      }

      val value = rowValue(row)
      val description = rowDescription(row)
      val height = listOf(
        cellHeight(dash(row.title), bodyPaint, tableColumns[0], cellPadding),
        cellHeight(value, bodyPaint, tableColumns[1], cellPadding),
        cellHeight(description, bodyPaint, tableColumns[2], cellPadding)
      ).maxOrNull() ?: (9f * mm)
      if (y + height > pageBottom) {
        finishPage()
        startPage()
        drawThreeColumnHeader()
      }
      drawCell(tableX, y, tableColumns[0], height, row.title, bodyPaint)
      drawCell(tableX + tableColumns[0], y, tableColumns[1], height, value, bodyPaint, align = Paint.Align.CENTER)
      drawCell(tableX + tableColumns[0] + tableColumns[1], y, tableColumns[2], height, description, bodyPaint)
      y += height
    }

    fun threeColumnTableHeight(rows: List<MonthlyReportExportRow>): Float {
      return threeColumnHeaderHeight() + rows.sumOf { threeColumnBodyRowHeight(it).toDouble() }.toFloat()
    }

    fun drawThreeColumnTable(rows: List<MonthlyReportExportRow>) {
      moveToNextPageIfNeeded(threeColumnTableHeight(rows))
      drawThreeColumnHeader()
      rows.forEach { row -> drawThreeColumnBodyRow(row) }
    }

    fun drawSectionTitle(title: String) {
      ensureSpace(10f * mm)
      drawTextLine(title, sectionPaint, margin, y)
      y += 6f * mm
    }

    fun drawSectionIdentity(label: String, name: String, phone: String) {
      drawTextLine("$label: ${dash(name)}", bodyPaint, margin + indent, y)
      y += 7f * mm
      drawTextLine("Nomor HP: ${dash(phone)}", bodyPaint, margin + indent, y)
      y += 5f * mm
    }

    fun drawAcademicSection(rows: List<MonthlyReportExportRow>) {
      val sectionHeight = (6f * mm) + (12f * mm) + threeColumnTableHeight(rows) + (10f * mm)
      moveToNextPageIfNeeded(sectionHeight)
      drawSectionTitle("A. Laporan Akademik")
      drawSectionIdentity("Wali Kelas", data.waliName, data.waliPhone)
      drawThreeColumnTable(rows)
      y += 10f * mm
    }

    fun drawTahfizSection(rows: List<MonthlyReportExportRow>) {
      val sectionHeight = (6f * mm) + (12f * mm) + threeColumnTableHeight(rows) + (10f * mm)
      moveToNextPageIfNeeded(sectionHeight)
      drawSectionTitle("B. Laporan Ketahfizan")
      val mentor = data.sections.getOrNull(1)?.subtitle.orEmpty()
      val muhaffizName = mentor.substringAfter("Muhaffiz:", "").substringBefore("|").trim().ifBlank { "-" }
      val muhaffizPhone = mentor.substringAfter("HP:", "").trim().ifBlank { "-" }
      drawSectionIdentity("Muhaffiz", muhaffizName, muhaffizPhone)
      drawThreeColumnTable(rows)
      y += 10f * mm
    }

    fun drawKesantrianSection(rows: List<MonthlyReportExportRow>) {
      finishPage()
      startPage()
      drawSectionTitle("C. Laporan Kesantrian")
      val mentor = data.sections.getOrNull(2)?.subtitle.orEmpty()
      val musyrifName = mentor.substringAfter("Musyrif:", "").substringBefore("|").trim().ifBlank { "-" }
      val musyrifPhone = mentor.substringAfter("HP:", "").trim().ifBlank { "-" }
      drawSectionIdentity("Musyrif", musyrifName, musyrifPhone)
      drawThreeColumnTable(rows)
      y += 10f * mm
    }

    fun drawEkskulRow(
      label: String,
      value: String,
      fillFirstColumn: Boolean = true
    ) {
      val leftHeight = cellHeight(label, ekskulPaint, ekskulColumns[0], ekskulCellPadding)
      val rightHeight = cellHeight(value, ekskulPaint, ekskulColumns[1], ekskulCellPadding)
      val height = maxOf(leftHeight, rightHeight)
      if (y + height > pageBottom) {
        finishPage()
        startPage()
      }
      drawCell(
        tableX,
        y,
        ekskulColumns[0],
        height,
        label,
        ekskulPaint,
        padding = ekskulCellPadding,
        fillColor = if (fillFirstColumn) tableHeaderFill else Color.WHITE
      )
      drawCell(
        tableX + ekskulColumns[0],
        y,
        ekskulColumns[1],
        height,
        value,
        ekskulPaint,
        padding = ekskulCellPadding
      )
      y += height
    }

    fun ekskulRowHeight(
      label: String,
      value: String
    ): Float {
      val leftHeight = cellHeight(label, ekskulPaint, ekskulColumns[0], ekskulCellPadding)
      val rightHeight = cellHeight(value, ekskulPaint, ekskulColumns[1], ekskulCellPadding)
      return maxOf(leftHeight, rightHeight)
    }

    fun drawEkskulNote(text: String) {
      val noteText = "Catatan PJ : ${dash(text)}"
      val height = cellHeight(noteText, ekskulBoldPaint, ekskulTableWidth, ekskulCellPadding)
      if (y + height > pageBottom) {
        finishPage()
        startPage()
      }
      drawCell(tableX, y, ekskulTableWidth, height, noteText, ekskulBoldPaint, padding = ekskulCellPadding)
      y += height
    }

    fun ekskulNoteHeight(text: String): Float {
      val noteText = "Catatan PJ : ${dash(text)}"
      return cellHeight(noteText, ekskulBoldPaint, ekskulTableWidth, ekskulCellPadding)
    }

    fun ekskulTableHeight(item: MonthlyReportExportExtracurricularRow): Float {
      return ekskulRowHeight("Kegiatan Ekstrakurikuler", dash(item.activityName)) +
        ekskulRowHeight("Penanggung Jawab (PJ)", dash(item.pjName)) +
        ekskulRowHeight("No. HP PJ", dash(item.pjPhone)) +
        ekskulRowHeight("Kehadiran", dash(item.attendance)) +
        ekskulNoteHeight(item.note) +
        (2f * mm)
    }

    fun drawEkskulTable(item: MonthlyReportExportExtracurricularRow) {
      moveToNextPageIfNeeded(ekskulTableHeight(item))
      drawEkskulRow("Kegiatan Ekstrakurikuler", dash(item.activityName))
      drawEkskulRow("Penanggung Jawab (PJ)", dash(item.pjName))
      drawEkskulRow("No. HP PJ", dash(item.pjPhone))
      drawEkskulRow("Kehadiran", dash(item.attendance))
      drawEkskulNote(item.note)
      y += 2f * mm
    }

    fun drawEkskulSection(rows: List<MonthlyReportExportExtracurricularRow>) {
      val normalizedRows = rows.ifEmpty {
        listOf(MonthlyReportExportExtracurricularRow(activityName = "-", pjName = "-", pjPhone = "-", attendance = "-", note = "-"))
      }
      val firstTableHeight = normalizedRows.firstOrNull()?.let(::ekskulTableHeight) ?: 0f
      moveToNextPageIfNeeded((6f * mm) + firstTableHeight)
      drawSectionTitle("D. Laporan Ekstrakulikuler")
      normalizedRows.forEachIndexed { index, item ->
        if (index > 0) y += 4f * mm
        drawEkskulTable(item)
      }
    }

    startPage()
    drawCenteredText("LAPORAN EVALUASI SANTRI", y)
    y += 6f * mm
    drawCenteredText("Periode ${dash(data.periodLabel)}", y)
    y += 12f * mm
    drawTextLine("Nama: ${dash(data.studentName)}", bodyBoldPaint, margin, y)
    y += 7f * mm
    drawTextLine("Kelas: ${dash(data.className)}", bodyBoldPaint, margin, y)
    y += 12f * mm

    drawAcademicSection(data.sections.getOrNull(0)?.rows.orEmpty())
    drawTahfizSection(data.sections.getOrNull(1)?.rows.orEmpty())
    drawKesantrianSection(data.sections.getOrNull(2)?.rows.orEmpty())
    drawEkskulSection(data.extracurricularRows)
    finishPage()
  }

  private fun buildFileName(data: MonthlyReportExportData): String {
    val period = sanitizeFileNamePart(data.periodLabel).ifBlank { "Periode" }
    val name = sanitizeFileNamePart(data.studentName).ifBlank { "Santri" }
    return "Laporan Evaluasi Bulan $period - $name"
  }

  private fun buildStoragePath(data: MonthlyReportExportData): String {
    val period = sanitizeFileNamePart(data.periodLabel).replace(" ", "-").ifBlank { "periode" }
    val name = sanitizeFileNamePart(data.studentName).replace(" ", "-").ifBlank { data.studentId.ifBlank { "santri" } }
    return "android/$period/$name-${System.currentTimeMillis()}.pdf"
  }

  private fun sanitizeFileNamePart(value: String): String {
    return value
      .trim()
      .replace(Regex("""[\\/:*?"<>|]"""), "")
      .replace(Regex("\\s+"), " ")
      .take(90)
  }

  private fun defaultMonthlyWhatsappTemplate(): String {
    return listOf(
      "Assalamu'alaikum warahmatullahi wabarakatuh",
      "",
      "Bapak/Ibu hafizakumullahu ta'ala",
      "",
      "Alhamdulillah kembali menyampaikan Laporan Evaluasi Perkembangan Santri bulan ini ananda <nama santri>.",
      "",
      "Mohon dibaca dengan seksama dan jika ada hal yang kurang jelas maka Ibu/Bapak bisa menanyakan secara langsung dengan menghubungi nomor penanggung jawab yang tertera.",
      "",
      "Laporan ini bisa menjadi catatan muhasabah untuk Ibu/Bapak atas perkembangan ananda selama sebulan di pondok.",
      "",
      "Semoga Allah SWT mengistiqamahkan ananda dalam kebaikan dan menjadikannya pribadi yang lebih baik ke depannya.",
      "",
      "Syukron wajazakumullahu khairan",
      "",
      "Link laporan:",
      "<link>"
    ).joinToString("\n")
  }
}

private class PdfFilePrintAdapter(
  private val pdfFile: File,
  private val jobName: String
) : PrintDocumentAdapter() {
  override fun onLayout(
    oldAttributes: PrintAttributes?,
    newAttributes: PrintAttributes?,
    cancellationSignal: CancellationSignal?,
    callback: LayoutResultCallback?,
    extras: Bundle?
  ) {
    if (cancellationSignal?.isCanceled == true) {
      callback?.onLayoutCancelled()
      return
    }
    callback?.onLayoutFinished(
      PrintDocumentInfo.Builder(jobName)
        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
        .build(),
      true
    )
  }

  override fun onWrite(
    pages: Array<out PageRange>?,
    destination: ParcelFileDescriptor?,
    cancellationSignal: CancellationSignal?,
    callback: WriteResultCallback?
  ) {
    if (destination == null) {
      callback?.onWriteFailed("Tujuan print tidak tersedia.")
      return
    }
    if (cancellationSignal?.isCanceled == true) {
      callback?.onWriteCancelled()
      return
    }
    runCatching {
      pdfFile.inputStream().use { input ->
        FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
      }
    }.onSuccess {
      callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    }.onFailure { error ->
      callback?.onWriteFailed(error.message ?: "Gagal menulis PDF.")
    }
  }
}

private inline fun <T> PdfDocument.use(block: (PdfDocument) -> T): T {
  return try {
    block(this)
  } finally {
    close()
  }
}

private fun HttpURLConnection.readPayload(useInputStream: Boolean): String {
  val stream = if (useInputStream) inputStream else errorStream
  if (stream == null) return ""
  return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
    reader.lineSequence().joinToString(separator = "")
  }
}
