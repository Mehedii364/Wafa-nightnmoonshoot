package com.example.processing

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

object MoonSegmentationEngine {

    data class SegmentationResult(
        val mask: ByteArray, // 0 = background (transparent), 255 = foreground (opaque), intermediate = anti-aliased edge
        val width: Int,
        val height: Int,
        val moonDetected: Boolean,
        val detectedCenterX: Int,
        val detectedCenterY: Int,
        val detectedRadius: Int
    )

    /**
     * Performs adaptive luminance and gradient edge segmentation
     * to isolate the Moon (full disk, gibbous or crescent) from the night sky.
     */
    suspend fun autoSegmentMoon(sourceBitmap: Bitmap): SegmentationResult = withContext(Dispatchers.Default) {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val totalPixels = width * height
        val pixels = IntArray(totalPixels)
        sourceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 1. Calculate luminance histogram and background sky black level
        var sumSkyLum = 0.0
        var skyPixelCount = 0
        var maxLum = 0f

        val step = (width / 150).coerceAtLeast(1)
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val p = pixels[y * width + x]
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                if (lum > maxLum) maxLum = lum
                if (lum < 50f) {
                    sumSkyLum += lum
                    skyPixelCount++
                }
            }
        }

        val skyFloor = if (skyPixelCount > 0) (sumSkyLum / skyPixelCount).toFloat() else 15f
        // Dynamic threshold to separate lunar craters and edge from deep space
        val threshold = (skyFloor + (maxLum - skyFloor) * 0.18f).coerceIn(20f, 95f)

        // 2. Find moon bounding cluster and centroid
        var sumX = 0L
        var sumY = 0L
        var brightCount = 0

        for (y in 0 until height step 2) {
            for (x in 0 until width step 2) {
                val p = pixels[y * width + x]
                val lum = 0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)
                if (lum >= threshold) {
                    sumX += x
                    sumY += y
                    brightCount++
                }
            }
        }

        val centerX = if (brightCount > 0) (sumX / brightCount).toInt() else width / 2
        val centerY = if (brightCount > 0) (sumY / brightCount).toInt() else height / 2

        // Approximate radius
        val approxArea = brightCount * 4.0
        val radius = sqrt(approxArea / Math.PI).toInt().coerceIn(10, width.coerceAtMost(height) / 2)

        // 3. Generate soft alpha mask (with 2-pixel anti-aliasing feather at the boundary)
        val mask = ByteArray(totalPixels)
        val maxDistSq = ((radius * 1.35f) * (radius * 1.35f)).toDouble()

        for (y in 0 until height) {
            val dy = y - centerY
            val dySq = (dy * dy).toDouble()
            val rowOffset = y * width

            for (x in 0 until width) {
                val dx = x - centerX
                val distSq = dx * dx + dySq
                val idx = rowOffset + x

                // Outside lunar envelope is definitely sky
                if (distSq > maxDistSq) {
                    mask[idx] = 0
                    continue
                }

                val p = pixels[idx]
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)
                val lum = 0.299f * r + 0.587f * g + 0.114f * b

                // Soft transition range between threshold - 6 and threshold + 8
                when {
                    lum >= (threshold + 8f) -> {
                        mask[idx] = 255.toByte()
                    }
                    lum <= (threshold - 6f) -> {
                        mask[idx] = 0
                    }
                    else -> {
                        // Linear interpolation for soft anti-aliased edge
                        val alpha = ((lum - (threshold - 6f)) / 14f * 255f).toInt().coerceIn(0, 255)
                        mask[idx] = alpha.toByte()
                    }
                }
            }
        }

        return@withContext SegmentationResult(
            mask = mask,
            width = width,
            height = height,
            moonDetected = brightCount > 50,
            detectedCenterX = centerX,
            detectedCenterY = centerY,
            detectedRadius = radius
        )
    }

    /**
     * Applies manual brush modification (Add to Mask or Erase from Mask)
     */
    fun applyBrush(
        mask: ByteArray,
        width: Int,
        height: Int,
        brushCenterX: Float,
        brushCenterY: Float,
        brushRadius: Float,
        isAddMode: Boolean
    ): ByteArray {
        val newMask = mask.copyOf()
        val rSq = (brushRadius * brushRadius).toDouble()
        val minX = (brushCenterX - brushRadius).toInt().coerceIn(0, width - 1)
        val maxX = (brushCenterX + brushRadius).toInt().coerceIn(0, width - 1)
        val minY = (brushCenterY - brushRadius).toInt().coerceIn(0, height - 1)
        val maxY = (brushCenterY + brushRadius).toInt().coerceIn(0, height - 1)

        val targetAlpha: Byte = if (isAddMode) 255.toByte() else 0.toByte()

        for (y in minY..maxY) {
            val dy = y - brushCenterY
            val dySq = dy * dy
            val rowOffset = y * width
            for (x in minX..maxX) {
                val dx = x - brushCenterX
                if ((dx * dx + dySq) <= rSq) {
                    newMask[rowOffset + x] = targetAlpha
                }
            }
        }
        return newMask
    }

    /**
     * Composites the source bitmap with the alpha mask to produce a genuine 32-bit RGBA bitmap.
     */
    fun createTransparentBitmap(sourceBitmap: Bitmap, mask: ByteArray): Bitmap {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val total = width * height

        val srcPixels = IntArray(total)
        sourceBitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val outPixels = IntArray(total)
        for (i in 0 until total) {
            val p = srcPixels[i]
            val a = mask[i].toInt() and 0xFF
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            outPixels[i] = Color.argb(a, r, g, b)
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }
}
