package com.privacyshield.monitor.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Aggregated per-app usage counts, used by the reports screen. */
data class AppUsageCount(
    val packageName: String,
    val appLabel: String,
    val count: Int,
)

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Query("SELECT * FROM events ORDER BY startTimeMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<EventEntity>>

    @Query(
        """
        SELECT * FROM events
        WHERE (:sensor IS NULL OR sensor = :sensor)
          AND (:risk IS NULL OR riskLevel = :risk)
          AND (:query = '' OR appLabel LIKE '%' || :query || '%' OR packageName LIKE '%' || :query || '%')
          AND startTimeMillis >= :since
        ORDER BY startTimeMillis DESC
        LIMIT :limit
        """,
    )
    fun filter(
        sensor: String?,
        risk: String?,
        query: String,
        since: Long,
        limit: Int = 500,
    ): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE endTimeMillis IS NULL")
    suspend fun activeEvents(): List<EventEntity>

    @Query("SELECT * FROM events WHERE packageName = :pkg AND sensor = :sensor AND endTimeMillis IS NULL LIMIT 1")
    suspend fun activeFor(pkg: String, sensor: String): EventEntity?

    @Query(
        "SELECT COUNT(*) FROM events WHERE packageName = :pkg AND sensor = :sensor " +
            "AND foreground = 0 AND startTimeMillis >= :since",
    )
    suspend fun backgroundHitsSince(pkg: String, sensor: String, since: Long): Int

    @Query("SELECT riskLevel FROM events WHERE packageName = :pkg AND startTimeMillis >= :since")
    suspend fun riskLevelsForApp(pkg: String, since: Long): List<String>

    @Query(
        "SELECT COUNT(*) FROM events WHERE packageName = :pkg AND foreground = 0 " +
            "AND startTimeMillis >= :since",
    )
    suspend fun backgroundCountForApp(pkg: String, since: Long): Int

    @Query("SELECT COUNT(*) FROM events WHERE riskLevel = :risk AND startTimeMillis >= :since")
    fun countByRiskSince(risk: String, since: Long): Flow<Int>

    @Query(
        """
        SELECT packageName, appLabel, COUNT(*) AS count FROM events
        WHERE sensor = :sensor AND startTimeMillis >= :since
        GROUP BY packageName ORDER BY count DESC LIMIT :limit
        """,
    )
    suspend fun topAppsForSensor(sensor: String, since: Long, limit: Int = 5): List<AppUsageCount>

    @Query("SELECT COUNT(*) FROM events WHERE riskLevel = :risk AND startTimeMillis >= :since")
    suspend fun countByRisk(risk: String, since: Long): Int

    @Query("DELETE FROM events WHERE startTimeMillis < :before")
    suspend fun deleteOlderThan(before: Long): Int

    @Query("DELETE FROM events")
    suspend fun clear()

    @Query("SELECT * FROM events ORDER BY startTimeMillis DESC")
    suspend fun allForExport(): List<EventEntity>
}
