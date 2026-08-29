package com.mira.miraai.agent

import com.mira.miraai.assessor.VerdictCode
import com.mira.miraai.perception.Side

/** One completed (or attempted-but-ended-early) step's result — feature-spec.md US-7. */
data class StepResult(
    val poseId: String,
    val side: Side?,
    val mostFrequentVerdict: VerdictCode,
    val completed: Boolean,
)

/**
 * US-7 Session Summary contract: total time, per-pose results, and a next-focus line derived
 * from the most frequent non-GOOD_FORM verdict across all steps.
 */
data class SessionSummary(
    val totalElapsedMs: Long,
    val stepResults: List<StepResult>,
    val nextFocusVerdict: VerdictCode?,
)
