package com.privacyshield.monitor.ui

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Gates a sensitive action (lifting the forced sensor block) behind the device's
 * fingerprint / face / PIN. This stops anyone who grabs the unlocked phone from
 * simply switching protection off — the block can only be lifted by the owner.
 *
 * Uses the device credential (PIN/pattern/password) as a fallback so it works
 * even on phones without a biometric sensor, as long as a screen lock is set.
 */
object BiometricGate {

    /** The authenticators we accept, adjusted for API level constraints. */
    private fun allowedAuthenticators(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        } else {
            // Combining weak biometric with device credential is unsupported
            // below API 30; fall back to device credential only.
            DEVICE_CREDENTIAL
        }

    /** True if the device can prompt (a screen lock and/or biometric is set up). */
    fun canAuthenticate(context: Context): Boolean =
        BiometricManager.from(context)
            .canAuthenticate(allowedAuthenticators()) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Shows the system authentication prompt. [onSuccess] fires only on a
     * verified identity; [onFailure] fires on cancel/error so the caller can
     * keep the protection in place.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
        onWrongAttempt: (() -> Unit)? = null,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    // A presented credential was wrong (e.g. unrecognised finger).
                    onWrongAttempt?.invoke()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(allowedAuthenticators())
            .build()
        prompt.authenticate(info)
    }
}
