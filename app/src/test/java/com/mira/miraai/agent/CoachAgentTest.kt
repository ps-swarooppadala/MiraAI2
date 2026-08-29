package com.mira.miraai.agent

import com.mira.miraai.assessor.VerdictCode
import com.mira.miraai.assessor.WarriorIIVerdict
import org.junit.Assert.assertEquals
import org.junit.Test

private const val COOLDOWN = CoachAgentThresholds.CUE_COOLDOWN_MS
private const val SAFETY_COOLDOWN = CoachAgentThresholds.SAFETY_COOLDOWN_MS
private const val MIN_CONFIDENCE = CoachAgentThresholds.MIN_CONFIDENCE_TO_COACH
private const val OK_CONFIDENCE = 0.9f

class FakeClock(private var nowMsValue: Long = 0L) : Clock {
    override fun nowMs(): Long = nowMsValue
    fun advanceBy(deltaMs: Long) {
        nowMsValue += deltaMs
    }
}

private fun issue(code: VerdictCode = VerdictCode.FRONT_KNEE_OVER_BENT, confidence: Float = OK_CONFIDENCE) =
    WarriorIIVerdict(code, hasCriticalIssue = false, confidence = confidence)

private fun critical(code: VerdictCode = VerdictCode.FRONT_KNEE_PAST_ANKLE, confidence: Float = OK_CONFIDENCE) =
    WarriorIIVerdict(code, hasCriticalIssue = true, confidence = confidence)

private fun goodForm(confidence: Float = OK_CONFIDENCE) =
    WarriorIIVerdict(VerdictCode.GOOD_FORM, hasCriticalIssue = false, confidence = confidence)

class CoachAgentTest {

    private val clock = FakeClock()
    private val agent = CoachAgent(clock)

    // --- 1. Cue cooldown ---

    @Test
    fun firstIssueVerdict_speaksCue() {
        val decision = agent.tick(issue())
        assertEquals(CoachIntent.SPEAK_CUE, decision.intent)
    }

    @Test
    fun secondIssueVerdict_insideCooldownWindow_isSilent() {
        agent.tick(issue())
        clock.advanceBy(COOLDOWN - 1)
        val decision = agent.tick(issue())
        assertEquals(CoachIntent.SILENT, decision.intent)
    }

    @Test
    fun sameIssue_afterCooldownElapses_speaksCueAgain() {
        agent.tick(issue())
        clock.advanceBy(COOLDOWN)
        val decision = agent.tick(issue())
        assertEquals(CoachIntent.SPEAK_CUE, decision.intent)
    }

    // --- 2. Escalation on persistence ---

    @Test
    fun sameUncorrectedCode_acrossEscalationThreshold_escalatesToFirm() {
        agent.tick(issue()) // 1st cue, NORMAL
        clock.advanceBy(COOLDOWN)
        val decision = agent.tick(issue()) // 2nd cue, same code -> FIRM
        assertEquals(CueEscalation.FIRM, decision.escalation)
    }

    @Test
    fun codeChanges_resetsEscalationToNormal() {
        agent.tick(issue(VerdictCode.FRONT_KNEE_OVER_BENT))
        clock.advanceBy(COOLDOWN)
        agent.tick(issue(VerdictCode.FRONT_KNEE_OVER_BENT)) // escalates to FIRM
        clock.advanceBy(COOLDOWN)
        val decision = agent.tick(issue(VerdictCode.TORSO_LEANING))
        assertEquals(CueEscalation.NORMAL, decision.escalation)
    }

    @Test
    fun improvement_resetsEscalationToNormal() {
        agent.tick(issue())
        clock.advanceBy(COOLDOWN)
        agent.tick(issue()) // escalates to FIRM
        clock.advanceBy(COOLDOWN)
        agent.tick(goodForm()) // CONFIRM_IMPROVEMENT
        clock.advanceBy(COOLDOWN)
        val decision = agent.tick(issue())
        assertEquals(CueEscalation.NORMAL, decision.escalation)
    }

    // --- 3. Confirm on improvement ---

    @Test
    fun issueThenGoodForm_confirmsImprovement() {
        agent.tick(issue())
        clock.advanceBy(COOLDOWN)
        val decision = agent.tick(goodForm())
        assertEquals(CoachIntent.CONFIRM_IMPROVEMENT, decision.intent)
    }

    @Test
    fun repeatedGoodForm_afterConfirmation_isSilent() {
        agent.tick(issue())
        clock.advanceBy(COOLDOWN)
        agent.tick(goodForm())
        val decision = agent.tick(goodForm())
        assertEquals(CoachIntent.SILENT, decision.intent)
    }

    // --- 4. Safety override ---

    @Test
    fun criticalVerdict_firesSafetyOverride_evenInsideNormalCueCooldown() {
        agent.tick(issue())
        val decision = agent.tick(critical())
        assertEquals(CoachIntent.SAFETY_OVERRIDE, decision.intent)
    }

    @Test
    fun criticalVerdict_firesSafetyOverride_evenBelowConfidenceThreshold() {
        val decision = agent.tick(critical(confidence = MIN_CONFIDENCE - 0.1f))
        assertEquals(CoachIntent.SAFETY_OVERRIDE, decision.intent)
    }

    @Test
    fun identicalCriticalVerdict_fasterThanSafetyCooldown_isSilent() {
        agent.tick(critical())
        clock.advanceBy(SAFETY_COOLDOWN - 1)
        val decision = agent.tick(critical())
        assertEquals(CoachIntent.SILENT, decision.intent)
    }

    @Test
    fun identicalCriticalVerdict_afterSafetyCooldownElapses_firesAgain() {
        agent.tick(critical())
        clock.advanceBy(SAFETY_COOLDOWN)
        val decision = agent.tick(critical())
        assertEquals(CoachIntent.SAFETY_OVERRIDE, decision.intent)
    }

    // --- 5. Low-confidence halts coaching ---

    @Test
    fun nonCriticalIssue_belowConfidenceThreshold_isSilentEvenAfterCooldown() {
        clock.advanceBy(COOLDOWN)
        val decision = agent.tick(issue(confidence = MIN_CONFIDENCE - 0.1f))
        assertEquals(CoachIntent.SILENT, decision.intent)
    }

    @Test
    fun confidenceRecovers_coachingResumes() {
        agent.tick(issue(confidence = MIN_CONFIDENCE - 0.1f))
        clock.advanceBy(COOLDOWN)
        val decision = agent.tick(issue(confidence = OK_CONFIDENCE))
        assertEquals(CoachIntent.SPEAK_CUE, decision.intent)
    }

    // --- 6. Pause blocks all state mutation ---

    @Test
    fun paused_cueEligibleVerdict_isSilent() {
        agent.pause()
        val decision = agent.tick(issue())
        assertEquals(CoachIntent.SILENT, decision.intent)
    }

    @Test
    fun paused_persistentIssue_doesNotAdvanceEscalation() {
        agent.tick(issue()) // 1st real cue, NORMAL, not yet escalated
        agent.pause()
        clock.advanceBy(COOLDOWN)
        agent.tick(issue()) // should be ignored entirely
        agent.tick(issue())
        agent.resume()
        clock.advanceBy(COOLDOWN)
        val decision = agent.tick(issue()) // this is really only the 2nd counted cue -> FIRM
        assertEquals(CueEscalation.FIRM, decision.escalation)
    }

    @Test
    fun paused_criticalVerdict_isSilent_pauseBeatsSafetyOverride() {
        agent.pause()
        val decision = agent.tick(critical())
        assertEquals(CoachIntent.SILENT, decision.intent)
    }

    @Test
    fun pauseThenResume_withNoTicksBetween_behavesAsIfNeverPaused() {
        val baseline = CoachAgent(clock)
        val pausedThenResumed = CoachAgent(clock).also {
            it.pause()
            it.resume()
        }

        val expected = baseline.tick(issue())
        val actual = pausedThenResumed.tick(issue())
        assertEquals(expected, actual)
    }
}
