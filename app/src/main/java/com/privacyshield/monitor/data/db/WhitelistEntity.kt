package com.privacyshield.monitor.data.db

import androidx.room.Entity
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.core.model.WhitelistRule
import com.privacyshield.monitor.core.model.WhitelistScope

/**
 * A user-authored exception. Keyed by (packageName, sensor) so each app can be
 * governed independently per sensor.
 */
@Entity(tableName = "whitelist", primaryKeys = ["packageName", "sensor"])
data class WhitelistEntity(
    val packageName: String,
    val sensor: SensorType,
    val scope: WhitelistScope,
    val note: String,
    val updatedAtMillis: Long,
)

fun WhitelistEntity.toModel(): WhitelistRule = WhitelistRule(
    packageName = packageName,
    sensor = sensor,
    scope = scope,
    note = note,
    updatedAtMillis = updatedAtMillis,
)

fun WhitelistRule.toEntity(): WhitelistEntity = WhitelistEntity(
    packageName = packageName,
    sensor = sensor,
    scope = scope,
    note = note,
    updatedAtMillis = updatedAtMillis,
)
