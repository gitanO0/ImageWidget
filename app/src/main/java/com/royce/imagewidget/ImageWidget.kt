package com.royce.imagewidget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class ImageWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: androidx.glance.appwidget.SizeMode = androidx.glance.appwidget.SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        
        provideContent {
            val prefs = currentState<Preferences>()
            val status = prefs[StatusKey] ?: "OK"
            val imageFile = WidgetState.imageFile(context, appWidgetId)
            
            // PRE-DECODE BITMAP ON BACKGROUND THREAD
            // Use 400x400 to stay safely under the 1MB IPC limit
            val bitmap = if (imageFile.exists() && (imageFile.length() > 0)) {
                val decoded = decodeSampledBitmapFromFile(imageFile.absolutePath, 400, 400)
                val zoom = WidgetState.getZoomFactor(context, appWidgetId)
                if (decoded != null && zoom > 1.0f) {
                    try {
                        val w = decoded.width
                        val h = decoded.height
                        val newW = (w / zoom).toInt()
                        val newH = (h / zoom).toInt()
                        
                        val centerX = WidgetState.getZoomCenterX(context, appWidgetId)
                        val centerY = WidgetState.getZoomCenterY(context, appWidgetId)
                        
                        val x = ((w - newW) * centerX).toInt().coerceIn(0, w - newW)
                        val y = ((h - newH) * centerY).toInt().coerceIn(0, h - newH)

                        val cropped = Bitmap.createBitmap(decoded, x, y, newW, newH)
                        if (cropped != decoded) decoded.recycle()
                        cropped
                    } catch (_: Exception) { decoded }
                } else decoded
            } else null

            ImageWidgetContent(context, appWidgetId, status, bitmap)
        }
    }

    companion object {
        val StatusKey = stringPreferencesKey("widget_status")
    }
}

@Composable
@SuppressLint("RestrictedApi")
private fun ImageWidgetContent(context: Context, appWidgetId: Int, status: String, bitmap: Bitmap?) {
    val lastUpdated = WidgetState.getLastUpdatedFormatted(context, appWidgetId)
    val manualOnly = WidgetState.getManualOnly(context, appWidgetId)
    val scaleType = WidgetState.getScaleType(context, appWidgetId)
    val nextRefreshTime = WidgetState.getUnifiedNextRefreshTime(context, appWidgetId)
    
    Log.d("ImageWidget", "[RENDER] ID: $appWidgetId, Status: $status, HasBitmap: ${bitmap != null}")

    val contentScale = when (scaleType) {
        "Fit" -> ContentScale.Fit
        "Fill" -> ContentScale.FillBounds
        else -> ContentScale.Crop
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.Black))
            .clickable(
                actionStartActivity<OpenImageActivity>(
                    actionParametersOf(WidgetState.WidgetIdKey to appWidgetId)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 1. The Image (Background)
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "Latest image",
                contentScale = contentScale,
                modifier = GlanceModifier
                    .fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = GlanceModifier.padding(16.dp)
            ) {
                Text(
                    "Image Widget",
                    style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (status != "OK") status else "Waiting for image...",
                    style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 12.sp)
                )
            }
        }

        // 2. Status overlay (Top Right)
        if (status != "OK") {
            Box(
                modifier = GlanceModifier.fillMaxSize().padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = status,
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 10.sp),
                    modifier = GlanceModifier.background(ColorProvider(Color(0x80000000))).padding(4.dp)
                )
            }
        }

        // 3. Controls Overlay (Bottom)
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(bottom = 4.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = GlanceModifier
                    .background(ColorProvider(Color(0x66000000)))
                    .cornerRadius(8.dp)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val isProcessing = status == "Starting..." || status == "Refreshing..." || status == "Downloading..."
                val textToShow = if (status == "OK") "Refresh" else status

                Text(
                    text = textToShow,
                    style = TextStyle(
                        color = if (isProcessing) ColorProvider(Color.LightGray) else ColorProvider(Color.White),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.then(if (!isProcessing) {
                        GlanceModifier.clickable(actionRunCallback<RefreshWidgetAction>(
                            actionParametersOf(WidgetState.WidgetIdKey to appWidgetId)
                        ))
                    } else GlanceModifier)
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                Text(
                    text = lastUpdated,
                    style = TextStyle(
                        color = ColorProvider(Color.White), 
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (nextRefreshTime != null) {
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "⏳ $nextRefreshTime",
                        style = TextStyle(
                            color = ColorProvider(Color.Yellow), 
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                if (manualOnly) {
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Box(modifier = GlanceModifier.size(6.dp).background(ColorProvider(Color.Red)).cornerRadius(3.dp)) {}
                }
            }
        }
    }
}

@Suppress("SameParameterValue")
private fun decodeSampledBitmapFromFile(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        BitmapFactory.decodeFile(path, options)
    } catch (e: Exception) {
        Log.e("ImageWidget", "Error decoding bitmap", e)
        null
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
