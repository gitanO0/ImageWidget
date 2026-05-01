package com.royce.imagewidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.net.toUri

class OpenImageActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Try to get widget ID from Glance's specific key or the standard Android extra
        val widgetId = intent.getIntExtra("app_widget_id", 
            intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID))
        
        val customClickUrl = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            WidgetState.getClickUrl(this, widgetId)
        } else ""

        var finalUrl = if (customClickUrl.isNotBlank()) {
            customClickUrl
        } else {
            val imageUrl = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                WidgetState.getUrl(this, widgetId)
            } else {
                ImageRefreshWorker.IMAGE_URL
            }
            "$imageUrl?t=${System.currentTimeMillis()}"
        }

        if (!finalUrl.startsWith("http://", ignoreCase = true) && !finalUrl.startsWith("https://", ignoreCase = true)) {
            finalUrl = "http://$finalUrl"
        }

        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            finalUrl.toUri()
        )
        startActivity(browserIntent)
        finish()
    }
}
