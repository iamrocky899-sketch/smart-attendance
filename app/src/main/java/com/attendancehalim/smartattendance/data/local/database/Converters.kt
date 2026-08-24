package com.attendancehalim.smartattendance.data.local.database

import androidx.room.TypeConverter
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.SyncStatus
import com.attendancehalim.smartattendance.domain.model.UserRole

class Converters {

    @TypeConverter
    fun fromAttendanceType(value: AttendanceType): String = value.name

    @TypeConverter
    fun toAttendanceType(value: String): AttendanceType = AttendanceType.fromString(value)

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return try {
            SyncStatus.valueOf(value)
        } catch (_: Exception) {
            SyncStatus.PENDING
        }
    }

    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.fromString(value)
}
