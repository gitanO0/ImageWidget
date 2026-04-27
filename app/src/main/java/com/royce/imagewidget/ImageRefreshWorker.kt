package com.royce.imagewidget

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

object WidgetMutexManager {
    private val mutexes = ConcurrentHashMap<Int, Mutex>()
    fun getMutex(widgetId: Int): Mutex {
        return mutexes.getOrPut(widgetId) { Mutex() }
    }
}

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

        val isManual = inputData.getBoolean(KEY_IS_MANUAL, false)
        
        // Skip night logic
        if (!isManual && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)) {
            val skipNight = WidgetState.getSkipNight(applicationContext, widgetId)
            if (skipNight) {
                val startStr = WidgetState.getSkipStart(applicationContext, widgetId)
                val endStr = WidgetState.getSkipEnd(applicationContext, widgetId)
                try {
                    val now = LocalTime.now()
                    val start = LocalTime.parse(startStr)
                    val end = LocalTime.parse(endStr)
                    
                    val isSkipping = if (start.isBefore(end)) {
                        now.isAfter(start) && now.isBefore(end)
                    } else {
                        // Overlapping midnight (e.g., 22:00 to 06:00)
                        now.isAfter(start) || now.isBefore(end)
                    }
                    
                    if (isSkipping) {
                        Log.d("ImageWorker", "Skipping update due to night mode ($startStr - $endStr)")
                        updateWidgetStatus(widgetId, "Zzz (Night)")
                        return Result.success()
                    }
                } catch (e: Exception) {
                    Log.e("ImageWorker", "Error parsing skip times", e)
                }
            }
        }

        var connection: HttpURLConnection? = null
        val widgetMutex = WidgetMutexManager.getMutex(widgetId)
        
        return widgetMutex.withLock {
            try {
                // Log with Timestamp to see races
                Log.d("ImageWorker", "[START] ID: $widgetId at ${System.currentTimeMillis()}")
                
                updateWidgetStatus(widgetId, "Downloading...")

                val imageUrl = WidgetState.getUrl(applicationContext, widgetId)
                val cacheBustedUrl = "$imageUrl?t=${System.currentTimeMillis()}"
                
                var currentUrl = cacheBustedUrl
                var redirectCount = 0
                val maxRedirects = 3
                
                val finalConnection = withContext(Dispatchers.IO) {
                    var lastConn: HttpURLConnection? = null
                    while (redirectCount < maxRedirects) {
                        lastConn = URL(currentUrl).openConnection() as HttpURLConnection
                        lastConn.apply {
                            connectTimeout = 10000
                            readTimeout = 10000
                            instanceFollowRedirects = true
                            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                            connect()
                        }

                        if (lastConn.responseCode in 301..303 || lastConn.responseCode == 307 || lastConn.responseCode == 308) {
                            val location = lastConn.getHeaderField("Location")
                            if (location != null) {
                                currentUrl = location
                                redirectCount++
                                lastConn.disconnect()
                                continue
                            }
                        }
                        break
                    }
                    lastConn
                }
                
                connection = finalConnection

                if (finalConnection == null || finalConnection.responseCode !in 200..299) {
                    updateWidgetStatus(widgetId, "HTTP ${finalConnection?.responseCode ?: "Error"}")
                    return@withLock Result.failure()
                }

                val tempFile = File(applicationContext.filesDir, "latest_$widgetId.tmp")
                
                // ATOMIC WRITE: Write to a unique temp file first
                val downloadTemp = withContext(Dispatchers.IO) {
                    File.createTempFile("down_$widgetId", ".tmp", applicationContext.filesDir)
                }
                try {
                    withContext(Dispatchers.IO) {
                        downloadTemp.outputStream().use { output ->
                            finalConnection.inputStream.use { input -> input.copyTo(output) }
                        }
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
                if (tempFile.exists() && tempFile.length() > 0) {
                    if (finalFile.exists()) finalFile.delete()
                    if (tempFile.renameTo(finalFile)) {
                        Log.d("ImageWorker", "Final rename success")
                    } else {
                        Log.e("ImageWorker", "Final rename FAILED")
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
