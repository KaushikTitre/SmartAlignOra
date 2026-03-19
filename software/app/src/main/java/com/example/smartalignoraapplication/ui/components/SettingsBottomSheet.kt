package com.example.smartalignoraapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smartalignoraapplication.controller.SettingsState
import com.example.smartalignoraapplication.controller.VibrationSequence
import com.example.smartalignoraapplication.ui.theme.AppColors
import com.example.smartalignoraapplication.ui.theme.AppDimens

// =============================================================================
// SettingsBottomSheet
//
// Motor control protocol:
//   S1-S6  → Start pattern (when bad posture)
//   V1-V100 → Intensity (sent live on slider change)
//   0       → Stop (when good posture)
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    state:                     SettingsState,
    onDismiss:                 () -> Unit,
    onVibrationToggled:        (Boolean) -> Unit,
    onVibrationLevelChanged:   (Float) -> Unit,
    onVibrationLevelFinished:  () -> Unit,
    onVibrationSequenceChanged:(VibrationSequence) -> Unit = {},
    onFallAlertToggled:        (Boolean) -> Unit = {},
    onFallAlertEmailChanged:   (String) -> Unit = {},
    showEmailDialog:           Boolean = false,
    onEmailConfirmed:          (String) -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = AppColors.CardWhite,
        shape            = RoundedCornerShape(topStart = AppDimens.radiusXl, topEnd = AppDimens.radiusXl)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, null, tint = AppColors.PurplePrimary,
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = AppColors.TextSecondary,
                        modifier = Modifier.size(20.dp))
                }
            }

            Divider(color = AppColors.BorderColor)

            // ── Vibration ON/OFF ──────────────────────────────────────────────
            SettingToggleRow(
                icon     = Icons.Default.Vibration,
                iconTint = if (state.vibrationEnabled) AppColors.ElectricBlue
                else AppColors.TextSecondary,
                title    = "Vibration alerts",
                subtitle = if (state.vibrationEnabled)
                    "Pattern: ${state.vibrationSequence.label} · ${state.vibrationSequence.bleCommand}"
                else "Haptic feedback on bad posture",
                checked         = state.vibrationEnabled,
                onCheckedChange = onVibrationToggled
            )

            Divider(color = AppColors.BorderColor)

            // ── Intensity slider — V1-V100 ────────────────────────────────────
            VibrationSliderRow(
                level           = state.vibrationLevel,
                enabled         = state.vibrationEnabled,
                onLevelChanged  = onVibrationLevelChanged,
                onLevelFinished = onVibrationLevelFinished
            )

            Divider(color = AppColors.BorderColor)

            // ── Pattern dropdown — S1-S6 ──────────────────────────────────────
            VibrationSequenceRow(
                selected   = state.vibrationSequence,
                enabled    = state.vibrationEnabled,
                onSelected = onVibrationSequenceChanged
            )

            Divider(color = AppColors.BorderColor)

            // ── Fall Alert ────────────────────────────────────────────────────
            SettingToggleRow(
                icon     = Icons.Default.PersonOff,
                iconTint = if (state.fallAlertEnabled) AppColors.BadRed
                else AppColors.TextSecondary,
                title    = "Fall alert",
                subtitle = if (state.fallAlertEnabled && state.fallAlertEmail.isNotBlank())
                    "📧 ${state.fallAlertEmail} · Auto-email after 10 falls"
                else "Email emergency contact when fall detected",
                checked         = state.fallAlertEnabled,
                onCheckedChange = onFallAlertToggled
            )

            Spacer(Modifier.height(16.dp))

            // Done button
            Button(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(AppDimens.radiusMd),
                colors   = ButtonDefaults.buttonColors(containerColor = AppColors.BlueNavy)
            ) {
                Text("Done", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Email input dialog — shown when fall alert toggled ON
    if (showEmailDialog) {
        EmailInputDialog(
            initialEmail = state.fallAlertEmail,
            onConfirm    = onEmailConfirmed,
            onDismiss    = { onEmailConfirmed("") }
        )
    }
}

// =============================================================================
// EMAIL INPUT DIALOG
// =============================================================================
@Composable
fun EmailInputDialog(
    initialEmail: String,
    onConfirm:    (String) -> Unit,
    onDismiss:    () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var error by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(AppDimens.radiusXl),
            colors    = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AppColors.BadBg)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.PersonOff, null, tint = AppColors.BadRed,
                        modifier = Modifier.size(28.dp))
                }

                Spacer(Modifier.height(16.dp))

                Text("Fall Alert Setup", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Enter an emergency contact email. When 10+ consecutive falls are detected, an alert email with your GPS location will be sent automatically.",
                    fontSize = 13.sp, color = AppColors.TextSecondary, lineHeight = 20.sp
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value          = email,
                    onValueChange  = { email = it; error = "" },
                    label          = { Text("Emergency contact email") },
                    placeholder    = { Text("example@gmail.com") },
                    leadingIcon    = {
                        Icon(Icons.Default.Email, null, tint = AppColors.BadRed,
                            modifier = Modifier.size(18.dp))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError        = error.isNotEmpty(),
                    supportingText = if (error.isNotEmpty()) {
                        { Text(error, color = AppColors.BadRed, fontSize = 11.sp) }
                    } else null,
                    modifier   = Modifier.fillMaxWidth(),
                    shape      = RoundedCornerShape(AppDimens.radiusLg),
                    colors     = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.BadRed,
                        focusedLabelColor  = AppColors.BadRed
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape    = RoundedCornerShape(AppDimens.radiusLg)
                    ) { Text("Cancel", color = AppColors.TextSecondary) }

                    Button(
                        onClick = {
                            if (email.isBlank() || !email.contains("@")) {
                                error = "Enter a valid email address"
                            } else {
                                onConfirm(email.trim())
                            }
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape    = RoundedCornerShape(AppDimens.radiusLg),
                        colors   = ButtonDefaults.buttonColors(containerColor = AppColors.BadRed)
                    ) { Text("Enable Alert", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// =============================================================================
// REUSABLE ROWS
// =============================================================================

@Composable
private fun SettingToggleRow(
    icon: ImageVector, iconTint: Color,
    title: String, subtitle: String,
    checked: Boolean, onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = AppColors.TextSecondary,
                modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors  = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = AppColors.TealAccent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = AppColors.BorderColor
            )
        )
    }
}

@Composable
private fun VibrationSliderRow(
    level: Float, enabled: Boolean,
    onLevelChanged: (Float) -> Unit, onLevelFinished: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Speed, null,
                tint = if (enabled) AppColors.ElectricBlue else AppColors.TextSecondary,
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Vibration intensity",
                    fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = if (enabled) AppColors.TextPrimary else AppColors.TextSecondary)
                // Shows V command that will be sent
                Text("Sends: V${level.toInt()} to ESP32",
                    fontSize = 12.sp, color = AppColors.TextSecondary)
            }
            Surface(shape = RoundedCornerShape(AppDimens.radiusSm),
                color = AppColors.ElectricBlue.copy(alpha = if (enabled) 0.12f else 0.05f)) {
                Text(vibrationLabel(level), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = if (enabled) AppColors.ElectricBlue else AppColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value = level, onValueChange = onLevelChanged,
            onValueChangeFinished = onLevelFinished,
            enabled = enabled, valueRange = 1f..100f,
            colors = SliderDefaults.colors(
                thumbColor               = AppColors.ElectricBlue,
                activeTrackColor         = AppColors.ElectricBlue,
                inactiveTrackColor       = AppColors.BorderColor,
                disabledThumbColor       = AppColors.TextSecondary,
                disabledActiveTrackColor = AppColors.TextSecondary
            ), modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Soft", "Low", "Medium", "High", "Max").forEach {
                Text(it, fontSize = 10.sp, color = AppColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun VibrationSequenceRow(
    selected: VibrationSequence, enabled: Boolean,
    onSelected: (VibrationSequence) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    fun label(seq: VibrationSequence) = when (seq) {
        VibrationSequence.SHORT       -> "▪ Short"
        VibrationSequence.MEDIUM      -> "▬ Medium"
        VibrationSequence.LONG        -> "━━ Long"
        VibrationSequence.TUK_TUK    -> "•• Tuk Tuk"
        VibrationSequence.KNOCK_KNOCK -> "▪▪▪ Knock Knock"
        VibrationSequence.HEARTBEAT   -> "♥ Heartbeat"
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GraphicEq, null,
                tint = if (enabled) AppColors.ElectricBlue else AppColors.TextSecondary,
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Vibration pattern", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = if (enabled) AppColors.TextPrimary else AppColors.TextSecondary)
                Text("Sends: ${selected.bleCommand} on bad posture",
                    fontSize = 12.sp, color = AppColors.TextSecondary)
            }
        }
        Spacer(Modifier.height(10.dp))

        // Dropdown trigger
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimens.radiusMd))
                .border(1.dp,
                    if (enabled) AppColors.ElectricBlue.copy(alpha = 0.4f)
                    else AppColors.BorderColor,
                    RoundedCornerShape(AppDimens.radiusMd))
                .background(
                    if (enabled) AppColors.ElectricBlue.copy(alpha = 0.05f)
                    else AppColors.BorderColor.copy(alpha = 0.3f)
                )
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                // BLE command badge
                Surface(shape = RoundedCornerShape(6.dp),
                    color = AppColors.TealAccent.copy(alpha = if (enabled) 0.15f else 0.05f)) {
                    Text(selected.bleCommand, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = if (enabled) AppColors.TealAccent else AppColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(label(selected), fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = if (enabled) AppColors.TextPrimary else AppColors.TextSecondary,
                    modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    null, tint = if (enabled) AppColors.ElectricBlue else AppColors.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.85f).background(AppColors.CardWhite)) {
            VibrationSequence.entries.forEach { seq ->
                val isSelected = seq == selected
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(6.dp),
                                color = if (isSelected)
                                    AppColors.TealAccent.copy(alpha = 0.15f)
                                else AppColors.BorderColor.copy(alpha = 0.5f)) {
                                Text(seq.bleCommand, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    color = if (isSelected) AppColors.TealAccent
                                    else AppColors.TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(label(seq), fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold
                                else FontWeight.Normal,
                                color = if (isSelected) AppColors.ElectricBlue
                                else AppColors.TextPrimary)
                        }
                    },
                    onClick = { onSelected(seq); expanded = false },
                    modifier = Modifier.background(
                        if (isSelected) AppColors.ElectricBlue.copy(alpha = 0.06f)
                        else Color.Transparent)
                )
                if (seq != VibrationSequence.entries.last())
                    Divider(color = AppColors.BorderColor, thickness = 0.5.dp)
            }
        }
    }
}

private fun vibrationLabel(level: Float): String = when {
    level < 20f -> "Soft"
    level < 40f -> "Low"
    level < 60f -> "Medium"
    level < 80f -> "High"
    else        -> "Max"
}