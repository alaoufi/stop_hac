package com.privacyshield.monitor.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.data.db.AppUsageCount
import com.privacyshield.monitor.data.repo.DAY_MS
import com.privacyshield.monitor.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Report period — weekly or monthly, per the spec. */
enum class ReportPeriod(val days: Int) { WEEK(7), MONTH(30) }

data class ReportData(
    val period: ReportPeriod = ReportPeriod.WEEK,
    val topCamera: List<AppUsageCount> = emptyList(),
    val topMicrophone: List<AppUsageCount> = emptyList(),
    val topLocation: List<AppUsageCount> = emptyList(),
    val normalCount: Int = 0,
    val attentionCount: Int = 0,
    val suspiciousCount: Int = 0,
    val criticalCount: Int = 0,
    /** Localisable recommendation keys derived from the data. */
    val recommendationKeys: List<String> = emptyList(),
    val loading: Boolean = true,
)

class ReportsViewModel(private val container: AppContainer) : ViewModel() {

    private val _data = MutableStateFlow(ReportData())
    val data: StateFlow<ReportData> = _data.asStateFlow()

    init {
        load(ReportPeriod.WEEK)
    }

    fun load(period: ReportPeriod) {
        viewModelScope.launch {
            _data.value = ReportData(period = period, loading = true)
            val since = System.currentTimeMillis() - period.days * DAY_MS
            val repo = container.eventRepository

            val camera = repo.topApps(SensorType.CAMERA, since)
            val mic = repo.topApps(SensorType.MICROPHONE, since)
            val location = repo.topApps(SensorType.LOCATION, since)
            val normal = repo.countByRisk(RiskLevel.NORMAL, since)
            val attention = repo.countByRisk(RiskLevel.ATTENTION, since)
            val suspicious = repo.countByRisk(RiskLevel.SUSPICIOUS, since)
            val critical = repo.countByRisk(RiskLevel.CRITICAL, since)

            _data.value = ReportData(
                period = period,
                topCamera = camera,
                topMicrophone = mic,
                topLocation = location,
                normalCount = normal,
                attentionCount = attention,
                suspiciousCount = suspicious,
                criticalCount = critical,
                recommendationKeys = recommend(critical, suspicious, mic, camera),
                loading = false,
            )
        }
    }

    private fun recommend(
        critical: Int,
        suspicious: Int,
        mic: List<AppUsageCount>,
        camera: List<AppUsageCount>,
    ): List<String> = buildList {
        if (critical > 0) add("rec_critical_present")
        if (suspicious > 0) add("rec_review_suspicious")
        if (mic.isNotEmpty()) add("rec_review_mic_top")
        if (camera.isNotEmpty()) add("rec_review_camera_top")
        add("rec_enable_max_protection")
        add("rec_periodic_review")
    }
}
