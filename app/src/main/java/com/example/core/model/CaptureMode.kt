package com.example.core.model

enum class CaptureMode {
    PHOTO,
    MOON_PRO,
    NIGHT_STUDIO
}

enum class NightSubMode {
    LIGHT_TRAILS,
    LIGHT_PAINTING,
    ASTROPHOTOGRAPHY
}

enum class CapabilityState {
    SUPPORTED,
    LIMITED,
    UNSUPPORTED,
    PERMISSION_REQUIRED,
    CAMERA_BUSY,
    TEMPORARILY_UNAVAILABLE,
    ERROR
}

enum class DetectionStatus {
    SEARCHING,
    DETECTED,
    PARTIALLY_OBSCURED,
    LOW_CONFIDENCE,
    NOT_DETECTED
}

data class MoonDetectionResult(
    val status: DetectionStatus = DetectionStatus.SEARCHING,
    val normalizedCenterX: Float = 0.5f,
    val normalizedCenterY: Float = 0.5f,
    val normalizedRadius: Float = 0.15f,
    val confidence: Float = 0f,
    val isClippedHighlight: Boolean = false,
    val averageLuminance: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)
