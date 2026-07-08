package com.privacyshield.monitor.ui.apps

import android.Manifest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.data.repo.DAY_MS
import com.privacyshield.monitor.di.AppContainer
import com.privacyshield.monitor.monitor.MonitorState
import com.privacyshield.monitor.monitor.RecentAppsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** How the app is running, as far as we can determine. */
enum class RunState { FOREGROUND, RECENT_BACKGROUND, INSTALLED }

/**
 * One row of the Apps screen: an installed app, what it can do, whether it has
 * been active, and its assessed risk — with everything needed to act on it.
 */
data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val category: Int,
    val canCamera: Boolean,
    val canMic: Boolean,
    val canLocation: Boolean,
    val runState: RunState,
    val liveSensor: SensorType?,
    val backgroundUses: Int,
    val risk: RiskLevel,
)

data class AppsUiState(
    val apps: List<AppEntry> = emptyList(),
    val loading: Boolean = true,
)

/**
 * Builds the device-wide app inventory with a per-app risk score. Honest by
 * construction: "running in the background" is derived from the supported
 * signals (recent usage events + live sensor state), not a fabricated live
 * process list, and every heavy call runs off the main thread.
 */
class AppsViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(AppsUiState())
    val state: StateFlow<AppsUiState> = _state.asStateFlow()

    fun refresh(includeSystem: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val apps = withContext(Dispatchers.IO) { build(includeSystem) }
            _state.value = AppsUiState(apps = apps, loading = false)
        }
    }

    private suspend fun build(includeSystem: Boolean): List<AppEntry> {
        val now = System.currentTimeMillis()
        val since = now - 30 * DAY_MS
        val repo = container.eventRepository
        val apps = container.appRepository

        val recent = RecentAppsProvider(container.appContext).recentApps(windowMinutes = 60, limit = 60)
        val recentMap = recent.associateBy { it.packageName }
        val liveUses = MonitorState.active.value.values

        val installed = apps.installedApps(includeSystem)

        return installed.map { info ->
            val pkg = info.packageName
            val canCamera = apps.holdsPermission(pkg, Manifest.permission.CAMERA)
            val canMic = apps.holdsPermission(pkg, Manifest.permission.RECORD_AUDIO)
            val canLocation = apps.holdsPermission(pkg, Manifest.permission.ACCESS_FINE_LOCATION) ||
                apps.holdsPermission(pkg, Manifest.permission.ACCESS_COARSE_LOCATION)

            val live = liveUses.firstOrNull { it.packageName == pkg }?.sensor
            val recentEntry = recentMap[pkg]
            val runState = when {
                recentEntry?.foreground == true -> RunState.FOREGROUND
                recentEntry != null -> RunState.RECENT_BACKGROUND
                else -> RunState.INSTALLED
            }

            val worstLogged = repo.worstRiskForApp(pkg, since)
            val bgUses = repo.backgroundCountForApp(pkg, since)
            val risk = assessRisk(worstLogged, bgUses, canCamera, canMic, canLocation, info.isSystemApp, live != null)

            AppEntry(
                packageName = pkg,
                label = info.label,
                isSystem = info.isSystemApp,
                category = apps.categoryOf(pkg),
                canCamera = canCamera,
                canMic = canMic,
                canLocation = canLocation,
                runState = runState,
                liveSensor = live,
                backgroundUses = bgUses,
                risk = risk,
            )
        }.sortedWith(
            compareByDescending<AppEntry> { it.risk.weight }
                .thenByDescending { it.liveSensor != null }
                .thenByDescending { it.runState.ordinal }
                .thenBy { it.label.lowercase() },
        )
    }

    /**
     * App-level risk: driven primarily by what was actually observed, with a
     * mild baseline for apps that *can* reach the camera/mic. We deliberately
     * avoid flagging every capable app red to keep the signal meaningful.
     */
    private fun assessRisk(
        worstLogged: RiskLevel?,
        backgroundUses: Int,
        canCamera: Boolean,
        canMic: Boolean,
        canLocation: Boolean,
        isSystem: Boolean,
        liveNow: Boolean,
    ): RiskLevel {
        val observed = worstLogged ?: RiskLevel.NORMAL
        val baseline = when {
            isSystem -> RiskLevel.NORMAL
            (canCamera || canMic) && backgroundUses > 0 -> RiskLevel.ATTENTION
            canCamera || canMic -> RiskLevel.NORMAL
            else -> RiskLevel.NORMAL
        }
        val combined = if (observed.weight >= baseline.weight) observed else baseline
        // A live background access right now is at least "attention".
        return if (liveNow && combined == RiskLevel.NORMAL) RiskLevel.ATTENTION else combined
    }
}
