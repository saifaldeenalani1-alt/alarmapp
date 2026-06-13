package com.alarmapp.receiver

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
import java.util.concurrent.ConcurrentHashMap

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarm_id") ?: return
        val slotIndex = intent.getIntExtra("slot_index", 0)

        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmApp:AlarmReceiver")
        wakeLock.acquire(30_000L)

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

            // Show notification (silent - sound played via MediaPlayer)
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
                .setVibrate(if (alarm.vibrate) longArrayOf(0, 500, 200, 500) else null)
                .setTimeoutAfter(300_000L)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(alarmId.hashCode(), notification)
            } catch (_: SecurityException) { }

            // Play tone via MediaPlayer (each alarm gets its own player)
            if (!alarm.muteInSilentMode || !isDndActive(context)) {
                playTone(context, alarmId, alarm.toneUri)
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun isDndActive(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            return nm.currentInterruptionFilter == android.app.NotificationManager.INTERRUPTION_FILTER_NONE ||
                   nm.currentInterruptionFilter == android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS
        }
        return false
    }

    private fun playTone(context: Context, alarmId: String, toneUri: String) {
        try {
            val uri = if (toneUri.isNotEmpty())
                Uri.parse(toneUri)
            else
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val player = MediaPlayer.create(context, uri) ?: return
            players[alarmId] = player
            player.setOnCompletionListener {
                player.release()
                players.remove(alarmId)
            }
            player.start()
        } catch (_: Exception) { }
    }

    companion object {
        private val players = ConcurrentHashMap<String, MediaPlayer>()

        fun stopTone(alarmId: String) {
            players.remove(alarmId)?.apply {
                if (isPlaying) stop()
                release()
            }
        }

        fun stopAllTones() {
            players.values.forEach { player ->
                try {
                    if (player.isPlaying) player.stop()
                    player.release()
                } catch (_: Exception) { }
            }
            players.clear()
        }
    }
}
