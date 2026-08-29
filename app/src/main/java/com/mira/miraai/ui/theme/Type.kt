package com.mira.miraai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Mira's editorial type pairing (feature-spec.md Section 7 / docs/ux/DESIGN.md): Playfair
 * Display for headlines, DM Sans for body/label text. Bundling the actual Google Fonts as
 * assets is out of scope for this pass (no network access to fetch the .ttf files from this
 * environment) — [FontFamily.Serif]/[FontFamily.SansSerif] approximate the pairing's contrast
 * for now. Flagged in docs/PROGRESS.md as needing real font assets.
 */
private val playfairDisplay = FontFamily.Serif
private val dmSans = FontFamily.SansSerif

object MiraType {
    val hero = TextStyle(fontFamily = playfairDisplay, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = (-0.96).sp)
    val headlineLg = TextStyle(fontFamily = playfairDisplay, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp)
    val headlineLgMobile = TextStyle(fontFamily = playfairDisplay, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp)
    val headlineMd = TextStyle(fontFamily = playfairDisplay, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp)
    val bodyLg = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 28.sp)
    val bodyMd = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val labelMd = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.7.sp)
    val labelSm = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp)
}

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)