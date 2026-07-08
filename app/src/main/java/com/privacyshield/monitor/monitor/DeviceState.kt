package com.privacyshield.monitor.monitor

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.telephony.TelephonyManager

/**
 * Snapshots the situational signals the risk engine needs at the moment a
 * sensor access starts: is the screen on, are we in a call, and is the
 * accessing app currently in the foreground.
 *
 * Foreground detection uses [UsageStatsManager] when the user has granted usage
 * access, falling back to [ActivityManager] importance for the querying app.
 * All of this is read-only, on-device inspection.
 */
class DeviceState(private val context: Context) {

    private val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    fun isScreenOn(): Boolean = power.isInteractive

    @Suppress("DEPRECATION")
    fun isInCall(): Boolean = try {
        telephony?.callState != TelephonyManager.CALL_STATE_IDLE && telephony != null
    } catch (e: SecurityException) {
        false
    }

    /**
     * Best-effort check of whether [packageName] is the current foreground app.
     * Returns null when we genuinely cannot tell (no usage-access permission),
     * so the caller can decide how to treat the uncertainty.
     */
    fun isForeground(packageName: String): Boolean? {
        val u = usage ?: return null
        val now = System.currentTimeMillis()
        val stats = try {
            u.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60_000, now)
        } catch (e: SecurityException) {
            return null
        }
        if (stats.isNullOrEmpty()) return null
        val recent = stats.maxByOrNull { it.lastTimeUsed } ?: return null
        // If no usage-access has been granted, lastTimeUsed is 0 for everything.
        if (recent.lastTimeUsed == 0L) return null
        return recent.packageName == packageName
    }

    /** True if the device booted recently (within [windowMs]). */
    fun bootedRecently(windowMs: Long = 2 * 60_000L): Boolean =
        SystemClock.elapsedRealtime() <= windowMs

    /** Whether this app currently holds usage-access, needed for foreground checks. */
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
