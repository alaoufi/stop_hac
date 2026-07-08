package com.privacyshield.monitor.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privacyshield.monitor.R
import com.privacyshield.monitor.data.prefs.AppLanguage
import com.privacyshield.monitor.data.prefs.ThemeMode
import com.privacyshield.monitor.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onOpenWhitelist: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.size(2.dp))

            SectionCard(title = stringResource(R.string.settings_monitoring)) {
                Column {
                    ToggleRow(
                        stringResource(R.string.settings_enable_monitoring),
                        stringResource(R.string.settings_enable_monitoring_desc),
                        settings.monitoringEnabled,
                    ) { vm.setMonitoring(it) }
                    ToggleRow(
                        stringResource(R.string.settings_start_on_boot),
                        stringResource(R.string.settings_start_on_boot_desc),
                        settings.startOnBoot,
                    ) { vm.setStartOnBoot(it) }
                    ToggleRow(
                        stringResource(R.string.settings_notify_normal),
                        stringResource(R.string.settings_notify_normal_desc),
                        settings.notifyNormal,
                    ) { vm.setNotifyNormal(it) }
                    ToggleRow(
                        stringResource(R.string.settings_include_system),
                        stringResource(R.string.settings_include_system_desc),
                        settings.includeSystemApps,
                    ) { vm.setIncludeSystemApps(it) }
                }
            }

            SectionCard(title = stringResource(R.string.settings_max_protection)) {
                Column {
                    ToggleRow(
                        stringResource(R.string.settings_max_protection),
                        stringResource(R.string.settings_max_protection_desc),
                        settings.maxProtectionEnabled,
                    ) { vm.setMaxProtection(it) }
                    Spacer(Modifier.size(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { context.startActivity(vm.sensorControlsIntent()) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_system_sensor_controls),
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                stringResource(
                                    if (vm.deviceSupportsSensorToggle()) R.string.settings_sensor_toggle_supported
                                    else R.string.settings_sensor_toggle_unsupported,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    }
                }
            }

            SectionCard(title = stringResource(R.string.settings_appearance)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeChip(stringResource(R.string.theme_system), settings.themeMode == ThemeMode.SYSTEM) { vm.setTheme(ThemeMode.SYSTEM) }
                        ThemeChip(stringResource(R.string.theme_light), settings.themeMode == ThemeMode.LIGHT) { vm.setTheme(ThemeMode.LIGHT) }
                        ThemeChip(stringResource(R.string.theme_dark), settings.themeMode == ThemeMode.DARK) { vm.setTheme(ThemeMode.DARK) }
                    }
                    Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeChip(stringResource(R.string.lang_system), settings.language == AppLanguage.SYSTEM) { vm.setLanguage(context, AppLanguage.SYSTEM) }
                        ThemeChip(stringResource(R.string.lang_arabic), settings.language == AppLanguage.ARABIC) { vm.setLanguage(context, AppLanguage.ARABIC) }
                        ThemeChip(stringResource(R.string.lang_english), settings.language == AppLanguage.ENGLISH) { vm.setLanguage(context, AppLanguage.ENGLISH) }
                    }
                }
            }

            SectionCard(title = stringResource(R.string.settings_data)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.settings_retention, settings.retentionDays),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7, 30, 90, 365).forEach { d ->
                            ThemeChip("$d", settings.retentionDays == d) { vm.setRetention(d) }
                        }
                    }
                    NavRow(stringResource(R.string.settings_whitelist), onOpenWhitelist)
                    Row(
                        Modifier.fillMaxWidth().clickable { confirmClear = true }.padding(vertical = 10.dp),
                    ) {
                        Text(
                            stringResource(R.string.settings_clear_history),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            SectionCard(title = stringResource(R.string.settings_about)) {
                Column {
                    Text(
                        stringResource(R.string.privacy_promise),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.size(8.dp))
                    NavRow(stringResource(R.string.about_title), onOpenAbout)
                }
            }
            Spacer(Modifier.size(24.dp))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            confirmButton = {
                TextButton(onClick = { vm.clearHistory(); confirmClear = false }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
            title = { Text(stringResource(R.string.settings_clear_history)) },
            text = { Text(stringResource(R.string.settings_clear_confirm)) },
        )
    }
}

@Composable
private fun ToggleRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun NavRow(title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Icon(Icons.Filled.ChevronRight, contentDescription = null)
    }
}
