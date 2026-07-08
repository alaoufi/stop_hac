package com.privacyshield.monitor.monitor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.privacyshield.monitor.data.repo.AppRepository

/**
 * Implements the dashboard "force block camera & microphone" switch.
 *
 * ### What "forcibly" honestly means on Android
 * A normal app cannot cut another app off from the sensors. Two real
 * enforcement paths exist, and this controller uses whichever the device
 * offers:
 *
 *  - **Rooted device** → we deny the CAMERA and RECORD_AUDIO app-ops for every
 *    (non-system) app via `appops ... ignore`. While the block is on, those
 *    apps get a black camera and a silent mic — a genuine, enforced block that
 *    persists until the user lifts it. Unblocking resets the ops to `default`.
 *  - **Non-rooted device** → the only true kill switch is the OS global sensor
 *    toggle (Android 12+). We open it for the user (who flips it) and keep our
 *    own "blocked" flag so monitoring stays aggressive. We never claim to have
 *    silently disabled the sensors ourselves when we haven't.
 */
class ForceBlockController(
    private val context: Context,
    private val appRepository: AppRepository,
) {

    sealed interface Result {
        /** Root enforced the block/unblock on [affected] apps. */
        data class Enforced(val affected: Int) : Result
        /** No root: we opened the system sensor toggle for the user to flip. */
        data object OpenedSystemToggle : Result
        data class Failed(val message: String) : Result
    }

    val rootAvailable: Boolean get() = RootShell.isRootBinaryPresent()

    suspend fun block(): Result = apply(op = "ignore")

    suspend fun unblock(): Result = apply(op = "default")

    private suspend fun apply(op: String): Result {
        if (!RootShell.isRootBinaryPresent()) {
            return Result.OpenedSystemToggle
        }
        val targets = targetPackages()
        if (targets.isEmpty()) return Result.Enforced(0)

        val script = buildString {
            targets.forEach { pkg ->
                append("cmd appops set ").append(pkg).append(" CAMERA ").append(op).append(" ; ")
                append("cmd appops set ").append(pkg).append(" RECORD_AUDIO ").append(op).append(" ; ")
            }
        }
        return when (val outcome = RootShell.exec(script)) {
            RootShell.Outcome.Success -> Result.Enforced(targets.size)
            RootShell.Outcome.NoRoot -> Result.OpenedSystemToggle
            is RootShell.Outcome.Failed -> Result.Failed(outcome.message)
        }
    }

    /** Non-system apps that hold camera or microphone access — the real threat. */
    private fun targetPackages(): List<String> =
        appRepository.installedApps(includeSystem = false)
            .asSequence()
            .map { it.packageName }
            .filter { it != context.packageName }
            .filter {
                appRepository.holdsPermission(it, Manifest.permission.CAMERA) ||
                    appRepository.holdsPermission(it, Manifest.permission.RECORD_AUDIO)
            }
            .toList()

    /** The OS global camera/mic kill switch (the real block on non-rooted devices). */
    fun systemSensorToggleIntent(): Intent =
        Intent(Settings.ACTION_PRIVACY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
