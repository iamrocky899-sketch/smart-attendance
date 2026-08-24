package com.attendancehalim.smartattendance.data.repository

import com.attendancehalim.smartattendance.core.common.Resource
import com.attendancehalim.smartattendance.data.local.dao.WorkerDao
import com.attendancehalim.smartattendance.data.local.entity.WorkerEntity
import com.attendancehalim.smartattendance.data.local.session.SessionManager
import com.attendancehalim.smartattendance.data.remote.api.AppsScriptApiService
import com.attendancehalim.smartattendance.data.remote.dto.LoginRequestDto
import com.attendancehalim.smartattendance.domain.model.UserRole
import com.attendancehalim.smartattendance.domain.model.UserSession
import com.attendancehalim.smartattendance.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AuthRepositoryImpl(
    private val sessionManager: SessionManager,
    private val workerDao: WorkerDao,
    private val apiService: AppsScriptApiService
) : AuthRepository {

    override suspend fun loginWorker(employeeId: String, password: String): Resource<UserSession> =
        withContext(Dispatchers.IO) {
            val trimmedIdentifier = employeeId.trim()
            val trimmedPassword = password.trim()

            if (trimmedIdentifier.isBlank()) {
                return@withContext Resource.Error("Please enter Employee ID or Mobile Number")
            }
            if (trimmedPassword.isBlank()) {
                return@withContext Resource.Error("Please enter your password")
            }

            try {
                val requestDto = LoginRequestDto(
                    action = "workerLogin",
                    identifier = trimmedIdentifier,
                    password = trimmedPassword
                )

                val response = apiService.login(requestDto)
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val data = response.body()?.data
                        ?: return@withContext Resource.Error("Malformed authentication response from server.")

                    val canonicalId = data.resolvedId
                    val canonicalName = data.resolvedName
                    val canonicalMobile = data.mobileNumber ?: ""
                    val canonicalWorkplace = data.resolvedWorkplace
                    val canonicalStatus = data.status ?: "ACTIVE"
                    val token = data.token ?: ""

                    if (token.isBlank()) {
                        return@withContext Resource.Error("Authentication failed: No session token provided by backend.")
                    }

                    // Save authenticated session in DataStore
                    val session = UserSession(
                        isLoggedIn = true,
                        employeeId = canonicalId,
                        userName = canonicalName,
                        mobileNumber = canonicalMobile,
                        role = UserRole.WORKER,
                        workplaceName = canonicalWorkplace,
                        photoUrl = "",
                        status = canonicalStatus,
                        authToken = token
                    )
                    sessionManager.saveSession(session)

                    // Sync/cache worker in local Room database for offline profile access
                    try {
                        val workerEntity = WorkerEntity(
                            employeeId = canonicalId,
                            fullName = canonicalName,
                            mobileNumber = canonicalMobile,
                            workplaceName = canonicalWorkplace,
                            designation = data.designation ?: "Field Staff",
                            photoUrl = "",
                            joiningDate = data.joiningDate ?: "2026-01-15",
                            isActive = canonicalStatus.equals("ACTIVE", ignoreCase = true),
                            password = "",
                            createdAt = System.currentTimeMillis()
                        )
                        workerDao.insertWorker(workerEntity)
                    } catch (_: Exception) {}

                    Resource.Success(session)
                } else {
                    val errorCode = response.body()?.errorCode ?: ""
                    val serverMessage = response.body()?.message

                    val errorMessage = when (errorCode) {
                        "WORKER_INACTIVE" -> "This worker account is deactivated. Please contact Administrator."
                        "INVALID_CREDENTIALS" -> "Invalid Employee ID / Mobile Number or Password. Please try again."
                        "WORKER_NOT_FOUND" -> "Worker account not found with the provided Employee ID or Mobile Number."
                        "MISSING_CREDENTIALS" -> "Employee ID / Mobile Number and Password are required."
                        "LOCK_TIMEOUT" -> "Server is busy processing requests. Please try again in a few moments."
                        else -> serverMessage ?: "Authentication failed. Error: ${response.code()}"
                    }

                    Resource.Error(errorMessage)
                }
            } catch (e: IOException) {
                Resource.Error("Unable to reach server. Please check your internet connection and try again.")
            } catch (e: Exception) {
                Resource.Error("Authentication error: ${e.localizedMessage ?: "Unexpected error"}", e)
            }
        }

    override suspend fun loginAdmin(username: String, password: String): Resource<UserSession> =
        withContext(Dispatchers.IO) {
            val trimmedIdentifier = username.trim()
            val trimmedPassword = password.trim()

            if (trimmedIdentifier.isBlank()) {
                return@withContext Resource.Error("Please enter Admin Mobile Number or Admin ID")
            }
            if (trimmedPassword.isBlank()) {
                return@withContext Resource.Error("Please enter Admin password")
            }

            try {
                val requestDto = LoginRequestDto(
                    action = "adminLogin",
                    identifier = trimmedIdentifier,
                    password = trimmedPassword
                )

                val response = apiService.login(requestDto)
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val data = response.body()?.data
                        ?: return@withContext Resource.Error("Malformed authentication response from server.")

                    val canonicalId = data.resolvedId
                    val canonicalName = data.resolvedName.ifBlank { "System Administrator" }
                    val canonicalMobile = data.mobileNumber ?: ""
                    val canonicalStatus = data.status ?: "ACTIVE"
                    val token = data.token ?: ""

                    if (token.isBlank()) {
                        return@withContext Resource.Error("Admin authentication failed: No session token provided.")
                    }

                    val session = UserSession(
                        isLoggedIn = true,
                        employeeId = canonicalId,
                        userName = canonicalName,
                        mobileNumber = canonicalMobile,
                        role = UserRole.ADMIN,
                        workplaceName = "Administrator Console",
                        photoUrl = "",
                        status = canonicalStatus,
                        authToken = token
                    )
                    sessionManager.saveSession(session)

                    Resource.Success(session)
                } else {
                    val errorCode = response.body()?.errorCode ?: ""
                    val serverMessage = response.body()?.message

                    val errorMessage = when (errorCode) {
                        "ADMIN_INACTIVE" -> "This Admin account is inactive. Please contact system owner."
                        "INVALID_CREDENTIALS" -> "Invalid Admin Mobile Number / ID or Password. Please try again."
                        "ADMIN_NOT_FOUND" -> "Admin account not found with the provided Mobile Number or Admin ID."
                        "MISSING_CREDENTIALS" -> "Admin identifier and Password are required."
                        "LOCK_TIMEOUT" -> "Server busy. Please try again in a few moments."
                        else -> serverMessage ?: "Admin authentication failed. Error: ${response.code()}"
                    }

                    Resource.Error(errorMessage)
                }
            } catch (e: IOException) {
                Resource.Error("Unable to reach server. Please check your internet connection and try again.")
            } catch (e: Exception) {
                Resource.Error("Admin authentication error: ${e.localizedMessage ?: "Unexpected error"}", e)
            }
        }

    override suspend fun logout(): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            sessionManager.clearSession()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Logout error: ${e.localizedMessage}", e)
        }
    }

    override suspend fun switchRole(targetRole: UserRole): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            sessionManager.updateRole(targetRole)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to update role preference: ${e.localizedMessage}", e)
        }
    }
}
