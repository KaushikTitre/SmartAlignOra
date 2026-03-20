package com.example.smartalignora.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

// ─── Colors ───────────────────────────────────────────────────────────────────
private val SkinColor    = Color(0xFFFFCC80)
private val SkinDark     = Color(0xFFFFAA60)
private val ShirtColor   = Color(0xFF1565C0)
private val ShirtDark    = Color(0xFF0D47A1)
private val PantsColor   = Color(0xFF37474F)
private val PantsDark    = Color(0xFF263238)
private val ShoesColor   = Color(0xFF212121)
private val GoodGreen    = Color(0xFF16A34A)
private val BadRed       = Color(0xFFDC2626)
private val WarningOrange = Color(0xFFF97316)

// ===========================================================================
//  3D POSTURE ANIMATION SCREEN
//  currentPitch  — forward/back lean angle (-60 to 60)
//  currentRoll   — left/right lean angle (-60 to 60)
//  fallLabel     — "safe" or "fall"
//  fallProbability — 0.0 to 1.0
// ===========================================================================
@Composable
fun PostureAnimationScreen(
    currentPitch:    Float = 0f,
    currentRoll:     Float = 0f,
    fallLabel:       String = "safe",
    fallProbability: Float  = 0f
) {
    // ── Smooth animations ─────────────────────────────────────────────────────
    val animPitch by animateFloatAsState(
        targetValue   = currentPitch,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 120f),
        label         = "pitch"
    )
    val animRoll by animateFloatAsState(
        targetValue   = currentRoll,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 120f),
        label         = "roll"
    )

    // Fall blink animation
    val isFall        = fallLabel == "fall"
    val isGoodPosture = animPitch.absoluteValue < 15f && animRoll.absoluteValue < 15f
    val postureColor  = when {
        isFall         -> BadRed
        !isGoodPosture -> WarningOrange
        else           -> GoodGreen
    }

    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue  = if (isFall) 0.3f else 1f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Status badges row ─────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Posture badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(postureColor.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text       = when {
                        isFall         -> "⚠️ FALL DETECTED"
                        !isGoodPosture -> "⚡ POOR POSTURE"
                        else           -> "✅ GOOD POSTURE"
                    },
                    color      = postureColor.copy(alpha = if (isFall) blinkAlpha else 1f),
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Probability badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(postureColor.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text       = if (isFall)
                        "Risk: ${"%.0f".format(fallProbability * 100)}%"
                    else
                        "Safe: ${"%.0f".format((1f - fallProbability) * 100)}%",
                    color      = postureColor,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Pitch and Roll info ───────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AngleIndicator(label = "Pitch", angle = animPitch, color = postureColor)
            AngleIndicator(label = "Roll",  angle = animRoll,  color = postureColor)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 3D Body Canvas ────────────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            draw3DBody(
                pitch    = animPitch,
                roll     = animRoll,
                isFall   = isFall,
                isGood   = isGoodPosture
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Posture description ───────────────────────────────────────────────
        Text(
            text = when {
                isFall              -> "Fall detected! Please check on the user immediately."
                animPitch > 25f     -> "Leaning too far forward — straighten up!"
                animPitch < -25f    -> "Leaning too far backward — correct posture!"
                animRoll > 25f      -> "Tilting right — align your spine!"
                animRoll < -25f     -> "Tilting left — align your spine!"
                animPitch > 10f     -> "Slight forward lean — almost there!"
                animPitch < -10f    -> "Slight backward lean — adjust posture!"
                else                -> "Perfect alignment — keep it up!"
            },
            color      = postureColor,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(postureColor.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ─── Angle Indicator ──────────────────────────────────────────────────────────
@Composable
fun AngleIndicator(label: String, angle: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 11.sp)
        Text(
            text       = "${"%.1f".format(angle)}°",
            color      = color,
            fontSize   = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// ─── 3D Body Drawing ──────────────────────────────────────────────────────────
private fun DrawScope.draw3DBody(
    pitch:  Float,
    roll:   Float,
    isFall: Boolean,
    isGood: Boolean
) {
    val cx       = size.width / 2f
    val cy       = size.height / 2f
    val scale    = size.width / 400f

    // Convert degrees to radians
    val pitchRad = Math.toRadians(pitch.toDouble()).toFloat()
    val rollRad  = Math.toRadians(roll.toDouble()).toFloat()

    // If fall — lay the figure horizontally
    val effectivePitch = if (isFall) 85f else pitch
    val effectiveRoll  = if (isFall) roll else roll
    val fallPitchRad   = Math.toRadians(effectivePitch.toDouble()).toFloat()
    val fallRollRad    = Math.toRadians(effectiveRoll.toDouble()).toFloat()

    // ── Ground shadow ─────────────────────────────────────────────────────────
    drawOval(
        color   = Color.Black.copy(alpha = 0.12f),
        topLeft = Offset(cx - 70f * scale, cy + 130f * scale),
        size    = Size(140f * scale, 30f * scale)
    )

    // ── Ground line ───────────────────────────────────────────────────────────
    drawLine(
        color       = Color.Gray.copy(alpha = 0.4f),
        start       = Offset(cx - 120f * scale, cy + 140f * scale),
        end         = Offset(cx + 120f * scale, cy + 140f * scale),
        strokeWidth = 3f * scale,
        cap         = StrokeCap.Round
    )

    // ── Reference spine line ──────────────────────────────────────────────────
    drawLine(
        color       = Color.LightGray.copy(alpha = 0.5f),
        start       = Offset(cx, cy + 130f * scale),
        end         = Offset(cx, cy - 180f * scale),
        strokeWidth = 2f * scale,
        pathEffect  = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
    )

    // ── Feet / shoes ──────────────────────────────────────────────────────────
    val footOffsetX = sin(fallRollRad) * 20f * scale
    val footY       = cy + 130f * scale

    // Left shoe
    drawOval(
        color   = ShoesColor,
        topLeft = Offset(cx - 35f * scale + footOffsetX, footY - 10f * scale),
        size    = Size(30f * scale, 16f * scale)
    )
    // Right shoe
    drawOval(
        color   = ShoesColor,
        topLeft = Offset(cx + 5f * scale + footOffsetX, footY - 10f * scale),
        size    = Size(30f * scale, 16f * scale)
    )

    // ── Legs (3D effect with two tones) ───────────────────────────────────────
    val hipY        = footY - 140f * scale * cos(fallPitchRad)
    val hipX        = cx + 140f * scale * sin(fallPitchRad) * 0.3f

    // Left leg dark
    drawLine(
        color       = PantsDark,
        start       = Offset(hipX - 10f * scale, hipY),
        end         = Offset(cx - 15f * scale + footOffsetX, footY),
        strokeWidth = 32f * scale,
        cap         = StrokeCap.Round
    )
    // Left leg light
    drawLine(
        color       = PantsColor,
        start       = Offset(hipX - 8f * scale, hipY),
        end         = Offset(cx - 13f * scale + footOffsetX, footY),
        strokeWidth = 20f * scale,
        cap         = StrokeCap.Round
    )

    // Right leg dark
    drawLine(
        color       = PantsDark,
        start       = Offset(hipX + 10f * scale, hipY),
        end         = Offset(cx + 15f * scale + footOffsetX, footY),
        strokeWidth = 32f * scale,
        cap         = StrokeCap.Round
    )
    // Right leg light
    drawLine(
        color       = PantsColor,
        start       = Offset(hipX + 8f * scale, hipY),
        end         = Offset(cx + 13f * scale + footOffsetX, footY),
        strokeWidth = 20f * scale,
        cap         = StrokeCap.Round
    )

    // ── Hip joint (3D sphere effect) ──────────────────────────────────────────
    drawCircle(color = PantsDark,  radius = 22f * scale, center = Offset(hipX, hipY))
    drawCircle(color = PantsColor, radius = 16f * scale, center = Offset(hipX - 4f * scale, hipY - 4f * scale))

    // ── Torso ─────────────────────────────────────────────────────────────────
    val torsoLen  = 130f * scale
    val shoulderX = hipX + torsoLen * sin(fallPitchRad) * cos(fallRollRad)
    val shoulderY = hipY - torsoLen * cos(fallPitchRad)

    // Torso shadow side
    drawLine(
        color       = ShirtDark,
        start       = Offset(hipX + 6f * scale, hipY),
        end         = Offset(shoulderX + 6f * scale, shoulderY),
        strokeWidth = 58f * scale,
        cap         = StrokeCap.Round
    )
    // Torso main
    drawLine(
        color       = ShirtColor,
        start       = Offset(hipX, hipY),
        end         = Offset(shoulderX, shoulderY),
        strokeWidth = 52f * scale,
        cap         = StrokeCap.Round
    )
    // Torso highlight
    drawLine(
        color       = ShirtColor.copy(alpha = 0.6f),
        start       = Offset(hipX - 8f * scale, hipY),
        end         = Offset(shoulderX - 8f * scale, shoulderY),
        strokeWidth = 20f * scale,
        cap         = StrokeCap.Round
    )

    // ── Arms ──────────────────────────────────────────────────────────────────
    val armLen    = 100f * scale
    val armAngle  = fallPitchRad + 0.3f

    // Left arm
    val leftArmEndX = shoulderX - armLen * cos(armAngle) * 0.8f
    val leftArmEndY = shoulderY + armLen * 0.85f
    drawLine(
        color       = SkinDark,
        start       = Offset(shoulderX - 20f * scale, shoulderY + 10f * scale),
        end         = Offset(leftArmEndX - 4f * scale, leftArmEndY),
        strokeWidth = 24f * scale,
        cap         = StrokeCap.Round
    )
    drawLine(
        color       = SkinColor,
        start       = Offset(shoulderX - 20f * scale, shoulderY + 10f * scale),
        end         = Offset(leftArmEndX, leftArmEndY),
        strokeWidth = 18f * scale,
        cap         = StrokeCap.Round
    )

    // Right arm
    val rightArmEndX = shoulderX + armLen * cos(armAngle) * 0.8f
    val rightArmEndY = shoulderY + armLen * 0.85f
    drawLine(
        color       = SkinDark,
        start       = Offset(shoulderX + 20f * scale, shoulderY + 10f * scale),
        end         = Offset(rightArmEndX + 4f * scale, rightArmEndY),
        strokeWidth = 24f * scale,
        cap         = StrokeCap.Round
    )
    drawLine(
        color       = SkinColor,
        start       = Offset(shoulderX + 20f * scale, shoulderY + 10f * scale),
        end         = Offset(rightArmEndX, rightArmEndY),
        strokeWidth = 18f * scale,
        cap         = StrokeCap.Round
    )

    // ── Neck ──────────────────────────────────────────────────────────────────
    val neckLen = 24f * scale
    val neckEndX = shoulderX + neckLen * sin(fallPitchRad)
    val neckEndY = shoulderY - neckLen * cos(fallPitchRad)
    drawLine(
        color       = SkinDark,
        start       = Offset(shoulderX + 3f * scale, shoulderY),
        end         = Offset(neckEndX + 3f * scale, neckEndY),
        strokeWidth = 26f * scale,
        cap         = StrokeCap.Round
    )
    drawLine(
        color       = SkinColor,
        start       = Offset(shoulderX, shoulderY),
        end         = Offset(neckEndX, neckEndY),
        strokeWidth = 22f * scale,
        cap         = StrokeCap.Round
    )

    // ── Head (3D sphere) ──────────────────────────────────────────────────────
    val headRadius = 38f * scale
    val headCX     = neckEndX + headRadius * 0.5f * sin(fallPitchRad)
    val headCY     = neckEndY - headRadius * 0.8f

    // Head shadow
    drawCircle(
        color  = SkinDark,
        radius = headRadius,
        center = Offset(headCX + 4f * scale, headCY + 4f * scale)
    )
    // Head main
    drawCircle(
        color  = SkinColor,
        radius = headRadius,
        center = Offset(headCX, headCY)
    )
    // Head highlight (3D effect)
    drawCircle(
        color  = Color.White.copy(alpha = 0.3f),
        radius = headRadius * 0.45f,
        center = Offset(headCX - headRadius * 0.25f, headCY - headRadius * 0.25f)
    )

    // ── Eyes ──────────────────────────────────────────────────────────────────
    drawCircle(
        color  = Color(0xFF333333),
        radius = 5f * scale,
        center = Offset(headCX - 12f * scale, headCY - 5f * scale)
    )
    drawCircle(
        color  = Color(0xFF333333),
        radius = 5f * scale,
        center = Offset(headCX + 12f * scale, headCY - 5f * scale)
    )

    // ── Posture angle arc ─────────────────────────────────────────────────────
    val arcColor = when {
        isFall  -> BadRed.copy(alpha = 0.3f)
        !isGood -> WarningOrange.copy(alpha = 0.3f)
        else    -> GoodGreen.copy(alpha = 0.3f)
    }
    drawArc(
        color      = arcColor,
        startAngle = -90f,
        sweepAngle = pitch.coerceIn(-70f, 70f),
        useCenter  = true,
        topLeft    = Offset(hipX - 30f * scale, hipY - 30f * scale),
        size       = Size(60f * scale, 60f * scale)
    )

    // ── Fall warning flash ────────────────────────────────────────────────────
    if (isFall) {
        drawRect(
            color   = BadRed.copy(alpha = 0.06f),
            topLeft = Offset.Zero,
            size    = size
        )
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────
@Preview(showBackground = true)
@Composable
private fun PostureGoodPreview() {
    PostureAnimationScreen(currentPitch = 0f, currentRoll = 0f, fallLabel = "safe")
}

@Preview(showBackground = true)
@Composable
private fun PostureLeanPreview() {
    PostureAnimationScreen(currentPitch = 30f, currentRoll = 10f, fallLabel = "safe", fallProbability = 0.3f)
}

@Preview(showBackground = true)
@Composable
private fun PostureFallPreview() {
    PostureAnimationScreen(currentPitch = 85f, currentRoll = 45f, fallLabel = "fall", fallProbability = 0.95f)
}