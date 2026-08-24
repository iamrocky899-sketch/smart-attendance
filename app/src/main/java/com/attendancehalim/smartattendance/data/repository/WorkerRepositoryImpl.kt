package com.attendancehalim.smartattendance.data.repository

import com.attendancehalim.smartattendance.core.common.Resource
import com.attendancehalim.smartattendance.data.local.dao.WorkerDao
import com.attendancehalim.smartattendance.data.local.entity.WorkerEntity
import com.attendancehalim.smartattendance.data.local.session.SessionManager
import com.attendancehalim.smartattendance.data.remote.api.AppsScriptApiService
import com.attendancehalim.smartattendance.data.remote.dto.CreateWorkerRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.ListWorkersRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.ResetWorkerPasswordRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.SetWorkerStatusRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.UpdateWorkerRequestDto
import com.attendancehalim.smartattendance.domain.model.WorkerProfile
import com.attendancehalim.smartattendance.domain.repository.WorkerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class WorkerRepositoryImpl(
    private val workerDao: WorkerDao,
    private val apiService: AppsScriptApiService,
    private val sessionManager: SessionManager
) : WorkerRepository {

    override fun getAllWorkers(): Flow<List<WorkerProfile>> {
        return workerDao.getAllWorkers().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getWorkerById(employeeId: String): Flow<WorkerProfile?> {
        return workerDao.getWorkerById(employeeId).map { it?.toDomainModel() }
    }

    override suspend fun getWorkerDirect(employeeId: String): WorkerProfile? = withContext(Dispatchers.IO) {
        workerDao.getWorkerDirect(employeeId)?.toDomainModel()
    }

    override suspend fun refreshWorkersFromRemote(): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = sessionManager.getActiveSession()
            if (session.authToken.isBlank()) {
                return@withContext Resource.Error("Session token unavailable. Please log in.")
            }

            val response = apiService.listWorkers(ListWorkersRequestDto(token = session.authToken))
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val remoteWorkers = response.body()?.data ?: emptyList()
                for (w in remoteWorkers) {
                    val id = w.resolvedId
                    if (id.isNotBlank()) {
                        val existing = workerDao.getWorkerDirect(id)
                        val entity = WorkerEntity(
                            employeeId = id,
                            fullName = w.resolvedName,
                            mobileNumber = w.mobileNumber ?: "",
                            workplaceName = w.resolvedWorkplace,
                            designation = w.designation ?: "Staff",
                            photoUrl = existing?.photoUrl ?: "",
                            joiningDate = w.joiningDate ?: "2026-01-15",
                            isActive = w.resolvedIsActive,
                            password = existing?.password ?: "",
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                        workerDao.insertWorker(entity)
                    }
                }
                Resource.Success(Unit)
            } else {
                val msg = response.body()?.message ?: "Failed to refresh worker list from server."
                Resource.Error(msg)
            }
        } catch (e: IOException) {
            Resource.Error("Network error while syncing workers. Using cached data.")
        } catch (e: Exception) {
            Resource.Error("Error syncing workers: ${e.localizedMessage}", e)
        }
    }

    override suspend fun addWorker(
        fullName: String,
        mobileNumber: String,
        workplaceName: String,
        designation: String,
        joiningDate: String,
        isActive: Boolean
    ): Resource<WorkerProfile> = withContext(Dispatchers.IO) {
        try {
            val trimmedName = fullName.trim()
            val trimmedMobile = mobileNumber.trim()

            if (trimmedName.isBlank()) {
                return@withContext Resource.Error("Employee name cannot be empty")
            }
            if (trimmedMobile.isBlank()) {
                return@withContext Resource.Error("Mobile number cannot be empty")
            }

            val session = sessionManager.getActiveSession()
            if (session.authToken.isBlank()) {
                return@withContext Resource.Error("Admin authorization token missing. Please log in again.")
            }

            // Call backend API — backend generates authoritative sequential ID (EMP-xxxx) and validates mobile uniqueness
            val requestDto = CreateWorkerRequestDto(
                token = session.authToken,
                fullName = trimmedName,
                mobileNumber = trimmedMobile,
                workplaceName = workplaceName.trim().ifBlank { "Headquarters" },
                designation = designation.trim().ifBlank { "Staff" },
                joiningDate = joiningDate.trim().ifBlank { "2026-01-15" },
                isActive = isActive
            )

            val response = apiService.createWorker(requestDto)
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val workerDto = response.body()?.data
                    ?: return@withContext Resource.Error("Malformed create worker response from server.")

                val serverGeneratedId = workerDto.resolvedId
                val entity = WorkerEntity(
                    employeeId = serverGeneratedId,
                    fullName = workerDto.resolvedName,
                    mobileNumber = workerDto.mobileNumber ?: trimmedMobile,
                    workplaceName = workerDto.resolvedWorkplace,
                    designation = workerDto.designation ?: "Staff",
                    photoUrl = "",
                    joiningDate = workerDto.joiningDate ?: joiningDate,
                    isActive = workerDto.resolvedIsActive,
                    password = "",
                    createdAt = System.currentTimeMillis()
                )

                workerDao.insertWorker(entity)
                Resource.Success(entity.toDomainModel())
            } else {
                val errorCode = response.body()?.errorCode ?: ""
                val serverMsg = response.body()?.message
                val msg = when (errorCode) {
                    "DUPLICATE_MOBILE_NUMBER" -> "This mobile number is already registered to another worker."
                    "INVALID_MOBILE_NUMBER" -> "Invalid mobile number format. Must be at least 10 digits."
                    "FORBIDDEN" -> "Administrator privileges required to create employee."
                    else -> serverMsg ?: "Failed to create worker on server."
                }
                Resource.Error(msg)
            }
        } catch (e: IOException) {
            Resource.Error("Unable to reach backend. Creating workers requires an active internet connection.")
        } catch (e: Exception) {
            Resource.Error("Failed to add employee: ${e.localizedMessage}", e)
        }
    }

    override suspend fun updateWorker(worker: WorkerProfile): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = sessionManager.getActiveSession()
            if (session.authToken.isNotBlank()) {
                val request = UpdateWorkerRequestDto(
                    token = session.authToken,
                    employeeId = worker.employeeId,
                    fullName = worker.fullName.trim(),
                    mobileNumber = worker.mobileNumber.trim(),
                    workplaceName = worker.workplaceName.trim(),
                    designation = worker.designation.trim(),
                    joiningDate = worker.joiningDate.trim(),
                    isActive = worker.isActive
                )
                val response = apiService.updateWorker(request)
                if (!response.isSuccessful || response.body()?.isSuccess != true) {
                    val msg = response.body()?.message ?: "Failed to update worker on server."
                    return@withContext Resource.Error(msg)
                }
            }

            val existing = workerDao.getWorkerDirect(worker.employeeId)
            val updatedEntity = (existing ?: WorkerEntity(
                employeeId = worker.employeeId,
                fullName = worker.fullName,
                mobileNumber = worker.mobileNumber,
                workplaceName = worker.workplaceName,
                designation = worker.designation,
                photoUrl = worker.photoUrl,
                joiningDate = worker.joiningDate,
                isActive = worker.isActive,
                password = "",
                createdAt = System.currentTimeMillis()
            )).copy(
                fullName = worker.fullName.trim(),
                mobileNumber = worker.mobileNumber.trim(),
                workplaceName = worker.workplaceName.trim(),
                designation = worker.designation.trim(),
                joiningDate = worker.joiningDate.trim(),
                isActive = worker.isActive
            )

            workerDao.updateWorker(updatedEntity)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error updating worker on server.")
        } catch (e: Exception) {
            Resource.Error("Failed to update employee: ${e.localizedMessage}", e)
        }
    }

    override suspend fun toggleWorkerStatus(
        employeeId: String,
        isActive: Boolean
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = sessionManager.getActiveSession()
            if (session.authToken.isNotBlank()) {
                val request = SetWorkerStatusRequestDto(
                    token = session.authToken,
                    employeeId = employeeId,
                    isActive = isActive
                )
                val response = apiService.setWorkerStatus(request)
                if (!response.isSuccessful || response.body()?.isSuccess != true) {
                    val msg = response.body()?.message ?: "Failed to update status on server."
                    return@withContext Resource.Error(msg)
                }
            }

            workerDao.updateWorkerStatus(employeeId, isActive)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error changing worker status on server.")
        } catch (e: Exception) {
            Resource.Error("Failed to update status: ${e.localizedMessage}", e)
        }
    }

    override suspend fun resetWorkerPassword(employeeId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = sessionManager.getActiveSession()
            if (session.authToken.isNotBlank()) {
                val request = ResetWorkerPasswordRequestDto(
                    token = session.authToken,
                    employeeId = employeeId
                )
                val response = apiService.resetWorkerPassword(request)
                if (!response.isSuccessful || response.body()?.isSuccess != true) {
                    val msg = response.body()?.message ?: "Failed to reset password on server."
                    return@withContext Resource.Error(msg)
                }
            }

            workerDao.updateWorkerPassword(employeeId, "")
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error resetting password on server.")
        } catch (e: Exception) {
            Resource.Error("Failed to reset password: ${e.localizedMessage}", e)
        }
    }

    override suspend fun generateNextEmployeeId(): String = withContext(Dispatchers.IO) {
        val workers = workerDao.getAllWorkersList()
        val pattern = Regex("EMP-(\\d+)", RegexOption.IGNORE_CASE)
        var maxIdNum = 0

        for (w in workers) {
            val match = pattern.find(w.employeeId)
            if (match != null) {
                val num = match.groupValues[1].toIntOrNull() ?: 0
                if (num > maxIdNum) {
                    maxIdNum = num
                }
            }
        }

        val nextNum = maxIdNum + 1
        String.format(Locale.US, "EMP-%04d", nextNum)
    }

    override suspend fun seedInitialWorkersIfEmpty() = withContext(Dispatchers.IO) {
        try {
            val existing = workerDao.getAllWorkersList()
            if (existing.isEmpty()) {
                refreshWorkersFromRemote()
            }
        } catch (_: Exception) {}
    }
}
