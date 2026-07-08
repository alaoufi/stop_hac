package com.privacyshield.monitor.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.privacyshield.monitor.R
import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.core.model.SecurityEvent
import com.privacyshield.monitor.ui.MainActivity

/**
 * Owns all notification behaviour: the persistent foreground-service note that
 * keeps monitoring alive, and per-event alerts whose importance scales with the
 * assessed risk. Actions deep-link to the OS App Info screen — the honest way
 * for a non-privileged app to let the user force-stop or revoke a permission.
 */
class Notifier(private val context: Context) {

    companion object {
        const val CHANNEL_SERVICE = "monitoring_service"
        const val CHANNEL_ALERTS = "risk_alerts"
        const val CHANNEL_INFO = "info_events"

        const val SERVICE_NOTIFICATION_ID = 1001
        private const val ALERT_BASE_ID = 2000
    }

    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val system = context.getSystemService(NotificationManager::class.java)

        val service = NotificationChannel(
            CHANNEL_SERVICE,
            context.getString(R.string.channel_service),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = context.getString(R.string.channel_service_desc) }

        val alerts = NotificationChannel(
            CHANNEL_ALERTS,
            context.getString(R.string.channel_alerts),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_alerts_desc)
            enableVibration(true)
        }

        val info = NotificationChannel(
            CHANNEL_INFO,
            context.getString(R.string.channel_info),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_info_desc) }

        system.createNotificationChannels(listOf(service, alerts, info))
    }

    /** The ongoing notification the foreground service must display. */
    fun buildServiceNotification(contentText: String, maxProtection: Boolean): Notification {
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = context.getString(
            if (maxProtection) R.string.notif_service_title_max else R.string.notif_service_title,
        )
        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /** Post an alert for one event; importance/urgency follow the risk level. */
    fun notifyEvent(event: SecurityEvent, reason: String) {
        if (!hasPostPermission()) return

        val channel = when (event.riskLevel) {
            RiskLevel.SUSPICIOUS, RiskLevel.CRITICAL -> CHANNEL_ALERTS
            else -> CHANNEL_INFO
        }
        val emoji = when (event.riskLevel) {
            RiskLevel.NORMAL -> "🟢"
            RiskLevel.ATTENTION -> "🟡"
            RiskLevel.SUSPICIOUS -> "🟠"
            RiskLevel.CRITICAL -> "🔴"
        }
        val sensorName = context.getString(
            when (event.sensor) {
                com.privacyshield.monitor.core.model.SensorType.CAMERA -> R.string.sensor_camera
                com.privacyshield.monitor.core.model.SensorType.MICROPHONE -> R.string.sensor_microphone
                com.privacyshield.monitor.core.model.SensorType.LOCATION -> R.string.sensor_location
            },
        )
        val title = "$emoji ${event.appLabel} · $sensorName"

        val notification = NotificationCompat.Builder(context, channel)
            .setContentTitle(title)
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setPriority(
                if (event.riskLevel.isElevated) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT,
            )
            .addAction(
                R.drawable.ic_settings,
                context.getString(R.string.action_app_info),
                appInfoIntent(event.packageName),
            )
            .addAction(
                R.drawable.ic_shield,
                context.getString(R.string.action_open_dashboard),
                openAppIntent(),
            )
            .build()

        runCatching { manager.notify(ALERT_BASE_ID + (event.id % 900).toInt(), notification) }
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context, 1,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun appInfoIntent(packageName: String): PendingIntent {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context, packageName.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun hasPostPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
