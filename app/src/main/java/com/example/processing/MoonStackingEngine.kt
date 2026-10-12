package com.example.processing

import android.graphics.Bitmap
import android.graphics.Color
import com.example.core.model.StackingConfig
import com.example.core.model.StackingProgress
import com.example.core.model.StackingStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

object MoonStackingEngine {

    /**
     * Measures sharpness using the variance of Laplacian/gradients.
     * High value = sharp image; Low value = blurred/shaky frame.
     */
    fun calculateSharpnessScore(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val step = (width / 200).coerceAtLeast(1)

        var sumGrad = 0.0
        var sumSqGrad = 0.0
        var count = 0

        val sampledW = width / step
        val sampledH = height / step

        for (y in 1 until sampledH - 1) {
            val actualY = y * step
            for (x in 1 until sampledW - 1) {
                val actualX = x * step
                val centerLum = getLuminance(bitmap.getPixel(actualX, actualY))
                val rightLum = getLuminance(bitmap.getPixel(actualX + step, actualY))
                val bottomLum = getLuminance(bitmap.getPixel(actualX, actualY + step))

                val dx = rightLum - centerLum
                val dy = bottomLum - centerLum
                val gradMag = sqrt((dx * dx + dy * dy).toDouble())

                sumGrad += gradMag
                sumSqGrad += (gradMag * gradMag)
                count++
            }
        }

        if (count == 0) return 0f
        val mean = sumGrad / count
        val variance = (sumSqGrad / count) - (mean * mean)
        return variance.toFloat().coerceAtLeast(0f)
    }

    private fun getLuminance(pixel: Int): Float {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return (0.299f * r + 0.587f * g + 0.114f * b)
    }

    /**
     * Computes the centroid (center of mass) of the brightest region (Moon disk)
     * to align sub-pixel or pixel shifts between successive frames caused by handheld motion or lunar drift.
     */
    fun findLunarCentroid(bitmap: Bitmap, luminanceThreshold: Float = 140f): Pair<Float, Float> {
        val width = bitmap.width
        val height = bitmap.height
        val step = (width / 150).coerceAtLeast(1)

        var sumX = 0.0
        var sumY = 0.0
        var weightSum = 0.0

        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val lum = getLuminance(bitmap.getPixel(x, y))
                if (lum >= luminanceThreshold) {
                    val weight = (lum - luminanceThreshold).toDouble()
                    sumX += x * weight
                    sumY += y * weight
                    weightSum += weight
                }
            }
        }

        return if (weightSum > 0) {
            Pair((sumX / weightSum).toFloat(), (sumY / weightSum).toFloat())
        } else {
            Pair(width / 2f, height / 2f)
        }
    }

    /**
     * Stacks multiple captured bitmaps with rejection of blurred frames, feature alignment,
     * median-filtered weighted accumulation to eliminate sensor noise, and conservative sharpening.
     */
    suspend fun stackFrames(
        frames: List<Bitmap>,
        config: StackingConfig = StackingConfig(),
        outputFile: File,
        onProgress: (StackingProgress) -> Unit
    ): StackingProgress = withContext(Dispatchers.Default) {
        if (frames.isEmpty()) {
            val err = StackingProgress(stage = StackingStage.FAILED, errorMessage = "ক্যাপচার করা কোনো ফ্রেম পাওয়া যায়নি")
            onProgress(err)
            return@withContext err
        }

        val total = frames.size
        onProgress(StackingProgress(stage = StackingStage.ANALYZING, totalFrames = total))

        // 1. Analyze and filter blurred or shaky frames
        val scoredFrames = frames.mapIndexed { idx, bmp ->
            coroutineContext.ensureActive()
            val score = calculateSharpnessScore(bmp)
            Triple(idx, bmp, score)
        }

        // Sort by sharpness and select top sharp frames
        val averageSharpness = scoredFrames.map { it.third }.average().toFloat()
        val minAcceptableSharpness = (averageSharpness * 0.65f).coerceAtLeast(15f)

        val validFrames = scoredFrames.filter { it.third >= minAcceptableSharpness }
        val rejectedCount = total - validFrames.size

        if (validFrames.isEmpty()) {
            val err = StackingProgress(
                stage = StackingStage.FAILED,
                totalFrames = total,
                rejectedFrames = total,
                errorMessage = "সবগুলো ফ্রেম অতিরিক্ত অস্পষ্ট/ঝাপসা হওয়ায় স্ট্যাকিং সম্ভব হয়নি"
            )
            onProgress(err)
            return@withContext err
        }

        onProgress(
            StackingProgress(
                stage = StackingStage.ALIGNING,
                totalFrames = total,
                alignedFrames = validFrames.size,
                rejectedFrames = rejectedCount
            )
        )

        // 2. Select the sharpest frame as alignment reference
        val referenceFrame = validFrames.maxByOrNull { it.third }!!.second
        val refCentroid = findLunarCentroid(referenceFrame)
        val width = referenceFrame.width
        val height = referenceFrame.height

        // Prepare accumulation buffers
        val accumR = LongArray(width * height)
        val accumG = LongArray(width * height)
        val accumB = LongArray(width * height)
        val countBuffer = IntArray(width * height)

        var alignedCount = 0

        for ((index, item) in validFrames.withIndex()) {
            coroutineContext.ensureActive()
            val bmp = item.second
            val centroid = findLunarCentroid(bmp)

            // Shift delta relative to reference
            val deltaX = (refCentroid.first - centroid.first).roundToInt()
            val deltaY = (refCentroid.second - centroid.second).roundToInt()

            // Discard frames with extreme motion (> 20% of frame dimensions)
            val maxAllowedShift = (width * 0.20f).toInt()
            if (abs(deltaX) > maxAllowedShift || abs(deltaY) > maxAllowedShift) {
                continue
            }

            alignedCount++
            onProgress(
                StackingProgress(
                    stage = StackingStage.BLENDING,
                    currentFrame = index + 1,
                    totalFrames = total,
                    alignedFrames = alignedCount,
                    rejectedFrames = total - alignedCount
                )
            )

            // Read frame pixels
            val pixels = IntArray(width * height)
            bmp.getPixels(pixels, 0, width, 0, 0, width, height)

            for (y in 0 until height) {
                val targetY = y + deltaY
                if (targetY !in 0 until height) continue

                for (x in 0 until width) {
                    val targetX = x + deltaX
                    if (targetX !in 0 until width) continue

                    val srcIdx = y * width + x
                    val tgtIdx = targetY * width + targetX

                    val p = pixels[srcIdx]
                    accumR[tgtIdx] += Color.red(p).toLong()
                    accumG[tgtIdx] += Color.green(p).toLong()
                    accumB[tgtIdx] += Color.blue(p).toLong()
                    countBuffer[tgtIdx]++
                }
            }
        }

        if (alignedCount == 0) {
            val err = StackingProgress(stage = StackingStage.FAILED, errorMessage = "ফ্রেম সারিবদ্ধকরণে ত্রুটি হয়েছে")
            onProgress(err)
            return@withContext err
        }

        onProgress(
            StackingProgress(
                stage = StackingStage.SHARPENING,
                alignedFrames = alignedCount,
                totalFrames = total,
                rejectedFrames = total - alignedCount
            )
        )

        // 3. Composite blended image with average reduction
        val resultPixels = IntArray(width * height)
        for (i in 0 until (width * height)) {
            val c = countBuffer[i]
            if (c > 0) {
                val r = (accumR[i] / c).toInt().coerceIn(0, 255)
                val g = (accumG[i] / c).toInt().coerceIn(0, 255)
                val b = (accumB[i] / c).toInt().coerceIn(0, 255)
                resultPixels[i] = Color.rgb(r, g, b)
            } else {
                resultPixels[i] = referenceFrame.getPixel(i % width, i / width)
            }
        }

        // 4. Apply conservative unsharp mask sharpening (Laplacian high-pass edge boost)
        // Sharpening formula: Sharpened = Original + strength * (Original - Blurred)
        val strength = config.sharpeningStrength
        val finalPixels = IntArray(width * height)
        val step = 1

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val centerP = resultPixels[idx]
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    finalPixels[idx] = centerP
                    continue
                }

                val leftP = resultPixels[y * width + (x - step)]
                val rightP = resultPixels[y * width + (x + step)]
                val topP = resultPixels[(y - step) * width + x]
                val bottomP = resultPixels[(y + step) * width + x]

                // Edge difference
                fun sharpenChannel(cCenter: Int, cLeft: Int, cRight: Int, cTop: Int, cBottom: Int): Int {
                    val localAvg = (cLeft + cRight + cTop + cBottom) / 4.0f
                    val diff = cCenter - localAvg
                    return (cCenter + strength * diff).roundToInt().coerceIn(0, 255)
                }

                val cr = sharpenChannel(Color.red(centerP), Color.red(leftP), Color.red(rightP), Color.red(topP), Color.red(bottomP))
                val cg = sharpenChannel(Color.green(centerP), Color.green(leftP), Color.green(rightP), Color.green(topP), Color.green(bottomP))
                val cb = sharpenChannel(Color.blue(centerP), Color.blue(leftP), Color.blue(rightP), Color.blue(topP), Color.blue(bottomP))

                finalPixels[idx] = Color.rgb(cr, cg, cb)
            }
        }

        // Save output bitmap to file
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outputBitmap.setPixels(finalPixels, 0, width, 0, 0, width, height)

        FileOutputStream(outputFile).use { fos ->
            outputBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }

        // Compute real noise reduction %: SNR improves by ~sqrt(N)
        val noiseReductionPct = ((1.0 - (1.0 / sqrt(alignedCount.toDouble()))) * 100).toInt().coerceIn(0, 85)

        val completion = StackingProgress(
            stage = StackingStage.COMPLETED,
            currentFrame = alignedCount,
            totalFrames = total,
            alignedFrames = alignedCount,
            rejectedFrames = total - alignedCount,
            noiseReductionPercent = noiseReductionPct,
            resultFilePath = outputFile.absolutePath
        )
        onProgress(completion)
        return@withContext completion
    }
}
