package com.cinesync.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Liquid Glass Palette (iOS 26/27 inspired)
object CineColors {
    val VoidDark = Color(0xFF090A0E)
    val VoidSurface = Color(0xFF11131A)
    val VoidCard = Color(0xFF161824)

    val PrimaryCoral = Color(0xFFFF5722)
    val PrimaryGlow = Color(0xFFFF7A45)
    val PrimaryDark = Color(0xFFE64A19)

    val LaserCyan = Color(0xFF38BDF8)
    val LaserBlue = Color(0xFF0284C7)

    val NeonPurple = Color(0xFFA855F7)
    val NeonViolet = Color(0xFF9333EA)

    val EmeraldGreen = Color(0xFF10B981)
    val MintGlow = Color(0xFF34D399)

    val RatingGold = Color(0xFFFFB800)
    val AmberGlow = Color(0xFFFBBF24)

    val GlassWhiteSubtle = Color(0x14FFFFFF)
    val GlassWhiteMedium = Color(0x24FFFFFF)
    val GlassBorder = Color(0x33FFFFFF)
    val GlassBorderSubtle = Color(0x1AFFFFFF)
    val GlassDarkSurface = Color(0xB312141F)
    val GlassDock = Color(0xD90E1018)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
}

private val Scheme = darkColorScheme(
    primary = CineColors.PrimaryCoral,
    onPrimary = Color.White,
    primaryContainer = CineColors.PrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = CineColors.LaserCyan,
    onSecondary = Color.Black,
    tertiary = CineColors.NeonPurple,
    onTertiary = Color.White,
    background = CineColors.VoidDark,
    onBackground = CineColors.TextPrimary,
    surface = CineColors.VoidSurface,
    onSurface = CineColors.TextPrimary,
    surfaceVariant = CineColors.VoidCard,
    onSurfaceVariant = CineColors.TextSecondary,
    outline = CineColors.GlassBorderSubtle,
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp,
        color = CineColors.TextPrimary,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.2).sp,
        color = CineColors.TextPrimary,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        color = CineColors.TextPrimary,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = CineColors.TextPrimary,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = CineColors.TextPrimary,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = CineColors.TextSecondary,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = CineColors.TextSecondary,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.2.sp,
    ),
)

@Composable
fun CineTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = Scheme,
    typography = AppTypography,
    content = content,
)
