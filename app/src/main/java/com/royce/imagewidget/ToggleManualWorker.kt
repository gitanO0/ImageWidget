package com.royce.imagewidget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ToggleManualWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val appWidgetId = inputData.getInt(ImageRefreshWorker.KEY_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return Result.success()

        val currentState = WidgetState.getManualOnly(applicationContext, appWidgetId)
        WidgetState.setManualOnly(applicationContext, appWidgetId, !currentState)

        ImageWidget().updateAll(applicationContext)
        
        return Result.success()
    }
}
