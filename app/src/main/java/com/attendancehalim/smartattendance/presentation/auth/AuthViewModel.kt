package com.attendancehalim.smartattendance.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendancehalim.smartattendance.core.common.Resource
import com.attendancehalim.smartattendance.domain.model.UserRole
import com.attendancehalim.smartattendance.domain.model.UserSession
import com.attendancehalim.smartattendance.domain.repository.AuthRepository
import com.attendancehalim.smartattendance.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val employeeId: String = "",
    val password: String = "",
    val adminUsername: String = "",
    val adminPassword: String = "",
    val selectedRole: UserRole = UserRole.WORKER,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false,
    val activeSession: UserSession? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmployeeIdChanged(employeeId: String) {
        _uiState.update { it.copy(employeeId = employeeId, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onAdminUsernameChanged(username: String) {
        _uiState.update { it.copy(adminUsername = username, errorMessage = null) }
    }

    fun onAdminPasswordChanged(password: String) {
        _uiState.update { it.copy(adminPassword = password, errorMessage = null) }
    }

    fun selectRole(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role, errorMessage = null) }
        viewModelScope.launch {
            sessionRepository.updateRole(role)
        }
    }

    fun loginWorker() {
        val state = _uiState.value
        if (state.employeeId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your Employee ID") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = authRepository.loginWorker(state.employeeId, state.password)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoginSuccess = true,
                            activeSession = result.data,
                            errorMessage = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun loginAdmin() {
        val state = _uiState.value
        if (state.adminUsername.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter admin username/email") }
            return
        }
        if (state.adminPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter admin password") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = authRepository.loginAdmin(state.adminUsername, state.adminPassword)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoginSuccess = true,
                            activeSession = result.data,
                            errorMessage = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetLoginSuccess() {
        _uiState.update { it.copy(isLoginSuccess = false) }
    }
}
