package com.attendancehalim.smartattendance.data.remote.api

import com.attendancehalim.smartattendance.data.remote.dto.ApiResponseDto
import com.attendancehalim.smartattendance.data.remote.dto.BatchSyncRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.CreateWorkerRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.GetWorkerRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.HealthCheckResponseDto
import com.attendancehalim.smartattendance.data.remote.dto.ListWorkersRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.LoginRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.LoginResponseDto
import com.attendancehalim.smartattendance.data.remote.dto.ManualAttendanceRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.PunchRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.QueryAttendanceRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.RemoteAttendanceItemDto
import com.attendancehalim.smartattendance.data.remote.dto.ResetWorkerPasswordRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.SetWorkerStatusRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.UpdateWorkerRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.WorkerDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit Interface for SMART ATTENDANCE Google Apps Script Backend Engine.
 * All core operations dispatch via HTTP POST / GET to the deployed Web App `exec` URL.
 */
interface AppsScriptApiService {

    // ========================================================================
    // HEALTH CHECK & DIAGNOSTICS
    // ========================================================================

    @GET("exec")
    suspend fun healthCheckGet(
        @Query("action") action: String = "healthCheck"
    ): Response<ApiResponseDto<HealthCheckResponseDto>>

    @POST("exec")
    suspend fun healthCheckPost(
        @Body request: Map<String, String> = mapOf("action" to "healthCheck")
    ): Response<ApiResponseDto<HealthCheckResponseDto>>

    // ========================================================================
    // AUTHENTICATION (Public Endpoints)
    // ========================================================================

    @POST("exec")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<ApiResponseDto<LoginResponseDto>>

    // ========================================================================
    // WORKER MANAGEMENT (Admin Authorized)
    // ========================================================================

    @POST("exec")
    suspend fun listWorkers(
        @Body request: ListWorkersRequestDto
    ): Response<ApiResponseDto<List<WorkerDto>>>

    @POST("exec")
    suspend fun getWorker(
        @Body request: GetWorkerRequestDto
    ): Response<ApiResponseDto<WorkerDto>>

    @POST("exec")
    suspend fun createWorker(
        @Body request: CreateWorkerRequestDto
    ): Response<ApiResponseDto<WorkerDto>>

    @POST("exec")
    suspend fun updateWorker(
        @Body request: UpdateWorkerRequestDto
    ): Response<ApiResponseDto<Map<String, Any>>>

    @POST("exec")
    suspend fun setWorkerStatus(
        @Body request: SetWorkerStatusRequestDto
    ): Response<ApiResponseDto<Map<String, Any>>>

    @POST("exec")
    suspend fun resetWorkerPassword(
        @Body request: ResetWorkerPasswordRequestDto
    ): Response<ApiResponseDto<Map<String, Any>>>

    // ========================================================================
    // ATTENDANCE OPERATIONS (Token Protected)
    // ========================================================================

    @POST("exec")
    suspend fun createPunch(
        @Body request: PunchRequestDto
    ): Response<ApiResponseDto<Map<String, Any>>>

    @POST("exec")
    suspend fun createManualAttendance(
        @Body request: ManualAttendanceRequestDto
    ): Response<ApiResponseDto<Map<String, Any>>>

    @POST("exec")
    suspend fun getWorkerAttendance(
        @Body request: QueryAttendanceRequestDto
    ): Response<ApiResponseDto<List<RemoteAttendanceItemDto>>>

    @POST("exec")
    suspend fun getAllAttendance(
        @Body request: QueryAttendanceRequestDto
    ): Response<ApiResponseDto<List<RemoteAttendanceItemDto>>>

    @POST("exec")
    suspend fun getTodayAttendance(
        @Body request: QueryAttendanceRequestDto
    ): Response<ApiResponseDto<List<RemoteAttendanceItemDto>>>

    // ========================================================================
    // OFFLINE SYNC (Token Protected)
    // ========================================================================

    @POST("exec")
    suspend fun syncAttendance(
        @Body request: PunchRequestDto
    ): Response<ApiResponseDto<Map<String, Any>>>

    @POST("exec")
    suspend fun batchSyncAttendance(
        @Body request: BatchSyncRequestDto
    ): Response<ApiResponseDto<Map<String, Any>>>
}
