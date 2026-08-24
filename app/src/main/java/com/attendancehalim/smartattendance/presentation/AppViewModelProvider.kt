package com.attendancehalim.smartattendance.presentation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.attendancehalim.smartattendance.SmartAttendanceApp
import com.attendancehalim.smartattendance.data.di.AppContainer
import com.attendancehalim.smartattendance.presentation.admin.AdminAttendanceViewModel
import com.attendancehalim.smartattendance.presentation.admin.AdminDashboardViewModel
import com.attendancehalim.smartattendance.presentation.admin.AdminEmployeesViewModel
import com.attendancehalim.smartattendance.presentation.auth.AuthViewModel
import com.attendancehalim.smartattendance.presentation.main.MainViewModel
import com.attendancehalim.smartattendance.presentation.worker.WorkerDashboardViewModel
import com.attendancehalim.smartattendance.presentation.worker.WorkerMarkAttendanceViewModel
import com.attendancehalim.smartattendance.presentation.worker.WorkerMyAttendanceViewModel

/**
 * Factory for instantiating ViewModels with AppContainer dependencies.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            MainViewModel(
                sessionRepository = smartAttendanceApp().container.sessionRepository
            )
        }
        initializer {
            AuthViewModel(
                authRepository = smartAttendanceApp().container.authRepository,
                sessionRepository = smartAttendanceApp().container.sessionRepository
            )
        }
        initializer {
            WorkerDashboardViewModel(
                sessionRepository = smartAttendanceApp().container.sessionRepository,
                attendanceRepository = smartAttendanceApp().container.attendanceRepository,
                authRepository = smartAttendanceApp().container.authRepository
            )
        }
        initializer {
            WorkerMarkAttendanceViewModel(
                sessionRepository = smartAttendanceApp().container.sessionRepository,
                attendanceRepository = smartAttendanceApp().container.attendanceRepository,
                locationRepository = smartAttendanceApp().container.locationRepository
            )
        }
        initializer {
            WorkerMyAttendanceViewModel(
                sessionRepository = smartAttendanceApp().container.sessionRepository,
                attendanceRepository = smartAttendanceApp().container.attendanceRepository
            )
        }
        initializer {
            AdminDashboardViewModel(
                sessionRepository = smartAttendanceApp().container.sessionRepository,
                workerRepository = smartAttendanceApp().container.workerRepository,
                attendanceRepository = smartAttendanceApp().container.attendanceRepository,
                authRepository = smartAttendanceApp().container.authRepository
            )
        }
        initializer {
            AdminEmployeesViewModel(
                workerRepository = smartAttendanceApp().container.workerRepository
            )
        }
        initializer {
            AdminAttendanceViewModel(
                sessionRepository = smartAttendanceApp().container.sessionRepository,
                attendanceRepository = smartAttendanceApp().container.attendanceRepository,
                workerRepository = smartAttendanceApp().container.workerRepository,
                locationRepository = smartAttendanceApp().container.locationRepository
            )
        }
        initializer {
            com.attendancehalim.smartattendance.presentation.admin.AdminReportsViewModel(
                attendanceRepository = smartAttendanceApp().container.attendanceRepository,
                workerRepository = smartAttendanceApp().container.workerRepository,
                sessionRepository = smartAttendanceApp().container.sessionRepository,
                networkMonitor = smartAttendanceApp().container.networkMonitor
            )
        }
    }
}

/**
 * Extension function to retrieve [SmartAttendanceApp] from [CreationExtras].
 */
fun CreationExtras.smartAttendanceApp(): SmartAttendanceApp =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SmartAttendanceApp)
