package com.privacyshield.monitor.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.privacyshield.monitor.PrivacyMonitorApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restarts monitoring after a reboot, but only if the user opted in
 * (start-on-boot) and monitoring was enabled. Also arms the periodic scan.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val pending = goAsync()
        val app = context.applicationContext as PrivacyMonitorApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = app.container.settings.settings.first()
                if (settings.startOnBoot && settings.monitoringEnabled) {
                    MonitorService.start(context)
                }
                if (settings.scheduleEnabled) {
                    BlockScheduler(context).schedule(
                        settings.scheduleStartMinutes,
                        settings.scheduleEndMinutes,
                    )
                }
                PeriodicScanWorker.schedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
