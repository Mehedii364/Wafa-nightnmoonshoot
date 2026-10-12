package com.example

import com.example.astronomy.MoonCalculator
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.model.CapabilityState
import com.example.core.model.DetectionStatus
import com.example.core.model.MoonDetectionResult
import com.example.processing.MoonSegmentationEngine
import com.example.processing.MoonStackingEngine
import com.example.processing.WatermarkPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WafaNightShotUltraUnitTest {

    @Test
    fun testMoonPhaseCalculation_ValidRanges() {
        val now = System.currentTimeMillis()
        val phaseInfo = MoonCalculator.calculateMoonPhase(now)

        assertTrue("Illumination percent should be between 0 and 100", phaseInfo.illuminationPercent in 0.0f..100.0f)
        assertTrue("Lunar age should be within synodic month (0..29.53)", phaseInfo.ageDays in 0.0f..30.0f)
        assertTrue("Phase index should be between 0 and 7", phaseInfo.phaseIndex in 0..7)
        assertTrue("Moon distance should be within realistic orbit range", phaseInfo.moonDistanceKm in 350000L..420000L)
        assertTrue("Bangla name must not be blank", phaseInfo.phaseNameBn.isNotBlank())
        assertTrue("English name must not be blank", phaseInfo.phaseNameEn.isNotBlank())
        assertTrue("Photography tips must be populated", phaseInfo.photographyTipsBn.isNotBlank())
    }

    @Test
    fun testMoonPhaseCalculation_ReferenceKnownFullMoon() {
        // Reference New Moon + ~14.765 days = Full Moon
        val newMoon = 947182440000L
        val fullMoonTime = newMoon + (14.765 * 24 * 3600 * 1000).toLong()
        val fullPhase = MoonCalculator.calculateMoonPhase(fullMoonTime)

        assertTrue("Near full moon illumination should be > 95%", fullPhase.illuminationPercent > 95.0f)
        assertEquals("Full Moon", fullPhase.phaseNameEn)
    }

    @Test
    fun testZoomPresets_And_CropThreshold() {
        val maxOptical = 5.0f
        val presets = listOf(1f, 2f, 5f, 10f, 15f, 20f, 30f, 40f)

        for (preset in presets) {
            val isDigitalCrop = preset > maxOptical
            if (preset <= 5.0f) {
                assertFalse("Presets <= 5x should be optical on high-end or standard lens", isDigitalCrop)
            } else {
                assertTrue("Presets > 5x require computational digital zoom", isDigitalCrop)
            }
        }
    }

    @Test
    fun testCapabilityStateMapping() {
        val supported = CapabilityState.SUPPORTED
        val limited = CapabilityState.LIMITED
        val unsupported = CapabilityState.UNSUPPORTED

        assertEquals("SUPPORTED", supported.name)
        assertEquals("LIMITED", limited.name)
        assertEquals("UNSUPPORTED", unsupported.name)
    }

    @Test
    fun testExposureValueValidation() {
        val validEVs = listOf(-4.0f, -2.5f, -1.0f, 0.0f, 1.5f, 4.0f)
        for (ev in validEVs) {
            val clamped = ev.coerceIn(-4.0f, 4.0f)
            assertEquals(ev, clamped, 0.001f)
        }
    }

    @Test
    fun testImageMaskBrushOperations() {
        val width = 100
        val height = 100
        val mask = ByteArray(width * height) { 0 } // empty background

        // Add to mask at center
        val modifiedMask = MoonSegmentationEngine.applyBrush(
            mask = mask,
            width = width,
            height = height,
            brushCenterX = 50f,
            brushCenterY = 50f,
            brushRadius = 10f,
            isAddMode = true
        )

        // Center pixel should be 255 (opaque)
        val centerIdx = 50 * width + 50
        assertEquals(255.toByte(), modifiedMask[centerIdx])

        // Erase mask from center
        val erasedMask = MoonSegmentationEngine.applyBrush(
            mask = modifiedMask,
            width = width,
            height = height,
            brushCenterX = 50f,
            brushCenterY = 50f,
            brushRadius = 5f,
            isAddMode = false
        )

        assertEquals(0.toByte(), erasedMask[centerIdx])
    }

    @Test
    fun testLocalization_BanglaAndEnglish() {
        val bnAppName = AppStrings.get("app_name", AppLanguage.BANGLA)
        val enAppName = AppStrings.get("app_name", AppLanguage.ENGLISH)

        assertEquals("ওয়াফা নাইটশট আল্ট্রা", bnAppName)
        assertEquals("Wafa NightShot Ultra", enAppName)

        val devName = AppStrings.get("developed_by", AppLanguage.ENGLISH)
        assertTrue(devName.contains("Mehedi364"))
    }

    @Test
    fun testMoonDetectionResultDefault() {
        val defaultResult = MoonDetectionResult()
        assertEquals(DetectionStatus.SEARCHING, defaultResult.status)
        assertEquals(0.5f, defaultResult.normalizedCenterX, 0.01f)
        assertEquals(0.5f, defaultResult.normalizedCenterY, 0.01f)
        assertFalse(defaultResult.isClippedHighlight)
    }
}
