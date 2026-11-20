package com.rake.muza.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object MuzaColors {
    val Background = Color(0xFF0A0A0F)
    val Surface = Color(0xFF11131D)
    val SurfaceElevated = Color(0xFF171B29)
    val SurfaceHighlight = Color(0xFF1E2438)
    val Border = Color(0xFF242B42)
    val BorderActive = Color(0xFF3B82F6)

    val BlueDeep = Color(0xFF0047BB)
    val BluePrimary = Color(0xFF3B82F6)
    val BlueGlow = Color(0xFF60A5FA)
    val BlueSoft = Color(0xFF93C5FD)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)

    val Emerald = Color(0xFF22C55E)
    val EmeraldSoft = Color(0xFF34D399)
    val Danger = Color(0xFFF43F5E)
    val Warning = Color(0xFFF59E0B)
    val HudBackground = Color(0xD907090F)

    val BrandGradient = Brush.linearGradient(
        colors = listOf(BlueDeep, BluePrimary, BlueGlow)
    )

    val ButtonGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF2563EB), Color(0xFF4F7DF9), Color(0xFF3B82F6))
    )

    val CardGlowGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF161C30), Color(0xFF10131F))
    )
}
