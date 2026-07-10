package com.privacyshield.monitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.privacyshield.monitor.R
import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.ui.theme.riskColorsFor

@Composable
fun riskLabel(level: RiskLevel): String {
    val res = when (level) {
        RiskLevel.NORMAL -> R.string.risk_normal
        RiskLevel.ATTENTION -> R.string.risk_attention
        RiskLevel.SUSPICIOUS -> R.string.risk_suspicious
        RiskLevel.CRITICAL -> R.string.risk_critical
    }
    return androidx.compose.ui.res.stringResource(res)
}

fun riskEmoji(level: RiskLevel): String = when (level) {
    RiskLevel.NORMAL -> "🟢"
    RiskLevel.ATTENTION -> "🟡"
    RiskLevel.SUSPICIOUS -> "🟠"
    RiskLevel.CRITICAL -> "🔴"
}

/** A coloured pill showing the risk level with its emoji and localised label. */
@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    val colors = riskColorsFor(level)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = riskEmoji(level))
        Text(
            text = "  ${riskLabel(level)}",
            color = colors.accent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** A small pulsing-style dot indicating whether something is currently active. */
@Composable
fun StatusDot(active: Boolean, modifier: Modifier = Modifier) {
    val color = if (active) riskColorsFor(RiskLevel.CRITICAL).accent
    else MaterialTheme.colorScheme.outline
    Box(
        modifier
            .clip(CircleShape)
            .background(color)
    )
}
