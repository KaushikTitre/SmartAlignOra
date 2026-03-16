package com.example.smartalignora.ui.screens
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Colors ───────────────────────────────────────────────────────────────────
private val SignUpPurple      = Color(0xFF6B21A8)
private val SignUpPurpleMid   = Color(0xFF9333EA)
private val SignUpPurpleWave  = Color(0xFFEDE9FE)
private val SignUpBg          = Color(0xFFFAF9FF)
private val SignUpTextDark    = Color(0xFF1A1A2E)
private val SignUpTextGray    = Color(0xFF9CA3AF)
private val SignUpInputBorder = Color(0xFFE9D5FF)
private val SignUpInputBg     = Color(0xFFFFFFFF)
private val SignUpGreen       = Color(0xFF22C55E)
private val SignUpErrorRed    = Color(0xFFEF4444)

// ===========================================================================
//  SCREEN — SignUpScreen
// ===========================================================================
@Composable
fun SignUpScreen(
    onBack: () -> Unit = {},
    onSignUp: () -> Unit = {},
    onLogin: () -> Unit = {}
) {
    var fullName        by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible        by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var agreedToTerms   by remember { mutableStateOf(false) }

    val passwordsMatch = password == confirmPassword || confirmPassword.isEmpty()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SignUpBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(52.dp))

            // ── Header row: back arrow + brand title ──────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = SignUpPurple
                    )
                }
                Text(
                    text = "SmartAlignora",
                    color = SignUpPurple,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Form card ─────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = SignUpPurple.copy(alpha = 0.08f),
                        spotColor = SignUpPurple.copy(alpha = 0.12f)
                    ),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Title
                    Text(
                        text = "Create Account",
                        color = SignUpTextDark,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Join SmartAlignora and start your journey to better posture today.",
                        color = SignUpTextGray,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Full Name
                    SignUpTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        placeholder = "Full Name",
                        icon = Icons.Default.Person,
                        keyboardType = KeyboardType.Text
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Email
                    SignUpTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email Address",
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = {
                            Text("Password", color = SignUpTextGray, fontSize = 14.sp)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SignUpPurpleMid,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            TextButton(
                                onClick = { passwordVisible = !passwordVisible },
                                contentPadding = PaddingValues(end = 8.dp)
                            ) {
                                Text(
                                    text = if (passwordVisible) "Hide" else "Show",
                                    color = SignUpPurpleMid,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = SignUpInputBorder,
                            focusedBorderColor = SignUpPurple,
                            unfocusedContainerColor = SignUpInputBg,
                            focusedContainerColor = SignUpInputBg,
                            cursorColor = SignUpPurple
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Confirm Password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = {
                            Text("Confirm Password", color = SignUpTextGray, fontSize = 14.sp)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (passwordsMatch) SignUpPurpleMid else SignUpErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            TextButton(
                                onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                                contentPadding = PaddingValues(end = 8.dp)
                            ) {
                                Text(
                                    text = if (confirmPasswordVisible) "Hide" else "Show",
                                    color = SignUpPurpleMid,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        isError = !passwordsMatch,
                        supportingText = {
                            if (!passwordsMatch) {
                                Text(
                                    text = "Passwords do not match",
                                    color = SignUpErrorRed,
                                    fontSize = 12.sp
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = if (passwordsMatch) SignUpInputBorder else SignUpErrorRed,
                            focusedBorderColor = if (passwordsMatch) SignUpPurple else SignUpErrorRed,
                            unfocusedContainerColor = SignUpInputBg,
                            focusedContainerColor = SignUpInputBg,
                            cursorColor = SignUpPurple
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Terms and conditions checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = agreedToTerms,
                            onCheckedChange = { agreedToTerms = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = SignUpPurple,
                                uncheckedColor = SignUpInputBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "I agree to the ",
                            color = SignUpTextGray,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Terms & Privacy Policy",
                            color = SignUpPurple,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Sign Up button
                    Button(
                        onClick = onSignUp,
                        enabled = agreedToTerms && passwordsMatch
                                && email.isNotEmpty()
                                && password.isNotEmpty()
                                && fullName.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SignUpPurple,
                            disabledContainerColor = SignUpPurple.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = "Create Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Already have account
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Already have an account? ",
                            color = SignUpTextGray,
                            fontSize = 14.sp
                        )
                        TextButton(
                            onClick = onLogin,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Login",
                                color = SignUpPurple,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }

        // ── Bottom wave ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            SignUpPurpleWave.copy(alpha = 0.6f),
                            SignUpPurpleWave
                        )
                    )
                )
        )

        // ── ChatBot FAB ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(58.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        ambientColor = SignUpPurple.copy(alpha = 0.3f),
                        spotColor = SignUpPurple.copy(alpha = 0.4f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SignUpPurple, SignUpPurpleMid)
                        )
                    )
            ) {
                Text(text = "🤖", fontSize = 26.sp, textAlign = TextAlign.Center)
            }
            // Green online dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(SignUpGreen)
                )
            }
        }
    }
}

// ─── Reusable text field ──────────────────────────────────────────────────────
@Composable
fun SignUpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = SignUpTextGray, fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SignUpPurpleMid,
                modifier = Modifier.size(20.dp)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = SignUpInputBorder,
            focusedBorderColor = SignUpPurple,
            unfocusedContainerColor = SignUpInputBg,
            focusedContainerColor = SignUpInputBg,
            cursorColor = SignUpPurple
        ),
        singleLine = true
    )
}

// ─── Preview ──────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpScreenPreview() {
    SignUpScreen()
}