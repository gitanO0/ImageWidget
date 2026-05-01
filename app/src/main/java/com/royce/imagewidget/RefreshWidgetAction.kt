package com.royce.imagewidget

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appWidgetId = parameters[WidgetState.WidgetIdKey] ?: return

        if (!NetworkUtils.isNetworkAvailable(context)) {
            Toast.makeText(context, "No internet...", Toast.LENGTH_SHORT).show()
            return
        }

        // Update state to show "Refreshing..." immediately
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[ImageWidget.StatusKey] = "Refreshing..."
        }
        ImageWidget().update(context, glanceId)

        val request = OneTimeWorkRequestBuilder<ImageRefreshWorker>()
            .setInputData(workDataOf(
                ImageRefreshWorker.KEY_IS_MANUAL to true,
                ImageRefreshWorker.KEY_WIDGET_ID to appWidgetId
            ))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "image_refresh_now_$appWidgetId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
