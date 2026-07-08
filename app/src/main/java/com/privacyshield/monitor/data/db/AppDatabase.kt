package com.privacyshield.monitor.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The single local database. All privacy data lives here on the device and is
 * never uploaded anywhere — there is no network code in this app.
 */
@Database(
    entities = [EventEntity::class, WhitelistEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun whitelistDao(): WhitelistDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "privacy_shield.db",
            ).build().also { instance = it }
        }
    }
}
