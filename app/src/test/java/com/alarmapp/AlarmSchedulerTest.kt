package com.alarmapp

import com.alarmapp.util.formatTimeShort
import org.junit.Assert.*
import org.junit.Test

class AlarmSchedulerTest {

    @Test
    fun `time formatting consistency`() {
        val testCases = mapOf(
            0L to "00:00",
            5L to "00:05",
            59L to "00:59",
            60L to "01:00",
            61L to "01:01",
            3600L to "1:00:00",
            3661L to "1:01:01",
            86400L to "24:00:00"
        )
        testCases.forEach { (seconds, expected) ->
            assertEquals(expected, formatTimeShort(seconds))
        }
    }

    @Test
    fun `formatTime matches formatTimeShort for large values`() {
        assertEquals("1:00:00", formatTimeShort(3600))
        // Format time adds leading zero for hours
        assertEquals("01:00:00", com.alarmapp.util.AlarmScheduler.formatTime(3600))
    }
}
