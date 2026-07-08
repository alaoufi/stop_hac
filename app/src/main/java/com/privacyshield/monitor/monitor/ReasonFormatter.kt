package com.privacyshield.monitor.monitor

import android.content.Context
import com.privacyshield.monitor.R

/**
 * Resolves a risk [reasonKey] (produced by the pure analysis layer) plus its
 * arguments into a localised, human-readable sentence. Keeping this mapping in
 * one place means the analyzer never touches Android resources and the strings
 * can be translated freely (Arabic / English) without code changes.
 *
 * A sensor argument (e.g. "camera") is itself localised before substitution.
 */
object ReasonFormatter {

    private val keyToRes: Map<String, Int> = mapOf(
        "reason_in_call" to R.string.reason_in_call,
        "reason_whitelist_always" to R.string.reason_whitelist_always,
        "reason_whitelist_foreground_ok" to R.string.reason_whitelist_foreground_ok,
        "reason_whitelist_foreground_violated" to R.string.reason_whitelist_foreground_violated,
        "reason_whitelist_screen_ok" to R.string.reason_whitelist_screen_ok,
        "reason_whitelist_screen_violated" to R.string.reason_whitelist_screen_violated,
        "reason_whitelist_block_bg" to R.string.reason_whitelist_block_bg,
        "reason_whitelist_always_block" to R.string.reason_whitelist_always_block,
        "reason_fg_camera" to R.string.reason_fg_camera,
        "reason_fg_mic" to R.string.reason_fg_mic,
        "reason_fg_location" to R.string.reason_fg_location,
        "reason_bg_long_recording" to R.string.reason_bg_long_recording,
        "reason_bg_frequent" to R.string.reason_bg_frequent,
        "reason_bg_after_boot" to R.string.reason_bg_after_boot,
        "reason_bg_system" to R.string.reason_bg_system,
        "reason_bg_unexpected" to R.string.reason_bg_unexpected,
        "reason_perm_granted" to R.string.reason_perm_granted,
        "reason_intrusion_blocked" to R.string.reason_intrusion_blocked,
    )

    /** Sensor ids ("camera"/"microphone"/"location") → localised names. */
    private fun localiseSensorArg(context: Context, arg: String): String = when (arg) {
        "camera" -> context.getString(R.string.sensor_camera)
        "microphone" -> context.getString(R.string.sensor_microphone)
        "location" -> context.getString(R.string.sensor_location)
        else -> arg
    }

    fun format(context: Context, reasonKey: String, args: List<String>): String {
        val resId = keyToRes[reasonKey] ?: return reasonKey
        val localisedArgs = args.map { localiseSensorArg(context, it) }.toTypedArray()
        return runCatching { context.getString(resId, *localisedArgs) }
            .getOrElse { context.getString(resId) }
    }
}
