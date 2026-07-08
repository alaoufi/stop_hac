package com.privacyshield.monitor.monitor

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.LifecycleService
import com.privacyshield.monitor.PrivacyMonitorApp
import com.privacyshield.monitor.core.analysis.AccessContext
import com.privacyshield.monitor.core.analysis.RiskAnalyzer
import com.privacyshield.monitor.core.model.SecurityEvent
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.core.model.UserDecision
import com.privacyshield.monitor.di.AppContainer
import com.privacyshield.monitor.notify.Notifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * The always-on foreground service that powers monitoring. It wires the
 * [SensorAccessMonitor] callbacks into the [RiskAnalyzer], persists every access
 * as a [SecurityEvent], updates [MonitorState] for the live dashboard, and posts
 * alerts through the [Notifier].
 *
 * Battery-conscious by design: no polling loop — we react to the system's
 * AppOps callbacks and only touch the database on real transitions.
 */
class MonitorService : LifecycleService() {

    private lateinit var container: AppContainer
    private lateinit var notifier: Notifier
    private lateinit var detector: SensorAccessMonitor
    private lateinit var deviceState: DeviceState
    private val analyzer = RiskAnalyzer()
    private val callbackExecutor = Executors.newSingleThreadExecutor()
    private lateinit var overlay: OverlayIndicator

    override fun onCreate() {
        super.onCreate()
        container = (application as PrivacyMonitorApp).container
        notifier = Notifier(this)
        detector = SensorAccessMonitor(this)
        deviceState = DeviceState(this)
        overlay = OverlayIndicator(this)
        observeOverlay()
    }

    /** Drives the floating privacy dot from live sensor state + the user's toggle. */
    private fun observeOverlay() {
        lifecycleScope.launch {
            combine(
                MonitorState.active,
                container.settings.settings,
            ) { active, settings ->
                if (!settings.overlayIndicatorEnabled) emptySet()
                else active.values.map { it.sensor }
                    .filter { it == SensorType.CAMERA || it == SensorType.MICROPHONE }
                    .toSet()
            }.collect { sensors -> overlay.update(sensors) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            stopEverything()
            return START_NOT_STICKY
        }

        startInForeground()
        MonitorState.setRunning(true)
        MonitorState.setDetectorSupported(detector.isSupported)

        val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            detector.start(callbackExecutor) { change -> handleChange(change) }
        } else {
            false
        }
        MonitorState.setDetectorSupported(started)

        return START_STICKY
    }

    private fun startInForeground() {
        val note = notifier.buildServiceNotification(
            contentText = getString(com.privacyshield.monitor.R.string.notif_service_active),
            maxProtection = false,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                Notifier.SERVICE_NOTIFICATION_ID,
                note,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(Notifier.SERVICE_NOTIFICATION_ID, note)
        }
    }

    private fun handleChange(change: SensorAccessChange) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (change.active) onAccessStarted(change) else onAccessStopped(change)
        }
    }

    private suspend fun onAccessStarted(change: SensorAccessChange) {
        val appInfo = container.appRepository.info(change.packageName)
        val settings = container.settings.settings.first()

        // Skip our own accesses and, unless opted in, system apps.
        if (change.packageName == packageName) return

        val foreground = deviceState.isForeground(change.packageName) ?: false
        val screenOn = deviceState.isScreenOn()
        val inCall = deviceState.isInCall()
        val rule = container.whitelistRepository.ruleFor(change.packageName, change.sensor)

        val ctx = AccessContext(
            packageName = change.packageName,
            appLabel = appInfo.label,
            sensor = change.sensor,
            foreground = foreground,
            screenOn = screenOn,
            inCall = inCall,
            isSystemApp = appInfo.isSystemApp,
            whitelistScope = rule?.scope,
            durationMillis = null,
            backgroundHitsToday = container.eventRepository
                .backgroundHitsToday(change.packageName, change.sensor, System.currentTimeMillis()),
            shortlyAfterBoot = deviceState.bootedRecently(),
        )
        val verdict = analyzer.analyze(ctx)
        val now = System.currentTimeMillis()

        val event = SecurityEvent(
            packageName = change.packageName,
            appLabel = appInfo.label,
            sensor = change.sensor,
            startTimeMillis = now,
            endTimeMillis = null,
            foreground = foreground,
            screenOn = screenOn,
            inCall = inCall,
            riskLevel = verdict.level,
            reasonKey = verdict.reasonKey,
            reasonArgs = verdict.reasonArgs,
            decision = if (rule != null) UserDecision.WHITELISTED else UserDecision.NONE,
            isSystemApp = appInfo.isSystemApp,
        )
        val id = container.eventRepository.insert(event)

        MonitorState.onStart(
            ActiveUse(change.sensor, change.packageName, appInfo.label, now),
        )

        val notify = settings.notifyNormal || verdict.level.isElevated ||
            verdict.level == com.privacyshield.monitor.core.model.RiskLevel.ATTENTION
        if (notify) {
            notifier.notifyEvent(
                event.copy(id = id),
                reason = ReasonFormatter.format(this, verdict.reasonKey, verdict.reasonArgs),
            )
        }
    }

    private suspend fun onAccessStopped(change: SensorAccessChange) {
        MonitorState.onStop(change.sensor, change.packageName)
        val active = container.eventRepository.activeFor(change.packageName, change.sensor) ?: return
        val now = System.currentTimeMillis()

        // Re-evaluate with the now-known duration; long covert use may escalate.
        val appInfo = container.appRepository.info(change.packageName)
        val rule = container.whitelistRepository.ruleFor(change.packageName, change.sensor)
        val ctx = AccessContext(
            packageName = change.packageName,
            appLabel = appInfo.label,
            sensor = change.sensor,
            foreground = active.foreground,
            screenOn = active.screenOn,
            inCall = active.inCall,
            isSystemApp = appInfo.isSystemApp,
            whitelistScope = rule?.scope,
            durationMillis = now - active.startTimeMillis,
            backgroundHitsToday = 0,
            shortlyAfterBoot = false,
        )
        val verdict = analyzer.analyze(ctx)
        val updated = active.copy(
            endTimeMillis = now,
            riskLevel = maxOf(active.riskLevel, verdict.level, compareBy { it.weight }),
            reasonKey = if (verdict.level > active.riskLevel) verdict.reasonKey else active.reasonKey,
            reasonArgs = if (verdict.level > active.riskLevel) verdict.reasonArgs else active.reasonArgs,
        )
        container.eventRepository.update(updated)

        // If duration pushed it into elevated territory, alert now.
        if (verdict.level.isElevated && verdict.level > active.riskLevel) {
            notifier.notifyEvent(
                updated,
                reason = ReasonFormatter.format(this, verdict.reasonKey, verdict.reasonArgs),
            )
        }
    }

    private fun stopEverything() {
        detector.stop()
        overlay.hide()
        MonitorState.setRunning(false)
        MonitorState.reset()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        detector.stop()
        overlay.hide()
        callbackExecutor.shutdown()
        MonitorState.setRunning(false)
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.privacyshield.monitor.STOP"

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, MonitorService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
