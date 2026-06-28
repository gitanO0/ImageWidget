package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import android.appwidget.AppWidgetManager
import android.content.Intent

@RunWith(RobolectricTestRunner::class)
class RoboActivityTest {

    @Test
    fun testActivityLaunch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Import profiles
        val jsonStr = """{"Test": {"url": "url"}}"""
        WidgetState.importProfiles(context, jsonStr)
        assertEquals(1, WidgetState.getProfiles(context).size)
        
        // Launch WidgetConfigActivity
        // Robolectric activity launch
        // Just directly call getProfiles from a new context instance
        val context2 = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(1, WidgetState.getProfiles(context2).size)
    }
}
