package com.attendancehalim.smartattendance.presentation.worker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.attendancehalim.smartattendance.core.util.DateTimeUtils
import com.attendancehalim.smartattendance.domain.model.AttendanceRecord
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.LocationDetails
import com.attendancehalim.smartattendance.domain.model.SyncStatus
import com.attendancehalim.smartattendance.presentation.components.AppButton
import com.attendancehalim.smartattendance.presentation.components.AppCard
import com.attendancehalim.smartattendance.presentation.components.SyncStatusBadge
import com.attendancehalim.smartattendance.ui.theme.BlueAccent
import com.attendancehalim.smartattendance.ui.theme.NavyDark
import com.attendancehalim.smartattendance.ui.theme.NavyPrimary
import com.attendancehalim.smartattendance.ui.theme.PendingYellow
import com.attendancehalim.smartattendance.ui.theme.PunchInGreen
import com.attendancehalim.smartattendance.ui.theme.PunchInGreenDark
import com.attendancehalim.smartattendance.ui.theme.PunchOutRed
import com.attendancehalim.smartattendance.ui.theme.PunchOutRedDark
import com.attendancehalim.smartattendance.ui.theme.SurfaceBorder
import com.attendancehalim.smartattendance.ui.theme.SurfaceCard
import com.attendancehalim.smartattendance.ui.theme.SurfaceWhite
import com.attendancehalim.smartattendance.ui.theme.SyncedBlue
import com.attendancehalim.smartattendance.ui.theme.TextMuted
import com.attendancehalim.smartattendance.ui.theme.TextPrimary
import com.attendancehalim.smartattendance.ui.theme.TextSecondary
import java.util.Locale

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun WorkerMarkAttendanceScreen(
    uiState: WorkerMarkAttendanceUiState,
    onCheckLocationState: () -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    onRefreshLocation: () -> Unit,
    onPunchIn: () -> Unit,
    onPunchOut: () -> Unit,
    onDismissConfirmation: () -> Unit,
    onDismissError: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto re-check location/permission on lifecycle resume (e.g. returning from settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onCheckLocationState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission launcher for Fine & Coarse Location
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val granted = fineGranted || coarseGranted
        onPermissionResult(granted)
    }

    // Confirmation Modal Dialog after successful Punch
    if (uiState.showConfirmationDialog && uiState.confirmationRecord != null) {
        AttendanceConfirmationDialog(
            record = uiState.confirmationRecord,
            onDismiss = onDismissConfirmation
        )
    }

    // Error Dialog
    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = PunchOutRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Attendance Notice",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Text(
                    text = uiState.errorMessage,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text("OK", color = NavyPrimary, fontWeight = FontWeight.Bold)
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
        // TOP NAVY HEADER WITH STATUS BAR INSETS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyPrimary)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                            text = "MARK ATTENDANCE",
                            color = SurfaceWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${uiState.session.userName.ifBlank { "Worker" }} • ${uiState.session.employeeId}",
                            color = SurfaceWhite.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onRefreshLocation,
                    enabled = !uiState.isCapturingLocation && !uiState.isFetchingPreview,
                    modifier = Modifier.size(40.dp)
                ) {
                    if (uiState.isFetchingPreview) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = SurfaceWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Location",
                            tint = SurfaceWhite
                        )
                    }
                }
            }
        }

        // SCROLLABLE CONTENT
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. LOCATION STATUS BANNER / CARDS
            when (uiState.locationStatus) {
                LocationStatusState.PERMISSION_REQUIRED -> {
                    AppCard(backgroundColor = SurfaceCard, elevation = 3.dp) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(52.dp),
                                shape = CircleShape,
                                color = PendingYellow.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = PendingYellow,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Location permission required",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Smart Attendance requires location permission to verify punch-in coordinates.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            AppButton(
                                text = "ALLOW LOCATION PERMISSION",
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                containerColor = NavyPrimary
                            )
                        }
                    }
                }

                LocationStatusState.GPS_OFF -> {
                    AppCard(backgroundColor = SurfaceCard, elevation = 3.dp) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(52.dp),
                                shape = CircleShape,
                                color = PunchOutRed.copy(alpha = 0.12f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.GpsOff,
                                        contentDescription = null,
                                        tint = PunchOutRed,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Turn on Location",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PunchOutRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "GPS is turned off on your device. Please turn on device location.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            AppButton(
                                text = "TURN ON LOCATION",
                                onClick = {
                                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                    context.startActivity(intent)
                                },
                                containerColor = PunchOutRed
                            )
                        }
                    }
                }

                LocationStatusState.CHECKING -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = BlueAccent.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, BlueAccent.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = BlueAccent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Checking location...",
                                color = NavyPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                LocationStatusState.UNAVAILABLE -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = PendingYellow.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, PendingYellow.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = PendingYellow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Location unavailable",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            TextButton(onClick = onRefreshLocation) {
                                Text("Retry", color = NavyPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                LocationStatusState.READY -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = PunchInGreen.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, PunchInGreen.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(PunchInGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Location ready",
                                color = PunchInGreenDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 2. CURRENT LOCATION INFORMATION CARD
            AppCard(elevation = 2.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = BlueAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Current Location",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = uiState.todayDateFormatted,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val loc = uiState.previewLocation
                    if (loc != null) {
                        // Local Area Address Name
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Local Area / Address",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = loc.addressName.ifBlank { "Location Ready" },
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // GPS Coordinates Grid (Latitude, Longitude, Accuracy)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CoordinateBox(
                                label = "Latitude",
                                value = String.format(Locale.US, "%.6f", loc.latitude),
                                modifier = Modifier.weight(1f)
                            )
                            CoordinateBox(
                                label = "Longitude",
                                value = String.format(Locale.US, "%.6f", loc.longitude),
                                modifier = Modifier.weight(1f)
                            )
                            CoordinateBox(
                                label = "Accuracy",
                                value = "${loc.accuracy.toInt()} m",
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                    } else {
                        // Placeholder
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (uiState.isFetchingPreview) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = BlueAccent,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Acquiring GPS coordinates...",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.GpsFixed,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (uiState.isLocationGpsEnabled && uiState.isPermissionGranted) "Checking GPS coordinates..." else "GPS location unavailable",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. TODAY'S ATTENDANCE STATUS CARD
            AppCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Today's Status",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.todayStatus.displayTitle,
                            color = when (uiState.todayStatus) {
                                TodayAttendanceStatus.PUNCHED_IN -> PunchInGreen
                                TodayAttendanceStatus.PUNCHED_OUT -> PunchOutRed
                                TodayAttendanceStatus.NOT_MARKED -> PendingYellow
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (uiState.todayPunchInRecord != null && uiState.todayStatus == TodayAttendanceStatus.PUNCHED_IN) {
                            Text(
                                text = "Since ${DateTimeUtils.formatTimeDisplay(uiState.todayPunchInRecord.time)}",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (uiState.todayStatus == TodayAttendanceStatus.PUNCHED_OUT) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Shift completed for today. Punched In at ${DateTimeUtils.formatTimeDisplay(uiState.todayPunchInRecord?.time ?: "")} • Punched Out at ${DateTimeUtils.formatTimeDisplay(uiState.todayPunchOutRecord?.time ?: "")}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 4. LOADING STATE DURING LOCATION CAPTURE & PUNCH
            if (uiState.isCapturingLocation) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = BlueAccent.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, BlueAccent.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = BlueAccent,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Recording Attendance...",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Saving to device database",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. LARGE PUNCH BUTTONS
            val punchInEnabled = uiState.isLocationReady &&
                    uiState.todayStatus == TodayAttendanceStatus.NOT_MARKED &&
                    !uiState.isCapturingLocation

            val punchOutEnabled = uiState.isLocationReady &&
                    uiState.todayStatus == TodayAttendanceStatus.PUNCHED_IN &&
                    !uiState.isCapturingLocation

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // PUNCH IN BUTTON
                Button(
                    onClick = onPunchIn,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    enabled = punchInEnabled,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PunchInGreen,
                        contentColor = SurfaceWhite,
                        disabledContainerColor = PunchInGreen.copy(alpha = 0.35f),
                        disabledContentColor = SurfaceWhite.copy(alpha = 0.6f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PUNCH IN",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // PUNCH OUT BUTTON
                Button(
                    onClick = onPunchOut,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    enabled = punchOutEnabled,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PunchOutRed,
                        contentColor = SurfaceWhite,
                        disabledContainerColor = PunchOutRed.copy(alpha = 0.35f),
                        disabledContentColor = SurfaceWhite.copy(alpha = 0.6f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PUNCH OUT",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CoordinateBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AttendanceConfirmationDialog(
    record: AttendanceRecord,
    onDismiss: () -> Unit
) {
    val isPunchIn = record.type == AttendanceType.PUNCH_IN
    val primaryColor = if (isPunchIn) PunchInGreen else PunchOutRed
    val actionTitle = if (isPunchIn) "Punched In Successfully" else "Punched Out Successfully"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Success Icon
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    color = primaryColor.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = actionTitle,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${DateTimeUtils.formatTimeDisplay(record.time)} • ${DateTimeUtils.formatFullDate(record.date)}",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Details Card inside Dialog
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailItemRow(label = "Location", value = record.localArea.ifBlank { "Location name unavailable" })
                        DetailItemRow(label = "Latitude", value = String.format(Locale.US, "%.6f", record.latitude))
                        DetailItemRow(label = "Longitude", value = String.format(Locale.US, "%.6f", record.longitude))
                        DetailItemRow(label = "Accuracy", value = "${record.accuracy.toInt()} m")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sync status indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SyncStatusBadge(status = record.syncStatus)
                }

                if (record.syncStatus == SyncStatus.PENDING) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Saved on device — waiting for sync",
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Done",
                onClick = onDismiss,
                containerColor = NavyPrimary,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun DetailItemRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}
