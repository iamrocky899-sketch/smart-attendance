package com.attendancehalim.smartattendance.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendancehalim.smartattendance.domain.model.UserSession
import com.attendancehalim.smartattendance.domain.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SessionUiState {
    object Loading : SessionUiState
    data class Authenticated(val session: UserSession) : SessionUiState
    object Unauthenticated : SessionUiState
}

class MainViewModel(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val sessionState: StateFlow<SessionUiState> = sessionRepository.sessionFlow
        .map { session ->
            if (session.isLoggedIn && session.employeeId.isNotBlank()) {
                if (session.isTokenExpired) {
                    viewModelScope.launch {
                        sessionRepository.clearSession()
                    }
                    SessionUiState.Unauthenticated
                } else {
                    SessionUiState.Authenticated(session)
                }
            } else {
                SessionUiState.Unauthenticated
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionUiState.Loading
        )
}
