package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import android.appwidget.AppWidgetManager

@RunWith(RobolectricTestRunner::class)
class ExactWorkflowTest {

    @Test
    fun testWorkflow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Import profiles
        WidgetState.saveProfile(context, WidgetState.WidgetProfile("Test", "url", "", 10, "Crop", 1.0f, 0.5f, 0.5f, false, true, "00:00", "06:00", "", false, 15))
        
        assertEquals(1, WidgetState.getProfiles(context).size)
        
        // Add new widget
        val receiver = ImageWidgetReceiver()
        receiver.onUpdate(context, AppWidgetManager.getInstance(context), intArrayOf(1))
        
        // Simulating the user canceling config
        receiver.onDeleted(context, intArrayOf(1))
        
        // Ensure profiles are still there
        assertEquals(1, WidgetState.getProfiles(context).size)
    }
}
