package com.privacyshield.monitor.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.privacyshield.monitor.di.AppContainer
import com.privacyshield.monitor.ui.dashboard.DashboardScreen
import com.privacyshield.monitor.ui.events.EventsScreen
import com.privacyshield.monitor.ui.intruder.IntruderScreen
import com.privacyshield.monitor.ui.navigation.Routes
import com.privacyshield.monitor.ui.navigation.TopDestination
import com.privacyshield.monitor.ui.settings.SettingsScreen
import com.privacyshield.monitor.ui.whitelist.WhitelistScreen

/**
 * Root composable: bottom-navigation scaffold + the navigation graph. Requests
 * the notification runtime permission on first composition so alerts can show.
 */
@Composable
fun PrivacyShieldApp(container: AppContainer, factory: ViewModelProvider.Factory) {
    RequestNotificationPermission()

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopDestination.entries.forEach { dest ->
                    val selected = backStack?.destination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(stringResource(dest.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopDestination.DASHBOARD.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(TopDestination.DASHBOARD.route) {
                DashboardScreen(
                    vm = viewModel(factory),
                    onOpenWhitelist = { navController.navigate(Routes.WHITELIST) },
                    onOpenEvents = { navController.navigate(TopDestination.EVENTS.route) },
                    onOpenSettings = { navController.navigate(TopDestination.SETTINGS.route) },
                )
            }
            composable(TopDestination.EVENTS.route) {
                EventsScreen(vm = viewModel(factory))
            }
            composable(TopDestination.SETTINGS.route) {
                SettingsScreen(
                    vm = viewModel(factory),
                    onOpenWhitelist = { navController.navigate(Routes.WHITELIST) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onOpenIntruders = { navController.navigate(Routes.INTRUDERS) },
                )
            }
            composable(Routes.WHITELIST) {
                WhitelistScreen(vm = viewModel(factory), onBack = { navController.popBackStack() })
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.INTRUDERS) {
                IntruderScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/** Small helper to fetch a ViewModel from our shared factory in Compose. */
@Composable
private inline fun <reified T : androidx.lifecycle.ViewModel> viewModel(
    factory: ViewModelProvider.Factory,
): T = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
