package com.example.smartalignora.ui.screens
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SetPurple      = Color(0xFF6B21A8)
private val SetPurpleMid   = Color(0xFF9333EA)
private val SetPurpleLight = Color(0xFFF3E8FF)
private val SetBg          = Color(0xFFF8F4FF)
private val SetTextDark    = Color(0xFF1A1A2E)
private val SetTextGray    = Color(0xFF6B7280)
private val SetRed         = Color(0xFFEF4444)
private val SetGreen       = Color(0xFF22C55E)
private val SetCardBg      = Color(0xFFFFFFFF)
private val SetOrange      = Color(0xFFF97316)
private val SetBlue        = Color(0xFF3B82F6)

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled     by remember { mutableStateOf(true) }
    var darkModeEnabled      by remember { mutableStateOf(false) }
    var autoConnectEnabled   by remember { mutableStateOf(true) }
    var postureAlertsEnabled by remember { mutableStateOf(true) }
    var weeklyReportEnabled  by remember { mutableStateOf(true) }
    var showResetDialog      by remember { mutableStateOf(false) }
    var showDeviceOffDialog  by remember { mutableStateOf(false) }
    var showDeleteDialog     by remember { mutableStateOf(false) }
    val scrollState          = rememberScrollState()

    if (showResetDialog) {
        ConfirmDialog(title = "Reset All Reports",
            message = "This will permanently delete all your posture reports and history.",
            confirmText = "Reset", confirmColor = SetRed,
            onConfirm = { showResetDialog = false }, onDismiss = { showResetDialog = false })
    }
    if (showDeviceOffDialog) {
        ConfirmDialog(title = "Turn Off Device",
            message = "Are you sure you want to disconnect and turn off your SmartAlignora device?",
            confirmText = "Turn Off", confirmColor = SetRed,
            onConfirm = { showDeviceOffDialog = false }, onDismiss = { showDeviceOffDialog = false })
    }
    if (showDeleteDialog) {
        ConfirmDialog(title = "Delete Account",
            message = "This will permanently delete your account and all associated data.",
            confirmText = "Delete", confirmColor = SetRed,
            onConfirm = { showDeleteDialog = false }, onDismiss = { showDeleteDialog = false })
    }

    Column(modifier = Modifier.fillMaxSize().background(SetBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SetPurple)
            }
            Text(text = "Settings", color = SetPurple, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.size(48.dp))
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 20.dp)) {

            SettingsSectionTitle(title = "Notifications")
            SettingsCard {
                SettingsToggleItem(Icons.Default.Notifications, SetPurpleLight, SetPurple, "Push Notifications", "Receive posture alerts", notificationsEnabled) { notificationsEnabled = it }
                SettingsDivider()
                SettingsToggleItem(Icons.Default.Phone, Color(0xFFEDE9FE), SetPurpleMid, "Vibration Alerts", "Haptic feedback on bad posture", vibrationEnabled) { vibrationEnabled = it }
                SettingsDivider()
                SettingsToggleItem(Icons.Default.Share, Color(0xFFECFDF5), SetGreen, "Weekly Report", "Get weekly posture summary", weeklyReportEnabled) { weeklyReportEnabled = it }
                SettingsDivider()
                SettingsToggleItem(Icons.Default.Warning, Color(0xFFFFF7ED), SetOrange, "Posture Alerts", "Alert when posture is poor", postureAlertsEnabled) { postureAlertsEnabled = it }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSectionTitle(title = "Device")
            SettingsCard {
                SettingsToggleItem(Icons.Default.Settings, Color(0xFFEFF6FF), SetBlue, "Auto Connect", "Connect device automatically", autoConnectEnabled) { autoConnectEnabled = it }
                SettingsDivider()
                SettingsClickItem(Icons.Default.Close, Color(0xFFFEF2F2), SetRed, "Turn Off Device", "Disconnect and power off") { showDeviceOffDialog = true }
                SettingsDivider()
                SettingsClickItem(Icons.Default.Refresh, Color(0xFFFFF7ED), SetOrange, "Recalibrate Device", "Reset device calibration") {}
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSectionTitle(title = "App Preferences")
            SettingsCard {
                SettingsToggleItem(Icons.Default.Star, Color(0xFF1E1B4B).copy(alpha = 0.1f), Color(0xFF4338CA), "Dark Mode", "Switch to dark theme", darkModeEnabled) { darkModeEnabled = it }
                SettingsDivider()
                SettingsClickItem(Icons.Default.Search, SetPurpleLight, SetPurple, "Language", "English (Default)") {}
                SettingsDivider()
                SettingsClickItem(Icons.Default.Edit, Color(0xFFECFDF5), SetGreen, "Measurement Unit", "Metric / Imperial") {}
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSectionTitle(title = "Data & Reports")
            SettingsCard {
                SettingsClickItem(Icons.Default.Delete, Color(0xFFFEF2F2), SetRed, "Reset All Reports", "Delete all posture history") { showResetDialog = true }
                SettingsDivider()
                SettingsClickItem(Icons.Default.Share, Color(0xFFECFDF5), SetGreen, "Export Data", "Download your posture data") {}
                SettingsDivider()
                SettingsClickItem(Icons.Default.Build, SetPurpleLight, SetPurple, "Backup Data", "Backup to cloud") {}
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSectionTitle(title = "Support")
            SettingsCard {
                SettingsClickItem(Icons.Default.Send, SetPurpleLight, SetPurple, "Contact Us", "support@smartalignora.com") {}
                SettingsDivider()
                SettingsClickItem(Icons.Default.Info, Color(0xFFFFF7ED), SetOrange, "Help & FAQ", "Get answers to common questions") {}
                SettingsDivider()
                SettingsClickItem(Icons.Default.Warning, Color(0xFFFEF2F2), SetRed, "Report a Bug", "Help us improve the app") {}
                SettingsDivider()
                SettingsClickItem(Icons.Default.Star, Color(0xFFFFFBEB), Color(0xFFF59E0B), "Rate the App", "Share your experience") {}
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSectionTitle(title = "Account")
            SettingsCard {
                SettingsClickItem(Icons.Default.Lock, SetPurpleLight, SetPurple, "Change Password", "Update your password") {}
                SettingsDivider()
                SettingsClickItem(Icons.Default.Info, Color(0xFFEFF6FF), SetBlue, "Privacy Policy", "Read our privacy policy") {}
                SettingsDivider()
                SettingsClickItem(Icons.Default.Check, Color(0xFFF0FDF4), SetGreen, "Terms of Service", "Read our terms") {}
                SettingsDivider()
                SettingsClickItem(Icons.Default.Delete, Color(0xFFFEF2F2), SetRed, "Delete Account", "Permanently delete your account") { showDeleteDialog = true }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = "SmartAlignora v1.0.0", color = SetTextGray, fontSize = 12.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SetRed)) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Logout", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(text = title, color = SetPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SetCardBg),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
fun SettingsToggleItem(icon: ImageVector, iconBg: Color, iconTint: Color,
                       title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(iconBg)) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = SetTextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = SetTextGray, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White,
                checkedTrackColor = SetPurple, uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD1D5DB)))
    }
}

@Composable
fun SettingsClickItem(icon: ImageVector, iconBg: Color, iconTint: Color,
                      title: String, subtitle: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }
        .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(iconBg)) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = SetTextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = SetTextGray, fontSize = 12.sp)
        }
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
            tint = SetTextGray, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp),
        color = Color(0xFFF3F4F6), thickness = 1.dp)
}

@Composable
fun ConfirmDialog(title: String, message: String, confirmText: String,
                  confirmColor: Color, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold, color = SetTextDark) },
        text = { Text(text = message, color = SetTextGray, fontSize = 14.sp) },
        confirmButton = {
            Button(onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                shape = RoundedCornerShape(10.dp)) {
                Text(text = confirmText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = "Cancel", color = SetTextGray) } },
        shape = RoundedCornerShape(20.dp))
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SettingsScreenPreview() { SettingsScreen() }
