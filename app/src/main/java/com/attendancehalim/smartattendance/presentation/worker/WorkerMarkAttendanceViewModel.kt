package com.attendancehalim.smartattendance.presentation.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendancehalim.smartattendance.core.common.Resource
import com.attendancehalim.smartattendance.core.util.DateTimeUtils
import com.attendancehalim.smartattendance.domain.model.AttendanceRecord
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.LocationDetails
import com.attendancehalim.smartattendance.domain.model.UserSession
import com.attendancehalim.smartattendance.domain.repository.AttendanceRepository
import com.attendancehalim.smartattendance.domain.repository.LocationRepository
import com.attendancehalim.smartattendance.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LocationStatusState {
    CHECKING,
    READY,
    PERMISSION_REQUIRED,
    GPS_OFF,
    UNAVAILABLE
}

data class WorkerMarkAttendanceUiState(
    val session: UserSession = UserSession(),
    val todayDateFormatted: String = "",
    val todayStatus: TodayAttendanceStatus = TodayAttendanceStatus.NOT_MARKED,
    val todayPunchInRecord: AttendanceRecord? = null,
    val todayPunchOutRecord: AttendanceRecord? = null,
    val isPermissionGranted: Boolean = false,
    val isLocationGpsEnabled: Boolean = false,
    val isLocationReady: Boolean = false,
    val locationStatus: LocationStatusState = LocationStatusState.CHECKING,
    val isCapturingLocation: Boolean = false,
    val previewLocation: LocationDetails? = null,
    val isFetchingPreview: Boolean = false,
    val errorMessage: String? = null,
    val confirmationRecord: AttendanceRecord? = null,
    val showConfirmationDialog: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkerMarkAttendanceViewModel(
    private val sessionRepository: SessionRepository,
    private val attendanceRepository: AttendanceRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _isPermissionGranted = MutableStateFlow(false)
    private val _isLocationGpsEnabled = MutableStateFlow(false)
    private val _isCapturingLocation = MutableStateFlow(false)
    private val _previewLocation = MutableStateFlow<LocationDetails?>(null)
    private val _isFetchingPreview = MutableStateFlow(false)
    private val _locationStatus = MutableStateFlow(LocationStatusState.CHECKING)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _confirmationRecord = MutableStateFlow<AttendanceRecord?>(null)
    private val _showConfirmationDialog = MutableStateFlow(false)
    private var _lastLocationFetchTime = 0L

    init {
        checkLocationAndPermissionState()
    }

    val uiState: StateFlow<WorkerMarkAttendanceUiState> = sessionRepository.sessionFlow
        .flatMapLatest { session ->
            if (session.employeeId.isBlank()) {
                flowOf(WorkerMarkAttendanceUiState(session = session))
            } else {
                val currentDate = DateTimeUtils.getCurrentDate()
                val todayRecordsFlow = attendanceRepository.getTodayAttendanceForWorker(
                    employeeId = session.employeeId,
                    date = currentDate
                )

                combine(
                    todayRecordsFlow,
                    _isPermissionGranted,
                    _isLocationGpsEnabled,
                    _locationStatus,
                    _isCapturingLocation,
                    _previewLocation,
                    _isFetchingPreview,
                    _errorMessage,
                    _confirmationRecord,
                    _showConfirmationDialog
                ) { params: Array<Any?> ->
                    @Suppress("UNCHECKED_CAST")
                    val todayRecords = params[0] as List<AttendanceRecord>
                    val permGranted = params[1] as Boolean
                    val gpsEnabled = params[2] as Boolean
                    val locStatus = params[3] as LocationStatusState
                    val capturingLoc = params[4] as Boolean
                    val previewLoc = params[5] as LocationDetails?
                    val fetchingPreview = params[6] as Boolean
                    val errorMsg = params[7] as String?
                    val confirmRec = params[8] as AttendanceRecord?
                    val showConfirm = params[9] as Boolean

                    val punchIn = todayRecords.firstOrNull { it.type == AttendanceType.PUNCH_IN || it.type == AttendanceType.MANUAL }
                    val punchOut = todayRecords.firstOrNull { it.type == AttendanceType.PUNCH_OUT }
                    val status = when {
                        punchOut != null -> TodayAttendanceStatus.PUNCHED_OUT
                        punchIn != null -> TodayAttendanceStatus.PUNCHED_IN
                        else -> TodayAttendanceStatus.NOT_MARKED
                    }

                    val effectiveLocStatus = when {
                        !permGranted -> LocationStatusState.PERMISSION_REQUIRED
                        !gpsEnabled -> LocationStatusState.GPS_OFF
                        previewLoc != null -> LocationStatusState.READY
                        fetchingPreview -> LocationStatusState.CHECKING
                        else -> locStatus
                    }

                    WorkerMarkAttendanceUiState(
                        session = session,
                        todayDateFormatted = DateTimeUtils.formatFullDate(currentDate),
                        todayStatus = status,
                        todayPunchInRecord = punchIn,
                        todayPunchOutRecord = punchOut,
                        isPermissionGranted = permGranted,
                        isLocationGpsEnabled = gpsEnabled,
                        isLocationReady = permGranted && gpsEnabled && previewLoc != null,
                        locationStatus = effectiveLocStatus,
                        isCapturingLocation = capturingLoc,
                        previewLocation = previewLoc,
                        isFetchingPreview = fetchingPreview,
                        errorMessage = errorMsg,
                        confirmationRecord = confirmRec,
                        showConfirmationDialog = showConfirm
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WorkerMarkAttendanceUiState()
        )

    fun checkLocationAndPermissionState() {
        val permGranted = locationRepository.hasLocationPermission()
        val gpsEnabled = locationRepository.isLocationEnabled()
        _isPermissionGranted.value = permGranted
        _isLocationGpsEnabled.value = gpsEnabled

        if (!permGranted) {
            _locationStatus.value = LocationStatusState.PERMISSION_REQUIRED
            return
        }
        if (!gpsEnabled) {
            _locationStatus.value = LocationStatusState.GPS_OFF
            return
        }

        // Location permission and GPS are enabled: check if we need to refresh preview
        val cacheAge = System.currentTimeMillis() - _lastLocationFetchTime
        if (_previewLocation.value == null || cacheAge > 45_000L) {
            refreshLocationPreview()
        } else {
            _locationStatus.value = LocationStatusState.READY
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _isPermissionGranted.value = granted
        val gpsEnabled = locationRepository.isLocationEnabled()
        _isLocationGpsEnabled.value = gpsEnabled

        if (!granted) {
            _locationStatus.value = LocationStatusState.PERMISSION_REQUIRED
            return
        }
        if (!gpsEnabled) {
            _locationStatus.value = LocationStatusState.GPS_OFF
            return
        }

        refreshLocationPreview()
    }

    fun refreshLocationPreview() {
        val perm = locationRepository.hasLocationPermission()
        val gps = locationRepository.isLocationEnabled()
        _isPermissionGranted.value = perm
        _isLocationGpsEnabled.value = gps

        if (!perm) {
            _locationStatus.value = LocationStatusState.PERMISSION_REQUIRED
            return
        }
        if (!gps) {
            _locationStatus.value = LocationStatusState.GPS_OFF
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isFetchingPreview.value = true
            _locationStatus.value = LocationStatusState.CHECKING

            when (val result = locationRepository.getCurrentLocation()) {
                is Resource.Success -> {
                    _previewLocation.value = result.data
                    _lastLocationFetchTime = System.currentTimeMillis()
                    _locationStatus.value = LocationStatusState.READY
                }
                is Resource.Error -> {
                    if (_previewLocation.value == null) {
                        _locationStatus.value = LocationStatusState.UNAVAILABLE
                    }
                }
                is Resource.Loading -> {}
            }
            _isFetchingPreview.value = false
        }
    }

    private var _isExecutingPunch = false

    fun punchIn() {
        if (_isCapturingLocation.value || _isExecutingPunch) return

        val state = uiState.value
        if (state.todayStatus == TodayAttendanceStatus.PUNCHED_IN) {
            _errorMessage.value = "You are already punched in today."
            return
        }
        if (state.todayStatus == TodayAttendanceStatus.PUNCHED_OUT) {
            _errorMessage.value = "Shift completed for today. Duplicate punch is not allowed."
            return
        }

        executePunch(AttendanceType.PUNCH_IN)
    }

    fun punchOut() {
        if (_isCapturingLocation.value || _isExecutingPunch) return

        val state = uiState.value
        if (state.todayStatus == TodayAttendanceStatus.NOT_MARKED) {
            _errorMessage.value = "You must punch in first before punching out."
            return
        }
        if (state.todayStatus == TodayAttendanceStatus.PUNCHED_OUT) {
            _errorMessage.value = "You have already punched out for today."
            return
        }

        executePunch(AttendanceType.PUNCH_OUT)
    }

    private fun executePunch(type: AttendanceType) {
        if (_isExecutingPunch) return
        _isExecutingPunch = true

        viewModelScope.launch {
            try {
                val permGranted = locationRepository.hasLocationPermission()
                val gpsEnabled = locationRepository.isLocationEnabled()
                _isPermissionGranted.value = permGranted
                _isLocationGpsEnabled.value = gpsEnabled

            if (!permGranted) {
                _locationStatus.value = LocationStatusState.PERMISSION_REQUIRED
                _errorMessage.value = "Location permission is required to mark attendance."
                return@launch
            }

            if (!gpsEnabled) {
                _locationStatus.value = LocationStatusState.GPS_OFF
                _errorMessage.value = "Location is OFF. Please turn on Location to mark attendance."
                return@launch
            }

            _errorMessage.value = null

            // 1. FAST PATH: Check if cached location is fresh (< 60 seconds old)
            val cacheAge = System.currentTimeMillis() - _lastLocationFetchTime
            val cachedLocation = _previewLocation.value

            val validLocation: LocationDetails = if (cachedLocation != null && cacheAge < 60_000L) {
                cachedLocation
            } else {
                // Fetch location with short timeout
                _isCapturingLocation.value = true
                val locResult = locationRepository.getCurrentLocation()
                _isCapturingLocation.value = false

                when (locResult) {
                    is Resource.Success -> {
                        val loc = locResult.data
                        _previewLocation.value = loc
                        _lastLocationFetchTime = System.currentTimeMillis()
                        _locationStatus.value = LocationStatusState.READY
                        loc
                    }
                    is Resource.Error -> {
                        _errorMessage.value = locResult.message ?: "Unable to acquire current GPS location. Please try again."
                        return@launch
                    }
                    is Resource.Loading -> return@launch
                }
            }

            // 2. Save Attendance Locally (Immediate Room insert, background async sync)
            val session = uiState.value.session
            when (val recordResult = attendanceRepository.recordAttendance(
                employeeId = session.employeeId,
                employeeName = session.userName,
                type = type,
                location = validLocation
            )) {
                is Resource.Success -> {
                    _confirmationRecord.value = recordResult.data
                    _showConfirmationDialog.value = true
                }
                is Resource.Error -> {
                    _errorMessage.value = recordResult.message ?: "Failed to save attendance record."
                }
                is Resource.Loading -> {}
            }
        } finally {
            _isExecutingPunch = false
        }
    }
}

    fun dismissConfirmation() {
        _showConfirmationDialog.value = false
        _confirmationRecord.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
