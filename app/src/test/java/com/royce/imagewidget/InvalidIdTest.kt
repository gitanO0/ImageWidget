package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import android.appwidget.AppWidgetManager

@RunWith(RobolectricTestRunner::class)
class InvalidIdTest {

    @Test
    fun testInvalidId() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        WidgetState.saveProfile(context, WidgetState.WidgetProfile("Test", "url", "", 10, "Crop", 1.0f, 0.5f, 0.5f, false, true, "00:00", "06:00", "", false, 15))
        
        // Android Launcher cancels the widget addition, so onDeleted is called with INVALID_APPWIDGET_ID
        WidgetState.clear(context, AppWidgetManager.INVALID_APPWIDGET_ID)
        
        assertEquals(1, WidgetState.getProfiles(context).size)
    }
}
