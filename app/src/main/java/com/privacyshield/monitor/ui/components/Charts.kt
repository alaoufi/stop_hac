package com.privacyshield.monitor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max

/** A single labelled value for the bar chart. */
data class BarDatum(val label: String, val value: Int, val color: Color)

/**
 * A compact horizontal bar chart. Pure Compose — no third-party charting
 * dependency, keeping the app small and free of network-fetched libraries.
 */
@Composable
fun HorizontalBarChart(data: List<BarDatum>, modifier: Modifier = Modifier) {
    val maxValue = max(1, data.maxOfOrNull { it.value } ?: 1)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        data.forEach { datum ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = datum.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(120.dp),
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(datum.value.toFloat() / maxValue)
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(datum.color),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(datum.value.toString(), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** One slice of the donut. */
data class DonutSlice(val value: Int, val color: Color, val label: String)

/**
 * A donut chart summarising the distribution of events by risk level.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    centerLabel: String = "",
) {
    val total = max(1, slices.sumOf { it.value })
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(140.dp)) {
                var startAngle = -90f
                val stroke = Stroke(width = 34f)
                val inset = stroke.width / 2
                slices.forEach { slice ->
                    val sweep = 360f * slice.value / total
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke.width, size.height - stroke.width),
                        style = stroke,
                    )
                    startAngle += sweep
                }
            }
            Text(centerLabel, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            slices.forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(slice.color),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${slice.label}: ${slice.value}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
