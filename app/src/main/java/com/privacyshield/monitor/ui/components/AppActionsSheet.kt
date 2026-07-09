package com.privacyshield.monitor.ui.components

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.privacyshield.monitor.R
import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.core.model.TrackedPermission
import com.privacyshield.monitor.monitor.RootShell
import kotlinx.coroutines.launch

/**
 * Bottom sheet of everything the user can do about one flagged app: see its
 * risk level and a short explanation, control its sensitive permissions, then
 * force-stop or uninstall it.
 *
 * ### Honesty about permission control
 * Android does not let an ordinary app grant or revoke another app's runtime
 * permissions. So this sheet does the real thing where the platform allows it —
 * on a **rooted** device it runs `pm grant` / `pm revoke` directly — and on a
 * normal device it deep-links the user straight into the system permission
 * screen, telling them plainly that they confirm it there. It never pretends to
 * toggle a permission it cannot actually change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    packageName: String,
    appLabel: String,
    riskLevel: RiskLevel,
    detail: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pkg = packageName
    val rootPresent = remember { RootShell.isRootBinaryPresent() }
    val container = remember {
        (context.applicationContext as com.privacyshield.monitor.PrivacyMonitorApp).container
    }
    val lockedSet by container.settings.lockedPermissions.collectAsState(initial = emptySet())

    // Bumping this re-reads the granted state after a root grant/revoke.
    var refreshTick by remember { mutableIntStateOf(0) }
    val perms = remember(pkg, refreshTick) { controllablePermissions(context, pkg) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(pkg, Modifier.size(48.dp))
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        appLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        pkg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.sheet_risk_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.size(8.dp))
                RiskBadge(riskLevel)
            }
            Text(detail, style = MaterialTheme.typography.bodyMedium)

            // ---- Permission control ----
            if (perms.isNotEmpty()) {
                Spacer(Modifier.size(4.dp))
                Text(
                    stringResource(R.string.sheet_permissions),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(
                        if (rootPresent) R.string.sheet_perm_root_mode
                        else R.string.sheet_perm_settings_mode,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                perms.forEach { p ->
                    val manifest = p.permission.manifestPermission
                    val locked = manifest != null && "$pkg|$manifest" in lockedSet
                    PermissionRow(
                        label = stringResource(p.labelRes),
                        granted = p.granted,
                        locked = locked,
                        onLockToggle = {
                            manifest ?: return@PermissionRow
                            scope.launch {
                                container.settings.setPermissionLocked(pkg, manifest, !locked)
                                if (!locked) {
                                    // Newly locked: enforce it now.
                                    if (rootPresent) {
                                        RootShell.revoke(pkg, manifest)
                                        refreshTick++
                                    } else {
                                        context.startActivity(appDetails(pkg))
                                    }
                                }
                            }
                        },
                        onToggle = {
                            if (rootPresent) {
                                scope.launch {
                                    val outcome = if (p.granted) {
                                        RootShell.revoke(pkg, p.permission.manifestPermission!!)
                                    } else {
                                        RootShell.grant(pkg, p.permission.manifestPermission!!)
                                    }
                                    val msg = when (outcome) {
                                        RootShell.Outcome.Success -> context.getString(R.string.sheet_perm_done)
                                        RootShell.Outcome.NoRoot -> context.getString(R.string.sheet_perm_no_root)
                                        is RootShell.Outcome.Failed ->
                                            context.getString(R.string.sheet_perm_failed, outcome.message)
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    refreshTick++
                                }
                            } else {
                                context.startActivity(appDetails(pkg))
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.sheet_perm_confirm_in_settings),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.size(4.dp))

            // ---- App-level actions ----
            ActionButton(
                icon = Icons.Filled.Block,
                text = stringResource(R.string.sheet_force_stop),
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
            ) { context.startActivity(appDetails(pkg)) }
            Text(
                stringResource(R.string.sheet_force_stop_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ActionButton(
                icon = Icons.Filled.DeleteForever,
                text = stringResource(R.string.sheet_uninstall),
                container = MaterialTheme.colorScheme.error,
                content = MaterialTheme.colorScheme.onError,
            ) {
                context.startActivity(
                    Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }

            OutlinedButton(
                onClick = { context.startActivity(appDetails(pkg)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.sheet_app_info))
            }
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    locked: Boolean,
    onLockToggle: () -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (granted) Icons.Filled.CheckCircle else Icons.Filled.Block,
            contentDescription = null,
            tint = if (granted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.IconButton(onClick = onLockToggle) {
            Icon(
                if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = stringResource(R.string.perm_keep_denied),
                tint = if (locked) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (granted) {
            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) { Text(stringResource(R.string.sheet_perm_revoke)) }
        } else {
            FilledTonalButton(onClick = onToggle) {
                Text(stringResource(R.string.sheet_perm_grant))
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text(text)
        Spacer(Modifier.size(6.dp))
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
    }
}

/** A sensitive permission we can surface a grant/revoke control for. */
private data class ControllablePermission(
    val permission: TrackedPermission,
    val labelRes: Int,
    val granted: Boolean,
)

private fun controllablePermissions(
    context: android.content.Context,
    pkg: String,
): List<ControllablePermission> {
    val pm = context.packageManager
    val candidates = listOf(
        TrackedPermission.CAMERA to R.string.perm_camera,
        TrackedPermission.MICROPHONE to R.string.perm_microphone,
        TrackedPermission.FINE_LOCATION to R.string.perm_location,
        TrackedPermission.READ_CONTACTS to R.string.perm_contacts,
        TrackedPermission.READ_SMS to R.string.perm_sms,
    )
    // Only surface permissions the app actually declares in its manifest.
    val requested = runCatching {
        pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS).requestedPermissions?.toSet()
    }.getOrNull() ?: emptySet()

    return candidates.mapNotNull { (perm, labelRes) ->
        val manifest = perm.manifestPermission ?: return@mapNotNull null
        if (manifest !in requested) return@mapNotNull null
        val granted = pm.checkPermission(manifest, pkg) == PackageManager.PERMISSION_GRANTED
        ControllablePermission(perm, labelRes, granted)
    }
}

private fun appDetails(pkg: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", pkg, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
