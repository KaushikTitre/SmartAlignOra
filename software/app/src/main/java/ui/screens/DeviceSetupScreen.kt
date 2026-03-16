package com.example.smartalignora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

// ─── Colors ───────────────────────────────────────────────────────────────────
private val SetupPurple      = Color(0xFF6B21A8)
private val SetupPurpleLight = Color(0xFFF3E8FF)
private val SetupPurpleMid   = Color(0xFF9333EA)
private val SetupBg          = Color(0xFFFFFFFF)
private val SetupTextDark    = Color(0xFF1A1A2E)
private val SetupTextGray    = Color(0xFF6B7280)
private val SetupLinkColor   = Color(0xFF7C3AED)

// ===========================================================================
//  SCREEN — DeviceSetupScreen
//  Page 3: "Locate your SmartAlignora device"
//  onNext     → goes to Page 4 (How to Wear)
//  onSkip     → skips setup
// ===========================================================================
@Composable
fun DeviceSetupScreen(
    onNext: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SetupBg)
    ) {
        // ── Scrollable body ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp),  // space for sticky button
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // Step indicator
            StepIndicator(currentStep = 1, totalSteps = 3)

            Spacer(modifier = Modifier.height(40.dp))

            // Device illustration placeholder
            DeviceIllustration()

            Spacer(modifier = Modifier.height(40.dp))

            // Title and description
            Column(
                modifier = Modifier.padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Locate your\nSmartAlignora\ndevice.",
                    color = SetupPurple,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "The device is designed to clip easily\nonto your clothing.",
                    color = SetupTextGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Watch video link
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Having trouble? ",
                        color = SetupTextGray,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Watch video",
                        color = SetupLinkColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // ── Sticky bottom section ─────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(SetupBg)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Next button
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SetupPurple)
            ) {
                Text(
                    text = "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Skip Setup — same style as Page 4
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Skip Setup",
                    color = SetupTextGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─── Step Indicator ───────────────────────────────────────────────────────────
// Shows "STEP 1 OF 3" with dot indicators
@Composable
fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "STEP $currentStep OF $totalSteps",
            color = SetupTextGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Dot row
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (index == currentStep - 1) 24.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentStep - 1) SetupPurple
                            else Color(0xFFE5E7EB)
                        )
                )
            }
        }
    }
}

// ─── Device Illustration ──────────────────────────────────────────────────────
// Purple rounded square with a white circle — represents the device
// Replace with your actual device image when available
@Composable
fun DeviceIllustration() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(SetupPurple, SetupPurpleMid)
                )
            )
    ) {
        // Inner white circle representing device sensor
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
        )

        // Outer ring
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        )
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DeviceSetupScreenPreview() {
    DeviceSetupScreen()
}
