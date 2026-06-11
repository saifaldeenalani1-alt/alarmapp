package com.alarmapp.model

enum class TimerMode {
    COUNTDOWN, COUNTUP
}

enum class TimerStatus {
    STOPPED, RUNNING, PAUSED, FINISHED
}

data class FloatingTimerState(
    val totalSeconds: Long = 0,
    val elapsedSeconds: Long = 0,
    val mode: TimerMode = TimerMode.COUNTDOWN,
    val status: TimerStatus = TimerStatus.STOPPED,
    val isVisible: Boolean = false,
    val posX: Float = 100f,
    val posY: Float = 200f
)
