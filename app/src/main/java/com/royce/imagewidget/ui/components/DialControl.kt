package com.royce.imagewidget.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DialControl(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0 // 0 means continuous
) {
    val startAngle = 135f
    val sweepAngle = 270f
    
    // Normalize value to 0..1
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    var center by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val dragPos = change.position
                        
                        // Calculate angle in degrees
                        var angle = Math.toDegrees(atan2((dragPos.y - center.y).toDouble(), (dragPos.x - center.x).toDouble())).toFloat()
                        // atan2 returns -180 to 180, where 0 is at 3 o'clock
                        // Let's normalize so it's continuous
                        if (angle < 0) angle += 360f
                        
                        // Map angle to normalized value
                        // Start angle is 135, sweep is 270. End angle is 135 + 270 = 405 -> 45
                        val valueAngle = if (angle >= 135f) {
                            angle - 135f
                        } else if (angle <= 45f) {
                            angle + 225f
                        } else {
                            // in the "dead zone" (45 to 135), stick to closest edge
                            if (angle > 90f) 0f else 270f
                        }
                        
                        var newNormalizedValue = (valueAngle / sweepAngle).coerceIn(0f, 1f)
                        
                        if (steps > 0) {
                            val stepSize = 1f / steps
                            newNormalizedValue = Math.round(newNormalizedValue / stepSize) * stepSize
                        }
                        
                        val newValue = valueRange.start + newNormalizedValue * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    }
                }
        ) {
            center = Offset(size.width / 2, size.height / 2)
            
            val strokeWidth = size.width * 0.1f
            val radius = (size.width - strokeWidth) / 2
            
            // Draw background track
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )
            
            // Draw filled track
            drawArc(
                color = primaryColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle * normalizedValue,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )
            
            // Draw indicator line (knob marker)
            val currentAngle = startAngle + sweepAngle * normalizedValue
            val currentAngleRad = currentAngle * PI / 180
            val innerRadius = radius - strokeWidth
            val outerRadius = radius + strokeWidth / 2
            
            val lineStartX = center.x + innerRadius * cos(currentAngleRad).toFloat()
            val lineStartY = center.y + innerRadius * sin(currentAngleRad).toFloat()
            val lineEndX = center.x + outerRadius * cos(currentAngleRad).toFloat()
            val lineEndY = center.y + outerRadius * sin(currentAngleRad).toFloat()
            
            drawLine(
                color = onSurfaceColor,
                start = Offset(lineStartX, lineStartY),
                end = Offset(lineEndX, lineEndY),
                strokeWidth = strokeWidth * 0.3f,
                cap = StrokeCap.Round
            )
            
            // Draw tick marks
            if (steps > 0) {
                for (i in 0..steps) {
                    val tickAngle = startAngle + (sweepAngle * i / steps)
                    val tickAngleRad = tickAngle * PI / 180
                    
                    val tickStartR = radius - strokeWidth / 2
                    val tickEndR = radius + strokeWidth / 2
                    
                    val tickStartX = center.x + tickStartR * cos(tickAngleRad).toFloat()
                    val tickStartY = center.y + tickStartR * sin(tickAngleRad).toFloat()
                    val tickEndX = center.x + tickEndR * cos(tickAngleRad).toFloat()
                    val tickEndY = center.y + tickEndR * sin(tickAngleRad).toFloat()
                    
                    // only draw ticks that aren't covered by the main indicator
                    if (Math.abs(currentAngle - tickAngle) > 2f) {
                        drawLine(
                            color = trackColor.copy(alpha = 0.5f),
                            start = Offset(tickStartX, tickStartY),
                            end = Offset(tickEndX, tickEndY),
                            strokeWidth = strokeWidth * 0.1f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
        
        // Inner text showing value
        Text(
            text = String.format(Locale.US, "%.2fx", value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = onSurfaceColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDialControl() {
    var value by remember { mutableStateOf(2.5f) }
    Box(modifier = Modifier.padding(16.dp)) {
        DialControl(
            value = value,
            onValueChange = { value = it },
            valueRange = 1f..5f,
            modifier = Modifier.size(150.dp),
            steps = 16 // 0.25f steps
        )
    }
}
