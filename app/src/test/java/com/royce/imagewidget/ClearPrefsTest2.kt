package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
class ClearPrefsTest2 {

    @Test
    fun testClear() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Let's print out what `WidgetState.clear` does
        
        // Add a profile
        WidgetState.saveProfile(context, WidgetState.WidgetProfile("Test", "url", "", 10, "Crop", 1.0f, 0.5f, 0.5f, false, true, "00:00", "06:00", "", false, 15))
        
        println("PROFILES: " + WidgetState.getProfiles(context).size)
        
        // This is what ImageWidgetReceiver does when widget is DELETED. 
        // Not added.
        WidgetState.clear(context, 123)
        println("PROFILES AFTER CLEAR: " + WidgetState.getProfiles(context).size)
        
        val p = context.applicationContext.getSharedPreferences("image_widget_prefs", Context.MODE_PRIVATE)
        val p2 = context.applicationContext.getSharedPreferences("image_widget_profiles", Context.MODE_PRIVATE)
        println("PREFS sizes: " + p.all.size + " " + p2.all.size)
        
        // If they ADD a new widget, what gets called?
        // WidgetConfigActivity is shown.
    }
}
