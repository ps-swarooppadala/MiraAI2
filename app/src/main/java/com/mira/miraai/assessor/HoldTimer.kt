package com.mira.miraai.assessor

/** Progress of a single asana hold — feature-spec.md Section 8.2 (pause) / 8.3 (hold logic). */
data class HoldTimerState(
    val elapsedHoldMs: Long,
    val cleanStreakMs: Long,
    val isComplete: Boolean
)

/**
 * Advances a hold only while form is clean (no critical issue) and the player isn't paused.
 * A hold completes once the target duration is reached AND the trailing [CLEAN_TAIL_MS] were
 * clean — this window is explicit in feature-spec.md Section 8.3, not a placeholder.
 */
class HoldTimer(targetHoldSec: Int) {

    companion object {
        const val CLEAN_TAIL_MS = 2_000L
    }

    private val targetMs = targetHoldSec * 1_000L

    fun tick(state: HoldTimerState, deltaMs: Long, isPaused: Boolean, hasCriticalIssue: Boolean): HoldTimerState {
        if (isPaused) return state

        val elapsed = if (hasCriticalIssue) state.elapsedHoldMs else state.elapsedHoldMs + deltaMs
        val cleanStreak = if (hasCriticalIssue) 0L else state.cleanStreakMs + deltaMs
        val isComplete = elapsed >= targetMs && cleanStreak >= CLEAN_TAIL_MS

        return HoldTimerState(elapsed, cleanStreak, isComplete)
    }
}
