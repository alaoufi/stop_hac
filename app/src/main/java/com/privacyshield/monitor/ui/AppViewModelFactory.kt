package com.privacyshield.monitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.privacyshield.monitor.di.AppContainer
import com.privacyshield.monitor.ui.apps.AppsViewModel
import com.privacyshield.monitor.ui.dashboard.DashboardViewModel
import com.privacyshield.monitor.ui.events.EventsViewModel
import com.privacyshield.monitor.ui.firewall.FirewallViewModel
import com.privacyshield.monitor.ui.permissions.PermissionsViewModel
import com.privacyshield.monitor.ui.reports.ReportsViewModel
import com.privacyshield.monitor.ui.settings.SettingsViewModel
import com.privacyshield.monitor.ui.whitelist.WhitelistViewModel

/**
 * One factory that knows how to build every screen's ViewModel from the shared
 * [AppContainer]. Simpler than per-VM factories and keeps wiring in one spot.
 */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
            DashboardViewModel(container) as T
        modelClass.isAssignableFrom(AppsViewModel::class.java) ->
            AppsViewModel(container) as T
        modelClass.isAssignableFrom(FirewallViewModel::class.java) ->
            FirewallViewModel(container) as T
        modelClass.isAssignableFrom(EventsViewModel::class.java) ->
            EventsViewModel(container) as T
        modelClass.isAssignableFrom(WhitelistViewModel::class.java) ->
            WhitelistViewModel(container) as T
        modelClass.isAssignableFrom(PermissionsViewModel::class.java) ->
            PermissionsViewModel(container) as T
        modelClass.isAssignableFrom(ReportsViewModel::class.java) ->
            ReportsViewModel(container) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(container) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
