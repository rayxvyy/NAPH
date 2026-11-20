package com.naph.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * NAPH Obsidian Dark Cinema Theme
 * Inspired by Apple TV+, HBO Max, and Netflix ultra-snappy dark aesthetics.
 */
object NaphColors {
    val ObsidianBg = Color(0xFF070709)
    val DeepSurface = Color(0xFF0E0F14)
    val CardSurface = Color(0xFF151720)
    val GlassSurface = Color(0xCC12141C)
    val ElevatedBorder = Color(0xFF232634)

    val ElectricPrimary = Color(0xFF6366F1) // Indigo Electric Accent
    val CyanGlow = Color(0xFF06B6D4)        // P2P / Instant Stream Highlight
    val CrimsonPlay = Color(0xFFE50914)     // Cinema Action Accent
    val GoldRating = Color(0xFFF5C518)      // IMDb / Rating Gold
    val EmeraldSeed = Color(0xFF10B981)     // Healthy P2P Seeders / Cached Debrid
    val AmberWarn = Color(0xFFF59E0B)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
}

private val NaphDarkColorScheme = darkColorScheme(
    primary = NaphColors.ElectricPrimary,
    onPrimary = Color.White,
    secondary = NaphColors.CyanGlow,
    onSecondary = Color.Black,
    tertiary = NaphColors.CrimsonPlay,
    background = NaphColors.ObsidianBg,
    onBackground = NaphColors.TextPrimary,
    surface = NaphColors.DeepSurface,
    onSurface = NaphColors.TextPrimary,
    surfaceVariant = NaphColors.CardSurface,
    onSurfaceVariant = NaphColors.TextSecondary,
    outline = NaphColors.ElevatedBorder
)

private val NaphTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 42.sp,
        letterSpacing = (-1.0).sp,
        color = NaphColors.TextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp,
        color = NaphColors.TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = NaphColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = NaphColors.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = NaphColors.TextSecondary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = NaphColors.TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp,
        color = NaphColors.TextPrimary
    )
)

@Composable
fun NaphTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NaphDarkColorScheme,
        typography = NaphTypography,
        content = content
    )
}
