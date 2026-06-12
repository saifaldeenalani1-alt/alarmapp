package com.alarmapp.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alarmapp.AlarmApp
import com.alarmapp.MainActivity
import com.alarmapp.R
import com.alarmapp.util.AlarmScheduler
import com.alarmapp.data.PreferencesManager

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarm_id") ?: return
        val interval = intent.getIntExtra("interval_minutes", 60)

        val prefs = PreferencesManager(context)
        val alarms = prefs.getAlarms()
        val alarm = alarms.find { it.id == alarmId } ?: return
        if (!alarm.isEnabled) return

        showAlarmNotification(context, alarmId, alarm.label, alarm.toneUri, alarm.vibrate)

        val nextCal = java.util.Calendar.getInstance()
        nextCal.add(java.util.Calendar.MINUTE, interval)

        if (alarm.isScheduled) {
            val now = java.util.Calendar.getInstance()
            val endCal = java.util.Calendar.getInstance()
            endCal.set(java.util.Calendar.HOUR_OF_DAY, alarm.endHour)
            endCal.set(java.util.Calendar.MINUTE, alarm.endMinute)

            if (now.after(endCal)) return
        }

        val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("interval_minutes", interval)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarmId.hashCode(), nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                nextCal.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                android.app.AlarmManager.RTC_WAKEUP,
                nextCal.timeInMillis,
                pendingIntent
            )
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
