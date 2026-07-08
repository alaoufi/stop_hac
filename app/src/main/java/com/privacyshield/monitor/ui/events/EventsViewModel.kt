package com.privacyshield.monitor.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.core.model.SecurityEvent
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.data.repo.DAY_MS
import com.privacyshield.monitor.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Time windows the log can be filtered to. */
enum class TimeWindow(val days: Int) { DAY(1), WEEK(7), MONTH(30), ALL(3650) }

data class EventsFilter(
    val sensor: SensorType? = null,
    val risk: RiskLevel? = null,
    val query: String = "",
    val window: TimeWindow = TimeWindow.WEEK,
)

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModel(private val container: AppContainer) : ViewModel() {

    private val _filter = MutableStateFlow(EventsFilter())
    val filter: StateFlow<EventsFilter> = _filter.asStateFlow()

    val events: StateFlow<List<SecurityEvent>> = _filter.flatMapLatest { f ->
        container.eventRepository.filter(
            sensor = f.sensor,
            risk = f.risk,
            query = f.query,
            sinceMillis = System.currentTimeMillis() - f.window.days * DAY_MS,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSensor(sensor: SensorType?) = _filter.update { it.copy(sensor = sensor) }
    fun setRisk(risk: RiskLevel?) = _filter.update { it.copy(risk = risk) }
    fun setQuery(query: String) = _filter.update { it.copy(query = query) }
    fun setWindow(window: TimeWindow) = _filter.update { it.copy(window = window) }

    fun clearAll() = viewModelScope.launch { container.eventRepository.clearAll() }

    /** Builds a CSV export of all events (used by the share action). */
    suspend fun exportCsv(): String = com.privacyshield.monitor.core.util.CsvExporter
        .toCsv(container.eventRepository.allForExport())
}
