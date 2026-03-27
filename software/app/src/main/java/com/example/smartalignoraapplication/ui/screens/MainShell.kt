package com.example.smartalignoraapplication.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smartalignoraapplication.controller.BleViewModel
import com.example.smartalignoraapplication.ui.components.PostureAnimationScreen
import com.example.smartalignoraapplication.ui.components.SettingsBottomSheet
import com.example.smartalignoraapplication.ui.theme.AppColors
import com.example.smartalignoraapplication.ui.theme.AppDimens

// ─────────────────────────────────────────────────────────────────────────────
// 4 Tabs: Home / Device / Posture / Analysis
// ─────────────────────────────────────────────────────────────────────────────
private data class BottomTab(
    val label:        String,
    val icon:         ImageVector,
    val selectedIcon: ImageVector = icon
)

private val tabs = listOf(
    BottomTab("Home",     Icons.Default.Home,         Icons.Filled.Home),
    BottomTab("Device",   Icons.Default.DevicesOther, Icons.Filled.DevicesOther),
    BottomTab("Posture",  Icons.Default.Person,        Icons.Filled.Person),
    BottomTab("Analysis", Icons.Default.BarChart,      Icons.Filled.BarChart)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    viewModel:             BleViewModel,
    onRequestBtPermission: () -> Unit,
    onEnableBluetooth:     () -> Unit,
    onRequestLocationPerm: () -> Unit
) {
    var selectedTab  by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

    val settings            = viewModel.settingsState
    val fallDetected        by viewModel.fallDetected
    val showLocationDialog  by viewModel.showLocationPermDialog
    val showEmailDialog     by viewModel.showEmailInputDialog
    val continuousFallCount by viewModel.continuousFallCount
    val emailSentStatus     by viewModel.emailSentStatus
    val battery             by viewModel.batteryLevel
    val isConnected         by viewModel.isConnected

    Scaffold(
        containerColor = AppColors.SoftBg,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(AppColors.BlueNavy, Color(0xFF1A5276))
                        )
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // ── Left: connection dot + app name ───────────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isConnected) AppColors.TealAccent
                                    else Color(0xFFEF4444)
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "SmartAlignOra",
                            color      = Color.White,
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // ── Right: battery chip + settings gear ───────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isConnected) {
                            BatteryChip(level = battery)
                            Spacer(Modifier.width(8.dp))
                        }
                        IconButton(
                            onClick  = { showSettings = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                Icons.Default.Settings, "Settings",
                                tint     = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = AppColors.CardWhite,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        label    = {
                            Text(
                                tab.label, fontSize = 11.sp,
                                fontWeight = if (selectedTab == index)
                                    FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                if (selectedTab == index) tab.selectedIcon else tab.icon,
                                tab.label
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = AppColors.PurplePrimary,
                            selectedTextColor   = AppColors.PurplePrimary,
                            indicatorColor      = AppColors.PurpleSurface,
                            unselectedIconColor = AppColors.TextTertiary,
                            unselectedTextColor = AppColors.TextTertiary
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(modifier = Modifier.padding(pad)) {
            when (selectedTab) {
                0 -> HomeTab(viewModel = viewModel)
                1 -> DeviceTab(
                    viewModel             = viewModel,
                    onRequestBtPermission = onRequestBtPermission,
                    onEnableBluetooth     = onEnableBluetooth
                )
                2 -> PostureTab(viewModel = viewModel)
                3 -> AnalysisScreen(viewModel = viewModel)
            }
        }
    }

    // ── Settings Bottom Sheet ─────────────────────────────────────────────────
    if (showSettings) {
        SettingsBottomSheet(
            state                      = settings,
            onDismiss                  = { showSettings = false },
            onVibrationToggled         = { viewModel.onVibrationToggled(it) },
            onVibrationLevelChanged    = { viewModel.onVibrationLevelChanged(it) },
            onVibrationLevelFinished   = { viewModel.onVibrationLevelFinished() },
            onVibrationSequenceChanged = { viewModel.onVibrationSequenceChanged(it) },
            onFallAlertToggled         = { viewModel.onFallAlertToggled(it) },
            onFallAlertEmailChanged    = { viewModel.onFallAlertEmailChanged(it) },
            showEmailDialog            = showEmailDialog,
            onEmailConfirmed           = { viewModel.onFallAlertEmailConfirmed(it) }
        )
    }

    // ── Location permission dialog ────────────────────────────────────────────
    if (showLocationDialog) {
        LocationPermissionDialog(
            onAllow = { onRequestLocationPerm() },
            onDeny  = { viewModel.onLocationPermissionResult(false) }
        )
    }

    // ── Fall alert popup ──────────────────────────────────────────────────────
    if (fallDetected && viewModel.settingsState.fallAlertEnabled) {
        FallAlertDialog(
            location         = viewModel.lastKnownLocation.value,
            fallCount        = continuousFallCount,
            fallAlertEnabled = true,
            emailSentStatus  = emailSentStatus,
            onDismiss        = { viewModel.dismissFallAlert() }
        )
    }
}

// =============================================================================
// BATTERY CHIP — compact pill shown in TopBar when device is connected
// =============================================================================
@Composable
private fun BatteryChip(level: Int) {
    val (icon, tint) = when {
        level < 0   -> Icons.Default.BatteryUnknown to Color.White.copy(alpha = 0.5f)
        level <= 10 -> Icons.Default.Battery0Bar    to Color(0xFFEF4444)
        level <= 30 -> Icons.Default.Battery2Bar    to Color(0xFFF59E0B)
        level <= 60 -> Icons.Default.Battery4Bar    to Color(0xFFE5E7EB)
        level <= 85 -> Icons.Default.Battery5Bar    to Color(0xFF86EFAC)
        else        -> Icons.Default.BatteryFull    to Color(0xFF4ADE80)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = "Battery",
            tint               = tint,
            modifier           = Modifier.size(16.dp)
        )
        if (level >= 0) {
            Spacer(Modifier.width(3.dp))
            Text(
                text       = "$level%",
                color      = tint,
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// =============================================================================
// BATTERY CARD — full card shown in DeviceTab when connected
// =============================================================================
@Composable
private fun BatteryCard(level: Int) {
    val barColor = when {
        level < 0   -> AppColors.BorderColor
        level <= 10 -> Color(0xFFEF4444)
        level <= 30 -> Color(0xFFF59E0B)
        else        -> AppColors.TealAccent
    }
    val statusLabel = when {
        level < 0   -> "Not available"
        level <= 10 -> "Critical — charge now"
        level <= 30 -> "Low — charge soon"
        level <= 60 -> "Moderate"
        level <= 85 -> "Good"
        else        -> "Full"
    }
    val battIcon = when {
        level < 0   -> Icons.Default.BatteryUnknown
        level <= 10 -> Icons.Default.Battery0Bar
        level <= 30 -> Icons.Default.Battery2Bar
        level <= 60 -> Icons.Default.Battery4Bar
        level <= 85 -> Icons.Default.Battery5Bar
        else        -> Icons.Default.BatteryFull
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(AppDimens.radiusXl),
        colors    = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── Header row ────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = battIcon,
                    contentDescription = "Battery",
                    tint               = barColor,
                    modifier           = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Battery",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = AppColors.TextPrimary,
                    modifier   = Modifier.weight(1f)
                )
                Text(
                    if (level >= 0) "$level%" else "–",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = barColor
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Status subtitle ───────────────────────────────────────────────
            Text(
                statusLabel,
                fontSize = 12.sp,
                color    = AppColors.TextSecondary
            )

            // ── Animated fill bar (only when level is known) ──────────────────
            if (level >= 0) {
                Spacer(Modifier.height(14.dp))
                val animW by animateFloatAsState(
                    targetValue   = level / 100f,
                    animationSpec = tween(700),
                    label         = "battery_bar"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(AppColors.BorderColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animW)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(barColor)
                    )
                }
                // ── Tick marks at 25 / 50 / 75 ───────────────────────────────
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("0%", "25%", "50%", "75%", "100%").forEach { tick ->
                        Text(tick, fontSize = 9.sp, color = AppColors.TextTertiary)
                    }
                }
            }
        }
    }
}

// =============================================================================
// HOME TAB
// =============================================================================
@Composable
fun HomeTab(viewModel: BleViewModel) {
    val postureState by viewModel.postureState
    val isConnected  by viewModel.isConnected
    val goodCount     = viewModel.sessionGoodCount.value
    val badCount      = viewModel.sessionBadCount.value
    val total         = goodCount + badCount
    val goodPct       = if (total > 0) goodCount * 100f / total else 0f
    val fallLabel    by viewModel.fallLabelForUI
    val fallProb     by viewModel.fallProbForUI

    val postureColor = when {
        postureState.contains("GOOD") -> AppColors.GoodGreen
        postureState.contains("BAD")  -> AppColors.BadRed
        else                          -> AppColors.WaitingGray
    }

    LazyColumn(
        modifier            = Modifier
            .fillMaxSize()
            .background(AppColors.SoftBg),
        contentPadding      = PaddingValues(AppDimens.paddingLg),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome card
        item {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(AppDimens.radiusXl),
                colors    = CardDefaults.cardColors(containerColor = AppColors.PurplePrimary),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(AppColors.PurplePrimary, AppColors.PurpleLight)
                            ),
                            shape = RoundedCornerShape(AppDimens.radiusXl)
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text("Welcome back 👋",
                            color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("SmartAlignOra", color = Color.White,
                            fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp).clip(CircleShape)
                                    .background(
                                        if (isConnected) AppColors.TealAccent
                                        else Color(0xFFEF4444)
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isConnected) "Device connected" else "Device offline",
                                color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Session stats
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeStatCard(Modifier.weight(1f), "Good posture",
                    "${"%.0f".format(goodPct)}%", AppColors.GoodGreen, Icons.Default.CheckCircle)
                HomeStatCard(Modifier.weight(1f), "Bad posture",
                    "${"%.0f".format(100f - goodPct)}%", AppColors.BadRed, Icons.Default.Warning)
            }
        }

        // Current status
        item {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(AppDimens.radiusXl),
                colors    = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Current Status", fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Posture
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Person, null, tint = postureColor,
                                modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                when {
                                    postureState.contains("GOOD") -> "GOOD"
                                    postureState.contains("BAD")  -> "BAD"
                                    else -> "WAITING"
                                },
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = postureColor
                            )
                            Text("Posture", fontSize = 11.sp, color = AppColors.TextSecondary)
                        }

                        Divider(modifier = Modifier.height(50.dp).width(1.dp),
                            color = AppColors.BorderColor)

                        // Pitch
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Straighten, null,
                                tint = AppColors.PurplePrimary, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("${"%.1f".format(viewModel.currentPitch.value)}°",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = AppColors.PurplePrimary)
                            Text("Pitch", fontSize = 11.sp, color = AppColors.TextSecondary)
                        }

                        Divider(modifier = Modifier.height(50.dp).width(1.dp),
                            color = AppColors.BorderColor)

                        // Fall
                        val fallColor = if (fallLabel == "fall") AppColors.BadRed
                        else AppColors.GoodGreen
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (fallLabel == "fall") Icons.Default.PersonOff
                                else Icons.Default.Person,
                                null, tint = fallColor, modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (fallLabel == "fall") "FALL" else "NORMAL",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fallColor
                            )
                            Text("${"%.0f".format(fallProb * 100f)}%",
                                fontSize = 11.sp, color = AppColors.TextSecondary)
                        }
                    }
                }
            }
        }

        // Recent sessions
        if (viewModel.postureSessions.isNotEmpty()) {
            item {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(AppDimens.radiusXl),
                    colors    = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Recent Sessions", fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                        Spacer(Modifier.height(12.dp))
                        viewModel.postureSessions.take(3).forEachIndexed { idx, session ->
                            val sTotal = session.goodCount + session.badCount
                            val sPct   = if (sTotal > 0)
                                session.goodCount * 100f / sTotal else 0f
                            Row(
                                modifier          = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(session.date, fontSize = 13.sp,
                                    color = AppColors.TextSecondary, modifier = Modifier.weight(1f))
                                Text("${"%.0f".format(sPct)}% good", fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (sPct >= 60f) AppColors.GoodGreen else AppColors.BadRed)
                            }
                            if (idx < minOf(viewModel.postureSessions.size, 3) - 1)
                                Divider(color = AppColors.BorderColor, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStatCard(
    modifier: Modifier, label: String, value: String,
    color: Color, icon: ImageVector
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(AppDimens.radiusLg),
        colors    = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 12.sp, color = AppColors.TextSecondary)
        }
    }
}

// =============================================================================
// DEVICE TAB
// =============================================================================
@Composable
fun DeviceTab(
    viewModel:             BleViewModel,
    onRequestBtPermission: () -> Unit,
    onEnableBluetooth:     () -> Unit
) {
    val isConnected   by viewModel.isConnected
    val status        by viewModel.status
    val isCalibrating by viewModel.isCalibrating
    val battery       by viewModel.batteryLevel

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(AppColors.SoftBg)
            .padding(AppDimens.paddingLg),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Connection card ───────────────────────────────────────────────────
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(AppDimens.radiusXl),
            colors    = CardDefaults.cardColors(
                containerColor = if (isConnected) AppColors.GoodBg else AppColors.WaitingBg
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                        .background(
                            if (isConnected) AppColors.GoodGreen.copy(alpha = 0.15f)
                            else AppColors.WaitingGray.copy(alpha = 0.1f)
                        )
                ) {
                    Icon(
                        if (isConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                        null,
                        tint     = if (isConnected) AppColors.GoodGreen else AppColors.WaitingGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isConnected) "Device Connected" else "Device Offline",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = if (isConnected) AppColors.GoodGreen else AppColors.TextPrimary
                    )
                    Text(status, fontSize = 13.sp, color = AppColors.TextSecondary)
                }
            }
        }

        // ── BT connect / disconnect button ────────────────────────────────────
        Button(
            onClick = {
                if (isConnected) viewModel.disconnect() else onRequestBtPermission()
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(AppDimens.radiusXl),
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) AppColors.BadRed else AppColors.PurplePrimary
            )
        ) {
            Icon(
                if (isConnected) Icons.Default.BluetoothDisabled else Icons.Default.Bluetooth,
                null, modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (isConnected) "Disconnect Device" else "Connect Device",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold
            )
        }

        // ── Battery card — always shown, updates when connected ───────────────
        BatteryCard(level = battery)

        // ── Calibration card ──────────────────────────────────────────────────
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(AppDimens.radiusXl),
            colors    = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, null, tint = AppColors.PurplePrimary,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Calibration", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Stand straight and hold still while the device calibrates your baseline posture.",
                    fontSize = 13.sp, color = AppColors.TextSecondary, lineHeight = 20.sp
                )
                Spacer(Modifier.height(16.dp))

                if (isCalibrating) {
                    CalibrationAnimation()
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedButton(
                    onClick  = { if (isConnected) viewModel.calibrate() },
                    enabled  = isConnected && !isCalibrating,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(AppDimens.radiusLg),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.PurplePrimary
                    )
                ) {
                    if (isCalibrating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp),
                            color = AppColors.PurplePrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Calibrating… stand straight")
                    } else {
                        Icon(Icons.Default.Tune, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Start Calibration", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // ── Live session stats (connected only) ───────────────────────────────
        if (isConnected) {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(AppDimens.radiusXl),
                colors    = CardDefaults.cardColors(containerColor = AppColors.PurpleSurface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    DeviceStatItem("Good",
                        viewModel.sessionGoodCount.value.toString(), AppColors.GoodGreen)
                    Divider(modifier = Modifier.height(40.dp).width(1.dp),
                        color = AppColors.BorderColor)
                    DeviceStatItem("Bad",
                        viewModel.sessionBadCount.value.toString(), AppColors.BadRed)
                    Divider(modifier = Modifier.height(40.dp).width(1.dp),
                        color = AppColors.BorderColor)
                    DeviceStatItem("Pitch",
                        "${"%.1f".format(viewModel.currentPitch.value)}°", AppColors.PurplePrimary)
                }
            }
        }
    }
}

@Composable
private fun DeviceStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = AppColors.TextSecondary)
    }
}

@Composable
private fun CalibrationAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "cal")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600),
            repeatMode = RepeatMode.Reverse
        ), label = "cal_alpha"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusMd))
            .background(AppColors.PurpleSurface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AccessibilityNew, null,
            tint = AppColors.PurplePrimary.copy(alpha = alpha), modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Stand straight for calibration", fontSize = 14.sp,
                fontWeight = FontWeight.Medium, color = AppColors.PurplePrimary)
            Text("Do not move", fontSize = 12.sp, color = AppColors.TextSecondary)
        }
    }
}

// =============================================================================
// POSTURE TAB
// =============================================================================
@Composable
fun PostureTab(viewModel: BleViewModel) {
    val postureState by viewModel.postureState
    val alertState   by viewModel.alertState
    val currentPitch by viewModel.currentPitch
    val fallLabel    by viewModel.fallLabelForUI
    val fallProb     by viewModel.fallProbForUI

    val statusColor = when {
        postureState.contains("GOOD") -> AppColors.GoodGreen
        postureState.contains("BAD")  -> AppColors.BadRed
        else                          -> AppColors.WaitingGray
    }
    val statusBg = when {
        postureState.contains("GOOD") -> AppColors.GoodBg
        postureState.contains("BAD")  -> AppColors.BadBg
        else                          -> AppColors.WaitingBg
    }
    val statusLabel = when {
        postureState.contains("GOOD") -> "GOOD"
        postureState.contains("BAD")  -> "BAD"
        else                          -> "WAITING"
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.SoftBg)) {
        // Posture status banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(statusBg)
                .padding(horizontal = AppDimens.paddingLg, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(8.dp))
                Text("Posture Status", fontSize = 12.sp, color = AppColors.TextSecondary)
                Spacer(Modifier.width(8.dp))
                Text(statusLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = statusColor)
            }
        }

        // Alert banner
        if (alertState.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF3E0))
                    .padding(horizontal = AppDimens.paddingLg, vertical = 8.dp)
            ) {
                Text(alertState, fontSize = 13.sp, color = AppColors.WarnOrange)
            }
        }

        PostureAnimationScreen(
            currentPitch    = currentPitch,
            fallLabel       = fallLabel,
            fallProbability = fallProb
        )
    }
}

// =============================================================================
// LOCATION PERMISSION DIALOG
// =============================================================================
@Composable
fun LocationPermissionDialog(onAllow: () -> Unit, onDeny: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDeny,
        icon = {
            Icon(Icons.Default.LocationOn, null, tint = AppColors.PurplePrimary,
                modifier = Modifier.size(32.dp))
        },
        title = {
            Text("Location Permission Required",
                fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
        },
        text = {
            Column {
                Text(
                    "The Fall Alert feature needs your location to include GPS coordinates in the emergency email.",
                    fontSize = 14.sp, color = AppColors.TextSecondary, lineHeight = 22.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppDimens.radiusMd))
                        .background(AppColors.PurpleSurface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = AppColors.PurplePrimary,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Location is only used during fall events, never stored.",
                        fontSize = 12.sp, color = AppColors.PurplePrimary)
                }
            }
        },
        confirmButton = {
            Button(onClick = onAllow,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.PurplePrimary)) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Allow Location")
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text("Not now", color = AppColors.TextSecondary)
            }
        },
        containerColor = AppColors.CardWhite,
        shape          = RoundedCornerShape(AppDimens.radiusXl)
    )
}

// =============================================================================
// FALL ALERT DIALOG
// =============================================================================
@Composable
fun FallAlertDialog(
    location:         android.location.Location?,
    fallCount:        Int,
    fallAlertEnabled: Boolean,
    emailSentStatus:  String,
    onDismiss:        () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fall_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.5f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Warning, null,
                tint     = AppColors.BadRed.copy(alpha = pulseAlpha),
                modifier = Modifier.size(40.dp))
        },
        title = {
            Text("🚨 Fall Detected!",
                fontWeight = FontWeight.ExtraBold, color = AppColors.BadRed,
                fontSize = 20.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = AppColors.BadBg) {
                    Text(
                        "Detection #${maxOf(fallCount, 1)}",
                        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = AppColors.BadRed
                    )
                }

                Text(
                    "10 consecutive falls confirmed.\nAn emergency email with GPS location has been sent automatically.",
                    fontSize   = 14.sp,
                    color      = AppColors.TextSecondary,
                    lineHeight = 22.sp,
                    textAlign  = TextAlign.Center
                )

                if (location != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDimens.radiusMd))
                            .background(AppColors.GoodBg)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, null, tint = AppColors.GoodGreen,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("GPS Ready ✓", fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, color = AppColors.GoodGreen)
                            Text("${"%.4f".format(location.latitude)}, ${"%.4f".format(location.longitude)}",
                                fontSize = 11.sp, color = AppColors.GoodGreen)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDimens.radiusMd))
                            .background(Color(0xFFFFF3E0))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOff, null, tint = AppColors.WarnOrange,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("GPS locating… email will include location when ready",
                            fontSize = 11.sp, color = AppColors.WarnOrange, lineHeight = 16.sp)
                    }
                }

                if (emailSentStatus.isNotEmpty()) {
                    val statusColor = when {
                        emailSentStatus.startsWith("✅") -> AppColors.GoodGreen
                        emailSentStatus.startsWith("❌") -> AppColors.BadRed
                        else                             -> AppColors.PurplePrimary
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDimens.radiusMd))
                            .background(statusColor.copy(alpha = 0.1f))
                            .padding(10.dp)
                    ) {
                        Text(emailSentStatus, fontSize = 12.sp,
                            color = statusColor, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier            = Modifier.fillMaxWidth()
            ) {
                if (fallAlertEnabled) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(AppDimens.radiusLg),
                        color    = AppColors.BadBg
                    ) {
                        Row(
                            modifier          = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Email, null,
                                tint = AppColors.BadRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Emergency email will be sent automatically",
                                fontSize = 12.sp, color = AppColors.BadRed)
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(AppDimens.radiusLg),
                        color    = AppColors.PurpleSurface
                    ) {
                        Row(modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, null, tint = AppColors.PurplePrimary,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Enable Fall Alert in ⚙ Settings to send emails",
                                fontSize = 12.sp, color = AppColors.PurplePrimary, lineHeight = 18.sp)
                        }
                    }
                }

                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape    = RoundedCornerShape(AppDimens.radiusLg),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.TextSecondary
                    )
                ) { Text("Dismiss", fontSize = 14.sp) }
            }
        },
        containerColor = AppColors.CardWhite,
        shape          = RoundedCornerShape(AppDimens.radiusXl)
    )
}