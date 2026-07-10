package com.privacyshield.monitor.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.privacyshield.monitor.R

/** The top-level, bottom-bar destinations. */
enum class TopDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    DASHBOARD("dashboard", R.string.nav_dashboard, Icons.Filled.Dashboard),
    EVENTS("events", R.string.nav_events, Icons.Filled.History),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings),
}

/** Secondary routes reachable from within screens. */
object Routes {
    const val WHITELIST = "whitelist"
    const val ABOUT = "about"
    const val FIREWALL = "firewall"
    const val INTRUDERS = "intruders"
}
