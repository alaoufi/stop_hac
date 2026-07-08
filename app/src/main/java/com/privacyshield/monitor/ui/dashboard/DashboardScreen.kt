package com.privacyshield.monitor.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.privacyshield.monitor.R
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.monitor.ActiveUse
import com.privacyshield.monitor.monitor.DeviceState
import com.privacyshield.monitor.monitor.DeviceStatusProvider
import com.privacyshield.monitor.monitor.RecentApp
import com.privacyshield.monitor.monitor.RecentAppsProvider
import com.privacyshield.monitor.ui.components.AppIcon
import com.privacyshield.monitor.ui.formatClock
import com.privacyshield.monitor.ui.rememberReason
import com.privacyshield.monitor.ui.sensorName
import com.privacyshield.monitor.ui.components.AppActionsSheet
import com.privacyshield.monitor.ui.components.EventRow
import com.privacyshield.monitor.ui.components.SectionCard
import com.privacyshield.monitor.ui.components.StatusTile
import com.privacyshield.monitor.ui.theme.RiskAmber
import com.privacyshield.monitor.ui.theme.RiskGreen
import com.privacyshield.monitor.ui.theme.RiskRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: DashboardViewModel,
    onOpenWhitelist: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val deviceStatus = remember { DeviceStatusProvider(context) }
    val deviceState = remember { DeviceState(context) }
    var selectedEvent by remember { mutableStateOf<com.privacyshield.monitor.core.model.SecurityEvent?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.nav_settings))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(Modifier.size(2.dp)) }
            item { SecurityLevelHeader(state) }

            // Highest-priority: an app is using the camera/mic right now.
            val liveCamMic = state.activeUses.filter {
                it.sensor == SensorType.CAMERA || it.sensor == SensorType.MICROPHONE
            }
            if (liveCamMic.isNotEmpty()) {
                item { LiveCameraMicAlert(liveCamMic) }
            }

            // Always-visible forced block toggle for the camera & microphone.
            item { ForceBlockCard(vm, state.forceBlockEnabled, state.blockLockEnabled) }

            if (!state.detectorSupported) {
                item { DetectorLimitationCard() }
            }

            item { SensorStatusGrid(state, deviceStatus) }

            item { BackgroundAppsSection(deviceState) }

            item {
                TodaySummary(
                    attention = state.attentionToday,
                    suspicious = state.suspiciousToday,
                    critical = state.criticalToday,
                )
            }

            item {
                SectionCard(
                    title = stringResource(R.string.dashboard_recent_events),
                    trailing = {
                        TextButton(onClick = onOpenEvents) { Text(stringResource(R.string.see_all)) }
                    },
                ) {
                    if (state.recentEvents.isEmpty()) {
                        Text(
                            stringResource(R.string.dashboard_no_events),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column {
                            state.recentEvents.take(6).forEach {
                                EventRow(it, onClick = { selectedEvent = it })
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.size(24.dp)) }
        }
    }

    selectedEvent?.let { event ->
        AppActionsSheet(
            packageName = event.packageName,
            appLabel = event.appLabel,
            riskLevel = event.riskLevel,
            detail = "${sensorName(event.sensor)} · ${rememberReason(event)}",
            onDismiss = { selectedEvent = null },
        )
    }
}

@Composable
private fun SecurityLevelHeader(state: DashboardUiState) {
    val (color, labelRes, descRes) = when (state.securityLevel) {
        SecurityLevel.HIGH -> Triple(RiskGreen, R.string.level_high, R.string.level_high_desc)
        SecurityLevel.MEDIUM -> Triple(RiskAmber, R.string.level_medium, R.string.level_medium_desc)
        SecurityLevel.LOW -> Triple(RiskRed, R.string.level_low, R.string.level_low_desc)
    }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.size(16.dp))
            Column {
                Text(
                    stringResource(R.string.dashboard_security_level),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(labelRes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Text(stringResource(descRes), style = MaterialTheme.typography.bodySmall)
                if (!state.running) {
                    Text(
                        stringResource(R.string.dashboard_monitoring_paused),
                        style = MaterialTheme.typography.bodySmall,
                        color = RiskRed,
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorStatusGrid(state: DashboardUiState, deviceStatus: DeviceStatusProvider) {
    val cameraActive = state.activeUses.any { it.sensor == SensorType.CAMERA }
    val micActive = state.activeUses.any { it.sensor == SensorType.MICROPHONE }
    val locationActive = state.activeUses.any { it.sensor == SensorType.LOCATION }

    SectionCard(title = stringResource(R.string.dashboard_live_status)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusTile(
                    Icons.Filled.CameraAlt, stringResource(R.string.sensor_camera),
                    stateText(cameraActive), cameraActive, Modifier.weight(1f),
                )
                StatusTile(
                    Icons.Filled.Mic, stringResource(R.string.sensor_microphone),
                    stateText(micActive), micActive, Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusTile(
                    Icons.Filled.LocationOn, stringResource(R.string.sensor_location),
                    if (deviceStatus.isLocationServiceOn()) stateText(locationActive)
                    else stringResource(R.string.status_service_off),
                    locationActive, Modifier.weight(1f),
                )
                StatusTile(
                    Icons.Filled.Bluetooth, stringResource(R.string.status_bluetooth),
                    onOff(deviceStatus.isBluetoothOn()),
                    deviceStatus.isBluetoothOn(), Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusTile(
                    Icons.Filled.Contactless, stringResource(R.string.status_nfc),
                    if (!deviceStatus.isNfcAvailable()) stringResource(R.string.status_unavailable)
                    else onOff(deviceStatus.isNfcOn()),
                    deviceStatus.isNfcOn(), Modifier.weight(1f),
                )
                StatusTile(
                    Icons.Filled.ScreenShare, stringResource(R.string.status_screen_share),
                    stringResource(R.string.status_monitored), false, Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun stateText(active: Boolean): String =
    if (active) stringResource(R.string.status_in_use) else stringResource(R.string.status_idle)

@Composable
private fun onOff(on: Boolean): String =
    if (on) stringResource(R.string.status_on) else stringResource(R.string.status_off)

@Composable
private fun TodaySummary(attention: Int, suspicious: Int, critical: Int) {
    SectionCard(title = stringResource(R.string.dashboard_today_summary)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            CountPill("🟡", attention, RiskAmber)
            CountPill("🟠", suspicious, com.privacyshield.monitor.ui.theme.RiskOrange)
            CountPill("🔴", critical, RiskRed)
        }
    }
}

@Composable
private fun CountPill(emoji: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.headlineSmall)
        Text(
            count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

/** Snapshot for the background-apps section: access state + the recent list. */
private data class BgAppsState(val granted: Boolean, val apps: List<RecentApp>)

/**
 * Shows recently-active apps (foreground/background) via Usage Access, or a
 * prompt to grant it. Refreshes every few seconds while visible.
 */
@Composable
private fun BackgroundAppsSection(deviceState: DeviceState) {
    val context = LocalContext.current
    val provider = remember { RecentAppsProvider(context) }

    val snapshot by produceState(BgAppsState(deviceState.hasUsageAccess(), emptyList())) {
        while (true) {
            val granted = deviceState.hasUsageAccess()
            val apps = if (granted) {
                withContext(Dispatchers.IO) { provider.recentApps() }
            } else {
                emptyList()
            }
            value = BgAppsState(granted, apps)
            delay(5_000)
        }
    }

    if (!snapshot.granted) {
        UsageAccessCard()
        return
    }

    SectionCard(title = stringResource(R.string.dashboard_background_apps)) {
        if (snapshot.apps.isEmpty()) {
            Text(
                stringResource(R.string.dashboard_background_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column {
                snapshot.apps.forEach { app -> RecentAppRow(app) }
            }
        }
    }
}

@Composable
private fun RecentAppRow(app: RecentApp) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.packageName, Modifier.size(36.dp))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                appLabelFor(app.packageName),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                stringResource(R.string.dashboard_last_active, formatClock(app.lastActiveMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(
                if (app.foreground) R.string.status_in_use else R.string.status_background,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = if (app.foreground) RiskAmber else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun appLabelFor(packageName: String): String {
    val context = LocalContext.current
    return remember(packageName) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }
}

@Composable
private fun UsageAccessCard() {
    val context = LocalContext.current
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RiskAmber.copy(alpha = 0.15f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.dashboard_usage_access_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.size(6.dp))
            Text(
                stringResource(R.string.dashboard_usage_access_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(10.dp))
            Button(
                onClick = { context.startActivity(RecentAppsProvider.usageAccessIntent()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.dashboard_usage_access_button))
            }
        }
    }
}

/** Prominent, real-time banner naming any app using the camera/mic right now. */
@Composable
private fun LiveCameraMicAlert(uses: List<ActiveUse>) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RiskRed.copy(alpha = 0.15f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = RiskRed)
                Spacer(Modifier.size(10.dp))
                Text(
                    stringResource(R.string.dashboard_live_alert_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RiskRed,
                )
            }
            Spacer(Modifier.size(8.dp))
            uses.forEach { use ->
                Row(
                    Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (use.sensor == SensorType.CAMERA) Icons.Filled.Videocam else Icons.Filled.Mic,
                        contentDescription = null,
                        tint = RiskRed,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        use.appLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * The forced camera/microphone block toggle. On rooted devices it enforces the
 * block via appops; otherwise it opens the OS global sensor switch. Reflects the
 * persisted blocked state and states honestly which mechanism is in effect.
 */
@Composable
private fun ForceBlockCard(vm: DashboardViewModel, blocked: Boolean, lockEnabled: Boolean) {
    val context = LocalContext.current
    val rootAvailable = remember { vm.rootAvailable }
    val unblockTitle = stringResource(R.string.block_auth_title)
    val unblockSubtitle = stringResource(R.string.block_auth_subtitle)

    fun performToggle(enable: Boolean) {
        vm.toggleForceBlock(enable) { result -> handleBlockResult(context, result, blocked) }
    }

    // Lifting the block can require authentication; enabling never does.
    fun onToggleRequested() {
        val activity = context as? androidx.fragment.app.FragmentActivity
        val mustAuth = blocked && lockEnabled && activity != null &&
            com.privacyshield.monitor.ui.BiometricGate.canAuthenticate(context)
        if (mustAuth) {
            com.privacyshield.monitor.ui.BiometricGate.authenticate(
                activity = activity!!,
                title = unblockTitle,
                subtitle = unblockSubtitle,
                onSuccess = { performToggle(false) },
                onFailure = { /* keep the block in place */ },
            )
        } else {
            performToggle(!blocked)
        }
    }
    val container = if (blocked) RiskRed.copy(alpha = 0.18f)
    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MicOff, contentDescription = null, tint = RiskRed)
                Spacer(Modifier.size(10.dp))
                Text(
                    stringResource(
                        if (blocked) R.string.dashboard_block_on_title
                        else R.string.dashboard_block_title,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.size(6.dp))
            Text(
                stringResource(
                    when {
                        blocked && rootAvailable -> R.string.dashboard_block_on_root
                        blocked -> R.string.dashboard_block_on_generic
                        rootAvailable -> R.string.dashboard_block_root
                        else -> R.string.dashboard_block_generic
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(10.dp))
            Button(
                onClick = { onToggleRequested() },
                modifier = Modifier.fillMaxWidth(),
                colors = if (blocked) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.buttonColors(containerColor = RiskRed)
                },
            ) {
                Icon(Icons.Filled.MicOff, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(
                        if (blocked) R.string.dashboard_block_unblock
                        else R.string.dashboard_block_now,
                    ),
                )
            }
            if (blocked && lockEnabled) {
                Spacer(Modifier.size(6.dp))
                Text(
                    stringResource(R.string.dashboard_block_locked_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun handleBlockResult(
    context: android.content.Context,
    result: com.privacyshield.monitor.monitor.ForceBlockController.Result,
    wasBlocked: Boolean,
) {
    val msg = when (result) {
        is com.privacyshield.monitor.monitor.ForceBlockController.Result.Enforced ->
            context.getString(
                if (wasBlocked) R.string.dashboard_block_lifted
                else R.string.dashboard_block_enforced,
                result.affected,
            )
        com.privacyshield.monitor.monitor.ForceBlockController.Result.OpenedSystemToggle -> {
            context.startActivity(
                com.privacyshield.monitor.monitor.ForceBlockController(
                    context, com.privacyshield.monitor.data.repo.AppRepository(context),
                ).systemSensorToggleIntent(),
            )
            context.getString(R.string.dashboard_block_open_system)
        }
        is com.privacyshield.monitor.monitor.ForceBlockController.Result.Failed ->
            context.getString(R.string.dashboard_block_failed, result.message)
    }
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
}

@Composable
private fun DetectorLimitationCard() {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RiskAmber.copy(alpha = 0.15f)),
    ) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = RiskAmber)
            Spacer(Modifier.size(12.dp))
            Text(
                stringResource(R.string.dashboard_detector_unsupported),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
