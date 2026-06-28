package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import android.appwidget.AppWidgetManager

@RunWith(RobolectricTestRunner::class)
class ProcessRestartTest {

    @Test
    fun testProcessRestart() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Save a profile
        WidgetState.saveProfile(context, WidgetState.WidgetProfile(
            "TestProfile", "https://old.com", "", 10, "Crop", 1.0f, 0.5f, 0.5f, false, true, "00:00", "06:00", "", false, 10
        ))
        
        assertEquals(1, WidgetState.getProfiles(context).size)
        
        // Let's create a NEW context to simulate process restart/activity start
        val context2 = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(1, WidgetState.getProfiles(context2).size)
    }
}
