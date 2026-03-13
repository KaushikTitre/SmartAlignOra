package com.example.smartalignoraapplication.jetpackcompose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartalignoraapplication.controller.BleViewModel
import kotlin.math.absoluteValue

// PostureAnimationScreen.kt
// This screen shows posture animation + slider to control vibration motor
// Slider sends value to ESP32 using BLE via BleViewModel

@Composable
fun PostureAnimationScreen(currentPitch: Float) {

    // Get ViewModel (used to send data to ESP32)
    val viewModel: BleViewModel = viewModel()

    // Animate pitch value smoothly
    val animatedPitch by animateFloatAsState(
        targetValue = currentPitch,
        animationSpec = tween(durationMillis = 400),
        label = "PostureAnim"
    )

    // Check posture condition
    val isGoodPosture = animatedPitch.absoluteValue < 10

    // Color based on posture
    val statusColor =
        if (isGoodPosture) Color(0xFF43A047)
        else Color(0xFFE53935)

    // Colors for body drawing
    val skinColor = Color(0xFFFFCC80)
    val shirtColor = Color(0xFF1976D2)
    val pantsColor = Color(0xFF424242)


    // Slider state (0–100 vibration level)
    var vibrationLevel by remember {
        mutableFloatStateOf(50f)
    }
    // Main layout column
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Title
        Text(
            "Posture Analysis",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Show pitch angle
        Text(
            text = "${"%.1f".format(animatedPitch)}°",
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = statusColor
        )

        // Posture text
        Text(
            text =
                if (animatedPitch > 15) "Leaning Forward"
                else if (animatedPitch < -15) "Leaning Back"
                else "Perfect Alignment",

            fontSize = 18.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))


        // =========================
        // SLIDER FOR VIBRATION LEVEL
        // =========================

        Text("Vibration Level: ${vibrationLevel.toInt()}")

        Slider(

            // current value
            value = vibrationLevel,

            // when sliding
            onValueChange = {
                vibrationLevel = it
            },

            // when user stops sliding → send to ESP32
            onValueChangeFinished = {

                // send vibration level to ESP32
                val level = when {

                    vibrationLevel < 20 -> "V0"
                    vibrationLevel < 40 -> "V25"
                    vibrationLevel < 60 -> "V50"
                    vibrationLevel < 80 -> "V75"
                    else -> "V100"
                }

                viewModel.send(level)
            },

            // range 0 to 100
            valueRange = 0f..100f
        )


        Spacer(modifier = Modifier.height(20.dp))


        // =========================
        // CANVAS ANIMATION
        // =========================

        Canvas(
            modifier = Modifier.size(360.dp, 550.dp)
        ) {

            val cx = size.width / 2
            val groundY = size.height - 50f

            // body sizes
            val legHeight = 250f
            val hipY = groundY - legHeight

            val torsoHeight = 220f
            val torsoWidth = 75f

            val headRadius = 50f
            val legWidth = 65f
            val neckLen = 30f
            val armWidth = 30f


            // Reference line
            drawLine(
                color = Color.LightGray,

                start = Offset(cx, groundY),

                end = Offset(cx, groundY - 550),

                strokeWidth = 4f,

                pathEffect =
                    PathEffect.dashPathEffect(
                        floatArrayOf(20f, 10f),
                        0f
                    )
            )


            // Legs
            drawLine(
                color = pantsColor,

                start = Offset(cx, hipY),

                end = Offset(cx + 15, groundY),

                strokeWidth = legWidth,

                cap = StrokeCap.Round
            )


            // Rotate upper body based on pitch
            withTransform({

                rotate(
                    animatedPitch,
                    Offset(cx, hipY)
                )

            }) {

                // Torso
                drawLine(
                    color = shirtColor,

                    start = Offset(cx, hipY),

                    end = Offset(
                        cx,
                        hipY - torsoHeight
                    ),

                    strokeWidth = torsoWidth
                )


                // Shoulder
                drawCircle(
                    color = shirtColor,

                    radius =
                        torsoWidth / 2,

                    center =
                        Offset(
                            cx,
                            hipY - torsoHeight
                        )
                )


                // Neck
                drawLine(
                    color = skinColor,

                    start =
                        Offset(
                            cx,
                            hipY - torsoHeight
                        ),

                    end =
                        Offset(
                            cx,
                            hipY -
                                    torsoHeight -
                                    neckLen
                        ),

                    strokeWidth = 35f
                )


                // Head
                drawCircle(
                    color = skinColor,

                    radius = headRadius,

                    center =
                        Offset(
                            cx + 8,
                            hipY -
                                    torsoHeight -
                                    neckLen -
                                    (headRadius * 0.8f)
                        )
                )


                // Arm
                drawLine(
                    color = skinColor,

                    start =
                        Offset(
                            cx,
                            hipY -
                                    torsoHeight +
                                    30
                        ),

                    end =
                        Offset(
                            cx + 15,
                            hipY -
                                    torsoHeight +
                                    190
                        ),

                    strokeWidth = armWidth,

                    cap = StrokeCap.Round
                )
            }


            // Hip joint
            drawCircle(
                color = pantsColor,

                radius =
                    legWidth / 1.8f,

                center =
                    Offset(cx, hipY)
            )


            // Ground
            drawLine(
                color = Color.Gray,

                start =
                    Offset(
                        cx - 140,
                        groundY + 20
                    ),

                end =
                    Offset(
                        cx + 140,
                        groundY + 20
                    ),

                strokeWidth = 8f,

                cap = StrokeCap.Round
            )
        }
    }
}