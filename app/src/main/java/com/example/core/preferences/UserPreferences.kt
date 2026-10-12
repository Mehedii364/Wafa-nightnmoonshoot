package com.example.core.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.core.localization.AppLanguage
import com.example.core.model.CaptureMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("wafa_nightshot_prefs", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(
        if (prefs.getString("lang", "bn") == "en") AppLanguage.ENGLISH else AppLanguage.BANGLA
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _gridEnabled = MutableStateFlow(prefs.getBoolean("grid", true))
    val gridEnabled: StateFlow<Boolean> = _gridEnabled.asStateFlow()

    private val _autoMoonDetection = MutableStateFlow(prefs.getBoolean("auto_moon", true))
    val autoMoonDetection: StateFlow<Boolean> = _autoMoonDetection.asStateFlow()

    private val _timerSeconds = MutableStateFlow(prefs.getInt("timer", 0))
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _rawCapture = MutableStateFlow(prefs.getBoolean("raw_cap", false))
    val rawCapture: StateFlow<Boolean> = _rawCapture.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString("lang", lang.code).apply()
        _language.value = lang
    }

    fun setGridEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("grid", enabled).apply()
        _gridEnabled.value = enabled
    }

    fun setAutoMoonDetection(enabled: Boolean) {
        prefs.edit().putBoolean("auto_moon", enabled).apply()
        _autoMoonDetection.value = enabled
    }

    fun setTimerSeconds(seconds: Int) {
        prefs.edit().putInt("timer", seconds).apply()
        _timerSeconds.value = seconds
    }

    fun setRawCapture(enabled: Boolean) {
        prefs.edit().putBoolean("raw_cap", enabled).apply()
        _rawCapture.value = enabled
    }
}
