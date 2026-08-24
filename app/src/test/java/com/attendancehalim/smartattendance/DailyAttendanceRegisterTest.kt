package com.attendancehalim.smartattendance

import com.attendancehalim.smartattendance.domain.model.AttendanceRecord
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.DailyAttendanceRegister
import com.attendancehalim.smartattendance.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DailyAttendanceRegisterTest {

    @Test
    fun testDailyAttendanceRegister_punchInAndOut() {
        val punchIn = AttendanceRecord(
            id = "in-1",
            employeeId = "EMP-0001",
            employeeName = "Rahul Das",
            date = "2026-08-24",
            time = "09:00:00",
            type = AttendanceType.PUNCH_IN,
            latitude = 26.7584,
            longitude = 93.1256,
            accuracy = 10f,
            localArea = "Main Office",
            syncStatus = SyncStatus.SYNCED,
            createdAt = 1700000000000L
        )

        val punchOut = AttendanceRecord(
            id = "out-1",
            employeeId = "EMP-0001",
            employeeName = "Rahul Das",
            date = "2026-08-24",
            time = "17:30:00",
            type = AttendanceType.PUNCH_OUT,
            latitude = 26.7584,
            longitude = 93.1256,
            accuracy = 10f,
            localArea = "Main Office",
            syncStatus = SyncStatus.SYNCED,
            createdAt = 1700030600000L
        )

        val register = DailyAttendanceRegister(
            date = "2026-08-24",
            displayDay = "24 Aug",
            fullDateDisplay = "Monday, August 24, 2026",
            monthHeader = "August 2026",
            punchInRecord = punchIn,
            punchOutRecord = punchOut,
            workingDuration = "8h 30m",
            locationSummary = "Main Office",
            overallSyncStatus = SyncStatus.SYNCED
        )

        assertNotNull(register.punchInRecord)
        assertNotNull(register.punchOutRecord)
        assertEquals("8h 30m", register.workingDuration)
        assertEquals(SyncStatus.SYNCED, register.overallSyncStatus)
    }

    @Test
    fun testDailyAttendanceRegister_pendingStatus() {
        val punchIn = AttendanceRecord(
            id = "in-pending",
            employeeId = "EMP-0001",
            employeeName = "Rahul Das",
            date = "2026-08-24",
            time = "09:00:00",
            type = AttendanceType.PUNCH_IN,
            latitude = 26.7584,
            longitude = 93.1256,
            accuracy = 10f,
            localArea = "Main Office",
            syncStatus = SyncStatus.PENDING,
            createdAt = 1700000000000L
        )

        val register = DailyAttendanceRegister(
            date = "2026-08-24",
            displayDay = "24 Aug",
            fullDateDisplay = "Monday, August 24, 2026",
            monthHeader = "August 2026",
            punchInRecord = punchIn,
            punchOutRecord = null,
            workingDuration = "In Progress",
            locationSummary = "Main Office",
            overallSyncStatus = SyncStatus.PENDING
        )

        assertEquals(SyncStatus.PENDING, register.overallSyncStatus)
        assertNull(register.punchOutRecord)
    }
}
