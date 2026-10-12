package com.example.core.model

data class PhysicalCameraDetail(
    val cameraId: String,
    val lensFacing: String, // FRONT, BACK, EXTERNAL
    val focalLengths: List<Float>,
    val maxDigitalZoom: Float,
    val hasOis: Boolean,
    val hardwareLevel: String,
    val supportsRaw: Boolean,
    val isoRange: ClosedFloatingPointRange<Float>?,
    val exposureTimeRangeNanos: ClosedRange<Long>?,
    val afModes: List<String>
)

data class CameraHardwareReport(
    val targetDeviceModel: String = "OPPO Reno13 F 4G (CPH2701)",
    val detectedDeviceModel: String = android.os.Build.MODEL,
    val detectedManufacturer: String = android.os.Build.MANUFACTURER,
    val isTargetModelMatched: Boolean = false,
    val overallStatus: CapabilityState = CapabilityState.SUPPORTED,
    val activeCameraId: String = "0",
    val physicalCameras: List<PhysicalCameraDetail> = emptyList(),
    val maxZoomAvailable: Float = 10f,
    val hasOpticalStabilization: Boolean = false,
    val isRawSupported: Boolean = false,
    val exposureCompensationRange: Pair<Int, Int> = Pair(-12, 12),
    val exposureCompensationStep: Float = 0.1666f,
    val explanationMessage: String = ""
)
