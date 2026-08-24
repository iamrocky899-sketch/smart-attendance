package com.attendancehalim.smartattendance.data.remote.dto

import com.google.gson.annotations.SerializedName

// ============================================================================
// AUTHENTICATION DTOS
// ============================================================================

data class LoginRequestDto(
    @SerializedName("action")
    val action: String = "workerLogin", // "workerLogin" or "adminLogin"
    @SerializedName("identifier")
    val identifier: String = "",
    @SerializedName("password")
    val password: String = "",
    @SerializedName("username")
    val username: String? = null,
    @SerializedName("role")
    val role: String? = null
)

data class LoginResponseDto(
    @SerializedName("employeeId")
    val employeeId: String? = null,
    @SerializedName("adminId")
    val adminId: String? = null,
    @SerializedName("fullName")
    val fullName: String? = null,
    @SerializedName("employeeName")
    val employeeName: String? = null,
    @SerializedName("adminName")
    val adminName: String? = null,
    @SerializedName("mobileNumber")
    val mobileNumber: String? = null,
    @SerializedName("workplace")
    val workplace: String? = null,
    @SerializedName("workplaceName")
    val workplaceName: String? = null,
    @SerializedName("designation")
    val designation: String? = null,
    @SerializedName("joiningDate")
    val joiningDate: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("role")
    val role: String? = null,
    @SerializedName("token")
    val token: String? = null
) {
    val resolvedId: String
        get() = (employeeId ?: adminId ?: "").trim()

    val resolvedName: String
        get() = (fullName ?: employeeName ?: adminName ?: resolvedId).trim()

    val resolvedWorkplace: String
        get() = (workplaceName ?: workplace ?: "Smart Facility").trim()
}

data class HealthCheckResponseDto(
    @SerializedName("service")
    val service: String? = null,
    @SerializedName("version")
    val version: String? = null,
    @SerializedName("timezone")
    val timezone: String? = null,
    @SerializedName("serverTime")
    val serverTime: String? = null
)

// ============================================================================
// WORKER MANAGEMENT DTOS (Admin Authorized)
// ============================================================================

data class WorkerDto(
    @SerializedName("employeeId")
    val employeeId: String? = null,
    @SerializedName("workerId")
    val workerId: String? = null,
    @SerializedName("fullName")
    val fullName: String? = null,
    @SerializedName("workerName")
    val workerName: String? = null,
    @SerializedName("mobileNumber")
    val mobileNumber: String? = null,
    @SerializedName("workplace")
    val workplace: String? = null,
    @SerializedName("workplaceName")
    val workplaceName: String? = null,
    @SerializedName("designation")
    val designation: String? = null,
    @SerializedName("joiningDate")
    val joiningDate: String? = null,
    @SerializedName("isActive")
    val isActive: Boolean? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("createdAt")
    val createdAt: Any? = null
) {
    val resolvedId: String
        get() = (employeeId ?: workerId ?: "").trim()

    val resolvedName: String
        get() = (fullName ?: workerName ?: "").trim()

    val resolvedWorkplace: String
        get() = (workplaceName ?: workplace ?: "Main Facility").trim()

    val resolvedIsActive: Boolean
        get() = isActive == true || status.equals("ACTIVE", ignoreCase = true)
}

data class ListWorkersRequestDto(
    @SerializedName("action")
    val action: String = "listWorkers",
    @SerializedName("token")
    val token: String
)

data class GetWorkerRequestDto(
    @SerializedName("action")
    val action: String = "getWorker",
    @SerializedName("token")
    val token: String,
    @SerializedName("workerId")
    val workerId: String
)

data class CreateWorkerRequestDto(
    @SerializedName("action")
    val action: String = "createWorker",
    @SerializedName("token")
    val token: String,
    @SerializedName("fullName")
    val fullName: String,
    @SerializedName("mobileNumber")
    val mobileNumber: String,
    @SerializedName("workplaceName")
    val workplaceName: String,
    @SerializedName("designation")
    val designation: String,
    @SerializedName("joiningDate")
    val joiningDate: String,
    @SerializedName("isActive")
    val isActive: Boolean = true
)

data class UpdateWorkerRequestDto(
    @SerializedName("action")
    val action: String = "updateWorker",
    @SerializedName("token")
    val token: String,
    @SerializedName("employeeId")
    val employeeId: String,
    @SerializedName("fullName")
    val fullName: String? = null,
    @SerializedName("mobileNumber")
    val mobileNumber: String? = null,
    @SerializedName("workplaceName")
    val workplaceName: String? = null,
    @SerializedName("designation")
    val designation: String? = null,
    @SerializedName("joiningDate")
    val joiningDate: String? = null,
    @SerializedName("isActive")
    val isActive: Boolean? = null
)

data class SetWorkerStatusRequestDto(
    @SerializedName("action")
    val action: String = "setWorkerStatus",
    @SerializedName("token")
    val token: String,
    @SerializedName("employeeId")
    val employeeId: String,
    @SerializedName("isActive")
    val isActive: Boolean
)

data class ResetWorkerPasswordRequestDto(
    @SerializedName("action")
    val action: String = "resetWorkerPassword",
    @SerializedName("token")
    val token: String,
    @SerializedName("employeeId")
    val employeeId: String
)

// ============================================================================
// ATTENDANCE OPERATION DTOS (Token Protected)
// ============================================================================

data class PunchRequestDto(
    @SerializedName("action")
    val action: String = "createPunchIn", // "createPunchIn", "createPunchOut", "recordAttendance", "syncAttendance"
    @SerializedName("token")
    val token: String = "",
    @SerializedName("attendanceId")
    val attendanceId: String,
    @SerializedName("employeeId")
    val employeeId: String,
    @SerializedName("employeeName")
    val employeeName: String? = null,
    @SerializedName("attendanceType")
    val attendanceType: String,
    @SerializedName("date")
    val date: String? = null,
    @SerializedName("time")
    val time: String? = null,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("accuracy")
    val accuracy: Float,
    @SerializedName("localArea")
    val localArea: String,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("notes")
    val notes: String? = null
)

data class ManualAttendanceRequestDto(
    @SerializedName("action")
    val action: String = "createManualAttendance",
    @SerializedName("token")
    val token: String,
    @SerializedName("employeeId")
    val employeeId: String,
    @SerializedName("date")
    val date: String,
    @SerializedName("inTime")
    val inTime: String,
    @SerializedName("outTime")
    val outTime: String? = null,
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("latitude")
    val latitude: Double? = 0.0,
    @SerializedName("longitude")
    val longitude: Double? = 0.0,
    @SerializedName("accuracy")
    val accuracy: Float? = 0f,
    @SerializedName("localArea")
    val localArea: String? = null
)

data class QueryAttendanceRequestDto(
    @SerializedName("action")
    val action: String = "getWorkerAttendance", // "getWorkerAttendance", "getAllAttendance", "getTodayAttendance"
    @SerializedName("token")
    val token: String,
    @SerializedName("workerId")
    val workerId: String? = null,
    @SerializedName("employeeId")
    val employeeId: String? = null,
    @SerializedName("date")
    val date: String? = null,
    @SerializedName("month")
    val month: String? = null
)

data class BatchSyncRequestDto(
    @SerializedName("action")
    val action: String = "syncPendingAttendance",
    @SerializedName("token")
    val token: String,
    @SerializedName("records")
    val records: List<PunchRequestDto>
)

data class RemoteAttendanceItemDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("attendanceId")
    val attendanceId: String? = null,
    @SerializedName("employeeId")
    val employeeId: String? = null,
    @SerializedName("workerId")
    val workerId: String? = null,
    @SerializedName("employeeName")
    val employeeName: String? = null,
    @SerializedName("workerName")
    val workerName: String? = null,
    @SerializedName("attendanceType")
    val attendanceType: String? = null,
    @SerializedName("date")
    val date: String? = null,
    @SerializedName("time")
    val time: String? = null,
    @SerializedName("timestamp")
    val timestamp: Any? = null,
    @SerializedName("latitude")
    val latitude: Double? = null,
    @SerializedName("longitude")
    val longitude: Double? = null,
    @SerializedName("accuracy")
    val accuracy: Float? = null,
    @SerializedName("localArea")
    val localArea: String? = null,
    @SerializedName("syncSource")
    val syncSource: String? = null,
    @SerializedName("createdByAdminId")
    val createdByAdminId: String? = null,
    @SerializedName("createdByAdminName")
    val createdByAdminName: String? = null,
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("createdAt")
    val createdAt: Any? = null
) {
    val resolvedId: String
        get() = (attendanceId ?: id ?: "").trim()

    val resolvedEmployeeId: String
        get() = (employeeId ?: workerId ?: "").trim()

    val resolvedEmployeeName: String
        get() = (employeeName ?: workerName ?: resolvedEmployeeId).trim()

    val resolvedTimestamp: Long
        get() = when (val t = timestamp) {
            is Number -> t.toLong()
            is String -> t.toLongOrNull() ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
}

// Backward-compatible alias for existing sync request
typealias AttendanceSyncRequestDto = PunchRequestDto
typealias AttendanceBatchSyncRequestDto = BatchSyncRequestDto
