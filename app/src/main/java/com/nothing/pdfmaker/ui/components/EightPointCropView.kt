package com.nothing.pdfmaker.ui.components

import android.graphics.Bitmap
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.pdfmaker.ui.theme.NothingBlack
import com.nothing.pdfmaker.ui.theme.NothingBorder
import com.nothing.pdfmaker.ui.theme.NothingGray
import com.nothing.pdfmaker.ui.theme.NothingRed
import com.nothing.pdfmaker.ui.theme.NothingSurface
import com.nothing.pdfmaker.ui.theme.NothingWhite
import com.nothing.pdfmaker.util.EightPointCropUtil
import com.nothing.pdfmaker.util.EightPointPolygon
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun EightPointCropScreen(
    bitmap: Bitmap,
    onCropCompleted: (Bitmap) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var polygon by remember { mutableStateOf(EightPointPolygon()) }
    var activePointIndex by remember { mutableIntStateOf(-1) }
    var rotationDegrees by remember { mutableStateOf(0f) }

    // Prepare rotated bitmap if needed
    val activeBitmap = remember(bitmap, rotationDegrees) {
        if (rotationDegrees != 0f) {
            val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = NothingWhite
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(NothingRed, CircleShape)
                    )
                    Text(
                        text = "8-POINT CROP & WARP",
                        color = NothingWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = {
                        rotationDegrees = (rotationDegrees + 90f) % 360f
                        polygon.reset()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = "Rotate",
                        tint = NothingWhite
                    )
                }
            }

            // Interactive 8-Point Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = constraints.maxWidth.toFloat()
                    val canvasHeight = constraints.maxHeight.toFloat()

                    // Compute fitted image display rect
                    val imgAspect = activeBitmap.width.toFloat() / activeBitmap.height.toFloat()
                    val canvasAspect = canvasWidth / canvasHeight

                    val (drawW, drawH) = if (imgAspect > canvasAspect) {
                        canvasWidth to (canvasWidth / imgAspect)
                    } else {
                        (canvasHeight * imgAspect) to canvasHeight
                    }

                    val offsetX = (canvasWidth - drawW) / 2f
                    val offsetY = (canvasHeight - drawH) / 2f

                    val imageRect = Rect(offsetX, offsetY, offsetX + drawW, offsetY + drawH)
                    val composeImage = remember(activeBitmap) { activeBitmap.asImageBitmap() }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(activeBitmap, drawW, drawH) {
                                detectDragGestures(
                                    onDragStart = { downOffset ->
                                        // Find closest handle among the 8 points
                                        val points = polygon.toList()
                                        var closestIdx = -1
                                        var minDistance = Float.MAX_VALUE
                                        val touchThreshold = 60.dp.toPx()

                                        points.forEachIndexed { idx, pt ->
                                            val screenX = offsetX + pt.x * drawW
                                            val screenY = offsetY + pt.y * drawH
                                            val dist = hypot(downOffset.x - screenX, downOffset.y - screenY)
                                            if (dist < minDistance && dist <= touchThreshold) {
                                                minDistance = dist
                                                closestIdx = idx
                                            }
                                        }
                                        activePointIndex = closestIdx
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (activePointIndex != -1) {
                                            change.consume()
                                            val currentPoint = when (activePointIndex) {
                                                0 -> polygon.topLeft
                                                1 -> polygon.topCenter
                                                2 -> polygon.topRight
                                                3 -> polygon.rightCenter
                                                4 -> polygon.bottomRight
                                                5 -> polygon.bottomCenter
                                                6 -> polygon.bottomLeft
                                                7 -> polygon.leftCenter
                                                else -> Offset.Zero
                                            }

                                            val newNormX = ((currentPoint.x * drawW + dragAmount.x) / drawW).coerceIn(0f, 1f)
                                            val newNormY = ((currentPoint.y * drawH + dragAmount.y) / drawH).coerceIn(0f, 1f)
                                            val updated = Offset(newNormX, newNormY)

                                            when (activePointIndex) {
                                                0 -> polygon = polygon.copy().apply { topLeft = updated }
                                                1 -> polygon = polygon.copy().apply { topCenter = updated }
                                                2 -> polygon = polygon.copy().apply { topRight = updated }
                                                3 -> polygon = polygon.copy().apply { rightCenter = updated }
                                                4 -> polygon = polygon.copy().apply { bottomRight = updated }
                                                5 -> polygon = polygon.copy().apply { bottomCenter = updated }
                                                6 -> polygon = polygon.copy().apply { bottomLeft = updated }
                                                7 -> polygon = polygon.copy().apply { leftCenter = updated }
                                            }
                                        }
                                    },
                                    onDragEnd = { activePointIndex = -1 },
                                    onDragCancel = { activePointIndex = -1 }
                                )
                            }
                    ) {
                        // 1. Draw base image
                        drawImage(
                            image = composeImage,
                            dstOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
                            dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt())
                        )

                        // 2. Build 8-point polygon Path
                        val pts = polygon.toList()
                        val screenPoints = pts.map { pt ->
                            Offset(offsetX + pt.x * drawW, offsetY + pt.y * drawH)
                        }

                        val polyPath = Path().apply {
                            moveTo(screenPoints[0].x, screenPoints[0].y)
                            for (i in 1 until 8) {
                                lineTo(screenPoints[i].x, screenPoints[i].y)
                            }
                            close()
                        }

                        // 3. Dark Scrim outside the 8 points
                        clipPath(polyPath, clipOp = ClipOp.Difference) {
                            drawRect(
                                color = Color.Black.copy(alpha = 0.65f),
                                topLeft = Offset(offsetX, offsetY),
                                size = Size(drawW, drawH)
                            )
                        }

                        // 4. Connecting lines
                        drawPath(
                            path = polyPath,
                            color = Color.White,
                            style = Stroke(width = 2.5f.dp.toPx())
                        )

                        // 5. Internal subtle dashed alignment crosshairs (TC <-> BC and LC <-> RC)
                        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.4f),
                            start = screenPoints[1], // TC
                            end = screenPoints[5],   // BC
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashedEffect
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.4f),
                            start = screenPoints[7], // LC
                            end = screenPoints[3],   // RC
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashedEffect
                        )

                        // 6. Draw 8 Handles
                        screenPoints.forEachIndexed { idx, point ->
                            val isCorner = idx % 2 == 0
                            val isActive = idx == activePointIndex

                            if (isCorner) {
                                // Corner Handles: Larger circle with red dot
                                val radius = if (isActive) 16.dp.toPx() else 12.dp.toPx()
                                drawCircle(
                                    color = Color.White,
                                    radius = radius,
                                    center = point
                                )
                                drawCircle(
                                    color = if (isActive) NothingRed else NothingBlack,
                                    radius = radius * 0.55f,
                                    center = point
                                )
                            } else {
                                // Edge Midpoint Handles: Pill / Rounded handle
                                val radius = if (isActive) 14.dp.toPx() else 10.dp.toPx()
                                drawCircle(
                                    color = if (isActive) NothingRed else Color.White,
                                    radius = radius,
                                    center = point
                                )
                                drawCircle(
                                    color = if (isActive) Color.White else NothingBlack,
                                    radius = radius * 0.5f,
                                    center = point
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NothingSurface)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                IconButton(
                    onClick = { polygon = EightPointPolygon() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Points",
                        tint = NothingGray
                    )
                }

                Text(
                    text = "DRAG CORNERS & MIDPOINTS",
                    color = NothingGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                // Apply Crop Button
                Button(
                    onClick = {
                        val cropped = EightPointCropUtil.cropWithEightPoints(activeBitmap, polygon)
                        onCropCompleted(cropped)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NothingWhite,
                        contentColor = NothingBlack
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Apply",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "APPLY",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
