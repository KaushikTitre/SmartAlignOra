package com.example.smartalignoraapplication.jetpackcompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleScreen(
    status: String,
    isConnected: Boolean,
    receivedData: List<String>,
    currentPitch: Float, // <--- NEW PARAMETER
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onClearDataClick: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // TAB STATE (0 = Logs, 1 = Animation)
    var selectedTab by remember { mutableStateOf(0) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("SmartAlign Menu", modifier = Modifier.padding(16.dp))
                Divider()
                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Home, null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("SmartAlignOra") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Data Logs") },
                        icon = { Icon(Icons.Default.List, null) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Visual") }, // New Tab
                        icon = { Icon(Icons.Default.Person, null) }
                    )
                }
            }
        ) { paddingValues ->

            Column(modifier = Modifier.padding(paddingValues)) {

                // Content Switcher
                if (selectedTab == 0) {
                    // --- TAB 1: DATA LOGS ---
                    DataLogScreen(
                        status, isConnected, receivedData,
                        onConnectClick, onDisconnectClick, onClearDataClick
                    )
                } else {
                    // --- TAB 2: ANIMATION ---
                    PostureAnimationScreen(currentPitch = currentPitch)
                }
            }
        }
    }
}

// जुन्या BleScreen मधील UI इथे Move केला आहे (Clean Code साठी)
@Composable
fun DataLogScreen(
    status: String,
    isConnected: Boolean,
    receivedData: List<String>,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onClearDataClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = if(isConnected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = status, modifier = Modifier.padding(16.dp), color = Color.Black)
        }

        Spacer(Modifier.height(10.dp))

        // Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onConnectClick, modifier = Modifier.weight(1f)) { Text("Connect") }
            Button(onClick = onDisconnectClick, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Stop") }
        }

        Spacer(Modifier.height(10.dp))

        // List
        LazyColumn {
            items(receivedData) { item ->
                Text(text = item, modifier = Modifier.padding(8.dp))
                Divider()
            }
        }
    }
}