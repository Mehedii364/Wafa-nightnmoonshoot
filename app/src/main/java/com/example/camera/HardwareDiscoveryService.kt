package com.example.camera

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.core.content.ContextCompat
import com.example.core.model.CameraHardwareReport
import com.example.core.model.CapabilityState
import com.example.core.model.PhysicalCameraDetail

object HardwareDiscoveryService {

    fun inspectHardware(context: Context): CameraHardwareReport {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return CameraHardwareReport(
                overallStatus = CapabilityState.PERMISSION_REQUIRED,
                explanationMessage = "ক্যামেরা পারমিশন প্রয়োজন। ক্যামেরা অ্যাক্সেস চালু করুন। (Camera permission is required)."
            )
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return CameraHardwareReport(
                overallStatus = CapabilityState.ERROR,
                explanationMessage = "CameraManager সার্ভিস পাওয়া যায়নি। (CameraManager service unavailable)."
            )

        try {
            val cameraIds = cameraManager.cameraIdList
            if (cameraIds.isEmpty()) {
                return CameraHardwareReport(
                    overallStatus = CapabilityState.UNSUPPORTED,
                    explanationMessage = "ডিভাইসে কোনো ক্যামেরা মডিউল পাওয়া যায়নি। (No camera devices found)."
                )
            }

            val physicalDetails = mutableListOf<PhysicalCameraDetail>()
            var primaryBackCameraId = cameraIds[0]
            var maxZoom = 1.0f
            var hasOis = false
            var supportsRaw = false
            var expRange = Pair(-12, 12)
            var expStep = 0.1666f

            for (id in cameraIds) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facingInt = chars.get(CameraCharacteristics.LENS_FACING)
                val facingStr = when (facingInt) {
                    CameraCharacteristics.LENS_FACING_BACK -> "BACK (পিছনের মূল ক্যামেরা)"
                    CameraCharacteristics.LENS_FACING_FRONT -> "FRONT (সেলফি ক্যামেরা)"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL (বাহ্যিক)"
                    else -> "UNKNOWN"
                }

                if (facingInt == CameraCharacteristics.LENS_FACING_BACK && primaryBackCameraId == cameraIds[0]) {
                    primaryBackCameraId = id
                }

                val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()
                val zoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
                if (zoom > maxZoom) maxZoom = zoom

                val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                val ois = oisModes?.any { it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON } ?: false
                if (ois) hasOis = true

                val hwLevelInt = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                val hwLevelStr = when (hwLevelInt) {
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                    else -> "UNKNOWN"
                }

                val caps = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val raw = caps?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
                if (raw) supportsRaw = true

                val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.let {
                    it.lower.toFloat()..it.upper.toFloat()
                }
                val expTimeRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.let {
                    it.lower..it.upper
                }

                val afModesArray = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: intArrayOf()
                val afList = afModesArray.map { mode ->
                    when (mode) {
                        CameraCharacteristics.CONTROL_AF_MODE_AUTO -> "AF_AUTO"
                        CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "AF_CONTINUOUS_PICTURE"
                        CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "AF_CONTINUOUS_VIDEO"
                        CameraCharacteristics.CONTROL_AF_MODE_MACRO -> "AF_MACRO"
                        CameraCharacteristics.CONTROL_AF_MODE_OFF -> "AF_MANUAL (ফোকাস বন্ধ/ম্যানুয়াল)"
                        else -> "AF_MODE_$mode"
                    }
                }

                if (id == primaryBackCameraId) {
                    chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)?.let { range ->
                        expRange = Pair(range.lower, range.upper)
                    }
                    chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)?.let { rational ->
                        expStep = rational.toFloat()
                    }
                }

                physicalDetails.add(
                    PhysicalCameraDetail(
                        cameraId = id,
                        lensFacing = facingStr,
                        focalLengths = focalLengths,
                        maxDigitalZoom = zoom,
                        hasOis = ois,
                        hardwareLevel = hwLevelStr,
                        supportsRaw = raw,
                        isoRange = isoRange,
                        exposureTimeRangeNanos = expTimeRange,
                        afModes = afList
                    )
                )
            }

            val isReno13F = android.os.Build.MODEL.contains("CPH2701", ignoreCase = true) ||
                    android.os.Build.MODEL.contains("Reno13", ignoreCase = true)

            val overall = when {
                maxZoom >= 10.0f -> CapabilityState.SUPPORTED
                maxZoom >= 4.0f -> CapabilityState.LIMITED
                else -> CapabilityState.LIMITED
            }

            val explanation = if (isReno13F) {
                "OPPO Reno13 F 4G (CPH2701) সনাক্ত হয়েছে। অপটিক্যাল ও সর্বোচ্চ সেন্সর জুম উপলব্ধ। (Target device confirmed)."
            } else {
                "ডিভাইস: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}। রিয়েলটাইম সেন্সর বিশ্লেষণ সক্রিয়।"
            }

            return CameraHardwareReport(
                targetDeviceModel = "OPPO Reno13 F 4G (CPH2701)",
                detectedDeviceModel = android.os.Build.MODEL,
                detectedManufacturer = android.os.Build.MANUFACTURER,
                isTargetModelMatched = isReno13F,
                overallStatus = overall,
                activeCameraId = primaryBackCameraId,
                physicalCameras = physicalDetails,
                maxZoomAvailable = maxZoom,
                hasOpticalStabilization = hasOis,
                isRawSupported = supportsRaw,
                exposureCompensationRange = expRange,
                exposureCompensationStep = expStep,
                explanationMessage = explanation
            )
        } catch (e: Exception) {
            return CameraHardwareReport(
                overallStatus = CapabilityState.ERROR,
                explanationMessage = "ক্যামেরা পরিদর্শনে ত্রুটি: ${e.localizedMessage}"
            )
        }
    }
}
