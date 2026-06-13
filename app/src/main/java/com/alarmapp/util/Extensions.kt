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
    private const val SLOT_BASE = 100000
    private const val RENEWAL_FLAG = -1

    fun scheduleAlarm(context: Context, alarm: Alarm) {
        if (!alarm.isEnabled) return
        cancelAlarm(context, alarm)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val slots = generateSlots(alarm, 2)
        val baseCode = alarm.id.hashCode() * SLOT_BASE

        for ((index, timeMs) in slots) {
            if (timeMs <= now) continue
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarm.id)
                putExtra("slot_index", index)
            }
            val pi = PendingIntent.getBroadcast(
                context, baseCode + index, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, timeMs, 120_000L, pi)
        }

        scheduleRenewal(context, alarm, alarmManager, baseCode, slots.lastOrNull()?.second ?: now)
    }

    private fun scheduleRenewal(context: Context, alarm: Alarm, alarmManager: AlarmManager, baseCode: Int, lastSlotMs: Long) {
        val cal = Calendar.getInstance().apply { timeInMillis = lastSlotMs }
        cal.add(Calendar.MINUTE, 5)
        val renewalTime = cal.timeInMillis
        if (renewalTime <= System.currentTimeMillis()) return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("slot_index", RENEWAL_FLAG)
        }
        val pi = PendingIntent.getBroadcast(
            context, baseCode + RENEWAL_FLAG, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, renewalTime, 120_000L, pi)
    }

    private fun generateSlots(alarm: Alarm, days: Int): List<Pair<Int, Long>> {
        val now = Calendar.getInstance()
        val slots = mutableListOf<Pair<Int, Long>>()
        var slotIdx = 0

        if (alarm.repeatDays.isEmpty()) {
            val cal = makeCal(alarm.startHour, alarm.startMinute)
            if (cal.timeInMillis <= now.timeInMillis) cal.add(Calendar.DAY_OF_YEAR, 1)
            slots.add(slotIdx++ to cal.timeInMillis)
            return slots
        }

        for (d in 0 until days) {
            val day = now.clone() as Calendar
            day.add(Calendar.DAY_OF_YEAR, d)
            if (day.get(Calendar.DAY_OF_WEEK) !in alarm.repeatDays) continue

            val sDay = makeCal(alarm.startHour, alarm.startMinute).apply {
                set(Calendar.YEAR, day.get(Calendar.YEAR))
                set(Calendar.DAY_OF_YEAR, day.get(Calendar.DAY_OF_YEAR))
            }
            val eDay = makeCal(alarm.endHour, alarm.endMinute).apply {
                set(Calendar.YEAR, day.get(Calendar.YEAR))
                set(Calendar.DAY_OF_YEAR, day.get(Calendar.DAY_OF_YEAR))
            }
            if (eDay.before(sDay)) eDay.add(Calendar.DAY_OF_YEAR, 1)

            var t = sDay.timeInMillis
            while (t < eDay.timeInMillis) {
                slots.add(slotIdx++ to t)
                t += alarm.intervalMinutes * 60_000L
            }
        }
        return slots
    }

    private fun makeCal(h: Int, m: Int) = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, h)
        set(Calendar.MINUTE, m)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    fun cancelAlarm(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val baseCode = alarm.id.hashCode() * SLOT_BASE
        val slots = generateSlots(alarm, 2)

        for ((index, _) in slots) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarm.id)
                putExtra("slot_index", index)
            }
            val pi = PendingIntent.getBroadcast(
                context, baseCode + index, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pi)
        }

        val rIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("slot_index", RENEWAL_FLAG)
        }
        val rPi = PendingIntent.getBroadcast(
            context, baseCode + RENEWAL_FLAG, rIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(rPi)
    }

    fun renewBatch(context: Context, alarm: Alarm) {
        scheduleAlarm(context, alarm)
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
