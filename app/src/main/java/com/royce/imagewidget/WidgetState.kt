package com.royce.imagewidget

import android.content.Context
import android.os.Build
import androidx.glance.action.ActionParameters
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.core.content.edit

object WidgetState {
    private const val PREFS_NAME = "image_widget_prefs"

    private const val KEY_URL = "url_"
    private const val KEY_CLICK_URL = "click_url_"
    private const val KEY_REFRESH_RATE = "refresh_rate_"
    private const val KEY_LAST_UPDATED = "last_updated_"
    private const val KEY_STATUS = "status_"
    private const val KEY_MANUAL_ONLY = "manual_only_"
    private const val KEY_SCALE_TYPE = "scale_type_"
    private const val KEY_ZOOM_FACTOR = "zoom_factor_"
    private const val KEY_ZOOM_CENTER_X = "zoom_center_x_"
    private const val KEY_ZOOM_CENTER_Y = "zoom_center_y_"
    private const val KEY_SKIP_NIGHT = "skip_night_"
    private const val KEY_SKIP_START = "skip_start_"
    private const val KEY_SKIP_END = "skip_end_"
    private const val KEY_DISCRETE_TIMES = "discrete_times_"

    private const val PROFS_PREFS_NAME = "image_widget_profiles"
    
    val WidgetIdKey = ActionParameters.Key<Int>("app_widget_id")

    private fun getPrefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getProfilePrefs(context: Context) =
        context.applicationContext.getSharedPreferences(PROFS_PREFS_NAME, Context.MODE_PRIVATE)

    data class WidgetProfile(
        val name: String,
        val url: String,
        val clickUrl: String = "",
        val rate: Int,
        val scale: String,
        val zoom: Float,
        val zoomCenterX: Float = 0.5f,
        val zoomCenterY: Float = 0.5f,
        val manual: Boolean,
        val skipNight: Boolean,
        val skipStart: String,
        val skipEnd: String,
        val discreteTimes: String = ""
    ) {
        fun toJson(): String {
            val json = JSONObject()
            json.put("url", url)
            json.put("clickUrl", clickUrl)
            json.put("rate", rate)
            json.put("scale", scale)
            json.put("zoom", zoom.toDouble())
            json.put("zoomCenterX", zoomCenterX.toDouble())
            json.put("zoomCenterY", zoomCenterY.toDouble())
            json.put("manual", manual)
            json.put("skipNight", skipNight)
            json.put("skipStart", skipStart)
            json.put("skipEnd", skipEnd)
            json.put("discreteTimes", discreteTimes)
            return json.toString()
        }

        companion object {
            fun fromJson(name: String, jsonStr: String): WidgetProfile {
                val json = JSONObject(jsonStr)
                return WidgetProfile(
                    name = name,
                    url = json.getString("url"),
                    clickUrl = json.optString("clickUrl", ""),
                    rate = json.getInt("rate"),
                    scale = json.getString("scale"),
                    zoom = json.getDouble("zoom").toFloat(),
                    zoomCenterX = json.optDouble("zoomCenterX", 0.5).toFloat(),
                    zoomCenterY = json.optDouble("zoomCenterY", 0.5).toFloat(),
                    manual = json.getBoolean("manual"),
                    skipNight = json.optBoolean("skipNight", true),
                    skipStart = json.optString("skipStart", "00:00"),
                    skipEnd = json.optString("skipEnd", "06:00"),
                    discreteTimes = json.optString("discreteTimes", "")
                )
            }
        }
    }

    @android.annotation.SuppressLint("ApplySharedPref")
    fun saveProfile(context: Context, profile: WidgetProfile): Boolean {
        return getProfilePrefs(context).edit().putString(profile.name, profile.toJson()).commit()
    }

    @android.annotation.SuppressLint("ApplySharedPref")
    fun deleteProfile(context: Context, name: String): Boolean {
        return getProfilePrefs(context).edit().remove(name).commit()
    }

    fun getProfiles(context: Context): List<WidgetProfile> {
        val prefs = getProfilePrefs(context)
        return prefs.all.asSequence().mapNotNull { (name, json) ->
            if (json is String) {
                try {
                    WidgetProfile.fromJson(name, json)
                } catch (_: Exception) {
                    null
                }
            } else null
        }.toList().sortedBy { it.name }
    }

    fun exportProfiles(context: Context): String {
        val profiles = getProfiles(context)
        val exportJson = JSONObject()
        profiles.forEach { profile ->
            exportJson.put(profile.name, JSONObject(profile.toJson()))
        }
        return exportJson.toString(2)
    }

    fun importProfiles(context: Context, jsonString: String): Int {
        var count = 0
        try {
            val prefs = getProfilePrefs(context)
            val root = JSONObject(jsonString)
            val keys = root.keys()
            while (keys.hasNext()) {
                val name = keys.next()
                if (!prefs.contains(name)) {
                    val profileJson = root.getJSONObject(name)
                    val profile = WidgetProfile.fromJson(name, profileJson.toString())
                    if (saveProfile(context, profile)) {
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }

    fun imageFile(context: Context, widgetId: Int): File {
        return File(context.applicationContext.filesDir, "latest_$widgetId.png")
    }

    fun setUrl(context: Context, widgetId: Int, url: String) {
        getPrefs(context).edit { putString(KEY_URL + widgetId, url) }
    }

    fun getUrl(context: Context, widgetId: Int): String {
        return getPrefs(context).getString(KEY_URL + widgetId, ImageRefreshWorker.IMAGE_URL) 
            ?: ImageRefreshWorker.IMAGE_URL
    }

    fun setClickUrl(context: Context, widgetId: Int, clickUrl: String) {
        getPrefs(context).edit { putString(KEY_CLICK_URL + widgetId, clickUrl) }
    }

    fun getClickUrl(context: Context, widgetId: Int): String {
        return getPrefs(context).getString(KEY_CLICK_URL + widgetId, "") ?: ""
    }

    fun setRefreshRate(context: Context, widgetId: Int, minutes: Int) {
        getPrefs(context).edit { putInt(KEY_REFRESH_RATE + widgetId, minutes) }
    }

    fun getRefreshRate(context: Context, widgetId: Int): Int {
        return getPrefs(context).getInt(KEY_REFRESH_RATE + widgetId, 15)
    }

    fun setLastUpdated(context: Context, widgetId: Int, value: String) {
        getPrefs(context).edit { putString(KEY_LAST_UPDATED + widgetId, value) }
    }

    fun getLastUpdatedRaw(context: Context, widgetId: Int): String {
        return getPrefs(context).getString(KEY_LAST_UPDATED + widgetId, "Never") ?: "Never"
    }

    fun getLastUpdatedFormatted(context: Context, widgetId: Int): String {
        val raw = getLastUpdatedRaw(context, widgetId)
        if (raw == "Never") return raw

        return try {
            val timeMillis = raw.toLong()
            val formatter = java.text.SimpleDateFormat("HH:mm - M/d", java.util.Locale.getDefault())
            formatter.format(java.util.Date(timeMillis))
        } catch (_: Exception) {
            // Fallback for old ISO-8601 strings if they still exist from previous versions
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val instant = Instant.parse(raw)
                    val formatter = DateTimeFormatter.ofPattern("HH:mm - M/d")
                        .withZone(ZoneId.systemDefault())
                    formatter.format(instant)
                } else {
                    raw
                }
            } catch (_: Exception) {
                raw
            }
        }
    }

    fun setManualOnly(context: Context, widgetId: Int, value: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_MANUAL_ONLY + widgetId, value) }
    }

    fun getManualOnly(context: Context, widgetId: Int): Boolean {
        return getPrefs(context).getBoolean(KEY_MANUAL_ONLY + widgetId, false)
    }

    fun setScaleType(context: Context, widgetId: Int, value: String) {
        getPrefs(context).edit { putString(KEY_SCALE_TYPE + widgetId, value) }
    }

    fun getScaleType(context: Context, widgetId: Int): String {
        return getPrefs(context).getString(KEY_SCALE_TYPE + widgetId, "Crop") ?: "Crop"
    }

    fun setZoomFactor(context: Context, widgetId: Int, value: Float) {
        getPrefs(context).edit { putFloat(KEY_ZOOM_FACTOR + widgetId, value) }
    }

    fun getZoomFactor(context: Context, widgetId: Int): Float {
        return getPrefs(context).getFloat(KEY_ZOOM_FACTOR + widgetId, 1.0f)
    }

    fun setZoomCenterX(context: Context, widgetId: Int, value: Float) {
        getPrefs(context).edit { putFloat(KEY_ZOOM_CENTER_X + widgetId, value) }
    }

    fun getZoomCenterX(context: Context, widgetId: Int): Float {
        return getPrefs(context).getFloat(KEY_ZOOM_CENTER_X + widgetId, 0.5f)
    }

    fun setZoomCenterY(context: Context, widgetId: Int, value: Float) {
        getPrefs(context).edit { putFloat(KEY_ZOOM_CENTER_Y + widgetId, value) }
    }

    fun getZoomCenterY(context: Context, widgetId: Int): Float {
        return getPrefs(context).getFloat(KEY_ZOOM_CENTER_Y + widgetId, 0.5f)
    }

    fun setSkipNight(context: Context, widgetId: Int, value: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_SKIP_NIGHT + widgetId, value) }
    }

    fun getSkipNight(context: Context, widgetId: Int): Boolean {
        return getPrefs(context).getBoolean(KEY_SKIP_NIGHT + widgetId, true)
    }

    fun setSkipStart(context: Context, widgetId: Int, value: String) {
        getPrefs(context).edit { putString(KEY_SKIP_START + widgetId, value) }
    }

    fun getSkipStart(context: Context, widgetId: Int): String {
        return getPrefs(context).getString(KEY_SKIP_START + widgetId, "00:00") ?: "00:00"
    }

    fun setSkipEnd(context: Context, widgetId: Int, value: String) {
        getPrefs(context).edit { putString(KEY_SKIP_END + widgetId, value) }
    }

    fun getSkipEnd(context: Context, widgetId: Int): String {
        return getPrefs(context).getString(KEY_SKIP_END + widgetId, "06:00") ?: "06:00"
    }

    fun setDiscreteTimes(context: Context, widgetId: Int, times: String) {
        getPrefs(context).edit { putString(KEY_DISCRETE_TIMES + widgetId, times) }
    }

    fun getDiscreteTimes(context: Context, widgetId: Int): String {
        return getPrefs(context).getString(KEY_DISCRETE_TIMES + widgetId, "") ?: ""
    }

    fun getNextDiscreteTime(context: Context, widgetId: Int): String? {
        val timesString = getDiscreteTimes(context, widgetId)
        if (timesString.isBlank()) return null
        
        val times = timesString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (times.isEmpty()) return null

        val now = java.util.Calendar.getInstance()
        var nextTimeStr: String? = null
        var minDelay = Long.MAX_VALUE

        for (time in times) {
            val parts = time.split(":")
            if (parts.size != 2) continue
            val hour = parts[0].toIntOrNull() ?: continue
            val minute = parts[1].toIntOrNull() ?: continue

            val targetTime = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            if (targetTime.before(now) || targetTime.timeInMillis == now.timeInMillis) {
                targetTime.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }

            val delay = targetTime.timeInMillis - now.timeInMillis
            if (delay < minDelay) {
                minDelay = delay
                nextTimeStr = time
            }
        }
        return nextTimeStr
    }

    fun clear(context: Context, widgetId: Int) {
        getPrefs(context).edit {
            remove(KEY_URL + widgetId)
                .remove(KEY_CLICK_URL + widgetId)
                .remove(KEY_REFRESH_RATE + widgetId)
                .remove(KEY_LAST_UPDATED + widgetId)
                .remove(KEY_STATUS + widgetId)
                .remove(KEY_MANUAL_ONLY + widgetId)
                .remove(KEY_SCALE_TYPE + widgetId)
                .remove(KEY_ZOOM_FACTOR + widgetId)
                .remove(KEY_ZOOM_CENTER_X + widgetId)
                .remove(KEY_ZOOM_CENTER_Y + widgetId)
                .remove(KEY_SKIP_NIGHT + widgetId)
                .remove(KEY_SKIP_START + widgetId)
                .remove(KEY_SKIP_END + widgetId)
                .remove(KEY_DISCRETE_TIMES + widgetId)
        }
        
        imageFile(context, widgetId).delete()
        File(context.applicationContext.filesDir, "latest_$widgetId.tmp").delete()
    }
}
