package com.privacyshield.monitor.util

import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.core.model.SecurityEvent
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.core.util.CsvExporter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    private fun event(label: String) = SecurityEvent(
        id = 1,
        packageName = "com.evil.app",
        appLabel = label,
        sensor = SensorType.MICROPHONE,
        startTimeMillis = 0L,
        endTimeMillis = 1000L,
        foreground = false,
        screenOn = false,
        inCall = false,
        riskLevel = RiskLevel.SUSPICIOUS,
        reasonKey = "reason_bg_unexpected",
        reasonArgs = listOf(label, "microphone"),
    )

    @Test
    fun formulaLabelIsDefused() {
        val csv = CsvExporter.toCsv(listOf(event("=HYPERLINK(\"https://evil.example\")")))
        // The dangerous field must be neutralized with a leading apostrophe and
        // must never appear as a live formula (no unquoted leading '=').
        assertTrue("formula should be prefixed with an apostrophe", csv.contains("'=HYPERLINK"))
        assertFalse("no raw formula field should start a cell", csv.contains(",=HYPERLINK"))
    }

    @Test
    fun plusAndAtAndMinusTriggersAreDefused() {
        listOf("+1+1", "-2+3", "@SUM(A1)").forEach { payload ->
            val csv = CsvExporter.toCsv(listOf(event(payload)))
            assertTrue("payload $payload must be defused", csv.contains("'$payload"))
        }
    }

    @Test
    fun benignLabelIsUnchanged() {
        val csv = CsvExporter.toCsv(listOf(event("Camera")))
        assertTrue(csv.contains(",Camera,"))
        assertFalse(csv.contains("'Camera"))
    }
}
