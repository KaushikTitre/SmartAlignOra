package com.example.smartalignora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ProfPurple      = Color(0xFF6B21A8)
private val ProfPurpleMid   = Color(0xFF9333EA)
private val ProfPurpleLight = Color(0xFFF3E8FF)
private val ProfBg          = Color(0xFFF8F4FF)
private val ProfTextDark    = Color(0xFF1A1A2E)
private val ProfTextGray    = Color(0xFF6B7280)
private val ProfCardBg      = Color(0xFFFFFFFF)

@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    var name      by remember { mutableStateOf("Your Name") }
    var email     by remember { mutableStateOf("Your email id") }
    var age       by remember { mutableStateOf("Your Age") }
    var height    by remember { mutableStateOf("Your Height") }
    var weight    by remember { mutableStateOf("Your Weight") }
    var goal      by remember { mutableStateOf("Improve posture & reduce back pain") }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().background(ProfBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = ProfPurple)
            }
            Text(text = "Profile", color = ProfPurple, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            IconButton(onClick = { isEditing = !isEditing }) {
                Icon(imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = "Edit", tint = ProfPurple)
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            // Gradient header
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(colors = listOf(ProfPurple, ProfPurpleMid, ProfBg)))
                .padding(bottom = 32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
                    Box(contentAlignment = Alignment.Center,
                        modifier = Modifier.size(90.dp).clip(CircleShape).background(Color.White)
                            .border(3.dp, ProfPurpleLight, CircleShape)) {
                        Text(text = "👤", fontSize = 42.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isEditing) {
                        OutlinedTextField(value = name, onValueChange = { name = it },
                            modifier = Modifier.width(220.dp),
                            textStyle = LocalTextStyle.current.copy(color = Color.White,
                                fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                cursorColor = Color.White),
                            singleLine = true)
                    } else {
                        Text(text = name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = email, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text(text = "⭐ Premium Member", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfStatCard("Posture Score", "78",  "/100", Modifier.weight(1f))
                ProfStatCard("Days Active",   "24",  "days", Modifier.weight(1f))
                ProfStatCard("Improvement",   "+12", "%",    Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            ProfSectionTitle("Personal Information")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ProfCardBg),
                elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    ProfEditItem(Icons.Default.Person, "Full Name", name, isEditing) { name = it }
                    ProfDivider()
                    ProfEditItem(Icons.Default.Email, "Email", email, isEditing) { email = it }
                    ProfDivider()
                    ProfEditItem(Icons.Default.Star, "Age", age, isEditing) { age = it }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ProfSectionTitle("Body Metrics")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ProfCardBg),
                elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    ProfEditItem(Icons.Default.Person, "Height", height, isEditing) { height = it }
                    ProfDivider()
                    ProfEditItem(Icons.Default.Person, "Weight", weight, isEditing) { weight = it }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ProfSectionTitle("My Goal")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ProfCardBg),
                elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(ProfPurpleLight)) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = ProfPurple, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        if (isEditing) {
                            OutlinedTextField(value = goal, onValueChange = { goal = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Goal", color = ProfTextGray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ProfPurple,
                                    unfocusedBorderColor = Color(0xFFE9D5FF),
                                    cursorColor = ProfPurple),
                                shape = RoundedCornerShape(12.dp))
                        } else {
                            Column {
                                Text(text = "Current Goal", color = ProfTextGray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = goal, color = ProfTextDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ProfSectionTitle("Achievements")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ProfCardBg),
                elevation = CardDefaults.cardElevation(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    AchievementBadge("🏆", "7 Day\nStreak")
                    AchievementBadge("⭐", "Top\nPosture")
                    AchievementBadge("🎯", "Goal\nAchiever")
                    AchievementBadge("💪", "Strong\nBack")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ProfCardBg),
                elevation = CardDefaults.cardElevation(2.dp),
                onClick = onNavigateToSettings) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(ProfPurpleLight)) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = ProfPurple, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Settings", color = ProfTextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Manage app preferences", color = ProfTextGray, fontSize = 12.sp)
                    }
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = ProfTextGray)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfStatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ProfCardBg),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, color = ProfPurple, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = unit, color = ProfTextGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
            Text(text = label, color = ProfTextGray, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ProfEditItem(icon: ImageVector, label: String, value: String, isEditing: Boolean, onValueChange: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(ProfPurpleLight)) {
            Icon(imageVector = icon, contentDescription = null, tint = ProfPurple, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        if (isEditing) {
            OutlinedTextField(value = value, onValueChange = onValueChange,
                label = { Text(label, color = ProfTextGray, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ProfPurple, unfocusedBorderColor = Color(0xFFE9D5FF), cursorColor = ProfPurple),
                shape = RoundedCornerShape(12.dp), singleLine = true)
        } else {
            Column {
                Text(text = label, color = ProfTextGray, fontSize = 12.sp)
                Text(text = value, color = ProfTextDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun AchievementBadge(emoji: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center,
            modifier = Modifier.size(52.dp).clip(CircleShape).background(ProfPurpleLight)
                .border(2.dp, ProfPurple.copy(alpha = 0.3f), CircleShape)) {
            Text(text = emoji, fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, color = ProfTextGray, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun ProfSectionTitle(title: String) {
    Text(text = title, color = ProfPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp, modifier = Modifier.padding(start = 24.dp, bottom = 8.dp))
}

@Composable
fun ProfDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF3F4F6), thickness = 1.dp)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProfileScreenPreview() { ProfileScreen() }
