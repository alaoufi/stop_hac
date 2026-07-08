package com.privacyshield.monitor.monitor

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.privacyshield.monitor.PrivacyMonitorApp
import com.privacyshield.monitor.R
import com.privacyshield.monitor.notify.Notifier
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Runs periodically (battery-friendly, ~every 3h) to:
 *  - diff app permissions and alert on newly granted sensitive ones, and
 *  - enforce the event-log retention window.
 *
 * WorkManager batches this with system maintenance windows so it costs almost
 * no battery, satisfying the "efficient background operation" requirement.
 */
class PeriodicScanWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as PrivacyMonitorApp
        val container = app.container
        val settings = container.settings.settings.first()

        // 1. Permission diff.
        val scanner = PermissionScanner(applicationContext, container.appRepository)
        val now = System.currentTimeMillis()
        val notifier = Notifier(applicationContext)
        val newGrants = scanner.scanForNewGrants(settings.includeSystemApps, now)
        newGrants.forEach { grant ->
            val reason = ReasonFormatter.format(
                applicationContext,
                grant.reason,
                listOf(grant.appLabel, grant.permission.name),
            )
            notifier.notifyEvent(
                com.privacyshield.monitor.core.model.SecurityEvent(
                    id = grant.detectedAtMillis % 900,
                    packageName = grant.packageName,
                    appLabel = grant.appLabel,
                    sensor = com.privacyshield.monitor.core.model.SensorType.LOCATION,
                    startTimeMillis = grant.detectedAtMillis,
                    foreground = false,
                    screenOn = false,
                    inCall = false,
                    riskLevel = grant.riskLevel,
                    reasonKey = grant.reason,
                    reasonArgs = listOf(grant.appLabel, grant.permission.name),
                ),
                reason = reason,
            )
        }

        // 2. Special-access diff (accessibility / notification listeners / admins).
        scanSpecialAccess(notifier)

        // 3. Retention.
        container.eventRepository.applyRetention(settings.retentionDays, now)

        return Result.success()
    }

    /** Detects newly granted high-power access since the last scan and alerts. */
    private fun scanSpecialAccess(notifier: Notifier) {
        val app = applicationContext as PrivacyMonitorApp
        val scanner = SpecialAccessScanner(applicationContext, app.container.appRepository)
        val current = scanner.scan()
        val currentKeys = current.map { "${it.type.name}:${it.packageName}" }.toSet()

        val prefs = applicationContext.getSharedPreferences("special_access_snapshot", android.content.Context.MODE_PRIVATE)
        val previous = prefs.getStringSet("set", null)

        if (previous != null) {
            current.filter { "${it.type.name}:${it.packageName}" !in previous }.forEach { sa ->
                val typeName = applicationContext.getString(
                    when (sa.type) {
                        com.privacyshield.monitor.core.model.SpecialAccessType.ACCESSIBILITY -> R.string.sa_accessibility
                        com.privacyshield.monitor.core.model.SpecialAccessType.NOTIFICATION_LISTENER -> R.string.sa_notification_listener
                        com.privacyshield.monitor.core.model.SpecialAccessType.DEVICE_ADMIN -> R.string.sa_device_admin
                    },
                )
                notifier.notifyGeneric(
                    notificationId = 4000 + (sa.packageName.hashCode() and 0x3FF),
                    title = applicationContext.getString(R.string.sa_new_alert_title),
                    text = applicationContext.getString(R.string.sa_new_alert_text, sa.label, typeName),
                    elevated = true,
                    packageName = sa.packageName,
                )
            }
        }
        prefs.edit().putStringSet("set", currentKeys).apply()
    }

    companion object {
        private const val WORK_NAME = "privacy_periodic_scan"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PeriodicScanWorker>(3, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
