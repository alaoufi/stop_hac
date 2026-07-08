package com.privacyshield.monitor.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacyshield.monitor.data.prefs.AppLanguage
import com.privacyshield.monitor.data.prefs.AppSettings
import com.privacyshield.monitor.data.prefs.ThemeMode
import com.privacyshield.monitor.di.AppContainer
import com.privacyshield.monitor.monitor.MaxProtectionController
import com.privacyshield.monitor.monitor.MonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val maxProtection = MaxProtectionController(container.appContext)

    fun deviceSupportsSensorToggle(): Boolean = maxProtection.deviceSupportsSensorToggle()
    fun sensorControlsIntent() = maxProtection.sensorControlsIntent()

    fun setMonitoring(enabled: Boolean) = viewModelScope.launch {
        container.settings.setMonitoring(enabled)
        if (enabled) MonitorService.start(container.appContext)
        else MonitorService.stop(container.appContext)
    }

    fun setStartOnBoot(enabled: Boolean) =
        viewModelScope.launch { container.settings.setStartOnBoot(enabled) }

    fun setMaxProtection(enabled: Boolean) = viewModelScope.launch {
        container.settings.setMaxProtection(enabled)
        // Max protection surfaces every access, so also enable normal alerts.
        container.settings.setNotifyNormal(enabled)
    }

    fun setNotifyNormal(enabled: Boolean) =
        viewModelScope.launch { container.settings.setNotifyNormal(enabled) }

    fun setAlertOnSensorUse(enabled: Boolean) =
        viewModelScope.launch { container.settings.setAlertOnSensorUse(enabled) }

    fun setOverlayIndicator(enabled: Boolean) =
        viewModelScope.launch { container.settings.setOverlayIndicator(enabled) }

    fun setBlockLock(enabled: Boolean) =
        viewModelScope.launch { container.settings.setBlockLock(enabled) }

    private val scheduler = com.privacyshield.monitor.monitor.BlockScheduler(container.appContext)

    /** Persists the auto-block schedule and (re)arms or cancels the alarms. */
    fun setSchedule(enabled: Boolean, startMinutes: Int, endMinutes: Int) {
        viewModelScope.launch {
            container.settings.setSchedule(enabled, startMinutes, endMinutes)
            if (enabled) scheduler.schedule(startMinutes, endMinutes) else scheduler.cancel()
        }
    }

    fun canAuthenticate(): Boolean =
        com.privacyshield.monitor.ui.BiometricGate.canAuthenticate(container.appContext)

    /** True when the "display over other apps" permission is already granted. */
    fun canDrawOverlay(): Boolean = android.provider.Settings.canDrawOverlays(container.appContext)

    fun overlayPermissionIntent() = android.content.Intent(
        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        android.net.Uri.parse("package:${container.appContext.packageName}"),
    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)

    fun setIncludeSystemApps(enabled: Boolean) =
        viewModelScope.launch { container.settings.setIncludeSystemApps(enabled) }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { container.settings.setTheme(mode) }

    fun setLanguage(context: Context, lang: AppLanguage) = viewModelScope.launch {
        container.settings.setLanguage(lang)
        LocaleController.apply(lang)
    }

    fun setRetention(days: Int) =
        viewModelScope.launch { container.settings.setRetentionDays(days) }

    fun clearHistory() = viewModelScope.launch { container.eventRepository.clearAll() }
}
