package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
class ClearPrefsTest {

    @Test
    fun testClear() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WidgetState.saveProfile(context, WidgetState.WidgetProfile(
            "TestProfile", "https://old.com", "", 10, "Crop", 1.0f, 0.5f, 0.5f, false, true, "00:00", "06:00", "", false, 10
        ))
        
        println("Saved. Profiles size: " + WidgetState.getProfiles(context).size)
        
        WidgetState.clear(context, 123)
        println("Cleared 123. Profiles size: " + WidgetState.getProfiles(context).size)
        
        val prefs = context.applicationContext.getSharedPreferences("image_widget_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        
        println("Cleared PREFS_NAME. Profiles size: " + WidgetState.getProfiles(context).size)
    }
}
