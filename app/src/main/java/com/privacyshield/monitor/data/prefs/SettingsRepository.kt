package com.privacyshield.monitor.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** User-selected UI theme. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** User-selected app language. */
enum class AppLanguage(val tag: String) { SYSTEM(""), ARABIC("ar"), ENGLISH("en") }

data class AppSettings(
    val monitoringEnabled: Boolean = true,
    val startOnBoot: Boolean = true,
    val maxProtectionEnabled: Boolean = false,
    val overlayIndicatorEnabled: Boolean = false,
    val notifyNormal: Boolean = false,
    val includeSystemApps: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    /** Retention window for the event log, in days. */
    val retentionDays: Int = 30,
    /** True once the user has completed first-run onboarding. */
    val onboarded: Boolean = false,
)

/**
 * Thin, typed wrapper over Preferences DataStore. Holds only lightweight
 * user preferences; the heavier event/whitelist data lives in Room.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val MONITORING = booleanPreferencesKey("monitoring_enabled")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val MAX_PROTECTION = booleanPreferencesKey("max_protection")
        val OVERLAY_INDICATOR = booleanPreferencesKey("overlay_indicator")
        val NOTIFY_NORMAL = booleanPreferencesKey("notify_normal")
        val INCLUDE_SYSTEM = booleanPreferencesKey("include_system")
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val RETENTION = stringPreferencesKey("retention_days")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            monitoringEnabled = p[Keys.MONITORING] ?: true,
            startOnBoot = p[Keys.START_ON_BOOT] ?: true,
            maxProtectionEnabled = p[Keys.MAX_PROTECTION] ?: false,
            overlayIndicatorEnabled = p[Keys.OVERLAY_INDICATOR] ?: false,
            notifyNormal = p[Keys.NOTIFY_NORMAL] ?: false,
            includeSystemApps = p[Keys.INCLUDE_SYSTEM] ?: false,
            themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            language = p[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.SYSTEM,
            retentionDays = p[Keys.RETENTION]?.toIntOrNull() ?: 30,
            onboarded = p[Keys.ONBOARDED] ?: false,
        )
    }

    suspend fun setMonitoring(enabled: Boolean) = edit { it[Keys.MONITORING] = enabled }
    suspend fun setStartOnBoot(enabled: Boolean) = edit { it[Keys.START_ON_BOOT] = enabled }
    suspend fun setMaxProtection(enabled: Boolean) = edit { it[Keys.MAX_PROTECTION] = enabled }
    suspend fun setOverlayIndicator(enabled: Boolean) = edit { it[Keys.OVERLAY_INDICATOR] = enabled }
    suspend fun setNotifyNormal(enabled: Boolean) = edit { it[Keys.NOTIFY_NORMAL] = enabled }
    suspend fun setIncludeSystemApps(enabled: Boolean) = edit { it[Keys.INCLUDE_SYSTEM] = enabled }
    suspend fun setTheme(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }
    suspend fun setLanguage(lang: AppLanguage) = edit { it[Keys.LANGUAGE] = lang.name }
    suspend fun setRetentionDays(days: Int) = edit { it[Keys.RETENTION] = days.toString() }
    suspend fun setOnboarded(value: Boolean) = edit { it[Keys.ONBOARDED] = value }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
