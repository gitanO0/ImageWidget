package com.royce.imagewidget

import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        appWidgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        val currentUrl = WidgetState.getUrl(this, appWidgetId)
        val currentRate = WidgetState.getRefreshRate(this, appWidgetId)
        val currentScale = WidgetState.getScaleType(this, appWidgetId)
        val currentManual = WidgetState.getManualOnly(this, appWidgetId)
        val currentZoom = WidgetState.getZoomFactor(this, appWidgetId)
        val currentSkipNight = WidgetState.getSkipNight(this, appWidgetId)
        val currentSkipStart = WidgetState.getSkipStart(this, appWidgetId)
        val currentSkipEnd = WidgetState.getSkipEnd(this, appWidgetId)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConfigScreen(
                        initialUrl = currentUrl,
                        initialRate = currentRate,
                        initialScale = currentScale,
                        initialManual = currentManual,
                        initialZoom = currentZoom,
                        initialSkipNight = currentSkipNight,
                        initialSkipStart = currentSkipStart,
                        initialSkipEnd = currentSkipEnd,
                        onSave = { url, rate, scale, manual, zoom, skipNight, start, end -> 
                            saveConfig(url, rate, scale, manual, zoom, skipNight, start, end) 
                        }
                    )
                }
            }
        }
    }

    private fun saveConfig(url: String, rate: Int, scale: String, manual: Boolean, zoom: Float, skipNight: Boolean, skipStart: String, skipEnd: String) {
        WidgetState.setUrl(this, appWidgetId, url)
        WidgetState.setRefreshRate(this, appWidgetId, rate)
        WidgetState.setScaleType(this, appWidgetId, scale)
        WidgetState.setManualOnly(this, appWidgetId, manual)
        WidgetState.setZoomFactor(this, appWidgetId, zoom)
        WidgetState.setSkipNight(this, appWidgetId, skipNight)
        WidgetState.setSkipStart(this, appWidgetId, skipStart)
        WidgetState.setSkipEnd(this, appWidgetId, skipEnd)
        
        lifecycleScope.launch {
            val context = applicationContext
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[ImageWidget.StatusKey] = "Starting..."
            }
            ImageWidget().update(context, glanceId)

            if (!manual) {
                val periodicRequest = PeriodicWorkRequestBuilder<ImageRefreshWorker>(rate.toLong(), TimeUnit.MINUTES)
                    .setInputData(workDataOf(ImageRefreshWorker.KEY_WIDGET_ID to appWidgetId))
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork("image_refresh_$appWidgetId", ExistingPeriodicWorkPolicy.UPDATE, periodicRequest)
            } else {
                WorkManager.getInstance(context).cancelUniqueWork("image_refresh_$appWidgetId")
            }

            // ATOMIC TRIGGER: One-time work with UNIQUE policy REPLACE
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

            val resultValue = Intent().apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    initialUrl: String, initialRate: Int, initialScale: String, initialManual: Boolean, initialZoom: Float, 
    initialSkipNight: Boolean, initialSkipStart: String, initialSkipEnd: String,
    onSave: (String, Int, String, Boolean, Float, Boolean, String, String) -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }
    var selectedRate by remember { mutableStateOf(initialRate) }
    var selectedScale by remember { mutableStateOf(initialScale) }
    var selectedZoom by remember { mutableStateOf(initialZoom) }
    var manualOnly by remember { mutableStateOf(initialManual) }
    var skipNight by remember { mutableStateOf(initialSkipNight) }
    var skipStart by remember { mutableStateOf(initialSkipStart) }
    var skipEnd by remember { mutableStateOf(initialSkipEnd) }

    var rateExpanded by remember { mutableStateOf(false) }
    var scaleExpanded by remember { mutableStateOf(false) }
    var zoomExpanded by remember { mutableStateOf(false) }
    
    var showProfileSaveDialog by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    var profiles by remember { mutableStateOf(WidgetState.getProfiles(context)) }
    var profilesExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val rates = listOf(15, 30, 60, 240, 480, 1440)
    val rateLabels = mapOf(15 to "15 Min", 30 to "30 Min", 60 to "1 Hour", 240 to "4 Hours", 480 to "8 Hours", 1440 to "24 Hours")
    val scales = listOf("Crop", "Fit", "Fill")
    val scaleLabels = mapOf("Crop" to "Crop to Fit", "Fit" to "Fit Content", "Fill" to "Stretch")
    val zooms = listOf(1.0f, 1.25f, 1.5f, 2.0f)

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Widget Configuration", style = MaterialTheme.typography.headlineSmall)
                
                if (profiles.isNotEmpty()) {
                    Box {
                        IconButton(
                            onClick = { profilesExpanded = true },
                            modifier = Modifier.size(64.dp) // Increased from default
                        ) {
                            Icon(
                                Icons.Default.ArrowDropDown, 
                                "Profiles",
                                modifier = Modifier.size(48.dp) // 2x bigger than typical 24dp
                            )
                        }
                        DropdownMenu(expanded = profilesExpanded, onDismissRequest = { profilesExpanded = false }) {
                            profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.name) },
                                    onClick = {
                                        url = profile.url; selectedRate = profile.rate; selectedScale = profile.scale
                                        selectedZoom = profile.zoom; manualOnly = profile.manual; skipNight = profile.skipNight
                                        skipStart = profile.skipStart; skipEnd = profile.skipEnd; profilesExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Image URL") }, modifier = Modifier.fillMaxWidth())

            ExposedDropdownMenuBox(expanded = rateExpanded, onExpandedChange = { rateExpanded = !rateExpanded }) {
                OutlinedTextField(value = rateLabels[selectedRate] ?: "$selectedRate Min", onValueChange = {}, readOnly = true, label = { Text("Refresh Rate") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rateExpanded) })
                ExposedDropdownMenu(expanded = rateExpanded, onDismissRequest = { rateExpanded = false }) {
                    rates.forEach { rate -> DropdownMenuItem(text = { Text(rateLabels[rate] ?: "$rate Min") }, onClick = { selectedRate = rate; rateExpanded = false }) }
                }
            }

            ExposedDropdownMenuBox(expanded = scaleExpanded, onExpandedChange = { scaleExpanded = !scaleExpanded }) {
                OutlinedTextField(value = scaleLabels[selectedScale] ?: selectedScale, onValueChange = {}, readOnly = true, label = { Text("Fitting") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scaleExpanded) })
                ExposedDropdownMenu(expanded = scaleExpanded, onDismissRequest = { scaleExpanded = false }) {
                    scales.forEach { s -> DropdownMenuItem(text = { Text(scaleLabels[s] ?: s) }, onClick = { selectedScale = s; scaleExpanded = false }) }
                }
            }

            ExposedDropdownMenuBox(expanded = zoomExpanded, onExpandedChange = { zoomExpanded = !zoomExpanded }) {
                OutlinedTextField(value = "${selectedZoom}x", onValueChange = {}, readOnly = true, label = { Text("Zoom Factor") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoomExpanded) })
                ExposedDropdownMenu(expanded = zoomExpanded, onDismissRequest = { zoomExpanded = false }) {
                    zooms.forEach { z -> DropdownMenuItem(text = { Text("${z}x") }, onClick = { selectedZoom = z; zoomExpanded = false }) }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().clickable { manualOnly = !manualOnly }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Manual Only"); Text("No background updates", style = MaterialTheme.typography.bodySmall) }
                Switch(checked = manualOnly, onCheckedChange = { manualOnly = it })
            }

            Row(modifier = Modifier.fillMaxWidth().clickable { skipNight = !skipNight }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Skip Night"); Text("Pause during hours", style = MaterialTheme.typography.bodySmall) }
                Switch(checked = skipNight, onCheckedChange = { skipNight = it })
            }

            if (skipNight) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TimeBox(label = "Start", time = skipStart, modifier = Modifier.weight(1f), onClick = { showTimePicker(context, skipStart) { skipStart = it } })
                    TimeBox(label = "End", time = skipEnd, modifier = Modifier.weight(1f), onClick = { showTimePicker(context, skipEnd) { skipEnd = it } })
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showProfileSaveDialog = true }, modifier = Modifier.weight(1f)) { Text("Save Profile") }
            Button(onClick = { onSave(url, selectedRate, selectedScale, manualOnly, selectedZoom, skipNight, skipStart, skipEnd) }, modifier = Modifier.weight(1f)) { Text("Save Config") }
        }
    }

    if (showProfileSaveDialog) {
        AlertDialog(
            onDismissRequest = { showProfileSaveDialog = false }, 
            title = { Text("Save Profile") },
            text = { OutlinedTextField(value = profileName, onValueChange = { profileName = it }, label = { Text("Profile Name") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    if (profileName.isNotBlank()) {
                        WidgetState.saveProfile(context, WidgetState.WidgetProfile(profileName, url, selectedRate, selectedScale, selectedZoom, manualOnly, skipNight, skipStart, skipEnd))
                        profiles = WidgetState.getProfiles(context); showProfileSaveDialog = false; profileName = ""
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showProfileSaveDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TimeBox(label: String, time: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedTextField(value = time, onValueChange = {}, readOnly = true, label = { Text(label) }, modifier = modifier.clickable { onClick() }, enabled = false,
        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant))
}

fun showTimePicker(context: Context, currentTime: String, onTimeSelected: (String) -> Unit) {
    val parts = currentTime.split(":"); TimePickerDialog(context, { _, h, m -> onTimeSelected(String.format("%02d:%02d", h, m)) }, parts[0].toInt(), parts[1].toInt(), true).show()
}
