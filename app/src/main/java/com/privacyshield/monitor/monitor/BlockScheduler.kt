package com.privacyshield.monitor.monitor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Arms daily alarms that turn the forced camera/mic block on at a start time and
 * off at an end time — e.g. block every night 22:00–07:00.
 *
 * Uses inexact repeating alarms (battery-friendly, no exact-alarm permission
 * needed); a few minutes of drift is fine for a privacy schedule. The window may
 * wrap past midnight (end <= start means the end is the next day).
 */
class BlockScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(startMinutes: Int, endMinutes: Int) {
        setAlarm(REQUEST_START, startMinutes, ScheduleReceiver.ACTION_BLOCK)
        setAlarm(REQUEST_END, endMinutes, ScheduleReceiver.ACTION_UNBLOCK)
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent(REQUEST_START, ScheduleReceiver.ACTION_BLOCK))
        alarmManager.cancel(pendingIntent(REQUEST_END, ScheduleReceiver.ACTION_UNBLOCK))
    }

    /** True if [nowMinutes] falls inside the [start, end) window (handles wrap). */
    fun isWithinWindow(startMinutes: Int, endMinutes: Int, nowMinutes: Int): Boolean =
        if (startMinutes <= endMinutes) {
            nowMinutes in startMinutes until endMinutes
        } else {
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }

    private fun setAlarm(requestCode: Int, minutesOfDay: Int, action: String) {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent(requestCode, action),
        )
    }

    private fun pendingIntent(requestCode: Int, action: String): PendingIntent {
        val intent = Intent(context, ScheduleReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        private const val REQUEST_START = 5001
        private const val REQUEST_END = 5002
    }
}
