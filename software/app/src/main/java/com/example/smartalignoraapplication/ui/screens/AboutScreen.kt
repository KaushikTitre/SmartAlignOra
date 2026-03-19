package com.example.smartalignoraapplication.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartalignoraapplication.ui.theme.AppColors
import com.example.smartalignoraapplication.ui.theme.AppDimens
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(
    onBack: () -> Unit = {},
    onGetStarted: () -> Unit = {}
) {
    val scrollState   = rememberScrollState()
    val scope         = rememberCoroutineScope()
    var buttonPulse   by remember { mutableStateOf(false) }

    val buttonScale by animateFloatAsState(
        targetValue   = if (buttonPulse) 1.04f else 1.0f,
        animationSpec = tween(200),
        label         = "btn_scale",
        finishedListener = { buttonPulse = false }
    )

    Scaffold(
        topBar = {
            AboutTopBar(
                onBack = onBack,
                onSkipReading = {
                    scope.launch {
                        scrollState.animateScrollTo(scrollState.maxValue)
                        buttonPulse = true
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.White)
                    .padding(horizontal = AppDimens.paddingLg, vertical = 16.dp)
            ) {
                Button(
                    onClick  = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape    = RoundedCornerShape(AppDimens.radiusXl),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = AppColors.PurplePrimary
                    )
                ) {
                    Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = AppColors.White
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(scrollState)
        ) {
            // ── Hero ─────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF4C1D95),
                                    Color(0xFF7C3AED),
                                    Color(0xFF3B82F6)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors  = listOf(Color.Transparent, Color(0xCC000000)),
                                startY  = 100f
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 24.dp, end = 20.dp)
                ) {
                    Text(
                        "Your smart posture\ncompanion",
                        color      = Color.White,
                        fontSize   = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 32.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Elevate your spine health with AI",
                        color    = Color(0xFFE9D5FF),
                        fontSize = 13.sp
                    )
                }
            }

            // ── Body ─────────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = AppDimens.paddingLg)) {
                Spacer(Modifier.height(24.dp))

                SectionTitle("About SmartAlignOra")
                Spacer(Modifier.height(8.dp))
                BodyText(
                    "SmartAlignOra uses advanced AI and sensor fusion to monitor your posture " +
                            "in real-time. Whether you're at your desk or on the move, we help you " +
                            "maintain a healthy spine through intelligent alerts and habit tracking."
                )

                Spacer(Modifier.height(24.dp))
                SectionTitle("The Problem")
                Spacer(Modifier.height(8.dp))
                BodyText(
                    "Modern life keeps us hunched over screens for hours. This \"tech neck\" " +
                            "leads to chronic tension, reduced lung capacity, and permanent spinal changes."
                )

                Spacer(Modifier.height(16.dp))
                StatCard()

                Spacer(Modifier.height(24.dp))
                SectionTitle("What It Does")
                Spacer(Modifier.height(12.dp))
                AboutFeatureItem("👁️", "Real-time Detection",
                    "AI-powered tracking detects even the slightest slouching or uneven shoulders.")
                Spacer(Modifier.height(12.dp))
                AboutFeatureItem("🔔", "Instant Alerts",
                    "Gentle haptic feedback the moment your posture slips.")
                Spacer(Modifier.height(12.dp))
                AboutFeatureItem("📈", "Progress Tracking",
                    "Weekly analytics to see how your posture improves over time.")
                Spacer(Modifier.height(12.dp))
                AboutFeatureItem("🚨", "Fall Detection",
                    "AI detects falls and alerts your emergency contact automatically.")

                Spacer(Modifier.height(24.dp))
                SectionTitle("Benefits")
                Spacer(Modifier.height(12.dp))
                listOf(
                    "Greater body awareness",
                    "Automatic habit building",
                    "Reduced muscle fatigue",
                    "Confidence & better breathing",
                    "Fall safety protection"
                ).forEach { BenefitItem(it) }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutTopBar(onBack: () -> Unit, onSkipReading: () -> Unit) {
    TopAppBar(
        title = {
            Text("SmartAlignOra", color = AppColors.PurplePrimary,
                fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = AppColors.PurplePrimary)
            }
        },
        actions = {
            TextButton(onClick = onSkipReading) {
                Icon(Icons.Default.KeyboardArrowDown, null,
                    tint = AppColors.PurplePrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                Text("Skip reading", color = AppColors.PurplePrimary, fontSize = 13.sp,
                    fontWeight = FontWeight.Medium)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.White)
    )
}

@Composable
private fun StatCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(AppDimens.radiusXl),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF5B21B6))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚠️", fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text("WHY IT MATTERS", color = Color(0xFFDDD6FE),
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text("619M+", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
            Text("people suffer from low back pain globally.",
                color = Color(0xFFEDE9FE), fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text("Source: World Health Organization (2023)",
                color = Color(0xFFBB9FD4), fontSize = 11.sp)
        }
    }
}

@Composable
private fun AboutFeatureItem(emoji: String, title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).clip(CircleShape)
                .background(AppColors.PurpleSurface)
        ) { Text(emoji, fontSize = 18.sp) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = AppColors.TextPrimary, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(description, color = AppColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun BenefitItem(text: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(24.dp).clip(CircleShape)
                .background(AppColors.PurplePrimary)
        ) {
            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = AppColors.PurplePrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
}

@Composable
private fun BodyText(text: String) {
    Text(text, color = AppColors.TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AboutPreview() { AboutScreen() }