package com.privacyshield.monitor.ui.whitelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privacyshield.monitor.R
import com.privacyshield.monitor.core.model.AppInfo
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.core.model.WhitelistRule
import com.privacyshield.monitor.core.model.WhitelistScope
import com.privacyshield.monitor.ui.components.AppIcon
import com.privacyshield.monitor.ui.sensorName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(vm: WhitelistViewModel, onBack: () -> Unit) {
    val rules by vm.rules.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadApps(includeSystem = false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.whitelist_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, stringResource(R.string.whitelist_add))
            }
        },
    ) { padding ->
        if (rules.isEmpty()) {
            Column(
                Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.whitelist_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.whitelist_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(rules, key = { "${it.packageName}:${it.sensor}" }) { rule ->
                    RuleRow(rule, vm)
                }
            }
        }
    }

    if (showAdd) {
        AddRuleDialog(vm = vm, onDismiss = { showAdd = false })
    }
}

@Composable
private fun RuleRow(rule: WhitelistRule, vm: WhitelistViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(rule.packageName, Modifier.size(40.dp))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(vm.appLabel(rule.packageName), fontWeight = FontWeight.SemiBold)
            Text(
                "${sensorName(rule.sensor)} · ${scopeLabel(rule.scope)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { vm.removeRule(rule) }) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_remove))
        }
    }
}

@Composable
private fun AddRuleDialog(vm: WhitelistViewModel, onDismiss: () -> Unit) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var sensor by remember { mutableStateOf(SensorType.CAMERA) }
    var scope by remember { mutableStateOf(WhitelistScope.ALWAYS_ALLOW) }
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = selectedApp != null,
                onClick = {
                    selectedApp?.let { vm.addRule(it.packageName, sensor, scope) }
                    onDismiss()
                },
            ) { Text(stringResource(R.string.whitelist_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        title = { Text(stringResource(R.string.whitelist_add)) },
        text = {
            Column {
                if (selectedApp == null) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        placeholder = { Text(stringResource(R.string.whitelist_pick_app)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.size(8.dp))
                    LazyColumn(Modifier.heightIn(max = 280.dp)) {
                        items(
                            apps.filter { it.label.contains(query, ignoreCase = true) }.take(60),
                            key = { it.packageName },
                        ) { app ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedApp = app }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIcon(app.packageName, Modifier.size(32.dp))
                                Spacer(Modifier.size(12.dp))
                                Text(app.label)
                            }
                        }
                    }
                } else {
                    Text(selectedApp!!.label, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { selectedApp = null }) {
                        Text(stringResource(R.string.whitelist_change_app))
                    }
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.whitelist_sensor), style = MaterialTheme.typography.labelLarge)
                    SensorType.entries.forEach { s ->
                        RadioRow(sensorName(s), sensor == s) { sensor = s }
                    }
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.whitelist_scope), style = MaterialTheme.typography.labelLarge)
                    WhitelistScope.entries.forEach { sc ->
                        RadioRow(scopeLabel(sc), scope == sc) { scope = sc }
                    }
                }
            }
        },
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onSelect() }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label)
    }
}

@Composable
private fun scopeLabel(scope: WhitelistScope): String = stringResource(
    when (scope) {
        WhitelistScope.ALWAYS_ALLOW -> R.string.scope_always_allow
        WhitelistScope.FOREGROUND_ONLY -> R.string.scope_foreground
        WhitelistScope.SCREEN_ON_ONLY -> R.string.scope_screen_on
        WhitelistScope.BLOCK_BACKGROUND -> R.string.scope_block_bg
        WhitelistScope.ALWAYS_BLOCK -> R.string.scope_always_block
    },
)
