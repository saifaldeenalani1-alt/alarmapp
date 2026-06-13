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
import com.alarmapp.util.AlarmScheduler

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarm_id") ?: return
        val slotIndex = intent.getIntExtra("slot_index", 0)

        // Acquire wake lock to keep CPU awake during processing
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmApp:AlarmReceiver")
        wakeLock.acquire(10_000L)

        try {
            // Renewal signal: schedule next batch of alarms
            if (slotIndex == -1) {
                val prefs = com.alarmapp.data.PreferencesManager(context)
                val alarm = prefs.getAlarms().find { it.id == alarmId } ?: return
                if (alarm.isEnabled) AlarmScheduler.renewBatch(context, alarm)
                return
            }

            val prefs = com.alarmapp.data.PreferencesManager(context)
            val alarms = prefs.getAlarms()
            val alarm = alarms.find { it.id == alarmId } ?: return
            if (!alarm.isEnabled) return

            val toneUri = if (alarm.toneUri.isNotEmpty())
                Uri.parse(alarm.toneUri)
            else
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val notification = NotificationCompat.Builder(context, AlarmApp.CHANNEL_ALARM)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(alarm.label.ifEmpty { "تنبيه" })
                .setContentText("حان وقت التنبيه!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setFullScreenIntent(
                    PendingIntent.getActivity(
                        context, 0,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    ), true
                )
                .setSound(toneUri)
                .setVibrate(if (alarm.vibrate) longArrayOf(0, 500, 200, 500) else null)
                .setTimeoutAfter(300_000L)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(alarmId.hashCode(), notification)
            } catch (_: SecurityException) { }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }
}
