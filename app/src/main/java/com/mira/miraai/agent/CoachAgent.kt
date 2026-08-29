package com.mira.miraai.agent

import com.mira.miraai.assessor.VerdictCode
import com.mira.miraai.assessor.WarriorIIVerdict

/**
 * Coach Agent state machine — feature-spec.md Section 10.2. Pure Kotlin, no Android/CameraX/
 * Compose imports (CLAUDE.md). Consumes Phase 1's per-frame [WarriorIIVerdict] stream and
 * decides whether/how to speak, per the guardrail rules named in build-architecture.md Section 3:
 * one cue per cooldown window, escalate only on persistence, confirm improvement, safety
 * overrides everything, never coach below confidence threshold. ("Never diagnose" is a Mouth/
 * template constraint, not a state-machine rule, so it isn't modeled here.)
 *
 * Two ordering decisions the spec leaves open, made explicit here rather than buried:
 * - Pause outranks safety override: a paused workout must not mutate state or speak at all
 *   (Section 8.2 carves out no exception for critical issues).
 * - Safety cues bypass both the cue cooldown and the confidence gate; confirm-improvement cues
 *   bypass the cooldown but NOT the confidence gate (don't praise on garbage tracking data).
 */
class CoachAgent(private val clock: Clock, private val config: CoachAgentThresholdsConfig = CoachAgentThresholdsConfig()) {

    private var isPaused = false
    private var lastCueTimestampMs = Long.MIN_VALUE / 2
    private var lastSafetyTimestampMs = Long.MIN_VALUE / 2
    private var lastCuedVerdictCode: VerdictCode? = null
    private var uncorrectedCount = 0
    private var currentEscalation = CueEscalation.NORMAL
    private var lastSafetyVerdictCode: VerdictCode? = null
    private var safetyRepeatCount = 0

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun tick(verdict: WarriorIIVerdict): CoachDecision {
        if (isPaused) return CoachDecision(CoachIntent.SILENT, null)

        if (verdict.hasCriticalIssue) {
            return handleCritical(verdict)
        }

        if (verdict.verdictCode == VerdictCode.GOOD_FORM) {
            return handleGoodForm(verdict)
        }

        return handleIssue(verdict)
    }

    private fun handleCritical(verdict: WarriorIIVerdict): CoachDecision {
        val now = clock.nowMs()
        if (now - lastSafetyTimestampMs < config.safetyCooldownMs) {
            return CoachDecision(CoachIntent.SILENT, null)
        }
        lastSafetyTimestampMs = now
        safetyRepeatCount = if (verdict.verdictCode == lastSafetyVerdictCode) safetyRepeatCount + 1 else 0
        lastSafetyVerdictCode = verdict.verdictCode
        return CoachDecision(CoachIntent.SAFETY_OVERRIDE, verdict.verdictCode, repeatIndex = safetyRepeatCount)
    }

    private fun handleGoodForm(verdict: WarriorIIVerdict): CoachDecision {
        val wasCorrectingAnIssue = lastCuedVerdictCode != null && lastCuedVerdictCode != VerdictCode.GOOD_FORM
        if (!wasCorrectingAnIssue) return CoachDecision(CoachIntent.SILENT, null)

        lastCuedVerdictCode = VerdictCode.GOOD_FORM
        uncorrectedCount = 0
        currentEscalation = CueEscalation.NORMAL
        return CoachDecision(CoachIntent.CONFIRM_IMPROVEMENT, verdict.verdictCode)
    }

    private fun handleIssue(verdict: WarriorIIVerdict): CoachDecision {
        if (verdict.confidence < config.minConfidenceToCoach) {
            return CoachDecision(CoachIntent.SILENT, null)
        }

        val now = clock.nowMs()
        if (now - lastCueTimestampMs < config.cueCooldownMs) {
            return CoachDecision(CoachIntent.SILENT, null)
        }

        uncorrectedCount = if (verdict.verdictCode == lastCuedVerdictCode) uncorrectedCount + 1 else 1
        lastCuedVerdictCode = verdict.verdictCode
        currentEscalation = if (uncorrectedCount >= config.escalationThreshold) CueEscalation.FIRM else CueEscalation.NORMAL
        lastCueTimestampMs = now

        return CoachDecision(CoachIntent.SPEAK_CUE, verdict.verdictCode, currentEscalation, repeatIndex = uncorrectedCount - 1)
    }
}

/** Injected copy of [CoachAgentThresholds] so tests can override without touching the placeholders. */
data class CoachAgentThresholdsConfig(
    val cueCooldownMs: Long = CoachAgentThresholds.CUE_COOLDOWN_MS,
    val safetyCooldownMs: Long = CoachAgentThresholds.SAFETY_COOLDOWN_MS,
    val minConfidenceToCoach: Float = CoachAgentThresholds.MIN_CONFIDENCE_TO_COACH,
    val escalationThreshold: Int = CoachAgentThresholds.ESCALATION_THRESHOLD
)
