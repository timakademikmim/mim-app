package com.mim.guruapp.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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
import androidx.core.content.FileProvider
import com.mim.guruapp.BuildConfig
import com.mim.guruapp.data.model.UtsReportPayload
import com.mim.guruapp.data.model.UtsReportSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class UtsReportWaTarget(
  val label: String,
  val phone: String
)

object UtsReportExporter {
  private const val BucketName = "laporan-uts"
  private const val PageWidth = 595
  private const val PageHeight = 842
  private val A4MediaSize = PrintAttributes.MediaSize.ISO_A4

  suspend fun createPdfFile(
    context: Context,
    data: UtsReportPayload
  ): File = withContext(Dispatchers.IO) {
    val outputDir = File(context.cacheDir, "laporan-pts").apply { mkdirs() }
    val file = File(outputDir, "${buildFileName(data)}.pdf")
    PdfDocument().use { document ->
      drawReport(context, document, data)
      FileOutputStream(file).use { output -> document.writeTo(output) }
    }
    file
  }

  fun printPdf(
    context: Context,
    pdfFile: File,
    data: UtsReportPayload
  ) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = "Laporan PTS - ${data.studentName}"
    printManager.print(
      jobName,
      UtsPdfFilePrintAdapter(pdfFile, jobName),
      PrintAttributes.Builder()
        .setMediaSize(A4MediaSize.asPortrait())
        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
        .build()
    )
  }

  suspend fun uploadPdfAndGetPublicUrl(
    pdfFile: File,
    data: UtsReportPayload
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
      if (code !in 200..299) throw IllegalStateException(payload.ifBlank { "HTTP $code" })
    } finally {
      connection.disconnect()
    }
    "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$BucketName/$storagePath"
  }

  fun sharePdfToWhatsApp(
    context: Context,
    pdfFile: File,
    phone: String,
    message: String
  ): Boolean {
    copyTextToClipboard(context, "Caption laporan PTS", message)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
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
      val chooser = Intent.createChooser(buildIntent(), "Kirim laporan PTS")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      context.startActivity(chooser)
      true
    }.getOrDefault(false)
  }

  fun buildWhatsappAttachmentMessage(data: UtsReportPayload): String {
    return listOf(
      "Assalamu'alaikum warahmatullahi wabarakatuh",
      "",
      "Bapak/Ibu hafizakumullahu ta'ala",
      "",
      "Berikut kami lampirkan Laporan Hasil Ujian Sumatif Tengah Semester ananda ${data.studentName}.",
      "",
      "Mohon dibaca dengan seksama. Jika ada hal yang perlu dikonfirmasi, silakan menghubungi wali kelas.",
      "",
      "Syukron wajazakumullahu khairan.",
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
    context: Context,
    document: PdfDocument,
    data: UtsReportPayload
  ) {
    val mm = 2.8346457f
    val margin = 25.4f * mm
    val baseTextSize = 10f
    val bodyLineGap = 4.8f * mm
    val topStartY = 42.6f * mm
    val pageBottom = PageHeight - (10f * mm)
    val black = Color.BLACK
    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var canvas: Canvas? = null
    var y = topStartY
    val background = runCatching {
      context.assets.open("background_rapor.png").use(BitmapFactory::decodeStream)
    }.getOrNull()

    fun textPaint(
      size: Float = baseTextSize,
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

    val titlePaint = textPaint(baseTextSize, Typeface.BOLD, Paint.Align.CENTER)
    val bodyPaint = textPaint(baseTextSize)
    val bodyBoldPaint = textPaint(baseTextSize, Typeface.BOLD)
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = black
      style = Paint.Style.STROKE
      strokeWidth = 0.18f * mm
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      style = Paint.Style.FILL
    }

    fun startPage() {
      pageNumber += 1
      page = document.startPage(PdfDocument.PageInfo.Builder(PageWidth, PageHeight, pageNumber).create())
      canvas = page?.canvas
      y = topStartY
      background?.let { bitmap ->
        canvas?.drawBitmap(bitmap, null, RectF(0f, 0f, PageWidth.toFloat(), PageHeight.toFloat()), null)
      }
    }

    fun finishPage() {
      page?.let(document::finishPage)
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

    fun wrapText(text: String, paint: Paint, width: Float): List<String> {
      val words = text.ifBlank { "-" }.split(Regex("\\s+")).filter { it.isNotBlank() }
      if (words.isEmpty()) return listOf("-")
      val lines = mutableListOf<String>()
      var line = ""
      words.forEach { word ->
        val candidate = if (line.isBlank()) word else "$line $word"
        if (paint.measureText(candidate) <= width || line.isBlank()) {
          line = candidate
        } else {
          lines += line
          line = word
        }
      }
      if (line.isNotBlank()) lines += line
      return lines
    }

    fun lineHeight(paint: Paint): Float {
      val metrics = paint.fontMetrics
      return (metrics.descent - metrics.ascent) * 1.12f
    }

    fun cellHeight(text: String, paint: Paint, width: Float, padding: Float): Float {
      return wrapText(text, paint, width - (padding * 2f)).size * lineHeight(paint) + (padding * 2f)
    }

    fun drawCell(
      x: Float,
      top: Float,
      width: Float,
      height: Float,
      text: String,
      paint: Paint,
      align: Paint.Align = Paint.Align.LEFT,
      padding: Float = 1.8f * mm
    ) {
      canvas?.drawRect(x, top, x + width, top + height, fillPaint)
      canvas?.drawRect(x, top, x + width, top + height, linePaint)
      val drawPaint = Paint(paint).apply { textAlign = align }
      val lines = wrapText(text.ifBlank { "-" }, drawPaint, width - (padding * 2f))
      val textHeight = lines.size * lineHeight(drawPaint)
      var baseline = top + ((height - textHeight) / 2f) - drawPaint.fontMetrics.ascent
      val textX = when (align) {
        Paint.Align.CENTER -> x + width / 2f
        Paint.Align.RIGHT -> x + width - padding
        else -> x + padding
      }
      lines.forEach { line ->
        canvas?.drawText(line, textX, baseline, drawPaint)
        baseline += lineHeight(drawPaint)
      }
    }

    fun drawFieldRow(label: String, value: String) {
      val fieldLabelX = margin
      val fieldColonX = margin + (29f * mm)
      val fieldValueX = margin + (33f * mm)
      val fieldValueWidth = PageWidth - margin - fieldValueX
      val lines = wrapText(value.ifBlank { "-" }, bodyPaint, fieldValueWidth)
      canvas?.drawText(label, fieldLabelX, y, bodyPaint)
      canvas?.drawText(":", fieldColonX, y, bodyPaint)
      lines.forEachIndexed { index, line ->
        canvas?.drawText(line, fieldValueX, y + (index * bodyLineGap), bodyPaint)
      }
      y += maxOf(bodyLineGap, lines.size * bodyLineGap)
    }

    fun drawTable(
      x: Float,
      columns: FloatArray,
      headers: List<String>,
      rows: List<List<String>>,
      centerColumns: Set<Int> = emptySet()
    ) {
      val padding = 1.8f * mm
      val headerHeight = headers.mapIndexed { index, text ->
        cellHeight(text, bodyBoldPaint, columns[index], padding)
      }.maxOrNull() ?: (7f * mm)
      ensureSpace(headerHeight)
      var cx = x
      headers.forEachIndexed { index, text ->
        drawCell(cx, y, columns[index], headerHeight, text, bodyBoldPaint, Paint.Align.CENTER, padding)
        cx += columns[index]
      }
      y += headerHeight
      rows.forEach { row ->
        if (row.firstOrNull() in listOf("Jumlah", "Rata-Rata") && columns.size >= 4) {
          val labelWidth = columns.take(3).sum()
          val valueWidth = columns[3]
          val rowHeight = maxOf(
            cellHeight(row.first(), bodyBoldPaint, labelWidth, padding),
            cellHeight(row.getOrNull(3).orEmpty(), bodyBoldPaint, valueWidth, padding)
          )
          ensureSpace(rowHeight)
          drawCell(x, y, labelWidth, rowHeight, row.first(), bodyBoldPaint, Paint.Align.CENTER, padding)
          drawCell(x + labelWidth, y, valueWidth, rowHeight, row.getOrNull(3).orEmpty(), bodyBoldPaint, Paint.Align.CENTER, padding)
          y += rowHeight
          return@forEach
        }
        val rowHeight = row.mapIndexed { index, text ->
          cellHeight(text, bodyPaint, columns[index], padding)
        }.maxOrNull() ?: (7f * mm)
        ensureSpace(rowHeight)
        cx = x
        row.forEachIndexed { index, text ->
          drawCell(
            cx,
            y,
            columns[index],
            rowHeight,
            text,
            if (row.firstOrNull() in listOf("Jumlah", "Rata-Rata")) bodyBoldPaint else bodyPaint,
            if (index in centerColumns || row.firstOrNull() in listOf("Jumlah", "Rata-Rata")) Paint.Align.CENTER else Paint.Align.LEFT,
            padding
          )
          cx += columns[index]
        }
        y += rowHeight
      }
    }

    val semesterSuffix = when {
      data.semesterLabel.contains("genap", ignoreCase = true) -> "GENAP"
      data.semesterLabel.contains("ganjil", ignoreCase = true) -> "GANJIL"
      else -> data.semesterLabel.uppercase(Locale.ROOT).ifBlank { "GANJIL" }
    }
    val schoolName = "PESANTREN QUR`AN PUTRA MARKAZ IMAM MALIK"
    val subjectTableWidth = 142.4f * mm
    val subjectTableX = (PageWidth - subjectTableWidth) / 2f
    val subjectColumns = floatArrayOf(9.9f * mm, 62.5f * mm, 32.5f * mm, 37.5f * mm)
    val midTableWidth = 142.5f * mm
    val midTableX = (PageWidth - midTableWidth) / 2f
    val midColumns = floatArrayOf(78.7f * mm, 63.8f * mm)
    val attendanceTableX = subjectTableX
    val attendanceColumns = floatArrayOf(9.5f * mm, 37.0f * mm, 27.5f * mm, 25.0f * mm)

    startPage()
    canvas?.drawText("LAPORAN HASIL UJIAN SUMATIF TENGAH SEMESTER (USTS) $semesterSuffix", PageWidth / 2f, y, titlePaint)
    y += bodyLineGap
    canvas?.drawText(schoolName, PageWidth / 2f, y, titlePaint)
    y += 9f * mm

    drawFieldRow("NAMA SANTRI", data.studentName)
    drawFieldRow("NISN", data.studentNisn)
    drawFieldRow("KELAS", data.className)
    canvas?.drawLine(margin, y, PageWidth - margin, y, linePaint)
    y += 7f * mm

    canvas?.drawText("A. MATA PELAJARAN", margin, y, bodyBoldPaint)
    y += 4.5f * mm
    val subjectRows = data.subjects.mapIndexed { index, subject: UtsReportSubject ->
      listOf((index + 1).toString(), subject.name, subject.kkmText.ifBlank { "17" }, subject.scoreText.ifBlank { "-" })
    } + listOf(
      listOf("Jumlah", "", "", data.totalScoreText.ifBlank { "-" }),
      listOf("Rata-Rata", "", "", data.averageScoreText.ifBlank { "-" })
    )
    drawTable(
      x = subjectTableX,
      columns = subjectColumns,
      headers = listOf("No", "Mata Pelajaran", "KKM", "Nilai"),
      rows = subjectRows,
      centerColumns = setOf(0, 2, 3)
    )
    y += 7f * mm

    canvas?.drawText("B. UJIAN MID SEMESTER KETAHFIZAN", margin, y, bodyBoldPaint)
    y += 4.5f * mm
    drawTable(
      x = midTableX,
      columns = midColumns,
      headers = listOf("CAPAIAN HAFALAN", "NILAI"),
      rows = listOf(listOf(data.midTahfizCapaian.ifBlank { "-" }, data.midTahfizScore.ifBlank { "-" })),
      centerColumns = setOf(0, 1)
    )
    y += 7f * mm

    canvas?.drawText("C. KEHADIRAN", margin, y, bodyBoldPaint)
    y += 4.5f * mm
    drawTable(
      x = attendanceTableX,
      columns = attendanceColumns,
      headers = listOf("No", "Kehadiran", "Izin", "Sakit"),
      rows = listOf(
        listOf("1", "Kelas", data.kelasIzinText.ifBlank { "0" }, data.kelasSakitText.ifBlank { "0" }),
        listOf("2", "Halaqah Tahfizh", data.halaqahIzinText.ifBlank { "0" }, data.halaqahSakitText.ifBlank { "0" })
      ),
      centerColumns = setOf(0, 2, 3)
    )
    y += 11f * mm

    ensureSpace(26f * mm)
    val signCenterX = (127.8f * mm) + ((56.6f * mm) / 2f)
    canvas?.drawText("Mengetahui,", signCenterX, y, Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER })
    y += 5f * mm
    canvas?.drawText("Wali Kelas", signCenterX, y, Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER })
    y += 18f * mm
    canvas?.drawText(data.waliKelasName.ifBlank { "-" }, signCenterX, y, Paint(bodyBoldPaint).apply { textAlign = Paint.Align.CENTER })
    finishPage()
  }

  private fun copyTextToClipboard(context: Context, label: String, text: String) {
    runCatching {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }
  }

  private fun buildFileName(data: UtsReportPayload): String {
    val semester = sanitizeFileNamePart(data.semesterLabel).ifBlank { "Semester" }
    val name = sanitizeFileNamePart(data.studentName).ifBlank { "Santri" }
    return "Laporan PTS $semester - $name"
  }

  private fun buildStoragePath(data: UtsReportPayload): String {
    val semester = sanitizeFileNamePart(data.semesterLabel).replace(" ", "-").ifBlank { "semester" }
    val name = sanitizeFileNamePart(data.studentName).replace(" ", "-").ifBlank { data.studentId.ifBlank { "santri" } }
    return "android/$semester/$name-${System.currentTimeMillis()}.pdf"
  }

  private fun sanitizeFileNamePart(value: String): String {
    return value.trim().replace(Regex("""[\\/:*?"<>|]"""), "").replace(Regex("\\s+"), " ").take(90)
  }
}

private class UtsPdfFilePrintAdapter(
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
      callback?.onWriteFailed("Tujuan cetak tidak tersedia.")
      return
    }
    try {
      pdfFile.inputStream().use { input ->
        FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
      }
      callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    } catch (error: Exception) {
      callback?.onWriteFailed(error.message)
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
