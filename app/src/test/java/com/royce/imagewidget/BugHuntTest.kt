package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle

@RunWith(RobolectricTestRunner::class)
class BugHuntTest {

    @Test
    fun testReimportBug() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // 1. User opens WidgetConfigActivity
        // In real app, `WidgetConfigActivity` is opened.
        // It has `var profiles by remember { mutableStateOf(WidgetState.getProfiles(context)) }`
        
        // Let's create some profiles
        WidgetState.saveProfile(context, WidgetState.WidgetProfile("Test1", "url1", "", 10, "Crop", 1f, 0.5f, 0.5f, false, true, "00:00", "06:00", "", false, 15))
        
        println("Initial profiles: " + WidgetState.getProfiles(context).size) // Should be 1
        
        // User clicks "Save Config"
        WidgetState.setUrl(context, 123, "url1")
        
        // A new widget is added, which triggers onUpdate?
        val receiver = ImageWidgetReceiver()
        receiver.onUpdate(context, AppWidgetManager.getInstance(context), intArrayOf(123))
        
        // Profiles still there?
        println("After onUpdate: " + WidgetState.getProfiles(context).size) // Should be 1
        
        // What if another widget is opened?
        val currentUrl = WidgetState.getUrl(context, 456)
        println("After getUrl for new widget: " + WidgetState.getProfiles(context).size) // Should be 1
    }
}
