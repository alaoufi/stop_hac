package com.privacyshield.monitor.monitor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide live status of the firewall VPN, observed by the UI so the
 * firewall screen reflects whether the tunnel is actually up and how many apps
 * are currently cut off.
 */
object FirewallState {
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val _blockedCount = MutableStateFlow(0)
    val blockedCount: StateFlow<Int> = _blockedCount.asStateFlow()

    fun setActive(active: Boolean) { _active.value = active }
    fun setBlockedCount(count: Int) { _blockedCount.value = count }
}
