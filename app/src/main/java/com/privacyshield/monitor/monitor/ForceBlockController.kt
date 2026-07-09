package com.privacyshield.monitor.monitor

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.privacyshield.monitor.data.repo.AppRepository

/**
 * Implements the dashboard "force block camera & microphone" switch — the app's
 * own control, not permission removal.
 *
 * ### What is actually enforceable, honestly
 * A normal app cannot switch another app's sensors off. But two real,
 * app-driven controls exist and this uses whichever is available:
 *
 *  - **Camera, via Device Admin** ([DevicePolicyManager.setCameraDisabled]).
 *    Once the user activates the app as a device admin, the app can cut the
 *    camera for the **entire device** on command — no root. This is genuine,
 *    app-controlled blocking that survives leaving/returning.
 *  - **Everything, via root** — `appops ... ignore` on the camera and mic ops for
 *    every non-system app.
 *
 * The **microphone** cannot be disabled by a device admin (Android exposes no
 * such policy); without root, only the OS global sensor toggle can cut it, which
 * the user flips. We never pretend to have muted the mic when we haven't.
 */
class ForceBlockController(
    private val context: Context,
    private val appRepository: AppRepository,
) {

    private val dpm =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin = TamperAdminReceiver.component(context)

    sealed interface Result {
        /** What the app actually enforced this toggle. */
        data class Enforced(val cameraByAdmin: Boolean, val appsByRoot: Int) : Result
        /** No app-level enforcement available — the system sensor toggle was opened. */
        data object OpenedSystemToggle : Result
        data class Failed(val message: String) : Result
    }

    val rootAvailable: Boolean get() = RootShell.isRootBinaryPresent()
    fun isDeviceAdminActive(): Boolean = runCatching { dpm.isAdminActive(admin) }.getOrDefault(false)

    suspend fun block(): Result = apply(block = true)
    suspend fun unblock(): Result = apply(block = false)

    private suspend fun apply(block: Boolean): Result {
        var cameraByAdmin = false

        // 1. Camera via device admin — real, no root.
        if (isDeviceAdminActive()) {
            val ok = runCatching { dpm.setCameraDisabled(admin, block) }.isSuccess
            if (ok) cameraByAdmin = true
        }

        // 2. Camera + mic for all apps via root, if available.
        var appsByRoot = 0
        if (RootShell.isRootBinaryPresent()) {
            val op = if (block) "ignore" else "default"
            val targets = targetPackages()
            if (targets.isNotEmpty()) {
                val script = buildString {
                    targets.forEach { pkg ->
                        append("cmd appops set ").append(pkg).append(" CAMERA ").append(op).append(" ; ")
                        append("cmd appops set ").append(pkg).append(" RECORD_AUDIO ").append(op).append(" ; ")
                    }
                }
                when (val outcome = RootShell.exec(script)) {
                    RootShell.Outcome.Success -> appsByRoot = targets.size
                    is RootShell.Outcome.Failed -> if (!cameraByAdmin) return Result.Failed(outcome.message)
                    RootShell.Outcome.NoRoot -> {}
                }
            }
        }

        // Nothing app-enforceable → fall back to the OS sensor toggle.
        if (!cameraByAdmin && appsByRoot == 0) return Result.OpenedSystemToggle
        return Result.Enforced(cameraByAdmin, appsByRoot)
    }

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
