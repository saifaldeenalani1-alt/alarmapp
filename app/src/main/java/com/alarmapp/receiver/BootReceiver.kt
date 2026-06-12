package com.alarmapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.alarmapp.data.PreferencesManager
import com.alarmapp.util.AlarmScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmApp:BootReceiver")
        wakeLock.acquire(10_000L)

        try {
            val prefs = PreferencesManager(context)
            val alarms = prefs.getAlarms()
            alarms.forEach { alarm ->
                if (alarm.isEnabled) {
                    AlarmScheduler.scheduleAlarm(context, alarm)
                }
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }
}
