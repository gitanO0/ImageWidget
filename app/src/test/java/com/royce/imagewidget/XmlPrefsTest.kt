package com.royce.imagewidget

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import java.io.File

@RunWith(RobolectricTestRunner::class)
class XmlPrefsTest {

    @Test
    fun testXml() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val p = WidgetState.WidgetProfile("Test", "url", "", 10, "Crop", 1.0f, 0.5f, 0.5f, false, true, "00:00", "06:00", "", false, 15)
        WidgetState.saveProfile(context, p)
        
        val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/image_widget_profiles.xml")
        println(prefsFile.readText())
        
        assertEquals(1, WidgetState.getProfiles(context).size)
    }
}
