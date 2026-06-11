package com.alarmapp.model

data class Alarm(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isEnabled: Boolean = true,
    val intervalMinutes: Int = 60,
    val startHour: Int = 0,
    val startMinute: Int = 0,
    val endHour: Int = 23,
    val endMinute: Int = 59,
    val isScheduled: Boolean = false,
    val toneUri: String = "",
    val vibrate: Boolean = true,
    val label: String = ""
)
