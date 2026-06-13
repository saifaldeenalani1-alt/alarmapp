package com.alarmapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import com.alarmapp.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarm_id") ?: return
        val slotIndex = intent.getIntExtra("slot_index", 0)

        val wl = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmApp:AlarmReceiver")
        wl.acquire(5_000L)

        try {
            // Renewal signal: delegate to AlarmService
            if (slotIndex == -1) {
                AlarmService.startRenewal(context, alarmId)
                return
            }

            val prefs = com.alarmapp.data.PreferencesManager(context)
            val alarms = prefs.getAlarms()
            val alarm = alarms.find { it.id == alarmId } ?: return
            if (!alarm.isEnabled) return

            if (!alarm.muteInSilentMode || !isInSilentMode(context)) {
                AlarmService.startAlarm(context, alarmId, alarm.toneUri, alarm.vibrate, alarm.label)
            }
        } finally {
            if (wl.isHeld) wl.release()
        }
    }

    private fun isInSilentMode(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (nm.currentInterruptionFilter == android.app.NotificationManager.INTERRUPTION_FILTER_NONE) return true
        }
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.ringerMode == AudioManager.RINGER_MODE_SILENT
    }
}
