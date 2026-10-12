package com.example.camera

import android.graphics.Bitmap
import android.media.Image
import androidx.camera.core.ImageProxy
import com.example.core.model.DetectionStatus
import com.example.core.model.MoonDetectionResult
import java.nio.ByteBuffer
import kotlin.math.sqrt

class MoonDetector {

    private var smoothedCenterX = 0.5f
    private var smoothedCenterY = 0.5f
    private var smoothedRadius = 0.15f
    private var lastDetectionTime = 0L
    private val smoothingFactor = 0.25f // exponential smoothing factor

    // Throttled frame analysis to keep preview buttery smooth and conserve battery
    private var lastAnalyzedTimestamp = 0L
    private val analysisIntervalMillis = 150L // ~6-7 fps analysis is optimal for tracking without thermal load

    fun analyzeImageProxy(imageProxy: ImageProxy): MoonDetectionResult {
        val now = System.currentTimeMillis()
        if (now - lastAnalyzedTimestamp < analysisIntervalMillis) {
            imageProxy.close()
            return MoonDetectionResult(
                status = if (now - lastDetectionTime < 1000L) DetectionStatus.DETECTED else DetectionStatus.SEARCHING,
                normalizedCenterX = smoothedCenterX,
                normalizedCenterY = smoothedCenterY,
                normalizedRadius = smoothedRadius
            )
        }
        lastAnalyzedTimestamp = now

        try {
            val planes = imageProxy.planes
            if (planes.isEmpty()) {
                imageProxy.close()
                return MoonDetectionResult(status = DetectionStatus.SEARCHING)
            }

            val yPlane = planes[0]
            val buffer: ByteBuffer = yPlane.buffer
            val width = imageProxy.width
            val height = imageProxy.height
            val rowStride = yPlane.rowStride
            val pixelStride = yPlane.pixelStride

            // Sub-sample grid for fast on-device analysis
            val sampleStep = 8
            val sampledW = width / sampleStep
            val sampledH = height / sampleStep

            var totalBrightPixels = 0
            var sumX = 0L
            var sumY = 0L
            var maxLuminance = 0
            var clippedPixelCount = 0
            var sumLuminance = 0L

            // 1. Detect bright regions against dark sky
            // Lunar disk in night sky typically has luminance > 160-255 while sky is < 40
            val luminanceThreshold = 155

            for (y in 0 until sampledH) {
                val rowOffset = y * sampleStep * rowStride
                for (x in 0 until sampledW) {
                    val index = rowOffset + (x * sampleStep * pixelStride)
                    if (index < buffer.limit()) {
                        val lum = buffer.get(index).toInt() and 0xFF
                        sumLuminance += lum
                        if (lum > maxLuminance) maxLuminance = lum
                        if (lum > 245) clippedPixelCount++

                        if (lum >= luminanceThreshold) {
                            totalBrightPixels++
                            sumX += (x * sampleStep)
                            sumY += (y * sampleStep)
                        }
                    }
                }
            }

            val totalSampledPixels = sampledW * sampledH
            val avgLuminance = if (totalSampledPixels > 0) (sumLuminance.toFloat() / totalSampledPixels) else 0f
            val isClipped = clippedPixelCount > (totalBrightPixels * 0.45) && totalBrightPixels > 20

            if (totalBrightPixels < 15) {
                // Not enough bright clustered pixels -> either pure dark sky or Moon obscured
                imageProxy.close()
                return MoonDetectionResult(
                    status = if (avgLuminance > 60f) DetectionStatus.SEARCHING else DetectionStatus.NOT_DETECTED,
                    normalizedCenterX = smoothedCenterX,
                    normalizedCenterY = smoothedCenterY,
                    normalizedRadius = smoothedRadius,
                    confidence = 0.1f,
                    isClippedHighlight = false,
                    averageLuminance = avgLuminance
                )
            }

            // Raw Centroid calculation
            val rawCenterX = (sumX.toDouble() / totalBrightPixels).toFloat()
            val rawCenterY = (sumY.toDouble() / totalBrightPixels).toFloat()

            // Calculate approximate radius from bright area
            // Area = PI * R^2 -> R = sqrt(Area / PI)
            val approximateArea = totalBrightPixels.toDouble() * (sampleStep * sampleStep)
            val rawRadius = sqrt(approximateArea / Math.PI).toFloat()

            val normX = (rawCenterX / width).coerceIn(0.05f, 0.95f)
            val normY = (rawCenterY / height).coerceIn(0.05f, 0.95f)
            val normR = (rawRadius / width.coerceAtMost(height)).coerceIn(0.04f, 0.45f)

            // Shape and Contrast verification
            // Calculate variance from center to check circularity/ellipticity
            var radialVarianceSum = 0.0
            var samplesChecked = 0
            for (y in 0 until sampledH step 2) {
                val rowOffset = y * sampleStep * rowStride
                for (x in 0 until sampledW step 2) {
                    val index = rowOffset + (x * sampleStep * pixelStride)
                    if (index < buffer.limit()) {
                        val lum = buffer.get(index).toInt() and 0xFF
                        if (lum >= luminanceThreshold) {
                            val px = (x * sampleStep).toFloat()
                            val py = (y * sampleStep).toFloat()
                            val dist = sqrt((px - rawCenterX) * (px - rawCenterX) + (py - rawCenterY) * (py - rawCenterY))
                            val diff = dist - rawRadius
                            radialVarianceSum += (diff * diff)
                            samplesChecked++
                        }
                    }
                }
            }

            val circularityScore = if (samplesChecked > 0) {
                val stdDev = sqrt(radialVarianceSum / samplesChecked)
                (1.0f - (stdDev.toFloat() / (rawRadius + 1f))).coerceIn(0.2f, 0.98f)
            } else 0.5f

            // Contrast against surrounding sky: check pixels just outside lunar radius
            val confidence = (circularityScore * 0.7f + if (maxLuminance > 200) 0.3f else 0.1f).coerceIn(0f, 1f)

            // Temporal coordinate smoothing to eliminate tracking jitter
            smoothedCenterX = smoothedCenterX + smoothingFactor * (normX - smoothedCenterX)
            smoothedCenterY = smoothedCenterY + smoothingFactor * (normY - smoothedCenterY)
            smoothedRadius = smoothedRadius + smoothingFactor * (normR - smoothedRadius)
            lastDetectionTime = now

            val status = when {
                confidence >= 0.65f -> DetectionStatus.DETECTED
                confidence >= 0.40f -> DetectionStatus.LOW_CONFIDENCE
                totalBrightPixels in 15..50 -> DetectionStatus.PARTIALLY_OBSCURED
                else -> DetectionStatus.SEARCHING
            }

            imageProxy.close()
            return MoonDetectionResult(
                status = status,
                normalizedCenterX = smoothedCenterX,
                normalizedCenterY = smoothedCenterY,
                normalizedRadius = smoothedRadius,
                confidence = confidence,
                isClippedHighlight = isClipped,
                averageLuminance = avgLuminance,
                timestamp = now
            )
        } catch (e: Exception) {
            imageProxy.close()
            return MoonDetectionResult(
                status = DetectionStatus.SEARCHING,
                normalizedCenterX = smoothedCenterX,
                normalizedCenterY = smoothedCenterY,
                normalizedRadius = smoothedRadius
            )
        }
    }

    fun setManualCoordinates(normX: Float, normY: Float, normR: Float = 0.15f) {
        smoothedCenterX = normX.coerceIn(0.05f, 0.95f)
        smoothedCenterY = normY.coerceIn(0.05f, 0.95f)
        smoothedRadius = normR.coerceIn(0.05f, 0.45f)
        lastDetectionTime = System.currentTimeMillis()
    }
}
