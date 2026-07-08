package com.privacyshield.monitor.core.model

/**
 * Lightweight description of an installed application, resolved from the
 * package name reported by AppOps. Icons are loaded lazily by the UI layer via
 * the package name, so this object stays cheap to pass around.
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    /** True for apps that ship as part of the system image. */
    val isSystemApp: Boolean = false,
    /** Whether the app currently holds the relevant runtime permission. */
    val installTimeMillis: Long = 0L,
)
