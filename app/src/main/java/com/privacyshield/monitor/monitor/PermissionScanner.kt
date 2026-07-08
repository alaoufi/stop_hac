package com.privacyshield.monitor.monitor

import android.content.Context
import android.content.pm.PackageManager
import com.privacyshield.monitor.core.model.PermissionGrantEvent
import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.core.model.TrackedPermission
import com.privacyshield.monitor.data.repo.AppRepository

/**
 * Detects when apps gain sensitive permissions between scans.
 *
 * We can't be notified of permission grants in real time without privileged
 * access, so we snapshot the granted set periodically and diff it. A newly
 * appeared grant becomes a [PermissionGrantEvent] the user is told about,
 * including *why* that permission can be dangerous — never a bare warning.
 */
class PermissionScanner(
    private val context: Context,
    private val appRepository: AppRepository,
) {
    private val pm: PackageManager = context.packageManager
    private val snapshot = context.getSharedPreferences("perm_snapshot", Context.MODE_PRIVATE)

    /** Returns the grants that are new since the previous scan. */
    fun scanForNewGrants(includeSystem: Boolean, now: Long): List<PermissionGrantEvent> {
        val newEvents = mutableListOf<PermissionGrantEvent>()
        val editor = snapshot.edit()

        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = appRepository.isSystemApp(pkg.packageName)
            if (isSystem && !includeSystem) continue

            val requested = pkg.requestedPermissions ?: continue
            val flags = pkg.requestedPermissionsFlags ?: continue

            val grantedNow = mutableSetOf<String>()
            requested.forEachIndexed { i, perm ->
                val isGranted = i < flags.size &&
                    (flags[i] and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                if (isGranted) grantedNow += perm
            }

            val key = "granted:${pkg.packageName}"
            val previous = snapshot.getStringSet(key, null)

            if (previous != null) {
                val fresh = grantedNow - previous
                fresh.forEach { perm ->
                    val tracked = trackedFor(perm) ?: return@forEach
                    val label = appRepository.info(pkg.packageName).label
                    newEvents += PermissionGrantEvent(
                        packageName = pkg.packageName,
                        appLabel = label,
                        permission = tracked,
                        detectedAtMillis = now,
                        riskLevel = riskFor(tracked, isSystem),
                        reason = "reason_perm_granted",
                    )
                }
            }
            editor.putStringSet(key, grantedNow)
        }
        editor.apply()
        return newEvents
    }

    private fun trackedFor(permission: String): TrackedPermission? =
        TrackedPermission.entries.firstOrNull { it.manifestPermission == permission }

    private fun riskFor(perm: TrackedPermission, isSystem: Boolean): RiskLevel = when {
        isSystem -> RiskLevel.ATTENTION
        perm.isSpecialAccess -> RiskLevel.SUSPICIOUS
        perm == TrackedPermission.CAMERA ||
            perm == TrackedPermission.MICROPHONE ||
            perm == TrackedPermission.BACKGROUND_LOCATION -> RiskLevel.ATTENTION
        else -> RiskLevel.NORMAL
    }
}
