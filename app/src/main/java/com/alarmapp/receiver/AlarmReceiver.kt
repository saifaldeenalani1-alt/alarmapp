package com.alarmapp.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alarmapp.AlarmApp
import com.alarmapp.MainActivity
import com.alarmapp.util.AlarmScheduler
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarm_id") ?: return
        val slotIndex = intent.getIntExtra("slot_index", 0)

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

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = notificationManager.getNotificationChannel(AlarmApp.CHANNEL_ALARM)
            if (existing == null) {
                val channel = android.app.NotificationChannel(
                    AlarmApp.CHANNEL_ALARM,
                    "تنبيهات المنبه",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "قناة تنبيهات المنبه"
                    enableVibration(alarm.vibrate)
                    if (alarm.vibrate) vibrationPattern = longArrayOf(0, 500, 200, 500)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, AlarmApp.CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(alarm.label.ifEmpty { "تنبيه" })
            .setContentText("حان وقت التنبيه!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(contentIntent, true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(alarmId.hashCode(), notification)
        } catch (_: SecurityException) { }

        if (!alarm.muteInSilentMode || !isDndActive(context)) {
            playToneOnce(context, alarm.toneUri, alarmId.hashCode(), notificationManager)
        } else {
            notificationManager.cancel(alarmId.hashCode())
        }
    }

    private fun isDndActive(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                   nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS
        }
        return false
    }

    private fun playToneOnce(context: Context, toneUri: String, notificationId: Int, notificationManager: NotificationManager) {
        stopTone()
        try {
            val wl = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmApp:AlarmTone")
            wl.acquire()

            val uri = if (toneUri.isNotEmpty()) Uri.parse(toneUri)
                else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            _player = MediaPlayer.create(context, uri)?.apply {
                setOnCompletionListener {
                    release()
                    _player = null
                    notificationManager.cancel(notificationId)
                    if (wl.isHeld) wl.release()
                }
                start()
            }
        } catch (_: Exception) { }
    }

    companion object {
        private var _player: MediaPlayer? = null

        fun stopTone() {
            _player?.apply {
                if (isPlaying) stop()
                release()
            }
            _player = null
        }
    }
}
