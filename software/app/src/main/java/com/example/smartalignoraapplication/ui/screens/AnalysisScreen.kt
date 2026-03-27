package com.example.smartalignoraapplication.ui.screens

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartalignoraapplication.controller.BleViewModel
import com.example.smartalignoraapplication.controller.PostureSession
import com.example.smartalignoraapplication.ui.theme.AppColors
import com.example.smartalignoraapplication.ui.theme.AppDimens
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// Filter options
// ─────────────────────────────────────────────────────────────────────────────
enum class AnalysisFilter(
    val label: String,
    val days: Int,
    val groupBy: String  // "hour" / "day"
) {
    DAILY   ("Today",   1,  "hour"),
    WEEKLY  ("7 Days",  7,  "day"),
    MONTHLY ("30 Days", 30, "day")
}

// ─────────────────────────────────────────────────────────────────────────────
// Data point — one bucket in the chart
// ─────────────────────────────────────────────────────────────────────────────
data class ChartPoint(
    val label:     String,
    val avgPitch:  Float,
    val goodCount: Int,
    val badCount:  Int
)

// ─────────────────────────────────────────────────────────────────────────────
// AnalysisScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AnalysisScreen(
    viewModel: BleViewModel
) {
    val context     = LocalContext.current
    var filter      by remember { mutableStateOf(AnalysisFilter.DAILY) }
    val pitchData   = viewModel.pitchData
    val isLoading   = viewModel.isLoadingData.value
    val isConnected = viewModel.isConnected.value
    val sessions    = viewModel.postureSessions
    val isExporting  = viewModel.isExporting.value
    val exportUri    = viewModel.exportUri.value
    val exportError  = viewModel.exportError.value
    val loadError    = viewModel.loadError.value
    val firebaseUid  = viewModel.firebaseUid.value

    // ── Load ALWAYS 30 days ONCE — switching Today/7D/30D only re-filters
    //    locally in buildChartPoints. This is the fix for "yesterday's data
    //    not showing": before, LaunchedEffect(filter) with DAILY overwrote the
    //    full dataset with only 24 h of data.
    // ─────────────────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.loadPitchDataFromFirebase(30)   // always max range
        viewModel.loadSessionsFromFirebase()
    }

    // ── Open report when export URI ready (MediaStore Downloads URI) ────────
    LaunchedEffect(exportUri) {
        if (exportUri != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(exportUri, "text/html")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(intent, "Open Posture Report")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (_: Exception) {
                // Some devices can't open HTML directly — file is saved in Downloads anyway
            }
            viewModel.clearExportUri()
        }
    }

    // Build chart points from pitchData — pure local filter, no network call
    val chartPoints = remember(pitchData.toList(), filter) {
        buildChartPoints(pitchData.toList(), filter)
    }

    val totalGood  = chartPoints.sumOf { it.goodCount }
    val totalBad   = chartPoints.sumOf { it.badCount }
    val total      = totalGood + totalBad
    val goodPct    = if (total > 0) totalGood * 100f / total else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SoftBg)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(AppColors.BlueNavy, Color(0xFF1A5276)))
                )
                .padding(horizontal = AppDimens.paddingLg, vertical = 20.dp)
        ) {
            Column {
                Text("Posture Analysis", color = Color.White,
                    fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("Track your posture trends over time",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                if (pitchData.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("${pitchData.size} readings loaded (last 30 days)",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }
            // Refresh button — top-end corner
            IconButton(
                onClick = {
                    viewModel.loadPitchDataFromFirebase(30)
                    viewModel.loadSessionsFromFirebase()
                },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Refresh, "Refresh",
                        tint = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        // ── "Not Connected" Firebase data banner ──────────────────────────────
        if (!isConnected) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF7ED))
                    .padding(horizontal = AppDimens.paddingLg, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        "Device not connected · Showing saved data",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFB45309)
                    )
                    Text(
                        "Connect your SmartAlignOra device to record new data",
                        fontSize = 11.sp,
                        color = Color(0xFFD97706)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(AppDimens.paddingLg),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Load Error / Debug Card ──────────────────────────────────────
            if (loadError != null || (pitchData.isEmpty() && !isLoading)) {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(AppDimens.radiusLg),
                    colors    = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, null,
                                tint = Color(0xFFB91C1C), modifier = Modifier.size(16.dp))
                            Text("Data Debug Info",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C))
                        }
                        if (loadError != null) {
                            Text(loadError, fontSize = 11.sp, color = Color(0xFFDC2626))
                        }
                        if (firebaseUid != null) {
                            Text("Firebase UID: $firebaseUid",
                                fontSize = 10.sp, color = Color(0xFF64748B))
                            Text("Check Firebase Console → users/$firebaseUid/pitch/",
                                fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                        // Retry button
                        OutlinedButton(
                            onClick = {
                                viewModel.loadPitchDataFromFirebase(30)
                                viewModel.loadSessionsFromFirebase()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry Loading Data", fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Filter chips ──────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(AppDimens.radiusXl),
                colors    = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalysisFilter.entries.forEach { f ->
                        val selected = filter == f
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(AppDimens.radiusLg))
                                .background(
                                    if (selected) AppColors.BlueNavy else AppColors.SoftBg
                                )
                                .clickable { filter = f }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                f.label,
                                fontSize   = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color      = if (selected) Color.White else AppColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // ── Summary stats ─────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(Modifier.weight(1f), "Good",
                    "${"%.0f".format(goodPct)}%", "$totalGood readings",
                    AppColors.GoodGreen, AppColors.GoodBg)
                SummaryCard(Modifier.weight(1f), "Bad",
                    "${"%.0f".format(100f - goodPct)}%", "$totalBad readings",
                    AppColors.BadRed, AppColors.BadBg)
            }

            // ── Pitch Line Chart ──────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(AppDimens.radiusXl),
                colors    = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timeline, null,
                            tint = AppColors.PurplePrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pitch Angle Over Time", fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                        Spacer(Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = AppColors.PurpleSurface) {
                            Text("degrees°", fontSize = 10.sp,
                                color = AppColors.PurplePrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(180.dp),
                            contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = AppColors.PurplePrimary, modifier = Modifier.size(32.dp))
                        }
                    } else if (chartPoints.isEmpty()) {
                        EmptyChart("No pitch data for this period")
                    } else {
                        PitchLineChart(chartPoints)
                    }
                }
            }

            // ── Good/Bad Bar Chart ────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(AppDimens.radiusXl),
                colors    = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, null,
                            tint = AppColors.ElectricBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Good vs Bad Posture", fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot("Good", AppColors.GoodGreen)
                        LegendDot("Bad",  AppColors.BadRed)
                    }

                    Spacer(Modifier.height(16.dp))

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = AppColors.ElectricBlue, modifier = Modifier.size(32.dp))
                        }
                    } else if (chartPoints.isEmpty()) {
                        EmptyChart("No data for this period")
                    } else {
                        PostureBarChart(chartPoints)
                    }
                }
            }

            // ── Donut Chart ───────────────────────────────────────────────────
            if (total > 0) {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(AppDimens.radiusXl),
                    colors    = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier            = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DonutLarge, null,
                                tint = AppColors.TealAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("${filter.label} Summary", fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                        }

                        Spacer(Modifier.height(24.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier.size(180.dp)
                        ) {
                            AnimatedDonutChart(goodPct)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${"%.0f".format(goodPct)}%",
                                    fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                                    color = AppColors.GoodGreen)
                                Text("Good", fontSize = 12.sp,
                                    color = AppColors.TextSecondary)
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DonutStat("Good Posture", totalGood, AppColors.GoodGreen)
                            DonutStat("Bad Posture",  totalBad,  AppColors.BadRed)
                            DonutStat("Total", total, AppColors.PurplePrimary)
                        }
                    }
                }
            }

            // ── Posture score card ────────────────────────────────────────────
            if (total > 0) {
                PostureScoreCard(goodPct, filter.label)
            }

            // ── Recent Sessions from Firebase ─────────────────────────────────
            if (sessions.isNotEmpty()) {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(AppDimens.radiusXl),
                    colors    = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, null,
                                tint = AppColors.TealAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Recent Sessions", fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                            Spacer(Modifier.weight(1f))
                            Text("${sessions.size} total", fontSize = 11.sp,
                                color = AppColors.TextSecondary)
                        }

                        Spacer(Modifier.height(12.dp))

                        sessions.take(7).forEach { session ->
                            SessionRow(session)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            // ── Download Report Button ─────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(AppDimens.radiusXl),
                colors    = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier            = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, null,
                            tint = AppColors.BlueNavy, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Export Report", fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Download a visual report with charts, session history & posture score",
                        fontSize = 12.sp,
                        color    = AppColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick  = { viewModel.exportReport(context) },
                        enabled  = !isExporting,
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(AppDimens.radiusLg),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = AppColors.BlueNavy,
                            contentColor   = Color.White
                        )
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                color    = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Building report…", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Default.FileDownload, null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Download HTML Report", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
                            "Saves to Downloads folder · opens in browser"
                        else
                            "Shared via any app (Gmail, Drive, etc.)",
                        fontSize  = 10.sp,
                        color     = AppColors.TextTertiary,
                        textAlign = TextAlign.Center
                    )
                    if (exportError != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(exportError,
                            fontSize = 11.sp,
                            color    = AppColors.BadRed,
                            textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// =============================================================================
// SESSION ROW — single line in recent sessions list
// =============================================================================
@Composable
private fun SessionRow(session: PostureSession) {
    val total    = (session.goodCount + session.badCount).coerceAtLeast(1)
    val pct      = session.goodCount * 100 / total
    val barColor = when {
        pct >= 70 -> AppColors.GoodGreen
        pct >= 40 -> Color(0xFFF59E0B)
        else      -> AppColors.BadRed
    }
    val durMins = session.durationSeconds / 60
    val durSecs = session.durationSeconds % 60

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Date chip
        Surface(
            shape  = RoundedCornerShape(8.dp),
            color  = AppColors.SoftBg
        ) {
            Text(
                session.date,
                fontSize = 10.sp,
                color    = AppColors.TextSecondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Progress bar + pct
        Column(modifier = Modifier.weight(1f)) {
            val animW by animateFloatAsState(
                targetValue   = pct / 100f,
                animationSpec = tween(600),
                label         = "session_bar_${session.date}"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppColors.BorderColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animW)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(barColor)
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Text("$pct% good · ${durMins}m ${durSecs}s",
                    fontSize = 10.sp, color = AppColors.TextSecondary)
                Text("pitch ${"%.1f".format(session.avgPitch)}°",
                    fontSize = 10.sp, color = AppColors.TextSecondary)
            }
        }

        // Score badge
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(barColor.copy(alpha = 0.15f))
        ) {
            Text("$pct%", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = barColor,
                textAlign = TextAlign.Center)
        }
    }
}

// =============================================================================
// CHART BUILDERS
// =============================================================================

fun buildChartPoints(
    data:   List<Map<String, Any>>,
    filter: AnalysisFilter
): List<ChartPoint> {
    if (data.isEmpty()) return emptyList()

    val buckets = LinkedHashMap<String, MutableList<Map<String, Any>>>()

    // ── Correct cutoff calculation ────────────────────────────────────────────
    // BUG was: "days=1" meant "last 24 hours" so yesterday evening's data
    //          appeared under "Today" tab.
    // FIX:     DAILY  → midnight of today (00:00:00.000)
    //          WEEKLY → midnight 7 days ago
    //          MONTHLY→ midnight 30 days ago
    val cutoff = when (filter) {
        AnalysisFilter.DAILY -> {
            // Start of TODAY at 00:00:00.000 in local timezone
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        else -> {
            // Start of day, N days ago
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, -(filter.days - 1))
            }.timeInMillis
        }
    }

    val relevant = data.filter { row ->
        val ts = (row["timestamp"] as? Number)?.toLong() ?: 0L
        ts >= cutoff
    }

    relevant.forEach { row ->
        val ts  = (row["timestamp"] as? Number)?.toLong() ?: return@forEach
        val cal = Calendar.getInstance().apply { timeInMillis = ts }

        val key = when (filter.groupBy) {
            "hour" -> {
                val h    = cal.get(Calendar.HOUR_OF_DAY)
                val ampm = if (h < 12) "am" else "pm"
                val h12  = if (h == 0) 12 else if (h > 12) h - 12 else h
                "${h12}${ampm}"
            }
            else -> SimpleDateFormat("EEE d", Locale.getDefault()).format(Date(ts))
        }

        buckets.getOrPut(key) { mutableListOf() }.add(row)
    }

    return buckets.map { (key, rows) ->
        val pitches   = rows.mapNotNull { (it["pitch"] as? Number)?.toFloat() }
        val avgPitch  = if (pitches.isNotEmpty()) pitches.average().toFloat() else 0f
        val goodCount = rows.count { it["postureLabel"] == "GOOD POSTURE" }
        val badCount  = rows.count { it["postureLabel"] == "BAD POSTURE" }
        ChartPoint(key, avgPitch, goodCount, badCount)
    }
}

// =============================================================================
// PITCH LINE CHART
// =============================================================================
@Composable
fun PitchLineChart(points: List<ChartPoint>) {
    val animProgress by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label         = "line_chart"
    )

    val pitchValues = points.map { it.avgPitch }
    val maxAbs      = pitchValues.maxOfOrNull { abs(it) }?.coerceAtLeast(5f) ?: 30f

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            if (points.isEmpty()) return@Canvas

            val padL  = 40f
            val padR  = 16f
            val padT  = 16f
            val padB  = 32f
            val w     = size.width - padL - padR
            val h     = size.height - padT - padB
            val midY  = padT + h / 2f

            // Grid lines
            val gridLines = listOf(-30f, -15f, 0f, 15f, 30f)
            gridLines.forEach { v ->
                if (abs(v) <= maxAbs + 5f) {
                    val y = midY - (v / (maxAbs + 5f)) * (h / 2f)
                    drawLine(
                        color       = if (v == 0f) Color(0x40000000) else Color(0x15000000),
                        start       = Offset(padL, y),
                        end         = Offset(padL + w, y),
                        strokeWidth = if (v == 0f) 1.5f else 1f,
                        pathEffect  = if (v == 0f) null else
                            PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            "${v.toInt()}°",
                            padL - 8f,
                            y + 4f,
                            android.graphics.Paint().apply {
                                color     = android.graphics.Color.parseColor("#94A3B8")
                                textSize  = 22f
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                        )
                    }
                }
            }

            // Gradient fill + line
            val step = if (points.size > 1) w / (points.size - 1) else w
            val pts  = points.mapIndexed { i, p ->
                val x = padL + i * step
                val y = midY - (p.avgPitch / (maxAbs + 5f)) * (h / 2f)
                Offset(x, y)
            }

            if (pts.size >= 2) {
                val path = Path().apply {
                    moveTo(pts[0].x, midY)
                    pts.forEach { lineTo(it.x, it.y) }
                    lineTo(pts.last().x, midY)
                    close()
                }
                drawPath(
                    path  = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x306B21A8), Color(0x006B21A8)),
                        startY = padT, endY = padT + h
                    )
                )

                for (i in 0 until (pts.size - 1).coerceAtMost((pts.size * animProgress).toInt())) {
                    drawLine(
                        color       = Color(0xFF6B21A8),
                        start       = pts[i],
                        end         = pts[i + 1],
                        strokeWidth = 2.5f,
                        cap         = StrokeCap.Round
                    )
                }
            }

            pts.forEachIndexed { i, p ->
                val color = when {
                    points[i].avgPitch > 10f  -> Color(0xFFDC2626)
                    points[i].avgPitch < -10f -> Color(0xFFEA580C)
                    else                       -> Color(0xFF16A34A)
                }
                drawCircle(Color.White, 6f, p)
                drawCircle(color, 4f, p)
            }

            val labelStep = (points.size / 6).coerceAtLeast(1)
            pts.forEachIndexed { i, p ->
                if (i % labelStep == 0 || i == pts.size - 1) {
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            points[i].label,
                            p.x,
                            size.height - 4f,
                            android.graphics.Paint().apply {
                                color     = android.graphics.Color.parseColor("#64748B")
                                textSize  = 20f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            LegendDot("Forward lean", Color(0xFFDC2626))
            Spacer(Modifier.width(16.dp))
            LegendDot("Upright", Color(0xFF16A34A))
            Spacer(Modifier.width(16.dp))
            LegendDot("Back lean", Color(0xFFEA580C))
        }
    }
}

// =============================================================================
// POSTURE BAR CHART — Good vs Bad per bucket
// =============================================================================
@Composable
fun PostureBarChart(points: List<ChartPoint>) {
    val animProgress by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label         = "bar_chart"
    )

    val maxVal = points.maxOfOrNull { it.goodCount + it.badCount }?.coerceAtLeast(1) ?: 1

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val padL  = 8f
        val padR  = 8f
        val padT  = 8f
        val padB  = 28f
        val w     = size.width - padL - padR
        val h     = size.height - padT - padB

        val barGroupW = w / points.size
        val barW      = (barGroupW * 0.35f).coerceAtLeast(4f)
        val gap       = 2f

        points.forEachIndexed { i, p ->
            val centerX = padL + i * barGroupW + barGroupW / 2f
            val goodH   = (p.goodCount.toFloat() / maxVal) * h * animProgress
            val badH    = (p.badCount.toFloat()  / maxVal) * h * animProgress

            if (goodH > 0) {
                drawRoundRect(
                    color        = Color(0xFF16A34A),
                    topLeft      = Offset(centerX - barW - gap, padT + h - goodH),
                    size         = Size(barW, goodH),
                    cornerRadius = CornerRadius(4f)
                )
            }

            if (badH > 0) {
                drawRoundRect(
                    color        = Color(0xFFDC2626),
                    topLeft      = Offset(centerX + gap, padT + h - badH),
                    size         = Size(barW, badH),
                    cornerRadius = CornerRadius(4f)
                )
            }

            val labelStep = (points.size / 6).coerceAtLeast(1)
            if (i % labelStep == 0 || i == points.size - 1) {
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        p.label,
                        centerX,
                        size.height - 4f,
                        android.graphics.Paint().apply {
                            color     = android.graphics.Color.parseColor("#64748B")
                            textSize  = 20f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

// =============================================================================
// ANIMATED DONUT CHART
// =============================================================================
@Composable
fun AnimatedDonutChart(goodPct: Float) {
    val animSweep by animateFloatAsState(
        targetValue   = goodPct * 3.6f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label         = "donut"
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        val stroke = 28f
        val inset  = stroke / 2f

        drawArc(
            color      = Color(0xFFDC2626),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter  = false,
            style      = Stroke(stroke, cap = StrokeCap.Round),
            topLeft    = Offset(inset, inset),
            size       = Size(size.width - stroke, size.height - stroke)
        )

        if (animSweep > 0f) {
            drawArc(
                brush      = Brush.sweepGradient(listOf(Color(0xFF16A34A), Color(0xFF22C55E))),
                startAngle = -90f,
                sweepAngle = animSweep,
                useCenter  = false,
                style      = Stroke(stroke, cap = StrokeCap.Round),
                topLeft    = Offset(inset, inset),
                size       = Size(size.width - stroke, size.height - stroke)
            )
        }
    }
}

// =============================================================================
// POSTURE SCORE CARD — A/B/C/D grade
// =============================================================================
@Composable
fun PostureScoreCard(goodPct: Float, periodLabel: String) {
    val grade = when {
        goodPct >= 90f -> "A" to "Excellent!"
        goodPct >= 75f -> "B" to "Good job"
        goodPct >= 60f -> "C" to "Keep going"
        goodPct >= 40f -> "D" to "Needs work"
        else           -> "F" to "Let's improve"
    }
    val gradeColor = when (grade.first) {
        "A"  -> AppColors.GoodGreen
        "B"  -> Color(0xFF16A34A)
        "C"  -> Color(0xFFF59E0B)
        "D"  -> Color(0xFFEA580C)
        else -> AppColors.BadRed
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(AppDimens.radiusXl),
        colors    = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier          = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(gradeColor.copy(alpha = 0.15f))
                    .border(2.dp, gradeColor, CircleShape)
            ) {
                Text(grade.first, fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold, color = gradeColor)
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text("$periodLabel Score: ${grade.second}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("You had good posture ${"%.0f".format(goodPct)}% of the time",
                    fontSize = 13.sp, color = AppColors.TextSecondary)
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AppColors.BorderColor)
                ) {
                    val animW by animateFloatAsState(
                        targetValue   = goodPct / 100f,
                        animationSpec = tween(800),
                        label         = "score_bar"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animW)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(gradeColor)
                    )
                }
            }
        }
    }
}

// =============================================================================
// SMALL REUSABLE COMPOSABLES
// =============================================================================

@Composable
private fun SummaryCard(
    modifier: Modifier,
    label:    String,
    pct:      String,
    sub:      String,
    color:    Color,
    bgColor:  Color
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(AppDimens.radiusLg),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(pct, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 12.sp, color = color.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(sub, fontSize = 11.sp, color = color.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun EmptyChart(message: String) {
    Box(
        modifier         = Modifier.fillMaxWidth().height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.BarChart, null,
                tint = AppColors.TextTertiary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(message, fontSize = 13.sp, color = AppColors.TextTertiary,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = AppColors.TextSecondary)
    }
}

@Composable
private fun DonutStat(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 11.sp, color = AppColors.TextSecondary,
            textAlign = TextAlign.Center)
    }
}