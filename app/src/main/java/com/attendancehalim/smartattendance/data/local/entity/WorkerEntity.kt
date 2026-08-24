package com.attendancehalim.smartattendance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.attendancehalim.smartattendance.domain.model.WorkerProfile

@Entity(tableName = "worker_profiles")
data class WorkerEntity(
    @PrimaryKey
    val employeeId: String,
    val fullName: String,
    val mobileNumber: String,
    val workplaceName: String,
    val designation: String,
    val photoUrl: String,
    val joiningDate: String,
    val isActive: Boolean = true,
    val password: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): WorkerProfile {
        return WorkerProfile(
            employeeId = employeeId,
            fullName = fullName,
            mobileNumber = mobileNumber,
            workplaceName = workplaceName,
            designation = designation,
            photoUrl = photoUrl,
            joiningDate = joiningDate,
            isActive = isActive,
            password = password,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomainModel(profile: WorkerProfile): WorkerEntity {
            return WorkerEntity(
                employeeId = profile.employeeId,
                fullName = profile.fullName,
                mobileNumber = profile.mobileNumber,
                workplaceName = profile.workplaceName,
                designation = profile.designation,
                photoUrl = profile.photoUrl,
                joiningDate = profile.joiningDate,
                isActive = profile.isActive,
                password = profile.password,
                createdAt = profile.createdAt
            )
        }
    }
}
