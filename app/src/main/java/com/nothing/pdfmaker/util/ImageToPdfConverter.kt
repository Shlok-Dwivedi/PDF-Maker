package com.nothing.pdfmaker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DocumentFilter {
    ORIGINAL,
    BLACK_AND_WHITE,
    GRAYSCALE,
    COLOR_ENHANCE
}

data class PageItem(
    val uri: Uri,
    var rotationDegrees: Float = 0f,
    var filter: DocumentFilter = DocumentFilter.ORIGINAL
)

object ImageToPdfConverter {

    // Standard A4 dimensions at 72 DPI points: 595 x 842
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842

    suspend fun convertImagesToPdf(
        context: Context,
        pages: List<PageItem>,
        fitToA4: Boolean = true,
        title: String? = null
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()

        pages.forEachIndexed { index, pageItem ->
            val rawBitmap = loadBitmapFromUri(context, pageItem.uri)
            if (rawBitmap != null) {
                // Apply rotation
                val rotatedBitmap = if (pageItem.rotationDegrees != 0f) {
                    val matrix = Matrix().apply { postRotate(pageItem.rotationDegrees) }
                    Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                } else {
                    rawBitmap
                }

                // Apply filter
                val filteredBitmap = applyFilter(rotatedBitmap, pageItem.filter)

                val pageWidth = if (fitToA4) A4_WIDTH else filteredBitmap.width
                val pageHeight = if (fitToA4) A4_HEIGHT else filteredBitmap.height

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Draw background white
                canvas.drawColor(Color.WHITE)

                if (fitToA4) {
                    // Fit inside A4 with margins
                    val margin = 20f
                    val targetWidth = pageWidth - (margin * 2)
                    val targetHeight = pageHeight - (margin * 2)

                    val scale = minOf(targetWidth / filteredBitmap.width, targetHeight / filteredBitmap.height)
                    val drawWidth = filteredBitmap.width * scale
                    val drawHeight = filteredBitmap.height * scale

                    val left = margin + (targetWidth - drawWidth) / 2f
                    val top = margin + (targetHeight - drawHeight) / 2f

                    val destRect = RectF(left, top, left + drawWidth, top + drawHeight)
                    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                    canvas.drawBitmap(filteredBitmap, null, destRect, paint)
                } else {
                    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                    canvas.drawBitmap(filteredBitmap, 0f, 0f, paint)
                }

                pdfDocument.finishPage(page)

                if (rotatedBitmap != rawBitmap) rotatedBitmap.recycle()
                if (filteredBitmap != rotatedBitmap && filteredBitmap != rawBitmap) filteredBitmap.recycle()
                rawBitmap.recycle()
            }
        }

        val pdfsDir = PdfStorageManager.getPdfsDirectory(context)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = if (!title.isNullOrBlank()) {
            "${title.trim().replace("[^a-zA-Z0-9._-]".toRegex(), "_")}.pdf"
        } else {
            "Document_$timeStamp.pdf"
        }

        val destFile = File(pdfsDir, fileName)
        FileOutputStream(destFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        destFile
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            // First decode bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            // Downsample if extremely huge (> 3000px) to conserve memory
            var sampleSize = 1
            while (options.outWidth / sampleSize > 3000 || options.outHeight / sampleSize > 3000) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun applyFilter(src: Bitmap, filter: DocumentFilter): Bitmap {
        return when (filter) {
            DocumentFilter.ORIGINAL -> src
            DocumentFilter.GRAYSCALE -> {
                val dest = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(dest)
                val paint = Paint()
                val cm = ColorMatrix().apply { setSaturation(0f) }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(src, 0f, 0f, paint)
                dest
            }
            DocumentFilter.BLACK_AND_WHITE -> {
                // High contrast document filter: convert to grayscale then boost contrast to clean paper background
                val dest = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(dest)
                val paint = Paint()
                val cm = ColorMatrix().apply {
                    setSaturation(0f)
                    // High contrast curve
                    val contrast = 1.8f
                    val scale = contrast
                    val translate = (-0.5f * contrast + 0.5f) * 255f
                    val array = floatArrayOf(
                        scale, 0f, 0f, 0f, translate,
                        0f, scale, 0f, 0f, translate,
                        0f, 0f, scale, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                    postConcat(ColorMatrix(array))
                }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(src, 0f, 0f, paint)
                dest
            }
            DocumentFilter.COLOR_ENHANCE -> {
                // Lift shadows and boost text readability
                val dest = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(dest)
                val paint = Paint()
                val cm = ColorMatrix().apply {
                    setSaturation(1.3f)
                    val contrast = 1.25f
                    val scale = contrast
                    val translate = (-0.5f * contrast + 0.5f) * 255f + 15f
                    val array = floatArrayOf(
                        scale, 0f, 0f, 0f, translate,
                        0f, scale, 0f, 0f, translate,
                        0f, 0f, scale, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                    postConcat(ColorMatrix(array))
                }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(src, 0f, 0f, paint)
                dest
            }
        }
    }
}
