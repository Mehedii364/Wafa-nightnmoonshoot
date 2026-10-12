package com.example.core.model

import android.net.Uri

enum class ImageCategory {
    ALL,
    ORIGINAL,
    MOON,
    NIGHT_STUDIO,
    STACKED,
    PNG_TRANSPARENT,
    WATERMARKED
}

data class CapturedImageItem(
    val id: String,
    val uri: Uri,
    val filePath: String,
    val category: ImageCategory,
    val title: String,
    val timestamp: Long,
    val width: Int = 0,
    val height: Int = 0,
    val iso: Int = 0,
    val exposureTimeSeconds: String = "",
    val zoomLevel: Float = 1.0f,
    val stackedFrameCount: Int = 1,
    val hasAlphaChannel: Boolean = false
)

data class StackingConfig(
    val frameCount: Int = 10,
    val blurThreshold: Float = 80.0f,
    val alignSensitivity: Float = 0.85f,
    val sharpeningStrength: Float = 1.2f
)

enum class StackingStage {
    IDLE,
    CAPTURING,
    ANALYZING,
    ALIGNING,
    BLENDING,
    SHARPENING,
    COMPLETED,
    FAILED
}

data class StackingProgress(
    val stage: StackingStage = StackingStage.IDLE,
    val currentFrame: Int = 0,
    val totalFrames: Int = 0,
    val alignedFrames: Int = 0,
    val rejectedFrames: Int = 0,
    val noiseReductionPercent: Int = 0,
    val errorMessage: String? = null,
    val resultFilePath: String? = null
)

data class MoonPhaseInfo(
    val phaseNameEn: String,
    val phaseNameBn: String,
    val illuminationPercent: Float,
    val ageDays: Float,
    val moonDistanceKm: Long,
    val phaseIndex: Int, // 0 to 7
    val moonriseTime: String?,
    val moonsetTime: String?,
    val photographyTipsEn: String,
    val photographyTipsBn: String
)
