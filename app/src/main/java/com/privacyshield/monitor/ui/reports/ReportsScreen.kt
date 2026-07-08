package com.privacyshield.monitor.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privacyshield.monitor.R
import com.privacyshield.monitor.data.db.AppUsageCount
import com.privacyshield.monitor.ui.components.BarDatum
import com.privacyshield.monitor.ui.components.DonutChart
import com.privacyshield.monitor.ui.components.DonutSlice
import com.privacyshield.monitor.ui.components.HorizontalBarChart
import com.privacyshield.monitor.ui.components.SectionCard
import com.privacyshield.monitor.ui.theme.RiskAmber
import com.privacyshield.monitor.ui.theme.RiskGreen
import com.privacyshield.monitor.ui.theme.RiskOrange
import com.privacyshield.monitor.ui.theme.RiskRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(vm: ReportsViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.reports_title)) }) },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(Modifier.size(2.dp)) }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ReportPeriod.entries.forEachIndexed { index, period ->
                        SegmentedButton(
                            selected = data.period == period,
                            onClick = { vm.load(period) },
                            shape = SegmentedButtonDefaults.itemShape(index, ReportPeriod.entries.size),
                        ) {
                            Text(
                                stringResource(
                                    if (period == ReportPeriod.WEEK) R.string.report_weekly
                                    else R.string.report_monthly,
                                ),
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = stringResource(R.string.report_risk_distribution)) {
                    val total = data.normalCount + data.attentionCount +
                        data.suspiciousCount + data.criticalCount
                    if (total == 0) {
                        Text(
                            stringResource(R.string.report_no_data),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        DonutChart(
                            slices = listOf(
                                DonutSlice(data.normalCount, RiskGreen, stringResource(R.string.risk_normal)),
                                DonutSlice(data.attentionCount, RiskAmber, stringResource(R.string.risk_attention)),
                                DonutSlice(data.suspiciousCount, RiskOrange, stringResource(R.string.risk_suspicious)),
                                DonutSlice(data.criticalCount, RiskRed, stringResource(R.string.risk_critical)),
                            ),
                            centerLabel = total.toString(),
                        )
                    }
                }
            }

            item {
                TopAppsCard(
                    title = stringResource(R.string.report_top_camera),
                    apps = data.topCamera,
                    color = RiskOrange,
                )
            }
            item {
                TopAppsCard(
                    title = stringResource(R.string.report_top_mic),
                    apps = data.topMicrophone,
                    color = RiskRed,
                )
            }
            item {
                TopAppsCard(
                    title = stringResource(R.string.report_top_location),
                    apps = data.topLocation,
                    color = RiskAmber,
                )
            }

            item {
                SectionCard(title = stringResource(R.string.report_recommendations)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        data.recommendationKeys.forEach { key ->
                            Text("•  ${recommendationText(key)}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item { Spacer(Modifier.size(24.dp)) }
        }
    }
}

@Composable
private fun TopAppsCard(title: String, apps: List<AppUsageCount>, color: androidx.compose.ui.graphics.Color) {
    SectionCard(title = title) {
        if (apps.isEmpty()) {
            Text(
                stringResource(R.string.report_no_data),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            HorizontalBarChart(
                data = apps.map { BarDatum(it.appLabel, it.count, color) },
            )
        }
    }
}

@Composable
private fun recommendationText(key: String): String = stringResource(
    when (key) {
        "rec_critical_present" -> R.string.rec_critical_present
        "rec_review_suspicious" -> R.string.rec_review_suspicious
        "rec_review_mic_top" -> R.string.rec_review_mic_top
        "rec_review_camera_top" -> R.string.rec_review_camera_top
        "rec_enable_max_protection" -> R.string.rec_enable_max_protection
        else -> R.string.rec_periodic_review
    },
)
