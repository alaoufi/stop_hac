package com.privacyshield.monitor.core.model

import android.app.AppOpsManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * The sensitive resources this app is able to observe.
 *
 * Each type maps to one or more [AppOpsManager] op strings. Watching "active"
 * ops (via [AppOpsManager.startWatchingActive]) is the only officially exposed
 * mechanism a non-privileged app has to learn, in real time, that *some* app on
 * the device started using the camera or the microphone. We rely on it here.
 */
enum class SensorType(val id: String) {
    CAMERA("camera"),
    MICROPHONE("microphone"),
    LOCATION("location");

    companion object {
        /** All AppOps op strings we register a watcher for, by [SensorType]. */
        @RequiresApi(Build.VERSION_CODES.R)
        fun opStringsFor(type: SensorType): List<String> = when (type) {
            CAMERA -> listOf(AppOpsManager.OPSTR_CAMERA)
            MICROPHONE -> listOf(AppOpsManager.OPSTR_RECORD_AUDIO)
            LOCATION -> listOf(
                AppOpsManager.OPSTR_FINE_LOCATION,
                AppOpsManager.OPSTR_COARSE_LOCATION,
            )
        }

        /** Reverse lookup: which [SensorType] does an AppOps op string belong to. */
        fun fromOp(op: String): SensorType? = when (op) {
            AppOpsManager.OPSTR_CAMERA -> CAMERA
            AppOpsManager.OPSTR_RECORD_AUDIO -> MICROPHONE
            AppOpsManager.OPSTR_FINE_LOCATION,
            AppOpsManager.OPSTR_COARSE_LOCATION -> LOCATION
            else -> null
        }
    }
}
