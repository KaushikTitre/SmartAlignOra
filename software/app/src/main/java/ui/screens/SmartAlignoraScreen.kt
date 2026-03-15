package com.example.smartalignora.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartalignora.R

// ─── Colors ───────────────────────────────────────────────────────────────────
private val BrandPurple     = Color(0xFF6B21A8)
private val TaglinePurple   = Color(0xFFAB47BC)
private val LoadingGray     = Color(0xFF9E9E9E)
private val BackgroundWhite = Color(0xFFFFFFFF)

// ─── Root Composable ──────────────────────────────────────────────────────────
@Composable
fun SmartAlignoraApp() {
    MaterialTheme {
        SplashScreen()
    }
}

// ─── Splash Screen ────────────────────────────────────────────────────────────
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(0.15f))

            // App name + tagline
            AppLogoSection()

            Spacer(modifier = Modifier.weight(0.25f))

            // Spine mascot illustration
            MascotSection()

            Spacer(modifier = Modifier.height(16.dp))

            // Pulsing loading label
            LoadingSection()

            Spacer(modifier = Modifier.weight(0.05f))
        }
    }
}

// ─── Logo Section ─────────────────────────────────────────────────────────────
@Composable
fun AppLogoSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppTitle(text = "SmartAlignora")
        AppTagline(text = "ALIGN RIGHT. LIVE BRIGHT.")
    }
}

@Composable
fun AppTitle(text: String) {
    Text(
        text = text,
        color = BrandPurple,
        fontSize = 34.sp,
        fontWeight = FontWeight.ExtraBold,
        fontStyle = FontStyle.Italic,
        textAlign = TextAlign.Center,
        letterSpacing = 0.5.sp
    )
}

@Composable
fun AppTagline(text: String) {
    Text(
        text = text,
        color = TaglinePurple,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 2.sp,
        textAlign = TextAlign.Center
    )
}

// ─── Mascot Section ───────────────────────────────────────────────────────────
@Composable
fun MascotSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_breathe")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue  = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .scale(scale)
    ) {
        Image(
            painter = painterResource(id = R.drawable.spine_mascot_v2),
            contentDescription = "SmartAlignora spine mascot",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ─── Loading Section ──────────────────────────────────────────────────────────
@Composable
fun LoadingSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loading_alpha"
    )

    Text(
        text = "Loading...",
        color = LoadingGray.copy(alpha = alpha),
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center
    )
}

// ─── Preview ──────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true, name = "Splash Screen")
@Composable
private fun SplashScreenPreview() {
    SmartAlignoraApp()
}
