package com.privacyshield.monitor.monitor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * One app that was recently active, with the last moment it was seen and whether
 * it is (as far as we can tell) currently in the foreground.
 */
data class RecentApp(
    val packageName: String,
    val lastActiveMillis: Long,
    val foreground: Boolean,
)

/**
 * Lists apps that have been active recently, using [UsageStatsManager] events.
 *
 * ### Honest scope
 * Android does not give an ordinary app a reliable, live list of every process
 * running in the background — `getRunningAppProcesses()` returns essentially
 * only our own process on modern Android. The supported signal is the usage
 * event stream, which tells us which apps moved to the foreground/background and
 * when. So this surfaces **recently active apps** (and which one is foreground),
 * which is the accurate, permission-based view — not a fabricated "live process"
 * list. It requires the user to grant Usage Access.
 */
class RecentAppsProvider(private val context: Context) {

    private val usage =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    fun recentApps(windowMinutes: Int = 30, limit: Int = 15): List<RecentApp> {
        val u = usage ?: return emptyList()
        val now = System.currentTimeMillis()
        val begin = now - windowMinutes * 60_000L

        val events = try {
            u.queryEvents(begin, now)
        } catch (e: SecurityException) {
            return emptyList()
        } ?: return emptyList()

        val lastSeen = HashMap<String, Long>()
        val lastState = HashMap<String, Int>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = event.eventType
            if (type == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                type == UsageEvents.Event.MOVE_TO_BACKGROUND ||
                type == UsageEvents.Event.ACTIVITY_RESUMED ||
                type == UsageEvents.Event.ACTIVITY_PAUSED
            ) {
                lastSeen[event.packageName] = event.timeStamp
                lastState[event.packageName] = type
            }
        }

        return lastSeen.entries
            .asSequence()
            .filter { it.key != context.packageName }
            .map { (pkg, ts) ->
                val fg = lastState[pkg] == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    lastState[pkg] == UsageEvents.Event.ACTIVITY_RESUMED
                RecentApp(pkg, ts, fg)
            }
            .sortedByDescending { it.lastActiveMillis }
            .take(limit)
            .toList()
    }

    companion object {
        /** Opens the system Usage Access settings so the user can grant it. */
        fun usageAccessIntent(): Intent =
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
