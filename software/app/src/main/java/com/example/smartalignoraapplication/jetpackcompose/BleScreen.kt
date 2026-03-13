package com.example.smartalignoraapplication.jetpackcompose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    currentPitch: Float,      // ✅ ORIGINAL (UNCHANGED)
    postureState: String,     // ✅ NEW (ML Prediction)
    alertState: String,       // ✅ NEW (Alerts)
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onClearDataClick: () -> Unit
) {

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, null)
                        }
                    }
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
                        label = { Text("Visual") },
                        icon = { Icon(Icons.Default.Person, null) }
                    )
                }
            }

        ) { paddingValues ->

            Column(modifier = Modifier.padding(paddingValues)) {

                if (selectedTab == 0) {

                    DataLogScreen(
                        status = status,
                        isConnected = isConnected,
                        receivedData = receivedData,
                        onConnectClick = onConnectClick,
                        onDisconnectClick = onDisconnectClick,
                        onClearDataClick = onClearDataClick
                    )

                } else {

                    PostureAnimationWithPrediction(
                        currentPitch = currentPitch,
                        postureState = postureState,
                        alertState = alertState
                    )
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Card(
            colors = CardDefaults.cardColors(
                containerColor =
                    if (isConnected) Color(0xFFE8F5E9)
                    else Color(0xFFFFEBEE)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = status,
                modifier = Modifier.padding(16.dp),
                color = Color.Black
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            Button(
                onClick = onConnectClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Connect")
            }

            Button(
                onClick = onDisconnectClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Stop")
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onClearDataClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Logs")
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn {

            items(receivedData) { item ->

                Text(
                    text = item,
                    modifier = Modifier.padding(8.dp)
                )

                Divider()
            }
        }
    }
}



@Composable
fun PostureAnimationWithPrediction(
    currentPitch: Float,
    postureState: String,
    alertState: String
) {

    val postureColor =
        if (postureState.contains("GOOD"))
            Color(0xFF4CAF50)
        else
            Color(0xFFF44336)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // ✅ POSTURE STATUS

        Card(modifier = Modifier.fillMaxWidth()) {

            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Posture Status",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = postureState,
                    fontSize = 24.sp,
                    color = postureColor
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ✅ ALERT MESSAGE

        if (alertState.isNotEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {

                Text(
                    text = alertState,
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFFF57C00)
                )
            }

            Spacer(Modifier.height(10.dp))
        }

        // ✅ ORIGINAL ANIMATION (UNCHANGED)

        PostureAnimationScreen(currentPitch = currentPitch)
    }
}

