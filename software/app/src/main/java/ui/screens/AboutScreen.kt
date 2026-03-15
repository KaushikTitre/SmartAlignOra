package com.example.smartalignora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartalignora.R

// ─── Colors ───────────────────────────────────────────────────────────────────
private val Purple700        = Color(0xFF6B21A8)
private val Purple500        = Color(0xFF9333EA)
private val PurpleLight      = Color(0xFFF3E8FF)
private val PurpleWarning    = Color(0xFFFAF0FF)
private val WarningBorder    = Color(0xFFD8B4FE)
private val TextDark         = Color(0xFF1A1A2E)
private val TextMedium       = Color(0xFF4A4A6A)
private val TextLight        = Color(0xFF7A7A9A)
private val BackgroundWhite  = Color(0xFFFFFFFF)
private val StatBg           = Color(0xFF5B21B6)

// ─── Entry Point ──────────────────────────────────────────────────────────────
// onBack  → called when the ← arrow is tapped (goes to previous screen)
// onGetStarted → called when "Get Started" button is tapped (goes to next screen)
@Composable
fun AboutScreen(
    onBack: () -> Unit = {},
    onGetStarted: () -> Unit = {}
) {
    Scaffold(
        topBar = { AboutTopBar(onBack = onBack) },
        bottomBar = { GetStartedButton(onGetStarted = onGetStarted) },
        containerColor = BackgroundWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Hero image with gradient overlay ──────────────────────────
            HeroSection()

            // ── Body content ──────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // About section
                SectionTitle(text = "About SmartAlignora")
                Spacer(modifier = Modifier.height(8.dp))
                BodyText(
                    text = "SmartAlignora uses advanced AI vision and sensor fusion to monitor your " +
                            "posture in real-time. Whether you're at your desk or on the move, we help " +
                            "you maintain a healthy spine and avoid long-term pain through intelligent " +
                            "alerts and habit building & tracking."
                )

                Spacer(modifier = Modifier.height(24.dp))

                // The Problem section
                SectionTitle(text = "The Problem")
                Spacer(modifier = Modifier.height(8.dp))
                BodyText(
                    text = "Modern life keeps us hunched over screens for hours. This \"tech neck\" " +
                            "leads to chronic tension, reduced lung capacity, and permanent spinal changes."
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stat card
                StatCard()

                Spacer(modifier = Modifier.height(24.dp))

                // What It Does section
                SectionTitle(text = "What It Does")
                Spacer(modifier = Modifier.height(12.dp))
                FeatureItem(
                    emoji = "👁️",
                    title = "Real-time Detection",
                    description = "AI-powered tracking detects even the slightest slouching or uneven shoulders."
                )
                Spacer(modifier = Modifier.height(12.dp))
                FeatureItem(
                    emoji = "🔔",
                    title = "Instant Alerts",
                    description = "Receive gentle haptic feedback or visual cues the moment your posture slips."
                )
                Spacer(modifier = Modifier.height(12.dp))
                FeatureItem(
                    emoji = "📈",
                    title = "Progress Tracking",
                    description = "Detailed weekly analytics to see how your posture improves over time."
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Benefits section
                SectionTitle(text = "Benefits")
                Spacer(modifier = Modifier.height(12.dp))
                BenefitItem(text = "Greater body awareness")
                BenefitItem(text = "Automatic habit building")
                BenefitItem(text = "Reduced muscle fatigue")
                BenefitItem(text = "Confidence & better breathing")

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ─── Top App Bar ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "SmartAligora",
                color = Purple700,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go back",
                    tint = Purple700
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BackgroundWhite
        )
    )
}

// ─── Hero Section ─────────────────────────────────────────────────────────────
// Full-width image with a gradient text overlay at the bottom
@Composable
fun HeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        // Background gradient placeholder — replace with your hero image:
        // Image(painter = painterResource(R.drawable.hero_posture), ...)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF4C1D95), Color(0xFF7C3AED), Color(0xFFDDD6FE))
                    )
                )
        )

        // Dark gradient overlay so text is readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000)),
                        startY = 80f
                    )
                )
        )

        // Hero text at the bottom of the image
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 20.dp, end = 20.dp)
        ) {
            Text(
                text = "Your smart posture\ncompanion",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Elevate your spine health with AI",
                color = Color(0xFFE9D5FF),
                fontSize = 13.sp
            )
        }
    }
}

// ─── Stat Card ────────────────────────────────────────────────────────────────
// Purple highlighted card showing the global back pain statistic
@Composable
fun StatCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StatBg)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚠️", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "WHY IT MATTERS",
                    color = Color(0xFFDDD6FE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "619M+",
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "people suffer from low back pain\nglobally.",
                color = Color(0xFFEDE9FE),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Source: World Health Organization (2023)",
                color = Color(0xFFBB9FD4),
                fontSize = 11.sp
            )
        }
    }
}

// ─── Feature Item ─────────────────────────────────────────────────────────────
// Row with emoji icon, bold title, and description
@Composable
fun FeatureItem(emoji: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Icon circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(PurpleLight)
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = title,
                color = TextDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                color = TextMedium,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

// ─── Benefit Item ─────────────────────────────────────────────────────────────
// Checkmark row for benefits list
@Composable
fun BenefitItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Purple700)
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
            color = TextDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Section Title ────────────────────────────────────────────────────────────
@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Purple700,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

// ─── Body Text ────────────────────────────────────────────────────────────────
@Composable
fun BodyText(text: String) {
    Text(
        text = text,
        color = TextMedium,
        fontSize = 14.sp,
        lineHeight = 22.sp
    )
}

// ─── Get Started Button ───────────────────────────────────────────────────────
// Sticky bottom button that navigates to the next screen
@Composable
fun GetStartedButton(onGetStarted: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundWhite)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple700)
        ) {
            Text(
                text = "Get Started",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AboutScreenPreview() {
    AboutScreen()
}