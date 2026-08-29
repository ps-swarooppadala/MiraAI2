package com.mira.miraai.assessor

import com.mira.miraai.perception.BodyJoint
import com.mira.miraai.perception.Landmark
import com.mira.miraai.perception.PoseFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Given/When/Then against a hand-authored "textbook" Chair Pose fixture — no `Side` param, both legs symmetric. */
class ChairPoseAssessorTest {

    private val assessor = ChairPoseAssessor()

    private fun landmark(x: Float, y: Float, visibility: Float = 1f) =
        Landmark(Point2D(x, y), visibility)

    /**
     * Both knees bent to ~110 deg (within [ChairPoseThresholds.KNEE_TARGET_DEG] +/-
     * tolerance), shins vertical over the ankles (no knee-past-toe drift), torso centered over
     * the hips, arms raised overhead.
     */
    private fun baseline(): Map<BodyJoint, Landmark> = mapOf(
        BodyJoint.LEFT_HIP to landmark(0.32f, 0.5836f),
        BodyJoint.RIGHT_HIP to landmark(0.68f, 0.5836f),
        BodyJoint.LEFT_KNEE to landmark(0.42f, 0.62f),
        BodyJoint.RIGHT_KNEE to landmark(0.58f, 0.62f),
        BodyJoint.LEFT_ANKLE to landmark(0.42f, 0.85f),
        BodyJoint.RIGHT_ANKLE to landmark(0.58f, 0.85f),
        BodyJoint.LEFT_SHOULDER to landmark(0.42f, 0.20f),
        BodyJoint.RIGHT_SHOULDER to landmark(0.58f, 0.20f),
        BodyJoint.LEFT_ELBOW to landmark(0.42f, 0.125f),
        BodyJoint.RIGHT_ELBOW to landmark(0.58f, 0.125f),
        BodyJoint.LEFT_WRIST to landmark(0.42f, 0.05f),
        BodyJoint.RIGHT_WRIST to landmark(0.58f, 0.05f),
    )

    private fun frame(landmarks: Map<BodyJoint, Landmark>) = PoseFrame(landmarks)
    private fun Map<BodyJoint, Landmark>.with(vararg overrides: Pair<BodyJoint, Landmark>) = this + overrides

    @Test
    fun assess_returnsGoodForm_whenTextbookPose() {
        val verdict = assessor.assess(frame(baseline()))
        assertEquals(VerdictCode.GOOD_FORM, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsNotLowEnough_whenBothLegsStraighterThanTarget() {
        val landmarks = baseline().with(
            BodyJoint.LEFT_HIP to landmark(0.42f, 0.45f),
            BodyJoint.RIGHT_HIP to landmark(0.58f, 0.45f),
        )
        val verdict = assessor.assess(frame(landmarks))
        assertEquals(VerdictCode.CHAIR_NOT_LOW_ENOUGH, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsKneesPastToes_whenAKneeDriftsForwardOfItsAnkle() {
        val landmarks = baseline().with(BodyJoint.LEFT_KNEE to landmark(0.55f, 0.62f))
        val verdict = assessor.assess(frame(landmarks))
        assertEquals(VerdictCode.CHAIR_KNEES_PAST_TOES, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsTorsoTooForward_whenShoulderMidpointDriftsFromHipMidpoint() {
        val landmarks = baseline().with(
            BodyJoint.LEFT_SHOULDER to landmark(0.27f, 0.20f),
            BodyJoint.RIGHT_SHOULDER to landmark(0.43f, 0.20f),
        )
        val verdict = assessor.assess(frame(landmarks))
        assertEquals(VerdictCode.CHAIR_TORSO_TOO_FORWARD, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsArmsNotRaised_whenWristsDropBelowShoulders() {
        val landmarks = baseline().with(
            BodyJoint.LEFT_WRIST to landmark(0.42f, 0.30f),
            BodyJoint.RIGHT_WRIST to landmark(0.58f, 0.30f),
        )
        val verdict = assessor.assess(frame(landmarks))
        assertEquals(VerdictCode.CHAIR_ARMS_NOT_RAISED, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsArmsNotStraight_whenElbowsBendUnderTarget() {
        val landmarks = baseline().with(
            BodyJoint.LEFT_ELBOW to landmark(0.36f, 0.125f),
            BodyJoint.RIGHT_ELBOW to landmark(0.64f, 0.125f),
        )
        val verdict = assessor.assess(frame(landmarks))
        assertEquals(VerdictCode.CHAIR_ARMS_NOT_STRAIGHT, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsInsufficientVisibility_whenKeyLandmarkBelowVisibilityThreshold() {
        val landmarks = baseline().with(BodyJoint.LEFT_KNEE to landmark(0.42f, 0.62f, visibility = 0.2f))
        val verdict = assessor.assess(frame(landmarks))
        assertEquals(VerdictCode.INSUFFICIENT_VISIBILITY, verdict.verdictCode)
        assertTrue(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_prioritizesNotLowEnough_whenKneesPastToesAlsoPresent() {
        val landmarks = baseline().with(
            BodyJoint.LEFT_HIP to landmark(0.42f, 0.45f),
            BodyJoint.RIGHT_HIP to landmark(0.58f, 0.45f),
            BodyJoint.LEFT_KNEE to landmark(0.55f, 0.62f),
        )
        val verdict = assessor.assess(frame(landmarks))
        assertEquals(VerdictCode.CHAIR_NOT_LOW_ENOUGH, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }
}
