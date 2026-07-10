package com.privacyshield.monitor.data.repo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.privacyshield.monitor.core.model.AppInfo

/**
 * Resolves package names to human labels / icons and caches the results.
 * Reads are cheap and purely local (PackageManager); nothing leaves the device.
 */
class AppRepository(context: Context) {

    private val pm: PackageManager = context.packageManager
    private val appContext = context.applicationContext
    private val labelCache = HashMap<String, AppInfo>()

    fun info(packageName: String): AppInfo = labelCache.getOrPut(packageName) {
        try {
            val ai = pm.getApplicationInfo(packageName, 0)
            AppInfo(
                packageName = packageName,
                label = pm.getApplicationLabel(ai).toString(),
                isSystemApp = ai.isSystem(),
            )
        } catch (e: PackageManager.NameNotFoundException) {
            AppInfo(packageName = packageName, label = packageName)
        }
    }

    fun isSystemApp(packageName: String): Boolean = info(packageName).isSystemApp

    /** ApplicationInfo.category (API 26+), or -1 (undefined) if unavailable. */
    fun categoryOf(packageName: String): Int = try {
        pm.getApplicationInfo(packageName, 0).category
    } catch (e: PackageManager.NameNotFoundException) {
        ApplicationInfo.CATEGORY_UNDEFINED
    }

    /** Whether [packageName] currently holds a granted [permission]. */
    fun holdsPermission(packageName: String, permission: String): Boolean =
        pm.checkPermission(permission, packageName) == PackageManager.PERMISSION_GRANTED

    fun isInstalled(packageName: String): Boolean = try {
        pm.getApplicationInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** All launchable / user-visible apps, sorted by label, for the whitelist picker. */
    fun installedApps(includeSystem: Boolean): List<AppInfo> =
        pm.getInstalledApplications(0)
            .asSequence()
            .filter { includeSystem || !it.isSystem() || it.packageName == appContext.packageName }
            .map {
                AppInfo(
                    packageName = it.packageName,
                    label = runCatching { pm.getApplicationLabel(it).toString() }.getOrDefault(it.packageName),
                    isSystemApp = it.isSystem(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()

    private fun ApplicationInfo.isSystem(): Boolean =
        (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
}
