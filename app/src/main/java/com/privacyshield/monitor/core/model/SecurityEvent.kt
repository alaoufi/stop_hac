package com.privacyshield.monitor.core.model

/**
 * The user's decision on an event, when one was made.
 */
enum class UserDecision {
    NONE,
    ALLOW_ONCE,
    ALLOW_ALWAYS,
    DENIED,
    APP_STOPPED,
    WHITELISTED,
}

/**
 * A single observed access to a sensitive resource, fully annotated by the
 * analysis engine. This is the central record the whole app revolves around —
 * it is what we persist, list, filter, export and summarise in reports.
 *
 * The risk explanation is stored as a [reasonKey] + [reasonArgs] pair rather
 * than a baked string, so the UI can render it in Arabic or English at display
 * time. All timestamps are epoch millis (device local clock). Everything stays
 * on the device; nothing here is ever transmitted off-device.
 */
data class SecurityEvent(
    val id: Long = 0L,
    val packageName: String,
    val appLabel: String,
    val sensor: SensorType,
    /** When the access started. */
    val startTimeMillis: Long,
    /** When the access ended, or null while still active. */
    val endTimeMillis: Long? = null,
    /** Was the app in the foreground (visible to the user) at start? */
    val foreground: Boolean,
    /** Was the device screen on at start? */
    val screenOn: Boolean,
    /** Was the device in an active phone/VoIP call at start? */
    val inCall: Boolean,
    val riskLevel: RiskLevel,
    /** String-resource key for the plain-language classification reason. */
    val reasonKey: String,
    /** Arguments filling the reason template's placeholders. */
    val reasonArgs: List<String> = emptyList(),
    val decision: UserDecision = UserDecision.NONE,
    val isSystemApp: Boolean = false,
) {
    val durationMillis: Long?
        get() = endTimeMillis?.let { it - startTimeMillis }

    val isActive: Boolean get() = endTimeMillis == null
}
