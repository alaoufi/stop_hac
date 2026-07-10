package com.privacyshield.monitor

import android.app.Application
import com.privacyshield.monitor.di.AppContainer
import com.privacyshield.monitor.notify.Notifier

/**
 * Application entry point. Owns the process-wide [AppContainer] and registers
 * notification channels once at startup.
 */
class PrivacyMonitorApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier(this).createChannels()
    }
}
