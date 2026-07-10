package com.privacyshield.monitor.data.repo

import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.core.model.WhitelistRule
import com.privacyshield.monitor.data.db.WhitelistDao
import com.privacyshield.monitor.data.db.toEntity
import com.privacyshield.monitor.data.db.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WhitelistRepository(private val dao: WhitelistDao) {

    val rules: Flow<List<WhitelistRule>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun upsert(rule: WhitelistRule) = dao.upsert(rule.toEntity())

    suspend fun remove(packageName: String, sensor: SensorType) =
        dao.delete(packageName, sensor.name)

    suspend fun ruleFor(packageName: String, sensor: SensorType): WhitelistRule? =
        dao.ruleFor(packageName, sensor.name)?.toModel()
}
