package com.privacyshield.monitor.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.core.model.SecurityEvent
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.core.model.UserDecision

/**
 * Persisted form of a [SecurityEvent]. Indexed on the columns the event log
 * filters and sorts by so browsing large histories stays responsive.
 */
@Entity(
    tableName = "events",
    indices = [
        Index("startTimeMillis"),
        Index("packageName"),
        Index("sensor"),
        Index("riskLevel"),
    ],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val appLabel: String,
    val sensor: SensorType,
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    val foreground: Boolean,
    val screenOn: Boolean,
    val inCall: Boolean,
    val riskLevel: RiskLevel,
    val reasonKey: String,
    val reasonArgs: String,
    val decision: UserDecision,
    val isSystemApp: Boolean,
)

/** Delimiter for the small reason-argument list — chosen so labels won't contain it. */
private const val ARG_SEP = "<|arg|>"

fun List<String>.encodeArgs(): String = joinToString(ARG_SEP)
fun String.decodeArgs(): List<String> = if (isEmpty()) emptyList() else split(ARG_SEP)

fun EventEntity.toModel(): SecurityEvent = SecurityEvent(
    id = id,
    packageName = packageName,
    appLabel = appLabel,
    sensor = sensor,
    startTimeMillis = startTimeMillis,
    endTimeMillis = endTimeMillis,
    foreground = foreground,
    screenOn = screenOn,
    inCall = inCall,
    riskLevel = riskLevel,
    reasonKey = reasonKey,
    reasonArgs = reasonArgs.decodeArgs(),
    decision = decision,
    isSystemApp = isSystemApp,
)

fun SecurityEvent.toEntity(): EventEntity = EventEntity(
    id = id,
    packageName = packageName,
    appLabel = appLabel,
    sensor = sensor,
    startTimeMillis = startTimeMillis,
    endTimeMillis = endTimeMillis,
    foreground = foreground,
    screenOn = screenOn,
    inCall = inCall,
    riskLevel = riskLevel,
    reasonKey = reasonKey,
    reasonArgs = reasonArgs.encodeArgs(),
    decision = decision,
    isSystemApp = isSystemApp,
)
