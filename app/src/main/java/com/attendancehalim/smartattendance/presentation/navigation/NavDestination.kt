package com.attendancehalim.smartattendance.presentation.navigation

/**
 * Screen destinations and route identifiers for SMART ATTENDANCE navigation graph.
 */
sealed class NavDestination(val route: String) {
    object Splash : NavDestination("splash")
    object RoleSelection : NavDestination("role_selection")
    object WorkerLogin : NavDestination("worker_login")
    object AdminLogin : NavDestination("admin_login")
    object WorkerDashboard : NavDestination("worker_dashboard")
    object WorkerMarkAttendance : NavDestination("worker_mark_attendance")
    object WorkerMyAttendance : NavDestination("worker_my_attendance")
    object WorkerProfile : NavDestination("worker_profile")
    object WorkerSettings : NavDestination("worker_settings")
    
    // Admin Destinations
    object AdminDashboard : NavDestination("admin_dashboard")
    object AdminEmployees : NavDestination("admin_employees")
    object AdminAddEmployee : NavDestination("admin_add_employee")
    object AdminEmployeeDetails : NavDestination("admin_employee_details/{employeeId}") {
        fun createRoute(employeeId: String) = "admin_employee_details/$employeeId"
    }
    object AdminEditEmployee : NavDestination("admin_edit_employee/{employeeId}") {
        fun createRoute(employeeId: String) = "admin_edit_employee/$employeeId"
    }
    object AdminAllAttendance : NavDestination("admin_all_attendance")
    object AdminWorkerAttendance : NavDestination("admin_worker_attendance/{employeeId}") {
        fun createRoute(employeeId: String) = "admin_worker_attendance/$employeeId"
    }
    object AdminManualAttendance : NavDestination("admin_manual_attendance")
    object AdminReports : NavDestination("admin_reports")
    object AdminReportDetail : NavDestination("admin_report_detail/{reportType}") {
        fun createRoute(reportType: String) = "admin_report_detail/$reportType"
    }
    object AdminSettings : NavDestination("admin_settings")
}
