package com.privacyshield.monitor.monitor

import com.privacyshield.monitor.core.model.SensorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Which app currently holds a given sensor, if any. */
data class ActiveUse(
    val sensor: SensorType,
    val packageName: String,
    val appLabel: String,
    val startTimeMillis: Long,
)

/**
 * Process-wide, in-memory view of what is happening *right now*, so the
 * dashboard can reflect live camera/mic/location activity without hitting the
 * database. Written by [MonitorService], observed by the UI.
 */
object MonitorState {
    private val _active = MutableStateFlow<Map<String, ActiveUse>>(emptyMap())
    val active: StateFlow<Map<String, ActiveUse>> = _active.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _detectorSupported = MutableStateFlow(true)
    val detectorSupported: StateFlow<Boolean> = _detectorSupported.asStateFlow()

    private fun key(sensor: SensorType, pkg: String) = "${sensor.name}:$pkg"

    fun onStart(use: ActiveUse) = _active.update { it + (key(use.sensor, use.packageName) to use) }

    fun onStop(sensor: SensorType, pkg: String) =
        _active.update { it - key(sensor, pkg) }

    fun isSensorActive(sensor: SensorType): Boolean =
        _active.value.values.any { it.sensor == sensor }

    fun activeFor(sensor: SensorType): List<ActiveUse> =
        _active.value.values.filter { it.sensor == sensor }

    fun setRunning(running: Boolean) { _running.value = running }
    fun setDetectorSupported(supported: Boolean) { _detectorSupported.value = supported }
    fun reset() { _active.value = emptyMap() }
}
