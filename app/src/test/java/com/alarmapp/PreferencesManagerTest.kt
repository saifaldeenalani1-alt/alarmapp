package com.alarmapp

import com.alarmapp.data.PreferencesManager
import com.alarmapp.model.Alarm
import com.alarmapp.model.DayCounterEvent
import com.alarmapp.model.AppSettings
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class PreferencesManagerTest {

    @Test
    fun `alarm CRUD operations`() {
        // Unit test for CRUD logic using PreferencesManager-like in-memory ops
        val alarms = mutableListOf<Alarm>()

        val a1 = Alarm(intervalMinutes = 30, label = "منبه 1")
        val a2 = Alarm(intervalMinutes = 60, label = "منبه 2")

        alarms.add(a1)
        alarms.add(a2)
        assertEquals(2, alarms.size)

        val a1updated = a1.copy(isEnabled = false)
        val idx = alarms.indexOfFirst { it.id == a1.id }
        alarms[idx] = a1updated
        assertFalse(alarms.find { it.id == a1.id }!!.isEnabled)

        alarms.removeAll { it.id == a2.id }
        assertEquals(1, alarms.size)
    }

    @Test
    fun `event CRUD operations`() {
        val events = mutableListOf<DayCounterEvent>()

        val e1 = DayCounterEvent(name = "حدث 1", date = System.currentTimeMillis())
        val e2 = DayCounterEvent(name = "حدث 2", date = System.currentTimeMillis(), isCountdown = false)

        events.add(e1)
        events.add(e2)
        assertEquals(2, events.size)

        events.removeAll { it.id == e1.id }
        assertEquals(1, events.size)
        assertEquals("حدث 2", events[0].name)
    }

    @Test
    fun `settings defaults`() {
        val settings = AppSettings()
        assertEquals(16, settings.fontSize)
        assertEquals(0xFFFFFFFF.toInt(), settings.fontColor)
        assertEquals(0xCC000000.toInt(), settings.backgroundColor)
        assertEquals(80, settings.transparency)
        assertTrue(settings.vibrateEnabled)
    }

    @Test
    fun `settings update`() {
        val settings = AppSettings().copy(
            fontSize = 24,
            fontColor = 0xFFD93030.toInt(),
            transparency = 50,
            vibrateEnabled = false
        )
        assertEquals(24, settings.fontSize)
        assertEquals(0xFFD93030.toInt(), settings.fontColor)
        assertEquals(50, settings.transparency)
        assertFalse(settings.vibrateEnabled)
    }
}
