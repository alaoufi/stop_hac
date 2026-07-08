package com.privacyshield.monitor.core.analysis

import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.core.model.WhitelistScope

/**
 * Outcome of analysing one access: a severity plus a localisable explanation.
 *
 * [reasonKey] identifies a string template; [reasonArgs] fills its placeholders.
 * Keeping the reason as a key (rather than a baked-in string) lets the UI render
 * it in Arabic or English without the analysis layer knowing about resources.
 */
data class RiskVerdict(
    val level: RiskLevel,
    val reasonKey: String,
    val reasonArgs: List<String> = emptyList(),
)

/**
 * The rule engine that turns raw sensor-access observations into a severity and
 * a human explanation. Deliberately pure (no Android dependencies) so it can be
 * unit-tested exhaustively and reasoned about in isolation.
 *
 * Design principle from the spec: never emit a bare colour. Every verdict names
 * the concrete signal that produced it.
 */
class RiskAnalyzer {

    companion object {
        /** Microphone/camera use beyond this while hidden is a strong red flag. */
        const val LONG_BACKGROUND_USE_MS = 5 * 60_000L // 5 minutes

        /** Repeated background hits above this in a day is suspicious. */
        const val FREQUENT_BACKGROUND_HITS = 5
    }

    fun analyze(ctx: AccessContext): RiskVerdict {
        // 1. Explicit user policy always wins — respect their configured scope.
        ctx.whitelistScope?.let { scope ->
            resolveWhitelist(ctx, scope)?.let { return it }
        }

        // 2. Foreground + screen-on use is the normal, expected case.
        if (ctx.foreground && ctx.screenOn) {
            return normalForeground(ctx)
        }

        // 3. In-call microphone use is expected even without foreground.
        if (ctx.inCall && (ctx.sensor == SensorType.MICROPHONE || ctx.sensor == SensorType.CAMERA)) {
            return RiskVerdict(RiskLevel.NORMAL, "reason_in_call", listOf(ctx.appLabel))
        }

        // 4. Everything below here is background / screen-off usage.
        return analyzeBackground(ctx)
    }

    private fun resolveWhitelist(ctx: AccessContext, scope: WhitelistScope): RiskVerdict? =
        when (scope) {
            WhitelistScope.ALWAYS_ALLOW ->
                RiskVerdict(RiskLevel.NORMAL, "reason_whitelist_always", listOf(ctx.appLabel))

            WhitelistScope.FOREGROUND_ONLY ->
                if (ctx.foreground) {
                    RiskVerdict(RiskLevel.NORMAL, "reason_whitelist_foreground_ok", listOf(ctx.appLabel))
                } else {
                    RiskVerdict(RiskLevel.SUSPICIOUS, "reason_whitelist_foreground_violated", listOf(ctx.appLabel))
                }

            WhitelistScope.SCREEN_ON_ONLY ->
                if (ctx.screenOn) {
                    RiskVerdict(RiskLevel.NORMAL, "reason_whitelist_screen_ok", listOf(ctx.appLabel))
                } else {
                    RiskVerdict(RiskLevel.SUSPICIOUS, "reason_whitelist_screen_violated", listOf(ctx.appLabel))
                }

            WhitelistScope.BLOCK_BACKGROUND ->
                if (ctx.foreground) null // fall through to normal analysis
                else RiskVerdict(RiskLevel.CRITICAL, "reason_whitelist_block_bg", listOf(ctx.appLabel))

            WhitelistScope.ALWAYS_BLOCK ->
                RiskVerdict(RiskLevel.CRITICAL, "reason_whitelist_always_block", listOf(ctx.appLabel))
        }

    private fun normalForeground(ctx: AccessContext): RiskVerdict = when (ctx.sensor) {
        SensorType.CAMERA ->
            RiskVerdict(RiskLevel.NORMAL, "reason_fg_camera", listOf(ctx.appLabel))
        SensorType.MICROPHONE ->
            RiskVerdict(RiskLevel.NORMAL, "reason_fg_mic", listOf(ctx.appLabel))
        SensorType.LOCATION ->
            RiskVerdict(RiskLevel.NORMAL, "reason_fg_location", listOf(ctx.appLabel))
    }

    private fun analyzeBackground(ctx: AccessContext): RiskVerdict {
        val duration = ctx.durationMillis ?: 0L
        val minutes = (duration / 60_000L).toString()

        // Long covert recording — the canonical "very dangerous" case.
        if (duration >= LONG_BACKGROUND_USE_MS &&
            (ctx.sensor == SensorType.MICROPHONE || ctx.sensor == SensorType.CAMERA)
        ) {
            return RiskVerdict(
                RiskLevel.CRITICAL,
                "reason_bg_long_recording",
                listOf(ctx.appLabel, minutes, ctx.sensor.id),
            )
        }

        // Repeated silent background access across the day.
        if (ctx.backgroundHitsToday >= FREQUENT_BACKGROUND_HITS) {
            return RiskVerdict(
                RiskLevel.SUSPICIOUS,
                "reason_bg_frequent",
                listOf(ctx.appLabel, ctx.backgroundHitsToday.toString()),
            )
        }

        // Access resuming right after reboot with no user interaction.
        if (ctx.shortlyAfterBoot && !ctx.foreground) {
            return RiskVerdict(
                RiskLevel.SUSPICIOUS,
                "reason_bg_after_boot",
                listOf(ctx.appLabel),
            )
        }

        // A trusted system app in the background is usually a platform service.
        if (ctx.isSystemApp) {
            return RiskVerdict(
                RiskLevel.ATTENTION,
                "reason_bg_system",
                listOf(ctx.appLabel, ctx.sensor.id),
            )
        }

        // Default background case: unexplained, deserves attention → suspicious.
        return RiskVerdict(
            RiskLevel.SUSPICIOUS,
            "reason_bg_unexpected",
            listOf(ctx.appLabel, ctx.sensor.id),
        )
    }
}
