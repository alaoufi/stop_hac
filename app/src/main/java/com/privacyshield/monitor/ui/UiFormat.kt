package com.privacyshield.monitor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.privacyshield.monitor.R
import com.privacyshield.monitor.core.model.SecurityEvent
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.monitor.ReasonFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Resolves an event's reason key + args into a localised sentence. */
@Composable
fun rememberReason(event: SecurityEvent): String {
    val context = LocalContext.current
    return ReasonFormatter.format(context, event.reasonKey, event.reasonArgs)
}

@Composable
fun sensorName(sensor: SensorType): String = stringResource(
    when (sensor) {
        SensorType.CAMERA -> R.string.sensor_camera
        SensorType.MICROPHONE -> R.string.sensor_microphone
        SensorType.LOCATION -> R.string.sensor_location
    },
)

private val timeFmt = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault())
private val clockFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

fun formatTime(millis: Long): String = timeFmt.format(Date(millis))
fun formatClock(millis: Long): String = clockFmt.format(Date(millis))

/** Human duration, e.g. "3m 12s" / "45s". */
fun formatDuration(millis: Long?): String {
    if (millis == null) return "—"
    val totalSec = millis / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}
