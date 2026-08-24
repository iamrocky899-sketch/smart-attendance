package com.attendancehalim.smartattendance

import com.attendancehalim.smartattendance.core.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateTimeUtilsTest {

    @Test
    fun testGetCurrentDate_format() {
        val date = DateTimeUtils.getCurrentDate()
        assertNotNull(date)
        assertTrue(date.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }

    @Test
    fun testGetCurrentTime_format() {
        val time = DateTimeUtils.getCurrentTime()
        assertNotNull(time)
        assertTrue(time.matches(Regex("""\d{2}:\d{2}:\d{2}(\s+[AP]M)?""", RegexOption.IGNORE_CASE)))
    }

    @Test
    fun testFormatFullDate() {
        val formatted = DateTimeUtils.formatFullDate("2026-08-24")
        assertEquals("24 August 2026", formatted)
    }

    @Test
    fun testGetDisplayDate() {
        val formatted = DateTimeUtils.getDisplayDate("2026-08-24")
        assertEquals("24 Aug 2026", formatted)
    }

    @Test
    fun testFormatShortDate() {
        val formatted = DateTimeUtils.formatShortDate("2026-08-24")
        assertEquals("24 Aug", formatted)
    }

    @Test
    fun testFormatMonthYear() {
        val formatted = DateTimeUtils.formatMonthYear("2026-08-24")
        assertEquals("AUGUST 2026", formatted)
    }

    @Test
    fun testFormatTimeDisplay_fromIsoWithOffset() {
        // 2026-08-24T16:11:00+05:30 should format as 04:11 PM in Asia/Kolkata
        val formatted = DateTimeUtils.formatTimeDisplay("2026-08-24T16:11:00+05:30")
        assertEquals("04:11 PM", formatted)
    }

    @Test
    fun testFormatTimeDisplay_from24Hour() {
        val formatted = DateTimeUtils.formatTimeDisplay("16:11:00")
        assertEquals("04:11 PM", formatted)
    }

    @Test
    fun testFormatTimeDisplay_from12Hour() {
        val formatted = DateTimeUtils.formatTimeDisplay("04:11:00 PM")
        assertEquals("04:11 PM", formatted)
    }

    @Test
    fun testGetDisplayDate_fromIsoWithOffset() {
        val formatted = DateTimeUtils.getDisplayDate("2026-08-24T16:11:00+05:30")
        assertEquals("24 Aug 2026", formatted)
    }

    @Test
    fun testCalculateDuration() {
        val start = 1700000000000L
        val end = start + (2 * 3600 * 1000) + (15 * 60 * 1000) // 2 hours 15 mins
        val duration = DateTimeUtils.calculateDuration(start, end)
        assertEquals("2h 15m", duration)
    }

    @Test
    fun testCalculateDuration_lessThanHour() {
        val start = 1700000000000L
        val end = start + (45 * 60 * 1000) // 45 mins
        val duration = DateTimeUtils.calculateDuration(start, end)
        assertEquals("45m", duration)
    }

    @Test
    fun testCalculateDuration_zeroOrNegative() {
        val start = 1700000000000L
        val end = start - 1000L
        val duration = DateTimeUtils.calculateDuration(start, end)
        assertEquals("0m", duration)
    }

    @Test
    fun testCalculateDurationFromTimes_standard12Hour() {
        val duration1 = DateTimeUtils.calculateDurationFromTimes("08:00 AM", "04:00 PM")
        assertEquals("8h 0m", duration1)

        val duration2 = DateTimeUtils.calculateDurationFromTimes("09:15 AM", "05:45 PM")
        assertEquals("8h 30m", duration2)
    }

    @Test
    fun testCalculateDurationFromTimes_overnightShift() {
        val duration = DateTimeUtils.calculateDurationFromTimes("11:55 PM", "12:10 AM")
        assertEquals("15m", duration)
    }

    @Test
    fun testCalculateDurationFromTimes_24Hour() {
        val duration = DateTimeUtils.calculateDurationFromTimes("08:30:00", "17:00:00")
        assertEquals("8h 30m", duration)
    }
}
