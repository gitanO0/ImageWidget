package com.royce.imagewidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.WorkManager

class ImageWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ImageWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // We don't trigger workers here anymore to avoid race conditions 
        // with the configuration activity or manual refresh actions.
        // Initial setup is handled by WidgetConfigActivity.
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            Log.d("ImageWidgetReceiver", "Cleaning up resources for widget ID: $appWidgetId")
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork("image_refresh_$appWidgetId")
            workManager.cancelUniqueWork("image_refresh_now_$appWidgetId")
            WidgetState.clear(context, appWidgetId)
        }
    }
}
