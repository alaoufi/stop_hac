package com.privacyshield.monitor.monitor

import android.content.Context
import android.content.pm.PackageManager
import com.privacyshield.monitor.PrivacyMonitorApp
import com.privacyshield.monitor.R
import com.privacyshield.monitor.notify.Notifier

/**
 * Enforces the user's "keep this permission denied" locks.
 *
 * Android lets an app that targets an old SDK — or one that re-requests on
 * launch — end up with a sensitive permission re-granted after the user revoked
 * it. This watchdog fights that: for every permission the user locked as denied,
 * it checks whether it has come back and, on a rooted device, revokes it again
 * automatically; without root it raises an alert so the user can act. It never
 * grants anything and sends nothing off-device.
 */
class PermissionEnforcer(private val context: Context) {

    data class Result(val reRevoked: Int, val alerted: Int)

    suspend fun enforce(): Result {
        val app = context.applicationContext as PrivacyMonitorApp
        val locked = app.container.settings.lockedPermissionsNow()
        if (locked.isEmpty()) return Result(0, 0)

        val pm = context.packageManager
        val notifier = Notifier(context)
        var reRevoked = 0
        var alerted = 0

        for (entry in locked) {
            val sep = entry.indexOf('|')
            if (sep <= 0) continue
            val pkg = entry.substring(0, sep)
            val permission = entry.substring(sep + 1)

            val granted = runCatching {
                pm.checkPermission(permission, pkg) == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
            if (!granted) continue

            if (RootShell.isRootBinaryPresent()) {
                if (RootShell.revoke(pkg, permission) is RootShell.Outcome.Success) reRevoked++
            } else {
                val label = app.container.appRepository.info(pkg).label
                val permName = permissionLabel(permission)
                notifier.notifyGeneric(
                    notificationId = 4500 + (entry.hashCode() and 0x3FF),
                    title = context.getString(R.string.perm_regrant_title),
                    text = context.getString(R.string.perm_regrant_text, label, permName),
                    elevated = true,
                    packageName = pkg,
                )
                alerted++
            }
        }
        return Result(reRevoked, alerted)
    }

    private fun permissionLabel(permission: String): String = context.getString(
        when (permission) {
            android.Manifest.permission.CAMERA -> R.string.perm_camera
            android.Manifest.permission.RECORD_AUDIO -> R.string.perm_microphone
            android.Manifest.permission.ACCESS_FINE_LOCATION -> R.string.perm_location
            android.Manifest.permission.READ_CONTACTS -> R.string.perm_contacts
            android.Manifest.permission.READ_SMS -> R.string.perm_sms
            else -> R.string.perm_generic_risk
        },
    )
}
