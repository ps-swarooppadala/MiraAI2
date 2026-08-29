package com.mira.miraai.memory

/**
 * Placeholder constants for [consolidate] — none of these are sourced from feature-spec.md or
 * build-architecture.md (Section 4 gives example rules only, e.g. "same issue on same pose
 * across >=3 sessions", not exact numbers). Flagged per CLAUDE.md as needing real tuning once
 * there's actual multi-session usage data to tune against.
 */
object ConsolidationThresholds {
    /** Confidence assigned the first time a Fact is inserted. */
    const val NEW_FACT_CONFIDENCE = 0.3f

    /** Confidence bump each time repeated evidence re-confirms an existing Fact. */
    const val CONFIDENCE_INCREMENT = 0.25f

    const val MAX_CONFIDENCE = 1.0f

    /** Drop in average hold time (seconds) between sessions that counts as "trending down". */
    const val SHORTER_HOLDS_TREND_THRESHOLD_SEC = 3
}
