package com.nothing.pdfmaker.model

import android.net.Uri

data class ScannedDocument(
    val id: String,
    val name: String,
    val fileUri: Uri,
    val filePath: String,
    val pageCount: Int,
    val sizeBytes: Long,
    val lastModified: Long,
    val thumbnailUri: Uri? = null
)
