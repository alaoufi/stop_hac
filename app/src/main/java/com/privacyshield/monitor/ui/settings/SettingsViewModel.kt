package com.privacyshield.monitor.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacyshield.monitor.data.prefs.AppLanguage
import com.privacyshield.monitor.data.prefs.AppSettings
import com.privacyshield.monitor.data.prefs.ThemeMode
import com.privacyshield.monitor.di.AppContainer
import com.privacyshield.monitor.monitor.MonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Settings for the focused camera/microphone monitor — only what the core needs.
 */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setMonitoring(enabled: Boolean) = viewModelScope.launch {
        container.settings.setMonitoring(enabled)
        if (enabled) MonitorService.start(container.appContext)
        else MonitorService.stop(container.appContext)
    }

    fun setStartOnBoot(enabled: Boolean) =
        viewModelScope.launch { container.settings.setStartOnBoot(enabled) }

    fun setAlertOnSensorUse(enabled: Boolean) =
        viewModelScope.launch { container.settings.setAlertOnSensorUse(enabled) }

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
