package com.privacyshield.monitor.ui.events

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privacyshield.monitor.R
import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.ui.components.AppActionsSheet
import com.privacyshield.monitor.ui.components.EventRow
import com.privacyshield.monitor.ui.components.riskLabel
import com.privacyshield.monitor.ui.sensorName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(vm: EventsViewModel) {
    val events by vm.events.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<com.privacyshield.monitor.core.model.SecurityEvent?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.events_title)) },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val csv = vm.exportCsv()
                            val file = withContext(Dispatchers.IO) {
                                File(context.cacheDir, "privacy_events.csv").apply { writeText(csv) }
                            }
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file,
                            )
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(share, context.getString(R.string.events_export)),
                            )
                        }
                    }) { Icon(Icons.Filled.Share, stringResource(R.string.events_export)) }
                    IconButton(onClick = { vm.clearAll() }) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.events_clear))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = vm::setQuery,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.events_search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Time window chips
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimeWindow.entries.forEach { w ->
                    FilterChip(
                        selected = filter.window == w,
                        onClick = { vm.setWindow(w) },
                        label = { Text(windowLabel(w)) },
                    )
                }
            }
            Spacer(Modifier.size(6.dp))

            // Sensor + risk chips
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filter.sensor == null,
                    onClick = { vm.setSensor(null) },
                    label = { Text(stringResource(R.string.filter_all)) },
                )
                SensorType.entries.forEach { s ->
                    FilterChip(
                        selected = filter.sensor == s,
                        onClick = { vm.setSensor(if (filter.sensor == s) null else s) },
                        label = { Text(sensorName(s)) },
                    )
                }
            }
            Spacer(Modifier.size(6.dp))
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RiskLevel.entries.forEach { r ->
                    FilterChip(
                        selected = filter.risk == r,
                        onClick = { vm.setRisk(if (filter.risk == r) null else r) },
                        label = { Text(riskLabel(r)) },
                    )
                }
            }

            HorizontalDivider(Modifier.padding(top = 8.dp))

            if (events.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        stringResource(R.string.events_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                    items(events, key = { it.id }) { event ->
                        EventRow(event, onClick = { selected = event })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    selected?.let { event ->
        AppActionsSheet(event = event, onDismiss = { selected = null })
    }
}

@Composable
private fun windowLabel(w: TimeWindow): String = stringResource(
    when (w) {
        TimeWindow.DAY -> R.string.window_day
        TimeWindow.WEEK -> R.string.window_week
        TimeWindow.MONTH -> R.string.window_month
        TimeWindow.ALL -> R.string.window_all
    },
)
