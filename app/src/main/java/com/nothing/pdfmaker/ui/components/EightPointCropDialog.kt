package com.nothing.pdfmaker.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nothing.pdfmaker.ui.theme.NothingBlack
import com.nothing.pdfmaker.ui.theme.NothingBorder
import com.nothing.pdfmaker.ui.theme.NothingGray
import com.nothing.pdfmaker.ui.theme.NothingRed
import com.nothing.pdfmaker.ui.theme.NothingSurface
import com.nothing.pdfmaker.ui.theme.NothingWhite
import com.nothing.pdfmaker.util.CropMesh8
import com.nothing.pdfmaker.util.Dewarp8PointHelper
import com.nothing.pdfmaker.util.PointF8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable
fun EightPointCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropped: (croppedUri: Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var mesh by remember { mutableStateOf<CropMesh8?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            val bmp = loadBitmap(context, imageUri)
            sourceBitmap = bmp
            if (bmp != null) {
                mesh = CropMesh8.createDefault(bmp.width.toFloat(), bmp.height.toFloat(), 0.06f)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NothingBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(NothingRed, CircleShape)
                            )
                            Text(
                                text = "8-POINT CURVED CROP",
                                color = NothingWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Drag 4 corners & 4 edge handles to fit curved pages",
                            color = NothingGray,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NothingWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive 8-point Viewport
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NothingSurface),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = sourceBitmap
                    val currentMesh = mesh

                    if (bmp != null && currentMesh != null) {
                        Interactive8PointCanvas(
                            bitmap = bmp,
                            mesh = currentMesh,
                            onMeshChanged = { mesh = it }
                        )
                    } else {
                        CircularProgressIndicator(color = NothingRed)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val bmp = sourceBitmap ?: return@OutlinedButton
                            mesh = CropMesh8.createDefault(bmp.width.toFloat(), bmp.height.toFloat(), 0.05f)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NothingWhite)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RESET",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val bmp = sourceBitmap ?: return@Button
                            val m = mesh ?: return@Button
                            isProcessing = true
                            scope.launch {
                                val croppedUri = withContext(Dispatchers.IO) {
                                    val dewarped = Dewarp8PointHelper.dewarp(bmp, m)
                                    saveCroppedBitmap(context, dewarped)
                                }
                                isProcessing = false
                                if (croppedUri != null) {
                                    onCropped(croppedUri)
                                }
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NothingWhite,
                            contentColor = NothingBlack
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NothingBlack,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DE-WARPING...",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "APPLY 8-POINT CROP",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Interactive8PointCanvas(
    bitmap: Bitmap,
    mesh: CropMesh8,
    onMeshChanged: (CropMesh8) -> Unit
) {
    var activeHandleIndex by remember { mutableStateOf<Int?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()

        // Compute aspect ratio scaling
        val scale = minOf(viewWidth / bitmap.width, viewHeight / bitmap.height)
        val drawnWidth = bitmap.width * scale
        val drawnHeight = bitmap.height * scale

        val offsetX = (viewWidth - drawnWidth) / 2f
        val offsetY = (viewHeight - drawnHeight) / 2f

        // Convert point between Bitmap coordinates and Screen View coordinates
        fun toScreen(pt: PointF8): Offset = Offset(offsetX + pt.x * scale, offsetY + pt.y * scale)
        fun toBitmap(offset: Offset): PointF8 = PointF8(
            ((offset.x - offsetX) / scale).coerceIn(0f, bitmap.width.toFloat()),
            ((offset.y - offsetY) / scale).coerceIn(0f, bitmap.height.toFloat())
        )

        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bitmap, mesh) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            // Hit test the 8 handles (radius 34dp equivalent in pixels)
                            val hitRadius = 34.dp.toPx()
                            val pts = mesh.getPoints()
                            var closestIdx: Int? = null
                            var closestDist = Float.MAX_VALUE

                            pts.forEachIndexed { index, pt ->
                                val scr = toScreen(pt)
                                val dist = (startOffset - scr).getDistance()
                                if (dist <= hitRadius && dist < closestDist) {
                                    closestDist = dist
                                    closestIdx = index
                                }
                            }
                            activeHandleIndex = closestIdx
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val idx = activeHandleIndex ?: return@detectDragGestures
                            val pts = mesh.getPoints()
                            val currentScr = toScreen(pts[idx])
                            val newScr = currentScr + dragAmount
                            val newBmpPt = toBitmap(newScr)

                            // Update corresponding point in mesh
                            when (idx) {
                                0 -> mesh.tl = newBmpPt
                                1 -> mesh.tc = newBmpPt
                                2 -> mesh.tr = newBmpPt
                                3 -> mesh.rc = newBmpPt
                                4 -> mesh.br = newBmpPt
                                5 -> mesh.bc = newBmpPt
                                6 -> mesh.bl = newBmpPt
                                7 -> mesh.lc = newBmpPt
                            }
                            onMeshChanged(mesh)
                        },
                        onDragEnd = { activeHandleIndex = null },
                        onDragCancel = { activeHandleIndex = null }
                    )
                }
        ) {
            // 1. Draw source photo
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                dstSize = IntSize(drawnWidth.toInt(), drawnHeight.toInt())
            )

            // Screen coordinates of the 8 points
            val sTL = toScreen(mesh.tl)
            val sTC = toScreen(mesh.tc)
            val sTR = toScreen(mesh.tr)
            val sRC = toScreen(mesh.rc)
            val sBR = toScreen(mesh.br)
            val sBC = toScreen(mesh.bc)
            val sBL = toScreen(mesh.bl)
            val sLC = toScreen(mesh.lc)

            // Approximate center
            val sCenter = Offset(
                (sTL.x + sTR.x + sBR.x + sBL.x) / 4f,
                (sTL.y + sTR.y + sBR.y + sBL.y) / 4f
            )

            // 2. Draw 8-point boundary polygon
            val boundaryPath = Path().apply {
                moveTo(sTL.x, sTL.y)
                lineTo(sTC.x, sTC.y)
                lineTo(sTR.x, sTR.y)
                lineTo(sRC.x, sRC.y)
                lineTo(sBR.x, sBR.y)
                lineTo(sBC.x, sBC.y)
                lineTo(sBL.x, sBL.y)
                lineTo(sLC.x, sLC.y)
                close()
            }

            // Fill boundary with soft tint
            drawPath(boundaryPath, color = Color(0x33D71921))

            // Outline boundary lines
            drawPath(boundaryPath, color = Color.White, style = Stroke(width = 2.5f.dp.toPx()))

            // 3. Draw crosshair guide lines (TC <-> BC and LC <-> RC)
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = sTC,
                end = sBC,
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = sLC,
                end = sRC,
                strokeWidth = 1.dp.toPx()
            )

            // 4. Draw the 8 Handles
            val handleRadius = 10.dp.toPx()
            val points = listOf(
                sTL to true,   // corner (true)
                sTC to false,  // midpoint (false)
                sTR to true,
                sRC to false,
                sBR to true,
                sBC to false,
                sBL to true,
                sLC to false
            )

            points.forEachIndexed { index, (pt, isCorner) ->
                val isActive = activeHandleIndex == index

                // Outer touch circle
                drawCircle(
                    color = if (isActive) NothingRed else if (isCorner) NothingRed else Color.White,
                    radius = if (isActive) handleRadius * 1.5f else handleRadius,
                    center = pt
                )
                // Inner center dot
                drawCircle(
                    color = if (isCorner) Color.White else NothingBlack,
                    radius = handleRadius * 0.45f,
                    center = pt
                )
            }
        }
    }
}

private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        var sampleSize = 1
        while (options.outWidth / sampleSize > 2000 || options.outHeight / sampleSize > 2000) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        }
    } catch (e: Exception) {
        null
    }
}

private fun saveCroppedBitmap(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val cacheDir = File(context.cacheDir, "cropped_images")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val file = File(cacheDir, "crop_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        Uri.fromFile(file)
    } catch (e: Exception) {
        null
    }
}
