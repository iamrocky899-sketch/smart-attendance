package com.attendancehalim.smartattendance.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendancehalim.smartattendance.core.common.Resource
import com.attendancehalim.smartattendance.domain.model.WorkerProfile
import com.attendancehalim.smartattendance.domain.repository.WorkerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkerFilterStatus(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    INACTIVE("Inactive")
}

data class AdminEmployeesUiState(
    val workers: List<WorkerProfile> = emptyList(),
    val filteredWorkers: List<WorkerProfile> = emptyList(),
    val searchQuery: String = "",
    val filterStatus: WorkerFilterStatus = WorkerFilterStatus.ALL,
    val generatedNextId: String = "EMP-0001",
    val isActionLoading: Boolean = false,
    val employeeAddedSuccess: WorkerProfile? = null,
    val actionSuccessMessage: String? = null,
    val errorMessage: String? = null
)

class AdminEmployeesViewModel(
    private val workerRepository: WorkerRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterStatus = MutableStateFlow(WorkerFilterStatus.ALL)
    private val _generatedNextId = MutableStateFlow("EMP-0001")
    private val _isActionLoading = MutableStateFlow(false)
    private val _employeeAddedSuccess = MutableStateFlow<WorkerProfile?>(null)
    private val _actionSuccessMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    init {
        loadNextId()
        refreshWorkers()
    }

    fun refreshWorkers() {
        viewModelScope.launch {
            workerRepository.refreshWorkersFromRemote()
            loadNextId()
        }
    }

    val uiState: StateFlow<AdminEmployeesUiState> = combine(
        workerRepository.getAllWorkers(),
        _searchQuery,
        _filterStatus,
        _generatedNextId,
        _isActionLoading,
        _employeeAddedSuccess,
        _actionSuccessMessage,
        _errorMessage
    ) { params: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val allWorkers = params[0] as List<WorkerProfile>
        val query = params[1] as String
        val filter = params[2] as WorkerFilterStatus
        val nextId = params[3] as String
        val actionLoading = params[4] as Boolean
        val addedSuccess = params[5] as WorkerProfile?
        val successMsg = params[6] as String?
        val errorMsg = params[7] as String?

        val filtered = allWorkers.filter { worker ->
            val matchesQuery = query.isBlank() ||
                    worker.fullName.contains(query, ignoreCase = true) ||
                    worker.employeeId.contains(query, ignoreCase = true) ||
                    worker.workplaceName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                WorkerFilterStatus.ALL -> true
                WorkerFilterStatus.ACTIVE -> worker.isActive
                WorkerFilterStatus.INACTIVE -> !worker.isActive
            }

            matchesQuery && matchesFilter
        }

        AdminEmployeesUiState(
            workers = allWorkers,
            filteredWorkers = filtered,
            searchQuery = query,
            filterStatus = filter,
            generatedNextId = nextId,
            isActionLoading = actionLoading,
            employeeAddedSuccess = addedSuccess,
            actionSuccessMessage = successMsg,
            errorMessage = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdminEmployeesUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterStatusChanged(status: WorkerFilterStatus) {
        _filterStatus.value = status
    }

    fun loadNextId() {
        viewModelScope.launch {
            _generatedNextId.value = workerRepository.generateNextEmployeeId()
        }
    }

    fun addEmployee(
        fullName: String,
        mobileNumber: String,
        workplaceName: String,
        designation: String,
        joiningDate: String,
        isActive: Boolean
    ) {
        if (fullName.isBlank()) {
            _errorMessage.value = "Employee name is required"
            return
        }

        _isActionLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            when (val result = workerRepository.addWorker(
                fullName = fullName,
                mobileNumber = mobileNumber,
                workplaceName = workplaceName,
                designation = designation,
                joiningDate = joiningDate,
                isActive = isActive
            )) {
                is Resource.Success -> {
                    _isActionLoading.value = false
                    _employeeAddedSuccess.value = result.data
                    loadNextId()
                }
                is Resource.Error -> {
                    _isActionLoading.value = false
                    _errorMessage.value = result.message
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateEmployee(worker: WorkerProfile) {
        _isActionLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            when (val result = workerRepository.updateWorker(worker)) {
                is Resource.Success -> {
                    _isActionLoading.value = false
                    _actionSuccessMessage.value = "Employee updated successfully."
                }
                is Resource.Error -> {
                    _isActionLoading.value = false
                    _errorMessage.value = result.message
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun toggleWorkerStatus(employeeId: String, currentActiveState: Boolean) {
        val targetState = !currentActiveState
        _isActionLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            when (val result = workerRepository.toggleWorkerStatus(employeeId, targetState)) {
                is Resource.Success -> {
                    _isActionLoading.value = false
                    _actionSuccessMessage.value = if (targetState) "Employee activated successfully." else "Employee deactivated successfully."
                }
                is Resource.Error -> {
                    _isActionLoading.value = false
                    _errorMessage.value = result.message
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun resetPassword(employeeId: String) {
        _isActionLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            when (val result = workerRepository.resetWorkerPassword(employeeId)) {
                is Resource.Success -> {
                    _isActionLoading.value = false
                    _actionSuccessMessage.value = "Password reset successfully. Default password is set to 12345."
                }
                is Resource.Error -> {
                    _isActionLoading.value = false
                    _errorMessage.value = result.message
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearAddedSuccess() {
        _employeeAddedSuccess.value = null
    }

    fun clearMessages() {
        _actionSuccessMessage.value = null
        _errorMessage.value = null
    }
}
