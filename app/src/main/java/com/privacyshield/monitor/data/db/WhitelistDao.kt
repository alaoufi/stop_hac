package com.privacyshield.monitor.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: WhitelistEntity)

    @Query("DELETE FROM whitelist WHERE packageName = :pkg AND sensor = :sensor")
    suspend fun delete(pkg: String, sensor: String)

    @Query("SELECT * FROM whitelist ORDER BY updatedAtMillis DESC")
    fun observeAll(): Flow<List<WhitelistEntity>>

    @Query("SELECT * FROM whitelist WHERE packageName = :pkg AND sensor = :sensor LIMIT 1")
    suspend fun ruleFor(pkg: String, sensor: String): WhitelistEntity?
}
