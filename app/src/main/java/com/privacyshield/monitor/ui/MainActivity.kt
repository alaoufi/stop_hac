package com.privacyshield.monitor.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.privacyshield.monitor.PrivacyMonitorApp
import com.privacyshield.monitor.data.prefs.ThemeMode
import com.privacyshield.monitor.monitor.MonitorService
import com.privacyshield.monitor.monitor.PeriodicScanWorker
import com.privacyshield.monitor.ui.theme.PrivacyShieldTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Single-activity host. Extends AppCompatActivity purely to get the per-app
 * language (Arabic/English) plumbing; the entire UI is Jetpack Compose.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as PrivacyMonitorApp).container

        // Honour the user's monitoring preference at launch and arm the scan.
        lifecycleScope.launch {
            val settings = container.settings.settings.first()
            if (settings.monitoringEnabled) MonitorService.start(this@MainActivity)
            PeriodicScanWorker.schedule(this@MainActivity)
        }

        setContent {
            val settings by container.settings.settings.collectAsState(
                initial = com.privacyshield.monitor.data.prefs.AppSettings(),
            )
            PrivacyShieldTheme(themeMode = settings.themeMode) {
                PrivacyShieldApp(
                    container = container,
                    factory = AppViewModelFactory(container),
                )
            }
        }
    }
}
