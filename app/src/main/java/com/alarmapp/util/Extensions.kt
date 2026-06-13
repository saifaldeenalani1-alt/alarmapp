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
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarm.id.hashCode(), intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var nextTime = nextAlarmMillis(alarm)
        val now = System.currentTimeMillis()
        if (nextTime <= now) nextTime = now + 60_000L

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true

        if (canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, nextTime, 120_000L, pendingIntent)
        }
    }

    private fun nextAlarmMillis(alarm: Alarm, fromTime: Calendar = Calendar.getInstance()): Long {
        val now = fromTime
        val intervalMs = alarm.intervalMinutes * 60 * 1000L

        fun makeCal(h: Int, m: Int) = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        fun nextRepeatDay(afterCal: Calendar): Long {
            val cal = afterCal.clone() as Calendar
            for (i in 1..7) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                if (alarm.repeatDays.contains(cal.get(Calendar.DAY_OF_WEEK))) {
                    val s = makeCal(alarm.startHour, alarm.startMinute).apply {
                        set(Calendar.YEAR, cal.get(Calendar.YEAR))
                        set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR))
                    }
                    return s.timeInMillis
                }
            }
            return makeCal(alarm.startHour, alarm.startMinute)
                .apply { add(Calendar.DAY_OF_YEAR, 8) }.timeInMillis
        }

        if (alarm.repeatDays.isNotEmpty()) {
            val today = now.clone() as Calendar
            val todayMs = today.timeInMillis
            val sToday = makeCal(alarm.startHour, alarm.startMinute).apply {
                set(Calendar.YEAR, today.get(Calendar.YEAR))
                set(Calendar.DAY_OF_YEAR, today.get(Calendar.DAY_OF_YEAR))
            }
            val eToday = makeCal(alarm.endHour, alarm.endMinute).apply {
                set(Calendar.YEAR, today.get(Calendar.YEAR))
                set(Calendar.DAY_OF_YEAR, today.get(Calendar.DAY_OF_YEAR))
            }
            if (eToday.before(sToday)) eToday.add(Calendar.DAY_OF_YEAR, 1)

            val dayOfWeek = today.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek in alarm.repeatDays) {
                if (todayMs < sToday.timeInMillis) return sToday.timeInMillis
                if (todayMs in sToday.timeInMillis..eToday.timeInMillis) {
                    val elapsed = todayMs - sToday.timeInMillis
                    val intervals = elapsed / intervalMs
                    val nextInWindow = sToday.timeInMillis + (intervals + 1) * intervalMs
                    if (nextInWindow <= eToday.timeInMillis) return nextInWindow
                }
            }
            return nextRepeatDay(today)
        }

        val single = makeCal(alarm.startHour, alarm.startMinute)
        if (single.timeInMillis <= now.timeInMillis) single.add(Calendar.DAY_OF_YEAR, 1)
        return single.timeInMillis
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
    return if (hrs > 0)
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    else
        String.format(Locale.US, "%02d:%02d", mins, secs)
}

fun formatDateFull(dateMillis: Long): String {
    val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ar"))
    return sdf.format(dateMillis)
}

fun Int.toHexColor(): String {
    return "#${String.format("%08X", this)}"
}
