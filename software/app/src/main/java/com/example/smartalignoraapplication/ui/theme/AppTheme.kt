package com.example.smartalignoraapplication.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AppColors {
    // ── Primary palette ─────────────────────────────────────────────────────
    val PurplePrimary    = Color(0xFF6B21A8)
    val PurpleLight      = Color(0xFF9333EA)
    val PurpleSurface    = Color(0xFFF3E8FF)
    val BlueNavy         = Color(0xFF0F4C81)
    val ElectricBlue     = Color(0xFF1A73E8)
    val TealAccent       = Color(0xFF00C9A7)

    // ── Status ──────────────────────────────────────────────────────────────
    val GoodGreen        = Color(0xFF16A34A)
    val GoodBg           = Color(0xFFDCFCE7)
    val BadRed           = Color(0xFFDC2626)
    val BadBg            = Color(0xFFFEE2E2)
    val WaitingGray      = Color(0xFF6B7280)
    val WaitingBg        = Color(0xFFF1F5F9)

    // ── Surfaces ─────────────────────────────────────────────────────────────
    val White            = Color(0xFFFFFFFF)
    val SoftBg           = Color(0xFFF8F9FC)
    val CardWhite        = Color(0xFFFFFFFF)

    // ── Text ─────────────────────────────────────────────────────────────────
    val TextPrimary      = Color(0xFF0F172A)
    val TextSecondary    = Color(0xFF475569)
    val TextTertiary     = Color(0xFF94A3B8)

    // ── Border ───────────────────────────────────────────────────────────────
    val BorderColor      = Color(0xFFE2E8F0)
    val BorderMedium     = Color(0xFFCBD5E1)

    // ── Legacy aliases (keep old code working) ───────────────────────────────
    val NavyBlue         = BlueNavy
    val SoftBgLegacy     = SoftBg
    val DangerRed        = Color(0xFFE53935)
    val WarnOrange       = Color(0xFFF57C00)
    val SkinColor        = Color(0xFFFFCC80)
    val SuccessGreen     = GoodGreen
}

object AppDimens {
    val radiusSm   = 8.dp
    val radiusMd   = 12.dp
    val radiusLg   = 16.dp
    val radiusXl   = 24.dp
    val radiusXxl  = 32.dp
    val paddingSm  = 12.dp
    val paddingMd  = 16.dp
    val paddingLg  = 20.dp
    val paddingXl  = 24.dp
}