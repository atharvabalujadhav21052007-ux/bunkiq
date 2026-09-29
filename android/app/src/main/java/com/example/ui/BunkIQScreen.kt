package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.model.AttendanceMetrics
import com.example.model.BunkIQConstants
import com.example.model.CampusGeofenceConfig
import com.example.model.DayAttendance
import com.example.model.DayBunkCost
import com.example.model.GeofenceLogEvent
import com.example.model.GeofenceStatus
import com.example.model.LectureSlot
import com.example.model.SlotStatus
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CanvasDark
import com.example.ui.theme.Emerald
import com.example.ui.theme.EmeraldMuted
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.Ruby
import com.example.ui.theme.RubyMuted
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextDisabled
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AttendanceViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BunkIQScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dayAttendance by viewModel.dayAttendance.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val whatIfBunks by viewModel.whatIfBunks.collectAsState()
    val campusConfig by viewModel.campusConfig.collectAsState()
    val geofenceStatus by viewModel.geofenceStatus.collectAsState()
    val geofenceLogs by viewModel.geofenceLogs.collectAsState()
    val isFetchingLocation by viewModel.isFetchingLocation.collectAsState()
    val lastEventNotification by viewModel.lastEventNotification.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showAddSlotDialog by remember { mutableStateOf(false) }
    var showGeofenceConfigDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }
    var proxyEditingSlot by remember { mutableStateOf<LectureSlot?>(null) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Feed, 1: Strategist & What-If, 2: Campus Radar

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.refreshLocationFromGps(context)
            Toast.makeText(context, "Location permission granted. Syncing GPS Radar...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Location permission required for live GPS radar.", Toast.LENGTH_LONG).show()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = CanvasDark
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Bar & Branding
            item {
                TopBrandingHeader(
                    onExportClick = { showExportDialog = true },
                    onImportClick = { showImportDialog = true },
                    onResetClick = { showResetConfirmDialog = true },
                    onSignOutClick = { showSignOutConfirmDialog = true }
                )
            }

            // 2. Metrics HUD
            item {
                AttendanceMetricsHud(metrics = metrics)
            }

            // 3. Threshold Warning Alert (if applicable)
            item {
                AnimatedVisibility(visible = metrics.isDanger) {
                    DangerAlertCard(metrics = metrics)
                }
            }

            // 4. Industrial 3-Tab Switcher
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TabSegmentButton(
                        title = "DAILY LOGGER",
                        subtitle = "Class Feed",
                        isSelected = activeTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { activeTab = 0 }
                    )
                    TabSegmentButton(
                        title = "BUNK ENGINE",
                        subtitle = "Strategist",
                        isSelected = activeTab == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { activeTab = 1 }
                    )
                    TabSegmentButton(
                        title = "CAMPUS RADAR",
                        subtitle = "Auto Bunk",
                        isSelected = activeTab == 2,
                        modifier = Modifier.weight(1f),
                        onClick = { activeTab = 2 }
                    )
                }
            }

            when (activeTab) {
                0 -> {
                    // 5. Date Navigation Header
                    item {
                        DateNavigatorBar(
                            selectedDate = selectedDate,
                            onPrevClick = { viewModel.goToPreviousDay() },
                            onNextClick = { viewModel.goToNextDay() },
                            onTodayClick = { viewModel.goToToday() },
                            onAddSlotClick = { showAddSlotDialog = true }
                        )
                    }

                    // 6. Slots Feed or Holiday Banner
                    if (dayAttendance.isHoliday) {
                        item {
                            HolidayInfoCard(
                                reason = dayAttendance.holidayReason ?: "No Lectures Scheduled",
                                date = selectedDate
                            )
                        }
                    } else if (dayAttendance.slots.isEmpty()) {
                        item {
                            EmptyDayCard(onAddSlotClick = { showAddSlotDialog = true })
                        }
                    } else {
                        items(
                            items = dayAttendance.slots,
                            key = { it.id }
                        ) { slot ->
                            LectureSlotRow(
                                slot = slot,
                                onStatusChange = { newStatus ->
                                    viewModel.setSlotStatus(slot.id, newStatus)
                                },
                                onProxyClick = {
                                    proxyEditingSlot = slot
                                },
                                onDeleteClick = {
                                    viewModel.removeSlot(slot.id)
                                }
                            )
                        }
                    }
                }
                1 -> {
                    // Strategist & Simulator Tab
                    item {
                        BunkStrategistView(
                            metrics = metrics,
                            bunkCosts = viewModel.dayBunkCosts,
                            whatIfBunks = whatIfBunks,
                            onWhatIfChange = { viewModel.setWhatIfBunks(it) },
                            simulatedPercentage = viewModel.calculateWhatIfPercentage(whatIfBunks)
                        )
                    }
                }
                2 -> {
                    // Location & Geofence Radar Tab
                    item {
                        CampusRadarView(
                            campusConfig = campusConfig,
                            geofenceStatus = geofenceStatus,
                            geofenceLogs = geofenceLogs,
                            isFetchingLocation = isFetchingLocation,
                            onToggleAutoAttendance = { enabled ->
                                viewModel.updateCampusConfig(campusConfig.copy(autoAttendanceEnabled = enabled))
                            },
                            onRefreshGpsClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            onSimulateStepInside = {
                                viewModel.simulateGeofenceStep(isInside = true)
                            },
                            onSimulateStepOutside = {
                                viewModel.simulateGeofenceStep(isInside = false)
                            },
                            onOpenSettings = {
                                showGeofenceConfigDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showExportDialog) {
        val exportJson = remember { viewModel.exportData() }
        ExportJsonDialog(
            json = exportJson,
            onDismiss = { showExportDialog = false },
            onCopy = {
                clipboardManager.setText(AnnotatedString(exportJson))
                Toast.makeText(context, "Export JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showImportDialog) {
        ImportJsonDialog(
            onDismiss = { showImportDialog = false },
            onImport = { jsonString ->
                val ok = viewModel.importData(jsonString)
                if (ok) {
                    Toast.makeText(context, "Attendance logs restored successfully!", Toast.LENGTH_SHORT).show()
                    showImportDialog = false
                } else {
                    Toast.makeText(context, "Invalid JSON format! Please check data.", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        ResetConfirmDialog(
            onDismiss = { showResetConfirmDialog = false },
            onConfirm = {
                viewModel.resetAllData()
                Toast.makeText(context, "Reset to baseline & Sept 10-12 data.", Toast.LENGTH_SHORT).show()
                showResetConfirmDialog = false
            }
        )
    }

    if (showAddSlotDialog) {
        AddSlotDialog(
            onDismiss = { showAddSlotDialog = false },
            onAdd = { num, time, subject, weight ->
                viewModel.addExtraSlot(num, time, subject, weight)
                showAddSlotDialog = false
            }
        )
    }

    proxyEditingSlot?.let { slot ->
        ProxySlotDialog(
            slot = slot,
            onDismiss = { proxyEditingSlot = null },
            onSave = { newSubject, newWeight, note ->
                viewModel.replaceSlot(slot.id, newSubject, newWeight, note)
                proxyEditingSlot = null
            },
            onReset = {
                viewModel.resetSlotToDefault(slot.id)
                proxyEditingSlot = null
            }
        )
    }

    if (showGeofenceConfigDialog) {
        GeofenceConfigDialog(
            currentConfig = campusConfig,
            onDismiss = { showGeofenceConfigDialog = false },
            onSave = { updated ->
                viewModel.updateCampusConfig(updated)
                showGeofenceConfigDialog = false
                Toast.makeText(context, "Campus geofence updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showSignOutConfirmDialog) {
        SignOutConfirmDialog(
            onDismiss = { showSignOutConfirmDialog = false },
            onConfirm = {
                showSignOutConfirmDialog = false
                com.example.ui.signOutUser(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = {
                        Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                    },
                    scope = coroutineScope
                )
            }
        )
    }
}

// -------------------------------------------------------------
// Component: Top Branding Header
// -------------------------------------------------------------
@Composable
fun TopBrandingHeader(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onResetClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val authUser = Firebase.auth.currentUser
    val userEmail = authUser?.email
    val subtitleText = if (!userEmail.isNullOrBlank()) {
        "${BunkIQConstants.STUDENT_NAME} • $userEmail"
    } else {
        "${BunkIQConstants.STUDENT_NAME} • ${BunkIQConstants.STUDENT_USN}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Emerald)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BUNKIQ",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "v2.0 RADAR",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitleText,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                color = AccentBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onExportClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                    .testTag("export_json_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = "Export JSON",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onImportClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                    .testTag("import_json_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Import JSON",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onResetClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                    .testTag("reset_baseline_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onSignOutClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign Out",
                    tint = Ruby,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Metrics HUD
// -------------------------------------------------------------
@Composable
fun AttendanceMetricsHud(metrics: AttendanceMetrics) {
    val isSafe = metrics.currentPercentage >= BunkIQConstants.TARGET_THRESHOLD
    val pctColor = if (isSafe) Emerald else Ruby

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Main split row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Current Attendance
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "CURRENT ATTENDANCE",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "%.1f%%", metrics.currentPercentage),
                            fontFamily = JetBrainsMono,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = pctColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "target: 75%",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.1f", metrics.totalAttended)} / ${String.format(Locale.US, "%.1f", metrics.totalConducted)} counts",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Card 2: Safe Bunk Balance
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        1.dp,
                        if (metrics.safeBunkBalance > 0) BorderSubtle else Ruby,
                        RoundedCornerShape(8.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (metrics.safeBunkBalance > 0) SurfaceDark else RubyMuted.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SAFE BUNK BALANCE",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (metrics.safeBunkBalance > 0) EmeraldMuted else RubyMuted)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (metrics.safeBunkBalance > 0) "SAFE" else "ALERT",
                                fontFamily = JetBrainsMono,
                                fontSize = 9.sp,
                                color = if (metrics.safeBunkBalance > 0) Emerald else Ruby,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${metrics.safeBunkBalance}",
                        fontFamily = JetBrainsMono,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (metrics.safeBunkBalance > 0) Emerald else Ruby
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "can miss before <75%",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Row 2: Secondary stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PROJECTED FINAL %",
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp,
                    color = TextMuted
                )
                Text(
                    text = String.format(Locale.US, "%.1f%%", metrics.projectedFinalPercentage),
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(BorderSubtle)
            )
            Column {
                Text(
                    text = "REMAINING CLASSES",
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp,
                    color = TextMuted
                )
                Text(
                    text = String.format(Locale.US, "%.0f counts", metrics.futureProjectedConducted),
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(BorderSubtle)
            )
            Column {
                Text(
                    text = "SEMESTER END",
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp,
                    color = TextMuted
                )
                Text(
                    text = "17 Oct 2026",
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentAmber
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Danger Alert Card
// -------------------------------------------------------------
@Composable
fun DangerAlertCard(metrics: AttendanceMetrics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Ruby, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = RubyMuted.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Danger Warning",
                tint = Ruby,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (metrics.currentPercentage < 75.0f) "THRESHOLD DANGER ALERT" else "BUNK LIMIT REACHED",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Ruby
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (metrics.consecutiveClassesToRecover > 0) {
                    Text(
                        text = "Must attend next ${metrics.consecutiveClassesToRecover} consecutive classes without missing to restore 75.0% threshold!",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                } else {
                    Text(
                        text = "Zero safe bunks remain. Any missed lecture will push attendance below the 75% target.",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Tab Segment Button
// -------------------------------------------------------------
@Composable
fun TabSegmentButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) SurfaceElevated else Color.Transparent,
        label = "tab_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AccentBlue else Color.Transparent,
        label = "tab_border"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(5.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontFamily = JetBrainsMono,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
                color = if (isSelected) TextPrimary else TextMuted
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = if (isSelected) AccentBlue else TextMuted
            )
        }
    }
}

// -------------------------------------------------------------
// Component: Date Navigator Bar
// -------------------------------------------------------------
@Composable
fun DateNavigatorBar(
    selectedDate: LocalDate,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onTodayClick: () -> Unit,
    onAddSlotClick: () -> Unit
) {
    val isToday = selectedDate == LocalDate.of(2026, 9, 12)
    val dayName = selectedDate.dayOfWeek.name.take(3)
    val formatted = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onPrevClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("prev_date_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Day",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$dayName, $formatted",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    if (isToday) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(AccentBlue.copy(alpha = 0.2f))
                                .border(0.5.dp, AccentBlue, RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "TODAY",
                                fontFamily = JetBrainsMono,
                                fontSize = 9.sp,
                                color = AccentBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = "Schedule & Attendance Log",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            IconButton(
                onClick = onNextClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("next_date_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Day",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!isToday) {
                OutlinedButton(
                    onClick = onTodayClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
                ) {
                    Text(text = "Today", fontFamily = JetBrainsMono, fontSize = 11.sp)
                }
            }

            Button(
                onClick = onAddSlotClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("add_lecture_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceElevated,
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Lecture",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "Add", fontFamily = JetBrainsMono, fontSize = 11.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Lecture Slot Row (Interactive Daily Logger)
// -------------------------------------------------------------
@Composable
fun LectureSlotRow(
    slot: LectureSlot,
    onStatusChange: (SlotStatus) -> Unit,
    onProxyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val statusBg = when (slot.status) {
        SlotStatus.PRESENT -> Emerald.copy(alpha = 0.08f)
        SlotStatus.ABSENT -> Ruby.copy(alpha = 0.08f)
        SlotStatus.CANCELLED -> CanvasDark
    }
    val borderColor = when (slot.status) {
        SlotStatus.PRESENT -> Emerald.copy(alpha = 0.35f)
        SlotStatus.ABSENT -> Ruby.copy(alpha = 0.35f)
        SlotStatus.CANCELLED -> BorderSubtle
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .testTag("slot_row_${slot.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(statusBg)
                .padding(12.dp)
        ) {
            // Header Line: Slot Period & Weight
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceElevated)
                            .border(0.5.dp, BorderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = slot.slotNumber,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = slot.timeRange,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                // Weight Tag
                val weightLabel = when (slot.weight) {
                    0f -> "0 count (excluded)"
                    1f -> "1 count"
                    2f -> "2 counts"
                    else -> "${slot.weight} counts"
                }
                val weightColor = if (slot.weight > 0f) TextSecondary else TextMuted

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = weightLabel,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        color = weightColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subject Title & Proxy Flag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = slot.subject,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (slot.isProxy) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AccentAmber.copy(alpha = 0.2f))
                                    .border(0.5.dp, AccentAmber, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "PROXY/REPLACED",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 9.sp,
                                    color = AccentAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (slot.isExtra) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AccentBlue.copy(alpha = 0.2f))
                                    .border(0.5.dp, AccentBlue, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "EXTRA",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 9.sp,
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (slot.proxyNote.isNotBlank()) {
                        Text(
                            text = "Note: ${slot.proxyNote}",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onProxyClick,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Replace / Proxy",
                            tint = AccentBlue,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Proxy",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = AccentBlue
                        )
                    }

                    if (slot.isExtra) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Extra Lecture",
                                tint = Ruby,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fast One-Thumb Status Selector: PRESENT | BUNK | CANCEL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                StatusChoiceButton(
                    label = "ATTENDED",
                    sublabel = "Present",
                    isSelected = slot.status == SlotStatus.PRESENT,
                    activeColor = Emerald,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(SlotStatus.PRESENT) }
                )
                StatusChoiceButton(
                    label = "BUNKED",
                    sublabel = "Absent",
                    isSelected = slot.status == SlotStatus.ABSENT,
                    activeColor = Ruby,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(SlotStatus.ABSENT) }
                )
                StatusChoiceButton(
                    label = "CANCELLED",
                    sublabel = "Off / Free",
                    isSelected = slot.status == SlotStatus.CANCELLED,
                    activeColor = TextMuted,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(SlotStatus.CANCELLED) }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Status Choice Button
// -------------------------------------------------------------
@Composable
fun StatusChoiceButton(
    label: String,
    sublabel: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (isSelected) activeColor else Color.Transparent
    val textColor = if (isSelected) CanvasDark else TextMuted
    val subColor = if (isSelected) CanvasDark.copy(alpha = 0.8f) else TextDisabled

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontFamily = JetBrainsMono,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
                color = textColor
            )
            Text(
                text = sublabel,
                fontSize = 9.sp,
                color = subColor
            )
        }
    }
}

// -------------------------------------------------------------
// Component: Holiday & Empty Cards
// -------------------------------------------------------------
@Composable
fun HolidayInfoCard(reason: String, date: LocalDate) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
                    .border(1.dp, BorderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Holiday",
                    tint = AccentAmber,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "COLLEGE HOLIDAY / OFF",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = AccentAmber
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reason,
                fontFamily = JetBrainsMono,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No lectures conducted on this date. Counts not deducted.",
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyDayCard(onAddSlotClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NO SCHEDULED LECTURES",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No standard slots for this day. You can add extra surprise lectures manually.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onAddSlotClick,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Add Surprise Slot", fontFamily = JetBrainsMono, fontSize = 12.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Bunk Strategist & Recommendation Engine View
// -------------------------------------------------------------
@Composable
fun BunkStrategistView(
    metrics: AttendanceMetrics,
    bunkCosts: List<DayBunkCost>,
    whatIfBunks: Int,
    onWhatIfChange: (Int) -> Unit,
    simulatedPercentage: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Section Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "SAFE BUNK RECOMMENDATION ENGINE",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = AccentBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "BunkIQ analyzes daily weights to determine the lowest-impact days to miss classes while guaranteeing your attendance stays above 75.0% till 17 October 2026.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
            }
        }

        // What-If Simulator Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WHAT-IF BUNK SIMULATOR",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(SurfaceElevated)
                            .border(0.5.dp, BorderSubtle, RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+$whatIfBunks bunks",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AccentAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = whatIfBunks.toFloat(),
                    onValueChange = { onWhatIfChange(it.toInt()) },
                    valueRange = 0f..25f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentBlue,
                        activeTrackColor = AccentBlue,
                        inactiveTrackColor = SurfaceElevated
                    ),
                    modifier = Modifier.testTag("what_if_slider")
                )

                Spacer(modifier = Modifier.height(6.dp))

                val isSimulatedSafe = simulatedPercentage >= 75.0f
                val simColor = if (isSimulatedSafe) Emerald else Ruby

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceElevated)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PROJECTED ATTENDANCE",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f%%", simulatedPercentage),
                            fontFamily = JetBrainsMono,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = simColor
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "THRESHOLD STATUS",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        Text(
                            text = if (isSimulatedSafe) "SAFE (>= 75%)" else "BELOW 75% TARGET",
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = simColor
                        )
                    }
                }
            }
        }

        // Best Days to Bunk Ranking Table
        Text(
            text = "WEEKDAY BUNK IMPACT RANKING",
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = TextMuted,
            modifier = Modifier.padding(start = 2.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bunkCosts.forEach { cost ->
                val badgeColor = when (cost.impactLevel) {
                    "LOWEST" -> Emerald
                    "LOW" -> AccentBlue
                    "HIGH" -> AccentAmber
                    "CRITICAL" -> Ruby
                    else -> TextMuted
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceElevated)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${cost.totalCount}",
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = badgeColor
                                )
                                Text(
                                    text = "cnt",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 8.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cost.name.uppercase(),
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(badgeColor.copy(alpha = 0.15f))
                                        .border(0.5.dp, badgeColor, RoundedCornerShape(3.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = cost.impactLevel,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 9.sp,
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = cost.description,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Dialog: Replace / Proxy Slot Dialog
// -------------------------------------------------------------
@Composable
fun ProxySlotDialog(
    slot: LectureSlot,
    onDismiss: () -> Unit,
    onSave: (newSubject: String, newWeight: Float, note: String) -> Unit,
    onReset: () -> Unit
) {
    var subjectText by remember { mutableStateOf(slot.subject) }
    var weightValue by remember { mutableFloatStateOf(slot.weight) }
    var noteText by remember { mutableStateOf(slot.proxyNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Column {
                Text(
                    text = "REPLACE / PROXY ENGINE",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = AccentBlue
                )
                Text(
                    text = "${slot.slotNumber} • Original: ${slot.originalSubject} (${slot.originalWeight} count)",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = subjectText,
                    onValueChange = { subjectText = it },
                    label = { Text("Substituted Subject", fontFamily = JetBrainsMono, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "ATTENDANCE WEIGHT:",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0f to "0 (Free/CEP)", 1f to "1 (Standard/Lab)", 2f to "2 (2-Hour/MDM)").forEach { (w, label) ->
                        val isSel = weightValue == w
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSel) AccentBlue else SurfaceElevated)
                                .border(1.dp, if (isSel) AccentBlue else BorderSubtle, RoundedCornerShape(4.dp))
                                .clickable { weightValue = w }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) CanvasDark else TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Proxy Reason / Teacher (Optional)", fontFamily = JetBrainsMono, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(subjectText, weightValue, noteText) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Save Proxy", color = CanvasDark, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (slot.isProxy) {
                    TextButton(onClick = onReset) {
                        Text("Reset Default", color = Ruby, fontFamily = JetBrainsMono)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextMuted, fontFamily = JetBrainsMono)
                }
            }
        }
    )
}

// -------------------------------------------------------------
// Dialog: Add Extra Slot Dialog
// -------------------------------------------------------------
@Composable
fun AddSlotDialog(
    onDismiss: () -> Unit,
    onAdd: (num: String, time: String, subject: String, weight: Float) -> Unit
) {
    var slotNumber by remember { mutableStateOf("Extra P7") }
    var timeRange by remember { mutableStateOf("04:00 - 05:00") }
    var subject by remember { mutableStateOf("") }
    var weight by remember { mutableFloatStateOf(1f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "ADD EXTRA / SURPRISE LECTURE",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AccentBlue
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject Name", fontFamily = JetBrainsMono, fontSize = 11.sp) },
                    placeholder = { Text("e.g. Extra DSA, Makeup Class") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = slotNumber,
                        onValueChange = { slotNumber = it },
                        label = { Text("Period Slot", fontFamily = JetBrainsMono, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = timeRange,
                        onValueChange = { timeRange = it },
                        label = { Text("Time Range", fontFamily = JetBrainsMono, fontSize = 11.sp) },
                        modifier = Modifier.weight(1.2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Text(
                    text = "ATTENDANCE WEIGHT:",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0f to "0 (Free)", 1f to "1 (1 Hr)", 2f to "2 (2 Hr)").forEach { (w, label) ->
                        val isSel = weight == w
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSel) AccentBlue else SurfaceElevated)
                                .border(1.dp, if (isSel) AccentBlue else BorderSubtle, RoundedCornerShape(4.dp))
                                .clickable { weight = w }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) CanvasDark else TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank()) {
                        onAdd(slotNumber, timeRange, subject, weight)
                    }
                },
                enabled = subject.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Add Slot", color = CanvasDark, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted, fontFamily = JetBrainsMono)
            }
        }
    )
}

// -------------------------------------------------------------
// Dialog: Export JSON
// -------------------------------------------------------------
@Composable
fun ExportJsonDialog(
    json: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "EXPORT ATTENDANCE DATA",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AccentBlue
            )
        },
        text = {
            Column {
                Text(
                    text = "Complete JSON backup of all daily overrides, proxy lectures, and historical attendance logs:",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CanvasDark)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = json,
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp,
                        color = TextSecondary,
                        lineHeight = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCopy,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy JSON", color = CanvasDark, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextMuted, fontFamily = JetBrainsMono)
            }
        }
    )
}

// -------------------------------------------------------------
// Dialog: Import JSON
// -------------------------------------------------------------
@Composable
fun ImportJsonDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var importText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "IMPORT ATTENDANCE DATA",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AccentBlue
            )
        },
        text = {
            Column {
                Text(
                    text = "Paste your previously exported JSON backup here to restore all logs:",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    placeholder = { Text("Paste JSON here...", fontFamily = JetBrainsMono, fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(importText.trim()) },
                enabled = importText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Restore Data", color = CanvasDark, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted, fontFamily = JetBrainsMono)
            }
        }
    )
}

// -------------------------------------------------------------
// Dialog: Reset Confirmation
// -------------------------------------------------------------
@Composable
fun ResetConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "RESET TO BASELINE DATA",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Ruby
            )
        },
        text = {
            Text(
                text = "Are you sure? This will reset all logs to the official baseline report (138.5 / 169.0) and historical absences for Sept 10–12, 2026. Any custom extra lectures will be wiped.",
                fontSize = 12.sp,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Ruby)
            ) {
                Text("Reset Data", color = TextPrimary, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted, fontFamily = JetBrainsMono)
            }
        }
    )
}

// -------------------------------------------------------------
// Component: Geofence Notification Banner
// -------------------------------------------------------------
@Composable
fun GeofenceNotificationBanner(
    event: GeofenceLogEvent,
    onDismiss: () -> Unit
) {
    val isPresentAction = event.actionTaken.contains("PRESENT")
    val bannerColor = if (isPresentAction) Emerald else Ruby
    val icon = if (isPresentAction) Icons.Default.Check else Icons.Default.Close

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, bannerColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(bannerColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = bannerColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isPresentAction) "RADAR: AUTO-PRESENT" else "RADAR: AUTO-BUNK TRIGGERED",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = bannerColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${event.timeFormatted}",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.message,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = TextPrimary
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Campus Radar & Location Tab View
// -------------------------------------------------------------
@Composable
fun CampusRadarView(
    campusConfig: CampusGeofenceConfig,
    geofenceStatus: GeofenceStatus,
    geofenceLogs: List<GeofenceLogEvent>,
    isFetchingLocation: Boolean,
    onToggleAutoAttendance: (Boolean) -> Unit,
    onRefreshGpsClick: () -> Unit,
    onSimulateStepInside: () -> Unit,
    onSimulateStepOutside: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val statusColor = when (geofenceStatus) {
        GeofenceStatus.INSIDE_CAMPUS -> Emerald
        GeofenceStatus.OUTSIDE_CAMPUS -> Ruby
        else -> AccentAmber
    }

    val statusText = when (geofenceStatus) {
        GeofenceStatus.INSIDE_CAMPUS -> "INSIDE CAMPUS • AUTO-PRESENT ARMED"
        GeofenceStatus.OUTSIDE_CAMPUS -> "OUTSIDE CAMPUS • AUTO-BUNK TRIGGERED"
        GeofenceStatus.PERMISSION_DENIED -> "GPS PERMISSION REQUIRED"
        GeofenceStatus.UNKNOWN -> "RADAR STANDBY (TAP GPS OR SIMULATE)"
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Radar Status HUD Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CAMPUS RADAR",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }

                    // Auto Attendance Switch
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (campusConfig.autoAttendanceEnabled) "AUTO ACTIVE" else "AUTO PAUSED",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = if (campusConfig.autoAttendanceEnabled) Emerald else TextMuted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = campusConfig.autoAttendanceEnabled,
                            onCheckedChange = onToggleAutoAttendance,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Emerald,
                                checkedTrackColor = SurfaceElevated,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceElevated
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big Status Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = statusText,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = statusColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Geofence: ${campusConfig.name} (Radius: ${campusConfig.radiusMeters.toInt()}m)",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Center: ${String.format(Locale.US, "%.4f", campusConfig.latitude)}° N, ${String.format(Locale.US, "%.4f", campusConfig.longitude)}° E",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(CanvasDark)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "100% SILENT • ZERO NOTIFICATIONS",
                                fontFamily = JetBrainsMono,
                                fontSize = 9.sp,
                                color = Emerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // GPS Fetch & Settings buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRefreshGpsClick,
                        enabled = !isFetchingLocation,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("refresh_gps_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AccentBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFetchingLocation) "Scanning..." else "Sync GPS Radar",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("configure_geofence_button"),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Config", fontFamily = JetBrainsMono, fontSize = 11.sp)
                    }
                }
            }
        }

        // 2. Interactive Testing & Simulator Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE LOCATION SIMULATOR (DEMO)",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = AccentAmber
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Test automatic attendance right now without physically moving. Entering marks current class Present; leaving triggers Auto-Bunk.",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSimulateStepInside,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_step_inside_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald,
                            contentColor = CanvasDark
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Step In Campus\n(Auto-Present)",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = onSimulateStepOutside,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_step_outside_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Ruby,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Step Out / Cafe\n(Auto-Bunk)",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 3. Radar Audit Trail / Event History
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "RADAR ACTIVITY LOG (${geofenceLogs.size} EVENTS)",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (geofenceLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No location events recorded yet.\nSync GPS or use simulator above.",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        geofenceLogs.take(10).forEach { log ->
                            GeofenceLogItemRow(log = log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeofenceLogItemRow(log: GeofenceLogEvent) {
    val isPresent = log.actionTaken.contains("PRESENT")
    val badgeColor = if (isPresent) Emerald else if (log.actionTaken.contains("BUNK")) Ruby else AccentBlue

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = log.actionTaken,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = badgeColor
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.message,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${log.timeFormatted} • Slot: ${log.slotNumber} • Distance: ${log.distanceMeters.toInt()}m",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Dialog: Configure Campus Geofence
// -------------------------------------------------------------
@Composable
fun GeofenceConfigDialog(
    currentConfig: CampusGeofenceConfig,
    onDismiss: () -> Unit,
    onSave: (CampusGeofenceConfig) -> Unit
) {
    var name by remember { mutableStateOf(currentConfig.name) }
    var latText by remember { mutableStateOf(currentConfig.latitude.toString()) }
    var lngText by remember { mutableStateOf(currentConfig.longitude.toString()) }
    var radius by remember { mutableFloatStateOf(currentConfig.radiusMeters) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "CAMPUS GEOFENCE SETTINGS",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Campus / Gate Name", fontFamily = JetBrainsMono, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Emerald,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("Latitude (e.g. 18.5204)", fontFamily = JetBrainsMono, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Emerald,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it },
                    label = { Text("Longitude (e.g. 73.8567)", fontFamily = JetBrainsMono, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Emerald,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Geofence Radius: ${radius.toInt()} meters",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = AccentBlue
                )
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 50f..500f,
                    colors = SliderDefaults.colors(
                        thumbColor = Emerald,
                        activeTrackColor = Emerald,
                        inactiveTrackColor = SurfaceElevated
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull() ?: currentConfig.latitude
                    val lng = lngText.toDoubleOrNull() ?: currentConfig.longitude
                    onSave(
                        currentConfig.copy(
                            name = name.ifBlank { "Campus Main Gate" },
                            latitude = lat,
                            longitude = lng,
                            radiusMeters = radius
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald)
            ) {
                Text("Save Geofence", color = CanvasDark, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted, fontFamily = JetBrainsMono)
            }
        }
    )
}

// -------------------------------------------------------------
// Dialog: Sign Out Confirmation
// -------------------------------------------------------------
@Composable
fun SignOutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "SIGN OUT FROM BUNKIQ",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Ruby
            )
        },
        text = {
            Text(
                text = "Are you sure you want to sign out? Your attendance data is securely backed up to the cloud.",
                fontSize = 12.sp,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Ruby)
            ) {
                Text("Sign Out", color = TextPrimary, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted, fontFamily = JetBrainsMono)
            }
        }
    )
}

