package com.mira.miraai.agent

import com.mira.miraai.assessor.VerdictCode
import com.mira.miraai.assessor.WarriorIIVerdict
import com.mira.miraai.content.PoseSide
import com.mira.miraai.content.Routine
import com.mira.miraai.content.RoutineLevel
import com.mira.miraai.content.RoutineStep
import com.mira.miraai.perception.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun goodForm() = WarriorIIVerdict(VerdictCode.GOOD_FORM, hasCriticalIssue = false, confidence = 0.9f)
private fun critical(code: VerdictCode = VerdictCode.FRONT_KNEE_PAST_ANKLE) =
    WarriorIIVerdict(code, hasCriticalIssue = true, confidence = 0.9f)

private val TWO_STEP_ROUTINE = Routine(
    id = "r", title = "R", categoryIds = emptyList(), level = RoutineLevel.BEGINNER,
    estimatedDurationSec = 0, coverImageRes = 0, isCoachingSupported = true,
    poseSequence = listOf(
        RoutineStep("warrior_ii", PoseSide.BOTH, targetHoldSec = 5, targetReps = null, order = 1),
        RoutineStep("chair_pose", PoseSide.NONE, targetHoldSec = 5, targetReps = null, order = 2),
    ),
)

class WorkoutSessionEngineTest {

    // --- 8.3 hold-timer wiring: completes only once the target is reached AND the trailing
    // clean-tail window (HoldTimer.CLEAN_TAIL_MS) is clean — not just on raw elapsed time.

    @Test
    fun reachingTargetRightAfterAnIssueCleared_doesNotCompleteUntilCleanTailElapses() {
        val engine = WorkoutSessionEngine(TWO_STEP_ROUTINE)
        engine.tick(4_000L, goodForm()) // elapsed=4000, clean=4000 — short of the 5000 target
        engine.tick(2_000L, critical()) // critical: elapsed frozen at 4000, clean streak resets to 0
        val atTarget = engine.tick(1_000L, goodForm()) // elapsed=5000 (== target), clean=1000 (< 2000 tail)
        assertEquals(SessionPhase.HOLDING, atTarget.phase)
        assertEquals(0, atTarget.currentStepIndex) // still on the first step, not yet complete

        val afterCleanTail = engine.tick(1_000L, goodForm()) // elapsed=6000, clean=2000 (== tail)
        assertEquals(SessionPhase.REST, afterCleanTail.phase)
    }

    @Test
    fun criticalIssue_switchesToCorrectingAndDoesNotAdvanceHold() {
        val engine = WorkoutSessionEngine(TWO_STEP_ROUTINE)
        val state = engine.tick(10_000L, critical())
        assertEquals(SessionPhase.CORRECTING, state.phase)
        assertEquals(0, state.elapsedHoldSec)
    }

    // --- 8.2 pause/resume: exact prior state restored, no silent progress ---

    @Test
    fun pause_freezesHoldProgress() {
        val engine = WorkoutSessionEngine(TWO_STEP_ROUTINE)
        engine.tick(2_000L, goodForm())
        engine.pause()
        val paused = engine.tick(3_000L, goodForm())
        assertEquals(SessionPhase.PAUSED, paused.phase)
        assertTrue(paused.isPaused)

        engine.resume()
        val resumed = engine.tick(0L, goodForm())
        assertEquals(2, resumed.elapsedHoldSec) // the 3s while paused never counted
    }

    // --- 8.6 rest between sides vs. between poses ---

    @Test
    fun switchingSidesOfSamePose_getsShortRest() {
        val engine = WorkoutSessionEngine(TWO_STEP_ROUTINE)
        // Clean 5s hold immediately reaches target with a clean streak already >= the tail, so
        // it completes on this single tick — step index advances into the next step right away
        // (so the UI can show "Step 2 of N" through the rest transition).
        val afterFirstSide = engine.tick(5_000L, goodForm())
        assertEquals(SessionPhase.REST, afterFirstSide.phase)
        assertEquals(1, afterFirstSide.currentStepIndex)

        val stillResting = engine.tick(WorkoutThresholds.REST_BETWEEN_SIDES_MS - 500L, goodForm())
        assertEquals(SessionPhase.REST, stillResting.phase)

        val afterRest = engine.tick(1_000L, goodForm())
        assertEquals(SessionPhase.HOLDING, afterRest.phase)
    }

    @Test
    fun movingToADifferentPose_getsLongerRestThanSwitchingSides() {
        val engine = WorkoutSessionEngine(TWO_STEP_ROUTINE)
        engine.tick(5_000L, goodForm()) // complete left warrior_ii -> REST (side-switch window)
        engine.tick(WorkoutThresholds.REST_BETWEEN_SIDES_MS, goodForm()) // rest elapses -> right warrior_ii starts
        val afterSecondSide = engine.tick(5_000L, goodForm()) // complete right warrior_ii
        assertEquals(SessionPhase.REST, afterSecondSide.phase)
        assertEquals(2, afterSecondSide.currentStepIndex) // already pointing at chair_pose

        // A rest shorter than the between-poses window must not yet start chair_pose's hold.
        val stillResting = engine.tick(WorkoutThresholds.REST_BETWEEN_POSES_MS - 500L, goodForm())
        assertEquals(SessionPhase.REST, stillResting.phase)

        val afterRest = engine.tick(1_000L, verdict = null)
        assertEquals(SessionPhase.HOLDING, afterRest.phase)
    }

    // --- Unsupported pose (no rule set) ticks its timer without form-gating ---

    @Test
    fun stepWithNoVerdict_stillCompletesOnTimerAlone() {
        val engine = WorkoutSessionEngine(TWO_STEP_ROUTINE)
        engine.tick(5_000L, goodForm()) // complete left warrior_ii
        engine.tick(WorkoutThresholds.REST_BETWEEN_SIDES_MS, goodForm())
        engine.tick(5_000L, goodForm()) // complete right warrior_ii
        engine.tick(WorkoutThresholds.REST_BETWEEN_POSES_MS, goodForm()) // -> chair_pose starts

        // Now on chair_pose, which has no Assessor rule set — verdict is null throughout.
        val state = engine.tick(5_000L, verdict = null)
        assertTrue(engine.isComplete())
        assertEquals(SessionPhase.SUMMARY, state.phase)
    }

    // --- 8.7 exit / early end ---

    @Test
    fun endingEarly_recordsInProgressStepAsAttemptedNotCompleted() {
        val engine = WorkoutSessionEngine(TWO_STEP_ROUTINE)
        engine.tick(2_000L, critical(VerdictCode.FRONT_KNEE_PAST_ANKLE))
        val summary = engine.endEarly()
        assertEquals(1, summary.stepResults.size)
        assertFalse(summary.stepResults[0].completed)
        assertEquals(Side.LEFT, summary.stepResults[0].side)
    }

    // --- US-7 next-focus line: most frequent non-GOOD_FORM verdict across the session ---

    @Test
    fun summary_nextFocusIsMostFrequentIssueAcrossSteps() {
        val engine = WorkoutSessionEngine(TWO_STEP_ROUTINE)
        engine.tick(1_000L, critical(VerdictCode.FRONT_KNEE_PAST_ANKLE))
        engine.tick(1_000L, critical(VerdictCode.FRONT_KNEE_PAST_ANKLE))
        engine.tick(1_000L, critical(VerdictCode.FRONT_KNEE_PAST_ANKLE))
        val summary = engine.endEarly()
        assertEquals(VerdictCode.FRONT_KNEE_PAST_ANKLE, summary.nextFocusVerdict)
    }

    @Test
    fun summary_allGoodForm_hasNoNextFocus() {
        val engine = WorkoutSessionEngine(TWO_STEP_ROUTINE)
        engine.tick(1_000L, goodForm())
        val summary = engine.endEarly()
        assertEquals(null, summary.nextFocusVerdict)
    }
}
