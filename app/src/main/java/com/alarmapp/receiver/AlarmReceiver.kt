package com.alarmapp.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alarmapp.AlarmApp
import com.alarmapp.MainActivity
import com.alarmapp.R
import com.alarmapp.data.PreferencesManager
import com.alarmapp.util.AlarmScheduler
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmApp:AlarmReceiver")
        wakeLock.acquire(10_000L)

        try {
            val alarmId = intent.getStringExtra("alarm_id") ?: return
            val prefs = PreferencesManager(context)
            val alarms = prefs.getAlarms()
            val alarm = alarms.find { it.id == alarmId } ?: return
            if (!alarm.isEnabled) return

            if (alarm.isScheduled) {
                val now = Calendar.getInstance()
                val endCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, alarm.endHour)
                    set(Calendar.MINUTE, alarm.endMinute)
                    set(Calendar.SECOND, 0)
                }
                if (now.after(endCal)) {
                    AlarmScheduler.cancelAlarm(context, alarm)
                    return
                }
            }

            showAlarmNotification(context, alarmId, alarm.label, alarm.toneUri, alarm.vibrate)

            val nextCal = Calendar.getInstance()
            nextCal.add(Calendar.MINUTE, alarm.intervalMinutes)

            val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarmId)
                putExtra("interval_minutes", alarm.intervalMinutes)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, alarmId.hashCode(), nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, nextCal.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, nextCal.timeInMillis, pendingIntent)
                }
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, nextCal.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, nextCal.timeInMillis, pendingIntent)
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun showAlarmNotification(context: Context, alarmId: String, label: String, toneUri: String, vibrate: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri: Uri = if (toneUri.isNotEmpty()) {
            Uri.parse(toneUri)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }

        val notification = NotificationCompat.Builder(context, AlarmApp.CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label.ifEmpty { "تنبيه" })
            .setContentText("حان وقت التنبيه!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(if (vibrate) longArrayOf(0, 500, 200, 500) else null)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(alarmId.hashCode(), notification)
        } catch (_: SecurityException) { }
    }
}
