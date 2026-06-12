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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextTime = nextAlarmMillis(alarm)

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

    private fun nextAlarmMillis(alarm: Alarm): Long {
        val now = Calendar.getInstance()
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.startHour)
            set(Calendar.MINUTE, alarm.startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.repeatDays.isNotEmpty()) {
            if (alarm.repeatDays.contains(now.get(Calendar.DAY_OF_WEEK)) && todayCal.timeInMillis > now.timeInMillis) {
                return todayCal.timeInMillis
            }
            for (i in 1..7) {
                todayCal.add(Calendar.DAY_OF_YEAR, 1)
                if (alarm.repeatDays.contains(todayCal.get(Calendar.DAY_OF_WEEK))) {
                    return todayCal.timeInMillis
                }
            }
            return todayCal.timeInMillis
        }

        if (todayCal.timeInMillis <= now.timeInMillis) {
            todayCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return todayCal.timeInMillis
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
