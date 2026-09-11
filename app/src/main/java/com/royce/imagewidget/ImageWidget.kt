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
                val rotate90 = WidgetState.getRotate90(context, appWidgetId)
                val zoom = WidgetState.getZoomFactor(context, appWidgetId)
                val centerX = WidgetState.getZoomCenterX(context, appWidgetId)
                val centerY = WidgetState.getZoomCenterY(context, appWidgetId)
                
                // Binder IPC limit is 1MB. Max safe RGB_565 bitmap size is 707x707 (exactly 1,000,000 bytes)
                decodeZoomedSampledBitmapFromFile(imageFile.absolutePath, 707, 707, rotate90, zoom, centerX, centerY)
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
        // Container for Image and Status (padded so it doesn't overlap the bottom controls)
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(bottom = 26.dp),
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
                    .padding(horizontal = 4.dp, vertical = 0.dp),
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

private fun decodeZoomedSampledBitmapFromFile(path: String, reqWidth: Int, reqHeight: Int, rotate90: Boolean, zoom: Float, centerX: Float, centerY: Float): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        val origW = options.outWidth
        val origH = options.outHeight
        if (origW <= 0 || origH <= 0) return null

        val apparentW = if (rotate90) origH else origW
        val apparentH = if (rotate90) origW else origH

        val zoomActual = if (zoom < 1.0f) 1.0f else zoom
        val cropW = (apparentW / zoomActual).toInt().coerceAtLeast(1)
        val cropH = (apparentH / zoomActual).toInt().coerceAtLeast(1)

        val axLeft = ((apparentW - cropW) * centerX).toInt().coerceIn(0, apparentW - cropW)
        val ayTop = ((apparentH - cropH) * centerY).toInt().coerceIn(0, apparentH - cropH)
        val axRight = axLeft + cropW
        val ayBottom = ayTop + cropH

        val origRect = if (rotate90) {
            android.graphics.Rect(ayTop, origH - axRight, ayBottom, origH - axLeft)
        } else {
            android.graphics.Rect(axLeft, ayTop, axRight, ayBottom)
        }

        val decoder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.graphics.BitmapRegionDecoder.newInstance(path)
        } else {
            @Suppress("DEPRECATION")
            android.graphics.BitmapRegionDecoder.newInstance(path, false)
        }

        if (decoder != null) {
            val decodeOptions = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
                inScaled = false
            }
            val regionW = origRect.width()
            val regionH = origRect.height()
            
            decodeOptions.inSampleSize = calculateInSampleSize(regionW, regionH, reqWidth, reqHeight)
            val decodedRegion = decoder.decodeRegion(origRect, decodeOptions)
            decoder.recycle()
            
            if (decodedRegion != null && rotate90) {
                val matrix = android.graphics.Matrix().apply { postRotate(90f) }
                val rotated = Bitmap.createBitmap(decodedRegion, 0, 0, decodedRegion.width, decodedRegion.height, matrix, true)
                if (rotated != decodedRegion) decodedRegion.recycle()
                return rotated
            }
            return decodedRegion
        } else {
            // Fallback
            options.inSampleSize = calculateInSampleSize(origW, origH, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565
            options.inScaled = false
            var bmp = BitmapFactory.decodeFile(path, options) ?: return null
            
            if (rotate90) {
                val matrix = android.graphics.Matrix().apply { postRotate(90f) }
                val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                if (rotated != bmp) bmp.recycle()
                bmp = rotated
            }
            if (zoomActual > 1.0f) {
                val w = bmp.width
                val h = bmp.height
                val newW = (w / zoomActual).toInt().coerceAtLeast(1)
                val newH = (h / zoomActual).toInt().coerceAtLeast(1)
                val x = ((w - newW) * centerX).toInt().coerceIn(0, w - newW)
                val y = ((h - newH) * centerY).toInt().coerceIn(0, h - newH)
                val cropped = Bitmap.createBitmap(bmp, x, y, newW, newH)
                if (cropped != bmp) bmp.recycle()
                bmp = cropped
            }
            return bmp
        }
    } catch (e: Exception) {
        Log.e("ImageWidget", "Error decoding zoomed bitmap", e)
        try {
            // Fallback for gifs/unsupported formats: just decode it normally without region decoder
            val fallbackOptions = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
                inScaled = false
            }
            val bmp = BitmapFactory.decodeFile(path, fallbackOptions) ?: return null
            val zoomActual = if (zoom < 1.0f) 1.0f else zoom
            var finalBmp = bmp
            if (rotate90) {
                val matrix = android.graphics.Matrix().apply { postRotate(90f) }
                val rotated = Bitmap.createBitmap(finalBmp, 0, 0, finalBmp.width, finalBmp.height, matrix, true)
                if (rotated != finalBmp) finalBmp.recycle()
                finalBmp = rotated
            }
            if (zoomActual > 1.0f) {
                val w = finalBmp.width
                val h = finalBmp.height
                val newW = (w / zoomActual).toInt().coerceAtLeast(1)
                val newH = (h / zoomActual).toInt().coerceAtLeast(1)
                val x = ((w - newW) * centerX).toInt().coerceIn(0, w - newW)
                val y = ((h - newH) * centerY).toInt().coerceIn(0, h - newH)
                val cropped = Bitmap.createBitmap(finalBmp, x, y, newW, newH)
                if (cropped != finalBmp) finalBmp.recycle()
                finalBmp = cropped
            }
            
            // Still need to scale down if it's too big
            val finalW = finalBmp.width
            val finalH = finalBmp.height
            if (finalW > reqWidth || finalH > reqHeight) {
                val scale = minOf(reqWidth.toFloat() / finalW, reqHeight.toFloat() / finalH)
                val scaledW = (finalW * scale).toInt().coerceAtLeast(1)
                val scaledH = (finalH * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(finalBmp, scaledW, scaledH, false)
                if (scaled != finalBmp) finalBmp.recycle()
                finalBmp = scaled
            }
            
            return finalBmp
        } catch (e2: Exception) {
            Log.e("ImageWidget", "Fallback decode failed", e2)
            return null
        }
    }
}

private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
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
