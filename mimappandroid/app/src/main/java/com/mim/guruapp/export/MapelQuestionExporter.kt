package com.mim.guruapp.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.mim.guruapp.data.model.SubjectOverview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class MapelQuestionExportData(
  val id: String,
  val title: String,
  val category: String,
  val form: String,
  val dateLabel: String,
  val instruction: String,
  val questionsText: String
)

object MapelQuestionExporter {
  private const val PageWidth = 595
  private const val PageHeight = 842
  private const val Margin = 48f
  private val A4MediaSize = PrintAttributes.MediaSize.ISO_A4

  suspend fun createPdfFile(
    context: Context,
    subject: SubjectOverview,
    data: MapelQuestionExportData
  ): File = withContext(Dispatchers.IO) {
    val outputDir = File(context.cacheDir, "mapel-soal").apply { mkdirs() }
    val file = File(outputDir, "${buildFileName(subject, data)}.pdf")
    PdfDocument().use { document ->
      drawQuestionSheet(document, subject, data)
      FileOutputStream(file).use { output -> document.writeTo(output) }
    }
    file
  }

  fun printPdf(
    context: Context,
    pdfFile: File,
    data: MapelQuestionExportData
  ) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = "Soal - ${data.title.ifBlank { "Mapel" }}"
    printManager.print(
      jobName,
      QuestionPdfPrintAdapter(pdfFile, jobName),
      PrintAttributes.Builder()
        .setMediaSize(A4MediaSize.asPortrait())
        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
        .build()
    )
  }

  private fun drawQuestionSheet(
    document: PdfDocument,
    subject: SubjectOverview,
    data: MapelQuestionExportData
  ) {
    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var y = Margin

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      textSize = 15f
      textAlign = Paint.Align.CENTER
      typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      textSize = 10.5f
      typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      textSize = 10.5f
      typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      strokeWidth = 0.8f
      style = Paint.Style.STROKE
    }

    fun startPage() {
      pageNumber += 1
      page = document.startPage(PdfDocument.PageInfo.Builder(PageWidth, PageHeight, pageNumber).create())
      y = Margin
    }

    fun finishPage() {
      page?.let(document::finishPage)
      page = null
    }

    fun ensureSpace(required: Float) {
      if (page == null) startPage()
      if (y + required > PageHeight - Margin) {
        finishPage()
        startPage()
      }
    }

    fun lineHeight(paint: Paint): Float {
      val metrics = paint.fontMetrics
      return (metrics.descent - metrics.ascent) * 1.22f
    }

    fun wrapText(text: String, paint: Paint, width: Float): List<String> {
      val clean = text.ifBlank { "-" }.replace("\t", " ")
      val paragraphs = clean.split('\n')
      return buildList {
        paragraphs.forEach { paragraph ->
          val words = paragraph.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
          if (words.isEmpty()) {
            add("")
          } else {
            var line = ""
            words.forEach { word ->
              val candidate = if (line.isBlank()) word else "$line $word"
              if (paint.measureText(candidate) <= width || line.isBlank()) {
                line = candidate
              } else {
                add(line)
                line = word
              }
            }
            if (line.isNotBlank()) add(line)
          }
        }
      }
    }

    fun drawWrappedText(text: String, paint: Paint, x: Float, width: Float) {
      val lines = wrapText(text, paint, width)
      lines.forEach { line ->
        ensureSpace(lineHeight(paint))
        page?.canvas?.drawText(line, x, y, paint)
        y += lineHeight(paint)
      }
    }

    fun drawMetaRow(label: String, value: String) {
      ensureSpace(17f)
      page?.canvas?.drawText(label, Margin, y, labelPaint)
      page?.canvas?.drawText(":", Margin + 86f, y, bodyPaint)
      page?.canvas?.drawText(value.ifBlank { "-" }, Margin + 98f, y, bodyPaint)
      y += 16f
    }

    startPage()
    page?.canvas?.drawText(data.title.ifBlank { "Soal ${subject.title}" }, PageWidth / 2f, y, titlePaint)
    y += 24f
    page?.canvas?.drawLine(Margin, y, PageWidth - Margin, y, linePaint)
    y += 20f

    drawMetaRow("Mapel", subject.title)
    drawMetaRow("Kelas", subject.className.ifBlank { "-" })
    drawMetaRow("Kategori", data.category.ifBlank { "Soal" })
    drawMetaRow("Bentuk", data.form.ifBlank { "-" })
    drawMetaRow("Tanggal", data.dateLabel.ifBlank { "-" })
    y += 8f

    if (data.instruction.isNotBlank()) {
      ensureSpace(18f)
      page?.canvas?.drawText("Instruksi", Margin, y, labelPaint)
      y += 16f
      drawWrappedText(data.instruction, bodyPaint, Margin, PageWidth - (Margin * 2f))
      y += 8f
    }

    ensureSpace(18f)
    page?.canvas?.drawText("Daftar Soal", Margin, y, labelPaint)
    y += 16f
    drawWrappedText(data.questionsText.ifBlank { "Belum ada isi soal." }, bodyPaint, Margin, PageWidth - (Margin * 2f))
    finishPage()
  }

  private fun buildFileName(subject: SubjectOverview, data: MapelQuestionExportData): String {
    val title = sanitizeFileNamePart(data.title).ifBlank { "Soal" }
    val mapel = sanitizeFileNamePart(subject.title).ifBlank { "Mapel" }
    return "$title - $mapel"
  }

  private fun sanitizeFileNamePart(value: String): String {
    return value.trim()
      .replace(Regex("""[\\/:*?"<>|]"""), "")
      .replace(Regex("\\s+"), " ")
      .take(90)
  }
}

private class QuestionPdfPrintAdapter(
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
    runCatching {
      FileOutputStream(destination.fileDescriptor).use { output ->
        pdfFile.inputStream().use { input -> input.copyTo(output) }
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
