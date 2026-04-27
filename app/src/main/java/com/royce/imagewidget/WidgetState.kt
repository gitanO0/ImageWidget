package com.royce.imagewidget

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
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
    private const val KEY_REFRESH_RATE = "refresh_rate_"
    private const val KEY_LAST_UPDATED = "last_updated_"
    private const val KEY_STATUS = "status_"
    private const val KEY_MANUAL_ONLY = "manual_only_"
    private const val KEY_SCALE_TYPE = "scale_type_"
    private const val KEY_ZOOM_FACTOR = "zoom_factor_"
    private const val KEY_SKIP_NIGHT = "skip_night_"
    private const val KEY_SKIP_START = "skip_start_"
    private const val KEY_SKIP_END = "skip_end_"

    private const val PROFS_PREFS_NAME = "image_widget_profiles"
    
    val WidgetIdKey = ActionParameters.Key<Int>("app_widget_id")

    private fun getPrefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getProfilePrefs(context: Context) =
        context.applicationContext.getSharedPreferences(PROFS_PREFS_NAME, Context.MODE_PRIVATE)

    data class WidgetProfile(
        val name: String,
        val url: String,
        val rate: Int,
        val scale: String,
        val zoom: Float,
        val manual: Boolean,
        val skipNight: Boolean,
        val skipStart: String,
        val skipEnd: String
    ) {
        fun toJson(): String {
            val json = JSONObject()
            json.put("url", url)
            json.put("rate", rate)
            json.put("scale", scale)
            json.put("zoom", zoom.toDouble())
            json.put("manual", manual)
            json.put("skipNight", skipNight)
            json.put("skipStart", skipStart)
            json.put("skipEnd", skipEnd)
            return json.toString()
        }

        companion object {
            fun fromJson(name: String, jsonStr: String): WidgetProfile {
                val json = JSONObject(jsonStr)
                return WidgetProfile(
                    name = name,
                    url = json.getString("url"),
                    rate = json.getInt("rate"),
                    scale = json.getString("scale"),
                    zoom = json.getDouble("zoom").toFloat(),
                    manual = json.getBoolean("manual"),
                    skipNight = json.optBoolean("skipNight", true),
                    skipStart = json.optString("skipStart", "00:00"),
                    skipEnd = json.optString("skipEnd", "06:00")
                )
            }
        }
    }

    fun saveProfile(context: Context, profile: WidgetProfile) {
        getProfilePrefs(context).edit { putString(profile.name, profile.toJson()) }
    }

    fun getProfiles(context: Context): List<WidgetProfile> {
        val prefs = getProfilePrefs(context)
        return prefs.all.mapNotNull { (name, json) ->
            if (json is String) {
                try {
                    WidgetProfile.fromJson(name, json)
                } catch (_: Exception) {
                    null
                }
            } else null
        }.sortedBy { it.name }
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun getLastUpdatedFormatted(context: Context, widgetId: Int): String {
        val raw = getLastUpdatedRaw(context, widgetId)
        if (raw == "Never") return raw

        return try {
            val instant = Instant.parse(raw)
            val formatter = DateTimeFormatter.ofPattern("HH:mm - M/d")
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        } catch (_: Exception) {
            raw
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

    fun clear(context: Context, widgetId: Int) {
        getPrefs(context).edit {
            remove(KEY_URL + widgetId)
                .remove(KEY_REFRESH_RATE + widgetId)
                .remove(KEY_LAST_UPDATED + widgetId)
                .remove(KEY_STATUS + widgetId)
                .remove(KEY_MANUAL_ONLY + widgetId)
                .remove(KEY_SCALE_TYPE + widgetId)
                .remove(KEY_ZOOM_FACTOR + widgetId)
                .remove(KEY_SKIP_NIGHT + widgetId)
                .remove(KEY_SKIP_START + widgetId)
                .remove(KEY_SKIP_END + widgetId)
        }
        
        imageFile(context, widgetId).delete()
        File(context.applicationContext.filesDir, "latest_$widgetId.tmp").delete()
    }
}
