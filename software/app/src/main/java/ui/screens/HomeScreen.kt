package com.example.smartalignora.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HomePurple      = Color(0xFF6B21A8)
private val HomePurpleMid   = Color(0xFF9333EA)
private val HomePurpleLight = Color(0xFFF3E8FF)
private val HomePurpleCard  = Color(0xFFFAF5FF)
private val HomeBg          = Color(0xFFF8F4FF)
private val HomeTextDark    = Color(0xFF1A1A2E)
private val HomeTextGray    = Color(0xFF6B7280)
private val HomeBarLight    = Color(0xFFDDD6FE)

@Composable
fun HomeScreen(
    onNavigateToAnalysis: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = HomeBg,
        bottomBar = {
            HomeBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                    when (index) {
                        1 -> onNavigateToAnalysis()
                        2 -> onNavigateToSettings()
                        3 -> onNavigateToProfile()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            HomeTopBar()
            Spacer(modifier = Modifier.height(20.dp))
            ConnectDeviceCard()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "SmartAlignora",
            color = HomePurple,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(HomePurpleLight),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🔔", fontSize = 18.sp)
        }
    }
}

@Composable
fun ConnectDeviceCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "📶", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Connect Device",
                    color = HomeTextDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            HomePostureCircle()
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HomeDeviceControl(emoji = "🎯", label = "Calibrate")
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(
                        checked = true,
                        onCheckedChange = {},
                        modifier = Modifier.height(28.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = HomePurple
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Vibration", color = HomeTextGray, fontSize = 11.sp)
                }
                HomeDeviceControl(emoji = "⏱️", label = "Delay")
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(HomeTextGray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Not connected", color = HomeTextGray, fontSize = 13.sp)
                }
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HomePurple),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(text = "Pair Now", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun HomePostureCircle() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokeWidth = 12.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            drawCircle(
                color = Color(0xFFE8F5E9),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = Color(0xFF4CAF50),
                startAngle = -210f,
                sweepAngle = 280f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F8E9))
        ) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.width(80.dp).height(110.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.TopCenter)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(Color(0xFF4CAF50))
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 20.dp, y = 8.dp)
        ) {
            Text(text = "❤️", fontSize = 12.sp)
        }
    }
}

@Composable
fun HomeDeviceControl(emoji: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(HomePurpleLight)
        ) {
            Text(text = emoji, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = HomeTextGray, fontSize = 11.sp)
    }
}


@Composable
fun HomeBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Pair("Home",     Icons.Default.Home),
            Pair("Analysis", Icons.Default.Search),
            Pair("Settings", Icons.Default.Settings),
            Pair("Profile",  Icons.Default.Person)
        )
        items.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp)) },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = HomePurple,
                    selectedTextColor = HomePurple,
                    unselectedIconColor = HomeTextGray,
                    unselectedTextColor = HomeTextGray,
                    indicatorColor = HomePurpleLight
                )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen()
}