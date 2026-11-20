package com.naph.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class NaphThemePreset(
    val label: String,
    val accent: Color,
    val accentSecondary: Color,
    val bg: Color = Color(0xFF0A0B0E),
    val surface: Color = Color(0xFF13151B),
    val card: Color = Color(0xFF1A1D26)
) {
    ARCTIC_BLUE("Arctic Blue", Color(0xFF3B82F6), Color(0xFF38BDF8)),
    GOLD("Cinema Gold", Color(0xFFE8A91C), Color(0xFFFFD45C), Color(0xFF0E0D0A), Color(0xFF181611), Color(0xFF221E16)),
    JADE("Emerald Jade", Color(0xFF10B981), Color(0xFF34D399)),
    CRIMSON("Netflix Crimson", Color(0xFFE50914), Color(0xFFFF4D57)),
    VIOLET("HBO Violet", Color(0xFF8B5CF6), Color(0xFFA78BFA)),
    GRAPHITE("Apple TV Graphite", Color(0xFFE2E8F0), Color(0xFF94A3B8))
}

object NaphColors {
    val ObsidianBg = Color(0xFF0A0B0E)
    val DeepSurface = Color(0xFF13151B)
    val CardSurface = Color(0xFF1A1D26)
    val GlassSurface = Color(0xE6151821)
    val ElevatedBorder = Color(0xFF2C3142)

    val ElectricPrimary = Color(0xFF3B82F6)
    val CyanGlow = Color(0xFF38BDF8)
    val CrimsonPlay = Color(0xFFE50914)
    val GoldRating = Color(0xFFF5C518)
    val EmeraldSeed = Color(0xFF10B981)
    val AmberWarn = Color(0xFFF59E0B)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFCBD5E1)
    val TextMuted = Color(0xFF94A3B8)
}

@Composable
fun naphTextFieldColors(accent: Color = MaterialTheme.colorScheme.primary): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        disabledTextColor = NaphColors.TextMuted,
        cursorColor = accent,
        focusedBorderColor = accent,
        unfocusedBorderColor = NaphColors.ElevatedBorder,
        focusedLabelColor = accent,
        unfocusedLabelColor = NaphColors.TextSecondary,
        focusedPlaceholderColor = NaphColors.TextMuted,
        unfocusedPlaceholderColor = NaphColors.TextMuted,
        focusedContainerColor = NaphColors.DeepSurface,
        unfocusedContainerColor = NaphColors.DeepSurface
    )

private val NaphTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 38.sp,
        letterSpacing = (-0.8).sp,
        color = NaphColors.TextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        letterSpacing = (-0.4).sp,
        color = NaphColors.TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        color = NaphColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = NaphColors.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = NaphColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = NaphColors.TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        color = NaphColors.TextPrimary
    )
)

@Composable
fun NaphTheme(
    preset: NaphThemePreset = NaphThemePreset.ARCTIC_BLUE,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val bg = if (amoledBlack) Color(0xFF000000) else preset.bg
    val surf = if (amoledBlack) Color(0xFF0A0A0D) else preset.surface
    val card = if (amoledBlack) Color(0xFF121318) else preset.card

    val scheme = darkColorScheme(
        primary = preset.accent,
        onPrimary = Color.White,
        secondary = preset.accentSecondary,
        onSecondary = Color.Black,
        tertiary = NaphColors.CrimsonPlay,
        background = bg,
        onBackground = Color.White,
        surface = surf,
        onSurface = Color.White,
        surfaceVariant = card,
        onSurfaceVariant = NaphColors.TextSecondary,
        outline = NaphColors.ElevatedBorder,
        outlineVariant = NaphColors.ElevatedBorder.copy(alpha = 0.65f)
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = NaphTypography,
        content = content
    )
}
