package com.example.smartalignoraapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartalignoraapplication.controller.BleViewModel
import com.example.smartalignoraapplication.ui.theme.AppColors
import com.example.smartalignoraapplication.ui.theme.AppDimens

@Composable
fun DeviceSetupScreen(
    viewModel: BleViewModel? = null,
    onNext: () -> Unit = {},
    onSkip: () -> Unit = {},
    onRequestBtPermission: () -> Unit = {},
    onEnableBluetooth: () -> Unit = {}
) {
    val isConnected   = viewModel?.isConnected?.value ?: false
    val isCalibrating = viewModel?.isCalibrating?.value ?: false
    val scrollState   = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            StepIndicator(currentStep = 1, totalSteps = 3)
            Spacer(Modifier.height(32.dp))

            // Device illustration
            DeviceIllustration()
            Spacer(Modifier.height(32.dp))

            // Title
            Column(
                modifier              = Modifier.padding(horizontal = 32.dp),
                horizontalAlignment   = Alignment.CenterHorizontally
            ) {
                Text(
                    "Locate your\nSmartAlignOra\ndevice.",
                    color      = AppColors.PurplePrimary,
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Center,
                    lineHeight = 34.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "The device clips easily onto your collar or shirt.",
                    color     = AppColors.TextSecondary,
                    fontSize  = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(20.dp))

                // ── BT Toggle button ──────────────────────────────────────────
                Button(
                    onClick = {
                        if (isConnected) viewModel?.disconnect()
                        else onRequestBtPermission()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(AppDimens.radiusXl),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) AppColors.BadRed
                        else AppColors.PurplePrimary
                    )
                ) {
                    Icon(
                        if (isConnected) Icons.Default.BluetoothDisabled else Icons.Default.Bluetooth,
                        null, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isConnected) "Disconnect" else "Connect via Bluetooth",
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Calibrate button ──────────────────────────────────────────
                OutlinedButton(
                    onClick  = { viewModel?.calibrate() },
                    enabled  = isConnected && !isCalibrating,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(AppDimens.radiusXl),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.PurplePrimary
                    )
                ) {
                    if (isCalibrating) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(16.dp),
                            color       = AppColors.PurplePrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Tune, null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isCalibrating) "Calibrating… stand still"
                        else "Calibrate Device",
                        fontSize = 15.sp, fontWeight = FontWeight.Medium
                    )
                }

                // Calibration instruction
                if (isCalibrating) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDimens.radiusLg))
                            .background(AppColors.PurpleSurface)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessibilityNew,
                            null,
                            tint     = AppColors.PurplePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Stand straight for calibration",
                                fontWeight = FontWeight.SemiBold,
                                color      = AppColors.PurplePrimary,
                                fontSize   = 14.sp
                            )
                            Text(
                                "Do not move for 3 seconds",
                                color    = AppColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Having trouble? ", color = AppColors.TextSecondary, fontSize = 13.sp)
                    Text("Watch video", color = AppColors.PurplePrimary,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        // ── Sticky bottom ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(AppColors.White)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick  = onNext,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(AppDimens.radiusXl),
                colors   = ButtonDefaults.buttonColors(containerColor = AppColors.PurplePrimary)
            ) {
                Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSkip) {
                Text("Skip Setup", color = AppColors.TextSecondary, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "STEP $currentStep OF $totalSteps",
            color         = AppColors.TextSecondary,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (index == currentStep - 1) 28.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentStep - 1) AppColors.PurplePrimary
                            else AppColors.BorderColor
                        )
                )
            }
        }
    }
}

@Composable
private fun DeviceIllustration() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(180.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(AppColors.PurplePrimary, AppColors.PurpleLight)
                )
            )
    ) {
        Box(
            modifier = Modifier.size(60.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
        )
        Box(
            modifier = Modifier.size(92.dp).clip(CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DeviceSetupPreview() { DeviceSetupScreen() }