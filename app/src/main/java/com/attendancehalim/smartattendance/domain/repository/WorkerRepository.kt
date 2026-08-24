package com.attendancehalim.smartattendance.domain.repository

import com.attendancehalim.smartattendance.core.common.Resource
import com.attendancehalim.smartattendance.domain.model.WorkerProfile
import kotlinx.coroutines.flow.Flow

interface WorkerRepository {
    fun getAllWorkers(): Flow<List<WorkerProfile>>

    fun getWorkerById(employeeId: String): Flow<WorkerProfile?>

    suspend fun getWorkerDirect(employeeId: String): WorkerProfile?

    suspend fun refreshWorkersFromRemote(): Resource<Unit>

    suspend fun addWorker(
        fullName: String,
        mobileNumber: String,
        workplaceName: String,
        designation: String,
        joiningDate: String,
        isActive: Boolean = true
    ): Resource<WorkerProfile>

    suspend fun updateWorker(worker: WorkerProfile): Resource<Unit>

    suspend fun toggleWorkerStatus(employeeId: String, isActive: Boolean): Resource<Unit>

    suspend fun resetWorkerPassword(employeeId: String): Resource<Unit>

    suspend fun generateNextEmployeeId(): String

    suspend fun seedInitialWorkersIfEmpty()
}
