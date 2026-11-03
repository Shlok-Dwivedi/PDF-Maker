package com.nothing.pdfmaker.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.nothing.pdfmaker.model.ScannedDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfStorageManager {

    private const val PDF_SUBDIR = "scanned_pdfs"
    private const val THUMB_SUBDIR = "thumbnails"

    fun getPdfsDirectory(context: Context): File {
        val dir = File(context.filesDir, PDF_SUBDIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getThumbnailsDirectory(context: Context): File {
        val dir = File(context.filesDir, THUMB_SUBDIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun saveScannedPdf(
        context: Context,
        sourceUri: Uri,
        pageCount: Int,
        customTitle: String? = null
    ): ScannedDocument = withContext(Dispatchers.IO) {
        val pdfsDir = getPdfsDirectory(context)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = if (!customTitle.isNullOrBlank()) {
            "${customTitle.trim().replace("[^a-zA-Z0-9._-]".toRegex(), "_")}.pdf"
        } else {
            "Scan_$timeStamp.pdf"
        }

        val destFile = File(pdfsDir, fileName)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        val thumbUri = generateThumbnailForPdf(context, destFile)

        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, destFile)

        ScannedDocument(
            id = destFile.name,
            name = destFile.nameWithoutExtension,
            fileUri = contentUri,
            filePath = destFile.absolutePath,
            pageCount = if (pageCount > 0) pageCount else getPageCount(destFile),
            sizeBytes = destFile.length(),
            lastModified = destFile.lastModified(),
            thumbnailUri = thumbUri
        )
    }

    suspend fun loadSavedDocuments(context: Context): List<ScannedDocument> = withContext(Dispatchers.IO) {
        val pdfsDir = getPdfsDirectory(context)
        val files = pdfsDir.listFiles { file -> file.isFile && file.extension.equals("pdf", ignoreCase = true) }
            ?: return@withContext emptyList()

        val authority = "${context.packageName}.fileprovider"
        files.sortedByDescending { it.lastModified() }.map { file ->
            val contentUri = try {
                FileProvider.getUriForFile(context, authority, file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }
            val thumbFile = File(getThumbnailsDirectory(context), "${file.nameWithoutExtension}.jpg")
            val thumbUri = if (thumbFile.exists()) {
                try {
                    FileProvider.getUriForFile(context, authority, thumbFile)
                } catch (e: Exception) {
                    Uri.fromFile(thumbFile)
                }
            } else {
                generateThumbnailForPdf(context, file)
            }

            ScannedDocument(
                id = file.name,
                name = file.nameWithoutExtension,
                fileUri = contentUri,
                filePath = file.absolutePath,
                pageCount = getPageCount(file),
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                thumbnailUri = thumbUri
            )
        }
    }

    private fun getPageCount(pdfFile: File): Int {
        return try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (e: Exception) {
            1
        }
    }

    private fun generateThumbnailForPdf(context: Context, pdfFile: File): Uri? {
        return try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount > 0) {
                        val page = renderer.openPage(0)
                        val width = 300
                        val height = (300f * page.height / page.width).toInt().coerceAtLeast(100)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()

                        val thumbsDir = getThumbnailsDirectory(context)
                        val thumbFile = File(thumbsDir, "${pdfFile.nameWithoutExtension}.jpg")
                        FileOutputStream(thumbFile).use { fos ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                        }
                        val authority = "${context.packageName}.fileprovider"
                        FileProvider.getUriForFile(context, authority, thumbFile)
                    } else null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun deleteDocument(context: Context, doc: ScannedDocument): Boolean {
        val file = File(doc.filePath)
        val thumbFile = File(getThumbnailsDirectory(context), "${file.nameWithoutExtension}.jpg")
        if (thumbFile.exists()) thumbFile.delete()
        return file.delete()
    }

    fun sharePdf(context: Context, doc: ScannedDocument) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, doc.fileUri)
            putExtra(Intent.EXTRA_SUBJECT, doc.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF with"))
    }

    fun openPdf(context: Context, doc: ScannedDocument) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(doc.fileUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            sharePdf(context, doc)
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format(Locale.getDefault(), "%d KB", bytes / 1024)
            else -> "$bytes B"
        }
    }

    fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
