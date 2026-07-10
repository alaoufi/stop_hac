package com.privacyshield.monitor.monitor

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Optional, opt-in bridge to real permission control on **rooted devices only**.
 *
 * ### Why this exists, and its boundaries
 * Android forbids an ordinary app from granting or revoking another app's
 * runtime permissions — that is a signature/privileged capability. The only way
 * to do it directly from a normal app is on a device the user has rooted, via
 * `pm grant` / `pm revoke` run as the superuser. This class does exactly that,
 * and **nothing** if root is unavailable: no fallback trickery, no elevation
 * attempts. On a non-rooted device the UI must fall back to deep-linking the
 * user into system settings, which is the honest, supported path.
 *
 * Root detection is done by looking for the `su` binary on disk (no execution),
 * so merely opening the app never triggers a superuser prompt — that only
 * happens the first time the user explicitly taps "revoke".
 */
object RootShell {

    private val suPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/system/sbin/su",
        "/vendor/bin/su",
        "/data/local/bin/su",
        "/data/local/xbin/su",
    )

    /** True if a `su` binary is present. Does NOT execute anything. */
    fun isRootBinaryPresent(): Boolean = suPaths.any { runCatching { File(it).exists() }.getOrDefault(false) }

    /** Result of a permission-control attempt. */
    sealed interface Outcome {
        data object Success : Outcome
        data object NoRoot : Outcome
        data class Failed(val message: String) : Outcome
    }

    suspend fun grant(pkg: String, permission: String): Outcome = run("grant", pkg, permission)
    suspend fun revoke(pkg: String, permission: String): Outcome = run("revoke", pkg, permission)

    /**
     * Runs an arbitrary shell [script] as root. Used for batched `appops`
     * commands. The caller is responsible for building [script] only from
     * trusted, fixed op names and package names it enumerated locally.
     */
    suspend fun exec(script: String): Outcome = withContext(Dispatchers.IO) {
        if (!isRootBinaryPresent()) return@withContext Outcome.NoRoot
        withTimeoutOrNull(30_000) {
            try {
                val process = ProcessBuilder("su", "-c", script)
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().readText().trim()
                val code = process.waitFor()
                if (code == 0) Outcome.Success
                else Outcome.Failed(if (output.isNotEmpty()) output else "exit $code")
            } catch (e: Exception) {
                Outcome.Failed(e.message ?: "error")
            }
        } ?: Outcome.Failed("timeout")
    }

    private suspend fun run(op: String, pkg: String, permission: String): Outcome =
        withContext(Dispatchers.IO) {
            if (!isRootBinaryPresent()) return@withContext Outcome.NoRoot
            withTimeoutOrNull(15_000) {
                try {
                    // Package name and op are from our own fixed set; permission
                    // is one of our TrackedPermission manifest constants — no
                    // externally-controlled string is interpolated here.
                    val process = ProcessBuilder("su", "-c", "pm $op $pkg $permission")
                        .redirectErrorStream(true)
                        .start()
                    val output = process.inputStream.bufferedReader().readText().trim()
                    val code = process.waitFor()
                    if (code == 0) Outcome.Success
                    else Outcome.Failed(if (output.isNotEmpty()) output else "exit $code")
                } catch (e: Exception) {
                    Outcome.Failed(e.message ?: "error")
                }
            } ?: Outcome.Failed("timeout")
        }
}
