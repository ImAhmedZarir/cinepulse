package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ═════════════════════════════════════════════════════════════════════════════
// 💎 STRICT 3-COLOR PALETTE (Exact Logo Red, Pitch Black & Pure White)
// Inspired by Netflix & Apple TV — Zero grayish/muddy tones, Zero orange fade
// ═════════════════════════════════════════════════════════════════════════════

// 1. Exact CinePulse Logo Crimson Red (Electric, punchy, sampled directly from logo)
val LogoRed                = Color(0xFFEE1E50)
val LogoRedLight           = Color(0xFFFF2A55)
val BrandCoral             = LogoRed
val BrandOrange            = LogoRedLight
val BrandBerry             = Color(0xFFC71542)

// 2. True Pitch Black (Pure OLED Black #000000)
val PitchBlack             = Color(0xFF000000)
val JetBlack               = PitchBlack
val CinemaBlack            = PitchBlack
val CinemaDark             = PitchBlack
val CinemaSurface          = Color(0xFF080808)   // Deep black surface
val CinemaSurfaceVariant   = Color(0xFF111111)   // Elevated black surface
val CinemaBorder           = Color(0x26FFFFFF)   // 15% pure white specular rim

// 3. Crisp Pure White (#FFFFFF)
val PureWhite              = Color(0xFFFFFFFF)
val TextPrimary            = PureWhite
val TextSecondary          = Color(0xB3FFFFFF)   // 70% white
val TextMuted              = Color(0x66FFFFFF)   // 40% white

// Unified Accents (Strictly Pure White or Logo Red — No cyan, gold or murky tones)
val CinemaRed              = LogoRed
val CinemaRedLight         = LogoRedLight
val CinemaOrange           = LogoRedLight
val CinemaAmber            = LogoRed
val CinemaCyan             = PureWhite           // Subtitle/Audio icons are pure white
val CinemaGreen            = LogoRed
val RatingGold             = PureWhite           // Clean Apple TV style white star

// ── Vibrant Logo Red Gradients ────────────────────────────────────────────────
val BrandLogoGradient      = listOf(LogoRedLight, LogoRed)
val BrandButtonGradient    = listOf(LogoRedLight, LogoRed)
val CinemaRedGradient      = BrandButtonGradient
val CinemaGradientBrush    = Brush.horizontalGradient(BrandButtonGradient)
val BrandFullGradientBrush = Brush.horizontalGradient(BrandLogoGradient)

// ── Apple TV & Netflix Pure Glass Design System (liquid-glass-main) ───────────
// Frosted glass over Pitch Black with crisp specular highlights (NO grayish tint)
val LiquidGlassBackground        = Color(0x14FFFFFF)   // 8% white frosted glass
val LiquidGlassSurface           = Color(0x1FFFFFFF)   // 12% white frosted glass
val LiquidGlassSurfaceElevated   = Color(0x2EFFFFFF)   // 18% white frosted glass
val LiquidGlassSurfaceHigh       = Color(0x3DFFFFFF)   // 24% white frosted glass
val LiquidGlassBorder            = Color(0x26FFFFFF)   // 15% white rim
val LiquidGlassSpecularHighlight = Color(0x80FFFFFF)   // 50% top white highlight
val LiquidGlassSpecularBottom    = Color(0x14FFFFFF)   // 8% bottom bounce rim
val LiquidGlassNavBg             = Color(0xF5000000)   // 96% Pitch Black dock
val LiquidGlassPillActive        = LogoRed
val LiquidGlassPillActiveBg      = Color(0x33EE1E50)

// Aliases
val GlassBackground     = LiquidGlassBackground
val GlassSurface        = LiquidGlassSurface
val GlassBorder         = LiquidGlassBorder
val GlassBorderSpecular = Color(0x40FFFFFF)
val GlassActiveBorder   = LogoRed

// Specular rim: 50% white top -> 15% lateral -> 8% bottom bounce
val LiquidGlassBorderBrush = Brush.verticalGradient(
    listOf(
        LiquidGlassSpecularHighlight,
        LiquidGlassBorder,
        LiquidGlassSpecularBottom
    )
)

val LiquidGlassActiveBorderBrush = Brush.horizontalGradient(
    listOf(LogoRedLight, LogoRed)
)

// Ambient sheen: 24% specular glare top-left fading down
val LiquidGlassSheenBrush = Brush.linearGradient(
    colorStops = arrayOf(
        0f    to Color(0x30FFFFFF),
        0.35f to Color(0x0AFFFFFF),
        0.75f to Color(0x00FFFFFF)
    )
)

// Translucent pitch-black glass gradient
val LiquidGlassSurfaceGradient = Brush.verticalGradient(
    listOf(
        Color(0x24FFFFFF),
        Color(0x08FFFFFF)
    )
)
