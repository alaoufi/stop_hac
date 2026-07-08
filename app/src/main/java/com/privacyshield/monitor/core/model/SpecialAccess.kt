package com.privacyshield.monitor.core.model

/**
 * High-power access that lives outside the normal runtime-permission model and
 * is granted in special system screens. These are the capabilities malware most
 * wants, so the app surfaces and watches them closely.
 */
enum class SpecialAccessType {
    /** Can read screen content and act on the user's behalf — the most abused. */
    ACCESSIBILITY,

    /** Can read every notification, including messages and one-time codes. */
    NOTIFICATION_LISTENER,

    /** Holds device-admin powers (lock, wipe, policy) — hard to remove. */
    DEVICE_ADMIN,
}

/** One app currently holding a [SpecialAccessType]. */
data class SpecialAccessApp(
    val packageName: String,
    val label: String,
    val type: SpecialAccessType,
)
