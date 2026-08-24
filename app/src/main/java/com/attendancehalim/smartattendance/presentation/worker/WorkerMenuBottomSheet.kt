package com.attendancehalim.smartattendance.presentation.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendancehalim.smartattendance.domain.model.UserSession
import com.attendancehalim.smartattendance.ui.theme.BlueAccent
import com.attendancehalim.smartattendance.ui.theme.NavyPrimary
import com.attendancehalim.smartattendance.ui.theme.PunchOutRed
import com.attendancehalim.smartattendance.ui.theme.SurfaceBorder
import com.attendancehalim.smartattendance.ui.theme.SurfaceWhite
import com.attendancehalim.smartattendance.ui.theme.TextPrimary
import com.attendancehalim.smartattendance.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerMenuBottomSheet(
    session: UserSession,
    onDismiss: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToMyAttendance: () -> Unit,
    onNavigateToMarkAttendance: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Profile Header in Bottom Sheet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    color = NavyPrimary.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = session.userName.take(2).uppercase().ifBlank { "EM" },
                            color = NavyPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = session.userName.ifBlank { "Worker" },
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: ${session.employeeId}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = session.workplaceName.ifBlank { "Smart Workplace" },
                        color = BlueAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = SurfaceBorder
            )

            // Menu Items
            MenuItemRow(
                title = "My Profile",
                icon = Icons.Default.Person,
                iconColor = NavyPrimary,
                onClick = {
                    onDismiss()
                    onNavigateToProfile()
                }
            )

            MenuItemRow(
                title = "My Attendance",
                icon = Icons.Default.CalendarMonth,
                iconColor = BlueAccent,
                onClick = {
                    onDismiss()
                    onNavigateToMyAttendance()
                }
            )

            MenuItemRow(
                title = "Mark Attendance",
                icon = Icons.Default.Fingerprint,
                iconColor = BlueAccent,
                onClick = {
                    onDismiss()
                    onNavigateToMarkAttendance()
                }
            )

            MenuItemRow(
                title = "Settings",
                icon = Icons.Default.Settings,
                iconColor = TextSecondary,
                onClick = {
                    onDismiss()
                    onNavigateToSettings()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = SurfaceBorder
            )

            // Logout Item
            MenuItemRow(
                title = "Logout",
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                iconColor = PunchOutRed,
                textColor = PunchOutRed,
                onClick = onLogoutClick
            )
        }
    }
}

@Composable
private fun MenuItemRow(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = TextPrimary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(10.dp),
            color = iconColor.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}
