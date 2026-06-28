package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ProcessKillTest {

    @Test
    fun testProcessKill() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        WidgetState.saveProfile(context, WidgetState.WidgetProfile("Test", "url", "", 10, "Crop", 1.0f, 0.5f, 0.5f, false, true, "00:00", "06:00", "", false, 15))
        assertEquals(1, WidgetState.getProfiles(context).size)
        
        // Verify the file exists on disk
        val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/image_widget_profiles.xml")
        println("File exists: " + prefsFile.exists())
        println("File content:\n" + prefsFile.readText())
    }
}
