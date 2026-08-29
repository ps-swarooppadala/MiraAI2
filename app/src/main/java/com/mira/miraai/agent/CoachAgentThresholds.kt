package com.mira.miraai.agent

/**
 * Numeric tolerances for the Coach Agent state machine. feature-spec.md Section 10.2 states the
 * guardrail rules (cooldown, escalation, confidence gate) but gives no numeric values — every
 * constant below is a placeholder pending on-device tuning (see docs/PROGRESS.md), not a value
 * sourced from spec.
 */
object CoachAgentThresholds {
    // placeholder — needs on-device tuning
    const val CUE_COOLDOWN_MS = 8_000L
    // placeholder — needs on-device tuning
    const val SAFETY_COOLDOWN_MS = 3_000L
    // placeholder — needs on-device tuning
    const val MIN_CONFIDENCE_TO_COACH = 0.6f
    // placeholder — needs on-device tuning
    const val ESCALATION_THRESHOLD = 2
}
