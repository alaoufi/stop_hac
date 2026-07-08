package com.privacyshield.monitor.ui.apps

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.privacyshield.monitor.ui.components.AppActionsSheet
import com.privacyshield.monitor.ui.components.AppIcon
import com.privacyshield.monitor.ui.components.RiskBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(vm: AppsViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var includeSystem by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<AppEntry?>(null) }

    androidx.compose.runtime.LaunchedEffect(includeSystem) { vm.refresh(includeSystem) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.apps_title)) }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !includeSystem,
                    onClick = { includeSystem = false },
                    label = { Text(stringResource(R.string.apps_user_only)) },
                )
                FilterChip(
                    selected = includeSystem,
                    onClick = { includeSystem = true },
                    label = { Text(stringResource(R.string.apps_include_system)) },
                )
            }
            Text(
                stringResource(R.string.apps_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            HorizontalDivider()

            if (state.loading) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }
            } else {
                LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                    items(state.apps, key = { it.packageName }) { app ->
                        AppRow(app, onClick = { selected = app })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    selected?.let { app ->
        AppActionsSheet(
            packageName = app.packageName,
            appLabel = app.label,
            riskLevel = app.risk,
            detail = appDetailText(app),
            onDismiss = { selected = null },
        )
    }
}

@Composable
private fun AppRow(app: AppEntry, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AppIcon(app.packageName, Modifier.size(44.dp))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                RiskBadge(app.risk)
            }
            Text(
                whatItDoes(app),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(4.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RunStateChip(app)
                if (app.canCamera) CapabilityChip(stringResource(R.string.sensor_camera))
                if (app.canMic) CapabilityChip(stringResource(R.string.sensor_microphone))
                if (app.canLocation) CapabilityChip(stringResource(R.string.sensor_location))
            }
        }
    }
}

@Composable
private fun RunStateChip(app: AppEntry) {
    val (labelRes, active) = when {
        app.liveSensor != null -> R.string.apps_state_live to true
        app.runState == RunState.FOREGROUND -> R.string.status_in_use to true
        app.runState == RunState.RECENT_BACKGROUND -> R.string.status_background to false
        else -> R.string.apps_state_installed to false
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(stringResource(labelRes)) },
        colors = if (active) {
            AssistChipDefaults.assistChipColors(
                disabledLabelColor = MaterialTheme.colorScheme.error,
            )
        } else {
            AssistChipDefaults.assistChipColors()
        },
    )
}

@Composable
private fun CapabilityChip(label: String) {
    AssistChip(onClick = {}, enabled = false, label = { Text(label) })
}

@Composable
private fun whatItDoes(app: AppEntry): String {
    val category = categoryLabel(app.category)
    return if (category.isNotEmpty()) category else stringResource(R.string.apps_category_other)
}

@Composable
private fun categoryLabel(category: Int): String = stringResource(
    when (category) {
        ApplicationInfo.CATEGORY_GAME -> R.string.cat_game
        ApplicationInfo.CATEGORY_AUDIO -> R.string.cat_audio
        ApplicationInfo.CATEGORY_VIDEO -> R.string.cat_video
        ApplicationInfo.CATEGORY_IMAGE -> R.string.cat_image
        ApplicationInfo.CATEGORY_SOCIAL -> R.string.cat_social
        ApplicationInfo.CATEGORY_NEWS -> R.string.cat_news
        ApplicationInfo.CATEGORY_MAPS -> R.string.cat_maps
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> R.string.cat_productivity
        else -> R.string.apps_category_other
    },
)

/** Rich detail string for the actions sheet: what it does + what it accessed. */
@Composable
private fun appDetailText(app: AppEntry): String {
    val caps = buildList {
        if (app.canCamera) add(stringResource(R.string.sensor_camera))
        if (app.canMic) add(stringResource(R.string.sensor_microphone))
        if (app.canLocation) add(stringResource(R.string.sensor_location))
    }.joinToString("، ")
    val capsLine = if (caps.isNotEmpty()) {
        stringResource(R.string.apps_can_access, caps)
    } else {
        stringResource(R.string.apps_no_sensitive)
    }
    val bgLine = if (app.backgroundUses > 0) {
        " " + stringResource(R.string.apps_bg_uses, app.backgroundUses)
    } else {
        ""
    }
    return whatItDoes(app) + " · " + capsLine + bgLine
}
