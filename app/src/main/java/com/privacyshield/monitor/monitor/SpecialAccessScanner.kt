package com.privacyshield.monitor.monitor

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.privacyshield.monitor.core.model.SpecialAccessApp
import com.privacyshield.monitor.core.model.SpecialAccessType
import com.privacyshield.monitor.data.repo.AppRepository

/**
 * Enumerates apps that hold high-power special access — accessibility services,
 * notification listeners and device-admin — by reading the relevant secure
 * settings and the device-policy manager. All read-only; nothing is transmitted.
 *
 * These grants are the ones surveillance and banking-trojan apps rely on, so the
 * app lists them prominently and (via the periodic scan) alerts when a new one
 * appears.
 */
class SpecialAccessScanner(
    private val context: Context,
    private val appRepository: AppRepository,
) {

    fun scan(): List<SpecialAccessApp> {
        val out = mutableListOf<SpecialAccessApp>()
        out += accessibilityApps()
        out += notificationListenerApps()
        out += deviceAdminApps()
        return out.distinctBy { it.packageName to it.type }
            .sortedBy { it.label.lowercase() }
    }

    private fun accessibilityApps(): List<SpecialAccessApp> =
        componentsFromSecure(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .map { it.packageName }
            .distinct()
            .map { app(it, SpecialAccessType.ACCESSIBILITY) }

    private fun notificationListenerApps(): List<SpecialAccessApp> =
        componentsFromSecure("enabled_notification_listeners")
            .map { it.packageName }
            .distinct()
            .map { app(it, SpecialAccessType.NOTIFICATION_LISTENER) }

    private fun deviceAdminApps(): List<SpecialAccessApp> {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val admins = runCatching { dpm?.activeAdmins }.getOrNull() ?: emptyList()
        return admins.map { it.packageName }.distinct().map { app(it, SpecialAccessType.DEVICE_ADMIN) }
    }

    /** Parses a colon-separated list of flattened ComponentNames from Secure settings. */
    private fun componentsFromSecure(key: String): List<ComponentName> {
        val value = runCatching {
            Settings.Secure.getString(context.contentResolver, key)
        }.getOrNull()
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(':')
            .filter { it.isNotBlank() }
            .mapNotNull { ComponentName.unflattenFromString(it) }
    }

    private fun app(pkg: String, type: SpecialAccessType) =
        SpecialAccessApp(pkg, appRepository.info(pkg).label, type)
}
