package com.privacyshield.monitor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.privacyshield.monitor.core.model.SecurityEvent
import com.privacyshield.monitor.ui.formatDuration
import com.privacyshield.monitor.ui.formatTime
import com.privacyshield.monitor.ui.rememberReason
import com.privacyshield.monitor.ui.sensorName

/**
 * A single event row: app icon, name + sensor + time, the plain-language reason,
 * and a risk badge. Reused by the dashboard's recent list and the full log.
 */
@Composable
fun EventRow(
    event: SecurityEvent,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AppIcon(event.packageName, modifier = Modifier.size(40.dp))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    event.appLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                RiskBadge(event.riskLevel)
            }
            Text(
                "${sensorName(event.sensor)} · ${formatTime(event.startTimeMillis)}" +
                    if (event.durationMillis != null) " · ${formatDuration(event.durationMillis)}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(2.dp))
            Text(
                rememberReason(event),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
