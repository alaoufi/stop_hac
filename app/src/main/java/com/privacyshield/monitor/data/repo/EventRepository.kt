package com.privacyshield.monitor.data.repo

import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.core.model.SecurityEvent
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.data.db.AppUsageCount
import com.privacyshield.monitor.data.db.EventDao
import com.privacyshield.monitor.data.db.toEntity
import com.privacyshield.monitor.data.db.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One day in millis — used for "today" and retention windows. */
const val DAY_MS = 24 * 60 * 60 * 1000L

class EventRepository(private val dao: EventDao) {

    fun observeRecent(limit: Int = 200): Flow<List<SecurityEvent>> =
        dao.observeRecent(limit).map { list -> list.map { it.toModel() } }

    fun filter(
        sensor: SensorType?,
        risk: RiskLevel?,
        query: String,
        sinceMillis: Long,
    ): Flow<List<SecurityEvent>> =
        dao.filter(sensor?.name, risk?.name, query, sinceMillis)
            .map { list -> list.map { it.toModel() } }

    suspend fun insert(event: SecurityEvent): Long = dao.insert(event.toEntity())

    suspend fun update(event: SecurityEvent) = dao.update(event.toEntity())

    suspend fun activeFor(packageName: String, sensor: SensorType): SecurityEvent? =
        dao.activeFor(packageName, sensor.name)?.toModel()

    suspend fun backgroundHitsToday(packageName: String, sensor: SensorType, now: Long): Int =
        dao.backgroundHitsSince(packageName, sensor.name, now - DAY_MS)

    fun countByRiskSince(risk: RiskLevel, sinceMillis: Long): Flow<Int> =
        dao.countByRiskSince(risk.name, sinceMillis)

    suspend fun countByRisk(risk: RiskLevel, sinceMillis: Long): Int =
        dao.countByRisk(risk.name, sinceMillis)

    suspend fun topApps(sensor: SensorType, sinceMillis: Long, limit: Int = 5): List<AppUsageCount> =
        dao.topAppsForSensor(sensor.name, sinceMillis, limit)

    suspend fun allForExport(): List<SecurityEvent> = dao.allForExport().map { it.toModel() }

    suspend fun applyRetention(retentionDays: Int, now: Long): Int =
        dao.deleteOlderThan(now - retentionDays * DAY_MS)

    suspend fun clearAll() = dao.clear()
}
