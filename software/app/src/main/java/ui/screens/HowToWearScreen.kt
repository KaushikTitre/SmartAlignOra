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
import androidx.compose.material.icons.filled.Info
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
private val WearPurple       = Color(0xFF6B21A8)
private val WearPurpleLight  = Color(0xFFF3E8FF)
private val WearPurpleMid    = Color(0xFF9333EA)
private val WearBg           = Color(0xFFFFFFFF)
private val WearTextDark     = Color(0xFF1A1A2E)
private val WearTextGray     = Color(0xFF6B7280)
private val WearTipBg        = Color(0xFFFAF5FF)
private val WearTipBorder    = Color(0xFFE9D5FF)
private val WearImageBg      = Color(0xFFF5F3FF)

// ===========================================================================
//  SCREEN — HowToWearScreen
//  Page 4: "How to Wear — Step 2: Clip to your collar or neckline"
//  onNext → goes to Page 5 (Clip-on Guide)
//  onSkip → skips setup
// ===========================================================================
@Composable
fun HowToWearScreen(
    onNext: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WearBg)
    ) {
        // ── Scrollable body ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // Top label
            Text(
                text = "How to Wear",
                color = WearPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main title
            Column(
                modifier = Modifier.padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Step 2: Clip to your\ncollar or neckline.",
                    color = WearTextDark,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Person wearing device illustration
            WearIllustration()

            Spacer(modifier = Modifier.height(24.dp))

            // Description text
            Column(
                modifier = Modifier.padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ensure the device is centered between\nyour shoulder blades for the most\naccurate posture tracking.",
                    color = WearTextGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pro Tip card
            ProTipCard()

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Sticky bottom buttons ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(WearBg)
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
                colors = ButtonDefaults.buttonColors(containerColor = WearPurple)
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

            Spacer(modifier = Modifier.height(12.dp))

            // Skip Setup
            TextButton(onClick = onSkip) {
                Text(
                    text = "Skip Setup",
                    color = WearTextGray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ─── Wear Illustration ────────────────────────────────────────────────────────
// Shows a person silhouette with the device clipped to collar/neckline
// Replace with your actual image when available using:
// Image(painter = painterResource(R.drawable.how_to_wear), ...)
@Composable
fun WearIllustration() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(WearImageBg)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Person silhouette placeholder
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.size(160.dp)
            ) {
                // Body silhouette
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(130.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFDDD6FE), Color(0xFFEDE9FE))
                            )
                        )
                )

                // Head circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.TopCenter)
                        .clip(CircleShape)
                        .background(Color(0xFFDDD6FE))
                )

                // Device clip indicator on collar
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 52.dp)
                        .clip(CircleShape)
                        .background(WearPurple)
                        .border(2.dp, Color.White, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Label below illustration
            Text(
                text = "Clip here ↑",
                color = WearPurple,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─── Pro Tip Card ─────────────────────────────────────────────────────────────
// Light purple card with info icon and tip text
@Composable
fun ProTipCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .border(1.dp, WearTipBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WearTipBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Info icon in purple circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(WearPurpleLight)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = WearPurple,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Pro Tip",
                    color = WearPurple,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "The clip should feel secure and the device should be oriented vertically for the best posture readings.",
                    color = WearTextGray,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HowToWearScreenPreview() {
    HowToWearScreen()
}