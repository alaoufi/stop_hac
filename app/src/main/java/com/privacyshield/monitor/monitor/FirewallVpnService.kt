package com.privacyshield.monitor.monitor

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.privacyshield.monitor.PrivacyMonitorApp
import com.privacyshield.monitor.R
import com.privacyshield.monitor.notify.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.nio.ByteBuffer

/**
 * A no-root, **local-only** per-app internet firewall built on [VpnService].
 *
 * ### How it blocks without any network access of its own
 * We create a VPN whose route captures *only* the apps the user chose to block
 * (via [VpnService.Builder.addAllowedApplication]). Every other app bypasses the
 * tunnel and keeps normal internet. For the blocked apps, their packets arrive at
 * our tunnel and we simply **drop them** — we never open an outbound socket, never
 * forward, never inspect payloads, and send nothing anywhere. That is why this
 * feature needs no INTERNET permission and cannot leak data: it only ever
 * discards the traffic of apps you explicitly muted.
 *
 * This is the strongest anti-exfiltration control a normal app can offer: a
 * suspicious app can be cut off from the internet entirely, on the spot.
 */
class FirewallVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var drainThread: Thread? = null
    @Volatile private var running = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            teardown()
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundNotification()
        scope.launch { establish() }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val note = Notifier(this).buildServiceNotification(
            contentText = getString(R.string.firewall_running_note),
            maxProtection = false,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FIREWALL_NOTIFICATION_ID,
                note,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(FIREWALL_NOTIFICATION_ID, note)
        }
    }

    private suspend fun establish() {
        teardownTunnel()
        val container = (application as PrivacyMonitorApp).container
        val blocked = container.settings.blockedAppsNow()
            .filter { it != packageName }
            .toSet()

        FirewallState.setBlockedCount(blocked.size)
        if (blocked.isEmpty()) {
            // Nothing to block — keep the service idle rather than route traffic.
            FirewallState.setActive(false)
            return
        }

        val builder = Builder()
            .setSession(getString(R.string.app_name))
            .addAddress("10.111.222.1", 32)
            .addAddress("fd00:1:2:3::1", 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .setBlocking(true)

        // Restrict the tunnel to ONLY the blocked apps; everyone else bypasses it.
        var added = 0
        for (pkg in blocked) {
            try {
                builder.addAllowedApplication(pkg)
                added++
            } catch (e: Exception) {
                // App may have been uninstalled between snapshots; skip it.
            }
        }
        if (added == 0) {
            FirewallState.setActive(false)
            return
        }

        val iface = try {
            builder.establish()
        } catch (e: Exception) {
            FirewallState.setActive(false)
            null
        } ?: return

        vpnInterface = iface
        running = true
        FirewallState.setActive(true)
        startDrain(iface)
    }

    /**
     * Reads and discards the blocked apps' packets. Draining prevents the tunnel
     * buffer from filling; because we never write a reply, those apps get no
     * connectivity. No packet ever leaves the device.
     */
    private fun startDrain(iface: ParcelFileDescriptor) {
        val thread = Thread({
            val input = FileInputStream(iface.fileDescriptor)
            val buffer = ByteBuffer.allocate(32767)
            try {
                while (running) {
                    val n = input.read(buffer.array())
                    if (n <= 0) {
                        Thread.sleep(50)
                        continue
                    }
                    // Drop: intentionally do nothing with the bytes.
                    buffer.clear()
                }
            } catch (e: Exception) {
                // Interface closed / service stopping.
            }
        }, "firewall-drain")
        thread.isDaemon = true
        drainThread = thread
        thread.start()
    }

    private fun teardownTunnel() {
        running = false
        drainThread?.interrupt()
        drainThread = null
        vpnInterface?.let { runCatching { it.close() } }
        vpnInterface = null
    }

    private fun teardown() {
        teardownTunnel()
        FirewallState.setActive(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        teardown()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.privacyshield.monitor.FIREWALL_STOP"
        const val FIREWALL_NOTIFICATION_ID = 1101

        fun start(context: Context) {
            val intent = Intent(context, FirewallVpnService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, FirewallVpnService::class.java).setAction(ACTION_STOP),
            )
        }

        /** Re-establish the tunnel to pick up a changed blocked-apps set. */
        fun restart(context: Context) = start(context)
    }
}
