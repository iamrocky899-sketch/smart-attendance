package com.attendancehalim.smartattendance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.attendancehalim.smartattendance.data.local.entity.AttendanceEntity
import com.attendancehalim.smartattendance.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attendances: List<AttendanceEntity>)

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId ORDER BY createdAt DESC")
    fun getAttendanceForWorker(employeeId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId AND date = :date ORDER BY createdAt ASC")
    fun getTodayAttendanceForWorker(employeeId: String, date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE syncStatus = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingSyncRecords(): List<AttendanceEntity>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE syncStatus = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("UPDATE attendance_records SET syncStatus = :status, syncedAt = :syncedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus, syncedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM attendance_records ORDER BY createdAt DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY createdAt ASC")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE date LIKE :monthPattern ORDER BY createdAt DESC")
    fun getAttendanceForMonth(monthPattern: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId AND date = :date ORDER BY createdAt ASC")
    suspend fun getAttendanceForWorkerAndDateDirect(employeeId: String, date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance_records WHERE id = :id LIMIT 1")
    suspend fun getAttendanceById(id: String): AttendanceEntity?

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteAttendanceById(id: String)
}
