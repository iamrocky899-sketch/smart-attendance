package com.attendancehalim.smartattendance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.attendancehalim.smartattendance.domain.model.AttendanceRecord
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.SyncStatus

@Entity(tableName = "attendance_records")
data class AttendanceEntity(
    @PrimaryKey
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val date: String,
    val time: String,
    val attendanceType: AttendanceType,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val localArea: String,
    val syncStatus: SyncStatus,
    val createdByAdminId: String? = null,
    val createdByAdminName: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
) {
    fun toDomainModel(): AttendanceRecord {
        return AttendanceRecord(
            id = id,
            employeeId = employeeId,
            employeeName = employeeName,
            date = date,
            time = time,
            type = attendanceType,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            localArea = localArea,
            syncStatus = syncStatus,
            createdByAdminId = createdByAdminId,
            createdByAdminName = createdByAdminName,
            notes = notes,
            createdAt = createdAt,
            syncedAt = syncedAt
        )
    }

    companion object {
        fun fromDomainModel(record: AttendanceRecord): AttendanceEntity {
            return AttendanceEntity(
                id = record.id,
                employeeId = record.employeeId,
                employeeName = record.employeeName,
                date = record.date,
                time = record.time,
                attendanceType = record.type,
                latitude = record.latitude,
                longitude = record.longitude,
                accuracy = record.accuracy,
                localArea = record.localArea,
                syncStatus = record.syncStatus,
                createdByAdminId = record.createdByAdminId,
                createdByAdminName = record.createdByAdminName,
                notes = record.notes,
                createdAt = record.createdAt,
                syncedAt = record.syncedAt
            )
        }
    }
}
