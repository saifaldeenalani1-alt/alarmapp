package com.alarmapp.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alarmapp.AlarmApp
import com.alarmapp.MainActivity
import com.alarmapp.util.AlarmScheduler
import java.util.concurrent.ConcurrentHashMap

class AlarmService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getStringExtra("alarm_id") ?: return START_NOT_STICKY
        val toneUri = intent.getStringExtra("tone_uri") ?: ""
        val vibrate = intent.getBooleanExtra("vibrate", true)
        val label = intent.getStringExtra("label") ?: ""
        val isRenewal = intent.getBooleanExtra("is_renewal", false)

        if (isRenewal) {
            handleRenewal(alarmId)
            return START_NOT_STICKY
        }

        val notificationId = alarmId.hashCode()
        val toneUriObj = if (toneUri.isNotEmpty()) Uri.parse(toneUri)
            else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val fullScreenIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AlarmApp.CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label.ifEmpty { "تنبيه" })
            .setContentText("حان وقت التنبيه!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenIntent, true)
            .setVibrate(if (vibrate) longArrayOf(0, 500, 200, 500) else null)
            .build()

        startForeground(notificationId, notification)

        playTone(alarmId, toneUriObj, notificationId)
        return START_NOT_STICKY
    }

    private fun handleRenewal(alarmId: String) {
        val prefs = com.alarmapp.data.PreferencesManager(this)
        val alarm = prefs.getAlarms().find { it.id == alarmId } ?: return
        if (alarm.isEnabled) AlarmScheduler.renewBatch(this, alarm)
        stopSelf()
    }

    private fun playTone(alarmId: String, toneUri: Uri, notificationId: Int) {
        try {
            val player = MediaPlayer.create(this, toneUri) ?: return
            activePlayers[alarmId] = player
            player.setOnCompletionListener {
                player.release()
                activePlayers.remove(alarmId)
                try { NotificationManagerCompat.from(this).cancel(notificationId) } catch (_: Exception) { }
                if (activePlayers.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            player.start()
        } catch (_: Exception) {
            try { NotificationManagerCompat.from(this).cancel(notificationId) } catch (_: Exception) { }
            if (activePlayers.isEmpty()) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private val activePlayers = ConcurrentHashMap<String, MediaPlayer>()

        fun startAlarm(context: Context, alarmId: String, toneUri: String, vibrate: Boolean, label: String) {
            val intent = Intent(context, AlarmService::class.java).apply {
                putExtra("alarm_id", alarmId)
                putExtra("tone_uri", toneUri)
                putExtra("vibrate", vibrate)
                putExtra("label", label)
                putExtra("is_renewal", false)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startRenewal(context: Context, alarmId: String) {
            val intent = Intent(context, AlarmService::class.java).apply {
                putExtra("alarm_id", alarmId)
                putExtra("is_renewal", true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTone(alarmId: String) {
            activePlayers.remove(alarmId)?.apply {
                if (isPlaying) stop()
                release()
            }
        }

        fun stopAllTones() {
            activePlayers.values.forEach { player ->
                try { if (player.isPlaying) player.stop(); player.release() } catch (_: Exception) { }
            }
            activePlayers.clear()
        }
    }
}
