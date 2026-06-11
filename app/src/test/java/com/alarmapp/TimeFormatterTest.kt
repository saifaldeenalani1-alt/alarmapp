package com.alarmapp

import com.alarmapp.util.formatTimeShort
import com.alarmapp.util.AlarmScheduler.formatTime
import org.junit.Assert.*
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun `format time zero`() {
        assertEquals("00:00:00", formatTime(0))
        assertEquals("00:00", formatTimeShort(0))
    }

    @Test
    fun `format time seconds only`() {
        assertEquals("00:00:30", formatTime(30))
        assertEquals("00:30", formatTimeShort(30))
    }

    @Test
    fun `format time minutes and seconds`() {
        assertEquals("00:05:30", formatTime(330))
        assertEquals("05:30", formatTimeShort(330))
    }

    @Test
    fun `format time hours`() {
        assertEquals("01:00:00", formatTime(3600))
        assertEquals("1:00:00", formatTimeShort(3600))
    }

    @Test
    fun `format time large values`() {
        assertEquals("24:00:00", formatTime(86400))
        assertEquals("24:00:00", formatTimeShort(86400))
    }

    @Test
    fun `format time edge cases`() {
        assertEquals("00:00:01", formatTime(1))
        assertEquals("00:00:59", formatTime(59))
        assertEquals("00:01:00", formatTime(60))
        assertEquals("00:59:59", formatTime(3599))
        assertEquals("01:00:00", formatTime(3600))
    }
}
