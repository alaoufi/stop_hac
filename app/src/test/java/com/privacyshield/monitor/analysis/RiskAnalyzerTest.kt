package com.privacyshield.monitor.analysis

import com.privacyshield.monitor.core.analysis.AccessContext
import com.privacyshield.monitor.core.analysis.RiskAnalyzer
import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.core.model.WhitelistScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskAnalyzerTest {

    private val analyzer = RiskAnalyzer()

    private fun ctx(
        sensor: SensorType = SensorType.MICROPHONE,
        foreground: Boolean = false,
        screenOn: Boolean = false,
        inCall: Boolean = false,
        isSystemApp: Boolean = false,
        whitelistScope: WhitelistScope? = null,
        durationMillis: Long? = null,
        backgroundHitsToday: Int = 0,
        shortlyAfterBoot: Boolean = false,
    ) = AccessContext(
        packageName = "com.example.app",
        appLabel = "Example",
        sensor = sensor,
        foreground = foreground,
        screenOn = screenOn,
        inCall = inCall,
        isSystemApp = isSystemApp,
        whitelistScope = whitelistScope,
        durationMillis = durationMillis,
        backgroundHitsToday = backgroundHitsToday,
        shortlyAfterBoot = shortlyAfterBoot,
    )

    @Test
    fun foregroundUseIsNormal() {
        val v = analyzer.analyze(ctx(foreground = true, screenOn = true))
        assertEquals(RiskLevel.NORMAL, v.level)
    }

    @Test
    fun inCallMicIsNormal() {
        val v = analyzer.analyze(ctx(inCall = true))
        assertEquals(RiskLevel.NORMAL, v.level)
        assertEquals("reason_in_call", v.reasonKey)
    }

    @Test
    fun longBackgroundRecordingIsCritical() {
        val v = analyzer.analyze(
            ctx(durationMillis = RiskAnalyzer.LONG_BACKGROUND_USE_MS + 1000),
        )
        assertEquals(RiskLevel.CRITICAL, v.level)
    }

    @Test
    fun frequentBackgroundHitsAreSuspicious() {
        val v = analyzer.analyze(ctx(backgroundHitsToday = RiskAnalyzer.FREQUENT_BACKGROUND_HITS))
        assertEquals(RiskLevel.SUSPICIOUS, v.level)
    }

    @Test
    fun alwaysAllowWhitelistOverridesToNormal() {
        val v = analyzer.analyze(
            ctx(durationMillis = RiskAnalyzer.LONG_BACKGROUND_USE_MS + 1000,
                whitelistScope = WhitelistScope.ALWAYS_ALLOW),
        )
        assertEquals(RiskLevel.NORMAL, v.level)
    }

    @Test
    fun alwaysBlockWhitelistIsCritical() {
        val v = analyzer.analyze(
            ctx(foreground = true, screenOn = true, whitelistScope = WhitelistScope.ALWAYS_BLOCK),
        )
        assertEquals(RiskLevel.CRITICAL, v.level)
    }

    @Test
    fun unexpectedBackgroundIsSuspicious() {
        val v = analyzer.analyze(ctx(sensor = SensorType.CAMERA))
        assertTrue(v.level.isElevated)
    }

    @Test
    fun systemAppBackgroundIsAttention() {
        val v = analyzer.analyze(ctx(sensor = SensorType.LOCATION, isSystemApp = true))
        assertEquals(RiskLevel.ATTENTION, v.level)
    }
}
