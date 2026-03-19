package com.example.smartalignoraapplication.ui.components


import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

// ─────────────────────────────────────────────────────────────────────────────
// PostureAnimationScreen
//
// ✅ Fix 1: fallLabel "safe"/"fall" — model output प्रमाणे
// ✅ Fix 2: prob=0.0 असेल तर label वरून display करतो
// ✅ Fix 3: NORMAL/FALL badge always दिसतो
// ✅ Fix 4: Body animation pitch वर rotate होतो
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PostureAnimationScreen(
    currentPitch:    Float,
    fallLabel:       String = "safe",  // "safe" किंवा "fall"
    fallProbability: Float  = 0f
) {
    // Smooth pitch animation
    val animatedPitch by animateFloatAsState(
        targetValue   = currentPitch,
        animationSpec = tween(durationMillis = 400),
        label         = "PostureAnim"
    )

    val isGoodPosture = animatedPitch.absoluteValue < 10
    val postureColor  = if (isGoodPosture) Color(0xFF16A34A) else Color(0xFFDC2626)

    // Body colors
    val skinColor  = Color(0xFFFFCC80)
    val shirtColor = Color(0xFF1976D2)
    val pantsColor = Color(0xFF424242)

    // ── Fall badge colors ──────────────────────────────────────────────────────
    val isFall    = fallLabel == "fall"
    val fallColor = if (isFall) Color(0xFFDC2626) else Color(0xFF16A34A)
    val fallBg    = if (isFall) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)

    // ── Probability display ────────────────────────────────────────────────────
    // ✅ Fix: prob=0.0 असेल तर label वरून "HIGH"/"LOW" दाखवतो
    // prob > 0 असेल तर % दाखवतो
    val displayProb = when {
        isFall -> {
            // Fall label आहे
            when {
                fallProbability > 0.01f ->
                    "${"%.0f".format(fallProbability * 100f)}%"
                else -> "HIGH"   // prob=0 but label=fall
            }
        }
        else -> {
            // Safe label आहे
            val safeProb = 1f - fallProbability
            when {
                safeProb > 0.01f ->
                    "${"%.0f".format(safeProb * 100f)}%"
                else -> "LOW"    // prob=0 but label=safe
            }
        }
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Title ─────────────────────────────────────────────────────────────
        Text(
            text       = "Posture Analysis",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        // ── Pitch angle ───────────────────────────────────────────────────────
        Text(
            text       = "${"%.1f".format(animatedPitch)}°",
            fontSize   = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = postureColor
        )

        // ── Posture label ─────────────────────────────────────────────────────
        Text(
            text = when {
                animatedPitch > 15  -> "Leaning Forward"
                animatedPitch < -15 -> "Leaning Back"
                else                -> "Perfect Alignment"
            },
            fontSize = 16.sp,
            color    = Color.Gray
        )

        Spacer(Modifier.height(12.dp))

        // ── Fall Detection Badge ───────────────────────────────────────────────
        // ✅ Always दिसतो — toggle वर अवलंबून नाही
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(fallBg)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = if (isFall) Icons.Default.PersonOff
                else Icons.Default.Person,
                contentDescription = null,
                tint               = fallColor,
                modifier           = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                // ✅ "fall" → "FALL DETECTED  85%"
                // ✅ "safe" → "NORMAL  95%"
                text       = if (isFall)
                    "FALL DETECTED  $displayProb"
                else
                    "NORMAL  $displayProb",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = fallColor
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Canvas Body Animation ─────────────────────────────────────────────
      Canvas(
            modifier = Modifier.size(360.dp, 440.dp)
        ) {
            val cx      = size.width / 2
            val groundY = size.height - 40f

            val legHeight   = 200f
            val hipY        = groundY - legHeight
            val torsoHeight = 190f
            val torsoWidth  = 75f
            val headRadius  = 46f
            val legWidth    = 62f
            val neckLen     = 28f
            val armWidth    = 28f

            // ── Reference vertical line ───────────────────────────────────────
            drawLine(
                color       = Color.LightGray,
                start       = Offset(cx, groundY),
                end         = Offset(cx, groundY - 460f),
                strokeWidth = 3f,
                pathEffect  = PathEffect.dashPathEffect(floatArrayOf(18f, 9f), 0f)
            )

            // ── Legs ──────────────────────────────────────────────────────────
            drawLine(
                color       = pantsColor,
                start       = Offset(cx, hipY),
                end         = Offset(cx + 14, groundY),
                strokeWidth = legWidth,
                cap         = StrokeCap.Round
            )

            // ── Upper body — pitch वर rotate होतो ────────────────────────────
            withTransform({ rotate(animatedPitch, Offset(cx, hipY)) }) {

                // Torso
                drawLine(
                    color       = shirtColor,
                    start       = Offset(cx, hipY),
                    end         = Offset(cx, hipY - torsoHeight),
                    strokeWidth = torsoWidth
                )

                // Shoulder cap
                drawCircle(
                    color  = shirtColor,
                    radius = torsoWidth / 2,
                    center = Offset(cx, hipY - torsoHeight)
                )

                // Neck
                drawLine(
                    color       = skinColor,
                    start       = Offset(cx, hipY - torsoHeight),
                    end         = Offset(cx, hipY - torsoHeight - neckLen),
                    strokeWidth = 33f
                )

                // Head
                drawCircle(
                    color  = skinColor,
                    radius = headRadius,
                    center = Offset(cx + 7, hipY - torsoHeight - neckLen - headRadius * 0.8f)
                )

                // Right arm
                drawLine(
                    color       = skinColor,
                    start       = Offset(cx, hipY - torsoHeight + 28),
                    end         = Offset(cx + 14, hipY - torsoHeight + 180),
                    strokeWidth = armWidth,
                    cap         = StrokeCap.Round
                )

                // Left arm
                drawLine(
                    color       = skinColor,
                    start       = Offset(cx, hipY - torsoHeight + 28),
                    end         = Offset(cx - 14, hipY - torsoHeight + 180),
                    strokeWidth = armWidth,
                    cap         = StrokeCap.Round
                )
            }

            // ── Hip joint ─────────────────────────────────────────────────────
            drawCircle(
                color  = pantsColor,
                radius = legWidth / 1.8f,
                center = Offset(cx, hipY)
            )

            // ── Posture arc indicator ─────────────────────────────────────────
            // Green = good posture, Red = bad posture
            drawArc(
                color      = if (isGoodPosture) Color(0x3000C9A7)
                else Color(0x30E53935),
                startAngle = -90f,
                sweepAngle = animatedPitch.coerceIn(-60f, 60f),
                useCenter  = true,
                topLeft    = Offset(cx - 28f, hipY - 28f),
                size       = Size(56f, 56f)
            )

            // ── Ground line ───────────────────────────────────────────────────
            drawLine(
                color       = Color.Gray,
                start       = Offset(cx - 130, groundY + 18),
                end         = Offset(cx + 130, groundY + 18),
                strokeWidth = 8f,
                cap         = StrokeCap.Round
            )
        }
    }
}