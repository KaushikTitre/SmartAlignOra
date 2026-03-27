package com.example.smartalignoraapplication.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// SplashScreen
//
// ✅ Fix: Pure Compose splash — Android logo दिसत नाही
// ✅ SplashTheme मध्ये purple background आधीच set केलाय
// ✅ 2 seconds नंतर onNavigateToAbout() call होतो
// ─────────────────────────────────────────────────────────────────────────────

private val SplashPurple     = Color(0xFF6B21A8)
private val SplashPurpleMid  = Color(0xFF9333EA)
private val SplashPurpleLight = Color(0xFFDDD6FE)
private val TaglinePurple    = Color(0xFFE9D5FF)
private val LoadingGray      = Color(0xFFE9D5FF)

@Composable
fun SplashScreen(onNavigateToAbout: () -> Unit = {}) {

    // ✅ Auto navigate after 2 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onNavigateToAbout()
    }

    // Breathing animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "splash_breathe")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 0.92f,
        targetValue   = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splash_scale"
    )

    // Loading pulse animation
    val alpha by infiniteTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splash_alpha"
    )

    // ── Full screen purple gradient background ────────────────────────────────
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SplashPurple, SplashPurpleMid, Color(0xFF4C1D95))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier            = Modifier.padding(32.dp)
        ) {

            // ── Logo placeholder ───────────────────────────────────────────────
            // जेव्हा तुमचा logo असेल तेव्हा हे replace करा:
            // Image(painter = painterResource(R.drawable.ic_app_logo), ...)
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Text(
                    text      = "🦴",
                    fontSize  = 56.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── App name ───────────────────────────────────────────────────────
            Text(
                text          = "SmartAlignOra",
                color         = Color.White,
                fontSize      = 32.sp,
                fontWeight    = FontWeight.ExtraBold,
                fontStyle     = FontStyle.Italic,
                textAlign     = TextAlign.Center,
                letterSpacing = 0.5.sp
            )

            Spacer(Modifier.height(8.dp))

            // ── Tagline ────────────────────────────────────────────────────────
            Text(
                text          = "ALIGN RIGHT. LIVE BRIGHT.",
                color         = TaglinePurple,
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 2.sp,
                textAlign     = TextAlign.Center
            )

            Spacer(Modifier.height(64.dp))

            // ── Loading text ───────────────────────────────────────────────────
            Text(
                text      = "Loading...",
                color     = LoadingGray.copy(alpha = alpha),
                fontSize  = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SplashPreview() {
    SplashScreen()
}