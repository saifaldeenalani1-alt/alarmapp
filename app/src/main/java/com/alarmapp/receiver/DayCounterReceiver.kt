package com.alarmapp.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alarmapp.AlarmApp
import com.alarmapp.MainActivity
import com.alarmapp.service.AlarmService
import java.util.Calendar

class DayCounterReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventName = intent.getStringExtra("event_name") ?: return
        val eventId = intent.getStringExtra("event_id") ?: return
        val type = intent.getStringExtra("type") ?: "completion"

        when (type) {
            "completion" -> handleCompletion(context, intent, eventName, eventId)
            "reminder" -> handleReminder(context, intent, eventName, eventId)
        }
    }

    private fun handleCompletion(context: Context, intent: Intent, eventName: String, eventId: String) {
        val isAlarm = intent.getBooleanExtra("alarm_enabled", false)

        if (isAlarm) {
            val toneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            AlarmService.startAlarm(context, eventId, toneUri.toString(), true, eventName)
        } else {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val contentIntent = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, AlarmApp.CHANNEL_ALARM)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(eventName)
                .setContentText("حان موعد المناسبة!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setTimeoutAfter(300_000L)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(eventId.hashCode(), notification)
            } catch (_: SecurityException) { }
        }
    }

    private fun handleReminder(context: Context, intent: Intent, eventName: String, eventId: String) {
        val daysRemaining = intent.getLongExtra("days_remaining", 0)
        val totalDays = intent.getLongExtra("total_days", 0)
        val isCountdown = intent.getBooleanExtra("is_countdown", true)
        val reminderIntervalDays = intent.getIntExtra("reminder_interval_days", 1)
        val eventDate = intent.getLongExtra("event_date", 0)

        val label = if (isCountdown) "متبقي $daysRemaining يوم"
            else "مضى ${totalDays - daysRemaining} يوم"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, AlarmApp.CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(eventName)
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(eventId.hashCode() + 1, notification)
        } catch (_: SecurityException) { }

        scheduleNextReminder(context, eventId, eventName, daysRemaining - reminderIntervalDays,
            totalDays, isCountdown, reminderIntervalDays, eventDate)
    }

    private fun scheduleNextReminder(context: Context, eventId: String, eventName: String,
                                      remainingDays: Long, totalDays: Long,
                                      isCountdown: Boolean, interval: Int, eventDate: Long) {
        if (remainingDays <= 0) return
        if (eventDate > 0 && Calendar.getInstance().timeInMillis >= eventDate) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminderCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, interval)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val reminderIntent = Intent(context, DayCounterReceiver::class.java).apply {
            putExtra("event_id", eventId)
            putExtra("event_name", eventName)
            putExtra("days_remaining", remainingDays)
            putExtra("total_days", totalDays)
            putExtra("is_countdown", isCountdown)
            putExtra("reminder_interval_days", interval)
            putExtra("event_date", eventDate)
            putExtra("type", "reminder")
        }
        val pi = PendingIntent.getBroadcast(
            context, eventId.hashCode() + 2000, reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.set(AlarmManager.RTC_WAKEUP, reminderCal.timeInMillis, pi)
    }
}
