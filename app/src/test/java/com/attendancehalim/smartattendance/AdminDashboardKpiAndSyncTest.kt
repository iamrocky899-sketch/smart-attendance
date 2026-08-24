package com.attendancehalim.smartattendance

import com.attendancehalim.smartattendance.data.remote.dto.ApiResponseDto
import com.attendancehalim.smartattendance.domain.model.AttendanceRecord
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.SyncStatus
import com.attendancehalim.smartattendance.domain.model.WorkerProfile
import com.attendancehalim.smartattendance.presentation.admin.AdminTodayAttendanceRow
import com.attendancehalim.smartattendance.presentation.admin.WorkerTodayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminDashboardKpiAndSyncTest {

    private val workers = listOf(
        WorkerProfile(employeeId = "EMP-0001", fullName = "Rahul Das", mobileNumber = "9876543210", workplaceName = "Main Facility", designation = "Technician", isActive = true),
        WorkerProfile(employeeId = "EMP-0002", fullName = "Anil Sharma", mobileNumber = "9876543211", workplaceName = "North Office", designation = "Engineer", isActive = true),
        WorkerProfile(employeeId = "EMP-0003", fullName = "Priya Roy", mobileNumber = "9876543212", workplaceName = "Main Facility", designation = "Supervisor", isActive = true)
    )

    private fun calculateKpis(
        workers: List<WorkerProfile>,
        todayRecords: List<AttendanceRecord>
    ): Pair<AdminKpis, List<AdminTodayAttendanceRow>> {
        var presentCount = 0
        var punchedInCount = 0
        var punchedOutCount = 0
        var notMarkedCount = 0

        val rows = workers.map { worker ->
            val workerDayRecords = todayRecords.filter { it.employeeId == worker.employeeId }
            val punchIn = workerDayRecords.firstOrNull { it.type == AttendanceType.PUNCH_IN || it.type == AttendanceType.MANUAL }
            val punchOut = workerDayRecords.firstOrNull { it.type == AttendanceType.PUNCH_OUT }

            val status: WorkerTodayStatus = when {
                punchIn != null && punchOut != null -> {
                    presentCount++
                    punchedOutCount++
                    WorkerTodayStatus.PRESENT
                }
                punchIn != null && punchOut == null -> {
                    presentCount++
                    punchedInCount++
                    WorkerTodayStatus.PUNCHED_IN
                }
                else -> {
                    notMarkedCount++
                    WorkerTodayStatus.NOT_MARKED
                }
            }

            AdminTodayAttendanceRow(
                employeeId = worker.employeeId,
                employeeName = worker.fullName,
                workplaceName = worker.workplaceName,
                punchInTime = punchIn?.time,
                punchOutTime = punchOut?.time,
                status = status,
                locationSummary = punchIn?.localArea ?: "No location"
            )
        }

        return Pair(
            AdminKpis(
                total = workers.size,
                present = presentCount,
                punchedIn = punchedInCount,
                punchedOut = punchedOutCount,
                notMarked = notMarkedCount
            ),
            rows
        )
    }

    data class AdminKpis(
        val total: Int,
        val present: Int,
        val punchedIn: Int,
        val punchedOut: Int,
        val notMarked: Int
    )

    @Test
    fun testInitialDashboardState_noPunches() {
        val (kpis, rows) = calculateKpis(workers, emptyList())

        assertEquals(3, kpis.total)
        assertEquals(0, kpis.present)
        assertEquals(0, kpis.punchedIn)
        assertEquals(0, kpis.punchedOut)
        assertEquals(3, kpis.notMarked)
        assertEquals(WorkerTodayStatus.NOT_MARKED, rows[0].status)
    }

    @Test
    fun testWorkerPunchedIn_kpisUpdated() {
        val punchIn = AttendanceRecord(
            id = "att-1",
            employeeId = "EMP-0001",
            employeeName = "Rahul Das",
            date = "2026-08-24",
            time = "16:59:00",
            type = AttendanceType.PUNCH_IN,
            latitude = 26.75,
            longitude = 93.12,
            accuracy = 8f,
            localArea = "Main Office",
            syncStatus = SyncStatus.SYNCED,
            createdAt = 1700000000000L
        )

        val (kpis, rows) = calculateKpis(workers, listOf(punchIn))

        assertEquals(3, kpis.total)
        assertEquals(1, kpis.present)
        assertEquals(1, kpis.punchedIn)
        assertEquals(0, kpis.punchedOut)
        assertEquals(2, kpis.notMarked)
        assertEquals(WorkerTodayStatus.PUNCHED_IN, rows.first { it.employeeId == "EMP-0001" }.status)
        assertEquals(WorkerTodayStatus.NOT_MARKED, rows.first { it.employeeId == "EMP-0002" }.status)
    }

    @Test
    fun testWorkerPunchedOut_kpisUpdated() {
        val punchIn = AttendanceRecord(
            id = "att-1",
            employeeId = "EMP-0001",
            employeeName = "Rahul Das",
            date = "2026-08-24",
            time = "09:00:00",
            type = AttendanceType.PUNCH_IN,
            latitude = 26.75,
            longitude = 93.12,
            accuracy = 8f,
            localArea = "Main Office",
            syncStatus = SyncStatus.SYNCED,
            createdAt = 1700000000000L
        )
        val punchOut = AttendanceRecord(
            id = "att-2",
            employeeId = "EMP-0001",
            employeeName = "Rahul Das",
            date = "2026-08-24",
            time = "18:00:00",
            type = AttendanceType.PUNCH_OUT,
            latitude = 26.75,
            longitude = 93.12,
            accuracy = 8f,
            localArea = "Main Office",
            syncStatus = SyncStatus.SYNCED,
            createdAt = 1700032400000L
        )

        val (kpis, rows) = calculateKpis(workers, listOf(punchIn, punchOut))

        assertEquals(3, kpis.total)
        assertEquals(1, kpis.present)
        assertEquals(0, kpis.punchedIn)
        assertEquals(1, kpis.punchedOut)
        assertEquals(2, kpis.notMarked)
        assertEquals(WorkerTodayStatus.PRESENT, rows.first { it.employeeId == "EMP-0001" }.status)
    }

    @Test
    fun testApiResponseDto_idempotentSuccessDetection() {
        val idempotentResponse = ApiResponseDto<Map<String, Any>>(
            success = true,
            message = "Attendance already recorded (Idempotent)"
        )
        assertTrue(idempotentResponse.isSuccess)
        assertTrue(idempotentResponse.message?.contains("Idempotent", ignoreCase = true) == true)
    }
}
