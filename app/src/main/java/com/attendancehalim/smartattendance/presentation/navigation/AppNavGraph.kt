package com.attendancehalim.smartattendance.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.attendancehalim.smartattendance.domain.model.SyncStatus
import com.attendancehalim.smartattendance.domain.model.UserRole
import com.attendancehalim.smartattendance.presentation.AppViewModelProvider
import com.attendancehalim.smartattendance.presentation.auth.AdminLoginScreen
import com.attendancehalim.smartattendance.presentation.auth.AuthViewModel
import com.attendancehalim.smartattendance.presentation.auth.RoleSelectionScreen
import com.attendancehalim.smartattendance.presentation.auth.WorkerLoginScreen
import com.attendancehalim.smartattendance.presentation.main.MainViewModel
import com.attendancehalim.smartattendance.presentation.main.SessionUiState
import com.attendancehalim.smartattendance.presentation.worker.WorkerDashboardScreen
import com.attendancehalim.smartattendance.presentation.worker.WorkerDashboardViewModel
import com.attendancehalim.smartattendance.presentation.worker.WorkerMarkAttendanceScreen
import com.attendancehalim.smartattendance.presentation.worker.WorkerMarkAttendanceViewModel
import com.attendancehalim.smartattendance.presentation.worker.WorkerMyAttendanceScreen
import com.attendancehalim.smartattendance.presentation.worker.WorkerMyAttendanceViewModel
import com.attendancehalim.smartattendance.presentation.worker.WorkerProfileScreen
import com.attendancehalim.smartattendance.presentation.worker.WorkerSettingsScreen
import com.attendancehalim.smartattendance.ui.theme.NavyPrimary

import com.attendancehalim.smartattendance.presentation.admin.AdminAddEmployeeScreen
import com.attendancehalim.smartattendance.presentation.admin.AdminAllAttendanceScreen
import com.attendancehalim.smartattendance.presentation.admin.AdminAttendanceViewModel
import com.attendancehalim.smartattendance.presentation.admin.AdminDashboardScreen
import com.attendancehalim.smartattendance.presentation.admin.AdminDashboardViewModel
import com.attendancehalim.smartattendance.presentation.admin.AdminEditEmployeeScreen
import com.attendancehalim.smartattendance.presentation.admin.AdminEmployeeDetailsScreen
import com.attendancehalim.smartattendance.presentation.admin.AdminEmployeesScreen
import com.attendancehalim.smartattendance.presentation.admin.AdminEmployeesViewModel
import com.attendancehalim.smartattendance.presentation.admin.AdminManualAttendanceScreen
import com.attendancehalim.smartattendance.presentation.admin.AdminReportsScreen
import com.attendancehalim.smartattendance.presentation.admin.AdminSettingsScreen
import com.attendancehalim.smartattendance.presentation.admin.AdminWorkerAttendanceScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory),
    modifier: Modifier = Modifier
) {
    val sessionState by mainViewModel.sessionState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = NavDestination.Splash.route,
        modifier = modifier
    ) {
        // SPLASH / SESSION RESOLUTION
        composable(NavDestination.Splash.route) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NavyPrimary),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            }

            LaunchedEffect(sessionState) {
                when (val state = sessionState) {
                    is SessionUiState.Loading -> {
                        // Keep showing splash
                    }
                    is SessionUiState.Authenticated -> {
                        if (state.session.role == UserRole.WORKER) {
                            navController.navigate(NavDestination.WorkerDashboard.route) {
                                popUpTo(NavDestination.Splash.route) { inclusive = true }
                            }
                        } else if (state.session.role == UserRole.ADMIN) {
                            navController.navigate(NavDestination.AdminDashboard.route) {
                                popUpTo(NavDestination.Splash.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(NavDestination.RoleSelection.route) {
                                popUpTo(NavDestination.Splash.route) { inclusive = true }
                            }
                        }
                    }
                    is SessionUiState.Unauthenticated -> {
                        navController.navigate(NavDestination.RoleSelection.route) {
                            popUpTo(NavDestination.Splash.route) { inclusive = true }
                        }
                    }
                }
            }
        }

        // ROLE SELECTION
        composable(NavDestination.RoleSelection.route) {
            val authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
            RoleSelectionScreen(
                onSelectRole = { role ->
                    authViewModel.selectRole(role)
                    when (role) {
                        UserRole.WORKER -> navController.navigate(NavDestination.WorkerLogin.route)
                        UserRole.ADMIN -> navController.navigate(NavDestination.AdminLogin.route)
                    }
                }
            )
        }

        // WORKER LOGIN
        composable(NavDestination.WorkerLogin.route) {
            val authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val authState by authViewModel.uiState.collectAsStateWithLifecycle()

            WorkerLoginScreen(
                uiState = authState,
                onEmployeeIdChange = authViewModel::onEmployeeIdChanged,
                onPasswordChange = authViewModel::onPasswordChanged,
                onLoginClick = authViewModel::loginWorker,
                onNavigateBack = { navController.popBackStack() },
                onLoginSuccess = {
                    authViewModel.resetLoginSuccess()
                    navController.navigate(NavDestination.WorkerDashboard.route) {
                        popUpTo(NavDestination.RoleSelection.route) { inclusive = true }
                    }
                }
            )
        }

        // ADMIN LOGIN
        composable(NavDestination.AdminLogin.route) {
            val authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val authState by authViewModel.uiState.collectAsStateWithLifecycle()

            AdminLoginScreen(
                uiState = authState,
                onUsernameChange = authViewModel::onAdminUsernameChanged,
                onPasswordChange = authViewModel::onAdminPasswordChanged,
                onLoginClick = authViewModel::loginAdmin,
                onNavigateBack = { navController.popBackStack() },
                onLoginSuccess = {
                    authViewModel.resetLoginSuccess()
                    navController.navigate(NavDestination.AdminDashboard.route) {
                        popUpTo(NavDestination.RoleSelection.route) { inclusive = true }
                    }
                }
            )
        }

        // WORKER DASHBOARD
        composable(NavDestination.WorkerDashboard.route) {
            val dashboardViewModel: WorkerDashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

            WorkerDashboardScreen(
                uiState = dashboardState,
                onNavigateToMarkAttendance = {
                    navController.navigate(NavDestination.WorkerMarkAttendance.route)
                },
                onNavigateToMyAttendance = {
                    navController.navigate(NavDestination.WorkerMyAttendance.route)
                },
                onNavigateToProfile = {
                    navController.navigate(NavDestination.WorkerProfile.route)
                },
                onNavigateToSettings = {
                    navController.navigate(NavDestination.WorkerSettings.route)
                },
                onLogoutClick = {
                    dashboardViewModel.logout {
                        navController.navigate(NavDestination.RoleSelection.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                },
                onLogoutSuccess = {
                    navController.navigate(NavDestination.RoleSelection.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        // WORKER MARK ATTENDANCE
        composable(NavDestination.WorkerMarkAttendance.route) {
            val markAttendanceViewModel: WorkerMarkAttendanceViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val markState by markAttendanceViewModel.uiState.collectAsStateWithLifecycle()

            WorkerMarkAttendanceScreen(
                uiState = markState,
                onCheckLocationState = markAttendanceViewModel::checkLocationAndPermissionState,
                onPermissionResult = markAttendanceViewModel::onPermissionResult,
                onRefreshLocation = markAttendanceViewModel::refreshLocationPreview,
                onPunchIn = markAttendanceViewModel::punchIn,
                onPunchOut = markAttendanceViewModel::punchOut,
                onDismissConfirmation = markAttendanceViewModel::dismissConfirmation,
                onDismissError = markAttendanceViewModel::dismissError,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // WORKER MY ATTENDANCE
        composable(NavDestination.WorkerMyAttendance.route) {
            val myAttendanceViewModel: WorkerMyAttendanceViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val myAttendanceState by myAttendanceViewModel.uiState.collectAsStateWithLifecycle()

            WorkerMyAttendanceScreen(
                uiState = myAttendanceState,
                onSelectItem = myAttendanceViewModel::selectItem,
                onClearSelectedItem = myAttendanceViewModel::clearSelectedItem,
                onExportPdf = myAttendanceViewModel::exportMyPdf,
                onExportExcel = myAttendanceViewModel::exportMyExcel,
                onOpenFile = myAttendanceViewModel::openExportedFile,
                onShareFile = myAttendanceViewModel::shareExportedFile,
                onDismissExportDialog = myAttendanceViewModel::dismissExportDialog,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // WORKER PROFILE
        composable(NavDestination.WorkerProfile.route) {
            val session = (sessionState as? SessionUiState.Authenticated)?.session
                ?: com.attendancehalim.smartattendance.domain.model.UserSession()
            WorkerProfileScreen(
                session = session,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // WORKER SETTINGS
        composable(NavDestination.WorkerSettings.route) {
            val dashboardViewModel: WorkerDashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

            WorkerSettingsScreen(
                isLoggingOut = dashboardState.isLoggingOut,
                onLogoutClick = {
                    dashboardViewModel.logout {
                        navController.navigate(NavDestination.RoleSelection.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ADMIN DASHBOARD
        composable(NavDestination.AdminDashboard.route) {
            val adminDashboardViewModel: AdminDashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val adminDashboardState by adminDashboardViewModel.uiState.collectAsStateWithLifecycle()

            AdminDashboardScreen(
                uiState = adminDashboardState,
                onNavigateToEmployees = {
                    navController.navigate(NavDestination.AdminEmployees.route)
                },
                onNavigateToAddEmployee = {
                    navController.navigate(NavDestination.AdminAddEmployee.route)
                },
                onNavigateToAllAttendance = {
                    navController.navigate(NavDestination.AdminAllAttendance.route)
                },
                onNavigateToManualAttendance = {
                    navController.navigate(NavDestination.AdminManualAttendance.route)
                },
                onNavigateToReports = {
                    navController.navigate(NavDestination.AdminReports.route)
                },
                onNavigateToSettings = {
                    navController.navigate(NavDestination.AdminSettings.route)
                },
                onRefresh = adminDashboardViewModel::refresh,
                onLogoutClick = {
                    adminDashboardViewModel.logout {
                        navController.navigate(NavDestination.RoleSelection.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                },
                onLogoutSuccess = {
                    navController.navigate(NavDestination.RoleSelection.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        // ADMIN EMPLOYEE LIST
        composable(NavDestination.AdminEmployees.route) {
            val employeesViewModel: AdminEmployeesViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val employeesState by employeesViewModel.uiState.collectAsStateWithLifecycle()

            AdminEmployeesScreen(
                uiState = employeesState,
                onSearchQueryChanged = employeesViewModel::onSearchQueryChanged,
                onFilterStatusChanged = employeesViewModel::onFilterStatusChanged,
                onNavigateToAddEmployee = {
                    navController.navigate(NavDestination.AdminAddEmployee.route)
                },
                onNavigateToEmployeeDetails = { employeeId ->
                    navController.navigate(NavDestination.AdminEmployeeDetails.createRoute(employeeId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ADMIN ADD EMPLOYEE
        composable(NavDestination.AdminAddEmployee.route) {
            val employeesViewModel: AdminEmployeesViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val employeesState by employeesViewModel.uiState.collectAsStateWithLifecycle()

            AdminAddEmployeeScreen(
                uiState = employeesState,
                onLoadNextId = employeesViewModel::loadNextId,
                onAddEmployee = { name, mobile, workplace, designation, joiningDate, isActive ->
                    employeesViewModel.addEmployee(name, mobile, workplace, designation, joiningDate, isActive)
                },
                onClearSuccess = employeesViewModel::clearAddedSuccess,
                onClearError = employeesViewModel::clearMessages,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ADMIN EMPLOYEE DETAILS
        composable(
            route = NavDestination.AdminEmployeeDetails.route,
            arguments = listOf(navArgument("employeeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val employeeId = backStackEntry.arguments?.getString("employeeId") ?: ""
            val employeesViewModel: AdminEmployeesViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val employeesState by employeesViewModel.uiState.collectAsStateWithLifecycle()
            val attendanceViewModel: AdminAttendanceViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val attendanceState by attendanceViewModel.uiState.collectAsStateWithLifecycle()

            val worker = employeesState.workers.firstOrNull { it.employeeId == employeeId }
            val workerRegisters = attendanceState.allRegisters.filter {
                it.punchInRecord?.employeeId == employeeId || it.punchOutRecord?.employeeId == employeeId
            }
            val totalPresent = workerRegisters.count { it.punchInRecord != null }
            val totalPunchLogs = workerRegisters.sumOf {
                (if (it.punchInRecord != null) 1 else 0) + (if (it.punchOutRecord != null) 1 else 0) as Int
            }

            AdminEmployeeDetailsScreen(
                worker = worker,
                totalPresentDays = totalPresent,
                totalPunchRecords = totalPunchLogs,
                onNavigateToEdit = { empId ->
                    navController.navigate(NavDestination.AdminEditEmployee.createRoute(empId))
                },
                onNavigateToAttendance = { empId ->
                    navController.navigate(NavDestination.AdminWorkerAttendance.createRoute(empId))
                },
                onToggleStatus = { empId, currentActive ->
                    employeesViewModel.toggleWorkerStatus(empId, currentActive)
                },
                onResetPassword = { empId ->
                    employeesViewModel.resetPassword(empId)
                },
                actionSuccessMessage = employeesState.actionSuccessMessage,
                onClearMessage = employeesViewModel::clearMessages,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ADMIN EDIT EMPLOYEE
        composable(
            route = NavDestination.AdminEditEmployee.route,
            arguments = listOf(navArgument("employeeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val employeeId = backStackEntry.arguments?.getString("employeeId") ?: ""
            val employeesViewModel: AdminEmployeesViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val employeesState by employeesViewModel.uiState.collectAsStateWithLifecycle()
            val worker = employeesState.workers.firstOrNull { it.employeeId == employeeId }

            AdminEditEmployeeScreen(
                worker = worker,
                uiState = employeesState,
                onSaveUpdate = employeesViewModel::updateEmployee,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ADMIN ALL ATTENDANCE
        composable(NavDestination.AdminAllAttendance.route) {
            val attendanceViewModel: AdminAttendanceViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val attendanceState by attendanceViewModel.uiState.collectAsStateWithLifecycle()

            AdminAllAttendanceScreen(
                uiState = attendanceState,
                onSearchQueryChanged = attendanceViewModel::onSearchQueryChanged,
                onFilterDateChanged = attendanceViewModel::onFilterDateChanged,
                onFilterMonthChanged = attendanceViewModel::onFilterMonthChanged,
                onClearFilters = attendanceViewModel::clearFilters,
                onSelectRegister = attendanceViewModel::selectRegister,
                onClearSelectedRegister = attendanceViewModel::clearSelectedRegister,
                onNavigateToManualAttendance = {
                    navController.navigate(NavDestination.AdminManualAttendance.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ADMIN WORKER ATTENDANCE
        composable(
            route = NavDestination.AdminWorkerAttendance.route,
            arguments = listOf(navArgument("employeeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val employeeId = backStackEntry.arguments?.getString("employeeId") ?: ""
            val employeesViewModel: AdminEmployeesViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val employeesState by employeesViewModel.uiState.collectAsStateWithLifecycle()
            val attendanceViewModel: AdminAttendanceViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val attendanceState by attendanceViewModel.uiState.collectAsStateWithLifecycle()

            val worker = employeesState.workers.firstOrNull { it.employeeId == employeeId }
            val workerRegisters = attendanceState.allRegisters.filter {
                it.punchInRecord?.employeeId == employeeId || it.punchOutRecord?.employeeId == employeeId
            }

            AdminWorkerAttendanceScreen(
                worker = worker,
                workerRegisters = workerRegisters,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ADMIN MANUAL ATTENDANCE
        composable(NavDestination.AdminManualAttendance.route) {
            val attendanceViewModel: AdminAttendanceViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val attendanceState by attendanceViewModel.uiState.collectAsStateWithLifecycle()

            AdminManualAttendanceScreen(
                uiState = attendanceState,
                onRecordManual = attendanceViewModel::recordManualAttendance,
                onClearMessages = attendanceViewModel::clearManualMessages,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ADMIN REPORTS
        composable(NavDestination.AdminReports.route) {
            val employeesViewModel: AdminEmployeesViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val employeesState by employeesViewModel.uiState.collectAsStateWithLifecycle()
            val attendanceViewModel: AdminAttendanceViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val attendanceState by attendanceViewModel.uiState.collectAsStateWithLifecycle()

            AdminReportsScreen(
                totalWorkers = employeesState.workers.size,
                totalAttendanceCount = attendanceState.allRegisters.size,
                pendingSyncCount = attendanceState.allRegisters.count { it.overallSyncStatus == SyncStatus.PENDING },
                onSelectReportType = { type ->
                    navController.navigate(NavDestination.AdminReportDetail.createRoute(type.name))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ADMIN REPORT DETAIL / PREVIEW & EXPORT
        composable(
            route = NavDestination.AdminReportDetail.route,
            arguments = listOf(
                navArgument("reportType") {
                    type = NavType.StringType
                    defaultValue = "DAILY"
                }
            )
        ) { backStackEntry ->
            val reportTypeStr = backStackEntry.arguments?.getString("reportType") ?: "DAILY"
            val reportType = com.attendancehalim.smartattendance.domain.model.ReportType.fromString(reportTypeStr)
            val reportsViewModel: com.attendancehalim.smartattendance.presentation.admin.AdminReportsViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val reportsState by reportsViewModel.uiState.collectAsStateWithLifecycle()

            androidx.compose.runtime.LaunchedEffect(reportType) {
                reportsViewModel.setReportType(reportType)
            }

            com.attendancehalim.smartattendance.presentation.admin.AdminReportDetailScreen(
                uiState = reportsState,
                onDateChange = reportsViewModel::setDateFilter,
                onMonthChange = reportsViewModel::setMonthFilter,
                onWorkerSelect = reportsViewModel::setSelectedWorker,
                onRefresh = reportsViewModel::generateReport,
                onExportPdf = reportsViewModel::exportPdf,
                onExportExcel = reportsViewModel::exportExcel,
                onOpenFile = reportsViewModel::openExportedFile,
                onShareFile = reportsViewModel::shareExportedFile,
                onDismissExportDialog = reportsViewModel::dismissExportDialog,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ADMIN SETTINGS
        composable(NavDestination.AdminSettings.route) {
            val adminDashboardViewModel: AdminDashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val dashboardState by adminDashboardViewModel.uiState.collectAsStateWithLifecycle()

            AdminSettingsScreen(
                session = dashboardState.session,
                totalWorkers = dashboardState.totalWorkersCount,
                totalAttendance = dashboardState.todayAttendanceList.size,
                isLoggedOut = dashboardState.isLoggedOut,
                onLogoutClick = {
                    adminDashboardViewModel.logout {
                        navController.navigate(NavDestination.RoleSelection.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                },
                onLogoutSuccess = {
                    navController.navigate(NavDestination.RoleSelection.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

