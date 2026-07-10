package com.privacyshield.monitor.di

import android.content.Context
import com.privacyshield.monitor.core.analysis.RiskAnalyzer
import com.privacyshield.monitor.data.db.AppDatabase
import com.privacyshield.monitor.data.prefs.SettingsRepository
import com.privacyshield.monitor.data.repo.AppRepository
import com.privacyshield.monitor.data.repo.EventRepository
import com.privacyshield.monitor.data.repo.WhitelistRepository

/**
 * Minimal hand-rolled dependency container. We keep DI explicit and free of
 * annotation processors so the build stays fast and the wiring is obvious —
 * everything the app needs is constructed lazily and shared process-wide.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val db by lazy { AppDatabase.get(appContext) }

    val settings by lazy { SettingsRepository(appContext) }
    val eventRepository by lazy { EventRepository(db.eventDao()) }
    val whitelistRepository by lazy { WhitelistRepository(db.whitelistDao()) }
    val appRepository by lazy { AppRepository(appContext) }
    val riskAnalyzer by lazy { RiskAnalyzer() }
}
