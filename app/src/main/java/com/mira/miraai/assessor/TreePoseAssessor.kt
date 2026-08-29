package com.mira.miraai.assessor

import com.mira.miraai.perception.BodyJoint
import com.mira.miraai.perception.PoseFrame
import com.mira.miraai.perception.Side
import kotlin.math.abs

/** Single-frame Tree Pose assessment result — same contract as [WarriorIIVerdict]. */
data class TreePoseVerdict(
    override val verdictCode: VerdictCode,
    override val hasCriticalIssue: Boolean,
    override val confidence: Float,
) : Verdict

/**
 * Deterministic Tree Pose rule engine, pure Kotlin (CLAUDE.md). Only 10 joints are tracked
 * ([com.mira.miraai.perception.BodyJoint] — no toe/heel landmark), so "foot on inner calf/thigh,
 * avoiding the knee" (feature-spec.md's own instruction text for this pose) can't be verified
 * precisely; the lifted-foot check here only confirms the foot is raised at least to standing-knee
 * height, not exact placement. Flagged, not silently overclaimed.
 *
 * [standingLeg]'s knee angle collapsing below [TreePoseThresholds.STANDING_LEG_STRAIGHT_MIN_DEG]
 * is treated as critical (same design choice as Warrior II's front-knee-past-ankle) since a bent
 * standing leg in a single-leg balance pose is a real fall risk, not just a form nitpick — a
 * decision made where the spec is silent, flagged here rather than guessed silently.
 */
class TreePoseAssessor {

    private val criticalCodes = setOf(VerdictCode.TREE_STANDING_LEG_BENT, VerdictCode.INSUFFICIENT_VISIBILITY)

    fun assess(frame: PoseFrame, standingLeg: Side): TreePoseVerdict {
        val liftedLeg = if (standingLeg == Side.LEFT) Side.RIGHT else Side.LEFT
        val keyJoints = keyJoints(standingLeg, liftedLeg)

        val confidence = keyJoints.map { frame.landmark(it)?.visibility ?: 0f }.average().toFloat()

        if (keyJoints.any { (frame.landmark(it)?.visibility ?: 0f) < TreePoseThresholds.MIN_LANDMARK_VISIBILITY }) {
            return TreePoseVerdict(VerdictCode.INSUFFICIENT_VISIBILITY, hasCriticalIssue = true, confidence = confidence)
        }

        val standingHip = position(frame, hipJoint(standingLeg))
        val standingKnee = position(frame, kneeJoint(standingLeg))
        val standingAnkle = position(frame, ankleJoint(standingLeg))
        val liftedAnkle = position(frame, ankleJoint(liftedLeg))
        val leftHip = position(frame, BodyJoint.LEFT_HIP)
        val rightHip = position(frame, BodyJoint.RIGHT_HIP)
        val leftWrist = position(frame, BodyJoint.LEFT_WRIST)
        val rightWrist = position(frame, BodyJoint.RIGHT_WRIST)
        val leftEar = position(frame, BodyJoint.LEFT_EAR)
        val rightEar = position(frame, BodyJoint.RIGHT_EAR)

        val standingLegAngle = angleDegrees(standingHip, standingKnee, standingAnkle)

        val issues = buildList {
            if (standingLegAngle < TreePoseThresholds.STANDING_LEG_STRAIGHT_MIN_DEG) {
                add(VerdictCode.TREE_STANDING_LEG_BENT)
            }
            // Normalized y increases downward, so a raised foot has a smaller y than the knee.
            if (liftedAnkle.y > standingKnee.y + TreePoseThresholds.LIFTED_FOOT_MIN_HEIGHT_TOLERANCE) {
                add(VerdictCode.TREE_LIFTED_FOOT_TOO_LOW)
            }
            if (abs(leftHip.y - rightHip.y) > TreePoseThresholds.HIPS_LEVEL_TOLERANCE) {
                add(VerdictCode.TREE_HIPS_NOT_LEVEL)
            }
            if (abs(leftWrist.x - rightWrist.x) > TreePoseThresholds.ARMS_AT_CHEST_X_TOLERANCE) {
                add(VerdictCode.TREE_ARMS_NOT_AT_CHEST)
            }
            if (abs(leftEar.y - rightEar.y) > TreePoseThresholds.HEAD_TILT_TOLERANCE) {
                add(VerdictCode.TREE_HEAD_TILTED)
            }
        }

        val verdictCode = issues.firstOrNull() ?: VerdictCode.GOOD_FORM
        return TreePoseVerdict(verdictCode, hasCriticalIssue = verdictCode in criticalCodes, confidence = confidence)
    }

    private fun keyJoints(standingLeg: Side, liftedLeg: Side) = listOf(
        hipJoint(standingLeg), kneeJoint(standingLeg), ankleJoint(standingLeg),
        hipJoint(liftedLeg), ankleJoint(liftedLeg),
        BodyJoint.LEFT_SHOULDER, BodyJoint.RIGHT_SHOULDER,
        BodyJoint.LEFT_WRIST, BodyJoint.RIGHT_WRIST,
        BodyJoint.LEFT_EAR, BodyJoint.RIGHT_EAR,
    )

    private fun hipJoint(side: Side) = if (side == Side.LEFT) BodyJoint.LEFT_HIP else BodyJoint.RIGHT_HIP
    private fun kneeJoint(side: Side) = if (side == Side.LEFT) BodyJoint.LEFT_KNEE else BodyJoint.RIGHT_KNEE
    private fun ankleJoint(side: Side) = if (side == Side.LEFT) BodyJoint.LEFT_ANKLE else BodyJoint.RIGHT_ANKLE

    private fun position(frame: PoseFrame, joint: BodyJoint): Point2D = frame.landmarks.getValue(joint).position
}
