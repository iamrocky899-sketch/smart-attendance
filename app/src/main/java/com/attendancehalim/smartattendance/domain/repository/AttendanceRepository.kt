package com.attendancehalim.smartattendance.domain.repository

import com.attendancehalim.smartattendance.core.common.Resource
import com.attendancehalim.smartattendance.domain.model.AttendanceRecord
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.LocationDetails
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {
    suspend fun recordAttendance(
        employeeId: String,
        employeeName: String,
        type: AttendanceType,
        location: LocationDetails
    ): Resource<AttendanceRecord>

    fun getAttendanceForWorker(employeeId: String): Flow<List<AttendanceRecord>>

    fun getTodayAttendanceForWorker(employeeId: String, date: String): Flow<List<AttendanceRecord>>

    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>>

    suspend fun recordManualAttendance(
        employeeId: String,
        employeeName: String,
        date: String,
        inTime: String,
        outTime: String?,
        notes: String?,
        adminId: String,
        adminName: String,
        location: LocationDetails?
    ): Resource<AttendanceRecord>

    suspend fun syncPendingAttendance(): Resource<Int>

    suspend fun syncAttendanceImmediately(attendanceId: String): Resource<Unit>

    suspend fun refreshTodayAttendance(): Resource<List<AttendanceRecord>>

    fun getPendingCount(): Flow<Int>
}
