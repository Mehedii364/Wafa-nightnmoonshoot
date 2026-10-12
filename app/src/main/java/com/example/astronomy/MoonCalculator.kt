package com.example.astronomy

import com.example.core.model.MoonPhaseInfo
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

object MoonCalculator {

    // Known reference new moon: January 6, 2000 18:14 UTC
    private const val REFERENCE_NEW_MOON_MILLIS = 947182440000L
    private const val SYNODIC_MONTH_MILLIS = 29.53058867 * 24 * 60 * 60 * 1000

    fun calculateMoonPhase(timeMillis: Long = System.currentTimeMillis(), lat: Double? = null, lon: Double? = null): MoonPhaseInfo {
        val diff = timeMillis - REFERENCE_NEW_MOON_MILLIS
        val cycles = diff.toDouble() / SYNODIC_MONTH_MILLIS
        val cycleFraction = (cycles - kotlin.math.floor(cycles))
        val ageDays = (cycleFraction * 29.53058867).toFloat()

        // Phase angle in radians: 0 = new moon, PI = full moon, 2*PI = new moon
        val phaseAngleRad = cycleFraction * 2.0 * Math.PI
        val illuminationPercent = (((1.0 - cos(phaseAngleRad)) / 2.0) * 100.0).toFloat()

        // Moon distance oscillation (approximate lunar orbit anomaly)
        // Average: 384,400 km, varying between 363,300 km and 405,500 km
        val anomalyRad = cycleFraction * 2.0 * Math.PI * (29.53058867 / 27.554551) // anomalistic month
        val distanceKm = (384400 - 21100 * cos(anomalyRad)).toLong()

        // Determine phase index (0 to 7)
        val phaseIndex = when {
            ageDays < 1.84566 -> 0 // New Moon
            ageDays < 5.53699 -> 1 // Waxing Crescent
            ageDays < 9.22831 -> 2 // First Quarter
            ageDays < 12.9196 -> 3 // Waxing Gibbous
            ageDays < 16.6109 -> 4 // Full Moon
            ageDays < 20.3023 -> 5 // Waning Gibbous
            ageDays < 23.9936 -> 6 // Last Quarter
            ageDays < 27.6849 -> 7 // Waning Crescent
            else -> 0              // New Moon
        }

        val (phaseEn, phaseBn) = when (phaseIndex) {
            0 -> "New Moon" to "অমাবস্যা"
            1 -> "Waxing Crescent" to "ক্রমবর্ধমান হেলাল (ক্রিসেন্ট)"
            2 -> "First Quarter" to "প্রথম চতুর্থাংশ (অর্ধচন্দ্র)"
            3 -> "Waxing Gibbous" to "ক্রমবর্ধমান কুঁজো চাঁদ"
            4 -> "Full Moon" to "পূর্ণিমা"
            5 -> "Waning Gibbous" to "ক্ষীয়মাণ কুঁজো চাঁদ"
            6 -> "Last Quarter" to "শেষ চতুর্থাংশ (অর্ধচন্দ্র)"
            7 -> "Waning Crescent" to "ক্ষীয়মাণ হেলাল (ক্রিসেন্ট)"
            else -> "Moon" to "চাঁদ"
        }

        val (tipsEn, tipsBn) = when (phaseIndex) {
            0 -> "The Moon is not illuminated from Earth. Ideal for astrophotography & deep sky galaxy capture without moonlight interference." to
                    "চাঁদ এখন অদৃশ্য। চাঁদের আলোর ব্যাঘাত ছাড়া দূর মহাকাশের তারা ও গ্যালাক্সি ফটোগ্রাফির জন্য শ্রেষ্ঠ সময়।"
            1, 7 -> "Crescent Moon: Delicate horns and earthshine visible. Use ISO 100-200, shutter 1/60s to 1/125s. Lock focus on the illuminated rim." to
                    "হেলাল বা ক্রিসেন্ট চাঁদ: সূক্ষ্ম প্রান্ত ও হালকা আলো। ISO ১০০-২০০ এবং শাটার ১/৬০-১/১২৫ ব্যবহার করুন। উজ্জ্বল প্রান্তে ফোকাস লক করুন।"
            2, 6 -> "Quarter Moon: The terminator line reveals high-contrast craters and mountain shadows! Use ISO 100, 1/125s to 1/250s, tripod recommended." to
                    "অর্ধচন্দ্র: টার্মিনেটর রেখা বরাবর গভীর খাদ ও চাঁদের পাহাড়ের ছায়া স্পষ্ট দেখা যায়। ISO ১০০, শাটার ১/১২৫-১/২৫০ এবং ট্রাইপড প্রস্তাবিত।"
            3, 5 -> "Gibbous Moon: High surface brightness. Lower EV compensation (-1.5 to -2.0) to prevent lunar maria highlight blowout." to
                    "কুঁজো চাঁদ: উচ্চ উজ্জ্বলতা। চাঁদের সমুদ্র বা মারিয়া অঞ্চলের স্পষ্টতার জন্য EV কমিয়ে (-১.৫ থেকে -২.০) রাখুন।"
            4 -> "Full Moon: Maximum surface reflection. High risk of overexposure. Use Looney 11 rule: ISO 100, shutter 1/250s to 1/500s, EV -2.0 to -3.0." to
                    "পূর্ণিমা: তীব্র প্রতিফলন! ওভার-এক্সপোজার এড়াতে লুনী ১১ নিয়ম মেনে চলুন: ISO ১০০, শাটার ১/২৫০-১/৫০০ এবং EV -২.০ থেকে -৩.০ রাখুন।"
            else -> "Use tripod, low ISO, and multi-frame stacking." to "ট্রাইপড, কম ISO এবং মাল্টি-ফ্রেম স্ট্যাকিং ব্যবহার করুন।"
        }

        // Approximate moonrise/moonset times (if location provided) or null
        var moonriseStr: String? = null
        var moonsetStr: String? = null
        if (lat != null && lon != null) {
            // Documented astronomical approximation for rise/set offset:
            // Moon rises approximately 50 minutes later each day than the previous day
            val calendar = Calendar.getInstance(TimeZone.getDefault())
            calendar.timeInMillis = timeMillis
            val hourOfDay = (6 + (ageDays * 0.8)).toInt() % 24
            val minute = ((ageDays * 48.0).toInt() % 60)
            val setHour = (hourOfDay + 12) % 24
            moonriseStr = String.format("%02d:%02d (আনুমানিক)", hourOfDay, minute)
            moonsetStr = String.format("%02d:%02d (আনুমানিক)", setHour, minute)
        }

        return MoonPhaseInfo(
            phaseNameEn = phaseEn,
            phaseNameBn = phaseBn,
            illuminationPercent = illuminationPercent,
            ageDays = ageDays,
            moonDistanceKm = distanceKm,
            phaseIndex = phaseIndex,
            moonriseTime = moonriseStr,
            moonsetTime = moonsetStr,
            photographyTipsEn = tipsEn,
            photographyTipsBn = tipsBn
        )
    }
}
