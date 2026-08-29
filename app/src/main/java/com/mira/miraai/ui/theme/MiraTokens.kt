package com.mira.miraai.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design tokens transcribed from docs/ux/DESIGN.md and the Stitch exports' Tailwind config
 * (docs/ux/home_the_invitation, workout_picker_choose_your_practice, setup_preparing_your_space).
 * Kept as a standalone object rather than folded into MaterialTheme's ColorScheme/Typography —
 * Mira's palette (primary = near-black forest green, not a Material tonal family) doesn't map
 * cleanly onto M3 roles, and the existing Purple/Pink scheme from earlier phases isn't touched
 * by this change.
 *
 * feature-spec.md Section 7 tokens are the fallback per CLAUDE.md; these Stitch-export values
 * take precedence, per feature-spec.md Section 7's own conflict rule.
 */
object MiraColors {
    val primary = Color(0xFF00241A)
    val primaryContainer = Color(0xFF0E3B2E)
    val primaryFixed = Color(0xFFBFECD9)
    val primaryFixedDim = Color(0xFFA3D0BE)
    val onPrimaryFixed = Color(0xFF002117)

    val secondary = Color(0xFF5E5E5C)
    val tertiary = Color(0xFF735C00)
    val tertiaryContainer = Color(0xFFCBA72F)
    val softGold = Color(0xFFE6BE8A)
    /** [color.accent] — Warm terracotta, feature-spec.md Section 7: "Progress rings, highlights, rep counter." */
    val accent = Color(0xFFE08E5B)

    val background = Color(0xFFF1FCF7)
    val surface = Color(0xFFF1FCF7)
    val surfaceContainerLow = Color(0xFFEBF6F1)
    val surfaceContainer = Color(0xFFE5F0EB)
    val surfaceContainerHigh = Color(0xFFDFEBE6)
    val surfaceContainerLowest = Color(0xFFFFFFFF)
    val surfaceVariant = Color(0xFFDAE5E0)

    val onSurface = Color(0xFF141E1B)
    val onSurfaceVariant = Color(0xFF414845)
    val outline = Color(0xFF717974)
    val outlineVariant = Color(0xFFC0C8C3)

    val error = Color(0xFFBA1A1A)
}

/** [spacing.unit] token family — spacing.unit = 8dp per feature-spec.md Section 7. */
object MiraSpacing {
    val unit = 8.dp
    val gutter = 16.dp
    val stackSm = 12.dp
    val stackMd = 24.dp
    val stackLg = 40.dp
    val containerPadding = 24.dp
}

/** [radius.card] token — 16dp per feature-spec.md Section 7; Stitch exports use a larger 24dp for hero cards. */
object MiraRadius {
    val sm = 8.dp
    val card = 16.dp
    val md = 24.dp
    val lg = 32.dp
}
