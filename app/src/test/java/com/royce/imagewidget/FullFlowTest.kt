package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import android.appwidget.AppWidgetManager

@RunWith(RobolectricTestRunner::class)
class FullFlowTest {

    @Test
    fun testFullFlow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // 1. User imports profiles
        val jsonStr = """
        {
          "TestProfile": {
            "clickUrl": "",
            "skipEnd": "06:00",
            "scale": "Crop",
            "zoom": 1,
            "rotate90": false,
            "manual": false,
            "url": "https://test.com",
            "timeout": 15,
            "zoomCenterX": 0.5,
            "zoomCenterY": 0.5,
            "skipStart": "00:00",
            "rate": 15,
            "discreteTimes": "",
            "skipNight": true
          }
        }
        """
        WidgetState.importProfiles(context, jsonStr)
        
        // Profiles should exist
        assertEquals(1, WidgetState.getProfiles(context).size)
        
        // 2. User configures a widget
        WidgetState.setUrl(context, 123, "https://test.com")
        
        // 3. User adds another widget
        val receiver = ImageWidgetReceiver()
        receiver.onUpdate(context, AppWidgetManager.getInstance(context), intArrayOf(123))
        
        // 4. WidgetConfigActivity opens for widget 456
        val profiles = WidgetState.getProfiles(context)
        
        println("Profiles size for new widget: " + profiles.size)
        assertEquals(1, profiles.size)
    }
}
