package com.alarmapp.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import com.alarmapp.model.Alarm
import com.alarmapp.receiver.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AlarmScheduler {

    fun scheduleAlarm(context: Context, alarm: Alarm) {
        if (!alarm.isEnabled) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("interval_minutes", alarm.intervalMinutes)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarm.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = Calendar.getInstance()
        val intervalMillis = alarm.intervalMinutes * 60 * 1000L

        var nextTime: Long

        if (alarm.isScheduled) {
            val startCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.startHour)
                set(Calendar.MINUTE, alarm.startMinute)
                set(Calendar.SECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.endHour)
                set(Calendar.MINUTE, alarm.endMinute)
                set(Calendar.SECOND, 0)
            }

            if (endCal.before(startCal)) {
                endCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val startMs = startCal.timeInMillis
            val endMs = endCal.timeInMillis

            if (now.timeInMillis < startMs) {
                nextTime = startMs
            } else if (now.timeInMillis in startMs..endMs) {
                val elapsed = now.timeInMillis - startMs
                val intervals = elapsed / intervalMillis
                nextTime = startMs + (intervals + 1) * intervalMillis
                if (nextTime > endMs) {
                    nextTime = startMs + Calendar.DAY_OF_YEAR.toLong() * 24 * 60 * 60 * 1000
                }
            } else {
                nextTime = startMs + Calendar.DAY_OF_YEAR.toLong() * 24 * 60 * 60 * 1000
            }
        } else {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.startHour)
                set(Calendar.MINUTE, alarm.startMinute)
                set(Calendar.SECOND, 0)
            }
            if (cal.timeInMillis <= now.timeInMillis) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            nextTime = cal.timeInMillis
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarm.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun formatTime(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    }

    fun getDefaultAlarmUri(): Uri {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }
}

fun formatTimeShort(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

fun formatDateFull(dateMillis: Long): String {
    val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ar"))
    return sdf.format(dateMillis)
}

fun Int.toHexColor(): String {
    return "#${String.format("%08X", this)}"
}
