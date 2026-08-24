package com.attendancehalim.smartattendance.presentation.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendancehalim.smartattendance.domain.model.WorkerProfile
import com.attendancehalim.smartattendance.presentation.components.AppButton
import com.attendancehalim.smartattendance.presentation.components.AppCard
import com.attendancehalim.smartattendance.presentation.components.AppOutlinedButton
import com.attendancehalim.smartattendance.ui.theme.BlueAccent
import com.attendancehalim.smartattendance.ui.theme.NavyDark
import com.attendancehalim.smartattendance.ui.theme.NavyPrimary
import com.attendancehalim.smartattendance.ui.theme.PendingYellow
import com.attendancehalim.smartattendance.ui.theme.PunchInGreen
import com.attendancehalim.smartattendance.ui.theme.PunchOutRed
import com.attendancehalim.smartattendance.ui.theme.SurfaceBorder
import com.attendancehalim.smartattendance.ui.theme.SurfaceCard
import com.attendancehalim.smartattendance.ui.theme.SurfaceWhite
import com.attendancehalim.smartattendance.ui.theme.SyncedBlue
import com.attendancehalim.smartattendance.ui.theme.TextMuted
import com.attendancehalim.smartattendance.ui.theme.TextPrimary
import com.attendancehalim.smartattendance.ui.theme.TextSecondary

@Composable
fun AdminEmployeeDetailsScreen(
    worker: WorkerProfile?,
    totalPresentDays: Int,
    totalPunchRecords: Int,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToAttendance: (String) -> Unit,
    onToggleStatus: (String, Boolean) -> Unit,
    onResetPassword: (String) -> Unit,
    actionSuccessMessage: String?,
    onClearMessage: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeactivateDialog by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }

    if (worker == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("Worker details not found.", color = TextSecondary)
        }
        return
    }

    // Deactivation / Activation Confirmation Dialog
    if (showDeactivateDialog) {
        val isCurrentlyActive = worker.isActive
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = {
                Text(
                    text = if (isCurrentlyActive) "Deactivate Worker?" else "Activate Worker?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = if (isCurrentlyActive)
                        "When deactivated, ${worker.fullName} (${worker.employeeId}) will NOT be able to log in or mark attendance. Existing attendance records will remain preserved."
                    else
                        "Re-activating will allow ${worker.fullName} (${worker.employeeId}) to log in and mark attendance again.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeactivateDialog = false
                        onToggleStatus(worker.employeeId, worker.isActive)
                    }
                ) {
                    Text(
                        text = if (isCurrentlyActive) "Deactivate" else "Activate",
                        color = if (isCurrentlyActive) PunchOutRed else PunchInGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Reset Password Confirmation Dialog
    if (showResetPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showResetPasswordDialog = false },
            title = {
                Text(
                    text = "Reset Worker Password?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to reset password for ${worker.fullName} (${worker.employeeId})? The login password will be set back to default: 12345.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetPasswordDialog = false
                        onResetPassword(worker.employeeId)
                    }
                ) {
                    Text("Reset Password", color = NavyPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetPasswordDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP NAVY HEADER with status bar insets
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyPrimary)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SurfaceWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Employee Details",
                            color = SurfaceWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${worker.employeeId} • ${if (worker.isActive) "Active" else "Inactive"}",
                            color = SurfaceWhite.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = { onNavigateToEdit(worker.employeeId) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Worker",
                        tint = SurfaceWhite
                    )
                }
            }
        }

        // SCROLLABLE CONTENT with navigation bar insets
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Action Success Banner
            if (actionSuccessMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = PunchInGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, PunchInGreen.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PunchInGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = actionSuccessMessage,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        TextButton(onClick = onClearMessage) {
                            Text("OK", color = NavyPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // PROFILE SUMMARY CARD
            AppCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape,
                            color = if (worker.isActive) BlueAccent.copy(alpha = 0.15f) else TextMuted.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = worker.fullName.take(2).uppercase().ifBlank { "WK" },
                                    color = if (worker.isActive) BlueAccent else TextSecondary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = worker.fullName,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Employee ID: ${worker.employeeId}",
                                color = NavyPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (worker.isActive) PunchInGreen.copy(alpha = 0.12f) else PunchOutRed.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (worker.isActive) "ACTIVE ACCOUNT" else "INACTIVE / DISABLED",
                                    color = if (worker.isActive) PunchInGreen else PunchOutRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = SurfaceBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Detail rows
                    DetailRow(icon = Icons.Default.Phone, label = "Mobile Number", value = worker.mobileNumber.ifBlank { "Not provided" })
                    DetailRow(icon = Icons.Default.Business, label = "Workplace / Company", value = worker.workplaceName.ifBlank { "Not provided" })
                    DetailRow(icon = Icons.Default.Work, label = "Designation", value = worker.designation.ifBlank { "Field Staff" })
                    DetailRow(icon = Icons.Default.CalendarMonth, label = "Joining Date", value = worker.joiningDate.ifBlank { "Not provided" })
                }
            }

            // ATTENDANCE STATS SUMMARY CARD
            AppCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Attendance Summary",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = PunchInGreen.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, PunchInGreen.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "Total Present", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "$totalPresentDays Days", color = PunchInGreen, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = BlueAccent.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, BlueAccent.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "Punch Records", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "$totalPunchRecords Logs", color = BlueAccent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // ACTIONS SECTION
            Text(
                text = "WORKER ACTIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            // View Attendance
            AppButton(
                text = "VIEW WORKER ATTENDANCE",
                onClick = { onNavigateToAttendance(worker.employeeId) },
                icon = Icons.AutoMirrored.Filled.EventNote,
                containerColor = NavyPrimary
            )

            // Edit Employee
            AppOutlinedButton(
                text = "Edit Employee Profile",
                onClick = { onNavigateToEdit(worker.employeeId) },
                icon = Icons.Default.Edit,
                borderColor = NavyPrimary,
                contentColor = NavyPrimary
            )

            // Reset Password
            AppOutlinedButton(
                text = "Reset Password (12345)",
                onClick = { showResetPasswordDialog = true },
                icon = Icons.Default.LockReset,
                borderColor = BlueAccent,
                contentColor = BlueAccent
            )

            // Activate / Deactivate
            AppOutlinedButton(
                text = if (worker.isActive) "Deactivate Worker Account" else "Activate Worker Account",
                onClick = { showDeactivateDialog = true },
                icon = Icons.Default.PowerSettingsNew,
                borderColor = if (worker.isActive) PunchOutRed else PunchInGreen,
                contentColor = if (worker.isActive) PunchOutRed else PunchInGreen
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, color = TextSecondary, fontSize = 11.sp)
            Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
