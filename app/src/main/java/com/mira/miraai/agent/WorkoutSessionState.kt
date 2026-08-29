package com.mira.miraai.agent

import com.mira.miraai.content.Routine

/** Live session phase — feature-spec.md Section 4.4. */
enum class SessionPhase {
    SETUP, FRAMING, CORRECTING, HOLDING, REP_COUNTING, REST, PAUSED, STEP_COMPLETE, SUMMARY
}

/**
 * Runtime session state — feature-spec.md Section 4.4, transcribed field-for-field. This is the
 * single source of truth the Coach Agent harness reads/writes and the UI observes.
 */
data class WorkoutSessionState(
    val routine: Routine,
    val currentStepIndex: Int,
    val phase: SessionPhase,
    val elapsedHoldSec: Int,
    val repCount: Int,
    val lastVerdictCode: String?,
    val lastCueTimestampMs: Long,
    val improvedSinceLastCue: Boolean,
    val confidenceScore: Float,
    val isPaused: Boolean,
)
