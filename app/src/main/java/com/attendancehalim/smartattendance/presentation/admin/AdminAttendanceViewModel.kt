package com.attendancehalim.smartattendance.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendancehalim.smartattendance.core.common.Resource
import com.attendancehalim.smartattendance.core.util.DateTimeUtils
import com.attendancehalim.smartattendance.domain.model.AttendanceRecord
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.DailyAttendanceRegister
import com.attendancehalim.smartattendance.domain.model.LocationDetails
import com.attendancehalim.smartattendance.domain.model.SyncStatus
import com.attendancehalim.smartattendance.domain.model.UserSession
import com.attendancehalim.smartattendance.domain.model.WorkerProfile
import com.attendancehalim.smartattendance.domain.repository.AttendanceRepository
import com.attendancehalim.smartattendance.domain.repository.LocationRepository
import com.attendancehalim.smartattendance.domain.repository.SessionRepository
import com.attendancehalim.smartattendance.domain.repository.WorkerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminAttendanceUiState(
    val session: UserSession = UserSession(),
    val allRegisters: List<DailyAttendanceRegister> = emptyList(),
    val filteredRegisters: List<DailyAttendanceRegister> = emptyList(),
    val workers: List<WorkerProfile> = emptyList(),
    val searchQuery: String = "",
    val filterDate: String? = null,
    val filterMonth: String? = null,
    val selectedRegister: DailyAttendanceRegister? = null,
    val isSavingManual: Boolean = false,
    val manualConflictWarning: String? = null,
    val manualSuccessMessage: String? = null,
    val manualErrorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class AdminAttendanceViewModel(
    private val sessionRepository: SessionRepository,
    private val attendanceRepository: AttendanceRepository,
    private val workerRepository: WorkerRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterDate = MutableStateFlow<String?>(null)
    private val _filterMonth = MutableStateFlow<String?>(null)
    private val _selectedRegister = MutableStateFlow<DailyAttendanceRegister?>(null)
    private val _isSavingManual = MutableStateFlow(false)
    private val _manualConflictWarning = MutableStateFlow<String?>(null)
    private val _manualSuccessMessage = MutableStateFlow<String?>(null)
    private val _manualErrorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AdminAttendanceUiState> = sessionRepository.sessionFlow
        .flatMapLatest { session ->
            val attendanceFlow = attendanceRepository.getAllAttendance()
            val workersFlow = workerRepository.getAllWorkers()

            combine(
                attendanceFlow,
                workersFlow,
                _searchQuery,
                _filterDate,
                _filterMonth,
                _selectedRegister,
                _isSavingManual,
                _manualConflictWarning,
                _manualSuccessMessage,
                _manualErrorMessage
            ) { params: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                val rawAttendance = params[0] as List<AttendanceRecord>
                val workersList = params[1] as List<WorkerProfile>
                val query = params[2] as String
                val dateFilter = params[3] as String?
                val monthFilter = params[4] as String?
                val selected = params[5] as DailyAttendanceRegister?
                val saving = params[6] as Boolean
                val warning = params[7] as String?
                val success = params[8] as String?
                val error = params[9] as String?

                val dailyRegisters = groupRecordsIntoDailyRegisters(rawAttendance)

                val filtered = dailyRegisters.filter { reg ->
                    val matchesWorker = query.isBlank() ||
                            (reg.punchInRecord?.employeeName?.contains(query, ignoreCase = true) == true) ||
                            (reg.punchInRecord?.employeeId?.contains(query, ignoreCase = true) == true) ||
                            (reg.punchOutRecord?.employeeName?.contains(query, ignoreCase = true) == true) ||
                            (reg.punchOutRecord?.employeeId?.contains(query, ignoreCase = true) == true)

                    val matchesDate = dateFilter.isNullOrBlank() || reg.date == dateFilter
                    val matchesMonth = monthFilter.isNullOrBlank() || reg.date.startsWith(monthFilter)

                    matchesWorker && matchesDate && matchesMonth
                }

                AdminAttendanceUiState(
                    session = session,
                    allRegisters = dailyRegisters,
                    filteredRegisters = filtered,
                    workers = workersList,
                    searchQuery = query,
                    filterDate = dateFilter,
                    filterMonth = monthFilter,
                    selectedRegister = selected,
                    isSavingManual = saving,
                    manualConflictWarning = warning,
                    manualSuccessMessage = success,
                    manualErrorMessage = error
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AdminAttendanceUiState()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterDateChanged(date: String?) {
        _filterDate.value = if (date.isNullOrBlank()) null else date
    }

    fun onFilterMonthChanged(month: String?) {
        _filterMonth.value = if (month.isNullOrBlank()) null else month
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _filterDate.value = null
        _filterMonth.value = null
    }

    fun selectRegister(register: DailyAttendanceRegister) {
        _selectedRegister.value = register
    }

    fun clearSelectedRegister() {
        _selectedRegister.value = null
    }

    fun checkConflict(employeeId: String, date: String): Boolean {
        val existing = uiState.value.allRegisters.firstOrNull {
            it.date == date && (it.punchInRecord?.employeeId == employeeId || it.punchOutRecord?.employeeId == employeeId)
        }
        return existing != null
    }

    fun recordManualAttendance(
        employeeId: String,
        date: String,
        inTime: String,
        outTime: String?,
        notes: String?
    ) {
        val worker = uiState.value.workers.firstOrNull { it.employeeId == employeeId }
        val employeeName = worker?.fullName ?: employeeId

        _isSavingManual.value = true
        _manualErrorMessage.value = null
        _manualSuccessMessage.value = null

        viewModelScope.launch {
            // Optional Admin location capture
            val adminLocation = try {
                if (locationRepository.hasLocationPermission() && locationRepository.isLocationEnabled()) {
                    locationRepository.getCurrentLocation().getOrNull()
                } else null
            } catch (_: Exception) {
                null
            }

            val session = uiState.value.session
            when (val result = attendanceRepository.recordManualAttendance(
                employeeId = employeeId,
                employeeName = employeeName,
                date = date,
                inTime = inTime,
                outTime = outTime,
                notes = notes,
                adminId = session.employeeId.ifBlank { "ADMIN" },
                adminName = session.userName.ifBlank { "Administrator" },
                location = adminLocation
            )) {
                is Resource.Success -> {
                    _isSavingManual.value = false
                    _manualSuccessMessage.value = "Manual attendance recorded successfully for $employeeName on ${DateTimeUtils.formatFullDate(date)}."
                }
                is Resource.Error -> {
                    _isSavingManual.value = false
                    _manualErrorMessage.value = result.message ?: "Failed to save manual attendance."
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearManualMessages() {
        _manualSuccessMessage.value = null
        _manualErrorMessage.value = null
        _manualConflictWarning.value = null
    }

    private fun groupRecordsIntoDailyRegisters(records: List<AttendanceRecord>): List<DailyAttendanceRegister> {
        // Group by Date + EmployeeId pair
        val grouped = records.groupBy { "${it.date}_${it.employeeId}" }

        val list = grouped.map { (_, dayRecords) ->
            val first = dayRecords.first()
            val date = first.date
            val punchIn = dayRecords.firstOrNull { it.type == AttendanceType.PUNCH_IN || it.type == AttendanceType.MANUAL }
            val punchOut = dayRecords.firstOrNull { it.type == AttendanceType.PUNCH_OUT }

            val duration: String? = when {
                punchIn != null && punchOut != null -> {
                    DateTimeUtils.calculateDuration(punchIn.createdAt, punchOut.createdAt, punchIn.time, punchOut.time)
                }
                punchIn != null -> {
                    if (date == DateTimeUtils.getCurrentDate()) "In Progress" else "Incomplete"
                }
                punchOut != null -> "Punch Out Only"
                else -> null
            }

            val locationSummary = when {
                punchIn != null && punchIn.localArea.isNotBlank() && punchIn.localArea != "Location name unavailable" -> punchIn.localArea
                punchOut != null && punchOut.localArea.isNotBlank() && punchOut.localArea != "Location name unavailable" -> punchOut.localArea
                punchIn != null -> punchIn.localArea
                punchOut != null -> punchOut.localArea
                else -> "Location unavailable"
            }

            val overallSync = when {
                dayRecords.all { it.syncStatus == SyncStatus.SYNCED } -> SyncStatus.SYNCED
                dayRecords.any { it.syncStatus == SyncStatus.FAILED } -> SyncStatus.FAILED
                else -> SyncStatus.PENDING
            }

            DailyAttendanceRegister(
                date = date,
                displayDay = DateTimeUtils.formatShortDate(date),
                fullDateDisplay = DateTimeUtils.formatFullDate(date),
                monthHeader = DateTimeUtils.formatMonthYear(date),
                punchInRecord = punchIn,
                punchOutRecord = punchOut,
                workingDuration = duration,
                locationSummary = locationSummary,
                overallSyncStatus = overallSync
            )
        }

        // Sort newest first by date and creation
        return list.sortedWith(
            compareByDescending<DailyAttendanceRegister> { it.date }
                .thenByDescending { it.punchInRecord?.createdAt ?: it.punchOutRecord?.createdAt ?: 0L }
        )
    }
}
