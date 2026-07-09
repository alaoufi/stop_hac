package com.privacyshield.monitor.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.privacyshield.monitor.PrivacyMonitorApp
import com.privacyshield.monitor.monitor.MonitorService
import com.privacyshield.monitor.monitor.PeriodicScanWorker
import com.privacyshield.monitor.ui.theme.PrivacyShieldTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Single-activity host. Extends AppCompatActivity purely to get the per-app
 * language (Arabic/English) plumbing; the entire UI is Jetpack Compose.
 *
 * When app-lock is enabled, the whole UI is gated behind a biometric/PIN prompt,
 * re-locking every time the app leaves the foreground so no one can open it — or
 * tamper with protection settings — without the owner's identity.
 */
class MainActivity : AppCompatActivity() {

    private var unlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as PrivacyMonitorApp).container

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
                val lockActive = settings.appLockEnabled && BiometricGate.canAuthenticate(this) && !unlocked
                if (lockActive) {
                    LockScreen(onUnlock = { promptUnlock() })
                    LaunchedEffect(Unit) { promptUnlock() }
                } else {
                    PrivacyShieldApp(
                        container = container,
                        factory = AppViewModelFactory(container),
                    )
                }
            }
        }
    }

    private fun promptUnlock() {
        BiometricGate.authenticate(
            activity = this,
            title = getString(com.privacyshield.monitor.R.string.app_lock_title),
            subtitle = getString(com.privacyshield.monitor.R.string.app_lock_subtitle),
            onSuccess = { unlocked = true },
            onFailure = { /* stay locked; the user can retry from the lock screen */ },
            onWrongAttempt = { maybeCaptureIntruder() },
        )
    }

    /** On a failed unlock, photograph the intruder if the user enabled it. */
    private fun maybeCaptureIntruder() {
        val container = (application as PrivacyMonitorApp).container
        lifecycleScope.launch {
            val settings = container.settings.settings.first()
            if (settings.intruderPhotoEnabled &&
                com.privacyshield.monitor.monitor.IntruderCapture.hasCameraPermission(this@MainActivity)
            ) {
                com.privacyshield.monitor.monitor.IntruderCapture.capture(
                    this@MainActivity, this@MainActivity, System.currentTimeMillis(),
                )
            }
        }
    }

    /** Re-lock whenever the app leaves the foreground. */
    override fun onStop() {
        super.onStop()
        unlocked = false
    }
}
