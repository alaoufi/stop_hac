package com.privacyshield.monitor.core.model

/**
 * How much freedom the user has granted a specific app for a specific sensor.
 * Ordered from most permissive to least permissive.
 */
enum class WhitelistScope {
    /** السماح دائماً — never flag this app for this sensor. */
    ALWAYS_ALLOW,

    /** السماح أثناء فتح التطبيق فقط — only benign while the app is foreground. */
    FOREGROUND_ONLY,

    /** السماح أثناء استخدام الشاشة فقط — benign only while the screen is on. */
    SCREEN_ON_ONLY,

    /** منع بالخلفية — background usage is always flagged. */
    BLOCK_BACKGROUND,

    /** منع دائماً — always treat as the highest concern. */
    ALWAYS_BLOCK,
}

/**
 * A single user-authored exception. The combination of [packageName] and
 * [sensor] is unique (enforced by the database).
 */
data class WhitelistRule(
    val packageName: String,
    val sensor: SensorType,
    val scope: WhitelistScope,
    val note: String = "",
    val updatedAtMillis: Long = 0L,
)
