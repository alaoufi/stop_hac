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
import androidx.compose.runtime.remember
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
import com.privacyshield.monitor.R
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.monitor.ActiveUse
import com.privacyshield.monitor.monitor.DeviceStatusProvider
import com.privacyshield.monitor.monitor.MaxProtectionController
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

            // Always-visible kill switch to fully cut the sensors via the OS.
            item { KillSwitchCard() }

            if (!state.detectorSupported) {
                item { DetectorLimitationCard() }
            }

            item { SensorStatusGrid(state, deviceStatus) }

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
                            state.recentEvents.take(6).forEach { EventRow(it) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.size(24.dp)) }
        }
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

/** Always-visible control to fully cut power to the camera & microphone. */
@Composable
private fun KillSwitchCard() {
    val context = LocalContext.current
    val controller = remember { MaxProtectionController(context) }
    val supported = remember { controller.deviceSupportsSensorToggle() }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MicOff, contentDescription = null, tint = RiskRed)
                Spacer(Modifier.size(10.dp))
                Text(
                    stringResource(R.string.dashboard_killswitch_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.size(6.dp))
            Text(
                stringResource(
                    if (supported) R.string.dashboard_killswitch_supported
                    else R.string.dashboard_killswitch_generic,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(10.dp))
            Button(
                onClick = { context.startActivity(controller.sensorControlsIntent()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.MicOff, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.dashboard_killswitch_button))
            }
        }
    }
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
