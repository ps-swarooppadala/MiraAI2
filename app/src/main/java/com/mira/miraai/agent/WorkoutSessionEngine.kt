package com.mira.miraai.agent

import com.mira.miraai.assessor.HoldTimer
import com.mira.miraai.assessor.HoldTimerState
import com.mira.miraai.assessor.VerdictCode
import com.mira.miraai.assessor.WarriorIIVerdict
import com.mira.miraai.content.Routine

/**
 * Routine Sequencer + Step Phase FSM — feature-spec.md Section 8 / build-architecture.md
 * Section 11.1 harness responsibilities 1-2. Pure Kotlin (CLAUDE.md), no dependency on
 * [com.mira.miraai.agent.CoachAgent] — that's a separate brain deciding *what to say*; this
 * engine only decides *what step/phase we're in and for how long*, per the "two brains" split.
 *
 * Only Warrior II (`warrior_ii`) has a real Assessor rule set today (Phase 1). Steps for other
 * poses in the MVP routine (Tree, Chair) tick their hold timer with `verdict = null`, which this
 * engine treats as an always-clean hold — i.e. timer-only, no live form-gating. This is a known
 * limitation, flagged in docs/PROGRESS.md, not a silent gap: those poses don't have rule sets
 * yet (see Phase 5's `isCoachingSupported` note), so there is nothing to gate on.
 */
class WorkoutSessionEngine(
    val routine: Routine,
    private val config: WorkoutThresholdsConfig = WorkoutThresholdsConfig(),
) {
    val steps = expandRoutineSteps(routine)

    private var stepIndex = 0
    private var phase = SessionPhase.HOLDING
    private var isPaused = false
    private var restRemainingMs = 0L
    private var totalElapsedMs = 0L

    private var holdTimer = HoldTimer(currentTargetHoldSec())
    private var holdState = HoldTimerState(0L, 0L, false)
    private val verdictCounts = mutableMapOf<VerdictCode, Int>()
    private val results = mutableListOf<StepResult>()

    private var lastVerdictCode: VerdictCode? = null
    private var lastCueTimestampMs: Long = 0L
    private var improvedSinceLastCue: Boolean = false
    private var lastConfidence: Float = 1f

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    /**
     * Advances the session by [deltaMs]. [verdict] is the current frame's Assessor output for
     * the step's pose, or null when no rule set exists for it (see class doc). [coachDecision],
     * if supplied, updates the cue-facing fields ([WorkoutSessionState.lastVerdictCode] etc.) —
     * this engine doesn't decide cues itself, [CoachAgent] does (Section 8.2 says pause freezes
     * "new voice cues" too, but that's enforced by [CoachAgent.pause] independently).
     */
    fun tick(deltaMs: Long, verdict: WarriorIIVerdict? = null, coachDecision: CoachDecision? = null): WorkoutSessionState {
        totalElapsedMs += deltaMs
        lastConfidence = verdict?.confidence ?: lastConfidence

        if (coachDecision != null && coachDecision.intent != CoachIntent.SILENT) {
            lastVerdictCode = coachDecision.verdictCode
            lastCueTimestampMs = totalElapsedMs
            improvedSinceLastCue = coachDecision.intent == CoachIntent.CONFIRM_IMPROVEMENT
        }

        if (isPaused) return currentState(SessionPhase.PAUSED)

        if (phase == SessionPhase.REST) {
            restRemainingMs -= deltaMs
            if (restRemainingMs <= 0) phase = SessionPhase.HOLDING
            return currentState(phase)
        }

        if (phase == SessionPhase.SUMMARY) return currentState(phase)

        val hasCriticalIssue = verdict?.hasCriticalIssue ?: false
        if (verdict != null) verdictCounts.merge(verdict.verdictCode, 1, Int::plus)

        holdState = holdTimer.tick(holdState, deltaMs, isPaused = false, hasCriticalIssue = hasCriticalIssue)
        phase = if (hasCriticalIssue) SessionPhase.CORRECTING else SessionPhase.HOLDING

        if (holdState.isComplete) {
            completeCurrentStep()
        }

        return currentState(phase)
    }

    /** Section 8.7: ending early records the in-progress step as attempted, not completed. */
    fun endEarly(): SessionSummary {
        if (phase != SessionPhase.SUMMARY) {
            val step = steps.getOrNull(stepIndex)
            if (step != null) {
                results += StepResult(step.poseId, step.side, mostFrequentOrGoodForm(), completed = false)
            }
        }
        phase = SessionPhase.SUMMARY
        return buildSummary()
    }

    fun isComplete(): Boolean = phase == SessionPhase.SUMMARY

    fun currentStep(): ExpandedStep? = steps.getOrNull(stepIndex)

    fun buildSummary(): SessionSummary {
        val nextFocus = results
            .map { it.mostFrequentVerdict }
            .filter { it != VerdictCode.GOOD_FORM }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        return SessionSummary(totalElapsedMs, results.toList(), nextFocus)
    }

    private fun completeCurrentStep() {
        val step = steps[stepIndex]
        results += StepResult(step.poseId, step.side, mostFrequentOrGoodForm(), completed = true)
        verdictCounts.clear()

        val nextIndex = stepIndex + 1
        if (nextIndex >= steps.size) {
            phase = SessionPhase.SUMMARY
            return
        }

        val nextStep = steps[nextIndex]
        val isSwitchingSides = nextStep.poseId == step.poseId && nextStep.side != step.side
        restRemainingMs = if (isSwitchingSides) config.restBetweenSidesMs else config.restBetweenPosesMs
        stepIndex = nextIndex
        holdTimer = HoldTimer(currentTargetHoldSec())
        holdState = HoldTimerState(0L, 0L, false)
        phase = SessionPhase.REST
    }

    private fun mostFrequentOrGoodForm(): VerdictCode =
        verdictCounts.maxByOrNull { it.value }?.key ?: VerdictCode.GOOD_FORM

    private fun currentTargetHoldSec(): Int = steps.getOrNull(stepIndex)?.targetHoldSec ?: 0

    private fun currentState(effectivePhase: SessionPhase): WorkoutSessionState = WorkoutSessionState(
        routine = routine,
        currentStepIndex = stepIndex,
        phase = effectivePhase,
        elapsedHoldSec = (holdState.elapsedHoldMs / 1000L).toInt(),
        repCount = 0,
        lastVerdictCode = lastVerdictCode?.name,
        lastCueTimestampMs = lastCueTimestampMs,
        improvedSinceLastCue = improvedSinceLastCue,
        confidenceScore = lastConfidence,
        isPaused = isPaused,
    )
}
