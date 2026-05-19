package com.mim.guruapp.export

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class UserGuidePdfSection(
  val title: String,
  val summary: String,
  val blocks: List<UserGuidePdfBlock>
)

data class UserGuidePdfBlock(
  val label: String,
  val title: String,
  val body: String,
  val hasImageSlot: Boolean,
  val imageResName: String = "",
  val imageCaption: String = ""
)

object UserGuidePdfExporter {
  private const val PageWidth = 595
  private const val PageHeight = 842
  private const val Margin = 42f
  private const val ContentWidth = PageWidth - (Margin * 2)

  suspend fun createPdfFile(
    context: Context,
    sections: List<UserGuidePdfSection>
  ): File = withContext(Dispatchers.IO) {
    val outputDir = File(context.cacheDir, "panduan-pengguna").apply { mkdirs() }
    val file = File(outputDir, "Panduan_Pengguna_MIM_APP.pdf")
    PdfDocument().use { document ->
      val writer = PdfWriter(context, document)
      writer.startPage()
      writer.drawTitlePage()
      sections.forEach { writer.drawSection(it) }
      writer.finish()
      FileOutputStream(file).use { output -> document.writeTo(output) }
    }
    file
  }

  fun openPdf(context: Context, pdfFile: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/pdf")
      clipData = ClipData.newUri(context.contentResolver, pdfFile.name, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      context.startActivity(Intent.createChooser(intent, "Buka panduan pengguna").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, pdfFile.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(Intent.createChooser(shareIntent, "Kirim panduan pengguna").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
  }

  private class PdfWriter(
    private val context: Context,
    private val document: PdfDocument
  ) {
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(24, 33, 53)
      textSize = 22f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(24, 33, 53)
      textSize = 16f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(37, 109, 123)
      textSize = 9f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(45, 55, 72)
      textSize = 11f
    }
    private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(100, 116, 139)
      textSize = 10f
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(218, 226, 236)
      style = Paint.Style.STROKE
      strokeWidth = 1.2f
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(244, 248, 251)
      style = Paint.Style.FILL
    }
    private val accentFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(232, 244, 247)
      style = Paint.Style.FILL
    }

    private var pageNumber = 0
    private var page: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var y = Margin

    fun startPage() {
      pageNumber += 1
      page = document.startPage(PdfDocument.PageInfo.Builder(PageWidth, PageHeight, pageNumber).create())
      canvas = page?.canvas
      y = Margin
      drawFooter()
    }

    fun finish() {
      page?.let(document::finishPage)
      page = null
      canvas = null
    }

    fun drawTitlePage() {
      ensureSpace(115f)
      drawWrappedText("Panduan Pengguna MIM APP", titlePaint, ContentWidth)
      y += 8f
      drawWrappedText(
        "Dokumen ini berisi panduan penggunaan aplikasi guru, mulai dari login, navigasi, input data, laporan, mutabaah, sampai pengaturan aplikasi.",
        bodyPaint,
        ContentWidth
      )
      y += 14f
      canvas?.drawLine(Margin, y, PageWidth - Margin, y, linePaint)
      y += 18f
    }

    fun drawSection(section: UserGuidePdfSection) {
      ensureSpace(95f)
      drawWrappedText(section.title, sectionPaint, ContentWidth)
      y += 4f
      drawWrappedText(section.summary, bodyPaint, ContentWidth)
      y += 10f
      section.blocks.forEachIndexed { index, block ->
        drawBlock(index + 1, block)
      }
      y += 12f
    }

    private fun drawBlock(number: Int, block: UserGuidePdfBlock) {
      val textHeight = measureWrappedTextHeight(block.body, bodyPaint, ContentWidth - 22f)
      val imageHeight = if (block.hasImageSlot) 112f else 0f
      val blockHeight = 52f + imageHeight + textHeight
      ensureSpace(blockHeight + 16f)

      val top = y
      val bottom = y + blockHeight
      val rect = RectF(Margin, top, PageWidth - Margin, bottom)
      canvas?.drawRoundRect(rect, 16f, 16f, fillPaint)
      canvas?.drawRoundRect(rect, 16f, 16f, linePaint)

      var cursor = top + 20f
      canvas?.drawText("$number", Margin + 14f, cursor, labelPaint)
      canvas?.drawText(block.label, Margin + 36f, cursor, labelPaint)
      cursor += 17f
      drawWrappedText(block.title, sectionPaint, ContentWidth - 28f, x = Margin + 14f, startY = cursor)
      cursor = y + 8f

      if (block.hasImageSlot) {
        val imageTop = cursor
        val imageRect = RectF(Margin + 14f, imageTop, PageWidth - Margin - 14f, imageTop + 96f)
        canvas?.drawRoundRect(imageRect, 14f, 14f, accentFillPaint)
        canvas?.drawRoundRect(imageRect, 14f, 14f, linePaint)
        val imageResId = block.imageResName
          .takeIf { it.isNotBlank() }
          ?.let { context.resources.getIdentifier(it, "drawable", context.packageName) }
          ?: 0
        if (imageResId != 0) {
          BitmapFactory.decodeResource(context.resources, imageResId)?.let { bitmap ->
            canvas?.drawBitmap(bitmap, null, imageRect, Paint(Paint.ANTI_ALIAS_FLAG))
          }
        } else {
          drawCenteredPlaceholderText(
            block.imageCaption.ifBlank { "Screenshot untuk langkah ini akan ditambahkan di sini." },
            imageRect
          )
        }
        cursor += 112f
      }

      drawWrappedText(block.body, bodyPaint, ContentWidth - 28f, x = Margin + 14f, startY = cursor)
      y = bottom + 12f
    }

    private fun drawCenteredPlaceholderText(text: String, rect: RectF) {
      val lines = wrapText(text, mutedPaint, rect.width() - 24f)
      val lineHeight = mutedPaint.textSize + 4f
      var textY = rect.centerY() - ((lines.size - 1) * lineHeight / 2f)
      lines.forEach { line ->
        val textX = rect.centerX() - mutedPaint.measureText(line) / 2f
        canvas?.drawText(line, textX, textY, mutedPaint)
        textY += lineHeight
      }
    }

    private fun ensureSpace(required: Float) {
      if (y + required <= PageHeight - Margin) return
      finish()
      startPage()
    }

    private fun drawFooter() {
      canvas?.drawText("MIM APP", Margin, PageHeight - 22f, mutedPaint)
      canvas?.drawText("Hal. $pageNumber", PageWidth - Margin - 42f, PageHeight - 22f, mutedPaint)
    }

    private fun drawWrappedText(
      text: String,
      paint: Paint,
      maxWidth: Float,
      x: Float = Margin,
      startY: Float = y
    ) {
      val lines = wrapText(text, paint, maxWidth)
      var currentY = startY
      lines.forEach { line ->
        canvas?.drawText(line, x, currentY, paint)
        currentY += paint.textSize + 5f
      }
      y = currentY
    }

    private fun measureWrappedTextHeight(text: String, paint: Paint, maxWidth: Float): Float =
      wrapText(text, paint, maxWidth).size * (paint.textSize + 5f)

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
      val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
      if (words.isEmpty()) return listOf("")
      val lines = mutableListOf<String>()
      var line = ""
      words.forEach { word ->
        val candidate = if (line.isBlank()) word else "$line $word"
        if (paint.measureText(candidate) <= maxWidth) {
          line = candidate
        } else {
          if (line.isNotBlank()) lines += line
          line = word
        }
      }
      if (line.isNotBlank()) lines += line
      return lines
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
