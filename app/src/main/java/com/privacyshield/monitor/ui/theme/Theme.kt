package com.privacyshield.monitor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.data.prefs.ThemeMode

private val LightColors = lightColorScheme(
    primary = ShieldTeal,
    secondary = Indigo,
    background = SurfaceLight,
    surface = androidx.compose.ui.graphics.Color.White,
)

private val DarkColors = darkColorScheme(
    primary = ShieldTealDark,
    secondary = IndigoLight,
    background = SurfaceDark,
    surface = SurfaceContainerDark,
)

/** Colours for a given risk level — (foreground, container). */
data class RiskColors(
    val accent: androidx.compose.ui.graphics.Color,
    val container: androidx.compose.ui.graphics.Color,
)

fun riskColorsFor(level: RiskLevel): RiskColors = when (level) {
    RiskLevel.NORMAL -> RiskColors(RiskGreen, RiskGreenContainer)
    RiskLevel.ATTENTION -> RiskColors(RiskAmber, RiskAmberContainer)
    RiskLevel.SUSPICIOUS -> RiskColors(RiskOrange, RiskOrangeContainer)
    RiskLevel.CRITICAL -> RiskColors(RiskRed, RiskRedContainer)
}

@Composable
fun PrivacyShieldTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
