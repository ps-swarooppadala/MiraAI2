package com.mira.miraai.assessor

import com.mira.miraai.perception.BodyJoint
import com.mira.miraai.perception.Landmark
import com.mira.miraai.perception.PoseFrame
import com.mira.miraai.perception.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Given/When/Then against a hand-authored "textbook" Tree Pose fixture, same fixture-building
 * style as [WarriorIIAssessorTest]. Standing leg is LEFT throughout — the mirror case is covered
 * by [assess_returnsGoodForm_whenStandingOnRightLeg], same pattern Warrior II used to prove
 * mirror-symmetry rather than duplicating every case for both sides.
 */
class TreePoseAssessorTest {

    private val assessor = TreePoseAssessor()

    private fun landmark(x: Float, y: Float, visibility: Float = 1f) =
        Landmark(Point2D(x, y), visibility)

    /**
     * Standing on the left leg, straight (hip/knee/ankle collinear -> 180 deg). Lifted (right)
     * ankle rests above the standing knee's height. Hips level, wrists together at chest height.
     */
    private fun baseline(): Map<BodyJoint, Landmark> = mapOf(
        BodyJoint.LEFT_HIP to landmark(0.5f, 0.45f),
        BodyJoint.LEFT_KNEE to landmark(0.5f, 0.65f),
        BodyJoint.LEFT_ANKLE to landmark(0.5f, 0.85f),
        BodyJoint.RIGHT_HIP to landmark(0.5f, 0.45f),
        BodyJoint.RIGHT_KNEE to landmark(0.65f, 0.55f),
        BodyJoint.RIGHT_ANKLE to landmark(0.55f, 0.63f),
        BodyJoint.LEFT_SHOULDER to landmark(0.45f, 0.20f),
        BodyJoint.RIGHT_SHOULDER to landmark(0.55f, 0.20f),
        BodyJoint.LEFT_WRIST to landmark(0.49f, 0.32f),
        BodyJoint.RIGHT_WRIST to landmark(0.51f, 0.32f),
        BodyJoint.LEFT_EAR to landmark(0.47f, 0.10f),
        BodyJoint.RIGHT_EAR to landmark(0.53f, 0.10f),
    )

    private fun frame(landmarks: Map<BodyJoint, Landmark>) = PoseFrame(landmarks)
    private fun Map<BodyJoint, Landmark>.with(vararg overrides: Pair<BodyJoint, Landmark>) = this + overrides

    @Test
    fun assess_returnsGoodForm_whenTextbookPoseStandingOnLeftLeg() {
        val verdict = assessor.assess(frame(baseline()), Side.LEFT)
        assertEquals(VerdictCode.GOOD_FORM, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsGoodForm_whenStandingOnRightLeg() {
        // Mirror the baseline: swap LEFT/RIGHT joint identities.
        val mirrored = mapOf(
            BodyJoint.RIGHT_HIP to landmark(0.5f, 0.45f),
            BodyJoint.RIGHT_KNEE to landmark(0.5f, 0.65f),
            BodyJoint.RIGHT_ANKLE to landmark(0.5f, 0.85f),
            BodyJoint.LEFT_HIP to landmark(0.5f, 0.45f),
            BodyJoint.LEFT_KNEE to landmark(0.35f, 0.55f),
            BodyJoint.LEFT_ANKLE to landmark(0.45f, 0.63f),
            BodyJoint.LEFT_SHOULDER to landmark(0.45f, 0.20f),
            BodyJoint.RIGHT_SHOULDER to landmark(0.55f, 0.20f),
            BodyJoint.LEFT_WRIST to landmark(0.49f, 0.32f),
            BodyJoint.RIGHT_WRIST to landmark(0.51f, 0.32f),
            BodyJoint.LEFT_EAR to landmark(0.47f, 0.10f),
            BodyJoint.RIGHT_EAR to landmark(0.53f, 0.10f),
        )
        val verdict = assessor.assess(frame(mirrored), Side.RIGHT)
        assertEquals(VerdictCode.GOOD_FORM, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsStandingLegBent_andIsCritical_whenStandingKneeAngleBelowThreshold() {
        val landmarks = baseline().with(BodyJoint.LEFT_KNEE to landmark(0.6f, 0.6f))
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.TREE_STANDING_LEG_BENT, verdict.verdictCode)
        assertTrue(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsLiftedFootTooLow_whenLiftedAnkleDropsBelowStandingKnee() {
        val landmarks = baseline().with(BodyJoint.RIGHT_ANKLE to landmark(0.55f, 0.75f))
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.TREE_LIFTED_FOOT_TOO_LOW, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsHipsNotLevel_whenHipHeightsDiverge() {
        val landmarks = baseline().with(BodyJoint.RIGHT_HIP to landmark(0.5f, 0.55f))
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.TREE_HIPS_NOT_LEVEL, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsArmsNotAtChest_whenWristsSpreadApart() {
        val landmarks = baseline().with(
            BodyJoint.LEFT_WRIST to landmark(0.3f, 0.32f),
            BodyJoint.RIGHT_WRIST to landmark(0.7f, 0.32f),
        )
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.TREE_ARMS_NOT_AT_CHEST, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsHeadTilted_whenEarHeightsDiverge() {
        val landmarks = baseline().with(BodyJoint.RIGHT_EAR to landmark(0.53f, 0.16f))
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.TREE_HEAD_TILTED, verdict.verdictCode)
        assertFalse(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_returnsInsufficientVisibility_whenKeyLandmarkBelowVisibilityThreshold() {
        val landmarks = baseline().with(BodyJoint.LEFT_ANKLE to landmark(0.5f, 0.85f, visibility = 0.2f))
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.INSUFFICIENT_VISIBILITY, verdict.verdictCode)
        assertTrue(verdict.hasCriticalIssue)
    }

    @Test
    fun assess_prioritizesCriticalStandingLegIssue_whenNonCriticalHipsIssueAlsoPresent() {
        val landmarks = baseline().with(
            BodyJoint.LEFT_KNEE to landmark(0.6f, 0.6f),
            BodyJoint.RIGHT_HIP to landmark(0.5f, 0.55f),
        )
        val verdict = assessor.assess(frame(landmarks), Side.LEFT)
        assertEquals(VerdictCode.TREE_STANDING_LEG_BENT, verdict.verdictCode)
        assertTrue(verdict.hasCriticalIssue)
    }
}
