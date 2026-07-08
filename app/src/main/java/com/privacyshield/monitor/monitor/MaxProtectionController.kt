package com.privacyshield.monitor.monitor

import android.content.Context
import android.content.Intent
import android.hardware.SensorPrivacyManager
import android.os.Build
import android.provider.Settings

/**
 * "وضع الحماية القصوى" (Maximum Protection).
 *
 * ### Honest scope
 * Android intentionally forbids a normal app from switching another app's
 * camera or microphone off. Only the OS-level sensor toggles (the global
 * camera/mic switches introduced in Android 12) can do that, and they are
 * driven by the user, not by third-party apps. So this controller does what a
 * privacy app *legitimately* can:
 *
 *  1. Raises monitoring sensitivity (every access, including "normal" ones, is
 *     surfaced) — handled by the settings flag the UI toggles.
 *  2. Detects whether the device even exposes hardware sensor toggles.
 *  3. Hands the user straight to the system control that actually cuts power to
 *     the sensors, and explains that clearly rather than pretending to do it
 *     silently.
 */
class MaxProtectionController(private val context: Context) {

    private val sensorPrivacy: SensorPrivacyManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SensorPrivacyManager::class.java)
        } else {
            null
        }

    /** True when the device has a hardware/software global sensor toggle. */
    fun deviceSupportsSensorToggle(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val spm = sensorPrivacy ?: return false
        return runCatching {
            spm.supportsSensorToggle(SensorPrivacyManager.Sensors.CAMERA) ||
                spm.supportsSensorToggle(SensorPrivacyManager.Sensors.MICROPHONE)
        }.getOrDefault(false)
    }

    /** Intent to the privacy controls where the user can cut the sensors. */
    fun sensorControlsIntent(): Intent =
        Intent(Settings.ACTION_PRIVACY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Intent to this app's own settings (to review granted access quickly). */
    fun appDetailsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
