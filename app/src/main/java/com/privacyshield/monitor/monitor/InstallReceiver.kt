package com.privacyshield.monitor.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.privacyshield.monitor.PrivacyMonitorApp
import com.privacyshield.monitor.R
import com.privacyshield.monitor.notify.Notifier

/**
 * Alerts the user the moment a new app is installed — highlighting any dangerous
 * permissions it already declares. Registered at runtime by [MonitorService]
 * (context-registered receivers still get PACKAGE_ADDED on modern Android, where
 * manifest-declared ones would not).
 */
class InstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.data?.schemeSpecificPart ?: return
        // Ignore the update half of a replace; we only care about genuinely new apps.
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> onInstalled(context, pkg)
            Intent.ACTION_PACKAGE_REMOVED -> onRemoved(context, pkg)
        }
    }

    private fun onInstalled(context: Context, pkg: String) {
        val app = context.applicationContext as PrivacyMonitorApp
        val label = app.container.appRepository.info(pkg).label
        val dangerous = dangerousPermissions(context, pkg)
        val notifier = Notifier(context)

        val text = if (dangerous.isEmpty()) {
            context.getString(R.string.install_new_app_plain, label)
        } else {
            context.getString(R.string.install_new_app_perms, label, dangerous.joinToString("، "))
        }
        notifier.notifyGeneric(
            notificationId = NOTIF_BASE + (pkg.hashCode() and 0x3FF),
            title = context.getString(R.string.install_new_app_title),
            text = text,
            elevated = dangerous.isNotEmpty(),
            packageName = pkg,
        )
    }

    private fun onRemoved(context: Context, pkg: String) {
        // Keep the snapshot store honest so a reinstall is treated as new.
        context.getSharedPreferences("perm_snapshot", Context.MODE_PRIVATE)
            .edit().remove("granted:$pkg").apply()
    }

    /** Human-readable names of the sensitive permissions a package declares. */
    private fun dangerousPermissions(context: Context, pkg: String): List<String> {
        val pm = context.packageManager
        val requested = runCatching {
            pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS).requestedPermissions
        }.getOrNull()?.toSet() ?: return emptyList()

        val map = mapOf(
            android.Manifest.permission.CAMERA to R.string.perm_camera,
            android.Manifest.permission.RECORD_AUDIO to R.string.perm_microphone,
            android.Manifest.permission.ACCESS_FINE_LOCATION to R.string.perm_location,
            android.Manifest.permission.READ_CONTACTS to R.string.perm_contacts,
            android.Manifest.permission.READ_SMS to R.string.perm_sms,
        )
        return map.filterKeys { it in requested }.values.map { context.getString(it) }
    }

    companion object {
        private const val NOTIF_BASE = 3000
    }
}
