package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.camera.HardwareDiscoveryService
import com.example.core.preferences.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Wafa NightShot Ultra", appName)
    }

    @Test
    fun `user preferences stores and recovers settings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferences(context)
        prefs.setGridEnabled(true)
        assertEquals(true, prefs.gridEnabled.value)
        prefs.setTimerSeconds(5)
        assertEquals(5, prefs.timerSeconds.value)
    }

    @Test
    fun `hardware discovery runs on robolectric context without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val report = HardwareDiscoveryService.inspectHardware(context)
        assertNotNull(report)
        assertNotNull(report.targetDeviceModel)
    }
}
