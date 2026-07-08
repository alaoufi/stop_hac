package com.privacyshield.monitor.ui.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacyshield.monitor.core.model.AppInfo
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.core.model.WhitelistRule
import com.privacyshield.monitor.core.model.WhitelistScope
import com.privacyshield.monitor.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WhitelistViewModel(private val container: AppContainer) : ViewModel() {

    val rules: StateFlow<List<WhitelistRule>> = container.whitelistRepository.rules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    fun loadApps(includeSystem: Boolean) = viewModelScope.launch {
        _apps.value = withContext(Dispatchers.IO) {
            container.appRepository.installedApps(includeSystem)
        }
    }

    fun addRule(packageName: String, sensor: SensorType, scope: WhitelistScope, note: String = "") =
        viewModelScope.launch {
            container.whitelistRepository.upsert(
                WhitelistRule(packageName, sensor, scope, note, System.currentTimeMillis()),
            )
        }

    fun removeRule(rule: WhitelistRule) = viewModelScope.launch {
        container.whitelistRepository.remove(rule.packageName, rule.sensor)
    }

    fun appLabel(packageName: String): String = container.appRepository.info(packageName).label
}
