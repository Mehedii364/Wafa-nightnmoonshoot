package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class WatermarkPosition {
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    TOP_RIGHT,
    TOP_LEFT,
    CENTER
}

data class WatermarkOptions(
    val text: String = "Wafa NightShot Ultra",
    val includeTimestamp: Boolean = true,
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val opacity: Float = 0.85f, // 0.0f to 1.0f
    val scaleFactor: Float = 1.0f,
    val rotationDegrees: Float = 0f,
    val signatureBitmap: Bitmap? = null,
    val logoBitmap: Bitmap? = null
)

object WatermarkComposer {

    suspend fun applyWatermark(
        sourceBitmap: Bitmap,
        options: WatermarkOptions,
        outputFile: File
    ): Boolean = withContext(Dispatchers.Default) {
        try {
            val width = sourceBitmap.width
            val height = sourceBitmap.height

            val mutableBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            val margin = (width * 0.04f).coerceAtLeast(24f)
            val alphaInt = (options.opacity * 255).toInt().coerceIn(0, 255)

            // 1. Draw Signature or Logo if present
            val graphicOverlay = options.signatureBitmap ?: options.logoBitmap
            if (graphicOverlay != null) {
                val targetOverlayWidth = (width * 0.28f * options.scaleFactor).toInt().coerceAtLeast(50)
                val targetOverlayHeight = ((targetOverlayWidth.toFloat() / graphicOverlay.width) * graphicOverlay.height).toInt().coerceAtLeast(30)
                val scaledOverlay = Bitmap.createScaledBitmap(graphicOverlay, targetOverlayWidth, targetOverlayHeight, true)

                val (ox, oy) = when (options.position) {
                    WatermarkPosition.BOTTOM_RIGHT -> Pair(width - targetOverlayWidth - margin, height - targetOverlayHeight - margin)
                    WatermarkPosition.BOTTOM_LEFT -> Pair(margin, height - targetOverlayHeight - margin)
                    WatermarkPosition.TOP_RIGHT -> Pair(width - targetOverlayWidth - margin, margin)
                    WatermarkPosition.TOP_LEFT -> Pair(margin, margin)
                    WatermarkPosition.CENTER -> Pair((width - targetOverlayWidth) / 2f, (height - targetOverlayHeight) / 2f)
                }

                val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    alpha = alphaInt
                }

                canvas.save()
                canvas.rotate(options.rotationDegrees, ox + targetOverlayWidth / 2f, oy + targetOverlayHeight / 2f)
                canvas.drawBitmap(scaledOverlay, ox, oy, overlayPaint)
                canvas.restore()
            }

            // 2. Draw Text Watermark if specified
            if (options.text.isNotBlank() || options.includeTimestamp) {
                val fullText = buildString {
                    if (options.text.isNotBlank()) append(options.text)
                    if (options.includeTimestamp) {
                        if (isNotEmpty()) append(" • ")
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        append(sdf.format(Date()))
                    }
                }

                val textSizePx = (width * 0.032f * options.scaleFactor).coerceIn(24f, 96f)
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = textSizePx
                    alpha = alphaInt
                    setShadowLayer(4f, 1f, 1f, Color.argb(180, 0, 0, 0))
                }

                val bounds = Rect()
                textPaint.getTextBounds(fullText, 0, fullText.length, bounds)

                val (tx, ty) = when (options.position) {
                    WatermarkPosition.BOTTOM_RIGHT -> Pair(width - bounds.width() - margin, height - margin)
                    WatermarkPosition.BOTTOM_LEFT -> Pair(margin, height - margin)
                    WatermarkPosition.TOP_RIGHT -> Pair(width - bounds.width() - margin, margin + bounds.height())
                    WatermarkPosition.TOP_LEFT -> Pair(margin, margin + bounds.height())
                    WatermarkPosition.CENTER -> Pair((width - bounds.width()) / 2f, (height + bounds.height()) / 2f)
                }

                canvas.save()
                canvas.rotate(options.rotationDegrees, tx + bounds.width() / 2f, ty - bounds.height() / 2f)
                canvas.drawText(fullText, tx, ty, textPaint)
                canvas.restore()
            }

            FileOutputStream(outputFile).use { fos ->
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
