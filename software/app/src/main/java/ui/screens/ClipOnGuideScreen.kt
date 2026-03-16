package com.example.smartalignora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
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
private val GuidePurple      = Color(0xFF6B21A8)
private val GuidePurpleLight = Color(0xFFF3E8FF)
private val GuidePurpleMid   = Color(0xFF9333EA)
private val GuideBg          = Color(0xFFFFFFFF)
private val GuideTextDark    = Color(0xFF1A1A2E)
private val GuideTextGray    = Color(0xFF6B7280)
private val GuideCardBg      = Color(0xFFFAF5FF)
private val GuideCardBorder  = Color(0xFFE9D5FF)
private val GuideFreeBg      = Color(0xFFECFDF5)
private val GuideFreeText    = Color(0xFF059669)

// ===========================================================================
//  SCREEN — ClipOnGuideScreen
//  Page 5: "Clip-on Guide — Step 3 of 3"
//  onFinish → finishes setup and goes to main app
// ===========================================================================
@Composable
fun ClipOnGuideScreen(
    onFinish: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GuideBg)
    ) {
        // ── Scrollable body ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // ── Top bar: centered title, no Free badge ────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Clip-on Guide",
                    color = GuidePurple,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            // Step indicator — Step 3 of 3
            GuideStepIndicator(currentStep = 3, totalSteps = 3)

            Spacer(modifier = Modifier.height(28.dp))

            // Main title
            Column(
                modifier = Modifier.padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ensure it sits flat\nagainst your skin.",
                    color = GuideTextDark,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Correct placement ensures the most\naccurate posture alerts and real-time\ntracking.",
                    color = GuideTextGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Body illustration with device placement
            BodyPlacementIllustration()

            Spacer(modifier = Modifier.height(28.dp))

            // Placement checklist
            PlacementChecklist()

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Sticky Finish & Calibrate button ─────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(GuideBg)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GuidePurple)
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Finish & Calibrate",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ─── Step Indicator ───────────────────────────────────────────────────────────
@Composable
fun GuideStepIndicator(currentStep: Int, totalSteps: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "STEP $currentStep OF $totalSteps",
            color = GuideTextGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (index == currentStep - 1) 24.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < currentStep) GuidePurple
                            else Color(0xFFE5E7EB)
                        )
                )
            }
        }
    }
}

// ─── Body Placement Illustration ─────────────────────────────────────────────
// Shows a full body silhouette with device placement indicator
// Replace with your actual image:
// Image(painter = painterResource(R.drawable.body_placement), ...)
@Composable
fun BodyPlacementIllustration() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .height(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF5F3FF))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .width(120.dp)
                    .height(220.dp)
            ) {
                // Full body silhouette
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(160.dp)
                        .align(Alignment.BottomCenter)
                        .clip(
                            RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = 8.dp,
                                bottomEnd = 8.dp
                            )
                        )
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFDDD6FE),
                                    Color(0xFFEDE9FE)
                                )
                            )
                        )
                )

                // Head
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.TopCenter)
                        .clip(CircleShape)
                        .background(Color(0xFFDDD6FE))
                )

                // Shoulder area highlight
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(30.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GuidePurple.copy(alpha = 0.15f))
                        .border(1.dp, GuidePurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                )

                // Device dot on chest
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 56.dp)
                        .clip(CircleShape)
                        .background(GuidePurple)
                        .border(2.5.dp, Color.White, CircleShape)
                )

                // Pulse ring around device
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 49.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, GuidePurple.copy(alpha = 0.35f), CircleShape)
                )
            }
        }

        // Placement label
        Text(
            text = "Optimal placement zone",
            color = GuidePurple,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

// ─── Placement Checklist ──────────────────────────────────────────────────────
// Green checkmark list of correct placement tips
@Composable
fun PlacementChecklist() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .border(1.dp, GuideCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GuideCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Placement Checklist",
                color = GuidePurple,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            ChecklistItem(text = "Device sits flat against skin")
            ChecklistItem(text = "Clip is secure and not loose")
            ChecklistItem(text = "Centered on collar or neckline")
            ChecklistItem(text = "No clothing bunching underneath")
        }
    }
}

// ─── Checklist Item ───────────────────────────────────────────────────────────
@Composable
fun ChecklistItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(GuidePurple)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = GuideTextDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ClipOnGuideScreenPreview() {
    ClipOnGuideScreen()
}
