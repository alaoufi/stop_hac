package com.privacyshield.monitor.ui.firewall

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacyshield.monitor.core.model.AppInfo
import com.privacyshield.monitor.di.AppContainer
import com.privacyshield.monitor.monitor.FirewallState
import com.privacyshield.monitor.monitor.FirewallVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FirewallViewModel(private val container: AppContainer) : ViewModel() {

    val active: StateFlow<Boolean> = FirewallState.active
    val enabled: StateFlow<Boolean> = container.settings.settings
        .map { it.firewallEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val blocked: StateFlow<Set<String>> = container.settings.blockedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    fun loadApps(includeSystem: Boolean) = viewModelScope.launch {
        _apps.value = withContext(Dispatchers.IO) {
            container.appRepository.installedApps(includeSystem)
                .filter { it.packageName != container.appContext.packageName }
        }
    }

    /** Toggle a single app's internet block; re-arms the tunnel if it's running. */
    fun toggleApp(context: Context, packageName: String, block: Boolean) {
        viewModelScope.launch {
            val current = container.settings.blockedAppsNow().toMutableSet()
            if (block) current.add(packageName) else current.remove(packageName)
            container.settings.setBlockedApps(current)
            // If the tunnel is up, re-establish it to apply the new rule set.
            if (FirewallState.active.value) {
                FirewallVpnService.restart(context)
            }
        }
    }

    /** Persist the firewall master switch. Starting requires prior VPN consent. */
    fun setEnabled(context: Context, enable: Boolean) {
        viewModelScope.launch {
            container.settings.setFirewallEnabled(enable)
            if (enable) FirewallVpnService.start(context) else FirewallVpnService.stop(context)
        }
    }

    fun appLabel(packageName: String): String = container.appRepository.info(packageName).label
}
