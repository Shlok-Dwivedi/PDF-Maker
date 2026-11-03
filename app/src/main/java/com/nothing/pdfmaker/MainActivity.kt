package com.nothing.pdfmaker

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.nothing.pdfmaker.model.ScannedDocument
import com.nothing.pdfmaker.ui.components.DocumentItem
import com.nothing.pdfmaker.ui.components.EightPointCropScreen
import com.nothing.pdfmaker.ui.components.ImageStudioModal
import com.nothing.pdfmaker.ui.components.NothingHeader
import com.nothing.pdfmaker.ui.components.ScanColorMode
import com.nothing.pdfmaker.ui.components.ScannerCards
import com.nothing.pdfmaker.ui.theme.NothingBlack
import com.nothing.pdfmaker.ui.theme.NothingGray
import com.nothing.pdfmaker.ui.theme.NothingLensTheme
import com.nothing.pdfmaker.ui.theme.NothingRed
import com.nothing.pdfmaker.ui.theme.NothingWhite
import com.nothing.pdfmaker.util.BitmapUtil
import com.nothing.pdfmaker.util.ImageToPdfConverter
import com.nothing.pdfmaker.util.PdfStorageManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NothingLensTheme {
                PDFMakerApp()
            }
        }
    }
}

@Composable
fun PDFMakerApp() {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val savedDocuments = remember { mutableStateListOf<ScannedDocument>() }
    var selectedGalleryUris by remember { mutableStateOf<List<Uri>?>(null) }
    var isGeneratingFromGallery by remember { mutableStateOf(false) }

    // 8-Point Crop State
    var activeEightPointBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingCameraCaptureUri by remember { mutableStateOf<Uri?>(null) }

    var currentScanMode by remember { mutableStateOf(ScanColorMode.NATURAL_FAST) }

    fun refreshDocuments() {
        scope.launch {
            val docs = PdfStorageManager.loadSavedDocuments(context)
            savedDocuments.clear()
            savedDocuments.addAll(docs)
        }
    }

    LaunchedEffect(Unit) {
        refreshDocuments()
    }

    // High-Resolution Camera Capture for 8-Point Crop
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraCaptureUri != null) {
            scope.launch {
                val b = BitmapUtil.loadBitmap(context, pendingCameraCaptureUri!!)
                if (b != null) {
                    activeEightPointBitmap = b
                }
            }
        }
    }

    fun launchEightPointCamera() {
        try {
            val dir = File(context.filesDir, "camera_captures")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            pendingCameraCaptureUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Camera error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Google ML Kit Document Scanner setup
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
            val pdf = result?.pdf
            if (pdf != null) {
                val firstPageUri = result.pages?.firstOrNull()?.imageUri
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val docName = "Scan_$timeStamp"
                val tempDoc = ScannedDocument(
                    id = "$docName.pdf",
                    name = docName,
                    fileUri = pdf.uri,
                    filePath = "",
                    pageCount = pdf.pageCount,
                    sizeBytes = 0,
                    lastModified = System.currentTimeMillis(),
                    thumbnailUri = firstPageUri
                )
                savedDocuments.add(0, tempDoc)

                scope.launch {
                    val savedDoc = PdfStorageManager.saveScannedPdf(
                        context = context,
                        sourceUri = pdf.uri,
                        pageCount = pdf.pageCount,
                        firstPageImageUri = firstPageUri
                    )
                    val index = savedDocuments.indexOfFirst { it.id == tempDoc.id }
                    if (index != -1) {
                        savedDocuments[index] = savedDoc
                    } else {
                        refreshDocuments()
                    }

                    val snackbarResult = snackbarHostState.showSnackbar(
                        message = "PDF saved: ${savedDoc.name} (${savedDoc.pageCount} pages)",
                        actionLabel = "OPEN",
                        duration = SnackbarDuration.Short
                    )
                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                        PdfStorageManager.openPdf(context, savedDoc)
                    }
                }
            } else {
                Toast.makeText(context, "Scan completed", Toast.LENGTH_SHORT).show()
                refreshDocuments()
            }
        }
    }

    fun startAutoScanning() {
        if (activity == null) return

        val scannerMode = if (currentScanMode == ScanColorMode.NATURAL_FAST) {
            GmsDocumentScannerOptions.SCANNER_MODE_BASE
        } else {
            GmsDocumentScannerOptions.SCANNER_MODE_FULL
        }

        val scannerOptions = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(100)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(scannerMode)
            .build()

        val scannerClient = GmsDocumentScanning.getClient(scannerOptions)

        scannerClient.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    context,
                    "Failed to open scanner: ${error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // Direct Gallery Photo Picker
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedGalleryUris = uris
        }
    }

    // If full-screen 8-Point Crop is active, show it immediately
    if (activeEightPointBitmap != null) {
        EightPointCropScreen(
            bitmap = activeEightPointBitmap!!,
            onCropCompleted = { croppedBitmap ->
                scope.launch {
                    val croppedUri = BitmapUtil.saveCroppedBitmap(context, croppedBitmap)
                    activeEightPointBitmap = null
                    if (croppedUri != null) {
                        selectedGalleryUris = listOf(croppedUri)
                    }
                }
            },
            onCancel = { activeEightPointBitmap = null }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NothingBlack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { launchEightPointCamera() },
                containerColor = NothingRed,
                contentColor = NothingWhite,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Crop,
                    contentDescription = "8-Point Precision Crop"
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = NothingBlack
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    NothingHeader()
                }

                item {
                    ScannerCards(
                        currentMode = currentScanMode,
                        onModeChange = { currentScanMode = it },
                        onStartEightPointScan = { launchEightPointCamera() },
                        onStartAutoScan = { startAutoScanning() },
                        onImportImages = {
                            galleryPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(28.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT SCANS",
                            color = NothingGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "${savedDocuments.size} FILES",
                            color = NothingGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (savedDocuments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp, start = 20.dp, end = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = NothingGray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    text = "NO SCANNED DOCUMENTS YET",
                                    color = NothingGray,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Tap '8-POINT PRECISION CROP' to take a photo with 8-point handles",
                                    color = NothingGray.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = savedDocuments,
                        key = { it.id }
                    ) { doc ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            DocumentItem(
                                document = doc,
                                onClick = { PdfStorageManager.openPdf(context, doc) },
                                onShare = { PdfStorageManager.sharePdf(context, doc) },
                                onDelete = {
                                    PdfStorageManager.deleteDocument(context, doc)
                                    refreshDocuments()
                                    Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        }

        // Gallery to PDF Studio Modal
        selectedGalleryUris?.let { uris ->
            ImageStudioModal(
                selectedUris = uris,
                onDismiss = { selectedGalleryUris = null },
                isGenerating = isGeneratingFromGallery,
                onGeneratePdf = { pages, fitToA4, docTitle ->
                    isGeneratingFromGallery = true
                    scope.launch {
                        try {
                            val generatedFile = ImageToPdfConverter.convertImagesToPdf(
                                context = context,
                                pages = pages,
                                fitToA4 = fitToA4,
                                title = docTitle
                            )
                            isGeneratingFromGallery = false
                            selectedGalleryUris = null
                            refreshDocuments()

                            val savedDoc = PdfStorageManager.loadSavedDocuments(context)
                                .firstOrNull { it.name == generatedFile.nameWithoutExtension }

                            val snackbarResult = snackbarHostState.showSnackbar(
                                message = "PDF generated: ${generatedFile.name}",
                                actionLabel = "OPEN",
                                duration = SnackbarDuration.Short
                            )
                            if (snackbarResult == SnackbarResult.ActionPerformed && savedDoc != null) {
                                PdfStorageManager.openPdf(context, savedDoc)
                            }
                        } catch (e: Exception) {
                            isGeneratingFromGallery = false
                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
    }
}
