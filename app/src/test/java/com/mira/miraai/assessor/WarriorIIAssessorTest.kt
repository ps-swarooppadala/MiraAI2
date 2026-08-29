package com.mira.miraai.assessor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WarriorIIAssessorTest {

    private val assessor = WarriorIIAssessor()

    private fun landmark(x: Float, y: Float, visibility: Float = 1f) =
        Landmark(Point2D(x, y), visibility)

    /**
     * Baseline "textbook" Warrior II with the front (bent) leg on [frontLeg]. Coordinates are
     * normalized image space (0..1), y increasing downward. Constructed so the front-knee angle
     * equals target (90 deg), the back leg is straight (180 deg), wrists are level with
     * shoulders, and the torso is centered over the hips — i.e. GOOD_FORM.
     */
    private fun baseline(frontLeg: Side): Map<BodyJoint, Landmark> {
        val canonical = mapOf(
            BodyJoint.LEFT_HIP to landmark(0.6f, 0.45f),
            BodyJoint.LEFT_KNEE to landmark(0.4f, 0.45f),
            BodyJoint.LEFT_ANKLE to landmark(0.4f, 0.70f),
            BodyJoint.RIGHT_HIP to landmark(0.75f, 0.45f),
            BodyJoint.RIGHT_KNEE to landmark(0.75f, 0.68f),
            BodyJoint.RIGHT_ANKLE to landmark(0.75f, 0.90f),
            BodyJoint.LEFT_SHOULDER to landmark(0.575f, 0.20f),
            BodyJoint.RIGHT_SHOULDER to landmark(0.775f, 0.20f),
            BodyJoint.LEFT_WRIST to landmark(0.4f, 0.20f),
            BodyJoint.RIGHT_WRIST to landmark(0.95f, 0.20f),
        )
        return if (frontLeg == Side.LEFT) canonical else mirror(canonical)
    }

    /** Reflects x (x' = 1 - x) and swaps LEFT/RIGHT joint identities, for the mirror-symmetry test. */
    private fun mirror(frame: Map<BodyJoint, Landmark>): Map<BodyJoint, Landmark> {
        fun flip(joint: BodyJoint) = when (joint) {
            BodyJoint.LEFT_HIP -> BodyJoint.RIGHT_HIP
            BodyJoint.RIGHT_HIP -> BodyJoint.LEFT_HIP
            BodyJoint.LEFT_KNEE -> BodyJoint.RIGHT_KNEE
            BodyJoint.RIGHT_KNEE -> BodyJoint.LEFT_KNEE
            BodyJoint.LEFT_ANKLE -> BodyJoint.RIGHT_ANKLE
            BodyJoint.RIGHT_ANKLE -> BodyJoint.LEFT_ANKLE
            BodyJoint.LEFT_SHOULDER -> BodyJoint.RIGHT_SHOULDER
            BodyJoint.RIGHT_SHOULDER -> BodyJoint.LEFT_SHOULDER
            BodyJoint.LEFT_WRIST -> BodyJoint.RIGHT_WRIST
            BodyJoint.RIGHT_WRIST -> BodyJoint.LEFT_WRIST
        }
        return frame.entries.associate { (joint, lm) ->
            flip(joint) to Landmark(Point2D(1f - lm.position.x, lm.position.y), lm.visibility)
        }
    }

    private fun frame(landmarks: Map<BodyJoint, Landmark>) = PoseFrame(landmarks)

    private fun Map<BodyJoint, Landmark>.with(vararg overrides: Pair<BodyJoint, Landmark>) = this + overrides

    @Test
    fun assess_returnsGoodForm_whenTextbookPoseWithLeftFrontLeg() {
        val verdict = assessor.assess(frame(baseline(Side.LEFT)), Side.LEFT)
        assertEquals(VerdictCode.GOOD_FORM, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsGoodForm_whenTextbookPoseWithRightFrontLeg() {
        val verdict = assessor.assess(frame(baseline(Side.RIGHT)), Side.RIGHT)
        assertEquals(VerdictCode.GOOD_FORM, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsFrontKneeTooStraight_whenFrontKneeAngleFarAboveTarget() {
        val landmarks = baseline(Side.LEFT).with(BodyJoint.LEFT_HIP to landmark(0.6f, 0.20f))
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.FRONT_KNEE_TOO_STRAIGHT, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsFrontKneeOverBent_whenFrontKneeAngleFarBelowTarget() {
        val landmarks = baseline(Side.LEFT).with(BodyJoint.LEFT_HIP to landmark(0.6f, 0.55f))
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.FRONT_KNEE_OVER_BENT, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsFrontKneePastAnkle_andIsCritical_whenKneeTracksBeyondAnkle() {
        val landmarks = baseline(Side.LEFT).with(BodyJoint.LEFT_ANKLE to landmark(0.72f, 0.70f))
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.FRONT_KNEE_PAST_ANKLE, verdict.verdictCode)
        assertTrue(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsBackLegBent_whenBackLegAngleBelowStraightThreshold() {
        val landmarks = baseline(Side.LEFT).with(BodyJoint.RIGHT_KNEE to landmark(0.85f, 0.68f))
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.BACK_LEG_BENT, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsArmsNotLevel_whenOneWristDropsBelowShoulderTolerance() {
        val landmarks = baseline(Side.LEFT).with(BodyJoint.LEFT_WRIST to landmark(0.4f, 0.45f))
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.ARMS_NOT_LEVEL, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsTorsoLeaning_whenShoulderMidpointDriftsFromHipMidpoint() {
        val landmarks = baseline(Side.LEFT).with(
            BodyJoint.LEFT_SHOULDER to landmark(0.475f, 0.20f),
            BodyJoint.RIGHT_SHOULDER to landmark(0.675f, 0.20f),
        )
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.TORSO_LEANING, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsInsufficientVisibility_whenKeyLandmarksBelowVisibilityThreshold() {
        val landmarks = baseline(Side.LEFT).with(
            BodyJoint.LEFT_KNEE to landmark(0.4f, 0.45f, visibility = 0.2f)
        )
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.INSUFFICIENT_VISIBILITY, verdict.verdictCode)
        assertTrue(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_prioritizesCriticalKneeIssue_whenNonCriticalTorsoLeanAlsoPresent() {
        val landmarks = baseline(Side.LEFT).with(
            BodyJoint.LEFT_ANKLE to landmark(0.72f, 0.70f),
            BodyJoint.LEFT_SHOULDER to landmark(0.475f, 0.20f),
            BodyJoint.RIGHT_SHOULDER to landmark(0.675f, 0.20f),
        )
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.FRONT_KNEE_PAST_ANKLE, verdict.verdictCode)
        assertTrue(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_hasCriticalIssueFalse_whenOnlyANonCriticalDeviationIsPresent() {
        val landmarks = baseline(Side.LEFT).with(
            BodyJoint.LEFT_SHOULDER to landmark(0.475f, 0.20f),
            BodyJoint.RIGHT_SHOULDER to landmark(0.675f, 0.20f),
        )
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.TORSO_LEANING, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }
}
