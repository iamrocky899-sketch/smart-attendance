package com.attendancehalim.smartattendance.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Standard Date & Time utility functions for SMART ATTENDANCE.
 * All formatting and parsing strictly adheres to Asia/Kolkata (India Standard Time).
 */
object DateTimeUtils {

    val TIMEZONE_INDIA: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    private const val PATTERN_DATE_STANDARD = "yyyy-MM-dd"
    private const val PATTERN_TIME_12H = "hh:mm:ss a"
    private const val PATTERN_TIME_24H = "HH:mm:ss"
    private const val PATTERN_DATE_DISPLAY = "dd MMM yyyy"
    private const val PATTERN_DATE_FULL = "dd MMMM yyyy"
    private const val PATTERN_DATE_SHORT = "dd MMM"
    private const val PATTERN_MONTH_YEAR = "MMMM yyyy"
    private const val PATTERN_TIME_SHORT = "hh:mm a"
    private const val PATTERN_ISO_WITH_OFFSET = "yyyy-MM-dd'T'HH:mm:ssXXX"
    private const val PATTERN_ISO_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    private const val PATTERN_ISO_MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"

    private fun createDateFormat(pattern: String): SimpleDateFormat {
        return SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TIMEZONE_INDIA
        }
    }

    fun getCurrentDate(): String {
        return createDateFormat(PATTERN_DATE_STANDARD).format(Date())
    }

    fun getCurrentTime(): String {
        return createDateFormat(PATTERN_TIME_12H).format(Date())
    }

    fun getCurrentTimeFormatted(): String {
        return createDateFormat(PATTERN_TIME_SHORT).format(Date())
    }

    /**
     * Converts a standard date string or ISO timestamp into "dd MMM yyyy" (e.g. "24 Aug 2026").
     */
    fun getDisplayDate(dateString: String): String {
        if (dateString.isBlank()) return ""
        val parsedDate = parseDateOrIso(dateString)
        return if (parsedDate != null) {
            createDateFormat(PATTERN_DATE_DISPLAY).format(parsedDate)
        } else {
            dateString
        }
    }

    /**
     * Converts a standard date string or ISO timestamp into "dd MMMM yyyy" (e.g. "24 August 2026").
     */
    fun formatFullDate(dateString: String): String {
        if (dateString.isBlank()) return ""
        val parsedDate = parseDateOrIso(dateString)
        return if (parsedDate != null) {
            createDateFormat(PATTERN_DATE_FULL).format(parsedDate)
        } else {
            dateString
        }
    }

    /**
     * Converts a date string into "dd MMM" (e.g. "24 Aug").
     */
    fun formatShortDate(dateString: String): String {
        if (dateString.isBlank()) return ""
        val parsedDate = parseDateOrIso(dateString)
        return if (parsedDate != null) {
            createDateFormat(PATTERN_DATE_SHORT).format(parsedDate)
        } else {
            dateString
        }
    }

    /**
     * Formats a date into uppercase month and year (e.g. "AUGUST 2026").
     */
    fun formatMonthYear(dateString: String): String {
        if (dateString.isBlank()) return "ATTENDANCE REGISTER"
        val parsedDate = parseDateOrIso(dateString)
        return if (parsedDate != null) {
            createDateFormat(PATTERN_MONTH_YEAR).format(parsedDate).uppercase(Locale.US)
        } else {
            "ATTENDANCE REGISTER"
        }
    }

    /**
     * Formats any valid time representation (12-hour, 24-hour, or ISO timestamp with offset)
     * into standard 12-hour "hh:mm a" format (e.g. "04:11 PM").
     */
    fun formatTimeDisplay(timeString: String): String {
        if (timeString.isBlank()) return "--:--"

        // 1. Try ISO timestamp parsing (e.g. 2026-08-24T16:11:00+05:30)
        if (timeString.contains("T")) {
            val isoDate = parseIsoTimestamp(timeString)
            if (isoDate != null) {
                return createDateFormat(PATTERN_TIME_SHORT).format(isoDate)
            }
        }

        // 2. Try 12-hour with seconds (e.g. "04:11:00 PM" or "4:11:00 PM")
        try {
            val parser = SimpleDateFormat(PATTERN_TIME_12H, Locale.US).apply { timeZone = TIMEZONE_INDIA }
            val date = parser.parse(timeString)
            if (date != null) {
                return createDateFormat(PATTERN_TIME_SHORT).format(date)
            }
        } catch (_: Exception) {}

        // 3. Try 12-hour without seconds (e.g. "04:11 PM" or "4:11 PM")
        try {
            val parser = SimpleDateFormat(PATTERN_TIME_SHORT, Locale.US).apply { timeZone = TIMEZONE_INDIA }
            val date = parser.parse(timeString)
            if (date != null) {
                return createDateFormat(PATTERN_TIME_SHORT).format(date)
            }
        } catch (_: Exception) {}

        // 4. Try 24-hour with seconds (e.g. "16:11:00")
        try {
            val parser = SimpleDateFormat(PATTERN_TIME_24H, Locale.US).apply { timeZone = TIMEZONE_INDIA }
            val date = parser.parse(timeString)
            if (date != null) {
                return createDateFormat(PATTERN_TIME_SHORT).format(date)
            }
        } catch (_: Exception) {}

        // 5. Try 24-hour without seconds (e.g. "16:11")
        try {
            val parser = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TIMEZONE_INDIA }
            val date = parser.parse(timeString)
            if (date != null) {
                return createDateFormat(PATTERN_TIME_SHORT).format(date)
            }
        } catch (_: Exception) {}

        return timeString
    }

    /**
     * Calculates duration string between two timestamps and/or time strings (e.g. "8h 00m", "8h 30m", or "15m").
     * Supports midnight-spanning shifts (e.g. 11:55 PM to 12:10 AM -> 15m) and manual entries.
     */
    fun calculateDuration(
        inTimestamp: Long,
        outTimestamp: Long,
        inTime: String? = null,
        outTime: String? = null
    ): String {
        if (outTimestamp > inTimestamp && (outTimestamp - inTimestamp) >= 60_000L) {
            val diffMillis = outTimestamp - inTimestamp
            val totalMinutes = diffMillis / (1000 * 60)
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }

        if (!inTime.isNullOrBlank() && !outTime.isNullOrBlank()) {
            return calculateDurationFromTimes(inTime, outTime)
        }

        return "0m"
    }

    fun calculateDuration(inTimestamp: Long, outTimestamp: Long): String {
        return calculateDuration(inTimestamp, outTimestamp, null, null)
    }

    /**
     * Calculates duration between two time strings (12h or 24h, e.g. "08:00 AM" to "04:00 PM" -> "8h 00m").
     */
    fun calculateDurationFromTimes(inTime: String, outTime: String): String {
        if (inTime.isBlank() || outTime.isBlank()) return "0m"
        val inMinutes = parseTimeToMinutes(inTime)
        val outMinutes = parseTimeToMinutes(outTime)
        if (inMinutes == null || outMinutes == null) return "0m"

        var diff = outMinutes - inMinutes
        if (diff < 0) {
            // Overnight shift spanning midnight (e.g. 11:55 PM to 12:10 AM)
            diff += 24 * 60
        }
        val hours = diff / 60
        val minutes = diff % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private fun parseTimeToMinutes(timeString: String): Int? {
        if (timeString.isBlank()) return null

        // 1. Try ISO timestamp
        if (timeString.contains("T")) {
            val isoDate = parseIsoTimestamp(timeString)
            if (isoDate != null) {
                val cal = java.util.Calendar.getInstance(TIMEZONE_INDIA)
                cal.time = isoDate
                return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            }
        }

        // 2. Try 12-hour with seconds ("08:00:00 AM" or "8:00:00 AM")
        try {
            val parser = SimpleDateFormat(PATTERN_TIME_12H, Locale.US).apply { timeZone = TIMEZONE_INDIA }
            val date = parser.parse(timeString)
            if (date != null) {
                val cal = java.util.Calendar.getInstance(TIMEZONE_INDIA)
                cal.time = date
                return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            }
        } catch (_: Exception) {}

        // 3. Try 12-hour short ("08:00 AM" or "8:00 AM")
        try {
            val parser = SimpleDateFormat(PATTERN_TIME_SHORT, Locale.US).apply { timeZone = TIMEZONE_INDIA }
            val date = parser.parse(timeString)
            if (date != null) {
                val cal = java.util.Calendar.getInstance(TIMEZONE_INDIA)
                cal.time = date
                return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            }
        } catch (_: Exception) {}

        // 4. Try 24-hour with seconds ("08:00:00" or "17:00:00")
        try {
            val parser = SimpleDateFormat(PATTERN_TIME_24H, Locale.US).apply { timeZone = TIMEZONE_INDIA }
            val date = parser.parse(timeString)
            if (date != null) {
                val cal = java.util.Calendar.getInstance(TIMEZONE_INDIA)
                cal.time = date
                return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            }
        } catch (_: Exception) {}

        // 5. Try 24-hour short ("08:00" or "17:00")
        try {
            val parser = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TIMEZONE_INDIA }
            val date = parser.parse(timeString)
            if (date != null) {
                val cal = java.util.Calendar.getInstance(TIMEZONE_INDIA)
                cal.time = date
                return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            }
        } catch (_: Exception) {}

        return null
    }

    fun getCurrentIsoTimestamp(): String {
        val sdf = SimpleDateFormat(PATTERN_ISO_WITH_OFFSET, Locale.US).apply {
            timeZone = TIMEZONE_INDIA
        }
        return sdf.format(Date())
    }

    private fun parseDateOrIso(dateString: String): Date? {
        if (dateString.contains("T")) {
            return parseIsoTimestamp(dateString)
        }

        // Try YYYY-MM-DD
        try {
            val parser = SimpleDateFormat(PATTERN_DATE_STANDARD, Locale.US).apply {
                timeZone = TIMEZONE_INDIA
            }
            return parser.parse(dateString)
        } catch (_: Exception) {}

        // Try YYYY-MM
        try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US).apply {
                timeZone = TIMEZONE_INDIA
            }
            return parser.parse(dateString)
        } catch (_: Exception) {}

        return null
    }

    private fun parseIsoTimestamp(isoString: String): Date? {
        val patterns = listOf(
            PATTERN_ISO_MILLIS,
            PATTERN_ISO_WITH_OFFSET,
            PATTERN_ISO_Z,
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US)
                val date = parser.parse(isoString)
                if (date != null) return date
            } catch (_: Exception) {}
        }
        return null
    }
}
