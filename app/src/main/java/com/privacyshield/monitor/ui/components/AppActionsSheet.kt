package com.privacyshield.monitor.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.privacyshield.monitor.R
import com.privacyshield.monitor.core.model.SecurityEvent
import com.privacyshield.monitor.ui.rememberReason
import com.privacyshield.monitor.ui.sensorName

/**
 * Bottom sheet of everything the user can do about one flagged app: see its
 * risk level and the reason, then act — force-stop (via the OS App Info screen),
 * uninstall it (system dialog), or open its settings to revoke a permission.
 *
 * Honesty: Android does not let a normal app kill or delete another app
 * silently. We launch the official system flows; the user confirms. That is the
 * only correct — and safe — way to do this.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(event: SecurityEvent, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val pkg = event.packageName

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(pkg, Modifier.size(48.dp))
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        event.appLabel,
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

            // Risk level + the concrete reason.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.sheet_risk_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.size(8.dp))
                RiskBadge(event.riskLevel)
            }
            Text(
                "${sensorName(event.sensor)} · ${rememberReason(event)}",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.size(4.dp))

            // Primary destructive-ish actions.
            ActionButton(
                icon = Icons.Filled.Block,
                text = stringResource(R.string.sheet_force_stop),
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                context.startActivity(appDetails(pkg))
            }
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

private fun appDetails(pkg: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", pkg, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
