package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
class FlowTest {

    @Test
    fun testFlow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
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
        
        val count = WidgetState.importProfiles(context, jsonStr)
        assertEquals(1, count)
        
        val profiles = WidgetState.getProfiles(context)
        assertEquals(1, profiles.size)
        
        // Simulating the addition of a widget by doing WidgetState.clear (what onDeleted does)
        WidgetState.clear(context, 123)
        
        // Simulating second widget configuration
        val context2 = ApplicationProvider.getApplicationContext<Context>()
        val profiles123 = WidgetState.getProfiles(context2)
        assertEquals(1, profiles123.size)
        
        println("All tests passed!")
    }
}
