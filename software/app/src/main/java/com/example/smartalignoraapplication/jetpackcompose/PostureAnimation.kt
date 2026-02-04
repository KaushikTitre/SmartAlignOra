package com.example.smartalignoraapplication.jetpackcompose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

@Composable
fun PostureAnimationScreen(currentPitch: Float) {

    val animatedPitch by animateFloatAsState(
        targetValue = currentPitch,
        animationSpec = tween(durationMillis = 400),
        label = "PostureAnim"
    )

    val isGoodPosture = animatedPitch.absoluteValue < 10
    val statusColor = if (isGoodPosture) Color(0xFF43A047) else Color(0xFFE53935)

    val skinColor = Color(0xFFFFCC80)
    val shirtColor = Color(0xFF1976D2)
    val pantsColor = Color(0xFF424242)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Posture Analysis", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${"%.1f".format(animatedPitch)}°",
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = statusColor
        )
        Text(
            text = if (animatedPitch > 15) "Leaning Forward"
            else if (animatedPitch < -15) "Leaning Back"
            else "Perfect Alignment",
            fontSize = 18.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp)) // Reduced spacer to fit larger canvas

        // --- INCREASED CANVAS SIZE ---
        Canvas(modifier = Modifier.size(360.dp, 550.dp)) {
            val cx = size.width / 2
            val groundY = size.height - 50f

            // --- SCALED UP DIMENSIONS (approx 1.5x larger) ---
            val legHeight = 250f  // Was 180f
            val hipY = groundY - legHeight

            val torsoHeight = 220f // Was 160f
            val torsoWidth = 75f   // Was 50f
            val headRadius = 50f   // Was 35f
            val legWidth = 65f     // Was 45f
            val neckLen = 30f      // Was 20f
            val armWidth = 30f     // Was 20f

            // 1. REFERENCE LINE
            drawLine(
                color = Color.LightGray,
                start = Offset(cx, groundY),
                end = Offset(cx, groundY - 550),
                strokeWidth = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
            )

            // 2. LEGS
            drawLine(
                color = pantsColor,
                start = Offset(cx, hipY),
                end = Offset(cx + 15, groundY), // Slight natural stance
                strokeWidth = legWidth,
                cap = StrokeCap.Round
            )

            // 3. UPPER BODY
            withTransform({
                rotate(degrees = animatedPitch, pivot = Offset(cx, hipY))
            }) {

                // A. TORSO
                drawLine(
                    color = shirtColor,
                    start = Offset(cx, hipY),
                    end = Offset(cx, hipY - torsoHeight),
                    strokeWidth = torsoWidth,
                    cap = StrokeCap.Butt
                )
                // Round shoulder
                drawCircle(
                    color = shirtColor,
                    radius = torsoWidth / 2,
                    center = Offset(cx, hipY - torsoHeight)
                )

                // B. NECK
                drawLine(
                    color = skinColor,
                    start = Offset(cx, hipY - torsoHeight),
                    end = Offset(cx, hipY - torsoHeight - neckLen),
                    strokeWidth = 35f
                )

                // C. HEAD
                drawCircle(
                    color = skinColor,
                    radius = headRadius,
                    center = Offset(cx + 8, hipY - torsoHeight - neckLen - (headRadius * 0.8f))
                )

                // Eye
                drawCircle(
                    color = Color.Black,
                    radius = 6f,
                    center = Offset(cx + 35, hipY - torsoHeight - neckLen - (headRadius * 0.9f))
                )

                // D. ARM
                drawLine(
                    color = skinColor,
                    start = Offset(cx, hipY - torsoHeight + 30), // Shoulder
                    end = Offset(cx + 15, hipY - torsoHeight + 190), // Hand
                    strokeWidth = armWidth,
                    cap = StrokeCap.Round
                )
                // Sleeve
                drawLine(
                    color = shirtColor,
                    start = Offset(cx, hipY - torsoHeight + 30),
                    end = Offset(cx + 5, hipY - torsoHeight + 90),
                    strokeWidth = armWidth,
                    cap = StrokeCap.Round
                )
            }

            // 4. HIP JOINT
            drawCircle(
                color = pantsColor,
                radius = legWidth / 1.8f,
                center = Offset(cx, hipY)
            )

            // 5. GROUND
            drawLine(
                color = Color.Gray,
                start = Offset(cx - 140, groundY + 20),
                end = Offset(cx + 140, groundY + 20),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }
    }
}