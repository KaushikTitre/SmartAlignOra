package com.example.smartalignoraapplication.jetpackcompose

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartalignoraapplication.controller.BleViewModel
import kotlinx.coroutines.launch


import com.example.smartalignoraapplication.ui.theme.AppColors

import com.example.smartalignoraapplication.ui.components.PostureAnimationScreen
import com.example.smartalignoraapplication.ui.components.SettingsBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleScreen(
    // ── your original parameters — 100% unchanged ────────────────────────────
    status: String,
    isConnected: Boolean,
    receivedData: List<String>,
    currentPitch: Float,
    postureState: String,
    alertState: String,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onClearDataClick: () -> Unit
) {
    // ── ViewModel — same instance used everywhere in this tree ───────────────
    val viewModel: BleViewModel = viewModel()

    // Read settings state from ViewModel
    val settings = viewModel.settingsState

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope       = rememberCoroutineScope()

    var selectedTab  by remember { mutableStateOf(0) }

    // ── NEW: controls whether settings sheet is visible ──────────────────────
    var showSettings by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // ── your original drawer — untouched ─────────────────────────────
            ModalDrawerSheet {
                Text("SmartAlign Menu", modifier = Modifier.padding(16.dp))
                Divider()
                NavigationDrawerItem(
                    label    = { Text("Dashboard") },
                    selected = true,
                    onClick  = { scope.launch { drawerState.close() } },
                    icon     = { Icon(Icons.Default.Home, null) }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = AppColors.SoftBg,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "SmartAlignOra",
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, null, tint = Color.White)
                        }
                    },
                    // ── NEW: settings gear icon — only on Visual tab ──────────
                    actions = {
                        if (selectedTab == 1) {
                            IconButton(
                                onClick  = { showSettings = true },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.Settings,
                                    contentDescription = "Open settings",
                                    tint               = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppColors.NavyBlue
                    )
                )
            },
            bottomBar = {
                // ── your original bottom nav — untouched ──────────────────────
                NavigationBar(
                    containerColor = AppColors.CardWhite
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick  = { selectedTab = 0 },
                        label    = { Text("Data Logs") },
                        icon     = { Icon(Icons.Default.List, null) },
                        colors   = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppColors.NavyBlue,
                            selectedTextColor = AppColors.NavyBlue,
                            indicatorColor    = AppColors.ElectricBlue.copy(alpha = 0.12f),
                            unselectedIconColor = AppColors.TextSecondary,
                            unselectedTextColor = AppColors.TextSecondary
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick  = { selectedTab = 1 },
                        label    = { Text("Visual") },
                        icon     = { Icon(Icons.Default.Person, null) },
                        colors   = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppColors.NavyBlue,
                            selectedTextColor = AppColors.NavyBlue,
                            indicatorColor    = AppColors.ElectricBlue.copy(alpha = 0.12f),
                            unselectedIconColor = AppColors.TextSecondary,
                            unselectedTextColor = AppColors.TextSecondary
                        )
                    )
                }
            }
        ) { paddingValues ->

            Column(modifier = Modifier.padding(paddingValues)) {

                if (selectedTab == 0) {
                    // ── your original DataLogScreen — untouched ───────────────
                    DataLogScreen(
                        status            = status,
                        isConnected       = isConnected,
                        receivedData      = receivedData,
                        onConnectClick    = onConnectClick,
                        onDisconnectClick = onDisconnectClick,
                        onClearDataClick  = onClearDataClick
                    )
                } else {
                    // ── your original PostureAnimationWithPrediction — untouched
                    PostureAnimationWithPrediction(
                        currentPitch = currentPitch,
                        postureState = postureState,
                        alertState   = alertState
                    )
                }
            }
        }

        // ── NEW: Settings sheet — wired to ViewModel ─────────────────────────
        // Renders on top of Scaffold when showSettings = true
        // Passes ViewModel state down, events back up
        if (showSettings) {
            SettingsBottomSheet(
                state                      = settings,
                onDismiss                  = { showSettings = false },
                onVibrationToggled         = { viewModel.onVibrationToggled(it) },
                onVibrationLevelChanged    = { viewModel.onVibrationLevelChanged(it) },
                onVibrationLevelFinished   = { viewModel.onVibrationLevelFinished() },
                onVibrationSequenceChanged = { viewModel.onVibrationSequenceChanged(it) }  // ← ADD THIS
            )
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// DataLogScreen — your original code, completely untouched
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DataLogScreen(
    status: String,
    isConnected: Boolean,
    receivedData: List<String>,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onClearDataClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isConnected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text     = status,
                modifier = Modifier.padding(16.dp),
                color    = Color.Black
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick  = onConnectClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Connect")
            }
            Button(
                onClick  = onDisconnectClick,
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Stop")
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick  = onClearDataClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Logs")
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn {
            items(receivedData) { item ->
                Text(text = item, modifier = Modifier.padding(8.dp))
                Divider()
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// PostureAnimationWithPrediction — your original code, completely untouched
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PostureAnimationWithPrediction(
    currentPitch: Float,
    postureState: String,
    alertState: String
) {
    val postureColor =
        if (postureState.contains("GOOD")) Color(0xFF4CAF50)
        else Color(0xFFF44336)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier            = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Posture Status", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                Text(text = postureState, fontSize = 24.sp, color = postureColor)
            }
        }

        Spacer(Modifier.height(10.dp))

        if (alertState.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Text(
                    text     = alertState,
                    modifier = Modifier.padding(12.dp),
                    color    = Color(0xFFF57C00)
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        // Calls PostureAnimationScreen — slider REMOVED from there, now in Settings
        PostureAnimationScreen(currentPitch = currentPitch)
    }
}