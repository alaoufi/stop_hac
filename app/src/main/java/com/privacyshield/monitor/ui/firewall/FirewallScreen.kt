package com.privacyshield.monitor.ui.firewall

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.privacyshield.monitor.ui.components.AppIcon
import com.privacyshield.monitor.ui.theme.RiskGreen
import com.privacyshield.monitor.ui.theme.RiskRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreen(vm: FirewallViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val enabled by vm.enabled.collectAsStateWithLifecycle()
    val active by vm.active.collectAsStateWithLifecycle()
    val blocked by vm.blocked.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    // The system VPN-consent dialog; on approval we enable + start the tunnel.
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.setEnabled(context, true)
    }

    LaunchedEffect(Unit) { vm.loadApps(includeSystem = false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.firewall_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = (if (active) RiskGreen else RiskRed).copy(alpha = 0.12f),
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.firewall_master),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                stringResource(
                                    if (active) R.string.firewall_active_count
                                    else R.string.firewall_off,
                                    blocked.size,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { on ->
                                if (on) {
                                    val prepare = VpnService.prepare(context)
                                    if (prepare != null) consentLauncher.launch(prepare)
                                    else vm.setEnabled(context, true)
                                } else {
                                    vm.setEnabled(context, false)
                                }
                            },
                        )
                    }
                    Spacer(Modifier.size(6.dp))
                    Text(
                        stringResource(R.string.firewall_explainer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.firewall_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Spacer(Modifier.size(8.dp))
            HorizontalDivider()

            LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                items(
                    apps.filter { it.label.contains(query, ignoreCase = true) },
                    key = { it.packageName },
                ) { app ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app.packageName, Modifier.size(40.dp))
                        Spacer(Modifier.size(12.dp))
                        Text(
                            app.label,
                            Modifier.weight(1f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                        Switch(
                            checked = app.packageName in blocked,
                            onCheckedChange = { block ->
                                vm.toggleApp(context, app.packageName, block)
                            },
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
