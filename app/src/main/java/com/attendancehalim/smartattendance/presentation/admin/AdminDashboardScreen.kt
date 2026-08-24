package com.attendancehalim.smartattendance.presentation.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendancehalim.smartattendance.presentation.components.AppCard
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

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.material.icons.filled.Refresh

@Composable
fun AdminDashboardScreen(
    uiState: AdminDashboardUiState,
    onNavigateToEmployees: () -> Unit,
    onNavigateToAddEmployee: () -> Unit,
    onNavigateToAllAttendance: () -> Unit,
    onNavigateToManualAttendance: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRefresh: () -> Unit = {},
    onLogoutClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogoutSuccess()
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isLoggingOut) {
                    showLogoutDialog = false
                }
            },
            title = {
                Text(
                    text = "Confirm Logout",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to logout?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLogoutClick()
                    },
                    enabled = !uiState.isLoggingOut
                ) {
                    Text("LOGOUT", color = PunchOutRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    enabled = !uiState.isLoggingOut
                ) {
                    Text("CANCEL", color = TextSecondary)
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
                .padding(top = 12.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = SurfaceWhite.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = SurfaceWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SMART ATTENDANCE",
                            color = SurfaceWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(38.dp)
                        ) {
                            if (uiState.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = SurfaceWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Attendance",
                                    tint = SurfaceWhite
                                )
                            }
                        }
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = SurfaceWhite
                            )
                        }
                        IconButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout",
                                tint = SurfaceWhite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Admin Info Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = BlueAccent.copy(alpha = 0.3f),
                        border = BorderStroke(2.dp, SurfaceWhite.copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = uiState.session.userName.take(2).uppercase().ifBlank { "AD" },
                                color = SurfaceWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = uiState.session.userName.ifBlank { "Administrator" },
                            color = SurfaceWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Admin ID: ${uiState.session.employeeId.ifBlank { "ADMIN" }} • Administrator",
                            color = SurfaceWhite.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // SCROLLABLE DASHBOARD BODY with navigation bar insets
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // DASHBOARD STATISTICS SECTION
            Text(
                text = "OVERVIEW STATISTICS • ${uiState.todayDateFormatted}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            // Main Total & Present Big Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "TOTAL WORKERS",
                    value = uiState.totalWorkersCount.toString(),
                    icon = Icons.Default.People,
                    color = NavyPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "PRESENT TODAY",
                    value = uiState.presentTodayCount.toString(),
                    icon = Icons.Default.Badge,
                    color = PunchInGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            // Sub Status Grid (Punched In, Punched Out, Not Marked)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniStatCard(
                    title = "PUNCHED IN",
                    value = uiState.punchedInCount.toString(),
                    color = PunchInGreen,
                    modifier = Modifier.weight(1f)
                )
                MiniStatCard(
                    title = "PUNCHED OUT",
                    value = uiState.punchedOutCount.toString(),
                    color = PunchOutRed,
                    modifier = Modifier.weight(1f)
                )
                MiniStatCard(
                    title = "NOT MARKED",
                    value = uiState.notMarkedCount.toString(),
                    color = PendingYellow,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // MANAGEMENT ACTION CARDS
            Text(
                text = "MANAGEMENT ACTIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            AdminActionCard(
                title = "EMPLOYEE MANAGEMENT",
                subtitle = "Manage workers, generate unique IDs & reset passwords",
                icon = Icons.Default.Group,
                color = NavyPrimary,
                onClick = onNavigateToEmployees
            )

            AdminActionCard(
                title = "ALL ATTENDANCE",
                subtitle = "Complete attendance register with date and worker filters",
                icon = Icons.Default.CalendarMonth,
                color = BlueAccent,
                onClick = onNavigateToAllAttendance
            )

            AdminActionCard(
                title = "ADD MANUAL ATTENDANCE",
                subtitle = "Create authorized manual attendance records",
                icon = Icons.Default.EditNote,
                color = SyncedBlue,
                onClick = onNavigateToManualAttendance
            )

            AdminActionCard(
                title = "REPORTS & ANALYTICS",
                subtitle = "Daily summary and monthly attendance logs",
                icon = Icons.Default.Assessment,
                color = NavyDark,
                onClick = onNavigateToReports
            )

            Spacer(modifier = Modifier.height(4.dp))

            // TODAY'S LIVE ATTENDANCE LIST SECTION
            AppCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Attendance",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.todayAttendanceList.count { it.status != WorkerTodayStatus.NOT_MARKED }} / ${uiState.totalWorkersCount}",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.todayAttendanceList.isEmpty()) {
                        Text(
                            text = "No attendance records today.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        uiState.todayAttendanceList.forEachIndexed { index, row ->
                            if (index > 0) {
                                HorizontalDivider(color = SurfaceBorder, thickness = 0.8.dp, modifier = Modifier.padding(vertical = 10.dp))
                            }
                            TodayAttendanceRowItem(row = row)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun MiniStatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
private fun AdminActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TodayAttendanceRowItem(
    row: AdminTodayAttendanceRow,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.employeeName,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${row.employeeId} • ${row.workplaceName}",
                color = TextSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            val timeDisplay = when {
                row.punchInTime != null && row.punchOutTime != null -> "${row.punchInTime} → ${row.punchOutTime}"
                row.punchInTime != null -> "${row.punchInTime} → In Progress"
                else -> "No punch recorded"
            }
            Text(
                text = timeDisplay,
                color = if (row.punchInTime != null) NavyPrimary else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        val (badgeColor, text, textColor) = when (row.status) {
            WorkerTodayStatus.PRESENT -> Triple(PunchInGreen.copy(alpha = 0.12f), "PRESENT", PunchInGreen)
            WorkerTodayStatus.PUNCHED_IN -> Triple(BlueAccent.copy(alpha = 0.12f), "PUNCHED IN", BlueAccent)
            WorkerTodayStatus.PUNCHED_OUT -> Triple(PunchOutRed.copy(alpha = 0.12f), "PUNCHED OUT", PunchOutRed)
            WorkerTodayStatus.NOT_MARKED -> Triple(PendingYellow.copy(alpha = 0.15f), "NOT MARKED", PendingYellow)
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = badgeColor
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
