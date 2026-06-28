package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import android.appwidget.AppWidgetManager
import android.content.SharedPreferences

@RunWith(RobolectricTestRunner::class)
class RobolectricCacheTest {

    @Test
    fun testCache() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        val p = WidgetState.WidgetProfile("Test1", "url1", "", 10, "Crop", 1f, 0.5f, 0.5f, false, true, "00:00", "06:00", "", false, 15)
        WidgetState.saveProfile(context, p)
        
        println("AFTER SAVE:")
        println(WidgetState.getProfiles(context))
        
        val p2 = WidgetState.WidgetProfile("Test2", "url2", "", 10, "Crop", 1f, 0.5f, 0.5f, false, true, "00:00", "06:00", "", false, 15)
        
        // Wait, did we just save correctly?
        // Let's modify directly
        val prefs = context.applicationContext.getSharedPreferences("image_widget_profiles", Context.MODE_PRIVATE)
        prefs.edit().putString("Test2", p2.toJson()).commit()
        
        println("AFTER DIRECT SAVE:")
        println(WidgetState.getProfiles(context))
        
        // Let's add a widget and see if profiles change
        val receiver = ImageWidgetReceiver()
        receiver.onUpdate(context, AppWidgetManager.getInstance(context), intArrayOf(123))
        
        println("AFTER ONUPDATE:")
        println(WidgetState.getProfiles(context))
        
        // Call configure
        WidgetState.setUrl(context, 123, "new")
        
        println("AFTER CONFIGURE:")
        println(WidgetState.getProfiles(context))
        
        WidgetState.clear(context, 123)
        println("AFTER CLEAR:")
        println(WidgetState.getProfiles(context))
    }
}
