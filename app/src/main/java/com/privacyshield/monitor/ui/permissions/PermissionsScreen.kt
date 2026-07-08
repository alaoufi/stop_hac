package com.privacyshield.monitor.ui.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.privacyshield.monitor.core.model.SpecialAccessApp
import com.privacyshield.monitor.core.model.SpecialAccessType
import com.privacyshield.monitor.core.model.TrackedPermission
import com.privacyshield.monitor.ui.components.AppIcon
import com.privacyshield.monitor.ui.components.SectionCard
import com.privacyshield.monitor.ui.theme.RiskRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(vm: PermissionsViewModel) {
    val groups by vm.groups.collectAsStateWithLifecycle()
    val specialAccess by vm.specialAccess.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.refresh(includeSystem = false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.permissions_title)) }) },
    ) { padding ->
        if (loading) {
            Column(
                Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                Modifier.padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SpecialAccessCard(specialAccess)
                }
                item {
                    Text(
                        stringResource(R.string.permissions_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(groups, key = { it.permission.name }) { group ->
                    PermissionGroupCard(group)
                }
                item { Spacer(Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
private fun SpecialAccessCard(apps: List<SpecialAccessApp>) {
    val context = LocalContext.current
    SectionCard(title = stringResource(R.string.permissions_special_title)) {
        Text(
            stringResource(R.string.permissions_special_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(8.dp))
        if (apps.isEmpty()) {
            Text(stringResource(R.string.sa_none), style = MaterialTheme.typography.bodyMedium)
        } else {
            Column {
                apps.forEach { sa ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", sa.packageName, null)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    },
                                )
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(sa.packageName, Modifier.size(36.dp))
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sa.label, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(
                                specialAccessRisk(sa.type),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            specialAccessLabel(sa.type),
                            style = MaterialTheme.typography.labelMedium,
                            color = RiskRed,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun specialAccessLabel(type: SpecialAccessType): String = stringResource(
    when (type) {
        SpecialAccessType.ACCESSIBILITY -> R.string.sa_accessibility
        SpecialAccessType.NOTIFICATION_LISTENER -> R.string.sa_notification_listener
        SpecialAccessType.DEVICE_ADMIN -> R.string.sa_device_admin
    },
)

@Composable
private fun specialAccessRisk(type: SpecialAccessType): String = stringResource(
    when (type) {
        SpecialAccessType.ACCESSIBILITY -> R.string.sa_accessibility_risk
        SpecialAccessType.NOTIFICATION_LISTENER -> R.string.sa_notification_risk
        SpecialAccessType.DEVICE_ADMIN -> R.string.sa_device_admin_risk
    },
)

@Composable
private fun PermissionGroupCard(group: PermissionGroup) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    SectionCard(
        title = "${permissionName(group.permission)} (${group.apps.size})",
        trailing = {
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.clickable { expanded = !expanded },
            )
        },
    ) {
        Text(
            permissionRisk(group.permission),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (expanded) {
            Spacer(Modifier.size(8.dp))
            Column {
                group.apps.forEach { app ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", app.packageName, null)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    },
                                )
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app.packageName, Modifier.size(32.dp))
                        Spacer(Modifier.size(12.dp))
                        Text(app.label, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun permissionName(p: TrackedPermission): String = stringResource(
    when (p) {
        TrackedPermission.CAMERA -> R.string.perm_camera
        TrackedPermission.MICROPHONE -> R.string.perm_microphone
        TrackedPermission.FINE_LOCATION -> R.string.perm_location
        TrackedPermission.BACKGROUND_LOCATION -> R.string.perm_bg_location
        TrackedPermission.NOTIFICATIONS -> R.string.perm_notifications
        TrackedPermission.READ_CONTACTS -> R.string.perm_contacts
        TrackedPermission.READ_SMS -> R.string.perm_sms
        TrackedPermission.READ_PHONE_STATE -> R.string.perm_phone
        TrackedPermission.ACCESSIBILITY -> R.string.perm_accessibility
        TrackedPermission.OVERLAY -> R.string.perm_overlay
        TrackedPermission.INSTALL_PACKAGES -> R.string.perm_install
        TrackedPermission.MANAGE_STORAGE -> R.string.perm_storage
        TrackedPermission.USAGE_ACCESS -> R.string.perm_usage
    },
)

@Composable
private fun permissionRisk(p: TrackedPermission): String = stringResource(
    when (p) {
        TrackedPermission.CAMERA -> R.string.perm_camera_risk
        TrackedPermission.MICROPHONE -> R.string.perm_microphone_risk
        TrackedPermission.FINE_LOCATION, TrackedPermission.BACKGROUND_LOCATION -> R.string.perm_location_risk
        TrackedPermission.READ_CONTACTS -> R.string.perm_contacts_risk
        TrackedPermission.READ_SMS -> R.string.perm_sms_risk
        TrackedPermission.ACCESSIBILITY -> R.string.perm_accessibility_risk
        TrackedPermission.OVERLAY -> R.string.perm_overlay_risk
        TrackedPermission.INSTALL_PACKAGES -> R.string.perm_install_risk
        TrackedPermission.MANAGE_STORAGE -> R.string.perm_storage_risk
        else -> R.string.perm_generic_risk
    },
)
