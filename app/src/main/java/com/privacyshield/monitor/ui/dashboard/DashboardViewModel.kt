package com.privacyshield.monitor.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.core.model.SecurityEvent
import com.privacyshield.monitor.data.prefs.AppSettings
import com.privacyshield.monitor.data.repo.DAY_MS
import com.privacyshield.monitor.di.AppContainer
import com.privacyshield.monitor.monitor.ActiveUse
import com.privacyshield.monitor.monitor.MonitorState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Overall protection posture shown prominently on the dashboard. */
enum class SecurityLevel { HIGH, MEDIUM, LOW }

data class DashboardUiState(
    val running: Boolean = false,
    val detectorSupported: Boolean = true,
    val maxProtection: Boolean = false,
    val forceBlockEnabled: Boolean = false,
    val blockLockEnabled: Boolean = false,
    val activeUses: List<ActiveUse> = emptyList(),
    val recentEvents: List<SecurityEvent> = emptyList(),
    val suspiciousToday: Int = 0,
    val criticalToday: Int = 0,
    val attentionToday: Int = 0,
    val securityLevel: SecurityLevel = SecurityLevel.HIGH,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val container: AppContainer) : ViewModel() {

    private val now get() = System.currentTimeMillis()

    private val forceBlock = com.privacyshield.monitor.monitor.ForceBlockController(
        container.appContext,
        container.appRepository,
    )

    val rootAvailable: Boolean get() = forceBlock.rootAvailable
    fun systemSensorToggleIntent() = forceBlock.systemSensorToggleIntent()

    /**
     * Toggles the forced camera/mic block. Persists the flag, applies the block
     * via root where possible, and reports the concrete outcome to the caller so
     * the UI can be honest about what actually happened.
     */
    fun toggleForceBlock(
        enable: Boolean,
        onResult: (com.privacyshield.monitor.monitor.ForceBlockController.Result) -> Unit,
    ) {
        viewModelScope.launch {
            container.settings.setForceBlock(enable)
            val result = if (enable) forceBlock.block() else forceBlock.unblock()
            onResult(result)
        }
    }

    /**
     * Panic: instant maximum lockdown. Turns on the forced sensor block and
     * maximum-protection alerting in one tap, then reports how the sensor block
     * was enforced (root vs system toggle).
     */
    fun panic(onResult: (com.privacyshield.monitor.monitor.ForceBlockController.Result) -> Unit) {
        viewModelScope.launch {
            container.settings.setMaxProtection(true)
            container.settings.setNotifyNormal(true)
            container.settings.setForceBlock(true)
            onResult(forceBlock.block())
        }
    }

    val state: StateFlow<DashboardUiState> = combine(
        MonitorState.running,
        MonitorState.detectorSupported,
        MonitorState.active,
        container.settings.settings,
        container.eventRepository.observeRecent(limit = 15),
    ) { running, supported, active, settings, recent ->
        Quint(running, supported, active.values.toList(), settings, recent)
    }.flatMapLatest { (running, supported, active, settings, recent) ->
        combine(
            container.eventRepository.countByRiskSince(RiskLevel.SUSPICIOUS, now - DAY_MS),
            container.eventRepository.countByRiskSince(RiskLevel.CRITICAL, now - DAY_MS),
            container.eventRepository.countByRiskSince(RiskLevel.ATTENTION, now - DAY_MS),
        ) { suspicious, critical, attention ->
            DashboardUiState(
                running = running,
                detectorSupported = supported,
                maxProtection = settings.maxProtectionEnabled,
                forceBlockEnabled = settings.forceBlockEnabled,
                blockLockEnabled = settings.blockLockEnabled,
                activeUses = active,
                recentEvents = recent,
                suspiciousToday = suspicious,
                criticalToday = critical,
                attentionToday = attention,
                securityLevel = computeLevel(critical, suspicious, active),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private fun computeLevel(
        critical: Int,
        suspicious: Int,
        active: List<ActiveUse>,
    ): SecurityLevel = when {
        critical > 0 -> SecurityLevel.LOW
        suspicious > 0 -> SecurityLevel.MEDIUM
        else -> SecurityLevel.HIGH
    }

    private data class Quint(
        val running: Boolean,
        val supported: Boolean,
        val active: List<ActiveUse>,
        val settings: AppSettings,
        val recent: List<SecurityEvent>,
    )
}
