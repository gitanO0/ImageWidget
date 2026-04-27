package com.royce.imagewidget

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import java.time.Instant

class ImageRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_IS_MANUAL = "is_manual"
        const val KEY_WIDGET_ID = "app_widget_id"
        const val IMAGE_URL = "https://picsum.photos/1024/1024"
    }

    override suspend fun doWork(): Result {
        val widgetId = inputData.getInt(KEY_WIDGET_ID, -1)
        if (widgetId == -1) return Result.success()

        var connection: HttpURLConnection? = null
        return try {
            // Log with Timestamp to see races
            Log.d("ImageWorker", "[START] ID: $widgetId at ${System.currentTimeMillis()}")
            
            updateWidgetStatus(widgetId, "Downloading...")

            val imageUrl = WidgetState.getUrl(applicationContext, widgetId)
            val cacheBustedUrl = "$imageUrl?t=${System.currentTimeMillis()}"
            
            var currentUrl = cacheBustedUrl
            var redirectCount = 0
            val maxRedirects = 3
            
            while (redirectCount < maxRedirects) {
                connection = withContext(Dispatchers.IO) {
                    URL(currentUrl).openConnection()
                } as HttpURLConnection
                connection.apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                    connect()
                }

                if (connection.responseCode in 301..303 || connection.responseCode == 307 || connection.responseCode == 308) {
                    val location = connection.getHeaderField("Location")
                    if (location != null) {
                        currentUrl = location
                        redirectCount++
                        connection.disconnect()
                        continue
                    }
                }
                break
            }

            val finalConnection = connection!!
            if (finalConnection.responseCode !in 200..299) {
                updateWidgetStatus(widgetId, "HTTP ${finalConnection.responseCode}")
                return Result.failure()
            }

            val tempFile = File(applicationContext.filesDir, "latest_$widgetId.tmp")
            
            // ATOMIC WRITE: Write to a unique temp file first
            val downloadTemp = File.createTempFile("down_$widgetId", ".tmp", applicationContext.filesDir)
            try {
                downloadTemp.outputStream().use { output ->
                    finalConnection.inputStream.use { input -> input.copyTo(output) }
                }
                
                // Only rename if download was non-zero
                if (downloadTemp.length() > 0) {
                    if (tempFile.exists()) tempFile.delete()
                    if (downloadTemp.renameTo(tempFile)) {
                        Log.d("ImageWorker", "Download atomic rename success")
                    }
                }
            } finally {
                if (downloadTemp.exists()) downloadTemp.delete()
            }

            val finalFile = WidgetState.imageFile(applicationContext, widgetId)
            
            // Critical Section: Move temp to final
            synchronized(this) {
                if (tempFile.exists() && tempFile.length() > 0) {
                    if (finalFile.exists()) finalFile.delete()
                    if (tempFile.renameTo(finalFile)) {
                        Log.d("ImageWorker", "Final rename success")
                    } else {
                        Log.e("ImageWorker", "Final rename FAILED")
                    }
                }
            }
            
            if (finalFile.exists() && finalFile.length() > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WidgetState.setLastUpdated(applicationContext, widgetId, Instant.now().toString())
                }
                updateWidgetStatus(widgetId, "OK")
                Log.d("ImageWorker", "[SUCCESS] ID: $widgetId")
                Result.success()
            } else {
                updateWidgetStatus(widgetId, "Empty File")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("ImageWorker", "Error in worker: ${e.message}", e)
            updateWidgetStatus(widgetId, "Failed")
            Result.retry()
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun updateWidgetStatus(widgetId: Int, status: String) {
        val context = applicationContext
        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceId = manager.getGlanceIdBy(widgetId)
            
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[ImageWidget.StatusKey] = status
            }
            ImageWidget().update(context, glanceId)
        } catch (e: Exception) {
            Log.e("ImageWorker", "Status update failed", e)
        }
    }
}
