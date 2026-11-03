package com.nothing.pdfmaker.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.hypot

data class PointF8(var x: Float, var y: Float) {
    fun copy(): PointF8 = PointF8(x, y)
}

data class CropMesh8(
    var tl: PointF8, // Top-Left
    var tc: PointF8, // Top-Center
    var tr: PointF8, // Top-Right
    var rc: PointF8, // Right-Center
    var br: PointF8, // Bottom-Right
    var bc: PointF8, // Bottom-Center
    var bl: PointF8, // Bottom-Left
    var lc: PointF8  // Left-Center
) {
    fun getPoints(): List<PointF8> = listOf(tl, tc, tr, rc, br, bc, bl, lc)

    companion object {
        fun createDefault(width: Float, height: Float, marginPercent: Float = 0.05f): CropMesh8 {
            val mx = width * marginPercent
            val my = height * marginPercent
            val w = width - mx
            val h = height - my

            val midX = width / 2f
            val midY = height / 2f

            return CropMesh8(
                tl = PointF8(mx, my),
                tc = PointF8(midX, my),
                tr = PointF8(w, my),
                rc = PointF8(w, midY),
                br = PointF8(w, h),
                bc = PointF8(midX, h),
                bl = PointF8(mx, h),
                lc = PointF8(mx, midY)
            )
        }
    }
}

object Dewarp8PointHelper {

    /**
     * De-warps an 8-point curved/polygon boundary into a flattened, rectangular document.
     * Uses 4-quadrant piecewise perspective projection (homography) to eliminate
     * paper curves, folds, and spine curvature.
     */
    fun dewarp(srcBitmap: Bitmap, mesh: CropMesh8): Bitmap {
        // Compute estimated output dimensions based on average width and height of the 8 points
        val topWidth = hypot(mesh.tr.x - mesh.tl.x, mesh.tr.y - mesh.tl.y)
        val bottomWidth = hypot(mesh.br.x - mesh.bl.x, mesh.br.y - mesh.bl.y)
        val midWidth = hypot(mesh.rc.x - mesh.lc.x, mesh.rc.y - mesh.lc.y)
        val outWidth = maxOf(topWidth, bottomWidth, midWidth).toInt().coerceIn(400, 3000)

        val leftHeight = hypot(mesh.bl.x - mesh.tl.x, mesh.bl.y - mesh.tl.y)
        val rightHeight = hypot(mesh.br.x - mesh.tr.x, mesh.br.y - mesh.tr.y)
        val midHeight = hypot(mesh.bc.x - mesh.tc.x, mesh.bc.y - mesh.tc.y)
        val outHeight = maxOf(leftHeight, rightHeight, midHeight).toInt().coerceIn(400, 4000)

        // Calculate center intersection point P_center from the horizontal and vertical centerlines
        val center = computeLineIntersection(
            mesh.tc, mesh.bc,
            mesh.lc, mesh.rc
        ) ?: PointF8(
            (mesh.tl.x + mesh.tr.x + mesh.br.x + mesh.bl.x) / 4f,
            (mesh.tl.y + mesh.tr.y + mesh.br.y + mesh.bl.y) / 4f
        )

        val destBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(destBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val halfW = outWidth / 2f
        val halfH = outHeight / 2f

        // Quadrant 1: Top-Left (TL, TC, Center, LC) -> Dest (0,0) to (halfW, halfH)
        warpQuadrant(
            canvas, srcBitmap, paint,
            src = floatArrayOf(
                mesh.tl.x, mesh.tl.y,
                mesh.tc.x, mesh.tc.y,
                center.x, center.y,
                mesh.lc.x, mesh.lc.y
            ),
            dstRect = RectF(0f, 0f, halfW, halfH)
        )

        // Quadrant 2: Top-Right (TC, TR, RC, Center) -> Dest (halfW, 0) to (outWidth, halfH)
        warpQuadrant(
            canvas, srcBitmap, paint,
            src = floatArrayOf(
                mesh.tc.x, mesh.tc.y,
                mesh.tr.x, mesh.tr.y,
                mesh.rc.x, mesh.rc.y,
                center.x, center.y
            ),
            dstRect = RectF(halfW, 0f, outWidth.toFloat(), halfH)
        )

        // Quadrant 3: Bottom-Left (LC, Center, BC, BL) -> Dest (0, halfH) to (halfW, outHeight)
        warpQuadrant(
            canvas, srcBitmap, paint,
            src = floatArrayOf(
                mesh.lc.x, mesh.lc.y,
                center.x, center.y,
                mesh.bc.x, mesh.bc.y,
                mesh.bl.x, mesh.bl.y
            ),
            dstRect = RectF(0f, halfH, halfW, outHeight.toFloat())
        )

        // Quadrant 4: Bottom-Right (Center, RC, BR, BC) -> Dest (halfW, halfH) to (outWidth, outHeight)
        warpQuadrant(
            canvas, srcBitmap, paint,
            src = floatArrayOf(
                center.x, center.y,
                mesh.rc.x, mesh.rc.y,
                mesh.br.x, mesh.br.y,
                mesh.bc.x, mesh.bc.y
            ),
            dstRect = RectF(halfW, halfH, outWidth.toFloat(), outHeight.toFloat())
        )

        return destBitmap
    }

    private fun warpQuadrant(
        canvas: Canvas,
        srcBitmap: Bitmap,
        paint: Paint,
        src: FloatArray, // 4 points: TL, TR, BR, BL
        dstRect: RectF
    ) {
        val dst = floatArrayOf(
            dstRect.left, dstRect.top,
            dstRect.right, dstRect.top,
            dstRect.right, dstRect.bottom,
            dstRect.left, dstRect.bottom
        )

        val matrix = Matrix()
        // setPolyToPoly maps 4 non-parallel points to a rectangle via projective homography
        if (matrix.setPolyToPoly(src, 0, dst, 0, 4)) {
            canvas.save()
            canvas.clipRect(dstRect)
            canvas.concat(matrix)
            canvas.drawBitmap(srcBitmap, 0f, 0f, paint)
            canvas.restore()
        }
    }

    private fun computeLineIntersection(
        p1: PointF8, p2: PointF8,
        p3: PointF8, p4: PointF8
    ): PointF8? {
        val d = (p1.x - p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x - p4.x)
        if (kotlin.math.abs(d) < 1e-5f) return null

        val t = ((p1.x - p3.x) * (p3.y - p4.y) - (p1.y - p3.y) * (p3.x - p4.x)) / d
        return PointF8(
            p1.x + t * (p2.x - p1.x),
            p1.y + t * (p2.y - p1.y)
        )
    }
}
