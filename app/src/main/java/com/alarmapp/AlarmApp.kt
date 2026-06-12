package com.alarmapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.alarmapp.data.PreferencesManager
import com.alarmapp.util.AlarmScheduler

class AlarmApp : Application() {

    lateinit var prefsManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager(this)
        createNotificationChannels()
        LocaleHelper.setLocale(this, "ar")
        rescheduleAlarms()
    }

    private fun rescheduleAlarms() {
        val alarms = prefsManager.getAlarms()
        alarms.forEach { alarm ->
            if (alarm.isEnabled) {
                AlarmScheduler.scheduleAlarm(this, alarm)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                "تنبيهات المنبه",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة تنبيهات المنبه والصوت"
                enableVibration(true)
            }

            val timerChannel = NotificationChannel(
                CHANNEL_FLOATING_TIMER,
                "المؤقت العائم",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "خدمة المؤقت العائم في الخلفية"
                setSound(null, null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(timerChannel)
        }
    }

    companion object {
        const val CHANNEL_ALARM = "alarm_channel"
        const val CHANNEL_FLOATING_TIMER = "floating_timer_channel"
    }
}
