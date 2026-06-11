package com.alarmapp.model

data class DayCounterEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val date: Long,
    val isCountdown: Boolean = true,
    val widgetId: Int = -1
)
