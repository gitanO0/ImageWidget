package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context

@RunWith(RobolectricTestRunner::class)
class CrashTest {

    @Test
    fun testCrash() {
        val p = WidgetState.WidgetProfile(
            "TestProfile", "https://old.com", "", 10, "Crop", 1.0f, 0.5f, 0.5f, false, true, "00:00", "06:00", "", false, 10
        )
        
        val jsonStr = p.toJson()
        println("JSON is: " + jsonStr)
        
        val p2 = WidgetState.WidgetProfile.fromJson("TestProfile", jsonStr)
        println("Parsed OK!")
    }
}
