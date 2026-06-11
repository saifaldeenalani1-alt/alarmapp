package com.alarmapp.model

data class AppSettings(
    val fontSize: Int = 16,
    val fontColor: Int = 0xFFFFFFFF.toInt(),
    val backgroundColor: Int = 0xCC000000.toInt(),
    val transparency: Int = 80,
    val alarmToneUri: String = "",
    val vibrateEnabled: Boolean = true,
    val floatingTimerEnabled: Boolean = false
)
