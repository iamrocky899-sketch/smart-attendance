package com.attendancehalim.smartattendance.presentation.worker

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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.attendancehalim.smartattendance.ui.theme.TextMuted
import com.attendancehalim.smartattendance.ui.theme.TextPrimary
import com.attendancehalim.smartattendance.ui.theme.TextSecondary

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun WorkerDashboardScreen(
    uiState: WorkerDashboardUiState,
    onNavigateToMarkAttendance: () -> Unit,
    onNavigateToMyAttendance: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogoutClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenuSheet by remember { mutableStateOf(false) }
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

    if (showMenuSheet) {
        WorkerMenuBottomSheet(
            session = uiState.session,
            onDismiss = { showMenuSheet = false },
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToMyAttendance = onNavigateToMyAttendance,
            onNavigateToMarkAttendance = onNavigateToMarkAttendance,
            onNavigateToSettings = onNavigateToSettings,
            onLogoutClick = {
                showMenuSheet = false
                showLogoutDialog = true
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Navy Profile Header with status bar insets
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyPrimary)
                .statusBarsPadding()
                .padding(top = 12.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)
        ) {
            Column {
                // Top App Bar row with Menu Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SMART ATTENDANCE",
                            color = SurfaceWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = { showMenuSheet = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = SurfaceWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Employee Profile Summary Card inside Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Photo / Avatar
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = BlueAccent.copy(alpha = 0.3f),
                        border = BorderStroke(2.dp, SurfaceWhite.copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = uiState.session.userName.take(2).uppercase().ifBlank { "WK" },
                                color = SurfaceWhite,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = uiState.session.userName.ifBlank { "Worker" },
                            color = SurfaceWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "ID: ${uiState.session.employeeId.ifBlank { "EMP-0001" }}",
                            color = SurfaceWhite.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = BlueAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = uiState.session.workplaceName.ifBlank { "Facility" },
                                color = SurfaceWhite.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Dashboard Body Content with navigation bar insets
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Today's Status Card
            AppCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Today's Status",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = uiState.todayDateFormatted.ifBlank { "Today" },
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status Highlight Pill
                    val (statusBg, statusFg) = when (uiState.todayStatus) {
                        TodayAttendanceStatus.PUNCHED_IN -> Pair(PunchInGreen.copy(alpha = 0.12f), PunchInGreen)
                        TodayAttendanceStatus.PUNCHED_OUT -> Pair(PunchOutRed.copy(alpha = 0.12f), PunchOutRed)
                        TodayAttendanceStatus.NOT_MARKED -> Pair(PendingYellow.copy(alpha = 0.15f), PendingYellow)
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = statusBg
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(statusFg, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = uiState.todayStatus.displayTitle,
                                    color = statusFg,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (uiState.lastPunchTime != null) "Last recorded at ${uiState.lastPunchTime}" else uiState.todayStatus.description,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // MAIN ACTION: MARK ATTENDANCE
            ActionCard(
                title = "MARK ATTENDANCE",
                subtitle = "GPS Punch In / Punch Out with location capture",
                icon = Icons.Default.Fingerprint,
                badgeText = "Location Punch",
                primaryColor = NavyPrimary,
                iconBackgroundColor = NavyPrimary.copy(alpha = 0.1f),
                onClick = onNavigateToMarkAttendance
            )

            // MAIN ACTION: MY ATTENDANCE
            ActionCard(
                title = "MY ATTENDANCE",
                subtitle = "View your attendance records and monthly logs",
                icon = Icons.Default.CalendarMonth,
                badgeText = "History",
                primaryColor = BlueAccent,
                iconBackgroundColor = BlueAccent.copy(alpha = 0.1f),
                onClick = onNavigateToMyAttendance
            )

            // Pending Sync Notice if any
            if (uiState.pendingSyncCount > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = PendingYellow.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, PendingYellow.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = PendingYellow,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${uiState.pendingSyncCount} Record(s) Pending Sync",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Saved offline locally. Will auto-sync when online.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String,
    primaryColor: Color,
    iconBackgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(14.dp),
                color = iconBackgroundColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
