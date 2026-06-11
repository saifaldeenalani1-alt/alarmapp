package com.alarmapp

import com.alarmapp.model.Alarm
import com.alarmapp.model.DayCounterEvent
import com.alarmapp.model.TimerMode
import com.alarmapp.model.TimerStatus
import com.alarmapp.model.FloatingTimerState
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class AlarmModelTest {

    @Test
    fun `alarm default values are correct`() {
        val alarm = Alarm()
        assertTrue(alarm.isEnabled)
        assertEquals(60, alarm.intervalMinutes)
        assertEquals(0, alarm.startHour)
        assertEquals(0, alarm.startMinute)
        assertEquals(23, alarm.endHour)
        assertEquals(59, alarm.endMinute)
        assertFalse(alarm.isScheduled)
        assertFalse(alarm.label.isNotEmpty())
    }

    @Test
    fun `alarm with custom values`() {
        val alarm = Alarm(
            intervalMinutes = 30,
            startHour = 8,
            startMinute = 0,
            endHour = 22,
            endMinute = 0,
            isScheduled = true,
            label = "اختبار"
        )
        assertEquals(30, alarm.intervalMinutes)
        assertEquals(8, alarm.startHour)
        assertEquals(22, alarm.endHour)
        assertTrue(alarm.isScheduled)
        assertEquals("اختبار", alarm.label)
    }

    @Test
    fun `timer state transitions`() {
        val state = FloatingTimerState(totalSeconds = 3600, mode = TimerMode.COUNTDOWN)
        assertEquals(TimerStatus.STOPPED, state.status)
        assertEquals(3600, state.totalSeconds)
        assertEquals(0, state.elapsedSeconds)
        assertFalse(state.isVisible)
    }

    @Test
    fun `day counter event date calculation`() {
        val futureCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 10) }
        val event = DayCounterEvent(
            name = "اختبار",
            date = futureCal.timeInMillis,
            isCountdown = true
        )
        assertEquals("اختبار", event.name)
        assertTrue(event.isCountdown)

        val now = Calendar.getInstance()
        val eventCal = Calendar.getInstance().apply { timeInMillis = event.date }
        val diff = (eventCal.timeInMillis - now.timeInMillis) / (1000 * 60 * 60 * 24)
        assertTrue(diff >= 9) // at least 9 days (accounting for time of day)
    }

    @Test
    fun `alarm unique ids`() {
        val a1 = Alarm()
        val a2 = Alarm()
        assertNotEquals(a1.id, a2.id)
    }
}
