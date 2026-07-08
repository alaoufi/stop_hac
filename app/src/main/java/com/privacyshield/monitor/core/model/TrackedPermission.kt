package com.privacyshield.monitor.core.model

import android.Manifest

/**
 * The sensitive permissions and special access states we keep an eye on. The
 * scanner compares snapshots over time and raises an event whenever one of
 * these transitions from "not granted" to "granted".
 */
enum class TrackedPermission(
    val manifestPermission: String?,
    /** True for "special" access that is toggled in Settings, not a runtime grant. */
    val isSpecialAccess: Boolean = false,
) {
    CAMERA(Manifest.permission.CAMERA),
    MICROPHONE(Manifest.permission.RECORD_AUDIO),
    FINE_LOCATION(Manifest.permission.ACCESS_FINE_LOCATION),
    BACKGROUND_LOCATION("android.permission.ACCESS_BACKGROUND_LOCATION"),
    NOTIFICATIONS("android.permission.POST_NOTIFICATIONS"),
    READ_CONTACTS(Manifest.permission.READ_CONTACTS),
    READ_SMS(Manifest.permission.READ_SMS),
    READ_PHONE_STATE(Manifest.permission.READ_PHONE_STATE),

    // Special access — surfaced as "why this can be dangerous" to the user.
    ACCESSIBILITY(null, isSpecialAccess = true),
    OVERLAY("android.permission.SYSTEM_ALERT_WINDOW", isSpecialAccess = true),
    INSTALL_PACKAGES("android.permission.REQUEST_INSTALL_PACKAGES", isSpecialAccess = true),
    MANAGE_STORAGE("android.permission.MANAGE_EXTERNAL_STORAGE", isSpecialAccess = true),
    USAGE_ACCESS(null, isSpecialAccess = true),
}

/**
 * A newly detected permission grant for an app.
 */
data class PermissionGrantEvent(
    val packageName: String,
    val appLabel: String,
    val permission: TrackedPermission,
    val detectedAtMillis: Long,
    val riskLevel: RiskLevel,
    val reason: String,
)
