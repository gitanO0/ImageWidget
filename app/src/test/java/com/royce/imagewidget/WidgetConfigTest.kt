package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import android.appwidget.AppWidgetManager

@RunWith(RobolectricTestRunner::class)
class WidgetConfigTest {

    @Test
    fun testProfilesPersist() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Save a profile
        WidgetState.saveProfile(context, WidgetState.WidgetProfile(
            "TestProfile", "https://old.com", "", 10, "Crop", 1.0f, 0.5f, 0.5f, false, true, "00:00", "06:00", "", false, 10
        ))
        
        // Get profiles
        val profiles = WidgetState.getProfiles(context)
        assertEquals(1, profiles.size)
        
        // Simulate app restart / new context? 
        // Robolectric uses same context in the test, but we can verify it's persisted in the preferences
        val prefs = context.applicationContext.getSharedPreferences("image_widget_profiles", Context.MODE_PRIVATE)
        assertEquals(1, prefs.all.size)
    }
}
