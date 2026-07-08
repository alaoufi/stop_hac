package com.privacyshield.monitor.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.privacyshield.monitor.PrivacyMonitorApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires at the scheduled start/end times to switch the forced block on or off,
 * persisting the flag and applying it (root-enforced where possible).
 */
class ScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val block = when (intent.action) {
            ACTION_BLOCK -> true
            ACTION_UNBLOCK -> false
            else -> return
        }
        val pending = goAsync()
        val app = context.applicationContext as PrivacyMonitorApp
        val controller = ForceBlockController(context.applicationContext, app.container.appRepository)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.settings.setForceBlock(block)
                if (block) controller.block() else controller.unblock()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_BLOCK = "com.privacyshield.monitor.SCHEDULE_BLOCK"
        const val ACTION_UNBLOCK = "com.privacyshield.monitor.SCHEDULE_UNBLOCK"
    }
}
