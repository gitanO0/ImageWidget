package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
class ClearTest2 {

    @Test
    fun testClear() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        val p = WidgetState.WidgetProfile("Test1", "url1", "", 10, "Crop", 1f, 0.5f, 0.5f, false, true, "00:00", "06:00", "", false, 15)
        WidgetState.saveProfile(context, p)
        
        WidgetState.clear(context, 123)
        assertEquals(1, WidgetState.getProfiles(context).size)
    }
}
