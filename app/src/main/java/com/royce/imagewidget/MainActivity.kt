package com.royce.imagewidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.edit
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fresh start logic: Clear all data if this is the first run of a new install/version
        handleFreshStart()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppScreen()
                }
            }
        }
    }

    private fun handleFreshStart() {
        val prefs = getSharedPreferences("app_internal_state", MODE_PRIVATE)
        val currentVersion = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
            }
        } catch (_: Exception) { 1L }

        val lastRunVersion = prefs.getLong("last_run_version", -1L)

        if (lastRunVersion != currentVersion) {
            // First run of a new install or update - clear everything
            WidgetState.clearAll(this)
            
            // Trigger refresh for all existing widgets that might still be on the home screen
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(this, ImageWidgetReceiver::class.java))
            
            ids.forEach { id ->
                WidgetState.setStatus(this, id, "Refreshing...")
                val request = OneTimeWorkRequestBuilder<ImageRefreshWorker>()
                    .setInitialDelay(5, java.util.concurrent.TimeUnit.SECONDS)
                    .setInputData(workDataOf(
                        ImageRefreshWorker.KEY_IS_MANUAL to true,
                        ImageRefreshWorker.KEY_WIDGET_ID to id,
                        "is_initial" to true
                    ))
                    .build()

                WorkManager.getInstance(this).enqueueUniqueWork(
                    "image_refresh_now_$id",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }

            prefs.edit { putLong("last_run_version", currentVersion) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppScreenPreview() {
    MaterialTheme {
        Surface {
            AppScreen()
        }
    }
}

@Composable
fun AppScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Image Widget", style = MaterialTheme.typography.headlineMedium)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("To use this app:", color = Color.White)
                Text("1. Go to your home screen.", color = Color.White)
                Text("2. Long press and select 'Widgets'.", color = Color.White)
                Text("3. Find 'ImageWidget' and drag it to your screen.", color = Color.White)
                Text("4. A configuration screen will appear where you can set your unique image URL and refresh rate.", color = Color.White)
                Text("5. You can add as many widgets as you like, each with its own settings!", color = Color.White)
            }
        }
    }
}
