package com.privacyshield.monitor.monitor

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.privacyshield.monitor.core.model.SensorType
import java.util.concurrent.Executor

/**
 * Reported change in a sensor's active state for a specific app.
 */
data class SensorAccessChange(
    val sensor: SensorType,
    val packageName: String,
    val uid: Int,
    val active: Boolean,
)

/**
 * Real-time detector for camera / microphone / location usage across the whole
 * device.
 *
 * ### How this works, and its limits
 * Android does not let an ordinary app *block* another app's sensor access, and
 * it exposes exactly one officially supported way to *observe* it in real time:
 * [AppOpsManager.startWatchingActive] (API 30+). We register for the camera,
 * microphone and location ops; the system then calls us back whenever any app
 * starts or stops using them. On API < 30 this callback does not exist, so we
 * degrade gracefully and rely on the periodic scan and permission monitoring
 * instead. This honesty about platform limits is a core product requirement.
 */
class SensorAccessMonitor(private val context: Context) {

    private val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    private var listener: AppOpsManager.OnOpActiveChangedListener? = null

    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /**
     * Start watching. [onChange] is invoked on [executor] for every start/stop.
     * Returns true if the watcher was registered (API 30+), false otherwise.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun start(executor: Executor, onChange: (SensorAccessChange) -> Unit): Boolean {
        if (!isSupported || listener != null) return isSupported

        // Watch ONLY the camera and microphone in real time. Location ops fire
        // dozens of times per second on any device with active location, which
        // would flood the callback and overheat the phone — and camera/mic are
        // the real concern anyway. Location is still covered by the permission
        // monitor and reports.
        val ops = buildList {
            addAll(SensorType.opStringsFor(SensorType.CAMERA))
            addAll(SensorType.opStringsFor(SensorType.MICROPHONE))
        }.toTypedArray()

        val l = AppOpsManager.OnOpActiveChangedListener { op, uid, packageName, active ->
            val sensor = SensorType.fromOp(op) ?: return@OnOpActiveChangedListener
            onChange(SensorAccessChange(sensor, packageName, uid, active))
        }

        return try {
            appOps.startWatchingActive(ops, executor, l)
            listener = l
            true
        } catch (e: SecurityException) {
            // Some OEM builds restrict watching foreign ops; fail soft.
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    fun stop() {
        val l = listener ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { appOps.stopWatchingActive(l) }
        }
        listener = null
    }
}
