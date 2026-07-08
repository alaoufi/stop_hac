package com.privacyshield.monitor.core.util

import com.privacyshield.monitor.core.model.SecurityEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Serialises events to CSV for the user's own records. Export is purely local —
 * the user chooses where the file goes via the system share sheet; the app
 * never uploads it anywhere.
 */
object CsvExporter {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun toCsv(events: List<SecurityEvent>): String {
        val sb = StringBuilder()
        sb.appendLine("start,end,duration_sec,app,package,sensor,foreground,screen_on,in_call,risk,reason_key")
        for (e in events) {
            val duration = e.durationMillis?.let { it / 1000 } ?: 0
            sb.append(dateFmt.format(Date(e.startTimeMillis))).append(',')
            sb.append(e.endTimeMillis?.let { dateFmt.format(Date(it)) } ?: "").append(',')
            sb.append(duration).append(',')
            sb.append(escape(e.appLabel)).append(',')
            sb.append(escape(e.packageName)).append(',')
            sb.append(e.sensor.name).append(',')
            sb.append(e.foreground).append(',')
            sb.append(e.screenOn).append(',')
            sb.append(e.inCall).append(',')
            sb.append(e.riskLevel.name).append(',')
            sb.append(escape(e.reasonKey)).appendLine()
        }
        return sb.toString()
    }

    private fun escape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
