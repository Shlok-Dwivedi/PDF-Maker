package com.nothing.pdfmaker.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot
import kotlin.math.roundToInt

data class EightPointPolygon(
    var topLeft: Offset = Offset(0.06f, 0.06f),
    var topCenter: Offset = Offset(0.50f, 0.06f),
    var topRight: Offset = Offset(0.94f, 0.06f),
    var rightCenter: Offset = Offset(0.94f, 0.50f),
    var bottomRight: Offset = Offset(0.94f, 0.94f),
    var bottomCenter: Offset = Offset(0.50f, 0.94f),
    var bottomLeft: Offset = Offset(0.06f, 0.94f),
    var leftCenter: Offset = Offset(0.06f, 0.50f)
) {
    fun toList(): List<Offset> = listOf(
        topLeft, topCenter, topRight,
        rightCenter, bottomRight, bottomCenter,
        bottomLeft, leftCenter
    )

    fun copy(): EightPointPolygon = EightPointPolygon(
        topLeft, topCenter, topRight,
        rightCenter, bottomRight, bottomCenter,
        bottomLeft, leftCenter
    )

    fun reset() {
        topLeft = Offset(0.06f, 0.06f)
        topCenter = Offset(0.50f, 0.06f)
        topRight = Offset(0.94f, 0.06f)
        rightCenter = Offset(0.94f, 0.50f)
        bottomRight = Offset(0.94f, 0.94f)
        bottomCenter = Offset(0.50f, 0.94f)
        bottomLeft = Offset(0.06f, 0.94f)
        leftCenter = Offset(0.06f, 0.50f)
    }
}

object EightPointCropUtil {

    /**
     * Warps and crops the given bitmap using the 8 boundary points.
     * Divides the 8-point polygon into 4 sub-quads meeting at the center,
     * perspective-warping each quadrant into a flat rectangular document.
     */
    fun cropWithEightPoints(
        srcBitmap: Bitmap,
        polygon: EightPointPolygon
    ): Bitmap {
        val bw = srcBitmap.width.toFloat()
        val bh = srcBitmap.height.toFloat()

        // Convert normalized points (0..1) to pixel coordinates
        val pTL = floatArrayOf(polygon.topLeft.x * bw, polygon.topLeft.y * bh)
        val pTC = floatArrayOf(polygon.topCenter.x * bw, polygon.topCenter.y * bh)
        val pTR = floatArrayOf(polygon.topRight.x * bw, polygon.topRight.y * bh)
        val pRC = floatArrayOf(polygon.rightCenter.x * bw, polygon.rightCenter.y * bh)
        val pBR = floatArrayOf(polygon.bottomRight.x * bw, polygon.bottomRight.y * bh)
        val pBC = floatArrayOf(polygon.bottomCenter.x * bw, polygon.bottomCenter.y * bh)
        val pBL = floatArrayOf(polygon.bottomLeft.x * bw, polygon.bottomLeft.y * bh)
        val pLC = floatArrayOf(polygon.leftCenter.x * bw, polygon.leftCenter.y * bh)

        // Center point: intersection of (topCenter -> bottomCenter) and (leftCenter -> rightCenter)
        val pCenter = calculateIntersection(pTC, pBC, pLC, pRC)

        // Compute destination document dimensions
        val topWidth = hypot(pTR[0] - pTL[0], pTR[1] - pTL[1])
        val bottomWidth = hypot(pBR[0] - pBL[0], pBR[1] - pBL[1])
        val leftHeight = hypot(pBL[0] - pTL[0], pBL[1] - pTL[1])
        val rightHeight = hypot(pBR[0] - pTR[0], pBR[1] - pTR[1])

        val dstWidth = maxOf(topWidth, bottomWidth).roundToInt().coerceIn(300, 4000)
        val dstHeight = maxOf(leftHeight, rightHeight).roundToInt().coerceIn(300, 4000)

        val halfW = dstWidth / 2f
        val halfH = dstHeight / 2f

        val dstBitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dstBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        // Sub-quad definitions:
        // Quad 0: Top-Left (pTL, pTC, pCenter, pLC) -> [ (0, 0), (halfW, 0), (halfW, halfH), (0, halfH) ]
        renderSubQuad(
            canvas, srcBitmap, paint,
            srcPoints = floatArrayOf(
                pTL[0], pTL[1],
                pTC[0], pTC[1],
                pCenter[0], pCenter[1],
                pLC[0], pLC[1]
            ),
            dstRect = Rect(0, 0, halfW.toInt(), halfH.toInt()),
            dstPoints = floatArrayOf(
                0f, 0f,
                halfW, 0f,
                halfW, halfH,
                0f, halfH
            )
        )

        // Quad 1: Top-Right (pTC, pTR, pRC, pCenter) -> [ (halfW, 0), (dstWidth, 0), (dstWidth, halfH), (halfW, halfH) ]
        renderSubQuad(
            canvas, srcBitmap, paint,
            srcPoints = floatArrayOf(
                pTC[0], pTC[1],
                pTR[0], pTR[1],
                pRC[0], pRC[1],
                pCenter[0], pCenter[1]
            ),
            dstRect = Rect(halfW.toInt(), 0, dstWidth, halfH.toInt()),
            dstPoints = floatArrayOf(
                halfW, 0f,
                dstWidth.toFloat(), 0f,
                dstWidth.toFloat(), halfH,
                halfW, halfH
            )
        )

        // Quad 2: Bottom-Right (pCenter, pRC, pBR, pBC) -> [ (halfW, halfH), (dstWidth, halfH), (dstWidth, dstHeight), (halfW, dstHeight) ]
        renderSubQuad(
            canvas, srcBitmap, paint,
            srcPoints = floatArrayOf(
                pCenter[0], pCenter[1],
                pRC[0], pRC[1],
                pBR[0], pBR[1],
                pBC[0], pBC[1]
            ),
            dstRect = Rect(halfW.toInt(), halfH.toInt(), dstWidth, dstHeight),
            dstPoints = floatArrayOf(
                halfW, halfH,
                dstWidth.toFloat(), halfH,
                dstWidth.toFloat(), dstHeight.toFloat(),
                halfW, dstHeight.toFloat()
            )
        )

        // Quad 3: Bottom-Left (pLC, pCenter, pBC, pBL) -> [ (0, halfH), (halfW, halfH), (halfW, dstHeight), (0, dstHeight) ]
        renderSubQuad(
            canvas, srcBitmap, paint,
            srcPoints = floatArrayOf(
                pLC[0], pLC[1],
                pCenter[0], pCenter[1],
                pBC[0], pBC[1],
                pBL[0], pBL[1]
            ),
            dstRect = Rect(0, halfH.toInt(), halfW.toInt(), dstHeight),
            dstPoints = floatArrayOf(
                0f, halfH,
                halfW, halfH,
                halfW, dstHeight.toFloat(),
                0f, dstHeight.toFloat()
            )
        )

        return dstBitmap
    }

    private fun renderSubQuad(
        canvas: Canvas,
        srcBitmap: Bitmap,
        paint: Paint,
        srcPoints: FloatArray,
        dstRect: Rect,
        dstPoints: FloatArray
    ) {
        val matrix = Matrix()
        val success = matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)
        if (success) {
            canvas.save()
            canvas.clipRect(dstRect)
            canvas.drawBitmap(srcBitmap, matrix, paint)
            canvas.restore()
        }
    }

    private fun calculateIntersection(
        p1: FloatArray, p2: FloatArray,
        p3: FloatArray, p4: FloatArray
    ): FloatArray {
        val x1 = p1[0]; val y1 = p1[1]
        val x2 = p2[0]; val y2 = p2[1]
        val x3 = p3[0]; val y3 = p3[1]
        val x4 = p4[0]; val y4 = p4[1]

        val denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
        if (kotlin.math.abs(denom) < 1e-4) {
            // Lines are nearly parallel, return average midpoint
            return floatArrayOf((x1 + x2 + x3 + x4) / 4f, (y1 + y2 + y3 + y4) / 4f)
        }

        val t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom
        val px = x1 + t * (x2 - x1)
        val py = y1 + t * (y2 - y1)
        return floatArrayOf(px, py)
    }
}
