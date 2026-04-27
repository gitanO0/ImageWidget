package com.royce.imagewidget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class ToggleManualAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appWidgetId = parameters[WidgetState.WidgetIdKey] ?: AppWidgetManager.INVALID_APPWIDGET_ID
        
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val request = OneTimeWorkRequestBuilder<ToggleManualWorker>()
            .setInputData(workDataOf(ImageRefreshWorker.KEY_WIDGET_ID to appWidgetId))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
