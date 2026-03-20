package com.example.smartalignora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val PlayerPurple      = Color(0xFF6B21A8)
private val PlayerPurpleLight = Color(0xFFF3E8FF)
private val PlayerBg          = Color(0xFFF8F4FF)
private val PlayerTextGray    = Color(0xFF6B7280)
private val PlayerRed         = Color(0xFFDC2626)

data class FallDataRow(
    val timestampMs: Long,
    val pitch: Float,
    val roll: Float,
    val accMag: Float,
    val label: String
)

// ✅ CSV PARSER (FIXED)
fun parseFallCsv(csvText: String): List<FallDataRow> {
    val lines = csvText.trim().split("\n")
    val rows = mutableListOf<FallDataRow>()

    for (i in 1 until lines.size) {
        val line = lines[i].trim()
        if (line.isEmpty()) continue

        val cols = line.split(",")
        if (cols.size < 11) continue

        try {
            rows.add(
                FallDataRow(
                    timestampMs = cols[0].trim().toLongOrNull() ?: 0L,
                    pitch = cols[1].trim().toFloatOrNull() ?: 0f,
                    roll = cols[2].trim().toFloatOrNull() ?: 0f,
                    accMag = cols[6].trim().toFloatOrNull() ?: 0f,
                    label = when (cols[10].trim().lowercase()) {
                        "fall" -> "fall"
                        else -> "safe"
                    }
                )
            )
        } catch (e: Exception) { }
    }
    return rows
}

@Composable
fun FallDataPlayerScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    // ✅ LOAD YOUR CSV (FILE NAME FIXED)
    val csvRows = remember {
        val text = context.assets.open("atharva_prefall_fall.csv")
            .bufferedReader()
            .readText()
        parseFallCsv(text)
    }

    // ✅ TIME NORMALIZATION
    val startTime = csvRows.firstOrNull()?.timestampMs ?: 0L

    var currentIndex by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }

    val currentRow = if (csvRows.isNotEmpty() && currentIndex < csvRows.size)
        csvRows[currentIndex] else null

    // ✅ REAL-TIME PLAYBACK
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && currentIndex < csvRows.size - 1) {

                val currentTime = csvRows[currentIndex].timestampMs
                val nextTime = csvRows[currentIndex + 1].timestampMs

                val delayTime = ((nextTime - currentTime) / playbackSpeed)
                    .toLong()
                    .coerceAtLeast(5L)

                delay(delayTime)
                currentIndex++

                if (currentIndex >= csvRows.size - 1) {
                    isPlaying = false
                    break
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerBg)
    ) {

        // 🔹 TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Fall Detection Test",
                color = PlayerPurple,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold)

            Text("${currentIndex + 1}/${csvRows.size}",
                color = PlayerPurple)
        }

        // 🔹 SENSOR VALUES
        currentRow?.let { row ->
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pitch: ${row.pitch}")
                Text("Roll: ${row.roll}")
                Text("Acc: ${row.accMag}")
                Text(
                    "Label: ${row.label.uppercase()}",
                    color = if (row.label == "fall") PlayerRed else Color.Green
                )
            }
        }

        // 🔹 ANIMATION
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .background(Color.White)
        ) {
            PostureAnimationScreen(
                currentPitch = currentRow?.pitch ?: 0f,
                currentRoll = currentRow?.roll ?: 0f,
                fallLabel = currentRow?.label ?: "safe",
                fallProbability = if (currentRow?.label == "fall") 0.95f else 0.05f
            )
        }

        // 🔹 TIME BAR (FIXED)
        Column(Modifier.padding(16.dp)) {

            LinearProgressIndicator(
                progress = if (csvRows.size > 1)
                    currentIndex.toFloat() / (csvRows.size - 1)
                else 0f,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text("0s")

                Text(
                    currentRow?.let {
                        "${"%.1f".format((it.timestampMs - startTime) / 1000f)}s"
                    } ?: "0s"
                )

                Text(
                    csvRows.lastOrNull()?.let {
                        "${"%.0f".format((it.timestampMs - startTime) / 1000f)}s"
                    } ?: "0s"
                )
            }
        }

        // 🔹 CONTROLS
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {

            IconButton(onClick = {
                currentIndex = 0
                isPlaying = false
            }) {
                Icon(Icons.Default.Refresh, contentDescription = null)
            }

            Button(onClick = { isPlaying = !isPlaying }) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Text(if (isPlaying) "Pause" else "Play")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewScreen() {
    FallDataPlayerScreen()
}