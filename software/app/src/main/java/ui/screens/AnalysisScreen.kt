package com.example.smartalignora.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val APurple      = Color(0xFF6B21A8)
private val APurpleMid   = Color(0xFF9333EA)
private val APurpleLight = Color(0xFFF3E8FF)
private val ABg          = Color(0xFFF8F4FF)
private val ATextDark    = Color(0xFF1A1A2E)
private val ATextGray    = Color(0xFF6B7280)

@Composable
fun AnalysisScreen(onBack: () -> Unit = {}) {
    var selectedPeriod by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ABg)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = APurple)
            }
            Text(
                text = "Analysis",
                color = APurple,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        // Daily / Weekly / Monthly tabs
        ATabRow(selectedPeriod = selectedPeriod, onPeriodSelected = { selectedPeriod = it })

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            AScoreCard(selectedPeriod = selectedPeriod)
            Spacer(modifier = Modifier.height(16.dp))
            AStatsRow(selectedPeriod = selectedPeriod)
            Spacer(modifier = Modifier.height(16.dp))
            ABarChart(selectedPeriod = selectedPeriod)
            Spacer(modifier = Modifier.height(16.dp))
            ABreakdownCard()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ATabRow(selectedPeriod: Int, onPeriodSelected: (Int) -> Unit) {
    val periods = listOf("Daily", "Weekly", "Monthly")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        periods.forEachIndexed { index, period ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selectedPeriod == index) APurple
                        else Color.Transparent
                    )
                    .clickable { onPeriodSelected(index) }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = period,
                    color = if (selectedPeriod == index) Color.White else ATextGray,
                    fontSize = 14.sp,
                    fontWeight = if (selectedPeriod == index) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun AScoreCard(selectedPeriod: Int) {
    val score = when (selectedPeriod) { 0 -> 78; 1 -> 82; else -> 75 }
    val label = when (selectedPeriod) { 0 -> "Today's Score"; 1 -> "Weekly Average"; else -> "Monthly Average" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = ATextGray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val sw = 14.dp.toPx()
                    drawArc(color = APurpleLight, startAngle = -220f, sweepAngle = 260f, useCenter = false, style = Stroke(width = sw, cap = StrokeCap.Round))
                    drawArc(color = APurple, startAngle = -220f, sweepAngle = 260f * (score / 100f), useCenter = false, style = Stroke(width = sw, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$score", color = APurple, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                    Text(text = "/ 100", color = ATextGray, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(APurpleLight)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(text = "📈 +12% from last period", color = APurple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun AStatsRow(selectedPeriod: Int) {
    val sitting  = when (selectedPeriod) { 0 -> "3h 20m"; 1 -> "22h"; else -> "88h" }
    val standing = when (selectedPeriod) { 0 -> "1h 45m"; 1 -> "12h"; else -> "48h" }
    val alerts   = when (selectedPeriod) { 0 -> "4"; 1 -> "28"; else -> "112" }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AStatItem(label = "Sitting",  value = sitting,  modifier = Modifier.weight(1f))
        AStatItem(label = "Standing", value = standing, modifier = Modifier.weight(1f))
        AStatItem(label = "Alerts",   value = alerts,   modifier = Modifier.weight(1f))
    }
}

@Composable
fun AStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, color = APurple, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, color = ATextGray, fontSize = 11.sp)
        }
    }
}

@Composable
fun ABarChart(selectedPeriod: Int) {
    val labels  = when (selectedPeriod) { 0 -> listOf("6am","9am","12pm","3pm","6pm","9pm"); 1 -> listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun"); else -> listOf("W1","W2","W3","W4") }
    val heights = when (selectedPeriod) { 0 -> listOf(0.4f,0.7f,0.9f,0.6f,0.8f,0.5f); 1 -> listOf(0.5f,0.75f,0.9f,0.6f,0.8f,0.45f,0.7f); else -> listOf(0.65f,0.8f,0.7f,0.9f) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = when (selectedPeriod) { 0 -> "Today's Activity"; 1 -> "This Week"; else -> "This Month" },
                color = ATextDark, fontSize = 15.sp, fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                labels.forEachIndexed { index, label ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height((heights[index] * 80).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(Brush.verticalGradient(listOf(APurpleMid, APurple)))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = label, color = ATextGray, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ABreakdownCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Posture Breakdown", color = ATextDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            ABreakdownItem(label = "Good Posture", percent = 0.72f, color = Color(0xFF22C55E))
            Spacer(modifier = Modifier.height(10.dp))
            ABreakdownItem(label = "Slouching",    percent = 0.18f, color = APurple)
            Spacer(modifier = Modifier.height(10.dp))
            ABreakdownItem(label = "Forward Lean", percent = 0.10f, color = Color(0xFFF59E0B))
        }
    }
}

@Composable
fun ABreakdownItem(label: String, percent: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, color = ATextGray, fontSize = 13.sp)
            Text(text = "${(percent * 100).toInt()}%", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0xFFF3F4F6))
        ) {
            Box(modifier = Modifier.fillMaxWidth(percent).height(8.dp).clip(CircleShape).background(color))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AnalysisScreenPreview() {
    AnalysisScreen()
}
