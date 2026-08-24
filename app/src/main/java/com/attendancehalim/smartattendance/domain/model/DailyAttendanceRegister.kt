package com.attendancehalim.smartattendance.domain.model

data class DailyAttendanceRegister(
    val date: String,
    val displayDay: String,
    val fullDateDisplay: String,
    val monthHeader: String,
    val punchInRecord: AttendanceRecord?,
    val punchOutRecord: AttendanceRecord?,
    val workingDuration: String?,
    val locationSummary: String,
    val overallSyncStatus: SyncStatus
)
