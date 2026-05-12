package com.velcuri.bassride.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

val BAND_LABELS = arrayOf(
    "32Hz", "64Hz", "125Hz", "250Hz", "500Hz",
    "1kHz",  "2kHz",  "4kHz",  "8kHz", "16kHz"
)

private val TrackBg    = Color(0xFF242538)
private val SliderCyan = Color(0xFF06B6D4)
private val SliderPurp = Color(0xFF8B5CF6)
private val MutedLabel = Color(0xFF64748B)

internal const val LABEL_HEIGHT_DP = 20

/**
 * Premium vertical EQ band slider drawn entirely with Canvas.
 * Drag up/down to change the band level. Shows a bidirectional fill from 0 dB
 * toward the current level in cyan (positive) or purple (negative).
 */
@Composable
fun BandSlider(
    bandIndex: Int,
    levelMillibels: Int,
    minMillibels: Int,
    maxMillibels: Int,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val label      = BAND_LABELS.getOrElse(bandIndex) { "${bandIndex + 1}" }
    val dbValue    = levelMillibels / 100f
    val dbDisplay  = when {
        dbValue > 0f -> "+${dbValue.toInt()}"
        dbValue < 0f -> "${dbValue.toInt()}"
        else         -> "0"
    }
    val levelColor = if (levelMillibels >= 0) SliderCyan else SliderPurp
    val range      = (maxMillibels - minMillibels).toFloat()

    Column(
        modifier = modifier.fillMaxHeight().widthIn(min = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dbDisplay,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = levelColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(LABEL_HEIGHT_DP.dp)
        )

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val heightPx = constraints.maxHeight.toFloat()
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(minMillibels, maxMillibels, heightPx) {
                        if (heightPx == 0f) return@pointerInput
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            val delta = (-dragAmount / heightPx * range).roundToInt()
                            val new   = (levelMillibels + delta).coerceIn(minMillibels, maxMillibels)
                            if (new != levelMillibels) onLevelChange(new)
                        }
                    }
                    .semantics { contentDescription = "$label band, $dbDisplay dB" }
            ) {
                val cx           = size.width / 2f
                val fraction     = 1f - (levelMillibels - minMillibels) / range
                val zeroFraction = (1f - (0f - minMillibels) / range).coerceIn(0f, 1f)
                val thumbY       = size.height * fraction
                val zeroY        = size.height * zeroFraction

                drawLine(
                    color = TrackBg, start = Offset(cx, 0f), end = Offset(cx, size.height),
                    strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round
                )

                val fillTop    = minOf(thumbY, zeroY)
                val fillBottom = maxOf(thumbY, zeroY)
                if (fillBottom - fillTop > 1f) {
                    val colors = if (levelMillibels >= 0)
                        listOf(levelColor, levelColor.copy(alpha = 0.5f))
                    else
                        listOf(levelColor.copy(alpha = 0.5f), levelColor)
                    drawLine(
                        brush = Brush.verticalGradient(colors, fillTop, fillBottom),
                        start = Offset(cx, fillTop), end = Offset(cx, fillBottom),
                        strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round
                    )
                }

                drawLine(
                    color = Color(0xFF3A3C55),
                    start = Offset(cx - 8.dp.toPx(), zeroY),
                    end   = Offset(cx + 8.dp.toPx(), zeroY),
                    strokeWidth = 1.5f
                )

                drawCircle(color = Color(0xFF1E2038), radius = 12.dp.toPx(), center = Offset(cx, thumbY))
                drawCircle(color = Color.White,       radius = 9.dp.toPx(),  center = Offset(cx, thumbY))
                drawCircle(color = levelColor,        radius = 4.dp.toPx(),  center = Offset(cx, thumbY))
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MutedLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(LABEL_HEIGHT_DP.dp)
        )
    }
}

/**
 * Vertical dB ruler that aligns with [BandSlider] track sections.
 * Pass the same min/max millibels as the sliders use.
 */
@Composable
fun DbRuler(
    minMillibels: Int,
    maxMillibels: Int,
    modifier: Modifier = Modifier
) {
    val range = (maxMillibels - minMillibels).toFloat()
    Column(modifier = modifier.fillMaxHeight().widthIn(max = 28.dp)) {
        Spacer(Modifier.height(LABEL_HEIGHT_DP.dp))
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val totalHeight = maxHeight
            listOf(12, 6, 0, -6, -12).forEach { db ->
                val mb = db * 100
                if (mb in minMillibels..maxMillibels) {
                    val fraction = 1f - (mb - minMillibels) / range
                    val yOffset  = totalHeight * fraction - 6.dp
                    Text(
                        text = if (db > 0) "+$db" else "$db",
                        modifier = Modifier.fillMaxWidth().absoluteOffset(y = yOffset),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MutedLabel
                    )
                }
            }
        }
        Spacer(Modifier.height(LABEL_HEIGHT_DP.dp))
    }
}
