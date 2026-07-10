package com.privacyshield.monitor.core.analysis

import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.core.model.WhitelistScope

/**
 * All the situational signals the [RiskAnalyzer] needs to reason about a single
 * sensor access. Collected by the monitoring layer at the moment access starts
 * (and refreshed when it ends so we can reason about duration).
 */
data class AccessContext(
    val packageName: String,
    val appLabel: String,
    val sensor: SensorType,
    val foreground: Boolean,
    val screenOn: Boolean,
    val inCall: Boolean,
    val isSystemApp: Boolean,
    /** Whitelist scope the user configured for this app+sensor, if any. */
    val whitelistScope: WhitelistScope?,
    /** How long the access lasted, if it has already ended. */
    val durationMillis: Long? = null,
    /** How many times this app accessed this sensor in the background today. */
    val backgroundHitsToday: Int = 0,
    /** True when access resumed shortly after a device reboot. */
    val shortlyAfterBoot: Boolean = false,
)
