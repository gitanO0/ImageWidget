package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import android.appwidget.AppWidgetManager

@RunWith(RobolectricTestRunner::class)
class BugTest2 {

    @Test
    fun testReimportBug() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // 1. Create a profile
        WidgetState.saveProfile(context, WidgetState.WidgetProfile("Test1", "url1", "", 10, "Crop", 1f, 0.5f, 0.5f, false, true, "00:00", "06:00", "", false, 15))
        
        // 2. Set url on a new widget 123
        WidgetState.setUrl(context, 123, "url1")
        
        // 3. User adds the widget to homescreen. What actually gets called in real code?
        // WidgetConfigActivity finishes.
        
        val receiver = ImageWidgetReceiver()
        receiver.onUpdate(context, AppWidgetManager.getInstance(context), intArrayOf(123))
        
        assertEquals(1, WidgetState.getProfiles(context).size)
    }
}
