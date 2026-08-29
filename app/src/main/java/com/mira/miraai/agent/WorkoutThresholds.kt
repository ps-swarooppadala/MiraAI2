package com.mira.miraai.agent

/**
 * needs tuning — placeholder constants for Workout Mode / Framing Assistant timing.
 * None are numerically sourced from feature-spec.md: Section 8.3's "open item" note about
 * unspecified numeric thresholds extends to these (framing confidence gate, rest windows).
 */
object WorkoutThresholds {
    const val FRAMING_CONFIDENCE_THRESHOLD = 0.75f
    const val FRAMING_SUSTAIN_MS = 1_000L

    // feature-spec.md Section 8.6: "5s Switch sides rest for BOTH; 3s transition between different poses" —
    // these two ARE spec-given, not placeholders.
    const val REST_BETWEEN_SIDES_MS = 5_000L
    const val REST_BETWEEN_POSES_MS = 3_000L
}

/** Injected copy so tests can override without touching the placeholders directly. */
data class WorkoutThresholdsConfig(
    val framingConfidenceThreshold: Float = WorkoutThresholds.FRAMING_CONFIDENCE_THRESHOLD,
    val framingSustainMs: Long = WorkoutThresholds.FRAMING_SUSTAIN_MS,
    val restBetweenSidesMs: Long = WorkoutThresholds.REST_BETWEEN_SIDES_MS,
    val restBetweenPosesMs: Long = WorkoutThresholds.REST_BETWEEN_POSES_MS,
)
