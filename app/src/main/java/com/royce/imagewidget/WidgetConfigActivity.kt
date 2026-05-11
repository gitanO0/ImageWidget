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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.InputChip
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.royce.imagewidget.ui.theme.ImageWidgetTheme
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
        val currentClickUrl = WidgetState.getClickUrl(this, appWidgetId)
        val currentRate = WidgetState.getRefreshRate(this, appWidgetId)
        val currentScale = WidgetState.getScaleType(this, appWidgetId)
        val currentManual = WidgetState.getManualOnly(this, appWidgetId)
        val currentZoom = WidgetState.getZoomFactor(this, appWidgetId)
        val currentZoomCenterX = WidgetState.getZoomCenterX(this, appWidgetId)
        val currentZoomCenterY = WidgetState.getZoomCenterY(this, appWidgetId)
        val currentSkipNight = WidgetState.getSkipNight(this, appWidgetId)
        val currentSkipStart = WidgetState.getSkipStart(this, appWidgetId)
        val currentSkipEnd = WidgetState.getSkipEnd(this, appWidgetId)
        val currentDiscreteTimes = WidgetState.getDiscreteTimes(this, appWidgetId)

        setContent {
            ImageWidgetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConfigScreen(
                        initialUrl = currentUrl,
                        initialClickUrl = currentClickUrl,
                        initialRate = currentRate,
                        initialScale = currentScale,
                        initialManual = currentManual,
                        initialZoom = currentZoom,
                        initialZoomCenterX = currentZoomCenterX,
                        initialZoomCenterY = currentZoomCenterY,
                        initialSkipNight = currentSkipNight,
                        initialSkipStart = currentSkipStart,
                        initialSkipEnd = currentSkipEnd,
                        initialDiscreteTimes = currentDiscreteTimes
                    ) { url, clickUrl, rate, scale, manual, zoom, zoomCenterX, zoomCenterY, skipNight, start, end, discreteTimes -> 
                        saveConfig(url, clickUrl, rate, scale, manual, zoom, zoomCenterX, zoomCenterY, skipNight, start, end, discreteTimes)
                    }
                }
            }
        }
    }

    private fun saveConfig(url: String, clickUrl: String, rate: Int, scale: String, manual: Boolean, zoom: Float, zoomCenterX: Float, zoomCenterY: Float, skipNight: Boolean, skipStart: String, skipEnd: String, discreteTimes: String) {
        WidgetState.setUrl(this, appWidgetId, url)
        WidgetState.setClickUrl(this, appWidgetId, clickUrl)
        WidgetState.setRefreshRate(this, appWidgetId, rate)
        WidgetState.setScaleType(this, appWidgetId, scale)
        WidgetState.setManualOnly(this, appWidgetId, manual)
        WidgetState.setZoomFactor(this, appWidgetId, zoom)
        WidgetState.setZoomCenterX(this, appWidgetId, zoomCenterX)
        WidgetState.setZoomCenterY(this, appWidgetId, zoomCenterY)
        WidgetState.setSkipNight(this, appWidgetId, skipNight)
        WidgetState.setSkipStart(this, appWidgetId, skipStart)
        WidgetState.setSkipEnd(this, appWidgetId, skipEnd)
        WidgetState.setDiscreteTimes(this, appWidgetId, discreteTimes)
        
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

            ImageRefreshWorker.scheduleDiscreteRefreshes(context, appWidgetId, discreteTimes)

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
    initialUrl: String, initialClickUrl: String, initialRate: Int, initialScale: String, initialManual: Boolean, initialZoom: Float, 
    initialZoomCenterX: Float, initialZoomCenterY: Float,
    initialSkipNight: Boolean, initialSkipStart: String, initialSkipEnd: String, initialDiscreteTimes: String,
    onSave: (String, String, Int, String, Boolean, Float, Float, Float, Boolean, String, String, String) -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }
    var clickUrl by remember { mutableStateOf(initialClickUrl) }
    var manualOnly by remember { mutableStateOf(initialManual) }
    var previousRate by remember { mutableIntStateOf(if (initialRate > 0) initialRate else 15) }
    var selectedRate by remember { mutableIntStateOf(if (initialManual) -1 else initialRate) }
    var selectedScale by remember { mutableStateOf(initialScale) }
    var selectedZoom by remember { mutableFloatStateOf(initialZoom) }
    var zoomCenterX by remember { mutableFloatStateOf(initialZoomCenterX) }
    var zoomCenterY by remember { mutableFloatStateOf(initialZoomCenterY) }
    var skipNight by remember { mutableStateOf(initialSkipNight) }
    var skipStart by remember { mutableStateOf(initialSkipStart) }
    var skipEnd by remember { mutableStateOf(initialSkipEnd) }
    var discreteTimes by remember { mutableStateOf(initialDiscreteTimes.split(",").filter { it.isNotBlank() }) }

    var rateExpanded by remember { mutableStateOf(false) }
    var scaleExpanded by remember { mutableStateOf(false) }
    val showProfileSaveDialog = remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    var profiles by remember { mutableStateOf(WidgetState.getProfiles(context)) }
    var profilesExpanded by remember { mutableStateOf(false) }
    var currentLoadedProfile by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            try {
                val json = WidgetState.exportProfiles(context)
                context.contentResolver.openOutputStream(it)?.use { out ->
                    out.write(json.toByteArray())
                }
                Toast.makeText(context, "Profiles exported", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Failed to export profiles", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                val jsonStr = context.contentResolver.openInputStream(it)?.bufferedReader().use { reader -> reader?.readText() } ?: ""
                val count = WidgetState.importProfiles(context, jsonStr)
                profiles = WidgetState.getProfiles(context)
                Toast.makeText(context, "Imported $count profiles", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Failed to import", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val rates = listOf(-1, 15, 30, 60, 240, 480, 1440)
    val rateLabels = mapOf(-1 to "None", 15 to "15 Min", 30 to "30 Min", 60 to "1 Hour", 240 to "4 Hours", 480 to "8 Hours", 1440 to "24 Hours")
    val scales = listOf("Crop", "Fit", "Fill")
    val scaleLabels = mapOf("Crop" to "Crop to Fit", "Fit" to "Fit Content", "Fill" to "Stretch")

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Widget Configuration", style = MaterialTheme.typography.headlineSmall)
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    Text("v${packageInfo.versionName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
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
                        if (profiles.isNotEmpty()) {
                            profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.name) },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            WidgetState.deleteProfile(context, profile.name)
                                            profiles = WidgetState.getProfiles(context)
                                            if (currentLoadedProfile == profile.name) currentLoadedProfile = ""
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Profile")
                                        }
                                    },
                                    onClick = {
                                        url = profile.url; clickUrl = profile.clickUrl
                                        manualOnly = profile.manual
                                        if (profile.manual) {
                                            if (profile.rate > 0) previousRate = profile.rate
                                            selectedRate = -1
                                        } else {
                                            selectedRate = profile.rate
                                            previousRate = profile.rate
                                        }
                                        selectedScale = profile.scale
                                        selectedZoom = profile.zoom; zoomCenterX = profile.zoomCenterX; zoomCenterY = profile.zoomCenterY
                                        skipNight = profile.skipNight
                                        skipStart = profile.skipStart; skipEnd = profile.skipEnd
                                        discreteTimes = profile.discreteTimes.split(",").filter { it.isNotBlank() }
                                        profilesExpanded = false
                                        currentLoadedProfile = profile.name
                                    }
                                )
                            }
                            HorizontalDivider()
                        }
                        DropdownMenuItem(
                            text = { Text("Import Profiles") },
                            onClick = { 
                                profilesExpanded = false
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Profiles") },
                            onClick = { 
                                profilesExpanded = false
                                exportLauncher.launch("imagewidget_profiles.json")
                            }
                        )
                    }
                }
            }

            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Image URL") }, modifier = Modifier.fillMaxWidth())
            
            OutlinedTextField(
                value = clickUrl, 
                onValueChange = { clickUrl = it }, 
                label = { Text("On-Click URL (Optional)") }, 
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Leave blank to open Image URL") }
            )

            ExposedDropdownMenuBox(expanded = rateExpanded && !manualOnly, onExpandedChange = { if (!manualOnly) rateExpanded = !rateExpanded }) {
                OutlinedTextField(
                    value = rateLabels[selectedRate] ?: "$selectedRate Min", 
                    onValueChange = {}, 
                    readOnly = true, 
                    label = { Text("Auto Refresh Rate") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = !manualOnly), 
                    enabled = !manualOnly,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rateExpanded && !manualOnly) }
                )
                ExposedDropdownMenu(expanded = rateExpanded && !manualOnly, onDismissRequest = { rateExpanded = false }) {
                    rates.forEach { rate -> 
                        DropdownMenuItem(
                            text = { Text(rateLabels[rate] ?: "$rate Min") }, 
                            onClick = { 
                                selectedRate = rate
                                if (rate == -1) {
                                    manualOnly = true
                                } else {
                                    manualOnly = false
                                    previousRate = rate
                                }
                                rateExpanded = false 
                            }
                        ) 
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().clickable { 
                manualOnly = !manualOnly
                if (manualOnly) {
                    if (selectedRate > 0) previousRate = selectedRate
                    selectedRate = -1
                } else {
                    selectedRate = previousRate
                }
            }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Manual Refresh Only"); Text("No background updates", style = MaterialTheme.typography.bodySmall) }
                Switch(checked = manualOnly, onCheckedChange = { 
                    manualOnly = it
                    if (it) {
                        if (selectedRate > 0) previousRate = selectedRate
                        selectedRate = -1
                    } else {
                        selectedRate = previousRate
                    }
                })
            }

            val skipNightEnabled = !manualOnly
            Row(modifier = Modifier.fillMaxWidth().clickable(enabled = skipNightEnabled) { skipNight = !skipNight }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.alpha(if (skipNightEnabled) 1f else 0.5f)) { Text("Skip automated Night Refresh"); Text("Pause during hours", style = MaterialTheme.typography.bodySmall) }
                Switch(checked = skipNight, onCheckedChange = { skipNight = it }, enabled = skipNightEnabled)
            }

            if (skipNight && skipNightEnabled) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TimeBox(label = "Start", time = skipStart, modifier = Modifier.weight(1f), onClick = { showTimePicker(context, skipStart) { skipStart = it } })
                    TimeBox(label = "End", time = skipEnd, modifier = Modifier.weight(1f), onClick = { showTimePicker(context, skipEnd) { skipEnd = it } })
                }
            }

            HorizontalDivider()
            
            Column {
                Text("Discrete Refresh Times", style = MaterialTheme.typography.titleMedium)
                Text("Override rules to refresh at exact times.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Note: Android battery optimizations may delay background refreshes by 15+ minutes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(discreteTimes) { time ->
                        androidx.compose.material3.InputChip(
                            selected = false,
                            onClick = { /* do nothing on click, just allow delete */ },
                            label = { Text(time) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { discreteTimes = discreteTimes.filter { it != time } },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove")
                                }
                            }
                        )
                    }
                }
                
                OutlinedButton(
                    onClick = {
                        showTimePicker(context, "12:00") { newTime ->
                            if (newTime !in discreteTimes) {
                                discreteTimes = (discreteTimes + listOf(newTime)).sorted()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Time")
                    Spacer(Modifier.width(8.dp))
                    Text("Add Exact Refresh Time")
                }
            }

            ExposedDropdownMenuBox(expanded = scaleExpanded, onExpandedChange = { scaleExpanded = !scaleExpanded }) {
                OutlinedTextField(value = scaleLabels[selectedScale] ?: selectedScale, onValueChange = {}, readOnly = true, label = { Text("Fitting") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scaleExpanded) })
                ExposedDropdownMenu(expanded = scaleExpanded, onDismissRequest = { scaleExpanded = false }) {
                    scales.forEach { s -> DropdownMenuItem(text = { Text(scaleLabels[s] ?: s) }, onClick = { selectedScale = s; scaleExpanded = false }) }
                }
            }

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Zoom Factor", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                com.royce.imagewidget.ui.components.DialControl(
                    value = selectedZoom,
                    onValueChange = { selectedZoom = it },
                    valueRange = 1f..5f,
                    modifier = Modifier.size(150.dp),
                    steps = 16
                )
            }
            
            if (selectedZoom > 1.0f) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Zoom Focus Center", style = MaterialTheme.typography.labelLarge)
                    
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("X:", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = zoomCenterX,
                            onValueChange = { zoomCenterX = it },
                            modifier = Modifier.weight(1f)
                        )
                        BasicTextField(
                            value = (zoomCenterX * 100).toInt().toString(),
                            onValueChange = { newVal ->
                                val parsed = newVal.toIntOrNull()
                                if (parsed != null && (parsed in 0..100)) {
                                    zoomCenterX = parsed / 100f
                                }
                            },
                            modifier = Modifier.width(40.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text("%", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Y:", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = zoomCenterY,
                            onValueChange = { zoomCenterY = it },
                            modifier = Modifier.weight(1f)
                        )
                        BasicTextField(
                            value = (zoomCenterY * 100).toInt().toString(),
                            onValueChange = { newVal ->
                                val parsed = newVal.toIntOrNull()
                                if (parsed != null && (parsed in 0..100)) {
                                    zoomCenterY = parsed / 100f
                                }
                            },
                            modifier = Modifier.width(40.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text("%", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { 
                    profileName = currentLoadedProfile
                    showProfileSaveDialog.value = true 
                }, 
                modifier = Modifier.weight(1f)
            ) { Text("Save Profile") }
            Button(onClick = { onSave(url, clickUrl, selectedRate, selectedScale, manualOnly, selectedZoom, zoomCenterX, zoomCenterY, skipNight, skipStart, skipEnd, discreteTimes.joinToString(",")) }, modifier = Modifier.weight(1f)) { Text("Save Config") }
        }
    }

    if (showProfileSaveDialog.value) {
        var profileDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showProfileSaveDialog.value = false }, 
            title = { Text("Save Profile") },
            text = {
                ExposedDropdownMenuBox(
                    expanded = profileDropdownExpanded,
                    onExpandedChange = { profileDropdownExpanded = !profileDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = profileDropdownExpanded) }
                    )
                    if (profiles.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = profileDropdownExpanded,
                            onDismissRequest = { profileDropdownExpanded = false }
                        ) {
                            profiles.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name) },
                                    onClick = {
                                        profileName = p.name
                                        profileDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (profileName.isNotBlank()) {
                        val isSaved = WidgetState.saveProfile(context, WidgetState.WidgetProfile(profileName, url, clickUrl, selectedRate, selectedScale, selectedZoom, zoomCenterX, zoomCenterY, manualOnly, skipNight, skipStart, skipEnd, discreteTimes.joinToString(",")))
                        if (isSaved) {
                            Toast.makeText(context, "Profile '$profileName' saved successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to save profile", Toast.LENGTH_SHORT).show()
                        }
                        profiles = WidgetState.getProfiles(context)
                        currentLoadedProfile = profileName
                        showProfileSaveDialog.value = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showProfileSaveDialog.value = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TimeBox(label: String, time: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedTextField(value = time, onValueChange = {}, readOnly = true, label = { Text(label) }, modifier = modifier.clickable { onClick() }, enabled = false,
        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant))
}

fun showTimePicker(context: Context, currentTime: String, onTimeSelected: (String) -> Unit) {
    val parts = currentTime.split(":")
    TimePickerDialog(context, { _, h, m -> 
        onTimeSelected(String.format(java.util.Locale.US, "%02d:%02d", h, m)) 
    }, parts[0].toInt(), parts[1].toInt(), true).show()
}
