package com.attendancehalim.smartattendance.data.repository

import android.content.Context
import com.attendancehalim.smartattendance.core.common.Resource
import com.attendancehalim.smartattendance.core.network.NetworkMonitor
import com.attendancehalim.smartattendance.core.util.DateTimeUtils
import com.attendancehalim.smartattendance.data.local.dao.AttendanceDao
import com.attendancehalim.smartattendance.data.local.entity.AttendanceEntity
import com.attendancehalim.smartattendance.data.local.session.SessionManager
import com.attendancehalim.smartattendance.data.remote.api.AppsScriptApiService
import com.attendancehalim.smartattendance.data.remote.dto.ManualAttendanceRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.PunchRequestDto
import com.attendancehalim.smartattendance.data.remote.dto.QueryAttendanceRequestDto
import com.attendancehalim.smartattendance.data.sync.AttendanceSyncWorker
import com.attendancehalim.smartattendance.domain.model.AttendanceRecord
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.LocationDetails
import com.attendancehalim.smartattendance.domain.model.SyncStatus
import com.attendancehalim.smartattendance.domain.model.UserRole
import com.attendancehalim.smartattendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class AttendanceRepositoryImpl(
    private val context: Context,
    private val attendanceDao: AttendanceDao,
    private val apiService: AppsScriptApiService,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
) : AttendanceRepository {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        // Automatically sync pending attendance records when network connects
        coroutineScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) {
                    try {
                        val session = sessionManager.getActiveSession()
                        if (session.authToken.isNotBlank()) {
                            syncPendingAttendance()
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    override suspend fun recordAttendance(
        employeeId: String,
        employeeName: String,
        type: AttendanceType,
        location: LocationDetails
    ): Resource<AttendanceRecord> = withContext(Dispatchers.IO) {
        try {
            val currentDate = DateTimeUtils.getCurrentDate()
            val existingList = attendanceDao.getAttendanceForWorkerAndDateDirect(employeeId, currentDate)
            val alreadyPunched = existingList.firstOrNull { it.attendanceType == type }
            if (alreadyPunched != null) {
                return@withContext Resource.Success(alreadyPunched.toDomainModel())
            }

            val recordId = UUID.randomUUID().toString()
            val currentTime = DateTimeUtils.getCurrentTime()
            val timestamp = System.currentTimeMillis()

            // 1. SAVE LOCALLY FIRST (Immediate Room commit for instant response)
            val initialEntity = AttendanceEntity(
                id = recordId,
                employeeId = employeeId,
                employeeName = employeeName,
                date = currentDate,
                time = currentTime,
                attendanceType = type,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                localArea = location.addressName,
                syncStatus = SyncStatus.PENDING,
                createdAt = timestamp,
                syncedAt = null
            )
            attendanceDao.insertAttendance(initialEntity)

            val savedEntity = attendanceDao.getAttendanceById(recordId) ?: initialEntity

            // 2. DISPATCH BACKGROUND ASYNC SYNC (Non-blocking)
            coroutineScope.launch {
                syncAttendanceImmediately(recordId)
            }

            Resource.Success(savedEntity.toDomainModel())
        } catch (e: Exception) {
            Resource.Error("Failed to save attendance record: ${e.localizedMessage}", e)
        }
    }

    override suspend fun syncAttendanceImmediately(attendanceId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = attendanceDao.getAttendanceById(attendanceId)
                ?: return@withContext Resource.Error("Attendance record not found")

            if (entity.syncStatus == SyncStatus.SYNCED) {
                return@withContext Resource.Success(Unit)
            }

            val session = sessionManager.getActiveSession()
            if (session.authToken.isBlank()) {
                AttendanceSyncWorker.scheduleImmediateSync(context)
                return@withContext Resource.Error("Authentication token is blank")
            }

            val actionName = when (entity.attendanceType) {
                AttendanceType.PUNCH_IN -> "createPunchIn"
                AttendanceType.PUNCH_OUT -> "createPunchOut"
                AttendanceType.MANUAL -> "createManualAttendance"
            }

            val response = if (entity.attendanceType == AttendanceType.MANUAL) {
                apiService.createManualAttendance(
                    ManualAttendanceRequestDto(
                        action = "createManualAttendance",
                        token = session.authToken,
                        employeeId = entity.employeeId,
                        date = entity.date,
                        inTime = entity.time,
                        outTime = null,
                        notes = entity.notes,
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        accuracy = entity.accuracy,
                        localArea = entity.localArea
                    )
                )
            } else {
                apiService.createPunch(
                    PunchRequestDto(
                        action = actionName,
                        token = session.authToken,
                        attendanceId = entity.id,
                        employeeId = entity.employeeId,
                        employeeName = entity.employeeName,
                        attendanceType = entity.attendanceType.name,
                        date = entity.date,
                        time = entity.time,
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        accuracy = entity.accuracy,
                        localArea = entity.localArea,
                        timestamp = entity.createdAt,
                        notes = entity.notes
                    )
                )
            }

            val isSuccess = response.isSuccessful && (
                response.body()?.isSuccess == true ||
                response.body()?.message?.contains("Idempotent", ignoreCase = true) == true
            )

            if (isSuccess) {
                attendanceDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                android.util.Log.d(
                    "ATTENDANCE_SYNC",
                    "attendanceId=${entity.id} workerId=${entity.employeeId} status=PENDING_TO_SYNCED"
                )
                Resource.Success(Unit)
            } else {
                AttendanceSyncWorker.scheduleImmediateSync(context)
                Resource.Error(response.body()?.message ?: "Sync response unconfirmed")
            }
        } catch (e: Exception) {
            AttendanceSyncWorker.scheduleImmediateSync(context)
            Resource.Error("Immediate sync failed: ${e.localizedMessage}", e)
        }
    }

    override fun getAttendanceForWorker(employeeId: String): Flow<List<AttendanceRecord>> {
        // Trigger remote sync in background if online
        coroutineScope.launch {
            try {
                if (networkMonitor.isCurrentlyOnline()) {
                    val session = sessionManager.getActiveSession()
                    if (session.authToken.isNotBlank()) {
                        val response = apiService.getWorkerAttendance(
                            QueryAttendanceRequestDto(
                                action = "getWorkerAttendance",
                                token = session.authToken,
                                workerId = employeeId
                            )
                        )
                        if (response.isSuccessful && response.body()?.isSuccess == true) {
                            val items = response.body()?.data ?: emptyList()
                            for (item in items) {
                                val id = item.resolvedId
                                if (id.isNotBlank()) {
                                    val existing = attendanceDao.getAttendanceById(id)
                                    val entity = AttendanceEntity(
                                        id = id,
                                        employeeId = item.resolvedEmployeeId,
                                        employeeName = item.resolvedEmployeeName,
                                        date = item.date ?: DateTimeUtils.getCurrentDate(),
                                        time = item.time ?: DateTimeUtils.getCurrentTime(),
                                        attendanceType = AttendanceType.fromString(item.attendanceType ?: "PUNCH_IN"),
                                        latitude = item.latitude ?: 0.0,
                                        longitude = item.longitude ?: 0.0,
                                        accuracy = item.accuracy ?: 0f,
                                        localArea = item.localArea ?: "",
                                        syncStatus = SyncStatus.SYNCED,
                                        createdByAdminId = item.createdByAdminId,
                                        createdByAdminName = item.createdByAdminName,
                                        notes = item.notes,
                                        createdAt = existing?.createdAt ?: item.resolvedTimestamp,
                                        syncedAt = System.currentTimeMillis()
                                    )
                                    attendanceDao.insertAttendance(entity)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return attendanceDao.getAttendanceForWorker(employeeId).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getTodayAttendanceForWorker(
        employeeId: String,
        date: String
    ): Flow<List<AttendanceRecord>> {
        return attendanceDao.getTodayAttendanceForWorker(employeeId, date).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getAllAttendance(): Flow<List<AttendanceRecord>> {
        // Trigger background refresh from backend for Admin if online
        coroutineScope.launch {
            try {
                if (networkMonitor.isCurrentlyOnline()) {
                    val session = sessionManager.getActiveSession()
                    if (session.role == UserRole.ADMIN && session.authToken.isNotBlank()) {
                        val response = apiService.getAllAttendance(
                            QueryAttendanceRequestDto(
                                action = "getAllAttendance",
                                token = session.authToken
                            )
                        )
                        if (response.isSuccessful && response.body()?.isSuccess == true) {
                            val items = response.body()?.data ?: emptyList()
                            for (item in items) {
                                val id = item.resolvedId
                                if (id.isNotBlank()) {
                                    val existing = attendanceDao.getAttendanceById(id)
                                    val entity = AttendanceEntity(
                                        id = id,
                                        employeeId = item.resolvedEmployeeId,
                                        employeeName = item.resolvedEmployeeName,
                                        date = item.date ?: DateTimeUtils.getCurrentDate(),
                                        time = item.time ?: DateTimeUtils.getCurrentTime(),
                                        attendanceType = AttendanceType.fromString(item.attendanceType ?: "PUNCH_IN"),
                                        latitude = item.latitude ?: 0.0,
                                        longitude = item.longitude ?: 0.0,
                                        accuracy = item.accuracy ?: 0f,
                                        localArea = item.localArea ?: "",
                                        syncStatus = SyncStatus.SYNCED,
                                        createdByAdminId = item.createdByAdminId,
                                        createdByAdminName = item.createdByAdminName,
                                        notes = item.notes,
                                        createdAt = existing?.createdAt ?: item.resolvedTimestamp,
                                        syncedAt = System.currentTimeMillis()
                                    )
                                    attendanceDao.insertAttendance(entity)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return attendanceDao.getAllAttendance().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>> {
        // Trigger remote sync for Admin
        coroutineScope.launch {
            try {
                if (networkMonitor.isCurrentlyOnline()) {
                    val session = sessionManager.getActiveSession()
                    if (session.role == UserRole.ADMIN && session.authToken.isNotBlank()) {
                        refreshTodayAttendance()
                    }
                }
            } catch (_: Exception) {}
        }

        return attendanceDao.getAttendanceForDate(date).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override suspend fun refreshTodayAttendance(): Resource<List<AttendanceRecord>> = withContext(Dispatchers.IO) {
        try {
            val session = sessionManager.getActiveSession()
            if (session.authToken.isBlank()) {
                return@withContext Resource.Error("Session token missing")
            }

            val response = apiService.getTodayAttendance(
                QueryAttendanceRequestDto(
                    action = "getTodayAttendance",
                    token = session.authToken
                )
            )

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val items = response.body()?.data ?: emptyList()
                val resultList = mutableListOf<AttendanceRecord>()

                for (item in items) {
                    val id = item.resolvedId
                    if (id.isNotBlank()) {
                        val existing = attendanceDao.getAttendanceById(id)
                        val entity = AttendanceEntity(
                            id = id,
                            employeeId = item.resolvedEmployeeId,
                            employeeName = item.resolvedEmployeeName,
                            date = item.date ?: DateTimeUtils.getCurrentDate(),
                            time = item.time ?: DateTimeUtils.getCurrentTime(),
                            attendanceType = AttendanceType.fromString(item.attendanceType ?: "PUNCH_IN"),
                            latitude = item.latitude ?: 0.0,
                            longitude = item.longitude ?: 0.0,
                            accuracy = item.accuracy ?: 0f,
                            localArea = item.localArea ?: "",
                            syncStatus = SyncStatus.SYNCED,
                            createdByAdminId = item.createdByAdminId,
                            createdByAdminName = item.createdByAdminName,
                            notes = item.notes,
                            createdAt = existing?.createdAt ?: item.resolvedTimestamp,
                            syncedAt = System.currentTimeMillis()
                        )
                        attendanceDao.insertAttendance(entity)
                        resultList.add(entity.toDomainModel())
                    }
                }

                Resource.Success(resultList)
            } else {
                Resource.Error(response.body()?.message ?: "Failed to fetch today attendance from server")
            }
        } catch (e: Exception) {
            Resource.Error("Remote attendance fetch error: ${e.localizedMessage}", e)
        }
    }

    override suspend fun recordManualAttendance(
        employeeId: String,
        employeeName: String,
        date: String,
        inTime: String,
        outTime: String?,
        notes: String?,
        adminId: String,
        adminName: String,
        location: LocationDetails?
    ): Resource<AttendanceRecord> = withContext(Dispatchers.IO) {
        try {
            val recordId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val localAreaName = location?.addressName?.ifBlank { "Manual Entry by Admin" } ?: "Manual Entry by Admin"
            val lat = location?.latitude ?: 0.0
            val lng = location?.longitude ?: 0.0
            val acc = location?.accuracy ?: 0f

            val inEntity = AttendanceEntity(
                id = recordId,
                employeeId = employeeId,
                employeeName = employeeName,
                date = date,
                time = inTime,
                attendanceType = AttendanceType.MANUAL,
                latitude = lat,
                longitude = lng,
                accuracy = acc,
                localArea = localAreaName,
                syncStatus = SyncStatus.PENDING,
                createdByAdminId = adminId,
                createdByAdminName = adminName,
                notes = notes,
                createdAt = timestamp,
                syncedAt = null
            )
            attendanceDao.insertAttendance(inEntity)

            var outRecordId: String? = null
            if (!outTime.isNullOrBlank()) {
                val newOutId = UUID.randomUUID().toString()
                outRecordId = newOutId
                val outEntity = AttendanceEntity(
                    id = newOutId,
                    employeeId = employeeId,
                    employeeName = employeeName,
                    date = date,
                    time = outTime,
                    attendanceType = AttendanceType.PUNCH_OUT,
                    latitude = lat,
                    longitude = lng,
                    accuracy = acc,
                    localArea = localAreaName,
                    syncStatus = SyncStatus.PENDING,
                    createdByAdminId = adminId,
                    createdByAdminName = adminName,
                    notes = notes,
                    createdAt = timestamp + 1000,
                    syncedAt = null
                )
                attendanceDao.insertAttendance(outEntity)
            }

            // Sync with backend if online
            coroutineScope.launch {
                syncAttendanceImmediately(recordId)
                if (outRecordId != null) {
                    syncAttendanceImmediately(outRecordId)
                }
            }

            Resource.Success(inEntity.toDomainModel())
        } catch (e: Exception) {
            Resource.Error("Failed to save manual attendance: ${e.localizedMessage}", e)
        }
    }

    override suspend fun syncPendingAttendance(): Resource<Int> = withContext(Dispatchers.IO) {
        try {
            val pendingList = attendanceDao.getPendingSyncRecords()
            if (pendingList.isEmpty()) {
                return@withContext Resource.Success(0)
            }

            val session = sessionManager.getActiveSession()
            if (session.authToken.isBlank()) {
                return@withContext Resource.Error("Session token missing for synchronization")
            }

            var syncedCount = 0
            for (entity in pendingList) {
                try {
                    val actionName = when (entity.attendanceType) {
                        AttendanceType.PUNCH_IN -> "createPunchIn"
                        AttendanceType.PUNCH_OUT -> "createPunchOut"
                        AttendanceType.MANUAL -> "createManualAttendance"
                    }

                    val response = if (entity.attendanceType == AttendanceType.MANUAL) {
                        apiService.createManualAttendance(
                            ManualAttendanceRequestDto(
                                action = "createManualAttendance",
                                token = session.authToken,
                                employeeId = entity.employeeId,
                                date = entity.date,
                                inTime = entity.time,
                                outTime = null,
                                notes = entity.notes,
                                latitude = entity.latitude,
                                longitude = entity.longitude,
                                accuracy = entity.accuracy,
                                localArea = entity.localArea
                            )
                        )
                    } else {
                        apiService.createPunch(
                            PunchRequestDto(
                                action = actionName,
                                token = session.authToken,
                                attendanceId = entity.id,
                                employeeId = entity.employeeId,
                                employeeName = entity.employeeName,
                                attendanceType = entity.attendanceType.name,
                                date = entity.date,
                                time = entity.time,
                                latitude = entity.latitude,
                                longitude = entity.longitude,
                                accuracy = entity.accuracy,
                                localArea = entity.localArea,
                                timestamp = entity.createdAt,
                                notes = entity.notes
                            )
                        )
                    }

                    val isSuccess = response.isSuccessful && (
                        response.body()?.isSuccess == true ||
                        response.body()?.message?.contains("Idempotent", ignoreCase = true) == true
                    )

                    if (isSuccess) {
                        attendanceDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                        syncedCount++
                        android.util.Log.d(
                            "ATTENDANCE_SYNC",
                            "attendanceId=${entity.id} workerId=${entity.employeeId} status=PENDING_TO_SYNCED"
                        )
                    }
                } catch (_: Exception) {
                    // Continue to next item
                }
            }

            Resource.Success(syncedCount)
        } catch (e: Exception) {
            Resource.Error("Sync failed: ${e.localizedMessage}", e)
        }
    }

    override fun getPendingCount(): Flow<Int> {
        return attendanceDao.getPendingCount()
    }
}
